-- ================================================
-- 数据库变更脚本 v2.4.0
-- 版本: 2.4.0
-- 日期: 2026-03-01
-- 说明: 添加退货管理功能、数据导出增强、操作日志增强
-- ================================================

-- 1. 创建退货订单表
CREATE TABLE IF NOT EXISTS return_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    return_order_id VARCHAR(50) UNIQUE NOT NULL COMMENT '退货单号',
    original_transaction_id VARCHAR(50) COMMENT '原交易ID',
    member_id INT COMMENT '会员ID',
    member_name VARCHAR(100) COMMENT '会员名称',
    return_date DATETIME NOT NULL COMMENT '退货日期',
    return_reason VARCHAR(500) COMMENT '退货原因',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '退货总金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING、APPROVED、REJECTED、COMPLETED',
    payment_method VARCHAR(20) COMMENT '退款方式：CASH、WECHAT、ALIPAY、CARD',
    operator_name VARCHAR(50) NOT NULL COMMENT '操作员',
    approver_name VARCHAR(50) COMMENT '审批人',
    approval_date DATETIME COMMENT '审批日期',
    approval_comment VARCHAR(500) COMMENT '审批意见',
    completed_date DATETIME COMMENT '完成日期',
    notes TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_return_order_id (return_order_id),
    INDEX idx_status (status),
    INDEX idx_member_id (member_id),
    INDEX idx_return_date (return_date),
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货订单表';

-- 2. 创建退货订单明细表
CREATE TABLE IF NOT EXISTS return_order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    return_order_id VARCHAR(50) NOT NULL COMMENT '退货单号',
    product_id INT NOT NULL COMMENT '商品ID',
    product_code VARCHAR(50) COMMENT '商品编号',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    barcode VARCHAR(100) COMMENT '条形码',
    category VARCHAR(100) COMMENT '分类',
    return_quantity INT NOT NULL DEFAULT 0 COMMENT '退货数量',
    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    return_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '退货金额',
    reason VARCHAR(500) COMMENT '退货原因',
    `condition` VARCHAR(20) NOT NULL DEFAULT 'GOOD' COMMENT '商品状态：GOOD、DAMAGED、OPENED',
    INDEX idx_return_order_id (return_order_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (return_order_id) REFERENCES return_orders(return_order_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货订单明细表';

-- 3. 扩展操作日志表
ALTER TABLE operation_logs 
ADD COLUMN IF NOT EXISTS log_level VARCHAR(20) DEFAULT 'INFO' COMMENT '日志级别：DEBUG、INFO、WARN、ERROR',
ADD COLUMN IF NOT EXISTS log_category VARCHAR(50) DEFAULT 'USER' COMMENT '日志分类：USER、SYSTEM、EXCEPTION',
ADD COLUMN IF NOT EXISTS operation_result VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '操作结果：SUCCESS、FAILURE',
ADD COLUMN IF NOT EXISTS affected_records INT COMMENT '影响的记录数',
ADD COLUMN IF NOT EXISTS request_data TEXT COMMENT '请求数据（JSON）',
ADD COLUMN IF NOT EXISTS response_data TEXT COMMENT '响应数据（JSON）';

-- 4. 创建导出历史表
CREATE TABLE IF NOT EXISTS export_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    export_type VARCHAR(50) NOT NULL COMMENT '导出类型',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小（字节）',
    export_format VARCHAR(20) NOT NULL COMMENT '导出格式：PDF、EXCEL、CSV',
    record_count INT DEFAULT 0 COMMENT '导出记录数',
    user_id VARCHAR(50) COMMENT '用户ID',
    username VARCHAR(100) COMMENT '用户名',
    export_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导出时间',
    export_status VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '导出状态：SUCCESS、FAILED',
    error_message TEXT COMMENT '错误信息',
    export_params TEXT COMMENT '导出参数（JSON）',
    INDEX idx_export_type (export_type),
    INDEX idx_export_time (export_time),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出历史表';

-- 5. 创建导出模板表
CREATE TABLE IF NOT EXISTS export_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(50) NOT NULL COMMENT '模板类型：TRANSACTION、INVENTORY、MEMBER等',
    template_format VARCHAR(20) NOT NULL COMMENT '模板格式：PDF、EXCEL、CSV',
    template_config TEXT NOT NULL COMMENT '模板配置（JSON）',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认模板',
    created_by VARCHAR(50) COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_template_name (template_name),
    INDEX idx_template_type (template_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出模板表';

-- 6. 添加默认导出模板
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

-- 完成
-- ================================================
-- v2.4.0 数据库变更完成
-- ================================================