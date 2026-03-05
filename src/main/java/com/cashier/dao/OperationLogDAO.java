package com.cashier.dao;

import com.cashier.model.OperationLog;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 操作日志数据访问对象
 * 负责操作日志相关的数据库操作
 */
public class OperationLogDAO {

    /**
     * 查询所有操作日志
     */
    public static List<OperationLog> findAll() throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT username, operation, details, timestamp, ip_address " +
                     "FROM operation_logs ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logs.add(mapRowToOperationLog(rs));
            }
        }
        return logs;
    }

    /**
     * 根据ID查找操作日志
     */
    public static OperationLog findById(String logId) throws SQLException {
        String sql = "SELECT username, operation, details, timestamp, ip_address " +
                     "FROM operation_logs WHERE log_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, logId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToOperationLog(rs);
            }
        }
        return null;
    }

    /**
     * 根据用户名查找操作日志
     */
    public static List<OperationLog> findByUsername(String username) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT username, operation, details, timestamp, ip_address " +
                     "FROM operation_logs WHERE username = ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapRowToOperationLog(rs));
            }
        }
        return logs;
    }

    /**
     * 根据操作类型查找操作日志
     */
    public static List<OperationLog> findByOperation(String operation) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT username, operation, details, timestamp, ip_address " +
                     "FROM operation_logs WHERE operation = ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, operation);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapRowToOperationLog(rs));
            }
        }
        return logs;
    }

    /**
     * 根据日期范围查找操作日志
     */
    public static List<OperationLog> findByDateRange(java.util.Date startDate, java.util.Date endDate) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT username, operation, details, timestamp, ip_address " +
                     "FROM operation_logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, new Timestamp(startDate.getTime()));
            pstmt.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapRowToOperationLog(rs));
            }
        }
        return logs;
    }

    /**
     * 插入新操作日志
     */
    public static boolean insert(OperationLog log) throws SQLException {
        String sql = "INSERT INTO operation_logs (username, operation, details, timestamp, ip_address) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            
            pstmt.setString(1, log.username);
            pstmt.setString(2, log.operation);
            pstmt.setString(3, log.details);
            pstmt.setTimestamp(4, new Timestamp(log.timestamp.getTime()));
            pstmt.setString(5, log.ipAddress);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入操作日志
     */
    public static void batchInsert(List<OperationLog> logs) throws SQLException {
        String sql = "INSERT INTO operation_logs (username, operation, details, timestamp, ip_address) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (OperationLog log : logs) {
                
                pstmt.setString(1, log.username);
                pstmt.setString(2, log.operation);
                pstmt.setString(3, log.details);
                pstmt.setTimestamp(4, new Timestamp(log.timestamp.getTime()));
                pstmt.setString(5, log.ipAddress);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 删除指定日期之前的日志
     */
    public static boolean deleteBeforeDate(java.util.Date date) throws SQLException {
        String sql = "DELETE FROM operation_logs WHERE timestamp < ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, new Timestamp(date.getTime()));
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 将 ResultSet 映射为 OperationLog 对象
     */
    private static OperationLog mapRowToOperationLog(ResultSet rs) throws SQLException {
        OperationLog log = new OperationLog();
        log.logId = rs.getString("log_id");
        log.username = rs.getString("username");
        log.operation = rs.getString("operation");
        log.details = rs.getString("details");
        log.timestamp = rs.getTimestamp("timestamp");
        log.ipAddress = rs.getString("ip_address");
        return log;
    }
}