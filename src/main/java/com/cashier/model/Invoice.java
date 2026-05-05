package com.cashier.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 电子发票模型
 */
public class Invoice {
    // 基本信息
    public String invoiceId;          // 发票编号（唯一）
    public String invoiceCode;        // 发票代码
    public String invoiceNumber;      // 发票号码
    public String transactionId;      // 关联交易ID
    
    // 开票信息
    public String buyerName;          // 购买方名称
    public String buyerTaxId;         // 购买方税号
    public String buyerAddress;       // 购买方地址
    public String buyerPhone;         // 购买方电话
    public String buyerBank;          // 购买方银行账号
    
    public String sellerName;         // 销售方名称
    public String sellerTaxId;        // 销售方税号
    public String sellerAddress;      // 销售方地址
    public String sellerPhone;        // 销售方电话
    public String sellerBank;         // 销售方银行账号
    
    // 商品明细
    public List<InvoiceItem> items;   // 商品明细列表
    
    // 金额信息
    public BigDecimal totalAmount;    // 合计金额（不含税）
    public BigDecimal taxAmount;      // 税额
    public BigDecimal finalAmount;    // 价税合计
    public BigDecimal taxRate;        // 率（默认13%）
    
    // 时间信息
    public Date createTime;           // 开票时间
    public Date printTime;            // 打印时间
    public String createBy;           // 开票人
    
    // 状态信息
    public String status;             // 状态：DRAFT(草稿)、ISSUED(已开具)、PRINTED(已打印)、VOIDED(已作废)
    public String voidReason;         // 作废原因
    public Date voidTime;             // 作废时间
    
    // 附加信息
    public String remark;             // 备注
    public String payee;              // 收款人
    public String checker;            // 复核人
    public int printCount;            // 打印次数
    
    // PDF/图片路径
    public String pdfPath;            // PDF文件路径
    public String imagePath;          // 图片文件路径
    
    public Invoice() {
        this.invoiceId = "";
        this.invoiceCode = "";
        this.invoiceNumber = "";
        this.transactionId = "";
        this.buyerName = "";
        this.buyerTaxId = "";
        this.buyerAddress = "";
        this.buyerPhone = "";
        this.buyerBank = "";
        this.sellerName = "";
        this.sellerTaxId = "";
        this.sellerAddress = "";
        this.sellerPhone = "";
        this.sellerBank = "";
        this.items = null;
        this.totalAmount = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.finalAmount = BigDecimal.ZERO;
        this.taxRate = new BigDecimal("0.13");
        this.createTime = new Date();
        this.printTime = null;
        this.createBy = "";
        this.status = "DRAFT";
        this.voidReason = "";
        this.voidTime = null;
        this.remark = "";
        this.payee = "";
        this.checker = "";
        this.printCount = 0;
        this.pdfPath = "";
        this.imagePath = "";
    }
    
    /**
     * 计算税额和总价
     */
    public void calculateAmounts() {
        if (items == null || items.isEmpty()) {
            this.totalAmount = BigDecimal.ZERO;
            this.taxAmount = BigDecimal.ZERO;
            this.finalAmount = BigDecimal.ZERO;
            return;
        }
        
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        
        for (InvoiceItem item : items) {
            item.calculateAmount(this.taxRate);
            total = total.add(item.amount);
            tax = tax.add(item.taxAmount);
        }
        
        this.totalAmount = total;
        this.taxAmount = tax;
        this.finalAmount = total.add(tax);
    }
    
    /**
     * 生成发票编号
     */
    public static String generateInvoiceId() {
        return "INV" + System.currentTimeMillis();
    }
}