# 收银系统 JavaFX 版本

## 项目概述

这是收银系统的 JavaFX 版本，提供现代化的 UI 和更好的用户体验。

## 技术栈

- **JavaFX**: 17.0.8
- **JDK**: 17 或 21
- **构建工具**: Maven
- **架构模式**: MVC

## 项目结构

```
hello/
├── src/
│   ├── main/
│   │   ├── java/com/cashier/
│   │   │   ├── CashierSystemFXApplication.java  # 主程序
│   │   │   ├── controller/                      # 控制器层
│   │   │   ├── service/                         # 服务层
│   │   │   ├── model/                           # 模型层（实体类）
│   │   │   ├── util/                            # 工具类
│   │   │   └── constant/                        # 常量
│   │   └── resources/
│   │       ├── css/                             # 样式文件
│   │       ├── fonts/                           # 字体文件
│   │       ├── images/                          # 图标和图片
│   │       └── com/cashier/view/                # FXML 视图
│   └── test/
├── data/                                         # 数据目录
└── pom.xml                                      # Maven 构建文件
```

## 环境要求

### 必需软件

1. **JDK 17 或 21**
   - 下载地址: https://www.oracle.com/java/technologies/downloads/
   - 或使用 OpenJDK: https://adoptium.net/

2. **Maven 3.8+**
   - 下载地址: https://maven.apache.org/download.cgi
   - 或使用 Chocolatey 安装: `choco install maven`

3. **JavaFX SDK 17**
   - 下载地址: https://gluonhq.com/products/javafx/
   - 解压到指定目录

### 环境变量配置

设置以下环境变量：

```bash
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17
set MAVEN_HOME=C:\Program Files\Apache\maven
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%
set JAVAFX_HOME=C:\javafx-sdk-17
```

## 构建项目

### 使用 Maven 构建

```bash
# 清理并编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn package

# 跳过测试打包
mvn package -DskipTests
```

### 使用 JavaFX Maven 插件运行

```bash
# 运行应用程序
mvn javafx:run
```

### 运行打包后的 JAR

```bash
# Windows
java -jar target/cashier-system-fx-2.0.0.jar

# Mac/Linux
java -jar target/cashier-system-fx-2.0.0.jar
```

## 开发指南

### 添加新功能

1. **创建控制器** (controller 包)
2. **创建视图** (resources/com/cashier/view 包)
3. **创建服务** (service 包，如需要)
4. **在主控制器中集成**

### 样式定制

所有样式文件位于 `src/main/resources/css/` 目录：

- `styles.css` - 主样式文件
- `light-theme.css` - 浅色主题
- `dark-theme.css` - 深色主题
- `intellij-theme.css` - IntelliJ 主题

### 主题切换

在控制器中调用：

```java
application.applyTheme(scene, "light");  // 浅色主题
application.applyTheme(scene, "dark");   // 深色主题
application.applyTheme(scene, "intellij"); // IntelliJ 主题
```

## 功能模块

### 已实现

- ✅ 登录系统
- ✅ 主界面框架
- ✅ 主题切换（浅色、深色、IntelliJ）
- ✅ 基础 UI 组件样式

### 开发中

- 🚧 库存管理
- 🚧 购物车
- 🚧 结账
- 🚧 会员管理
- 🚧 交易记录
- 🚧 数据统计
- 🚧 用户管理
- 🚧 交接班
- 🚧 促销管理
- 🚧 设置

### 待实现

- ⏳ 小票打印
- ⏳ 条形码扫描
- ⏳ 数据导出（Excel、CSV）
- ⏳ 更多图表

## 默认账户

系统首次运行时会自动创建默认管理员账户：

- **用户名**: `admin`
- **密码**: `admin123`
- **角色**: 管理员

## 数据兼容性

JavaFX 版本与 Swing 版本使用相同的数据格式，可以无缝切换：

- 数据文件位置: `data/` 目录
- 数据格式: 文本文件（.txt）
- 实体类: 完全兼容

## 快捷键

### 功能键
- **F1** - 添加商品
- **F2** - 补货
- **F3** - 删除商品
- **F4** - 搜索
- **F5** - 刷新当前面板
- **F6** - 分类管理
- **F7** - 会员管理
- **F8** - 结账
- **F9** - 促销管理
- **F10** - 库存预警
- **F11** - 数据备份
- **F12** - 数据恢复
- **ESC** - 清空搜索
- **Delete** - 删除选中项

### Ctrl 组合键
- **Ctrl+N** - 添加商品
- **Ctrl+S** - 保存数据
- **Ctrl+F** - 搜索
- **Ctrl+D** - 导出数据
- **Ctrl+R** - 刷新当前面板
- **Ctrl+Q** - 退出程序
- **Ctrl+A** - 全选
- **Ctrl+E** - 编辑选中项
- **Ctrl+B** - 批量操作
- **Ctrl+M** - 会员管理
- **Ctrl+T** - 交易统计
- **Ctrl+1** - 切换到库存管理
- **Ctrl+2** - 切换到购物车
- **Ctrl+3** - 切换到交易记录
- **Ctrl+4** - 切换到设置

## 故障排除

### 编译错误

**问题**: 找不到 JavaFX 类
**解决**: 确保 JDK 版本为 17 或 21，并正确配置环境变量

**问题**: Maven 依赖下载失败
**解决**: 检查网络连接，或配置 Maven 镜像源

### 运行错误

**问题**: 无法加载 FXML 文件
**解决**: 确保 FXML 文件位于 `resources/com/cashier/view/` 目录

**问题**: CSS 样式不生效
**解决**: 确保 CSS 文件位于 `resources/css/` 目录

### 数据问题

**问题**: 无法加载数据
**解决**: 确保 `data/` 目录存在，并有正确的权限

## 开发工具推荐

- **IDE**: IntelliJ IDEA, Eclipse, VS Code
- **FXML 编辑器**: Scene Builder
- **版本控制**: Git

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

木兰宽松许可证 v2 (MulanPSL2)

## 联系方式

- 项目仓库: https://gitee.com/nevell/hello
- 问题反馈: https://gitee.com/nevell/hello/issues

## 更新日志

### v2.0.0 (2026-01-06)
- ✨ 迁移到 JavaFX 框架
- ✨ 实现登录系统
- ✨ 实现主界面框架
- ✨ 支持三种主题（浅色、深色、IntelliJ）
- ✨ 创建完整的 CSS 样式系统
- ✨ 创建基础工具类和常量
- ✨ 复用现有实体类和数据管理器

---

**注意**: 本项目仅供学习和参考使用，不建议直接用于生产环境。