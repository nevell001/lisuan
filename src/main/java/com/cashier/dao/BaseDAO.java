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
        if (conn != null) {
            conn.setAutoCommit(false);
            logger.debug("事务已开始");
        }
    }

    /**
     * 提交事务
     * @param conn 数据库连接
     * @throws SQLException 如果提交事务失败
     */
    protected void commitTransaction(Connection conn) throws SQLException {
        if (conn != null && !conn.getAutoCommit()) {
            conn.commit();
            conn.setAutoCommit(true);
            logger.debug("事务已提交");
        }
    }

    /**
     * 回滚事务
     * @param conn 数据库连接
     */
    protected void rollbackTransaction(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    logger.debug("事务已回滚");
                }
            } catch (SQLException e) {
                logger.error("回滚事务失败", e);
            }
        }
    }

    /**
     * 在事务中执行操作
     * @param operation 数据库操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws SQLException 如果操作失败
     */
    protected <T> T executeInTransaction(TransactionOperation<T> operation) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            beginTransaction(conn);
            T result = operation.execute(conn);
            commitTransaction(conn);
            return result;
        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw e;
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
