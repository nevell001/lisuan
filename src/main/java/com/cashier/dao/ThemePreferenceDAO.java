package com.cashier.dao;

import com.cashier.util.DatabaseManager;

import java.sql.*;

/**
 * 主题偏好数据访问对象
 * 负责主题偏好相关的数据库操作
 */
public class ThemePreferenceDAO {

    /**
     * 获取主题偏好
     */
    public static String getThemePreference(String username) throws SQLException {
        String sql = "SELECT theme_name FROM theme_preferences WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("theme_name");
            }
        }
        return "light"; // 默认主题
    }

    /**
     * 设置主题偏好
     */
    public static boolean setThemePreference(String username, String themeName) throws SQLException {
        String sql = "INSERT INTO theme_preferences (username, theme_name, updated_at) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE theme_name = ?, updated_at = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            long now = System.currentTimeMillis();
            pstmt.setString(1, username);
            pstmt.setString(2, themeName);
            pstmt.setLong(3, now);
            pstmt.setString(4, themeName);
            pstmt.setLong(5, now);
            return pstmt.executeUpdate() > 0;
        }
    }
}