package com.cashier.service;

import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发票 HTML 生成测试
 * 验证用户可控字段被 HTML 转义，防止存储型 XSS
 */
class InvoicePrintServiceTest {

    @TempDir
    Path tempDir;

    private Invoice xssInvoice() {
        Invoice invoice = new Invoice();
        invoice.invoiceId = "INV001";
        invoice.invoiceCode = "123";
        invoice.invoiceNumber = "456";
        invoice.buyerName = "<script>alert('xss')</script>";
        invoice.buyerTaxId = "TAX\"onmouseover=alert(1)";
        invoice.remark = "备注<img src=x onerror=alert(1)>";
        InvoiceItem item = new InvoiceItem();
        item.productName = "<b>商品</b>";
        item.specification = "规格&型号";
        item.unit = "件";
        item.quantity = 1;
        item.unitPrice = BigDecimal.TEN;
        item.amount = BigDecimal.TEN;
        item.taxRate = new BigDecimal("0.13");
        item.taxAmount = new BigDecimal("1.30");
        item.totalAmount = new BigDecimal("11.30");
        invoice.items = new ArrayList<>();
        invoice.items.add(item);
        return invoice;
    }

    @Test
    @DisplayName("发票 HTML 生成时会转义用户可控字段，防止 XSS")
    void generateHtmlEscapesUserControlledFields() throws IOException {
        InvoicePrintService.setOutputDir(tempDir.toString());
        String filePath = InvoicePrintService.generateHtml(xssInvoice());

        String html = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);

        // 原始注入脚本不应直接出现
        assertFalse(html.contains("<script>alert('xss')</script>"),
            "buyerName 中的脚本必须被转义");
        assertFalse(html.contains("<img src=x onerror=alert(1)>"),
            "remark 中的 IMG onerror 必须被转义");
        assertFalse(html.contains("<b>商品</b>"),
            "productName 中的标签必须被转义");

        // 转义后的实体应出现
        assertTrue(html.contains("&lt;script&gt;"),
            "脚本标签应被转义为实体");
        assertTrue(html.contains("&lt;b&gt;商品&lt;/b&gt;"),
            "商品名标签应被转义为实体");
        assertTrue(html.contains("&amp;"),
            "& 应被转义为 &amp;");
    }

    @Test
    @DisplayName("非整数百分比税率（6.5%）也能打印，不再因 setScale 抛异常")
    void generateHtmlHandlesFractionalTaxRate() throws IOException {
        InvoicePrintService.setOutputDir(tempDir.toString());
        Invoice invoice = xssInvoice();
        BigDecimal fractional = new BigDecimal("0.065");
        invoice.taxRate = fractional;
        invoice.items.get(0).taxRate = fractional;

        String html = Files.readString(Path.of(InvoicePrintService.generateHtml(invoice)), StandardCharsets.UTF_8);

        // 修复前 rate.multiply(100).setScale(0) 会抛 ArithmeticException: Rounding necessary，
        // 打印接口直接 500（TD-034）
        assertTrue(html.contains("7%"), "6.5% 应四舍五入显示为 7%，且不得抛异常");
    }

    @Test
    @DisplayName("空值字段不会导致生成失败")
    void generateHtmlHandlesNullFields() throws IOException {
        InvoicePrintService.setOutputDir(tempDir.toString());
        Invoice invoice = xssInvoice();
        invoice.buyerAddress = null;
        invoice.buyerPhone = null;
        invoice.buyerBank = null;
        invoice.payee = null;
        invoice.checker = null;
        invoice.createBy = null;
        invoice.remark = null;

        String filePath = InvoicePrintService.generateHtml(invoice);
        assertTrue(Files.exists(Path.of(filePath)));
    }
}