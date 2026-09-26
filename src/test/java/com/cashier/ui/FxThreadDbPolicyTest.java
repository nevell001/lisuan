package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Test
    @DisplayName("启动期数据库初始化必须离开 FX 线程，并有有界等待")
    void startupDatabaseInitializationRunsOffTheFxThreadWithBoundedWait() throws Exception {
        String app = readMainSource("CashierSystemFXApplication.java");

        String startPhase = methodBody(app, "private void initializeApplication(");
        assertFalse(startPhase.contains("PaymentService.init()"),
            "建连接池/建表迁移/语言偏好不得再留在 FX 线程：会把启动画面占死，进度文案刷不出来");
        assertTrue(startPhase.contains("STARTUP_DATABASE_THREAD"),
            "数据库阶段必须在具名后台线程上执行");

        String dbPhase = methodBody(app, "private StartupDatabase initializeDatabasePhase(");
        assertTrue(dbPhase.contains("PaymentService.init()"),
            "首次触碰 DatabaseManager（建连接池/建表迁移）应发生在后台的数据库阶段里");

        assertTrue(startPhase.contains("whenComplete("),
            "数据库阶段应以非阻塞回调收口，否则 FX 线程仍会被占住");
        assertFalse(startPhase.contains(".get("),
            "不得在 FX 线程上阻塞等待数据库阶段：启动画面会卡住，看门狗也跑不起来");
        assertTrue(app.contains("STARTUP_DATABASE_TIMEOUT_MS"),
            "等待数据库必须有上限，超时要给出可操作的提示而不是无限期停在启动画面");
        assertTrue(app.contains("已等待"),
            "等待期间要向用户反馈已经等了多久");
    }

    @Test
    @DisplayName("数据库连接超时默认值必须容得下冷启动首连")
    void databaseConnectionTimeoutToleratesColdStart() throws Exception {
        String databaseManager = readMainSource("util/DatabaseManager.java");

        Matcher matcher = Pattern.compile("connectionTimeout = (\\d+)").matcher(databaseManager);
        assertTrue(matcher.find(), "DatabaseManager 必须为 connectionTimeout 设默认值");
        int timeout = Integer.parseInt(matcher.group(1));
        // 本机实测：同一 JVM 内首连 8.5s、后续约 40ms；原来的 5000ms 会让数据库其实正常的机器建池失败
        assertTrue(timeout >= 10_000,
            "db.connection.timeout 默认值过小（当前 " + timeout + "ms）：冷启动首连要好几秒，"
                + "会让数据库其实正常的机器启动时直接报 Communications link failure");

        assertTrue(databaseManager.contains("connectTimeout"),
            "应给 JDBC 设 TCP 建连超时：Connector/J 默认不超时，被丢弃的路由会让启动一直挂住");
    }

    @Test
    @DisplayName("报表的后台查库方法必须作为后台任务提交，且只被调用一次（TD-006 第二批）")
    void reportLoadersAreOnlyCalledAsBackgroundTask() throws Exception {
        // 每行：源码文件、后台 loader 方法签名、该方法内的查库语句、调用它的后台任务写法
        String[][] loaders = {
            {"controller/InventoryReportController.java",
                "private ReportData loadReportData(String categoryName, LocalDate startDate, LocalDate endDate)",
                "getTransactionDAO().findByDateRange(",
                "() -> loadReportData(categoryName, startDate, endDate)"},
            {"controller/PurchaseReportController.java",
                "private ReportData loadReportData(LocalDate startDate, LocalDate endDate)",
                "getPurchaseOrderItemDAO().findByOrderIds(",
                "() -> loadReportData(startDate, endDate)"},
        };

        StringBuilder failures = new StringBuilder();
        for (String[] loader : loaders) {
            String source = readMainSource(loader[0]);
            String body = methodBody(source, loader[1]);
            String call = loader[3];
            if (!body.contains(loader[2])) {
                failures.append(loader[0]).append(": ").append(loader[1]).append(" 里没有查库语句; ");
            }
            if (!source.contains(call)) {
                failures.append(loader[0]).append(": 后台查库必须写成 ").append(call).append("; ");
            } else if (countOccurrences(source, call) != 1) {
                failures.append(loader[0]).append(": 后台任务只应提交一次; ");
            }
        }
        assertTrue(failures.isEmpty(),
            "以下报表查库没有走后台入口（生成报表会冻结界面）: " + failures);
    }

    @Test
    @DisplayName("利润报表不得在 FX 线程渲染时查库（运营成本比例由 worker 预取）")
    void profitReportDoesNotQuerySettingsOnTheFxThread() throws Exception {
        String source = readMainSource("controller/ProfitReportController.java");

        // calculateStatistics 在 Platform.runLater 里执行，属于 FX 线程渲染路径
        String render = methodBody(source, "private void calculateStatistics(LocalDate startDate, LocalDate endDate, String categoryName,");
        assertFalse(render.contains("loadOperatingCostRatio()"),
            "运营成本比例必须在 worker 里取好再传入，不能在 FX 线程渲染时查设置表");

        // worker 里应先取好比例，再提交渲染
        int fetch = source.indexOf("final double operatingCostRatio = loadOperatingCostRatio();");
        int renderLater = source.indexOf("calculateStatistics(startDate, endDate, selectedCategory, operatingCostRatio)");
        assertTrue(fetch > 0, "worker 里应先取好运营成本比例");
        assertTrue(renderLater > fetch, "渲染必须使用 worker 取好的比例");
    }

    @Test
    @DisplayName("非收银台页面的加载/查询也必须离开 FX 线程（TD-006）")
    void nonPosPageLoadsRunOffTheFxThread() throws Exception {
        // 每行：源码文件、方法签名、必须出现在该方法里的查库语句
        String[][] pageLoads = {
            {"controller/InventoryController.java", "protected void loadTableData()",
                "productDAO.findAll(FIRST_PAGE, DESKTOP_PAGE_SIZE)"},
            {"controller/InventoryController.java", "private void loadCategories()",
                "getCategoryDAO().findAll()"},
            {"controller/SupplierController.java", "private void loadSuppliers()",
                "getSupplierDAO().findRecent(SUPPLIER_LIST_LIMIT)"},
            {"controller/SupplierController.java", "public void handleSearch()",
                "getSupplierDAO().search(searchText, SUPPLIER_LIST_LIMIT)"},
            {"controller/PromotionController.java", "private void loadPromotions()",
                "DataService::loadPromotions"},
            {"controller/TransactionController.java", "private void loadTransactions()",
                "getTransactionDAO().findByDateRange(range.start(), range.end())"},
            {"controller/TransactionController.java", "private void applyFilters()",
                "getTransactionDAO().findByDateRange(range.start(), range.end())"},
            {"controller/ShiftController.java", "private void loadShifts()",
                "getShiftDAO().findRecent(SHIFT_HISTORY_LIMIT)"},
            {"controller/PurchaseOrderController.java", "private void loadSuppliers()",
                "getSupplierDAO().findByStatus(true, PURCHASE_SUPPLIER_LIMIT)"},
            {"controller/PurchaseOrderController.java", "private void loadOrders()",
                "getPurchaseOrderDAO().findRecent(PURCHASE_ORDER_LIMIT)"},
            {"controller/PurchaseApprovalController.java", "private void loadPendingOrders()",
                "findByStatus(\"pending\")"},
            {"controller/PurchaseApprovalController.java", "private void loadAllOrders()",
                "findRecent(APPROVAL_ORDER_LIMIT)"},
            {"controller/PurchaseApprovalController.java", "private void updateCountLabel()",
                "countByStatus(\"pending\")"},
            {"controller/PurchaseInboundController.java", "private void loadApprovedOrders()",
                "findByStatus(\"approved\")"},
            {"controller/ProductEditController.java", "private void loadCategories()",
                "getCategoryDAO().findAll()"},
            {"controller/ProductEditController.java", "private void loadUnits()",
                "getUnitDAO().findAll()"},
            {"controller/ProductEditController.java", "private void loadSuppliers()",
                "findByStatus(true, PRODUCT_SUPPLIER_LIMIT)"},
            {"controller/SearchController.java", "private void performSearch(String query)",
                "SearchManager.search(query, 10)"},
            {"controller/InventoryReportController.java",
                "private void calculateStatistics(LocalDate startDate, LocalDate endDate, String categoryName,",
                "() -> loadReportData(categoryName, startDate, endDate)"},
            {"controller/PurchaseReportController.java", "public void handleQuery()",
                "() -> loadReportData(startDate, endDate)"},
            {"controller/PurchaseReportController.java", "private void loadData()",
                "getSupplierDAO().findRecent(PURCHASE_REPORT_SUPPLIER_LIMIT)"},
            {"controller/ReturnApprovalController.java", "private void loadOrderItems(String returnOrderId)",
                "getReturnOrderItemDAO().findByReturnOrderId(returnOrderId)"},
            {"controller/ReturnOrderController.java", "private void loadReturnOrderItems(String returnOrderId)",
                "getReturnOrderItemDAO().findByReturnOrderId(returnOrderId)"},
            {"controller/SupplierController.java",
                "private void applyGeneratedSupplierCode(TextField codeField)",
                "countBySupplierCodePrefix(prefix)"},
            {"controller/InventoryController.java",
                "private void loadCategoryManagementData(ObservableList<Category> target)",
                "getCategoryDAO().findAll()"},
            {"controller/InventoryController.java",
                "private void loadUnitManagementData(ObservableList<Unit> target)",
                "getUnitDAO().findAll()"},
            {"controller/MemberController.java", "public void handleSearch()",
                "getMemberDAO().search(searchText, FIRST_PAGE, DESKTOP_PAGE_SIZE)"},
            {"controller/ShiftController.java", "private void updateShiftButtonStates()",
                "getShiftDAO().hasActiveShift()"},
        };

        StringBuilder failures = new StringBuilder();
        for (String[] load : pageLoads) {
            String body = methodBody(readMainSource(load[0]), load[1]);
            if (!body.contains("UIOptimizer.runInBackground(")) {
                failures.append(load[0]).append(' ').append(load[1]).append(": 未走后台执行入口; ");
            } else if (!body.contains(load[2])) {
                failures.append(load[0]).append(' ').append(load[1]).append(": 查库语句已不在该方法里; ");
            } else if (body.contains("catch (SQLException")) {
                failures.append(load[0]).append(' ').append(load[1])
                    .append(": 仍是同步 try/catch 查库; ");
            }
        }
        assertTrue(failures.isEmpty(),
            "以下页面加载仍在 FX 线程同步查库（打开页面会冻结界面）: " + failures);
    }

    /** 取出指定方法（按大括号配对）的方法体，便于断言"查库与改界面是否还在同一个方法里"。 */
    private static String methodBody(String source, String signature) {        int start = source.indexOf(signature);
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
