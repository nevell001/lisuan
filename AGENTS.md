# AGENTS.md - 收银系统项目指南

## 项目概述

**项目名称**: 收银系统 (Cashier System)

**当前版本**: v2.4.3

**最新更新**: 2026-03-05

**项目类型**: JavaFX 桌面应用程序

**项目描述**: 一个功能完整的现代化收银系统，使用 JavaFX 17 开发，提供完整的 POS（销售点）管理、库存管理、会员管理、采购管理、退货管理、报表统计、打印机管理、扫描枪管理、数据导出、通知管理等功能。

**技术栈**:
- **前端框架**: JavaFX 17.0.8
- **编程语言**: Java 17
- **构建工具**: Maven 3.8+
- **数据库**: MySQL 8.0（唯一存储方式）
- **连接池**: HikariCP 5.1.0
- **ORM**: 自定义 DAO 层
- **日志**: SLF4J 2.0.9 + Logback 1.4.11
- **测试**: JUnit 5.10.0 + TestFX 4.0.18 + H2 Database 2.2.224
- **UI 增强**: ControlsFX 11.2.1, FontAwesomeFX 4.7.0-9.1.2
- **密码加密**: BCrypt 0.10.2
- **数据导出**: Apache POI 5.2.5 (Excel), Apache PDFBox 2.0.31 (PDF)
- **硬件支持**: USB HID 扫描枪、打印机
- **缓存管理**: 内置缓存管理器（5分钟过期）
- **数据导入**: 支持从 CSV 文件和 GitHub 导入商品数据
- **性能优化**: UI 渲染优化、查询优化、批量操作优化

**主入口**: `com.cashier.CashierSystemFXApplication`

**默认登录**: admin / admin123

---

## 构建和运行

### 环境要求

- **JDK**: Java 17 或更高版本
- **Maven**: 3.8 或更高版本
- **MySQL**: 8.0 或更高版本（必需）
- **Docker**: 可选，用于快速启动 MySQL

### 构建命令

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn javafx:run

# 打包项目
mvn clean package

# 运行打包后的 JAR
java -jar target/cashier-system-fx-2.4.3-jar-with-dependencies.jar

# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=PasswordUtilTest
mvn test -Dtest=ProductDAOTest
mvn test -Dtest=UserDAOTest

# 运行特定测试方法
mvn test -Dtest=PasswordUtilTest#testHashPassword

# 跳过测试打包
mvn clean package -DskipTests
```

### Windows 快速启动

```batch
REM 一键安装脚本（图形化安装程序）
install.bat

REM 简单命令行安装
installer-simple.bat

REM 启动应用
start.bat

REM 创建桌面快捷方式
create-shortcut.bat

REM 打包为 Windows 安装程序
package-windows.bat

REM 无控制台启动
start-silent.bat
```

### Linux/macOS 快速启动

```bash
# 智能安装脚本（自动检测 Docker 和本地 MySQL）
./install.sh

# 启动应用
./start.sh

# 设置数据库密码环境变量（安全方式）
export CASHER_DB_PASSWORD="YourPassword"
./start.sh
```

### 数据库启动

**使用 Docker Compose（推荐）**:
```bash
# 启动 MySQL 8.0
docker-compose up -d mysql

# 查看日志
docker-compose logs -f mysql

# 停止 MySQL
docker-compose down

# 重启 MySQL
docker-compose restart mysql
```

**手动初始化数据库**:
```bash
# 使用完整初始化脚本（包含所有功能）
docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < docker/mysql-init/00-init-complete.sql
```

**数据库版本升级**（适用于从旧版本升级）：
```bash
# 从 v2.4.0 升级到 v2.4.1
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql

# 从 v2.3.1 升级到 v2.4.1
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/05-v2.4.0-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql

# 从 v2.3.0 升级到 v2.4.1
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/04-v2.3.1-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/05-v2.4.0-updates.sql
docker exec cashier-mysql mysql -uroot -pRootPassword123! cashier_system < docker/mysql-init/06-v2.4.1-updates.sql
```

**诊断和修复历史数据**（可选）：
```bash
# 检查交易明细重复记录
docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < docker/mysql-init/07-fix-transaction-items.sql
```

> **注意**：v2.4.3 版本添加了商品名称唯一性约束，详见数据库升级脚本，无需执行数据库升级脚本。详细升级指南请参考 [docker/mysql-init/DATABASE_VERSIONS.md](docker/mysql-init/DATABASE_VERSIONS.md)

**使用本地 MySQL**:
```bash
# 创建数据库和用户
mysql -u root -p

CREATE DATABASE cashier_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cashier'@'%' IDENTIFIED BY 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
FLUSH PRIVILEGES;

# 初始化数据库
mysql -u root -p cashier_system < docker/mysql-init/00-init-complete.sql
```

### 配置文件

- **数据库配置**: `config/database.properties`
- **JVM 配置**: `config/jvm.config`
- **打印机配置**: `config/printer.properties`

**安全提示**: 数据库密码建议使用环境变量 `CASHER_DB_PASSWORD` 存储，避免明文存储在配置文件中。

---

## 项目结构

```
hello/
├── src/main/java/com/cashier/
│   ├── CashierSystemFXApplication.java    # 主程序入口
│   ├── constant/                           # 常量定义
│   │   ├── FXConstants.java               # JavaFX 常量
│   │   └── SpacingConstants.java          # 间距常量
│   ├── controller/                        # 控制器层 (30 个)
│   │   ├── CartController.java            # 购物车控制器
│   │   ├── CheckoutController.java        # 结账控制器
│   │   ├── InventoryController.java       # 库存管理控制器
│   │   ├── InventoryCheckController.java  # 库存盘点控制器
│   │   ├── InventoryReportController.java # 库存报表控制器
│   │   ├── MemberController.java          # 会员管理控制器
│   │   ├── MemberEditController.java      # 会员编辑控制器
│   │   ├── PromotionController.java       # 促销管理控制器
│   │   ├── TransactionController.java     # 交易记录控制器
│   │   ├── StatisticsController.java      # 数据统计控制器
│   │   ├── ShiftController.java           # 交接班控制器
│   │   ├── UserController.java            # 用户管理控制器
│   │   ├── PasswordResetController.java   # 密码重置控制器
│   │   ├── SettingsController.java        # 系统设置控制器
│   │   ├── LoginController.java           # 登录控制器
│   │   ├── MainController.java            # 主界面控制器
│   │   ├── ProductEditController.java     # 商品编辑控制器
│   │   ├── RestockController.java         # 补货控制器
│   │   ├── RechargeController.java        # 充值控制器
│   │   ├── SupplierController.java        # 供应商管理控制器
│   │   ├── PurchaseOrderController.java   # 采购订单控制器
│   │   ├── PurchaseApprovalController.java # 采购审批控制器
│   │   ├── PurchaseInboundController.java # 采购入库控制器
│   │   ├── PurchaseReportController.java  # 采购报表控制器
│   │   ├── ProfitReportController.java    # 利润分析控制器
│   │   ├── PosModeController.java         # POS模式控制器 (v2.3.1 新增)
│   │   ├── ReturnOrderController.java     # 退货订单控制器 (v2.4.0 新增)
│   │   ├── ReturnApprovalController.java  # 退货审批控制器 (v2.4.0 新增)
│   │   ├── ReturnReportController.java    # 退货报表控制器 (v2.4.0 新增)
│   │   └── CreateReturnOrderDialogController.java # 创建退货订单对话框控制器 (v2.4.0 新增)
│   ├── dao/                               # 数据访问层 (22 个)
│   │   ├── UserDAO.java                   # 用户 DAO
│   │   ├── ProductDAO.java                # 商品 DAO
│   │   ├── MemberDAO.java                 # 会员 DAO
│   │   ├── TransactionDAO.java            # 交易 DAO
│   │   ├── ShiftDAO.java                  # 交接班 DAO
│   │   ├── CategoryDAO.java               # 分类 DAO
│   │   ├── UnitDAO.java                   # 单位 DAO
│   │   ├── PromotionDAO.java              # 促销 DAO
│   │   ├── OperationLogDAO.java           # 操作日志 DAO
│   │   ├── RechargeRecordDAO.java         # 充值记录 DAO
│   │   ├── SystemSettingsDAO.java         # 系统设置 DAO
│   │   ├── ThemePreferenceDAO.java        # 主题偏好 DAO
│   │   ├── SupplierDAO.java               # 供应商 DAO
│   │   ├── PurchaseOrderDAO.java          # 采购订单 DAO
│   │   ├── PurchaseOrderItemDAO.java      # 采购订单明细 DAO
│   │   ├── PurchaseApprovalDAO.java       # 采购审批 DAO
│   │   ├── PurchaseInboundDAO.java        # 采购入库 DAO
│   │   ├── PurchaseInboundItemDAO.java    # 采购入库明细 DAO
│   │   ├── InventoryCheckDAO.java         # 库存盘点 DAO
│   │   ├── InventoryCheckItemDAO.java     # 库存盘点明细 DAO
│   │   ├── ReturnOrderDAO.java            # 退货订单 DAO (v2.4.0 新增)
│   │   └── ReturnOrderItemDAO.java        # 退货订单明细 DAO (v2.4.0 新增)
│   ├── model/                             # 实体类 (21 个)
│   │   ├── Product.java                   # 商品实体类
│   │   ├── Member.java                    # 会员实体类
│   │   ├── User.java                      # 用户实体类
│   │   ├── Transaction.java               # 交易实体类
│   │   ├── Category.java                  # 分类实体类
│   │   ├── Unit.java                      # 单位实体类
│   │   ├── Promotion.java                 # 促销实体类
│   │   ├── RechargeRecord.java            # 充值记录类
│   │   ├── OperationLog.java              # 操作日志类
│   │   ├── Shift.java                     # 交接班记录类
│   │   ├── CartItem.java                  # 购物车项类
│   │   ├── Supplier.java                  # 供应商实体类
│   │   ├── PurchaseOrder.java             # 采购订单实体类
│   │   ├── PurchaseOrderItem.java         # 采购订单明细类
│   │   ├── PurchaseApproval.java          # 采购审批记录类
│   │   ├── PurchaseInbound.java           # 采购入库记录类
│   │   ├── PurchaseInboundItem.java       # 采购入库明细类
│   │   ├── InventoryCheck.java            # 库存盘点实体类
│   │   ├── InventoryCheckItem.java        # 库存盘点明细类
│   │   ├── ReturnOrder.java               # 退货订单实体类 (v2.4.0 新增)
│   │   └── ReturnOrderItem.java           # 退货订单明细类 (v2.4.0 新增)
│   ├── printer/                           # 打印机管理模块 (10 个类，v2.3.1 新增)
│   │   ├── PrinterManager.java            # 打印机管理器
│   │   ├── PrinterDevice.java             # 打印设备
│   │   ├── PrinterStatus.java             # 打印机状态
│   │   ├── PrinterDeviceStatus.java       # 设备状态枚举
│   │   ├── PrinterDeviceType.java         # 设备类型枚举
│   │   ├── PrintTask.java                 # 打印任务
│   │   ├── PrintTaskType.java             # 任务类型枚举
│   │   ├── PrintTemplate.java             # 打印模板
│   │   ├── PrintUtil.java                 # 打印工具类
│   │   └── PrintPreviewDialog.java        # 打印预览对话框
│   ├── scanner/                           # 扫描枪管理模块 (10 个类，v2.3.1 新增)
│   │   ├── ScannerManager.java            # 扫描枪管理器
│   │   ├── ScannerDevice.java             # 扫描设备
│   │   ├── ScannerDeviceStatus.java       # 设备状态枚举
│   │   ├── ScannerDeviceType.java         # 设备类型枚举
│   │   ├── USBHIDScannerDevice.java       # USB HID 扫描设备实现
│   │   ├── ScanEvent.java                 # 扫描事件
│   │   ├── ScanListener.java              # 扫描监听器接口
│   │   ├── ScanDataType.java              # 扫描数据类型枚举
│   │   ├── FocusManager.java              # 焦点管理器
│   │   └── FocusTarget.java               # 焦点目标接口
│   ├── notification/                      # 通知管理模块 (5 个类，v2.4.1 新增)
│   │   ├── NotificationManager.java       # 通知管理器
│   │   ├── Notification.java              # 通知实体类
│   │   ├── NotificationType.java          # 通知类型枚举
│   │   ├── NotificationListener.java      # 通知监听器接口
│   │   └── NotificationIntegration.java   # 通知集成类
│   ├── installer/                         # 安装程序模块 (1 个类，v2.4.1 新增)
│   │   └── Installer.java                 # 图形化安装程序
│   ├── service/                           # 服务层 (5 个)
│   │   ├── DataService.java               # 数据服务
│   │   ├── InventoryService.java          # 库存服务 (v2.3.1 新增)
│   │   ├── MemberService.java             # 会员服务 (v2.3.1 新增)
│   │   ├── TransactionService.java        # 交易服务 (v2.3.1 新增)
│   │   └── ReturnService.java             # 退货服务 (v2.4.0 新增)
│   └── util/                              # 工具类 (13 个)
│       ├── DatabaseManager.java           # 数据库管理器
│       ├── PasswordUtil.java              # 密码工具
│       ├── FXUtils.java                   # JavaFX 工具类
│       ├── FXMLUtils.java                 # FXML 工具类
│       ├── LoggerFactoryUtil.java         # 日志工厂工具
│       ├── StatusBarManager.java          # 状态栏管理器
│       ├── ReceiptPrinter.java            # 收据打印机
│       ├── CacheManager.java              # 缓存管理器 (v2.3.1 新增)
│       ├── ProductDataImporter.java       # 商品数据导入工具 (v2.3.1 新增)
│       ├── ExportUtil.java                # 数据导出工具 (v2.4.0 新增)
│       ├── UIOptimizer.java               # UI 渲染优化工具 (v2.4.0 新增)
│       ├── QueryOptimizer.java            # 查询优化工具 (v2.4.0 新增)
│       └── BatchOperationUtil.java        # 批量操作工具 (v2.4.0 新增)
├── src/test/java/com/cashier/             # 测试代码
│   ├── util/
│   │   ├── PasswordUtilTest.java          # 密码工具测试
│   │   └── DatabaseTestBase.java          # 测试基类 (v2.4.1 新增)
│   ├── dao/
│   │   ├── ProductDAOTest.java            # 商品 DAO 测试
│   │   └── UserDAOTest.java               # 用户 DAO 测试
│   └── service/                           # 服务层测试 (v2.3.1 新增)
├── src/main/resources/
│   ├── com/cashier/view/                  # FXML 视图文件 (30 个)
│   │   ├── CartView.fxml
│   │   ├── CheckoutView.fxml
│   │   ├── InventoryCheckView.fxml
│   │   ├── InventoryReportView.fxml
│   │   ├── InventoryView.fxml
│   │   ├── LoginView.fxml
│   │   ├── MainView.fxml
│   │   ├── MemberEditView.fxml
│   │   ├── MemberView.fxml
│   │   ├── PasswordResetView.fxml
│   │   ├── PosModeView.fxml               # POS模式视图 (v2.3.1 新增)
│   │   ├── ProductEditView.fxml
│   │   ├── ProfitReportView.fxml
│   │   ├── PromotionView.fxml
│   │   ├── PurchaseApprovalView.fxml
│   │   ├── PurchaseInboundView.fxml
│   │   ├── PurchaseOrderView.fxml
│   │   ├── PurchaseReportView.fxml
│   │   ├── RechargeView.fxml
│   │   ├── RestockView.fxml
│   │   ├── SettingsView.fxml
│   │   ├── ShiftView.fxml
│   │   ├── StatisticsView.fxml
│   │   ├── SupplierView.fxml
│   │   ├── TransactionView.fxml
│   │   ├── UserView.fxml
│   │   ├── ReturnOrderView.fxml           # 退货订单视图 (v2.4.0 新增)
│   │   ├── ReturnApprovalView.fxml        # 退货审批视图 (v2.4.0 新增)
│   │   ├── ReturnReportView.fxml          # 退货报表视图 (v2.4.0 新增)
│   │   └── CreateReturnOrderDialog.fxml   # 创建退货订单对话框 (v2.4.0 新增)
│   ├── css/                               # 样式文件
│   │   ├── styles.css                     # 主样式文件
│   │   ├── light-theme.css                # 浅色主题
│   │   ├── dark-theme.css                 # 深色主题
│   │   └── intellij-theme.css             # IntelliJ主题
│   ├── sounds/                            # 音效文件 (v2.3.1 新增)
│   │   ├── scan_error.wav                 # 扫描错误音效
│   │   ├── scan_not_found.wav             # 扫描未找到音效
│   │   └── scan_success.wav               # 扫描成功音效
│   ├── images/                            # 图片资源 (v2.3.1 新增)
│   │   └── logos/                         # Logo 目录
│   ├── fonts/                             # 字体文件 (PDF 导出中文支持)
│   │   └── NotoSansSC-Regular.ttf         # 思源黑体
│   └── logback.xml                        # 日志配置
├── config/                                # 配置目录
│   ├── database.properties.example        # 数据库配置示例
│   ├── jvm.config.example                 # JVM 配置示例
│   └── printer.properties.example         # 打印机配置示例
├── docker/                                # Docker 配置
│   ├── mysql-init/                        # 数据库初始化脚本
│   │   ├── 00-grant-root-permissions.sql  # Root 权限配置
│   │   ├── 00-init-complete.sql           # 完整初始化脚本（整合所有功能，v2.4.3）
│   │   ├── 04-v2.3.1-updates.sql          # v2.3.1 独立升级脚本
│   │   ├── 05-v2.4.0-updates.sql          # v2.4.0 独立升级脚本（退货管理、数据导出）
│   │   ├── 06-v2.4.1-updates.sql          # v2.4.1 独立升级脚本（交易明细优化）
│   │   ├── 07-fix-transaction-items.sql   # 修复交易明细重复记录（诊断脚本）
│   │   └── DATABASE_VERSIONS.md           # 数据库版本管理文档
│   └── mysql-backup/                      # 数据库备份目录
├── docs/                                  # 文档目录
│   ├── DATABASE_CHANGES_v2.3.1.md         # v2.3.1 数据库变更文档
│   ├── DATABASE_INIT.md                   # 数据库初始化文档
│   ├── MYSQL_SETUP.md                     # MySQL 部署指南
│   ├── PURCHASE_TABLE_DESIGN.md           # 采购表结构设计
│   ├── ICON_GUIDE.md                      # 应用图标指南
│   ├── WINDOWS_MYSQL_SETUP.md             # Windows MySQL 安装指南
│   ├── PDF_TIME_FORMAT_OPTIMIZATION.md    # PDF 时间格式优化文档 (v2.3.2 新增)
│   └── INSTALLER.md                       # 安装程序使用指南 (v2.4.1 新增)
├── exports/                               # 导出文件目录
│   ├── 交易记录/                          # 交易记录导出
│   └── 数据统计/                          # 数据统计导出
├── data/                                  # 数据目录
├── logs/                                  # 日志目录
├── install.sh                             # Linux/macOS 智能安装脚本 (v2.4.1 优化)
├── install.bat                            # Windows 图形化安装程序 (v2.4.1 新增)
├── installer-simple.bat                   # Windows 命令行安装脚本 (v2.4.1 新增)
├── start.sh                               # Linux/macOS 启动脚本
├── start.bat                              # Windows 启动脚本
├── start-silent.bat                       # Windows 无控制台启动 (v2.4.1 新增)
├── create-shortcut.bat                    # 创建桌面快捷方式
├── package-windows.bat                    # 打包为 Windows 安装程序
├── docker-compose.yml                     # Docker Compose 配置
├── pom.xml                                # Maven 配置文件
├── AGENTS.md                              # 项目指南
├── README.md                              # 项目说明
└── CLAUDE.md                              # Claude Code 指南
```

---

## 核心功能模块

### 1. POS 收银台
- 商品扫码或搜索添加到购物车
- 快捷键系统（F1-添加、Delete-删除、Ctrl+L-清空等）
- 会员信息查询和绑定
- 多种支付方式（现金、微信、支付宝、银行卡）
- 现金找零自动计算
- 会员折扣自动应用（折扣值 0-10，10 表示不打折，0 表示免费）
- 支持扫描枪自动扫码（v2.3.1 新增）
- 扫描音效反馈（成功、失败、未找到）
- POS 模式专用界面（v2.3.1 新增）

### 2. 库存管理
- 商品添加、编辑、删除
- 库存补货
- 商品搜索和筛选
- 库存预警显示
- 分类管理
- 单位管理
- 商品编号自动生成（格式：P + 年月日 + 4位序号）
- 条形码重复支持（允许多个商品使用相同条形码）

### 3. 会员管理
- 会员注册（自动生成会员编号，格式：M000001）
- 积分查询和累计
- 等级自动升级（普通→银卡→金卡→钻石）
- 余额充值
- 生日特权
- 折扣值系统（10=不打折，9.8=9.8折，9=9折，0=免费）

### 4. 促销管理
- 满减活动
- 折扣活动
- 优惠券管理
- 促销时间范围设置
- 促销使用统计

### 5. 交易记录
- 完整交易历史
- 按日期、支付方式筛选
- 交易详情查看

### 6. 数据统计
- 销售额统计
- 交易量统计
- 平均客单价
- 分类销售占比

### 7. 交接班管理
- 班次开始/结束
- 班次收入统计
- 多支付方式收入明细
- 班次对比分析

### 8. 用户管理
- 用户增删改查
- 权限分配（管理员、收银员、财务）
- 密码重置
- 密码修改

### 9. 系统设置
- 主题切换（浅色/深色/IntelliJ）
- 税率配置
- 数据备份/恢复
- 主题偏好持久化

### 10. 采购管理 (v2.3.0)
- 供应商管理（A/B/C级分级）
- 采购订单创建和审批
- 采购订单状态管理（待审批、已审批、已拒绝、已完成）
- 采购入库管理
- 支持部分入库
- 自动更新库存

### 11. 库存盘点 (v2.3.0)
- 创建盘点单（全盘/部分盘点）
- 实际库存录入
- 自动计算差异
- 完成盘点并调整库存
- 盘点单审核

### 12. 报表统计 (v2.3.0)
- 采购报表（订单统计、金额趋势、供应商排名）
- 库存报表（周转率、滞销商品、积压分析）
- 利润分析（采购成本、销售收入、毛利率、净利润）

### 13. 退货管理 (v2.4.0 新增)
- 创建退货订单（基于原交易）
- 退货商品明细管理
- 退货审批流程（待审批、已审批、已拒绝、已完成）
- 退款方式管理（现金、微信、支付宝、银行卡）
- 退货历史记录查询
- 退货报表统计

### 14. 打印机管理 (v2.3.1 新增)
- 支持多种打印机设备（热敏打印机、针式打印机、喷墨打印机）
- 打印任务队列管理
- 打印预览功能
- 打印模板定制
- 打印历史记录
- 打印状态监控

### 15. 扫描枪管理 (v2.3.1 新增)
- 支持 USB HID 扫描枪
- 自动检测扫描设备
- 智能焦点管理（自动定位到商品搜索框）
- 扫描事件监听和处理
- 支持多种扫描数据类型（条形码、二维码）
- 扫描音效反馈

### 16. 数据导出 (v2.4.0 新增)
- 支持多种导出格式（PDF、Excel）
- Excel 导出功能（Apache POI 5.2.5）
  - 支持多 Sheet 导出
  - 支持单元格样式自定义
  - 支持数据格式化
  - 支持大数据量导出
- PDF 导出功能（Apache PDFBox 2.0.31）
  - 支持表格绘制
  - 支持自定义字体（中文字体支持）
  - 支持分页导出
  - 支持水印和页眉页脚
  - 优化时间格式显示（日期和时间分行显示，v2.3.2 优化）
- 导出类型
  - 交易记录导出
  - 库存报表导出
  - 数据统计导出
  - 交接班记录导出
  - 采购报表导出
  - 利润分析导出
  - 会员列表导出
  - 退货报表导出
- 导出历史记录
  - 自动记录每次导出操作
  - 记录导出文件信息（文件名、路径、大小）
  - 记录导出参数和结果
  - 支持导出记录查询和统计
- 导出参数配置
  - 支持自定义导出模板
  - 支持日期范围筛选
  - 支持列配置和排序

### 17. 数据导入 (v2.3.1 新增)
- 支持 CSV 文件导入商品数据
- 支持 GitHub 商品条码库导入
- 自动分类和单位标准化
- 自动创建缺失的分类和单位
- 导入统计和进度显示

### 18. 缓存管理 (v2.3.1 新增)
- 商品数据缓存（5分钟过期）
- 多维度缓存（ID、名称、条形码）
- 自动缓存刷新
- 缓存预热功能
- 减少数据库查询次数

### 19. 通知管理 (v2.4.1 新增)
- 系统通知管理
- 支持多种通知类型（信息、警告、错误、成功）
- 通知监听器接口
- 通知集成功能
- 实时通知推送

### 20. 图形化安装程序 (v2.4.1 新增)
- 基于 JavaFX 的图形化安装向导
- 自动环境检测（Java、Maven、Docker）
- 智能数据库配置（Docker MySQL / 本地 MySQL）
- 端口冲突检测和解决
- 自动创建配置文件
- 桌面快捷方式生成
- 多平台支持（Windows、Linux、macOS）

---

## 权限系统

系统支持三种角色：

- **管理员 (admin)**: 完整的系统管理权限
- **收银员 (cashier)**: 日常收银操作权限
- **财务 (finance)**: 财务报表和数据统计权限

---

## 快捷键系统

### 主界面快捷键

| 功能键 | 功能 |
|--------|------|
| F1 | 添加商品 |
| F2 | 补货 |
| F3 | 删除商品 |
| F4 | 搜索 |
| F5 | 刷新当前面板 |
| F6 | 分类管理 |
| F7 | 会员管理 |
| F8 | 结账 |
| F9 | 促销管理 |
| F10 | 库存预警 |
| F11 | 数据备份 |
| F12 | 数据恢复 |
| ESC | 清空搜索或关闭标签页 |

### POS/结账页面快捷键

- **F1** - 添加选中商品到购物车
- **Delete** - 移除购物车中选中的商品
- **Ctrl+L** - 清空购物车
- **双击商品** - 快速添加到购物车
- **Ctrl+F** - 聚焦到搜索框
- **Ctrl+M** - 聚焦到会员手机号框
- **F8** - 现金支付
- **Ctrl+1** - 微信支付
- **Ctrl+2** - 支付宝支付
- **Ctrl+3** - 银行卡支付
- **Ctrl+/** - 显示快捷键帮助

### 扫描枪快捷操作 (v2.3.1 新增)

- **扫描商品条形码** - 自动添加到购物车
- **扫描会员二维码** - 自动绑定会员
- **扫描商品码** - 自动聚焦到搜索框并查询商品

---

## 数据库架构

### 核心表

- `users` - 用户账户
- `products` - 商品库存信息（barcode 字段移除 UNIQUE 约束，允许重复）
- `members` - 会员账户和积分（新增 member_code 字段，格式：M000001）
- `transactions` - 交易记录主表
- `transaction_items` - 交易明细（包含 product_id、product_code、barcode 字段，v2.4.1 优化）
- `shifts` - 交接班记录

### 辅助表

- `categories` - 商品分类
- `units` - 计量单位
- `promotions` - 促销活动
- `operation_logs` - 操作日志
- `recharge_records` - 充值记录
- `system_settings` - 系统设置
- `theme_preferences` - 主题偏好

### 采购管理表 (v2.3.0)

- `suppliers` - 供应商信息
- `purchase_orders` - 采购订单
- `purchase_order_items` - 采购订单明细
- `purchase_approvals` - 采购审批记录
- `purchase_inbound` - 采购入库记录
- `purchase_inbound_items` - 采购入库明细

### 库存管理表 (v2.3.0)

- `inventory_check` - 库存盘点
- `inventory_check_items` - 库存盘点明细

### 退货管理表 (v2.4.0 新增)

- `return_orders` - 退货订单
- `return_order_items` - 退货订单明细

### 数据导出表 (v2.4.0 新增)

- `export_history` - 导出历史记录
- `export_templates` - 导出模板

### 数据库变更 (v2.3.1)

- **products 表**: 移除 barcode 字段的 UNIQUE 约束，改为普通索引，允许多个商品使用相同条形码
- **members 表**: 新增 member_code 字段（VARCHAR(50) UNIQUE），自动生成会员编号（格式：M000001）

### 数据库变更 (v2.4.0)

- **新增 return_orders 表**: 退货订单主表
- **新增 return_order_items 表**: 退货订单明细表
- **operation_logs 表**: 扩展字段（log_level、log_category、operation_result、affected_records、request_data、response_data）
- **新增 export_history 表**: 导出历史记录
- **新增 export_templates 表**: 导出模板配置
- **添加默认导出模板**: 交易记录、库存报表、会员列表

### 数据库变更 (v2.4.1)

- **transaction_items 表**: 新增 product_id 字段（商品ID）
- **transaction_items 表**: 新增 product_code 字段（商品编号）
- **transaction_items 表**: 新增 barcode 字段（条形码）
- **添加索引**: idx_product_id 优化查询性能
- **新增数据库版本管理文档**: DATABASE_VERSIONS.md

### 数据库变更 (v2.4.2)

- **无数据库结构变更**
- 修复 SQL 脚本导入时的中文编码问题
- 优化字符集配置和声明
- 确保中文字符正确导入和显示

详细变更说明请参考：
- [docs/DATABASE_CHANGES_v2.3.1.md](docs/DATABASE_CHANGES_v2.3.1.md) - v2.3.1 数据库变更
- [docs/PDF_TIME_FORMAT_OPTIMIZATION.md](docs/PDF_TIME_FORMAT_OPTIMIZATION.md) - PDF 导出优化
- [docker/mysql-init/DATABASE_VERSIONS.md](docker/mysql-init/DATABASE_VERSIONS.md) - 完整版本管理

---

## 测试

### 测试框架

- **JUnit 5.10.0** - 单元测试框架
- **TestFX 4.0.18** - JavaFX UI 测试框架
- **H2 Database 2.2.224** - 测试用内存数据库

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=PasswordUtilTest
mvn test -Dtest=ProductDAOTest
mvn test -Dtest=UserDAOTest

# 运行特定测试方法
mvn test -Dtest=PasswordUtilTest#testHashPassword
```

### 现有测试

- **PasswordUtilTest** - 密码加密工具测试（10个测试用例）
- **ProductDAOTest** - 商品数据访问测试（10个测试用例）
- **UserDAOTest** - 用户数据访问测试（10个测试用例）
- **DatabaseTestBase** - 测试基类（v2.4.1 新增）

### 测试约定

1. 测试类命名: `{ClassName}Test.java`
2. 测试方法命名: `test{MethodName}` 或使用 `@DisplayName` 注解
3. 使用 `@Test`、`@BeforeAll`、`@BeforeEach` 等注解组织测试
4. 测试文件位置: `src/test/java/com/cashier/`
5. 使用 H2 内存数据库进行单元测试，避免影响生产数据库
6. 继承 `DatabaseTestBase` 基类以复用测试配置（v2.4.1 新增）

---

## 开发约定

### 代码风格

- **包命名**: 全小写，点分隔（如 `com.cashier.dao`）
- **类命名**: 大驼峰（如 `ProductDAO`）
- **方法命名**: 小驼峰（如 `findAll()`）
- **常量命名**: 全大写下划线分隔（如 `FXConstants.DEFAULT_THEME`）

### DAO 层规范

所有 DAO 类应包含以下方法：

- `insert()` - 插入新记录
- `update()` - 更新记录
- `delete()` - 删除记录
- `findById()` - 根据 ID 查找
- `findAll()` - 查询所有记录
- 其他特定查询方法（如 `findByName()`, `findByDateRange()` 等）

使用 PreparedStatement 防止 SQL 注入，所有参数化查询。

### Service 层规范 (v2.3.1 新增)

Service 层封装业务逻辑，提供以下功能：

- **InventoryService** - 库存相关业务逻辑
  - 批量更新库存（优化版，使用批量 SQL）
  - 库存搜索和筛选
  - 低库存商品查询
  - 库存统计信息
- **MemberService** - 会员相关业务逻辑
  - 会员充值（事务管理）
  - 会员等级自动升级
  - 会员余额检查
  - 会员折扣计算
- **TransactionService** - 交易相关业务逻辑
  - 交易执行（完整事务管理）
  - 库存扣减（乐观锁）
  - 会员余额和积分更新
  - 交易统计信息
- **ReturnService** - 退货相关业务逻辑 (v2.4.0 新增)
  - 退货订单创建（关联原交易）
  - 退货审批流程管理
  - 库存恢复（乐观锁）
  - 退款处理（余额/积分/现金）
  - 退货统计信息
  - 退货记录查询和筛选

### 控制器层规范

- 所有控制器应使用 `@FXML` 注解注入 UI 组件
- 初始化逻辑放在 `initialize()` 方法中
- 事件处理方法使用 `@FXML` 注解
- 使用 `showError()` 和 `showAlert()` 方法显示提示信息

### 测试规范

- 使用 `@DisplayName` 注解提供清晰的测试描述
- 使用 `@Order` 注解控制测试执行顺序（如需要）
- 测试应独立运行，不依赖执行顺序
- 每个测试方法应测试单一功能点
- 使用 `@BeforeAll` 和 `@AfterAll` 进行测试环境的设置和清理
- 继承 `DatabaseTestBase` 基类以复用测试配置（v2.4.1 新增）

### 日志规范

使用 SLF4J + Logback 进行日志记录：

```java
private static final Logger logger = LoggerFactoryUtil.getLogger(YourClass.class);

logger.info("信息日志");
logger.warn("警告日志");
logger.error("错误日志", exception);
```

**重要**: v2.3.1 统一使用 `LoggerFactoryUtil.getLogger()` 创建日志记录器。

### 主题系统

系统支持三种主题：
- `light-theme.css` - 浅色主题（默认）
- `dark-theme.css` - 深色主题
- `intellij-theme.css` - IntelliJ主题

在控制器中使用：
```java
getApp().applyTheme(getScene(), themeName);
```

### 数据库安全

1. 使用 PreparedStatement 防止 SQL 注入
2. 密码使用 BCrypt 加密存储
3. 数据库密码建议使用环境变量 `CASHER_DB_PASSWORD`
4. 生产环境避免使用 root 用户
5. **重要**: v2.3.1 移除文件存储备用模式，所有数据必须存储在 MySQL 数据库中

### 缓存管理规范 (v2.3.1 新增)

- 使用 `CacheManager` 管理商品数据缓存
- 缓存过期时间：5分钟
- 多维度缓存：商品ID、商品名称、商品条形码
- 批量更新库存后自动清除缓存
- 应用启动时可预热缓存

### 数据导入规范 (v2.3.1 新增)

- 使用 `ProductDataImporter` 导入商品数据
- 支持 CSV 文件格式（逗号分隔或 | 分隔）
- 支持 GitHub 商品条码库导入
- 自动标准化单位和分类
- 自动创建缺失的分类和单位
- 导入时跳过已存在的商品

### 硬件设备管理规范 (v2.3.1 新增)

#### 打印机管理

- 使用 `PrinterManager` 单例管理所有打印设备
- 打印任务通过 `PrintTask` 对象封装
- 支持打印预览功能，使用 `PrintPreviewDialog`
- 打印模板使用 `PrintTemplate` 类定义

#### 扫描枪管理

- 使用 `ScannerManager` 单例管理所有扫描设备
- 扫描事件通过 `ScanListener` 接口监听
- 自动焦点管理，使用 `FocusManager` 定位扫描输入目标
- 支持 USB HID 扫描枪自动检测
- 扫描音效反馈（成功、失败、未找到）

### 数据导出规范 (v2.4.0 新增)

- 使用 `ExportUtil` 工具类进行数据导出
- 支持 Excel 格式（使用 Apache POI 5.2.5）
  - 支持多 Sheet 导出
  - 支持单元格样式自定义
  - 支持数据格式化
  - 支持大数据量导出
- 支持 PDF 格式（使用 Apache PDFBox 2.0.31）
  - 支持表格绘制
  - 支持自定义字体（中文字体支持）
  - 支持分页导出
  - 支持水印和页眉页脚
  - 优化时间格式显示（日期和时间分行显示，v2.3.2 优化）
- 导出类型
  - 交易记录导出
  - 库存报表导出
  - 数据统计导出
  - 交接班记录导出
  - 采购报表导出
  - 利润分析导出
  - 会员列表导出
  - 退货报表导出
- 导出历史记录
  - 自动记录每次导出操作
  - 记录导出文件信息（文件名、路径、大小）
  - 记录导出参数和结果
  - 支持导出记录查询和统计

### 性能优化规范 (v2.4.0 新增)

#### UI 渲染优化

- 使用 `UIOptimizer` 进行 UI 渲染优化
  - 虚拟化：只渲染可见区域的元素
  - 异步加载：使用后台线程加载数据
  - 缓存优化：缓存常用数据减少重复计算
  - 延迟加载：按需加载数据和组件

#### 查询优化

- 使用 `QueryOptimizer` 进行数据库查询优化
  - 批量查询：将大量 ID 分成小批次查询
  - 索引优化：为常用查询字段添加索引
  - 查询缓存：缓存常用查询结果
  - 查询计划分析：使用 EXPLAIN 分析查询性能

#### 批量操作优化

- 使用 `BatchOperationUtil` 进行批量操作优化
  - 批量插入：使用 JDBC 批处理功能
  - 批量更新：减少数据库往返次数
  - 事务管理：使用事务确保数据一致性
  - 错误处理：提供详细的错误信息和重试机制

### 通知管理规范 (v2.4.1 新增)

- 使用 `NotificationManager` 管理系统通知
- 支持多种通知类型（INFO、WARNING、ERROR、SUCCESS）
- 实现 `NotificationListener` 接口监听通知事件
- 使用 `NotificationIntegration` 集成通知功能

---

## 常见任务

### 添加新功能模块

1. 创建 Model 类（在 `model/` 目录）
2. 创建 DAO 类（在 `dao/` 目录）
3. 创建 Service 类（在 `service/` 目录，如需要）
4. 创建 Controller 类（在 `controller/` 目录）
5. 创建 FXML 视图文件（在 `view/` 目录）
6. 在 `MainController` 中添加菜单项
7. 在 `DatabaseManager` 中添加建表 SQL（如需要）

### 数据库表变更

1. 在 `DatabaseManager.initializeDatabase()` 中添加建表 SQL
2. 或在 `docker/mysql-init/` 中添加迁移脚本
3. 创建对应的 DAO 类
4. 更新 Model 类
5. 添加对应的测试用例
6. 更新数据库变更文档（`docs/DATABASE_CHANGES_vX.Y.Z.md`）
7. 更新 `docker/mysql-init/DATABASE_VERSIONS.md`（v2.4.1 新增）

### 添加新的快捷键

1. 在 `MainView.fxml` 或相关 FXML 文件中添加快捷键提示
2. 在对应的 Controller 中添加事件处理方法
3. 更新 AGENTS.md 中的快捷键说明

### 添加单元测试

1. 在 `src/test/java/com/cashier/` 下创建测试类
2. 继承 `DatabaseTestBase` 基类（v2.4.1 新增）
3. 使用 JUnit 5 注解
4. 使用 H2 内存数据库进行测试
5. 编写测试方法并使用断言验证结果
6. 运行测试确保通过

### 导入商品数据 (v2.3.1 新增)

**从 CSV 文件导入**:
```java
ProductDataImporter importer = new ProductDataImporter();
Map<String, Object> result = importer.importFromCSV("/path/to/data.csv");
```

**从 GitHub 导入**:
```java
ProductDataImporter importer = new ProductDataImporter();
Map<String, Object> result = importer.importFromGitHub();
```

### 使用缓存管理器 (v2.3.1 新增)

```java
// 获取商品缓存
Product product = CacheManager.getProductFromCache(productId);

// 添加商品到缓存
CacheManager.addToCache(product);

// 批量添加商品到缓存
CacheManager.batchAddToCache(products);

// 清除缓存
CacheManager.clearCache();

// 检查缓存是否有效
boolean valid = CacheManager.isCacheValid();
```

### 集成扫描枪功能 (v2.3.1 新增)

1. 在控制器中获取 `ScannerManager` 实例
2. 实现 `ScanListener` 接口
3. 注册扫描监听器：`ScannerManager.getInstance().addListener(listener)`
4. 在 `onScan()` 方法中处理扫描数据
5. 播放扫描音效（可选）

### 集成打印机功能 (v2.3.1 新增)

1. 在控制器中获取 `PrinterManager` 实例
2. 创建 `PrintTask` 对象
3. 使用 `PrintTemplate` 定义打印内容
4. 提交打印任务：`PrinterManager.getInstance().submitTask(task)`
5. 可选：使用 `PrintPreviewDialog` 预览打印内容

### 使用数据导出功能 (v2.4.0 新增)

**导出为 Excel**:
```java
ExportUtil.exportToExcel(title, headers, data, subDir);
```

**导出为 PDF**:
```java
ExportUtil.exportToPDF(title, headers, data, subDir);
```

**自定义导出参数**:
```java
Map<String, Object> params = new HashMap<>();
params.put("startDate", startDate);
params.put("endDate", endDate);
params.put("exportType", "TRANSACTION");
params.put("exportFormat", "EXCEL");

ExportUtil.exportToExcel(title, headers, data, subDir, params);
```

### 使用退货管理功能 (v2.4.0 新增)

**创建退货订单**:
```java
ReturnService returnService = new ReturnService();
ReturnOrder returnOrder = returnService.createReturnOrder(
    originalTransactionId,
    member,
    returnItems,
    returnReason,
    operatorName
);
```

**审批退货订单**:
```java
ReturnOrder approvedOrder = returnService.approveReturnOrder(
    returnOrderId,
    approverName,
    approvalComment,
    refundMethod
);
```

**查询退货记录**:
```java
List<ReturnOrder> returnOrders = returnService.getReturnOrdersByDateRange(
    startDate, endDate
);
```

### 使用性能优化工具 (v2.4.0 新增)

**UI 渲染优化**:
```java
// 异步加载数据
UIOptimizer.asyncLoad(data -> {
    // 更新 UI
    tableView.setItems(FXCollections.observableArrayList(data));
});

// 虚拟化列表
UIOptimizer.virtualize(tableView, items);
```

**查询优化**:
```java
// 批量查询
List<Product> products = QueryOptimizer.batchQuery(
    productIds,
    batchIds -> ProductDAO.getInstance().findByIds(batchIds),
    1000
);
```

**批量操作**:
```java
// 批量插入
Connection conn = DatabaseManager.getConnection();
List<Object[]> params = prepareParams();
int[] results = BatchOperationUtil.batchInsert(conn, sql, params);
```

### 使用通知管理功能 (v2.4.1 新增)

**发送通知**:
```java
NotificationManager.getInstance().sendNotification(
    "操作成功",
    "商品已成功添加到库存",
    NotificationType.SUCCESS
);
```

**监听通知**:
```java
NotificationManager.getInstance().addListener(new NotificationListener() {
    @Override
    public void onNotification(Notification notification) {
        System.out.println("收到通知: " + notification.getMessage());
    }
});
```

---

## 常见问题

### 应用无法启动

1. 检查 JDK 版本（需要 Java 17+）
2. 检查 MySQL 服务是否运行（v2.3.1 移除文件存储，MySQL 是必需的）
3. 检查 `config/database.properties` 配置
4. 查看日志文件 `logs/`

### 数据库连接失败

1. 确保 MySQL 8.0 正在运行
2. 检查防火墙设置
3. 验证数据库用户名和密码
4. 确认数据库 URL 格式正确
5. 尝试使用环境变量 `CASHER_DB_PASSWORD` 存储密码

### 商品条形码重复问题 (v2.3.1)

**问题**: 添加商品时提示条形码已存在？

**解决方案**: v2.3.1 已移除 barcode 字段的 UNIQUE 约束，允许多个商品使用相同条形码。如果仍然遇到问题，请运行升级脚本：

```bash
mysql -u root -p cashier_system < docker/mysql-init/04-v2.3.1-updates.sql
```

### 会员编号为空 (v2.3.1)

**问题**: 会员表的 member_code 字段为空？

**解决方案**: 运行以下 SQL 为现有会员生成编号：

```sql
UPDATE members 
SET member_code = CONCAT('M', LPAD(id, 6, '0'))
WHERE member_code IS NULL OR member_code = '';
```

或者重启应用，系统会自动检测并生成会员编号。

### 扫描枪无法工作 (v2.3.1)

**问题**: 扫描枪扫描后没有反应？

**解决方案**:
1. 确认扫描枪已正确连接（USB 接口）
2. 确认扫描枪处于 HID 模式
3. 检查 `ScannerManager` 是否已启动
4. 确认焦点管理器已正确配置
5. 查看日志获取详细错误信息

### 打印机无法工作 (v2.3.1)

**问题**: 打印任务提交后没有反应？

**解决方案**:
1. 确认打印机已正确连接并开机
2. 确认打印机驱动已安装
3. 检查 `config/printer.properties` 配置
4. 使用 `PrintPreviewDialog` 预览打印内容
5. 查看打印历史记录确认任务状态

### 编译错误

```bash
# 清理并重新编译
mvn clean compile

# 如果依赖下载失败
mvn dependency:resolve

# 查看详细错误信息
mvn compile -X
```

### 测试失败

```bash
# 运行单个测试类查看详细错误
mvn test -Dtest=ClassName

# 查看测试报告
cat target/surefire-reports/*.txt
```

### 缓存问题 (v2.3.1)

**问题**: 商品数据未及时更新？

**解决方案**:
1. 检查缓存是否过期：`CacheManager.isCacheValid()`
2. 清除缓存：`CacheManager.clearCache()`
3. 批量更新后自动清除缓存
4. 预热缓存：`CacheManager.warmupCache()`

### PDF 导出时间显示问题 (v2.3.2)

**问题**: PDF 导出的时间格式显示不正常？

**解决方案**: v2.3.2 已优化 PDF 导出时间格式显示，日期时间自动分成两行。如果仍有问题：
1. 检查 `ExportUtil.java` 中的时间格式检测逻辑
2. 确认 PDF 字体文件 `NotoSansSC-Regular.ttf` 存在
3. 查看导出日志获取详细错误信息

### Docker 端口冲突 (v2.4.1)

**问题**: Docker MySQL 无法启动，提示端口被占用？

**解决方案**: 智能安装脚本会自动检测端口冲突并提供解决方案：
1. 使用 Docker MySQL（推荐）
2. 停止 Docker MySQL 并使用本地 MySQL
3. 使用不同的端口

手动解决：
```bash
# 检查端口占用
lsof -i :3306

# 停止占用端口的进程
kill -9 <PID>

# 或修改 docker-compose.yml 使用其他端口
```

### 中文字符显示乱码 (v2.4.2)

**问题**: 数据库中的中文字符显示为乱码？

**解决方案**: v2.4.2 已修复中文字符编码问题。如果仍然遇到问题：
1. 确保数据库使用 utf8mb4 字符集
2. 确保连接 URL 包含 `characterEncoding=utf8mb4`
3. 确保使用最新的初始化脚本（00-init-complete.sql v2.4.3）
4. 查看日志获取详细错误信息

### SQL 脚本导入失败 (v2.4.2)

**问题**: 执行 SQL 初始化脚本时出现错误？

**解决方案**: v2.4.2 已修复 SQL 脚本导入问题。如果仍然遇到问题：
1. 确保使用 `--default-character-set=utf8mb4` 参数
2. 确保使用最新的初始化脚本（00-init-complete.sql v2.4.3）
3. 检查 MySQL 用户权限
4. 查看详细错误信息

---

## 相关文档

- [README.md](README.md) - 项目说明
- [CLAUDE.md](CLAUDE.md) - Claude Code 指南
- [INSTALLER.md](INSTALLER.md) - 安装程序使用指南 (v2.4.1 新增)
- [docs/DATABASE_CHANGES_v2.3.1.md](docs/DATABASE_CHANGES_v2.3.1.md) - v2.3.1 数据库变更文档
- [docs/DATABASE_INIT.md](docs/DATABASE_INIT.md) - 数据库初始化文档
- [docs/MYSQL_SETUP.md](docs/MYSQL_SETUP.md) - MySQL 部署指南
- [docs/WINDOWS_MYSQL_SETUP.md](docs/WINDOWS_MYSQL_SETUP.md) - Windows MySQL 安装指南
- [docs/PURCHASE_TABLE_DESIGN.md](docs/PURCHASE_TABLE_DESIGN.md) - 采购表结构设计
- [docs/ICON_GUIDE.md](docs/ICON_GUIDE.md) - 应用图标指南
- [docs/PDF_TIME_FORMAT_OPTIMIZATION.md](docs/PDF_TIME_FORMAT_OPTIMIZATION.md) - PDF 时间格式优化文档
- [docker/mysql-init/DATABASE_VERSIONS.md](docker/mysql-init/DATABASE_VERSIONS.md) - 数据库版本管理文档 (v2.4.1 新增)
- [docker/README.md](docker/README.md) - Docker 配置说明

---

## 版本历史

### v2.4.2 (2026-03-05)
- 🐛 修复 SQL 脚本导入时的中文编码问题
  - 修复 docker-compose.yml 中的字符集配置
  - 修复 00-init-complete.sql 的字符集声明
  - 确保中文字符正确导入和显示
- 🐛 修复中文字符显示乱码问题
  - 修复数据库连接字符集配置
  - 修复日志输出字符编码
  - 修复 UI 界面中文字符显示
- 🐛 修复数据库连接和安装脚本问题
  - 优化 install.sh 脚本的数据库连接检测
  - 优化 install.bat 脚本的错误处理
  - 改进数据库初始化流程
- 🐛 修复创建退货订单失败的问题
  - 修复退货订单创建时的空指针异常
  - 优化退货订单验证逻辑
  - 改进错误提示信息
- 🐛 修复 ProfitReportView.fxml 中的重复元素
  - 修复 FXML 加载失败的问题
  - 移除重复的 UI 元素定义
  - 优化界面结构
- 🐛 为利润分析、退货报表、促销管理按钮添加 @FXML 注解
  - 修复按钮点击事件无法触发的问题
  - 确保所有按钮正确注入
  - 改进事件处理机制
- 🐛 为 ShiftView.fxml 添加图表导入语句
  - 修复交接班界面图表无法显示的问题
  - 添加必要的 FXML 导入
  - 优化图表显示效果
- 🎨 优化数据统计页面上部分布局
  - 改进统计数据的展示方式
  - 优化卡片布局和间距
  - 提升用户体验
- 📝 更新文档
  - 更新 README.md 补充新功能说明
  - 更新 AGENTS.md 反映最新项目状态
  - 优化安装和使用文档

### v2.4.1 (2026-03-01)
- 🐛 修复交易明细表结构
  - 为 transaction_items 表新增 product_id 字段（商品ID）
  - 为 transaction_items 表新增 product_code 字段（商品编号）
  - 为 transaction_items 表新增 barcode 字段（条形码）
  - 添加索引 idx_product_id 优化查询性能
- 🗄️ 数据库版本管理
  - 新增 DATABASE_VERSIONS.md 数据库版本管理文档
  - 提供完整的升级和回滚脚本
  - 添加数据库版本检查功能
  - 整合历史脚本到 00-init-complete.sql
- 📦 安装程序增强
  - 新增图形化安装程序（Installer.java）
  - 智能环境检测（Java、Maven、Docker）
  - 端口冲突检测和自动解决
  - 自动创建配置文件和桌面快捷方式
  - 多平台支持（Windows、Linux、macOS）
- 🔔 通知管理功能
  - 新增通知管理模块（5 个类）
  - 支持多种通知类型（INFO、WARNING、ERROR、SUCCESS）
  - 通知监听器接口
  - 实时通知推送
- 🧪 测试改进
  - 新增 DatabaseTestBase 测试基类
  - 优化测试配置复用
- 📝 文档更新
  - 更新 AGENTS.md 反映项目最新状态
  - 新增 INSTALLER.md 安装程序指南
  - 添加性能优化规范说明
  - 添加数据导出规范说明
  - 添加通知管理规范说明
- 🔧 代码优化
  - 优化退货订单创建功能
  - 优化商品编辑界面
  - 优化数据库初始化流程
  - 优化安装脚本（install.sh、install.bat）

### v2.4.0 (2026-02-29)
- ✨ 新增退货管理功能
  - 创建退货订单（基于原交易）
  - 退货审批流程
  - 退款方式管理
  - 退货历史记录和报表
- ✨ 数据导出功能增强
  - 支持多种导出格式（PDF、Excel、CSV）
  - 导出历史记录管理
  - 自定义导出模板
  - 默认模板配置
- 🔧 操作日志增强
  - 新增日志级别（DEBUG、INFO、WARN、ERROR）
  - 新增日志分类（USER、SYSTEM、EXCEPTION）
  - 新增操作结果字段
  - 新增请求/响应数据记录
- 🗄️ 新增退货管理相关数据表（return_orders、return_order_items）
- 🗄️ 新增数据导出相关数据表（export_history、export_templates）
- 🐛 修复交易记录中商品重复显示的问题
- 📝 添加数据库版本管理文档
- 📝 添加交易明细重复记录检查脚本
- 🔄 优化数据库初始化和升级流程
- 🎨 新增 4 个退货管理界面
- 🎨 新增 4 个退货管理控制器
- 🎨 新增 2 个退货管理 DAO
- 🎨 新增 2 个退货管理模型

### v2.3.2 (2026-03-01)
- ✨ 优化 PDF 导出时间格式显示
  - 日期时间自动分成两行（日期在上，时间在下）
  - 修复时间列宽度计算和文本绘制顺序
  - 优化表格边框和列宽分配算法
- 📊 修复交接班导出问题
  - 修复 NULL 时间处理，避免显示异常
  - 修复班次编号显示不全问题
  - 确保所有内容都在单元格内显示
- 📝 添加 PDF 导出时间格式优化文档
- 🔄 统一所有文档版本号到 v2.3.2

### v2.3.1 (2026-02-13)
- ✨ 新增打印机管理模块（10 个类）
- ✨ 新增扫描枪管理模块（11 个类）
- ✨ 新增 POS 模式控制器和视图
- ✨ 新增 3 个 Service 类（InventoryService、MemberService、TransactionService）
- ✨ 新增缓存管理器（CacheManager）
- ✨ 新增商品数据导入工具（ProductDataImporter）
- ✨ 支持多种打印设备和打印模板
- ✨ 支持 USB HID 扫描枪自动检测
- ✨ 支持扫描音效反馈
- ✨ 添加 H2 Database 用于单元测试
- 🗄️ 移除文件存储备用模式，只使用 MySQL 数据库
- 🗄️ 移除 products.barcode 的 UNIQUE 约束，允许重复条形码
- 🗄️ 为 members 表新增 member_code 字段（会员编号）
- 🗄️ 新增 v2.3.1 数据库升级脚本
- 📝 添加数据库变更文档 `docs/DATABASE_CHANGES_v2.3.1.md`
- 🐛 优化核心功能和性能
- 🎨 优化商品管理界面和交互体验

### v2.3.0 (2026-02-07)
- ✨ 完善采购管理模块（供应商、订单、审批、入库）
- ✨ 完善库存盘点模块
- ✨ 完善报表统计模块（采购、库存、利润）
- ✨ 添加单元测试支持
- 🐛 修复多项UI和数据库问题

### v2.2.1 (2026-02-04)
- 🎨 UI 优化和数据库初始化完善
- 🗄️ 添加数据库初始化脚本
- 🔤 修复中文字符编码问题

### v2.2.0 (2026-02-03)
- ✨ 完整迁移到 MySQL 8.0 数据库
- 📊 Docker Compose 一键部署支持
- 🔄 自动数据迁移工具

### v2.1.0 (2026-01-08)
- ✨ 实现完整的快捷键系统
- 🎨 UI 优化和菜单结构调整
- 🐛 修复退出登录功能

---

## 许可证

木兰宽松许可证 v2 (MulanPSL2)

---

## 未来计划

- [ ] 云端数据同步
- [ ] 移动端应用
- [ ] 二维码生成和打印
- [ ] 更多统计图表
- [ ] 多语言支持
- [ ] 短信通知功能
- [x] 退货管理功能（v2.4.0 已实现）
- [x] 数据导出功能（Excel/PDF）（v2.4.0 已实现）
- [ ] 多仓库管理
- [x] 单元测试覆盖（v2.3.1 已实现）
- [ ] 集成测试
- [x] 打印机管理功能（v2.3.1 已实现）
- [x] 扫描枪管理功能（v2.3.1 已实现）
- [x] 缓存管理功能（v2.3.1 已实现）
- [x] 数据导入功能（v2.3.1 已实现）
- [x] Service 层封装（v2.3.1 已实现）
- [x] 性能优化（v2.4.0 已实现）
- [x] 通知管理功能（v2.4.1 已实现）
- [x] 图形化安装程序（v2.4.1 已实现）
- [ ] 支持更多打印机型号
- [ ] 支持蓝牙扫描枪
- [ ] 支持云打印服务
- [ ] 支持多语言界面
- [ ] 退货单据打印
- [ ] 退货数据统计图表
- [ ] 会员等级自定义配置
- [ ] 商品规格管理（颜色、尺寸等）
- [ ] 批发价格管理
- [ ] 供应商评价系统
- [ ] 库存预警通知
- [ ] 销售预测分析
- [ ] 数据可视化仪表板
- [ ] 自动化报表生成
- [ ] 财务会计模块
- [ ] 税务管理
- [ ] 客户关系管理（CRM）