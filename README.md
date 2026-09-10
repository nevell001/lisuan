# 狸算(LiSuan)收银系统

简体中文 | [English](./README_en.md) | [繁體中文](./README_zh_TW.md)

狸算(LiSuan)收银系统是一个基于 JavaFX 17 的桌面 POS 收银系统，面向零售门店的收银、商品、会员、采购、库存、退货、报表、用户权限、数据备份和硬件接入等日常经营场景。

**当前版本**: v2.6.0 | **最新更新**: 2026-08-29 | **测试覆盖**: 589 个测试用例

> 测试口径：`mvn -q clean verify` 默认运行 589 个用例（含测试 + SpotBugs + JaCoCo 门禁）；
> `LoginControllerUITest`（17 个用例）需要真实显示环境，在桌面环境用
> `mvn -Pui-tests -Dtest=LoginControllerUITest test` 显式运行，全量共 606 个。

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![MySQL](https://img.shields.io/badge/MySQL-8.4-blue)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

## 功能概览

### 收银管理
- 结账收银、购物车增删改、商品搜索、会员手机号识别
- 支持现金、微信、支付宝、银行卡等支付方式
- 支付支持 `disabled` / `mock` / `production` 三种模式；mock 模式可在支付弹窗一键“模拟支付成功”，本地闭环验证整条支付链路
- 会员折扣、积分、余额消费与交易记录联动
- 小票打印、打印预览、钱箱打开、快捷键帮助
- 交班管理，记录班次销售额和收银员业绩
- 触屏版：扫码/输入精确命中商品自动加入购物车（含成功/未找到/库存不足提示音），连续快速录入
- 触屏版底部状态栏显示“触屏版POS”、班次、日期(含星期)与时间；退出交班流程与 PC 版一致（交班 / 取消 / 确定）

### 商品与库存
- 商品增删改查，支持商品编码、条码、分类、单位、规格、售价、成本价和库存信息
- 商品名称唯一约束，条码允许重复
- 库存预警、低库存查询、快速入库
- 库存盘点、盘点明细、差异处理和盘点状态跟踪
- 商品 CSV 导入、Excel/PDF 导出

### 会员与客户
- 会员注册、编辑、查询、充值和余额管理
- 消费积分累计，会员等级自动升级
- 等级折扣规则：普通 10 折、银卡 9.5 折、金卡 9 折、钻石 8.5 折
- 充值记录查询，会员消费与退货流程联动

### 采购管理
- 供应商资料维护
- 采购订单创建、编辑、查询和状态流转
- 采购审批流程
- 采购入库和入库历史
- 采购报表统计

### 退货管理
- 基于原交易创建退货订单
- 退货商品、退货原因、商品状态和退款方式记录
- 退货审批流程
- 审批通过后恢复库存，并按现金、余额或积分等方式处理退款
- 退货订单与退货报表支持查询、筛选、导出和打印

### 报表与统计
- 交易记录查询与今日交易统计
- 数据统计视图
- 采购报表、库存报表、利润分析、退货报表
- 支持 Excel 和 PDF 导出
- 利润分析包含销售额、成本、毛利、毛利率、品类和日趋势等视图

### 用户、权限与审计
- 默认角色：管理员、收银员、财务
- BCrypt 密码加密，支持密码重置和首次登录改密
- 用户启用、停用、删除和角色管理
- 管理员可查看审计日志，覆盖登录认证、交易、退货、库存、采购、会员、用户和系统设置等操作

### 数据备份与恢复
- 菜单内置数据备份和数据恢复
- SQL 备份默认生成到 `backups/sql`
- 备份服务支持数据库、配置、日志、发票和业务数据文件打包
- 支持本地备份、自动备份配置、保留策略和备份清理
- API 侧保留云备份配置字段，便于对接对象存储
- 恢复备份前必须输入管理员密码确认，防止未授权覆盖数据

### REST API 与同步
- 内置 Javalin API 服务，默认端口 `8080`
- 当前注册 90+ HTTP 路由和 WebSocket 同步端点
- 覆盖认证、商品、会员、交易、库存、报表、设置、发票、用户、打印、支付、备份、国际化和同步状态
- Token 身份认证、角色授权、请求限流和安全响应头
- 支付回调路由（`/api/payment/notify/*`）对外公开，安全由渠道验签（微信 RSA/AES-GCM、支付宝 RSA、mock 密钥）保证
- WebSocket 同步端点：`/ws/sync`

### 硬件支持
- ESC/POS 热敏打印、网络打印、打印队列和打印历史
- 打印机发现、连接、状态检测、默认打印机配置
- USB HID 扫描枪接入和扫码焦点管理
- 电子支付二维码生成与支付状态查询

### 国际化与主题
- 支持简体中文、英文、繁體中文
- 运行时文案、弹窗、表单校验、状态栏和报表页面持续国际化
- 货币显示国际化
- 主题支持：LiSuan、浅色、深色，并兼容旧版 IntelliJ 主题偏好
- 内置 Noto Sans CJK SC，优化跨平台中文显示
- 左侧功能导航图标已按模块语义整理，避免重复
- 触屏版支持一键快速切换语言，设置全局生效

## 快速开始

### 环境要求
- JDK 17 或更高版本
- Maven 3.8 或更高版本
- MySQL 8.4 或兼容的 MySQL 8.0/8.3
- Docker Compose 可选，用于快速启动 MySQL

### 默认账户
- 用户名：`admin`
- 初始密码：`admin123`
- 首次登录后建议立即修改密码

### 启动数据库

```bash
docker compose up -d mysql
```

数据库初始化脚本位于 `docker/mysql-init/00-init-complete.sql`。配置文件为 `config/database.properties`，其中 `db.password` **始终留空**——密码由运行环境提供。应用与各启动脚本的取值顺序一致：**进程环境变量 → 工作目录 `.env` → 配置文件**：

```bash
export CASHIER_DB_PASSWORD=your_password
# 也可以写进根目录 .env（已被 .gitignore 忽略）：
# CASHIER_DB_PASSWORD=your_password
```

### 环境变量配置（.env）

项目支持用根目录的 `.env` 文件集中管理安装与启动配置，避免把密码、密钥写进配置文件或脚本。应用运行时（含直接 `java -jar`）也会读取该文件中的 `CASHIER_DB_PASSWORD`。

```bash
cp .env.example .env
```

`.env.example` 是随仓库维护的模板，`.env` 是本地实际配置（已被 `.gitignore` 忽略，请勿提交真实密钥）。主要变量：

| 变量 | 说明 |
|------|------|
| `APP_VERSION` / `APP_NAME` | 应用版本号与品牌名（`install.sh` 打包用） |
| `ENVIRONMENT` | `development`（root 连库）或 `production`（专用用户 lisuan 连库） |
| `DB_TYPE` | 数据库类型：`docker` / `local` / `none`，用于 `install.sh` 引导 |
| `MYSQL_CONTAINER_NAME` / `MYSQL_IMAGE` | Docker MySQL 容器与镜像 |
| `MYSQL_ROOT_PASSWORD` / `CASHIER_DB_PASSWORD` / `MYSQL_USER` / `MYSQL_DATABASE` | Docker 数据库初始化账号（`CASHIER_DB_PASSWORD` 同时作为应用运行密码） |
| `DB_HOST` / `DB_PORT` | 数据库连接地址与端口 |
| `TZ` | 时区（默认 `Asia/Shanghai`） |
| `JVM_OPTS` | JVM 启动参数（`config/jvm.config` 优先） |
| `TOKEN_SECRET` | API Token 密钥，至少 32 位强随机串 |
| `CORS_ALLOWED_ORIGINS` | API 允许的跨域来源，生产禁止 `*` |
| `CASHIER_DB_PASSWORD` / `CASHER_DB_PASSWORD` | 应用数据库用户密码（Docker 建库建用户与应用运行共用；`CASHER_DB_PASSWORD` 为旧拼写兼容） |

使用说明：

- `install.sh` / `install.bat` 会整体加载 `.env` 用于数据库初始化、配置生成和打包；未定义的变量会走交互式引导。
- 所有安装/配置通道（`install.sh`、`install.bat`、`DataConfig.bat`、`docker/docker-init.sh`）都**不会**把密码写进 `config/database.properties`——该文件的 `db.password` 在任何环境下都留空。开发模式把密码写入根目录 `.env`（Linux/macOS 下权限收到 `rw-------`），生产模式由环境变量注入。`release.sh` / `release.bat` 有对应门禁：配置文件出现非空 `db.password` 即拒绝发布。
- `start.sh` 只定向读取 `CASHIER_DB_PASSWORD` / `CASHER_DB_PASSWORD` 两个密码变量覆盖数据库密码，不把 `TOKEN_SECRET`、`CORS_ALLOWED_ORIGINS` 等占位配置带入运行环境（API 侧仍通过系统环境变量传递）。
- `DataConfig.bat` 同样读取 `.env` 生成数据库配置，开发模式下把密码写回 `.env` 而不是配置文件。
- 直接 `java -jar` 启动（不经过 `start.bat`/`start.sh`）时，应用自身也会读取工作目录的 `.env`，因此三种启动方式共用同一份密码来源。
- **安全提示**：首次使用务必替换 `MYSQL_ROOT_PASSWORD`、`CASHIER_DB_PASSWORD`、`TOKEN_SECRET` 为各自独立的强随机值，并限制 `CORS_ALLOWED_ORIGINS`。旧版 `.env` 中的 `MYSQL_PASSWORD` 已统一为 `CASHIER_DB_PASSWORD`（脚本仍向后兼容读取旧变量）。

### 开发运行

```bash
mvn clean compile
mvn javafx:run
```

### 打包运行

```bash
mvn clean package
java -jar target/lisuan-fx-2.6.0-jar-with-dependencies.jar
```

也可以使用通配符启动：

```bash
java -jar target/lisuan-fx-*-jar-with-dependencies.jar
```

### Windows

首次安装推荐运行：

```bat
install.bat
```

日常启动：

```bat
start.bat
start.bat --gui
```

数据库配置工具：

```bat
DataConfig.bat
```

### Linux / macOS

```bash
chmod +x install.sh start.sh
./install.sh
./start.sh
```

`install.sh` 会检查 Java/Maven、构建可执行 JAR，并引导配置 Docker、本地或远程 MySQL。`start.sh` 会优先启动带依赖的可执行 JAR。

## 常用命令

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 跳过测试打包
mvn clean package -DskipTests

# 运行指定测试类
mvn test -Dtest=ProductDAOTest

# 运行指定测试方法
mvn test -Dtest=PasswordUtilTest#testHashPassword

# 静态质量检查
mvn -q -DskipTests spotbugs:check

# 完整门禁：测试 + SpotBugs + JaCoCo 覆盖率
mvn -q clean verify
```

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

API 服务默认运行在 `8080` 端口。生产环境启动 API 前必须配置安全参数：

```bash
export TOKEN_SECRET=at_least_32_characters_secret
export CORS_ALLOWED_ORIGINS=https://your-domain.example
```

主要接口分组：

| 分组 | 路径 | 说明 |
|------|------|------|
| 健康检查 | `/api/health` | 服务与数据库状态 |
| 认证 | `/api/auth/*` | 登录、刷新 Token、登出、当前用户 |
| 商品 | `/api/products/*` | 商品 CRUD、低库存 |
| 会员 | `/api/members/*` | 会员 CRUD、手机号查询、充值 |
| 交易 | `/api/transactions/*` | 交易创建、查询、退款、今日统计 |
| 库存 | `/api/inventory/*` | 库存列表、预警、盘点、库存更新 |
| 报表 | `/api/reports/*` | 日报、月报、热销商品、支付方式统计 |
| 设置 | `/api/settings/*` | 系统设置读写 |
| 发票 | `/api/invoices/*` | 发票创建、查询、作废、打印记录 |
| 用户 | `/api/users/*` | 用户管理 |
| 打印 | `/api/printers/*` | 打印机发现、连接、状态、打印 |
| 支付 | `/api/payment/*` | 支付创建、状态查询、退款、配置 |
| 备份 | `/api/backup/*` | 备份执行、恢复、下载、配置 |
| 国际化 | `/api/i18n/*` | 语言、消息和语言包查询 |
| 同步 | `/ws/sync`, `/api/sync/status` | 多终端同步和在线状态 |

除 `/api/health`、`/api/auth/login` 和支付回调 `/api/payment/notify/*` 外，API 默认需要认证
（支付回调由微信/支付宝服务器直接调用，无法携带本系统 Token，安全性由渠道验签保证）。

## 数据库

项目使用 MySQL，连接池为 HikariCP。关键表包括：

| 表 | 说明 |
|----|------|
| `products` | 商品信息，商品名称唯一 |
| `members` | 会员信息 |
| `transactions`, `transaction_items` | 交易与交易明细 |
| `return_orders`, `return_order_items` | 退货订单与退货明细 |
| `purchase_orders`, `purchase_order_items` | 采购订单与明细 |
| `purchase_approvals` | 采购审批记录 |
| `purchase_inbound`, `purchase_inbound_items` | 采购入库与明细 |
| `suppliers` | 供应商 |
| `inventory_check`, `inventory_check_items` | 库存盘点 |
| `users` | 用户与角色 |
| `shifts` | 交班记录 |
| `invoices` | 发票 |
| `payment_orders`, `refund_records` | 支付订单与退款记录 |
| `promotions` | 促销规则 |
| `backup_records`, `backup_config` | 备份记录与备份配置 |
| `operation_logs` | 审计日志 |
| `settings` | 系统设置 |

数据库版本说明见 `docker/mysql-init/DATABASE_VERSIONS.md`。

## 项目结构

```text
src/main/java/com/cashier/
├── api/           # Javalin REST API、认证、同步
├── component/     # 可复用 JavaFX 组件
├── constant/      # 应用、数据库、系统属性等常量
├── controller/    # JavaFX 控制器
├── dao/           # 数据访问对象
├── i18n/          # 国际化管理和键名常量
├── model/         # 业务实体
├── notification/  # 通知系统
├── printer/       # 打印设备与模板
├── scanner/       # 扫描枪接入
├── service/       # 业务服务
└── util/          # 数据库、导入导出、日志、主题等工具
```

```text
src/main/resources/
├── com/cashier/view/      # FXML 页面
├── com/cashier/i18n/      # 语言资源
├── css/                   # LiSuan、浅色、深色主题
├── fonts/                 # 内置字体
└── images/                # 应用图标和图片资源
```

## 相关文档

- [CLAUDE.md](CLAUDE.md) — 项目架构、迁移状态与开发约定
- [docs/GO_LIVE_CHECKLIST.md](docs/GO_LIVE_CHECKLIST.md) — 上线走查清单
- [docs/HARDWARE_ACCEPTANCE.md](docs/HARDWARE_ACCEPTANCE.md) — 硬件验收测试表（打印机/扫码枪/钱箱/触屏/客显）
- [docs/CREDENTIALS_CHECKLIST.md](docs/CREDENTIALS_CHECKLIST.md) — 上线凭据准备清单（数据库/API/支付/云备份）
- [docs/DATABASE_INIT.md](docs/DATABASE_INIT.md) — 数据库初始化说明

## 代码质量与安全

### 安全措施
- **密码存储**: 使用 BCrypt 加密，禁止明文存储
- **SQL 注入防护**: 全部使用 `PreparedStatement` 参数化查询
- **随机数生成**: 安全敏感场景使用 `SecureRandom`
- **资源管理**: JDBC 连接使用 try-with-resources 防止泄漏
- **登录防暴力**: 登录失败按用户累计并锁定，成功/过期自动重置
- **恢复保护**: 数据恢复前校验管理员密码
- **支付安全**: 回调验签 + 金额校验 + 终态保护（已退款/关闭订单拒绝迟到回调，重复回调幂等）
- **API 认证**: Token 基础认证，24 小时过期
- **角色授权**: 三级权限（管理员、收银员、财务）
- **请求限流**: 每 IP 每分钟最多 60 次请求
- **安全响应头**: X-Content-Type-Options, X-Frame-Options, X-XSS-Protection

### 代码质量
- **单元测试**: 589 个测试用例（`mvn -q clean verify`），覆盖 DAO、Service、工具类、并发安全与 API（全量 606，含需显示环境的 UI 测试）
- **静态检查**: SpotBugs 高风险缺陷门禁
- **覆盖率门禁**: JaCoCo 行覆盖率 ≥10%
- **i18n 门禁**: 强制三套语言包 key 一致、`I18nKeys` 常量齐全、源码 i18n 调用 key 齐全
- **并发安全测试**: 乐观锁防库存超卖、防会员余额超扣
- **依赖管理**: Maven Enforcer 插件确保依赖一致性
- **日志规范**: 统一使用 SLF4J + LoggerFactoryUtil
- **代码规范**: 遵循 CLAUDE.md 中的行为指南

## 开发约定

- 日志统一使用 `LoggerFactoryUtil.getLogger()`。
- DAO 使用 `PreparedStatement`，并继承 `BaseDAO`。
- 跨 DAO 的业务事务统一通过 `DatabaseManager.executeBooleanTransaction()` 等事务入口处理。
- 新增界面优先使用 FXML + Controller，并补齐三套语言资源。
- 新增 i18n key 优先放入 `I18nKeys`，避免控制器里散落字符串。
- 新增非 i18n 常量优先收口到对应常量类，避免重复字面量。
- 代码质量警告修复后至少运行编译，涉及公共逻辑时补跑测试。

## 最近更新

### v2.6.0

#### 2026-08-29
- 打印设置接线：设置页的“启用打印 / 打印机名称 / 纸张 / Logo”现对触屏版结账小票生效（未启用则不出票）；设置页新增“测试打印”按钮
- 扫码枪测试：设置页新增“扫码枪测试”输入框，与设备一致校验归一化、空数据与 128 字符上限

#### 2026-08-28
- 配置安全：统一数据库密码变量为 `CASHIER_DB_PASSWORD`（兼容旧拼写 `CASHER_DB_PASSWORD` 与旧版 `MYSQL_PASSWORD`），配置文件不再保存明文密码，运行密码由 `.env`/环境变量注入
- Docker：初始化 SQL 不再写死库名，支持自定义 `MYSQL_DATABASE`；compose 变量化并挂载 `my.cnf`
- 脚本修复：install.sh JDBC characterEncoding 改为 UTF-8；release.bat 中文输出与版本号门禁修复

#### 2026-08-23
- DAO 层全部完成实例化迁移（`XxxDAORefactored extends BaseDAO` + `DAOFactory`），移除静态 DAO 与死代码
- 支付：支持 mock 模式“模拟支付成功”按钮，界面内闭环测试支付；支付回调路由公开并由渠道验签保证安全
- 支付回调业务加固：金额校验、重复回调幂等、已退款/关闭订单拒绝迟到回调
- 恢复备份前必须输入管理员密码确认
- 触屏版：扫码/输入精确命中商品自动加购 + 成功/未找到/库存不足提示音；退出交班流程（交班/取消/确定，交班后直接退出）；底部状态栏显示“触屏版POS”、班次与日期星期时间；支付弹窗增加模拟支付按钮
- PC 版：主界面退出改为退出到登录界面（登录页退出仍关闭应用）；结账添加商品成功后清空搜索栏
- 代码质量：修复支付回调被鉴权拦截、健康检查连接泄漏、电子支付弹窗 NPE 等问题；i18n 完整性门禁、并发安全测试、token 过期测试
- 新增上线走查清单 `docs/GO_LIVE_CHECKLIST.md` 与凭据准备清单 `docs/CREDENTIALS_CHECKLIST.md`

#### 2026-07-24
- 触屏版新增语言切换功能：工具栏一键切换简体中文/英文/繁體中文
- 语言偏好优化：同时保存用户偏好和全局默认，新用户自动使用系统语言
- 修复语言切换不生效问题：统一语言偏好存储逻辑，优先读取用户偏好、全局默认、系统默认
- I18nManager 改进：使用语言标签匹配替代 Locale 对象比较，提升跨平台兼容性
- 代码质量统一：LanguagePreferenceDAO 日志系统从 java.util.logging 迁移至 SLF4J
- 触屏版新增混合支付提示：部分现金支付时强制继续使用现金完成，避免记账错误

### v2.5.9 (2026-07-19)
- 安全加固：支付订单和退款记录使用 `SecureRandom` 生成随机码
- 修复 FormValidator DISCOUNT 验证规则边界值问题（10.1 应无效）
- 修复 DatabaseManager JDBC 资源泄漏问题（ResultSet 未关闭）
- 依赖更新：Jackson 2.18.2, SLF4J 2.0.16, Mockito 5.15.2, JUnit 5.11.3, H2 2.3.232
- 测试增强：新增 FormValidatorTest (33 个测试) 和 LoginControllerUITest (17 个测试)
- 380 个测试用例全部通过，测试覆盖率持续提升

### v2.5.9-maintenance (2026-07-05)
- 修复了 API Token 的安全测试失败问题
- 优化了测试环境下的 Mockito 配置
- 强化了生产环境配置模板的安全提示
- 统一了数据库初始化脚本，支持更多功能模块的开箱即用
- 优化了 README 文档结构，提升易读性

### v2.5.9-maintenance (2026-07-04)
- 整理 `I18nKeys`，将常用国际化键名集中管理
- 收口系统属性、数据库配置、资源包名、日期时间格式等非 i18n 常量
- 修复多处代码质量警告，包括复杂度、重复字符串、switch/default、资源关闭和静态检查问题
- 更新左侧功能导航图标，保证分组和功能入口图标不重复
- README 按当前功能、API、数据库和维护流程重新整理

### v2.5.9 (2026-06-20)
- 国际化完善：统一简体中文、英文和繁體中文资源，补齐弹窗、状态、日期、审批及表单校验提示
- 退货流程优化：增加快捷日期筛选，修复查看原交易、打印单据、完成退货和导出提示的多语言显示
- 采购与盘点修复：修复入库历史列不显示、盘点类型文字截断及空表提示未跟随应用语言的问题
- 表单显示修复：移除商品添加/编辑页空错误标签产生的红色竖线，并本地化校验错误
- 字体与主题优化：默认使用 LiSuan 主题，简体中文优先加载 Noto Sans CJK SC 字形并完善跨平台回退
- 语言精简：移除日语、韩语，仅保留简体中文、英文和繁體中文

### v2.5.7-l10n (2026-06-20)
- 语言包精简为简体中文、英文、繁體中文
- 补齐缺失的国际化键值，减少页面翻译回退
- 优化弹窗、二级页面、状态栏、提示语等文案显示
- 调整部分页面顶部标签和输入区域的布局宽度

### v2.5.7 (2026-06-12)
- 支持 development/production 环境区分
- 开发环境使用 root，生产环境使用 lisuan 专用用户
- FormValidator 安全解析方法，防止 NumberFormatException
- 安装脚本支持 `.env` 和自动环境检测
- Windows 配置工具支持环境变量
- 脚本显示品牌统一为 LiSuan System

### v2.5.6 (2026-06-10)
- 品牌名称统一为“狸算(LiSuan)收银系统”
- 更新用户界面、脚本、小票打印、图标和 Docker 容器品牌信息

### v2.5.5 (2026-06-09)
- 标签页宽度和关闭按钮视觉优化
- 新增字号调整功能
- 修复 fcitx5 输入法兼容性
- 优化繁体中文显示和多平台字体回退链

### v2.5.4 (2026-05-21)
- 新增 GUI 数据库配置工具
- 优化 Windows 分发包和安装脚本

### v2.5.3 (2026-05-15)
- Windows 平台优化
- 启动体验增强
- 同步逻辑增强
- DAO 重构完成
- 修复资源清理和内存泄漏问题

## 故障排除

**应用无法启动**
- 检查 JDK 是否为 17 或更高版本
- 检查 MySQL 是否正在运行
- 检查 `config/database.properties`（密码不在该文件里，需通过环境变量或根目录 `.env` 提供）
- 查看 `logs/` 目录下的日志文件

**数据库连接失败**
- 确认数据库地址、端口、库名、用户名和密码
- 确认 `CASHIER_DB_PASSWORD` 有值：进程环境变量优先，其次工作目录 `.env`（直接 `java -jar` 启动也会读 `.env`）
- 若用 `.env` 管理配置，确认已运行 `cp .env.example .env` 且 `DB_HOST`/`DB_PORT`/`MYSQL_*` 与本地一致
- Docker 环境可先执行 `docker compose up -d mysql`

**API 服务无法启动**
- 确认已设置至少 32 字符的 `TOKEN_SECRET`
- 生产环境不要使用 `CORS_ALLOWED_ORIGINS=*`
- 查看日志中的端口占用或配置错误

**扫描枪无法工作**
- 确认扫描枪已连接并处于 USB HID 模式
- 确认当前页面焦点未被其他输入控件占用

**打印无响应**
- 检查打印机连接、驱动和纸张状态
- 查看打印机状态检测和打印历史
- 网络打印需确认 IP、端口和防火墙配置

**中文显示异常**
- 优先确认内置 Noto Sans CJK SC 是否随资源打包
- Linux 环境可额外安装中文字体包

## 许可证

木兰宽松许可证 v2 (MulanPSL2)

---

**仓库**: https://gitee.com/nevell/lisuan.git

**问题反馈**: https://gitee.com/nevell/lisuan/issues
