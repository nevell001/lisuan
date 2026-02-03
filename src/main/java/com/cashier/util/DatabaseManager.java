package com.cashier.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库管理器
 * 负责 MySQL 数据库连接的创建、管理和初始化
 */
public class DatabaseManager {

    private static HikariDataSource dataSource;
    private static boolean initialized = false;

    // 数据库配置
    private static final String CONFIG_FILE = "config/database.properties";
    private static String dbUrl;
    private static String dbUsername;
    private static String dbPassword;
    private static int poolSize = 10;

    static {
        try {
            // 加载配置
            loadConfig();

            // 配置 HikariCP 连接池
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // 连接池配置
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000); // 30秒超时
            config.setIdleTimeout(600000); // 10分钟空闲超时
            config.setMaxLifetime(1800000); // 30分钟最大生命周期

            // MySQL 特定配置
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            // 时区设置（重要！）
            config.addDataSourceProperty("serverTimezone", "Asia/Shanghai");
            config.addDataSourceProperty("useUnicode", "true");
            config.addDataSourceProperty("characterEncoding", "UTF-8");

            dataSource = new HikariDataSource(config);

            // 初始化数据库表结构
            initializeDatabase();

        } catch (Exception e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载数据库配置
     */
    private static void loadConfig() {
        Properties props = new Properties();

        // 默认配置
        props.setProperty("db.url", "jdbc:mysql://localhost:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        props.setProperty("db.username", "root");
        props.setProperty("db.password", "root");
        props.setProperty("db.pool.size", "10");

        // 从文件加载配置
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                System.out.println("已加载数据库配置: " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("加载配置文件失败，使用默认配置: " + e.getMessage());
            }
        } else {
            System.out.println("配置文件不存在，使用默认配置");
            // 创建默认配置文件
            saveDefaultConfig(props);
        }

        dbUrl = props.getProperty("db.url");
        dbUsername = props.getProperty("db.username");
        dbPassword = props.getProperty("db.password");
        try {
            poolSize = Integer.parseInt(props.getProperty("db.pool.size", "10"));
        } catch (NumberFormatException e) {
            poolSize = 10;
        }
    }

    /**
     * 保存默认配置文件
     */
    private static void saveDefaultConfig(Properties props) {
        try {
            File configFile = new File(CONFIG_FILE);
            configFile.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                props.store(fos, "收银系统数据库配置");
                System.out.println("已创建默认配置文件: " + CONFIG_FILE);
            }
        } catch (IOException e) {
            System.err.println("创建配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据库连接
     * @return Connection 对象
     * @throws SQLException 如果获取连接失败
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("数据源未初始化或已关闭");
        }
        return dataSource.getConnection();
    }

    /**
     * 初始化数据库表结构
     */
    private static void initializeDatabase() {
        if (initialized) {
            return;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 创建数据库（如果不存在）
            stmt.execute("CREATE DATABASE IF NOT EXISTS cashier_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.execute("USE cashier_system");

            // 创建用户表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    active TINYINT(1) DEFAULT 1,
                    last_login_time BIGINT,
                    create_time BIGINT,
                    INDEX idx_username (username),
                    INDEX idx_role (role)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建商品表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    quantity INT DEFAULT 0,
                    category VARCHAR(50),
                    barcode VARCHAR(50) UNIQUE,
                    unit VARCHAR(20) DEFAULT '件',
                    description TEXT,
                    brand VARCHAR(100),
                    supplier VARCHAR(100),
                    spec VARCHAR(100),
                    min_stock INT DEFAULT 0,
                    cost DECIMAL(10,2),
                    created_at BIGINT,
                    updated_at BIGINT,
                    INDEX idx_name (name),
                    INDEX idx_barcode (barcode),
                    INDEX idx_category (category),
                    FULLTEXT idx_ft_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建会员表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    phone VARCHAR(20) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    balance DECIMAL(10,2) DEFAULT 0,
                    points DECIMAL(10,2) DEFAULT 0,
                    level VARCHAR(20) DEFAULT '普通',
                    discount DECIMAL(4,2) DEFAULT 10.00,
                    join_date BIGINT,
                    birthday VARCHAR(10),
                    INDEX idx_name (name),
                    INDEX idx_level (level)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建交易表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    transaction_id VARCHAR(50) PRIMARY KEY,
                    timestamp VARCHAR(50) NOT NULL,
                    total_amount DECIMAL(10,2) NOT NULL,
                    tax DECIMAL(10,2) DEFAULT 0,
                    final_amount DECIMAL(10,2) NOT NULL,
                    payment_method VARCHAR(20) NOT NULL,
                    operator_username VARCHAR(50),
                    operator_name VARCHAR(100),
                    member_phone VARCHAR(20),
                    transaction_type VARCHAR(20) DEFAULT 'sale',
                    voided TINYINT(1) DEFAULT 0,
                    voided_by VARCHAR(50),
                    voided_at BIGINT,
                    INDEX idx_timestamp (timestamp),
                    INDEX idx_operator (operator_username),
                    INDEX idx_member (member_phone),
                    INDEX idx_payment_method (payment_method),
                    FOREIGN KEY (operator_username) REFERENCES users(username) ON DELETE SET NULL,
                    FOREIGN KEY (member_phone) REFERENCES members(phone) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建交易商品明细表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transaction_items (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    transaction_id VARCHAR(50) NOT NULL,
                    product_name VARCHAR(200) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    quantity INT NOT NULL,
                    subtotal DECIMAL(10,2) NOT NULL,
                    INDEX idx_transaction_id (transaction_id),
                    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建班次表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS shifts (
                    shift_id VARCHAR(50) PRIMARY KEY,
                    operator_username VARCHAR(50),
                    operator_name VARCHAR(100),
                    start_time BIGINT NOT NULL,
                    end_time BIGINT,
                    opening_revenue DECIMAL(10,2) DEFAULT 0,
                    closing_revenue DECIMAL(10,2) DEFAULT 0,
                    shift_revenue DECIMAL(10,2) DEFAULT 0,
                    opening_transaction_count INT DEFAULT 0,
                    closing_transaction_count INT DEFAULT 0,
                    shift_transaction_count INT DEFAULT 0,
                    cash_revenue DECIMAL(10,2) DEFAULT 0,
                    wechat_revenue DECIMAL(10,2) DEFAULT 0,
                    alipay_revenue DECIMAL(10,2) DEFAULT 0,
                    card_revenue DECIMAL(10,2) DEFAULT 0,
                    notes TEXT,
                    INDEX idx_operator (operator_username),
                    INDEX idx_start_time (start_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建促销表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS promotions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    threshold DECIMAL(10,2) DEFAULT 0,
                    discount DECIMAL(10,2) NOT NULL,
                    description TEXT,
                    start_date BIGINT,
                    end_date BIGINT,
                    enabled TINYINT(1) DEFAULT 1,
                    usage_count INT DEFAULT 0,
                    max_usage INT,
                    created_at BIGINT,
                    INDEX idx_type (type),
                    INDEX idx_enabled (enabled)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建分类表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    name VARCHAR(50) PRIMARY KEY,
                    description TEXT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建充值记录表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recharges (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    member_phone VARCHAR(20) NOT NULL,
                    member_name VARCHAR(100) NOT NULL,
                    amount DECIMAL(10,2) NOT NULL,
                    payment_method VARCHAR(20) NOT NULL,
                    operator_username VARCHAR(50) NOT NULL,
                    operator_name VARCHAR(100) NOT NULL,
                    timestamp BIGINT,
                    INDEX idx_member_phone (member_phone),
                    INDEX idx_timestamp (timestamp),
                    FOREIGN KEY (member_phone) REFERENCES members(phone) ON DELETE CASCADE,
                    FOREIGN KEY (operator_username) REFERENCES users(username) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建操作日志表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS operation_logs (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    operation VARCHAR(100) NOT NULL,
                    details TEXT,
                    ip_address VARCHAR(50),
                    timestamp BIGINT,
                    INDEX idx_timestamp (timestamp),
                    INDEX idx_username (username),
                    FOREIGN KEY (username) REFERENCES users(username) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建系统设置表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    `key` VARCHAR(100) PRIMARY KEY,
                    value TEXT NOT NULL,
                    description TEXT,
                    updated_at BIGINT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            initialized = true;
            System.out.println("MySQL 数据库初始化成功");

        } catch (SQLException e) {
            System.err.println("数据库表创建失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查数据库是否已初始化（包含数据）
     * @return 如果数据库包含数据返回 true，否则返回 false
     */
    public static boolean isDatabasePopulated() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 检查用户表是否有数据
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
            if (rs.next() && rs.getInt("count") > 0) {
                return true;
            }

        } catch (SQLException e) {
            System.err.println("检查数据库状态失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 关闭数据库连接池
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("数据库连接池已关闭");
        }
    }

    /**
     * 执行数据库备份（使用 mysqldump）
     * @param backupFile 备份文件路径
     * @return 如果备份成功返回 true，否则返回 false
     */
    public static boolean backup(File backupFile) {
        try {
            // 确保备份目录存在
            File backupDir = backupFile.getParentFile();
            if (backupDir != null && !backupDir.exists()) {
                backupDir.mkdirs();
            }

            // 构建 mysqldump 命令
            String[] command = {
                "mysqldump",
                "--host=" + getHostFromUrl(dbUrl),
                "--port=" + getPortFromUrl(dbUrl),
                "--user=" + dbUsername,
                "--password=" + dbPassword,
                "--result-file=" + backupFile.getAbsolutePath(),
                "--single-transaction",
                "--routines",
                "--triggers",
                "cashier_system"
            };

            // 执行备份命令
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("数据库备份成功: " + backupFile.getAbsolutePath());
                return true;
            } else {
                System.err.println("mysqldump 执行失败，退出码: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            System.err.println("数据库备份失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从 JDBC URL 提取主机名
     */
    private static String getHostFromUrl(String url) {
        // jdbc:mysql://localhost:3306/dbname
        int start = url.indexOf("://") + 3;
        int colon = url.indexOf(":", start);
        int slash = url.indexOf("/", start);
        return url.substring(start, Math.min(colon > 0 ? colon : Integer.MAX_VALUE, slash));
    }

    /**
     * 从 JDBC URL 提取端口
     */
    private static int getPortFromUrl(String url) {
        // jdbc:mysql://localhost:3306/dbname
        int colon = url.indexOf(":", url.indexOf("://") + 3);
        int slash = url.indexOf("/", colon);
        if (colon > 0 && slash > colon) {
            return Integer.parseInt(url.substring(colon + 1, slash));
        }
        return 3306; // 默认端口
    }

    /**
     * 检查数据库连接是否正常
     * @return 如果连接正常返回 true，否则返回 false
     */
    public static boolean isConnectionValid() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5); // 5秒超时
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 重新加载数据库配置
     */
    public static void reloadConfig() {
        shutdown();
        initialized = false;
        loadConfig();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.addDataSourceProperty("serverTimezone", "Asia/Shanghai");

        dataSource = new HikariDataSource(config);

        System.out.println("数据库配置已重新加载");
    }
}
