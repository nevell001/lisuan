package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发票服务金额计算测试。
 */
@DisplayName("发票服务测试")
class InvoiceServiceTest extends DatabaseTestBase {

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    private InvoiceService.InvoiceRequest manualRequest(List<InvoiceItem> items, BigDecimal taxRate) {
        InvoiceService.InvoiceRequest request = new InvoiceService.InvoiceRequest();
        request.items = items;
        request.taxRate = taxRate;
        request.buyerName = "测试公司";
        request.createBy = "admin";
        return request;
    }

    private InvoiceItem item(String name, BigDecimal unitPrice, int quantity) {
        InvoiceItem item = new InvoiceItem();
        item.productName = name;
        item.unit = "个";
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        return item;
    }

    @Test
    @DisplayName("手工发票按税率正确计算金额与税额")
    void manualInvoiceCalculatesAmounts() throws SQLException {
        List<InvoiceItem> items = List.of(
            item("商品A", BigDecimal.valueOf(100.00), 2),   // 金额 200
            item("商品B", BigDecimal.valueOf(50.00), 1));   // 金额 50

        Invoice invoice = InvoiceService.createManualInvoice(manualRequest(items, new BigDecimal("0.13")));

        assertNotNull(invoice.invoiceId);
        assertEquals(0, BigDecimal.valueOf(250.00).compareTo(invoice.totalAmount));
        assertEquals(0, BigDecimal.valueOf(32.50).compareTo(invoice.taxAmount));
        assertEquals(0, BigDecimal.valueOf(282.50).compareTo(invoice.finalAmount));
    }

    @Test
    @DisplayName("多行明细的行税额与表头税额一致（逐行取整到分，不再差 1 分）")
    void lineTaxesSumToHeaderTax() throws SQLException {
        // 3 × 33.33 @13%：每行税额 4.3329 → 落库 4.33，Σ=12.99。
        // 表头若用未取整值累加会得到 13.00，与明细对不上（TD-034）
        List<InvoiceItem> items = List.of(
            item("商品A", new BigDecimal("33.33"), 1),
            item("商品B", new BigDecimal("33.33"), 1),
            item("商品C", new BigDecimal("33.33"), 1));

        Invoice invoice = InvoiceService.createManualInvoice(manualRequest(items, new BigDecimal("0.13")));

        BigDecimal lineTax = invoice.items.stream()
            .map(i -> i.taxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lineTotal = invoice.items.stream()
            .map(i -> i.totalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, invoice.taxAmount.compareTo(lineTax),
            "表头税额 " + invoice.taxAmount + " 必须等于明细行税额之和 " + lineTax);
        assertEquals(0, invoice.finalAmount.compareTo(lineTotal),
            "价税合计 " + invoice.finalAmount + " 必须等于明细行合计之和 " + lineTotal);
        assertEquals(0, new BigDecimal("12.99").compareTo(invoice.taxAmount),
            "3 × 33.33 @13% 的行税额合计应为 12.99");
    }

    @Test
    @DisplayName("空明细发票金额为零")
    void emptyItemsInvoiceIsZero() throws SQLException {
        Invoice invoice = InvoiceService.createManualInvoice(manualRequest(List.of(), new BigDecimal("0.13")));

        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.totalAmount));
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.taxAmount));
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.finalAmount));
    }

    /** 造一张 2 × 113.00、原价合计 226.00、9.5 折实付 214.70 的交易。 */
    private String insertDiscountedTransaction(String transactionId) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.transactionId = transactionId;
        transaction.timestamp = "2026-10-08 12:00:00";
        transaction.totalAmount = new BigDecimal("226.00");
        transaction.finalAmount = new BigDecimal("214.70");
        transaction.tax = BigDecimal.ZERO;
        transaction.paymentMethod = "现金";
        transaction.operatorName = "测试收银员";
        transaction.operatorUsername = "tester";
        transaction.items = new ArrayList<>();

        Product line = new Product();
        line.name = "开票商品";
        line.price = new BigDecimal("113.00");
        line.quantity = 2;          // 交易明细里的 quantity 是成交数量
        line.unit = "个";
        transaction.items.add(line);

        assertTrue(DAOFactory.getInstance().getTransactionDAO().insert(transaction));
        return transactionId;
    }

    @Test
    @DisplayName("从交易开票：按实付比例折算，价税合计等于顾客实付（不是原价 226.00）")
    void invoiceFromTransactionReflectsPaidAmount() throws Exception {
        // 结账用的税率来自系统设置（TransactionService.calculateTax 读的就是它）
        DataService.saveSettings(java.util.Map.of("taxRate", "0.13"));
        String transactionId = insertDiscountedTransaction("T-INV-F3-A");

        InvoiceService.InvoiceRequest request = new InvoiceService.InvoiceRequest();
        request.buyerName = "测试公司";
        request.createBy = "admin";

        Invoice invoice = InvoiceService.createInvoiceFromTransaction(transactionId, request, "admin");

        // 2 × 113.00 打 9.5 折 = 214.70；按 13% 拆分 → 不含税 190.00 + 税 24.70
        assertEquals(0, new BigDecimal("190.00").compareTo(invoice.totalAmount),
            "不含税金额应为 190.00（用原价开票会得到 200.00）");
        assertEquals(0, new BigDecimal("24.70").compareTo(invoice.taxAmount));
        assertEquals(0, new BigDecimal("214.70").compareTo(invoice.finalAmount),
            "价税合计必须等于顾客实付 214.70，而不是原价 226.00");
    }

    @Test
    @DisplayName("从交易开票不接受客户端自报开票方/税率/开票人（TD-038）")
    void invoiceFromTransactionIgnoresClientSuppliedSellerAndTaxRate() throws Exception {
        DataService.saveSettings(java.util.Map.of("taxRate", "0.06"));
        InvoiceService.setDefaultSellerInfo("真销方有限公司", "91310000REAL", "真地址", "真电话", "真银行");
        String transactionId = insertDiscountedTransaction("T-INV-TD038");

        InvoiceService.InvoiceRequest request = new InvoiceService.InvoiceRequest();
        request.buyerName = "买家";
        // 冒用管理员配置：伪造销方主体、用任意税率决定税额、把开票人写成别人
        request.sellerName = "假销方有限公司";
        request.sellerTaxId = "91310000FAKE";
        request.taxRate = new BigDecimal("0.99");
        request.createBy = "admin";

        Invoice invoice = InvoiceService.createInvoiceFromTransaction(transactionId, request, "cashier01");

        assertEquals("真销方有限公司", invoice.sellerName, "开票方必须取管理员配置，不能被请求体覆盖");
        assertEquals("91310000REAL", invoice.sellerTaxId);
        assertEquals(0, new BigDecimal("0.06").compareTo(invoice.taxRate),
            "税率必须取系统设置，不能被请求体的 0.99 覆盖（那会直接改掉税额与价税合计）");
        assertEquals("cashier01", invoice.createBy, "开票人必须是认证用户，不能自报");
        assertEquals("买家", invoice.buyerName, "买家信息是顾客提供的，仍按请求体写入");
    }

    @Test
    @DisplayName("从交易开票：默认税率取系统设置（不是硬编码 13%），净单价与税额用同一个税率")
    void invoiceFromTransactionUsesConfiguredRateForNetPrice() throws Exception {
        DataService.saveSettings(java.util.Map.of("taxRate", "0.06"));
        String transactionId = insertDiscountedTransaction("T-INV-F3-B");

        InvoiceService.InvoiceRequest request = new InvoiceService.InvoiceRequest();
        request.buyerName = "测试公司";
        request.createBy = "admin";

        Invoice invoice = InvoiceService.createInvoiceFromTransaction(transactionId, request, "admin");

        assertEquals(0, new BigDecimal("0.06").compareTo(invoice.taxRate),
            "未指定税率时必须取系统设置里的 taxRate，而不是写死的 0.13");
        // 含税单价 107.35（= 113.00 × 0.95）按 6% 拆：净单价 101.27
        BigDecimal expectedNet = new BigDecimal("101.27");
        assertEquals(0, expectedNet.compareTo(invoice.items.get(0).unitPrice),
            "净单价必须按本次税率换算；按硬编码 13% 拆会得到 100.00");
        // 净单价 × (1+税率) 必须还原回含税单价（±1 分）
        BigDecimal backToGross = invoice.items.get(0).unitPrice.multiply(new BigDecimal("1.06"))
            .setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, new BigDecimal("107.35").compareTo(backToGross));
        // 价税合计落在实付附近（逐行取整允许 1~2 分漂移），绝不能是旧口径的 212.00
        BigDecimal diff = invoice.finalAmount.subtract(new BigDecimal("214.70")).abs();
        assertTrue(diff.compareTo(new BigDecimal("0.02")) <= 0,
            "价税合计 " + invoice.finalAmount + " 与实付 214.70 的偏差应 ≤ 0.02");
    }
}
