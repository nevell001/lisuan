package com.cashier.controller;

import com.cashier.dao.ProductDAO;
import com.cashier.model.DataManager;
import com.cashier.model.Product;
import com.cashier.util.StatusBarManager;

import java.sql.SQLException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

/**
 * 库存管理控制器
 * 处理商品库存的增删改查
 */
public class InventoryController {

    @FXML
    private TableView<Product> inventoryTable;

    @FXML
    private TableColumn<Product, String> nameColumn;

    @FXML
    private TableColumn<Product, String> priceColumn;

    @FXML
    private TableColumn<Product, String> quantityColumn;

    @FXML
    private TableColumn<Product, String> minStockColumn;

    @FXML
    private TableColumn<Product, String> categoryColumn;

    @FXML
    private TableColumn<Product, String> warningColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private Label countLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button restockButton;

    private ObservableList<Product> inventoryList;
    private Map<String, Product> inventory;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化排序列表
        sortComboBox.setItems(FXCollections.observableArrayList(
            "默认排序",
            "按名称",
            "按价格(低→高)",
            "按价格(高→低)",
            "按库存(多→少)",
            "按库存(少→多)"
        ));
        sortComboBox.getSelectionModel().select(0);
        sortComboBox.setOnAction(event -> sortInventory());

        // 设置表格列
        setupTableColumns();

        // 加载库存数据
        loadInventory();

        // 设置表格选择模式
        inventoryTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 添加表格选择监听
        inventoryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().price)));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        minStockColumn.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        warningColumn.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            if (p.quantity <= 0) {
                return new SimpleStringProperty("缺货");
            } else if (p.quantity < p.minStock) {
                return new SimpleStringProperty("库存不足");
            } else {
                return new SimpleStringProperty("正常");
            }
        });
    }

    /**
     * 加载库存数据
     */
    private void loadInventory() {
        try {
            // 尝试从数据库加载
            var products = ProductDAO.findAll();
            inventory = new java.util.HashMap<>();
            for (Product product : products) {
                inventory.put(product.name, product);
            }
        } catch (SQLException e) {
            System.err.println("从数据库加载商品失败: " + e.getMessage());
            e.printStackTrace();
            // 降级到文件存储
            inventory = DataManager.loadInventory();
        }
        inventoryList = FXCollections.observableArrayList(inventory.values());
        inventoryTable.setItems(inventoryList);
        updateCountLabel();
    }

    /**
     * 更新商品数量标签
     */
    private void updateCountLabel() {
        countLabel.setText("商品数量: " + inventoryList.size());
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = !inventoryTable.getSelectionModel().getSelectedItems().isEmpty();
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        restockButton.setDisable(!hasSelection);
    }

    /**
     * 处理添加商品
     */
    @FXML
    private void handleAddProduct() {
        try {
            // 加载商品编辑对话框
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/ProductEditView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            ProductEditController controller = loader.getController();

            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.setTitle("添加商品");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(inventoryTable.getScene().getWindow());
            dialogStage.setResizable(false);

            // 设置场景
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            dialogStage.setScene(scene);

            // 设置控制器引用
            controller.setDialogStage(dialogStage);
            controller.setProduct(null);

            // 显示对话框并等待响应
            dialogStage.showAndWait();

            // 如果用户点击了保存
            if (controller.isOkClicked()) {
                Product newProduct = controller.getProduct();
                try {
                    ProductDAO.insert(newProduct);
                    loadInventory();
                    updateStatus("商品添加成功: " + newProduct.name);
                } catch (SQLException e) {
                    System.err.println("数据库保存失败，降级到文件存储: " + e.getMessage());
                    // 降级到文件存储
                    inventory.put(newProduct.name, newProduct);
                    DataManager.saveInventory(inventory);
                    loadInventory();
                    updateStatus("商品添加成功: " + newProduct.name);
                }
            }

        } catch (IOException e) {
            showError("加载添加商品对话框失败: " + e.getMessage());
        }
    }

    /**
     * 处理编辑商品
     */
    @FXML
    private void handleEditProduct() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // 加载商品编辑对话框
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/com/cashier/view/ProductEditView.fxml"));
                VBox root = loader.load();

                // 获取控制器
                ProductEditController controller = loader.getController();

                // 创建对话框
                Stage dialogStage = new Stage();
                dialogStage.setTitle("编辑商品");
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(inventoryTable.getScene().getWindow());
                dialogStage.setResizable(false);

                // 设置场景
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

                dialogStage.setScene(scene);

                // 设置控制器引用
                controller.setDialogStage(dialogStage);
                controller.setProduct(selected);

                // 显示对话框并等待响应
                dialogStage.showAndWait();

                // 如果用户点击了保存
                if (controller.isOkClicked()) {
                    Product updatedProduct = controller.getProduct();
                    try {
                        ProductDAO.update(updatedProduct);
                        loadInventory();
                        updateStatus("商品更新成功: " + updatedProduct.name);
                    } catch (SQLException e) {
                        System.err.println("数据库更新失败，降级到文件存储: " + e.getMessage());
                        // 降级到文件存储
                        inventory.put(updatedProduct.name, updatedProduct);
                        DataManager.saveInventory(inventory);
                        loadInventory();
                        updateStatus("商品更新成功: " + updatedProduct.name);
                    }
                }

            } catch (IOException e) {
                showError("加载编辑商品对话框失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理删除商品
     */
    @FXML
    private void handleDeleteProduct() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除商品 \"" + selected.name + "\" 吗？");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    ProductDAO.delete(selected.name);
                    loadInventory();
                } catch (SQLException e) {
                    System.err.println("数据库删除失败，降级到文件存储: " + e.getMessage());
                    // 降级到文件存储
                    inventory.remove(selected.name);
                    DataManager.saveInventory(inventory);
                    loadInventory();
                }
            }
        }
    }

    /**
     * 处理补货
     */
    @FXML
    private void handleRestock() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // 加载补货对话框
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/com/cashier/view/RestockView.fxml"));
                VBox root = loader.load();

                // 获取控制器
                RestockController controller = loader.getController();

                // 创建对话框
                Stage dialogStage = new Stage();
                dialogStage.setTitle("补货");
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(inventoryTable.getScene().getWindow());
                dialogStage.setResizable(false);

                // 设置场景
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

                dialogStage.setScene(scene);

                // 设置控制器引用
                controller.setDialogStage(dialogStage);
                controller.setProduct(selected);

                // 显示对话框并等待响应
                dialogStage.showAndWait();

                // 如果用户点击了确认
                if (controller.isOkClicked()) {
                    loadInventory();
                    updateStatus("补货成功: " + selected.name + " (+" + controller.getRestockQuantity() + ")");
                }

            } catch (IOException e) {
                showError("加载补货对话框失败: " + e.getMessage());
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
            inventoryList.setAll(inventory.values());
        } else {
            inventoryList.setAll(inventory.values().stream()
                .filter(p -> p.name.toLowerCase().contains(searchText))
                .toList());
        }
        updateCountLabel();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    private void handleClearSearch() {
        searchField.clear();
        inventoryList.setAll(inventory.values());
        updateCountLabel();
    }

    /**
     * 排序库存
     */
    private void sortInventory() {
        String selected = sortComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        switch (selected) {
            case "按名称":
                inventoryList.sort((p1, p2) -> p1.name.compareTo(p2.name));
                break;
            case "按价格(低→高)":
                inventoryList.sort((p1, p2) -> Double.compare(p1.price, p2.price));
                break;
            case "按价格(高→低)":
                inventoryList.sort((p1, p2) -> Double.compare(p2.price, p1.price));
                break;
            case "按库存(多→少)":
                inventoryList.sort((p1, p2) -> Integer.compare(p2.quantity, p1.quantity));
                break;
            case "按库存(少→多)":
                inventoryList.sort((p1, p2) -> Integer.compare(p1.quantity, p2.quantity));
                break;
            default:
                inventoryList.setAll(inventory.values());
        }
    }

    /**
     * 刷新库存
     */
    public void refreshInventory() {
        loadInventory();
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