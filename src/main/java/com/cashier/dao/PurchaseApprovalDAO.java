package com.cashier.dao;

import com.cashier.model.PurchaseApproval;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购审批记录数据访问对象
 * 负责采购审批记录相关的数据库操作
 */
public class PurchaseApprovalDAO {
    private static final Logger logger = LoggerFactory.getLogger(PurchaseApprovalDAO.class);

    /**
     * 根据ID查找采购审批记录
     *
     * @param id 记录ID
     * @return 采购审批记录对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseApproval findById(int id) throws SQLException {
        String sql = "SELECT id, order_id, approver, action, remark, approval_time " +
                     "FROM purchase_approvals WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseApproval(rs);
            }
        }
        return null;
    }

    /**
     * 查询所有采购审批记录
     *
     * @return 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseApproval> findAll() throws SQLException {
        List<PurchaseApproval> approvals = new ArrayList<>();
        String sql = "SELECT id, order_id, approver, action, remark, approval_time " +
                     "FROM purchase_approvals ORDER BY approval_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                approvals.add(mapRowToPurchaseApproval(rs));
            }
        }
        return approvals;
    }

    /**
     * 根据订单ID查找所有审批记录
     *
     * @param orderId 订单ID
     * @return 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseApproval> findByOrderId(int orderId) throws SQLException {
        List<PurchaseApproval> approvals = new ArrayList<>();
        String sql = "SELECT id, order_id, approver, action, remark, approval_time " +
                     "FROM purchase_approvals WHERE order_id = ? ORDER BY approval_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                approvals.add(mapRowToPurchaseApproval(rs));
            }
        }
        return approvals;
    }

    /**
     * 根据审批人查找审批记录
     *
     * @param approver 审批人
     * @return 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseApproval> findByApprover(String approver) throws SQLException {
        List<PurchaseApproval> approvals = new ArrayList<>();
        String sql = "SELECT id, order_id, approver, action, remark, approval_time " +
                     "FROM purchase_approvals WHERE approver = ? ORDER BY approval_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, approver);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                approvals.add(mapRowToPurchaseApproval(rs));
            }
        }
        return approvals;
    }

    /**
     * 根据审批动作查找审批记录
     *
     * @param action 审批动作（approve-通过，reject-拒绝）
     * @return 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseApproval> findByAction(String action) throws SQLException {
        List<PurchaseApproval> approvals = new ArrayList<>();
        String sql = "SELECT id, order_id, approver, action, remark, approval_time " +
                     "FROM purchase_approvals WHERE action = ? ORDER BY approval_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, action);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                approvals.add(mapRowToPurchaseApproval(rs));
            }
        }
        return approvals;
    }

    /**
     * 根据订单ID和审批人查找审批记录
     *
     * @param orderId  订单ID
     * @param approver 审批人
     * @return 采购审批记录对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseApproval findByOrderAndApprover(int orderId, String approver) throws SQLException {
        String sql = "SELECT id, order_id, approver, action, remark, approval_time " +
                     "FROM purchase_approvals WHERE order_id = ? AND approver = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            pstmt.setString(2, approver);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseApproval(rs);
            }
        }
        return null;
    }

    /**
     * 插入新采购审批记录
     *
     * @param approval 采购审批记录对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insert(PurchaseApproval approval) throws SQLException {
        String sql = "INSERT INTO purchase_approvals (order_id, approver, action, remark, approval_time) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, approval.orderId);
            pstmt.setString(2, approval.approver);
            pstmt.setString(3, approval.action);
            pstmt.setString(4, approval.remark);
            pstmt.setTimestamp(5, approval.approvalTime);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        approval.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 删除采购审批记录
     *
     * @param id 记录ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM purchase_approvals WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据订单ID删除所有审批记录
     *
     * @param orderId 订单ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean deleteByOrderId(int orderId) throws SQLException {
        String sql = "DELETE FROM purchase_approvals WHERE order_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入采购审批记录
     *
     * @param approvals 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public static void batchInsert(List<PurchaseApproval> approvals) throws SQLException {
        String sql = "INSERT INTO purchase_approvals (order_id, approver, action, remark, approval_time) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (PurchaseApproval approval : approvals) {
                pstmt.setInt(1, approval.orderId);
                pstmt.setString(2, approval.approver);
                pstmt.setString(3, approval.action);
                pstmt.setString(4, approval.remark);
                pstmt.setTimestamp(5, approval.approvalTime);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 统计审批记录数量
     *
     * @param action 审批动作（可为null）
     * @return 记录数量
     * @throws SQLException 数据库操作异常
     */
    public static int countByAction(String action) throws SQLException {
        String sql;
        if (action == null || action.isEmpty()) {
            sql = "SELECT COUNT(*) as count FROM purchase_approvals";
        } else {
            sql = "SELECT COUNT(*) as count FROM purchase_approvals WHERE action = ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (action != null && !action.isEmpty()) {
                pstmt.setString(1, action);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    /**
     * 将 ResultSet 映射为 PurchaseApproval 对象
     *
     * @param rs 结果集
     * @return 采购审批记录对象
     * @throws SQLException 数据库操作异常
     */
    private static PurchaseApproval mapRowToPurchaseApproval(ResultSet rs) throws SQLException {
        PurchaseApproval approval = new PurchaseApproval();
        approval.id = rs.getInt("id");
        approval.orderId = rs.getInt("order_id");
        approval.approver = rs.getString("approver");
        approval.action = rs.getString("action");
        approval.remark = rs.getString("remark");
        approval.approvalTime = rs.getTimestamp("approval_time");
        return approval;
    }
}