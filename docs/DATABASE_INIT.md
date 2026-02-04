# 数据库初始化文档

## 概述

收银系统使用 MySQL 8.0 作为主数据库，采用双存储架构（MySQL + 文件存储），支持优雅降级。数据库初始化分为两个阶段：

1. **MySQL 容器初始化** - 创建数据库和专用用户
2. **应用启动初始化** - 创建所有数据表结构

---

## 初始化流程

### 1. MySQL 容器初始化

当使用 Docker Compose 启动 MySQL 容器时，会自动执行以下初始化脚本：

**文件**: `docker/mysql-init/01-create-user.sql`

```sql
-- 创建 cashier 用户
CREATE USER IF NOT EXISTS 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';
CREATE USER IF NOT EXISTS 'cashier'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';

-- 授予权限
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'localhost';
```

**作用**:
- 创建应用专用用户 `cashier`
- 授予对 `cashier_system` 数据库的完全访问权限
- 支持本地和远程连接

### 2. 应用启动初始化

应用启动时，`DatabaseManager.java` 会自动创建所有数据表：

```java
// 位置: src/main/java/com/cashier/util/DatabaseManager.java
private static void initializeDatabase() {
    // 创建数据库（如果不存在）
    // 创建所有表结构（如果不存在）
}
```

**特点**:
- 使用 `CREATE TABLE IF NOT EXISTS` 确保幂等性
- 首次启动时创建所有表
- 后续启动时跳过已存在的表

---

## 数据库配置

### 配置文件

**文件**: `config/database.properties`

```properties
# MySQL 数据库连接 URL
db.url=jdbc:mysql://localhost:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai

# MySQL 用户名
db.username=cashier

# MySQL 密码
db.password=YourStrongPassword123!

# 数据库连接池大小
db.pool.size=10
```

### 连接池配置

系统使用 HikariCP 连接池，提供高性能数据库访问：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| 最大连接数 | 10 | 根据收银机数量调整 |
| 最小空闲连接 | 2 | 保持的最小连接数 |
| 连接超时 | 30秒 | 获取连接的超时时间 |
| 空闲超时 | 10分钟 | 空闲连接的最大存活时间 |
| 最大生命周期 | 30分钟 | 连接的最大使用时间 |

---

## 数据表结构

### 核心业务表

#### 1. users - 用户表

```sql
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active TINYINT(1) DEFAULT 1,
    last_login_time BIGINT,
    create_time BIGINT,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**字段说明**:
- `password`: BCrypt 加密后的密码
- `role`: 用户角色（admin/cashier/finance）
- 时间字段: 使用 `BIGINT` 存储毫秒时间戳

#### 2. products - 商品表

```sql
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    quantity INT DEFAULT 0,
    category VARCHAR(50),
    barcode VARCHAR(50) UNIQUE,
    unit VARCHAR(20) DEFAULT '件',
    description TEXT,
    brand VARCHAR(100),
    supplier VARCHAR(100),
    spec VARCHAR(100),
    min_stock INT DEFAULT 0,
    cost DECIMAL(10,2),
    created_at BIGINT,
    updated_at BIGINT,
    INDEX idx_name (name),
    INDEX idx_barcode (barcode),
    INDEX idx_category (category),
    FULLTEXT idx_ft_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 3. members - 会员表

```sql
CREATE TABLE IF NOT EXISTS members (
    phone VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    balance DECIMAL(10,2) DEFAULT 0,
    points DECIMAL(10,2) DEFAULT 0,
    level VARCHAR(20) DEFAULT '普通',
    discount DECIMAL(4,2) DEFAULT 10.00,
    join_date BIGINT,
    birthday VARCHAR(10),
    INDEX idx_name (name),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**字段说明**:
- `discount`: 折扣值（10=不打折，9.8=9.8折，0=免费）

#### 4. transactions - 交易表

```sql
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    timestamp VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    operator_username VARCHAR(50),
    operator_name VARCHAR(100),
    member_phone VARCHAR(20),
    transaction_type VARCHAR(20) DEFAULT 'sale',
    voided TINYINT(1) DEFAULT 0,
    voided_by VARCHAR(50),
    voided_at BIGINT,
    INDEX idx_timestamp (timestamp),
    INDEX idx_operator (operator_username),
    INDEX idx_member (member_phone),
    INDEX idx_payment_method (payment_method),
    FOREIGN KEY (operator_username) REFERENCES users(username) ON DELETE SET NULL,
    FOREIGN KEY (member_phone) REFERENCES members(phone) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 5. transaction_items - 交易明细表

```sql
CREATE TABLE IF NOT EXISTS transaction_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    INDEX idx_transaction_id (transaction_id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 辅助业务表

#### 6. shifts - 班次表

```sql
CREATE TABLE IF NOT EXISTS shifts (
    shift_id VARCHAR(50) PRIMARY KEY,
    operator_username VARCHAR(50),
    operator_name VARCHAR(100),
    start_time BIGINT NOT NULL,
    end_time BIGINT,
    opening_revenue DECIMAL(10,2) DEFAULT 0,
    closing_revenue DECIMAL(10,2) DEFAULT 0,
    shift_revenue DECIMAL(10,2) DEFAULT 0,
    opening_transaction_count INT DEFAULT 0,
    closing_transaction_count INT DEFAULT 0,
    shift_transaction_count INT DEFAULT 0,
    cash_revenue DECIMAL(10,2) DEFAULT 0,
    wechat_revenue DECIMAL(10,2) DEFAULT 0,
    alipay_revenue DECIMAL(10,2) DEFAULT 0,
    card_revenue DECIMAL(10,2) DEFAULT 0,
    notes TEXT,
    INDEX idx_operator (operator_username),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 7. promotions - 促销表

```sql
CREATE TABLE IF NOT EXISTS promotions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    threshold DECIMAL(10,2) DEFAULT 0,
    discount DECIMAL(10,2) NOT NULL,
    description TEXT,
    start_date BIGINT,
    end_date BIGINT,
    enabled TINYINT(1) DEFAULT 1,
    usage_count INT DEFAULT 0,
    max_usage INT,
    created_at BIGINT,
    INDEX idx_type (type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 8. categories - 分类表

```sql
CREATE TABLE IF NOT EXISTS categories (
    name VARCHAR(50) PRIMARY KEY,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 9. recharges - 充值记录表

```sql
CREATE TABLE IF NOT EXISTS recharges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_phone VARCHAR(20) NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    operator_username VARCHAR(50) NOT NULL,
    operator_name VARCHAR(100) NOT NULL,
    timestamp BIGINT,
    INDEX idx_member_phone (member_phone),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (member_phone) REFERENCES members(phone) ON DELETE CASCADE,
    FOREIGN KEY (operator_username) REFERENCES users(username) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 10. operation_logs - 操作日志表

```sql
CREATE TABLE IF NOT EXISTS operation_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(50),
    timestamp BIGINT,
    INDEX idx_timestamp (timestamp),
    INDEX idx_username (username),
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 11. settings - 系统设置表

```sql
CREATE TABLE IF NOT EXISTS settings (
    `key` VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT,
    updated_at BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 数据迁移

### 自动迁移

系统首次启动时会自动执行数据迁移：

1. 检测 MySQL 数据库是否为空
2. 自动备份原有 `.txt` 数据文件到 `data/backup_<timestamp>/`
3. 将数据从文件存储迁移到 MySQL
4. 显示迁移统计信息

### 手动迁移

如需手动触发迁移：

```bash
# 方式1: 运行迁移工具
java -cp target/classes com.cashier.util.DataMigrationTool

# 方式2: 重新启动应用（会自动执行）
mvn javafx:run
```

### 迁移的数据类型

| 数据类型 | 文件存储 | MySQL 表 |
|---------|---------|----------|
| 用户数据 | `users.txt` | `users` |
| 商品库存 | `inventory.txt` | `products` |
| 会员数据 | `members.txt` | `members` |
| 交易记录 | `transactions.txt` | `transactions` + `transaction_items` |
| 交接班记录 | `shifts.txt` | `shifts` |
| 促销数据 | `promotions.txt` | `promotions` |
| 充值记录 | `recharge.txt` | `recharges` |
| 分类数据 | `categories.txt` | `categories` |
| 操作日志 | `operation_logs.txt` | `operation_logs` |
| 系统设置 | `settings.txt` | `settings` |

---

## 备份和恢复

### 数据库备份

使用 `DatabaseManager.backup()` 方法执行备份：

```java
// 备份到指定文件
File backupFile = new File("backup/cashier_system_20260204.sql");
boolean success = DatabaseManager.backup(backupFile);
```

### 使用 mysqldump 备份

```bash
# 备份整个数据库
mysqldump -u cashier -p cashier_system > backup.sql

# 备份到 Docker 容器
docker exec cashier-mysql mysqldump -u cashier -p cashier_system > backup.sql
```

### 数据库恢复

```bash
# 恢复数据库
mysql -u cashier -p cashier_system < backup.sql

# 在 Docker 容器中恢复
docker exec -i cashier-mysql mysql -u cashier -p cashier_system < backup.sql
```

### Docker 卷备份

```bash
# 备份 Docker 数据卷
docker run --rm -v cashier-mysql-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-data-backup.tar.gz /data

# 恢复 Docker 数据卷
docker run --rm -v cashier-mysql-data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql-data-backup.tar.gz -C /
```

---

## 用户和权限

### 默认用户

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | admin123 | admin | 系统管理员 |
| cashier | YourStrongPassword123! | cashier | 应用专用用户 |

### 权限说明

| 角色 | 权限 |
|------|------|
| admin | 所有权限 |
| cashier | 日常收银操作 |
| finance | 报表和数据统计 |

### 创建新用户

```sql
-- 创建用户
CREATE USER 'newuser'@'%' IDENTIFIED BY 'password';

-- 授予权限
GRANT ALL PRIVILEGES ON cashier_system.* TO 'newuser'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

---

## 常见问题

### 1. 数据库连接失败

**原因**: MySQL 服务未启动或配置错误

**解决**:
```bash
# 检查 MySQL 容器状态
docker-compose ps

# 查看 MySQL 日志
docker-compose logs mysql

# 重启 MySQL
docker-compose restart mysql
```

### 2. 时区问题

**现象**: 时间显示不正确

**解决**: 确保 JDBC URL 包含 `serverTimezone=Asia/Shanghai`

```properties
db.url=jdbc:mysql://localhost:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai
```

### 3. 字符编码问题

**现象**: 中文乱码

**解决**: 数据库和表使用 `utf8mb4` 字符集

```sql
CREATE DATABASE cashier_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. 数据迁移失败

**原因**: 原有数据文件损坏或格式错误

**解决**:
1. 检查 `data/` 目录下的 `.txt` 文件
2. 查看迁移日志
3. 使用备份文件恢复

### 5. 权限不足

**现象**: Access denied for user 'cashier'@'%'

**解决**:
```sql
-- 重新授予权限
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
FLUSH PRIVILEGES;
```

---

## 最佳实践

1. **定期备份**: 每天自动备份数据库
2. **监控性能**: 定期检查慢查询日志
3. **索引优化**: 根据查询模式调整索引
4. **连接池配置**: 根据并发量调整连接池大小
5. **日志保留**: 定期清理操作日志表
6. **权限管理**: 定期审查用户权限
7. **安全加固**: 修改默认密码，限制远程访问

---

## 参考文档

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [HikariCP 文档](https://github.com/brettwooldridge/HikariCP)
- [Docker MySQL 文档](https://hub.docker.com/_/mysql)
- 数据迁移工具: `com.cashier.util.DataMigrationTool`
- 数据库管理器: `com.cashier.util.DatabaseManager`