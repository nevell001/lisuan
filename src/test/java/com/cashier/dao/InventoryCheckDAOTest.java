package com.cashier.dao;

import com.cashier.model.InventoryCheck;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("库存盘点数据访问对象测试")
class InventoryCheckDAOTest extends DatabaseTestBase {

    private final InventoryCheckDAORefactored inventoryCheckDAO =
        DAOFactory.getInstance().getInventoryCheckDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    @Test
    @DisplayName("生成盘点单号时基于数据库已有最大序号递增")
    void testGenerateNextCheckNoUsesDatabaseMaxSequence() throws SQLException {
        InventoryCheck existing = new InventoryCheck();
        existing.checkNo = "IC202606170001";
        existing.checkDate = "2026-06-17";
        existing.checkType = "full";
        existing.status = "checking";
        existing.operator = "admin";

        inventoryCheckDAO.insert(existing);

        String nextCheckNo = inventoryCheckDAO.generateNextCheckNo("2026-06-17");

        assertEquals("IC202606170002", nextCheckNo);
    }

    @Test
    @DisplayName("查询最近盘点记录时按创建时间倒序并限制数量")
    void testFindRecentUsesLimitAndNewestFirst() throws SQLException {
        InventoryCheck oldCheck = createCheck("IC202607130001", "2026-07-13", 1_000L);
        InventoryCheck middleCheck = createCheck("IC202607130002", "2026-07-13", 2_000L);
        InventoryCheck newestCheck = createCheck("IC202607130003", "2026-07-13", 3_000L);

        inventoryCheckDAO.insert(oldCheck);
        inventoryCheckDAO.insert(middleCheck);
        inventoryCheckDAO.insert(newestCheck);

        var recentChecks = inventoryCheckDAO.findRecent(2);

        assertEquals(2, recentChecks.size());
        assertEquals("IC202607130003", recentChecks.get(0).checkNo);
        assertEquals("IC202607130002", recentChecks.get(1).checkNo);
    }

    @Test
    @DisplayName("盘点记录支持状态流转与删除")
    void testCheckLifecycle() throws SQLException {
        InventoryCheck check = createCheck("IC202608230001", "2026-08-23", 5_000L);
        check.remark = "月底盘点";

        assertTrue(inventoryCheckDAO.insert(check));
        assertTrue(check.id > 0);

        InventoryCheck loaded = inventoryCheckDAO.findById(check.id);
        assertEquals("IC202608230001", loaded.checkNo);
        assertEquals("月底盘点", loaded.remark);

        assertTrue(inventoryCheckDAO.updateStatus(check.id, "checking"));
        assertEquals("checking", inventoryCheckDAO.findById(check.id).status);

        assertTrue(inventoryCheckDAO.updateStatistics(check.id, 10, 2));
        InventoryCheck updated = inventoryCheckDAO.findById(check.id);
        assertEquals(10, updated.totalItems);
        assertEquals(2, updated.diffItems);

        assertTrue(inventoryCheckDAO.complete(check.id, "admin"));
        assertEquals("completed", inventoryCheckDAO.findById(check.id).status);

        assertTrue(inventoryCheckDAO.delete(check.id));
        assertEquals(0, inventoryCheckDAO.findByStatus("completed").size());
    }

    @Test
    @DisplayName("重复完成盘点不产生状态迁移（防止差额二次累加）")
    void completeIsGuardedAgainstRepeatedCompletion() throws SQLException {
        InventoryCheck check = createCheck("IC202608230099", "2026-08-23", 6_000L);
        assertTrue(inventoryCheckDAO.insert(check));

        assertTrue(inventoryCheckDAO.complete(check.id, "admin"), "首次完成应发生状态迁移");
        assertFalse(inventoryCheckDAO.complete(check.id, "admin"),
            "盘点单已是 completed 时不得再返回 true，否则调用方会把差额二次加到库存上");
        assertEquals("completed", inventoryCheckDAO.findById(check.id).status);
    }

    private InventoryCheck createCheck(String checkNo, String checkDate, long createTimeMillis) {
        InventoryCheck check = new InventoryCheck();
        check.checkNo = checkNo;
        check.checkDate = checkDate;
        check.checkType = "full";
        check.status = "checking";
        check.operator = "admin";
        check.createTime = new Timestamp(createTimeMillis);
        check.updateTime = new Timestamp(createTimeMillis);
        return check;
    }
}
