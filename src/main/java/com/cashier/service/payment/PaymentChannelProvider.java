package com.cashier.service.payment;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;

import java.util.Map;

/** 支付渠道适配器。真实微信/支付宝 SDK 实现只需接入此接口。 */
public interface PaymentChannelProvider {
    PaymentOrder.PaymentChannel channel();

    boolean isAvailable();

    String unavailableReason();

    void createOrder(PaymentOrder order);

    PaymentOrder.PaymentStatus queryStatus(PaymentOrder order);

    boolean verifyNotification(Map<String, String> notification);

    void refund(PaymentOrder order, RefundRecord refund);

    /**
     * 查询退款终态（F9 对账用）。
     *
     * <p>渠道退款可能是**异步**的：微信 {@code /v3/refund/domestic/refunds} 返回非 SUCCESS 时，
     * 本地只能先记 {@code PROCESSING}（预占额度、不允许再退）。没有回查能力时这条记录永远停在
     * 处理中——额度永久占用、订单状态还会被错标为"部分退款"。对账任务靠本方法把状态收敛到终态。</p>
     *
     * @return 渠道当前状态；无法判定（仍在处理）时返回 {@link RefundRecord.RefundStatus#PROCESSING}
     */
    RefundRecord.RefundStatus queryRefund(PaymentOrder order, RefundRecord refund);
}
