package com.cashier.controller;

import com.cashier.CashierSystemFXApplication;
import com.cashier.model.DataManager;
import com.cashier.model.User;
import com.cashier.util.FXUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.*;
import java.util.Map;
import java.util.Properties;

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
    private CheckBox rememberMeCheckBox;

    @FXML
    private Label errorLabel;

    @FXML
    private Label versionLabel;

    @FXML
    private VBox loginCard;

    private CashierSystemFXApplication application;
    private static final String CONFIG_FILE = "config/login_config.properties";

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 加载记住的密码
        loadSavedCredentials();

        // 设置默认焦点
        usernameField.requestFocus();

        // 设置回车键登录
        usernameField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> handleLogin());

        // 设置版本信息
        versionLabel.setText("版本 2.0.0 (JavaFX)");

        // 添加入场动画
        addEntranceAnimation();
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
            showError("用户名和密码不能为空！");
            shakeTextField(usernameField);
            shakeTextField(passwordField);
            return;
        }

        // 显示加载状态
        setLoginState(true);

        // 异步验证登录（避免阻塞 UI）
        new Thread(() -> {
            try {
                // 加载用户数据
                Map<String, User> users = DataManager.loadUsers();

                // 验证用户
                User user = users.get(username);
                if (user == null) {
                    showError("用户名不存在！");
                    shakeTextField(usernameField);
                    setLoginState(false);
                    return;
                }

                if (!user.password.equals(password)) {
                    showError("密码错误！");
                    shakeTextField(passwordField);
                    setLoginState(false);
                    return;
                }

                if (!user.active) {
                    showError("该账户已被禁用！");
                    setLoginState(false);
                    return;
                }

                // 更新最后登录时间
                user.lastLoginTime = new java.util.Date();
                DataManager.saveUsers(users);

                // 保存记住的密码
                if (rememberMeCheckBox.isSelected()) {
                    saveCredentials(username, password);
                } else {
                    clearSavedCredentials();
                }

                // 登录成功，切换到主界面
                javafx.application.Platform.runLater(() -> {
                    if (application != null) {
                        application.switchToMainView(user);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("登录失败：" + e.getMessage());
                    setLoginState(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 处理退出
     */
    @FXML
    private void handleExit() {
        System.exit(0);
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);

        // 添加淡入动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), errorLabel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // 3秒后自动隐藏
        javafx.application.Platform.runLater(() -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), errorLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> errorLabel.setVisible(false));
            fadeOut.play();
        }, 3000);
    }

    /**
     * 设置登录状态（启用/禁用输入）
     * @param loading 是否正在加载
     */
    private void setLoginState(boolean loading) {
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        rememberMeCheckBox.setDisable(loading);
    }

    /**
     * 文本框抖动动画
     * @param textField 要抖动的文本框
     */
    private void shakeTextField(TextField textField) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), textField);
        scaleTransition.setFromX(1.0);
        scaleTransition.setToX(1.05);
        scaleTransition.setCycleCount(2);
        scaleTransition.setAutoReverse(true);
        scaleTransition.play();
    }

    /**
     * 添加入场动画
     */
    private void addEntranceAnimation() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), loginCard);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(500), loginCard);
        scaleUp.setFromX(0.95);
        scaleUp.setFromY(0.95);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        scaleUp.play();
    }

    /**
     * 保存凭据
     * @param username 用户名
     * @param password 密码
     */
    private void saveCredentials(String username, String password) {
        try {
            File configFile = new File(CONFIG_FILE);
            configFile.getParentFile().mkdirs();

            Properties props = new Properties();
            props.setProperty("username", username);
            props.setProperty("password", password);
            props.setProperty("remember", "true");

            try (OutputStream output = new FileOutputStream(configFile)) {
                props.store(output, "Login Configuration");
            }
        } catch (IOException e) {
            System.err.println("保存凭据失败: " + e.getMessage());
        }
    }

    /**
     * 加载保存的凭据
     */
    private void loadSavedCredentials() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                return;
            }

            Properties props = new Properties();
            try (InputStream input = new FileInputStream(configFile)) {
                props.load(input);
            }

            String remember = props.getProperty("remember", "false");
            if ("true".equals(remember)) {
                usernameField.setText(props.getProperty("username", ""));
                passwordField.setText(props.getProperty("password", ""));
                rememberMeCheckBox.setSelected(true);
            }
        } catch (IOException e) {
            System.err.println("加载凭据失败: " + e.getMessage());
        }
    }

    /**
     * 清除保存的凭据
     */
    private void clearSavedCredentials() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                configFile.delete();
            }
        } catch (Exception e) {
            System.err.println("清除凭据失败: " + e.getMessage());
        }
    }
}
