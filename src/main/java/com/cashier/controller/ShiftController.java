package com.cashier.controller;

import com.cashier.model.DataManager;
import com.cashier.model.Shift;
import com.cashier.model.Transaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 交接班控制器
 * 处理交接班记录的查询和显示
 */
public class ShiftController {

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
    private Button viewDetailButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button refreshButton;

    private ObservableList<Shift> shiftList;
    private List<Shift> allShifts;

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
     * 加载交接班数据
     */
    private void loadShifts() {
        System.out.println("ShiftController: 开始加载交接班数据...");
        allShifts = DataManager.loadShifts();
        shiftList = FXCollections.observableArrayList(allShifts);
        shiftTable.setItems(shiftList);
        updateStatistics();
        System.out.println("ShiftController: 加载了 " + allShifts.size() + " 条交接班记录");
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        countLabel.setText("班次数量: " + shiftList.size());

        double totalRevenue = 0.0;
        int totalTransaction = 0;

        for (Shift s : shiftList) {
            totalRevenue += s.shiftRevenue;
            totalTransaction += s.shiftTransactionCount;
        }

        totalRevenueLabel.setText(String.format("总营业额: ¥%.2f", totalRevenue));
        totalTransactionLabel.setText(String.format("总交易数: %d", totalTransaction));
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
        detail.append("开始时间: ").append(sdf.format(shift.startTime)).append("\n");
        detail.append("结束时间: ").append(sdf.format(shift.endTime)).append("\n");
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("导出");
        alert.setHeaderText(null);
        alert.setContentText("导出功能正在开发中...");
        alert.showAndWait();
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
        // TODO: 更新主界面的状态栏
        System.out.println("状态: " + status);
    }

    /**
     * 刷新交接班列表
     */
    public void refreshShifts() {
        loadShifts();
    }
}
