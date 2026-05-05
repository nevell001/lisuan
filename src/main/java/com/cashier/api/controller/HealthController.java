package com.cashier.api.controller;

import com.cashier.util.DatabaseManager;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查接口
 */
public class HealthController {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    /**
     * 基础健康检查
     * GET /api/health
     */
    public static void check(Context ctx) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("service", "cashier-api");
        result.put("timestamp", System.currentTimeMillis());
        ctx.json(result);
    }
    
    /**
     * 详细健康检查
     * GET /api/health/detail
     */
    public static void detail(Context ctx) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("service", "cashier-api");
        result.put("timestamp", System.currentTimeMillis());
        
        // 检查数据库
        try {
            boolean dbOk = DatabaseManager.getConnection() != null;
            result.put("database", dbOk ? "connected" : "disconnected");
        } catch (Exception e) {
            result.put("database", "error: " + e.getMessage());
        }
        
        // 检查内存
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        result.put("memory_used_mb", usedMemory / 1024 / 1024);
        result.put("memory_total_mb", runtime.totalMemory() / 1024 / 1024);
        result.put("memory_max_mb", runtime.maxMemory() / 1024 / 1024);
        
        ctx.json(result);
    }
}