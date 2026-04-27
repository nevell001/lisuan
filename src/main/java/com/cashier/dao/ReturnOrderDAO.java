package com.cashier.dao;

import com.cashier.model.ReturnOrder;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 退货订单数据访问对象
 */
public class ReturnOrderDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnOrderDAO.class);

    /**
     * 插入退货订单
     */
    public static boolean insert(ReturnOrder returnOrder) {
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection()) {
            return insertWithConnection(conn, returnOrder);
        } catch (SQLException e) {
            logger.error("插入退货订单失败", e);
            return false;
        }
    }

    /**
     * 使用指定连接插入退货订单
     * @param conn 数据库连接
     * @param returnOrder 退货订单
     * @return 插入是否成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insertWithConnection(Connection conn, ReturnOrder returnOrder) throws SQLException {
        String sql = "INSERT INTO return_orders (return_order_id, original_transaction_id, member_id, member_name, " +
                "return_date, return_reason, total_amount, status, payment_method, operator_name, " +
                "approver_name, approval_date, approval_comment, completed_date, notes, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, returnOrder.returnOrderId);
            stmt.setString(2, returnOrder.originalTransactionId);
            if (returnOrder.memberId != null) {
                stmt.setInt(3, returnOrder.memberId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, returnOrder.memberName);
            stmt.setTimestamp(5, new Timestamp(returnOrder.returnDate.getTime()));
            stmt.setString(6, returnOrder.returnReason);
            stmt.setBigDecimal(7, returnOrder.totalAmount);
            stmt.setString(8, returnOrder.status);
            stmt.setString(9, returnOrder.paymentMethod);
            stmt.setString(10, returnOrder.operatorName);
            stmt.setString(11, returnOrder.approverName);
            stmt.setTimestamp(12, returnOrder.approvalDate != null ? new Timestamp(returnOrder.approvalDate.getTime()) : null);
            stmt.setString(13, returnOrder.approvalComment);
            stmt.setTimestamp(14, returnOrder.completedDate != null ? new Timestamp(returnOrder.completedDate.getTime()) : null);
            stmt.setString(15, returnOrder.notes);
            stmt.setTimestamp(16, new Timestamp(returnOrder.createTime.getTime()));
            stmt.setTimestamp(17, new Timestamp(returnOrder.updateTime.getTime()));

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新退货订单
     */
    public static boolean update(ReturnOrder returnOrder) {
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection()) {
            return updateWithConnection(conn, returnOrder);
        } catch (SQLException e) {
            logger.error("更新退货订单失败", e);
            return false;
        }
    }

    /**
     * 使用指定连接更新退货订单
     * @param conn 数据库连接
     * @param returnOrder 退货订单
     * @return 更新是否成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean updateWithConnection(Connection conn, ReturnOrder returnOrder) throws SQLException {
        String sql = "UPDATE return_orders SET original_transaction_id = ?, member_id = ?, member_name = ?, " +
                "return_date = ?, return_reason = ?, total_amount = ?, status = ?, payment_method = ?, " +
                "operator_name = ?, approver_name = ?, approval_date = ?, approval_comment = ?, " +
                "completed_date = ?, notes = ?, update_time = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, returnOrder.originalTransactionId);
            if (returnOrder.memberId != null) {
                stmt.setInt(2, returnOrder.memberId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, returnOrder.memberName);
            stmt.setTimestamp(4, new Timestamp(returnOrder.returnDate.getTime()));
            stmt.setString(5, returnOrder.returnReason);
            stmt.setBigDecimal(6, returnOrder.totalAmount);
            stmt.setString(7, returnOrder.status);
            stmt.setString(8, returnOrder.paymentMethod);
            stmt.setString(9, returnOrder.operatorName);
            stmt.setString(10, returnOrder.approverName);
            stmt.setTimestamp(11, returnOrder.approvalDate != null ? new Timestamp(returnOrder.approvalDate.getTime()) : null);
            stmt.setString(12, returnOrder.approvalComment);
            stmt.setTimestamp(13, returnOrder.completedDate != null ? new Timestamp(returnOrder.completedDate.getTime()) : null);
            stmt.setString(14, returnOrder.notes);
            stmt.setTimestamp(15, new Timestamp(new Date().getTime()));
            stmt.setInt(16, returnOrder.id);

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除退货订单
     */
    public static boolean delete(int id) {
        String sql = "DELETE FROM return_orders WHERE id = ?";
        
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("删除退货订单失败", e);
            return false;
        }
    }

    /**
     * 根据 ID 查找退货订单
     */
    public static ReturnOrder findById(int id) {
        String sql = "SELECT id, return_order_id, original_transaction_id, member_id, member_name, return_date, return_reason, total_amount, status, payment_method, operator_name, approver_name, approval_date, approval_comment, completed_date, notes, create_time, update_time FROM return_orders WHERE id = ?";
        
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapRowToReturnOrder(rs);
            }
        } catch (SQLException e) {
            logger.error("查找退货订单失败", e);
        }
        
        return null;
    }

    /**
     * 根据退货单号查找退货订单
     */
    public static ReturnOrder findByReturnOrderId(String returnOrderId) {
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection()) {
            return findByReturnOrderIdWithConnection(conn, returnOrderId);
        } catch (SQLException e) {
            logger.error("查找退货订单失败", e);
        }

        return null;
    }

    /**
     * 使用指定连接根据退货单号查找退货订单
     * @param conn 数据库连接
     * @param returnOrderId 退货单号
     * @return 退货订单，不存在时返回 null
     * @throws SQLException 数据库操作异常
     */
    public static ReturnOrder findByReturnOrderIdWithConnection(Connection conn, String returnOrderId) throws SQLException {
        String sql = "SELECT id, return_order_id, original_transaction_id, member_id, member_name, return_date, return_reason, total_amount, status, payment_method, operator_name, approver_name, approval_date, approval_comment, completed_date, notes, create_time, update_time FROM return_orders WHERE return_order_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, returnOrderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToReturnOrder(rs);
                }
            }
        }

        return null;
    }

    /**
     * 查找所有退货订单
     */
    public static List<ReturnOrder> findAll() {
        String sql = "SELECT id, return_order_id, original_transaction_id, member_id, member_name, return_date, return_reason, total_amount, status, payment_method, operator_name, approver_name, approval_date, approval_comment, completed_date, notes, create_time, update_time FROM return_orders ORDER BY create_time DESC";
        List<ReturnOrder> returnOrders = new ArrayList<>();
        
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                returnOrders.add(mapRowToReturnOrder(rs));
            }
        } catch (SQLException e) {
            logger.error("查找所有退货订单失败", e);
        }
        
        return returnOrders;
    }

    /**
     * 根据状态查找退货订单
     */
    public static List<ReturnOrder> findByStatus(String status) {
        String sql = "SELECT id, return_order_id, original_transaction_id, member_id, member_name, return_date, return_reason, total_amount, status, payment_method, operator_name, approver_name, approval_date, approval_comment, completed_date, notes, create_time, update_time FROM return_orders WHERE status = ? ORDER BY create_time DESC";
        List<ReturnOrder> returnOrders = new ArrayList<>();
        
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                returnOrders.add(mapRowToReturnOrder(rs));
            }
        } catch (SQLException e) {
            logger.error("根据状态查找退货订单失败", e);
        }
        
        return returnOrders;
    }

    /**
     * 根据会员ID查找退货订单
     */
    public static List<ReturnOrder> findByMemberId(int memberId) {
        String sql = "SELECT id, return_order_id, original_transaction_id, member_id, member_name, return_date, return_reason, total_amount, status, payment_method, operator_name, approver_name, approval_date, approval_comment, completed_date, notes, create_time, update_time FROM return_orders WHERE member_id = ? ORDER BY create_time DESC";
        List<ReturnOrder> returnOrders = new ArrayList<>();
        
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                returnOrders.add(mapRowToReturnOrder(rs));
            }
        } catch (SQLException e) {
            logger.error("根据会员ID查找退货订单失败", e);
        }
        
        return returnOrders;
    }

    /**
     * 根据日期范围查找退货订单
     */
    public static List<ReturnOrder> findByDateRange(Date startDate, Date endDate) {
        String sql = "SELECT id, return_order_id, original_transaction_id, member_id, member_name, return_date, return_reason, total_amount, status, payment_method, operator_name, approver_name, approval_date, approval_comment, completed_date, notes, create_time, update_time FROM return_orders WHERE return_date BETWEEN ? AND ? ORDER BY return_date DESC";
        List<ReturnOrder> returnOrders = new ArrayList<>();

        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(startDate.getTime()));
            stmt.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                returnOrders.add(mapRowToReturnOrder(rs));
            }
        } catch (SQLException e) {
            logger.error("根据日期范围查找退货订单失败", e);
        }

        return returnOrders;
    }

    /**
     * 根据原交易ID查找退货订单（不包括已拒绝的）
     */
    public static List<ReturnOrder> findByOriginalTransactionId(String transactionId) {
        String sql = "SELECT id, return_order_id, original_transaction_id, member_id, member_name, return_date, return_reason, total_amount, status, payment_method, operator_name, approver_name, approval_date, approval_comment, completed_date, notes, create_time, update_time FROM return_orders WHERE original_transaction_id = ? AND status != 'REJECTED' ORDER BY create_time DESC";
        List<ReturnOrder> returnOrders = new ArrayList<>();

        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transactionId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                returnOrders.add(mapRowToReturnOrder(rs));
            }
        } catch (SQLException e) {
            logger.error("根据原交易ID查找退货订单失败", e);
        }

        return returnOrders;
    }

    /**
     * 生成下一个退货单号
     */
    public static String generateNextReturnOrderId() {
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection()) {
            return generateNextReturnOrderId(conn);
        } catch (SQLException e) {
            logger.error("生成退货单号失败", e);
        }

        String prefix = "R" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        return prefix + "0001";
    }

    /**
     * 使用指定连接生成下一个退货单号
     * @param conn 数据库连接
     * @return 退货单号
     * @throws SQLException 数据库操作异常
     */
    public static String generateNextReturnOrderId(Connection conn) throws SQLException {
        String prefix = "R" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        String sql = "SELECT return_order_id FROM return_orders WHERE return_order_id LIKE ? ORDER BY return_order_id DESC LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String lastId = rs.getString("return_order_id");
                    int lastSeq = Integer.parseInt(lastId.substring(9));
                    return prefix + String.format("%04d", lastSeq + 1);
                }
            }
        }

        return prefix + "0001";
    }

    /**
     * 将 ResultSet 映射为 ReturnOrder 对象
     */
    private static ReturnOrder mapRowToReturnOrder(ResultSet rs) throws SQLException {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.id = rs.getInt("id");
        returnOrder.returnOrderId = rs.getString("return_order_id");
        returnOrder.originalTransactionId = rs.getString("original_transaction_id");
        int memberId = rs.getInt("member_id");
        returnOrder.memberId = rs.wasNull() ? null : memberId;
        returnOrder.memberName = rs.getString("member_name");
        returnOrder.returnDate = new Date(rs.getTimestamp("return_date").getTime());
        returnOrder.returnReason = rs.getString("return_reason");
        returnOrder.totalAmount = rs.getBigDecimal("total_amount");
        returnOrder.status = rs.getString("status");
        returnOrder.paymentMethod = rs.getString("payment_method");
        returnOrder.operatorName = rs.getString("operator_name");
        returnOrder.approverName = rs.getString("approver_name");
        
        Timestamp approvalDate = rs.getTimestamp("approval_date");
        returnOrder.approvalDate = approvalDate != null ? new Date(approvalDate.getTime()) : null;
        
        returnOrder.approvalComment = rs.getString("approval_comment");
        
        Timestamp completedDate = rs.getTimestamp("completed_date");
        returnOrder.completedDate = completedDate != null ? new Date(completedDate.getTime()) : null;
        
        returnOrder.notes = rs.getString("notes");
        returnOrder.createTime = new Date(rs.getTimestamp("create_time").getTime());
        returnOrder.updateTime = new Date(rs.getTimestamp("update_time").getTime());
        
        return returnOrder;
    }
}