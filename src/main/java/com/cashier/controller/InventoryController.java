package com.cashier.controller;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.CategoryDAO;
import com.cashier.dao.ProductDAO;
import com.cashier.dao.UnitDAO;
import com.cashier.model.Category;
import com.cashier.model.Product;
import com.cashier.model.Unit;
import com.cashier.util.FXMLUtils;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;
import java.util.Comparator;

/**
 * 商品管理控制器
 * 处理商品的添加、编辑、删除、补货等操作
 */
@SuppressWarnings("unchecked")
public class InventoryController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InventoryController.class);

    @FXML
    private TableView<Product> inventoryTable;

    @FXML
    private TableColumn<Product, String> barcodeColumn;

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

    @FXML
    private Button categoryButton;

    @FXML
    private Button unitButton;

    private ObservableList<Product> inventoryList;
    private Map<Integer, Product> inventory;

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
        barcodeColumn.setCellValueFactory(new PropertyValueFactory<>("barcode"));
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

        // 设置列排序功能
        nameColumn.setSortable(true);
        nameColumn.setComparator(Comparator.naturalOrder());

        priceColumn.setSortable(true);
        priceColumn.setComparator((s1, s2) -> {
            try {
                double d1 = Double.parseDouble(s1);
                double d2 = Double.parseDouble(s2);
                return Double.compare(d1, d2);
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        quantityColumn.setSortable(true);
        quantityColumn.setComparator((s1, s2) -> {
            try {
                int i1 = Integer.parseInt(s1);
                int i2 = Integer.parseInt(s2);
                return Integer.compare(i1, i2);
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        minStockColumn.setSortable(true);
        minStockColumn.setComparator((s1, s2) -> {
            try {
                int i1 = Integer.parseInt(s1);
                int i2 = Integer.parseInt(s2);
                return Integer.compare(i1, i2);
            } catch (NumberFormatException e) {
                return s1.compareTo(s2);
            }
        });

        categoryColumn.setSortable(true);
        categoryColumn.setComparator(Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /**
     * 加载库存数据
     */
    private void loadInventory() {
        try {
            var products = ProductDAO.findAll();
            inventory = new java.util.HashMap<>();
            for (Product product : products) {
                inventory.put(product.id, product);
            }
        } catch (SQLException e) {
            logger.error("加载商品数据失败", e);
            showError("加载商品数据失败: " + e.getMessage());
            inventory = new java.util.HashMap<>();
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
    public void handleAddProduct() {
        try {
            // 加载商品编辑对话框
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ProductEditView.fxml");
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

            // 如果用户点击了保存（ProductEditController 内部已经完成数据库操作）
            if (controller.isOkClicked()) {
                Product newProduct = controller.getProduct();
                loadInventory();
                updateStatus("商品添加成功: " + newProduct.name);
            }

        } catch (IOException e) {
            showError("加载添加商品对话框失败: " + e.getMessage());
        }
    }

    /**
     * 处理编辑商品
     */
    @FXML
    public void handleEditProduct() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // 加载商品编辑对话框
                FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ProductEditView.fxml");
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

                // 如果用户点击了保存（ProductEditController 内部已经完成数据库操作）
                if (controller.isOkClicked()) {
                    Product updatedProduct = controller.getProduct();
                    loadInventory();
                    updateStatus("商品更新成功: " + updatedProduct.name);
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
    public void handleDeleteProduct() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除商品 \"" + selected.name + "\" 吗？");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    ProductDAO.delete(selected.id);
                    loadInventory();
                    updateStatus("商品删除成功: " + selected.name);
                } catch (SQLException e) {
                    logger.error("删除商品失败", e);
                    showError("删除商品失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 处理快速入库
     */
    @FXML
    public void handleRestock() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // 加载快速入库对话框
                FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/RestockView.fxml");
                VBox root = loader.load();

                // 获取控制器
                RestockController controller = loader.getController();

                // 创建对话框
                Stage dialogStage = new Stage();
                dialogStage.setTitle("快速入库");
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
                    updateStatus("快速入库成功: " + selected.name + " (+" + controller.getRestockQuantity() + ")");
                }

            } catch (IOException e) {
                showError("加载快速入库对话框失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    public void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            inventoryList.setAll(inventory.values());
        } else {
            inventoryList.setAll(inventory.values().stream()
                .filter(p -> p.name.toLowerCase().contains(searchText) || 
                         (p.barcode != null && p.barcode.toLowerCase().contains(searchText)))
                .toList());
        }
        updateCountLabel();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        inventoryList.setAll(inventory.values());
        updateCountLabel();
    }

    /**
     * 处理刷新
     */
    @FXML
    public void handleRefresh() {
        refreshInventory();
        updateStatus("商品列表已刷新");
        showInfo("商品列表已刷新");
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
                inventoryList.sort((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()));
                break;
            case "按价格(高→低)":
                inventoryList.sort((p1, p2) -> p2.getPrice().compareTo(p1.getPrice()));
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
     * 显示信息
     * @param message 信息消息
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nManager.getInstance().get("common.tip"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 处理分类管理
     */
    @FXML
    public void handleCategoryManagement() {
        showCategoryManagementDialog();
    }

    /**
     * 显示分类管理对话框
     */
    private void showCategoryManagementDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("分类管理");
        dialog.setHeaderText("管理商品分类");

        // 创建表格
        TableView<Category> categoryTable = new TableView<>();
        TableColumn<Category, String> nameColumn = new TableColumn<>("分类名称");
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name));
        nameColumn.setPrefWidth(150);

        TableColumn<Category, String> descColumn = new TableColumn<>("描述");
        descColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().description));
        descColumn.setPrefWidth(250);

        categoryTable.getColumns().addAll(nameColumn, descColumn);
        categoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 加载分类数据
        ObservableList<Category> categoryList = FXCollections.observableArrayList();
        try {
            categoryList.addAll(CategoryDAO.findAll());
        } catch (SQLException e) {
            logger.error("加载分类失败", e);
        }
        categoryTable.setItems(categoryList);

        // 创建按钮面板
        HBox buttonPanel = new HBox(10);
        Button addButton = new Button("添加");
        Button editButton = new Button("编辑");
        Button deleteButton = new Button("删除");

        // 按钮样式
        addButton.getStyleClass().add("primary-button");
        editButton.getStyleClass().add("info-button");
        deleteButton.getStyleClass().add("danger-button");

        buttonPanel.getChildren().addAll(addButton, editButton, deleteButton);

        // 添加按钮事件
        addButton.setOnAction(event -> {
            showAddCategoryDialog(categoryList);
        });

        editButton.setOnAction(event -> {
            Category selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditCategoryDialog(selected, categoryList);
            } else {
                showInfo("请先选择要编辑的分类");
            }
        });

        deleteButton.setOnAction(event -> {
            Category selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showDeleteCategoryDialog(selected, categoryList);
            } else {
                showInfo("请先选择要删除的分类");
            }
        });

        // 创建对话框内容
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(categoryTable, buttonPanel);

        // 设置对话框内容
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // 显示对话框
        dialog.showAndWait();

        // 刷新库存表格
        loadInventory();
    }

    /**
     * 显示添加分类对话框
     */
    private void showAddCategoryDialog(ObservableList<Category> categoryList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("添加分类");
        dialog.setHeaderText("添加新的商品分类");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField categoryCodeField = new TextField();
        TextField nameField = new TextField();
        TextField descField = new TextField();

        grid.add(new Label("分类编号:"), 0, 0);
        grid.add(categoryCodeField, 1, 0);
        grid.add(new Label("分类名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("描述:"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 自动聚焦到编号字段
        javafx.application.Platform.runLater(categoryCodeField::requestFocus);

        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String categoryCode = categoryCodeField.getText().trim();
                String name = nameField.getText().trim();
                String description = descField.getText().trim();

                if (name.isEmpty()) {
                    showInfo("分类名称不能为空");
                    return;
                }

                try {
                    if (CategoryDAO.exists(name)) {
                        showInfo("分类已存在: " + name);
                        return;
                    }

                    Category category = new Category(name, description);
                    category.categoryCode = categoryCode;
                    if (CategoryDAO.insert(category)) {
                        categoryList.add(category);
                        showInfo("分类添加成功");
                    } else {
                        showInfo("分类添加失败");
                    }
                } catch (SQLException e) {
                    logger.error("添加分类失败", e);
                    showInfo("添加分类失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 显示编辑分类对话框
     */
    private void showEditCategoryDialog(Category category, ObservableList<Category> categoryList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("编辑分类");
        dialog.setHeaderText("编辑商品分类");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField categoryCodeField = new TextField(category.categoryCode);
        TextField nameField = new TextField(category.name);
        nameField.setEditable(false); // 分类名称不可修改
        TextField descField = new TextField(category.description);

        grid.add(new Label("分类编号:"), 0, 0);
        grid.add(categoryCodeField, 1, 0);
        grid.add(new Label("分类名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("描述:"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String categoryCode = categoryCodeField.getText().trim();
                String description = descField.getText().trim();

                try {
                    category.categoryCode = categoryCode;
                    category.description = description;
                    if (CategoryDAO.update(category)) {
                        categoryList.set(categoryList.indexOf(category), category);
                        showInfo("分类更新成功");
                    } else {
                        showInfo("分类更新失败");
                    }
                } catch (SQLException e) {
                    logger.error("更新分类失败", e);
                    showInfo("更新分类失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 显示删除分类确认对话框
     */
    private void showDeleteCategoryDialog(Category category, ObservableList<Category> categoryList) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get("common.delete"));
        alert.setHeaderText("确认删除");
        alert.setContentText("确定要删除分类 \"" + category.name + "\" 吗？");

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    if (CategoryDAO.deleteByName(category.name)) {
                        categoryList.remove(category);
                        showInfo("分类删除成功");
                    } else {
                        showInfo("分类删除失败");
                    }
                } catch (SQLException e) {
                    logger.error("删除分类失败", e);
                    showInfo("删除分类失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 处理单位管理
     */
    @FXML
    public void handleUnitManagement() {
        showUnitManagementDialog();
    }

    /**
     * 显示单位管理对话框
     */
    private void showUnitManagementDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("单位管理");
        dialog.setHeaderText("管理商品单位");

        // 创建表格
        TableView<Unit> unitTable = new TableView<>();
        TableColumn<Unit, String> nameColumn = new TableColumn<>("单位名称");
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name));
        nameColumn.setPrefWidth(150);

        TableColumn<Unit, String> descColumn = new TableColumn<>("描述");
        descColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().description));
        descColumn.setPrefWidth(250);

        unitTable.getColumns().addAll(nameColumn, descColumn);
        unitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 加载单位数据
        ObservableList<Unit> unitList = FXCollections.observableArrayList();
        try {
            unitList.addAll(UnitDAO.findAll());
        } catch (SQLException e) {
            logger.error("加载单位失败", e);
        }
        unitTable.setItems(unitList);

        // 创建按钮面板
        HBox buttonPanel = new HBox(10);
        Button addButton = new Button("添加");
        Button editButton = new Button("编辑");
        Button deleteButton = new Button("删除");

        // 按钮样式
        addButton.getStyleClass().add("primary-button");
        editButton.getStyleClass().add("info-button");
        deleteButton.getStyleClass().add("danger-button");

        buttonPanel.getChildren().addAll(addButton, editButton, deleteButton);

        // 添加按钮事件
        addButton.setOnAction(event -> {
            showAddUnitDialog(unitList);
        });

        editButton.setOnAction(event -> {
            Unit selected = unitTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditUnitDialog(selected, unitList);
            } else {
                showInfo("请先选择要编辑的单位");
            }
        });

        deleteButton.setOnAction(event -> {
            Unit selected = unitTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showDeleteUnitDialog(selected, unitList);
            } else {
                showInfo("请先选择要删除的单位");
            }
        });

        // 创建对话框内容
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(unitTable, buttonPanel);

        // 设置对话框内容
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // 显示对话框
        dialog.showAndWait();

        // 刷新库存表格
        loadInventory();
    }

    /**
     * 显示添加单位对话框
     */
    private void showAddUnitDialog(ObservableList<Unit> unitList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("添加单位");
        dialog.setHeaderText("添加新的商品单位");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        TextField descField = new TextField();

        grid.add(new Label("单位名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("描述:"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 自动聚焦到名称字段
        javafx.application.Platform.runLater(nameField::requestFocus);

        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String name = nameField.getText().trim();
                String description = descField.getText().trim();

                if (name.isEmpty()) {
                    showInfo("单位名称不能为空");
                    return;
                }

                try {
                    if (UnitDAO.exists(name)) {
                        showInfo("单位已存在: " + name);
                        return;
                    }

                    Unit unit = new Unit(name, description);
                    if (UnitDAO.insert(unit)) {
                        unitList.add(unit);
                        showInfo("单位添加成功");
                    } else {
                        showInfo("单位添加失败");
                    }
                } catch (SQLException e) {
                    logger.error("添加单位失败", e);
                    showInfo("添加单位失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 显示编辑单位对话框
     */
    private void showEditUnitDialog(Unit unit, ObservableList<Unit> unitList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("编辑单位");
        dialog.setHeaderText("编辑商品单位");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(unit.name);
        nameField.setEditable(false); // 单位名称不可修改
        TextField descField = new TextField(unit.description);

        grid.add(new Label("单位名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("描述:"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String description = descField.getText().trim();

                try {
                    unit.description = description;
                    if (UnitDAO.update(unit)) {
                        unitList.set(unitList.indexOf(unit), unit);
                        showInfo("单位更新成功");
                    } else {
                        showInfo("单位更新失败");
                    }
                } catch (SQLException e) {
                    logger.error("更新单位失败", e);
                    showInfo("更新单位失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 显示删除单位确认对话框
     */
    private void showDeleteUnitDialog(Unit unit, ObservableList<Unit> unitList) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get("common.delete"));
        alert.setHeaderText("确认删除");
        alert.setContentText("确定要删除单位 \"" + unit.name + "\" 吗？");

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    if (UnitDAO.deleteByName(unit.name)) {
                        unitList.remove(unit);
                        showInfo("单位删除成功");
                    } else {
                        showInfo("单位删除失败");
                    }
                } catch (SQLException e) {
                    logger.error("删除单位失败", e);
                    showInfo("删除单位失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nManager.getInstance().get("label.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}