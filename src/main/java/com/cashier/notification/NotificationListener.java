package com.cashier.notification;

/**
 * 通知监听器接口
 */
public interface NotificationListener {
    /**
     * 当收到通知时调用
     * @param notification 通知对象
     */
    void onNotification(Notification notification);
}