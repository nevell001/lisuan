package com.cashier.service.payment;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;

import java.util.Map;

public final class UnavailablePaymentChannelProvider implements PaymentChannelProvider {
    private final PaymentOrder.PaymentChannel channel;
    private final String reason;

    public UnavailablePaymentChannelProvider(PaymentOrder.PaymentChannel channel, String reason) {
        this.channel = channel;
        this.reason = reason;
    }

    @Override public PaymentOrder.PaymentChannel channel() { return channel; }
    @Override public boolean isAvailable() { return false; }
    @Override public String unavailableReason() { return reason; }
    @Override public void createOrder(PaymentOrder order) { throw unavailable(); }
    @Override public PaymentOrder.PaymentStatus queryStatus(PaymentOrder order) { throw unavailable(); }
    @Override public boolean verifyNotification(Map<String, String> notification) { return false; }
    @Override public void refund(PaymentOrder order, RefundRecord refund) { throw unavailable(); }
    @Override public RefundRecord.RefundStatus queryRefund(PaymentOrder order, RefundRecord refund) { throw unavailable(); }

    private IllegalStateException unavailable() {
        return new IllegalStateException(reason);
    }
}
