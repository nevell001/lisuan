# 采购管理模块数据库表结构设计

## 1. 供应商表 (suppliers)

```sql
CREATE TABLE suppliers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    supplier_code VARCHAR(50) UNIQUE COMMENT '供应商编号',
    name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '地址',
    rank VARCHAR(10) DEFAULT 'C' COMMENT '供应商分级（A级、B级、C级）',
    status TINYINT DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    remark TEXT COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_rank (rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';
```

## 2. 采购订单表 (purchase_orders)

```sql
CREATE TABLE purchase_orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) UNIQUE NOT NULL COMMENT '采购订单号',
    supplier_id INT NOT NULL COMMENT '供应商ID',
    purchase_date DATE NOT NULL COMMENT '采购日期',
    expected_date DATE COMMENT '预计到货日期',
    total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '订单总金额',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '订单状态（pending-待审批，approved-已审批，rejected-已拒绝，completed-已完成）',
    purchaser VARCHAR(50) COMMENT '采购人',
    approver VARCHAR(50) COMMENT '审批人',
    approval_time TIMESTAMP NULL COMMENT '审批时间',
    approval_remark TEXT COMMENT '审批意见',
    remark TEXT COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT,
    INDEX idx_order_no (order_no),
    INDEX idx_supplier (supplier_id),
    INDEX idx_status (status),
    INDEX idx_purchase_date (purchase_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单表';
```

## 3. 采购订单明细表 (purchase_order_items)

```sql
CREATE TABLE purchase_order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL COMMENT '订单ID',
    product_id INT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    quantity INT NOT NULL COMMENT '采购数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_price DECIMAL(10,2) NOT NULL COMMENT '小计',
    inbound_quantity INT DEFAULT 0 COMMENT '已入库数量',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    INDEX idx_order (order_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细表';
```

## 4. 采购审批记录表 (purchase_approvals)

```sql
CREATE TABLE purchase_approvals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL COMMENT '订单ID',
    approver VARCHAR(50) NOT NULL COMMENT '审批人',
    action VARCHAR(20) NOT NULL COMMENT '审批动作（approve-通过，reject-拒绝）',
    remark TEXT COMMENT '审批意见',
    approval_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    INDEX idx_order (order_id),
    INDEX idx_approver (approver)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购审批记录表';
```

## 5. 采购入库记录表 (purchase_inbound)

```sql
CREATE TABLE purchase_inbound (
    id INT PRIMARY KEY AUTO_INCREMENT,
    inbound_no VARCHAR(50) UNIQUE NOT NULL COMMENT '入库单号',
    order_id INT NOT NULL COMMENT '采购订单ID',
    inbound_date DATE NOT NULL COMMENT '入库日期',
    total_quantity INT DEFAULT 0 COMMENT '入库总数量',
    total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '入库总金额',
    operator VARCHAR(50) COMMENT '操作人',
    remark TEXT COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    INDEX idx_inbound_no (inbound_no),
    INDEX idx_order (order_id),
    INDEX idx_inbound_date (inbound_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库记录表';
```

## 6. 采购入库明细表 (purchase_inbound_items)

```sql
CREATE TABLE purchase_inbound_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    inbound_id INT NOT NULL COMMENT '入库单ID',
    order_item_id INT NOT NULL COMMENT '订单明细ID',
    product_id INT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL COMMENT '入库数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_price DECIMAL(10,2) NOT NULL COMMENT '小计',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inbound_id) REFERENCES purchase_inbound(id) ON DELETE CASCADE,
    FOREIGN KEY (order_item_id) REFERENCES purchase_order_items(id) ON DELETE RESTRICT,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    INDEX idx_inbound (inbound_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库明细表';
```

## 7. 库存盘点表 (inventory_check)

```sql
CREATE TABLE inventory_check (
    id INT PRIMARY KEY AUTO_INCREMENT,
    check_no VARCHAR(50) UNIQUE NOT NULL COMMENT '盘点单号',
    check_date DATE NOT NULL COMMENT '盘点日期',
    check_type VARCHAR(20) DEFAULT 'full' COMMENT '盘点类型（full-全盘，partial-部分盘点）',
    total_items INT DEFAULT 0 COMMENT '盘点商品总数',
    diff_items INT DEFAULT 0 COMMENT '差异商品数',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '盘点状态（pending-待盘点，checking-盘点中，completed-已完成）',
    operator VARCHAR(50) COMMENT '盘点人',
    checker VARCHAR(50) COMMENT '审核人',
    remark TEXT COMMENT '备注',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_check_no (check_no),
    INDEX idx_check_date (check_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点表';
```

## 8. 库存盘点明细表 (inventory_check_items)

```sql
CREATE TABLE inventory_check_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    check_id INT NOT NULL COMMENT '盘点单ID',
    product_id INT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    book_quantity INT NOT NULL COMMENT '账面数量',
    actual_quantity INT NOT NULL COMMENT '实际数量',
    diff_quantity INT NOT NULL COMMENT '差异数量',
    diff_reason TEXT COMMENT '差异原因',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (check_id) REFERENCES inventory_check(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    INDEX idx_check (check_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点明细表';
```

## 索引说明

- 所有主键都使用 AUTO_INCREMENT 自增
- 外键约束确保数据完整性
- 为常用查询字段添加索引提高查询性能
- 使用 InnoDB 引擎支持事务
- 字符集使用 utf8mb4 支持中文