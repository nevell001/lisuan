# 狸算(LiSuan)收银系统

简体中文 | [English](./README_en.md) | [繁體中文](./README_zh_TW.md)

狸算(LiSuan)收银系统是一个基于 JavaFX 17 的桌面 POS 收银系统，面向零售门店的收银、商品、会员、采购、库存、退货、报表、用户权限、数据备份和硬件接入等日常经营场景。

**当前版本**: v2.6.0 | **最新更新**: 2026-11-15

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![MySQL](https://img.shields.io/badge/MySQL-8.4-blue)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

## 功能概览

| 模块 | 能力 |
|------|------|
| 收银 | 商品搜索、购物车、会员手机号识别；现金/微信/支付宝/银行卡支付；小票打印、钱箱、交班；支付支持 `disabled` / `mock` / `production`，mock 模式可一键模拟支付成功 |
| 触屏版 | 扫码或输入精确命中自动加购（含成功/未找到/库存不足提示音）、一键切换语言、底部显示班次与日期时间，交班流程与 PC 版一致 |
| 商品与库存 | 商品增删改查、名称唯一（条码可重复）、库存预警与快速入库、库存盘点、CSV 导入、Excel/PDF 导出 |
| 会员 | 注册、查询、充值、积分累计与等级自动升级（普通/银卡/金卡/钻石），消费与退货联动 |
| 挂单与取单 | 收银中暂存购物车并恢复，标准收银台与触屏版共用；REST API 支持挂单、取单、取消，管理员可清理旧单或删除记录，金额由服务端重算，收银员只能操作自己的挂单 |
| 促销 | 满减/打折/优惠券，自动选择优惠最大的方案，与收银台同一计价口径；REST API 写操作限财务/管理员，并提供只读试算接口 |
| 采购 | 供应商、采购订单与审批、采购入库与入库历史、采购报表 |
| 退货 | 基于原交易退货，记录退货原因、商品状态和退款方式；审批通过后恢复库存并按现金、余额或积分退款 |
| 报表 | 交易统计、采购/库存/利润/退货报表，支持 Excel 与 PDF 导出 |
| 用户与审计 | 管理员/收银员/财务三种角色、BCrypt 密码、密码重置与首次登录改密、审计日志 |
| 备份与恢复 | 本地备份（数据库/配置/日志/发票/业务数据）、自动备份与保留策略；恢复前需管理员密码确认；未接入的云备份接口返回 501 |
| REST API | Javalin 服务默认 `8080`，100+ 路由与 `/ws/sync` WebSocket，Token 认证、角色授权、限流与安全响应头 |
| 硬件 | ESC/POS 热敏与网络打印、打印机发现与状态检测、USB HID 扫码枪、支付二维码 |
| 国际化与主题 | 简体中文/英文/繁體中文；LiSuan/浅色/深色主题；内置 Noto Sans CJK SC |

## 快速开始

### 环境要求

- JDK 17 或更高版本
- Maven 3.8 或更高版本
- MySQL 8.4（兼容 8.0/8.3）
- Docker Compose 可选，用于快速启动 MySQL

### 配置与启动数据库

复制 `.env.example` 为根目录 `.env`，至少把 `MYSQL_ROOT_PASSWORD`、`CASHIER_DB_PASSWORD`、`TOKEN_SECRET` 改成各自独立的强随机值，然后启动数据库：

```bash
cp .env.example .env
docker compose up -d mysql
```

数据库密码的来源顺序为 **进程环境变量 → 工作目录 `.env` → `config/database.properties`**；配置文件中的 `db.password` 始终留空，密码不写入该文件。初始化脚本 `docker/mysql-init/00-init-complete.sql` 只在数据卷为空时执行一次，后续表结构由应用启动时自动升级。

### 首次运行

空库首次启动会弹出**首次运行向导**，由操作者当场创建管理员账号并设置密码。仓库不提供任何默认口令。

### 运行

```bash
# 开发运行
mvn clean compile
mvn javafx:run

# 打包运行
mvn clean package
java -jar target/lisuan-fx-*-jar-with-dependencies.jar
```

Windows 首次安装运行 `install.bat`，日常用 `start.bat`（或 `start.bat --gui`）启动，数据库配置工具为 `DataConfig.bat`。Linux/macOS 执行 `./install.sh`，之后用 `./start.sh` 启动。

> 生产环境启动 API 前必须设置至少 32 字符的 `TOKEN_SECRET`，并把 `CORS_ALLOWED_ORIGINS` 限制为实际域名（禁止 `*`）。

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
| 普通会员 | 0-999 | 无折扣，按 10.0 计算 |
| 银卡会员 | 1000-4999 | 9.5 折 |
| 金卡会员 | 5000-9999 | 9 折 |
| 钻石会员 | 10000+ | 8.5 折 |

折扣计算方式：`消费金额 * (会员折扣 / 10.0)`。

## REST API

API 服务默认监听 `8080`，当前注册 100+ HTTP 路由和 WebSocket 同步端点 `/ws/sync`，覆盖认证、商品、会员、交易、挂单、促销、库存、报表、设置、发票、用户、打印、支付、备份、国际化和同步状态。

除 `/api/health`、`/api/auth/login` 和支付回调 `/api/payment/notify/*` 外，API 默认需要认证。支付回调由渠道服务器直接调用，无法携带本系统 Token，安全性由微信/支付宝验签保证。

## 安全要点

- 密码使用 BCrypt 存储，登录失败按用户累计并锁定
- 所有 SQL 使用 `PreparedStatement`，安全敏感场景使用 `SecureRandom`
- 支付回调验签 + 金额校验 + 终态保护（重复回调幂等）
- API Token 认证（24 小时过期）、三级角色授权、每 IP 每分钟 60 次限流
- 库存与会员余额使用乐观锁，防止超卖和超额扣款

## 数据库

项目使用 MySQL，连接池为 HikariCP。表结构由应用启动时自动创建与升级，完整初始化脚本见 `docker/mysql-init/00-init-complete.sql`，版本说明见 `docker/mysql-init/DATABASE_VERSIONS.md`。

## 相关文档

- [CLAUDE.md](CLAUDE.md) — 项目架构、迁移状态与开发约定
- [AGENTS.md](AGENTS.md) — 开发速查与验证命令
- [docs/GO_LIVE_CHECKLIST.md](docs/GO_LIVE_CHECKLIST.md) — 上线走查清单
- [docs/HARDWARE_ACCEPTANCE.md](docs/HARDWARE_ACCEPTANCE.md) — 硬件验收测试表（打印机/扫码枪/钱箱/触屏/客显）
- [docs/CREDENTIALS_CHECKLIST.md](docs/CREDENTIALS_CHECKLIST.md) — 上线凭据准备清单（数据库/API/支付/备份）
- [docs/DATABASE_INIT.md](docs/DATABASE_INIT.md) — 数据库初始化说明
- [docker/README.md](docker/README.md) — Docker MySQL 部署与备份

## 最近更新

### v2.6.0

**2026-08-29**
- 打印设置接线：设置页的“启用打印 / 打印机名称 / 纸张 / Logo”对触屏版结账小票生效（未启用则不出票），并新增“测试打印”
- 设置页新增“扫码枪测试”，与设备一致校验归一化、空数据和 128 字符上限

**2026-08-28**
- 数据库密码统一为 `CASHIER_DB_PASSWORD`（兼容旧拼写），配置文件不再保存明文密码，改由 `.env` / 环境变量注入
- Docker 初始化 SQL 不再写死库名，支持自定义 `MYSQL_DATABASE`

**2026-08-23**
- 支付支持 mock 模式“模拟支付成功”，界面内即可闭环测试；回调增加金额校验、重复回调幂等和终态保护
- 恢复备份前必须输入管理员密码确认
- 触屏版：精确扫码自动加购与提示音、交班退出流程、底部状态栏、支付弹窗模拟支付；PC 版退出回到登录界面
- 修复支付回调被鉴权拦截、健康检查连接泄漏、电子支付弹窗 NPE

**2026-07-24**
- 触屏版支持工具栏一键切换简体中文/英文/繁體中文，语言偏好全局生效
- 修复语言切换不生效问题，新用户自动使用系统语言
- 触屏版部分现金支付时提示继续用现金完成，避免记账错误

更早版本的功能与修复记录见 Git 提交历史。

## 故障排除

**应用无法启动**：确认 JDK 17+、MySQL 正在运行，并查看 `logs/` 下的日志。

**数据库连接失败**：确认 `CASHIER_DB_PASSWORD` 已在环境变量或根目录 `.env` 中提供（配置文件不保存密码），且 `DB_HOST`/`DB_PORT`/`MYSQL_*` 与本地一致；Docker 环境可先执行 `docker compose up -d mysql`。

**API 服务无法启动**：确认 `TOKEN_SECRET` 至少 32 字符、`CORS_ALLOWED_ORIGINS` 未使用 `*`，并检查日志中的端口占用。

**扫描枪无响应**：确认扫描枪处于 USB HID 模式，且当前焦点未被其他输入控件占用。

**打印无响应**：检查打印机连接、驱动和纸张状态；网络打印确认 IP、端口和防火墙配置。

**中文显示异常**：确认内置 Noto Sans CJK SC 随资源打包，Linux 环境可额外安装中文字体包。

---

## 许可证

木兰宽松许可证 v2 (MulanPSL2)

**仓库**: https://gitee.com/nevell/lisuan.git

**问题反馈**: https://gitee.com/nevell/lisuan/issues
