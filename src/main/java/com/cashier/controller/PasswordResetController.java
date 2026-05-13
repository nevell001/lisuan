package com.cashier.controller;

import com.cashier.dao.UserDAO;
import com.cashier.model.User;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Map;

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
            showError("用户名和邮箱不能为空！");
            return;
        }

        // 验证邮箱格式
        if (!isValidEmail(email)) {
            showError("邮箱格式不正确！");
            return;
        }

        // 显示加载状态
        setSubmitState(true);

        // 异步处理（避免阻塞 UI）
        new Thread(() -> {
            try {
                // 查找用户
                User user;
                try {
                    user = UserDAO.findByUsername(username);
                } catch (SQLException e) {
                    showError("查询用户失败：" + e.getMessage());
                    setSubmitState(false);
                    return;
                }

                // 验证用户和邮箱
                if (user == null) {
                    showError("用户名不存在！");
                    setSubmitState(false);
                    return;
                }

                if (!user.email.equals(email)) {
                    showError("邮箱与注册邮箱不匹配！");
                    setSubmitState(false);
                    return;
                }

                // 使用 JavaFX PauseTransition 模拟发送重置邮件的延迟
                setSubmitState(true); // 显示加载状态

                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1000));
                pause.setOnFinished(event -> {
                    setSubmitState(false); // 恢复提交状态
                    // 显示成功消息
                    showSuccess("重置链接已发送到您的邮箱，请查收！");

                    // 3秒后关闭对话框
                    javafx.animation.PauseTransition closePause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(3000));
                    closePause.setOnFinished(closeEvent -> dialogStage.close());
                    closePause.play();
                });
                pause.play();

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("发送失败：" + e.getMessage());
                    setSubmitState(false);
                });
                logger.error("发送失败", e);
            }
        }).start();
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
            submitButton.setDisable(loading);
            submitButton.setText(loading ? "发送中..." : "发送重置链接");
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