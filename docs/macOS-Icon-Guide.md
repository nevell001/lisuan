# macOS 图标创建指南

## 为什么需要 ICNS 图标？

jpackage 在 macOS 上打包需要 ICNS 格式的图标。PNG 格式会被忽略，使用默认图标。

## 创建方法

### 方法一：在线转换（推荐）

1. 访问 https://www.icoconverter.com/（支持 ICNS）
2. 或访问 https://cloudconvert.com/png-to-icns
3. 上传 `src/main/resources/images/logos/app-icon.png`
4. 选择生成 ICNS
5. 下载并保存为 `app-icon.icns`

### 方法二：使用 iconutil（macOS 自带）

```bash
# 创建 iconset 目录
mkdir -p app-icon.iconset

# 生成不同尺寸
sips -z 16 16     app-icon.png --out app-icon.iconset/icon_16x16.png
sips -z 32 32     app-icon.png --out app-icon.iconset/icon_16x16@2x.png
sips -z 32 32     app-icon.png --out app-icon.iconset/icon_32x32.png
sips -z 64 64     app-icon.png --out app-icon.iconset/icon_32x32@2x.png
sips -z 128 128   app-icon.png --out app-icon.iconset/icon_128x128.png
sips -z 256 256   app-icon.png --out app-icon.iconset/icon_128x128@2x.png
sips -z 256 256   app-icon.png --out app-icon.iconset/icon_256x256.png
sips -z 512 512   app-icon.png --out app-icon.iconset/icon_256x256@2x.png
sips -z 512 512   app-icon.png --out app-icon.iconset/icon_512x512.png
sips -z 1024 1024 app-icon.png --out app-icon.iconset/icon_512x512@2x.png

# 转换为 ICNS
iconutil -c icns app-icon.iconset

# 移动到项目目录
mv app-icon.icns src/main/resources/images/logos/
```

### 方法三：ImageMagick

```bash
convert app-icon.png -define icon:auto-resize=16,32,64,128,256,512,1024 app-icon.icns
```

## 更新脚本

创建好 ICNS 文件后，更新 `jpackage.sh`：

```bash
# 找到这一行（约 91 行）
--icon src/main/resources/images/logos/app-icon.png

# 改为
--icon src/main/resources/images/logos/app-icon.icns
```

## 狸算收银系统

狸算收银系统 - 现代化收银系统
