package com.cashier.exception;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * 全局异常处理器
 * 统一处理所有异常，提供用户友好的错误提示
 */
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactoryUtil.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理异常
     * @param e 异常
     */
    public static void handle(Throwable e) {
        handle(e, null);
    }
    
    /**
     * 处理异常（带上下文信息）
     * @param e 异常
     * @param context 上下文信息（操作描述）
     */
    public static void handle(Throwable e, String context) {
        // 记录日志
        logException(e, context);
        
        // 显示用户提示
        showErrorAlert(e);
    }
    
    /**
     * 记录异常日志
     * @param e 异常
     * @param context 上下文
     */
    private static void logException(Throwable e, String context) {
        String contextPrefix = context != null ? "[" + context + "] " : "";
        
        if (e instanceof CashierException) {
            CashierException ce = (CashierException) e;
            switch (ce.getErrorLevel()) {
                case INFO:
                    logger.info("{}{}", contextPrefix, ce.getFullMessage());
                    break;
                case WARNING:
                    logger.warn("{}{}", contextPrefix, ce.getFullMessage());
                    break;
                case ERROR:
                    logger.error("{}{}", contextPrefix, ce.getFullMessage(), e);
                    break;
                case CRITICAL:
                    logger.error("[CRITICAL] {}{}", contextPrefix, ce.getFullMessage(), e);
                    break;
            }
        } else if (e instanceof RuntimeException) {
            logger.error("{}运行时异常: {}", contextPrefix, e.getMessage(), e);
        } else {
            logger.error("{}异常: {}", contextPrefix, e.getMessage(), e);
        }
    }
    
    /**
     * 显示错误提示框
     * @param e 异常
     */
    private static void showErrorAlert(Throwable e) {
        // 确保在JavaFX线程执行
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showErrorAlertInternal(e));
            return;
        }
        showErrorAlertInternal(e);
    }
    
    private static void showErrorAlertInternal(Throwable e) {
        Alert alert;
        String title;
        String message;
        
        if (e instanceof AuthenticationException) {
            AuthenticationException ae = (AuthenticationException) e;
            alert = new Alert(AlertType.WARNING);
            title = "认证错误";
            message = ae.getMessage();
        } else if (e instanceof BusinessException) {
            BusinessException be = (BusinessException) e;
            alert = new Alert(AlertType.WARNING);
            title = "业务错误";
            message = be.getMessage();
        } else if (e instanceof DatabaseException) {
            DatabaseException de = (DatabaseException) e;
            alert = new Alert(AlertType.ERROR);
            title = "数据库错误";
            message = de.getMessage() + "\n请联系技术人员处理。";
        } else if (e instanceof CashierException) {
            CashierException ce = (CashierException) e;
            alert = new Alert(ce.getErrorLevel() == CashierException.ErrorLevel.CRITICAL 
                ? AlertType.ERROR : AlertType.WARNING);
            title = "系统错误";
            message = ce.getMessage();
        } else {
            alert = new Alert(AlertType.ERROR);
            title = "未知错误";
            message = "发生未知错误: " + e.getMessage();
        }
        
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * 获取用户友好的错误消息
     * @param e 异常
     * @return 用户友好的消息
     */
    public static String getUserFriendlyMessage(Throwable e) {
        if (e instanceof CashierException) {
            return e.getMessage();
        }
        return "系统发生错误，请稍后重试或联系技术人员。";
    }
    
    /**
     * 判断是否需要重试
     * @param e 异常
     * @return 是否可重试
     */
    public static boolean isRetryable(Throwable e) {
        if (e instanceof DatabaseException) {
            DatabaseException de = (DatabaseException) e;
            return de.getDbErrorType() == DatabaseException.DbErrorType.TIMEOUT 
                || de.getDbErrorType() == DatabaseException.DbErrorType.LOCK_TIMEOUT
                || de.getDbErrorType() == DatabaseException.DbErrorType.CONNECTION_FAILED;
        }
        return false;
    }
    
    /**
     * 判断是否需要管理员介入
     * @param e 异常
     * @return 是否需要管理员
     */
    public static boolean requiresAdminIntervention(Throwable e) {
        if (e instanceof DatabaseException) {
            return true;
        }
        if (e instanceof CashierException) {
            CashierException ce = (CashierException) e;
            return ce.getErrorLevel() == CashierException.ErrorLevel.CRITICAL;
        }
        return false;
    }
}