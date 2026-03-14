-- 商品规格管理功能
-- 版本：v2.4.5
-- 日期：2026-03-08
-- 说明：添加商品规格类型、规格值、商品规格关联表

-- 创建商品规格类型表
CREATE TABLE IF NOT EXISTS specifications (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '规格类型ID',
    name VARCHAR(100) NOT NULL COMMENT '规格名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '规格代码',
    type VARCHAR(20) NOT NULL COMMENT '规格类型：COLOR-颜色，SIZE-尺寸，MATERIAL-材质，OTHER-其他',
    description TEXT COMMENT '规格描述',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_type (type),
    INDEX idx_enabled (enabled),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格类型表';

-- 创建商品规格值表
CREATE TABLE IF NOT EXISTS specification_values (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '规格值ID',
    specification_id INT NOT NULL COMMENT '规格类型ID',
    value VARCHAR(100) NOT NULL COMMENT '规格值',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '规格值代码',
    color_code VARCHAR(20) COMMENT '颜色代码（用于颜色规格）',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (specification_id) REFERENCES specifications(id) ON DELETE CASCADE,
    INDEX idx_specification_id (specification_id),
    INDEX idx_enabled (enabled),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格值表';

-- 创建商品规格关联表
CREATE TABLE IF NOT EXISTS product_specifications (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    product_id INT NOT NULL COMMENT '商品ID',
    specification_id INT NOT NULL COMMENT '规格类型ID',
    specification_value_id INT NOT NULL COMMENT '规格值ID',
    sku_code VARCHAR(50) UNIQUE COMMENT 'SKU编码',
    price_adjustment DECIMAL(10,2) DEFAULT 0.00 COMMENT '价格调整值',
    quantity INT DEFAULT 0 COMMENT '库存数量',
    barcode VARCHAR(50) COMMENT '条形码',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (specification_id) REFERENCES specifications(id) ON DELETE CASCADE,
    FOREIGN KEY (specification_value_id) REFERENCES specification_values(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id),
    INDEX idx_specification_id (specification_id),
    INDEX idx_specification_value_id (specification_value_id),
    INDEX idx_sku_code (sku_code),
    INDEX idx_barcode (barcode),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格关联表';

-- 插入默认规格类型数据
INSERT INTO specifications (name, code, type, description, sort_order) VALUES
('颜色', 'COLOR', 'COLOR', '商品颜色规格', 1),
('尺寸', 'SIZE', 'SIZE', '商品尺寸规格', 2),
('材质', 'MATERIAL', 'MATERIAL', '商品材质规格', 3);

-- 插入默认颜色规格值数据
INSERT INTO specification_values (specification_id, value, code, color_code, sort_order) VALUES
(1, '红色', 'RED', '#FF0000', 1),
(1, '蓝色', 'BLUE', '#0000FF', 2),
(1, '绿色', 'GREEN', '#00FF00', 3),
(1, '黄色', 'YELLOW', '#FFFF00', 4),
(1, '黑色', 'BLACK', '#000000', 5),
(1, '白色', 'WHITE', '#FFFFFF', 6);

-- 插入默认尺寸规格值数据
INSERT INTO specification_values (specification_id, value, code, sort_order) VALUES
(2, 'XS', 'XS', 1),
(2, 'S', 'S', 2),
(2, 'M', 'M', 3),
(2, 'L', 'L', 4),
(2, 'XL', 'XL', 5),
(2, 'XXL', 'XXL', 6);

-- 插入默认材质规格值数据
INSERT INTO specification_values (specification_id, value, code, sort_order) VALUES
(3, '棉质', 'COTTON', 1),
(3, '涤纶', 'POLYESTER', 2),
(3, '丝绸', 'SILK', 3),
(3, '羊毛', 'WOOL', 4),
(3, '麻质', 'LINEN', 5);

-- 创建唯一索引，防止同一商品重复关联同一规格值
CREATE UNIQUE INDEX idx_product_spec_value ON product_specifications(product_id, specification_id, specification_value_id);