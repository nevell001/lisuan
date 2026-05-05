package com.cashier.api.middleware;

import com.cashier.api.ApiServer;
import com.cashier.dao.UserDAO;
import com.cashier.model.User;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 认证中间件
 * 支持 Token 认证和会话管理
 */
public class AuthMiddleware {
    private static final Logger logger = LoggerFactoryUtil.getLogger(AuthMiddleware.class);
    
    // Token 存储（生产环境应使用 Redis）
    private static final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    
    // Token 过期时间（24小时）
    private static final long TOKEN_EXPIRE_MS = 24 * 60 * 60 * 1000;
    
    /**
     * 认证处理
     */
    public static void handle(Context ctx) {
        String token = extractToken(ctx);
        
        if (token == null || token.isEmpty()) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(new ApiServer.ApiError(401, "未提供认证 Token"));
            return;
        }
        
        SessionInfo session = sessions.get(token);
        
        if (session == null) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(new ApiServer.ApiError(401, "无效的 Token"));
            return;
        }
        
        if (session.isExpired()) {
            sessions.remove(token);
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(new ApiServer.ApiError(401, "Token 已过期"));
            return;
        }
        
        // 更新最后活动时间
        session.updateActivity();
        
        // 将用户信息存入上下文
        ctx.attribute("user", session.user);
        ctx.attribute("token", token);
    }
    
    /**
     * 从请求中提取 Token
     */
    private static String extractToken(Context ctx) {
        // 优先从 Header 获取
        String header = ctx.header("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        
        // 从查询参数获取
        return ctx.queryParam("token");
    }
    
    /**
     * 创建会话
     */
    public static String createSession(User user) {
        String token = generateToken();
        SessionInfo session = new SessionInfo(user, token);
        sessions.put(token, session);
        logger.info("创建会话: 用户={}, Token={}", user.username, token);
        return token;
    }
    
    /**
     * 销毁会话
     */
    public static void destroySession(String token) {
        SessionInfo session = sessions.remove(token);
        if (session != null) {
            logger.info("销毁会话: 用户={}", session.user.username);
        }
    }
    
    /**
     * 获取会话
     */
    public static SessionInfo getSession(String token) {
        return sessions.get(token);
    }
    
    /**
     * 生成 Token
     */
    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + 
               Long.toHexString(System.currentTimeMillis());
    }
    
    /**
     * 验证权限
     */
    public static boolean hasRole(Context ctx, String... roles) {
        User user = ctx.attribute("user");
        if (user == null) return false;
        
        for (String role : roles) {
            if (user.role.equals(role)) return true;
        }
        return false;
    }
    
    /**
     * 管理员权限检查
     */
    public static void requireAdmin(Context ctx) {
        if (!hasRole(ctx, "admin")) {
            ctx.status(HttpStatus.FORBIDDEN)
               .json(new ApiServer.ApiError(403, "需要管理员权限"));
        }
    }
    
    /**
     * 会话信息
     */
    public static class SessionInfo {
        public User user;
        public String token;
        public long createTime;
        public long lastActivity;
        
        public SessionInfo(User user, String token) {
            this.user = user;
            this.token = token;
            this.createTime = System.currentTimeMillis();
            this.lastActivity = this.createTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastActivity > TOKEN_EXPIRE_MS;
        }
        
        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
    }
}