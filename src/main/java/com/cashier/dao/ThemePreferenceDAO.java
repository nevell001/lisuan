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
    public static String getThemePreference() throws SQLException {
        String sql = "SELECT theme_name FROM theme_preferences WHERE id = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
    public static boolean setThemePreference(String themeName) throws SQLException {
        String sql = "INSERT INTO theme_preferences (id, theme_name, updated_at) VALUES (1, ?, NOW()) " +
                     "ON DUPLICATE KEY UPDATE theme_name = ?, updated_at = NOW()";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, themeName);
            pstmt.setString(2, themeName);
            return pstmt.executeUpdate() > 0;
        }
    }
}