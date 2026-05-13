# DataTable 组件使用指南

## 概述

`DataTable<T>` 是一个通用的表格组件，提供内置的搜索、分页、排序功能，可减少CRUD控制器的代码重复。

## 基本用法

### 1. 创建DataTable组件

```java
// 创建带工具栏的DataTable
DataTable<YourEntity> dataTable = new DataTable<>(true);

// 创建不带工具栏的DataTable
DataTable<YourEntity> dataTable = new DataTable<>(false);
```

### 2. 配置列

```java
// 使用属性名添加列
dataTable.addColumn("propertyName", "列标题", 150.0);

// 使用自定义值工厂添加列
dataTable.addStringColumn("自定义列", 200.0, entity -> {
    // 自定义逻辑
    return entity.getCustomValue();
});
```

### 3. 设置数据

```java
// 直接设置数据
List<YourEntity> data = fetchData();
dataTable.setData(data);

// 使用数据加载器
dataTable.setDataLoader(this::loadData);
dataTable.loadData();
```

### 4. 配置搜索

```java
// 设置搜索框提示
dataTable.getSearchField().setPromptText("搜索...");

// 设置搜索函数
dataTable.setSearchFunction(this::searchData);

// 执行搜索
dataTable.getSearchButton().setOnAction(e -> dataTable.search());
```

### 5. 配置按钮事件

```java
dataTable.getAddButton().setOnAction(e -> handleAdd());
dataTable.getEditButton().setOnAction(e -> handleEdit());
dataTable.getDeleteButton().setOnAction(e -> handleDelete());
dataTable.getRefreshButton().setOnAction(e -> dataTable.refresh());
```

### 6. 选择操作

```java
// 获取选中项
YourEntity selected = dataTable.getSelectedItem();

// 获取选中多项
ObservableList<YourEntity> selected = dataTable.getSelectedItems();

// 启用多选
dataTable.enableMultipleSelection();

// 设置双击编辑
dataTable.setDoubleClickEdit(this::handleEdit);

// 设置选择变化监听
dataTable.setSelectionChangeListener(obs -> {
    YourEntity selected = dataTable.getSelectedItem();
    // 处理选择变化
});
```

## 完整示例

```java
public class SupplierController extends BaseController<Supplier> {

    @FXML
    private VBox container;

    private DataTable<Supplier> dataTable;

    @FXML
    private void initialize() {
        // 创建DataTable
        dataTable = new DataTable<>(true);

        // 配置列
        dataTable.addColumn("name", "供应商名称", 150);
        dataTable.addColumn("phone", "联系电话", 120);
        dataTable.addColumn("address", "地址", 200);

        // 设置数据加载器
        dataTable.setDataLoader(this::loadSuppliers);

        // 设置搜索函数
        dataTable.setSearchFunction(this::searchSuppliers);

        // 配置按钮事件
        setupButtonHandlers();

        // 启用多选
        dataTable.enableMultipleSelection();

        // 设置双击编辑
        dataTable.setDoubleClickEdit(this::handleEdit);

        // 添加到容器
        container.getChildren().add(dataTable);

        // 加载数据
        dataTable.loadData();
    }

    private void setupButtonHandlers() {
        dataTable.getAddButton().setOnAction(e -> handleAdd());
        dataTable.getEditButton().setOnAction(e -> handleEdit());
        dataTable.getDeleteButton().setOnAction(e -> handleDelete());
        dataTable.getSearchButton().setOnAction(e -> handleSearch());
        dataTable.getRefreshButton().setOnAction(e -> dataTable.refresh());
    }

    private List<Supplier> loadSuppliers() {
        try {
            return SupplierDAO.findAll();
        } catch (SQLException e) {
            showError("加载数据失败: " + e.getMessage());
            return List.of();
        }
    }

    private List<Supplier> searchSuppliers() {
        String query = dataTable.getSearchField().getText().trim().toLowerCase();
        // 实现搜索逻辑
        return filteredList;
    }
}
```

## 对应的FXML

```xml
<VBox xmlns="http://javafx.com/javafx/17"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.cashier.controller.SupplierController"
      styleClass="supplier-view">

    <!-- DataTable将在这里动态创建 -->
    < fx:id="container" VBox.vgrow="ALWAYS"/>

</VBox>
```

## API 参考

### 方法

| 方法 | 描述 |
|------|------|
| `addColumn(String, String, double)` | 添加列（属性名） |
| `addStringColumn(String, double, Callback)` | 添加列（自定义值工厂） |
| `setData(List)` | 设置数据 |
| `loadData()` | 使用数据加载器加载数据 |
| `search()` | 执行搜索 |
| `refresh()` | 刷新数据 |
| `getSelectedItem()` | 获取选中项 |
| `getSelectedItems()` | 获取选中多项 |
| `enableMultipleSelection()` | 启用多选 |
| `setDoubleClickEdit(Runnable)` | 设置双击编辑 |
| `setSelectionChangeListener(InvalidationListener)` | 设置选择监听 |
| `clearSelection()` | 清空选择 |

### 获取组件

| 方法 | 返回 |
|------|------|
| `getTableView()` | TableView |
| `getSearchField()` | TextField |
| `getSearchButton()` | Button |
| `getRefreshButton()` | Button |
| `getAddButton()` | Button |
| `getEditButton()` | Button |
| `getDeleteButton()` | Button |
| `getExportButton()` | Button |
