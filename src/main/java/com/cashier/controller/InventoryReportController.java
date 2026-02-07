package com.cashier.controller;

import com.cashier.dao.ProductDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
    private static final Logger logger = LoggerFactory.getLogger(InventoryReportController.class);

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
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().stockValue)));
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
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().stockValue)));
        overstockDaysColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("%.0f", cellData.getValue().inventoryDays)));
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

        // 计算天数
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

        for (Product product : allProducts) {
            // 分类筛选
            if (categoryName != null && !categoryName.equals("全部分类") &&
                !categoryName.equals(product.category)) {
                continue;
            }

            totalProducts++;
            double stockValue = product.quantity * product.cost;
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
        totalStockValueLabel.setText(String.format("¥%,.2f", totalStockValue));
        avgTurnoverRateLabel.setText(String.format("%.2f", avgTurnoverRate));
        lowStockCountLabel.setText(String.valueOf(lowStockCount));
        slowSalesCountLabel.setText(String.valueOf(slowSalesCount));
        overstockCountLabel.setText(String.valueOf(overstockCount));

        // 更新表格
        updateProductTable(productRecords);
        updateSlowSalesTable(slowSalesRecords);
        updateOverstockTable(overstockRecords);
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
     * 处理导出
     */
    @FXML
    private void handleExport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("导出");
        alert.setHeaderText(null);
        alert.setContentText("导出功能正在开发中...");
        alert.showAndWait();
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