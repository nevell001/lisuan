package com.cashier.util;

import com.cashier.service.DataService;
import org.slf4j.Logger;

import java.util.Map;

/**
 * 密码策略的唯一实现：所有设置/修改密码的入口（首次运行向导、首次登录强制改密、
 * 桌面新建用户/重置密码、REST 建号/改密）共用同一套判定，避免各入口各写一份而漂移。
 */
public final class PasswordPolicy {

    public static final int DEFAULT_MIN_LENGTH = 6;
    public static final boolean DEFAULT_REQUIRE_COMPLEXITY = true;

    private static final String SETTING_MIN_LENGTH = "passwordMinLength";
    private static final String SETTING_REQUIRE_COMPLEXITY = "passwordComplexity";
    private static final Logger logger = LoggerFactoryUtil.getLogger(PasswordPolicy.class);

    public enum Violation {
        EMPTY,
        TOO_SHORT,
        NEEDS_LETTER_AND_DIGIT
    }

    public record Settings(int minLength, boolean requireComplexity) {

        public Settings {
            if (minLength < 1) {
                minLength = DEFAULT_MIN_LENGTH;
            }
        }

        /**
         * @return 不满足的规则；满足时返回 {@code null}
         */
        public Violation check(String password) {
            if (password == null || password.isEmpty()) {
                return Violation.EMPTY;
            }
            if (password.length() < minLength) {
                return Violation.TOO_SHORT;
            }
            if (requireComplexity
                && !(password.matches(".*[a-zA-Z].*") && password.matches(".*\\d.*"))) {
                return Violation.NEEDS_LETTER_AND_DIGIT;
            }
            return null;
        }

        public boolean isValid(String password) {
            return check(password) == null;
        }

        public String hintKey() {
            return requireComplexity
                ? "firstrun.password_policy_complexity" : "firstrun.password_policy";
        }
    }

    public static Settings defaults() {
        return new Settings(DEFAULT_MIN_LENGTH, DEFAULT_REQUIRE_COMPLEXITY);
    }

    public static Settings fromSettings(Map<String, String> settings) {
        if (settings == null) {
            return defaults();
        }
        int minLength = DEFAULT_MIN_LENGTH;
        try {
            minLength = Integer.parseInt(
                settings.getOrDefault(SETTING_MIN_LENGTH, String.valueOf(DEFAULT_MIN_LENGTH)).trim());
        } catch (NumberFormatException e) {
            minLength = DEFAULT_MIN_LENGTH;
        }
        boolean requireComplexity = Boolean.parseBoolean(
            settings.getOrDefault(SETTING_REQUIRE_COMPLEXITY, String.valueOf(DEFAULT_REQUIRE_COMPLEXITY)));
        return new Settings(minLength, requireComplexity);
    }

    /**
     * 读取系统设置中的密码策略；设置读不出来时回退默认值，不能把用户挡在改密/建号之外。
     */
    public static Settings load() {
        try {
            return fromSettings(DataService.loadSettings());
        } catch (RuntimeException e) {
            logger.warn("读取密码策略失败，使用默认值", e);
            return defaults();
        }
    }

    private PasswordPolicy() {
    }
}
