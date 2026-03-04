package com.cashier.notification;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知实体类
 */
public class Notification {
    public String id;
    public NotificationType type;
    public String title;
    public String message;
    public LocalDateTime timestamp;
    public boolean read;
    public Map<String, Object> data;
    
    public Notification(String id, NotificationType type, String title, String message, 
                      LocalDateTime timestamp, boolean read) {
        this(id, type, title, message, timestamp, read, null);
    }
    
    public Notification(String id, NotificationType type, String title, String message, 
                      LocalDateTime timestamp, boolean read, Map<String, Object> data) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
        this.data = data;
    }
    
    public Notification(NotificationType type, String title, String message, Map<String, Object> data) {
        this(java.util.UUID.randomUUID().toString(), type, title, message, 
             LocalDateTime.now(), false, data);
    }
}