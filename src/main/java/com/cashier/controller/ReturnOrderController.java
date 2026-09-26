package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.service.ReturnService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.UIOptimizer;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;
import com.cashier.util.ReceiptPrinter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 退货订单管理控制器
 */
public class ReturnOrderController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnOrderController.class);
    private static final int RETURN_ORDER_LIMIT = 500;

    @FXML private TableView<ReturnOrder> returnOrderTable;
    @FXML private TableColumn<ReturnOrder, String> returnOrderIdColumn;
    @FXML private TableColumn<ReturnOrder, String> memberNameColumn;
    @FXML private TableColumn<ReturnOrder, String> returnDateColumn;
    @FXML private TableColumn<ReturnOrder, String> totalAmountColumn;
    @FXML private TableColumn<ReturnOrder, String> statusColumn;
    @FXML private TableColumn<ReturnOrder, String> operatorNameColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> quickDateComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Label returnOrderIdLabel;
    @FXML private Label memberNameLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label statusLabel;
    @FXML private Label operatorNameLabel;
    @FXML private Label returnDateLabel;
    @FXML private TextArea returnReasonTextArea;
    @FXML private TextArea notesTextArea;

    @FXML private TableView<ReturnOrderItem> itemTable;
    @FXML private TableColumn<ReturnOrderItem, String> productCodeColumn;
    @FXML private TableColumn<ReturnOrderItem, String> productNameColumn;
    @FXML private TableColumn<ReturnOrderItem, Integer> returnQuantityColumn;
    @FXML private TableColumn<ReturnOrderItem, Double> unitPriceColumn;
    @FXML private TableColumn<ReturnOrderItem, Double> returnAmountColumn;
    @FXML private TableColumn<ReturnOrderItem, String> conditionColumn;

    private ObservableList<ReturnOrder> returnOrderList = FXCollections.observableArrayList();
    private ObservableList<ReturnOrderItem> itemList = FXCollections.observableArrayList();
    private ReturnOrder selectedReturnOrder;

    @FXML
    public void initialize() {
        logger.info("初始化退货订单管理控制器");

        // 初始化状态过滤器
        statusFilter.setItems(FXCollections.observableArrayList(
            "全部", "待审批", "已批准", "已拒绝", "已完成"
        ));
        com.cashier.util.I18nUiUtils.configureComboBox(statusFilter, value ->
            "全部".equals(value) ? com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Filter.ALL)
                : com.cashier.util.I18nUiUtils.purchaseStatus(value));
        statusFilter.setValue("全部");

        quickDateComboBox.setItems(FXCollections.observableArrayList(
            "今天", "昨天", "本周", "上周", "本月", "上月", "全部报表", "自定义"
        ));
        com.cashier.util.I18nUiUtils.configureComboBox(
            quickDateComboBox, com.cashier.util.I18nUiUtils::dateRange);
        LocalDate today = LocalDate.now();
        quickDateComboBox.setValue("本月");
        setDateRange(today.withDayOfMonth(1), today);
        quickDateComboBox.setOnAction(event -> handleQuickDateRange());

        // 初始化表格列
        initializeReturnOrderTable();
        initializeItemTable();

        // 设置表格空数据占位符（i18n）
        returnOrderTable.setPlaceholder(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.DATA_EMPTY)));
        itemTable.setPlaceholder(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.DATA_EMPTY)));

        // 加载退货订单数据
        loadReturnOrders();

        // 设置表格选择监听
        returnOrderTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                selectedReturnOrder = newVal;
                showReturnOrderDetail(newVal);
            }
        );
    }

    private void initializeReturnOrderTable() {
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
                        Instant date = Instant.ofEpochMilli(FormValidator.parseLong(item));
                        setText(com.cashier.util.DateTimeFormats.formatStandard(
                            date.atZone(ZoneId.systemDefault()).toLocalDateTime()));
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

    private void initializeItemTable() {
        productCodeColumn.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        returnQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("returnQuantity"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        returnAmountColumn.setCellValueFactory(new PropertyValueFactory<>("returnAmount"));
        conditionColumn.setCellValueFactory(new PropertyValueFactory<>("condition"));

        // 自定义显示格式
        unitPriceColumn.setCellFactory(column -> new TableCell<ReturnOrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CurrencyUtil.format(item));
                }
            }
        });

        returnAmountColumn.setCellFactory(column -> new TableCell<ReturnOrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CurrencyUtil.format(item));
                }
            }
        });

        conditionColumn.setCellFactory(column -> new TableCell<ReturnOrderItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    clearSemanticTextStyles(this);
                } else {
                    setText(com.cashier.util.I18nUiUtils.itemCondition(item));
                    clearSemanticTextStyles(this);
                    switch (item) {
                        case "DAMAGED":
                            applySemanticTextStyle(this, "text-danger");
                            break;
                        case "OPENED":
                            applySemanticTextStyle(this, "text-warning");
                            break;
                        default:
                            break;
                    }
                }
            }
        });

        itemTable.setItems(itemList);
    }

    private void applySemanticTextStyle(TableCell<?, ?> cell, String styleClass) {
        clearSemanticTextStyles(cell);
        cell.getStyleClass().add(styleClass);
    }

    private void clearSemanticTextStyles(TableCell<?, ?> cell) {
        cell.getStyleClass().removeAll("text-success", "text-danger", "text-warning", "text-info");
    }

    private void loadReturnOrders() {
        returnOrderList.clear();
        List<ReturnOrder> orders = loadOrdersForCurrentDateRange();
        returnOrderList.addAll(orders);
        logger.info("加载了 {} 条退货订单记录", orders.size());
    }

    private void showReturnOrderDetail(ReturnOrder returnOrder) {
        if (returnOrder == null) {
            clearDetail();
            return;
        }

        returnOrderIdLabel.setText(returnOrder.returnOrderId);
        memberNameLabel.setText(returnOrder.memberName != null ? returnOrder.memberName : com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Statistics.NO_DATA));
        totalAmountLabel.setText(CurrencyUtil.format(returnOrder.totalAmount.doubleValue()));
        statusLabel.setText(returnOrder.getStatusText());
        operatorNameLabel.setText(returnOrder.operatorName != null && !returnOrder.operatorName.isEmpty()
            ? returnOrder.operatorName
            : com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.NONE));
        returnDateLabel.setText(returnOrder.returnDate != null
            ? com.cashier.util.DateTimeFormats.formatStandard(returnOrder.returnDate.atZone(ZoneId.systemDefault()).toLocalDateTime())
            : "");
        returnReasonTextArea.setText(returnOrder.returnReason != null ? returnOrder.returnReason : "");
        notesTextArea.setText(returnOrder.notes != null ? returnOrder.notes : "");

        // 加载退货明细
        loadReturnOrderItems(returnOrder.returnOrderId);
    }

    private void loadReturnOrderItems(String returnOrderId) {
        // 点开一张退货单就要查一次明细：放后台，回 FX 线程再填表（TD-006）
        itemList.clear();
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getReturnOrderItemDAO().findByReturnOrderId(returnOrderId),
            itemList::addAll,
            e -> logger.error("加载退货明细失败: {}", returnOrderId, e));
    }

    private void clearDetail() {
        returnOrderIdLabel.setText("");
        memberNameLabel.setText("");
        totalAmountLabel.setText("");
        statusLabel.setText("");
        operatorNameLabel.setText("");
        returnDateLabel.setText("");
        returnReasonTextArea.setText("");
        notesTextArea.setText("");
        itemList.clear();
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText().trim();
        String status = statusFilter.getValue();

        returnOrderList.clear();
        returnOrderList.addAll(loadOrdersForCurrentDateRange());

        if (!"全部".equals(status)) {
            String statusCode = "";
            switch (status) {
                case "待审批": statusCode = "PENDING"; break;
                case "已批准": statusCode = "APPROVED"; break;
                case "已拒绝": statusCode = "REJECTED"; break;
                case "已完成": statusCode = "COMPLETED"; break;
                default:
                    logger.warn("未知退货订单状态筛选: {}", status);
                    return;
            }
            String effectiveStatus = statusCode;
            returnOrderList.removeIf(order -> !effectiveStatus.equals(order.status));
        }

        // 搜索过滤
        if (!keyword.isEmpty()) {
            returnOrderList.removeIf(order -> 
                !order.returnOrderId.toLowerCase().contains(keyword.toLowerCase()) &&
                (order.memberName == null || !order.memberName.toLowerCase().contains(keyword.toLowerCase()))
            );
        }

        logger.info("搜索结果: {} 条记录", returnOrderList.size());
    }

    private List<ReturnOrder> loadOrdersForCurrentDateRange() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate == null && endDate == null) {
            return DAOFactory.getInstance().getReturnOrderDAO().findRecent(RETURN_ORDER_LIMIT);
        }

        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return DAOFactory.getInstance().getReturnOrderDAO().findByDateRange(
            java.util.Date.from(effectiveStart.atStartOfDay(ZoneId.systemDefault()).toInstant()),
            java.util.Date.from(effectiveEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant())
        );
    }

    @FXML
    public void handleQuickDateRange() {
        String option = quickDateComboBox.getValue();
        if (option == null || "自定义".equals(option)) {
            return;
        }

        LocalDate today = LocalDate.now();
        switch (option) {
            case "今天" -> setDateRange(today, today);
            case "昨天" -> setDateRange(today.minusDays(1), today.minusDays(1));
            case "本周" -> setDateRange(
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today);
            case "上周" -> {
                LocalDate lastWeekEnd = today.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)).minusDays(1);
                setDateRange(lastWeekEnd.minusDays(6), lastWeekEnd);
            }
            case "本月" -> setDateRange(today.withDayOfMonth(1), today);
            case "上月" -> {
                LocalDate lastMonth = today.minusMonths(1);
                setDateRange(lastMonth.withDayOfMonth(1),
                    lastMonth.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case "全部报表" -> setDateRange(null, null);
            default -> {
                return;
            }
        }
        handleSearch();
    }

    private void setDateRange(LocalDate startDate, LocalDate endDate) {
        startDatePicker.setValue(startDate);
        endDatePicker.setValue(endDate);
    }

    @FXML
    public void handleRefresh() {
        loadReturnOrders();
        clearDetail();
        searchField.clear();
        statusFilter.setValue("全部");
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today);
        quickDateComboBox.setValue("本月");
        logger.info("刷新退货订单列表");
    }

    @FXML
    public void handleCreateReturn() {
        // 显示一个简单的对话框提示用户使用交易记录创建退货
        showInformationOnlyAlert(
            com.cashier.i18n.I18nManager.getInstance().get("return_order.title"),
            com.cashier.i18n.I18nManager.getInstance().get("runtime.return_help"));
    }

    @FXML
    public void handleExport() {
        if (returnOrderList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_data"));
            return;
        }

        // 准备导出数据
        com.cashier.i18n.I18nManager i18n = com.cashier.i18n.I18nManager.getInstance();
        List<String> headers = List.of(
            i18n.get("return_order_list.return_id"),
            i18n.get("return_order_list.member_name"),
            i18n.get("return_order_list.return_date"),
            i18n.get("return_order_list.total_amount"),
            i18n.get("return_order_list.status"),
            i18n.get("return_order_list.operator"),
            i18n.get("return_order.reason")
        );
        List<String[]> data = new java.util.ArrayList<>();

        for (ReturnOrder order : returnOrderList) {
                data.add(new String[]{
                order.returnOrderId,
                order.memberName != null ? order.memberName : i18n.get(I18nKeys.Common.NONE),
                com.cashier.util.DateTimeFormats.formatStandard(
                    order.returnDate != null ? order.returnDate.atZone(ZoneId.systemDefault()).toLocalDateTime() : java.time.LocalDateTime.now()),
                CurrencyUtil.format(order.totalAmount.doubleValue()),
                order.getStatusText(),
                order.operatorName,
                order.returnReason != null ? order.returnReason : ""
            });
        }

        // 调用导出
        String filePath = com.cashier.util.ExportUtil.export(
            i18n.get("return_order_list.title"),
            headers,
            data,
            com.cashier.util.ExportUtil.ExportFormat.EXCEL,
            i18n.get(I18nKeys.Nav.RETURN_ORDER)
        );

        if (filePath != null) {
            showAlert(Alert.AlertType.INFORMATION, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT),
                    com.cashier.i18n.I18nManager.getInstance().get("runtime.exported_to", filePath));
            logger.info("退货订单导出成功: {}", filePath);
        } else {
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_DATA), com.cashier.i18n.I18nManager.getInstance().get("runtime.export_log_short"));
        }
    }

    @FXML
    public void handleViewOriginalTransaction() {
        if (selectedReturnOrder == null || selectedReturnOrder.originalTransactionId == null) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_RETURN_ORDER));
            return;
        }

        // 查询并显示原交易详情
        try {
            Transaction transaction = DAOFactory.getInstance().getTransactionDAO().findById(selectedReturnOrder.originalTransactionId);
            if (transaction != null) {
                String details = com.cashier.i18n.I18nManager.getInstance().get(
                    "runtime.original_transaction_details",
                    transaction.transactionId,
                    transaction.timestamp,
                    transaction.operatorName != null && !transaction.operatorName.isEmpty()
                        ? transaction.operatorName
                        : com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.NONE),
                    com.cashier.util.I18nUiUtils.paymentMethod(transaction.paymentMethod),
                    String.format("%.2f", transaction.totalAmount),
                    transaction.memberName != null ? transaction.memberName
                        : com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.NONE)
                );
                showInformationOnlyAlert(com.cashier.i18n.I18nManager.getInstance().get("runtime.original_transaction"), details);
            } else {
                showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.original_transaction_missing"));
            }
        } catch (Exception e) {
            logger.error("查询原交易失败", e);
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.ERROR),
                    com.cashier.i18n.I18nManager.getInstance().get("runtime.original_transaction_query_failed", e.getMessage()));
        }
    }

    @FXML
    public void handlePrintReturnReceipt() {
        if (selectedReturnOrder == null) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_RETURN_ORDER));
            return;
        }

        // 获取退货商品明细
        List<ReturnOrderItem> returnItems = DAOFactory.getInstance().getReturnOrderItemDAO().findByReturnOrderId(selectedReturnOrder.returnOrderId);
        
        if (returnItems == null || returnItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_items_empty"));
            return;
        }

        try {
            // 打印退货单据
            String filePath = ReceiptPrinter.printReturnReceipt(selectedReturnOrder, returnItems);
            
            if (filePath != null) {
                showAlert(Alert.AlertType.INFORMATION, com.cashier.i18n.I18nManager.getInstance().get("runtime.print_success"),
                    com.cashier.i18n.I18nManager.getInstance().get("runtime.return_print_success",
                        selectedReturnOrder.returnOrderId, filePath));
                logger.info("退货单据打印成功: {}, 文件路径: {}", selectedReturnOrder.returnOrderId, filePath);
            } else {
                showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get("runtime.print_failed"), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_print_log_error"));
                logger.error("退货单据打印失败: {}", selectedReturnOrder.returnOrderId);
            }
        } catch (Exception e) {
            logger.error("打印退货单据时发生错误", e);
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get("runtime.print_failed"),
                    com.cashier.i18n.I18nManager.getInstance().get("runtime.return_print_error", e.getMessage()));
        }
    }

    @FXML
    public void handleCompleteReturn() {
        if (selectedReturnOrder == null) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_RETURN_ORDER));
            return;
        }

        // 只有已批准的退货单才能完成
        if (!"APPROVED".equals(selectedReturnOrder.status)) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_only_approved"));
            return;
        }

        // 确认完成
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.confirm_complete_return"));
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get(
            "runtime.return_complete_confirm_details",
            selectedReturnOrder.returnOrderId,
            String.format("%.2f", selectedReturnOrder.totalAmount),
            selectedReturnOrder.memberName != null ? selectedReturnOrder.memberName
                : com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.NONE)
        ));

        ButtonType confirmButton = new ButtonType(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.OK), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(confirmButton, cancelButton);

        if (confirmAlert.showAndWait().orElse(cancelButton) != confirmButton) {
            return;
        }

        // 完成退货订单
        boolean result = ReturnService.completeReturnOrder(selectedReturnOrder.returnOrderId);

        if (result) {
            showAlert(Alert.AlertType.INFORMATION, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.SUCCESS),
                com.cashier.i18n.I18nManager.getInstance().get("runtime.return_complete_success",
                    selectedReturnOrder.returnOrderId, String.format("%.2f", selectedReturnOrder.totalAmount)));
            
            // 刷新列表
            loadReturnOrders();
            clearDetail();
            logger.info("退货订单完成: {}", selectedReturnOrder.returnOrderId);
        } else {
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.FAILED), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_complete_failed"));
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        updateStatusForAlert(type, message);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType okButton = new ButtonType(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.OK), ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        alert.showAndWait();
    }

    private void showInformationOnlyAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType okButton = new ButtonType(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.OK), ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
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
