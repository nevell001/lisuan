package com.cashier.dao;

import com.cashier.model.ReturnReservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 退货占用台账 DAO（F10）。
 *
 * <p>只提供"写入占用"和"读取仍占用量"两个能力，均在调用方事务内执行：
 * 建单时先锁原交易行（{@code TransactionDAORefactored.lockForReturnWithConnection}），
 * 再用这里的占用合计做校验，最后与退货单、明细同事务写入。</p>
 *
 * <p><b>占用是否生效以父退货单状态为准</b>：查询 join {@code return_orders} 并排除
 * {@code REJECTED}，因此被驳回的退货立刻释放额度，不依赖台账行自身状态是否已同步。</p>
 */
public class ReturnReservationDAORefactored extends BaseDAO {

    /**
     * 批量写入占用台账。
     *
     * @return 是否全部写入（批量返回行数非空即视为成功，与既有 DAO 的 batchInsert 口径一致）
     */
    public boolean batchInsertWithConnection(Connection conn, List<ReturnReservation> reservations)
            throws SQLException {
        if (reservations == null || reservations.isEmpty()) {
            return true;
        }
        String sql = "INSERT INTO return_reservations "
            + "(return_order_id, original_transaction_id, product_id, quantity, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ReturnReservation reservation : reservations) {
                stmt.setString(1, reservation.returnOrderId);
                stmt.setString(2, reservation.originalTransactionId);
                stmt.setInt(3, reservation.productId);
                stmt.setInt(4, reservation.quantity);
                stmt.setString(5, reservation.status != null ? reservation.status : ReturnReservation.STATUS_PENDING);
                stmt.addBatch();
            }
            int[] results = stmt.executeBatch();
            return results.length > 0;
        }
    }

    /**
     * 某原交易上**仍占用**的数量，按商品聚合。
     *
     * <p>已驳回的退货单不占额度（{@code ro.status <> 'REJECTED'}），与建单界面过去
     * "排除 REJECTED 后按商品汇总已退数量"的口径一致。</p>
     */
    public Map<Integer, Integer> sumReservedQuantitiesWithConnection(Connection conn, String transactionId)
            throws SQLException {
        Map<Integer, Integer> reserved = new HashMap<>();
        String sql = "SELECT rr.product_id AS pid, SUM(rr.quantity) AS qty FROM return_reservations rr "
            + "JOIN return_orders ro ON rr.return_order_id = ro.return_order_id "
            + "WHERE rr.original_transaction_id = ? AND ro.status <> 'REJECTED' GROUP BY rr.product_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transactionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reserved.put(rs.getInt("pid"), rs.getInt("qty"));
                }
            }
        }
        return reserved;
    }

    /**
     * 同步某退货单台账行的状态（F10-b），与退货单状态迁移**同事务**调用。
     *
     * <p>占用是否生效本来就按父退货单状态判定（见类注释），所以这里只影响审计可视化与
     * 排查便利；返回 0 表示这张单没有台账行（如台账上线前建的、或 API 直接完成的整单），
     * 调用方据此只记日志，不当成失败。</p>
     *
     * @return 受影响行数
     */
    public int updateStatusForReturnOrderWithConnection(Connection conn, String returnOrderId, String status)
            throws SQLException {
        if (returnOrderId == null || returnOrderId.isBlank() || status == null) {
            return 0;
        }
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE return_reservations SET status = ? WHERE return_order_id = ?")) {
            stmt.setString(1, status);
            stmt.setString(2, returnOrderId);
            return stmt.executeUpdate();
        }
    }
}
