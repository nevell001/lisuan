package com.cashier.controller;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.service.ReturnService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.LoggerFactoryUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
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
        refundMethodComboBox.setItems(FXCollections.observableArrayList("现金", "微信", "支付宝", "银行卡"));
        refundMethodComboBox.setValue("现金");

        // 初始化表格列
        initializePendingOrderTable();
        initializeItemTable();

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
                        setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
                    } catch (Exception e) {
                        setText(item);
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
                } else {
                    setText(CurrencyUtil.format(item));
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        conditionColumn.setCellFactory(column -> new TableCell<ReturnOrderItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "GOOD":
                            setText("完好");
                            setStyle("-fx-text-fill: green;");
                            break;
                        case "DAMAGED":
                            setText("损坏");
                            setStyle("-fx-text-fill: red;");
                            break;
                        case "OPENED":
                            setText("已拆封");
                            setStyle("-fx-text-fill: orange;");
                            break;
                        default:
                            setText(item);
                    }
                }
            }
        });

        itemTable.setItems(itemList);
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
        memberNameLabel.setText(order.memberName != null ? order.memberName : "无");
        totalAmountLabel.setText(CurrencyUtil.format(order.totalAmount.doubleValue()));
        returnDateLabel.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(order.returnDate));
        operatorNameLabel.setText(order.operatorName);
        returnReasonTextArea.setText(order.returnReason != null ? order.returnReason : "");
        originalTransactionLabel.setText(order.originalTransactionId != null ? order.originalTransactionId : "无");
        paymentMethodLabel.setText(order.paymentMethod != null ? order.getPaymentMethodText() : "未设置");

        // 加载退货明细
        loadOrderItems(order.returnOrderId);
    }

    private void loadOrderItems(String returnOrderId) {
        itemList.clear();
        List<ReturnOrderItem> items = ReturnOrderItemDAO.findByReturnOrderId(returnOrderId);
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
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择要审批的退货订单");
            return;
        }

        String approvalComment = approvalCommentTextArea.getText().trim();
        if (approvalComment.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请填写审批意见");
            return;
        }

        String refundMethod = refundMethodComboBox.getValue();
        String paymentMethod = "";
        switch (refundMethod) {
            case "现金": paymentMethod = "CASH"; break;
            case "微信": paymentMethod = "WECHAT"; break;
            case "支付宝": paymentMethod = "ALIPAY"; break;
            case "银行卡": paymentMethod = "CARD"; break;
        }

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
            showAlert(Alert.AlertType.INFORMATION, "审批成功", "退货订单已批准");
            loadPendingOrders();
            clearDetail();
        } else {
            showAlert(Alert.AlertType.ERROR, "审批失败", "审批失败，请查看日志");
        }
    }

    @FXML
    public void handleReject() {
        if (selectedOrder == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择要审批的退货订单");
            return;
        }

        String approvalComment = approvalCommentTextArea.getText().trim();
        if (approvalComment.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请填写拒绝原因");
            return;
        }

        // 确认拒绝
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认拒绝");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确认拒绝此退货订单吗？");
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
            showAlert(Alert.AlertType.INFORMATION, "拒绝成功", "退货订单已拒绝");
            loadPendingOrders();
            clearDetail();
        } else {
            showAlert(Alert.AlertType.ERROR, "操作失败", "操作失败，请查看日志");
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
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择退货订单");
            return;
        }

        // 查找原交易记录
        try {
            Transaction transaction = TransactionDAO.findById(selectedOrder.originalTransactionId);
            if (transaction != null) {
                // 显示交易详情
                String details = String.format(
                    "交易ID: %s\n交易时间: %s\n收银员: %s\n支付方式: %s\n总金额: ¥%.2f\n会员: %s",
                    transaction.transactionId,
                    transaction.timestamp,
                    transaction.operatorName,
                    transaction.paymentMethod,
                    transaction.totalAmount,
                    transaction.memberName != null ? transaction.memberName : "无"
                );
                showAlert(Alert.AlertType.INFORMATION, "原交易详情", details);
            } else {
                showAlert(Alert.AlertType.WARNING, "提示", "未找到原交易记录");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "错误", "查询原交易失败: " + e.getMessage());
        }
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private String getApproverName() {
        return currentUser != null ? currentUser.name : "admin";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
