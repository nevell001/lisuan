package com.cashier.notification;

import javafx.application.Platform;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 消息通知管理器
 * 负责创建、分发和管理系统通知
 */
public class NotificationManager {
    private static final Logger logger = LoggerFactoryUtil.getLogger(NotificationManager.class);
    private static NotificationManager instance;
    
    private final ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private final Queue<Notification> notificationQueue = new LinkedList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<NotificationType, List<NotificationListener>> listeners = new ConcurrentHashMap<>();
    
    private NotificationManager() {
        startNotificationProcessor();
        logger.info("通知管理器初始化完成");
    }
    
    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
    
    /**
     * 添加通知监听器
     */
    public void addListener(NotificationType type, NotificationListener listener) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
    
    /**
     * 移除通知监听器
     */
    public void removeListener(NotificationType type, NotificationListener listener) {
        List<NotificationListener> typeListeners = listeners.get(type);
        if (typeListeners != null) {
            typeListeners.remove(listener);
        }
    }
    
    /**
     * 发送通知
     */
    public void sendNotification(NotificationType type, String title, String message) {
        Notification notification = new Notification(
            UUID.randomUUID().toString(),
            type,
            title,
            message,
            LocalDateTime.now(),
            false
        );
        
        notificationQueue.add(notification);
        logger.info("通知已添加到队列: {} - {}", title, message);
    }
    
    /**
     * 添加通知（便捷方法，支持额外数据）
     */
    public void addNotification(Notification notification) {
        notificationQueue.add(notification);
        logger.info("通知已添加到队列: {} - {}", notification.title, notification.message);
    }
    
    /**
     * 发送延迟通知
     */
    public void sendDelayedNotification(NotificationType type, String title, String message, long delayMillis) {
        scheduler.schedule(() -> {
            sendNotification(type, title, message);
        }, delayMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 启动通知处理器
     */
    private void startNotificationProcessor() {
        scheduler.scheduleAtFixedRate(() -> {
            Notification notification = notificationQueue.poll();
            if (notification != null) {
                processNotification(notification);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 处理通知
     */
    private void processNotification(Notification notification) {
        // 添加到通知列表
        Platform.runLater(() -> {
            notifications.add(0, notification);
            // 保持最多100条通知
            if (notifications.size() > 100) {
                notifications.remove(notifications.size() - 1);
            }
        });
        
        // 通知监听器
        List<NotificationListener> typeListeners = listeners.get(notification.type);
        if (typeListeners != null) {
            for (NotificationListener listener : typeListeners) {
                try {
                    listener.onNotification(notification);
                } catch (Exception e) {
                    logger.error("通知监听器执行失败", e);
                }
            }
        }
        
        logger.info("通知已处理: {} - {}", notification.title, notification.message);
    }
    
    /**
     * 获取所有通知
     */
    public ObservableList<Notification> getNotifications() {
        return notifications;
    }
    
    /**
     * 标记通知为已读
     */
    public void markAsRead(String notificationId) {
        for (Notification notification : notifications) {
            if (notification.id.equals(notificationId)) {
                notification.read = true;
                break;
            }
        }
    }
    
    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead() {
        for (Notification notification : notifications) {
            notification.read = true;
        }
    }
    
    /**
     * 删除通知
     */
    public void removeNotification(String notificationId) {
        notifications.removeIf(n -> n.id.equals(notificationId));
    }
    
    /**
     * 清空所有通知
     */
    public void clearAll() {
        notifications.clear();
    }
    
    /**
     * 获取未读通知数量
     */
    public int getUnreadCount() {
        int count = 0;
        for (Notification notification : notifications) {
            if (!notification.read) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 关闭通知管理器
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("通知管理器已关闭");
    }
}