package com.cashier.dao;

import com.cashier.model.InventoryCheck;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

/**
 * 库存盘点数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class InventoryCheckDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "id, check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time ";

    private static final RowMapper<InventoryCheck> CHECK_MAPPER = new RowMapper<InventoryCheck>() {
        @Override
        public InventoryCheck mapRow(ResultSet rs, int rowNum) throws SQLException {
            InventoryCheck check = new InventoryCheck();
            check.id = rs.getInt("id");
            check.checkNo = rs.getString("check_no");
            check.checkDate = rs.getString("check_date");
            check.checkType = rs.getString("check_type");
            check.totalItems = rs.getInt("total_items");
            check.diffItems = rs.getInt("diff_items");
            check.status = rs.getString("status");
            check.operator = rs.getString("operator");
            check.checker = rs.getString("checker");
            check.remark = rs.getString("remark");
            check.createTime = rs.getTimestamp("create_time");
            check.updateTime = rs.getTimestamp("update_time");
            return check;
        }
    };

    /**
     * 根据ID查找库存盘点记录
     *
     * @param id 盘点ID
     * @return 库存盘点对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public InventoryCheck findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check WHERE id = ?", CHECK_MAPPER, id);
    }

    /**
     * 查询所有库存盘点记录
     *
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheck> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check ORDER BY create_time DESC", CHECK_MAPPER);
    }

    /**
     * 查询最近的库存盘点记录，用于桌面列表默认加载。
     *
     * @param limit 最大返回数量
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheck> findRecent(int limit) throws SQLException {
        int safeLimit = limit > 0 ? limit : 100;
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check ORDER BY create_time DESC LIMIT ?", CHECK_MAPPER, safeLimit);
    }

    /**
     * 根据盘点单号查找库存盘点记录
     *
     * @param checkNo 盘点单号
     * @return 库存盘点对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public InventoryCheck findByCheckNo(String checkNo) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check WHERE check_no = ?", CHECK_MAPPER, checkNo);
    }

    /**
     * 根据数据库中已有单号生成下一个盘点单号。
     *
     * @param checkDate 盘点日期（yyyy-MM-dd）
     * @return 新盘点单号，格式 ICyyyyMMdd0001
     * @throws SQLException 数据库操作异常
     */
    public String generateNextCheckNo(String checkDate) throws SQLException {
        String dateStr = checkDate != null ? checkDate.replaceAll("[^0-9]", "") : "";
        if (dateStr.length() != 8) {
            dateStr = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
                .format(com.cashier.util.DateTimeFormats.COMPACT_DATE);
        }

        String prefix = "IC" + dateStr;
        String latestCheckNo = queryOneOrNull(
            "SELECT check_no FROM inventory_check WHERE check_no LIKE ? ORDER BY check_no DESC LIMIT 1",
            (rs, rowNum) -> rs.getString("check_no"), prefix + "%");

        int maxSeq = 0;
        if (latestCheckNo != null && latestCheckNo.length() > prefix.length()) {
            try {
                maxSeq = Integer.parseInt(latestCheckNo.substring(prefix.length()));
            } catch (NumberFormatException ignored) {
                maxSeq = 0;
            }
        }

        return prefix + String.format("%04d", maxSeq + 1);
    }

    /**
     * 根据盘点类型查找库存盘点记录
     *
     * @param checkType 盘点类型（full-全盘，partial-部分盘点）
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheck> findByCheckType(String checkType) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check WHERE check_type = ? ORDER BY create_time DESC", CHECK_MAPPER, checkType);
    }

    /**
     * 根据状态查找库存盘点记录
     *
     * @param status 盘点状态（pending-待盘点，checking-盘点中，completed-已完成）
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheck> findByStatus(String status) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check WHERE status = ? ORDER BY create_time DESC", CHECK_MAPPER, status);
    }

    /**
     * 根据盘点人查找库存盘点记录
     *
     * @param operator 盘点人
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheck> findByOperator(String operator) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check WHERE operator = ? ORDER BY create_time DESC", CHECK_MAPPER, operator);
    }

    /**
     * 根据日期范围查找库存盘点记录
     *
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<InventoryCheck> findByDateRange(String startDate, String endDate) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM inventory_check WHERE check_date BETWEEN ? AND ? ORDER BY create_time DESC",
            CHECK_MAPPER, startDate, endDate);
    }

    /**
     * 插入新库存盘点记录
     *
     * @param check 库存盘点对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insert(InventoryCheck check) throws SQLException {
        try (Connection conn = getConnection()) {
            return insertWithConnection(conn, check);
        }
    }

    /**
     * 使用指定连接插入库存盘点记录（事务内调用）。
     *
     * @param conn  数据库连接
     * @param check 库存盘点对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insertWithConnection(Connection conn, InventoryCheck check) throws SQLException {
        String sql = "INSERT INTO inventory_check (check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, check.checkNo);
            pstmt.setString(2, check.checkDate);
            pstmt.setString(3, check.checkType);
            pstmt.setInt(4, check.totalItems);
            pstmt.setInt(5, check.diffItems);
            pstmt.setString(6, check.status);
            pstmt.setString(7, check.operator);
            pstmt.setString(8, check.checker);
            pstmt.setString(9, check.remark);
            pstmt.setTimestamp(10, check.createTime);
            pstmt.setTimestamp(11, check.updateTime);
            if (pstmt.executeUpdate() <= 0) {
                return false;
            }
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    check.id = keys.getInt(1);
                }
            }
            return true;
        }
    }

    /**
     * 更新库存盘点记录
     *
     * @param check 库存盘点对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public boolean update(InventoryCheck check) throws SQLException {
        try (Connection conn = getConnection()) {
            return updateWithConnection(conn, check);
        }
    }

    /**
     * 使用指定连接更新库存盘点记录（事务内调用）。
     *
     * @param conn  数据库连接
     * @param check 库存盘点对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public boolean updateWithConnection(Connection conn, InventoryCheck check) throws SQLException {
        String sql = "UPDATE inventory_check SET check_no = ?, check_date = ?, check_type = ?, "
            + "total_items = ?, diff_items = ?, status = ?, operator = ?, checker = ?, remark = ?, update_time = ? "
            + "WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, check.checkNo);
            pstmt.setString(2, check.checkDate);
            pstmt.setString(3, check.checkType);
            pstmt.setInt(4, check.totalItems);
            pstmt.setInt(5, check.diffItems);
            pstmt.setString(6, check.status);
            pstmt.setString(7, check.operator);
            pstmt.setString(8, check.checker);
            pstmt.setString(9, check.remark);
            pstmt.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(11, check.id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新库存盘点状态
     *
     * @param id     盘点ID
     * @param status 新状态
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public boolean updateStatus(int id, String status) throws SQLException {
        return executeUpdate(
            "UPDATE inventory_check SET status = ?, update_time = ? WHERE id = ?",
            status, new Timestamp(System.currentTimeMillis()), id) > 0;
    }

    /**
     * 更新盘点统计信息
     *
     * @param id         盘点ID
     * @param totalItems 总商品数
     * @param diffItems  差异数
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public boolean updateStatistics(int id, int totalItems, int diffItems) throws SQLException {
        return executeUpdate(
            "UPDATE inventory_check SET total_items = ?, diff_items = ?, update_time = ? WHERE id = ?",
            totalItems, diffItems, new Timestamp(System.currentTimeMillis()), id) > 0;
    }

    /**
     * 完成盘点
     *
     * @param id      盘点ID
     * @param checker 审核人
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public boolean complete(int id, String checker) throws SQLException {
        try (Connection conn = getConnection()) {
            return completeWithConnection(conn, id, checker);
        }
    }

    /**
     * 在调用方事务内把盘点单置为已完成后返回是否真的发生了状态迁移。
     *
     * <p>{@code AND status <> 'completed'} 是并发/重复点击的守卫：没有它时重复完成会再返回 true，
     * 调用方就会把盘点差额**二次**加到库存上。</p>
     *
     * @param conn    调用方事务连接
     * @param id      盘点ID
     * @param checker 审核人
     * @return 状态确实从非 completed 迁移到 completed 时返回 true
     * @throws SQLException 数据库操作异常
     */
    public boolean completeWithConnection(Connection conn, int id, String checker) throws SQLException {
        String sql = "UPDATE inventory_check SET status = 'completed', checker = ?, update_time = ? "
            + "WHERE id = ? AND status <> 'completed'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, checker);
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(3, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除库存盘点记录
     *
     * @param id 盘点ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = getConnection()) {
            return deleteWithConnection(conn, id);
        }
    }

    /**
     * 使用指定连接删除库存盘点记录（事务内调用）。
     *
     * @param conn 数据库连接
     * @param id   盘点ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public boolean deleteWithConnection(Connection conn, int id) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM inventory_check WHERE id = ?")) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入库存盘点记录
     *
     * @param checks 库存盘点记录列表
     * @throws SQLException 数据库操作异常
     */
    public void batchInsert(List<InventoryCheck> checks) throws SQLException {
        batchUpdate(
            "INSERT INTO inventory_check (check_no, check_date, check_type, total_items, diff_items, status, operator, checker, remark, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            checks.stream()
                .map(check -> new Object[]{
                    check.checkNo, check.checkDate, check.checkType, check.totalItems, check.diffItems,
                    check.status, check.operator, check.checker, check.remark, check.createTime, check.updateTime})
                .toList());
    }

    /**
     * 统计盘点记录数量
     *
     * @param status 盘点状态（可为null）
     * @return 记录数量
     * @throws SQLException 数据库操作异常
     */
    public int countByStatus(String status) throws SQLException {
        if (status == null || status.isEmpty()) {
            return queryInt("SELECT COUNT(*) FROM inventory_check");
        }
        return queryInt("SELECT COUNT(*) FROM inventory_check WHERE status = ?", status);
    }
}
