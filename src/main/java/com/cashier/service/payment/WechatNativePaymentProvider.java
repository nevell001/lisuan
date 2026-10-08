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
            String eventType = root.path("event_type").asText();
            boolean paymentEvent = "TRANSACTION.SUCCESS".equals(eventType);
            // F9-c：退款回调（REFUND.SUCCESS/REFUND.ABNORMAL/REFUND.CLOSED）走同一条验签解密路径，
            // 只是解出来的字段不同（有 out_refund_no 而没有 trade_state）
            boolean refundEvent = eventType.startsWith("REFUND.");
            if (!paymentEvent && !refundEvent) {
                return false;
            }
            JsonNode resource = root.path("resource");
            String plainText = PaymentCryptoUtil.decryptAes256Gcm(
                config.wechatApiKey,
                resource.path("nonce").asText(),
                resource.path("associated_data").asText(),
                resource.path("ciphertext").asText()
            );
            JsonNode payload = MAPPER.readTree(plainText);
            notification.put("event_type", eventType);
            notification.put("wechat_plain_body", plainText);

            if (refundEvent) {
                String refundStatus = payload.path("refund_status").asText("");
                notification.put("out_refund_no", payload.path("out_refund_no").asText(""));
                notification.put("out_trade_no", payload.path("out_trade_no").asText(""));
                notification.put("refund_id", payload.path("refund_id").asText(""));
                notification.put("refund_status", refundStatus);
                notification.put("refund_amount", fromCents(payload.path("amount").path("refund").asInt())
                    .toPlainString());
                return !notification.get("out_refund_no").isBlank();
            }

            if (!"SUCCESS".equals(payload.path("trade_state").asText())) {
                return false;
            }
            notification.put("out_trade_no", payload.path("out_trade_no").asText(""));
            notification.put("trade_status", "SUCCESS");
            notification.put("transaction_id", payload.path("transaction_id").asText(""));
            notification.put("buyer_id", payload.path("payer").path("openid").asText(""));
            notification.put("total_amount", fromCents(payload.path("amount").path("payer_total")
                .asInt(payload.path("amount").path("total").asInt())).toPlainString());
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
            String body = MAPPER.writeValueAsString(new java.util.LinkedHashMap<>(Map.of(
                "out_trade_no", order.merchantOrderNo,
                "out_refund_no", refund.merchantRefundNo,
                "reason", refund.reason == null ? "POS refund" : refund.reason,
                "amount", Map.of(
                    "refund", toCents(refund.refundAmount),
                    "total", toCents(order.amount),
                    "currency", "CNY"
                )
            )));
            body = withRefundNotifyUrl(body);
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

    /**
     * 回查退款：{@code GET /v3/refund/domestic/refunds/{out_refund_no}}。
     *
     * <p>退款请求没有带 {@code notify_url}（微信不会推送退款结果），所以这是把 PROCESSING 收敛到
     * 终态的唯一途径，由 {@code PaymentRefundReconcileService} 定期调用（F9）。</p>
     */
    @Override
    public RefundRecord.RefundStatus queryRefund(PaymentOrder order, RefundRecord refund) {
        ensureAvailable();
        try {
            String path = "/v3/refund/domestic/refunds/" + PaymentCryptoUtil.urlEncode(refund.merchantRefundNo);
            HttpResponse<String> response = send("GET", path, "", "");
            JsonNode root = parseSuccess(response);
            if (root.hasNonNull("refund_id")) {
                refund.channelRefundNo = root.path("refund_id").asText();
            }
            if (root.hasNonNull("success_time")) {
                refund.refundTime = Date.from(java.time.OffsetDateTime
                    .parse(root.path("success_time").asText()).toInstant());
            }
            return mapRefundStatus(root.path("status").asText());
        } catch (Exception e) {
            throw new IllegalStateException("查询微信退款状态失败", e);
        }
    }

    /**
     * 退款请求里带上 {@code notify_url}（F9-c）。
     *
     * <p>微信退款通知既可以通过申请退款时的 {@code notify_url} 推送，也可以不带（那就只能靠
     * {@code PaymentRefundReconcileService} 轮询）。回调把收敛时间从"分钟级"降到"秒级"，
     * 两条路都保留：回调丢了/ABNORMAL 时对账仍然兜得住。</p>
     */
    private String withRefundNotifyUrl(String body) {
        if (config.notifyUrl == null || config.notifyUrl.isBlank()) {
            logger.debug("未配置 notify.url，退款结果只能靠对账任务轮询收敛");
            return body;
        }
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>(
                MAPPER.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { }));
            payload.put("notify_url", config.notifyUrl);
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            logger.warn("写入退款 notify_url 失败，退回无回调模式（仍有对账兜底）: {}", e.getMessage());
            return body;
        }
    }

    /**
     * 退款回调里的 {@code refund_status} → 本地状态（与查询接口同一套映射）。
     */
    @Override
    public RefundRecord.RefundStatus refundStatusFromNotification(Map<String, String> notification) {
        return mapRefundStatus(notification == null ? null : notification.get("refund_status"));
    }

    /**
     * 微信退款状态 → 本地状态。
     *
     * <p>{@code ABNORMAL}（退款异常，资金去向需人工确认）**刻意保持 PROCESSING**：标 FAILED 会立刻
     * 释放预占额度、允许对同一笔支付再退，而实际上这笔钱可能已经出去了。留在处理中即进入
     * "需人工核对"。</p>
     */
    static RefundRecord.RefundStatus mapRefundStatus(String wechatStatus) {
        return switch (wechatStatus == null ? "" : wechatStatus) {
            case "SUCCESS" -> RefundRecord.RefundStatus.SUCCESS;
            case "CLOSED" -> RefundRecord.RefundStatus.CLOSED;
            default -> RefundRecord.RefundStatus.PROCESSING;
        };
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
