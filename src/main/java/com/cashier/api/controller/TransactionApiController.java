package com.cashier.api.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.i18n.I18nManager;
import com.cashier.model.*;
import com.cashier.service.MemberService;
import com.cashier.service.ReturnService;
import com.cashier.service.TransactionService;
import com.cashier.util.DatabaseManager;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 交易收银 REST API
 */
public class TransactionApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TransactionApiController.class);
    private static final DateTimeFormatter ID_FORMATTER = com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME_MILLIS;
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    private static final int DEFAULT_TRANSACTION_LIST_LIMIT = 100;
    private static final int MAX_TRANSACTION_LIST_LIMIT = 500;
    
    /**
     * 获取交易列表
     * GET /api/transactions
     */
    public static void list(Context ctx) {
        try {
            String startDate = ctx.queryParam("startDate");
            String endDate = ctx.queryParam("endDate");
            String paymentMethod = ctx.queryParam("paymentMethod");
            int requestedLimit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(DEFAULT_TRANSACTION_LIST_LIMIT);
            int limit = Math.max(1, Math.min(requestedLimit, MAX_TRANSACTION_LIST_LIMIT));

            List<Transaction> transactions = hasDateFilter(startDate, endDate)
                ? DAOFactory.getInstance().getTransactionDAO().findByDateRange(toStartDateTime(startDate), toEndDateTime(endDate), limit)
                : DAOFactory.getInstance().getTransactionDAO().findRecent(limit);
            
            // 按条件筛选
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                transactions = filterTransactions(transactions, paymentMethod);
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
               .json(Map.of("success", false, "message", "获取交易列表失败"));
        }
    }
    
    /**
     * 获取单个交易
     * GET /api/transactions/:id
     */
    public static void get(Context ctx) {
        try {
            String transactionId = ctx.pathParam("id");
            Transaction transaction = DAOFactory.getInstance().getTransactionDAO().findById(transactionId);
            
            if (transaction == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "交易不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", transaction));
        } catch (Exception e) {
            logger.error("获取交易详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取交易详情失败"));
        }
    }
    
    /**
     * 创建新交易（收银）
     * POST /api/transactions
     */
    public static void create(Context ctx) {
        try {
            TransactionRequest request = ctx.bodyAsClass(TransactionRequest.class);
            if (request == null || request.items == null || request.items.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "交易明细不能为空"));
                return;
            }
            if (request.paymentMethod == null || request.paymentMethod.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "支付方式不能为空"));
                return;
            }

            // 明细只信任商品 ID 与数量：单价/小计/合计/税额一律按库中商品由服务端重算，
            // 避免客户端直接指定 finalAmount 少收款，也让库存与积分走与收银台相同的引擎。
            List<CartItem> cartItems = new ArrayList<>();
            Map<String, Product> inventory = new HashMap<>();
            for (Product requested : request.items) {
                if (requested == null || requested.id <= 0 || requested.quantity <= 0) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", "交易明细不合法（需要商品 ID 与正数数量）"));
                    return;
                }
                Product product = productDAO.findById(requested.id);
                if (product == null) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", "商品不存在: " + requested.id));
                    return;
                }
                inventory.put(product.name, product);
                cartItems.add(new CartItem(product, requested.quantity));
            }

            Member member = resolveMember(request);
            if (member == null && request.memberPhone != null && !request.memberPhone.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "会员不存在: " + request.memberPhone));
                return;
            }

            // 操作员一律取认证用户，忽略请求体中的自报身份（防止审计归属被伪造）
            User operator = ctx.attribute("currentUser");
            String transactionId = "T" + LocalDateTime.now().format(ID_FORMATTER);

            Transaction transaction = new Transaction();
            transaction.transactionId = transactionId;
            transaction.timestamp = LocalDateTime.now().format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
            transaction.items = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                transaction.items.add(saleLine(cartItem));
            }
            transaction.totalAmount = TransactionService.calculateTotalAmount(cartItems);
            Promotion promotion = TransactionService.selectBestPromotion(transaction.totalAmount);
            transaction.finalAmount = TransactionService.calculateFinalAmount(cartItems, member, promotion);
            // 税额按实付金额计（与两个收银台同口径）；价内税，不影响应付金额
            transaction.tax = TransactionService.calculateTax(transaction.finalAmount);
            transaction.paymentMethod = request.paymentMethod;
            if (member != null) {
                transaction.memberId = member.id;
                transaction.memberPhone = member.phone;
                transaction.memberName = member.name;
            }
            transaction.operatorUsername = operator != null ? operator.username : "";
            transaction.operatorName = operator != null ? operator.name : "";

            TransactionService.TransactionResult result = TransactionService.executeTransaction(
                cartItems, member, transaction, inventory, promotion);
            if (!result.isSuccess() || result.getTransaction() == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", result.getMessage()));
                return;
            }

            logger.info("创建交易: {} - 金额: {} - 支付方式: {} - 操作员: {}",
                transactionId, transaction.finalAmount, transaction.paymentMethod, transaction.operatorUsername);

            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", transaction, "transactionId", transactionId));
        } catch (Exception e) {
            logger.error("创建交易失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建交易失败"));
        }
    }

    /**
     * 构造交易明细行：数量取成交数量（库存数量由 {@code inventory} 单独维护）。
     */
    private static Product saleLine(CartItem cartItem) {
        Product product = cartItem.product;
        Product line = new Product();
        line.id = product.id;
        line.productCode = product.productCode;
        line.barcode = product.barcode;
        line.name = product.name;
        line.price = product.price;
        line.quantity = cartItem.quantity;
        line.category = product.category;
        line.unit = product.unit;
        line.cost = product.cost;
        return line;
    }

    private static Member resolveMember(TransactionRequest request) throws SQLException {
        if (request.memberId != null && request.memberId > 0) {
            return DAOFactory.getInstance().getMemberDAO().findById(request.memberId);
        }
        if (request.memberPhone != null && !request.memberPhone.isBlank()) {
            return DAOFactory.getInstance().getMemberDAO().findByPhone(request.memberPhone);
        }
        return null;
    }
    
    /**
     * 取消交易（退货）
     * POST /api/transactions/:id/refund
     */
    public static void refund(Context ctx) {
        String transactionId = ctx.pathParam("id");
        Transaction transaction;
        try {
            transaction = DAOFactory.getInstance().getTransactionDAO().findById(transactionId);
        } catch (SQLException e) {
            logger.error("读取交易失败: {}", transactionId, e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "交易退款失败"));
            return;
        }
        refundFetched(ctx, transactionId, transaction);
    }

    /**
     * 退款主体：入参 {@code transaction} 是调用方读取到的快照。
     *
     * <p>单独抽出来是为了能测试"读取快照之后、抢占之前被并发退款"的窗口——
     * 此时 {@code validateRefundRequest} 用的是陈旧快照、会放行，
     * 真正的拦截依赖事务内的原子抢占，必须返回 409 而不是 500。</p>
     */
    static void refundFetched(Context ctx, String transactionId, Transaction transaction) {
        try {
            if (!validateRefundRequest(ctx, transactionId, transaction)) {
                return;
            }

            // 执行退款事务
            boolean success = DatabaseManager.executeBooleanTransaction(
                conn -> processRefundTransaction(conn, transactionId, transaction));

            if (success) {
                respondRefundSuccess(ctx, transactionId, transaction);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .json(Map.of("success", false, "message", "退款处理失败"));
            }
        } catch (RefundConflictException e) {
            // 并发/重复退款：交易已被另一方抢占，属客户端可理解的冲突而非服务器错误
            logger.warn("退款冲突: {}", e.getMessage());
            ctx.status(HttpStatus.CONFLICT)
               .json(Map.of("success", false, "message", "该交易已退款"));
        } catch (Exception e) {
            logger.error("交易退款失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "交易退款失败"));
        }
    }

    /** 事务内原子抢占退款标记失败：说明同一交易已被并发/重复请求退过。 */
    private static final class RefundConflictException extends RuntimeException {
        RefundConflictException(String transactionId) {
            super("该交易已退款: " + transactionId);
        }
    }

    private static boolean validateRefundRequest(Context ctx, String transactionId, Transaction transaction) {
        if (transaction == null) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(Map.of("success", false, "message", "交易不存在"));
            return false;
        }
        if ("REFUNDED".equals(transaction.status)) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(Map.of("success", false, "message", "该交易已退款"));
            return false;
        }
        // 桌面退货流程不写 transactions.status，必须显式检查已有退货单，否则同一单会被退两次
        if (!DAOFactory.getInstance().getReturnOrderDAO()
                .findByOriginalTransactionId(transactionId).isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(Map.of("success", false, "message", "该交易已有退货记录，不能重复退款"));
            return false;
        }
        return true;
    }

    private static boolean processRefundTransaction(Connection conn, String transactionId, Transaction transaction)
            throws SQLException {
        try {
            // 原子抢占退款标记：并发/重复请求只有一次能成功，其余抛冲突（由上层映射为 409）
            if (!DAOFactory.getInstance().getTransactionDAO().claimRefundWithConnection(conn, transactionId)) {
                throw new RefundConflictException(transactionId);
            }
            Member member = resolveMemberByPhone(conn, transaction.memberPhone);
            ReturnOrder returnOrder = createRefundReturnOrder(conn, transactionId, transaction, member);
            if (!DAOFactory.getInstance().getReturnOrderDAO().insertWithConnection(conn, returnOrder)) {
                return false;
            }
            if (!createReturnItemsAndRestoreInventory(conn, transaction, returnOrder.returnOrderId)) {
                return false;
            }
            applyRefundToMember(conn, member, transaction, returnOrder);
            return true;
        } catch (SQLException e) {
            logger.error("退款事务执行失败", e);
            throw e;
        }
    }

    /**
     * 退款落会员账：非现金单退回余额 + 冲减本单积分 + 重算等级。
     *
     * <p>现金退款不退会员余额（否则顾客既拿现金又得多一笔可消费余额），
     * 只写 {@code RETURN_REFUND_CASH} 操作日志留痕，与
     * {@code ReturnService.completeReturnOrder} 的退款口径一致。</p>
     */
    private static void applyRefundToMember(Connection conn, Member member, Transaction transaction,
                                            ReturnOrder returnOrder) throws SQLException {
        if (member == null) {
            return;
        }

        BigDecimal refundAmount = returnOrder.getTotalAmount();
        // 积分冲减口径与桌面退货共用一个方法：每元 10 分，整单退货恰好冲掉原单积分
        BigDecimal earnedPoints = ReturnService.pointsToReverse(refundAmount, transaction.finalAmount);

        BigDecimal updatedPoints = member.getPoints().subtract(earnedPoints).max(BigDecimal.ZERO);
        String level = MemberService.calculateLevel(updatedPoints);

        boolean cashRefund = ReturnService.isCashPaymentMethod(returnOrder.paymentMethod);
        if (!cashRefund) {
            member.balance = member.getBalance().add(refundAmount);
        }
        member.points = updatedPoints;
        member.level = level;
        member.discount = MemberService.getDiscountByLevelDecimal(level);
        member.discountRate = member.discount;

        if (!DAOFactory.getInstance().getMemberDAO().updateWithConnection(conn, member)) {
            throw new SQLException(I18nManager.getInstance().get("service.member_update_failed"));
        }

        if (cashRefund) {
            OperationLog log = new OperationLog();
            log.username = returnOrder.operatorName;
            log.operation = "RETURN_REFUND_CASH";
            log.details = String.format("现金退款: %s, 金额: %.2f (原交易 %s)",
                returnOrder.returnOrderId, refundAmount, returnOrder.originalTransactionId);
            log.ipAddress = "api";
            log.timestamp = java.time.Instant.now();
            log.category = "REFUND";
            log.result = "SUCCESS";
            log.affectedRecords = 1;
            DAOFactory.getInstance().getOperationLogDAO().insertWithConnection(conn, log);
            return;
        }

        RechargeRecord record = new RechargeRecord();
        record.memberPhone = member.phone;
        record.memberName = member.name;
        record.amount = refundAmount;
        record.paymentMethod = returnOrder.paymentMethod;
        record.operator = returnOrder.operatorName;
        record.timestamp = new Date();
        record.recordId = returnOrder.returnOrderId;
        if (!DAOFactory.getInstance().getRechargeRecordDAO().insertWithConnection(conn, record)) {
            throw new SQLException(I18nManager.getInstance().get("service.return_refund_record_insert_failed"));
        }
    }

    private static Member resolveMemberByPhone(Connection conn, String memberPhone) throws SQLException {
        if (memberPhone == null || memberPhone.isBlank()) {
            return null;
        }
        return DAOFactory.getInstance().getMemberDAO().findByPhoneWithConnection(conn, memberPhone);
    }

    private static ReturnOrder createRefundReturnOrder(Connection conn, String transactionId, Transaction transaction,
                                                       Member member) throws SQLException {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.originalTransactionId = transactionId;
        returnOrder.memberId = member != null ? member.id : null;
        returnOrder.memberName = member != null ? member.name : transaction.memberName;
        returnOrder.totalAmount = transaction.finalAmount != null ? transaction.finalAmount : BigDecimal.ZERO;
        returnOrder.returnReason = "API退款";
        returnOrder.paymentMethod = mapPaymentMethodToRefund(transaction.paymentMethod);
        // return_orders.operator_name 非空；历史交易可能没有操作员（空名会被存成 NULL）
        returnOrder.operatorName = transaction.operatorName != null && !transaction.operatorName.isBlank()
            ? transaction.operatorName : "system";
        returnOrder.status = "COMPLETED"; // 直接完成，无需审批
        returnOrder.returnOrderId = DAOFactory.getInstance().getReturnOrderDAO().generateNextReturnOrderId(conn);
        return returnOrder;
    }

    private static boolean createReturnItemsAndRestoreInventory(
            Connection conn, Transaction transaction, String returnOrderId) throws SQLException {
        if (transaction.items == null || transaction.items.isEmpty()) {
            return true;
        }

        // 明细只存原价，退款单价同样按整单实付比例折算，明细金额之和才等于实际退款额
        BigDecimal gross = BigDecimal.ZERO;
        for (Product product : transaction.items) {
            BigDecimal price = product.price != null ? product.price : BigDecimal.ZERO;
            gross = gross.add(price.multiply(BigDecimal.valueOf(product.quantity)));
        }

        List<ReturnOrderItem> returnItems = new ArrayList<>();
        for (Product product : transaction.items) {
            returnItems.add(createReturnOrderItem(returnOrderId, product, transaction.finalAmount, gross));
            productDAO.updateQuantityWithConnection(conn, product.id, product.quantity);
        }
        return DAOFactory.getInstance().getReturnOrderItemDAO().batchInsertWithConnection(conn, returnItems);
    }

    private static ReturnOrderItem createReturnOrderItem(String returnOrderId, Product product,
                                                         BigDecimal paidAmount, BigDecimal grossAmount) {
        ReturnOrderItem item = new ReturnOrderItem();
        item.returnOrderId = returnOrderId;
        item.productId = product.id;
        item.productCode = product.productCode;
        item.productName = product.name;
        item.barcode = product.barcode;
        item.category = product.category;
        item.returnQuantity = product.quantity;
        item.unitPrice = ReturnService.refundUnitPrice(product.price, paidAmount, grossAmount);
        item.returnAmount = item.unitPrice.multiply(BigDecimal.valueOf(item.returnQuantity));
        item.condition = "GOOD";
        return item;
    }

    private static void respondRefundSuccess(Context ctx, String transactionId, Transaction transaction) {
        logger.info("交易退款成功: {} - 金额: {}", transactionId, transaction.finalAmount);

        // 广播交易退款事件
        com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
            com.cashier.api.sync.SyncEventType.TRANSACTION_REFUNDED,
            Map.of(
                "transactionId", transactionId,
                "refundAmount", transaction.finalAmount.toString(),
                "timestamp", LocalDateTime.now().format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME)
            )
        );

        ctx.json(Map.of("success", true, "message", "退款成功",
            "transactionId", transactionId,
            "refundAmount", transaction.finalAmount));
    }
    
    /**
     * 映射支付方式到退款方式
     */
    private static String mapPaymentMethodToRefund(String paymentMethod) {
        String canonical = com.cashier.util.I18nUiUtils.canonicalPaymentMethod(paymentMethod);
        if (canonical == null) return "CASH";
        // 非现金渠道按代码原样落库，退款时才能正确区分是否退会员余额；
        // 会员余额支付退款也必须退余额（不能落成 CASH）
        return switch (canonical) {
            case "WECHAT", "ALIPAY", "CARD", "MEMBER_BALANCE" -> canonical;
            default -> "CASH";
        };
    }
    
    /**
     * 今日交易统计
     * GET /api/transactions/today
     */
    public static void todayStats(Context ctx) {
        try {
            String today = LocalDateTime.now().format(com.cashier.util.DateTimeFormats.DATE);
            List<Transaction> transactions = DAOFactory.getInstance().getTransactionDAO().findByDateRange(
                today + " 00:00:00",
                today + " 23:59:59"
            );
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (Transaction t : transactions) {
                if (t.finalAmount != null) {
                    totalAmount = totalAmount.add(t.finalAmount);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("date", today);
            result.put("count", transactions.size());
            result.put("totalAmount", totalAmount);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取今日交易统计失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取今日交易统计失败"));
        }
    }
    
    /**
     * 筛选交易。
     *
     * <p>支付方式在库里既有本地化文案（「现金」）也有代码（{@code CASH}），
     * 必须先归一化再比较，否则按代码筛选永远为空、按中文又漏掉代码形式的记录。</p>
     */
    private static List<Transaction> filterTransactions(List<Transaction> transactions, String paymentMethod) {
        String target = com.cashier.util.I18nUiUtils.canonicalPaymentMethod(paymentMethod);
        List<Transaction> result = new ArrayList<>();
        
        for (Transaction t : transactions) {
            if (t.paymentMethod == null || target == null) {
                continue;
            }
            if (target.equals(com.cashier.util.I18nUiUtils.canonicalPaymentMethod(t.paymentMethod))) {
                result.add(t);
            }
        }
        
        return result;
    }

    private static boolean hasDateFilter(String startDate, String endDate) {
        return startDate != null && !startDate.isBlank()
            || endDate != null && !endDate.isBlank();
    }

    private static String toStartDateTime(String startDate) {
        String date = startDate == null || startDate.isBlank()
            ? LocalDate.now().minusDays(30).format(com.cashier.util.DateTimeFormats.DATE)
            : startDate;
        return date.length() == 10 ? date + " 00:00:00" : date;
    }

    private static String toEndDateTime(String endDate) {
        String date = endDate == null || endDate.isBlank()
            ? LocalDate.now().format(com.cashier.util.DateTimeFormats.DATE)
            : endDate;
        return date.length() == 10 ? date + " 23:59:59" : date;
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
