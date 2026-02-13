package com.cashier.scanner;

/**
 * 扫描设备状态枚举
 */
public enum ScannerDeviceStatus {
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
     * 扫描中
     */
    SCANNING("扫描中"),
    
    /**
     * 已断开
     */
    DISCONNECTED("已断开"),
    
    /**
     * 错误状态
     */
    ERROR("错误"),
    
    /**
     * 已销毁
     */
    DISPOSED("已销毁");
    
    private final String displayName;
    
    ScannerDeviceStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}