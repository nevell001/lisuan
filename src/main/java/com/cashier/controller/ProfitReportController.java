package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.DateTimeFormats;
import com.cashier.model.Category;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * 利润分析控制器
 * 处理采购成本、销售收入、毛利率分析
 */
public class ProfitReportController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ProfitReportController.class);

    // 运营成本比例（默认为收入的5%）
    private static final double DEFAULT_OPERATING_COST_RATIO = 0.05;
    private static final String PERCENT_FORMAT = "%.2f%%";
    private static final String EXPORT_EXCEL = "Excel";
    private static final String EXPORT_PDF = "PDF";
    private static final String HEADER_MARGIN = "毛利率(%)";

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> timeRangeComboBox;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private Label totalCostLabel;

    @FXML
    private Label grossProfitLabel;

    @FXML
    private Label grossMarginLabel;

    @FXML
    private Label netProfitLabel;

    @FXML
    private Label avgMarginLabel;

    @FXML
    private PieChart profitCompositionPieChart;

    @FXML
    private LineChart<String, Number> dailyProfitTrendLineChart;

    @FXML
    private BarChart<String, Number> categoryProfitBarChart;

    @FXML
    private TableView<ProfitReportRecord> productProfitTable;

    @FXML
    private TableColumn<ProfitReportRecord, String> productNameColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> productCategoryColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> salesRevenueColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> salesCostColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> salesProfitColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> salesMarginColumn;

    @FXML
    private TableView<ProfitReportRecord> categoryProfitTable;

    @FXML
    private TableColumn<ProfitReportRecord, String> categoryNameColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> categoryRevenueColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> categoryCostColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> categoryProfitColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> categoryMarginColumn;

    @FXML
    private TableView<ProfitReportRecord> dailyProfitTable;

    @FXML
    private TableColumn<ProfitReportRecord, String> dateColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> dailyRevenueColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> dailyCostColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> dailyProfitColumn;

    @FXML
    private TableColumn<ProfitReportRecord, String> dailyMarginColumn;

    @FXML
    private Button queryButton;

    @FXML
    private Button exportButton;

    private List<Transaction> allTransactions;
    private Map<String, Double> productActualCostMap; // 商品实际成本（加权平均）
    private Map<String, Product> productNameMap; // 商品名称到商品的映射
    private Set<String> allCategories;
    /** 防止重复触发查询（后台查询执行中忽略新查询） */
    private boolean profitQueryInProgress;
    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();

    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
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

        // 设置表格列
        setupProductProfitTableColumns();
        setupCategoryProfitTableColumns();
        setupDailyProfitTableColumns();

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
     * 设置商品利润表格列
     */
    private void setupProductProfitTableColumns() {
        productNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().productName));
        productCategoryColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().category));
        salesRevenueColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().revenue)));
        salesCostColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().cost)));
        salesProfitColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().profit)));
        salesMarginColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format(PERCENT_FORMAT, cellData.getValue().margin * 100)));
    }

    /**
     * 设置分类利润表格列
     */
    private void setupCategoryProfitTableColumns() {
        categoryNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().category));
        categoryRevenueColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().revenue)));
        categoryCostColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().cost)));
        categoryProfitColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().profit)));
        categoryMarginColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format(PERCENT_FORMAT, cellData.getValue().margin * 100)));
    }

    /**
     * 设置每日利润表格列
     */
    private void setupDailyProfitTableColumns() {
        dateColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().date));
        dailyRevenueColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().revenue)));
        dailyCostColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().cost)));
        dailyProfitColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().profit)));
        dailyMarginColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format(PERCENT_FORMAT, cellData.getValue().margin * 100)));
    }

    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 利润构成饼图
        profitCompositionPieChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.chart.profit_composition"));
        profitCompositionPieChart.setLegendSide(javafx.geometry.Side.RIGHT);

        // 每日利润趋势折线图
        dailyProfitTrendLineChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.chart.daily_profit"));
        dailyProfitTrendLineChart.getXAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.DATE));
        dailyProfitTrendLineChart.getYAxis().setLabel(I18nManager.getInstance().get("chart.profit"));
        dailyProfitTrendLineChart.setCreateSymbols(false);
        dailyProfitTrendLineChart.setLegendVisible(true);

        // 分类利润对比柱状图
        categoryProfitBarChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.chart.category_profit"));
        categoryProfitBarChart.getXAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.CATEGORY));
        categoryProfitBarChart.getYAxis().setLabel(I18nManager.getInstance().get("chart.profit"));
        categoryProfitBarChart.setLegendVisible(false);
    }

    /**
     * 加载分类下拉（快）。交易/成本等重数据统一由 handleQuery 后台加载，
     * 避免打开页面时 loadData+handleQuery 各跑一遍整段区间查询。
     */
    private void loadData() {
        try {
            allCategories = loadAllCategoryNames();

            // 加载分类列表到下拉框
            javafx.collections.ObservableList<String> categoryList = javafx.collections.FXCollections.observableArrayList();
            categoryList.add("全部分类");
            categoryList.addAll(allCategories);
            categoryComboBox.setItems(categoryList);
            com.cashier.util.I18nUiUtils.configureComboBox(categoryComboBox, value ->
                "全部分类".equals(value) ? I18nManager.getInstance().get(I18nKeys.Filter.ALL_CATEGORIES) : value);
            categoryComboBox.getSelectionModel().select(0);
        } catch (SQLException e) {
            logger.error("加载分类数据失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            allCategories = new TreeSet<>();
        }
    }

    private Map<String, Product> loadProductNameMap(List<Transaction> transactions) throws SQLException {
        Set<String> productNames = new HashSet<>();
        for (Transaction transaction : transactions) {
            if (transaction.items == null) {
                continue;
            }
            for (Product item : transaction.items) {
                if (item.name != null && !item.name.isBlank()) {
                    productNames.add(item.name);
                }
            }
        }
        return productDAO.findByNames(productNames);
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
     * 加载每个商品的加权平均采购成本
     */
    private void loadProductActualCosts() throws SQLException {
        Map<Integer, BigDecimal> averageCostByProductId = DAOFactory.getInstance().getPurchaseInboundItemDAO().findAverageUnitCostByProductId();
        for (Product product : productNameMap.values()) {
            BigDecimal averageCost = averageCostByProductId.get(product.id);
            if (averageCost != null && product.name != null && !product.name.isEmpty()) {
                productActualCostMap.put(product.name, averageCost.doubleValue());
            }
        }

        logger.info("加载了 {} 个商品的实际成本", productActualCostMap.size());
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
     * 处理查询（交易/商品/成本等重查询放到 daemon 线程，聚合与 UI 更新经 runLater 回 FX）
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

        if (profitQueryInProgress) {
            return;
        }
        profitQueryInProgress = true;

        final String selectedCategory = categoryComboBox.getSelectionModel().getSelectedItem();

        Thread worker = new Thread(() -> {
            try {
                // 后台加载：区间交易、交易涉及商品、加权平均成本
                allTransactions = findTransactionsByDateRange(startDate, endDate);
                productNameMap = loadProductNameMap(allTransactions);
                productActualCostMap = new HashMap<>();
                loadProductActualCosts();

                logger.info("利润报表加载完成: {} 条交易, {} 个商品, {} 个实际成本",
                    allTransactions.size(), productNameMap.size(), productActualCostMap.size());
                javafx.application.Platform.runLater(() -> {
                    profitQueryInProgress = false;
                    // 计算统计数据（内存聚合 + UI 更新）
                    calculateStatistics(startDate, endDate, selectedCategory);
                });
            } catch (SQLException e) {
                logger.error("加载利润报表交易记录失败", e);
                javafx.application.Platform.runLater(() -> {
                    profitQueryInProgress = false;
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
                    allTransactions = new ArrayList<>();
                    productNameMap = new HashMap<>();
                    productActualCostMap = new HashMap<>();
                });
            }
        }, "profit-report-query");
        worker.setDaemon(true);
        worker.start();
    }

    private List<Transaction> findTransactionsByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        if (startDate == null || endDate == null) {
            return new ArrayList<>();
        }
        return DAOFactory.getInstance().getTransactionDAO().findByDateRange(
            startDate.atStartOfDay().format(DateTimeFormats.STANDARD_DATE_TIME),
            endDate.plusDays(1).atStartOfDay().minusSeconds(1).format(DateTimeFormats.STANDARD_DATE_TIME)
        );
    }

    /**
     * 计算统计数据
     */
    private void calculateStatistics(LocalDate startDate, LocalDate endDate, String categoryName) {
        ProfitStatistics statistics = collectProfitStatistics(startDate, endDate, categoryName);
        double grossProfit = statistics.totalRevenue - statistics.totalCost;
        double grossMargin = statistics.totalRevenue > 0 ? grossProfit / statistics.totalRevenue : 0.0;
        double operatingCost = statistics.totalRevenue * loadOperatingCostRatio();
        double netProfit = grossProfit - operatingCost;
        double avgMargin = calculateAverageMargin(statistics.productProfitMap);

        updateSummaryCards(statistics.totalRevenue, statistics.totalCost, grossProfit,
            grossMargin, netProfit, avgMargin);
        logCostSourceStats(statistics.productProfitMap);
        updateProductProfitTable(statistics.productProfitMap);
        updateCategoryProfitTable(statistics.categoryProfitMap);
        updateDailyProfitTable(statistics.dailyProfitMap);
        updateCharts(grossProfit, operatingCost, netProfit, statistics.categoryProfitMap, statistics.dailyProfitMap);
    }

    private ProfitStatistics collectProfitStatistics(LocalDate startDate, LocalDate endDate, String categoryName) {
        ProfitStatistics statistics = new ProfitStatistics(startDate, endDate);
        for (Transaction transaction : allTransactions) {
            try {
                LocalDate transactionDate = parseTransactionDate(transaction);
                if (isWithinRange(transactionDate, startDate, endDate)) {
                    accumulateTransactionProfit(statistics, transaction, transactionDate, categoryName);
                }
            } catch (Exception e) {
                logger.warn("处理交易记录失败: {}", transaction.transactionId, e);
            }
        }
        return statistics;
    }

    private LocalDate parseTransactionDate(Transaction transaction) {
        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(transaction.timestamp, com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
        return ldt.toLocalDate();
    }

    private boolean isWithinRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private void accumulateTransactionProfit(
            ProfitStatistics statistics,
            Transaction transaction,
            LocalDate transactionDate,
            String categoryName) {

        if (transaction.items == null) {
            return;
        }

        for (Product item : transaction.items) {
            Product product = findProductByName(item.name);
            String category = resolveCategory(product);
            if (!matchesCategoryFilter(categoryName, category)) {
                continue;
            }
            accumulateItemProfit(statistics, item, product, category, transactionDate.toString());
        }
    }

    private String resolveCategory(Product product) {
        return product != null && product.category != null
            ? product.category
            : I18nManager.getInstance().get(I18nKeys.Report.UNCATEGORIZED);
    }

    private boolean matchesCategoryFilter(String categoryName, String category) {
        return categoryName == null || "全部分类".equals(categoryName) || categoryName.equals(category);
    }

    private void accumulateItemProfit(
            ProfitStatistics statistics,
            Product item,
            Product product,
            String category,
            String dateStr) {

        double cost = resolveUnitCost(item, product);
        double revenue = item.getPrice().multiply(BigDecimal.valueOf(item.quantity)).doubleValue();
        double itemCost = cost * item.quantity;
        double profit = revenue - itemCost;

        statistics.totalRevenue += revenue;
        statistics.totalCost += itemCost;

        ProductProfit productProfit = statistics.productProfitMap
            .computeIfAbsent(item.name, name -> new ProductProfit(name, category));
        productProfit.revenue += revenue;
        productProfit.cost += itemCost;
        productProfit.profit += profit;
        productProfit.quantity += item.quantity;

        CategoryProfit categoryProfit = statistics.categoryProfitMap
            .computeIfAbsent(category, CategoryProfit::new);
        categoryProfit.revenue += revenue;
        categoryProfit.cost += itemCost;
        categoryProfit.profit += profit;

        DailyProfit dailyProfit = statistics.dailyProfitMap.get(dateStr);
        if (dailyProfit != null) {
            dailyProfit.revenue += revenue;
            dailyProfit.cost += itemCost;
            dailyProfit.profit += profit;
        }
    }

    private double resolveUnitCost(Product item, Product product) {
        if (productActualCostMap.containsKey(item.name)) {
            return productActualCostMap.get(item.name);
        }
        if (product != null && product.getCost().compareTo(BigDecimal.ZERO) > 0) {
            return product.getCost().doubleValue();
        }
        return item.getPrice().multiply(Product.DEFAULT_COST_RATE).doubleValue();
    }

    private double loadOperatingCostRatio() {
        double costRatio = DEFAULT_OPERATING_COST_RATIO;
        try {
            String ratioStr = com.cashier.dao.DAOFactory.getInstance().getSystemSettingsDAO().getSetting("operatingCostRatio");
            if (ratioStr != null) {
                costRatio = FormValidator.parseDouble(ratioStr);
            }
        } catch (Exception e) {
            logger.warn("加载运营成本比例设置失败，使用默认值: {}", e.getMessage());
        }
        return costRatio;
    }

    private double calculateAverageMargin(Map<String, ProductProfit> productProfitMap) {
        return !productProfitMap.isEmpty()
            ? productProfitMap.values().stream()
                .mapToDouble(pp -> pp.revenue > 0 ? pp.profit / pp.revenue : 0.0)
                .average()
                .orElse(0.0)
            : 0.0;
    }

    private void updateSummaryCards(
            double totalRevenue,
            double totalCost,
            double grossProfit,
            double grossMargin,
            double netProfit,
            double avgMargin) {

        totalRevenueLabel.setText(CurrencyUtil.format(totalRevenue));
        totalCostLabel.setText(CurrencyUtil.format(totalCost));
        grossProfitLabel.setText(CurrencyUtil.format(grossProfit));
        grossMarginLabel.setText(String.format(PERCENT_FORMAT, grossMargin * 100));
        netProfitLabel.setText(CurrencyUtil.format(netProfit));
        avgMarginLabel.setText(String.format(PERCENT_FORMAT, avgMargin * 100));
    }

    private void logCostSourceStats(Map<String, ProductProfit> productProfitMap) {
        int actualCostCount = 0;
        int estimatedCostCount = 0;
        for (ProductProfit pp : productProfitMap.values()) {
            if (productActualCostMap.containsKey(pp.productName)) {
                actualCostCount++;
            } else {
                estimatedCostCount++;
            }
        }
        logger.info("成本来源统计: 实际成本商品 {} 个, 估算成本商品 {} 个", actualCostCount, estimatedCostCount);
    }

    /**
     * 根据名称查找商品
     */
    private Product findProductByName(String name) {
        return productNameMap.get(name);
    }

    /**
     * 更新商品利润表格
     */
    private void updateProductProfitTable(Map<String, ProductProfit> productProfitMap) {
        javafx.collections.ObservableList<ProfitReportRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (ProductProfit pp : productProfitMap.values()) {
            double margin = pp.revenue > 0 ? pp.profit / pp.revenue : 0.0;
            list.add(new ProfitReportRecord(
                pp.productName,
                pp.category,
                pp.revenue,
                pp.cost,
                pp.profit,
                margin
            ));
        }

        // 按利润排序
        list.sort((a, b) -> Double.compare(b.profit, a.profit));
        productProfitTable.setItems(list);
    }

    /**
     * 更新分类利润表格
     */
    private void updateCategoryProfitTable(Map<String, CategoryProfit> categoryProfitMap) {
        javafx.collections.ObservableList<ProfitReportRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (CategoryProfit cp : categoryProfitMap.values()) {
            double margin = cp.revenue > 0 ? cp.profit / cp.revenue : 0.0;
            list.add(new ProfitReportRecord(
                cp.category,
                cp.revenue,
                cp.cost,
                cp.profit,
                margin
            ));
        }

        // 按利润排序
        list.sort((a, b) -> Double.compare(b.profit, a.profit));
        categoryProfitTable.setItems(list);
    }

    /**
     * 更新每日利润表格
     */
    private void updateDailyProfitTable(Map<String, DailyProfit> dailyProfitMap) {
        javafx.collections.ObservableList<ProfitReportRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (DailyProfit dp : dailyProfitMap.values()) {
            double margin = dp.revenue > 0 ? dp.profit / dp.revenue : 0.0;
            list.add(new ProfitReportRecord(
                dp.date,
                dp.revenue,
                dp.cost,
                dp.profit,
                margin,
                true
            ));
        }

        dailyProfitTable.setItems(list);
    }

    /**
     * 更新图表
     */
    private void updateCharts(double grossProfit,
                               double operatingCost, double netProfit,
                               Map<String, CategoryProfit> categoryProfitMap,
                               Map<String, DailyProfit> dailyProfitMap) {
        // 更新利润构成饼图
        updateProfitCompositionPieChart(grossProfit, operatingCost, netProfit);

        // 更新每日利润趋势折线图
        updateDailyProfitTrendChart(dailyProfitMap);

        // 更新分类利润对比柱状图
        updateCategoryProfitBarChart(categoryProfitMap);
    }

    /**
     * 更新利润构成饼图
     */
    private void updateProfitCompositionPieChart(double grossProfit, double operatingCost, double netProfit) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        if (grossProfit > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get("chart.series.gross_profit"), grossProfit));
        }
        if (operatingCost > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get("chart.series.operating_cost"), operatingCost));
        }
        if (netProfit > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get("chart.series.net_profit"), netProfit));
        }

        profitCompositionPieChart.setData(pieChartData);
    }

    /**
     * 更新每日利润趋势折线图
     */
    private void updateDailyProfitTrendChart(Map<String, DailyProfit> dailyProfitMap) {
        XYChart.Series<String, Number> grossProfitSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> netProfitSeries = new XYChart.Series<>();

        grossProfitSeries.setName(I18nManager.getInstance().get("chart.series.gross_profit"));
        netProfitSeries.setName(I18nManager.getInstance().get("chart.series.net_profit"));

        for (Map.Entry<String, DailyProfit> entry : dailyProfitMap.entrySet()) {
            DailyProfit dp = entry.getValue();
            grossProfitSeries.getData().add(new XYChart.Data<>(entry.getKey(), dp.profit));
            // 净利润 = 毛利润 - 运营成本（假设运营成本为收入的5%）
            double operatingCost = dp.revenue * DEFAULT_OPERATING_COST_RATIO;
            netProfitSeries.getData().add(new XYChart.Data<>(entry.getKey(), dp.profit - operatingCost));
        }

        dailyProfitTrendLineChart.getData().clear();
        dailyProfitTrendLineChart.getData().add(grossProfitSeries);
        dailyProfitTrendLineChart.getData().add(netProfitSeries);
    }

    /**
     * 更新分类利润对比柱状图
     */
    private void updateCategoryProfitBarChart(Map<String, CategoryProfit> categoryProfitMap) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(I18nManager.getInstance().get("chart.series.profit"));

        for (Map.Entry<String, CategoryProfit> entry : categoryProfitMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue().profit));
        }

        categoryProfitBarChart.getData().clear();
        categoryProfitBarChart.getData().add(series);
    }

    /**
     * 处理导出
     */
    @FXML
    public void handleExport() {
        // 显示导出选项对话框
        ChoiceDialog<String> exportDialog = new ChoiceDialog<>(
            "商品利润", "商品利润", "分类利润", "每日利润"
        );
        exportDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_EXPORT_CONTENT));
        exportDialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_EXPORT_CONTENT_HEADER));
        exportDialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_CONTENT_LABEL));

        exportDialog.showAndWait().ifPresent(exportType -> {
            if (exportType.equals("商品利润")) {
                exportProductProfit();
            } else if (exportType.equals("分类利润")) {
                exportCategoryProfit();
            } else if (exportType.equals("每日利润")) {
                exportDailyProfit();
            }
        });
    }

    /**
     * 导出商品利润
     */
    private void exportProductProfit() {
        if (productProfitTable.getItems().isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_product_profit"));
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            EXPORT_EXCEL, EXPORT_EXCEL, EXPORT_PDF
        );
        formatDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.EXPORT_FORMAT));
        formatDialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.PLEASE_SELECT_FORMAT));
        formatDialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.FORMAT_LABEL));

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                EXPORT_EXCEL.equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "商品名称", "商品分类", "销售收入", "销售成本", "毛利润", HEADER_MARGIN
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (ProfitReportRecord record : productProfitTable.getItems()) {
                    double profitMargin = record.revenue > 0 ? (record.profit / record.revenue) * 100 : 0;
                    data.add(new String[]{
                        record.productName != null ? record.productName : "",
                        record.category != null ? record.category : "",
                        CurrencyUtil.format(record.revenue),
                        CurrencyUtil.format(record.cost),
                        CurrencyUtil.format(record.profit),
                        String.format("%.2f", profitMargin)
                    });
                }

                // 导出数据
                String filePath = com.cashier.util.ExportUtil.export(
                    "商品利润分析报表",
                    headers,
                    data,
                    exportFormat,
                    "商品利润"
                );

                if (filePath != null) {
                    com.cashier.util.StatusBarManager.updateSuccess(
                        com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    successAlert.setHeaderText(null);
                    successAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_SUCCESS_PATH) + "\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("商品利润分析报表导出成功: {}", filePath);
                } else {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_FAILED));
                }
            } catch (Exception e) {
                logger.error("导出商品利润分析报表失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_FAILED_DETAIL, e.getMessage()));
            }
        });
    }

    /**
     * 导出分类利润
     */
    private void exportCategoryProfit() {
        if (categoryProfitTable.getItems().isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_category_profit"));
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            EXPORT_EXCEL, EXPORT_EXCEL, EXPORT_PDF
        );
        formatDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.EXPORT_FORMAT));
        formatDialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.PLEASE_SELECT_FORMAT));
        formatDialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.FORMAT_LABEL));

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                EXPORT_EXCEL.equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "商品分类", "销售收入", "销售成本", "毛利润", HEADER_MARGIN
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (ProfitReportRecord record : categoryProfitTable.getItems()) {
                    double profitMargin = record.revenue > 0 ? (record.profit / record.revenue) * 100 : 0;
                    data.add(new String[]{
                        record.category != null ? record.category : "",
                        CurrencyUtil.format(record.revenue),
                        CurrencyUtil.format(record.cost),
                        CurrencyUtil.format(record.profit),
                        String.format("%.2f", profitMargin)
                    });
                }

                // 导出数据
                String filePath = com.cashier.util.ExportUtil.export(
                    "分类利润分析报表",
                    headers,
                    data,
                    exportFormat,
                    "分类利润"
                );

                if (filePath != null) {
                    com.cashier.util.StatusBarManager.updateSuccess(
                        com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    successAlert.setHeaderText(null);
                    successAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_SUCCESS_PATH) + "\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("分类利润分析报表导出成功: {}", filePath);
                } else {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_FAILED));
                }
            } catch (Exception e) {
                logger.error("导出分类利润分析报表失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_FAILED_DETAIL, e.getMessage()));
            }
        });
    }

    /**
     * 导出每日利润
     */
    private void exportDailyProfit() {
        if (dailyProfitTable.getItems().isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_daily_profit"));
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            EXPORT_EXCEL, EXPORT_EXCEL, EXPORT_PDF
        );
        formatDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.EXPORT_FORMAT));
        formatDialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.PLEASE_SELECT_FORMAT));
        formatDialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.FORMAT_LABEL));

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                EXPORT_EXCEL.equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "日期", "销售收入", "销售成本", "毛利润", HEADER_MARGIN, "净利润"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (ProfitReportRecord record : dailyProfitTable.getItems()) {
                    double profitMargin = record.revenue > 0 ? (record.profit / record.revenue) * 100 : 0;
                    data.add(new String[]{
                        record.date != null ? record.date : "",
                        CurrencyUtil.format(record.revenue),
                        CurrencyUtil.format(record.cost),
                        CurrencyUtil.format(record.profit),
                        String.format("%.2f", profitMargin),
                        CurrencyUtil.format(record.profit * 0.95) // 假设净利润为毛利润的95%
                    });
                }

                // 导出数据
                String filePath = com.cashier.util.ExportUtil.export(
                    "每日利润分析报表",
                    headers,
                    data,
                    exportFormat,
                    "每日利润"
                );

                if (filePath != null) {
                    com.cashier.util.StatusBarManager.updateSuccess(
                        com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                    successAlert.setHeaderText(null);
                    successAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_SUCCESS_PATH) + "\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("每日利润分析报表导出成功: {}", filePath);
                } else {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_FAILED));
                }
            } catch (Exception e) {
                logger.error("导出每日利润分析报表失败", e);
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

    private static class ProfitStatistics {
        double totalRevenue;
        double totalCost;
        final Map<String, ProductProfit> productProfitMap = new HashMap<>();
        final Map<String, CategoryProfit> categoryProfitMap = new HashMap<>();
        final Map<String, DailyProfit> dailyProfitMap = new HashMap<>();

        ProfitStatistics(LocalDate startDate, LocalDate endDate) {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.toString();
                dailyProfitMap.put(dateStr, new DailyProfit(dateStr));
                currentDate = currentDate.plusDays(1);
            }
        }
    }

    /**
     * 商品利润内部类
     */
    private static class ProductProfit {
        String productName;
        String category;
        double revenue;
        double cost;
        double profit;
        int quantity;

        public ProductProfit(String productName, String category) {
            this.productName = productName;
            this.category = category;
        }
    }

    /**
     * 分类利润内部类
     */
    private static class CategoryProfit {
        String category;
        double revenue;
        double cost;
        double profit;

        public CategoryProfit(String category) {
            this.category = category;
        }
    }

    /**
     * 每日利润内部类
     */
    private static class DailyProfit {
        String date;
        double revenue;
        double cost;
        double profit;

        public DailyProfit(String date) {
            this.date = date;
        }
    }

    /**
     * 利润报表记录内部类
     */
    private static class ProfitReportRecord {
        String productName;
        String category;
        String date;
        double revenue;
        double cost;
        double profit;
        double margin;

        // 商品利润构造函数
        public ProfitReportRecord(String productName, String category,
                                 double revenue, double cost, double profit, double margin) {
            this.productName = productName;
            this.category = category;
            this.revenue = revenue;
            this.cost = cost;
            this.profit = profit;
            this.margin = margin;
        }

        // 分类利润构造函数
        public ProfitReportRecord(String category, double revenue, double cost, double profit, double margin) {
            this.category = category;
            this.revenue = revenue;
            this.cost = cost;
            this.profit = profit;
            this.margin = margin;
        }

        // 每日利润记录必须单独赋值 date，避免误用分类利润构造函数。
        public ProfitReportRecord(String date, double revenue, double cost,
                                  double profit, double margin, boolean dailyRecord) {
            this.date = date;
            this.revenue = revenue;
            this.cost = cost;
            this.profit = profit;
            this.margin = margin;
        }

        // 每日利润构造函数（使用不同参数顺序区分）
        public ProfitReportRecord(String date, String revenueStr, String costStr, String profitStr, String marginStr) {
            this.date = date;
            this.revenue = FormValidator.parseDouble(revenueStr);
            this.cost = FormValidator.parseDouble(costStr);
            this.profit = FormValidator.parseDouble(profitStr);
            this.margin = FormValidator.parseDouble(marginStr);
        }
    }
}
