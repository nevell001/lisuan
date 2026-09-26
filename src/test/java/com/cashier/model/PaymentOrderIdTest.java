package com.cashier.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付单号生成测试。
 *
 * <p>回归：{@code payment_orders.payment_id} 是主键，此前用的是裸毫秒时间戳
 * （{@code "PAY" + System.currentTimeMillis()}），同毫秒创建两笔支付单就会主键冲突、
 * 支付单直接丢失。随机段此前只加在了 {@code merchant_order_no} 上。</p>
 */
@DisplayName("支付单号生成测试")
class PaymentOrderIdTest {

    private static final Pattern PAYMENT_ID_PATTERN = Pattern.compile("^PAY\\d{13}\\d{4}[0-9a-f]{6}$");

    @Test
    @DisplayName("支付单号含时间戳/序号/随机段，且不超过主键列长度")
    void paymentIdIsHardenedAndBounded() {
        String paymentId = PaymentOrder.generatePaymentId();

        assertTrue(PAYMENT_ID_PATTERN.matcher(paymentId).matches(),
            "支付单号应为 PAY+13位毫秒+4位序号+6位随机段: " + paymentId);
        assertTrue(paymentId.length() <= 50, "支付单号应在 payment_orders.payment_id VARCHAR(50) 范围内");
    }

    @Test
    @DisplayName("同毫秒连续生成大量支付单号不重复（原实现在此必撞主键）")
    void paymentIdsDoNotCollideWithinSameMillisecond() {
        int count = 5000;
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < count; i++) {
            ids.add(PaymentOrder.generatePaymentId());
        }
        assertEquals(count, ids.size(), "同一毫秒内生成的支付单号不应重复");
    }

    @Test
    @DisplayName("创建扫码支付订单时即带支付单号，不再依赖 DAO 兜底")
    void scanPayOrderCarriesPaymentId() {
        PaymentOrder order = PaymentOrder.createScanPayOrder(
            "T20260101000000001", new BigDecimal("9.90"),
            PaymentOrder.PaymentChannel.WECHAT, "T1");

        assertNotNull(order.paymentId, "创建时就应有支付单号");
        assertTrue(PAYMENT_ID_PATTERN.matcher(order.paymentId).matches(), order.paymentId);
        assertNotNull(order.merchantOrderNo);
        assertTrue(!order.paymentId.equals(order.merchantOrderNo), "支付单号与商户单号应是两个独立标识");
    }
}
