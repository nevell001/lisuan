package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformancePolicyTest {

    @Test
    @DisplayName("利润报表加载入库明细不得逐单查询")
    void profitReportLoadsInboundItemsInBatch() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ProfitReportController.java"
        ));
        String dao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/PurchaseInboundItemDAORefactored.java"
        ));

        assertTrue(controller.contains("getPurchaseInboundItemDAO().findAverageUnitCostByProductId()"));
        assertTrue(controller.contains("productDAO.findByNames(productNames)"));
        assertFalse(controller.contains("getPurchaseInboundDAO().findAll()"));
        assertFalse(controller.contains("getPurchaseInboundItemDAO().findByInboundIds("));
        assertFalse(controller.contains("allProducts = productDAO.findAll()"));
        assertTrue(dao.contains("findByInboundIds(Collection<Integer> inboundIds)"));
        assertTrue(dao.contains("findAverageUnitCostByProductId()"));
        assertTrue(dao.contains("GROUP BY product_id"));
        assertFalse(controller.contains("getPurchaseInboundItemDAO().findByInboundId(inbound.id)"));
    }

    @Test
    @DisplayName("同步最近交易必须在数据库侧限制数量")
    void syncRecentTransactionsUsesDatabaseLimit() throws Exception {
        String syncManager = Files.readString(Path.of(
            "src/main/java/com/cashier/api/sync/SyncManager.java"
        ));
        String transactionDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/TransactionDAORefactored.java"
        ));

        assertTrue(syncManager.contains("getTransactionDAO().findRecent(100)"));
        assertTrue(transactionDao.contains("findRecent(int limit)"));
        assertTrue(transactionDao.contains("ORDER BY timestamp DESC LIMIT ?"));
        assertFalse(syncManager.contains("getTransactionDAO().findAll()"));
        assertFalse(syncManager.contains("transactions.subList("));
    }

    @Test
    @DisplayName("同步商品和会员必须分页返回")
    void syncProductsAndMembersUsePagedQueries() throws Exception {
        String syncManager = Files.readString(Path.of(
            "src/main/java/com/cashier/api/sync/SyncManager.java"
        ));
        String memberDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/MemberDAORefactored.java"
        ));

        assertTrue(syncManager.contains("findAll(page, pageSize)"));
        assertTrue(syncManager.contains("putPageResult(responseData"));
        assertTrue(syncManager.contains("MAX_SYNC_PAGE_SIZE"));
        assertTrue(memberDao.contains("PageResult<Member> findAll(int pageNum, int pageSize)"));
        assertFalse(syncManager.contains("getProductDAO().findAll())"));
        assertFalse(syncManager.contains("getMemberDAO().findAll())"));
    }

    @Test
    @DisplayName("统计页必须按日期范围查询交易")
    void statisticsReportQueriesTransactionsByDateRange() throws Exception {
        String statisticsController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/StatisticsController.java"
        ));

        assertTrue(statisticsController.contains("getTransactionDAO().findByDateRange("));
        assertTrue(statisticsController.contains("DateTimeFormats.STANDARD_DATE_TIME"));
        assertFalse(statisticsController.contains("getTransactionDAO().findAll()"));
        assertFalse(statisticsController.contains("filterTransactionsByDate("));
    }

    @Test
    @DisplayName("采购报表加载订单明细不得逐单查询")
    void purchaseReportLoadsOrderItemsInBatch() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseReportController.java"
        ));
        String dao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/PurchaseOrderItemDAORefactored.java"
        ));

        assertTrue(controller.contains("getPurchaseOrderItemDAO().findByOrderIds("));
        assertTrue(dao.contains("findByOrderIds(Collection<Integer> orderIds)"));
        assertFalse(controller.contains("getPurchaseOrderItemDAO().findByOrderId(order.id)"));
    }

    @Test
    @DisplayName("库存报表必须按日期范围查询交易并预聚合销量")
    void inventoryReportQueriesTransactionsByDateRangeAndAggregatesSales() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryReportController.java"
        ));

        assertTrue(controller.contains("getTransactionDAO().findByDateRange("));
        assertTrue(controller.contains("allProducts = new ArrayList<>()"));
        assertTrue(controller.contains("INVENTORY_REPORT_PRODUCT_LIMIT = 5000"));
        assertTrue(controller.contains("loadProductsForReport(categoryName)"));
        assertTrue(controller.contains("productDAO.findByCategory("));
        assertTrue(controller.contains("FIRST_PAGE"));
        assertTrue(controller.contains("productDAO.findAll(FIRST_PAGE, INVENTORY_REPORT_PRODUCT_LIMIT)"));
        assertTrue(controller.contains("loadAllCategoryNames()"));
        assertTrue(controller.contains("buildSalesStatsMap()"));
        assertTrue(controller.contains("SalesStats"));
        assertFalse(controller.contains("getTransactionDAO().findAll()"));
        assertFalse(controller.contains("allProducts = productDAO.findAll()"));
        assertFalse(controller.contains("productDAO.findByCategory(categoryName);"));
        assertFalse(controller.contains("calculateSalesQuantity("));
        assertFalse(controller.contains("getLastSaleDate("));
    }

    @Test
    @DisplayName("交易 API 列表和今日统计不得全量拉取交易")
    void transactionApiUsesBoundedQueries() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/TransactionApiController.java"
        ));

        assertTrue(controller.contains("getTransactionDAO().findRecent(limit)"));
        assertTrue(controller.contains("getTransactionDAO().findByDateRange("));
        assertFalse(controller.contains("List<Transaction> transactions = TransactionDAO.findAll();"));
    }

    @Test
    @DisplayName("日报和月报 API 必须按日期范围查询交易")
    void reportApiDailyAndMonthlyUseDateRangeQueries() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/ReportApiController.java"
        ));

        assertTrue(controller.contains("getTransactionDAO().findByDateRange("));
    }

    @Test
    @DisplayName("商品排行和支付方式 API 必须使用数据库聚合")
    void reportApiRankingAndPaymentStatsUseDatabaseAggregation() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/ReportApiController.java"
        ));
        String dao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/TransactionDAORefactored.java"
        ));

        assertTrue(controller.contains("getTransactionDAO().getTopProducts(limit)"));
        assertTrue(controller.contains("getTransactionDAO().getPaymentMethodStats()"));
        assertTrue(dao.contains("getTopProducts(int limit)"));
        assertTrue(dao.contains("getPaymentMethodStats()"));
        assertFalse(controller.contains("getTransactionDAO().findAll()"));
    }

    @Test
    @DisplayName("商品会员库存列表 API 必须分页查询")
    void coreListApisUsePagedQueries() throws Exception {
        String productApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/ProductApiController.java"
        ));
        String memberApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/MemberApiController.java"
        ));
        String inventoryApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/InventoryApiController.java"
        ));
        String pagination = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/ApiPagination.java"
        ));
        String productDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/ProductDAORefactored.java"
        ));

        assertTrue(productApi.contains("ApiPagination.from(ctx)"));
        assertTrue(productApi.contains("productDAO.findAll(page.page(), page.pageSize())"));
        assertTrue(productApi.contains("productDAO.search(keyword, page.page(), page.pageSize())"));
        assertTrue(productApi.contains("productDAO.findByCategory(category, page.page(), page.pageSize())"));
        assertFalse(productApi.contains("productDAO.findAll()"));
        assertFalse(productApi.contains("productDAO.search(keyword)"));

        assertTrue(memberApi.contains("getMemberDAO().findAll(page.page(), page.pageSize())"));
        assertFalse(memberApi.contains("getMemberDAO().findAll()"));

        assertTrue(inventoryApi.contains("productDAO.findAll(page.page(), page.pageSize())"));
        assertTrue(inventoryApi.contains("productDAO.findLowStock(page.page(), page.pageSize())"));
        assertTrue(inventoryApi.contains("productDAO.getInventorySummary()"));
        assertFalse(inventoryApi.contains("productDAO.findAll()"));

        assertTrue(pagination.contains("MAX_PAGE_SIZE = 500"));
        assertTrue(productDao.contains("findByCategory(String category, int pageNum, int pageSize)"));
        assertTrue(productDao.contains("findLowStock(int pageNum, int pageSize)"));
        assertTrue(productDao.contains("getInventorySummary()"));
    }

    @Test
    @DisplayName("用户列表 API 必须分页查询")
    void userApiUsesPagedQuery() throws Exception {
        String userApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/UserApiController.java"
        ));
        String userDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/UserDAORefactored.java"
        ));

        assertTrue(userApi.contains("ApiPagination.from(ctx)"));
        assertTrue(userApi.contains("getUserDAO().findAll(page.page(), page.pageSize())"));
        assertTrue(userApi.contains("ApiPagination.success(users)"));
        assertFalse(userApi.contains("UserDAO.findAll()"));

        assertTrue(userDao.contains("PageResult<User> findAll(int pageNum, int pageSize)"));
        assertTrue(userDao.contains("ORDER BY username LIMIT ? OFFSET ?"));
        assertTrue(userDao.contains("long total = count()"));
    }

    @Test
    @DisplayName("桌面用户列表必须有界加载")
    void desktopUserListUsesPagedQuery() throws Exception {
        String userController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/UserController.java"
        ));

        assertTrue(userController.contains("USER_LIST_PAGE_SIZE = 500"));
        assertTrue(userController.contains("getUserDAO().findAll(FIRST_PAGE, USER_LIST_PAGE_SIZE)"));
        assertFalse(userController.contains("List<User> userListData = UserDAO.findAll()"));
    }

    @Test
    @DisplayName("发票列表 API 必须分页查询")
    void invoiceApiUsesPagedQuery() throws Exception {
        String invoiceApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/InvoiceApiController.java"
        ));
        String invoiceService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/InvoiceService.java"
        ));
        String invoiceDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/InvoiceDAORefactored.java"
        ));

        assertTrue(invoiceApi.contains("ApiPagination.from(ctx)"));
        assertTrue(invoiceApi.contains("InvoiceService.getInvoicesPage("));
        assertTrue(invoiceApi.contains("ApiPagination.success(invoices)"));
        assertFalse(invoiceApi.contains("InvoiceService.getAllInvoices()"));

        assertTrue(invoiceService.contains("DEFAULT_INVOICE_LIST_LIMIT = 5000"));
        assertTrue(invoiceService.contains("getInvoicesPage(null, null, null, FIRST_PAGE, DEFAULT_INVOICE_LIST_LIMIT).getData()"));
        assertFalse(invoiceService.contains("return InvoiceDAO.findAll()"));
        assertTrue(invoiceService.contains("PageResult<Invoice> getInvoicesPage("));
        assertTrue(invoiceDao.contains("PageResult<Invoice> findPage("));
        assertTrue(invoiceDao.contains("ORDER BY create_time DESC LIMIT ? OFFSET ?"));
        assertTrue(invoiceDao.contains("SELECT COUNT(*) FROM invoices"));
    }

    @Test
    @DisplayName("打印历史必须有界保留并限制 API 返回数量")
    void printHistoryIsBounded() throws Exception {
        String printerManager = Files.readString(Path.of(
            "src/main/java/com/cashier/printer/PrinterManager.java"
        ));
        String printApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/PrintApiController.java"
        ));

        assertTrue(printerManager.contains("MAX_PRINT_HISTORY_SIZE = 500"));
        assertTrue(printerManager.contains("new ConcurrentLinkedQueue<>()"));
        assertTrue(printerManager.contains("Collections.synchronizedList(new ArrayList<>())"));
        assertTrue(printerManager.contains("getRecentPrintHistory(int limit)"));
        assertTrue(printerManager.contains("printHistory.remove(0)"));
        assertFalse(printerManager.contains("new LinkedList<>()"));

        assertTrue(printApi.contains("DEFAULT_PRINT_HISTORY_LIMIT = 100"));
        assertTrue(printApi.contains("MAX_PRINT_HISTORY_LIMIT = 500"));
        assertTrue(printApi.contains("manager.getRecentPrintHistory(limit)"));
        assertFalse(printApi.contains("manager.getPrintHistory()"));
    }

    @Test
    @DisplayName("网络打印机发现必须限制扫描范围和超时")
    void printerDiscoveryIsBounded() throws Exception {
        String printApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/PrintApiController.java"
        ));

        assertTrue(printApi.contains("DEFAULT_DISCOVERY_HOST_LIMIT = 64"));
        assertTrue(printApi.contains("MAX_DISCOVERY_HOST_LIMIT = 254"));
        assertTrue(printApi.contains("DEFAULT_DISCOVERY_TIMEOUT_MS = 150"));
        assertTrue(printApi.contains("MAX_DISCOVERY_TIMEOUT_MS = 1000"));
        // 只扫本机子网、只探标准打印端口：不接受调用方指定子网与任意端口，避免变成内网端口扫描器
        assertTrue(printApi.contains("String subnet = getDefaultSubnet()"));
        assertTrue(printApi.contains("STANDARD_PRINTER_PORTS"));
        assertFalse(printApi.contains("ctx.queryParam(\"subnet\")"));
        assertFalse(printApi.contains("Math.min(requestedPort, 65535)"));
        assertTrue(printApi.contains("for (int i = 1; i <= hostLimit; i++)"));
        assertTrue(printApi.contains("checkPrinterPort(host, port, timeoutMs)"));
        assertFalse(printApi.contains("for (int i = 1; i < 255; i++)"));
        assertFalse(printApi.contains("socket.connect(new InetSocketAddress(host, port), 500)"));
    }

    @Test
    @DisplayName("外部 API limit 参数必须设置上限")
    void publicApiLimitParametersAreCapped() throws Exception {
        String transactionApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/TransactionApiController.java"
        ));
        String reportApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/ReportApiController.java"
        ));
        String backupApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/BackupApiController.java"
        ));
        String paymentApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/PaymentApiController.java"
        ));
        String paymentDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/PaymentDAORefactored.java"
        ));

        assertTrue(transactionApi.contains("MAX_TRANSACTION_LIST_LIMIT = 500"));
        assertTrue(transactionApi.contains("Math.max(1, Math.min(requestedLimit, MAX_TRANSACTION_LIST_LIMIT))"));

        assertTrue(reportApi.contains("MAX_TOP_PRODUCTS_LIMIT = 100"));
        assertTrue(reportApi.contains("Math.max(1, Math.min(requestedLimit, MAX_TOP_PRODUCTS_LIMIT))"));

        assertTrue(backupApi.contains("MAX_BACKUP_LIST_LIMIT = 200"));
        assertTrue(backupApi.contains("Math.max(1, Math.min(requestedLimit, MAX_BACKUP_LIST_LIMIT))"));

        assertTrue(paymentApi.contains("MAX_WAITING_PAYMENT_LIMIT = 500"));
        assertTrue(paymentApi.contains("getPaymentDAO().findWaitingOrders(limit)"));
        assertTrue(paymentDao.contains("findWaitingOrders(int limit)"));
        assertTrue(paymentDao.contains("ORDER BY create_time DESC LIMIT ?"));
    }

    @Test
    @DisplayName("备份下载必须限制在备份目录内")
    void backupDownloadIsRestrictedToBackupDirectory() throws Exception {
        String backupApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/BackupApiController.java"
        ));

        assertTrue(backupApi.contains("resolveDownloadableBackupFile(BackupRecord record)"));
        assertTrue(backupApi.contains("backupFile.startsWith(backupRoot)"));
        assertTrue(backupApi.contains("Files.isRegularFile(backupFile)"));
        assertTrue(backupApi.contains("sanitizeDownloadFileName(record.fileName)"));
        assertFalse(backupApi.contains("new File(record.localPath)"));
        assertFalse(backupApi.contains("\"error\", \"文件不存在: \" + record.localPath"));
    }

    @Test
    @DisplayName("启动缓存预热必须有界加载商品")
    void cacheWarmupUsesBoundedProductLoad() throws Exception {
        String cacheManager = Files.readString(Path.of(
            "src/main/java/com/cashier/util/CacheManager.java"
        ));
        String app = Files.readString(Path.of(
            "src/main/java/com/cashier/CashierSystemFXApplication.java"
        ));

        assertTrue(app.contains("CacheManager.warmupCache()"));
        assertTrue(cacheManager.contains("MAX_CACHE_SIZE = 5000"));
        // L-3: 分页加载全部商品，每页 MAX_CACHE_SIZE
        assertTrue(cacheManager.contains("findAll(page, pageSize)"));
        assertTrue(cacheManager.contains("pageSize = MAX_CACHE_SIZE"));
        assertFalse(cacheManager.contains("getProductDAO().findAll()"));
    }

    @Test
    @DisplayName("安装器等待 Docker MySQL 必须轮询就绪状态")
    void installerWaitsForMysqlReadinessInsteadOfFixedSleep() throws Exception {
        String installer = Files.readString(Path.of(
            "src/main/java/com/cashier/installer/Installer.java"
        ));

        assertTrue(installer.contains("MYSQL_READY_TIMEOUT_SECONDS = 60"));
        assertTrue(installer.contains("COMMAND_CHECK_TIMEOUT_SECONDS = 10"));
        assertTrue(installer.contains("INSTALL_COMMAND_TIMEOUT_SECONDS = 10 * 60"));
        assertTrue(installer.contains("waitForDockerMysqlReady()"));
        assertTrue(installer.contains("mysqladmin ping"));
        assertTrue(installer.contains("commandSucceeds(command"));
        assertTrue(installer.contains("readProcessOutputAsync(process, charset)"));
        assertTrue(installer.contains("waitForProcess(process, command, INSTALL_COMMAND_TIMEOUT_SECONDS)"));
        assertFalse(installer.contains("Thread.sleep(10000)"));
        assertFalse(installer.contains("process.waitFor();"));
    }

    @Test
    @DisplayName("数据库备份恢复外部命令必须设置超时")
    void databaseBackupRestoreCommandsUseTimeouts() throws Exception {
        String databaseManager = Files.readString(Path.of(
            "src/main/java/com/cashier/util/DatabaseManager.java"
        ));

        assertTrue(databaseManager.contains("DATABASE_COMMAND_TIMEOUT_SECONDS = 30 * 60"));
        assertTrue(databaseManager.contains("DOCKER_STATUS_TIMEOUT_SECONDS = 10"));
        assertTrue(databaseManager.contains("waitForProcess(process, \"Docker 数据库备份\", DATABASE_COMMAND_TIMEOUT_SECONDS)"));
        assertTrue(databaseManager.contains("waitForProcess(copyProcess, \"复制 Docker 数据库备份\", DATABASE_COMMAND_TIMEOUT_SECONDS)"));
        assertTrue(databaseManager.contains("waitForProcess(process, \"本地数据库备份\", DATABASE_COMMAND_TIMEOUT_SECONDS)"));
        assertTrue(databaseManager.contains("waitForProcess(process, \"Docker 数据库恢复\", DATABASE_COMMAND_TIMEOUT_SECONDS)"));
        assertTrue(databaseManager.contains("waitForProcess(process, \"本地数据库恢复\", DATABASE_COMMAND_TIMEOUT_SECONDS)"));
        assertTrue(databaseManager.contains("process.waitFor(timeoutSeconds, TimeUnit.SECONDS)"));
        assertFalse(databaseManager.contains("int exitCode = process.waitFor();"));
        assertFalse(databaseManager.contains("int copyExitCode = copyProcess.waitFor();"));
    }

    @Test
    @DisplayName("打包向导外部命令必须设置超时")
    void packageWizardCommandsUseTimeouts() throws Exception {
        String packageWizard = Files.readString(Path.of(
            "src/main/java/com/cashier/packager/PackageWizardController.java"
        ));

        assertTrue(packageWizard.contains("TOOL_LOOKUP_TIMEOUT_SECONDS = 10"));
        assertTrue(packageWizard.contains("PACKAGE_COMMAND_TIMEOUT_SECONDS = 10 * 60"));
        assertTrue(packageWizard.contains("POWERSHELL_PACKAGE_TIMEOUT_SECONDS = 30 * 60"));
        assertTrue(packageWizard.contains("logProcessOutputAsync(compileProcess"));
        assertTrue(packageWizard.contains("waitForProcess(compileProcess, \"Maven 编译\", PACKAGE_COMMAND_TIMEOUT_SECONDS)"));
        assertTrue(packageWizard.contains("waitForProcess(packageProcess, \"Maven 打包\", PACKAGE_COMMAND_TIMEOUT_SECONDS)"));
        assertTrue(packageWizard.contains("waitForProcess(jlinkProcess, \"jlink 创建 JRE\", PACKAGE_COMMAND_TIMEOUT_SECONDS)"));
        assertTrue(packageWizard.contains("waitForProcess(psProcess, \"PowerShell 打包\", POWERSHELL_PACKAGE_TIMEOUT_SECONDS)"));
        assertFalse(packageWizard.contains(".waitFor()"));
        assertFalse(packageWizard.contains("logProcessOutput("));
    }

    @Test
    @DisplayName("打包向导取消时必须释放后台任务线程")
    void packageWizardCancelReleasesExecutor() throws Exception {
        String packageWizard = Files.readString(Path.of(
            "src/main/java/com/cashier/packager/PackageWizardController.java"
        ));

        assertTrue(packageWizard.contains("shutdownExecutor()"));
        assertTrue(packageWizard.contains("executorService.shutdownNow()"));
        assertTrue(packageWizard.contains("javafx.application.Platform.exit()"));
        assertFalse(packageWizard.contains("System.exit(0)"));
    }

    @Test
    @DisplayName("登出后可重启的后台服务不能永久关闭线程池")
    void logoutKeepsReusableBackgroundServicesRestartable() throws Exception {
        String app = Files.readString(Path.of(
            "src/main/java/com/cashier/CashierSystemFXApplication.java"
        ));
        String logoutMethod = app.substring(
            app.indexOf("public void logoutToLoginView()"),
            app.indexOf("public Stage getPrimaryStage()")
        );
        String alertService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/InventoryAlertService.java"
        ));

        assertTrue(alertService.contains("private ScheduledExecutorService scheduler"));
        assertTrue(alertService.contains("createScheduler()"));
        assertTrue(alertService.contains("scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()"));
        assertFalse(alertService.contains("private final ScheduledExecutorService scheduler"));

        assertFalse(logoutMethod.contains("NotificationManager.getInstance().shutdown()"));
        assertFalse(logoutMethod.contains("UIOptimizer.shutdown()"));
    }

    @Test
    @DisplayName("UI 优化器关闭时必须释放所有后台线程池")
    void uiOptimizerShutdownReleasesAllExecutors() throws Exception {
        String uiOptimizer = Files.readString(Path.of(
            "src/main/java/com/cashier/util/UIOptimizer.java"
        ));

        assertTrue(uiOptimizer.contains("private static final ScheduledExecutorService cleanupExecutor"));
        assertTrue(uiOptimizer.contains("shutdownExecutor(asyncExecutor, \"UI优化线程池\")"));
        assertTrue(uiOptimizer.contains("shutdownExecutor(cleanupExecutor, \"UI缓存清理线程池\")"));
        assertTrue(uiOptimizer.contains("executor.awaitTermination(5, TimeUnit.SECONDS)"));
        assertTrue(uiOptimizer.contains("ConcurrentHashMap<String, CacheEntry> cache"));
        assertTrue(uiOptimizer.contains("class CacheEntry"));
        assertFalse(uiOptimizer.contains("java.util.concurrent.ScheduledExecutorService cleanupExecutor"));
    }

    @Test
    @DisplayName("通知队列必须支持跨线程生产消费")
    void notificationQueueIsThreadSafe() throws Exception {
        String notificationManager = Files.readString(Path.of(
            "src/main/java/com/cashier/notification/NotificationManager.java"
        ));

        assertTrue(notificationManager.contains("new ConcurrentLinkedQueue<>()"));
        assertFalse(notificationManager.contains("new LinkedList<>()"));
    }

    @Test
    @DisplayName("桌面 UI 退出必须走 JavaFX 生命周期清理")
    void desktopExitUsesJavaFxLifecycleCleanup() throws Exception {
        String app = Files.readString(Path.of(
            "src/main/java/com/cashier/CashierSystemFXApplication.java"
        ));
        String loginController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/LoginController.java"
        ));
        String mainController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MainController.java"
        ));

        assertTrue(app.contains("public void requestExit()"));
        assertTrue(app.contains("public void exitApplication()"));
        assertTrue(app.contains("public void stop()"));
        assertTrue(app.contains("shutdown();"));
        assertTrue(app.contains("javafx.application.Platform.exit()"));

        assertTrue(loginController.contains("application.requestExit()"));
        // 主界面退出与触屏版一致：确认后回到登录界面（应用保持运行），不直接关闭
        assertTrue(mainController.contains("application.logoutToLoginView()"));

        assertFalse(app.contains("System.exit(0)"));
        assertFalse(loginController.contains("System.exit(0)"));
        assertFalse(mainController.contains("System.exit(0)"));
    }

    @Test
    @DisplayName("安装工具退出必须集中释放窗口")
    void installerExitDisposesWindowThroughCentralMethod() throws Exception {
        String installer = Files.readString(Path.of(
            "src/main/java/com/cashier/installer/Installer.java"
        ));
        String databaseDialog = Files.readString(Path.of(
            "src/main/java/com/cashier/installer/DatabaseConfigDialog.java"
        ));

        assertTrue(installer.contains("frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE)"));
        assertTrue(installer.contains("private void exitInstaller()"));
        assertTrue(installer.contains("frame.dispose()"));
        assertTrue(installer.contains("cancelButton.addActionListener(e -> exitInstaller())"));
        assertFalse(installer.contains("JFrame.EXIT_ON_CLOSE"));
        assertFalse(installer.contains("e -> System.exit(0)"));

        assertTrue(databaseDialog.contains("frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE)"));
        assertTrue(databaseDialog.contains("private void exitDialog()"));
        assertTrue(databaseDialog.contains("frame.dispose()"));
        assertTrue(databaseDialog.contains("exitDialog();"));
        assertFalse(databaseDialog.contains("JFrame.EXIT_ON_CLOSE"));
    }

    @Test
    @DisplayName("安装/配置工具不得把数据库密码落盘到 config/")
    void installersNeverPersistPasswordIntoConfig() throws Exception {
        String installer = Files.readString(Path.of(
            "src/main/java/com/cashier/installer/Installer.java"
        ));
        String databaseDialog = Files.readString(Path.of(
            "src/main/java/com/cashier/installer/DatabaseConfigDialog.java"
        ));

        assertTrue(installer.contains("Production deployments should provide the password through CASHIER_DB_PASSWORD."));
        assertTrue(installer.contains("\"db.password=\\n\""));
        assertTrue(installer.contains("DotEnv.upsert(DotEnv.DB_PASSWORD_KEY, dbPassword)"));
        assertTrue(installer.contains("\"production\".equalsIgnoreCase(System.getenv(\"ENVIRONMENT\"))"));

        assertTrue(databaseDialog.contains("Production deployments should provide the password through CASHIER_DB_PASSWORD."));
        assertTrue(databaseDialog.contains("\"db.password=\\n\""));
        assertTrue(databaseDialog.contains("DotEnv.upsert(DotEnv.DB_PASSWORD_KEY, password)"));
        assertTrue(databaseDialog.contains("\"production\".equalsIgnoreCase(System.getenv(\"ENVIRONMENT\"))"));

        // 任何模式（含开发模式）都不得把密码拼进 config/database.properties：
        // 开发模式写 .env，生产模式由运维通过环境变量注入
        assertFalse(installer.contains("passwordValueForConfig"));
        assertFalse(databaseDialog.contains("passwordValueForConfig"));
        assertFalse(installer.contains("db.password=%s"));
        assertFalse(databaseDialog.contains("db.password=%s"));
    }

    @Test
    @DisplayName("没有任何安装通道把数据库密码写进 config/database.properties")
    void noInstallerWritesPlaintextPasswordIntoConfig() throws Exception {
        // 覆盖全部会生成该文件的通道：Java 安装器/配置对话框 + shell/docker 安装脚本。
        // 只要有一条通道写明文，release 门禁就会在装了库的机器上失败。
        assertNoPasswordInConfigTemplate("src/main/java/com/cashier/installer/Installer.java");
        assertNoPasswordInConfigTemplate("src/main/java/com/cashier/installer/DatabaseConfigDialog.java");
        assertNoPasswordInConfigTemplate("install.sh");
        assertNoPasswordInConfigTemplate("docker/docker-init.sh");
    }

    private static void assertNoPasswordInConfigTemplate(String path) throws Exception {
        String source = Files.readString(Path.of(path));
        assertFalse(source.contains("db.password=${"),
            path + " 不得把密码变量直接拼进 db.password（应写入 .env）");
        assertFalse(source.contains("db.password=%s"),
            path + " 不得把密码参数直接拼进 db.password（应写入 .env）");
    }

    @Test
    @DisplayName("桌面交易相关页面必须避免默认全量交易加载")
    void desktopTransactionViewsUseDateRangeOrAggregates() throws Exception {
        String transactionController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/TransactionController.java"
        ));
        String shiftController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ShiftController.java"
        ));
        String profitReportController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ProfitReportController.java"
        ));

        assertTrue(transactionController.contains("findTransactionsByCurrentDateRange()"));
        assertTrue(transactionController.contains("getTransactionDAO().findByDateRange("));
        assertTrue(transactionController.contains("LocalDate.now().minusDays(30)"));
        assertFalse(transactionController.contains("allTransactions = TransactionDAO.findAll();"));
        assertFalse(transactionController.contains("return TransactionDAO.findAll();"));

        assertTrue(shiftController.contains("getTransactionDAO().getTotalRevenue("));
        assertTrue(shiftController.contains("getTransactionDAO().getTransactionCount("));
        assertTrue(shiftController.contains("getTransactionDAO().findByDateRange("));
        assertTrue(shiftController.contains("getShiftDAO().findRecent(SHIFT_HISTORY_LIMIT)"));
        assertFalse(shiftController.contains("ShiftDAO.findAll()"));
        assertFalse(shiftController.contains("getTransactionDAO().findAll()"));

        assertTrue(profitReportController.contains("findTransactionsByDateRange(startDate"));
        assertTrue(profitReportController.contains("getTransactionDAO().findByDateRange("));
        assertFalse(profitReportController.contains("allTransactions = TransactionDAO.findAll();"));
    }

    @Test
    @DisplayName("会员和库存桌面列表必须使用分页加载")
    void desktopMemberAndInventoryListsUsePagedQueries() throws Exception {
        String memberController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MemberController.java"
        ));
        String memberEditController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MemberEditController.java"
        ));
        String inventoryController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryController.java"
        ));
        String memberDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/MemberDAORefactored.java"
        ));

        assertTrue(memberController.contains("getMemberDAO().findAll(FIRST_PAGE, DESKTOP_PAGE_SIZE)"));
        assertTrue(memberController.contains("getMemberDAO().search(searchText, FIRST_PAGE, DESKTOP_PAGE_SIZE)"));
        assertFalse(memberController.contains("getMemberDAO().findAll()"));
        assertTrue(memberEditController.contains("getMemberDAO().findByPhone(phone)"));
        assertFalse(memberEditController.contains("getMemberDAO().findAll()"));

        assertTrue(inventoryController.contains("productDAO.findAll(FIRST_PAGE, DESKTOP_PAGE_SIZE)"));
        assertTrue(inventoryController.contains("productDAO.search(searchText, FIRST_PAGE, DESKTOP_PAGE_SIZE)"));
        assertFalse(inventoryController.contains("productDAO.findAll()"));
        assertFalse(inventoryController.contains("productDAO.search(searchText)"));

        assertTrue(memberDao.contains("PageResult<Member> search(String keyword, int pageNum, int pageSize)"));
    }

    @Test
    @DisplayName("收银台商品加载和扫码必须避免无界全量商品查询")
    void cartProductLookupUsesPagedAndTargetedQueries() throws Exception {
        String cartController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));

        assertTrue(cartController.contains("CART_PRODUCT_PAGE_SIZE = 500"));
        assertTrue(cartController.contains("productDAO.findAll(FIRST_PAGE, CART_PRODUCT_PAGE_SIZE)"));
        assertTrue(cartController.contains("productDAO.search(searchText.trim(), FIRST_PAGE, CART_PRODUCT_PAGE_SIZE)"));
        assertTrue(cartController.contains("findExactScanMatches(normalizedScanText)"));
        assertTrue(cartController.contains("productDAO.findByBarcode(scanText)"));
        assertTrue(cartController.contains("productDAO.findByProductCode(scanText)"));
        assertTrue(cartController.contains("productDAO.findByName(scanText)"));
        assertFalse(cartController.contains("productDAO.findAll()"));
        assertFalse(cartController.contains("DataService.loadInventory()"));
        assertFalse(cartController.contains("DataService.loadMembers()"));
    }

    @Test
    @DisplayName("盘点补货和商品编辑不得无界全量加载商品")
    void inventoryDialogsAvoidUnboundedProductLoads() throws Exception {
        String inventoryCheckController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryCheckController.java"
        ));
        String restockController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/RestockController.java"
        ));
        String productEditController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ProductEditController.java"
        ));
        String productDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/ProductDAORefactored.java"
        ));

        assertTrue(inventoryCheckController.contains("CHECK_PRODUCT_PAGE_SIZE = 500"));
        assertTrue(inventoryCheckController.contains("productDAO.findAll(FIRST_PAGE, CHECK_PRODUCT_PAGE_SIZE)"));
        assertTrue(inventoryCheckController.contains("productDAO.search(normalizedSearch, FIRST_PAGE, CHECK_PRODUCT_PAGE_SIZE)"));
        assertTrue(inventoryCheckController.contains("productDAO.findByCategory(selectedCategory, FIRST_PAGE, CHECK_PRODUCT_PAGE_SIZE)"));
        assertFalse(inventoryCheckController.contains("productDAO.findAll()"));

        assertFalse(restockController.contains("productDAO.findAll()"));
        assertFalse(productEditController.contains("productDAO.findAll()"));
        assertTrue(productEditController.contains("PRODUCT_SUPPLIER_LIMIT = 500"));
        assertTrue(productEditController.contains("getSupplierDAO().findByStatus(true, PRODUCT_SUPPLIER_LIMIT)"));
        assertFalse(productEditController.contains("SupplierDAO.findAll()"));
        assertTrue(productEditController.contains("productDAO.findMaxProductCodeSequence(prefix)"));
        assertTrue(productDao.contains("findMaxProductCodeSequence(String prefix)"));
    }

    @Test
    @DisplayName("审计日志和退货订单列表必须避免默认全量加载")
    void auditAndReturnOrderListsUseBoundedQueries() throws Exception {
        String auditLogController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/AuditLogController.java"
        ));
        String operationLogDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/OperationLogDAORefactored.java"
        ));
        String returnOrderController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ReturnOrderController.java"
        ));
        String returnOrderDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/ReturnOrderDAORefactored.java"
        ));

        assertTrue(auditLogController.contains("getOperationLogDAO().findRecent(AUDIT_LOG_LIMIT)"));
        assertTrue(operationLogDao.contains("findRecent(int limit)"));
        assertTrue(operationLogDao.contains("ORDER BY timestamp DESC LIMIT ?"));
        assertFalse(auditLogController.contains("OperationLogDAO.findAll()"));

        assertTrue(returnOrderController.contains("getReturnOrderDAO().findRecent(RETURN_ORDER_LIMIT)"));
        assertTrue(returnOrderController.contains("getReturnOrderDAO().findByDateRange("));
        assertTrue(returnOrderDao.contains("findRecent(int limit)"));
        assertTrue(returnOrderDao.contains("ORDER BY create_time DESC LIMIT ?"));
        assertFalse(returnOrderController.contains("getReturnOrderDAO().findAll()"));
    }

    @Test
    @DisplayName("采购订单和入库历史默认列表必须有界加载")
    void purchaseListsUseRecentQueriesByDefault() throws Exception {
        String purchaseOrderController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseOrderController.java"
        ));
        String purchaseOrderDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/PurchaseOrderDAORefactored.java"
        ));
        String purchaseInboundController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseInboundController.java"
        ));
        String purchaseInboundDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/PurchaseInboundDAORefactored.java"
        ));

        assertTrue(purchaseOrderController.contains("getPurchaseOrderDAO().findRecent(PURCHASE_ORDER_LIMIT)"));
        assertTrue(purchaseOrderDao.contains("findRecent(int limit)"));
        assertTrue(purchaseOrderDao.contains("ORDER BY po.create_time DESC LIMIT ?"));
        assertFalse(purchaseOrderController.contains("getPurchaseOrderDAO().findAll()"));

        assertTrue(purchaseInboundController.contains("getPurchaseInboundDAO().findRecent(INBOUND_HISTORY_LIMIT)"));
        assertTrue(purchaseInboundDao.contains("findRecent(int limit)"));
        assertTrue(purchaseInboundDao.contains("ORDER BY pi.create_time DESC LIMIT ?"));
        assertFalse(purchaseInboundController.contains("getPurchaseInboundDAO().findAll()"));
    }

    @Test
    @DisplayName("采购报表和审批页面不得默认全量加载采购订单")
    void purchaseReportsAndApprovalAvoidUnboundedOrderLoads() throws Exception {
        String purchaseReportController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseReportController.java"
        ));
        String purchaseApprovalController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseApprovalController.java"
        ));

        assertTrue(purchaseReportController.contains("getPurchaseOrderDAO().findByDateRange("));
        assertTrue(purchaseReportController.contains("loadOrdersByDateRange(startDate, endDate)"));
        assertTrue(purchaseReportController.contains("PURCHASE_REPORT_SUPPLIER_LIMIT = 500"));
        assertTrue(purchaseReportController.contains("getSupplierDAO().findRecent(PURCHASE_REPORT_SUPPLIER_LIMIT)"));
        assertFalse(purchaseReportController.contains("getPurchaseOrderDAO().findAll()"));
        assertFalse(purchaseReportController.contains("SupplierDAO.findAll()"));

        assertTrue(purchaseApprovalController.contains("getPurchaseOrderDAO().findRecent(APPROVAL_ORDER_LIMIT)"));
        assertFalse(purchaseApprovalController.contains("getPurchaseOrderDAO().findAll()"));
    }

    @Test
    @DisplayName("库存统计和预警服务必须使用数据库侧聚合或筛选")
    void inventoryServicesUseDatabaseAggregationAndAlertFiltering() throws Exception {
        String productDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/ProductDAORefactored.java"
        ));
        String inventoryService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/InventoryService.java"
        ));
        String productService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/ProductService.java"
        ));
        String alertService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/InventoryAlertService.java"
        ));
        String alertController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryAlertController.java"
        ));

        assertTrue(productDao.contains("InventoryStatistics getInventoryStatistics()"));
        assertTrue(productDao.contains("COALESCE(SUM(quantity), 0) AS total_quantity"));
        assertTrue(productDao.contains("findProductsRequiringStockAlert()"));
        assertTrue(productDao.contains("WHERE min_stock > 0 AND quantity <= min_stock"));

        assertTrue(inventoryService.contains("productDAO.getInventoryStatistics()"));
        assertTrue(inventoryService.contains("INVENTORY_LOAD_LIMIT = 5000"));
        assertTrue(inventoryService.contains("productDAO.findAll(FIRST_PAGE, INVENTORY_LOAD_LIMIT).getData()"));
        assertFalse(inventoryService.contains("List<Product> products = productDAO.findAll()"));

        assertTrue(productService.contains("DEFAULT_PRODUCT_LIST_LIMIT = 5000"));
        assertTrue(productService.contains("productDAO.findAll(FIRST_PAGE, DEFAULT_PRODUCT_LIST_LIMIT).getData()"));
        assertFalse(productService.contains("return productDAO.findAll()"));

        assertTrue(alertService.contains("productDAO.findProductsRequiringStockAlert()"));
        assertFalse(alertService.contains("productDAO.findAll()"));
        assertTrue(alertController.contains("productDAO.findProductsRequiringStockAlert()"));
        assertFalse(alertController.contains("productDAO.findAll()"));
    }

    @Test
    @DisplayName("会员统计必须使用数据库聚合")
    void memberStatisticsUsesDatabaseAggregation() throws Exception {
        String memberService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/MemberService.java"
        ));
        String memberDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/MemberDAORefactored.java"
        ));

        assertTrue(memberService.contains("getMemberDAO().getMemberSummary()"));
        assertTrue(memberService.contains("getMemberDAO().countByLevel()"));
        assertFalse(memberService.contains("List<Member> members = MemberDAO.findAll()"));

        assertTrue(memberDao.contains("getMemberSummary()"));
        assertTrue(memberDao.contains("COALESCE(SUM(balance), 0) AS total_balance"));
        assertTrue(memberDao.contains("countByLevel()"));
        assertTrue(memberDao.contains("GROUP BY level"));
    }

    @Test
    @DisplayName("库存盘点记录默认列表必须有界加载")
    void inventoryCheckListUsesRecentQueryByDefault() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryCheckController.java"
        ));
        String dao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/InventoryCheckDAORefactored.java"
        ));

        assertTrue(controller.contains("INVENTORY_CHECK_LIMIT = 500"));
        assertTrue(controller.contains("inventoryCheckDAO.findRecent(INVENTORY_CHECK_LIMIT)"));
        assertFalse(controller.contains("inventoryCheckDAO.findAll()"));

        assertTrue(dao.contains("findRecent(int limit)"));
        assertTrue(dao.contains("ORDER BY create_time DESC LIMIT ?"));
    }

    @Test
    @DisplayName("供应商列表和编号生成必须避免默认全量加载")
    void supplierListAndCodeGenerationAvoidUnboundedLoads() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/SupplierController.java"
        ));
        String dao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/SupplierDAORefactored.java"
        ));

        assertTrue(controller.contains("SUPPLIER_LIST_LIMIT = 500"));
        assertTrue(controller.contains("getSupplierDAO().findRecent(SUPPLIER_LIST_LIMIT)"));
        assertTrue(controller.contains("getSupplierDAO().search(searchText, SUPPLIER_LIST_LIMIT)"));
        assertTrue(controller.contains("getSupplierDAO().countBySupplierCodePrefix(prefix)"));
        assertFalse(controller.contains("SupplierDAO.findAll()"));

        assertTrue(dao.contains("findRecent(int limit)"));
        assertTrue(dao.contains("search(String keyword, int limit)"));
        assertTrue(dao.contains("countBySupplierCodePrefix(String prefix)"));
        assertTrue(dao.contains("ORDER BY create_time DESC, id DESC LIMIT ?"));
    }

    @Test
    @DisplayName("采购单商品选择必须使用有界商品查询")
    void purchaseOrderProductSelectionUsesBoundedQueries() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseOrderController.java"
        ));

        assertTrue(controller.contains("PRODUCT_SELECTION_PAGE_SIZE = 500"));
        assertTrue(controller.contains("PURCHASE_SUPPLIER_LIMIT = 500"));
        assertTrue(controller.contains("getSupplierDAO().findByStatus(true, PURCHASE_SUPPLIER_LIMIT)"));
        assertTrue(controller.contains("productDAO.findAll(FIRST_PAGE, PRODUCT_SELECTION_PAGE_SIZE)"));
        assertTrue(controller.contains("productDAO.search(normalizedSearch, FIRST_PAGE, PRODUCT_SELECTION_PAGE_SIZE)"));
        assertTrue(controller.contains("productDAO.findByCategory(category, FIRST_PAGE, PRODUCT_SELECTION_PAGE_SIZE)"));
        assertFalse(controller.contains("productDAO.findAll()"));
        assertFalse(controller.contains("SupplierDAO.findAll()"));
        assertFalse(controller.contains("filterProducts("));
    }

    @Test
    @DisplayName("会员充值历史必须按会员和数量限制查询")
    void rechargeHistoryUsesBoundedMemberQuery() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/RechargeController.java"
        ));
        String dao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/RechargeRecordDAORefactored.java"
        ));

        assertTrue(controller.contains("RECHARGE_HISTORY_LIMIT = 10"));
        assertTrue(controller.contains("getRechargeRecordDAO().findRecentByMemberPhone("));
        assertFalse(controller.contains("DataService.loadRechargeRecords()"));
        assertFalse(controller.contains("memberRecords.subList(0, 10)"));

        assertTrue(dao.contains("findRecentByMemberPhone(String memberPhone, int limit)"));
        assertTrue(dao.contains("ORDER BY timestamp DESC LIMIT ?"));
    }

    @Test
    @DisplayName("遗留数据读取入口必须有界加载")
    void legacyDataServiceReadsAreBounded() throws Exception {
        String dataService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/DataService.java"
        ));
        String rechargeDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/RechargeRecordDAORefactored.java"
        ));

        assertTrue(dataService.contains("LEGACY_LOAD_LIMIT = 5000"));
        assertTrue(dataService.contains("productDAO.findAll(FIRST_PAGE, LEGACY_LOAD_LIMIT).getData()"));
        assertTrue(dataService.contains("getUserDAO().findAll(FIRST_PAGE, LEGACY_LOAD_LIMIT).getData()"));
        assertTrue(dataService.contains("getMemberDAO().findAll(FIRST_PAGE, LEGACY_LOAD_LIMIT).getData()"));
        assertTrue(dataService.contains("getTransactionDAO().findRecent(LEGACY_LOAD_LIMIT)"));
        assertTrue(dataService.contains("getPromotionDAO().findRecent(LEGACY_LOAD_LIMIT)"));
        assertTrue(dataService.contains("getRechargeRecordDAO().findRecent(LEGACY_LOAD_LIMIT)"));
        assertTrue(dataService.contains("getOperationLogDAO().findRecent(LEGACY_LOAD_LIMIT)"));

        assertTrue(rechargeDao.contains("findRecent(int limit)"));
        assertTrue(rechargeDao.contains("ORDER BY timestamp DESC LIMIT ?"));
    }

    @Test
    @DisplayName("生产代码不得调用遗留覆盖式数据写入口")
    void productionCodeDoesNotUseLegacyOverwriteDataServiceWrites() throws Exception {
        String productionSources;
        try (var paths = Files.walk(Path.of("src/main/java/com/cashier"))) {
            productionSources = paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.toString().endsWith("DataService.java"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (java.io.IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                })
                .reduce("", String::concat);
        }

        assertFalse(productionSources.contains("DataService.saveInventory("));
        assertFalse(productionSources.contains("DataService.saveUsers("));
        assertFalse(productionSources.contains("DataService.saveMembers("));
        assertFalse(productionSources.contains("DataService.saveTransactions("));
        assertFalse(productionSources.contains("DataService.savePromotions("));
        assertFalse(productionSources.contains("DataService.saveRechargeRecords("));
        assertFalse(productionSources.contains("DataService.saveOperationLogs("));
    }

    @Test
    @DisplayName("遗留库存保存入口不得整表覆盖商品")
    void legacyInventorySaveDoesNotReplaceAllProducts() throws Exception {
        String dataService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/DataService.java"
        ));

        assertTrue(dataService.contains("productDAO.findByNamesWithConnection(conn, inventory.keySet())"));
        assertFalse(dataService.contains("List<Product> existing = productDAO.findAll()"));
        assertFalse(dataService.contains("productDAO.delete(p.id)"));
        assertFalse(dataService.contains("productDAO.batchInsert(products)"));
    }

    @Test
    @DisplayName("生产工具类不得直接写标准输出")
    void productionUtilitiesDoNotWriteDirectlyToStdout() throws Exception {
        String databaseManager = Files.readString(Path.of(
            "src/main/java/com/cashier/util/DatabaseManager.java"
        ));

        assertFalse(databaseManager.contains("System.out.println("));
    }
}
