package com.cashier.dao;

import com.cashier.model.User;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 用户数据访问对象
 * 负责用户相关的数据库操作
 */
public class UserDAO {

    /**
     * 根据ID查找用户
     */
    public static User findById(int id) throws SQLException {
        String sql = "SELECT id, username, password, name, role, create_time, last_login_time, active FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToUser(rs);
            }
        }
        return null;
    }

    /**
     * 根据用户名查找用户
     */
    public static User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password, name, role, create_time, last_login_time, active FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToUser(rs);
            }
        }
        return null;
    }

    /**
     * 验证用户登录（已废弃，请在应用层使用 findByUsername + PasswordUtil.verifyPassword）
     * @deprecated 请使用 findByUsername(String username) + PasswordUtil.verifyPassword() 进行验证
     */
    @Deprecated
    public static User authenticate(String username, String password) throws SQLException {
        // 此方法已废弃，请使用 findByUsername + PasswordUtil.verifyPassword 进行验证
        return findByUsername(username);
    }

    /**
     * 查询所有用户
     */
    public static List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password, name, role, create_time, last_login_time, active FROM users ORDER BY username";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    /**
     * 插入新用户
     * 如果用户ID大于0，则使用指定的ID；否则由数据库自动生成ID
     */
    public static boolean insert(User user) throws SQLException {
        String sql;
        boolean useProvidedId = user.id > 0;

        if (useProvidedId) {
            // 使用用户提供的ID
            sql = "INSERT INTO users (id, username, password, name, role, create_time, last_login_time, active) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            // 由数据库自动生成ID
            sql = "INSERT INTO users (username, password, name, role, create_time, last_login_time, active) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (useProvidedId) {
                pstmt.setInt(paramIndex++, user.id);
            }

            pstmt.setString(paramIndex++, user.username);
            pstmt.setString(paramIndex++, user.password);
            pstmt.setString(paramIndex++, user.name);
            pstmt.setString(paramIndex++, user.role);
            pstmt.setTimestamp(paramIndex++, new Timestamp(user.createTime.getTime()));
            pstmt.setTimestamp(paramIndex++, new Timestamp(user.lastLoginTime.getTime()));
            pstmt.setBoolean(paramIndex++, user.active);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0 && !useProvidedId) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新用户
     */
    public static boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET password = ?, name = ?, role = ?, active = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.password);
            pstmt.setString(2, user.name);
            pstmt.setString(3, user.role);
            pstmt.setBoolean(4, user.active);
            pstmt.setInt(5, user.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新最后登录时间
     */
    public static boolean updateLastLoginTime(int id) throws SQLException {
        String sql = "UPDATE users SET last_login_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(2, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据用户名更新最后登录时间（兼容旧代码）
     */
    public static boolean updateLastLoginTimeByUsername(String username) throws SQLException {
        String sql = "UPDATE users SET last_login_time = ? WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, username);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除用户
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据用户名删除用户（兼容旧代码）
     */
    public static boolean deleteByUsername(String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 检查用户名是否存在
     */
    public static boolean exists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * 批量插入用户
     */
    public static void batchInsert(List<User> users) throws SQLException {
        String sql = "INSERT INTO users (username, password, name, role, create_time, last_login_time, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (User user : users) {
                pstmt.setString(1, user.username);
                pstmt.setString(2, user.password);
                pstmt.setString(3, user.name);
                pstmt.setString(4, user.role);
                pstmt.setTimestamp(5, new Timestamp(user.createTime.getTime()));
                pstmt.setTimestamp(6, new Timestamp(user.lastLoginTime.getTime()));
                pstmt.setBoolean(7, user.active);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 User 对象
     */
    private static User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.id = rs.getInt("id");
        user.username = rs.getString("username");
        user.password = rs.getString("password");
        user.name = rs.getString("name");
        user.role = rs.getString("role");

        // 从 BIGINT 读取时间戳并转换为 Date
        long createTime = rs.getLong("create_time");
        if (!rs.wasNull()) {
            user.createTime = new java.util.Date(createTime);
        }

        long lastLoginTime = rs.getLong("last_login_time");
        if (!rs.wasNull()) {
            user.lastLoginTime = new java.util.Date(lastLoginTime);
        }

        user.active = rs.getBoolean("active");
        return user;
    }
}
