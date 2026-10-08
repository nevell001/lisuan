# Application Icons

## 状态：图标已提供

图标文件位于子目录 `logos/`，应用启动时会加载；文件缺失时回退 JavaFX 默认图标。

## 容错处理

代码已包含容错处理：如果图标文件不存在，应用会正常启动，仅显示默认图标。

## 需要的图标文件

1. **app-icon.png** (256x256) - JavaFX 跨平台图标
2. **app-icon.ico** - Windows 快捷方式图标

参考 [logos/README.md](logos/README.md) 了解图标资产与再生成方法。

## 快速生成图标

访问以下在线工具上传 SVG 并生成所需格式：
- https://www.favicon.cc/
- https://www.icoconverter.com/
- https://cloudconvert.com/svg-to-png
