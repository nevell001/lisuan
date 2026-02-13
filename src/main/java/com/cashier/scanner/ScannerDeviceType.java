package com.cashier.scanner;

/**
 * 扫描设备类型枚举
 */
public enum ScannerDeviceType {
    /**
     * USB HID 扫描枪（模拟键盘输入）
     */
    USB_HID("USB HID 扫描枪"),
    
    /**
     * 串口扫描枪
     */
    SERIAL("串口扫描枪"),
    
    /**
     * 蓝牙扫描枪
     */
    BLUETOOTH("蓝牙扫描枪"),
    
    /**
     * 网络扫描枪
     */
    NETWORK("网络扫描枪"),
    
    /**
     * 二维码摄像头扫描
     */
    CAMERA("二维码扫描"),
    
    /**
     * 其他类型
     */
    OTHER("其他设备");
    
    private final String displayName;
    
    ScannerDeviceType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}