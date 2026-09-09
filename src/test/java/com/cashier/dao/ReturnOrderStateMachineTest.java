package com.cashier.dao;

import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退货单状态机原子迁移测试
 * 回归 P1：审批/完成必须带状态条件（WHERE status=...），防止并发重复审批（重复恢复库存）
 * 与重复完成（重复退款/重复写流水）。
 */
@DisplayName("退货单状态机原子迁移测试")
public class ReturnOrderStateMachineTest extends DatabaseTestBase {

    private final ReturnOrderDAORefactored returnOrderDAO = DAOFactory.getInstance().getReturnOrderDAO();

    private void insertReturnOrder(String returnOrderId, String status) throws Exception {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO return_orders (return_order_id, original_transaction_id, return_date, " +
                "total_amount, status, operator_name) VALUES ('" + returnOrderId + "', 'TX-1', " +
                "'" + Timestamp.valueOf("2026-01-01 10:00:00") + "', 100.00, '" + status + "', 'operator')");
        }
    }

    private String queryStatus(String returnOrderId) throws Exception {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT status FROM return_orders WHERE return_order_id = '" + returnOrderId + "'")) {
            assertTrue(rs.next());
            return rs.getString("status");
        }
    }

    @Test
    @DisplayName("PENDING 单审批成功，且第二次审批返回失败（防重复恢复库存）")
    void approveOnlyOnceFromPending() throws Exception {
        insertReturnOrder("R-TEST-0001", "PENDING");

        try (Connection conn = getTestConnection()) {
            assertTrue(returnOrderDAO.markApprovalWithConnection(conn, "R-TEST-0001", "APPROVED", "admin", "同意"),
                "PENDING 单首次审批应成功");
            assertFalse(returnOrderDAO.markApprovalWithConnection(conn, "R-TEST-0001", "APPROVED", "admin", "再批一次"),
                "已 APPROVED 的单再次审批应失败（并发冲突场景）");
        }
        assertEquals("APPROVED", queryStatus("R-TEST-0001"));
    }

    @Test
    @DisplayName("驳回与审批互斥：REJECTED 单不能再审批")
    void rejectThenApproveFails() throws Exception {
        insertReturnOrder("R-TEST-0002", "PENDING");

        try (Connection conn = getTestConnection()) {
            assertTrue(returnOrderDAO.markApprovalWithConnection(conn, "R-TEST-0002", "REJECTED", "admin", "拒绝"));
            assertFalse(returnOrderDAO.markApprovalWithConnection(conn, "R-TEST-0002", "APPROVED", "admin", "再批"),
                "REJECTED 单不能再被审批为 APPROVED");
        }
        assertEquals("REJECTED", queryStatus("R-TEST-0002"));
    }

    @Test
    @DisplayName("仅 APPROVED 单可完成，且只能完成一次（防重复退款）")
    void completeOnlyOnceFromApproved() throws Exception {
        insertReturnOrder("R-TEST-0003", "APPROVED");

        try (Connection conn = getTestConnection()) {
            assertTrue(returnOrderDAO.markCompletedWithConnection(conn, "R-TEST-0003"),
                "APPROVED 单首次完成应成功");
            assertFalse(returnOrderDAO.markCompletedWithConnection(conn, "R-TEST-0003"),
                "COMPLETED 单再次完成应失败（并发冲突场景）");
        }
        assertEquals("COMPLETED", queryStatus("R-TEST-0003"));
    }

    @Test
    @DisplayName("PENDING 单不能直接完成")
    void completeFailsFromPending() throws Exception {
        insertReturnOrder("R-TEST-0004", "PENDING");

        try (Connection conn = getTestConnection()) {
            assertFalse(returnOrderDAO.markCompletedWithConnection(conn, "R-TEST-0004"),
                "未审批的单不能直接完成");
        }
        assertEquals("PENDING", queryStatus("R-TEST-0004"));
    }
}
