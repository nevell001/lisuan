package com.cashier.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("密码策略")
class PasswordPolicyTest {

    private static final PasswordPolicy.Settings DEFAULT = PasswordPolicy.defaults();

    @Test
    @DisplayName("默认策略：至少 6 位且字母+数字都需要")
    void defaultPolicyChecksLengthAndComplexity() {
        assertTrue(DEFAULT.isValid("abc123"));
        assertEquals(PasswordPolicy.Violation.TOO_SHORT, DEFAULT.check("abc12"));
        assertEquals(PasswordPolicy.Violation.NEEDS_LETTER_AND_DIGIT, DEFAULT.check("abcdef"));
        assertEquals(PasswordPolicy.Violation.NEEDS_LETTER_AND_DIGIT, DEFAULT.check("123456"));
        assertEquals(PasswordPolicy.Violation.EMPTY, DEFAULT.check(""));
        assertEquals(PasswordPolicy.Violation.EMPTY, DEFAULT.check(null));
        assertNull(DEFAULT.check("abc123"));
    }

    @Test
    @DisplayName("关闭复杂度后只校验长度")
    void withoutComplexityOnlyLengthMatters() {
        PasswordPolicy.Settings settings = new PasswordPolicy.Settings(4, false);
        assertTrue(settings.isValid("abcd"));
        assertEquals(PasswordPolicy.Violation.TOO_SHORT, settings.check("abc"));
        assertEquals("firstrun.password_policy", settings.hintKey());
    }

    @Test
    @DisplayName("开启复杂度时提示 key 用带复杂度的文案")
    void complexityHintKey() {
        assertEquals("firstrun.password_policy_complexity", DEFAULT.hintKey());
    }

    @Test
    @DisplayName("从系统设置解析：缺省/非法值回退默认")
    void fromSettingsFallsBackToDefaults() {
        assertEquals(DEFAULT, PasswordPolicy.fromSettings(null));
        assertEquals(DEFAULT, PasswordPolicy.fromSettings(Map.of()));

        PasswordPolicy.Settings parsed = PasswordPolicy.fromSettings(Map.of(
            "passwordMinLength", "10", "passwordComplexity", "false"));
        assertEquals(new PasswordPolicy.Settings(10, false), parsed);

        assertEquals(DEFAULT, PasswordPolicy.fromSettings(Map.of("passwordMinLength", "not-a-number")));
        assertEquals(PasswordPolicy.DEFAULT_MIN_LENGTH,
            PasswordPolicy.fromSettings(Map.of("passwordMinLength", "0")).minLength());
        assertFalse(parsed.requireComplexity());
    }
}
