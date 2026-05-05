package com.cashier.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 退款记录模型
 */
public class RefundRecord {
    
    /**
     * 退款ID
     */
    public String refundId;
    
    /**
     * 原支付订单ID
     */
    public String paymentId;
    
    /**
     * 原交易ID
     */
    public String transactionId;
    
    /**
     * 商户退款单号
     */
    public String merchantRefundNo;
    
    /**
     * 渠道退款单号
     */
    public String channelRefundNo;
    
    /**
     * 退款金额
     */
    public BigDecimal refundAmount;
    
    /**
     * 原订单金额
     */
    public BigDecimal originalAmount;
    
    /**
     * 退款原因
     */
    public String reason;
    
    /**
     * 退款状态
     */
    public RefundStatus status;
    
    /**
     * 退款渠道
     */
    public String channel;
    
    /**
     * 申请时间
     */
    public Date createTime;
    
    /**
     * 退款完成时间
     */
    public Date refundTime;
    
    /**
     * 操作员
     */
    public String operator;
    
    /**
     * 退款状态枚举
     */
    public enum RefundStatus {
        APPLYING("申请中"),
        PROCESSING("处理中"),
        SUCCESS("退款成功"),
        FAILED("退款失败"),
        CLOSED("退款关闭");
        
        private final String displayName;
        
        RefundStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isSuccess() {
            return this == SUCCESS;
        }
        
        public boolean isFinal() {
            return this == SUCCESS || this == FAILED || this == CLOSED;
        }
    }
    
    /**
     * 默认构造函数
     */
    public RefundRecord() {
        this.status = RefundStatus.APPLYING;
        this.createTime = new Date();
    }
    
    /**
     * 创建退款记录
     */
    public static RefundRecord create(String paymentId, BigDecimal refundAmount, 
                                       String reason, String operator) {
        RefundRecord record = new RefundRecord();
        record.paymentId = paymentId;
        record.merchantRefundNo = generateRefundNo();
        record.refundAmount = refundAmount;
        record.reason = reason;
        record.operator = operator;
        return record;
    }
    
    /**
     * 生成退款单号
     */
    private static String generateRefundNo() {
        return "RFD" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
}