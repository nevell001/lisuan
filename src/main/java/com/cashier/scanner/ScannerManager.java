package com.cashier.scanner;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扫描设备管理器
 * 负责管理所有扫描设备的注册、启动、停止和事件分发
 */
public class ScannerManager {
    
    private static final Logger logger = LoggerFactoryUtil.getLogger(ScannerManager.class);
    
    /**
     * 单例实例
     */
    private static volatile ScannerManager instance;
    
    /**
     * 所有注册的扫描设备
     */
    private final Map<String, ScannerDevice> devices;
    
    /**
     * 全局扫描监听器列表
     */
    private final List<ScanListener> globalListeners;
    
    /**
     * 活跃的扫描设备
     */
    private ScannerDevice activeDevice;
    
    /**
     * 智能焦点管理器
     */
    private FocusManager focusManager;
    
    /**
     * 私有构造函数
     */
    private ScannerManager() {
        this.devices = new ConcurrentHashMap<>();
        this.globalListeners = new ArrayList<>();
        this.focusManager = new FocusManager();
    }
    
    /**
     * 获取单例实例
     * @return ScannerManager 实例
     */
    public static ScannerManager getInstance() {
        if (instance == null) {
            synchronized (ScannerManager.class) {
                if (instance == null) {
                    instance = new ScannerManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册扫描设备
     * @param device 扫描设备
     */
    public void registerDevice(ScannerDevice device) {
        if (device == null) {
            logger.warn("尝试注册空设备");
            return;
        }
        
        String deviceId = device.getDeviceId();
        if (devices.containsKey(deviceId)) {
            logger.warn("设备已注册: {}", deviceId);
            return;
        }
        
        devices.put(deviceId, device);
        
        // 注册全局监听器
        device.addScanListener(this::onScanEvent);
        
        logger.info("注册扫描设备: {} ({})", device.getDeviceName(), deviceId);
    }
    
    /**
     * 注销扫描设备
     * @param deviceId 设备ID
     */
    public void unregisterDevice(String deviceId) {
        ScannerDevice device = devices.remove(deviceId);
        if (device != null) {
            if (activeDevice == device) {
                activeDevice = null;
            }
            device.dispose();
            logger.info("注销扫描设备: {}", deviceId);
        }
    }
    
    /**
     * 获取设备
     * @param deviceId 设备ID
     * @return 扫描设备
     */
    public ScannerDevice getDevice(String deviceId) {
        return devices.get(deviceId);
    }
    
    /**
     * 获取所有设备
     * @return 设备列表
     */
    public List<ScannerDevice> getAllDevices() {
        return new ArrayList<>(devices.values());
    }
    
    /**
     * 获取已连接的设备
     * @return 已连接的设备列表
     */
    public List<ScannerDevice> getConnectedDevices() {
        List<ScannerDevice> connected = new ArrayList<>();
        for (ScannerDevice device : devices.values()) {
            if (device.isConnected()) {
                connected.add(device);
            }
        }
        return connected;
    }
    
    /**
     * 设置活跃设备
     * @param deviceId 设备ID
     */
    public void setActiveDevice(String deviceId) {
        ScannerDevice device = getDevice(deviceId);
        if (device != null && device.isConnected()) {
            activeDevice = device;
            logger.info("设置活跃扫描设备: {}", device.getDeviceName());
        } else {
            logger.warn("无法设置活跃设备: {}", deviceId);
        }
    }
    
    /**
     * 获取活跃设备
     * @return 活跃的扫描设备
     */
    public ScannerDevice getActiveDevice() {
        return activeDevice;
    }
    
    /**
     * 启动所有设备
     */
    public void startAllDevices() {
        for (ScannerDevice device : devices.values()) {
            if (device.isConnected()) {
                device.start();
                logger.info("启动扫描设备: {}", device.getDeviceName());
            }
        }
    }
    
    /**
     * 停止所有设备
     */
    public void stopAllDevices() {
        for (ScannerDevice device : devices.values()) {
            if (device.isConnected()) {
                device.stop();
                logger.info("停止扫描设备: {}", device.getDeviceName());
            }
        }
    }
    
    /**
     * 添加全局扫描监听器
     * @param listener 扫描监听器
     */
    public void addGlobalListener(ScanListener listener) {
        if (listener != null && !globalListeners.contains(listener)) {
            globalListeners.add(listener);
        }
    }
    
    /**
     * 移除全局扫描监听器
     * @param listener 扫描监听器
     */
    public void removeGlobalListener(ScanListener listener) {
        globalListeners.remove(listener);
    }
    
    /**
     * 获取焦点管理器
     * @return 焦点管理器
     */
    public FocusManager getFocusManager() {
        return focusManager;
    }
    
    /**
     * 处理扫描事件
     * @param event 扫描事件
     */
    private void onScanEvent(ScanEvent event) {
        logger.info("收到扫描事件: {}", event.getData());
        
        // 通知所有全局监听器
        for (ScanListener listener : globalListeners) {
            try {
                listener.onScan(event);
            } catch (Exception e) {
                logger.error("扫描监听器执行失败", e);
            }
        }
    }
    
    /**
     * 销毁管理器，释放所有资源
     */
    public void dispose() {
        stopAllDevices();
        for (ScannerDevice device : devices.values()) {
            device.dispose();
        }
        devices.clear();
        globalListeners.clear();
        activeDevice = null;
        logger.info("扫描设备管理器已销毁");
    }
}