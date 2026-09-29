package com.cashier.service.payment;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.service.PaymentService;
import com.cashier.util.LoggerFactoryUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

public final class WechatNativePaymentProvider implements PaymentChannelProvider {
    private static final Logger logger = LoggerFactoryUtil.getLogger(WechatNativePaymentProvider.class);
    private static final String API_BASE = "https://api.mch.weixin.qq.com";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter WECHAT_TIME =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.ofHours(8));
    /**
     * 网关超时必须显式设置：java.net.http 的默认是**无限等待**，网关不可达时会把调用线程永久挂住
     * （收银台在 FX 线程调用下单，曾表现为界面彻底卡死，TD-031）。
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final PaymentService.PaymentConfig config;
    private final HttpClient client;
    private final String unavailableReason;

    public WechatNativePaymentProvider(PaymentService.PaymentConfig config) {
        this(config, newHttpClient());
    }

    private static HttpClient newHttpClient() {
        return HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    WechatNativePaymentProvider(PaymentService.PaymentConfig config, HttpClient client) {
        this.config = config;
        this.client = client;
        this.unavailableReason = validate(config);
    }

    @Override public PaymentOrder.PaymentChannel channel() { return PaymentOrder.PaymentChannel.WECHAT; }
    @Override public boolean isAvailable() { return unavailableReason.isBlank(); }
    @Override public String unavailableReason() { return unavailableReason; }

    @Override
    public void createOrder(PaymentOrder order) {
        ensureAvailable();
        try {
            String body = MAPPER.writeValueAsString(Map.of(
                "appid", config.wechatAppId,
                "mchid", config.wechatMchId,
                "description", "LiSuan POS " + order.transactionId,
                "out_trade_no", order.merchantOrderNo,
                "time_expire", WECHAT_TIME.format(order.expireTime.toInstant()),
                "notify_url", config.notifyUrl,
                "amount", Map.of("total", toCents(order.amount), "currency", "CNY")
            ));
            HttpResponse<String> response = send("POST", "/v3/pay/transactions/native", "", body);
            JsonNode root = parseSuccess(response);
            order.qrCodeContent = root.path("code_url").asText();
            order.qrCodeUrl = order.qrCodeContent;
            order.status = PaymentOrder.PaymentStatus.WAITING;
        } catch (Exception e) {
            throw new IllegalStateException("创建微信支付订单失败", e);
        }
    }

    @Override
    public PaymentOrder.PaymentStatus queryStatus(PaymentOrder order) {
        ensureAvailable();
        try {
            String path = "/v3/pay/transactions/out-trade-no/" + order.merchantOrderNo;
            String query = "mchid=" + PaymentCryptoUtil.urlEncode(config.wechatMchId);
            HttpResponse<String> response = send("GET", path, query, "");
            JsonNode root = parseSuccess(response);
            String state = root.path("trade_state").asText();
            if ("SUCCESS".equals(state)) {
                order.channelTransactionId = root.path("transaction_id").asText(null);
                order.channelUserId = root.path("payer").path("openid").asText(null);
                JsonNode amount = root.path("amount");
                if (amount.has("payer_total")) {
                    order.paidAmount = fromCents(amount.path("payer_total").asInt());
                } else if (amount.has("total")) {
                    order.paidAmount = fromCents(amount.path("total").asInt());
                }
                return PaymentOrder.PaymentStatus.SUCCESS;
            }
            if ("CLOSED".equals(state) || "REVOKED".equals(state)) return PaymentOrder.PaymentStatus.CLOSED;
            if ("PAYERROR".equals(state)) return PaymentOrder.PaymentStatus.FAILED;
            if (order.expireTime != null && new Date().after(order.expireTime)) return PaymentOrder.PaymentStatus.CLOSED;
            return PaymentOrder.PaymentStatus.WAITING;
        } catch (Exception e) {
            throw new IllegalStateException("查询微信支付状态失败", e);
        }
    }

    @Override
    public boolean verifyNotification(Map<String, String> notification) {
        if (!isAvailable() || notification == null || PaymentCryptoUtil.isBlank(config.wechatCertPath)) {
            return false;
        }
        try {
            String rawBody = notification.get("raw_body");
            String timestamp = notification.get("Wechatpay-Timestamp");
            String nonce = notification.get("Wechatpay-Nonce");
            String signature = notification.get("Wechatpay-Signature");
            if (PaymentCryptoUtil.isBlank(rawBody) || PaymentCryptoUtil.isBlank(timestamp)
                    || PaymentCryptoUtil.isBlank(nonce) || PaymentCryptoUtil.isBlank(signature)) {
                return false;
            }
            String message = timestamp + "\n" + nonce + "\n" + rawBody + "\n";
            PublicKey publicKey = PaymentCryptoUtil.loadPublicKeyFromCertificateOrPem(config.wechatCertPath);
            if (!PaymentCryptoUtil.verifySha256WithRsa(message, signature, publicKey)) {
                logger.warn("微信支付回调签名验证失败，可能存在伪造请求");
                return false;
            }

            JsonNode root = MAPPER.readTree(rawBody);
            if (!"TRANSACTION.SUCCESS".equals(root.path("event_type").asText())) {
                return false;
            }
            JsonNode resource = root.path("resource");
            String plainText = PaymentCryptoUtil.decryptAes256Gcm(
                config.wechatApiKey,
                resource.path("nonce").asText(),
                resource.path("associated_data").asText(),
                resource.path("ciphertext").asText()
            );
            JsonNode transaction = MAPPER.readTree(plainText);
            if (!"SUCCESS".equals(transaction.path("trade_state").asText())) {
                return false;
            }
            notification.put("out_trade_no", transaction.path("out_trade_no").asText(""));
            notification.put("trade_status", "SUCCESS");
            notification.put("transaction_id", transaction.path("transaction_id").asText(""));
            notification.put("buyer_id", transaction.path("payer").path("openid").asText(""));
            notification.put("total_amount", fromCents(transaction.path("amount").path("payer_total")
                .asInt(transaction.path("amount").path("total").asInt())).toPlainString());
            notification.put("wechat_plain_body", plainText);
            return true;
        } catch (Exception e) {
            logger.warn("微信支付回调验签或解密异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void refund(PaymentOrder order, RefundRecord refund) {
        ensureAvailable();
        try {
            String body = MAPPER.writeValueAsString(Map.of(
                "out_trade_no", order.merchantOrderNo,
                "out_refund_no", refund.merchantRefundNo,
                "reason", refund.reason == null ? "POS refund" : refund.reason,
                "amount", Map.of(
                    "refund", toCents(refund.refundAmount),
                    "total", toCents(order.amount),
                    "currency", "CNY"
                )
            ));
            HttpResponse<String> response = send("POST", "/v3/refund/domestic/refunds", "", body);
            JsonNode root = parseSuccess(response);
            refund.channelRefundNo = root.path("refund_id").asText();
            String status = root.path("status").asText();
            refund.status = "SUCCESS".equals(status)
                ? RefundRecord.RefundStatus.SUCCESS
                : RefundRecord.RefundStatus.PROCESSING;
            if (refund.status == RefundRecord.RefundStatus.SUCCESS) {
                refund.refundTime = new Date();
            }
        } catch (Exception e) {
            throw new IllegalStateException("申请微信退款失败", e);
        }
    }

    private HttpResponse<String> send(String method, String path, String query, String body) throws Exception {
        String url = API_BASE + path + (query.isBlank() ? "" : "?" + query);
        String authorization = authorization(method, path, query, body);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/json")
            .header("Authorization", authorization)
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json");
        HttpRequest request = "GET".equals(method)
            ? builder.GET().build()
            : builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String authorization(String method, String path, String query, String body) {
        long timestamp = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String canonicalUrl = path + (query.isBlank() ? "" : "?" + query);
        String message = method + "\n" + canonicalUrl + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        PrivateKey privateKey = PaymentCryptoUtil.loadPrivateKeyFromPem(config.wechatPrivateKeyPath);
        String signature = PaymentCryptoUtil.signSha256WithRsa(message, privateKey);
        return "WECHATPAY2-SHA256-RSA2048 mchid=\"" + config.wechatMchId
            + "\",nonce_str=\"" + nonce
            + "\",signature=\"" + signature
            + "\",timestamp=\"" + timestamp
            + "\",serial_no=\"" + config.wechatMerchantSerialNo + "\"";
    }

    private JsonNode parseSuccess(HttpResponse<String> response) throws Exception {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("微信支付接口返回 " + response.statusCode() + ": " + response.body());
        }
        return MAPPER.readTree(response.body());
    }

    private static int toCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private static BigDecimal fromCents(int cents) {
        return BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.HALF_UP);
    }

    private void ensureAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException(unavailableReason);
        }
    }

    private static String validate(PaymentService.PaymentConfig config) {
        if (config == null) return "微信支付配置为空";
        if (PaymentCryptoUtil.isBlank(config.wechatAppId)) return "微信 App ID 未配置";
        if (PaymentCryptoUtil.isBlank(config.wechatMchId)) return "微信商户号未配置";
        if (PaymentCryptoUtil.isBlank(config.wechatApiKey)) return "微信 API v3 密钥未配置";
        if (PaymentCryptoUtil.isBlank(config.wechatPrivateKeyPath)) return "微信商户私钥路径未配置";
        if (PaymentCryptoUtil.isBlank(config.wechatMerchantSerialNo)) return "微信商户证书序列号未配置";
        if (PaymentCryptoUtil.isBlank(config.notifyUrl)) return "支付回调地址未配置";
        return "";
    }
}
