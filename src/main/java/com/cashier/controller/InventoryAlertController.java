package com.cashier.controller;

import com.cashier.dao.ProductDAO;
import com.cashier.model.Product;
import com.cashier.service.InventoryAlertService;
import com.cashier.util.ExportUtil;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FXUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 库存预警控制器
 * 显示和管理库存预警信息
 */
public class InventoryAlertController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InventoryAlertController.class);

    @FXML
    private TableView<AlertItem> alertTable;

    @FXML
    private TableColumn<AlertItem, String> nameColumn;

    @FXML
    private TableColumn<AlertItem, String> productCodeColumn;

    @FXML
    private TableColumn<AlertItem, String> currentStockColumn;

    @FXML
    private TableColumn<AlertItem, String> minStockColumn;

    @FXML
    private TableColumn<AlertItem, String> unitColumn;

    @FXML
    private TableColumn<AlertItem, String> alertLevelColumn;

    @FXML
    private TableColumn<AlertItem, String> lastAlertTimeColumn;

    @FXML
    private Label serviceStatusLabel;

    @FXML
    private Circle statusIndicator;

    @FXML
    private Label lastCheckTimeLabel;

    @FXML
    private Label checkIntervalLabel;

    @FXML
    private Label alertCooldownLabel;

    @FXML
    private Label alertCountLabel;

    @FXML
    private Label criticalCountLabel;

    @FXML
    private Label warningCountLabel;

    @FXML
    private Label infoCountLabel;

    @FXML
    private Button refreshButton;

    @FXML
    private Button clearCooldownButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button closeButton;

    private InventoryAlertService alertService;
    private ObservableList<AlertItem> alertList;
    private Timer updateTimer;
    private SimpleDateFormat dateFormat;

    /**
     * 库存预警数据项
     */
    public static class AlertItem {
        private final SimpleStringProperty name;
        private final SimpleStringProperty productCode;
        private final SimpleStringProperty currentStock;
        private final SimpleStringProperty minStock;
        private final SimpleStringProperty unit;
        private final SimpleStringProperty alertLevel;
        private final SimpleStringProperty lastAlertTime;
        private final Product product;
        private final AlertLevel level;

        public AlertItem(Product product) {
            this.product = product;
            this.name = new SimpleStringProperty(product.name);
            this.productCode = new SimpleStringProperty(product.productCode != null ? product.productCode : "无");
            this.currentStock = new SimpleStringProperty(String.valueOf(product.quantity));
            this.minStock = new SimpleStringProperty(String.valueOf(product.minStock));
            this.unit = new SimpleStringProperty(product.unit != null ? product.unit : "个");
            this.level = calculateAlertLevel(product);
            this.alertLevel = new SimpleStringProperty(level.getDisplayName());
            this.lastAlertTime = new SimpleStringProperty("--");
        }

        private AlertLevel calculateAlertLevel(Product product) {
            if (product.quantity == 0) {
                return AlertLevel.CRITICAL;
            } else if (product.quantity < product.minStock / 2) {
                return AlertLevel.WARNING;
            } else {
                return AlertLevel.INFO;
            }
        }

        public Product getProduct() {
            return product;
        }

        public AlertLevel getLevel() {
            return level;
        }

        public void setLastAlertTime(Date time) {
            if (time != null) {
                this.lastAlertTime.set(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time));
            }
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty productCodeProperty() {
            return productCode;
        }

        public SimpleStringProperty currentStockProperty() {
            return currentStock;
        }

        public SimpleStringProperty minStockProperty() {
            return minStock;
        }

        public SimpleStringProperty unitProperty() {
            return unit;
        }

        public SimpleStringProperty alertLevelProperty() {
            return alertLevel;
        }

        public SimpleStringProperty lastAlertTimeProperty() {
            return lastAlertTime;
        }
    }

    /**
     * 预警级别枚举
     */
    public enum AlertLevel {
        CRITICAL("严重警告", "#D32F2F"),
        WARNING("警告", "#FF9800"),
        INFO("提示", "#2196F3");

        private final String displayName;
        private final String color;

        AlertLevel(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        logger.info("InventoryAlertController: 初始化库存预警界面...");
        alertService = InventoryAlertService.getInstance();
        alertList = FXCollections.observableArrayList();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 设置表格列
        setupTableColumns();

        // 设置表格数据
        alertTable.setItems(alertList);

        // 更新服务状态
        updateServiceStatus();

        // 启动定时更新
        startUpdateTimer();

        logger.info("InventoryAlertController: 库存预警界面初始化完成");
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        productCodeColumn.setCellValueFactory(cellData -> cellData.getValue().productCodeProperty());
        currentStockColumn.setCellValueFactory(cellData -> cellData.getValue().currentStockProperty());
        minStockColumn.setCellValueFactory(cellData -> cellData.getValue().minStockProperty());
        unitColumn.setCellValueFactory(cellData -> cellData.getValue().unitProperty());

        // 预警级别列带颜色
        alertLevelColumn.setCellValueFactory(cellData -> cellData.getValue().alertLevelProperty());
        alertLevelColumn.setCellFactory(column -> new TableCell<AlertItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    AlertItem alertItem = getTableView().getItems().get(getIndex());
                    if (alertItem != null) {
                        setStyle("-fx-text-fill: " + alertItem.getLevel().getColor() + "; -fx-font-weight: bold;");
                    }
                }
            }
        });

        lastAlertTimeColumn.setCellValueFactory(cellData -> cellData.getValue().lastAlertTimeProperty());
    }

    /**
     * 启动定时更新
     */
    private void startUpdateTimer() {
        updateTimer = new Timer("AlertViewUpdateTimer", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> {
                    updateServiceStatus();
                    loadAlertItems();
                });
            }
        }, 0, 5000); // 每5秒更新一次
    }

    /**
     * 更新服务状态
     */
    private void updateServiceStatus() {
        boolean isRunning = alertService.isRunning();

        if (isRunning) {
            serviceStatusLabel.setText("运行中");
            serviceStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
            statusIndicator.setFill(Color.web("#4CAF50"));
        } else {
            serviceStatusLabel.setText("未启动");
            serviceStatusLabel.setStyle("-fx-text-fill: #F44336;");
            statusIndicator.setFill(Color.web("#F44336"));
        }

        // 更新配置信息
        long intervalMs = alertService.getCheckInterval();
        long cooldownMs = alertService.getAlertCooldown();
        long lastCheckTime = alertService.getLastCheckTime();

        checkIntervalLabel.setText(formatDuration(intervalMs));
        alertCooldownLabel.setText(formatDuration(cooldownMs));

        if (lastCheckTime > 0) {
            lastCheckTimeLabel.setText("上次检查: " + dateFormat.format(new Date(lastCheckTime)));
        } else {
            lastCheckTimeLabel.setText("上次检查: 未检查");
        }
    }

    /**
     * 加载预警商品
     */
    private void loadAlertItems() {
        try {
            List<Product> allProducts = ProductDAO.findAll();
            if (allProducts == null) {
                allProducts = new ArrayList<>();
            }

            List<AlertItem> alertItems = new ArrayList<>();
            int criticalCount = 0;
            int warningCount = 0;
            int infoCount = 0;

            for (Product product : allProducts) {
                // 跳过没有设置最低库存的商品
                if (product.minStock <= 0) {
                    continue;
                }

                // 检查库存是否低于最低库存
                if (product.quantity <= product.minStock) {
                    AlertItem alertItem = new AlertItem(product);
                    alertItems.add(alertItem);

                    // 统计预警级别
                    switch (alertItem.getLevel()) {
                        case CRITICAL:
                            criticalCount++;
                            break;
                        case WARNING:
                            warningCount++;
                            break;
                        case INFO:
                            infoCount++;
                            break;
                    }
                }
            }

            // 更新列表
            alertList.clear();
            alertList.addAll(alertItems);

            // 更新统计信息
            alertCountLabel.setText(String.valueOf(alertItems.size()));
            criticalCountLabel.setText(String.valueOf(criticalCount));
            warningCountLabel.setText(String.valueOf(warningCount));
            infoCountLabel.setText(String.valueOf(infoCount));

        } catch (Exception e) {
            logger.error("加载预警商品失败", e);
        }
    }

    /**
     * 格式化持续时间
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + "小时";
        } else if (minutes > 0) {
            return minutes + "分钟";
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 处理立即检查
     */
    @FXML
    private void handleRefresh() {
        logger.info("手动触发库存预警检查");
        alertService.triggerCheck();
        updateServiceStatus();
        loadAlertItems();
        FXUtils.showInfoAlert("检查完成", "库存预警检查已完成！");
    }

    /**
     * 处理清除所有冷却
     */
    @FXML
    private void handleClearCooldown() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认清除");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确定要清除所有预警商品的冷却时间吗？\n\n清除后，所有库存不足的商品将再次发送预警通知。");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            logger.info("清除所有预警冷却");
            alertService.clearAllAlertCooldowns();
            FXUtils.showInfoAlert("清除成功", "所有预警冷却时间已清除！");
        }
    }

    /**
     * 处理导出预警
     */
    @FXML
    private void handleExport() {
        if (alertList.isEmpty()) {
            FXUtils.showErrorAlert("导出失败", "当前没有预警商品可导出！");
            return;
        }

        try {
            List<String> headers = Arrays.asList(
                "商品名称", "商品编号", "当前库存", "最低库存", "单位", "预警级别"
            );

            List<String[]> data = new ArrayList<>();
            for (AlertItem item : alertList) {
                data.add(new String[]{
                    item.getProduct().name,
                    item.getProduct().productCode != null ? item.getProduct().productCode : "无",
                    String.valueOf(item.getProduct().quantity),
                    String.valueOf(item.getProduct().minStock),
                    item.getProduct().unit != null ? item.getProduct().unit : "个",
                    item.getLevel().getDisplayName()
                });
            }

            ExportUtil.export("库存预警报告", headers, data,
                com.cashier.util.ExportUtil.ExportFormat.EXCEL, "reports");
            FXUtils.showInfoAlert("导出成功", "库存预警报告已导出到 reports 目录！");

        } catch (Exception e) {
            logger.error("导出预警报告失败", e);
            FXUtils.showErrorAlert("导出失败", "导出预警报告失败：" + e.getMessage());
        }
    }

    /**
     * 处理关闭
     */
    @FXML
    private void handleClose() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }

        // 获取当前窗口
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }
}
