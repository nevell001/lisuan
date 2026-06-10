#!/bin/bash
# ========================================
# 狸算收银系统 - jpackage 原生打包脚本
# Linux/macOS 版本
# ========================================

set -e

echo "========================================"
echo "  狸算收银系统 - jpackage 原生打包"
echo "========================================"
echo ""

# 从 pom.xml 读取版本号
APP_VERSION=$(grep "<version>" pom.xml | grep -v "javafx\|maven\|java\|mysql\|hikaricp\|poi\|pdfbox\|controlsfx\|fontawesomefx\|junit\|testfx\|h2\|bcrypt\|logback\|jackson\|javalin\|slf4j\|plugin" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
if [ -z "$APP_VERSION" ]; then
    APP_VERSION="2.5.5"
fi

# JavaFX 版本
JAVAFX_VERSION="17.0.12"

echo "[INFO] 版本: $APP_VERSION"
echo "[INFO] JavaFX: $JAVAFX_VERSION"
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
PKG_NAME="LiSuan"
ARCH=$(uname -m)

echo "[2/3] 开始打包..."
echo "[INFO] 平台: $OS_TYPE $ARCH"
echo ""

# 确定 JavaFX 平台后缀
case "$OS_TYPE" in
    Linux*)
        JAVAFX_PLATFORM="linux"
        ;;
    Darwin*)
        if [ "$ARCH" = "arm64" ]; then
            JAVAFX_PLATFORM="mac-aarch64"
        else
            JAVAFX_PLATFORM="mac"
        fi
        ;;
    *)
        JAVAFX_PLATFORM=""
        ;;
esac

case "$OS_TYPE" in
    Linux*)
        PKG_TYPE="rpm"
        if command -v dpkg &> /dev/null; then
            PKG_TYPE="deb"
        fi
        echo "[INFO] 检测到 Linux 平台，生成 $PKG_TYPE 包"
        echo ""

        # 构建模块路径（仅包含平台特定的 jar）
        JAVAFX_MODULES=""
        if [ -n "$JAVAFX_PLATFORM" ]; then
            JFX_BASE="$HOME/.m2/repository/org/openjfx/javafx-base/$JAVAFX_VERSION/javafx-base-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
            JFX_CONTROLS="$HOME/.m2/repository/org/openjfx/javafx-controls/$JAVAFX_VERSION/javafx-controls-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
            JFX_FXML="$HOME/.m2/repository/org/openjfx/javafx-fxml/$JAVAFX_VERSION/javafx-fxml-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
            JFX_GRAPHICS="$HOME/.m2/repository/org/openjfx/javafx-graphics/$JAVAFX_VERSION/javafx-graphics-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
            JAVAFX_MOD_PATH="--module-path $JFX_BASE:$JFX_CONTROLS:$JFX_FXML:$JFX_GRAPHICS"
            JAVAFX_MODULES="$JAVAFX_MOD_PATH --add-modules javafx.controls,javafx.fxml,javafx.graphics"
        fi

        jpackage \
            --type $PKG_TYPE \
            --name "$PKG_NAME" \
            --app-version "$APP_VERSION" \
            --vendor "LiSuan" \
            --description "狸算收银系统 - 现代化收银系统" \
            --dest target/dist \
            --input target \
            --main-jar "$FAT_JAR" \
            --main-class com.cashier.CashierSystemFXApplication \
            --java-options "-Xms512m" \
            --java-options "-Xmx1024m" \
            --java-options "-Dfile.encoding=UTF-8" \
            $JAVAFX_MODULES \
            --linux-menu-group "Office" \
            --linux-shortcut \
            --linux-app-category "Business" \
            --linux-package-deps \
            --icon src/main/resources/images/logos/app-icon.png
        ;;

    Darwin*)
        PKG_TYPE="dmg"
        echo "[INFO] 检测到 macOS 平台，生成 $PKG_TYPE 包"
        echo "[INFO] JavaFX 平台: $JAVAFX_PLATFORM"
        echo ""

        # 构建模块路径（仅包含平台特定的 jar）
        JAVAFX_MODULES=""
        if [ -n "$JAVAFX_PLATFORM" ]; then
            JFX_BASE="$HOME/.m2/repository/org/openjfx/javafx-base/$JAVAFX_VERSION/javafx-base-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
            JFX_CONTROLS="$HOME/.m2/repository/org/openjfx/javafx-controls/$JAVAFX_VERSION/javafx-controls-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
            JFX_FXML="$HOME/.m2/repository/org/openjfx/javafx-fxml/$JAVAFX_VERSION/javafx-fxml-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
            JFX_GRAPHICS="$HOME/.m2/repository/org/openjfx/javafx-graphics/$JAVAFX_VERSION/javafx-graphics-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
            JAVAFX_MOD_PATH="--module-path $JFX_BASE:$JFX_CONTROLS:$JFX_FXML:$JFX_GRAPHICS"
            JAVAFX_MODULES="$JAVAFX_MOD_PATH --add-modules javafx.controls,javafx.fxml,javafx.graphics"
        fi

        jpackage \
            --type $PKG_TYPE \
            --name "$PKG_NAME" \
            --app-version "$APP_VERSION" \
            --vendor "LiSuan" \
            --description "狸算收银系统 - 现代化收银系统" \
            --dest target/dist \
            --input target \
            --main-jar "$FAT_JAR" \
            --main-class com.cashier.CashierSystemFXApplication \
            --java-options "-Xms512m" \
            --java-options "-Xmx1024m" \
            --java-options "-Dfile.encoding=UTF-8" \
            $JAVAFX_MODULES \
            --mac-package-name "LiSuan" \
            --icon src/main/resources/images/logos/app-icon.icns
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

# 显示文件大小
PKG_FILE=$(find target/dist -name "LiSuan-*" -type f 2>/dev/null | head -1)
if [ -n "$PKG_FILE" ]; then
    PKG_SIZE=$(du -h "$PKG_FILE" | cut -f1)
    echo "安装包: $PKG_FILE"
    echo "安装包大小: $PKG_SIZE"
    echo ""
fi

echo "注意: macOS/Linux 版本需要系统已安装 Java 17+"
echo ""

# 打开输出目录
if command -v xdg-open &> /dev/null; then
    xdg-open target/dist &> /dev/null &
elif command -v open &> /dev/null; then
    open target/dist &> /dev/null &
fi

exit 0
