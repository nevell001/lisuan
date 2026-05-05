# 收银系统生产级功能开发路线图

> 版本: v2.5.0 规划
> 更新日期: 2026-05-05
> 目标: 从学习项目升级为生产级收银系统

---

## 🎯 开发目标

将收银系统从单机学习项目升级为支持多终端、多支付方式、云备份的生产级系统。

---

## 📋 功能规划总览

| 功能模块 | 优先级 | 预计工作量 | 状态 |
|---------|--------|-----------|------|
| REST API 基础框架 | P0 | 3天 | 🟡 进行中 |
| 多终端同步（WebSocket） | P0 | 2天 | ⏳ 待开始 |
| 电子支付集成 | P1 | 5天 | ⏳ 待开始 |
| 发票功能 | P1 | 3天 | ⏳ 待开始 |
| 云备份 | P2 | 2天 | ⏳ 待开始 |
| 网络打印 | P2 | 2天 | ⏳ 待开始 |
| 多语言支持 | P2 | 3天 | ⏳ 待开始 |

---

## 🔧 详细开发计划

### 1️⃣ REST API 基础框架（P0）

**目标**: 提供 HTTP API 接口，支持多终端访问

**技术选型**:
- **Javalin 6.x** - 轻量级嵌入式 Web 框架
- **Jackson** - JSON 序列化
- **Token 认证** - 简单有效的认证方案

**API 接口清单**:

```
/api/v1/
├── /health              # 健康检查（无需认证）
├── /auth/
│   ├── POST /login      # 登录获取 Token
│   ├── POST /token      # 刷新 Token
│   ├── POST /logout     # 注销
│   └── GET  /me         # 当前用户信息
├── /products/
│   ├── GET  /           # 商品列表（分页）
│   ├── GET  /{id}       # 商品详情
│   ├── POST /           # 创建商品
│   ├── PUT  /{id}       # 更新商品
│   ├── DELETE /{id}     # 删除商品
│   └── GET  /search     # 商品搜索
├── /members/
│   ├── GET  /           # 会员列表
│   ├── GET  /{id}       # 会员详情
│   ├── GET  /phone/{phone} # 按手机号查询
│   ├── POST /           # 创建会员
│   ├── PUT  /{id}       # 更新会员
│   ├── POST /{id}/recharge # 会员充值
│   └── GET  /search     # 会员搜索
├── /transactions/
│   ├── GET  /           # 交易列表
│   ├── GET  /{id}       # 交易详情
│   ├── POST /           # 创建交易（收银）
│   ├── GET  /today      # 今日交易
│   └── GET  /stats      # 交易统计
├── /inventory/
│   ├── GET  /           # 库存列表
│   ├── GET  /alerts     # 库存预警
│   ├── POST /update     # 更新库存
│   └── GET  /check      # 库存盘点
├── /reports/
│   ├── GET  /daily      # 日报表
│   ├── GET  /monthly    # 月报表
│   ├── GET  /sales      # 销售报表
│   └── GET  /products   # 商品排行
├── /settings/
│   ├── GET  /           # 系统设置
│   └── PUT  /           # 更新设置
├── /users/              # 用户管理（管理员）
│   ├── GET  /           # 用户列表
│   ├── POST /           # 创建用户
│   ├── PUT  /{id}       # 更新用户
│   ├── DELETE /{id}     # 删除用户
└── /ws/
    └── /sync            # WebSocket 实时同步
```

**开发任务**:
1. 添加 Javalin/Jackson 依赖到 pom.xml
2. 创建 `ApiServer.java` - API 服务器主类
3. 创建 `AuthMiddleware.java` - Token 认证中间件
4. 创建各 API Controller:
   - `AuthController.java` - 认证
   - `HealthController.java` - 健康检查
   - `ProductApiController.java` - 商品
   - `MemberApiController.java` - 会员
   - `TransactionApiController.java` - 交易
   - `InventoryApiController.java` - 库存
   - `ReportApiController.java` - 报表
   - `SettingsApiController.java` - 设置
   - `UserApiController.java` - 用户管理
5. 创建 DTO 类（请求/响应对象）
6. 在主应用中集成 API 服务器启动/停止
7. 添加 API 配置选项（端口、启用/禁用）

---

### 2️⃣ 多终端同步（P0）

**目标**: 多个收银终端实时同步库存、交易数据

**技术方案**:
- **WebSocket** - 实时双向通信
- **数据库锁** - 乐观锁 + 悲观锁防超卖
- **事件广播** - 库存变化、交易完成广播

**功能点**:
1. WebSocket 连接管理
2. 库存实时同步（商品增减）
3. 交易实时广播
4. 心跳检测与断线重连
5. 终端标识与管理

**开发任务**:
1. `SyncWebSocket.java` - WebSocket 处理器
2. `SyncEvent.java` - 同步事件类型
3. `TerminalManager.java` - 终端管理
4. 修改 `TransactionService` 添加事件广播
5. 修改 `ProductService` 添加库存变化广播

---

### 3️⃣ 电子支付集成（P1）

**目标**: 支持微信支付、支付宝扫码支付

**技术方案**:
- **二维码支付** - 商家扫用户付款码
- **支付回调** - 支付成功通知
- **支付记录** - 与交易关联

**功能点**:
1. 微信支付接口集成
2. 支付宝支付接口集成
3. 支付状态查询
4. 支付退款处理
5. 支付记录与对账

**开发任务**:
1. `PaymentService.java` - 支付服务抽象
2. `WeChatPayService.java` - 微信支付实现
3. `AlipayService.java` - 支付宝实现
4. `PaymentController.java` - 支付接口
5. 支付配置文件
6. 支付回调处理
7. 支付记录数据库表

**前置条件**:
- 需要商户资质（微信支付商户号、支付宝商户ID）
- 需要申请 API 密钥

---

### 4️⃣ 发票功能（P1）

**目标**: 生成电子发票，支持税务合规

**技术方案**:
- **发票生成** - PDF 格式电子发票
- **发票号码** - 自动编号管理
- **发票打印** - 发票格式输出

**功能点**:
1. 发票信息录入（买家信息）
2. 发票自动生成（PDF）
3. 发票号码管理
4. 发票查询与打印
5. 发票红冲（作废）

**开发任务**:
1. `InvoiceService.java` - 发票服务
2. `InvoiceDAO.java` - 发票数据访问
3. `Invoice.java` - 发票模型
4. `InvoiceController.java` - 发票 UI
5. `InvoiceApiController.java` - 发票 API
6. 发票 PDF 模板
7. 发票数据库表

---

### 5️⃣ 云备份（P2）

**目标**: 支持云端备份，异地灾备

**技术方案**:
- **阿里云 OSS** / **腾讯云 COS** - 云存储
- **定时备份** - 自动上传
- **增量备份** - 减少传输量

**功能点**:
1. 云存储配置
2. 数据自动备份上传
3. 增量备份支持
4. 备份恢复下载
5. 备份加密

**开发任务**:
1. `CloudBackupService.java` - 云备份服务
2. 云存储 SDK 集成
3. 备份加密实现
4. 备份配置界面
5. 备份日志与监控

---

### 6️⃣ 网络打印（P2）

**目标**: 支持远程打印机、网络小票机

**技术方案**:
- **网络打印机** - IP/端口直连
- **打印队列** - 异步打印
- **打印状态** - 成功/失败反馈

**功能点**:
1. 网络打印机配置
2. 远程打印指令发送
3. 打印队列管理
4. 打印状态追踪
5. ESC/POS 指令支持

**开发任务**:
1. `NetworkPrinter.java` - 网络打印机类
2. `PrintQueue.java` - 打印队列
3. 打印机配置界面
4. ESC/POS 指令封装

---

### 7️⃣ 多语言支持（P2）

**目标**: 支持中文、英文等多语言界面

**技术方案**:
- **ResourceBundle** - Java 国际化标准
- **语言包** - 各语言翻译文件
- **动态切换** - 界面即时切换

**功能点**:
1. 中文语言包（默认）
2. 英文语言包
3. 语言切换功能
4. FXML 多语言绑定
5. API 多语言响应

**开发任务**:
1. `I18nManager.java` - 国际化管理器
2. 语言包文件:
   - `messages_zh_CN.properties`
   - `messages_en_US.properties`
3. FXML 多语言改造
4. 语言切换界面

---

## 📊 数据库变更

需要新增的数据表:

```sql
-- 支付记录表
CREATE TABLE payment_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    payment_type VARCHAR(20) NOT NULL,  -- wechat/alipay/cash/member
    payment_amount DECIMAL(10,2) NOT NULL,
    payment_time BIGINT NOT NULL,
    payment_status VARCHAR(20) DEFAULT 'success',
    external_id VARCHAR(100),           -- 第三方支付流水号
    operator VARCHAR(50),
    INDEX idx_transaction (transaction_id),
    INDEX idx_time (payment_time)
);

-- 发票表
CREATE TABLE invoices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(20) UNIQUE NOT NULL,
    transaction_id VARCHAR(50) NOT NULL,
    buyer_name VARCHAR(100),
    buyer_tax_no VARCHAR(50),
    buyer_address VARCHAR(200),
    buyer_phone VARCHAR(50),
    invoice_amount DECIMAL(10,2) NOT NULL,
    tax_amount DECIMAL(10,2),
    invoice_time BIGINT NOT NULL,
    invoice_status VARCHAR(20) DEFAULT 'valid',  -- valid/cancelled
    pdf_path VARCHAR(200),
    INDEX idx_transaction (transaction_id),
    INDEX idx_invoice_no (invoice_no)
);

-- API 会话表（可选，当前使用内存）
CREATE TABLE api_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(100) UNIQUE NOT NULL,
    user_id INT NOT NULL,
    create_time BIGINT NOT NULL,
    expire_time BIGINT NOT NULL,
    last_activity BIGINT NOT NULL,
    INDEX idx_token (token),
    INDEX idx_user (user_id)
);

-- 终端表
CREATE TABLE terminals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    terminal_id VARCHAR(50) UNIQUE NOT NULL,
    terminal_name VARCHAR(100),
    terminal_type VARCHAR(20),           -- pos/mobile/web
    status VARCHAR(20) DEFAULT 'online',
    last_sync_time BIGINT,
    create_time BIGINT
);
```

---

## 📁 目录结构变更

新增目录:

```
src/main/java/com/cashier/
├── api/                          # REST API 模块
│   ├── ApiServer.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── ProductApiController.java
│   │   ├── MemberApiController.java
│   │   ├── TransactionApiController.java
│   │   ├── ...
│   ├── dto/                      # 数据传输对象
│   │   ├── ProductDTO.java
│   │   ├── MemberDTO.java
│   │   ├── TransactionDTO.java
│   │   ├── ...
│   └── middleware/
│       ├── AuthMiddleware.java
│       └── RateLimitMiddleware.java
├── payment/                      # 支付模块
│   ├── PaymentService.java
│   ├── WeChatPayService.java
│   ├── AlipayService.java
│   └── PaymentRecord.java
├── invoice/                      # 发票模块
│   ├── InvoiceService.java
│   ├── InvoiceDAO.java
│   └── Invoice.java
├── sync/                         # 多终端同步
│   ├── SyncWebSocket.java
│   ├── TerminalManager.java
│   └── SyncEvent.java
├── backup/                       # 云备份
│   ├── CloudBackupService.java
│   └── BackupConfig.java
├── i18n/                         # 国际化
│   ├── I18nManager.java
│   └── LanguageBundle.java
└── printer/                      # 已有，扩展网络打印
    ├── NetworkPrinter.java
    └── PrintQueue.java

src/main/resources/
├── i18n/                         # 语言包
│   ├── messages_zh_CN.properties
│   └── messages_en_US.properties
├── invoice_template.pdf          # 发票模板
└── api_config.properties         # API 配置
```

---

## ⚙️ 配置新增

`config/api.properties`:
```properties
# API 服务器配置
api.enabled=true
api.port=8080
api.host=0.0.0.0

# CORS 配置
cors.allowed.origins=*

# Token 配置
token.expire.hours=24
token.secret=your_secret_key_here
```

`config/payment.properties`:
```properties
# 微信支付配置
wechat.app.id=YOUR_WECHAT_APPID
wechat.mch.id=YOUR_MCH_ID
wechat.api.key=YOUR_API_KEY

# 支付宝配置
alipay.app.id=YOUR_ALIPAY_APPID
alipay.private.key=YOUR_PRIVATE_KEY
```

`config/cloud_backup.properties`:
```properties
# 云存储类型: aliyun/tencent/local
backup.provider=aliyun
backup.bucket.name=your_bucket
backup.access.key=YOUR_ACCESS_KEY
backup.secret.key=YOUR_SECRET_KEY
backup.encrypt.enabled=true
```

---

## 🧪 测试计划

每个模块需要编写:
1. 单元测试 - 核心逻辑测试
2. 集成测试 - API 接口测试
3. 手动测试 - UI 功能验证

---

## 📅 时间规划

| 阶段 | 内容 | 时间 |
|-----|------|-----|
| 第一周 | REST API 基础框架 + 多终端同步 | 5天 |
| 第二周 | 电子支付集成 | 5天 |
| 第三周 | 发票功能 + 云备份 | 5天 |
| 第四周 | 网络打印 + 多语言 + 测试 | 5天 |

---

## ✅ 里程碑

- **v2.5.0-alpha1**: REST API + WebSocket 完成
- **v2.5.0-alpha2**: 电子支付完成
- **v2.5.0-alpha3**: 发票 + 云备份完成
- **v2.5.0-beta**: 全功能测试完成
- **v2.5.0**: 生产级版本发布

---

## 📝 注意事项

1. **支付接口**: 需要商户资质，测试阶段可用沙箱环境
2. **发票功能**: 需确认当地税务法规要求
3. **云备份**: 需考虑数据隐私和安全
4. **多终端**: 需测试并发场景下的数据一致性

---

## 🔄 更新日志

- 2026-05-05: 初始规划文档创建