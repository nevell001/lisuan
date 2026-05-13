package com.cashier.dao;

import com.cashier.model.HoldOrder;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 挂单数据访问对象
 */
public class HoldOrderDAO {

    /**
     * 创建挂单表
     */
    public static void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS hold_orders (
                id INT PRIMARY KEY AUTO_INCREMENT,
                order_number VARCHAR(50) UNIQUE NOT NULL,
                user_id INT NOT NULL,
                member_id INT,
                member_name VARCHAR(100),
                member_phone VARCHAR(20),
                total_amount DECIMAL(10,2) DEFAULT 0,
                discount_amount DECIMAL(10,2) DEFAULT 0,
                final_amount DECIMAL(10,2) DEFAULT 0,
                item_count INT DEFAULT 0,
                items_json TEXT,
                hold_date DATE NOT NULL,
                hold_time TIME NOT NULL,
                notes VARCHAR(500),
                status INT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_user_id (user_id),
                INDEX idx_status (status),
                INDEX idx_hold_date (hold_date)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * 插入挂单
     */
    public static int insert(HoldOrder holdOrder) throws SQLException {
        String sql = """
            INSERT INTO hold_orders (order_number, user_id, member_id, member_name, member_phone,
                total_amount, discount_amount, final_amount, item_count, items_json,
                hold_date, hold_time, notes, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURDATE(), CURTIME(), ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, holdOrder.orderNumber);
            pstmt.setInt(2, holdOrder.userId);
            if (holdOrder.memberId != null) {
                pstmt.setInt(3, holdOrder.memberId);
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.setString(4, holdOrder.memberName);
            pstmt.setString(5, holdOrder.memberPhone);
            pstmt.setBigDecimal(6, holdOrder.totalAmount);
            pstmt.setBigDecimal(7, holdOrder.discountAmount);
            pstmt.setBigDecimal(8, holdOrder.finalAmount);
            pstmt.setInt(9, holdOrder.itemCount);
            pstmt.setString(10, holdOrder.itemsJson);
            pstmt.setString(11, holdOrder.notes);
            pstmt.setInt(12, holdOrder.status);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        holdOrder.id = rs.getInt(1);
                    }
                }
            }
            return affectedRows;
        }
    }

    /**
     * 更新挂单状态
     */
    public static int updateStatus(int id, int status) throws SQLException {
        String sql = "UPDATE hold_orders SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, status);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate();
        }
    }

    /**
     * 删除挂单
     */
    public static int delete(int id) throws SQLException {
        String sql = "DELETE FROM hold_orders WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate();
        }
    }

    /**
     * 根据ID获取挂单
     */
    public static HoldOrder findById(int id) throws SQLException {
        String sql = "SELECT * FROM hold_orders WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToHoldOrder(rs);
                }
            }
        }
        return null;
    }

    /**
     * 根据订单号获取挂单
     */
    public static HoldOrder findByOrderNumber(String orderNumber) throws SQLException {
        String sql = "SELECT * FROM hold_orders WHERE order_number = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, orderNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToHoldOrder(rs);
                }
            }
        }
        return null;
    }

    /**
     * 获取用户的所有挂单（挂单中状态）
     */
    public static List<HoldOrder> findActiveByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM hold_orders WHERE user_id = ? AND status = 0 ORDER BY hold_date DESC, hold_time DESC";

        List<HoldOrder> orders = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRowToHoldOrder(rs));
                }
            }
        }
        return orders;
    }

    /**
     * 获取所有活跃挂单
     */
    public static List<HoldOrder> findAllActive() throws SQLException {
        String sql = "SELECT * FROM hold_orders WHERE status = 0 ORDER BY hold_date DESC, hold_time DESC";

        List<HoldOrder> orders = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                orders.add(mapRowToHoldOrder(rs));
            }
        }
        return orders;
    }

    /**
     * 清理过期的挂单（超过指定天数）
     */
    public static int cleanExpiredOrders(int days) throws SQLException {
        String sql = "DELETE FROM hold_orders WHERE status = 0 AND hold_date < DATE_SUB(CURDATE(), INTERVAL ? DAY)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, days);
            return pstmt.executeUpdate();
        }
    }

    /**
     * 映射ResultSet到HoldOrder对象
     */
    private static HoldOrder mapRowToHoldOrder(ResultSet rs) throws SQLException {
        HoldOrder order = new HoldOrder();
        order.id = rs.getInt("id");
        order.orderNumber = rs.getString("order_number");
        order.userId = rs.getInt("user_id");
        
        int memberId = rs.getInt("member_id");
        if (!rs.wasNull()) {
            order.memberId = memberId;
        }
        
        order.memberName = rs.getString("member_name");
        order.memberPhone = rs.getString("member_phone");
        order.totalAmount = rs.getBigDecimal("total_amount");
        order.discountAmount = rs.getBigDecimal("discount_amount");
        order.finalAmount = rs.getBigDecimal("final_amount");
        order.itemCount = rs.getInt("item_count");
        order.itemsJson = rs.getString("items_json");
        order.holdDate = rs.getDate("hold_date");
        order.holdTime = rs.getTime("hold_time");
        order.notes = rs.getString("notes");
        order.status = rs.getInt("status");
        return order;
    }
}
