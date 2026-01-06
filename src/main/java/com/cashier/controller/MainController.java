package com.cashier.controller;

import com.cashier.CashierSystemFXApplication;
import com.cashier.model.User;
import com.cashier.util.FXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * 主控制器
 * 处理主界面的导航和功能
 */
public class MainController {

    @FXML
    private Label statusLabel;

    @FXML
    private Label userLabel;

    private CashierSystemFXApplication application;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        updateStatus("就绪");
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
        userLabel.setText("用户: " + user.name + " (" + user.getRoleDisplayName() + ")");
    }

    /**
     * 更新状态栏
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
    }

    // ========== 菜单处理方法 ==========

    @FXML
    private void handleExit() {
        if (FXUtils.showConfirmAlert("确认退出", "确定要退出系统吗？")) {
            System.exit(0);
        }
    }

    @FXML
    private void handleUserManagement() {
        updateStatus("用户管理");
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
    private void handleAbout() {
        updateStatus("关于");
        FXUtils.showInfoAlert("关于", "收银系统 v2.0.0\nJavaFX 版本");
    }

    // ========== 导航处理方法 ==========

    @FXML
    private void handleInventory() {
        updateStatus("库存管理");
        FXUtils.showInfoAlert("开发中", "库存管理功能正在开发中...");
    }

    @FXML
    private void handleCart() {
        updateStatus("购物车");
        FXUtils.showInfoAlert("开发中", "购物车功能正在开发中...");
    }

    @FXML
    private void handleTransactions() {
        updateStatus("交易记录");
        FXUtils.showInfoAlert("开发中", "交易记录功能正在开发中...");
    }

    @FXML
    private void handleMembers() {
        updateStatus("会员管理");
        FXUtils.showInfoAlert("开发中", "会员管理功能正在开发中...");
    }

    @FXML
    private void handleStatistics() {
        updateStatus("数据统计");
        FXUtils.showInfoAlert("开发中", "数据统计功能正在开发中...");
    }

    @FXML
    private void handlePromotions() {
        updateStatus("促销管理");
        FXUtils.showInfoAlert("开发中", "促销管理功能正在开发中...");
    }

    @FXML
    private void handleShift() {
        updateStatus("交接班");
        FXUtils.showInfoAlert("开发中", "交接班功能正在开发中...");
    }

    @FXML
    private void handleSettings() {
        updateStatus("设置");
        FXUtils.showInfoAlert("开发中", "设置功能正在开发中...");
    }
}