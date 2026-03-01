-- ================================================
-- 检查和修复交易明细中的重复商品记录
-- 版本: v2.4.1
-- 日期: 2026-03-01
-- 说明: 检查并修复同一交易中相同商品的重复记录
-- ================================================

-- 1. 检查是否有交易包含重复商品（按商品名称）
SELECT 
    transaction_id,
    product_name,
    COUNT(*) as duplicate_count,
    SUM(quantity) as total_quantity,
    GROUP_CONCAT(id ORDER BY id) as item_ids
FROM transaction_items
GROUP BY transaction_id, product_name
HAVING COUNT(*) > 1
ORDER BY transaction_id, product_name;

-- 2. 显示有问题的交易记录详情
SELECT 
    t.transaction_id,
    t.timestamp,
    t.final_amount,
    COUNT(ti.id) as total_items,
    COUNT(DISTINCT ti.product_name) as unique_products
FROM transactions t
LEFT JOIN transaction_items ti ON t.transaction_id = ti.transaction_id
GROUP BY t.transaction_id
HAVING COUNT(ti.id) > COUNT(DISTINCT ti.product_name)
ORDER BY t.transaction_id;

-- 3. 统计受影响的交易数量
SELECT 
    COUNT(DISTINCT transaction_id) as affected_transactions,
    COUNT(*) as duplicate_items
FROM (
    SELECT 
        transaction_id,
        product_name,
        COUNT(*) as cnt
    FROM transaction_items
    GROUP BY transaction_id, product_name
    HAVING COUNT(*) > 1
) as duplicates;

-- ================================================
-- 修复脚本（谨慎使用，请在备份后执行）
-- ================================================

-- 注意：修复步骤
-- 1. 备份数据库
-- 2. 对每个有重复的交易，删除重复的记录，保留一个并累加数量
-- 3. 更新 subtotal 字段

-- 示例修复逻辑（需要针对每个交易执行）：
-- DELETE FROM transaction_items 
-- WHERE id IN (
--     SELECT id FROM (
--         SELECT id, ROW_NUMBER() OVER (
--             PARTITION BY transaction_id, product_name 
--             ORDER BY id
--         ) as rn
--         FROM transaction_items
--         WHERE transaction_id = '特定交易ID' AND product_name = '特定商品名称'
--     ) t WHERE rn > 1
-- );

-- ================================================
-- 建议
-- ================================================
-- 1. 对于新交易，代码已经修复（CheckoutController.java）
-- 2. 对于历史交易，建议手动检查并修复
-- 3. 或者接受历史数据保持原样，只确保新交易正确
-- ================================================