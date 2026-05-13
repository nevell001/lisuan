package com.cashier.controller;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.ProductDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.util.CurrencyUtil;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
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

    private List<Product> allProducts;
    private List<Transaction> allTransactions;
    private Set<String> allCategories;

    // 默认阈值
    private static final double DEFAULT_TURNOVER_THRESHOLD = 1.0;  // 周转率阈值
    private static final int DEFAULT_SLOW_SALES_THRESHOLD = 10;   // 滞销阈值（销量）
    private static final int DEFAULT_INVENTORY_DAYS = 90;          // 库存天数阈值

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
            new javafx.beans.property.SimpleStringProperty(String.format("%.2f", cellData.getValue().turnoverRate)));
        inventoryDaysColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("%.0f", cellData.getValue().inventoryDays)));
        statusColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().status));
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
            new javafx.beans.property.SimpleStringProperty(String.format("%.0f", cellData.getValue().inventoryDays)));
    }

    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 库存状态分布饼图
        stockStatusPieChart.setTitle("库存状态分布");
        stockStatusPieChart.setLegendSide(javafx.geometry.Side.RIGHT);

        // 库存周转率分析柱状图
        turnoverRateBarChart.setTitle("库存周转率分析");
        turnoverRateBarChart.getXAxis().setLabel("商品");
        turnoverRateBarChart.getYAxis().setLabel("周转率");
        turnoverRateBarChart.setLegendVisible(false);

        // 分类库存价值对比柱状图
        categoryStockValueBarChart.setTitle("分类库存价值对比");
        categoryStockValueBarChart.getXAxis().setLabel("分类");
        categoryStockValueBarChart.getYAxis().setLabel("库存价值（元）");
        categoryStockValueBarChart.setLegendVisible(false);
    }

    /**
     * 加载数据
     */
    private void loadData() {
        try {
            allProducts = ProductDAO.findAll();
            allTransactions = TransactionDAO.findAll();
            allCategories = new TreeSet<>();

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

            logger.info("成功加载 {} 个商品，{} 条交易记录", allProducts.size(), allTransactions.size());
        } catch (SQLException e) {
            logger.error("加载数据失败", e);
            showError("加载数据失败: " + e.getMessage());
            allProducts = new ArrayList<>();
            allTransactions = new ArrayList<>();
            allCategories = new TreeSet<>();
        }
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
            showError("请选择日期范围！");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showError("开始日期不能晚于结束日期！");
            return;
        }

        // 获取阈值
        double turnoverThreshold;
        int slowSalesThreshold;
        int inventoryDaysThreshold;

        try {
            turnoverThreshold = Double.parseDouble(turnoverThresholdField.getText());
            slowSalesThreshold = Integer.parseInt(slowSalesThresholdField.getText());
            inventoryDaysThreshold = Integer.parseInt(inventoryDaysField.getText());
        } catch (NumberFormatException e) {
            showError("阈值输入格式错误！");
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

            totalProducts++;
            double stockValue = product.getCost().multiply(BigDecimal.valueOf(product.quantity)).doubleValue();
            totalStockValue += stockValue;

            // 统计销售数量
            int salesQuantity = calculateSalesQuantity(product.name, startDate, endDate);

            // 计算周转率 = 销售数量 / 平均库存
            // 平均库存 = (期初库存 + 期末库存) / 2
            // 这里简化为：周转率 = 销售数量 / 当前库存 * (365 / 分析天数)
            double turnoverRate = product.quantity > 0
                ? (salesQuantity / (double) product.quantity) * (365.0 / daysBetween)
                : 0.0;
            totalTurnoverRate += turnoverRate;

            // 计算库存天数
            double inventoryDays = salesQuantity > 0
                ? (product.quantity / (double) salesQuantity) * daysBetween
                : 999.0; // 如果没有销售，设为999天

            // 库存状态
            String status = "正常";
            if (product.quantity <= product.minStock) {
                status = "库存不足";
                lowStockCount++;
            }
            if (salesQuantity < slowSalesThreshold) {
                status = "滞销";
                slowSalesCount++;
            }
            if (inventoryDays > inventoryDaysThreshold) {
                status = "积压";
                overstockCount++;
            }

            // 获取最后销售日期
            String lastSaleDate = getLastSaleDate(product.name);

            // 商品记录
            productRecords.add(new InventoryReportRecord(
                product.name,
                product.category != null ? product.category : "未分类",
                product.quantity,
                stockValue,
                salesQuantity,
                turnoverRate,
                inventoryDays,
                status
            ));

            // 更新分类统计
            String category = product.category != null ? product.category : "未分类";
            categoryQuantityMap.put(category, categoryQuantityMap.getOrDefault(category, 0) + product.quantity);
            categoryAmountMap.put(category, categoryAmountMap.getOrDefault(category, 0.0) + stockValue);

            // 滞销商品记录
            if (salesQuantity < slowSalesThreshold) {
                slowSalesRecords.add(new InventoryReportRecord(
                    product.name,
                    product.category != null ? product.category : "未分类",
                    product.quantity,
                    salesQuantity,
                    lastSaleDate
                ));
            }

            // 积压商品记录
            if (inventoryDays > inventoryDaysThreshold) {
                overstockRecords.add(new InventoryReportRecord(
                    product.name,
                    product.category != null ? product.category : "未分类",
                    product.quantity,
                    stockValue,
                    inventoryDays
                ));
            }
        }

        // 计算平均周转率
        double avgTurnoverRate = totalProducts > 0 ? totalTurnoverRate / totalProducts : 0.0;

        // 更新统计卡片
        totalProductsLabel.setText(String.valueOf(totalProducts));
        totalStockValueLabel.setText(CurrencyUtil.format(totalStockValue));
        avgTurnoverRateLabel.setText(String.format("%.2f", avgTurnoverRate));
        lowStockCountLabel.setText(String.valueOf(lowStockCount));
        slowSalesCountLabel.setText(String.valueOf(slowSalesCount));
        overstockCountLabel.setText(String.valueOf(overstockCount));

        // 更新表格
        updateProductTable(productRecords);
        updateSlowSalesTable(slowSalesRecords);
        updateOverstockTable(overstockRecords);

        // 更新图表
        updateCharts(productRecords, categoryQuantityMap, categoryAmountMap);
    }

    /**
     * 计算销售数量
     */
    private int calculateSalesQuantity(String productName, LocalDate startDate, LocalDate endDate) {
        int salesQuantity = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Transaction transaction : allTransactions) {
            try {
                Date date = sdf.parse(transaction.timestamp);
                LocalDate localDate = date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

                if (!localDate.isBefore(startDate) && !localDate.isAfter(endDate)) {
                    if (transaction.items != null) {
                        for (var item : transaction.items) {
                            if (item.name.equals(productName)) {
                                salesQuantity += item.quantity;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 日期解析失败，跳过该记录
            }
        }

        return salesQuantity;
    }

    /**
     * 获取最后销售日期
     */
    private String getLastSaleDate(String productName) {
        String lastSaleDate = "从未销售";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date maxDate = null;

        for (Transaction transaction : allTransactions) {
            if (transaction.items != null) {
                for (var item : transaction.items) {
                    if (item.name.equals(productName)) {
                        try {
                            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(transaction.timestamp);
                            if (maxDate == null || date.after(maxDate)) {
                                maxDate = date;
                            }
                        } catch (Exception e) {
                            // 日期解析失败，跳过
                        }
                    }
                }
            }
        }

        if (maxDate != null) {
            lastSaleDate = sdf.format(maxDate);
        }

        return lastSaleDate;
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
            pieChartData.add(new PieChart.Data("正常", normalCount));
        }
        if (lowStockCount > 0) {
            pieChartData.add(new PieChart.Data("库存不足", lowStockCount));
        }
        if (slowSalesCount > 0) {
            pieChartData.add(new PieChart.Data("滞销", slowSalesCount));
        }
        if (overstockCount > 0) {
            pieChartData.add(new PieChart.Data("积压", overstockCount));
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
        series.setName("周转率");
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
        series.setName("库存价值");
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
        exportDialog.setTitle("选择导出内容");
        exportDialog.setHeaderText("请选择要导出的内容");
        exportDialog.setContentText("导出内容:");

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
            showError("没有可导出的商品数据");
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
                        String.format("%.2f", record.turnoverRate),
                        String.format("%.1f", record.inventoryDays),
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
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("导出成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("文件已成功导出到:\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("库存商品统计报表导出成功: {}", filePath);
                } else {
                    showError("导出失败，请查看日志获取详细信息");
                }
            } catch (Exception e) {
                logger.error("导出库存商品统计报表失败", e);
                showError("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 导出滞销商品
     */
    private void exportSlowMovingProducts() {
        if (slowSalesTable.getItems().isEmpty()) {
            showError("没有可导出的滞销商品数据");
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
                        String.format("%.1f", record.inventoryDays)
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
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("导出成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("文件已成功导出到:\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("滞销商品报表导出成功: {}", filePath);
                } else {
                    showError("导出失败，请查看日志获取详细信息");
                }
            } catch (Exception e) {
                logger.error("导出滞销商品报表失败", e);
                showError("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 导出库存积压
     */
    private void exportOverstockProducts() {
        if (overstockTable.getItems().isEmpty()) {
            showError("没有可导出的库存积压数据");
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
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("导出成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("文件已成功导出到:\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("库存积压报表导出成功: {}", filePath);
                } else {
                    showError("导出失败，请查看日志获取详细信息");
                }
            } catch (Exception e) {
                logger.error("导出库存积压报表失败", e);
                showError("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nManager.getInstance().get("label.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
}