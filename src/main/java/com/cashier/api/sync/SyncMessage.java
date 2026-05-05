package com.cashier.api.sync;

import java.util.Map;

/**
 * 同步消息结构
 */
public class SyncMessage {
    public String type;           // 消息类型
    public long timestamp;        // 时间戳
    public Map<String, Object> data;  // 消息数据
    
    public SyncMessage() {}
    
    public SyncMessage(String type, long timestamp, Map<String, Object> data) {
        this.type = type;
        this.timestamp = timestamp;
        this.data = data;
    }
    
    /**
     * 创建简单消息
     */
    public static SyncMessage create(SyncEventType type, Map<String, Object> data) {
        return new SyncMessage(type.name(), System.currentTimeMillis(), data);
    }
    
    /**
     * 创建带终端信息的消息
     */
    public static SyncMessage createFromTerminal(SyncEventType type, String terminalId, Map<String, Object> data) {
        Map<String, Object> fullData = new java.util.HashMap<>(data);
        fullData.put("sourceTerminal", terminalId);
        return create(type, fullData);
    }
}