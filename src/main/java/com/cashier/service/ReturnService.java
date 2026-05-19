package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * 退货管理服务类
 */
public class ReturnService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnService.class);
    private static final com.cashier.dao.ProductDAORefactored productDAO = com.cashier.dao.DAOFactory.getInstance().getProductDAO();

    /**
     * 创建退货订单（事务）
     */
    public static boolean createReturnOrder(ReturnOrder returnOrder, List<ReturnOrderItem> items) {
        try {
            boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                returnOrder.returnOrderId = ReturnOrderDAO.generateNextReturnOrderId(conn);
                returnOrder.status = "PENDING";

                if (items != null && !items.isEmpty()) {
                    for (ReturnOrderItem item : items) {
                        item.returnOrderId = returnOrder.returnOrderId;
                    }
                }

                if (!ReturnOrderDAO.insertWithConnection(conn, returnOrder)) {
                    return false;
                }

                return items == null || items.isEmpty() || ReturnOrderItemDAO.batchInsertWithConnection(conn, items);
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
            }
            return success;
        } catch (SQLException e) {
            logger.error("创建退货订单失败", e);
            return false;
        }
    }

    /**
     * 审批退货订单（事务）
     */
    public static boolean approveReturnOrder(String returnOrderId, String approverName,
                                              String approvalComment, boolean approved) {
        try {
            boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                ReturnOrder returnOrder = ReturnOrderDAO.findByReturnOrderIdWithConnection(conn, returnOrderId);
                if (returnOrder == null) {
                    return false;
                }

                returnOrder.status = approved ? "APPROVED" : "REJECTED";
                returnOrder.approverName = approverName;
                returnOrder.approvalDate = new Date();
                returnOrder.approvalComment = approvalComment;

                if (!ReturnOrderDAO.updateWithConnection(conn, returnOrder)) {
                    return false;
                }

                if (!approved) {
                    return true;
                }

                List<ReturnOrderItem> items = ReturnOrderItemDAO.findByReturnOrderIdWithConnection(conn, returnOrderId);
                for (ReturnOrderItem item : items) {
                    Product product = productDAO.findByIdWithConnection(conn, item.productId);
                    if (product == null) {
                        throw new SQLException("退货商品不存在: productId=" + item.productId);
                    }

                    product.quantity += item.returnQuantity;
                    if (!productDAO.updateWithVersionWithConnection(conn, product)) {
                        throw new SQLException("恢复退货库存失败: productId=" + item.productId);
                    }
                }

                OperationLog log = new OperationLog();
                log.username = approverName;
                log.operation = "RETURN_APPROVAL";
                log.details = String.format("审批退货单: %s, 金额: %.2f",
                    returnOrderId, returnOrder.totalAmount);
                log.ipAddress = "localhost";
                log.timestamp = new Date();
                return OperationLogDAO.insertWithConnection(conn, log);
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
                ReturnOrder returnOrder = ReturnOrderDAO.findByReturnOrderIdWithConnection(conn, returnOrderId);
                if (returnOrder == null) {
                    return false;
                }

                if (!"APPROVED".equals(returnOrder.status)) {
                    return false;
                }

                returnOrder.status = "COMPLETED";
                returnOrder.completedDate = new Date();

                if (!ReturnOrderDAO.updateWithConnection(conn, returnOrder)) {
                    return false;
                }

                if (returnOrder.memberId == null || returnOrder.memberId <= 0) {
                    return true;
                }

                Member member = MemberDAO.findByIdWithConnection(conn, returnOrder.memberId);
                if (member == null) {
                    throw new SQLException("退货会员不存在: memberId=" + returnOrder.memberId);
                }

                member.balance = member.getBalance().add(returnOrder.getTotalAmount());
                if (!MemberDAO.updateWithConnection(conn, member)) {
                    throw new SQLException("更新退货会员余额失败: memberId=" + returnOrder.memberId);
                }

                RechargeRecord record = new RechargeRecord();
                record.memberPhone = member.phone;
                record.memberName = member.name;
                record.amount = returnOrder.getTotalAmount();
                record.paymentMethod = returnOrder.paymentMethod;
                record.operator = returnOrder.operatorName;
                record.timestamp = new Date();
                record.recordId = returnOrderId;
                if (!RechargeRecordDAO.insertWithConnection(conn, record)) {
                    throw new SQLException("插入退款记录失败");
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
        return ReturnOrderDAO.findByStatus("PENDING");
    }

    /**
     * 获取已批准的退货订单列表
     */
    public static List<ReturnOrder> getApprovedReturnOrders() {
        return ReturnOrderDAO.findByStatus("APPROVED");
    }

    /**
     * 获取已完成的退货订单列表
     */
    public static List<ReturnOrder> getCompletedReturnOrders() {
        return ReturnOrderDAO.findByStatus("COMPLETED");
    }

    /**
     * 获取会员的退货订单列表
     */
    public static List<ReturnOrder> getMemberReturnOrders(int memberId) {
        return ReturnOrderDAO.findByMemberId(memberId);
    }

    /**
     * 计算退货统计
     */
    public static ReturnStatistics calculateReturnStatistics(Date startDate, Date endDate) {
        ReturnStatistics stats = new ReturnStatistics();
        
        List<ReturnOrder> returnOrders = ReturnOrderDAO.findByDateRange(startDate, endDate);
        
        stats.totalReturnOrders = returnOrders.size();
        stats.totalReturnAmount = 0;
        stats.approvedOrders = 0;
        stats.rejectedOrders = 0;
        stats.completedOrders = 0;
        
        for (ReturnOrder order : returnOrders) {
            stats.totalReturnAmount += order.getTotalAmount().doubleValue();
            
            switch (order.status) {
                case "APPROVED":
                    stats.approvedOrders++;
                    break;
                case "REJECTED":
                    stats.rejectedOrders++;
                    break;
                case "COMPLETED":
                    stats.completedOrders++;
                    break;
            }
        }
        
        return stats;
    }

    /**
     * 退货统计类
     */
    public static class ReturnStatistics {
        public int totalReturnOrders;
        public double totalReturnAmount;
        public int approvedOrders;
        public int rejectedOrders;
        public int completedOrders;
    }
}