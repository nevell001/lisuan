package com.cashier.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 会员等级与折扣映射边界测试（纯逻辑，无需数据库）。
 */
@DisplayName("会员等级边界测试")
class MemberLevelBoundaryTest {

    @Test
    @DisplayName("金卡门槛边界：5000 升级，4999 保持银卡")
    void goldBoundary() {
        assertEquals("金卡", MemberService.calculateLevel(BigDecimal.valueOf(5000)));
        assertEquals("银卡", MemberService.calculateLevel(BigDecimal.valueOf(4999)));
    }

    @Test
    @DisplayName("银卡门槛边界：999 普通，1000 升级银卡（业务确认门槛=1000）")
    void silverBoundary() {
        assertEquals("普通", MemberService.calculateLevel(BigDecimal.valueOf(999)));
        assertEquals("银卡", MemberService.calculateLevel(BigDecimal.valueOf(1000)));
        assertEquals("银卡", MemberService.calculateLevel(BigDecimal.valueOf(4999)));
    }

    @Test
    @DisplayName("钻石门槛边界：10000 升级，9999 保持金卡")
    void diamondBoundary() {
        assertEquals("钻石", MemberService.calculateLevel(BigDecimal.valueOf(10000)));
        assertEquals("金卡", MemberService.calculateLevel(BigDecimal.valueOf(9999)));
    }

    @Test
    @DisplayName("null 与负数积分按普通会员处理")
    void nullAndNegativePoints() {
        assertEquals("普通", MemberService.calculateLevel((BigDecimal) null));
        assertEquals("普通", MemberService.calculateLevel(BigDecimal.valueOf(-100)));
    }

    @Test
    @DisplayName("折扣映射：普通 10 / 银卡 9.5 / 金卡 9 / 钻石 8.5")
    void discountMapping() {
        assertEquals(0, BigDecimal.TEN.compareTo(MemberService.getDiscountByLevelDecimal("普通")));
        assertEquals(0, BigDecimal.valueOf(9.5).compareTo(MemberService.getDiscountByLevelDecimal("银卡")));
        assertEquals(0, BigDecimal.valueOf(9.0).compareTo(MemberService.getDiscountByLevelDecimal("金卡")));
        assertEquals(0, BigDecimal.valueOf(8.5).compareTo(MemberService.getDiscountByLevelDecimal("钻石")));
        // 未知等级兜底为不打折
        assertEquals(0, BigDecimal.TEN.compareTo(MemberService.getDiscountByLevelDecimal("不存在等级")));
        // 历史脏数据（带"会员"后缀）也不得享受折扣，须等数据清洗/重算
        assertEquals(0, BigDecimal.TEN.compareTo(MemberService.getDiscountByLevelDecimal("银卡会员")));
        assertEquals(0, BigDecimal.TEN.compareTo(MemberService.getDiscountByLevelDecimal("金卡会员")));
    }
}
