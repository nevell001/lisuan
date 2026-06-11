#!/bin/bash
#
# Docker MySQL 初始化脚本
# 用于初始化 MySQL 数据库和更新应用配置
#

set -e

echo "========================================="
echo "  MySQL 数据库初始化脚本"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

# 检查 docker compose 是否可用
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    echo -e "${RED}错误: 未找到 docker-compose 或 docker compose 命令${NC}"
    exit 1
fi

# 检查容器是否已运行
if ! $DOCKER_COMPOSE ps mysql | grep -q "Up"; then
    echo -e "${YELLOW}MySQL 容器未运行，正在启动...${NC}"
    $DOCKER_COMPOSE up -d mysql

    echo -e "${YELLOW}等待 MySQL 启动...${NC}"
    for i in {1..30}; do
        if $DOCKER_COMPOSE exec -T mysql mysqladmin ping -h localhost --silent; then
            echo -e "${GREEN}✓ MySQL 启动成功${NC}"
            break
        fi
        if [ $i -eq 30 ]; then
            echo -e "${RED}错误: MySQL 启动超时${NC}"
            exit 1
        fi
        sleep 2
    done
else
    echo -e "${GREEN}✓ MySQL 容器已运行${NC}"
fi

# MySQL 配置 - ⚠️ 请修改以下密码！
# 可以通过环境变量覆盖：
#   export MYSQL_ROOT_PASSWORD="your_root_password"
#   export MYSQL_PASSWORD="your_app_password"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-YOUR_ROOT_PASSWORD_HERE}"
MYSQL_HOST="localhost"
MYSQL_PORT="3306"
MYSQL_DATABASE="lisuan_system"
MYSQL_USER="lisuan"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-YOUR_CASHIER_PASSWORD_HERE}"

# 检查是否使用了默认密码
if [[ "$MYSQL_ROOT_PASSWORD" == "YOUR_ROOT_PASSWORD_HERE" ]] || [[ "$MYSQL_PASSWORD" == "YOUR_CASHIER_PASSWORD_HERE" ]]; then
    echo ""
    echo -e "${YELLOW}⚠️  安全警告：您正在使用默认密码占位符！${NC}"
    echo -e "${YELLOW}   请设置环境变量或在脚本中修改密码：${NC}"
    echo "   export MYSQL_ROOT_PASSWORD='your_secure_password'"
    echo "   export MYSQL_PASSWORD='your_secure_password'"
    echo ""
    read -p "是否继续？(y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "已取消操作"
        exit 1
    fi
fi

echo ""
echo "========================================="
echo "  配置 MySQL 认证方式"
echo "========================================="

# 修改 root 用户认证方式为 mysql_native_password（兼容性更好）
$DOCKER_COMPOSE exec -T mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';
    ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';
    ALTER USER 'root'@'172.%.%.%' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';
    FLUSH PRIVILEGES;
" 2>/dev/null

echo -e "${GREEN}✓ MySQL 认证方式已设置为 mysql_native_password${NC}"

echo ""
echo "========================================="
echo "  创建应用专用用户"
echo "========================================="

# 创建 cashier 用户（更安全的做法）
$DOCKER_COMPOSE exec -T mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "
    CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED WITH mysql_native_password BY '${MYSQL_PASSWORD}';
    GRANT ALL PRIVILEGES ON ${MYSQL_DATABASE}.* TO '${MYSQL_USER}'@'%';
    FLUSH PRIVILEGES;
" 2>/dev/null

echo -e "${GREEN}✓ 应用专用用户已创建: ${MYSQL_USER}${NC}"

echo ""
echo "========================================="
echo "  更新应用配置文件"
echo "========================================="

# 检查配置文件是否存在
if [ ! -f "config/database.properties" ]; then
    echo "创建默认配置文件..."
    mkdir -p config
fi

# 更新配置文件
cat > config/database.properties << EOF
# Database Configuration
db.url=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
db.username=${MYSQL_USER}
db.password=${MYSQL_PASSWORD}
db.pool.size=10
db.connection.timeout=30000
db.idle.timeout=600000
db.max.lifetime=1800000
EOF

echo -e "${GREEN}✓ 应用配置文件已更新${NC}"
echo ""
echo "配置信息："
echo "  - 数据库主机: ${MYSQL_HOST}:${MYSQL_PORT}"
echo "  - 数据库名称: ${MYSQL_DATABASE}"
echo "  - 应用用户: ${MYSQL_USER}"
echo "  - 密码: (已配置)"

echo ""
echo "========================================="
echo "  测试数据库连接"
echo "========================================="

# 测试连接
if $DOCKER_COMPOSE exec -T mysql mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD} -e "SELECT 'Connection successful!' AS Status;" 2>/dev/null; then
    echo -e "${GREEN}✓ 数据库连接测试成功${NC}"
else
    echo -e "${RED}✗ 数据库连接测试失败${NC}"
    echo "请检查配置并重试"
    exit 1
fi

echo ""
echo "========================================="
echo -e "${GREEN}  初始化完成！${NC}"
echo "========================================="
echo ""
echo "现在可以启动应用了："
echo "  ./start.sh"
echo ""
echo "提示："
echo "  - root 用户密码: (请使用您设置的密码)"
echo "  - 应用用户: ${MYSQL_USER}"
echo "  - 应用密码: (请使用您设置的密码)"
echo "  - 推荐工具: DBeaver (https://dbeaver.io/download/)"
echo ""