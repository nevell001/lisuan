package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 界面可见文案不得硬编码（TD-014）。
 *
 * <p>i18n 的一致性门禁（{@code I18nBundleConsistencyTest}）只保证"key 都在四份语言包里"，
 * 查不出**硬编码**：切到 en / zh_TW 后这些文案仍是中文。本门禁针对已迁移过的文件，
 * 只检查**面向用户的调用位置**——日志（{@code logger.*}）与注释里的中文是允许的，
 * 项目本来就以中文写日志。</p>
 *
 * <p>覆盖范围是逐步扩大的：下面 {@code MIGRATED_FILES} 里的文件都应保持"零硬编码可见文案"，
 * 新迁移一个文件就往列表里加一个。</p>
 */
@DisplayName("界面文案硬编码门禁")
class HardcodedUiTextPolicyTest {

    /** 已完成文案迁移、需要守住的文件。 */
    private static final List<String> MIGRATED_FILES = List.of(
        "src/main/java/com/cashier/controller/MainController.java",
        "src/main/java/com/cashier/controller/RechargeController.java",
        "src/main/java/com/cashier/printer/PrintPreviewDialog.java",
        "src/main/java/com/cashier/SplashWindow.java",
        "src/main/java/com/cashier/CashierSystemFXApplication.java",
        "src/main/java/com/cashier/controller/PurchaseApprovalController.java",
        "src/main/java/com/cashier/controller/PurchaseInboundController.java",
        "src/main/java/com/cashier/controller/PurchaseOrderController.java",
        "src/main/java/com/cashier/controller/SettingsController.java",
        "src/main/java/com/cashier/controller/ShiftController.java",
        "src/main/java/com/cashier/controller/SupplierController.java",
        "src/main/java/com/cashier/controller/TransactionController.java",
        "src/main/resources/com/cashier/view/InventoryView.fxml");

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    /**
     * 设计期占位文案、运行时必被覆盖的 FXML 文本（都在 TouchCartView 上，控制器启动时 setText）。
     * 登记在这里而不是改成 %key：它们是预览用的假数据，改成语言包只会增加无意义的 key。
     * 每条都必须指得出覆盖它的那行代码。
     */
    private static final List<String> RUNTIME_OVERWRITTEN_FXML_TEXT = List.of(
        "TouchCartView.fxml:便利店",            // storeNameLabel ← TouchCartController:559 用设置里的店名覆盖
        "TouchCartView.fxml:2026-08-23 星期日"); // dateLabel     ← TouchCartController:296 用当前日期覆盖

    /** 面向用户的调用：标题/正文/按钮/提示/弹窗内容等，参数是字符串字面量时不得含中文。 */
    private static final Pattern UI_CALL = Pattern.compile(
        "(setTitle|setHeaderText|setContentText|setPromptText|setTooltipText|setText|showErrorAlert|"
            + "showInfoAlert|showWarningAlert|showError|showWarning|showInformation|showPlaceholder|"
            + "showConfirm|updateStatus|updateWarning)\\s*\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** 直接构造控件时传入的可见文案。 */
    private static final Pattern NEW_CONTROL = Pattern.compile(
        "new\\s+(Label|Button|CheckBox|RadioButton|MenuItem|TableColumn|TitledPane|Tab)\\s*"
            + "(?:<[^>]*>)?\\s*\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** FXML 里的可见文案属性。 */
    private static final Pattern FXML_TEXT = Pattern.compile(
        "(text|promptText|title|headerText)\\s*=\\s*\"([^\"]*)\"");

    @Test
    @DisplayName("已迁移文件里不得再出现硬编码的中文界面文案")
    void migratedFilesHaveNoHardcodedChineseUiText() throws Exception {
        List<String> violations = new ArrayList<>();
        for (String file : MIGRATED_FILES) {
            String text = Files.readString(Path.of(file));
            Pattern pattern = file.endsWith(".fxml") ? FXML_TEXT : UI_CALL;
            collect(text, pattern, file, violations, 2);
            if (!file.endsWith(".fxml")) {
                collect(text, NEW_CONTROL, file, violations, 2);
            }
        }
        assertTrue(violations.isEmpty(),
            "以下界面文案仍硬编码中文（切到 en/zh_TW 后不会翻译；日志与注释里的中文不算）：\n  "
                + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("FXML 的可见文案必须走 %key，不得写死中文")
    void fxmlVisibleTextUsesResourceKeys() throws Exception {
        // 全量扫描视图目录：可见文案属性要么是 %key，要么不含中文（图标/占位符/技术串）
        List<String> violations = new ArrayList<>();
        try (var walk = Files.walk(Path.of("src/main/resources/com/cashier/view"))) {
            for (Path fxml : walk.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                String text = Files.readString(fxml);
                Matcher matcher = FXML_TEXT.matcher(text);
                while (matcher.find()) {
                    String value = matcher.group(2);
                    if (RUNTIME_OVERWRITTEN_FXML_TEXT.contains(fxml.getFileName() + ":" + value)) {
                        continue; // 设计期占位，运行时会被覆盖（见常量注释）
                    }
                    if (!value.startsWith("%") && CJK.matcher(value).find()) {
                        int line = text.substring(0, matcher.start()).split("\n", -1).length;
                        violations.add(fxml + ":" + line + "  →  " + matcher.group(1) + "=\"" + value + "\"");
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(),
            "FXML 里的可见文案必须写成 %resource.key（缺 key 时界面会直接显示 key，属 UI 缺陷）：\n  "
                + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("迁移过的文案 key 必须真的存在于四份语言包")
    void migratedKeysExistInEveryBundle() throws Exception {
        // 抽查本次迁移用到的 key：任一份语言包缺失都会让该语言下显示成 key 本身
        List<String> bundles = List.of("messages.properties", "messages_zh_CN.properties",
            "messages_zh_TW.properties", "messages_en.properties");
        List<String> keys = List.of(
            // 本轮新增并保留的文案 key
            "runtime.backup_in_progress", "runtime.backup_failed", "runtime.backup_none_found",
            "runtime.restore_in_progress", "runtime.restore_failed",
            "runtime.print_preview", "runtime.print_button",
            "runtime.splash_starting", "runtime.splash_initializing", "runtime.splash_loading_data",
            "runtime.splash_starting_services", "runtime.splash_finishing",
            "runtime.startup_failed_title", "runtime.startup_failed_detail",
            "runtime.ui_font_missing_title", "runtime.ui_font_missing_detail",
            "runtime.feature_search_in_development", "runtime.feature_edit_in_development",
            "runtime.feature_batch_in_development", "runtime.feature_export_in_development",
            "runtime.in_development_title", "runtime.shift_handover",
            // 状态栏改为复用的既有 key（本轮去重：同值不再新增 key）
            "menu.data.backup", "menu.data.restore", "status_message.data_saved", "status.ready",
            "status_message.refreshed", "status_message.refresh_failed", "status_message.refreshed_item",
            "status_message.export_data", "status_message.inventory_alert",
            "status_message.no_refresh_needed", "status_message.theme_light",
            "status_message.theme_dark", "status_message.theme_lisuan",
            "nav.return_report", "nav.return_approval");
        for (String bundle : bundles) {
            String path = "src/main/resources/com/cashier/i18n/" + bundle;
            String text = Files.readString(Path.of(path));
            for (String key : keys) {
                assertTrue(text.contains("\n" + key + "=") || text.startsWith(key + "="),
                    path + " 缺少 key: " + key);
            }
        }
    }

    @Test
    @DisplayName("打印预览对话框的按钮文案不得写死")
    void printPreviewButtonsUseI18n() throws Exception {
        String text = Files.readString(Path.of("src/main/java/com/cashier/printer/PrintPreviewDialog.java"));
        assertFalse(text.contains("new Button(\"打印\")") || text.contains("new Button(\"取消\")"),
            "打印预览的按钮必须走 i18n");
        assertTrue(text.contains("new Button(I18nManager.getInstance().get("),
            "打印预览按钮应通过 I18nManager 取名");
    }

    private static void collect(String text, Pattern pattern, String file, List<String> violations, int group) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (CJK.matcher(matcher.group(group)).find()) {
                int line = text.substring(0, matcher.start()).split("\n", -1).length;
                violations.add(file + ":" + line + "  →  " + matcher.group(group - 1) + " 含中文: "
                    + matcher.group(group));
            }
        }
    }
}
