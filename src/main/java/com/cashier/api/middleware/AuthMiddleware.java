package com.cashier.api.middleware;

import com.cashier.api.ApiServer;
import com.cashier.model.User;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Token 认证中间件
 */
public class AuthMiddleware {
    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);
    
    /**
     * 验证 Token 并设置用户属性
     */
    public static void authenticate(Context ctx) {
        String authHeader = ctx.header("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(Map.of("success", false, "message", "缺少认证 Token"));
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            User user = ApiServer.getInstance().validateToken(token);
            if (user == null) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "Token 无效或已过期"));
                return;
            }
            
            ctx.attribute("currentUser", user);
        } catch (Exception e) {
            logger.error("Token 验证失败", e);
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(Map.of("success", false, "message", "Token 验证失败"));
        }
    }
}