package com.cashier.dao;

import com.cashier.model.Promotion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 促销数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class PromotionDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "id, promotion_code, name, type, threshold, discount, description, enabled, " +
        "start_date, end_date, usage_count, max_usage ";

    private static final RowMapper<Promotion> PROMOTION_MAPPER = new RowMapper<Promotion>() {
        @Override
        public Promotion mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp startDateTs = rs.getTimestamp("start_date");
            Timestamp endDateTs = rs.getTimestamp("end_date");
            return new Promotion(
                rs.getInt("id"),
                rs.getString("promotion_code"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getBigDecimal("threshold"),
                rs.getBigDecimal("discount"),
                rs.getString("description"),
                rs.getBoolean("enabled"),
                startDateTs != null ? startDateTs.toLocalDateTime() : null,
                endDateTs != null ? endDateTs.toLocalDateTime() : null,
                rs.getInt("usage_count"),
                rs.getInt("max_usage")
            );
        }
    };

    public List<Promotion> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + " FROM promotions ORDER BY id DESC", PROMOTION_MAPPER);
    }

    public List<Promotion> findRecent(int limit) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM promotions ORDER BY id DESC LIMIT ?", PROMOTION_MAPPER, Math.max(1, limit));
    }

    public Promotion findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM promotions WHERE id = ?", PROMOTION_MAPPER, id);
    }

    public Promotion findByIdWithConnection(Connection conn, int id) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM promotions WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? PROMOTION_MAPPER.mapRow(rs, 0) : null;
            }
        }
    }

    public List<Promotion> findEnabled() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM promotions WHERE enabled = true ORDER BY id DESC", PROMOTION_MAPPER);
    }

    public List<Promotion> findActive() throws SQLException {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM promotions WHERE enabled = true AND start_date <= ? AND end_date >= ? ORDER BY id DESC",
            PROMOTION_MAPPER, now, now);
    }

    public boolean insert(Promotion promotion) throws SQLException {
        long id = executeInsertReturnId(
            "INSERT INTO promotions (promotion_code, name, type, threshold, discount, description, " +
                "enabled, start_date, end_date, usage_count, max_usage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            promotion.promotionCode, promotion.name, promotion.type, promotion.threshold, promotion.discount,
            promotion.description, promotion.enabled,
            promotion.startDate != null ? Timestamp.valueOf(promotion.startDate) : null,
            promotion.endDate != null ? Timestamp.valueOf(promotion.endDate) : null,
            promotion.usageCount, promotion.maxUsage);
        promotion.id = (int) id;
        invalidatePricingCache();
        return id > 0;
    }

    public boolean update(Promotion promotion) throws SQLException {
        invalidatePricingCache();
        return executeUpdate(
            "UPDATE promotions SET promotion_code = ?, name = ?, type = ?, threshold = ?, discount = ?, " +
                "description = ?, enabled = ?, start_date = ?, end_date = ?, usage_count = ?, max_usage = ? WHERE id = ?",
            promotion.promotionCode, promotion.name, promotion.type, promotion.threshold, promotion.discount,
            promotion.description, promotion.enabled,
            promotion.startDate != null ? Timestamp.valueOf(promotion.startDate) : null,
            promotion.endDate != null ? Timestamp.valueOf(promotion.endDate) : null,
            promotion.usageCount, promotion.maxUsage, promotion.id) > 0;
    }

    public boolean delete(int id) throws SQLException {
        invalidatePricingCache();
        return executeUpdate("DELETE FROM promotions WHERE id = ?", id) > 0;
    }

    /**
     * 促销被改动后让定价用的短 TTL 缓存立刻失效（TD-032）。
     *
     * <p>放在 DAO 写入处而不是只放 Service：任何写路径（含直连 DAO 的调用与跨终端同步）都要失效，
     * 否则收银台可能继续按已删除/已停用的促销算钱。</p>
     */
    private static void invalidatePricingCache() {
        com.cashier.service.TransactionService.invalidatePromotionCache();
    }

    public boolean incrementUsage(int id) throws SQLException {
        return executeUpdate(
            "UPDATE promotions SET usage_count = usage_count + 1 WHERE id = ? AND (max_usage <= 0 OR usage_count < max_usage)",
            id) > 0;
    }

    public boolean incrementUsageWithConnection(Connection conn, int id) throws SQLException {
        String sql = "UPDATE promotions SET usage_count = usage_count + 1 WHERE id = ? AND (max_usage <= 0 OR usage_count < max_usage)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public void batchInsert(List<Promotion> promotions) throws SQLException {
        List<Object[]> params = new ArrayList<>(promotions.size());
        for (Promotion promotion : promotions) {
            params.add(new Object[]{
                promotion.promotionCode, promotion.name, promotion.type, promotion.threshold, promotion.discount,
                promotion.description, promotion.enabled,
                promotion.startDate != null ? Timestamp.valueOf(promotion.startDate) : null,
                promotion.endDate != null ? Timestamp.valueOf(promotion.endDate) : null,
                promotion.usageCount, promotion.maxUsage});
        }
        batchUpdate(
            "INSERT INTO promotions (promotion_code, name, type, threshold, discount, description, " +
                "enabled, start_date, end_date, usage_count, max_usage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            params);
    }

    public void batchInsertWithConnection(Connection conn, List<Promotion> promotions) throws SQLException {
        if (promotions == null || promotions.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO promotions (promotion_code, name, type, threshold, discount, description, " +
            "enabled, start_date, end_date, usage_count, max_usage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Promotion promotion : promotions) {
                pstmt.setString(1, promotion.promotionCode);
                pstmt.setString(2, promotion.name);
                pstmt.setString(3, promotion.type);
                pstmt.setBigDecimal(4, promotion.threshold);
                pstmt.setBigDecimal(5, promotion.discount);
                pstmt.setString(6, promotion.description);
                pstmt.setBoolean(7, promotion.enabled);
                pstmt.setTimestamp(8, promotion.startDate != null ? Timestamp.valueOf(promotion.startDate) : null);
                pstmt.setTimestamp(9, promotion.endDate != null ? Timestamp.valueOf(promotion.endDate) : null);
                pstmt.setInt(10, promotion.usageCount);
                pstmt.setInt(11, promotion.maxUsage);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}
