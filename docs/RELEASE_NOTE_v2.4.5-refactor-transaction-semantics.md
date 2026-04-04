# v2.4.5-refactor-transaction-semantics

发布日期：2026-04-04
关联提交：`bdd2514`
建议标签：`v2.4.5-refactor-transaction-semantics`

## 本次发布定位

这是一个面向事务一致性和服务层边界收敛的维护型发布，不引入新的业务功能，也不变更数据库版本；重点是把原先分散在 DAO、Service、Controller 中的事务控制逻辑进一步统一，降低后续继续重构时的认知成本和回归风险。

## 关键改动

### 1. 统一事务语义

- `DatabaseManager.commitTransaction()` 与 `rollbackTransaction()` 在提交/回滚后都统一恢复 `autoCommit=true`
- `BaseDAO.beginTransaction()` / `commitTransaction()` / `rollbackTransaction()` 统一委托 `DatabaseManager`
- `BaseDAO.executeInTransaction()` 保留可覆写 `getConnection()` 的能力，兼顾测试替身与统一事务语义

### 2. 收敛服务层重复事务模板

以下服务已删除各自重复的本地事务模板，统一改为直接使用 `DatabaseManager.executeBooleanTransaction(...)`：

- `ReturnService`
- `InventoryService`
- `MemberService`

这使得“服务层定义事务边界、DAO 只做连接内操作”的方向更明确，也减少了多处复制粘贴式事务代码带来的漂移风险。

### 3. 交易主流程改为统一事务入口

`TransactionService.executeTransaction(...)` 已从手工 `Connection + begin/commit/rollback/finally close` 风格收敛为统一事务模板，并完成以下调整：

- 商品库存扣减继续在同一事务内完成
- 会员余额、积分、等级、折扣更新统一并入同一事务
- 交易记录写入与促销使用次数更新继续保持原子性
- 移除提交后再调用 `MemberService.updateMemberLevel()` 的“事务外补丁式升级”路径

### 4. DAO 连接内操作接口继续补齐

- `TransactionDAO.insert(...)` 已改为复用统一事务入口
- 现有 `WithConnection` 风格接口继续作为服务层事务内编排的主要承载形式
- 这一轮改动进一步强化了“连接在服务层统一传递、DAO 负责连接内读写”的边界

## 回归验证

全量 `mvn test` 已于 2026-04-04 再跑通过，共 122/122：

- `PasswordUtilTest` 10/10
- `BatchOperationUtilTest` 3/3
- `UserDAOTest` 12/12
- `BaseDAOTest` 3/3
- `ProductDAOTest` 12/12
- `ProductDAORefactoredTest` 5/5
- `TransactionConcurrencyTest` 11/11
- `PromotionServiceTest` 14/14
- `InventoryServiceTest` 5/5
- `TransactionServiceTest` 11/11
- `MemberServiceTest` 21/21
- `ReturnServiceTest` 15/15

其中，事务重构直接相关的定向验证为 55/55：

- `BaseDAOTest` 3/3
- `TransactionServiceTest` 11/11
- `MemberServiceTest` 21/21
- `InventoryServiceTest` 5/5
- `ReturnServiceTest` 15/15

新增回归覆盖包括：

- 会员交易在同一事务内更新等级与折扣
- 促销次数更新失败时交易与库存整体回滚
- 连续充值生成唯一充值记录 ID
- 多商品库存失败时整体回滚

## 影响范围

### 直接影响模块

- `src/main/java/com/cashier/util/DatabaseManager.java`
- `src/main/java/com/cashier/dao/BaseDAO.java`
- `src/main/java/com/cashier/dao/TransactionDAO.java`
- `src/main/java/com/cashier/service/ReturnService.java`
- `src/main/java/com/cashier/service/InventoryService.java`
- `src/main/java/com/cashier/service/MemberService.java`
- `src/main/java/com/cashier/service/TransactionService.java`
- 相关控制器、DAO、Model 与测试文件

### 不包含在本次发布中的内容

- 不变更对外功能菜单与业务流程入口
- 不新增数据库迁移脚本
- 不调整应用主版本号，仍为 `v2.4.5`

## 建议使用场景

建议将该标签作为 `v2.4.5` 之下的细粒度维护检查点，用于：

- 标记 BigDecimal 迁移后事务收敛阶段的稳定里程碑
- 给后续 Repository/实例化 Service 重构提供清晰基线
- 在需要回溯“事务语义统一前后”行为差异时快速定位版本

## 后续建议

1. 继续把静态 DAO/Service 向接口化、实例化方向推进
2. 逐步减少 Controller 对 Model 公有字段的直接拼装
3. 以后每轮事务或金额相关重构前后，都补跑一次全量 `mvn test`，沿用当前 122/122 基线做对比
