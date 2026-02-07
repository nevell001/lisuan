package com.cashier.model;

import java.math.BigDecimal;

/**
 * 采购入库明细模型
 */
public class PurchaseInboundItem {
    public int id;                   // 明细ID（数据库自增主键）
    public int inboundId;            // 入库单ID
    public int orderItemId;          // 订单明细ID
    public int productId;            // 商品ID
    public String productName;       // 商品名称
    public int quantity;             // 入库数量
    public BigDecimal unitPrice;     // 单价
    public BigDecimal totalPrice;    // 小计

    public PurchaseInboundItem() {
        this.id = 0;
        this.inboundId = 0;
        this.orderItemId = 0;
        this.productId = 0;
        this.productName = "";
        this.quantity = 0;
        this.unitPrice = BigDecimal.ZERO;
        this.totalPrice = BigDecimal.ZERO;
    }

    public PurchaseInboundItem(int inboundId, int orderItemId, int productId,
                               String productName, int quantity, BigDecimal unitPrice) {
        this();
        this.inboundId = inboundId;
        this.orderItemId = orderItemId;
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
}