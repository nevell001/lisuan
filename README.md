# 收银系统 (Cashier System)

一个功能完整的收银系统，使用 JavaFX 17 开发，提供现代化的图形化界面。

**当前版本**: v2.5.4 | **最新更新**: 2026-05-21

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

## ✨ 核心特性

### 🖥️ 现代化图形界面
- 基于 JavaFX 17.0.12 的现代化界面
- 支持浅色、深色、IntelliJ 三种主题
- 流畅的动画效果和微交互

### 💰 核心功能
- **POS系统** - 购物车和结账一体化，支持现金、微信、支付宝、银行卡支付
- **商品管理** - 商品添加、编辑、删除、快速入库、搜索、库存预警
- **会员管理** - 会员注册、积分、等级自动升级、折扣、余额充值
- **促销管理** - 满减、折扣、优惠券等多种促销类型
- **交易记录** - 完整的交易历史记录和查询
- **数据统计** - 销售额、交易量、平均客单价等统计
- **交接班管理** - 班次开始/结束、交接班记录、收入统计
- **用户管理** - 用户增删改查、权限管理（管理员、收银员、财务）
- **系统设置** - 主题切换、税率配置、数据备份/恢复

### 🏭 进销存功能
- **采购管理** - 供应商管理、采购订单、采购审批、采购入库
- **库存盘点** - 创建盘点单、实际库存录入、自动计算差异
- **报表统计** - 采购报表、库存报表、利润分析

### 🔄 退货管理
- **退货订单创建** - 基于原交易创建退货订单
- **退货审批流程** - 审批通过/拒绝、自动库存管理
- **退货统计** - 退货订单统计、退货金额统计

### 🔧 数据管理
- **数据导入** - 支持 CSV 文件导入、GitHub 商品条码库导入
- **数据导出** - 支持 Excel 和 PDF 格式导出
- **缓存管理** - 商品数据缓存（5分钟过期）
- **数据备份** - 自动和手动数据备份

### 🔔 消息通知
- **实时通知** - 系统操作实时通知提醒
- **通知类型** - 支持信息、警告、错误、成功等多种类型
- **通知管理** - 通知历史记录和管理

### 🖨️ 硬件支持
- **打印机管理** - 支持多种打印机设备、打印预览、打印模板定制
- **扫描枪管理** - 支持 USB HID 扫描枪、自动检测、智能焦点管理

### 🔒 安全与权限
- 三种角色权限管理（管理员、收银员、财务）
- 操作日志完整记录
- 密码 BCrypt 加密存储

## 🗺️ 开发路线图 (v2.6.0)

> v2.5.0 已完成生产级核心功能，后续版本专注于优化与扩展

| 功能模块 | 优先级 | 状态 |
|---------|--------|------|
| **REST API** | P0 | ✅ 已完成 (60+ 端点) |
| **多终端同步** | P0 | ✅ 已完成 (WebSocket) |
| **电子支付集成** (微信/支付宝) | P1 | ✅ 已完成 (11 端点) |
| **发票功能** | P1 | ✅ 已完成 (10 端点) |
| **云备份** | P2 | ✅ 已完成 (9 端点) |
| **网络打印** | P2 | ✅ 已完成 (14 端点) |
| **多语言支持** | P2 | ✅ 已完成 (6 端点) |

详细规划见：[docs/ROADMAP.md](docs/ROADMAP.md)

## 🚀 快速开始

### 环境要求
- **JDK**: Java 17 或更高版本
- **Maven**: 3.8 或更高版本（开发需要）
- **MySQL**: 8.4 或更高版本
- **操作系统**: Windows、macOS、Linux

### Windows 用户（推荐）

1. **获取分发包**
   - 下载 `CashierSystem-v{version}.zip`
   - 解压到任意目录

2. **配置数据库**
   - 双击运行 `Database Config.bat`
   - 选择数据库类型：
     - **Local MySQL** - 本地安装的 MySQL
     - **Docker MySQL** - Docker 容器（推荐）
     - **Remote MySQL** - 远程 MySQL 服务器
   - 点击 "Test Connection" 测试连接
   - 点击 "Save & Start" 保存配置

3. **启动应用**
   - 配置完成后选择 "Yes" 自动启动
   - 或双击 `Quick Start.bat` 手动启动

### 开发者安装

1. **克隆仓库**
```bash
git clone https://gitee.com/nevell/hello.git
cd hello
```

2. **启动 MySQL 数据库**

**使用 Docker Compose（推荐）**：
```bash
# 启动 MySQL 数据库
docker compose up -d mysql
```

**使用本地 MySQL**：
```bash
mysql -u root -p

CREATE DATABASE cashier_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cashier'@'%' IDENTIFIED BY 'YourPassword123!';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
FLUSH PRIVILEGES;
```

3. **编译并运行**
```bash
mvn clean compile
mvn javafx:run
```

**打包后运行**：
```bash
mvn clean package
java -jar target/cashier-system-fx-2.5.4-jar-with-dependencies.jar
```

### 创建分发包

```bash
# Windows
package.bat

# 手动打包
mvn clean package -DskipTests
mkdir -p dist/CashierSystem
cp target/cashier-system-fx-*-jar-with-dependencies.jar dist/CashierSystem/
cp start.bat dist/CashierSystem/
cp create-shortcut.bat dist/CashierSystem/
```

### 默认账户
- **用户名**: `admin`
- **初始密码**: 首次启动时自动生成（查看 `logs/app.log` 或控制台输出）
- **角色**: 管理员

## 📖 主要功能

### 快捷键
**POS/结账页面**：
- `F1` - 添加商品到购物车
- `Delete` - 移除选中商品
- `Ctrl+L` - 清空购物车
- `F8` - 现金支付
- `Ctrl+1/2/3` - 微信/支付宝/银行卡支付
- `Ctrl+F` - 聚焦搜索框
- `Ctrl+M` - 聚焦会员手机号框
- `Ctrl+/` - 显示快捷键帮助

### 会员等级
- **普通会员**: 0-999积分
- **银卡会员**: 1000-4999积分（9.5折）
- **金卡会员**: 5000-9999积分（9折）
- **钻石会员**: 10000+积分（8.5折）

## 🎯 最近更新

### v2.5.4 (2026-05-21) - Windows 分发包优化 📦
- 🖥️ **GUI 数据库配置工具** - 图形化数据库配置界面
  - 下拉菜单选择数据库类型（Local/Docker/Remote MySQL）
  - 实时连接测试
  - 自动创建数据库
  - 配置保存后可直接启动应用
- 📦 **简化分发流程** - 一键打包 Windows 分发包
  - `package.bat` 创建完整分发包
  - 包含 GUI 配置工具和启动脚本
  - 自动生成 ZIP 压缩包
- 🗂️ **清理冗余文件** - 移除 18+ 个过时的批处理脚本
- 📝 **更新文档** - 重写 INSTALLER.md 以反映新工作流程

### v2.5.3 (2026-05-19) - Windows 平台优化 🪟
- 🔒 **单实例限制** - 使用 FileLock 机制防止应用多次启动
- 🎨 **Splash 启动画面** - 专业的加载画面提升用户体验
- 🚀 **启动脚本优化** - start.bat 增强功能
- 📦 **jpackage 配置** - 支持原生 Windows EXE 打包

### v2.5.0 (2026-05-06) - 生产级版本 🎉
重大里程碑版本，系统已具备生产级能力：
- 🌐 **REST API** - 60+ 端点，支持远程访问和多终端集成
- 🔄 **多终端同步** - WebSocket 实时数据同步
- 💳 **电子支付** - 11 个端点，微信/支付宝集成接口
- 🧾 **发票功能** - 10 个端点，发票开具和管理
- 🖨️ **网络打印** - 14 个端点，远程打印机管理
- ☁️ **云备份** - 9 个端点，数据云端备份
- 🌍 **多语言** - 6 个端点，国际化支持

项目规模：179 个 Java 文件，2.2MB 源码，126 个测试全部通过

## 📁 项目结构

```
hello/
├── pom.xml                          # Maven 配置
├── package.bat                      # Windows 打包脚本
├── start.bat                        # Windows 启动脚本
├── create-shortcut.bat              # 快捷方式创建工具
├── install.sh                       # Linux/macOS 安装脚本
├── src/main/java/com/cashier/
│   ├── CashierSystemFXApplication.java  # 主程序入口
│   ├── constant/                       # 常量定义
│   ├── dao/                            # 数据访问层
│   ├── controller/                     # 控制器层
│   ├── model/                          # 实体类
│   ├── service/                        # 服务层
│   ├── installer/                      # 安装配置工具
│   ├── printer/                        # 打印机管理模块
│   ├── scanner/                        # 扫描枪管理模块
│   ├── notification/                   # 消息通知模块
│   └── util/                           # 工具类
├── src/main/resources/
│   ├── com/cashier/view/               # FXML 视图文件
│   ├── css/                            # 样式文件
│   ├── fonts/                          # 字体文件
│   ├── images/                         # 图片资源
│   └── sounds/                         # 音效文件
├── config/                             # 配置目录
├── docker/                             # Docker 配置
│   └── mysql-init/                     # MySQL 初始化脚本
├── docs/                               # 项目文档
└── dist/                               # 分发包输出（已忽略）
```

## 🛠️ 技术栈

- **前端框架**: JavaFX 17.0.12
- **构建工具**: Maven 3.8+
- **编程语言**: Java 17
- **数据库**: MySQL 8.4
- **连接池**: HikariCP 5.1.0
- **数据导出**: Apache POI 5.2.5 (Excel) + Apache PDFBox 2.0.32 (PDF)
- **测试框架**: JUnit 5 + TestFX + H2 Database

## 🔧 故障排除

### 应用无法启动
- 检查 JDK 版本是否为 17 或更高
- 检查 MySQL 服务是否运行
- 查看 `config/database.properties` 配置
- 查看 `logs/app.log` 获取详细错误信息

### 数据库连接失败
- 确保 MySQL 8.0 正在运行
- 检查数据库用户名和密码
- 确认防火墙允许 3306 端口
- 使用 `Database Config.bat` 重新配置

### 扫描枪无法工作
- 确认扫描枪已正确连接（USB 接口）
- 确认扫描枪处于 HID 模式
- 检查焦点是否在正确的输入框

## 📝 许可证

本项目采用 **木兰宽松许可证 v2 (MulanPSL2)**

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

- 代码仓库: https://gitee.com/nevell/hello.git
- 问题反馈: https://gitee.com/nevell/hello/issues
