package com.cashier.dao;

import com.cashier.util.DatabaseManager;

import java.sql.*;

/**
 * 语言偏好数据访问对象
 * 负责语言偏好相关的数据库操作
 */
public class LanguagePreferenceDAO {

    /**
     * 获取语言偏好
     */
    public static String getLanguagePreference(String username) throws SQLException {
        String sql = "SELECT language_tag FROM language_preferences WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("language_tag");
            }
        }
        return "zh-CN"; // 默认简体中文
    }

    /**
     * 设置语言偏好
     */
    public static boolean setLanguagePreference(String username, String languageTag) throws SQLException {
        String sql = "INSERT INTO language_preferences (username, language_tag, updated_at) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE language_tag = ?, updated_at = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            long now = System.currentTimeMillis();
            pstmt.setString(1, username);
            pstmt.setString(2, languageTag);
            pstmt.setLong(3, now);
            pstmt.setString(4, languageTag);
            pstmt.setLong(5, now);
            return pstmt.executeUpdate() > 0;
        }
    }
}
