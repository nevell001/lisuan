package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 收银台 FX 线程纪律门禁。
 *
 * <p>回归：标准收银台在 {@code initialize()} 里同步查库（一次加载 500 个商品），
 * 触屏收银台在初始化时同步加载分类与全部商品，都会把 JavaFX 应用线程整个卡住，
 * 扫码/按键无响应。库存加载、分类加载与搜索必须走后台线程，查询结果回到 FX 线程刷新界面。</p>
 */
class FxThreadDbPolicyTest {

    private static String readMainSource(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/com/cashier", relativePath));
    }

    @Test
    @DisplayName("后台执行入口统一在 UIOptimizer，且结果回到 FX 线程")
    void sharedBackgroundHelperDispatchesToFxThread() throws Exception {
        String uiOptimizer = readMainSource("util/UIOptimizer.java");

        assertTrue(uiOptimizer.contains("public static <T> void runInBackground(Callable<T> work"),
            "查库→刷新界面这类后台任务应有统一入口 UIOptimizer.runInBackground");
        assertTrue(uiOptimizer.contains("Platform.runLater(() -> {"),
            "后台结果必须回到 JavaFX 应用线程后再更新界面");
    }

    @Test
    @DisplayName("标准收银台库存加载与搜索必须在后台线程执行")
    void cartControllerLoadsDataOffTheFxThread() throws Exception {
        String cart = readMainSource("controller/CartController.java");

        assertTrue(cart.contains("UIOptimizer.runInBackground("),
            "收银台查库必须走后台执行入口");
        assertTrue(cart.contains("() -> productDAO.findAll(FIRST_PAGE, CART_PRODUCT_PAGE_SIZE).getData()"),
            "库存加载的数据库查询必须作为后台任务提交");
        assertTrue(cart.contains("() -> searchProducts(searchText)"),
            "商品搜索的数据库查询必须作为后台任务提交");
        assertTrue(cart.contains("productQuerySequence"),
            "并发/连续搜索需要序号保护，避免过期结果覆盖新结果");

        // 曾经的同步写法，一旦回归就会重新冻结界面
        assertFalse(cart.contains("List<Product> products = productDAO.findAll(FIRST_PAGE, CART_PRODUCT_PAGE_SIZE).getData();"),
            "库存加载不得在 FX 线程同步查库");
        assertFalse(cart.contains("matchedProducts = searchProducts(searchText);"),
            "搜索不得在 FX 线程同步查库");
    }

    @Test
    @DisplayName("触屏收银台分类/商品加载与搜索必须在后台线程执行")
    void touchCartControllerLoadsDataOffTheFxThread() throws Exception {
        String touch = readMainSource("controller/TouchCartController.java");

        assertTrue(touch.contains("UIOptimizer.runInBackground("),
            "触屏收银台查库必须走后台执行入口");
        assertTrue(touch.contains("() -> DAOFactory.getInstance().getCategoryDAO().findAll()"),
            "分类加载必须作为后台任务提交");
        assertTrue(touch.contains("() -> findExactProduct(keyword)"),
            "实时精确匹配必须作为后台任务提交");
        assertTrue(touch.contains("() -> findExactProduct(trimmed)"),
            "回车匹配与“未找到”检查必须作为后台任务提交");
        assertTrue(touch.contains("() -> DAOFactory.getInstance().getShiftDAO().findActiveShift()"),
            "班次信息查询必须作为后台任务提交");
        assertTrue(touch.contains("productQuerySequence"),
            "切换分类/搜索需要序号保护，避免过期结果覆盖新结果");

        // 曾经的同步写法与同步助手，一旦回归就会重新冻结界面
        assertFalse(touch.contains("List<Category> cats = DAOFactory.getInstance().getCategoryDAO().findAll();"),
            "分类加载不得在 FX 线程同步查库");
        assertFalse(touch.contains("products = productDAO.findAll();"),
            "商品加载不得在 FX 线程同步查库");
        assertFalse(touch.contains("private boolean tryAddExactMatch("),
            "精确匹配助手不得再于 FX 线程同步查库");
        assertFalse(touch.contains("loadProducts(null);"),
            "初始化不得再额外同步加载一次全部商品（初始列表由默认选中的“热销推荐”驱动）");
    }

    @Test
    @DisplayName("恢复挂单时逐条查库必须在后台线程执行")
    void holdOrderRestoreLoadsOffTheFxThread() throws Exception {
        String cart = readMainSource("controller/CartController.java");
        String touch = readMainSource("controller/TouchCartController.java");

        // 解析助手只允许出现在「定义处 + 后台任务里」两处：别处调用就说明又在 FX 线程查库了
        assertEquals(2, countOccurrences(cart, "parseCartItems("),
            "CartController 的挂单解析只应在定义处与后台任务中出现");
        assertEquals(2, countOccurrences(touch, "parseHoldCartItems("),
            "TouchCartController 的挂单解析只应在定义处与后台任务中出现");

        assertTrue(cart.contains("parseCartItems(order.itemsJson)"),
            "标准收银台恢复挂单必须在后台解析");
        assertTrue(touch.contains("parseHoldCartItems(order.itemsJson)"),
            "触屏收银台恢复挂单必须在后台解析");
        assertTrue(cart.contains("private record ResumedOrder(") && touch.contains("private record ResumedHoldOrder("),
            "后台结果应通过不可变结果对象一次性带回 FX 线程");

        // 曾经的同步写法：边解析边直接改 UI 列表
        assertFalse(cart.contains("deserializeCartItems"),
            "挂单解析不得再边查库边改 cartList");
        assertFalse(touch.contains("deserializeHoldCartItems"),
            "挂单解析不得再边查库边改 cartItems");
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int idx = text.indexOf(needle);
        while (idx >= 0) {
            count++;
            idx = text.indexOf(needle, idx + needle.length());
        }
        return count;
    }
}
