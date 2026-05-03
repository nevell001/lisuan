package com.cashier.exception;

/**
 * 认证异常
 * 用于登录、权限验证等认证相关的错误
 */
public class AuthenticationException extends CashierException {
    
    /** 认证错误类型 */
    public enum AuthErrorType {
        INVALID_CREDENTIALS,    // 用户名或密码错误
        ACCOUNT_LOCKED,         // 账户被锁定
        ACCOUNT_DISABLED,       // 账户被禁用
        SESSION_EXPIRED,        // 会话过期
        PERMISSION_DENIED,      // 权限不足
        FORCE_PASSWORD_CHANGE   // 强制修改密码
    }
    
    private final AuthErrorType authErrorType;
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param authErrorType 认证错误类型
     */
    public AuthenticationException(String message, AuthErrorType authErrorType) {
        super(message, "AUTH_" + authErrorType.name(), ErrorLevel.WARNING);
        this.authErrorType = authErrorType;
    }
    
    /**
     * 构造函数
     * @param message 错误消息
     * @param authErrorType 认证错误类型
     * @param cause 原因
     */
    public AuthenticationException(String message, AuthErrorType authErrorType, Throwable cause) {
        super(message, "AUTH_" + authErrorType.name(), ErrorLevel.WARNING, cause);
        this.authErrorType = authErrorType;
    }
    
    /**
     * 快速创建：用户名或密码错误
     */
    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("用户名或密码错误", AuthErrorType.INVALID_CREDENTIALS);
    }
    
    /**
     * 快速创建：账户被锁定
     */
    public static AuthenticationException accountLocked() {
        return new AuthenticationException("账户已被锁定，请联系管理员", AuthErrorType.ACCOUNT_LOCKED);
    }
    
    /**
     * 快速创建：账户被禁用
     */
    public static AuthenticationException accountDisabled() {
        return new AuthenticationException("账户已被禁用", AuthErrorType.ACCOUNT_DISABLED);
    }
    
    /**
     * 快速创建：会话过期
     */
    public static AuthenticationException sessionExpired() {
        return new AuthenticationException("登录会话已过期，请重新登录", AuthErrorType.SESSION_EXPIRED);
    }
    
    /**
     * 快速创建：权限不足
     * @param requiredRole 需要的角色
     */
    public static AuthenticationException permissionDenied(String requiredRole) {
        return new AuthenticationException(
            "权限不足，需要 " + requiredRole + " 角色", 
            AuthErrorType.PERMISSION_DENIED
        );
    }
    
    /**
     * 快速创建：强制修改密码
     */
    public static AuthenticationException forcePasswordChange() {
        return new AuthenticationException("需要修改初始密码", AuthErrorType.FORCE_PASSWORD_CHANGE);
    }
    
    public AuthErrorType getAuthErrorType() {
        return authErrorType;
    }
}