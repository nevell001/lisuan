-- 商品名称唯一性约束升级脚本
-- ============================================
-- 版本: v2.4.3
-- 更新日期: 2026-03-07
-- 说明: 为 products 表的 name 字段添加 UNIQUE 约束
--
-- 使用方法: docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < 08-v2.4.3-product-name-unique.sql
--
-- 注意事项:
-- 1. 执行此脚本前，请先检查并处理重复的商品名称
-- 2. 如果存在重复名称，需要先合并或重命名重复的商品
-- 3. 建议在业务低峰期执行
-- ============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE cashier_system;

-- ============================================
-- 检查是否有重复的商品名称
-- ============================================
SELECT 
    '检查重复商品名称' AS step,
    COUNT(*) AS duplicate_count
FROM (
    SELECT name, COUNT(*) AS cnt
    FROM products
    GROUP BY name
    HAVING cnt > 1
) AS duplicates;

-- ============================================
-- 显示重复的商品名称列表
-- ============================================
SELECT 
    '重复商品名称列表' AS step,
    name AS 商品名称,
    COUNT(*) AS 重复数量,
    GROUP_CONCAT(id) AS 商品ID列表,
    GROUP_CONCAT(barcode) AS 条形码列表
FROM products
GROUP BY name
HAVING COUNT(*) > 1;

-- ============================================
-- 添加 UNIQUE 约束
-- ============================================
-- 注意: 如果存在重复名称，此语句会失败
-- 需要先手动处理重复数据
ALTER TABLE products 
ADD CONSTRAINT uk_product_name UNIQUE (name);

-- ============================================
-- 验证约束是否添加成功
-- ============================================
SELECT 
    '验证约束' AS step,
    CONSTRAINT_NAME AS 约束名称,
    COLUMN_NAME AS 字段名称
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'cashier_system'
  AND TABLE_NAME = 'products'
  AND CONSTRAINT_NAME = 'uk_product_name';

-- ============================================
-- 完成
-- ============================================
SELECT 
    '升级完成' AS step,
    'products 表的 name 字段已添加 UNIQUE 约束' AS message;