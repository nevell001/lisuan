package com.cashier.dao;

import com.cashier.exception.DatabaseException;
import com.cashier.model.ReturnOrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 退货订单明细数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 * 非连接方法保持原签名（不抛 SQLException，错误包装为 DatabaseException）。
 */
public class ReturnOrderItemDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "id, return_order_id, product_id, transaction_item_id, product_code, product_name, barcode, category, " +
        "return_quantity, unit_price, return_amount, reason, `condition` ";

    private static final RowMapper<ReturnOrderItem> ITEM_MAPPER = new RowMapper<ReturnOrderItem>() {
        @Override
        public ReturnOrderItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            ReturnOrderItem item = new ReturnOrderItem();
            item.id = rs.getInt("id");
            item.returnOrderId = rs.getString("return_order_id");
            item.productId = rs.getInt("product_id");
            int transactionItemId = rs.getInt("transaction_item_id");
            // 列可空：老库/老数据没有行级信息时必须保持 null，否则行级校验会误判成"第 0 行"
            item.transactionItemId = rs.wasNull() ? null : transactionItemId;
            item.productCode = rs.getString("product_code");
            item.productName = rs.getString("product_name");
            item.barcode = rs.getString("barcode");
            item.category = rs.getString("category");
            item.returnQuantity = rs.getInt("return_quantity");
            item.unitPrice = rs.getBigDecimal("unit_price");
            item.returnAmount = rs.getBigDecimal("return_amount");
            item.reason = rs.getString("reason");
            item.condition = rs.getString("condition");
            return item;
        }
    };

    /**
     * 插入退货订单明细
     */
    public boolean insert(ReturnOrderItem item) {
        try (Connection conn = getConnection()) {
            return insertWithConnection(conn, item);
        } catch (SQLException e) {
            logger.error("插入退货订单明细失败", e);
            throw new DatabaseException("插入退货订单明细失败", DatabaseException.DbErrorType.INSERT_FAILED, e);
        }
    }

    /**
     * 使用指定连接插入退货订单明细
     *
     * @param conn 数据库连接
     * @param item 退货订单明细
     * @return 插入是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insertWithConnection(Connection conn, ReturnOrderItem item) throws SQLException {
        String sql = "INSERT INTO return_order_items (return_order_id, product_id, product_code, product_name, " +
            "barcode, category, return_quantity, unit_price, return_amount, reason, `condition`, transaction_item_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindInsertParams(stmt, item);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入退货订单明细
     */
    public boolean batchInsert(List<ReturnOrderItem> items) {
        try (Connection conn = getConnection()) {
            return batchInsertWithConnection(conn, items);
        } catch (SQLException e) {
            logger.error("批量插入退货订单明细失败", e);
            throw new DatabaseException("批量插入退货订单明细失败", DatabaseException.DbErrorType.INSERT_FAILED, e);
        }
    }

    /**
     * 使用指定连接批量插入退货订单明细
     *
     * @param conn  数据库连接
     * @param items 退货订单明细列表
     * @return 插入是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean batchInsertWithConnection(Connection conn, List<ReturnOrderItem> items) throws SQLException {
        String sql = "INSERT INTO return_order_items (return_order_id, product_id, product_code, product_name, " +
            "barcode, category, return_quantity, unit_price, return_amount, reason, `condition`, transaction_item_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ReturnOrderItem item : items) {
                bindInsertParams(stmt, item);
                stmt.addBatch();
            }
            int[] results = stmt.executeBatch();
            return results.length > 0;
        }
    }

    /**
     * 更新退货订单明细
     */
    public boolean update(ReturnOrderItem item) {
        String sql = "UPDATE return_order_items SET product_id = ?, product_code = ?, product_name = ?, " +
            "barcode = ?, category = ?, return_quantity = ?, unit_price = ?, return_amount = ?, " +
            "reason = ?, `condition` = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, item.productId);
            stmt.setString(2, item.productCode);
            stmt.setString(3, item.productName);
            stmt.setString(4, item.barcode);
            stmt.setString(5, item.category);
            stmt.setInt(6, item.returnQuantity);
            stmt.setBigDecimal(7, item.unitPrice);
            stmt.setBigDecimal(8, item.returnAmount);
            stmt.setString(9, item.reason);
            stmt.setString(10, item.condition);
            stmt.setInt(11, item.id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("更新退货订单明细失败", e);
            throw new DatabaseException("更新退货订单明细失败", DatabaseException.DbErrorType.UPDATE_FAILED, e);
        }
    }

    /**
     * 删除退货订单明细
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM return_order_items WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("删除退货订单明细失败", e);
            throw new DatabaseException("删除退货订单明细失败", DatabaseException.DbErrorType.DELETE_FAILED, e);
        }
    }

    /**
     * 根据 ID 查找退货订单明细
     */
    public ReturnOrderItem findById(int id) {
        try (Connection conn = getConnection()) {
            return queryOneOrNullWithConnection(conn, "SELECT " + SELECT_COLUMNS +
                " FROM return_order_items WHERE id = ?", ITEM_MAPPER, id);
        } catch (SQLException e) {
            logger.error("查找退货订单明细失败", e);
            throw new DatabaseException("查找退货订单明细失败", DatabaseException.DbErrorType.QUERY_FAILED, e);
        }
    }

    /**
     * 根据退货单号查找所有明细
     */
    public List<ReturnOrderItem> findByReturnOrderId(String returnOrderId) {
        try (Connection conn = getConnection()) {
            return findByReturnOrderIdWithConnection(conn, returnOrderId);
        } catch (SQLException e) {
            logger.error("根据退货单号查找明细失败", e);
            throw new DatabaseException("根据退货单号查找明细失败", DatabaseException.DbErrorType.QUERY_FAILED, e);
        }
    }

    /**
     * 使用指定连接根据退货单号查找所有明细
     *
     * @param conn          数据库连接
     * @param returnOrderId 退货单号
     * @return 明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<ReturnOrderItem> findByReturnOrderIdWithConnection(Connection conn, String returnOrderId)
            throws SQLException {
        return queryListWithConnection(conn, "SELECT " + SELECT_COLUMNS +
            " FROM return_order_items WHERE return_order_id = ?", ITEM_MAPPER, returnOrderId);
    }

    /**
     * 删除指定退货单的所有明细
     */
    /**
     * 某原交易上**按行**仍占用的退货数量（行 id → 数量），供 F10-c 行级可退量校验。
     *
     * <p>取的是明细表（每行退了多少是权威记录），并排除已驳回的退货单——与台账口径一致。
     * 行 id 为空的老数据不参与行级校验（商品级校验仍然兜着）。</p>
     */
    public Map<Integer, Integer> sumReturnedQuantitiesByTransactionItemWithConnection(Connection conn,
                                                                                     String transactionId)
            throws SQLException {
        Map<Integer, Integer> returned = new HashMap<>();
        String sql = "SELECT roi.transaction_item_id AS item_id, SUM(roi.return_quantity) AS qty "
            + "FROM return_order_items roi JOIN return_orders ro ON roi.return_order_id = ro.return_order_id "
            + "WHERE ro.original_transaction_id = ? AND ro.status <> 'REJECTED' "
            + "AND roi.transaction_item_id IS NOT NULL GROUP BY roi.transaction_item_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transactionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    returned.put(rs.getInt("item_id"), rs.getInt("qty"));
                }
            }
        }
        return returned;
    }

    public boolean deleteByReturnOrderId(String returnOrderId) {
        String sql = "DELETE FROM return_order_items WHERE return_order_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, returnOrderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("删除退货单明细失败", e);
            throw new DatabaseException("删除退货单明细失败", DatabaseException.DbErrorType.DELETE_FAILED, e);
        }
    }

    /**
     * 计算退货单的总金额
     */
    public double calculateTotalAmount(String returnOrderId) {
        String sql = "SELECT SUM(return_amount) as total FROM return_order_items WHERE return_order_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, returnOrderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            logger.error("计算退货单总金额失败", e);
            throw new DatabaseException("计算退货单总金额失败", DatabaseException.DbErrorType.QUERY_FAILED, e);
        }

        return 0.0;
    }

    private void bindInsertParams(PreparedStatement stmt, ReturnOrderItem item) throws SQLException {
        stmt.setString(1, item.returnOrderId);
        stmt.setInt(2, item.productId);
        stmt.setString(3, item.productCode);
        stmt.setString(4, item.productName);
        stmt.setString(5, item.barcode);
        stmt.setString(6, item.category);
        stmt.setInt(7, item.returnQuantity);
        stmt.setBigDecimal(8, item.unitPrice);
        stmt.setBigDecimal(9, item.returnAmount);
        stmt.setString(10, item.reason);
        stmt.setString(11, item.condition);
        if (item.transactionItemId != null && item.transactionItemId > 0) {
            stmt.setInt(12, item.transactionItemId);
        } else {
            stmt.setNull(12, java.sql.Types.INTEGER);
        }
    }

    private <T> T queryOneOrNullWithConnection(Connection conn, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        List<T> list = queryListWithConnection(conn, sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    private <T> List<T> queryListWithConnection(Connection conn, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        List<T> results = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                int rowNum = 0;
                while (rs.next()) {
                    results.add(mapper.mapRow(rs, ++rowNum));
                }
            }
        }
        return results;
    }
}
