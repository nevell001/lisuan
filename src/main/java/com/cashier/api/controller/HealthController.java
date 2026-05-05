package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.util.DatabaseManager;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 健康检查控制器
 */
public class HealthController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(HealthController.class);
    
    /**
     * 健康检查
     */
    public static void health(Context ctx) {
        HealthInfo health = new HealthInfo();
        health.status = "ok";
        health.timestamp = System.currentTimeMillis();
        
        // 检查数据库连接
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                health.database = "connected";
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (Exception e) {
            health.database = "error: " + e.getMessage();
            health.status = "degraded";
            logger.warn("健康检查: 数据库连接异常", e);
        }
        
        // 检查 API 服务器状态
        health.apiServer = DatabaseManager.isInitialized() ? "running" : "not initialized";
        
        ctx.json(health);
    }
    
    public static class HealthInfo {
        public String status;
        public long timestamp;
        public String database;
        public String apiServer;
    }
}