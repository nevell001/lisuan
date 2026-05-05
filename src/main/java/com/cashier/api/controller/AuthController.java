package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.api.middleware.AuthMiddleware;
import com.cashier.dao.UserDAO;
import com.cashier.model.User;
import com.cashier.util.PasswordUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;

/**
 * 认证控制器
 * 处理登录、Token 刷新、注销等
 */
public class AuthController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(AuthController.class);
    
    /**
     * 登录
     */
    public static void login(Context ctx) {
        LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
        
        if (request.username == null || request.username.isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ApiServer.ApiError(400, "用户名不能为空"));
            return;
        }
        
        if (request.password == null || request.password.isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ApiServer.ApiError(400, "密码不能为空"));
            return;
        }
        
        try {
            User user = UserDAO.findByUsername(request.username);
            
            if (user == null) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(new ApiServer.ApiError(401, "用户不存在"));
                return;
            }
            
            if (!user.active) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(new ApiServer.ApiError(401, "用户已禁用"));
                return;
            }
            
            // 验证密码
            if (!PasswordUtil.verifyPassword(request.password, user.password, user.username)) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(new ApiServer.ApiError(401, "密码错误"));
                return;
            }
            
            // 创建会话
            String token = AuthMiddleware.createSession(user);
            
            // 更新登录时间
            UserDAO.updateLastLoginTime(user.id);
            
            LoginResponse response = new LoginResponse();
            response.token = token;
            response.user = toUserInfo(user);
            response.expiresIn = 24 * 60 * 60; // 24小时
            
            logger.info("API 登录成功: {}", user.username);
            ctx.json(new ApiServer.ApiSuccess<>("登录成功", response));
            
        } catch (SQLException e) {
            logger.error("登录失败: {}", e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ApiServer.ApiError(500, "登录失败: " + e.getMessage()));
        }
    }
    
    /**
     * 刷新 Token
     */
    public static void refreshToken(Context ctx) {
        String oldToken = ctx.header("Authorization");
        if (oldToken != null && oldToken.startsWith("Bearer ")) {
            oldToken = oldToken.substring(7);
        }
        
        AuthMiddleware.SessionInfo session = AuthMiddleware.getSession(oldToken);
        if (session == null || session.isExpired()) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(new ApiServer.ApiError(401, "Token 无效或已过期"));
            return;
        }
        
        // 创建新 Token
        String newToken = AuthMiddleware.createSession(session.user);
        AuthMiddleware.destroySession(oldToken);
        
        LoginResponse response = new LoginResponse();
        response.token = newToken;
        response.user = toUserInfo(session.user);
        response.expiresIn = 24 * 60 * 60;
        
        ctx.json(new ApiServer.ApiSuccess<>("Token 已刷新", response));
    }
    
    /**
     * 注销
     */
    public static void logout(Context ctx) {
        String token = ctx.attribute("token");
        if (token != null) {
            AuthMiddleware.destroySession(token);
        }
        ctx.json(new ApiServer.ApiSuccess<>("已注销", null));
    }
    
    /**
     * 获取当前用户信息
     */
    public static void getCurrentUser(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(new ApiServer.ApiError(401, "未认证"));
            return;
        }
        
        ctx.json(new ApiServer.ApiSuccess<>(toUserInfo(user)));
    }
    
    /**
     * 转换为用户信息（去除敏感字段）
     */
    private static UserInfo toUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.id = user.id;
        info.username = user.username;
        info.name = user.name;
        info.role = user.role;
        info.active = user.active;
        return info;
    }
    
    // DTO 类
    public static class LoginRequest {
        public String username;
        public String password;
    }
    
    public static class LoginResponse {
        public String token;
        public UserInfo user;
        public int expiresIn;
    }
    
    public static class UserInfo {
        public int id;
        public String username;
        public String name;
        public String role;
        public boolean active;
    }
}