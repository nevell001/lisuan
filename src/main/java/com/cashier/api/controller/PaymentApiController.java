package com.cashier.api.controller;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.dao.PaymentDAO;
import com.cashier.service.PaymentService;
import com.cashier.api.sync.SyncManager;
import com.cashier.api.sync.SyncEventType;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 电子支付 REST API 控制器
 * 微信支付、支付宝支付管理
 */
public class PaymentApiController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentApiController.class);
    
    /**
     * 创建支付订单
     * POST /api/payment/create
     * Body: { "transactionId": "T123", "amount": 100.00, "channel": "WECHAT", "terminalId": "POS01" }
     */
    public static void createPayment(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            
            String transactionId = (String) body.get("transactionId");
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String channelStr = (String) body.getOrDefault("channel", "WECHAT");
            String terminalId = (String) body.getOrDefault("terminalId", "default");
            String operator = (String) body.getOrDefault("operator", "system");
            
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
                    "paymentId", order.paymentId,
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
                "error", "创建失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 查询支付状态
     * GET /api/payment/:paymentId/status
     */
    public static void queryStatus(Context ctx) {
        String paymentId = ctx.pathParam("paymentId");
        
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
                "error", "查询失败: " + e.getMessage()
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
            List<PaymentOrder> orders = PaymentDAO.findByTransactionId(transactionId);
            
            List<Map<String, Object>> orderList = orders.stream()
                .map(order -> Map.<String, Object>of(
                    "paymentId", order.paymentId,
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
                "error", "查询失败: " + e.getMessage()
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
            if (channel == PaymentOrder.PaymentChannel.WECHAT) {
                // 微信回调 XML
                String xml = ctx.body();
                // 简化解析（实际需要XML解析）
                notifyData.put("out_trade_no", extractXmlValue(xml, "out_trade_no"));
                notifyData.put("transaction_id", extractXmlValue(xml, "transaction_id"));
                notifyData.put("trade_status", extractXmlValue(xml, "result_code"));
                notifyData.put("total_amount", extractXmlValue(xml, "total_fee"));
            } else {
                // 支付宝回调
                Map<String, List<String>> params = ctx.formParamMap();
                notifyData.put("out_trade_no", params.containsKey("out_trade_no") ? params.get("out_trade_no").get(0) : "");
                notifyData.put("transaction_id", params.containsKey("trade_no") ? params.get("trade_no").get(0) : "");
                notifyData.put("trade_status", params.containsKey("trade_status") ? params.get("trade_status").get(0) : "");
                notifyData.put("total_amount", params.containsKey("total_amount") ? params.get("total_amount").get(0) : "0");
            }
            
            boolean success = PaymentService.handlePaymentNotify(channel, notifyData);
            
            // 返回响应
            if (channel == PaymentOrder.PaymentChannel.WECHAT) {
                ctx.result("<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>");
            } else {
                ctx.result("success");
            }
            
        } catch (Exception e) {
            logger.error("处理支付回调失败", e);
            if (channel == PaymentOrder.PaymentChannel.WECHAT) {
                ctx.result("<xml><return_code><![CDATA[FAIL]]></return_code></xml>");
            } else {
                ctx.result("fail");
            }
        }
    }
    
    /**
     * 简化XML值提取
     */
    private static String extractXmlValue(String xml, String key) {
        try {
            int start = xml.indexOf("<" + key + ">") + key.length() + 2;
            int end = xml.indexOf("</" + key + ">");
            if (start > 0 && end > start) {
                String value = xml.substring(start, end);
                // 去除CDATA
                if (value.startsWith("<![CDATA[")) {
                    value = value.substring(9, value.length() - 3);
                }
                return value;
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }
    
    /**
     * 申请退款
     * POST /api/payment/:paymentId/refund
     * Body: { "amount": 50.00, "reason": "商品质量问题", "operator": "张三" }
     */
    public static void applyRefund(Context ctx) {
        String paymentId = ctx.pathParam("paymentId");
        
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            
            BigDecimal refundAmount = new BigDecimal(body.get("amount").toString());
            String reason = (String) body.getOrDefault("reason", "用户申请退款");
            String operator = (String) body.getOrDefault("operator", "system");
            
            RefundRecord refund = PaymentService.applyRefund(paymentId, refundAmount, reason, operator);
            
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
                "error", "退款失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 查询待支付订单
     * GET /api/payment/waiting
     */
    public static void getWaitingOrders(Context ctx) {
        try {
            List<PaymentOrder> orders = PaymentDAO.findWaitingOrders();
            
            List<Map<String, Object>> orderList = orders.stream()
                .map(order -> Map.<String, Object>of(
                    "paymentId", order.paymentId,
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
                "total", orderList.size()
            ));
            
        } catch (SQLException e) {
            logger.error("查询待支付订单失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "查询失败: " + e.getMessage()
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
                "error", "关闭失败: " + e.getMessage()
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
            Date date = dateStr != null ? 
                new java.text.SimpleDateFormat("yyyy-MM-dd").parse(dateStr) : 
                new Date();
            
            Map<String, Object> stats = PaymentDAO.getDailyStats(date);
            
            ctx.json(Map.of(
                "success", true,
                "data", stats,
                "date", new java.text.SimpleDateFormat("yyyy-MM-dd").format(date)
            ));
            
        } catch (Exception e) {
            logger.error("获取支付统计失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "查询失败: " + e.getMessage()
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
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            
            PaymentService.PaymentConfig config = PaymentService.getConfig();
            
            if (body.containsKey("wechatEnabled")) {
                config.wechatEnabled = Boolean.parseBoolean(body.get("wechatEnabled").toString());
            }
            if (body.containsKey("wechatAppId")) {
                config.wechatAppId = (String) body.get("wechatAppId");
            }
            if (body.containsKey("wechatMchId")) {
                config.wechatMchId = (String) body.get("wechatMchId");
            }
            if (body.containsKey("wechatApiKey")) {
                config.wechatApiKey = (String) body.get("wechatApiKey");
            }
            
            if (body.containsKey("alipayEnabled")) {
                config.alipayEnabled = Boolean.parseBoolean(body.get("alipayEnabled").toString());
            }
            if (body.containsKey("alipayAppId")) {
                config.alipayAppId = (String) body.get("alipayAppId");
            }
            if (body.containsKey("alipayPrivateKey")) {
                config.alipayPrivateKey = (String) body.get("alipayPrivateKey");
            }
            
            if (body.containsKey("orderExpireMinutes")) {
                config.orderExpireMinutes = ((Number) body.get("orderExpireMinutes")).intValue();
            }
            if (body.containsKey("notifyUrl")) {
                config.notifyUrl = (String) body.get("notifyUrl");
            }
            
            PaymentService.setConfig(config);
            
            ctx.json(Map.of(
                "success", true,
                "message", "支付配置已更新"
            ));
            
        } catch (Exception e) {
            logger.error("设置支付配置失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "设置失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 构建支付订单返回数据
     */
    private static Map<String, Object> buildPaymentOrderData(PaymentOrder order) {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", order.paymentId);
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
}