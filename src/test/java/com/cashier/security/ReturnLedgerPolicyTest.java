package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退货占用台账门禁（F10-a）。
 *
 * <p>背景：退货建单的"累计退货量 ≤ 原销量"校验此前只在 UI 线程、事务外做
 * （`CreateReturnOrderDialogController.validateReturnItems`），两个终端并发提交时都能通过，
 * 于是同一交易被退两次（库存恢复两次 + 退款两次）。修法是把校验搬进建单事务：
 * 先锁原交易行串行化，再用占用台账重算可退余量。</p>
 *
 * <p>行为侧由 {@code ReturnReservationConcurrencyTest}（7 项，含真实双线程并发）守住；
 * 这里钉住源码形状，防止有人把校验挪回事务外、或把台账查询改成包含已驳回单据。</p>
 */
@DisplayName("退货占用台账门禁")
class ReturnLedgerPolicyTest {

    @Test
    @DisplayName("行级可退量校验（F10-c）：必须按原交易明细行校验，且与商品级同在锁内事务里")
    void lineLevelValidationIsEnforcedInsideTransaction() throws Exception {
        String service = read("com/cashier/service/ReturnService.java");
        assertTrue(service.contains("validateRequestedLines("),
            "同一商品多行时必须按行校验可退量，否则可以把超出该行的数量算到更贵的一行多退钱");
        int lock = service.indexOf("lockAndValidateReturnable(conn,");
        int line = service.indexOf("validateRequestedLines(conn,");
        assertTrue(lock > 0 && line > lock,
            "行级校验必须与行锁/商品级校验在同一事务内（事务外校验挡不住并发）");
        assertTrue(service.contains("sumReturnedQuantitiesByTransactionItemWithConnection"),
            "行级已退量必须取明细表的权威记录");

        String dao = read("com/cashier/dao/ReturnOrderItemDAORefactored.java");
        assertTrue(dao.contains("AND ro.status <> 'REJECTED'"),
            "行级口径必须与台账一致：已驳回的退货单不占额度");
        assertTrue(dao.contains("transaction_item_id IS NOT NULL"),
            "没有行 id 的老数据不参与行级校验（商品级校验仍然兜着）");

        // 三条写路径都要带上行 id，否则行级校验形同虚设
        assertTrue(read("com/cashier/dao/TransactionDAORefactored.java").contains("ti.id AS item_id"),
            "加载原单明细必须带出行 id");
        assertTrue(read("com/cashier/controller/CreateReturnOrderDialogController.java")
                .contains("item.transactionItemId"), "桌面建单明细必须带上行 id");
        assertTrue(read("com/cashier/api/controller/TransactionApiController.java")
                .contains("item.transactionItemId"), "API 整单退款的明细也要带上行 id");
    }

    private static String read(String relativeToMain) throws Exception {
        return Files.readString(Path.of("src/main/java/" + relativeToMain));
    }

    @Test
    @DisplayName("可退量校验必须在建单事务内：先锁原交易行，再按台账算余量")
    void returnCreationValidatesInsideTransaction() throws Exception {
        String service = read("com/cashier/service/ReturnService.java");

        assertTrue(service.contains("lockForReturnWithConnection("),
            "建单必须先锁原交易行（SELECT ... FOR UPDATE），否则并发建单各自都能通过校验");
        assertTrue(service.contains("sumReservedQuantitiesWithConnection("),
            "可退余量必须来自占用台账");
        assertTrue(service.contains("sumItemQuantitiesByProductWithConnection("),
            "原单数量必须按商品跨行合计（同一商品多行时按行比较会误判）");
        assertTrue(service.contains("getReturnReservationDAO()"),
            "建单成功必须同时写台账（占用与单据同事务）");

        int validateCall = service.indexOf("lockAndValidateReturnable(conn,");
        int insertOrder = service.indexOf("insertWithConnection(conn, returnOrder)");
        assertTrue(validateCall > 0, "校验必须发生在事务连接上（conn 来自 executeBooleanTransaction 的 lambda）");
        assertTrue(insertOrder > validateCall,
            "行锁与校验必须在插入退货单之前（顺序反了等于没锁）");
    }

    @Test
    @DisplayName("占用统计必须排除已驳回的退货单（驳回即释放额度）")
    void reservationOccupancyExcludesRejectedOrders() throws Exception {
        String dao = read("com/cashier/dao/ReturnReservationDAORefactored.java");
        assertTrue(dao.contains("ro.status <> 'REJECTED'"),
            "台账占用查询必须 join return_orders 并排除 REJECTED，否则被驳回的单会永久占住额度");
    }

    @Test
    @DisplayName("老库退货单必须被幂等回填进台账（否则历史交易又能再退一次）")
    void legacyReturnOrdersAreBackfilled() throws Exception {
        String databaseManager = read("com/cashier/util/DatabaseManager.java");
        assertTrue(databaseManager.contains("backfillReturnReservations();"),
            "启动时必须回填老退货单，否则台账里没有它们的占用");
        assertTrue(databaseManager.contains("NOT EXISTS (SELECT 1 FROM return_reservations"),
            "回填必须幂等（按 (退货单号, 商品) 去重），重复执行不得重复插入");
    }

    @Test
    @DisplayName("三条写路径都要写台账：桌面建单、审批/完成同步、API 整单退款（F10-b）")
    void everyWritePathKeepsLedgerConsistent() throws Exception {
        String service = read("com/cashier/service/ReturnService.java");
        assertTrue(service.contains("syncReservationStatus(conn, returnOrderId"),
            "审批/完成必须同步台账行状态（与单据状态同事务）");
        assertTrue(service.contains("ReturnReservation.STATUS_APPROVED")
                && service.contains("ReturnReservation.STATUS_REJECTED")
                && service.contains("ReturnReservation.STATUS_COMPLETED"),
            "PENDING/APPROVED/COMPLETED/REJECTED 四个状态都要落到台账");

        String api = read("com/cashier/api/controller/TransactionApiController.java");
        assertTrue(api.contains("getReturnReservationDAO()"),
            "API 整单退款（直接 COMPLETED）也必须写台账，否则桌面退货查不到这笔占用、会把同一交易再退一次");
    }

    @Test
    @DisplayName("可退量规则只在服务层实现一次：界面不得再照抄一份校验")
    void ruleIsImplementedOnlyInService() throws Exception {
        String service = read("com/cashier/service/ReturnService.java");
        assertTrue(service.contains("ReturnQuantityExceededException"),
            "超量必须由服务层抛带文案的异常，界面才能拿到明确原因（并发输掉的那次也一样）");
        assertTrue(service.contains("runtime.return_quantity_exceeded"),
            "文案在服务层用 i18n 组装，避免各界面各拼一套");

        String dialog = read("com/cashier/controller/CreateReturnOrderDialogController.java");
        assertTrue(!dialog.contains("findByOriginalTransactionId"),
            "建单界面不得再自己汇总已退数量（那是事务外的 check-then-act，也是规则的第二个实现）");
        assertTrue(dialog.contains("ReturnService.ReturnQuantityExceededException"),
            "界面应捕获服务层的超量异常并展示其文案");
    }
}
