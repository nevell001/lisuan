package com.cashier.api.middleware;

import com.cashier.api.support.TestContext;
import com.cashier.model.User;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationMiddlewareTest {

    @Test
    @DisplayName("管理员可以访问所有 API")
    void adminCanAccessAllApis() {
        assertTrue(AuthorizationMiddleware.isAllowed("admin", "POST", "/api/backup/restore"));
        assertTrue(AuthorizationMiddleware.isAllowed("admin", "PUT", "/api/settings/theme"));
    }

    @Test
    @DisplayName("收银员不能操作系统、备份和资金审核接口")
    void cashierCannotAccessPrivilegedApis() {
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "PUT", "/api/settings/theme"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/backup/execute"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "PUT", "/api/payment/config"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/transactions/T1/refund"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/payment/P1/refund"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/printers/P1/cashdrawer"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "DELETE", "/api/products/10"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "PUT", "/api/inventory/10"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/invoices/manual"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/members/10/recharge"));
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "PUT", "/api/members/10"));
        // 开票方信息会写进之后所有发票，属全局配置，收银员不得修改（读取仍允许）
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "PUT", "/api/invoices/seller-info"));
        assertTrue(AuthorizationMiddleware.isAllowed("cashier", "GET", "/api/invoices/seller-info"));
        // 资金统计属财务口径，收银员不可查看
        assertFalse(AuthorizationMiddleware.isAllowed("cashier", "GET", "/api/payment/stats/daily"));
    }

    @Test
    @DisplayName("财务可以审核资金操作但不能修改系统配置")
    void financeHasFinancialPermissionsOnly() {
        assertTrue(AuthorizationMiddleware.isAllowed("finance", "POST", "/api/transactions/T1/refund"));
        assertTrue(AuthorizationMiddleware.isAllowed("finance", "POST", "/api/payment/P1/refund"));
        assertTrue(AuthorizationMiddleware.isAllowed("finance", "POST", "/api/invoices/manual"));
        assertTrue(AuthorizationMiddleware.isAllowed("finance", "GET", "/api/reports/daily"));
        assertTrue(AuthorizationMiddleware.isAllowed("finance", "POST", "/api/members/10/recharge"));
        assertTrue(AuthorizationMiddleware.isAllowed("finance", "PUT", "/api/members/10"));
        assertFalse(AuthorizationMiddleware.isAllowed("finance", "PUT", "/api/settings/theme"));
        assertFalse(AuthorizationMiddleware.isAllowed("finance", "POST", "/api/backup/execute"));
        assertFalse(AuthorizationMiddleware.isAllowed("finance", "PUT", "/api/invoices/seller-info"));
    }

    @Test
    @DisplayName("收银员保留日常收银和打印小票能力")
    void cashierRetainsPosPermissions() {
        assertTrue(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/transactions"));
        assertTrue(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/payment/create"));
        assertTrue(AuthorizationMiddleware.isAllowed("cashier", "POST", "/api/printers/P1/receipt"));
        assertTrue(AuthorizationMiddleware.isAllowed("cashier", "GET", "/api/products"));
        assertTrue(AuthorizationMiddleware.isAllowed("cashier", "GET", "/api/inventory"));
    }

    @Test
    @DisplayName("中间件拒绝越权请求并停止后续处理")
    void middlewareRejectsUnauthorizedRequest() {
        User cashier = new User();
        cashier.role = "cashier";
        TestContext ctx = new TestContext()
            .withAttribute("currentUser", cashier)
            .withRequest(HandlerType.DELETE, "/api/products/10");

        AuthorizationMiddleware.authorize(ctx.context);

        assertEquals(HttpStatus.FORBIDDEN, ctx.status);
        assertTrue(ctx.skipped);
    }
}
