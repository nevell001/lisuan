-- ============================================
-- 收银系统示例数据
-- ============================================
-- 此脚本用于创建示例商品、会员和交易记录
-- 使用方法: docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < 03-sample-data.sql

-- 清除旧数据
DELETE FROM products;
DELETE FROM transactions;
DELETE FROM members;
DELETE FROM promotions;

-- 插入示例商品
INSERT INTO products (name, price, quantity, category, barcode, unit, description, brand, min_stock, cost) VALUES 
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

-- 插入示例会员
INSERT INTO members (name, phone, birthday, points, balance, discount, level, join_date) VALUES 
('张三', '13800138001', '1990-01-01', 100, 100.00, 9.5, '银卡', 1704163200000),
('李四', '13800138002', '1995-05-15', 500, 200.00, 9.0, '金卡', 1738368000000),
('王五', '13800138003', '1988-08-20', 2000, 500.00, 8.5, '钻石', 1740873600000);

-- 插入示例促销
INSERT INTO promotions (name, type, discount, start_date, end_date, description) VALUES 
('新年特惠', 'discount', 0.9, 1704067200000, 1740691200000, '全场商品9折优惠'),
('满减活动', 'threshold_discount', 10.0, 1705276800000, 1739606400000, '满100减10元'),
('会员专享', 'percentage', 0.85, 1738368000000, 1741046400000, '会员专属85折');

-- 插入示例交易记录
INSERT INTO transactions (transaction_id, timestamp, total_amount, tax, final_amount, payment_method, operator_username, operator_name, transaction_type) VALUES 
('TXN20260204234501', '2026-02-04 23:45:01', 35.50, 0.00, 35.50, '现金', 'admin', '系统管理员', 'sale'),
('TXN20260204234602', '2026-02-04 23:46:02', 12.00, 0.00, 12.00, '微信', 'admin', '系统管理员', 'sale'),
('TXN20260204234703', '2026-02-04 23:47:03', 68.00, 0.00, 68.00, '支付宝', 'admin', '系统管理员', 'sale'),
('TXN20260204234804', '2026-02-04 23:48:04', 25.50, 0.00, 25.50, '现金', 'admin', '系统管理员', 'sale'),
('TXN20260204234905', '2026-02-04 23:49:05', 42.00, 0.00, 42.00, '银行卡', 'admin', '系统管理员', 'sale');

-- 验证数据插入
SELECT '=== 示例数据创建完成 ===' AS status;
SELECT COUNT(*) as 商品数量 FROM products;
SELECT COUNT(*) as 会员数量 FROM members;
SELECT COUNT(*) as 促销数量 FROM promotions;
SELECT COUNT(*) as 交易记录数量 FROM transactions;