package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.api.LoginRateLimiter;
import com.cashier.dao.DAOFactory;
import com.cashier.model.User;
import com.cashier.util.PasswordUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.Map;

/**
 * 认证接口
 */
public class AuthController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(AuthController.class);

    // 桌面端持久化锁定（login_attempts 表）阈值与时长仅由桌面端使用。
    // API 侧失败限流使用内存版 LoginRateLimiter（按 IP，不写库），
    // 防止未认证攻击者通过公开登录口锁死真实账号或撑大 login_attempts 表。
    private static final String DUMMY_PASSWORD_HASH = PasswordUtil.hashPassword("login-timing-equalizer-dummy");

    /**
     * 登录
     * POST /api/auth/login
     */
    public static void login(Context ctx) {
        try {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "请求体不能为空"));
                return;
            }
            
            if (request.username == null || request.password == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "用户名和密码不能为空"));
                return;
            }

            String clientIp = ctx.ip();

            // 内存按 IP 限流：防止同一来源（脚本/单机）暴力尝试。
            // 不写库 → 攻击者无法借 API 锁死真实账号（与桌面端解耦）。
            if (LoginRateLimiter.getInstance().isBlocked(clientIp)) {
                long remainingSeconds = LoginRateLimiter.getInstance().remainingLockMillis(clientIp) / 1000;
                ctx.status(HttpStatus.TOO_MANY_REQUESTS)
                   .json(Map.of("success", false, "message",
                       "尝试次数过多，请 " + Math.max(1, remainingSeconds) + " 秒后重试"));
                return;
            }

            // 用户名规范化
            request.username = request.username.trim();

            // 桌面端持久化的真实锁定（如管理员本地重置后遗留）仍然生效
            if (DAOFactory.getInstance().getLoginAttemptDAO().isLocked(request.username)) {
                long remainingSeconds = DAOFactory.getInstance().getLoginAttemptDAO().getRemainingLockoutSeconds(request.username);
                ctx.status(HttpStatus.TOO_MANY_REQUESTS)
                   .json(Map.of("success", false, "message", "账户已锁定，请 " + remainingSeconds + " 秒后重试"));
                return;
            }
            
            User user = DAOFactory.getInstance().getUserDAO().findByUsername(request.username);

            if (user == null) {
                // 用户名不存在也执行一次 BCrypt 校验，抹平耗时差异，防止计时枚举有效账号；
                // 失败计数只在内存按 IP 记录，不落库。
                PasswordUtil.verifyPassword(request.password, DUMMY_PASSWORD_HASH);
                LoginRateLimiter.getInstance().recordFailure(clientIp);
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "用户名或密码错误"));
                return;
            }

            if (!PasswordUtil.verifyPassword(request.password, user.password)) {
                LoginRateLimiter.getInstance().recordFailure(clientIp);
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "用户名或密码错误"));
                return;
            }

            if (!user.active) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "用户已被禁用"));
                return;
            }

            // 登录成功，重置内存限流；并清理该用户名遗留的持久化失败记录（桌面端路径可能残留）
            LoginRateLimiter.getInstance().reset(clientIp);
            DAOFactory.getInstance().getLoginAttemptDAO().resetAttempts(request.username);
            
            // 生成 Token
            String token = ApiServer.getInstance().generateToken(user);
            
            // 更新最后登录时间
            DAOFactory.getInstance().getUserDAO().updateLastLoginTime(user.id);
            
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
               .json(Map.of("success", false, "message", "登录失败"));
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
            
            // 吊销旧 Token，避免刷新后旧 token 仍有效（泄露的旧 token 失效）
            ApiServer.getInstance().invalidateToken(token);

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
