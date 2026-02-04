package com.cashier.controller;

import com.cashier.dao.ProductDAO;
import com.cashier.model.Product;
import com.cashier.util.StatusBarManager;
import javafx.fxml.FXML;

import java.sql.SQLException;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Map;

/**
 * 商品编辑控制器
 * 处理商品添加和编辑对话框的逻辑
 */
public class ProductEditController {

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
    private TextField categoryField;

    @FXML
    private TextField barcodeField;

    @FXML
    private TextField unitField;

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
            e.printStackTrace();
            inventory = new java.util.HashMap<>();
        }

        // 设置默认值
        minStockField.setText("10");
        categoryField.setText("默认分类");
        unitField.setText("个");
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
            nameField.setText(product.name);
            priceField.setText(String.format("%.2f", product.price));
            quantityField.setText(String.valueOf(product.quantity));
            minStockField.setText(String.valueOf(product.minStock));
            categoryField.setText(product.category);
            barcodeField.setText(product.barcode);
            unitField.setText(product.unit);
            descriptionField.setText(product.description);
            brandField.setText(product.brand);
            supplierField.setText(product.supplier);
            specField.setText(product.spec);
            costField.setText(String.format("%.2f", product.cost));
        } else {
            // 添加模式
            titleLabel.setText("添加商品");
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
            } else {
                // 编辑现有商品
                product.name = nameField.getText().trim();
                product.price = Double.parseDouble(priceField.getText().trim());
                product.quantity = Integer.parseInt(quantityField.getText().trim());
            }

            // 更新商品信息
            product.minStock = Integer.parseInt(minStockField.getText().trim());
            product.category = categoryField.getText().trim().isEmpty() ? "默认分类" : categoryField.getText().trim();
            product.barcode = barcodeField.getText().trim();
            product.unit = unitField.getText().trim().isEmpty() ? "个" : unitField.getText().trim();
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