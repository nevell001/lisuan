package com.cashier.dao;

import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 基础 DAO 类
 * 提供数据库连接管理和事务支持
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
}
