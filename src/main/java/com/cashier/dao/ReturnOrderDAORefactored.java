package com.cashier.dao;

import com.cashier.exception.DatabaseException;
import com.cashier.model.ReturnOrder;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 退货订单数据访问对象（重构版）
 * 实例方法 + BaseDAO，通过 DAOFactory 获取；保留 DatabaseException 包装行为。
 */
public class ReturnOrderDAORefactored extends BaseDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnOrderDAORefactored.class);

    private static final String SELECT_COLUMNS =
        "id, return_order_id, original_transaction_id, member_id, member_name, return_date, return_reason, total_amount, status, " +
        "payment_method, operator_name, approver_name, approval_date, approval_comment, completed_date, notes, create_time, update_time ";

    public boolean insert(ReturnOrder returnOrder) {
        try (Connection conn = getConnection()) {
            return insertWithConnection(conn, returnOrder);
        } catch (SQLException e) {
            logger.error("插入退货订单失败", e);
            throw new DatabaseException("插入退货订单失败", DatabaseException.DbErrorType.INSERT_FAILED, e);
        }
    }

    public boolean insertWithConnection(Connection conn, ReturnOrder returnOrder) throws SQLException {
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
            stmt.setTimestamp(5, returnOrder.returnDate != null ? Timestamp.from(returnOrder.returnDate) : null);
            stmt.setString(6, returnOrder.returnReason);
            stmt.setBigDecimal(7, returnOrder.totalAmount);
            stmt.setString(8, returnOrder.status);
            stmt.setString(9, returnOrder.paymentMethod);
            stmt.setString(10, returnOrder.operatorName);
            stmt.setString(11, returnOrder.approverName);
            stmt.setTimestamp(12, returnOrder.approvalDate != null ? Timestamp.from(returnOrder.approvalDate) : null);
            stmt.setString(13, returnOrder.approvalComment);
            stmt.setTimestamp(14, returnOrder.completedDate != null ? Timestamp.from(returnOrder.completedDate) : null);
            stmt.setString(15, returnOrder.notes);
            stmt.setTimestamp(16, returnOrder.createTime != null ? Timestamp.from(returnOrder.createTime) : null);
            stmt.setTimestamp(17, returnOrder.updateTime != null ? Timestamp.from(returnOrder.updateTime) : null);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean update(ReturnOrder returnOrder) {
        try (Connection conn = getConnection()) {
            return updateWithConnection(conn, returnOrder);
        } catch (SQLException e) {
            logger.error("更新退货订单失败", e);
            throw new DatabaseException("更新退货订单失败", DatabaseException.DbErrorType.UPDATE_FAILED, e);
        }
    }

    public boolean updateWithConnection(Connection conn, ReturnOrder returnOrder) throws SQLException {
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
            stmt.setTimestamp(4, returnOrder.returnDate != null ? Timestamp.from(returnOrder.returnDate) : null);
            stmt.setString(5, returnOrder.returnReason);
            stmt.setBigDecimal(6, returnOrder.totalAmount);
            stmt.setString(7, returnOrder.status);
            stmt.setString(8, returnOrder.paymentMethod);
            stmt.setString(9, returnOrder.operatorName);
            stmt.setString(10, returnOrder.approverName);
            stmt.setTimestamp(11, returnOrder.approvalDate != null ? Timestamp.from(returnOrder.approvalDate) : null);
            stmt.setString(12, returnOrder.approvalComment);
            stmt.setTimestamp(13, returnOrder.completedDate != null ? Timestamp.from(returnOrder.completedDate) : null);
            stmt.setString(14, returnOrder.notes);
            stmt.setTimestamp(15, Timestamp.from(java.time.Instant.now()));
            stmt.setInt(16, returnOrder.id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 原子审批/驳回：仅当退货单当前状态为 PENDING 时迁移到 newStatus 并记录审批信息。
     * 并发下第二次操作影响行数为 0，防止重复审批导致的重复恢复库存。
     *
     * @param newStatus 目标状态（APPROVED 或 REJECTED）
     * @return 是否成功迁移；0 行受影响（状态已非 PENDING）返回 false
     */
    public boolean markApprovalWithConnection(Connection conn, String returnOrderId,
                                              String newStatus, String approverName,
                                              String approvalComment) throws SQLException {
        String sql = "UPDATE return_orders SET status = ?, approver_name = ?, approval_date = ?, " +
            "approval_comment = ?, update_time = ? WHERE return_order_id = ? AND status = 'PENDING'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, approverName);
            stmt.setTimestamp(3, Timestamp.from(java.time.Instant.now()));
            stmt.setString(4, approvalComment);
            stmt.setTimestamp(5, Timestamp.from(java.time.Instant.now()));
            stmt.setString(6, returnOrderId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 原子完成：仅当退货单当前状态为 APPROVED 时迁移到 COMPLETED 并记录完成时间。
     * 并发下第二次操作影响行数为 0，防止重复完成导致的重复退款/重复写流水。
     *
     * @return 是否成功迁移；0 行受影响（状态非 APPROVED）返回 false
     */
    public boolean markCompletedWithConnection(Connection conn, String returnOrderId) throws SQLException {
        String sql = "UPDATE return_orders SET status = 'COMPLETED', completed_date = ?, update_time = ? " +
            "WHERE return_order_id = ? AND status = 'APPROVED'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.from(java.time.Instant.now()));
            stmt.setTimestamp(2, Timestamp.from(java.time.Instant.now()));
            stmt.setString(3, returnOrderId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM return_orders WHERE id = ?")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("删除退货订单失败", e);
            throw new DatabaseException("删除退货订单失败", DatabaseException.DbErrorType.DELETE_FAILED, e);
        }
    }

    public ReturnOrder findById(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + SELECT_COLUMNS +
                 " FROM return_orders WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToReturnOrder(rs) : null;
            }
        } catch (SQLException e) {
            logger.error("查找退货订单失败", e);
            throw new DatabaseException("查找退货订单失败", DatabaseException.DbErrorType.QUERY_FAILED, e);
        }
    }

    public ReturnOrder findByReturnOrderId(String returnOrderId) {
        try (Connection conn = getConnection()) {
            return findByReturnOrderIdWithConnection(conn, returnOrderId);
        } catch (SQLException e) {
            logger.error("查找退货订单失败", e);
            throw new DatabaseException("查找退货订单失败", DatabaseException.DbErrorType.QUERY_FAILED, e);
        }
    }

    public ReturnOrder findByReturnOrderIdWithConnection(Connection conn, String returnOrderId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT " + SELECT_COLUMNS +
            " FROM return_orders WHERE return_order_id = ?")) {
            stmt.setString(1, returnOrderId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRowToReturnOrder(rs) : null;
            }
        }
    }

    public List<ReturnOrder> findAll() {
        return queryWrapped("SELECT " + SELECT_COLUMNS + " FROM return_orders ORDER BY create_time DESC", null, null, "查找所有退货订单失败");
    }

    public List<ReturnOrder> findRecent(int limit) {
        if (limit < 1) {
            return List.of();
        }
        return queryWrapped("SELECT " + SELECT_COLUMNS + " FROM return_orders ORDER BY create_time DESC LIMIT ?",
            limit, null, "查找最近退货订单失败");
    }

    public List<ReturnOrder> findByStatus(String status) {
        return queryWrapped("SELECT " + SELECT_COLUMNS +
            " FROM return_orders WHERE status = ? ORDER BY create_time DESC", status, null, "根据状态查找退货订单失败");
    }

    public List<ReturnOrder> findByMemberId(int memberId) {
        return queryWrapped("SELECT " + SELECT_COLUMNS +
            " FROM return_orders WHERE member_id = ? ORDER BY create_time DESC", memberId, null, "根据会员ID查找退货订单失败");
    }

    public List<ReturnOrder> findByDateRange(Date startDate, Date endDate) {
        return queryWrapped("SELECT " + SELECT_COLUMNS +
            " FROM return_orders WHERE return_date BETWEEN ? AND ? ORDER BY return_date DESC",
            new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime()), "根据日期范围查找退货订单失败");
    }

    public Map<String, Object> getStatistics(Date startDate, Date endDate) {
        String sql = "SELECT COUNT(*) AS total_return_orders, COALESCE(SUM(total_amount), 0) AS total_return_amount, " +
            "SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_orders, " +
            "SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_orders, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_orders " +
            "FROM return_orders WHERE return_date BETWEEN ? AND ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, new Timestamp(startDate.getTime()));
            stmt.setTimestamp(2, new Timestamp(endDate.getTime()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("total_return_orders", rs.getInt("total_return_orders"));
                    stats.put("total_return_amount", rs.getBigDecimal("total_return_amount"));
                    stats.put("approved_orders", rs.getInt("approved_orders"));
                    stats.put("rejected_orders", rs.getInt("rejected_orders"));
                    stats.put("completed_orders", rs.getInt("completed_orders"));
                    return stats;
                }
            }
        } catch (SQLException e) {
            logger.error("获取退货统计失败", e);
            throw new DatabaseException("获取退货统计失败", DatabaseException.DbErrorType.QUERY_FAILED, e);
        }
        Map<String, Object> empty = new HashMap<>();
        empty.put("total_return_orders", 0);
        empty.put("total_return_amount", java.math.BigDecimal.ZERO);
        empty.put("approved_orders", 0);
        empty.put("rejected_orders", 0);
        empty.put("completed_orders", 0);
        return empty;
    }

    public List<ReturnOrder> findByOriginalTransactionId(String transactionId) {
        return queryWrapped("SELECT " + SELECT_COLUMNS +
            " FROM return_orders WHERE original_transaction_id = ? AND status != 'REJECTED' ORDER BY create_time DESC",
            transactionId, null, "根据原交易ID查找退货订单失败");
    }

    public String generateNextReturnOrderId() {
        try (Connection conn = getConnection()) {
            return generateNextReturnOrderId(conn);
        } catch (SQLException e) {
            logger.error("生成退货单号失败", e);
            throw new DatabaseException("生成退货单号失败", DatabaseException.DbErrorType.QUERY_FAILED, e);
        }
    }

    public String generateNextReturnOrderId(Connection conn) throws SQLException {
        String prefix = "R" + java.time.LocalDate.now(java.time.ZoneId.systemDefault())
            .format(com.cashier.util.DateTimeFormats.COMPACT_DATE);
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

    private List<ReturnOrder> queryWrapped(String sql, Object param1, Object param2, String errorMessage) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (param1 != null) {
                stmt.setObject(1, param1);
            }
            if (param2 != null) {
                stmt.setObject(2, param2);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<ReturnOrder> returnOrders = new ArrayList<>();
                while (rs.next()) {
                    returnOrders.add(mapRowToReturnOrder(rs));
                }
                return returnOrders;
            }
        } catch (SQLException e) {
            logger.error(errorMessage, e);
            throw new DatabaseException(errorMessage, DatabaseException.DbErrorType.QUERY_FAILED, e);
        }
    }

    private ReturnOrder mapRowToReturnOrder(ResultSet rs) throws SQLException {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.id = rs.getInt("id");
        returnOrder.returnOrderId = rs.getString("return_order_id");
        returnOrder.originalTransactionId = rs.getString("original_transaction_id");
        int memberId = rs.getInt("member_id");
        returnOrder.memberId = rs.wasNull() ? null : memberId;
        returnOrder.memberName = rs.getString("member_name");
        Timestamp returnDateTs = rs.getTimestamp("return_date");
        returnOrder.returnDate = returnDateTs != null ? returnDateTs.toInstant() : null;
        returnOrder.returnReason = rs.getString("return_reason");
        returnOrder.totalAmount = rs.getBigDecimal("total_amount");
        returnOrder.status = rs.getString("status");
        returnOrder.paymentMethod = rs.getString("payment_method");
        returnOrder.operatorName = rs.getString("operator_name");
        returnOrder.approverName = rs.getString("approver_name");
        Timestamp approvalDate = rs.getTimestamp("approval_date");
        returnOrder.approvalDate = approvalDate != null ? approvalDate.toInstant() : null;
        returnOrder.approvalComment = rs.getString("approval_comment");
        Timestamp completedDate = rs.getTimestamp("completed_date");
        returnOrder.completedDate = completedDate != null ? completedDate.toInstant() : null;
        returnOrder.notes = rs.getString("notes");
        Timestamp createTs = rs.getTimestamp("create_time");
        Timestamp updateTs = rs.getTimestamp("update_time");
        returnOrder.createTime = createTs != null ? createTs.toInstant() : null;
        returnOrder.updateTime = updateTs != null ? updateTs.toInstant() : null;
        return returnOrder;
    }
}
