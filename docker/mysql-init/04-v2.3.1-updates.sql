-- ============================================
-- 收银系统 MySQL 数据库升级脚本 v2.3.1
-- ============================================
-- 版本: v2.3.1
-- 更新日期: 2026-02-13
-- 说明: 此脚本用于升级现有数据库到 v2.3.1 版本
-- 运行方法: mysql -u root -p cashier_system < 04-v2.3.1-updates.sql
-- 
-- v2.3.1 主要更新:
-- 1. 移除 products 表 barcode 字段的 UNIQUE 约束
-- 2. 确保 members 表包含 member_code 字段
-- 3. 优化商品管理流程，通过进销存管理库存
-- ============================================

-- ============================================
-- 第1部分：移除 barcode 字段的 UNIQUE 约束
-- ============================================

-- 1.1 移除 products 表 barcode 字段的 UNIQUE 约束（如果存在）
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

-- 1.2 确保 barcode 索引存在（普通索引，用于快速查询）
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

-- ============================================
-- 第2部分：确保 members 表包含 member_code 字段
-- ============================================

-- 2.1 添加 member_code 字段（如果不存在）
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

-- 2.2 为现有会员生成会员编号（如果 member_code 为空）
SET @sql = IF(@column_exists = 0,
    'UPDATE members SET member_code = CONCAT(''M'', LPAD(id, 6, ''0'')) WHERE member_code IS NULL OR member_code = ''''',
    'SELECT "No members need member_code generation" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.3 添加 member_code 索引（如果不存在）
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

SELECT '=== member_code 字段已添加 ===' AS status;

-- ============================================
-- 验证升级结果
-- ============================================

SELECT '=== v2.3.1 数据库升级完成 ===' AS status;

-- 验证 products 表结构
SELECT 
    'products' AS table_name,
    COLUMN_NAME AS column_name,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'products'
AND COLUMN_NAME IN ('barcode', 'product_code')
ORDER BY ORDINAL_POSITION;

-- 验证 members 表结构
SELECT 
    'members' AS table_name,
    COLUMN_NAME AS column_name,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'members'
AND COLUMN_NAME IN ('member_code', 'id', 'phone')
ORDER BY ORDINAL_POSITION;

-- 验证索引
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    NON_UNIQUE,
    COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME IN ('products', 'members')
AND INDEX_NAME IN ('idx_barcode', 'idx_member_code', 'barcode', 'product_code')
ORDER BY TABLE_NAME, INDEX_NAME;

SELECT '=== 升级验证完成 ===' AS status;