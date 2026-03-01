-- ================================================
-- 数据库变更脚本 v2.4.1
-- 版本: 2.4.1
-- 日期: 2026-03-01
-- 说明: 修复交易明细表结构，添加商品ID和编号字段
-- ================================================

-- 1. 修改 transaction_items 表，添加 product_id 和 product_code 字段
ALTER TABLE transaction_items 
ADD COLUMN IF NOT EXISTS product_id INT COMMENT '商品ID',
ADD COLUMN IF NOT EXISTS product_code VARCHAR(50) COMMENT '商品编号',
ADD COLUMN IF NOT EXISTS barcode VARCHAR(100) COMMENT '条形码',
ADD INDEX idx_product_id (product_id);

-- 2. 为已存在的数据填充 product_id（如果可能）
-- 注意：由于历史数据可能无法准确匹配，这里只做示例
-- UPDATE transaction_items ti
-- LEFT JOIN products p ON ti.product_name = p.name AND ti.price = p.price
-- SET ti.product_id = p.id, ti.product_code = p.product_code, ti.barcode = p.barcode
-- WHERE ti.product_id IS NULL;

-- 完成
-- ================================================
-- v2.4.1 数据库变更完成
-- ================================================