package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Transaction;
import com.cashier.service.TransactionService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 交易报表 REST API
 */
public class ReportApiController {
    private static final Logger logger = LoggerFactory.getLogger(ReportApiController.class);
    
    /**
     * 销售日报
     * GET /api/reports/daily?date=2024-01-01
     */
    public static void dailySales(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) dateStr = LocalDate.now().toString();
            LocalDate date = LocalDate.parse(dateStr);
            
            List<Transaction> transactions = TransactionDAO.findAll();
            
            // 筛选当天交易
            long dayStart = date.atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli();
            long dayEnd = date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli();
            
            // 由于 Transaction 使用 timestamp 字符串，需要解析
            List<Transaction> dayTransactions = new ArrayList<>();
            for (Transaction t : transactions) {
                if (t.timestamp != null && !t.timestamp.isEmpty()) {
                    try {
                        // 假设格式: 2024-01-01 12:30:45
                        LocalDate tDate = LocalDate.parse(t.timestamp.substring(0, 10));
                        if (tDate.equals(date)) {
                            dayTransactions.add(t);
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal cashAmount = BigDecimal.ZERO;
            BigDecimal wechatAmount = BigDecimal.ZERO;
            BigDecimal alipayAmount = BigDecimal.ZERO;
            BigDecimal cardAmount = BigDecimal.ZERO;
            
            for (Transaction t : dayTransactions) {
                if (t.finalAmount != null) {
                    totalAmount = totalAmount.add(t.finalAmount);
                    
                    String payment = t.paymentMethod != null ? t.paymentMethod : "";
                    if (payment.contains("现金")) {
                        cashAmount = cashAmount.add(t.finalAmount);
                    } else if (payment.contains("微信")) {
                        wechatAmount = wechatAmount.add(t.finalAmount);
                    } else if (payment.contains("支付宝")) {
                        alipayAmount = alipayAmount.add(t.finalAmount);
                    } else if (payment.contains("银行卡")) {
                        cardAmount = cardAmount.add(t.finalAmount);
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("date", dateStr);
            result.put("totalTransactions", dayTransactions.size());
            result.put("totalAmount", totalAmount);
            result.put("cashAmount", cashAmount);
            result.put("wechatAmount", wechatAmount);
            result.put("alipayAmount", alipayAmount);
            result.put("cardAmount", cardAmount);
            result.put("transactions", dayTransactions);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取日报失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取日报失败: " + e.getMessage()));
        }
    }
    
    /**
     * 销售月报
     * GET /api/reports/monthly?month=2024-01
     */
    public static void monthlySales(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) monthStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            List<Transaction> transactions = TransactionDAO.findAll();
            
            // 筛选当月交易
            List<Transaction> monthTransactions = new ArrayList<>();
            for (Transaction t : transactions) {
                if (t.timestamp != null && !t.timestamp.isEmpty()) {
                    try {
                        String tMonth = t.timestamp.substring(0, 7); // 2024-01
                        if (tMonth.equals(monthStr)) {
                            monthTransactions.add(t);
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            Map<String, BigDecimal> dailyAmounts = new TreeMap<>();
            Map<String, Integer> dailyCounts = new TreeMap<>();
            
            for (Transaction t : monthTransactions) {
                if (t.finalAmount != null) {
                    totalAmount = totalAmount.add(t.finalAmount);
                    
                    String day = t.timestamp.substring(0, 10);
                    dailyAmounts.merge(day, t.finalAmount, BigDecimal::add);
                    dailyCounts.merge(day, 1, Integer::sum);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("month", monthStr);
            result.put("totalTransactions", monthTransactions.size());
            result.put("totalAmount", totalAmount);
            result.put("dayCount", dailyAmounts.size());
            result.put("dailyAmounts", dailyAmounts);
            result.put("dailyCounts", dailyCounts);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取月报失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取月报失败: " + e.getMessage()));
        }
    }
    
    /**
     * 商品销售排行
     * GET /api/reports/top-products?limit=10
     */
    public static void topProducts(Context ctx) {
        try {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(10);
            
            List<Transaction> transactions = TransactionDAO.findAll();
            
            // 统计商品销量
            Map<String, Integer> productCounts = new HashMap<>();
            Map<String, BigDecimal> productAmounts = new HashMap<>();
            
            for (Transaction t : transactions) {
                if (t.items != null) {
                    for (var item : t.items) {
                        String name = item.name;
                        productCounts.merge(name, item.quantity, Integer::sum);
                        productAmounts.merge(name, item.price.multiply(BigDecimal.valueOf(item.quantity)), BigDecimal::add);
                    }
                }
            }
            
            // 排序
            List<Map<String, Object>> topList = new ArrayList<>();
            productCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .forEach(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", e.getKey());
                    item.put("quantity", e.getValue());
                    item.put("amount", productAmounts.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    topList.add(item);
                });
            
            ctx.json(Map.of("success", true, "data", topList));
        } catch (Exception e) {
            logger.error("获取商品排行失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取商品排行失败: " + e.getMessage()));
        }
    }
    
    /**
     * 支付方式统计
     * GET /api/reports/payment-methods
     */
    public static void paymentMethods(Context ctx) {
        try {
            List<Transaction> transactions = TransactionDAO.findAll();
            
            Map<String, Integer> methodCounts = new HashMap<>();
            Map<String, BigDecimal> methodAmounts = new HashMap<>();
            
            for (Transaction t : transactions) {
                String method = t.paymentMethod != null ? t.paymentMethod : "未知";
                methodCounts.merge(method, 1, Integer::sum);
                methodAmounts.merge(method, t.finalAmount != null ? t.finalAmount : BigDecimal.ZERO, BigDecimal::add);
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            methodCounts.forEach((method, count) -> {
                Map<String, Object> item = new HashMap<>();
                item.put("method", method);
                item.put("count", count);
                item.put("amount", methodAmounts.get(method));
                result.add(item);
            });
            
            ctx.json(Map.of("success", true, "data", result));
        } catch (Exception e) {
            logger.error("获取支付方式统计失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取支付方式统计失败: " + e.getMessage()));
        }
    }
}