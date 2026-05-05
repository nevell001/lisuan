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
    
    // 心跳
    PING,
    PONG
}