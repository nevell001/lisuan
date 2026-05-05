package com.cashier.api.controller;

import com.cashier.dao.InvoiceDAO;
import com.cashier.model.Invoice;
import com.cashier.service.InvoiceService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发票管理 REST API
 */
public class InvoiceApiController {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceApiController.class);
    
    /**
     * 获取发票列表
     * GET /api/invoices
     */
    public static void list(Context ctx) {
        try {
            String startDate = ctx.queryParam("startDate");
            String endDate = ctx.queryParam("endDate");
            String status = ctx.queryParam("status");
            
            List<Invoice> invoices;
            
            if (startDate != null && endDate != null) {
                Date start = java.sql.Date.valueOf(startDate);
                Date end = java.sql.Date.valueOf(endDate);
                invoices = InvoiceService.getInvoicesByDateRange(start, end);
            } else {
                invoices = InvoiceService.getAllInvoices();
            }
            
            // 状态筛选
            if (status != null && !status.isEmpty()) {
                invoices = invoices.stream()
                    .filter(i -> i.status.equals(status))
                    .toList();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", invoices);
            result.put("total", invoices.size());
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取发票列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取发票列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个发票
     * GET /api/invoices/:id
     */
    public static void get(Context ctx) {
        try {
            String invoiceId = ctx.pathParam("id");
            Invoice invoice = InvoiceService.getInvoice(invoiceId);
            
            if (invoice == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "发票不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", invoice));
        } catch (Exception e) {
            logger.error("获取发票详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取发票详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据交易ID获取发票
     * GET /api/invoices/transaction/:transactionId
     */
    public static void getByTransaction(Context ctx) {
        try {
            String transactionId = ctx.pathParam("transactionId");
            Invoice invoice = InvoiceService.getInvoiceByTransaction(transactionId);
            
            if (invoice == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "该交易未开具发票"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", invoice));
        } catch (Exception e) {
            logger.error("获取交易发票失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取交易发票失败: " + e.getMessage()));
        }
    }
    
    /**
     * 从交易创建发票
     * POST /api/invoices/from-transaction
     */
    public static void createFromTransaction(Context ctx) {
        try {
            InvoiceService.InvoiceRequest request = ctx.bodyAsClass(InvoiceService.InvoiceRequest.class);
            
            if (request.transactionId == null || request.transactionId.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "交易ID不能为空"));
                return;
            }
            
            Invoice invoice = InvoiceService.createInvoiceFromTransaction(request.transactionId, request);
            
            logger.info("发票创建成功: {}", invoice.invoiceId);
            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", invoice, "message", "发票创建成功"));
        } catch (Exception e) {
            logger.error("创建发票失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建发票失败: " + e.getMessage()));
        }
    }
    
    /**
     * 手工创建发票
     * POST /api/invoices/manual
     */
    public static void createManual(Context ctx) {
        try {
            InvoiceService.InvoiceRequest request = ctx.bodyAsClass(InvoiceService.InvoiceRequest.class);
            
            if (request.items == null || request.items.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "商品明细不能为空"));
                return;
            }
            
            Invoice invoice = InvoiceService.createManualInvoice(request);
            
            logger.info("手工发票创建成功: {}", invoice.invoiceId);
            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", invoice, "message", "发票创建成功"));
        } catch (Exception e) {
            logger.error("创建手工发票失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建手工发票失败: " + e.getMessage()));
        }
    }
    
    /**
     * 作废发票
     * POST /api/invoices/:id/void
     */
    public static void voidInvoice(Context ctx) {
        try {
            String invoiceId = ctx.pathParam("id");
            VoidRequest request = ctx.bodyAsClass(VoidRequest.class);
            
            if (request.reason == null || request.reason.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "作废原因不能为空"));
                return;
            }
            
            Invoice invoice = InvoiceService.voidInvoice(invoiceId, request.reason);
            
            logger.info("发票作废: {} - 原因: {}", invoiceId, request.reason);
            ctx.json(Map.of("success", true, "data", invoice, "message", "发票已作废"));
        } catch (Exception e) {
            logger.error("作废发票失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "作废发票失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新发票打印信息
     * POST /api/invoices/:id/print
     */
    public static void recordPrint(Context ctx) {
        try {
            String invoiceId = ctx.pathParam("id");
            PrintRequest request = ctx.bodyAsClass(PrintRequest.class);
            
            InvoiceDAO.updatePrintInfo(invoiceId, request.pdfPath, request.imagePath);
            
            logger.info("发票打印记录: {}", invoiceId);
            ctx.json(Map.of("success", true, "message", "打印记录已更新"));
        } catch (Exception e) {
            logger.error("记录打印失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "记录打印失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取销售方默认信息
     * GET /api/invoices/seller-info
     */
    public static void getSellerInfo(Context ctx) {
        try {
            Map<String, String> info = InvoiceService.getDefaultSellerInfo();
            ctx.json(Map.of("success", true, "data", info));
        } catch (Exception e) {
            logger.error("获取销售方信息失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取销售方信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 设置销售方默认信息
     * PUT /api/invoices/seller-info
     */
    public static void setSellerInfo(Context ctx) {
        try {
            Map<String, String> info = ctx.bodyAsClass(Map.class);
            
            InvoiceService.setDefaultSellerInfo(
                info.getOrDefault("name", ""),
                info.getOrDefault("taxId", ""),
                info.getOrDefault("address", ""),
                info.getOrDefault("phone", ""),
                info.getOrDefault("bank", "")
            );
            
            logger.info("销售方信息已更新");
            ctx.json(Map.of("success", true, "message", "销售方信息已更新"));
        } catch (Exception e) {
            logger.error("设置销售方信息失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "设置销售方信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 发票统计
     * GET /api/invoices/stats
     */
    public static void stats(Context ctx) {
        try {
            String startDate = ctx.queryParam("startDate");
            String endDate = ctx.queryParam("endDate");
            
            Date start = startDate != null ? java.sql.Date.valueOf(startDate) : new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
            Date end = endDate != null ? java.sql.Date.valueOf(endDate) : new Date();
            
            List<Invoice> invoices = InvoiceService.getInvoicesByDateRange(start, end);
            
            int totalCount = invoices.size();
            int issuedCount = 0;
            int printedCount = 0;
            int voidedCount = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (Invoice i : invoices) {
                switch (i.status) {
                    case "ISSUED": issuedCount++; break;
                    case "PRINTED": printedCount++; break;
                    case "VOIDED": voidedCount++; break;
                }
                
                if (!"VOIDED".equals(i.status)) {
                    totalAmount = totalAmount.add(i.finalAmount);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("totalCount", totalCount);
            result.put("issuedCount", issuedCount);
            result.put("printedCount", printedCount);
            result.put("voidedCount", voidedCount);
            result.put("totalAmount", totalAmount);
            result.put("startDate", startDate);
            result.put("endDate", endDate);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("发票统计失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "发票统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 作废请求 DTO
     */
    public static class VoidRequest {
        public String reason;
    }
    
    /**
     * 打印请求 DTO
     */
    public static class PrintRequest {
        public String pdfPath;
        public String imagePath;
    }
}