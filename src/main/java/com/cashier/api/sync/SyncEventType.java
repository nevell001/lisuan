package com.cashier.api.sync;

/**
 * 同步事件类型
 */
public enum SyncEventType {
    // 终端状态
    TERMINAL_CONNECTED,       // 终端上线
    TERMINAL_DISCONNECTED,    // 终端下线
    ONLINE_TERMINALS,         // 在线终端列表
    
    // 业务事件
    TRANSACTION_CREATED,      // 新交易创建
    TRANSACTION_REFUNDED,     // 交易退款
    TRANSACTION_COMPLETED,    // 交易完成
    
    PRODUCT_CREATED,          // 商品新增
    PRODUCT_UPDATED,          // 商品更新
    PRODUCT_DELETED,          // 商品删除
    
    MEMBER_CREATED,           // 会员新增
    MEMBER_UPDATED,           // 会员更新
    MEMBER_RECHARGED,         // 会员充值
    
    INVENTORY_CHANGED,        // 库存变化
    INVENTORY_ALERT,          // 库存预警
    
    USER_LOGIN,               // 用户登录
    USER_LOGOUT,              // 用户登出
    
    SETTINGS_CHANGED,         // 设置变更
    
    // 系统事件
    SYSTEM_ALERT,             // 系统告警
    SYNC_REQUEST,             // 同步请求
    SYNC_RESPONSE,            // 同步响应
    
    // 发票事件
    INVOICE_CREATED,          // 发票创建
    INVOICE_PRINTED,          // 发票打印
    INVOICE_VOIDED,           // 发票作废
    
    // 打印机事件
    PRINTER_ADDED,            // 打印机添加
    PRINTER_CONNECTED,        // 打印机连接
    PRINTER_DISCONNECTED,     // 打印机断开
    PRINTER_REMOVED,          // 打印机删除
    PRINT_TASK_COMPLETED,     // 打印任务完成
    PRINT_TASK_FAILED,        // 打印任务失败
    
    // 支付事件
    PAYMENT_ORDER_CREATED,    // 支付订单创建
    PAYMENT_SUCCESS,          // 支付成功
    PAYMENT_FAILED,           // 支付失败
    PAYMENT_REFUND,           // 支付退款
    PAYMENT_ORDER_CLOSED,     // 订单关闭
    
    // 备份事件
    BACKUP_SUCCESS,           // 备份成功
    BACKUP_FAILED,            // 备份失败
    BACKUP_RESTORED,          // 备份恢复
    BACKUP_CLEANED,           // 备份清理
    
    // 心跳
    PING,
    PONG;
    
    /**
     * 从字符串获取事件类型
     */
    public static SyncEventType fromName(String name) {
        for (SyncEventType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }
}