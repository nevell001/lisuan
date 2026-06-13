package com.cashier.service;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.dao.PaymentDAO;
import com.cashier.api.sync.SyncManager;
import com.cashier.api.sync.SyncEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 电子支付服务
 * 统一管理微信支付、支付宝支付等电子支付渠道
 */
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    // 支付配置
    private static PaymentConfig config = new PaymentConfig();
    
    /**
     * 初始化支付服务
     */
    public static void init() {
        try {
            PaymentDAO.createTable();
            logger.info("支付服务初始化成功");
            
            // 加载配置
            loadConfig();
        } catch (Exception e) {
            logger.error("支付服务初始化失败", e);
        }
    }
    
    /**
     * 加载支付配置
     */
    private static void loadConfig() {
        java.io.File configFile = new java.io.File("config/payment.properties");
        if (configFile.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                java.util.Properties props = new java.util.Properties();
                props.load(fis);
                
                config.wechatEnabled = Boolean.parseBoolean(props.getProperty("wechat.enabled", "true"));
                config.wechatAppId = props.getProperty("wechat.app.id");
                config.wechatMchId = props.getProperty("wechat.mch.id");
                config.wechatApiKey = props.getProperty("wechat.api.key");
                
                config.alipayEnabled = Boolean.parseBoolean(props.getProperty("alipay.enabled", "true"));
                config.alipayAppId = props.getProperty("alipay.app.id");
                config.alipayPrivateKey = props.getProperty("alipay.private.key");
                
                config.orderExpireMinutes = Integer.parseInt(props.getProperty("order.expire.minutes", "15"));
                config.notifyUrl = props.getProperty("notify.url", "https://localhost:8080/api/payment/notify");
                
                logger.info("支付配置加载成功");
            } catch (Exception e) {
                logger.warn("加载支付配置失败，使用默认值: {}", e.getMessage());
                setDefaultConfig();
            }
        } else {
            logger.info("支付配置文件不存在，使用默认值");
            setDefaultConfig();
        }
    }
    
    /**
     * 设置默认配置
     */
    private static void setDefaultConfig() {
        config.wechatEnabled = true;
        config.alipayEnabled = true;
        config.orderExpireMinutes = 15;
        config.notifyUrl = "https://localhost:8080/api/payment/notify";
    }
    
    /**
     * 创建支付订单（二维码支付）
     * @param transactionId 交易ID
     * @param amount 支付金额
     * @param channel 支付渠道
     * @param terminalId 终端ID
     * @return 支付订单
     */
    public static PaymentOrder createPaymentOrder(String transactionId, BigDecimal amount,
                                                   PaymentOrder.PaymentChannel channel,
                                                   String terminalId) throws SQLException {
        // 创建订单
        PaymentOrder order = PaymentOrder.createScanPayOrder(transactionId, amount, channel, terminalId);
        
        // 根据渠道生成二维码
        switch (channel) {
            case WECHAT:
                generateWechatQrCode(order);
                break;
            case ALIPAY:
                generateAlipayQrCode(order);
                break;
            default:
                logger.warn("不支持的支付渠道: {}", channel);
        }
        
        // 保存订单
        PaymentDAO.insert(order);
        
        // 广播事件
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PAYMENT_ORDER_CREATED,
            Map.of("paymentId", order.paymentId, "amount", order.amount.toString(), "channel", channel.name()));
        
        logger.info("支付订单创建成功: {} - {} {}", order.merchantOrderNo, channel.getDisplayName(), amount);
        
        return order;
    }
    
    /**
     * 生成微信支付二维码
     */
    private static void generateWechatQrCode(PaymentOrder order) {
        // 模式二：生成支付二维码链接
        // 实际项目中需要调用微信支付API
        
        // 模拟二维码内容（实际应该调用微信支付API获取）
        String qrContent = generateMockWechatQrCode(order);
        order.qrCodeUrl = "weixin://wxpay/bizpayurl?pr=" + order.merchantOrderNo;
        order.qrCodeContent = qrContent;
        
        order.status = PaymentOrder.PaymentStatus.WAITING;
        
        logger.debug("微信支付二维码生成: {}", order.merchantOrderNo);
    }
    
    /**
     * 生成支付宝支付二维码
     */
    private static void generateAlipayQrCode(PaymentOrder order) {
        // 当面付：生成支付二维码
        // 实际项目中需要调用支付宝API
        
        // 模拟二维码内容
        String qrContent = generateMockAlipayQrCode(order);
        order.qrCodeUrl = "https://qr.alipay.com/" + order.merchantOrderNo;
        order.qrCodeContent = qrContent;
        
        order.status = PaymentOrder.PaymentStatus.WAITING;
        
        logger.debug("支付宝二维码生成: {}", order.merchantOrderNo);
    }
    
    /**
     * 模拟微信支付二维码（实际项目中调用真实API）
     */
    private static String generateMockWechatQrCode(PaymentOrder order) {
        // 实际项目中：
        // 1. 调用微信支付统一下单API
        // 2. 获取code_url
        // 3. 生成二维码图片
        
        // 模拟数据
        StringBuilder sb = new StringBuilder();
        sb.append("weixin://wxpay/bizpayurl?");
        sb.append("appid=").append(config.wechatAppId != null ? config.wechatAppId : "wxmock123456").append("&");
        sb.append("mch_id=").append(config.wechatMchId != null ? config.wechatMchId : "1234567890").append("&");
        sb.append("nonce_str=").append(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16)).append("&");
        sb.append("product_id=").append(order.merchantOrderNo).append("&");
        sb.append("time_stamp=").append(System.currentTimeMillis() / 1000);
        
        return sb.toString();
    }
    
    /**
     * 模拟支付宝二维码（实际项目中调用真实API）
     */
    private static String generateMockAlipayQrCode(PaymentOrder order) {
        // 实际项目中：
        // 1. 调用支付宝当面付预下单API (alipay.trade.precreate)
        // 2. 获取qr_code
        // 3. 生成二维码图片
        
        // 模拟数据
        StringBuilder sb = new StringBuilder();
        sb.append("https://qr.alipay.com/bax00");
        sb.append(order.merchantOrderNo);
        sb.append("_");
        sb.append(order.amount.setScale(2, RoundingMode.HALF_UP).toString().replace(".", ""));
        
        return sb.toString();
    }
    
    /**
     * 查询支付状态
     */
    public static PaymentOrder queryPaymentStatus(String paymentId) throws SQLException {
        PaymentOrder order = PaymentDAO.findById(paymentId);
        
        if (order == null) {
            logger.warn("支付订单不存在: {}", paymentId);
            return null;
        }
        
        // 如果订单未完成，主动查询支付渠道状态
        if (!order.status.isFinal()) {
            queryChannelStatus(order);
        }
        
        return order;
    }
    
    /**
     * 查询支付渠道状态
     */
    private static void queryChannelStatus(PaymentOrder order) {
        // 实际项目中调用支付渠道查询API
        
        switch (order.channel) {
            case WECHAT:
                queryWechatStatus(order);
                break;
            case ALIPAY:
                queryAlipayStatus(order);
                break;
        }
    }
    
    /**
     * 查询微信支付状态（模拟）
     */
    private static void queryWechatStatus(PaymentOrder order) {
        // 实际：调用 wxpay.orderquery
        // 这里模拟：检查是否超时
        if (order.expireTime != null && new Date().after(order.expireTime)) {
            try {
                PaymentDAO.updateStatus(order.paymentId, PaymentOrder.PaymentStatus.CLOSED);
                order.status = PaymentOrder.PaymentStatus.CLOSED;
            } catch (SQLException e) {
                logger.error("更新订单状态失败", e);
            }
        }
    }
    
    /**
     * 查询支付宝支付状态（模拟）
     */
    private static void queryAlipayStatus(PaymentOrder order) {
        // 实际：调用 alipay.trade.query
        // 同上模拟
        if (order.expireTime != null && new Date().after(order.expireTime)) {
            try {
                PaymentDAO.updateStatus(order.paymentId, PaymentOrder.PaymentStatus.CLOSED);
                order.status = PaymentOrder.PaymentStatus.CLOSED;
            } catch (SQLException e) {
                logger.error("更新订单状态失败", e);
            }
        }
    }
    
    /**
     * 处理支付回调通知
     * @param channel 渠道
     * @param notifyData 回调数据
     * @return 处理是否成功
     */
    public static boolean handlePaymentNotify(PaymentOrder.PaymentChannel channel, 
                                                Map<String, String> notifyData) throws SQLException {
        String merchantOrderNo = notifyData.get("out_trade_no");
        String channelTransactionId = notifyData.get("transaction_id");
        String tradeStatus = notifyData.get("trade_status");
        
        PaymentOrder order = PaymentDAO.findByMerchantOrderNo(merchantOrderNo);
        
        if (order == null) {
            logger.warn("回调订单不存在: {}", merchantOrderNo);
            return false;
        }
        
        // 更新回调信息
        try {
            PaymentDAO.updateNotifyInfo(order.paymentId, new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(notifyData));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.warn("JSON序列化回调数据失败", e);
        }
        
        // 处理支付结果
        boolean success = false;
        if ("SUCCESS".equals(tradeStatus) || "TRADE_SUCCESS".equals(tradeStatus)) {
            BigDecimal paidAmount = new BigDecimal(notifyData.getOrDefault("total_amount", "0"));
            BigDecimal discountAmount = new BigDecimal(notifyData.getOrDefault("discount_amount", "0"));
            String channelUserId = notifyData.get("buyer_id");
            
            PaymentDAO.updatePaymentSuccess(order.paymentId, channelTransactionId, 
                channelUserId, paidAmount, discountAmount);
            
            order.status = PaymentOrder.PaymentStatus.SUCCESS;
            order.payTime = new Date();
            
            // 广播支付成功事件
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PAYMENT_SUCCESS,
                Map.of("paymentId", order.paymentId, "transactionId", order.transactionId, 
                       "amount", paidAmount.toString()));
            
            success = true;
            logger.info("支付成功: {} - {}", order.merchantOrderNo, paidAmount);
            
        } else if ("CLOSED".equals(tradeStatus) || "TRADE_CLOSED".equals(tradeStatus)) {
            PaymentDAO.updateStatus(order.paymentId, PaymentOrder.PaymentStatus.CLOSED);
            order.status = PaymentOrder.PaymentStatus.CLOSED;
            
            logger.info("订单关闭: {}", order.merchantOrderNo);
        }
        
        return success;
    }
    
    /**
     * 申请退款
     */
    public static RefundRecord applyRefund(String paymentId, BigDecimal refundAmount,
                                            String reason, String operator) throws SQLException {
        PaymentOrder order = PaymentDAO.findById(paymentId);
        
        if (order == null) {
            throw new IllegalArgumentException("支付订单不存在: " + paymentId);
        }
        
        if (!order.status.canRefund()) {
            throw new IllegalStateException("订单状态不允许退款: " + order.status);
        }
        
        if (refundAmount.compareTo(order.paidAmount != null ? order.paidAmount : order.amount) > 0) {
            throw new IllegalArgumentException("退款金额超过已支付金额");
        }
        
        // 创建退款记录
        RefundRecord refund = RefundRecord.create(paymentId, refundAmount, reason, operator);
        refund.transactionId = order.transactionId;
        refund.originalAmount = order.paidAmount != null ? order.paidAmount : order.amount;
        refund.channel = order.channel.name();
        
        // 调用退款API（模拟）
        processRefund(refund, order);
        
        // 保存退款记录
        PaymentDAO.insertRefund(refund);
        
        // 更新支付订单状态
        if (refundAmount.compareTo(refund.originalAmount) == 0) {
            PaymentDAO.updateStatus(paymentId, PaymentOrder.PaymentStatus.REFUNDED);
        } else {
            PaymentDAO.updateStatus(paymentId, PaymentOrder.PaymentStatus.PARTIAL_REFUND);
        }
        
        // 广播退款事件
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PAYMENT_REFUND,
            Map.of("refundId", refund.refundId, "paymentId", paymentId, "amount", refundAmount.toString()));
        
        logger.info("退款申请成功: {} - {}", refund.merchantRefundNo, refundAmount);
        
        return refund;
    }
    
    /**
     * 处理退款（调用渠道API）
     */
    private static void processRefund(RefundRecord refund, PaymentOrder order) {
        // 实际项目中调用退款API
        
        switch (order.channel) {
            case WECHAT:
                processWechatRefund(refund, order);
                break;
            case ALIPAY:
                processAlipayRefund(refund, order);
                break;
        }
    }
    
    /**
     * 微信退款（模拟）
     */
    private static void processWechatRefund(RefundRecord refund, PaymentOrder order) {
        // 实际：调用 secapi.pay.refund
        // 模拟成功
        refund.status = RefundRecord.RefundStatus.SUCCESS;
        refund.channelRefundNo = "WX_RFD_" + System.currentTimeMillis();
        refund.refundTime = new Date();
    }
    
    /**
     * 支付宝退款（模拟）
     */
    private static void processAlipayRefund(RefundRecord refund, PaymentOrder order) {
        // 实际：调用 alipay.trade.refund
        // 模拟成功
        refund.status = RefundRecord.RefundStatus.SUCCESS;
        refund.channelRefundNo = "ALI_RFD_" + System.currentTimeMillis();
        refund.refundTime = new Date();
    }
    
    /**
     * 关闭过期订单
     */
    public static int closeExpiredOrders() throws SQLException {
        int count = PaymentDAO.closeExpiredOrders();
        
        if (count > 0) {
            logger.info("关闭过期订单: {} 个", count);
        }
        
        return count;
    }
    
    /**
     * 获取支付配置
     */
    public static PaymentConfig getConfig() {
        return config;
    }
    
    /**
     * 设置支付配置
     */
    public static void setConfig(PaymentConfig newConfig) {
        config = newConfig;
    }
    
    /**
     * 支付配置类
     */
    public static class PaymentConfig {
        // 微信支付配置
        public String wechatAppId;
        public String wechatMchId;
        public String wechatApiKey;
        public String wechatCertPath;
        public boolean wechatEnabled;
        
        // 支付宝配置
        public String alipayAppId;
        public String alipayPrivateKey;
        public String alipayPublicKey;
        public String alipayCertPath;
        public boolean alipayEnabled;
        
        // 通用配置
        public int orderExpireMinutes = 15;
        public String notifyUrl;
        public String returnUrl;
    }
}