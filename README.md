# 收银系统 (Cashier System)

一个功能完整的收银系统，使用 JavaFX 17 开发，提供现代化的图形化界面。

**当前版本**: v2.4.6 | **最新更新**: 2026-04-27

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



## 🚀 快速开始

### 环境要求
- **JDK**: Java 17 或更高版本
- **Maven**: 3.8 或更高版本
- **MySQL**: 8.4 或更高版本
- **操作系统**: Windows、macOS、Linux

### 安装步骤

1. **克隆仓库**
```bash
git clone https://gitee.com/nevell/hello.git
cd hello
```

2. **启动 MySQL 数据库**

**使用 Docker Compose（推荐）**：
```bash
# 启动 MySQL 数据库
docker-compose up -d mysql
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
java -jar target/cashier-system-fx-2.4.6-jar-with-dependencies.jar
```



### 默认账户
- **用户名**: `admin`
- **密码**: `admin123`
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
- **普通会员**: 0-1999积分
- **银卡会员**: 2000-4999积分（9.5折）
- **金卡会员**: 5000-9999积分（9折）
- **钻石会员**: 10000+积分（8.5折）

## 🎯 最近更新

### v2.4.6 (2026-04-27) - 安全优化
- 🔒 **优化 SQL 查询并移除安全隐患**
  - 重构 SQL 查询语句，提升性能和安全性
  - 移除潜在的安全漏洞
  - 升级版本至 v2.4.6

### v2.4.5-fix-javafx-plugin (2026-04-05) - 修复
- 🔧 **修复 javafx-maven-plugin 版本问题**
  - 将 javafx-maven-plugin 版本从 0.0.13 改回 0.0.8
  - 解决了插件版本不存在导致的启动失败问题
  - 确保应用能够正常启动和运行

### v2.4.5-refactor-transaction-semantics (2026-04-04) - 维护检查点
- 🔒 **事务语义统一**
  - `DatabaseManager` / `BaseDAO` 的提交、回滚后行为已统一，都会恢复 `autoCommit=true`
  - Service 层重复事务模板已收敛到统一事务入口
- 🧩 **交易流程收敛**
  - `TransactionService.executeTransaction(...)` 改为统一事务模板
  - 会员余额、积分、等级、折扣更新并入同一事务
  - 促销使用次数更新失败时继续保证整体回滚

### v2.4.5 (2026-03-14) - 发布
- 🔧 **版本管理优化**
  - 创建 AppConstants 集中管理版本号
  - 帮助菜单版本号使用常量
  - 启动脚本自动从 pom.xml 读取版本号
  - 更新所有相关文件版本号到 v2.4.5

## 📁 项目结构

```
hello/
├── pom.xml                          # Maven 配置
├── src/main/java/com/cashier/
│   ├── CashierSystemFXApplication.java  # 主程序入口
│   ├── constant/                       # 常量定义
│   ├── dao/                            # 数据访问层
│   ├── controller/                     # 控制器层
│   ├── model/                          # 实体类
│   ├── service/                        # 服务层
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
├── install.sh                          # Linux/macOS 安装脚本
├── install.bat                         # Windows 安装脚本
└── start.sh / start.bat                # 启动脚本
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

### 数据库连接失败
- 确保 MySQL 8.0 正在运行
- 检查数据库用户名和密码
- 确认防火墙允许 3306 端口

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

