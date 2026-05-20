package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.dao.UserDAO;
import com.cashier.model.User;
import com.cashier.util.PasswordUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 认证接口
 */
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    /**
     * 登录
     * POST /api/auth/login
     */
    public static void login(Context ctx) {
        try {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            
            if (request.username == null || request.password == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "用户名和密码不能为空"));
                return;
            }
            
            User user = UserDAO.findByUsername(request.username);

            if (user == null || !PasswordUtil.verifyPassword(request.password, user.password)) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "用户名或密码错误"));
                return;
            }
            
            if (!user.active) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "用户已被禁用"));
                return;
            }
            
            // 生成 Token
            String token = ApiServer.getInstance().generateToken(user);
            
            // 更新最后登录时间
            UserDAO.updateLastLoginTime(user.id);
            
            user.password = null;
            
            logger.info("用户登录: {}", user.username);
            ctx.json(Map.of(
                "success", true,
                "token", token,
                "user", user,
                "message", "登录成功"
            ));
        } catch (Exception e) {
            logger.error("登录失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "登录失败: " + e.getMessage()));
        }
    }
    
    /**
     * 刷新 Token
     * POST /api/auth/refresh
     */
    public static void refresh(Context ctx) {
        String token = ctx.header("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(Map.of("success", false, "message", "缺少认证 Token"));
            return;
        }
        
        token = token.substring(7);
        
        try {
            User user = ApiServer.getInstance().validateToken(token);
            if (user == null) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "Token 无效或已过期"));
                return;
            }
            
            String newToken = ApiServer.getInstance().generateToken(user);
            ctx.json(Map.of("success", true, "token", newToken));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "Token 刷新失败"));
        }
    }
    
    /**
     * 注销
     * POST /api/auth/logout
     */
    public static void logout(Context ctx) {
        String token = ctx.header("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            ApiServer.getInstance().invalidateToken(token);
        }
        ctx.json(Map.of("success", true, "message", "已注销"));
    }
    
    /**
     * 获取当前用户信息
     * GET /api/auth/me
     */
    public static void getCurrentUser(Context ctx) {
        User user = ctx.attribute("currentUser");
        if (user == null) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(Map.of("success", false, "message", "未登录"));
            return;
        }
        
        user.password = null;
        ctx.json(Map.of("success", true, "user", user));
    }
    
    /**
     * 登录请求
     */
    public static class LoginRequest {
        public String username;
        public String password;
    }
}