package com.cashier.dao;

import com.cashier.model.PageResult;
import com.cashier.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class UserDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "id, username, password, name, role, create_time, last_login_time, active, force_password_change ";

    private static final RowMapper<User> USER_MAPPER = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.id = rs.getInt("id");
            user.username = rs.getString("username");
            user.password = rs.getString("password");
            user.name = rs.getString("name");
            user.role = rs.getString("role");
            user.createTime = readDateColumn(rs, "create_time");
            user.lastLoginTime = readDateColumn(rs, "last_login_time");
            user.active = rs.getBoolean("active");
            user.forcePasswordChange = rs.getBoolean("force_password_change");
            return user;
        }
    };

    public User findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM users WHERE id = ?", USER_MAPPER, id);
    }

    public User findByUsername(String username) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM users WHERE username = ?", USER_MAPPER, username);
    }

    public List<User> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM users ORDER BY username", USER_MAPPER);
    }

    public PageResult<User> findAll(int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }
        long total = count();
        int offset = (pageNum - 1) * pageSize;
        List<User> users = queryList("SELECT " + SELECT_COLUMNS +
            " FROM users ORDER BY username LIMIT ? OFFSET ?", USER_MAPPER, pageSize, offset);
        return new PageResult<>(users, pageNum, pageSize, total);
    }

    public long count() throws SQLException {
        return queryLong("SELECT COUNT(*) FROM users");
    }

    public boolean insert(User user) throws SQLException {
        if (user.id > 0) {
            return executeUpdate(
                "INSERT INTO users (id, username, password, name, role, create_time, last_login_time, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                user.id, user.username, user.password, user.name, user.role,
                toEpochMillis(user.createTime), toEpochMillis(user.lastLoginTime),
                user.active) > 0;
        }
        long id = executeInsertReturnId(
            "INSERT INTO users (username, password, name, role, create_time, last_login_time, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            user.username, user.password, user.name, user.role,
            toEpochMillis(user.createTime), toEpochMillis(user.lastLoginTime),
            user.active);
        user.id = (int) id;
        return id > 0;
    }

    public boolean update(User user) throws SQLException {
        return executeUpdate(
            "UPDATE users SET password = ?, name = ?, role = ?, active = ?, force_password_change = ? WHERE id = ?",
            user.password, user.name, user.role, user.active, user.forcePasswordChange, user.id) > 0;
    }

    public boolean updateLastLoginTime(int id) throws SQLException {
        return executeUpdate(
            "UPDATE users SET last_login_time = ? WHERE id = ?",
            System.currentTimeMillis(), id) > 0;
    }

    public boolean updateLastLoginTimeByUsername(String username) throws SQLException {
        return executeUpdate(
            "UPDATE users SET last_login_time = ? WHERE username = ?",
            System.currentTimeMillis(), username) > 0;
    }

    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM users WHERE id = ?", id) > 0;
    }

    public boolean deleteByUsername(String username) throws SQLException {
        return executeUpdate("DELETE FROM users WHERE username = ?", username) > 0;
    }

    public boolean exists(String username) throws SQLException {
        return queryInt("SELECT COUNT(*) FROM users WHERE username = ?", username) > 0;
    }

    public void batchInsert(List<User> users) throws SQLException {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Object[]> params = new ArrayList<>(users.size());
        for (User user : users) {
            params.add(new Object[]{
                user.username, user.password, user.name, user.role,
                toEpochMillis(user.createTime), toEpochMillis(user.lastLoginTime),
                user.active});
        }
        // BaseDAO.batchUpdate 不返回自增ID；批量插入后按用户名回填ID以保持兼容
        batchUpdate(
            "INSERT INTO users (username, password, name, role, create_time, last_login_time, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)", params);
        for (User user : users) {
            User saved = findByUsername(user.username);
            if (saved != null) {
                user.id = saved.id;
            }
        }
    }

    public boolean updatePassword(int id, String newPassword) throws SQLException {
        return executeUpdate(
            "UPDATE users SET password = ?, force_password_change = 0 WHERE id = ?",
            newPassword, id) > 0;
    }

    public boolean updatePasswordByUsername(String username, String newPassword) throws SQLException {
        return executeUpdate(
            "UPDATE users SET password = ?, force_password_change = 0 WHERE username = ?",
            newPassword, username) > 0;
    }

    /**
     * 把时间值转成 users 表要求的 epoch 毫秒。
     *
     * <p>{@code create_time} / {@code last_login_time} 在 MySQL 侧是 BIGINT，
     * 语义是 epoch 毫秒（与 {@code docker/mysql-init/00-init-complete.sql} 的
     * {@code UNIX_TIMESTAMP() * 1000} 以及 {@code DatabaseManager} 的 {@code setLong} 一致）。
     * 若直接传 {@link Timestamp}，MySQL 会把时间值按 {@code YYYYMMDDHHMMSS} 数值化写进 BIGINT，
     * 读回时又被当作毫秒，界面会显示成 2612 年。</p>
     */
    private static Long toEpochMillis(java.util.Date date) {
        return date == null ? null : date.getTime();
    }

    /** 读取日期列，兼容 BIGINT (epoch millis) 和 TIMESTAMP 两种存储方式 */
    private static java.util.Date readDateColumn(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return new java.util.Date(ts.getTime());
        }
        if (value instanceof java.util.Date dt) {
            return new java.util.Date(dt.getTime());
        }
        if (value instanceof Number num) {
            return new java.util.Date(num.longValue());
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return new java.util.Date(Long.parseLong(str));
            } catch (NumberFormatException e) {
                return java.sql.Timestamp.valueOf(str);
            }
        }
        return null;
    }
}
