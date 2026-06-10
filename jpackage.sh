#!/bin/bash
# ========================================
# 收银系统 - jpackage 原生打包脚本
# Linux/macOS 版本
# ========================================

set -e

echo "========================================"
echo "  收银系统 - jpackage 原生打包"
echo "========================================"
echo ""

# 从 pom.xml 读取版本号
APP_VERSION=$(grep -A 1 "<artifactId>cashier-system-fx</artifactId>" pom.xml | grep "<version>" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | head -1)
if [ -z "$APP_VERSION" ]; then
    APP_VERSION=$(grep "<version>" pom.xml | grep -v "javafx\|maven\|java\|mysql\|hikaricp\|poi\|pdfbox\|controlsfx\|fontawesomefx\|junit\|testfx\|h2\|bcrypt\|logback\|jackson\|javalin\|slf4j\|plugin" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
fi
if [ -z "$APP_VERSION" ]; then
    APP_VERSION="2.5.5"
fi

echo "[INFO] 版本: $APP_VERSION"
echo ""

# 检查 JAR 文件
FAT_JAR="cashier-system-fx-$APP_VERSION-jar-with-dependencies.jar"
if [ ! -f "target/$FAT_JAR" ]; then
    echo "[ERROR] 未找到 JAR 文件: target/$FAT_JAR"
    echo ""
    echo "请先运行: mvn clean package"
    exit 1
fi

# 检查 jpackage 命令
if ! command -v jpackage &> /dev/null; then
    echo "[ERROR] 未找到 jpackage 命令"
    echo ""
    echo "jpackage 是 JDK 14+ 自带的工具。"
    echo "请确保 JDK 的 bin 目录在 PATH 中"
    echo ""
    echo "检查 JDK:"
    java -version
    echo ""
    exit 1
fi

# 清理旧的打包文件
echo "[1/3] 清理旧的打包文件..."
rm -rf target/dist
mkdir -p target/dist
echo "[OK] 清理完成"
echo ""

# 检测平台并设置对应参数
OS_TYPE=$(uname -s)
PKG_TYPE=""
PKG_NAME="CashierSystem"

echo "[2/3] 开始打包..."
echo ""

case "$OS_TYPE" in
    Linux*)
        PKG_TYPE="rpm"
        if command -v dpkg &> /dev/null; then
            PKG_TYPE="deb"
        fi
        echo "[INFO] 检测到 Linux 平台，生成 $PKG_TYPE 包"
        
        jpackage \
            --type $PKG_TYPE \
            --name "$PKG_NAME" \
            --app-version "$APP_VERSION" \
            --vendor "Cashier System" \
            --description "现代化收银系统 - 库存管理、会员管理、交易管理" \
            --dest target/dist \
            --input target \
            --main-jar "$FAT_JAR" \
            --main-class com.cashier.CashierSystemFXApplication \
            --java-options "-Xms512m" \
            --java-options "-Xmx1024m" \
            --java-options "-Dfile.encoding=UTF-8" \
            --linux-menu-group "Office" \
            --linux-shortcut \
            --icon src/main/resources/images/logos/app-icon.png
        ;;
    
    Darwin*)
        PKG_TYPE="dmg"
        echo "[INFO] 检测到 macOS 平台，生成 $PKG_TYPE 包"
        
        jpackage \
            --type $PKG_TYPE \
            --name "$PKG_NAME" \
            --app-version "$APP_VERSION" \
            --vendor "Cashier System" \
            --description "现代化收银系统 - 库存管理、会员管理、交易管理" \
            --dest target/dist \
            --input target \
            --main-jar "$FAT_JAR" \
            --main-class com.cashier.CashierSystemFXApplication \
            --java-options "-Xms512m" \
            --java-options "-Xmx1024m" \
            --java-options "-Dfile.encoding=UTF-8" \
            --mac-package-name "Cashier System" \
            --icon src/main/resources/images/logos/app-icon.png
        ;;
    
    *)
        echo "[ERROR] 不支持的平台: $OS_TYPE"
        exit 1
        ;;
esac

if [ $? -ne 0 ]; then
    echo ""
    echo "========================================"
    echo "  [ERROR] 打包失败！"
    echo "========================================"
    echo ""
    exit 1
fi

echo ""
echo "[3/3] 打包完成！"
echo ""

echo "========================================"
echo "  [SUCCESS] 原生安装包已创建"
echo "========================================"
echo ""
echo "安装包位置: target/dist/"
echo ""

# 打开输出目录
if command -v xdg-open &> /dev/null; then
    xdg-open target/dist &> /dev/null &
elif command -v open &> /dev/null; then
    open target/dist &> /dev/null &
fi

exit 0
