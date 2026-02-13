package com.cashier.controller;

import com.cashier.dao.TransactionDAO;
import com.cashier.model.Transaction;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.fxml.FXML;

import java.sql.SQLException;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 数据统计控制器
 * 处理销售统计和报表
 */
public class StatisticsController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(StatisticsController.class);

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> timeRangeComboBox;

    @FXML
    private Label totalSalesLabel;

    @FXML
    private Label transactionCountLabel;

    @FXML
    private Label avgTransactionLabel;

    @FXML
    private Label memberSalesLabel;

    @FXML
    private Label cashSalesLabel;

    @FXML
    private Label wechatSalesLabel;

    @FXML
    private Label alipaySalesLabel;

    @FXML
    private Label cardSalesLabel;

    @FXML
    private Label topProductLabel;

    @FXML
    private Label topProductCountLabel;

    @FXML
    private Label topProductAmountLabel;

    @FXML
    private TableView<StatisticsRecord> categoryTable;

    @FXML
    private TableColumn<StatisticsRecord, String> categoryNameColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> categoryCountColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> categoryAmountColumn;

    @FXML
    private TableView<StatisticsRecord> hourlyTable;

    @FXML
    private TableColumn<StatisticsRecord, String> hourColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> hourCountColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> hourAmountColumn;

    @FXML
    private Button queryButton;

    @FXML
    private Button exportButton;

    private List<Transaction> allTransactions;

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
        timeRangeComboBox.getSelectionModel().select(0);

        // 设置默认日期范围（今天）
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);

        // 设置表格列
        setupCategoryTableColumns();
        setupHourlyTableColumns();

        // 加载交易数据
        loadTransactions();

        // 执行查询
        handleQuery();

        // 监听时间范围变化
        timeRangeComboBox.setOnAction(event -> handleTimeRangeChange());
    }

    /**
     * 设置分类表格列
     */
    private void setupCategoryTableColumns() {
        categoryNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
        categoryCountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().count)));
        categoryAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%.2f", cellData.getValue().amount)));
    }

    /**
     * 设置小时表格列
     */
    private void setupHourlyTableColumns() {
        hourColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
        hourCountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().count)));
        hourAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%.2f", cellData.getValue().amount)));
    }

    /**
     * 加载交易数据
     */
    private void loadTransactions() {
        logger.info("StatisticsController: 开始加载交易数据...");
        try {
            allTransactions = TransactionDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载交易数据失败", e);
            showError("加载交易数据失败: " + e.getMessage());
            allTransactions = new java.util.ArrayList<>();
        }
        logger.info("StatisticsController: 加载了 {} 条交易记录", allTransactions.size());
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

        // 筛选交易记录
        List<Transaction> filteredTransactions = filterTransactionsByDate(startDate, endDate);

        // 计算统计数据
        calculateStatistics(filteredTransactions);
    }

    /**
     * 按日期筛选交易记录
     */
    private List<Transaction> filterTransactionsByDate(LocalDate startDate, LocalDate endDate) {
        List<Transaction> filtered = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Transaction t : allTransactions) {
            try {
                Date date = sdf.parse(t.timestamp);
                LocalDate localDate = date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

                if (!localDate.isBefore(startDate) && !localDate.isAfter(endDate)) {
                    filtered.add(t);
                }
            } catch (Exception e) {
                // 日期解析失败，跳过该记录
            }
        }

        return filtered;
    }

    /**
     * 计算统计数据
     */
    private void calculateStatistics(List<Transaction> transactions) {
        // 总销售额
        double totalSales = 0.0;
        int transactionCount = transactions.size();

        // 按支付方式统计
        double cashSales = 0.0;
        double wechatSales = 0.0;
        double alipaySales = 0.0;
        double cardSales = 0.0;
        double memberSales = 0.0;

        // 商品统计
        Map<String, Integer> productCountMap = new HashMap<>();
        Map<String, Double> productAmountMap = new HashMap<>();

        // 分类统计
        Map<String, Integer> categoryCountMap = new HashMap<>();
        Map<String, Double> categoryAmountMap = new HashMap<>();

        // 小时统计
        Map<Integer, Integer> hourCountMap = new HashMap<>();
        Map<Integer, Double> hourAmountMap = new HashMap<>();

        for (Transaction t : transactions) {
            totalSales += t.finalAmount;

            // 支付方式统计
            switch (t.paymentMethod) {
                case "现金":
                    cashSales += t.finalAmount;
                    break;
                case "微信":
                    wechatSales += t.finalAmount;
                    break;
                case "支付宝":
                    alipaySales += t.finalAmount;
                    break;
                case "银行卡":
                    cardSales += t.finalAmount;
                    break;
            }

            // 会员统计
            if (t.memberPhone != null && !t.memberPhone.isEmpty()) {
                memberSales += t.finalAmount;
            }

            // 商品统计
            if (t.items != null) {
                for (var item : t.items) {
                    productCountMap.put(item.name, productCountMap.getOrDefault(item.name, 0) + item.quantity);
                    productAmountMap.put(item.name, productAmountMap.getOrDefault(item.name, 0.0) + item.price * item.quantity);

                    // 分类统计
                    String category = item.category;
                    if (category == null || category.isEmpty()) {
                        category = "未分类";
                    }
                    categoryCountMap.put(category, categoryCountMap.getOrDefault(category, 0) + item.quantity);
                    categoryAmountMap.put(category, categoryAmountMap.getOrDefault(category, 0.0) + item.price * item.quantity);
                }
            }

            // 小时统计
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = sdf.parse(t.timestamp);
                // 使用 Calendar API 替代已过时的 Date.getHours()
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hourCountMap.put(hour, hourCountMap.getOrDefault(hour, 0) + 1);
                hourAmountMap.put(hour, hourAmountMap.getOrDefault(hour, 0.0) + t.finalAmount);
            } catch (Exception e) {
                // 解析失败，跳过
            }
        }

        // 更新UI
        totalSalesLabel.setText(String.format("¥%.2f", totalSales));
        transactionCountLabel.setText(String.valueOf(transactionCount));
        avgTransactionLabel.setText(transactionCount > 0 ? String.format("¥%.2f", totalSales / transactionCount) : "¥0.00");
        memberSalesLabel.setText(String.format("¥%.2f", memberSales));
        cashSalesLabel.setText(String.format("¥%.2f", cashSales));
        wechatSalesLabel.setText(String.format("¥%.2f", wechatSales));
        alipaySalesLabel.setText(String.format("¥%.2f", alipaySales));
        cardSalesLabel.setText(String.format("¥%.2f", cardSales));

        // 找出销售最多的商品
        if (!productCountMap.isEmpty()) {
            String topProduct = Collections.max(productCountMap.entrySet(), Map.Entry.comparingByValue()).getKey();
            topProductLabel.setText(topProduct);
            topProductCountLabel.setText(String.valueOf(productCountMap.get(topProduct)));
            topProductAmountLabel.setText(String.format("¥%.2f", productAmountMap.get(topProduct)));
        } else {
            topProductLabel.setText("无");
            topProductCountLabel.setText("0");
            topProductAmountLabel.setText("¥0.00");
        }

        // 更新分类表格
        updateCategoryTable(categoryCountMap, categoryAmountMap);

        // 更新小时表格
        updateHourlyTable(hourCountMap, hourAmountMap);
    }

    /**
     * 更新分类表格
     */
    private void updateCategoryTable(Map<String, Integer> countMap, Map<String, Double> amountMap) {
        javafx.collections.ObservableList<StatisticsRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (String category : countMap.keySet()) {
            list.add(new StatisticsRecord(category, countMap.get(category), amountMap.get(category)));
        }

        // 按金额排序
        list.sort((a, b) -> Double.compare(b.amount, a.amount));
        categoryTable.setItems(list);
    }

    /**
     * 更新小时表格
     */
    private void updateHourlyTable(Map<Integer, Integer> countMap, Map<Integer, Double> amountMap) {
        javafx.collections.ObservableList<StatisticsRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (int hour = 0; hour < 24; hour++) {
            if (countMap.containsKey(hour)) {
                list.add(new StatisticsRecord(
                    String.format("%02d:00-%02d:00", hour, hour + 1),
                    countMap.get(hour),
                    amountMap.get(hour)
                ));
            }
        }

        hourlyTable.setItems(list);
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
     * @param message 错误消息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 统计记录内部类
     */
    private static class StatisticsRecord {
        String name;
        int count;
        double amount;

        public StatisticsRecord(String name, int count, double amount) {
            this.name = name;
            this.count = count;
            this.amount = amount;
        }
    }
}