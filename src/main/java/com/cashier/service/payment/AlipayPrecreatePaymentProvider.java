package com.cashier.service.payment;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class AlipayPrecreatePaymentProvider implements PaymentChannelProvider {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_GATEWAY = "https://openapi.alipay.com/gateway.do";
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PaymentService.PaymentConfig config;
    private final HttpClient client;
    private final String unavailableReason;

    public AlipayPrecreatePaymentProvider(PaymentService.PaymentConfig config) {
        this(config, HttpClient.newHttpClient());
    }

    AlipayPrecreatePaymentProvider(PaymentService.PaymentConfig config, HttpClient client) {
        this.config = config;
        this.client = client;
        this.unavailableReason = validate(config);
    }

    @Override public PaymentOrder.PaymentChannel channel() { return PaymentOrder.PaymentChannel.ALIPAY; }
    @Override public boolean isAvailable() { return unavailableReason.isBlank(); }
    @Override public String unavailableReason() { return unavailableReason; }

    @Override
    public void createOrder(PaymentOrder order) {
        ensureAvailable();
        try {
            Map<String, Object> biz = new HashMap<>();
            biz.put("out_trade_no", order.merchantOrderNo);
            biz.put("total_amount", order.amount.toPlainString());
            biz.put("subject", "LiSuan POS " + order.transactionId);
            biz.put("timeout_express", config.orderExpireMinutes + "m");
            Map<String, String> params = baseParams("alipay.trade.precreate");
            params.put("notify_url", config.notifyUrl);
            params.put("biz_content", MAPPER.writeValueAsString(biz));
            JsonNode response = call(params, "alipay_trade_precreate_response");
            order.qrCodeContent = response.path("qr_code").asText();
            order.qrCodeUrl = order.qrCodeContent;
            order.status = PaymentOrder.PaymentStatus.WAITING;
        } catch (Exception e) {
            throw new IllegalStateException("创建支付宝支付订单失败", e);
        }
    }

    @Override
    public PaymentOrder.PaymentStatus queryStatus(PaymentOrder order) {
        ensureAvailable();
        try {
            Map<String, String> params = baseParams("alipay.trade.query");
            params.put("biz_content", MAPPER.writeValueAsString(Map.of("out_trade_no", order.merchantOrderNo)));
            JsonNode response = call(params, "alipay_trade_query_response");
            String status = response.path("trade_status").asText();
            if ("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status)) {
                order.channelTransactionId = response.path("trade_no").asText(null);
                order.channelUserId = response.path("buyer_user_id").asText(null);
                if (response.has("buyer_logon_id") && (order.channelUserId == null || order.channelUserId.isBlank())) {
                    order.channelUserId = response.path("buyer_logon_id").asText(null);
                }
                if (response.has("receipt_amount")) {
                    order.paidAmount = new BigDecimal(response.path("receipt_amount").asText());
                } else if (response.has("total_amount")) {
                    order.paidAmount = new BigDecimal(response.path("total_amount").asText());
                }
                return PaymentOrder.PaymentStatus.SUCCESS;
            }
            if ("TRADE_CLOSED".equals(status)) return PaymentOrder.PaymentStatus.CLOSED;
            if (order.expireTime != null && new Date().after(order.expireTime)) return PaymentOrder.PaymentStatus.CLOSED;
            return PaymentOrder.PaymentStatus.WAITING;
        } catch (Exception e) {
            throw new IllegalStateException("查询支付宝支付状态失败", e);
        }
    }

    @Override
    public boolean verifyNotification(Map<String, String> notification) {
        if (!isAvailable() || notification == null || PaymentCryptoUtil.isBlank(notification.get("sign"))) {
            return false;
        }
        PublicKey publicKey = PaymentCryptoUtil.loadPublicKeyFromPem(config.alipayPublicKey);
        return PaymentCryptoUtil.verifySha256WithRsa(
            PaymentCryptoUtil.buildAlipaySignContent(notification),
            notification.get("sign"),
            publicKey
        );
    }

    @Override
    public void refund(PaymentOrder order, RefundRecord refund) {
        ensureAvailable();
        try {
            Map<String, String> params = baseParams("alipay.trade.refund");
            params.put("biz_content", MAPPER.writeValueAsString(Map.of(
                "out_trade_no", order.merchantOrderNo,
                "out_request_no", refund.merchantRefundNo,
                "refund_amount", refund.refundAmount.toPlainString(),
                "refund_reason", refund.reason == null ? "POS refund" : refund.reason
            )));
            JsonNode response = call(params, "alipay_trade_refund_response");
            refund.channelRefundNo = response.path("trade_no").asText();
            refund.status = RefundRecord.RefundStatus.SUCCESS;
            refund.refundTime = new Date();
        } catch (Exception e) {
            throw new IllegalStateException("申请支付宝退款失败", e);
        }
    }

    private JsonNode call(Map<String, String> params, String responseNode) throws Exception {
        sign(params);
        HttpRequest request = HttpRequest.newBuilder(URI.create(gateway()))
            .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(formBody(params)))
            .build();
        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
            throw new IllegalStateException("支付宝接口返回 " + httpResponse.statusCode() + ": " + httpResponse.body());
        }
        JsonNode root = MAPPER.readTree(httpResponse.body());
        JsonNode response = root.path(responseNode);
        if (!"10000".equals(response.path("code").asText())) {
            throw new IllegalStateException("支付宝接口调用失败: " + response);
        }
        if (!verifyAlipayResponse(root, responseNode)) {
            throw new IllegalStateException("支付宝响应验签失败");
        }
        return response;
    }

    private Map<String, String> baseParams(String method) {
        Map<String, String> params = new HashMap<>();
        params.put("app_id", config.alipayAppId);
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", "UTF-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(ALIPAY_TIME));
        params.put("version", "1.0");
        return params;
    }

    private void sign(Map<String, String> params) {
        PrivateKey privateKey = PaymentCryptoUtil.loadPrivateKeyFromPem(config.alipayPrivateKey);
        params.put("sign", PaymentCryptoUtil.signSha256WithRsa(
            PaymentCryptoUtil.buildAlipaySignContent(params),
            privateKey
        ));
    }

    /**
     * 校验支付宝出站响应签名。
     *
     * <p>支付宝对每个响应都会签名；缺少 sign 时无法证明响应来自支付宝，
     * 必须拒绝，否则伪造/中间人返回的网关响应会被直接采信。</p>
     */
    boolean verifyAlipayResponse(JsonNode root, String responseNode) {
        String sign = root.path("sign").asText();
        if (PaymentCryptoUtil.isBlank(sign)) {
            return false;
        }
        PublicKey publicKey = PaymentCryptoUtil.loadPublicKeyFromPem(config.alipayPublicKey);
        return PaymentCryptoUtil.verifySha256WithRsa(root.path(responseNode).toString(), sign, publicKey);
    }

    private String formBody(Map<String, String> params) {
        StringBuilder body = new StringBuilder();
        params.forEach((key, value) -> {
            if (body.length() > 0) body.append('&');
            body.append(PaymentCryptoUtil.urlEncode(key)).append('=').append(PaymentCryptoUtil.urlEncode(value));
        });
        return body.toString();
    }

    private String gateway() {
        return PaymentCryptoUtil.isBlank(config.alipayGateway) ? DEFAULT_GATEWAY : config.alipayGateway;
    }

    private void ensureAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException(unavailableReason);
        }
    }

    private static String validate(PaymentService.PaymentConfig config) {
        if (config == null) return "支付宝配置为空";
        if (PaymentCryptoUtil.isBlank(config.alipayAppId)) return "支付宝 App ID 未配置";
        if (PaymentCryptoUtil.isBlank(config.alipayPrivateKey)) return "支付宝应用私钥未配置";
        if (PaymentCryptoUtil.isBlank(config.alipayPublicKey)) return "支付宝公钥未配置";
        if (PaymentCryptoUtil.isBlank(config.notifyUrl)) return "支付回调地址未配置";
        return "";
    }
}
