# 技术债清单

记录已识别但**有意推迟**的技术问题：每条都说明为什么现在不做、什么条件下必须做。
新条目请沿用同样的小节结构，并在表格中登记。

| ID | 标题 | 类别 | 状态 | 关联需求 |
|---|---|---|---|---|
| TD-001 | [安全预留] 发票文件路径白名单校验 | 安全 | **门禁已就位（2026-09）**：行为未改；一旦有人按路径读取且未同批校验，CI 立即失败（2 项门禁 + 变异验证） | 发票预览/下载需求 |
| TD-002 | 支付方式未在落库前归一化，报表/交班对不上账 | 正确性 | **已修复（2026-09）** | 支付方式口径统一 |
| TD-003 | 已退款交易仍计入营业额（毛/净口径未定） | 正确性 | **已修复（2026-09）** | 报表口径 |
| TD-004 | REST API 校验弱于桌面端（会员/支付方式/operator） | 安全/正确性 | **已修复（2026-09）** | API 加固 |
| TD-005 | 触屏收银台状态栏未接线，提示在界面上不可见 | UI 缺陷 | **已修复（2026-09）** | 触屏收银台 |
| TD-006 | 非收银台控制器仍在 FX 线程同步查库 | 性能/体验 | **已修复（2026-09）**：页面级加载 + 报表链路 + 点击后短查询 + 交班结算（唯一无界查询），登记表 28 条 + 交班专项 11 条断言；其余剩余项经复核明确接受（有界查询/单行写，附重估条件） | 界面卡顿 |
| TD-007 | 结账 worker 线程改 FX 侧共享集合 | 并发 | **已修复（2026-09）** | 收银台线程纪律 |
| TD-008 | 标准收银台成功弹窗用结账后会员重算金额 | 正确性 | **已修复（2026-09）** | 结账口径 |
| TD-009 | H2 测试库无外键（生产 14 个），外键类缺陷测不出 | 测试基建 | **已修复（2026-09）** | 回归门禁 |
| TD-010 | docker/mysql-init 的「完整初始化」与 Java 建表漂移 | 运维 | **已修复（2026-09）**：补 9 表、对齐列与类型、5 张遗留表标注，5 项门禁 | 数据库初始化 |
| TD-011 | Windows 启动脚本把未匹配的 JAR 通配符当路径，静默跳过构建 | 发布 | **已关闭（2026-09）**：检测/构建/版本显示/代码页/退出码五项均 Windows 实机验证通过 | Windows 启动 |
| TD-012 | install.sh / docker-init.sh 静默成功与占位口令 | 运维 | **已修复（2026-09）**：失败可见 + 口令硬守卫 + 容器名同源 + 删未实现开关，4 项门禁 | 安装脚本 |
| TD-013 | 版本号门禁只覆盖 4 处中的 2 处 | 发布 | **已修复（2026-09）** | 版本管理 |
| TD-014 | i18n 硬编码与文档中的测试数过期 | 文档/体验 | **已修复（2026-09）**：可见文案迁完（13 文件门禁）+ 无用 key 清零（-196 key / -60 常量）+ AGENTS.md 入库与一致性门禁；打包向导**明确不译**（开发者工具，POS 界面零引用） | — |
| TD-015 | `String.format` 用默认 locale 格式化金额（73 处） | 正确性 | **已修复（2026-09）**：73+5 处固定 `Locale.ROOT`，3 项门禁 + 德语 locale 全量验证 | 非中文 locale 部署 |
| TD-016 | 界面集合字段未初始化，异步回调顺序一变就 NPE | UI 正确性 | **已修复（2026-09）**：14 个控制器 / 25 个字段改为声明即初始化 + 2 项门禁（Windows 实测发现） | 异步加载页面 |
| TD-017 | 会员「金额类字段」的修改权限 | 权限/产品策略 | **已修复（2026-09）**：桌面端按角色锁定等级/折扣/积分/余额（原来只有 API 受限，桌面端更宽松——旧结论写反了）；两条路径补等级/折扣变更审计日志；3 项门禁 + 5 处变异 | 会员权限策略 |
| TD-018 | 会员充值的角色口径两侧不一致 | 权限/产品策略 | **已修复（2026-09）**：按"收银员可充值"统一（充值有 RechargeRecord 流水留痕），金额类字段仍限 finance/admin；2 项门禁 + 变异验证；余"PUT 字段级"待定（API 更严，无害） | 充值权限策略 |
| TD-019 | 商品管理页数量显示露出未替换的 `{0}` | 正确性/体验 | **已修复（2026-09，用户实测发现）**：`get(key)` 漏传参；新增"占位符必须传参"门禁（1 项 + 变异验证） | 界面文案 |
| TD-020 | 深色模式下硬编码亮色背景的块 | 体验/主题 | **已修复（2026-09）**：交班页橙色横条（顶部栏被覆盖成饱和橙 + 分隔条无覆盖）→ 深品牌色/中性色；随后做通用排查又修 `.error-label`/`.validation-error`/`.product-info-card` 等 6 处；门禁改为"硬编码亮色背景的简单类必须被深色主题覆盖"（+变异验证） | 深色主题 |
| TD-021 | FXML 的 `%key` 引用了带占位符的 key，界面显示 `{0}` | 正确性/体验 | **已修复（2026-09，由 TD-019 顺藤摸瓜发现）**：13 个标签改为空文本 + 删掉 10 个无用 key；门禁 4 项（含"变量 key"与兼容映射表两个盲区，均变异验证） | 界面文案 |
| TD-022 | F8/命令面板进收银台后导航高亮消失（`checkoutBtn` 在 FXML 里根本不存在） | UI 正确性 | **已修复（2026-09）**：删陈旧字段 + `handleCheckout` 委托 `handleCart()`；新增 FXML↔控制器绑定门禁（3 项 + 4 处变异） | 界面绑定 |
| TD-023 | 盘点单保存/删除非原子：一次失败会清空明细 | 正确性/数据丢失 | **已修复（2026-09）**：表头+明细（新建/编辑/删除）统一进事务 + 拒绝空明细 + 明细加载失败可见；门禁 2 项 + 变异 | 库存盘点 |
| TD-024 | 退款还原库存丢弃 UPDATE 结果；相对增减库存不递增 version 被并发覆盖 | 正确性/并发 | **已修复（2026-09）**：消费返回值并留 WARN；`updateQuantityWithConnection` 补 `version = version + 1`（退款/入库/盘点三处受益）；门禁 + 行为测试 | 库存与退款 |
| TD-025 | 手工开票表头与明细不在同一事务（会留"有金额无明细"的孤儿发票） | 正确性 | **已修复（2026-09）**：改走 `insertWithConnection` + 事务；门禁 + 变异 | 发票 |
| TD-026 | 建库/建表/迁移失败被 catch 吞掉（启动看似成功，随后页面全报 SQL 错） | 运维/可诊断性 | **已修复（2026-09）**：`initializeDatabase` 抛 `SQLException`，由静态块转成"初始化失败 + 排查指引"；门禁 + 变异 | 启动排障 |
| TD-027 | `ProductDataImporter` 的 ZIP 分支必抛 `Stream closed`（已实测复现），`importFromGitHub` 零调用方 | 死代码/潜在缺陷 | **待产品决定（2026-09）**：功能是否保留——删死代码，或修 ZIP 解析（关闭 BufferedReader 会连带关掉 ZipInputStream） | 是否还要"从 GitHub 拉商品数据" |
| TD-028 | Apache POI 5.2.5 受 CVE-2025-31672 影响（poi-ooxml < 5.4.0） | 依赖安全 | **已评估（2026-09）**：本仓库只用 POI **写** xlsx，全仓库没有解析外部 Office 文件的路径 → 该 CVE 不可达；升级到 ≥5.4.0 属加固，非必须 | 依赖升级窗口 |
| TD-029 | 月报/任意区间报表用 `findByDateRange(start,end)` 全量 JOIN 物化到内存 | 性能 | **待处理（2026-09 审计发现）**：列表接口已有 `limit` 重载，报表侧未用 | 大数据量门店 |
| TD-030 | 首次登录改密对话框取消后，登录界面永久禁用（只能杀进程） | UI 缺陷 | **已修复（2026-09）**：改密对话框返回后，凡未真正切到主界面（取消/关窗/改密失败/无 application）一律 `setLoginState(false)`；1 项门禁 + 变异验证 | 强制改密 |
| TD-031 | 网关下单在 FX 线程且 HttpClient 无任何超时 → 收银台可无限卡死 | 体验/健壮性 | **已修复（2026-09）**：两个渠道都加 `connectTimeout(5s)` + 请求 `timeout(15s)`；两个收银台的下单改走 `UIOptimizer.runInBackground`（异步窗口内置 `paymentInProgress` 防重复提交）；2 项门禁 + 变异验证 | 电子支付 |
| TD-032 | FX 线程同步查库若干处（选品框逐字符搜索、每次购物车变更查促销、登录时同步启动两个服务） | 性能 | **待处理** | 界面流畅度 |
| TD-033 | 触屏切语言泄漏 scheduler/Timeline/全局监听；`BackupService.start()` 无并发守卫 | 资源泄漏 | **待处理** | 触屏收银台 |
| TD-034 | 金额/数量口径一批（7 项：发票税额百分比 `setScale(0)` 抛异常、挂单折扣走 double、退款单价取整使 Σ明细≠实付、积分冲减按次取整、充值小数积分被编辑保存截断、`promotions.discount` 列精度与输入不匹配、发票行税与表头差 1 分） | 正确性 | **已修复（2026-09）**：7 项全部处理（退款改为"总额权威、明细只许少算"，积分/充值改 FLOOR）；4 项门禁 + 5 项行为测试，11 处变异全红 | 对账 |
| TD-035 | 其它（打包向导 FX 线程违规、`NotificationManager` 定时任务无 try/catch、`hasActiveShift` 吞异常让收银员看到"请先开班"、非 daemon 线程、`CurrencyUtil` HALF_EVEN…） | 杂项 | **待处理** | — |
| TD-036 | 结尾斜杠绕过**全部**角色门禁（收银员可退款/改会员折扣/改支付配置） | 安全 | **已修复（2026-09）**：`isAllowed` 先归一化末尾斜杠；回归测试覆盖 13 条受控路由 × {正常,`/`,`//`} + 变异验证 | 越权 |
| TD-037 | 收银员可用 `POST /api/printers/{id}/receipt` 的 `openCashDrawer` 打开钱箱（而 `/cashdrawer` 是管理员专属） | 安全/权限策略 | **待产品决定（2026-09）**：堵住这条等价路径，还是按"收银员本就要开钱箱找零"放开 `/cashdrawer`——两条路的业务含义不同，不擅自改 | 钱箱权限 |
| TD-038 | `POST /api/invoices/from-transaction` 请求体可自报开票方信息/`createBy`/`taxRate` | 安全 | **待处理（2026-09）**：同一条路由的 `PUT /api/invoices/seller-info` 是管理员专属，此处却可覆盖全局开票方并伪造开票人 | 发票 |
| TD-039 | `POST /api/invoices/{id}/print` 可写任意 `pdfPath`/`imagePath`；mock 支付模式回调密钥熵低 | 安全 | **待处理（2026-09，低危）**：路径字段目前不被当文件读取（TD-001 门禁守着），影响限于数据伪造；mock 模式仅出现在本地未跟踪配置 | 发票/支付 |

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
>
> **已修（第十批）**：TD-012——`install.sh` 的建库/导入失败不再被 `|| true` 吃掉（改为报出 mysql
> 真实错误 + 退出非零），`docker-init.sh` 先读 `.env` 且占位口令硬失败（不再"答 y 继续"），
> `start-mysql.sh` 容器名统一走 `MYSQL_CONTAINER_NAME`，删掉无人实现的 `DB_USE_SSL`；
> 加 `InstallScriptPolicyTest`（4 项，含变异验证）并用桩脚本做了真跑行为验证。
>
> **已修（第十一批）**：TD-015——73 处字面量 + 5 处常量格式串的浮点格式化统一固定
> `Locale.ROOT`（小票、CSS `rgba()`、金额工具等），加 `LocaleFormatPolicyTest`（2 项）与
> `LocaleIndependenceBehaviorTest`（德语默认 locale 下走真实代码路径），
> 并额外验证整套 713 用例在德语 locale 下全绿。
>
> **已修（第十二批）**：TD-014 剩余——审计点名的可见文案全部迁入语言包（30 个新 key，四份语言包同步，
> I18nKeys 补常量），含备份/恢复对话框、充值支付方式显示层本地化（落库值不变）、打印预览、
> 启动画面、启动失败/字体缺失弹窗、`InventoryView.fxml` 的 `promptText`；
> 加 `HardcodedUiTextPolicyTest`（4 项，含变异验证），FXML 的两处运行时覆盖占位走带理由的白名单。
>
> **已修复（第十九批）**：TD-014 收尾——`AGENTS.md` 入库（含两处过时表述修正与 `README` 一处残留
> 测试数），新增 `InstructionsDocPolicyTest`（3 项，含变异验证）钉住"文档被跟踪 / 版本同步句写全四个来源 /
> 不写死测试数量"。
>
> **已修（第十八批）**：TD-014 无用 key 清理——精确判定后删除 196 个 key（四份包各 196 行）
> 与 60 个只指向它们的常量（含一个既存死常量），新增 `I18nUnusedKeyPolicyTest`（2 项，含变异验证）；
> 判定过程用 grep 做了独立复核，纠正了两次误判（常量链重复前缀、子串误报）。
>
> **已修（第十七批）**：TD-014 状态栏清零——7 个控制器 / 17 处 `updateStatus("中文")` 全部迁走，
> 全部复用既有 key（新增 0 个同值 key）；门禁 `MIGRATED_FILES` 扩到 13 个文件，
> 并当场又抓出 `SettingsController` 一处 `showError("…")` 漏网与一处"首参是三元表达式"的写法。
>
> **已修（第十五批）**：TD-016——Windows 实机跑出来的两次 NPE（库存页、采购订单页）根因是两个异步
> 加载器互相依赖、读字段时另一个还没赋值；14 个控制器 / 25 个集合字段统一改为"声明即初始化"，
> 加 `UiStateInitializationPolicyTest`（2 项，含变异验证），并给 `start.bat` 加 `chcp 65001` 让中文日志可读。
>
> **已关闭（第十四~十六批）**：TD-011——从"通配符 for 检测"这一个根因出发，实机验收又暴露并修掉了
> 三处相关问题（中文注释破坏批处理解析、`chcp` 影响同窗口后续命令、`install.bat` 覆盖已跟踪的
> `DataConfig.bat`），最终 5 步实机验收全部通过；共 10 项门禁，全部做过变异验证。
>
> **已修（第十四批）**：TD-011 静态部分——`start.bat`/`install.bat`/`DataConfig.bat`（含 install.bat
> 生成的 heredoc）的 JAR 检测从"for 迭代通配符 + 空串判断"改为 `for /f + dir /b` + `if not defined`；
> `start.bat` 按产品决定不再自动构建而是报错退出；install.bat 构建后补产出校验；
> 4 个 `.bat` 的工作区行尾从 LF 规范化回 CRLF；加 `WindowsScriptPolicyTest`（7 项，含变异验证）
> 与 cmd 语义的模型复现。**实机验收清单见该条目**（需 Windows）。
>
> **已修（第十三批）**：TD-014 状态栏收尾——`MainController` 的 26 处 `updateStatus("中文")`
> 与 2 处带参刷新提示全部迁走（**复用**导航与既有状态 key，净新增 0 个 key），
> 门禁的调用清单加入 `updateStatus`/`updateWarning`；同时把我上一批多建的 5 个同值 key 去掉、改为复用既有 key。

---

## TD-001 [安全预留] 发票文件路径白名单校验

**类别**：安全（预留加固）
**状态**：**触发条件门禁已就位（2026-09）** —— 行为未改（现状无实际可达路径）；
等「发票预览/下载」需求落地时**必须与该校验同一次实现**，门禁会强制这一点
**提出来源**：安全审计 L8

### 现状

`POST /api/invoices/{id}/print` 会把请求体里的 `pdfPath` / `imagePath` 原样持久化到
`invoices` 表：

- `InvoiceApiController.printInvoice` 读取 `pdfPath` / `imagePath`
- `InvoiceDAORefactored.updatePrintInfoWithConnection` 直接写库

这两个字段目前**没有任何地方会去打开或读取文件内容**，所以现在不构成漏洞，
只是一个"写进去的任意字符串"。一旦将来做发票预览/下载，把它当成文件路径去读，
就会立刻变成一个本地任意文件读取原语（例如 `../../config/database.properties`）。

### 产品决定（2026-09，已确认）

- 用户确认：**目前没有任何外部系统在调 `POST /api/invoices/{id}/print`**（只有自家前端，且未使用该能力）。
  因此将来若要收紧为"只收相对路径 + 固定根目录"，不会伤到既存调用方——但**根目录仍需随需求确定**：
  应用自己生成的是 `Invoices` 目录下的 **HTML**（`InvoicePrintService`），而这两个字段叫 **pdf/image**，
  更像是外部电子发票/扫描件服务写出来的路径，未必在 `invoices/` 下。
- 结论：现在**不定**白名单（定了大概率是错的），改为把"将来别忘了"变成 CI 红灯。

### 门禁：`InvoicePathGuardPolicyTest`（2 项）

1. `invoicePathsAreNeverDereferencedAsFiles`：扫 `src/main/java` 全部源码，任何把
   `invoices.pdf_path` / `image_path` / `Invoice.pdfPath`/`imagePath` 当**文件系统路径**解引用的地方
   （`new File(`、`Files.read/write/...`、`Path.of(`、`Paths.get(`、`FileInputStream`、`FileOutputStream`、
   `toURI()`、`getResourceAsStream`；读写都算），若未登记进 `VALIDATED_READ_SITES` 即失败，
   失败信息直接给出必须同批实现的四件事（相对路径 + 固定根 + 规范化前缀校验 + 扩展名白名单 + 读取时二次校验）；
   该白名单目前**为空**——不是遗漏，而是确实还没有读取方，实现时登记一行并注明校验方式即可；
2. `writeSiteKeepsTheDeliberateChoiceDocumented`：`InvoiceApiController` 里要保留"**故意不校验**、
   原因见 TD-001、将来实现预览/下载必须同批做校验"的说明，避免后人把"没校验"误读成漏写。

**检测边界（诚实记录）**：解引用匹配范围是"使用点同一行或前后三行"，覆盖
`Path.of(invoice.pdfPath)` 这类直接写法；先把路径存进局部变量、十几行后再打开这种**间接用法抓不到**，
需靠 review。收紧方式（改成"所在方法体内"）会带来迁移/DDL 方法的误报，需一并处理白名单，本轮未做。

变异验证：① 模拟"将来实现了预览但没做校验"（加一个 `Files.readAllBytes(Path.of(invoice.pdfPath))`）
→ 第 1 项变红；② 削弱门禁的识别规则（模拟字段改名）→ 触发**防空转**断言变红。
注意第 2 条第一版是"使用点总数 ≥ 3"这种弱检查，DAO 里的列名就够满足（变异测试当场发现），
已改为**必须分别命中 `InvoiceApiController` 与 `InvoiceDAORefactored`**。

### 为什么当初不直接修（保留原始理由）

按最小改动原则，现状下加白名单属于**为不存在的能力做防御**；而且"合法路径范围"
（是导出目录下？还是用户自选目录？）取决于尚未确定的发票预览/下载产品设计，
现在定下来的白名单大概率是错的。

### 触发条件（必须处理；门禁已覆盖第 2 条）

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
- 实现完成后，把校验过的读取点登记进 `InvoicePathGuardPolicyTest.VALIDATED_READ_SITES`。

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

## TD-017 会员「金额类字段」的修改权限（已修复，2026-09）

**类别**：权限 / 产品策略　**状态**：**已修复（2026-09）**——桌面端按角色锁定金额类字段，
两条路径的等级/折扣变更补上操作日志；剩余两处角色口径不一致登记为 TD-018
**提出来源**：2026-09 全量审计（权限）

### 先纠正一版错误结论（诚实记录）

本条最早（作为"补充"节）写的是"API `PUT /api/members/{id}` 只要求登录、不区分角色"——**这是错的**。
核实后的事实：

- `ApiServer` 的 `before` 过滤器挂了 `AuthorizationMiddleware.authorize`，而它的
  `isFinanceOrAdminPath` 里**早有** `path.matches("/api/members/[^/]+") && isMutating(method)`
  → `PUT /api/members/{id}` **一直是 finance/admin 专属**，且 `AuthorizationMiddlewareTest` 早就断言了
  `isAllowed("cashier","PUT","/api/members/10") == false`；
- 真正的问题在**桌面端**：`MemberEditController` 的等级/折扣/积分/余额字段对任何登录用户可编辑，
  `MemberController` 里 0 处角色判断 → 收银员虽然过不了 API，却可以直接在界面里改折扣/余额。

即：**桌面端比 API 宽松**（方向与本条原先写的相反）。原始"补充"节是基于阅读控制器代码得出的推断，
没有回查中间件——**教训：判断某个接口的角色限制，要看中间件与路由挂载，不能只看控制器方法体。**

### 已修复

**桌面端按角色锁定金额类字段**（`MemberController.showEditDialog` → `MemberEditController`）：

- 角色判定：`currentUser != null && (admin || finance)`，取不到用户时按**不可改**处理（fail-closed）；
- 锁定字段：等级、折扣、**积分**、**余额**（2026-09 用户确认该范围）。为什么不止两个：
  - 等级与折扣在 `handleSave` 里由 **积分**推导（`MemberService.calculateLevel(points)`）——
    只禁用等级/折扣两个控件，收银员改积分即可改折扣；
  - 余额是会员储值金额，等同"发钱"，且 API 的 `POST /api/members/{id}/recharge` 本就限 finance/admin
    （收银员充值走独立充值流程，会写充值流水，不依赖本对话框）；
- 仅禁用控件不够（禁用不阻止程序化赋值、也不阻止上面那段推导覆盖），因此保存时把四个字段
  **还原为打开对话框时的值**，且还原发生在"按积分推导等级"**之后**。

**等级/折扣变更补操作日志**：核实发现会员的建档/修改/充值路径**此前完全没有** `operation_logs`
（写日志的是交易、退货、审计等路径）——"谁把折扣改成 0.5 折"事后查不到。现两条路径都在
等级/折扣真正发生变化时写 `MEMBER/ MEMBER_LEVEL_DISCOUNT_UPDATED`（含旧值→新值）：

- 桌面：`MemberController.showEditDialog`（改动前的值在 `showAndWait()` **之前**快照，
  因为对话框是就地把值写进 `item`）；
- API：`MemberApiController.update`（操作员取 `ctx.attribute("currentUser")`）。

### 门禁与变异验证

`com.cashier.api.middleware.MemberPermissionPolicyTest`（3 项）：

1. **行为级**（直接调 `AuthorizationMiddleware.isAllowed`）：cashier 改会员 = false，
   finance/admin = true，**且 cashier 建档（`POST /api/members`）仍为 true**——本次只收紧金额类字段，
   "建档是收银台日常操作"这条产品口径不变；
2. 桌面端：必须提供 `setSensitiveFieldsEditable`，四个字段都 `setDisable(!editable)`，
   还原语句必须存在且在"按积分推导等级"之后，`MemberController` 必须传角色判定结果（admin/finance）；
3. 两条路径都必须写 `MEMBER_LEVEL_DISCOUNT_UPDATED`。

变异验证（5 处，全部确认变红后还原）：① 桌面不再按角色锁定；② 删掉保存时的还原；
③ API 侧不留痕；④ **放宽 API 的会员写权限**（删掉 `isFinanceOrAdminPath` 里的 members 子句，
行为级断言立刻红）；⑤ 状态栏文案改回**续行**硬编码（见 TD-014）。

### 残留（已登记为 TD-018，需产品决定）

- **API 比桌面端更严**：`PUT /api/members/{id}` 对 cashier 完全关闭（连姓名/生日也改不了），
  桌面端只锁金额类字段。方向上无害（更严），但两侧口径不一致；
- **充值的角色口径相反**：API 的 `POST /api/members/{id}/recharge` 限 finance/admin，
  而桌面端收银员可自由充值——两边必有一边与业务不符，见 TD-018。


## TD-018 会员充值的角色口径两侧不一致（已修复，2026-09）

**类别**：权限 / 产品策略　**状态**：**已修复（2026-09）**——按"收银员可充值"统一两侧口径；
余下一条"PUT 是否拆到字段级"仍待产品决定（方向上 API 更严，无害）
**提出来源**：2026-09 全量审计（权限，TD-017 复核时发现）

### 问题（已核实）

修 TD-017 时发现两侧口径相反：`POST /api/members/{id}/recharge` 被 `AuthorizationMiddleware`
归入 `isFinanceOrAdminPath`（限 finance/admin），而桌面端收银员一直可以自由充值（顾客当面充卡）。

### 决定与落地（2026-09）

**按"收银员可充值"统一**（用户决定）：把 `/api/members/[^/]+/recharge` 从 `isFinanceOrAdminPath` 移出，
并在原位留下注释说明原因与边界——**金额类的"改折扣/等级/积分/余额"仍限 finance/admin**（`/api/members/[^/]+` + 写方法那条）。

- 充值本身有留痕：`MemberService.recharge` 会写 `RechargeRecord` 流水（两条路径共用），
  所以"谁给谁充了多少"可查，无需再补 operation_logs；
- 门禁同步：`AuthorizationMiddlewareTest` 的 cashier 充值断言由 `assertFalse` 翻为 `assertTrue`；
  `MemberPermissionPolicyTest` 新增"cashier 可充值、但不可改金额类字段"两条断言。
  变异验证：把 recharge 重新加回 `isFinanceOrAdminPath` → 两条断言立刻变红（已还原）。
- **未做**：单笔充值限额/审批策略——用户未要求；若门店需要，另开条目。

### 残留（方向无害，暂不改）

`PUT /api/members/{id}` 对整个请求限 finance/admin，而桌面端只锁金额类字段——即收银员
**改姓名/生日**：桌面端可以，API 不行。方向上 API 更严（不构成风险），是否拆到字段级取决于
"允许收银员改会员联系方式"这条产品口径，留待下一轮决定。


## TD-006 非收银台控制器仍在 FX 线程同步查库（约 18 个）

**类别**：性能 / 体验　**状态**：**已修复（2026-09）**——页面级加载、报表链路、点击后短查询、交班结算（唯一的无界查询）——登记表 28 条 + 交班专项 11 条断言；其余剩余项经复核**明确接受、不再修**（理由见末节）
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

### 已修复（2026-09，第四批：交班结算整体后台化）

`ShiftController.handleEndShift` 是剩余项里**唯一含无界查询**的一条：先查活跃班次（单行），确认后调
`loadShiftTransactions` → `findByDateRange(班次开始, 现在)` 把**整班交易**拉回来聚合（忙时上千笔），
全程在 FX 线程 → 点"交班"会冻结界面，而交班是每天都要做的操作。现已整体后台化：

- `handleEndShift`：只提交后台任务（连活跃班次查询也进后台，沿"单行查库同样不得留在 FX 线程"的口径）；
- `confirmEndShift`（FX 线程）：无活跃班次则提示；否则弹确认框，确认后再提交后台结算；
- `endShiftInBackground`（后台）：`loadShiftTransactions` → `categorizeShiftRevenue` → `endShift` → `shiftDao.update`，
  **不触碰 UI**；`loadShiftTransactions` 原来在方法内 `showError` 并返回 null，现改为向上抛给错误回调；
- `renderShiftEnded`（FX 线程）：置位 `shiftEnded` → 刷新列表/按钮 → 交班详情弹窗 → 更新主界面 → 关窗。

**不可调换的顺序（契约）**：`shiftEnded = true` 必须早于 `closeWindow()`。`TouchCartController` 是
打开模态交接班窗 → `showAndWait()` 返回后**同步**读 `isShiftEnded()` 来决定是否自动退出登录，
把置位挪到关窗之后会让该流程永远不退出。

门禁 `FxThreadDbPolicyTest.endShiftSettlementRunsOffTheFxThread`（11 条断言）：入口必须走后台且无同步
`catch (SQLException)`；FX 侧 `confirmEndShift` 不得出现查库调用（防止后台化被绕过）；无界查询与同窗口
退货必须在后台取数方法里；后台方法不得出现 `showError(`/`new Alert(`；`shiftEnded = true` 必须在
`closeWindow();` 之前。

变异验证（3 处，全部确认变红后还原）：① `shiftEnded = true` 挪到 `closeWindow();` 之后 → 顺序断言红；
② 后台方法里加 `showError(...)` → 后台碰 UI 断言红；③ FX 侧 `confirmEndShift` 里加一次同步查库 → 新加的
"FX 侧不得查库"断言红。**教训**：变异③第一版直接调用了会抛受检异常的方法 → **变异代码编译不过**，
Maven 在编译期就失败，日志里没有测试失败行，看起来和"门禁通过"一模一样（我一度误判为假通过）。
**变异不红时先确认它编译过了**（看 `Tests run:` 行或 `COMPILATION ERROR`）。

### 明确接受、不再修（2026-09 复核）

剩下的条目逐条回读后确认：**都是有界查询或单行写**，冻结风险可忽略；改动反而会动到错误提示/刷新时序。
每条写明依据与"什么条件下重新评估"：

| 剩余项 | 实测开销 | 为什么接受 | 何时重新评估 |
|---|---|---|---|
| `PurchaseInboundController` 明细/入库单弹窗（`findByOrderId`、`findRecent(上限)`） | 单订单明细 + 有上限的历史条数 | 有界；弹窗本身是模态，用户预期一次等待 | 若改成"全量入库历史"或去掉上限 |
| `PurchaseOrderController` 商品选择器（分页 search/findByCategory/findAll + 类别 `findAll()`） | 每页 `PRODUCT_SELECTION_PAGE_SIZE` 条；类别是小表 | 有界分页，且是用户主动打开的选择器 | 若商品选择器改为一次拉全表 |
| `PromotionController` 弹窗内批量启用/禁用/删除（`updateSelectedPromotionState`/`deletePromotions`） | 单行写 × 用户勾选条数 | **写**操作；促销数量级小，且改完必须立即刷新勾选状态 | 若促销数量级变大，或改为"全选启用" |
| `UserController` 搜索/停用/重置口令 | `findAll(FIRST_PAGE, USER_LIST_PAGE_SIZE)` 分页 + 单行写 | 分页有界；用户表是小表 | 若用户检索改为全表或去掉分页 |
| `MemberController`/`UserController` 写操作（insert/update/delete + 紧随刷新） | 单行写 | 改异步会改变错误提示与刷新时序（点保存后要立刻知道成功与否），收益低于风险 | 若写操作变成批量（导入/批量改价） |


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


## TD-019 商品管理页数量显示露出未替换的 `{0}`（已修复，2026-09，用户实测发现）

**类别**：正确性 / 体验　**状态**：**已修复（2026-09）**

### 现象与根因

商品管理页右上角显示 `商品数量: {0}: 94/94`。根因在 `InventoryController`：

```java
countLabel.setText(i18n.get("inventory.count") + ": " + inventoryList.size() + "/" + totalProducts);
```

而语言包里 `inventory.count=商品数量: {0}` —— **`get(key)` 没传参**，占位符原样显示，再被拼上 `": 94/94"`。
对照：收银台的 `cart.product_count`（同样是 `商品数量: {0}`）调用时传了参数，所以那边显示正常。

### 修复与门禁

- 改为 `i18n.get("inventory.count", inventoryList.size() + "/" + totalProducts)` → 显示 `商品数量: 94/94`；
- 新增 `com.cashier.i18n.I18nPlaceholderArgsPolicyTest`（1 项）：扫描全部 `get("<key>"…)}` /
  `get(I18nKeys.X…)` 调用（1676 处可静态判定），把语言包值里的最大占位符序号与**实参个数（不含 key）**
  比较，`n+1 > 实参个数` 即失败；防空转要求识别到的调用数 > 1000。
  这是**静默**缺陷：编译与原有门禁都不会报，只有界面上看得见。
- 变异验证：把该行改回不传参的写法 → 门禁报
  `InventoryController:376 key=inventory.count 值="商品数量: {0}" 需要 1 个参数，只传了 0`（已还原）。
  **注意**：这道门禁第一版**自己写错了**——统计实参个数时把 key 本身也算进去，
  于是 `get(key)` 恰好通过（假绿灯），是变异测试当场抓出来的；已修正（见 CLAUDE.md 的教训）。

## TD-020 深色模式交班页有一条刺眼橙色横条（已修复，2026-09，用户实测发现）

**类别**：体验 / 主题　**状态**：**已修复（2026-09）**

### 现象与根因

深色模式下交班管理页面顶部有一条亮橙色横条。核实后有两处：

1. **顶部栏**：其它页面都用共享的 `styleClass="toolbar"`（深色下 `-lisuan-surface` 一类中性灰），
   **只有交班页**用自己的 `shift-toolbar`；深色主题把它覆盖成 `-lisuan-primary-muted`(`#B85C1B`)——
   饱和橙铺满一条，在近黑背景上非常抢眼；
2. **分隔条** `.shift-separator`：基础样式用 `-lisuan-primary-border`(`#A3571E`)，
   深色主题**没有为它提供覆盖规则**，于是又是一条橙线。

### 修复与门禁

- 深色主题里 `shift-toolbar` 改用**深品牌色** `-lisuan-primary-darker`(`#5A2D0C`)：
  保留品牌识别（浅色主题仍是有意的品牌橙），但深色下不再抢眼，白字对比度足够；
- 深色主题新增 `shift-separator { -fx-background-color: -lisuan-border-strong; }`（中性分隔色）；
- 新增 `com.cashier.ui.DarkThemeSurfacePolicyTest`（1 项）：断言深色主题里
  `.shift-toolbar` 不得用 `-lisuan-primary-muted`、必须用深品牌色/中性色，
  且 `.shift-separator` **必须有**覆盖规则并用中性边框色。变异验证：分别把顶部栏改回饱和橙、
  删掉分隔条覆盖 → 均变红（已还原）。

### 顺带做的通用排查（2026-09，同一类问题的系统化）

修完交班页后按同一思路做了通用扫描：**基础样式里"简单类选择器 + 硬编码亮色背景"的规则，
深色主题是否都有覆盖**（硬编码字面量不随主题变，深色下就是近黑背景上的一块亮色）。
扫出 **6 条**，其中 3 条确实被使用：

| 类 | 原背景 | 用在哪 |
|---|---|---|
| `.error-label` | `#F8D7DA` 浅粉 | 登录页、会员编辑、改密页的错误提示（4 个 FXML） |
| `.validation-error` | `#FFEBEE` 浅粉 | `FormValidator` 运行时给非法输入加样式 |
| `.product-info-card` | `#F8FAFC` 浅灰 | 补货页信息卡 |
| `.validation-warning` / `.bg-warning` / `.bg-danger` | 浅黄/亮橙/亮红 | **当前无人使用**（死 CSS，已顺手补深色覆盖以免门禁留例外） |

全部补上深色覆盖（用深色主题既有变量为主），并把门禁**通用化**为
`DarkThemeSurfacePolicyTest.brightBackgroundClassesHaveDarkOverrides`：
凡此类规则都必须在 `dark-theme.css` 里出现同名选择器（防空转：候选数须 > 20，2026-09 为 55）。
变异验证：往 `styles.css` 加一个 `.probe-bright-card { -fx-background-color: #F5F5F5; }` → 变红（已删）。

### 可选的另一种做法（未采用）

把交班页顶部栏直接换成共享的 `toolbar` 类，与其它页面完全一致。未采用的原因：浅色主题下这个
彩色顶部栏是有意的页面识别色，整体改动会连带调整该页按钮与标题配色（白字变深字），
超出"优化深色下这条橙色"的范围。若希望全站统一，另行提出即可。


## TD-021 FXML 的 `%key` 引用了带占位符的 key（已修复，2026-09）

**类别**：正确性 / 体验　**状态**：**已修复（2026-09）**（由 TD-019 的 `{0}` 问题顺藤摸瓜发现）

### 现象与根因

修完商品数量那句后，顺手扫了全部 FXML：**13 个标签**用 `%key` 引用了**带占位符**的 key，
而 JavaFX 的 `%key` 由 ResourceBundle **直接解析、不做参数替换**——所以这些标签在控制器填值之前
显示的是 `商品数量: {0}`、`交易数量: {0}`、`总金额: ¥{0}` 这类原文。
页面加载在 TD-006 之后已**后台化**，慢库/加载失败时这段"占位符原文"会停留可见。

涉及：`InventoryView`、`TransactionView`（数量与总金额）、`MemberView`、`SupplierView`、
`PurchaseOrderView`、`PurchaseInboundView`、`PurchaseApprovalView`（两处）、`PromotionView`、
`InventoryCheckView`、`UserView`、`CartView`。

### 修复

- 这 13 个标签在 FXML 里改为 `text=""`（真实文案由控制器用**另一套** `runtime.*` key 带参写入，
  已是既有做法）；由此有 **10 个 key** 只被这些占位初始文本引用、变成无用，一并从四份语言包删除
  （`cart.product_count`/`inventory.count`/`member.count` 仍被 Java 使用，保留）；
- 新增门禁 `I18nPlaceholderArgsPolicyTest.fxmlDoesNotReferencePlaceholderKeys`：扫全部 FXML 的
  `%key` 引用（2026-09 为 900 处），若其语言包值含 `{n}` 即失败并报出 `文件:行号`；
  防空转要求引用数 > 200。变异验证：把 `InventoryView` 的 countLabel 改回 `%inventory.count` → 变红。
- 教训：**FXML 的 `%key` 与 Java 的 `get(key, args)` 不是同一套能力**——前者不能传参，
  所以"带占位符的 key"只能出现在 Java 调用里。

### 顺带补掉的两个盲区（2026-09）

1. **首参是变量的 `get(var)`**：门禁原本只能判定字面量/常量键，变量键是盲区。
   现补保守推断：若该变量**在同一文件里被赋成字面量**（`var = "key"`），就按那个 key 检查占位符；
   参数/字段/动态拼接仍跳过（无法静态判定）。全仓库此类调用 **101 处**，推断结果 **0 处违规**
   （`SearchManager` 的 `SearchType` key、`ProductEditController`/`CartController`/`MemberEditController`
   的 messageKey 等都不是带占位符的文案）。变异验证：注入
   `String probeKey = "status_message.product_deleted"; get(probeKey);` → 变红。
2. **`StatusBarManager` 兼容映射表**（中文串 → key）里不少 key 是带占位符的
   （`status_message.product_deleted=商品删除成功: {0}` 等），它是用
   `get(key, status.substring(prefix.length()))` 把后缀当参数传的——**若被"简化"成 `get(key)`，
   所有走该表的旧调用点会直接显示 `{0}`**。已加门禁断言这一条（变异验证：改成 `get(key)` → 变红）。
   另外此检查过程中确认过：该映射表的兜底逻辑本身正确（不是潜在 bug）。

---

## TD-011 Windows 启动脚本把未匹配的 JAR 通配符当路径，静默跳过构建

**类别**：发布　**状态**：**已关闭（2026-09）**：静态修复 + Windows 实机 5 步验收全部通过
**提出来源**：2026-09 全量审计（脚本）

### 根因

`start.bat` / `install.bat` / `DataConfig.bat` 用
`for %%f in (target\lisuan-fx-*-jar-with-dependencies.jar) do set "JAR_FILE=%%f"` 检测 JAR，
再用 `if "%JAR_FILE%"==""` 判断"没找到"。

cmd 的语义是：**带通配符的集合在无匹配时会把该模式原样当成一项**——于是 `JAR_FILE` 变成
`target\lisuan-fx-*-jar-with-dependencies.jar`（非空），"没找到"的分支**永远不可达**：

- `start.bat`：跳过 `mvn clean package`，最后 `java -jar "target\...*..."` 报 `Unable to access jarfile`；
- `install.bat`：在**干净机器**上打印 `[SKIP] Detected existing compiled JAR: target\lisuan-fx-*-...`，
  于是从不构建，装完启动不了（这是"装完不能用"的一类）；
- `DataConfig.bat`：友好的"JAR not found"提示写了但走不到，用户看到的是 Java 的原始报错。

### 修复（2026-09，静态部分）

- 检测统一改为 **`for /f "delims=" %%f in ('dir /b /o-d "<模式>" 2^>nul') do ...`**：
  无匹配时 `dir` 不输出任何行 → 变量保持未定义 → 判断可靠；`/o-d` 保证多版本时取最新的。
- 判空统一改为 **`if not defined JAR_FILE`**（不再用 `if "%VAR%"==""`，免去引号坑），
  并补 `if not exist "%JAR_FILE%"` 二次校验。
- **按产品决定：`start.bat` 不再自动构建**——缺 JAR 时打印
  `[ERROR] Application JAR not found in target\` + `mvn clean package -DskipTests` + `pause` + `exit /b 1`。
  理由：启动脚本偷偷跑一次 5 分钟且依赖本机 Maven/网络的构建，失败面大且难排查。
  `install.bat` 的构建是**显式安装步骤**，保留；但构建后新增"真的产出 fat JAR"校验，
  否则同样 `exit /b 1`。
- `install.bat` **生成的** `DataConfig.bat`（heredoc，`%%%%` 转义）同步改用同一套检测写法——
  否则同一个 bug 会在生成物里复活。
- **行尾**：`git ls-files --eol` 显示 `DataConfig.bat`、`diagnose.bat`、`release.bat`、
  `docker/start-mysql.bat` 的**工作区是 LF**（`.gitattributes` 虽写了 `*.bat eol=crlf`，
  但 eol 只在 checkout 生效，这些文件是被工具写过 LF 后一直没重新 checkout）。
  `git clone` 到 Windows 没问题，但**任何不走 git checkout 的分发方式**（zip 工作区、从 mac 拷给客户）
  会带着 LF 的 `.bat`，在 Windows 上 `goto :label` 与 `if (...)` 块**解析失败**。
  已用 `git checkout --` 全部重写为 CRLF（索引仍是 LF 归一化形态）。
  注：`release.sh` 并不打包 `.bat`（已核实），所以这条风险目前只存在于手工分发场景。
- 不动 `chcp`：`start.bat` / `install.bat` / `DataConfig.bat` 的**控制台输出全是英文**
  （`install.bat` 里两处中文只出现在写入文件的 `echo REM` 中，不进控制台），
  而会打印中文的 `release.bat` / `docker/start-mysql.bat` 本来就设了 `chcp 65001`。

### 门禁（7 项，全部做过变异验证）

`com.cashier.security.WindowsScriptPolicyTest`：

1. `noBatchScriptIteratesWildcardInForSet`：扫描**全部跟踪的 `.bat`**，任何
   `for %%x in (<含通配符的集合>)` 即失败（`REM` 注释里为说明问题而写出坏写法不计）；
2. `jarDetectionUsesDirBAndDefinedCheck`：`start.bat`/`install.bat`/`DataConfig.bat` 必须有
   `for /f + dir /b` 检测、必须用 `if not defined`，且不得再出现 `if "%JAR_FILE%"==""`；
3. `startBatReportsMissingJarInsteadOfBuilding`：`start.bat` 不得有真正的 maven 调用
   （行首 `mvn`/`call mvn`；错误提示里的 `echo mvn ...` 不算），缺 JAR 必须报错 + `exit /b 1`，
   且启动语句 `-jar "%JAR_FILE%"` 必须排在该退出之后；
4. `javaNeverReceivesWildcardPath`：`java` 命令行不得出现通配符（cmd 不为外部命令展开通配符）；
5. `generatedDataConfigKeepsSameDetectionIdiom`：`install.bat` 生成的 `DataConfig.bat` 必须与
   随包版本用同一套检测写法，防止两处漂移；
6. `installVerifiesJarAfterBuild`：显式构建之后必须再校验一次 fat JAR 产出并退出非零；
7. `batchFilesAreStoredWithCrlfLineEndings`：跟踪的 `.bat` 工作区必须是 CRLF（或混合行尾即失败）；
8. `nonAsciiBatchScriptsMustSetCodePage`：含非 ASCII 字节的 `.bat` **必须**自己切换代码页
   （必须匹配行首 `chcp 65001`）——否则中文 Windows 会重现上面的乱码/解析失败。
   这条正是为上面那处回归补的；**第一版只查子串 `chcp`，被变异文本里自带的 "chcp" 字样骗过
   （变异测试当场发现），已收紧为必须匹配真正的代码页切换行**；
9. `startBatRestoresCodePageAfterLaunch`：`start.bat` 切了代码页就必须恢复（含纯数字校验），
   且恢复子过程要在 `java` 返回后与 GUI 模式提前返回前各调用一次；
10. `installKeepsShippedDataConfig`：`install.bat` 必须先 `if exist "DataConfig.bat"` 跳过、
   再考虑生成，两条路汇到 `:dataconfig_ready`。

变异验证：① `start.bat` 检测改回通配符 `for` → 1、2 变红；② `start.bat` 加回 `call mvn` → 3 变红；
③ `release.bat` 写回 LF → 7 变红；④ `install.bat` 生成段改回通配符 → 5 变红；
⑤ `DataConfig.bat` 写回中文注释 → 8 变红（实机回归的等价复现）；
⑥ 把 `release.bat` 的 `chcp` 行改成别的内容 → 8 变红（收紧后的版本才抓得住）。均确认后还原。

### 首轮 Windows 实机测试结果（2026-09，用户在 Windows 上跑 install.bat）

**生效的部分**：`[4/4]` 段打印的是
`[SKIP] Detected existing compiled JAR: target\lisuan-fx-2.6.0-jar-with-dependencies.jar`
——是**真实文件名**而不是 `target\lisuan-fx-*-jar-with-dependencies.jar`，说明
`for /f + dir /b` 的检测修复生效（旧写法在这里会打印通配符本身）。

**本批引入的回归（已修）**：我为解释问题在 `start.bat`/`install.bat` 里加了**中文 `REM` 注释**，
而 cmd 用当前 OEM 代码页（中文 Windows 是 936/GBK）读 `.bat`——UTF-8 中文变成乱码后**字节错位**，
把 `REM` 后的空格与 `^(` 的 `^` 吞掉，于是注释被当成命令执行、块解析失败。用户实测输出：

```
'涓€椤癸紝浜庢槸杩欏噷姘歌繙"妫€娴嬪埌宸叉湁 JAR"銆?' 不是内部或外部命令，也不是可运行的程序
'瀯寤猴紝瑁呭畬鍚姩鏃舵墠鎶?Unable' 不是内部或外部命令，也不是可运行的程序
...
[INFO] Creating database configuration tool...
此时不应有 閭ｆ牰鎶婇€氶厤绗﹀師鏍峰綋鎴愯矾寰勩€?。
```

前两条是我那两行注释的**尾部**被当成命令，第三条（`此时不应有` = "was unexpected at this time"）
是 heredoc 里 `echo REM ...：...` 那行破坏了 `( ... )` 块 → `install.bat` 直接中断。
**教训**：我上一轮只检查了"中文 echo 行数 = 0"，漏掉了 `REM` 注释——注释里一样不能有非 ASCII 字节。

**修复**：这几个 `.bat` 里的**全部非 ASCII 文本（都是 `REM` 注释，无任何用户可见文案）改写成英文**，
于是文件变成**纯 ASCII**，不再依赖控制台代码页：

| 文件 | 非 ASCII 字节（前→后） | 处理 |
|---|---|---|
| `start.bat` | 336 → 0 | 注释英文化 |
| `install.bat` | 477 → 0 | 注释英文化（含 heredoc 里的生成物文案） |
| `DataConfig.bat` | 132 → 0 | 注释英文化（含随包与生成两处） |
| `create-shortcut.bat` | 183 → 0 | 注释英文化（同类隐患，顺手一并清掉） |
| `release.bat` / `docker/start-mysql.bat` | 525 / 21 | **保持**：它们开头就有 `chcp 65001`，中文是用户可见文案，既有做法可用 |

**第二轮实机验收（2026-09，修复后）**：

| 步骤 | 结果 |
|---|---|
| 1 `findstr` 看新版脚本 | ✅ |
| 2 缺 JAR 报错退出 | ✅ `[ERROR] Application JAR not found in target\` + 构建提示，**未出现** `Unable to access jarfile`，**未走到 java**，`EXIT=1` |
| 3 有 JAR 正常启动 | ✅ `[OK] JAR file found: target\lisuan-fx-2.6.0-jar-with-dependencies.jar`，应用正常启动 |
| 4 install.bat 干净构建 | ✅ `[2/4] Version: 3.9.16`（不再是 scoop 路径）、`[4/4]` 真的执行 `mvn clean package`（不再 `[SKIP]`）、无"不是内部或外部命令"/"此时不应有" |
| 5 生成物可用 | ✅ `[OK] Created DataConfig.bat` → 配置工具启动 → `Found application JAR: ...真实文件名` → 写 `.env` → 安装完成 |

**控制台代码页的取舍（实机暴露，已按用户选择实现）**：给 `start.bat` 加 `chcp 65001` 修好了应用日志乱码，
但 `chcp` 在同一窗口持续生效，导致**紧接着在同一窗口跑 maven 时 javac 的 GBK 输出反向乱码**。
现改为**只包住 `java` 调用**：启动前记住原代码页并切到 65001，`java` 返回后（以及 `--gui` 提前返回前）
调用 `:restore_code_page` 恢复。要点：

- 报错退出分支（缺 JAR）在切换**之前**，不涉及恢复；
- 恢复前用 `findstr /r "^[0-9][0-9]*$"` 校验解析结果是纯数字——`chcp` 的输出文案随系统语言变化，
  解析不出来时宁可不切回（保持原状），不会执行一条畸形命令；
- 门禁第 9 条钉住这个配对：`start.bat` 出现 `chcp 65001` 就必须出现 `chcp %ORIGINAL_CODE_PAGE%`
  与 `:restore_code_page`；`release.bat`/`docker/start-mysql.bat` 自身输出就是中文，保留 65001 是有意的，不在规则内。
- 变异验证：删掉恢复行 → 第 9 条变红。

**第四轮实机（代码页切换/恢复）**：

- `start.bat` 打印 `[INFO] Console switched to UTF-8 (65001); it will be restored to 65001 on exit`
  与退出时的 `[INFO] Console code page restored to 65001` —— **解析与恢复逻辑都生效**
  （该窗口本来就是 65001，所以恢复值等于原值；936 → 恢复 936 的路径需在**新窗口**里再看一眼）。
- 缺 JAR 路径：打印 `[ERROR] Application JAR not found in target\` + 构建提示，**未出现**
  `Unable to access jarfile`，且没有走到 `java` —— 功能上通过；用户侧看到的 `EXIT=0` 是**测量方式**问题
  （`cmd /c "start.bat" < nul & echo EXIT=%ERRORLEVEL%` 让 PowerShell 展开了 `%ERRORLEVEL%`，
  PowerShell 要用 `$LASTEXITCODE`）。正确写法：`cmd /c "start.bat < nul"; "EXIT=$LASTEXITCODE"`。
- **`install.bat` 不再无条件覆盖已跟踪的 `DataConfig.bat`**：仓库随包提供的那份就是可用的，
  重新生成会把它改脏（`git status` 出现改动）而且让同一份逻辑在两处维护——通配符检测 bug 当初正是
  只在其中一处被修。现在 `if exist "DataConfig.bat"` → `[SKIP]` + `goto :dataconfig_ready`，
  缺失时才生成；门禁第 10 条钉住这个顺序（变异验证：去掉跳过判断 → 变红）。

**同一轮实机发现的另一个 bug（已修）**：`[2/4]` 打印的
`Version: C:\Users\nevell\scoop\apps\maven\current` —— 版本行显示成了路径。
原因是 `findstr /i "Apache Maven"` 会被 scoop shim 输出的其它含该字样的行命中，
再用 `tokens=3` 取到路径。现改为锚定真实横幅行
（`findstr /r /c:"^Apache Maven [0-9]"`），解析不到时**如实说明**而不是把路径当版本号打印。

### 证据：cmd 语义的**模型复现**（不是实机验证）

本机没有 `wine`/`cmd`/`powershell`/`dosbox`（已实测），`.bat` **既不能执行也不能语法检查**。
因此用一个按 cmd 文档语义建模的脚本复现了错误链（两种场景：干净机器 / 已构建）：

- 旧写法・干净机器 → `JAR_FILE` = `lisuan-fx-*-jar-with-dependencies.jar`（字面量）→ **跳过构建** →
  `java -jar` 得到 `Unable to access jarfile lisuan-fx-*-jar-with-dependencies.jar`；
- 新写法・干净机器 → `JAR_FILE` 未定义 → **报错退出 `exit /b 1`**，不再走到 `java`；
- 两种写法在"已有 JAR"时行为相同（都能正常启动）。

**这只能证明我对 cmd 语义的理解与修复方向自洽，不能替代实机验收。** 下面是实机清单。

### Windows 实机验收清单（**待重跑**：首轮在第 4 步被上述回归中断）

前置：用 `git clone`（不要用 zip 拷贝 mac 工作区），`cmd` 里先 `cd /d` 到项目根目录。

| # | 命令 | 期望 |
|---|---|---|
| 1 | `findstr /C:"for /f" start.bat` | 有输出（说明拿到的是新版脚本） |
| 2 | `del /q target\lisuan-fx-*-jar-with-dependencies.jar` 然后 `start.bat < nul & echo EXIT=%ERRORLEVEL%` | 打印 `[ERROR] Application JAR not found in target\`、`mvn clean package -DskipTests`；`EXIT=1`；**不得**出现 `Unable to access jarfile` |
| 3 | `mvn clean package -DskipTests` 然后 `start.bat` | 打印 `[OK] JAR file found: target\lisuan-fx-2.6.0-jar-with-dependencies.jar` 并打开收银界面 |
| 4 | `del /q target\lisuan-fx-*-jar-with-dependencies.jar` 然后 `install.bat` | `[2/4]` 的 `Version:` 是形如 `3.9.x` 的版本号（**不再是 scoop 路径**）；`[4/4]` 段**真的执行** `mvn clean package -DskipTests`（不再打印 `[SKIP] Detected existing compiled JAR`）；**不得**再出现"不是内部或外部命令"或"此时不应有"；完成后生成 `DataConfig.bat` 并能打开数据库配置界面 |
| 5 | `findstr /C:"for %%" DataConfig.bat` | **无输出**（生成物已不含坏写法）；`findstr /C:"for /f" DataConfig.bat` 有输出 |

失败时请把这些发我：**完整命令输出**（含 `EXIT=` 那行的值）、`echo %ERRORLEVEL%`、
以及 `target` 目录下的实际文件名（`dir /b target\*jar-with-dependencies.jar`）。

### 剩余

- **5 步全部实机通过**（2026-09）：检测、缺 JAR 报错退出、有 JAR 启动、干净构建、生成物可用。
- 仍未覆盖的角落：路径含空格或中文的安装目录、`--gui` 模式的代码页恢复（`start "" javaw` 那条分支）、
  非中文 Windows 上的 `chcp` 输出解析。这些属于"有条件才触发"，风险与影响都小，暂不追加验证。
- 两点测量经验（都踩过）：① 验证退出码要在 PowerShell 里用 `$LASTEXITCODE`
  （`cmd /c "start.bat < nul"; "EXIT=$LASTEXITCODE"`），`%ERRORLEVEL%` 是 cmd 语法、会被 PowerShell 展开；
  ② `del /q` 也不是 PowerShell 命令，用 `Remove-Item ... -Force`。
- `install.bat` 在跳过构建时仍打印 `[OK] Project built successfully`（紧跟在 `[SKIP] ...` 之后），
  措辞略有矛盾，属观感问题，本轮未动。
- `diagnose.bat` 的 `dir /b ... 2>nul || echo ...` 经核对是**正确写法**（`dir` 无匹配返回非零，
  `||` 分支正是"没找到"的提示），不是静默吞错，无需改动。

---

## TD-012 install.sh / docker-init.sh 静默成功与占位口令

**类别**：运维　**状态**：**已修复（2026-09）**
**提出来源**：2026-09 全量审计（脚本）

### 现状（修复前，四项）

1. `install.sh` 本地/远程两条路径的建库与 SQL 导入用 `2>/dev/null || true` 吞掉失败，
   之后照样打印 `[Done] Database initialization completed` → **装完报成功但库是空的**；
   Docker 路径虽受 `set -e` 保护，但 stderr 被丢进 `/dev/null`，报错没有任何指向。
2. `docker/docker-init.sh` **从不读 `.env`**：只做了 `cp .env.example .env` 的机器会退化成脚本里的
   占位常量；而且占位口令只警告，用户答 `y` 就会 `ALTER USER ... IDENTIFIED BY 'YOUR_CASHIER_PASSWORD_HERE'`
   并把它写回 `.env`。
3. `docker/start-mysql.sh` 硬编码容器名 `lisuan-mysql`，与 compose/install.sh 的可配
   `MYSQL_CONTAINER_NAME` 不一致 → 自定义名时误报"启动失败"。
4. `.env.example` 与 `docs/CREDENTIALS_CHECKLIST.md` 提到 `DB_USE_SSL`，但**没有任何代码读它**
   （SSL 实际由 `db.url` 的 `sslMode` 决定）。

### 修复（2026-09）

- **失败可见**：`install.sh` 新增统一的 `fail_db_import()`：建库/导入的 **7 个失败分支**
  （Docker 3 + 本地 2 + 远程 2）都改为 `if ! ... ; then fail_db_import ...`，
  失败时打印 mysql 的真实输出（末尾 5 行，stderr 收进临时文件而非 `/dev/null`）、
  说明后果（"数据库可能只有空结构"）与可操作提示，然后 `exit 1`。
- **口令守卫**：`docker-init.sh` 开头 `set -a; . ./.env; set +a` 先读工作目录的 `.env`；
  新增 `is_placeholder_password()`（空值、`YOUR_*_HERE`、`changeme`、`password`、`123456`），
  命中即**硬失败**（删除原来的"是否继续？(y/N)"分支），并把口令检查放在第一条 `ALTER USER` 之前。
- **容器名同源**：`start-mysql.sh` 读 `MYSQL_CONTAINER_NAME`（默认 `lisuan-mysql`，与 install.sh 一致），
  并在读 `.env` 后再取一次；容器存在性判断改用 `docker ps [-a] --format '{{.Names}}' | grep -qx "${MYSQL_CONTAINER_NAME}"`
  （精确匹配，不再子串误命中）。
- **删掉未实现的开关**：`.env.example` 与凭证清单里的 `DB_USE_SSL` 改为指向真实机制
  （`config/database.properties` 的 `db.url` 里的 `sslMode`）。

### 门禁（4 项，全部做过变异验证）

`com.cashier.security.InstallScriptPolicyTest`：

1. `installFailsLoudlyWhenDatabaseImportFails`：不得再出现 `00-init-complete.sql 2>/dev/null || true`；
   7 处失败分支必须调用 `fail_db_import`；3 条路径的导入必须把 stderr 收进临时文件；3 处成功提示仍在；
2. `dockerInitReadsEnvAndRejectsPlaceholderPasswords`：必须 `. ./.env`；必须有 `is_placeholder_password`；
   **不得**再出现"是否继续？"；守卫必须是可执行判断（`if is_placeholder_password ...`）且早于第一条 `ALTER USER`
   （第一版门禁只查"文本存在"，被 `if false && ...` 短路后照样通过 —— 变异测试当场发现，已收紧）；
3. `containerNameComesFromOneSource`：start-mysql 不得硬编码容器名，两处判断必须精确匹配变量，
   变量默认值要与 install.sh 一致；
4. `noUnimplementedDeploymentSwitches`：`.env.example` 与凭证清单不得再提 `DB_USE_SSL`。

变异验证：① 本地路径导入改回 `|| true` → 1 变红；② 守卫改成 `if false && ...` → 2 变红；
③ 容器名改回硬编码 → 3 变红；④ `.env.example` 恢复 `DB_USE_SSL` → 4 变红。

### 行为验证（本机真跑，不只静态检查）

用桩 `docker`/`docker compose`（记录每次调用）在临时目录跑 `docker/docker-init.sh`：

- `.env` 里是占位口令 → **退出码 1**、打印"数据库口令为空或仍是占位符，已停止初始化"，
  且调用日志里 **`ALTER USER` 出现 0 次**（旧版本答 y 后会真的写库）；
- `.env` 里是真实口令 → 退出码 0、`ALTER USER` 用的是 `.env` 里的口令（证明脚本确实读了 `.env`，
  旧版本从不读）、口令按原值写回 `.env` 且权限 600。

用桩 `mvn`/`mysql` 跑 `install.sh` 的本地 MySQL 路径（`mysql` 桩让 `SELECT 1` 与建库成功、导入失败）：

- 退出码 **1**，输出里能看到 mysql 的真实错误 `ERROR 1045 (28000): Access denied ...`、
  `[Error] [Local MySQL] 导入 00-init-complete.sql 失败（数据库可能只有空结构）` 与修复建议；
- 且**不再打印** `Database initialization completed`。

### 剩余

- `install.bat`（Windows）里若存在同类"静默成功"写法，需在 Windows 上核对——本轮未改 `.bat`，
  也没有 Windows 环境做行为验证（见 TD-011）。

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

**类别**：文档 / 体验　**状态**：**已修复（2026-09）**——可见文案全部迁完（状态栏 0 处硬编码）、
无用 key 清零（196 key + 60 常量）、说明文档入库并加一致性门禁；打包向导**明确不译**
（开发者工具，零引用于 POS 界面，已记录理由与重估条件）
**提出来源**：2026-09 全量审计（UI/i18n + 文档）

### 已修复

**第一批（文档与 locale）**

- **文档里的测试数不再写死**：README 头部与正文、`AGENTS.md` 原有 `589 / 606 / 647`，
  实际早已不同。改为"`mvn -q clean verify` 全绿 + 数量以构建输出 `Tests run:` 为准"。
- **`I18nUiUtils` 的大小写折叠改用 `Locale.ROOT`**：土耳其语环境下 `"CHECKING".toLowerCase()`
  会得到 `checkıng`，落库值匹配失败、界面回退成英文原值。门禁
  `I18nUiUtilsTest.caseFoldingIsLocaleIndependent`（切 `tr_TR` 后断言）。
- 澄清一条误报：`CurrencyUtil` 已显式用 `DecimalFormatSymbols(Locale.SIMPLIFIED_CHINESE)`，
  不受默认 locale 影响（真正的 locale 问题见 TD-015）。

**第二批（运行时可见文案迁移）**

把审计点名的硬编码文案全部迁到语言包（新增 30 个 key，`I18nKeys.Runtime` 补常量，
四份语言包同步；能复用的都复用了，如 `common.cancel`、`app.name`、`runtime.backup_file_success`）：

- **新增 key 数**：第二批新增 30 个，随后发现其中 5 个（`runtime.status_ready`、`status_refreshed`、
  `status_data_saved`、`status_data_backup`、`status_data_restore`）与既有 key **同值重复**，
  已改为复用既有 key（`status.ready`、`status_message.refreshed`、`status_message.data_saved`、
  `menu.data.backup`、`menu.data.restore`）并删除重复项 → **净新增 25 个**；
  本批（第三批）状态栏文案**全部复用既有 key，新增 0 个**
- `MainController` 备份/恢复对话框（成功/失败/无备份 → 标题 + 正文，`{0}` 占位）、
  状态栏文案（就绪/数据已保存/已刷新/数据备份/数据恢复）与"功能开发中"占位弹窗（搜索/编辑/批量操作/导出）；
  顺带把该文件里误用的 `i18n.get(...)`（本来没有这个字段）统一为 `I18nManager.getInstance().get(...)`
- `RechargeController` 支付方式下拉框：**下拉项仍是规范中文落库值**（TD-002 口径），
  通过 `StringConverter` 只把**显示层**本地化（`I18nUiUtils.paymentMethod`）——
  既让 en/zh_TW 用户看到母语，又保证落库/筛选值不变
- `PrintPreviewDialog`：标题、预览标签、打印/取消按钮
- `SplashWindow`：启动/初始化/加载数据/启动服务/即将完成五段进度文案 + 窗口标题（复用 `app.name`）
- `CashierSystemFXApplication`：启动失败、界面字体缺失两个弹窗的标题与正文
- `InventoryView.fxml`：`promptText="全部"` → `%inventory.all_categories`（沿用既有 key）
- `TouchCartView.fxml` 的两处设计期占位（`便利店`、`2026-08-23 星期日`）**保持硬编码**并登记白名单：
  它们都有 `fx:id`，运行时必被覆盖（`TouchCartController` 用设置里的店名与当前日期 `setText`），
  改成语言包只会增加无意义的 key——白名单里逐条注明覆盖它的代码位置

**第三批（状态栏文案）**

`MainController` 里 **26 处** `updateStatus("中文")` + 2 处带参数的拼接（`"无法刷新: " + title`、
`"已刷新: " + title`）全部迁走。这批文案都是菜单/导航名，直接**复用导航栏与既有状态文案的 key**
（`nav.transactions`、`nav.members`、`menu.data.backup`、`status.ready`、`status_message.theme_*` 等），
状态栏与导航项文案因此永远一致；新增常量少量（`Menu.DATA_BACKUP/DATA_RESTORE`、
`Nav.RETURN_APPROVAL/RETURN_REPORT`、`Runtime.SHIFT_HANDOVER`，
以及集中 `status_message.*` 的新嵌套类 `I18nKeys.StatusMessage`）。

门禁同步升级：`UI_CALL` 清单加入 `updateStatus`/`updateWarning`，状态栏文案从此也在守卫范围内。
变异验证：把一处 `updateStatus(I18nManager.getInstance().get(I18nKeys.Nav.TRANSACTIONS))`
改回 `updateStatus("交易记录")` → 门禁变红（已还原）。

### 明确不译（2026-09 产品决定）：打包向导

`com.cashier.packager.PackageWizardController`（104 处中文字面量）**不迁 i18n**，依据：

- 它是**独立的开发者/发布工具**，不是门店界面：`PackageWizardApp` 有自己的 `main`，由 Maven profile
  `-Ppackager` 构建（launcher `lisuan-packager`）；`PackageWizardController` 在 `src/main`/`src/test`/
  脚本/FXML 里**零引用**（POS 界面不会加载它）；
- 受众是打包/发布的人，与 `release.bat`（同为中文、同样刻意保留）一致；
- 迁它要新增约 100 个 key × 4 份语言包并给该工具接一套 i18n 布线，而没有任何门店用户会看到这些文字。

**何时重新评估**：若把打包向导交给门店用户使用，或需交付给非中文开发者/外部集成方。

### 门禁（4 项，全部做过变异验证）

`com.cashier.security.HardcodedUiTextPolicyTest`：

1. `migratedFilesHaveNoHardcodedChineseUiText`：已迁移文件里，
   `setTitle/setHeaderText/setContentText/setText/setPromptText/setTooltipText/show*Alert/showPlaceholder`
   以及 `new Label/Button/...("中文")` 不得含中文（**日志与注释里的中文不算**——项目本来就用中文写日志）；
2. `fxmlVisibleTextUsesResourceKeys`：视图目录内 `text/promptText/title/headerText` 要么是 `%key`、
   要么不含中文（运行时覆盖的占位走白名单，白名单必须逐条注明理由）；
3. `migratedKeysExistInEveryBundle`：本次迁移的 key 必须在四份语言包里都存在；
4. `printPreviewButtonsUseI18n`：打印预览按钮不得回退成字面量。

**第六批（说明文档入库与一致性门禁）**

`AGENTS.md`（4.2 KB 的速查）入库，与 `CLAUDE.md`（权威细节）并存；新增
`com.cashier.security.InstructionsDocPolicyTest`（3 项）钉住最容易漂移的三件事：

1. `instructionDocsAreTracked`：两份文档都必须被 git 跟踪（`AGENTS.md` 曾经只存在于一台机器上）；
2. `instructionDocsAgreeOnVersionSources`：**讲"版本号四处同步"的那一句**必须列全
   `AppConstants` / `pom.xml` / `installer/Installer.java` / `.env.example`；
3. `instructionDocsDoNotHardcodeTestCounts`：两份"当前事实"文档不得写死测试数量
   （README 的历史更新日志不受限——那里的数字是史实，只把残留的"380 个测试用例"改成"当时 380 个"）。

变异验证：① 往 CLAUDE.md 写死"731 个测试" → 3 变红；② 把 AGENTS.md 同步句里的 `.env.example` 去掉
→ 2 变红；③ `git rm --cached AGENTS.md` → 1 变红。**注意第 2 条第一版是假绿灯**：它只要求来源名
"在文档里任意位置出现过"，而 AGENTS.md 别处（compose 说明）也提到 `.env.example`，去掉同步句里的那处
照样通过——变异测试当场发现，已收紧为"只在那一句里查"。

**第五批（无用 key 清理）**

判定口径（保守，宁可漏报也不误删）——一个 key 被算作"在用"当且仅当满足其一：

1. Java 里出现**完整等于该 key 的字符串字面量**（`get("a.b")`）；
2. 引用了 `I18nKeys.*` / `I18n.*` 常量且常量值等于该 key（**两个常量持有者都要算**——
   `/api/i18n/messages/all` 走的是 `I18n` 那一套）；
3. FXML/资源里出现 `"%key"`（本项目 792 处）；
4. 代码里有 `"前缀." + 变量` 这种**拼接**，则该前缀下所有 key 一律放行
   （实测只有 3 个真前缀：`audit.category.`、`audit.result.`、`payment.channel.`，因此保守保留了 21 个 key）。

过程里踩到并修掉的两个坑（都值得记）：

- 第一版扫描器把**最外层类名**也算进常量链（`I18nKeys.I18nKeys.Common.EDIT`），于是所有常量引用
  都没匹配上，未引用数被高估到 313；**用 `grep` 做独立复核**时又遇到大量**子串误报**
  （`invoice.buyer` 命中 SQL 列名 `buyer_name`、`menu.file` 命中 `menu.file.export`）。
  最终改为"精确字面量 + 常量名引用 + 整词复核"三重判定，收敛到 196；
- 删除后新门禁立刻发现一个**既存死常量**：`I18n.INVOICE_VOID` 指向的 `invoice_void`
  在语言包里从来不存在（`HEAD` 里也是 0 处）→ 已删。**这类"常量指向不存在的 key"以前没有任何门禁**，
  现在由 `constantValuesExistInBundles` 守着。

**第四批（状态栏清零）**把门禁扩到 7 个采购/设置/交班/供应商/交易控制器后，它**又当场抓出两处**：

- `SettingsController:981` 的 `showError("备份正在进行中，请稍候…")` —— 人工枚举时漏掉了（我按
  `updateStatus/updateWarning` 筛的，它用的是 `showError`）→ 已迁（新增 `runtime.backup_already_running`）；
- `PurchaseApprovalController` 里 `get("approve".equals(action) ? ... : ..., order.no)` 这种
  **首参是表达式**的写法骗过了 i18n key 扫描（它把字面量 `"approve"` 当成 key）→ 改为先把 key
  提取成局部变量再传。**教训**：源码级门禁只认它假定的语法形状，写代码时要顺着门禁的形状写，
  否则会得到"假失败"或"假绿灯"。

无用 key 门禁（`com.cashier.i18n.I18nUnusedKeyPolicyTest`，2 项）：

- `bundlesHaveNoUnusedKeys`：按上面的四条口径判定，任何"没人引用且不在动态前缀下"的 key 即失败；
  若代码里出现静态导入 `I18nKeys.*`/`I18n.*`（本门禁解析不了短名），会**显式报"门禁需要更新"**
  而不是悄悄当成无用；
- `constantValuesExistInBundles`：`I18nKeys` 与 `I18n` 的**每个常量值都必须在语言包里存在**
  （比 `I18nBundleConsistencyTest` 更严——后者只查 `I18nKeys`）；既存死常量 `I18n.INVOICE_VOID` 就是它抓的。

变异验证：往四份语言包各加一个 `runtime.__unused_probe` → `bundlesHaveNoUnusedKeys` 变红（已还原）。

**第五批（状态栏规则升级为全仓库 / 表达式感知）**

- 那 8 处的实际情况分两类：**7 处**被 `StatusBarManager` 的 `LEGACY_STATUS_KEYS` /
  `PREFIXED_STATUS_KEYS`（"中文串 → key"兼容映射）**兜底翻译**了，界面并不会露中文；
  **1 处**（`TouchCartController` 的「交接班完成，正在退出…」）**真未翻译**，切到 en 会是中文。
  现全部改为显式 i18n 调用（新增 1 个 key `status_message.shift_ending_logout`，四份语言包同步）；
  库存那处三元拆成 if/else，避免 `get(cond ? A : B)` 这种门禁不喜欢的形状。
- 新规则 `HardcodedUiTextPolicyTest.statusBarTextIsNeverHardcodedRepoWide`：扫 `src/main` 全部 `.java` 的
  `updateStatus/updateSuccess/updateWarning/updateError/updateInfo` 调用，**平衡括号取实参、跳过注释、
  只看字符串字面量**里是否有中文（i18n key 全 ASCII，故参数里有中文字面量必是硬编码）——
  续行、三元、拼接都逃不掉；我上一轮正是被单行 grep 骗过。防空转：扫到的调用数须 > 50（2026-09 实际 97 处）。
  变异验证：把 `ProductEditController` 的文案改回**续行**中文 → 变红。
- **兼容映射表的双面性**：它让迁移期不露中文，但**会让硬编码在运行时被翻译，从而掩盖问题**
  （源码门禁看不见、复制到别处就失效、未命中时 `localizeStatus` 原样返回中文=静默不翻译）。
  因此调用方一律直接传 key，这张表只当安全网。
- **顺带修好两条被"文案迁移"打破的门禁**：`SuccessStatusLevelPolicyTest` 与
  `PromotionLoginRechargeFeedbackPolicyTest.successFeedbackUsesSuccessLevel` 原本钉的是
  `updateSuccess("商品删除成功: " + name)` 这类**硬编码完整文本**，于是把文案迁到 i18n 会被误判成
  "不再使用成功级别"。现改为按**文案 key 断言级别**（新增测试助手 `StatusBarAssertions`：
  取 key 所在语句，断言用 `updateSuccess(` 且未回退 `updateStatus(`）——门禁的意图是"级别"，
  不该与文案字面量耦合。

变异验证：① 打印预览按钮改回 `new Button("打印")` → 1、4 变红；
② 英文包删掉 `runtime.splash_finishing` → 3 变红（`I18nBundleConsistencyTest` 同时变红）；
③ `InventoryView.fxml` 写回 `promptText="全部"` → 2 变红。均确认后还原。

### 仍待处理

- ~~其它控制器里的 17 处状态栏文案~~ **已清零（2026-09，第四批）**：7 个文件 / 17 处全部迁走，
  **全部复用既有 `status_message.*` / `success.export` / `runtime.*_in_progress` key（新增 0 个同值 key，
  只新增 1 个真正不同的文案 `runtime.backup_already_running`）**；门禁 `MIGRATED_FILES` 扩到 13 个文件，
  ~~全仓库 `updateStatus/updateWarning("中文")` 扫描结果为 **0**~~ —— **该结论不准确（2026-09 复核订正）**：
  那条 grep 要求"中文紧跟左括号"，于是漏掉了中文写在**续行**、或第一实参是三元的写法。
  用"平衡括号取实参 + 跳过注释 + 只看参数字面量"精确重扫，发现仍有 **8 处**把中文传给状态栏。
- `PackageWizardController`（89 处中文字面量）与 `SplashWindow` 品牌名以外的文案：打包向导是**维护者工具**
  （不是终端用户界面），优先级低；若要让向导也支持英文，需单独一批
- ~~159 个无人引用的 bundle key~~ **已清理（2026-09，第五批）**：精确判定后删除 **196 个** key
  （四份语言包各删 196 行，1924 → 1728）、**60 个只指向它们的常量**（`I18n.java` 133 → 73，
  含一个既存死常量 `I18n.INVOICE_VOID = "invoice_void"`——该 key 从来没在语言包里存在过，
  一旦被引用就会直接显示 `invoice_void`）；新增 `I18nUnusedKeyPolicyTest`（2 项）防止再生。
- ~~`AGENTS.md` 未被 git 跟踪~~ **已入库（2026-09，第六批）**：`.gitignore` 里改为一句说明（防止被加回去），
  并把文件自身过时的"AGENTS.md 被 gitignore"改成"两份都是跟踪文档"；顺带修正它两处过时表述
  （"语言包有三份"→ 四份含 `messages.properties` 回退包；`logs/` → `logs/*.log`）。
  入库前逐条核实了它的断言（`ProductDAOCold` 确已不存在、compose 只自动挂载 `00-init-complete.sql`、
  必填变量展开、CI 命令），只有上述两处需改。另修掉 `README.md` 里一处残留的"380 个测试用例"（改为"当时"）。

---

## TD-015 `String.format` 用默认 locale 格式化金额（73 处）

**类别**：正确性（潜在）　**状态**：**已修复（2026-09）**
**提出来源**：2026-09 全量审计（TD-014 顺带发现）

### 现状（修复前）

主源码里有 **73 处** `String.format("...%.2f...", ...)` 未指定 `Locale`（另有 5 处用常量格式串
`PERCENT_FORMAT = "%.2f%%"`）。`String.format` 默认用 `Locale.getDefault(FORMAT)`：目标环境是中文
（`.` 作小数分隔符）所以当时不会出问题，但在德语/法语等默认 locale 的机器上：

- **小票**（`ReceiptPrinter`/`PrintUtil`，共 19 处）会打出 `1,50`，与收款金额、落库值对不上；
- **CSS 颜色**（`FXConstants.toCssColor` → `rgba(%d, %d, %d, %.2f)`）会变成 `rgba(18, 52, 86, 0,50)`，
  JavaFX 直接**静默丢弃整条样式**（这类错误最难点查：没有报错，只是样式不生效）；
- 任何会被再次解析的字符串（`Double.parseDouble("1,50")` 直接抛异常）。

### 修复（2026-09）

- **73 处字面量格式串**统一改为 `String.format(java.util.Locale.ROOT, ...)`（25 个文件），
  另外 5 处用常量的（`ProfitReportController.PERCENT_FORMAT`）同样补上。
  选 `Locale.ROOT` 而不是某个语言：这些是数据/机器字符串（小票行、CSS、消息、金额），
  而面向用户的货币符号与千分位由 `CurrencyUtil` 负责——它本来就固定了
  `DecimalFormatSymbols(Locale.SIMPLIFIED_CHINESE)`，不受默认 locale 影响。
- 复核：`grep 'String\.format([^"]' | grep -v Locale.ROOT` 归零；含浮点转换的未固定点归零。

### 门禁（3 项，全部做过变异验证）

- `com.cashier.security.LocaleFormatPolicyTest.floatFormattingAlwaysPinsLocale`：
  扫描 `src/main/java` 所有 `String.format(...)`，**按引号配对解析第一个实参**（格式串内部含逗号时
  用字符类截断会漏判——第一版门禁就栽在这里，变异测试当场发现），字面量含浮点转换
  （`%f`/`%.2f`/`%e`/`%g`）而未带 `Locale.ROOT` 即失败；**常量格式串会在全仓库范围内解析**
  （`PERCENT_FORMAT = "%.2f%%"` 这类也能抓住）。
- `LocaleFormatPolicyTest.receiptAndStyleFormattingPinnedLocale`：小票三件套 +
  `FXConstants` + `CurrencyUtil` 这几个"错了就静默失效"的文件单独再守一遍（失败信息更直白）。
- `LocaleIndependenceBehaviorTest`（**行为级**）：把 JVM 默认 locale 真的切成 `de_DE`，
  先断言参照系（该 locale 下 `String.format("%.2f", 1.5)` 确实是 `1,50`），
  再走真实代码路径断言金额工具仍是 `1.50`、千分位仍是 `1,234.50`、
  小票金额行是 `180.00`、`FXConstants.toCssColor` 是 `rgba(18, 52, 86, 0.50)`、
  支付方式归一化仍是 `CASH`。

变异验证：① 把 `FXConstants.toCssColor` 的 `Locale.ROOT` 去掉 → 三项全红
（行为级测试给出 `实际: rgba(18, 52, 86, 0,50)`）；② 把 `PERCENT_FORMAT` 的 5 处 `Locale.ROOT` 去掉 →
全局门禁靠常量解析抓住。均确认后还原。

### 额外验证：整套测试在德语 locale 下跑

`mvn test -DargLine="-Duser.language=de -Duser.country=DE"` → **713 个用例全绿**，
说明除金额/CSS 外，其余依赖默认 locale 的假设也已不存在（测试里原本就可能断言格式化结果）。

### 剩余

- 无（`String.format` 的浮点格式化已全部固定 Locale）。
  注：纯 `%d`/`%s` 的格式化不强制带 Locale——它们不受 locale 影响。


## TD-016 界面集合字段未初始化，异步回调顺序一变就 NPE

**类别**：UI 正确性　**状态**：**已修复（2026-09）**
**提出来源**：**Windows 实机测试**（TD-011 验收过程中用户跑出来）

### 现象（用户 Windows 实测，macOS 本机跑不出来）

打开库存页与采购订单页时，FX 线程抛两次 NPE、界面卡在未刷新状态：

```
NullPointerException: Cannot invoke "javafx.collections.ObservableList.size()" because "this.inventoryList" is null
    at InventoryController.updateCountLabel(InventoryController.java:390)
    at InventoryController.applyFilters(InventoryController.java:247)
    at InventoryController.handleCategoryFilter(InventoryController.java:203)
    at InventoryController.lambda$initialize$1(InventoryController.java:139)
    at ... ComboBox$ComboBoxSelectionModel ... InventoryController.lambda$loadCategories$8(InventoryController.java:193)

NullPointerException: Cannot invoke "java.util.Map.values()" because "this.orders" is null
    at PurchaseOrderController.filterOrders(PurchaseOrderController.java:194)
    at PurchaseOrderController.lambda$loadSuppliers$5(PurchaseOrderController.java:160)
```

### 根因：两个异步加载器互相依赖，读字段时另一个还没赋值

2026-09 那批"页面打开即查库改为后台执行"（TD-006 第三批）把两个加载器都异步化了，
于是**谁先回来不确定**：

- `InventoryController`：`loadCategories()` 的回调里 `categoryFilterComboBox.getSelectionModel().select(0)`
  会触发 `initialize()` 里注册的分类监听器 → `handleCategoryFilter → applyFilters → updateCountLabel`
  读 `inventoryList`；而它要到 `loadTableData()` 的回调才被赋值。分类先回来就 NPE。
- `PurchaseOrderController`：`loadSuppliers()` 的回调末尾会调 `filterOrders()` 刷一次（为了解析供应商名），
  而 `filterOrders()` 读 `orders`；`orders` 要到 `loadOrders()` 的回调才赋值。供应商先回来就 NPE。

macOS 上恰好是"订单/商品先回来"，所以本机全绿也发现不了——**只有实机能暴露顺序敏感缺陷**。

### 修复：控制器里的集合状态字段一律"声明即初始化"

```java
private final ObservableList<Product> inventoryList = javafx.collections.FXCollections.observableArrayList();
private final Map<Integer, Product> inventoryMap = new HashMap<>();
```

数据没到就表现为**空集合**而不是 null，读取顺序不再敏感。共改 **14 个控制器 / 25 个字段**：

- 崩溃的 4 个：`InventoryController.inventoryList/inventoryMap`、`PurchaseOrderController.orders/suppliers`
  （这两个文件的赋值语句同步改成 `clear()` / `setAll(...)`，并把 `final` 加上）；
- 扫描出的同类潜在竞态 3 个（用户还没踩到）：`CartController.inventoryMap`（异步加载完成后才能入车）、
  `MemberController.memberList`、`SupplierController.supplierList`（搜索回调会经 `updateCountLabel` 读）；
- 其余 18 个同类字段（`PromotionController`、`ShiftController`、`TransactionController`、`UserController`、
  `PurchaseApprovalController`、`PurchaseInboundController`、`PurchaseReportController`、
  `InventoryReportController`、`InventoryCheckController`、`InventoryAlertController`、
  `ProfitReportController`、`StatisticsController`、`PurchaseOrderController.orderList` 等）
  一并加初始化器，让"集合字段永不为 null"成为**统一不变量**（已确认全仓库没有任何
  `== null` / `!= null` 依赖这些字段，故行为等价）。
  唯一排除项：`SearchController.resultsList` 是 FXML 注入的 `ListView` 控件（`@FXML private ListView<HBox> resultsList;`），
  不是集合字段。

### 门禁（2 项）

`com.cashier.security.UiStateInitializationPolicyTest`：

1. `controllerCollectionsAreInitializedAtDeclaration`：扫 `src/main/java/com/cashier/controller` 全部源码，
   任何"集合类型 + 无初始化器"的私有字段即失败（`ListView` 等控件类型不在规则内），
   失败信息里直接写明 Windows 撞过的 NPE 与改法；
2. `previouslyCrashingFieldsStayInitialized`：把曾经崩溃的 5 个字段作为回归锚点逐个断言"声明处即初始化"。

变异验证：把 `InventoryController.inventoryList` 的初始化器去掉 → 门禁变红（已还原）。

### 另外顺手修的（同一轮实机发现）

- **Windows 控制台中文日志乱码**：用户贴出的日志是 `鎴愬姛鍔犺浇 ... 瀛椾綋` 这种莫吉托乱码——
  JVM 按 `-Dfile.encoding=UTF-8` 输出，而控制台代码页是 936。已在 `start.bat` 头部加
  `chcp 65001 >nul`（与 `release.bat`/`docker/start-mysql.bat` 的既有做法一致），中文日志从此可读。
  该文件仍是纯 ASCII，不影响批处理解析。

---

---

## TD-022 F8/命令面板进收银台后导航高亮消失（已修复，2026-09 审计）

### 现象与根因

`MainController.handleCheckout()`（F8、命令面板"收银台"都走它）调用 `setActiveButton(checkoutBtn)`，
而 `MainView.fxml` 里**没有** `checkoutBtn`——收银台导航按钮的 id 是 `cartBtn`。

FXML 注入是**按名字绑定**：名字对不上不报错，字段恒为 `null`。`setActiveButton(null)` 会先把旧按钮的
高亮去掉，再因为参数为 null 而不设新高亮 → 进收银台后**没有任何导航项处于选中态**。
同一方法里 `configurePermissions()` 的 `setButtonAccess(checkoutBtn, …)` 也是空操作
（幸好 `cartBtn` 已被同样地限制，且 `handleCheckout` 自身有 `requirePermission`，**没有**权限漏洞）。

另外 `handleCheckout()` 整段是 `handleCart()` 的复制粘贴，只有日志文案和高亮目标不同。

### 修复

- 删除陈旧字段 `checkoutBtn` 与那行空操作 `setButtonAccess`；
- `handleCheckout()` 改为委托 `handleCart()`（购物车与结账自 v2.6.0 起已是同一个标签页）。

### 门禁：`FxmlControllerBindingPolicyTest`（3 项）

1. `everyFxmlHandlerResolvesToControllerMethod`：FXML 里每个 `onAction="#xxx"` 都能在
   `fx:controller` 指定的类里找到方法（写错会在加载视图时抛 `LoadException`）；
2. `everyInjectedFieldHasMatchingFxId`：每个 `@FXML` 字段至少在一个声明该控制器的 FXML 里有同名
   `fx:id`，豁免表 `KNOWN_ORPHAN_FIELDS` 每条都要写清原因（现有 7 条：`CartController` 两个遗留字段
   代码已判空 + 5 个从未被引用的死字段），且**豁免失效即报错**（防止表腐烂）；
3. `checkoutEntryPointSharesCartNavigation`：回归锚点，钉住"不得再出现 `checkoutBtn` 调用/字段"
   与"`handleCheckout` 必须委托 `handleCart()`"。

变异验证（4 处）：加一个无 fx:id 的 `@FXML` 字段 → 第 2 项红；把某 FXML 的 `onAction` 改错名字 →
第 1 项红；豁免表写一个不存在的字段名 → 第 2 项"豁免表过期"红；把 `handleCart()` 换成别的语句 →
第 3 项红。另注：第 3 项第一版被**注释里提到的旧写法**误红，故加了 `withoutComments`（保留字符串字面量）。

## TD-023 盘点单保存/删除非原子（已修复，2026-09 审计）

### 现象与根因

`InventoryCheckController.handleSaveCheck` 编辑路径原来依次调用
`inventoryCheckDAO.update(newCheck)` → `inventoryCheckItemDAO.deleteByCheckId(...)` → 逐条 `insert(...)`。
这些方法各自从连接池取一条 autocommit 连接并立即提交，于是 `DELETE` 先落库；
之后任一步失败（连接抖动、数值非法、进程被杀），结果就是**已提交的删除 + 半截明细**：
表头仍写着 N 条、差额从此算错，且 `canComplete` 要求 `checking`，用户也无法重做。
`handleDeleteCheck` 同样是两条独立提交（先删明细、再删表头）。

### 修复

- 新增 `insertWithConnection` / `updateWithConnection` / `deleteWithConnection`
  （`InventoryCheckDAORefactored`）与 `insertWithConnection` / `deleteByCheckIdWithConnection`
  （`InventoryCheckItemDAORefactored`），保存与删除整体放进 `DatabaseManager.executeBooleanTransaction`；
- 取号 `generateNextCheckNo` 挪到事务**外**（避免事务内再占一条池连接）；
- 新增空明细守卫（`items.isEmpty()` → 报错返回，与 `PurchaseOrderController` 一致）：
  明细加载失败时列表为空，空列表提交等于把盘点明细整批清空；
- 明细加载失败的 catch 不再只记日志，改为弹出可见错误。

### 门禁（2 项）

- `InventoryCheckSaveAtomicityTest`（行为级，H2）：在事务里模拟"改表头 → 删旧明细 → 插新明细 → 抛异常"，
  断言**旧明细还在、表头改动一并回滚**；成功路径断言明细被整体替换。
  这是钉住"`*WithConnection` 确实参与调用方事务"的关键测试（H2 已启用外键，TD-009）；
- `WriteAtomicityPolicyTest.inventoryCheckSaveIsAtomic`（源码级）：保存方法必须用
  `executeBooleanTransaction` + 三个 `*WithConnection`，且全文件不得再出现自带连接的
  `update(newCheck)` / `insert(newCheck)` / `insert(...)` / `deleteByCheckId(...)`；并断言空明细守卫存在。

变异验证：把保存改回自带连接写法 → 源码门禁红（行为级测试仍绿——它只覆盖 DAO 层参与事务，
控制器接线由源码门禁覆盖，两者互补，这里如实记录）。

## TD-024 退款还原库存丢弃结果 + 相对增减库存不递增 version（已修复，2026-09 审计）

### 现象与根因

`TransactionApiController` 的退款事务里，还库存是
`productDAO.updateQuantityWithConnection(conn, product.id, product.quantity);`——**返回值被丢弃**。
`transaction_items.product_id` 允许为 NULL（DAO 映射成 0），旧数据里确实存在；
此时 UPDATE 影响 0 行，但接口照样回 200「退款成功」。

更隐蔽的一条：`updateQuantityWithConnection` 的 SQL 是 `quantity = quantity + ?`，**不动 `version`**，
而结账扣库存走 `updateWithVersionWithConnection`（`quantity` 绝对值 + `WHERE id=? AND version=?`）。
退款在结账的"快照读之后、UPDATE 之前"提交时，结账仍能用旧 version 命中，
把刚还回的库存**覆盖掉**——即退款成功、货没回来。入库、盘点调整同样受影响。

### 修复

- 退款侧新增 `restoreInventoryForRefund(conn, product)`：`product.id <= 0` 时记 WARN 跳过
  （旧数据无从知道还给哪个商品），`id > 0` 但影响 0 行时记 WARN（商品已删除；**钱照退**，
  不能因为商品下架就让顾客收不到退款），但**结果必须被消费**；
- `updateQuantityWithConnection` 补上 `version = version + 1`（并顺手让非连接版
  `updateQuantity` 委托它，保持口径一致）——三处调用（退款还原、入库、盘点调整）同时受益。

### 门禁

- `WriteAtomicityPolicyTest.refundRestoreConsumesUpdateResult`（源码级：必须 `if (!...updateQuantityWithConnection(...))` 且留 WARN）；
- `WriteAtomicityPolicyTest.stockQuantityUpdateBumpsVersion`（源码级：方法体必须含 `version = version + 1`）；
- `ProductDAOTest.testUpdateQuantity`（行为级：调用后 `version` 必须 +1）。

变异验证：去掉 `version = version + 1` → 行为测试与源码门禁同时红；把退款返回值改成丢弃 → 源码门禁红。

## TD-025 手工开票表头与明细不在同一事务（已修复，2026-09 审计）

`InvoiceService.createManualInvoice` 调 `invoiceDAO.insert(invoice)`，而 `insert()` 用**自己的**
autocommit 连接；`insertWithConnection` 内部先插表头（提交），再插明细。
明细失败（例如 `product_name` 超长、SQL 严格模式）时接口回 500，但表头已经落库：
发票列表里出现一张"有金额、无明细"的孤儿发票，用户重试还会再产生一张（`invoice_id` 每次重新生成）。
对比：`createInvoiceFromTransaction` 早就把同样的工作包在 `executeBooleanTransaction` 里。

修复：手工开票改走 `insertWithConnection(conn, invoice)` + `executeBooleanTransaction`。
门禁 `WriteAtomicityPolicyTest.manualInvoiceInsertIsAtomic`（变异：改回 `insert(invoice)` → 红）。

## TD-026 建库/迁移失败被吞掉（已修复，2026-09 审计）

`DatabaseManager.initializeDatabase()` 的 `catch (SQLException e) { logger.error(...) }` 只记日志，
而类静态块里的 `catch (Exception e)` 本意是打印**可行动的**"数据库初始化失败 + 排查指引"
（MySQL 未启动 / 主机端口 / CASHIER_DB_PASSWORD …）并终止启动——因为异常被吞，这段永远不会触发。

后果：建表或 `upgradeTableStructure` 中途失败时应用照常启动，之后每个页面都报
`Table 'xxx' doesn't exist` / 未知列，用户看不出根因；半套迁移也会静默留存。

修复：`initializeDatabase()` 声明 `throws SQLException` 并在 catch 里 `throw e;`，交给静态块统一处理。
门禁 `WriteAtomicityPolicyTest.schemaInitializationFailureIsNotSwallowed`（变异：去掉 `throw e;` → 红）。
注意：本机没有可连的 MySQL，**"初始化失败时确实弹出该提示并退出"需在 Windows 实机确认**
（只想验证提示文案的可以临时改错 `config/database.properties` 的端口）。

---

## 第五批审计待办（2026-09，TD-027 ~ TD-035）

本批只修了**会造成数据丢失/错误或直接卡死**的 TD-022 ~ TD-026；下面这些已核实但未改，逐条给了
可复现的输入或路径，下一批按优先级处理。
（**2026-09 更新**：TD-030 / TD-031 / TD-034 已在下一批修复，详见上文
"TD-030 / TD-031 / TD-034 修复"；以下三节保留原始复现记录，状态以表格为准。）

### TD-027 ZIP 导入（死代码 + 必失败）

`ProductDataImporter.parseZipData` 用 `try (BufferedReader reader = ...)` 包住 `ZipInputStream` 的每个 entry，
读完第一个 entry 时 reader 关闭会**连带关闭 ZipInputStream**，随后 `closeEntry()`/`getNextEntry()`
抛 `IOException: Stream closed`——已用最小程序实测复现（单 entry 的 zip 也一样）。
但 `importFromGitHub()` 全仓库**零调用方**（`SettingsController` 只调 `importFromCSV`），
`DATA_FILES` 两项却都是 `.zip`，说明这条路径从未真正跑通。需要产品决定：删掉死代码，
还是修好 ZIP 解析（改用不关闭底层流的包装，或按 entry 读字节）。

### TD-028 POI CVE

`pom.xml` 用 `poi`/`poi-ooxml` 5.2.5。CVE-2025-31672（重复 zip 条目导致的输入校验问题）影响
**poi-ooxml < 5.4.0**，5.2.5 在受影响区间内。但全仓库只有 `ExportUtil` 使用 POI 且仅
`new XSSFWorkbook()` **写**文件，没有任何解析上传/下载 Office 文件的代码路径 → 该 CVE 在本项目
**不可达**。若日后加入 Excel 导入（`ProductDataImporter` 目前只读 CSV），必须同批升级到 ≥5.4.0。

### TD-029 报表按区间全量物化

`ReportApiController:49/128`（日/月报）、`StatisticsController:253`（用户可选区间）、
`ShiftController:789` 用 `TransactionDAORefactored.findByDateRange(start, end)`，
实现是 4 表 JOIN 把区间内**每一笔交易和每一条明细**都构造成对象再在 Java 侧聚合。
列表接口已有 `findByDateRange(start, end, limit)` 重载（`TransactionApiController:46` 已用）。
建议：报表侧改为 SQL 聚合（`SUM`/`GROUP BY`）或分页流式处理。

### TD-030 改密对话框取消 → 登录界面永久禁用（优先） — **已修复，见下文专节**

`LoginController:106` 先 `setLoginState(true)`（禁用用户名/密码框 + 显示 loading），
成功路径 `:152-161` 只调用 `showPasswordChangeDialog(user)` / `switchToMainView(user)`，
**从不** `setLoginState(false)`；改密对话框 `:234` 带 `CANCEL`，`:262` 用
`showAndWait().ifPresent(...)`——取消/关窗时 Optional 为空，什么都不执行。
结果：首次登录用户取消改密后，登录页两个输入框是灰的、转圈还在转，只能杀进程。
修法：对话框返回后（含取消、含异常）一律 `setLoginState(false)`。

### TD-031 网关下单在 FX 线程且无超时（优先） — **已修复，见下文专节**

`CartController:1236` / `TouchCartController:1510` 直接调用
`PaymentService.createPaymentOrder(...)`，其内部 `provider.createOrder(order)` 用
`HttpClient.newHttpClient()` 发 HTTP（`AlipayPrecreatePaymentProvider:134`、`WechatNativePaymentProvider:193`），
全仓库 **没有任何 `.timeout(...)`**。网关不可达时 `java.net.http` 的默认行为是无限等待，
收银员按了"微信/支付宝"后整个界面卡死且无法取消。
修法：`.connectTimeout(...)` + `.timeout(...)`，下单放到 `UIOptimizer.runInBackground`。

### TD-032 FX 线程上的同步查库

- `PurchaseOrderController:881` / `InventoryCheckController:651`：`searchField` 的 text 监听器
  每敲一个字符就 `productDAO.search(...)`（仓库里同类输入的正确做法是
  `UIOptimizer.runInBackground` + `PauseTransition` 防抖，见 `TouchCartController:184`）；
- `CartController:1519` / `TouchCartController:892`：每次加/减商品、改数量、开始支付都
  `PromotionDAORefactored.findActive()`（无缓存）；
- `CashierSystemFXApplication:770/778/832/840`：登录时同步 `InventoryAlertService.start()`
  （内部立刻跑一次全表低库存查询并逐条发通知）与 `BackupService.start()`。

### TD-033 触屏切换语言泄漏资源

`TouchCartController:1941`（`switchLanguage` 内）直接 `application.switchToPosModeView(currentUser)`，
没有调 `cleanup()`：`clockTimeline`（INDEFINITE，1 秒）继续跑、`bindStatusBar()` 绑定的全局监听又叠一层、
旧场景被 Timeline 引用无法回收；`BackupService.start()`（`:543-561`）没有
`InventoryAlertService` 那样的 `isRunning` 守卫，于是每切一次语言就多一个非 daemon 的
`ScheduledExecutorService` 线程。

### TD-034 金额/数量口径（7 项，均有"输入 → 错误输出"） — **已修复，见下文专节**

1. `InvoicePrintService:218` `rate.multiply(100).setScale(0)` 未给 `RoundingMode`：
   税率 0.065（6.5%）→ `ArithmeticException: Rounding necessary` → 打印发票接口 500
   （现有测试只用 0.13 所以没暴露）；
2. `CartController:2193-2194` 挂单折扣用 `double discountRate = discount.doubleValue()/10.0` +
   `BigDecimal.valueOf(1 - discountRate)`：总价 1.10、银卡 9.5 折存 `final_amount=1.04`，
   而结账实际收 1.05（触屏台用 `BigDecimal.subtract`，两台收银机口径不一致）；
3. `ReturnService:53-56` 退款单价按行取整后再求和：2×10.10、9.5 折 → 实付 19.19 却退 19.20；
   反向也有少退 1 分的情况（API 侧退了准确的 `finalAmount`，但 `return_order_items` 之和与之不符）；
4. `ReturnService:275-277` 积分按"每次退货"取整：实付 10.10 得 101 分，分两次各退 5.05 →
   冲减 51+51=102 分（顾客损失从未获得的积分）；
5. `MemberService:70` 充值积分 `amount * 10` 不取整（`points` 是 DECIMAL(10,2)），
   而 `MemberEditController:149` 显示时 `intValue()` 截断、`:233` 保存这个截断值 →
   充值 10.55 元得 105.5 分，打开会员编辑再保存就变成 105.00（0.5 分静默丢失）；
6. `promotions.discount` 列是 DECIMAL(10,2)，而 `PromotionController:640` 接受任意精度
   （只校验 0<x<1）：输入 0.985 存成 0.99 → 1000 元订单少打 5 元折；
7. `InvoiceItem:44-46` 行税额不取整、`Invoice:104-115` 用未取整值累加表头：
   3×33.33 @13% → 各行 4.33（Σ=12.99），表头 13.00，实付合计与明细差 1 分。

### TD-035 其它

- `PackageWizardController:508` 的 `Task.call()` 在 worker 线程里调 `appendLog` →
  `logTextArea.appendText(...)`（Node 写，仅进程输出那处包了 `Platform.runLater`）；
  该向导的 `Executors.newSingleThreadExecutor()` 非 daemon 且只由 `handleCancel` 关闭；
- `NotificationManager:93-98` 的 `scheduleAtFixedRate` 任务体没有 try/catch：
  按 `ScheduledExecutorService` 语义，任务抛一次异常就**永久取消**后续通知（当前监听器各自兜了异常，属潜在）；
- `DataService.hasActiveShift():428-435` 吞 `SQLException` 返回 false，调用方（`CartController:568/1397`）
  于是把数据库故障显示成"请先开班/没有活跃班次"——排查方向被带偏；
- ~~11 处~~ **8 处** `new Thread(...)` 未设 `setDaemon(true)`（2026-10 复核订正：原文举的
  `StatisticsController:251` **已经**有 `worker.setDaemon(true)`（同文件 274 行），数量也数错了。
  实测 28 处 `new Thread(...)` 里 20 处已设 daemon，剩 8 处：`AuditLogController:123`、
  `LoginController:109`、`MemberController:135`、`PasswordResetController:93`、`RechargeController:163`、
  `SettingsController:1562`、`UserController:168`、`installer/Installer.java:348`）：
  线程正常结束无碍，但卡住的 JDBC 调用会让关窗后进程不退出；
- `CurrencyUtil:63-65` 的 `DecimalFormat` 未设 `RoundingMode`（默认 HALF_EVEN），
  而全应用金额是 HALF_UP：`format(1.005)` → `1.00`（只影响 3 位以上小数的中间值展示）；
- `InventoryAlertController:410-416` 先弹"检查完成"再异步刷新（且刷新失败只记日志，表格静默保留旧数据）。

---

## TD-036 结尾斜杠绕过全部角色门禁（已修复，2026-09 审计，安全）

### 漏洞

`AuthorizationMiddleware.authorize` 用 `ctx.path()`（**原始请求 URI**）做全串比对，
而 Javalin 6 默认 `ignoreTrailingSlashes = true`：`PUT /api/members/1/` 照样命中路由
`/api/members/{id}`，但 `path.matches("/api/members/[^/]+")` 因末尾多一个斜杠而不成立 →
**整条角色门禁被跳过**。收银员 token 即可：

| 路由 | 越权后果 |
|---|---|
| `POST /api/transactions/{id}/refund` | 任意交易退款（还库存、退回余额/积分、写现金退款日志） |
| `POST /api/payment/{paymentId}/refund` | 渠道退款（不超过已付金额） |
| `PUT /api/members/{id}` | 改等级/折扣（折扣 0 = 免费卖）、改手机号（可把会员"换号"后花其余额/积分） |
| `POST /api/invoices/manual`、`POST /api/invoices/{id}/void` | 任意开票/作废 |
| `PUT /api/invoices/seller-info` | 改全局开票方名称/税号/银行账号（正常仅管理员） |
| `PUT /api/payment/config` | 改微信/支付宝 appId、密钥、`alipayGateway`、回调地址（正常仅管理员） |

财务角色同样能靠斜杠拿到两条管理员专属配置。

### 我自己的复现（不只依赖报告）

用项目 classpath 起了一个最小 Javalin 6.1.3 应用（`before` 打印 `ctx.path()`，路由 `/api/members/{id}`）：

```
PUT /api/members/1    -> HTTP 200 | before path=/api/members/1  matchedPath=*
PUT /api/members/1/   -> HTTP 200 | before path=/api/members/1/ matchedPath=*   <-- 命中处理器，且 before 看到的是带斜杠原始路径
PUT /api/members/1//  -> HTTP 404 | Endpoint PUT /api/members/1// not found
PUT /API/members/1    -> HTTP 404
```

即：**单结尾斜杠是有效攻击向量**，双斜杠与大小写变体会 404（不能利用）。

### 修复

`AuthorizationMiddleware.isAllowed` 入口先归一化：`withoutTrailingSlash(path)` 去掉全部末尾斜杠
（`"/"`/`""` → `"/"`），再走原有的 `equals`/`matches` 判定。所有 `app.before` 调用点自动受益，
正常写法的行为完全不变；`/api/printers/{id}/receipt/` 之类原本因斜杠而"更严"的判定也一并归位。

刻意**没有**改 `ApiServer.isPublicApiPath`（仍是精确匹配）：带斜杠访问登录/健康检查会 401 而不是放行，
属 fail-closed，不影响安全。

### 门禁

`AuthorizationMiddlewareTest.trailingSlashCannotBypassRoleGate`：13 条受控路由 × {正常写法, 一个斜杠, 两个斜杠}
都必须返回 false，并断言 finance 拿不到管理员配置、`/api/members/10/recharge/` 与
`GET /api/invoices/seller-info/` 这些**本该放行**的仍放行（防止归一化过度收紧），
外加 `withoutTrailingSlash` 的边界（`"/"`、`""`、多斜杠）。

变异验证：把 `String normalized = withoutTrailingSlash(path);` 改回 `String normalized = path;` →
测试立刻报 `POST /api/transactions/T1/refund/ ==> expected: <false> but was: <true>`（复现修复前的越权）。

## TD-037 ~ TD-039 REST API 审计的其余发现（未改，需决定）

同一轮 API 审计（解析全部 92 条路由 + 用真实 Javalin 验证路由/中间件行为 + 逐路由跑真实
`isAllowed()`）还发现三条，均**已核实但未改**，因为都涉及"权限该松还是该紧"的产品判断：

- **TD-037 钱箱**：`POST /api/printers/{id}/cashdrawer` 是管理员专属，
  但同组的 `POST /api/printers/{id}/receipt` 被排除规则放行，而请求体 `openCashDrawer:true`
  会走到 `PrintTask.createReceiptTask(content, printLogo, true)` → `PrinterManager.print(...)`
  → `printer.openCashDrawer()`。于是收银员 token 能开钱箱。两条路的业务含义不同
  （收银员找零本来就要开钱箱 vs 抽屉开启属受控操作），需产品先定口径。
- **TD-038 发票字段自报**：`POST /api/invoices/from-transaction`（仅认证，收银员可调）把请求体交给
  `InvoiceService.createInvoiceFromTransaction`：`sellerName/sellerTaxId/...` 覆盖管理员配置的全局开票方，
  `createBy` 伪造开票人，`payee/checker/taxRate` 也来自请求体（`taxRate` 直接决定税额与价税合计）。
  明细金额是从交易重算的，所以货值不能伪造。桌面端没有任何调用方。
- **TD-039**：`POST /api/invoices/{id}/print` 仅认证，可把任意 `pdfPath`/`imagePath` 写进任何发票
  （当前这两个字段不被当文件路径读取，TD-001 的门禁守着，所以影响限于数据伪造）；
  另外 `payment.mode=mock` 时回调只要 `mock_signature` 等于配置密钥即可把待支付订单标记为已付——
  本地未跟踪的 `config/payment.properties` 里确实是 mock + 低熵密钥，但仓库里的示例默认
  `disabled` + 占位符，是否会在生产开 mock 需运维确认。

---

## TD-030 / TD-031 / TD-034 修复（2026-09，第二批）

### TD-030 强制改密对话框取消后登录界面永久禁用

`handleLogin` 在异步校验前调用 `setLoginState(true)`（禁用用户名/密码框 + 显示转圈）。
首次登录用户会被弹"必须改密"对话框，而 `showPasswordChangeDialog` 只有
"改密成功且切到主界面"这一条出路：原来用 `dialog.showAndWait().ifPresent(response -> {...})`，
**取消/关窗时 Optional 为空，什么也不做**；改密抛异常时也只弹了个错误。
两种情况都停在"输入框是灰的、转圈还在转"的登录页，只能杀进程。

修复：把结果记进 `boolean switchedToMain`，`showAndWait()` 返回后统一
`if (!switchedToMain) setLoginState(false);`；外层 catch（对话框本身出错）也恢复。

门禁 `LoginStateRecoveryPolicyTest`：断言恢复调用出现在 `showAndWait()` **之后**、
数量 ≥ 2（内层 catch + 外层 catch）、且挂在 `if (!switchedToMain)` 分支上。
**这里踩过一次坑**：第一版只数了 `setLoginState(false)` 的出现次数与位置，
把条件写成 `if (switchedToMain)`（语义完全反了）居然还是绿的——变异测试抓出来的，
因此补了"必须用取反分支"的断言。

### TD-031 网关 HTTP 无超时 + 下单跑在 FX 线程

两个渠道 provider 都用 `HttpClient.newHttpClient()`（**默认无限等待**）且请求不带 `timeout`，
而收银台在 FX 线程直接调用 `PaymentService.createPaymentOrder(...)`：
网关不可达时整个收银界面卡死且无法取消，收银员卡在一笔交易中间。

修复两处：

1. `AlipayPrecreatePaymentProvider` / `WechatNativePaymentProvider`：
   `HttpClient.newBuilder().connectTimeout(5s)` + 每个请求 `.timeout(15s)`；
2. 两个收银台的 `startElectronicPayment` 把下单改到 `UIOptimizer.runInBackground(...)`
   （仓库既有的后台→回 FX 线程范式），并在异步窗口内先置 `paymentInProgress` /
   `setPaymentInProgress(true)`，挡住重复点击造成两笔支付单；失败分支负责恢复该标记。

门禁 `ElectronicPaymentSafetyPolicyTest` 新增 2 项：网关必须同时有连接超时与请求超时
（且不得再出现 `HttpClient.newHttpClient()`）；两个 `startElectronicPayment` 内必须出现
`UIOptimizer.runInBackground(`。变异验证：去掉 `.timeout(...)`、把触屏台改回同步调用 → 各自变红。

**顺手撞上的棘轮门禁**：`ControllerSizePolicyTest` 钉着 `CartController ≤ 2240` 行，
第一版用裸 `CompletableFuture.supplyAsync` 把它顶到 2245 行而被判红。
按门禁意图改为复用 `UIOptimizer.runInBackground`（少 13 行）后回到上限内——
这条门禁的作用正是"别把逻辑继续堆进巨型控制器"。

### TD-034 金额/数量口径 7 项

| # | 问题 | 修法 |
|---|---|---|
| 1 | `InvoicePrintService.formatPercent` 用 `setScale(0)` 不带 RoundingMode：税率 6.5% 时抛 `ArithmeticException: Rounding necessary`，打印发票接口 500 | 改 `setScale(0, HALF_UP)`（行为测试：6.5% → "7%"） |
| 2 | 挂单折扣用 `double discountRate = discount.doubleValue()/10.0` 反推：总价 1.10 银卡 9.5 折落库 1.04，结账实收 1.05 | `calculateHoldOrderFinal` 直接调用 `TransactionService.calculateFinalAmount(cartList, currentMember, appliedPromotion)`，折扣 = 总额 − 实付（与结账同一算法） |
| 3 | 退款按"每行单价四舍五入×数量"求和：2 × 10.10、9.5 折实付 19.19 却退 19.20（实测约 6.7% 的金额组合会偏） | 新增 `ReturnService.refundTotal(paid, gross, returnedGross)` 作为**权威退款额**（按实付比例取整、上限为实付）；`alignItemsToRefundTotal` 把明细之和压到不超过它。桌面退货单与 API 退款都改用这两个方法 |
| 4 | 积分冲减按"每次退货"四舍五入：实付 10.10 得 101 分，分两次各退 5.05 → 51+51=102 分 | 改 `FLOOR`（整单退货仍恰好冲掉全部积分；部分退货宁可少扣，也不扣会员从未得到的积分） |
| 5 | 充值产生小数积分（10.55 元 → 105.5 分），会员编辑界面 `intValue()` 显示 105，保存即丢 0.5 分 | 充值积分按"每元 10 分、向下取整"（与销售一致）→ 105；编辑界面改 `stripTrailingZeros().toPlainString()` 原样显示，历史小数积分不再被截断 |
| 6 | `promotions.discount` 是 DECIMAL(10,2)，界面却接受任意精度：输入 0.985 被存成 0.99（1000 元订单少打 5 元折） | 校验阶段拒绝超过 2 位小数（新增 key `promotion.validation.discount_scale`，三语） |
| 7 | 发票行税额不取整、表头用未取整值累加：3 × 33.33 @13% 表头 13.00、明细之和 12.99 | `InvoiceItem.calculateAmount` 逐行 `setScale(2, HALF_UP)`，表头累加取整后的行值 |

**关于第 3 项的口径（有意保留的取舍）**：`return_order_items.unit_price` 是 `DECIMAL(10,2)`，
而 `return_amount = unit_price × quantity`，所以"单行 2 件、应退 19.19"在语义上无法用
2 位小数单价表达（9.595 存不了）。因此口径定为：**退款金额以 `return_orders.total_amount`
（= `refundTotal`）为准**，明细行只允许少算（`alignItemsToRefundTotal` 逐分下调金额最大的行）。
即"钱一分不多退，明细之和 ≤ 实退"，账面上不会再出现明细之和大于退款额。
若日后要连明细也精确，需要把该列放宽到 DECIMAL(10,4) 或让 `return_amount` 独立于单价——留待有需要再做。

门禁与测试：`MoneyPrecisionPolicyTest`（4 项源码门禁：挂单算法/积分显示/促销精度/发票逐行取整）、
`ReturnServiceTest` 新增 3 项（比例取整、明细不超过总额、多次退货不超冲积分）、
`InvoiceServiceTest` 新增 1 项（行税之和 == 表头）、`InvoicePrintServiceTest` 新增 1 项（6.5% 可打印）、
`MemberServiceTest` 新增 1 项（充值 10.55 → 105 分）。
**11 处变异全部验证为红**（含把门禁条件写反的那次，见 TD-030）。

---

## 2026-10 第二轮全量审计（F1~F15 / G1~G3）

**审计方式**：`mvn -B -ntp clean verify` 从零跑三关（**796 用例全绿**、SpotBugs High 0、JaCoCo 达标）、
德语 locale 下全量再跑一遍（同样全绿）、覆盖率实测（行 22.4%）、并把工作拆给 3 路只读深审
（金额/库存/支付、认证/API/密钥、生命周期/文档一致性）——**每条结论都由本人回读源码复核**后才写进本文件。
本轮修 13 项、补 6 个门禁，未修的逐条写明状态。

### 钱与权限（优先修）

| 项 | 问题（每条都有"输入 → 错误输出"） | 修法 | 门禁/测试 |
|---|---|---|---|
| **F1** | 退货审批的"退款方式"下拉是死值（`String paymentMethod = refundMethod;` 之后再没被引用），而 `settleRefund` 是按 `return_orders.payment_method` 决定"退现金还是退回会员余额"→ 微信单在审批时改成现金，仍然退进会员余额（反之亦然） | 下拉框的值与状态迁移放进**同一条** UPDATE（`markApprovalWithConnection` 增加 `payment_method = COALESCE(?, payment_method)`）；选中退货单时把下拉框刷成该单原有支付方式 | `ReturnServiceTest` 4 项（落库/去向/非法值被拒/旧调用方不变）+ `ReturnApprovalRefundMethodPolicyTest`（控制器必须把值传下去，不得回归死变量） |
| **F2** | `POST /api/transactions` 接受 `会员余额` 但可以不带会员：库存照扣、交易照落，**没有任何账户被扣款**（余额校验在 `workingMember != null` 分支里，而该分支整段被跳过） | 服务层 `executeTransaction` 入口兜底拒绝（覆盖所有调用方），API 层提前回 400 给明确错误 | `TransactionApiControllerTest`/`TransactionServiceTest` 各 1 项（400 + 库存不动 + 不落单） |
| **F7** | **空口令是有效凭据**：`PasswordUtil.hashPassword("")` 能写入且 `verifyPassword("", hash)` 通过；`POST /api/users` 完全不校验口令，`login` 只挡 `null` → 空口令账号可直接登录；桌面"添加用户"口令留空也照样建号（OK 从不禁用） | 建号与登录两侧都拒绝空白口令；桌面新建用户对话框在口令/用户名为空时禁用 OK；更新接口把空白口令视为"不修改" | `UserApiControllerTest` 2 项、`AuthControllerTest` 1 项、`UserCreationPasswordPolicyTest`（桌面路径无法无头测，用源码门禁钉住"空口令不哈希 + OK 禁用"） |
| **F8** | `PUT /api/settings/{key}` 写的是 `config/settings.properties`——**全仓库没有任何代码读它**（桌面设置走 DB），接口却回 `success:true`；写失败也被本地 catch 吞掉 | 改为读写桌面端同一份 `settings` 表（`SystemSettingsDAORefactored`），写/删失败如实回 500，key 做非空与长度校验；弃用的配置文件已删除 | `SettingsApiControllerTest.setAffectsWhatTheApplicationReads`（断言 `DataService.loadSettings()` 真能看到新值，旧实现必红） |

### 报表/金额口径

| 项 | 问题 | 修法 | 门禁/测试 |
|---|---|---|---|
| **F3** | 从交易开票金额与顾客实付不符：净单价**硬编码除以 1.13**、明细取商品**原价**（折扣从不参与）、税率缺省写死 13%（不读系统设置）→ 106 元单按 6% 开票得 99.44，9.5 折单按 13% 开成 226.00 | 净单价按**本次税率**换算；含税单价按「实付/原价合计」比例折算（与退款同一口径）；缺省税率取系统设置的 `taxRate` | `InvoiceServiceTest` 2 项（按实付折算 = 214.70；净单价与税额同一税率） |
| **F4** | 利润报表**对桌面退货不减**：桌面退货从不写 `transactions.status=REFUNDED`（只有 API 退款会），而报表只跳过 `REFUNDED` → 卖 100 成本 60 后退掉，利润仍显示 40。TD-003 的"已修"只对 API 路径成立 | 在后台线程查区间内**已完成退货**及其明细，按退货完成日冲减收入与成本（退货回库，成本同样要冲回），分类过滤与销售侧同口径 | `ProfitReportControllerReturnDeductionTest` 3 项（整单退货归零/无退货不变/区间外不冲） |
| **F5** | 触屏收银台**按旧价格收款**：刚查到的行只用于库存校验，购物车行仍用网格里的旧对象 → 改价后仍按 10.00 卖（标准台对已在车里的行也有同样问题） | `CartItem.refreshProduct(fresh)`：加购/改数量时把行内商品对象换成刚读到的行并重算小计；标准台与触屏台都用它 | `CartItemTest` + `PosStalePricePolicyTest`（两个收银台都不得再用旧对象建行） |

### 设置项"只写不读"（F6，产品决定：全部实现）

审计实测：设置页有 8 个控件写进 `settings` 表后**没有任何读取方**。现已逐项接通：

- **口令锁定次数** `passwordMaxAttempts`：桌面 `LoginController` 与 API `LoginRateLimiter` 都改为读设置
  （`DataService.getIntSetting`，越界钳制、脏值回落）；
- **空闲自动登出** `autoLogout`/`autoLogoutMinutes`：新增 `util/IdleLogoutMonitor`（场景级活动监听 +
  守护定时器，未登录或关闭开关时不触发），由 `CashierSystemFXApplication` 在启动时挂上、关窗时停止；
- **自动备份/备份频率** `autoBackup`/`backupFrequency`：保存设置时落到 `backup_config` 并**重启调度器**
  （`BackupService.applyScheduleFromSettings`，中英文文案都认）；
- **门店地址/电话** `storeAddress`/`storePhone`：印在小票页头（触屏 `PrintUtil` 与标准 `ReceiptPrinter`
  的文本 + ESC/POS 两条路径共用 `PrintUtil.storeContactLines`，留空不留空标签行）；
- **打印条码** `printBarcode`：为 ESC/POS 网络路径补上唯一的"原始字节通道"（`PrintTask.rawPrintBytes`
  由 `NetworkPrinterDevice` 在正文后直写），开启时按交易号打 Code128 条码；文本/文件小票与 REST
  纯文本小票**不带**条码。`EscPosUtils.barcodeCode128` 此前**零调用方**，就是没有这条通道。

门禁/测试：`DataServiceTest` 2 项（整数/开关设置钳制、频率映射）、`IdleLogoutMonitorTest`、
`SalesReceiptPrintTest` 5 项（地址电话有/无、参数只填一项、模板不残留 `{{storeInfo}}`、条码字节契约）。

### 门禁本身的假绿灯（G1~G3）

- **G1 · i18n 硬编码门禁只认"第一个实参"**：`UI_CALL` 要求中文**紧跟左括号**，于是
  `showErrorAlert(标题, "中文正文")` 的第二实参与 `setTitle(常量 + "中文")` 的拼接写法全都漏检。
  实测白名单（"零硬编码"的 13 个文件）里仍有 3 处可见中文（主界面两处弹窗正文 + 触屏收银台窗口标题）。
  **已修**：规则改为"平衡括号取全部实参 + 跳过 i18n key 字面量"，3 处文案迁到 `runtime.dialog_open_failed`、
  `runtime.tpos_window_title`；顺带覆盖了 `updateSuccess/updateError/updateInfo` 三个状态栏出口。
- **G2 · 明文凭据门禁的引号盲区**：脚本规则要求"凭据键 = **带引号**字面量"，于是
  `DB_PASSWORD=Abc123xyz` 这类不带引号的赋值整类漏检；而 `release.sh`/`release.bat` 里那个
  **历史明文口令金丝雀**（`RootPassword123!`）也因此被判合规——等于把已泄露口令重新明文发布在 HEAD。
  **已修**：引号改为可选（并识别 cmd 的 `!VAR!` 引用，避免误报），release 脚本改为通用形态检查、
  不再内置任何口令字面量；新增 7 条规则单测（含"口令里的 `!` 仍要被抓到"）。
- **G3 · SpotBugs 注释与事实不符**：`pom.xml` 注释称"中低优先级告警保留在报告中逐步治理"，
  但 `threshold=High` 让报告里**一条都没有**，仓库/CI 里没有任何可治理的清单。
  **已修**：注释改为事实，并新增可选 profile `mvn -Pspotbugs-backlog -DskipTests verify`
  生成 Medium 清单（2026-10 实测 **267 条**，主要是 `PA_PUBLIC_PRIMITIVE_ATTRIBUTE`、
  `EI_EXPOSE_REP`、`DLS_DEAD_LOCAL_STORE`、`RV_RETURN_VALUE_IGNORED_BAD_PRACTICE` 等）。
  默认构建仍只卡 High（保持 CI 时长）。

### 静默降级与死代码（F11/F12）

- F11：购物车/扫码查库失败时**不得拿旧快照充数**，也不得把"查库失败"报成"未找到商品"
  （标准台与触屏台的加购路径改为显式提示并放弃；充值历史加载失败改为弹错误而不是静默空表）；
- F12：`startBackgroundServices()` 里那个"延迟 3 秒启动库存预警/备份/API"的守护 `Timer` 任务体
  **只打一行日志**（服务实际在登录后启动），属误导性死代码，已删除；
  `CashierSystemFXApplication.loadMainScene()` 与 `NotificationManager.addListener/removeListener`
  仍无生产调用方，登记为待清理。

### F14 库存预警页硬编码中文迁移（2026-10，已修）

`InventoryAlertController` 原有的用户可见中文已全部迁到 i18n，并把该文件加进
`HardcodedUiTextPolicyTest.MIGRATED_FILES`（现 14 个文件）：

| 位置 | 原文案 | 处理 |
|---|---|---|
| 级别展示名（枚举常量） | 严重警告/警告/提示 | 改为 `getDisplayName()` 内按当前语言解析（复用既有 `inventory_alert.critical/warning/info`） |
| 商品编号/单位兜底 | 无 / 个 | 复用 `common.none` + 新增 `common.unit_default` |
| 时长展示 | `小时/分钟/秒` | 新增 `inventory_alert.duration_hours/minutes/seconds`（带参） |
| 立即检查/清除冷却弹窗 | 检查完成、库存预警检查已完成！、清除成功、所有预警冷却时间已清除！ | 4 个新 key |
| 导出 | 6 个表头、文件名"库存预警报告"、空数据提示、成功/失败提示 | 表头复用 FXML 同款 key（`inventory_alert.product_name` 等）；其余 4 个新 key，空数据沿用 `runtime.no_export_*` 家族新增 `runtime.no_export_inventory_alerts` |

**同时暴露了门禁的两处真实盲区（已用定点锚点补上）**：门禁只检查**可见调用点实参里**的字面量，
所以"中文在别处拼好再传进来显示"截不到。变异实测：把 `formatDuration` 改回 `hours + "小时"`
**不会**让主规则变红。为此在 `HardcodedUiTextPolicyTest` 新增两条定点锚点：
`inventoryAlertLevelNamesAreLocalized`（枚举常量不得存中文）与
`inventoryAlertDurationTextsAreLocalized`（时长必须走带参 key，且不得拼接回去）。

### F14 附录：门禁看不见的同类残留——已修 5 处 / 剩余 5 处（2026-10）

按"中文在别处拼好/返回再显示"的形状扫描 `MIGRATED_FILES` 全部 14 个文件。
**重要订正（2026-10 复核）**：首版清单是按**行**筛的，把 3 处**多行调用成的
`logger`/审计日志续行**误判成了界面文案，已剔除并在此记录，避免下次照单返工：

| 首版误报 | 实际情况 |
|---|---|
| `CashierSystemFXApplication:451-453` | 是 `logger.error("内置界面字体 {} 未注册成功…")` 的**续行**，属日志；字体缺失**弹窗**早就走 `runtime.ui_font_missing_detail` |
| `SettingsController:805-807` | 是 `AuditService.success(..., "支付模式=…")` 的**审计日志明细**，不是界面文案 |
| `PurchaseInboundController:403/410`、`PurchaseApprovalController:279/369` | 同为审计/异常日志文本 |

#### 已修（第二批，含本批）

| 位置 | 处理 |
|---|---|
| `RechargeController` 5 条充值校验文案 | 新增 `recharge.validation.*`（上限用常量 `MAX_RECHARGE_AMOUNT` 传入，避免改了阈值忘改文案） |
| `MainController` 关于弹窗正文 | 新增 `runtime.about_dialog_content`（7 个占位符：名称/版本/开发者/JavaFX/Maven/JDK/许可证） |
| `CashierSystemFXApplication` 闪屏进度 ×5 + 数据库超时异常 | 新增 `runtime.splash_connecting_db(_waiting)`、`runtime.splash_loading_ui`、`runtime.startup_db_timeout`；"正在启动服务..."/"即将完成..." 复用既有 `runtime.splash_starting_services`/`splash_finishing` |
| `ShiftController` 导出 | 12 个表头 + 报表名 + 导出目录 + 未开始/未结束/未完成/无 → `shift.export.*` + 复用 `shift.not_started/not_ended`、`runtime.incomplete`、`common.none`、`shift.operator` |
| `TransactionController` 导出 | 8 个表头 + 报表名 + 目录 + 无商品/非会员 → `transaction.export.*` + 复用 `recharge.payment_method`、`transaction.no_items`、`runtime.non_member` |

配套门禁：`updateProgress`/`export` 加入 `HardcodedUiTextPolicyTest` 的可见出口名单；
新增锚点 `variableBuiltVisibleTextIsLocalized` 钉住"拼进变量/列表"的 5 处形状不得回退。

#### 剩余（下一批）

| 文件 | 残留（行号，2026-10-08 复核） | 备注 |
|---|---|---|
| `SettingsController` | 1214/1217 税率校验文案（`errorMessage +=`）；1402-1410 测试打印正文（设备名称:/IP地址:/端口:…）；873/1533/1536 文件选择器过滤标签（`new ExtensionFilter("CSV 文件", …)`） | 与充值校验同形状；测试打印是**打印件**，同样要译 |
| `PurchaseOrderController` | 489 `String.format("%s - %s (%s级)")`（供应商展示串） | 下拉/表格可见 |
| `PurchaseOrderController` **995** | `((Label) node).getText().startsWith("总金额:")` | **这是功能缺陷不是文案问题**：该标签的文字已由 i18n 设置（`runtime.total_amount_value`），英文环境下前缀不再等于"总金额:"，于是**总金额标签永远不会刷新**。修法：用同一个 key 拼前缀比较，或给标签一个 `fx:id` 后直接引用 |
| `TransactionController`/`PurchaseOrderController`/`SettingsController` 的**筛选下拉值**（全部/今天/本月/待审批/简体中文/每天/58mm (热敏纸)…） | 既是显示文本又是落库/匹配值 | 要翻译必须走 TD-002 的模式（落库存代码 + `StringConverter` 只翻译显示层），是一次独立重构，不在"文案迁移"范围内 |

**根治方案（仍未做）**：把门禁升级为"除白名单数据值外，迁移文件里不得出现中文字面量"。
白名单约 40 条（落库规范值 现金/微信/简体中文/每天/全部…、品牌名 `APP_TITLE`、
历史标签兼容表 `actions.put("商品管理", …)`、FXML 设计期占位等），每条要写明理由；
该方案能覆盖上述**全部**形状（返回值、拼接、`Arrays.asList`、日志除外），但必须先把白名单核准确，
否则会误报大量落库值。**当前仍靠逐文件定点锚点兜底。**

### 本轮**未修**（状态与理由）

- **F9 微信异步退款永不落终态**：`WechatNativePaymentProvider` 对非 SUCCESS 状态写 `PROCESSING`，
  而全仓库没有任何消费方 → 支付单停在 `PARTIAL_REFUND`、预留额度不释放（保守，不会多退钱）。
  需要设计"退款结果轮询/回调"（查询接口或对账任务），属功能开发，未在本次修。
- **F10 退货创建是 check-then-act**：校验在事务外，`return_orders` 无唯一约束/已退数量台账，
  两个终端同时提交可各退满额。窗口窄，串行操作会被拦；彻底修需要"已退数量台账 + 唯一约束"的设计。
- **F13 `ProductDAORefactored.batchUpdateWithConnection` 丢弃 `executeBatch()` 结果**并无条件 `version++`：
  当前唯一调用方 `DataService.saveInventory` 无生产调用方，属埋雷，未动。
- **F14 `InventoryAlertController` 的硬编码中文**：**已修**（弹窗/导出/时长/级别名全部迁 i18n，
  该文件已进 `MIGRATED_FILES`；其余迁移文件的同类残留见上文"F14 附录"，属下一批）。
- **F15 依赖版本**：Javalin 6.1.3 传递来 **Jetty 11.0.20**（早于修 CVE-2024-8184/6763 的 11.0.24）；
  **logback 1.5.18** 命中 CVE-2025-11226（本仓库无 Janino + 无 Spring → 不可达）。
  升级需要联网解析依赖并复跑全量回归，未在本次动。
- **F6 的"只写不读"已全部实现**，但 `theme` 这个 key 仍只写不读（主题偏好走 `theme_preferences` 表，
  功能本身正常），属历史冗余键。
