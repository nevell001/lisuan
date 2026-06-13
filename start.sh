#!/bin/bash

# ============================================
# LiSuan System Startup Script (Linux/Mac)
# ============================================
#
# 数据库配置：
# 应用启动时会从 config/database.properties 读取数据库连接信息
# 安装脚本 install.sh 会根据 ENVIRONMENT 变量自动配置：
#   - development: 使用 root 用户（便于开发调试）
#   - production: 使用 lisuan 用户（更安全）
# ============================================

APP_NAME="LiSuan System"

# Read version from pom.xml automatically (get first occurrence - project version)
APP_VERSION=$(awk '/<version>[^<]+<\/version>/ {gsub(/.*<version>|<\/version>.*/, ""); if ($0 !~ /\$\{/) {print; exit}}' pom.xml)

# Fallback if version not found
if [ -z "$APP_VERSION" ]; then
    APP_VERSION="2.5.7"
fi
MAIN_CLASS="com.cashier.CashierSystemFXApplication"
CONFIG_FILE="config/jvm.config"

# 获取脚本所在目录
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR"

# 设置输入法环境变量（Linux fcitx5 兼容性）
if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "linux"* ]]; then
    export GTK_IM_MODULE=fcitx
    export QT_IM_MODULE=fcitx
    export XMODIFIERS=@im=fcitx
    echo "[Info] Input method environment set for fcitx5"
fi

echo "========================================"
echo "  LiSuan System Startup"
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
mkdir -p config data logs temp
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
JAR_FILE="target/lisuan-fx-${APP_VERSION}.jar"

# 如果 shaded JAR 不存在，尝试旧版本的命名
if [ ! -f "$JAR_FILE" ]; then
    JAR_FILE="target/lisuan-fx-${APP_VERSION}-jar-with-dependencies.jar"
fi

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

# 构建 JavaFX 模块路径
JFX_BASE="$HOME/.m2/repository/org/openjfx"
JFX_VERSION="17.0.12"

# Detect platform
if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "linux"* ]]; then
    JFX_PLATFORM="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # 检测 macOS 架构
    ARCH=$(uname -m)
    if [[ "$ARCH" == "arm64" ]] || [[ "$ARCH" == "aarch64" ]]; then
        JFX_PLATFORM="mac-aarch64"
    else
        JFX_PLATFORM="mac"
    fi
else
    JFX_PLATFORM="win"
fi

JFX_PATH="$JFX_BASE/javafx-base/$JFX_VERSION/javafx-base-$JFX_VERSION-$JFX_PLATFORM.jar:$JFX_BASE/javafx-controls/$JFX_VERSION/javafx-controls-$JFX_VERSION-$JFX_PLATFORM.jar:$JFX_BASE/javafx-fxml/$JFX_VERSION/javafx-fxml-$JFX_VERSION-$JFX_PLATFORM.jar:$JFX_BASE/javafx-graphics/$JFX_VERSION/javafx-graphics-$JFX_VERSION-$JFX_PLATFORM.jar"

JFX_MODULES=""
# 检查 JAR 中是否包含 JavaFX（fat JAR）
if jar tf "$JAR_FILE" 2>/dev/null | grep -q "javafx/scene/control/Control.class"; then
    echo "[OK] JavaFX bundled in JAR"
    # 使用外部 JavaFX 模块路径（即使 JAR 中包含 JavaFX，launch() 仍需要模块路径）
    if [ -f "$JFX_BASE/javafx-base/$JFX_VERSION/javafx-base-$JFX_VERSION-$JFX_PLATFORM.jar" ]; then
        JFX_MODULES="--module-path $JFX_PATH --add-modules javafx.controls,javafx.fxml,javafx.graphics"
        echo "[Info] Using external JavaFX modules for runtime"
    fi
else
    # 如果 JAR 中没有 JavaFX，使用外部 module-path
    if [ -f "$JFX_BASE/javafx-base/$JFX_VERSION/javafx-base-$JFX_VERSION-$JFX_PLATFORM.jar" ]; then
        JFX_MODULES="--module-path $JFX_PATH --add-modules javafx.controls,javafx.fxml,javafx.graphics"
        echo "[OK] Using external JavaFX modules ($JFX_PLATFORM)"
    else
        echo "[Warning] JavaFX not found"
    fi
fi

# 启动应用
echo "[6/6] Starting application..."
echo ""
echo "========================================"
echo "  ${APP_NAME} ${APP_VERSION}"
echo "========================================"
echo ""
echo "Starting, please wait..."
echo ""

# 使用 JAR 运行，带上必要的模块参数
java $JVM_OPTS --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.desktop/java.awt=ALL-UNNAMED $JFX_MODULES -jar "$JAR_FILE"

# 检查退出码
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "========================================"
    echo "[Error] Application exited abnormally (Error code: $EXIT_CODE)"
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