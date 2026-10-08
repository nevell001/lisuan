# MySQL 数据库部署指南

本文档详细说明如何为收银系统安装和配置 MySQL 数据库。

## 目录
1. [系统要求](#系统要求)
2. [安装 MySQL](#安装-mysql)
3. [配置数据库](#配置数据库)
4. [配置应用](#配置应用)
5. [数据迁移](#数据迁移)
6. [网络配置](#网络配置)
7. [备份与恢复](#备份与恢复)
8. [故障排查](#故障排查)

---

## 系统要求

### 主机（服务器）要求
- **操作系统**: Windows 10+, macOS 10.15+, 或 Linux (Ubuntu 20.04+, CentOS 7+)
- **内存**: 最低 2GB，推荐 4GB+
- **磁盘空间**: 最低 1GB 可用空间
- **网络**: 局域网连接（100Mbps+）

### 客户端（收银机）要求
- **操作系统**: Windows 10+, macOS 10.15+, 或 Linux
- **网络**: 能够通过局域网访问主机
- **Java**: JDK 17 或更高版本

---

## 安装 MySQL

### Windows 安装

推荐使用官方 MSI 安装包（Developer Default，含 MySQL Workbench）。详细的图文安装步骤见 [WINDOWS_MYSQL_SETUP.md](WINDOWS_MYSQL_SETUP.md)。

### macOS 安装

1. **使用 Homebrew（推荐）**
   ```bash
   # 安装 Homebrew（如果未安装）
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

   # 安装 MySQL
   brew install mysql

   # 启动 MySQL 服务
   brew services start mysql
   ```

2. **或下载 DMG 安装包**
   - 访问: https://dev.mysql.com/downloads/mysql/
   - 下载 macOS DMG 文件
   - 双击安装

### Linux (Ubuntu) 安装

```bash
# 更新包列表
sudo apt update

# 安装 MySQL Server
sudo apt install mysql-server -y

# 启动 MySQL 服务
sudo systemctl start mysql

# 设置开机自启动
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

---

## 配置数据库

### 1. 创建数据库用户

```bash
# 登录 MySQL
mysql -u root -p

# 或者在 Windows 上使用 MySQL Workbench
```

### 2. 执行 SQL 命令

```sql
-- 创建收银系统专用用户（用户名需与后续 config/database.properties 的 db.username 一致）
CREATE USER 'lisuan'@'%' IDENTIFIED BY 'REPLACE_WITH_STRONG_RANDOM_PASSWORD';

-- 创建数据库
CREATE DATABASE lisuan_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 授予权限
GRANT ALL PRIVILEGES ON lisuan_system.* TO 'lisuan'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 退出
EXIT;
```

> Docker 部署无需手工执行以上 SQL：`docker-compose.yml` 会用 `.env` 的 `MYSQL_DATABASE`/`MYSQL_USER`/`CASHIER_DB_PASSWORD` 自动建库建用户（默认用户名即 `lisuan`）。
>
> 这里只需准备数据库和用户，**表结构不用手工创建**：应用每次启动都会幂等建表/升级，Docker 首次启动还会先执行 `docker/mysql-init/00-init-complete.sql`（机制见 [DATABASE_INIT.md](DATABASE_INIT.md)）。

### 3. 配置远程访问（可选）

如果数据库和收银系统不在同一台机器上：

**编辑 MySQL 配置文件**:

- **Windows**: `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`
- **macOS**: `/etc/my.cnf`
- **Linux**: `/etc/mysql/mysql.conf.d/mysqld.cnf`

```ini
[mysqld]
# 绑定所有网络接口
bind-address = 0.0.0.0

# 或指定特定 IP
# bind-address = 192.168.1.100
```

**重启 MySQL 服务**:
```bash
# Windows
net stop MySQL80
net start MySQL80

# macOS
brew services restart mysql

# Linux
sudo systemctl restart mysql
```

---

## 配置应用

### 1. 复制配置文件

```bash
# 从示例配置创建实际配置
cp config/database.properties.example config/database.properties
```

### 2. 编辑配置文件

**config/database.properties**:
```properties
# 修改为实际的主机地址
db.url=jdbc:mysql://192.168.1.100:3306/lisuan_system?sslMode=PREFERRED&serverTimezone=Asia/Shanghai

# 修改为实际的用户名和密码（与创建数据库时的用户名一致）
db.username=lisuan
# 推荐留空，并通过 CASHIER_DB_PASSWORD 环境变量提供
db.password=

# 根据收银机数量调整连接池
db.pool.size=10
```

### 3. 配置参数说明

| 参数 | 说明 | 示例值 |
|-----|------|--------|
| db.url | 数据库连接地址 | jdbc:mysql://localhost:3306/lisuan_system |
| db.username | 数据库用户名 | lisuan |
| db.password | 数据库密码 | **留空**（由 `CASHIER_DB_PASSWORD` 环境变量或根目录 `.env` 提供；写入明文会被发布门禁拒绝） |
| db.pool.size | 连接池大小 | 10 (2-3台收银机) |

> 表结构无需在此步处理：应用启动时自动创建/升级（见 [DATABASE_INIT.md](DATABASE_INIT.md)）。

---

## 网络配置

### 主机（服务器）固定 IP

建议为数据库服务器设置静态 IP 地址：

**Windows**:
```
控制面板 → 网络和 Internet → 网络和共享中心
→ 更改适配器设置 → 以太网 → 属性 → IPv4 设置
```

**macOS**:
```
系统偏好设置 → 网络 → 高级 → TCP/IP → 配置 IPv4
```

**Linux (Ubuntu)**:
```bash
# 编辑网络配置
sudo nano /etc/netplan/01-netcfg.yaml

network:
  version: 2
  ethernets:
    eth0:
      dhcp4: no
      addresses: [192.168.1.100/24]
      gateway4: 192.168.1.1
      nameservers:
        addresses: [8.8.8.8, 8.8.4.4]

# 应用配置
sudo netplan apply
```

### 防火墙配置

确保 MySQL 端口（3306）允许局域网访问：

**Windows**:
```
Windows Defender 防火墙 → 高级设置 → 入站规则
→ 新建规则 → 端口 → TCP 3306 → 允许连接
```

**macOS**:
```bash
# 系统偏好设置 → 安全性与隐私 → 防火墙选项
# 或使用命令行
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/local/mysql/bin/mysqld
```

**Linux (Ubuntu)**:
```bash
sudo ufw allow from 192.168.1.0/24 to any port 3306
sudo ufw reload
```

---



## 备份与恢复

### 自动备份（推荐）

使用系统的定时任务定期备份数据库：

**Windows - 任务计划程序**:
```
创建基本任务 → 每天 02:00
→ 操作: 启动程序
→ 程序: mysqldump
→ 参数: --user=root --password=YourPass --result-file=D:\backup\lisuan_%date:~0,10%.sql lisuan_system
```

**macOS/Linux - Cron**:
```bash
# 编辑 crontab
crontab -e

# 每天凌晨 2 点备份
0 2 * * * mysqldump -u root -pYourPass lisuan_system > /backup/lisuan_$(date +\%Y\%m\%d).sql
```

### 手动备份

**使用 mysqldump**:
```bash
# 完整备份
mysqldump -u root -p lisuan_system > backup_$(date +%Y%m%d).sql

# 压缩备份
mysqldump -u root -p lisuan_system | gzip > backup_$(date +%Y%m%d).sql.gz
```

**使用应用内置备份**:
应用设置界面有"数据备份"功能，可一键备份（底层为 `DatabaseManager.backup(File)`/`restore(File)`）。

### Docker 部署的备份与恢复

Docker 部署的 MySQL 数据在容器数据卷中，容器名默认 `lisuan-mysql`（可用 `db` 配置项 `backup.mysql.container` 覆盖）。

**备份**（导出 SQL 到宿主机）:
```bash
docker exec lisuan-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" lisuan_system' > backup_$(date +%Y%m%d).sql
# compose 已把宿主机 backups/sql 挂载到容器 /backup，也可直接写进去：
docker exec lisuan-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" lisuan_system > /backup/backup_$(date +%Y%m%d).sql'
```

**恢复**（把 SQL 灌回容器）:
```bash
docker exec -i lisuan-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" lisuan_system' < backup_20250203.sql
```

**数据卷级备份**（整卷打包，含所有库）:
```bash
# 建议先停容器，保证数据文件一致
docker compose stop mysql
# compose 会在卷名前加项目前缀，实际卷名以 docker volume ls 为准
docker run --rm -v "$(docker volume ls -q -f name=lisuan-mysql-data | head -n1)":/data -v "$PWD":/backup alpine tar czf /backup/mysql-volume-$(date +%Y%m%d).tar.gz -C /data .
docker compose start mysql
```

### 恢复数据

```bash
# 从 SQL 文件恢复
mysql -u root -p lisuan_system < backup_20250203.sql

# 或使用命令行
mysql -u root -p
USE lisuan_system;
SOURCE /path/to/backup.sql;
```

---

## 故障排查

### 问题 1: 无法连接到数据库

**错误信息**: `Communications link failure`

**解决方案**:
1. 检查 MySQL 服务是否运行:
   ```bash
   # Windows
   sc query MySQL80

   # macOS/Linux
   sudo systemctl status mysql

   # Docker
   docker compose ps
   docker compose logs mysql --tail 50
   ```

2. 检查防火墙是否允许 3306 端口

3. 检查配置文件中的主机地址是否正确

4. 测试网络连通性:
   ```bash
   ping 192.168.1.100
   telnet 192.168.1.100 3306
   ```

### 问题 2: 时区错误

**错误信息**: `The server time zone value 'XXX' is unrecognized`

**解决方案**:
已在配置文件中添加 `serverTimezone=Asia/Shanghai`，如果仍有问题：

```sql
-- 在 MySQL 中设置时区
SET GLOBAL time_zone = 'Asia/Shanghai';
```

### 问题 3: 认证插件错误

**错误信息**: `Authentication plugin 'caching_sha2_password' cannot be loaded`

**解决方案**:
```sql
-- 修改用户使用旧版认证
ALTER USER 'lisuan'@'%' IDENTIFIED WITH mysql_native_password BY 'YourPassword123!';
FLUSH PRIVILEGES;
```
> 用户名替换为实际的 `db.username`。Docker 部署已通过启动参数 `--mysql-native-password=ON` 兼容此场景。

### 问题 4: 字符集问题

**现象**: 中文显示乱码

**解决方案**:
```sql
-- 检查数据库字符集
SHOW VARIABLES LIKE 'character%';

-- 应该看到:
-- character_set_database = utf8mb4
-- character_set_server = utf8mb4

-- 如果不是，修改配置文件并重启
```

### 问题 5: 权限不足

**错误信息**: `Access denied for user 'lisuan'@'%'`（错误中的用户名即 `db.username` 的配置值）

**解决方案**:
1. 确认应用 `db.username` 与数据库用户一致，且口令与 `CASHIER_DB_PASSWORD`（或 `.env`）一致
2. 补授权限:
   ```sql
   GRANT ALL PRIVILEGES ON lisuan_system.* TO 'lisuan'@'%';
   FLUSH PRIVILEGES;
   ```
   > 用户名/库名替换为实际值（Docker 部署默认均为 `lisuan` / `lisuan_system`）。

---

## 性能优化

### MySQL 配置优化

编辑 `my.cnf` (Linux) 或 `my.ini` (Windows):

```ini
[mysqld]
# 内存配置（根据服务器内存调整）
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M

# 连接配置
max_connections = 100
connect_timeout = 10

# 查询缓存（MySQL 5.7 及以下）
query_cache_size = 64M
query_cache_type = 1

# 慢查询日志
slow_query_log = 1
long_query_time = 2
```

### 应用连接池优化

连接池参数（HikariCP）及其默认值如下，按需在 **config/database.properties** 覆盖：

| 参数 | 默认值 | 说明 |
|-----|-------|------|
| `db.pool.size` | `10` | 最大连接数（2-3 台收银机 10 足够） |
| `db.connection.timeout` | `15000` | 等待连接的毫秒数。冷启动首次建连可能数秒，不建议调得太小 |
| `db.idle.timeout` | `600000` | 空闲连接回收阈值（ms） |
| `db.max.lifetime` | `1800000` | 连接最大存活时间（ms） |
| `db.connection.leakDetectionThreshold` | `30000` | 连接泄漏检测阈值（ms），0 关闭 |
| `db.validationTimeout` | `3000` | 连接校验超时（ms） |

> 最小空闲连接固定为 `max(2, 连接池大小 / 4)`、连接测试语句为 `SELECT 1`，均不可配置。
> 完整参数解析逻辑见 `com.cashier.util.DatabaseManager#loadConfig`。

---

## 安全建议

1. **使用专用用户**: 不要使用 root 用户连接应用
2. **强密码**: 使用复杂的密码（大小写字母+数字+符号）
3. **限制网络访问**: 只允许局域网访问，不要暴露到公网
4. **定期备份**: 每天自动备份数据库
5. **SSL 连接**: 生产环境建议使用 SSL（在 `db.url` 中设置 `sslMode=REQUIRED`）
6. **更新 MySQL**: 定期更新到最新稳定版本

---

## 技术支持

如遇到问题：
1. 检查本文档的故障排查部分
2. 查看 MySQL 日志: `/var/log/mysql/error.log`
3. 查看应用日志: `logs/application.log`
4. 联系技术支持并提供错误日志

---

## 附录

### 常用 MySQL 命令

```bash
# 登录 MySQL
mysql -u root -p

# 查看数据库
SHOW DATABASES;

# 使用数据库
USE lisuan_system;

# 查看表
SHOW TABLES;

# 查看表结构
DESCRIBE users;

# 查看表数据
SELECT * FROM users LIMIT 10;

# 统计记录数
SELECT COUNT(*) FROM transactions;

# 查看连接数
SHOW PROCESSLIST;

# 查看数据库大小
SELECT
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'lisuan_system'
GROUP BY table_schema;
```

### 连接字符串示例

```
# 本机 MySQL
jdbc:mysql://localhost:3306/lisuan_system

# 局域网 MySQL
jdbc:mysql://192.168.1.100:3306/lisuan_system

# 带超时配置
jdbc:mysql://localhost:3306/lisuan_system?connectTimeout=10000&socketTimeout=30000

# SSL 连接
jdbc:mysql://localhost:3306/lisuan_system?sslMode=REQUIRED
```
