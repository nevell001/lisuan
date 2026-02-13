package com.cashier.printer;

import java.util.Date;

/**
 * 打印机状态信息类
 */
public class PrinterStatus {
    
    /**
     * 设备ID
     */
    private final String deviceId;
    
    /**
     * 设备状态
     */
    private final PrinterDeviceStatus status;
    
    /**
     * 检查时间
     */
    private final Date timestamp;
    
    /**
     * 错误信息
     */
    private final String errorMessage;
    
    /**
     * 纸张剩余百分比
     */
    private final int paperRemaining;
    
    /**
     * 墨水/碳带剩余百分比
     */
    private final int inkRemaining;
    
    /**
     * 打印头温度
     */
    private final Double headTemperature;
    
    public PrinterStatus(String deviceId, PrinterDeviceStatus status) {
        this(deviceId, status, null, 0, 0, null);
    }
    
    public PrinterStatus(String deviceId, PrinterDeviceStatus status, String errorMessage, 
                       int paperRemaining, int inkRemaining, Double headTemperature) {
        this.deviceId = deviceId;
        this.status = status;
        this.timestamp = new Date();
        this.errorMessage = errorMessage;
        this.paperRemaining = paperRemaining;
        this.inkRemaining = inkRemaining;
        this.headTemperature = headTemperature;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public PrinterDeviceStatus getStatus() {
        return status;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public int getPaperRemaining() {
        return paperRemaining;
    }
    
    public int getInkRemaining() {
        return inkRemaining;
    }
    
    public Double getHeadTemperature() {
        return headTemperature;
    }
    
    /**
     * 是否需要维护
     * @return 是否需要维护
     */
    public boolean needsMaintenance() {
        return paperRemaining < 10 || inkRemaining < 10 || 
               (headTemperature != null && headTemperature > 60);
    }
    
    @Override
    public String toString() {
        return "PrinterStatus{" +
                "deviceId='" + deviceId + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                ", errorMessage='" + errorMessage + '\'' +
                ", paperRemaining=" + paperRemaining +
                ", inkRemaining=" + inkRemaining +
                ", headTemperature=" + headTemperature +
                '}';
    }
}