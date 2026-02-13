package com.cashier.printer;

/**
 * 打印设备类型枚举
 */
public enum PrinterDeviceType {
    /**
     * USB 热敏打印机
     */
    USB_THERMAL("USB 热敏打印机"),
    
    /**
     * 串口打印机
     */
    SERIAL("串口打印机"),
    
    /**
     * 蓝牙打印机
     */
    BLUETOOTH("蓝牙打印机"),
    
    /**
     * 网络打印机
     */
    NETWORK("网络打印机"),
    
    /**
     * Windows 默认打印机
     */
    WINDOWS_DEFAULT("Windows 默认打印机"),
    
    /**
     * 其他类型
     */
    OTHER("其他设备");
    
    private final String displayName;
    
    PrinterDeviceType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}