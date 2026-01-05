#!/bin/bash
# 中文字体下载脚本
# 下载开源中文字体到本地 fonts 目录

set -e

FONTS_DIR="$(cd "$(dirname "$0")" && pwd)/fonts"

echo "========================================"
echo "收银系统 - 中文字体下载脚本"
echo "========================================"
echo ""

# 创建字体目录
mkdir -p "$FONTS_DIR"
echo "字体目录: $FONTS_DIR"
echo ""

# 检查是否已有字体
if [ -f "$FONTS_DIR/wqy-microhei.ttc" ] || [ -f "$FONTS_DIR/NotoSansSC-Regular.otf" ]; then
    echo "✓ 检测到已下载的字体文件"
    echo ""
    ls -lh "$FONTS_DIR"/*.{ttf,ttc,otf} 2>/dev/null | awk '{print $9, "("$5")"}'
    echo ""
    read -p "是否重新下载字体？(y/N): " redownload
    if [ "$redownload" != "y" ] && [ "$redownload" != "Y" ]; then
        echo "已取消下载。"
        exit 0
    fi
    echo ""
fi

echo "开始下载中文字体..."
echo ""

# 下载 WQY Microhei (文泉驿微米黑)
echo "下载 WQY Microhei (文泉驿微米黑)..."
if command -v wget &> /dev/null; then
    wget -O "$FONTS_DIR/wqy-microhei.ttc" \
        "https://github.com/adobe-fonts/source-han-sans/raw/release/SubsetOTF/CN/SourceHanSansCN-Regular.otf" \
        --timeout=30 --tries=3 2>/dev/null || {
        echo "⚠️  使用备用下载源..."
        wget -O "$FONTS_DIR/wqy-microhei.ttc" \
            "https://fonts.gstatic.com/s/notosanssc/v36/k3kXo84MPvpLmixcA63oeALZTYKLgASIOQ.woff2" \
            --timeout=30 --tries=3 2>/dev/null || echo "✗ 下载失败"
    }
elif command -v curl &> /dev/null; then
    curl -L -o "$FONTS_DIR/wqy-microhei.ttc" \
        "https://github.com/adobe-fonts/source-han-sans/raw/release/SubsetOTF/CN/SourceHanSansCN-Regular.otf" \
        --max-time 30 --retry 3 2>/dev/null || {
        echo "⚠️  使用备用下载源..."
        curl -L -o "$FONTS_DIR/wqy-microhei.ttc" \
            "https://fonts.gstatic.com/s/notosanssc/v36/k3kXo84MPvpLmixcA63oeALZTYKLgASIOQ.woff2" \
            --max-time 30 --retry 3 2>/dev/null || echo "✗ 下载失败"
    }
else
    echo "✗ 错误：未找到 wget 或 curl 工具"
    echo "请安装后重试："
    echo "  Ubuntu/Debian: sudo apt-get install wget"
    echo "  CentOS/RHEL: sudo dnf install wget"
    echo "  Mac: brew install wget"
    exit 1
fi

echo ""
echo "========================================"
echo "下载完成！"
echo "========================================"
echo ""

if [ -f "$FONTS_DIR/wqy-microhei.ttc" ]; then
    echo "✓ 字体文件已保存到: $FONTS_DIR"
    echo ""
    echo "已下载的字体："
    ls -lh "$FONTS_DIR"/*.{ttf,ttc,otf,woff2} 2>/dev/null | awk '{print $9, "("$5")"}'
    echo ""
    echo "现在可以运行收银系统了："
    echo "  ./run_with_flatlaf.sh"
else
    echo "✗ 字体下载失败"
    echo ""
    echo "请手动下载中文字体并放到 $FONTS_DIR 目录"
    echo "推荐字体："
    echo "  - 文泉驿微米黑 (WQY Microhei)"
    echo "  - 思源黑体 (Source Han Sans)"
    echo "  - Noto Sans SC"
fi