package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusBarSeverityPolicyTest {

    @Test
    @DisplayName("全局状态栏应支持成功、警告和错误级别")
    void statusBarSupportsSeverityLevels() throws Exception {
        String statusBarManager = Files.readString(Path.of(
            "src/main/java/com/cashier/util/StatusBarManager.java"
        ));

        assertTrue(statusBarManager.contains("enum StatusLevel"));
        assertTrue(statusBarManager.contains("statusLevelProperty()"));
        assertTrue(statusBarManager.contains("updateSuccess(String status)"));
        assertTrue(statusBarManager.contains("updateWarning(String status)"));
        assertTrue(statusBarManager.contains("updateError(String status)"));
        assertTrue(statusBarManager.contains("inferStatusLevel(String status)"));
        assertTrue(statusBarManager.contains("\"成功\""));
        assertTrue(statusBarManager.contains("\"失败\""));
        assertTrue(statusBarManager.contains("\"警告\""));
    }

    @Test
    @DisplayName("主窗口状态栏应按级别切换颜色类")
    void shellControllersApplySeverityStyleClasses() throws Exception {
        String mainController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MainController.java"
        ));

        assertSeverityBinding(mainController);
        // 注：触屏收银台（TouchCartView）底部没有状态栏文本控件，也不绑定 statusLevelProperty，
        // 因此 StatusBarManager 的级别提示在触屏版界面上不显示（只有弹窗类提示可见）。
        // 详见 CLAUDE.md 的待修项；补上状态栏后应在此处同时断言 TouchCartController。
    }

    @Test
    @DisplayName("收银台扫码提示应区分成功、警告和错误")
    void cartScanMessagesUseSeverityApis() throws Exception {
        String cartController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));

        assertTrue(cartController.contains("enum ScanMessageLevel"));
        assertTrue(cartController.contains("ScanMessageLevel.SUCCESS"));
        assertTrue(cartController.contains("ScanMessageLevel.WARNING"));
        assertTrue(cartController.contains("ScanMessageLevel.ERROR"));
        assertTrue(cartController.contains("StatusBarManager.updateSuccess(message)"));
        assertTrue(cartController.contains("StatusBarManager.updateWarning(message)"));
        assertTrue(cartController.contains("StatusBarManager.updateError(message)"));
        assertTrue(cartController.contains("cart.scan.duplicate_ignored\", normalizedScanText), ScanMessageLevel.WARNING"));
        assertTrue(cartController.contains("cart.scan.multiple_matches\", exactMatches.size()), ScanMessageLevel.WARNING"));
    }

    @Test
    @DisplayName("通用弹窗和基础控制器应同步更新状态栏级别")
    void commonFeedbackUtilitiesUpdateStatusSeverity() throws Exception {
        String dialogBuilder = Files.readString(Path.of(
            "src/main/java/com/cashier/util/DialogBuilder.java"
        ));
        String fxUtils = Files.readString(Path.of(
            "src/main/java/com/cashier/util/FXUtils.java"
        ));
        String baseController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/base/BaseController.java"
        ));

        assertTrue(dialogBuilder.contains("updateStatusBarForAlert()"));
        assertTrue(dialogBuilder.contains("StatusBarManager.updateError(contentText)"));
        assertTrue(dialogBuilder.contains("StatusBarManager.updateWarning(contentText)"));
        assertTrue(fxUtils.contains("StatusBarManager.updateWarning(message)"));
        assertTrue(fxUtils.contains("StatusBarManager.updateError(message)"));
        assertTrue(baseController.contains("StatusBarManager.updateSuccess(message)"));
        assertTrue(baseController.contains("StatusBarManager.updateError(message)"));
        assertTrue(baseController.contains("StatusBarManager.updateWarning(message)"));
        assertTrue(baseController.contains("DialogBuilder.warning()"));
    }

    @Test
    @DisplayName("核心业务页面自定义弹窗应同步状态栏错误级别")
    void coreControllersMirrorCustomErrorDialogsToStatusBar() throws Exception {
        List<String> controllerFiles = List.of(
            "src/main/java/com/cashier/controller/CartController.java",
            "src/main/java/com/cashier/controller/MainController.java",
            "src/main/java/com/cashier/controller/SettingsController.java",
            "src/main/java/com/cashier/controller/ShiftController.java",
            "src/main/java/com/cashier/controller/PurchaseOrderController.java",
            "src/main/java/com/cashier/controller/PurchaseInboundController.java",
            "src/main/java/com/cashier/controller/PurchaseApprovalController.java",
            "src/main/java/com/cashier/controller/SupplierController.java",
            "src/main/java/com/cashier/controller/TransactionController.java",
            "src/main/java/com/cashier/controller/InventoryCheckController.java",
            "src/main/java/com/cashier/controller/UserController.java",
            "src/main/java/com/cashier/controller/StatisticsController.java",
            "src/main/java/com/cashier/controller/InventoryReportController.java",
            "src/main/java/com/cashier/controller/PurchaseReportController.java",
            "src/main/java/com/cashier/controller/ProfitReportController.java"
        );

        for (String file : controllerFiles) {
            String controller = Files.readString(Path.of(file));
            assertTrue(controller.contains("StatusBarManager.updateError(message)")
                    || controller.contains("FXUtils.showError(message)"),
                file + " 的 showError 应同步状态栏错误级别");
        }
    }

    @Test
    @DisplayName("高频成功弹窗应同步状态栏成功级别")
    void highFrequencySuccessDialogsUpdateStatusBar() throws Exception {
        String cartController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));
        String settingsController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/SettingsController.java"
        ));
        String shiftController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ShiftController.java"
        ));

        assertTrue(cartController.contains("StatusBarManager.updateSuccess(message)"));
        assertTrue(settingsController.contains("StatusBarManager.updateSuccess(message)"));
        assertTrue(shiftController.contains("StatusBarManager.updateSuccess(message)"));
    }

    private void assertSeverityBinding(String controllerSource) {
        assertTrue(controllerSource.contains("StatusBarManager.statusLevelProperty().addListener"));
        assertTrue(controllerSource.contains("applyStatusLevelStyle"));
        assertTrue(controllerSource.contains("\"text-success\""));
        assertTrue(controllerSource.contains("\"text-warning\""));
        assertTrue(controllerSource.contains("\"text-danger\""));
    }
}
