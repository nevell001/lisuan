# 应用图标和资源指南

本文档说明如何为收银系统创建和配置应用图标。

## 图标要求

### Windows 图标 (.ico)

- **尺寸**: 需要包含多种尺寸（16x16, 32x32, 48x48, 64x64, 128x128, 256x256）
- **格式**: ICO 格式
- **颜色**: 支持 32 位真彩色和 8 位透明度
- **用途**:
  - 应用程序窗口图标
  - 任务栏图标
  - 桌面快捷方式图标
  - 开始菜单图标

### macOS 图标 (.icns)

- **尺寸**: 需要包含多种尺寸（16x16, 32x32, 128x128, 256x256, 512x512, 1024x1024）
- **格式**: ICNS 格式
- **用途**:
  - 应用程序图标
  - Dock 栏图标
  - Finder 中的图标

### PNG 图标

- **尺寸**: 建议至少 512x512 像素
- **格式**: PNG 格式，支持透明背景
- **用途**:
  - JavaFX 应用图标
  - Splash 屏幕
  - Web 应用图标（如果有）

## 图标设计建议

### 主题和风格

- **风格**: 现代、简洁、专业
- **颜色**: 使用品牌色调（建议蓝色或绿色为主）
- **元素**: 可以包含以下元素之一：
  - 收银机图标
  - 购物车图标
  - 金钱/货币符号（¥ 或 $）
  - 店铺/商店图标

### 设计原则

1. **简洁性**: 避免过多细节，确保在小尺寸下仍然清晰可辨
2. **对比度**: 使用适当的对比度，确保在不同背景下都能看清
3. **一致性**: 图标风格应与应用界面风格保持一致
4. **专业性**: 体现收银系统的专业性和可靠性

## 图标文件位置

```
src/main/resources/images/
├── app-icon.png          # JavaFX 应用图标 (512x512)
├── app-icon.ico          # Windows 图标 (多尺寸)
├── app-icon.icns         # macOS 图标 (多尺寸)
└── logos/
    └── app-icon.png      # 备用位置
```

## 创建图标的方法

### 方法 1: 在线工具

推荐使用以下在线工具创建图标：

1. **ICO Converter** (Windows)
   - 网站: https://icoconvert.com/
   - 步骤:
     1. 上传 PNG 图片
     2. 选择需要的尺寸
     3. 下载 ICO 文件

2. **Canva**
   - 网站: https://www.canva.com/
   - 步骤:
     1. 选择 "Logo" 模板
     2. 设计图标
     3. 导出为 PNG
     4. 使用在线工具转换为 ICO

3. **Favicon.io**
   - 网站: https://favicon.io/
   - 步骤:
     1. 上传图片或使用文字/图标
     2. 自动生成多种尺寸
     3. 下载图标包

### 方法 2: 使用专业设计软件

1. **Adobe Illustrator**
   - 使用矢量图形设计
   - 导出为不同尺寸的 PNG
   - 使用插件或在线工具转换为 ICO

2. **Photoshop**
   - 设计图标
   - 导出为 PNG
   - 使用 "ICO Format" 插件保存为 ICO

3. **GIMP (免费)**
   - 设计图标
   - 使用 "保存为" → ICO 格式
   - 支持多尺寸打包

### 方法 3: 使用编程工具

1. **ImageMagick (命令行)**
   ```bash
   # 从 PNG 创建 ICO
   convert icon.png -define icon:auto-resize=256,128,96,64,48,32,16 icon.ico
   ```

2. **Java 代码**
   - 可以使用 Java 的 `BufferedImage` 和 `ImageIO` 类生成图标

## 图标配置

### JavaFX 应用图标

在 `src/main/java/com/cashier/CashierSystemFXApplication.java` 中：

```java
private void setupApplicationIcon() {
    try {
        URL iconUrl = getClass().getResource("/images/app-icon.png");
        if (iconUrl != null) {
            primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
    } catch (Exception e) {
        System.err.println("无法加载应用图标: " + e.getMessage());
    }
}
```

### Windows 打包图标

在 `package-windows.bat` 中：

```batch
if exist "src\main\resources\images\app-icon.ico" (
    set ICON_PATH=--icon src\main\resources\images\app-icon.ico
)
```

### macOS 打包图标

在 macOS 打包脚本中：

```bash
--icon src/main/resources/images/app-icon.icns
```

## 图标文件清单

为了完整的 Windows 支持，需要准备以下图标文件：

- [ ] `src/main/resources/images/app-icon.png` (512x512 或更大)
- [ ] `src/main/resources/images/app-icon.ico` (多尺寸: 16, 32, 48, 64, 128, 256)
- [ ] `src/main/resources/images/app-icon.icns` (多尺寸: 16, 32, 128, 256, 512, 1024)
- [ ] `src/main/resources/images/logos/app-icon.png` (备用)

## 测试图标

### 测试 JavaFX 图标

1. 运行应用：`mvn javafx:run`
2. 检查窗口左上角是否有图标
3. 检查任务栏是否有图标

### 测试 Windows 打包图标

1. 运行打包脚本：`package-windows.bat`
2. 安装生成的 MSI 文件
3. 检查桌面快捷方式、开始菜单、任务栏图标

### 测试 macOS 图标

1. 运行 macOS 打包脚本
2. 安装应用程序
3. 检查 Dock 栏和 Finder 中的图标

## 常见问题

### 问题 1: 图标显示不正确

**解决方案**:
- 检查图标文件路径是否正确
- 确保图标文件格式正确
- 清除应用程序缓存并重新运行

### 问题 2: 图标模糊

**解决方案**:
- 确保图标包含足够大的尺寸（至少 256x256）
- 使用矢量图形设计，然后转换为位图
- 检查图标的 DPI 设置

### 问题 3: Windows 打包后图标未显示

**解决方案**:
- 确保 ICO 文件包含所有必需的尺寸
- 检查 ICO 文件格式是否正确
- 重新打包应用程序

## 图标资源

### 免费图标网站

- **Flaticon**: https://www.flaticon.com/
- **Icons8**: https://icons8.com/
- **IconFinder**: https://www.iconfinder.com/
- **Freepik**: https://www.freepik.com/

### 图标设计工具

- **Canva**: https://www.canva.com/
- **Figma**: https://www.figma.com/
- **Sketch**: https://www.sketch.com/
- **GIMP**: https://www.gimp.org/

### 图标转换工具

- **ICO Convert**: https://icoconvert.com/
- **ConvertICO**: https://convertico.com/
- **Online Icon Maker**: https://onlineiconmaker.com/

## 下一步

1. 设计或下载合适的图标
2. 使用工具生成不同格式和尺寸的图标
3. 将图标文件放置到正确的位置
4. 测试图标在不同场景下的显示效果
5. 如果需要，调整图标设计

## 相关文档

- [项目 README](../README.md)
- [Windows 打包指南](../docs/WINDOWS_MYSQL_SETUP.md)
- [JavaFX 图标文档](https://openjfx.io/javadoc/17/javafx.graphics/javafx/stage/Stage.html)

## 技术支持

如有问题，请访问：
- 项目 Issue: https://gitee.com/nevell/hello/issues