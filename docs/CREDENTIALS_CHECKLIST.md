# LiSuan 上线凭据准备清单

> 用途：正式环境上线前需要向各平台申请/生成的真实凭据清单。
> 每个凭据按「配置字段 → 申请入口 → 安全要求 → 存放位置 → 验证方式」整理。
> ⚠️ 所有密钥均不得写入 Git；生产建议通过环境变量注入（`.env` 或系统环境）。

## 0. 优先级说明

- **必填**：数据库、API 安全、至少一个支付通道（微信或支付宝）
- **选配**：云备份（任选一种远端即可，也可仅本地备份）
- 准备顺序建议：数据库 → API 安全 → 支付 → 备份

---

## 1. 数据库（必填）

| 凭据 | 配置字段 | 申请/生成方式 | 安全要求 | 存放位置 |
|---|---|---|---|---|
| MySQL root 密码 | `.env` `MYSQL_ROOT_PASSWORD` | 自行生成 | ≥16 位强随机，仅用于初始化 | `.env`（gitignored） |
| 应用用户密码（lisuan） | `.env` `CASHIER_DB_PASSWORD` | 自行生成 | ≥16 位强随机，与 root 不同 | 仅 `.env`（方案A：不写入 `config/database.properties`） |
| 数据库主机/端口 | `DB_HOST` / `DB_PORT` / `db.url` | 部署环境 | 生产内网或 SSH 隧道 | `.env` + `config/database.properties` |
| 数据库 SSL | `db.url` 里的 `sslMode`（`config/database.properties`，建议生产用 `REQUIRED`） | 部署环境 | 生产启用 SSL/TLS | `config/database.properties` |

验证：`mysql -h <host> -P <port> -u lisuan -p -e "SELECT 1"`；启动应用观察连接池日志。

## 2. API 安全（必填，启用 API 前）

| 凭据 | 配置字段 | 生成方式 | 安全要求 | 存放位置 |
|---|---|---|---|---|
| Token 密钥 | `TOKEN_SECRET`（环境变量优先）；`config/api.properties` `token.secret` | `openssl rand -base64 48` | ≥32 字符（建议 64），强随机 | 环境变量或 `config/api.properties` |
| CORS 允许来源 | `CORS_ALLOWED_ORIGINS`；`cors.allowed.origins` | 部署方填写 | 必须为具体域名，禁止 `*` | 环境变量或 `config/api.properties` |
| Token 过期小时 | `token.expire.hours` | 部署方决定 | 默认 24，按安全策略调整 | `config/api.properties` |

验证：`curl http://<host>:8080/api/health`；`POST /api/auth/login`；无 token 访问受保护接口返回 401。

## 3. 支付通道（至少启用一个）

### 3.1 微信支付（Native 扫码）

申请入口：微信支付商户平台（pay.weixin.qq.com），需营业执照开户。

| 凭据 | 配置字段（`config/payment.properties`） | 说明 | 安全要求 |
|---|---|---|---|
| 商户号 MCH ID | `wechat.mch.id` | 商户平台开户后获得 | - |
| 应用 AppID | `wechat.app.id` | 公众号/小程序/开放平台应用 | 与商户号绑定 |
| API v3 密钥 | `wechat.api.key` | 商户平台→账户中心→API 安全 | 32 字符随机，仅 API 使用 |
| 商户 API 私钥 | `wechat.private.key.path` | 商户平台生成 API 证书时下载 | `apiclient_key.pem`，权限 600 |
| 商户证书序列号 | `wechat.merchant.serial.no` | API 证书详情中查看 | 与私钥配对 |
| 平台证书/公钥 | `wechat.cert.path` | 微信支付平台证书下载 | 用于回调验签 |
| 回调地址 | `notify.url` | 部署方填写 | 必须为 HTTPS 公网可达，`/api/payment/notify` |

验证：创建支付订单生成 `weixin://` 二维码；用微信扫码支付；确认回调验签（日志含"签名验证"与金额校验）。

### 3.2 支付宝（当面付/预下单）

申请入口：支付宝开放平台（open.alipay.com）→ 创建应用 → 签约当面付。

| 凭据 | 配置字段（`config/payment.properties`） | 说明 | 安全要求 |
|---|---|---|---|
| 应用 AppID | `alipay.app.id` | 开放平台应用详情 | - |
| 应用私钥 | `alipay.private.key` | 开放平台→密钥管理→生成/上传 | RSA2，2048 位，仅服务端持有 |
| 支付宝公钥 | `alipay.public.key` | 开放平台密钥管理中获取 | 用于回调/响应验签 |
| 支付宝证书路径 | `alipay.cert.path` | 证书模式时使用（可空，公钥模式够用） | 与模式一致 |
| 网关 | `alipay.gateway` | 默认 `https://openapi.alipay.com/gateway.do` | 生产勿改 |
| 回调地址 | `notify.url` | 部署方填写 | HTTPS 公网可达 |

验证：创建订单生成支付宝二维码；扫码支付；确认回调返回 `success`。

### 3.3 通用

| 配置 | 字段 | 说明 |
|---|---|---|
| 支付模式 | `payment.mode` | `production`（生产）/ `mock`（测试）/ `disabled` |
| 订单过期分钟 | `order.expire.minutes` | 默认 15 |

## 4. 云备份（选配，任选一种）

凭据填写入口：应用「系统设置→备份设置」或直接写 `backup_config`；字段见代码 `BackupConfig`。

### 阿里云 OSS
| 凭据 | 字段 | 申请入口 |
|---|---|---|
| AccessKey ID / Secret | `aliyunAccessKey` / `aliyunSecretKey` | 阿里云 RAM 用户，仅授予 OSS 目标桶读写权限 |
| Bucket / Endpoint / Region | `aliyunBucket` / `aliyunEndpoint` / `aliyunRegion` | OSS 控制台 |

### 腾讯云 COS
| 凭据 | 字段 | 申请入口 |
|---|---|---|
| SecretId / SecretKey | `tencentSecretId` / `tencentSecretKey` | 腾讯云 CAM 子账号 |
| Bucket / Region | `tencentBucket` / `tencentRegion` | COS 控制台 |

### 七牛云
| 凭据 | 字段 | 申请入口 |
|---|---|---|
| AccessKey / SecretKey | `qiniuAccessKey` / `qiniuSecretKey` | 七牛控制台密钥管理 |
| Bucket / Domain | `qiniuBucket` / `qiniuDomain` | 七牛空间 |

### AWS S3
| 凭据 | 字段 | 申请入口 |
|---|---|---|
| AccessKey / SecretKey | `awsAccessKey` / `awsSecretKey` | AWS IAM 用户，最小权限策略 |
| Bucket / Region | `awsBucket` / `awsRegion` | S3 控制台 |

### FTP / WebDAV（自建或 NAS）
| 凭据 | 字段 | 说明 |
|---|---|---|
| FTP 主机/端口/用户/密码/路径 | `ftpHost` / `ftpPort` / `ftpUser` / `ftpPassword` / `ftpPath` | 专用备份账号，禁 root |
| WebDAV URL/用户/密码/路径 | `webdavUrl` / `webdavUser` / `webdavPassword` / `webdavPath` | 支持 HTTPS 的 WebDAV |

验证：触发一次手动备份，确认远端文件出现、大小与本地一致；再做一次恢复演练。

## 5. 其他

| 项目 | 说明 | 是否需凭据 |
|---|---|---|
| 打印机 | 系统设置中选择已连接打印机 | 否（硬件） |
| 扫码枪 | USB-HID 自动识别 | 否（硬件） |
| 局域网/多端同步 | API 端口、防火墙放行 | 否（网络） |

## 6. 密钥轮换与备份

- 每半年轮换：`TOKEN_SECRET`、支付 API v3 密钥、应用私钥、云存储 SecretKey
- 私钥/密钥首次配置后离线备份一份（加密压缩），注明用途与日期
- 凭据发生疑似泄露时，先轮换再排查，禁止把旧凭据留在配置文件中

---
准备人：________ 日期：________ 复核人：________
