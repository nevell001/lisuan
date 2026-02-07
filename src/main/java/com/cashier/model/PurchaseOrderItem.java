package com.cashier.model;

import java.math.BigDecimal;

/**
 * 采购订单明细模型
 */
public class PurchaseOrderItem {
    public int id;                   // 明细ID（数据库自增主键）
    public int orderId;              // 订单ID
    public int productId;            // 商品ID
    public String productName;       // 商品名称
    public int quantity;             // 采购数量
    public BigDecimal unitPrice;     // 单价
    public BigDecimal totalPrice;    // 小计
    public int inboundQuantity;      // 已入库数量

    public PurchaseOrderItem() {
        this.id = 0;
        this.orderId = 0;
        this.productId = 0;
        this.productName = "";
        this.quantity = 0;
        this.unitPrice = BigDecimal.ZERO;
        this.totalPrice = BigDecimal.ZERO;
        this.inboundQuantity = 0;
    }

    public PurchaseOrderItem(int orderId, int productId, String productName,
                             int quantity, BigDecimal unitPrice) {
        this();
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 计算小计
     */
    public void calculateTotal() {
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 获取未入库数量
     */
    public int getPendingQuantity() {
        return quantity - inboundQuantity;
    }

    /**
     * 是否全部入库
     */
    public boolean isInboundComplete() {
        return inboundQuantity >= quantity;
    }
}