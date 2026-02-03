# 项目上下文文件

## 项目概述

这是一个功能完整的收银系统（Cashier System），使用 JavaFX 17.0.8 开发，提供现代化的图形化界面。系统采用 MVC + DAO + Service 三层架构，支持 MySQL 8.0 数据库和文件存储双模式，支持多种支付方式、会员管理、库存管理、促销管理等核心功能。

**当前版本**: v2.2.0 | **最新更新**: 2026-02-03

### 主要技术栈

- **编程语言**: Java 17
- **GUI 框架**: JavaFX 17.0.8
- **构建工具**: Maven 3.8+
- **测试框架**: JUnit 5.10.0 + TestFX 4.0.18
- **UI 增强**: ControlsFX 11.2.1
- **图标库**: FontAwesomeFX 4.7.0-9.1.2
- **数据库**: MySQL 8.0 (主存储) + 文本文件 (备用存储)
- **连接池**: HikariCP 5.1.0
- **密码加密**: BCrypt 0.10.2
- **容器化**: Docker & Docker Compose

### 项目架构

采用经典的三层架构：MVC + DAO + Service

- **Model**: 数据模型层（`com.cashier.model`）
  - `Product` - 商品模型
  - `Member` - 会员模型
  - `User` - 用户模型
  - `Transaction` - 交易模型
  - `Promotion` - 促销模型
  - `Category` - 分类模型
  - `RechargeRecord` - 充值记录
  - `OperationLog` - 操作日志
  - `Shift` - 交接班记录
  - `CartItem` - 购物车项
  - `DataManager` - 文件存储数据管理类（备用）

- **View**: 视图层（`src/main/resources/com/cashier/view`）
  - 使用 FXML 定义界面布局
  - CSS 样式文件（支持三种主题）

- **Controller**: 控制器层（`com.cashier.controller`）
  - `MainController` - 主界面控制器
  - `CartController` - 购物车控制器
  - `CheckoutController` - 结账控制器
  - `InventoryController` - 库存管理控制器
  - `MemberController` - 会员管理控制器
  - `MemberEditController` - 会员编辑控制器
  - `PromotionController` - 促销管理控制器
  - `TransactionController` - 交易记录控制器
  - `StatisticsController` - 数据统计控制器
  - `ShiftController` - 交接班控制器
  - `UserController` - 用户管理控制器
  - `SettingsController` - 系统设置控制器
  - `LoginController` - 登录控制器
  - `ProductEditController` - 商品编辑控制器
  - `RechargeController` - 充值控制器
  - `RestockController` - 补货控制器
  - `PasswordResetController` - 密码重置控制器

- **DAO**: 数据访问层（`com.cashier.dao`）
  - `UserDAO` - 用户数据访问对象
  - `ProductDAO` - 商品数据访问对象
  - `MemberDAO` - 会员数据访问对象
  - `TransactionDAO` - 交易数据访问对象
  - `ShiftDAO` - 交接班数据访问对象

- **Service**: 服务层（`com.cashier.service`）
  - `DataService` - 数据服务（提供与 DataManager 相同的接口，但使用 MySQL）

- **Util**: 工具类（`com.cashier.util`）
  - `DatabaseManager` - 数据库管理器（HikariCP 连接池管理）
  - `DataMigrationTool` - 数据迁移工具（文件 → MySQL）
  - `PasswordUtil` - 密码加密工具（BCrypt）
  - `FXUtils` - JavaFX 工具类
  - `FXMLUtils` - FXML 工具类
  - `StatusBarManager` - 状态栏管理器
  - `ReceiptPrinter` - 小票打印工具

### 核心功能模块

1. **POS 系统** - 购物车和结账一体化
   - 支持现金、微信、支付宝、银行卡支付
   - 现金支付带找零计算
   - 会员折扣自动应用（折扣值 0-10，10 表示不打折，0 表示免费）
   - 完整的快捷键系统，大幅提升收银效率

2. **库存管理**
   - 商品添加、编辑、删除
   - 库存补货、搜索
   - 库存预警显示

3. **会员管理**
   - 会员注册、积分累计
   - 等级自动升级（普通→银卡→金卡→钻石）
   - 余额充值、生日特权
   - 折扣值系统（10=不打折，9.8=9.8折，9=9折，0=免费）

4. **促销管理**
   - 满减、折扣、优惠券
   - 促销时间范围控制

5. **交易记录**
   - 完整交易历史
   - 按日期、支付方式筛选

6. **数据统计**
   - 销售额、交易量、平均客单价统计

7. **交接班管理**
   - 班次开始/结束记录
   - 多支付方式收入统计

8. **用户管理**
   - 用户增删改查
   - 权限管理（管理员、收银员、财务）

9. **系统设置**
   - 主题切换（浅色/深色/IntelliJ）
   - 税率配置
   - 数据备份/恢复

### 数据存储架构

系统采用**双存储架构**，确保数据安全和高可用性：

1. **主存储：MySQL 8.0 数据库**
   - 使用 HikariCP 连接池，高性能数据库访问
   - 完整的 DAO 层封装，提供类型安全的数据库操作
   - 事务支持，确保数据一致性
   - 自动创建表结构和索引

2. **备用存储：文件系统**
   - 当数据库不可用时自动降级
   - 数据存储在 `data/` 目录的 `.txt` 文件中
   - 确保系统在数据库故障时仍可运行

### 最近更新

#### v2.2.0 (2026-02-03)

**数据库迁移完成**
- ✨ 完整迁移到 MySQL 8.0 数据库
  - 所有核心功能使用 MySQL 数据持久化
  - 实现优雅降级：数据库失败时自动切换到文件存储
  - 使用 HikariCP 连接池，高性能数据库访问
  - DAO 层封装：UserDAO, ProductDAO, MemberDAO, TransactionDAO, ShiftDAO
- 📊 支持通过 Docker Compose 一键启动 MySQL
- 🔄 自动数据迁移工具：从文件存储无缝迁移到 MySQL
- 📦 完整的数据备份和恢复方案
- 🛡️ 事务支持：确保数据一致性

#### v2.1.0 (2026-01-08)

**POS 页面快捷键系统优化**
- ✨ 实现完整的快捷键系统，大幅提升收银效率
  - F1 - 快速添加商品
  - Delete - 移除选中商品
  - Ctrl+L - 清空购物车
  - 双击商品 - 快速添加到购物车
  - Ctrl+F - 聚焦搜索框
  - Ctrl+M - 聚焦会员手机号框
  - F8 - 现金支付
  - Ctrl+1/2/3 - 微信/支付宝/银行卡支付
  - Ctrl+/ - 显示快捷键帮助
- 🎨 UI 优化：所有按钮添加快捷键提示文本
- 🔧 修复退出登录功能，正确返回登录界面
- 🧹 结算完成后自动清除会员信息
- 📝 统一左侧菜单和标签页名称
- 🗂️ 优化顶端菜单，移除冗余菜单项

#### v2.0.0 (2025-12-XX)

**会员折扣系统重构**
- 🔨 会员折扣值从 0-1 改为 0-10（10 表示不打折，0 表示免费）
- 🐛 修复折扣计算逻辑错误
- 🐛 修复购物车数量更新不刷新问题
- 🐛 修复无会员时应付金额为 0 的问题

## 构建和运行

### 环境要求

**必需组件**:
- **JDK**: Java 17 或更高版本
- **Maven**: 3.8 或更高版本
- **MySQL**: 8.0 或更高版本（通过 Docker 或本地安装）
- **Docker**: Docker & Docker Compose（推荐，用于快速启动 MySQL）
- **操作系统**: Windows、macOS、Linux
- **内存**: 最小 2GB，推荐 4GB+
- **磁盘**: 最小 500MB 可用空间（含数据库）

### 安装步骤

1. **克隆仓库**
```bash
git clone https://gitee.com/nevell/hello.git
cd hello
```

2. **启动 MySQL 数据库（三种方式）**

**方式一：使用 Docker Compose（推荐）**
```bash
# 启动 MySQL 8.0 和 phpMyAdmin
docker-compose up -d

# 查看日志
docker-compose logs -f mysql

# 访问 phpMyAdmin 管理界面
# http://localhost:8080
# 用户名: root, 密码: RootPassword123!
```

**方式二：使用本地 MySQL**
```bash
# 创建数据库和用户
mysql -u root -p

CREATE DATABASE cashier_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cashier'@'%' IDENTIFIED BY 'YourPassword123!';
GRANT ALL PRIVILEGES ON cashier_system.* TO 'cashier'@'%';
FLUSH PRIVILEGES;
```

**方式三：使用 Colima (macOS ARM)**
```bash
# 启动 Colima 并启用端口转发
colima start --network-address --network-host-addresses

# 然后运行 docker-compose up -d
```

3. **配置数据库连接**

创建 `config/database.properties`：
```properties
db.url=jdbc:mysql://localhost:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai
db.username=root
db.password=RootPassword123!
db.pool.size=10
```

4. **编译项目**
```bash
mvn clean compile
```

5. **运行程序**
```bash
mvn javafx:run
```

首次运行时，系统会自动检测并迁移数据到 MySQL。

**打包后运行**：
```bash
mvn clean package
java -jar target/cashier-system-fx-2.0.0.jar
```

### Maven 命令

```bash
# 清理并编译项目
mvn clean compile

# 运行项目（使用 JavaFX Maven 插件）
mvn javafx:run

# 打包项目
mvn clean package

# 运行打包后的 JAR 文件
java -jar target/cashier-system-fx-2.0.0.jar

# 运行测试
mvn test

# 跳过测试打包
mvn clean package -DskipTests

# 启动 Docker MySQL
docker-compose up -d

# 停止 Docker MySQL
docker-compose down

# 查看 Docker 日志
docker-compose logs -f mysql
```

### 主程序入口

- **主类**: `com.cashier.CashierSystemFXApplication`
- **FXML 资源路径**: `/com/cashier/view/`
- **CSS 资源路径**: `/css/`

### 默认账户

- **用户名**: admin
- **密码**: admin123
- **角色**: 管理员

## 开发约定

### 代码风格

1. **包命名**: 使用小写字母，如 `com.cashier.controller`
2. **类命名**: 使用帕斯卡命名法（PascalCase），如 `CartController`
3. **方法命名**: 使用驼峰命名法（camelCase），如 `handleSearchMember`
4. **常量命名**: 使用全大写下划线分隔，如 `DEFAULT_THEME`
5. **变量命名**: 使用驼峰命名法（camelCase）

### 项目结构规范

```
src/main/java/com/cashier/
├── CashierSystemFXApplication.java    # 主程序入口
├── constant/                           # 常量定义
│   ├── FXConstants.java               # JavaFX 常量
│   └── SpacingConstants.java          # 间距常量
├── dao/                               # 数据访问层 (v2.2.0 新增)
│   ├── UserDAO.java                   # 用户 DAO
│   ├── ProductDAO.java                # 商品 DAO
│   ├── MemberDAO.java                 # 会员 DAO
│   ├── TransactionDAO.java            # 交易 DAO
│   └── ShiftDAO.java                  # 交接班 DAO
├── service/                           # 服务层 (v2.2.0 新增)
│   └── DataService.java               # 数据服务
├── controller/                         # 控制器层
├── model/                             # 数据模型层
└── util/                              # 工具类
    ├── DatabaseManager.java           # 数据库管理器 (v2.2.0 新增)
    ├── DataMigrationTool.java         # 数据迁移工具 (v2.2.0 新增)
    └── ...

config/                                # 配置目录 (v2.2.0 新增)
├── database.properties                # 数据库配置
└── database.properties.example        # 数据库配置示例

docker/                                # Docker 配置 (v2.2.0 新增)
├── mysql-init/                        # MySQL 初始化脚本
└── mysql-backup/                      # 备份目录

docs/                                  # 文档目录 (v2.2.0 新增)
└── MYSQL_SETUP.md                     # MySQL 部署指南
```

### 数据存储约定

#### MySQL 数据库（主存储）

- **数据库**: `cashier_system`
- **字符集**: `utf8mb4`
- **排序规则**: `utf8mb4_unicode_ci`
- **表结构**:
  - `users` - 用户账户（管理员、收银员、财务）
  - `products` - 商品库存信息
  - `members` - 会员账户和积分
  - `transactions` - 交易记录主表
  - `transaction_items` - 交易明细（商品列表）
  - `shifts` - 交接班记录

#### 文件存储（备用存储）

- **数据目录**: `data/`（自动创建）
- **数据格式**: 文本文件，使用 `|` 分隔符
- **数据文件**:
  - `inventory.txt` - 库存数据
  - `transactions.txt` - 交易记录
  - `members.txt` - 会员数据
  - `users.txt` - 用户数据
  - `promotions.txt` - 促销数据
  - `categories.txt` - 分类数据
  - `recharge.txt` - 充值记录
  - `operation_logs.txt` - 操作日志
  - `shifts.txt` - 交接班记录
  - `settings.txt` - 系统设置

### 数据迁移约定

系统首次启动时会自动执行数据迁移：
1. 检测 MySQL 数据库是否为空
2. 自动备份原有 `.txt` 数据文件到 `data/backup_<timestamp>/`
3. 将数据迁移到 MySQL 数据库
4. 显示迁移统计信息

### UI 开发约定

1. **FXML 文件命名**: 使用 PascalCase + View 后缀，如 `CartView.fxml`
2. **控制器命名**: 与 FXML 文件对应，如 `CartController`
3. **CSS 样式**:
   - 主样式文件: `styles.css`
   - 主题文件: `{theme}-theme.css`（light、dark、intellij）
4. **常量定义**: 使用 `FXConstants` 类定义颜色、尺寸、字体等常量

### 数据管理约定

- 使用 `DataService` 类统一管理 MySQL 数据库操作
- 使用 `DataManager` 类作为文件存储的备用方案
- DAO 层封装所有数据库操作，提供类型安全的接口
- 数据库操作使用 PreparedStatement 防止 SQL 注入
- 支持批量操作提高性能
- 事务支持确保数据一致性

### 快捷键约定

系统支持以下快捷键：

**功能键**:
- F1 - 添加商品
- F2 - 补货
- F3 - 删除商品
- F4 - 搜索
- F5 - 刷新当前面板
- F6 - 分类管理
- F7 - 会员管理
- F8 - 结账
- F9 - 促销管理
- F10 - 库存预警
- F11 - 数据备份
- F12 - 数据恢复
- ESC - 清空搜索或关闭标签页

**Ctrl 组合键**:
- Ctrl+N - 添加商品
- Ctrl+S - 保存数据
- Ctrl+F - 搜索
- Ctrl+R - 刷新当前面板
- Ctrl+Q - 退出程序
- Ctrl+A - 全选
- Ctrl+B - 批量操作
- Ctrl+M - 会员管理
- Ctrl+T - 交易统计
- Ctrl+1~4 - 切换标签页

**POS/结账页面快捷键**:

**商品操作**:
- F1 - 添加选中商品到购物车
- Delete - 移除购物车中选中的商品
- Ctrl+L - 清空购物车
- 双击商品 - 快速添加到购物车

**搜索和查询**:
- Ctrl+F - 聚焦到搜索框
- Enter - 执行搜索（在搜索框中）
- Escape - 清空搜索（在搜索框中）

**会员操作**:
- Ctrl+M - 聚焦到会员手机号框
- Enter - 查询会员（在会员手机号框中）
- Escape - 清空会员信息（在会员手机号框中）

**支付方式**:
- F8 - 现金支付
- Ctrl+1 - 微信支付
- Ctrl+2 - 支付宝支付
- Ctrl+3 - 银行卡支付

**帮助**:
- Ctrl+/ - 显示快捷键帮助对话框

### 主题系统

支持三种主题：
- **light** - 浅色主题（默认）
- **dark** - 深色主题
- **intellij** - IntelliJ 风格主题

主题切换通过 `applyTheme(Scene scene, String themeName)` 方法实现。

### 权限系统

三种角色权限：
- **admin** - 管理员（完整权限）
- **cashier** - 收银员（日常收银操作）
- **finance** - 财务（报表和数据统计）

### 日志和调试

- 使用 `System.out.println` 进行简单调试
- 关键操作记录到 `operation_logs.txt`
- 错误使用 `e.printStackTrace()` 输出
- 数据库操作使用 SQLException 捕获异常

### 测试规范

- 使用 JUnit 5 进行单元测试
- 使用 TestFX 进行 UI 测试
- 测试类命名: `{ClassName}Test.java`

### 数据库开发约定

- 使用 DAO 模式封装数据库操作
- 所有 SQL 操作使用 PreparedStatement
- 数据库连接使用 HikariCP 连接池
- 支持批量操作提高性能
- 事务支持确保数据一致性
- 数据库配置文件: `config/database.properties`

## 重要提示

1. **数据安全**: 主要数据存储在 MySQL 数据库中，建议定期备份
2. **并发访问**: MySQL 支持多用户并发访问
3. **数据库**: 当前使用 MySQL 8.0 作为主存储，文件存储作为备用
4. **生产环境**: 本项目仅供学习和参考使用，不建议直接用于生产环境
5. **会员折扣系统**: 折扣值范围为 0-10，10 表示不打折，0 表示免费
6. **快捷键系统**: POS 页面支持完整的快捷键操作，可按 Ctrl+/ 查看帮助
7. **数据迁移**: 首次运行时会自动从文件存储迁移到 MySQL
8. **优雅降级**: MySQL 不可用时自动切换到文件存储

## 常见问题

### 编译错误

如果遇到 JavaFX 相关错误，确保：
- JDK 版本为 17 或更高
- Maven 依赖正确配置
- 使用 `mvn javafx:run` 运行而不是 `mvn exec:java`

### 数据库连接失败

如果无法连接到 MySQL 数据库：
1. 检查 MySQL 服务是否运行: `docker-compose ps` 或 `systemctl status mysql`
2. 检查配置文件 `config/database.properties` 中的连接信息
3. 确保数据库用户有足够的权限
4. 检查防火墙是否允许 3306 端口

### 数据文件丢失

如果 `data/` 目录不存在，系统会自动创建并初始化默认数据。

### 主题切换无效

确保 CSS 文件路径正确，主题文件存在于 `src/main/resources/css/` 目录。

### 会员折扣计算错误

确保会员折扣值在 0-10 范围内：
- 10.0 = 不打折
- 9.8 = 9.8折
- 9.0 = 9折
- 8.5 = 8.5折
- 0.0 = 免费

### Docker 相关问题

**Docker MySQL 无法启动**:
```bash
# 查看日志
docker-compose logs mysql

# 删除并重新创建容器
docker-compose down
docker-compose up -d
```

**Colima 端口转发问题** (macOS ARM):
```bash
# 启动 Colima 时启用网络地址
colima start --network-address --network-host-addresses

# 重启 Colima
colima restart
```

## 未来计划

- [ ] 云端数据同步
- [ ] 移动端应用
- [ ] 二维码扫描支持
- [x] 小票打印功能（已实现）
- [ ] 更多统计图表
- [ ] 多语言支持
- [ ] 短信通知功能
- [x] 完整的快捷键系统（v2.1.0 已实现）
- [x] UI 优化和菜单结构调整（v2.1.0 已实现）
- [x] 退出登录功能修复（v2.1.0 已实现）
- [x] 数据库支持 MySQL（v2.2.0 已实现）
- [x] 交接班管理数据库集成（v2.2.0 已实现）
- [x] Docker Compose 一键部署（v2.2.0 已实现）
- [x] 数据迁移工具（v2.2.0 已实现）

## 许可证

本项目采用 **木兰宽松许可证 v2 (MulanPSL2)**

## 联系方式

- 代码仓库: https://gitee.com/nevell/hello.git
- 问题反馈: https://gitee.com/nevell/hello/issues
- MySQL 部署指南: docs/MYSQL_SETUP.md
- Docker 快速部署: docker-mysql-setup.md