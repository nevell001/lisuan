#!/bin/bash
# 收银系统启动脚本（Linux/Mac）
# 包含编译检查、本地中文字体检测和加载功能

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FONTS_DIR="$SCRIPT_DIR/fonts"

echo "========================================"
echo "收银系统启动中..."
echo "========================================"
echo ""

# 检查是否已编译
echo "检查编译状态..."
if [ ! -f "$SCRIPT_DIR/CashierSystemGUI.class" ]; then
    echo "⚠️  未检测到编译文件，开始编译..."
    echo ""
    bash "$SCRIPT_DIR/compile_with_flatlaf.sh"

    if [ $? -ne 0 ]; then
        echo ""
        echo "✗ 编译失败，请检查错误信息"
        exit 1
    fi

    echo ""
    echo "✓ 编译完成"
    echo ""
else
    echo "✓ 检测到编译文件"
    echo ""
fi

# 检测本地字体目录
USE_LOCAL_FONT=false
if [ -d "$FONTS_DIR" ]; then
    FONT_COUNT=$(find "$FONTS_DIR" -type f \( -name "*.ttf" -o -name "*.ttc" -o -name "*.otf" -o -name "*.woff2" \) 2>/dev/null | wc -l)
    if [ "$FONT_COUNT" -gt 0 ]; then
        USE_LOCAL_FONT=true
        echo "✓ 检测到本地字体文件 ($FONT_COUNT 个)"
        echo "  字体目录: $FONTS_DIR"
    fi
fi

# 检测操作系统类型
OS_TYPE=$(uname -s)

# 如果没有本地字体，检查系统字体
if [ "$USE_LOCAL_FONT" = false ] && [ "$OS_TYPE" = "Linux" ]; then
    echo "检查系统字体..."
    if ! fc-list :lang=zh | grep -q . 2>/dev/null; then
        echo "⚠️  系统中未检测到中文字体"
        echo ""
        echo "推荐方案："
        echo "  1. 下载本地字体（推荐）：./download_fonts.sh"
        echo "  2. 安装系统字体：sudo apt-get install fonts-noto-cjk"
        echo ""
        read -p "是否现在下载本地字体？(Y/n): " download_font

        if [ "$download_font" != "n" ] && [ "$download_font" != "N" ]; then
            echo ""
            bash "$SCRIPT_DIR/download_fonts.sh"
            if [ $? -eq 0 ]; then
                USE_LOCAL_FONT=true
            fi
        fi
    else
        echo "✓ 系统中已安装中文字体"
    fi
    echo ""
fi

# 启动收银系统
echo "启动收银系统..."

# 设置 Java 字体参数
JAVA_CMD="java"
if [ "$USE_LOCAL_FONT" = true ]; then
    JAVA_CMD="java -Djava.awt.fonts=$FONTS_DIR"
fi

# 运行程序
$JAVA_CMD -cp flatlaf-3.5.jar:. CashierSystemGUI