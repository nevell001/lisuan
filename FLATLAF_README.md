# 收银系统 - FlatLaf 现代化版本

## 概述

本收银系统已经使用 **FlatLaf** 开源框架进行了界面优化，界面更加现代、简洁、美观。

## FlatLaf 简介

FlatLaf 是一个现代化的 Swing Look and Feel，提供了：
- 简洁扁平的界面设计
- 优雅的动画效果
- 多种主题选择（浅色、深色、IntelliJ）
- 更好的可访问性
- 高性能渲染

## 运行方式

### Windows 用户
双击运行 `run_with_flatlaf.bat` 文件

### Mac/Linux 用户
```bash
./run_with_flatlaf.sh
```

或者直接运行：
```bash
java -cp flatlaf-3.5.jar:. CashierSystemGUI
```

## 新功能

### 1. 主题切换
在设置面板中，可以选择以下主题：
- **浅色主题 (Light)**: 明亮清爽的界面
- **深色主题 (Dark)**: 护眼的深色界面
- **IntelliJ主题**: 类似IDE的专业风格

切换主题后，点击"🔄 应用主题"按钮即可生效。

### 2. 现代化界面
- 扁平化设计
- 流畅的动画效果
- 更好的按钮和表格样式
- 统一的视觉风格

### 3. 简洁的代码
- 移除了大量自定义样式代码
- 使用FlatLaf的默认样式
- 更易维护和扩展

## 系统要求

- Java 11 或更高版本
- FlatLaf 3.5 (已包含在项目中)

## 文件说明

- `flatlaf-3.5.jar`: FlatLaf框架库文件
- `CashierSystemGUI.java`: 主程序文件
- `DataManager.java`: 数据管理类
- `Product.java`: 商品类
- `Transaction.java`: 交易类
- `run_with_flatlaf.sh`: Mac/Linux启动脚本
- `run_with_flatlaf.bat`: Windows启动脚本

## 注意事项

1. 首次运行时，请确保 `flatlaf-3.5.jar` 文件在同一目录下
2. 如果主题切换后界面没有立即更新，请尝试重启程序
3. 数据自动保存在 `data/` 目录下
4. 备份数据时，请确保有写入权限

## 技术栈

- **GUI框架**: Swing
- **Look and Feel**: FlatLaf 3.5
- **数据存储**: 文本文件
- **字体**: 系统默认字体

## 许可证

FlatLaf 使用 Apache 2.0 许可证

## 参考资料

- FlatLaf 官网: https://www.formdev.com/flatlaf/
- FlatLaf GitHub: https://github.com/JFormDesigner/FlatLaf