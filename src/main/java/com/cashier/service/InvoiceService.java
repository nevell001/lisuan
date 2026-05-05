package com.cashier.service;

import com.cashier.dao.InvoiceDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.api.sync.SyncBroadcastService;
import com.cashier.api.sync.SyncEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发票服务层
 */
public class InvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    
    // 默认销售方信息（可配置）
    private static String defaultSellerName = "某某商贸有限公司";
    private static String defaultSellerTaxId = "91110108MA01234567";
    private static String defaultSellerAddress = "北京市海淀区某某路123号";
    private static String defaultSellerPhone = "010-12345678";
    private static String defaultSellerBank = "中国工商银行北京支行 1234567890";
    private static BigDecimal defaultTaxRate = new BigDecimal("0.13");
    
    /**
     * 创建发票表
     */
    public static void init() {
        try {
            InvoiceDAO.createTable();
            logger.info("发票系统初始化完成");
        } catch (SQLException e) {
            logger.error("创建发票表失败", e);
        }
    }
    
    /**
     * 从交易创建发票
     */
    public static Invoice createInvoiceFromTransaction(String transactionId, InvoiceRequest request) throws SQLException {
        // 获取交易信息
        Transaction transaction = TransactionDAO.findById(transactionId);
        if (transaction == null) {
            throw new SQLException("交易不存在: " + transactionId);
        }
        
        // 检查是否已开具发票
        Invoice existing = InvoiceDAO.findByTransactionId(transactionId);
        if (existing != null && !"VOIDED".equals(existing.status)) {
            throw new SQLException("该交易已开具发票: " + existing.invoiceId);
        }
        
        // 创建发票
        Invoice invoice = new Invoice();
        invoice.invoiceId = Invoice.generateInvoiceId();
        invoice.invoiceCode = request.invoiceCode != null ? request.invoiceCode : "044001900111";
        invoice.invoiceNumber = generateInvoiceNumber();
        invoice.transactionId = transactionId;
        
        // 购买方信息
        invoice.buyerName = request.buyerName != null ? request.buyerName : "个人";
        invoice.buyerTaxId = request.buyerTaxId != null ? request.buyerTaxId : "";
        invoice.buyerAddress = request.buyerAddress != null ? request.buyerAddress : "";
        invoice.buyerPhone = request.buyerPhone != null ? request.buyerPhone : "";
        invoice.buyerBank = request.buyerBank != null ? request.buyerBank : "";
        
        // 销售方信息
        invoice.sellerName = request.sellerName != null ? request.sellerName : defaultSellerName;
        invoice.sellerTaxId = request.sellerTaxId != null ? request.sellerTaxId : defaultSellerTaxId;
        invoice.sellerAddress = request.sellerAddress != null ? request.sellerAddress : defaultSellerAddress;
        invoice.sellerPhone = request.sellerPhone != null ? request.sellerPhone : defaultSellerPhone;
        invoice.sellerBank = request.sellerBank != null ? request.sellerBank : defaultSellerBank;
        
        // 税率
        invoice.taxRate = request.taxRate != null ? request.taxRate : defaultTaxRate;
        
        // 商品明细
        invoice.items = new ArrayList<>();
        if (transaction.items != null) {
            for (Product product : transaction.items) {
                InvoiceItem item = InvoiceItem.fromProduct(product, product.quantity);
                invoice.items.add(item);
            }
        }
        
        // 计算金额
        invoice.calculateAmounts();
        
        // 时间和状态
        invoice.createTime = new Date();
        invoice.createBy = request.createBy != null ? request.createBy : transaction.operatorUsername;
        invoice.status = "ISSUED";
        invoice.remark = request.remark != null ? request.remark : "";
        invoice.payee = request.payee != null ? request.payee : "";
        invoice.checker = request.checker != null ? request.checker : "";
        
        // 保存发票
        InvoiceDAO.insert(invoice);
        
        logger.info("发票创建成功: {} - 金额: {}", invoice.invoiceId, invoice.finalAmount);
        
        // 广播发票创建事件
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("invoiceId", invoice.invoiceId);
            data.put("transactionId", transactionId);
            data.put("finalAmount", invoice.finalAmount);
            data.put("buyerName", invoice.buyerName);
            
            com.cashier.api.sync.SyncManager.getInstance()
                .broadcastSyncEvent(SyncEventType.fromName("INVOICE_CREATED"), data);
        } catch (Exception e) {
            logger.warn("广播发票事件失败", e);
        }
        
        return invoice;
    }
    
    /**
     * 创建空发票（手工开票）
     */
    public static Invoice createManualInvoice(InvoiceRequest request) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.invoiceId = Invoice.generateInvoiceId();
        invoice.invoiceCode = request.invoiceCode != null ? request.invoiceCode : "044001900111";
        invoice.invoiceNumber = generateInvoiceNumber();
        invoice.transactionId = "";  // 无关联交易
        
        // 购买方信息
        invoice.buyerName = request.buyerName != null ? request.buyerName : "个人";
        invoice.buyerTaxId = request.buyerTaxId != null ? request.buyerTaxId : "";
        invoice.buyerAddress = request.buyerAddress != null ? request.buyerAddress : "";
        invoice.buyerPhone = request.buyerPhone != null ? request.buyerPhone : "";
        invoice.buyerBank = request.buyerBank != null ? request.buyerBank : "";
        
        // 销售方信息
        invoice.sellerName = request.sellerName != null ? request.sellerName : defaultSellerName;
        invoice.sellerTaxId = request.sellerTaxId != null ? request.sellerTaxId : defaultSellerTaxId;
        invoice.sellerAddress = request.sellerAddress != null ? request.sellerAddress : defaultSellerAddress;
        invoice.sellerPhone = request.sellerPhone != null ? request.sellerPhone : defaultSellerPhone;
        invoice.sellerBank = request.sellerBank != null ? request.sellerBank : defaultSellerBank;
        
        // 税率
        invoice.taxRate = request.taxRate != null ? request.taxRate : defaultTaxRate;
        
        // 商品明细
        invoice.items = request.items != null ? request.items : new ArrayList<>();
        
        // 计算金额
        invoice.calculateAmounts();
        
        // 时间和状态
        invoice.createTime = new Date();
        invoice.createBy = request.createBy != null ? request.createBy : "";
        invoice.status = "ISSUED";
        invoice.remark = request.remark != null ? request.remark : "";
        invoice.payee = request.payee != null ? request.payee : "";
        invoice.checker = request.checker != null ? request.checker : "";
        
        // 保存发票
        InvoiceDAO.insert(invoice);
        
        logger.info("手工发票创建成功: {} - 金额: {}", invoice.invoiceId, invoice.finalAmount);
        
        return invoice;
    }
    
    /**
     * 作废发票
     */
    public static Invoice voidInvoice(String invoiceId, String reason) throws SQLException {
        Invoice invoice = InvoiceDAO.findById(invoiceId);
        if (invoice == null) {
            throw new SQLException("发票不存在: " + invoiceId);
        }
        
        if ("VOIDED".equals(invoice.status)) {
            throw new SQLException("发票已作废");
        }
        
        InvoiceDAO.voidInvoice(invoiceId, reason);
        
        invoice.status = "VOIDED";
        invoice.voidReason = reason;
        invoice.voidTime = new Date();
        
        logger.info("发票作废: {} - 原因: {}", invoiceId, reason);
        
        return invoice;
    }
    
    /**
     * 查询发票
     */
    public static Invoice getInvoice(String invoiceId) throws SQLException {
        return InvoiceDAO.findById(invoiceId);
    }
    
    /**
     * 查询交易发票
     */
    public static Invoice getInvoiceByTransaction(String transactionId) throws SQLException {
        return InvoiceDAO.findByTransactionId(transactionId);
    }
    
    /**
     * 查询所有发票
     */
    public static List<Invoice> getAllInvoices() throws SQLException {
        return InvoiceDAO.findAll();
    }
    
    /**
     * 按日期查询发票
     */
    public static List<Invoice> getInvoicesByDateRange(Date startDate, Date endDate) throws SQLException {
        return InvoiceDAO.findByDateRange(startDate, endDate);
    }
    
    /**
     * 生成发票号码
     */
    private static String generateInvoiceNumber() {
        return String.format("%08d", System.currentTimeMillis() % 100000000);
    }
    
    /**
     * 设置默认销售方信息
     */
    public static void setDefaultSellerInfo(String name, String taxId, String address, String phone, String bank) {
        defaultSellerName = name;
        defaultSellerTaxId = taxId;
        defaultSellerAddress = address;
        defaultSellerPhone = phone;
        defaultSellerBank = bank;
    }
    
    /**
     * 获取默认销售方信息
     */
    public static Map<String, String> getDefaultSellerInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("name", defaultSellerName);
        info.put("taxId", defaultSellerTaxId);
        info.put("address", defaultSellerAddress);
        info.put("phone", defaultSellerPhone);
        info.put("bank", defaultSellerBank);
        return info;
    }
    
    /**
     * 发票请求 DTO
     */
    public static class InvoiceRequest {
        public String transactionId;
        public String invoiceCode;
        public String buyerName;
        public String buyerTaxId;
        public String buyerAddress;
        public String buyerPhone;
        public String buyerBank;
        public String sellerName;
        public String sellerTaxId;
        public String sellerAddress;
        public String sellerPhone;
        public String sellerBank;
        public BigDecimal taxRate;
        public List<InvoiceItem> items;
        public String createBy;
        public String remark;
        public String payee;
        public String checker;
    }
}