package com.cashier.api;

import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * API 配置管理
 */
public class ApiConfig {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ApiConfig.class);
    
    private static final String CONFIG_FILE = "config/api.properties";
    
    private static boolean enabled = true;
    private static int port = 8080;
    private static String host = "0.0.0.0";
    private static String corsOrigins = "*";
    private static int tokenExpireHours = 24;
    private static String tokenSecret = "default_secret_key";
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                
                enabled = Boolean.parseBoolean(props.getProperty("api.enabled", "true"));
                port = Integer.parseInt(props.getProperty("api.port", "8080"));
                host = props.getProperty("api.host", "0.0.0.0");
                corsOrigins = props.getProperty("cors.allowed.origins", "*");
                tokenExpireHours = Integer.parseInt(props.getProperty("token.expire.hours", "24"));
                tokenSecret = props.getProperty("token.secret", "default_secret_key");
                
                logger.info("API 配置加载成功: enabled={}, port={}", enabled, port);
            } catch (Exception e) {
                logger.warn("加载 API 配置失败，使用默认值: {}", e.getMessage());
            }
        } else {
            logger.info("API 配置文件不存在，使用默认值");
        }
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static int getPort() {
        return port;
    }
    
    public static String getHost() {
        return host;
    }
    
    public static String getCorsOrigins() {
        return corsOrigins;
    }
    
    public static int getTokenExpireHours() {
        return tokenExpireHours;
    }
    
    public static String getTokenSecret() {
        return tokenSecret;
    }
    
    public static void setEnabled(boolean value) {
        enabled = value;
    }
    
    public static void setPort(int value) {
        port = value;
    }
}