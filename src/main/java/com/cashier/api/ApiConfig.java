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
    private static final String DEFAULT_TOKEN_SECRET = "default_secret_key";
    
    // API 涉及库存、退款、备份等敏感操作，必须由部署者显式开启。
    private static boolean enabled = false;
    private static int port = 8080;
    private static String host = "127.0.0.1";
    private static String corsOrigins = "*";
    private static int tokenExpireHours = 24;
    private static String tokenSecret = DEFAULT_TOKEN_SECRET;
    
    static {
        loadConfig();

        // API 默认关闭（安全默认）；关闭时这些告警每次启动都会刷屏，只有真正启用后才提示
        if (enabled) {
            warnIfInsecureConfiguration();
        }
    }

    private static void warnIfInsecureConfiguration() {
        // 安全检查：警告使用默认密钥
        if (tokenSecret.equals(DEFAULT_TOKEN_SECRET)) {
            logger.warn("========================================");
            logger.warn("安全警告: 使用默认 TOKEN_SECRET 密钥！");
            logger.warn("请设置环境变量 TOKEN_SECRET 或在 api.properties 中配置");
            logger.warn("生产环境必须使用强随机密钥！");
            logger.warn("========================================");
        }

        // CORS 警告
        if (corsOrigins.equals("*") || corsOrigins.contains("*")) {
            logger.warn("========================================");
            logger.warn("安全警告: CORS 配置允许所有来源 (*)");
            logger.warn("请设置环境变量 CORS_ALLOWED_ORIGINS 限制允许的域名");
            logger.warn("生产环境必须限制 CORS 来源！");
            logger.warn("========================================");
        }
    }
    
    private static void loadConfig() {
        // 优先从环境变量读取敏感配置（生产环境推荐）
        String envTokenSecret = System.getenv("TOKEN_SECRET");
        if (envTokenSecret != null && !envTokenSecret.trim().isEmpty()) {
            tokenSecret = envTokenSecret;
            logger.info("使用环境变量 TOKEN_SECRET");
        }

        String envCorsOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (envCorsOrigins != null && !envCorsOrigins.trim().isEmpty()) {
            corsOrigins = envCorsOrigins;
            logger.info("使用环境变量 CORS_ALLOWED_ORIGINS: {}", corsOrigins);
        }

        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);

                enabled = Boolean.parseBoolean(props.getProperty("api.enabled", "false"));
                port = Integer.parseInt(props.getProperty("api.port", "8080"));
                host = props.getProperty("api.host", "127.0.0.1");

                // 只有在环境变量未设置时才从配置文件读取
                if (tokenSecret.equals(DEFAULT_TOKEN_SECRET)) {
                    tokenSecret = props.getProperty("token.secret", DEFAULT_TOKEN_SECRET);
                }
                if (corsOrigins.equals("*")) {
                    corsOrigins = props.getProperty("cors.allowed.origins", "*");
                }
                tokenExpireHours = Integer.parseInt(props.getProperty("token.expire.hours", "24"));

                logger.info("API 配置加载成功: enabled={}, port={}", enabled, port);
                if (enabled) {
                    logger.warn("生产环境请设置环境变量 TOKEN_SECRET 和 CORS_ALLOWED_ORIGINS");
                }
            } catch (Exception e) {
                enabled = false;
                logger.warn("加载 API 配置失败，API 已安全关闭: {}", e.getMessage());
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

    /**
     * 检查配置是否适合生产环境
     * @return 如果配置安全返回 true，否则返回 false
     */
    public static boolean isProductionReady() {
        boolean ready = isSecurityConfigurationValid(tokenSecret, corsOrigins);
        if (tokenSecret == null || tokenSecret.equals(DEFAULT_TOKEN_SECRET) || tokenSecret.length() < 32) {
            logger.error("生产环境检查失败: TOKEN_SECRET 不安全");
        }
        if (corsOrigins == null || corsOrigins.isBlank() || corsOrigins.contains("*")) {
            logger.error("生产环境检查失败: CORS 配置不安全");
        }
        return ready;
    }

    static boolean isSecurityConfigurationValid(String secret, String allowedOrigins) {
        return secret != null
            && !secret.equals(DEFAULT_TOKEN_SECRET)
            && secret.length() >= 32
            && allowedOrigins != null
            && !allowedOrigins.isBlank()
            && !allowedOrigins.contains("*");
    }
}
