package com.cashier.dao;

import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 发票数据访问层
 */
public class InvoiceDAO {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceDAO.class);
    
    /**
     * 创建发票表
     */
    public static void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS invoices (
                invoice_id VARCHAR(50) PRIMARY KEY,
                invoice_code VARCHAR(20),
                invoice_number VARCHAR(20),
                transaction_id VARCHAR(50),
                buyer_name VARCHAR(100),
                buyer_tax_id VARCHAR(30),
                buyer_address VARCHAR(200),
                buyer_phone VARCHAR(50),
                buyer_bank VARCHAR(100),
                seller_name VARCHAR(100),
                seller_tax_id VARCHAR(30),
                seller_address VARCHAR(200),
                seller_phone VARCHAR(50),
                seller_bank VARCHAR(100),
                total_amount DECIMAL(10,2),
                tax_amount DECIMAL(10,2),
                final_amount DECIMAL(10,2),
                tax_rate DECIMAL(5,4),
                create_time DATETIME,
                print_time DATETIME,
                create_by VARCHAR(50),
                status VARCHAR(20),
                void_reason VARCHAR(200),
                void_time DATETIME,
                remark VARCHAR(500),
                payee VARCHAR(50),
                checker VARCHAR(50),
                print_count INT DEFAULT 0,
                pdf_path VARCHAR(200),
                image_path VARCHAR(200)
            )
            """;
        
        String itemsSql = """
            CREATE TABLE IF NOT EXISTS invoice_items (
                id INT AUTO_INCREMENT PRIMARY KEY,
                invoice_id VARCHAR(50),
                product_name VARCHAR(100),
                specification VARCHAR(100),
                unit VARCHAR(20),
                quantity INT,
                unit_price DECIMAL(10,2),
                amount DECIMAL(10,2),
                tax_rate DECIMAL(5,4),
                tax_amount DECIMAL(10,2),
                total_amount DECIMAL(10,2),
                FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
            )
            """;
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(itemsSql);
            logger.info("发票表创建成功");
        }
    }
    
    /**
     * 插入发票
     */
    public static boolean insert(Invoice invoice) throws SQLException {
        String sql = """
            INSERT INTO invoices (
                invoice_id, invoice_code, invoice_number, transaction_id,
                buyer_name, buyer_tax_id, buyer_address, buyer_phone, buyer_bank,
                seller_name, seller_tax_id, seller_address, seller_phone, seller_bank,
                total_amount, tax_amount, final_amount, tax_rate,
                create_time, create_by, status, remark, payee, checker
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, invoice.invoiceId);
            pstmt.setString(2, invoice.invoiceCode);
            pstmt.setString(3, invoice.invoiceNumber);
            pstmt.setString(4, invoice.transactionId);
            pstmt.setString(5, invoice.buyerName);
            pstmt.setString(6, invoice.buyerTaxId);
            pstmt.setString(7, invoice.buyerAddress);
            pstmt.setString(8, invoice.buyerPhone);
            pstmt.setString(9, invoice.buyerBank);
            pstmt.setString(10, invoice.sellerName);
            pstmt.setString(11, invoice.sellerTaxId);
            pstmt.setString(12, invoice.sellerAddress);
            pstmt.setString(13, invoice.sellerPhone);
            pstmt.setString(14, invoice.sellerBank);
            pstmt.setBigDecimal(15, invoice.totalAmount);
            pstmt.setBigDecimal(16, invoice.taxAmount);
            pstmt.setBigDecimal(17, invoice.finalAmount);
            pstmt.setBigDecimal(18, invoice.taxRate);
            pstmt.setTimestamp(19, new Timestamp(invoice.createTime.getTime()));
            pstmt.setString(20, invoice.createBy);
            pstmt.setString(21, invoice.status);
            pstmt.setString(22, invoice.remark);
            pstmt.setString(23, invoice.payee);
            pstmt.setString(24, invoice.checker);
            
            int rows = pstmt.executeUpdate();
            
            // 插入商品明细
            if (rows > 0 && invoice.items != null) {
                insertItems(conn, invoice.invoiceId, invoice.items);
            }
            
            return rows > 0;
        }
    }
    
    /**
     * 插入发票商品明细
     */
    private static void insertItems(Connection conn, String invoiceId, List<InvoiceItem> items) throws SQLException {
        String sql = """
            INSERT INTO invoice_items (
                invoice_id, product_name, specification, unit,
                quantity, unit_price, amount, tax_rate, tax_amount, total_amount
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (InvoiceItem item : items) {
                pstmt.setString(1, invoiceId);
                pstmt.setString(2, item.productName);
                pstmt.setString(3, item.specification);
                pstmt.setString(4, item.unit);
                pstmt.setInt(5, item.quantity);
                pstmt.setBigDecimal(6, item.unitPrice);
                pstmt.setBigDecimal(7, item.amount);
                pstmt.setBigDecimal(8, item.taxRate);
                pstmt.setBigDecimal(9, item.taxAmount);
                pstmt.setBigDecimal(10, item.totalAmount);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    /**
     * 根据ID查找发票
     */
    public static Invoice findById(String invoiceId) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE invoice_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, invoiceId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.items = findItemsByInvoiceId(conn, invoiceId);
                    return invoice;
                }
            }
        }
        return null;
    }
    
    /**
     * 根据交易ID查找发票
     */
    public static Invoice findByTransactionId(String transactionId) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE transaction_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, transactionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.items = findItemsByInvoiceId(conn, invoice.invoiceId);
                    return invoice;
                }
            }
        }
        return null;
    }
    
    /**
     * 查询所有发票
     */
    public static List<Invoice> findAll() throws SQLException {
        String sql = "SELECT * FROM invoices ORDER BY create_time DESC";
        List<Invoice> invoices = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Invoice invoice = mapResultSetToInvoice(rs);
                invoice.items = findItemsByInvoiceId(conn, invoice.invoiceId);
                invoices.add(invoice);
            }
        }
        return invoices;
    }
    
    /**
     * 查询发票商品明细
     */
    private static List<InvoiceItem> findItemsByInvoiceId(Connection conn, String invoiceId) throws SQLException {
        String sql = "SELECT * FROM invoice_items WHERE invoice_id = ?";
        List<InvoiceItem> items = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, invoiceId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem();
                    item.productName = rs.getString("product_name");
                    item.specification = rs.getString("specification");
                    item.unit = rs.getString("unit");
                    item.quantity = rs.getInt("quantity");
                    item.unitPrice = rs.getBigDecimal("unit_price");
                    item.amount = rs.getBigDecimal("amount");
                    item.taxRate = rs.getBigDecimal("tax_rate");
                    item.taxAmount = rs.getBigDecimal("tax_amount");
                    item.totalAmount = rs.getBigDecimal("total_amount");
                    items.add(item);
                }
            }
        }
        return items;
    }
    
    /**
     * 更新发票状态
     */
    public static boolean updateStatus(String invoiceId, String status) throws SQLException {
        String sql = "UPDATE invoices SET status = ? WHERE invoice_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setString(2, invoiceId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 作废发票
     */
    public static boolean voidInvoice(String invoiceId, String reason) throws SQLException {
        String sql = "UPDATE invoices SET status = 'VOIDED', void_reason = ?, void_time = ? WHERE invoice_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, reason);
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(3, invoiceId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 更新打印信息
     */
    public static boolean updatePrintInfo(String invoiceId, String pdfPath, String imagePath) throws SQLException {
        String sql = "UPDATE invoices SET status = 'PRINTED', print_time = ?, print_count = print_count + 1, pdf_path = ?, image_path = ? WHERE invoice_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, pdfPath);
            pstmt.setString(3, imagePath);
            pstmt.setString(4, invoiceId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 按日期范围查询
     */
    public static List<Invoice> findByDateRange(Date startDate, Date endDate) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE create_time BETWEEN ? AND ? ORDER BY create_time DESC";
        List<Invoice> invoices = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(startDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(endDate.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.items = findItemsByInvoiceId(conn, invoice.invoiceId);
                    invoices.add(invoice);
                }
            }
        }
        return invoices;
    }
    
    /**
     * ResultSet 映射到 Invoice
     */
    private static Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.invoiceId = rs.getString("invoice_id");
        invoice.invoiceCode = rs.getString("invoice_code");
        invoice.invoiceNumber = rs.getString("invoice_number");
        invoice.transactionId = rs.getString("transaction_id");
        invoice.buyerName = rs.getString("buyer_name");
        invoice.buyerTaxId = rs.getString("buyer_tax_id");
        invoice.buyerAddress = rs.getString("buyer_address");
        invoice.buyerPhone = rs.getString("buyer_phone");
        invoice.buyerBank = rs.getString("buyer_bank");
        invoice.sellerName = rs.getString("seller_name");
        invoice.sellerTaxId = rs.getString("seller_tax_id");
        invoice.sellerAddress = rs.getString("seller_address");
        invoice.sellerPhone = rs.getString("seller_phone");
        invoice.sellerBank = rs.getString("seller_bank");
        invoice.totalAmount = rs.getBigDecimal("total_amount");
        invoice.taxAmount = rs.getBigDecimal("tax_amount");
        invoice.finalAmount = rs.getBigDecimal("final_amount");
        invoice.taxRate = rs.getBigDecimal("tax_rate");
        invoice.createTime = rs.getTimestamp("create_time");
        invoice.printTime = rs.getTimestamp("print_time");
        invoice.createBy = rs.getString("create_by");
        invoice.status = rs.getString("status");
        invoice.voidReason = rs.getString("void_reason");
        invoice.voidTime = rs.getTimestamp("void_time");
        invoice.remark = rs.getString("remark");
        invoice.payee = rs.getString("payee");
        invoice.checker = rs.getString("checker");
        invoice.printCount = rs.getInt("print_count");
        invoice.pdfPath = rs.getString("pdf_path");
        invoice.imagePath = rs.getString("image_path");
        return invoice;
    }
}