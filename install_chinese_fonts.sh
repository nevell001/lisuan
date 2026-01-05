#!/bin/bash
# 中文字体安装脚本
# 支持的 Linux 发行版：Ubuntu/Debian, CentOS/RHEL/Fedora, Arch Linux

set -e

echo "========================================"
echo "收银系统 - 中文字体安装脚本"
echo "========================================"
echo ""

# 检测 Linux 发行版
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
    VERSION=$VERSION_ID
else
    echo "错误：无法检测 Linux 发行版"
    exit 1
fi

echo "检测到的系统: $OS $VERSION"
echo ""

# 检查是否已安装中文字体
echo "检查系统中是否有中文字体..."
if fc-list :lang=zh | grep -q .; then
    echo "✓ 系统中已安装中文字体："
    fc-list :lang=zh | head -5
    echo ""
    read -p "是否要重新安装字体？(y/N): " reinstall
    if [ "$reinstall" != "y" ] && [ "$reinstall" != "Y" ]; then
        echo "已取消安装。"
        exit 0
    fi
else
    echo "⚠️  系统中未检测到中文字体"
    echo ""
fi

# 根据发行版安装字体
case $OS in
    ubuntu|debian)
        echo "使用 apt 安装中文字体..."
        sudo apt-get update
        sudo apt-get install -y fonts-noto-cjk fonts-wqy-microhei fonts-wqy-zenhei
        ;;

    centos|rhel|fedora)
        echo "使用 dnf 安装中文字体..."
        sudo dnf install -y google-noto-sans-cjk-fonts wqy-microhei-fonts wqy-zenhei-fonts
        ;;

    arch|manjaro)
        echo "使用 pacman 安装中文字体..."
        sudo pacman -S --noconfirm noto-fonts-cjk wqy-microhei wqy-zenhei
        ;;

    *)
        echo "错误：不支持的 Linux 发行版: $OS"
        echo "请手动安装中文字体，参考文档："
        echo "  Ubuntu/Debian: sudo apt-get install fonts-noto-cjk fonts-wqy-microhei"
        echo "  CentOS/RHEL: sudo dnf install google-noto-sans-cjk-fonts"
        echo "  Arch Linux: sudo pacman -S noto-fonts-cjk"
        exit 1
        ;;
esac

echo ""
echo "刷新字体缓存..."
fc-cache -fv

echo ""
echo "========================================"
echo "验证字体安装..."
echo "========================================"
echo ""

if fc-list :lang=zh | grep -q .; then
    echo "✓ 中文字体安装成功！"
    echo ""
    echo "已安装的中文字体："
    fc-list :lang=zh | sed 's|/usr/share/fonts/||' | sed 's|:.*||' | sort -u
    echo ""
    echo "========================================"
    echo "安装完成！现在可以运行收银系统了。"
    echo "========================================"
    echo ""
    echo "运行命令："
    echo "  ./run_with_flatlaf.sh"
else
    echo "✗ 字体安装失败，请检查错误信息。"
    exit 1
fi