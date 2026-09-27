# 技术债清单

记录已识别但**有意推迟**的技术问题：每条都说明为什么现在不做、什么条件下必须做。
新条目请沿用同样的小节结构，并在表格中登记。

| ID | 标题 | 类别 | 状态 | 关联需求 |
|---|---|---|---|---|
| TD-001 | [安全预留] 发票文件路径白名单校验 | 安全 | 待处理（阻塞于需求） | 发票预览 / 下载 |
| TD-002 | 支付方式未在落库前归一化，报表/交班对不上账 | 正确性 | **已修复（2026-09）** | 支付方式口径统一 |
| TD-003 | 已退款交易仍计入营业额（毛/净口径未定） | 正确性 | **已修复（2026-09）** | 报表口径 |
| TD-004 | REST API 校验弱于桌面端（会员/支付方式/operator） | 安全/正确性 | **已修复（2026-09）** | API 加固 |
| TD-005 | 触屏收银台状态栏未接线，提示在界面上不可见 | UI 缺陷 | **已修复（2026-09）** | 触屏收银台 |
| TD-006 | 非收银台控制器仍在 FX 线程同步查库 | 性能/体验 | **部分修复（2026-09）**：页面级加载 + 报表链路 + 点击后短查询已改完（27 条门禁），剩约 6 处（含交互流程与写操作） | 界面卡顿 |
| TD-007 | 结账 worker 线程改 FX 侧共享集合 | 并发 | **已修复（2026-09）** | 收银台线程纪律 |
| TD-008 | 标准收银台成功弹窗用结账后会员重算金额 | 正确性 | **已修复（2026-09）** | 结账口径 |
| TD-009 | H2 测试库无外键（生产 14 个），外键类缺陷测不出 | 测试基建 | **已修复（2026-09）** | 回归门禁 |
| TD-010 | docker/mysql-init 的「完整初始化」与 Java 建表漂移 | 运维 | **已修复（2026-09）**：补 9 表、对齐列与类型、5 张遗留表标注，5 项门禁 | 数据库初始化 |
| TD-011 | Windows 启动脚本把未匹配的 JAR 通配符当路径，静默跳过构建 | 发布 | 待处理 | Windows 启动 |
| TD-012 | install.sh / docker-init.sh 静默成功与占位口令 | 运维 | 待处理 | 安装脚本 |
| TD-013 | 版本号门禁只覆盖 4 处中的 2 处 | 发布 | **已修复（2026-09）** | 版本管理 |
| TD-014 | i18n 硬编码与文档中的测试数过期 | 文档/体验 | **部分修复（2026-09）**：文档数字与 locale 折叠已修，可见文案硬编码待办 | — |
| TD-015 | `String.format` 用默认 locale 格式化金额（73 处） | 正确性 | 待处理 | 非中文 locale 部署 |

> 本节条目来自 2026-09 的全量审计（`mvn verify` 三关全绿的前提下，逐条回读代码 + 真实
> Javalin 最小复现验证）。
>
> **已修（第一批，P0）**：路由遮蔽（`products/low-stock`、`invoices/seller-info`）、
> 商品/库存 API 丢弃乐观锁返回值、标准端与 API 的裸毫秒订单号、支付单号裸毫秒主键、
> 盘点完成非原子、会员对象回滚污染、退款 `APPLYING` 预占泄漏。
>
> **已修（第二批）**：TD-002 支付方式落库前归一化 + 聚合归一化、TD-003 营业额净额口径
> （剔除已整单退款 + 扣减已完成退货，日报/月报/桌面统计/交班/利润报表统一）、
> TD-005 触屏收银台状态栏接线。
>
> **已修（第三批）**：TD-006 的**页面级加载**（9 个控制器 / 18 个方法）全部改走
> `UIOptimizer.runInBackground` 并逐条登记门禁；弹窗/处理器级的剩余项见该条目。
>
> **已修（第四批）**：TD-004 全项——会员更新校验对齐桌面端 + 乐观锁回 409、
> `POST /api/payment/create` 金额必须与交易实付一致（交易不存在回 404）、
> 支付/备份的操作员一律取认证用户且支付单操作员在落库前写入。
>
> **已修（第五批）**：TD-009——审计归属列（`transactions.operator_username`、
> `operation_logs.username`）改为不带外键的纯文本（删用户/改名不得改写历史，
> 且审计日志里本来就会写显示名），其余 14 个外键在测试库补齐，
> 生产 DDL / 测试 schema / docker 初始化脚本三处一致，并加 5 项门禁。
>
> **已修（第六批）**：TD-008——标准收银台结账成功弹窗改用本单落库的 `final_amount`
> （此前用结账后被改写等级的会员重算，跨 1000 分当场升银卡的会员会少显示 5%）。
>
> **已修（第七批）**：TD-007——两个收银台的结账 worker 改为只吃切线程前的快照
> （明细/库存/会员/促销），库存由 FX 线程在回填时并回，不再有后台线程写 FX 侧 `HashMap`
> 与直接改写 `currentMember` 的竞态。
>
> **已修（第八批）**：TD-013——版本号四处（`pom.xml`/`AppConstants`/`Installer`/`.env.example`）
> 一致性改为 **JUnit 门禁**（随 `mvn verify` 在 CI 跑），两个发布脚本的比对清单同步扩到四处；
> TD-014 部分——文档测试数不再写死、`I18nUiUtils` 大小写折叠改 `Locale.ROOT`
> （土耳其语环境会把 `CHECKING` 折成 `checkıng` 导致状态匹配失败），并澄清 `CurrencyUtil`
> 那条是误报；新登记 TD-015（73 处 `String.format` 默认 locale）。
>
> **已修（第九批）**：TD-010——`docker/mysql-init` 补齐 Java 侧会建的 9 张表（DDL 从源码原样提取）、
> 对齐 3 处列集合与 12 处列类型、把 5 张无引用的历史表显式标注为遗留，
> 并加 `InitSchemaParityTest`（表/列/类型/白名单/核心表 5 项，含变异验证）。

---

## TD-001 [安全预留] 发票文件路径白名单校验

**类别**：安全（预留加固）
**状态**：待处理 —— 当前无实际可达路径，等「发票预览/下载」需求落地时必须一并实现
**提出来源**：安全审计 L8

### 现状

`POST /api/invoices/{id}/print` 会把请求体里的 `pdfPath` / `imagePath` 原样持久化到
`invoices` 表：

- `InvoiceApiController.printInvoice` 读取 `pdfPath` / `imagePath`
- `InvoiceDAORefactored.updatePrintInfoWithConnection` 直接写库

这两个字段目前**没有任何地方会去打开或读取文件内容**，所以现在不构成漏洞，
只是一个"写进去的任意字符串"。一旦将来做发票预览/下载，把它当成文件路径去读，
就会立刻变成一个本地任意文件读取原语（例如 `../../config/database.properties`）。

### 为什么现在不修

按最小改动原则，现状下加白名单属于**为不存在的能力做防御**；而且"合法路径范围"
（是导出目录下？还是用户自选目录？）取决于尚未确定的发票预览/下载产品设计，
现在定下来的白名单大概率是错的。

### 触发条件（必须处理）

出现以下任一情况时，本条必须与对应需求**同一次**实现，不得分批：

1. 新增「发票预览」「发票下载」「打开 PDF/图片」等任何会按该路径读取文件的接口或 UI；
2. 现有代码中出现对 `invoices.pdf_path` / `invoices.image_path` 的读取（`new File(path)`、
   `Files.read*`、`Path.of(path)`、作为静态资源返回等）；
3. 把该字段暴露给比 `finance/admin` 更低的角色，或暴露给外部集成。

### 建议方案（届时细化）

- 只接受**相对路径 + 固定根目录**（导出目录或专用发票目录），拒绝绝对路径与 `..`；
- 落库前按规范化后的路径做前缀校验（参考 `ExportUtil.export` 与
  `BackupService` 恢复路径的既有做法，二者都已做过同类校验）；
- 读取时二次校验（TOCTOU 防护），并限制扩展名白名单（`.pdf` / `.png` / `.jpg`）；
- 补一条"路径穿越被拒"的回归测试。

### 参考实现

- `src/main/java/com/cashier/util/ExportUtil.java` —— 子目录名与规范化前缀校验
- `src/main/java/com/cashier/service/BackupService.java` —— 解压条目逃逸工作区目录的校验

---

## TD-002 支付方式未在落库前归一化，报表/交班对不上账

**类别**：正确性（口径）　**状态**：**已修复（2026-09）**
**提出来源**：2026-09 全量审计（API + 数据层）

### 现状（修复前）

- 筛选与退款已归一化（`I18nUiUtils.canonicalPaymentMethod`），但**落库与聚合没跟上**：
  - `TransactionApiController.create` 把请求体里的 `paymentMethod` 原样写库（只判非空），库里因此同时存在
    「现金」与「CASH」「WeChat Pay」；
  - `ReportApiController.dailySales` 用 `payment.contains("现金")` 分桶 → 代码形式的现金单
    **计入 totalAmount、四个桶全是 0**；
  - `TransactionDAORefactored.getPaymentMethodStats` 裸 `GROUP BY payment_method` → 同一方式被拆成多行；
  - `ShiftController` 交班只匹配「现金」/「CASH」等字面量 → 交班现金合计与总营业额对不上。

### 修复（2026-09）

- **写入口归一化**：新增 `I18nUiUtils.storedPaymentMethod(value)`（中文/繁体/英文/代码 → 规范中文落库值），
  `TransactionApiController.create` 归一化后才写库；无法识别的写法直接 **400**
  （顺带修掉"任意字符串落库"与"超 20 字符 → 500"两个问题）。
- **聚合按归一化合并/分桶**：
  - `getPaymentMethodStats` 按归一化代码合并同一方式的多种写法（标签优先用规范中文值，既有输出不变），
    并按金额排序；
  - `ReportApiController.dailySales`、`StatisticsController`、`ShiftController.categorizeShiftRevenue`
    全部改为按归一化代码分桶；
  - `getStatistics` 的现金笔数兼容 `'现金','現金','CASH','Cash'`。
- **门禁**（均做变异验证）：
  - `TransactionApiControllerTest.createNormalizesPaymentMethodForStorage`（CASH → 落库「现金」；未知写法 → 400）
  - `TransactionDAOTest.paymentMethodStatsAndCashCountAreNormalized`（「现金」+CASH 只出一行、现金笔数=2）
  - `ReportApiControllerTest.dailySalesBucketsPaymentMethodAfterNormalization`（CASH 也进现金桶）
  - `CheckoutConsistencyPolicyTest` 增加"支付方式分组统计必须归一化合并"断言

### 剩余

- 历史数据里若已存在 `CASH` 等写法，聚合已能正确合并，但**不建议**改写历史行（口径已统一，无需迁移）。

---

## TD-003 已退款交易仍计入营业额（毛/净口径未定）

**类别**：正确性（口径）　**状态**：**已修复（2026-09）**，口径已定：**净额**
**提出来源**：2026-09 全量审计（数据层）

### 现状（修复前）

退款只写 `return_orders`，不写负交易；`TransactionDAORefactored.getStatistics`、日报/月报、交班、
桌面统计都 `SUM(final_amount)` 且**不过滤 `status = 'REFUNDED'`**，桌面退货金额也完全不从营业额里扣，
所以已退款的销售仍算营业额。

### 口径（2026-09 定稿）

- **销售额（totalAmount）**＝ 区间内**未整单退款**（`COALESCE(status,'NORMAL') <> 'REFUNDED'`）交易的实付合计；
- **退款额（refundedAmount）**＝ 区间内**已完成**退货单的金额，按 `completed_date` 归属
  （退货是在完成那一刻把钱退回去的，按完成时间才与钱箱/交班对得上），**排除**原交易已被整单标记
  `REFUNDED` 的退货单（那种交易已从销售额剔除，再减一次就是重复扣减）；
- **净营业额（netAmount）**＝ 销售额 − 退款额；
- **分渠道桶**：先按归一化方式对有效销售分桶，再按**退款方式**冲减对应渠道
  （现金退款减现金桶，退回会员余额只影响总额），退货发生在收款之前时允许为负。

### 修复（2026-09）

- `TransactionDAORefactored`：`getStatistics` / `getTotalRevenue` / `getTransactionCount` /
  `getPaymentMethodStats` 全部剔除 `REFUNDED`。
- 新增 `ReturnOrderDAORefactored.findCompletedReturnsBetween(start, end)`（含 NOT EXISTS 去重规则）；
  纯函数 `TransactionService.returnsByMethod / returnsByDay / totalReturns`。
- 消费方全部按上述口径：`ReportApiController` 日报/月报（新增 `refundedAmount` / `netAmount`，
  月报日趋势按退货完成日冲减，保证 Σ日趋势 == netAmount）、`StatisticsController`、
  `ShiftController`（交班净额 + 现金桶扣现金退款）、`ProfitReportController`（已退款交易不计利润/毛利）。
- `TransactionApiController.createRefundReturnOrder` 补写 `completedDate`（此前 API 退款写的退货单
  `completed_date` 为 NULL，与桌面流程不一致）。
- **门禁**（均做变异验证）：`ReportApiControllerTest.dailySalesIsNetOfRefundedTransactionsAndCompletedReturns`
  / `monthlySalesNetMatchesDailyTrend`、`TransactionDAOTest.refundedTransactionsAreExcludedFromRevenueAggregates`、
  `TransactionServiceTest.returnsAreGroupedByNormalizedMethodAndCompletionDay`。

### 剩余（有意保留）

- **销量/库龄类口径仍为毛**：热销榜 `getTopProducts`、`InventoryReportController` 的周转率按"卖出去多少"
  统计，不剔除已退款交易——它们是选品/周转信号而非收入；改动会影响排行语义，未并入本轮。
- 交易列表/明细仍展示已退款交易（供审计），仅金额聚合剔除。

---

## TD-005 触屏收银台状态栏未接线，提示在界面上不可见

**类别**：UI 缺陷　**状态**：**已修复（2026-09）**
**提出来源**：`StatusBarSeverityPolicyTest` 注释中的既有待修项（CLAUDE.md 亦已记录）

### 现状（修复前）

`TouchCartView.fxml` 底栏只有品牌/班次/日期/时间，**没有状态文本控件**，也不绑定
`StatusBarManager.statusLevelProperty()`；而 `TouchCartController` 有 ≥10 处 `StatusBarManager.update*`，
其中商品/分类/热销加载失败三处**没有弹窗兜底** → 触屏版报错完全不可见（商品区空白且无解释）。

### 修复（2026-09）

- `TouchCartView.fxml` 底栏新增 `fx:id="statusLabel"`（`HBox.hgrow="ALWAYS"`，初值 `%status.ready`）；
- `TouchCartController` 新增 `bindStatusBar()`（`initialize()` 调用）：绑定
  `StatusBarManager.statusProperty()`，按级别套 `text-success/text-warning/text-danger`
  （与 `MainController.applyStatusLevelStyle` 同一套样式类），进场重置为"就绪"；
- `cleanup()` 解除绑定与监听，避免静态单例强引用整棵旧触屏界面；
- 门禁 `StatusBarSeverityPolicyTest.shellControllersApplySeverityStyleClasses` 扩展到触屏控制器
  与 FXML（要求绑定表达式、`initialize()` 调用、`cleanup()` 解绑、FXML 存在状态控件；已做变异验证）。

---

## TD-004 REST API 校验弱于桌面端（会员/支付方式/operator）

**类别**：安全 / 正确性　**状态**：**已修复（2026-09）**
**提出来源**：2026-09 全量审计（API）

### 现状（修复前）

- `MemberApiController.update` 直接赋值 `name/phone/level/discount`，无 0..10 范围校验
  （桌面端 `MemberEditController` 有）、`phone` 唯一冲突 → 500，乐观锁冲突也被兜成 500；
- `POST /api/transactions` 的 `paymentMethod` 只判非空（**已于 TD-002 修复**）；
- `POST /api/payment/create` 只校验 `amount > 0`，不与交易 `finalAmount` 交叉校验：
  认证用户可以用 0.01 元为一张 1000 元的交易生成**真实收款码**；
- `PaymentApiController` 的 `operator` 取自请求体且在 INSERT **之后**赋值 → `payment_orders.operator`
  恒为 NULL；`BackupApiController` 的 `operator` 取自请求体并落库 → 备份归属可伪造。

### 修复（2026-09）

- **会员更新校验对齐桌面端**（`MemberApiController.update`）：姓名非空白、手机号 11 位数字、
  等级必须是 `普通/银卡/金卡/钻石`（新增 `MemberService.isKnownLevel`）、折扣 0..10；
  非法值回 **400** 并说明原因。同时补 `discountRate = discount`（桌面端本来两边都写，接口此前只改一个）。
- **乐观锁语义**：新增 `MemberDAORefactored.OptimisticLockException extends SQLException`
  （既有按 `SQLException` 捕获的调用方行为不变），接口单独捕获并回 **409**。
- **支付金额交叉校验**（`POST /api/payment/create`）：交易必须存在（否则 404）、
  不得是 `REFUNDED`（400）、`amount` 必须等于该交易 `finalAmount`（400）。
  ⚠️ **契约变更**：以前可以用任意 `transactionId`/金额建单，现在必须先经 `POST /api/transactions`
  落一笔交易、且金额与实付一致；桌面两个收银台不走这个端点（直接调 `PaymentService`），不受影响。
- **操作员一律取认证用户**：`PaymentApiController` / `BackupApiController` 改为
  `ctx.attribute("currentUser")`，忽略请求体 `operator`；`PaymentService.createPaymentOrder`
  增加 `operator` 参数并**在 insert 之前**赋值，`payment_orders.operator` 不再恒为 NULL。

### 门禁（全部做过变异验证）

- `MemberApiControllerTest.updateMemberValidatesFieldsLikeDesktop`（折扣/等级/手机号/姓名非法值 → 400，
  合法值成功且 `discountRate` 同步）
- `MemberDAOTest.staleVersionThrowsTypedOptimisticLockException`（`version` 未命中抛专用异常）
- `PaymentApiControllerTest.createPaymentValidatesTransactionAndAmount`（交易不存在 → 404、
  金额不一致 → 400）、`createPaymentInMockModeSucceeds`（`payment_orders.operator` 落库为认证用户，
  请求体自报的 operator 被忽略）
- `OperatorIdentityPolicyTest`（3 项源码门禁：三个写审计归属的控制器必须取认证上下文、
  支付单操作员必须在 insert 之前赋值、会员冲突必须映射 409）

### 剩余

- 无（本条全项完成）。`POST /api/members` 仍允许 cashier 调用——那是角色策略问题，见「补充」。

---

## 补充：`POST /api/members` 的角色策略（待产品决定，未登记为新条目）

会员 CRUD 对 `cashier` 开放（充值/建档是收银台日常操作），但 `PUT /api/members/:id`
可直接改等级与折扣——当前校验只保证"值合法"，不保证"该角色有权改折扣"。
若要收紧，建议在 `AuthorizationMiddleware` 里把"改折扣/等级"限定为 finance/admin，
并与桌面端 `MemberEditController` 的可见性对齐。

---

## TD-006 非收银台控制器仍在 FX 线程同步查库（约 18 个）

**类别**：性能 / 体验　**状态**：**部分修复（2026-09）**——页面级加载 + 报表链路已改完，部分弹窗/处理器仍待办
**提出来源**：2026-09 全量审计（UI）

### 现状（修复前）

CLAUDE.md 的"查库统一后台化"只覆盖两个收银台（`FxThreadDbPolicyTest` 也只断言它们）。
已抽验：`InventoryController.initialize → loadTableData → productDAO.findAll`、
`ShiftController.setCurrentUser → loadShifts → shiftDAO.findRecent` 等都是 FX 线程同步查询，
打开这些页面会冻结界面；登录 → 主界面切换另有 3~5 次同步读。

### 已修复（2026-09，页面级加载共 18 个方法 / 9 个控制器）

统一走 `UIOptimizer.runInBackground`（后台查库 + 成功/失败回调都在 FX 线程），
并顺带修正两处"后台线程读界面控件"的隐患（`TransactionController` 的日期选择器、
`PurchaseReport` 之外的筛选条件一律在提交前取值）：

- `InventoryController`：`loadTableData`、`loadCategories`（并抽出 `refreshInventoryTable()` 只做渲染）
- `SupplierController`：`loadSuppliers`、`handleSearch`
- `PromotionController`：`loadPromotions`
- `TransactionController`：`loadTransactions`、`applyFilters`（查询窗口在 FX 线程取好后再提交，
  抽 `renderTransactions()` / `applyFiltersAndRender()`）
- `ShiftController`：`loadShifts`（收银员过滤条件在 FX 线程取好；抽 `renderShifts()`）
- `PurchaseOrderController`：`loadSuppliers`、`loadOrders`（供应商表到齐后重刷一次，避免解析不到供应商名）
- `PurchaseApprovalController`：`loadPendingOrders`、`loadAllOrders`、`updateCountLabel`
  （"待审批"计数本身就是一次聚合查询，也改后台）
- `PurchaseInboundController`：`loadApprovedOrders`（原本还有 N+1 次按订单查明细）
- `ProductEditController`：`loadCategories`、`loadUnits`、`loadSuppliers`
- `SearchController`：`performSearch`（每次按键一次全库搜索；加 `searchSequence` 丢弃过期结果）
- 顺带核实：`MemberController.loadTableData`、`UserController.loadUsers` **本来就是**
  `new Thread + Platform.runLater`，无需改动（审计清单里的这两条是误报）

门禁 `FxThreadDbPolicyTest.nonPosPageLoadsRunOffTheFxThread`：按**方法体**逐条断言
"必须出现 `UIOptimizer.runInBackground(` + 查库语句仍在方法内 + 不得再有 `catch (SQLException)` 同步写法"，
18 条全部登记（已做变异验证：把 `InventoryController.loadTableData` 改回同步 try/catch 即变红）。

### 已修复（2026-09，第二批：报表链路 + 退货审批明细）

报表页此前是"点一次按钮同步查两次库再聚合渲染"，本批把**查询**与**聚合/渲染**拆开：

- `InventoryReportController.calculateStatistics`：原先自己同步调 `loadProductsForReport` +
  `loadTransactions`；现改为 `UIOptimizer.runInBackground(() -> loadReportData(...), data -> renderStatistics(...))`，
  新增后台 loader `loadReportData(categoryName, startDate, endDate)`（返回 `ReportData` 记录：
  商品 + 区间交易）与 FX 线程渲染方法 `renderStatistics(...)`；原先两个同步加载方法已删除（本次改造的孤立产物）
- `PurchaseReportController.handleQuery`：订单 + 明细两个查询进后台 loader
  `loadReportData(startDate, endDate)`（返回 `ReportData`：订单 + 按订单分组的明细），失败返回 `null`；
  筛选 `filterOrders` 与统计 `calculateStatistics` 回 FX 线程。
  `loadData()`（页面初始化）的供应商查询同样放后台，下拉框回 FX 线程填充
- `ProfitReportController`：主体加载**本来就是** worker + `Platform.runLater`，但
  `calculateStatistics` 在 FX 线程渲染时仍会调 `loadOperatingCostRatio()`（查 `system_settings`）——
  现在该设置由 worker 预先查好、以参数传入渲染方法（**这是本批唯一真正"FX 线程查库"的报表问题**）
- `ReturnApprovalController.loadOrderItems`：点开一张待审批退货单查明细，改为后台 + 回 FX 线程填表

门禁（均已变异验证）：

- `FxThreadDbPolicyTest.nonPosPageLoadsRunOffTheFxThread` 表从 18 条扩到 **22 条**
  （新增库存报表 `calculateStatistics`、采购报表 `handleQuery`/`loadData`、退货审批 `loadOrderItems`）
- 新增 `FxThreadDbPolicyTest.reportLoadersAreOnlyCalledAsBackgroundTask`：后台 loader 里必须有查库语句、
  且只能以 `() -> loadXxx(...)` 的形式被提交一次
- 新增 `FxThreadDbPolicyTest.profitReportDoesNotQuerySettingsOnTheFxThread`：`calculateStatistics`
  不得出现 `loadOperatingCostRatio()`，且 worker 必须先取好比例再提交渲染

变异验证：① 把两个报表文件整体回退到改动前（`git show HEAD:` 的同步版本）→ 两条新门禁 + 表内 4 行同时变红；
② 把利润报表的 `operatingCostRatio` 参数换回现场查库 → `profitReportDoesNotQuerySettingsOnTheFxThread` 变红；
③ 把退货审批明细改回同步 `itemList.addAll(dao...)` → 表内该行变红。

### 已修复（2026-09，第三批：其它点击后短查询）

- `ReturnOrderController.loadReturnOrderItems`：点开退货单查明细 → 后台 + 回 FX 线程填表
- `ReturnApprovalController.loadOrderItems`（第二批已改）、
  `SupplierController` 自动生成供应商编号（`generateSupplierCode()` → 异步
  `applyGeneratedSupplierCode(codeField)`：查库放后台，回 FX 线程填输入框；
  顺带去掉"查询失败返回 0"的旧行为——那会生成与已有编号可能冲突的 `S...0001`）
- `InventoryController` 分类/单位管理弹窗的数据加载 → 抽出
  `loadCategoryManagementData(target)` / `loadUnitManagementData(target)` 两个后台 loader
  （弹窗自身的表格与列表仍在 FX 线程持有，编辑/删除弹窗继续用同一个 `ObservableList`）
- `MemberController.handleSearch`：会员搜索（全表 LIKE）→ 后台 + 回 FX 线程填表与计数
- `ShiftController.updateShiftButtonStates`：活跃班次查询 → 后台 + 回 FX 线程切换按钮可用性
  （失败时按"没有活跃班次"处理，与改造前一致）

门禁：`nonPosPageLoadsRunOffTheFxThread` 表再扩到 **27 条**。
变异验证：① 退货单明细改回同步 → 该行变红；② 分类弹窗 loader 改回同步（带 try/catch）→ 该行变红；
③ 会员搜索改回同步 → 该行变红；④ 班次按钮状态改回同步 → 该行变红。
（两次变异脚本曾因漏 catch checked 异常而**编译失败**——编译失败不算有效变异，已改成可编译版本重跑。）

### 仍待处理（约 6 处）

- `PurchaseInboundController` 明细/入库单弹窗、`PurchaseOrderController` 商品选择器与订单详情弹窗
- `PromotionController` 弹窗内的促销循环（762-777）
- `UserController` 搜索/停用/重置口令等处理器（写操作 + 紧随刷新）
- `ShiftController.handleEndShift` + `loadShiftTransactions`：**交互流程**（查活跃班次 → 确认框 → 落库），
  要后台化得把整条流程改成回调链，单独评估
- `MemberController` / `UserController` 的**写操作**（insert/update/delete）：单行写 + 紧随刷新，
  转换会改变错误提示与刷新时序，收益低，暂不动

列表里其余"查询"均已改完；这些剩余项要么含交互流程、要么是写操作。

## TD-010 docker/mysql-init 的「完整初始化」与 Java 建表漂移

**类别**：运维　**状态**：**已修复（2026-09）**
**提出来源**：2026-09 全量审计（表级对比）

### 现状（修复前）

`docker/mysql-init/00-init-complete.sql` 被当作"完整初始化"给 DBA/BI 用，但实际（用脚本逐表比对
`DatabaseManager` + 各 DAO 的 `createTable()`）差异比审计当时描述的更精确：

- **缺 9 张表**：`invoices`、`invoice_items`、`backup_config`、`backup_records`、
  `login_attempts`、`settings`、`payment_orders`、`refund_records`、`hold_orders`；
- **多 5 张全仓库无 Java 引用的表**：`specifications`、`specification_values`、
  `product_specifications`、`export_history`、`export_templates`；
- **3 处列集合不同**：`categories.created_at`、`units.created_at`（脚本有、Java 无）、
  `return_order_items.create_time`（Java 有、脚本无）；
- **12 处列类型不同**（脚本普遍偏窄）：`units.name` VARCHAR(20) vs 50、
  `return_orders.reason/approval_comment` VARCHAR(500) vs TEXT、
  `return_orders` 的 5 个时间列 DATETIME vs TIMESTAMP 等；
- 脚本尾部还有一段重复的"初始化脚本信息"块，脚本自述版本停在 v2.5.9（应用已是 2.6.0）。

### 修复（2026-09）

以 Java 侧为唯一可执行事实来源（应用启动时 `CREATE TABLE IF NOT EXISTS` 才是真正定义）：

- **补建 9 张表**：DDL 由脚本从 `DatabaseManager` / `PaymentDAORefactored` /
  `HoldOrderDAORefactored` 的建表语句**原样提取**（不手抄），插入到脚本的
  "v2.5+ 表（补建）"区段，并在区段头注明"请勿手改，一致性由 `InitSchemaParityTest` 守着"；
- **列集合对齐**：删掉 `categories/units` 的 `created_at`，给 `return_order_items` 补 `create_time`；
- **12 处列类型对齐到 Java**（`units.name` → 50、`return_orders.reason` → TEXT、时间列 → TIMESTAMP 等）——
  只改初始化脚本，不影响既有库；
- **5 张历史遗留表显式标注**：在脚本里加注释说明"当前 Java 侧已无引用，保留只为兼容旧库/BI 取数"，
  并与门禁的 `LEGACY_TABLES` 白名单互相印证；
- 顺带删除尾部重复的"初始化脚本信息"块，脚本自述版本更新为 v2.6.0。

### 门禁（5 项，全部做过变异验证）

`com.cashier.util.InitSchemaParityTest`（解析 Java 源码与 `docker/mysql-init/*.sql` 的建表语句）：

1. `initScriptCreatesEveryJavaTable`：Java 侧会建的每张表脚本里都必须有；
2. `commonTablesHaveSameColumnsAndTypes`：共有表的列集合**与列类型**都必须一致
   （类型比较前做大写/去空格规范化）；
3. `legacyWhitelistMatchesReality`：脚本里"Java 侧没有的表"必须与 `LEGACY_TABLES` 白名单完全一致
   （新增无引用表必须显式登记）；
4. `legacyTablesHaveNoJavaReference`：白名单里的表确实没有任何 Java 引用（`FROM/JOIN/INTO/UPDATE/TABLE`）；
5. `initScriptStillContainsCoreTables`：15 张核心表必须在（防止脚本被截断或回退成旧版本）。

变异验证：① 删掉脚本里的 `settings` 表 → 1、5 变红；② 删掉 `payment_orders.terminal_id` 列 →
2 变红；③ 加一张未登记的 `legacy_undocumented` 表 → 3 变红；④ 在 Java 里引用 `specifications` →
4 变红；⑤ 把 `units.name` 类型改回 VARCHAR(20) → 2 报出类型不一致。五处均确认后还原。

### 剩余

- **6 处索引命名/二级索引差异**（不影响列结构与约束语义，未对齐）：
  `categories`/`units`（脚本多 `idx_name`）、`operation_logs`（索引名不同且脚本多 `idx_log_level`）、
  `return_orders`/`return_order_items`（各多对方没有的二级索引，如 Java 的 `idx_original_transaction`）、
  `products`（Java 用命名约束 `uk_product_name`、脚本用内联 `UNIQUE`，**语义相同**）。
  这些只影响索引命名与个别查询的索引可用性；如需完全一致再逐个对齐索引定义。

---

## TD-011 Windows 启动脚本把未匹配的 JAR 通配符当路径，静默跳过构建

**类别**：发布　**状态**：待处理
**提出来源**：2026-09 全量审计（脚本）

### 现状

`start.bat` / `install.bat`（以及 install.bat 生成的 DataConfig.bat）用
`for %%f in (target\lisuan-fx-*-jar-with-dependencies.jar) do set "JAR_FILE=%%f"`，
cmd 在**无匹配**时会把通配符原样当成一项 → `JAR_FILE` 非空 → `if "%JAR_FILE%"==""` 永远为假
→ 跳过 `mvn clean package`，最终 `java -jar "target\...*..."` 报 `Unable to access jarfile`。
`start.sh` 用 `[ ! -f "$JAR_FILE" ]` 判断，是对的（Windows/Linux 行为不一致）。

### 为什么现在不修

需要一台 Windows 实测（本轮在 macOS 上只能按 cmd 语义推断），且要先确定"自动构建"在 Windows
上是否是期望行为（有些用户希望脚本不要偷偷跑 maven）。

### 触发条件

Windows 干净机器首次运行脚本、或发布前验证脚本门禁时。

### 建议方案

匹配后用 `if exist "%%f"` 校验（或 `for /f` + `dir /b` 判断是否真的存在），再决定构建；
并补一条"脚本必须执行到构建/启动分支"的回归门禁（参照 release.bat 的 `[3/3]` 思路）。

---

## TD-012 install.sh / docker-init.sh 静默成功与占位口令

**类别**：运维　**状态**：待处理
**提出来源**：2026-09 全量审计（脚本）

### 现状

- `install.sh:261` `mysql ... < 00-init-complete.sql 2>/dev/null || true` 之后直接打印
  `[Done] Database initialization completed` → 导入失败也报成功（`set -e` 被 `|| true` 吃掉）；
- `docker/docker-init.sh` 从不读 `.env`，会在只做了 `cp .env.example .env` 的机器上退化成
  占位口令，并在用户答 y 后 `ALTER USER ... IDENTIFIED BY 'YOUR_CASHIER_PASSWORD_HERE'` 且写回 `.env`；
- `docker/start-mysql.sh` 硬编码容器名 `lisuan-mysql`，与 compose 的可配
  `MYSQL_CONTAINER_NAME` 不一致 → 自定义名时误报"启动失败"；
- `.env.example`/`docs/CREDENTIALS_CHECKLIST.md` 里的 `DB_USE_SSL` 无人实现（实际由 `db.url` 的
  `sslMode` 决定）。

### 为什么现在不修

属安装/运维脚本批次，需在 Linux + Docker 环境实测；本轮聚焦应用侧正确性。

### 触发条件

新环境部署、或出现"装完报成功但库是空的/连不上"的工单。

### 建议方案

去掉 `|| true` 并对导入结果判错；`docker-init.sh` 先 `source .env`；容器名统一读同一变量；
删除或实现 `DB_USE_SSL`。

---

## TD-013 版本号门禁只覆盖 4 处中的 2 处

**类别**：发布　**状态**：**已修复（2026-09）**
**提出来源**：2026-09 全量审计（发布脚本）

### 现状（修复前）

`release.sh` / `release.bat` 的版本一致性门禁只比较 `pom.xml` 与 `AppConstants.APP_VERSION`；
`src/main/java/com/cashier/installer/Installer.java`（安装包标题会显示）与 `.env.example`
（部署脚本据此展示）未纳入。而且发布脚本只在本地发版时跑，**CI 看不到**。

### 修复（2026-09）

- **两个发布脚本都扩到四处比对**：`pom.xml` / `AppConstants.java` / `installer/Installer.java` /
  `.env.example`，任一处不同即 `exit 1` 并打印两边的值（`release.sh` 用 for 循环，`release.bat` 用
  `for /f` + `tokens delims==` 解析同一份比对清单）
- **同一个不变量加进 CI**：新增 `com.cashier.constant.VersionConsistencyTest`（随 `mvn verify` 跑）：
  - `versionIsConsistentAcrossAllSources`：四处版本号必须相等，失败时把四处的实际值一起报出来；
  - `releaseScriptsCheckEverySource`：两个脚本的**比对清单**里必须出现
    `installer/Installer.java:$VERSION_INSTALLER` 与 `.env.example:$VERSION_ENV`
    （只"提到"来源、不参与比对不算——第一版门禁就是这么写的，变异测试当场发现它抓不住）

### 门禁（做过变异验证）

① `Installer.java` 改成 `9.9.9` → `versionIsConsistentAcrossAllSources` 报出四处实际值；
② 把两个脚本比对清单里的来源换成假的（读取处保持原样）→ `releaseScriptsCheckEverySource` 变红；
③ 直接跑 `release.sh` 的检查段：`/tmp` 版脚本在 `.env.example` 被改成 `9.9.9` 时 `exit 1`
并打印 `pom.xml: 2.6.0` / `.env.example: 9.9.9`，正常时 `exit 0`。

---

## TD-014 i18n 硬编码与文档中的测试数过期

**类别**：文档 / 体验　**状态**：**部分修复（2026-09）**
**提出来源**：2026-09 全量审计（UI/i18n + 文档）

### 已修复（2026-09）

- **文档里的测试数不再写死**：README 头部与正文、`AGENTS.md` 原来写死 `589 / 606 / 647`，
  实际早已不同。现在统一改成"`mvn -q clean verify` 全绿 + 数量以构建输出 `Tests run:` 为准"
  （写死数字必然过期，是每批修复都会踩的坑）。
- **`I18nUiUtils` 的大小写折叠改用 `Locale.ROOT`**（`I18nUiUtils.java` 4 处
  `toLowerCase()/toUpperCase()`）：土耳其语环境下 `"CHECKING".toLowerCase()` 会得到
  `checkıng`（无点 ı），落库值 `CASH`/`PENDING`/`CHECKING` 永远匹配不上、界面回退成英文原值。
  门禁 `I18nUiUtilsTest.caseFoldingIsLocaleIndependent`（把默认 locale 切成 `tr_TR` 再断言；
  改回默认 locale 即变红，实测返回 `CHECKING` 而不是 `盘点中`）。
- **澄清一条误报**：审计说"`CurrencyUtil` 小数分隔符随默认 locale"——实际它显式用
  `DecimalFormatSymbols(Locale.SIMPLIFIED_CHINESE)`，不受默认 locale 影响。

### 仍待处理

- **可见文案硬编码**：`MainController` 备份/恢复对话框、`RechargeController` 支付方式与校验文案、
  `PrintPreviewDialog`、`SplashWindow`、`PackageWizardController`、`CashierSystemFXApplication`
  的错误弹窗标题、`InventoryView.fxml` 的 `promptText="全部"`。
  ⚠️ 改 `RechargeController` 时注意：`"现金"/"微信"…` **同时是落库的支付方式规范值**，只能改显示层。
  门禁可参照 `TouchCashDialogI18nTest` 对 `TouchCartViewFactory` 的做法（指定文件不得出现中文 UI 字面量）。
- 159 个无人引用的 bundle key（清理需先确认没有 FXML/反射引用）。
- `AGENTS.md` 未被 git 跟踪（`.gitignore:38`）——本机使用没问题，但换机器就丢；是否入库待定。

---

## TD-015 `String.format` 用默认 locale 格式化金额（73 处）

**类别**：正确性（潜在）　**状态**：待处理
**提出来源**：2026-09 全量审计（TD-014 顺带发现）

### 现状

主源码里有 73 处 `String.format("...%.2f...", ...)` 未指定 `Locale`，`String.format` 默认用
`Locale.getDefault(FORMAT)`。目标环境是中文（`.` 作小数分隔符）所以**当前不会出问题**；
但在德语/法语等默认 locale 的机器上，小票、弹窗、导出的金额会打成 `1,50`——
小票是给人看也用于核对的，导出文件更可能被再次解析（`Double.parseDouble("1,50")` 直接抛异常）。

### 为什么现在不修

73 处一次性替换是机械但面广的改动，且要看每处用途（小票/详情/导出/日志风格各不相同），
适合单独一批做；`CurrencyUtil` 已经显式锁定了 `Locale.SIMPLIFIED_CHINESE`，主要路径（界面金额）
本身是安全的。

### 触发条件

部署到非中文默认 locale 的机器、或导出文件需要跨语言环境解析。

### 建议方案

统一走 `String.format(Locale.ROOT, ...)`（或已有的 `CurrencyUtil.format`），
优先改 `ReceiptPrinter`、导出工具与被再解析的字符串；配套一条"`src/main` 内不得出现
未指定 Locale 的 `%f` 格式化"的门禁。
