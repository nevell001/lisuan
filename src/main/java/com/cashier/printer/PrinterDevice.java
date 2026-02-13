package com.cashier.printer;

import java.util.Map;

/**
 * 打印设备接口
 * 所有打印设备类型都需要实现此接口
 */
public interface PrinterDevice {
    
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
    PrinterDeviceType getDeviceType();
    
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
    PrinterDeviceStatus getStatus();
    
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
     * 打印文本
     * @param text 要打印的文本
     * @return 是否打印成功
     */
    boolean printText(String text);
    
    /**
     * 打印打印任务
     * @param task 打印任务
     * @return 是否打印成功
     */
    boolean print(PrintTask task);
    
    /**
     * 打开钱箱
     * @return 是否成功
     */
    boolean openCashDrawer();
    
    /**
     * 切纸
     * @return 是否成功
     */
    boolean cutPaper();
    
    /**
     * 检查打印机状态
     * @return 打印机状态信息
     */
    PrinterStatus checkStatus();
    
    /**
     * 销毁设备，释放资源
     */
    void dispose();
}