package com.cashier.controller;

import com.cashier.dao.PurchaseOrderDAO;
import com.cashier.dao.PurchaseOrderItemDAO;
import com.cashier.dao.SupplierDAO;
import com.cashier.model.PurchaseOrder;
import com.cashier.model.PurchaseOrderItem;
import com.cashier.model.Supplier;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * 采购报表控制器
 * 处理采购统计和报表分析
 */
public class PurchaseReportController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PurchaseReportController.class);

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> timeRangeComboBox;

    @FXML
    private ComboBox<String> supplierComboBox;

    @FXML
    private Label totalOrdersLabel;

    @FXML
    private Label totalAmountLabel;

    @FXML
    private Label totalQuantityLabel;

    @FXML
    private Label avgOrderAmountLabel;

    @FXML
    private Label pendingOrdersLabel;

    @FXML
    private Label approvedOrdersLabel;

    @FXML
    private Label completedOrdersLabel;

    @FXML
    private TableView<PurchaseReportRecord> orderTable;

    @FXML
    private TableColumn<PurchaseReportRecord, String> orderNoColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> supplierColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> dateColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> amountColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> quantityColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> statusColumn;

    @FXML
    private TableView<PurchaseReportRecord> supplierRankTable;

    @FXML
    private TableColumn<PurchaseReportRecord, String> rankColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> supplierNameColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> orderCountColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> supplierAmountColumn;

    @FXML
    private TableView<PurchaseReportRecord> categoryTable;

    @FXML
    private TableColumn<PurchaseReportRecord, String> categoryColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> categoryQuantityColumn;

    @FXML
    private TableColumn<PurchaseReportRecord, String> categoryAmountColumn;

    @FXML
    private Button queryButton;

    @FXML
    private Button exportButton;

    private List<PurchaseOrder> allOrders;
    private List<Supplier> allSuppliers;
    private Map<Integer, List<PurchaseOrderItem>> orderItemsMap;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化时间范围下拉框
        timeRangeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "今天",
            "昨天",
            "本周",
            "上周",
            "本月",
            "上月",
            "自定义"
        ));
        timeRangeComboBox.getSelectionModel().select(4); // 默认选中本月

        // 设置默认日期范围（本月）
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        startDatePicker.setValue(startOfMonth);
        endDatePicker.setValue(today);

        // 设置表格列
        setupOrderTableColumns();
        setupSupplierRankTableColumns();
        setupCategoryTableColumns();

        // 加载数据
        loadData();

        // 执行查询
        handleQuery();

        // 监听时间范围变化
        timeRangeComboBox.setOnAction(event -> handleTimeRangeChange());
    }

    /**
     * 设置订单表格列
     */
    private void setupOrderTableColumns() {
        orderNoColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().orderNo));
        supplierColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().supplierName));
        dateColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().date));
        amountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().amount)));
        quantityColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().quantity)));
        statusColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().status));
    }

    /**
     * 设置供应商排名表格列
     */
    private void setupSupplierRankTableColumns() {
        rankColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().rank)));
        supplierNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().supplierName));
        orderCountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().orderCount)));
        supplierAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().amount)));
    }

    /**
     * 设置分类表格列
     */
    private void setupCategoryTableColumns() {
        categoryColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().category));
        categoryQuantityColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().quantity)));
        categoryAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.format("¥%,.2f", cellData.getValue().amount)));
    }

    /**
     * 加载数据
     */
    private void loadData() {
        try {
            allOrders = PurchaseOrderDAO.findAll();
            allSuppliers = SupplierDAO.findAll();
            orderItemsMap = new HashMap<>();

            // 加载订单明细
            for (PurchaseOrder order : allOrders) {
                List<PurchaseOrderItem> items = PurchaseOrderItemDAO.findByOrderId(order.id);
                orderItemsMap.put(order.id, items);
            }

            // 加载供应商列表到下拉框
            javafx.collections.ObservableList<String> supplierList = javafx.collections.FXCollections.observableArrayList();
            supplierList.add("全部供应商");
            for (Supplier supplier : allSuppliers) {
                supplierList.add(supplier.name);
            }
            supplierComboBox.setItems(supplierList);
            supplierComboBox.getSelectionModel().select(0);

            logger.info("成功加载 {} 条采购订单记录", allOrders.size());
        } catch (SQLException e) {
            logger.error("加载数据失败", e);
            showError("加载数据失败: " + e.getMessage());
            allOrders = new ArrayList<>();
            allSuppliers = new ArrayList<>();
            orderItemsMap = new HashMap<>();
        }
    }

    /**
     * 处理时间范围变化
     */
    private void handleTimeRangeChange() {
        String selected = timeRangeComboBox.getSelectionModel().getSelectedItem();
        LocalDate today = LocalDate.now();

        switch (selected) {
            case "今天":
                startDatePicker.setValue(today);
                endDatePicker.setValue(today);
                break;
            case "昨天":
                LocalDate yesterday = today.minusDays(1);
                startDatePicker.setValue(yesterday);
                endDatePicker.setValue(yesterday);
                break;
            case "本周":
                LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
                startDatePicker.setValue(startOfWeek);
                endDatePicker.setValue(today);
                break;
            case "上周":
                LocalDate startOfLastWeek = today.minusDays(today.getDayOfWeek().getValue() - 1).minusWeeks(1);
                LocalDate endOfLastWeek = startOfLastWeek.plusDays(6);
                startDatePicker.setValue(startOfLastWeek);
                endDatePicker.setValue(endOfLastWeek);
                break;
            case "本月":
                LocalDate startOfMonth = today.withDayOfMonth(1);
                startDatePicker.setValue(startOfMonth);
                endDatePicker.setValue(today);
                break;
            case "上月":
                LocalDate startOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                LocalDate endOfLastMonth = today.withDayOfMonth(1).minusDays(1);
                startDatePicker.setValue(startOfLastMonth);
                endDatePicker.setValue(endOfLastMonth);
                break;
            case "自定义":
                // 不自动设置日期
                break;
        }
    }

    /**
     * 处理查询
     */
    @FXML
    private void handleQuery() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showError("请选择日期范围！");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showError("开始日期不能晚于结束日期！");
            return;
        }

        String selectedSupplier = supplierComboBox.getSelectionModel().getSelectedItem();

        // 筛选订单记录
        List<PurchaseOrder> filteredOrders = filterOrders(startDate, endDate, selectedSupplier);

        // 计算统计数据
        calculateStatistics(filteredOrders);
    }

    /**
     * 筛选订单记录
     */
    private List<PurchaseOrder> filterOrders(LocalDate startDate, LocalDate endDate, String supplierName) {
        List<PurchaseOrder> filtered = new ArrayList<>();

        for (PurchaseOrder order : allOrders) {
            try {
                LocalDate orderDate = LocalDate.parse(order.purchaseDate);

                // 日期范围筛选
                if (!orderDate.isBefore(startDate) && !orderDate.isAfter(endDate)) {
                    // 供应商筛选
                    if (supplierName == null || supplierName.equals("全部供应商") ||
                        supplierName.equals(order.supplierName)) {
                        filtered.add(order);
                    }
                }
            } catch (Exception e) {
                // 日期解析失败，跳过该记录
                logger.warn("日期解析失败: {}", order.purchaseDate, e);
            }
        }

        return filtered;
    }

    /**
     * 计算统计数据
     */
    private void calculateStatistics(List<PurchaseOrder> orders) {
        // 总订单数、总金额、总数量
        int totalOrders = orders.size();
        double totalAmount = 0.0;
        int totalQuantity = 0;

        // 按状态统计
        int pendingOrders = 0;
        int approvedOrders = 0;
        int completedOrders = 0;

        // 供应商统计
        Map<String, Integer> supplierOrderCountMap = new HashMap<>();
        Map<String, Double> supplierAmountMap = new HashMap<>();

        // 分类统计
        Map<String, Integer> categoryQuantityMap = new HashMap<>();
        Map<String, Double> categoryAmountMap = new HashMap<>();

        for (PurchaseOrder order : orders) {
            totalAmount += order.totalAmount.doubleValue();

            // 状态统计
            switch (order.status) {
                case "pending":
                    pendingOrders++;
                    break;
                case "approved":
                    approvedOrders++;
                    break;
                case "completed":
                    completedOrders++;
                    break;
            }

            // 供应商统计
            supplierOrderCountMap.put(order.supplierName,
                supplierOrderCountMap.getOrDefault(order.supplierName, 0) + 1);
            supplierAmountMap.put(order.supplierName,
                supplierAmountMap.getOrDefault(order.supplierName, 0.0) + order.totalAmount.doubleValue());

            // 获取订单明细
            List<PurchaseOrderItem> items = orderItemsMap.get(order.id);
            if (items != null) {
                for (PurchaseOrderItem item : items) {
                    totalQuantity += item.quantity;

                    // 分类统计（从商品名称中提取分类，如果没有则使用"未分类"）
                    String category = "未分类";
                    if (item.productName != null && item.productName.contains("-")) {
                        category = item.productName.split("-")[0].trim();
                    }
                    categoryQuantityMap.put(category,
                        categoryQuantityMap.getOrDefault(category, 0) + item.quantity);
                    categoryAmountMap.put(category,
                        categoryAmountMap.getOrDefault(category, 0.0) + item.totalPrice.doubleValue());
                }
            }
        }

        // 更新统计卡片
        totalOrdersLabel.setText(String.valueOf(totalOrders));
        totalAmountLabel.setText(String.format("¥%,.2f", totalAmount));
        totalQuantityLabel.setText(String.valueOf(totalQuantity));
        avgOrderAmountLabel.setText(totalOrders > 0 ? String.format("¥%,.2f", totalAmount / totalOrders) : "¥0.00");
        pendingOrdersLabel.setText(String.valueOf(pendingOrders));
        approvedOrdersLabel.setText(String.valueOf(approvedOrders));
        completedOrdersLabel.setText(String.valueOf(completedOrders));

        // 更新订单表格
        updateOrderTable(orders);

        // 更新供应商排名表格
        updateSupplierRankTable(supplierOrderCountMap, supplierAmountMap);

        // 更新分类表格
        updateCategoryTable(categoryQuantityMap, categoryAmountMap);
    }

    /**
     * 更新订单表格
     */
    private void updateOrderTable(List<PurchaseOrder> orders) {
        javafx.collections.ObservableList<PurchaseReportRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (PurchaseOrder order : orders) {
            int quantity = 0;
            List<PurchaseOrderItem> items = orderItemsMap.get(order.id);
            if (items != null) {
                for (PurchaseOrderItem item : items) {
                    quantity += item.quantity;
                }
            }

            list.add(new PurchaseReportRecord(
                order.orderNo,
                order.supplierName,
                order.purchaseDate,
                order.totalAmount.doubleValue(),
                quantity,
                order.getStatusDisplayName()
            ));
        }

        orderTable.setItems(list);
    }

    /**
     * 更新供应商排名表格
     */
    private void updateSupplierRankTable(Map<String, Integer> orderCountMap, Map<String, Double> amountMap) {
        javafx.collections.ObservableList<PurchaseReportRecord> list = javafx.collections.FXCollections.observableArrayList();

        // 按金额排序
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(amountMap.entrySet());
        sortedEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int rank = 1;
        for (Map.Entry<String, Double> entry : sortedEntries) {
            list.add(new PurchaseReportRecord(
                rank++,
                entry.getKey(),
                orderCountMap.get(entry.getKey()),
                entry.getValue()
            ));
        }

        supplierRankTable.setItems(list);
    }

    /**
     * 更新分类表格
     */
    private void updateCategoryTable(Map<String, Integer> quantityMap, Map<String, Double> amountMap) {
        javafx.collections.ObservableList<PurchaseReportRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (String category : quantityMap.keySet()) {
            list.add(new PurchaseReportRecord(
                category,
                quantityMap.get(category),
                amountMap.get(category)
            ));
        }

        // 按金额排序
        list.sort((a, b) -> Double.compare(b.amount, a.amount));
        categoryTable.setItems(list);
    }

    /**
     * 处理导出
     */
    @FXML
    private void handleExport() {
        // 显示导出选项对话框
        ChoiceDialog<String> exportDialog = new ChoiceDialog<>(
            "订单统计", "订单统计", "供应商排名", "分类采购"
        );
        exportDialog.setTitle("选择导出内容");
        exportDialog.setHeaderText("请选择要导出的内容");
        exportDialog.setContentText("导出内容:");

        exportDialog.showAndWait().ifPresent(exportType -> {
            if (exportType.equals("订单统计")) {
                exportOrderReport();
            } else if (exportType.equals("供应商排名")) {
                exportSupplierRanking();
            } else if (exportType.equals("分类采购")) {
                exportCategoryReport();
            }
        });
    }

    /**
     * 导出订单报表
     */
    private void exportOrderReport() {
        if (orderTable.getItems().isEmpty()) {
            showError("没有可导出的订单数据");
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle("选择导出格式");
        formatDialog.setHeaderText("请选择导出格式");
        formatDialog.setContentText("格式:");

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "订单编号", "供应商名称", "采购日期", "订单金额", "商品数量", "订单状态"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (PurchaseReportRecord record : orderTable.getItems()) {
                    data.add(new String[]{
                        record.orderNo,
                        record.supplierName,
                        record.date,
                        String.format("¥%.2f", record.amount),
                        String.valueOf(record.quantity),
                        record.status
                    });
                }

                // 导出数据
                String filePath = com.cashier.util.ExportUtil.export(
                    "采购订单报表",
                    headers,
                    data,
                    exportFormat,
                    "采购订单"
                );

                if (filePath != null) {
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("导出成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("文件已成功导出到:\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("采购订单报表导出成功: {}", filePath);
                } else {
                    showError("导出失败，请查看日志获取详细信息");
                }
            } catch (Exception e) {
                logger.error("导出采购订单报表失败", e);
                showError("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 导出供应商排名
     */
    private void exportSupplierRanking() {
        if (supplierRankTable.getItems().isEmpty()) {
            showError("没有可导出的供应商数据");
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle("选择导出格式");
        formatDialog.setHeaderText("请选择导出格式");
        formatDialog.setContentText("格式:");

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "排名", "供应商名称", "订单数量", "采购总金额"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (PurchaseReportRecord record : supplierRankTable.getItems()) {
                    data.add(new String[]{
                        String.valueOf(record.rank),
                        record.supplierName,
                        String.valueOf(record.orderCount),
                        String.format("¥%.2f", record.amount)
                    });
                }

                // 导出数据
                String filePath = com.cashier.util.ExportUtil.export(
                    "供应商采购排名",
                    headers,
                    data,
                    exportFormat,
                    "供应商排名"
                );

                if (filePath != null) {
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("导出成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("文件已成功导出到:\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("供应商排名导出成功: {}", filePath);
                } else {
                    showError("导出失败，请查看日志获取详细信息");
                }
            } catch (Exception e) {
                logger.error("导出供应商排名失败", e);
                showError("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 导出分类采购报表
     */
    private void exportCategoryReport() {
        if (categoryTable.getItems().isEmpty()) {
            showError("没有可导出的分类数据");
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle("选择导出格式");
        formatDialog.setHeaderText("请选择导出格式");
        formatDialog.setContentText("格式:");

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            try {
                // 准备表头
                java.util.List<String> headers = java.util.Arrays.asList(
                    "商品分类", "采购数量", "采购金额"
                );

                // 准备数据
                java.util.List<String[]> data = new java.util.ArrayList<>();
                for (PurchaseReportRecord record : categoryTable.getItems()) {
                    data.add(new String[]{
                        record.category,
                        String.valueOf(record.quantity),
                        String.format("¥%.2f", record.amount)
                    });
                }

                // 导出数据
                String filePath = com.cashier.util.ExportUtil.export(
                    "分类采购报表",
                    headers,
                    data,
                    exportFormat,
                    "分类采购"
                );

                if (filePath != null) {
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("导出成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("文件已成功导出到:\n" + filePath);
                    successAlert.showAndWait();
                    logger.info("分类采购报表导出成功: {}", filePath);
                } else {
                    showError("导出失败，请查看日志获取详细信息");
                }
            } catch (Exception e) {
                logger.error("导出分类采购报表失败", e);
                showError("导出失败: " + e.getMessage());
            }
        });
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 采购报表记录内部类
     */
    private static class PurchaseReportRecord {
        String orderNo;
        String supplierName;
        String date;
        double amount;
        int quantity;
        String status;
        int rank;
        int orderCount;
        String category;

        // 订单记录构造函数
        public PurchaseReportRecord(String orderNo, String supplierName, String date,
                                   double amount, int quantity, String status) {
            this.orderNo = orderNo;
            this.supplierName = supplierName;
            this.date = date;
            this.amount = amount;
            this.quantity = quantity;
            this.status = status;
        }

        // 供应商排名构造函数
        public PurchaseReportRecord(int rank, String supplierName, int orderCount, double amount) {
            this.rank = rank;
            this.supplierName = supplierName;
            this.orderCount = orderCount;
            this.amount = amount;
        }

        // 分类统计构造函数
        public PurchaseReportRecord(String category, int quantity, double amount) {
            this.category = category;
            this.quantity = quantity;
            this.amount = amount;
        }
    }
}