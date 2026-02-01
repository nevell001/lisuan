package com.cashier.util;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * 状态栏管理器
 * 提供全局的状态栏更新功能
 */
public class StatusBarManager {

    private static final StringProperty statusProperty = new SimpleStringProperty("就绪");

    private StatusBarManager() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取状态属性，用于绑定到 UI
     * @return 状态属性
     */
    public static StringProperty statusProperty() {
        return statusProperty;
    }

    /**
     * 获取当前状态
     * @return 当前状态文本
     */
    public static String getStatus() {
        return statusProperty.get();
    }

    /**
     * 更新状态栏
     * @param status 状态文本
     */
    public static void updateStatus(String status) {
        Platform.runLater(() -> statusProperty.set(status));
    }

    /**
     * 清除状态栏（恢复默认状态）
     */
    public static void clearStatus() {
        updateStatus("就绪");
    }
}
