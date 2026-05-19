package com.cashier.api.sync;

import com.cashier.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 同步管理器
 * 管理多终端连接和事件广播
 */
public class SyncManager {
    private static final Logger logger = LoggerFactory.getLogger(SyncManager.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private static SyncManager instance;
    
    // 终端连接映射: sessionId -> TerminalConnection
    private final ConcurrentHashMap<String, TerminalConnection> connections = new ConcurrentHashMap<>();
    
    // 用户终端映射: userId -> Set of sessionId
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Boolean>> userTerminals = new ConcurrentHashMap<>();
    
    private SyncManager() {}
    
    public static SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }
    
    /**
     * 注册新终端连接
     */
    public void registerTerminal(WsContext ctx, User user, String terminalName) {
        String sessionId = getSessionId(ctx);
        String terminalId = generateTerminalId(user.id, terminalName);
        
        TerminalConnection conn = new TerminalConnection();
        conn.sessionId = sessionId;
        conn.terminalId = terminalId;
        conn.userId = user.id;
        conn.username = user.username;
        conn.terminalName = terminalName;
        conn.connectedAt = System.currentTimeMillis();
        conn.ctx = ctx;
        
        connections.put(sessionId, conn);
        
        // 添加到用户终端列表
        userTerminals.computeIfAbsent(user.id, k -> new ConcurrentHashMap<>())
                     .put(sessionId, true);
        
        logger.info("终端连接: {} - {} ({})", user.username, terminalName, terminalId);
        
        // 广播终端上线事件
        broadcastSyncEvent(SyncEventType.TERMINAL_CONNECTED, Map.of(
            "terminalId", terminalId,
            "terminalName", terminalName,
            "username", user.username,
            "userId", user.id,
            "connectedAt", conn.connectedAt
        ));
        
        // 发送当前在线终端列表
        sendOnlineTerminals(ctx);
    }
    
    /**
     * 移除终端连接
     */
    public void unregisterTerminal(WsContext ctx) {
        String sessionId = getSessionId(ctx);
        TerminalConnection conn = connections.remove(sessionId);
        
        if (conn != null) {
            // 从用户终端列表移除
            ConcurrentHashMap<String, Boolean> userTerm = userTerminals.get(conn.userId);
            if (userTerm != null) {
                userTerm.remove(sessionId);
                if (userTerm.isEmpty()) {
                    userTerminals.remove(conn.userId);
                }
            }
            
            logger.info("终端断开: {} - {}", conn.username, conn.terminalName);
            
            // 广播终端下线事件
            broadcastSyncEvent(SyncEventType.TERMINAL_DISCONNECTED, Map.of(
                "terminalId", conn.terminalId,
                "terminalName", conn.terminalName,
                "username", conn.username
            ));
        }
    }
    
    /**
     * 广播同步事件到所有终端
     */
    public void broadcastSyncEvent(SyncEventType eventType, Map<String, Object> data) {
        SyncMessage message = new SyncMessage();
        message.type = eventType.name();
        message.timestamp = System.currentTimeMillis();
        message.data = data;
        
        String json = toJson(message);
        
        for (TerminalConnection conn : connections.values()) {
            try {
                conn.ctx.send(json);
            } catch (Exception e) {
                logger.warn("发送消息失败: {}", conn.terminalId, e);
            }
        }
        
        logger.debug("广播事件: {} -> {} 个终端", eventType, connections.size());
    }
    
    /**
     * 广播到指定用户的终端
     */
    public void broadcastToUser(int userId, SyncEventType eventType, Map<String, Object> data) {
        ConcurrentHashMap<String, Boolean> terminals = userTerminals.get(userId);
        if (terminals == null || terminals.isEmpty()) return;
        
        SyncMessage message = new SyncMessage();
        message.type = eventType.name();
        message.timestamp = System.currentTimeMillis();
        message.data = data;
        
        String json = toJson(message);
        
        for (String sessionId : terminals.keySet()) {
            TerminalConnection conn = connections.get(sessionId);
            if (conn != null) {
                try {
                    conn.ctx.send(json);
                } catch (Exception e) {
                    logger.warn("发送消息失败: {}", conn.terminalId, e);
                }
            }
        }
    }
    
    /**
     * 发送在线终端列表
     */
    private void sendOnlineTerminals(WsContext ctx) {
        SyncMessage message = new SyncMessage();
        message.type = SyncEventType.ONLINE_TERMINALS.name();
        message.timestamp = System.currentTimeMillis();
        
        // 构建在线终端列表
        java.util.List<Map<String, Object>> terminals = new java.util.ArrayList<>();
        for (TerminalConnection conn : connections.values()) {
            terminals.add(Map.of(
                "terminalId", conn.terminalId,
                "terminalName", conn.terminalName,
                "username", conn.username,
                "userId", conn.userId,
                "connectedAt", conn.connectedAt
            ));
        }
        
        message.data = Map.of("terminals", terminals, "count", terminals.size());
        
        ctx.send(toJson(message));
    }
    
    /**
     * 处理客户端消息
     */
    public void handleMessage(WsContext ctx, String message) {
        try {
            SyncMessage msg = mapper.readValue(message, SyncMessage.class);
            String sessionId = getSessionId(ctx);
            TerminalConnection conn = connections.get(sessionId);
            
            if (conn == null) return;
            
            logger.debug("收到消息: {} from {}", msg.type, conn.terminalName);
            
            switch (msg.type) {
                case "PING":
                    ctx.send(toJson(new SyncMessage("PONG", System.currentTimeMillis(), Map.of("sessionId", sessionId))));
                    break;
                    
                case "TRANSACTION_CREATED":
                    // 广播新交易到所有终端
                    broadcastSyncEvent(SyncEventType.TRANSACTION_CREATED, msg.data);
                    break;
                    
                case "PRODUCT_UPDATED":
                    // 广播商品更新
                    broadcastSyncEvent(SyncEventType.PRODUCT_UPDATED, msg.data);
                    break;
                    
                case "MEMBER_UPDATED":
                    // 广播会员更新
                    broadcastSyncEvent(SyncEventType.MEMBER_UPDATED, msg.data);
                    break;
                    
                case "INVENTORY_CHANGED":
                    // 广播库存变化
                    broadcastSyncEvent(SyncEventType.INVENTORY_CHANGED, msg.data);
                    break;
                    
                case "REQUEST_SYNC":
                    // 响应同步请求
                    handleSyncRequest(ctx, msg);
                    break;
                    
                default:
                    logger.warn("未知消息类型: {}", msg.type);
            }
        } catch (Exception e) {
            logger.error("处理消息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 处理同步请求
     */
    private void handleSyncRequest(WsContext ctx, SyncMessage msg) {
        String entity = (String) msg.data.get("entity");
        SyncMessage response = new SyncMessage();
        response.type = "SYNC_RESPONSE";
        response.timestamp = System.currentTimeMillis();
        
        Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("entity", entity);
        
        try {
            if ("PRODUCTS".equals(entity)) {
                responseData.put("items", com.cashier.dao.DAOFactory.getInstance().getProductDAO().findAll());
            } else if ("MEMBERS".equals(entity)) {
                responseData.put("items", com.cashier.dao.MemberDAO.findAll());
            } else if ("TRANSACTIONS".equals(entity)) {
                // 返回最近的100条交易
                List<com.cashier.model.Transaction> transactions = com.cashier.dao.TransactionDAO.findAll();
                transactions.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
                responseData.put("items", transactions.subList(0, Math.min(transactions.size(), 100)));
            } else {
                responseData.put("status", "error");
                responseData.put("message", "不支持的同步实体: " + entity);
                response.data = responseData;
                ctx.send(toJson(response));
                return;
            }
            
            responseData.put("status", "success");
            responseData.put("count", ((java.util.List<?>) responseData.get("items")).size());
        } catch (Exception e) {
            logger.error("同步数据失败: {}", entity, e);
            responseData.put("status", "error");
            responseData.put("message", "同步失败: " + e.getMessage());
        }
        
        response.data = responseData;
        ctx.send(toJson(response));
    }
    
    /**
     * 获取在线终端数量
     */
    public int getOnlineCount() {
        return connections.size();
    }
    
    /**
     * 获取指定用户的在线终端数量
     */
    public int getUserOnlineCount(int userId) {
        ConcurrentHashMap<String, Boolean> terminals = userTerminals.get(userId);
        return terminals != null ? terminals.size() : 0;
    }
    
    /**
     * 获取所有在线终端信息
     */
    public java.util.List<TerminalConnection> getOnlineTerminals() {
        return new java.util.ArrayList<>(connections.values());
    }
    
    /**
     * 获取会话ID（兼容不同版本）
     */
    private String getSessionId(WsContext ctx) {
        // Javalin 5.x 使用 hashCode 作为 sessionId
        return "WS_" + System.identityHashCode(ctx);
    }
    
    /**
     * 生成终端ID
     */
    private String generateTerminalId(int userId, String terminalName) {
        return "TERM_" + userId + "_" + terminalName + "_" + System.currentTimeMillis();
    }
    
    /**
     * 转换为 JSON
     */
    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            logger.error("JSON转换失败", e);
            return "{}";
        }
    }
}