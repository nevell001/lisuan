package com.cashier.dao;

import com.cashier.model.InventoryCheckItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 库存盘点明细数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class InventoryCheckItemDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "id, check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time ";

    private static final RowMapper<InventoryCheckItem> ITEM_MAPPER = new RowMapper<InventoryCheckItem>() {
        @Override
        public InventoryCheckItem mapRow(ResultSet rs, int rowNum) throws SQLException {
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
    };

    /**
     * 根据ID查找库存盘点明细
     *
     * @param id 明细ID
     * @return 库存盘点明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public InventoryCheckItem findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check_items WHERE id = ?", ITEM_MAPPER, id);
    }

    /**
     * 根据盘点ID查找所有明细
     *
     * @param checkId 盘点ID
     * @return 库存盘点明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheckItem> findByCheckId(int checkId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check_items WHERE check_id = ? ORDER BY id", ITEM_MAPPER, checkId);
    }

    /**
     * 在调用方事务内按盘点ID查找明细，供"完成盘点"一次性读取。
     *
     * @param conn    调用方事务连接
     * @param checkId 盘点ID
     * @return 库存盘点明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheckItem> findByCheckIdWithConnection(Connection conn, int checkId) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS +
            " FROM inventory_check_items WHERE check_id = ? ORDER BY id";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, checkId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<InventoryCheckItem> items = new java.util.ArrayList<>();
                while (rs.next()) {
                    items.add(ITEM_MAPPER.mapRow(rs, items.size()));
                }
                return items;
            }
        }
    }

    /**
     * 根据盘点ID查找所有明细（别名方法）
     *
     * @param checkId 盘点ID
     * @return 库存盘点明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheckItem> findByCheck(int checkId) throws SQLException {
        return findByCheckId(checkId);
    }

    /**
     * 根据商品ID查找盘点明细
     *
     * @param productId 商品ID
     * @return 库存盘点明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheckItem> findByProductId(int productId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check_items WHERE product_id = ? ORDER BY create_time DESC", ITEM_MAPPER, productId);
    }

    /**
     * 根据盘点ID和商品ID查找明细
     *
     * @param checkId   盘点ID
     * @param productId 商品ID
     * @return 库存盘点明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public InventoryCheckItem findByCheckAndProduct(int checkId, int productId) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check_items WHERE check_id = ? AND product_id = ?",
            ITEM_MAPPER, checkId, productId);
    }

    /**
     * 插入新库存盘点明细
     *
     * @param item 库存盘点明细对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insert(InventoryCheckItem item) throws SQLException {
        try (Connection conn = getConnection()) {
            return insertWithConnection(conn, item);
        }
    }

    /**
     * 使用指定连接插入库存盘点明细（事务内调用）。
     *
     * @param conn 数据库连接
     * @param item 库存盘点明细对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insertWithConnection(Connection conn, InventoryCheckItem item) throws SQLException {
        String sql = "INSERT INTO inventory_check_items (check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.checkId);
            pstmt.setInt(2, item.productId);
            pstmt.setString(3, item.productName);
            pstmt.setInt(4, item.bookQuantity);
            pstmt.setInt(5, item.actualQuantity);
            pstmt.setInt(6, item.diffQuantity);
            pstmt.setString(7, item.diffReason);
            pstmt.setTimestamp(8, item.createTime);
            if (pstmt.executeUpdate() <= 0) {
                return false;
            }
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    item.id = keys.getInt(1);
                }
            }
            return true;
        }
    }

    /**
     * 更新库存盘点明细
     *
     * @param item 库存盘点明细对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public boolean update(InventoryCheckItem item) throws SQLException {
        return executeUpdate(
            "UPDATE inventory_check_items SET product_name = ?, book_quantity = ?, actual_quantity = ?, " +
                "diff_quantity = ?, diff_reason = ? WHERE id = ?",
            item.productName, item.bookQuantity, item.actualQuantity,
            item.diffQuantity, item.diffReason, item.id) > 0;
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
    public boolean updateActualQuantity(int id, int actualQuantity, String diffReason) throws SQLException {
        return executeUpdate(
            "UPDATE inventory_check_items SET actual_quantity = ?, diff_quantity = actual_quantity - book_quantity, diff_reason = ? WHERE id = ?",
            actualQuantity, diffReason, id) > 0;
    }

    /**
     * 删除库存盘点明细
     *
     * @param id 明细ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM inventory_check_items WHERE id = ?", id) > 0;
    }

    /**
     * 根据盘点ID删除所有明细
     *
     * @param checkId 盘点ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public boolean deleteByCheckId(int checkId) throws SQLException {
        try (Connection conn = getConnection()) {
            return deleteByCheckIdWithConnection(conn, checkId);
        }
    }

    /**
     * 使用指定连接按盘点ID删除明细（事务内调用）。
     *
     * @param conn    数据库连接
     * @param checkId 盘点ID
     * @return 是否有行被删除
     * @throws SQLException 数据库操作异常
     */
    public boolean deleteByCheckIdWithConnection(Connection conn, int checkId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM inventory_check_items WHERE check_id = ?")) {
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
    public void batchInsert(List<InventoryCheckItem> items) throws SQLException {
        batchUpdate(
            "INSERT INTO inventory_check_items (check_id, product_id, product_name, book_quantity, actual_quantity, diff_quantity, diff_reason, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            items.stream()
                .map(item -> new Object[]{
                    item.checkId, item.productId, item.productName, item.bookQuantity,
                    item.actualQuantity, item.diffQuantity, item.diffReason, item.createTime})
                .toList());
    }

}
