package com.cashier.dao;

import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.Map;

/**
 * 系统设置数据访问对象
 * 负责系统设置相关的数据库操作
 */
public class SystemSettingsDAO {

    /**
     * 获取税率设置
     */
    public static double getTaxRate() throws SQLException {
        String sql = "SELECT value FROM settings WHERE `key` = 'taxRate'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("value");
            }
        }
        return 0.0; // 默认税率
    }

    /**
     * 设置税率
     */
    public static boolean setTaxRate(double taxRate) throws SQLException {
        String sql = "INSERT INTO settings (`key`, value) VALUES ('taxRate', ?) " +
                     "ON DUPLICATE KEY UPDATE value = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, taxRate);
            pstmt.setDouble(2, taxRate);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 获取交易计数
     */
    public static int getTransactionCount() throws SQLException {
        String sql = "SELECT value FROM settings WHERE `key` = 'transactionCount'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("value");
            }
        }
        return 0; // 默认计数
    }

    /**
     * 设置交易计数
     */
    public static boolean setTransactionCount(int count) throws SQLException {
        String sql = "INSERT INTO settings (`key`, value) VALUES ('transactionCount', ?) " +
                     "ON DUPLICATE KEY UPDATE value = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, count);
            pstmt.setInt(2, count);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 增加交易计数
     */
    public static boolean incrementTransactionCount() throws SQLException {
        String sql = "INSERT INTO settings (`key`, value) VALUES ('transactionCount', 1) " +
                     "ON DUPLICATE KEY UPDATE value = CAST(CAST(value AS UNSIGNED) + 1 AS CHAR)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 获取指定设置值
     */
    public static String getSetting(String key) throws SQLException {
        String sql = "SELECT value FROM settings WHERE `key` = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        }
        return null;
    }

    /**
     * 设置指定值
     */
    public static boolean setSetting(String key, String value) throws SQLException {
        String sql = "INSERT INTO settings (`key`, value) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE value = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.setString(3, value);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除指定设置
     */
    public static boolean deleteSetting(String key) throws SQLException {
        String sql = "DELETE FROM settings WHERE `key` = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 获取所有设置
     */
    public static java.util.Map<String, String> getAllSettings() throws SQLException {
        java.util.Map<String, String> settings = new java.util.HashMap<>();
        String sql = "SELECT `key`, value FROM settings";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                settings.put(rs.getString("key"), rs.getString("value"));
            }
        }
        return settings;
    }
}