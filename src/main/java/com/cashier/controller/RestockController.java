package com.cashier.controller;

import com.cashier.dao.ProductDAO;
import com.cashier.model.Product;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.fxml.FXML;

import java.sql.SQLException;
import javafx.scene.control.*;

import java.util.Map;

/**
 * 补货控制器
 * 处理商品补货对话框的逻辑
 */
public class RestockController {
    private static final Logger logger = LoggerFactory.getLogger(RestockController.class);

    @FXML
    private Label titleLabel;

    @FXML
    private Label productLabel;

    @FXML
    private Label currentStockLabel;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField reasonField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button confirmButton;

    private javafx.stage.Stage dialogStage;
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
    }

    /**
     * 设置对话框舞台
     * @param dialogStage 对话框舞台
     */
    public void setDialogStage(javafx.stage.Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 设置要补货的商品
     * @param product 商品对象
     */
    public void setProduct(Product product) {
        this.product = product;

        if (product != null) {
            productLabel.setText(product.name);
            currentStockLabel.setText(String.format("当前库存: %d %s", product.quantity, product.unit));
        }
    }

    /**
     * 获取补货数量
     * @return 补货数量
     */
    public int getRestockQuantity() {
        try {
            return Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 是否点击了确认按钮
     * @return 如果点击了确认返回true，否则返回false
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * 处理确认
     */
    @FXML
    private void handleConfirm() {
        if (isInputValid()) {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            
            // 更新库存
            product.quantity += quantity;
            
            // 保存到数据库
            try {
                if (ProductDAO.update(product)) {
                    okClicked = true;
                    dialogStage.close();
                } else {
                    errorLabel.setText("补货失败");
                }
            } catch (SQLException e) {
                System.err.println("补货失败: " + e.getMessage());
                logger.error("补货失败", e);
                errorLabel.setText("补货失败: " + e.getMessage());
            }
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

        // 验证补货数量
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) {
                errorMessage += "补货数量必须大于0！\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "补货数量格式不正确！\n";
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
