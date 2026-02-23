package com.cashier.dao;

import com.cashier.model.RechargeRecord;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 充值记录数据访问对象
 * 负责充值记录相关的数据库操作
 */
public class RechargeRecordDAO {

    /**
     * 查询所有充值记录
     */
    public static List<RechargeRecord> findAll() throws SQLException {
        List<RechargeRecord> records = new ArrayList<>();
        String sql = "SELECT record_id, member_phone, member_name, amount, payment_method, " +
                     "timestamp, operator FROM recharge_records ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(mapRowToRechargeRecord(rs));
            }
        }
        return records;
    }

    /**
     * 根据ID查找充值记录
     */
    public static RechargeRecord findById(String recordId) throws SQLException {
        String sql = "SELECT record_id, member_phone, member_name, amount, payment_method, " +
                     "timestamp, operator FROM recharge_records WHERE record_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, recordId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToRechargeRecord(rs);
            }
        }
        return null;
    }

    /**
     * 根据会员手机号查找充值记录
     */
    public static List<RechargeRecord> findByMemberPhone(String memberPhone) throws SQLException {
        List<RechargeRecord> records = new ArrayList<>();
        String sql = "SELECT record_id, member_phone, member_name, amount, payment_method, " +
                     "timestamp, operator FROM recharge_records WHERE member_phone = ? " +
                     "ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, memberPhone);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                records.add(mapRowToRechargeRecord(rs));
            }
        }
        return records;
    }

    /**
     * 根据日期范围查找充值记录
     */
    public static List<RechargeRecord> findByDateRange(java.util.Date startDate, java.util.Date endDate) throws SQLException {
        List<RechargeRecord> records = new ArrayList<>();
        String sql = "SELECT record_id, member_phone, member_name, amount, payment_method, " +
                     "timestamp, operator FROM recharge_records " +
                     "WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, new Timestamp(startDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                records.add(mapRowToRechargeRecord(rs));
            }
        }
        return records;
    }

    /**
     * 插入新充值记录
     */
    public static boolean insert(RechargeRecord record) throws SQLException {
        String sql = "INSERT INTO recharge_records (record_id, member_phone, member_name, " +
                     "amount, payment_method, timestamp, operator) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, record.recordId);
            pstmt.setString(2, record.memberPhone);
            pstmt.setString(3, record.memberName);
            pstmt.setDouble(4, record.amount);
            pstmt.setString(5, record.paymentMethod);
            pstmt.setTimestamp(6, new Timestamp(record.timestamp.getTime()));
            pstmt.setString(7, record.operator);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 使用指定的数据库连接插入充值记录
     * @param conn 数据库连接
     * @param record 充值记录
     * @return 如果插入成功返回true，否则返回false
     * @throws SQLException 数据库操作异常
     */
    public static boolean insertWithConnection(Connection conn, RechargeRecord record) throws SQLException {
        String sql = "INSERT INTO recharges (member_phone, member_name, " +
                     "amount, payment_method, timestamp, operator) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.memberPhone);
            pstmt.setString(2, record.memberName);
            pstmt.setDouble(3, record.amount);
            pstmt.setString(4, record.paymentMethod);
            pstmt.setTimestamp(5, new Timestamp(record.timestamp.getTime()));
            pstmt.setString(6, record.operator);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入充值记录
     */
    public static void batchInsert(List<RechargeRecord> records) throws SQLException {
        String sql = "INSERT INTO recharge_records (record_id, member_phone, member_name, " +
                     "amount, payment_method, timestamp, operator) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (RechargeRecord record : records) {
                pstmt.setString(1, record.recordId);
                pstmt.setString(2, record.memberPhone);
                pstmt.setString(3, record.memberName);
                pstmt.setDouble(4, record.amount);
                pstmt.setString(5, record.paymentMethod);
                pstmt.setTimestamp(6, new Timestamp(record.timestamp.getTime()));
                pstmt.setString(7, record.operator);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 RechargeRecord 对象
     */
    private static RechargeRecord mapRowToRechargeRecord(ResultSet rs) throws SQLException {
        RechargeRecord record = new RechargeRecord();
        record.recordId = rs.getString("record_id");
        record.memberPhone = rs.getString("member_phone");
        record.memberName = rs.getString("member_name");
        record.amount = rs.getDouble("amount");
        record.paymentMethod = rs.getString("payment_method");
        record.timestamp = rs.getTimestamp("timestamp");
        record.operator = rs.getString("operator");
        return record;
    }
}