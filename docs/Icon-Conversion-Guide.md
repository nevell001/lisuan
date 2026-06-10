# Windows ICO 图标创建指南

## 为什么需要 ICO 图标？

jpackage 在 Windows 上打包需要 ICO 格式的图标。PNG 格式虽然可以工作，但可能导致：
- 任务栏图标不显示
- 程序列表图标缺失
- Alt+Tab 切换时图标异常

## 创建方法

### 方法一：在线转换（最简单）

1. 访问 https://www.icoconverter.com/
2. 上传 `src/main/resources/images/logos/app-icon.png`
3. 选择生成多分辨率 ICO（256, 128, 64, 48, 32, 16）
4. 下载并保存为 `app-icon.ico`
5. 替换项目中的图标文件

### 方法二：ImageMagick

```bash
# 安装 ImageMagick 后运行
convert src/main/resources/images/logos/app-icon.png \
  -define icon:auto-resize=256,128,96,64,48,32,16 \
  src/main/resources/images/logos/app-icon.ico
```

### 方法三：GIMP

1. 用 GIMP 打开 `app-icon.png`
2. 菜单：文件 → 导出为
3. 选择 ICO 格式
4. 在导出对话框中：
   - 勾选"写入多分辨率"
   - 选择所需分辨率
5. 保存为 `app-icon.ico`

### 方法四：Photoshop 插件

1. 安装 [ICOFormat 插件](https://www.telegraphics.com.au/sw/)
2. 用 Photoshop 打开 PNG
3. 文件 → 另存为 → ICO
4. 选择所需分辨率
5. 保存

## 验证图标

创建完成后，检查：

```batch
# Windows 命令行查看图标
dir src\main\resources\images\logos\app-icon.ico
```

图标文件应包含多个分辨率，以适应不同显示场景。

## 完成后

创建好 ICO 文件后，运行打包命令：

```batch
jpackage.bat
```

或使用 Maven：

```bash
mvn clean package jpackage:jpackage
```

## 狸算

狸算 - 现代化收银系统
