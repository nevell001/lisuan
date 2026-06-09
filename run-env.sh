#!/bin/bash
# 收银系统启动脚本 - Linux输入法兼容性修复

# 设置fcitx5输入法环境变量
export GTK_IM_MODULE=fcitx
export QT_IM_MODULE=fcitx
export XMODIFIERS=@im=fcitx
export GLFW_IM_MODULE=ibus  # 某系统需要

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 启动应用
exec java -jar "$SCRIPT_DIR/target/cashier-system-fx-2.5.4-jar-with-dependencies.jar" "$@"
