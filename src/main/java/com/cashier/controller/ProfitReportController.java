package com.cashier.controller;

import com.cashier.dao.ProductDAO;
import com.cashier.dao.PurchaseInboundDAO;
import com.cashier.dao.PurchaseOrderDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Product;
import com.cashier.model.PurchaseInbound;
import com.cashier.model.PurchaseOrder;
import com.cashier.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.fxml.FXML;
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
    private static final Logger logger = LoggerFactory.getLogger(ProfitReportController.class);

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
    private Set<String> allCategories;

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

        // 设置表格列
        setupProductProfitTableColumns();
        setupCategoryProfitTableColumns();
        setupDailyProfitTableColumns();

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
     * 加载数据
     */
    private void loadData() {
        try {
            allProducts = ProductDAO.findAll();
            allTransactions = TransactionDAO.findAll();
            allInboundRecords = PurchaseInboundDAO.findAll();
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

            logger.info("成功加载 {} 个商品，{} 条交易记录，{} 条入库记录",
                allProducts.size(), allTransactions.size(), allInboundRecords.size());
        } catch (SQLException e) {
            logger.error("加载数据失败", e);
            showError("加载数据失败: " + e.getMessage());
            allProducts = new ArrayList<>();
            allTransactions = new ArrayList<>();
            allInboundRecords = new ArrayList<>();
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
                            // 查找商品成本
                            Product product = findProductByName(item.name);
                            double cost = product != null ? product.cost : item.price * 0.7; // 默认成本为售价的70%
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

        // 净利润（简化计算：假设运营成本为收入的5%）
        double operatingCost = totalRevenue * 0.05;
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

        // 更新表格
        updateProductProfitTable(productProfitMap);
        updateCategoryProfitTable(categoryProfitMap);
        updateDailyProfitTable(dailyProfitMap);
    }

    /**
     * 根据名称查找商品
     */
    private Product findProductByName(String name) {
        for (Product product : allProducts) {
            if (product.name.equals(name)) {
                return product;
            }
        }
        return null;
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