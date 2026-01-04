# 项目概述

这是一个功能完整的收银系统项目，使用 Java Swing 开发，支持图形化界面（GUI）和命令行界面（CLI）两种模式。系统集成了库存管理、会员管理、促销管理、交易记录、用户权限、交接班等完整的零售业务功能，并使用 FlatLaf 框架实现了现代化的界面设计。

## 技术栈

- **语言**: Java
- **JDK 版本**: JDK 17
- **IDE**: IntelliJ IDEA
- **GUI框架**: Swing + FlatLaf 3.5
- **数据存储**: 文本文件（基于 DataManager 类）
- **项目结构**: 单模块项目，所有源代码位于根目录

## 核心功能

### 1. 收银系统 GUI（CashierSystemGUI）
- **用户登录**: 支持多用户登录，包含角色权限管理
- **库存管理**: 商品添加、编辑、删除、补货、搜索、排序
- **购物车**: 商品添加、修改数量、删除、批量操作
- **结账系统**: 支持多种支付方式、税率设置、促销应用
- **会员管理**: 会员注册、积分、等级、折扣、余额充值
- **促销管理**: 满减、折扣等促销活动管理
- **交易记录**: 完整的交易历史记录和查询
- **数据统计**: 销售额、交易量、平均客单价等统计
- **交接班管理**: 班次开始/结束、交接班记录、收入统计
- **用户管理**: 用户增删改查、权限管理
- **操作日志**: 记录所有关键操作
- **数据备份/恢复**: 支持数据备份和恢复功能
- **主题切换**: 支持浅色、深色、IntelliJ 三种主题

### 2. 收银系统 CLI（CashierSystem）
- 命令行界面版本，支持基本的收银功能
- 库存管理、购物车、结账、搜索等核心功能

## 项目结构

```
hello/
├── 核心业务类
│   ├── CashierSystem.java           # 命令行版收银系统
│   ├── CashierSystemGUI.java        # GUI版收银系统（主程序）
│   ├── DataManager.java             # 数据管理类（文件读写）
│   ├── Product.java                 # 商品实体类
│   ├── Member.java                  # 会员实体类
│   ├── User.java                    # 用户实体类
│   ├── Transaction.java             # 交易实体类
│   ├── Category.java                # 分类实体类
│   ├── Promotion.java               # 促销实体类
│   ├── RechargeRecord.java          # 充值记录类
│   ├── OperationLog.java            # 操作日志类
│   └── Shift.java                   # 交接班记录类
│
├── 学习示例类
│   ├── Hello.java                   # 简单的 Hello World
│   ├── Student.java                 # 学生信息演示
│   └── InteractiveHello.java        # 交互式 Hello
│
├── 配置文件
│   ├── hello.iml                    # IntelliJ IDEA 模块配置
│   ├── flatlaf-3.5.jar              # FlatLaf 界面框架
│   ├── compile_with_flatlaf.sh      # Mac/Linux 编译脚本
│   ├── run_with_flatlaf.sh          # Mac/Linux 运行脚本
│   ├── compile_with_flatlaf.bat     # Windows 编译脚本
│   └── run_with_flatlaf.bat         # Windows 运行脚本
│
├── 数据目录
│   ├── inventory.txt                # 库存数据
│   ├── transactions.txt             # 交易记录
│   ├── members.txt                  # 会员数据
│   ├── users.txt                    # 用户数据
│   ├── categories.txt               # 商品分类
│   ├── promotions.txt               # 促销活动
│   ├── recharge.txt                 # 充值记录
│   ├── operation_logs.txt           # 操作日志
│   ├── shifts.txt                   # 交接班记录
│   └── settings.txt                 # 系统设置
│
├── 文档
│   ├── IFLOW.md                     # 项目文档（本文件）
│   ├── FLATLAF_README.md            # FlatLaf 使用说明
│   ├── FLATLAF_UPGRADE_SUMMARY.md   # FlatLaf 升级总结
│   └── ICON_FIX_SUMMARY.md          # 图标修复总结
│
├── .idea/                           # IntelliJ IDEA 配置目录
└── out/                             # 编译输出目录
```

## 主要文件说明

### CashierSystemGUI.java
- **路径**: `/Users/nevell/code/hello/CashierSystemGUI.java`
- **用途**: 图形化收银系统主程序
- **功能**: 
  - 完整的 GUI 界面，包含 5 个主要标签页
  - 用户登录和权限验证
  - 库存管理、购物车、结账、交易记录、数据统计
  - 会员管理、促销管理、交接班管理
  - 键盘快捷键支持（F1-F12、Ctrl组合键）
  - 数据自动保存和备份/恢复功能
  - FlatLaf 主题切换

### CashierSystem.java
- **路径**: `/Users/nevell/code/hello/CashierSystem.java`
- **用途**: 命令行版收银系统
- **功能**: 
  - 基于控制台的收银功能
  - 库存管理、购物车、结账、搜索、排序
  - 彩色终端输出支持

### DataManager.java
- **路径**: `/Users/nevell/code/hello/DataManager.java`
- **用途**: 数据持久化管理类
- **功能**: 
  - 所有数据的文件读写操作
  - 支持数据备份和恢复
  - 数据格式兼容性处理（支持旧格式升级）

### Product.java
- **路径**: `/Users/nevell/code/hello/Product.java`
- **用途**: 商品实体类
- **属性**: 名称、价格、数量、分类、条形码、单位、描述、品牌、供应商、规格、最低库存、成本价

### Member.java
- **路径**: `/Users/nevell/code/hello/Member.java`
- **用途**: 会员实体类
- **属性**: 手机号、姓名、积分、等级、折扣率、余额、生日
- **功能**: 等级自动升级、生日检测

### User.java
- **路径**: `/Users/nevell/code/hello/User.java`
- **用途**: 用户实体类
- **属性**: 用户名、密码、姓名、角色、创建时间、最后登录时间、激活状态
- **角色**: admin（管理员）、cashier（收银员）、finance（财务）

## 构建和运行

### 编译项目

#### 使用脚本（推荐）
```bash
# Mac/Linux
./compile_with_flatlaf.sh

# Windows
compile_with_flatlaf.bat
```

#### 手动编译
```bash
# 编译所有 Java 文件（包含 FlatLaf）
javac -cp flatlaf-3.5.jar *.java

# 或者使用 IntelliJ IDEA 的构建功能（Build -> Build Project）
```

### 运行程序

#### GUI 版本（推荐）
```bash
# Mac/Linux
./run_with_flatlaf.sh

# Windows
run_with_flatlaf.bat

# 或手动运行
java -cp flatlaf-3.5.jar:. CashierSystemGUI
```

#### CLI 版本
```bash
java CashierSystem
```

#### 学习示例
```bash
# 运行 Hello
java Hello

# 运行 Student
java Student

# 运行交互式 Hello
java InteractiveHello
```

### 默认用户账户

系统首次运行时会创建默认管理员账户：
- **用户名**: admin
- **密码**: admin123
- **角色**: 管理员

### 使用 IntelliJ IDEA
1. 打开 IntelliJ IDEA
2. 导入项目（选择 `hello.iml`）
3. 右键点击 `CashierSystemGUI.java`
4. 选择 "Run" 或 "Debug"

## 快捷键说明

### 功能键
- **F1**: 添加商品
- **F2**: 补货
- **F3**: 删除商品
- **F4**: 搜索（聚焦搜索框）
- **F5**: 刷新当前面板
- **F6**: 分类管理
- **F7**: 会员管理
- **F8**: 结账
- **F9**: 促销管理
- **F10**: 库存预警
- **F11**: 数据备份
- **F12**: 数据恢复

### Ctrl 组合键
- **Ctrl+N**: 添加商品
- **Ctrl+S**: 保存数据
- **Ctrl+F**: 搜索
- **Ctrl+D**: 导出数据
- **Ctrl+R**: 刷新当前面板
- **Ctrl+Q**: 退出程序
- **Ctrl+A**: 全选
- **Ctrl+E**: 编辑选中项
- **Ctrl+B**: 批量操作
- **Ctrl+M**: 会员管理
- **Ctrl+T**: 交易统计
- **Ctrl+1~4**: 切换标签页

### 其他
- **ESC**: 清空搜索
- **Delete**: 删除选中项
- **Enter**: 确认/搜索

## 开发规范

### 代码风格
- 使用 4 空格缩进
- 类名使用大驼峰命名法（PascalCase）
- 变量名使用小驼峰命名法（camelCase）
- 常量使用全大写下划线命名法（UPPER_SNAKE_CASE）
- GUI 组件使用有意义的命名（如 inventoryTable、cartTable）

### 项目约定
- 所有源代码文件位于项目根目录
- 数据文件存储在 `data/` 目录
- 编译输出目录为 `out/production/hello/`
- 使用 JDK 17 进行编译和运行
- 使用 IntelliJ IDEA 作为主要开发环境
- 数据格式使用 `|` 分隔，特殊字符需要转义

### 数据管理
- 所有数据操作通过 DataManager 类进行
- 数据修改后自动保存到文件
- 支持数据备份和恢复功能
- 操作日志自动记录关键操作

## 系统要求

- **JDK**: Java 11 或更高版本（推荐 JDK 17）
- **操作系统**: Windows、macOS、Linux
- **内存**: 最小 512MB，推荐 1GB+
- **磁盘**: 最小 100MB 可用空间

## 特色功能

### 1. 现代化界面
- 使用 FlatLaf 框架实现扁平化设计
- 支持浅色、深色、IntelliJ 三种主题
- 流畅的动画效果和交互体验

### 2. 完整的权限系统
- 三种角色：管理员、收银员、财务
- 基于角色的权限控制
- 用户激活/禁用管理

### 3. 会员系统
- 会员等级自动升级（普通→银卡→金卡→钻石）
- 积分累计和折扣优惠
- 会员余额充值功能
- 生日特权检测

### 4. 促销管理
- 支持多种促销类型（满减、折扣）
- 促销活动时间范围控制
- 促销启用/禁用管理

### 5. 交接班管理
- 班次开始/结束记录
- 班次收入统计
- 多种支付方式收入统计（现金、微信、支付宝、银行卡）

### 6. 数据安全
- 数据自动备份功能
- 操作日志完整记录
- 数据恢复支持

## 注意事项

- 项目目前没有使用构建工具（如 Maven 或 Gradle）
- FlatLaf 3.5 JAR 文件必须包含在项目目录中
- 数据文件存储在 `data/` 目录，首次运行会自动创建
- 默认管理员账户：admin/admin123
- 建议定期备份数据
- 修改密码等敏感操作需要管理员权限

## 扩展建议

### 功能扩展
- 添加商品图片支持
- 实现小票打印功能
- 集成支付网关（微信、支付宝）
- 添加报表导出（Excel、PDF）
- 实现多门店管理
- 添加库存预警功能

### 技术升级
- 迁移到数据库（MySQL、PostgreSQL）
- 使用 Maven 或 Gradle 管理依赖
- 引入 Spring Boot 框架
- 实现前后端分离架构
- 添加单元测试和集成测试
- 使用 CI/CD 自动化部署

## 许可证

FlatLaf 使用 Apache 2.0 许可证
FlatLaf 官网: https://www.formdev.com/flatlaf/