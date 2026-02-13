package com.cashier.scanner;

import java.util.Date;

/**
 * 扫描事件类
 */
public class ScanEvent {
    
    /**
     * 扫描的数据
     */
    private final String data;
    
    /**
     * 扫描的设备ID
     */
    private final String deviceId;
    
    /**
     * 扫描时间
     */
    private final Date timestamp;
    
    /**
     * 扫描数据类型（条形码、二维码等）
     */
    private final ScanDataType dataType;
    
    /**
     * 是否成功
     */
    private final boolean success;
    
    /**
     * 错误信息（如果失败）
     */
    private final String errorMessage;
    
    public ScanEvent(String data, String deviceId) {
        this(data, deviceId, ScanDataType.BARCODE, true, null);
    }
    
    public ScanEvent(String data, String deviceId, ScanDataType dataType, boolean success, String errorMessage) {
        this.data = data;
        this.deviceId = deviceId;
        this.timestamp = new Date();
        this.dataType = dataType;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public String getData() {
        return data;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public ScanDataType getDataType() {
        return dataType;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String toString() {
        return "ScanEvent{" +
                "data='" + data + "'" +
                ", deviceId='" + deviceId + "'" +
                ", timestamp=" + timestamp +
                ", dataType=" + dataType +
                ", success=" + success +
                ", errorMessage='" + errorMessage + "'" +
                '}';
    }
}