package com.cashier.exception;

/**
 * 收银系统业务异常基类
 * 所有业务相关异常都应继承此类
 */
public class CashierException extends RuntimeException {
    
    /** 错误代码 */
    private final String errorCode;
    
    /** 错误级别 */
    private final ErrorLevel errorLevel;
    
    /**
     * 错误级别枚举
     */
    public enum ErrorLevel {
        INFO,       // 信息提示
        WARNING,    // 警告
        ERROR,      // 错误
        CRITICAL    // 严重错误
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     */
    public CashierException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.errorLevel = ErrorLevel.ERROR;
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param errorCode 错误代码
     */
    public CashierException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.errorLevel = ErrorLevel.ERROR;
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param errorCode 错误代码
     * @param errorLevel 错误级别
     */
    public CashierException(String message, String errorCode, ErrorLevel errorLevel) {
        super(message);
        this.errorCode = errorCode;
        this.errorLevel = errorLevel;
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param cause 原因
     */
    public CashierException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN";
        this.errorLevel = ErrorLevel.ERROR;
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param errorCode 错误代码
     * @param cause 原因
     */
    public CashierException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorLevel = ErrorLevel.ERROR;
    }
    
    /**
     * 构造函数（完整参数）
     * @param message 错误消息
     * @param errorCode 错误代码
     * @param errorLevel 错误级别
     * @param cause 原因
     */
    public CashierException(String message, String errorCode, ErrorLevel errorLevel, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorLevel = errorLevel;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public ErrorLevel getErrorLevel() {
        return errorLevel;
    }
    
    /**
     * 获取完整的错误信息（包含错误代码和级别）
     * @return 完整错误信息
     */
    public String getFullMessage() {
        return String.format("[%s][%s] %s", errorLevel, errorCode, getMessage());
    }
}