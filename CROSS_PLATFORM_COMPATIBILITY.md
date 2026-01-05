# 跨平台兼容性说明

## 概述

本项目已优化以确保在 Windows、macOS 和 Linux 系统上都能正常运行。

## 已实现的跨平台支持

### 1. 字体系统 ✅

#### 问题
- Windows 系统使用 "微软雅黑" 字体
- macOS 系统使用 "PingFang SC" 字体
- Linux 系统需要使用其他中文字体

#### 解决方案
添加了智能字体选择方法：

```java
private static Font getChineseFont(int style, int size) {
    String osName = System.getProperty("os.name", "").toLowerCase();
    String fontName;

    if (osName.contains("win")) {
        // Windows 系统
        fontName = "微软雅黑";
    } else if (osName.contains("mac")) {
        // macOS 系统
        fontName = "PingFang SC";
    } else {
        // Linux 或其他系统
        String[] linuxFonts = {
            "Noto Sans CJK SC",      // Google Noto 字体（推荐）
            "WenQuanYi Micro Hei",   // 文泉驿微米黑
            "WenQuanYi Zen Hei",     // 文泉驿正黑
            "SimHei",                // 黑体
            "DejaVu Sans",           // DejaVu 字体
            "SansSerif"              // Java 逻辑字体（默认）
        };

        fontName = "SansSerif"; // 默认值

        // 检查系统中可用的字体
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        for (String font : linuxFonts) {
            for (String available : availableFonts) {
                if (available.equalsIgnoreCase(font)) {
                    fontName = font;
                    break;
                }
            }
            if (!fontName.equals("SansSerif")) {
                break;
            }
        }
    }

    return new Font(fontName, style, size);
}
```

#### 字体优先级（Linux）
1. Noto Sans CJK SC（推荐，支持最完整）
2. WenQuanYi Micro Hei（文泉驿微米黑）
3. WenQuanYi Zen Hei（文泉驿正黑）
4. SimHei（黑体）
5. DejaVu Sans
6. SansSerif（默认后备）

### 2. 文件路径 ✅

#### 问题
- Windows 使用 `\` 作为路径分隔符
- Unix-like 系统使用 `/` 作为路径分隔符

#### 解决方案
Java 的 `File` 类会自动处理路径分隔符，代码中统一使用 `/`，Java 会自动转换为正确的平台分隔符：

```java
// 在所有平台上都能正常工作
private static final String DATA_DIR = "data";
private static final String INVENTORY_FILE = DATA_DIR + "/inventory.txt";
```

### 3. 文件换行符 ✅

#### 问题
- Windows 使用 `\r\n` (CRLF)
- Unix-like 系统使用 `\n` (LF)

#### 解决方案
使用 `PrintWriter` 和 `FileWriter` 时，Java 会自动使用平台的默认换行符：

```java
try (PrintWriter writer = new PrintWriter(new FileWriter(INVENTORY_FILE))) {
    writer.printf("%s|%.2f|%d\n", ...);  // Java 会自动处理换行符
}
```

### 4. 编译脚本 ✅

#### Linux/macOS (compile_with_flatlaf.sh)
```bash
#!/bin/bash
javac -cp flatlaf-3.5.jar Category.java Product.java Transaction.java Member.java Promotion.java RechargeRecord.java User.java OperationLog.java Shift.java DataManager.java CashierSystemGUI.java
```

#### Windows (compile_with_flatlaf.bat)
```batch
@echo off
javac -cp flatlaf-3.5.jar Category.java Product.java Transaction.java Member.java Promotion.java RechargeRecord.java User.java OperationLog.java Shift.java DataManager.java CashierSystemGUI.java
if %errorlevel% neq 0 (
    echo 编译失败！
) else (
    echo 编译成功！
)
pause
```

### 5. 运行脚本 ✅

#### Linux/macOS (run_with_flatlaf.sh)
```bash
#!/bin/bash
java -cp flatlaf-3.5.jar:. CashierSystemGUI
```

#### Windows (run_with_flatlaf.bat)
```batch
@echo off
java -cp flatlaf-3.5.jar;. CashierSystemGUI
```

### 6. 脚本执行权限 ✅

Linux/macOS 脚本已添加执行权限：
```bash
chmod +x compile_with_flatlaf.sh run_with_flatlaf.sh
```

### 7. 数据文件格式 ✅

所有数据文件使用统一的格式：
- 字段分隔符：`|`
- 换行符：由 Java 自动处理
- 特殊字符转义：`\|` 和 `\n`

## 平台特定配置

### Windows

#### 系统要求
- JDK 11 或更高版本（推荐 JDK 17）
- Windows 7 或更高版本

#### 中文字体
系统默认包含 "微软雅黑" 字体，无需额外安装。

#### 编译和运行
```batch
# 编译
compile_with_flatlaf.bat

# 运行
run_with_flatlaf.bat
```

### macOS

#### 系统要求
- JDK 11 或更高版本（推荐 JDK 17）
- macOS 10.12 或更高版本

#### 中文字体
系统默认包含 "PingFang SC" 字体，无需额外安装。

#### 编译和运行
```bash
# 编译
./compile_with_flatlaf.sh

# 运行
./run_with_flatlaf.sh
```

### Linux

#### 系统要求
- JDK 11 或更高版本（推荐 JDK 17）
- 任意主流 Linux 发行版（Ubuntu、CentOS、Fedora、Arch 等）

#### 中文字体安装（必需！）

**重要提示：** Linux 系统必须安装中文字体才能正确显示中文！如果未安装，中文将显示为小方格（□□□）。

**检查是否已安装中文字体：**
```bash
# 检查系统中是否有中文字体
fc-list :lang=zh

# 如果没有输出或输出很少，说明需要安装中文字体
```

**Ubuntu/Debian:**
```bash
# 更新软件包列表
sudo apt-get update

# 安装 Google Noto CJK 字体（推荐，最完整）
sudo apt-get install fonts-noto-cjk

# 安装文泉驿字体（备选）
sudo apt-get install fonts-wqy-microhei fonts-wqy-zenhei

# 刷新字体缓存
fc-cache -fv
```

**CentOS/RHEL/Fedora:**
```bash
# 安装 Google Noto CJK 字体（推荐）
sudo dnf install google-noto-sans-cjk-fonts

# 安装文泉驿字体（备选）
sudo dnf install wqy-microhei-fonts wqy-zenhei-fonts

# 刷新字体缓存
fc-cache -fv
```

**Arch Linux:**
```bash
# 安装 Google Noto CJK 字体（推荐）
sudo pacman -S noto-fonts-cjk

# 安装文泉驿字体（备选）
sudo pacman -S wqy-microhei wqy-zenhei

# 刷新字体缓存
fc-cache -fv
```

**验证字体安装：**
```bash
# 检查已安装的中文字体
fc-list :lang=zh

# 应该看到类似以下的输出：
# /usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc: Noto Sans CJK SC:style=Regular
# /usr/share/fonts/wqy-microhei/wqy-microhei.ttc: WenQuanYi Micro Hei Mono:style=Regular
```

#### 编译和运行
```bash
# 编译
./compile_with_flatlaf.sh

# 运行
./run_with_flatlaf.sh
```

## 测试清单

### 功能测试
- [x] 登录界面正常显示
- [x] 主界面标签页正常显示
- [x] 菜单项正常显示
- [x] 按钮和标签文本正常显示
- [x] 表格内容正常显示
- [x] 数据保存和加载正常
- [x] 备份和恢复功能正常
- [x] 快捷键功能正常

### 平台测试
- [x] Windows 10/11
- [ ] macOS 12/13/14（待测试）
- [x] Linux (Ubuntu 22.04)
- [ ] Linux (CentOS 8)（待测试）
- [ ] Linux (Arch)（待测试）

## 已知问题和限制

### 1. 中文字体依赖
**问题**: Linux 系统如果没有安装中文字体，可能无法正确显示中文。

**解决**: 建议安装 Noto CJK 或文泉驿字体。系统会自动检测可用字体并使用第一个可用的字体。

### 2. 文件权限
**问题**: 在某些 Linux 系统上，可能需要管理员权限才能创建数据目录。

**解决**: 确保对项目目录有读写权限：
```bash
chmod -R 755 /path/to/hello
```

### 3. FlatLaf 主题
**问题**: 某些 FlatLaf 主题在特定平台上可能有显示问题。

**解决**: 系统默认使用 FlatLightLaf，可以在设置中切换主题。

## 最佳实践

### 开发建议
1. 使用 Java 标准库的跨平台 API（如 `File`、`Path`）
2. 避免使用平台特定的路径分隔符
3. 使用 `System.getProperty()` 检测操作系统特性
4. 测试时覆盖所有目标平台

### 部署建议
1. 为每个平台提供对应的启动脚本
2. 在 README 中明确说明平台要求
3. 提供常见问题的解决方案
4. 考虑使用打包工具（如 jpackage）创建平台特定的安装包

## 更新日志

### 2026-01-05
- ✅ 添加智能字体选择方法
- ✅ 修复 Windows 编译脚本（添加缺失的类文件）
- ✅ 为 Linux/macOS 脚本添加执行权限
- ✅ 验证文件路径跨平台兼容性
- ✅ 创建跨平台兼容性文档

## 相关文档

- [FONT_FIX_LINUX.md](./FONT_FIX_LINUX.md) - Linux 中文显示修复详情
- [README.md](./README.md) - 项目说明
- [IFLOW.md](./IFLOW.md) - 详细项目文档

## 联系方式

如有跨平台相关的问题，请通过以下方式联系：
- 提交 Issue: https://gitee.com/nevell/hello/issues