# Docker 数据库配置说明

## 📋 配置文件结构

```
项目根目录/
├── .env                    # 环境变量（生产配置，不提交）
├── .env.example           # 环境变量模板（提交）
├── docker-compose.yml     # Docker Compose 配置
├── install.sh             # 安装脚本（读取 .env）
└── start.sh               # 启动脚本（读取 .env）
```

## 🚀 快速开始

### 方法一：使用 .env 文件（推荐）

```bash
# 1. 复制配置模板
cp .env.example .env

# 2. 编辑 .env 文件，修改密码
vim .env

# 3. 运行安装脚本
./install.sh
```

### 方法二：使用环境变量

```bash
# 设置环境变量
export MYSQL_ROOT_PASSWORD="YourSecurePassword"
export MYSQL_PASSWORD="YourAppPassword"

# 运行安装脚本
./install.sh
```

## 📝 配置优先级

1. 环境变量（最高优先级）
2. `.env` 文件
3. 脚本默认值（最低优先级）

## 🔧 Docker 常用命令

```bash
# 启动
docker compose up -d

# 查看日志
docker compose logs -f mysql

# 停止
docker compose stop

# 重启
docker compose restart

# 完全删除（包括数据）
docker compose down -v

# 进入容器
docker exec -it cashier-mysql bash
```

## 📊 配置变量说明

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_TYPE` | docker | 数据库类型：docker/local/none |
| `MYSQL_CONTAINER_NAME` | cashier-mysql | 容器名称 |
| `MYSQL_IMAGE` | mysql:8.4 | MySQL 镜像版本 |
| `MYSQL_ROOT_PASSWORD` | - | Root 密码（必填） |
| `MYSQL_DATABASE` | cashier_system | 数据库名称 |
| `MYSQL_USER` | cashier | 应用用户名 |
| `MYSQL_PASSWORD` | - | 应用密码（必填） |
| `DB_HOST` | localhost | 数据库主机 |
| `DB_PORT` | 3306 | 数据库端口 |

## ⚠️ 安全注意事项

1. **不要提交 .env 文件到版本控制**
2. **生产环境必须修改默认密码**
3. **定期备份数据库**
