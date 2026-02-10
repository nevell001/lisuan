-- ============================================
-- 收银系统示例数据
-- ============================================
-- 此脚本用于创建示例商品、会员和交易记录
-- 使用方法: docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < 03-sample-data.sql

-- 清除旧数据
DELETE FROM inventory_check_items;
DELETE FROM inventory_check;
DELETE FROM purchase_inbound_items;
DELETE FROM purchase_inbound;
DELETE FROM purchase_approvals;
DELETE FROM purchase_order_items;
DELETE FROM purchase_orders;
DELETE FROM suppliers;
DELETE FROM products;
DELETE FROM transactions;
DELETE FROM members;
DELETE FROM promotions;

-- ============================================
-- v2.3.0 新增：采购管理模块示例数据
-- ============================================

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

-- ============================================
-- v2.3.0 新增：库存盘点模块示例数据
-- ============================================

-- 插入示例库存盘点记录
INSERT INTO inventory_check (check_no, check_date, check_type, total_items, diff_items, status, operator, remark) VALUES 
('IC202602100001', '2026-02-10', 'full', 10, 2, 'completed', '张三', '月度盘点'),
('IC202602100002', '2026-02-10', 'partial', 5, 1, 'pending', '李四', '部分盘点');

-- 插入示例库存盘点明细
INSERT INTO inventory_check_items (check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason) VALUES 
(1, 1, '可口可乐 330ml', 100, 98, -2, '破损2瓶'),
(1, 4, '巧克力棒', 60, 62, 2, '账实不符'),
(2, 1, '可口可乐 330ml', 100, 99, -1, '销售未及时更新');

-- ============================================
-- 示例商品、会员、交易记录
-- ============================================

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
SELECT COUNT(*) as 供应商数量 FROM suppliers;
SELECT COUNT(*) as 采购订单数量 FROM purchase_orders;
SELECT COUNT(*) as 采购入库数量 FROM purchase_inbound;
SELECT COUNT(*) as 库存盘点数量 FROM inventory_check;