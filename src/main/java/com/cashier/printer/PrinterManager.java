package com.cashier.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 打印设备管理器
 * 负责管理所有打印设备的注册、启动、停止和打印任务调度
 */
public class PrinterManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PrinterManager.class);
    
    /**
     * 单例实例
     */
    private static volatile PrinterManager instance;
    
    /**
     * 所有注册的打印设备
     */
    private final Map<String, PrinterDevice> devices;
    
    /**
     * 默认打印机
     */
    private PrinterDevice defaultPrinter;
    
    /**
     * 任务队列
     */
    private final Queue<PrintTask> taskQueue;
    
    /**
     * 打印历史记录
     */
    private final List<PrintTask> printHistory;
    
    /**
     * 私有构造函数
     */
    private PrinterManager() {
        this.devices = new ConcurrentHashMap<>();
        this.taskQueue = new LinkedList<>();
        this.printHistory = new ArrayList<>();
    }
    
    /**
     * 获取单例实例
     * @return PrinterManager 实例
     */
    public static PrinterManager getInstance() {
        if (instance == null) {
            synchronized (PrinterManager.class) {
                if (instance == null) {
                    instance = new PrinterManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册打印设备
     * @param device 打印设备
     */
    public void registerDevice(PrinterDevice device) {
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
        logger.info("注册打印设备: {} ({})", device.getDeviceName(), deviceId);
    }
    
    /**
     * 注销打印设备
     * @param deviceId 设备ID
     */
    public void unregisterDevice(String deviceId) {
        PrinterDevice device = devices.remove(deviceId);
        if (device != null) {
            if (defaultPrinter == device) {
                defaultPrinter = null;
            }
            device.dispose();
            logger.info("注销打印设备: {}", deviceId);
        }
    }
    
    /**
     * 获取设备
     * @param deviceId 设备ID
     * @return 打印设备
     */
    public PrinterDevice getDevice(String deviceId) {
        return devices.get(deviceId);
    }
    
    /**
     * 获取所有设备
     * @return 设备列表
     */
    public List<PrinterDevice> getAllDevices() {
        return new ArrayList<>(devices.values());
    }
    
    /**
     * 获取已连接的设备
     * @return 已连接的设备列表
     */
    public List<PrinterDevice> getConnectedDevices() {
        List<PrinterDevice> connected = new ArrayList<>();
        for (PrinterDevice device : devices.values()) {
            if (device.isConnected()) {
                connected.add(device);
            }
        }
        return connected;
    }
    
    /**
     * 设置默认打印机
     * @param deviceId 设备ID
     */
    public void setDefaultPrinter(String deviceId) {
        PrinterDevice device = getDevice(deviceId);
        if (device != null && device.isConnected()) {
            defaultPrinter = device;
            logger.info("设置默认打印机: {}", device.getDeviceName());
        } else {
            logger.warn("无法设置默认打印机: {}", deviceId);
        }
    }
    
    /**
     * 获取默认打印机
     * @return 默认的打印设备
     */
    public PrinterDevice getDefaultPrinter() {
        return defaultPrinter;
    }
    
    /**
     * 启动所有设备
     */
    public void startAllDevices() {
        for (PrinterDevice device : devices.values()) {
            if (device.isConnected()) {
                device.start();
                logger.info("启动打印设备: {}", device.getDeviceName());
            }
        }
    }
    
    /**
     * 停止所有设备
     */
    public void stopAllDevices() {
        for (PrinterDevice device : devices.values()) {
            if (device.isConnected()) {
                device.stop();
                logger.info("停止打印设备: {}", device.getDeviceName());
            }
        }
    }
    
    /**
     * 打印文本
     * @param text 要打印的文本
     * @return 是否打印成功
     */
    public boolean printText(String text) {
        if (defaultPrinter != null && defaultPrinter.isConnected()) {
            return defaultPrinter.printText(text);
        }
        logger.warn("没有可用的默认打印机");
        return false;
    }
    
    /**
     * 打印任务
     * @param task 打印任务
     * @return 是否打印成功
     */
    public boolean print(PrintTask task) {
        if (task == null) {
            logger.warn("打印任务为空");
            return false;
        }
        
        PrinterDevice printer = defaultPrinter;
        if (printer == null) {
            // 尝试使用第一个可用的打印机
            List<PrinterDevice> connected = getConnectedDevices();
            if (!connected.isEmpty()) {
                printer = connected.get(0);
            }
        }
        
        if (printer == null || !printer.isConnected()) {
            logger.warn("没有可用的打印机");
            return false;
        }
        
        logger.info("开始打印任务: {} - {}", task.getTaskName(), task.getTaskId());
        
        boolean success = printer.print(task);
        
        if (success) {
            // 打印后处理
            if (task.isOpenCashDrawer()) {
                printer.openCashDrawer();
            }
            if (task.isCutPaper()) {
                printer.cutPaper();
            }
            
            // 添加到历史记录
            printHistory.add(task);
            
            logger.info("打印任务完成: {}", task.getTaskId());
        } else {
            logger.error("打印任务失败: {}", task.getTaskId());
        }
        
        return success;
    }
    
    /**
     * 添加打印任务到队列
     * @param task 打印任务
     */
    public void addPrintTask(PrintTask task) {
        if (task != null) {
            taskQueue.add(task);
            logger.info("添加打印任务到队列: {}", task.getTaskName());
        }
    }
    
    /**
     * 处理队列中的所有打印任务
     */
    public void processQueue() {
        while (!taskQueue.isEmpty()) {
            PrintTask task = taskQueue.poll();
            if (task != null) {
                print(task);
            }
        }
    }
    
    /**
     * 获取打印历史
     * @return 打印历史记录
     */
    public List<PrintTask> getPrintHistory() {
        return new ArrayList<>(printHistory);
    }
    
    /**
     * 清空打印历史
     */
    public void clearPrintHistory() {
        printHistory.clear();
        logger.info("清空打印历史");
    }
    
    /**
     * 打开钱箱
     * @return 是否成功
     */
    public boolean openCashDrawer() {
        if (defaultPrinter != null && defaultPrinter.isConnected()) {
            return defaultPrinter.openCashDrawer();
        }
        logger.warn("没有可用的默认打印机");
        return false;
    }
    
    /**
     * 检查所有打印机状态
     * @return 打印机状态列表
     */
    public List<PrinterStatus> checkAllStatus() {
        List<PrinterStatus> statuses = new ArrayList<>();
        for (PrinterDevice device : devices.values()) {
            if (device.isConnected()) {
                PrinterStatus status = device.checkStatus();
                statuses.add(status);
            }
        }
        return statuses;
    }
    
    /**
     * 检查默认打印机状态
     * @return 打印机状态
     */
    public PrinterStatus checkDefaultStatus() {
        if (defaultPrinter != null && defaultPrinter.isConnected()) {
            return defaultPrinter.checkStatus();
        }
        return null;
    }
    
    /**
     * 销毁管理器，释放所有资源
     */
    public void dispose() {
        stopAllDevices();
        for (PrinterDevice device : devices.values()) {
            device.dispose();
        }
        devices.clear();
        taskQueue.clear();
        printHistory.clear();
        defaultPrinter = null;
        logger.info("打印设备管理器已销毁");
    }
}