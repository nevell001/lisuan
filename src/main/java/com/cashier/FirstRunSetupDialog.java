package com.cashier;

import com.cashier.i18n.I18nManager;
import com.cashier.model.User;
import com.cashier.service.DataService;
import com.cashier.service.FirstRunSetupService;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.ThemeUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

/**
 * 首次运行向导：空库启动时先让用户设置管理员账号，再进登录界面。
 *
 * <p>替代原来的两条不靠谱路径——应用自建随机密码（javaw 启动看不到控制台，用户拿不到密码）
 * 和 SQL 种子里的 admin/admin123（公开弱口令）。校验规则与登录页的"首次登录改密"
 * 对话框保持一致：长度取系统设置 {@code passwordMinLength}，复杂度开关取
 * {@code passwordComplexity}，要求的字符类型同样只认"字母 + 数字"。</p>
 */
public final class FirstRunSetupDialog {

    private static final Logger logger = LoggerFactoryUtil.getLogger(FirstRunSetupDialog.class);

    private static final int DEFAULT_MIN_PASSWORD_LENGTH = 6;

    private FirstRunSetupDialog() {
    }

    /**
     * 显示向导并等待用户创建管理员。
     *
     * @return 创建成功的用户；用户取消/关闭窗口时为空
     */
    public static Optional<User> showAndWait(Window owner) {
        int parsedMinLength = DEFAULT_MIN_PASSWORD_LENGTH;
        boolean parsedComplexity = true;
        try {
            Map<String, String> settings = DataService.loadSettings();
            parsedMinLength = Integer.parseInt(
                settings.getOrDefault("passwordMinLength", String.valueOf(DEFAULT_MIN_PASSWORD_LENGTH)));
            parsedComplexity = Boolean.parseBoolean(settings.getOrDefault("passwordComplexity", "true"));
        } catch (Exception e) {
            // 设置读不出来就用默认策略，不能因为读设置失败把用户挡在向导里
            logger.warn("读取密码策略失败，使用默认值", e);
        }
        // 定稿成 final：校验 lambda 会捕获这两个值
        final int minPasswordLength = parsedMinLength;
        final boolean requireComplexity = parsedComplexity;

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(I18nManager.getInstance().get("firstrun.title"));
        dialog.setHeaderText(I18nManager.getInstance().get("firstrun.header"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType createType = new ButtonType(
            I18nManager.getInstance().get("firstrun.create"), ButtonBar.ButtonData.OK_DONE);
        ButtonType exitType = new ButtonType(
            I18nManager.getInstance().get("firstrun.exit"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, exitType);

        TextField usernameField = new TextField(FirstRunSetupService.DEFAULT_ADMIN_USERNAME);
        usernameField.setPromptText(I18nManager.getInstance().get("firstrun.username_hint"));
        TextField nameField = new TextField();
        nameField.setPromptText(I18nManager.getInstance().get("firstrun.display_name_hint"));
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(I18nManager.getInstance().get("firstrun.password"));
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText(I18nManager.getInstance().get("firstrun.confirm_password"));

        String policyKey = requireComplexity
            ? "firstrun.password_policy_complexity" : "firstrun.password_policy";
        Label policyLabel = new Label(
            I18nManager.getInstance().get(policyKey, minPasswordLength));
        policyLabel.getStyleClass().addAll("text-muted", "caption-text");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        grid.add(new Label(I18nManager.getInstance().get("firstrun.username")), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label(I18nManager.getInstance().get("firstrun.display_name")), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label(I18nManager.getInstance().get("firstrun.password")), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label(I18nManager.getInstance().get("firstrun.confirm_password")), 0, 3);
        grid.add(confirmField, 1, 3);
        grid.add(policyLabel, 1, 4);
        grid.add(errorLabel, 1, 5);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(520);
        ThemeUtils.applyDialogTheme(dialog.getDialogPane());

        Button createButton = (Button) dialog.getDialogPane().lookupButton(createType);
        createButton.setDisable(true);

        Runnable validate = () -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            String password = passwordField.getText();
            String confirm = confirmField.getText();
            boolean valid = !usernameField.getText().isBlank()
                && password.length() >= minPasswordLength
                && password.equals(confirm);
            if (valid && requireComplexity) {
                valid = password.matches(".*[a-zA-Z].*") && password.matches(".*\\d.*");
            }
            createButton.setDisable(!valid);
        };
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());
        confirmField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());

        User[] created = new User[1];
        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                created[0] = FirstRunSetupService.createAdministrator(
                    usernameField.getText(), passwordField.getText(), nameField.getText());
            } catch (Exception e) {
                // 建号失败不关窗：留在向导里改用户名/重试，关掉就只能重开应用
                logger.error("首次运行向导创建管理员失败", e);
                String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                errorLabel.setText(I18nManager.getInstance().get("firstrun.create_failed", reason));
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                event.consume();
            }
        });

        dialog.setResultConverter(button -> button == createType ? created[0] : null);
        return Optional.ofNullable(dialog.showAndWait().orElse(null));
    }
}
