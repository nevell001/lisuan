-- ============================================
-- 收银系统 MySQL 表结构升级脚本
-- ============================================
-- 此脚本用于为现有表添加 id 字段
-- 运行此脚本前请确保已备份数据库

-- 1. 为 members 表添加 id 字段（如果不存在）
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

-- 2. 为 categories 表添加 id 字段（如果不存在）
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

-- 3. 为 units 表添加 id 字段（如果不存在）
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

-- 4. 创建主题偏好表（如果不存在）
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

-- ============================================
-- v2.3.0-v2.3.1 新增表：采购管理模块
-- ============================================

-- 5. 创建供应商表
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

-- 6. 创建采购订单表
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

-- 7. 创建采购订单明细表
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

-- 8. 创建采购审批记录表
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

-- 9. 创建采购入库记录表
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

-- 10. 创建采购入库明细表
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

-- 11. 创建库存盘点表
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

-- 12. 创建库存盘点明细表
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
-- v2.3.0-v2.3.1 更新：添加会员编号字段
-- ============================================

-- 13. 为 members 表添加 member_code 字段（如果不存在）
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

SELECT '=== 表结构升级完成 ===' AS status;

-- ============================================
-- v2.3.1 更新：移除 barcode 字段的唯一约束
-- ============================================

-- 14. 移除 products 表 barcode 字段的 UNIQUE 约束（如果存在）
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

-- 确保 barcode 索引存在（普通索引）
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

SELECT '=== barcode 字段唯一约束已移除 ===' AS status;