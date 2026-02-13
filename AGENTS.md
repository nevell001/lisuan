# AGENTS.md - 收银系统项目指南

## 项目概述

**项目名称**: 收银系统 (Cashier System)

**当前版本**: v2.3.1

**项目类型**: JavaFX 桌面应用程序

**项目描述**: 一个功能完整的现代化收银系统，使用 JavaFX 17 开发，提供完整的 POS（销售点）管理、库存管理、会员管理、采购管理、报表统计等功能。

**技术栈**:
- **前端框架**: JavaFX 17.0.8
- **编程语言**: Java 17
- **构建工具**: Maven 3.8+
- **数据库**: MySQL 8.0
- **连接池**: HikariCP 5.1.0
- **ORM**: 自定义 DAO 层
- **日志**: SLF4J + Logback
- **测试**: JUnit 5.10.0 + TestFX 4.0.18
- **UI 增强**: ControlsFX 11.2.1, FontAwesomeFX 4.7.0-9.1.2
- **密码加密**: BCrypt 0.10.2

**主入口**: `com.cashier.CashierSystemFXApplication`

**默认登录**: admin / admin123

---

## 构建和运行

### 环境要求

- **JDK**: Java 17 或更高版本
- **Maven**: 3.8 或更高版本
- **MySQL**: 8.0 或更高版本
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
java -jar target/cashier-system-fx-2.3.0.jar

# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=PasswordUtilTest
mvn test -Dtest=ProductDAOTest
mvn test -Dtest=UserDAOTest

# 跳过测试打包
mvn clean package -DskipTests
```

### Windows 快速启动

```bash
# 一键安装脚本
install.bat

# 启动应用
start.bat

# 创建桌面快捷方式
create-shortcut.bat

# 打包为 Windows 安装程序
package-windows.bat
```

### 数据库启动

**使用 Docker Compose（推荐）**:
```bash
docker-compose up -d mysql

# 查看日志
docker-compose logs -f mysql

# 访问 phpMyAdmin
# URL: http://localhost:8080
# 用户名: root
# 密码: RootPassword123!
```

**手动初始化数据库**:
```bash
# 使用初始化脚本
docker exec cashier-mysql mysql -uroot -pRootPassword123! --default-character-set=utf8mb4 cashier_system < docker/mysql-init/00-init-complete.sql
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
│   ├── constant/
│   │   ├── FXConstants.java               # JavaFX 常量
│   │   └── SpacingConstants.java          # 间距常量
│   ├── controller/                        # 控制器层 (25 个)
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
│   │   └── ProfitReportController.java    # 利润分析控制器
│   ├── dao/                               # 数据访问层 (20 个)
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
│   │   └── InventoryCheckItemDAO.java     # 库存盘点明细 DAO
│   ├── model/                             # 实体类 (20 个)
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
│   │   └── InventoryCheckItem.java        # 库存盘点明细类
│   ├── service/
│   │   └── DataService.java               # 数据服务
│   └── util/                              # 工具类
│       ├── DatabaseManager.java           # 数据库管理器
│       ├── PasswordUtil.java              # 密码工具
│       ├── FXUtils.java                   # JavaFX 工具类
│       ├── FXMLUtils.java                 # FXML 工具类
│       ├── LoggerFactoryUtil.java         # 日志工厂工具
│       ├── StatusBarManager.java          # 状态栏管理器
│       └── ReceiptPrinter.java            # 收据打印机
├── src/test/java/com/cashier/             # 测试代码
│   ├── util/
│   │   └── PasswordUtilTest.java          # 密码工具测试
│   └── dao/
│       ├── ProductDAOTest.java            # 商品 DAO 测试
│       └── UserDAOTest.java               # 用户 DAO 测试
├── src/main/resources/
│   ├── com/cashier/view/                  # FXML 视图文件 (25 个)
│   ├── css/                               # 样式文件
│   │   ├── styles.css                     # 主样式文件
│   │   ├── light-theme.css                # 浅色主题
│   │   ├── dark-theme.css                 # 深色主题
│   │   └── intellij-theme.css             # IntelliJ主题
│   └── logback.xml                        # 日志配置
├── config/                                # 配置目录
├── docker/                                # Docker 配置
│   ├── mysql-init/                        # 数据库初始化脚本
│   └── mysql-backup/                      # 数据库备份目录
├── docs/                                  # 文档目录
└── pom.xml                                # Maven 配置文件
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

### 2. 库存管理
- 商品添加、编辑、删除
- 库存补货
- 商品搜索和筛选
- 库存预警显示
- 分类管理
- 单位管理

### 3. 会员管理
- 会员注册
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

---

## 数据库架构

### 核心表

- `users` - 用户账户
- `products` - 商品库存信息
- `members` - 会员账户和积分
- `transactions` - 交易记录主表
- `transaction_items` - 交易明细
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

---

## 测试

### 测试框架

- **JUnit 5.10.0** - 单元测试框架
- **TestFX 4.0.18** - JavaFX UI 测试框架

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

### 测试约定

1. 测试类命名: `{ClassName}Test.java`
2. 测试方法命名: `test{MethodName}` 或使用 `@DisplayName` 注解
3. 使用 `@Test`、`@BeforeAll`、`@BeforeEach` 等注解组织测试
4. 测试文件位置: `src/test/java/com/cashier/`

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

### 日志规范

使用 SLF4J + Logback 进行日志记录：

```java
private static final Logger logger = LoggerFactory.getLogger(YourClass.class);

logger.info("信息日志");
logger.warn("警告日志");
logger.error("错误日志", exception);
```

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

---

## 常见任务

### 添加新功能模块

1. 创建 Model 类（在 `model/` 目录）
2. 创建 DAO 类（在 `dao/` 目录）
3. 创建 Controller 类（在 `controller/` 目录）
4. 创建 FXML 视图文件（在 `view/` 目录）
5. 在 `MainController` 中添加菜单项
6. 在 `DatabaseManager` 中添加建表 SQL（如需要）

### 数据库表变更

1. 在 `DatabaseManager.initializeDatabase()` 中添加建表 SQL
2. 或在 `docker/mysql-init/` 中添加迁移脚本
3. 创建对应的 DAO 类
4. 更新 Model 类
5. 添加对应的测试用例

### 添加新的快捷键

1. 在 `MainView.fxml` 或相关 FXML 文件中添加快捷键提示
2. 在对应的 Controller 中添加事件处理方法
3. 更新 AGENTS.md 中的快捷键说明

### 添加单元测试

1. 在 `src/test/java/com/cashier/` 下创建测试类
2. 继承或使用 JUnit 5 注解
3. 编写测试方法并使用断言验证结果
4. 运行测试确保通过

---

## 常见问题

### 应用无法启动

1. 检查 JDK 版本（需要 Java 17+）
2. 检查 MySQL 服务是否运行
3. 检查 `config/database.properties` 配置
4. 查看日志文件 `logs/`

### 数据库连接失败

1. 确保 MySQL 8.0 正在运行
2. 检查防火墙设置
3. 验证数据库用户名和密码
4. 确认数据库 URL 格式正确
5. 尝试使用环境变量 `CASHER_DB_PASSWORD` 存储密码

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

---

## 相关文档

- [README.md](README.md) - 项目说明
- [docs/MYSQL_SETUP.md](docs/MYSQL_SETUP.md) - MySQL 部署指南
- [docs/WINDOWS_MYSQL_SETUP.md](docs/WINDOWS_MYSQL_SETUP.md) - Windows MySQL 安装指南
- [docs/DATABASE_INIT.md](docs/DATABASE_INIT.md) - 数据库初始化文档
- [docs/PURCHASE_TABLE_DESIGN.md](docs/PURCHASE_TABLE_DESIGN.md) - 采购表结构设计
- [docs/ICON_GUIDE.md](docs/ICON_GUIDE.md) - 应用图标指南

---

## 版本历史

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
- [ ] 二维码扫描支持
- [ ] 更多统计图表
- [ ] 多语言支持
- [ ] 短信通知功能
- [ ] 退货管理功能
- [ ] 数据导出功能（Excel/PDF）
- [ ] 多仓库管理
- [x] 单元测试覆盖
- [ ] 集成测试
- [ ] 性能优化