package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.TransactionDAORefactored;
import com.cashier.model.ReturnOrder;
import com.cashier.model.Transaction;
import com.cashier.util.DatabaseManager;
import com.cashier.util.DatabaseTestBase;
import com.cashier.util.DateTimeFormats;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportApiControllerTest extends DatabaseTestBase {

    private final TransactionDAORefactored transactionDAO = DAOFactory.getInstance().getTransactionDAO();

    private void insertTransaction(String id, String timestamp, String paymentMethod) throws Exception {
        Transaction transaction = new Transaction(
            id, timestamp, List.of(),
            BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        transaction.paymentMethod = paymentMethod;
        transaction.operatorUsername = "op";
        transaction.operatorName = "操作员";
        assertTrue(transactionDAO.insert(transaction));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("日报返回成功")
    void dailySalesReturnsSuccess() throws Exception {
        insertTransaction("R-DAILY-001", "2026-08-06 12:00:00", "现金");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/daily-sales")
            .withQueryParam("date", "2026-08-06");
        ReportApiController.dailySales(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("月报返回成功")
    void monthlySalesReturnsSuccess() throws Exception {
        insertTransaction("R-MONTH-001", "2026-08-06 12:00:00", "微信");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/monthly-sales")
            .withQueryParam("month", "2026-08");
        ReportApiController.monthlySales(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("商品销售排行返回成功")
    void topProductsReturnsSuccess() throws Exception {
        insertTransaction("R-TOP-001", "2026-08-06 12:00:00", "现金");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/top-products");
        ReportApiController.topProducts(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("支付方式统计返回成功")
    void paymentMethodsReturnsSuccess() throws Exception {
        insertTransaction("R-PAY-001", "2026-08-06 12:00:00", "支付宝");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/payment-methods");
        ReportApiController.paymentMethods(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("日报：支付方式按归一化分桶——代码形式的 CASH 也进现金桶")
    void dailySalesBucketsPaymentMethodAfterNormalization() throws Exception {
        insertTransaction("R-NORM-001", "2026-08-08 10:00:00", "CASH");
        insertTransaction("R-NORM-002", "2026-08-08 11:00:00", "现金");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/daily-sales")
            .withQueryParam("date", "2026-08-08");
        ReportApiController.dailySales(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        // 两笔都是现金：此前裸 contains("现金") 会让 CASH 那笔计入总额却不出现在现金桶里
        assertAmountEquals("20.00", response(ctx).get("cashAmount"));
        assertAmountEquals("20.00", response(ctx).get("totalAmount"));
    }

    @Test
    @DisplayName("日报净额口径：已整单退款不计营业额，已完成退货从同渠道桶与净额中扣除")
    void dailySalesIsNetOfRefundedTransactionsAndCompletedReturns() throws Exception {
        insertTransaction("R-NET-001", "2026-08-09 10:00:00", "现金");   // 有效销售 10 元
        insertTransaction("R-NET-002", "2026-08-09 11:00:00", "现金");   // 该单整单退款

        assertTrue(DatabaseManager.executeBooleanTransaction(conn ->
            transactionDAO.updateStatusWithConnection(conn, "R-NET-002", "REFUNDED")));

        // 桌面退货（原单未标记 REFUNDED）4 元、退现金、当日完成 → 应从现金桶与净额中扣除
        insertCompletedReturn("RET-NET-001", "R-NET-001", "现金", "4.00", "2026-08-09 12:00:00");
        // 原单已 REFUNDED 的退货单：不能再扣一次（API 退款会同时写 REFUNDED 与退货单）
        insertCompletedReturn("RET-NET-002", "R-NET-002", "现金", "10.00", "2026-08-09 13:00:00");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/daily-sales")
            .withQueryParam("date", "2026-08-09");
        ReportApiController.dailySales(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertEquals(1, response(ctx).get("totalTransactions"), "已退款交易不计入有效销售笔数");
        assertAmountEquals("10.00", response(ctx).get("totalAmount"));
        assertAmountEquals("4.00", response(ctx).get("refundedAmount"), "已 REFUNDED 的退货单不得重复扣减");
        assertAmountEquals("6.00", response(ctx).get("netAmount"));
        assertAmountEquals("6.00", response(ctx).get("cashAmount"), "现金退款要从现金桶扣除");
    }

    @Test
    @DisplayName("月报净额口径：日趋势已扣退货，ΣdailyAmounts 与 netAmount 可对账")
    void monthlySalesNetMatchesDailyTrend() throws Exception {
        insertTransaction("R-MON-001", "2026-09-03 10:00:00", "微信");
        insertCompletedReturn("RET-MON-001", "R-MON-001", "微信", "3.00", "2026-09-03 12:00:00");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/monthly-sales")
            .withQueryParam("month", "2026-09");
        ReportApiController.monthlySales(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertAmountEquals("10.00", response(ctx).get("totalAmount"));
        assertAmountEquals("3.00", response(ctx).get("refundedAmount"));
        assertAmountEquals("7.00", response(ctx).get("netAmount"));

        @SuppressWarnings("unchecked")
        Map<String, Object> dailyAmounts = (Map<String, Object>) response(ctx).get("dailyAmounts");
        BigDecimal trendSum = dailyAmounts.values().stream()
            .map(value -> new BigDecimal(value.toString()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("7.00").compareTo(trendSum),
            "日趋势之和必须等于 netAmount，否则月报自相矛盾");
    }

    /** 直接写一笔"已完成"的退货单（绕过审批流，用于净额口径验证）。 */
    private void insertCompletedReturn(String returnOrderId, String originalTransactionId,
                                       String paymentMethod, String amount, String completedAt) {
        ReturnOrder order = new ReturnOrder();
        order.returnOrderId = returnOrderId;
        order.originalTransactionId = originalTransactionId;
        order.paymentMethod = paymentMethod;
        order.totalAmount = new BigDecimal(amount);
        order.status = "COMPLETED";
        order.operatorName = "op";
        Instant completed = LocalDateTime.parse(completedAt, DateTimeFormats.STANDARD_DATE_TIME)
            .atZone(ZoneId.systemDefault()).toInstant();
        order.returnDate = completed;
        order.completedDate = completed;
        assertTrue(DAOFactory.getInstance().getReturnOrderDAO().insert(order));
    }

    private static void assertAmountEquals(String expected, Object actual) {
        assertAmountEquals(expected, actual, null);
    }

    private static void assertAmountEquals(String expected, Object actual, String message) {
        assertNotNull(actual, "字段缺失，期望 " + expected);
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())),
            (message != null ? message + "：" : "") + "期望 " + expected + "，实际 " + actual);
    }
}
