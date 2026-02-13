package com.cashier.printer;

/**
 * 打印设备状态枚举
 */
public enum PrinterDeviceStatus {
    /**
     * 未初始化
     */
    UNINITIALIZED("未初始化"),
    
    /**
     * 初始化中
     */
    INITIALIZING("初始化中"),
    
    /**
     * 已初始化，未启动
     */
    READY("已就绪"),
    
    /**
     * 启动中
     */
    STARTING("启动中"),
    
    /**
     * 已连接，运行中
     */
    CONNECTED("已连接"),
    
    /**
     * 打印中
     */
    PRINTING("打印中"),
    
    /**
     * 已断开
     */
    DISCONNECTED("已断开"),
    
    /**
     * 缺纸
     */
    PAPER_OUT("缺纸"),
    
    /**
     * 打印头过热
     */
    HEAD_OVERHEAT("打印头过热"),
    
    /**
     * 打印头错误
     */
    HEAD_ERROR("打印头错误"),
    
    /**
     * 错误状态
     */
    ERROR("错误"),
    
    /**
     * 已销毁
     */
    DISPOSED("已销毁");
    
    private final String displayName;
    
    PrinterDeviceStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 是否是错误状态
     * @return 是否是错误状态
     */
    public boolean isError() {
        return this == ERROR || this == PAPER_OUT || this == HEAD_OVERHEAT || this == HEAD_ERROR;
    }
}