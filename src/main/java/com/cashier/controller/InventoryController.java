package com.cashier.controller;

import com.cashier.controller.base.BaseController;
import com.cashier.dao.CategoryDAO;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
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

    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    private ObservableList<Product> inventoryList;
    private Map<Integer, Product> inventoryMap;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
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
        inventoryTable.setFixedCellSize(40.0);
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
                return new SimpleStringProperty(i18n.get("inventory.status.out_of_stock"));
            } else if (p.quantity < p.minStock) {
                return new SimpleStringProperty(i18n.get("inventory.status.low_stock"));
            } else {
                return new SimpleStringProperty(i18n.get("inventory.status.normal"));
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
    @Override
    protected void loadTableData() {
        try {
            List<Product> products = productDAO.findAll();
            inventoryMap = new HashMap<>();
            for (Product product : products) {
                inventoryMap.put(product.id, product);
            }
        } catch (SQLException e) {
            logger.error("加载商品数据失败", e);
            showError("加载商品数据失败: " + e.getMessage());
            inventoryMap = new HashMap<>();
        }
        inventoryList = FXCollections.observableArrayList(inventoryMap.values());
        inventoryTable.setItems(inventoryList);
        updateCountLabel();
    }

    /**
     * 更新商品数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(i18n.get("inventory.count") + ": " + inventoryList.size());
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
        Product selected = getSelectedItem(inventoryTable);
        if (selected != null) {
            showEditDialog(selected);
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
        ObservableList<Product> selected = getSelectedItems(inventoryTable);
        if (selected.isEmpty()) return;

        if (selected.size() == 1) {
            Product product = selected.get(0);
            if (confirmDeleteWithName(product.name)) {
                try {
                    if (productDAO.delete(product.id)) {
                        loadTableData();
                        StatusBarManager.updateStatus("商品删除成功: " + product.name);
                    }
                } catch (SQLException e) {
                    logger.error("删除商品失败", e);
                    showError("删除商品失败: " + e.getMessage());
                }
            }
        } else {
            if (confirm(i18n.get("dialog.confirm"), String.format("确定要批量删除选中的 %d 个商品吗？", selected.size()))) {
                try {
                    int successCount = 0;
                    for (Product product : selected) {
                        if (productDAO.delete(product.id)) {
                            successCount++;
                        }
                    }
                    loadTableData();
                    showSuccess(String.format("成功删除 %d 个商品", successCount));
                } catch (SQLException e) {
                    logger.error("批量删除商品失败", e);
                    showError("批量删除商品失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 显示编辑对话框
     */
    @Override
    protected boolean showEditDialog(Product item) {
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ProductEditView.fxml");
            VBox root = loader.load();

            ProductEditController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(item == null ? i18n.get("product.add") : i18n.get("product.edit"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(inventoryTable.getScene().getWindow());
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

            dialogStage.setScene(scene);
            controller.setDialogStage(dialogStage);
            controller.setProduct(item);

            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                loadTableData();
                StatusBarManager.updateStatus(item == null ? "商品添加成功" : "商品更新成功");
                return true;
            }
        } catch (IOException e) {
            showError("加载编辑对话框失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 处理快速入库
     */
    @FXML
    public void handleRestock() {
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
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());

                dialogStage.setScene(scene);
                controller.setDialogStage(dialogStage);
                controller.setProduct(selected);

                dialogStage.showAndWait();

                if (controller.isOkClicked()) {
                    loadTableData();
                    StatusBarManager.updateStatus("快速入库成功: " + selected.name + " (+" + controller.getRestockQuantity() + ")");
                }
            } catch (IOException e) {
                showError("加载快速入库对话框失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理搜索
     */
    @Override
    @FXML
    public void handleSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            inventoryList.setAll(inventoryMap.values());
        } else {
            try {
                List<Product> results = productDAO.search(searchText);
                inventoryList.setAll(results);
            } catch (SQLException e) {
                logger.error("搜索商品失败", e);
                showError("搜索商品失败: " + e.getMessage());
            }
        }
        updateCountLabel();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        inventoryList.setAll(inventoryMap.values());
        updateCountLabel();
    }

    /**
     * 处理刷新
     */
    @FXML
    public void handleRefresh() {
        loadTableData();
        StatusBarManager.updateStatus("商品列表已刷新");
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
        showCategoryManagementDialog();
    }

    /**
     * 显示分类管理对话框
     */
    private void showCategoryManagementDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("inventory.category_management"));
        dialog.setHeaderText(i18n.get("inventory.category_management.header"));

        // 创建表格
        TableView<Category> categoryTable = new TableView<>();
        TableColumn<Category, String> nameCol = new TableColumn<>(i18n.get("product.category"));
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name));
        nameCol.setPrefWidth(150);

        TableColumn<Category, String> descCol = new TableColumn<>(i18n.get("common.description"));
        descCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().description));
        descCol.setPrefWidth(250);

        categoryTable.getColumns().addAll(nameCol, descCol);
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
        Button addBtn = new Button(i18n.get("common.add"));
        Button editBtn = new Button(i18n.get("common.edit"));
        Button deleteBtn = new Button(i18n.get("common.delete"));

        addBtn.getStyleClass().add("primary-button");
        editBtn.getStyleClass().add("info-button");
        deleteBtn.getStyleClass().add("danger-button");

        buttonPanel.getChildren().addAll(addBtn, editBtn, deleteBtn);

        addBtn.setOnAction(event -> showAddCategoryDialog(categoryList));
        editBtn.setOnAction(event -> {
            Category sel = categoryTable.getSelectionModel().getSelectedItem();
            if (sel != null) showEditCategoryDialog(sel, categoryList);
        });
        deleteBtn.setOnAction(event -> {
            Category sel = categoryTable.getSelectionModel().getSelectedItem();
            if (sel != null) showDeleteCategoryDialog(sel, categoryList);
        });

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(categoryTable, buttonPanel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.showAndWait();

        loadTableData();
    }

    private void showAddCategoryDialog(ObservableList<Category> categoryList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("common.add"));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField codeField = new TextField();
        TextField nameField = new TextField();
        TextField descField = new TextField();

        grid.add(new Label(i18n.get("category.code") + ":"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label(i18n.get("category.name") + ":"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label(i18n.get("common.description") + ":"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    Category cat = new Category(nameField.getText().trim(), descField.getText().trim());
                    cat.categoryCode = codeField.getText().trim();
                    if (CategoryDAO.insert(cat)) {
                        categoryList.add(cat);
                    }
                } catch (SQLException e) {
                    showError("添加分类失败: " + e.getMessage());
                }
            }
        });
    }

    private void showEditCategoryDialog(Category category, ObservableList<Category> categoryList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("common.edit"));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField codeField = new TextField(category.categoryCode);
        TextField nameField = new TextField(category.name);
        nameField.setEditable(false);
        TextField descField = new TextField(category.description);

        grid.add(new Label(i18n.get("category.code") + ":"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label(i18n.get("category.name") + ":"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label(i18n.get("common.description") + ":"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    category.categoryCode = codeField.getText().trim();
                    category.description = descField.getText().trim();
                    if (CategoryDAO.update(category)) {
                        categoryList.set(categoryList.indexOf(category), category);
                    }
                } catch (SQLException e) {
                    showError("更新分类失败: " + e.getMessage());
                }
            }
        });
    }

    private void showDeleteCategoryDialog(Category category, ObservableList<Category> categoryList) {
        if (confirmDeleteWithName(category.name)) {
            try {
                if (CategoryDAO.deleteByName(category.name)) {
                    categoryList.remove(category);
                }
            } catch (SQLException e) {
                showError("删除分类失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理单位管理
     */
    @FXML
    public void handleUnitManagement() {
        showUnitManagementDialog();
    }

    private void showUnitManagementDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("inventory.unit_management"));

        TableView<Unit> unitTable = new TableView<>();
        TableColumn<Unit, String> nameCol = new TableColumn<>(i18n.get("unit.name"));
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name));
        nameCol.setPrefWidth(150);

        TableColumn<Unit, String> descCol = new TableColumn<>(i18n.get("common.description"));
        descCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().description));
        descCol.setPrefWidth(250);

        unitTable.getColumns().addAll(nameCol, descCol);
        unitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        ObservableList<Unit> unitList = FXCollections.observableArrayList();
        try {
            unitList.addAll(UnitDAO.findAll());
        } catch (SQLException e) {
            logger.error("加载单位失败", e);
        }
        unitTable.setItems(unitList);

        HBox buttonPanel = new HBox(10);
        Button addBtn = new Button(i18n.get("common.add"));
        Button editBtn = new Button(i18n.get("common.edit"));
        Button deleteBtn = new Button(i18n.get("common.delete"));

        addBtn.getStyleClass().add("primary-button");
        editBtn.getStyleClass().add("info-button");
        deleteBtn.getStyleClass().add("danger-button");

        buttonPanel.getChildren().addAll(addBtn, editBtn, deleteBtn);

        addBtn.setOnAction(event -> showAddUnitDialog(unitList));
        editBtn.setOnAction(event -> {
            Unit sel = unitTable.getSelectionModel().getSelectedItem();
            if (sel != null) showEditUnitDialog(sel, unitList);
        });
        deleteBtn.setOnAction(event -> {
            Unit sel = unitTable.getSelectionModel().getSelectedItem();
            if (sel != null) showDeleteUnitDialog(sel, unitList);
        });

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(unitTable, buttonPanel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.showAndWait();

        loadTableData();
    }

    private void showAddUnitDialog(ObservableList<Unit> unitList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("common.add"));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        TextField descField = new TextField();

        grid.add(new Label(i18n.get("unit.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(i18n.get("common.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    Unit unit = new Unit(nameField.getText().trim(), descField.getText().trim());
                    if (UnitDAO.insert(unit)) {
                        unitList.add(unit);
                    }
                } catch (SQLException e) {
                    showError("添加单位失败: " + e.getMessage());
                }
            }
        });
    }

    private void showEditUnitDialog(Unit unit, ObservableList<Unit> unitList) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("common.edit"));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(unit.name);
        nameField.setEditable(false);
        TextField descField = new TextField(unit.description);

        grid.add(new Label(i18n.get("unit.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(i18n.get("common.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    unit.description = descField.getText().trim();
                    if (UnitDAO.update(unit)) {
                        unitList.set(unitList.indexOf(unit), unit);
                    }
                } catch (SQLException e) {
                    showError("更新单位失败: " + e.getMessage());
                }
            }
        });
    }

    private void showDeleteUnitDialog(Unit unit, ObservableList<Unit> unitList) {
        if (confirmDeleteWithName(unit.name)) {
            try {
                if (UnitDAO.deleteByName(unit.name)) {
                    unitList.remove(unit);
                }
            } catch (SQLException e) {
                showError("删除单位失败: " + e.getMessage());
            }
        }
    }
}
