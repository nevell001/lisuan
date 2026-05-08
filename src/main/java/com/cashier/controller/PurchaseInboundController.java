package com.cashier.controller;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalAmount)));
        purchaserColumn.setCellValueFactory(new PropertyValueFactory<>("purchaser"));
    }

    /**
     * 加载可入库订单（已审批但未完成的订单）
     */
    private void loadApprovedOrders() {
        try {
            List<PurchaseOrder> orderData = PurchaseOrderDAO.findByStatus("approved");
            logger.info("找到 {} 个审批通过的订单", orderData.size());

            orders = new HashMap<>();
            for (PurchaseOrder order : orderData) {
                            logger.info("订单: {}, 供应商: {}, 采购日期: {}", order.orderNo, order.supplierName, order.purchaseDate);
                            // 检查是否还有未入库的商品
                            List<PurchaseOrderItem> items = PurchaseOrderItemDAO.findByOrderId(order.id);
                            boolean hasUninbound = items.stream()
                                .anyMatch(item -> item.inboundQuantity < item.quantity);
                            logger.info("  订单明细数: {}, 有未入库: {}", items.size(), hasUninbound);
                            if (hasUninbound) {
                                orders.put(order.id, order);
                            }            }
            logger.info("可入库订单总数: {}", orders.size());
        } catch (SQLException e) {
            logger.error("加载可入库订单失败", e);
            showError("加载可入库订单失败: " + e.getMessage());
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
        countLabel.setText("可入库订单: " + orderList.size() + " 条");
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
    private void handleInbound() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showInboundDialog(selected);
        }
    }

    /**
     * 显示入库对话框
     */
    private void showInboundDialog(PurchaseOrder order) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.setTitle("采购入库 - " + order.orderNo);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

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

            // 入库日期
            DatePicker inboundDatePicker = new DatePicker();
            inboundDatePicker.setValue(java.time.LocalDate.now());

            // 商品明细表格
            TableView<InboundItemWrapper> itemTable = new TableView<>();
            itemTable.setEditable(true);
            itemTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

            TableColumn<InboundItemWrapper, String> productNameCol = new TableColumn<>("商品名称");
            productNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));

            TableColumn<InboundItemWrapper, Number> orderQtyCol = new TableColumn<>("订单数量");
            orderQtyCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getOrderQuantity()));

            TableColumn<InboundItemWrapper, Number> inboundedQtyCol = new TableColumn<>("已入库");
            inboundedQtyCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getInboundQuantity()));

            TableColumn<InboundItemWrapper, Integer> inboundQtyCol = new TableColumn<>("本次入库");
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
                // 直接更新属性值
                e.getRowValue().thisInboundQuantity.set(newQty);
                logger.debug("设置后的值: {}", e.getRowValue().thisInboundQuantity.get());
            });

            TableColumn<InboundItemWrapper, String> unitPriceCol = new TableColumn<>("单价");
            unitPriceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().getUnitPrice())));

            TableColumn<InboundItemWrapper, String> totalCol = new TableColumn<>("小计");
            totalCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f",
                    cellData.getValue().getUnitPrice().multiply(BigDecimal.valueOf(cellData.getValue().thisInboundQuantity.get())))));

            itemTable.getColumns().addAll(productNameCol, orderQtyCol, inboundedQtyCol, inboundQtyCol, unitPriceCol, totalCol);
            
            // 设置行工厂，使选中行背景色更明显
            itemTable.setRowFactory(tv -> {
                TableRow<InboundItemWrapper> row = new TableRow<>();
                ChangeListener<Boolean> changeListener = (obs, wasSelected, isNowSelected) -> {
                    if (isNowSelected) {
                        row.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                        // 为每个单元格设置白色文字
                        for (Node node : row.getChildrenUnmodifiable()) {
                            if (node instanceof Labeled) {
                                ((Labeled) node).setTextFill(javafx.scene.paint.Color.WHITE);
                            }
                        }
                    } else {
                        row.setStyle("");
                        // 恢复默认文字颜色
                        for (Node node : row.getChildrenUnmodifiable()) {
                            if (node instanceof Labeled) {
                                ((Labeled) node).setTextFill(javafx.scene.paint.Color.BLACK);
                            }
                        }
                    }
                };
                row.selectedProperty().addListener(changeListener);
                return row;
            });

            // 加载订单明细
            List<PurchaseOrderItem> items = PurchaseOrderItemDAO.findByOrderId(order.id);
            logger.debug("加载订单明细: 订单ID={}, 商品数={}", order.id, items.size());
            // 使用提取器创建ObservableList，使JavaFX能监听属性变化
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

            // 总金额标签
            Label totalLabel = new Label("本次入库总金额: ¥0.00");

            // 实时更新总金额
            itemTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                BigDecimal total = BigDecimal.ZERO;
                for (InboundItemWrapper wrapper : itemTable.getItems()) {
                    total = total.add(wrapper.unitPrice.multiply(BigDecimal.valueOf(wrapper.thisInboundQuantity.get())));
                }
                totalLabel.setText("本次入库总金额: ¥" + String.format("%.2f", total));
            });

            // 操作提示
            Label hintLabel = new Label("提示：双击'本次入库'列输入入库数量");
            hintLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            // 备注字段
            TextArea remarkArea = new TextArea();
            remarkArea.setPromptText("备注");
            remarkArea.setPrefRowCount(2);

            // 按钮
            Button confirmButton = new Button("确认入库");
            confirmButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

            Button cancelButton = new Button("取消");

            confirmButton.setOnAction(e -> {
                // 检查是否有入库数量
                boolean hasInbound = itemTable.getItems().stream()
                    .anyMatch(wrapper -> wrapper.thisInboundQuantity.get() > 0);

                if (!hasInbound) {
                    showError("请至少输入一个商品的入库数量");
                    return;
                }

                try {
                    // 创建入库单
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    String inboundNo = "IB" + sdf.format(new Date()) + String.format("%04d", new Random().nextInt(10000));

                    PurchaseInbound inbound = new PurchaseInbound();
                    inbound.inboundNo = inboundNo;
                    inbound.orderId = order.id;
                    inbound.orderNo = order.orderNo;
                    inbound.inboundDate = inboundDatePicker.getValue().toString();
                    inbound.operator = currentUser;
                    inbound.remark = remarkArea.getText().trim();

                    // 计算总数量和总金额
                    int totalQty = 0;
                    BigDecimal totalAmount = BigDecimal.ZERO;

                    for (InboundItemWrapper wrapper : itemTable.getItems()) {
                        int qty = wrapper.thisInboundQuantity.get();
                        if (qty > 0) {
                            totalQty += qty;
                            totalAmount = totalAmount.add(wrapper.unitPrice.multiply(BigDecimal.valueOf(qty)));
                        }
                    }

                    inbound.totalQuantity = totalQty;
                    inbound.totalAmount = totalAmount;

                    // 插入入库单
                    PurchaseInboundDAO.insert(inbound);

                    // 插入入库明细并更新订单明细
                    for (InboundItemWrapper wrapper : itemTable.getItems()) {
                        int qty = wrapper.thisInboundQuantity.get();
                        if (qty > 0) {
                            // 创建入库明细
                            PurchaseInboundItem inboundItem = new PurchaseInboundItem();
                            inboundItem.inboundId = inbound.id;
                            inboundItem.orderItemId = wrapper.orderItem.id;
                            inboundItem.productId = wrapper.orderItem.productId;
                            inboundItem.productName = wrapper.orderItem.productName;
                            inboundItem.quantity = qty;
                            inboundItem.unitPrice = wrapper.orderItem.unitPrice;
                            inboundItem.totalPrice = wrapper.orderItem.unitPrice.multiply(BigDecimal.valueOf(qty));
                            PurchaseInboundItemDAO.insert(inboundItem);

                            // 更新订单明细的入库数量
                            PurchaseOrderItemDAO.increaseInboundQuantity(wrapper.orderItem.id, qty);

                            // 更新商品库存
                            ProductDAO.updateQuantity(wrapper.orderItem.productId, qty);
                        }
                    }

                    // 检查订单是否全部入库
                    boolean allInbound = true;
                    List<PurchaseOrderItem> updatedItems = PurchaseOrderItemDAO.findByOrderId(order.id);
                    for (PurchaseOrderItem item : updatedItems) {
                        if (item.inboundQuantity < item.quantity) {
                            allInbound = false;
                            break;
                        }
                    }

                    if (allInbound) {
                        PurchaseOrderDAO.updateStatus(order.id, "completed");
                    }

                    updateStatus("入库成功: " + inboundNo);
                    loadApprovedOrders();
                    dialogStage.close();

                } catch (SQLException ex) {
                    logger.error("入库失败", ex);
                    showError("入库失败: " + ex.getMessage());
                }
            });

            cancelButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(10, confirmButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(
                new Label("订单信息:"),
                infoPane,
                new Label("入库日期:"),
                inboundDatePicker,
                new Label("商品明细:"),
                itemTable,
                totalLabel,
                hintLabel,
                new Label("备注:"),
                remarkArea,
                buttonBox
            );

            Scene scene = new Scene(root, 700, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载订单明细失败", e);
            showError("加载订单明细失败: " + e.getMessage());
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
            infoPane.add(new Label("总金额:"), 0, 3);
            infoPane.add(new Label(CurrencyUtil.format(order.totalAmount.doubleValue())), 1, 3);

            // 商品明细
            TableView<PurchaseOrderItem> itemTable = new TableView<>();
            TableColumn<PurchaseOrderItem, String> nameCol = new TableColumn<>("商品名称");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<PurchaseOrderItem, Number> qtyCol = new TableColumn<>("订单数量");
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            TableColumn<PurchaseOrderItem, Number> inboundedCol = new TableColumn<>("已入库");
            inboundedCol.setCellValueFactory(new PropertyValueFactory<>("inboundQuantity"));

            TableColumn<PurchaseOrderItem, String> priceCol = new TableColumn<>("单价");
            priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().unitPrice)));

            TableColumn<PurchaseOrderItem, String> totalCol = new TableColumn<>("小计");
            totalCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalPrice)));

            itemTable.getColumns().addAll(nameCol, qtyCol, inboundedCol, priceCol, totalCol);

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
                new Label("商品明细:"),
                itemTable,
                closeButton
            );

            Scene scene = new Scene(root, 600, 500);
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
     * 处理查看入库历史
     */
    @FXML
    private void handleViewHistory() {
        showInboundHistoryDialog();
    }

    /**
     * 显示入库历史对话框
     */
    private void showInboundHistoryDialog() {
        try {
            Stage dialogStage = new Stage();
            dialogStage.setTitle("入库历史");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(20));

            // 入库记录表格
            TableView<PurchaseInbound> inboundTable = new TableView<>();

            TableColumn<PurchaseInbound, String> inboundNoCol = new TableColumn<>("入库单号");
            inboundNoCol.setCellValueFactory(new PropertyValueFactory<>("inboundNo"));

            TableColumn<PurchaseInbound, String> orderNoCol = new TableColumn<>("订单号");
            orderNoCol.setCellValueFactory(new PropertyValueFactory<>("orderNo"));

            TableColumn<PurchaseInbound, String> dateCol = new TableColumn<>("入库日期");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("inboundDate"));

            TableColumn<PurchaseInbound, Number> qtyCol = new TableColumn<>("入库数量");
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));

            TableColumn<PurchaseInbound, String> amountCol = new TableColumn<>("入库金额");
            amountCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalAmount)));

            TableColumn<PurchaseInbound, String> operatorCol = new TableColumn<>("操作人");
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

            List<PurchaseInbound> inboundList = PurchaseInboundDAO.findAll();
            inboundTable.setItems(FXCollections.observableArrayList(inboundList));

            Button closeButton = new Button("关闭");
            closeButton.setOnAction(e -> dialogStage.close());

            root.getChildren().addAll(inboundTable, closeButton);

            Scene scene = new Scene(root, 800, 400);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载入库历史失败", e);
            showError("加载入库历史失败: " + e.getMessage());
        }
    }

    /**
     * 显示入库详情对话框
     */
    private void showInboundDetailDialog(PurchaseInbound inbound, Stage parentStage) {
        try {
            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(20));

            // 入库单信息
            GridPane infoPane = new GridPane();
            infoPane.setHgap(10);
            infoPane.setVgap(10);
            infoPane.add(new Label("入库单号:"), 0, 0);
            infoPane.add(new Label(inbound.inboundNo), 1, 0);
            infoPane.add(new Label("订单号:"), 0, 1);
            infoPane.add(new Label(inbound.orderNo), 1, 1);
            infoPane.add(new Label("入库日期:"), 0, 2);
            infoPane.add(new Label(inbound.inboundDate), 1, 2);
            infoPane.add(new Label("入库数量:"), 0, 3);
            infoPane.add(new Label(String.valueOf(inbound.totalQuantity)), 1, 3);
            infoPane.add(new Label("入库金额:"), 0, 4);
            infoPane.add(new Label(CurrencyUtil.format(inbound.totalAmount.doubleValue())), 1, 4);
            infoPane.add(new Label("操作人:"), 0, 5);
            infoPane.add(new Label(inbound.operator), 1, 5);
            infoPane.add(new Label("备注:"), 0, 6);
            infoPane.add(new Label(inbound.remark != null ? inbound.remark : ""), 1, 6);

            // 入库明细
            TableView<PurchaseInboundItem> itemTable = new TableView<>();
            
            TableColumn<PurchaseInboundItem, String> productNameCol = new TableColumn<>("商品名称");
            productNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<PurchaseInboundItem, Number> quantityCol = new TableColumn<>("入库数量");
            quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            TableColumn<PurchaseInboundItem, String> unitPriceCol = new TableColumn<>("单价");
            unitPriceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().unitPrice)));

            TableColumn<PurchaseInboundItem, String> totalCol = new TableColumn<>("小计");
            totalCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalPrice)));

            itemTable.getColumns().addAll(productNameCol, quantityCol, unitPriceCol, totalCol);

            // 加载入库明细
            List<PurchaseInboundItem> items = PurchaseInboundItemDAO.findByInboundId(inbound.id);
            itemTable.setItems(FXCollections.observableArrayList(items));

            Button closeButton = new Button("关闭");
            closeButton.setOnAction(e -> parentStage.close());

            root.getChildren().addAll(
                new Label("入库单信息:"),
                infoPane,
                new Label("入库明细:"),
                itemTable,
                closeButton
            );

            Stage detailStage = new Stage();
            detailStage.setTitle("入库详情 - " + inbound.inboundNo);
            detailStage.initModality(Modality.WINDOW_MODAL);
            detailStage.initOwner(parentStage);

            Scene scene = new Scene(root, 600, 500);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            detailStage.setScene(scene);
            detailStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载入库详情失败", e);
            showError("加载入库详情失败: " + e.getMessage());
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
}