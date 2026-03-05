#!/bin/bash

# ============================================
# Cashier System Startup Script (Linux/Mac)
# ============================================
#
# 安全提示：
# 为了安全起见，建议设置环境变量 CASHER_DB_PASSWORD 来存储数据库密码
#
# Linux/Mac 设置方式:
#   export CASHER_DB_PASSWORD="YourPassword"
#   ./start.sh
#
# 或者创建 .env 文件（需要先 source .env）:
#   echo 'export CASHER_DB_PASSWORD="YourPassword"' >> .env
#   source .env
#   ./start.sh
#
# 如果设置了环境变量，config/database.properties 中的 db.password 将被忽略
# ============================================

APP_NAME="Cashier System"
APP_VERSION="2.4.2"
MAIN_CLASS="com.cashier.CashierSystemFXApplication"
CONFIG_FILE="config/jvm.config"

# 获取脚本所在目录
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR"

echo "========================================"
echo "  Cashier System Startup"
echo "========================================"
echo ""

# 检查 Java 环境
echo "[1/6] Checking Java environment..."
if ! command -v java &> /dev/null; then
    echo "[Error] Java runtime not found!"
    echo "Please ensure JDK 17 or higher is installed"
    echo "Download: https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "[Info] Java version: ${JAVA_VERSION}"

if ! command -v javac &> /dev/null; then
    echo "[Warning] Only JRE detected, full JDK recommended"
fi

echo "[Done] Java environment check passed"
echo ""

# 检查必要目录
echo "[2/6] Checking necessary directories..."
mkdir -p config data logs
echo "[Done] Directory check passed"
echo ""

# 检查配置文件
echo "[3/6] Checking configuration files..."
if [ ! -f "config/database.properties" ]; then
    echo "[Warning] Database config file not found, using default settings"
    echo "[Tip] Please run ./install.sh for full installation"
fi

if [ ! -f "config/jvm.config" ]; then
    echo "[Create] Creating JVM config file"
    cp config/jvm.config.example config/jvm.config 2>/dev/null || true
fi

echo "[Done] Configuration files checked"
echo ""

# 检查依赖文件
echo "[4/6] Checking dependency files..."
JAR_FILE="target/cashier-system-fx-${APP_VERSION}-jar-with-dependencies.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "[Warning] Compiled JAR file not found"
    echo "[Compile] Starting project compilation..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "[Error] Compilation failed"
        exit 1
    fi
    echo "[Done] Compilation completed"
fi

echo "[Done] Dependency files checked"
echo ""

# 构建 JVM 参数
echo "[5/6] Building JVM parameters..."
JVM_OPTS=""

if [ -f "$CONFIG_FILE" ]; then
    while IFS= read -r line || [[ -n "$line" ]]; do
        # 忽略空行和注释（以 # 开头的行）
        if [[ -n "$line" ]] && [[ ! "$line" =~ ^[[:space:]]*# ]]; then
            JVM_OPTS="$JVM_OPTS $line"
        fi
    done < "$CONFIG_FILE"
fi

# 默认 JVM 参数
if [ -z "$JVM_OPTS" ]; then
    JVM_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"
fi

# macOS DPI 缩放支持
if [[ "$OSTYPE" == "darwin"* ]]; then
    JVM_OPTS="$JVM_OPTS -Dsun.java2d.dpiaware=true"
fi

echo "[Done] JVM parameters built"
echo ""

# 启动应用
echo "[6/6] Starting application..."
echo ""
echo "========================================"
echo "  ${APP_NAME} ${APP_VERSION}"
echo "========================================"
echo ""
echo "Starting, please wait..."
echo ""

# 使用 Maven JavaFX 插件启动（推荐用于 JavaFX 应用）
mvn javafx:run

# 检查退出码
if [ $? -ne 0 ]; then
    echo ""
    echo "========================================"
    echo "[Error] Application exited abnormally (Error code: $?)"
    echo "========================================"
    echo ""
    echo "Check log file: logs/app.log"
    exit 1
fi

echo ""
echo "========================================"
echo "[Info] Application exited normally"
echo "========================================"
echo ""