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

1. **下载 MySQL Installer**
   - 访问: https://dev.mysql.com/downloads/installer/
   - 下载 "mysql-installer-community"

2. **运行安装程序**
   ```
   双击 mysql-installer-community-8.x.x.x.msi
   选择 "Developer Default" 或 "Server only"
   ```

3. **配置 MySQL Server**
   - **Type**: Standalone MySQL Server
   - **Config Type**: Development Computer
   - **Port**: 3306 (默认)
   - **Root Password**: 设置一个强密码并记住它！
   - **Windows Service**: 启用

4. **完成安装**
   - 点击 "Execute" 完成配置
   - 确保所有步骤显示 "Complete"

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
-- 创建收银系统专用用户
CREATE USER 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';

-- 创建数据库
CREATE DATABASE cashier_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 授予权限
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 退出
EXIT;
```

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
db.url=jdbc:mysql://192.168.1.100:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai

# 修改为实际的用户名和密码
db.username=cashier
db.password=YourStrongPassword123!

# 根据收银机数量调整连接池
db.pool.size=10
```

### 3. 配置参数说明

| 参数 | 说明 | 示例值 |
|-----|------|--------|
| db.url | 数据库连接地址 | jdbc:mysql://localhost:3306/cashier_system |
| db.username | 数据库用户名 | cashier |
| db.password | 数据库密码 | YourPassword123 |
| db.pool.size | 连接池大小 | 10 (2-3台收银机) |

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

## 数据迁移

### 从文件存储迁移到 MySQL

启动应用时，会自动检测并执行数据迁移：

```bash
# 运行应用
java -jar cashier-system-fx.jar

# 或使用 Maven
mvn javafx:run
```

**迁移过程**:
1. 检测 MySQL 数据库是否为空
2. 自动备份原有 .txt 数据文件到 `data/backup_<timestamp>/`
3. 将数据迁移到 MySQL 数据库
4. 显示迁移统计信息

**手动运行迁移工具**:
```bash
java -cp target/classes com.cashier.util.DataMigrationTool
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
→ 参数: --user=root --password=YourPass --result-file=D:\backup\cashier_%date:~0,10%.sql cashier_system
```

**macOS/Linux - Cron**:
```bash
# 编辑 crontab
crontab -e

# 每天凌晨 2 点备份
0 2 * * * mysqldump -u root -pYourPass cashier_system > /backup/cashier_$(date +\%Y\%m\%d).sql
```

### 手动备份

**使用 mysqldump**:
```bash
# 完整备份
mysqldump -u root -p cashier_system > backup_$(date +%Y%m%d).sql

# 压缩备份
mysqldump -u root -p cashier_system | gzip > backup_$(date +%Y%m%d).sql.gz
```

**使用应用内置备份**:
应用设置界面有"数据备份"功能，可一键备份。

### 恢复数据

```bash
# 从 SQL 文件恢复
mysql -u root -p cashier_system < backup_20250203.sql

# 或使用命令行
mysql -u root -p
USE cashier_system;
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
ALTER USER 'cashier'@'%' IDENTIFIED WITH mysql_native_password BY 'YourPassword123!';
FLUSH PRIVILEGES;
```

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

**config/database.properties**:
```properties
# 根据实际并发需求调整
db.pool.size=15
db.connection.timeout=30000
db.idle.timeout=300000
db.max.lifetime=1800000
```

---

## 安全建议

1. **使用专用用户**: 不要使用 root 用户连接应用
2. **强密码**: 使用复杂的密码（大小写字母+数字+符号）
3. **限制网络访问**: 只允许局域网访问，不要暴露到公网
4. **定期备份**: 每天自动备份数据库
5. **SSL 连接**: 生产环境建议使用 SSL (`useSSL=true`)
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
USE cashier_system;

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
WHERE table_schema = 'cashier_system'
GROUP BY table_schema;
```

### 连接字符串示例

```
# 本机 MySQL
jdbc:mysql://localhost:3306/cashier_system

# 局域网 MySQL
jdbc:mysql://192.168.1.100:3306/cashier_system

# 带超时配置
jdbc:mysql://localhost:3306/cashier_system?connectTimeout=10000&socketTimeout=30000

# SSL 连接
jdbc:mysql://localhost:3306/cashier_system?useSSL=true&requireSSL=true
```
