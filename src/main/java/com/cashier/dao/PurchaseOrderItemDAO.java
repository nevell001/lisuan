package com.cashier.dao;

import com.cashier.model.PurchaseOrderItem;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购订单明细数据访问对象
 * 负责采购订单明细相关的数据库操作
 */
public class PurchaseOrderItemDAO {
    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderItemDAO.class);

    /**
     * 根据ID查找采购订单明细
     *
     * @param id 明细ID
     * @return 采购订单明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseOrderItem findById(int id) throws SQLException {
        String sql = "SELECT id, order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity " +
                     "FROM purchase_order_items WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseOrderItem(rs);
            }
        }
        return null;
    }

    /**
     * 根据订单ID查找所有明细
     *
     * @param orderId 订单ID
     * @return 采购订单明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseOrderItem> findByOrderId(int orderId) throws SQLException {
        List<PurchaseOrderItem> items = new ArrayList<>();
        String sql = "SELECT id, order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity " +
                     "FROM purchase_order_items WHERE order_id = ? ORDER BY id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapRowToPurchaseOrderItem(rs));
            }
        }
        return items;
    }

    /**
     * 根据订单ID查找所有明细（别名方法）
     *
     * @param orderId 订单ID
     * @return 采购订单明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseOrderItem> findByOrder(int orderId) throws SQLException {
        return findByOrderId(orderId);
    }

    /**
     * 根据商品ID查找采购订单明细
     *
     * @param productId 商品ID
     * @return 采购订单明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseOrderItem> findByProductId(int productId) throws SQLException {
        List<PurchaseOrderItem> items = new ArrayList<>();
        String sql = "SELECT id, order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity " +
                     "FROM purchase_order_items WHERE product_id = ? ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapRowToPurchaseOrderItem(rs));
            }
        }
        return items;
    }

    /**
     * 根据订单ID和商品ID查找明细
     *
     * @param orderId   订单ID
     * @param productId 商品ID
     * @return 采购订单明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseOrderItem findByOrderAndProduct(int orderId, int productId) throws SQLException {
        String sql = "SELECT id, order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity " +
                     "FROM purchase_order_items WHERE order_id = ? AND product_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            pstmt.setInt(2, productId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseOrderItem(rs);
            }
        }
        return null;
    }

    /**
     * 插入新采购订单明细
     *
     * @param item 采购订单明细对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insert(PurchaseOrderItem item) throws SQLException {
        String sql = "INSERT INTO purchase_order_items (order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, item.orderId);
            pstmt.setInt(2, item.productId);
            pstmt.setString(3, item.productName);
            pstmt.setInt(4, item.quantity);
            pstmt.setBigDecimal(5, item.unitPrice);
            pstmt.setBigDecimal(6, item.totalPrice);
            pstmt.setInt(7, item.inboundQuantity);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新采购订单明细
     *
     * @param item 采购订单明细对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean update(PurchaseOrderItem item) throws SQLException {
        String sql = "UPDATE purchase_order_items SET product_name = ?, quantity = ?, unit_price = ?, total_price = ?, inbound_quantity = ? " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.productName);
            pstmt.setInt(2, item.quantity);
            pstmt.setBigDecimal(3, item.unitPrice);
            pstmt.setBigDecimal(4, item.totalPrice);
            pstmt.setInt(5, item.inboundQuantity);
            pstmt.setInt(6, item.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新入库数量
     *
     * @param id              明细ID
     * @param inboundQuantity 新的入库数量
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean updateInboundQuantity(int id, int inboundQuantity) throws SQLException {
        String sql = "UPDATE purchase_order_items SET inbound_quantity = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, inboundQuantity);
            pstmt.setInt(2, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 增加入库数量
     *
     * @param id    明细ID
     * @param delta 增量
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean increaseInboundQuantity(int id, int delta) throws SQLException {
        String sql = "UPDATE purchase_order_items SET inbound_quantity = inbound_quantity + ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, delta);
            pstmt.setInt(2, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除采购订单明细
     *
     * @param id 明细ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM purchase_order_items WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据订单ID删除所有明细
     *
     * @param orderId 订单ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean deleteByOrderId(int orderId) throws SQLException {
        String sql = "DELETE FROM purchase_order_items WHERE order_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入采购订单明细
     *
     * @param items 采购订单明细列表
     * @throws SQLException 数据库操作异常
     */
    public static void batchInsert(List<PurchaseOrderItem> items) throws SQLException {
        String sql = "INSERT INTO purchase_order_items (order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (PurchaseOrderItem item : items) {
                pstmt.setInt(1, item.orderId);
                pstmt.setInt(2, item.productId);
                pstmt.setString(3, item.productName);
                pstmt.setInt(4, item.quantity);
                pstmt.setBigDecimal(5, item.unitPrice);
                pstmt.setBigDecimal(6, item.totalPrice);
                pstmt.setInt(7, item.inboundQuantity);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 计算订单的总数量和总金额
     *
     * @param orderId 订单ID
     * @return 数组，[0]=总数量，[1]=总金额
     * @throws SQLException 数据库操作异常
     */
    public static Object[] calculateOrderTotal(int orderId) throws SQLException {
        String sql = "SELECT SUM(quantity) as total_quantity, SUM(total_price) as total_amount " +
                     "FROM purchase_order_items WHERE order_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Object[]{
                    rs.getInt("total_quantity"),
                    rs.getBigDecimal("total_amount")
                };
            }
        }
        return new Object[]{0, BigDecimal.ZERO};
    }

    /**
     * 将 ResultSet 映射为 PurchaseOrderItem 对象
     *
     * @param rs 结果集
     * @return 采购订单明细对象
     * @throws SQLException 数据库操作异常
     */
    private static PurchaseOrderItem mapRowToPurchaseOrderItem(ResultSet rs) throws SQLException {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.id = rs.getInt("id");
        item.orderId = rs.getInt("order_id");
        item.productId = rs.getInt("product_id");
        item.productName = rs.getString("product_name");
        item.quantity = rs.getInt("quantity");
        item.unitPrice = rs.getBigDecimal("unit_price");
        item.totalPrice = rs.getBigDecimal("total_price");
        item.inboundQuantity = rs.getInt("inbound_quantity");
        return item;
    }
}