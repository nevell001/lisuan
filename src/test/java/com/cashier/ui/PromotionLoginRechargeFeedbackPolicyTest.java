package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromotionLoginRechargeFeedbackPolicyTest {

    @Test
    @DisplayName("促销页面空选择和表单校验应同步警告状态")
    void promotionWarningsUpdateStatusBar() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PromotionController.java"
        ));

        assertTrue(controller.contains("showWarning(I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_PROMOTION))"));
        assertTrue(controller.contains("StatusBarManager.updateWarning(e.getMessage())"));
        assertTrue(controller.contains("StatusBarManager.updateWarning(message)"));
        assertTrue(controller.contains("StatusBarManager.updateError(message)"));
    }

    @Test
    @DisplayName("登录和充值页面错误提示应同步状态栏")
    void loginAndRechargeErrorsUpdateStatusBar() throws Exception {
        List<String> controllerFiles = List.of(
            "src/main/java/com/cashier/controller/LoginController.java",
            "src/main/java/com/cashier/controller/RechargeController.java"
        );

        for (String file : controllerFiles) {
            String controller = Files.readString(Path.of(file));
            assertTrue(controller.contains("StatusBarManager.updateError(message)")
                    || controller.contains("FXUtils.showError(message)"), file);
        }

        String rechargeController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/RechargeController.java"
        ));
        assertTrue(rechargeController.contains("StatusBarManager.updateError(errorMessage)"));
    }

    @Test
    @DisplayName("高频成功反馈应显式使用成功状态")
    void successFeedbackUsesSuccessLevel() throws Exception {
        String loginController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/LoginController.java"
        ));
        String productEditController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ProductEditController.java"
        ));

        assertTrue(loginController.contains("StatusBarManager.updateSuccess"));
        StatusBarAssertions.assertUsesSuccessLevel(productEditController, "status_message.product_created", "ProductEditController");
        StatusBarAssertions.assertUsesSuccessLevel(productEditController, "status_message.product_updated", "ProductEditController");
    }

    @Test
    @DisplayName("促销空选择提示应具备完整国际化文案")
    void promotionSelectionWarningIsLocalized() throws Exception {
        List<String> bundleFiles = List.of(
            "src/main/resources/com/cashier/i18n/messages.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_CN.properties",
            "src/main/resources/com/cashier/i18n/messages_en.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_TW.properties"
        );

        for (String file : bundleFiles) {
            String bundle = Files.readString(Path.of(file));
            assertTrue(bundle.contains("runtime.select_promotion="), file);
        }
    }
}
