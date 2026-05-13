package com.cashier.controller.base;

import com.cashier.i18n.I18nManager;
import com.cashier.util.DialogBuilder;
import com.cashier.util.FormValidator;
import com.cashier.util.ResponsiveLayout;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 基础控制器抽象类
 * 提供通用的CRUD操作和UI组件配置
 *
 * @param <T> 实体类型
 */
public abstract class BaseController<T> {

    protected final I18nManager i18n = I18nManager.getInstance();

    // ========== 抽象方法 - 子类必须实现 ==========

    /**
     * 加载表格数据
     */
    protected abstract void loadTableData();

    /**
     * 处理添加操作
     */
    protected abstract void handleAdd();

    /**
     * 处理编辑操作
     */
    protected abstract void handleEdit();

    /**
     * 处理删除操作
     */
    protected abstract void handleDelete();

    /**
     * 处理搜索操作
     */
    protected abstract void handleSearch();

    /**
     * 显示编辑对话框
     * @param item 要编辑的项，null表示新增
     * @return 是否保存成功
     */
    protected abstract boolean showEditDialog(T item);

    // ========== 通用表格配置 ==========

    /**
     * 配置表格列
     * @param table 表格
     * @param propertyName 属性名
     * @param title 列标题
     * @param width 列宽
     */
    protected <S> void configureColumn(TableView<T> table, String propertyName, String title, double width) {
        TableColumn<T, S> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setPrefWidth(width);
        column.setResizable(true);
        table.getColumns().add(column);
    }

    /**
     * 配置表格列（自动宽度）
     */
    protected <S> void configureColumn(TableView<T> table, String propertyName, String title) {
        TableColumn<T, S> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setResizable(true);
        table.getColumns().add(column);
    }

    /**
     * 设置表格数据
     */
    protected void setTableData(TableView<T> table, List<T> data) {
        ObservableList<T> observableList = FXCollections.observableArrayList(data);
        table.setItems(observableList);
    }

    /**
     * 获取表格选中的项
     */
    protected T getSelectedItem(TableView<T> table) {
        return table.getSelectionModel().getSelectedItem();
    }

    /**
     * 获取表格选中的多项
     */
    protected ObservableList<T> getSelectedItems(TableView<T> table) {
        return table.getSelectionModel().getSelectedItems();
    }

    // ========== 通用操作确认 ==========

    /**
     * 确认删除操作
     */
    protected boolean confirmDelete(String itemName) {
        return DialogBuilder.confirmation()
                .title(i18n.get("dialog.confirm"))
                .content(i18n.get("dialog.delete.confirm_message"))
                .showAndGetConfirm();
    }

    /**
     * 确认删除操作（带名称）
     */
    protected boolean confirmDeleteWithName(String name) {
        return DialogBuilder.confirmation()
                .title(i18n.get("dialog.confirm"))
                .content(String.format(i18n.get("dialog.delete.confirm_with_name"), name))
                .showAndGetConfirm();
    }

    /**
     * 显示操作成功消息
     */
    protected void showSuccess(String message) {
        DialogBuilder.information()
                .title(i18n.get("label.success"))
                .content(message)
                .show();
    }

    /**
     * 显示操作失败消息
     */
    protected void showError(String message) {
        DialogBuilder.error()
                .title(i18n.get("label.error"))
                .content(message)
                .show();
    }

    /**
     * 显示确认对话框
     */
    protected boolean confirm(String title, String message) {
        return DialogBuilder.confirmation()
                .title(title)
                .content(message)
                .showAndGetConfirm();
    }

    // ========== 通用表格选择监听 ==========

    /**
     * 设置表格选择监听
     */
    protected void setupTableSelectionListener(TableView<T> table, Consumer<T> onSelect) {
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onSelect.accept(newVal);
            }
        });
    }

    /**
     * 设置表格双击编辑监听
     */
    protected void setupTableDoubleClickListener(TableView<T> table) {
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEdit();
                }
            });
            return row;
        });
    }

    // ========== 通用搜索功能 ==========

    /**
     * 执行搜索并更新表格
     */
    protected void searchAndUpdateTable(TableView<T> table, TextField searchField, Supplier<List<T>> searchFunction) {
        String query = searchField.getText();
        List<T> results = searchFunction.get();
        setTableData(table, results);
    }

    /**
     * 清空搜索并重新加载
     */
    protected void clearSearch(TableView<T> table, TextField searchField) {
        searchField.clear();
        loadTableData();
    }

    // ========== 通用按钮状态管理 ==========

    /**
     * 更新按钮状态（基于选中状态）
     */
    protected void updateButtonState(TableView<T> table, Button editButton, Button deleteButton) {
        T selectedItem = getSelectedItem(table);
        boolean hasSelection = selectedItem != null;

        if (editButton != null) {
            editButton.setDisable(!hasSelection);
        }
        if (deleteButton != null) {
            deleteButton.setDisable(!hasSelection);
        }
    }

    /**
     * 设置按钮状态
     */
    protected void setButtonEnabled(Button button, boolean enabled) {
        if (button != null) {
            button.setDisable(!enabled);
        }
    }

    // ========== 通用表单验证 ==========

    /**
     * 验证必填字段
     */
    protected boolean validateRequired(TextField... fields) {
        for (TextField field : fields) {
            if (field.getText() == null || field.getText().trim().isEmpty()) {
                field.requestFocus();
                return false;
            }
        }
        return true;
    }

    /**
     * 使用 FormValidator 验证字段
     */
    protected boolean validateField(TextField field, Label errorLabel, FormValidator.ValidationRule... rules) {
        FormValidator.ValidationResult result = FormValidator.validate(field, errorLabel, rules);
        return result.isValid();
    }

    // ========== 通用分页功能 ==========

    /**
     * 创建分页控件
     */
    protected Pagination createPagination(int pageCount) {
        Pagination pagination = new Pagination(pageCount, 0);
        pagination.setPageCount(pageCount);
        return pagination;
    }

    // ========== 通用导出功能 ==========

    /**
     * 导出表格数据为CSV
     */
    protected void exportToCSV(TableView<T> table, String fileName) {
        // 实现导出功能
        showSuccess("导出功能开发中");
    }
}
