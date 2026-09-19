package com.cashier.service.payment;

import com.cashier.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付宝回调验签测试：合法签名接受、篡改/缺失签名拒绝。
 */
@DisplayName("支付宝回调验签测试")
class AlipayPrecreatePaymentProviderTest {

    private static PrivateKey privateKey;
    private static AlipayPrecreatePaymentProvider provider;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getEncoder().encodeToString(publicKey.getEncoded())
            + "\n-----END PUBLIC KEY-----";
        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getEncoder().encodeToString(privateKey.getEncoded())
            + "\n-----END PRIVATE KEY-----";

        PaymentService.PaymentConfig config = new PaymentService.PaymentConfig();
        config.mode = "production";
        config.alipayAppId = "test-app-id";
        config.alipayPrivateKey = privatePem;
        config.alipayEnabled = true;
        config.alipayPublicKey = publicPem;
        config.notifyUrl = "https://pos.example.com/api/payment/notify";
        provider = new AlipayPrecreatePaymentProvider(config);
    }

    private Map<String, String> alipayNotify(Map<String, String> params) {
        Map<String, String> notify = new HashMap<>(params);
        String content = PaymentCryptoUtil.buildAlipaySignContent(notify);
        notify.put("sign", PaymentCryptoUtil.signSha256WithRsa(content, privateKey));
        return notify;
    }

    @Test
    @DisplayName("合法签名的支付宝回调被接受")
    void validSignAccepted() {
        Map<String, String> notify = new HashMap<>();
        notify.put("out_trade_no", "A202608230001");
        notify.put("trade_status", "TRADE_SUCCESS");
        notify.put("total_amount", "88.50");
        notify.put("trade_no", "2026082322000000000000000000");
        notify.put("buyer_id", "2088000000000000");

        assertTrue(provider.verifyNotification(alipayNotify(notify)));
    }

    @Test
    @DisplayName("篡改回调参数后验签失败")
    void tamperedParamsRejected() {
        Map<String, String> notify = new HashMap<>();
        notify.put("out_trade_no", "A202608230002");
        notify.put("trade_status", "TRADE_SUCCESS");
        notify.put("total_amount", "88.50");
        notify.put("trade_no", "2026082322000000000000000001");

        Map<String, String> signed = alipayNotify(notify);
        signed.put("total_amount", "99.00"); // 篡改金额

        assertFalse(provider.verifyNotification(signed));
    }

    @Test
    @DisplayName("验签后追加未签名参数会失败（控制器不得注入合成字段）")
    void extraUnsignedParamBreaksVerification() {
        Map<String, String> notify = new HashMap<>();
        notify.put("out_trade_no", "A202608230004");
        notify.put("trade_status", "TRADE_SUCCESS");
        notify.put("total_amount", "88.50");
        notify.put("trade_no", "2026082322000000000000000002");

        Map<String, String> signed = alipayNotify(notify);
        // 控制器曾经在验签前把 trade_no 复制成 transaction_id，等价于在签名后多塞一个参数：
        // 支付宝的待验签内容由全部非空参数拼成，多出来的键会让真实回调永远验签失败。
        signed.put("transaction_id", signed.get("trade_no"));

        assertFalse(provider.verifyNotification(signed),
            "待验签参数必须与支付宝签名时的参数完全一致");
    }

    @Test
    @DisplayName("缺少签名或签名为空被拒绝")
    void missingOrBlankSignRejected() {
        Map<String, String> notify = new HashMap<>();
        notify.put("out_trade_no", "A202608230003");
        notify.put("trade_status", "TRADE_SUCCESS");

        assertFalse(provider.verifyNotification(notify));
        assertFalse(provider.verifyNotification(null));
    }

    @Test
    @DisplayName("出站响应缺少签名时验签失败（不得放行）")
    void outboundResponseWithoutSignRejected() {
        ObjectNode root = MAPPER.createObjectNode();
        root.putObject("alipay_trade_query_response").put("code", "10000").put("trade_status", "TRADE_SUCCESS");

        // 回归：此前缺 sign 直接 return true，伪造/中间人返回的网关响应会被采信
        assertFalse(provider.verifyAlipayResponse(root, "alipay_trade_query_response"),
            "支付宝响应没有签名时无法证明来源，必须拒绝");
    }

    @Test
    @DisplayName("出站响应签名合法被接受，响应体被篡改则失败")
    void outboundResponseSignatureVerified() {
        ObjectNode root = MAPPER.createObjectNode();
        root.putObject("alipay_trade_query_response").put("code", "10000").put("trade_status", "TRADE_SUCCESS");
        root.put("sign", PaymentCryptoUtil.signSha256WithRsa(
            root.path("alipay_trade_query_response").toString(), privateKey));

        assertTrue(provider.verifyAlipayResponse(root, "alipay_trade_query_response"));

        // 篡改响应体后原签名失效
        root.putObject("alipay_trade_query_response").put("code", "10000").put("trade_status", "TRADE_CLOSED");
        assertFalse(provider.verifyAlipayResponse(root, "alipay_trade_query_response"));
    }
}
