package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 会员等级口径单一来源门禁
 * 回归 P1：等级阈值/文案/折扣只能定义在 MemberService（LEVEL_POINTS/LEVEL_DISCOUNTS），
 * 其他层（尤其 API 退款重算）必须委托 MemberService，禁止再写第二套带“会员”后缀的
 * 等级文案或重复阈值，否则会导致折扣查询失效。
 */
class MemberLevelPolicyTest {

    private static String readMainSource(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/com/cashier", relativePath));
    }

    @Test
    @DisplayName("等级阈值与折扣只在 MemberService 定义一次")
    void levelRulesAreSingleSourced() throws Exception {
        String memberService = readMainSource("service/MemberService.java");

        assertTrue(memberService.contains("LEVEL_DISCOUNTS.put(\"普通\""));
        assertTrue(memberService.contains("LEVEL_POINTS.put(\"银卡\""));
        assertTrue(memberService.contains("LEVEL_POINTS.put(\"金卡\""));
        assertTrue(memberService.contains("LEVEL_POINTS.put(\"钻石\""));
    }

    @Test
    @DisplayName("API 退款重算委托 MemberService，不携带独立等级文案")
    void apiRefundRecalcDelegatesToMemberService() throws Exception {
        String apiTx = readMainSource("api/controller/TransactionApiController.java");

        assertTrue(apiTx.contains("MemberService.calculateLevel("),
            "API 退款重算必须使用 MemberService.calculateLevel");
        assertTrue(apiTx.contains("MemberService.getDiscountByLevelDecimal("),
            "API 退款重算必须同步刷新折扣（MemberService.getDiscountByLevelDecimal）");
        assertFalse(apiTx.contains("LEVEL_POINTS"),
            "API 层不得重复定义等级阈值");
        assertFalse(apiTx.contains("钻石会员") || apiTx.contains("金卡会员")
            || apiTx.contains("银卡会员") || apiTx.contains("普通会员"),
            "API 层不得写入带“会员”后缀的非规范等级文案");
        assertFalse(apiTx.contains("calculateMemberLevel"),
            "API 层私有等级计算方法应已删除");
    }
}
