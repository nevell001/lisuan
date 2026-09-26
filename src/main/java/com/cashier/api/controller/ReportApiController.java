package com.cashier.api.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.model.Transaction;
import com.cashier.service.TransactionService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 交易报表 REST API
 *
 * <p>营业额为净额口径（TD-003）：已整单退款（{@code status='REFUNDED'}）的交易不计入
 * {@code totalAmount}，该期间已完成的退货按退款方式冲减对应渠道桶，另给
 * {@code refundedAmount} / {@code netAmount} 供对账。</p>
 */
public class ReportApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReportApiController.class);
    private static final int DEFAULT_TOP_PRODUCTS_LIMIT = 10;
    private static final int MAX_TOP_PRODUCTS_LIMIT = 100;

    /** 该交易是否已被整单退款（不计营业额，但仍在明细里供对账）。 */
    private static boolean isRefunded(Transaction transaction) {
        return transaction != null && "REFUNDED".equals(transaction.status);
    }

    /** 从渠道桶里扣掉同渠道的退货金额；退货发生在收款之前（如退昨天的现金单）时允许为负。 */
    private static BigDecimal subtractReturns(BigDecimal bucket, BigDecimal returns) {
        return returns == null ? bucket : bucket.subtract(returns);
    }
    
    /**
     * 销售日报
     * GET /api/reports/daily?date=2024-01-01
     */
    public static void dailySales(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) dateStr = LocalDate.now().toString();
            LocalDate date = LocalDate.parse(dateStr);
            String dayStart = date.atStartOfDay().format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
            String dayEnd = date.plusDays(1).atStartOfDay().minusSeconds(1)
                .format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
            List<Transaction> dayTransactions = DAOFactory.getInstance().getTransactionDAO().findByDateRange(dayStart, dayEnd);
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal cashAmount = BigDecimal.ZERO;
            BigDecimal wechatAmount = BigDecimal.ZERO;
            BigDecimal alipayAmount = BigDecimal.ZERO;
            BigDecimal cardAmount = BigDecimal.ZERO;
            int effectiveTransactions = 0;
            
            for (Transaction t : dayTransactions) {
                // 已整单退款的交易不计营业额（净额口径，见 docs/TECH_DEBT.md 的 TD-003），
                // 但仍出现在 transactions 明细里供对账/审计
                if (isRefunded(t)) {
                    continue;
                }
                if (t.finalAmount != null) {
                    effectiveTransactions++;
                    totalAmount = totalAmount.add(t.finalAmount);
                    
                    // 归一化后再分桶：库里可能同时存在「现金」与历史/导入的 CASH，
                    // 裸 contains 会让代码形式的现金单计入 totalAmount 却不出现在任何桶里
                    String payment = com.cashier.util.I18nUiUtils.canonicalPaymentMethod(t.paymentMethod);
                    if ("CASH".equals(payment)) {
                        cashAmount = cashAmount.add(t.finalAmount);
                    } else if ("WECHAT".equals(payment)) {
                        wechatAmount = wechatAmount.add(t.finalAmount);
                    } else if ("ALIPAY".equals(payment)) {
                        alipayAmount = alipayAmount.add(t.finalAmount);
                    } else if ("CARD".equals(payment)) {
                        cardAmount = cardAmount.add(t.finalAmount);
                    }
                }
            }

            // 该日已完成的退货按退款方式冲减对应渠道：现金退款减少现金桶，退回余额只影响总额
            java.util.Map<String, BigDecimal> returns = TransactionService.returnsByMethod(
                DAOFactory.getInstance().getReturnOrderDAO().findCompletedReturnsBetween(dayStart, dayEnd));
            BigDecimal refundedAmount = TransactionService.totalReturns(returns);
            cashAmount = subtractReturns(cashAmount, returns.get("CASH"));
            wechatAmount = subtractReturns(wechatAmount, returns.get("WECHAT"));
            alipayAmount = subtractReturns(alipayAmount, returns.get("ALIPAY"));
            cardAmount = subtractReturns(cardAmount, returns.get("CARD"));
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("date", dateStr);
            result.put("totalTransactions", effectiveTransactions);
            // totalAmount = 有效销售（不含已退款交易）；netAmount = 再扣掉该日已完成的退货
            result.put("totalAmount", totalAmount);
            result.put("refundedAmount", refundedAmount);
            result.put("netAmount", totalAmount.subtract(refundedAmount));
            result.put("cashAmount", cashAmount);
            result.put("wechatAmount", wechatAmount);
            result.put("alipayAmount", alipayAmount);
            result.put("cardAmount", cardAmount);
            result.put("transactions", dayTransactions);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取日报失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取日报失败"));
        }
    }
    
    /**
     * 销售月报
     * GET /api/reports/monthly?month=2024-01
     */
    public static void monthlySales(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) monthStr = LocalDate.now().format(com.cashier.util.DateTimeFormats.MONTH);
            LocalDate monthStart = LocalDate.parse(monthStr + "-01", com.cashier.util.DateTimeFormats.DATE);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            String monthStartDateTime = monthStart.atStartOfDay()
                .format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
            String monthEndDateTime = monthEnd.plusDays(1).atStartOfDay().minusSeconds(1)
                .format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
            List<Transaction> monthTransactions = DAOFactory.getInstance().getTransactionDAO().findByDateRange(monthStartDateTime, monthEndDateTime);
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            Map<String, BigDecimal> dailyAmounts = new TreeMap<>();
            Map<String, Integer> dailyCounts = new TreeMap<>();
            int effectiveTransactions = 0;
            
            for (Transaction t : monthTransactions) {
                if (isRefunded(t)) {
                    continue; // 已整单退款不计营业额（净额口径，TD-003）
                }
                if (t.finalAmount != null && t.timestamp != null && t.timestamp.length() >= 10) {
                    effectiveTransactions++;
                    totalAmount = totalAmount.add(t.finalAmount);
                    
                    String day = t.timestamp.substring(0, 10);
                    dailyAmounts.merge(day, t.finalAmount, BigDecimal::add);
                    dailyCounts.merge(day, 1, Integer::sum);
                }
            }

            // 按退货完成日冲减日趋势，保证 ΣdailyAmounts == netAmount 可对账
            List<com.cashier.model.ReturnOrder> monthReturns = DAOFactory.getInstance().getReturnOrderDAO()
                .findCompletedReturnsBetween(monthStartDateTime, monthEndDateTime);
            BigDecimal refundedAmount = TransactionService.totalReturns(TransactionService.returnsByMethod(monthReturns));
            for (Map.Entry<String, BigDecimal> entry : TransactionService.returnsByDay(monthReturns).entrySet()) {
                dailyAmounts.merge(entry.getKey(), entry.getValue().negate(), BigDecimal::add);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("month", monthStr);
            result.put("totalTransactions", effectiveTransactions);
            result.put("totalAmount", totalAmount);
            result.put("refundedAmount", refundedAmount);
            result.put("netAmount", totalAmount.subtract(refundedAmount));
            result.put("dayCount", dailyAmounts.size());
            result.put("dailyAmounts", dailyAmounts);
            result.put("dailyCounts", dailyCounts);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取月报失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取月报失败"));
        }
    }
    
    /**
     * 商品销售排行
     * GET /api/reports/top-products?limit=10
     */
    public static void topProducts(Context ctx) {
        try {
            int requestedLimit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(DEFAULT_TOP_PRODUCTS_LIMIT);
            int limit = Math.max(1, Math.min(requestedLimit, MAX_TOP_PRODUCTS_LIMIT));
            List<Map<String, Object>> topList = DAOFactory.getInstance().getTransactionDAO().getTopProducts(limit);
            
            ctx.json(Map.of("success", true, "data", topList));
        } catch (Exception e) {
            logger.error("获取商品排行失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取商品排行失败"));
        }
    }
    
    /**
     * 支付方式统计
     * GET /api/reports/payment-methods
     */
    public static void paymentMethods(Context ctx) {
        try {
            List<Map<String, Object>> result = DAOFactory.getInstance().getTransactionDAO().getPaymentMethodStats();
            
            ctx.json(Map.of("success", true, "data", result));
        } catch (Exception e) {
            logger.error("获取支付方式统计失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取支付方式统计失败"));
        }
    }
}
