package com.cashier.dao;

import com.cashier.model.PurchaseInbound;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购入库记录数据访问对象
 * 负责采购入库记录相关的数据库操作
 */
public class PurchaseInboundDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PurchaseInboundDAO.class);

    /**
     * 根据ID查找采购入库记录
     *
     * @param id 入库ID
     * @return 采购入库记录对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseInbound findById(int id) throws SQLException {
        String sql = "SELECT pi.id, pi.inbound_no, pi.order_id, po.order_no, pi.inbound_date, pi.total_quantity, pi.total_amount, pi.operator, pi.remark, pi.create_time " +
                     "FROM purchase_inbound pi LEFT JOIN purchase_orders po ON pi.order_id = po.id WHERE pi.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseInbound(rs);
            }
        }
        return null;
    }

    /**
     * 查询所有采购入库记录
     *
     * @return 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseInbound> findAll() throws SQLException {
        List<PurchaseInbound> inboundList = new ArrayList<>();
        String sql = "SELECT pi.id, pi.inbound_no, pi.order_id, po.order_no, pi.inbound_date, pi.total_quantity, pi.total_amount, pi.operator, pi.remark, pi.create_time " +
                     "FROM purchase_inbound pi LEFT JOIN purchase_orders po ON pi.order_id = po.id ORDER BY pi.create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                inboundList.add(mapRowToPurchaseInbound(rs));
            }
        }
        return inboundList;
    }

    /**
     * 根据入库单号查找采购入库记录
     *
     * @param inboundNo 入库单号
     * @return 采购入库记录对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseInbound findByInboundNo(String inboundNo) throws SQLException {
        String sql = "SELECT pi.id, pi.inbound_no, pi.order_id, po.order_no, pi.inbound_date, pi.total_quantity, pi.total_amount, pi.operator, pi.remark, pi.create_time " +
                     "FROM purchase_inbound pi LEFT JOIN purchase_orders po ON pi.order_id = po.id WHERE pi.inbound_no = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, inboundNo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseInbound(rs);
            }
        }
        return null;
    }

    /**
     * 根据订单ID查找采购入库记录
     *
     * @param orderId 订单ID
     * @return 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseInbound> findByOrderId(int orderId) throws SQLException {
        List<PurchaseInbound> inboundList = new ArrayList<>();
        String sql = "SELECT pi.id, pi.inbound_no, pi.order_id, po.order_no, pi.inbound_date, pi.total_quantity, pi.total_amount, pi.operator, pi.remark, pi.create_time " +
                     "FROM purchase_inbound pi LEFT JOIN purchase_orders po ON pi.order_id = po.id WHERE pi.order_id = ? ORDER BY pi.create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                inboundList.add(mapRowToPurchaseInbound(rs));
            }
        }
        return inboundList;
    }

    /**
     * 根据操作人查找采购入库记录
     *
     * @param operator 操作人
     * @return 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseInbound> findByOperator(String operator) throws SQLException {
        List<PurchaseInbound> inboundList = new ArrayList<>();
        String sql = "SELECT pi.id, pi.inbound_no, pi.order_id, po.order_no, pi.inbound_date, pi.total_quantity, pi.total_amount, pi.operator, pi.remark, pi.create_time " +
                     "FROM purchase_inbound pi LEFT JOIN purchase_orders po ON pi.order_id = po.id WHERE pi.operator = ? ORDER BY pi.create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, operator);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                inboundList.add(mapRowToPurchaseInbound(rs));
            }
        }
        return inboundList;
    }

    /**
     * 根据日期范围查找采购入库记录
     *
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseInbound> findByDateRange(String startDate, String endDate) throws SQLException {
        List<PurchaseInbound> inboundList = new ArrayList<>();
        String sql = "SELECT pi.id, pi.inbound_no, pi.order_id, po.order_no, pi.inbound_date, pi.total_quantity, pi.total_amount, pi.operator, pi.remark, pi.create_time " +
                     "FROM purchase_inbound pi LEFT JOIN purchase_orders po ON pi.order_id = po.id WHERE pi.inbound_date BETWEEN ? AND ? ORDER BY pi.create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                inboundList.add(mapRowToPurchaseInbound(rs));
            }
        }
        return inboundList;
    }

    /**
     * 插入新采购入库记录
     *
     * @param inbound 采购入库记录对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insert(PurchaseInbound inbound) throws SQLException {
        String sql = "INSERT INTO purchase_inbound (inbound_no, order_id, inbound_date, total_quantity, total_amount, operator, remark, create_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, inbound.inboundNo);
            pstmt.setInt(2, inbound.orderId);
            pstmt.setString(3, inbound.inboundDate);
            pstmt.setInt(4, inbound.totalQuantity);
            pstmt.setBigDecimal(5, inbound.totalAmount);
            pstmt.setString(6, inbound.operator);
            pstmt.setString(7, inbound.remark);
            pstmt.setTimestamp(8, inbound.createTime);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        inbound.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新采购入库记录
     *
     * @param inbound 采购入库记录对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean update(PurchaseInbound inbound) throws SQLException {
        String sql = "UPDATE purchase_inbound SET inbound_no = ?, order_id = ?, inbound_date = ?, " +
                     "total_quantity = ?, total_amount = ?, operator = ?, remark = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, inbound.inboundNo);
            pstmt.setInt(2, inbound.orderId);
            pstmt.setString(3, inbound.inboundDate);
            pstmt.setInt(4, inbound.totalQuantity);
            pstmt.setBigDecimal(5, inbound.totalAmount);
            pstmt.setString(6, inbound.operator);
            pstmt.setString(7, inbound.remark);
            pstmt.setInt(8, inbound.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除采购入库记录
     *
     * @param id 入库ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM purchase_inbound WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入采购入库记录
     *
     * @param inboundList 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public static void batchInsert(List<PurchaseInbound> inboundList) throws SQLException {
        String sql = "INSERT INTO purchase_inbound (inbound_no, order_id, inbound_date, total_quantity, total_amount, operator, remark, create_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (PurchaseInbound inbound : inboundList) {
                pstmt.setString(1, inbound.inboundNo);
                pstmt.setInt(2, inbound.orderId);
                pstmt.setString(3, inbound.inboundDate);
                pstmt.setInt(4, inbound.totalQuantity);
                pstmt.setBigDecimal(5, inbound.totalAmount);
                pstmt.setString(6, inbound.operator);
                pstmt.setString(7, inbound.remark);
                pstmt.setTimestamp(8, inbound.createTime);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 统计入库记录数量
     *
     * @param startDate 开始日期（可为null）
     * @param endDate   结束日期（可为null）
     * @return 记录数量
     * @throws SQLException 数据库操作异常
     */
    public static int countByDateRange(String startDate, String endDate) throws SQLException {
        String sql;
        if (startDate == null || startDate.isEmpty()) {
            sql = "SELECT COUNT(*) as count FROM purchase_inbound";
        } else {
            sql = "SELECT COUNT(*) as count FROM purchase_inbound WHERE inbound_date BETWEEN ? AND ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (startDate != null && !startDate.isEmpty()) {
                pstmt.setString(1, startDate);
                pstmt.setString(2, endDate);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    /**
     * 统计入库总数量和总金额
     *
     * @param startDate 开始日期（可为null）
     * @param endDate   结束日期（可为null）
     * @return 数组，[0]=总数量，[1]=总金额
     * @throws SQLException 数据库操作异常
     */
    public static Object[] sumByDateRange(String startDate, String endDate) throws SQLException {
        String sql;
        if (startDate == null || startDate.isEmpty()) {
            sql = "SELECT SUM(total_quantity) as total_quantity, SUM(total_amount) as total_amount FROM purchase_inbound";
        } else {
            sql = "SELECT SUM(total_quantity) as total_quantity, SUM(total_amount) as total_amount " +
                  "FROM purchase_inbound WHERE inbound_date BETWEEN ? AND ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (startDate != null && !startDate.isEmpty()) {
                pstmt.setString(1, startDate);
                pstmt.setString(2, endDate);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Object[]{
                    rs.getInt("total_quantity"),
                    rs.getBigDecimal("total_amount")
                };
            }
        }
        return new Object[]{0, BigDecimal.ZERO};
    }

    /**
     * 将 ResultSet 映射为 PurchaseInbound 对象
     *
     * @param rs 结果集
     * @return 采购入库记录对象
     * @throws SQLException 数据库操作异常
     */
    private static PurchaseInbound mapRowToPurchaseInbound(ResultSet rs) throws SQLException {
        PurchaseInbound inbound = new PurchaseInbound();
        inbound.id = rs.getInt("id");
        inbound.inboundNo = rs.getString("inbound_no");
        inbound.orderId = rs.getInt("order_id");
        inbound.orderNo = rs.getString("order_no");
        inbound.inboundDate = rs.getString("inbound_date");
        inbound.totalQuantity = rs.getInt("total_quantity");
        inbound.totalAmount = rs.getBigDecimal("total_amount");
        inbound.operator = rs.getString("operator");
        inbound.remark = rs.getString("remark");
        inbound.createTime = rs.getTimestamp("create_time");
        return inbound;
    }
}