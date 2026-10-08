package com.cashier.controller;

import com.cashier.model.Product;
import com.cashier.model.ReturnOrder;
import com.cashier.model.ReturnOrderItem;
import com.cashier.model.Transaction;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 利润报表的净额口径：**已完成退货**必须冲减收入与成本（2026-10 审计 F4）。
 *
 * <p>背景：桌面退货流程从不把原交易标成 {@code REFUNDED}（只有 API 退款会），而
 * {@code ProfitReportController} 原来只跳过 {@code REFUNDED} 的交易，于是卖出去又退回来的单子
 * 依旧计入利润（卖 100 成本 60 后退掉，利润仍显示 40）。</p>
 *
 * <p>控制器依赖 JavaFX 控件，无法在无头环境构造出可用实例；但聚合逻辑只读普通字段，
 * 因此用反射塞入数据后调用（仓库里已有同类反射式测试）。</p>
 */
@DisplayName("利润报表退货冲减测试")
class ProfitReportControllerReturnDeductionTest extends DatabaseTestBase {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 8);

    private static Product product() {
        Product product = new Product();
        product.id = 1;
        product.name = "利润测试商品";
        product.price = new BigDecimal("100.00");
        product.quantity = 1;
        product.cost = new BigDecimal("60.00");
        product.category = "测试分类";
        return product;
    }

    private static Transaction sale(Product product) {
        Transaction transaction = new Transaction();
        transaction.transactionId = "T-PROFIT-1";
        transaction.timestamp = "2026-10-08 10:00:00";
        transaction.totalAmount = new BigDecimal("100.00");
        transaction.finalAmount = new BigDecimal("100.00");
        transaction.status = "NORMAL";
        transaction.items = new ArrayList<>(List.of(product));
        return transaction;
    }

    private static ReturnOrder completedReturn(LocalDate completedOn) {
        ReturnOrder order = new ReturnOrder();
        order.returnOrderId = "R-PROFIT-1";
        order.status = "COMPLETED";
        order.completedDate = completedOn.atStartOfDay(ZoneId.systemDefault()).toInstant();
        order.totalAmount = new BigDecimal("100.00");
        return order;
    }

    private static ReturnOrderItem returnedLine(Product product) {
        ReturnOrderItem item = new ReturnOrderItem();
        item.returnOrderId = "R-PROFIT-1";
        item.productName = product.name;
        item.returnQuantity = 1;
        item.unitPrice = new BigDecimal("100.00");
        item.returnAmount = new BigDecimal("100.00");
        return item;
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field declared = target.getClass().getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(target, value);
    }

    private static Object collect(Object target, List<Transaction> transactions, List<ReturnOrder> returns,
                                  Map<String, List<ReturnOrderItem>> itemsByOrder) throws Exception {
        inject(target, "allTransactions", new ArrayList<>(transactions));
        inject(target, "productNameMap", new HashMap<>(Map.of("利润测试商品", product())));
        inject(target, "productActualCostMap", new HashMap<String, Double>());
        inject(target, "periodReturns", new ArrayList<>(returns));
        inject(target, "returnItemsByOrder", new HashMap<>(itemsByOrder));

        for (Method method : target.getClass().getDeclaredMethods()) {
            if (method.getName().equals("collectProfitStatistics") && method.getParameterCount() == 3) {
                method.setAccessible(true);
                return method.invoke(target, DAY, DAY, null);
            }
        }
        throw new NoSuchMethodException("collectProfitStatistics");
    }

    private static double readDouble(Object statistics, String field) throws Exception {
        Field declared = statistics.getClass().getDeclaredField(field);
        declared.setAccessible(true);
        return declared.getDouble(statistics);
    }

    @Test
    @DisplayName("卖 100（成本 60）后退掉：收入与成本都冲回 0，而不是继续算 40 利润")
    void completedReturnDeductsRevenueAndCost() throws Exception {
        Product product = product();
        ReturnOrder order = completedReturn(DAY);

        Object statistics = collect(new ProfitReportController(),
            List.of(sale(product)),
            List.of(order),
            Map.of(order.returnOrderId, List.of(returnedLine(product))));

        assertEquals(0.0, readDouble(statistics, "totalRevenue"), 0.001,
            "桌面退货不写 REFUNDED，必须靠退货单把收入冲掉");
        assertEquals(0.0, readDouble(statistics, "totalCost"), 0.001,
            "货已回库，成本也要冲回，否则利润被低估");
    }

    @Test
    @DisplayName("没有退货时口径不变：收入 100、成本 60")
    void saleWithoutReturnKeepsOriginalAmounts() throws Exception {
        Product product = product();

        Object statistics = collect(new ProfitReportController(),
            List.of(sale(product)), List.of(), Map.of());

        assertEquals(100.0, readDouble(statistics, "totalRevenue"), 0.001);
        assertEquals(60.0, readDouble(statistics, "totalCost"), 0.001);
    }

    @Test
    @DisplayName("区间外的退货不冲减本区间（按退货完成日归属）")
    void returnOutsideRangeIsIgnored() throws Exception {
        Product product = product();
        ReturnOrder order = completedReturn(DAY.minusDays(3));

        Object statistics = collect(new ProfitReportController(),
            List.of(sale(product)),
            List.of(order),
            Map.of(order.returnOrderId, List.of(returnedLine(product))));

        assertEquals(100.0, readDouble(statistics, "totalRevenue"), 0.001);
        assertTrue(readDouble(statistics, "totalCost") > 0);
    }
}
