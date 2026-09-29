package com.cashier.service;

import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
