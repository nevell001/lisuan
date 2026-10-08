package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import com.cashier.model.PageResult;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.api.sync.SyncEventType;
import org.slf4j.Logger;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 发票服务层
 */
public class InvoiceService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InvoiceService.class);
    private static final int FIRST_PAGE = 1;
    private static final int DEFAULT_INVOICE_LIST_LIMIT = 5000;
    
    // 默认销售方信息（可配置）
    private static volatile String defaultSellerName = "某某商贸有限公司";
    private static volatile String defaultSellerTaxId = "91110108MA01234567";
    private static volatile String defaultSellerAddress = "北京市海淀区某某路123号";
    private static volatile String defaultSellerPhone = "010-12345678";
    private static volatile String defaultSellerBank = "中国工商银行北京支行 1234567890";
    private static volatile BigDecimal defaultTaxRate = new BigDecimal("0.13");

    // 发票号码序列号，防止同毫秒生成重复号码
    private static final AtomicLong invoiceSeq = new AtomicLong(0);
    
    /**
     * 创建发票表
     */
    public static void init() {
        try {
            DAOFactory.getInstance().getInvoiceDAO().createTable();
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
        Transaction transaction = DAOFactory.getInstance().getTransactionDAO().findById(transactionId);
        if (transaction == null) {
            throw new SQLException("交易不存在: " + transactionId);
        }

        Invoice invoice = createBaseInvoice(request);
        invoice.transactionId = transactionId;
        invoice.items = createInvoiceItems(transaction, invoice.taxRate);
        invoice.calculateAmounts();
        invoice.createBy = request.createBy != null ? request.createBy : transaction.operatorUsername;

        // 检查+插入在同一事务内，消除 TOCTOU 竞态
        DatabaseManager.executeBooleanTransaction(conn -> {
            Invoice existing = DAOFactory.getInstance().getInvoiceDAO().findByTransactionIdWithConnection(conn, transactionId);
            if (existing != null && !"VOIDED".equals(existing.status)) {
                throw new SQLException("该交易已开具发票: " + existing.invoiceId);
            }
            DAOFactory.getInstance().getInvoiceDAO().insertWithConnection(conn, invoice);
            return true;
        });

        logger.info("发票创建成功: {} - 金额: {}", invoice.invoiceId, invoice.finalAmount);

        // 广播发票创建事件
        broadcastInvoiceCreated(invoice, transactionId);

        return invoice;
    }
    
    /**
     * 创建空发票（手工开票）
     */
    public static Invoice createManualInvoice(InvoiceRequest request) throws SQLException {
        Invoice invoice = createBaseInvoice(request);
        invoice.transactionId = "";  // 无关联交易
        invoice.items = request.items != null ? request.items : new ArrayList<>();
        invoice.calculateAmounts();
        invoice.createBy = request.createBy != null ? request.createBy : "";
        
        // 表头与明细必须同一事务：insert() 走的是自己的 autocommit 连接，
        // 表头先提交、明细再插，明细失败（如明细字段超长）就留下一张"有金额、无明细"的孤儿发票，
        // 用户重试还会再产生一张（invoice_id 每次重新生成）。
        DatabaseManager.executeBooleanTransaction(conn -> {
            if (!DAOFactory.getInstance().getInvoiceDAO().insertWithConnection(conn, invoice)) {
                throw new SQLException("保存发票失败: " + invoice.invoiceId);
            }
            return true;
        });
        
        logger.info("手工发票创建成功: {} - 金额: {}", invoice.invoiceId, invoice.finalAmount);
        
        return invoice;
    }

    private static Invoice createBaseInvoice(InvoiceRequest request) {
        Invoice invoice = new Invoice();
        invoice.invoiceId = Invoice.generateInvoiceId();
        invoice.invoiceCode = request.invoiceCode != null ? request.invoiceCode : "044001900111";
        invoice.invoiceNumber = generateInvoiceNumber();
        applyBuyerInfo(invoice, request);
        applySellerInfo(invoice, request);
        invoice.taxRate = resolveTaxRate(request);
        invoice.createTime = new Date();
        invoice.status = "ISSUED";
        invoice.remark = request.remark != null ? request.remark : "";
        invoice.payee = request.payee != null ? request.payee : "";
        invoice.checker = request.checker != null ? request.checker : "";
        return invoice;
    }

    /**
     * 开票税率：请求体优先，其次系统设置里的 {@code taxRate}（结账用的就是它），最后回落默认值。
     *
     * <p>此前无论请求体给不给，缺省一律写死 13%，与结账/税额口径（{@code TransactionService.calculateTax}
     * 读的是系统设置）不一致（审计 F3）。</p>
     */
    private static BigDecimal resolveTaxRate(InvoiceRequest request) {
        if (request != null && request.taxRate != null) {
            return request.taxRate;
        }
        String configured = DataService.loadSettings().get("taxRate");
        if (configured != null && !configured.isBlank()) {
            try {
                return new BigDecimal(configured.trim());
            } catch (NumberFormatException e) {
                logger.warn("税率配置无法解析，按默认 {} 处理: {}", defaultTaxRate, configured);
            }
        }
        return defaultTaxRate;
    }

    private static void applyBuyerInfo(Invoice invoice, InvoiceRequest request) {
        invoice.buyerName = request.buyerName != null ? request.buyerName : "个人";
        invoice.buyerTaxId = request.buyerTaxId != null ? request.buyerTaxId : "";
        invoice.buyerAddress = request.buyerAddress != null ? request.buyerAddress : "";
        invoice.buyerPhone = request.buyerPhone != null ? request.buyerPhone : "";
        invoice.buyerBank = request.buyerBank != null ? request.buyerBank : "";
    }

    private static void applySellerInfo(Invoice invoice, InvoiceRequest request) {
        invoice.sellerName = request.sellerName != null ? request.sellerName : defaultSellerName;
        invoice.sellerTaxId = request.sellerTaxId != null ? request.sellerTaxId : defaultSellerTaxId;
        invoice.sellerAddress = request.sellerAddress != null ? request.sellerAddress : defaultSellerAddress;
        invoice.sellerPhone = request.sellerPhone != null ? request.sellerPhone : defaultSellerPhone;
        invoice.sellerBank = request.sellerBank != null ? request.sellerBank : defaultSellerBank;
    }

    /**
     * 由交易明细生成开票明细。
     *
     * <p>金额必须与顾客实付一致：会员折扣与促销作用在**整单**上，而 {@code transaction.items}
     * 只存商品原价，因此按「实付 / 原价合计」把含税单价折下来（与退货退款同一折算口径）。
     * 此前直接用原价开票，9.5 折成交的单子会按原价多开（审计 F3）。</p>
     */
    private static List<InvoiceItem> createInvoiceItems(Transaction transaction, BigDecimal taxRate) {
        List<InvoiceItem> items = new ArrayList<>();
        if (transaction.items == null) {
            return items;
        }
        // 原价合计无效（脏数据）时不折算，与 ReturnService.refundRatio 的兜底一致
        BigDecimal grossTotal = BigDecimal.ZERO;
        for (Product product : transaction.items) {
            if (product != null && product.price != null) {
                grossTotal = grossTotal.add(product.price.multiply(BigDecimal.valueOf(product.quantity)));
            }
        }
        BigDecimal paid = transaction.finalAmount != null ? transaction.finalAmount : grossTotal;
        BigDecimal ratio = grossTotal.compareTo(BigDecimal.ZERO) <= 0
            ? BigDecimal.ONE
            : paid.divide(grossTotal, 4, java.math.RoundingMode.HALF_UP)
                .min(BigDecimal.ONE).max(BigDecimal.ZERO);
        for (Product product : transaction.items) {
            BigDecimal grossUnitPrice = product.price.multiply(ratio)
                .setScale(2, java.math.RoundingMode.HALF_UP);
            items.add(InvoiceItem.fromProduct(product, product.quantity, taxRate, grossUnitPrice));
        }
        return items;
    }

    private static void broadcastInvoiceCreated(Invoice invoice, String transactionId) {
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
    }
    
    /**
     * 作废发票
     */
    public static Invoice voidInvoice(String invoiceId, String reason) throws SQLException {
        Invoice invoice = DAOFactory.getInstance().getInvoiceDAO().findById(invoiceId);
        if (invoice == null) {
            throw new SQLException("发票不存在: " + invoiceId);
        }
        
        if ("VOIDED".equals(invoice.status)) {
            throw new SQLException("发票已作废");
        }
        
        DAOFactory.getInstance().getInvoiceDAO().voidInvoice(invoiceId, reason);
        
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
        return DAOFactory.getInstance().getInvoiceDAO().findById(invoiceId);
    }
    
    /**
     * 查询交易发票
     */
    public static Invoice getInvoiceByTransaction(String transactionId) throws SQLException {
        return DAOFactory.getInstance().getInvoiceDAO().findByTransactionId(transactionId);
    }
    
    /**
     * 查询发票列表。为兼容旧调用保留方法名，但默认只返回有界数据。
     */
    public static List<Invoice> getAllInvoices() throws SQLException {
        return getInvoicesPage(null, null, null, FIRST_PAGE, DEFAULT_INVOICE_LIST_LIMIT).getData();
    }

    /**
     * 分页查询发票。
     */
    public static PageResult<Invoice> getInvoicesPage(
            LocalDate startDate,
            LocalDate endDate,
            String status,
            int pageNum,
            int pageSize
    ) throws SQLException {
        return DAOFactory.getInstance().getInvoiceDAO().findPage(startDate, endDate, status, pageNum, pageSize);
    }
    
    /**
     * 按日期查询发票
     */
    public static List<Invoice> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        return DAOFactory.getInstance().getInvoiceDAO().findByDateRange(startDate, endDate);
    }
    
    /**
     * 生成发票号码（时间戳 + 序列号，防止同毫秒重复）
     */
    private static String generateInvoiceNumber() {
        long timestamp = System.currentTimeMillis() % 100000000;
        long seq = invoiceSeq.getAndIncrement() % 10000;
        return String.format("%08d%04d", timestamp, seq);
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
