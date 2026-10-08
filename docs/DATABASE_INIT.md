# 数据库初始化文档

## 概述

收银系统使用 MySQL 8.0 作为主数据库。数据库初始化有两条通道：

1. **Docker 容器初始化**（仅 Docker Compose 首次以空数据卷启动时）- 由环境变量创建数据库和专用用户，并执行建表脚本
2. **应用启动初始化**（每次启动）- 幂等创建/升级表结构，是应用侧表结构的权威来源

非 Docker 部署只需自行准备数据库和用户，表结构交给应用启动时自动创建。

---

## 初始化流程

### 1. Docker 容器初始化

数据库名和应用专用用户由 `docker-compose.yml` 传给官方 MySQL 镜像的环境变量创建，**没有**单独的建用户 SQL 脚本：

| 环境变量 | 来源 | 默认值 |
|----------|------|--------|
| `MYSQL_DATABASE` | `.env` | `lisuan_system` |
| `MYSQL_USER` | `.env` | `lisuan` |
| `MYSQL_PASSWORD` | `.env` 的 `CASHIER_DB_PASSWORD` | 无默认值，必填 |
| `MYSQL_ROOT_PASSWORD` | `.env` | 无默认值，必填 |

镜像在首次以空数据卷启动时创建上述数据库和用户并授权，随后执行 `/docker-entrypoint-initdb.d/` 下的脚本。`docker-compose.yml` 只挂载了一个脚本：

- `docker/mysql-init/00-init-complete.sql` - 完整建表脚本

**注意**:
- 初始化脚本只在数据卷为空时执行一次；已有数据的卷不会重跑，由应用启动初始化兜底补建/升级。
- `docker/mysql-init/` 下的 `07-`、`08-`、`09-`、`10-`、`11-`、`99-` 系列脚本用于**手工**升级旧库，compose 不会自动挂载执行。

### 2. 应用启动初始化

应用每次启动都会调用 `DatabaseManager.initializeDatabase()`（`src/main/java/com/cashier/util/DatabaseManager.java`）：

- 用 `CREATE TABLE IF NOT EXISTS` 幂等创建缺失的表
- 用带 try/catch 的幂等 `ALTER TABLE ... ADD COLUMN` 就地升级旧表（如 `users.force_password_change`、`products.is_hot`）

**这是应用侧表结构的权威来源**：新增表或字段必须改这里，而不是只改 SQL 文件。此外部分功能 DAO 也会按需建表（`CREATE TABLE IF NOT EXISTS`），`00-init-complete.sql` 中则包含少量应用侧不再创建的表——几处清单并不完全一致，请一律以源码为准，不要依赖文档中的清单。

---

## 数据库配置

配置文件字段、环境变量优先级与连接池参数（含默认值）见 [MYSQL_SETUP.md](MYSQL_SETUP.md) 的「配置应用」与「性能优化」章节；Docker 部署的数据库环境变量见 [DOCKER_CONFIGURATION.md](DOCKER_CONFIGURATION.md)。

---

## 数据表结构

> 以下为核心/常用表的**示意**结构（并非全部表，个别字段可能与最新源码有出入）。完整、权威的表结构以 `DatabaseManager.java`、功能 DAO 的建表语句与 `docker/mysql-init/00-init-complete.sql` 为准。

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
    force_password_change TINYINT(1) DEFAULT 0,
    last_login_time BIGINT,
    create_time BIGINT,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**字段说明**:
- `password`: BCrypt 加密后的密码
- `role`: 用户角色（admin/cashier/finance）
- `force_password_change`: 强制改密标记（为 1 时登录会被要求改密，改密后自动清零）
- 时间字段: 使用 `BIGINT` 存储毫秒时间戳

#### 2. products - 商品表

```sql
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(50) UNIQUE COMMENT '商品编号',
    name VARCHAR(200) NOT NULL COMMENT '商品名称（唯一）',
    price DECIMAL(10,2) NOT NULL,
    quantity INT DEFAULT 0,
    category VARCHAR(50),
    barcode VARCHAR(50),
    unit VARCHAR(20) DEFAULT '件',
    description TEXT,
    brand VARCHAR(100),
    supplier VARCHAR(100),
    spec VARCHAR(100),
    min_stock INT DEFAULT 0,
    cost DECIMAL(10,2),
    version INT DEFAULT 0 COMMENT '版本号（用于乐观锁）',
    is_hot TINYINT DEFAULT 0 COMMENT '是否热销',
    created_at BIGINT,
    updated_at BIGINT,
    UNIQUE KEY uk_product_name (name),
    INDEX idx_product_code (product_code),
    INDEX idx_name (name),
    INDEX idx_barcode (barcode),
    INDEX idx_category (category),
    FULLTEXT idx_ft_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**字段说明**:
- `name`: 商品名称，**全局唯一**（不允许重名）
- `barcode`: 条码，**允许重复**
- `is_hot`: 热销标记（收银页热销区使用）

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
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '交易状态（NORMAL-正常，REFUNDED-已退款）',
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

#### 9. recharge_records - 充值记录表

列结构与 `RechargeRecordDAORefactored` 保持一致（v2.6.x 起替代废弃的旧表 `recharges`）。

```sql
CREATE TABLE IF NOT EXISTS recharge_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    record_id VARCHAR(50) UNIQUE NOT NULL COMMENT '充值记录编号',
    member_phone VARCHAR(20) NOT NULL,
    member_name VARCHAR(100),
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20),
    operator VARCHAR(50),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_member_phone (member_phone),
    INDEX idx_timestamp (timestamp)
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



## 备份和恢复

完整方法（自动备份、mysqldump、Docker 容器/数据卷备份、应用内置备份）见 [MYSQL_SETUP.md](MYSQL_SETUP.md) 的「备份与恢复」章节。

---

## 用户和权限

### 默认账号

| 账号 | 密码 | 类型 | 说明 |
|------|------|------|------|
| `lisuan` | `.env` 的 `CASHIER_DB_PASSWORD` | 数据库用户 | Docker 首次启动时按 `MYSQL_USER` 创建（默认 `lisuan`，可改），供应用连接数据库 |
| `admin` | 首次运行向导中设置（无默认口令） | 应用用户 | 系统管理员，空库首次启动时由向导创建 |

### 应用角色权限

| 角色 | 权限 |
|------|------|
| admin | 所有权限 |
| cashier | 日常收银操作 |
| finance | 报表和数据统计 |

### 创建数据库用户

新增或调整数据库账号的 SQL 示例见 [MYSQL_SETUP.md](MYSQL_SETUP.md) 的「配置数据库」章节。

---

## 常见问题

连接失败（含 Docker 容器检查）、时区、字符编码、权限不足等问题的排查见 [MYSQL_SETUP.md](MYSQL_SETUP.md) 的「故障排查」章节。

---

## 最佳实践

1. **定期备份**: 每天自动备份数据库
2. **监控性能**: 定期检查慢查询日志
3. **索引优化**: 根据查询模式调整索引
4. **连接池配置**: 根据并发量调整连接池大小
5. **日志保留**: 定期清理操作日志表
6. **权限管理**: 定期审查用户权限
7. **安全加固**: 定期轮换数据库口令，限制数据库远程访问

---

## 参考文档

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [HikariCP 文档](https://github.com/brettwooldridge/HikariCP)
- [Docker MySQL 文档](https://hub.docker.com/_/mysql)
- 数据库管理器: `com.cashier.util.DatabaseManager`
