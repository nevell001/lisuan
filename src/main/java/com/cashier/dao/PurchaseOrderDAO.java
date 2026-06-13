package com.cashier.dao;

import com.cashier.model.PurchaseOrder;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购订单数据访问对象
 * 负责采购订单相关的数据库操作
 */
public class PurchaseOrderDAO {

    /**
     * 根据ID查找采购订单
     *
     * @param id 订单ID
     * @return 采购订单对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseOrder findById(int id) throws SQLException {
        String sql = "SELECT po.id, po.order_no, po.supplier_id, s.name as supplier_name, po.purchase_date, po.expected_date, " +
                     "po.total_amount, po.status, po.purchaser, po.approver, po.approval_time, po.approval_remark, po.remark, po.create_time, po.update_time " +
                     "FROM purchase_orders po LEFT JOIN suppliers s ON po.supplier_id = s.id WHERE po.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseOrder(rs);
            }
        }
        return null;
    }

    /**
     * 查询所有采购订单
     *
     * @return 采购订单列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseOrder> findAll() throws SQLException {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = "SELECT po.id, po.order_no, po.supplier_id, s.name as supplier_name, po.purchase_date, po.expected_date, " +
                     "po.total_amount, po.status, po.purchaser, po.approver, po.approval_time, po.approval_remark, po.remark, po.create_time, po.update_time " +
                     "FROM purchase_orders po LEFT JOIN suppliers s ON po.supplier_id = s.id ORDER BY po.create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                orders.add(mapRowToPurchaseOrder(rs));
            }
        }
        return orders;
    }

    /**
     * 根据订单号查找采购订单
     *
     * @param orderNo 订单号
     * @return 采购订单对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseOrder findByOrderNo(String orderNo) throws SQLException {
        String sql = "SELECT po.id, po.order_no, po.supplier_id, s.name as supplier_name, po.purchase_date, po.expected_date, " +
                     "po.total_amount, po.status, po.purchaser, po.approver, po.approval_time, po.approval_remark, po.remark, po.create_time, po.update_time " +
                     "FROM purchase_orders po LEFT JOIN suppliers s ON po.supplier_id = s.id WHERE po.order_no = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, orderNo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseOrder(rs);
            }
        }
        return null;
    }

    /**
     * 根据供应商ID查找采购订单
     *
     * @param supplierId 供应商ID
     * @return 采购订单列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseOrder> findBySupplier(int supplierId) throws SQLException {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = "SELECT po.id, po.order_no, po.supplier_id, s.name as supplier_name, po.purchase_date, po.expected_date, " +
                     "po.total_amount, po.status, po.purchaser, po.approver, po.approval_time, po.approval_remark, po.remark, po.create_time, po.update_time " +
                     "FROM purchase_orders po LEFT JOIN suppliers s ON po.supplier_id = s.id WHERE po.supplier_id = ? ORDER BY po.create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, supplierId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                orders.add(mapRowToPurchaseOrder(rs));
            }
        }
        return orders;
    }

    /**
     * 根据订单状态查找采购订单
     *
     * @param status 订单状态（pending-待审批，approved-已审批，rejected-已拒绝，completed-已完成）
     * @return 采购订单列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseOrder> findByStatus(String status) throws SQLException {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = "SELECT po.id, po.order_no, po.supplier_id, s.name as supplier_name, po.purchase_date, po.expected_date, " +
                     "po.total_amount, po.status, po.purchaser, po.approver, po.approval_time, po.approval_remark, po.remark, po.create_time, po.update_time " +
                     "FROM purchase_orders po LEFT JOIN suppliers s ON po.supplier_id = s.id WHERE po.status = ? ORDER BY po.create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                orders.add(mapRowToPurchaseOrder(rs));
            }
        }
        return orders;
    }

    /**
     * 根据日期范围查找采购订单
     *
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 采购订单列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseOrder> findByDateRange(String startDate, String endDate) throws SQLException {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = "SELECT po.id, po.order_no, po.supplier_id, s.name as supplier_name, po.purchase_date, po.expected_date, " +
                     "po.total_amount, po.status, po.purchaser, po.approver, po.approval_time, po.approval_remark, po.remark, po.create_time, po.update_time " +
                     "FROM purchase_orders po LEFT JOIN suppliers s ON po.supplier_id = s.id WHERE po.purchase_date BETWEEN ? AND ? ORDER BY po.create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                orders.add(mapRowToPurchaseOrder(rs));
            }
        }
        return orders;
    }

    /**
     * 根据采购人查找采购订单
     *
     * @param purchaser 采购人
     * @return 采购订单列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseOrder> findByPurchaser(String purchaser) throws SQLException {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = "SELECT po.id, po.order_no, po.supplier_id, s.name as supplier_name, po.purchase_date, po.expected_date, " +
                     "po.total_amount, po.status, po.purchaser, po.approver, po.approval_time, po.approval_remark, po.remark, po.create_time, po.update_time " +
                     "FROM purchase_orders po LEFT JOIN suppliers s ON po.supplier_id = s.id WHERE po.purchaser = ? ORDER BY po.create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, purchaser);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                orders.add(mapRowToPurchaseOrder(rs));
            }
        }
        return orders;
    }

    /**
     * 插入新采购订单
     *
     * @param order 采购订单对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insert(PurchaseOrder order) throws SQLException {
        String sql = "INSERT INTO purchase_orders (order_no, supplier_id, purchase_date, expected_date, " +
                     "total_amount, status, purchaser, approver, approval_time, approval_remark, remark, create_time, update_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, order.orderNo);
            pstmt.setInt(2, order.supplierId);
            pstmt.setString(3, order.purchaseDate);
            pstmt.setString(4, order.expectedDate);
            pstmt.setBigDecimal(5, order.totalAmount);
            pstmt.setString(6, order.status);
            pstmt.setString(7, order.purchaser);
            pstmt.setString(8, order.approver);
            pstmt.setTimestamp(9, order.approvalTime);
            pstmt.setString(10, order.approvalRemark);
            pstmt.setString(11, order.remark);
            pstmt.setTimestamp(12, order.createTime);
            pstmt.setTimestamp(13, order.updateTime);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        order.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新采购订单
     *
     * @param order 采购订单对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean update(PurchaseOrder order) throws SQLException {
        String sql = "UPDATE purchase_orders SET supplier_id = ?, purchase_date = ?, expected_date = ?, " +
                     "total_amount = ?, status = ?, purchaser = ?, approver = ?, approval_time = ?, approval_remark = ?, " +
                     "remark = ?, update_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, order.supplierId);
            pstmt.setString(2, order.purchaseDate);
            pstmt.setString(3, order.expectedDate);
            pstmt.setBigDecimal(4, order.totalAmount);
            pstmt.setString(5, order.status);
            pstmt.setString(6, order.purchaser);
            pstmt.setString(7, order.approver);
            pstmt.setTimestamp(8, order.approvalTime);
            pstmt.setString(9, order.approvalRemark);
            pstmt.setString(10, order.remark);
            pstmt.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(12, order.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新采购订单状态
     *
     * @param id     订单ID
     * @param status 新状态
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE purchase_orders SET status = ?, update_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(3, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 审批采购订单
     *
     * @param id             订单ID
     * @param approver       审批人
     * @param approvalRemark 审批意见
     * @param status         审批状态（approved-已审批，rejected-已拒绝）
     * @return 是否审批成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean approve(int id, String approver, String approvalRemark, String status) throws SQLException {
        String sql = "UPDATE purchase_orders SET status = ?, approver = ?, approval_time = ?, approval_remark = ?, update_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setString(2, approver);
            pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(4, approvalRemark);
            pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(6, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除采购订单
     *
     * @param id 订单ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean delete(int id) throws SQLException {
        // 检查是否有入库单引用此订单
        if (hasInboundRecords(id)) {
            throw new SQLException("该采购订单存在入库记录，无法删除。请先删除相关入库记录。");
        }

        String sql = "DELETE FROM purchase_orders WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 检查采购订单是否有入库记录
     *
     * @param orderId 订单ID
     * @return 如果有入库记录返回 true
     * @throws SQLException 数据库操作异常
     */
    public static boolean hasInboundRecords(int orderId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM purchase_inbound WHERE order_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * 统计采购订单数量
     *
     * @param status 订单状态（可为null）
     * @return 订单数量
     * @throws SQLException 数据库操作异常
     */
    public static int countByStatus(String status) throws SQLException {
        String sql;
        if (status == null || status.isEmpty()) {
            sql = "SELECT COUNT(*) as count FROM purchase_orders";
        } else {
            sql = "SELECT COUNT(*) as count FROM purchase_orders WHERE status = ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (status != null && !status.isEmpty()) {
                pstmt.setString(1, status);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    /**
     * 批量插入采购订单
     *
     * @param orders 采购订单列表
     * @throws SQLException 数据库操作异常
     */
    public static void batchInsert(List<PurchaseOrder> orders) throws SQLException {
        String sql = "INSERT INTO purchase_orders (order_no, supplier_id, purchase_date, expected_date, " +
                     "total_amount, status, purchaser, approver, approval_time, approval_remark, remark, create_time, update_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (PurchaseOrder order : orders) {
                pstmt.setString(1, order.orderNo);
                pstmt.setInt(2, order.supplierId);
                pstmt.setString(3, order.purchaseDate);
                pstmt.setString(4, order.expectedDate);
                pstmt.setBigDecimal(5, order.totalAmount);
                pstmt.setString(6, order.status);
                pstmt.setString(7, order.purchaser);
                pstmt.setString(8, order.approver);
                pstmt.setTimestamp(9, order.approvalTime);
                pstmt.setString(10, order.approvalRemark);
                pstmt.setString(11, order.remark);
                pstmt.setTimestamp(12, order.createTime);
                pstmt.setTimestamp(13, order.updateTime);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 PurchaseOrder 对象
     *
     * @param rs 结果集
     * @return 采购订单对象
     * @throws SQLException 数据库操作异常
     */
    private static PurchaseOrder mapRowToPurchaseOrder(ResultSet rs) throws SQLException {
        PurchaseOrder order = new PurchaseOrder();
        order.id = rs.getInt("id");
        order.orderNo = rs.getString("order_no");
        order.supplierId = rs.getInt("supplier_id");
        order.supplierName = rs.getString("supplier_name");
        order.purchaseDate = rs.getString("purchase_date");
        order.expectedDate = rs.getString("expected_date");
        order.totalAmount = rs.getBigDecimal("total_amount");
        order.status = rs.getString("status");
        order.purchaser = rs.getString("purchaser");
        order.approver = rs.getString("approver");
        order.approvalTime = rs.getTimestamp("approval_time");
        order.approvalRemark = rs.getString("approval_remark");
        order.remark = rs.getString("remark");
        order.createTime = rs.getTimestamp("create_time");
        order.updateTime = rs.getTimestamp("update_time");
        return order;
    }
}