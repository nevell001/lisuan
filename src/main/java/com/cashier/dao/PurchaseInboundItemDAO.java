package com.cashier.dao;

import com.cashier.model.PurchaseInboundItem;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购入库明细数据访问对象
 * 负责采购入库明细相关的数据库操作
 */
public class PurchaseInboundItemDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PurchaseInboundItemDAO.class);

    /**
     * 根据ID查找采购入库明细
     *
     * @param id 明细ID
     * @return 采购入库明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseInboundItem findById(int id) throws SQLException {
        String sql = "SELECT ii.id, ii.inbound_id, ii.order_item_id, ii.product_id, p.name as product_name, ii.quantity, ii.unit_price, ii.total_price " +
                     "FROM purchase_inbound_items ii LEFT JOIN products p ON ii.product_id = p.id WHERE ii.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseInboundItem(rs);
            }
        }
        return null;
    }

    /**
     * 根据入库ID查找所有明细
     *
     * @param inboundId 入库ID
     * @return 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseInboundItem> findByInboundId(int inboundId) throws SQLException {
        List<PurchaseInboundItem> items = new ArrayList<>();
        String sql = "SELECT ii.id, ii.inbound_id, ii.order_item_id, ii.product_id, p.name as product_name, ii.quantity, ii.unit_price, ii.total_price " +
                     "FROM purchase_inbound_items ii LEFT JOIN products p ON ii.product_id = p.id WHERE ii.inbound_id = ? ORDER BY ii.id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, inboundId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapRowToPurchaseInboundItem(rs));
            }
        }
        return items;
    }

    /**
     * 根据入库ID查找所有明细（别名方法）
     *
     * @param inboundId 入库ID
     * @return 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseInboundItem> findByInbound(int inboundId) throws SQLException {
        return findByInboundId(inboundId);
    }

    /**
     * 根据订单明细ID查找入库明细
     *
     * @param orderItemId 订单明细ID
     * @return 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseInboundItem> findByOrderItemId(int orderItemId) throws SQLException {
        List<PurchaseInboundItem> items = new ArrayList<>();
        String sql = "SELECT ii.id, ii.inbound_id, ii.order_item_id, ii.product_id, p.name as product_name, ii.quantity, ii.unit_price, ii.total_price " +
                     "FROM purchase_inbound_items ii LEFT JOIN products p ON ii.product_id = p.id WHERE ii.order_item_id = ? ORDER BY ii.id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderItemId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapRowToPurchaseInboundItem(rs));
            }
        }
        return items;
    }

    /**
     * 根据商品ID查找入库明细
     *
     * @param productId 商品ID
     * @return 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<PurchaseInboundItem> findByProductId(int productId) throws SQLException {
        List<PurchaseInboundItem> items = new ArrayList<>();
        String sql = "SELECT ii.id, ii.inbound_id, ii.order_item_id, ii.product_id, p.name as product_name, ii.quantity, ii.unit_price, ii.total_price " +
                     "FROM purchase_inbound_items ii LEFT JOIN products p ON ii.product_id = p.id WHERE ii.product_id = ? ORDER BY ii.id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapRowToPurchaseInboundItem(rs));
            }
        }
        return items;
    }

    /**
     * 根据入库ID和商品ID查找明细
     *
     * @param inboundId 入库ID
     * @param productId 商品ID
     * @return 采购入库明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static PurchaseInboundItem findByInboundAndProduct(int inboundId, int productId) throws SQLException {
        String sql = "SELECT ii.id, ii.inbound_id, ii.order_item_id, ii.product_id, p.name as product_name, ii.quantity, ii.unit_price, ii.total_price " +
                     "FROM purchase_inbound_items ii LEFT JOIN products p ON ii.product_id = p.id WHERE ii.inbound_id = ? AND ii.product_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, inboundId);
            pstmt.setInt(2, productId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPurchaseInboundItem(rs);
            }
        }
        return null;
    }

    /**
     * 插入新采购入库明细
     *
     * @param item 采购入库明细对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insert(PurchaseInboundItem item) throws SQLException {
        String sql = "INSERT INTO purchase_inbound_items (inbound_id, order_item_id, product_id, quantity, unit_price, total_price) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, item.inboundId);
            pstmt.setInt(2, item.orderItemId);
            pstmt.setInt(3, item.productId);
            pstmt.setInt(4, item.quantity);
            pstmt.setBigDecimal(5, item.unitPrice);
            pstmt.setBigDecimal(6, item.totalPrice);

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
     * 更新采购入库明细
     *
     * @param item 采购入库明细对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean update(PurchaseInboundItem item) throws SQLException {
        String sql = "UPDATE purchase_inbound_items SET quantity = ?, unit_price = ?, total_price = ? " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, item.quantity);
            pstmt.setBigDecimal(2, item.unitPrice);
            pstmt.setBigDecimal(3, item.totalPrice);
            pstmt.setInt(4, item.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除采购入库明细
     *
     * @param id 明细ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM purchase_inbound_items WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据入库ID删除所有明细
     *
     * @param inboundId 入库ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean deleteByInboundId(int inboundId) throws SQLException {
        String sql = "DELETE FROM purchase_inbound_items WHERE inbound_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, inboundId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入采购入库明细
     *
     * @param items 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public static void batchInsert(List<PurchaseInboundItem> items) throws SQLException {
        String sql = "INSERT INTO purchase_inbound_items (inbound_id, order_item_id, product_id, quantity, unit_price, total_price) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (PurchaseInboundItem item : items) {
                pstmt.setInt(1, item.inboundId);
                pstmt.setInt(2, item.orderItemId);
                pstmt.setInt(3, item.productId);
                pstmt.setInt(4, item.quantity);
                pstmt.setBigDecimal(5, item.unitPrice);
                pstmt.setBigDecimal(6, item.totalPrice);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 计算入库单的总数量和总金额
     *
     * @param inboundId 入库ID
     * @return 数组，[0]=总数量，[1]=总金额
     * @throws SQLException 数据库操作异常
     */
    public static Object[] calculateInboundTotal(int inboundId) throws SQLException {
        String sql = "SELECT SUM(quantity) as total_quantity, SUM(total_price) as total_amount " +
                     "FROM purchase_inbound_items WHERE inbound_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, inboundId);
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
     * 将 ResultSet 映射为 PurchaseInboundItem 对象
     *
     * @param rs 结果集
     * @return 采购入库明细对象
     * @throws SQLException 数据库操作异常
     */
    private static PurchaseInboundItem mapRowToPurchaseInboundItem(ResultSet rs) throws SQLException {
        PurchaseInboundItem item = new PurchaseInboundItem();
        item.id = rs.getInt("id");
        item.inboundId = rs.getInt("inbound_id");
        item.orderItemId = rs.getInt("order_item_id");
        item.productId = rs.getInt("product_id");
        item.productName = rs.getString("product_name");
        item.quantity = rs.getInt("quantity");
        item.unitPrice = rs.getBigDecimal("unit_price");
        item.totalPrice = rs.getBigDecimal("total_price");
        return item;
    }
}