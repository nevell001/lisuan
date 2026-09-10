package com.cashier.api.controller;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.model.User;
import com.cashier.dao.DAOFactory;
import com.cashier.service.PaymentService;
import com.cashier.util.LoggerFactoryUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 电子支付 REST API 控制器
 * 微信支付、支付宝支付管理
 */
public class PaymentApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PaymentApiController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PAYMENT_ID_FIELD = "paymentId";
    private static final String OUT_TRADE_NO_FIELD = "out_trade_no";
    private static final int DEFAULT_WAITING_PAYMENT_LIMIT = 100;
    private static final int MAX_WAITING_PAYMENT_LIMIT = 500;
    
    /**
     * 创建支付订单
     * POST /api/payment/create
     * Body: { "transactionId": "T123", "amount": 100.00, "channel": "WECHAT", "terminalId": "POS01" }
     */
    public static void createPayment(Context ctx) {
        try {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            if (body == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "请求体不能为空"
                ));
                return;
            }
            
            String transactionId = getString(body, "transactionId", null);
            BigDecimal amount = getBigDecimal(body, "amount");
            String channelStr = getString(body, "channel", "WECHAT");
            String terminalId = getString(body, "terminalId", "default");
            String operator = getString(body, "operator", "system");
            
            if (transactionId == null || amount == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "缺少必要参数: transactionId, amount"
                ));
                return;
            }
            
            PaymentOrder.PaymentChannel channel = PaymentOrder.PaymentChannel.fromString(channelStr);
            
            // 创建支付订单
            PaymentOrder order = PaymentService.createPaymentOrder(transactionId, amount, channel, terminalId);
            order.operator = operator;
            
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    PAYMENT_ID_FIELD, order.paymentId,
                    "merchantOrderNo", order.merchantOrderNo,
                    "amount", order.amount,
                    "channel", order.channel.getDisplayName(),
                    "qrCodeUrl", order.qrCodeUrl,
                    "qrCodeContent", order.qrCodeContent,
                    "expireTime", order.expireTime,
                    "status", order.status.getDisplayName()
                ),
                "message", "支付订单创建成功"
            ));
            
        } catch (Exception e) {
            logger.error("创建支付订单失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "创建失败"
            ));
        }
    }
    
    /**
     * 查询支付状态
     * GET /api/payment/:paymentId/status
     */
    public static void queryStatus(Context ctx) {
        String paymentId = ctx.pathParam(PAYMENT_ID_FIELD);
        
        try {
            PaymentOrder order = PaymentService.queryPaymentStatus(paymentId);
            
            if (order == null) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "error", "支付订单不存在"
                ));
                return;
            }
            
            ctx.json(Map.of(
                "success", true,
                "data", buildPaymentOrderData(order)
            ));
            
        } catch (SQLException e) {
            logger.error("查询支付状态失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "查询失败"
            ));
        }
    }
    
    /**
     * 根据交易ID查询支付记录
     * GET /api/payment/transaction/:transactionId
     */
    public static void getByTransaction(Context ctx) {
        String transactionId = ctx.pathParam("transactionId");
        
        try {
            List<PaymentOrder> orders = DAOFactory.getInstance().getPaymentDAO().findByTransactionId(transactionId);
            
            List<Map<String, Object>> orderList = orders.stream()
                .map(order -> Map.<String, Object>of(
                    PAYMENT_ID_FIELD, order.paymentId,
                    "channel", order.channel.getDisplayName(),
                    "amount", order.amount,
                    "paidAmount", order.paidAmount != null ? order.paidAmount : 0,
                    "status", order.status.getDisplayName(),
                    "createTime", order.createTime != null ? order.createTime.toString() : "",
                    "payTime", order.payTime != null ? order.payTime.toString() : ""
                ))
                .collect(Collectors.toList());
            
            ctx.json(Map.of(
                "success", true,
                "data", orderList,
                "total", orderList.size()
            ));
            
        } catch (SQLException e) {
            logger.error("查询交易支付记录失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "查询失败"
            ));
        }
    }
    
    /**
     * 支付回调通知
     * POST /api/payment/notify/:channel
     */
    public static void handleNotify(Context ctx) {
        String channelStr = ctx.pathParam("channel");
        PaymentOrder.PaymentChannel channel = PaymentOrder.PaymentChannel.fromString(channelStr);
        
        try {
            // 解析回调数据
            Map<String, String> notifyData = new HashMap<>();
            // 先统一解析表单参数（mock 渠道与支付宝回调使用；微信 v3 JSON 回调此处为空，无副作用）
            Map<String, List<String>> formParams = ctx.formParamMap();
            formParams.forEach((key, values) ->
                notifyData.put(key, values == null || values.isEmpty() ? "" : values.get(0)));
            // 注意：不要在这里往 notifyData 里塞支付宝没有参与签名的合成字段（如 transaction_id），
            // 否则 buildAlipaySignContent 会把该字段算进待验签内容，真实回调永远验签失败。
            if (channel == PaymentOrder.PaymentChannel.WECHAT) {
                notifyData.put("raw_body", ctx.body());
                notifyData.put("Wechatpay-Timestamp", ctx.header("Wechatpay-Timestamp"));
                notifyData.put("Wechatpay-Nonce", ctx.header("Wechatpay-Nonce"));
                notifyData.put("Wechatpay-Signature", ctx.header("Wechatpay-Signature"));
                notifyData.put("Wechatpay-Serial", ctx.header("Wechatpay-Serial"));
                extractWechatV3NotifyMetadata(ctx.body(), notifyData);
            }
            
            if (ctx.formParam("mock_signature") != null) {
                notifyData.put("mock_signature", ctx.formParam("mock_signature"));
            }
            logger.info("支付回调接收: channel={}, keys={}, mock_signature={}, out_trade_no={}",
                channelStr, notifyData.keySet(),
                notifyData.get("mock_signature") != null ? "***" : null,
                notifyData.get("out_trade_no"));
            boolean success = PaymentService.handlePaymentNotify(channel, notifyData);
            
            // 返回响应
            if (!success) {
                ctx.status(400);
            }
            if (channel == PaymentOrder.PaymentChannel.WECHAT && success) {
                ctx.json(Map.of("code", "SUCCESS", "message", "成功"));
            } else if (channel == PaymentOrder.PaymentChannel.WECHAT) {
                ctx.json(Map.of("code", "FAIL", "message", "签名或业务校验失败"));
            } else if (success) {
                ctx.result("success");
            } else {
                ctx.result("fail");
            }
            
        } catch (Exception e) {
            logger.error("处理支付回调失败", e);
            if (channel == PaymentOrder.PaymentChannel.WECHAT) {
                ctx.status(500).json(Map.of("code", "FAIL", "message", "处理失败"));
            } else {
                ctx.result("fail");
            }
        }
    }
    
    private static void extractWechatV3NotifyMetadata(String body, Map<String, String> notifyData) {
        try {
            var root = OBJECT_MAPPER.readTree(body);
            notifyData.put("wechat_event_type", root.path("event_type").asText(""));
            notifyData.put("wechat_resource_type", root.path("resource_type").asText(""));
        } catch (Exception e) {
            logger.warn("解析微信支付 v3 回调元数据失败: {}", e.getMessage());
        }
    }
    
    /**
     * 申请退款
     * POST /api/payment/:paymentId/refund
     * Body: { "amount": 50.00, "reason": "商品质量问题" }
     * 操作员取认证用户，不从请求体读取。
     */
    public static void applyRefund(Context ctx) {
        String paymentId = ctx.pathParam(PAYMENT_ID_FIELD);
        
        try {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            if (body == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "请求体不能为空"
                ));
                return;
            }
            
            BigDecimal refundAmount = getBigDecimal(body, "amount");
            String reason = getString(body, "reason", "用户申请退款");
            // 操作员取认证用户，忽略请求体中的自报身份：退款审计与流水归属必须可信
            User operator = ctx.attribute("currentUser");
            String operatorName = operator != null ? operator.name : "system";

            RefundRecord refund = PaymentService.applyRefund(paymentId, refundAmount, reason, operatorName);
            
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    "refundId", refund.refundId,
                    "merchantRefundNo", refund.merchantRefundNo,
                    "refundAmount", refund.refundAmount,
                    "status", refund.status.getDisplayName(),
                    "refundTime", refund.refundTime
                ),
                "message", "退款成功"
            ));
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            ctx.status(400).json(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (SQLException e) {
            logger.error("退款失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "退款失败"
            ));
        }
    }
    
    /**
     * 查询待支付订单
     * GET /api/payment/waiting
     */
    public static void getWaitingOrders(Context ctx) {
        try {
            int requestedLimit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(DEFAULT_WAITING_PAYMENT_LIMIT);
            int limit = Math.max(1, Math.min(requestedLimit, MAX_WAITING_PAYMENT_LIMIT));
            List<PaymentOrder> orders = DAOFactory.getInstance().getPaymentDAO().findWaitingOrders(limit);
            
            List<Map<String, Object>> orderList = orders.stream()
                .map(order -> Map.<String, Object>of(
                    PAYMENT_ID_FIELD, order.paymentId,
                    "merchantOrderNo", order.merchantOrderNo,
                    "amount", order.amount,
                    "channel", order.channel.getDisplayName(),
                    "createTime", order.createTime != null ? order.createTime.toString() : "",
                    "expireTime", order.expireTime != null ? order.expireTime.toString() : "",
                    "qrCodeContent", order.qrCodeContent != null ? order.qrCodeContent : ""
                ))
                .collect(Collectors.toList());
            
            ctx.json(Map.of(
                "success", true,
                "data", orderList,
                "limit", limit,
                "total", orderList.size()
            ));
            
        } catch (SQLException e) {
            logger.error("查询待支付订单失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "查询失败"
            ));
        }
    }
    
    /**
     * 关闭过期订单
     * POST /api/payment/close-expired
     */
    public static void closeExpired(Context ctx) {
        try {
            int count = PaymentService.closeExpiredOrders();
            
            ctx.json(Map.of(
                "success", true,
                "data", Map.of("closedCount", count),
                "message", "关闭过期订单: " + count + " 个"
            ));
            
        } catch (SQLException e) {
            logger.error("关闭过期订单失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "关闭失败"
            ));
        }
    }
    
    /**
     * 获取支付统计
     * GET /api/payment/stats/daily
     */
    public static void getDailyStats(Context ctx) {
        String dateStr = ctx.queryParam("date");
        
        try {
            LocalDate date = dateStr != null
                ? LocalDate.parse(dateStr, com.cashier.util.DateTimeFormats.DATE)
                : LocalDate.now(ZoneId.systemDefault());
            
            Map<String, Object> stats = DAOFactory.getInstance().getPaymentDAO().getDailyStats(java.sql.Date.valueOf(date));
            
            ctx.json(Map.of(
                "success", true,
                "data", stats,
                "date", date.format(com.cashier.util.DateTimeFormats.DATE)
            ));
            
        } catch (Exception e) {
            logger.error("获取支付统计失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "查询失败"
            ));
        }
    }
    
    /**
     * 获取支付配置
     * GET /api/payment/config
     */
    public static void getConfig(Context ctx) {
        PaymentService.PaymentConfig config = PaymentService.getConfig();
        
        ctx.json(Map.of(
            "success", true,
            "data", Map.of(
                "wechatEnabled", config.wechatEnabled,
                "alipayEnabled", config.alipayEnabled,
                "orderExpireMinutes", config.orderExpireMinutes,
                "notifyUrl", config.notifyUrl != null ? config.notifyUrl : ""
            )
        ));
    }
    
    /**
     * 设置支付配置
     * PUT /api/payment/config
     */
    public static void setConfig(Context ctx) {
        try {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            if (body == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "请求体不能为空"
                ));
                return;
            }
            
            PaymentService.PaymentConfig config = PaymentService.getConfig();
            
            if (body.containsKey("wechatEnabled")) {
                config.wechatEnabled = Boolean.parseBoolean(String.valueOf(body.get("wechatEnabled")));
            }
            if (body.containsKey("wechatAppId")) {
                config.wechatAppId = getString(body, "wechatAppId", null);
            }
            if (body.containsKey("wechatMchId")) {
                config.wechatMchId = getString(body, "wechatMchId", null);
            }
            if (body.containsKey("wechatApiKey")) {
                config.wechatApiKey = getString(body, "wechatApiKey", null);
            }
            if (body.containsKey("wechatCertPath")) {
                config.wechatCertPath = getString(body, "wechatCertPath", null);
            }
            if (body.containsKey("wechatPrivateKeyPath")) {
                config.wechatPrivateKeyPath = getString(body, "wechatPrivateKeyPath", null);
            }
            if (body.containsKey("wechatMerchantSerialNo")) {
                config.wechatMerchantSerialNo = getString(body, "wechatMerchantSerialNo", null);
            }
            
            if (body.containsKey("alipayEnabled")) {
                config.alipayEnabled = Boolean.parseBoolean(String.valueOf(body.get("alipayEnabled")));
            }
            if (body.containsKey("alipayAppId")) {
                config.alipayAppId = getString(body, "alipayAppId", null);
            }
            if (body.containsKey("alipayPrivateKey")) {
                config.alipayPrivateKey = getString(body, "alipayPrivateKey", null);
            }
            if (body.containsKey("alipayPublicKey")) {
                config.alipayPublicKey = getString(body, "alipayPublicKey", null);
            }
            if (body.containsKey("alipayCertPath")) {
                config.alipayCertPath = getString(body, "alipayCertPath", null);
            }
            if (body.containsKey("alipayGateway")) {
                config.alipayGateway = getString(body, "alipayGateway", null);
            }
            
            if (body.containsKey("orderExpireMinutes")) {
                config.orderExpireMinutes = getInt(body, "orderExpireMinutes", config.orderExpireMinutes);
            }
            if (body.containsKey("notifyUrl")) {
                config.notifyUrl = getString(body, "notifyUrl", null);
            }
            
            PaymentService.saveConfig(config);
            
            ctx.json(Map.of(
                "success", true,
                "message", "支付配置已更新"
            ));
            
        } catch (Exception e) {
            logger.error("设置支付配置失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "设置失败"
            ));
        }
    }
    
    /**
     * 构建支付订单返回数据
     */
    private static Map<String, Object> buildPaymentOrderData(PaymentOrder order) {
        Map<String, Object> data = new HashMap<>();
        data.put(PAYMENT_ID_FIELD, order.paymentId);
        data.put("merchantOrderNo", order.merchantOrderNo);
        data.put("transactionId", order.transactionId);
        data.put("amount", order.amount);
        data.put("paidAmount", order.paidAmount);
        data.put("channel", order.channel.getDisplayName());
        data.put("status", order.status.getDisplayName());
        data.put("isSuccess", order.status.isSuccess());
        data.put("createTime", order.createTime);
        data.put("payTime", order.payTime);
        data.put("expireTime", order.expireTime);
        data.put("channelTransactionId", order.channelTransactionId);
        return data;
    }

    private static String getString(Map<?, ?> body, String key, String defaultValue) {
        Object value = body.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static BigDecimal getBigDecimal(Map<?, ?> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private static int getInt(Map<?, ?> body, String key, int defaultValue) {
        Object value = body.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return defaultValue;
    }
}
