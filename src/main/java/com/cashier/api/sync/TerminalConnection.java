package com.cashier.api.sync;

/**
 * 终端连接信息
 */
public class TerminalConnection {
    public String sessionId;      // WebSocket 会话ID
    public String terminalId;     // 终端唯一标识
    public int userId;            // 用户ID
    public String username;       // 用户名
    public String terminalName;   // 终端名称（如：收银台1、仓库终端）
    public long connectedAt;      // 连接时间
    public io.javalin.websocket.WsContext ctx;  // WebSocket 上下文
    
    @Override
    public String toString() {
        return "TerminalConnection{" +
                "terminalId='" + terminalId + '\'' +
                ", username='" + username + '\'' +
                ", terminalName='" + terminalName + '\'' +
                '}';
    }
}