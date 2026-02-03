package com.cashier.dao;

import com.cashier.model.Shift;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 班次数据访问对象
 * 负责班次相关的数据库操作
 */
public class ShiftDAO {

    /**
     * 插入新班次
     */
    public static boolean insert(Shift shift) throws SQLException {
        String sql = "INSERT INTO shifts (shift_id, operator_username, operator_name, start_time, end_time, " +
                     "opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, " +
                     "closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, " +
                     "alipay_revenue, card_revenue, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, shift.shiftId);
            pstmt.setString(2, shift.username);
            pstmt.setString(3, shift.operatorName);
            pstmt.setLong(4, shift.startTime.getTime());
            pstmt.setLong(5, shift.endTime.getTime());
            pstmt.setDouble(6, shift.openingRevenue);
            pstmt.setDouble(7, shift.closingRevenue);
            pstmt.setDouble(8, shift.shiftRevenue);
            pstmt.setInt(9, shift.openingTransactionCount);
            pstmt.setInt(10, shift.closingTransactionCount);
            pstmt.setInt(11, shift.shiftTransactionCount);
            pstmt.setDouble(12, shift.cashRevenue);
            pstmt.setDouble(13, shift.wechatRevenue);
            pstmt.setDouble(14, shift.alipayRevenue);
            pstmt.setDouble(15, shift.cardRevenue);
            pstmt.setString(16, shift.notes);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新班次
     */
    public static boolean update(Shift shift) throws SQLException {
        String sql = "UPDATE shifts SET end_time = ?, closing_revenue = ?, shift_revenue = ?, " +
                     "closing_transaction_count = ?, shift_transaction_count = ?, " +
                     "cash_revenue = ?, wechat_revenue = ?, alipay_revenue = ?, card_revenue = ?, " +
                     "notes = ? WHERE shift_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, shift.endTime.getTime());
            pstmt.setDouble(2, shift.closingRevenue);
            pstmt.setDouble(3, shift.shiftRevenue);
            pstmt.setInt(4, shift.closingTransactionCount);
            pstmt.setInt(5, shift.shiftTransactionCount);
            pstmt.setDouble(6, shift.cashRevenue);
            pstmt.setDouble(7, shift.wechatRevenue);
            pstmt.setDouble(8, shift.alipayRevenue);
            pstmt.setDouble(9, shift.cardRevenue);
            pstmt.setString(10, shift.notes);
            pstmt.setString(11, shift.shiftId);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据班次ID查找班次
     */
    public static Shift findById(String shiftId) throws SQLException {
        String sql = "SELECT shift_id, operator_username, operator_name, start_time, end_time, " +
                     "opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, " +
                     "closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, " +
                     "alipay_revenue, card_revenue, notes FROM shifts WHERE shift_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, shiftId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToShift(rs);
            }
        }
        return null;
    }

    /**
     * 查询所有班次
     */
    public static List<Shift> findAll() throws SQLException {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT shift_id, operator_username, operator_name, start_time, end_time, " +
                     "opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, " +
                     "closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, " +
                     "alipay_revenue, card_revenue, notes FROM shifts ORDER BY start_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                shifts.add(mapRowToShift(rs));
            }
        }
        return shifts;
    }

    /**
     * 查找活跃班次（未结束的班次）
     */
    public static Shift findActiveShift() throws SQLException {
        String sql = "SELECT shift_id, operator_username, operator_name, start_time, end_time, " +
                     "opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, " +
                     "closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, " +
                     "alipay_revenue, card_revenue, notes FROM shifts " +
                     "WHERE end_time = start_time ORDER BY start_time DESC LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return mapRowToShift(rs);
            }
        }
        return null;
    }

    /**
     * 检查是否有活跃班次
     */
    public static boolean hasActiveShift() throws SQLException {
        String sql = "SELECT COUNT(*) FROM shifts WHERE end_time = start_time";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * 按操作员查询班次
     */
    public static List<Shift> findByOperator(String username) throws SQLException {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT shift_id, operator_username, operator_name, start_time, end_time, " +
                     "opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, " +
                     "closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, " +
                     "alipay_revenue, card_revenue, notes FROM shifts " +
                     "WHERE operator_username = ? ORDER BY start_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                shifts.add(mapRowToShift(rs));
            }
        }
        return shifts;
    }

    /**
     * 删除班次
     */
    public static boolean delete(String shiftId) throws SQLException {
        String sql = "DELETE FROM shifts WHERE shift_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, shiftId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入班次
     */
    public static void batchInsert(List<Shift> shifts) throws SQLException {
        String sql = "INSERT INTO shifts (shift_id, operator_username, operator_name, start_time, end_time, " +
                     "opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, " +
                     "closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, " +
                     "alipay_revenue, card_revenue, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Shift shift : shifts) {
                pstmt.setString(1, shift.shiftId);
                pstmt.setString(2, shift.username);
                pstmt.setString(3, shift.operatorName);
                pstmt.setLong(4, shift.startTime.getTime());
                pstmt.setLong(5, shift.endTime.getTime());
                pstmt.setDouble(6, shift.openingRevenue);
                pstmt.setDouble(7, shift.closingRevenue);
                pstmt.setDouble(8, shift.shiftRevenue);
                pstmt.setInt(9, shift.openingTransactionCount);
                pstmt.setInt(10, shift.closingTransactionCount);
                pstmt.setInt(11, shift.shiftTransactionCount);
                pstmt.setDouble(12, shift.cashRevenue);
                pstmt.setDouble(13, shift.wechatRevenue);
                pstmt.setDouble(14, shift.alipayRevenue);
                pstmt.setDouble(15, shift.cardRevenue);
                pstmt.setString(16, shift.notes);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 Shift 对象
     */
    private static Shift mapRowToShift(ResultSet rs) throws SQLException {
        Shift shift = new Shift();
        shift.shiftId = rs.getString("shift_id");
        shift.username = rs.getString("operator_username");
        shift.operatorName = rs.getString("operator_name");

        long startTime = rs.getLong("start_time");
        shift.startTime = new java.util.Date(startTime);

        long endTime = rs.getLong("end_time");
        shift.endTime = new java.util.Date(endTime);

        shift.openingRevenue = rs.getDouble("opening_revenue");
        shift.closingRevenue = rs.getDouble("closing_revenue");
        shift.shiftRevenue = rs.getDouble("shift_revenue");
        shift.openingTransactionCount = rs.getInt("opening_transaction_count");
        shift.closingTransactionCount = rs.getInt("closing_transaction_count");
        shift.shiftTransactionCount = rs.getInt("shift_transaction_count");
        shift.cashRevenue = rs.getDouble("cash_revenue");
        shift.wechatRevenue = rs.getDouble("wechat_revenue");
        shift.alipayRevenue = rs.getDouble("alipay_revenue");
        shift.cardRevenue = rs.getDouble("card_revenue");
        shift.notes = rs.getString("notes");

        return shift;
    }
}
