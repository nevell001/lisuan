package com.cashier.exception;

/**
 * 数据库异常
 * 用于数据库操作相关的错误
 */
public class DatabaseException extends CashierException {
    
    /** 数据库错误类型 */
    public enum DbErrorType {
        CONNECTION_FAILED,     // 连接失败
        QUERY_FAILED,          // 查询失败
        INSERT_FAILED,         // 插入失败
        UPDATE_FAILED,         // 更新失败
        DELETE_FAILED,         // 删除失败
        TRANSACTION_FAILED,    // 事务失败
        TIMEOUT,               // 操作超时
        DUPLICATE_KEY,         // 重复键
        CONSTRAINT_VIOLATION,  // 约束违反
        LOCK_TIMEOUT           // 锁等待超时
    }
    
    private final DbErrorType dbErrorType;
    private final String sql; // 相关SQL语句（可选）
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param dbErrorType 数据库错误类型
     */
    public DatabaseException(String message, DbErrorType dbErrorType) {
        super(message, "DB_" + dbErrorType.name(), ErrorLevel.ERROR);
        this.dbErrorType = dbErrorType;
        this.sql = null;
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param dbErrorType 数据库错误类型
     * @param cause 原因
     */
    public DatabaseException(String message, DbErrorType dbErrorType, Throwable cause) {
        super(message, "DB_" + dbErrorType.name(), ErrorLevel.ERROR, cause);
        this.dbErrorType = dbErrorType;
        this.sql = null;
    }
    
    /**
     * 构造函数（包含SQL）
     * @param message 错误消息
     * @param dbErrorType 数据库错误类型
     * @param sql 相关SQL语句
     * @param cause 原因
     */
    public DatabaseException(String message, DbErrorType dbErrorType, String sql, Throwable cause) {
        super(message, "DB_" + dbErrorType.name(), ErrorLevel.ERROR, cause);
        this.dbErrorType = dbErrorType;
        this.sql = sql;
    }
    
    /**
     * 快速创建：连接失败
     */
    public static DatabaseException connectionFailed(Throwable cause) {
        return new DatabaseException("数据库连接失败", DbErrorType.CONNECTION_FAILED, cause);
    }
    
    /**
     * 快速创建：查询失败
     * @param sql SQL语句
     * @param cause 原因
     */
    public static DatabaseException queryFailed(String sql, Throwable cause) {
        return new DatabaseException("数据库查询失败", DbErrorType.QUERY_FAILED, sql, cause);
    }
    
    /**
     * 快速创建：插入失败
     */
    public static DatabaseException insertFailed(String tableName, Throwable cause) {
        return new DatabaseException(
            "数据插入失败: " + tableName, 
            DbErrorType.INSERT_FAILED, 
            cause
        );
    }
    
    /**
     * 快速创建：更新失败
     */
    public static DatabaseException updateFailed(String tableName, Throwable cause) {
        return new DatabaseException(
            "数据更新失败: " + tableName, 
            DbErrorType.UPDATE_FAILED, 
            cause
        );
    }
    
    /**
     * 快速创建：事务失败
     */
    public static DatabaseException transactionFailed(String operation, Throwable cause) {
        return new DatabaseException(
            "事务执行失败: " + operation, 
            DbErrorType.TRANSACTION_FAILED, 
            cause
        );
    }
    
    /**
     * 快速创建：重复键
     * @param keyName 重复的键名
     */
    public static DatabaseException duplicateKey(String keyName) {
        return new DatabaseException(
            "数据已存在: " + keyName, 
            DbErrorType.DUPLICATE_KEY
        );
    }
    
    public DbErrorType getDbErrorType() {
        return dbErrorType;
    }
    
    public String getSql() {
        return sql;
    }
}