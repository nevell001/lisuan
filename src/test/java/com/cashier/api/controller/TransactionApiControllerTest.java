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
import java.sql.Connection;
import java.time.Instant;
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
    @DisplayName("现金单 API 退款不退会员余额、冲减积分、还原库存，且不能重复退款")
    void cashRefundDoesNotCreditMemberBalance() throws Exception {
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
        // 现金退款退的是现金，不能再给会员加一笔可消费余额（否则等于退两次）
        assertAmountEquals(new BigDecimal("1000.00"), afterRefund.balance);
        assertAmountEquals(BigDecimal.ZERO, afterRefund.points);
        assertEquals(50, DAOFactory.getInstance().getProductDAO().findById(product.id).quantity);
        assertEquals("REFUNDED", transactionDAO.findById(transactionId).status);

        // 二次退款必须被拒
        TestContext secondRefund = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions/" + transactionId + "/refund")
            .withPathParam("id", transactionId);
        TransactionApiController.refund(secondRefund.context);
        assertEquals(HttpStatus.BAD_REQUEST, secondRefund.status);
        assertAmountEquals(new BigDecimal("1000.00"), DAOFactory.getInstance().getMemberDAO().findById(member.id).balance);
    }

    @Test
    @DisplayName("非现金单 API 退款退回会员余额")
    void nonCashRefundCreditsMemberBalance() throws Exception {
        Product product = insertProduct("API微信退款商品", "APIREFUND002", new BigDecimal("10.00"), 50);
        Member member = insertMember("13900000003");

        TestContext saleCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions")
            .withBody(createRequest(product.id, 2, "微信", member.phone));
        TransactionApiController.create(saleCtx.context);
        assertEquals(HttpStatus.CREATED, saleCtx.status);
        String transactionId = (String) response(saleCtx).get("transactionId");

        TestContext refundCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions/" + transactionId + "/refund")
            .withPathParam("id", transactionId);
        TransactionApiController.refund(refundCtx.context);

        assertEquals(HttpStatus.OK, refundCtx.status);
        // 非现金单退款应退回会员余额（1000 + 20）
        assertAmountEquals(new BigDecimal("1020.00"),
            DAOFactory.getInstance().getMemberDAO().findById(member.id).balance);
    }

    @Test
    @DisplayName("会员余额支付扣减余额，退款退回余额（不误判为现金）")
    void memberBalancePaymentDebitsAndRefundsBalance() throws Exception {
        Product product = insertProduct("API余额支付商品", "APIBALANCE001", new BigDecimal("10.00"), 50);
        Member member = insertMember("13900000005");

        // 用英文文案支付：此前只有中文文案才会扣余额，导致收款却不扣款
        TestContext saleCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions")
            .withBody(createRequest(product.id, 2, "Member Balance", member.phone));
        TransactionApiController.create(saleCtx.context);
        assertEquals(HttpStatus.CREATED, saleCtx.status);
        String transactionId = (String) response(saleCtx).get("transactionId");
        assertAmountEquals(new BigDecimal("980.00"),
            DAOFactory.getInstance().getMemberDAO().findById(member.id).balance);

        TestContext refundCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions/" + transactionId + "/refund")
            .withPathParam("id", transactionId);
        TransactionApiController.refund(refundCtx.context);

        assertEquals(HttpStatus.OK, refundCtx.status);
        // 余额支付退款必须退回余额，而不是当作现金退款
        assertAmountEquals(new BigDecimal("1000.00"),
            DAOFactory.getInstance().getMemberDAO().findById(member.id).balance);
    }

    @Test
    @DisplayName("读取快照后被并发退款：原子抢占失败返回 409 而不是 500")
    void refundConflictReturns409() throws Exception {
        Product product = insertProduct("API并发退款商品", "APIREFUND409", new BigDecimal("10.00"), 50);

        TestContext saleCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions")
            .withBody(createRequest(product.id, 2, "现金", null));
        TransactionApiController.create(saleCtx.context);
        assertEquals(HttpStatus.CREATED, saleCtx.status);
        String transactionId = (String) response(saleCtx).get("transactionId");

        // 调用方读到的快照：状态仍为 NORMAL
        Transaction snapshot = transactionDAO.findById(transactionId);
        assertEquals("NORMAL", snapshot.status);

        // 模拟并发：另一个请求在"读取快照之后、抢占之前"完成了退款（抢占成功）
        try (Connection conn = getTestConnection()) {
            assertTrue(transactionDAO.claimRefundWithConnection(conn, transactionId));
        }

        // 用陈旧快照发起退款：预检放行，事务内原子抢占必须失败 → 409（而不是 500）
        TestContext refundCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions/" + transactionId + "/refund")
            .withPathParam("id", transactionId);
        TransactionApiController.refundFetched(refundCtx.context, transactionId, snapshot);

        assertEquals(HttpStatus.CONFLICT, refundCtx.status, "并发退款冲突应为 409 而非 500");
        // 也不应产生第二张退货单
        assertEquals(0, DAOFactory.getInstance().getReturnOrderDAO()
            .findByOriginalTransactionId(transactionId).size());
    }

    @Test
    @DisplayName("桌面端已有退货记录的交易不允许再走 API 退款")
    void refundRejectedWhenReturnOrderAlreadyExists() throws Exception {
        Product product = insertProduct("API重复退款商品", "APIREFUND003", new BigDecimal("10.00"), 50);
        Member member = insertMember("13900000004");

        TestContext saleCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions")
            .withBody(createRequest(product.id, 2, "现金", member.phone));
        TransactionApiController.create(saleCtx.context);
        String transactionId = (String) response(saleCtx).get("transactionId");

        // 模拟桌面退货流程：只建退货单，不改 transactions.status
        ReturnOrder existing = new ReturnOrder();
        existing.returnOrderId = DAOFactory.getInstance().getReturnOrderDAO().generateNextReturnOrderId();
        existing.originalTransactionId = transactionId;
        existing.returnDate = Instant.now();
        existing.totalAmount = new BigDecimal("20.00");
        existing.status = "COMPLETED";
        existing.operatorName = "操作员";
        assertTrue(DAOFactory.getInstance().getReturnOrderDAO().insert(existing));

        TestContext refundCtx = new TestContext()
            .withRequest(HandlerType.POST, "/api/transactions/" + transactionId + "/refund")
            .withPathParam("id", transactionId);
        TransactionApiController.refund(refundCtx.context);

        assertEquals(HttpStatus.BAD_REQUEST, refundCtx.status);
        // 交易状态、库存、余额都不得被二次退款改动
        assertEquals("NORMAL", transactionDAO.findById(transactionId).status);
        assertEquals(48, DAOFactory.getInstance().getProductDAO().findById(product.id).quantity);
        assertAmountEquals(new BigDecimal("1000.00"),
            DAOFactory.getInstance().getMemberDAO().findById(member.id).balance);
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
    @DisplayName("带日期筛选时 limit 生效，不返回整段交易")
    void listByDateRangeRespectsLimit() throws Exception {
        insertTransaction("T-LIM-001");
        insertTransaction("T-LIM-002");
        insertTransaction("T-LIM-003");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions")
            .withQueryParam("startDate", "2026-08-06")
            .withQueryParam("endDate", "2026-08-06")
            .withQueryParam("limit", "2");
        TransactionApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertEquals(2, (Integer) response(ctx).get("total"));
        assertEquals(2, ((List<?>) response(ctx).get("data")).size());
    }

    @Test
    @DisplayName("按支付方式代码筛选能匹配落库的中文")
    void listByPaymentMethodMatchesCanonicalForm() throws Exception {
        insertTransaction("T-PAY-CANON-001"); // 落库为「现金」

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions")
            .withQueryParam("paymentMethod", "CASH");
        TransactionApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertEquals(1, (Integer) response(ctx).get("total"));

        // 反向：其它支付方式不应命中现金单
        TestContext wechat = new TestContext().withRequest(HandlerType.GET, "/api/transactions")
            .withQueryParam("paymentMethod", "WECHAT");
        TransactionApiController.list(wechat.context);
        assertEquals(0, (Integer) response(wechat).get("total"));
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
