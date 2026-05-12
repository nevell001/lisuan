package com.cashier.controller;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import java.math.BigDecimal;
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
        try {
            List<PurchaseOrder> orderData = PurchaseOrderDAO.findByStatus("pending");
            orders = new HashMap<>();
            for (PurchaseOrder order : orderData) {
                orders.put(order.id, order);
            }
        } catch (SQLException e) {
            logger.error("加载待审批订单失败", e);
            showError("加载待审批订单失败: " + e.getMessage());
            orders = new HashMap<>();
        }
        orderList = FXCollections.observableArrayList(orders.values());
        orderTable.setItems(orderList);
        updateCountLabel();
    }

    /**
     * 加载所有订单
     */
    private void loadAllOrders() {
        try {
            List<PurchaseOrder> orderData = PurchaseOrderDAO.findAll();
            orders = new HashMap<>();
            for (PurchaseOrder order : orderData) {
                orders.put(order.id, order);
            }
        } catch (SQLException e) {
            logger.error("加载订单失败", e);
            showError("加载订单失败: " + e.getMessage());
            orders = new HashMap<>();
        }
        orderList = FXCollections.observableArrayList(orders.values());
        orderTable.setItems(orderList);
        updateCountLabel();
    }

    /**
     * 更新订单数量标签
     */
    private void updateCountLabel() {
        countLabel.setText("当前显示: " + orderList.size() + " 条");
        try {
            int pendingCount = PurchaseOrderDAO.countByStatus("pending");
            pendingCountLabel.setText("待审批: " + pendingCount + " 条");
        } catch (SQLException e) {
            logger.error("统计待审批订单失败", e);
            pendingCountLabel.setText("待审批: 0 条");
        }
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
    private void handleApprove() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showApprovalDialog(selected, "approve");
        }
    }

    /**
     * 处理审批拒绝
     */
    @FXML
    private void handleReject() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showApprovalDialog(selected, "reject");
        }
    }

    /**
     * 显示审批对话框
     */
    private void showApprovalDialog(PurchaseOrder order, String action) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.setTitle("approve".equals(action) ? "审批通过" : "审批拒绝");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            VBox root = new VBox(15);
            root.setPadding(new javafx.geometry.Insets(20));

            // 订单信息
            GridPane infoPane = new GridPane();
            infoPane.setHgap(10);
            infoPane.setVgap(10);
            infoPane.add(new Label("订单号:"), 0, 0);
            infoPane.add(new Label(order.orderNo), 1, 0);
            infoPane.add(new Label("供应商:"), 0, 1);
            infoPane.add(new Label(order.supplierName), 1, 1);
            infoPane.add(new Label("采购日期:"), 0, 2);
            infoPane.add(new Label(order.purchaseDate), 1, 2);
            infoPane.add(new Label("采购人:"), 0, 3);
            infoPane.add(new Label(order.purchaser), 1, 3);
            infoPane.add(new Label("总金额:"), 0, 4);
            infoPane.add(new Label(CurrencyUtil.format(order.totalAmount.doubleValue())), 1, 4);

            // 审批意见
            TextArea remarkArea = new TextArea();
            remarkArea.setPromptText("请输入审批意见");
            remarkArea.setPrefRowCount(3);

            // 按钮
            Button confirmButton = new Button("approve".equals(action) ? "通过" : "拒绝");
            confirmButton.setStyle("approve".equals(action)
                ? "-fx-background-color: #4CAF50; -fx-text-fill: white;"
                : "-fx-background-color: #f44336; -fx-text-fill: white;");

            Button cancelButton = new Button("取消");

            confirmButton.setOnAction(e -> {
                String remark = remarkArea.getText().trim();
                if ("reject".equals(action) && remark.isEmpty()) {
                    showError("请输入审批意见");
                    return;
                }

                try {
                    // 转换状态值：approve -> approved, reject -> rejected
                    String statusValue = "approve".equals(action) ? "approved" : "rejected";
                    
                    // 更新订单状态
                    PurchaseOrderDAO.approve(order.id, currentUser, remark, statusValue);

                    // 添加审批记录
                    PurchaseApproval approval = new PurchaseApproval();
                    approval.orderId = order.id;
                    approval.approver = currentUser;
                    approval.action = action;
                    approval.remark = remark;
                    PurchaseApprovalDAO.insert(approval);

                    updateStatus("订单" + ("approve".equals(action) ? "通过" : "拒绝") + ": " + order.orderNo);
                    loadPendingOrders();
                    dialogStage.close();

                } catch (SQLException ex) {
                    logger.error("审批失败", ex);
                    showError("审批失败: " + ex.getMessage());
                }
            });

            cancelButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(10, confirmButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(
                new Label("订单信息:"),
                infoPane,
                new Label("审批意见:"),
                remarkArea,
                buttonBox
            );

            Scene scene = new Scene(root, 500, 450);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.error("显示审批对话框失败", e);
            showError("加载对话框失败: " + e.getMessage());
        }
    }

    /**
     * 处理查看详情
     */
    @FXML
    private void handleViewDetail() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showOrderDetailDialog(selected);
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
            infoPane.add(new Label("订单号:"), 0, 0);
            infoPane.add(new Label(order.orderNo), 1, 0);
            infoPane.add(new Label("供应商:"), 0, 1);
            infoPane.add(new Label(order.supplierName), 1, 1);
            infoPane.add(new Label("采购日期:"), 0, 2);
            infoPane.add(new Label(order.purchaseDate), 1, 2);
            infoPane.add(new Label("预计到货:"), 0, 3);
            infoPane.add(new Label(order.expectedDate != null ? order.expectedDate : "-"), 1, 3);
            infoPane.add(new Label("采购人:"), 0, 4);
            infoPane.add(new Label(order.purchaser), 1, 4);
            infoPane.add(new Label("状态:"), 0, 5);
            infoPane.add(new Label(order.getStatusDisplayName()), 1, 5);
            infoPane.add(new Label("总金额:"), 0, 6);
            infoPane.add(new Label(CurrencyUtil.format(order.totalAmount.doubleValue())), 1, 6);

            // 审批历史
            Label approvalHistoryLabel = new Label("审批历史:");
            TextArea approvalHistoryArea = new TextArea();
            approvalHistoryArea.setEditable(false);
            approvalHistoryArea.setPrefRowCount(3);

            try {
                List<PurchaseApproval> approvals = PurchaseApprovalDAO.findByOrderId(order.id);
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
                approvalHistoryArea.setText("加载审批历史失败");
            }

            // 商品明细
            TableView<PurchaseOrderItem> itemTable = new TableView<>();
            TableColumn<PurchaseOrderItem, String> nameCol = new TableColumn<>("商品名称");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<PurchaseOrderItem, Number> qtyCol = new TableColumn<>("数量");
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            TableColumn<PurchaseOrderItem, Number> priceCol = new TableColumn<>("单价");
            priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

            TableColumn<PurchaseOrderItem, String> totalCol = new TableColumn<>("小计");
            totalCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalPrice)));

            itemTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);

            List<PurchaseOrderItem> items = PurchaseOrderItemDAO.findByOrderId(order.id);
            itemTable.setItems(FXCollections.observableArrayList(items));

            // 创建对话框Stage（需要在按钮回调之前声明）
            final Stage dialogStage = new Stage();
            dialogStage.setTitle("订单详情 - " + order.orderNo);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            Button closeButton = new Button("关闭");
            closeButton.setOnAction(e -> dialogStage.close());

            root.getChildren().addAll(
                new Label("订单信息:"),
                infoPane,
                approvalHistoryLabel,
                approvalHistoryArea,
                new Label("商品明细:"),
                itemTable,
                closeButton
            );

            Scene scene = new Scene(root, 600, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载订单详情失败", e);
            showError("加载订单详情失败: " + e.getMessage());
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    private void handleSearch() {
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
    private void handleRefresh() {
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nManager.getInstance().get("label.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}