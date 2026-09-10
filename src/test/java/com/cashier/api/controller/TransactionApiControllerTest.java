package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.TransactionDAORefactored;
import com.cashier.model.*;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionApiControllerTest extends DatabaseTestBase {

    private final TransactionDAORefactored transactionDAO = DAOFactory.getInstance().getTransactionDAO();

    private Transaction insertTransaction(String id) throws Exception {
        Transaction transaction = new Transaction(
            id, "2026-08-06 12:00:00", List.of(),
            BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        transaction.paymentMethod = "现金";
        transaction.operatorUsername = "op";
        transaction.operatorName = "操作员";
        assertTrue(transactionDAO.insert(transaction));
        return transaction;
    }

    @Test
    @DisplayName("下单金额由服务端重算：客户端伪造的单价/应付被忽略，库存扣减、积分累计")
    void createRecomputesAmountsAndAppliesInventoryAndPoints() throws Exception {
        Product product = insertProduct("API下单商品", "APICREATE001", new BigDecimal("10.00"), 50);
        Member member = insertMember("13900000001");

        User operator = new User();
        operator.username = "cashier01";
        operator.name = "真实收银员";

        TestContext ctx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions")
            .withAttribute("currentUser", operator)
            .withBody(createRequest(product.id, 2, "现金", member.phone));

        TransactionApiController.create(ctx.context);

        assertEquals(HttpStatus.CREATED, ctx.status);
        String transactionId = (String) response(ctx).get("transactionId");
        assertNotNull(transactionId);

        // 2 × 10.00 = 20.00；若信任请求体里的 finalAmount 会得到 0.01
        Transaction saved = transactionDAO.findById(transactionId);
        assertAmountEquals(new BigDecimal("20.00"), saved.finalAmount);
        // 操作员取认证用户，而不是请求体里的伪造身份
        assertEquals("cashier01", saved.operatorUsername);
        assertEquals("真实收银员", saved.operatorName);
        // 库存扣减
        assertEquals(48, DAOFactory.getInstance().getProductDAO().findById(product.id).quantity);
        // 积分按每元 10 分：20 × 10 = 200
        assertAmountEquals(new BigDecimal("200"), DAOFactory.getInstance().getMemberDAO().findById(member.id).points);
    }

    @Test
    @DisplayName("明细缺少数量或商品不存在时拒绝下单")
    void createRejectsInvalidItems() throws Exception {
        Product product = insertProduct("API下单校验商品", "APICREATE002", new BigDecimal("10.00"), 5);

        TestContext noQuantity = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions")
            .withBody(createRequest(product.id, 0, "现金", null));
        TransactionApiController.create(noQuantity.context);
        assertEquals(HttpStatus.BAD_REQUEST, noQuantity.status);

        TestContext missingProduct = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions")
            .withBody(createRequest(999999, 1, "现金", null));
        TransactionApiController.create(missingProduct.context);
        assertEquals(HttpStatus.BAD_REQUEST, missingProduct.status);

        // 校验失败不得扣减库存
        assertEquals(5, DAOFactory.getInstance().getProductDAO().findById(product.id).quantity);
    }

    @Test
    @DisplayName("API 退款退回会员余额、冲减积分、还原库存，且不能重复退款")
    void refundCreditsBalanceReversesPointsAndRestoresStock() throws Exception {
        Product product = insertProduct("API退款商品", "APIREFUND001", new BigDecimal("10.00"), 50);
        Member member = insertMember("13900000002");

        User operator = new User();
        operator.username = "cashier02";
        operator.name = "退款收银员";

        TestContext saleCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions")
            .withAttribute("currentUser", operator)
            .withBody(createRequest(product.id, 2, "现金", member.phone));
        TransactionApiController.create(saleCtx.context);
        assertEquals(HttpStatus.CREATED, saleCtx.status);
        String transactionId = (String) response(saleCtx).get("transactionId");

        assertAmountEquals(new BigDecimal("1000.00"), DAOFactory.getInstance().getMemberDAO().findById(member.id).balance);
        assertAmountEquals(new BigDecimal("200"), DAOFactory.getInstance().getMemberDAO().findById(member.id).points);

        TestContext refundCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions/" + transactionId + "/refund")
            .withPathParam("id", transactionId);
        TransactionApiController.refund(refundCtx.context);

        assertEquals(HttpStatus.OK, refundCtx.status);
        Member afterRefund = DAOFactory.getInstance().getMemberDAO().findById(member.id);
        assertAmountEquals(new BigDecimal("1020.00"), afterRefund.balance);
        assertAmountEquals(BigDecimal.ZERO, afterRefund.points);
        assertEquals(50, DAOFactory.getInstance().getProductDAO().findById(product.id).quantity);
        assertEquals("REFUNDED", transactionDAO.findById(transactionId).status);

        // 二次退款必须被拒
        TestContext secondRefund = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions/" + transactionId + "/refund")
            .withPathParam("id", transactionId);
        TransactionApiController.refund(secondRefund.context);
        assertEquals(HttpStatus.BAD_REQUEST, secondRefund.status);
        assertAmountEquals(new BigDecimal("1020.00"), DAOFactory.getInstance().getMemberDAO().findById(member.id).balance);
    }

    private static TransactionApiController.TransactionRequest createRequest(
            int productId, int quantity, String paymentMethod, String memberPhone) {
        Product item = new Product();
        item.id = productId;
        item.quantity = quantity;
        // 客户端伪造的单价与应付金额，服务端必须忽略
        item.price = new BigDecimal("0.01");

        TransactionApiController.TransactionRequest request = new TransactionApiController.TransactionRequest();
        request.items = List.of(item);
        request.totalAmount = new BigDecimal("0.01");
        request.finalAmount = new BigDecimal("0.01");
        request.paymentMethod = paymentMethod;
        request.memberPhone = memberPhone;
        request.operatorUsername = "spoofed";
        request.operatorName = "伪造操作员";
        return request;
    }

    private Product insertProduct(String name, String code, BigDecimal price, int quantity) throws Exception {
        Product product = new Product();
        product.productCode = code;
        product.name = name;
        product.price = price;
        product.quantity = quantity;
        product.category = "测试分类";
        product.unit = "个";
        product.minStock = 0;
        product.cost = BigDecimal.ONE;
        assertTrue(DAOFactory.getInstance().getProductDAO().insert(product));
        return DAOFactory.getInstance().getProductDAO().findByName(name);
    }

    private Member insertMember(String phone) throws Exception {
        Member member = new Member();
        member.phone = phone;
        member.name = "API测试会员";
        member.balance = new BigDecimal("1000.00");
        member.points = BigDecimal.ZERO;
        member.level = "普通";
        member.discount = BigDecimal.TEN;
        member.discountRate = BigDecimal.TEN;
        assertTrue(DAOFactory.getInstance().getMemberDAO().insert(member));
        return DAOFactory.getInstance().getMemberDAO().findByPhone(phone);
    }

    private void assertAmountEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("交易列表返回最近记录")
    void listReturnsTransactions() throws Exception {
        insertTransaction("T-LIST-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions");
        TransactionApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Integer) response(ctx).get("total") >= 1);
    }

    @Test
    @DisplayName("按日期范围筛选交易")
    void listByDateRange() throws Exception {
        insertTransaction("T-DATE-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions")
            .withQueryParam("startDate", "2026-08-06")
            .withQueryParam("endDate", "2026-08-06");
        TransactionApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Integer) response(ctx).get("total") >= 1);
    }

    @Test
    @DisplayName("按支付方式筛选交易")
    void listByPaymentMethod() throws Exception {
        insertTransaction("T-PAY-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions")
            .withQueryParam("paymentMethod", "现金");
        TransactionApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Integer) response(ctx).get("total") >= 1);
    }

    @Test
    @DisplayName("获取交易详情")
    void getExistingTransaction() throws Exception {
        Transaction saved = insertTransaction("T-GET-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions/T-GET-001")
            .withPathParam("id", saved.transactionId);
        TransactionApiController.get(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("不存在的交易返回 404")
    void getMissingTransactionReturns404() {
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions/NOPE")
            .withPathParam("id", "NOPE");
        TransactionApiController.get(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("今日统计返回成功")
    void todayStatsReturnsSuccess() throws Exception {
        insertTransaction("T-STAT-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions/today-stats");
        TransactionApiController.todayStats(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }
}
