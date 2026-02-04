package com.cashier.model;

import java.util.List;

public class Transaction {
    public String transactionId;
    public String timestamp;
    public List<Product> items;
    public double totalAmount;
    public double tax;
    public double finalAmount;
    public String paymentMethod;  // 支付方式：现金、微信支付、支付宝、银行卡、组合支付
    public String memberPhone;    // 会员手机号
    public String operatorUsername; // 操作员用户名
    public String operatorName;    // 操作员姓名

    public Transaction() {
        this.transactionId = "";
        this.timestamp = "";
        this.items = null;
        this.totalAmount = 0.0;
        this.tax = 0.0;
        this.finalAmount = 0.0;
        this.paymentMethod = "";
        this.memberPhone = "";
        this.operatorUsername = "";
        this.operatorName = "";
    }

    public Transaction(String transactionId, String timestamp, List<Product> items, double totalAmount, double tax, double finalAmount) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.items = items;
        this.totalAmount = totalAmount;
        this.tax = tax;
        this.finalAmount = finalAmount;
        this.paymentMethod = "";
        this.memberPhone = "";
    }

    public Transaction(String transactionId, String timestamp, List<Product> items, double totalAmount, double tax, double finalAmount, String paymentMethod) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.items = items;
        this.totalAmount = totalAmount;
        this.tax = tax;
        this.finalAmount = finalAmount;
        this.paymentMethod = paymentMethod;
        this.memberPhone = "";
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

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getTax() {
        return tax;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getMemberPhone() {
        return memberPhone;
    }
}