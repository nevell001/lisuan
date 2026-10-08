package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.model.Invoice;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceApiControllerTest extends DatabaseTestBase {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("空请求体从交易创建发票返回 400")
    void createFromTransactionWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/invoices/from-transaction");
        InvoiceApiController.createFromTransaction(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体手工创建发票返回 400")
    void createManualWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/invoices/manual");
        InvoiceApiController.createManual(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体作废发票返回 400")
    void voidInvoiceWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/invoices/INV-1/void")
            .withPathParam("id", "INV-1");
        InvoiceApiController.voidInvoice(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体记录打印返回 400")
    void recordPrintWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/invoices/INV-2/print")
            .withPathParam("id", "INV-2");
        InvoiceApiController.recordPrint(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("发票打印记录不采纳客户端自报的文件路径（TD-039）")
    void recordPrintIgnoresClientSuppliedPaths() throws Exception {
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        DAOFactory.getInstance().getInvoiceDAO().createTable();
        Invoice invoice = new Invoice();
        invoice.invoiceId = "INV-PATH-1";
        invoice.invoiceCode = "044001900111";
        invoice.invoiceNumber = "00000001";
        invoice.taxRate = new BigDecimal("0.13");
        invoice.createTime = new java.util.Date();
        invoice.status = "ISSUED";
        DAOFactory.getInstance().getInvoiceDAO().insert(invoice);

        InvoiceApiController.PrintRequest forged = new InvoiceApiController.PrintRequest();
        forged.pdfPath = "/tmp/forged.pdf";
        forged.imagePath = "/tmp/forged.png";
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/invoices/INV-PATH-1/print")
            .withPathParam("id", "INV-PATH-1")
            .withBody(forged);
        InvoiceApiController.recordPrint(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        Invoice stored = DAOFactory.getInstance().getInvoiceDAO().findById("INV-PATH-1");
        assertEquals("PRINTED", stored.status, "打印记录本身仍要更新");
        assertEquals(null, stored.pdfPath, "客户端提供的文件路径不得落库");
        assertEquals(null, stored.imagePath);
    }

    @Test
    @DisplayName("设置销售方信息成功")
    void setSellerInfoSucceeds() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/invoices/seller-info")
            .withBody(Map.of("name", "测试公司", "taxId", "91330100"));
        InvoiceApiController.setSellerInfo(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("空请求体设置销售方信息返回 400")
    void setSellerInfoWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/invoices/seller-info");
        InvoiceApiController.setSellerInfo(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }
}
