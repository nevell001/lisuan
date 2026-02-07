package com.cashier.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类
 * 统一管理项目日志输出
 */
public class LoggerFactoryUtil {

    private LoggerFactoryUtil() {
        // 工具类不允许实例化
    }

    /**
     * 获取指定类的日志记录器
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * 获取指定名称的日志记录器
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
}