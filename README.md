# 收银系统 (Cashier System)

一个功能完整的收银系统，使用 JavaFX 17 开发，提供现代化的图形化界面。

**当前版本**: v2.4.1 | **最新更新**: 2026-03-01

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.8-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)

## ✨ 特性

### 🖥️ 现代化图形界面
- 基于 JavaFX 17.0.8 的现代化界面
- 支持浅色、深色、IntelliJ 三种主题
- 响应式布局，适配不同屏幕尺寸
- 流畅的动画效果和微交互

### 💰 核心功能
- **POS系统** - 购物车和结账一体化，支持多种支付方式
  - 现金支付（带找零计算）
  - 微信支付
  - 支付宝支付
  - 银行卡支付
  - 扫描枪自动扫码
  - 扫描音效反馈
  - POS 模式专用界面
- **商品管理** - 商品添加、编辑、删除、快速入库、搜索
  - 商品编号自动生成
  - 库存通过进销存流程管理（采购入库、快速入库、库存盘点）
  - 商品分类和单位管理
  - 库存预警显示
  - 条形码重复支持
  - 商品数据导入（CSV/GitHub）
- **会员管理** - 会员注册、积分、等级、折扣、余额充值
  - 会员编号自动生成
  - 等级自动升级（普通→银卡→金卡→钻石）
  - 积分累计和折扣优惠
  - 余额充值功能
  - 生日特权检测
- **促销管理** - 满减、折扣、优惠券等多种促销类型
- **交易记录** - 完整的交易历史记录和查询
- **数据统计** - 销售额、交易量、平均客单价等统计
- **交接班管理** - 班次开始/结束、交接班记录、收入统计
- **用户管理** - 用户增删改查、权限管理
- **系统设置** - 主题切换、税率配置、数据备份/恢复

### 🏭 采购管理
- **供应商管理** - 供应商信息管理、供应商分级（A级/B级/C级）、供应商搜索
- **采购订单** - 创建采购订单、多商品采购、订单状态管理、自动计算总金额
- **采购审批** - 审批流程管理、审批通过/拒绝、审批意见记录、审批历史查询
- **采购入库** - 基于已审批订单入库、支持部分入库、自动更新库存、生成入库单

### 🔍 库存盘点
- **盘点单管理** - 创建盘点单、盘点类型选择（全盘/部分盘点）
- **实际库存录入** - 账面数量对比、实际数量录入、自动计算差异
- **盘点完成** - 自动调整库存、差异原因记录、盘点统计

### 📊 报表统计
- **采购报表** - 采购订单统计、采购金额趋势（日/周/月）、供应商采购排名、分类采购统计
- **库存报表** - 库存周转率分析、滞销商品分析、库存积压分析、库存不足提醒
- **利润分析** - 采购成本统计、销售收入统计、毛利润和毛利率分析、净利润计算

### 🔧 数据管理
- **数据导入** - 支持 CSV 文件导入商品数据
- **GitHub 集成** - 支持 GitHub 商品条码库导入
- **缓存管理** - 商品数据缓存（5分钟过期）
- **数据备份** - 自动和手动数据备份

### 🖨️ 硬件支持
- **打印机管理** - 支持多种打印机设备（热敏、针式、喷墨）
- **打印预览** - 打印预览功能
- **打印模板** - 打印模板定制
- **扫描枪管理** - 支持 USB HID 扫描枪
- **自动检测** - 自动检测扫描设备
- **智能焦点** - 智能焦点管理

### 🔒 安全与权限
- 三种角色权限管理（管理员、收银员、财务）
- 操作日志完整记录
- 数据自动备份和恢复
- 密码复杂度检查
- 密码 BCrypt 加密存储

## 🎯 最近更新

### v2.3.2 (2026-03-01)

**PDF 导出优化**
- 📄 修复 PDF 导出时间显示问题
  - 日期时间格式自动分成两行显示（日期在上，时间在下）
  - 修复时间列宽度计算，确保日期和时间都能完整显示
  - 修复文本绘制顺序，确保第一行（日期）在顶部，第二行（时间）在底部
- 📊 优化表格布局
  - 修复表格边框显示问题，确保表头和数据行都有完整的竖线分隔
  - 优化列宽分配算法，按列类型设置不同的最小宽度
  - 增加时间列和班次编号列的宽度，确保内容完整显示
- 🔧 修复数据问题
  - 修复交接班导出时的 NULL 时间处理问题
  - 修复 calculateLineCount 方法，使其与 splitTextIntoLines 逻辑一致
  - 添加 PDF 导出时间格式优化文档

**文档更新**
- 📝 添加 `docs/PDF_TIME_FORMAT_OPTIMIZATION.md` - PDF 导出时间格式优化文档
- 🔄 更新所有文档版本号到 v2.3.2

### v2.3.1 (2026-02-13)

**商品管理优化**
- 📦 商品管理模块重构
  - 将"库存管理"改名为"商品管理"，更准确反映功能定位
  - 移除添加商品时的库存数量输入，通过进销存流程管理库存
  - 新增商品编号自动生成功能（格式：P + 年月日 + 4位序号）
  - 修复条形码重复问题（移除 UNIQUE 约束）
- 🎨 界面优化
  - 优化商品管理列表选中效果（渐变背景色 + 左侧边框）
  - 优化补货功能为"快速入库"，支持多种入库来源
  - 实时预览入库后库存和金额
  - 添加入库来源选择（采购入库、退回入库、调拨入库等）
- 🔧 数据库优化
  - 自动检测并添加 `member_code` 字段
  - 为现有会员自动生成会员编号
  - 优化数据库迁移逻辑

**硬件支持增强**
- 🖨️ 打印机管理模块
  - 支持多种打印机设备（热敏、针式、喷墨）
  - 打印任务队列管理
  - 打印预览功能
  - 打印模板定制
- 🔫 扫描枪管理模块
  - 支持 USB HID 扫描枪
  - 自动检测扫描设备
  - 智能焦点管理
  - 扫描音效反馈（成功、失败、未找到）

**数据管理增强**
- 📥 数据导入功能
  - 支持 CSV 文件导入商品数据
  - 支持 GitHub 商品条码库导入
  - 自动分类和单位标准化
  - 自动创建缺失的分类和单位
- 💾 缓存管理
  - 商品数据缓存（5分钟过期）
  - 多维度缓存（ID、名称、条形码）
  - 自动缓存刷新
  - 缓存预热功能

**架构优化**
- 🏗️ Service 层封装
  - InventoryService - 库存相关业务逻辑
  - MemberService - 会员相关业务逻辑
  - TransactionService - 交易相关业务逻辑
- 🎵 音效支持
  - 扫描成功音效
  - 扫描失败音效
  - 扫描未找到音效

### v2.3.0 (2026-02-07)

**进销存功能完善**
- 🏭 采购管理模块
  - 供应商管理：支持供应商分级（A级、B级、C级）、供应商信息管理
  - 采购订单：创建采购订单、多商品采购、订单状态管理（待审批、已审批、已拒绝、已完成）
  - 采购审批：审批流程管理、审批通过/拒绝、审批意见记录
  - 采购入库：基于已审批订单入库、支持部分入库、自动更新库存
- 🔍 库存盘点模块
  - 创建盘点单（全盘/部分盘点）
  - 实际库存录入、自动计算差异
  - 完成盘点后自动调整库存
- 📊 报表统计模块
  - 采购报表：采购订单统计、采购金额趋势、供应商采购排名
  - 库存报表：库存周转率、滞销商品、库存积压分析
  - 利润分析：采购成本、销售收入、毛利率、净利润计算
- 🗄️ 数据库表设计
  - 新增8张采购相关数据表
  - 完整的DAO层封装（8个DAO类）
  - 支持完整的采购业务流程
- 🎨 界面优化
  - 新增5个采购管理界面
  - 新增3个报表统计界面
  - 统一的UI风格和交互体验

### v2.2.1 (2026-02-04)

**UI 优化和数据库初始化完善**
- 🎨 现金支付页面 UI 优化
  - 放大面额按钮（100x60px）
  - 放大输入框（高度45px）
  - 放大支付按钮和提示文本
  - 提升可读性和易用性
- 🆔 模型统一管理
  - 为 Category 模型添加 ID 字段
  - 为 Unit 模型添加 ID 字段
  - 为 User 模型添加 ID 字段
  - 统一使用 AUTO_INCREMENT PRIMARY KEY
- 🗄️ 数据库初始化脚本完善
  - 添加 `02-alter-tables.sql` 表结构升级脚本
  - 添加 `03-sample-data.sql` 示例数据脚本
  - 包含 10 条商品数据
  - 包含 3 条会员数据
  - 包含 3 条促销数据
  - 包含 5 条交易记录
- 🔤 字符编码修复
  - 数据库连接字符集从 utf8 改为 utf8mb4
  - 修复中文字符乱码问题
  - 确保 emoji 和特殊字符正确显示
- 📦 新增 DAO 类
  - CategoryDAO - 分类数据访问对象
  - UnitDAO - 单位数据访问对象
  - PromotionDAO - 促销数据访问对象
  - OperationLogDAO - 操作日志数据访问对象
  - RechargeRecordDAO - 充值记录数据访问对象
  - SystemSettingsDAO - 系统设置数据访问对象
  - ThemePreferenceDAO - 主题偏好数据访问对象

### v2.2.0 (2026-02-03)

**数据库迁移完成**
- ✨ 完整迁移到 MySQL 8.0 数据库
  - 所有核心功能使用 MySQL 数据持久化
  - 使用 HikariCP 连接池，高性能数据库访问
  - DAO 层封装：UserDAO, ProductDAO, MemberDAO, TransactionDAO, ShiftDAO
- 📊 支持通过 Docker Compose 一键启动 MySQL
- 📦 完整的数据备份和恢复方案
- 🛡️ 事务支持：确保数据一致性

### v2.1.0 (2026-01-08)

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

## 🚀 快速开始

### Windows 10 快速开始

专为 Windows 10 用户提供快速安装指南：

#### 方式一：使用一键安装脚本（推荐）

1. **双击运行 `install.bat`**
   - 自动检查 Java、Maven 环境
   - 下载依赖并编译项目
   - 创建必要目录和配置文件
   - 可选：创建桌面快捷方式

2. **安装 MySQL 数据库**
   - 查看 [Windows MySQL 安装指南](docs/WINDOWS_MYSQL_SETUP.md)
   - 或使用 Docker Desktop：双击运行 `docker/start-mysql.bat`

3. **配置数据库连接**
   - 编辑 `config/database.properties`
   - 修改数据库用户名和密码

4. **启动应用**
   - 双击运行 `start.bat`
   - 或使用桌面快捷方式

#### 方式二：使用便携版

1. 下载便携版 ZIP（如果已提供）
2. 解压到任意目录
3. 双击运行 `start.bat`
4. 首次运行会自动初始化

#### Windows 特性支持

- ✅ **一键启动** - 双击 `start.bat` 即可运行
- ✅ **桌面快捷方式** - 运行 `create-shortcut.bat` 创建快捷方式
- ✅ **高 DPI 支持** - 自动适配高分辨率显示器
- ✅ **打印机支持** - 完整的 Windows 打印机配置
- ✅ **性能优化** - 针对 Windows 10 优化的 JVM 参数
- ✅ **安装程序** - 使用 `package-windows.bat` 创建 MSI 安装包

详见 [Windows 使用文档](docs/WINDOWS_SETUP.md) (待创建)

### 环境要求

**必需组件**:
- **JDK**: Java 17 或更高版本
- **Maven**: 3.8 或更高版本
- **MySQL**: 8.0 或更高版本（通过 Docker 或本地安装）
- **Docker**: Docker & Docker Compose（推荐，用于快速启动 MySQL）
- **操作系统**: Windows、macOS、Linux
- **内存**: 最小 2GB，推荐 4GB+
- **磁盘**: 最小 500MB 可用空间（含数据库）

### 安装

1. 克隆仓库
```bash
git clone https://gitee.com/nevell/hello.git
cd hello
```

2. 启动 MySQL 数据库（三种方式）

**方式一：使用 Docker Compose（推荐）**
```bash
# 启动 MySQL 8.0
docker-compose up -d

# 查看日志
docker-compose logs -f mysql

# 数据库管理工具推荐：DBeaver
# 下载地址：https://dbeaver.io/download/
# 连接配置：
#   主机：localhost
#   端口：3306
#   数据库：cashier_system
#   用户名：root
#   密码：RootPassword123!
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

3. 配置数据库连接

创建 `config/database.properties`：
```properties
db.url=jdbc:mysql://localhost:3306/cashier_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
db.username=root
db.password=RootPassword123!
db.pool.size=10
```

4. 编译项目
```bash
mvn clean compile
```

5. 运行程序
```bash
mvn javafx:run
```

首次运行时，系统会自动检测并迁移数据到 MySQL。

**打包后运行**：
```bash
mvn clean package
java -jar target/cashier-system-fx-2.3.1.jar
```

### 默认账户

系统首次运行时会自动创建默认管理员账户：

- **用户名**: `admin`
- **密码**: `admin123`
- **角色**: 管理员

## 📖 使用说明

### 主要功能模块

1. **POS（收银台）**
   - 商品扫码或搜索添加到购物车
   - 完整的快捷键系统，大幅提升收银效率
     - F1 添加商品、Delete 移除商品、Ctrl+L 清空购物车
     - 双击商品快速添加、Ctrl+F 搜索、Ctrl+M 查询会员
     - F8 现金支付、Ctrl+1/2/3 微信/支付宝/银行卡支付
   - 修改商品数量
   - 会员信息查询和绑定
   - 多种支付方式选择
   - 现金找零自动计算
   - 结算后自动清除会员信息
   - 扫描枪自动扫码
   - 扫描音效反馈

2. **商品管理**
   - 商品添加、编辑、删除
   - 商品编号自动生成
   - 快速入库（采购入库、退回入库、调拨入库、盘盈入库等）
   - 商品搜索和筛选
   - 库存预警显示
   - 商品分类和单位管理
   - 条形码重复支持
   - 商品数据导入（CSV/GitHub）

3. **会员管理**
   - 会员注册
   - 会员编号自动生成
   - 积分查询和累计
   - 等级自动升级（普通→银卡→金卡→钻石）
   - 余额充值
   - 生日特权

4. **促销管理**
   - 满减活动
   - 折扣活动
   - 优惠券管理
   - 促销时间范围设置

5. **交易记录**
   - 完整交易历史
   - 按日期、支付方式筛选
   - 交易详情查看

6. **数据统计**
   - 销售额统计
   - 交易量统计
   - 平均客单价
   - 分类销售占比

7. **交接班管理**
   - 班次开始/结束
   - 班次收入统计
   - 多支付方式收入明细
   - 交接班记录查询

8. **用户管理**
   - 用户增删改查
   - 权限分配
   - 密码重置

9. **系统设置**
   - 主题切换（浅色/深色/IntelliJ）
   - 税率配置
   - 数据备份路径选择
   - 数据备份和恢复
   - 自动登出设置
   - 密码安全设置

10. **供应商管理**
    - 供应商信息添加、编辑、删除
    - 供应商分级（A级/B级/C级）
    - 供应商搜索和筛选
    - 供应商统计信息

11. **采购订单**
    - 创建采购订单
    - 选择供应商和采购日期
    - 添加商品到订单
    - 编辑商品数量和单价
    - 自动计算订单总金额
    - 提交审批

12. **采购审批**
    - 查看待审批订单列表
    - 审批通过或拒绝
    - 填写审批意见
    - 查看审批历史

13. **采购入库**
    - 选择待入库订单
    - 输入入库数量（支持部分入库）
    - 自动更新商品库存
    - 生成入库单
    - 查看入库历史

14. **库存盘点**
    - 创建盘点单
    - 选择盘点类型（全盘/部分盘点）
    - 添加盘点商品
    - 输入实际库存数量
    - 自动计算盘点差异
    - 完成盘点并调整库存

15. **采购报表**
    - 采购订单统计
    - 采购金额趋势分析（日/周/月）
    - 供应商采购排名
    - 分类采购统计
    - 订单状态统计

16. **库存报表**
    - 库存周转率分析
    - 滞销商品分析
    - 库存积压分析
    - 库存不足提醒
    - 可配置阈值设置

17. **利润分析**
    - 采购成本统计
    - 销售收入统计
    - 毛利润和毛利率分析
    - 净利润计算
    - 分类利润分析
    - 每日利润趋势

18. **打印机管理**
    - 打印设备管理
    - 打印任务队列
    - 打印预览
    - 打印模板定制
    - 打印历史记录

19. **扫描枪管理**
    - USB HID 扫描枪支持
    - 自动设备检测
    - 智能焦点管理
    - 扫描事件监听

20. **数据导入**
    - CSV 文件导入
    - GitHub 商品条码库导入
    - 自动分类和单位标准化
    - 导入统计和进度显示

### 快捷键

#### 主界面快捷键

| 功能键 | 功能 |
|--------|------|
| F1 | 添加商品 |
| F2 | 快速入库 |
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

| Ctrl 组合键 | 功能 |
|-------------|------|
| Ctrl+N | 添加商品 |
| Ctrl+S | 保存数据 |
| Ctrl+F | 搜索 |
| Ctrl+R | 刷新当前面板 |
| Ctrl+Q | 退出程序 |
| Ctrl+A | 全选 |
| Ctrl+B | 批量操作 |
| Ctrl+M | 会员管理 |
| Ctrl+T | 交易统计 |
| Ctrl+1~4 | 切换标签页 |

#### POS/结账页面快捷键

**商品操作**
- **F1** - 添加选中商品到购物车
- **Delete** - 移除购物车中选中的商品
- **Ctrl+L** - 清空购物车
- **双击商品** - 快速添加到购物车

**搜索和查询**
- **Ctrl+F** - 聚焦到搜索框
- **Enter** - 执行搜索（在搜索框中）
- **Escape** - 清空搜索（在搜索框中）

**会员操作**
- **Ctrl+M** - 聚焦到会员手机号框
- **Enter** - 查询会员（在会员手机号框中）
- **Escape** - 清空会员信息（在会员手机号框中）

**支付方式**
- **F8** - 现金支付
- **Ctrl+1** - 微信支付
- **Ctrl+2** - 支付宝支付
- **Ctrl+3** - 银行卡支付

**帮助**
- **Ctrl+/** - 显示快捷键帮助对话框

#### 扫描枪快捷操作

- **扫描商品条形码** - 自动添加到购物车
- **扫描会员二维码** - 自动绑定会员
- **扫描商品码** - 自动聚焦到搜索框并查询商品

## 📁 项目结构

```
hello/
├── pom.xml                          # Maven 配置文件
├── src/
│   └── main/
│       ├── java/com/cashier/
│       │   ├── CashierSystemFXApplication.java  # 主程序入口
│       │   ├── constant/
│       │   │   ├── FXConstants.java             # JavaFX 常量
│       │   │   └── SpacingConstants.java        # 间距常量
│       │   ├── dao/                            # 数据访问层 (20个)
│       │   │   ├── UserDAO.java                # 用户 DAO
│       │   │   ├── ProductDAO.java             # 商品 DAO
│       │   │   ├── MemberDAO.java              # 会员 DAO
│       │   │   ├── TransactionDAO.java         # 交易 DAO
│       │   │   ├── ShiftDAO.java               # 交接班 DAO
│       │   │   ├── CategoryDAO.java            # 分类 DAO
│       │   │   ├── UnitDAO.java                # 单位 DAO
│       │   │   ├── PromotionDAO.java           # 促销 DAO
│       │   │   ├── OperationLogDAO.java        # 操作日志 DAO
│       │   │   ├── RechargeRecordDAO.java      # 充值记录 DAO
│       │   │   ├── SystemSettingsDAO.java      # 系统设置 DAO
│       │   │   ├── ThemePreferenceDAO.java     # 主题偏好 DAO
│       │   │   ├── SupplierDAO.java            # 供应商 DAO
│       │   │   ├── PurchaseOrderDAO.java       # 采购订单 DAO
│       │   │   ├── PurchaseOrderItemDAO.java   # 采购订单明细 DAO
│       │   │   ├── PurchaseApprovalDAO.java    # 采购审批 DAO
│       │   │   ├── PurchaseInboundDAO.java     # 采购入库 DAO
│       │   │   ├── PurchaseInboundItemDAO.java # 采购入库明细 DAO
│       │   │   ├── InventoryCheckDAO.java      # 库存盘点 DAO
│       │   │   └── InventoryCheckItemDAO.java  # 库存盘点明细 DAO
│       │   ├── controller/                     # 控制器层 (26个)
│       │   │   ├── CartController.java          # 购物车控制器
│       │   │   ├── CheckoutController.java      # 结账控制器
│       │   │   ├── InventoryController.java     # 商品管理控制器
│       │   │   ├── MemberController.java        # 会员管理控制器
│       │   │   ├── PromotionController.java     # 促销管理控制器
│       │   │   ├── TransactionController.java   # 交易记录控制器
│       │   │   ├── StatisticsController.java    # 数据统计控制器
│       │   │   ├── ShiftController.java         # 交接班控制器
│       │   │   ├── UserController.java          # 用户管理控制器
│       │   │   ├── SettingsController.java      # 系统设置控制器
│       │   │   ├── LoginController.java         # 登录控制器
│       │   │   ├── MainController.java          # 主界面控制器
│       │   │   ├── MemberEditController.java    # 会员编辑控制器
│       │   │   ├── ProductEditController.java   # 商品编辑控制器
│       │   │   ├── RechargeController.java      # 充值控制器
│       │   │   ├── RestockController.java       # 补货控制器
│       │   │   ├── PasswordResetController.java # 密码重置控制器
│       │   │   ├── SupplierController.java     # 供应商管理控制器
│       │   │   ├── PurchaseOrderController.java # 采购订单控制器
│       │   │   ├── PurchaseApprovalController.java # 采购审批控制器
│       │   │   ├── PurchaseInboundController.java # 采购入库控制器
│       │   │   ├── InventoryCheckController.java # 库存盘点控制器
│       │   │   ├── PurchaseReportController.java # 采购报表控制器
│       │   │   ├── InventoryReportController.java # 库存报表控制器
│       │   │   ├── ProfitReportController.java # 利润分析控制器
│       │   │   └── PosModeController.java      # POS模式控制器 (v2.3.1 新增)
│       │   ├── model/                          # 实体类 (20个)
│       │   │   ├── Product.java                 # 商品实体类
│       │   │   ├── Member.java                  # 会员实体类
│       │   │   ├── User.java                    # 用户实体类
│       │   │   ├── Transaction.java             # 交易实体类
│       │   │   ├── Category.java                # 分类实体类
│       │   │   ├── Unit.java                    # 单位实体类
│       │   │   ├── Promotion.java               # 促销实体类
│       │   │   ├── RechargeRecord.java          # 充值记录类
│       │   │   ├── OperationLog.java            # 操作日志类
│       │   │   ├── Shift.java                   # 交接班记录类
│       │   │   ├── CartItem.java                # 购物车项类
│       │   │   ├── Supplier.java                # 供应商实体类
│       │   │   ├── PurchaseOrder.java           # 采购订单实体类
│       │   │   ├── PurchaseOrderItem.java       # 采购订单明细类
│       │   │   ├── PurchaseApproval.java        # 采购审批记录类
│       │   │   ├── PurchaseInbound.java         # 采购入库记录类
│       │   │   ├── PurchaseInboundItem.java     # 采购入库明细类
│       │   │   ├── InventoryCheck.java          # 库存盘点实体类
│       │   │   └── InventoryCheckItem.java      # 库存盘点明细类
│       │   ├── service/                        # 服务层 (4个)
│       │   │   ├── DataService.java            # 数据服务
│       │   │   ├── InventoryService.java       # 库存服务 (v2.3.1 新增)
│       │   │   ├── MemberService.java          # 会员服务 (v2.3.1 新增)
│       │   │   └── TransactionService.java     # 交易服务 (v2.3.1 新增)
│       │   ├── printer/                        # 打印机管理模块 (10个类)
│       │   │   ├── PrinterManager.java         # 打印机管理器
│       │   │   ├── PrinterDevice.java          # 打印设备
│       │   │   ├── PrinterStatus.java          # 打印机状态
│       │   │   ├── PrinterDeviceStatus.java    # 设备状态枚举
│       │   │   ├── PrinterDeviceType.java      # 设备类型枚举
│       │   │   ├── PrintTask.java              # 打印任务
│       │   │   ├── PrintTaskType.java          # 任务类型枚举
│       │   │   ├── PrintTemplate.java          # 打印模板
│       │   │   ├── PrintUtil.java              # 打印工具类
│       │   │   └── PrintPreviewDialog.java     # 打印预览对话框
│       │   ├── scanner/                        # 扫描枪管理模块 (11个类)
│       │   │   ├── ScannerManager.java         # 扫描枪管理器
│       │   │   ├── ScannerDevice.java          # 扫描设备
│       │   │   ├── ScannerDeviceStatus.java    # 设备状态枚举
│       │   │   ├── ScannerDeviceType.java      # 设备类型枚举
│       │   │   ├── USBHIDScannerDevice.java    # USB HID 扫描设备实现
│       │   │   ├── ScanEvent.java              # 扫描事件
│       │   │   ├── ScanListener.java           # 扫描监听器接口
│       │   │   ├── ScanDataType.java           # 扫描数据类型枚举
│       │   │   ├── FocusManager.java           # 焦点管理器
│       │   │   └── FocusTarget.java            # 焦点目标接口
│       │   └── util/                           # 工具类 (9个)
│       │       ├── DatabaseManager.java        # 数据库管理器
│       │       ├── PasswordUtil.java           # 密码工具
│       │       ├── FXUtils.java               # JavaFX 工具类
│       │       ├── FXMLUtils.java             # FXML 工具类
│       │       ├── LoggerFactoryUtil.java      # 日志工厂工具
│       │       ├── StatusBarManager.java       # 状态栏管理器
│       │       ├── ReceiptPrinter.java         # 收据打印机
│       │       ├── CacheManager.java           # 缓存管理器 (v2.3.1 新增)
│       │       └── ProductDataImporter.java    # 商品数据导入工具 (v2.3.1 新增)
│       └── resources/
│           ├── com/cashier/view/               # FXML 视图文件 (26个)
│           │   ├── MainView.fxml              # 主界面
│           │   ├── LoginView.fxml             # 登录界面
│           │   ├── CartView.fxml              # 购物车界面
│           │   ├── CheckoutView.fxml          # 结账界面
│           │   ├── InventoryView.fxml         # 商品管理界面
│           │   ├── MemberView.fxml            # 会员管理界面
│           │   ├── PromotionView.fxml         # 促销管理界面
│           │   ├── TransactionView.fxml       # 交易记录界面
│           │   ├── StatisticsView.fxml        # 数据统计界面
│           │   ├── ShiftView.fxml             # 交接班界面
│           │   ├── UserView.fxml              # 用户管理界面
│           │   ├── SettingsView.fxml          # 系统设置界面
│           │   ├── MemberEditView.fxml        # 会员编辑界面
│           │   ├── ProductEditView.fxml       # 商品编辑界面
│           │   ├── RechargeView.fxml          # 充值界面
│           │   ├── RestockView.fxml           # 补货界面
│           │   ├── PasswordResetView.fxml     # 密码重置界面
│           │   ├── SupplierView.fxml          # 供应商管理界面
│           │   ├── PurchaseOrderView.fxml     # 采购订单界面
│           │   ├── PurchaseApprovalView.fxml  # 采购审批界面
│           │   ├── PurchaseInboundView.fxml   # 采购入库界面
│           │   ├── InventoryCheckView.fxml    # 库存盘点界面
│           │   ├── PurchaseReportView.fxml    # 采购报表界面
│           │   ├── InventoryReportView.fxml   # 库存报表界面
│           │   ├── ProfitReportView.fxml      # 利润分析界面
│           │   └── PosModeView.fxml          # POS模式界面 (v2.3.1 新增)
│           ├── css/                            # 样式文件
│           │   ├── styles.css                 # 主样式文件
│           │   ├── light-theme.css            # 浅色主题
│           │   ├── dark-theme.css             # 深色主题
│           │   └── intellij-theme.css         # IntelliJ主题
│           ├── sounds/                         # 音效文件 (v2.3.1 新增)
│           │   ├── scan_error.wav             # 扫描错误音效
│           │   ├── scan_not_found.wav         # 扫描未找到音效
│           │   └── scan_success.wav           # 扫描成功音效
│           └── images/                         # 图片资源 (v2.3.1 新增)
│               └── logos/                      # Logo 目录
├── config/                          # 配置目录
│   ├── database.properties            # 数据库配置
│   └── database.properties.example  # 数据库配置示例
├── docker/                          # Docker 配置
│   └── mysql-init/                   # MySQL 初始化脚本
│       ├── 00-init-complete.sql      # 完整初始化脚本
│       ├── 01-create-user.sql       # 创建数据库用户
│       ├── 02-alter-tables.sql      # 表结构升级脚本
│       ├── 03-sample-data.sql       # 示例数据脚本
│       └── 04-v2.3.1-updates.sql    # v2.3.1 升级脚本
├── docker-compose.yml               # Docker Compose 配置
├── README.md                        # 项目说明（本文件）
├── LICENSE                          # 木兰宽松许可证 v2
└── docs/
    ├── DATABASE_CHANGES_v2.3.1.md  # v2.3.1 数据库变更文档
    ├── DATABASE_INIT.md             # 数据库初始化文档
    ├── MYSQL_SETUP.md               # MySQL 部署指南
    ├── PURCHASE_TABLE_DESIGN.md     # 采购表结构设计
    ├── WINDOWS_MYSQL_SETUP.md       # Windows MySQL 安装指南
    └── ICON_GUIDE.md                # 应用图标指南
```

## 🛠️ 开发

### 使用 IntelliJ IDEA

1. 打开 IntelliJ IDEA
2. 选择 "File" -> "Open"
3. 选择项目根目录（包含 pom.xml 的目录）
4. IntelliJ IDEA 会自动识别 Maven 项目
5. 等待依赖下载完成
6. 右键点击 `CashierSystemFXApplication.java`
7. 选择 "Run" 或 "Debug"

### 使用命令行

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn javafx:run

# 打包项目
mvn clean package

# 运行打包后的 JAR
java -jar target/cashier-system-fx-2.3.1.jar

# 运行测试
mvn test
```

### Maven 依赖

项目主要依赖：
- JavaFX 17.0.8 - 图形界面框架
- MySQL Connector J 8.3.0 - MySQL JDBC 驱动
- HikariCP 5.1.0 - 高性能 JDBC 连接池
- ControlsFX 11.2.1 - 增强的 UI 控件
- FontAwesomeFX 4.7.0-9.1.2 - 图标库
- JUnit 5.10.0 - 单元测试框架
- TestFX 4.0.18 - JavaFX UI 测试框架
- H2 Database 2.2.224 - 测试用内存数据库
- BCrypt 0.10.2 - 密码加密库

### 开发工具配置

**IntelliJ IDEA 配置**：
1. 确保安装了 JavaFX 插件
2. 配置 JDK 17
3. 启用注解处理
4. 配置数据库连接（可选）

**VS Code 配置**：
1. 安装 "Extension Pack for Java" 插件
2. 配置 settings.json 中的 Java 路径
3. 安装 "Scene Builder" 插件用于 FXML 可视化编辑

## 🎨 主题切换

系统支持三种主题：

- **浅色主题 (Light)**: 明亮清爽的界面，适合白天使用
- **深色主题 (Dark)**: 护眼的深色界面，适合夜间使用
- **IntelliJ主题**: 类似IDE的专业风格

在系统设置中切换主题，实时生效。

## 🔒 权限系统

系统支持三种角色：

- **管理员 (admin)**: 完整的系统管理权限
  - 用户管理
  - 系统设置
  - 数据备份/恢复
  - 所有功能访问权限

- **收银员 (cashier)**: 日常收银操作权限
  - POS收银
  - 库存查看
  - 会员查询
  - 交易记录查看

- **财务 (finance)**: 财务报表和数据统计权限
  - 数据统计
  - 交易记录
  - 数据导出
  - 报表查看

## 💾 数据库架构

### 数据存储策略

系统采用 **MySQL 8.0 数据库** 作为唯一数据存储方案：

- 使用 HikariCP 连接池，高性能数据库访问
- 完整的 DAO 层封装，提供类型安全的数据库操作
- 事务支持，确保数据一致性
- 自动创建表结构和索引
- 支持数据备份和恢复功能
- 内置缓存管理器，减少数据库查询

### 数据库表结构

**核心表**：
- `users` - 用户账户（管理员、收银员、财务）
- `products` - 商品库存信息（支持重复条形码）
- `members` - 会员账户和积分（自动生成会员编号）
- `transactions` - 交易记录主表
- `transaction_items` - 交易明细（商品列表）
- `shifts` - 交接班记录

**辅助表**：
- `categories` - 商品分类
- `units` - 计量单位
- `promotions` - 促销活动
- `operation_logs` - 操作日志
- `recharge_records` - 充值记录
- `system_settings` - 系统设置
- `theme_preferences` - 主题偏好

**采购管理表**：
- `suppliers` - 供应商信息
- `purchase_orders` - 采购订单
- `purchase_order_items` - 采购订单明细
- `purchase_approvals` - 采购审批记录
- `purchase_inbound` - 采购入库记录
- `purchase_inbound_items` - 采购入库明细

**库存管理表**：
- `inventory_check` - 库存盘点
- `inventory_check_items` - 库存盘点明细

### 数据备份

**自动备份**（推荐）：
```bash
# 使用 cron 定期备份
0 2 * * * mysqldump -u root -p cashier_system > /backup/cashier_$(date +\%Y\%m\%d).sql
```

**手动备份**：
```bash
# 使用 mysqldump
mysqldump -u root -p cashier_system > backup.sql

# 或使用应用内的"数据备份"功能
```

**数据恢复**：
```bash
mysql -u root -p cashier_system < backup.sql
```

## 💡 特色功能

### 快捷键系统
- **全面的快捷键支持** - 覆盖所有常用操作
  - 商品操作：添加、删除、清空购物车
  - 搜索查询：商品搜索、会员查询
  - 支付方式：现金、微信、支付宝、银行卡
  - 导航操作：切换标签页、聚焦输入框
- **智能快捷键提示** - 所有按钮显示对应的快捷键
- **快捷键帮助** - 按 Ctrl+/ 随时查看所有快捷键
- **提高收银效率** - 减少鼠标操作，提升工作效率

### 会员系统
- 会员等级自动升级（普通→银卡→金卡→钻石）
  - 普通会员：0-1999积分
  - 银卡会员：2000-4999积分（9.5折）
  - 金卡会员：5000-9999积分（9折）
  - 钻石会员：10000+积分（8.5折）
- 积分累计和折扣优惠
- 会员余额充值功能
- 会员编号自动生成
- 生日特权检测（生日当天可能享受额外优惠）

### 促销管理
- 支持多种促销类型
  - **满减**: 满足金额后减免固定金额
  - **打折**: 满足金额后享受折扣
  - **优惠券**: 直接使用优惠券抵扣
- 促销活动时间范围控制
- 促销启用/禁用管理
- 促销使用次数限制

### 交接班管理
- 班次开始/结束记录
- 班次收入统计
- 多种支付方式收入统计
  - 现金收入
  - 微信收入
  - 支付宝收入
  - 银行卡收入
- 班次时长计算

### 数据安全
- 数据自动备份功能
  - 支持自定义备份路径
  - 自动生成带时间戳的备份目录
- 操作日志完整记录
  - 记录所有关键操作
  - 包含操作时间、操作人、操作详情
- 数据恢复支持
  - 从备份目录选择恢复
  - 按时间排序显示备份列表
- 密码 BCrypt 加密存储

### 硬件支持
- **打印机管理**
  - 支持多种打印机类型
  - 打印任务队列
  - 打印预览功能
  - 打印模板定制
- **扫描枪管理**
  - USB HID 扫描枪支持
  - 自动设备检测
  - 智能焦点管理
  - 扫描音效反馈

### 数据导入
- **CSV 文件导入**
  - 支持逗号分隔和 | 分隔
  - 自动检测文件格式
  - 自动标准化单位和分类
- **GitHub 集成**
  - 支持 GitHub 商品条码库
  - 一键导入大量商品数据
- **智能处理**
  - 自动创建缺失的分类和单位
  - 跳过已存在的商品
  - 实时显示导入进度

### 缓存管理
- **商品数据缓存**
  - 5分钟过期时间
  - 多维度缓存（ID、名称、条形码）
  - 自动缓存刷新
  - 批量更新后自动清除
- **缓存预热**
  - 应用启动时预热缓存
  - 减少首次加载时间
- **性能优化**
  - 减少数据库查询次数
  - 提升系统响应速度

## 🔧 Windows 故障排除

### 应用无法启动

**问题**: 双击 `start.bat` 后闪退或无法启动

**解决方案**:
1. 打开命令提示符（cmd）
2. 进入项目目录
3. 手动运行 `start.bat` 查看错误信息
4. 检查是否已安装 JDK 17 或更高版本

### Java 版本不兼容

**问题**: 提示 Java 版本过低

**解决方案**:
1. 下载并安装 JDK 17: https://www.oracle.com/java/technologies/downloads/
2. 设置 JAVA_HOME 环境变量
3. 将 JDK 的 bin 目录添加到 PATH

### MySQL 连接失败

**问题**: 应用无法连接到 MySQL 数据库

**解决方案**:
1. 检查 MySQL 服务是否运行
   - 打开 "服务"（services.msc）
   - 确保 MySQL80 服务正在运行
2. 检查防火墙设置
   - 允许 MySQL 端口 3306 通过
3. 检查 `config/database.properties` 配置是否正确
4. 详见 [Windows MySQL 安装指南](docs/WINDOWS_MYSQL_SETUP.md)

### 扫描枪无法工作

**问题**: 扫描枪扫描后没有反应

**解决方案**:
1. 确认扫描枪已正确连接（USB 接口）
2. 确认扫描枪处于 HID 模式
3. 检查应用是否启动扫描枪管理器
4. 确认焦点在正确的输入框
5. 查看应用日志获取详细错误信息

### 打印机无法打印小票

**问题**: 小票打印功能无法正常工作

**解决方案**:
1. 检查打印机是否正确安装
2. 配置 `config/printer.properties` 文件
3. 检查打印机驱动是否正常
4. 使用打印预览功能测试
5. 尝试使用不同的打印命令

### 界面模糊或显示异常

**问题**: 在高分辨率显示器上界面模糊

**解决方案**:
1. 检查 `config/jvm.config` 中的 DPI 设置
2. 确保 `-Dsun.java2d.dpiaware=true` 已启用
3. 在 Windows 显示设置中调整缩放比例
4. 重启应用

### 打包失败

**问题**: 运行 `package-windows.bat` 打包失败

**解决方案**:
1. 确保使用完整 JDK（非 JRE）
2. 检查 jpackage 工具是否可用（JDK 14+）
3. 确保已成功编译项目（`mvn clean package`）
4. 检查是否有足够的磁盘空间

## 📝 许可证

本项目采用 **木兰宽松许可证 v2 (MulanPSL2)**

您可以根据木兰宽松许可证 v2 的条款和条件使用本软件。

获取木兰宽松许可证 v2 副本：
- 中文：http://license.coscl.org.cn/MulanPSL2

### JavaFX 许可证

JavaFX 使用 GPL v2 with Classpath Exception 许可证
JavaFX 官网: https://openjfx.io/

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📧 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue: https://gitee.com/nevell/hello/issues

## 🙏 致谢

- [JavaFX](https://openjfx.io/) - 现代化的 Java GUI 框架
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) - 强大的 Java IDE
- [Maven](https://maven.apache.org/) - 项目管理和构建工具
- [MySQL](https://www.mysql.com/) - 世界最流行的开源数据库
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - 快速的 JDBC 连接池

## 📚 相关文档

- [AGENTS.md](AGENTS.md) - 项目开发指南
- [CLAUDE.md](CLAUDE.md) - 给 AI 助手的代码库指南
- [MySQL 数据库部署指南](docs/MYSQL_SETUP.md) - 详细的 MySQL 安装和配置说明
- [Docker MySQL 快速部署](docker-mysql-setup.md) - 使用 Docker 快速启动 MySQL
- [数据库初始化文档](docs/DATABASE_INIT.md) - 数据库表结构和初始化流程
- [采购表结构设计](docs/PURCHASE_TABLE_DESIGN.md) - 采购管理表结构设计
- [Windows MySQL 安装指南](docs/WINDOWS_MYSQL_SETUP.md) - Windows 平台 MySQL 安装
- [v2.3.1 数据库变更](docs/DATABASE_CHANGES_v2.3.1.md) - 数据库变更说明
- [应用图标指南](docs/ICON_GUIDE.md) - 应用图标使用指南

## 📊 技术栈

- **前端框架**: JavaFX 17.0.8
- **构建工具**: Maven 3.8+
- **编程语言**: Java 17
- **UI设计**: FXML + CSS
- **数据库**: MySQL 8.0
- **连接池**: HikariCP 5.1.0
- **ORM**: 自定义 DAO 层
- **缓存**: 内置缓存管理器
- **测试框架**: JUnit 5 + TestFX + H2 Database

## 🔮 未来计划

- [ ] 云端数据同步
- [ ] 移动端应用
- [ ] 二维码生成和打印
- [x] 小票打印功能（已实现）
- [ ] 更多统计图表
- [ ] 多语言支持
- [ ] 短信通知功能
- [x] 完整的快捷键系统（v2.1.0 已实现）
- [x] UI 优化和菜单结构调整（v2.1.0 已实现）
- [x] 退出登录功能修复（v2.1.0 已实现）
- [x] 数据库支持 MySQL（v2.2.0 已实现）
- [x] 交接班管理数据库集成（v2.2.0 已实现）
- [x] 现金支付页面 UI 优化（v2.2.1 已实现）
- [x] 模型 ID 字段统一管理（v2.2.1 已实现）
- [x] 数据库初始化脚本完善（v2.2.1 已实现）
- [x] 字符编码修复（v2.2.1 已实现）
- [x] 商品管理功能优化（v2.3.1 已实现）
- [x] 商品编号自动生成（v2.3.1 已实现）
- [x] 快速入库功能（v2.3.1 已实现）
- [x] 会员编号自动迁移（v2.3.1 已实现）
- [x] 商品管理列表选中效果优化（v2.3.1 已实现）
- [x] 采购管理功能（v2.3.0 已实现）
- [x] 库存盘点功能（v2.3.0 已实现）
- [x] 报表统计功能（v2.3.0 已实现）
- [x] 打印机管理功能（v2.3.1 已实现）
- [x] 扫描枪管理功能（v2.3.1 已实现）
- [x] 缓存管理功能（v2.3.1 已实现）
- [x] 数据导入功能（v2.3.1 已实现）
- [x] Service 层封装（v2.3.1 已实现）
- [x] 单元测试支持（v2.3.1 已实现）
- [ ] 退货管理功能
- [ ] 数据导出功能（Excel/PDF）
- [ ] 多仓库管理
- [ ] 操作日志功能增强
- [ ] 消息通知功能
- [ ] 性能优化
- [ ] 支持更多打印机型号
- [ ] 支持蓝牙扫描枪
- [ ] 支持云打印服务
- [ ] 支持多语言界面

---

**注意**: 本项目仅供学习和参考使用，不建议直接用于生产环境。如需用于生产环境，请进行充分的测试和安全加固。