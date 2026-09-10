package com.cashier.service;

import com.cashier.api.sync.SyncEventType;
import com.cashier.api.sync.SyncManager;
import com.cashier.dao.DAOFactory;
import com.cashier.exception.DatabaseException;
import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.service.payment.AlipayPrecreatePaymentProvider;
import com.cashier.service.payment.MockPaymentChannelProvider;
import com.cashier.service.payment.PaymentChannelProvider;
import com.cashier.service.payment.UnavailablePaymentChannelProvider;
import com.cashier.service.payment.WechatNativePaymentProvider;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/** 电子支付编排服务。渠道协议、签名和网络调用由 PaymentChannelProvider 实现。 */
public final class PaymentService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PaymentService.class);
    private static final String PAYMENT_CONFIG_PATH = "config/payment.properties";
    private static final Map<PaymentOrder.PaymentChannel, PaymentChannelProvider> providers =
        new EnumMap<>(PaymentOrder.PaymentChannel.class);
    private static PaymentConfig config = new PaymentConfig();

    static {
        rebuildProviders();
    }

    private PaymentService() {
    }

    public static void init() {
        try {
            DAOFactory.getInstance().getPaymentDAO().createTable();
            reloadConfig();
            logger.info("支付服务初始化成功，模式: {}", config.mode);
        } catch (Exception e) {
            logger.error("支付服务初始化失败", e);
            throw new DatabaseException("支付服务初始化失败", DatabaseException.DbErrorType.CONNECTION_FAILED, e);
        }
    }

    public static synchronized void reloadConfig() {
        PaymentConfig loaded = new PaymentConfig();
        File file = new File(PAYMENT_CONFIG_PATH);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(fis);
                loaded.mode = props.getProperty("payment.mode", "disabled").trim().toLowerCase();
                loaded.mockEnabled = Boolean.parseBoolean(props.getProperty("payment.mock.enabled", "false"));
                loaded.mockCallbackSecret = props.getProperty("payment.mock.callback.secret");
                loaded.wechatEnabled = Boolean.parseBoolean(props.getProperty("wechat.enabled", "false"));
                loaded.wechatAppId = props.getProperty("wechat.app.id");
                loaded.wechatMchId = props.getProperty("wechat.mch.id");
                loaded.wechatApiKey = props.getProperty("wechat.api.key");
                loaded.wechatCertPath = props.getProperty("wechat.cert.path");
                loaded.wechatPrivateKeyPath = props.getProperty("wechat.private.key.path");
                loaded.wechatMerchantSerialNo = props.getProperty("wechat.merchant.serial.no");
                loaded.alipayEnabled = Boolean.parseBoolean(props.getProperty("alipay.enabled", "false"));
                loaded.alipayAppId = props.getProperty("alipay.app.id");
                loaded.alipayPrivateKey = props.getProperty("alipay.private.key");
                loaded.alipayPublicKey = props.getProperty("alipay.public.key");
                loaded.alipayCertPath = props.getProperty("alipay.cert.path");
                loaded.alipayGateway = props.getProperty("alipay.gateway");
                loaded.orderExpireMinutes = Integer.parseInt(props.getProperty("order.expire.minutes", "15"));
                loaded.notifyUrl = props.getProperty("notify.url");
                loaded.returnUrl = props.getProperty("return.url");
            } catch (Exception e) {
                logger.warn("支付配置无效，电子支付保持禁用: {}", e.getMessage());
            }
        }
        setConfig(loaded);
    }

    public static synchronized void saveConfig(PaymentConfig newConfig) {
        PaymentConfig nextConfig = newConfig == null ? new PaymentConfig() : newConfig;
        File file = new File(PAYMENT_CONFIG_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("无法创建支付配置目录: " + parent.getAbsolutePath());
        }

        Properties props = new Properties();
        props.setProperty("payment.mode", safe(nextConfig.mode, "disabled"));
        props.setProperty("payment.mock.enabled", String.valueOf(nextConfig.mockEnabled));
        props.setProperty("payment.mock.callback.secret", safe(nextConfig.mockCallbackSecret, ""));
        props.setProperty("wechat.enabled", String.valueOf(nextConfig.wechatEnabled));
        props.setProperty("wechat.app.id", safe(nextConfig.wechatAppId, ""));
        props.setProperty("wechat.mch.id", safe(nextConfig.wechatMchId, ""));
        props.setProperty("wechat.api.key", safe(nextConfig.wechatApiKey, ""));
        props.setProperty("wechat.cert.path", safe(nextConfig.wechatCertPath, ""));
        props.setProperty("wechat.private.key.path", safe(nextConfig.wechatPrivateKeyPath, ""));
        props.setProperty("wechat.merchant.serial.no", safe(nextConfig.wechatMerchantSerialNo, ""));
        props.setProperty("alipay.enabled", String.valueOf(nextConfig.alipayEnabled));
        props.setProperty("alipay.app.id", safe(nextConfig.alipayAppId, ""));
        props.setProperty("alipay.private.key", safe(nextConfig.alipayPrivateKey, ""));
        props.setProperty("alipay.public.key", safe(nextConfig.alipayPublicKey, ""));
        props.setProperty("alipay.cert.path", safe(nextConfig.alipayCertPath, ""));
        props.setProperty("alipay.gateway", safe(nextConfig.alipayGateway, ""));
        props.setProperty("order.expire.minutes", String.valueOf(nextConfig.orderExpireMinutes));
        props.setProperty("notify.url", safe(nextConfig.notifyUrl, ""));
        props.setProperty("return.url", safe(nextConfig.returnUrl, ""));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "LiSuan payment configuration");
        } catch (Exception e) {
            throw new IllegalStateException("保存支付配置失败", e);
        }

        setConfig(nextConfig);
    }

    private static String safe(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static synchronized void registerProvider(PaymentChannelProvider provider) {
        if (provider == null || provider.channel() == null) {
            throw new IllegalArgumentException("支付渠道适配器不能为空");
        }
        providers.put(provider.channel(), provider);
    }

    public static boolean isChannelAvailable(PaymentOrder.PaymentChannel channel) {
        PaymentChannelProvider provider = providers.get(channel);
        return provider != null && provider.isAvailable();
    }

    public static String getChannelUnavailableReason(PaymentOrder.PaymentChannel channel) {
        PaymentChannelProvider provider = providers.get(channel);
        return provider == null ? "不支持的支付渠道" : provider.unavailableReason();
    }

    public static PaymentOrder createPaymentOrder(String transactionId, BigDecimal amount,
                                                   PaymentOrder.PaymentChannel channel,
                                                   String terminalId) throws SQLException {
        if (transactionId == null || transactionId.isBlank() || amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("支付订单参数无效");
        }
        PaymentChannelProvider provider = requireProvider(channel);
        PaymentOrder order = PaymentOrder.createScanPayOrder(transactionId, amount, channel, terminalId);
        order.expireTime = new Date(System.currentTimeMillis() + config.orderExpireMinutes * 60_000L);
        provider.createOrder(order);
        DAOFactory.getInstance().getPaymentDAO().insert(order);
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PAYMENT_ORDER_CREATED,
            Map.of("paymentId", order.paymentId, "amount", amount.toString(), "channel", channel.name()));
        return order;
    }

    public static PaymentOrder queryPaymentStatus(String paymentId) throws SQLException {
        PaymentOrder order = DAOFactory.getInstance().getPaymentDAO().findById(paymentId);
        if (order == null || order.status.isFinal()) {
            return order;
        }
        PaymentOrder.PaymentStatus status = requireProvider(order.channel).queryStatus(order);
        if (status != order.status) {
            if (status == PaymentOrder.PaymentStatus.SUCCESS) {
                DAOFactory.getInstance().getPaymentDAO().updatePaymentSuccess(
                    order.paymentId,
                    order.channelTransactionId,
                    order.channelUserId,
                    order.paidAmount != null ? order.paidAmount : order.amount,
                    order.discountAmount != null ? order.discountAmount : BigDecimal.ZERO
                );
            } else {
                DAOFactory.getInstance().getPaymentDAO().updateStatus(order.paymentId, status);
            }
            order.status = status;
        }
        return order;
    }

    /**
     * 取渠道交易号：支付宝回调用 {@code trade_no}，微信 v3 用 {@code transaction_id}。
     *
     * <p>不能在回调入口把 {@code trade_no} 复制成 {@code transaction_id}：支付宝的验签内容
     * 由回调的全部非空参数拼成，多出的合成字段会让真实回调验签失败。</p>
     */
    private static String channelTransactionId(Map<String, String> notifyData) {
        String transactionId = notifyData.get("transaction_id");
        if (transactionId != null && !transactionId.isBlank()) {
            return transactionId;
        }
        return notifyData.get("trade_no");
    }

    public static boolean handlePaymentNotify(PaymentOrder.PaymentChannel channel,
                                               Map<String, String> notifyData) throws SQLException {
        PaymentChannelProvider provider = requireProvider(channel);
        if (!provider.verifyNotification(notifyData)) {
            logger.warn("拒绝未通过签名验证的支付回调: channel={}, mock_signature={}, secret_configured={}",
                channel,
                notifyData.get("mock_signature") != null ? "present" : "missing",
                config != null && config.mockCallbackSecret != null && !config.mockCallbackSecret.isBlank());
            return false;
        }
        String merchantOrderNo = notifyData.get("out_trade_no");
        PaymentOrder order = DAOFactory.getInstance().getPaymentDAO().findByMerchantOrderNo(merchantOrderNo);
        if (order == null || order.channel != channel) {
            return false;
        }

        // 幂等与终态保护：已成功订单重复通知直接确认；已终结订单拒绝迟到回调，防止状态回退
        if (order.status == PaymentOrder.PaymentStatus.SUCCESS) {
            logger.info("收到重复支付成功通知，幂等确认: order={}", merchantOrderNo);
            return true;
        }
        if (order.status == PaymentOrder.PaymentStatus.REFUNDED
                || order.status == PaymentOrder.PaymentStatus.CLOSED
                || order.status == PaymentOrder.PaymentStatus.CANCELLED) {
            logger.warn("拒绝已终结订单的支付回调: order={}, status={}", merchantOrderNo, order.status);
            return false;
        }

        String tradeStatus = notifyData.get("trade_status");
        if (!("SUCCESS".equals(tradeStatus) || "TRADE_SUCCESS".equals(tradeStatus))) {
            return false;
        }
        BigDecimal paidAmount = new BigDecimal(notifyData.getOrDefault("total_amount", "0"));
        if (paidAmount.compareTo(order.amount) != 0) {
            logger.warn("支付回调金额不一致: order={}, expected={}, actual={}",
                merchantOrderNo, order.amount, paidAmount);
            return false;
        }
        // 两次 DB 更新包裹在同一事务中，确保原子性
        DatabaseManager.executeBooleanTransaction(conn -> {
            DAOFactory.getInstance().getPaymentDAO().updateNotifyInfoWithConnection(conn, order.paymentId, notifyData.toString());
            DAOFactory.getInstance().getPaymentDAO().updatePaymentSuccessWithConnection(conn, order.paymentId, channelTransactionId(notifyData),
                notifyData.get("buyer_id"), paidAmount, BigDecimal.ZERO);
            return true;
        });
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PAYMENT_SUCCESS,
            Map.of("paymentId", order.paymentId, "transactionId", order.transactionId, "amount", paidAmount.toString()));
        return true;
    }

    public static RefundRecord applyRefund(String paymentId, BigDecimal refundAmount,
                                           String reason, String operator) throws SQLException {
        PaymentOrder order = DAOFactory.getInstance().getPaymentDAO().findById(paymentId);
        if (order == null) throw new IllegalArgumentException("支付订单不存在: " + paymentId);
        if (!order.status.canRefund()) throw new IllegalStateException("订单状态不允许退款: " + order.status);
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("退款金额必须大于零");
        BigDecimal refundable = order.paidAmount != null ? order.paidAmount : order.amount;
        if (refundAmount.compareTo(refundable) > 0) throw new IllegalArgumentException("退款金额超过已支付金额");

        RefundRecord refund = RefundRecord.create(paymentId, refundAmount, reason, operator);
        refund.transactionId = order.transactionId;
        refund.originalAmount = refundable;
        refund.channel = order.channel.name();
        requireProvider(order.channel).refund(order, refund);
        DAOFactory.getInstance().getPaymentDAO().insertRefund(refund);
        DAOFactory.getInstance().getPaymentDAO().updateStatus(paymentId, refundAmount.compareTo(refundable) == 0
            ? PaymentOrder.PaymentStatus.REFUNDED : PaymentOrder.PaymentStatus.PARTIAL_REFUND);
        AuditService.success(operator, "REFUND", "PAYMENT_REFUND",
            "退款单=" + refund.merchantRefundNo + ", 支付单=" + paymentId + ", 金额=" + refundAmount, 1);
        return refund;
    }

    public static int closeExpiredOrders() throws SQLException {
        return DAOFactory.getInstance().getPaymentDAO().closeExpiredOrders();
    }

    public static boolean cancelPaymentOrder(String paymentId) throws SQLException {
        return DAOFactory.getInstance().getPaymentDAO().updateStatusIfPending(paymentId, PaymentOrder.PaymentStatus.CANCELLED);
    }

    public static PaymentConfig getConfig() { return config; }

    public static synchronized void setConfig(PaymentConfig newConfig) {
        config = newConfig == null ? new PaymentConfig() : newConfig;
        rebuildProviders();
    }

    private static PaymentChannelProvider requireProvider(PaymentOrder.PaymentChannel channel) {
        PaymentChannelProvider provider = providers.get(channel);
        if (provider == null || !provider.isAvailable()) {
            throw new IllegalStateException(getChannelUnavailableReason(channel));
        }
        return provider;
    }

    private static synchronized void rebuildProviders() {
        providers.clear();
        registerConfiguredProvider(PaymentOrder.PaymentChannel.WECHAT, config.wechatEnabled);
        registerConfiguredProvider(PaymentOrder.PaymentChannel.ALIPAY, config.alipayEnabled);
    }

    private static void registerConfiguredProvider(PaymentOrder.PaymentChannel channel, boolean enabled) {
        if (enabled && "mock".equals(config.mode) && config.mockEnabled) {
            providers.put(channel, new MockPaymentChannelProvider(channel, config.mockCallbackSecret));
            return;
        }
        if (enabled && "production".equals(config.mode)) {
            registerProductionProvider(channel);
            return;
        }
        String reason = !enabled ? "支付渠道未启用"
            : "支付模式未启用生产适配器";
        providers.put(channel, new UnavailablePaymentChannelProvider(channel, reason));
    }

    private static void registerProductionProvider(PaymentOrder.PaymentChannel channel) {
        PaymentChannelProvider provider = channel == PaymentOrder.PaymentChannel.WECHAT
            ? new WechatNativePaymentProvider(config)
            : new AlipayPrecreatePaymentProvider(config);
        if (provider.isAvailable()) {
            providers.put(channel, provider);
        } else {
            providers.put(channel, new UnavailablePaymentChannelProvider(channel, provider.unavailableReason()));
        }
    }

    public static class PaymentConfig {
        public String mode = "disabled";
        public boolean mockEnabled;
        public String mockCallbackSecret;
        public String wechatAppId;
        public String wechatMchId;
        public String wechatApiKey;
        public String wechatCertPath;
        public String wechatPrivateKeyPath;
        public String wechatMerchantSerialNo;
        public boolean wechatEnabled;
        public String alipayAppId;
        public String alipayPrivateKey;
        public String alipayPublicKey;
        public String alipayCertPath;
        public String alipayGateway;
        public boolean alipayEnabled;
        public int orderExpireMinutes = 15;
        public String notifyUrl;
        public String returnUrl;
    }
}
