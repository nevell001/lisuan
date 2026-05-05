package com.cashier.api;

import com.cashier.api.controller.*;
import com.cashier.api.middleware.*;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;

/**
 * REST API 服务器
 * 提供多终端访问、移动支付集成、远程管理等能力
 */
public class ApiServer {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ApiServer.class);
    
    private static ApiServer instance;
    private Javalin app;
    private int port = 8080;
    private boolean running = false;
    
    // API 版本
    public static final String API_VERSION = "v1";
    public static final String API_PREFIX = "/api/" + API_VERSION;
    
    private ApiServer() {}
    
    public static synchronized ApiServer getInstance() {
        if (instance == null) {
            instance = new ApiServer();
        }
        return instance;
    }
    
    /**
     * 启动 API 服务器
     * @param port 端口号
     */
    public void start(int port) {
        if (running) {
            logger.warn("API 服务器已在运行中");
            return;
        }
        
        this.port = port;
        
        // 配置 JSON 序列化
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        // 创建 Javalin 应用
        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper));
            config.showJavalinBanner = false;
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowAllOrigins = true; // 生产环境应限制
                    it.allowAllHeaders = true;
                    it.allowAllMethods = true;
                });
            });
        });
        
        // 注册中间件
        registerMiddleware();
        
        // 注册路由
        registerRoutes();
        
        // 启动服务器
        app.start(port);
        running = true;
        
        logger.info("API 服务器启动成功，端口: {}", port);
        logger.info("API 地址: http://localhost:{}{}", port, API_PREFIX);
    }
    
    /**
     * 停止 API 服务器
     */
    public void stop() {
        if (!running) {
            logger.warn("API 服务器未运行");
            return;
        }
        
        app.stop();
        running = false;
        logger.info("API 服务器已停止");
    }
    
    /**
     * 注册中间件
     */
    private void registerMiddleware() {
        // 全局异常处理
        app.exception(Exception.class, (e, ctx) -> {
            logger.error("API 异常: {}", e.getMessage(), e);
            ctx.status(500).json(new ApiError(500, "服务器内部错误: " + e.getMessage()));
        });
        
        // 请求日志
        app.before(ctx -> {
            logger.debug("API 请求: {} {}", ctx.method(), ctx.path());
        });
        
        // 认证中间件（对需要认证的路径）
        app.before(API_PREFIX + "/auth/*", ctx -> {
            // 登录接口不需要认证
            if (ctx.path().endsWith("/login") || ctx.path().endsWith("/token")) {
                return;
            }
            AuthMiddleware.handle(ctx);
        });
        
        app.before(API_PREFIX + "/products/*", AuthMiddleware.handle);
        app.before(API_PREFIX + "/members/*", AuthMiddleware.handle);
        app.before(API_PREFIX + "/transactions/*", AuthMiddleware.handle);
        app.before(API_PREFIX + "/inventory/*", AuthMiddleware.handle);
        app.before(API_PREFIX + "/reports/*", AuthMiddleware.handle);
        app.before(API_PREFIX + "/settings/*", AuthMiddleware.handle);
        app.before(API_PREFIX + "/users/*", AuthMiddleware.handle);
    }
    
    /**
     * 注册路由
     */
    private void registerRoutes() {
        // 健康检查（无需认证）
        app.get("/health", HealthController.health);
        app.get(API_PREFIX + "/health", HealthController.health);
        
        // 认证接口
        app.post(API_PREFIX + "/auth/login", AuthController.login);
        app.post(API_PREFIX + "/auth/token", AuthController.refreshToken);
        app.post(API_PREFIX + "/auth/logout", AuthController.logout);
        app.get(API_PREFIX + "/auth/me", AuthController.getCurrentUser);
        
        // 商品接口
        app.get(API_PREFIX + "/products", ProductApiController.list);
        app.get(API_PREFIX + "/products/{id}", ProductApiController.get);
        app.post(API_PREFIX + "/products", ProductApiController.create);
        app.put(API_PREFIX + "/products/{id}", ProductApiController.update);
        app.delete(API_PREFIX + "/products/{id}", ProductApiController.delete);
        app.get(API_PREFIX + "/products/search", ProductApiController.search);
        
        // 会员接口
        app.get(API_PREFIX + "/members", MemberApiController.list);
        app.get(API_PREFIX + "/members/{id}", MemberApiController.get);
        app.get(API_PREFIX + "/members/phone/{phone}", MemberApiController.getByPhone);
        app.post(API_PREFIX + "/members", MemberApiController.create);
        app.put(API_PREFIX + "/members/{id}", MemberApiController.update);
        app.post(API_PREFIX + "/members/{id}/recharge", MemberApiController.recharge);
        app.get(API_PREFIX + "/members/search", MemberApiController.search);
        
        // 交易接口
        app.get(API_PREFIX + "/transactions", TransactionApiController.list);
        app.get(API_PREFIX + "/transactions/{id}", TransactionApiController.get);
        app.post(API_PREFIX + "/transactions", TransactionApiController.create);
        app.get(API_PREFIX + "/transactions/today", TransactionApiController.today);
        app.get(API_PREFIX + "/transactions/stats", TransactionApiController.stats);
        
        // 库存接口
        app.get(API_PREFIX + "/inventory", InventoryApiController.list);
        app.get(API_PREFIX + "/inventory/alerts", InventoryApiController.alerts);
        app.post(API_PREFIX + "/inventory/update", InventoryApiController.updateStock);
        app.get(API_PREFIX + "/inventory/check", InventoryApiController.check);
        
        // 报表接口
        app.get(API_PREFIX + "/reports/daily", ReportApiController.daily);
        app.get(API_PREFIX + "/reports/monthly", ReportApiController.monthly);
        app.get(API_PREFIX + "/reports/sales", ReportApiController.sales);
        app.get(API_PREFIX + "/reports/products", ReportApiController.productRanking);
        
        // 系统设置接口
        app.get(API_PREFIX + "/settings", SettingsApiController.list);
        app.put(API_PREFIX + "/settings", SettingsApiController.update);
        
        // 用户管理接口（管理员）
        app.get(API_PREFIX + "/users", UserApiController.list);
        app.get(API_PREFIX + "/users/{id}", UserApiController.get);
        app.post(API_PREFIX + "/users", UserApiController.create);
        app.put(API_PREFIX + "/users/{id}", UserApiController.update);
        app.delete(API_PREFIX + "/users/{id}", UserApiController.delete);
        
        // WebSocket 实时同步
        app.ws(API_PREFIX + "/ws/sync", ws -> {
            ws.onConnect(ctx -> {
                logger.info("WebSocket 连接: {}", ctx.getSessionId());
                SyncWebSocket.onConnect(ctx);
            });
            ws.onClose(ctx -> {
                logger.info("WebSocket 断开: {}", ctx.getSessionId());
                SyncWebSocket.onClose(ctx);
            });
            ws.onMessage(ctx -> {
                SyncWebSocket.onMessage(ctx);
            });
        });
        
        logger.info("已注册 {} 个 API 路由", app.router().getEndpointHandlerPaths().size());
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
    }
    
    /**
     * API 错误响应
     */
    public static class ApiError {
        public int code;
        public String message;
        
        public ApiError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
    
    /**
     * API 成功响应
     */
    public static class ApiSuccess<T> {
        public int code = 200;
        public String message = "success";
        public T data;
        
        public ApiSuccess(T data) {
            this.data = data;
        }
        
        public ApiSuccess(String message, T data) {
            this.message = message;
            this.data = data;
        }
    }
    
    /**
     * 分页响应
     */
    public static class PageResult<T> {
        public int code = 200;
        public T data;
        public int total;
        public int page;
        public int pageSize;
        
        public PageResult(T data, int total, int page, int pageSize) {
            this.data = data;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
    }
}