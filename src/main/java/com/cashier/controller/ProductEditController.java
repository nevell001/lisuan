package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.Category;
import com.cashier.model.Product;
import com.cashier.model.Supplier;
import com.cashier.model.Unit;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;
import javafx.fxml.FXML;

import java.math.BigDecimal;
import java.sql.SQLException;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品编辑控制器
 * 处理商品添加和编辑对话框的逻辑
 * 已重构为使用重构版 DAO
 */
public class ProductEditController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ProductEditController.class);
    private static final int PRODUCT_SUPPLIER_LIMIT = 500;
    /** 自动生成商品编号撞号时的最大重试次数 */
    private static final int PRODUCT_CODE_RETRY_LIMIT = 3;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField minStockField;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextField barcodeField;

    @FXML
    private ComboBox<String> unitComboBox;

    @FXML
    private TextArea descriptionField;

    @FXML
    private TextField brandField;

    @FXML
    private TextField specField;

    @FXML
    private TextField costField;

    @FXML
    private TextField productCodeField;

    @FXML
    private ComboBox<String> supplierComboBox;

    @FXML
    private CheckBox autoCodeCheckBox;

    @FXML
    private Label errorLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;

    private Stage dialogStage;
    private Product product;
    private boolean okClicked = false;
    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.textProperty().addListener((obs, oldText, newText) -> {
            boolean hasError = newText != null && !newText.isBlank();
            errorLabel.setVisible(hasError);
            errorLabel.setManaged(hasError);
        });

        // 加载分类数据
        loadCategories();

        // 加载单位数据
        loadUnits();

        // 加载供应商数据
        loadSuppliers();

        // 设置默认值
        minStockField.setText("10");
        categoryComboBox.getSelectionModel().select("默认分类");
        unitComboBox.getSelectionModel().select("个");

        // 自动编号复选框默认选中
        autoCodeCheckBox.setSelected(true);
        productCodeField.setDisable(true);

        // 监听自动编号复选框变化
        autoCodeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            productCodeField.setDisable(newVal);
            if (newVal) {
                productCodeField.clear();
            }
        });

    }

    /**
     * 加载分类数据
     */
    private void loadCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("默认分类"); // 添加默认分类

        try {
            List<Category> categoryList = DAOFactory.getInstance().getCategoryDAO().findAll();
            for (Category category : categoryList) {
                categories.add(category.name);
            }
        } catch (SQLException e) {
            logger.error("加载分类数据失败", e);
        }

        categoryComboBox.setItems(javafx.collections.FXCollections.observableArrayList(categories));
    }

    /**
     * 加载单位数据
     */
    private void loadUnits() {
        List<String> units = new ArrayList<>();
        units.add("个"); // 添加默认单位

        try {
            List<Unit> unitList = DAOFactory.getInstance().getUnitDAO().findAll();
            for (Unit unit : unitList) {
                units.add(unit.name);
            }
        } catch (SQLException e) {
            logger.error("加载单位数据失败", e);
        }

        unitComboBox.setItems(javafx.collections.FXCollections.observableArrayList(units));
    }

    /**
     * 加载供应商数据
     */
    private void loadSuppliers() {
        List<String> suppliers = new ArrayList<>();
        // 不添加默认供应商，要求必须选择

        try {
            List<Supplier> supplierList = DAOFactory.getInstance().getSupplierDAO().findByStatus(true, PRODUCT_SUPPLIER_LIMIT);
            for (Supplier supplier : supplierList) {
                suppliers.add(supplier.name);
            }
        } catch (SQLException e) {
            logger.error("加载供应商数据失败", e);
        }

        supplierComboBox.setItems(javafx.collections.FXCollections.observableArrayList(suppliers));
    }

    /**
     * 设置对话框舞台
     * @param dialogStage 对话框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 设置要编辑的商品
     * @param product 商品对象
     */
    public void setProduct(Product product) {
        this.product = product;

        if (product != null) {
            // 编辑模式
            titleLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ProductEdit.EDIT));
            productCodeField.setText(product.productCode);
            autoCodeCheckBox.setSelected(false);
            productCodeField.setDisable(false);
            nameField.setText(product.name);
            priceField.setText(String.format("%.2f", product.price));
            minStockField.setText(String.valueOf(product.minStock));
            categoryComboBox.getSelectionModel().select(product.category);
            barcodeField.setText(product.barcode);
            unitComboBox.getSelectionModel().select(product.unit);
            descriptionField.setText(product.description);
            brandField.setText(product.brand);
            supplierComboBox.getSelectionModel().select(product.supplier);
            specField.setText(product.spec);
            costField.setText(String.format("%.2f", product.cost));
        } else {
            // 添加模式
            titleLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("product.edit.title"));
            autoCodeCheckBox.setSelected(true);
            productCodeField.setDisable(true);
        }
    }

    /**
     * 获取编辑后的商品
     * @return 商品对象
     */
    public Product getProduct() {
        return product;
    }

    /**
     * 是否点击了确定按钮
     * @return 如果点击了确定返回true，否则返回false
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * 生成自动商品编号
     * 格式：P + 年月日 + 4位序号（如：P202602130001）
     * @return 自动生成的商品编号
     */
    private String generateProductCode() {
        // 获取当前日期字符串
        String dateStr = java.time.LocalDate.now().toString().replace("-", ""); // 如：20260213

        // 取当天已用的最大序号（不能用"当天数量 +1"：删过商品后数量会回退，会重新生成已存在的编号）
        String prefix = "P" + dateStr;
        long maxSequence = 0;
        try {
            maxSequence = productDAO.findMaxProductCodeSequence(prefix);
        } catch (SQLException e) {
            logger.error("查询商品编号序号失败", e);
        }

        // 生成4位序号，从0001开始
        String sequence = String.format("%04d", maxSequence + 1);
        return prefix + sequence;
    }

    /**
     * 处理保存
     */
    @FXML
    public void handleSave() {
        logger.info("开始处理保存操作，product是否为null: {}", (product == null));
        if (!isInputValid()) {
            return;
        }

        logger.info("输入验证通过");
        try {
            boolean saved = product == null ? createProduct() : updateProduct();
            if (saved) {
                okClicked = true;
                dialogStage.close();
            }
        } catch (SQLException e) {
            logger.error("保存商品失败", e);
            errorLabel.setText(I18nManager.getInstance().get("runtime.product_save_failed", e.getMessage()));
        }
    }

    private boolean createProduct() throws SQLException {
        product = new Product(
            nameField.getText().trim(),
            FormValidator.parseDouble(priceField.getText().trim()),
            0
        );
        product.productCode = resolveProductCodeForNewProduct();
        applyFormValues(product);

        if (isDuplicateProductName(product.name, product.id)) {
            showDuplicateProductNameError(product.name);
            return false;
        }

        // 自动编号在两端同时建品时可能撞号（各自取到同一序号），撞了就重新取号重试；
        // 非编号冲突的错误在重试用尽后照常抛出。
        boolean autoCode = autoCodeCheckBox.isSelected();
        for (int attempt = 1; ; attempt++) {
            try {
                if (!productDAO.insert(product)) {
                    errorLabel.setText(I18nManager.getInstance().get("runtime.product_add_retry"));
                    return false;
                }
                break;
            } catch (SQLException e) {
                if (!autoCode || attempt >= PRODUCT_CODE_RETRY_LIMIT) {
                    throw e;
                }
                logger.warn("商品保存失败，重新生成编号后重试（第 {} 次）: {}", attempt, product.productCode);
                product.productCode = generateProductCode();
            }
        }

        StatusBarManager.updateSuccess("商品添加成功: " + product.name);
        showSuccessAlert("runtime.product_added");
        logger.info("商品添加成功: {} ({})", product.name, product.productCode);
        return true;
    }

    private boolean updateProduct() throws SQLException {
        logger.info("编辑现有商品，商品ID: {}, 原名称: {}", product.id, product.name);

        product.name = nameField.getText().trim();
        product.price = new BigDecimal(priceField.getText().trim());
        if (!autoCodeCheckBox.isSelected()) {
            product.productCode = productCodeField.getText().trim();
        }
        applyFormValues(product);

        if (isDuplicateProductName(product.name, product.id)) {
            showDuplicateProductNameError(product.name);
            return false;
        }

        logger.info("准备更新商品到数据库: id={}, name={}, price={}", product.id, product.name, product.price);
        boolean success = productDAO.update(product);
        logger.info("数据库更新结果: {}", success);

        if (!success) {
            errorLabel.setText(I18nManager.getInstance().get("runtime.product_update_retry"));
            logger.error("更新商品失败");
            return false;
        }

        StatusBarManager.updateSuccess("商品更新成功: " + product.name);
        showSuccessAlert("runtime.product_updated");
        logger.info("商品更新成功: {} ({})", product.name, product.productCode);
        return true;
    }

    private void applyFormValues(Product target) {
        target.minStock = FormValidator.parseInt(minStockField.getText().trim());
        target.category = selectedOrDefault(categoryComboBox, "默认分类");
        target.barcode = trimText(barcodeField);
        target.unit = selectedOrDefault(unitComboBox, "个");
        target.description = trimText(descriptionField);
        target.brand = trimText(brandField);
        target.supplier = supplierComboBox.getSelectionModel().getSelectedItem();
        target.spec = trimText(specField);
        target.cost = parseCostOrDefault(target);
    }

    private String resolveProductCodeForNewProduct() {
        return autoCodeCheckBox.isSelected()
            ? generateProductCode()
            : productCodeField.getText().trim();
    }

    private BigDecimal parseCostOrDefault(Product target) {
        String costText = costField.getText();
        if (costText != null && !costText.trim().isEmpty()) {
            return new BigDecimal(costText.trim());
        }
        return target.getPrice().multiply(Product.DEFAULT_COST_RATE);
    }

    private boolean isDuplicateProductName(String productName, int currentProductId) throws SQLException {
        Product existingProduct = productDAO.findByName(productName);
        return existingProduct != null && existingProduct.id != currentProductId;
    }

    private void showDuplicateProductNameError(String productName) {
        errorLabel.setText(I18nManager.getInstance().get("runtime.product_name_duplicate"));
        logger.warn("商品名称已存在: {}", productName);
    }

    private void showSuccessAlert(String dialogMessageKey) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Label.SUCCESS));
        alert.setHeaderText(null);
        alert.setContentText(I18nManager.getInstance().get(dialogMessageKey));
        alert.showAndWait();
    }

    private String selectedOrDefault(ComboBox<String> comboBox, String defaultValue) {
        String selected = comboBox.getSelectionModel().getSelectedItem();
        return selected == null || selected.trim().isEmpty() ? defaultValue : selected;
    }

    private String trimText(TextInputControl input) {
        return input.getText() == null ? "" : input.getText().trim();
    }

    /**
     * 处理取消
     */
    @FXML
    public void handleCancel() {
        dialogStage.close();
    }

    /**
     * 验证输入
     * @return 如果输入有效返回true，否则返回false
     */
    private boolean isInputValid() {
        logger.info("开始验证输入，product是否为null: {}", (product == null));

        StringBuilder errorMessage = new StringBuilder();

        validateProductCode(errorMessage);
        validateProductName(errorMessage);
        validatePositivePrice(errorMessage);
        validateMinStock(errorMessage);
        validateCost(errorMessage);

        if (errorMessage.isEmpty()) {
            errorLabel.setText("");
            logger.info("输入验证通过");
            return true;
        }

        errorLabel.setText(errorMessage.toString());
        logger.info("输入验证失败: {}", errorMessage);
        return false;
    }

    private void validateProductCode(StringBuilder errorMessage) {
        if (autoCodeCheckBox.isSelected()) {
            return;
        }

        if (productCodeField.getText().trim().isEmpty()) {
            appendValidationError(errorMessage, "product.validation.code_required");
            return;
        }

        try {
            Product existingProduct = productDAO.findByProductCode(productCodeField.getText().trim());
            if (existingProduct != null && (product == null || existingProduct.id != product.id)) {
                appendValidationError(errorMessage, "product.validation.code_duplicate");
            }
        } catch (SQLException e) {
            logger.error("验证商品编号失败", e);
            appendValidationError(errorMessage, "product.validation.code_check_failed");
        }
    }

    private void validateProductName(StringBuilder errorMessage) {
        if (nameField.getText().trim().isEmpty()) {
            appendValidationError(errorMessage, "product.validation.name_required");
        }
    }

    private void validatePositivePrice(StringBuilder errorMessage) {
        try {
            double price = FormValidator.parseDouble(priceField.getText().trim());
            if (price <= 0) {
                appendValidationError(errorMessage, "product.validation.price_positive");
            }
        } catch (IllegalArgumentException e) {
            appendValidationError(errorMessage, "product.validation.price_invalid");
        }
    }

    private void validateMinStock(StringBuilder errorMessage) {
        try {
            int minStock = FormValidator.parseInt(minStockField.getText().trim());
            if (minStock < 0) {
                appendValidationError(errorMessage, "product.validation.min_stock_nonnegative");
            }
        } catch (IllegalArgumentException e) {
            appendValidationError(errorMessage, "product.validation.min_stock_invalid");
        }
    }

    private void validateCost(StringBuilder errorMessage) {
        if (!costField.getText().trim().isEmpty()) {
            try {
                double cost = FormValidator.parseDouble(costField.getText().trim());
                if (cost < 0) {
                    appendValidationError(errorMessage, "product.validation.cost_nonnegative");
                }
            } catch (IllegalArgumentException e) {
                appendValidationError(errorMessage, "product.validation.cost_invalid");
            }
        }
    }

    private void appendValidationError(StringBuilder errorMessage, String messageKey) {
        errorMessage.append(I18nManager.getInstance().get(messageKey)).append('\n');
    }

    /**
     * 填充商品信息
     */
    private void fillProductInfo(Product product) {
        nameField.setText(product.name);
        priceField.setText(String.valueOf(product.price));
        barcodeField.setText(product.barcode);
        specField.setText(product.spec != null ? product.spec : "");
        brandField.setText(product.brand != null ? product.brand : "");
        descriptionField.setText(product.description != null ? product.description : "");
        costField.setText(product.getCost().compareTo(BigDecimal.ZERO) > 0 ? String.valueOf(product.getCost()) : "");
        
        if (product.category != null && !product.category.isEmpty()) {
            categoryComboBox.getSelectionModel().select(product.category);
        }
        
        if (product.unit != null && !product.unit.isEmpty()) {
            unitComboBox.getSelectionModel().select(product.unit);
        }
    }
}
