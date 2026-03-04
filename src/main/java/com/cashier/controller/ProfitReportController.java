package com.cashier.controller;

import com.cashier.dao.ProductDAO;
import com.cashier.dao.PurchaseInboundDAO;
import com.cashier.dao.PurchaseInboundItemDAO;
import com.cashier.dao.PurchaseOrderDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Product;
import com.cashier.model.PurchaseInbound;
import com.cashier.model.PurchaseInboundItem;
import com.cashier.model.PurchaseOrder;
import com.cashier.model.Transaction;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 利润分析控制器
 * 处理采购成本、销售收入、毛利率分析
 */
public class ProfitReportController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ProfitReportController.class);

    // 运营成本比例（默认为收入的5%）
    private static final double DEFAULT_OPERATING_COST_RATIO = 0.05;

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
    private Label bestMarginProductLabel;

    @FXML
    private Label bestMarginValueLabel;

    @FXML
    private Label worstMarginProductLabel;

    @FXML
    private Label worstMarginValueLabel;

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
    private TableColumn<ProfitReportRecord, String> categoryColumn;

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

    private List<Product> allProducts;
    private List<Transaction> allTransactions;
    private List<PurchaseInbound> allInboundRecords;
    private List<PurchaseInboundItem> allInboundItems;
    private Map<String, Double> productActualCostMap; // 商品实际成本（加权平均）
    private Map<String, Product> productNameMap; // 商品名称到商品的映射
    private Set<String> allCategories;

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
        categoryColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().category));
        salesRevenueColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().revenue)));
        salesCostColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().cost)));
        salesProfitColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().profit)));
        salesMarginColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("%.2f%%", cellData.getValue().margin * 100)));
    }

    /**
     * 设置分类利润表格列
     */
    private void setupCategoryProfitTableColumns() {
        categoryNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().category));
        categoryRevenueColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().revenue)));
        categoryCostColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().cost)));
        categoryProfitColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().profit)));
        categoryMarginColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("%.2f%%", cellData.getValue().margin * 100)));
    }

    /**
     * 设置每日利润表格列
     */
    private void setupDailyProfitTableColumns() {
        dateColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().date));
        dailyRevenueColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().revenue)));
        dailyCostColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().cost)));
        dailyProfitColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().profit)));
        dailyMarginColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("%.2f%%", cellData.getValue().margin * 100)));
    }

    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 利润构成饼图
        profitCompositionPieChart.setTitle("利润构成");
        profitCompositionPieChart.setLegendSide(javafx.geometry.Side.RIGHT);

        // 每日利润趋势折线图
        dailyProfitTrendLineChart.setTitle("每日利润趋势");
        dailyProfitTrendLineChart.getXAxis().setLabel("日期");
        dailyProfitTrendLineChart.getYAxis().setLabel("利润（元）");
        dailyProfitTrendLineChart.setCreateSymbols(false);
        dailyProfitTrendLineChart.setLegendVisible(true);

        // 分类利润对比柱状图
        categoryProfitBarChart.setTitle("分类利润对比");
        categoryProfitBarChart.getXAxis().setLabel("分类");
        categoryProfitBarChart.getYAxis().setLabel("利润（元）");
        categoryProfitBarChart.setLegendVisible(false);
    }

    /**
     * 加载数据
     */
    private void loadData() {
        try {
            allProducts = ProductDAO.findAll();
            allTransactions = TransactionDAO.findAll();
            allInboundRecords = PurchaseInboundDAO.findAll();
            allInboundItems = new ArrayList<>();
            productActualCostMap = new HashMap<>();
            productNameMap = new HashMap<>();
            allCategories = new TreeSet<>();

            // 加载所有采购入库明细
            for (PurchaseInbound inbound : allInboundRecords) {
                List<PurchaseInboundItem> items = PurchaseInboundItemDAO.findByInboundId(inbound.id);
                allInboundItems.addAll(items);
            }

            // 计算每个商品的加权平均成本
            calculateProductActualCosts();

            // 构建商品名称映射（用于快速查找）
            productNameMap = new HashMap<>();
            for (Product product : allProducts) {
                if (product.name != null && !product.name.isEmpty()) {
                    productNameMap.put(product.name, product);
                }
            }

            // 收集所有分类
            for (Product product : allProducts) {
                if (product.category != null && !product.category.isEmpty()) {
                    allCategories.add(product.category);
                }
            }

            // 加载分类列表到下拉框
            javafx.collections.ObservableList<String> categoryList = javafx.collections.FXCollections.observableArrayList();
            categoryList.add("全部分类");
            categoryList.addAll(allCategories);
            categoryComboBox.setItems(categoryList);
            categoryComboBox.getSelectionModel().select(0);

            logger.info("成功加载 {} 个商品，{} 条交易记录，{} 条入库记录，{} 条入库明细",
                allProducts.size(), allTransactions.size(), allInboundRecords.size(), allInboundItems.size());
        } catch (SQLException e) {
            logger.error("加载数据失败", e);
            showError("加载数据失败: " + e.getMessage());
            allProducts = new ArrayList<>();
            allTransactions = new ArrayList<>();
            allInboundRecords = new ArrayList<>();
            allInboundItems = new ArrayList<>();
            productActualCostMap = new HashMap<>();
            productNameMap = new HashMap<>();
            allCategories = new TreeSet<>();
        }
    }

    /**
     * 计算每个商品的加权平均成本
     */
    private void calculateProductActualCosts() {
        Map<Integer, Double> totalCostMap = new HashMap<>();
        Map<Integer, Integer> totalQuantityMap = new HashMap<>();

        // 统计每个商品的总成本和总数量
        for (PurchaseInboundItem item : allInboundItems) {
            if (item.productId > 0) {
                double cost = item.unitPrice.doubleValue();
                totalCostMap.put(item.productId, totalCostMap.getOrDefault(item.productId, 0.0) + cost * item.quantity);
                totalQuantityMap.put(item.productId, totalQuantityMap.getOrDefault(item.productId, 0) + item.quantity);
            }
        }

        // 计算加权平均成本并映射到商品名称
        for (Product product : allProducts) {
            if (totalCostMap.containsKey(product.id) && totalQuantityMap.get(product.id) > 0) {
                double avgCost = totalCostMap.get(product.id) / totalQuantityMap.get(product.id);
                productActualCostMap.put(product.name, avgCost);
            }
        }

        logger.info("计算了 {} 个商品的实际成本", productActualCostMap.size());
    }

    /**
     * 处理时间范围变化
     */
    private void handleTimeRangeChange() {
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
        }
    }

    /**
     * 处理查询
     */
    @FXML
    private void handleQuery() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showError("请选择日期范围！");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showError("开始日期不能晚于结束日期！");
            return;
        }

        String selectedCategory = categoryComboBox.getSelectionModel().getSelectedItem();

        // 计算统计数据
        calculateStatistics(startDate, endDate, selectedCategory);
    }

    /**
     * 计算统计数据
     */
    private void calculateStatistics(LocalDate startDate, LocalDate endDate, String categoryName) {
        // 总收入、总成本
        double totalRevenue = 0.0;
        double totalCost = 0.0;

        // 商品利润统计
        Map<String, ProductProfit> productProfitMap = new HashMap<>();

        // 分类利润统计
        Map<String, CategoryProfit> categoryProfitMap = new HashMap<>();

        // 每日利润统计
        Map<String, DailyProfit> dailyProfitMap = new HashMap<>();

        // 初始化每日利润统计
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.toString();
            dailyProfitMap.put(dateStr, new DailyProfit(dateStr));
            currentDate = currentDate.plusDays(1);
        }

        // 统计销售数据
        for (Transaction transaction : allTransactions) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = sdf.parse(transaction.timestamp);
                LocalDate localDate = date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

                if (!localDate.isBefore(startDate) && !localDate.isAfter(endDate)) {
                    String dateStr = localDate.toString();

                    if (transaction.items != null) {
                        for (var item : transaction.items) {
                            // 查找商品信息
                            Product product = findProductByName(item.name);
                            double cost;

                            // 优先使用实际采购成本（加权平均）
                            if (productActualCostMap.containsKey(item.name)) {
                                cost = productActualCostMap.get(item.name);
                            } else if (product != null && product.cost > 0) {
                                // 使用商品成本价
                                cost = product.cost;
                            } else {
                                // 默认成本为售价的70%（仅用于没有采购记录的商品）
                                cost = item.price * 0.7;
                            }

                            String category = product != null && product.category != null ? product.category : "未分类";

                            // 分类筛选
                            if (categoryName != null && !categoryName.equals("全部分类") &&
                                !categoryName.equals(category)) {
                                continue;
                            }

                            double revenue = item.price * item.quantity;
                            double itemCost = cost * item.quantity;
                            double profit = revenue - itemCost;

                            totalRevenue += revenue;
                            totalCost += itemCost;

                            // 商品利润统计
                            productProfitMap.putIfAbsent(item.name, new ProductProfit(item.name, category));
                            ProductProfit pp = productProfitMap.get(item.name);
                            pp.revenue += revenue;
                            pp.cost += itemCost;
                            pp.profit += profit;
                            pp.quantity += item.quantity;

                            // 分类利润统计
                            categoryProfitMap.putIfAbsent(category, new CategoryProfit(category));
                            CategoryProfit cp = categoryProfitMap.get(category);
                            cp.revenue += revenue;
                            cp.cost += itemCost;
                            cp.profit += profit;

                            // 每日利润统计
                            if (dailyProfitMap.containsKey(dateStr)) {
                                DailyProfit dp = dailyProfitMap.get(dateStr);
                                dp.revenue += revenue;
                                dp.cost += itemCost;
                                dp.profit += profit;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("处理交易记录失败: {}", transaction.transactionId, e);
            }
        }

        // 计算毛利率
        double grossProfit = totalRevenue - totalCost;
        double grossMargin = totalRevenue > 0 ? grossProfit / totalRevenue : 0.0;

        // 净利润计算（毛利润 - 运营成本）
        // 运营成本包括：人工、水电、租金等，这里使用固定比例估算
        // TODO: 可以从配置文件读取或让用户自定义运营成本比例
        double operatingCost = totalRevenue * DEFAULT_OPERATING_COST_RATIO;
        double netProfit = grossProfit - operatingCost;

        // 平均毛利率
        double avgMargin = !productProfitMap.isEmpty()
            ? productProfitMap.values().stream()
                .mapToDouble(pp -> pp.revenue > 0 ? pp.profit / pp.revenue : 0.0)
                .average()
                .orElse(0.0)
            : 0.0;

        // 找出毛利率最高和最低的商品
        String bestMarginProduct = "无";
        double bestMarginValue = 0.0;
        String worstMarginProduct = "无";
        double worstMarginValue = 1.0;

        for (ProductProfit pp : productProfitMap.values()) {
            double margin = pp.revenue > 0 ? pp.profit / pp.revenue : 0.0;
            if (margin > bestMarginValue && pp.quantity > 0) {
                bestMarginValue = margin;
                bestMarginProduct = pp.productName;
            }
            if (margin < worstMarginValue && pp.quantity > 0) {
                worstMarginValue = margin;
                worstMarginProduct = pp.productName;
            }
        }

        // 更新统计卡片
        totalRevenueLabel.setText(String.format("¥%,.2f", totalRevenue));
        totalCostLabel.setText(String.format("¥%,.2f", totalCost));
        grossProfitLabel.setText(String.format("¥%,.2f", grossProfit));
        grossMarginLabel.setText(String.format("%.2f%%", grossMargin * 100));
        netProfitLabel.setText(String.format("¥%,.2f", netProfit));
        avgMarginLabel.setText(String.format("%.2f%%", avgMargin * 100));
        bestMarginProductLabel.setText(bestMarginProduct);
        bestMarginValueLabel.setText(String.format("%.2f%%", bestMarginValue * 100));
        worstMarginProductLabel.setText(worstMarginProduct);
        worstMarginValueLabel.setText(String.format("%.2f%%", worstMarginValue * 100));

        // 记录成本来源统计信息
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

        // 更新表格
        updateProductProfitTable(productProfitMap);
        updateCategoryProfitTable(categoryProfitMap);
        updateDailyProfitTable(dailyProfitMap);

        // 更新图表
        updateCharts(totalRevenue, totalCost, grossProfit, operatingCost, netProfit, categoryProfitMap, dailyProfitMap);
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
                margin
            ));
        }

        dailyProfitTable.setItems(list);
    }

    /**
     * 更新图表
     */
    private void updateCharts(double totalRevenue, double totalCost, double grossProfit,
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
            pieChartData.add(new PieChart.Data("毛利润", grossProfit));
        }
        if (operatingCost > 0) {
            pieChartData.add(new PieChart.Data("运营成本", operatingCost));
        }
        if (netProfit > 0) {
            pieChartData.add(new PieChart.Data("净利润", netProfit));
        }

        profitCompositionPieChart.setData(pieChartData);
    }

    /**
     * 更新每日利润趋势折线图
     */
    private void updateDailyProfitTrendChart(Map<String, DailyProfit> dailyProfitMap) {
        XYChart.Series<String, Number> grossProfitSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> netProfitSeries = new XYChart.Series<>();

        grossProfitSeries.setName("毛利润");
        netProfitSeries.setName("净利润");

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
        series.setName("利润");

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
    private void handleExport() {
        // 显示导出选项对话框
        ChoiceDialog<String> exportDialog = new ChoiceDialog<>(
            "商品利润", "商品利润", "分类利润", "每日利润"
        );
        exportDialog.setTitle("选择导出内容");
        exportDialog.setHeaderText("请选择要导出的内容");
        exportDialog.setContentText("导出内容:");

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
            showError("没有可导出的商品利润数据");
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle("选择导出格式");
        formatDialog.setHeaderText("请选择导出格式");
        formatDialog.setContentText("格式:");

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "商品名称", "商品分类", "销售收入", "销售成本", "毛利润", "毛利率(%)"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (ProfitReportRecord record : productProfitTable.getItems()) {
                    double profitMargin = record.revenue > 0 ? (record.profit / record.revenue) * 100 : 0;
                    data.add(new String[]{
                        record.productName != null ? record.productName : "",
                        record.category != null ? record.category : "",
                        String.format("¥%.2f", record.revenue),
                        String.format("¥%.2f", record.cost),
                        String.format("¥%.2f", record.profit),
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
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("导出成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("文件已成功导出到:\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("商品利润分析报表导出成功: {}", filePath);
                } else {
                    showError("导出失败，请查看日志获取详细信息");
                }
            } catch (Exception e) {
                logger.error("导出商品利润分析报表失败", e);
                showError("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 导出分类利润
     */
    private void exportCategoryProfit() {
        if (categoryProfitTable.getItems().isEmpty()) {
            showError("没有可导出的分类利润数据");
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle("选择导出格式");
        formatDialog.setHeaderText("请选择导出格式");
        formatDialog.setContentText("格式:");

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "商品分类", "销售收入", "销售成本", "毛利润", "毛利率(%)"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (ProfitReportRecord record : categoryProfitTable.getItems()) {
                    double profitMargin = record.revenue > 0 ? (record.profit / record.revenue) * 100 : 0;
                    data.add(new String[]{
                        record.category != null ? record.category : "",
                        String.format("¥%.2f", record.revenue),
                        String.format("¥%.2f", record.cost),
                        String.format("¥%.2f", record.profit),
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
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("导出成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("文件已成功导出到:\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("分类利润分析报表导出成功: {}", filePath);
                } else {
                    showError("导出失败，请查看日志获取详细信息");
                }
            } catch (Exception e) {
                logger.error("导出分类利润分析报表失败", e);
                showError("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 导出每日利润
     */
    private void exportDailyProfit() {
        if (dailyProfitTable.getItems().isEmpty()) {
            showError("没有可导出的每日利润数据");
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle("选择导出格式");
        formatDialog.setHeaderText("请选择导出格式");
        formatDialog.setContentText("格式:");

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "日期", "销售收入", "销售成本", "毛利润", "毛利率(%)", "净利润"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (ProfitReportRecord record : dailyProfitTable.getItems()) {
                    double profitMargin = record.revenue > 0 ? (record.profit / record.revenue) * 100 : 0;
                    data.add(new String[]{
                        record.date != null ? record.date : "",
                        String.format("¥%.2f", record.revenue),
                        String.format("¥%.2f", record.cost),
                        String.format("¥%.2f", record.profit),
                        String.format("%.2f", profitMargin),
                        String.format("¥%.2f", record.profit * 0.95) // 假设净利润为毛利润的95%
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
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("导出成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("文件已成功导出到:\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("每日利润分析报表导出成功: {}", filePath);
                } else {
                    showError("导出失败，请查看日志获取详细信息");
                }
            } catch (Exception e) {
                logger.error("导出每日利润分析报表失败", e);
                showError("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

        // 每日利润构造函数（使用不同参数顺序区分）
        public ProfitReportRecord(String date, String revenueStr, String costStr, String profitStr, String marginStr) {
            this.date = date;
            this.revenue = Double.parseDouble(revenueStr);
            this.cost = Double.parseDouble(costStr);
            this.profit = Double.parseDouble(profitStr);
            this.margin = Double.parseDouble(marginStr);
        }
    }
}