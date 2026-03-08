-- 促销表修复脚本
-- ============================================
-- 版本: v2.4.3
-- 更新日期: 2026-03-08
-- 说明: 修复促销表结构，添加 promotion_code 字段，修复日期字段类型问题
--
-- 使用方法: docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < 09-v2.4.3-fix-promotions.sql
-- ============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE cashier_system;

-- ============================================
-- 检查表结构
-- ============================================
SELECT 
    '检查当前表结构' AS step,
    COLUMN_NAME AS 字段名,
    DATA_TYPE AS 数据类型,
    IS_NULLABLE AS 可空,
    COLUMN_DEFAULT AS 默认值
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'cashier_system'
  AND TABLE_NAME = 'promotions'
ORDER BY ORDINAL_POSITION;

-- ============================================
-- 添加 promotion_code 字段（如果不存在）
-- ============================================
-- 检查字段是否已存在
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'cashier_system'
      AND TABLE_NAME = 'promotions'
      AND COLUMN_NAME = 'promotion_code'
);

-- 如果字段不存在，则添加
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE promotions ADD COLUMN promotion_code VARCHAR(50) UNIQUE AFTER id',
    'SELECT ''promotion_code 字段已存在'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 为现有的促销生成编号（如果 promotion_code 为空）
-- ============================================
UPDATE promotions
SET promotion_code = CONCAT('P', LPAD(id, 6, '0'))
WHERE promotion_code IS NULL OR promotion_code = '';

-- ============================================
-- 显示修复后的促销数据
-- ============================================
SELECT 
    '修复后的促销数据' AS step,
    id AS ID,
    promotion_code AS 促销编号,
    name AS 促销名称,
    type AS 类型,
    start_date AS 开始日期,
    end_date AS 结束日期,
    enabled AS 启用状态
FROM promotions
ORDER BY id;

-- ============================================
-- 验证字段是否添加成功
-- ============================================
SELECT 
    '验证修复结果' AS step,
    COLUMN_NAME AS 字段名,
    DATA_TYPE AS 数据类型,
    IS_NULLABLE AS 可空
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'cashier_system'
  AND TABLE_NAME = 'promotions'
  AND COLUMN_NAME = 'promotion_code';

-- ============================================
-- 完成
-- ============================================
SELECT 
    '升级完成' AS step,
    'promotions 表已修复，添加了 promotion_code 字段' AS message;