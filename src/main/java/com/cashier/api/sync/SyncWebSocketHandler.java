package com.cashier.api.sync;

import com.cashier.api.ApiServer;
import com.cashier.dao.UserDAO;
import com.cashier.model.User;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket 同步处理器
 * 处理终端连接、断开、消息
 */
public class SyncWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(SyncWebSocketHandler.class);
    
    /**
     * 处理连接
     */
    public static void onConnect(WsConnectContext ctx) {
        try {
            // 从 query 参数获取认证信息
            String token = ctx.queryParam("token");
            String terminalName = ctx.queryParam("terminal");
            
            if (terminalName == null || terminalName.isEmpty()) {
                terminalName = "终端" + System.currentTimeMillis() % 1000;
            }
            
            // 验证 Token
            User user = null;
            if (token != null && !token.isEmpty()) {
                user = ApiServer.getInstance().validateToken(token);
            }
            
            if (user == null) {
                logger.warn("WebSocket 连接认证失败");
                // 发送错误消息后关闭
                ctx.send("{\"type\":\"ERROR\",\"data\":{\"message\":\"认证失败\"}}");
                return;
            }
            
            // 注册终端
            SyncManager.getInstance().registerTerminal(ctx, user, terminalName);
            
            logger.info("WebSocket 连接成功: {} - {}", user.username, terminalName);
            
        } catch (Exception e) {
            logger.error("WebSocket 连接处理失败", e);
        }
    }
    
    /**
     * 处理断开
     */
    public static void onClose(WsCloseContext ctx) {
        try {
            SyncManager.getInstance().unregisterTerminal(ctx);
            logger.info("WebSocket 连接关闭");
        } catch (Exception e) {
            logger.error("WebSocket 断开处理失败", e);
        }
    }
    
    /**
     * 处理消息
     */
    public static void onMessage(WsMessageContext ctx) {
        try {
            String message = ctx.message();
            SyncManager.getInstance().handleMessage(ctx, message);
        } catch (Exception e) {
            logger.error("WebSocket 消息处理失败", e);
        }
    }
    
    /**
     * 处理错误（可选）
     */
    public static void onError(io.javalin.websocket.WsErrorContext ctx) {
        logger.error("WebSocket 错误: {}", ctx.error());
        SyncManager.getInstance().unregisterTerminal(ctx);
    }
}