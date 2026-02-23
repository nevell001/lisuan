package com.cashier.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库管理器
 * 负责 MySQL 数据库连接的创建、管理和初始化
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactoryUtil.getLogger(DatabaseManager.class);

    private static HikariDataSource dataSource;
    private static boolean initialized = false;
    private static HikariDataSource testDataSource = null; // 测试用数据源（连接池）

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
            logger.error("数据库初始化失败", e);
        }
    }

    /**
     * 加载数据库配置
     */
    private static void loadConfig() {
        Properties props = new Properties();

        // 从文件加载配置
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                System.out.println("已加载数据库配置: " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("加载配置文件失败: " + e.getMessage());
                throw new RuntimeException("无法加载数据库配置文件: " + CONFIG_FILE, e);
            }
        } else {
            // 配置文件不存在，创建默认配置文件模板
            System.out.println("配置文件不存在，创建默认配置文件模板");
            saveDefaultConfigTemplate();
            throw new RuntimeException("数据库配置文件不存在: " + CONFIG_FILE + "\n" +
                "请先配置数据库连接信息：\n" +
                "1. 编辑 config/database.properties 文件\n" +
                "2. 设置正确的数据库 URL、用户名和密码\n" +
                "3. 或者设置环境变量 CASHER_DB_PASSWORD 来避免明文存储密码\n" +
                "4. 然后重新启动应用");
        }

        // 验证必需的配置项
        dbUrl = props.getProperty("db.url");
        dbUsername = props.getProperty("db.username");
        
        // 优先从环境变量读取密码（更安全），如果没有则从配置文件读取
        String envPassword = System.getenv("CASHER_DB_PASSWORD");
        if (envPassword != null && !envPassword.isEmpty()) {
            dbPassword = envPassword;
            System.out.println("已从环境变量读取数据库密码");
        } else {
            dbPassword = props.getProperty("db.password");
        }

        if (dbUrl == null || dbUrl.isEmpty() ||
            dbUsername == null || dbUsername.isEmpty() ||
            dbPassword == null || dbPassword.isEmpty()) {
            throw new RuntimeException("数据库配置不完整！\n" +
                "请配置以下参数：\n" +
                "- db.url (数据库连接URL)\n" +
                "- db.username (数据库用户名)\n" +
                "- db.password (数据库密码，或设置环境变量 CASHER_DB_PASSWORD)");
        }

        try {
            poolSize = Integer.parseInt(props.getProperty("db.pool.size", "10"));
        } catch (NumberFormatException e) {
            poolSize = 10;
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
            props.setProperty("db.url", "jdbc:mysql://localhost:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4");
            props.setProperty("db.username", "cashier");
            // 安全提示：建议使用环境变量 CASHER_DB_PASSWORD 存储密码，避免明文存储
            // Windows: set CASHER_DB_PASSWORD=YourPassword
            // Linux/Mac: export CASHER_DB_PASSWORD=YourPassword
            props.setProperty("db.password", "YourStrongPassword123!");
            props.setProperty("db.pool.size", "10");

            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                props.store(fos, "收银系统数据库配置文件模板\n" +
                    "安全提示：建议设置环境变量 CASHER_DB_PASSWORD 来存储数据库密码，避免明文存储");
                System.out.println("已创建默认配置文件模板: " + CONFIG_FILE);
            }
        } catch (IOException e) {
            System.err.println("创建配置文件模板失败: " + e.getMessage());
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
    public static void setTestConnection(HikariDataSource dataSource) {
        testDataSource = dataSource;
        System.out.println("DatabaseManager: 测试数据源已设置，testDataSource=" + testDataSource);
    }

    /**
     * 清除测试数据源（仅用于单元测试）
     */
    public static void clearTestConnection() {
        if (testDataSource != null && !testDataSource.isClosed()) {
            testDataSource.close();
        }
        testDataSource = null;
        System.out.println("DatabaseManager: 测试数据源已清除");
    }

    /**
     * 初始化数据库表结构
     */
    private static void initializeDatabase() {
        // 每次启动都检查表结构，确保升级脚本被执行
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
                    force_password_change TINYINT(1) DEFAULT 0,
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
                    created_at BIGINT,
                    updated_at BIGINT,
                    INDEX idx_product_code (product_code),
                    INDEX idx_name (name),
                    INDEX idx_barcode (barcode),
                    INDEX idx_category (category),
                    FULLTEXT idx_ft_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

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
                ResultSet rs = stmt.executeQuery(checkColumnSql);
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
                rs.close();
            } catch (SQLException e) {
                logger.warn("检查或添加 member_code 字段时出错（可能已存在）: " + e.getMessage());
            }

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
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) UNIQUE NOT NULL,
                    description TEXT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // 创建单位表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS units (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) UNIQUE NOT NULL,
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
                    operator_username VARCHAR(50),
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
                    username VARCHAR(50),
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

            // 创建主题偏好表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS theme_preferences (
                    username VARCHAR(50) PRIMARY KEY,
                    theme_name VARCHAR(20) DEFAULT 'light',
                    updated_at BIGINT,
                    INDEX idx_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

            // ========== v2.3.0-v2.3.1 新增表：采购管理相关表 ==========

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

            // ========== v2.3.0-v2.3.1 新增表结束 ==========

            // 升级表结构（添加 id 字段）
            upgradeTableStructure(stmt);

            // 创建默认管理员用户（如果不存在）
            createDefaultAdminUser(stmt);

            initialized = true;
            System.out.println("MySQL 数据库初始化成功");

        } catch (SQLException e) {
            logger.error("数据库表创建失败", e);
        }
    }

    /**
     * 升级表结构（为旧表添加 id 字段）
     */
    private static void upgradeTableStructure(Statement stmt) throws SQLException {
        System.out.println("检查表结构...");
        
        // 为 products 表添加 product_code 字段（如果不存在）
        ResultSet rs = stmt.executeQuery("""
            SELECT COUNT(*) as count 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'products' 
            AND COLUMN_NAME = 'product_code'
        """);
        if (rs.next() && rs.getInt("count") == 0) {
            System.out.println("正在为 products 表添加 product_code 字段...");
            stmt.execute("ALTER TABLE products ADD COLUMN product_code VARCHAR(50) UNIQUE COMMENT '商品编号' AFTER id");
            stmt.execute("ALTER TABLE products ADD INDEX idx_product_code (product_code)");
        }
        rs.close();
        
        // 为 members 表添加 id 字段（如果不存在）
        rs = stmt.executeQuery("""
            SELECT COUNT(*) as count 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'members' 
            AND COLUMN_NAME = 'id'
        """);
        if (rs.next() && rs.getInt("count") == 0) {
            System.out.println("正在为 members 表添加 id 字段...");
            stmt.execute("ALTER TABLE members ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
        }
        rs.close();
        
        // 为 categories 表添加 id 字段（如果不存在）
        rs = stmt.executeQuery("""
            SELECT COUNT(*) as count 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'categories' 
            AND COLUMN_NAME = 'id'
        """);
        if (rs.next() && rs.getInt("count") == 0) {
            System.out.println("正在为 categories 表添加 id 字段...");
            stmt.execute("ALTER TABLE categories ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
            stmt.execute("ALTER TABLE categories MODIFY COLUMN name VARCHAR(50) UNIQUE NOT NULL");
        }
        rs.close();
        
        // 为 units 表添加 id 字段（如果不存在）
        rs = stmt.executeQuery("""
            SELECT COUNT(*) as count 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'units' 
            AND COLUMN_NAME = 'id'
        """);
        if (rs.next() && rs.getInt("count") == 0) {
            System.out.println("正在为 units 表添加 id 字段...");
            stmt.execute("ALTER TABLE units ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
            stmt.execute("ALTER TABLE units MODIFY COLUMN name VARCHAR(50) UNIQUE NOT NULL");
        }
        rs.close();
        
        // 创建主题偏好表（如果不存在）
        rs = stmt.executeQuery("""
            SELECT COUNT(*) as count 
            FROM INFORMATION_SCHEMA.TABLES 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'theme_preferences'
        """);
        if (rs.next() && rs.getInt("count") == 0) {
            System.out.println("正在创建 theme_preferences 表...");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS theme_preferences (
                    username VARCHAR(50) PRIMARY KEY,
                    theme_name VARCHAR(20) DEFAULT 'light',
                    updated_at BIGINT,
                    INDEX idx_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        }
        rs.close();

        // 为 users 表添加 force_password_change 字段（如果不存在）
        rs = stmt.executeQuery("""
            SELECT COUNT(*) as count 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'users' 
            AND COLUMN_NAME = 'force_password_change'
        """);
        if (rs.next() && rs.getInt("count") == 0) {
            System.out.println("正在为 users 表添加 force_password_change 字段...");
            stmt.execute("ALTER TABLE users ADD COLUMN force_password_change TINYINT(1) DEFAULT 0 AFTER active");
        }
        rs.close();
        
        System.out.println("表结构检查完成");
    }

    /**
     * 创建默认管理员用户（如果不存在）
     */
    private static void createDefaultAdminUser(Statement stmt) throws SQLException {
        System.out.println("检查默认用户...");

        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
        if (rs.next() && rs.getInt("count") == 0) {
            System.out.println("创建默认管理员用户...");
            // 使用明文密码，首次登录时强制修改
            String plainPassword = "admin123";
            long currentTime = System.currentTimeMillis();

            // 使用 PreparedStatement 防止 SQL 注入
            String sql = "INSERT INTO users (username, password, name, role, active, force_password_change, create_time, last_login_time) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, NULL)";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "admin");
                pstmt.setString(2, plainPassword);
                pstmt.setString(3, "系统管理员");
                pstmt.setString(4, "admin");
                pstmt.setInt(5, 1);
                pstmt.setInt(6, 1);
                pstmt.setLong(7, currentTime);
                pstmt.executeUpdate();
            }

            System.out.println("默认管理员用户创建成功:");
            System.out.println("  用户名: admin");
            System.out.println("  密码: admin123 (明文，首次登录需修改)");
        } else {
            System.out.println("用户表已有数据，跳过创建默认用户");
        }
        rs.close();
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
            conn.commit();
            logger.debug("事务已提交");
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
                    conn.rollback();
                    logger.debug("事务已回滚");
                }
            } catch (SQLException e) {
                logger.error("回滚事务失败", e);
            }
        }
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

            // 检查是否可以使用 Docker 容器
            if (isDockerContainerRunning("cashier-mysql")) {
                return backupViaDocker(backupFile);
            } else {
                return backupViaLocalCommand(backupFile);
            }

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
        
        // 构建 docker exec 命令
        String[] command = {
            "docker", "exec", "cashier-mysql",
            "mysqldump",
            "-u" + dbUsername,
            "-p" + dbPassword,
            "--single-transaction",
            "--routines",
            "--triggers",
            "cashier_system",
            "-r", containerPath
        };

        System.out.println("执行 Docker 备份命令...");
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            // 读取错误输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("Docker 错误: " + line);
                }
            }
            System.err.println("Docker 备份失败，退出码: " + exitCode);
            return false;
        }

        // 从容器复制文件到本地
        String[] copyCommand = {
            "docker", "cp", "cashier-mysql:" + containerPath,
            backupFile.getAbsolutePath()
        };

        Process copyProcess = Runtime.getRuntime().exec(copyCommand);
        int copyExitCode = copyProcess.waitFor();

        if (copyExitCode == 0) {
            // 清理容器中的临时文件
            Runtime.getRuntime().exec(new String[]{"docker", "exec", "cashier-mysql", "rm", "-f", containerPath});
            
            System.out.println("数据库备份成功: " + backupFile.getAbsolutePath());
            return true;
        } else {
            System.err.println("从容器复制备份文件失败，退出码: " + copyExitCode);
            return false;
        }
    }

    /**
     * 使用本地命令执行备份
     */
    private static boolean backupViaLocalCommand(File backupFile) throws Exception {
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

        System.out.println("执行本地备份命令...");
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println("数据库备份成功: " + backupFile.getAbsolutePath());
            return true;
        } else {
            System.err.println("mysqldump 执行失败，退出码: " + exitCode);
            return false;
        }
    }

    /**
     * 执行数据库恢复（使用 mysql 命令）
     * @param backupFile 备份文件路径
     * @return 如果恢复成功返回 true，否则返回 false
     */
    public static boolean restore(File backupFile) {
        if (!backupFile.exists()) {
            System.err.println("备份文件不存在: " + backupFile.getAbsolutePath());
            return false;
        }

        try {
            // 检查是否可以使用 Docker 容器
            if (isDockerContainerRunning("cashier-mysql")) {
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
        String containerPath = "/tmp/" + backupFile.getName();
        
        // 复制文件到容器
        String[] copyCommand = {
            "docker", "cp", backupFile.getAbsolutePath(),
            "cashier-mysql:" + containerPath
        };

        Process copyProcess = Runtime.getRuntime().exec(copyCommand);
        int copyExitCode = copyProcess.waitFor();

        if (copyExitCode != 0) {
            System.err.println("复制文件到容器失败，退出码: " + copyExitCode);
            return false;
        }

        // 构建 docker exec 命令 - 使用 bash 在容器内执行重定向
        String[] command = {
            "docker", "exec", "cashier-mysql",
            "bash", "-c",
            "mysql -u" + dbUsername + " -p" + dbPassword + " cashier_system < " + containerPath
        };

        System.out.println("执行 Docker 恢复命令...");
        
        Process process = Runtime.getRuntime().exec(command);
        
        // 读取输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        
        // 读取错误
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }
        }

        int exitCode = process.waitFor();

        // 清理容器中的临时文件
        Runtime.getRuntime().exec(new String[]{"docker", "exec", "cashier-mysql", "rm", "-f", containerPath});

        if (exitCode == 0) {
            System.out.println("数据库恢复成功: " + backupFile.getAbsolutePath());
            return true;
        } else {
            System.err.println("Docker 恢复失败，退出码: " + exitCode);
            return false;
        }
    }

    /**
     * 使用本地命令执行恢复
     */
    private static boolean restoreViaLocalCommand(File backupFile) throws Exception {
        // 构建 mysql 命令
        String[] command = {
            "mysql",
            "--host=" + getHostFromUrl(dbUrl),
            "--port=" + getPortFromUrl(dbUrl),
            "--user=" + dbUsername,
            "--password=" + dbPassword,
            "cashier_system"
        };

        System.out.println("执行本地恢复命令...");
        
        // 使用 ProcessBuilder 重定向输入
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectInput(ProcessBuilder.Redirect.from(backupFile));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // 读取输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println("数据库恢复成功: " + backupFile.getAbsolutePath());
            return true;
        } else {
            System.err.println("mysql 恢复失败，退出码: " + exitCode);
            return false;
        }
    }

    /**
     * 检查 Docker 容器是否运行
     */
    private static boolean isDockerContainerRunning(String containerName) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"docker", "ps", "--filter", "name=" + containerName});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
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
