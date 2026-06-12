package com.cashier.controller;

import com.cashier.dao.CategoryDAO;
import com.cashier.i18n.I18nManager;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.dao.SupplierDAO;
import com.cashier.dao.UnitDAO;
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
import java.util.Map;
import java.util.HashMap;

/**
 * 商品编辑控制器
 * 处理商品添加和编辑对话框的逻辑
 * 已重构为使用重构版 DAO
 */
public class ProductEditController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ProductEditController.class);

    @FXML
    private Label titleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField quantityField;

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
    private Map<String, Product> inventoryMap;
    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 加载库存数据
        try {
            List<Product> products = productDAO.findAll();
            inventoryMap = new HashMap<>();
            for (Product p : products) {
                inventoryMap.put(p.name, p);
            }
        } catch (SQLException e) {
            logger.error("加载商品数据失败", e);
            inventoryMap = new HashMap<>();
        }

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

        // 强制设置 ComboBox 样式，去除所有内部边框
        styleComboBox(categoryComboBox);
        styleComboBox(unitComboBox);
        styleComboBox(supplierComboBox);

        // 强制设置 TextArea 样式，去除所有内部边框
        styleTextArea(descriptionField);
    }

    /**
     * 强制设置 ComboBox 样式，去除所有内部边框
     */
    private void styleComboBox(javafx.scene.control.ComboBox<String> comboBox) {
        // 使用 CSS 强制覆盖所有内部组件样式
        comboBox.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-insets: 0; " +
            "-fx-background-radius: 4px; " +
            "-fx-border-color: #E0E0E0; " +
            "-fx-border-insets: 0; " +
            "-fx-border-radius: 4px; " +
            "-fx-border-width: 1px; " +
            "-fx-padding: 6px 8px; " +
            "-fx-font-size: 14px;"
        );

        // 监听显示变化，确保样式持续应用
        comboBox.showingProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                comboBox.setStyle(comboBox.getStyle());
            }
        });
    }

    /**
     * 强制设置 TextArea 样式，去除所有内部边框
     */
    private void styleTextArea(javafx.scene.control.TextArea textArea) {
        // 使用 CSS 强制覆盖所有内部组件样式
        textArea.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-insets: 0; " +
            "-fx-background-radius: 4px; " +
            "-fx-border-color: #E0E0E0; " +
            "-fx-border-insets: 0; " +
            "-fx-border-radius: 4px; " +
            "-fx-border-width: 1px; " +
            "-fx-padding: 8px 12px; " +
            "-fx-font-size: 13px; " +
            "-fx-text-fill: #424242;"
        );
    }

    /**
     * 加载分类数据
     */
    private void loadCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("默认分类"); // 添加默认分类

        try {
            List<Category> categoryList = CategoryDAO.findAll();
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
            List<Unit> unitList = UnitDAO.findAll();
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
            List<Supplier> supplierList = SupplierDAO.findAll();
            for (Supplier supplier : supplierList) {
                if (supplier.status) { // 只加载启用的供应商
                    suppliers.add(supplier.name);
                }
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
            titleLabel.setText("编辑商品");
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
            titleLabel.setText("添加商品");
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

        // 查询当天生成的商品数量
        String prefix = "P" + dateStr;
        int count = 0;
        try {
            List<Product> allProducts = productDAO.findAll();
            for (Product p : allProducts) {
                if (p.productCode != null && p.productCode.startsWith(prefix)) {
                    count++;
                }
            }
        } catch (SQLException e) {
            logger.error("查询商品数量失败", e);
        }

        // 生成4位序号，从0001开始
        String sequence = String.format("%04d", count + 1);
        return prefix + sequence;
    }

    /**
     * 处理保存
     */
    @FXML
    public void handleSave() {
        logger.info("开始处理保存操作，product是否为null: {}", (product == null));
        
        if (isInputValid()) {
            logger.info("输入验证通过");
            try {
                        if (product == null) {
                            // 添加新商品
                            product = new Product(
                                nameField.getText().trim(),
                                FormValidator.parseDouble(priceField.getText().trim()),
                                0  // 库存数量默认为0，通过进销存管理
                            );
            
                            // 自动生成商品编号
                            if (autoCodeCheckBox.isSelected()) {
                                product.productCode = generateProductCode();
                            } else {
                                product.productCode = productCodeField.getText().trim();
                            }
            
                            // 更新商品信息
                            product.minStock = FormValidator.parseInt(minStockField.getText().trim());
                            product.category = categoryComboBox.getSelectionModel().getSelectedItem();
                            if (product.category == null || product.category.trim().isEmpty()) {
                                product.category = "默认分类";
                            }
                            product.barcode = barcodeField.getText() != null ? barcodeField.getText().trim() : "";
                            product.unit = unitComboBox.getSelectionModel().getSelectedItem();
                            if (product.unit == null || product.unit.trim().isEmpty()) {
                                product.unit = "个";
                            }
                            product.description = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
                            product.brand = brandField.getText() != null ? brandField.getText().trim() : "";
                            product.supplier = supplierComboBox.getSelectionModel().getSelectedItem();
                            product.spec = specField.getText() != null ? specField.getText().trim() : "";
                            product.cost = costField.getText() != null && !costField.getText().trim().isEmpty()
                                ? new BigDecimal(costField.getText().trim())
                                : product.getPrice().multiply(new BigDecimal("0.7"));
            
                            // 检查商品名称是否已存在
                            Product existingProduct = productDAO.findByName(product.name);
                            if (existingProduct != null) {
                                errorLabel.setText("商品名称已存在，请使用其他名称");
                                logger.warn("商品名称已存在: {}", product.name);
                                return;
                            }
                            
                            // 插入数据库
                            boolean success = productDAO.insert(product);
                            if (!success) {
                                errorLabel.setText("添加商品失败，请重试");
                                return;
                            }

                            // 添加成功，显示提示
                            StatusBarManager.updateStatus("商品添加成功: " + product.name);
                            logger.info("商品添加成功: {} ({})", product.name, product.productCode);

                            // 显示成功提示
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            alert.setTitle(I18nManager.getInstance().get("label.success"));
                            alert.setHeaderText(null);
                            alert.setContentText("商品添加成功！");
                            alert.showAndWait();

                        } else {
                            // 编辑现有商品（不修改库存数量）
                            logger.info("编辑现有商品，商品ID: {}, 原名称: {}", product.id, product.name);
                            
                            product.name = nameField.getText().trim();
                            product.price = new BigDecimal(priceField.getText().trim());
            
                            // 更新商品编号（如果不是自动生成）
                            if (!autoCodeCheckBox.isSelected()) {
                                product.productCode = productCodeField.getText().trim();
                            }
                            
                            // 检查商品名称是否已存在（排除当前商品）
                            Product existingProduct = productDAO.findByName(product.name);
                            if (existingProduct != null && existingProduct.id != product.id) {
                                errorLabel.setText("商品名称已存在，请使用其他名称");
                                logger.warn("商品名称已存在: {}", product.name);
                                return;
                            }
            
                            // 更新商品信息
                            product.minStock = FormValidator.parseInt(minStockField.getText().trim());
                            product.category = categoryComboBox.getSelectionModel().getSelectedItem();
                            if (product.category == null || product.category.trim().isEmpty()) {
                                product.category = "默认分类";
                            }
                            product.barcode = barcodeField.getText() != null ? barcodeField.getText().trim() : "";
                            product.unit = unitComboBox.getSelectionModel().getSelectedItem();
                            if (product.unit == null || product.unit.trim().isEmpty()) {
                                product.unit = "个";
                            }
                            product.description = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
                            product.brand = brandField.getText() != null ? brandField.getText().trim() : "";
                            product.supplier = supplierComboBox.getSelectionModel().getSelectedItem();
                            product.spec = specField.getText() != null ? specField.getText().trim() : "";
                            product.cost = costField.getText() != null && !costField.getText().trim().isEmpty()
                                ? new BigDecimal(costField.getText().trim())
                                : product.getPrice().multiply(new BigDecimal("0.7"));
                            logger.info("准备更新商品到数据库: id={}, name={}, price={}", product.id, product.name, product.price);
                            
                            // 更新数据库
                            boolean success = productDAO.update(product);
                            logger.info("数据库更新结果: {}", success);
                            
                            if (!success) {
                                errorLabel.setText("更新商品失败，请重试");
                                logger.error("更新商品失败");
                                return;
                            }

                            // 更新成功，显示提示
                            StatusBarManager.updateStatus("商品更新成功: " + product.name);
                            logger.info("商品更新成功: {} ({})", product.name, product.productCode);

                            // 显示成功提示
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            alert.setTitle(I18nManager.getInstance().get("label.success"));
                            alert.setHeaderText(null);
                            alert.setContentText("商品更新成功！");
                            alert.showAndWait();
                        }

                // 操作成功，关闭对话框
                okClicked = true;
                dialogStage.close();

            } catch (SQLException e) {
                logger.error("保存商品失败", e);
                errorLabel.setText("保存商品失败: " + e.getMessage());
            }
        }
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

        String errorMessage = "";

        // 验证商品编号（仅当手动输入时才验证）
        if (!autoCodeCheckBox.isSelected()) {
            if (productCodeField.getText().trim().isEmpty()) {
                errorMessage += "商品编号不能为空！\n";
            } else {
                // 验证商品编号是否已存在
                try {
                    Product existingProduct = productDAO.findByProductCode(productCodeField.getText().trim());
                    if (existingProduct != null && (product == null || existingProduct.id != product.id)) {
                        errorMessage += "商品编号已存在，请使用其他编号！\n";
                    }
                } catch (SQLException e) {
                    logger.error("验证商品编号失败", e);
                    errorMessage += "验证商品编号失败，请重试！\n";
                }
            }
        }

        // 验证商品名称
        if (nameField.getText().trim().isEmpty()) {
            errorMessage += "商品名称不能为空！\n";
        } else if (product == null && inventoryMap.containsKey(nameField.getText().trim())) {
            errorMessage += "商品名称已存在！\n";
        }

        // 验证单价
        try {
            double price = FormValidator.parseDouble(priceField.getText().trim());
            if (price <= 0) {
                errorMessage += "单价必须大于0！\n";
            }
        } catch (IllegalArgumentException e) {
            errorMessage += "单价格式不正确！\n";
        }

        // 验证最低库存
        try {
            int minStock = FormValidator.parseInt(minStockField.getText().trim());
            if (minStock < 0) {
                errorMessage += "最低库存不能为负数！\n";
            }
        } catch (IllegalArgumentException e) {
            errorMessage += "最低库存格式不正确！\n";
        }

        // 验证成本价
        if (!costField.getText().trim().isEmpty()) {
            try {
                double cost = FormValidator.parseDouble(costField.getText().trim());
                if (cost < 0) {
                    errorMessage += "成本价不能为负数！\n";
                }
            } catch (IllegalArgumentException e) {
                errorMessage += "成本价格式不正确！\n";
            }
        }

        if (errorMessage.isEmpty()) {
            errorLabel.setText("");
            logger.info("输入验证通过");
            return true;
        } else {
            errorLabel.setText(errorMessage);
            logger.info("输入验证失败: {}", errorMessage);
            return false;
        }
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