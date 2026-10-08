package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.i18n.I18nManager;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 退货管理服务类
 */
public class ReturnService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnService.class);
    private static final com.cashier.dao.ProductDAORefactored productDAO = com.cashier.dao.DAOFactory.getInstance().getProductDAO();

    /**
     * 退款单价折算比例 = 整单实付金额 / 整单原价合计。
     *
     * <p>会员折扣与促销都作用在整单上、交易明细只存原价，因此退货必须按该比例折算退款单价，
     * 否则打折成交的商品全额退货会把优惠部分一并退给顾客（9.5 折买 100 元退 100 元）。</p>
     *
     * @param paidAmount 整单实付金额（transactions.final_amount）
     * @param grossAmount 整单原价合计
     * @return 比例，落在 [0, 1]；原价合计无效时返回 1（不折算）
     */
    public static BigDecimal refundRatio(BigDecimal paidAmount, BigDecimal grossAmount) {
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal paid = paidAmount != null ? paidAmount : grossAmount;
        return paid.divide(grossAmount, 4, RoundingMode.HALF_UP)
            .min(BigDecimal.ONE)
            .max(BigDecimal.ZERO);
    }

    /**
     * 计算退货退款单价：按整单实付比例折算后保留 2 位小数。
     *
     * @param listPrice 原价（交易明细中的单价）
     * @param paidAmount 整单实付金额
     * @param grossAmount 整单原价合计
     * @return 退款单价
     */
    public static BigDecimal refundUnitPrice(BigDecimal listPrice, BigDecimal paidAmount, BigDecimal grossAmount) {
        BigDecimal price = listPrice != null ? listPrice : BigDecimal.ZERO;
        return price.multiply(refundRatio(paidAmount, grossAmount)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按整单实付比例计算**应退总额**——退货单金额的权威来源。
     *
     * <p>不能拿"每行单价四舍五入后再乘数量"求和：`return_order_items.unit_price` 是
     * {@code DECIMAL(10,2)}，2 × 10.10、9.5 折时每件 9.595 只能存 9.60，行金额 19.20，
     * 而整单实付是 19.19——按行求和会**多退 1 分**（TD-034；实测 6.7% 的金额组合会偏）。
     * 退款金额以本方法为准，明细行只是分解。</p>
     *
     * @param paidAmount          整单实付
     * @param grossAmount         整单原价合计
     * @param returnedGrossAmount 本次退货部分（选中且退货数量 &gt; 0）的原价合计
     * @return 应退金额，2 位小数，落在 [0, paidAmount]
     */
    public static BigDecimal refundTotal(BigDecimal paidAmount, BigDecimal grossAmount,
                                         BigDecimal returnedGrossAmount) {
        BigDecimal returned = returnedGrossAmount != null ? returnedGrossAmount : BigDecimal.ZERO;
        if (returned.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal paid = (paidAmount != null ? paidAmount : returned).setScale(2, RoundingMode.HALF_UP);
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return returned.setScale(2, RoundingMode.HALF_UP).min(paid);
        }
        return paid.multiply(returned)
            .divide(grossAmount, 2, RoundingMode.HALF_UP)
            .min(paid)
            .max(BigDecimal.ZERO);
    }

    /**
     * 把明细行的金额之和**压到不超过**应退总额。
     *
     * <p>单价只有 2 位小数，逐行"单价×数量"最多比精确分摊多几分；退款已按
     * {@link #refundTotal} 生效，明细只允许少算、不允许多算，否则账面明细之和会大于实际退款额。
     * 逐分从当前金额最大的行往下调（每轮至少减 1 分，循环次数有上限）。</p>
     */
    public static void alignItemsToRefundTotal(List<ReturnOrderItem> items, BigDecimal refundTotal) {
        if (items == null || items.isEmpty() || refundTotal == null) {
            return;
        }
        BigDecimal sum = items.stream()
            .map(ReturnOrderItem::getReturnAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int guard = 0;
        while (sum.compareTo(refundTotal) > 0 && guard++ < 10_000) {
            ReturnOrderItem largest = null;
            for (ReturnOrderItem item : items) {
                if (item.returnQuantity <= 0 || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if (largest == null || item.getReturnAmount().compareTo(largest.getReturnAmount()) > 0) {
                    largest = item;
                }
            }
            if (largest == null) {
                return;
            }
            largest.unitPrice = largest.getUnitPrice().subtract(new BigDecimal("0.01"));
            largest.calculateAmount();
            sum = items.stream()
                .map(ReturnOrderItem::getReturnAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * 创建退货订单（事务）。
     *
     * <p><b>F10</b>：建单校验（累计退货量 ≤ 原销量）不再是"事务外 check-then-act"。
     * 事务内先 {@code SELECT ... FOR UPDATE} 锁住原交易行使同一交易的建单串行化，
     * 再用占用台账（{@code return_reservations}）重算可退余量；通过后与退货单、明细、
     * 台账一起提交。这样两个终端同时提交时只有一个能成功，另一个会被余量校验拦下。</p>
     *
     * <p>退货单号由 MAX+1 生成，多进程并发创建时可能撞唯一键；失败时整体重试一次
     * （事务只做纯插入，失败已回滚，重放安全），提升并发场景成功率。</p>
     */
    public static boolean createReturnOrder(ReturnOrder returnOrder, List<ReturnOrderItem> items) {
        if (returnOrder == null) {
            return false;
        }
        final Map<Integer, Integer> requested = aggregateRequestedQuantities(items);
        final int maxAttempts = 2;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                    // 行锁 + 余量校验必须与插入同事务：事务外的校验挡不住并发建单
                    if (!lockAndValidateReturnable(conn, returnOrder.originalTransactionId, requested)) {
                        return false;
                    }

                    returnOrder.returnOrderId = DAOFactory.getInstance().getReturnOrderDAO().generateNextReturnOrderId(conn);
                    returnOrder.status = "PENDING";

                    if (items != null && !items.isEmpty()) {
                        for (ReturnOrderItem item : items) {
                            item.returnOrderId = returnOrder.returnOrderId;
                        }
                    }

                    if (!DAOFactory.getInstance().getReturnOrderDAO().insertWithConnection(conn, returnOrder)) {
                        return false;
                    }

                    if (items != null && !items.isEmpty()
                        && !DAOFactory.getInstance().getReturnOrderItemDAO().batchInsertWithConnection(conn, items)) {
                        return false;
                    }

                    return insertReservations(conn, returnOrder, requested);
                });

                if (success) {
                    logger.info("退货订单创建成功: {}", returnOrder.returnOrderId);
                    // 广播退货单创建事件
                    com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
                        com.cashier.api.sync.SyncEventType.RETURN_ORDER_CREATED,
                        java.util.Map.of(
                            "returnOrderId", returnOrder.returnOrderId,
                            "totalAmount", returnOrder.totalAmount.toString()
                        )
                    );
                    return true;
                }

                if (attempt < maxAttempts) {
                    logger.warn("退货订单创建失败（第 {} 次），将重试一次", attempt);
                }
            } catch (SQLException e) {
                if (attempt >= maxAttempts) {
                    logger.error("创建退货订单失败", e);
                    return false;
                }
                logger.warn("创建退货订单异常（第 {} 次），将重试一次: {}", attempt, e.getMessage());
            }
        }
        logger.error("创建退货订单失败，已达最大重试次数: {}", maxAttempts);
        return false;
    }

    /** 本次退货按商品跨行合计的数量（退货明细可能把同一商品拆成多行）。 */
    private static Map<Integer, Integer> aggregateRequestedQuantities(List<ReturnOrderItem> items) {
        Map<Integer, Integer> requested = new java.util.LinkedHashMap<>();
        if (items == null) {
            return requested;
        }
        for (ReturnOrderItem item : items) {
            if (item != null && item.returnQuantity > 0) {
                requested.merge(item.productId, item.returnQuantity, Integer::sum);
            }
        }
        return requested;
    }

    /**
     * 锁原交易行并校验可退余量（F10）。必须在建单事务内调用。
     *
     * <p>可退余量 = 原单该商品数量（跨行合计）− 台账占用合计（排除已驳回退货单）。</p>
     *
     * <p>两种数据缺失时**放行但仍记台账**，避免把老数据挡死：① 原交易行不存在（历史/测试数据）；
     * ② 原交易没有 {@code transaction_items} 明细（无从得知基数）。真实结账一定写明细，
     * 因此这条兜底只影响无法校验的脏数据，且会留 WARN。</p>
     *
     * @return 允许建单
     */
    private static boolean lockAndValidateReturnable(Connection conn, String transactionId,
                                                     Map<Integer, Integer> requested) throws SQLException {
        if (transactionId == null || transactionId.isBlank()) {
            logger.warn("退货建单缺少原交易号，拒绝创建");
            return false;
        }
        if (requested.isEmpty()) {
            return true;
        }
        if (!DAOFactory.getInstance().getTransactionDAO().lockForReturnWithConnection(conn, transactionId)) {
            logger.warn("退货建单的原交易不存在，拒绝创建: transactionId={}", transactionId);
            return false;
        }
        Map<Integer, Integer> original = DAOFactory.getInstance().getTransactionDAO()
            .sumItemQuantitiesByProductWithConnection(conn, transactionId);
        if (original.isEmpty()) {
            logger.warn("原交易无明细，跳过可退量校验（仅记台账）: transactionId={}", transactionId);
            return true;
        }
        Map<Integer, Integer> reserved = DAOFactory.getInstance().getReturnReservationDAO()
            .sumReservedQuantitiesWithConnection(conn, transactionId);
        for (Map.Entry<Integer, Integer> entry : requested.entrySet()) {
            int productId = entry.getKey();
            int available = original.getOrDefault(productId, 0);
            int alreadyReserved = reserved.getOrDefault(productId, 0);
            if (alreadyReserved + entry.getValue() > available) {
                logger.warn("退货数量超出可退余量，拒绝创建: transactionId={}, productId={}, 已占用={}, 本次={}, 原单={}",
                    transactionId, productId, alreadyReserved, entry.getValue(), available);
                // 文案在服务层生成：并发输掉的那次建单也必须给出"超出可退余量"而不是通用的创建失败
                throw new ReturnQuantityExceededException(I18nManager.getInstance().get(
                    "runtime.return_quantity_exceeded", productNameWithConnection(conn, productId),
                    available, alreadyReserved, entry.getValue()));
            }
        }
        return true;
    }

    /** 商品名（用于把超量原因说清楚）；历史/脏数据的商品可能查不到，退化成 #id。 */
    private static String productNameWithConnection(Connection conn, int productId) {
        try {
            Product product = productDAO.findByIdWithConnection(conn, productId);
            if (product != null && product.name != null && !product.name.isBlank()) {
                return product.name;
            }
        } catch (SQLException e) {
            logger.warn("查询商品名失败（用于退货超量提示）: productId={}", productId, e);
        }
        return "#" + productId;
    }

    /**
     * 可退余量不足（并发建单、超量退货）。
     *
     * <p>文案由服务层用 i18n 组装好（{@code runtime.return_quantity_exceeded}），
     * 调用方直接展示即可——避免每个界面各自拼一套文案。</p>
     */
    public static class ReturnQuantityExceededException extends RuntimeException {
        public ReturnQuantityExceededException(String message) {
            super(message);
        }
    }

    /** 把本次退货的占用写进台账（与退货单同事务；F10-b 会在此基础上同步行状态）。 */
    private static boolean insertReservations(Connection conn, ReturnOrder returnOrder,
                                              Map<Integer, Integer> requested) throws SQLException {
        if (requested.isEmpty()) {
            return true;
        }
        java.util.List<ReturnReservation> reservations = new java.util.ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : requested.entrySet()) {
            ReturnReservation reservation = new ReturnReservation();
            reservation.returnOrderId = returnOrder.returnOrderId;
            reservation.originalTransactionId = returnOrder.originalTransactionId;
            reservation.productId = entry.getKey();
            reservation.quantity = entry.getValue();
            reservation.status = ReturnReservation.STATUS_PENDING;
            reservations.add(reservation);
        }
        return DAOFactory.getInstance().getReturnReservationDAO().batchInsertWithConnection(conn, reservations);
    }

    /**
     * 同步台账行状态（F10-b）；0 行只记日志——台账上线前建的退货单、或 API 直接完成的整单
     * 可能没有台账行，不当成失败。
     */
    private static void syncReservationStatus(Connection conn, String returnOrderId, String status)
            throws SQLException {
        int updated = DAOFactory.getInstance().getReturnReservationDAO()
            .updateStatusForReturnOrderWithConnection(conn, returnOrderId, status);
        if (updated == 0) {
            logger.info("退货单 {} 没有台账行可同步为 {}（旧单或 API 整单退款）", returnOrderId, status);
        }
    }

    /**
     * 审批退货订单（事务）
     */
    public static boolean approveReturnOrder(String returnOrderId, String approverName,
                                              String approvalComment, boolean approved) {
        return approveReturnOrder(returnOrderId, approverName, approvalComment, approved, null);
    }

    /**
     * 审批退货订单（事务），并落库审批人选择的退款方式。
     *
     * <p>{@code completeReturnOrder → settleRefund} 是按 {@code return_orders.payment_method}
     * 决定"退现金还是退回会员余额"的，而审批界面上的"退款方式"下拉框此前只被读进一个
     * 局部变量就丢弃，于是审批人改成现金也照样退回会员余额（反之亦然）。这里把选择
     * 与状态迁移放在同一条 UPDATE 里，保证原子。</p>
     *
     * @param refundPaymentMethod 审批人选择的退款方式（中文/代码/英文文案均可）；
     *                            为 null/空白时沿用建单时的值；无法识别时审批失败（不写脏值）
     */
    public static boolean approveReturnOrder(String returnOrderId, String approverName,
                                              String approvalComment, boolean approved,
                                              String refundPaymentMethod) {
        String paymentMethod = normalizeRefundPaymentMethod(refundPaymentMethod);
        if (refundPaymentMethod != null && !refundPaymentMethod.isBlank() && paymentMethod == null) {
            logger.warn("审批退货单时收到无法识别的退款方式，已拒绝: {} -> {}", returnOrderId, refundPaymentMethod);
            return false;
        }
        try {
            boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                ReturnOrder returnOrder = DAOFactory.getInstance().getReturnOrderDAO().findByReturnOrderIdWithConnection(conn, returnOrderId);
                if (returnOrder == null) {
                    return false;
                }

                // 原子状态迁移（PENDING -> APPROVED/REJECTED），防止并发重复审批重复恢复库存；
                // 0 行受影响说明该单已被他人处理，直接失败回滚。
                String newStatus = approved ? "APPROVED" : "REJECTED";
                if (!DAOFactory.getInstance().getReturnOrderDAO().markApprovalWithConnection(
                        conn, returnOrderId, newStatus, approverName, approvalComment, paymentMethod)) {
                    logger.warn("退货单状态已变更，审批冲突: {}", returnOrderId);
                    return false;
                }

                // F10-b：台账行状态与单据状态同事务同步（占用与否本就按父单状态判定，这里为审计可视化）
                syncReservationStatus(conn, returnOrderId,
                    approved ? ReturnReservation.STATUS_APPROVED : ReturnReservation.STATUS_REJECTED);

                if (!approved) {
                    return true;
                }

                List<ReturnOrderItem> items = DAOFactory.getInstance().getReturnOrderItemDAO().findByReturnOrderIdWithConnection(conn, returnOrderId);
                for (ReturnOrderItem item : items) {
                    Product product = productDAO.findByIdWithConnection(conn, item.productId);
                    if (product == null) {
                        throw new SQLException(I18nManager.getInstance().get("service.return_product_not_found", item.productId));
                    }

                    product.quantity += item.returnQuantity;
                    if (!productDAO.updateWithVersionWithConnection(conn, product)) {
                        throw new SQLException(I18nManager.getInstance().get("service.return_stock_restore_failed", item.productId));
                    }
                }

                OperationLog log = new OperationLog();
                log.username = approverName;
                log.operation = "RETURN_APPROVAL";
                log.details = String.format(java.util.Locale.ROOT, "审批退货单: %s, 金额: %.2f",
                    returnOrderId, returnOrder.totalAmount);
                log.ipAddress = "localhost";
                log.timestamp = java.time.Instant.now();
                log.category = "REFUND";
                log.operation = "RETURN_APPROVAL";
                log.result = "SUCCESS";
                log.affectedRecords = items.size();
                return DAOFactory.getInstance().getOperationLogDAO().insertWithConnection(conn, log);
            });

            if (success) {
                logger.info("退货订单审批成功: {}, 结果: {}", returnOrderId, approved ? "通过" : "拒绝");
            }
            return success;
        } catch (SQLException e) {
            logger.error("审批退货订单失败", e);
            return false;
        }
    }

    /**
     * 退货完成时的退款落账。
     *
     * <p><b>现金单退现金</b>：只写一条退款操作日志留痕，不冲会员余额（现金是从钱箱退给顾客的，
     * 记进余额会让会员凭空多出一笔可再次消费的钱）。非现金单退回会员余额并写充值流水。</p>
     *
     * <p>两种情况都按"退货金额占原单实付的比例"冲减原单产生的积分，等级与折扣随之重算。</p>
     */
    private static void settleRefund(Connection conn, ReturnOrder returnOrder, String returnOrderId)
            throws SQLException {
        boolean hasMember = returnOrder.memberId != null && returnOrder.memberId > 0;
        Member member = hasMember
            ? DAOFactory.getInstance().getMemberDAO().findByIdWithConnection(conn, returnOrder.memberId)
            : null;
        if (hasMember && member == null) {
            throw new SQLException(I18nManager.getInstance().get("service.return_member_not_found", returnOrder.memberId));
        }

        BigDecimal refundAmount = returnOrder.getTotalAmount();
        boolean cashRefund = isCashPaymentMethod(returnOrder.paymentMethod);

        if (member != null) {
            if (!cashRefund) {
                member.balance = member.getBalance().add(refundAmount);
            }
            BigDecimal reversal = pointsToReverse(refundAmount, originalPaidAmount(returnOrder));
            if (reversal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal updatedPoints = member.getPoints().subtract(reversal).max(BigDecimal.ZERO);
                String level = MemberService.calculateLevel(updatedPoints);
                member.points = updatedPoints;
                member.level = level;
                member.discount = MemberService.getDiscountByLevelDecimal(level);
                member.discountRate = member.discount;
            }
            if (!DAOFactory.getInstance().getMemberDAO().updateWithConnection(conn, member)) {
                throw new SQLException(I18nManager.getInstance().get("service.return_member_balance_update_failed", member.id));
            }
        }

        if (cashRefund) {
            OperationLog log = new OperationLog();
            log.username = returnOrder.operatorName;
            log.operation = "RETURN_REFUND_CASH";
            log.details = String.format(java.util.Locale.ROOT, "现金退款: %s, 金额: %.2f (原交易 %s)",
                returnOrderId, refundAmount, returnOrder.originalTransactionId);
            log.ipAddress = "localhost";
            log.timestamp = java.time.Instant.now();
            log.category = "REFUND";
            log.result = "SUCCESS";
            log.affectedRecords = 1;
            DAOFactory.getInstance().getOperationLogDAO().insertWithConnection(conn, log);
            return;
        }

        if (member != null) {
            RechargeRecord record = new RechargeRecord();
            record.memberPhone = member.phone;
            record.memberName = member.name;
            record.amount = refundAmount;
            record.paymentMethod = returnOrder.paymentMethod;
            record.operator = returnOrder.operatorName;
            record.timestamp = new Date();
            record.recordId = returnOrderId;
            if (!DAOFactory.getInstance().getRechargeRecordDAO().insertWithConnection(conn, record)) {
                throw new SQLException(I18nManager.getInstance().get("service.return_refund_record_insert_failed"));
            }
        }
    }

    /** 退款方式是否为现金（兼容 POS 落库的中文与接口写入的代码形式）。 */
    public static boolean isCashPaymentMethod(String paymentMethod) {
        return "CASH".equals(com.cashier.util.I18nUiUtils.canonicalPaymentMethod(paymentMethod));
    }

    /**
     * 归一化审批人选择的退款方式为落库用的规范代码；空白返回 null（表示不改），无法识别也返回 null。
     */
    private static String normalizeRefundPaymentMethod(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String canonical = com.cashier.util.I18nUiUtils.canonicalPaymentMethod(value.trim());
        return switch (canonical == null ? "" : canonical) {
            case "CASH", "WECHAT", "ALIPAY", "CARD", "MEMBER_BALANCE" -> canonical;
            default -> null;
        };
    }

    private static BigDecimal originalPaidAmount(ReturnOrder returnOrder) throws SQLException {
        if (returnOrder.originalTransactionId == null || returnOrder.originalTransactionId.isBlank()) {
            return null;
        }
        Transaction transaction = DAOFactory.getInstance().getTransactionDAO()
            .findById(returnOrder.originalTransactionId);
        return transaction != null ? transaction.finalAmount : null;
    }

    /**
     * 退货应冲减的积分：按退货金额占原单实付的比例冲减原单产生的积分。
     *
     * <p>积分累计口径与 {@code TransactionService} 一致（每元 10 分，向下取整），
     * 因此整单退货恰好冲掉原单积分，部分退货按比例冲减。</p>
     *
     * <p>这里必须用 {@code FLOOR} 而不是 {@code HALF_UP}：本方法按**每次退货**调用，
     * 四舍五入会让多次部分退货累计冲减超过原单得分。实测原单实付 10.10（得 101 分），
     * 分两次各退 5.05 时 101×0.5=50.5，HALF_UP 每次 51、合计 102 &gt; 101（TD-034）。
     * 向下取整则每次最多少冲 1 分，宁可少扣也不扣掉会员从未得到的积分。</p>
     *
     * @param refundAmount 本次退货金额
     * @param originalPaidAmount 原单实付金额
     * @return 应冲减的积分；参数缺失或非法时返回 0
     */
    public static BigDecimal pointsToReverse(BigDecimal refundAmount, BigDecimal originalPaidAmount) {
        if (refundAmount == null || originalPaidAmount == null
                || refundAmount.compareTo(BigDecimal.ZERO) <= 0
                || originalPaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal earnedPoints = originalPaidAmount.multiply(BigDecimal.TEN).setScale(0, RoundingMode.FLOOR);
        BigDecimal ratio = refundAmount.divide(originalPaidAmount, 6, RoundingMode.HALF_UP).min(BigDecimal.ONE);
        return earnedPoints.multiply(ratio).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 完成退货订单（事务）
     */
    public static boolean completeReturnOrder(String returnOrderId) {
        try {
            boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                ReturnOrder returnOrder = DAOFactory.getInstance().getReturnOrderDAO().findByReturnOrderIdWithConnection(conn, returnOrderId);
                if (returnOrder == null) {
                    return false;
                }

                if (!"APPROVED".equals(returnOrder.status)) {
                    return false;
                }

                // 原子状态迁移（APPROVED -> COMPLETED），防止并发重复完成导致重复退款/重复写流水；
                // 0 行受影响说明该单已被他人完成，直接失败回滚。
                if (!DAOFactory.getInstance().getReturnOrderDAO().markCompletedWithConnection(conn, returnOrderId)) {
                    logger.warn("退货单已被处理，完成冲突: {}", returnOrderId);
                    return false;
                }

                // F10-b：台账行同步为已完成（同事务）
                syncReservationStatus(conn, returnOrderId, ReturnReservation.STATUS_COMPLETED);

                settleRefund(conn, returnOrder, returnOrderId);

                // 广播退货完成（退款）事件（无会员的退货此前会被提前 return 而漏播）
                com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
                    com.cashier.api.sync.SyncEventType.TRANSACTION_REFUNDED,
                    java.util.Map.of(
                        "returnOrderId", returnOrderId,
                        "transactionId", returnOrder.originalTransactionId != null ? returnOrder.originalTransactionId : "",
                        "refundAmount", returnOrder.getTotalAmount().toString()
                    )
                );

                return true;
            });

            if (success) {
                logger.info("退货订单完成成功: {}", returnOrderId);
            }
            return success;
        } catch (SQLException e) {
            logger.error("完成退货订单失败", e);
            return false;
        }
    }

    /**
     * 获取待审批的退货订单列表
     */
    public static List<ReturnOrder> getPendingReturnOrders() {
        return DAOFactory.getInstance().getReturnOrderDAO().findByStatus("PENDING");
    }

    /**
     * 获取已批准的退货订单列表
     */
    public static List<ReturnOrder> getApprovedReturnOrders() {
        return DAOFactory.getInstance().getReturnOrderDAO().findByStatus("APPROVED");
    }

    /**
     * 获取已完成的退货订单列表
     */
    public static List<ReturnOrder> getCompletedReturnOrders() {
        return DAOFactory.getInstance().getReturnOrderDAO().findByStatus("COMPLETED");
    }

    /**
     * 获取会员的退货订单列表
     */
    public static List<ReturnOrder> getMemberReturnOrders(int memberId) {
        return DAOFactory.getInstance().getReturnOrderDAO().findByMemberId(memberId);
    }

    /**
     * 计算退货统计
     */
    public static ReturnStatistics calculateReturnStatistics(Date startDate, Date endDate) {
        ReturnStatistics stats = new ReturnStatistics();

        Map<String, Object> data = DAOFactory.getInstance().getReturnOrderDAO().getStatistics(startDate, endDate);

        stats.totalReturnOrders = (int) data.get("total_return_orders");
        stats.totalReturnAmount = (BigDecimal) data.get("total_return_amount");
        stats.approvedOrders = (int) data.get("approved_orders");
        stats.rejectedOrders = (int) data.get("rejected_orders");
        stats.completedOrders = (int) data.get("completed_orders");

        return stats;
    }

    /**
     * 退货统计类
     */
    public static class ReturnStatistics {
        public int totalReturnOrders;
        public BigDecimal totalReturnAmount;
        public int approvedOrders;
        public int rejectedOrders;
        public int completedOrders;
    }
}
