-- ============================================
-- 收银系统 MySQL 完整初始化脚本
-- ============================================
-- 此脚本整合了用户创建、表结构初始化和示例数据
-- 使用方法: docker exec lisuan-mysql mysql -uroot -pYOUR_PASSWORD --default-character-set=utf8mb4 lisuan_system < 00-init-complete.sql
-- 
-- 版本: v2.4.3
-- 更新日期: 2026-03-07
-- 
-- 变更说明:
-- - 支持 MySQL 8.4 LTS
-- - 使用 --mysql-native-password=ON 参数确保向后兼容
-- - 优化 TIMESTAMP 字段以支持 MySQL 8.4 的新特性
--
-- MySQL 8.4 兼容性说明:
-- - 此脚本完全兼容 MySQL 8.0、8.3 和 8.4
-- - MySQL 8.4 中 default-authentication-plugin 已弃用
-- - 请使用 --mysql-native-password=ON 启动参数确保兼容性
-- - 所有 TIMESTAMP 字段使用 DEFAULT CURRENT_TIMESTAMP 和 ON UPDATE CURRENT_TIMESTAMP
-- - 26 张数据表完整创建，支持所有功能模块

-- ============================================
-- 设置字符集（关键：确保中文正确导入）
-- ============================================
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 确保使用正确的数据库
-- ============================================
USE lisuan_system;

-- ============================================
-- 第一部分：创建专用用户
-- ============================================

-- 1. 创建专用用户（如果不存在）
-- 注意：通过 docker-compose.yml 环境变量创建的用户可能权限不足
-- 这个脚本确保用户有完整的权限

-- ⚠️ 安全警告：请将 'YOUR_CASHIER_PASSWORD_HERE' 替换为您的实际密码！
CREATE USER IF NOT EXISTS 'lisuan'@'%' IDENTIFIED BY 'YOUR_LISUAN_PASSWORD_HERE';
CREATE USER IF NOT EXISTS 'lisuan'@'localhost' IDENTIFIED BY 'YOUR_LISUAN_PASSWORD_HERE';

-- 2. 授予所有权限
GRANT ALL PRIVILEGES ON lisuan_system.* TO 'lisuan'@'%';
GRANT ALL PRIVILEGES ON lisuan_system.* TO 'lisuan'@'localhost';

-- 3. 刷新权限
FLUSH PRIVILEGES;

-- 4. 显示创建的用户
SELECT '=== MySQL 用户创建完成 ===' AS status;
SELECT user, host FROM mysql.user WHERE user IN ('root', 'lisuan');

-- ============================================
-- 第二部分：创建基础表
-- ============================================

-- 创建用户表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建商品表
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(50) UNIQUE COMMENT '商品编号',
    name VARCHAR(200) NOT NULL UNIQUE COMMENT '商品名称（唯一）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建会员表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建交易表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建交易商品明细表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建班次表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建促销表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建分类表
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at BIGINT,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建单位表
CREATE TABLE IF NOT EXISTS units (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    description TEXT,
    created_at BIGINT,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建充值记录表
CREATE TABLE IF NOT EXISTS recharges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_phone VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    operator_username VARCHAR(50),
    operator_name VARCHAR(100),
    timestamp BIGINT NOT NULL,
    balance_before DECIMAL(10,2),
    balance_after DECIMAL(10,2),
    notes TEXT,
    INDEX idx_member (member_phone),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (member_phone) REFERENCES members(phone) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    operation VARCHAR(200) NOT NULL,
    details TEXT,
    timestamp BIGINT NOT NULL,
    log_level VARCHAR(20) DEFAULT 'INFO',
    log_category VARCHAR(50) DEFAULT 'SYSTEM',
    operation_result VARCHAR(20) DEFAULT 'SUCCESS',
    affected_records INT DEFAULT 0,
    request_data TEXT,
    response_data TEXT,
    INDEX idx_username (username),
    INDEX idx_timestamp (timestamp),
    INDEX idx_log_level (log_level),
    INDEX idx_log_category (log_category),
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '=== 基础表创建完成 ===' AS status;

-- ============================================
-- 第三部分：升级现有表结构（向后兼容）
-- ============================================
-- 注意：基础表已在第二部分创建
-- 下面的 ALTER TABLE 语句仅用于从旧版本升级时的向后兼容

-- 创建默认管理员用户（如果不存在）
-- 密码: admin123 (明文，首次登录时强制修改密码)
INSERT IGNORE INTO users (username, password, name, role, active, force_password_change, create_time, last_login_time)
VALUES ('admin', '$2a$10$EVvVqIyQ7Ve2dZb9DKnv/u8JVIyfsp6flS1q9qTVaDB1X4SUTywsu', '系统管理员', 'admin', 1, 1, UNIX_TIMESTAMP() * 1000, NULL);

-- 为 products 表添加 product_code 字段（如果不存在）
-- 注意：product_code 字段已在 CREATE TABLE 中定义（见第二部分）
-- 此语句仅用于从旧版本升级时的向后兼容
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'products'
    AND COLUMN_NAME = 'product_code'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE products ADD COLUMN product_code VARCHAR(50) UNIQUE COMMENT ''商品编号'' AFTER id',
    'SELECT "products.product_code column already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 products 表添加 product_code 索引（如果不存在）
SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'products'
    AND INDEX_NAME = 'idx_product_code'
);

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE products ADD INDEX idx_product_code (product_code)',
    'SELECT "products.idx_product_code index already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 移除 products 表 barcode 字段的 UNIQUE 约束（如果存在）
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'products'
    AND CONSTRAINT_TYPE = 'UNIQUE'
    AND CONSTRAINT_NAME = 'barcode'
);

SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE products DROP INDEX barcode',
    'SELECT "products.barcode UNIQUE constraint does not exist" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 确保 barcode 索引存在（普通索引，用于快速查询）
SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'products'
    AND INDEX_NAME = 'idx_barcode'
);

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE products ADD INDEX idx_barcode (barcode)',
    'SELECT "products.idx_barcode index already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 members 表添加 id 字段（如果不存在）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'members'
    AND COLUMN_NAME = 'id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE members ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST',
    'SELECT "members.id column already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 members 表添加 member_code 字段（如果不存在）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'members'
    AND COLUMN_NAME = 'member_code'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE members ADD COLUMN member_code VARCHAR(50) UNIQUE COMMENT ''会员编号'' AFTER id',
    'SELECT "members.member_code column already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 member_code 索引（如果不存在）
SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'members'
    AND INDEX_NAME = 'idx_member_code'
);

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE members ADD INDEX idx_member_code (member_code)',
    'SELECT "members.idx_member_code index already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 categories 表添加 id 字段（如果不存在）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'categories'
    AND COLUMN_NAME = 'id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE categories ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST, MODIFY COLUMN name VARCHAR(50) UNIQUE NOT NULL',
    'SELECT "categories.id column already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 units 表添加 id 字段（如果不存在）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'units'
    AND COLUMN_NAME = 'id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE units ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST, MODIFY COLUMN name VARCHAR(50) UNIQUE NOT NULL',
    'SELECT "units.id column already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建主题偏好表（如果不存在）
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'theme_preferences'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS theme_preferences (
        username VARCHAR(50) PRIMARY KEY,
        theme_name VARCHAR(20) DEFAULT ''light'',
        updated_at BIGINT,
        INDEX idx_username (username),
        FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
    'SELECT "theme_preferences table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建语言偏好表（如果不存在）
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'language_preferences'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS language_preferences (
        username VARCHAR(50) PRIMARY KEY,
        language_tag VARCHAR(10) DEFAULT ''zh-CN'',
        updated_at BIGINT,
        INDEX idx_username (username),
        FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
    'SELECT "language_preferences table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- v2.3.0-v2.3.1 新增表：采购管理模块
-- ============================================

-- 创建供应商表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'suppliers'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS suppliers (
        id INT PRIMARY KEY AUTO_INCREMENT,
        supplier_code VARCHAR(50) UNIQUE COMMENT ''供应商编号'',
        name VARCHAR(100) NOT NULL COMMENT ''供应商名称'',
        contact_person VARCHAR(50) COMMENT ''联系人'',
        phone VARCHAR(20) COMMENT ''联系电话'',
        address VARCHAR(200) COMMENT ''地址'',
        `rank` VARCHAR(10) DEFAULT ''C'' COMMENT ''供应商分级（A级、B级、C级）'',
        status TINYINT DEFAULT 1 COMMENT ''状态（1-启用，0-禁用）'',
        remark TEXT COMMENT ''备注'',
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_name (name),
        INDEX idx_rank (`rank`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''供应商表''',
    'SELECT "suppliers table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建采购订单表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'purchase_orders'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS purchase_orders (
        id INT PRIMARY KEY AUTO_INCREMENT,
        order_no VARCHAR(50) UNIQUE NOT NULL COMMENT ''采购订单号'',
        supplier_id INT NOT NULL COMMENT ''供应商ID'',
        purchase_date DATE NOT NULL COMMENT ''采购日期'',
        expected_date DATE COMMENT ''预计到货日期'',
        total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT ''订单总金额'',
        status VARCHAR(20) DEFAULT ''pending'' COMMENT ''订单状态（pending-待审批，approved-已审批，rejected-已拒绝，completed-已完成）'',
        purchaser VARCHAR(50) COMMENT ''采购人'',
        approver VARCHAR(50) COMMENT ''审批人'',
        approval_time TIMESTAMP NULL COMMENT ''审批时间'',
        approval_remark TEXT COMMENT ''审批意见'',
        remark TEXT COMMENT ''备注'',
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT,
        INDEX idx_order_no (order_no),
        INDEX idx_supplier (supplier_id),
        INDEX idx_status (status),
        INDEX idx_purchase_date (purchase_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''采购订单表''',
    'SELECT "purchase_orders table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建采购订单明细表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'purchase_order_items'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS purchase_order_items (
        id INT PRIMARY KEY AUTO_INCREMENT,
        order_id INT NOT NULL COMMENT ''订单ID'',
        product_id INT NOT NULL COMMENT ''商品ID'',
        product_name VARCHAR(100) NOT NULL COMMENT ''商品名称'',
        quantity INT NOT NULL COMMENT ''采购数量'',
        unit_price DECIMAL(10,2) NOT NULL COMMENT ''单价'',
        total_price DECIMAL(10,2) NOT NULL COMMENT ''小计'',
        inbound_quantity INT DEFAULT 0 COMMENT ''已入库数量'',
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
        INDEX idx_order (order_id),
        INDEX idx_product (product_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''采购订单明细表''',
    'SELECT "purchase_order_items table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建采购审批记录表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'purchase_approvals'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS purchase_approvals (
        id INT PRIMARY KEY AUTO_INCREMENT,
        order_id INT NOT NULL COMMENT ''订单ID'',
        approver VARCHAR(50) NOT NULL COMMENT ''审批人'',
        action VARCHAR(20) NOT NULL COMMENT ''审批动作（approve-通过，reject-拒绝）'',
        remark TEXT COMMENT ''审批意见'',
        approval_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
        INDEX idx_order (order_id),
        INDEX idx_approver (approver)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''采购审批记录表''',
    'SELECT "purchase_approvals table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建采购入库记录表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'purchase_inbound'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS purchase_inbound (
        id INT PRIMARY KEY AUTO_INCREMENT,
        inbound_no VARCHAR(50) UNIQUE NOT NULL COMMENT ''入库单号'',
        order_id INT NOT NULL COMMENT ''采购订单ID'',
        inbound_date DATE NOT NULL COMMENT ''入库日期'',
        total_quantity INT DEFAULT 0 COMMENT ''入库总数量'',
        total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT ''入库总金额'',
        operator VARCHAR(50) COMMENT ''操作人'',
        remark TEXT COMMENT ''备注'',
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE RESTRICT,
        INDEX idx_inbound_no (inbound_no),
        INDEX idx_order (order_id),
        INDEX idx_inbound_date (inbound_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''采购入库记录表''',
    'SELECT "purchase_inbound table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建采购入库明细表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'purchase_inbound_items'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS purchase_inbound_items (
        id INT PRIMARY KEY AUTO_INCREMENT,
        inbound_id INT NOT NULL COMMENT ''入库单ID'',
        order_item_id INT NOT NULL COMMENT ''订单明细ID'',
        product_id INT NOT NULL COMMENT ''商品ID'',
        quantity INT NOT NULL COMMENT ''入库数量'',
        unit_price DECIMAL(10,2) NOT NULL COMMENT ''单价'',
        total_price DECIMAL(10,2) NOT NULL COMMENT ''小计'',
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (inbound_id) REFERENCES purchase_inbound(id) ON DELETE CASCADE,
        FOREIGN KEY (order_item_id) REFERENCES purchase_order_items(id) ON DELETE RESTRICT,
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
        INDEX idx_inbound (inbound_id),
        INDEX idx_product (product_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''采购入库明细表''',
    'SELECT "purchase_inbound_items table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- v2.3.0-v2.3.1 新增表：库存盘点模块
-- ============================================

-- 创建库存盘点表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'inventory_check'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS inventory_check (
        id INT PRIMARY KEY AUTO_INCREMENT,
        check_no VARCHAR(50) UNIQUE NOT NULL COMMENT ''盘点单号'',
        check_date DATE NOT NULL COMMENT ''盘点日期'',
        check_type VARCHAR(20) DEFAULT ''full'' COMMENT ''盘点类型（full-全盘，partial-部分盘点）'',
        total_items INT DEFAULT 0 COMMENT ''盘点商品总数'',
        diff_items INT DEFAULT 0 COMMENT ''差异商品数'',
        status VARCHAR(20) DEFAULT ''pending'' COMMENT ''盘点状态（pending-待盘点，checking-盘点中，completed-已完成）'',
        operator VARCHAR(50) COMMENT ''盘点人'',
        checker VARCHAR(50) COMMENT ''审核人'',
        remark TEXT COMMENT ''备注'',
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_check_no (check_no),
        INDEX idx_check_date (check_date),
        INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''库存盘点表''',
    'SELECT "inventory_check table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建库存盘点明细表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'inventory_check_items'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS inventory_check_items (
        id INT PRIMARY KEY AUTO_INCREMENT,
        check_id INT NOT NULL COMMENT ''盘点单ID'',
        product_id INT NOT NULL COMMENT ''商品ID'',
        product_name VARCHAR(100) NOT NULL COMMENT ''商品名称'',
        book_quantity INT NOT NULL COMMENT ''账面数量'',
        actual_quantity INT NOT NULL COMMENT ''实际数量'',
        diff_quantity INT NOT NULL COMMENT ''差异数量'',
        diff_reason TEXT COMMENT ''差异原因'',
        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (check_id) REFERENCES inventory_check(id) ON DELETE CASCADE,
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
        INDEX idx_check (check_id),
        INDEX idx_product (product_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''库存盘点明细表''',
    'SELECT "inventory_check_items table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- v2.4.0 新增表：退货管理模块
-- ============================================

-- 创建退货订单表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'return_orders'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS return_orders (
        id INT AUTO_INCREMENT PRIMARY KEY,
        return_order_id VARCHAR(50) UNIQUE NOT NULL COMMENT ''退货单号'',
        original_transaction_id VARCHAR(50) COMMENT ''原交易ID'',
        member_id INT COMMENT ''会员ID'',
        member_name VARCHAR(100) COMMENT ''会员名称'',
        return_date DATETIME NOT NULL COMMENT ''退货日期'',
        return_reason VARCHAR(500) COMMENT ''退货原因'',
        total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''退货总金额'',
        status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''状态：PENDING、APPROVED、REJECTED、COMPLETED'',
        payment_method VARCHAR(20) COMMENT ''退款方式：CASH、WECHAT、ALIPAY、CARD'',
        operator_name VARCHAR(50) NOT NULL COMMENT ''操作员'',
        approver_name VARCHAR(50) COMMENT ''审批人'',
        approval_date DATETIME COMMENT ''审批日期'',
        approval_comment VARCHAR(500) COMMENT ''审批意见'',
        completed_date DATETIME COMMENT ''完成日期'',
        notes TEXT COMMENT ''备注'',
        create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
        update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
        INDEX idx_return_order_id (return_order_id),
        INDEX idx_status (status),
        INDEX idx_member_id (member_id),
        INDEX idx_return_date (return_date),
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''退货订单表''',
    'SELECT "return_orders table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建退货订单明细表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'return_order_items'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS return_order_items (
        id INT AUTO_INCREMENT PRIMARY KEY,
        return_order_id VARCHAR(50) NOT NULL COMMENT ''退货单号'',
        product_id INT NOT NULL COMMENT ''商品ID'',
        product_code VARCHAR(50) COMMENT ''商品编号'',
        product_name VARCHAR(200) NOT NULL COMMENT ''商品名称'',
        barcode VARCHAR(100) COMMENT ''条形码'',
        category VARCHAR(100) COMMENT ''分类'',
        return_quantity INT NOT NULL DEFAULT 0 COMMENT ''退货数量'',
        unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''单价'',
        return_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''退货金额'',
        reason VARCHAR(500) COMMENT ''退货原因'',
        `condition` VARCHAR(20) NOT NULL DEFAULT ''GOOD'' COMMENT ''商品状态：GOOD、DAMAGED、OPENED'',
        INDEX idx_return_order_id (return_order_id),
        INDEX idx_product_id (product_id),
        FOREIGN KEY (return_order_id) REFERENCES return_orders(return_order_id) ON DELETE CASCADE,
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''退货订单明细表''',
    'SELECT "return_order_items table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- v2.4.0 新增表：导出历史和模板
-- ============================================

-- 创建导出历史表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'export_history'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS export_history (
        id INT AUTO_INCREMENT PRIMARY KEY,
        export_type VARCHAR(50) NOT NULL COMMENT ''导出类型'',
        file_name VARCHAR(255) NOT NULL COMMENT ''文件名'',
        file_path VARCHAR(500) COMMENT ''文件路径'',
        file_size BIGINT COMMENT ''文件大小（字节）'',
        export_format VARCHAR(20) NOT NULL COMMENT ''导出格式：PDF、EXCEL、CSV'',
        record_count INT DEFAULT 0 COMMENT ''导出记录数'',
        user_id VARCHAR(50) COMMENT ''用户ID'',
        username VARCHAR(100) COMMENT ''用户名'',
        export_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''导出时间'',
        export_status VARCHAR(20) DEFAULT ''SUCCESS'' COMMENT ''导出状态：SUCCESS、FAILED'',
        error_message TEXT COMMENT ''错误信息'',
        export_params TEXT COMMENT ''导出参数（JSON）'',
        INDEX idx_export_type (export_type),
        INDEX idx_export_time (export_time),
        INDEX idx_user_id (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''导出历史表''',
    'SELECT "export_history table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建导出模板表
SET @table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'export_templates'
);

SET @sql = IF(@table_exists = 0,
    'CREATE TABLE IF NOT EXISTS export_templates (
        id INT AUTO_INCREMENT PRIMARY KEY,
        template_name VARCHAR(100) NOT NULL COMMENT ''模板名称'',
        template_type VARCHAR(50) NOT NULL COMMENT ''模板类型：TRANSACTION、INVENTORY、MEMBER等'',
        template_format VARCHAR(20) NOT NULL COMMENT ''模板格式：PDF、EXCEL、CSV'',
        template_config TEXT NOT NULL COMMENT ''模板配置（JSON）'',
        is_default BOOLEAN DEFAULT FALSE COMMENT ''是否默认模板'',
        created_by VARCHAR(50) COMMENT ''创建人'',
        create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
        update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
        UNIQUE KEY uk_template_name (template_name),
        INDEX idx_template_type (template_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''导出模板表''',
    'SELECT "export_templates table already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '=== 表结构升级完成 ===' AS status;

-- ============================================
-- 第四部分：示例数据
-- ============================================

-- 清除旧数据（仅清理新增的表，避免影响原有数据）
DELETE FROM return_order_items;
DELETE FROM return_orders;
DELETE FROM export_templates;
DELETE FROM export_history;
DELETE FROM inventory_check_items;
DELETE FROM inventory_check;
DELETE FROM purchase_inbound_items;
DELETE FROM purchase_inbound;
DELETE FROM purchase_approvals;
DELETE FROM purchase_order_items;
DELETE FROM purchase_orders;
DELETE FROM suppliers;

-- v2.3.0-v2.3.1 采购管理模块示例数据

-- 插入示例供应商
INSERT INTO suppliers (supplier_code, name, contact_person, phone, address, `rank`, status, remark) VALUES 
('SUP001', '康师傅宝鸡分公司', '张经理', '0917-88886666', '宝鸡市金台区', 'A', 1, '长期合作供应商'),
('SUP002', '达利园食品', '李经理', '0917-88887777', '西安市雁塔区', 'B', 1, '主要供应商'),
('SUP003', '可口可乐陕西', '王经理', '029-88885555', '西安市未央区', 'A', 1, '饮料供应商');

-- 插入示例采购订单
INSERT INTO purchase_orders (order_no, supplier_id, purchase_date, expected_date, total_amount, status, purchaser, remark) VALUES 
('PO202602100001', 1, '2026-02-10', '2026-02-15', 30.00, 'approved', '张三', '月度采购'),
('PO202602100002', 1, '2026-02-10', '2026-02-15', 45.00, 'approved', '张三', '紧急补货'),
('PO202602100003', 2, '2026-02-10', '2026-02-20', 50.00, 'approved', '李四', '常规采购');

-- 插入示例采购订单明细
INSERT INTO purchase_order_items (order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity) VALUES 
(1, 4, '巧克力棒', 10, 5.00, 50.00, 5),
(2, 4, '巧克力棒', 15, 5.00, 75.00, 10),
(3, 1, '可口可乐 330ml', 20, 3.50, 70.00, 0),
(3, 3, '农夫山泉 550ml', 20, 2.00, 40.00, 0);

-- 插入示例采购审批记录
INSERT INTO purchase_approvals (order_id, approver, action, remark) VALUES 
(1, 'admin', 'approve', '审批通过，价格合理'),
(2, 'admin', 'approve', '审批通过，同意采购'),
(3, 'admin', 'approve', '审批通过');

-- 插入示例采购入库记录
INSERT INTO purchase_inbound (inbound_no, order_id, inbound_date, total_quantity, total_amount, operator, remark) VALUES 
('IB202602100001', 1, '2026-02-10', 5, 25.00, '张三', '部分入库'),
('IB202602100002', 2, '2026-02-10', 10, 50.00, '张三', '部分入库');

-- 插入示例采购入库明细
INSERT INTO purchase_inbound_items (inbound_id, order_item_id, product_id, quantity, unit_price, total_price) VALUES 
(1, 1, 4, 5, 5.00, 25.00),
(2, 2, 4, 10, 5.00, 50.00);

-- v2.3.0-v2.3.1 库存盘点模块示例数据

-- 插入示例库存盘点记录
INSERT INTO inventory_check (check_no, check_date, check_type, total_items, diff_items, status, operator, remark) VALUES 
('IC202602100001', '2026-02-10', 'full', 10, 2, 'completed', '张三', '月度盘点'),
('IC202602100002', '2026-02-10', 'partial', 5, 1, 'pending', '李四', '部分盘点');

-- 插入示例库存盘点明细
INSERT INTO inventory_check_items (check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason) VALUES 
(1, 1, '可口可乐 330ml', 100, 98, -2, '破损2瓶'),
(1, 4, '巧克力棒', 60, 62, 2, '账实不符'),
(2, 1, '可口可乐 330ml', 100, 99, -1, '销售未及时更新');

-- v2.4.0 导出模板示例数据

INSERT INTO export_templates (template_name, template_type, template_format, template_config, is_default, created_by) VALUES
('交易记录默认模板', 'TRANSACTION', 'EXCEL', 
 '{"headers":["交易ID","日期","金额","支付方式","会员"],"fields":["transactionId","transactionTime","totalAmount","paymentMethod","memberName"]}', 
 TRUE, 'system'),
('库存报表默认模板', 'INVENTORY', 'EXCEL', 
 '{"headers":["商品编号","商品名称","分类","库存数量","单价"],"fields":["productCode","productName","category","stock","price"]}', 
 TRUE, 'system'),
('会员列表默认模板', 'MEMBER', 'EXCEL', 
 '{"headers":["会员编号","姓名","手机号","积分","余额"],"fields":["memberCode","name","phone","points","balance"]}', 
 TRUE, 'system')
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

-- 原有示例数据（仅在表为空时插入）
-- 注意：这里使用 INSERT IGNORE 避免重复插入

INSERT IGNORE INTO products (name, price, quantity, category, barcode, unit, description, brand, min_stock, cost) VALUES 
('可口可乐 330ml', 3.50, 100, '饮料', '6902083888888', '瓶', '可口可乐经典口味，330ml罐装', '可口可乐', 10, 2.50),
('百事可乐 330ml', 3.50, 80, '饮料', '6902083888889', '瓶', '百事可乐经典口味，330ml罐装', '百事', 10, 2.50),
('农夫山泉 550ml', 2.00, 150, '饮料', '6902083888890', '瓶', '农夫山泉饮用天然水550ml', '农夫山泉', 20, 1.20),
('薯片原味', 8.00, 50, '零食', '6902083888891', '包', '乐事原味薯片70g', '乐事', 10, 5.00),
('巧克力棒', 5.00, 60, '零食', '6902083888892', '支', '德芙巧克力牛奶味', '德芙', 15, 3.00),
('脉动青柠 500ml', 5.00, 70, '饮料', '6902083888893', '瓶', '脉动青柠味500ml', '达利园', 10, 3.50),
('奥利奥饼干', 6.00, 40, '零食', '6902083888894', '包', '奥利奥原味饼干', '奥利奥', 10, 4.00),
('红牛功能饮料', 6.00, 60, '饮料', '6902083888895', '罐', '红牛功能饮料250ml', '红牛', 15, 4.50),
('旺仔牛奶', 5.00, 80, '零食', '6902083888896', '盒', '旺仔牛奶125ml', '旺旺', 20, 3.00),
('雪碧 330ml', 3.50, 90, '饮料', '69020883888997', '瓶', '雪碧柠檬味330ml', '可口可乐', 10, 2.50);

INSERT IGNORE INTO members (name, phone, birthday, points, balance, discount, level, join_date) VALUES 
('张三', '13800138001', '1990-01-01', 100, 100.00, 9.5, '银卡', 1704163200000),
('李四', '13800138002', '1995-05-15', 500, 200.00, 9.0, '金卡', 1738368000000),
('王五', '13800138003', '1988-08-20', 2000, 500.00, 8.5, '钻石', 1740873600000);

INSERT IGNORE INTO promotions (name, type, discount, start_date, end_date, description) VALUES 
('新年特惠', 'discount', 0.9, 1704067200000, 1740691200000, '全场商品9折优惠'),
('满减活动', 'threshold_discount', 10.0, 1705276800000, 1739606400000, '满100减10元'),
('会员专享', 'percentage', 0.85, 1738368000000, 1741046400000, '会员专属85折');

INSERT IGNORE INTO transactions (transaction_id, timestamp, total_amount, tax, final_amount, payment_method, operator_username, operator_name, transaction_type) VALUES 
('TXN20260204234501', '2026-02-04 23:45:01', 35.50, 0.00, 35.50, '现金', 'admin', '系统管理员', 'sale'),
('TXN20260204234602', '2026-02-04 23:46:02', 12.00, 0.00, 12.00, '微信', 'admin', '系统管理员', 'sale'),
('TXN20260204234703', '2026-02-04 23:47:03', 68.00, 0.00, 68.00, '支付宝', 'admin', '系统管理员', 'sale'),
('TXN20260204234804', '2026-02-04 23:48:04', 25.50, 0.00, 25.50, '现金', 'admin', '系统管理员', 'sale'),
('TXN20260204234905', '2026-02-04 23:49:05', 42.00, 0.00, 42.00, '银行卡', 'admin', '系统管理员', 'sale');

-- ============================================
-- 验证初始化完成
-- ============================================

SELECT '=== 数据库初始化完成 ===' AS status;
SELECT CONCAT('MySQL 版本: ', VERSION()) AS mysql_version;
SELECT COUNT(*) as 商品数量 FROM products;
SELECT COUNT(*) as 会员数量 FROM members;
SELECT COUNT(*) as 促销数量 FROM promotions;
SELECT COUNT(*) as 交易记录数量 FROM transactions;
SELECT COUNT(*) as 供应商数量 FROM suppliers;
SELECT COUNT(*) as 采购订单数量 FROM purchase_orders;
SELECT COUNT(*) as 采购入库数量 FROM purchase_inbound;
SELECT COUNT(*) as 库存盘点数量 FROM inventory_check;
SELECT COUNT(*) as 退货订单数量 FROM return_orders;
SELECT COUNT(*) as 导出模板数量 FROM export_templates;
SELECT COUNT(*) as 数据表总数 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'lisuan_system';

-- ============================================
-- 初始化脚本信息
-- ============================================
SELECT CONCAT('脚本版本: v2.4.3') AS script_version;
SELECT CONCAT('更新日期: 2026-03-06') AS script_date;
SELECT '支持 MySQL 版本: 8.0、8.3、8.4 LTS' AS mysql_compatibility;