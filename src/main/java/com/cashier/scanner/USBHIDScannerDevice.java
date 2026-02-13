package com.cashier.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * USB HID 扫描设备实现
 * 模拟键盘输入的扫描枪
 */
public class USBHIDScannerDevice implements ScannerDevice {
    
    private static final Logger logger = LoggerFactory.getLogger(USBHIDScannerDevice.class);
    
    private final String deviceId;
    private final String deviceName;
    private ScannerDeviceStatus status;
    private Map<String, String> configuration;
    private final List<ScanListener> listeners;
    private boolean connected;
    
    public USBHIDScannerDevice(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.status = ScannerDeviceStatus.UNINITIALIZED;
        this.configuration = new HashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.connected = false;
        
        // 默认配置
        configuration.put("baudRate", "9600");
        configuration.put("dataBits", "8");
        configuration.put("stopBits", "1");
        configuration.put("parity", "none");
        configuration.put("autoEnter", "true");
    }
    
    @Override
    public String getDeviceId() {
        return deviceId;
    }
    
    @Override
    public String getDeviceName() {
        return deviceName;
    }
    
    @Override
    public ScannerDeviceType getDeviceType() {
        return ScannerDeviceType.USB_HID;
    }
    
    @Override
    public boolean initialize() {
        logger.info("初始化 USB HID 扫描设备: {}", deviceName);
        status = ScannerDeviceStatus.INITIALIZING;
        
        try {
            // USB HID 设备模拟键盘输入，无需特殊初始化
            status = ScannerDeviceStatus.READY;
            connected = true;
            logger.info("USB HID 扫描设备初始化成功: {}", deviceName);
            return true;
        } catch (Exception e) {
            logger.error("USB HID 扫描设备初始化失败: {}", deviceName, e);
            status = ScannerDeviceStatus.ERROR;
            return false;
        }
    }
    
    @Override
    public boolean start() {
        logger.info("启动 USB HID 扫描设备: {}", deviceName);
        status = ScannerDeviceStatus.STARTING;
        
        try {
            status = ScannerDeviceStatus.CONNECTED;
            logger.info("USB HID 扫描设备已启动: {}", deviceName);
            return true;
        } catch (Exception e) {
            logger.error("USB HID 扫描设备启动失败: {}", deviceName, e);
            status = ScannerDeviceStatus.ERROR;
            return false;
        }
    }
    
    @Override
    public boolean stop() {
        logger.info("停止 USB HID 扫描设备: {}", deviceName);
        status = ScannerDeviceStatus.DISCONNECTED;
        return true;
    }
    
    @Override
    public boolean isConnected() {
        return connected && status == ScannerDeviceStatus.CONNECTED;
    }
    
    @Override
    public ScannerDeviceStatus getStatus() {
        return status;
    }
    
    @Override
    public Map<String, String> getConfiguration() {
        return new HashMap<>(configuration);
    }
    
    @Override
    public void setConfiguration(Map<String, String> config) {
        if (config != null) {
            configuration.putAll(config);
        }
    }
    
    @Override
    public void addScanListener(ScanListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeScanListener(ScanListener listener) {
        listeners.remove(listener);
    }
    
    @Override
    public void dispose() {
        logger.info("销毁 USB HID 扫描设备: {}", deviceName);
        stop();
        listeners.clear();
        status = ScannerDeviceStatus.DISPOSED;
        connected = false;
    }
    
    /**
     * 模拟收到扫描数据
     * 此方法供外部调用，模拟扫描枪输入
     * @param data 扫描数据
     */
    public void onScanDataReceived(String data) {
        if (!isConnected()) {
            logger.warn("设备未连接，忽略扫描数据: {}", data);
            return;
        }
        
        logger.debug("收到扫描数据: {}", data);
        status = ScannerDeviceStatus.SCANNING;
        
        ScanEvent event = new ScanEvent(data, deviceId, ScanDataType.BARCODE, true, null);
        notifyListeners(event);
        
        status = ScannerDeviceStatus.CONNECTED;
    }
    
    /**
     * 通知所有监听器
     * @param event 扫描事件
     */
    private void notifyListeners(ScanEvent event) {
        for (ScanListener listener : listeners) {
            try {
                listener.onScan(event);
            } catch (Exception e) {
                logger.error("扫描监听器执行失败", e);
            }
        }
    }
}