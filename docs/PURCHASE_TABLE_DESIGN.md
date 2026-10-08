# 采购管理模块数据库表结构设计

> **本文件说明"设计意图"，不复制 DDL。**
>
> 采购/盘点 8 张表的**唯一 DDL 真源**是 Java 侧建表代码
> （`com.cashier.util.DatabaseManager` 的 `createTableSuppliers` / `createTablePurchaseOrders` /
> `createTablePurchaseOrderItems` / `createTablePurchaseApprovals` / `createTablePurchaseInbound` /
> `createTablePurchaseInboundItems` / `createTableInventoryCheck` / `createTableInventoryCheckItems`），
> 并由 `docker/mysql-init/00-init-complete.sql` 镜像给 Docker 部署，
> 两者的一致性由 `InitSchemaParityTest` 门禁守着。
>
> 此前本文件内嵌了 8 段手写 `CREATE TABLE`，那份副本**不受任何门禁保护**，改了表结构就会静默漂移；
> 现改为"字段说明表 + 指向真源"，需要看确切 DDL/索引/外键时请直接读真源。

## 1. 供应商表 (suppliers)

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `supplier_code` | VARCHAR(50) UNIQUE | 供应商编号 |
| `name` | VARCHAR(100) NOT NULL | 供应商名称 |
| `contact_person` | VARCHAR(50) | 联系人 |
| `phone` | VARCHAR(20) | 联系电话 |
| `address` | VARCHAR(200) | 地址 |
| `rank` | VARCHAR(10) DEFAULT 'C' | 供应商分级（A/B/C） |
| `status` | TINYINT DEFAULT 1 | 状态（1-启用，0-禁用） |
| `remark` | TEXT | 备注 |
| `create_time` / `update_time` | TIMESTAMP | 创建/更新时间（`update_time` 自动更新） |

索引：`idx_name(name)`、`idx_rank(rank)`。

## 2. 采购订单表 (purchase_orders)

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `order_no` | VARCHAR(50) UNIQUE NOT NULL | 采购订单号 |
| `supplier_id` | INT NOT NULL | 供应商 ID（FK → `suppliers.id`，`ON DELETE RESTRICT`） |
| `purchase_date` | DATE NOT NULL | 采购日期 |
| `expected_date` | DATE | 预计到货日期 |
| `total_amount` | DECIMAL(10,2) DEFAULT 0.00 | 订单总金额 |
| `status` | VARCHAR(20) DEFAULT 'pending' | pending-待审批 / approved-已审批 / rejected-已拒绝 / completed-已完成 |
| `purchaser` | VARCHAR(50) | 采购人 |
| `approver` | VARCHAR(50) | 审批人 |
| `approval_time` | TIMESTAMP NULL | 审批时间 |
| `approval_remark` | TEXT | 审批意见 |
| `remark` | TEXT | 备注 |
| `create_time` / `update_time` | TIMESTAMP | 创建/更新时间 |

索引：`idx_order_no`、`idx_supplier`、`idx_status`、`idx_purchase_date`。

## 3. 采购订单明细表 (purchase_order_items)

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `order_id` | INT NOT NULL | 订单 ID（FK → `purchase_orders.id`，`ON DELETE CASCADE`） |
| `product_id` | INT NOT NULL | 商品 ID（FK → `products.id`，`ON DELETE RESTRICT`） |
| `product_name` | VARCHAR(100) NOT NULL | 商品名称（下单时快照） |
| `quantity` | INT NOT NULL | 采购数量 |
| `unit_price` | DECIMAL(10,2) NOT NULL | 单价 |
| `total_price` | DECIMAL(10,2) NOT NULL | 小计 |
| `inbound_quantity` | INT DEFAULT 0 | 已入库数量（用于"部分入库"进度） |
| `create_time` | TIMESTAMP | 创建时间 |

索引：`idx_order(order_id)`、`idx_product(product_id)`。

## 4. 采购审批记录表 (purchase_approvals)

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `order_id` | INT NOT NULL | 订单 ID（FK → `purchase_orders.id`，`ON DELETE CASCADE`） |
| `approver` | VARCHAR(50) NOT NULL | 审批人 |
| `action` | VARCHAR(20) NOT NULL | 审批动作（approve-通过 / reject-拒绝） |
| `remark` | TEXT | 审批意见 |
| `approval_time` | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | 审批时间 |

索引：`idx_order(order_id)`、`idx_approver(approver)`。

> 说明：`purchase_orders` 上的 `approver`/`approval_time`/`approval_remark` 是**最新一次**审批结果，
> 本表保留**每一次**审批动作的历史（可多次审批）。

## 5. 采购入库记录表 (purchase_inbound)

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `inbound_no` | VARCHAR(50) UNIQUE NOT NULL | 入库单号 |
| `order_id` | INT NOT NULL | 采购订单 ID（FK → `purchase_orders.id`，`ON DELETE RESTRICT`） |
| `inbound_date` | DATE NOT NULL | 入库日期 |
| `total_quantity` | INT DEFAULT 0 | 入库总数量 |
| `total_amount` | DECIMAL(10,2) DEFAULT 0.00 | 入库总金额 |
| `operator` | VARCHAR(50) | 操作人 |
| `remark` | TEXT | 备注 |
| `create_time` | TIMESTAMP | 创建时间 |

索引：`idx_inbound_no`、`idx_order`、`idx_inbound_date`。

## 6. 采购入库明细表 (purchase_inbound_items)

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `inbound_id` | INT NOT NULL | 入库单 ID（FK → `purchase_inbound.id`，`ON DELETE CASCADE`） |
| `order_item_id` | INT NOT NULL | 订单明细 ID（FK → `purchase_order_items.id`，`ON DELETE RESTRICT`） |
| `product_id` | INT NOT NULL | 商品 ID（FK → `products.id`，`ON DELETE RESTRICT`） |
| `quantity` | INT NOT NULL | 入库数量 |
| `unit_price` | DECIMAL(10,2) NOT NULL | 单价 |
| `total_price` | DECIMAL(10,2) NOT NULL | 小计 |
| `create_time` | TIMESTAMP | 创建时间 |

索引：`idx_inbound(inbound_id)`、`idx_product(product_id)`。

> 明细挂 `order_item_id` 而不是只挂 `product_id`：同一商品在同一订单里可能有多行（不同单价），
> 入库必须能对回**具体那一行**。

## 7. 库存盘点表 (inventory_check)

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `check_no` | VARCHAR(50) UNIQUE NOT NULL | 盘点单号 |
| `check_date` | DATE NOT NULL | 盘点日期 |
| `check_type` | VARCHAR(20) DEFAULT 'full' | full-全盘 / partial-部分盘点 |
| `total_items` | INT DEFAULT 0 | 盘点商品总数 |
| `diff_items` | INT DEFAULT 0 | 差异商品数 |
| `status` | VARCHAR(20) DEFAULT 'pending' | pending-待盘点 / checking-盘点中 / completed-已完成 |
| `operator` | VARCHAR(50) | 盘点人 |
| `checker` | VARCHAR(50) | 审核人 |
| `remark` | TEXT | 备注 |
| `create_time` / `update_time` | TIMESTAMP | 创建/更新时间 |

索引：`idx_check_no`、`idx_check_date`、`idx_status`。

## 8. 库存盘点明细表 (inventory_check_items)

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `check_id` | INT NOT NULL | 盘点单 ID（FK → `inventory_check.id`，`ON DELETE CASCADE`） |
| `product_id` | INT NOT NULL | 商品 ID（FK → `products.id`，`ON DELETE RESTRICT`） |
| `product_name` | VARCHAR(100) NOT NULL | 商品名称 |
| `book_quantity` | INT NOT NULL | 账面数量 |
| `actual_quantity` | INT NOT NULL | 实际数量 |
| `diff_quantity` | INT NOT NULL | 差异数量 |
| `diff_reason` | TEXT | 差异原因 |
| `create_time` | TIMESTAMP | 创建时间 |

索引：`idx_check(check_id)`、`idx_product(product_id)`。

> 盘点单的保存/删除必须走 `*WithConnection` + `executeBooleanTransaction`：
> "先删全部旧明细再逐条插"若中途失败，会留下"已提交的 DELETE + 半截明细"，而表头仍写着 N 条
> （见 `TECH_DEBT.md` 的 TD-023 与 `WriteAtomicityPolicyTest`）。

## 索引与约定

- 所有主键都使用 `AUTO_INCREMENT` 自增
- 外键约束确保数据完整性（子表 `ON DELETE CASCADE`、被引用的主数据 `ON DELETE RESTRICT`）
- 为常用查询字段添加索引提高查询性能
- 使用 InnoDB 引擎支持事务
- 字符集使用 utf8mb4 支持中文
- **相对增减库存必须递增 `version`**（`updateQuantityWithConnection` 里 `version = version + 1`），
  否则并发结账会拿旧 version 命中并把刚还回的库存覆盖掉（见 TD-024）