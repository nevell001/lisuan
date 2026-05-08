package com.cashier.printer;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网络打印机设备实现
 * 支持 TCP/IP Socket 连接的热敏打印机（ESC/POS 协议）
 * 典型端口：9100（RAW）、515（LPR）
 */
public class NetworkPrinterDevice implements PrinterDevice {
    
    private static final Logger logger = LoggerFactoryUtil.getLogger(NetworkPrinterDevice.class);
    
    // 默认配置
    private static final int DEFAULT_PORT = 9100;
    private static final int DEFAULT_TIMEOUT = 5000; // 5秒连接超时
    private static final int DEFAULT_PAPER_WIDTH = 80; // 80mm 热敏纸
    
    // 设备信息
    private final String deviceId;
    private final String deviceName;
    private final String hostAddress;
    private final int port;
    
    // 连接状态
    private volatile boolean connected = false;
    private volatile PrinterDeviceStatus status = PrinterDeviceStatus.UNINITIALIZED;
    
    // 配置参数
    private Map<String, String> configuration;
    private int timeout = DEFAULT_TIMEOUT;
    private int paperWidth = DEFAULT_PAPER_WIDTH;
    
    // Socket 连接
    private Socket socket;
    private OutputStream outputStream;
    
    /**
     * 构造函数
     * @param deviceId 设备ID
     * @param deviceName 设备名称
     * @param hostAddress 打印机IP地址
     * @param port 打印机端口
     */
    public NetworkPrinterDevice(String deviceId, String deviceName, String hostAddress, int port) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.hostAddress = hostAddress;
        this.port = port;
        this.configuration = new HashMap<>();
    }
    
    /**
     * 简化构造函数（使用默认端口 9100）
     */
    public NetworkPrinterDevice(String deviceId, String deviceName, String hostAddress) {
        this(deviceId, deviceName, hostAddress, DEFAULT_PORT);
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
    public PrinterDeviceType getDeviceType() {
        return PrinterDeviceType.NETWORK;
    }
    
    @Override
    public boolean initialize() {
        if (status == PrinterDeviceStatus.CONNECTED) {
            logger.warn("打印机已初始化: {}", deviceId);
            return true;
        }
        
        status = PrinterDeviceStatus.INITIALIZING;
        logger.info("初始化网络打印机: {} ({})", deviceName, hostAddress);
        
        try {
            // 测试连接
            Socket testSocket = new Socket();
            testSocket.connect(new InetSocketAddress(hostAddress, port), timeout);
            testSocket.close();
            
            status = PrinterDeviceStatus.READY;
            logger.info("网络打印机初始化成功: {}", deviceId);
            return true;
        } catch (IOException e) {
            status = PrinterDeviceStatus.ERROR;
            logger.error("网络打印机初始化失败: {} - {}", deviceId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean start() {
        if (status == PrinterDeviceStatus.CONNECTED) {
            return true;
        }
        
        if (status != PrinterDeviceStatus.READY) {
            if (!initialize()) {
                return false;
            }
        }
        
        status = PrinterDeviceStatus.STARTING;
        logger.info("启动网络打印机: {}", deviceId);
        
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(hostAddress, port), timeout);
            socket.setSoTimeout(timeout);
            socket.setKeepAlive(true);
            
            outputStream = socket.getOutputStream();
            
            connected = true;
            status = PrinterDeviceStatus.CONNECTED;
            
            logger.info("网络打印机已连接: {} ({})", deviceName, hostAddress);
            return true;
        } catch (IOException e) {
            status = PrinterDeviceStatus.ERROR;
            connected = false;
            logger.error("网络打印机连接失败: {} - {}", deviceId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean stop() {
        if (!connected) {
            return true;
        }
        
        logger.info("停止网络打印机: {}", deviceId);
        
        try {
            // 发送切纸和关闭指令
            if (outputStream != null) {
                sendESCPOSCommand(EscPosUtils.CUT_PAPER);
                outputStream.flush();
                outputStream.close();
            }
            
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            connected = false;
            status = PrinterDeviceStatus.DISCONNECTED;
            
            logger.info("网络打印机已断开: {}", deviceId);
            return true;
        } catch (IOException e) {
            logger.error("网络打印机断开失败: {} - {}", deviceId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && socket.isConnected();
    }
    
    @Override
    public PrinterDeviceStatus getStatus() {
        return status;
    }
    
    @Override
    public Map<String, String> getConfiguration() {
        return new HashMap<>(configuration);
    }
    
    @Override
    public void setConfiguration(Map<String, String> config) {
        this.configuration = new HashMap<>(config);
        
        // 解析配置参数
        if (config.containsKey("timeout")) {
            try {
                timeout = Integer.parseInt(config.get("timeout"));
            } catch (NumberFormatException e) {
                logger.warn("无效的超时配置: {}", config.get("timeout"));
            }
        }
        
        if (config.containsKey("paperWidth")) {
            try {
                paperWidth = Integer.parseInt(config.get("paperWidth"));
            } catch (NumberFormatException e) {
                logger.warn("无效的纸张宽度配置: {}", config.get("paperWidth"));
            }
        }
    }
    
    @Override
    public boolean printText(String text) {
        if (!ensureConnected()) {
            logger.warn("打印机未连接，无法打印: {}", deviceId);
            return false;
        }
        
        try {
            status = PrinterDeviceStatus.PRINTING;
            
            // 发送初始化指令
            sendESCPOSCommand(EscPosUtils.INIT);
            
            // 发送文本内容
            outputStream.write(text.getBytes("GBK"));
            outputStream.write(EscPosUtils.LINE_FEED);
            
            outputStream.flush();
            
            status = PrinterDeviceStatus.CONNECTED;
            logger.info("文本打印完成: {} - {} bytes", deviceId, text.length());
            return true;
        } catch (IOException e) {
            status = PrinterDeviceStatus.ERROR;
            logger.error("文本打印失败: {} - {}", deviceId, e.getMessage());
            reconnect();
            return false;
        }
    }
    
    @Override
    public boolean print(PrintTask task) {
        if (!ensureConnected()) {
            logger.warn("打印机未连接，无法打印任务: {}", deviceId);
            return false;
        }
        
        try {
            status = PrinterDeviceStatus.PRINTING;
            
            // 初始化打印机
            sendESCPOSCommand(EscPosUtils.INIT);
            
            // 打印 Logo
            if (task.isPrintLogo()) {
                printLogo();
            }
            
            // 打印内容
            String content = task.getContent();
            outputStream.write(content.getBytes("GBK"));
            
            // 添加换行
            outputStream.write(EscPosUtils.LINE_FEED);
            outputStream.write(EscPosUtils.LINE_FEED);
            
            outputStream.flush();
            
            status = PrinterDeviceStatus.CONNECTED;
            logger.info("打印任务完成: {} - {}", deviceId, task.getTaskId());
            return true;
        } catch (IOException e) {
            status = PrinterDeviceStatus.ERROR;
            logger.error("打印任务失败: {} - {}", deviceId, e.getMessage());
            reconnect();
            return false;
        }
    }
    
    /**
     * 发送 ESC/POS 命令
     */
    private void sendESCPOSCommand(byte[] command) throws IOException {
        if (outputStream != null) {
            outputStream.write(command);
        }
    }
    
    /**
     * 打印 Logo（如果配置了）
     */
    private void printLogo() throws IOException {
        // 尝试从配置获取 Logo 路径
        String logoPath = configuration.get("logoPath");

        if (logoPath != null && !logoPath.isEmpty()) {
            // 尝试加载图片 Logo
            byte[] logoData = LogoPrinter.loadLogoFromFile(logoPath, paperWidth);
            if (logoData != null) {
                byte[] fullCommand = LogoPrinter.generateLogoPrintCommand(logoData, true);
                if (fullCommand.length > 0) {
                    outputStream.write(fullCommand);
                    logger.debug("已打印图片 Logo: {}", logoPath);
                    return;
                }
            }
        }

        // 尝试从资源加载默认 Logo
        byte[] resourceLogo = LogoPrinter.loadLogoFromResource("images/logo.png", paperWidth);
        if (resourceLogo != null) {
            byte[] fullCommand = LogoPrinter.generateLogoPrintCommand(resourceLogo, true);
            if (fullCommand.length > 0) {
                outputStream.write(fullCommand);
                logger.debug("已打印默认资源 Logo");
                return;
            }
        }

        // 如果没有图片 Logo，使用文本 Logo 作为备用
        byte[] textLogo = LogoPrinter.createTextLogo("收银系统");
        if (textLogo != null) {
            outputStream.write(textLogo);
            logger.debug("已打印文本 Logo");
        }
    }
    
    @Override
    public boolean openCashDrawer() {
        if (!ensureConnected()) {
            return false;
        }
        
        try {
            // 发送打开钱箱指令
            sendESCPOSCommand(EscPosUtils.OPEN_CASH_DRAWER);
            outputStream.flush();
            
            logger.info("钱箱已打开: {}", deviceId);
            return true;
        } catch (IOException e) {
            logger.error("打开钱箱失败: {} - {}", deviceId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean cutPaper() {
        if (!ensureConnected()) {
            return false;
        }
        
        try {
            // 走纸后切纸
            outputStream.write(EscPosUtils.LINE_FEED);
            outputStream.write(EscPosUtils.LINE_FEED);
            outputStream.write(EscPosUtils.LINE_FEED);
            sendESCPOSCommand(EscPosUtils.CUT_PAPER);
            outputStream.flush();
            
            logger.info("纸张已切割: {}", deviceId);
            return true;
        } catch (IOException e) {
            logger.error("切纸失败: {} - {}", deviceId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public PrinterStatus checkStatus() {
        if (!isConnected()) {
            return new PrinterStatus(deviceId, PrinterDeviceStatus.DISCONNECTED);
        }
        
        try {
            // 尝试发送状态查询指令
            // ESC/POS DLE EOT n 查询打印机状态
            outputStream.write(new byte[]{0x10, 0x04, 0x01});
            outputStream.flush();
            
            // 简化处理：假设连接正常
            return new PrinterStatus(deviceId, PrinterDeviceStatus.CONNECTED, null, 100, 100, 25.0);
        } catch (IOException e) {
            return new PrinterStatus(deviceId, PrinterDeviceStatus.ERROR, e.getMessage(), 0, 0, null);
        }
    }
    
    @Override
    public void dispose() {
        stop();
        status = PrinterDeviceStatus.DISPOSED;
        logger.info("网络打印机已销毁: {}", deviceId);
    }
    
    /**
     * 确保连接状态
     */
    private boolean ensureConnected() {
        if (isConnected()) {
            return true;
        }
        return reconnect();
    }
    
    /**
     * 重连打印机
     */
    private boolean reconnect() {
        logger.info("尝试重连打印机: {}", deviceId);
        
        // 先断开
        stop();
        
        // 等待一小段时间
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 重新连接
        return start();
    }
    
    /**
     * 获取主机地址
     */
    public String getHostAddress() {
        return hostAddress;
    }
    
    /**
     * 获取端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 获取纸张宽度
     */
    public int getPaperWidth() {
        return paperWidth;
    }
    
    /**
     * 设置纸张宽度
     */
    public void setPaperWidth(int width) {
        this.paperWidth = width;
    }
    
    @Override
    public String toString() {
        return "NetworkPrinterDevice{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", hostAddress='" + hostAddress + '\'' +
                ", port=" + port +
                ", status=" + status +
                ", connected=" + connected +
                '}';
    }
}