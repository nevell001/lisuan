package com.cashier.model;

import java.math.BigDecimal;
import java.util.List;

public class Transaction {
    public String transactionId;
    public String timestamp;
    public List<Product> items;
    public BigDecimal totalAmount;
    public BigDecimal tax;
    public BigDecimal finalAmount;
    public String paymentMethod;  // 支付方式：现金、微信支付、支付宝、银行卡、组合支付
    public int memberId;         // 会员ID
    public String memberPhone;    // 会员手机号
    public String memberName;     // 会员姓名
    public String operatorUsername; // 操作员用户名
    public String operatorName;    // 操作员姓名

    public Transaction() {
        this.transactionId = "";
        this.timestamp = "";
        this.items = null;
        this.totalAmount = BigDecimal.ZERO;
        this.tax = BigDecimal.ZERO;
        this.finalAmount = BigDecimal.ZERO;
        this.paymentMethod = "";
        this.memberPhone = "";
        this.operatorUsername = "";
        this.operatorName = "";
    }

    public Transaction(String transactionId, String timestamp, List<Product> items, BigDecimal totalAmount, BigDecimal tax, BigDecimal finalAmount) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.items = items;
        this.totalAmount = defaultDecimal(totalAmount);
        this.tax = defaultDecimal(tax);
        this.finalAmount = defaultDecimal(finalAmount);
        this.paymentMethod = "";
        this.memberPhone = "";
    }

    public Transaction(String transactionId, String timestamp, List<Product> items, double totalAmount, double tax, double finalAmount) {
        this(transactionId, timestamp, items, BigDecimal.valueOf(totalAmount), BigDecimal.valueOf(tax), BigDecimal.valueOf(finalAmount));
    }

    public Transaction(String transactionId, String timestamp, List<Product> items, BigDecimal totalAmount, BigDecimal tax, BigDecimal finalAmount, String paymentMethod) {
        this(transactionId, timestamp, items, totalAmount, tax, finalAmount);
        this.paymentMethod = paymentMethod;
    }

    public Transaction(String transactionId, String timestamp, List<Product> items, double totalAmount, double tax, double finalAmount, String paymentMethod) {
        this(transactionId, timestamp, items, BigDecimal.valueOf(totalAmount), BigDecimal.valueOf(tax), BigDecimal.valueOf(finalAmount), paymentMethod);
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // Getter方法
    public String getTransactionId() {
        return transactionId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<Product> getItems() {
        return items;
    }

    public BigDecimal getTotalAmount() {
        return defaultDecimal(totalAmount);
    }

    public BigDecimal getTax() {
        return defaultDecimal(tax);
    }

    public BigDecimal getFinalAmount() {
        return defaultDecimal(finalAmount);
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getMemberPhone() {
        return memberPhone;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getPaymentMethodText() {
        switch (paymentMethod) {
            case "CASH":
                return "现金";
            case "WECHAT":
                return "微信";
            case "ALIPAY":
                return "支付宝";
            case "CARD":
                return "银行卡";
            default:
                return paymentMethod;
        }
    }
}