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

        // 解析入口只允许出现一次——就在后台任务里；回到 FX 线程就会在恢复大额挂单时卡死界面
        assertEquals(1, countOccurrences(cart, "HoldOrderCodec.parse(order.itemsJson, productDAO)"),
            "标准收银台恢复挂单必须在后台解析");
        assertEquals(1, countOccurrences(touch, "HoldOrderCodec.parse(order.itemsJson, productDAO)"),
            "触屏收银台恢复挂单必须在后台解析");

        assertTrue(cart.contains("private record ResumedOrder(") && touch.contains("private record ResumedHoldOrder("),
            "后台结果应通过不可变结果对象一次性带回 FX 线程");

        // 编解码必须共用同一实现：两端各留一份复制，格式会悄悄分叉
        assertTrue(readMainSource("service/HoldOrderCodec.java").contains("public static List<CartItem> parse("),
            "挂单明细编解码应收敛到 HoldOrderCodec");
        assertFalse(cart.contains("parseCartItems"), "控制器不得再自带一份挂单解析");
        assertFalse(touch.contains("parseHoldCartItems"), "控制器不得再自带一份挂单解析");
        assertFalse(cart.contains("deserializeCartItems"),
            "挂单解析不得再边查库边改 cartList");
        assertFalse(touch.contains("deserializeHoldCartItems"),
            "挂单解析不得再边查库边改 cartItems");
    }

    @Test
    @DisplayName("单行查库（入车刷新库存、会员查询）必须在后台线程执行")
    void singleRowLookupsRunOffTheFxThread() throws Exception {
        String cart = readMainSource("controller/CartController.java");
        String touch = readMainSource("controller/TouchCartController.java");

        // 入车：班次检查与最新库存查询都必须先提交到后台。
        // 回归判据：同步写法里查库与界面改动（下同）在同一个方法体内，后台化后二者被拆开
        String cartAdd = methodBody(cart, "private void addToCart(Product product, int quantity)");
        assertTrue(cartAdd.contains("UIOptimizer.runInBackground("),
            "标准收银台入车前的查库必须作为后台任务提交");
        assertTrue(cartAdd.contains("DataService.hasActiveShift()"),
            "入车前的班次检查也必须放后台（它同样是一次查库）");
        assertTrue(cartAdd.contains("productDAO.findById(product.id)"),
            "最新库存查询必须在后台任务里");
        assertFalse(cartAdd.contains("cartList.add("),
            "入车方法不得再在 FX 线程同步查库后直接改购物车");

        String touchAdd = methodBody(touch, "private void addToCart(Product product)");
        assertTrue(touchAdd.contains("UIOptimizer.runInBackground("),
            "触屏收银台入车前的库存刷新必须作为后台任务提交");
        assertTrue(touchAdd.contains("productDAO.findById(product.id)"),
            "最新库存查询必须在后台任务里");
        assertFalse(touchAdd.contains("refreshCartView()"),
            "入车方法不得再在 FX 线程同步查库后直接刷新购物车");

        // 会员查询
        String cartMember = methodBody(cart, "public void handleSearchMember()");
        assertTrue(cartMember.contains("UIOptimizer.runInBackground("),
            "标准收银台会员查询必须作为后台任务提交");
        assertFalse(cartMember.contains("catch ("),
            "会员查询不得再在 FX 线程同步查库并就地捕获异常");

        String touchMember = methodBody(touch, "private void handleSearchMember()");
        assertTrue(touchMember.contains("UIOptimizer.runInBackground("),
            "触屏收银台会员查询必须作为后台任务提交");
        assertFalse(touchMember.contains("catch ("),
            "会员查询不得再在 FX 线程同步查库并就地捕获异常");
    }

    /** 取出指定方法（按大括号配对）的方法体，便于断言"查库与改界面是否还在同一个方法里"。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法签名: " + signature);
        int open = source.indexOf('{', start + signature.length());
        assertTrue(open >= 0, "方法没有方法体: " + signature);
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
        throw new IllegalStateException("方法体未闭合: " + signature);
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
