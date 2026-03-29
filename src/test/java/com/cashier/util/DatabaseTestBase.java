package com.cashier.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.*;
import java.sql.*;
import java.util.Properties;

/**
 * 数据库测试基类
 * 提供H2内存数据库的初始化和清理功能
 */
public abstract class DatabaseTestBase {

    private static final String TEST_CONFIG_PATH = "src/test/resources/database.properties";
    private static HikariDataSource testDataSource;
    private static boolean initialized = false;

    /**
     * 检查测试数据库是否已初始化
     */
    protected static boolean isInitialized() {
        return initialized && testDataSource != null && !testDataSource.isClosed();
    }

    /**
     * 初始化测试数据库
     */
    protected static void initTestDatabase() throws SQLException {
        try {
            // 如果已经初始化且数据源未关闭，直接返回
            if (initialized && testDataSource != null && !testDataSource.isClosed()) {
                return;
            }

            // 如果之前的数据源已关闭，先清理
            if (testDataSource != null && !testDataSource.isClosed()) {
                testDataSource.close();
            }

            // 加载测试配置
            Properties props = new Properties();
            File configFile = new File(TEST_CONFIG_PATH);
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                }
            }

            String url = props.getProperty("db.url", "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
            String username = props.getProperty("db.username", "sa");
            String password = props.getProperty("db.password", "");

            // 使用HikariCP创建连接池
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.h2.Driver");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            testDataSource = new HikariDataSource(config);

            // 初始化表结构
            try (Connection conn = testDataSource.getConnection()) {
                createTestTables(conn);
            }

            // 设置到DatabaseManager
            DatabaseManager.setTestConnection(testDataSource);

            initialized = true;
            System.out.println("测试数据库初始化成功");

        } catch (Exception e) {
            throw new SQLException("测试数据库初始化失败", e);
        }
    }

    /**
     * 获取测试数据库连接
     */
    protected static Connection getTestConnection() throws SQLException {
        if (!initialized) {
            initTestDatabase();
        }
        return testDataSource.getConnection();
    }

    /**
     * 创建测试表结构（简化版，只包含必要的表）
     */
    private static void createTestTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        // 创建 users 表
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                name VARCHAR(100) NOT NULL,
                role VARCHAR(20) NOT NULL,
                active TINYINT(1) DEFAULT 1,
                force_password_change TINYINT(1) DEFAULT 0,
                last_login_time TIMESTAMP,
                create_time TIMESTAMP
            )
            """);

        // 创建 products 表
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id INT AUTO_INCREMENT PRIMARY KEY,
                product_code VARCHAR(50) UNIQUE,
                name VARCHAR(200) NOT NULL UNIQUE,
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
                version INT DEFAULT 0,
                created_at BIGINT,
                updated_at BIGINT
            )
            """);

        // 创建 members 表
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS members (
                id INT AUTO_INCREMENT PRIMARY KEY,
                member_code VARCHAR(50) UNIQUE,
                phone VARCHAR(20) UNIQUE NOT NULL,
                name VARCHAR(100) NOT NULL,
                balance DECIMAL(10,2) DEFAULT 0,
                points DECIMAL(10,2) DEFAULT 0,
                level VARCHAR(20) DEFAULT '普通',
                discount DECIMAL(4,2) DEFAULT 10.00,
                join_date BIGINT,
                birthday VARCHAR(10),
                address VARCHAR(200),
                remark TEXT,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 transactions 表
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
                voided_at BIGINT
            )
            """);

        // 创建 transaction_items 表
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaction_items (
                id INT AUTO_INCREMENT PRIMARY KEY,
                transaction_id VARCHAR(50) NOT NULL,
                product_name VARCHAR(100) NOT NULL,
                price DECIMAL(10,2) NOT NULL,
                quantity INT NOT NULL,
                subtotal DECIMAL(10,2) NOT NULL,
                category VARCHAR(50),
                product_id INT,
                product_code VARCHAR(50),
                barcode VARCHAR(50)
            )
            """);

        // 创建 suppliers 表（采购相关）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS suppliers (
                id INT PRIMARY KEY AUTO_INCREMENT,
                supplier_code VARCHAR(50) UNIQUE,
                name VARCHAR(100) NOT NULL,
                contact_person VARCHAR(50),
                phone VARCHAR(20),
                address VARCHAR(200),
                `rank` VARCHAR(10) DEFAULT 'C',
                status TINYINT DEFAULT 1,
                remark TEXT,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 purchase_orders 表（采购订单）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS purchase_orders (
                id INT PRIMARY KEY AUTO_INCREMENT,
                order_no VARCHAR(50) UNIQUE NOT NULL,
                supplier_id INT NOT NULL,
                purchase_date DATE NOT NULL,
                expected_date DATE,
                total_amount DECIMAL(10,2) DEFAULT 0.00,
                status VARCHAR(20) DEFAULT 'pending',
                purchaser VARCHAR(50),
                approver VARCHAR(50),
                approval_time TIMESTAMP NULL,
                approval_remark TEXT,
                remark TEXT,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 purchase_order_items 表（采购订单明细）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS purchase_order_items (
                id INT PRIMARY KEY AUTO_INCREMENT,
                order_id INT NOT NULL,
                product_id INT NOT NULL,
                product_name VARCHAR(100) NOT NULL,
                quantity INT NOT NULL,
                unit_price DECIMAL(10,2) NOT NULL,
                total_price DECIMAL(10,2) NOT NULL,
                inbound_quantity INT DEFAULT 0,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 purchase_inbound 表（采购入库）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS purchase_inbound (
                id INT PRIMARY KEY AUTO_INCREMENT,
                inbound_no VARCHAR(50) UNIQUE NOT NULL,
                order_id INT NOT NULL,
                inbound_date DATE NOT NULL,
                total_quantity INT DEFAULT 0,
                total_amount DECIMAL(10,2) DEFAULT 0.00,
                operator VARCHAR(50),
                remark TEXT,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 purchase_inbound_items 表（采购入库明细）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS purchase_inbound_items (
                id INT PRIMARY KEY AUTO_INCREMENT,
                inbound_id INT NOT NULL,
                order_item_id INT NOT NULL,
                product_id INT NOT NULL,
                quantity INT NOT NULL,
                unit_price DECIMAL(10,2) NOT NULL,
                total_price DECIMAL(10,2) NOT NULL,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 inventory_check 表（库存盘点）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS inventory_check (
                id INT PRIMARY KEY AUTO_INCREMENT,
                check_no VARCHAR(50) UNIQUE NOT NULL,
                check_date DATE NOT NULL,
                check_type VARCHAR(20) DEFAULT 'full',
                total_items INT DEFAULT 0,
                diff_items INT DEFAULT 0,
                status VARCHAR(20) DEFAULT 'pending',
                operator VARCHAR(50),
                checker VARCHAR(50),
                remark TEXT,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 inventory_check_items 表（库存盘点明细）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS inventory_check_items (
                id INT PRIMARY KEY AUTO_INCREMENT,
                check_id INT NOT NULL,
                product_id INT NOT NULL,
                product_name VARCHAR(100) NOT NULL,
                book_quantity INT NOT NULL,
                actual_quantity INT NOT NULL,
                diff_quantity INT NOT NULL,
                diff_reason TEXT,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 promotions 表（促销）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS promotions (
                id INT PRIMARY KEY AUTO_INCREMENT,
                promotion_code VARCHAR(50) UNIQUE,
                name VARCHAR(200) NOT NULL,
                type VARCHAR(20) NOT NULL,
                threshold DECIMAL(10,2) DEFAULT 0,
                discount DECIMAL(10,2) NOT NULL,
                description TEXT,
                enabled TINYINT(1) DEFAULT 1,
                start_date BIGINT,
                end_date BIGINT,
                usage_count INT DEFAULT 0,
                max_usage INT,
                created_at BIGINT
            )
            """);

        // 创建 return_orders 表（退货订单）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS return_orders (
                id INT AUTO_INCREMENT PRIMARY KEY,
                return_order_id VARCHAR(50) UNIQUE NOT NULL,
                original_transaction_id VARCHAR(50),
                member_id INT,
                member_name VARCHAR(100),
                return_date TIMESTAMP NOT NULL,
                return_reason VARCHAR(500),
                total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                payment_method VARCHAR(20),
                operator_name VARCHAR(50) NOT NULL,
                approver_name VARCHAR(50),
                approval_date TIMESTAMP,
                approval_comment VARCHAR(500),
                completed_date TIMESTAMP,
                notes TEXT,
                create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 return_order_items 表（退货订单明细）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS return_order_items (
                id INT PRIMARY KEY AUTO_INCREMENT,
                return_order_id VARCHAR(50) NOT NULL,
                product_id INT,
                product_code VARCHAR(50),
                product_name VARCHAR(100) NOT NULL,
                barcode VARCHAR(50),
                category VARCHAR(50),
                return_quantity INT NOT NULL,
                unit_price DECIMAL(10,2) NOT NULL,
                return_amount DECIMAL(10,2) NOT NULL,
                reason TEXT,
                `condition` VARCHAR(50),
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 operation_logs 表（操作日志）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS operation_logs (
                id INT PRIMARY KEY AUTO_INCREMENT,
                username VARCHAR(50) NOT NULL,
                operation VARCHAR(50) NOT NULL,
                details TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        // 创建 recharge_records 表（充值记录）
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS recharge_records (
                id INT PRIMARY KEY AUTO_INCREMENT,
                record_id VARCHAR(50) UNIQUE NOT NULL,
                member_phone VARCHAR(20) NOT NULL,
                member_name VARCHAR(100),
                amount DECIMAL(10,2) NOT NULL,
                payment_method VARCHAR(20),
                operator VARCHAR(50),
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        stmt.close();
    }

    /**
     * 清理测试数据库
     */
    protected static void cleanupTestDatabase() {
        if (testDataSource != null && !testDataSource.isClosed()) {
            try {
                testDataSource.close();
                initialized = false;
                System.out.println("测试数据库已关闭");
            } catch (Exception e) {
                System.err.println("关闭测试数据库失败: " + e.getMessage());
            }
        }
    }

    /**
     * 清空所有测试数据
     */
    protected static void clearTestData() throws SQLException {
        // 检查数据源是否关闭，如果关闭则重新初始化
        if (testDataSource == null || testDataSource.isClosed()) {
            System.out.println("测试数据源已关闭，重新初始化...");
            initialized = false;
            initTestDatabase();
            System.out.println("重新初始化完成");
        }

        // 从连接池获取连接来清空数据
        try (Connection conn = testDataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            // 按依赖关系倒序删除
            stmt.execute("DELETE FROM transaction_items");
            stmt.execute("DELETE FROM transactions");
            stmt.execute("DELETE FROM members");
            stmt.execute("DELETE FROM inventory_check_items");
            stmt.execute("DELETE FROM inventory_check");
            stmt.execute("DELETE FROM purchase_inbound_items");
            stmt.execute("DELETE FROM purchase_inbound");
            stmt.execute("DELETE FROM purchase_order_items");
            stmt.execute("DELETE FROM purchase_orders");
            stmt.execute("DELETE FROM suppliers");
            stmt.execute("DELETE FROM products");
            stmt.execute("DELETE FROM users");
            stmt.execute("DELETE FROM promotions");
            stmt.execute("DELETE FROM return_order_items");
            stmt.execute("DELETE FROM return_orders");
            stmt.execute("DELETE FROM operation_logs");
            stmt.execute("DELETE FROM recharge_records");
            stmt.close();
        }
    }
}