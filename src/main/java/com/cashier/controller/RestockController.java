package com.cashier.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.Product;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.fxml.FXML;

import java.math.BigDecimal;
import java.sql.SQLException;
import javafx.scene.control.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * 快速入库控制器
 * 处理商品快速入库对话框的逻辑
 * 已重构为使用重构版 DAO
 */
public class RestockController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(RestockController.class);

    @FXML
    private Label titleLabel;

    @FXML
    private Label productLabel;

    @FXML
    private Label currentStockLabel;

    @FXML
    private Label costPriceLabel;

    @FXML
    private TextField quantityField;

    @FXML
    private ComboBox<String> sourceComboBox;

    @FXML
    private TextField reasonField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label afterStockLabel;

    @FXML
    private Label totalCostLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button confirmButton;

    private javafx.stage.Stage dialogStage;
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

        // 初始化入库来源下拉框
        sourceComboBox.getItems().addAll(
            "采购入库",
            "退回入库",
            "调拨入库",
            "盘盈入库",
            "其他"
        );
        sourceComboBox.getSelectionModel().selectFirst();

        // 监听数量变化，实时更新预览
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePreview();
        });
    }

    /**
     * 更新预览信息
     */
    private void updatePreview() {
        if (product == null) {
            afterStockLabel.setText("-");
            totalCostLabel.setText("-");
            return;
        }

        try {
            String text = quantityField.getText().trim();
            if (!text.isEmpty()) {
                int quantity = Integer.parseInt(text);
                if (quantity > 0) {
                    // 入库后库存
                    int afterStock = product.quantity + quantity;
                    afterStockLabel.setText(String.format("%d %s", afterStock, product.unit));

                    // 入库金额
                    double totalCost = product.getCost().multiply(BigDecimal.valueOf(quantity)).doubleValue();
                    totalCostLabel.setText(CurrencyUtil.format(totalCost));
                } else {
                    afterStockLabel.setText(String.format("%d %s", product.quantity, product.unit));
                    totalCostLabel.setText(CurrencyUtil.format(0));
                }
            } else {
                afterStockLabel.setText(String.format("%d %s", product.quantity, product.unit));
                totalCostLabel.setText(CurrencyUtil.format(0));
            }
        } catch (NumberFormatException e) {
            afterStockLabel.setText("-");
            totalCostLabel.setText("-");
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
     * 设置要入库的商品
     * @param product 商品对象
     */
    public void setProduct(Product product) {
        this.product = product;

        if (product != null) {
            productLabel.setText(product.name);
            currentStockLabel.setText(String.format("当前库存: %d %s", product.quantity, product.unit));
            costPriceLabel.setText(String.format("成本价: ¥%.2f/%s", product.cost, product.unit));
            updatePreview();
        }
    }

    /**
     * 获取入库数量
     * @return 入库数量
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
    public void handleConfirm() {
        if (isInputValid()) {
            int quantity = Integer.parseInt(quantityField.getText().trim());

            // 更新库存
            product.quantity += quantity;

            // 保存到数据库
            try {
                if (productDAO.update(product)) {
                    okClicked = true;
                    dialogStage.close();
                } else {
                    errorLabel.setText("入库失败");
                }
            } catch (SQLException e) {
                logger.error("入库失败", e);
                errorLabel.setText("入库失败: " + e.getMessage());
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
        String errorMessage = "";

        // 验证入库数量
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) {
                errorMessage += "入库数量必须大于0！\n";
            }
            if (quantity > 100000) {
                errorMessage += "入库数量过大，请检查输入！\n";
            }
        } catch (NumberFormatException e) {
            errorMessage += "入库数量格式不正确！\n";
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
