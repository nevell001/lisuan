# 使用 Docker 安装 MySQL 8.4

本文档详细说明如何使用 Docker 快速部署 MySQL 8.4 数据库用于收银系统。

## 目录
1. [安装 Docker](#安装-docker)
2. [快速启动](#快速启动)
3. [配置说明](#配置说明)
4. [创建数据库用户](#创建数据库用户)
5. [数据持久化](#数据持久化)
6. [常用命令](#常用命令)
7. [故障排查](#故障排查)

---

## 安装 Docker

### Windows 安装

1. **下载 Docker Desktop**
   - 访问: https://www.docker.com/products/docker-desktop/
   - 下载 Windows 版本

2. **运行安装程序**
   ```
   双击 Docker Desktop Installer.exe
   按照提示完成安装
   重启计算机
   ```

3. **验证安装**
   ```powershell
   docker --version
   docker-compose --version
   ```

### macOS 安装

1. **下载 Docker Desktop**
   - 访问: https://www.docker.com/products/docker-desktop/
   - 下载 Mac with Intel chip 或 Apple Silicon 版本

2. **安装并启动**
   ```
   双击 Docker.dmg
   拖动到应用程序文件夹
   启动 Docker Desktop
   ```

3. **验证安装**
   ```bash
   docker --version
   docker-compose --version
   ```

### Linux (Ubuntu) 安装

```bash
# 更新包列表
sudo apt update

# 安装必要依赖
sudo apt install -y ca-certificates curl gnupg lsb-release

# 添加 Docker 官方 GPG 密钥
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 添加 Docker 仓库
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker Engine
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 启动 Docker 服务
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
docker --version
```

---

## 快速启动

### 方法 1: 使用 docker run（最简单）

```bash
# 拉取 MySQL 8.4 镜像
docker pull mysql:8.4

# 启动 MySQL 容器
docker run -d \
  --name lisuan-mysql \
  --restart unless-stopped \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=RootPassword123! \
  -e MYSQL_DATABASE=lisuan_system \
  -e MYSQL_USER=cashier \
  -e MYSQL_PASSWORD=YourStrongPassword123! \
  -v lisuan-mysql-data:/var/lib/mysql \
  mysql:8.4 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

### 方法 2: 使用 docker-compose（推荐）

创建 `docker-compose.yml` 文件：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.4
    container_name: lisuan-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: RootPassword123!
      MYSQL_DATABASE: lisuan_system
      MYSQL_USER: cashier
      MYSQL_PASSWORD: YourStrongPassword123!
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - lisuan-mysql-data:/var/lib/mysql
      - ./mysql-init:/docker-entrypoint-initdb.d
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-authentication-plugin=mysql_native_password
      - --max-connections=200
      - --innodb-buffer-pool-size=1G
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  lisuan-mysql-data:
    driver: local
```

启动容器：

```bash
# 启动
docker-compose up -d

# 查看日志
docker-compose logs -f mysql

# 停止
docker-compose down

# 停止并删除数据（谨慎！）
docker-compose down -v
```

---

## 配置说明

### 环境变量详解

| 变量 | 说明 | 示例值 |
|-----|------|--------|
| `MYSQL_ROOT_PASSWORD` | root 用户密码（必需） | RootPassword123! |
| `MYSQL_DATABASE` | 创建的数据库名 | lisuan_system |
| `MYSQL_USER` | 创建的专用用户名 | cashier |
| `MYSQL_PASSWORD` | 专用用户密码 | YourStrongPassword123! |
| `TZ` | 时区 | Asia/Shanghai |

### 端口映射

```bash
-p 3306:3306
#   ↑       ↑
#   主机    容器
#   端口    端口
```

如果主机 3306 端口被占用，可以映射到其他端口：
```bash
-p 3307:3306  # 使用主机 3307 端口
```

### 数据卷

```bash
-v lisuan-mysql-data:/var/lib/mysql
#            ↑
#            容器内 MySQL 数据目录
```

这会将 MySQL 数据持久化到 Docker 卷，即使删除容器数据也不会丢失。

---

## 创建数据库用户

### 方法 1: 通过环境变量自动创建（推荐）

在启动容器时通过环境变量创建，参见上面的 `docker run` 或 `docker-compose.yml`。

### 方法 2: 进入容器手动创建

```bash
# 进入容器
docker exec -it lisuan-mysql mysql -u root -p

# 或使用 bash
docker exec -it lisuan-mysql bash
mysql -u root -p
```

然后执行 SQL：

```sql
-- 创建专用用户
CREATE USER 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS lisuan_system
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- 授予权限
GRANT ALL PRIVILEGES ON lisuan_system.* TO 'cashier'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 查看用户
SELECT host, user FROM mysql.user;

-- 退出
EXIT;
```

### 方法 3: 使用初始化脚本

1. **创建初始化脚本目录**
```bash
mkdir -p mysql-init
```

2. **创建 SQL 脚本** `mysql-init/01-create-user.sql`:
```sql
-- 创建专用用户
CREATE USER IF NOT EXISTS 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';

-- 授予所有权限
GRANT ALL PRIVILEGES ON lisuan_system.* TO 'cashier'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

3. **修改 docker-compose.yml** 添加卷挂载：
```yaml
volumes:
  - lisuan-mysql-data:/var/lib/mysql
  - ./mysql-init:/docker-entrypoint-initdb.d  # 添加这行
```

4. **启动容器**
```bash
docker-compose up -d
```

容器首次启动时会自动执行 `mysql-init` 目录下的所有 SQL 文件。

---

## 数据持久化

### 数据卷管理

```bash
# 查看所有卷
docker volume ls

# 查看卷详情
docker volume inspect lisuan-mysql-data

# 备份数据卷
docker run --rm \
  -v lisuan-mysql-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/lisuan-mysql-backup.tar.gz -C /data .

# 恢复数据卷
docker run --rm \
  -v lisuan-mysql-data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/lisuan-mysql-backup.tar.gz -C /data

# 删除数据卷（谨慎！）
docker volume rm lisuan-mysql-data
```

### 使用 mysqldump 备份

```bash
# 备份整个数据库
docker exec lisuan-mysql mysqldump -u root -pRootPassword123! lisuan_system > backup_$(date +%Y%m%d).sql

# 备份并压缩
docker exec lisuan-mysql mysqldump -u root -pRootPassword123! lisuan_system | gzip > backup_$(date +%Y%m%d).sql.gz

# 从备份恢复
docker exec -i lisuan-mysql mysql -u root -pRootPassword123! lisuan_system < backup_20250203.sql
```

---

## 常用命令

### 容器管理

```bash
# 启动容器
docker start lisuan-mysql

# 停止容器
docker stop lisuan-mysql

# 重启容器
docker restart lisuan-mysql

# 查看容器状态
docker ps -a | grep lisuan-mysql

# 查看容器日志
docker logs lisuan-mysql

# 实时查看日志
docker logs -f lisuan-mysql

# 删除容器（数据保留）
docker rm lisuan-mysql

# 删除容器和数据卷（谨慎！）
docker rm -v lisuan-mysql
```

### 进入容器

```bash
# 进入 MySQL 命令行
docker exec -it lisuan-mysql mysql -u cashier -pYourStrongPassword123! lisuan_system

# 进入容器 Bash
docker exec -it lisuan-mysql bash

# 查看容器资源使用
docker stats lisuan-mysql
```

### 数据库操作

```bash
# 执行 SQL 文件
docker exec -i lisuan-mysql mysql -u root -pRootPassword123! < init.sql

# 查看数据库列表
docker exec lisuan-mysql mysql -u root -pRootPassword123! -e "SHOW DATABASES;"

# 查看表列表
docker exec lisuan-mysql mysql -u root -pRootPassword123! lisuan_system -e "SHOW TABLES;"

# 查看表结构
docker exec lisuan-mysql mysql -u root -pRootPassword123! lisuan_system -e "DESCRIBE users;"

# 查看表数据
docker exec lisuan-mysql mysql -u root -pRootPassword123! lisuan_system -e "SELECT * FROM users LIMIT 10;"
```

---

## 更新配置文件

### 创建自定义 MySQL 配置文件

1. **创建配置文件** `my.cnf`:
```ini
[mysqld]
# 字符集
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

# 连接配置
max_connections=200
connect_timeout=10
wait_timeout=600
interactive_timeout=600

# InnoDB 配置
innodb_buffer_pool_size=1G
innodb_log_file_size=256M
innodb_flush_log_at_trx_commit=2

# 查询缓存（MySQL 8.0 已移除）
# query_cache_size=0

# 慢查询日志
slow_query_log=1
long_query_time=2
slow_query_log_file=/var/log/mysql/slow.log

# 二进制日志
log-bin=mysql-bin
binlog_expire_logs_seconds=604800

# 时区
default-time-zone='+08:00'

# 认证插件
default-authentication-plugin=mysql_native_password

[client]
default-character-set=utf8mb4
```

2. **修改 docker-compose.yml** 挂载配置文件：
```yaml
services:
  mysql:
    # ... 其他配置 ...
    volumes:
      - lisuan-mysql-data:/var/lib/mysql
      - ./mysql-init:/docker-entrypoint-initdb.d
      - ./my.cnf:/etc/mysql/conf.d/custom.cnf:ro  # 添加这行
```

3. **重启容器应用配置**
```bash
docker-compose down
docker-compose up -d
```

---

## 故障排查

### 问题 1: 容器无法启动

**错误信息**: `Port 3306 is already in use`

**解决方案**:
```bash
# 查看占用 3306 端口的进程
# Windows
netstat -ano | findstr :3306

# macOS/Linux
lsof -i :3306

# 更改端口映射
docker run -d -p 3307:3306 ...  # 使用 3307 端口
```

### 问题 2: 无法连接到数据库

**错误信息**: `Communications link failure`

**解决方案**:
```bash
# 1. 检查容器是否运行
docker ps | grep lisuan-mysql

# 2. 检查容器日志
docker logs lisuan-mysql

# 3. 检查网络连通性
telnet localhost 3306

# 4. 测试连接
docker exec lisuan-mysql mysql -u cashier -pYourStrongPassword123! -e "SELECT 1;"
```

### 问题 3: 权限错误

**错误信息**: `Access denied for user 'cashier'@'localhost'`

**解决方案**:
```bash
# 重新创建用户
docker exec -it lisuan-mysql mysql -u root -pRootPassword123! <<EOF
DROP USER IF EXISTS 'cashier'@'%';
CREATE USER 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON lisuan_system.* TO 'cashier'@'%';
FLUSH PRIVILEGES;
EOF
```

### 问题 4: 数据丢失

**原因**: 删除容器时使用了 `-v` 标志或没有正确挂载数据卷

**解决方案**:
```bash
# 检查数据卷是否存在
docker volume ls | grep lisuan-mysql-data

# 查看数据卷内容
docker run --rm -v lisuan-mysql-data:/data alpine ls -la /data

# 恢复数据（如果有备份）
docker run --rm \
  -v lisuan-mysql-data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/lisuan-mysql-backup.tar.gz -C /data
```

### 问题 5: 容器启动后立即退出

**原因**: 配置错误或权限问题

**解决方案**:
```bash
# 查看详细日志
docker logs lisuan-mysql --tail 100

# 检查容器状态
docker inspect lisuan-mysql

# 以交互模式运行查看错误
docker run -it --rm mysql:8.4 --verbose --help
```

---

## 应用配置

修改应用配置文件 `config/database.properties`:

```properties
# 本机 Docker MySQL
db.url=jdbc:mysql://localhost:3306/lisuan_system?useSSL=false&serverTimezone=Asia/Shanghai

# 如果使用其他端口
# db.url=jdbc:mysql://localhost:3307/lisuan_system?useSSL=false&serverTimezone=Asia/Shanghai

# Docker 容器 IP（跨机器访问）
# db.url=jdbc:mysql://192.168.1.100:3306/lisuan_system?useSSL=false&serverTimezone=Asia/Shanghai

# 用户名和密码
db.username=cashier
db.password=YourStrongPassword123!

# 连接池大小
db.pool.size=10
```

---

## 性能优化

### Docker 容器资源限制

```yaml
# docker-compose.yml
services:
  mysql:
    # ... 其他配置 ...
    deploy:
      resources:
        limits:
          cpus: '2.0'      # 限制使用 2 个 CPU 核心
          memory: 2G      # 限制内存使用 2GB
        reservations:
          cpus: '1.0'      # 保留 1 个 CPU 核心
          memory: 1G      # 保留 1GB 内存
```

### MySQL 性能调优

```ini
# my.cnf
[mysqld]
# 内存配置（根据服务器内存调整）
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M

# 连接数
max_connections = 200
thread_cache_size = 50

# 临时表
tmp_table_size = 64M
max_heap_table_size = 64M

# 慢查询
slow_query_log = 1
long_query_time = 2
```

---

## 安全建议

1. **修改默认密码**
   ```bash
   docker exec lisuan-mysql mysql -u root -p \
     -e "ALTER USER 'root'@'%' IDENTIFIED BY 'NewStrongPassword123!';"
   ```

2. **限制网络访问**
   ```yaml
   # docker-compose.yml
   ports:
     - "127.0.0.1:3306:3306"  # 只允许本机访问
   ```

3. **使用 secrets 管理密码**（Docker Swarm）
   ```bash
   echo "YourStrongPassword123!" | docker secret create mysql_password -
   ```

4. **定期备份**
   ```bash
   # 添加到 crontab
   0 2 * * * docker exec lisuan-mysql mysqldump -u root -pRootPassword123! lisuan_system | gzip > /backup/cashier_$(date +\%Y\%m\%d).sql.gz
   ```

5. **更新镜像**
   ```bash
   docker pull mysql:8.4
   docker-compose down
   docker-compose up -d
   ```

---

## 升级 MySQL 版本

```bash
# 1. 备份数据
docker exec lisuan-mysql mysqldump -u root -pRootPassword123! --all-databases > backup.sql

# 2. 停止并删除旧容器
docker-compose down

# 3. 更新镜像
docker pull mysql:8.4

# 4. 启动新容器
docker-compose up -d

# 5. 恢复数据
docker exec -i lisuan-mysql mysql -u root -pRootPassword123! < backup.sql
```

---

## 附录

### Docker Compose 完整示例

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.4
    container_name: lisuan-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: RootPassword123!
      MYSQL_DATABASE: lisuan_system
      MYSQL_USER: cashier
      MYSQL_PASSWORD: YourStrongPassword123!
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - lisuan-mysql-data:/var/lib/mysql
      - ./mysql-init:/docker-entrypoint-initdb.d:ro
      - ./my.cnf:/etc/mysql/conf.d/custom.cnf:ro
      - ./mysql-backup:/backup
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-authentication-plugin=mysql_native_password
      - --max-connections=200
      - --innodb-buffer-pool-size=1G
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-pRootPassword123!"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    networks:
      - lisuan-network

volumes:
  lisuan-mysql-data:
    driver: local

networks:
  lisuan-network:
    driver: bridge
```

### 数据库管理工具推荐

我们推荐使用 **DBeaver** 作为数据库管理工具。DBeaver 是一款免费、开源、跨平台的通用数据库工具，支持 MySQL、PostgreSQL、Oracle 等多种数据库。

#### 为什么选择 DBeaver？

- ✅ 完全免费且开源（Apache 2.0 许可证）
- ✅ 跨平台支持（Windows、macOS、Linux）
- ✅ 现代化 UI，操作直观
- ✅ 强大的 SQL 编辑器和代码补全
- ✅ ER 图可视化数据库结构
- ✅ 数据导入/导出功能
- ✅ 支持多种数据库（MySQL、PostgreSQL、Oracle、SQL Server 等）
- ✅ 安全性高，支持 SSH 隧道
- ✅ 活跃的社区支持

#### 下载 DBeaver

根据您的操作系统下载对应版本：

- **Windows**: https://dbeaver.io/download/
- **macOS**: https://dbeaver.io/download/（支持 Intel 和 Apple Silicon）
- **Linux**: https://dbeaver.io/download/（支持 .deb、.rpm、AppImage）

#### 安装 DBeaver

**Windows**:
1. 下载 DBeaver 安装程序（.exe 或 .msi）
2. 双击运行安装程序
3. 按照安装向导完成安装
4. 启动 DBeaver

**macOS**:
1. 下载 DBeaver DMG 文件
2. 双击 DMG 文件打开
3. 将 DBeaver 图标拖拽到 Applications 文件夹
4. 从应用程序启动 DBeaver

**Linux**:
```bash
# Ubuntu/Debian (.deb)
sudo dpkg -i dbeaver-ce_latest_stable_deb_amd64.deb
sudo apt-get install -f

# Fedora/CentOS/RHEL (.rpm)
sudo rpm -ivh dbeaver-ce_latest_stable_rpm_x86_64.rpm

# AppImage（通用）
chmod +x dbeaver-ce-latest-stable-linux.gtk.x86_64.noarch.rpm
./dbeaver-ce-latest-stable-linux.gtk.x86_64.noarch.rpm
```

#### 配置 Docker MySQL 连接

1. 启动 DBeaver
2. 点击左上角的"新建数据库连接"按钮（或使用快捷键 `Ctrl+Shift+N` / `Cmd+Shift+N`）
3. 在左侧选择 **MySQL**
4. 点击"下一步"

**连接配置**：
- **主机**: `localhost`
- **端口**: `3306`
- **数据库**: `lisuan_system`
- **用户名**: `cashier`
- **密码**: `YourStrongPassword123!`（请替换为您设置的密码）
- **驱动**: 使用默认驱动（MySQL Connector/J）

**高级配置（可选）**：
- 点击"驱动设置"可以查看和修改驱动配置
- 建议勾选"连接时自动创建数据库"（如果数据库不存在）
- 可以设置连接池大小、超时时间等参数

5. 点击"测试连接"验证配置是否正确
6. 测试成功后，点击"完成"保存连接

#### DBeaver 基本使用

**浏览数据库结构**：
1. 在左侧"数据库导航器"中找到您的连接
2. 展开连接，查看所有数据库
3. 展开 `lisuan_system` 数据库，查看所有表
4. 右键点击表，可以查看表结构、数据、索引等

**执行 SQL 查询**：
1. 右键点击连接或数据库，选择"SQL 编辑器"
2. 在 SQL 编辑器中输入 SQL 语句
3. 点击工具栏的"执行"按钮（或按 `F5`）执行查询
4. 查询结果会显示在下方的结果面板中

**编辑数据**：
1. 在数据库导航器中右键点击表
2. 选择"查看数据"
3. 在结果面板中可以直接编辑数据
4. 点击"保存"按钮提交更改

**导入数据**：
1. 右键点击表，选择"导入数据"
2. 选择要导入的文件（支持 CSV、Excel 等格式）
3. 配置字段映射和导入选项
4. 点击"开始"导入数据

**导出数据**：
1. 右键点击表，选择"导出数据"
2. 选择导出格式（CSV、Excel、SQL 等）
3. 配置导出选项
4. 点击"开始"导出数据

**查看 ER 图**：
1. 右键点击数据库或多个表
2. 选择"创建 ER 图"
3. 可视化查看表之间的关系

#### 常用快捷键

- `Ctrl+Shift+N` / `Cmd+Shift+N` - 新建数据库连接
- `F5` - 执行 SQL 查询
- `Ctrl+Space` - SQL 代码补全
- `Ctrl+F` - 在 SQL 编辑器中查找
- `Ctrl+H` - 在 SQL 编辑器中替换
- `Ctrl+S` - 保存 SQL 脚本
- `Ctrl+W` - 关闭当前标签页

#### 故障排查

**连接失败**：
1. 确认 Docker MySQL 容器正在运行：`docker ps`
2. 确认端口映射正确：`docker-compose ps`
3. 检查防火墙设置
4. 确认用户名和密码正确

**驱动加载失败**：
1. 点击"驱动设置"
2. 点击"下载/更新"按钮
3. 下载并安装最新驱动

**字符编码问题**：
1. 在连接设置中，点击"驱动设置"
2. 在"连接参数"中添加：`useUnicode=true&characterEncoding=utf8mb4`

#### 其他替代方案

如果您需要其他数据库管理工具，也可以考虑：

- **HeidiSQL**（仅 Windows）：轻量级，适合 MySQL 和 MariaDB
- **MySQL Workbench**（官方工具）：功能全面，但仅支持 MySQL
- **Navicat**（商业软件）：功能强大，但需要付费
- **DataGrip**（JetBrains）：专业的 IDE 集成数据库工具，需要付费

### 常用 Docker 命令速查

```bash
# 拉取镜像
docker pull mysql:8.4

# 运行容器
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=pass mysql:8.4

# 停止容器
docker stop mysql

# 启动容器
docker start mysql

# 重启容器
docker restart mysql

# 删除容器
docker rm mysql

# 查看日志
docker logs mysql
docker logs -f mysql  # 实时
docker logs --tail 100 mysql  # 最后 100 行

# 进入容器
docker exec -it mysql bash

# 查看容器详情
docker inspect mysql

# 查看资源使用
docker stats mysql

# 清理未使用的资源
docker system prune -a
```

---

## 技术支持

如遇问题：
1. 查看容器日志: `docker logs lisuan-mysql`
2. 查看本文档的故障排查部分
3. 检查 Docker 官方文档: https://docs.docker.com/
4. 检查 MySQL 官方文档: https://dev.mysql.com/doc/
