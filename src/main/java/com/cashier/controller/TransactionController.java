package com.cashier.controller;

import com.cashier.controller.CreateReturnOrderDialogController;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Transaction;
import com.cashier.model.Product;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * 交易记录控制器
 * 处理交易记录的查询和显示
 */
public class TransactionController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TransactionController.class);

    @FXML
    private TableView<Transaction> transactionTable;

    @FXML
    private TableColumn<Transaction, String> transactionIdColumn;

    @FXML
    private TableColumn<Transaction, String> timestampColumn;

    @FXML
    private TableColumn<Transaction, String> itemsColumn;

    @FXML
    private TableColumn<Transaction, String> amountColumn;

    @FXML
    private TableColumn<Transaction, String> paymentColumn;

    @FXML
    private TableColumn<Transaction, String> memberColumn;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> paymentMethodComboBox;

    @FXML
    private Label countLabel;

    @FXML
    private Label totalAmountLabel;

    @FXML
    private Button viewDetailButton;

    @FXML
    private Button createReturnButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button refreshButton;

    private ObservableList<Transaction> transactionList;
    private List<Transaction> allTransactions;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化支付方式下拉框
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(
            "全部",
            "现金",
            "微信",
            "支付宝",
            "银行卡"
        ));
        paymentMethodComboBox.getSelectionModel().select(0);

        // 设置表格列
        setupTableColumns();

        // 加载交易数据
        loadTransactions();

        // 设置表格选择模式
        transactionTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 添加表格选择监听
        transactionTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );

        // 设置行点击事件
        transactionTable.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Transaction transaction = row.getItem();
                    if (transaction != null) {
                        showTransactionDetail(transaction);
                    }
                }
            });
            return row;
        });
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        transactionIdColumn.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        itemsColumn.setCellValueFactory(cellData -> {
            Transaction t = cellData.getValue();
            if (t.items == null || t.items.isEmpty()) {
                return new SimpleStringProperty("无商品");
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(t.items.size(), 3); i++) {
                if (i > 0) sb.append(", ");
                sb.append(t.items.get(i).name);
            }
            if (t.items.size() > 3) {
                sb.append(" 等").append(t.items.size()).append("件商品");
            }
            return new SimpleStringProperty(sb.toString());
        });
        amountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("¥%.2f", cellData.getValue().finalAmount)));
        paymentColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        memberColumn.setCellValueFactory(cellData -> {
            String phone = cellData.getValue().memberPhone;
            return new SimpleStringProperty(phone == null || phone.isEmpty() ? "非会员" : phone);
        });
    }

    /**
     * 加载交易数据
     */
    private void loadTransactions() {
        logger.info("TransactionController: 开始加载交易数据...");
        try {
            allTransactions = TransactionDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载交易数据失败", e);
            showError("加载交易数据失败: " + e.getMessage());
            allTransactions = new java.util.ArrayList<>();
        }
        transactionList = FXCollections.observableArrayList(allTransactions);
        transactionTable.setItems(transactionList);
        updateStatistics();
        logger.info("TransactionController: 加载了 {} 条交易记录", allTransactions.size());
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        countLabel.setText("交易数量: " + transactionList.size());

        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : transactionList) {
            total = total.add(t.getFinalAmount());
        }
        totalAmountLabel.setText(String.format("总金额: ¥%.2f", total));
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = transactionTable.getSelectionModel().getSelectedItem() != null;
        viewDetailButton.setDisable(!hasSelection);
    }

    /**
     * 处理查看详情
     */
    @FXML
    private void handleViewDetail() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showTransactionDetail(selected);
        }
    }

    /**
     * 显示交易详情
     * @param transaction 交易记录
     */
    private void showTransactionDetail(Transaction transaction) {
        StringBuilder detail = new StringBuilder();
        detail.append("交易详情\n\n");
        detail.append("订单号: ").append(transaction.transactionId).append("\n");
        detail.append("交易时间: ").append(transaction.timestamp).append("\n");
        detail.append("支付方式: ").append(transaction.paymentMethod).append("\n");
        detail.append("会员手机: ").append(
            transaction.memberPhone == null || transaction.memberPhone.isEmpty() ? "无" : transaction.memberPhone
        ).append("\n\n");

        detail.append("商品列表:\n");
        if (transaction.items != null && !transaction.items.isEmpty()) {
            for (int i = 0; i < transaction.items.size(); i++) {
                var item = transaction.items.get(i);
                detail.append(String.format("  %d. %s x%d = ¥%.2f\n",
                    i + 1,
                    item.name,
                    item.quantity,
                    item.getPrice().multiply(BigDecimal.valueOf(item.quantity)).doubleValue()
                ));
            }
        } else {
            detail.append("  无商品\n");
        }

        detail.append("\n");
        detail.append("商品金额: ¥").append(String.format("%.2f", transaction.totalAmount)).append("\n");
        detail.append("税费: ¥").append(String.format("%.2f", transaction.tax)).append("\n");
        detail.append("实付金额: ¥").append(String.format("%.2f", transaction.finalAmount)).append("\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("交易详情");
        alert.setHeaderText(null);
        alert.setContentText(detail.toString());
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    /**
     * 处理创建退货
     */
    @FXML
    private void handleCreateReturn() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择要退货的交易记录");
            return;
        }

        try {
            // 获取交易明细
            List<Product> items = selected.getItems();
            if (items == null || items.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "提示", "未找到交易明细信息");
                return;
            }

            // 加载FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/CreateReturnOrderDialog.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置数据
            CreateReturnOrderDialogController controller = loader.getController();
            controller.setOriginalTransaction(selected, items);

            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.setTitle("创建退货订单 - " + selected.transactionId);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);

            // 显示对话框并等待关闭
            dialogStage.showAndWait();

            // 如果提交成功，刷新交易列表
            if (controller.isSubmitted()) {
                showAlert(Alert.AlertType.INFORMATION, "成功", "退货订单创建成功！");
                loadTransactions();
            }

        } catch (Exception e) {
            logger.error("打开退货订单对话框失败", e);
            showAlert(Alert.AlertType.ERROR, "错误", "打开退货订单对话框失败: " + e.getMessage());
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    private void handleSearch() {
        applyFilters();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    private void handleClearSearch() {
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        searchField.clear();
        paymentMethodComboBox.getSelectionModel().select(0);
        applyFilters();
    }

    /**
     * 应用筛选条件
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String paymentMethod = paymentMethodComboBox.getSelectionModel().getSelectedItem();

        transactionList.setAll(allTransactions.stream()
            .filter(t -> {
                // 日期筛选
                if (startDatePicker.getValue() != null || endDatePicker.getValue() != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        java.util.Date date = sdf.parse(t.timestamp);
                        java.time.LocalDate localDate = date.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();

                        if (startDatePicker.getValue() != null && localDate.isBefore(startDatePicker.getValue())) {
                            return false;
                        }
                        if (endDatePicker.getValue() != null && localDate.isAfter(endDatePicker.getValue())) {
                            return false;
                        }
                    } catch (Exception e) {
                        // 日期解析失败，跳过该记录
                        return false;
                    }
                }

                // 支付方式筛选
                if (!"全部".equals(paymentMethod) && !paymentMethod.equals(t.paymentMethod)) {
                    return false;
                }

                // 搜索文本筛选（订单号或会员手机号）
                if (!searchText.isEmpty()) {
                    return t.transactionId.toLowerCase().contains(searchText) ||
                           (t.memberPhone != null && t.memberPhone.contains(searchText));
                }

                return true;
            })
            .toList());

        updateStatistics();
    }

    /**
     * 处理导出
     */
    @FXML
    private void handleExport() {
        if (transactionList.isEmpty()) {
            showError("没有可导出的交易记录");
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

            exportTransactions(exportFormat);
        });
    }

    /**
     * 导出交易记录
     */
    private void exportTransactions(com.cashier.util.ExportUtil.ExportFormat format) {
        try {
            // 准备表头
            java.util.List<String> headers = java.util.Arrays.asList(
                "交易编号", "交易时间", "商品列表", "总金额", "税额", "最终金额", "支付方式", "会员手机号"
            );

            // 准备数据
            java.util.List<String[]> data = new java.util.ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (Transaction t : transactionList) {
                // 构建商品列表字符串
                String itemsStr;
                if (t.items == null || t.items.isEmpty()) {
                    itemsStr = "无商品";
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < t.items.size(); i++) {
                        if (i > 0) sb.append("; ");
                        com.cashier.model.Product product = t.items.get(i);
                        sb.append(product.name).append(" x ").append(product.quantity);
                    }
                    itemsStr = sb.toString();
                }

                // 处理时间格式
                String timestampStr;
                try {
                    // 尝试将时间戳转换为 Date
                    if (t.timestamp != null && !t.timestamp.isEmpty()) {
                        long time = Long.parseLong(t.timestamp);
                        java.util.Date date = new java.util.Date(time);
                        timestampStr = sdf.format(date);
                    } else {
                        timestampStr = "";
                    }
                } catch (Exception e) {
                    // 如果转换失败，直接使用原始值
                    timestampStr = t.timestamp != null ? t.timestamp : "";
                }

                data.add(new String[]{
                    t.transactionId,
                    timestampStr,
                    itemsStr,
                    String.format("¥%.2f", t.totalAmount),
                    String.format("¥%.2f", t.tax),
                    String.format("¥%.2f", t.finalAmount),
                    t.paymentMethod,
                    t.memberPhone == null || t.memberPhone.isEmpty() ? "非会员" : t.memberPhone
                });
            }

            // 导出数据
            String filePath = com.cashier.util.ExportUtil.export(
                "交易记录报表",
                headers,
                data,
                format,
                "交易记录"
            );

            if (filePath != null) {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("导出成功");
                successAlert.setHeaderText(null);
                successAlert.setContentText("文件已成功导出到:\n" + filePath);
                successAlert.showAndWait();
                updateStatus("导出成功");
            } else {
                showError("导出失败，请查看日志获取详细信息");
            }
        } catch (Exception e) {
            logger.error("导出交易记录失败", e);
            showError("导出失败: " + e.getMessage());
        }
    }

    /**
     * 处理刷新
     */
    @FXML
    private void handleRefresh() {
        loadTransactions();
        updateStatus("已刷新");
    }

    /**
     * 更新状态
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    /**
     * 显示提示信息
     * @param type 提示类型
     * @param title 标题
     * @param message 消息内容
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

    /**
     * 刷新交易列表
     */
    public void refreshTransactions() {
        loadTransactions();
    }
}
