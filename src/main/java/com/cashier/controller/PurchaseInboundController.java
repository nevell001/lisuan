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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

/**
 * 采购入库控制器
 * 处理采购订单的入库操作
 */
@SuppressWarnings("unchecked")
public class PurchaseInboundController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PurchaseInboundController.class);
    private static final int INBOUND_HISTORY_LIMIT = 500;
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

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
    private Button inboundButton;

    @FXML
    private Button viewDetailButton;

    @FXML
    private Button viewHistoryButton;

    private ObservableList<PurchaseOrder> orderList;
    private Map<Integer, PurchaseOrder> orders;

    // 当前用户
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

        // 加载可入库订单
        loadApprovedOrders();

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
            new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().totalAmount)));
        purchaserColumn.setCellValueFactory(new PropertyValueFactory<>("purchaser"));
    }

    /**
     * 加载可入库订单（已审批但未完成的订单）
     */
    private void loadApprovedOrders() {
        // 这里对每个已审批订单还要查一次明细（N+1 次查询）：整段放后台，否则打开页面必卡
        UIOptimizer.runInBackground(
            () -> {
                List<PurchaseOrder> orderData = DAOFactory.getInstance().getPurchaseOrderDAO().findByStatus("approved");
                logger.info("找到 {} 个审批通过的订单", orderData.size());
                Map<Integer, PurchaseOrder> inboundable = new HashMap<>();
                for (PurchaseOrder order : orderData) {
                    logger.info("订单: {}, 供应商: {}, 采购日期: {}", order.orderNo, order.supplierName, order.purchaseDate);
                    // 检查是否还有未入库的商品
                    List<PurchaseOrderItem> items = DAOFactory.getInstance().getPurchaseOrderItemDAO().findByOrderId(order.id);
                    boolean hasUninbound = items.stream()
                        .anyMatch(item -> item.inboundQuantity < item.quantity);
                    logger.info("  订单明细数: {}, 有未入库: {}", items.size(), hasUninbound);
                    if (hasUninbound) {
                        inboundable.put(order.id, order);
                    }
                }
                logger.info("可入库订单总数: {}", inboundable.size());
                return inboundable;
            },
            inboundable -> {
                orders = inboundable;
                orderList = FXCollections.observableArrayList(orders.values());
                orderTable.setItems(orderList);
                updateCountLabel();
            },
            e -> {
                logger.error("加载可入库订单失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
                orders = new HashMap<>();
                orderList = FXCollections.observableArrayList(orders.values());
                orderTable.setItems(orderList);
                updateCountLabel();
            });
    }

    /**
     * 更新订单数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(I18nManager.getInstance().get("runtime.inbound_count", orderList.size()));
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        boolean hasSelection = selected != null && selected.canInbound();

        inboundButton.setDisable(!hasSelection);
        viewDetailButton.setDisable(selected == null);
        viewHistoryButton.setDisable(false);
    }

    /**
     * 处理入库
     */
    @FXML
    public void handleInbound() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showInboundDialog(selected);
        } else {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PURCHASE_ORDER));
        }
    }

    /**
     * 显示入库对话框
     */
    private void showInboundDialog(PurchaseOrder order) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.setTitle(I18nManager.getInstance().get("runtime.inbound_title", order.orderNo));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

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

            // 入库日期
            DatePicker inboundDatePicker = new DatePicker();
            inboundDatePicker.setValue(java.time.LocalDate.now());

            // 商品明细表格
            TableView<InboundItemWrapper> itemTable = createInboundItemTable();
            
            // 加载订单明细
            loadInboundWrappers(itemTable, order);

            // 总金额标签
            Label totalLabel = new Label(I18nManager.getInstance().get("runtime.inbound_total", CurrencyUtil.format(0)));

            // 实时更新总金额
            itemTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                BigDecimal total = BigDecimal.ZERO;
                for (InboundItemWrapper wrapper : itemTable.getItems()) {
                    total = total.add(wrapper.unitPrice.multiply(BigDecimal.valueOf(wrapper.thisInboundQuantity.get())));
                }
                totalLabel.setText(I18nManager.getInstance().get("runtime.inbound_total", CurrencyUtil.format(total.doubleValue())));
            });

            // 操作提示
            Label hintLabel = new Label(I18nManager.getInstance().get("runtime.inbound_edit_hint"));
            hintLabel.getStyleClass().addAll("text-muted", "text-sm");

            // 备注字段
            TextArea remarkArea = new TextArea();
            remarkArea.setPromptText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Restock.REASON));
            remarkArea.setPrefRowCount(2);

            // 按钮
            Button confirmButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("restock.confirm"));
            confirmButton.getStyleClass().add("success-button");

            Button cancelButton = new Button(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrder.CANCEL));

            confirmButton.setOnAction(e ->
                handleInboundConfirm(dialogStage, itemTable, inboundDatePicker, remarkArea, order));

            cancelButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(10, confirmButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(
                new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.ORDER_INFO)),
                infoPane,
                new Label(I18nManager.getInstance().get("runtime.inbound_date")),
                inboundDatePicker,
                new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PRODUCT_DETAILS)),
                itemTable,
                totalLabel,
                hintLabel,
                new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrderList.NOTES_LABEL)),
                remarkArea,
                buttonBox
            );

            Scene scene = new Scene(root, 700, 600);
            applyCurrentTheme(scene);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载订单明细失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    /** 创建入库明细表格（含可编辑入库数量列与越界修正） */
    private TableView<InboundItemWrapper> createInboundItemTable() {
        TableView<InboundItemWrapper> itemTable = new TableView<>();
        itemTable.setEditable(true);
        itemTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<InboundItemWrapper, String> productNameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.PRODUCT_NAME));
        productNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));

        TableColumn<InboundItemWrapper, Number> orderQtyCol = new TableColumn<>(I18nManager.getInstance().get("purchase_inbound.order_quantity"));
        orderQtyCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getOrderQuantity()));

        TableColumn<InboundItemWrapper, Number> inboundedQtyCol = new TableColumn<>(I18nManager.getInstance().get("purchase_inbound.already_inbound"));
        inboundedQtyCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getInboundQuantity()));

        TableColumn<InboundItemWrapper, Integer> inboundQtyCol = new TableColumn<>(I18nManager.getInstance().get("purchase_inbound.current_inbound"));
        inboundQtyCol.setPrefWidth(100);
        inboundQtyCol.setCellValueFactory(cellData -> cellData.getValue().thisInboundQuantityProperty().asObject());
        inboundQtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        inboundQtyCol.setOnEditCommit(e -> {
            int maxQty = e.getRowValue().orderQuantity - e.getRowValue().inboundQuantity;
            Integer newQty = e.getNewValue();
            logger.debug("编辑提交 - 新值: {}, 最大可入库: {}", newQty, maxQty);
            if (newQty == null || newQty < 0) {
                newQty = 0;
            } else if (newQty > maxQty) {
                newQty = maxQty;
            }
            e.getRowValue().thisInboundQuantity.set(newQty);
            logger.debug("设置后的值: {}", e.getRowValue().thisInboundQuantity.get());
        });

        TableColumn<InboundItemWrapper, String> unitPriceCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.UNIT_PRICE));
        unitPriceCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().getUnitPrice())));

        TableColumn<InboundItemWrapper, String> totalCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SUBTOTAL));
        totalCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f",
                cellData.getValue().getUnitPrice().multiply(BigDecimal.valueOf(cellData.getValue().thisInboundQuantity.get())))));

        itemTable.getColumns().addAll(productNameCol, orderQtyCol, inboundedQtyCol, inboundQtyCol, unitPriceCol, totalCol);
        return itemTable;
    }

    /** 加载订单中尚未完全入库的商品明细 */
    private void loadInboundWrappers(TableView<InboundItemWrapper> itemTable, PurchaseOrder order) throws SQLException {
        List<PurchaseOrderItem> items = DAOFactory.getInstance().getPurchaseOrderItemDAO().findByOrderId(order.id);
        logger.debug("加载订单明细: 订单ID={}, 商品数={}", order.id, items.size());
        ObservableList<InboundItemWrapper> wrappers = FXCollections.observableArrayList(wrapper ->
            new javafx.beans.Observable[] { wrapper.thisInboundQuantityProperty() }
        );
        for (PurchaseOrderItem item : items) {
            logger.debug("商品明细: productName={}, quantity={}, inboundQuantity={}, unitPrice={}",
                item.productName, item.quantity, item.inboundQuantity, item.unitPrice);
            if (item.inboundQuantity < item.quantity) {
                InboundItemWrapper wrapper = new InboundItemWrapper(item);
                logger.debug("创建Wrapper: productName={}, orderQuantity={}, inboundQuantity={}",
                    wrapper.getProductName(), wrapper.getOrderQuantity(), wrapper.getInboundQuantity());
                wrappers.add(wrapper);
            }
        }
        itemTable.setItems(wrappers);
        logger.debug("可入库商品数: {}", wrappers.size());
    }

    /** 处理确认入库：校验、创建入库单与明细、落库、审计并关闭窗口 */
    private void handleInboundConfirm(Stage dialogStage, TableView<InboundItemWrapper> itemTable,
                                      DatePicker inboundDatePicker, TextArea remarkArea, PurchaseOrder order) {
        boolean hasInbound = itemTable.getItems().stream()
            .anyMatch(wrapper -> wrapper.thisInboundQuantity.get() > 0);

        if (!hasInbound) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.inbound_quantity_required"));
            return;
        }

        try {
            String inboundNo = "IB" + java.time.LocalDate.now(java.time.ZoneId.systemDefault())
                .format(com.cashier.util.DateTimeFormats.COMPACT_DATE) + String.format("%04d", SECURE_RANDOM.nextInt(10000));

            PurchaseInbound inbound = new PurchaseInbound();
            inbound.inboundNo = inboundNo;
            inbound.orderId = order.id;
            inbound.orderNo = order.orderNo;
            inbound.inboundDate = inboundDatePicker.getValue().toString();
            inbound.operator = currentUser;
            inbound.remark = remarkArea.getText().trim();

            int totalQty = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<PurchaseInboundItem> inboundItems = new ArrayList<>();
            for (InboundItemWrapper wrapper : itemTable.getItems()) {
                int qty = wrapper.thisInboundQuantity.get();
                if (qty > 0) {
                    totalQty += qty;
                    totalAmount = totalAmount.add(wrapper.unitPrice.multiply(BigDecimal.valueOf(qty)));

                    PurchaseInboundItem inboundItem = new PurchaseInboundItem();
                    inboundItem.orderItemId = wrapper.orderItem.id;
                    inboundItem.productId = wrapper.orderItem.productId;
                    inboundItem.productName = wrapper.orderItem.productName;
                    inboundItem.quantity = qty;
                    inboundItem.unitPrice = wrapper.orderItem.unitPrice;
                    inboundItem.totalPrice = wrapper.orderItem.unitPrice.multiply(BigDecimal.valueOf(qty));
                    inboundItems.add(inboundItem);
                }
            }

            inbound.totalQuantity = totalQty;
            inbound.totalAmount = totalAmount;
            PurchaseService.receiveInbound(inbound, inboundItems);

            updateStatus("入库成功: " + inboundNo);
            com.cashier.service.AuditService.success(currentUser, "PURCHASE", "PURCHASE_INBOUND",
                "入库单=" + inboundNo + ", 采购单=" + order.orderNo + ", 数量=" + totalQty,
                totalQty);
            loadApprovedOrders();
            dialogStage.close();
        } catch (SQLException ex) {
            logger.error("入库失败", ex);
            com.cashier.service.AuditService.failure(currentUser, "PURCHASE", "PURCHASE_INBOUND",
                "采购单=" + order.orderNo + ", 原因=" + ex.getMessage());
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED) + ": " + ex.getMessage());
        }
    }

    /**
     * 入库项包装类
     */
    private static class InboundItemWrapper {
        PurchaseOrderItem orderItem;
        String productName;
        int orderQuantity;
        int inboundQuantity;
        BigDecimal unitPrice;
        javafx.beans.property.IntegerProperty thisInboundQuantity = new javafx.beans.property.SimpleIntegerProperty(0);

        public InboundItemWrapper(PurchaseOrderItem item) {
            this.orderItem = item;
            this.productName = item.productName;
            this.orderQuantity = item.quantity;
            this.inboundQuantity = item.inboundQuantity;
            this.unitPrice = item.unitPrice;
        }

        public String getProductName() { return productName; }
        public int getOrderQuantity() { return orderQuantity; }
        public int getInboundQuantity() { return inboundQuantity; }
        public int getThisInboundQuantity() { return thisInboundQuantity.get(); }
        public javafx.beans.property.IntegerProperty thisInboundQuantityProperty() { return thisInboundQuantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
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
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.TOTAL_AMOUNT)), 0, 3);
            infoPane.add(new Label(CurrencyUtil.format(order.totalAmount.doubleValue())), 1, 3);

            // 商品明细
            TableView<PurchaseOrderItem> itemTable = new TableView<>();
            TableColumn<PurchaseOrderItem, String> nameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.PRODUCT_NAME));
            nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<PurchaseOrderItem, Number> qtyCol = new TableColumn<>(I18nManager.getInstance().get("purchase_inbound.order_quantity"));
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            TableColumn<PurchaseOrderItem, Number> inboundedCol = new TableColumn<>(I18nManager.getInstance().get("purchase_inbound.already_inbound"));
            inboundedCol.setCellValueFactory(new PropertyValueFactory<>("inboundQuantity"));

            TableColumn<PurchaseOrderItem, String> priceCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.UNIT_PRICE));
            priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().unitPrice)));

            TableColumn<PurchaseOrderItem, String> totalCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SUBTOTAL));
            totalCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().totalPrice)));

            itemTable.getColumns().addAll(nameCol, qtyCol, inboundedCol, priceCol, totalCol);

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
                new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PRODUCT_DETAILS)),
                itemTable,
                closeButton
            );

            Scene scene = new Scene(root, 600, 500);
            applyCurrentTheme(scene);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载订单详情失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    /**
     * 处理查看入库历史
     */
    @FXML
    public void handleViewHistory() {
        showInboundHistoryDialog();
    }

    /**
     * 显示入库历史对话框
     */
    private void showInboundHistoryDialog() {
        try {
            Stage dialogStage = new Stage();
            dialogStage.setTitle(com.cashier.i18n.I18nManager.getInstance().get("purchase_inbound.view_history"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(20));

            Label titleLabel = new Label(I18nManager.getInstance().get("purchase_inbound.view_history"));
            titleLabel.getStyleClass().add("view-title");

            // 入库记录表格
            TableView<PurchaseInbound> inboundTable = new TableView<>();
            inboundTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<PurchaseInbound, String> inboundNoCol = new TableColumn<>(I18nManager.getInstance().get("purchase_inbound.inbound_no"));
            inboundNoCol.setMinWidth(170);
            inboundNoCol.setPrefWidth(190);
            inboundNoCol.setCellValueFactory(new PropertyValueFactory<>("inboundNo"));

            TableColumn<PurchaseInbound, String> orderNoCol = new TableColumn<>(I18nManager.getInstance().get("purchase_inbound.order_no"));
            orderNoCol.setMinWidth(170);
            orderNoCol.setPrefWidth(190);
            orderNoCol.setCellValueFactory(new PropertyValueFactory<>("orderNo"));

            TableColumn<PurchaseInbound, String> dateCol = new TableColumn<>(I18nManager.getInstance().get("purchase_inbound.inbound_date"));
            dateCol.setMinWidth(130);
            dateCol.setPrefWidth(150);
            dateCol.setCellValueFactory(new PropertyValueFactory<>("inboundDate"));

            TableColumn<PurchaseInbound, Number> qtyCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("restock.quantity"));
            qtyCol.setMinWidth(110);
            qtyCol.setPrefWidth(130);
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));

            TableColumn<PurchaseInbound, String> amountCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("restock.total_cost"));
            amountCol.setMinWidth(140);
            amountCol.setPrefWidth(170);
            amountCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().totalAmount)));

            TableColumn<PurchaseInbound, String> operatorCol = new TableColumn<>(I18nManager.getInstance().get("purchase_inbound.operator"));
            operatorCol.setMinWidth(130);
            operatorCol.setPrefWidth(150);
            operatorCol.setCellValueFactory(new PropertyValueFactory<>("operator"));

            inboundTable.getColumns().addAll(inboundNoCol, orderNoCol, dateCol, qtyCol, amountCol, operatorCol);

            // 添加双击事件查看详情
            inboundTable.setRowFactory(tv -> {
                TableRow<PurchaseInbound> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        PurchaseInbound inbound = row.getItem();
                        showInboundDetailDialog(inbound, dialogStage);
                    }
                });
                return row;
            });

            List<PurchaseInbound> inboundList = DAOFactory.getInstance().getPurchaseInboundDAO().findRecent(INBOUND_HISTORY_LIMIT);
            inboundTable.setItems(FXCollections.observableArrayList(inboundList));
            inboundTable.setPlaceholder(new Label(I18nManager.getInstance().get("purchase_inbound.history_no_data")));

            Button closeButton = new Button(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.CLOSE));
            closeButton.getStyleClass().add("secondary-button");
            closeButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(closeButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            root.getChildren().addAll(titleLabel, inboundTable, buttonBox);

            Scene scene = new Scene(root, 1080, 520);
            applyCurrentTheme(scene);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载入库历史失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    /**
     * 显示入库详情对话框
     */
    private void showInboundDetailDialog(PurchaseInbound inbound, Stage parentStage) {
        try {
            Stage detailStage = new Stage();
            detailStage.setTitle(I18nManager.getInstance().get("runtime.inbound_detail_title", inbound.inboundNo));
            detailStage.initModality(Modality.WINDOW_MODAL);
            detailStage.initOwner(parentStage);

            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(20));

            // 入库单信息
            GridPane infoPane = new GridPane();
            infoPane.setHgap(10);
            infoPane.setVgap(10);
            infoPane.add(new Label(I18nManager.getInstance().get("runtime.inbound_no")), 0, 0);
            infoPane.add(new Label(inbound.inboundNo), 1, 0);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Checkout.ORDER_NUMBER)), 0, 1);
            infoPane.add(new Label(inbound.orderNo), 1, 1);
            infoPane.add(new Label(I18nManager.getInstance().get("runtime.inbound_date")), 0, 2);
            infoPane.add(new Label(inbound.inboundDate), 1, 2);
            infoPane.add(new Label(I18nManager.getInstance().get("runtime.inbound_quantity")), 0, 3);
            infoPane.add(new Label(String.valueOf(inbound.totalQuantity)), 1, 3);
            infoPane.add(new Label(I18nManager.getInstance().get("runtime.inbound_amount")), 0, 4);
            infoPane.add(new Label(CurrencyUtil.format(inbound.totalAmount.doubleValue())), 1, 4);
            infoPane.add(new Label(I18nManager.getInstance().get("runtime.operator")), 0, 5);
            infoPane.add(new Label(inbound.operator), 1, 5);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrderList.NOTES_LABEL)), 0, 6);
            infoPane.add(new Label(inbound.remark != null ? inbound.remark : ""), 1, 6);

            // 入库明细
            TableView<PurchaseInboundItem> itemTable = new TableView<>();
            
            TableColumn<PurchaseInboundItem, String> productNameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.PRODUCT_NAME));
            productNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProductName()));

            TableColumn<PurchaseInboundItem, Number> quantityCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("restock.quantity"));
            quantityCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getQuantity()));

            TableColumn<PurchaseInboundItem, String> unitPriceCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.UNIT_PRICE));
            unitPriceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().unitPrice)));

            TableColumn<PurchaseInboundItem, String> totalCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SUBTOTAL));
            totalCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().totalPrice)));

            itemTable.getColumns().addAll(productNameCol, quantityCol, unitPriceCol, totalCol);

            // 加载入库明细
            List<PurchaseInboundItem> items = DAOFactory.getInstance().getPurchaseInboundItemDAO().findByInboundId(inbound.id);
            itemTable.setItems(FXCollections.observableArrayList(items));

            Button closeButton = new Button(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.CLOSE));
            closeButton.setOnAction(e -> detailStage.close());

            root.getChildren().addAll(
                new Label(I18nManager.getInstance().get("runtime.inbound_info")),
                infoPane,
                new Label(I18nManager.getInstance().get("runtime.inbound_details")),
                itemTable,
                closeButton
            );

            Scene scene = new Scene(root, 600, 500);
            applyCurrentTheme(scene);

            detailStage.setScene(scene);
            detailStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载入库详情失败", e);
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
        loadApprovedOrders();
        updateStatus("已刷新可入库订单");
    }

    /**
     * 刷新订单列表
     */
    public void refreshOrders() {
        loadApprovedOrders();
    }

    /**
     * 更新状态
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    private void applyCurrentTheme(Scene scene) {
        com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());
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
