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
        BigDecimal rate = taxRate != null ? taxRate : BigDecimal.ZERO;
        // 逐行取整到分：invoice_items 的 amount/tax_amount 是 DECIMAL(10,2)，落库本来就会取整；
        // 表头若用未取整值累加，就会出现"表头 13.00、明细之和 12.99"的 1 分差
        // （3 × 33.33 @13%：每行税额 4.3329 → 存 4.33，Σ=12.99，表头却是 13.00，TD-034）
        this.amount = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity))
            .setScale(2, RoundingMode.HALF_UP);
        this.taxAmount = this.amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = this.amount.add(this.taxAmount);
    }
    
    /**
     * 从商品转换（开票明细）。
     *
     * <p>{@code product.price} 是**含税**原价，而电子发票明细要求填不含税单价，所以要按
     * {@code 税率} 换算（{@code 含税单价 / (1 + 税率)}）。此前这里把税率硬编码成 13%，
     * 而税额又按 {@code invoice.taxRate}（可能来自请求体）计算，两个税率不一致时
     * 价税合计会直接算错（审计 F3）：106 元商品按 6% 开票会得到 99.44。</p>
     *
     * @param product        商品（{@code price} 为含税价）
     * @param quantity       数量
     * @param taxRate        本次开票使用的税率（小数，如 0.13）；null 视为 0
     * @param grossUnitPrice 实际成交的含税单价（已按整单实付比例折掉会员折扣/促销）；
     *                       null 表示按商品原价
     */
    public static InvoiceItem fromProduct(Product product, int quantity, BigDecimal taxRate,
                                          BigDecimal grossUnitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.productName = product.name;
        item.specification = product.spec != null ? product.spec : "";
        item.unit = product.unit != null ? product.unit : "个";
        item.quantity = quantity;

        BigDecimal rate = taxRate != null ? taxRate : BigDecimal.ZERO;
        BigDecimal gross = grossUnitPrice != null ? grossUnitPrice : product.price;
        item.unitPrice = gross.divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP);

        item.calculateAmount(rate);

        return item;
    }
}