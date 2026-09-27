package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;
import com.cashier.util.UIOptimizer;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.application.Platform;
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.IntegerStringConverter;

/**
 * 采购订单控制器
 * 处理采购订单的创建、编辑、提交审批
 */
@SuppressWarnings("unchecked")
public class PurchaseOrderController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PurchaseOrderController.class);
    private static final String TEXT_MUTED_STYLE = "text-muted";
    private static final String TEXT_DEFAULT_STYLE = "text-default";
    private static final int PURCHASE_ORDER_LIMIT = 500;
    private static final int PRODUCT_SELECTION_PAGE_SIZE = 500;
    private static final int PURCHASE_SUPPLIER_LIMIT = 500;
    private static final int FIRST_PAGE = 1;
    private final com.cashier.dao.ProductDAORefactored productDAO = com.cashier.dao.DAOFactory.getInstance().getProductDAO();

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
        statusFilterCombo.getItems().addAll("all", "pending", "approved", "rejected", "completed");
        com.cashier.util.I18nUiUtils.configureComboBox(statusFilterCombo, value ->
            "all".equals(value) ? I18nManager.getInstance().get(I18nKeys.Filter.ALL)
                : com.cashier.util.I18nUiUtils.purchaseStatus(value));
        statusFilterCombo.setValue("all");

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
            new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().totalAmount)));
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatusDisplayName()));
        purchaserColumn.setCellValueFactory(new PropertyValueFactory<>("purchaser"));
    }

    /**
     * 加载供应商数据
     */
    private void loadSuppliers() {
        // 打开采购单页即查库：放后台，避免整屏冻结
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getSupplierDAO().findByStatus(true, PURCHASE_SUPPLIER_LIMIT),
            supplierData -> {
                suppliers = new HashMap<>();
                for (Supplier supplier : supplierData) {
                    suppliers.put(supplier.id, supplier);
                }
                // 供应商名解析依赖这张表：先渲染时可能还没到，到齐后再刷一次
                filterOrders();
            },
            e -> {
                logger.error("加载供应商数据失败", e);
                suppliers = new HashMap<>();
            });
    }

    /**
     * 加载采购订单数据
     */
    private void loadOrders() {
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getPurchaseOrderDAO().findRecent(PURCHASE_ORDER_LIMIT),
            orderData -> {
                orders = new HashMap<>();
                for (PurchaseOrder order : orderData) {
                    orders.put(order.id, order);
                }
                filterOrders();
            },
            e -> {
                logger.error("加载采购订单数据失败", e);
                showError(I18nManager.getInstance().get("runtime.purchase_order_load_failed", e.getMessage()));
                orders = new HashMap<>();
                filterOrders();
            });
    }

    /**
     * 筛选订单
     */
    private void filterOrders() {
        String statusFilter = statusFilterCombo.getValue();
        List<PurchaseOrder> filtered = orders.values().stream()
            .filter(order -> {
                if ("all".equals(statusFilter)) return true;
                return statusFilter != null && statusFilter.equals(order.status);
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
        countLabel.setText(I18nManager.getInstance().get("runtime.purchase_order_count", orderList.size()));
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
    public void handleAddOrder() {
        showOrderDialog(null);
    }

    /**
     * 处理编辑订单
     */
    @FXML
    public void handleEditOrder() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showOrderDialog(selected);
        } else {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PURCHASE_ORDER));
        }
    }

    /**
     * 显示订单对话框
     */
    private void showOrderDialog(PurchaseOrder order) {
        try {
            // 创建对话框内容
            VBox root = new VBox(15);
            root.setPadding(new javafx.geometry.Insets(20));
            root.getStyleClass().add("surface-muted");

            // 表单字段
            OrderFormFields form = createOrderFormFields();

            // 如果是编辑模式，填充数据
            boolean isEdit = order != null;
            populateOrderForm(order, isEdit, form);

            // 创建对话框Stage（需要在按钮回调之前声明）
            final Stage dialogStage = new Stage();
            dialogStage.setTitle(isEdit ? com.cashier.i18n.I18nManager.getInstance().get("runtime.purchase_order_edit") : com.cashier.i18n.I18nManager.getInstance().get("runtime.purchase_order_new"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            layoutOrderFormGrid(form, dialogStage);

            TableView<PurchaseOrderItem> itemTable = createOrderItemTable();

            ObservableList<PurchaseOrderItem> items = FXCollections.observableArrayList();
            itemTable.setItems(items);

            // 如果是编辑模式，加载订单明细
            if (isEdit) {
                try {
                    List<PurchaseOrderItem> orderItems = DAOFactory.getInstance().getPurchaseOrderItemDAO().findByOrderId(order.id);
                    items.addAll(orderItems);
                } catch (SQLException ex) {
                    logger.error("加载订单明细失败", ex);
                }
                updateItemTotal(itemTable);
            }

            // 添加商品按钮
            Button addProductButton = new Button(I18nManager.getInstance().get("runtime.add_product"));
            addProductButton.getStyleClass().add("primary-button");
            addProductButton.getStyleClass().addAll("font-bold", "text-md");
            addProductButton.setOnAction(e -> showProductSelector(itemTable));

            // 总金额标签
            Label totalLabel = new Label(I18nManager.getInstance().get("runtime.total_amount_value", CurrencyUtil.format(0)));
            totalLabel.getStyleClass().add(TEXT_DEFAULT_STYLE);
            totalLabel.getStyleClass().add("title-sm");

            // 商品明细标签
            Label itemLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PRODUCT_DETAILS));
            itemLabel.getStyleClass().add(TEXT_DEFAULT_STYLE);
            itemLabel.getStyleClass().add("title-xs");

            // 按钮
            Button saveButton = new Button(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Shortcut.SAVE));
            Button cancelButton = new Button(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrder.CANCEL));

            saveButton.setOnAction(e -> saveOrderFromDialog(
                order,
                isEdit,
                dialogStage,
                form.supplierCombo,
                form.orderNoField,
                form.purchaseDatePicker,
                form.expectedDatePicker,
                form.purchaserField,
                form.remarkArea,
                items));

            cancelButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(10, saveButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(form.grid, itemLabel, addProductButton, itemTable, totalLabel, buttonBox);

            Scene scene = new Scene(root, 750, 550);
            applyCurrentTheme(scene);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.error("显示订单对话框失败", e);
            showError(I18nManager.getInstance().get("runtime.dialog_load_failed", e.getMessage()));
        }
    }

    /** 采购订单表单字段集合 */
    private static class OrderFormFields {
        final GridPane grid;
        final TextField orderNoField;
        final ComboBox<Supplier> supplierCombo;
        final DatePicker purchaseDatePicker;
        final DatePicker expectedDatePicker;
        final TextField purchaserField;
        final TextArea remarkArea;

        OrderFormFields(GridPane grid, TextField orderNoField, ComboBox<Supplier> supplierCombo,
                        DatePicker purchaseDatePicker, DatePicker expectedDatePicker,
                        TextField purchaserField, TextArea remarkArea) {
            this.grid = grid;
            this.orderNoField = orderNoField;
            this.supplierCombo = supplierCombo;
            this.purchaseDatePicker = purchaseDatePicker;
            this.expectedDatePicker = expectedDatePicker;
            this.purchaserField = purchaserField;
            this.remarkArea = remarkArea;
        }
    }

    private OrderFormFields createOrderFormFields() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(15);
        gridPane.setVgap(15);

        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setPrefWidth(100);
        col1.setMinWidth(90);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setPrefWidth(180);
        col2.setMinWidth(150);
        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.ColumnConstraints col3 = new javafx.scene.layout.ColumnConstraints();
        col3.setPrefWidth(100);
        col3.setMinWidth(90);
        javafx.scene.layout.ColumnConstraints col4 = new javafx.scene.layout.ColumnConstraints();
        col4.setPrefWidth(180);
        col4.setMinWidth(150);
        col4.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(col1, col2, col3, col4);

        TextField orderNoField = new TextField();
        orderNoField.setEditable(false);
        orderNoField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ProductEdit.AUTO_GENERATE));

        ComboBox<Supplier> supplierCombo = new ComboBox<>();
        supplierCombo.getItems().setAll(suppliers.values());
        supplierCombo.setConverter(createSupplierStringConverter());

        DatePicker purchaseDatePicker = new DatePicker();
        purchaseDatePicker.setValue(java.time.LocalDate.now());
        DatePicker expectedDatePicker = new DatePicker();
        expectedDatePicker.setValue(java.time.LocalDate.now().plusDays(7));

        TextField purchaserField = new TextField();
        purchaserField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("purchase_inbound.purchaser"));

        TextArea remarkArea = new TextArea();
        remarkArea.setPromptText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Restock.REASON));
        remarkArea.setPrefRowCount(2);
        remarkArea.setPrefHeight(50);
        return new OrderFormFields(gridPane, orderNoField, supplierCombo,
            purchaseDatePicker, expectedDatePicker, purchaserField, remarkArea);
    }

    private void populateOrderForm(PurchaseOrder order, boolean isEdit, OrderFormFields form) {
        if (isEdit) {
            form.orderNoField.setText(order.orderNo);
            Supplier supplier = findSupplierForOrder(order.supplierId);
            if (supplier != null) {
                form.supplierCombo.setValue(supplier);
            }
            form.purchaseDatePicker.setValue(java.time.LocalDate.parse(order.purchaseDate));
            if (order.expectedDate != null && !order.expectedDate.isEmpty()) {
                form.expectedDatePicker.setValue(java.time.LocalDate.parse(order.expectedDate));
            }
            form.purchaserField.setText(order.purchaser);
            form.remarkArea.setText(order.remark);
        } else {
            form.orderNoField.setText(generateOrderNo());
        }
    }

    /** 布局表单网格：订单号/供应商、日期、采购人/备注、供应商详情（含选择联动） */
    private void layoutOrderFormGrid(OrderFormFields form, Stage dialogStage) {
        GridPane gridPane = form.grid;
        gridPane.add(createLabel(I18nManager.getInstance().get("runtime.order_no")), 0, 0);
        gridPane.add(form.orderNoField, 1, 0);
        gridPane.add(createLabel(I18nManager.getInstance().get("runtime.supplier_required")), 2, 0);
        HBox supplierBox = new HBox(10);
        supplierBox.getChildren().add(form.supplierCombo);
        Button newSupplierButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_new"));
        newSupplierButton.getStyleClass().addAll("text-xs", "p-3-8");
        newSupplierButton.setOnAction(e -> showSupplierManagementDialog(dialogStage, form.supplierCombo));
        supplierBox.getChildren().add(newSupplierButton);
        gridPane.add(supplierBox, 3, 0);

        Label supplierInfoLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_info"));
        supplierInfoLabel.getStyleClass().add(TEXT_MUTED_STYLE);
        supplierInfoLabel.getStyleClass().add("font-bold");
        Label supplierDetailLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SUPPLIER_SELECT));
        supplierDetailLabel.getStyleClass().add(TEXT_MUTED_STYLE);
        supplierDetailLabel.getStyleClass().add("text-sm");
        supplierDetailLabel.setWrapText(true);

        form.supplierCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String info = I18nManager.getInstance().get("runtime.supplier_summary",
                    newVal.contactPerson != null ? newVal.contactPerson : "-",
                    newVal.phone != null ? newVal.phone : "-");
                if (newVal.address != null && !newVal.address.isEmpty()) {
                    info = I18nManager.getInstance().get("runtime.supplier_summary_address", info, newVal.address);
                }
                supplierDetailLabel.setText(info);
                supplierDetailLabel.getStyleClass().removeAll(TEXT_MUTED_STYLE, TEXT_DEFAULT_STYLE);
                supplierDetailLabel.getStyleClass().add(TEXT_DEFAULT_STYLE);
            } else {
                supplierDetailLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SUPPLIER_SELECT));
                supplierDetailLabel.getStyleClass().removeAll(TEXT_MUTED_STYLE, TEXT_DEFAULT_STYLE);
                supplierDetailLabel.getStyleClass().add(TEXT_MUTED_STYLE);
            }
        });

        gridPane.add(createLabel(I18nManager.getInstance().get(I18nKeys.Runtime.PURCHASE_DATE)), 0, 1);
        gridPane.add(form.purchaseDatePicker, 1, 1);
        gridPane.add(createLabel(I18nManager.getInstance().get("runtime.expected_date_full")), 2, 1);
        gridPane.add(form.expectedDatePicker, 3, 1);

        gridPane.add(createLabel(I18nManager.getInstance().get(I18nKeys.Runtime.PURCHASER)), 0, 2);
        gridPane.add(form.purchaserField, 1, 2);
        gridPane.add(createLabel(I18nManager.getInstance().get("runtime.notes")), 2, 2);
        gridPane.add(form.remarkArea, 3, 2);

        gridPane.add(supplierInfoLabel, 0, 3);
        gridPane.add(supplierDetailLabel, 1, 3, 3, 1);
    }

    private javafx.util.StringConverter<Supplier> createSupplierStringConverter() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(Supplier supplier) {
                if (supplier == null) {
                    return "";
                }
                return String.format("%s - %s (%s级)",
                    supplier.supplierCode,
                    supplier.name,
                    supplier.rank);
            }

            @Override
            public Supplier fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return null;
                }
                String supplierName = extractSupplierName(string);
                return suppliers.values().stream()
                    .filter(supplier -> supplier.name.equals(supplierName))
                    .findFirst()
                    .orElse(null);
            }
        };
    }

    private String extractSupplierName(String supplierDisplayText) {
        int dashIndex = supplierDisplayText.indexOf(" - ");
        int spaceIndex = supplierDisplayText.lastIndexOf(" (");
        if (dashIndex != -1 && spaceIndex != -1 && spaceIndex > dashIndex) {
            return supplierDisplayText.substring(dashIndex + 3, spaceIndex);
        }
        return supplierDisplayText;
    }

    private Supplier findSupplierForOrder(int supplierId) {
        Supplier supplier = suppliers.get(supplierId);
        if (supplier != null) {
            return supplier;
        }

        try {
            supplier = DAOFactory.getInstance().getSupplierDAO().findById(supplierId);
            if (supplier != null) {
                suppliers.put(supplier.id, supplier);
            }
            return supplier;
        } catch (SQLException e) {
            logger.error("加载采购订单供应商失败，supplierId={}", supplierId, e);
            return null;
        }
    }

    private TableView<PurchaseOrderItem> createOrderItemTable() {
        TableView<PurchaseOrderItem> itemTable = new TableView<>();
        itemTable.setEditable(true);
        itemTable.getStyleClass().add("text-md");
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        itemTable.setPlaceholder(new Label(I18nManager.getInstance().get(I18nKeys.Message.DATA_EMPTY)));

        itemTable.getColumns().addAll(
            createProductNameColumn(),
            createQuantityColumn(itemTable),
            createUnitPriceColumn(itemTable),
            createTotalPriceColumn(),
            createActionColumn(itemTable)
        );
        return itemTable;
    }

    private TableColumn<PurchaseOrderItem, String> createProductNameColumn() {
        TableColumn<PurchaseOrderItem, String> column = new TableColumn<>(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.PRODUCT_NAME));
        column.setPrefWidth(200);
        column.getStyleClass().add("font-bold");
        column.setCellValueFactory(new PropertyValueFactory<>("productName"));
        return column;
    }

    private TableColumn<PurchaseOrderItem, Integer> createQuantityColumn(TableView<PurchaseOrderItem> itemTable) {
        TableColumn<PurchaseOrderItem, Integer> column = new TableColumn<>(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Cart.QUANTITY));
        column.setPrefWidth(100);
        column.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        column.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        column.setOnEditCommit(event -> {
            event.getRowValue().quantity = event.getNewValue();
            event.getRowValue().totalPrice = event.getRowValue().unitPrice.multiply(BigDecimal.valueOf(event.getNewValue()));
            itemTable.refresh();
            updateItemTotal(itemTable);
        });
        return column;
    }

    private TableColumn<PurchaseOrderItem, BigDecimal> createUnitPriceColumn(TableView<PurchaseOrderItem> itemTable) {
        TableColumn<PurchaseOrderItem, BigDecimal> column = new TableColumn<>(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.UNIT_PRICE));
        column.setPrefWidth(100);
        column.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        column.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        column.setOnEditCommit(event -> {
            event.getRowValue().unitPrice = event.getNewValue();
            event.getRowValue().totalPrice = event.getRowValue().quantity > 0
                ? event.getNewValue().multiply(BigDecimal.valueOf(event.getRowValue().quantity))
                : BigDecimal.ZERO;
            itemTable.refresh();
            updateItemTotal(itemTable);
        });
        return column;
    }

    private TableColumn<PurchaseOrderItem, String> createTotalPriceColumn() {
        TableColumn<PurchaseOrderItem, String> column = new TableColumn<>(I18nManager.getInstance().get(I18nKeys.Runtime.SUBTOTAL));
        column.setPrefWidth(100);
        column.getStyleClass().add("font-bold");
        column.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().totalPrice)));
        return column;
    }

    private TableColumn<PurchaseOrderItem, String> createActionColumn(TableView<PurchaseOrderItem> itemTable) {
        TableColumn<PurchaseOrderItem, String> column = new TableColumn<>(I18nManager.getInstance().get("runtime.action"));
        column.setPrefWidth(80);
        column.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button(
                com.cashier.i18n.I18nManager.getInstance().get("inventory_check.delete"));

            {
                deleteButton.getStyleClass().add("danger-button");
                deleteButton.getStyleClass().add("font-bold");
                deleteButton.setOnAction(event -> {
                    PurchaseOrderItem item = getTableView().getItems().get(getIndex());
                    itemTable.getItems().remove(item);
                    updateItemTotal(itemTable);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });
        return column;
    }

    private void saveOrderFromDialog(
            PurchaseOrder order,
            boolean isEdit,
            Stage dialogStage,
            ComboBox<Supplier> supplierCombo,
            TextField orderNoField,
            DatePicker purchaseDatePicker,
            DatePicker expectedDatePicker,
            TextField purchaserField,
            TextArea remarkArea,
            ObservableList<PurchaseOrderItem> items) {

        if (!validateOrderDialogInput(supplierCombo, items)) {
            return;
        }

        PurchaseOrder newOrder = buildPurchaseOrderFromDialog(
            supplierCombo.getValue(),
            orderNoField,
            purchaseDatePicker,
            expectedDatePicker,
            purchaserField,
            remarkArea,
            items);

        try {
            savePurchaseOrder(order, newOrder, items, isEdit);
            loadOrders();
            dialogStage.close();
        } catch (SQLException ex) {
            logger.error("保存采购订单失败", ex);
            showError(I18nManager.getInstance().get("runtime.purchase_order_save_failed", ex.getMessage()));
        }
    }

    private boolean validateOrderDialogInput(ComboBox<Supplier> supplierCombo, ObservableList<PurchaseOrderItem> items) {
        if (supplierCombo.getValue() == null) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SUPPLIER_SELECT));
            return false;
        }
        if (items.isEmpty()) {
            showError(I18nManager.getInstance().get("runtime.purchase_order_product_required"));
            return false;
        }
        return true;
    }

    private PurchaseOrder buildPurchaseOrderFromDialog(
            Supplier supplier,
            TextField orderNoField,
            DatePicker purchaseDatePicker,
            DatePicker expectedDatePicker,
            TextField purchaserField,
            TextArea remarkArea,
            ObservableList<PurchaseOrderItem> items) {

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
        return newOrder;
    }

    private void savePurchaseOrder(
            PurchaseOrder existingOrder,
            PurchaseOrder newOrder,
            ObservableList<PurchaseOrderItem> items,
            boolean isEdit) throws SQLException {

        if (isEdit) {
            newOrder.id = existingOrder.id;
            DAOFactory.getInstance().getPurchaseOrderDAO().update(newOrder);
            DAOFactory.getInstance().getPurchaseOrderItemDAO().deleteByOrderId(existingOrder.id);
            savePurchaseOrderItems(items, existingOrder.id);
            updateStatus("采购订单更新成功");
            return;
        }

        DAOFactory.getInstance().getPurchaseOrderDAO().insert(newOrder);
        savePurchaseOrderItems(items, newOrder.id);
        updateStatus("采购订单创建成功");
    }

    private void savePurchaseOrderItems(ObservableList<PurchaseOrderItem> items, int orderId) throws SQLException {
        for (PurchaseOrderItem item : items) {
            item.orderId = orderId;
            DAOFactory.getInstance().getPurchaseOrderItemDAO().insert(item);
        }
    }

    /**
     * 显示商品选择器
     */
    private void showProductSelector(TableView<PurchaseOrderItem> itemTable) {
        try {
            Stage selectorStage = new Stage();
            selectorStage.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.select_product"));
            selectorStage.initModality(Modality.WINDOW_MODAL);
            selectorStage.initOwner(orderTable.getScene().getWindow());
            selectorStage.setWidth(650);
            selectorStage.setHeight(500);

            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(15));
            root.getStyleClass().add("surface-muted");

            ComboBox<String> categoryCombo = new ComboBox<>();
            categoryCombo.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.all_categories"));
            categoryCombo.setPrefWidth(150);
            categoryCombo.setStyle("-fx-border-radius: 4px; -fx-padding: 5px 10px;");
            TextField searchField = new TextField();
            searchField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.search_product_hint"));
            searchField.setPrefWidth(200);

            HBox searchBox = createProductSelectorFilterBox(searchField, categoryCombo);
            loadProductSelectorCategories(categoryCombo);

            TableView<Product> productTable = createProductSelectorTable();
            loadProductSelectionPage(productTable, searchField.getText(), categoryCombo.getValue());

            wireProductSelectorFilterEvents(searchField, categoryCombo, productTable);
            HBox selectButtonsBox = createProductSelectorButtonsBox(productTable);
            Button addButton = createProductSelectorAddButton(itemTable, productTable, selectorStage);
            Button cancelButton = createProductSelectorCancelButton(selectorStage);

            HBox buttonBox = new HBox(10, addButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            VBox selectBox = new VBox(5, selectButtonsBox, buttonBox);
            selectBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(searchBox, productTable, selectBox);

            Scene scene = new Scene(root);
            applyCurrentTheme(scene);
            selectorStage.setScene(scene);
            selectorStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载商品选择器失败", e);
            showError(I18nManager.getInstance().get("runtime.product_load_short_failed", e.getMessage()));
        }
    }

    /** 创建商品选择器的搜索 + 分类筛选行 */
    private HBox createProductSelectorFilterBox(TextField searchField, ComboBox<String> categoryCombo) {
        Label searchLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("purchase_inbound.search_label"));
        searchLabel.getStyleClass().add(TEXT_DEFAULT_STYLE);
        searchLabel.getStyleClass().addAll("font-bold", "text-md");

        Label categoryLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("product.edit.category"));
        categoryLabel.getStyleClass().add(TEXT_DEFAULT_STYLE);
        categoryLabel.getStyleClass().addAll("font-bold", "text-md");

        HBox searchBox = new HBox(10, searchLabel, searchField, categoryLabel, categoryCombo);
        searchBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return searchBox;
    }

    /** 加载商品分类下拉列表 */
    private void loadProductSelectorCategories(ComboBox<String> categoryCombo) throws SQLException {
        List<Category> categories = DAOFactory.getInstance().getCategoryDAO().findAll();
        ObservableList<String> categoryList = FXCollections.observableArrayList("全部分类");
        for (Category category : categories) {
            categoryList.add(category.name);
        }
        categoryCombo.setItems(categoryList);
    }

    /** 创建商品选择表格（复选框 + 名称/条码/成本/库存列） */
    private TableView<Product> createProductSelectorTable() {
        TableView<Product> productTable = new TableView<>();
        productTable.getStyleClass().add("text-md");
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        productTable.setPlaceholder(new Label(I18nManager.getInstance().get(I18nKeys.Message.DATA_EMPTY)));

        TableColumn<Product, Boolean> selectColumn = new TableColumn<>();
        selectColumn.setPrefWidth(50);
        selectColumn.setSortable(false);
        selectColumn.setCellValueFactory(param -> new SimpleBooleanProperty(true));
        selectColumn.setCellFactory(col -> createProductSelectorCheckBoxCell(productTable));
        productTable.getColumns().add(selectColumn);

        TableColumn<Product, String> nameCol = new TableColumn<>(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.PRODUCT_NAME));
        nameCol.setPrefWidth(200);
        nameCol.getStyleClass().add("font-bold");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> barcodeCol = new TableColumn<>(
            I18nManager.getInstance().get("runtime.barcode"));
        barcodeCol.setPrefWidth(150);
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));

        TableColumn<Product, Number> costCol = new TableColumn<>(
            I18nManager.getInstance().get("runtime.cost_price"));
        costCol.setPrefWidth(100);
        costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));

        TableColumn<Product, Number> stockCol = new TableColumn<>(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Product.STOCK));
        stockCol.setPrefWidth(80);
        stockCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        productTable.getColumns().addAll(nameCol, barcodeCol, costCol, stockCol);

        productTable.getSelectionModel().getSelectedItems().addListener(
            (javafx.collections.ListChangeListener<Product>) c -> Platform.runLater(productTable::refresh));
        return productTable;
    }

    /** 复选框列单元格：勾选与表格选择状态双向同步 */
    private TableCell<Product, Boolean> createProductSelectorCheckBoxCell(TableView<Product> productTable) {
        return new TableCell<Product, Boolean>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    if (!isEmpty()) {
                        Product product = getTableView().getItems().get(getIndex());
                        if (checkBox.isSelected()) {
                            getTableView().getSelectionModel().select(product);
                        } else {
                            getTableView().getSelectionModel().clearSelection(getIndex());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product product = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(productTable.getSelectionModel().getSelectedItems().contains(product));
                    setGraphic(checkBox);
                }
            }
        };
    }

    /** 绑定搜索与分类筛选事件 */
    private void wireProductSelectorFilterEvents(TextField searchField, ComboBox<String> categoryCombo,
                                                 TableView<Product> productTable) {
        searchField.textProperty().addListener((obs, oldVal, newVal) ->
            loadProductSelectionPage(productTable, newVal, categoryCombo.getValue()));
        categoryCombo.setOnAction(e ->
            loadProductSelectionPage(productTable, searchField.getText(), categoryCombo.getValue()));
    }

    /** 创建全选/取消全选按钮组 */
    private HBox createProductSelectorButtonsBox(TableView<Product> productTable) {
        Button selectAllButton = new Button(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Shortcut.SELECT_ALL));
        selectAllButton.setOnAction(e -> productTable.getSelectionModel().selectAll());

        Button deselectAllButton = new Button(I18nManager.getInstance().get("runtime.deselect_all"));
        deselectAllButton.setOnAction(e -> productTable.getSelectionModel().clearSelection());

        return new HBox(10, selectAllButton, deselectAllButton);
    }

    /** 创建“添加选中商品”按钮 */
    private Button createProductSelectorAddButton(TableView<PurchaseOrderItem> itemTable,
                                                  TableView<Product> productTable, Stage selectorStage) {
        Button addButton = new Button(
            com.cashier.i18n.I18nManager.getInstance().get("runtime.add_selected_products"));
        addButton.getStyleClass().addAll("primary-button", "font-bold");
        addButton.setOnAction(e -> addSelectedProductsToOrder(itemTable, productTable, selectorStage));
        return addButton;
    }

    /** 将选中商品加入采购订单明细，已存在的商品跳过 */
    private void addSelectedProductsToOrder(TableView<PurchaseOrderItem> itemTable,
                                            TableView<Product> productTable, Stage selectorStage) {
        ObservableList<Product> selectedProducts = productTable.getSelectionModel().getSelectedItems();
        if (selectedProducts == null || selectedProducts.isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PRODUCT_FIRST));
            return;
        }

        int addedCount = 0;
        for (Product selected : selectedProducts) {
            boolean exists = itemTable.getItems().stream()
                .anyMatch(item -> item.productId == selected.id);

            if (!exists) {
                PurchaseOrderItem item = new PurchaseOrderItem();
                item.productId = selected.id;
                item.productName = selected.name != null ? selected.name : "";
                item.quantity = 1;
                item.unitPrice = selected.getCost().compareTo(BigDecimal.ZERO) > 0
                    ? selected.getCost() : selected.getPrice();
                item.totalPrice = item.unitPrice.multiply(BigDecimal.valueOf(item.quantity));
                item.inboundQuantity = 0;

                itemTable.getItems().add(item);
                addedCount++;
            }
        }

        if (addedCount > 0) {
            itemTable.refresh();
            updateItemTotal(itemTable);
            selectorStage.close();
            logger.info("成功添加 {} 个商品到采购订单", addedCount);
        } else {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.product_already_added"));
        }
    }

    /** 创建取消按钮 */
    private Button createProductSelectorCancelButton(Stage selectorStage) {
        Button cancelButton = new Button(
            com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrder.CANCEL));
        cancelButton.getStyleClass().addAll("secondary-button", "font-bold");
        cancelButton.setOnAction(e -> selectorStage.close());
        return cancelButton;
    }

    private void loadProductSelectionPage(TableView<Product> productTable, String searchText, String category) {
        try {
            String normalizedSearch = searchText == null ? "" : searchText.trim();
            boolean allCategorySelected = category == null || category.isBlank() || "全部分类".equals(category);
            List<Product> products;

            if (!normalizedSearch.isEmpty()) {
                products = productDAO.search(normalizedSearch, FIRST_PAGE, PRODUCT_SELECTION_PAGE_SIZE).getData();
                if (!allCategorySelected) {
                    products = products.stream()
                        .filter(product -> category.equals(product.category))
                        .toList();
                }
            } else if (!allCategorySelected) {
                products = productDAO.findByCategory(category, FIRST_PAGE, PRODUCT_SELECTION_PAGE_SIZE).getData();
            } else {
                products = productDAO.findAll(FIRST_PAGE, PRODUCT_SELECTION_PAGE_SIZE).getData();
            }

            productTable.setItems(FXCollections.observableArrayList(products));
            productTable.getSelectionModel().clearSelection();
        } catch (SQLException e) {
            logger.error("加载采购商品选择列表失败", e);
            showError(I18nManager.getInstance().get("runtime.product_load_short_failed", e.getMessage()));
        }
    }

    /**
     * 更新商品明细总金额
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
                    ((Label) node).setText(I18nManager.getInstance().get("runtime.total_amount_value", CurrencyUtil.format(total.doubleValue())));
                    break;
                }
            }
        } else {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PURCHASE_ORDER));
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
        String dateStr = com.cashier.util.DateTimeFormats.COMPACT_DATE.format(LocalDate.now(ZoneId.systemDefault()));
        String prefix = "PO" + dateStr;

        int maxSeq = 0;
        try {
            for (PurchaseOrder order : orders.values()) {
                if (order.orderNo != null && order.orderNo.startsWith(prefix)) {
                    String seqStr = order.orderNo.substring(prefix.length());
                    try {
                        int seq = FormValidator.parseInt(seqStr);
                        if (seq > maxSeq) {
                            maxSeq = seq;
                        }
                    } catch (NumberFormatException e) {
                        logger.debug("解析订单序号失败: {}", seqStr, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("生成新订单号失败", e);
        }

        return prefix + String.format("%04d", maxSeq + 1);
    }

    /**
     * 处理删除订单
     */
    @FXML
    public void handleDeleteOrder() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.purchase_order_delete_confirm", selected.orderNo));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    DAOFactory.getInstance().getPurchaseOrderItemDAO().deleteByOrderId(selected.id);
                    DAOFactory.getInstance().getPurchaseOrderDAO().delete(selected.id);
                    orders.remove(selected.id);
                    filterOrders();
                    updateStatus("采购订单删除成功");
                } catch (SQLException e) {
                    logger.error("删除采购订单失败", e);
                    showError(I18nManager.getInstance().get("runtime.purchase_order_delete_failed", e.getMessage()));
                }
            }
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
            infoPane.add(new Label(order.purchaser != null ? order.purchaser : "-"), 1, 4);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrderList.STATUS_LABEL)), 0, 5);
            infoPane.add(new Label(order.getStatusDisplayName()), 1, 5);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.TOTAL_AMOUNT)), 0, 6);
            infoPane.add(new Label(CurrencyUtil.format(order.totalAmount.doubleValue())), 1, 6);

            // 商品明细
            TableView<PurchaseOrderItem> itemTable = new TableView<>();
            TableColumn<PurchaseOrderItem, String> nameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.PRODUCT_NAME));
            nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<PurchaseOrderItem, Number> qtyCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Cart.QUANTITY));
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            TableColumn<PurchaseOrderItem, Number> priceCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnApproval.UNIT_PRICE));
            priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

            TableColumn<PurchaseOrderItem, String> totalCol = new TableColumn<>(I18nManager.getInstance().get(I18nKeys.Runtime.SUBTOTAL));
            totalCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().totalPrice)));

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

            root.getChildren().addAll(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.ORDER_INFO)), infoPane, new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PRODUCT_DETAILS)), itemTable, closeButton);

            Scene scene = new Scene(root, 600, 500);
            applyCurrentTheme(scene);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载订单详情失败", e);
            showError(I18nManager.getInstance().get("runtime.purchase_order_detail_failed", e.getMessage()));
        }
    }

    /**
     * 处理提交审批
     */
    @FXML
    public void handleSubmitApproval() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.purchase_order_submit_confirm", selected.orderNo));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    // 订单状态已经是pending，不需要改变
                    updateStatus("订单已提交审批: " + selected.orderNo);
                } catch (Exception e) {
                    logger.error("提交审批失败", e);
                    showError(I18nManager.getInstance().get("runtime.purchase_order_submit_failed", e.getMessage()));
                }
            }
        } else {
            showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PURCHASE_ORDER));
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    public void handleSearch() {
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
    public void handleStatusFilter() {
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
        com.cashier.util.FXUtils.showError(message);
    }

    private void showWarning(String message) {
        com.cashier.util.FXUtils.showWarningAlert(
            I18nManager.getInstance().get(I18nKeys.Common.WARNING), message);
    }

    /**
     * 创建带样式的标签
     * @param text 标签文字
     * @return Label对象
     */
    /**
     * 显示供应商管理对话框
     */
    private void showSupplierManagementDialog(Stage parentStage, ComboBox<Supplier> supplierCombo) {
        try {
            // 简化的供应商添加对话框
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_new"));
            dialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_add"));

            // 创建表单
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            TextField codeField = new TextField();
            codeField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_code_hint"));

            TextField nameField = new TextField();
            nameField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("purchase_report.supplier_name"));

            TextField contactField = new TextField();
            contactField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("supplier.contact"));

            TextField phoneField = new TextField();
            phoneField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("supplier.phone"));

            TextField addressField = new TextField();
            addressField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("supplier.address"));

            ComboBox<String> rankCombo = new ComboBox<>();
            rankCombo.getItems().addAll("A", "B", "C");
            rankCombo.setValue("C");

            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_code_required")), 0, 0);
            grid.add(codeField, 1, 0);
            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_name_required")), 0, 1);
            grid.add(nameField, 1, 1);
            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.contact")), 0, 2);
            grid.add(contactField, 1, 2);
            grid.add(new Label(I18nManager.getInstance().get("runtime.phone")), 0, 3);
            grid.add(phoneField, 1, 3);
            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.address")), 0, 4);
            grid.add(addressField, 1, 4);
            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.level")), 0, 5);
            grid.add(rankCombo, 1, 5);

            dialog.getDialogPane().setContent(grid);

            // 添加按钮
            dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK,
                ButtonType.CANCEL
            );

            // 验证输入
            Runnable validate = () -> {
                boolean valid = !codeField.getText().trim().isEmpty() 
                             && !nameField.getText().trim().isEmpty();
                dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
            };

            codeField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());
            nameField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());

            // 显示对话框
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initOwner(parentStage);

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        Supplier supplier = new Supplier();
                        supplier.supplierCode = codeField.getText().trim();
                        supplier.name = nameField.getText().trim();
                        supplier.contactPerson = contactField.getText().trim();
                        supplier.phone = phoneField.getText().trim();
                        supplier.address = addressField.getText().trim();
                        supplier.rank = rankCombo.getValue();
                        supplier.status = true;

                        DAOFactory.getInstance().getSupplierDAO().insert(supplier);
                        
                        // 刷新供应商列表
                        loadSuppliers();
                        supplierCombo.getItems().setAll(suppliers.values());
                        
                        // 选择新添加的供应商
                        Supplier newSupplier = suppliers.values().stream()
                            .filter(s -> s.supplierCode.equals(supplier.supplierCode))
                            .findFirst()
                            .orElse(null);
                        if (newSupplier != null) {
                            supplierCombo.setValue(newSupplier);
                        }
                        
                        updateStatus("供应商添加成功");
                        
                    } catch (SQLException e) {
                        logger.error("添加供应商失败", e);
                        showError(I18nManager.getInstance().get("runtime.supplier_add_failed", e.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            logger.error("显示供应商管理对话框失败", e);
            showError(I18nManager.getInstance().get("runtime.supplier_dialog_failed", e.getMessage()));
        }
    }

    /**
     * 创建标签
     */
    private Label createLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(TEXT_DEFAULT_STYLE);
        label.getStyleClass().addAll("font-bold", "text-md");
        return label;
    }

    private void applyCurrentTheme(Scene scene) {
        com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());
    }
}
