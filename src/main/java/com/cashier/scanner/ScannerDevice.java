package com.cashier.scanner;

import java.util.Map;

/**
 * 扫描设备接口
 * 所有扫描设备类型都需要实现此接口
 */
public interface ScannerDevice {
    
    /**
     * 获取设备ID
     * @return 设备唯一标识
     */
    String getDeviceId();
    
    /**
     * 获取设备名称
     * @return 设备名称
     */
    String getDeviceName();
    
    /**
     * 获取设备类型
     * @return 设备类型
     */
    ScannerDeviceType getDeviceType();
    
    /**
     * 初始化设备
     * @return 是否初始化成功
     */
    boolean initialize();
    
    /**
     * 启动设备
     * @return 是否启动成功
     */
    boolean start();
    
    /**
     * 停止设备
     * @return 是否停止成功
     */
    boolean stop();
    
    /**
     * 检查设备是否已连接
     * @return 是否已连接
     */
    boolean isConnected();
    
    /**
     * 获取设备状态
     * @return 设备状态
     */
    ScannerDeviceStatus getStatus();
    
    /**
     * 获取设备配置
     * @return 配置参数
     */
    Map<String, String> getConfiguration();
    
    /**
     * 设置设备配置
     * @param config 配置参数
     */
    void setConfiguration(Map<String, String> config);
    
    /**
     * 注册扫描监听器
     * @param listener 扫描监听器
     */
    void addScanListener(ScanListener listener);
    
    /**
     * 移除扫描监听器
     * @param listener 扫描监听器
     */
    void removeScanListener(ScanListener listener);
    
    /**
     * 销毁设备，释放资源
     */
    void dispose();
}