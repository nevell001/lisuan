package com.cashier.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 发票商品明细
 */
public class InvoiceItem {
    public String productName;       // 商品名称
    public String specification;     // 规格型号
    public String unit;              // 单位
    public int quantity;             // 数量
    public BigDecimal unitPrice;     // 单价（不含税）
    public BigDecimal amount;        // 金额（不含税）
    public BigDecimal taxRate;       // 税率
    public BigDecimal taxAmount;     // 税额
    public BigDecimal totalAmount;   // 价税合计
    
    public InvoiceItem() {
        this.productName = "";
        this.specification = "";
        this.unit = "个";
        this.quantity = 0;
        this.unitPrice = BigDecimal.ZERO;
        this.amount = BigDecimal.ZERO;
        this.taxRate = new BigDecimal("0.13");
        this.taxAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    public InvoiceItem(String productName, int quantity, BigDecimal unitPrice) {
        this();
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    
    /**
     * 计算金额
     */
    public void calculateAmount(BigDecimal taxRate) {
        this.taxRate = taxRate;
        this.amount = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        this.taxAmount = this.amount.multiply(this.taxRate);
        this.totalAmount = this.amount.add(this.taxAmount);
    }
    
    /**
     * 从商品转换
     */
    public static InvoiceItem fromProduct(Product product, int quantity) {
        InvoiceItem item = new InvoiceItem();
        item.productName = product.name;
        item.specification = product.spec != null ? product.spec : "";
        item.unit = product.unit != null ? product.unit : "个";
        item.quantity = quantity;
        
        // 价格不含税计算（假设价格是含税价）
        BigDecimal taxRate = new BigDecimal("0.13");
        BigDecimal taxDivisor = BigDecimal.ONE.add(taxRate);
        item.unitPrice = product.price.divide(taxDivisor, 2, RoundingMode.HALF_UP);
        
        item.calculateAmount(taxRate);
        
        return item;
    }
}