# F9 / F10 设计方案（2026-10，待评审）

> 对象：[TECH_DEBT.md](TECH_DEBT.md) 「2026-10 第二轮全量审计（F1~F15 / G1~G3）」里**本轮未修**的两条：
> **F9** 微信异步退款永不落终态、**F10** 退货创建是 check-then-act。
> 本文只做设计（现状事实 → 目标 → 方案对比 → 推荐设计 → 测试 / 工作量 / 风险 → 待确认），**不含实现**。

## 0. 摘要与建议顺序

| 项 | 性质 | 后果 | 建议 |
|---|---|---|---|
| **F10** | 正确性 / 资损 | 同一交易可被退两次：**库存恢复两次 + 退款两次**（不可逆） | **先做**：台账表 + 交易行锁 + 事务内校验；1 张表 + 4 处写路径 + 2 项并发测试，约 1.5~2 人日 |
| **F9** | 正确性 / 资金占用 | 微信退款停在 `PROCESSING`：**该额度永久不能再退**，订单还被错标"部分退款" | 次做：`queryRefund` + 对账调度 + 状态机收敛；约 2~2.5 人日 |

两者互不依赖，可并行。都不需要前端改版（F10 需要把一条既有错误文案的触发点挪进服务层）。

---

## 1. F10：退货创建 / 审批的 check-then-act

### 1.1 现状（代码事实）

- **建单校验在 UI 线程、事务外**：`CreateReturnOrderDialogController.validateReturnItems()`（:369-407）
  读 `return_orders WHERE original_transaction_id = ? AND status != 'REJECTED'`
  （`ReturnOrderDAORefactored.findByOriginalTransactionId`），逐单读明细，按 `product_id` 汇总已退数量，
  与 `ReturnItem.originalQuantity`（= 该**行**的 `transaction_items.quantity`，见 :325）比较。
- **落库没有容量校验、也没有唯一约束**：`ReturnService.createReturnOrder()`（:129-177）只做插入事务；
  `return_orders` 的约束只有 `return_order_id UNIQUE`（`DatabaseManager:924`）。
- **审批返还库存**：`approveReturnOrder()`（:198-270）先用 `markApprovalWithConnection`
  （`UPDATE ... WHERE status='PENDING'`）原子迁移**同一张单**，随后 `product.quantity += item.returnQuantity`；
  **不同退货单之间没有互斥**。完成（`completeReturnOrder` :389-434）再退款（现金/余额 + 积分冲减）。
- **数据模型**：`return_order_items` 只有 `product_id`，**没有 `transaction_item_id`**（:954-971）——
  所以"已退数量"目前只能按 **(原交易, product_id)** 聚合。
- **API 退款这条路已经是原子的**：`TransactionApiController.processRefundTransaction`（:313-334）
  用 `claimRefundWithConnection`（`UPDATE transactions SET status='REFUNDED' WHERE status<>'REFUNDED'`）
  抢占，外加已有退货单预检（:304-309）。**缺口只在桌面退货建单/审批**。

### 1.2 竞态与后果

两个终端（或两个窗口）同时对同一交易的同一商品建满额单：
双方校验都通过 → 两张单入库 → 都审批 → **库存恢复两次** → 都完成 → **退款两次**（现金/余额、积分各按各自单子算）。
`approveReturnOrder` 的乐观锁只保护**商品行**的并发写，不保护"累计退货量 ≤ 原销量"这条业务不变式。
窗口窄（要并发提交），但后果不可逆。

### 1.3 目标 / 非目标

- 目标：把不变式落到**数据库 + 单条事务**里——并发建单只有一个成功，第二个拿到可理解的失败；
  桌面与 API 两条路共用同一套"已退量"口径。
- 非目标：不改退款金额/积分口径（TD-034 已定稿）；不做自动拆单；不改审批流程本身。

### 1.4 方案对比

| 方案 | 做法 | 评价 |
|---|---|---|
| A. 唯一约束 | `UNIQUE(original_transaction_id, product_id)` | ❌ 只允许退一次，禁掉合法的多次/部分退货 |
| B. **交易行锁 + 事务内校验** | 建单事务里 `SELECT ... FROM transactions WHERE transaction_id=? FOR UPDATE`，重算可退量后插入 | ✅ 改动最小；串行化整单退货，天然覆盖"多商品、多次部分退" |
| C. **已退数量台账** | `return_reservations(return_order_id, original_transaction_id, product_id, quantity, status)`；PENDING/APPROVED/COMPLETED 占用、REJECTED 释放 | ✅ 把"已退量"从"扫明细求和"变成可索引、可审计的一等数据；是桌面/API 统一口径的落点 |
| D. 给 `transaction_items` 加 `returned_quantity` | 原子 `UPDATE ... WHERE quantity - returned_quantity >= ?` | ⚠️ 抢占式扣减最简单，但 PENDING 被拒后要回滚占用；且与现状"按 product 聚合"粒度不一致 |

**推荐 B + C**：行锁保证同一交易串行；台账让"可退余量"变成一条 SQL（原单数量 − 非 REJECTED 的占用合计）。

### 1.5 详细设计（推荐）

1. **建表**（`DatabaseManager.createTable*` + `docker/mysql-init/00-init-complete.sql`；`InitSchemaParityTest` 会比对两处）：

   ```sql
   CREATE TABLE IF NOT EXISTS return_reservations (
     id INT AUTO_INCREMENT PRIMARY KEY,
     return_order_id VARCHAR(50) NOT NULL,
     original_transaction_id VARCHAR(50) NOT NULL,
     product_id INT NOT NULL,
     quantity INT NOT NULL,
     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING/APPROVED/COMPLETED/REJECTED
     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     UNIQUE KEY uk_return_product (return_order_id, product_id),
     INDEX idx_tx_product (original_transaction_id, product_id),
     FOREIGN KEY (return_order_id) REFERENCES return_orders(return_order_id) ON DELETE CASCADE
   );
   ```

2. **建单**（`ReturnService.createReturnOrder`，同一事务内）：
   ① `SELECT ... FROM transactions WHERE transaction_id = ? FOR UPDATE`（缺行即失败）；
   ② 按 `product_id` 汇总**跨行**原单数量（顺带修掉"同一商品多行时按行比较"的口径隐患）；
   ③ 汇总台账占用（`status <> 'REJECTED'` 的 `quantity`）；
   ④ 逐商品校验 `已占用 + 本次 ≤ 原数量`，越界即失败并回传明确原因；
   ⑤ 插 `return_orders` + `return_order_items` + 台账。
   UI 的 `validateReturnItems` 保留为**即时反馈**，但不再是防线。

3. **审批**：`markApprovalWithConnection` 成功后，同事务把台账 `PENDING → APPROVED`；驳回则 `→ REJECTED`（释放额度）。库存返还维持现状（同一事务）。

4. **完成**：`markCompletedWithConnection` 成功后同事务把台账 `→ COMPLETED`（若只关心占用，PENDING/APPROVED 都已占，可视为可选）。

5. **历史数据 backfill**：一次性脚本从 `return_orders` + `return_order_items` 回填台账
   （`status='REJECTED'` 不写；`COMPLETED` 置 `COMPLETED`，其余按 `APPROVED`/`PENDING` 落）。

6. **API 路径统一**：`TransactionApiController` 的整单退款也写一条台账（数量=整单数量）；
   它的 `claimRefundWithConnection` 保留（先抢 `transactions.status`），两条防线不冲突。

7. **错误契约**：桌面弹既有 key `runtime.return_quantity_exceeded`（现在由 UI 拼串，改由服务层给出），
   API 若将来开放退货则回 **409 + 明确文案**（不沿用 500）。

### 1.6 测试与门禁

- 行为（H2）：新增 `ReturnReservationConcurrencyTest`
  - 两线程同时对同一交易满额建单 → **只有一个成功**，另一个得到"超出可退数量"；
  - 部分退两次各半都成功，第三次失败；
  - REJECTED 释放额度后可重新建满额单；
  - 同一商品在交易里出现**两行**时按跨行合计判断（回归现在按行比较的隐患）。
- 源码门禁：`ReturnLedgerPolicyTest` —— 断言建单事务内出现 `FOR UPDATE`；断言台账写入与建单同事务；
  断言 `CreateReturnOrderDialogController` 不再是唯一防线。
- 迁移一致性：`InitSchemaParityTest`（新表必须同时出现在 Java 建表与 `00-init-complete.sql`）。
- 现有 `ReturnOrderStateMachineTest`（5 项）与 `ReturnServiceTest` 必须保持绿。

### 1.7 影响面 / 工作量 / 风险

- 文件：`DatabaseManager`、`00-init-complete.sql`、`ReturnOrderDAORefactored`（+ 新 `ReturnReservationDAORefactored`）、
  `ReturnService`、`TransactionApiController`、`CreateReturnOrderDialogController`、2 个测试类 + 文档。
- 约 **1.5~2 人日**（含回填脚本与并发测试）。
- 风险：**低**——改动集中在写路径，报表/列表读路径不受影响；回滚=删表 + 回滚服务层（台账不参与金额口径）。
- 待确认：① `PENDING` 长期不审批是否要过期释放占用（建议先不释放，人工处理）；
  ② 是否把 `return_order_items` 升级为**行级**（加 `transaction_item_id`）——更精确但要迁移，建议独立一条 TD。

---

## 2. F9：微信异步退款永不落终态

### 2.1 现状（代码事实）

- 渠道接口**没有查退款**：`PaymentChannelProvider`（:9-23）只有 `refund(...)`（:22），没有 `queryRefund`。
- 微信 `refund()`（`WechatNativePaymentProvider:166-192`）：渠道返回非 `SUCCESS` 一律写 `PROCESSING`；
  **请求体里没有 `notify_url`**，所以微信不会推送退款结果。
- `PaymentService.applyRefund`（:266-322）：事务内预占（`APPLYING`）→ 事务外调渠道 → `settleRefund`（:327-342）
  写退款状态，并按 `sumSettledRefundAmount`（只算 `SUCCESS`，`PaymentDAORefactored:425`）决定订单状态。
- **只有微信会留 `PROCESSING`**：支付宝（`AlipayPrecreatePaymentProvider.refund`）与 Mock 的退款都是同步终态（直接 `SUCCESS`）。
- 模型层早已预留：`RefundRecord.RefundStatus` 有 `PROCESSING` 与 `isFinal()`（:106-108），但**全仓库没有任何消费方**。
- 唯一调用方是 `POST /api/payment/{paymentId}/refund`（`PaymentApiController.applyRefund:264-296`），
  响应体固定 `"message": "退款成功"`。

### 2.2 问题（三层）

1. **额度永久占用**：`sumRefundedAmount`（:398）把 `APPLYING`/`PROCESSING` 都算作已退（预占，防并发超退），
   但**没有任何路径把 `PROCESSING` 推进终态** → 这笔金额永久不能再退、也无法释放。
2. **状态错标**：`settleRefund` 在 `settledAmount(0) < originalAmount` 时把订单标成 **`PARTIAL_REFUND`**——
   一分钱没退成功却显示"部分退款"。
3. **契约误导**：API 返回 `status:"处理中"` 却配 `message:"退款成功"`。

### 2.3 目标 / 非目标

- 目标：`PROCESSING` 能在有限时间内收敛到 `SUCCESS/FAILED/CLOSED`；订单状态只在**真有成功退款**时变化。
- 非目标：不改微信签名/证书逻辑；不做对账文件（bill）下载——那是另一个量级。

### 2.4 方案对比

| 方案 | 说明 | 评价 |
|---|---|---|
| A. 退款结果回调 | 退款请求带 `notify_url`，新增 `POST /api/payment/notify/refund` 处理 `REFUND.SUCCESS/ABNORMAL/CLOSED` | ✅ 实时；❌ 依赖回调可达（未配 notifyUrl 就完全没有兜底），只能与对账并行，不能替代 |
| B. **主动轮询对账（推荐）** | 定时任务捞非终态退款 → 调渠道查退款 → 收敛 | ✅ 唯一不依赖外部可达性的兜底；可定重试/放弃边界 |
| C. 人工核对入口 | 列表 + 手工"查询/标记" | 作为 B 的运维可视化补充，不能单独用 |

**推荐：B 为主 + A 可选（配了 notifyUrl 才启用）+ C 作兜底可见性。**

### 2.5 详细设计（推荐）

1. **接口扩展**：`PaymentChannelProvider` 加
   `RefundStatus queryRefund(PaymentOrder order, RefundRecord refund)`。
   微信实现 `GET /v3/refund/domestic/refunds/{out_refund_no}`（复用现有 `send/authorization/parseSuccess` 与显式超时），
   映射：`SUCCESS→SUCCESS`、`CLOSED→CLOSED`、`ABNORMAL→FAILED`（或保持 `PROCESSING` + 告警，见待确认）、其余 `PROCESSING`。
   支付宝/Mock 直接返回 `SUCCESS`（已是终态）。
2. **DAO 新增**：
   - `findUnsettledRefunds(channel, olderThanSeconds, limit)`：`refund_records` join `payment_orders`
     （对账需要 `merchant_order_no`/`amount`）；
   - `updateRefundStatusIfNotFinalWithConnection(conn, refundId, fromStatus, toStatus, channelRefundNo)`：
     `... WHERE refund_id=? AND status=?` —— **杜绝状态回退与重复收敛**（现有 `updateRefundStatusWithConnection` 是裸 UPDATE）。
3. **调度服务** `PaymentRefundReconcileService`（单例，`volatile boolean isRunning` 守卫，
   照抄 `InventoryAlertService:24/54-64`——**这正是 TD-033 指出 `BackupService` 缺的守卫**）：
   - 间隔默认 60s，可配置开关；
   - 每次取 N（如 50）条，逐条 `queryRefund`；终态则在同一事务里走现有 `settleRefund`（状态 + 订单状态一起生效）；
   - **退避与放弃**：按 `create_time` 做退避；超过 `maxTrackHours`（默认 24h）仍未终态 → 保持 `PROCESSING`、
     写 WARN + 审计，进入"需人工核对"，不再无脑重试；
   - `FAILED/CLOSED` → 预占自动释放（`sumRefundedAmount` 已排除这两个状态）并允许再次退款；
   - 收敛后发 `SyncManager` 事件（与支付成功一致）并写审计。
4. **订单终态判定修正**（`settleRefund`）：
   `settled == 0` → **不改**订单状态（保持 `SUCCESS`）；`0 < settled < original` → `PARTIAL_REFUND`；
   `settled >= original` → `REFUNDED`。（现在的 `settled==0 → PARTIAL_REFUND` 是 bug。）
5. **API 契约**：`POST /api/payment/{paymentId}/refund` 的 `message` 按状态给
   （`SUCCESS→"退款成功"`、`PROCESSING→"退款已受理，处理中"`）；
   新增 `GET /api/payment/refunds?status=PROCESSING`（finance/admin）供对账。
6. **配置**：`PaymentConfig` 加 `refundReconcileEnabled`（默认 true）、`refundReconcileSeconds`（60）、
   `refundMaxTrackHours`（24）；是否在设置页暴露，本次可只给默认值。

### 2.6 数据模型

- 现有 `refund_records`（建表见 `PaymentDAORefactored:61`）够用：`status` + `channel_refund_no` + `refund_time`。
- 可选加 `query_attempts INT DEFAULT 0`、`last_query_time TIMESTAMP NULL`（免去用 `create_time` 做退避）；
  不加也能跑。
- **无需回填**：历史 `PROCESSING` 行会被调度任务自动捞起。

### 2.7 测试与门禁

- 行为（H2 + 可控 FakeProvider）：新增 `PaymentRefundReconcileTest`
  - `PROCESSING → SUCCESS`：订单变 `REFUNDED`、`refund_time` 落库、额度不再被预占；
  - `PROCESSING → FAILED/CLOSED`：额度释放，可再次退款；
  - 重复对账 / 迟到回调 → **状态不倒退**（`WHERE status=?` 生效）；
  - `settled == 0` 时订单**不得**被标 `PARTIAL_REFUND`（当前 bug 的回归锚点）；
  - 超过 `maxTrackHours` → 保持 `PROCESSING` 且只告警一次（不无限重试）。
- 源码门禁：`RefundTerminalStatePolicyTest` —— 断言存在 `queryRefund` 并被调度服务调用；
  断言退款状态更新带状态条件（裸 UPDATE 即失败）；断言调度服务有 `isRunning` 守卫。
- 复用现有：`PaymentServiceRefundTest`（并发上限，2 项）保持绿。

### 2.8 影响面 / 工作量 / 风险

- 文件：`PaymentChannelProvider` + 4 个实现、`PaymentService.settleRefund`、`PaymentDAORefactored`（2 个查询）、
  新 `PaymentRefundReconcileService`、`CashierSystemFXApplication`（登录时 start）、`PaymentApiController`、2 个测试类。
- 约 **2~2.5 人日**；若同时做回调（A）再加 0.5~1 人日。
- 风险：**中**。调度任务会周期性发外部 HTTP——必须复用 TD-031 已定的显式超时、daemon 线程 +
  `isRunning` 守卫（TD-033 教训）；对账失败不得影响收银主流程。
- 待确认：① 微信 `ABNORMAL` 是继续查还是直接判失败；② 是否要退款回调（生产需配 `notify_url`）；
  ③ 人工核对入口放 API 还是桌面页。

---

## 3. 落地顺序与拆批

1. **F10-a**：台账表 + 建单事务内校验（含 backfill、并发测试）——解决资损。
2. **F10-b**：审批/完成/API 路径接台账 + 错误文案统一。
3. **F9-a**：`queryRefund` + 状态机收敛（`settled==0` 修正）+ 幂等更新。
4. **F9-b**：对账调度服务 + 配置 + 运维查询接口。
5. 可选：**F9-c** 退款回调、**F10-c** 行级明细（`transaction_item_id`）。

## 4. 需要产品 / 运维确认的问题

1. 退货单 `PENDING` 长期不审批是否自动过期释放占用？（建议：不释放，人工处理）
2. 微信退款 `ABNORMAL` 后自动重试还是转人工？
3. 退款对账间隔与最长跟踪时长（建议 60s / 24h）——是否需要可配？
4. 是否对外暴露"退款处理中"的查询接口（财务对账用）？
