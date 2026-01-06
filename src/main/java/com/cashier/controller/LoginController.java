package com.cashier.controller;

import com.cashier.CashierSystemFXApplication;
import com.cashier.model.DataManager;
import com.cashier.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Map;

/**
 * 登录控制器
 * 处理用户登录逻辑
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private CashierSystemFXApplication application;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置默认焦点
        usernameField.requestFocus();

        // 设置回车键登录
        usernameField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> handleLogin());
    }

    /**
     * 设置应用程序引用
     * @param application 应用程序实例
     */
    public void setApplication(CashierSystemFXApplication application) {
        this.application = application;
    }

    /**
     * 处理登录
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // 验证输入
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("用户名和密码不能为空！", "danger");
            return;
        }

        // 加载用户数据
        Map<String, User> users = DataManager.loadUsers();

        // 验证用户
        User user = users.get(username);
        if (user == null) {
            showMessage("用户名不存在！", "danger");
            return;
        }

        if (!user.password.equals(password)) {
            showMessage("密码错误！", "danger");
            return;
        }

        if (!user.active) {
            showMessage("该账户已被禁用！", "danger");
            return;
        }

        // 更新最后登录时间
        user.lastLoginTime = new java.util.Date();
        DataManager.saveUsers(users);

        // 登录成功，切换到主界面
        showMessage("登录成功！", "success");

        // 延迟切换到主界面
        javafx.application.Platform.runLater(() -> {
            if (application != null) {
                application.switchToMainView(user);
            }
        });
    }

    /**
     * 处理退出
     */
    @FXML
    private void handleExit() {
        System.exit(0);
    }

    /**
     * 显示消息
     * @param message 消息内容
     * @param styleClass 样式类
     */
    private void showMessage(String message, String styleClass) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().clear();
        messageLabel.getStyleClass().add(styleClass);
    }
}