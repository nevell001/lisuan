package com.cashier.api.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.model.Invoice;
import com.cashier.model.PageResult;
import com.cashier.service.InvoiceService;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;

import com.cashier.util.DateTimeFormats;
import java.time.LocalDate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发票管理 REST API
 */
public class InvoiceApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InvoiceApiController.class);
    
    /**
     * 获取发票列表
     * GET /api/invoices
     */
    public static void list(Context ctx) {
        try {
            String startDate = ctx.queryParam("startDate");
            String endDate = ctx.queryParam("endDate");
            String status = ctx.queryParam("status");
            ApiPagination.PageRequest page = ApiPagination.from(ctx);
            LocalDate start = startDate != null ? LocalDate.parse(startDate, DateTimeFormats.DATE) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate, DateTimeFormats.DATE) : null;
            PageResult<Invoice> invoices = InvoiceService.getInvoicesPage(
                start,
                end,
                status,
                page.page(),
                page.pageSize()
            );

            ctx.json(ApiPagination.success(invoices));
        } catch (Exception e) {
            logger.error("获取发票列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取发票列表失败"));
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
               .json(Map.of("success", false, "message", "获取发票详情失败"));
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
               .json(Map.of("success", false, "message", "获取交易发票失败"));
        }
    }
    
    /**
     * 从交易创建发票
     * POST /api/invoices/from-transaction
     */
    public static void createFromTransaction(Context ctx) {
        try {
            InvoiceService.InvoiceRequest request = ApiRequest.parse(ctx, InvoiceService.InvoiceRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "请求体为空或字段不合法"));
                return;
            }
            
            if (request.transactionId == null || request.transactionId.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "交易ID不能为空"));
                return;
            }
            
            // TD-038：开票方/税率/开票人不接受请求体自报，开票人取认证用户
            com.cashier.model.User operator = ctx.attribute("currentUser");
            String operatorName = operator != null
                ? (operator.name != null && !operator.name.isBlank() ? operator.name : operator.username)
                : null;
            if (request.sellerName != null || request.sellerTaxId != null || request.taxRate != null
                    || request.createBy != null) {
                logger.warn("忽略 from-transaction 请求体里的开票方/税率/开票人自报字段: operator={}, taxRate={}, createBy={}",
                    operatorName, request.taxRate, request.createBy);
            }
            Invoice invoice = InvoiceService.createInvoiceFromTransaction(request.transactionId, request, operatorName);
            
            logger.info("发票创建成功: {}", invoice.invoiceId);
            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", invoice, "message", "发票创建成功"));
        } catch (Exception e) {
            logger.error("创建发票失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建发票失败"));
        }
    }
    
    /**
     * 手工创建发票
     * POST /api/invoices/manual
     */
    public static void createManual(Context ctx) {
        try {
            InvoiceService.InvoiceRequest request = ApiRequest.parse(ctx, InvoiceService.InvoiceRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "请求体为空或字段不合法"));
                return;
            }
            
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
               .json(Map.of("success", false, "message", "创建手工发票失败"));
        }
    }
    
    /**
     * 作废发票
     * POST /api/invoices/:id/void
     */
    public static void voidInvoice(Context ctx) {
        try {
            String invoiceId = ctx.pathParam("id");
            VoidRequest request = ApiRequest.parse(ctx, VoidRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "请求体为空或字段不合法"));
                return;
            }
            
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
               .json(Map.of("success", false, "message", "作废发票失败"));
        }
    }
    
    /**
     * 更新发票打印信息
     * POST /api/invoices/:id/print
     */
    public static void recordPrint(Context ctx) {
        try {
            String invoiceId = ctx.pathParam("id");
            PrintRequest request = ApiRequest.parse(ctx, PrintRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "请求体为空或字段不合法"));
                return;
            }
            
            // TD-039：不再接受客户端自报的 pdfPath/imagePath。这两个字段服务端从不使用（TD-001 守着），
            // 接受它们只会让任意认证用户往任何发票上写入伪造的文件路径；打印记录本身（状态/时间/次数）
            // 仍然照常更新。若将来要由服务端产出文件，路径必须由服务端计算并做白名单/前缀校验
            // （见 ExportUtil / BackupService），同批登记到 InvoicePathGuardPolicyTest.VALIDATED_READ_SITES。
            if (request.pdfPath != null || request.imagePath != null) {
                logger.warn("忽略发票打印记录里自报的文件路径（服务端不采纳客户端路径）: invoiceId={}", invoiceId);
            }
            DAOFactory.getInstance().getInvoiceDAO().updatePrintInfo(invoiceId, null, null);
            
            logger.info("发票打印记录: {}", invoiceId);
            ctx.json(Map.of("success", true, "message", "打印记录已更新"));
        } catch (Exception e) {
            logger.error("记录打印失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "记录打印失败"));
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
               .json(Map.of("success", false, "message", "获取销售方信息失败"));
        }
    }
    
    /**
     * 设置销售方默认信息
     * PUT /api/invoices/seller-info
     */
    public static void setSellerInfo(Context ctx) {
        try {
            Map<?, ?> info = ApiRequest.parse(ctx, Map.class);
            if (info == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "请求体为空或字段不合法"));
                return;
            }
            
            InvoiceService.setDefaultSellerInfo(
                getString(info, "name"),
                getString(info, "taxId"),
                getString(info, "address"),
                getString(info, "phone"),
                getString(info, "bank")
            );
            
            logger.info("销售方信息已更新");
            ctx.json(Map.of("success", true, "message", "销售方信息已更新"));
        } catch (Exception e) {
            logger.error("设置销售方信息失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "设置销售方信息失败"));
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
            
            LocalDate start = startDate != null ? LocalDate.parse(startDate, DateTimeFormats.DATE)
                                                  : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate, DateTimeFormats.DATE)
                                            : LocalDate.now();

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
                    default:
                        logger.debug("跳过未知发票状态统计: {}", i.status);
                        break;
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
            result.put("startDate", start.format(DateTimeFormats.DATE));
            result.put("endDate", end.format(DateTimeFormats.DATE));
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("发票统计失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "发票统计失败"));
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
        /** 已不再采纳（TD-039）：服务端不使用客户端提供的路径，仅保留字段以容忍旧客户端请求体。 */
        public String pdfPath;
        public String imagePath;
    }

    private static String getString(Map<?, ?> info, String key) {
        Object value = info.get(key);
        return value != null ? value.toString() : "";
    }
}
