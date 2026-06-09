# 收银系统 (Cashier System)

功能完整的收银系统，基于 JavaFX 17 开发。

**当前版本**: v2.5.5 | **最新更新**: 2026-06-09

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

## 核心功能

- **POS收银** - 购物车、结账、多种支付方式（现金/微信/支付宝/银行卡）
- **商品管理** - 商品增删改查、库存管理、快速入库、库存预警
- **会员管理** - 会员注册、积分、等级自动升级、折扣、余额充值
- **进销存** - 采购管理、库存盘点、利润分析
- **退货管理** - 退货订单、审批流程、库存恢复
- **数据统计** - 销售报表、交易记录、交接班管理
- **用户权限** - 三种角色（管理员/收银员/财务）、操作日志
- **数据管理** - Excel/PDF 导出、数据导入/导出、备份恢复
- **REST API** - 60+ 端点，支持多终端同步
- **硬件支持** - 打印机、扫描枪

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
git clone https://gitee.com/nevell/hello.git
cd hello

# 启动 MySQL（Docker）
docker compose up -d mysql

# 运行
mvn javafx:run

# 打包
mvn clean package
java -jar target/cashier-system-fx-2.5.5-jar-with-dependencies.jar
```

### 默认账户
- 用户名: `admin`
- 初始密码: 首次启动时自动生成（查看 `logs/app.log`）

## 快捷键

| 快捷键 | 功能 |
|--------|------|
| `F1` | 添加商品到购物车 |
| `Delete` | 移除选中商品 |
| `Ctrl+L` | 清空购物车 |
| `F8` | 现金支付 |
| `Ctrl+1/2/3` | 微信/支付宝/银行卡支付 |
| `Ctrl+F` | 聚焦搜索框 |
| `Ctrl+M` | 聚焦会员手机号 |

## 会员等级

| 等级 | 积分范围 | 折扣 |
|------|----------|------|
| 普通会员 | 0-999 | 无折扣 |
| 银卡会员 | 1000-4999 | 9.5折 |
| 金卡会员 | 5000-9999 | 9折 |
| 钻石会员 | 10000+ | 8.5折 |

## 最近更新

### v2.5.5 (2026-06-09)
- 标签页宽度优化
- 标签关闭按钮视觉优化
- 新增字号调整功能
- fcitx5 输入法兼容性修复

### v2.5.4 (2026-05-21)
- GUI 数据库配置工具
- Windows 分发包优化

## 技术栈

- **前端**: JavaFX 17.0.12
- **语言**: Java 17
- **数据库**: MySQL 8.4 + HikariCP
- **构建**: Maven 3.8+
- **测试**: JUnit 5 + TestFX

## 故障排除

**应用无法启动**
- 检查 JDK 版本是否为 17+
- 检查 MySQL 服务是否运行
- 查看 `logs/app.log`

**数据库连接失败**
- 确保 MySQL 正在运行
- 检查数据库用户名和密码
- 使用 `Database Config.bat` 重新配置

**扫描枪无法工作**
- 确认扫描枪已正确连接（USB）
- 确认扫描枪处于 HID 模式

## 许可证

木兰宽松许可证 v2 (MulanPSL2)

---

**仓库**: https://gitee.com/nevell/hello.git
**问题反馈**: https://gitee.com/nevell/hello/issues
