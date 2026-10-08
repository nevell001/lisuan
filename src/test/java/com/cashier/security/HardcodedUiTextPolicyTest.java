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
 *
 * <p><b>已知覆盖边界（2026-10 实测，务必知道）</b>：本门禁只看**可见调用点的实参里**的中文字面量
 * （含拼接/三元/续行，见 {@code collectUiCallLiterals}）。因此"中文在别处拼好、再作为变量/返回值
 * 传进来显示"的形状抓不到，实测两类都不变红：① 辅助方法返回（{@code formatDuration} 改回
 * {@code hours + "小时"}）；② 变量拼接后显示（{@code RechargeController.isInputValid} 的
 * {@code errorMessage += "充值金额不能为空！"} 与 {@code MainController.handleAbout} 的 about 正文）。
 * 这两类目前靠**逐文件定点锚点**兜（见下面 {@code inventoryAlert*} 两项），
 * 剩余未迁文件的实测清单登记在 {@code docs/TECH_DEBT.md} 的 TD-014/F14 附录。</p>
 */
@DisplayName("界面文案硬编码门禁")
class HardcodedUiTextPolicyTest {

    /** 已完成文案迁移、需要守住的文件。 */
    private static final List<String> MIGRATED_FILES = List.of(
        "src/main/java/com/cashier/controller/MainController.java",
        "src/main/java/com/cashier/controller/RechargeController.java",
        "src/main/java/com/cashier/controller/InventoryAlertController.java",
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

    /**
     * 面向用户的调用（只匹配调用头，实参形状不设限）。参数里的中文**字面量**一律算硬编码，
     * 除非它是 i18n key（{@code get("...")} 的字面量，key 本身可能含中文，如 "快捷键.title"）。
     *
     * <p>为什么改成"平衡括号取全部实参"：旧写法要求中文**紧跟左括号**，于是
     * ① {@code showErrorAlert(title, "中文正文")} 的第二实参、② {@code setTitle(常量 + "中文")}
     * 这类拼接写法全都漏掉了——2026-10 审计实测：白名单文件里仍有 3 处可见中文，门禁却是绿的。</p>
     */
    private static final Pattern UI_CALL_HEAD = Pattern.compile(
        "\\.?(setTitle|setHeaderText|setContentText|setPromptText|setTooltipText|setText|showErrorAlert|"
            + "showInfoAlert|showWarningAlert|showError|showWarning|showInformation|showPlaceholder|"
            + "showConfirm|updateStatus|updateWarning|updateSuccess|updateError|updateInfo)\\s*\\(");

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
            if (file.endsWith(".fxml")) {
                collect(text, FXML_TEXT, file, violations, 2);
                continue;
            }
            collectUiCallLiterals(text, file, violations);
            collect(text, NEW_CONTROL, file, violations, 2);
        }
        assertTrue(violations.isEmpty(),
            "以下界面文案仍硬编码中文（切到 en/zh_TW 后不会翻译；日志与注释里的中文不算）：\n  "
                + String.join("\n  ", violations));
    }

    /**
     * 逐个可见调用取**完整实参列表**（平衡括号，跳过字符串与行注释），
     * 再检查实参里的每个中文字面量；i18n key 本身不算硬编码。
     */
    private static void collectUiCallLiterals(String text, String file, List<String> violations) {
        Matcher call = UI_CALL_HEAD.matcher(text);
        int checked = 0;
        while (call.find()) {
            int open = text.indexOf('(', call.start());
            int end = matchingParen(text, open);
            if (open < 0 || end < 0) {
                continue;
            }
            checked++;
            String arguments = text.substring(open + 1, end);
            for (String literal : stringLiterals(arguments)) {
                if (!CJK.matcher(literal).find()) {
                    continue;
                }
                // i18n key（get("...") / getOrDefault("...") 的字面量）不是可见文案
                if (arguments.contains("get(\"" + literal + "\"")
                        || arguments.contains("getOrDefault(\"" + literal + "\"")) {
                    continue;
                }
                int line = text.substring(0, open).split("\n", -1).length;
                violations.add(file + ":" + line + "  →  " + literal);
            }
        }
        // 防空转：解析规则失效时必须报出来，而不是静默 0 违规
        assertTrue(checked > 0, file + " 未识别到任何可见调用，门禁解析规则可能失效");
    }

    @Test
    @DisplayName("库存预警级别展示名必须走 i18n（枚举常量不得缓存中文文案）")
    void inventoryAlertLevelNamesAreLocalized() throws Exception {
        // 上面那条规则只看"面向用户的调用点"，抓不到"枚举常量里存中文、再被赋给单元格"这种形状：
        // AlertLevel 一旦把「严重警告/警告/提示」写回常量，切语言后那一列就不再翻译（2026-10 F14）
        String text = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryAlertController.java"));

        assertTrue(text.contains("get(\"inventory_alert.critical\")")
                && text.contains("get(\"inventory_alert.warning\")")
                && text.contains("get(\"inventory_alert.info\")"),
            "预警级别展示名必须在 getDisplayName() 里按当前语言解析，不能写进枚举常量");
    }

    @Test
    @DisplayName("库存预警页的时长文案必须走 i18n（辅助方法返回再显示的文案门禁看不见）")
    void inventoryAlertDurationTextsAreLocalized() throws Exception {
        // 变异实测（2026-10 F14）：把 formatDuration 改回 hours + "小时" **不会**让上面那条
        // "调用点实参"规则变红——字面量在 return 语句里，不在 setText 的实参里。补定点锚点。
        String text = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryAlertController.java"));

        assertTrue(text.contains("get(\"inventory_alert.duration_hours\", hours)")
                && text.contains("get(\"inventory_alert.duration_minutes\", minutes)")
                && text.contains("get(\"inventory_alert.duration_seconds\", seconds)"),
            "时长文案必须走带参的 i18n key");
        assertFalse(text.contains("hours + \"小时\"") || text.contains("minutes + \"分钟\"")
                || text.contains("seconds + \"秒\""),
            "时长拼接回硬编码中文会绕过调用点规则，不得回归");
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

    /**
     * 状态栏文案不得硬编码——**全仓库**、**跨行/表达式**都算。
     *
     * <p>为什么单列一条：此前这条规则只扫已迁移文件里的 `updateStatus/updateWarning("中文…")`，
     * 而实际漏了两种写法——① 中文写在**续行**（`updateSuccess("商品删除成功: "\n + name)`）；
     * ② 第一实参是**三元/拼接表达式**。于是"全仓库状态栏硬编码为 0"这个结论是错的，
     * 现在改为：对 `src/main` 全量扫描状态栏调用，跳过注释后用**字符串字面量**判中文
     * （i18n key 全是 ASCII，所以参数里出现中文字面量必是硬编码）。</p>
     *
     * <p>注意 `StatusBarManager.LEGACY_STATUS_KEYS` 那张"中文串 → key"的兼容映射**不在**本规则范围内：
     * 它是兜底翻译表（不是调用实参），但它的存在会让这类硬编码在运行时被翻译，
     * 从而掩盖问题——所以调用方应直接传 key，不要依赖它。</p>
     */
    @Test
    @DisplayName("状态栏文案不得硬编码（全仓库、跨行表达式）")
    void statusBarTextIsNeverHardcodedRepoWide() throws IOException {
        List<String> violations = new ArrayList<>();
        int checked = 0;
        for (Path file : mainJavaSources()) {
            String text = Files.readString(file);
            Matcher call = STATUS_BAR_CALL.matcher(text);
            while (call.find()) {
                int open = text.indexOf('(', call.start());
                int end = matchingParen(text, open);
                if (end < 0) {
                    continue;
                }
                checked++;
                String arguments = text.substring(open + 1, end);
                for (String literal : stringLiterals(arguments)) {
                    if (CJK.matcher(literal).find()) {
                        int line = text.substring(0, open).split("\n", -1).length;
                        violations.add(relative(file) + ":" + line + "  →  " + literal);
                    }
                }
            }
        }
        assertTrue(checked > 50, "只扫到 " + checked + " 处状态栏调用（2026-09 实际 97 处），识别规则可能失效（本门禁会变空转）");
        assertTrue(violations.isEmpty(),
            "状态栏文案仍是硬编码中文（切到 en/zh_TW 后不会翻译；日志与注释里的中文不算）。"
                + "请改为 `I18nManager.getInstance().get(\"key\")` 或复用既有 key：\n  "
                + String.join("\n  ", violations));
    }

    /** 状态栏写入入口（含 touch POS 用的 updateSuccess/Error/Warning）。 */
    private static final Pattern STATUS_BAR_CALL =
        Pattern.compile("\\.(updateStatus|updateSuccess|updateWarning|updateError|updateInfo)\\s*\\(");

    /** 配对括号：跳过字符串字面量与注释，避免 `get(\"a (b)\")` 之类误判。 */
    private static int matchingParen(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                i = skipString(text, i);
            } else if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                i = text.indexOf('\n', i);
                if (i < 0) {
                    return -1;
                }
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int skipString(String text, int start) {
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            } else if (c == '\n') {
                return i - 1;
            }
        }
        return text.length() - 1;
    }

    /** 取出一段代码里的字符串字面量内容（跳过注释）。 */
    private static List<String> stringLiterals(String code) {
        List<String> literals = new ArrayList<>();
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                int nl = code.indexOf('\n', i);
                i = nl < 0 ? code.length() : nl;
            } else if (c == '"') {
                int end = skipString(code, i);
                literals.add(code.substring(i + 1, Math.max(i + 1, end)));
                i = end;
            }
        }
        return literals;
    }

    private static List<Path> mainJavaSources() throws IOException {
        List<Path> files = new ArrayList<>();
        try (var walk = Files.walk(Path.of("src/main/java"))) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
        }
        assertFalse(files.isEmpty(), "未找到 Java 源码，路径不对？");
        return files;
    }

    private static String relative(Path file) {
        return "src/main/java/" + Path.of("src/main/java").relativize(file);
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
