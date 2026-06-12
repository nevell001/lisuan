package com.cashier.util;

import javafx.scene.control.*;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 表单验证工具类
 * 提供实时表单验证功能
 */
public class FormValidator {
    private static final Logger logger = LoggerFactoryUtil.getLogger(FormValidator.class);

    /**
     * 验证规则
     */
    public static class ValidationRule {
        private final String name;
        private final Predicate<String> validator;
        private final String errorMessage;

        public ValidationRule(String name, Predicate<String> validator, String errorMessage) {
            this.name = name;
            this.validator = validator;
            this.errorMessage = errorMessage;
        }

        public String getName() {
            return name;
        }

        public boolean test(String value) {
            return validator.test(value);
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 常用验证规则
     */
    public static class Rules {
        /** 非空验证 */
        public static final ValidationRule NOT_EMPTY = new ValidationRule(
                "not_empty",
                value -> value != null && !value.trim().isEmpty(),
                "此字段不能为空"
        );

        /** 手机号验证（中国大陆） */
        public static final ValidationRule PHONE = new ValidationRule(
                "phone",
                value -> value == null || value.isEmpty() || Pattern.matches("^1[3-9]\\d{9}$", value),
                "请输入正确的手机号"
        );

        /** 邮箱验证 */
        public static final ValidationRule EMAIL = new ValidationRule(
                "email",
                value -> value == null || value.isEmpty() || Pattern.matches(
                        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$", value),
                "请输入正确的邮箱地址"
        );

        /** 金额验证 */
        public static final ValidationRule AMOUNT = new ValidationRule(
                "amount",
                value -> value == null || value.isEmpty() ||
                        Pattern.matches("^(0|[1-9]\\d*)(\\.\\d{1,2})?$", value),
                "请输入正确的金额"
        );

        /** 数量验证 */
        public static final ValidationRule QUANTITY = new ValidationRule(
                "quantity",
                value -> value == null || value.isEmpty() ||
                        Pattern.matches("^(0|[1-9]\\d*)$", value),
                "请输入正确的数量"
        );

        /** 价格验证 */
        public static final ValidationRule PRICE = new ValidationRule(
                "price",
                value -> value == null || value.isEmpty() ||
                        Pattern.matches("^(0|[1-9]\\d*)(\\.\\d{1,2})?$", value),
                "请输入正确的价格"
        );

        /** 折扣验证 */
        public static final ValidationRule DISCOUNT = new ValidationRule(
                "discount",
                value -> value == null || value.isEmpty() ||
                        Pattern.matches("^([0-9]|10)(\\.\\d)?$", value),
                "请输入0-10之间的折扣值"
        );

        /** 百分比验证 */
        public static final ValidationRule PERCENTAGE = new ValidationRule(
                "percentage",
                value -> value == null || value.isEmpty() ||
                        Pattern.matches("^(100|[1-9]?\\d?)(\\.\\d{1,2})?%?$", value),
                "请输入正确的百分比(0-100)"
        );

        /** 密码验证 */
        public static final ValidationRule PASSWORD = new ValidationRule(
                "password",
                value -> value != null && value.length() >= 6,
                "密码至少6位"
        );

        /** 长度验证 */
        public static ValidationRule length(int min, int max) {
            return new ValidationRule(
                    "length",
                    value -> value == null || value.length() >= min && value.length() <= max,
                    String.format("长度必须在%d到%d之间", min, max)
            );
        }

        /** 自定义正则验证 */
        public static ValidationRule regex(String regex, String errorMessage) {
            return new ValidationRule(
                    "regex_" + regex.hashCode(),
                    value -> value == null || Pattern.matches(regex, value),
                    errorMessage
            );
        }
    }

    /**
     * 验证字段并更新 UI 状态
     * @param control 输入控件
     * @param errorLabel 错误提示标签
     * @param rules 验证规则
     * @return 验证结果
     */
    public static ValidationResult validate(TextField control, Label errorLabel, ValidationRule... rules) {
        if (control == null) {
            return new ValidationResult(false, "控件不能为空");
        }

        String value = control.getText();

        // 清除之前的错误样式
        control.getStyleClass().remove("validation-error");
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }

        // 如果值为空且非空验证，返回有效（延迟验证）
        if ((value == null || value.trim().isEmpty()) && !hasRequiredRule(rules)) {
            return new ValidationResult(true, "");
        }

        // 执行验证
        for (ValidationRule rule : rules) {
            if (!rule.test(value)) {
                // 显示错误状态
                control.getStyleClass().add("validation-error");
                if (errorLabel != null) {
                    errorLabel.setText(rule.getErrorMessage());
                    errorLabel.setVisible(true);
                }
                return new ValidationResult(false, rule.getErrorMessage());
            }
        }

        return new ValidationResult(true, "");
    }

    /**
     * 验证 TextArea
     */
    public static ValidationResult validate(TextArea control, Label errorLabel, ValidationRule... rules) {
        if (control == null) {
            return new ValidationResult(false, "控件不能为空");
        }

        String value = control.getText();

        // 清除之前的错误样式
        control.getStyleClass().remove("validation-error");
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }

        // 如果值为空且非空验证，返回有效
        if ((value == null || value.trim().isEmpty()) && !hasRequiredRule(rules)) {
            return new ValidationResult(true, "");
        }

        // 执行验证
        for (ValidationRule rule : rules) {
            if (!rule.test(value)) {
                control.getStyleClass().add("validation-error");
                if (errorLabel != null) {
                    errorLabel.setText(rule.getErrorMessage());
                    errorLabel.setVisible(true);
                }
                return new ValidationResult(false, rule.getErrorMessage());
            }
        }

        return new ValidationResult(true, "");
    }

    /**
     * 验证 ComboBox
     */
    public static ValidationResult validate(ComboBox<?> control, Label errorLabel, ValidationRule... rules) {
        if (control == null) {
            return new ValidationResult(false, "控件不能为空");
        }

        Object value = control.getValue();

        // 清除之前的错误样式
        control.getStyleClass().remove("validation-error");
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }

        // 如果值为空且非空验证，返回有效
        if (value == null && !hasRequiredRule(rules)) {
            return new ValidationResult(true, "");
        }

        String valueStr = value != null ? value.toString() : "";

        // 执行验证
        for (ValidationRule rule : rules) {
            if (!rule.test(valueStr)) {
                control.getStyleClass().add("validation-error");
                if (errorLabel != null) {
                    errorLabel.setText(rule.getErrorMessage());
                    errorLabel.setVisible(true);
                }
                return new ValidationResult(false, rule.getErrorMessage());
            }
        }

        return new ValidationResult(true, "");
    }

    /**
     * 检查是否有必填规则
     */
    private static boolean hasRequiredRule(ValidationRule[] rules) {
        for (ValidationRule rule : rules) {
            if ("not_empty".equals(rule.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 为 TextField 添加实时验证
     * @param control 输入框
     * @param errorLabel 错误提示标签
     * @param rules 验证规则
     * @return 清理函数（用于移除监听器）
     */
    public static Runnable setupRealTimeValidation(TextField control, Label errorLabel, ValidationRule... rules) {
        if (control == null) {
            return () -> {};
        }

        // 焦点丢失时验证
        control.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validate(control, errorLabel, rules);
            }
        });

        // 文本变化时验证（仅在已有错误时）
        control.textProperty().addListener((obs, oldVal, newVal) -> {
            if (control.getStyleClass().contains("validation-error")) {
                validate(control, errorLabel, rules);
            }
        });

        // 返回清理函数
        return () -> {
            control.getStyleClass().remove("validation-error");
            if (errorLabel != null) {
                errorLabel.setVisible(false);
            }
        };
    }

    /**
     * 为 TextArea 添加实时验证
     */
    public static Runnable setupRealTimeValidation(TextArea control, Label errorLabel, ValidationRule... rules) {
        if (control == null) {
            return () -> {};
        }

        control.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validate(control, errorLabel, rules);
            }
        });

        control.textProperty().addListener((obs, oldVal, newVal) -> {
            if (control.getStyleClass().contains("validation-error")) {
                validate(control, errorLabel, rules);
            }
        });

        return () -> {
            control.getStyleClass().remove("validation-error");
            if (errorLabel != null) {
                errorLabel.setVisible(false);
            }
        };
    }

    /**
     * 为 ComboBox 添加实时验证
     */
    public static Runnable setupRealTimeValidation(ComboBox<?> control, Label errorLabel, ValidationRule... rules) {
        if (control == null) {
            return () -> {};
        }

        control.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validate(control, errorLabel, rules);
            }
        });

        control.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (control.getStyleClass().contains("validation-error")) {
                validate(control, errorLabel, rules);
            }
        });

        return () -> {
            control.getStyleClass().remove("validation-error");
            if (errorLabel != null) {
                errorLabel.setVisible(false);
            }
        };
    }

    /**
     * 添加文本格式化器
     */
    public static void setTextFormatter(TextField field, TextFormatter<?> formatter) {
        field.setTextFormatter(formatter);
    }

    /**
     * 添加数字限制
     */
    public static void setNumberOnly(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));
    }

    /**
     * 添加小数限制
     */
    public static void setDecimalOnly(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*\\.?\\d*")) {
                return change;
            }
            return null;
        }));
    }

    /**
     * 添加长度限制
     */
    public static void setMaxLength(TextField field, int maxLength) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > maxLength) {
                field.setText(oldVal);
            }
        });
    }

    /**
     * 添加必填标记
     */
    public static void markRequired(TextField field, Label label) {
        if (label != null) {
            label.setText(label.getText() + " *");
            label.getStyleClass().add("required-field");
        }
    }

    /**
     * 添加必填标记
     */
    public static void markRequired(TextArea field, Label label) {
        if (label != null) {
            label.setText(label.getText() + " *");
            label.getStyleClass().add("required-field");
        }
    }

    /**
     * 安全解析 Double
     * @param value 字符串值
     * @param defaultValue 默认值
     * @return 解析后的值或默认值
     */
    public static double parseDouble(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("无法解析为数字: {}, 使用默认值: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 安全解析 Double（非空）
     * @param value 字符串值
     * @return 解析后的值
     * @throws IllegalArgumentException 如果解析失败
     */
    public static double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("值不能为空");
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的数字: " + value);
        }
    }

    /**
     * 安全解析 Integer
     * @param value 字符串值
     * @param defaultValue 默认值
     * @return 解析后的值或默认值
     */
    public static int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("无法解析为整数: {}, 使用默认值: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 安全解析 Integer（非空）
     * @param value 字符串值
     * @return 解析后的值
     * @throws IllegalArgumentException 如果解析失败
     */
    public static int parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("值不能为空");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的整数: " + value);
        }
    }

    /**
     * 安全解析 Long
     * @param value 字符串值
     * @param defaultValue 默认值
     * @return 解析后的值或默认值
     */
    public static long parseLong(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("无法解析为长整数: {}, 使用默认值: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 安全解析 Long（非空）
     * @param value 字符串值
     * @return 解析后的值
     * @throws IllegalArgumentException 如果解析失败
     */
    public static long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("值不能为空");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的长整数: " + value);
        }
    }
}
