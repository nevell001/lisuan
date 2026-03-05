#!/bin/bash

# ============================================
# Cashier System Installation Script (Linux/Mac)
# ============================================

set -e

APP_VERSION="2.4.1"
DB_TYPE="none"
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="cashier_system"
DB_USERNAME="root"
DB_PASSWORD="RootPassword123!"

JAR_PATH="target/cashier-system-fx-${APP_VERSION}-jar-with-dependencies.jar"

echo "========================================"
echo "  Cashier System Installation"
echo "========================================"
echo ""

# 检查 Java 环境
echo "[1/8] Checking Java environment..."
if ! command -v java &> /dev/null; then
    echo "[Error] Java runtime not found!"
    echo "Please install JDK 17 or higher"
    echo "Download: https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "[Info] Java version: ${JAVA_VERSION}"

if ! command -v javac &> /dev/null; then
    echo "[Warning] JDK compiler not found"
    echo "[Note] Maven will use bundled compiler"
else
    echo "[Info] JDK compiler found"
fi

echo "[Done] Java environment check passed"
echo ""

# 检查 Maven 环境
echo "[2/8] Checking Maven environment..."
if ! command -v mvn &> /dev/null; then
    echo "[Error] Maven not found!"
    echo "Please install Maven 3.8 or higher"
    exit 1
fi

MAVEN_VERSION=$(mvn -version 2>&1 | head -n 1 | cut -d' ' -f3)
echo "[Info] Maven version: ${MAVEN_VERSION}"
echo "[Done] Maven environment check passed"
echo ""

# 创建必要目录
echo "[3/8] Creating necessary directories..."
mkdir -p config data logs docker/mysql-init docker/mysql-backup
echo "[Done] Directories created"
echo ""

# 检查配置文件
echo "[4/8] Checking configuration files..."
if [ ! -f "config/database.properties" ]; then
    echo "[Create] Creating database config file"
    cp config/database.properties.example config/database.properties
    echo "[Tip] Please edit config/database.properties"
fi

if [ ! -f "config/jvm.config" ]; then
    echo "[Create] Creating JVM config file"
    cp config/jvm.config.example config/jvm.config
fi

echo "[Done] Configuration files checked"
echo ""

# 下载 Maven 依赖
echo "[5/8] Downloading Maven dependencies..."
echo "[Tip] First installation may take a while..."
mvn dependency:resolve
echo "[Done] Dependencies downloaded"
echo ""

# 编译项目
echo "[6/8] Compiling project..."
if [ -f "$JAR_PATH" ]; then
    read -p "[Warning] Detected existing compiled files. Recompile? (y/N): " REPLY
    if [[ "$REPLY" =~ ^[Yy]$ ]]; then
        echo "[Clean] Cleaning old files..."
        mvn clean
    else
        echo "[Skip] Skipping compilation"
    fi
fi

mvn clean package -DskipTests
echo "[Done] Project compiled"
echo ""

# 数据库安装选项
echo "[7/8] Database Installation"
echo ""
echo "Please select database installation option:"
echo "  1 - Install Docker (Recommended)"
echo "  2 - Use existing local MySQL"
echo ""
read -p "Enter option (1/2, default=1): " DB_CHOICE
DB_CHOICE=${DB_CHOICE:-1}

if [ "$DB_CHOICE" == "1" ]; then
    DB_TYPE="docker"
    echo "[Info] Selected: Install Docker"
elif [ "$DB_CHOICE" == "2" ]; then
    DB_TYPE="local"
    echo "[Info] Selected: Use existing local MySQL"
else
    echo "[Warning] Invalid option, defaulting to Docker"
    DB_TYPE="docker"
fi

echo ""

# 检查 Docker
if [ "$DB_TYPE" == "docker" ]; then
    echo "[Docker] Checking Docker installation..."
    if ! command -v docker &> /dev/null; then
        echo "[Info] Docker not installed yet"
        echo ""
        echo "Please follow these steps:"
        echo "  1. Install Docker Desktop from: https://www.docker.com/products/docker-desktop"
        echo "  2. Start Docker Desktop"
        echo "  3. Restart this script and select option 1"
        echo ""
        DB_TYPE="none"
    elif ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo "[Error] Docker Compose not found!"
        echo "Please ensure Docker Desktop is installed and running"
        echo ""
        DB_TYPE="none"
    else
        echo "[Docker] Starting MySQL container..."
        if docker compose version &> /dev/null; then
            docker compose up -d mysql
        else
            docker-compose up -d mysql
        fi

        echo "[Docker] MySQL container started successfully"
        echo "[Docker] Waiting for MySQL to be ready..."
        sleep 10

        echo "[Docker] Importing sample data..."
        docker exec cashier-mysql mysql -uroot -p${DB_PASSWORD} --default-character-set=utf8mb4 ${DB_NAME} < docker/mysql-init/03-sample-data.sql 2>/dev/null || true
        echo "[Done] Database initialization completed"
        echo "[Note] Tables will be created automatically when you start the application"
        echo ""
    fi
fi

# 本地 MySQL 设置
if [ "$DB_TYPE" == "local" ]; then
    echo "[Local MySQL] Configuring local MySQL connection..."
    echo ""
    echo "Please enter your MySQL connection details:"
    echo ""

    read -p "MySQL Host (default=localhost): " DB_HOST_INPUT
    DB_HOST=${DB_HOST_INPUT:-localhost}

    read -p "MySQL Port (default=3306): " DB_PORT_INPUT
    DB_PORT=${DB_PORT_INPUT:-3306}

    read -p "MySQL Username (default=root): " DB_USERNAME_INPUT
    DB_USERNAME=${DB_USERNAME_INPUT:-root}

    read -s -p "MySQL Password: " DB_PASSWORD_INPUT
    DB_PASSWORD=${DB_PASSWORD_INPUT:-${DB_PASSWORD}}
    echo ""

    echo ""

    

        # 智能检测：检查端口冲突和 Docker MySQL 状态

        echo "[Local MySQL] Checking port ${DB_PORT} for conflicts..."

    

        # 首先检查 Docker MySQL 容器是否运行（双重保险）

        DOCKER_MYSQL_RUNNING=false

        if command -v docker &> /dev/null && docker info &> /dev/null; then

            if docker ps --format '{{.Names}}' | grep -q cashier-mysql; then

                DOCKER_MYSQL_RUNNING=true

            fi

        fi

    

        # 检测端口占用情况

        PORT_OCCUPIED=false

        PORT_PROCESS=""

        PORT_CMD=""

        if command -v lsof &> /dev/null; then

            if lsof -Pi :${DB_PORT} -sTCP:LISTEN -t >/dev/null 2>&1; then

                PORT_OCCUPIED=true

                PORT_PROCESS=$(lsof -Pi :${DB_PORT} -sTCP:LISTEN -t | head -1)

                PORT_CMD=$(ps -p $PORT_PROCESS -o command= 2>/dev/null || echo "")

            fi

        fi

    

        # 如果 Docker MySQL 运行或端口被占用，检测冲突

        if $DOCKER_MYSQL_RUNNING || $PORT_OCCUPIED; then

            if $PORT_OCCUPIED; then

                echo "[Warning] Port ${DB_PORT} is already in use!"

                echo ""

                echo "Process using port ${DB_PORT}:"

                echo "  PID: $PORT_PROCESS"

                echo "  Command: $PORT_CMD"

                echo ""

            else

                echo "[Warning] Docker MySQL container is running!"

                echo ""

                echo "Docker container: cashier-mysql"

                echo ""

            fi

    

            # 检测是否为 Docker MySQL 或 Colima ssh

            if echo "$PORT_CMD" | grep -qE "(colima|docker)" || $DOCKER_MYSQL_RUNNING; then

                # 检测到 Docker MySQL 正在运行

                echo "[Info] Detected Docker MySQL is running on port ${DB_PORT}!"

                echo ""

                echo "Please select an option:"

                echo "  1 - Switch to use Docker MySQL (Recommended)"

                echo "  2 - Stop Docker MySQL and use local MySQL"

                echo "  3 - Use a different port for local MySQL"

                echo ""

                read -p "Enter option (1/2/3, default=1): " PORT_CHOICE

                PORT_CHOICE=${PORT_CHOICE:-1}

    

                if [ "$PORT_CHOICE" == "1" ]; then

                    echo "[Info] Switching to Docker MySQL..."

                    DB_HOST="localhost"

                    DB_PORT="${DB_PORT:-3306}"

                    DB_USERNAME="cashier"

                    DB_PASSWORD="YourStrongPassword123!"

                    echo "[Info] Will use Docker MySQL on port ${DB_PORT}"

                elif [ "$PORT_CHOICE" == "2" ]; then

                    echo "[Local MySQL] Stopping Docker MySQL..."

                    if command -v docker-compose &> /dev/null; then

                        docker-compose stop mysql &> /dev/null || true

                    else

                        docker compose stop mysql &> /dev/null || true

                    fi

                    # 等待端口释放

                    for i in {1..10}; do

                        if ! lsof -Pi :${DB_PORT} -sTCP:LISTEN -t >/dev/null 2>&1; then

                            echo "[Local MySQL] Docker MySQL stopped, port ${DB_PORT} is now free"

                            break

                        fi

                        if [ $i -eq 10 ]; then

                            echo "[Warning] Port ${DB_PORT} is still in use, continuing anyway"

                        fi

                        sleep 1

                    done

                    echo "[Local MySQL] Proceeding with local MySQL configuration..."

                elif [ "$PORT_CHOICE" == "3" ]; then

                    echo "[Local MySQL] Please enter a different port:"

                    read -p "New port (default=3307): " DB_PORT_INPUT

                    DB_PORT=${DB_PORT_INPUT:-3307}

                    echo "[Local MySQL] Using port ${DB_PORT}"

                else

                    echo "[Warning] Invalid option, continuing with port ${DB_PORT}"

                fi

            fi

        else

            echo "[Local MySQL] Port ${DB_PORT} is free"

        fi

        echo ""

    

        echo "[Local MySQL] Testing connection..."
    if command -v mysql &> /dev/null; then
        if mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USERNAME} -p${DB_PASSWORD} -e "SELECT 1" &> /dev/null; then
            echo "[Local MySQL] Connection successful"
            echo ""

            echo "[Local MySQL] Creating database if not exists..."
            mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USERNAME} -p${DB_PASSWORD} -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true

            echo "[Local MySQL] Importing sample data..."
            mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USERNAME} -p${DB_PASSWORD} ${DB_NAME} < docker/mysql-init/03-sample-data.sql 2>/dev/null || true

            echo "[Done] Database initialization completed"
            echo "[Note] Tables will be created automatically when you start the application"
            echo ""
        else
            echo "[Error] Failed to connect to MySQL"
            echo "Please check your connection details:"
            echo "  Host: ${DB_HOST}"
            echo "  Port: ${DB_PORT}"
            echo "  Username: ${DB_USERNAME}"
            echo ""
            DB_TYPE="none"
        fi
    else
        echo "[Warning] MySQL client not found in PATH"
        echo ""
        echo "[Note] Database will be initialized automatically when you start the application"
        echo ""
    fi
fi

# 更新配置文件
if [ "$DB_TYPE" == "docker" ]; then
    DB_HOST="localhost"
    DB_PORT="3306"
    DB_USERNAME="root"
    DB_PASSWORD="RootPassword123!"
    echo "[Config] Updating database.properties for Docker MySQL..."
elif [ "$DB_TYPE" == "local" ]; then
    echo "[Config] Updating database.properties for Local MySQL..."
else
    echo "[Skip] Database configuration skipped"
    echo ""
fi

cat > config/database.properties << EOF
# Database Configuration
db.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
db.username=${DB_USERNAME}
db.password=${DB_PASSWORD}
db.pool.size=10
db.connection.timeout=30000
db.idle.timeout=600000
db.max.lifetime=1800000
EOF

echo "[Done] Database configuration updated"
echo ""

# 创建快捷方式
echo "[8/8] Creating desktop shortcut..."
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "[Info] Linux system detected"
    read -p "[Tip] Create desktop shortcut? (Y/n): " CREATE_SHORTCUT
    CREATE_SHORTCUT=${CREATE_SHORTCUT:-Y}

    if [[ ! "$CREATE_SHORTCUT" =~ ^[Nn]$ ]]; then
        DESKTOP_DIR="${XDG_DESKTOP_DIR:-$HOME/Desktop}"
        mkdir -p "$DESKTOP_DIR"

        cat > "$DESKTOP_DIR/cashier-system.desktop" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Cashier System
Comment=Cashier System v${APP_VERSION}
Exec=bash $(pwd)/start.sh
Icon=$(pwd)/images/logos/app-icon.png
Terminal=true
Categories=Office;
EOF

        chmod +x "$DESKTOP_DIR/cashier-system.desktop"
        echo "[Done] Desktop shortcut created"
    fi
elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "[Info] macOS system detected"
    echo "[Note] To create a dock shortcut, drag start.sh to Applications folder"
fi

echo ""

# 完成
echo "========================================"
echo "[Success] Installation completed!"
echo "========================================"
echo ""
echo "Application Info:"
echo "  Version: ${APP_VERSION}"
echo "  JAR: target/cashier-system-fx-${APP_VERSION}.jar"
echo ""

if [ "$DB_TYPE" == "docker" ]; then
    echo "Database: Docker MySQL (localhost:3306)"
    echo "  Start: docker compose up -d mysql"
    echo "  Stop: docker compose stop mysql"
elif [ "$DB_TYPE" == "local" ]; then
    echo "Database: Local MySQL (${DB_HOST}:${DB_PORT})"
else
    echo "Database: Not configured"
fi

echo ""
echo "Next Steps:"
echo "  1. Review config/database.properties"
echo "  2. Run ./start.sh to start"
echo ""
echo "Default Login: admin / admin123"
echo ""