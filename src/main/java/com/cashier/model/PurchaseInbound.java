package com.cashier.model;

import java.sql.Timestamp;
import java.math.BigDecimal;

/**
 * 采购入库记录模型
 */
public class PurchaseInbound {
    public int id;                   // 入库ID（数据库自增主键）
    public String inboundNo;         // 入库单号
    public int orderId;              // 采购订单ID
    public String orderNo;           // 采购订单号
    public String inboundDate;       // 入库日期（yyyy-MM-dd）
    public int totalQuantity;        // 入库总数量
    public BigDecimal totalAmount;   // 入库总金额
    public String operator;          // 操作人
    public String remark;            // 备注
    public Timestamp createTime;     // 创建时间

    public PurchaseInbound() {
        this.id = 0;
        this.inboundNo = "";
        this.orderId = 0;
        this.orderNo = "";
        this.inboundDate = "";
        this.totalQuantity = 0;
        this.totalAmount = BigDecimal.ZERO;
        this.operator = "";
        this.remark = "";
        this.createTime = new Timestamp(System.currentTimeMillis());
    }

    public PurchaseInbound(int orderId, String orderNo, String inboundDate, String operator) {
        this();
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.inboundDate = inboundDate;
        this.operator = operator;
    }
}