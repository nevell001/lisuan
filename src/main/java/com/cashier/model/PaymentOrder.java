package com.cashier.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付订单模型
 * 用于电子支付（微信、支付宝）的订单记录
 */
public class PaymentOrder {
    
    /**
     * 支付订单ID（支付平台返回）
     */
    public String paymentId;
    
    /**
     * 关联的交易ID
     */
    public String transactionId;
    
    /**
     * 商户订单号（系统内部订单号）
     */
    public String merchantOrderNo;
    
    /**
     * 支付类型
     */
    public PaymentType paymentType;
    
    /**
     * 支付渠道
     */
    public PaymentChannel channel;
    
    /**
     * 支付金额
     */
    public BigDecimal amount;
    
    /**
     * 支付状态
     */
    public PaymentStatus status;
    
    /**
     * 支付二维码链接
     */
    public String qrCodeUrl;
    
    /**
     * 支付二维码内容（用于生成二维码图片）
     */
    public String qrCodeContent;
    
    /**
     * 用户实际支付金额（可能有优惠）
     */
    public BigDecimal paidAmount;
    
    /**
     * 优惠金额
     */
    public BigDecimal discountAmount;
    
    /**
     * 创建时间
     */
    public Date createTime;
    
    /**
     * 支付时间
     */
    public Date payTime;
    
    /**
     * 过期时间
     */
    public Date expireTime;
    
    /**
     * 支付渠道返回的交易号
     */
    public String channelTransactionId;
    
    /**
     * 支付渠道返回的用户ID（OpenId等）
     */
    public String channelUserId;
    
    /**
     * 支付备注
     */
    public String remark;
    
    /**
     * 终端ID
     */
    public String terminalId;
    
    /**
     * 操作员
     */
    public String operator;
    
    /**
     * 回调通知时间
     */
    public Date notifyTime;
    
    /**
     * 回调内容（JSON）
     */
    public String notifyContent;
    
    /**
     * 支付类型枚举
     */
    public enum PaymentType {
        SCAN_PAY("扫码支付"),       // 商户扫码用户付款码
        QRCODE_PAY("二维码支付"),    // 用户扫商户二维码
        APP_PAY("APP支付"),         // APP内唤起支付
        H5_PAY("H5支付"),           // 网页支付
        
        // 微信特有
        MINIAPP_PAY("小程序支付"),
        JSAPI_PAY("公众号支付"),
        
        // 支付宝特有
        WAP_PAY("WAP支付"),
        COMPUTER_PAY("电脑网站支付");
        
        private final String displayName;
        
        PaymentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 支付渠道枚举
     */
    public enum PaymentChannel {
        WECHAT("微信支付"),
        ALIPAY("支付宝"),
        CASH("现金"),
        CARD("银行卡"),
        MEMBER("会员余额"),
        MIXED("混合支付"),
        OTHER("其他");
        
        private final String displayName;
        
        PaymentChannel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * 从字符串解析
         */
        public static PaymentChannel fromString(String str) {
            for (PaymentChannel channel : values()) {
                if (channel.name().equalsIgnoreCase(str) || 
                    channel.displayName.equals(str)) {
                    return channel;
                }
            }
            return OTHER;
        }
    }
    
    /**
     * 支付状态枚举
     */
    public enum PaymentStatus {
        CREATED("已创建"),       // 订单已创建，等待支付
        WAITING("等待支付"),     // 等待用户支付
        PAYING("支付中"),       // 正在支付
        SUCCESS("支付成功"),    // 支付完成
        FAILED("支付失败"),     // 支付失败
        CANCELLED("已取消"),    // 用户取消
        CLOSED("已关闭"),       // 订单超时关闭
        REFUNDED("已退款"),     // 已退款
        PARTIAL_REFUND("部分退款"); // 部分退款
        
        private final String displayName;
        
        PaymentStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * 是否是终态（不可变更）
         */
        public boolean isFinal() {
            return this == SUCCESS || this == FAILED || 
                   this == CANCELLED || this == CLOSED || 
                   this == REFUNDED || this == PARTIAL_REFUND;
        }
        
        /**
         * 是否支付成功
         */
        public boolean isSuccess() {
            return this == SUCCESS;
        }
        
        /**
         * 是否可退款
         */
        public boolean canRefund() {
            return this == SUCCESS || this == PARTIAL_REFUND;
        }
    }
    
    /**
     * 默认构造函数
     */
    public PaymentOrder() {
        this.status = PaymentStatus.CREATED;
        this.createTime = new Date();
    }
    
    /**
     * 创建扫码支付订单
     */
    public static PaymentOrder createScanPayOrder(String transactionId, BigDecimal amount, 
                                                   PaymentChannel channel, String terminalId) {
        PaymentOrder order = new PaymentOrder();
        order.transactionId = transactionId;
        order.merchantOrderNo = generateMerchantOrderNo();
        order.paymentType = PaymentType.QRCODE_PAY;
        order.channel = channel;
        order.amount = amount;
        order.terminalId = terminalId;
        order.status = PaymentStatus.CREATED;
        
        // 默认15分钟过期
        order.expireTime = new Date(System.currentTimeMillis() + 15 * 60 * 1000);
        
        return order;
    }
    
    /**
     * 生成商户订单号
     */
    private static String generateMerchantOrderNo() {
        return "PAY" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
    
    @Override
    public String toString() {
        return "PaymentOrder{" +
                "paymentId='" + paymentId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", merchantOrderNo='" + merchantOrderNo + '\'' +
                ", channel=" + channel +
                ", amount=" + amount +
                ", status=" + status +
                '}';
    }
}