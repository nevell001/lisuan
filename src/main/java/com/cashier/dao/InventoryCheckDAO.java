package com.cashier.dao;

import com.cashier.model.InventoryCheck;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存盘点数据访问对象
 * 负责库存盘点相关的数据库操作
 */
public class InventoryCheckDAO {
    private static final Logger logger = LoggerFactory.getLogger(InventoryCheckDAO.class);

    /**
     * 根据ID查找库存盘点记录
     *
     * @param id 盘点ID
     * @return 库存盘点对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static InventoryCheck findById(int id) throws SQLException {
        String sql = "SELECT id, check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time " +
                     "FROM inventory_checks WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToInventoryCheck(rs);
            }
        }
        return null;
    }

    /**
     * 查询所有库存盘点记录
     *
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<InventoryCheck> findAll() throws SQLException {
        List<InventoryCheck> checks = new ArrayList<>();
        String sql = "SELECT id, check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time " +
                     "FROM inventory_checks ORDER BY create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                checks.add(mapRowToInventoryCheck(rs));
            }
        }
        return checks;
    }

    /**
     * 根据盘点单号查找库存盘点记录
     *
     * @param checkNo 盘点单号
     * @return 库存盘点对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static InventoryCheck findByCheckNo(String checkNo) throws SQLException {
        String sql = "SELECT id, check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time " +
                     "FROM inventory_checks WHERE check_no = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, checkNo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToInventoryCheck(rs);
            }
        }
        return null;
    }

    /**
     * 根据盘点类型查找库存盘点记录
     *
     * @param checkType 盘点类型（full-全盘，partial-部分盘点）
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<InventoryCheck> findByCheckType(String checkType) throws SQLException {
        List<InventoryCheck> checks = new ArrayList<>();
        String sql = "SELECT id, check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time " +
                     "FROM inventory_checks WHERE check_type = ? ORDER BY create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, checkType);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                checks.add(mapRowToInventoryCheck(rs));
            }
        }
        return checks;
    }

    /**
     * 根据状态查找库存盘点记录
     *
     * @param status 盘点状态（pending-待盘点，checking-盘点中，completed-已完成）
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<InventoryCheck> findByStatus(String status) throws SQLException {
        List<InventoryCheck> checks = new ArrayList<>();
        String sql = "SELECT id, check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time " +
                     "FROM inventory_checks WHERE status = ? ORDER BY create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                checks.add(mapRowToInventoryCheck(rs));
            }
        }
        return checks;
    }

    /**
     * 根据盘点人查找库存盘点记录
     *
     * @param operator 盘点人
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<InventoryCheck> findByOperator(String operator) throws SQLException {
        List<InventoryCheck> checks = new ArrayList<>();
        String sql = "SELECT id, check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time " +
                     "FROM inventory_checks WHERE operator = ? ORDER BY create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, operator);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                checks.add(mapRowToInventoryCheck(rs));
            }
        }
        return checks;
    }

    /**
     * 根据日期范围查找库存盘点记录
     *
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public static List<InventoryCheck> findByDateRange(String startDate, String endDate) throws SQLException {
        List<InventoryCheck> checks = new ArrayList<>();
        String sql = "SELECT id, check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time " +
                     "FROM inventory_checks WHERE check_date BETWEEN ? AND ? ORDER BY create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                checks.add(mapRowToInventoryCheck(rs));
            }
        }
        return checks;
    }

    /**
     * 插入新库存盘点记录
     *
     * @param check 库存盘点对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insert(InventoryCheck check) throws SQLException {
        String sql = "INSERT INTO inventory_checks (check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, check.checkNo);
            pstmt.setString(2, check.checkDate);
            pstmt.setString(3, check.checkType);
            pstmt.setInt(4, check.totalItems);
            pstmt.setInt(5, check.diffItems);
            pstmt.setString(6, check.status);
            pstmt.setString(7, check.operator);
            pstmt.setString(8, check.checker);
            pstmt.setString(9, check.remark);
            pstmt.setTimestamp(10, check.createTime);
            pstmt.setTimestamp(11, check.updateTime);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        check.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新库存盘点记录
     *
     * @param check 库存盘点对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean update(InventoryCheck check) throws SQLException {
        String sql = "UPDATE inventory_checks SET check_no = ?, check_date = ?, check_type = ?, " +
                     "total_items = ?, diff_items = ?, status = ?, operator = ?, checker = ?, remark = ?, update_time = ? " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, check.checkNo);
            pstmt.setString(2, check.checkDate);
            pstmt.setString(3, check.checkType);
            pstmt.setInt(4, check.totalItems);
            pstmt.setInt(5, check.diffItems);
            pstmt.setString(6, check.status);
            pstmt.setString(7, check.operator);
            pstmt.setString(8, check.checker);
            pstmt.setString(9, check.remark);
            pstmt.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(11, check.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新库存盘点状态
     *
     * @param id     盘点ID
     * @param status 新状态
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE inventory_checks SET status = ?, update_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(3, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新盘点统计信息
     *
     * @param id         盘点ID
     * @param totalItems 总商品数
     * @param diffItems  差异数
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean updateStatistics(int id, int totalItems, int diffItems) throws SQLException {
        String sql = "UPDATE inventory_checks SET total_items = ?, diff_items = ?, update_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, totalItems);
            pstmt.setInt(2, diffItems);
            pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(4, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 完成盘点
     *
     * @param id      盘点ID
     * @param checker 审核人
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean complete(int id, String checker) throws SQLException {
        String sql = "UPDATE inventory_checks SET status = 'completed', checker = ?, update_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, checker);
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(3, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除库存盘点记录
     *
     * @param id 盘点ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM inventory_checks WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入库存盘点记录
     *
     * @param checks 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public static void batchInsert(List<InventoryCheck> checks) throws SQLException {
        String sql = "INSERT INTO inventory_checks (check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (InventoryCheck check : checks) {
                pstmt.setString(1, check.checkNo);
                pstmt.setString(2, check.checkDate);
                pstmt.setString(3, check.checkType);
                pstmt.setInt(4, check.totalItems);
                pstmt.setInt(5, check.diffItems);
                pstmt.setString(6, check.status);
                pstmt.setString(7, check.operator);
                pstmt.setString(8, check.checker);
                pstmt.setString(9, check.remark);
                pstmt.setTimestamp(10, check.createTime);
                pstmt.setTimestamp(11, check.updateTime);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 统计盘点记录数量
     *
     * @param status 盘点状态（可为null）
     * @return 记录数量
     * @throws SQLException 数据库操作异常
     */
    public static int countByStatus(String status) throws SQLException {
        String sql;
        if (status == null || status.isEmpty()) {
            sql = "SELECT COUNT(*) as count FROM inventory_checks";
        } else {
            sql = "SELECT COUNT(*) as count FROM inventory_checks WHERE status = ?";
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
     * 将 ResultSet 映射为 InventoryCheck 对象
     *
     * @param rs 结果集
     * @return 库存盘点对象
     * @throws SQLException 数据库操作异常
     */
    private static InventoryCheck mapRowToInventoryCheck(ResultSet rs) throws SQLException {
        InventoryCheck check = new InventoryCheck();
        check.id = rs.getInt("id");
        check.checkNo = rs.getString("check_no");
        check.checkDate = rs.getString("check_date");
        check.checkType = rs.getString("check_type");
        check.totalItems = rs.getInt("total_items");
        check.diffItems = rs.getInt("diff_items");
        check.status = rs.getString("status");
        check.operator = rs.getString("operator");
        check.checker = rs.getString("checker");
        check.remark = rs.getString("remark");
        check.createTime = rs.getTimestamp("create_time");
        check.updateTime = rs.getTimestamp("update_time");
        return check;
    }
}