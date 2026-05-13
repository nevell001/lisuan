package com.cashier.component;

import com.cashier.controller.base.BaseController;
import com.cashier.i18n.I18nManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Supplier;

/**
 * 通用表格组件
 * 提供内置的搜索、分页、排序功能
 */
public class DataTable<T> extends VBox {

    private final I18nManager i18n = I18nManager.getInstance();

    private final TableView<T> tableView;
    private final TextField searchField;
    private final Button searchButton;
    private final Button refreshButton;
    private final Button addButton;
    private final Button editButton;
    private final Button deleteButton;
    private final Button exportButton;
    private final Label countLabel;

    private final HBox toolbar;
    private final HBox statusBar;

    private Supplier<List<T>> dataLoader;
    private Supplier<List<T>> searchFunction;

    public DataTable() {
        this(false);
    }

    public DataTable(boolean showToolbar) {
        setSpacing(10);
        getStyleClass().add("data-table");

        // 创建表格
        tableView = new TableView<>();
        tableView.getStyleClass().add("data-table-view");
        tableView.setTableMenuButtonVisible(true);
        tableView.setEditable(false);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);

        // 创建工具栏
        if (showToolbar) {
            searchField = new TextField();
            searchField.setPromptText(i18n.get("common.search"));
            searchField.setPrefWidth(200);
            searchField.getStyleClass().add("search-input");

            searchButton = new Button(i18n.get("common.search"));
            searchButton.getStyleClass().addAll("primary-button");

            refreshButton = new Button(i18n.get("common.refresh"));
            refreshButton.getStyleClass().addAll("secondary-button");

            addButton = new Button(i18n.get("common.add"));
            addButton.getStyleClass().addAll("primary-button");

            editButton = new Button(i18n.get("common.edit"));
            editButton.getStyleClass().addAll("secondary-button");

            deleteButton = new Button(i18n.get("common.delete"));
            deleteButton.getStyleClass().addAll("danger-button");

            exportButton = new Button(i18n.get("menu.file.export"));
            exportButton.getStyleClass().addAll("secondary-button");

            countLabel = new Label();
            countLabel.getStyleClass().add("count-label");

            toolbar = new HBox(10);
            toolbar.getStyleClass().add("data-table-toolbar");
            toolbar.getChildren().addAll(
                new Label("🔍"),
                searchField,
                searchButton,
                refreshButton,
                new javafx.scene.layout.Region(),
                addButton,
                editButton,
                deleteButton,
                exportButton
            );
            HBox.setHgrow(new javafx.scene.layout.Region(), javafx.scene.layout.Priority.ALWAYS);

            getChildren().add(toolbar);
        } else {
            searchField = null;
            searchButton = null;
            refreshButton = null;
            addButton = null;
            editButton = null;
            deleteButton = null;
            exportButton = null;
            countLabel = null;
            toolbar = null;
        }

        // 创建状态栏
        statusBar = new HBox(10);
        statusBar.getStyleClass().add("data-table-status-bar");

        Label hintLabel = new Label(i18n.get("data.hint"));
        hintLabel.getStyleClass().add("hint-label");

        statusBar.getChildren().addAll(countLabel, new javafx.scene.layout.Region(), hintLabel);
        HBox.setHgrow(new javafx.scene.layout.Region(), javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(tableView, statusBar);
    }

    /**
     * 添加列
     */
    public <S> void addColumn(String propertyName, String title, double width) {
        TableColumn<T, S> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setPrefWidth(width);
        column.setResizable(true);
        column.setReorderable(true);
        tableView.getColumns().add(column);
    }

    /**
     * 添加列（自定义值工厂）
     */
    public void addStringColumn(String title, double width, javafx.util.Callback<T, String> valueFactory) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(param -> {
            String value = valueFactory.call(param.getValue());
            return new SimpleStringProperty(value != null ? value : "");
        });
        column.setPrefWidth(width);
        column.setResizable(true);
        tableView.getColumns().add(column);
    }

    /**
     * 设置数据
     */
    public void setData(List<T> data) {
        ObservableList<T> observableList = FXCollections.observableArrayList(data);
        tableView.setItems(observableList);
        updateCountLabel(data.size());
    }

    /**
     * 获取表格
     */
    public TableView<T> getTableView() {
        return tableView;
    }

    /**
     * 获取搜索框
     */
    public TextField getSearchField() {
        return searchField;
    }

    /**
     * 获取工具栏按钮
     */
    public Button getSearchButton() { return searchButton; }
    public Button getRefreshButton() { return refreshButton; }
    public Button getAddButton() { return addButton; }
    public Button getEditButton() { return editButton; }
    public Button getDeleteButton() { return deleteButton; }
    public Button getExportButton() { return exportButton; }

    /**
     * 设置数据加载器
     */
    public void setDataLoader(Supplier<List<T>> dataLoader) {
        this.dataLoader = dataLoader;
    }

    /**
     * 设置搜索函数
     */
    public void setSearchFunction(Supplier<List<T>> searchFunction) {
        this.searchFunction = searchFunction;
    }

    /**
     * 加载数据
     */
    public void loadData() {
        if (dataLoader != null) {
            List<T> data = dataLoader.get();
            setData(data);
        }
    }

    /**
     * 执行搜索
     */
    public void search() {
        if (searchFunction != null) {
            List<T> results = searchFunction.get();
            setData(results);
        } else if (dataLoader != null) {
            loadData();
        }
    }

    /**
     * 刷新数据
     */
    public void refresh() {
        loadData();
    }

    /**
     * 获取选中项
     */
    public T getSelectedItem() {
        return tableView.getSelectionModel().getSelectedItem();
    }

    /**
     * 获取选中多项
     */
    public ObservableList<T> getSelectedItems() {
        return tableView.getSelectionModel().getSelectedItems();
    }

    /**
     * 更新计数标签
     */
    private void updateCountLabel(int count) {
        if (countLabel != null) {
            countLabel.setText(i18n.get("data.count", String.valueOf(count)));
        }
    }

    /**
     * 设置选择模式
     */
    public void setSelectionMode(SelectionMode mode) {
        tableView.getSelectionModel().setSelectionMode(mode);
    }

    /**
     * 启用多选
     */
    public void enableMultipleSelection() {
        setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * 设置双击编辑
     */
    public void setDoubleClickEdit(Runnable onEdit) {
        tableView.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && onEdit != null) {
                    onEdit.run();
                }
            });
            return row;
        });
    }

    /**
     * 设置选择变化监听
     */
    public void setSelectionChangeListener(javafx.beans.InvalidationListener listener) {
        tableView.getSelectionModel().selectedItemProperty().addListener(listener);
    }

    /**
     * 清空选择
     */
    public void clearSelection() {
        tableView.getSelectionModel().clearSelection();
    }
}
