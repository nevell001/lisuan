package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.i18n.I18nManager;
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
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.*;

/**
 * 库存预警控制器
 * 显示和管理库存预警信息
 */
public class InventoryAlertController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InventoryAlertController.class);
    private static final String STATUS_RUNNING_STYLE = "status-running";
    private static final String STATUS_STOPPED_STYLE = "status-stopped";
    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();

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
    private ObservableList<AlertItem> alertList = javafx.collections.FXCollections.observableArrayList();
    private Timer updateTimer;

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
            this.productCode = new SimpleStringProperty(product.productCode != null
                ? product.productCode : I18nManager.getInstance().get(I18nKeys.Common.NONE));
            this.currentStock = new SimpleStringProperty(String.valueOf(product.quantity));
            this.minStock = new SimpleStringProperty(String.valueOf(product.minStock));
            this.unit = new SimpleStringProperty(product.unit != null
                ? product.unit : I18nManager.getInstance().get("common.unit_default"));
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
            this.lastAlertTime.set(com.cashier.util.DateTimeFormats.formatStandard(
                time.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
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
        CRITICAL("alert-level-critical"),
        WARNING("alert-level-warning"),
        INFO("alert-level-info");

        private final String styleClass;

        AlertLevel(String styleClass) {
            this.styleClass = styleClass;
        }

        /**
         * 展示名按**当前语言**解析：枚举常量在类加载时就固定，把中文写进常量会让这一列
         * 切到 en/zh_TW 后永远是中文（2026-10 F14 迁移）。
         */
        public String getDisplayName() {
            return switch (this) {
                case CRITICAL -> I18nManager.getInstance().get("inventory_alert.critical");
                case WARNING -> I18nManager.getInstance().get("inventory_alert.warning");
                case INFO -> I18nManager.getInstance().get("inventory_alert.info");
            };
        }

        public String getStyleClass() {
            return styleClass;
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
                getStyleClass().removeAll("alert-level-critical", "alert-level-warning", "alert-level-info");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    AlertItem alertItem = getTableView().getItems().get(getIndex());
                    if (alertItem != null) {
                        getStyleClass().add(alertItem.getLevel().getStyleClass());
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
                try {
                    // DB 查询在 Timer（daemon）线程执行，避免每 5 秒在 FX 线程做全表预警查询；
                    // 仅 UI 更新通过 runLater 回到 FX 线程。
                    final List<Product> alertProducts = productDAO.findProductsRequiringStockAlert();
                    javafx.application.Platform.runLater(() -> {
                        updateServiceStatus();
                        renderAlertList(alertProducts);
                    });
                } catch (Exception e) {
                    // 轮询失败不打断界面，仅记录日志
                    logger.error("定时刷新库存预警失败", e);
                }
            }
        }, 0, 5000); // 每5秒更新一次
    }

    /**
     * 更新服务状态
     */
    private void updateServiceStatus() {
        boolean isRunning = alertService.isRunning();

        if (isRunning) {
            serviceStatusLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("runtime.service_running"));
            serviceStatusLabel.getStyleClass().removeAll("text-success", "text-danger");
            serviceStatusLabel.getStyleClass().add("text-success");
            updateStatusIndicator(STATUS_RUNNING_STYLE);
        } else {
            serviceStatusLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("inventory_alert.service_not_started"));
            serviceStatusLabel.getStyleClass().removeAll("text-success", "text-danger");
            serviceStatusLabel.getStyleClass().add("text-danger");
            updateStatusIndicator(STATUS_STOPPED_STYLE);
        }

        // 更新配置信息
        long intervalMs = alertService.getCheckInterval();
        long cooldownMs = alertService.getAlertCooldown();
        long lastCheckTime = alertService.getLastCheckTime();

        checkIntervalLabel.setText(formatDuration(intervalMs));
        alertCooldownLabel.setText(formatDuration(cooldownMs));

        if (lastCheckTime > 0) {
            lastCheckTimeLabel.setText(I18nManager.getInstance().get("runtime.last_check",
                com.cashier.util.DateTimeFormats.formatStandard(
                    new java.util.Date(lastCheckTime).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())));
        } else {
            lastCheckTimeLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("inventory_alert.last_check"));
        }
    }

    private void updateStatusIndicator(String statusStyle) {
        statusIndicator.getStyleClass().removeAll(STATUS_RUNNING_STYLE, STATUS_STOPPED_STYLE);
        statusIndicator.getStyleClass().add(statusStyle);
    }

    /**
     * 加载预警商品（打开窗口/手动刷新时调用；DB 查询放到后台线程，UI 更新回 FX）
     */
    private void loadAlertItems() {
        loadAlertItems(null);
    }

    /**
     * 加载预警商品列表（TD-035）。
     *
     * <p>此前刷新失败只记日志、表格静默保留旧数据，用户以为"检查过了没问题"；
     * 现在把结果回传（{@code onDone} 收到 true/false），调用方据此决定提示什么。</p>
     */
    private void loadAlertItems(java.util.function.Consumer<Boolean> onDone) {
        Thread worker = new Thread(() -> {
            boolean ok = true;
            List<Product> alertProducts = null;
            try {
                alertProducts = productDAO.findProductsRequiringStockAlert();
            } catch (SQLException e) {
                logger.error("从数据库加载商品失败", e);
                ok = false;
            } catch (Exception e) {
                logger.error("加载预警商品失败", e);
                ok = false;
            }
            final boolean success = ok;
            final List<Product> products = alertProducts;
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    renderAlertList(products);
                }
                if (onDone != null) {
                    onDone.accept(success);
                }
            });
        }, "inventory-alert-load");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 把预警商品列表渲染到表格与统计标签（必须在 FX 线程调用）
     */
    private void renderAlertList(List<Product> alertProducts) {
        if (alertList == null) {
            return;
        }
        List<AlertItem> alertItems = new ArrayList<>();
        int criticalCount = 0;
        int warningCount = 0;
        int infoCount = 0;

        for (Product product : alertProducts) {
            AlertItem alertItem = new AlertItem(product);
            alertItems.add(alertItem);

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
                default:
                    logger.warn("未知库存预警级别: {}", alertItem.getLevel());
                    break;
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
    }

    /**
     * 格式化持续时间
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return I18nManager.getInstance().get("inventory_alert.duration_hours", hours);
        } else if (minutes > 0) {
            return I18nManager.getInstance().get("inventory_alert.duration_minutes", minutes);
        } else {
            return I18nManager.getInstance().get("inventory_alert.duration_seconds", seconds);
        }
    }

    /**
     * 处理立即检查
     */
    @FXML
    public void handleRefresh() {
        logger.info("手动触发库存预警检查");
        alertService.triggerCheck();
        updateServiceStatus();
        // TD-035：提示必须在刷新**成功**之后——此前先弹"检查完成"再异步刷新（且失败静默），
        // 用户会看到"检查完成"而表格其实没更新
        loadAlertItems(success -> {
            if (success) {
                FXUtils.showInfoAlert(
                    I18nManager.getInstance().get("inventory_alert.check_done_title"),
                    I18nManager.getInstance().get("inventory_alert.check_done_message"));
            } else {
                FXUtils.showErrorAlert(
                    I18nManager.getInstance().get("inventory_alert.check_done_title"),
                    I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA));
            }
        });
    }

    /**
     * 处理清除所有冷却
     */
    @FXML
    public void handleClearCooldown() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.confirm_clear"));
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.alert_clear_confirm"));

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            logger.info("清除所有预警冷却");
            alertService.clearAllAlertCooldowns();
            FXUtils.showInfoAlert(
                I18nManager.getInstance().get("inventory_alert.clear_done_title"),
                I18nManager.getInstance().get("inventory_alert.clear_done_message"));
        }
    }

    /**
     * 处理导出预警
     */
    @FXML
    public void handleExport() {
        if (alertList.isEmpty()) {
            FXUtils.showErrorAlert(I18nManager.getInstance().get(I18nKeys.Error.EXPORT_DATA),
                I18nManager.getInstance().get("runtime.no_export_inventory_alerts"));
            return;
        }

        try {
            // 表头复用 FXML 列标题的同一批 key，保证界面与导出文件用词一致
            List<String> headers = Arrays.asList(
                I18nManager.getInstance().get("inventory_alert.product_name"),
                I18nManager.getInstance().get("inventory_alert.product_code"),
                I18nManager.getInstance().get("inventory_alert.current_stock"),
                I18nManager.getInstance().get("inventory_alert.min_stock"),
                I18nManager.getInstance().get("inventory_alert.unit"),
                I18nManager.getInstance().get("inventory_alert.alert_level")
            );

            List<String[]> data = new ArrayList<>();
            for (AlertItem item : alertList) {
                data.add(new String[]{
                    item.getProduct().name,
                    item.getProduct().productCode != null
                        ? item.getProduct().productCode : I18nManager.getInstance().get(I18nKeys.Common.NONE),
                    String.valueOf(item.getProduct().quantity),
                    String.valueOf(item.getProduct().minStock),
                    item.getProduct().unit != null
                        ? item.getProduct().unit : I18nManager.getInstance().get("common.unit_default"),
                    item.getLevel().getDisplayName()
                });
            }

            ExportUtil.export(I18nManager.getInstance().get("inventory_alert.export_file_name"), headers, data,
                com.cashier.util.ExportUtil.ExportFormat.EXCEL, "reports");
            FXUtils.showInfoAlert(I18nManager.getInstance().get(I18nKeys.Success.EXPORT),
                I18nManager.getInstance().get("inventory_alert.export_success_message"));

        } catch (Exception e) {
            logger.error("导出预警报告失败", e);
            FXUtils.showErrorAlert(I18nManager.getInstance().get(I18nKeys.Error.EXPORT_DATA),
                I18nManager.getInstance().get("inventory_alert.export_failed", e.getMessage()));
        }
    }

    /**
     * 处理关闭
     */
    @FXML
    public void handleClose() {
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
