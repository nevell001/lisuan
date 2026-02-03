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
  --name cashier-mysql \
  --restart unless-stopped \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=RootPassword123! \
  -e MYSQL_DATABASE=cashier_system \
  -e MYSQL_USER=cashier \
  -e MYSQL_PASSWORD=YourStrongPassword123! \
  -v cashier-mysql-data:/var/lib/mysql \
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
    container_name: cashier-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: RootPassword123!
      MYSQL_DATABASE: cashier_system
      MYSQL_USER: cashier
      MYSQL_PASSWORD: YourStrongPassword123!
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - cashier-mysql-data:/var/lib/mysql
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
  cashier-mysql-data:
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
| `MYSQL_DATABASE` | 创建的数据库名 | cashier_system |
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
-v cashier-mysql-data:/var/lib/mysql
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
docker exec -it cashier-mysql mysql -u root -p

# 或使用 bash
docker exec -it cashier-mysql bash
mysql -u root -p
```

然后执行 SQL：

```sql
-- 创建专用用户
CREATE USER 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS cashier_system
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- 授予权限
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';

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
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

3. **修改 docker-compose.yml** 添加卷挂载：
```yaml
volumes:
  - cashier-mysql-data:/var/lib/mysql
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
docker volume inspect cashier-mysql-data

# 备份数据卷
docker run --rm \
  -v cashier-mysql-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/cashier-mysql-backup.tar.gz -C /data .

# 恢复数据卷
docker run --rm \
  -v cashier-mysql-data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/cashier-mysql-backup.tar.gz -C /data

# 删除数据卷（谨慎！）
docker volume rm cashier-mysql-data
```

### 使用 mysqldump 备份

```bash
# 备份整个数据库
docker exec cashier-mysql mysqldump -u root -pRootPassword123! cashier_system > backup_$(date +%Y%m%d).sql

# 备份并压缩
docker exec cashier-mysql mysqldump -u root -pRootPassword123! cashier_system | gzip > backup_$(date +%Y%m%d).sql.gz

# 从备份恢复
docker exec -i cashier-mysql mysql -u root -pRootPassword123! cashier_system < backup_20250203.sql
```

---

## 常用命令

### 容器管理

```bash
# 启动容器
docker start cashier-mysql

# 停止容器
docker stop cashier-mysql

# 重启容器
docker restart cashier-mysql

# 查看容器状态
docker ps -a | grep cashier-mysql

# 查看容器日志
docker logs cashier-mysql

# 实时查看日志
docker logs -f cashier-mysql

# 删除容器（数据保留）
docker rm cashier-mysql

# 删除容器和数据卷（谨慎！）
docker rm -v cashier-mysql
```

### 进入容器

```bash
# 进入 MySQL 命令行
docker exec -it cashier-mysql mysql -u cashier -pYourStrongPassword123! cashier_system

# 进入容器 Bash
docker exec -it cashier-mysql bash

# 查看容器资源使用
docker stats cashier-mysql
```

### 数据库操作

```bash
# 执行 SQL 文件
docker exec -i cashier-mysql mysql -u root -pRootPassword123! < init.sql

# 查看数据库列表
docker exec cashier-mysql mysql -u root -pRootPassword123! -e "SHOW DATABASES;"

# 查看表列表
docker exec cashier-mysql mysql -u root -pRootPassword123! cashier_system -e "SHOW TABLES;"

# 查看表结构
docker exec cashier-mysql mysql -u root -pRootPassword123! cashier_system -e "DESCRIBE users;"

# 查看表数据
docker exec cashier-mysql mysql -u root -pRootPassword123! cashier_system -e "SELECT * FROM users LIMIT 10;"
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
      - cashier-mysql-data:/var/lib/mysql
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
docker ps | grep cashier-mysql

# 2. 检查容器日志
docker logs cashier-mysql

# 3. 检查网络连通性
telnet localhost 3306

# 4. 测试连接
docker exec cashier-mysql mysql -u cashier -pYourStrongPassword123! -e "SELECT 1;"
```

### 问题 3: 权限错误

**错误信息**: `Access denied for user 'cashier'@'localhost'`

**解决方案**:
```bash
# 重新创建用户
docker exec -it cashier-mysql mysql -u root -pRootPassword123! <<EOF
DROP USER IF EXISTS 'cashier'@'%';
CREATE USER 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
FLUSH PRIVILEGES;
EOF
```

### 问题 4: 数据丢失

**原因**: 删除容器时使用了 `-v` 标志或没有正确挂载数据卷

**解决方案**:
```bash
# 检查数据卷是否存在
docker volume ls | grep cashier-mysql-data

# 查看数据卷内容
docker run --rm -v cashier-mysql-data:/data alpine ls -la /data

# 恢复数据（如果有备份）
docker run --rm \
  -v cashier-mysql-data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/cashier-mysql-backup.tar.gz -C /data
```

### 问题 5: 容器启动后立即退出

**原因**: 配置错误或权限问题

**解决方案**:
```bash
# 查看详细日志
docker logs cashier-mysql --tail 100

# 检查容器状态
docker inspect cashier-mysql

# 以交互模式运行查看错误
docker run -it --rm mysql:8.4 --verbose --help
```

---

## 应用配置

修改应用配置文件 `config/database.properties`:

```properties
# 本机 Docker MySQL
db.url=jdbc:mysql://localhost:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai

# 如果使用其他端口
# db.url=jdbc:mysql://localhost:3307/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai

# Docker 容器 IP（跨机器访问）
# db.url=jdbc:mysql://192.168.1.100:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai

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
   docker exec cashier-mysql mysql -u root -p \
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
   0 2 * * * docker exec cashier-mysql mysqldump -u root -pRootPassword123! cashier_system | gzip > /backup/cashier_$(date +\%Y\%m\%d).sql.gz
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
docker exec cashier-mysql mysqldump -u root -pRootPassword123! --all-databases > backup.sql

# 2. 停止并删除旧容器
docker-compose down

# 3. 更新镜像
docker pull mysql:8.4

# 4. 启动新容器
docker-compose up -d

# 5. 恢复数据
docker exec -i cashier-mysql mysql -u root -pRootPassword123! < backup.sql
```

---

## 附录

### Docker Compose 完整示例

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.4
    container_name: cashier-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: RootPassword123!
      MYSQL_DATABASE: cashier_system
      MYSQL_USER: cashier
      MYSQL_PASSWORD: YourStrongPassword123!
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - cashier-mysql-data:/var/lib/mysql
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
      - cashier-network

  # 可选：phpMyAdmin 管理界面
  phpmyadmin:
    image: phpmyadmin/phpmyadmin:latest
    container_name: cashier-phpmyadmin
    restart: unless-stopped
    environment:
      PMA_HOST: mysql
      PMA_PORT: 3306
      PMA_USER: root
      PMA_PASSWORD: RootPassword123!
    ports:
      - "8080:80"
    depends_on:
      - mysql
    networks:
      - cashier-network

volumes:
  cashier-mysql-data:
    driver: local

networks:
  cashier-network:
    driver: bridge
```

访问 phpMyAdmin: http://localhost:8080

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
1. 查看容器日志: `docker logs cashier-mysql`
2. 查看本文档的故障排查部分
3. 检查 Docker 官方文档: https://docs.docker.com/
4. 检查 MySQL 官方文档: https://dev.mysql.com/doc/
