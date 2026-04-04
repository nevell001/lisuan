package com.cashier.dao;

import com.cashier.model.ReturnOrderItem;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 退货订单明细数据访问对象
 */
public class ReturnOrderItemDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReturnOrderItemDAO.class);

    /**
     * 插入退货订单明细
     */
    public static boolean insert(ReturnOrderItem item) {
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection()) {
            return insertWithConnection(conn, item);
        } catch (SQLException e) {
            logger.error("插入退货订单明细失败", e);
            return false;
        }
    }

    /**
     * 使用指定连接插入退货订单明细
     * @param conn 数据库连接
     * @param item 退货订单明细
     * @return 插入是否成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insertWithConnection(Connection conn, ReturnOrderItem item) throws SQLException {
        String sql = "INSERT INTO return_order_items (return_order_id, product_id, product_code, product_name, " +
                "barcode, category, return_quantity, unit_price, return_amount, reason, `condition`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入退货订单明细
     */
    public static boolean batchInsert(List<ReturnOrderItem> items) {
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection()) {
            return batchInsertWithConnection(conn, items);
        } catch (SQLException e) {
            logger.error("批量插入退货订单明细失败", e);
            return false;
        }
    }

    /**
     * 使用指定连接批量插入退货订单明细
     * @param conn 数据库连接
     * @param items 退货订单明细列表
     * @return 插入是否成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean batchInsertWithConnection(Connection conn, List<ReturnOrderItem> items) throws SQLException {
        String sql = "INSERT INTO return_order_items (return_order_id, product_id, product_code, product_name, " +
                "barcode, category, return_quantity, unit_price, return_amount, reason, `condition`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ReturnOrderItem item : items) {
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
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            return results.length > 0;
        }
    }

    /**
     * 更新退货订单明细
     */
    public static boolean update(ReturnOrderItem item) {
        String sql = "UPDATE return_order_items SET product_id = ?, product_code = ?, product_name = ?, " +
                "barcode = ?, category = ?, return_quantity = ?, unit_price = ?, return_amount = ?, " +
                "reason = ?, `condition` = ? WHERE id = ?";

        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
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
            return false;
        }
    }

    /**
     * 删除退货订单明细
     */
    public static boolean delete(int id) {
        String sql = "DELETE FROM return_order_items WHERE id = ?";
        
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("删除退货订单明细失败", e);
            return false;
        }
    }

    /**
     * 根据 ID 查找退货订单明细
     */
    public static ReturnOrderItem findById(int id) {
        String sql = "SELECT * FROM return_order_items WHERE id = ?";
        
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapRowToReturnOrderItem(rs);
            }
        } catch (SQLException e) {
            logger.error("查找退货订单明细失败", e);
        }
        
        return null;
    }

    /**
     * 根据退货单号查找所有明细
     */
    public static List<ReturnOrderItem> findByReturnOrderId(String returnOrderId) {
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection()) {
            return findByReturnOrderIdWithConnection(conn, returnOrderId);
        } catch (SQLException e) {
            logger.error("根据退货单号查找明细失败", e);
        }

        return new ArrayList<>();
    }

    /**
     * 使用指定连接根据退货单号查找所有明细
     * @param conn 数据库连接
     * @param returnOrderId 退货单号
     * @return 明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<ReturnOrderItem> findByReturnOrderIdWithConnection(Connection conn, String returnOrderId) throws SQLException {
        String sql = "SELECT * FROM return_order_items WHERE return_order_id = ?";
        List<ReturnOrderItem> items = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, returnOrderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRowToReturnOrderItem(rs));
                }
            }
        }

        return items;
    }

    /**
     * 删除指定退货单的所有明细
     */
    public static boolean deleteByReturnOrderId(String returnOrderId) {
        String sql = "DELETE FROM return_order_items WHERE return_order_id = ?";
        
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, returnOrderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("删除退货单明细失败", e);
            return false;
        }
    }

    /**
     * 计算退货单的总金额
     */
    public static double calculateTotalAmount(String returnOrderId) {
        String sql = "SELECT SUM(return_amount) as total FROM return_order_items WHERE return_order_id = ?";
        
        try (Connection conn = com.cashier.util.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, returnOrderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            logger.error("计算退货单总金额失败", e);
        }
        
        return 0.0;
    }

    /**
     * 将 ResultSet 映射为 ReturnOrderItem 对象
     */
    private static ReturnOrderItem mapRowToReturnOrderItem(ResultSet rs) throws SQLException {
        ReturnOrderItem item = new ReturnOrderItem();
        item.id = rs.getInt("id");
        item.returnOrderId = rs.getString("return_order_id");
        item.productId = rs.getInt("product_id");
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
}