package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.controller.base.BaseController;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.Category;
import com.cashier.model.PageResult;
import com.cashier.model.Product;
import com.cashier.model.Unit;
import com.cashier.model.User;
import com.cashier.util.CacheManager;
import com.cashier.util.FXMLUtils;
import com.cashier.util.StatusBarManager;
import com.cashier.util.FormValidator;
import com.cashier.util.UIOptimizer;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.List;
import java.util.HashMap;

/**
 * 商品管理控制器
 * 处理商品的添加、编辑、删除、补货等操作
 * 已重构为继承 BaseController 并使用重构版 DAO
 */
@SuppressWarnings("unchecked")
public class InventoryController extends BaseController<Product> {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InventoryController.class);
    private static final int FIRST_PAGE = 1;
    private static final int DESKTOP_PAGE_SIZE = 500;

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
    private TableColumn<Product, Product> hotColumn;

    @FXML
    private TableColumn<Product, String> warningColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryFilterComboBox;

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

    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    // 声明即初始化：这两个字段由异步加载器赋值，而分类下拉框的监听器（同样由异步回调触发选中）
    // 会在数据到达前就读它们 —— 曾经因此抛 NPE（Windows 上实测：updateCountLabel 里 inventoryList 为 null）
    private final ObservableList<Product> inventoryList = FXCollections.observableArrayList();
    private final Map<Integer, Product> inventoryMap = new HashMap<>();
    private long totalProducts;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化分类筛选列表
        loadCategories();

        // 初始化排序列表
        sortComboBox.setItems(FXCollections.observableArrayList(
            i18n.get("inventory.sort.default"),
            i18n.get("inventory.sort.name"),
            i18n.get("inventory.sort.price_asc"),
            i18n.get("inventory.sort.price_desc"),
            i18n.get("inventory.sort.stock_desc"),
            i18n.get("inventory.sort.stock_asc")
        ));
        sortComboBox.getSelectionModel().select(0);
        sortComboBox.setOnAction(event -> sortInventory());

        // 分类筛选监听
        categoryFilterComboBox.setOnAction(event -> handleCategoryFilter());

        // 设置表格列
        setupTableColumns();

        // 加载库存数据
        loadTableData();

        // 设置表格选择模式
        inventoryTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 添加表格选择监听
        setupTableSelectionListener(inventoryTable, product -> updateButtonStates());

        // 设置表格双击编辑监听
        setupTableDoubleClickListener(inventoryTable);

        // 启用 UI 性能优化（固定行高启用更好的虚拟流）
        inventoryTable.setFixedCellSize(50.0);
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * 加载分类列表到筛选下拉框
     */
    /** 单位管理弹窗的数据加载：后台查库 + 回 FX 线程填表（TD-006）。 */
    private void loadUnitManagementData(ObservableList<Unit> target) {
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getUnitDAO().findAll(),
            target::setAll,
            e -> logger.error("加载单位失败", e));
    }

    /** 分类管理弹窗的数据加载：后台查库 + 回 FX 线程填表（TD-006）。 */
    private void loadCategoryManagementData(ObservableList<Category> target) {
        UIOptimizer.runInBackground(
            () -> DAOFactory.getInstance().getCategoryDAO().findAll(),
            target::setAll,
            e -> logger.error("加载分类失败", e));
    }

    private void loadCategories() {
        // 打开库存页还会查一次分类：同样放后台
        UIOptimizer.runInBackground(
            () -> {
                List<String> categories = new java.util.ArrayList<>();
                categories.add(i18n.get("inventory.all_categories")); // 全部分类
                for (Category c : DAOFactory.getInstance().getCategoryDAO().findAll()) {
                    categories.add(c.name);
                }
                return categories;
            },
            categories -> {
                categoryFilterComboBox.setItems(FXCollections.observableArrayList(categories));
                categoryFilterComboBox.getSelectionModel().select(0); // 默认选中"全部"
            },
            e -> logger.error("加载分类列表失败", e));
    }

    /**
     * 处理分类筛选
     */
    @FXML
    private void handleCategoryFilter() {
        applyFilters();
    }

    /**
     * 应用搜索和分类筛选
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim();
        String selectedCategory = categoryFilterComboBox.getSelectionModel().getSelectedItem();
        boolean isAllCategories = i18n.get("inventory.all_categories").equals(selectedCategory);

        try {
            if (searchText.isEmpty() && isAllCategories) {
                // 无筛选，加载全部
                loadTableData();
            } else if (!searchText.isEmpty() && isAllCategories) {
                // 仅搜索关键词
                PageResult<Product> results = productDAO.search(searchText, FIRST_PAGE, DESKTOP_PAGE_SIZE);
                totalProducts = results.getTotal();
                setLoadedProducts(results.getData());
                inventoryList.setAll(inventoryMap.values());
            } else if (searchText.isEmpty() && !isAllCategories) {
                // 仅按分类筛选
                PageResult<Product> results = productDAO.findByCategory(selectedCategory, FIRST_PAGE, DESKTOP_PAGE_SIZE);
                totalProducts = results.getTotal();
                setLoadedProducts(results.getData());
                inventoryList.setAll(inventoryMap.values());
            } else {
                // 同时按关键词和分类筛选：按分类分页查询后在结果内过滤关键词（避免全量加载）
                PageResult<Product> results = productDAO.findByCategory(selectedCategory, FIRST_PAGE, DESKTOP_PAGE_SIZE);
                List<Product> filtered = new java.util.ArrayList<>();

                for (Product p : results.getData()) {
                    if (containsIgnoreCase(p.name, searchText)
                        || containsIgnoreCase(p.barcode, searchText)
                        || containsIgnoreCase(p.productCode, searchText)) {
                        filtered.add(p);
                    }
                }

                totalProducts = filtered.size();
                setLoadedProducts(filtered);
                inventoryList.setAll(inventoryMap.values());
            }
            updateCountLabel();
        } catch (SQLException e) {
            logger.error("筛选商品失败", e);
            showError(i18n.get(I18nKeys.Message.OPERATION_FAILED) + ": " + e.getMessage());
        }
    }

    private static boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        barcodeColumn.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format(java.util.Locale.ROOT, "%.2f", cellData.getValue().price)));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        minStockColumn.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        // 热销列：显示●图标，点击可切换
        // 使用 Object-based 单元格，直接访问 Product 对象
        hotColumn.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue()));
        hotColumn.setCellFactory(column -> new TableCell<Product, Product>() {
            {
                getStyleClass().addAll("cursor-hand", "fs-20");
                setOnMouseClicked(event -> {
                    Product p = getItem();
                    if (p != null) {
                        logger.info("点击切换热销状态: {} (当前状态: {})", p.name, p.isHot);
                        toggleHotStatus(p);
                    }
                });
            }

            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                } else {
                    setText(product.isHot ? "●" : "○");
                    setAlignment(Pos.CENTER);
                    logger.debug("渲染热销单元格: 商品={}, isHot={}", product.name, product.isHot);
                }
            }
        });

        warningColumn.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            if (p.quantity <= 0) {
                return new SimpleStringProperty(i18n.get("inventory.status.out_of_stock"));
            } else if (p.quantity < p.minStock) {
                return new SimpleStringProperty(i18n.get(I18nKeys.Inventory.Status.LOW_STOCK));
            } else {
                return new SimpleStringProperty(i18n.get(I18nKeys.Inventory.Status.NORMAL));
            }
        });

        // 设置列排序功能
        nameColumn.setSortable(true);
        nameColumn.setComparator(Comparator.naturalOrder());

        priceColumn.setSortable(true);
        priceColumn.setComparator((s1, s2) -> {
            try {
                double d1 = FormValidator.parseDouble(s1, 0);
                double d2 = FormValidator.parseDouble(s2, 0);
                return Double.compare(d1, d2);
            } catch (IllegalArgumentException e) {
                return s1.compareTo(s2);
            }
        });

        quantityColumn.setSortable(true);
        quantityColumn.setComparator((s1, s2) -> {
            try {
                int i1 = FormValidator.parseInt(s1, 0);
                int i2 = FormValidator.parseInt(s2, 0);
                return Integer.compare(i1, i2);
            } catch (IllegalArgumentException e) {
                return s1.compareTo(s2);
            }
        });

        minStockColumn.setSortable(true);
        minStockColumn.setComparator((s1, s2) -> {
            try {
                int i1 = FormValidator.parseInt(s1);
                int i2 = FormValidator.parseInt(s2);
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
    @Override
    protected void loadTableData() {
        // 打开库存页就是一次整表分页查询：放后台，否则数据量大时整屏冻结
        UIOptimizer.runInBackground(
            () -> productDAO.findAll(FIRST_PAGE, DESKTOP_PAGE_SIZE),
            products -> {
                totalProducts = products.getTotal();
                setLoadedProducts(products.getData());
                refreshInventoryTable();
            },
            e -> {
                logger.error("加载商品数据失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
                inventoryMap.clear();
                totalProducts = 0;
                refreshInventoryTable();
            });
    }

    /** 把内存中的商品映射刷到表格（仅 FX 线程调用） */
    private void refreshInventoryTable() {
        inventoryList.setAll(inventoryMap.values());
        inventoryTable.setItems(inventoryList);
        updateCountLabel();
    }

    private void setLoadedProducts(java.util.Collection<Product> products) {
        inventoryMap.clear();
        for (Product product : products) {
            inventoryMap.put(product.id, product);
        }
    }

    /**
     * 更新商品数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(i18n.get("inventory.count") + ": " + inventoryList.size() + "/" + totalProducts);
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = !inventoryTable.getSelectionModel().getSelectedItems().isEmpty();
        setButtonEnabled(editButton, hasSelection);
        setButtonEnabled(deleteButton, hasSelection);
        setButtonEnabled(restockButton, hasSelection);
    }

    /**
     * 处理添加商品
     */
    @Override
    protected void handleAdd() {
        handleAddProduct();
    }

    @FXML
    public void handleAddProduct() {
        if (!requireInventoryManagement()) return;
        showEditDialog(null);
    }

    /**
     * 处理编辑商品
     */
    @Override
    protected void handleEdit() {
        handleEditProduct();
    }

    @FXML
    public void handleEditProduct() {
        if (!requireInventoryManagement()) return;
        Product selected = getSelectedItem(inventoryTable);
        if (selected != null) {
            showEditDialog(selected);
        } else {
            showWarning(i18n.get(I18nKeys.Runtime.SELECT_PRODUCT_FIRST));
        }
    }

    /**
     * 处理删除商品
     */
    @Override
    protected void handleDelete() {
        handleDeleteProduct();
    }

    @FXML
    public void handleDeleteProduct() {
        if (!requireInventoryManagement()) return;
        ObservableList<Product> selected = getSelectedItems(inventoryTable);
        if (selected.isEmpty()) {
            showWarning(i18n.get(I18nKeys.Runtime.SELECT_PRODUCT_FIRST));
            return;
        }

        if (selected.size() == 1) {
            Product product = selected.get(0);
            if (confirmDeleteWithName(product.name)) {
                try {
                    if (productDAO.delete(product.id)) {
                        loadTableData();
                        StatusBarManager.updateSuccess(com.cashier.i18n.I18nManager.getInstance().get("status_message.product_deleted", product.name));
                    }
                } catch (SQLException e) {
                    logger.error("删除商品失败", e);
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.DELETE_DATA) + ": " + e.getMessage());
                }
            }
        } else {
            if (confirm(i18n.get(I18nKeys.Dialog.CONFIRM), String.format("确定要批量删除选中的 %d 个商品吗？", selected.size()))) {
                try {
                    int successCount = 0;
                    for (Product product : selected) {
                        if (productDAO.delete(product.id)) {
                            successCount++;
                        }
                    }
                    loadTableData();
                    showSuccess(i18n.get("runtime.products_deleted", successCount));
                } catch (SQLException e) {
                    logger.error("批量删除商品失败", e);
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.DELETE_DATA) + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * 显示编辑对话框
     */
    @Override
    protected boolean showEditDialog(Product item) {
        if (!requireInventoryManagement()) return false;
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ProductEditView.fxml");
            VBox root = loader.load();

            ProductEditController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(item == null ? i18n.get("product.add") : i18n.get(I18nKeys.ProductEdit.EDIT));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(inventoryTable.getScene().getWindow());
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

            dialogStage.setScene(scene);
            controller.setDialogStage(dialogStage);
            controller.setProduct(item);

            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                loadTableData();
                if (item == null) {
                    StatusBarManager.updateSuccess(com.cashier.i18n.I18nManager.getInstance()
                        .get("status_message.product_created_plain"));
                } else {
                    StatusBarManager.updateSuccess(com.cashier.i18n.I18nManager.getInstance()
                        .get("status_message.product_updated_plain"));
                }
                return true;
            }
        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * 处理快速入库
     */
    @FXML
    public void handleRestock() {
        if (!requireInventoryManagement()) return;
        Product selected = getSelectedItem(inventoryTable);
        if (selected != null) {
            try {
                FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/RestockView.fxml");
                VBox root = loader.load();

                RestockController controller = loader.getController();

                Stage dialogStage = new Stage();
                dialogStage.setTitle(i18n.get("inventory.restock"));
                dialogStage.initModality(Modality.WINDOW_MODAL);
                dialogStage.initOwner(inventoryTable.getScene().getWindow());
                dialogStage.setResizable(false);

                Scene scene = new Scene(root);
                com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

                dialogStage.setScene(scene);
                controller.setDialogStage(dialogStage);
                controller.setProduct(selected);

                dialogStage.showAndWait();

                if (controller.isOkClicked()) {
                    loadTableData();
                    StatusBarManager.updateSuccess(com.cashier.i18n.I18nManager.getInstance().get("status_message.quick_restock",
                        selected.name + " (+" + controller.getRestockQuantity() + ")"));
                    com.cashier.service.AuditService.success(currentUsername, "INVENTORY", "QUICK_RESTOCK",
                        "商品=" + selected.name + ", 数量=" + controller.getRestockQuantity(),
                        controller.getRestockQuantity());
                }
            } catch (IOException e) {
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            }
        } else {
            showWarning(i18n.get(I18nKeys.Runtime.SELECT_PRODUCT_FIRST));
        }
    }

    /**
     * 处理搜索
     */
    @Override
    @FXML
    public void handleSearch() {
        applyFilters();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        categoryFilterComboBox.getSelectionModel().select(0); // 重置分类为"全部"
        applyFilters();
    }

    /**
     * 处理刷新
     */
    @FXML
    public void handleRefresh() {
        loadTableData();
        StatusBarManager.updateSuccess(com.cashier.i18n.I18nManager.getInstance().get("status_message.product_list_refreshed"));
    }

    /**
     * 排序库存
     */
    private void sortInventory() {
        String selected = sortComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.equals(i18n.get("inventory.sort.name"))) {
            inventoryList.sort((p1, p2) -> p1.name.compareTo(p2.name));
        } else if (selected.equals(i18n.get("inventory.sort.price_asc"))) {
            inventoryList.sort((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()));
        } else if (selected.equals(i18n.get("inventory.sort.price_desc"))) {
            inventoryList.sort((p1, p2) -> p2.getPrice().compareTo(p1.getPrice()));
        } else if (selected.equals(i18n.get("inventory.sort.stock_desc"))) {
            inventoryList.sort((p1, p2) -> Integer.compare(p2.quantity, p1.quantity));
        } else if (selected.equals(i18n.get("inventory.sort.stock_asc"))) {
            inventoryList.sort((p1, p2) -> Integer.compare(p1.quantity, p2.quantity));
        } else {
            inventoryList.setAll(inventoryMap.values());
        }
    }

    /**
     * 处理分类管理
     */
    @FXML
    public void handleCategoryManagement() {
        if (!requireInventoryManagement()) return;
        showCategoryManagementDialog();
    }

    /**
     * 显示分类管理对话框
     */
    private void showCategoryManagementDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        prepareInventoryDialog(dialog, 600);
        dialog.setTitle(i18n.get("inventory.category_management"));
        dialog.setHeaderText(null);

        // 创建表格
        TableView<Category> categoryTable = new TableView<>();
        TableColumn<Category, String> nameCol = new TableColumn<>(i18n.get("product.category"));
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name));
        nameCol.setPrefWidth(150);

        TableColumn<Category, String> descCol = new TableColumn<>(i18n.get(I18nKeys.Common.DESCRIPTION));
        descCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().description));
        descCol.setPrefWidth(250);

        categoryTable.getColumns().addAll(nameCol, descCol);
        categoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 加载分类数据（后台查询，回 FX 线程填表）；categoryList 同时供编辑/删除分类弹窗使用
        ObservableList<Category> categoryList = FXCollections.observableArrayList();
        categoryTable.setItems(categoryList);
        loadCategoryManagementData(categoryList);

        // 创建按钮面板
        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        Button addBtn = new Button(i18n.get(I18nKeys.Common.ADD));
        Button editBtn = new Button(i18n.get(I18nKeys.Common.EDIT));
        Button deleteBtn = new Button(i18n.get(I18nKeys.Common.DELETE));

        addBtn.getStyleClass().add("primary-button");
        editBtn.getStyleClass().add("info-button");
        deleteBtn.getStyleClass().add("danger-button");

        buttonPanel.getChildren().addAll(addBtn, editBtn, deleteBtn);

        addBtn.setOnAction(event -> showAddCategoryDialog(categoryList));
        editBtn.setOnAction(event -> {
            Category sel = categoryTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showEditCategoryDialog(sel, categoryList);
            } else {
                showWarning(i18n.get(I18nKeys.Runtime.SELECT_PRODUCT_FIRST));
            }
        });
        deleteBtn.setOnAction(event -> {
            Category sel = categoryTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showDeleteCategoryDialog(sel, categoryList);
            } else {
                showWarning(i18n.get(I18nKeys.Runtime.SELECT_PRODUCT_FIRST));
            }
        });

        VBox content = new VBox(10);
        content.getStyleClass().add("dialog-content");
        content.setPadding(new Insets(10));
        content.getChildren().addAll(categoryTable, buttonPanel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.showAndWait();

        loadTableData();
    }

    private void showAddCategoryDialog(ObservableList<Category> categoryList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        prepareInventoryDialog(dialog, 520);
        dialog.setTitle(i18n.get(I18nKeys.Common.ADD));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.getStyleClass().add("form-grid");

        TextField codeField = new TextField();
        TextField nameField = new TextField();
        TextField descField = new TextField();

        grid.add(new Label(i18n.get("category.code") + ":"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label(i18n.get("category.name") + ":"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label(i18n.get(I18nKeys.Common.DESCRIPTION) + ":"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    Category cat = new Category(nameField.getText().trim(), descField.getText().trim());
                    cat.categoryCode = codeField.getText().trim();
                    if (DAOFactory.getInstance().getCategoryDAO().insert(cat)) {
                        categoryList.add(cat);
                    }
                } catch (SQLException e) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.SAVE_DATA) + ": " + e.getMessage());
                }
            }
        });
    }

    private void showEditCategoryDialog(Category category, ObservableList<Category> categoryList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        prepareInventoryDialog(dialog, 520);
        dialog.setTitle(i18n.get(I18nKeys.Common.EDIT));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.getStyleClass().add("form-grid");

        TextField codeField = new TextField(category.categoryCode);
        TextField nameField = new TextField(category.name);
        nameField.setEditable(false);
        TextField descField = new TextField(category.description);

        grid.add(new Label(i18n.get("category.code") + ":"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label(i18n.get("category.name") + ":"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label(i18n.get(I18nKeys.Common.DESCRIPTION) + ":"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    category.categoryCode = codeField.getText().trim();
                    category.description = descField.getText().trim();
                    if (DAOFactory.getInstance().getCategoryDAO().update(category)) {
                        categoryList.set(categoryList.indexOf(category), category);
                    }
                } catch (SQLException e) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.SAVE_DATA) + ": " + e.getMessage());
                }
            }
        });
    }

    private void showDeleteCategoryDialog(Category category, ObservableList<Category> categoryList) {
        if (confirmDeleteWithName(category.name)) {
            try {
                if (DAOFactory.getInstance().getCategoryDAO().deleteByName(category.name)) {
                    categoryList.remove(category);
                }
            } catch (SQLException e) {
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.DELETE_DATA) + ": " + e.getMessage());
            }
        }
    }

    /**
     * 处理单位管理
     */
    @FXML
    public void handleUnitManagement() {
        if (!requireInventoryManagement()) return;
        showUnitManagementDialog();
    }

    private void showUnitManagementDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        prepareInventoryDialog(dialog, 600);
        dialog.setTitle(i18n.get("inventory.unit_management"));

        TableView<Unit> unitTable = new TableView<>();
        TableColumn<Unit, String> nameCol = new TableColumn<>(i18n.get(I18nKeys.Unit.NAME));
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name));
        nameCol.setPrefWidth(150);

        TableColumn<Unit, String> descCol = new TableColumn<>(i18n.get(I18nKeys.Common.DESCRIPTION));
        descCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().description));
        descCol.setPrefWidth(250);

        unitTable.getColumns().addAll(nameCol, descCol);
        unitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 加载单位数据（后台查询，回 FX 线程填表）；unitList 同时供编辑/删除单位弹窗使用
        ObservableList<Unit> unitList = FXCollections.observableArrayList();
        unitTable.setItems(unitList);
        loadUnitManagementData(unitList);

        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        Button addBtn = new Button(i18n.get(I18nKeys.Common.ADD));
        Button editBtn = new Button(i18n.get(I18nKeys.Common.EDIT));
        Button deleteBtn = new Button(i18n.get(I18nKeys.Common.DELETE));

        addBtn.getStyleClass().add("primary-button");
        editBtn.getStyleClass().add("info-button");
        deleteBtn.getStyleClass().add("danger-button");

        buttonPanel.getChildren().addAll(addBtn, editBtn, deleteBtn);

        addBtn.setOnAction(event -> showAddUnitDialog(unitList));
        editBtn.setOnAction(event -> {
            Unit sel = unitTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showEditUnitDialog(sel, unitList);
            } else {
                showWarning(i18n.get(I18nKeys.Runtime.SELECT_PRODUCT_FIRST));
            }
        });
        deleteBtn.setOnAction(event -> {
            Unit sel = unitTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showDeleteUnitDialog(sel, unitList);
            } else {
                showWarning(i18n.get(I18nKeys.Runtime.SELECT_PRODUCT_FIRST));
            }
        });

        VBox content = new VBox(10);
        content.getStyleClass().add("dialog-content");
        content.setPadding(new Insets(10));
        content.getChildren().addAll(unitTable, buttonPanel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.showAndWait();

        loadTableData();
    }

    private void showAddUnitDialog(ObservableList<Unit> unitList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        prepareInventoryDialog(dialog, 520);
        dialog.setTitle(i18n.get(I18nKeys.Common.ADD));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.getStyleClass().add("form-grid");

        TextField nameField = new TextField();
        TextField descField = new TextField();

        grid.add(new Label(i18n.get(I18nKeys.Unit.NAME) + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(i18n.get(I18nKeys.Common.DESCRIPTION) + ":"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    Unit unit = new Unit(nameField.getText().trim(), descField.getText().trim());
                    if (DAOFactory.getInstance().getUnitDAO().insert(unit)) {
                        unitList.add(unit);
                    }
                } catch (SQLException e) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.SAVE_DATA) + ": " + e.getMessage());
                }
            }
        });
    }

    private void showEditUnitDialog(Unit unit, ObservableList<Unit> unitList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        prepareInventoryDialog(dialog, 520);
        dialog.setTitle(i18n.get(I18nKeys.Common.EDIT));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.getStyleClass().add("form-grid");

        TextField nameField = new TextField(unit.name);
        nameField.setEditable(false);
        TextField descField = new TextField(unit.description);

        grid.add(new Label(i18n.get(I18nKeys.Unit.NAME) + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(i18n.get(I18nKeys.Common.DESCRIPTION) + ":"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    unit.description = descField.getText().trim();
                    if (DAOFactory.getInstance().getUnitDAO().update(unit)) {
                        unitList.set(unitList.indexOf(unit), unit);
                    }
                } catch (SQLException e) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.SAVE_DATA) + ": " + e.getMessage());
                }
            }
        });
    }

    private void showDeleteUnitDialog(Unit unit, ObservableList<Unit> unitList) {
        if (confirmDeleteWithName(unit.name)) {
            try {
                if (DAOFactory.getInstance().getUnitDAO().deleteByName(unit.name)) {
                    unitList.remove(unit);
                }
            } catch (SQLException e) {
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.DELETE_DATA) + ": " + e.getMessage());
            }
        }
    }

    private void prepareInventoryDialog(Dialog<?> dialog, double width) {
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(width);
        dialog.getDialogPane().getStyleClass().add("management-dialog");
        if (inventoryTable != null && inventoryTable.getScene() != null) {
            dialog.initOwner(inventoryTable.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(inventoryTable.getScene().getStylesheets());
        }
    }

    private String currentUsername;
    private User currentUser;

    public void setCurrentUser(User user) {
        currentUser = user;
        currentUsername = user == null ? null : user.username;
        boolean canManage = user != null && user.hasPermission(User.PERMISSION_MANAGE_INVENTORY);
        for (Button button : List.of(addButton, editButton, deleteButton, restockButton, categoryButton, unitButton)) {
            button.setVisible(canManage);
            button.setManaged(canManage);
        }
    }

    /**
     * 切换商品热销状态
     * @param product 商品
     */
    private void toggleHotStatus(Product product) {
        try {
            boolean newHotStatus = !product.isHot;
            logger.info("切换热销状态: {} -> {}, 商品: {}", product.isHot, newHotStatus, product.name);
            boolean success = productDAO.updateHotStatus(product.id, newHotStatus);
            if (success) {
                product.isHot = newHotStatus;
                logger.info("本地对象已更新: {}, isHot = {}", product.name, product.isHot);
                // 清除缓存，确保触屏界面能获取最新数据
                CacheManager.clearCache();
                inventoryTable.refresh();
                String msg = newHotStatus
                    ? i18n.get("product.hot_marked") + ": " + product.name
                    : i18n.get("product.hot_unmarked") + ": " + product.name;
                StatusBarManager.updateSuccess(msg);
            } else {
                StatusBarManager.updateError(i18n.get("label.error") + ": " + i18n.get("operation.failed"));
            }
        } catch (SQLException e) {
            logger.error("更新热销状态失败", e);
            StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
        }
    }

    private boolean requireInventoryManagement() {
        if (currentUser != null && currentUser.hasPermission(User.PERMISSION_MANAGE_INVENTORY)) {
            return true;
        }
        showError(i18n.get("permission.access_denied"));
        return false;
    }
}
