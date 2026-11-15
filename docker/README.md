# Docker MySQL 部署快速指南

本目录包含使用 Docker 部署 MySQL 8.4 的完整配置。

## 快速开始

以下命令均在仓库根目录执行（`.env` 与 `docker-compose.yml` 都在根目录）。

### 首次使用：先准备 `.env`

`docker-compose.yml` 要求必须提供两个口令，未提供（通常来自 `.env`）会直接启动失败：

```bash
cp .env.example .env
# Windows 命令提示符下使用：copy .env.example .env
# 编辑 .env，至少把下面两项替换为两个独立的强随机值：
#   MYSQL_ROOT_PASSWORD     root 口令
#   CASHIER_DB_PASSWORD     应用用户 lisuan 的口令（应用也读这一项）
```

### Windows 用户

```cmd
# 1. 启动 Docker Desktop

# 2. 按上一节创建并填写 .env

# 3. 双击运行启动脚本
start-mysql.bat

# 4. 等待初始化完成（首次启动要建库建表，约 1 分钟）
```

> 若脚本提示找不到 `docker-compose`，说明本机只有 Compose v2，可直接执行
> `docker compose up -d mysql`。

### macOS/Linux 用户

```bash
# 1. 按上一节创建并填写 .env

# 2. 添加执行权限
chmod +x docker/start-mysql.sh

# 3. 运行启动脚本
./docker/start-mysql.sh
```

## 手动启动

```bash
# 启动容器（需先准备好 .env）
docker compose up -d mysql

# 查看日志
docker compose logs -f mysql

# 停止容器
docker compose stop

# 重启容器
docker compose restart
```

## 配置说明

| 项目 | 默认值 | 覆盖变量 |
|-----|---|---|
| 容器名称 | lisuan-mysql | `MYSQL_CONTAINER_NAME` |
| 端口 | 仅本机 `127.0.0.1:3306` | `DB_PORT` |
| 数据库 | lisuan_system | `MYSQL_DATABASE` |
| 用户名 | lisuan | `MYSQL_USER` |
| 应用用户密码 | 无默认值，必填 | `CASHIER_DB_PASSWORD`（写在 `.env`） |
| Root 密码 | 无默认值，必填 | `MYSQL_ROOT_PASSWORD`（写在 `.env`） |

> **安全提示**:
> - 项目不提供默认凭据，初始化 SQL 也不创建任何账号；首次启动应用时由向导创建管理员。
> - `.env` 已被 git 忽略，不要提交；两个口令应各自独立。
> - 数据库端口只绑定 `127.0.0.1`，默认不对外开放。
> - 如需把 `.env` 中的口令同步进 MySQL 用户并生成应用配置，可运行 `./docker/docker-init.sh`。

## 访问服务

- **MySQL**: `127.0.0.1:3306`
- **数据库管理工具**: 推荐使用 DBeaver
  - 下载地址: https://dbeaver.io/download/
  - 连接配置:
    - 主机: 127.0.0.1
    - 端口: 3306
    - 数据库: lisuan_system
    - 用户名: lisuan
    - 密码: `.env` 中的 `CASHIER_DB_PASSWORD`

## 数据持久化

数据存储在 Docker 卷 `lisuan-mysql-data` 中，即使删除容器也不会丢失。

初始化 SQL 只会在数据卷为空时执行一次；之后的结构升级由应用启动时的 Java 迁移完成。
如需重新执行初始化脚本，必须先删除数据卷（会丢失全部数据，见“故障排查”）。

## 备份数据

```bash
# 从 .env 读取口令，避免把密码写进命令行（shell history / ps 可见）
set -a; . ./.env; set +a
CONTAINER="${MYSQL_CONTAINER_NAME:-lisuan-mysql}"

# 备份到 backups/sql 目录
docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$CONTAINER" \
  mysqldump -uroot lisuan_system > backups/sql/backup_$(date +%Y%m%d).sql

# 恢复备份
docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$CONTAINER" \
  mysql -uroot lisuan_system < backups/sql/backup_20250203.sql
```

## 配置文件

- **.env**: 数据库/应用口令（从 `.env.example` 复制，不提交）
- **docker-compose.yml**: Docker Compose 配置
- **docker/my.cnf**: MySQL 配置文件
- **docker/mysql-init/**: 初始化 SQL 脚本
- **backups/sql/**: 备份文件目录

## 故障排查

```bash
# 查看容器状态
docker ps -a | grep lisuan-mysql

# 查看容器日志
docker logs lisuan-mysql

# 进入容器
docker exec -it lisuan-mysql bash

# 重启容器
docker compose restart mysql

# 完全删除并重建（⚠️ down -v 会删除数据卷，数据全部丢失）
docker compose down -v
docker compose up -d mysql
```

## 下一步

1. 确认 `.env` 中的 `CASHIER_DB_PASSWORD` 与容器内应用用户口令一致
2. 可选：运行 `./docker/docker-init.sh` 同步 MySQL 用户并生成 `config/database.properties`
3. 启动收银系统应用；首次启动会由向导创建管理员账号
4. 应用启动时会自动补齐/迁移表结构

## 更多信息

详细文档请参阅：
- [Docker 配置文档](../docs/DOCKER_CONFIGURATION.md)
- [MySQL 部署文档](../docs/MYSQL_SETUP.md)
