package com.cashier.controller;

import com.cashier.CashierSystemFXApplication;
import com.cashier.model.User;
import com.cashier.util.FXUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 主控制器
 * 处理主界面的导航和功能
 */
public class MainController {

    @FXML
    private Label currentUserLabel;

    @FXML
    private Label currentTimeLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label currentShiftLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private StackPane contentPane;

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
    private Button statisticsBtn;

    @FXML
    private Button promotionsBtn;

    @FXML
    private Button shiftBtn;

    @FXML
    private Button settingsBtn;

    private CashierSystemFXApplication application;
    private User currentUser;
    private Timeline timeTimeline;
    private Button activeButton;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 启动时间更新
        startTimeUpdate();

        // 设置初始激活按钮
        activeButton = inventoryBtn;

        // 更新状态
        updateStatus("就绪");
        updateDate();
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
        statusLabel.setText(status);
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
        if (FXUtils.showConfirmAlert("确认退出", "确定要退出登录吗？")) {
            // TODO: 保存数据
            // TODO: 返回登录界面
            System.exit(0);
        }
    }

    @FXML
    private void handleExit() {
        if (FXUtils.showConfirmAlert("确认退出", "确定要退出系统吗？")) {
            // TODO: 保存数据
            System.exit(0);
        }
    }

    @FXML
    private void handleUserManagement() {
        updateStatus("用户管理");
        setActiveButton(null);
        FXUtils.showInfoAlert("开发中", "用户管理功能正在开发中...");
    }

    @FXML
    private void handleDataBackup() {
        updateStatus("数据备份");
        FXUtils.showInfoAlert("开发中", "数据备份功能正在开发中...");
    }

    @FXML
    private void handleDataRestore() {
        updateStatus("数据恢复");
        FXUtils.showInfoAlert("开发中", "数据恢复功能正在开发中...");
    }

    @FXML
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
    private void handleShortcuts() {
        updateStatus("快捷键");
        String shortcuts = 
            "快捷键列表:\n\n" +
            "功能键:\n" +
            "F1 - 添加商品\n" +
            "F2 - 补货\n" +
            "F3 - 删除商品\n" +
            "F4 - 搜索\n" +
            "F5 - 刷新当前面板\n" +
            "F6 - 分类管理\n" +
            "F7 - 会员管理\n" +
            "F8 - 结账\n" +
            "F9 - 促销管理\n" +
            "F10 - 库存预警\n" +
            "F11 - 数据备份\n" +
            "F12 - 数据恢复\n" +
            "ESC - 清空搜索\n" +
            "Delete - 删除选中项\n\n" +
            "Ctrl 组合键:\n" +
            "Ctrl+N - 添加商品\n" +
            "Ctrl+S - 保存数据\n" +
            "Ctrl+F - 搜索\n" +
            "Ctrl+D - 导出数据\n" +
            "Ctrl+R - 刷新当前面板\n" +
            "Ctrl+Q - 退出程序\n" +
            "Ctrl+A - 全选\n" +
            "Ctrl+E - 编辑选中项\n" +
            "Ctrl+B - 批量操作\n" +
            "Ctrl+M - 会员管理\n" +
            "Ctrl+T - 交易统计\n" +
            "Ctrl+1 - 切换到库存管理\n" +
            "Ctrl+2 - 切换到购物车\n" +
            "Ctrl+3 - 切换到交易记录\n" +
            "Ctrl+4 - 切换到设置";
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("快捷键");
        alert.setHeaderText(null);
        alert.setContentText(shortcuts);
        alert.getDialogPane().setPrefWidth(600);
        alert.showAndWait();
    }

    @FXML
    private void handleAbout() {
        updateStatus("关于");
        String about = 
            "收银系统 Cashier System\n\n" +
            "版本: 2.0.0 (JavaFX)\n" +
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
        updateStatus("库存管理");
        setActiveButton(inventoryBtn);
        showPlaceholder("库存管理", "📦", "商品库存管理功能正在开发中...");
    }

    @FXML
    private void handleCart() {
        updateStatus("购物车");
        setActiveButton(cartBtn);
        showPlaceholder("购物车", "🛒", "购物车功能正在开发中...");
    }

    @FXML
    private void handleCheckout() {
        updateStatus("结账");
        setActiveButton(checkoutBtn);
        showPlaceholder("结账", "💳", "结账功能正在开发中...");
    }

    @FXML
    private void handleTransactions() {
        updateStatus("交易记录");
        setActiveButton(transactionsBtn);
        showPlaceholder("交易记录", "📊", "交易记录功能正在开发中...");
    }

    @FXML
    private void handleMembers() {
        updateStatus("会员管理");
        setActiveButton(membersBtn);
        showPlaceholder("会员管理", "👥", "会员管理功能正在开发中...");
    }

    @FXML
    private void handleStatistics() {
        updateStatus("数据统计");
        setActiveButton(statisticsBtn);
        showPlaceholder("数据统计", "📈", "数据统计功能正在开发中...");
    }

    @FXML
    private void handlePromotions() {
        updateStatus("促销管理");
        setActiveButton(promotionsBtn);
        showPlaceholder("促销管理", "🎉", "促销管理功能正在开发中...");
    }

    @FXML
    private void handleShift() {
        updateStatus("交接班");
        setActiveButton(shiftBtn);
        showPlaceholder("交接班", "🔄", "交接班功能正在开发中...");
    }

    @FXML
    private void handleSettings() {
        updateStatus("设置");
        setActiveButton(settingsBtn);
        showPlaceholder("设置", "⚙️", "设置功能正在开发中...");
    }

    /**
     * 显示占位符内容
     * @param title 标题
     * @param icon 图标
     * @param message 消息
     */
    private void showPlaceholder(String title, String icon, String message) {
        VBox placeholder = new VBox(20);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        placeholder.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 40;");
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 64px;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #3F51B5;");
        
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #757575;");
        
        placeholder.getChildren().addAll(iconLabel, titleLabel, messageLabel);
        
        contentPane.getChildren().clear();
        contentPane.getChildren().add(placeholder);
    }
}
