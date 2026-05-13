package com.cashier.controller;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.service.ReturnService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.LoggerFactoryUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 退货报表统计控制器
 */
public class ReturnReportController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnReportController.class);

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private Button generateButton;
    @FXML private Button exportButton;
    @FXML private Button refreshButton;

    @FXML private Label totalReturnOrdersLabel;
    @FXML private Label totalReturnAmountLabel;
    @FXML private Label approvedOrdersLabel;
    @FXML private Label rejectedOrdersLabel;
    @FXML private Label completedOrdersLabel;
    @FXML private Label pendingOrdersLabel;
    @FXML private Label avgReturnAmountLabel;

    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> returnTrendBarChart;
    @FXML private BarChart<String, Number> categoryReturnBarChart;

    @FXML private TableView<ReturnOrder> returnOrderTable;
    @FXML private TableColumn<ReturnOrder, String> returnOrderIdColumn;
    @FXML private TableColumn<ReturnOrder, String> memberNameColumn;
    @FXML private TableColumn<ReturnOrder, String> returnDateColumn;
    @FXML private TableColumn<ReturnOrder, String> totalAmountColumn;
    @FXML private TableColumn<ReturnOrder, String> statusColumn;
    @FXML private TableColumn<ReturnOrder, String> operatorNameColumn;

    private ObservableList<ReturnOrder> returnOrderList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        logger.info("初始化退货报表统计控制器");

        // 初始化报表类型下拉框
        reportTypeComboBox.setItems(FXCollections.observableArrayList(
            "全部报表", "今日报表", "本周报表", "本月报表", "自定义日期"
        ));
        reportTypeComboBox.setValue("本月报表");

        // 设置默认日期范围（本月）
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        startDatePicker.setValue(firstDayOfMonth);
        endDatePicker.setValue(now);

        // 初始化表格列
        initializeTableColumns();

        // 初始化图表
        initializeCharts();

        // 监听报表类型变化
        reportTypeComboBox.setOnAction(event -> handleReportTypeChange());

        // 监听日期变化
        startDatePicker.setOnAction(event -> generateReport());
        endDatePicker.setOnAction(event -> generateReport());

        // 生成报表
        generateReport();
    }

    private void initializeTableColumns() {
        returnOrderIdColumn.setCellValueFactory(new PropertyValueFactory<>("returnOrderId"));
        memberNameColumn.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        returnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDateFormatted"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmountFormatted"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        operatorNameColumn.setCellValueFactory(new PropertyValueFactory<>("operatorName"));

        // 自定义显示格式
        returnDateColumn.setCellFactory(column -> new TableCell<ReturnOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    try {
                        Date date = new Date(Long.parseLong(item));
                        setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
                    } catch (Exception e) {
                        setText(item);
                    }
                }
            }
        });

        totalAmountColumn.setCellFactory(column -> new TableCell<ReturnOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    try {
                        double amount = Double.parseDouble(item);
                        setText(CurrencyUtil.format(amount));
                    } catch (Exception e) {
                        setText(item);
                    }
                }
            }
        });

        statusColumn.setCellFactory(column -> new TableCell<ReturnOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "PENDING":
                            setText("待审批");
                            setStyle("-fx-text-fill: orange;");
                            break;
                        case "APPROVED":
                            setText("已批准");
                            setStyle("-fx-text-fill: green;");
                            break;
                        case "REJECTED":
                            setText("已拒绝");
                            setStyle("-fx-text-fill: red;");
                            break;
                        case "COMPLETED":
                            setText("已完成");
                            setStyle("-fx-text-fill: blue;");
                            break;
                        default:
                            setText(item);
                    }
                }
            }
        });

        returnOrderTable.setItems(returnOrderList);
    }

    private void initializeCharts() {
        // 状态饼图
        statusPieChart.setTitle("退货订单状态分布");
        statusPieChart.setLegendSide(Side.RIGHT);

        // 退货趋势柱状图
        returnTrendBarChart.setTitle("退货金额趋势");
        returnTrendBarChart.getXAxis().setLabel("日期");
        returnTrendBarChart.getYAxis().setLabel("金额（元）");
        returnTrendBarChart.setLegendVisible(false);

        // 分类退货柱状图
        categoryReturnBarChart.setTitle("分类退货统计");
        categoryReturnBarChart.getXAxis().setLabel("分类");
        categoryReturnBarChart.getYAxis().setLabel("退货金额（元）");
        categoryReturnBarChart.setLegendVisible(false);
    }

    @FXML
    public void handleReportTypeChange() {
        String reportType = reportTypeComboBox.getValue();
        LocalDate now = LocalDate.now();

        switch (reportType) {
            case "今日报表":
                startDatePicker.setValue(now);
                endDatePicker.setValue(now);
                break;
            case "本周报表":
                LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1);
                endDatePicker.setValue(now);
                startDatePicker.setValue(startOfWeek);
                break;
            case "本月报表":
                LocalDate firstDayOfMonth = now.withDayOfMonth(1);
                startDatePicker.setValue(firstDayOfMonth);
                endDatePicker.setValue(now);
                break;
            case "全部报表":
                startDatePicker.setValue(null);
                endDatePicker.setValue(null);
                break;
            case "自定义日期":
                // 不自动设置日期
                break;
        }

        generateReport();
    }

    @FXML
    public void handleGenerate() {
        generateReport();
    }

    @FXML
    public void handleRefresh() {
        generateReport();
    }

    @FXML
    public void handleExport() {
        if (returnOrderList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "没有可导出的数据");
            return;
        }

        // 准备导出数据
        List<String> headers = List.of("退货单号", "会员名称", "退货日期", "退货金额", "状态", "操作员", "退货原因");
        List<String[]> data = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (ReturnOrder order : returnOrderList) {
            data.add(new String[]{
                order.returnOrderId,
                order.memberName != null ? order.memberName : "无",
                sdf.format(order.returnDate),
                CurrencyUtil.format(order.totalAmount.doubleValue()),
                order.getStatusText(),
                order.operatorName,
                order.returnReason != null ? order.returnReason : ""
            });
        }

        // 调用导出
        String filePath = com.cashier.util.ExportUtil.export(
            "退货报表_" + reportTypeComboBox.getValue(),
            headers,
            data,
            com.cashier.util.ExportUtil.ExportFormat.EXCEL,
            "退货报表"
        );

        if (filePath != null) {
            showAlert(Alert.AlertType.INFORMATION, "导出成功", "文件已导出到:\n" + filePath);
            logger.info("退货报表导出成功: {}", filePath);
        } else {
            showAlert(Alert.AlertType.ERROR, "导出失败", "导出失败，请查看日志");
        }
    }

    private void generateReport() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        Date start = startDate != null ? Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date end = endDate != null ? Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()) : null;

        if (start == null || end == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择日期范围");
            return;
        }

        if (start.after(end)) {
            showAlert(Alert.AlertType.WARNING, "提示", "开始日期不能晚于结束日期");
            return;
        }

        try {
            // 获取退货统计
            ReturnService.ReturnStatistics stats = ReturnService.calculateReturnStatistics(start, end);

            // 更新统计标签
            totalReturnOrdersLabel.setText(String.valueOf(stats.totalReturnOrders));
            totalReturnAmountLabel.setText(CurrencyUtil.format(stats.totalReturnAmount));
            approvedOrdersLabel.setText(String.valueOf(stats.approvedOrders));
            rejectedOrdersLabel.setText(String.valueOf(stats.rejectedOrders));
            completedOrdersLabel.setText(String.valueOf(stats.completedOrders));
            pendingOrdersLabel.setText(String.valueOf(stats.totalReturnOrders - stats.approvedOrders - stats.rejectedOrders - stats.completedOrders));
            avgReturnAmountLabel.setText(CurrencyUtil.format(
                stats.totalReturnOrders > 0 ? stats.totalReturnAmount / stats.totalReturnOrders : 0));

            // 加载退货订单列表
            returnOrderList.clear();
            List<ReturnOrder> orders = ReturnOrderDAO.findByDateRange(start, end);
            returnOrderList.addAll(orders);

            // 更新图表
            updateStatusPieChart(stats);
            updateReturnTrendChart(orders);
            updateCategoryReturnChart(orders);

            logger.info("退货报表生成成功，统计期: {} 至 {}", startDate, endDate);
        } catch (Exception e) {
            logger.error("生成退货报表失败", e);
            showAlert(Alert.AlertType.ERROR, "错误", "生成报表失败: " + e.getMessage());
        }
    }

    private void updateStatusPieChart(ReturnService.ReturnStatistics stats) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        if (stats.approvedOrders > 0) {
            pieChartData.add(new PieChart.Data("已批准", stats.approvedOrders));
        }
        if (stats.rejectedOrders > 0) {
            pieChartData.add(new PieChart.Data("已拒绝", stats.rejectedOrders));
        }
        if (stats.completedOrders > 0) {
            pieChartData.add(new PieChart.Data("已完成", stats.completedOrders));
        }
        int pending = stats.totalReturnOrders - stats.approvedOrders - stats.rejectedOrders - stats.completedOrders;
        if (pending > 0) {
            pieChartData.add(new PieChart.Data("待审批", pending));
        }

        statusPieChart.setData(pieChartData);
    }

    private void updateReturnTrendChart(List<ReturnOrder> orders) {
        Map<String, Double> dailyReturns = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // 初始化所有日期的数据
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        LocalDate date = start;
        while (!date.isAfter(end)) {
            dailyReturns.put(sdf.format(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())), 0.0);
            date = date.plusDays(1);
        }

        // 汇总每日退货金额
        for (ReturnOrder order : orders) {
            String dateKey = sdf.format(order.returnDate);
            dailyReturns.put(dateKey, dailyReturns.getOrDefault(dateKey, 0.0) + order.getTotalAmount().doubleValue());
        }

        // 创建柱状图数据
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Double> entry : dailyReturns.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        returnTrendBarChart.getData().clear();
        returnTrendBarChart.getData().add(series);
    }

    private void updateCategoryReturnChart(List<ReturnOrder> orders) {
        Map<String, Double> categoryReturns = new HashMap<>();

        // 汇总分类退货金额
        for (ReturnOrder order : orders) {
            List<ReturnOrderItem> items = ReturnOrderItemDAO.findByReturnOrderId(order.returnOrderId);
            for (ReturnOrderItem item : items) {
                String category = item.category != null && !item.category.isEmpty() ? item.category : "未分类";
                categoryReturns.put(category, categoryReturns.getOrDefault(category, 0.0) + item.getReturnAmount().doubleValue());
            }
        }

        // 创建柱状图数据
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Double> entry : categoryReturns.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        categoryReturnBarChart.getData().clear();
        categoryReturnBarChart.getData().add(series);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}