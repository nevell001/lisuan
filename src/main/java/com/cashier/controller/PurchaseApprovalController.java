package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.service.PurchaseService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.StatusBarManager;
import com.cashier.util.UIOptimizer;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 采购审批控制器
 * 处理采购订单的审批
 */
@SuppressWarnings("unchecked")
public class PurchaseApprovalController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PurchaseApprovalController.class);
    private static final int APPROVAL_ORDER_LIMIT = 500;

    @FXML
    private TableView<PurchaseOrder> orderTable;

    @FXML
    private TableColumn<PurchaseOrder, String> orderNoColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> supplierColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> purchaseDateColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> totalAmountColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> purchaserColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Label countLabel;

    @FXML
    private Label pendingCountLabel;

    @FXML
    private Button approveButton;

    @FXML
    private Button rejectButton;

    @FXML
    private Button viewDetailButton;

    private ObservableList<PurchaseOrder> orderList;
    private Map<Integer, PurchaseOrder> orders;

    // 当前用户（审批人）
    private String currentUser = "admin";

    public void setCurrentUser(com.cashier.model.User user) {
        currentUser = user == null ? null : user.username;
    }

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置表格列
        setupTableColumns();

        // 加载待审批订单
        loadPendingOrders();

        // 设置表格选择模式
        orderTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 添加表格选择监听
        orderTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        orderNoColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        purchaseDateColumn.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        totalAmountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalAmount)));
        purchaserColumn.setCellValueFactory(new PropertyValueFactory<>("purchaser"));
    }

    /**
     * 加载待审批订单
     */
    private void loadPendingOrders() {
        // 打开审批页即查库：放后台，避免整屏冻结
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getPurchaseOrderDAO().findByStatus("pending"),
            orderData -> {
                orders = new HashMap<>();
                for (PurchaseOrder order : orderData) {
                    orders.put(order.id, order);
                }
                renderOrders();
            },
            e -> {
                logger.error("加载待审批订单失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
                orders = new HashMap<>();
                renderOrders();
            });
    }

    /**
     * 加载所有订单
     */
    private void loadAllOrders() {
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getPurchaseOrderDAO().findRecent(APPROVAL_ORDER_LIMIT),
            orderData -> {
                orders = new HashMap<>();
                for (PurchaseOrder order : orderData) {
                    orders.put(order.id, order);
                }
                renderOrders();
            },
            e -> {
                logger.error("加载订单失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
                orders = new HashMap<>();
                renderOrders();
            });
    }

    /** 把内存中的订单刷到表格与计数标签（仅 FX 线程调用） */
    private void renderOrders() {
        orderList = FXCollections.observableArrayList(orders.values());
        orderTable.setItems(orderList);
        updateCountLabel();
    }

    /**
     * 更新订单数量标签（"待审批"计数是一次数据库聚合，同样不能在 FX 线程查）
     */
    private void updateCountLabel() {
        countLabel.setText(I18nManager.getInstance().get("runtime.approval_showing", orderList.size()));
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getPurchaseOrderDAO().countByStatus("pending"),
            pendingCount -> pendingCountLabel.setText(
                I18nManager.getInstance().get("runtime.approval_pending", pendingCount)),
            e -> {
                logger.error("统计待审批订单失败", e);
                pendingCountLabel.setText(I18nManager.getInstance().get("runtime.approval_pending", 0));
            });
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        boolean hasSelection = selected != null && selected.canApprove();

        approveButton.setDisable(!hasSelection);
        rejectButton.setDisable(!hasSelection);
        viewDetailButton.setDisable(selected == null);
    }

    /**
     * 处理审批通过
     */
    @FXML
    public void handleApprove() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showApprovalDialog(selected, "approve");
        } else {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PURCHASE_ORDER));
        }
    }

    /**
     * 处理审批拒绝
     */
    @FXML
    public void handleReject() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showApprovalDialog(selected, "reject");
        } else {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PURCHASE_ORDER));
        }
    }

    /**
     * 显示审批对话框
     */
    private void showApprovalDialog(PurchaseOrder order, String action) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.setTitle("approve".equals(action) ? com.cashier.i18n.I18nManager.getInstance().get("purchase_approval.approve") : com.cashier.i18n.I18nManager.getInstance().get("purchase_approval.reject"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            VBox root = new VBox(15);
            root.setPadding(new javafx.geometry.Insets(20));

            // 订单信息
            GridPane infoPane = new GridPane();
            infoPane.setHgap(10);
            infoPane.setVgap(10);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Checkout.ORDER_NUMBER)), 0, 0);
            infoPane.add(new Label(order.orderNo), 1, 0);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ProductEdit.SUPPLIER)), 0, 1);
            infoPane.add(new Label(order.supplierName), 1, 1);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PURCHASE_DATE)), 0, 2);
            infoPane.add(new Label(order.purchaseDate), 1, 2);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PURCHASER)), 0, 3);
            infoPane.add(new Label(order.purchaser), 1, 3);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.TOTAL_AMOUNT)), 0, 4);
            infoPane.add(new Label(CurrencyUtil.format(order.totalAmount.doubleValue())), 1, 4);

            // 审批意见
            TextArea remarkArea = new TextArea();
            remarkArea.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("return_approval.approval_comment_hint"));
            remarkArea.setPrefRowCount(3);

            // 按钮
            Button confirmButton = new Button("approve".equals(action) ? I18nManager.getInstance().get("runtime.approve") : I18nManager.getInstance().get("return_approval.reject"));
            confirmButton.getStyleClass().add("approve".equals(action) ? "success-button" : "danger-button");

            Button cancelButton = new Button(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrder.CANCEL));

            confirmButton.setOnAction(e -> {
                String remark = remarkArea.getText().trim();
                if ("reject".equals(action) && remark.isEmpty()) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("return_approval.approval_comment_hint"));
                    return;
                }

                try {
                    // 转换状态值：approve -> approved, reject -> rejected
                    String statusValue = "approve".equals(action) ? "approved" : "rejected";
                    
                    PurchaseService.approveOrder(order.id, currentUser, action, remark);

                    updateStatus("订单" + ("approve".equals(action) ? "通过" : "拒绝") + ": " + order.orderNo);
                    com.cashier.service.AuditService.success(currentUser, "PURCHASE", "PURCHASE_APPROVAL",
                        "采购单=" + order.orderNo + ", 结果=" + statusValue, 1);
                    loadPendingOrders();
                    dialogStage.close();

                } catch (SQLException ex) {
                    logger.error("审批失败", ex);
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED) + ": " + ex.getMessage());
                }
            });

            cancelButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(10, confirmButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(
                new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.ORDER_INFO)),
                infoPane,
                new Label(com.cashier.i18n.I18nManager.getInstance().get("return_approval.approval_comment")),
                remarkArea,
                buttonBox
            );

            Scene scene = new Scene(root, 500, 450);
            com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.error("显示审批对话框失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    /**
     * 处理查看详情
     */
    @FXML
    public void handleViewDetail() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showOrderDetailDialog(selected);
        } else {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PURCHASE_ORDER));
        }
    }

    /**
     * 显示订单详情对话框
     */
    private void showOrderDetailDialog(PurchaseOrder order) {
        try {
            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(20));

            // 订单信息
            GridPane infoPane = new GridPane();
            infoPane.setHgap(10);
            infoPane.setVgap(10);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Checkout.ORDER_NUMBER)), 0, 0);
            infoPane.add(new Label(order.orderNo), 1, 0);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ProductEdit.SUPPLIER)), 0, 1);
            infoPane.add(new Label(order.supplierName), 1, 1);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PURCHASE_DATE)), 0, 2);
            infoPane.add(new Label(order.purchaseDate), 1, 2);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.expected_date")), 0, 3);
            infoPane.add(new Label(order.expectedDate != null ? order.expectedDate : "-"), 1, 3);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PURCHASER)), 0, 4);
            infoPane.add(new Label(order.purchaser), 1, 4);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrderList.STATUS_LABEL)), 0, 5);
            infoPane.add(new Label(order.getStatusDisplayName()), 1, 5);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.TOTAL_AMOUNT)), 0, 6);
            infoPane.add(new Label(CurrencyUtil.format(order.totalAmount.doubleValue())), 1, 6);

            // 审批历史
            Label approvalHistoryLabel = new Label(I18nManager.getInstance().get("runtime.approval_history"));
            TextArea approvalHistoryArea = new TextArea();
            approvalHistoryArea.setEditable(false);
            approvalHistoryArea.setPrefRowCount(3);

            try {
                List<PurchaseApproval> approvals = DAOFactory.getInstance().getPurchaseApprovalDAO().findByOrderId(order.id);
                StringBuilder history = new StringBuilder();
                for (PurchaseApproval approval : approvals) {
                    history.append(String.format("%s - %s: %s\n",
                        approval.approvalTime,
                        approval.approver,
                        approval.getActionDisplayName()));
                    if (approval.remark != null && !approval.remark.isEmpty()) {
                        history.append("  意见: ").append(approval.remark).append("\n");
                    }
                }
                approvalHistoryArea.setText(history.toString());
            } catch (SQLException e) {
                logger.error("加载审批历史失败", e);
                approvalHistoryArea.setText(I18nManager.getInstance().get("runtime.approval_history_failed"));
            }

            // 商品明细
            TableView<PurchaseOrderItem> itemTable = new TableView<>();
            TableColumn<PurchaseOrderItem, String> nameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.PRODUCT_NAME));
            nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<PurchaseOrderItem, Number> qtyCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Cart.QUANTITY));
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            TableColumn<PurchaseOrderItem, Number> priceCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.UNIT_PRICE));
            priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

            TableColumn<PurchaseOrderItem, String> totalCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SUBTOTAL));
            totalCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalPrice)));

            itemTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);

            List<PurchaseOrderItem> items = DAOFactory.getInstance().getPurchaseOrderItemDAO().findByOrderId(order.id);
            itemTable.setItems(FXCollections.observableArrayList(items));

            // 创建对话框Stage（需要在按钮回调之前声明）
            final Stage dialogStage = new Stage();
            dialogStage.setTitle(I18nManager.getInstance().get(I18nKeys.Runtime.PURCHASE_ORDER_DETAIL_TITLE, order.orderNo));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            Button closeButton = new Button(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.CLOSE));
            closeButton.setOnAction(e -> dialogStage.close());

            root.getChildren().addAll(
                new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.ORDER_INFO)),
                infoPane,
                approvalHistoryLabel,
                approvalHistoryArea,
                new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PRODUCT_DETAILS)),
                itemTable,
                closeButton
            );

            Scene scene = new Scene(root, 600, 600);
            com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载订单详情失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    public void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            orderList.setAll(orders.values());
        } else {
            orderList.setAll(orders.values().stream()
                .filter(order -> order.orderNo.toLowerCase().contains(searchText) ||
                         order.supplierName.toLowerCase().contains(searchText))
                .collect(Collectors.toList()));
        }
        updateCountLabel();
    }

    /**
     * 处理刷新
     */
    @FXML
    public void handleRefresh() {
        loadPendingOrders();
        updateStatus("已刷新待审批订单");
    }

    /**
     * 刷新订单列表
     */
    public void refreshOrders() {
        loadPendingOrders();
    }

    /**
     * 更新状态
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        com.cashier.util.FXUtils.showError(message);
    }

    private void showWarning(String message) {
        com.cashier.util.FXUtils.showWarningAlert(
            I18nManager.getInstance().get(I18nKeys.Common.WARNING), message);
    }
}
