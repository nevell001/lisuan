# 收银系统 (Cashier System)

一个功能完整的收银系统，使用 JavaFX 17 开发，提供现代化的图形化界面。

**当前版本**: v2.4.5 | **最新更新**: 2026-03-14

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.8-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

## ✨ 核心特性

### 🖥️ 现代化图形界面
- 基于 JavaFX 17.0.8 的现代化界面
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
- **数据导出** - 支持 Excel 和 PDF 格式导出（交易记录、库存报表、数据统计等）
- **缓存管理** - 商品数据缓存（5分钟过期）
- **数据备份** - 自动和手动数据备份

### 🔔 消息通知
- **实时通知** - 系统操作实时通知提醒
- **通知类型** - 支持信息、警告、错误、成功等多种类型
- **通知管理** - 通知历史记录和管理

### 🖨️ 硬件支持
- **打印机管理** - 支持多种打印机设备、打印预览、打印模板定制
- **扫描枪管理** - 支持 USB HID 扫描枪、自动检测、智能焦点管理

### 📦 安装程序
- **Windows** - 运行 `install.bat` 图形化安装向导
- **Linux/macOS** - 运行 `./install.sh` 智能安装脚本
- **自动配置** - 自动检测环境并配置数据库连接

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
docker-compose up -d
```

**使用本地 MySQL**：
```bash
mysql -u root -p

CREATE DATABASE cashier_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cashier'@'%' IDENTIFIED BY 'YourPassword123!';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
FLUSH PRIVILEGES;
```

3. **配置数据库连接**

创建 `config/database.properties`：
```properties
db.url=jdbc:mysql://localhost:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
db.username=root
db.password=RootPassword123!
db.pool.size=10
```

4. **编译并运行**
```bash
mvn clean compile
mvn javafx:run
```

**打包后运行**：
```bash
mvn clean package
java -jar target/cashier-system-fx-2.4.5.jar
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

### 数据库架构

**核心表**：
- `users` - 用户账户
- `products` - 商品库存信息（name 字段添加 UNIQUE 约束）
- `members` - 会员账户和积分
- `transactions` - 交易记录主表
- `transaction_items` - 交易明细

**业务表**：
- `suppliers` - 供应商信息
- `purchase_orders` - 采购订单
- `purchase_inbound` - 采购入库
- `inventory_check` - 库存盘点
- `return_orders` - 退货订单

## 🎯 最近更新

### v2.4.5-refactor-transaction-semantics (2026-04-04) - 维护检查点
- 🔒 **事务语义统一**
  - `DatabaseManager` / `BaseDAO` 的提交、回滚后行为已统一，都会恢复 `autoCommit=true`
  - Service 层重复事务模板已收敛到统一事务入口
- 🧩 **交易流程收敛**
  - `TransactionService.executeTransaction(...)` 改为统一事务模板
  - 会员余额、积分、等级、折扣更新并入同一事务
  - 促销使用次数更新失败时继续保证整体回滚
- ✅ **定向回归通过**
  - `BaseDAOTest`、`TransactionServiceTest`、`MemberServiceTest`、`InventoryServiceTest`、`ReturnServiceTest` 共 55/55 通过
- 📝 **详细说明**
  - 见 `docs/RELEASE_NOTE_v2.4.5-refactor-transaction-semantics.md`

### v2.4.5 (2026-03-14) - 发布
- 🔧 **版本管理优化**
  - ✅ 创建 AppConstants 集中管理版本号
  - ✅ 帮助菜单版本号使用常量（MainController、LoginController）
  - ✅ 启动脚本自动从 pom.xml 读取版本号
  - ✅ 更新所有相关文件版本号到 v2.4.5
  - ✅ 删除问题文件 start.vbs
- 📝 **文档改进**
  - 更新 README.md 版本信息
  - 更新 AGENTS.md 版本信息
  - 更新所有启动脚本和安装脚本版本号
- 🎯 **改进统计**
  - 修改文件数量：14 个
  - 新增文件：1 个（AppConstants.java）
  - 删除文件：1 个（start.vbs）
  - 编译状态：✅ BUILD SUCCESS

### v2.4.3 (2026-03-07)
- ✨ **商品名称唯一性约束**
  - 数据库层面添加商品名称 UNIQUE 约束
  - 应用层面添加商品添加/编辑时的名称唯一性检查
  - 提供友好错误提示："商品名称已存在，请使用其他名称"
  - 确保商品数据的一致性和准确性
- 🐛 **修复商品管理界面数据丢失问题**
  - 修复 HashMap 使用商品名称作为 key 导致同名商品被覆盖的问题
  - 改为使用商品 ID 作为 key，确保所有商品都能正确显示
- 🎨 **商品选择对话框优化**
  - 在采购订单和库存盘点的商品选择对话框中添加条形码列
  - 解决同名商品无法区分的问题
  - 商品表格列顺序：复选框、商品名称、条形码、成本价、库存
- 🐛 **修复库存盘点多选功能**
  - 统一库存盘点和采购订单的复选框实现
  - 修复选择多个商品时之前选择被取消的问题
  - 使用 Platform.runLater() 确保表格刷新正确
- 📝 **数据库版本管理**
  - 新增 v2.4.3 数据库升级脚本（08-v2.4.3-product-name-unique.sql）
  - 更新 DATABASE_VERSIONS.md 文档
  - 提供详细的升级指南

### v2.4.2 (2026-03-07)
- 🐛 修复退货订单审批失败问题（移除 OperationLogDAO 中不存在的 ip_address 字段引用）
- 🎨 优化报表页面布局（采购报表、库存报表、利润分析）
  - 上部（1/4 高度）：紧凑的两行数据汇总卡片
  - 中部（1/3 高度）：图表展示区域（饼图、折线图、柱状图）
  - 底部（剩余空间）：使用 TabPane 显示多个表格
- 🐛 修复利润分析界面加载失败问题（移除控制器中不存在的 FXML 字段引用）
- 🗄️ 升级数据库到 MySQL 8.4 和 MySQL JDBC 驱动到 8.4.0
- 📝 更新文档说明

### v2.4.1 (2026-03-05)
- 图形化安装程序（install.bat/install.sh）
- 消息通知模块
- 数据库版本管理文档
- transaction_items 表结构优化（新增 product_id、product_code、barcode 字段）

### v2.4.0 (2026-02-29)
- 退货管理功能（退货订单、审批、统计）
- 数据导出功能（Excel、PDF）
- 性能优化（UI渲染、查询、批量操作）

### v2.3.1 (2026-02-13)
- 商品管理模块重构（商品编号自动生成、条形码重复支持）
- 打印机管理和扫描枪管理模块
- 数据导入功能（CSV/GitHub）
- 缓存管理功能
- Service 层封装

### v2.3.0 (2026-02-07)
- 完整的采购管理模块
- 库存盘点功能
- 报表统计模块（采购、库存、利润）

## 📁 项目结构

```
hello/
├── pom.xml                          # Maven 配置
├── src/main/java/com/cashier/
│   ├── CashierSystemFXApplication.java  # 主程序入口
│   ├── constant/                       # 常量定义 (2个)
│   ├── dao/                            # 数据访问层 (22个)
│   ├── controller/                     # 控制器层 (30个)
│   ├── model/                          # 实体类 (22个)
│   ├── service/                        # 服务层 (5个)
│   ├── printer/                        # 打印机管理模块 (10个类)
│   ├── scanner/                        # 扫描枪管理模块 (10个类)
│   ├── notification/                   # 消息通知模块 (5个类)
│   └── util/                           # 工具类 (13个)
├── src/main/resources/
│   ├── com/cashier/view/               # FXML 视图文件 (30个)
│   ├── css/                            # 样式文件
│   ├── fonts/                          # 字体文件 (PDF中文支持)
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

## 🛠️ 开发

### Maven 命令

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn javafx:run

# 打包项目
mvn clean package

# 运行测试
mvn test
```

### 技术栈

- **前端框架**: JavaFX 17.0.8
- **构建工具**: Maven 3.8+
- **编程语言**: Java 17
- **数据库**: MySQL 8.4
- **连接池**: HikariCP 5.1.0
- **MySQL JDBC**: mysql-connector-j 8.4.0
- **数据导出**: Apache POI 5.2.5 (Excel) + Apache PDFBox 2.0.31 (PDF)
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

## 📚 相关文档

- [AGENTS.md](AGENTS.md) - 项目开发指南
- [IMPROVEMENTS_SUMMARY.md](IMPROVEMENTS_SUMMARY.md) - 代码改进总结（v2.4.5）
- [MySQL 部署指南](docs/MYSQL_SETUP.md)
- [数据库初始化文档](docs/DATABASE_INIT.md)
- [采购表结构设计](docs/PURCHASE_TABLE_DESIGN.md)
- [Windows MySQL 安装指南](docs/WINDOWS_MYSQL_SETUP.md)

---

**注意**: 本项目仅供学习和参考使用，不建议直接用于生产环境。