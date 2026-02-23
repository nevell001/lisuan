#!/bin/bash

# ============================================
# 收银系统 MySQL Docker 快速启动脚本
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  收银系统 MySQL Docker 启动脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检测 docker compose 命令
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
elif command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
else
    echo -e "${RED}错误: 未找到 docker compose 或 docker-compose！${NC}"
    echo "请确保安装了 Docker Desktop 或 Docker Compose"
    exit 1
fi

echo -e "${GREEN}✓ 检测到命令: ${DOCKER_COMPOSE}${NC}"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装！${NC}"
    echo "请先安装 Docker: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# 创建必要的目录
echo -e "${YELLOW}创建必要的目录...${NC}"
mkdir -p docker/mysql-init
mkdir -p docker/mysql-backup
mkdir -p config

# 检查配置文件
if [ ! -f "config/database.properties" ]; then
    echo -e "${YELLOW}创建数据库配置文件...${NC}"
    cp config/database.properties.example config/database.properties
    echo -e "${GREEN}✓ 配置文件已创建: config/database.properties${NC}"
    echo -e "${YELLOW}请编辑此文件并修改数据库连接信息${NC}"
fi

# 检查是否已存在容器
if docker ps -a | grep -q "cashier-mysql"; then
    echo -e "${YELLOW}检测到已存在的 MySQL 容器${NC}"
    read -p "是否删除旧容器并重新创建？(y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}停止并删除旧容器...${NC}"
        $DOCKER_COMPOSE down -v
        echo -e "${GREEN}✓ 旧容器已删除${NC}"
    else
        echo -e "${YELLOW}启动现有容器...${NC}"
        $DOCKER_COMPOSE start
        echo -e "${GREEN}✓ 容器已启动${NC}"
        exit 0
    fi
fi

# 启动 MySQL 容器
echo ""
echo -e "${GREEN}启动 MySQL 容器...${NC}"
$DOCKER_COMPOSE up -d

# 等待 MySQL 启动
echo -e "${YELLOW}等待 MySQL 启动...${NC}"
sleep 10

# 检查容器状态
if docker ps | grep -q "cashier-mysql"; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ MySQL 启动成功！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "数据库连接信息："
    echo -e "  主机: ${GREEN}localhost${NC}"
    echo -e "  端口: ${GREEN}3306${NC}"
    echo -e "  数据库: ${GREEN}cashier_system${NC}"
    echo -e "  用户名: ${GREEN}cashier${NC}"
    echo -e "  密码: ${GREEN}YourStrongPassword123!${NC}"
    echo ""
    echo "管理工具："
    echo -e "  推荐: DBeaver (${GREEN}https://dbeaver.io/download/${NC})"
    echo ""
    echo "常用命令："
    echo "  查看日志: $DOCKER_COMPOSE logs -f mysql"
    echo "  停止容器: $DOCKER_COMPOSE stop"
    echo "  重启容器: $DOCKER_COMPOSE restart"
    echo "  进入容器: docker exec -it cashier-mysql bash"
    echo ""
    echo "下一步："
    echo "  1. 修改 config/database.properties"
    echo "  2. 启动收银系统应用"
    echo "  3. 应用会自动迁移数据到 MySQL"
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}✗ MySQL 启动失败！${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "查看错误日志："
    echo "  $DOCKER_COMPOSE logs mysql"
    exit 1
fi
