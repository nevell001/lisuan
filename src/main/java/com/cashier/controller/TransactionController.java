package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.dao.DAOFactory;
import com.cashier.i18n.I18nManager;
import com.cashier.model.Transaction;
import com.cashier.model.Product;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.DateTimeFormats;
import com.cashier.util.FXMLUtils;
import com.cashier.util.StatusBarManager;
import com.cashier.util.FormValidator;
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

import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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
    private ComboBox<String> quickDateComboBox;

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
            "CASH",
            "WECHAT",
            "ALIPAY",
            "CARD"
        ));
        com.cashier.util.I18nUiUtils.configureComboBox(
            paymentMethodComboBox, com.cashier.util.I18nUiUtils::paymentMethod);
        paymentMethodComboBox.getSelectionModel().select(0);

        quickDateComboBox.setItems(FXCollections.observableArrayList(
            "今天", "昨天", "本周", "上周", "本月", "上月", "全部报表", "自定义"
        ));
        com.cashier.util.I18nUiUtils.configureComboBox(
            quickDateComboBox, com.cashier.util.I18nUiUtils::dateRange);
        LocalDate today = LocalDate.now();
        quickDateComboBox.setValue("本月");
        setDateRange(today.withDayOfMonth(1), today);

        // 设置表格列
        setupTableColumns();

        // 加载交易数据
        loadTransactions();
        quickDateComboBox.setOnAction(event -> handleQuickDateRange());

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

        // 启用 UI 性能优化（固定行高启用更好的虚拟流）
        transactionTable.setFixedCellSize(40.0);
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
                return new SimpleStringProperty(I18nManager.getInstance().get("transaction.no_items"));
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(t.items.size(), 3); i++) {
                if (i > 0) sb.append(", ");
                sb.append(t.items.get(i).name);
            }
            if (t.items.size() > 3) {
                sb.append(I18nManager.getInstance().get("transaction.items_more", t.items.size()));
            }
            return new SimpleStringProperty(sb.toString());
        });
        amountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyUtil.format(cellData.getValue().finalAmount.doubleValue())));
        paymentColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            com.cashier.util.I18nUiUtils.paymentMethod(cellData.getValue().paymentMethod)));
        memberColumn.setCellValueFactory(cellData -> {
            String phone = cellData.getValue().memberPhone;
            return new SimpleStringProperty(phone == null || phone.isEmpty()
                ? I18nManager.getInstance().get("runtime.non_member") : phone);
        });
    }

    /**
     * 加载交易数据
     */
    private void loadTransactions() {
        logger.info("TransactionController: 开始加载交易数据...");
        try {
            allTransactions = findTransactionsByCurrentDateRange();
        } catch (SQLException e) {
            logger.error("加载交易数据失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            allTransactions = new java.util.ArrayList<>();
        }
        transactionList = FXCollections.observableArrayList(allTransactions);
        transactionTable.setItems(transactionList);
        updateStatistics();
        logger.info("TransactionController: 加载了 {} 条交易记录", allTransactions.size());
    }

    private List<Transaction> findTransactionsByCurrentDateRange() throws SQLException {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return DAOFactory.getInstance().getTransactionDAO().findByDateRange(
            effectiveStart.atStartOfDay().format(DateTimeFormats.STANDARD_DATE_TIME),
            effectiveEnd.plusDays(1).atStartOfDay().minusSeconds(1).format(DateTimeFormats.STANDARD_DATE_TIME)
        );
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        countLabel.setText(I18nManager.getInstance().get("runtime.transaction_count", transactionList.size()));

        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : transactionList) {
            total = total.add(t.getFinalAmount());
        }
        totalAmountLabel.setText(I18nManager.getInstance().get("runtime.transaction_total", CurrencyUtil.format(total.doubleValue())));
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
    public void handleViewDetail() {
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
        I18nManager i18n = I18nManager.getInstance();
        detail.append(i18n.get("transaction.detail_title")).append("\n\n");
        detail.append(i18n.get("transaction.order_no")).append(transaction.transactionId).append("\n");
        detail.append(i18n.get("transaction.time_label")).append(transaction.timestamp).append("\n");
        detail.append(i18n.get("transaction.payment_label"))
            .append(com.cashier.util.I18nUiUtils.paymentMethod(transaction.paymentMethod)).append("\n");
        detail.append(i18n.get("transaction.member_phone")).append(
            transaction.memberPhone == null || transaction.memberPhone.isEmpty() ? i18n.get(I18nKeys.Common.NONE) : transaction.memberPhone
        ).append("\n\n");

        detail.append(i18n.get("transaction.item_list")).append("\n");
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
            detail.append("  ").append(i18n.get("transaction.no_items")).append("\n");
        }

        detail.append("\n");
        detail.append(i18n.get("transaction.product_amount")).append(CurrencyUtil.format(transaction.totalAmount.doubleValue())).append("\n");
        detail.append(i18n.get("transaction.tax_label")).append(CurrencyUtil.format(transaction.tax.doubleValue())).append("\n");
        detail.append(i18n.get("transaction.paid_amount")).append(CurrencyUtil.format(transaction.finalAmount.doubleValue())).append("\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Label.TRANSACTION_DETAIL));
        alert.setHeaderText(null);
        alert.setContentText(detail.toString());
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    /**
     * 处理创建退货
     */
    @FXML
    public void handleCreateReturn() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.select_return_transaction"));
            return;
        }

        try {
            // 获取交易明细
            List<Product> items = selected.getItems();
            if (items == null || items.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.InventoryAlert.INFO), com.cashier.i18n.I18nManager.getInstance().get("runtime.transaction_detail_missing"));
                return;
            }

            // 加载FXML
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/CreateReturnOrderDialog.fxml");
            Parent root = loader.load();

            // 获取控制器并设置数据
            CreateReturnOrderDialogController controller = loader.getController();
            controller.setOriginalTransaction(selected, items);

            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.setTitle(I18nManager.getInstance().get("runtime.create_return_title", selected.transactionId));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(transactionTable.getScene().getWindow());
            Scene dialogScene = new Scene(root, 1100, 780);
            com.cashier.util.ThemeUtils.applyCurrentTheme(dialogScene, getClass());
            dialogStage.setScene(dialogScene);
            dialogStage.setMinWidth(1000);
            dialogStage.setMinHeight(720);
            dialogStage.setResizable(true);
            controller.setDialogStage(dialogStage);

            // 显示对话框并等待关闭
            dialogStage.showAndWait();

            // 如果提交成功，刷新交易列表
            if (controller.isSubmitted()) {
                showAlert(Alert.AlertType.INFORMATION, I18nManager.getInstance().get(I18nKeys.Label.SUCCESS), I18nManager.getInstance().get("success.create_return"));
                loadTransactions();
            }

        } catch (Exception e) {
            logger.error("打开退货订单对话框失败", e);
            showAlert(Alert.AlertType.ERROR, I18nManager.getInstance().get(I18nKeys.Label.ERROR), I18nManager.getInstance().get("runtime.return_dialog_failed", e.getMessage()));
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    public void handleSearch() {
        applyFilters();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        searchField.clear();
        paymentMethodComboBox.getSelectionModel().select(0);
        quickDateComboBox.setValue("全部报表");
        applyFilters();
    }

    @FXML
    public void handleQuickDateRange() {
        String option = quickDateComboBox.getValue();
        if (option == null || "自定义".equals(option)) {
            return;
        }

        LocalDate today = LocalDate.now();
        switch (option) {
            case "今天" -> setDateRange(today, today);
            case "昨天" -> setDateRange(today.minusDays(1), today.minusDays(1));
            case "本周" -> setDateRange(
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today);
            case "上周" -> {
                LocalDate lastWeekEnd = today.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)).minusDays(1);
                setDateRange(lastWeekEnd.minusDays(6), lastWeekEnd);
            }
            case "本月" -> setDateRange(today.withDayOfMonth(1), today);
            case "上月" -> {
                LocalDate lastMonth = today.minusMonths(1);
                setDateRange(lastMonth.withDayOfMonth(1), lastMonth.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case "全部报表" -> setDateRange(null, null);
            default -> { return; }
        }
        applyFilters();
    }

    private void setDateRange(LocalDate start, LocalDate end) {
        startDatePicker.setValue(start);
        endDatePicker.setValue(end);
    }

    /**
     * 应用筛选条件
     */
    private void applyFilters() {
        try {
            allTransactions = findTransactionsByCurrentDateRange();
        } catch (SQLException e) {
            logger.error("筛选交易记录失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            allTransactions = java.util.List.of();
        }

        String searchText = searchField.getText().trim().toLowerCase();
        String paymentMethod = paymentMethodComboBox.getSelectionModel().getSelectedItem();

        transactionList.setAll(allTransactions.stream()
            .filter(t -> {
                // 日期筛选
                if (startDatePicker.getValue() != null || endDatePicker.getValue() != null) {
                    try {
                        java.time.LocalDate localDate = java.time.LocalDateTime.parse(t.timestamp,
                                com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME)
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

                // 支付方式筛选：下拉框是代码（CASH/WECHAT/...），历史数据存中文，需归一化后比较
                if (!"全部".equals(paymentMethod)) {
                    String selectedMethod = com.cashier.util.I18nUiUtils.canonicalPaymentMethod(paymentMethod);
                    if (!selectedMethod.equals(com.cashier.util.I18nUiUtils.canonicalPaymentMethod(t.paymentMethod))) {
                        return false;
                    }
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
    public void handleExport() {
        if (transactionList.isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_transactions"));
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.EXPORT_FORMAT));
        formatDialog.setHeaderText(I18nManager.getInstance().get(I18nKeys.Label.PLEASE_SELECT_FORMAT));
        formatDialog.setContentText(I18nManager.getInstance().get("label.format") + ":");

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
            java.time.format.DateTimeFormatter sdf = com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME;

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
                    // 尝试将时间戳转换为 DateTime
                    if (t.timestamp != null && !t.timestamp.isEmpty()) {
                        long time = FormValidator.parseLong(t.timestamp);
                        java.time.Instant instant = java.time.Instant.ofEpochMilli(time);
                        timestampStr = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()).format(sdf);
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
                    CurrencyUtil.format(t.totalAmount.doubleValue()),
                    CurrencyUtil.format(t.tax.doubleValue()),
                    CurrencyUtil.format(t.finalAmount.doubleValue()),
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
                com.cashier.util.StatusBarManager.updateSuccess(
                    I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle(I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                successAlert.setHeaderText(null);
                successAlert.setContentText(I18nManager.getInstance().get("success.export_file") + ":\n" + filePath);
                successAlert.showAndWait();
                updateStatus(I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
            } else {
                showError(I18nManager.getInstance().get(I18nKeys.Error.EXPORT_FAILED));
            }
        } catch (Exception e) {
            logger.error("导出交易记录失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_FAILED_DETAIL, e.getMessage()));
        }
    }

    /**
     * 处理刷新
     */
    @FXML
    public void handleRefresh() {
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
        updateStatusForAlert(type, message);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateStatusForAlert(Alert.AlertType type, String message) {
        if (type == Alert.AlertType.ERROR) {
            com.cashier.util.StatusBarManager.updateError(message);
        } else if (type == Alert.AlertType.WARNING) {
            com.cashier.util.StatusBarManager.updateWarning(message);
        } else if (type == Alert.AlertType.INFORMATION) {
            com.cashier.util.StatusBarManager.updateSuccess(message);
        }
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        com.cashier.util.FXUtils.showError(message);
    }

    /**
     * 刷新交易列表
     */
    public void refreshTransactions() {
        loadTransactions();
    }
}
