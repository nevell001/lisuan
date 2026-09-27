package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.Category;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.DateTimeFormats;
import com.cashier.util.UIOptimizer;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * 库存报表控制器
 * 处理库存周转率、滞销商品、库存积压分析
 */
public class InventoryReportController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InventoryReportController.class);

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> timeRangeComboBox;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextField turnoverThresholdField;

    @FXML
    private TextField slowSalesThresholdField;

    @FXML
    private TextField inventoryDaysField;

    @FXML
    private Label totalProductsLabel;

    @FXML
    private Label totalStockValueLabel;

    @FXML
    private Label avgTurnoverRateLabel;

    @FXML
    private Label lowStockCountLabel;

    @FXML
    private Label slowSalesCountLabel;

    @FXML
    private Label overstockCountLabel;

    @FXML
    private PieChart stockStatusPieChart;

    @FXML
    private BarChart<String, Number> turnoverRateBarChart;

    @FXML
    private BarChart<String, Number> categoryStockValueBarChart;

    @FXML
    private TableView<InventoryReportRecord> productTable;

    @FXML
    private TableColumn<InventoryReportRecord, String> productNameColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> categoryColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> currentStockColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> stockValueColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> salesQuantityColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> turnoverRateColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> inventoryDaysColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> statusColumn;

    @FXML
    private TableView<InventoryReportRecord> slowSalesTable;

    @FXML
    private TableColumn<InventoryReportRecord, String> slowProductNameColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> slowCategoryColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> currentStockColumn2;

    @FXML
    private TableColumn<InventoryReportRecord, String> salesQuantityColumn2;

    @FXML
    private TableColumn<InventoryReportRecord, String> lastSaleDateColumn;

    @FXML
    private TableView<InventoryReportRecord> overstockTable;

    @FXML
    private TableColumn<InventoryReportRecord, String> overstockProductNameColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> overstockCategoryColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> overstockQuantityColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> overstockValueColumn;

    @FXML
    private TableColumn<InventoryReportRecord, String> overstockDaysColumn;

    @FXML
    private Button queryButton;

    @FXML
    private Button exportButton;

    private List<Product> allProducts = new java.util.ArrayList<>();
    private List<Transaction> allTransactions = new java.util.ArrayList<>();
    private Set<String> allCategories = new java.util.LinkedHashSet<>();
    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();

    // 默认阈值
    private static final double DEFAULT_TURNOVER_THRESHOLD = 1.0;  // 周转率阈值
    private static final int DEFAULT_SLOW_SALES_THRESHOLD = 10;   // 滞销阈值（销量）
    private static final int DEFAULT_INVENTORY_DAYS = 90;          // 库存天数阈值
    private static final int FIRST_PAGE = 1;
    private static final int INVENTORY_REPORT_PRODUCT_LIMIT = 5000;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化时间范围下拉框
        timeRangeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "今天",
            "昨天",
            "本周",
            "上周",
            "本月",
            "上月",
            "自定义"
        ));
        com.cashier.util.I18nUiUtils.configureComboBox(
            timeRangeComboBox, com.cashier.util.I18nUiUtils::dateRange);
        timeRangeComboBox.getSelectionModel().select(4); // 默认选中本月

        // 设置默认日期范围（本月）
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        startDatePicker.setValue(startOfMonth);
        endDatePicker.setValue(today);

        // 设置默认阈值
        turnoverThresholdField.setText(String.valueOf(DEFAULT_TURNOVER_THRESHOLD));
        slowSalesThresholdField.setText(String.valueOf(DEFAULT_SLOW_SALES_THRESHOLD));
        inventoryDaysField.setText(String.valueOf(DEFAULT_INVENTORY_DAYS));

        // 设置表格列
        setupProductTableColumns();
        setupSlowSalesTableColumns();
        setupOverstockTableColumns();

        // 初始化图表
        initializeCharts();

        // 加载数据
        loadData();

        // 执行查询
        handleQuery();

        // 监听时间范围变化
        timeRangeComboBox.setOnAction(event -> handleTimeRangeChange());
    }

    /**
     * 设置商品表格列
     */
    private void setupProductTableColumns() {
        productNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().productName));
        categoryColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().category));
        currentStockColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().currentStock)));
        stockValueColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().stockValue)));
        salesQuantityColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().salesQuantity)));
        turnoverRateColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().turnoverRate)));
        inventoryDaysColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.0f", cellData.getValue().inventoryDays)));
        statusColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                com.cashier.util.I18nUiUtils.inventoryStatus(cellData.getValue().status)));
    }

    /**
     * 设置滞销商品表格列
     */
    private void setupSlowSalesTableColumns() {
        slowProductNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().productName));
        slowCategoryColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().category));
        currentStockColumn2.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().currentStock)));
        salesQuantityColumn2.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().salesQuantity)));
        lastSaleDateColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().lastSaleDate));
    }

    /**
     * 设置积压商品表格列
     */
    private void setupOverstockTableColumns() {
        overstockProductNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().productName));
        overstockCategoryColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().category));
        overstockQuantityColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().currentStock)));
        overstockValueColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().stockValue)));
        overstockDaysColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.0f", cellData.getValue().inventoryDays)));
    }

    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 库存状态分布饼图
        stockStatusPieChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("inventory_report.stock_status"));
        stockStatusPieChart.setLegendSide(javafx.geometry.Side.RIGHT);

        // 库存周转率分析柱状图
        turnoverRateBarChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("inventory_report.turnover_analysis"));
        turnoverRateBarChart.getXAxis().setLabel(I18nManager.getInstance().get("chart.product"));
        turnoverRateBarChart.getYAxis().setLabel(I18nManager.getInstance().get("chart.turnover_rate"));
        turnoverRateBarChart.setLegendVisible(false);

        // 分类库存价值对比柱状图
        categoryStockValueBarChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.chart.category_stock_value"));
        categoryStockValueBarChart.getXAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.CATEGORY));
        categoryStockValueBarChart.getYAxis().setLabel(I18nManager.getInstance().get("chart.stock_value"));
        categoryStockValueBarChart.setLegendVisible(false);
    }

    /**
     * 加载数据
     */
    private void loadData() {
        try {
            allProducts = new ArrayList<>();
            allTransactions = new ArrayList<>();
            allCategories = loadAllCategoryNames();

            // 加载分类列表到下拉框
            javafx.collections.ObservableList<String> categoryList = javafx.collections.FXCollections.observableArrayList();
            categoryList.add("全部分类");
            categoryList.addAll(allCategories);
            categoryComboBox.setItems(categoryList);
            com.cashier.util.I18nUiUtils.configureComboBox(categoryComboBox, value ->
                "全部分类".equals(value) ? I18nManager.getInstance().get(I18nKeys.Filter.ALL_CATEGORIES) : value);
            categoryComboBox.getSelectionModel().select(0);

            logger.info("成功加载 {} 个库存报表分类", allCategories.size());
        } catch (SQLException e) {
            logger.error("加载数据失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            allProducts = new ArrayList<>();
            allTransactions = new ArrayList<>();
            allCategories = new TreeSet<>();
        }
    }

    private Set<String> loadAllCategoryNames() throws SQLException {
        Set<String> categories = new TreeSet<>();
        for (Category category : DAOFactory.getInstance().getCategoryDAO().findAll()) {
            if (category.name != null && !category.name.isBlank()) {
                categories.add(category.name);
            }
        }
        return categories;
    }

    /**
     * 处理时间范围变化
     */
    public void handleTimeRangeChange() {
        String selected = timeRangeComboBox.getSelectionModel().getSelectedItem();
        LocalDate today = LocalDate.now();

        switch (selected) {
            case "今天":
                startDatePicker.setValue(today);
                endDatePicker.setValue(today);
                break;
            case "昨天":
                LocalDate yesterday = today.minusDays(1);
                startDatePicker.setValue(yesterday);
                endDatePicker.setValue(yesterday);
                break;
            case "本周":
                LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
                startDatePicker.setValue(startOfWeek);
                endDatePicker.setValue(today);
                break;
            case "上周":
                LocalDate startOfLastWeek = today.minusDays(today.getDayOfWeek().getValue() - 1).minusWeeks(1);
                LocalDate endOfLastWeek = startOfLastWeek.plusDays(6);
                startDatePicker.setValue(startOfLastWeek);
                endDatePicker.setValue(endOfLastWeek);
                break;
            case "本月":
                LocalDate startOfMonth = today.withDayOfMonth(1);
                startDatePicker.setValue(startOfMonth);
                endDatePicker.setValue(today);
                break;
            case "上月":
                LocalDate startOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                LocalDate endOfLastMonth = today.withDayOfMonth(1).minusDays(1);
                startDatePicker.setValue(startOfLastMonth);
                endDatePicker.setValue(endOfLastMonth);
                break;
            case "自定义":
                // 不自动设置日期
                break;
            default:
                break;
        }
    }

    /**
     * 处理查询
     */
    @FXML
    public void handleQuery() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_DATE_RANGE));
            return;
        }

        if (startDate.isAfter(endDate)) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.INVALID_DATE_RANGE));
            return;
        }

        // 获取阈值
        double turnoverThreshold;
        int slowSalesThreshold;
        int inventoryDaysThreshold;

        try {
            turnoverThreshold = FormValidator.parseDouble(turnoverThresholdField.getText());
            slowSalesThreshold = FormValidator.parseInt(slowSalesThresholdField.getText());
            inventoryDaysThreshold = FormValidator.parseInt(inventoryDaysField.getText());
        } catch (NumberFormatException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.threshold_invalid"));
            return;
        }

        String selectedCategory = categoryComboBox.getSelectionModel().getSelectedItem();

        // 计算统计数据
        calculateStatistics(startDate, endDate, selectedCategory, turnoverThreshold, slowSalesThreshold, inventoryDaysThreshold);
    }

    /**
     * 计算统计数据
     */
    private void calculateStatistics(LocalDate startDate, LocalDate endDate, String categoryName,
                                    double turnoverThreshold, int slowSalesThreshold, int inventoryDaysThreshold) {
        // 商品（最多 INVENTORY_REPORT_PRODUCT_LIMIT 条）与区间内全部交易两个查询都放后台：
        // 点击"生成报表"时同步查库会让界面冻结。聚合与渲染回 FX 线程执行（只碰界面与内存数据）。
        UIOptimizer.runInBackground(
            () -> loadReportData(categoryName, startDate, endDate),
            data -> {
                if (data == null) {
                    allProducts = new ArrayList<>();
                    allTransactions = new ArrayList<>();
                    return;
                }
                allProducts = data.products();
                allTransactions = data.transactions();
                renderStatistics(startDate, endDate, categoryName, turnoverThreshold,
                    slowSalesThreshold, inventoryDaysThreshold);
            },
            e -> {
                logger.error("生成库存报表失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA)
                    + ": " + e.getMessage());
            });
    }

    /** 后台线程用：只查库并返回数据，不碰界面。返回 {@code null} 表示查询失败。 */
    private ReportData loadReportData(String categoryName, LocalDate startDate, LocalDate endDate) {
        try {
            List<Product> products;
            if (categoryName != null && !"全部分类".equals(categoryName)) {
                products = productDAO.findByCategory(categoryName, FIRST_PAGE, INVENTORY_REPORT_PRODUCT_LIMIT).getData();
            } else {
                products = productDAO.findAll(FIRST_PAGE, INVENTORY_REPORT_PRODUCT_LIMIT).getData();
            }
            logger.info("库存报表加载商品 {} 条，单次上限 {}", products.size(), INVENTORY_REPORT_PRODUCT_LIMIT);

            List<Transaction> transactions = DAOFactory.getInstance().getTransactionDAO().findByDateRange(
                startDate.atStartOfDay().format(DateTimeFormats.STANDARD_DATE_TIME),
                endDate.plusDays(1).atStartOfDay().minusSeconds(1).format(DateTimeFormats.STANDARD_DATE_TIME));
            return new ReportData(products, transactions);
        } catch (SQLException e) {
            logger.error("加载库存报表数据失败", e);
            return null;
        }
    }

    /** 后台查询结果：商品 + 区间内交易。 */
    private record ReportData(List<Product> products, List<Transaction> transactions) {
    }

    /** FX 线程用：聚合 + 渲染（{@code allProducts}/{@code allTransactions} 已就绪）。 */
    private void renderStatistics(LocalDate startDate, LocalDate endDate, String categoryName,
                                  double turnoverThreshold, int slowSalesThreshold, int inventoryDaysThreshold) {        Map<String, SalesStats> salesStatsMap = buildSalesStatsMap();

        // 总商品数、总库存价值
        int totalProducts = 0;
        double totalStockValue = 0.0;
        double totalTurnoverRate = 0.0;

        // 统计计数
        int lowStockCount = 0;
        int slowSalesCount = 0;
        int overstockCount = 0;

        // 商品统计记录
        List<InventoryReportRecord> productRecords = new ArrayList<>();
        List<InventoryReportRecord> slowSalesRecords = new ArrayList<>();
        List<InventoryReportRecord> overstockRecords = new ArrayList<>();

        // 分类统计
        Map<String, Integer> categoryQuantityMap = new HashMap<>();
        Map<String, Double> categoryAmountMap = new HashMap<>();

        // 计算天数
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

        for (Product product : allProducts) {
            // 分类筛选
            if (categoryName != null && !categoryName.equals("全部分类") &&
                !categoryName.equals(product.category)) {
                continue;
            }
            ProductStat s = computeProductStat(product,
                salesStatsMap.getOrDefault(product.name, SalesStats.empty()), daysBetween,
                slowSalesThreshold, inventoryDaysThreshold);
            totalProducts++;
            totalStockValue += s.stockValue;
            totalTurnoverRate += s.turnoverRate;
            if (s.lowStock) {
                lowStockCount++;
            }
            if (s.slowSales) {
                slowSalesCount++;
            }
            if (s.overstock) {
                overstockCount++;
            }
            productRecords.add(new InventoryReportRecord(
                product.name, s.category, product.quantity, s.stockValue,
                s.salesQuantity, s.turnoverRate, s.inventoryDays, s.status));
            categoryQuantityMap.merge(s.category, product.quantity, Integer::sum);
            categoryAmountMap.merge(s.category, s.stockValue, Double::sum);
            if (s.slowSales) {
                slowSalesRecords.add(new InventoryReportRecord(
                    product.name, s.category, product.quantity, s.salesQuantity, s.lastSaleDate));
            }
            if (s.overstock) {
                overstockRecords.add(new InventoryReportRecord(
                    product.name, s.category, product.quantity, s.stockValue, s.inventoryDays));
            }
        }

        // 计算平均周转率
        double avgTurnoverRate = totalProducts > 0 ? totalTurnoverRate / totalProducts : 0.0;
        updateStatisticsDisplay(totalProducts, totalStockValue, avgTurnoverRate,
            lowStockCount, slowSalesCount, overstockCount,
            productRecords, slowSalesRecords, overstockRecords,
            categoryQuantityMap, categoryAmountMap);
    }

    /** 单个商品统计结果 */
    private record ProductStat(double stockValue, int salesQuantity, double turnoverRate,
                               double inventoryDays, String status, String category,
                               boolean lowStock, boolean slowSales, boolean overstock,
                               String lastSaleDate) {
    }

    private ProductStat computeProductStat(Product product, SalesStats salesStats, long daysBetween,
                                           int slowSalesThreshold, double inventoryDaysThreshold) {
        double stockValue = product.getCost().multiply(BigDecimal.valueOf(product.quantity)).doubleValue();
        int salesQuantity = salesStats.quantity;
        double turnoverRate = product.quantity > 0
            ? (salesQuantity / (double) product.quantity) * (365.0 / daysBetween)
            : 0.0;
        double inventoryDays = salesQuantity > 0
            ? (product.quantity / (double) salesQuantity) * daysBetween
            : 999.0;

        String status = "正常";
        boolean lowStock = product.quantity <= product.minStock;
        boolean slowSales = salesQuantity < slowSalesThreshold;
        boolean overstock = inventoryDays > inventoryDaysThreshold;
        if (lowStock) {
            status = "库存不足";
        }
        if (slowSales) {
            status = "滞销";
        }
        if (overstock) {
            status = "积压";
        }
        String category = product.category != null
            ? product.category
            : I18nManager.getInstance().get(I18nKeys.Report.UNCATEGORIZED);
        return new ProductStat(stockValue, salesQuantity, turnoverRate, inventoryDays,
            status, category, lowStock, slowSales, overstock, salesStats.lastSaleDate());
    }

    private void updateStatisticsDisplay(int totalProducts, double totalStockValue, double avgTurnoverRate,
                                         int lowStockCount, int slowSalesCount, int overstockCount,
                                         List<InventoryReportRecord> productRecords,
                                         List<InventoryReportRecord> slowSalesRecords,
                                         List<InventoryReportRecord> overstockRecords,
                                         Map<String, Integer> categoryQuantityMap,
                                         Map<String, Double> categoryAmountMap) {
        totalProductsLabel.setText(String.valueOf(totalProducts));
        totalStockValueLabel.setText(CurrencyUtil.format(totalStockValue));
        avgTurnoverRateLabel.setText(String.format(java.util.Locale.ROOT, "%.2f", avgTurnoverRate));
        lowStockCountLabel.setText(String.valueOf(lowStockCount));
        slowSalesCountLabel.setText(String.valueOf(slowSalesCount));
        overstockCountLabel.setText(String.valueOf(overstockCount));

        updateProductTable(productRecords);
        updateSlowSalesTable(slowSalesRecords);
        updateOverstockTable(overstockRecords);
        updateCharts(productRecords, categoryQuantityMap, categoryAmountMap);
    }

    private Map<String, SalesStats> buildSalesStatsMap() {
        Map<String, SalesStats> statsMap = new HashMap<>();
        for (Transaction transaction : allTransactions) {
            if (transaction.items == null || transaction.timestamp == null) {
                continue;
            }
            LocalDate saleDate;
            try {
                saleDate = DateTimeFormats.parseStandard(transaction.timestamp).toLocalDate();
            } catch (Exception e) {
                continue;
            }
            for (var item : transaction.items) {
                statsMap.computeIfAbsent(item.name, ignored -> new SalesStats())
                    .record(item.quantity, saleDate);
            }
        }
        return statsMap;
    }

    /**
     * 更新商品表格
     */
    private void updateProductTable(List<InventoryReportRecord> records) {
        javafx.collections.ObservableList<InventoryReportRecord> list = javafx.collections.FXCollections.observableArrayList(records);

        // 按周转率排序
        list.sort((a, b) -> Double.compare(b.turnoverRate, a.turnoverRate));
        productTable.setItems(list);
    }

    /**
     * 更新滞销商品表格
     */
    private void updateSlowSalesTable(List<InventoryReportRecord> records) {
        javafx.collections.ObservableList<InventoryReportRecord> list = javafx.collections.FXCollections.observableArrayList(records);

        // 按库存排序
        list.sort((a, b) -> Integer.compare(b.currentStock, a.currentStock));
        slowSalesTable.setItems(list);
    }

    /**
     * 更新积压商品表格
     */
    private void updateOverstockTable(List<InventoryReportRecord> records) {
        javafx.collections.ObservableList<InventoryReportRecord> list = javafx.collections.FXCollections.observableArrayList(records);

        // 按库存天数排序
        list.sort((a, b) -> Double.compare(b.inventoryDays, a.inventoryDays));
        overstockTable.setItems(list);
    }

    /**
     * 更新图表
     */
    private void updateCharts(List<InventoryReportRecord> productRecords,
                               Map<String, Integer> categoryQuantityMap,
                               Map<String, Double> categoryAmountMap) {
        // 计算库存状态统计
        int normalCount = 0;
        int lowStockCount = 0;
        int slowSalesCount = 0;
        int overstockCount = 0;

        for (InventoryReportRecord record : productRecords) {
            if (record.status.equals("库存不足")) {
                lowStockCount++;
            } else if (record.status.equals("滞销")) {
                slowSalesCount++;
            } else if (record.status.equals("积压")) {
                overstockCount++;
            } else {
                normalCount++;
            }
        }

        // 更新状态饼图
        updateStockStatusPieChart(normalCount, lowStockCount, slowSalesCount, overstockCount);

        // 更新周转率柱状图
        updateTurnoverRateBarChart(productRecords);

        // 更新分类库存价值柱状图
        updateCategoryStockValueBarChart(categoryAmountMap);
    }

    /**
     * 更新库存状态饼图
     */
    private void updateStockStatusPieChart(int normalCount, int lowStockCount,
                                           int slowSalesCount, int overstockCount) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        if (normalCount > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Inventory.Status.NORMAL), normalCount));
        }
        if (lowStockCount > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Inventory.Status.LOW_STOCK), lowStockCount));
        }
        if (slowSalesCount > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get("inventory_report.status.slow"), slowSalesCount));
        }
        if (overstockCount > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get("inventory_report.status.overstock"), overstockCount));
        }

        stockStatusPieChart.setData(pieChartData);
    }

    /**
     * 更新周转率柱状图
     */
    private void updateTurnoverRateBarChart(List<InventoryReportRecord> records) {
        // 按周转率排序，只显示前15名
        List<InventoryReportRecord> sortedRecords = new ArrayList<>(records);
        sortedRecords.sort((a, b) -> Double.compare(b.turnoverRate, a.turnoverRate));
        sortedRecords = sortedRecords.subList(0, Math.min(15, sortedRecords.size()));

        // 创建柱状图数据
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(I18nManager.getInstance().get("chart.turnover_rate"));
        for (InventoryReportRecord record : sortedRecords) {
            series.getData().add(new XYChart.Data<>(record.productName, record.turnoverRate));
        }

        turnoverRateBarChart.getData().clear();
        turnoverRateBarChart.getData().add(series);
    }

    /**
     * 更新分类库存价值柱状图
     */
    private void updateCategoryStockValueBarChart(Map<String, Double> categoryAmountMap) {
        // 按金额排序
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(categoryAmountMap.entrySet());
        sortedEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // 创建柱状图数据
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(I18nManager.getInstance().get("chart.stock_value"));
        for (Map.Entry<String, Double> entry : sortedEntries) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        categoryStockValueBarChart.getData().clear();
        categoryStockValueBarChart.getData().add(series);
    }

    /**
     * 处理导出
     */
    @FXML
    public void handleExport() {
        // 显示导出选项对话框
        ChoiceDialog<String> exportDialog = new ChoiceDialog<>(
            "商品统计", "商品统计", "滞销商品", "库存积压"
        );
        exportDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_EXPORT_CONTENT));
        exportDialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_EXPORT_CONTENT_HEADER));
        exportDialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_CONTENT_LABEL));

        exportDialog.showAndWait().ifPresent(exportType -> {
            if (exportType.equals("商品统计")) {
                exportProductStatistics();
            } else if (exportType.equals("滞销商品")) {
                exportSlowMovingProducts();
            } else if (exportType.equals("库存积压")) {
                exportOverstockProducts();
            }
        });
    }

    /**
     * 导出商品统计
     */
    private void exportProductStatistics() {
        if (productTable.getItems().isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_products"));
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.EXPORT_FORMAT));
        formatDialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.PLEASE_SELECT_FORMAT));
        formatDialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.FORMAT_LABEL));

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "商品名称", "商品分类", "当前库存", "库存金额", "销售数量", "周转率(%)", "库存天数", "状态"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (InventoryReportRecord record : productTable.getItems()) {
                    data.add(new String[]{
                        record.productName,
                        record.category,
                        String.valueOf(record.currentStock),
                        CurrencyUtil.format(record.stockValue),
                        String.valueOf(record.salesQuantity),
                        String.format(java.util.Locale.ROOT, "%.2f", record.turnoverRate),
                        String.format(java.util.Locale.ROOT, "%.1f", record.inventoryDays),
                        record.status
                    });
                }

                // 导出数据
                String filePath = com.cashier.util.ExportUtil.export(
                    "库存商品统计报表",
                    headers,
                    data,
                    exportFormat,
                    "库存商品统计"
                );

                if (filePath != null) {
                    com.cashier.util.StatusBarManager.updateSuccess(
                        com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    successAlert.setHeaderText(null);
                    successAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_SUCCESS_PATH) + "\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("库存商品统计报表导出成功: {}", filePath);
                } else {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_FAILED));
                }
            } catch (Exception e) {
                logger.error("导出库存商品统计报表失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_FAILED_DETAIL, e.getMessage()));
            }
        });
    }

    /**
     * 导出滞销商品
     */
    private void exportSlowMovingProducts() {
        if (slowSalesTable.getItems().isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_slow_products"));
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.EXPORT_FORMAT));
        formatDialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.PLEASE_SELECT_FORMAT));
        formatDialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.FORMAT_LABEL));

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "商品名称", "商品分类", "当前库存", "库存金额", "最后销售日期", "滞销天数"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (InventoryReportRecord record : slowSalesTable.getItems()) {
                    data.add(new String[]{
                        record.productName,
                        record.category,
                        String.valueOf(record.currentStock),
                        CurrencyUtil.format(record.stockValue),
                        record.lastSaleDate != null ? record.lastSaleDate : "从未销售",
                        String.format(java.util.Locale.ROOT, "%.1f", record.inventoryDays)
                    });
                }

                // 导出数据
                String filePath = com.cashier.util.ExportUtil.export(
                    "滞销商品报表",
                    headers,
                    data,
                    exportFormat,
                    "滞销商品"
                );

                if (filePath != null) {
                    com.cashier.util.StatusBarManager.updateSuccess(
                        com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    successAlert.setHeaderText(null);
                    successAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_SUCCESS_PATH) + "\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("滞销商品报表导出成功: {}", filePath);
                } else {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_FAILED));
                }
            } catch (Exception e) {
                logger.error("导出滞销商品报表失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_FAILED_DETAIL, e.getMessage()));
            }
        });
    }

    /**
     * 导出库存积压
     */
    private void exportOverstockProducts() {
        if (overstockTable.getItems().isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_overstock"));
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.EXPORT_FORMAT));
        formatDialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.PLEASE_SELECT_FORMAT));
        formatDialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.FORMAT_LABEL));

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "商品名称", "商品分类", "当前库存", "最低库存", "库存金额", "超储数量", "超储金额"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (InventoryReportRecord record : overstockTable.getItems()) {
                    int overstockQuantity = record.currentStock - (int)(record.stockValue / 100); // 简化计算
                    double overstockAmount = overstockQuantity * (record.stockValue / record.currentStock);
                    data.add(new String[]{
                        record.productName,
                        record.category,
                        String.valueOf(record.currentStock),
                        "10", // 默认最低库存
                        CurrencyUtil.format(record.stockValue),
                        String.valueOf(overstockQuantity),
                        CurrencyUtil.format(overstockAmount)
                    });
                }

                // 导出数据
                String filePath = com.cashier.util.ExportUtil.export(
                    "库存积压报表",
                    headers,
                    data,
                    exportFormat,
                    "库存积压"
                );

                if (filePath != null) {
                    com.cashier.util.StatusBarManager.updateSuccess(
                        com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    successAlert.setHeaderText(null);
                    successAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_SUCCESS_PATH) + "\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("库存积压报表导出成功: {}", filePath);
                } else {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_FAILED));
                }
            } catch (Exception e) {
                logger.error("导出库存积压报表失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_FAILED_DETAIL, e.getMessage()));
            }
        });
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        com.cashier.util.FXUtils.showError(message);
    }

    /**
     * 库存报表记录内部类
     */
    private static class InventoryReportRecord {
        String productName;
        String category;
        int currentStock;
        double stockValue;
        int salesQuantity;
        double turnoverRate;
        double inventoryDays;
        String status;
        String lastSaleDate;

        // 商品统计构造函数
        public InventoryReportRecord(String productName, String category, int currentStock,
                                    double stockValue, int salesQuantity, double turnoverRate,
                                    double inventoryDays, String status) {
            this.productName = productName;
            this.category = category;
            this.currentStock = currentStock;
            this.stockValue = stockValue;
            this.salesQuantity = salesQuantity;
            this.turnoverRate = turnoverRate;
            this.inventoryDays = inventoryDays;
            this.status = status;
        }

        // 滞销商品构造函数
        public InventoryReportRecord(String productName, String category, int currentStock,
                                    int salesQuantity, String lastSaleDate) {
            this.productName = productName;
            this.category = category;
            this.currentStock = currentStock;
            this.salesQuantity = salesQuantity;
            this.lastSaleDate = lastSaleDate;
        }

        // 积压商品构造函数
        public InventoryReportRecord(String productName, String category, int currentStock,
                                    double stockValue, double inventoryDays) {
            this.productName = productName;
            this.category = category;
            this.currentStock = currentStock;
            this.stockValue = stockValue;
            this.inventoryDays = inventoryDays;
        }
    }

    private static class SalesStats {
        int quantity;
        LocalDate lastSaleDate;

        static SalesStats empty() {
            return new SalesStats();
        }

        void record(int soldQuantity, LocalDate saleDate) {
            quantity += soldQuantity;
            if (lastSaleDate == null || saleDate.isAfter(lastSaleDate)) {
                lastSaleDate = saleDate;
            }
        }

        String lastSaleDate() {
            return lastSaleDate == null
                ? I18nManager.getInstance().get("report.never_sold")
                : lastSaleDate.format(DateTimeFormats.DATE);
        }
    }
}
