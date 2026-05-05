package com.cashier.api;

import com.cashier.api.controller.*;
import com.cashier.api.middleware.AuthMiddleware;
import com.cashier.api.sync.SyncWebSocketHandler;
import com.cashier.api.sync.SyncManager;
import com.cashier.dao.UserDAO;
import com.cashier.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API 服务器
 * 基于 Javalin 框架
 */
public class ApiServer {
    private static final Logger logger = LoggerFactory.getLogger(ApiServer.class);
    
    private static ApiServer instance;
    private Javalin app;
    private int port = 8080;
    private boolean running = false;
    
    // Token 存储
    private final ConcurrentHashMap<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRE_MS = 24 * 60 * 60 * 1000; // 24小时
    
    private ApiServer() {}
    
    public static ApiServer getInstance() {
        if (instance == null) {
            instance = new ApiServer();
        }
        return instance;
    }
    
    /**
     * 启动 API 服务器
     */
    public void start(int port) {
        if (running) {
            logger.warn("API 服务器已在运行");
            return;
        }
        
        this.port = port;
        
        // 创建 Javalin 应用
        app = Javalin.create(config -> {
            // JSON 配置
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            config.jsonMapper(new JavalinJackson(mapper, false));
            
            // CORS 配置
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.allowHost("/*");
                });
            });
            
            // 启用请求日志
            config.requestLogger.http((ctx, ms) -> {
                logger.debug("{} {} - {}ms", ctx.method(), ctx.path(), ms);
            });
        });
        
        // 注册路由
        registerRoutes();
        
        // 启动服务器
        app.start(port);
        running = true;
        
        logger.info("REST API 服务器已启动，端口: {}", port);
        logger.info("API 文档: http://localhost:{}{}", port, "/api/health");
    }
    
    /**
     * 注册所有 API 路由
     */
    private void registerRoutes() {
        // 健康检查（无需认证）
        app.get("/api/health", HealthController::check);
        app.get("/api/health/detail", HealthController::detail);
        
        // 认证接口（无需认证）
        app.post("/api/auth/login", AuthController::login);
        app.post("/api/auth/refresh", AuthController::refresh);
        app.post("/api/auth/logout", AuthController::logout);
        app.get("/api/auth/me", AuthController::getCurrentUser);
        
        // 商品管理（需要认证）
        app.get("/api/products", ProductApiController::list);
        app.get("/api/products/{id}", ProductApiController::get);
        app.post("/api/products", ProductApiController::create);
        app.put("/api/products/{id}", ProductApiController::update);
        app.delete("/api/products/{id}", ProductApiController::delete);
        app.get("/api/products/low-stock", ProductApiController::lowStock);
        
        // 会员管理
        app.get("/api/members", MemberApiController::list);
        app.get("/api/members/{id}", MemberApiController::get);
        app.get("/api/members/phone/{phone}", MemberApiController::getByPhone);
        app.post("/api/members", MemberApiController::create);
        app.put("/api/members/{id}", MemberApiController::update);
        app.post("/api/members/{id}/recharge", MemberApiController::recharge);
        
        // 交易管理
        app.get("/api/transactions", TransactionApiController::list);
        app.get("/api/transactions/{id}", TransactionApiController::get);
        app.post("/api/transactions", TransactionApiController::create);
        app.post("/api/transactions/{id}/refund", TransactionApiController::refund);
        app.get("/api/transactions/today", TransactionApiController::todayStats);
        
        // 库存管理
        app.get("/api/inventory", InventoryApiController::list);
        app.get("/api/inventory/alerts", InventoryApiController::alerts);
        app.put("/api/inventory/{id}", InventoryApiController::updateStock);
        app.post("/api/inventory/check", InventoryApiController::check);
        
        // 报表统计
        app.get("/api/reports/daily", ReportApiController::dailySales);
        app.get("/api/reports/monthly", ReportApiController::monthlySales);
        app.get("/api/reports/top-products", ReportApiController::topProducts);
        app.get("/api/reports/payment-methods", ReportApiController::paymentMethods);
        
        // 系统设置
        app.get("/api/settings", SettingsApiController::list);
        app.get("/api/settings/{key}", SettingsApiController::get);
        app.put("/api/settings/{key}", SettingsApiController::set);
        app.delete("/api/settings/{key}", SettingsApiController::delete);
        
        // 发票管理
        app.get("/api/invoices", InvoiceApiController::list);
        app.get("/api/invoices/stats", InvoiceApiController::stats);
        app.get("/api/invoices/{id}", InvoiceApiController::get);
        app.get("/api/invoices/transaction/{transactionId}", InvoiceApiController::getByTransaction);
        app.post("/api/invoices/from-transaction", InvoiceApiController::createFromTransaction);
        app.post("/api/invoices/manual", InvoiceApiController::createManual);
        app.post("/api/invoices/{id}/void", InvoiceApiController::voidInvoice);
        app.post("/api/invoices/{id}/print", InvoiceApiController::recordPrint);
        app.get("/api/invoices/seller-info", InvoiceApiController::getSellerInfo);
        app.put("/api/invoices/seller-info", InvoiceApiController::setSellerInfo);
        
        // 用户管理（管理员）
        app.get("/api/users", UserApiController::list);
        app.get("/api/users/{id}", UserApiController::get);
        app.post("/api/users", UserApiController::create);
        app.put("/api/users/{id}", UserApiController::update);
        app.delete("/api/users/{id}", UserApiController::delete);
        
        // 打印机管理
        app.get("/api/printers", PrintApiController::listPrinters);
        app.get("/api/printers/connected", PrintApiController::getConnectedPrinters);
        app.get("/api/printers/discover", PrintApiController::discoverPrinters);
        app.get("/api/printers/history", PrintApiController::getPrintHistory);
        app.get("/api/printers/{id}", PrintApiController::getPrinter);
        app.get("/api/printers/{id}/status", PrintApiController::checkPrinterStatus);
        app.post("/api/printers/add", PrintApiController::addPrinter);
        app.post("/api/printers/{id}/connect", PrintApiController::connectPrinter);
        app.post("/api/printers/{id}/disconnect", PrintApiController::disconnectPrinter);
        app.post("/api/printers/{id}/set-default", PrintApiController::setDefaultPrinter);
        app.post("/api/printers/{id}/test", PrintApiController::printTest);
        app.post("/api/printers/{id}/receipt", PrintApiController::printReceipt);
        app.post("/api/printers/{id}/invoice/{invoiceId}", PrintApiController::printInvoice);
        app.post("/api/printers/{id}/cashdrawer", PrintApiController::openCashDrawer);
        app.delete("/api/printers/{id}", PrintApiController::removePrinter);
        
        // 电子支付管理
        app.post("/api/payment/create", PaymentApiController::createPayment);
        app.get("/api/payment/{paymentId}/status", PaymentApiController::queryStatus);
        app.get("/api/payment/transaction/{transactionId}", PaymentApiController::getByTransaction);
        app.post("/api/payment/notify/{channel}", PaymentApiController::handleNotify);
        app.post("/api/payment/{paymentId}/refund", PaymentApiController::applyRefund);
        app.get("/api/payment/waiting", PaymentApiController::getWaitingOrders);
        app.post("/api/payment/close-expired", PaymentApiController::closeExpired);
        app.get("/api/payment/stats/daily", PaymentApiController::getDailyStats);
        app.get("/api/payment/config", PaymentApiController::getConfig);
        app.put("/api/payment/config", PaymentApiController::setConfig);
        
        // 云备份管理
        app.post("/api/backup/execute", BackupApiController::executeBackup);
        app.post("/api/backup/cleanup", BackupApiController::cleanupBackups);
        app.get("/api/backup/list", BackupApiController::listBackups);
        app.get("/api/backup/stats", BackupApiController::getStats);
        app.get("/api/backup/config", BackupApiController::getConfig);
        app.put("/api/backup/config", BackupApiController::updateConfig);
        app.get("/api/backup/{backupId}", BackupApiController::getBackup);
        app.post("/api/backup/{backupId}/restore", BackupApiController::restoreBackup);
        app.get("/api/backup/{backupId}/download", BackupApiController::downloadBackup);
        
        // WebSocket 同步端点
        app.ws("/ws/sync", ws -> {
            ws.onConnect(SyncWebSocketHandler::onConnect);
            ws.onClose(SyncWebSocketHandler::onClose);
            ws.onMessage(SyncWebSocketHandler::onMessage);
            ws.onError(SyncWebSocketHandler::onError);
        });
        
        // WebSocket 状态查询 API
        app.get("/api/sync/status", ctx -> {
            ctx.json(Map.of(
                "success", true,
                "onlineTerminals", SyncManager.getInstance().getOnlineCount(),
                "terminals", SyncManager.getInstance().getOnlineTerminals()
            ));
        });
        
        // 全局异常处理
        app.exception(Exception.class, (e, ctx) -> {
            logger.error("API 异常: {} - {}", ctx.path(), e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "服务器内部错误: " + e.getMessage()));
        });
        
        // 404 处理
        app.error(HttpStatus.NOT_FOUND.getCode(), ctx -> {
            ctx.json(Map.of("success", false, "message", "接口不存在: " + ctx.path()));
        });
    }
    
    /**
     * 停止 API 服务器
     */
    public void stop() {
        if (app != null) {
            app.stop();
            running = false;
            logger.info("REST API 服务器已停止");
        }
    }
    
    /**
     * 检查服务器状态
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * 获取端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 生成 Token
     */
    public String generateToken(User user) {
        String token = "TK" + System.currentTimeMillis() + "_" + user.id;
        tokens.put(token, new TokenInfo(user.id, System.currentTimeMillis() + TOKEN_EXPIRE_MS));
        return token;
    }
    
    /**
     * 验证 Token
     */
    public User validateToken(String token) {
        TokenInfo info = tokens.get(token);
        if (info == null || info.expireTime < System.currentTimeMillis()) {
            tokens.remove(token);
            return null;
        }
        
        try {
            return UserDAO.findById(info.userId);
        } catch (Exception e) {
            logger.error("获取用户失败: {}", info.userId, e);
            return null;
        }
    }
    
    /**
     * 注销 Token
     */
    public void invalidateToken(String token) {
        tokens.remove(token);
    }
    
    /**
     * Token 信息
     */
    private static class TokenInfo {
        int userId;
        long expireTime;
        
        TokenInfo(int userId, long expireTime) {
            this.userId = userId;
            this.expireTime = expireTime;
        }
    }
}