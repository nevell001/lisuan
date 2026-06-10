package com.cashier.dao;

import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基础 DAO 类
 * 提供数据库连接管理、事务支持和通用查询方法
 */
public abstract class BaseDAO {
    protected final Logger logger = LoggerFactoryUtil.getLogger(getClass());

    /**
     * 获取数据库连接
     * @return 数据库连接
     * @throws SQLException 如果获取连接失败
     */
    protected Connection getConnection() throws SQLException {
        return DatabaseManager.getConnection();
    }

    /**
     * 开始事务
     * @param conn 数据库连接
     * @throws SQLException 如果开始事务失败
     */
    protected void beginTransaction(Connection conn) throws SQLException {
        DatabaseManager.beginTransaction(conn);
    }

    /**
     * 提交事务
     * @param conn 数据库连接
     * @throws SQLException 如果提交事务失败
     */
    protected void commitTransaction(Connection conn) throws SQLException {
        DatabaseManager.commitTransaction(conn);
    }

    /**
     * 回滚事务
     * @param conn 数据库连接
     */
    protected void rollbackTransaction(Connection conn) {
        DatabaseManager.rollbackTransaction(conn);
    }

    /**
     * 在事务中执行操作
     * @param operation 数据库操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws SQLException 如果操作失败
     */
    protected <T> T executeInTransaction(TransactionOperation<T> operation) throws SQLException {
        try (Connection conn = getConnection()) {
            beginTransaction(conn);
            try {
                T result = operation.execute(conn);
                commitTransaction(conn);
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackTransaction(conn);
                throw e;
            }
        }
    }

    /**
     * 事务操作接口
     * @param <T> 返回类型
     */
    @FunctionalInterface
    protected interface TransactionOperation<T> {
        T execute(Connection conn) throws SQLException;
    }

    // ============ 通用查询方法 ============

    /**
     * 查询列表
     * @param sql SQL 语句
     * @param mapper 行映射器
     * @param params SQL 参数
     * @param <T> 结果类型
     * @return 结果列表
     * @throws SQLException 如果查询失败
     */
    protected <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameters(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                int rowNum = 0;
                while (rs.next()) {
                    results.add(mapper.mapRow(rs, ++rowNum));
                }
            }
        }
        return results;
    }

    /**
     * 查询单个对象
     * @param sql SQL 语句
     * @param mapper 行映射器
     * @param params SQL 参数
     * @param <T> 结果类型
     * @return Optional 包装的结果对象
     * @throws SQLException 如果查询失败
     */
    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> list = queryList(sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 查询单个对象，如果不存在返回 null
     * @param sql SQL 语句
     * @param mapper 行映射器
     * @param params SQL 参数
     * @param <T> 结果类型
     * @return 结果对象，不存在返回 null
     * @throws SQLException 如果查询失败
     */
    protected <T> T queryOneOrNull(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        return queryOne(sql, mapper, params).orElse(null);
    }

    /**
     * 查询并返回单个值（如 COUNT、SUM 等）
     * @param sql SQL 语句
     * @param params SQL 参数
     * @param <T> 结果类型
     * @return 查询结果
     * @throws SQLException 如果查询失败
     */
    @SuppressWarnings("unchecked")
    protected <T> T queryScalar(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameters(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return (T) rs.getObject(1);
                }
            }
        }
        return null;
    }

    /**
     * 查询 int 值
     * @param sql SQL 语句
     * @param params SQL 参数
     * @return int 值，如果没有结果返回 0
     * @throws SQLException 如果查询失败
     */
    protected int queryInt(String sql, Object... params) throws SQLException {
        Integer value = queryScalar(sql, params);
        return value != null ? value : 0;
    }

    /**
     * 查询 long 值
     * @param sql SQL 语句
     * @param params SQL 参数
     * @return long 值，如果没有结果返回 0
     * @throws SQLException 如果查询失败
     */
    protected long queryLong(String sql, Object... params) throws SQLException {
        Long value = queryScalar(sql, params);
        return value != null ? value : 0L;
    }

    /**
     * 执行更新操作（INSERT、UPDATE、DELETE）
     * @param sql SQL 语句
     * @param params SQL 参数
     * @return 影响的行数
     * @throws SQLException 如果执行失败
     */
    protected int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameters(pstmt, params);
            return pstmt.executeUpdate();
        }
    }

    /**
     * 执行插入操作并返回自增 ID
     * @param sql SQL 语句
     * @param params SQL 参数
     * @return 自增 ID
     * @throws SQLException 如果执行失败
     */
    protected long executeInsertReturnId(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            setParameters(pstmt, params);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insert failed, no rows affected.");
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }
        throw new SQLException("Insert failed, no ID obtained.");
    }

    /**
     * 批量更新操作
     * @param sql SQL 语句
     * @param paramsList 参数列表
     * @return 每条语句影响的行数数组
     * @throws SQLException 如果执行失败
     */
    protected int[] batchUpdate(String sql, List<Object[]> paramsList) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Object[] params : paramsList) {
                setParameters(pstmt, params);
                pstmt.addBatch();
            }
            return pstmt.executeBatch();
        }
    }

    /**
     * 检查是否存在记录
     * @param sql SQL 语句
     * @param params SQL 参数
     * @return 如果存在返回 true
     * @throws SQLException 如果查询失败
     */
    protected boolean exists(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameters(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 统计记录数
     * @param tableName 表名
     * @param whereClause WHERE 条件（可选，不带 WHERE）
     * @param params WHERE 参数
     * @return 记录数
     * @throws SQLException 如果查询失败
     */
    protected long count(String tableName, String whereClause, Object... params) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(tableName);
        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        return queryLong(sql.toString(), params);
    }

    /**
     * 设置 PreparedStatement 参数
     * @param pstmt PreparedStatement
     * @param params 参数数组
     * @throws SQLException 如果设置参数失败
     */
    private void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
        }
    }
}
