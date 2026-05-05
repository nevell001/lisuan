package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 系统设置 REST API
 */
public class SettingsApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(SettingsApiController.class);
    
    private static final String SETTINGS_FILE = "config/settings.properties";
    private static Properties settings = new Properties();
    
    static {
        loadSettings();
    }
    
    private static void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                settings.load(fis);
                logger.info("系统设置加载成功");
            } catch (Exception e) {
                logger.warn("加载系统设置失败: {}", e.getMessage());
            }
        }
    }
    
    private static void saveSettings() {
        File file = new File(SETTINGS_FILE);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            settings.store(fos, "Cashier System Settings");
            logger.info("系统设置保存成功");
        } catch (Exception e) {
            logger.error("保存系统设置失败", e);
        }
    }
    
    /**
     * 获取所有设置
     * GET /api/settings
     */
    public static void list(Context ctx) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", new HashMap<>(settings));
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取系统设置失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取系统设置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个设置
     * GET /api/settings/:key
     */
    public static void get(Context ctx) {
        try {
            String key = ctx.pathParam("key");
            String value = settings.getProperty(key);
            
            if (value == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "设置项不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "key", key, "value", value));
        } catch (Exception e) {
            logger.error("获取设置项失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取设置项失败: " + e.getMessage()));
        }
    }
    
    /**
     * 设置值
     * PUT /api/settings/:key
     */
    public static void set(Context ctx) {
        try {
            String key = ctx.pathParam("key");
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String value = body.get("value");
            
            if (value == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "缺少 value 参数"));
                return;
            }
            
            settings.setProperty(key, value);
            saveSettings();
            
            logger.info("设置已更新: {} = {}", key, value);
            ctx.json(Map.of("success", true, "key", key, "value", value, "message", "设置已更新"));
        } catch (Exception e) {
            logger.error("更新设置失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "更新设置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除设置
     * DELETE /api/settings/:key
     */
    public static void delete(Context ctx) {
        try {
            String key = ctx.pathParam("key");
            String value = settings.getProperty(key);
            
            if (value == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "设置项不存在"));
                return;
            }
            
            settings.remove(key);
            saveSettings();
            
            logger.info("设置已删除: {}", key);
            ctx.json(Map.of("success", true, "message", "设置已删除"));
        } catch (Exception e) {
            logger.error("删除设置失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "删除设置失败: " + e.getMessage()));
        }
    }
}