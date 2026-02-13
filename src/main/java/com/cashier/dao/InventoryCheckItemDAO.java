package com.cashier.dao;

import com.cashier.model.InventoryCheckItem;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存盘点明细数据访问对象
 * 负责库存盘点明细相关的数据库操作
 */
public class InventoryCheckItemDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InventoryCheckItemDAO.class);

    /**
     * 根据ID查找库存盘点明细
     *
     * @param id 明细ID
     * @return 库存盘点明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static InventoryCheckItem findById(int id) throws SQLException {
        String sql = "SELECT id, check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time " +
                     "FROM inventory_check_items WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToInventoryCheckItem(rs);
            }
        }
        return null;
    }

    /**
     * 根据盘点ID查找所有明细
     *
     * @param checkId 盘点ID
     * @return 库存盘点明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<InventoryCheckItem> findByCheckId(int checkId) throws SQLException {
        List<InventoryCheckItem> items = new ArrayList<>();
        String sql = "SELECT id, check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time " +
                     "FROM inventory_check_items WHERE check_id = ? ORDER BY id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, checkId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapRowToInventoryCheckItem(rs));
            }
        }
        return items;
    }

    /**
     * 根据盘点ID查找所有明细（别名方法）
     *
     * @param checkId 盘点ID
     * @return 库存盘点明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<InventoryCheckItem> findByCheck(int checkId) throws SQLException {
        return findByCheckId(checkId);
    }

    /**
     * 根据商品ID查找盘点明细
     *
     * @param productId 商品ID
     * @return 库存盘点明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<InventoryCheckItem> findByProductId(int productId) throws SQLException {
        List<InventoryCheckItem> items = new ArrayList<>();
        String sql = "SELECT id, check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time " +
                     "FROM inventory_check_items WHERE product_id = ? ORDER BY create_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapRowToInventoryCheckItem(rs));
            }
        }
        return items;
    }

    /**
     * 根据盘点ID和商品ID查找明细
     *
     * @param checkId   盘点ID
     * @param productId 商品ID
     * @return 库存盘点明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static InventoryCheckItem findByCheckAndProduct(int checkId, int productId) throws SQLException {
        String sql = "SELECT id, check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time " +
                     "FROM inventory_check_items WHERE check_id = ? AND product_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, checkId);
            pstmt.setInt(2, productId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToInventoryCheckItem(rs);
            }
        }
        return null;
    }

    /**
     * 根据盘点ID查找有差异的明细
     *
     * @param checkId 盘点ID
     * @return 有差异的库存盘点明细列表
     * @throws SQLException 数据库操作异常
     */
    public static List<InventoryCheckItem> findDifferenceByCheckId(int checkId) throws SQLException {
        List<InventoryCheckItem> items = new ArrayList<>();
        String sql = "SELECT id, check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time " +
                     "FROM inventory_check_items WHERE check_id = ? AND diff_quantity != 0 ORDER BY id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, checkId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapRowToInventoryCheckItem(rs));
            }
        }
        return items;
    }

    /**
     * 插入新库存盘点明细
     *
     * @param item 库存盘点明细对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insert(InventoryCheckItem item) throws SQLException {
        String sql = "INSERT INTO inventory_check_items (check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, item.checkId);
            pstmt.setInt(2, item.productId);
            pstmt.setString(3, item.productName);
            pstmt.setInt(4, item.bookQuantity);
            pstmt.setInt(5, item.actualQuantity);
            pstmt.setInt(6, item.diffQuantity);
            pstmt.setString(7, item.diffReason);
            pstmt.setTimestamp(8, item.createTime);

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
     * 更新库存盘点明细
     *
     * @param item 库存盘点明细对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean update(InventoryCheckItem item) throws SQLException {
        String sql = "UPDATE inventory_check_items SET product_name = ?, book_quantity = ?, actual_quantity = ?, " +
                     "diff_quantity = ?, diff_reason = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.productName);
            pstmt.setInt(2, item.bookQuantity);
            pstmt.setInt(3, item.actualQuantity);
            pstmt.setInt(4, item.diffQuantity);
            pstmt.setString(5, item.diffReason);
            pstmt.setInt(6, item.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新实际数量和差异数量
     *
     * @param id             明细ID
     * @param actualQuantity 实际数量
     * @param diffReason     差异原因
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean updateActualQuantity(int id, int actualQuantity, String diffReason) throws SQLException {
        String sql = "UPDATE inventory_check_items SET actual_quantity = ?, diff_quantity = actual_quantity - book_quantity, diff_reason = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, actualQuantity);
            pstmt.setString(2, diffReason);
            pstmt.setInt(3, id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除库存盘点明细
     *
     * @param id 明细ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM inventory_check_items WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据盘点ID删除所有明细
     *
     * @param checkId 盘点ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean deleteByCheckId(int checkId) throws SQLException {
        String sql = "DELETE FROM inventory_check_items WHERE check_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, checkId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入库存盘点明细
     *
     * @param items 库存盘点明细列表
     * @throws SQLException 数据库操作异常
     */
    public static void batchInsert(List<InventoryCheckItem> items) throws SQLException {
        String sql = "INSERT INTO inventory_check_items (check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (InventoryCheckItem item : items) {
                pstmt.setInt(1, item.checkId);
                pstmt.setInt(2, item.productId);
                pstmt.setString(3, item.productName);
                pstmt.setInt(4, item.bookQuantity);
                pstmt.setInt(5, item.actualQuantity);
                pstmt.setInt(6, item.diffQuantity);
                pstmt.setString(7, item.diffReason);
                pstmt.setTimestamp(8, item.createTime);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 统计盘点单的统计信息
     *
     * @param checkId 盘点ID
     * @return 数组，[0]=总商品数，[1]=差异商品数
     * @throws SQLException 数据库操作异常
     */
    public static int[] calculateCheckStatistics(int checkId) throws SQLException {
        String sql = "SELECT COUNT(*) as total_items, SUM(CASE WHEN diff_quantity != 0 THEN 1 ELSE 0 END) as diff_items " +
                     "FROM inventory_check_items WHERE check_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, checkId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new int[]{
                    rs.getInt("total_items"),
                    rs.getInt("diff_items")
                };
            }
        }
        return new int[]{0, 0};
    }

    /**
     * 计算差异数量总和（盘盈+盘亏）
     *
     * @param checkId 盘点ID
     * @return 数组，[0]=盘盈总数，[1]=盘亏总数
     * @throws SQLException 数据库操作异常
     */
    public static int[] calculateDiffSummary(int checkId) throws SQLException {
        String sql = "SELECT SUM(CASE WHEN diff_quantity > 0 THEN diff_quantity ELSE 0 END) as profit_quantity, " +
                     "SUM(CASE WHEN diff_quantity < 0 THEN ABS(diff_quantity) ELSE 0 END) as loss_quantity " +
                     "FROM inventory_check_items WHERE check_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, checkId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new int[]{
                    rs.getInt("profit_quantity"),
                    rs.getInt("loss_quantity")
                };
            }
        }
        return new int[]{0, 0};
    }

    /**
     * 将 ResultSet 映射为 InventoryCheckItem 对象
     *
     * @param rs 结果集
     * @return 库存盘点明细对象
     * @throws SQLException 数据库操作异常
     */
    private static InventoryCheckItem mapRowToInventoryCheckItem(ResultSet rs) throws SQLException {
        InventoryCheckItem item = new InventoryCheckItem();
        item.id = rs.getInt("id");
        item.checkId = rs.getInt("check_id");
        item.productId = rs.getInt("product_id");
        item.productName = rs.getString("product_name");
        item.bookQuantity = rs.getInt("book_quantity");
        item.actualQuantity = rs.getInt("actual_quantity");
        item.diffQuantity = rs.getInt("diff_quantity");
        item.diffReason = rs.getString("diff_reason");
        item.createTime = rs.getTimestamp("create_time");
        return item;
    }
}