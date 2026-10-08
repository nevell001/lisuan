package com.cashier.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.model.User;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.StatusBarManager;

import java.sql.SQLException;
import java.util.Objects;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;


/**
 * 密码重置控制器
 * 处理用户密码重置请求
 */
public class PasswordResetController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PasswordResetController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label successLabel;

    @FXML
    private Button submitButton;

    private Stage dialogStage;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置默认焦点
        usernameField.requestFocus();

        // 设置回车键提交
        usernameField.setOnAction(event -> emailField.requestFocus());
        emailField.setOnAction(event -> handleSubmit());

        // H-18: 标注为演示模式，禁用提交按钮
        submitButton.setDisable(true);
        submitButton.setText(com.cashier.i18n.I18nManager.getInstance().get("password_reset.demo_mode"));
        submitButton.setTooltip(new Tooltip(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_reset_demo_warning")));
        logger.warn("密码重置功能当前为演示模式，实际邮件发送功能未实现");
    }

    /**
     * 设置对话框阶段
     * @param dialogStage 对话框阶段
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 处理提交
     */
    @FXML
    public void handleSubmit() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();

        // 验证输入
        if (username.isEmpty() || email.isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_reset_required"));
            return;
        }

        // 验证邮箱格式
        if (!isValidEmail(email)) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.email_invalid"));
            return;
        }

        // 显示加载状态
        setSubmitState(true);

        // 异步处理（避免阻塞 UI）
        Thread backgroundThread = new Thread(() -> {
            try {
                // 查找用户
                User user;
                try {
                    user = DAOFactory.getInstance().getUserDAO().findByUsername(username);
                } catch (SQLException e) {
                    logger.error("查询用户失败", e);
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_reset_service_unavailable"));
                    setSubmitState(false);
                    return;
                }

                // H-19: 统一返回模糊消息，防止用户枚举攻击
                // 无论用户不存在还是邮箱不匹配，都返回相同的消息
                boolean credentialsMatch = (user != null) && Objects.equals(user.email, email);
                if (!credentialsMatch) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_reset_credentials_mismatch"));
                    setSubmitState(false);
                    return;
                }

                // H-18: 演示模式 — 不实际发送邮件
                logger.warn("密码重置功能为演示模式，用户 {} 的重置请求未实际处理", username);
                javafx.application.Platform.runLater(() -> {
                    setSubmitState(false);
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_reset_demo_warning"));

                    // 3秒后关闭对话框
                    javafx.animation.PauseTransition closePause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(3000));
                    closePause.setOnFinished(closeEvent -> dialogStage.close());
                    closePause.play();
                });

            } catch (Exception e) {
                logger.error("密码重置处理异常", e);
                javafx.application.Platform.runLater(() -> {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_reset_service_unavailable"));
                    setSubmitState(false);
                });
            }
        });
        // TD-035：非 daemon 线程会让关窗后进程不退出
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    /**
     * 处理取消
     */
    @FXML
    public void handleCancel() {
        dialogStage.close();
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        javafx.application.Platform.runLater(() -> {
            StatusBarManager.updateError(message);
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            successLabel.setVisible(false);
            successLabel.setManaged(false);

            // 添加淡入动画
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), errorLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
    }

    /**
     * 显示成功信息
     * @param message 成功消息
     */
    private void showSuccess(String message) {
        javafx.application.Platform.runLater(() -> {
            StatusBarManager.updateSuccess(message);
            successLabel.setText(message);
            successLabel.setVisible(true);
            successLabel.setManaged(true);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            // 禁用提交按钮
            submitButton.setDisable(true);

            // 添加淡入动画
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), successLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
    }

    /**
     * 设置提交状态（启用/禁用提交按钮）
     * @param loading 是否正在加载
     */
    private void setSubmitState(boolean loading) {
        javafx.application.Platform.runLater(() -> {
            usernameField.setDisable(loading);
            emailField.setDisable(loading);
            // H-18: 演示模式下提交按钮始终保持禁用
            submitButton.setDisable(true);
            submitButton.setText(com.cashier.i18n.I18nManager.getInstance().get("password_reset.demo_mode"));
        });
    }

    /**
     * 验证邮箱格式
     * @param email 邮箱地址
     * @return 是否有效
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
}
