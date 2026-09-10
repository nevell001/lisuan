package com.cashier.service.payment;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;

import java.util.Date;
import java.util.Map;

/** 仅供显式开发测试使用，生产配置不得启用。 */
public final class MockPaymentChannelProvider implements PaymentChannelProvider {
    private final PaymentOrder.PaymentChannel channel;
    private final String callbackSecret;

    public MockPaymentChannelProvider(PaymentOrder.PaymentChannel channel, String callbackSecret) {
        this.channel = channel;
        this.callbackSecret = callbackSecret;
    }

    @Override public PaymentOrder.PaymentChannel channel() { return channel; }
    @Override public boolean isAvailable() { return true; }
    @Override public String unavailableReason() { return ""; }

    @Override
    public void createOrder(PaymentOrder order) {
        String prefix = channel == PaymentOrder.PaymentChannel.WECHAT ? "weixin://mockpay/" : "https://mock.alipay/";
        order.qrCodeContent = prefix + order.merchantOrderNo;
        order.qrCodeUrl = order.qrCodeContent;
        order.status = PaymentOrder.PaymentStatus.WAITING;
    }

    @Override
    public PaymentOrder.PaymentStatus queryStatus(PaymentOrder order) {
        if (order.expireTime != null && new Date().after(order.expireTime)) {
            return PaymentOrder.PaymentStatus.CLOSED;
        }
        return order.status;
    }

    @Override
    public boolean verifyNotification(Map<String, String> notification) {
        if (callbackSecret == null || callbackSecret.isBlank()) {
            return false;
        }
        String signature = notification.get("mock_signature");
        if (signature == null) {
            return false;
        }
        // 定长比较，避免用 String.equals 逐字符短路泄漏密钥前缀
        return java.security.MessageDigest.isEqual(
            callbackSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            signature.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public void refund(PaymentOrder order, RefundRecord refund) {
        refund.status = RefundRecord.RefundStatus.SUCCESS;
        refund.channelRefundNo = "MOCK_RFD_" + System.currentTimeMillis();
        refund.refundTime = new Date();
    }
}
