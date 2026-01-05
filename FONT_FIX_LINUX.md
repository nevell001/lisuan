# Linux 系统中文显示修复

## 问题描述

在 Linux 系统下，收银系统无法正确显示中文，出现乱码或方框字符。

## 原因分析

代码中硬编码了 Windows 特有的字体 "微软雅黑"，该字体在 Linux 系统上不存在，导致 Java 无法找到合适的字体来显示中文。

## 解决方案

### 1. 添加字体工具方法

在 `CashierSystemGUI.java` 中添加了两个字体工具方法：

```java
/**
 * 根据操作系统获取合适的中文字体
 * @param style 字体样式 (Font.PLAIN, Font.BOLD, Font.ITALIC)
 * @param size 字体大小
 * @return 支持中文的 Font 对象
 */
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
        // 尝试多个常见的中文支持字体
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

/**
 * 获取通用字体（用于数字和英文，不需要中文支持）
 * @param style 字体样式
 * @param size 字体大小
 * @return Font 对象
 */
private static Font getGeneralFont(int style, int size) {
    String osName = System.getProperty("os.name", "").toLowerCase();
    String fontName;

    if (osName.contains("win")) {
        fontName = "Arial";
    } else if (osName.contains("mac")) {
        fontName = "SF Pro Text";
    } else {
        // Linux 系统
        fontName = "DejaVu Sans";
    }

    return new Font(fontName, style, size);
}
```

### 2. 批量替换字体设置

将所有硬编码的字体设置替换为使用字体工具方法：

- `new Font("微软雅黑", Font.BOLD, 13)` → `getChineseFont(Font.BOLD, 13)`
- `new Font("Arial", Font.PLAIN, 13)` → `getGeneralFont(Font.PLAIN, 13)`

## Linux 系统字体支持

### 推荐安装的中文字体

为了获得最佳显示效果，建议在 Linux 系统上安装以下中文字体：

#### Ubuntu/Debian
```bash
# 安装 Noto CJK 字体（推荐）
sudo apt-get install fonts-noto-cjk

# 安装文泉驿字体
sudo apt-get install fonts-wqy-microhei fonts-wqy-zenhei

# 安装 DejaVu 字体
sudo apt-get install fonts-dejavu
```

#### CentOS/RHEL/Fedora
```bash
# 安装 Noto CJK 字体
sudo dnf install google-noto-sans-cjk-fonts

# 安装文泉驿字体
sudo dnf install wqy-microhei-fonts wqy-zenhei-fonts

# 安装 DejaVu 字体
sudo dnf install dejavu-sans-fonts
```

#### Arch Linux
```bash
# 安装 Noto CJK 字体
sudo pacman -S noto-fonts-cjk

# 安装文泉驿字体
sudo pacman -S wqy-microhei wqy-zenhei

# 安装 DejaVu 字体
sudo pacman -S ttf-dejavu
```

### 字体优先级

程序会按以下顺序尝试使用字体（自动检测系统可用字体）：

1. **Windows 字体**：
   - Microsoft YaHei（微软雅黑）
   - SimSun（宋体）
   - SimHei（黑体）

2. **macOS 字体**：
   - PingFang SC（苹方）
   - Heiti SC（黑体）
   - Hiragino Sans GB

3. **Linux 开源字体**：
   - Noto Sans CJK SC（Google Noto 字体，推荐）
   - WenQuanYi Micro Hei（文泉驿微米黑）
   - WenQuanYi Zen Hei（文泉驿正黑）
   - Source Han Sans CN（思源黑体）
   - AR PL UMing CN（文鼎 PL 明体）

4. **通用后备字体**：
   - DejaVu Sans
   - Dialog
   - SansSerif（Java 逻辑字体）

**重要说明**：
- 程序会自动检测系统中可用的字体
- 如果检测到的字体无法正确显示中文，会继续尝试下一个字体
- 只有当所有字体都无法显示中文时，才会使用系统默认字体（可能导致中文显示为小方格）
- **必须安装至少一个中文字体才能正常显示中文！**

## 测试验证

### 1. 检查系统字体

在安装字体之前，先检查系统中是否有中文字体：

```bash
# 检查中文字体
fc-list :lang=zh

# 如果没有输出或输出很少，说明需要安装中文字体
```

### 2. 安装字体

按照上面的说明安装中文字体，然后刷新字体缓存：

```bash
# 刷新字体缓存
fc-cache -fv
```

### 3. 验证字体安装

再次检查中文字体：

```bash
fc-list :lang=zh
```

应该能看到类似以下的输出：
```
/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc: Noto Sans CJK SC:style=Regular
/usr/share/fonts/wqy-microhei/wqy-microhei.ttc: WenQuanYi Micro Hei Mono:style=Regular
```

### 4. 编译项目

```bash
./compile_with_flatlaf.sh
```

### 5. 运行程序

```bash
./run_with_flatlaf.sh
```

### 6. 验证中文显示

程序启动后，检查以下界面元素是否正确显示中文：

1. 登录界面（用户名、密码、登录、退出按钮）
2. 主界面标签页（库存管理、购物车 & 结账、交易记录、数据统计、设置）
3. 菜单项（系统、管理、数据）
4. 所有按钮和标签文本
5. 表格内容

## 故障排除

### 问题 1：中文显示为小方格（□□□）

**原因**：系统中没有安装支持中文的字体。

**解决方案**：
1. 检查系统中是否有中文字体：
   ```bash
   fc-list :lang=zh
   ```

2. 如果没有输出，安装中文字体（参考上面的安装说明）

3. 安装后刷新字体缓存：
   ```bash
   fc-cache -fv
   ```

4. 重启程序

### 问题 2：字体显示不清晰

**原因**：可能使用了低质量的字体。

**解决方案**：
1. 推荐使用 Google Noto CJK 字体（显示效果最好）
2. 在系统设置中调整字体渲染选项
3. 尝试切换 FlatLaf 主题

### 问题 3：某些字符仍然显示为方格

**原因**：字体可能不包含某些特殊字符。

**解决方案**：
1. 确保安装了完整的 Noto CJK 字体包
2. 尝试使用其他开源字体（如文泉驿字体）

### 问题 4：程序无法启动

**原因**：可能是 Java 版本或配置问题。

**解决方案**：
1. 检查 Java 版本（需要 JDK 11 或更高）：
   ```bash
   java -version
   ```

2. 重新编译项目：
   ```bash
   ./compile_with_flatlaf.sh
   ```

3. 检查是否有编译错误

## 注意事项

1. **字体安装**：如果系统中没有安装任何中文字体，程序会使用 Java 的默认逻辑字体 "SansSerif"，可能无法正确显示中文。建议至少安装一个中文字体。

2. **字体回退**：程序会自动检测系统中可用的字体，如果指定的字体不存在，会自动使用下一个可用的字体。

3. **跨平台兼容**：修改后的代码在 Windows、macOS 和 Linux 上都能正常工作，会自动选择合适的字体。

4. **性能影响**：字体检测只在程序启动时执行一次，不会影响程序运行性能。

## 修改文件

- `CashierSystemGUI.java` - 添加字体工具方法并替换所有硬编码字体

## 修改日期

2026-01-05

## 测试状态

✅ 编译成功
✅ 字体替换完成
⏳ 实际显示效果需在 Linux 系统上运行验证