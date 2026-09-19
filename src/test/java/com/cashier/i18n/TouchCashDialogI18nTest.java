package com.cashier.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 触屏收银台「现金支付弹窗」的 i18n 门禁。
 *
 * <p>回归：弹窗里的「应付金额 / 已付 / 还需 / 找零 / 精确金额 / 清除 / 确认收款」等文案一度是
 * 硬编码中文，触屏收银台有语言切换按钮，切到 en / zh_TW 后弹窗仍是中文。</p>
 */
@DisplayName("触屏现金弹窗 i18n 测试")
class TouchCashDialogI18nTest {

    private static final List<String> CASH_DIALOG_KEYS = List.of(
        I18nKeys.Tpos.CASH_AMOUNT_DUE_LABEL,
        I18nKeys.Tpos.CASH_EXACT_AMOUNT,
        I18nKeys.Tpos.CASH_CLEAR_AMOUNT,
        I18nKeys.Tpos.CASH_CONFIRM_RECEIPT,
        I18nKeys.Tpos.CASH_SECTION_AMOUNT_RECEIVED,
        I18nKeys.Tpos.CASH_SECTION_QUICK_AMOUNT,
        I18nKeys.Tpos.CASH_PARTIAL_PAYMENT_HINT,
        I18nKeys.Runtime.AMOUNT_PAID,
        I18nKeys.Runtime.AMOUNT_REMAINING,
        I18nKeys.Runtime.PAYMENT_AMOUNT_HINT,
        I18nKeys.Runtime.CHANGE_AMOUNT,
        I18nKeys.Runtime.INVALID_AMOUNT);

    /** 带 {0} 占位符的键：既要能格式化，也要三种语言都有。 */
    private static final List<String> PARAMETERIZED_KEYS = List.of(
        I18nKeys.Tpos.CASH_PARTIAL_PAYMENT_HINT,
        I18nKeys.Runtime.AMOUNT_PAID,
        I18nKeys.Runtime.AMOUNT_REMAINING,
        I18nKeys.Runtime.CHANGE_AMOUNT);

    private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    @Test
    @DisplayName("现金弹窗文案在简中/繁中/英文下都有译文")
    void cashDialogTextIsTranslatedInAllLocales() {
        I18nManager i18n = I18nManager.getInstance();

        for (String key : CASH_DIALOG_KEYS) {
            String zh = i18n.get(Locale.SIMPLIFIED_CHINESE, key);
            String tw = i18n.get(Locale.TRADITIONAL_CHINESE, key);
            String en = i18n.get(Locale.ENGLISH, key);

            for (String text : List.of(zh, tw, en)) {
                assertFalse(text == null || text.isBlank() || text.equals(key),
                    "文案缺失（界面会直接显示 key）: " + key);
            }
            assertNotEquals(zh, en, key + " 英文与简体中文相同，疑似未翻译");
            assertNotEquals(tw, en, key + " 英文与繁体中文相同，疑似未翻译");
        }
    }

    @Test
    @DisplayName("带占位符的文案能正确格式化")
    void parameterizedTextFormats() {
        I18nManager i18n = I18nManager.getInstance();

        for (String key : PARAMETERIZED_KEYS) {
            String zh = i18n.get(key, "30.00");
            assertTrue(zh.contains("30.00"), key + " 未代入金额: " + zh);
            assertFalse(zh.contains("{0}"), key + " 占位符未被替换: " + zh);
        }
    }

    @Test
    @DisplayName("触屏现金弹窗不得再硬编码中文文案")
    void cashDialogHasNoHardcodedChinese() throws Exception {
        String factory = Files.readString(
            Path.of("src/main/java/com/cashier/controller/TouchCartViewFactory.java"));
        String touch = Files.readString(
            Path.of("src/main/java/com/cashier/controller/TouchCartController.java"));

        // 视图工厂里所有字符串字面量都不得含中文（注释不算）
        Matcher literals = STRING_LITERAL.matcher(factory);
        while (literals.find()) {
            String literal = literals.group(1);
            assertFalse(CJK.matcher(literal).find(),
                "TouchCartViewFactory 仍硬编码中文文案（应走 I18nManager）: \"" + literal + "\"");
        }

        // 曾经的硬编码写法，一旦回归弹窗在 en/zh_TW 下又会显示中文
        for (String hardcoded : List.of(
            "\"应付金额\"", "\"已付 \"", "\"还需 \"", "\"还需支付 \"", "\"找零 \"",
            "\"请输入收款金额\"", "\"精确金额\"", "\"清除 C\"", "\"确认收款 (Enter)\"",
            "\"收款成功！还需: \"", "\"收款金额\"", "\"快捷金额\"", "\"请输入有效的金额\"")) {
            assertFalse(touch.contains(hardcoded), "触屏现金流程不得再硬编码: " + hardcoded);
            assertFalse(factory.contains(hardcoded), "触屏现金弹窗不得再硬编码: " + hardcoded);
        }
    }
}
