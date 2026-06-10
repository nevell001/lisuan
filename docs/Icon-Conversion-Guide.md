# Windows ICO 图标创建指南

## 为什么需要 ICO 图标？

jpackage 在 Windows 上打包需要 ICO 格式的图标。PNG 格式虽然可以工作，但可能导致：
- 任务栏图标不显示
- 程序列表图标缺失
- Alt+Tab 切换时图标异常

## 源文件

**图标源文件**: `src/main/resources/images/logos/logo.png`

## 创建方法

### 方法一：在线转换（推荐）

1. 访问 https://www.icoconverter.com/
   或 https://convertio.co/zh/png-ico/
2. 上传 `src/main/resources/images/logos/logo.png`
3. 选择生成多分辨率 ICO（256, 128, 64, 48, 32, 16）
4. 下载并重命名为 `app-icon.ico`
5. 替换 `src/main/resources/images/logos/app-icon.ico`

### 方法二：ImageMagick (Windows/Linux)

```bash
convert src/main/resources/images/logos/logo.png \
  -define icon:auto-resize=256,128,96,64,48,32,16 \
  src/main/resources/images/logos/app-icon.ico
```

### 方法三：GIMP

1. 用 GIMP 打开 `src/main/resources/images/logos/logo.png`
2. 菜单：图像 → 缩放图像 → 调整为 256x256
3. 文件 → 导出为
4. 选择 ICO 格式
5. 勾选"写入多分辨率"
6. 保存为 `app-icon.ico`

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

## 狸算收银系统

狸算收银系统 - 现代化收银系统
