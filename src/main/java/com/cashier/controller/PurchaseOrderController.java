package com.cashier.controller;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.IntegerStringConverter;

/**
 * 采购订单控制器
 * 处理采购订单的创建、编辑、提交审批
 */
public class PurchaseOrderController {
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderController.class);

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
    private TableColumn<PurchaseOrder, String> statusColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> purchaserColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilterCombo;

    @FXML
    private Label countLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button viewDetailButton;

    @FXML
    private Button submitApprovalButton;

    private ObservableList<PurchaseOrder> orderList;
    private Map<Integer, PurchaseOrder> orders;
    private Map<Integer, Supplier> suppliers;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置表格列
        setupTableColumns();

        // 设置状态筛选
        statusFilterCombo.getItems().addAll("全部", "待审批", "已审批", "已拒绝", "已完成");
        statusFilterCombo.setValue("全部");

        // 加载数据
        loadSuppliers();
        loadOrders();

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
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatusDisplayName()));
        purchaserColumn.setCellValueFactory(new PropertyValueFactory<>("purchaser"));
    }

    /**
     * 加载供应商数据
     */
    private void loadSuppliers() {
        try {
            List<Supplier> supplierData = SupplierDAO.findAll();
            suppliers = new HashMap<>();
            for (Supplier supplier : supplierData) {
                suppliers.put(supplier.id, supplier);
            }
        } catch (SQLException e) {
            logger.error("加载供应商数据失败", e);
            suppliers = new HashMap<>();
        }
    }

    /**
     * 加载采购订单数据
     */
    private void loadOrders() {
        try {
            List<PurchaseOrder> orderData = PurchaseOrderDAO.findAll();
            orders = new HashMap<>();
            for (PurchaseOrder order : orderData) {
                orders.put(order.id, order);
            }
        } catch (SQLException e) {
            logger.error("加载采购订单数据失败", e);
            showError("加载采购订单数据失败: " + e.getMessage());
            orders = new HashMap<>();
        }
        filterOrders();
    }

    /**
     * 筛选订单
     */
    private void filterOrders() {
        String statusFilter = statusFilterCombo.getValue();
        List<PurchaseOrder> filtered = orders.values().stream()
            .filter(order -> {
                if ("全部".equals(statusFilter)) return true;
                switch (statusFilter) {
                    case "待审批": return "pending".equals(order.status);
                    case "已审批": return "approved".equals(order.status);
                    case "已拒绝": return "rejected".equals(order.status);
                    case "已完成": return "completed".equals(order.status);
                    default: return true;
                }
            })
            .collect(Collectors.toList());

        orderList = FXCollections.observableArrayList(filtered);
        orderTable.setItems(orderList);
        updateCountLabel();
    }

    /**
     * 更新订单数量标签
     */
    private void updateCountLabel() {
        countLabel.setText("订单数量: " + orderList.size());
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        boolean hasSelection = selected != null;
        boolean canEdit = hasSelection && selected.canEdit();

        editButton.setDisable(!canEdit);
        deleteButton.setDisable(!canEdit);
        viewDetailButton.setDisable(!hasSelection);
        submitApprovalButton.setDisable(!canEdit);
    }

    /**
     * 处理添加订单
     */
    @FXML
    private void handleAddOrder() {
        showOrderDialog(null);
    }

    /**
     * 处理编辑订单
     */
    @FXML
    private void handleEditOrder() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showOrderDialog(selected);
        }
    }

    /**
     * 显示订单对话框
     */
    private void showOrderDialog(PurchaseOrder order) {
        try {
            // 创建对话框内容
            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(20));

            // 表单字段
            GridPane gridPane = new GridPane();
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            TextField orderNoField = new TextField();
            orderNoField.setEditable(false);
            orderNoField.setPromptText("自动生成");

            ComboBox<Supplier> supplierCombo = new ComboBox<>();
            supplierCombo.getItems().setAll(suppliers.values());
            supplierCombo.setConverter(new javafx.util.StringConverter<Supplier>() {
                @Override
                public String toString(Supplier supplier) {
                    return supplier != null ? supplier.name : "";
                }
                @Override
                public Supplier fromString(String string) {
                    return suppliers.values().stream()
                        .filter(s -> s.name.equals(string))
                        .findFirst()
                        .orElse(null);
                }
            });

            DatePicker purchaseDatePicker = new DatePicker();
            purchaseDatePicker.setValue(java.time.LocalDate.now());

            DatePicker expectedDatePicker = new DatePicker();
            expectedDatePicker.setValue(java.time.LocalDate.now().plusDays(7));

            TextField purchaserField = new TextField();
            purchaserField.setPromptText("采购人");

            TextArea remarkArea = new TextArea();
            remarkArea.setPromptText("备注");
            remarkArea.setPrefRowCount(2);

            // 商品列表表格
            TableView<PurchaseOrderItem> itemTable = new TableView<>();
            itemTable.setEditable(true);

            TableColumn<PurchaseOrderItem, String> productNameCol = new TableColumn<>("商品名称");
            productNameCol.setPrefWidth(200);
            productNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<PurchaseOrderItem, Integer> quantityCol = new TableColumn<>("数量");
            quantityCol.setPrefWidth(100);
            quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            quantityCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            quantityCol.setOnEditCommit(e -> {
                e.getRowValue().quantity = e.getNewValue();
                e.getRowValue().totalPrice = e.getRowValue().unitPrice.multiply(BigDecimal.valueOf(e.getNewValue()));
                itemTable.refresh();
                updateItemTotal(itemTable);
            });

            TableColumn<PurchaseOrderItem, BigDecimal> unitPriceCol = new TableColumn<>("单价");
            unitPriceCol.setPrefWidth(100);
            unitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
            unitPriceCol.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
            unitPriceCol.setOnEditCommit(e -> {
                e.getRowValue().unitPrice = e.getNewValue();
                e.getRowValue().totalPrice = e.getRowValue().quantity > 0
                    ? e.getNewValue().multiply(BigDecimal.valueOf(e.getRowValue().quantity))
                    : BigDecimal.ZERO;
                itemTable.refresh();
                updateItemTotal(itemTable);
            });

            TableColumn<PurchaseOrderItem, String> totalPriceCol = new TableColumn<>("小计");
            totalPriceCol.setPrefWidth(100);
            totalPriceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalPrice)));

            TableColumn<PurchaseOrderItem, String> actionCol = new TableColumn<>("操作");
            actionCol.setPrefWidth(80);
            actionCol.setCellFactory(col -> new TableCell<PurchaseOrderItem, String>() {
                private final Button deleteBtn = new Button("删除");
                {
                    deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                    deleteBtn.setOnAction(e -> {
                        PurchaseOrderItem item = getTableView().getItems().get(getIndex());
                        itemTable.getItems().remove(item);
                        updateItemTotal(itemTable);
                    });
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteBtn);
                    }
                }
            });

            itemTable.getColumns().addAll(productNameCol, quantityCol, unitPriceCol, totalPriceCol, actionCol);

            ObservableList<PurchaseOrderItem> items = FXCollections.observableArrayList();
            itemTable.setItems(items);

            // 添加商品按钮
            Button addProductButton = new Button("添加商品");
            addProductButton.setOnAction(e -> showProductSelector(itemTable));

            // 总金额标签
            Label totalLabel = new Label("总金额: ¥0.00");

            // 如果是编辑模式，填充数据
            boolean isEdit = order != null;
            if (isEdit) {
                orderNoField.setText(order.orderNo);
                Supplier supplier = suppliers.get(order.supplierId);
                if (supplier != null) {
                    supplierCombo.setValue(supplier);
                }
                purchaseDatePicker.setValue(java.time.LocalDate.parse(order.purchaseDate));
                if (order.expectedDate != null && !order.expectedDate.isEmpty()) {
                    expectedDatePicker.setValue(java.time.LocalDate.parse(order.expectedDate));
                }
                purchaserField.setText(order.purchaser);
                remarkArea.setText(order.remark);

                try {
                    List<PurchaseOrderItem> orderItems = PurchaseOrderItemDAO.findByOrderId(order.id);
                    items.addAll(orderItems);
                } catch (SQLException ex) {
                    logger.error("加载订单明细失败", ex);
                }
                updateItemTotal(itemTable);
            } else {
                // 自动生成订单号
                orderNoField.setText(generateOrderNo());
            }

            // 添加表单元素
            gridPane.add(new Label("订单号:"), 0, 0);
            gridPane.add(orderNoField, 1, 0);
            gridPane.add(new Label("供应商*:"), 0, 1);
            gridPane.add(supplierCombo, 1, 1);
            gridPane.add(new Label("采购日期:"), 0, 2);
            gridPane.add(purchaseDatePicker, 1, 2);
            gridPane.add(new Label("预计到货日期:"), 0, 3);
            gridPane.add(expectedDatePicker, 1, 3);
            gridPane.add(new Label("采购人:"), 0, 4);
            gridPane.add(purchaserField, 1, 4);
            gridPane.add(new Label("备注:"), 0, 5);
            gridPane.add(remarkArea, 1, 5);

            // 创建对话框Stage（需要在按钮回调之前声明）
            final Stage dialogStage = new Stage();
            dialogStage.setTitle(isEdit ? "编辑采购订单" : "新建采购订单");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            // 按钮
            Button saveButton = new Button("保存");
            Button cancelButton = new Button("取消");

            saveButton.setOnAction(e -> {
                if (supplierCombo.getValue() == null) {
                    showError("请选择供应商");
                    return;
                }
                if (items.isEmpty()) {
                    showError("请至少添加一个商品");
                    return;
                }

                Supplier supplier = supplierCombo.getValue();
                PurchaseOrder newOrder = new PurchaseOrder();
                newOrder.orderNo = orderNoField.getText();
                newOrder.supplierId = supplier.id;
                newOrder.supplierName = supplier.name;
                newOrder.purchaseDate = purchaseDatePicker.getValue().toString();
                newOrder.expectedDate = expectedDatePicker.getValue().toString();
                newOrder.totalAmount = calculateTotalAmount(items);
                newOrder.status = "pending";
                newOrder.purchaser = purchaserField.getText().trim();
                newOrder.remark = remarkArea.getText().trim();

                try {
                    if (isEdit) {
                        newOrder.id = order.id;
                        PurchaseOrderDAO.update(newOrder);
                        // 删除旧明细并插入新明细
                        PurchaseOrderItemDAO.deleteByOrderId(order.id);
                        for (PurchaseOrderItem item : items) {
                            item.orderId = order.id;
                            PurchaseOrderItemDAO.insert(item);
                        }
                        updateStatus("采购订单更新成功");
                    } else {
                        PurchaseOrderDAO.insert(newOrder);
                        for (PurchaseOrderItem item : items) {
                            item.orderId = newOrder.id;
                            PurchaseOrderItemDAO.insert(item);
                        }
                        updateStatus("采购订单创建成功");
                    }
                    loadOrders();
                    dialogStage.close();
                } catch (SQLException ex) {
                    logger.error("保存采购订单失败", ex);
                    showError("保存采购订单失败: " + ex.getMessage());
                }
            });

            cancelButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(10, saveButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(gridPane, new Label("商品明细:"), addProductButton, itemTable, totalLabel, buttonBox);

            Scene scene = new Scene(root, 700, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.error("显示订单对话框失败", e);
            showError("加载对话框失败: " + e.getMessage());
        }
    }

    /**
     * 显示商品选择器
     */
    private void showProductSelector(TableView<PurchaseOrderItem> itemTable) {
        try {
            Stage selectorStage = new Stage();
            selectorStage.setTitle("选择商品");
            selectorStage.initModality(Modality.WINDOW_MODAL);
            selectorStage.initOwner(orderTable.getScene().getWindow());

            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(10));

            // 搜索框
            TextField searchField = new TextField();
            searchField.setPromptText("输入商品名称搜索");
            HBox searchBox = new HBox(10, new Label("搜索:"), searchField);

            // 商品表格
            TableView<Product> productTable = new TableView<>();
            TableColumn<Product, String> nameCol = new TableColumn<>("商品名称");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

            TableColumn<Product, Number> costCol = new TableColumn<>("成本价");
            costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));

            TableColumn<Product, Number> stockCol = new TableColumn<>("库存");
            stockCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            productTable.getColumns().addAll(nameCol, costCol, stockCol);

            // 加载商品数据
            List<Product> products = ProductDAO.findAll();
            ObservableList<Product> productList = FXCollections.observableArrayList(products);
            productTable.setItems(productList);

            // 搜索功能
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                String searchText = newVal.toLowerCase();
                if (searchText.isEmpty()) {
                    productTable.setItems(productList);
                } else {
                    productTable.setItems(FXCollections.observableArrayList(productList.stream()
                        .filter(p -> p.name.toLowerCase().contains(searchText))
                        .collect(Collectors.toList())));
                }
            });

            // 添加按钮
            Button addButton = new Button("添加选中商品");
            addButton.setOnAction(e -> {
                Product selected = productTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    PurchaseOrderItem item = new PurchaseOrderItem();
                    item.productId = selected.id;
                    item.productName = selected.name;
                    item.quantity = 1;
                    item.unitPrice = BigDecimal.valueOf(selected.cost);
                    item.totalPrice = item.unitPrice;
                    item.inboundQuantity = 0;
                    itemTable.getItems().add(item);
                    updateItemTotal(itemTable);
                    selectorStage.close();
                }
            });

            Button cancelButton = new Button("取消");
            cancelButton.setOnAction(e -> selectorStage.close());

            HBox buttonBox = new HBox(10, addButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(searchBox, productTable, buttonBox);

            Scene scene = new Scene(root, 500, 400);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            selectorStage.setScene(scene);
            selectorStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载商品数据失败", e);
            showError("加载商品数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品明细总金额
     */
    private void updateItemTotal(TableView<PurchaseOrderItem> itemTable) {
        BigDecimal total = calculateTotalAmount(itemTable.getItems());
        // 更新总金额显示
        VBox parent = (VBox) itemTable.getParent();
        if (parent != null) {
            for (javafx.scene.Node node : parent.getChildren()) {
                if (node instanceof Label && ((Label) node).getText().startsWith("总金额:")) {
                    ((Label) node).setText("总金额: ¥" + String.format("%.2f", total));
                    break;
                }
            }
        }
    }

    /**
     * 计算总金额
     */
    private BigDecimal calculateTotalAmount(ObservableList<PurchaseOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            total = total.add(item.totalPrice);
        }
        return total;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        String prefix = "PO" + dateStr;

        int maxSeq = 0;
        try {
            for (PurchaseOrder order : orders.values()) {
                if (order.orderNo != null && order.orderNo.startsWith(prefix)) {
                    String seqStr = order.orderNo.substring(prefix.length());
                    try {
                        int seq = Integer.parseInt(seqStr);
                        if (seq > maxSeq) {
                            maxSeq = seq;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return prefix + String.format("%04d", maxSeq + 1);
    }

    /**
     * 处理删除订单
     */
    @FXML
    private void handleDeleteOrder() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除采购订单 \"" + selected.orderNo + "\" 吗？");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    PurchaseOrderItemDAO.deleteByOrderId(selected.id);
                    PurchaseOrderDAO.delete(selected.id);
                    orders.remove(selected.id);
                    filterOrders();
                    updateStatus("采购订单删除成功");
                } catch (SQLException e) {
                    logger.error("删除采购订单失败", e);
                    showError("删除采购订单失败: " + e.getMessage());
                }
            }
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
            infoPane.add(new Label(order.purchaser != null ? order.purchaser : "-"), 1, 4);
            infoPane.add(new Label("状态:"), 0, 5);
            infoPane.add(new Label(order.getStatusDisplayName()), 1, 5);
            infoPane.add(new Label("总金额:"), 0, 6);
            infoPane.add(new Label("¥" + String.format("%.2f", order.totalAmount)), 1, 6);

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

            root.getChildren().addAll(new Label("订单信息:"), infoPane, new Label("商品明细:"), itemTable, closeButton);

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
     * 处理提交审批
     */
    @FXML
    private void handleSubmitApproval() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认提交");
            alert.setHeaderText(null);
            alert.setContentText("确定要提交订单 \"" + selected.orderNo + "\" 进行审批吗？");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    // 订单状态已经是pending，不需要改变
                    updateStatus("订单已提交审批: " + selected.orderNo);
                } catch (Exception e) {
                    logger.error("提交审批失败", e);
                    showError("提交审批失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            filterOrders();
        } else {
            String statusFilter = statusFilterCombo.getValue();
            List<PurchaseOrder> filtered = orders.values().stream()
                .filter(order -> {
                    if ("全部".equals(statusFilter)) return true;
                    switch (statusFilter) {
                        case "待审批": return "pending".equals(order.status);
                        case "已审批": return "approved".equals(order.status);
                        case "已拒绝": return "rejected".equals(order.status);
                        case "已完成": return "completed".equals(order.status);
                        default: return true;
                    }
                })
                .filter(order -> order.orderNo.toLowerCase().contains(searchText) ||
                         order.supplierName.toLowerCase().contains(searchText))
                .collect(Collectors.toList());

            orderList = FXCollections.observableArrayList(filtered);
            orderTable.setItems(orderList);
            updateCountLabel();
        }
    }

    /**
     * 处理状态筛选
     */
    @FXML
    private void handleStatusFilter() {
        filterOrders();
    }

    /**
     * 刷新订单列表
     */
    public void refreshOrders() {
        loadOrders();
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
