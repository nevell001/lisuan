package com.cashier.notification;

/**
 * 通知类型枚举
 */
public enum NotificationType {
    // 库存相关
    LOW_STOCK("库存不足", "商品库存低于最低库存"),
    OVERSTOCK("库存积压", "商品库存积压"),
    SLOW_MOVING("滞销商品", "商品滞销预警"),
    
    // 促销相关
    PROMOTION_START("促销开始", "促销活动开始"),
    PROMOTION_END("促销结束", "促销活动结束"),
    PROMOTION("促销活动", "促销活动相关通知"),
    
    // 系统相关
    SYSTEM("系统通知", "系统重要通知"),
    WARNING("警告", "系统警告"),
    ERROR("错误", "系统错误"),
    INFO("信息", "系统信息"),
    CRITICAL("严重警告", "系统严重警告"),
    SUCCESS("成功", "操作成功通知"),
    
    // 业务相关
    TRANSACTION("交易通知", "交易相关通知"),
    RECHARGE("充值通知", "会员充值通知"),
    RETURN("退货通知", "退货相关通知"),
    PURCHASE("采购通知", "采购相关通知"),
    
    // 成就相关
    MILESTONE("里程碑", "业务里程碑达成通知");
    
    private final String displayName;
    private final String description;
    
    NotificationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}