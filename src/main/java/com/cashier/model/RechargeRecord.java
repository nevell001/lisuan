package com.cashier.model;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RechargeRecord {
    public String recordId;      // 充值记录ID
    public String memberPhone;   // 会员手机号
    public String memberName;    // 会员姓名
    public BigDecimal amount;    // 充值金额
    public String paymentMethod; // 支付方式
    public Date timestamp;       // 充值时间
    public String operator;      // 操作员

    public RechargeRecord() {
        this.recordId = "";
        this.memberPhone = "";
        this.memberName = "";
        this.amount = BigDecimal.ZERO;
        this.paymentMethod = "现金";
        this.timestamp = new Date();
        this.operator = "系统";
    }

    public RechargeRecord(String recordId, String memberPhone, String memberName, BigDecimal amount, String paymentMethod, String operator) {
        this.recordId = recordId;
        this.memberPhone = memberPhone;
        this.memberName = memberName;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.paymentMethod = paymentMethod;
        this.timestamp = new Date();
        this.operator = operator;
    }

    public RechargeRecord(String recordId, String memberPhone, String memberName, double amount, String paymentMethod, String operator) {
        this(recordId, memberPhone, memberName, BigDecimal.valueOf(amount), paymentMethod, operator);
    }

    // Getter方法
    public String getRecordId() {
        return recordId;
    }

    public String getMemberPhone() {
        return memberPhone;
    }

    public String getMemberName() {
        return memberName;
    }

    public BigDecimal getAmount() {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getOperator() {
        return operator;
    }
}