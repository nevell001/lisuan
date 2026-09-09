package com.cashier.util;

import com.cashier.constant.DatabaseConfigKeys;

import com.cashier.exception.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 数据库管理器
 * 负责 MySQL 数据库连接的创建、管理和初始化
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactoryUtil.getLogger(DatabaseManager.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static SecureRandom getSecureRandom() {
        return SECURE_RANDOM;
    }

    private static volatile HikariDataSource dataSource;
    private static volatile boolean initialized = false;
    private static volatile HikariDataSource testDataSource = null; // 测试用数据源（连接池）

    // 数据库配置
    private static final String CONFIG_FILE = "config/database.properties";
    private static String dbUrl;
    private static String dbUsername;
    private static String dbPassword;
    private static int poolSize = 10;  // 桌面应用默认连接池大小
    private static long connectionTimeout = 5000;  // 减少超时时间避免 UI 冻结
    private static long idleTimeout = 600000;
    private static long maxLifetime = 1800000;
    private static long leakDetectionThreshold = 30000;  // 30秒泄漏检测
    private static String connectionTestQuery = "SELECT 1";
    private static long validationTimeout = 3000;
    private static String dockerMysqlContainerName = "lisuan-mysql";
    private static final String CONSOLE_SEPARATOR = "========================================";
    private static final String DEFAULT_DATABASE_NAME = "lisuan_system";
    private static final String OPERATION_LOGS_TABLE = "operation_logs";
    private static final long DATABASE_COMMAND_TIMEOUT_SECONDS = 30 * 60;
    private static final long DOCKER_STATUS_TIMEOUT_SECONDS = 10;

    static {
        // 检查是否在测试环境中运行
        // 如果设置了 testDataSource（测试数据库）或者环境变量指示测试模式，则跳过 MySQL 初始化
        boolean isTestMode = System.getProperty("test.mode", "false").equals("true") ||
                             System.getenv("TEST_MODE") != null;

        if (!isTestMode) {
            try {
                loadConfig();
                dataSource = new HikariDataSource(createHikariConfig());
                initializeDatabase();
            } catch (Exception e) {
                logger.error("数据库初始化失败，系统将终止启动", e);
                throw new ExceptionInInitializerError("数据库初始化失败: " + e.getMessage());
            }
        } else {
            logger.info("检测到测试模式，跳过 MySQL 数据库初始化");
        }
    }

    /**
     * 创建 HikariCP 配置（供静态初始化器和 reloadConfig 共用，确保配置一致）
     */
    private static HikariConfig createHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 连接池配置
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(Math.max(2, poolSize / 4));
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        // 连接泄漏检测
        if (leakDetectionThreshold > 0) {
            config.setLeakDetectionThreshold(leakDetectionThreshold);
        }

        // 连接验证配置
        config.setConnectionTestQuery(connectionTestQuery);
        config.setValidationTimeout(validationTimeout);

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

        // 时区与编码设置
        config.addDataSourceProperty("serverTimezone", "Asia/Shanghai");
        config.addDataSourceProperty("useUnicode", "true");
        config.addDataSourceProperty("characterEncoding", "UTF-8");

        return config;
    }

    /**
     * 加载数据库配置
     */
    private static void loadConfig() {
        Properties props = loadDatabaseProperties();

        // 验证必需的配置项
        dbUrl = props.getProperty(DatabaseConfigKeys.URL);
        dbUsername = props.getProperty(DatabaseConfigKeys.USERNAME);
        dbPassword = resolveDatabasePassword(props);

        if (dbUrl == null || dbUrl.isEmpty() ||
            dbUsername == null || dbUsername.isEmpty() ||
            dbPassword == null || dbPassword.isEmpty()) {
            throw new DatabaseException(
                "数据库配置不完整！\n" +
                "请配置以下参数：\n" +
                "- db.url (数据库连接URL)\n" +
                "- db.username (数据库用户名)\n" +
                "- db.password (数据库密码，或设置环境变量 CASHIER_DB_PASSWORD)",
                DatabaseException.DbErrorType.CONNECTION_FAILED
            );
        }

        poolSize = parseIntProperty(props, DatabaseConfigKeys.POOL_SIZE, poolSize);
        connectionTimeout = parseLongProperty(props, "db.connection.timeout", connectionTimeout);
        idleTimeout = parseLongProperty(props, "db.idle.timeout", idleTimeout);
        maxLifetime = parseLongProperty(props, "db.max.lifetime", maxLifetime);
        leakDetectionThreshold = parseLongProperty(props, "db.connection.leakDetectionThreshold", leakDetectionThreshold);

        connectionTestQuery = props.getProperty("db.connectionTestQuery", "SELECT 1");
        dockerMysqlContainerName = props.getProperty("backup.mysql.container", "lisuan-mysql").trim();
        if (dockerMysqlContainerName.isEmpty()) {
            dockerMysqlContainerName = "lisuan-mysql";
        }

        validationTimeout = parseLongProperty(props, "db.validationTimeout", 3000);
    }

    private static Properties loadDatabaseProperties() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            handleMissingConfigFile();
        }

        try (FileInputStream fis = new FileInputStream(configFile);
             InputStreamReader isr = new InputStreamReader(fis, "UTF-8")) {
            props.load(isr);
            logger.info("已加载数据库配置: {}", CONFIG_FILE);
            return props;
        } catch (IOException e) {
            logger.error("加载配置文件失败: {}", e.getMessage(), e);
            throw DatabaseException.connectionFailed(e);
        }
    }

    private static void handleMissingConfigFile() {
        logger.info("配置文件不存在，创建默认配置文件模板");
        saveDefaultConfigTemplate();
        throw new DatabaseException(
            "数据库配置文件不存在: " + CONFIG_FILE + "\n" +
            "请先配置数据库连接信息：\n" +
            "1. 编辑 config/database.properties 文件\n" +
            "2. 设置正确的数据库 URL、用户名和密码\n" +
            "3. 或者设置环境变量 CASHIER_DB_PASSWORD 来避免明文存储密码\n" +
            "4. 然后重新启动应用",
            DatabaseException.DbErrorType.CONNECTION_FAILED
        );
    }

    private static String resolveDatabasePassword(Properties props) {
        String envPassword = System.getenv("CASHIER_DB_PASSWORD");
        if (envPassword == null || envPassword.isEmpty()) {
            // L-2: 向后兼容旧拼写 CASHER_DB_PASSWORD
            envPassword = System.getenv("CASHER_DB_PASSWORD");
            if (envPassword != null && !envPassword.isEmpty()) {
                logger.warn("检测到旧环境变量 CASHER_DB_PASSWORD，建议迁移到 CASHIER_DB_PASSWORD");
            }
        }
        if (envPassword != null && !envPassword.isEmpty()) {
            logger.info("已从环境变量读取数据库密码");
            return envPassword;
        }
        return props.getProperty(DatabaseConfigKeys.PASSWORD);
    }

    private static int parseIntProperty(Properties props, String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long parseLongProperty(Properties props, String key, long defaultValue) {
        try {
            return Long.parseLong(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 保存默认配置文件模板
     */
    private static void saveDefaultConfigTemplate() {
        try {
            File configFile = new File(CONFIG_FILE);
            configFile.getParentFile().mkdirs();

            Properties props = new Properties();
            props.setProperty(DatabaseConfigKeys.URL, "jdbc:mysql://localhost:3306/lisuan_system?sslMode=PREFERRED&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
            props.setProperty(DatabaseConfigKeys.USERNAME, "lisuan");
            // 安全提示：建议使用环境变量 CASHIER_DB_PASSWORD 存储密码，避免明文存储
            // Windows: set CASHIER_DB_PASSWORD=YourPassword
            // Linux/Mac: export CASHIER_DB_PASSWORD=YourPassword
            props.setProperty(DatabaseConfigKeys.PASSWORD, "");
            props.setProperty(DatabaseConfigKeys.POOL_SIZE, "10");
            props.setProperty("backup.mysql.container", "lisuan-mysql");

            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                props.store(fos, "收银系统数据库配置文件模板\n" +
                    "安全提示：建议设置环境变量 CASHIER_DB_PASSWORD 来存储数据库密码，避免明文存储");
                logger.info("已创建默认配置文件模板: {}", CONFIG_FILE);
            }
        } catch (IOException e) {
            logger.error("创建配置文件模板失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取数据库连接
     * @return Connection 对象
     * @throws SQLException 如果获取连接失败
     */
    public static Connection getConnection() throws SQLException {
        // 如果设置了测试数据源，优先返回测试数据源的连接
        if (testDataSource != null && !testDataSource.isClosed()) {
            return testDataSource.getConnection();
        }

        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("数据源未初始化或已关闭");
        }
        return dataSource.getConnection();
    }

    /**
     * 设置测试数据源（仅用于单元测试）
     * @param dataSource 测试数据库数据源（连接池）
     */
    public static synchronized void setTestConnection(HikariDataSource dataSource) {
        testDataSource = dataSource;
        logger.debug("测试数据源已设置，testDataSource={}", testDataSource);
    }

    /**
     * 清除测试数据源（仅用于单元测试）
     */
    public static synchronized void clearTestConnection() {
        if (testDataSource != null && !testDataSource.isClosed()) {
            testDataSource.close();
        }
        testDataSource = null;
        logger.debug("测试数据源已清除");
    }

    /**
     * 初始化数据库表结构
     */
    private static void initializeDatabase() {
        // 每次启动都检查表结构，确保升级脚本被执行
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 创建数据库（如果不存在）
            stmt.execute("CREATE DATABASE IF NOT EXISTS lisuan_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.execute("USE lisuan_system");
            createTableUsers(stmt);
            createTableProducts(stmt);
            createTableMembers(stmt);
            createTableTransactions(stmt);
            createTableTransactionItems(stmt);
            createTableShifts(stmt);
            createTablePromotions(stmt);
            createTableCategories(stmt);
            createTableUnits(stmt);
            createTableRechargeRecords(stmt);
            createTableOperationLogs(stmt);
            createTableSettings(stmt);
            createTableThemePreferences(stmt);
            createTableSuppliers(stmt);
            createTablePurchaseOrders(stmt);
            createTablePurchaseOrderItems(stmt);
            createTablePurchaseApprovals(stmt);
            createTablePurchaseInbound(stmt);
            createTablePurchaseInboundItems(stmt);
            createTableInventoryCheck(stmt);
            createTableInventoryCheckItems(stmt);
            createTableReturnOrders(stmt);
            createTableReturnOrderItems(stmt);
            createTableInvoices(stmt);
            createTableInvoiceItems(stmt);
            createTableBackupRecords(stmt);
            createTableBackupConfig(stmt);
            upgradeTableStructure(stmt);

            // 创建默认管理员用户（如果不存在）
            createDefaultAdminUser(stmt);

            initialized = true;
            logger.info("MySQL 数据库初始化成功");

        } catch (SQLException e) {
            logger.error("数据库表创建失败", e);
        }
    }

    private static void createTableUsers(Statement stmt) throws SQLException {
            // 创建用户表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    active TINYINT(1) DEFAULT 1,
                    force_password_change TINYINT(1) DEFAULT 0,
                    last_login_time BIGINT,
                    create_time BIGINT,
                    INDEX idx_username (username),
                    INDEX idx_role (role)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

    }

    private static void createTableProducts(Statement stmt) throws SQLException {
            // 创建商品表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    product_code VARCHAR(50) UNIQUE COMMENT '商品编号',
                    name VARCHAR(200) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    quantity INT DEFAULT 0,
                    category VARCHAR(50),
                    barcode VARCHAR(50),
                    unit VARCHAR(20) DEFAULT '件',
                    description TEXT,
                    brand VARCHAR(100),
                    supplier VARCHAR(100),
                    spec VARCHAR(100),
                    min_stock INT DEFAULT 0,
                    cost DECIMAL(10,2),
                    version INT DEFAULT 0 COMMENT '版本号（用于乐观锁）',
                    is_hot TINYINT DEFAULT 0 COMMENT '是否热销',
                    created_at BIGINT,
                    updated_at BIGINT,
                    INDEX idx_product_code (product_code),
                    INDEX idx_name (name),
                    INDEX idx_barcode (barcode),
                    INDEX idx_category (category),
                    FULLTEXT idx_ft_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 确保 products.is_hot 列存在（旧库可能没有；CREATE TABLE IF NOT EXISTS 对已存在的表不会补列）
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN is_hot TINYINT DEFAULT 0 COMMENT '是否热销'");
            } catch (SQLException e) {
                // 列已存在会抛 Duplicate column 错误，属于正常情况，忽略
                logger.debug("products.is_hot 列已存在或添加失败: {}", e.getMessage());
            }

            // 挂单表
            try {
                com.cashier.dao.DAOFactory.getInstance().getHoldOrderDAO().createTable();
            } catch (SQLException e) {
                logger.warn("创建挂单表失败", e);
            }

    }

    private static void createTableMembers(Statement stmt) throws SQLException {
            // 创建会员表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    member_code VARCHAR(50) UNIQUE COMMENT '会员编号',
                    phone VARCHAR(20) UNIQUE NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    balance DECIMAL(10,2) DEFAULT 0,
                    points DECIMAL(10,2) DEFAULT 0,
                    level VARCHAR(20) DEFAULT '普通',
                    discount DECIMAL(4,2) DEFAULT 10.00,
                    join_date BIGINT,
                    birthday VARCHAR(10),
                    version INT DEFAULT 0 COMMENT '乐观锁版本号',
                    INDEX idx_member_code (member_code),
                    INDEX idx_name (name),
                    INDEX idx_level (level)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 为已存在的 members 表添加 member_code 字段（如果不存在）
            try {
                String checkColumnSql = """
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'members'
                    AND COLUMN_NAME = 'member_code'
                """;
                try (ResultSet rs = stmt.executeQuery(checkColumnSql)) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.execute("ALTER TABLE members ADD COLUMN member_code VARCHAR(50) UNIQUE COMMENT '会员编号' AFTER id");
                        logger.info("已为 members 表添加 member_code 字段");

                        // 为现有会员生成会员编号
                        stmt.execute("""
                            UPDATE members
                            SET member_code = CONCAT('M', LPAD(id, 6, '0'))
                            WHERE member_code IS NULL OR member_code = ''
                        """);
                        logger.info("已为现有会员生成会员编号");
                    }
                }
            } catch (SQLException e) {
                logger.warn("检查或添加 member_code 字段时出错（可能已存在）: " + e.getMessage());
            }

            // 为已存在的 members 表添加 version 字段（乐观锁，如果不存在）
            try {
                if (columnMissing(stmt, "members", "version")) {
                    stmt.execute("ALTER TABLE members ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号'");
                    logger.info("已为 members 表添加 version 字段（乐观锁）");
                }
            } catch (SQLException e) {
                logger.warn("检查或添加 version 字段时出错（可能已存在）: " + e.getMessage());
            }

    }

    private static void createTableTransactions(Statement stmt) throws SQLException {
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
                    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '交易状态（NORMAL-正常，REFUNDED-已退款）',
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

    }

    private static void createTableTransactionItems(Statement stmt) throws SQLException {
            // 创建交易商品明细表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transaction_items (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    transaction_id VARCHAR(50) NOT NULL,
                    product_id INT COMMENT '商品ID',
                    product_code VARCHAR(50) COMMENT '商品编号',
                    product_name VARCHAR(200) NOT NULL,
                    barcode VARCHAR(100) COMMENT '条形码',
                    price DECIMAL(10,2) NOT NULL,
                    quantity INT NOT NULL,
                    subtotal DECIMAL(10,2) NOT NULL,
                    INDEX idx_transaction_id (transaction_id),
                    INDEX idx_product_id (product_id),
                    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

    }

    private static void createTableShifts(Statement stmt) throws SQLException {
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

    }

    private static void createTablePromotions(Statement stmt) throws SQLException {
            // 创建促销表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS promotions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    threshold DECIMAL(10,2) DEFAULT 0,
                    discount DECIMAL(10,2) NOT NULL,
                    description TEXT,
                    start_date TIMESTAMP NULL,
                    end_date TIMESTAMP NULL,
                    enabled TINYINT(1) DEFAULT 1,
                    usage_count INT DEFAULT 0,
                    max_usage INT,
                    created_at BIGINT,
                    INDEX idx_type (type),
                    INDEX idx_enabled (enabled)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

    }

    private static void createTableCategories(Statement stmt) throws SQLException {
            // 创建分类表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) UNIQUE NOT NULL,
                    description TEXT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

    }

    private static void createTableUnits(Statement stmt) throws SQLException {
            // 创建单位表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS units (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) UNIQUE NOT NULL,
                    description TEXT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

    }

    private static void createTableRechargeRecords(Statement stmt) throws SQLException {
            // 创建充值记录表
            // 列结构与 RechargeRecordDAORefactored / 测试库 DatabaseTestBase 保持一致。
            // 注意：v2.5 之前的旧表 recharges 已废弃（无任何生产读写方），老库中的历史行如需
            // 保留请人工迁移到本表后再删除旧表。
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recharge_records (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    record_id VARCHAR(50) UNIQUE NOT NULL COMMENT '充值记录编号',
                    member_phone VARCHAR(20) NOT NULL,
                    member_name VARCHAR(100),
                    amount DECIMAL(10,2) NOT NULL,
                    payment_method VARCHAR(20),
                    operator VARCHAR(50),
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_member_phone (member_phone),
                    INDEX idx_timestamp (timestamp)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

    }

    private static void createTableOperationLogs(Statement stmt) throws SQLException {
            // 创建操作日志表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS operation_logs (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) DEFAULT NULL,
                    operation VARCHAR(200) NOT NULL,
                    details TEXT,
                    ip_address VARCHAR(50),
                    timestamp BIGINT NOT NULL,
                    log_level VARCHAR(20) NOT NULL DEFAULT 'INFO',
                    log_category VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
                    operation_result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
                    affected_records INT NOT NULL DEFAULT 0,
                    INDEX idx_timestamp (timestamp),
                    INDEX idx_username (username),
                    INDEX idx_category (log_category),
                    INDEX idx_result (operation_result),
                    FOREIGN KEY (username) REFERENCES users(username) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

    }

    private static void createTableSettings(Statement stmt) throws SQLException {
            // 创建系统设置表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    `key` VARCHAR(100) PRIMARY KEY,
                    value TEXT NOT NULL,
                    description TEXT,
                    updated_at BIGINT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

    }

    private static void createTableThemePreferences(Statement stmt) throws SQLException {
            // 创建主题偏好表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS theme_preferences (
                    username VARCHAR(50) PRIMARY KEY,
                    theme_name VARCHAR(20) DEFAULT 'lisuan',
                    updated_at BIGINT,
                    INDEX idx_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // ========== v2.3.0-v2.3.1 新增表：采购管理相关表 ==========

    }

    private static void createTableSuppliers(Statement stmt) throws SQLException {
            // 创建供应商表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS suppliers (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    supplier_code VARCHAR(50) UNIQUE COMMENT '供应商编号',
                    name VARCHAR(100) NOT NULL COMMENT '供应商名称',
                    contact_person VARCHAR(50) COMMENT '联系人',
                    phone VARCHAR(20) COMMENT '联系电话',
                    address VARCHAR(200) COMMENT '地址',
                    `rank` VARCHAR(10) DEFAULT 'C' COMMENT '供应商分级（A级、B级、C级）',
                    status TINYINT DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
                    remark TEXT COMMENT '备注',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_name (name),
                    INDEX idx_rank (`rank`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表'
                """);

    }

    private static void createTablePurchaseOrders(Statement stmt) throws SQLException {
            // 创建采购订单表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchase_orders (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    order_no VARCHAR(50) UNIQUE NOT NULL COMMENT '采购订单号',
                    supplier_id INT NOT NULL COMMENT '供应商ID',
                    purchase_date DATE NOT NULL COMMENT '采购日期',
                    expected_date DATE COMMENT '预计到货日期',
                    total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '订单总金额',
                    status VARCHAR(20) DEFAULT 'pending' COMMENT '订单状态（pending-待审批，approved-已审批，rejected-已拒绝，completed-已完成）',
                    purchaser VARCHAR(50) COMMENT '采购人',
                    approver VARCHAR(50) COMMENT '审批人',
                    approval_time TIMESTAMP NULL COMMENT '审批时间',
                    approval_remark TEXT COMMENT '审批意见',
                    remark TEXT COMMENT '备注',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT,
                    INDEX idx_order_no (order_no),
                    INDEX idx_supplier (supplier_id),
                    INDEX idx_status (status),
                    INDEX idx_purchase_date (purchase_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单表'
                """);

    }

    private static void createTablePurchaseOrderItems(Statement stmt) throws SQLException {
            // 创建采购订单明细表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchase_order_items (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    order_id INT NOT NULL COMMENT '订单ID',
                    product_id INT NOT NULL COMMENT '商品ID',
                    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
                    quantity INT NOT NULL COMMENT '采购数量',
                    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
                    total_price DECIMAL(10,2) NOT NULL COMMENT '小计',
                    inbound_quantity INT DEFAULT 0 COMMENT '已入库数量',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
                    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
                    INDEX idx_order (order_id),
                    INDEX idx_product (product_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细表'
                """);

    }

    private static void createTablePurchaseApprovals(Statement stmt) throws SQLException {
            // 创建采购审批记录表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchase_approvals (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    order_id INT NOT NULL COMMENT '订单ID',
                    approver VARCHAR(50) NOT NULL COMMENT '审批人',
                    action VARCHAR(20) NOT NULL COMMENT '审批动作（approve-通过，reject-拒绝）',
                    remark TEXT COMMENT '审批意见',
                    approval_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
                    INDEX idx_order (order_id),
                    INDEX idx_approver (approver)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购审批记录表'
                """);

    }

    private static void createTablePurchaseInbound(Statement stmt) throws SQLException {
            // 创建采购入库记录表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchase_inbound (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    inbound_no VARCHAR(50) UNIQUE NOT NULL COMMENT '入库单号',
                    order_id INT NOT NULL COMMENT '采购订单ID',
                    inbound_date DATE NOT NULL COMMENT '入库日期',
                    total_quantity INT DEFAULT 0 COMMENT '入库总数量',
                    total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '入库总金额',
                    operator VARCHAR(50) COMMENT '操作人',
                    remark TEXT COMMENT '备注',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE RESTRICT,
                    INDEX idx_inbound_no (inbound_no),
                    INDEX idx_order (order_id),
                    INDEX idx_inbound_date (inbound_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库记录表'
                """);

    }

    private static void createTablePurchaseInboundItems(Statement stmt) throws SQLException {
            // 创建采购入库明细表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchase_inbound_items (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    inbound_id INT NOT NULL COMMENT '入库单ID',
                    order_item_id INT NOT NULL COMMENT '订单明细ID',
                    product_id INT NOT NULL COMMENT '商品ID',
                    quantity INT NOT NULL COMMENT '入库数量',
                    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
                    total_price DECIMAL(10,2) NOT NULL COMMENT '小计',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (inbound_id) REFERENCES purchase_inbound(id) ON DELETE CASCADE,
                    FOREIGN KEY (order_item_id) REFERENCES purchase_order_items(id) ON DELETE RESTRICT,
                    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
                    INDEX idx_inbound (inbound_id),
                    INDEX idx_product (product_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库明细表'
                """);

    }

    private static void createTableInventoryCheck(Statement stmt) throws SQLException {
            // 创建库存盘点表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_check (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    check_no VARCHAR(50) UNIQUE NOT NULL COMMENT '盘点单号',
                    check_date DATE NOT NULL COMMENT '盘点日期',
                    check_type VARCHAR(20) DEFAULT 'full' COMMENT '盘点类型（full-全盘，partial-部分盘点）',
                    total_items INT DEFAULT 0 COMMENT '盘点商品总数',
                    diff_items INT DEFAULT 0 COMMENT '差异商品数',
                    status VARCHAR(20) DEFAULT 'pending' COMMENT '盘点状态（pending-待盘点，checking-盘点中，completed-已完成）',
                    operator VARCHAR(50) COMMENT '盘点人',
                    checker VARCHAR(50) COMMENT '审核人',
                    remark TEXT COMMENT '备注',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_check_no (check_no),
                    INDEX idx_check_date (check_date),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存盘点表'
                """);

    }

    private static void createTableInventoryCheckItems(Statement stmt) throws SQLException {
            // 创建库存盘点明细表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_check_items (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    check_id INT NOT NULL COMMENT '盘点单ID',
                    product_id INT NOT NULL COMMENT '商品ID',
                    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
                    book_quantity INT NOT NULL COMMENT '账面数量',
                    actual_quantity INT NOT NULL COMMENT '实际数量',
                    diff_quantity INT NOT NULL COMMENT '差异数量',
                    diff_reason TEXT COMMENT '差异原因',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (check_id) REFERENCES inventory_check(id) ON DELETE CASCADE,
                    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
                    INDEX idx_check (check_id),
                    INDEX idx_product (product_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存盘点明细表'
                """);

            // ========== v2.4.5 新增表：退货管理相关表 ==========

    }

    private static void createTableReturnOrders(Statement stmt) throws SQLException {
            // 创建退货订单表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS return_orders (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    return_order_id VARCHAR(50) UNIQUE NOT NULL COMMENT '退货单号',
                    original_transaction_id VARCHAR(50) NOT NULL COMMENT '原交易ID',
                    member_id INT COMMENT '会员ID',
                    member_name VARCHAR(100) COMMENT '会员姓名',
                    return_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '退货日期',
                    return_reason TEXT COMMENT '退货原因',
                    total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '退货总金额',
                    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
                    payment_method VARCHAR(20) COMMENT '退款方式',
                    operator_name VARCHAR(50) COMMENT '操作员',
                    approver_name VARCHAR(50) COMMENT '审批人',
                    approval_date TIMESTAMP NULL COMMENT '审批日期',
                    approval_comment TEXT COMMENT '审批意见',
                    completed_date TIMESTAMP NULL COMMENT '完成日期',
                    notes TEXT COMMENT '备注',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_return_order_id (return_order_id),
                    INDEX idx_original_transaction (original_transaction_id),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货订单表'
                """);

    }

    private static void createTableReturnOrderItems(Statement stmt) throws SQLException {
            // 创建退货订单明细表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS return_order_items (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    return_order_id VARCHAR(50) NOT NULL COMMENT '退货单号',
                    product_id INT NOT NULL COMMENT '商品ID',
                    product_code VARCHAR(50) COMMENT '商品编号',
                    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
                    barcode VARCHAR(100) COMMENT '条形码',
                    category VARCHAR(50) COMMENT '分类',
                    return_quantity INT NOT NULL COMMENT '退货数量',
                    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
                    return_amount DECIMAL(10,2) NOT NULL COMMENT '退货金额',
                    reason TEXT COMMENT '退货原因',
                    `condition` VARCHAR(20) DEFAULT 'GOOD' COMMENT '商品状态',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (return_order_id) REFERENCES return_orders(return_order_id) ON DELETE CASCADE,
                    INDEX idx_return_order (return_order_id),
                    INDEX idx_product (product_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货订单明细表'
                """);

            // ========== v2.5.0 新增表：发票和备份相关表 ==========

    }

    private static void createTableInvoices(Statement stmt) throws SQLException {
            // 创建发票表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invoices (
                    invoice_id VARCHAR(50) PRIMARY KEY,
                    invoice_code VARCHAR(20),
                    invoice_number VARCHAR(20),
                    transaction_id VARCHAR(50),
                    buyer_name VARCHAR(100),
                    buyer_tax_id VARCHAR(30),
                    buyer_address VARCHAR(200),
                    buyer_phone VARCHAR(50),
                    buyer_bank VARCHAR(100),
                    seller_name VARCHAR(100),
                    seller_tax_id VARCHAR(30),
                    seller_address VARCHAR(200),
                    seller_phone VARCHAR(50),
                    seller_bank VARCHAR(100),
                    total_amount DECIMAL(10,2),
                    tax_amount DECIMAL(10,2),
                    final_amount DECIMAL(10,2),
                    tax_rate DECIMAL(5,4),
                    create_time DATETIME,
                    print_time DATETIME,
                    create_by VARCHAR(50),
                    status VARCHAR(20),
                    void_reason VARCHAR(200),
                    void_time DATETIME,
                    remark VARCHAR(500),
                    payee VARCHAR(50),
                    checker VARCHAR(50),
                    print_count INT DEFAULT 0,
                    pdf_path VARCHAR(200),
                    image_path VARCHAR(200),
                    INDEX idx_transaction (transaction_id),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票表'
                """);

    }

    private static void createTableInvoiceItems(Statement stmt) throws SQLException {
            // 创建发票商品明细表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invoice_items (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    invoice_id VARCHAR(50),
                    product_name VARCHAR(100),
                    specification VARCHAR(100),
                    unit VARCHAR(20),
                    quantity INT,
                    unit_price DECIMAL(10,2),
                    amount DECIMAL(10,2),
                    tax_rate DECIMAL(5,4),
                    tax_amount DECIMAL(10,2),
                    total_amount DECIMAL(10,2),
                    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE CASCADE,
                    INDEX idx_invoice (invoice_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票商品明细表'
                """);

    }

    private static void createTableBackupRecords(Statement stmt) throws SQLException {
            // 创建备份记录表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS backup_records (
                    backup_id VARCHAR(50) PRIMARY KEY,
                    backup_type VARCHAR(20),
                    target VARCHAR(20),
                    file_name VARCHAR(100),
                    local_path VARCHAR(200),
                    remote_path VARCHAR(200),
                    file_size BIGINT,
                    status VARCHAR(20),
                    create_time DATETIME,
                    start_time DATETIME,
                    finish_time DATETIME,
                    duration_seconds INT,
                    content_type VARCHAR(20),
                    scope VARCHAR(20),
                    operator VARCHAR(50),
                    remark VARCHAR(200),
                    error_message VARCHAR(500),
                    checksum VARCHAR(50),
                    auto_backup BOOLEAN,
                    INDEX idx_status (status),
                    INDEX idx_create_time (create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备份记录表'
                """);

    }

    private static void createTableBackupConfig(Statement stmt) throws SQLException {
            // 创建备份配置表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS backup_config (
                    id INT PRIMARY KEY,
                    auto_backup_enabled BOOLEAN,
                    target VARCHAR(20),
                    content_type VARCHAR(20),
                    backup_interval_hours INT,
                    retention_days INT,
                    max_backup_count INT,
                    last_backup_time DATETIME,
                    next_backup_time DATETIME,
                    aliyun_endpoint VARCHAR(100),
                    aliyun_bucket VARCHAR(50),
                    aliyun_access_key VARCHAR(100),
                    aliyun_secret_key VARCHAR(100),
                    local_backup_path VARCHAR(100),
                    create_time DATETIME,
                    update_time DATETIME
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备份配置表'
                """);

            // ========== v2.3.0-v2.3.1 新增表结束 ==========

            // 升级表结构（添加 id 字段）
    }

    /**
     * 升级表结构（为旧表添加 id 字段）
     */
    private static void upgradeTableStructure(Statement stmt) throws SQLException {
        logger.info("检查表结构...");

        // 为 products 表添加 product_code 字段（如果不存在）
        if (columnMissing(stmt, "products", "product_code")) {
            logger.info("正在为 products 表添加 product_code 字段...");
            stmt.execute("ALTER TABLE products ADD COLUMN product_code VARCHAR(50) UNIQUE COMMENT '商品编号' AFTER id");
            stmt.execute("ALTER TABLE products ADD INDEX idx_product_code (product_code)");
        }

        // 为 members 表添加 id 字段（如果不存在）
        if (columnMissing(stmt, "members", "id")) {
            logger.info("正在为 members 表添加 id 字段...");
            stmt.execute("ALTER TABLE members ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
        }

        // 为 categories 表添加 id 字段（如果不存在）
        if (columnMissing(stmt, "categories", "id")) {
            logger.info("正在为 categories 表添加 id 字段...");
            stmt.execute("ALTER TABLE categories ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
            stmt.execute("ALTER TABLE categories MODIFY COLUMN name VARCHAR(50) UNIQUE NOT NULL");
        }

        // 为 units 表添加 id 字段（如果不存在）
        if (columnMissing(stmt, "units", "id")) {
            logger.info("正在为 units 表添加 id 字段...");
            stmt.execute("ALTER TABLE units ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
            stmt.execute("ALTER TABLE units MODIFY COLUMN name VARCHAR(50) UNIQUE NOT NULL");
        }

        // 创建主题偏好表（如果不存在）
        if (tableMissing(stmt, "theme_preferences")) {
            logger.info("正在创建 theme_preferences 表...");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS theme_preferences (
                    username VARCHAR(50) PRIMARY KEY,
                    theme_name VARCHAR(20) DEFAULT 'lisuan',
                    updated_at BIGINT,
                    INDEX idx_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        }

        // 创建字号偏好表（如果不存在）
        if (tableMissing(stmt, "font_size_preferences")) {
            logger.info("正在创建 font_size_preferences 表...");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS font_size_preferences (
                    username VARCHAR(50) PRIMARY KEY,
                    font_size VARCHAR(20) DEFAULT 'medium',
                    updated_at BIGINT,
                    INDEX idx_username (username),
                    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        }

        // 为 promotions 表添加 promotion_code 字段（如果不存在）
        if (columnMissing(stmt, "promotions", "promotion_code")) {
            logger.info("正在为 promotions 表添加 promotion_code 字段...");
            stmt.execute("ALTER TABLE promotions ADD COLUMN promotion_code VARCHAR(50) UNIQUE AFTER id");
        }
        stmt.execute("UPDATE promotions SET promotion_code = CONCAT('P', LPAD(id, 6, '0')) WHERE promotion_code IS NULL OR promotion_code = ''");

        // 迁移 promotions 表日期列：BIGINT → TIMESTAMP（M-24）
        migratePromotionDateColumns(stmt);

        // 为 users 表添加 force_password_change 字段（如果不存在）
        if (columnMissing(stmt, "users", "force_password_change")) {
            logger.info("正在为 users 表添加 force_password_change 字段...");
            stmt.execute("ALTER TABLE users ADD COLUMN force_password_change TINYINT(1) DEFAULT 0 AFTER active");
        }

        // 为 transactions 表添加 status 字段（REST 退款终态/幂等保护，如果不存在）
        ensureColumn(stmt, "transactions", "status",
            "VARCHAR(20) DEFAULT 'NORMAL' COMMENT '交易状态（NORMAL-正常，REFUNDED-已退款）'");

        ensureColumn(stmt, OPERATION_LOGS_TABLE, "ip_address", "VARCHAR(50) DEFAULT NULL");
        ensureColumn(stmt, OPERATION_LOGS_TABLE, "log_level", "VARCHAR(20) NOT NULL DEFAULT 'INFO'");
        ensureColumn(stmt, OPERATION_LOGS_TABLE, "log_category", "VARCHAR(50) NOT NULL DEFAULT 'SYSTEM'");
        ensureColumn(stmt, OPERATION_LOGS_TABLE, "operation_result", "VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'");
        ensureColumn(stmt, OPERATION_LOGS_TABLE, "affected_records", "INT NOT NULL DEFAULT 0");

        // 创建登录尝试表（用于持久化登录锁定状态）
        if (tableMissing(stmt, "login_attempts")) {
            logger.info("正在创建 login_attempts 表...");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS login_attempts (
                    username VARCHAR(50) PRIMARY KEY,
                    attempt_count INT DEFAULT 0,
                    lockout_until BIGINT DEFAULT NULL,
                    last_attempt_time BIGINT DEFAULT NULL,
                    INDEX idx_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        }

        logger.info("表结构检查完成");
    }

    private static boolean columnMissing(Statement stmt, String table, String column) throws SQLException {
        String sql = "SELECT COUNT(*) AS count FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement pstmt = stmt.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, table);
            pstmt.setString(2, column);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("count") == 0;
            }
        }
    }

    private static boolean tableMissing(Statement stmt, String table) throws SQLException {
        String sql = "SELECT COUNT(*) AS count FROM INFORMATION_SCHEMA.TABLES " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement pstmt = stmt.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, table);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("count") == 0;
            }
        }
    }

    private static void ensureColumn(Statement stmt, String table, String column, String definition) throws SQLException {
        if (columnMissing(stmt, table, column)) {
            logger.info("为 {} 表添加 {} 字段", table, column);
            stmt.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
        }
    }

    /**
     * 检查指定列的数据类型是否为 bigint
     */
    private static boolean columnTypeIsBigint(Statement stmt, String table, String column) throws SQLException {
        String sql = "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement pstmt = stmt.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, table);
            pstmt.setString(2, column);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dataType = rs.getString("DATA_TYPE");
                    return "bigint".equalsIgnoreCase(dataType);
                }
            }
        }
        return false;
    }

    /**
     * 迁移 promotions 表日期列从 BIGINT(epoch millis) 到 TIMESTAMP（M-24）
     * 仅在检测到列类型为 bigint 时执行，保证新库不受影响
     */
    private static void migratePromotionDateColumns(Statement stmt) throws SQLException {
        if (!columnTypeIsBigint(stmt, "promotions", "start_date")) {
            return; // 已经是 TIMESTAMP 类型，无需迁移
        }
        logger.info("检测到 promotions.start_date 为 BIGINT，开始迁移到 TIMESTAMP...");

        for (String col : new String[]{"start_date", "end_date"}) {
            String tmpCol = col + "_ts";
            // 1. 添加临时 TIMESTAMP 列
            stmt.execute("ALTER TABLE promotions ADD COLUMN " + tmpCol + " TIMESTAMP NULL");
            // 2. 转换数据：epoch millis → datetime
            stmt.execute("UPDATE promotions SET " + tmpCol + " = " +
                    "CASE WHEN " + col + " IS NOT NULL AND " + col + " > 0 " +
                    "THEN DATE_ADD('1970-01-01 00:00:00', INTERVAL " + col + " MICROSECOND) " +
                    "ELSE NULL END");
            // 3. 删除旧列
            stmt.execute("ALTER TABLE promotions DROP COLUMN " + col);
            // 4. 重命名临时列
            stmt.execute("ALTER TABLE promotions CHANGE COLUMN " + tmpCol + " " + col + " TIMESTAMP NULL");
        }
        logger.info("promotions 日期列迁移完成");
    }

    /**
     * 创建默认管理员用户（如果不存在）
     */
    private static void createDefaultAdminUser(Statement stmt) throws SQLException {
        logger.info("检查默认用户...");

        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users")) {
            if (rs.next() && rs.getInt("count") == 0) {
                logger.info("创建默认管理员用户...");
                // 生成随机初始密码并加密存储
                String initialPassword = generateRandomPassword();
                String hashedPassword = com.cashier.util.PasswordUtil.hashPassword(initialPassword);
                long currentTime = System.currentTimeMillis();

                // 使用 PreparedStatement 防止 SQL 注入
                String sql = "INSERT INTO users (username, password, name, role, active, force_password_change, create_time, last_login_time) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, NULL)";

                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, hashedPassword);
                    pstmt.setString(3, "系统管理员");
                    pstmt.setString(4, "admin");
                    pstmt.setInt(5, 1);
                    pstmt.setInt(6, 1);
                    pstmt.setLong(7, currentTime);
                    pstmt.executeUpdate();
                }

                logger.info("默认管理员用户创建成功");
                // 安全提示：不在日志中记录用户名和明文密码，避免日志泄露凭据
                logger.info("  初始密码已生成并使用 BCrypt 加密存储，请查看控制台输出获取临时密码");
                printInitialAdminPassword(initialPassword);
            } else {
                logger.info("用户表已有数据，跳过创建默认用户");
            }
        }
    }

    private static void printInitialAdminPassword(String initialPassword) {
        Console console = System.console();
        PrintWriter writer = console == null ? new PrintWriter(System.err, true, StandardCharsets.UTF_8) : console.writer();
        writer.println(CONSOLE_SEPARATOR);
        writer.println("  默认管理员初始密码: " + initialPassword);
        writer.println("  请妥善保存，首次登录后需立即修改！");
        writer.println(CONSOLE_SEPARATOR);
    }

    /**
     * 生成随机密码
     * @return 随机生成的密码
     */
    private static String generateRandomPassword() {
        // L-7: 增强密码复杂度——长度16位，包含大小写字母、数字、特殊字符
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()-_=+";
        String all = upper + lower + digits + special;
        StringBuilder sb = new StringBuilder(16);
        // 确保每类字符至少出现一次
        sb.append(upper.charAt(SECURE_RANDOM.nextInt(upper.length())));
        sb.append(lower.charAt(SECURE_RANDOM.nextInt(lower.length())));
        sb.append(digits.charAt(SECURE_RANDOM.nextInt(digits.length())));
        sb.append(special.charAt(SECURE_RANDOM.nextInt(special.length())));
        for (int i = 4; i < 16; i++) {
            sb.append(all.charAt(SECURE_RANDOM.nextInt(all.length())));
        }
        // 打乱顺序
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return new String(arr);
    }

    /**
     * 检查数据库是否已初始化（包含数据）
     * @return 如果数据库包含数据返回 true，否则返回 false
     */
    public static boolean isDatabasePopulated() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users")) {

            if (rs.next() && rs.getInt("count") > 0) {
                return true;
            }

        } catch (SQLException e) {
            logger.error("检查数据库状态失败: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * 开始事务
     * @param conn 数据库连接
     * @throws SQLException 如果开始事务失败
     */
    public static void beginTransaction(Connection conn) throws SQLException {
        if (conn != null && !conn.getAutoCommit()) {
            throw new SQLException("事务已经在进行中");
        }
        if (conn != null) {
            conn.setAutoCommit(false);
            logger.debug("事务已开始");
        }
    }

    /**
     * 提交事务
     * @param conn 数据库连接
     * @throws SQLException 如果提交事务失败
     */
    public static void commitTransaction(Connection conn) throws SQLException {
        if (conn != null && !conn.getAutoCommit()) {
            try {
                conn.commit();
                logger.debug("事务已提交");
            } finally {
                restoreAutoCommit(conn);
            }
        }
    }

    /**
     * 回滚事务
     * @param conn 数据库连接
     */
    public static void rollbackTransaction(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    try {
                        conn.rollback();
                        logger.debug("事务已回滚");
                    } finally {
                        restoreAutoCommit(conn);
                    }
                }
            } catch (SQLException e) {
                logger.error("回滚事务失败", e);
            }
        }
    }

    /**
     * 执行返回任意结果的事务操作
     * @param callback 事务回调
     * @param <T> 返回值类型
     * @return 事务执行结果
     * @throws SQLException 数据库操作异常
     */
    public static <T> T executeInTransaction(TransactionCallback<T> callback) throws SQLException {
        try (Connection conn = getConnection()) {
            beginTransaction(conn);
            try {
                T result = callback.execute(conn);
                commitTransaction(conn);
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackTransaction(conn);
                throw e;
            }
        }
    }

    /**
     * 执行返回成功状态的事务操作；当回调返回 false 时统一回滚
     * @param callback 事务回调
     * @return 是否执行成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean executeBooleanTransaction(BooleanTransactionCallback callback) throws SQLException {
        try (Connection conn = getConnection()) {
            beginTransaction(conn);
            try {
                boolean success = callback.execute(conn);
                if (!success) {
                    rollbackTransaction(conn);
                    return false;
                }
                commitTransaction(conn);
                return true;
            } catch (SQLException | RuntimeException e) {
                rollbackTransaction(conn);
                throw e;
            }
        }
    }

    private static void restoreAutoCommit(Connection conn) throws SQLException {
        if (conn != null && !conn.getAutoCommit()) {
            conn.setAutoCommit(true);
        }
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute(Connection conn) throws SQLException;
    }

    @FunctionalInterface
    public interface BooleanTransactionCallback {
        boolean execute(Connection conn) throws SQLException;
    }

    /**
     * 关闭数据库连接池
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("数据库连接池已关闭");
        }
    }

    /**
     * 执行数据库备份（使用 mysqldump）
     * @param backupFile 备份文件路径
     * @return 如果备份成功返回 true，否则返回 false
     */
    public static boolean backup(File backupFile) {
        try {
            if (backupFile == null) {
                logger.error("备份文件不能为空");
                return false;
            }

            // 确保备份目录存在
            File backupDir = backupFile.getParentFile();
            if (backupDir != null && !backupDir.exists()) {
                Files.createDirectories(backupDir.toPath());
            }

            // 检查是否可以使用 Docker 容器
            boolean success;
            if (isDockerContainerRunning(dockerMysqlContainerName)) {
                success = backupViaDocker(backupFile);
            } else {
                success = backupViaLocalCommand(backupFile);
            }

            if (!success || !isValidSqlBackupFile(backupFile)) {
                logger.error("数据库备份未生成有效文件: {}", backupFile.getAbsolutePath());
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("数据库备份失败", e);
            return false;
        }
    }

    /**
     * 使用 Docker 容器执行备份
     */
    private static boolean backupViaDocker(File backupFile) throws Exception {
        String containerPath = "/tmp/" + backupFile.getName();

        // 使用 ProcessBuilder 替代 Runtime.exec，密码通过环境变量传递而非命令行参数
        // docker exec -e MYSQL_PWD（不带=值）会从当前进程环境继承该变量
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "exec", "-e", "MYSQL_PWD",  // 不带值，从环境继承
            dockerMysqlContainerName,
            "mysqldump",
            "-u" + dbUsername,
            "--single-transaction",
            "--routines",
            "--triggers",
            getDatabaseNameFromUrl(dbUrl),
            "-r", containerPath
        );
        // 密码通过环境变量传递，不会出现在 ps / /proc/<pid>/cmdline 中
        pb.environment().put("MYSQL_PWD", dbPassword);

        logger.info("执行 Docker 备份命令...");
        Process process = pb.start();
        int exitCode = waitForProcess(process, "Docker 数据库备份", DATABASE_COMMAND_TIMEOUT_SECONDS);

        if (exitCode != 0) {
            // 读取错误输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.error("Docker 错误: {}", line);
                }
            }
            logger.error("Docker 备份失败，退出码: {}", exitCode);
            return false;
        }

        // 从容器复制文件到本地
        String[] copyCommand = {
            "docker", "cp", dockerMysqlContainerName + ":" + containerPath,
            backupFile.getAbsolutePath()
        };

        Process copyProcess = Runtime.getRuntime().exec(copyCommand);
        int copyExitCode = waitForProcess(copyProcess, "复制 Docker 数据库备份", DATABASE_COMMAND_TIMEOUT_SECONDS);

        if (copyExitCode == 0) {
            // 清理容器中的临时文件
            Runtime.getRuntime().exec(new String[]{"docker", "exec", dockerMysqlContainerName, "rm", "-f", containerPath});

            logger.info("数据库备份成功: {}", backupFile.getAbsolutePath());
            return true;
        } else {
            logger.error("从容器复制备份文件失败，退出码: {}", copyExitCode);
            return false;
        }
    }

    /**
     * 使用本地命令执行备份
     */
    private static boolean backupViaLocalCommand(File backupFile) throws Exception {
        // 构建 mysqldump 命令 - 使用环境变量传递密码
        ProcessBuilder pb = new ProcessBuilder(
            "mysqldump",
            "--host=" + getHostFromUrl(dbUrl),
            "--port=" + getPortFromUrl(dbUrl),
            "--user=" + dbUsername,
            "--result-file=" + backupFile.getAbsolutePath(),
            "--single-transaction",
            "--routines",
            "--triggers",
            getDatabaseNameFromUrl(dbUrl)
        );
        
        // 设置环境变量传递密码
        Map<String, String> env = pb.environment();
        env.put("MYSQL_PWD", dbPassword);

        logger.info("执行本地备份命令...");
        Process process = pb.start();
        int exitCode = waitForProcess(process, "本地数据库备份", DATABASE_COMMAND_TIMEOUT_SECONDS);

        if (exitCode == 0) {
            logger.info("数据库备份成功: {}", backupFile.getAbsolutePath());
            return true;
        } else {
            logger.error("mysqldump 执行失败，退出码: {}", exitCode);
            return false;
        }
    }

    /**
     * 执行数据库恢复（使用 mysql 命令）
     * @param backupFile 备份文件路径
     * @return 如果恢复成功返回 true，否则返回 false
     */
    public static boolean restore(File backupFile) {
        if (backupFile == null || !backupFile.exists()) {
            logger.error("备份文件不存在: {}", backupFile != null ? backupFile.getAbsolutePath() : "null");
            return false;
        }

        if (!isValidSqlBackupFile(backupFile)) {
            logger.error("备份文件为空或不可读: {}", backupFile.getAbsolutePath());
            return false;
        }

        try {
            // 检查是否可以使用 Docker 容器
            if (isDockerContainerRunning(dockerMysqlContainerName)) {
                return restoreViaDocker(backupFile);
            } else {
                return restoreViaLocalCommand(backupFile);
            }

        } catch (Exception e) {
            logger.error("数据库恢复失败", e);
            return false;
        }
    }

    /**
     * 使用 Docker 容器执行恢复
     */
    private static boolean restoreViaDocker(File backupFile) throws Exception {
        // 密码通过环境变量传递，不会出现在 ps / /proc/<pid>/cmdline 中
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "exec", "-i", "-e", "MYSQL_PWD",  // 不带值，从环境继承
            dockerMysqlContainerName,
            "mysql",
            "-u" + dbUsername,
            getDatabaseNameFromUrl(dbUrl)
        );
        pb.environment().put("MYSQL_PWD", dbPassword);
        pb.redirectInput(ProcessBuilder.Redirect.from(backupFile));
        pb.redirectErrorStream(true);

        logger.info("执行 Docker 恢复命令...");

        Process process = pb.start();
        int exitCode = waitForProcess(process, "Docker 数据库恢复", DATABASE_COMMAND_TIMEOUT_SECONDS);
        logProcessOutput(process);

        if (exitCode == 0) {
            logger.info("数据库恢复成功: {}", backupFile.getAbsolutePath());
            return true;
        } else {
            logger.error("Docker 恢复失败，退出码: {}", exitCode);
            return false;
        }
    }

    /**
     * 使用本地命令执行恢复
     */
    private static boolean restoreViaLocalCommand(File backupFile) throws Exception {
        // 构建 mysql 命令 - 使用环境变量传递密码
        ProcessBuilder pb = new ProcessBuilder(
            "mysql",
            "--host=" + getHostFromUrl(dbUrl),
            "--port=" + getPortFromUrl(dbUrl),
            "--user=" + dbUsername,
            getDatabaseNameFromUrl(dbUrl)
        );
        
        // 设置环境变量传递密码
        Map<String, String> env = pb.environment();
        env.put("MYSQL_PWD", dbPassword);

        // 重定向输入
        pb.redirectInput(ProcessBuilder.Redirect.from(backupFile));
        pb.redirectErrorStream(true);

        logger.info("执行本地恢复命令...");

        Process process = pb.start();
        int exitCode = waitForProcess(process, "本地数据库恢复", DATABASE_COMMAND_TIMEOUT_SECONDS);
        logProcessOutput(process);

        if (exitCode == 0) {
            logger.info("数据库恢复成功: {}", backupFile.getAbsolutePath());
            return true;
        } else {
            logger.error("mysql 恢复失败，退出码: {}", exitCode);
            return false;
        }
    }

    /**
     * 检查 Docker 容器是否运行
     */
    private static boolean isDockerContainerRunning(String containerName) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                "docker", "ps", "--filter", "name=" + containerName, "--format", "{{.Names}}"
            });
            int exitCode = waitForProcess(process, "检查 Docker 容器状态", DOCKER_STATUS_TIMEOUT_SECONDS);
            if (exitCode != 0) {
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (containerName.equals(line.trim())) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static int waitForProcess(Process process, String operation, long timeoutSeconds)
            throws InterruptedException {
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            logger.error("{}超时（{}秒），已终止进程", operation, timeoutSeconds);
            return -1;
        }
        return process.exitValue();
    }

    private static void logProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    logger.info(line);
                }
            }
        }
    }

    static boolean isValidSqlBackupFile(File backupFile) {
        return backupFile != null
            && backupFile.exists()
            && backupFile.isFile()
            && backupFile.canRead()
            && backupFile.length() > 0;
    }

    /**
     * 从 JDBC URL 提取主机名
     */
    static String getHostFromUrl(String url) {
        // jdbc:mysql://localhost:3306/dbname
        int start = url.indexOf("://") + 3;
        int colon = url.indexOf(":", start);
        int slash = url.indexOf("/", start);
        return url.substring(start, Math.min(colon > 0 ? colon : Integer.MAX_VALUE, slash));
    }

    /**
     * 从 JDBC URL 提取端口
     */
    static int getPortFromUrl(String url) {
        // jdbc:mysql://localhost:3306/dbname
        int colon = url.indexOf(":", url.indexOf("://") + 3);
        int slash = url.indexOf("/", colon);
        if (colon > 0 && slash > colon) {
            return Integer.parseInt(url.substring(colon + 1, slash));
        }
        return 3306; // 默认端口
    }

    static String getDatabaseNameFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_DATABASE_NAME;
        }

        try {
            int protocolEnd = url.indexOf("://");
            int start = url.indexOf("/", protocolEnd >= 0 ? protocolEnd + 3 : 0);
            if (start < 0 || start + 1 >= url.length()) {
                return DEFAULT_DATABASE_NAME;
            }

            int queryStart = url.indexOf("?", start);
            String databaseName = queryStart >= 0 ? url.substring(start + 1, queryStart) : url.substring(start + 1);
            int paramsStart = databaseName.indexOf(";");
            if (paramsStart >= 0) {
                databaseName = databaseName.substring(0, paramsStart);
            }

            databaseName = databaseName.trim();
            return databaseName.isEmpty() ? DEFAULT_DATABASE_NAME : databaseName;
        } catch (Exception e) {
            return DEFAULT_DATABASE_NAME;
        }
    }

    public static String getCurrentDatabaseName() {
        return getDatabaseNameFromUrl(dbUrl);
    }

    public static String getBackupFilePrefix() {
        return sanitizeBackupFilePrefix(getCurrentDatabaseName());
    }

    static String sanitizeBackupFilePrefix(String databaseName) {
        if (databaseName == null || databaseName.isBlank()) {
            return DEFAULT_DATABASE_NAME;
        }
        return databaseName.replaceAll("[^A-Za-z0-9._-]", "_");
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
        synchronized (DatabaseManager.class) {
            shutdown();
            initialized = false;
            loadConfig();
            dataSource = new HikariDataSource(createHikariConfig());
            logger.info("数据库配置已重新加载");
        }
    }
}
