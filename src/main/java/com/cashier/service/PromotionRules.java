package com.cashier.service;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * 促销类型与金额规则。
 *
 * <p>数据库沿用中文类型（满减/打折/优惠券），REST API 对外使用稳定代码；
 * 金额/折扣的校验口径与桌面端一致，避免两套入口算出不同的优惠。</p>
 */
public final class PromotionRules {

    public static final String TYPE_REDUCTION = "满减";
    public static final String TYPE_DISCOUNT = "打折";
    public static final String TYPE_COUPON = "优惠券";

    public static final String CODE_REDUCTION = "REDUCTION";
    public static final String CODE_DISCOUNT = "DISCOUNT";
    public static final String CODE_COUPON = "COUPON";

    private PromotionRules() {
    }

    /** API 稳定代码 → 数据库中文类型；未知代码返回 {@code null}。 */
    public static String toLegacyType(String code) {
        if (code == null) {
            return null;
        }
        switch (code.trim().toUpperCase(Locale.ROOT)) {
            case CODE_REDUCTION:
                return TYPE_REDUCTION;
            case CODE_DISCOUNT:
                return TYPE_DISCOUNT;
            case CODE_COUPON:
                return TYPE_COUPON;
            default:
                return null;
        }
    }

    /** 数据库中文类型 → API 稳定代码；未知类型返回 {@code null}。 */
    public static String toCode(String legacyType) {
        if (legacyType == null) {
            return null;
        }
        switch (legacyType) {
            case TYPE_REDUCTION:
                return CODE_REDUCTION;
            case TYPE_DISCOUNT:
                return CODE_DISCOUNT;
            case TYPE_COUPON:
                return CODE_COUPON;
            default:
                return null;
        }
    }

    /** 本地化显示名 key；未知类型返回 {@code null}。 */
    public static String typeNameKey(String legacyType) {
        if (legacyType == null) {
            return null;
        }
        switch (legacyType) {
            case TYPE_REDUCTION:
                return "api.promotion.type.reduction";
            case TYPE_DISCOUNT:
                return "api.promotion.type.discount";
            case TYPE_COUPON:
                return "api.promotion.type.coupon";
            default:
                return null;
        }
    }

    /**
     * 校验折扣值（与桌面端 {@code validateDiscountValue} 同一口径）。
     *
     * @return 校验失败时返回带 i18n key 的问题；通过返回 {@code null}
     */
    public static ValidationIssue validateDiscount(String legacyType, BigDecimal threshold, BigDecimal discount) {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            return new ValidationIssue(requiredKey(legacyType));
        }
        if (discount.stripTrailingZeros().scale() > 2) {
            return new ValidationIssue("promotion.validation.discount_scale");
        }
        if (TYPE_DISCOUNT.equals(legacyType)) {
            if (discount.compareTo(BigDecimal.ONE) >= 0) {
                return new ValidationIssue("promotion.validation.discount_rate_range");
            }
        } else if (TYPE_REDUCTION.equals(legacyType)) {
            BigDecimal safeThreshold = threshold == null ? BigDecimal.ZERO : threshold;
            if (safeThreshold.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(safeThreshold) > 0) {
                return new ValidationIssue("promotion.validation.reduction_limit");
            }
        }
        return null;
    }

    /** 门槛金额最多 2 位小数；返回 {@code null} 表示通过。 */
    public static ValidationIssue validateThreshold(BigDecimal threshold) {
        if (threshold == null) {
            return new ValidationIssue("promotion.validation.threshold");
        }
        if (threshold.compareTo(BigDecimal.ZERO) < 0) {
            return new ValidationIssue("promotion.validation.threshold_negative");
        }
        if (threshold.stripTrailingZeros().scale() > 2) {
            return new ValidationIssue("promotion.validation.discount_scale");
        }
        return null;
    }

    /** 最大使用次数：null 表示不限；传入值必须大于 0。 */
    public static ValidationIssue validateMaxUsage(Integer maxUsage) {
        if (maxUsage != null && maxUsage <= 0) {
            return new ValidationIssue("promotion.validation.max_usage_positive");
        }
        return null;
    }

    /** 对外统一用 -1 表示不限次数（兼容历史数据里的 SQL NULL / 0）。 */
    public static int normalizeMaxUsage(Integer maxUsage) {
        return maxUsage == null || maxUsage <= 0 ? -1 : maxUsage;
    }

    private static String requiredKey(String legacyType) {
        if (TYPE_DISCOUNT.equals(legacyType)) {
            return "promotion.validation.discount_rate_required";
        }
        if (TYPE_COUPON.equals(legacyType)) {
            return "promotion.validation.coupon_value_required";
        }
        return "promotion.validation.reduction_required";
    }

    /** 校验失败信息：key + MessageFormat 参数，由调用方按自己的语言解析。 */
    public static final class ValidationIssue {
        private final String key;
        private final Object[] params;

        public ValidationIssue(String key, Object... params) {
            this.key = key;
            this.params = params;
        }

        public String getKey() {
            return key;
        }

        public Object[] getParams() {
            return params;
        }
    }
}
