package com.cashier.dao;

import com.cashier.model.HoldOrder;
import com.cashier.model.PageResult;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 挂单数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class HoldOrderDAORefactored extends BaseDAO {

    private static final RowMapper<HoldOrder> ORDER_MAPPER = new RowMapper<HoldOrder>() {
        @Override
        public HoldOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
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
    };

    /**
     * 创建挂单表
     */
    public void createTable() throws SQLException {
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
                INDEX idx_hold_date (hold_date),
                INDEX idx_user_status_hold (user_id, status, hold_date, hold_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * 插入挂单
     *
     * @return 受影响行数
     */
    public int insert(HoldOrder holdOrder) throws SQLException {
        String sql = """
            INSERT INTO hold_orders (order_number, user_id, member_id, member_name, member_phone,
                total_amount, discount_amount, final_amount, item_count, items_json,
                hold_date, hold_time, notes, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURDATE(), CURTIME(), ?, ?)
            """;

        try (Connection conn = getConnection();
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
     *
     * @return 受影响行数
     */
    public int updateStatus(int id, int status) throws SQLException {
        return executeUpdate("UPDATE hold_orders SET status = ? WHERE id = ?", status, id);
    }

    /**
     * 仅当当前状态等于 {@code expected} 时才更新（取单/取消的 CAS）。
     *
     * @return 受影响行数；0 表示记录不存在或状态已被其他终端改变
     */
    public int updateStatusIfCurrent(int id, int expected, int target) throws SQLException {
        return executeUpdate(
            "UPDATE hold_orders SET status = ? WHERE id = ? AND status = ?",
            target, id, expected);
    }

    /**
     * 分页查询挂单，按挂单时间倒序。
     *
     * @param userId 为 null 时查全部用户（仅管理员使用）
     * @param status 为 null 时不限状态
     */
    public PageResult<HoldOrder> findPage(Integer userId, Integer status, int pageNum, int pageSize)
            throws SQLException {
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.max(1, pageSize);
        List<Object> params = new ArrayList<>();
        String where = buildWhere(userId, status, params);
        long total = count(userId, status);

        params.add(safeSize);
        params.add((safePage - 1) * safeSize);
        List<HoldOrder> rows = queryList(
            "SELECT * FROM hold_orders" + where
                + " ORDER BY hold_date DESC, hold_time DESC, id DESC LIMIT ? OFFSET ?",
            ORDER_MAPPER, params.toArray());
        return new PageResult<>(rows, safePage, safeSize, total);
    }

    /** 与 {@link #findPage} 相同过滤条件的总数。 */
    public long count(Integer userId, Integer status) throws SQLException {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(userId, status, params);
        return queryLong("SELECT COUNT(*) FROM hold_orders" + where, params.toArray());
    }

    private static String buildWhere(Integer userId, Integer status, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (userId != null) {
            where.append(" AND user_id = ?");
            params.add(userId);
        }
        if (status != null) {
            where.append(" AND status = ?");
            params.add(status);
        }
        return where.toString();
    }

    /**
     * 删除挂单
     *
     * @return 受影响行数
     */
    public int delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM hold_orders WHERE id = ?", id);
    }

    /**
     * 根据ID获取挂单
     */
    public HoldOrder findById(int id) throws SQLException {
        return queryOneOrNull("SELECT * FROM hold_orders WHERE id = ?", ORDER_MAPPER, id);
    }

    /**
     * 根据订单号获取挂单
     */
    public HoldOrder findByOrderNumber(String orderNumber) throws SQLException {
        return queryOneOrNull("SELECT * FROM hold_orders WHERE order_number = ?", ORDER_MAPPER, orderNumber);
    }

    /**
     * 获取用户的所有挂单（挂单中状态）
     */
    public List<HoldOrder> findActiveByUserId(int userId) throws SQLException {
        return queryList(
            "SELECT * FROM hold_orders WHERE user_id = ? AND status = 0 ORDER BY hold_date DESC, hold_time DESC",
            ORDER_MAPPER, userId);
    }

    /**
     * 获取所有活跃挂单
     */
    public List<HoldOrder> findAllActive() throws SQLException {
        return queryList(
            "SELECT * FROM hold_orders WHERE status = 0 ORDER BY hold_date DESC, hold_time DESC",
            ORDER_MAPPER);
    }

    /**
     * 清理早于指定天数的已完成挂单（已取单/已取消）。
     *
     * <p>截止日由 Java 计算后作为参数传入，避免依赖 MySQL 专有的 {@code DATE_SUB}，
     * 测试库（H2）与生产库走同一条 SQL。</p>
     *
     * @return 受影响行数
     */
    public int deleteFinishedBefore(int days) throws SQLException {
        Date cutoff = Date.valueOf(LocalDate.now().minusDays(days));
        return executeUpdate(
            "DELETE FROM hold_orders WHERE status IN (1, 2) AND hold_date < ?", cutoff);
    }

    /**
     * 清理过期的挂单（超过指定天数）
     *
     * @return 受影响行数
     */
    public int cleanExpiredOrders(int days) throws SQLException {
        return executeUpdate(
            "DELETE FROM hold_orders WHERE status = 0 AND hold_date < DATE_SUB(CURDATE(), INTERVAL ? DAY)",
            days);
    }
}
