-- ============================================
-- 收银系统 MySQL 完整初始化脚本
-- ============================================
-- 此脚本整合了用户创建、表结构初始化和示例数据
-- 使用方法: docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < 00-init-complete.sql
-- 
-- 版本: v2.3.1
-- 更新日期: 2026-02-13

-- ============================================
-- 确保使用正确的数据库
-- ============================================
USE cashier_system;

-- ============================================
-- 第一部分：创建专用用户
-- ============================================

-- 1. 创建专用用户（如果不存在）
-- 注意：通过 docker-compose.yml 环境变量创建的用户可能权限不足
-- 这个脚本确保用户有完整的权限

CREATE USER IF NOT EXISTS 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';
CREATE USER IF NOT EXISTS 'cashier'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';

-- 2. 授予所有权限
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'localhost';

-- 3. 刷新权限
FLUSH PRIVILEGES;

-- 4. 显示创建的用户
SELECT '=== MySQL 用户创建完成 ===' AS status;
SELECT user, host FROM mysql.user WHERE user IN ('root', 'cashier');

-- ============================================
-- 第二部分：升级现有表结构
-- ============================================

-- 创建默认管理员用户（如果不存在）
-- 密码: admin123 (明文，首次登录时强制修改密码)
INSERT INTO users (username, password, name, role, active, force_password_change, create_time, last_login_time)
SELECT 'admin', 'admin123', '系统管理员', 'admin', 1, 1, UNIX_TIMESTAMP() * 1000, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- 为 products 表添加 product_code 字段（如果不存在）
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

SELECT '=== 表结构升级完成 ===' AS status;

-- ============================================
-- 第三部分：示例数据
-- ============================================

-- 清除旧数据（仅清理新增的v2.3.0-v2.3.1表，避免影响原有数据）
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
SELECT COUNT(*) as 商品数量 FROM products;
SELECT COUNT(*) as 会员数量 FROM members;
SELECT COUNT(*) as 促销数量 FROM promotions;
SELECT COUNT(*) as 交易记录数量 FROM transactions;
SELECT COUNT(*) as 供应商数量 FROM suppliers;
SELECT COUNT(*) as 采购订单数量 FROM purchase_orders;
SELECT COUNT(*) as 采购入库数量 FROM purchase_inbound;
SELECT COUNT(*) as 库存盘点数量 FROM inventory_check;