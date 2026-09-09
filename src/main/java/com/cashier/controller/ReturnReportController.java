package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.service.ReturnService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;

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
    /** 防止重复触发报表查询（后台生成期间忽略新的触发） */
    private boolean reportInProgress;

    @FXML
    public void initialize() {
        logger.info("初始化退货报表统计控制器");

        // 初始化报表类型下拉框
        reportTypeComboBox.setItems(FXCollections.observableArrayList(
            "全部报表", "今日报表", "本周报表", "本月报表", "自定义日期"
        ));
        com.cashier.util.I18nUiUtils.configureComboBox(
            reportTypeComboBox, com.cashier.util.I18nUiUtils::dateRange);
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
                        java.time.LocalDateTime dateTime = java.time.Instant.ofEpochMilli(FormValidator.parseLong(item))
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                        setText(dateTime.format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME));
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
                        double amount = FormValidator.parseDouble(item);
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
                    clearSemanticTextStyles(this);
                } else {
                    switch (item) {
                        case "PENDING":
                            setText(com.cashier.i18n.I18nManager.getInstance().get("return_report.pending_orders"));
                            applySemanticTextStyle(this, "text-warning");
                            break;
                        case "APPROVED":
                            setText(com.cashier.i18n.I18nManager.getInstance().get("return_report.approved_orders"));
                            applySemanticTextStyle(this, "text-success");
                            break;
                        case "REJECTED":
                            setText(com.cashier.i18n.I18nManager.getInstance().get("return_report.rejected_orders"));
                            applySemanticTextStyle(this, "text-danger");
                            break;
                        case "COMPLETED":
                            setText(com.cashier.i18n.I18nManager.getInstance().get("return_report.completed_orders"));
                            applySemanticTextStyle(this, "text-info");
                            break;
                        default:
                            setText(item);
                            clearSemanticTextStyles(this);
                    }
                }
            }
        });

        returnOrderTable.setItems(returnOrderList);
    }

    private void applySemanticTextStyle(TableCell<?, ?> cell, String styleClass) {
        clearSemanticTextStyles(cell);
        cell.getStyleClass().add(styleClass);
    }

    private void clearSemanticTextStyles(TableCell<?, ?> cell) {
        cell.getStyleClass().removeAll("text-success", "text-danger", "text-warning", "text-info");
    }

    private void initializeCharts() {
        // 状态饼图
        statusPieChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("return_report.status_distribution"));
        statusPieChart.setLegendSide(Side.RIGHT);

        // 退货趋势柱状图
        returnTrendBarChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("return_report.amount_trend"));
        returnTrendBarChart.getXAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.DATE));
        returnTrendBarChart.getYAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.AMOUNT));
        returnTrendBarChart.setLegendVisible(false);

        // 分类退货柱状图
        categoryReturnBarChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("return_report.category_statistics"));
        categoryReturnBarChart.getXAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.CATEGORY));
        categoryReturnBarChart.getYAxis().setLabel(I18nManager.getInstance().get("chart.return_amount"));
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
            default:
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
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_data"));
            return;
        }

        // 准备导出数据
        List<String> headers = List.of("退货单号", "会员名称", "退货日期", "退货金额", "状态", "操作员", "退货原因");
        List<String[]> data = new ArrayList<>();

        for (ReturnOrder order : returnOrderList) {
            data.add(new String[]{
                order.returnOrderId,
                order.memberName != null ? order.memberName : "无",
                order.returnDate != null ? order.returnDate.atZone(ZoneId.systemDefault()).toLocalDateTime().format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME) : "",
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
            showAlert(Alert.AlertType.INFORMATION, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT),
                    com.cashier.i18n.I18nManager.getInstance().get("runtime.exported_to", filePath));
            logger.info("退货报表导出成功: {}", filePath);
        } else {
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_DATA), com.cashier.i18n.I18nManager.getInstance().get("runtime.export_log_short"));
        }
    }

    /**
     * 生成报表（统计/订单列表/分类图数据在后台线程加载，UI 更新经 runLater 回 FX）
     */
    private void generateReport() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        Date start = startDate != null ? Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date end = endDate != null ? Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()) : null;

        // "全部报表" = 不按日期过滤（start/end 均为 null）
        boolean unbounded = start == null || end == null;
        if (!unbounded && start.after(end)) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.invalid_date_range_plain"));
            return;
        }
        if (reportInProgress) {
            return;
        }
        reportInProgress = true;

        final Date fStart = start;
        final Date fEnd = end;
        final boolean all = unbounded;
        final LocalDate fStartDate = startDate;
        final LocalDate fEndDate = endDate;

        Thread worker = new Thread(() -> {
            try {
                ReturnService.ReturnStatistics stats;
                List<ReturnOrder> orders;
                if (all) {
                    orders = DAOFactory.getInstance().getReturnOrderDAO().findAll();
                    stats = deriveStatistics(orders);
                } else {
                    stats = ReturnService.calculateReturnStatistics(fStart, fEnd);
                    orders = DAOFactory.getInstance().getReturnOrderDAO().findByDateRange(fStart, fEnd);
                }
                // 分类图明细在后台批量拉取，避免在 FX 线程做逐单 N+1 查询
                Map<String, Double> categoryReturns = computeCategoryReturns(orders);

                final ReturnService.ReturnStatistics finalStats = stats;
                final List<ReturnOrder> finalOrders = orders;
                javafx.application.Platform.runLater(() -> {
                    reportInProgress = false;
                    applyReportToUi(finalStats, finalOrders, categoryReturns, fStartDate, fEndDate);
                });
            } catch (Exception e) {
                logger.error("生成退货报表失败", e);
                javafx.application.Platform.runLater(() -> {
                    reportInProgress = false;
                    showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.ERROR),
                        com.cashier.i18n.I18nManager.getInstance().get("runtime.report_generate_failed", e.getMessage()));
                });
            }
        }, "return-report-generate");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 在后台线程逐单拉取明细并汇总分类退货金额（替代原先 FX 上的 N+1）
     */
    private Map<String, Double> computeCategoryReturns(List<ReturnOrder> orders) {
        Map<String, Double> categoryReturns = new HashMap<>();
        for (ReturnOrder order : orders) {
            List<ReturnOrderItem> items = DAOFactory.getInstance().getReturnOrderItemDAO()
                .findByReturnOrderId(order.returnOrderId);
            for (ReturnOrderItem item : items) {
                String category = item.category != null && !item.category.isEmpty()
                    ? item.category : I18nManager.getInstance().get(I18nKeys.Report.UNCATEGORIZED);
                categoryReturns.put(category, categoryReturns.getOrDefault(category, 0.0)
                    + item.getReturnAmount().doubleValue());
            }
        }
        return categoryReturns;
    }

    /** 在 FX 线程把后台计算结果渲染到界面 */
    private void applyReportToUi(ReturnService.ReturnStatistics stats, List<ReturnOrder> orders,
                                 Map<String, Double> categoryReturns,
                                 LocalDate startDate, LocalDate endDate) {
        // 更新统计标签
        totalReturnOrdersLabel.setText(String.valueOf(stats.totalReturnOrders));
        totalReturnAmountLabel.setText(CurrencyUtil.format(stats.totalReturnAmount));
        approvedOrdersLabel.setText(String.valueOf(stats.approvedOrders));
        rejectedOrdersLabel.setText(String.valueOf(stats.rejectedOrders));
        completedOrdersLabel.setText(String.valueOf(stats.completedOrders));
        pendingOrdersLabel.setText(String.valueOf(stats.totalReturnOrders - stats.approvedOrders - stats.rejectedOrders - stats.completedOrders));
        avgReturnAmountLabel.setText(CurrencyUtil.format(
            stats.totalReturnOrders > 0
                ? stats.totalReturnAmount.divide(java.math.BigDecimal.valueOf(stats.totalReturnOrders), 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO));

        // 加载退货订单列表
        returnOrderList.clear();
        returnOrderList.addAll(orders);

        // 更新图表
        updateStatusPieChart(stats);
        updateReturnTrendChart(orders);
        updateCategoryReturnChart(categoryReturns);

        logger.info("退货报表生成成功，统计期: {} 至 {}", startDate, endDate);
    }

    /**
     * 由全量退货单计算统计（"全部报表"无日期过滤时使用）
     */
    private ReturnService.ReturnStatistics deriveStatistics(List<ReturnOrder> orders) {
        ReturnService.ReturnStatistics stats = new ReturnService.ReturnStatistics();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (ReturnOrder order : orders) {
            if (order.getTotalAmount() != null) {
                total = total.add(order.getTotalAmount());
            }
            if ("APPROVED".equals(order.status)) {
                stats.approvedOrders++;
            } else if ("REJECTED".equals(order.status)) {
                stats.rejectedOrders++;
            } else if ("COMPLETED".equals(order.status)) {
                stats.completedOrders++;
            }
        }
        stats.totalReturnOrders = orders.size();
        stats.totalReturnAmount = total;
        return stats;
    }

    private void updateStatusPieChart(ReturnService.ReturnStatistics stats) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        if (stats.approvedOrders > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.STATUS_APPROVED), stats.approvedOrders));
        }
        if (stats.rejectedOrders > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.STATUS_REJECTED), stats.rejectedOrders));
        }
        if (stats.completedOrders > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.STATUS_COMPLETED), stats.completedOrders));
        }
        int pending = stats.totalReturnOrders - stats.approvedOrders - stats.rejectedOrders - stats.completedOrders;
        if (pending > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.STATUS_PENDING_APPROVAL), pending));
        }

        statusPieChart.setData(pieChartData);
    }

    private void updateReturnTrendChart(List<ReturnOrder> orders) {
        Map<String, Double> dailyReturns = new LinkedHashMap<>();
        java.time.format.DateTimeFormatter formatter = com.cashier.util.DateTimeFormats.DATE;

        // 初始化所有日期的数据（"全部报表"无日期范围时跳过，仅汇总实际存在的日期）
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start != null && end != null) {
            LocalDate date = start;
            while (!date.isAfter(end)) {
                dailyReturns.put(date.format(com.cashier.util.DateTimeFormats.DATE), 0.0);
                date = date.plusDays(1);
            }
        }

        // 汇总每日退货金额
            for (ReturnOrder order : orders) {
            if (order.returnDate == null) {
                continue;
            }
            String dateKey = order.returnDate.atZone(ZoneId.systemDefault()).format(formatter);
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

    private void updateCategoryReturnChart(Map<String, Double> categoryReturns) {
        // 创建柱状图数据（分类汇总已在后台 computeCategoryReturns 完成，这里只做渲染）
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Double> entry : categoryReturns.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        categoryReturnBarChart.getData().clear();
        categoryReturnBarChart.getData().add(series);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        updateStatusForAlert(type, message);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateStatusForAlert(Alert.AlertType type, String message) {
        if (type == Alert.AlertType.ERROR) {
            com.cashier.util.StatusBarManager.updateError(message);
        } else if (type == Alert.AlertType.WARNING) {
            com.cashier.util.StatusBarManager.updateWarning(message);
        } else if (type == Alert.AlertType.INFORMATION) {
            com.cashier.util.StatusBarManager.updateSuccess(message);
        }
    }
}
