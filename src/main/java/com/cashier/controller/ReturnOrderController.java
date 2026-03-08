package com.cashier.controller;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.service.ReturnService;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.ReceiptPrinter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 退货订单管理控制器
 */
public class ReturnOrderController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnOrderController.class);

    @FXML private TableView<ReturnOrder> returnOrderTable;
    @FXML private TableColumn<ReturnOrder, String> returnOrderIdColumn;
    @FXML private TableColumn<ReturnOrder, String> memberNameColumn;
    @FXML private TableColumn<ReturnOrder, String> returnDateColumn;
    @FXML private TableColumn<ReturnOrder, String> totalAmountColumn;
    @FXML private TableColumn<ReturnOrder, String> statusColumn;
    @FXML private TableColumn<ReturnOrder, String> operatorNameColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Label returnOrderIdLabel;
    @FXML private Label memberNameLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label statusLabel;
    @FXML private Label operatorNameLabel;
    @FXML private Label returnDateLabel;
    @FXML private TextArea returnReasonTextArea;
    @FXML private TextArea notesTextArea;

    @FXML private TableView<ReturnOrderItem> itemTable;
    @FXML private TableColumn<ReturnOrderItem, String> productCodeColumn;
    @FXML private TableColumn<ReturnOrderItem, String> productNameColumn;
    @FXML private TableColumn<ReturnOrderItem, Integer> returnQuantityColumn;
    @FXML private TableColumn<ReturnOrderItem, Double> unitPriceColumn;
    @FXML private TableColumn<ReturnOrderItem, Double> returnAmountColumn;
    @FXML private TableColumn<ReturnOrderItem, String> conditionColumn;

    private ObservableList<ReturnOrder> returnOrderList = FXCollections.observableArrayList();
    private ObservableList<ReturnOrderItem> itemList = FXCollections.observableArrayList();
    private ReturnOrder selectedReturnOrder;

    @FXML
    public void initialize() {
        logger.info("初始化退货订单管理控制器");

        // 初始化状态过滤器
        statusFilter.setItems(FXCollections.observableArrayList(
            "全部", "待审批", "已批准", "已拒绝", "已完成"
        ));
        statusFilter.setValue("全部");

        // 初始化表格列
        initializeReturnOrderTable();
        initializeItemTable();

        // 加载退货订单数据
        loadReturnOrders();

        // 设置表格选择监听
        returnOrderTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                selectedReturnOrder = newVal;
                showReturnOrderDetail(newVal);
            }
        );
    }

    private void initializeReturnOrderTable() {
        returnOrderIdColumn.setCellValueFactory(new PropertyValueFactory<>("returnOrderId"));
        memberNameColumn.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        returnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDateFormatted"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmountFormatted"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        operatorNameColumn.setCellValueFactory(new PropertyValueFactory<>("operatorName"));

        // 自定义显示格式
        returnDateColumn.setCellFactory(column -> new TableCell<ReturnOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    try {
                        Date date = new Date(Long.parseLong(item));
                        setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
                    } catch (Exception e) {
                        setText(item);
                    }
                }
            }
        });

        totalAmountColumn.setCellFactory(column -> new TableCell<ReturnOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    try {
                        double amount = Double.parseDouble(item);
                        setText(String.format("¥%.2f", amount));
                    } catch (Exception e) {
                        setText(item);
                    }
                }
            }
        });

        statusColumn.setCellFactory(column -> new TableCell<ReturnOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "PENDING":
                            setText("待审批");
                            setStyle("-fx-text-fill: orange;");
                            break;
                        case "APPROVED":
                            setText("已批准");
                            setStyle("-fx-text-fill: green;");
                            break;
                        case "REJECTED":
                            setText("已拒绝");
                            setStyle("-fx-text-fill: red;");
                            break;
                        case "COMPLETED":
                            setText("已完成");
                            setStyle("-fx-text-fill: blue;");
                            break;
                        default:
                            setText(item);
                    }
                }
            }
        });

        returnOrderTable.setItems(returnOrderList);
    }

    private void initializeItemTable() {
        productCodeColumn.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        returnQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("returnQuantity"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        returnAmountColumn.setCellValueFactory(new PropertyValueFactory<>("returnAmount"));
        conditionColumn.setCellValueFactory(new PropertyValueFactory<>("condition"));

        // 自定义显示格式
        unitPriceColumn.setCellFactory(column -> new TableCell<ReturnOrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("¥%.2f", item));
                }
            }
        });

        returnAmountColumn.setCellFactory(column -> new TableCell<ReturnOrderItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("¥%.2f", item));
                }
            }
        });

        conditionColumn.setCellFactory(column -> new TableCell<ReturnOrderItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "GOOD":
                            setText("完好");
                            break;
                        case "DAMAGED":
                            setText("损坏");
                            setStyle("-fx-text-fill: red;");
                            break;
                        case "OPENED":
                            setText("已拆封");
                            setStyle("-fx-text-fill: orange;");
                            break;
                        default:
                            setText(item);
                    }
                }
            }
        });

        itemTable.setItems(itemList);
    }

    private void loadReturnOrders() {
        returnOrderList.clear();
        List<ReturnOrder> orders = ReturnOrderDAO.findAll();
        returnOrderList.addAll(orders);
        logger.info("加载了 {} 条退货订单记录", orders.size());
    }

    private void showReturnOrderDetail(ReturnOrder returnOrder) {
        if (returnOrder == null) {
            clearDetail();
            return;
        }

        returnOrderIdLabel.setText(returnOrder.returnOrderId);
        memberNameLabel.setText(returnOrder.memberName != null ? returnOrder.memberName : "无");
        totalAmountLabel.setText(String.format("¥%.2f", returnOrder.totalAmount));
        statusLabel.setText(returnOrder.getStatusText());
        operatorNameLabel.setText(returnOrder.operatorName);
        returnDateLabel.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(returnOrder.returnDate));
        returnReasonTextArea.setText(returnOrder.returnReason != null ? returnOrder.returnReason : "");
        notesTextArea.setText(returnOrder.notes != null ? returnOrder.notes : "");

        // 加载退货明细
        loadReturnOrderItems(returnOrder.returnOrderId);
    }

    private void loadReturnOrderItems(String returnOrderId) {
        itemList.clear();
        List<ReturnOrderItem> items = ReturnOrderItemDAO.findByReturnOrderId(returnOrderId);
        itemList.addAll(items);
    }

    private void clearDetail() {
        returnOrderIdLabel.setText("");
        memberNameLabel.setText("");
        totalAmountLabel.setText("");
        statusLabel.setText("");
        operatorNameLabel.setText("");
        returnDateLabel.setText("");
        returnReasonTextArea.setText("");
        notesTextArea.setText("");
        itemList.clear();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        String status = statusFilter.getValue();

        returnOrderList.clear();

        if ("全部".equals(status)) {
            returnOrderList.addAll(ReturnOrderDAO.findAll());
        } else {
            String statusCode = "";
            switch (status) {
                case "待审批": statusCode = "PENDING"; break;
                case "已批准": statusCode = "APPROVED"; break;
                case "已拒绝": statusCode = "REJECTED"; break;
                case "已完成": statusCode = "COMPLETED"; break;
            }
            returnOrderList.addAll(ReturnOrderDAO.findByStatus(statusCode));
        }

        // 搜索过滤
        if (!keyword.isEmpty()) {
            returnOrderList.removeIf(order -> 
                !order.returnOrderId.toLowerCase().contains(keyword.toLowerCase()) &&
                (order.memberName == null || !order.memberName.toLowerCase().contains(keyword.toLowerCase()))
            );
        }

        logger.info("搜索结果: {} 条记录", returnOrderList.size());
    }

    @FXML
    private void handleRefresh() {
        loadReturnOrders();
        clearDetail();
        searchField.clear();
        statusFilter.setValue("全部");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        logger.info("刷新退货订单列表");
    }

    @FXML
    private void handleCreateReturn() {
        // 显示一个简单的对话框提示用户使用交易记录创建退货
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("创建退货订单");
        alert.setHeaderText("功能说明");
        alert.setContentText("请在交易记录页面选择需要退货的交易，然后点击创建退货按钮。\n\n" +
                           "创建退货订单后，系统会自动生成退货申请，需要经过审批才能完成退货。");
        alert.showAndWait();
    }

    @FXML
    private void handleExport() {
        if (returnOrderList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "没有可导出的数据");
            return;
        }

        // 准备导出数据
        List<String> headers = List.of("退货单号", "会员名称", "退货日期", "退货金额", "状态", "操作员", "退货原因");
        List<String[]> data = new java.util.ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (ReturnOrder order : returnOrderList) {
            data.add(new String[]{
                order.returnOrderId,
                order.memberName != null ? order.memberName : "无",
                sdf.format(order.returnDate),
                String.format("¥%.2f", order.totalAmount),
                order.getStatusText(),
                order.operatorName,
                order.returnReason != null ? order.returnReason : ""
            });
        }

        // 调用导出
        String filePath = com.cashier.util.ExportUtil.export(
            "退货订单报表",
            headers,
            data,
            com.cashier.util.ExportUtil.ExportFormat.EXCEL,
            "退货订单"
        );

        if (filePath != null) {
            showAlert(Alert.AlertType.INFORMATION, "导出成功", "文件已导出到:\n" + filePath);
            logger.info("退货订单导出成功: {}", filePath);
        } else {
            showAlert(Alert.AlertType.ERROR, "导出失败", "导出失败，请查看日志");
        }
    }

    @FXML
    private void handleViewOriginalTransaction() {
        if (selectedReturnOrder == null || selectedReturnOrder.originalTransactionId == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择退货订单");
            return;
        }

        // 跳转到交易详情页面
        // TODO: 实现跳转逻辑
        showAlert(Alert.AlertType.INFORMATION, "提示", "查看原交易: " + selectedReturnOrder.originalTransactionId);
    }

    @FXML
    private void handlePrintReturnReceipt() {
        if (selectedReturnOrder == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择退货订单");
            return;
        }

        // 获取退货商品明细
        List<ReturnOrderItem> returnItems = ReturnOrderItemDAO.findByReturnOrderId(selectedReturnOrder.returnOrderId);
        
        if (returnItems == null || returnItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "退货商品明细为空，无法打印");
            return;
        }

        try {
            // 打印退货单据
            String filePath = ReceiptPrinter.printReturnReceipt(selectedReturnOrder, returnItems);
            
            if (filePath != null) {
                showAlert(Alert.AlertType.INFORMATION, "打印成功", 
                    "退货单据已打印！\n\n" +
                    "退货单号: " + selectedReturnOrder.returnOrderId + "\n" +
                    "文件路径: " + filePath);
                logger.info("退货单据打印成功: {}, 文件路径: {}", selectedReturnOrder.returnOrderId, filePath);
            } else {
                showAlert(Alert.AlertType.ERROR, "打印失败", "打印退货单据失败，请查看日志");
                logger.error("退货单据打印失败: {}", selectedReturnOrder.returnOrderId);
            }
        } catch (Exception e) {
            logger.error("打印退货单据时发生错误", e);
            showAlert(Alert.AlertType.ERROR, "打印失败", "打印退货单据时发生错误:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleCompleteReturn() {
        if (selectedReturnOrder == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择退货订单");
            return;
        }

        // 只有已批准的退货单才能完成
        if (!"APPROVED".equals(selectedReturnOrder.status)) {
            showAlert(Alert.AlertType.WARNING, "提示", "只有已批准的退货订单才能完成");
            return;
        }

        // 确认完成
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认完成退货");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(String.format(
            "确认完成此退货订单吗？\n\n" +
            "退货单号: %s\n" +
            "退货金额: ¥%.2f\n" +
            "会员: %s\n\n" +
            "完成后将恢复商品库存并退款到会员账户。",
            selectedReturnOrder.returnOrderId,
            selectedReturnOrder.totalAmount,
            selectedReturnOrder.memberName != null ? selectedReturnOrder.memberName : "无"
        ));

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        // 完成退货订单
        boolean result = ReturnService.completeReturnOrder(selectedReturnOrder.returnOrderId);

        if (result) {
            showAlert(Alert.AlertType.INFORMATION, "成功", 
                "退货订单已完成！\n\n" +
                "退货单号: " + selectedReturnOrder.returnOrderId + "\n" +
                "退货金额: ¥" + String.format("%.2f", selectedReturnOrder.totalAmount));
            
            // 刷新列表
            loadReturnOrders();
            clearDetail();
            logger.info("退货订单完成: {}", selectedReturnOrder.returnOrderId);
        } else {
            showAlert(Alert.AlertType.ERROR, "失败", "完成退货订单失败，请查看日志");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
