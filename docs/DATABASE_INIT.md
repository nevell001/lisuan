# 数据库初始化文档

## 概述

收银系统使用 MySQL 8.0 作为主数据库。数据库初始化有两条通道：

1. **Docker 容器初始化**（仅 Docker Compose 首次以空数据卷启动时）- 由环境变量创建数据库和专用用户，并执行建表脚本
2. **应用启动初始化**（每次启动）- 幂等创建/升级表结构，是应用侧表结构的权威来源

非 Docker 部署只需自行准备数据库和用户，表结构交给应用启动时自动创建。

---

## 初始化流程

### 1. Docker 容器初始化

数据库名和应用专用用户由 `docker-compose.yml` 传给官方 MySQL 镜像的环境变量创建，**没有**单独的建用户 SQL 脚本：

| 环境变量 | 来源 | 默认值 |
|----------|------|--------|
| `MYSQL_DATABASE` | `.env` | `lisuan_system` |
| `MYSQL_USER` | `.env` | `lisuan` |
| `MYSQL_PASSWORD` | `.env` 的 `CASHIER_DB_PASSWORD` | 无默认值，必填 |
| `MYSQL_ROOT_PASSWORD` | `.env` | 无默认值，必填 |

镜像在首次以空数据卷启动时创建上述数据库和用户并授权，随后执行 `/docker-entrypoint-initdb.d/` 下的脚本。`docker-compose.yml` 只挂载了一个脚本：

- `docker/mysql-init/00-init-complete.sql` - 完整建表脚本

**注意**:
- 初始化脚本只在数据卷为空时执行一次；已有数据的卷不会重跑，由应用启动初始化兜底补建/升级。
- `docker/mysql-init/` 下的 `07-`、`08-`、`09-`、`10-`、`11-`、`99-` 系列脚本用于**手工**升级旧库，compose 不会自动挂载执行。

### 2. 应用启动初始化

应用每次启动都会调用 `DatabaseManager.initializeDatabase()`（`src/main/java/com/cashier/util/DatabaseManager.java`）：

- 用 `CREATE TABLE IF NOT EXISTS` 幂等创建缺失的表
- 用带 try/catch 的幂等 `ALTER TABLE ... ADD COLUMN` 就地升级旧表（如 `users.force_password_change`、`products.is_hot`）

**这是应用侧表结构的权威来源**：新增表或字段必须改这里，而不是只改 SQL 文件。此外部分功能 DAO 也会按需建表（`CREATE TABLE IF NOT EXISTS`），`00-init-complete.sql` 中则包含少量应用侧不再创建的表——几处清单并不完全一致，请一律以源码为准，不要依赖文档中的清单。

---

## 数据库配置

配置文件字段、环境变量优先级与连接池参数（含默认值）见 [MYSQL_SETUP.md](MYSQL_SETUP.md) 的「配置应用」与「性能优化」章节；Docker 部署的数据库环境变量见 [DOCKER_CONFIGURATION.md](DOCKER_CONFIGURATION.md)。

---

## 数据表结构

> 以下为核心/常用表的**示意**结构（并非全部表，个别字段可能与最新源码有出入）。完整、权威的表结构以 `DatabaseManager.java`、功能 DAO 的建表语句与 `docker/mysql-init/00-init-complete.sql` 为准。

### 核心业务表

> **本节说明字段语义，不复制 DDL。** 唯一权威来源是 Java 侧建表代码（`DatabaseManager.java` 的
> `createTable*`）与功能 DAO 的按需建表；`docker/mysql-init/00-init-complete.sql` 是给 Docker 部署的
> 镜像，两者由 `InitSchemaParityTest` 守着。
>
> 此前本节内嵌了 11 段手写 `CREATE TABLE`，**不受门禁保护**，实测已漂移 4 张表——
> `members` 缺 `id`/`member_code`/`version`、`transaction_items` 缺
> `product_id`/`product_code`/`barcode`、`categories` 缺 `id`、`operation_logs` 缺 4 个审计列；
> 照那份副本建表会建出**真实的错误结构**（例如把 `phone` 当会员表主键、漏掉乐观锁列）。
> 现已改为"字段说明 + 指向真源"。

#### 1. users - 用户表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `username` | VARCHAR UNIQUE | 登录名 |
| `password` | VARCHAR | BCrypt 口令哈希 |
| `name` | VARCHAR | 姓名 |
| `role` | VARCHAR | admin / cashier / finance |
| `active` | TINYINT | 是否启用 |
| `force_password_change` | TINYINT | 首次登录/重置后强制改密（由幂等 ALTER 追加） |
| `last_login_time` | BIGINT | 最后登录时间戳 |
| `create_time` | TIMESTAMP | 创建时间 |

> 角色决定登录后进 `MainView` 还是触屏收银台。

#### 2. products - 商品表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `product_code` | VARCHAR UNIQUE | 商品编号（P+YYYYMMDD+4 位序号） |
| `name` | VARCHAR UNIQUE NOT NULL | 商品名称（UNIQUE，v2.4.3 起） |
| `price / cost` | DECIMAL(10,2) | 售价 / 成本价 |
| `quantity` | INT | 库存数量 |
| `category / unit / brand / supplier / spec` | VARCHAR | 分类、单位、品牌、供应商、规格 |
| `barcode` | VARCHAR | 条码（v2.3.1 起允许重复） |
| `min_stock` | INT | 低库存预警阈值 |
| `version` | INT | 乐观锁版本号：结账扣库存 `WHERE version=?`，相对增减必须 `version = version + 1`（TD-024） |
| `is_hot` | TINYINT | 热销标记（由幂等 ALTER 追加） |
| `description` | TEXT | 描述 |
| `created_at / updated_at` | TIMESTAMP | 创建 / 更新时间 |

> `version` 是防超卖的核心，任何改数量的路径都不能漏掉它。

#### 3. members - 会员表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `member_code` | VARCHAR UNIQUE | 会员编号（MEM+YYYYMMDD+4 位序号） |
| `phone` | VARCHAR UNIQUE NOT NULL | 手机号（UNIQUE，但不是主键） |
| `name` | VARCHAR NOT NULL | 姓名 |
| `balance` | DECIMAL(10,2) | 储值余额（等同发钱，改它限 finance/admin，TD-017） |
| `points` | DECIMAL(10,2) | 积分（BigDecimal；等级由积分推导 `MemberService.calculateLevel`） |
| `level` | VARCHAR | 普通 / 银卡 / 金卡 / 钻石 |
| `discount` | DECIMAL(4,2) | 折扣，10=不打折、9.5=95 折…0=免单 |
| `join_date` | BIGINT | 入会时间戳 |
| `birthday` | VARCHAR | 生日 |
| `version` | INT | 乐观锁版本号（防余额超扣） |

> 等级/折扣在保存时由积分推导，只锁等级/折扣挡不住改折扣（TD-017）。

#### 4. transactions - 交易表

| 字段 | 类型 | 说明 |
|---|---|---|
| `transaction_id` | VARCHAR PK | 交易号 |
| `timestamp` | BIGINT | 成交时间戳 |
| `total_amount` | DECIMAL(10,2) | 明细原价合计（三处结账路径口径统一） |
| `final_amount` | DECIMAL(10,2) | 实付金额（含会员折扣与促销） |
| `tax` | DECIMAL(10,2) | 税额 = `calculateTax(final_amount)`（价内税，按实付计） |
| `payment_method` | VARCHAR | 现金/微信/支付宝/银行卡（筛选取数前必须归一化，TD-002） |
| `operator_username / operator_name` | VARCHAR | 经手人登录名 / 显示名 |
| `member_phone` | VARCHAR | 会员手机号（可空） |
| `transaction_type` | VARCHAR | 交易类型 |
| `status` | VARCHAR | 状态；退款口径见 `COALESCE(status,'NORMAL')`（TD-003） |
| `voided / voided_by / voided_at` | — | 作废标记、作废人、作废时间 |

> `total_amount - final_amount` 才是优惠额；小票上两者不等时必须打优惠行。

#### 5. transaction_items - 交易明细表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键（退货行级校验按它定位，F10-c） |
| `transaction_id` | VARCHAR | 所属交易号（FK → `transactions.transaction_id`） |
| `product_id` | INT | 商品 ID |
| `product_code` | VARCHAR | 商品编号（快照） |
| `product_name` | VARCHAR | 商品名称（快照） |
| `barcode` | VARCHAR | 条码（快照，v2.4.1 起） |
| `price` | DECIMAL(10,2) | 成交单价（单位金额，DECIMAL(10,2)，非整数） |
| `quantity` | INT | 数量 |
| `subtotal` | DECIMAL(10,2) | 小计 |

> 同一商品可能有多行（不同单价）——只按商品合计校验退货量会多退（F10-c）。

#### 6. shifts - 班次表

| 字段 | 类型 | 说明 |
|---|---|---|
| `shift_id` | VARCHAR PK | 班次号 |
| `operator_username / operator_name` | VARCHAR | 交班人 |
| `start_time / end_time` | BIGINT | 开班 / 交班时间戳 |
| `opening_revenue / closing_revenue / shift_revenue` | DECIMAL(10,2) | 期初 / 期末 / 本班营业额 |
| `opening_transaction_count / closing_transaction_count / shift_transaction_count` | INT | 期初 / 期末 / 本班笔数 |
| `cash_revenue / wechat_revenue / alipay_revenue / card_revenue` | DECIMAL(10,2) | 分支付方式营业额 |
| `notes` | TEXT | 备注 |

> 结账/支付前必须确认有有效班次；查不到班次状态时不得当作没开班（TD-035）。

#### 7. promotions - 促销表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `name / description` | VARCHAR / TEXT | 名称 / 说明 |
| `type` | VARCHAR | 促销类型 |
| `threshold` | DECIMAL(10,2) | 门槛金额 |
| `discount` | DECIMAL(10,2) | 折扣值；界面必须拒绝超过 2 位小数（0.985 会被存成 0.99，TD-034） |
| `start_date / end_date` | — | 生效区间 |
| `enabled` | TINYINT | 是否启用 |
| `usage_count / max_usage` | INT | 已用 / 上限次数 |
| `created_at` | TIMESTAMP | 创建时间 |

> 促销按原价总额计算并选优（`selectBestPromotion`）；定价缓存 30s，写路径须使缓存失效（TD-032）。

#### 8. categories - 分类表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `name` | VARCHAR | 分类名 |
| `description` | TEXT | 描述 |

#### 9. recharge_records - 充值记录表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `record_id` | VARCHAR | 充值流水号 |
| `member_phone / member_name` | VARCHAR | 会员手机号 / 姓名 |
| `amount` | DECIMAL(10,2) | 充值金额 |
| `payment_method` | VARCHAR | 支付方式 |
| `operator` | VARCHAR | 操作人 |
| `timestamp` | BIGINT | 充值时间戳 |

> 充值不限角色（收银台日常操作），且写本表留痕（TD-018）。

#### 10. operation_logs - 操作日志表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | 主键 |
| `username` | VARCHAR | 操作人 |
| `operation` | VARCHAR | 操作类型 |
| `details` | TEXT | 详情 |
| `ip_address` | VARCHAR | 来源 IP |
| `timestamp` | BIGINT | 时间戳 |
| `log_level` | VARCHAR | 日志级别 |
| `log_category` | VARCHAR | 日志分类 |
| `operation_result` | VARCHAR | 操作结果 |
| `affected_records` | INT | 影响记录数 |

> 会员等级/折扣变更必须写审计日志（`MEMBER/MEMBER_LEVEL_DISCOUNT_UPDATED`，TD-017）。

#### 11. settings - 系统设置表

| 字段 | 类型 | 说明 |
|---|---|---|
| `key` | VARCHAR PK | 设置项（如 `paperSize`、`backupFrequency`） |
| `value` | VARCHAR | 稳定代码（如 `58mm`/`daily`），不是本地化显示串（TD-040） |
| `description` | VARCHAR | 说明 |
| `updated_at` | TIMESTAMP | 更新时间 |

> 落库值必须是代码，显示层翻译；读取时要回读（TD-040 曾因此静默重置设置）。

## 备份和恢复

完整方法（自动备份、mysqldump、Docker 容器/数据卷备份、应用内置备份）见 [MYSQL_SETUP.md](MYSQL_SETUP.md) 的「备份与恢复」章节。

---

## 用户和权限

### 默认账号

| 账号 | 密码 | 类型 | 说明 |
|------|------|------|------|
| `lisuan` | `.env` 的 `CASHIER_DB_PASSWORD` | 数据库用户 | Docker 首次启动时按 `MYSQL_USER` 创建（默认 `lisuan`，可改），供应用连接数据库 |
| `admin` | 首次运行向导中设置（无默认口令） | 应用用户 | 系统管理员，空库首次启动时由向导创建 |

### 应用角色权限

| 角色 | 权限 |
|------|------|
| admin | 所有权限 |
| cashier | 日常收银操作 |
| finance | 报表和数据统计 |

### 创建数据库用户

新增或调整数据库账号的 SQL 示例见 [MYSQL_SETUP.md](MYSQL_SETUP.md) 的「配置数据库」章节。

---

## 常见问题

连接失败（含 Docker 容器检查）、时区、字符编码、权限不足等问题的排查见 [MYSQL_SETUP.md](MYSQL_SETUP.md) 的「故障排查」章节。

---

## 最佳实践

1. **定期备份**: 每天自动备份数据库
2. **监控性能**: 定期检查慢查询日志
3. **索引优化**: 根据查询模式调整索引
4. **连接池配置**: 根据并发量调整连接池大小
5. **日志保留**: 定期清理操作日志表
6. **权限管理**: 定期审查用户权限
7. **安全加固**: 定期轮换数据库口令，限制数据库远程访问

---

## 参考文档

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [HikariCP 文档](https://github.com/brettwooldridge/HikariCP)
- [Docker MySQL 文档](https://hub.docker.com/_/mysql)
- 数据库管理器: `com.cashier.util.DatabaseManager`
