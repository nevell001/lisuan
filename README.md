# 狸算(LiSuan)收银系统

狸算(LiSuan)收银系统 - 功能完整的收银系统，基于 JavaFX 17 开发。

**当前版本**: v2.5.6 | **最新更新**: 2026-06-10

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

## 核心功能

### 🛒 POS 收银
- 购物车管理（添加、修改、删除商品）
- 多种支付方式：现金、微信支付、支付宝、银行卡
- 会员折扣自动计算
- 挂单/取单功能
- 小票打印（支持多种打印机）
- 快捷键操作支持

### 📦 商品管理
- 商品增删改查
- SKU/条码管理
- 规格管理（颜色、尺寸、材质等）
- 分类管理
- 库存预警
- 快速入库
- 批量导入/导出

### 👥 会员管理
- 会员注册与信息管理
- 积分系统（消费累计积分）
- 等级自动升级（普通/银卡/金卡/钻石）
- 会员折扣（无折扣/5%/10%/15%）
- 余额充值与消费
- 充值记录查询

### 📊 进销存管理
- 采购订单管理
- 采购入库
- 供应商管理
- 库存盘点
- 利润分析报表
- 采购报表统计

### 🔄 退货管理
- 退货订单创建
- 退货审批流程
- 库存自动恢复
- 退款处理（现金/余额/积分）

### 📈 数据统计
- 日销售报表
- 月销售报表
- 交易记录查询
- 利润分析报表
- 库存报表
- 采购报表
- 退货报表

### 👤 用户权限
- 三种角色：管理员、收银员、财务
- 角色权限控制
- 操作日志记录
- 密码安全（BCrypt 加密）
- 密码重置功能

### 💾 数据管理
- Excel 导出（Apache POI）
- PDF 导出（PDFBox）
- 数据批量导入
- 云备份系统（支持多云存储）
- 本地备份恢复
- 自动定时备份

### 🔌 REST API
- 60+ REST 端点
- Token 身份验证（24小时有效期）
- WebSocket 多终端实时同步
- 支持第三方集成
- API 文档

### 🖨️ 硬件支持
- 热敏打印机（ESC/POS）
- 网络打印
- 打印预览
- USB 扫描枪（HID 模式）
- 打印队列管理

### 🌐 国际化 (i18n)
- 多语言支持：简体中文、英文、日语、韩语
- 货币国际化
- 动态语言切换

### 🎨 主题系统
- 三种主题：浅色、深色、IntelliJ
- 用户偏好持久化
- 字号调整功能

### 🔄 交接班管理
- 交接班记录
- 班次销售额统计
- 收银员业绩统计

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+（开发需要）
- MySQL 8.4+

### Windows 用户

1. 下载 `CashierSystem-v{version}.zip` 并解压
2. 双击 `Database Config.bat` 配置数据库
3. 双击 `Quick Start.bat` 启动应用

### 开发者安装

```bash
# 克隆仓库
git clone https://gitee.com/nevell/lisuan.git
cd lisuan

# 启动 MySQL（Docker）
docker compose up -d mysql

# 运行
mvn javafx:run

# 打包
mvn clean package
java -jar target/cashier-system-fx-*-jar-with-dependencies.jar
```

### 默认账户
- 用户名: `admin`
- 初始密码: `admin123`（首次登录后建议修改）

## 快捷键

| 快捷键 | 功能 |
|--------|------|
| `F1` | 添加商品到购物车 |
| `Delete` | 移除选中商品 |
| `Ctrl+L` | 清空购物车 |
| `F8` | 现金支付 |
| `Ctrl+1` | 微信支付 |
| `Ctrl+2` | 支付宝支付 |
| `Ctrl+3` | 银行卡支付 |
| `Ctrl+F` | 聚焦搜索框 |
| `Ctrl+M` | 聚焦会员手机号 |
| `Ctrl+/` | 显示快捷键帮助 |

## 会员等级

| 等级 | 积分范围 | 折扣 |
|------|----------|------|
| 普通会员 | 0-1999 | 无折扣 (10.0) |
| 银卡会员 | 2000-4999 | 9.5折 |
| 金卡会员 | 5000-9999 | 9折 |
| 钻石会员 | 10000+ | 8.5折 |

## 最近更新

### v2.5.6 (2026-06-10)
- 品牌名称统一为"狸算(LiSuan)收银系统"
- 更新所有用户界面和文档中的品牌信息
- 更新 Docker 容器名称为 lisuan-mysql
- 小票打印和图标中的品牌信息更新

### v2.5.5 (2026-06-09)
- 标签页宽度优化（减小约 50%）
- 标签关闭按钮视觉优化
- 新增字号调整功能
- fcitx5 输入法兼容性修复
- 繁体中文显示优化
- 多平台字体回退链增强

### v2.5.4 (2026-05-21)
- GUI 数据库配置工具
- Windows 分发包优化
- 安装脚本改进

### v2.5.3 (2026-05-15)
- Windows 平台优化
- 启动体验增强（Splash 画面）
- 同步逻辑增强
- DAO 重构完成
- 资源清理机制（修复内存泄漏）

### v2.5.2 (2026-05-10)
- UI/UX 优化
- 功能增强

### v2.5.1 (2026-05-05)
- 双语界面支持
- 货币国际化

### v2.5.0 (2026-05-01)
- 多语言支持（简体中文、英文、日语、韩语）
- 云备份系统（多云存储支持）
- 电子支付系统（微信/支付宝）
- 网络打印系统（ESC/POS）
- 电子发票系统
- WebSocket 多终端同步
- REST API（60+ 端点）

## 技术栈

- **前端**: JavaFX 17.0.12
- **语言**: Java 17
- **数据库**: MySQL 8.4 + HikariCP 连接池
- **构建**: Maven 3.8+
- **测试**: JUnit 5 + TestFX + H2
- **API 服务**: Javalin 6.1.3
- **序列化**: Jackson
- **文档**: Apache POI 5.2.5 + PDFBox 2.0.32
- **日志**: SLF4J + Logback

## REST API

服务器运行在端口 8080，提供 60+ REST 端点：

- **认证** (`/api/v1/auth/*`) - 登录、令牌刷新、登出
- **商品** (`/api/v1/products/*`) - 商品 CRUD、搜索
- **会员** (`/api/v1/members/*`) - 会员 CRUD、充值、查询
- **交易** (`/api/v1/transactions/*`) - 交易处理、统计
- **库存** (`/api/v1/inventory/*`) - 库存更新、预警
- **报表** (`/api/v1/reports/*`) - 日报、月报、销售报表
- **支付** (`/api/v1/payments/*`) - 电子支付
- **发票** (`/api/v1/invoices/*`) - 发票管理
- **打印** (`/api/v1/print/*`) - 网络打印
- **备份** (`/api/v1/backup/*`) - 云备份
- **国际化** (`/api/v1/i18n/*`) - 多语言支持
- **用户** (`/api/v1/users/*`) - 用户管理（管理员）
- **设置** (`/api/v1/settings/*`) - 系统设置
- **健康** (`/api/v1/health`) - 健康检查（无需认证）

## 数据库架构

主要数据表：
- `products` - 商品信息（名称唯一约束）
- `specifications` - 规格类型
- `specification_values` - 规格值
- `product_specifications` - 商品规格关联
- `members` - 会员信息（会员号唯一）
- `transactions` - 交易记录
- `transaction_items` - 交易明细
- `returns` - 退货订单
- `return_items` - 退货明细
- `purchase_orders` - 采购订单
- `purchase_inbound` - 采购入库
- `suppliers` - 供应商
- `users` - 用户（三种角色）
- `shifts` - 交接班记录
- `invoices` - 发票
- `payment_records` - 支付记录
- `operation_logs` - 操作日志

## 故障排除

**应用无法启动**
- 检查 JDK 版本是否为 17+
- 检查 MySQL 服务是否运行
- 查看 `logs/app.log`

**数据库连接失败**
- 确保 MySQL 正在运行
- 检查数据库用户名和密码
- 使用 `Database Config.bat` / `Database Config.sh` 重新配置

**扫描枪无法工作**
- 确认扫描枪已正确连接（USB）
- 确认扫描枪处于 HID 模式

**打印无响应**
- 检查打印机是否正确连接
- 确认打印队列状态
- 尝试重启打印服务

**中文显示异常**
- Linux: 安装中文字体包
- Windows/macOS: 系统自带支持

## 许可证

木兰宽松许可证 v2 (MulanPSL2)

---

**仓库**: https://gitee.com/nevell/lisuan.git
**问题反馈**: https://gitee.com/nevell/lisuan/issues
