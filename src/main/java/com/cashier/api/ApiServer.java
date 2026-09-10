package com.cashier.api;

import com.cashier.api.controller.*;
import com.cashier.api.middleware.AuthMiddleware;
import com.cashier.api.middleware.AuthorizationMiddleware;
import com.cashier.api.sync.SyncWebSocketHandler;
import com.cashier.api.sync.SyncManager;
import com.cashier.dao.DAOFactory;
import com.cashier.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST API 服务器
 * 基于 Javalin 框架
 */
public class ApiServer {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ApiServer.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    private static final ApiServer INSTANCE = new ApiServer();
    private Javalin app;
    private int port = 8080;
    private boolean running = false;
    
    // Token 存储
    private final ConcurrentHashMap<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    // 速率限制：每个IP每分钟最多60次请求
    private final ConcurrentHashMap<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();
    private static final int RATE_LIMIT_PER_MINUTE = 60;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000;
    // 防止无界增长：超过该数量时清理早已过期的条目（来源 IP 的一次性请求不再长期占用内存）
    private static final int MAX_RATE_LIMIT_TRACKED_IPS = 10_000;
    
    private ApiServer() {}
    
    public static ApiServer getInstance() {
        return INSTANCE;
    }
    
    /**
     * 启动 API 服务器
     */
    public void start(int port) {
        if (running) {
            logger.warn("API 服务器已在运行");
            return;
        }

        if (!ApiConfig.isProductionReady()) {
            throw new IllegalStateException(
                "API 安全配置不完整：请设置至少 32 字符的 TOKEN_SECRET，并限制 CORS_ALLOWED_ORIGINS"
            );
        }
        
        this.port = port;
        
        // 创建 Javalin 应用
        app = Javalin.create(config -> {
            // JSON 配置
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            config.jsonMapper(new JavalinJackson(mapper, false));
            
            // CORS 配置 - 从配置读取允许的来源
            String corsOrigins = ApiConfig.getCorsOrigins();
            logger.info("CORS 配置: 允许来源 = {}", corsOrigins);
            config.bundledPlugins.enableCors(cors -> {
                if ("*".equals(corsOrigins)) {
                    // 开发环境：允许所有来源
                    cors.addRule(rule -> {
                        rule.allowHost("/*");
                    });
                    logger.warn("⚠️ CORS 配置为允许所有来源 - 生产环境请设置 CORS_ALLOWED_ORIGINS 环境变量");
                } else {
                    // 生产环境：仅允许指定来源
                    String[] origins = corsOrigins.split(",");
                    for (String origin : origins) {
                        origin = origin.trim();
                        if (!origin.isEmpty()) {
                            final String allowedOrigin = origin;
                            cors.addRule(rule -> {
                                rule.allowHost(allowedOrigin);
                            });
                        }
                    }
                    logger.info("✓ CORS 已限制为以下来源: {}", corsOrigins);
                }
            });
            
            // 启用请求日志
            config.requestLogger.http((ctx, ms) -> {
                logger.debug("{} {} - {}ms", ctx.method(), ctx.path(), ms);
            });
        });

        // 安全响应头
        app.before(ctx -> {
            ctx.header("X-Content-Type-Options", "nosniff");
            ctx.header("X-Frame-Options", "DENY");
            ctx.header("X-XSS-Protection", "1; mode=block");
            ctx.header("Referrer-Policy", "strict-origin-when-cross-origin");
        });

        // 速率限制
        app.before(this::checkRateLimit);

        // API 认证，健康检查和登录接口除外
        app.before(ctx -> {
            if (!isPublicApiPath(ctx.path())) {
                AuthMiddleware.authenticate(ctx);
                if (ctx.attribute("currentUser") != null) {
                    AuthorizationMiddleware.authorize(ctx);
                }
            }
        });

        // 注册路由
        registerRoutes();

        // 异常详情只写入服务端日志，所有 5xx 响应统一隐藏内部实现信息。
        app.after(ctx -> {
            if (ctx.statusCode() >= 500) {
                ctx.json(Map.of("success", false, "message", "服务器内部错误"));
            }
        });
        
        // 启动服务器
        app.start(ApiConfig.getHost(), port);
        running = true;
        
        logger.info("REST API 服务器已启动，地址: {}:{}", ApiConfig.getHost(), port);
        logger.info("API 健康检查: http://{}:{}{}", ApiConfig.getHost(), port, "/api/health");
    }
    
    /**
     * 注册所有 API 路由
     */
    private void registerRoutes() {
        registerHealthAndAuthRoutes();
        registerProductRoutes();
        registerMemberRoutes();
        registerTransactionRoutes();
        registerInventoryRoutes();
        registerReportRoutes();
        registerSettingsRoutes();
        registerInvoiceRoutes();
        registerUserRoutes();
        registerPrinterRoutes();
        registerPaymentRoutes();
        registerBackupRoutes();
        registerI18nRoutes();
        registerSyncAndErrorRoutes();
    }

    private void registerHealthAndAuthRoutes() {
        app.get("/api/health", HealthController::check);
        app.get("/api/health/detail", HealthController::detail);
        app.post("/api/auth/login", AuthController::login);
        app.post("/api/auth/refresh", AuthController::refresh);
        app.post("/api/auth/logout", AuthController::logout);
        app.get("/api/auth/me", AuthController::getCurrentUser);
    }

    private void registerProductRoutes() {
        app.get("/api/products", ProductApiController::list);
        app.get("/api/products/{id}", ProductApiController::get);
        app.post("/api/products", ProductApiController::create);
        app.put("/api/products/{id}", ProductApiController::update);
        app.delete("/api/products/{id}", ProductApiController::delete);
        app.get("/api/products/low-stock", ProductApiController::lowStock);
    }

    private void registerMemberRoutes() {
        app.get("/api/members", MemberApiController::list);
        app.get("/api/members/{id}", MemberApiController::get);
        app.get("/api/members/phone/{phone}", MemberApiController::getByPhone);
        app.post("/api/members", MemberApiController::create);
        app.put("/api/members/{id}", MemberApiController::update);
        app.post("/api/members/{id}/recharge", MemberApiController::recharge);
    }

    private void registerTransactionRoutes() {
        app.get("/api/transactions", TransactionApiController::list);
        app.get("/api/transactions/{id}", TransactionApiController::get);
        app.post("/api/transactions", TransactionApiController::create);
        app.post("/api/transactions/{id}/refund", TransactionApiController::refund);
        app.get("/api/transactions/today", TransactionApiController::todayStats);
    }

    private void registerInventoryRoutes() {
        app.get("/api/inventory", InventoryApiController::list);
        app.get("/api/inventory/alerts", InventoryApiController::alerts);
        app.put("/api/inventory/{id}", InventoryApiController::updateStock);
        app.post("/api/inventory/check", InventoryApiController::check);
    }

    private void registerReportRoutes() {
        app.get("/api/reports/daily", ReportApiController::dailySales);
        app.get("/api/reports/monthly", ReportApiController::monthlySales);
        app.get("/api/reports/top-products", ReportApiController::topProducts);
        app.get("/api/reports/payment-methods", ReportApiController::paymentMethods);
    }

    private void registerSettingsRoutes() {
        app.get("/api/settings", SettingsApiController::list);
        app.get("/api/settings/{key}", SettingsApiController::get);
        app.put("/api/settings/{key}", SettingsApiController::set);
        app.delete("/api/settings/{key}", SettingsApiController::delete);
    }

    private void registerInvoiceRoutes() {
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
    }

    private void registerUserRoutes() {
        app.get("/api/users", UserApiController::list);
        app.get("/api/users/{id}", UserApiController::get);
        app.post("/api/users", UserApiController::create);
        app.put("/api/users/{id}", UserApiController::update);
        app.delete("/api/users/{id}", UserApiController::delete);
    }

    private void registerPrinterRoutes() {
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
    }

    private void registerPaymentRoutes() {
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
    }

    private void registerBackupRoutes() {
        app.post("/api/backup/execute", BackupApiController::executeBackup);
        app.post("/api/backup/cleanup", BackupApiController::cleanupBackups);
        app.get("/api/backup/list", BackupApiController::listBackups);
        app.get("/api/backup/stats", BackupApiController::getStats);
        app.get("/api/backup/config", BackupApiController::getConfig);
        app.put("/api/backup/config", BackupApiController::updateConfig);
        app.get("/api/backup/{backupId}", BackupApiController::getBackup);
        app.post("/api/backup/{backupId}/restore", BackupApiController::restoreBackup);
        app.get("/api/backup/{backupId}/download", BackupApiController::downloadBackup);
    }

    private void registerI18nRoutes() {
        app.get("/api/i18n/locale", I18nApiController::getCurrentLocale);
        app.put("/api/i18n/locale", I18nApiController::setLocale);
        app.get("/api/i18n/locales", I18nApiController::getAvailableLocales);
        app.get("/api/i18n/messages", I18nApiController::getMessage);
        app.get("/api/i18n/messages/all", I18nApiController::getAllMessages);
        app.get("/api/i18n/messages/locale/{locale}", I18nApiController::getMessagesForLocale);
    }

    private void registerSyncAndErrorRoutes() {
        app.ws("/ws/sync", ws -> {
            ws.onConnect(SyncWebSocketHandler::onConnect);
            ws.onClose(SyncWebSocketHandler::onClose);
            ws.onMessage(SyncWebSocketHandler::onMessage);
            ws.onError(SyncWebSocketHandler::onError);
        });

        app.get("/api/sync/status", ctx -> {
            ctx.json(Map.of(
                "success", true,
                "onlineTerminals", SyncManager.getInstance().getOnlineCount(),
                "terminals", SyncManager.getInstance().getOnlineTerminals()
            ));
        });

        app.exception(Exception.class, (e, ctx) -> {
            logger.error("API 异常: {} - {}", ctx.path(), e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "服务器内部错误"));
        });

        app.error(HttpStatus.NOT_FOUND.getCode(), ctx -> {
            ctx.json(Map.of("success", false, "message", "接口不存在: " + ctx.path()));
        });
    }

    static boolean isPublicApiPath(String path) {
        return path.equals("/api/health")
            || path.equals("/api/auth/login")
            // 支付回调由微信/支付宝服务器直接调用，无法携带本系统 Token；
            // 安全性由渠道验签（RSA/解密）保证，必须公开
            || path.startsWith("/api/payment/notify/");
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
        purgeExpiredTokens();
        String token = createSecureToken();
        tokens.put(token, new TokenInfo(user.id, System.currentTimeMillis() + getTokenExpireMs()));
        return token;
    }

    /**
     * 清理已过期的 Token。
     *
     * <p>此前只有被再次使用到的过期 token 才会被移除，反复登录会让过期条目常驻内存（每个约 200 字节）。
     * 在签发新 token 时顺带清理，成本与登录频率同阶。</p>
     *
     * @return 本次清理的条目数
     */
    int purgeExpiredTokens() {
        long now = System.currentTimeMillis();
        int before = tokens.size();
        tokens.entrySet().removeIf(entry -> entry.getValue().expireTime < now);
        return before - tokens.size();
    }

    private String createSecureToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String tokenId = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return tokenId + "." + signTokenId(tokenId);
    }

    private String signTokenId(String tokenId) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                ApiConfig.getTokenSecret().getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );
            mac.init(keySpec);
            byte[] signature = mac.doFinal(tokenId.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException("生成 Token 签名失败", e);
        }
    }

    private long getTokenExpireMs() {
        return ApiConfig.getTokenExpireHours() * 60L * 60L * 1000L;
    }
    
    /**
     * 验证 Token
     */
    public User validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        TokenInfo info = tokens.get(token);
        if (info == null || info.expireTime < System.currentTimeMillis()) {
            tokens.remove(token);
            return null;
        }
        
        try {
            User user = DAOFactory.getInstance().getUserDAO().findById(info.userId);
            if (user == null || !user.active) {
                // 用户已被删除或禁用：立即作废其 token，避免旧 token 继续生效或通过 refresh 续期
                tokens.remove(token);
                return null;
            }
            return user;
        } catch (Exception e) {
            logger.error("获取用户失败: {}", info.userId, e);
            return null;
        }
    }
    
    /**
     * 注销指定用户的全部 Token
     * 在禁用账号、重置密码或删除用户后调用，使该用户已签发（可能泄露）的 token 立即失效
     * @param userId 用户 ID
     */
    public void invalidateUserTokens(int userId) {
        tokens.entrySet().removeIf(entry -> entry.getValue().userId == userId);
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

    /**
     * 速率限制条目
     */
    private static class RateLimitEntry {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart;

        RateLimitEntry() {
            this.windowStart = System.currentTimeMillis();
        }
    }

    /**
     * 检查速率限制
     */
    private void checkRateLimit(Context ctx) {
        pruneRateLimitEntriesIfNeeded();

        String clientIp = ctx.ip();
        long now = System.currentTimeMillis();

        RateLimitEntry entry = rateLimitMap.computeIfAbsent(clientIp, k -> new RateLimitEntry());

        synchronized (entry) {
            if (now - entry.windowStart > RATE_LIMIT_WINDOW_MS) {
                entry.windowStart = now;
                entry.count.set(0);
            }

            int currentCount = entry.count.incrementAndGet();
            if (currentCount > RATE_LIMIT_PER_MINUTE) {
                ctx.status(HttpStatus.TOO_MANY_REQUESTS)
                   .json(Map.of("success", false, "message", "请求过于频繁，请稍后再试"));
                ctx.skipRemainingHandlers();
                return;
            }

            ctx.header("X-RateLimit-Limit", String.valueOf(RATE_LIMIT_PER_MINUTE));
            ctx.header("X-RateLimit-Remaining", String.valueOf(Math.max(0, RATE_LIMIT_PER_MINUTE - currentCount)));
        }
    }

    /**
     * 清理速率限制表中的过期条目，防止不同来源 IP 无限堆积导致内存无界增长。
     * 仅当条目数量超过上限时执行，开销摊薄到后续请求。
     */
    private void pruneRateLimitEntriesIfNeeded() {
        if (rateLimitMap.size() < MAX_RATE_LIMIT_TRACKED_IPS) {
            return;
        }
        long cutoff = System.currentTimeMillis() - RATE_LIMIT_WINDOW_MS * 2;
        rateLimitMap.entrySet().removeIf(entry -> entry.getValue().windowStart < cutoff);
    }
}
