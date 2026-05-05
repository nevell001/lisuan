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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 交易收银 REST API
 */
public class TransactionApiController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionApiController.class);
    private static final DateTimeFormatter ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    
    /**
     * 获取交易列表
     * GET /api/transactions
     */
    public static void list(Context ctx) {
        try {
            String startDate = ctx.queryParam("startDate");
            String endDate = ctx.queryParam("endDate");
            String paymentMethod = ctx.queryParam("paymentMethod");
            
            List<Transaction> transactions = TransactionDAO.findAll();
            
            // 按条件筛选
            if (startDate != null || endDate != null || paymentMethod != null) {
                transactions = filterTransactions(transactions, startDate, endDate, paymentMethod);
            }
            
            // 按时间倒序
            transactions.sort((a, b) -> {
                if (a.timestamp == null || b.timestamp == null) return 0;
                return b.timestamp.compareTo(a.timestamp);
            });
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", transactions);
            result.put("total", transactions.size());
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取交易列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取交易列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个交易
     * GET /api/transactions/:id
     */
    public static void get(Context ctx) {
        try {
            String transactionId = ctx.pathParam("id");
            Transaction transaction = TransactionDAO.findById(transactionId);
            
            if (transaction == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "交易不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", transaction));
        } catch (Exception e) {
            logger.error("获取交易详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取交易详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建新交易（收银）
     * POST /api/transactions
     */
    public static void create(Context ctx) {
        try {
            TransactionRequest request = ctx.bodyAsClass(TransactionRequest.class);
            
            // 生成交易ID
            String transactionId = "T" + LocalDateTime.now().format(ID_FORMATTER);
            
            Transaction transaction = new Transaction();
            transaction.transactionId = transactionId;
            transaction.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            transaction.items = request.items;
            transaction.totalAmount = request.totalAmount != null ? request.totalAmount : BigDecimal.ZERO;
            transaction.tax = request.tax != null ? request.tax : BigDecimal.ZERO;
            transaction.finalAmount = request.finalAmount != null ? request.finalAmount : BigDecimal.ZERO;
            transaction.paymentMethod = request.paymentMethod != null ? request.paymentMethod : "现金";
            transaction.memberId = request.memberId != null ? request.memberId : 0;
            transaction.memberPhone = request.memberPhone != null ? request.memberPhone : "";
            transaction.memberName = request.memberName != null ? request.memberName : "";
            transaction.operatorUsername = request.operatorUsername != null ? request.operatorUsername : "";
            transaction.operatorName = request.operatorName != null ? request.operatorName : "";
            
            TransactionDAO.insert(transaction);
            
            logger.info("创建交易: {} - 金额: {} - 支付方式: {}", 
                transactionId, transaction.finalAmount, transaction.paymentMethod);
            
            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", transaction, "transactionId", transactionId));
        } catch (Exception e) {
            logger.error("创建交易失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建交易失败: " + e.getMessage()));
        }
    }
    
    /**
     * 取消交易（退货）
     * POST /api/transactions/:id/refund
     */
    public static void refund(Context ctx) {
        try {
            String transactionId = ctx.pathParam("id");
            Transaction transaction = TransactionDAO.findById(transactionId);
            
            if (transaction == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "交易不存在"));
                return;
            }
            
            // TODO: 实现退货逻辑
            // 1. 恢复库存
            // 2. 创建退款记录
            // 3. 更新会员积分
            
            logger.info("交易退款: {}", transactionId);
            ctx.json(Map.of("success", true, "message", "退款成功", "transactionId", transactionId));
        } catch (Exception e) {
            logger.error("交易退款失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "交易退款失败: " + e.getMessage()));
        }
    }
    
    /**
     * 今日交易统计
     * GET /api/transactions/today
     */
    public static void todayStats(Context ctx) {
        try {
            List<Transaction> transactions = TransactionDAO.findAll();
            
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            int count = 0;
            
            for (Transaction t : transactions) {
                if (t.timestamp != null && t.timestamp.startsWith(today)) {
                    count++;
                    if (t.finalAmount != null) {
                        totalAmount = totalAmount.add(t.finalAmount);
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("date", today);
            result.put("count", count);
            result.put("totalAmount", totalAmount);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取今日交易统计失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取今日交易统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 筛选交易
     */
    private static List<Transaction> filterTransactions(List<Transaction> transactions, 
            String startDate, String endDate, String paymentMethod) {
        List<Transaction> result = new ArrayList<>();
        
        for (Transaction t : transactions) {
            // 支付方式筛选
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                if (t.paymentMethod == null || !t.paymentMethod.contains(paymentMethod)) {
                    continue;
                }
            }
            
            // 日期筛选
            if (t.timestamp != null && !t.timestamp.isEmpty()) {
                String date = t.timestamp.substring(0, 10);
                
                if (startDate != null && !startDate.isEmpty() && date.compareTo(startDate) < 0) {
                    continue;
                }
                
                if (endDate != null && !endDate.isEmpty() && date.compareTo(endDate) > 0) {
                    continue;
                }
            }
            
            result.add(t);
        }
        
        return result;
    }
    
    /**
     * 交易请求DTO
     */
    public static class TransactionRequest {
        public List<com.cashier.model.Product> items;
        public BigDecimal totalAmount;
        public BigDecimal tax;
        public BigDecimal finalAmount;
        public String paymentMethod;
        public Integer memberId;
        public String memberPhone;
        public String memberName;
        public String operatorUsername;
        public String operatorName;
    }
}