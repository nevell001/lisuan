package com.cashier.controller;

import com.cashier.model.DataManager;
import com.cashier.model.Product;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

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
        inventory = DataManager.loadInventory();
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
        // TODO: 实现添加商品对话框
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("添加商品");
        alert.setHeaderText(null);
        alert.setContentText("添加商品功能正在开发中...");
        alert.showAndWait();
    }

    /**
     * 处理编辑商品
     */
    @FXML
    private void handleEditProduct() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // TODO: 实现编辑商品对话框
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("编辑商品");
            alert.setHeaderText(null);
            alert.setContentText("编辑商品功能正在开发中...\n商品: " + selected.name);
            alert.showAndWait();
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
                inventory.remove(selected.name);
                DataManager.saveInventory(inventory);
                loadInventory();
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
            // TODO: 实现补货对话框
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("补货");
            alert.setHeaderText(null);
            alert.setContentText("补货功能正在开发中...\n商品: " + selected.name);
            alert.showAndWait();
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
}
