# Docker MySQL 部署快速指南

本目录包含使用 Docker 部署 MySQL 8.4 的完整配置。

## 快速开始

### Windows 用户

```cmd
# 1. 启动 Docker Desktop

# 2. 双击运行启动脚本
start-mysql.bat

# 3. 等待启动完成
```

### macOS/Linux 用户

```bash
# 1. 添加执行权限
chmod +x docker/start-mysql.sh

# 2. 运行启动脚本
./docker/start-mysql.sh
```

## 手动启动

```bash
# 启动容器
docker-compose up -d

# 查看日志
docker-compose logs -f mysql

# 停止容器
docker-compose stop

# 重启容器
docker-compose restart
```

## 配置说明

| 项目 | 值 |
|-----|---|
| 容器名称 | lisuan-mysql |
| 端口 | 3306 |
| 数据库 | lisuan_system |
| 用户名 | lisuan |
| 密码 | ⚠️ 请修改为您的密码 |
| Root 密码 | ⚠️ 请修改为您的密码 |

> **安全提示**: 请在 `docker-compose.yml` 和 `mysql-init/` 目录中的 SQL 脚本中修改默认密码！

## 访问服务

- **MySQL**: `localhost:3306`
- **数据库管理工具**: 推荐使用 DBeaver
  - 下载地址: https://dbeaver.io/download/
  - 连接配置:
    - 主机: localhost
    - 端口: 3306
    - 数据库: lisuan_system
    - 用户名: lisuan
    - 密码: (您设置的密码)

## 数据持久化

数据存储在 Docker 卷 `lisuan-mysql-data` 中，即使删除容器也不会丢失。

## 备份数据

```bash
# 备份到 docker/mysql-backup 目录
docker exec lisuan-mysql mysqldump -u root -pYOUR_ROOT_PASSWORD lisuan_system > docker/mysql-backup/backup_$(date +%Y%m%d).sql

# 恢复备份
docker exec -i lisuan-mysql mysql -u root -pYOUR_ROOT_PASSWORD lisuan_system < docker/mysql-backup/backup_20250203.sql
```

## 配置文件

- **docker-compose.yml.example**: Docker Compose 配置示例（复制为 docker-compose.yml）
- **docker/my.cnf**: MySQL 配置文件
- **docker/mysql-init/**: 初始化 SQL 脚本
- **docker/mysql-backup/**: 备份文件目录

## 故障排查

```bash
# 查看容器状态
docker ps -a | grep lisuan-mysql

# 查看容器日志
docker logs lisuan-mysql

# 进入容器
docker exec -it lisuan-mysql bash

# 重启容器
docker-compose restart mysql

# 完全删除并重建
docker-compose down -v
docker-compose up -d
```

## 下一步

1. 修改 `config/database.properties` 配置数据库连接
2. 启动收银系统应用
3. 应用会自动迁移数据到 MySQL

## 更多信息

详细文档请参阅：
- [Docker MySQL 完整指南](../docker-mysql-setup.md)
- [MySQL 部署文档](../docs/MYSQL_SETUP.md)