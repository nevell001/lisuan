package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.service.ReturnService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.StatusBarManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.Callback;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建退货订单对话框控制器
 */
public class CreateReturnOrderDialogController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(CreateReturnOrderDialogController.class);

    @FXML private Label transactionIdLabel;
    @FXML private Label transactionAmountLabel;
    @FXML private Label transactionDateLabel;
    @FXML private Label paymentMethodLabel;
    @FXML private Label memberNameLabel;

    @FXML private CheckBox selectAllCheckBox;
    @FXML private TableView<ReturnItem> returnItemTable;
    @FXML private TableColumn<ReturnItem, Boolean> selectColumn;
    @FXML private TableColumn<ReturnItem, String> productCodeColumn;
    @FXML private TableColumn<ReturnItem, String> productNameColumn;
    @FXML private TableColumn<ReturnItem, Integer> originalQuantityColumn;
    @FXML private TableColumn<ReturnItem, Integer> returnQuantityColumn;
    @FXML private TableColumn<ReturnItem, Double> unitPriceColumn;
    @FXML private TableColumn<ReturnItem, Double> returnAmountColumn;
    @FXML private TableColumn<ReturnItem, String> conditionColumn;

    @FXML private TextField returnReasonField;
    @FXML private ComboBox<String> refundMethodComboBox;
    @FXML private TextArea notesArea;
    @FXML private Label totalReturnAmountLabel;

    private ObservableList<ReturnItem> returnItems = FXCollections.observableArrayList();
    private Transaction originalTransaction;

    /**
     * 整单原价合计与实付金额：退款单价按两者比例折算。
     * 会员折扣与促销都作用在整单上、明细只存原价，不折算会把优惠一并退给顾客。
     */
    private BigDecimal grossAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private Stage dialogStage;
    private boolean submitted = false;
    private User currentUser;

    // 退货商品临时类
    public static class ReturnItem {
        private BooleanProperty selected = new SimpleBooleanProperty(false);
        public int productId;
        public String productCode;
        public String productName;
        public int originalQuantity;
        public int returnQuantity;
        public double unitPrice;
        public String condition = "GOOD";

        public ReturnItem() {
            selected.addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    returnQuantity = 0;
                }
            });
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        // Getter方法供PropertyValueFactory使用
        public int getProductId() {
            return productId;
        }

        public String getProductCode() {
            return productCode;
        }

        public String getProductName() {
            return productName;
        }

        public int getOriginalQuantity() {
            return originalQuantity;
        }

        public int getReturnQuantity() {
            return returnQuantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public String getCondition() {
            return condition;
        }

        public double getReturnAmount() {
            return returnQuantity * unitPrice;
        }
    }

    @FXML
    public void initialize() {
        logger.info("初始化创建退货订单对话框");

        // 初始化退款方式下拉框
        refundMethodComboBox.setItems(FXCollections.observableArrayList(
            "CASH", "WECHAT", "ALIPAY", "CARD"
        ));
        com.cashier.util.I18nUiUtils.configureComboBox(
            refundMethodComboBox, com.cashier.util.I18nUiUtils::paymentMethod);
        refundMethodComboBox.setValue("CASH");

        // 初始化表格列
        initializeTableColumns();

        // 初始化全选复选框
        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (ReturnItem item : returnItems) {
                item.setSelected(newVal);
            }
            returnItemTable.refresh();
            calculateTotal();
        });
    }

    private void initializeTableColumns() {
        // 选择列（复选框）
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        // 商品编号
        productCodeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProductCode()));

        // 商品名称
        productNameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProductName()));

        // 原数量
        originalQuantityColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getOriginalQuantity()));

        // 退货数量（可编辑）
        returnQuantityColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getReturnQuantity()));
        returnQuantityColumn.setCellFactory(createReturnQuantityCellFactory());

        // 单价
        unitPriceColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getUnitPrice()));
        unitPriceColumn.setCellFactory(createCurrencyCellFactory());

        // 退货金额
        returnAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getReturnAmount()));
        returnAmountColumn.setCellFactory(createCurrencyCellFactory());

        // 商品状态（可编辑）
        conditionColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getCondition()));
        conditionColumn.setCellFactory(createConditionCellFactory());

        returnItemTable.setItems(returnItems);
    }

    /** 退货数量列：可编辑 Spinner，联动选中状态与总额 */
    private Callback<TableColumn<ReturnItem, Integer>, TableCell<ReturnItem, Integer>> createReturnQuantityCellFactory() {
        return column -> new TableCell<ReturnItem, Integer>() {
            private Spinner<Integer> spinner;

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                ReturnItem returnItem = getTableRow().getItem();
                if (spinner == null) {
                    spinner = new Spinner<>(0, returnItem.originalQuantity, returnItem.returnQuantity);
                    spinner.setEditable(true);
                    spinner.setPrefWidth(80);
                    spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (returnItem.isSelected()) {
                            returnItem.returnQuantity = newVal;
                            calculateTotal();
                        }
                    });
                    returnItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        spinner.setDisable(!newVal);
                        if (!newVal) {
                            spinner.getValueFactory().setValue(0);
                        }
                    });
                }
                spinner.getValueFactory().setValue(returnItem.returnQuantity);
                spinner.setDisable(!returnItem.isSelected());
                setGraphic(spinner);
            }
        };
    }

    /** 金额列：格式化两位小数的只读单元格 */
    private Callback<TableColumn<ReturnItem, Double>, TableCell<ReturnItem, Double>> createCurrencyCellFactory() {
        return column -> new TableCell<ReturnItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : CurrencyUtil.format(item));
            }
        };
    }

    /** 商品状态列：可编辑下拉框（GOOD/DAMAGED/OPENED） */
    private Callback<TableColumn<ReturnItem, String>, TableCell<ReturnItem, String>> createConditionCellFactory() {
        return column -> new TableCell<ReturnItem, String>() {
            private ComboBox<String> comboBox;

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                ReturnItem returnItem = getTableRow().getItem();
                if (comboBox == null) {
                    comboBox = new ComboBox<>(FXCollections.observableArrayList("GOOD", "DAMAGED", "OPENED"));
                    com.cashier.util.I18nUiUtils.configureComboBox(
                        comboBox, com.cashier.util.I18nUiUtils::itemCondition);
                    comboBox.setMinWidth(118);
                    comboBox.setPrefWidth(128);
                    comboBox.setOnAction(event -> returnItem.condition = comboBox.getValue());
                }
                comboBox.setValue(returnItem.condition);
                setGraphic(comboBox);
            }
        };
    }

    /**
     * 设置原交易信息
     */
    public void setOriginalTransaction(Transaction transaction, List<Product> items) {
        this.originalTransaction = transaction;

        // 显示原交易信息
        transactionIdLabel.setText(transaction.transactionId);
        transactionAmountLabel.setText(CurrencyUtil.format(transaction.finalAmount.doubleValue()));
        transactionDateLabel.setText(transaction.timestamp);  // timestamp已经是格式化的字符串
        paymentMethodLabel.setText(com.cashier.util.I18nUiUtils.paymentMethod(transaction.paymentMethod));
        memberNameLabel.setText(transaction.memberName != null ? transaction.memberName : com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Statistics.NO_DATA));

        // 设置退款方式
        refundMethodComboBox.setValue(transaction.paymentMethod);

        // 打折/促销成交的订单，退款单价需按整单实付比例折算
        computeRefundRatio(items);

        // 加载退货商品
        loadReturnItems(items);
    }

    /**
     * 计算整单原价合计与实付金额，供退款单价折算使用。
     */
    private void computeRefundRatio(List<Product> items) {
        BigDecimal gross = BigDecimal.ZERO;
        for (Product product : items) {
            gross = gross.add(product.getPrice().multiply(BigDecimal.valueOf(product.quantity)));
        }
        grossAmount = gross;
        paidAmount = originalTransaction.finalAmount != null ? originalTransaction.finalAmount : gross;
        if (ReturnService.refundRatio(paidAmount, grossAmount).compareTo(BigDecimal.ONE) < 0) {
            logger.info("退货按整单实付比例折算: 原价合计={}, 实付={}", gross, paidAmount);
        }
    }

    /**
     * 加载退货商品列表
     */
    private void loadReturnItems(List<Product> items) {
        returnItems.clear();

        for (Product product : items) {
            ReturnItem returnItem = new ReturnItem();
            returnItem.productId = product.id;
            returnItem.productCode = product.productCode != null ? product.productCode : "";
            returnItem.productName = product.name;
            returnItem.originalQuantity = product.quantity;
            returnItem.returnQuantity = product.quantity;
            // 退款单价按整单实付比例折算（ReturnService 统一口径）
            returnItem.unitPrice = ReturnService
                .refundUnitPrice(product.getPrice(), paidAmount, grossAmount)
                .doubleValue();
            returnItems.add(returnItem);
        }

        returnItemTable.refresh();
        calculateTotal();
    }

    /**
     * 计算退货总金额
     */
    private void calculateTotal() {
        double total = 0;
        for (ReturnItem item : returnItems) {
            if (item.isSelected()) {
                total += item.getReturnAmount();
            }
        }
        totalReturnAmountLabel.setText(CurrencyUtil.format(total));
    }

    /**
     * 处理全选
     */
    @FXML
    public void handleSelectAll() {
        // 逻辑已在 initialize() 中实现
    }

    /**
     * 处理取消
     */
    @FXML
    public void handleCancel() {
        submitted = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * 验证退货商品 - 防止重复退货
     */
    private String validateReturnItems() {
        try {
            // 查询该交易的所有已存在退货订单（不包括已拒绝的）
            List<ReturnOrder> existingReturns = DAOFactory.getInstance().getReturnOrderDAO().findByOriginalTransactionId(
                originalTransaction.transactionId
            );

            if (existingReturns.isEmpty()) {
                return null;  // 没有退货记录，允许创建
            }

            // 统计所有已退货的商品和数量
            java.util.Map<Integer, Integer> returnedQuantities = new java.util.HashMap<>();
            for (ReturnOrder returnOrder : existingReturns) {
                List<ReturnOrderItem> items = DAOFactory.getInstance().getReturnOrderItemDAO().findByReturnOrderId(returnOrder.returnOrderId);
                for (ReturnOrderItem item : items) {
                    returnedQuantities.put(item.productId,
                        returnedQuantities.getOrDefault(item.productId, 0) + item.returnQuantity);
                }
            }

            // 检查当前要退货的商品是否会超过原交易数量
            for (ReturnItem item : returnItems) {
                if (item.isSelected() && item.returnQuantity > 0) {
                    int returnedQty = returnedQuantities.getOrDefault(item.productId, 0);
                    int totalReturnQty = returnedQty + item.returnQuantity;

                    if (totalReturnQty > item.originalQuantity) {
                        return com.cashier.i18n.I18nManager.getInstance().get(
                            "runtime.return_quantity_exceeded", item.productName,
                            item.originalQuantity, returnedQty, item.returnQuantity);
                    }
                }
            }

            return null;  // 验证通过

        } catch (Exception e) {
            logger.error("验证退货商品失败", e);
            return com.cashier.i18n.I18nManager.getInstance().get("runtime.return_validation_error", e.getMessage());
        }
    }

    /**
     * 处理提交
     */
    @FXML
    public void handleSubmit() {
        // 验证输入
        String returnReason = returnReasonField.getText().trim();
        if (returnReason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("return_order.return_reason_hint"));
            return;
        }

        if (!hasSelectedReturnItem()) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_item_required"));
            return;
        }

        // 验证退货订单 - 防止重复退货
        String validationResult = validateReturnItems();
        if (validationResult != null) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get("runtime.return_validation_failed"), validationResult);
            return;
        }

        ReturnOrder returnOrder = buildReturnOrder(returnReason, resolveMemberId());
        List<ReturnOrderItem> items = buildReturnOrderItems(returnReason);
        returnOrder.totalAmount = calculateReturnTotal(items);

        // 保存退货订单
        boolean result = ReturnService.createReturnOrder(returnOrder, items);
        handleReturnOrderSaveResult(result, returnOrder);
    }

    private boolean hasSelectedReturnItem() {
        return returnItems.stream().anyMatch(item -> item.isSelected() && item.returnQuantity > 0);
    }

    private Integer resolveMemberId() {
        if (originalTransaction.memberPhone == null || originalTransaction.memberPhone.trim().isEmpty()) {
            return null;
        }
        try {
            Member member = DAOFactory.getInstance().getMemberDAO().findByPhone(originalTransaction.memberPhone);
            return member != null ? member.id : null;
        } catch (SQLException e) {
            logger.error("查询会员信息失败: {}", e.getMessage());
            return null;
        }
    }

    private ReturnOrder buildReturnOrder(String returnReason, Integer memberId) {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.originalTransactionId = originalTransaction.transactionId;
        returnOrder.memberId = memberId;  // 可以为null
        returnOrder.memberName = originalTransaction.memberName;
        returnOrder.returnReason = returnReason;
        returnOrder.paymentMethod = getPaymentMethodCode(refundMethodComboBox.getValue());
        returnOrder.operatorName = getOperatorName();
        returnOrder.notes = notesArea.getText().trim();
        return returnOrder;
    }

    private List<ReturnOrderItem> buildReturnOrderItems(String returnReason) {
        List<ReturnOrderItem> items = new ArrayList<>();
        for (ReturnItem item : returnItems) {
            if (item.isSelected() && item.returnQuantity > 0) {
                items.add(buildReturnOrderItem(item, returnReason));
            }
        }
        return items;
    }

    private ReturnOrderItem buildReturnOrderItem(ReturnItem item, String returnReason) {
        ReturnOrderItem returnItem = new ReturnOrderItem();
        returnItem.productId = item.productId;
        returnItem.productCode = item.productCode;
        returnItem.productName = item.productName;
        returnItem.returnQuantity = item.returnQuantity;
        returnItem.unitPrice = BigDecimal.valueOf(item.unitPrice);
        returnItem.returnAmount = BigDecimal.valueOf(item.getReturnAmount());
        returnItem.condition = getConditionCode(item.condition);
        returnItem.reason = returnReason;
        returnItem.calculateAmount();
        return returnItem;
    }

    private BigDecimal calculateReturnTotal(List<ReturnOrderItem> items) {
        return items.stream()
            .map(ReturnOrderItem::getReturnAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void handleReturnOrderSaveResult(boolean result, ReturnOrder returnOrder) {
        if (!result) {
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.FAILED), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_create_failed"));
            return;
        }

        submitted = true;
        showAlert(Alert.AlertType.INFORMATION, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.SUCCESS),
            com.cashier.i18n.I18nManager.getInstance().get("runtime.return_create_success",
                returnOrder.returnOrderId, String.format("%.2f", returnOrder.totalAmount)));
        logger.info("退货订单创建成功: {}", returnOrder.returnOrderId);

        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * 获取支付方式代码
     */
    private String getPaymentMethodCode(String text) {
        return text == null ? "CASH" : text;
    }

    /**
     * 获取商品状态代码
     */
    private String getConditionCode(String text) {
        return text == null ? "GOOD" : text;
    }

    /**
     * 获取操作员名称
     */
    private String getOperatorName() {
        return currentUser != null ? currentUser.name : "admin";
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * 设置对话框舞台
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * 是否已提交
     */
    public boolean isSubmitted() {
        return submitted;
    }

    /**
     * 显示提示
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        updateStatusForAlert(type, message);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateStatusForAlert(Alert.AlertType type, String message) {
        if (type == Alert.AlertType.ERROR) {
            StatusBarManager.updateError(message);
        } else if (type == Alert.AlertType.WARNING) {
            StatusBarManager.updateWarning(message);
        } else if (type == Alert.AlertType.INFORMATION) {
            StatusBarManager.updateSuccess(message);
        }
    }
}
