# 数据库版本管理

## 版本历史

| 版本 | 脚本文件 | 说明 | 发布日期 | 状态 |
|------|----------|------|----------|------|
| v2.4.1 | 00-init-complete.sql | 完整初始化脚本（整合所有功能） | 2026-03-01 | ✅ 已发布 |
| v2.4.1 | 06-v2.4.1-updates.sql | 添加商品ID和编号字段 | 2026-03-01 | ✅ 已发布 |
| v2.4.0 | 05-v2.4.0-updates.sql | 退货管理、导出历史 | 2026-02-29 | ✅ 已发布 |
| v2.3.1 | 04-v2.3.1-updates.sql | 退货管理功能 | 2026-02-13 | ✅ 已发布 |
| v2.3.0 | 02-alter-tables.sql | 进销存管理 | 2026-02-07 | ✅ 已发布 |
| v2.2.0 | 01-create-user.sql | MySQL迁移 | 2026-02-03 | ✅ 已发布 |

## 初始化新环境

执行完整初始化：

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/00-init-complete.sql
```

## 升级现有环境

### 从 v2.4.0 升级到 v2.4.1

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql
```

### 从 v2.3.1 升级到 v2.4.0

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/05-v2.4.0-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql
```

### 从 v2.3.0 升级到 v2.4.1

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/04-v2.3.1-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/05-v2.4.0-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql
```

## 可选脚本

### 修复历史数据

```bash
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/07-fix-transaction-items.sql
```

注意：此脚本仅用于检查和诊断，不会自动修复数据。

## 检查数据库版本

```sql
-- 检查表结构
SHOW CREATE TABLE transaction_items;

-- 检查是否包含新字段
DESC transaction_items;
```

## 脚本编写规范

1. **命名规范**
   - 升级脚本：`v{version}-updates.sql`
   - 修复脚本：`fix-{description}.sql`
   - 检查脚本：`check-{description}.sql`

2. **脚本结构**
   ```sql
   -- ================================================
   -- 版本: v{version}
   -- 日期: {date}
   -- 说明: {description}
   -- ================================================

   -- 1. 变更1
   ALTER TABLE...

   -- 2. 变更2
   ALTER TABLE...

   -- 完成
   ```

3. **幂等性**
   - 使用 `IF NOT EXISTS`、`ADD COLUMN IF NOT EXISTS`
   - 避免重复执行时出错

4. **注释清晰**
   - 每个变更都要有说明
   - 标注关联的功能或需求

## 回滚策略

每个升级脚本都应该有对应的回滚脚本（可选）：

```sql
-- rollback-v2.4.1.sql
ALTER TABLE transaction_items DROP COLUMN product_id;
ALTER TABLE transaction_items DROP COLUMN product_code;
ALTER TABLE transaction_items DROP COLUMN barcode;
```

## 备份建议

执行任何升级脚本前，先备份数据库：

```bash
# 备份
docker exec cashier-mysql mysqldump -uroot -pRootPassword123! cashier_system > backup_$(date +%Y%m%d_%H%M%S).sql

# 恢复
docker exec -i cashier-mysql mysql -uroot -pRootPassword123! cashier_system < backup_20260301_220000.sql
```