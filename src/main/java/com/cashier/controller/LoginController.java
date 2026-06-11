package com.cashier.controller;

import com.cashier.CashierSystemFXApplication;
import com.cashier.constant.AppConstants;
import com.cashier.dao.UserDAO;
import com.cashier.model.User;
import com.cashier.util.FXUtils;
import com.cashier.util.PasswordUtil;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.Instant;

/**
 * 登录控制器
 * 处理用户登录逻辑
 */
public class LoginController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(LoginController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label versionLabel;

    @FXML
    private VBox loginCard;

    @FXML
    private ProgressIndicator loadingIndicator;

    private CashierSystemFXApplication application;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 5; // 锁定5分钟
    private int loginAttempts = 0;
    private Instant lockoutEndTime = null; // 锁定结束时间

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

        // 设置版本信息
        versionLabel.setText("版本 " + AppConstants.APP_VERSION + " (" + AppConstants.APP_SUBTITLE + ")");

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
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // 验证输入
        if (username.isEmpty() || password.isEmpty()) {
            showError("用户名和密码不能为空！");
            shakeTextField(usernameField);
            shakeTextField(passwordField);
            return;
        }

        // 检查是否处于锁定状态
        if (isLockedOut()) {
            long remainingSeconds = getRemainingLockoutSeconds();
            showError("登录尝试次数过多，请 " + remainingSeconds + " 秒后再试！");
            return;
        }

        // 显示加载状态
        setLoginState(true);

        // 异步验证登录（避免阻塞 UI）
        new Thread(() -> {
            try {
                // 使用数据库验证用户
                User user = UserDAO.findByUsername(username);
                if (user == null) {
                    loginAttempts++;
                    checkAndLockAccount(username);
                    Platform.runLater(() -> {
                        showError("用户名不存在！剩余尝试次数：" + (MAX_LOGIN_ATTEMPTS - loginAttempts));
                        shakeTextField(usernameField);
                        setLoginState(false);
                    });
                    return;
                }

                if (!PasswordUtil.verifyPassword(password, user.password, username)) {
                    loginAttempts++;
                    checkAndLockAccount(username);
                    Platform.runLater(() -> {
                        showError("密码错误！剩余尝试次数：" + (MAX_LOGIN_ATTEMPTS - loginAttempts));
                        shakeTextField(passwordField);
                        setLoginState(false);
                    });
                    return;
                }

                if (!user.active) {
                    Platform.runLater(() -> {
                        showError("该账户已被禁用！");
                        setLoginState(false);
                    });
                    return;
                }

                // 更新最后登录时间到数据库
                UserDAO.updateLastLoginTimeByUsername(username);

                // 重置登录尝试次数
                loginAttempts = 0;

                // 登录成功，切换到主界面
                javafx.application.Platform.runLater(() -> {
                    if (application != null) {
                        // 检查是否需要修改密码
                        if (user.forcePasswordChange) {
                            showPasswordChangeDialog(user);
                        } else {
                            application.switchToMainView(user);
                        }
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("登录失败：" + e.getMessage());
                    setLoginState(false);
                });
                logger.error("登录失败", e);
            }
        }).start();
    }

    /**
     * 处理退出
     */
    @FXML
    public void handleExit() {
        System.exit(0);
    }

    /**
     * 显示密码修改对话框（首次登录）
     */
    private void showPasswordChangeDialog(com.cashier.model.User user) {
        try {
            // 创建对话框
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("首次登录 - 修改密码");
            dialog.setHeaderText("为了安全起见，请修改您的初始密码");

            // 创建UI
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            javafx.scene.control.Label newPasswordLabel = new javafx.scene.control.Label("新密码:");
            javafx.scene.control.PasswordField newPasswordField = new javafx.scene.control.PasswordField();
            newPasswordField.setPromptText("请输入新密码");

            javafx.scene.control.Label confirmPasswordLabel = new javafx.scene.control.Label("确认密码:");
            javafx.scene.control.PasswordField confirmPasswordField = new javafx.scene.control.PasswordField();
            confirmPasswordField.setPromptText("请再次输入新密码");

            grid.add(newPasswordLabel, 0, 0);
            grid.add(newPasswordField, 1, 0);
            grid.add(confirmPasswordLabel, 0, 1);
            grid.add(confirmPasswordField, 1, 1);

            // 添加提示信息
            javafx.scene.control.Label hintLabel = new javafx.scene.control.Label("密码要求：至少6位字符");
            hintLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11;");
            grid.add(hintLabel, 1, 2);

            dialog.getDialogPane().setContent(grid);

            // 添加按钮
            dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK,
                ButtonType.CANCEL
            );

            // 禁用OK按钮，直到输入有效
            javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.setDisable(true);

            // 验证输入
            Runnable validate = () -> {
                String newPassword = newPasswordField.getText();
                String confirmPassword = confirmPasswordField.getText();
                boolean valid = newPassword.length() >= 6 && newPassword.equals(confirmPassword);
                okButton.setDisable(!valid);
            };

            newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());
            confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());

            // 显示对话框并等待响应
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initOwner(usernameField.getScene().getWindow());

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        String newPassword = newPasswordField.getText();
                        String hashedPassword = com.cashier.util.PasswordUtil.hashPassword(newPassword);

                        // 更新密码
                        com.cashier.dao.UserDAO.updatePassword(user.id, hashedPassword);

                        // 显示成功消息
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("密码修改成功");
                        alert.setHeaderText(null);
                        alert.setContentText("密码修改成功！现在可以进入系统了。");
                        alert.showAndWait();

                        // 切换到主界面
                        if (application != null) {
                            application.switchToMainView(user);
                        }

                    } catch (Exception e) {
                        logger.error("密码修改失败", e);
                        showError("密码修改失败：" + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            logger.error("显示密码修改对话框失败", e);
            showError("显示密码修改对话框失败：" + e.getMessage());
        }
    }

    /**
     * 处理关于
     */
    @FXML
    public void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于");
        alert.setHeaderText(AppConstants.APP_NAME + " v" + AppConstants.APP_VERSION);
        alert.setContentText("狸算(LiSuan)收银系统 - " + AppConstants.APP_SUBTITLE + "\n\n" +
                "技术栈：\n" +
                "- JavaFX " + AppConstants.JAVAFX_VERSION + "\n" +
                "- Maven " + AppConstants.MIN_MAVEN_VERSION + "+\n" +
                "- JDK " + AppConstants.MIN_JDK_VERSION + "\n\n" +
                "功能特性：\n" +
                "- 商品管理\n" +
                "- 购物车\n" +
                "- 结账系统\n" +
                "- 会员管理\n" +
                "- 交易记录\n" +
                "- 数据统计\n\n" +
                "© 2026 " + AppConstants.DEVELOPER);
        alert.initOwner(usernameField.getScene().getWindow());
        alert.showAndWait();
    }

    /**
     * 检查是否需要锁定账户
     * @param username 用户名
     */
    private void checkAndLockAccount(String username) {
        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            lockoutEndTime = Instant.now().plusSeconds(LOCKOUT_DURATION_MINUTES * 60);
            logger.warn("用户 {} 登录失败次数过多，账户已锁定 {} 分钟", username, LOCKOUT_DURATION_MINUTES);
        }
    }

    /**
     * 检查是否处于锁定状态
     * @return 是否被锁定
     */
    private boolean isLockedOut() {
        if (lockoutEndTime == null) {
            return false;
        }
        if (Instant.now().isAfter(lockoutEndTime)) {
            // 锁定时间已过，重置
            lockoutEndTime = null;
            loginAttempts = 0;
            Platform.runLater(() -> {
                usernameField.setDisable(false);
                passwordField.setDisable(false);
            });
            return false;
        }
        return true;
    }

    /**
     * 获取剩余锁定时间（秒）
     * @return 剩余秒数
     */
    private long getRemainingLockoutSeconds() {
        if (lockoutEndTime == null) {
            return 0;
        }
        long seconds = java.time.Duration.between(Instant.now(), lockoutEndTime).getSeconds();
        return Math.max(0, seconds);
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
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), errorLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> errorLabel.setVisible(false));
            fadeOut.play();
        });
        pause.play();
    }

    /**
     * 设置登录状态（启用/禁用输入）
     * @param loading 是否正在加载
     */
    private void setLoginState(boolean loading) {
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);

        // 显示/隐藏加载指示器
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
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
        // 淡入动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), loginCard);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        // 缩放动画
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(800), loginCard);
        scaleUp.setFromX(0.85);
        scaleUp.setFromY(0.85);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        scaleUp.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        // 同时播放
        javafx.animation.ParallelTransition parallelTransition = new javafx.animation.ParallelTransition(fadeIn, scaleUp);
        parallelTransition.play();

        // 添加输入框焦点动画
        addInputFieldAnimations();
    }

    /**
     * 添加输入框焦点动画
     */
    private void addInputFieldAnimations() {
        // 用户名输入框焦点动画
        usernameField.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                // 获得焦点时的动画
                javafx.animation.ScaleTransition scaleUp = new javafx.animation.ScaleTransition(Duration.millis(200), usernameField);
                scaleUp.setFromX(1.0);
                scaleUp.setFromY(1.0);
                scaleUp.setToX(1.02);
                scaleUp.setToY(1.05);
                scaleUp.play();
            }
        });

        // 密码输入框焦点动画
        passwordField.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                // 获得焦点时的动画
                javafx.animation.ScaleTransition scaleUp = new javafx.animation.ScaleTransition(Duration.millis(200), passwordField);
                scaleUp.setFromX(1.0);
                scaleUp.setFromY(1.0);
                scaleUp.setToX(1.02);
                scaleUp.setToY(1.05);
                scaleUp.play();
            }
        });
    }
}