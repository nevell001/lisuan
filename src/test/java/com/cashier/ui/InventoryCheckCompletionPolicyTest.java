package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 盘点完成原子性门禁。
 *
 * <p>回归：此前是"先 UPDATE 状态为 completed，再逐条用各自 autocommit 连接调整库存"，
 * 中途失败/崩溃会留下「盘点单已完成、库存只改了一半」且不可重做的状态
 * （canComplete 要求 checking），差额永久丢失；重复点击还会把差额二次累加。</p>
 */
@DisplayName("盘点完成原子性门禁")
class InventoryCheckCompletionPolicyTest {

    @Test
    @DisplayName("状态迁移与库存调整必须同一事务，且状态迁移带 completed 守卫")
    void completionIsAtomicAndGuarded() throws Exception {
        String controller = Files.readString(
            Path.of("src/main/java/com/cashier/controller/InventoryCheckController.java"));
        String dao = Files.readString(
            Path.of("src/main/java/com/cashier/dao/InventoryCheckDAORefactored.java"));

        assertTrue(controller.contains("DatabaseManager.executeBooleanTransaction"),
            "完成盘点必须走一个事务，否则中途失败会留下「已完成但库存只改了一半」的不可重做状态");
        assertTrue(controller.contains("completeWithConnection(conn, selected.id, currentUser)"),
            "状态迁移必须在事务连接上执行");
        assertTrue(controller.contains("updateQuantityWithConnection("),
            "库存调整必须复用同一个事务连接");
        assertFalse(controller.contains("productDAO.updateQuantity(item.productId, item.diffQuantity)"),
            "逐条 autocommit 改库存的旧写法不得回归");

        int methodStart = dao.indexOf("completeWithConnection(Connection conn");
        assertTrue(methodStart > 0, "找不到 completeWithConnection");
        String methodBody = dao.substring(methodStart, Math.min(dao.length(), methodStart + 900));
        assertTrue(methodBody.contains("status <> 'completed'"),
            "状态迁移必须带 status <> 'completed' 守卫，否则重复点击会把差额二次累加到库存");
    }
}
