package com.cashier.controller;

import com.cashier.dao.CategoryDAO;
import com.cashier.dao.ProductDAO;
import com.cashier.dao.UnitDAO;
import com.cashier.model.Category;
import com.cashier.model.Product;
import com.cashier.model.Unit;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.fxml.FXML;

import java.sql.SQLException;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 商品编辑控制器
 * 处理商品添加和编辑对话框的逻辑
 */
public class ProductEditController {
    private static final Logger logger = LoggerFactory.getLogger(ProductEditController.class);

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
    private TextField supplierField;

    @FXML
    private TextField specField;

    @FXML
    private TextField costField;

    @FXML
    private TextField productCodeField;

    @FXML
    private TextField idField;

    @FXML
    private CheckBox autoIdCheckBox;

    @FXML
    private Label errorLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button saveButton;

    private Stage dialogStage;
    private Product product;
    private boolean okClicked = false;
    private Map<String, Product> inventory;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 加载库存数据
        try {
            var products = ProductDAO.findAll();
            inventory = new java.util.HashMap<>();
            for (Product p : products) {
                inventory.put(p.name, p);
            }
        } catch (SQLException e) {
            System.err.println("加载商品数据失败: " + e.getMessage());
            logger.error("加载商品数据失败", e);
            inventory = new java.util.HashMap<>();
        }

        // 加载分类数据
        loadCategories();

        // 加载单位数据
        loadUnits();

        // 设置默认值
        minStockField.setText("10");
        categoryComboBox.getSelectionModel().select("默认分类");
        unitComboBox.getSelectionModel().select("个");

        // 自动ID复选框默认选中
        autoIdCheckBox.setSelected(true);
        idField.setDisable(true);

        // 监听自动ID复选框变化
        autoIdCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            idField.setDisable(newVal);
            if (newVal) {
                idField.clear();
            }
        });

        // 强制设置 ComboBox 样式，去除所有内部边框
        styleComboBox(categoryComboBox);
        styleComboBox(unitComboBox);

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
            System.err.println("加载分类数据失败: " + e.getMessage());
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
            System.err.println("加载单位数据失败: " + e.getMessage());
        }

        unitComboBox.setItems(javafx.collections.FXCollections.observableArrayList(units));
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
            idField.setText(String.valueOf(product.id));
            autoIdCheckBox.setSelected(false);
            idField.setDisable(false);
            productCodeField.setText(product.productCode);
            nameField.setText(product.name);
            priceField.setText(String.format("%.2f", product.price));
            quantityField.setText(String.valueOf(product.quantity));
            minStockField.setText(String.valueOf(product.minStock));
            categoryComboBox.getSelectionModel().select(product.category);
            barcodeField.setText(product.barcode);
            unitComboBox.getSelectionModel().select(product.unit);
            descriptionField.setText(product.description);
            brandField.setText(product.brand);
            supplierField.setText(product.supplier);
            specField.setText(product.spec);
            costField.setText(String.format("%.2f", product.cost));
        } else {
            // 添加模式
            titleLabel.setText("添加商品");
            autoIdCheckBox.setSelected(true);
            idField.setDisable(true);
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
     * 处理保存
     */
    @FXML
    private void handleSave() {
        if (isInputValid()) {
            if (product == null) {
                // 添加新商品
                product = new Product(
                    nameField.getText().trim(),
                    Double.parseDouble(priceField.getText().trim()),
                    Integer.parseInt(quantityField.getText().trim())
                );
                // 设置ID（如果不是自动生成）
                if (!autoIdCheckBox.isSelected()) {
                    try {
                        product.id = Integer.parseInt(idField.getText().trim());
                    } catch (NumberFormatException e) {
                        errorLabel.setText("ID必须是数字");
                        return;
                    }
                }
            } else {
                // 编辑现有商品
                product.name = nameField.getText().trim();
                product.price = Double.parseDouble(priceField.getText().trim());
                product.quantity = Integer.parseInt(quantityField.getText().trim());
            }

            // 更新商品信息
            product.productCode = productCodeField.getText().trim();
            product.minStock = Integer.parseInt(minStockField.getText().trim());
            product.category = categoryComboBox.getSelectionModel().getSelectedItem();
            if (product.category == null || product.category.trim().isEmpty()) {
                product.category = "默认分类";
            }
            product.barcode = barcodeField.getText().trim();
            product.unit = unitComboBox.getSelectionModel().getSelectedItem();
            if (product.unit == null || product.unit.trim().isEmpty()) {
                product.unit = "个";
            }
            product.description = descriptionField.getText().trim();
            product.brand = brandField.getText().trim();
            product.supplier = supplierField.getText().trim();
            product.spec = specField.getText().trim();
            product.cost = costField.getText().trim().isEmpty() ? product.price * 0.7 : Double.parseDouble(costField.getText().trim());

            okClicked = true;
            dialogStage.close();
        }
    }

    /**
     * 处理取消
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * 验证输入
     * @return 如果输入有效返回true，否则返回false
     */
    private boolean isInputValid() {
        String errorMessage = "";

        // 验证ID（如果不是自动生成）
        if (!autoIdCheckBox.isSelected() && !idField.getText().trim().isEmpty()) {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                if (id <= 0) {
                    errorMessage += "ID必须大于0！\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "ID格式不正确！\n";
            }
        }

        // 验证商品名称
        if (nameField.getText().trim().isEmpty()) {
            errorMessage += "商品名称不能为空！\n";
        } else if (product == null && inventory.containsKey(nameField.getText().trim())) {
            errorMessage += "商品名称已存在！\n";
        }

        // 验证单价
        try {
            double price = Double.parseDouble(priceField.getText().trim());
            if (price <= 0) {
                errorMessage += "单价必须大于0！\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "单价格式不正确！\n";
        }

        // 验证库存数量
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity < 0) {
                errorMessage += "库存数量不能为负数！\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "库存数量格式不正确！\n";
        }

        // 验证最低库存
        try {
            int minStock = Integer.parseInt(minStockField.getText().trim());
            if (minStock < 0) {
                errorMessage += "最低库存不能为负数！\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "最低库存格式不正确！\n";
        }

        // 验证成本价
        if (!costField.getText().trim().isEmpty()) {
            try {
                double cost = Double.parseDouble(costField.getText().trim());
                if (cost < 0) {
                    errorMessage += "成本价不能为负数！\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "成本价格式不正确！\n";
            }
        }

        if (errorMessage.isEmpty()) {
            errorLabel.setText("");
            return true;
        } else {
            errorLabel.setText(errorMessage);
            return false;
        }
    }
}