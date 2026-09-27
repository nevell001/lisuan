package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 界面前端状态字段必须"声明即初始化"（TD-016）。
 *
 * <p>2026-09 在 Windows 实测撞到两次崩溃：页面打开时**两个异步加载器互相依赖**，任一个先回来都会调用
 * 读取另一个尚未赋值的字段：</p>
 * <pre>
 * NullPointerException: Cannot invoke "ObservableList.size()" because "this.inventoryList" is null
 *     at InventoryController.updateCountLabel   ← 分类下拉框回调触发 applyFilters
 * NullPointerException: Cannot invoke "Map.values()" because "this.orders" is null
 *     at PurchaseOrderController.filterOrders   ← 供应商回调触发 filterOrders
 * </pre>
 *
 * <p>{@code initialize()} 里注册的监听器（例如"默认选中第一个分类"）会被**异步回调**触发，
 * 而数据字段要到另一个回调才赋值——于是顺序一变就 NPE。这类缺陷在 mac 上不一定复现，
 * 本机测试全绿也挡不住。</p>
 *
 * <p>规则：控制器里的集合状态字段一律**声明即初始化**（`private final List&lt;X&gt; xs = new ArrayList&lt;&gt;()`，
 * 或不可 final 时至少带初始化器）。这样"数据还没到"表现为**空集合**而不是 null，读取顺序不再敏感。
 * JavaFX 注入的控件（{@code TableView}/{@code ListView} 等）不是集合，不在此规则内。</p>
 */
@DisplayName("界面状态字段初始化门禁")
class UiStateInitializationPolicyTest {

    /** 集合类型（不含 JavaFX 控件）。 */
    private static final String COLLECTION_TYPES =
        "List|Map|Set|Collection|Deque|Queue|ObservableList|ObservableMap|ObservableSet"
            + "|ArrayList|LinkedList|HashMap|LinkedHashMap|TreeMap|HashSet|LinkedHashSet|TreeSet|ArrayDeque";

    /** 声明了集合字段但**没有初始化器**：`private List<X> xs;` */
    private static final Pattern UNINITIALIZED_COLLECTION_FIELD = Pattern.compile(
        "^\\s*private\\s+(?:static\\s+)?(?!final\\b)(" + COLLECTION_TYPES
            + ")[<>\\w,\\s\\.\\[\\]]*\\s+(\\w+)\\s*;\\s*$");

    /** final 但同样没有初始化器的（那连编译都过不了，双保险）。 */
    private static final Pattern UNINITIALIZED_FINAL_FIELD = Pattern.compile(
        "^\\s*private\\s+static\\s+final\\s+(" + COLLECTION_TYPES
            + ")[<>\\w,\\s\\.\\[\\]]*\\s+(\\w+)\\s*;\\s*$");

    @Test
    @DisplayName("控制器里的集合状态字段必须声明即初始化")
    void controllerCollectionsAreInitializedAtDeclaration() throws Exception {
        List<String> violations = new ArrayList<>();
        List<Path> controllers = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java/com/cashier/controller"))) {
            controllers.addAll(walk.filter(p -> p.toString().endsWith(".java")).toList());
        }
        assertTrue(controllers.size() > 20, "未找到控制器源码，路径不对？");

        for (Path file : controllers) {
            String text = Files.readString(file);
            check(text, file, UNINITIALIZED_COLLECTION_FIELD, violations);
            check(text, file, UNINITIALIZED_FINAL_FIELD, violations);
        }

        assertTrue(violations.isEmpty(),
            "以下集合字段没有初始化器：数据由异步回调赋值，别处可能在数据到达前读它 —— "
                + "Windows 实测已因此抛过 NullPointerException（item 还没到、筛选/计数就先跑了）。"
                + "改法：声明即初始化，例如 private final List<X> xs = new java.util.ArrayList<>(); "
                + "（ObservableList 用 javafx.collections.FXCollections.observableArrayList()）：\n  "
                + String.join("\n  ", violations));
    }

    private static void check(String text, Path file, Pattern pattern, List<String> violations) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if ("ListView".equals(matcher.group(1))) {
                continue; // FXML 注入的控件不是集合
            }
            int line = text.substring(0, matcher.start()).split("\n", -1).length;
            violations.add(file + ":" + line + "  →  " + matcher.group().trim());
        }
    }

    @Test
    @DisplayName("曾经崩溃的两个字段必须是声明即初始化（回归锚点）")
    void previouslyCrashingFieldsStayInitialized() throws IOException {
        assertInitialized("src/main/java/com/cashier/controller/InventoryController.java",
            "inventoryList", "inventoryMap");
        assertInitialized("src/main/java/com/cashier/controller/PurchaseOrderController.java",
            "orders", "suppliers");
        assertInitialized("src/main/java/com/cashier/controller/CartController.java", "inventoryMap");
        assertInitialized("src/main/java/com/cashier/controller/MemberController.java", "memberList");
        assertInitialized("src/main/java/com/cashier/controller/SupplierController.java", "supplierList");
    }

    private static void assertInitialized(String file, String... fields) throws IOException {
        String text = Files.readString(Path.of(file));
        for (String field : fields) {
            // 泛型里有空格（Map<Integer, Product>），所以类型部分只能宽松匹配
            Pattern declaration = Pattern.compile(
                "^\\s*private\\s+.*\\b" + field + "\\s*=", Pattern.MULTILINE);
            assertTrue(declaration.matcher(text).find(),
                file + " 的 " + field + " 必须在声明处就初始化（否则异步回调顺序一变就 NPE）");
        }
    }
}
