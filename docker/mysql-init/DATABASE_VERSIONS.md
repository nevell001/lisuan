# 数据库版本管理

## 版本历史

| 版本 | 脚本文件 | 说明 | 发布日期 | 状态 |
|------|----------|------|----------|------|
| v2.4.3 | 00-init-complete.sql | 商品名称唯一性约束 + MySQL 8.4 LTS 兼容性 | 2026-03-07 | ✅ 已发布 |
| v2.4.3 | 08-v2.4.3-product-name-unique.sql | 商品名称唯一性约束升级脚本 | 2026-03-07 | ✅ 已发布 |
| v2.4.2 | 00-init-complete.sql | 完整初始化脚本（整合所有功能） | 2026-03-05 | ✅ 已发布 |
| v2.4.1 | 00-init-complete.sql | 完整初始化脚本（整合所有功能） | 2026-03-01 | ✅ 已发布 |
| v2.4.1 | 06-v2.4.1-updates.sql | 添加商品ID和编号字段 | 2026-03-01 | ✅ 已发布 |
| v2.4.0 | 05-v2.4.0-updates.sql | 退货管理、导出历史 | 2026-02-29 | ✅ 已发布 |
| v2.3.1 | 04-v2.3.1-updates.sql | 退货管理功能 | 2026-02-13 | ✅ 已发布 |
| v2.3.0 | 02-alter-tables.sql | 进销存管理 | 2026-02-07 | ⚠️ 已整合（已删除） |
| v2.2.0 | 01-create-user.sql | MySQL迁移 | 2026-02-03 | ⚠️ 已整合（已删除） |
| - | 03-sample-data.sql | 示例数据 | - | ⚠️ 已整合（已删除） |

> **注意**：v2.3.0 及之前的独立脚本（01/02/03）已被 `00-init-complete.sql` 完全整合，已从目录中删除以避免混淆。

## 📋 使用指南

### 场景一：全新安装 / 完全重建数据库

**推荐使用**：`00-init-complete.sql`

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < docker/mysql-init/00-init-complete.sql
```

**特点**：
- ✅ 包含所有功能的完整初始化
- ✅ 创建所有必要的表结构
- ✅ 插入示例数据
- ✅ 适用于 Docker Compose 首次启动
- ✅ 适用于 install.sh 安装脚本
- ✅ 兼容 MySQL 8.0、8.3、8.4 LTS

**注意**：此脚本会重置所有数据，请勿在生产环境直接使用。

---

### 场景二：版本间增量升级

**从 v2.4.2 升级到 v2.4.3**

> **重要**：v2.4.3 版本添加了商品名称唯一性约束，升级前必须检查并处理重复的商品名称。

**步骤 1：检查重复的商品名称**
```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < docker/mysql-init/08-v2.4.3-product-name-unique.sql
```

脚本会显示重复的商品名称列表，请根据实际情况进行处理：
- 如果重复的商品是同一个商品的不同条形码版本，请合并为一个商品
- 如果确实是不同的商品，请重命名其中一个商品的名称

**步骤 2：添加 UNIQUE 约束**
```sql
-- 手动执行（在处理完重复名称后）
ALTER TABLE products ADD CONSTRAINT uk_product_name UNIQUE (name);
```

**MySQL 8.4 兼容性**：
> - 如果使用 MySQL 8.0 或 8.3，应用层面已添加名称唯一性检查，无需数据库约束
> - 如果升级到 MySQL 8.4，请确保 docker-compose.yml 使用正确的启动参数

**MySQL 8.4 升级步骤**：

1. 更新 `docker-compose.yml`：
```yaml
image: mysql:8.4
command: --mysql-native-password=ON --bind-address=0.0.0.0 --skip-name-resolve
```

2. 重新初始化数据库（如需要）：
```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < docker/mysql-init/00-init-complete.sql
```

**从 v2.4.1 升级到 v2.4.2**

> **说明**：v2.4.2 版本无数据库结构变更，无需执行数据库升级脚本。直接更新应用代码即可。

**从 v2.4.0 升级到 v2.4.1**

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql
```

**从 v2.3.1 升级到 v2.4.1**

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/05-v2.4.0-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql
```

**从 v2.3.0 升级到 v2.4.1**

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/04-v2.3.1-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/05-v2.4.0-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql
```

**特点**：
- ✅ 保留现有数据
- ✅ 只添加缺失的字段和表
- ✅ 支持幂等操作（可重复执行）
- ✅ 适用于生产环境升级

---

### 场景三：诊断和修复历史数据

**检查交易明细重复记录**

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/07-fix-transaction-items.sql
```

**特点**：
- ⚠️ 仅用于诊断，不会自动修复
- ⚠️ 需要手动检查结果后决定是否修复
- ⚠️ 建议先备份数据库

## 可选脚本

### 修复历史数据

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/07-fix-transaction-items.sql
```

注意：此脚本仅用于检查和诊断，不会自动修复数据。

## 🔍 检查数据库版本

```sql
-- 检查 transaction_items 表是否包含 v2.4.1 新字段
DESC transaction_items;

-- 检查是否包含 product_id、product_code、barcode 字段
SHOW CREATE TABLE transaction_items;

-- 检查所有表
SHOW TABLES;
```

**v2.4.1 完整版本应包含的表**：
- ✅ `transaction_items` 包含 `product_id`、`product_code`、`barcode` 字段
- ✅ `return_orders`、`return_order_items`（退货管理）
- ✅ `export_history`、`export_templates`（数据导出）
- ✅ `suppliers`、`purchase_orders`...（采购管理）
- ✅ `inventory_check`、`inventory_check_items`（库存盘点）

---

## 🛠️ 脚本编写规范

### 1. 命名规范

| 文件类型 | 命名格式 | 示例 |
|---------|---------|------|
| 完整初始化 | `00-init-complete.sql` | - |
| Root 权限配置 | `00-grant-root-permissions.sql` | - |
| 版本升级 | `v{version}-updates.sql` | `06-v2.4.1-updates.sql` |
| 诊断修复 | `07-fix-{description}.sql` | `07-fix-transaction-items.sql` |

### 2. 脚本结构模板

```sql
-- ================================================
-- 数据库变更脚本 v{version}
-- 版本: {version}
-- 日期: {date}
-- 说明: {description}
-- ================================================

-- 1. 变更1（使用 IF NOT EXISTS 确保幂等性）
ALTER TABLE table_name ADD COLUMN IF NOT EXISTS column_name;

-- 2. 变更2
CREATE TABLE IF NOT EXISTS table_name (...);

-- 完成
SELECT '=== v{version} 数据库变更完成 ===' AS status;
```

### 3. 幂等性要求

**必须保证脚本可重复执行**：
- ✅ 使用 `CREATE TABLE IF NOT EXISTS`
- ✅ 使用 `ADD COLUMN IF NOT EXISTS`
- ✅ 使用 `ADD INDEX IF NOT EXISTS`
- ✅ 检查字段/索引是否存在后再操作

### 4. 注释要求

- 每个变更都要有清晰的说明
- 标注关联的功能或需求
- 说明变更的影响范围

---

## ⚠️ 最佳实践

### 开发环境

```bash
# 推荐使用完整初始化脚本（简单快速）
./install.sh
# 或
docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < docker/mysql-init/00-init-complete.sql
```

### 生产环境

```bash
# 1. 备份数据库（必须！）
docker exec cashier-mysql mysqldump -uroot -pRootPassword123! cashier_system > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 执行增量升级（按顺序）
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/04-v2.3.1-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/05-v2.4.0-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql

# 3. 验证升级结果
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system -e "DESC transaction_items;"
```

### 回滚策略

**如果升级失败，可以恢复备份**：

```bash
docker exec -i cashier-mysql mysql -uroot -pRootPassword123! cashier_system < backup_20260301_220000.sql
```

---

## 📦 当前文件清单

```
docker/mysql-init/
├── 00-grant-root-permissions.sql  # Root 权限配置（Docker 初始化用）
├── 00-init-complete.sql           # 完整初始化脚本（推荐用于全新安装）
├── 04-v2.3.1-updates.sql          # v2.3.1 升级脚本
├── 05-v2.4.0-updates.sql          # v2.4.0 升级脚本
├── 06-v2.4.1-updates.sql          # v2.4.1 升级脚本
├── 07-fix-transaction-items.sql   # 诊断修复脚本
└── DATABASE_VERSIONS.md            # 本文档
```

**已删除的文件**（已被 `00-init-complete.sql` 整合）：
- ❌ `01-create-user.sql`
- ❌ `02-alter-tables.sql`
- ❌ `03-sample-data.sql`