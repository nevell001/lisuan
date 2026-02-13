package com.cashier.controller;

import com.cashier.CashierSystemFXApplication;
import com.cashier.service.DataService;
import com.cashier.model.User;
import com.cashier.util.FXUtils;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 主控制器
 * 处理主界面的导航和功能
 */
public class MainController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(MainController.class);

    @FXML
    private Label currentUserLabel;

    @FXML
    private Label currentTimeLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label avatarLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label currentShiftLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private TabPane tabPane;

    @FXML
    private Button inventoryBtn;

    @FXML
    private Button cartBtn;

    @FXML
    private Button checkoutBtn;

    @FXML
    private Button transactionsBtn;

    @FXML
    private Button membersBtn;

    @FXML
    private Button supplierBtn;

    @FXML
    private Button purchaseOrderBtn;

    @FXML
    private Button purchaseApprovalBtn;

    @FXML
    private Button purchaseInboundBtn;

    @FXML
    private Button inventoryCheckBtn;

    @FXML
    private Button statisticsBtn;

    @FXML
    private Button purchaseReportBtn;

    @FXML
    private Button inventoryReportBtn;

    @FXML
    private Button profitReportBtn;

    @FXML
    private Button promotionsBtn;

    @FXML
    private Button shiftBtn;

    @FXML
    private Button userManagementBtn;

    @FXML
    private Button settingsBtn;

    private CashierSystemFXApplication application;
    private User currentUser;
    private Timeline timeTimeline;
    private Button activeButton;
    private Map<String, Tab> openTabs = new HashMap<>(); // 管理打开的标签页
    private StackPane loadingOverlay;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 启动时间更新
        startTimeUpdate();

        // 设置初始激活按钮
        activeButton = inventoryBtn;

        // 绑定状态栏到 StatusBarManager
        statusLabel.textProperty().bind(StatusBarManager.statusProperty());

        // 更新状态
        StatusBarManager.updateStatus("就绪");
        updateDate();

        // 创建加载覆盖层
        createLoadingOverlay();

        // 设置快捷键
        setupShortcuts();
    }

    /**
     * 设置快捷键
     */
    private void setupShortcuts() {
        // 获取场景
        if (tabPane.getScene() == null) {
            // 如果场景还未设置，延迟设置快捷键
            tabPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupSceneShortcuts(newScene);
                }
            });
        } else {
            setupSceneShortcuts(tabPane.getScene());
        }
    }

    /**
     * 为场景设置快捷键
     * @param scene 场景
     */
    private void setupSceneShortcuts(javafx.scene.Scene scene) {
        // 功能键
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F1) {
                handleInventory();
                event.consume();
            } else if (event.getCode() == KeyCode.F5) {
                // 刷新当前标签页
                refreshCurrentTab();
                event.consume();
            } else if (event.getCode() == KeyCode.F7) {
                handleMembers();
                event.consume();
            } else if (event.getCode() == KeyCode.F8) {
                handleCheckout();
                event.consume();
            } else if (event.getCode() == KeyCode.F9) {
                handlePromotions();
                event.consume();
            } else if (event.getCode() == KeyCode.F10) {
                showPlaceholder("库存预警", "⚠️", "库存预警功能正在开发中...");
                event.consume();
            } else if (event.getCode() == KeyCode.F11) {
                handleDataBackup();
                event.consume();
            } else if (event.getCode() == KeyCode.F12) {
                handleDataRestore();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                // 清空搜索或关闭当前标签页
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null && !selectedTab.getText().equals("欢迎")) {
                    tabPane.getTabs().remove(selectedTab);
                    openTabs.remove(selectedTab.getText());
                    event.consume();
                }
            }
        });

        // Ctrl 组合键
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.N) {
                    handleInventory();
                    event.consume();
                } else if (event.getCode() == KeyCode.S) {
                    updateStatus("数据已保存");
                    event.consume();
                } else if (event.getCode() == KeyCode.F) {
                    showPlaceholder("搜索", "🔍", "搜索功能正在开发中...");
                    event.consume();
                } else if (event.getCode() == KeyCode.D) {
                    handleExportData();
                    event.consume();
                } else if (event.getCode() == KeyCode.R) {
                    updateStatus("已刷新");
                    event.consume();
                } else if (event.getCode() == KeyCode.Q) {
                    handleExit();
                    event.consume();
                } else if (event.getCode() == KeyCode.A) {
                    // 全选
                    event.consume();
                } else if (event.getCode() == KeyCode.E) {
                    showPlaceholder("编辑", "✏️", "编辑功能正在开发中...");
                    event.consume();
                } else if (event.getCode() == KeyCode.B) {
                    showPlaceholder("批量操作", "📋", "批量操作功能正在开发中...");
                    event.consume();
                } else if (event.getCode() == KeyCode.M) {
                    handleMembers();
                    event.consume();
                } else if (event.getCode() == KeyCode.T) {
                    handleStatistics();
                    event.consume();
                } else if (event.getCode() == KeyCode.DIGIT1) {
                    handleInventory();
                    event.consume();
                } else if (event.getCode() == KeyCode.DIGIT2) {
                    handleCart();
                    event.consume();
                } else if (event.getCode() == KeyCode.DIGIT3) {
                    handleTransactions();
                    event.consume();
                } else if (event.getCode() == KeyCode.DIGIT4) {
                    handleSettings();
                    event.consume();
                }
            }
        });
    }

    /**
     * 设置应用程序引用
     * @param application 应用程序实例
     */
    public void setApplication(CashierSystemFXApplication application) {
        this.application = application;
    }

    /**
         * 设置当前用户
         * @param user 当前用户
         */
        public void setCurrentUser(User user) {
            this.currentUser = user;
            
            // 更新用户信息显示
            currentUserLabel.setText(user.name + " (" + user.getRoleDisplayName() + ")");
            userNameLabel.setText(user.name);
            userRoleLabel.setText(user.getRoleDisplayName());
            
            // 设置头像（显示用户名的首字母）
            if (user.name != null && !user.name.isEmpty()) {
                avatarLabel.setText(user.name.substring(0, 1).toUpperCase());
            }
            
            // 根据角色设置头像颜色
            String role = user.role;
            if ("admin".equals(role)) {
                avatarLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
            } else if ("cashier".equals(role)) {
                avatarLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
            } else if ("finance".equals(role)) {
                avatarLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
            }
        }
    /**
     * 启动时间更新
     */
    private void startTimeUpdate() {
        timeTimeline = new Timeline(new KeyFrame(
            Duration.seconds(1),
            event -> updateTime()
        ));
        timeTimeline.setCycleCount(Animation.INDEFINITE);
        timeTimeline.play();
    }

    /**
     * 更新时间
     */
    private void updateTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        currentTimeLabel.setText(timeFormat.format(new Date()));
    }

    /**
     * 更新日期
     */
    private void updateDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd EEEE");
        dateLabel.setText(dateFormat.format(new Date()));
    }

    /**
     * 更新状态栏
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    /**
     * 切换导航按钮激活状态
     * @param button 要激活的按钮
     */
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        button.getStyleClass().add("nav-button-active");
        activeButton = button;
    }

    // ========== 菜单处理方法 ==========

    @FXML
    private void handleLogout() {
        // 检查是否有活跃班次
        if (com.cashier.service.DataService.hasActiveShift()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认退出");
            alert.setHeaderText(null);
            alert.setContentText("当前有活跃班次未交班！\n\n确定要退出登录吗？\n\n提示：建议先交班后再退出。");
            
            ButtonType yesButton = new ButtonType("先交班", ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType("直接退出", ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
            
            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == yesButton) {
                    // 用户选择先交班，跳转到交班管理页面
                    handleShift();
                } else if (buttonType == noButton) {
                    // 用户选择直接退出
                    if (application != null) {
                        application.logoutToLoginView();
                    }
                }
                // 如果选择取消，不做任何操作
            });
        } else {
            // 没有活跃班次，直接退出
            if (FXUtils.showConfirmAlert("确认退出", "确定要退出登录吗？")) {
                if (application != null) {
                    application.logoutToLoginView();
                }
            }
        }
    }

    @FXML
    private void handleExit() {
        // 检查是否有活跃班次
        if (com.cashier.service.DataService.hasActiveShift()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认退出");
            alert.setHeaderText(null);
            alert.setContentText("当前有活跃班次未交班！\n\n确定要退出系统吗？\n\n提示：建议先交班后再退出。");
            
            ButtonType yesButton = new ButtonType("先交班", ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType("直接退出", ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
            
            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == yesButton) {
                    // 用户选择先交班，跳转到交班管理页面
                    handleShift();
                } else if (buttonType == noButton) {
                    // 用户选择直接退出
                    System.exit(0);
                }
                // 如果选择取消，不做任何操作
            });
        } else {
            // 没有活跃班次，直接退出
            if (FXUtils.showConfirmAlert("确认退出", "确定要退出系统吗？")) {
                System.exit(0);
            }
        }
    }

    @FXML
    private void handleUserManagement() {
        updateStatus("用户管理");
        setActiveButton(userManagementBtn);
        
        try {
            // 加载用户管理界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/UserView.fxml"));
            VBox root = loader.load();
            
            // 获取控制器
            UserController controller = loader.getController();
            
            // 创建内容标签页
            createContentTab("用户管理", root);
            
        } catch (IOException e) {
            showError("加载用户管理界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleDataBackup() {
        updateStatus("数据备份");
        
        try {
            // 创建备份目录
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupPath = "backup_" + timestamp;
            
            // 执行备份
            DataService.backupData(backupPath);
            
            FXUtils.showInfoAlert("备份成功", "数据备份成功！\n备份位置: " + backupPath);
        } catch (Exception e) {
            FXUtils.showErrorAlert("备份失败", "数据备份失败: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDataRestore() {
        updateStatus("数据恢复");
        
        // 列出可用的备份目录
        File projectDir = new File(System.getProperty("user.dir"));
        File[] backupDirs = projectDir.listFiles((dir, name) -> 
            name.startsWith("backup_") && dir.isDirectory()
        );
        
        if (backupDirs == null || backupDirs.length == 0) {
            FXUtils.showErrorAlert("无备份", "未找到任何备份目录！\n请先进行数据备份。");
            return;
        }
        
        // 按修改时间排序，最新的在前
        Arrays.sort(backupDirs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        // 创建选择对话框
        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle("选择备份");
        dialog.setHeaderText("请选择要恢复的备份：");
        dialog.setContentText("可用备份：");
        
        // 添加备份选项
        ObservableList<String> options = FXCollections.observableArrayList();
        for (File dir : backupDirs) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStr = sdf.format(new Date(dir.lastModified()));
            options.add(dir.getName() + " (" + timeStr + ")");
        }
        dialog.getItems().addAll(options);
        
        dialog.showAndWait().ifPresent(selected -> {
            // 提取备份目录名
            String backupDirName = selected.split(" \\(")[0];
            
            try {
                // 确认恢复
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("确认恢复");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("确定要从以下备份恢复数据吗？\n备份: " + backupDirName + "\n\n恢复数据将覆盖当前数据，确定要继续吗？");
                
                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    DataService.restoreData(backupDirName);
                    FXUtils.showInfoAlert("恢复成功", "数据恢复成功！\n请重新登录以加载最新数据。");
                }
            } catch (Exception e) {
                FXUtils.showErrorAlert("恢复失败", "数据恢复失败: " + e.getMessage());
            }
        });
    }    @FXML
    private void handleExportData() {
        updateStatus("导出数据");
        FXUtils.showInfoAlert("开发中", "导出数据功能正在开发中...");
    }

    @FXML
    private void handleLightTheme() {
        if (application != null) {
            application.applyTheme(application.getPrimaryStage().getScene(), "light");
            updateStatus("已切换到浅色主题");
        }
    }

    @FXML
    private void handleDarkTheme() {
        if (application != null) {
            application.applyTheme(application.getPrimaryStage().getScene(), "dark");
            updateStatus("已切换到深色主题");
        }
    }

    @FXML
    private void handleIntelliJTheme() {
        if (application != null) {
            application.applyTheme(application.getPrimaryStage().getScene(), "intellij");
            updateStatus("已切换到 IntelliJ 主题");
        }
    }

    @FXML
    private void handleAbout() {
        updateStatus("关于");
        String about =
            "收银系统 Cashier System\n\n" +
            "版本: 2.3.0 (JavaFX)\n" +
            "开发: nevell\n\n" +
            "技术栈:\n" +
            "- JavaFX 17.0.8\n" +
            "- Maven 3.8+\n" +
            "- JDK 17/21\n\n" +
            "许可证: 木兰宽松许可证 v2 (MulanPSL2)";

        FXUtils.showInfoAlert("关于", about);
    }

    // ========== 导航处理方法 ==========

    @FXML
    private void handleInventory() {
        updateStatus("商品管理");
        setActiveButton(inventoryBtn);
        
        try {
            // 加载商品管理界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/InventoryView.fxml"));
            VBox root = loader.load();
            
            // 获取控制器
            InventoryController controller = loader.getController();
            
            // 创建内容标签页
            createContentTab("商品管理", root);
            
        } catch (IOException e) {
            showError("加载商品管理界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleCart() {
        updateStatus("POS");
        setActiveButton(cartBtn);

        try {
            logger.debug("MainController: 开始加载购物车界面...");
            // 加载购物车界面（购物车和结账已合并）
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/CartView.fxml"));
            logger.debug("MainController: FXML文件路径: {}", getClass().getResource("/com/cashier/view/CartView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            CartController controller = loader.getController();
            logger.debug("MainController: 获取控制器成功");

            // 创建内容标签页
            createContentTab("pos/结账", root);
            logger.debug("MainController: 购物车界面加载成功");

        } catch (IOException e) {
            logger.error("加载购物车界面失败", e);
            showError("加载POS界面失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("加载购物车界面时发生异常", e);
            showError("加载POS界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleCheckout() {
        updateStatus("POS");
        setActiveButton(checkoutBtn);
        
        try {
            // 加载购物车界面（购物车和结账已合并）
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/CartView.fxml"));
            VBox root = loader.load();
            
            // 获取控制器
            CartController controller = loader.getController();
            
            // 创建内容标签页
            createContentTab("pos/结账", root);
            
        } catch (IOException e) {
            showError("加载POS界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleTransactions() {
        updateStatus("交易记录");
        setActiveButton(transactionsBtn);

        try {
            // 加载交易记录界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/TransactionView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            TransactionController controller = loader.getController();

            // 创建内容标签页
            createContentTab("交易记录", root);

        } catch (IOException e) {
            showError("加载交易记录界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleMembers() {
        updateStatus("会员管理");
        setActiveButton(membersBtn);
        
        try {
            // 加载会员管理界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/MemberView.fxml"));
            VBox root = loader.load();
            
            // 获取控制器
            MemberController controller = loader.getController();
            
            // 创建内容标签页
            createContentTab("会员管理", root);
            
        } catch (IOException e) {
            showError("加载会员管理界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleSupplier() {
        updateStatus("供应商管理");
        setActiveButton(supplierBtn);

        try {
            // 加载供应商管理界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/SupplierView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            SupplierController controller = loader.getController();

            // 创建内容标签页
            createContentTab("供应商管理", root);

        } catch (IOException e) {
            showError("加载供应商管理界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handlePurchaseOrder() {
        updateStatus("采购订单");
        setActiveButton(purchaseOrderBtn);

        try {
            // 加载采购订单界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/PurchaseOrderView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            PurchaseOrderController controller = loader.getController();

            // 创建内容标签页
            createContentTab("采购订单", root);

        } catch (IOException e) {
            showError("加载采购订单界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handlePurchaseApproval() {
        updateStatus("采购审批");
        setActiveButton(purchaseApprovalBtn);

        try {
            // 加载采购审批界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/PurchaseApprovalView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            PurchaseApprovalController controller = loader.getController();

            // 创建内容标签页
            createContentTab("采购审批", root);

        } catch (IOException e) {
            showError("加载采购审批界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handlePurchaseInbound() {
        updateStatus("采购入库");
        setActiveButton(purchaseInboundBtn);

        try {
            // 加载采购入库界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/PurchaseInboundView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            PurchaseInboundController controller = loader.getController();

            // 创建内容标签页
            createContentTab("采购入库", root);

        } catch (IOException e) {
            showError("加载采购入库界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleInventoryCheck() {
        updateStatus("库存盘点");
        setActiveButton(inventoryCheckBtn);

        try {
            // 加载库存盘点界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/InventoryCheckView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            InventoryCheckController controller = loader.getController();

            // 创建内容标签页
            createContentTab("库存盘点", root);

        } catch (IOException e) {
            showError("加载库存盘点界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleStatistics() {
        updateStatus("数据统计");
        setActiveButton(statisticsBtn);

        try {
            // 加载数据统计界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/StatisticsView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            StatisticsController controller = loader.getController();

            // 创建内容标签页
            createContentTab("数据统计", root);

        } catch (IOException e) {
            showError("加载数据统计界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handlePurchaseReport() {
        updateStatus("采购报表");
        setActiveButton(purchaseReportBtn);

        try {
            // 加载采购报表界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/PurchaseReportView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            PurchaseReportController controller = loader.getController();

            // 创建内容标签页
            createContentTab("采购报表", root);

        } catch (IOException e) {
            showError("加载采购报表界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleInventoryReport() {
        updateStatus("库存报表");
        setActiveButton(inventoryReportBtn);

        try {
            // 加载库存报表界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/InventoryReportView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            InventoryReportController controller = loader.getController();

            // 创建内容标签页
            createContentTab("库存报表", root);

        } catch (IOException e) {
            showError("加载库存报表界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleProfitReport() {
        updateStatus("利润分析");
        setActiveButton(profitReportBtn);

        try {
            // 加载利润分析界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/ProfitReportView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            ProfitReportController controller = loader.getController();

            // 创建内容标签页
            createContentTab("利润分析", root);

        } catch (IOException e) {
            showError("加载利润分析界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handlePromotions() {
        updateStatus("促销管理");
        setActiveButton(promotionsBtn);

        try {
            // 加载促销管理界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/PromotionView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            PromotionController controller = loader.getController();

            // 创建内容标签页
            createContentTab("促销管理", root);

        } catch (IOException e) {
            showError("加载促销管理界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleShift() {
        updateStatus("交接班");
        setActiveButton(shiftBtn);

        try {
            // 加载交接班界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/ShiftView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            ShiftController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            // 创建内容标签页
            createContentTab("交班管理", root);

        } catch (IOException e) {
            showError("加载交接班界面失败: " + e.getMessage());
        }
    }

    @FXML
    private void handleSettings() {
        updateStatus("系统设置");
        setActiveButton(settingsBtn);

        try {
            // 加载设置界面
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/cashier/view/SettingsView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            SettingsController controller = loader.getController();

            // 创建内容标签页
            createContentTab("系统设置", root);
            
        } catch (IOException e) {
            showError("加载设置界面失败: " + e.getMessage());
        }
    }

    /**
         * 显示占位符内容
         * @param title 标题
         * @param icon 图标
         * @param message 消息
         */
        private void showPlaceholder(String title, String icon, String message) {
            // 检查是否已经打开该标签页
            if (openTabs.containsKey(title)) {
                // 如果已打开，切换到该标签页
                tabPane.getSelectionModel().select(openTabs.get(title));
                return;
            }
    
            // 创建新的标签页
            Tab tab = new Tab(title);
            tab.setClosable(true);
    
            // 创建占位符内容
            VBox placeholder = new VBox(20);
            placeholder.setAlignment(javafx.geometry.Pos.CENTER);
            placeholder.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 40;");
            
            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 64px; -fx-font-family: 'Segoe UI Symbol', 'Microsoft YaHei', sans-serif;");
            
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #3F51B5;");
            
            Label messageLabel = new Label(message);
            messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #757575;");
            
            placeholder.getChildren().addAll(iconLabel, titleLabel, messageLabel);
            
            tab.setContent(placeholder);
    
            // 添加标签页关闭事件
            tab.setOnClosed(event -> {
                openTabs.remove(title);
            });
    
            // 添加到标签页管理器
            openTabs.put(title, tab);
    
            // 添加到TabPane
            tabPane.getTabs().add(tab);
    
            // 切换到新标签页
            tabPane.getSelectionModel().select(tab);
        }
    
        /**
         * 创建内容标签页
         * @param title 标题
         * @param content 内容节点
         */
        private void createContentTab(String title, javafx.scene.Node content) {
            // 检查是否已经打开该标签页
            if (openTabs.containsKey(title)) {
                // 如果已打开，切换到该标签页
                tabPane.getSelectionModel().select(openTabs.get(title));
                return;
            }

            // 创建新的标签页
            Tab tab = new Tab(title);
            tab.setClosable(true);
            tab.setContent(content);

            // 添加标签页关闭事件
            tab.setOnClosed(event -> {
                openTabs.remove(title);
            });

            // 添加到标签页管理器
            openTabs.put(title, tab);

            // 添加到TabPane
            tabPane.getTabs().add(tab);

            // 切换到新标签页
            tabPane.getSelectionModel().select(tab);
        }

    /**
     * 刷新当前标签页
     */
    private void refreshCurrentTab() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null || selectedTab.getText().equals("欢迎")) {
            updateStatus("无需刷新");
            return;
        }

        String title = selectedTab.getText();

        // 关闭当前标签页
        if (openTabs.containsKey(title)) {
            tabPane.getTabs().remove(openTabs.get(title));
            openTabs.remove(title);
        }

        // 根据标题重新打开对应的界面
        switch (title) {
            case "商品管理":
                handleInventory();
                break;
            case "pos/结账":
                handleCheckout();
                break;
            case "交易记录":
                handleTransactions();
                break;
            case "会员管理":
                handleMembers();
                break;
            case "数据统计":
                handleStatistics();
                break;
            case "促销管理":
                handlePromotions();
                break;
            case "交班管理":
                handleShift();
                break;
            case "系统设置":
                handleSettings();
                break;
            case "用户管理":
                handleUserManagement();
                break;
            default:
                updateStatus("无法刷新: " + title);
                return;
        }

        updateStatus("已刷新: " + title);
    }
    
        /**
    
             * 关闭所有标签页（除了欢迎页）
    
             */
    
            private void closeAllTabs() {
    
                // 保留第一个标签页（欢迎页）
    
                while (tabPane.getTabs().size() > 1) {
    
                    Tab tab = tabPane.getTabs().get(tabPane.getTabs().size() - 1);
    
                    openTabs.remove(tab.getText());
    
                    tabPane.getTabs().remove(tab);
    
                }
    
            }
    
        
    
            /**
    
        
    
                 * 创建加载覆盖层
    
        
    
                 */
    
        
    
                private void createLoadingOverlay() {
    
        
    
                    loadingOverlay = new StackPane();
    
        
    
                    loadingOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
    
        
    
            
    
        
    
                    ProgressIndicator progressIndicator = new ProgressIndicator();
    
        
    
                    progressIndicator.setStyle("-fx-progress-color: #3F51B5;");
    
        
    
            
    
        
    
                    Label loadingLabel = new Label("加载中...");
    
        
    
                    loadingLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: bold;");
    
        
    
            
    
        
    
                    VBox vbox = new VBox(10, progressIndicator, loadingLabel);
    
        
    
                    vbox.setAlignment(javafx.geometry.Pos.CENTER);
    
        
    
                    vbox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 10; -fx-padding: 20;");
    
        
    
            
    
        
    
                    loadingOverlay.getChildren().add(vbox);
    
        
    
                    loadingOverlay.setVisible(false);
    
        
    
                    loadingOverlay.setMouseTransparent(true);
    
        
    
                }
    
        
    
            
    
        
    
                /**
    
        
    
                 * 显示加载动画
    
        
    
                 */
    
        
    
                private void showLoading() {
    
        
    
                    loadingOverlay.setVisible(true);
    
        
    
                    loadingOverlay.setMouseTransparent(false);
    
        
    
                }
    
        
    
            
    
        
    
                /**
    
        
    
            
    
        
    
                     * 隐藏加载动画
    
        
    
            
    
        
    
                     */
    
        
    
            
    
        
    
                    private void hideLoading() {
    
        
    
            
    
        
    
                        loadingOverlay.setVisible(false);
    
        
    
            
    
        
    
                        loadingOverlay.setMouseTransparent(true);
    
        
    
            
    
        
    
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
     * 异步加载内容
     * @param title 标题
     * @param icon 图标
     * @param message 消息
     */
    private void showPlaceholderAsync(String title, String icon, String message) {
        showLoading();

        // 使用 JavaFX PauseTransition 替代 Thread.sleep
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        pause.setOnFinished(event -> {
            showPlaceholder(title, icon, message);
            hideLoading();
        });
        pause.play();
    }
}