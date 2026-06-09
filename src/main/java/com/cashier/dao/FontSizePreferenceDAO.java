package com.cashier.dao;

import com.cashier.util.DatabaseManager;

import java.sql.*;

/**
 * 字号偏好数据访问对象
 * 负责字号偏好相关的数据库操作
 */
public class FontSizePreferenceDAO {

    /**
     * 获取字号偏好
     */
    public static String getFontSizePreference(String username) throws SQLException {
        String sql = "SELECT font_size FROM font_size_preferences WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("font_size");
            }
        }
        return "medium"; // 默认中等字号
    }

    /**
     * 设置字号偏好
     */
    public static boolean setFontSizePreference(String username, String fontSize) throws SQLException {
        String sql = "INSERT INTO font_size_preferences (username, font_size, updated_at) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE font_size = ?, updated_at = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            long now = System.currentTimeMillis();
            pstmt.setString(1, username);
            pstmt.setString(2, fontSize);
            pstmt.setLong(3, now);
            pstmt.setString(4, fontSize);
            pstmt.setLong(5, now);
            return pstmt.executeUpdate() > 0;
        }
    }
}
