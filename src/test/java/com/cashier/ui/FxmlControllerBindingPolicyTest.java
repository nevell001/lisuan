package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FXML 与控制器绑定门禁（2026-09 审计发现）。
 *
 * <p>现象：按 <b>F8</b>（或命令面板"收银台"）进入收银台后，左侧导航高亮消失——因为
 * {@code MainController.handleCheckout} 调用了 {@code setActiveButton(checkoutBtn)}，
 * 而 {@code MainView.fxml} 里根本没有 {@code checkoutBtn}（按钮 id 是 {@code cartBtn}），
 * 于是"清掉旧高亮 + 不设新高亮"= 没有任何按钮处于选中态。FXML 注入是**按名字绑定**的：
 * 名字对不上不会报错，字段只是保持 {@code null}，直到某处解引用才变成 NPE 或静默失效。</p>
 *
 * <p>同一类缺陷的另一种表现是 {@code onAction="#handleXxx"} 写错名字——FXMLLoader 会在加载
 * 视图时抛 {@code LoadException}，页面直接打不开。本门禁把两个方向都钉住：</p>
 * <ol>
 *   <li>FXML 里引用的每个事件处理器都能在 {@code fx:controller} 指定的类里找到方法；</li>
 *   <li>每个 {@code @FXML} 字段至少在某一个声明了该控制器的 FXML 里有同名 {@code fx:id}
 *       （合法豁免登记在 {@link #KNOWN_ORPHAN_FIELDS}，每条都要写清原因，且豁免失效即报错）。</li>
 * </ol>
 */
@DisplayName("FXML 与控制器绑定门禁")
class FxmlControllerBindingPolicyTest {

    private static final Path VIEW_DIR = Path.of("src/main/resources/com/cashier/view");
    private static final Path MAIN_SRC = Path.of("src/main/java");

    private static final Pattern CONTROLLER = Pattern.compile("fx:controller=\"([^\"]+)\"");
    private static final Pattern HANDLER = Pattern.compile("on\\w+=\"#([^\"]+)\"");
    private static final Pattern FX_ID = Pattern.compile("fx:id=\"([^\"]+)\"");
    private static final Pattern FX_FIELD = Pattern.compile(
        "@FXML\\s+(?:public\\s+|private\\s+|protected\\s+)?[\\w<>,\\[\\]\\. ]+\\s+(\\w+)\\s*;");

    /**
     * 允许"声明了 @FXML 但 FXML 里没有同名 fx:id"的字段。加入前先判断属于哪种情况：
     * <ul>
     *   <li>遗留字段：代码已判空，注入与否都不影响；</li>
     *   <li>未使用字段：既没有 fx:id 也没有任何代码引用（登记待清理，见 docs/TECH_DEBT.md）。</li>
     * </ul>
     * 任何**被解引用却没有判空**的孤儿字段都是真 NPE，不许进这张表。
     */
    private static final Map<String, String> KNOWN_ORPHAN_FIELDS = new LinkedHashMap<>();

    static {
        // 遗留字段：CartView.fxml 未提供这两个节点，控制器里两处使用都判了空。
        KNOWN_ORPHAN_FIELDS.put("com.cashier.controller.CartController.addButton",
            "遗留字段：FXML 无此节点，代码已判空（CartController:518/1374）");
        KNOWN_ORPHAN_FIELDS.put("com.cashier.controller.CartController.removeButton",
            "遗留字段：FXML 无此节点，代码已判空（CartController:513/1377）");
        // 未使用字段：声明后从未被引用，删除即可（登记待清理）。
        KNOWN_ORPHAN_FIELDS.put("com.cashier.controller.ReturnReportController.generateButton",
            "未使用字段：无 fx:id、无代码引用");
        KNOWN_ORPHAN_FIELDS.put("com.cashier.controller.ReturnReportController.exportButton",
            "未使用字段：无 fx:id、无代码引用");
        KNOWN_ORPHAN_FIELDS.put("com.cashier.controller.ReturnReportController.refreshButton",
            "未使用字段：无 fx:id、无代码引用");
        KNOWN_ORPHAN_FIELDS.put("com.cashier.controller.StatisticsController.queryButton",
            "未使用字段：无 fx:id、无代码引用");
        KNOWN_ORPHAN_FIELDS.put("com.cashier.controller.StatisticsController.exportButton",
            "未使用字段：无 fx:id、无代码引用");
    }

    @Test
    @DisplayName("FXML 引用的每个事件处理器都必须存在于对应控制器")
    void everyFxmlHandlerResolvesToControllerMethod() throws Exception {
        List<String> unresolved = new ArrayList<>();
        int handlers = 0;
        for (Path fxml : fxmlFiles()) {
            String xml = Files.readString(fxml);
            Matcher controller = CONTROLLER.matcher(xml);
            if (!controller.find()) {
                continue;
            }
            String source = readControllerSource(controller.group(1));
            if (source == null) {
                unresolved.add(fxml.getFileName() + ": 找不到控制器 " + controller.group(1));
                continue;
            }
            Set<String> names = new HashSet<>();
            Matcher handler = HANDLER.matcher(xml);
            while (handler.find()) {
                names.add(handler.group(1));
            }
            for (String name : names) {
                handlers++;
                if (!Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\(").matcher(source).find()) {
                    unresolved.add(fxml.getFileName() + " -> " + controller.group(1) + "#" + name + "() 不存在");
                }
            }
        }

        assertTrue(handlers > 150, "识别到的 FXML 事件处理器只有 " + handlers
            + " 个，门禁可能已空转（解析失效），请检查扫描逻辑");
        assertTrue(unresolved.isEmpty(), "FXML 引用了不存在的处理器（页面会在加载时抛 LoadException）:\n"
            + String.join("\n", unresolved));
    }

    @Test
    @DisplayName("每个 @FXML 字段都必须有同名 fx:id（豁免表须登记且不得过期）")
    void everyInjectedFieldHasMatchingFxId() throws Exception {
        Map<String, Set<String>> idsByController = new HashMap<>();
        int controllers = 0;
        for (Path fxml : fxmlFiles()) {
            String xml = Files.readString(fxml);
            Matcher controller = CONTROLLER.matcher(xml);
            if (!controller.find()) {
                continue;
            }
            controllers++;
            Set<String> ids = idsByController.computeIfAbsent(controller.group(1), key -> new HashSet<>());
            Matcher id = FX_ID.matcher(xml);
            while (id.find()) {
                ids.add(id.group(1));
            }
        }

        int fields = 0;
        List<String> orphans = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : idsByController.entrySet()) {
            String source = readControllerSource(entry.getKey());
            if (source == null) {
                continue;
            }
            Matcher field = FX_FIELD.matcher(source);
            while (field.find()) {
                fields++;
                String name = field.group(1);
                if (entry.getValue().contains(name)) {
                    continue;
                }
                String key = entry.getKey() + "." + name;
                seen.add(key);
                if (!KNOWN_ORPHAN_FIELDS.containsKey(key)) {
                    orphans.add(key + "（FXML 里没有 fx:id=\"" + name + "\"；"
                        + "注入会静默失败，解引用即 NPE，请补 fx:id 或删除字段）");
                }
            }
        }

        assertTrue(controllers >= 30, "只解析到 " + controllers + " 个带 fx:controller 的 FXML，门禁可能已空转");
        assertTrue(fields > 500, "只解析到 " + fields + " 个 @FXML 字段，门禁可能已空转");
        assertTrue(orphans.isEmpty(), "存在没有对应 fx:id 的 @FXML 字段:\n" + String.join("\n", orphans));

        // 豁免表不得腐烂：字段已补上 fx:id、或字段已被删除时，必须同步删掉豁免。
        List<String> stale = new ArrayList<>();
        for (String key : KNOWN_ORPHAN_FIELDS.keySet()) {
            if (!seen.contains(key)) {
                stale.add(key + "（已不是孤儿字段或已不存在，请从 KNOWN_ORPHAN_FIELDS 删除）");
            }
        }
        assertTrue(stale.isEmpty(), "孤儿字段豁免表已过期:\n" + String.join("\n", stale));
    }

    @Test
    @DisplayName("F8/命令面板进入收银台与导航按钮同路径（曾指向不存在的 checkoutBtn）")
    void checkoutEntryPointSharesCartNavigation() throws Exception {
        String source = Files.readString(MAIN_SRC.resolve("com/cashier/controller/MainController.java"));
        String code = withoutComments(source);

        // 只看代码形态（注释里提到这个名字是允许的）
        assertFalse(Pattern.compile("setActiveButton\\s*\\(\\s*checkoutBtn\\s*\\)").matcher(code).find(),
            "MainController 不得再调用 setActiveButton(checkoutBtn)：MainView.fxml 里收银台按钮的 id 是 "
                + "cartBtn，对 null 调用是空操作，会让 F8 进入收银台后导航停在无高亮状态");
        assertFalse(Pattern.compile("private\\s+Button\\s+checkoutBtn\\s*;").matcher(code).find(),
            "MainController 不得再声明 checkoutBtn 字段：MainView.fxml 没有这个 fx:id，字段恒为 null");

        String body = methodBody(source, "public void handleCheckout()");
        assertTrue(body.contains("handleCart()"),
            "handleCheckout 必须委托给 handleCart()，否则 F8 与导航按钮进入的是两套实现"
                + "（历史上就因此漏掉了导航高亮）");
    }

    private static List<Path> fxmlFiles() throws Exception {
        try (Stream<Path> paths = Files.walk(VIEW_DIR)) {
            return paths.filter(path -> path.toString().endsWith(".fxml")).sorted().toList();
        }
    }

    private static String readControllerSource(String fullyQualifiedName) throws Exception {
        Path path = MAIN_SRC.resolve(fullyQualifiedName.replace('.', '/') + ".java");
        return Files.exists(path) ? Files.readString(path) : null;
    }

    /**
     * 去掉注释但保留字符串字面量，避免"注释里提到旧写法"把门禁写成假红/假绿
     * （本门禁第一版就因此误报过一次）。注释字符替换为空格，不改变行号。
     */
    private static String withoutComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        boolean inString = false;
        boolean inChar = false;
        boolean inLine = false;
        boolean inBlock = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (inLine) {
                inLine = c != '\n';
                out.append(c == '\n' ? c : ' ');
            } else if (inBlock) {
                if (c == '*' && next == '/') {
                    inBlock = false;
                    out.append("  ");
                    i++;
                } else {
                    out.append(c == '\n' ? c : ' ');
                }
            } else if (inString || inChar) {
                out.append(c);
                if (c == '\\' && next != '\0') {
                    out.append(next);
                    i++;
                } else if (inString && c == '"') {
                    inString = false;
                } else if (inChar && c == '\'') {
                    inChar = false;
                }
            } else if (c == '/' && next == '/') {
                inLine = true;
                out.append("  ");
                i++;
            } else if (c == '/' && next == '*') {
                inBlock = true;
                out.append("  ");
                i++;
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '\'') {
                    inChar = true;
                }
                out.append(c);
            }
        }
        return out.toString();
    }

    /** 取方法体（按花括号配对），用于断言委托关系。 */
    private static String methodBody(String source, String signature) {        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法签名: " + signature);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new AssertionError("方法体不闭合: " + signature);
    }
}
