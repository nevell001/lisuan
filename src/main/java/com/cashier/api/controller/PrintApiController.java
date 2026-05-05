package com.cashier.api.controller;

import com.cashier.printer.*;
import com.cashier.service.InvoicePrintService;
import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import com.cashier.dao.InvoiceDAO;
import com.cashier.api.ApiConfig;
import com.cashier.api.sync.SyncManager;
import com.cashier.api.sync.SyncEventType;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 打印管理 REST API 控制器
 * 网络打印机管理、打印任务提交、状态查询
 */
public class PrintApiController {
    private static final Logger logger = LoggerFactory.getLogger(PrintApiController.class);
    
    private static final int DEFAULT_PAPER_WIDTH = 48; // 80mm 纸张约48字符
    
    /**
     * 获取所有打印机列表
     * GET /api/printers
     */
    public static void listPrinters(Context ctx) {
        PrinterManager manager = PrinterManager.getInstance();
        List<PrinterDevice> devices = manager.getAllDevices();
        
        List<Map<String, Object>> printerList = devices.stream()
            .map(device -> {
                Map<String, Object> info = new HashMap<>();
                info.put("deviceId", device.getDeviceId());
                info.put("deviceName", device.getDeviceName());
                info.put("deviceType", device.getDeviceType().getDisplayName());
                info.put("status", device.getStatus().getDisplayName());
                info.put("connected", device.isConnected());
                
                if (device instanceof NetworkPrinterDevice) {
                    NetworkPrinterDevice netPrinter = (NetworkPrinterDevice) device;
                    info.put("hostAddress", netPrinter.getHostAddress());
                    info.put("port", netPrinter.getPort());
                    info.put("paperWidth", netPrinter.getPaperWidth());
                }
                
                return info;
            })
            .collect(Collectors.toList());
        
        ctx.json(Map.of(
            "success", true,
            "data", printerList,
            "defaultPrinter", manager.getDefaultPrinter() != null ? 
                manager.getDefaultPrinter().getDeviceId() : null,
            "total", printerList.size()
        ));
    }
    
    /**
     * 获取已连接的打印机
     * GET /api/printers/connected
     */
    public static void getConnectedPrinters(Context ctx) {
        PrinterManager manager = PrinterManager.getInstance();
        List<PrinterDevice> connected = manager.getConnectedDevices();
        
        List<Map<String, Object>> printerList = connected.stream()
            .map(device -> {
                Map<String, Object> info = new HashMap<>();
                info.put("deviceId", device.getDeviceId());
                info.put("deviceName", device.getDeviceName());
                info.put("deviceType", device.getDeviceType().getDisplayName());
                info.put("status", device.getStatus().getDisplayName());
                
                PrinterStatus status = device.checkStatus();
                if (status != null) {
                    info.put("paperRemaining", status.getPaperRemaining());
                    info.put("inkRemaining", status.getInkRemaining());
                    info.put("needsMaintenance", status.needsMaintenance());
                }
                
                return info;
            })
            .collect(Collectors.toList());
        
        ctx.json(Map.of(
            "success", true,
            "data", printerList,
            "total", printerList.size()
        ));
    }
    
    /**
     * 获取打印机详情
     * GET /api/printers/:id
     */
    public static void getPrinter(Context ctx) {
        String deviceId = ctx.pathParam("id");
        PrinterManager manager = PrinterManager.getInstance();
        PrinterDevice device = manager.getDevice(deviceId);
        
        if (device == null) {
            ctx.status(404).json(Map.of(
                "success", false,
                "error", "打印机不存在: " + deviceId
            ));
            return;
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("deviceId", device.getDeviceId());
        info.put("deviceName", device.getDeviceName());
        info.put("deviceType", device.getDeviceType().getDisplayName());
        info.put("status", device.getStatus().getDisplayName());
        info.put("connected", device.isConnected());
        info.put("configuration", device.getConfiguration());
        
        PrinterStatus status = device.checkStatus();
        if (status != null) {
            info.put("paperRemaining", status.getPaperRemaining());
            info.put("inkRemaining", status.getInkRemaining());
            info.put("headTemperature", status.getHeadTemperature());
            info.put("errorMessage", status.getErrorMessage());
            info.put("needsMaintenance", status.needsMaintenance());
        }
        
        if (device instanceof NetworkPrinterDevice) {
            NetworkPrinterDevice netPrinter = (NetworkPrinterDevice) device;
            info.put("hostAddress", netPrinter.getHostAddress());
            info.put("port", netPrinter.getPort());
            info.put("paperWidth", netPrinter.getPaperWidth());
        }
        
        ctx.json(Map.of("success", true, "data", info));
    }
    
    /**
     * 添加网络打印机
     * POST /api/printers/add
     * Body: { "name": "收银台打印机", "host": "192.168.1.100", "port": 9100 }
     */
    public static void addPrinter(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            
            String name = (String) body.get("name");
            String host = (String) body.get("host");
            Integer port = body.get("port") != null ? 
                ((Number) body.get("port")).intValue() : 9100;
            
            if (name == null || host == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "缺少必要参数: name, host"
                ));
                return;
            }
            
            // 生成设备ID
            String deviceId = "NET-" + host + "-" + port;
            
            // 创建网络打印机实例
            NetworkPrinterDevice printer = new NetworkPrinterDevice(deviceId, name, host, port);
            
            // 设置配置
            if (body.containsKey("timeout")) {
                Map<String, String> config = new HashMap<>();
                config.put("timeout", String.valueOf(((Number) body.get("timeout")).intValue()));
                printer.setConfiguration(config);
            }
            
            if (body.containsKey("paperWidth")) {
                printer.setPaperWidth(((Number) body.get("paperWidth")).intValue());
            }
            
            // 注册到管理器
            PrinterManager manager = PrinterManager.getInstance();
            manager.registerDevice(printer);
            
            // 初始化连接
            boolean initialized = printer.initialize();
            
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    "deviceId", deviceId,
                    "deviceName", name,
                    "hostAddress", host,
                    "port", port,
                    "initialized", initialized,
                    "status", printer.getStatus().getDisplayName()
                ),
                "message", initialized ? "打印机添加成功" : "打印机添加成功，但连接失败"
            ));
            
            // 广播打印机添加事件
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRINTER_ADDED, 
                Map.of("deviceId", deviceId, "deviceName", name));
            
        } catch (Exception e) {
            logger.error("添加打印机失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "添加打印机失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 连接打印机
     * POST /api/printers/:id/connect
     */
    public static void connectPrinter(Context ctx) {
        String deviceId = ctx.pathParam("id");
        PrinterManager manager = PrinterManager.getInstance();
        PrinterDevice device = manager.getDevice(deviceId);
        
        if (device == null) {
            ctx.status(404).json(Map.of(
                "success", false,
                "error", "打印机不存在: " + deviceId
            ));
            return;
        }
        
        boolean connected = device.start();
        
        ctx.json(Map.of(
            "success", connected,
            "data", Map.of(
                "deviceId", deviceId,
                "status", device.getStatus().getDisplayName(),
                "connected", device.isConnected()
            ),
            "message", connected ? "打印机连接成功" : "打印机连接失败"
        ));
        
        if (connected) {
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRINTER_CONNECTED, 
                Map.of("deviceId", deviceId, "deviceName", device.getDeviceName()));
        }
    }
    
    /**
     * 断开打印机
     * POST /api/printers/:id/disconnect
     */
    public static void disconnectPrinter(Context ctx) {
        String deviceId = ctx.pathParam("id");
        PrinterManager manager = PrinterManager.getInstance();
        PrinterDevice device = manager.getDevice(deviceId);
        
        if (device == null) {
            ctx.status(404).json(Map.of(
                "success", false,
                "error", "打印机不存在: " + deviceId
            ));
            return;
        }
        
        boolean disconnected = device.stop();
        
        ctx.json(Map.of(
            "success", disconnected,
            "data", Map.of(
                "deviceId", deviceId,
                "status", device.getStatus().getDisplayName(),
                "connected", device.isConnected()
            ),
            "message", disconnected ? "打印机已断开" : "打印机断开失败"
        ));
        
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRINTER_DISCONNECTED, 
            Map.of("deviceId", deviceId, "deviceName", device.getDeviceName()));
    }
    
    /**
     * 删除打印机
     * DELETE /api/printers/:id
     */
    public static void removePrinter(Context ctx) {
        String deviceId = ctx.pathParam("id");
        PrinterManager manager = PrinterManager.getInstance();
        
        PrinterDevice device = manager.getDevice(deviceId);
        if (device == null) {
            ctx.status(404).json(Map.of(
                "success", false,
                "error", "打印机不存在: " + deviceId
            ));
            return;
        }
        
        String deviceName = device.getDeviceName();
        manager.unregisterDevice(deviceId);
        
        ctx.json(Map.of(
            "success", true,
            "message", "打印机已删除: " + deviceName
        ));
        
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRINTER_REMOVED, 
            Map.of("deviceId", deviceId, "deviceName", deviceName));
    }
    
    /**
     * 设置默认打印机
     * POST /api/printers/:id/set-default
     */
    public static void setDefaultPrinter(Context ctx) {
        String deviceId = ctx.pathParam("id");
        PrinterManager manager = PrinterManager.getInstance();
        
        manager.setDefaultPrinter(deviceId);
        
        ctx.json(Map.of(
            "success", true,
            "data", Map.of(
                "defaultPrinter", manager.getDefaultPrinter() != null ? 
                    manager.getDefaultPrinter().getDeviceId() : null
            ),
            "message", "默认打印机已设置: " + deviceId
        ));
    }
    
    /**
     * 检查打印机状态
     * GET /api/printers/:id/status
     */
    public static void checkPrinterStatus(Context ctx) {
        String deviceId = ctx.pathParam("id");
        PrinterManager manager = PrinterManager.getInstance();
        PrinterDevice device = manager.getDevice(deviceId);
        
        if (device == null) {
            ctx.status(404).json(Map.of(
                "success", false,
                "error", "打印机不存在: " + deviceId
            ));
            return;
        }
        
        PrinterStatus status = device.checkStatus();
        
        if (status == null) {
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    "deviceId", deviceId,
                    "connected", device.isConnected(),
                    "status", device.getStatus().getDisplayName(),
                    "available", false
                )
            ));
        } else {
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    "deviceId", deviceId,
                    "connected", device.isConnected(),
                    "status", status.getStatus().getDisplayName(),
                    "paperRemaining", status.getPaperRemaining(),
                    "inkRemaining", status.getInkRemaining(),
                    "headTemperature", status.getHeadTemperature(),
                    "errorMessage", status.getErrorMessage(),
                    "needsMaintenance", status.needsMaintenance(),
                    "timestamp", status.getTimestamp(),
                    "available", !status.getStatus().isError()
                )
            ));
        }
    }
    
    /**
     * 打印测试页
     * POST /api/printers/:id/test
     */
    public static void printTest(Context ctx) {
        String deviceId = ctx.pathParam("id");
        PrinterManager manager = PrinterManager.getInstance();
        PrinterDevice device = manager.getDevice(deviceId);
        
        if (device == null) {
            ctx.status(404).json(Map.of(
                "success", false,
                "error", "打印机不存在: " + deviceId
            ));
            return;
        }
        
        if (!device.isConnected()) {
            ctx.status(400).json(Map.of(
                "success", false,
                "error", "打印机未连接"
            ));
            return;
        }
        
        // 构建测试内容
        StringBuilder content = new StringBuilder();
        content.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '-')).append("\n");
        content.append(EscPosUtils.centerText("打印机测试页", DEFAULT_PAPER_WIDTH)).append("\n");
        content.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '-')).append("\n");
        content.append("设备名称: ").append(device.getDeviceName()).append("\n");
        content.append("设备ID: ").append(device.getDeviceId()).append("\n");
        content.append("设备类型: ").append(device.getDeviceType().getDisplayName()).append("\n");
        
        if (device instanceof NetworkPrinterDevice) {
            NetworkPrinterDevice netPrinter = (NetworkPrinterDevice) device;
            content.append("IP地址: ").append(netPrinter.getHostAddress()).append("\n");
            content.append("端口: ").append(netPrinter.getPort()).append("\n");
        }
        
        content.append("打印时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        content.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '-')).append("\n");
        content.append("\n\n");
        
        // 创建打印任务
        PrintTask task = PrintTask.createReceiptTask(content.toString(), false, false);
        
        boolean success = device.print(task);
        
        if (success) {
            device.cutPaper();
        }
        
        ctx.json(Map.of(
            "success", success,
            "message", success ? "测试页打印成功" : "测试页打印失败"
        ));
    }
    
    /**
     * 打印小票
     * POST /api/printers/:id/receipt
     * Body: { "content": "...", "printLogo": true, "openCashDrawer": true }
     */
    public static void printReceipt(Context ctx) {
        String deviceId = ctx.pathParam("id");
        
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String content = (String) body.get("content");
            Boolean printLogo = body.get("printLogo") != null ? (Boolean) body.get("printLogo") : false;
            Boolean openCashDrawer = body.get("openCashDrawer") != null ? (Boolean) body.get("openCashDrawer") : false;
            
            if (content == null || content.isEmpty()) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "缺少打印内容"
                ));
                return;
            }
            
            PrinterManager manager = PrinterManager.getInstance();
            PrinterDevice device = deviceId != null ? manager.getDevice(deviceId) : manager.getDefaultPrinter();
            
            if (device == null || !device.isConnected()) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "打印机未连接"
                ));
                return;
            }
            
            PrintTask task = PrintTask.createReceiptTask(content, printLogo, openCashDrawer);
            
            boolean success = manager.print(task);
            
            ctx.json(Map.of(
                "success", success,
                "data", Map.of(
                    "taskId", task.getTaskId(),
                    "printerId", device.getDeviceId(),
                    "printerName", device.getDeviceName()
                ),
                "message", success ? "小票打印成功" : "小票打印失败"
            ));
            
        } catch (Exception e) {
            logger.error("打印小票失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "打印失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 打印发票
     * POST /api/printers/:id/invoice/:invoiceId
     */
    public static void printInvoice(Context ctx) {
        String deviceId = ctx.pathParam("id");
        String invoiceId = ctx.pathParam("invoiceId");
        
        try {
            Invoice invoice = InvoiceDAO.findById(invoiceId);
            
            if (invoice == null) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "error", "发票不存在: " + invoiceId
                ));
                return;
            }
            
            // 生成发票 HTML 文件
            String filePath = InvoicePrintService.generateHtml(invoice);
            
            PrinterManager manager = PrinterManager.getInstance();
            PrinterDevice device = deviceId != null ? manager.getDevice(deviceId) : manager.getDefaultPrinter();
            
            if (device == null) {
                ctx.json(Map.of(
                    "success", true,
                    "data", Map.of(
                        "invoiceId", invoiceId,
                        "filePath", filePath,
                        "printed", false
                    ),
                    "message", "发票HTML已生成，但无可用打印机"
                ));
                return;
            }
            
            // 对于网络打印机，生成文本格式的小票
            String invoiceText = generateInvoiceText(invoice);
            
            PrintTask task = PrintTask.createReceiptTask(invoiceText, false, false);
            boolean success = manager.print(task);
            
            if (success) {
                device.cutPaper();
                
                // 更新打印信息
                InvoiceDAO.updatePrintInfo(invoiceId, filePath, null);
                
                // 广播打印事件
                SyncManager.getInstance().broadcastSyncEvent(SyncEventType.INVOICE_PRINTED, 
                    Map.of("invoiceId", invoiceId, "invoiceNumber", invoice.invoiceNumber));
            }
            
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    "invoiceId", invoiceId,
                    "invoiceNumber", invoice.invoiceNumber,
                    "filePath", filePath,
                    "printerId", device.getDeviceId(),
                    "printed", success
                ),
                "message", success ? "发票打印成功" : "发票打印失败，但HTML已生成"
            ));
            
        } catch (Exception e) {
            logger.error("打印发票失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "打印发票失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 生成发票文本格式（用于热敏打印机）
     */
    private static String generateInvoiceText(Invoice invoice) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '=')).append("\n");
        sb.append(EscPosUtils.centerText("电子发票", DEFAULT_PAPER_WIDTH)).append("\n");
        sb.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '=')).append("\n");
        
        sb.append("发票号码: ").append(invoice.invoiceNumber).append("\n");
        sb.append("发票代码: ").append(invoice.invoiceCode).append("\n");
        sb.append("开票时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(invoice.createTime)).append("\n");
        
        sb.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '-')).append("\n");
        sb.append("购买方: ").append(invoice.buyerName).append("\n");
        if (invoice.buyerTaxId != null && !invoice.buyerTaxId.isEmpty()) {
            sb.append("纳税人识别号: ").append(invoice.buyerTaxId).append("\n");
        }
        
        sb.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '-')).append("\n");
        sb.append("销售方: ").append(invoice.sellerName).append("\n");
        sb.append("纳税人识别号: ").append(invoice.sellerTaxId).append("\n");
        
        sb.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '-')).append("\n");
        sb.append("商品明细:\n");
        
        if (invoice.items != null) {
            for (InvoiceItem item : invoice.items) {
                sb.append(item.productName).append("\n");
                sb.append("  数量: ").append(item.quantity).append("  金额: ").append(item.amount).append("\n");
            }
        }
        
        sb.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '-')).append("\n");
        sb.append("合计金额: ").append(invoice.totalAmount).append("\n");
        sb.append("税额: ").append(invoice.taxAmount).append("\n");
        sb.append("价税合计: ").append(invoice.finalAmount).append("\n");
        
        sb.append(EscPosUtils.createSeparator(DEFAULT_PAPER_WIDTH, '=')).append("\n");
        sb.append("\n\n");
        
        return sb.toString();
    }
    
    /**
     * 打开钱箱
     * POST /api/printers/:id/cashdrawer
     */
    public static void openCashDrawer(Context ctx) {
        String deviceId = ctx.pathParam("id");
        PrinterManager manager = PrinterManager.getInstance();
        PrinterDevice device = deviceId != null ? manager.getDevice(deviceId) : manager.getDefaultPrinter();
        
        if (device == null || !device.isConnected()) {
            ctx.status(400).json(Map.of(
                "success", false,
                "error", "打印机未连接"
            ));
            return;
        }
        
        boolean success = device.openCashDrawer();
        
        ctx.json(Map.of(
            "success", success,
            "message", success ? "钱箱已打开" : "打开钱箱失败"
        ));
    }
    
    /**
     * 获取打印历史
     * GET /api/printers/history
     */
    public static void getPrintHistory(Context ctx) {
        PrinterManager manager = PrinterManager.getInstance();
        List<PrintTask> history = manager.getPrintHistory();
        
        List<Map<String, Object>> historyList = history.stream()
            .map(task -> {
                Map<String, Object> info = new HashMap<>();
                info.put("taskId", task.getTaskId());
                info.put("taskName", task.getTaskName());
                info.put("taskType", task.getTaskType().getDisplayName());
                info.put("createdAt", task.getCreatedAt());
                info.put("copies", task.getCopies());
                return info;
            })
            .collect(Collectors.toList());
        
        ctx.json(Map.of(
            "success", true,
            "data", historyList,
            "total", historyList.size()
        ));
    }
    
    /**
     * 发现网络打印机（扫描局域网）
     * GET /api/printers/discover?subnet=192.168.1&port=9100
     */
    public static void discoverPrinters(Context ctx) {
        String subnet = ctx.queryParam("subnet");
        Integer port = ctx.queryParamAsClass("port", Integer.class).getOrDefault(9100);
        
        if (subnet == null) {
            // 自动获取本机所在子网
            subnet = getDefaultSubnet();
        }
        
        List<Map<String, Object>> discovered = new ArrayList<>();
        
        // 扫描子网中的打印机端口
        for (int i = 1; i < 255; i++) {
            String host = subnet + "." + i;
            
            if (checkPrinterPort(host, port)) {
                discovered.add(Map.of(
                    "host", host,
                    "port", port,
                    "status", "在线",
                    "deviceId", "NET-" + host + "-" + port
                ));
            }
        }
        
        ctx.json(Map.of(
            "success", true,
            "data", discovered,
            "subnet", subnet,
            "port", port,
            "total", discovered.size()
        ));
    }
    
    /**
     * 获取默认子网
     */
    private static String getDefaultSubnet() {
        try {
            String localHost = java.net.InetAddress.getLocalHost().getHostAddress();
            int lastDot = localHost.lastIndexOf('.');
            return localHost.substring(0, lastDot);
        } catch (Exception e) {
            return "192.168.1";
        }
    }
    
    /**
     * 检查打印机端口是否可用
     */
    private static boolean checkPrinterPort(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}