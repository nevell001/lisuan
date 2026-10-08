package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.CashierSystemFXApplication;
import com.cashier.constant.AppConstants;
import com.cashier.dao.DAOFactory;
import com.cashier.i18n.I18nManager;
import com.cashier.model.User;
import com.cashier.service.DataService;
import com.cashier.util.PasswordUtil;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.service.AuditService;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Map;

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
        versionLabel.setText(I18nManager.getInstance().get("runtime.version", AppConstants.APP_VERSION, AppConstants.APP_SUBTITLE));

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
            showError(I18nManager.getInstance().get("runtime.login_required"));
            shakeTextField(usernameField);
            shakeTextField(passwordField);
            return;
        }

        // 检查是否处于锁定状态（从数据库读取，支持跨重启持久化）
        if (DAOFactory.getInstance().getLoginAttemptDAO().isLocked(username)) {
            long remainingSeconds = DAOFactory.getInstance().getLoginAttemptDAO().getRemainingLockoutSeconds(username);
            showError(I18nManager.getInstance().get("runtime.login_rate_limited", remainingSeconds));
            return;
        }

        // 显示加载状态
        setLoginState(true);

        // 异步验证登录（避免阻塞 UI）
        new Thread(() -> {
            try {
                // 锁定阈值取系统设置 passwordMaxAttempts（此前写死在常量里，设置页改了不生效，审计 F6）
                final int maxLoginAttempts = com.cashier.service.DataService.getIntSetting(
                    "passwordMaxAttempts", MAX_LOGIN_ATTEMPTS, 1, 50);
                // 使用数据库验证用户
                User user = DAOFactory.getInstance().getUserDAO().findByUsername(username);
                if (user == null) {
                    AuditService.failure(null, "AUTH", "LOGIN", "用户名不存在: " + username);
                    int attempts = DAOFactory.getInstance().getLoginAttemptDAO().recordFailedAttempt(username, maxLoginAttempts, LOCKOUT_DURATION_MINUTES * 60 * 1000);
                    Platform.runLater(() -> {
                        showError(I18nManager.getInstance().get("runtime.login_user_missing", maxLoginAttempts - attempts));
                        shakeTextField(usernameField);
                        setLoginState(false);
                    });
                    return;
                }

                if (!PasswordUtil.verifyPassword(password, user.password, username)) {
                    AuditService.failure(username, "AUTH", "LOGIN", "密码验证失败");
                    int attempts = DAOFactory.getInstance().getLoginAttemptDAO().recordFailedAttempt(username, maxLoginAttempts, LOCKOUT_DURATION_MINUTES * 60 * 1000);
                    Platform.runLater(() -> {
                        showError(I18nManager.getInstance().get("runtime.login_wrong_password", maxLoginAttempts - attempts));
                        shakeTextField(passwordField);
                        setLoginState(false);
                    });
                    return;
                }

                if (!user.active) {
                    AuditService.failure(username, "AUTH", "LOGIN", "账户已禁用");
                    Platform.runLater(() -> {
                        showError(I18nManager.getInstance().get("runtime.login_disabled"));
                        setLoginState(false);
                    });
                    return;
                }

                // 更新最后登录时间到数据库
                DAOFactory.getInstance().getUserDAO().updateLastLoginTimeByUsername(username);
                AuditService.success(username, "AUTH", "LOGIN", "登录成功", 1);

                // 重置登录尝试次数（持久化到数据库）
                DAOFactory.getInstance().getLoginAttemptDAO().resetAttempts(username);

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
                AuditService.failure(null, "AUTH", "LOGIN", "登录处理异常: " + e.getClass().getSimpleName());
                javafx.application.Platform.runLater(() -> {
                    showError(I18nManager.getInstance().get("runtime.login_failed", I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED)));
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
        if (application != null) {
            application.requestExit();
        } else {
            javafx.application.Platform.exit();
        }
    }

    /**
     * 显示密码修改对话框（首次登录）
     */
    private void showPasswordChangeDialog(com.cashier.model.User user) {
        try {
            // 读取系统密码策略配置
            Map<String, String> settings = DataService.loadSettings();
            int minPasswordLength = Integer.parseInt(settings.getOrDefault("passwordMinLength", "6"));
            boolean requireComplexity = Boolean.parseBoolean(settings.getOrDefault("passwordComplexity", "true"));

            // 创建对话框
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.first_login_title"));
            dialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get("runtime.first_login_header"));

            // 创建UI
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            javafx.scene.control.Label newPasswordLabel = new javafx.scene.control.Label("新密码:");
            javafx.scene.control.PasswordField newPasswordField = new javafx.scene.control.PasswordField();
            newPasswordField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.new_password_hint"));

            javafx.scene.control.Label confirmPasswordLabel = new javafx.scene.control.Label("确认密码:");
            javafx.scene.control.PasswordField confirmPasswordField = new javafx.scene.control.PasswordField();
            confirmPasswordField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.confirm_password_hint"));

            grid.add(newPasswordLabel, 0, 0);
            grid.add(newPasswordField, 1, 0);
            grid.add(confirmPasswordLabel, 0, 1);
            grid.add(confirmPasswordField, 1, 1);

            // 根据系统配置动态生成密码要求提示
            String hint = "密码要求：至少" + minPasswordLength + "位字符";
            if (requireComplexity) {
                hint += "，需包含字母和数字";
            }
            javafx.scene.control.Label hintLabel = new javafx.scene.control.Label(hint);
            hintLabel.getStyleClass().addAll("text-muted", "caption-text");
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

            // 验证输入（基于系统配置的密码策略）
            Runnable validate = () -> {
                String newPassword = newPasswordField.getText();
                String confirmPassword = confirmPasswordField.getText();
                boolean valid = newPassword.length() >= minPasswordLength && newPassword.equals(confirmPassword);
                if (requireComplexity && valid) {
                    boolean hasLetter = newPassword.matches(".*[a-zA-Z].*");
                    boolean hasDigit = newPassword.matches(".*\\d.*");
                    valid = hasLetter && hasDigit;
                }
                okButton.setDisable(!valid);
            };

            newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());
            confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());

            // 显示对话框并等待响应
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initOwner(usernameField.getScene().getWindow());

            // 只有"改密成功且已切到主界面"才算离开登录页；其余情况（取消/关窗/改密失败/
            // application 为空）都必须把登录界面恢复可用——handleLogin 在进入这里之前
            // 已经 setLoginState(true) 禁用了两个输入框，不恢复就只能杀进程（TD-030）。
            boolean switchedToMain = false;
            java.util.Optional<ButtonType> response = dialog.showAndWait();
            if (response.isPresent() && response.get() == ButtonType.OK) {
                try {
                    String newPassword = newPasswordField.getText();
                    String hashedPassword = com.cashier.util.PasswordUtil.hashPassword(newPassword);

                    // 更新密码
                    com.cashier.dao.DAOFactory.getInstance().getUserDAO().updatePassword(user.id, hashedPassword);

                    // 显示成功消息
                    StatusBarManager.updateSuccess(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_changed_message"));
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_changed_title"));
                    alert.setHeaderText(null);
                    alert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_changed_message"));
                    alert.showAndWait();

                    // 切换到主界面
                    if (application != null) {
                        application.switchToMainView(user);
                        switchedToMain = true;
                    }

                } catch (Exception e) {
                    logger.error("密码修改失败", e);
                    showError(I18nManager.getInstance().get("runtime.password_change_failed", I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED)));
                }
            }

            if (!switchedToMain) {
                setLoginState(false);
            }

        } catch (Exception e) {
            logger.error("显示密码修改对话框失败", e);
            setLoginState(false);
            showError(I18nManager.getInstance().get("runtime.password_dialog_failed", I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED)));
        }
    }

    /**
     * 处理关于
     */
    @FXML
    public void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Menu.Help.ABOUT));
        alert.setHeaderText(AppConstants.APP_NAME + " v" + AppConstants.APP_VERSION);
        alert.setContentText(I18nManager.getInstance().get("runtime.about_content",
                AppConstants.APP_SUBTITLE, AppConstants.JAVAFX_VERSION,
                AppConstants.MIN_MAVEN_VERSION, AppConstants.MIN_JDK_VERSION)
                + "\n\n© 2026 " + AppConstants.DEVELOPER);
        alert.initOwner(usernameField.getScene().getWindow());
        alert.showAndWait();
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        StatusBarManager.updateError(message);
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
