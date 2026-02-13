# 数据库变更记录 v2.3.1

## 版本信息
- **版本**: v2.3.1
- **更新日期**: 2026-02-13
- **数据库**: MySQL 8.0

## 变更概述

### 1. products 表变更

#### 变更类型：修改字段约束
- **字段**: `barcode` (条形码)
- **变更内容**: 移除 UNIQUE 唯一约束，改为普通索引
- **原因**: 允许多个商品使用相同的条形码（如不同批次的同一商品）
- **影响**: 
  - 添加商品时不再需要唯一的条形码
  - 条形码可以重复，但仍保留索引用于快速查询

#### 变更前后对比

**变更前**:
```sql
barcode VARCHAR(50) UNIQUE
```

**变更后**:
```sql
barcode VARCHAR(50)
INDEX idx_barcode (barcode)
```

### 2. members 表变更

#### 变更类型：新增字段
- **字段**: `member_code` (会员编号)
- **数据类型**: VARCHAR(50)
- **约束**: UNIQUE
- **位置**: 在 `id` 字段之后
- **默认值**: 自动生成（格式：M + 6位数字，如 M000001）
- **原因**: 提供会员唯一标识，便于会员管理和查询

#### 变更前后对比

**变更前**:
```sql
CREATE TABLE members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    ...
);
```

**变更后**:
```sql
CREATE TABLE members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_code VARCHAR(50) UNIQUE COMMENT '会员编号',
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    ...
    INDEX idx_member_code (member_code),
    ...
);
```

## 文件同步情况

### 已同步的文件

| 文件 | 状态 | 说明 |
|------|------|------|
| `src/main/java/com/cashier/util/DatabaseManager.java` | ✓ | 表创建语句已更新 |
| `docker/mysql-init/00-init-complete.sql` | ✓ | 包含 member_code 字段添加 |
| `docker/mysql-init/02-alter-tables.sql` | ✓ | 包含所有 v2.3.1 变更 |
| `docker/mysql-init/04-v2.3.1-updates.sql` | ✓ | 新建独立升级脚本 |

### 代码同步情况

| 文件 | 状态 | 说明 |
|------|------|------|
| `src/main/java/com/cashier/dao/MemberDAO.java` | ✓ | 已使用 member_code 字段 |
| `src/main/java/com/cashier/dao/ProductDAO.java` | ✓ | 已移除 barcode UNIQUE 检查 |
| `src/main/java/com/cashier/model/Member.java` | ✓ | 已包含 member_code 属性 |
| `src/main/java/com/cashier/model/Product.java` | ✓ | 已无 barcode UNIQUE 约束 |

## 升级脚本说明

### 方式一：使用 02-alter-tables.sql（推荐）

```bash
# 对于从 v2.3.0 升级到 v2.3.1
mysql -u root -p cashier_system < docker/mysql-init/02-alter-tables.sql
```

### 方式二：使用 04-v2.3.1-updates.sql（独立升级）

```bash
# 专门针对 v2.3.1 的升级脚本
mysql -u root -p cashier_system < docker/mysql-init/04-v2.3.1-updates.sql
```

### 方式三：使用 00-init-complete.sql（完整初始化）

```bash
# 完整初始化（包含所有版本变更）
mysql -u root -p cashier_system < docker/mysql-init/00-init-complete.sql
```

## 自动迁移逻辑

系统启动时会自动检测并执行以下迁移：

### 1. member_code 字段自动添加

```java
// DatabaseManager.java 中的自动迁移逻辑
try {
    String checkColumnSql = """
        SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'members'
        AND COLUMN_NAME = 'member_code'
    """;
    ResultSet rs = stmt.executeQuery(checkColumnSql);
    if (rs.next() && rs.getInt(1) == 0) {
        stmt.execute("ALTER TABLE members ADD COLUMN member_code VARCHAR(50) UNIQUE COMMENT '会员编号' AFTER id");
        logger.info("已为 members 表添加 member_code 字段");
        
        // 为现有会员生成会员编号
        stmt.execute("""
            UPDATE members 
            SET member_code = CONCAT('M', LPAD(id, 6, '0'))
            WHERE member_code IS NULL OR member_code = ''
        """);
        logger.info("已为现有会员生成会员编号");
    }
    rs.close();
} catch (SQLException e) {
    logger.warn("检查或添加 member_code 字段时出错（可能已存在）: " + e.getMessage());
}
```

### 2. 应用启动时的自动检查

应用启动时会自动：
1. 检查 `member_code` 字段是否存在
2. 如果不存在，自动添加该字段
3. 为现有会员自动生成会员编号
4. 记录日志提示操作结果

## 数据兼容性

### 新安装

- 使用 `00-init-complete.sql` 或应用自动初始化
- 新数据库自动包含所有 v2.3.1 变更

### 从 v2.3.0 升级

- 运行 `02-alter-tables.sql` 或 `04-v2.3.1-updates.sql`
- 应用启动时会自动检测并迁移

### 从更早版本升级

- 先运行 `02-alter-tables.sql`（包含 v2.3.0 和 v2.3.1 的所有变更）
- 然后运行 `04-v2.3.1-updates.sql`（可选，确保 v2.3.1 变更）

## 验证方法

### 验证 products 表变更

```sql
-- 查看 products 表结构
DESCRIBE products;

-- 查看 barcode 索引
SHOW INDEX FROM products WHERE Key_name = 'idx_barcode';

-- 验证无 UNIQUE 约束
SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'products'
AND CONSTRAINT_TYPE = 'UNIQUE';
```

### 验证 members 表变更

```sql
-- 查看 members 表结构
DESCRIBE members;

-- 查看 member_code 字段
SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_KEY 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'members'
AND COLUMN_NAME = 'member_code';

-- 查看现有会员的会员编号
SELECT id, member_code, phone, name FROM members;
```

## 回滚方案

如需回滚到 v2.3.0：

```sql
-- 回滚 products 表变更
ALTER TABLE products ADD UNIQUE INDEX barcode (barcode);

-- 回滚 members 表变更（谨慎：会删除 member_code 数据）
ALTER TABLE members DROP COLUMN member_code;
```

**注意**: 回滚前请先备份数据库！

## 常见问题

### Q1: 升级后 member_code 为空？

**A**: 运行以下 SQL 为现有会员生成编号：
```sql
UPDATE members 
SET member_code = CONCAT('M', LPAD(id, 6, '0'))
WHERE member_code IS NULL OR member_code = '';
```

### Q2: barcode 重复问题已解决？

**A**: 是的，移除 UNIQUE 约束后，多个商品可以拥有相同的条形码。

### Q3: 应用启动时自动迁移失败？

**A**: 检查数据库用户是否有 ALTER TABLE 权限，查看日志获取详细错误信息。

## 相关文档

- [数据库初始化文档](DATABASE_INIT.md)
- [MySQL 部署指南](MYSQL_SETUP.md)
- [采购表结构设计](PURCHASE_TABLE_DESIGN.md)

## 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|----------|------|
| 2026-02-13 | v2.3.1 | 移除 barcode UNIQUE 约束，添加 member_code 字段 | iFlow CLI |