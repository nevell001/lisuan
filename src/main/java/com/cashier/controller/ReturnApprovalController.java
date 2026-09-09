package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.dao.*;
import com.cashier.i18n.I18nManager;
import com.cashier.model.*;
import com.cashier.service.ReturnService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 退货审批控制器
 */
public class ReturnApprovalController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnApprovalController.class);

    @FXML private TableView<ReturnOrder> pendingOrderTable;
    @FXML private TableColumn<ReturnOrder, String> returnOrderIdColumn;
    @FXML private TableColumn<ReturnOrder, String> memberNameColumn;
    @FXML private TableColumn<ReturnOrder, String> returnDateColumn;
    @FXML private TableColumn<ReturnOrder, String> totalAmountColumn;
    @FXML private TableColumn<ReturnOrder, String> returnReasonColumn;
    @FXML private TableColumn<ReturnOrder, String> operatorNameColumn;

    @FXML private TableView<ReturnOrderItem> itemTable;
    @FXML private TableColumn<ReturnOrderItem, String> productCodeColumn;
    @FXML private TableColumn<ReturnOrderItem, String> productNameColumn;
    @FXML private TableColumn<ReturnOrderItem, Integer> returnQuantityColumn;
    @FXML private TableColumn<ReturnOrderItem, Double> unitPriceColumn;
    @FXML private TableColumn<ReturnOrderItem, Double> returnAmountColumn;
    @FXML private TableColumn<ReturnOrderItem, String> conditionColumn;

    @FXML private Label returnOrderIdLabel;
    @FXML private Label memberNameLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label returnDateLabel;
    @FXML private Label operatorNameLabel;
    @FXML private TextArea returnReasonTextArea;
    @FXML private Label originalTransactionLabel;
    @FXML private Label paymentMethodLabel;

    @FXML private TextArea approvalCommentTextArea;
    @FXML private ComboBox<String> refundMethodComboBox;

    private ObservableList<ReturnOrder> pendingOrderList = FXCollections.observableArrayList();
    private ObservableList<ReturnOrderItem> itemList = FXCollections.observableArrayList();
    private ReturnOrder selectedOrder;
    private User currentUser;

    @FXML
    public void initialize() {
        logger.info("初始化退货审批控制器");

        // 初始化退款方式下拉框
        refundMethodComboBox.setItems(FXCollections.observableArrayList("CASH", "WECHAT", "ALIPAY", "CARD"));
        com.cashier.util.I18nUiUtils.configureComboBox(
            refundMethodComboBox, com.cashier.util.I18nUiUtils::paymentMethod);
        refundMethodComboBox.setValue("CASH");

        // 初始化表格列
        initializePendingOrderTable();
        initializeItemTable();

        // 设置表格空数据占位符（i18n）
        pendingOrderTable.setPlaceholder(new Label(I18nManager.getInstance().get("return_approval.pending_no_data")));
        itemTable.setPlaceholder(new Label(I18nManager.getInstance().get("return_approval.items_no_data")));

        // 加载待审批订单
        loadPendingOrders();

        // 设置表格选择监听
        pendingOrderTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                selectedOrder = newVal;
                showOrderDetail(newVal);
            }
        );
    }

    private void initializePendingOrderTable() {
        returnOrderIdColumn.setCellValueFactory(new PropertyValueFactory<>("returnOrderId"));
        memberNameColumn.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        returnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDateFormatted"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmountFormatted"));
        returnReasonColumn.setCellValueFactory(new PropertyValueFactory<>("returnReason"));
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
                        java.time.LocalDateTime dateTime = Instant.ofEpochMilli(FormValidator.parseLong(item))
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
                    clearSemanticTextStyles(this);
                } else {
                    try {
                        double amount = FormValidator.parseDouble(item);
                        setText(CurrencyUtil.format(amount));
                        getStyleClass().add("font-bold");
                        applySemanticTextStyle(this, "text-danger");
                    } catch (Exception e) {
                        setText(item);
                        clearSemanticTextStyles(this);
                    }
                }
            }
        });

        pendingOrderTable.setItems(pendingOrderList);
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
                    clearSemanticTextStyles(this);
                } else {
                    setText(CurrencyUtil.format(item));
                    getStyleClass().add("font-bold");
                    clearSemanticTextStyles(this);
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
                        case "GOOD":
                            applySemanticTextStyle(this, "text-success");
                            break;
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

    private void loadPendingOrders() {
        pendingOrderList.clear();
        List<ReturnOrder> orders = ReturnService.getPendingReturnOrders();
        pendingOrderList.addAll(orders);
        logger.info("加载了 {} 条待审批退货订单", orders.size());
    }

    private void showOrderDetail(ReturnOrder order) {
        if (order == null) {
            clearDetail();
            return;
        }

        returnOrderIdLabel.setText(order.returnOrderId);
        memberNameLabel.setText(order.memberName != null ? order.memberName : com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Statistics.NO_DATA));
        totalAmountLabel.setText(CurrencyUtil.format(order.totalAmount.doubleValue()));
        returnDateLabel.setText(order.returnDate != null ? order.returnDate.atZone(ZoneId.systemDefault()).toLocalDateTime()
            .format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME) : "");
        operatorNameLabel.setText(order.operatorName != null && !order.operatorName.isEmpty()
            ? order.operatorName
            : com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Common.NONE));
        returnReasonTextArea.setText(order.returnReason != null ? order.returnReason : "");
        originalTransactionLabel.setText(order.originalTransactionId != null ? order.originalTransactionId : com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Statistics.NO_DATA));
        paymentMethodLabel.setText(order.paymentMethod != null
            ? com.cashier.util.I18nUiUtils.paymentMethod(order.paymentMethod)
            : com.cashier.i18n.I18nManager.getInstance().get("runtime.not_set"));

        // 加载退货明细
        loadOrderItems(order.returnOrderId);
    }

    private void loadOrderItems(String returnOrderId) {
        itemList.clear();
        List<ReturnOrderItem> items = DAOFactory.getInstance().getReturnOrderItemDAO().findByReturnOrderId(returnOrderId);
        itemList.addAll(items);
    }

    private void clearDetail() {
        returnOrderIdLabel.setText("");
        memberNameLabel.setText("");
        totalAmountLabel.setText("");
        returnDateLabel.setText("");
        operatorNameLabel.setText("");
        returnReasonTextArea.setText("");
        originalTransactionLabel.setText("");
        paymentMethodLabel.setText("");
        approvalCommentTextArea.setText("");
        itemList.clear();
    }

    @FXML
    public void handleApprove() {
        if (selectedOrder == null) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_select_for_approval"));
            return;
        }

        String approvalComment = approvalCommentTextArea.getText().trim();
        if (approvalComment.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.approval_comment_required"));
            return;
        }

        String refundMethod = refundMethodComboBox.getValue();
        String paymentMethod = refundMethod;

        // 获取审批人名称（从当前登录用户获取）
        String approverName = getApproverName();

        String returnOrderId = selectedOrder.returnOrderId;  // 保存退货单号
        boolean result = ReturnService.approveReturnOrder(
            returnOrderId,
            approverName,
            approvalComment,
            true  // 审批通过
        );

        if (result) {
            logger.info("退货订单审批通过: {}", returnOrderId);
            showAlert(Alert.AlertType.INFORMATION, com.cashier.i18n.I18nManager.getInstance().get("runtime.approval_success"), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_approved"));
            loadPendingOrders();
            clearDetail();
        } else {
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get("runtime.approval_failed"), com.cashier.i18n.I18nManager.getInstance().get("runtime.approval_log_error"));
        }
    }

    @FXML
    public void handleReject() {
        if (selectedOrder == null) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_select_for_approval"));
            return;
        }

        String approvalComment = approvalCommentTextArea.getText().trim();
        if (approvalComment.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.rejection_reason_required"));
            return;
        }

        // 确认拒绝
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.confirm_reject"));
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.reject_return_confirm"));
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        // 获取审批人名称
        String approverName = getApproverName();
        String returnOrderId = selectedOrder.returnOrderId;  // 保存退货单号

        boolean result = ReturnService.approveReturnOrder(
            returnOrderId,
            approverName,
            approvalComment,
            false  // 拒绝
        );

        if (result) {
            logger.info("退货订单已拒绝: {}", returnOrderId);
            showAlert(Alert.AlertType.INFORMATION, com.cashier.i18n.I18nManager.getInstance().get("runtime.rejection_success"), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_rejected"));
            loadPendingOrders();
            clearDetail();
        } else {
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED), com.cashier.i18n.I18nManager.getInstance().get("runtime.operation_log_error"));
        }
    }

    @FXML
    public void handleRefresh() {
        loadPendingOrders();
        clearDetail();
        approvalCommentTextArea.clear();
        logger.info("刷新待审批订单列表");
    }

    @FXML
    public void handleViewOriginalTransaction() {
        if (selectedOrder == null || selectedOrder.originalTransactionId == null) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_RETURN_ORDER));
            return;
        }

        // 查找原交易记录
        try {
            Transaction transaction = DAOFactory.getInstance().getTransactionDAO().findById(selectedOrder.originalTransactionId);
            if (transaction != null) {
                // 显示交易详情
                String details = I18nManager.getInstance().get("runtime.original_transaction_details",
                    transaction.transactionId,
                    transaction.timestamp,
                    transaction.operatorName != null && !transaction.operatorName.isEmpty()
                        ? transaction.operatorName
                        : I18nManager.getInstance().get(I18nKeys.Common.NONE),
                    com.cashier.util.I18nUiUtils.paymentMethod(transaction.paymentMethod),
                    String.format("%.2f", transaction.totalAmount),
                    transaction.memberName != null ? transaction.memberName : I18nManager.getInstance().get(I18nKeys.Statistics.NO_DATA));
                showInformationOnlyAlert(com.cashier.i18n.I18nManager.getInstance().get("runtime.original_transaction"), details);
            } else {
                showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.original_transaction_missing"));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.ERROR),
                    com.cashier.i18n.I18nManager.getInstance().get("runtime.original_transaction_query_failed", e.getMessage()));
        }
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private String getApproverName() {
        // 宿主 MainController 必须注入当前用户；禁止静默回退 "admin"，避免审批审计失真。
        return currentUser != null ? currentUser.name : "system";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        updateStatusForAlert(type, message);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 仅弹出信息对话框，不将完整内容写入状态栏。
     * 用于显示多行详情（如原交易详情），状态栏仅显示简短提示。
     */
    private void showInformationOnlyAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType okButton = new ButtonType(
            I18nManager.getInstance().get(I18nKeys.Common.OK), ButtonBar.ButtonData.OK_DONE);
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
