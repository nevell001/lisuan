package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * 退货管理服务类
 */
public class ReturnService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnService.class);

    /**
     * 创建退货订单（事务）
     */
    public static boolean createReturnOrder(ReturnOrder returnOrder, List<ReturnOrderItem> items) {
        Connection conn = null;
        try {
            conn = com.cashier.util.DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 生成退货单号
            returnOrder.returnOrderId = ReturnOrderDAO.generateNextReturnOrderId();
            returnOrder.status = "PENDING";

            // 设置退货订单明细的退货单号
            if (items != null && !items.isEmpty()) {
                for (ReturnOrderItem item : items) {
                    item.returnOrderId = returnOrder.returnOrderId;
                }
            }

            // 插入退货订单
            boolean result = ReturnOrderDAO.insert(returnOrder);
            if (!result) {
                conn.rollback();
                return false;
            }

            // 批量插入退货订单明细
            if (items != null && !items.isEmpty()) {
                result = ReturnOrderItemDAO.batchInsert(items);
                if (!result) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            logger.info("退货订单创建成功: {}", returnOrder.returnOrderId);
            return true;
        } catch (SQLException e) {
            logger.error("创建退货订单失败", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("回滚事务失败", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败", e);
                }
            }
        }
    }

    /**
     * 审批退货订单（事务）
     */
    public static boolean approveReturnOrder(String returnOrderId, String approverName, 
                                              String approvalComment, boolean approved) {
        Connection conn = null;
        try {
            conn = com.cashier.util.DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 查找退货订单
            ReturnOrder returnOrder = ReturnOrderDAO.findByReturnOrderId(returnOrderId);
            if (returnOrder == null) {
                conn.rollback();
                return false;
            }

            // 更新退货订单状态
            returnOrder.status = approved ? "APPROVED" : "REJECTED";
            returnOrder.approverName = approverName;
            returnOrder.approvalDate = new Date();
            returnOrder.approvalComment = approvalComment;

            boolean result = ReturnOrderDAO.update(returnOrder);
            if (!result) {
                conn.rollback();
                return false;
            }

            // 如果审批通过，处理库存和退款
            if (approved) {
                List<ReturnOrderItem> items = ReturnOrderItemDAO.findByReturnOrderId(returnOrderId);
                for (ReturnOrderItem item : items) {
                    // 增加库存
                    Product product = ProductDAO.findById(item.productId);
                    if (product != null) {
                        product.quantity += item.returnQuantity;
                        ProductDAO.update(product);
                    }
                }

                // 记录操作日志
                OperationLog log = new OperationLog();
                log.username = approverName;
                log.operation = "RETURN_APPROVAL";
                log.details = String.format("审批退货单: %s, 金额: %.2f", 
                    returnOrderId, returnOrder.totalAmount);
                log.ipAddress = "localhost";
                log.timestamp = new Date();
                OperationLogDAO.insert(log);
            }

            conn.commit();
            logger.info("退货订单审批成功: {}, 结果: {}", returnOrderId, approved ? "通过" : "拒绝");
            return true;
        } catch (SQLException e) {
            logger.error("审批退货订单失败", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("回滚事务失败", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败", e);
                }
            }
        }
    }

    /**
     * 完成退货订单（事务）
     */
    public static boolean completeReturnOrder(String returnOrderId) {
        Connection conn = null;
        try {
            conn = com.cashier.util.DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 查找退货订单
            ReturnOrder returnOrder = ReturnOrderDAO.findByReturnOrderId(returnOrderId);
            if (returnOrder == null) {
                conn.rollback();
                return false;
            }

            // 只有已批准的退货单才能完成
            if (!"APPROVED".equals(returnOrder.status)) {
                conn.rollback();
                return false;
            }

            // 更新退货订单状态
            returnOrder.status = "COMPLETED";
            returnOrder.completedDate = new Date();

            boolean result = ReturnOrderDAO.update(returnOrder);
            if (!result) {
                conn.rollback();
                return false;
            }

            // 会员退款（如果有的话）
            if (returnOrder.memberId > 0) {
                Member member = MemberDAO.findById(returnOrder.memberId);
                if (member != null) {
                    member.balance += returnOrder.totalAmount;
                    MemberDAO.update(member);

                    // 记录充值记录
                    RechargeRecord record = new RechargeRecord();
                    record.memberPhone = member.phone;
                    record.memberName = member.name;
                    record.amount = returnOrder.totalAmount;
                    record.paymentMethod = returnOrder.paymentMethod;
                    record.operator = returnOrder.operatorName;
                    record.timestamp = new Date();
                    record.recordId = returnOrderId; // 使用退货单号作为记录ID
                    RechargeRecordDAO.insert(record);
                }
            }

            conn.commit();
            logger.info("退货订单完成成功: {}", returnOrderId);
            return true;
        } catch (SQLException e) {
            logger.error("完成退货订单失败", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("回滚事务失败", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败", e);
                }
            }
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
            stats.totalReturnAmount += order.totalAmount;
            
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