package com.cashier.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("促销类型与金额规则")
class PromotionRulesTest {

    @Test
    @DisplayName("对外代码与数据库中文类型双向映射，且忽略大小写与空白")
    void typeCodesMapToLegacyTypes() {
        assertEquals(PromotionRules.TYPE_REDUCTION, PromotionRules.toLegacyType(" reduction "));
        assertEquals(PromotionRules.TYPE_DISCOUNT, PromotionRules.toLegacyType("DISCOUNT"));
        assertEquals(PromotionRules.TYPE_COUPON, PromotionRules.toLegacyType("Coupon"));
        assertNull(PromotionRules.toLegacyType("BOGUS"));
        assertNull(PromotionRules.toLegacyType(null));

        assertEquals("REDUCTION", PromotionRules.toCode(PromotionRules.TYPE_REDUCTION));
        assertEquals("DISCOUNT", PromotionRules.toCode(PromotionRules.TYPE_DISCOUNT));
        assertEquals("COUPON", PromotionRules.toCode(PromotionRules.TYPE_COUPON));
        assertNull(PromotionRules.toCode("未知类型"));

        assertEquals("api.promotion.type.reduction",
            PromotionRules.typeNameKey(PromotionRules.TYPE_REDUCTION));
        assertNull(PromotionRules.typeNameKey("未知类型"));
    }

    @Test
    @DisplayName("折扣校验按类型区分必填项与上限")
    void discountValidationFollowsDesktopRules() {
        assertIssue("promotion.validation.reduction_required",
            PromotionRules.validateDiscount(PromotionRules.TYPE_REDUCTION, new BigDecimal("100"), null));
        assertIssue("promotion.validation.coupon_value_required",
            PromotionRules.validateDiscount(PromotionRules.TYPE_COUPON, null, BigDecimal.ZERO));
        assertIssue("promotion.validation.discount_rate_required",
            PromotionRules.validateDiscount(PromotionRules.TYPE_DISCOUNT, null, null));
        assertIssue("promotion.validation.discount_scale",
            PromotionRules.validateDiscount(PromotionRules.TYPE_COUPON, null, new BigDecimal("1.234")));
        assertIssue("promotion.validation.discount_rate_range",
            PromotionRules.validateDiscount(PromotionRules.TYPE_DISCOUNT, null, BigDecimal.ONE));
        assertIssue("promotion.validation.reduction_limit",
            PromotionRules.validateDiscount(PromotionRules.TYPE_REDUCTION, new BigDecimal("50"),
                new BigDecimal("60")));

        assertNull(PromotionRules.validateDiscount(PromotionRules.TYPE_REDUCTION,
            new BigDecimal("100"), new BigDecimal("100")));
        assertNull(PromotionRules.validateDiscount(PromotionRules.TYPE_DISCOUNT,
            new BigDecimal("50"), new BigDecimal("0.85")));
        assertNull(PromotionRules.validateDiscount(PromotionRules.TYPE_COUPON, null, new BigDecimal("20")));
    }

    @Test
    @DisplayName("门槛金额与最大使用次数校验")
    void thresholdAndMaxUsageValidation() {
        assertIssue("promotion.validation.threshold", PromotionRules.validateThreshold(null));
        assertIssue("promotion.validation.threshold_negative",
            PromotionRules.validateThreshold(new BigDecimal("-0.01")));
        assertIssue("promotion.validation.discount_scale",
            PromotionRules.validateThreshold(new BigDecimal("10.001")));
        assertNull(PromotionRules.validateThreshold(BigDecimal.ZERO));
        assertNull(PromotionRules.validateThreshold(new BigDecimal("99.99")));

        assertNull(PromotionRules.validateMaxUsage(null));
        assertIssue("promotion.validation.max_usage_positive", PromotionRules.validateMaxUsage(0));
        assertIssue("promotion.validation.max_usage_positive", PromotionRules.validateMaxUsage(-1));
        assertNull(PromotionRules.validateMaxUsage(1));

        assertEquals(-1, PromotionRules.normalizeMaxUsage(null));
        assertEquals(-1, PromotionRules.normalizeMaxUsage(0));
        assertEquals(5, PromotionRules.normalizeMaxUsage(5));
    }

    private static void assertIssue(String expectedKey, PromotionRules.ValidationIssue issue) {
        assertNotNull(issue, "应返回校验问题");
        assertEquals(expectedKey, issue.getKey());
    }
}
