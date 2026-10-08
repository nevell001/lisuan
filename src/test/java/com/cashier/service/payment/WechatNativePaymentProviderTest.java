package com.cashier.service.payment;

import com.cashier.model.RefundRecord;
import com.cashier.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatNativePaymentProviderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String API_V3_KEY = "12345678901234567890123456789012";

    @Test
    @DisplayName("微信 v3 回调应验签解密并转换为统一支付通知字段")
    void wechatV3NotificationIsVerifiedDecryptedAndMapped() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        WechatNativePaymentProvider provider = new WechatNativePaymentProvider(configWithPublicKey(keyPair));
        String plainBody = MAPPER.writeValueAsString(Map.of(
            "out_trade_no", "LS202607100001",
            "transaction_id", "420000000020260710000001",
            "trade_state", "SUCCESS",
            "payer", Map.of("openid", "openid-001"),
            "amount", Map.of("total", 1288, "payer_total", 1288)
        ));
        String resourceNonce = "resourceNonce";
        String associatedData = "transaction";
        String ciphertext = encryptAesGcm(plainBody, resourceNonce, associatedData);
        String rawBody = MAPPER.writeValueAsString(Map.of(
            "id", "EV-1",
            "event_type", "TRANSACTION.SUCCESS",
            "resource_type", "encrypt-resource",
            "resource", Map.of(
                "algorithm", "AEAD_AES_256_GCM",
                "ciphertext", ciphertext,
                "associated_data", associatedData,
                "nonce", resourceNonce
            )
        ));
        String timestamp = "1783688400";
        String notifyNonce = "notifyNonce";
        String signature = sign(timestamp + "\n" + notifyNonce + "\n" + rawBody + "\n", keyPair);

        Map<String, String> notification = new HashMap<>();
        notification.put("raw_body", rawBody);
        notification.put("Wechatpay-Timestamp", timestamp);
        notification.put("Wechatpay-Nonce", notifyNonce);
        notification.put("Wechatpay-Signature", signature);

        assertTrue(provider.verifyNotification(notification));
        assertEquals("LS202607100001", notification.get("out_trade_no"));
        assertEquals("SUCCESS", notification.get("trade_status"));
        assertEquals("420000000020260710000001", notification.get("transaction_id"));
        assertEquals("openid-001", notification.get("buyer_id"));
        assertEquals("12.88", notification.get("total_amount"));
    }

    @Test
    @DisplayName("微信 v3 回调签名错误时必须拒绝")
    void wechatV3NotificationRejectsInvalidSignature() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        WechatNativePaymentProvider provider = new WechatNativePaymentProvider(configWithPublicKey(keyPair));
        Map<String, String> notification = new HashMap<>();
        notification.put("raw_body", "{}");
        notification.put("Wechatpay-Timestamp", "1783688400");
        notification.put("Wechatpay-Nonce", "notifyNonce");
        notification.put("Wechatpay-Signature", "invalid");

        assertFalse(provider.verifyNotification(notification));
    }

    @Test
    @DisplayName("微信退款回调应验签解密为退款字段；ABNORMAL 不落终态")
    void wechatRefundNotificationIsVerifiedAndMapped() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        WechatNativePaymentProvider provider = new WechatNativePaymentProvider(configWithPublicKey(keyPair));
        String plainBody = MAPPER.writeValueAsString(Map.of(
            "out_trade_no", "LS202607100001",
            "out_refund_no", "RF202607100001",
            "refund_id", "503000000020260710000001",
            "refund_status", "SUCCESS",
            "amount", Map.of("total", 1288, "refund", 1288)
        ));

        Map<String, String> notification = signedNotification(keyPair, "REFUND.SUCCESS", plainBody, "refund");
        assertTrue(provider.verifyNotification(notification), "退款回调必须能通过验签与解密");
        assertEquals("RF202607100001", notification.get("out_refund_no"));
        assertEquals("503000000020260710000001", notification.get("refund_id"));
        assertEquals("12.88", notification.get("refund_amount"));
        assertEquals(RefundRecord.RefundStatus.SUCCESS, provider.refundStatusFromNotification(notification));

        // ABNORMAL（资金去向需人工确认）：不能当失败，否则立刻释放预占额度、允许同一笔支付再退
        String abnormalBody = MAPPER.writeValueAsString(Map.of(
            "out_trade_no", "LS202607100001",
            "out_refund_no", "RF202607100002",
            "refund_id", "503000000020260710000002",
            "refund_status", "ABNORMAL",
            "amount", Map.of("total", 1288, "refund", 1288)
        ));
        Map<String, String> abnormal = signedNotification(keyPair, "REFUND.ABNORMAL", abnormalBody, "refund");
        assertTrue(provider.verifyNotification(abnormal));
        assertEquals(RefundRecord.RefundStatus.PROCESSING, provider.refundStatusFromNotification(abnormal));
    }

    @Test
    @DisplayName("微信非支付非退款事件必须拒绝（不得把未知事件当回调处理）")
    void wechatRejectsUnknownEventTypes() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        WechatNativePaymentProvider provider = new WechatNativePaymentProvider(configWithPublicKey(keyPair));
        Map<String, String> notification = signedNotification(keyPair, "SOMETHING.ELSE", "{}", "refund");
        assertFalse(provider.verifyNotification(notification));
    }

    private static Map<String, String> signedNotification(KeyPair keyPair, String eventType, String plainBody,
                                                          String associatedData) throws Exception {
        String resourceNonce = "resourceNonce";
        String ciphertext = encryptAesGcm(plainBody, resourceNonce, associatedData);
        String rawBody = MAPPER.writeValueAsString(Map.of(
            "id", "EV-" + eventType,
            "event_type", eventType,
            "resource_type", "encrypt-resource",
            "resource", Map.of(
                "algorithm", "AEAD_AES_256_GCM",
                "ciphertext", ciphertext,
                "associated_data", associatedData,
                "nonce", resourceNonce
            )
        ));
        String timestamp = "1783688400";
        String notifyNonce = "notifyNonce";
        String signature = sign(timestamp + "\n" + notifyNonce + "\n" + rawBody + "\n", keyPair);

        Map<String, String> notification = new HashMap<>();
        notification.put("raw_body", rawBody);
        notification.put("Wechatpay-Timestamp", timestamp);
        notification.put("Wechatpay-Nonce", notifyNonce);
        notification.put("Wechatpay-Signature", signature);
        return notification;
    }

    @Test
    @DisplayName("微信退款状态映射：只有 SUCCESS/CLOSED 落终态，ABNORMAL 必须留在处理中")
    void refundStatusMappingKeepsAbnormalInProgress() {
        assertEquals(RefundRecord.RefundStatus.SUCCESS,
            WechatNativePaymentProvider.mapRefundStatus("SUCCESS"));
        assertEquals(RefundRecord.RefundStatus.CLOSED,
            WechatNativePaymentProvider.mapRefundStatus("CLOSED"));
        // ABNORMAL（退款异常，资金去向需人工确认）不能当失败：标 FAILED 会立刻释放预占额度、
        // 允许对同一笔支付再退，而这笔钱可能已经出去了
        assertEquals(RefundRecord.RefundStatus.PROCESSING,
            WechatNativePaymentProvider.mapRefundStatus("ABNORMAL"));
        assertEquals(RefundRecord.RefundStatus.PROCESSING,
            WechatNativePaymentProvider.mapRefundStatus("PROCESSING"));
        assertEquals(RefundRecord.RefundStatus.PROCESSING,
            WechatNativePaymentProvider.mapRefundStatus(null), "缺失状态一律按处理中，不能猜终态");
    }

    private static PaymentService.PaymentConfig configWithPublicKey(KeyPair keyPair) {
        PaymentService.PaymentConfig config = new PaymentService.PaymentConfig();
        config.wechatAppId = "wx-app";
        config.wechatMchId = "mch-id";
        config.wechatApiKey = API_V3_KEY;
        config.wechatPrivateKeyPath = privateKeyPem(keyPair);
        config.wechatMerchantSerialNo = "SERIAL123";
        config.wechatCertPath = publicKeyPem(keyPair);
        config.notifyUrl = "https://pos.example.com/api/payment/notify/wechat";
        return config;
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String sign(String content, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static String encryptAesGcm(String plainText, String nonce, String associatedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
            new SecretKeySpec(API_V3_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
            new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    private static String privateKeyPem(KeyPair keyPair) {
        return pem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
    }

    private static String publicKeyPem(KeyPair keyPair) {
        return pem("PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    private static String pem(String label, byte[] encoded) {
        return "-----BEGIN " + label + "-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(encoded)
            + "\n-----END " + label + "-----";
    }
}
