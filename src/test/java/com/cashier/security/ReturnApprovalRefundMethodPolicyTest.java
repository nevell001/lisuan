package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退货审批的"退款方式"必须真正生效（2026-10 审计 F1）。
 *
 * <p>背景：{@code ReturnService.settleRefund} 是按 {@code return_orders.payment_method} 决定
 * "退现金还是退回会员余额"的，而审批界面上的下拉框此前只被读进一个局部变量（{@code String paymentMethod
 * = refundMethod;}）就丢弃 —— 审批人把微信单改成现金，退款照样退回会员余额，报表也从错误的渠道扣除。</p>
 *
 * <p>行为侧由 {@code ReturnServiceTest}（选择落库 + 决定退款去向 + 非法值被拒）守住；这里钉住
 * **控制器**必须把选择传下去，并且下拉框必须与该单原有的支付方式对齐（不刷新就会出现"打开微信单、
 * 下拉框停在现金"的错位）。控制器依赖 JavaFX 控件，无头环境跑不了，所以用源码门禁。</p>
 */
@DisplayName("退货审批退款方式门禁")
class ReturnApprovalRefundMethodPolicyTest {

    private static final String CONTROLLER =
        "src/main/java/com/cashier/controller/ReturnApprovalController.java";

    @Test
    @DisplayName("审批通过时必须把下拉框的退款方式传给服务，不得丢弃选择")
    void approvePassesChosenRefundMethod() throws Exception {
        String source = Files.readString(Path.of(CONTROLLER));

        int call = source.indexOf("ReturnService.approveReturnOrder(");
        assertTrue(call > 0, "退货审批控制器必须调用 ReturnService.approveReturnOrder");
        int end = source.indexOf(");", call);
        assertTrue(end > call, "找不到 approveReturnOrder 调用的实参列表");
        String arguments = source.substring(call, end);

        assertTrue(arguments.contains("refundMethod"),
            "审批必须把退款方式作为实参传给 approveReturnOrder，否则审批人的选择会被丢弃（F1）");
        assertFalse(source.contains("String paymentMethod = refundMethod;"),
            "把下拉框值赋给一个再也不用的局部变量正是 F1 的形态，不得回归");
    }

    @Test
    @DisplayName("选中退货单时，退款方式下拉框必须与订单原有支付方式对齐")
    void comboFollowsSelectedOrder() throws Exception {
        String source = Files.readString(Path.of(CONTROLLER));

        assertTrue(source.contains("refundMethodComboBox.setValue("),
            "选中退货单后必须刷新退款方式下拉框");
        assertTrue(source.contains("refundMethodComboBox.getItems().contains("),
            "刷新时必须校验订单支付方式在下拉框可选值范围内，避免出现非法选中值");
    }
}
