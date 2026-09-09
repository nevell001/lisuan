package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.i18n.I18nManager;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
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
     * 创建退货订单（事务）
     *
     * <p>退货单号由 MAX+1 生成，多进程并发创建时可能撞唯一键；失败时整体重试一次
     * （事务只做纯插入，失败已回滚，重放安全），提升并发场景成功率。</p>
     */
    public static boolean createReturnOrder(ReturnOrder returnOrder, List<ReturnOrderItem> items) {
        final int maxAttempts = 2;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
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

                    return items == null || items.isEmpty()
                        || DAOFactory.getInstance().getReturnOrderItemDAO().batchInsertWithConnection(conn, items);
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

    /**
     * 审批退货订单（事务）
     */
    public static boolean approveReturnOrder(String returnOrderId, String approverName,
                                              String approvalComment, boolean approved) {
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
                        conn, returnOrderId, newStatus, approverName, approvalComment)) {
                    logger.warn("退货单状态已变更，审批冲突: {}", returnOrderId);
                    return false;
                }

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
                log.details = String.format("审批退货单: %s, 金额: %.2f",
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

                if (returnOrder.memberId == null || returnOrder.memberId <= 0) {
                    return true;
                }

                Member member = DAOFactory.getInstance().getMemberDAO().findByIdWithConnection(conn, returnOrder.memberId);
                if (member == null) {
                    throw new SQLException(I18nManager.getInstance().get("service.return_member_not_found", returnOrder.memberId));
                }

                member.balance = member.getBalance().add(returnOrder.getTotalAmount());
                if (!DAOFactory.getInstance().getMemberDAO().updateWithConnection(conn, member)) {
                    throw new SQLException(I18nManager.getInstance().get("service.return_member_balance_update_failed", returnOrder.memberId));
                }

                RechargeRecord record = new RechargeRecord();
                record.memberPhone = member.phone;
                record.memberName = member.name;
                record.amount = returnOrder.getTotalAmount();
                record.paymentMethod = returnOrder.paymentMethod;
                record.operator = returnOrder.operatorName;
                record.timestamp = new Date();
                record.recordId = returnOrderId;
                if (!DAOFactory.getInstance().getRechargeRecordDAO().insertWithConnection(conn, record)) {
                    throw new SQLException(I18nManager.getInstance().get("service.return_refund_record_insert_failed"));
                }
                
                // 广播退货完成（退款）事件
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
