package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.service.PaymentService;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentApiControllerTest extends DatabaseTestBase {

    @AfterEach
    void restoreDisabledConfig() {
        PaymentService.setConfig(new PaymentService.PaymentConfig());
    }

    /** 建一笔真实交易：金额必须与交易实付一致才能生成收款码（新契约） */
    private void insertTransaction(String transactionId, String amount) throws Exception {
        com.cashier.model.Transaction transaction = new com.cashier.model.Transaction(
            transactionId, "2026-08-06 12:00:00", java.util.List.of(),
            new java.math.BigDecimal(amount), java.math.BigDecimal.ZERO, new java.math.BigDecimal(amount));
        transaction.paymentMethod = "现金";
        transaction.operatorUsername = "op";
        transaction.operatorName = "操作员";
        assertTrue(DAOFactory.getInstance().getTransactionDAO().insert(transaction));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("缺少必填参数返回 400")
    void createPaymentMissingParamsReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/payment/create")
            .withBody(Map.of("channel", "WECHAT"));

        PaymentApiController.createPayment(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体创建支付返回 400")
    void createPaymentWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/payment/create");

        PaymentApiController.createPayment(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("支付默认禁用时创建订单返回 500 且不泄露内部细节")
    void createPaymentWhenDisabledFails() throws Exception {
        insertTransaction("T-1", "100.00");

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/payment/create")
            .withBody(Map.of("transactionId", "T-1", "amount", 100.0, "channel", "WECHAT", "terminalId", "POS-1"));

        PaymentApiController.createPayment(ctx.context);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ctx.status);
        assertTrue(((String) response(ctx).get("error")).contains("创建失败"));
    }

    @Test
    @DisplayName("显式开启 mock 模式后创建订单成功，且操作员取认证用户")
    void createPaymentInMockModeSucceeds() throws Exception {
        try {
            DAOFactory.getInstance().getPaymentDAO().createTable();
        } catch (Exception e) {
            throw new IllegalStateException("初始化支付表失败", e);
        }
        PaymentService.PaymentConfig config = new PaymentService.PaymentConfig();
        config.mode = "mock";
        config.mockEnabled = true;
        config.mockCallbackSecret = "test-secret";
        config.wechatEnabled = true;
        PaymentService.setConfig(config);
        insertTransaction("T-2", "88.00");

        com.cashier.model.User cashier = new com.cashier.model.User();
        cashier.username = "cashier01";

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/payment/create")
            .withAttribute("currentUser", cashier)
            .withBody(Map.of("transactionId", "T-2", "amount", 88.0, "channel", "WECHAT",
                "terminalId", "POS-1", "operator", "别人"));

        PaymentApiController.createPayment(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        // 请求体里自报的 operator 必须被忽略，落库的是认证用户
        String paymentId = (String) ((Map<?, ?>) response(ctx).get("data")).get("paymentId");
        assertEquals("cashier01", DAOFactory.getInstance().getPaymentDAO().findById(paymentId).operator,
            "payment_orders.operator 必须落库为认证用户，且不能被请求体伪造");
    }

    @Test
    @DisplayName("交易不存在或金额与实付不一致时拒绝创建收款码")
    void createPaymentValidatesTransactionAndAmount() throws Exception {
        PaymentService.PaymentConfig config = new PaymentService.PaymentConfig();
        config.mode = "mock";
        config.mockEnabled = true;
        config.mockCallbackSecret = "test-secret";
        config.wechatEnabled = true;
        PaymentService.setConfig(config);
        insertTransaction("T-3", "1000.00");

        TestContext missing = new TestContext().withRequest(HandlerType.POST, "/api/payment/create")
            .withBody(Map.of("transactionId", "NO-SUCH-TX", "amount", 1000.0, "channel", "WECHAT"));
        PaymentApiController.createPayment(missing.context);
        assertEquals(HttpStatus.NOT_FOUND, missing.status, "交易不存在必须回 404");

        // 关键回归：0.01 元不能为 1000 元的交易生成收款码
        TestContext underpaid = new TestContext().withRequest(HandlerType.POST, "/api/payment/create")
            .withBody(Map.of("transactionId", "T-3", "amount", 0.01, "channel", "WECHAT"));
        PaymentApiController.createPayment(underpaid.context);
        assertEquals(HttpStatus.BAD_REQUEST, underpaid.status, "金额与交易实付不一致必须回 400");
    }
}
