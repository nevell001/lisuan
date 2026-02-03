# ✅ MySQL Docker 部署完成！

## 当前状态

### 🎉 成功部分
- ✅ MySQL 8.0 容器运行正常
- ✅ phpMyAdmin 可访问 (http://localhost:8080)
- ✅ root 用户已创建并设置密码
- ✅ cashier 用户已创建并授权
- ✅ 数据库 `cashier_system` 已创建
- ✅ 配置文件已生成

### ⚠️ 需要注意的事项

#### Apple Silicon Mac 用户

由于Docker Desktop在Apple Silicon上的网络限制，需要在配置中使用特殊的主机名：

**当前配置已更新为**: `host.docker.internal` (config/database.properties)

如果连接仍有问题，请尝试：

1. **方案1**: 使用 host.docker.internal (已配置)
2. **方案2**: 使用容器的实际IP地址：
   ```bash
   # 获取容器IP
   docker inspect cashier-mysql | grep IPAddress
   ```

3. **方案3**: 使用网络模式：
   ```bash
   # 停止容器
   docker compose down

   # 修改 docker-compose.yml，在服务定义中添加：
   # extra_hosts:
   #   - "mysql.local:host-gateway"

   # 重新启动
   docker compose up -d
   ```

#### Linux/Windows 用户

配置使用 `localhost` 即可正常连接。

---

## 📋 下一步操作

### 1. 验证 MySQL 连接

**从命令行测试**:
```bash
# 进入MySQL容器
docker exec -it cashier-mysql bash

# 连接MySQL
mysql --socket=/var/run/mysqld/mysqld.sock -u cashier -pYourStrongPassword123!

# 查看数据库
SHOW DATABASES;
USE cashier_system;
SHOW TABLES;
```

**通过 phpMyAdmin**:
1. 访问 http://localhost:8080
2. 用户名: `root`
3. 密码: `RootPassword123!`

### 2. 启动收银系统

```bash
# 方式1: 使用 Maven
mvn javafx:run

# 方式2: 使用 JAR
java -jar target/cashier-system-fx.jar
```

### 3. 数据迁移

应用启动时会自动：
1. 检测数据库表结构
2. 自动创建所有表（users, products, members等）
3. 从 .txt 文件迁移数据到 MySQL
4. 备份原有数据到 `data/backup_<timestamp>/`

---

## 🔑 连接信息

| 项目 | 值 |
|-----|---|
| **主机** | `host.docker.internal` (Apple Silicon) 或 `localhost` (Intel/Windows) |
| **端口** | `3306` |
| **数据库** | `cashier_system` |
| **用户名** | `cashier` |
| **密码** | `YourStrongPassword123!` |
| **Root 密码** | `RootPassword123!` |

---

## 🛠️ 常用命令

### Docker 管理
```bash
# 查看容器状态
docker ps | grep cashier

# 查看日志
docker logs -f cashier-mysql

# 进入MySQL容器
docker exec -it cashier-mysql bash

# 通过套接字连接MySQL（推荐）
docker exec -it cashier-mysql mysql --socket=/var/run/mysqld/mysqld.sock -u cashier -p

# 停止容器
docker compose stop

# 启动容器
docker compose start

# 重启容器
docker compose restart

# 完全删除（数据丢失！）
docker compose down -v
```

### 数据库管理
```sql
-- 在容器内或通过phpMyAdmin执行

-- 查看所有表
SHOW TABLES;

-- 查看用户
SELECT user, host, plugin FROM mysql.user;

-- 查看表结构
DESCRIBE users;

-- 查看数据
SELECT * FROM users LIMIT 10;

-- 备份数据
mysqldump -u cashier -p cashier_system > backup.sql
```

---

## 📁 文件结构

```
hello/
├── docker-compose.yml          # Docker Compose 配置
├── docker/
│   ├── README.md              # Docker 快速指南
│   ├── start-mysql.sh         # Linux/macOS 启动脚本
│   ├── start-mysql.bat        # Windows 启动脚本
│   ├── mysql-init/            # 初始化 SQL 脚本
│   │   ├── 01-create-user.sql
│   │   └── 02-fix-root.sql
│   ├── mysql-backup/          # 备份目录
│   └── create-cashier-user.sql
├── config/
│   ├── database.properties.example
│   └── database.properties    # ✅ 已生成（已配置 host.docker.internal）
└── docs/
    ├── MYSQL_SETUP.md         # MySQL 部署完整指南
    └── docker-mysql-setup.md  # Docker 部署完整指南
```

---

## ⚡ 快速命令

```bash
# 启动MySQL
docker compose up -d

# 查看状态
docker ps | grep cashier

# 测试连接
docker exec cashier-mysql mysql --socket=/var/run/mysqld/mysqld.sock -u cashier -pYourStrongPassword123! -e "SELECT 1"

# 启动应用
mvn javafx:run

# 查看日志
docker logs -f cashier-mysql
```

---

## 🐛 故障排查

### 问题1: 无法连接到MySQL

**错误**: `Communications link failure`

**解决方案**:
1. 确认容器正在运行: `docker ps | grep cashier`
2. 检查端口: `docker port cashier-mysql`
3. Apple Silicon: 使用 `host.docker.internal`
4. 其他平台: 使用 `localhost`

### 问题2: 密码认证失败

**错误**: `Access denied for user 'cashier'`

**解决方案**:
```bash
# 重置cashier用户密码
docker exec cashier-mysql mysql --socket=/var/run/mysqld/mysqld.sock -u root -pRootPassword123! <<EOF
ALTER USER 'cashier'@'%' IDENTIFIED BY 'YourNewPassword123!';
FLUSH PRIVILEGES;
EOF
```

### 问题3: 容器重启循环

**解决方案**:
```bash
# 查看日志找出问题
docker logs cashier-mysql

# 删除并重新创建
docker compose down -v
docker volume prune -f
docker compose up -d
```

---

## 📚 更多信息

- [Docker 快速指南](docker/README.md)
- [Docker 完整部署文档](docker-mysql-setup.md)
- [MySQL 部署文档](docs/MYSQL_SETUP.md)

---

## ✅ 准备就绪！

MySQL 数据库已经完全配置好，可以启动收银系统进行数据迁移了！
