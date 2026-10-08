package com.cashier.api.controller;

import com.cashier.printer.*;
import com.cashier.service.InvoicePrintService;
import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import com.cashier.model.User;
import com.cashier.dao.DAOFactory;
import com.cashier.api.sync.SyncManager;
import com.cashier.api.sync.SyncEventType;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 打印管理 REST API 控制器
 * 网络打印机管理、打印任务提交、状态查询
 */
public class PrintApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PrintApiController.class);
    
    private static final int DEFAULT_PAPER_WIDTH = 48; // 80mm 纸张约48字符
    private static final String DEVICE_ID_FIELD = "deviceId";
    private static final String DEVICE_NAME_FIELD = "deviceName";
    private static final String CONNECTED_FIELD = "connected";
    private static final String PAPER_WIDTH_FIELD = "paperWidth";
    private static final String INVOICE_ID_FIELD = "invoiceId";
    private static final int DEFAULT_PRINT_HISTORY_LIMIT = 100;
    private static final int MAX_PRINT_HISTORY_LIMIT = 500;
    private static final int DEFAULT_DISCOVERY_HOST_LIMIT = 64;
    private static final int MAX_DISCOVERY_HOST_LIMIT = 254;
    private static final int DEFAULT_DISCOVERY_TIMEOUT_MS = 150;
    private static final int MAX_DISCOVERY_TIMEOUT_MS = 1000;
    private static final int DEFAULT_DISCOVERY_PORT = 9100;
    /** 标准网络打印端口：RAW 9100、LPD 515、IPP 631 */
    private static final Set<Integer> STANDARD_PRINTER_PORTS = Set.of(9100, 515, 631);
    /** 小票内容上限，避免用一单请求把打印机缓冲区灌满 */
    private static final int MAX_RECEIPT_CONTENT_LENGTH = 8192;
    /** 小票内容允许的字符：可打印文本 + 换行/回车/制表符；其余控制字符（含 ESC/POS 的 0x1B）一律拒绝 */
    private static final Pattern UNSAFE_RECEIPT_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
    
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
                info.put(DEVICE_ID_FIELD, device.getDeviceId());
                info.put(DEVICE_NAME_FIELD, device.getDeviceName());
                info.put("deviceType", device.getDeviceType().name());
        info.put("deviceTypeName", deviceTypeName(ctx, device.getDeviceType()));
                info.put("status", device.getStatus().name());
        info.put("statusName", deviceStatusName(ctx, device.getStatus()));
                info.put(CONNECTED_FIELD, device.isConnected());
                
                if (device instanceof NetworkPrinterDevice) {
                    NetworkPrinterDevice netPrinter = (NetworkPrinterDevice) device;
                    info.put("hostAddress", netPrinter.getHostAddress());
                    info.put("port", netPrinter.getPort());
                    info.put(PAPER_WIDTH_FIELD, netPrinter.getPaperWidth());
                }
                
                return info;
            })
            .collect(Collectors.toList());
        
        // Map.of 不接受 null 值：未设默认打印机时 defaultPrinter 就是 null，
        // 原写法（三元给 null）会让整个 /api/printers 恒 500（冒烟实测）。
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", printerList);
        result.put("defaultPrinter", manager.getDefaultPrinter() != null
            ? manager.getDefaultPrinter().getDeviceId() : null);
        result.put("total", printerList.size());
        ctx.json(result);
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
                info.put(DEVICE_ID_FIELD, device.getDeviceId());
                info.put(DEVICE_NAME_FIELD, device.getDeviceName());
                info.put("deviceType", device.getDeviceType().name());
        info.put("deviceTypeName", deviceTypeName(ctx, device.getDeviceType()));
                info.put("status", device.getStatus().name());
        info.put("statusName", deviceStatusName(ctx, device.getStatus()));
                
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
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.not_found", deviceId)
            ));
            return;
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put(DEVICE_ID_FIELD, device.getDeviceId());
        info.put(DEVICE_NAME_FIELD, device.getDeviceName());
        info.put("deviceType", device.getDeviceType().name());
        info.put("deviceTypeName", deviceTypeName(ctx, device.getDeviceType()));
        info.put("status", device.getStatus().name());
        info.put("statusName", deviceStatusName(ctx, device.getStatus()));
        info.put(CONNECTED_FIELD, device.isConnected());
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
            info.put(PAPER_WIDTH_FIELD, netPrinter.getPaperWidth());
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
            Map<?, ?> body = ApiRequest.parse(ctx, Map.class);
            if (body == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", com.cashier.api.ApiMessages.text(ctx, "api.common.bad_request")
                ));
                return;
            }
            
            String name = getString(body, "name", null);
            String host = getString(body, "host", null);
            int port = getInt(body, "port", 9100);
            
            if (name == null || host == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", com.cashier.api.ApiMessages.text(ctx, "api.common.missing_param", "name, host")
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
                config.put("timeout", String.valueOf(getInt(body, "timeout", 0)));
                printer.setConfiguration(config);
            }
            
            if (body.containsKey(PAPER_WIDTH_FIELD)) {
                printer.setPaperWidth(getInt(body, PAPER_WIDTH_FIELD, DEFAULT_PAPER_WIDTH));
            }
            
            // 注册到管理器
            PrinterManager manager = PrinterManager.getInstance();
            manager.registerDevice(printer);
            
            // 初始化连接
            boolean initialized = printer.initialize();
            
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    DEVICE_ID_FIELD, deviceId,
                    DEVICE_NAME_FIELD, name,
                    "hostAddress", host,
                    "port", port,
                    "initialized", initialized,
                    "status", printer.getStatus().name(),
                    "statusName", deviceStatusName(ctx, printer.getStatus())
                ),
                "message", com.cashier.api.ApiMessages.text(ctx,
                    initialized ? "api.printer.added" : "api.printer.added_but_disconnected")
            ));
            
            // 广播打印机添加事件
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRINTER_ADDED, 
                Map.of(DEVICE_ID_FIELD, deviceId, DEVICE_NAME_FIELD, name));
            
        } catch (Exception e) {
            logger.error("添加打印机失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.add_failed")
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
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.not_found", deviceId)
            ));
            return;
        }
        
        boolean connected = device.start();
        
        ctx.json(Map.of(
            "success", connected,
            "data", Map.of(
                DEVICE_ID_FIELD, deviceId,
                "status", device.getStatus().name(),
                    "statusName", deviceStatusName(ctx, device.getStatus()),
                CONNECTED_FIELD, device.isConnected()
            ),
            "message", com.cashier.api.ApiMessages.text(ctx,
                    connected ? "api.printer.connected" : "api.printer.connect_failed")
        ));
        
        if (connected) {
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRINTER_CONNECTED, 
                Map.of(DEVICE_ID_FIELD, deviceId, DEVICE_NAME_FIELD, device.getDeviceName()));
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
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.not_found", deviceId)
            ));
            return;
        }
        
        boolean disconnected = device.stop();
        
        ctx.json(Map.of(
            "success", disconnected,
            "data", Map.of(
                DEVICE_ID_FIELD, deviceId,
                "status", device.getStatus().name(),
                    "statusName", deviceStatusName(ctx, device.getStatus()),
                CONNECTED_FIELD, device.isConnected()
            ),
            "message", com.cashier.api.ApiMessages.text(ctx,
                    disconnected ? "api.printer.disconnected" : "api.printer.disconnect_failed")
        ));
        
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRINTER_DISCONNECTED, 
            Map.of(DEVICE_ID_FIELD, deviceId, DEVICE_NAME_FIELD, device.getDeviceName()));
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
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.not_found", deviceId)
            ));
            return;
        }
        
        String deviceName = device.getDeviceName();
        manager.unregisterDevice(deviceId);
        
        ctx.json(Map.of(
            "success", true,
            "message", com.cashier.api.ApiMessages.text(ctx, "api.printer.removed", deviceName)
        ));
        
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.PRINTER_REMOVED, 
            Map.of(DEVICE_ID_FIELD, deviceId, DEVICE_NAME_FIELD, deviceName));
    }
    
    /**
     * 设置默认打印机
     * POST /api/printers/:id/set-default
     */
    public static void setDefaultPrinter(Context ctx) {
        String deviceId = ctx.pathParam("id");
        PrinterManager manager = PrinterManager.getInstance();
        
        manager.setDefaultPrinter(deviceId);
        
        // 同 listPrinters：Map.of 不接受 null，设置失败时这里本意是返回 null 而不是 500
        Map<String, Object> data = new HashMap<>();
        data.put("defaultPrinter", manager.getDefaultPrinter() != null
            ? manager.getDefaultPrinter().getDeviceId() : null);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        result.put("message", com.cashier.api.ApiMessages.text(ctx, "api.printer.default_set", deviceId));
        ctx.json(result);
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
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.not_found", deviceId)
            ));
            return;
        }
        
        PrinterStatus status = device.checkStatus();
        
        if (status == null) {
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    DEVICE_ID_FIELD, deviceId,
                    CONNECTED_FIELD, device.isConnected(),
                    "status", device.getStatus().name(),
                    "statusName", deviceStatusName(ctx, device.getStatus()),
                    "available", false
                )
            ));
        } else {
            // 字段数超过 Map.of 的 10 对上限，改用 LinkedHashMap（TD-041 新增了 statusName）
            Map<String, Object> statusData = new java.util.LinkedHashMap<>();
            statusData.put(DEVICE_ID_FIELD, deviceId);
            statusData.put(CONNECTED_FIELD, device.isConnected());
            statusData.put("status", status.getStatus().name());
            statusData.put("statusName", deviceStatusName(ctx, status.getStatus()));
            statusData.put("paperRemaining", status.getPaperRemaining());
            statusData.put("inkRemaining", status.getInkRemaining());
            statusData.put("headTemperature", status.getHeadTemperature());
            statusData.put("errorMessage", status.getErrorMessage());
            statusData.put("needsMaintenance", status.needsMaintenance());
            statusData.put("timestamp", status.getTimestamp());
            statusData.put("available", !status.getStatus().isError());
            ctx.json(Map.of("success", true, "data", statusData));
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
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.not_found", deviceId)
            ));
            return;
        }
        
        if (!device.isConnected()) {
            ctx.status(400).json(Map.of(
                "success", false,
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.not_connected")
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
        
        content.append("打印时间: ").append(com.cashier.util.DateTimeFormats.formatStandard(LocalDateTime.now(ZoneId.systemDefault()))).append("\n");
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
            "message", com.cashier.api.ApiMessages.text(ctx,
                success ? "api.printer.test_ok" : "api.printer.test_failed")
        ));
    }
    
    /**
     * 打印小票
     * POST /api/printers/:id/receipt
     * Body: { "content": "...", "printLogo": true, "openCashDrawer": true }
     *
     * <p>{@code content} 只接受纯文本：长度上限 8KB，且不允许控制字符（含 ESC/POS 指令），
     * 避免任意登录用户把任意字节推给网络打印机。</p>
     */
    public static void printReceipt(Context ctx) {
        String deviceId = ctx.pathParam("id");
        
        try {
            Map<?, ?> body = ApiRequest.parse(ctx, Map.class);
            if (body == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", com.cashier.api.ApiMessages.text(ctx, "api.common.bad_request")
                ));
                return;
            }
            String content = getString(body, "content", null);
            boolean printLogo = getBoolean(body, "printLogo", false);
            boolean openCashDrawer = getBoolean(body, "openCashDrawer", false);

            // TD-037：开钱箱是受控操作（POST /api/printers/{id}/cashdrawer 一直是管理员专属），
            // 但请求体带 openCashDrawer=true 的小票打印能绕开那条限制（同一台打印机同一条指令）。
            // 角色门禁在中间件里按路径判定，看不到请求体，所以这里按当前用户角色兜住，fail-closed。
            if (openCashDrawer && !isAdmin(ctx)) {
                logger.warn("拒绝非管理员通过小票打印开启钱箱: deviceId={}, user={}", deviceId, currentUsername(ctx));
                ctx.status(403).json(Map.of(
                    "success", false,
                    "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.cash_drawer_admin_only")
                ));
                return;
            }
            
            if (content == null || content.isEmpty()) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.content_required")
                ));
                return;
            }
            // 只接受纯文本小票：限制长度并拒绝控制字符，避免把任意字节（含 ESC/POS 指令）推给打印机
            if (content.length() > MAX_RECEIPT_CONTENT_LENGTH) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.content_too_long")
                ));
                return;
            }
            if (UNSAFE_RECEIPT_CHARS.matcher(content).find()) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.content_unsafe")
                ));
                return;
            }
            
            PrinterManager manager = PrinterManager.getInstance();
            PrinterDevice device = deviceId != null ? manager.getDevice(deviceId) : manager.getDefaultPrinter();
            
            if (device == null || !device.isConnected()) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.not_connected")
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
                "message", com.cashier.api.ApiMessages.text(ctx,
                success ? "api.printer.receipt_ok" : "api.printer.receipt_failed")
            ));
            
        } catch (Exception e) {
            logger.error("打印小票失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.print_failed")
            ));
        }
    }
    
    /**
     * 打印发票
     * POST /api/printers/:id/invoice/:invoiceId
     */
    public static void printInvoice(Context ctx) {
        String deviceId = ctx.pathParam("id");
        String invoiceId = ctx.pathParam(INVOICE_ID_FIELD);
        
        try {
            Invoice invoice = DAOFactory.getInstance().getInvoiceDAO().findById(invoiceId);
            
            if (invoice == null) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.invoice_not_found", invoiceId)
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
                        INVOICE_ID_FIELD, invoiceId,
                        "filePath", filePath,
                        "printed", false
                    ),
                    "message", com.cashier.api.ApiMessages.text(ctx, "api.printer.invoice_html_only")
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
                DAOFactory.getInstance().getInvoiceDAO().updatePrintInfo(invoiceId, filePath, null);
                
                // 广播打印事件
                SyncManager.getInstance().broadcastSyncEvent(SyncEventType.INVOICE_PRINTED, 
                    Map.of(INVOICE_ID_FIELD, invoiceId, "invoiceNumber", invoice.invoiceNumber));
            }
            
            ctx.json(Map.of(
                "success", true,
                "data", Map.of(
                    INVOICE_ID_FIELD, invoiceId,
                    "invoiceNumber", invoice.invoiceNumber,
                    "filePath", filePath,
                    "printerId", device.getDeviceId(),
                    "printed", success
                ),
                "message", com.cashier.api.ApiMessages.text(ctx,
                    success ? "api.printer.invoice_ok" : "api.printer.invoice_failed_html_kept")
            ));
            
        } catch (Exception e) {
            logger.error("打印发票失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.invoice_print_failed")
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
        sb.append("开票时间: ").append(com.cashier.util.DateTimeFormats.formatStandard(
            invoice.createTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        )).append("\n");
        
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
                "error", com.cashier.api.ApiMessages.text(ctx, "api.printer.not_connected")
            ));
            return;
        }
        
        boolean success = device.openCashDrawer();
        
        ctx.json(Map.of(
            "success", success,
            "message", com.cashier.api.ApiMessages.text(ctx,
                success ? "api.printer.cash_drawer_ok" : "api.printer.cash_drawer_failed")
        ));
    }
    
    /**
     * 获取打印历史
     * GET /api/printers/history
     */
    public static void getPrintHistory(Context ctx) {
        PrinterManager manager = PrinterManager.getInstance();
        int requestedLimit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(DEFAULT_PRINT_HISTORY_LIMIT);
        int limit = Math.max(1, Math.min(requestedLimit, MAX_PRINT_HISTORY_LIMIT));
        List<PrintTask> history = manager.getRecentPrintHistory(limit);
        
        List<Map<String, Object>> historyList = history.stream()
            .map(task -> {
                Map<String, Object> info = new HashMap<>();
                info.put("taskId", task.getTaskId());
                info.put("taskName", task.getTaskName());
                info.put("taskType", task.getTaskType().name());
        info.put("taskTypeName", taskTypeName(ctx, task.getTaskType()));
                info.put("createdAt", task.getCreatedAt());
                info.put("startedAt", task.getStartedAt());
                info.put("finishedAt", task.getFinishedAt());
                info.put("copies", task.getCopies());
                info.put("status", task.getStatus().name());
        info.put("statusName", taskStatusName(ctx, task.getStatus()));
                info.put("errorMessage", task.getErrorMessage());
                return info;
            })
            .collect(Collectors.toList());
        
        ctx.json(Map.of(
            "success", true,
            "data", historyList,
            "limit", limit,
            "total", historyList.size()
        ));
    }
    
    /**
     * 发现网络打印机（扫描局域网）。
     *
     * <p>GET /api/printers/discover?port=9100&amp;limit=64&amp;timeoutMs=150</p>
     *
     * <p>只扫描<b>本机所在子网</b>，且端口限定在标准打印端口：此前允许调用方指定任意 IPv4 /24
     * 与 1-65535 端口，等于给任意登录用户一个内网端口扫描器。</p>
     */
    public static void discoverPrinters(Context ctx) {
        Integer requestedPort = ctx.queryParamAsClass("port", Integer.class).getOrDefault(DEFAULT_DISCOVERY_PORT);
        int port = STANDARD_PRINTER_PORTS.contains(requestedPort) ? requestedPort : DEFAULT_DISCOVERY_PORT;
        int requestedLimit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(DEFAULT_DISCOVERY_HOST_LIMIT);
        int hostLimit = Math.max(1, Math.min(requestedLimit, MAX_DISCOVERY_HOST_LIMIT));
        int requestedTimeout = ctx.queryParamAsClass("timeoutMs", Integer.class).getOrDefault(DEFAULT_DISCOVERY_TIMEOUT_MS);
        int timeoutMs = Math.max(50, Math.min(requestedTimeout, MAX_DISCOVERY_TIMEOUT_MS));

        String subnet = getDefaultSubnet();
        List<Map<String, Object>> discovered = new ArrayList<>();
        
        // 扫描本机子网中的打印机端口
        for (int i = 1; i <= hostLimit; i++) {
            String host = subnet + "." + i;
            
            if (checkPrinterPort(host, port, timeoutMs)) {
                // 与枚举同口径：代码 + 本地化显示名（TD-041）
                Map<String, Object> found = new HashMap<>();
                found.put("host", host);
                found.put("port", port);
                found.put("status", PrinterDeviceStatus.CONNECTED.name());
                found.put("statusName", deviceStatusName(ctx, PrinterDeviceStatus.CONNECTED));
                found.put(DEVICE_ID_FIELD, "NET-" + host + "-" + port);
                discovered.add(found);
            }
        }
        
        ctx.json(Map.of(
            "success", true,
            "data", discovered,
            "subnet", subnet,
            "port", port,
            "scannedHosts", hostLimit,
            "timeoutMs", timeoutMs,
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
    private static boolean checkPrinterPort(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 当前认证用户是否管理员（取不到用户时按非管理员处理：开钱箱必须 fail-closed）。 */
    private static boolean isAdmin(Context ctx) {
        User user = ctx.attribute("currentUser");
        return user != null && user.role != null
            && "admin".equalsIgnoreCase(user.role.trim());
    }

    private static String currentUsername(Context ctx) {
        User user = ctx.attribute("currentUser");
        return user == null ? "unknown" : user.username;
    }

    /** 设备类型显示名（TD-041：JSON 里 deviceType 是稳定代码，deviceTypeName 是按请求语言的名字）。 */
    private static String deviceTypeName(Context ctx, PrinterDeviceType type) {
        return com.cashier.api.ApiMessages.enumName(ctx,
            "api.printer.device_type." + type.name(), type.getDisplayName());
    }

    private static String deviceStatusName(Context ctx, PrinterDeviceStatus status) {
        return com.cashier.api.ApiMessages.enumName(ctx,
            "api.printer.device_status." + status.name(), status.getDisplayName());
    }

    private static String taskTypeName(Context ctx, PrintTaskType type) {
        return com.cashier.api.ApiMessages.enumName(ctx,
            "api.print_task.type." + type.name(), type.getDisplayName());
    }

    private static String taskStatusName(Context ctx, PrintTaskStatus status) {
        return com.cashier.api.ApiMessages.enumName(ctx,
            "api.print_task.status." + status.name(), status.getDisplayName());
    }

    private static String getString(Map<?, ?> body, String key, String defaultValue) {
        Object value = body.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static int getInt(Map<?, ?> body, String key, int defaultValue) {
        Object value = body.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return defaultValue;
    }

    private static boolean getBoolean(Map<?, ?> body, String key, boolean defaultValue) {
        Object value = body.get(key);
        return value != null ? Boolean.parseBoolean(value.toString()) : defaultValue;
    }
}
