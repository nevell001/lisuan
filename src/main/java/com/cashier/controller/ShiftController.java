package com.cashier.controller;

import com.cashier.controller.MainController;
import com.cashier.dao.ShiftDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Shift;
import com.cashier.model.Transaction;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 交接班控制器
 * 处理交接班记录的查询和显示
 */
public class ShiftController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ShiftController.class);

    @FXML
    private TableView<Shift> shiftTable;

    @FXML
    private TableColumn<Shift, String> shiftIdColumn;

    @FXML
    private TableColumn<Shift, String> operatorColumn;

    @FXML
    private TableColumn<Shift, String> timeColumn;

    @FXML
    private TableColumn<Shift, String> durationColumn;

    @FXML
    private TableColumn<Shift, String> transactionColumn;

    @FXML
    private TableColumn<Shift, String> revenueColumn;

    @FXML
    private TableColumn<Shift, String> paymentColumn;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField searchField;

    @FXML
    private Label countLabel;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private Label totalTransactionLabel;

    @FXML
    private BarChart<String, Number> shiftRevenueBarChart;

    @FXML
    private PieChart paymentMethodPieChart;

    @FXML
    private Button viewDetailButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button startShiftButton;

    @FXML
    private Button endShiftButton;

    private ObservableList<Shift> shiftList;
    private List<Shift> allShifts;
    private com.cashier.model.User currentUser;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置默认日期范围（今天）
        java.time.LocalDate today = java.time.LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);

        // 设置表格列
        setupTableColumns();

        // 初始化图表
        initializeCharts();

        // 加载交接班数据
        loadShifts();

        // 设置表格选择模式
        shiftTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 添加表格选择监听
        shiftTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );

        // 设置行点击事件
        shiftTable.setRowFactory(tv -> {
            TableRow<Shift> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Shift shift = row.getItem();
                    if (shift != null) {
                        showShiftDetail(shift);
                    }
                }
            });
            return row;
        });

        // 更新开班/交班按钮状态
        updateShiftButtonStates();
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        shiftIdColumn.setCellValueFactory(new PropertyValueFactory<>("shiftId"));
        operatorColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().operatorName));
        timeColumn.setCellValueFactory(cellData -> {
            Shift s = cellData.getValue();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
            return new SimpleStringProperty(String.format("%s 至 %s",
                sdf.format(s.startTime),
                sdf.format(s.endTime)));
        });
        durationColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDurationText()));
        transactionColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().shiftTransactionCount)));
        revenueColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("¥%.2f", cellData.getValue().shiftRevenue)));
        paymentColumn.setCellValueFactory(cellData -> {
            Shift s = cellData.getValue();
            return new SimpleStringProperty(String.format("现金:¥%.2f 微信:¥%.2f 支付宝:¥%.2f 银行卡:¥%.2f",
                s.cashRevenue, s.wechatRevenue, s.alipayRevenue, s.cardRevenue));
        });
    }

    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 班次收入对比柱状图
        shiftRevenueBarChart.setTitle("班次收入对比");
        shiftRevenueBarChart.getXAxis().setLabel("班次");
        shiftRevenueBarChart.getYAxis().setLabel("营业额（元）");
        shiftRevenueBarChart.setLegendVisible(false);

        // 支付方式分布饼图
        paymentMethodPieChart.setTitle("支付方式分布");
        paymentMethodPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
    }

    /**
     * 加载交接班数据
     */
    private void loadShifts() {
        logger.info("ShiftController: 开始加载交接班数据...");
        try {
            allShifts = ShiftDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载交接班数据失败", e);
            showError("加载交接班数据失败: " + e.getMessage());
            allShifts = new java.util.ArrayList<>();
        }
        shiftList = FXCollections.observableArrayList(allShifts);
        shiftTable.setItems(shiftList);
        updateStatistics();
        logger.info("ShiftController: 加载了 {} 条交接班记录", allShifts.size());
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        countLabel.setText("班次数量: " + shiftList.size());

        double totalRevenue = 0.0;
        int totalTransaction = 0;

        for (Shift s : shiftList) {
            // 只统计有效的正数数据，忽略负数或无效数据
            if (s.shiftRevenue > 0) {
                totalRevenue += s.shiftRevenue;
            }
            if (s.shiftTransactionCount > 0) {
                totalTransaction += s.shiftTransactionCount;
            }
        }

        totalRevenueLabel.setText(String.format("总营业额: ¥%.2f", totalRevenue));
        totalTransactionLabel.setText(String.format("总交易数: %d", totalTransaction));

        // 更新图表
        updateCharts(shiftList);
    }

    /**
     * 更新图表
     */
    private void updateCharts(ObservableList<Shift> shifts) {
        // 更新班次收入柱状图
        updateShiftRevenueChart(shifts);

        // 更新支付方式饼图
        updatePaymentMethodPieChart(shifts);
    }

    /**
     * 更新班次收入柱状图
     */
    private void updateShiftRevenueChart(ObservableList<Shift> shifts) {
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();

        // 限制显示最近的10个班次
        int limit = Math.min(10, shifts.size());
        for (int i = 0; i < limit; i++) {
            Shift shift = shifts.get(i);
            String label = "班次" + (i + 1);
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(label, shift.shiftRevenue));
        }

        shiftRevenueBarChart.getData().clear();
        shiftRevenueBarChart.getData().add(series);
    }

    /**
     * 更新支付方式饼图
     */
    private void updatePaymentMethodPieChart(ObservableList<Shift> shifts) {
        double totalCash = 0.0;
        double totalWechat = 0.0;
        double totalAlipay = 0.0;
        double totalCard = 0.0;

        for (Shift shift : shifts) {
            totalCash += shift.cashRevenue > 0 ? shift.cashRevenue : 0;
            totalWechat += shift.wechatRevenue > 0 ? shift.wechatRevenue : 0;
            totalAlipay += shift.alipayRevenue > 0 ? shift.alipayRevenue : 0;
            totalCard += shift.cardRevenue > 0 ? shift.cardRevenue : 0;
        }

        javafx.collections.ObservableList<javafx.scene.chart.PieChart.Data> pieChartData =
            javafx.collections.FXCollections.observableArrayList();

        if (totalCash > 0) {
            pieChartData.add(new javafx.scene.chart.PieChart.Data("现金", totalCash));
        }
        if (totalWechat > 0) {
            pieChartData.add(new javafx.scene.chart.PieChart.Data("微信", totalWechat));
        }
        if (totalAlipay > 0) {
            pieChartData.add(new javafx.scene.chart.PieChart.Data("支付宝", totalAlipay));
        }
        if (totalCard > 0) {
            pieChartData.add(new javafx.scene.chart.PieChart.Data("银行卡", totalCard));
        }

        paymentMethodPieChart.setData(pieChartData);
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = shiftTable.getSelectionModel().getSelectedItem() != null;
        viewDetailButton.setDisable(!hasSelection);
    }

    /**
     * 处理查看详情
     */
    @FXML
    private void handleViewDetail() {
        Shift selected = shiftTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showShiftDetail(selected);
        }
    }

    /**
     * 显示交接班详情
     * @param shift 交接班记录
     */
    private void showShiftDetail(Shift shift) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        StringBuilder detail = new StringBuilder();
        detail.append("交接班详情\n\n");
        detail.append("班次ID: ").append(shift.shiftId).append("\n");
        detail.append("操作员: ").append(shift.operatorName).append("\n");
        detail.append("开始时间: ").append(shift.startTime != null ? sdf.format(shift.startTime) : "未开始").append("\n");
        detail.append("结束时间: ").append(shift.endTime != null ? sdf.format(shift.endTime) : "未结束").append("\n");
        detail.append("班次时长: ").append(shift.getDurationText()).append("\n\n");

        detail.append("营业额统计:\n");
        detail.append("  开机营业额: ¥").append(String.format("%.2f", shift.openingRevenue)).append("\n");
        detail.append("  关机营业额: ¥").append(String.format("%.2f", shift.closingRevenue)).append("\n");
        detail.append("  本班营业额: ¥").append(String.format("%.2f", shift.shiftRevenue)).append("\n\n");

        detail.append("交易统计:\n");
        detail.append("  开机交易数: ").append(shift.openingTransactionCount).append("\n");
        detail.append("  关机交易数: ").append(shift.closingTransactionCount).append("\n");
        detail.append("  本班交易数: ").append(shift.shiftTransactionCount).append("\n\n");

        detail.append("支付方式统计:\n");
        detail.append("  现金: ¥").append(String.format("%.2f", shift.cashRevenue)).append("\n");
        detail.append("  微信: ¥").append(String.format("%.2f", shift.wechatRevenue)).append("\n");
        detail.append("  支付宝: ¥").append(String.format("%.2f", shift.alipayRevenue)).append("\n");
        detail.append("  银行卡: ¥").append(String.format("%.2f", shift.cardRevenue)).append("\n\n");

        if (shift.notes != null && !shift.notes.isEmpty()) {
            detail.append("备注: ").append(shift.notes).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("交接班详情");
        alert.setHeaderText(null);
        alert.setContentText(detail.toString());
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    /**
     * 处理搜索
     */
    @FXML
    private void handleSearch() {
        applyFilters();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    private void handleClearSearch() {
        startDatePicker.setValue(java.time.LocalDate.now());
        endDatePicker.setValue(java.time.LocalDate.now());
        searchField.clear();
        applyFilters();
    }

    /**
     * 应用筛选条件
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();

        shiftList.setAll(allShifts.stream()
            .filter(s -> {
                // 日期筛选
                if (startDatePicker.getValue() != null || endDatePicker.getValue() != null) {
                    java.time.LocalDate shiftDate = s.startTime.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();

                    if (startDatePicker.getValue() != null && shiftDate.isBefore(startDatePicker.getValue())) {
                        return false;
                    }
                    if (endDatePicker.getValue() != null && shiftDate.isAfter(endDatePicker.getValue())) {
                        return false;
                    }
                }

                // 搜索文本筛选（操作员姓名）
                if (!searchText.isEmpty()) {
                    return s.operatorName.toLowerCase().contains(searchText) ||
                           s.shiftId.toLowerCase().contains(searchText);
                }

                return true;
            })
            .toList());

        updateStatistics();
    }

    /**
     * 处理导出
     */
    @FXML
    private void handleExport() {
        if (shiftList.isEmpty()) {
            showError("没有可导出的交接班记录");
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

            exportShifts(exportFormat);
        });
    }

    /**
     * 导出交接班记录
     */
    private void exportShifts(com.cashier.util.ExportUtil.ExportFormat format) {
        try {
            // 准备表头
            java.util.List<String> headers = java.util.Arrays.asList(
                "班次编号", "操作员", "开始时间", "结束时间", "班次时长", "交易数量",
                "总收入", "现金收入", "微信收入", "支付宝收入", "银行卡收入", "备注"
            );

            // 准备数据
            java.util.List<String[]> data = new java.util.ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (Shift s : shiftList) {
                // 格式化时间，处理 NULL 值
                String startTimeStr = (s.startTime != null) ? sdf.format(s.startTime) : "未开始";
                String endTimeStr = (s.endTime != null) ? sdf.format(s.endTime) : "未结束";
                
                // 计算班次时长，处理 NULL 值
                String durationText = "未完成";
                if (s.startTime != null && s.endTime != null) {
                    durationText = s.getDurationText();
                }

                data.add(new String[]{
                    s.shiftId,
                    s.operatorName,
                    startTimeStr,
                    endTimeStr,
                    durationText,
                    String.valueOf(s.shiftTransactionCount),
                    String.format("¥%.2f", s.shiftRevenue),
                    String.format("¥%.2f", s.cashRevenue),
                    String.format("¥%.2f", s.wechatRevenue),
                    String.format("¥%.2f", s.alipayRevenue),
                    String.format("¥%.2f", s.cardRevenue),
                    s.notes == null || s.notes.isEmpty() ? "无" : s.notes
                });
            }

            // 导出数据
            String filePath = com.cashier.util.ExportUtil.export(
                "交接班报表",
                headers,
                data,
                format,
                "交接班记录"
            );

            if (filePath != null) {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("导出成功");
                successAlert.setHeaderText(null);
                successAlert.setContentText("文件已成功导出到:\n" + filePath);
                successAlert.showAndWait();
                updateStatus("导出成功");
            } else {
                showError("导出失败，请查看日志获取详细信息");
            }
        } catch (Exception e) {
            logger.error("导出交接班记录失败", e);
            showError("导出失败: " + e.getMessage());
        }
    }

    /**
     * 处理刷新
     */
    @FXML
    private void handleRefresh() {
        loadShifts();
        updateStatus("已刷新");
    }

    /**
     * 更新状态
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    /**
     * 刷新交接班列表
     */
    public void refreshShifts() {
        loadShifts();
    }

    /**
     * 更新开班/交班按钮状态
     */
    private void updateShiftButtonStates() {
        boolean hasActiveShift = false;
        try {
            hasActiveShift = ShiftDAO.hasActiveShift();
        } catch (SQLException e) {
            logger.error("检查活跃班次失败", e);
            hasActiveShift = false;
        }
        startShiftButton.setDisable(hasActiveShift);
        endShiftButton.setDisable(!hasActiveShift);
    }

    /**
     * 处理开班
     */
    @FXML
    private void handleStartShift() {
        // 检查是否已有活跃班次
        try {
            if (ShiftDAO.hasActiveShift()) {
                showError("当前已有活跃班次，请先交班后再开新班！");
                return;
            }
        } catch (SQLException e) {
            logger.error("检查活跃班次失败", e);
            showError("检查活跃班次失败，请稍后重试");
            return;
        }

        // 确认开班
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认开班");
        alert.setHeaderText(null);
        alert.setContentText("确定要开始新的班次吗？");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // 获取当前用户
            if (currentUser == null) {
                showError("无法获取当前用户信息！");
                return;
            }

            // 加载现有交易记录，获取当前总营业额和交易数
            List<Transaction> transactions;
            try {
                transactions = TransactionDAO.findAll();
            } catch (SQLException e) {
                logger.error("加载交易记录失败", e);
                showError("加载交易记录失败: " + e.getMessage());
                return;
            }

            double totalRevenue = 0.0;
            int totalTransactions = transactions.size();

            for (Transaction t : transactions) {
                totalRevenue += t.finalAmount;
            }

            // 生成班次ID
            String shiftId = "SHIFT" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());

            // 创建新班次
            Shift shift = new Shift(
                shiftId,
                currentUser.username,
                currentUser.name,
                new java.util.Date(),
                totalRevenue,
                totalTransactions
            );

            // 保存班次到数据库
            try {
                ShiftDAO.insert(shift);
            } catch (SQLException e) {
                logger.error("保存班次失败", e);
                showError("保存班次失败: " + e.getMessage());
                return;
            }

            // 刷新列表
            loadShifts();
            updateShiftButtonStates();

            showSuccess("开班成功！班次ID: " + shiftId);

            // 更新主界面的班次信息
            MainController.updateShiftInfoGlobal();

        } catch (Exception e) {
            showError("开班失败: " + e.getMessage());
            logger.error("开班失败", e);
        }
    }

    /**
     * 处理交班
     */
    @FXML
    private void handleEndShift() {
        // 检查是否有活跃班次
        Shift activeShift = null;
try {
            activeShift = ShiftDAO.findActiveShift();
        } catch (SQLException e) {
            logger.error("获取活跃班次失败", e);
            showError("获取活跃班次失败: " + e.getMessage());
            return;
        }

        if (activeShift == null) {
            showError("当前没有活跃班次！");
            return;
        }

        // 确认交班
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认交班");
        alert.setHeaderText(null);
        alert.setContentText("确定要结束当前班次吗？");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // 加载所有交易记录
            List<Transaction> allTransactions;
            try {
                allTransactions = TransactionDAO.findAll();
            } catch (SQLException e) {
                logger.error("加载交易记录失败", e);
                showError("加载交易记录失败: " + e.getMessage());
                return;
            }

            // 筛选本班次的交易记录（在班次开始时间之后的交易）
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            List<Transaction> shiftTransactions = new java.util.ArrayList<>();
            double cashRevenue = 0.0;
            double wechatRevenue = 0.0;
            double alipayRevenue = 0.0;
            double cardRevenue = 0.0;
            double totalRevenue = 0.0;

            for (Transaction t : allTransactions) {
                try {
                    java.util.Date transactionTime = sdf.parse(t.timestamp);
                    if (transactionTime.after(activeShift.startTime) || transactionTime.equals(activeShift.startTime)) {
                        shiftTransactions.add(t);
                        totalRevenue += t.finalAmount;

                        // 按支付方式分类统计
                        if ("现金".equals(t.paymentMethod)) {
                            cashRevenue += t.finalAmount;
                        } else if ("微信".equals(t.paymentMethod)) {
                            wechatRevenue += t.finalAmount;
                        } else if ("支付宝".equals(t.paymentMethod)) {
                            alipayRevenue += t.finalAmount;
                        } else if ("银行卡".equals(t.paymentMethod)) {
                            cardRevenue += t.finalAmount;
                        }
                    }
                } catch (Exception e) {
                    logger.error("解析交易时间失败: {}", t.timestamp, e);
                }
            }

            // 结束班次
            // 计算班次结束时的累计总营业额和总交易数
            double closingRevenue = activeShift.openingRevenue + totalRevenue;
            int closingTransactionCount = activeShift.openingTransactionCount + shiftTransactions.size();
            activeShift.endShift(closingRevenue, closingTransactionCount, cashRevenue, wechatRevenue, alipayRevenue, cardRevenue);

            // 保存班次到数据库
            try {
                ShiftDAO.update(activeShift);
            } catch (SQLException e) {
                logger.error("更新班次失败", e);
                showError("更新班次失败: " + e.getMessage());
                return;
            }

            // 刷新列表
            loadShifts();
            updateShiftButtonStates();

            // 显示交班详情
            String detail = String.format(
                "交班成功！\n\n" +
                "班次ID: %s\n" +
                "操作员: %s\n" +
                "班次时长: %s\n" +
                "本班次交易数: %d\n" +
                "本班次营业额: ¥%.2f\n\n" +
                "支付方式明细:\n" +
                "现金: ¥%.2f\n" +
                "微信: ¥%.2f\n" +
                "支付宝: ¥%.2f\n" +
                "银行卡: ¥%.2f",
                activeShift.shiftId,
                activeShift.operatorName,
                activeShift.getDurationText(),
                activeShift.shiftTransactionCount,
                activeShift.shiftRevenue,
                cashRevenue,
                wechatRevenue,
                alipayRevenue,
                cardRevenue
            );

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("交班成功");
            successAlert.setHeaderText(null);
            successAlert.setContentText(detail);
            successAlert.getDialogPane().setPrefWidth(500);
            successAlert.showAndWait();

            // 更新主界面的班次信息
            MainController.updateShiftInfoGlobal();

            // 退出登录
            handleLogout();

        } catch (Exception e) {
            showError("交班失败: " + e.getMessage());
            logger.error("交班失败", e);
        }
    }

    /**
     * 设置当前用户
     * @param user 用户
     */
    public void setCurrentUser(com.cashier.model.User user) {
        this.currentUser = user;
    }

    /**
     * 处理退出登录
     */
    private void handleLogout() {
        try {
            // 返回登录界面
            javafx.application.Platform.runLater(() -> {
                com.cashier.CashierSystemFXApplication application = com.cashier.CashierSystemFXApplication.getInstance();
                if (application != null) {
                    application.logoutToLoginView();
                }
            });
        } catch (Exception e) {
            showError("退出登录失败: " + e.getMessage());
        }
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
     * 显示成功信息
     * @param message 成功消息
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("成功");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
