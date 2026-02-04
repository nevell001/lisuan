package com.cashier.dao;

import com.cashier.model.Promotion;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 促销数据访问对象
 * 负责促销相关的数据库操作
 */
public class PromotionDAO {

    /**
     * 查询所有促销
     */
    public static List<Promotion> findAll() throws SQLException {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT id, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                promotions.add(mapRowToPromotion(rs));
            }
        }
        return promotions;
    }

    /**
     * 根据ID查找促销
     */
    public static Promotion findById(int id) throws SQLException {
        String sql = "SELECT id, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToPromotion(rs);
            }
        }
        return null;
    }

    /**
     * 查询启用的促销
     */
    public static List<Promotion> findEnabled() throws SQLException {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT id, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions " +
                     "WHERE enabled = true ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                promotions.add(mapRowToPromotion(rs));
            }
        }
        return promotions;
    }

    /**
     * 查询当前有效的促销
     */
    public static List<Promotion> findActive() throws SQLException {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT id, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions " +
                     "WHERE enabled = true AND start_date <= NOW() AND end_date >= NOW() " +
                     "ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                promotions.add(mapRowToPromotion(rs));
            }
        }
        return promotions;
    }

    /**
     * 插入新促销
     */
    public static boolean insert(Promotion promotion) throws SQLException {
        String sql = "INSERT INTO promotions (name, type, threshold, discount, description, " +
                     "enabled, start_date, end_date, usage_count, max_usage) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, promotion.name);
            pstmt.setString(2, promotion.type);
            pstmt.setDouble(3, promotion.threshold);
            pstmt.setDouble(4, promotion.discount);
            pstmt.setString(5, promotion.description);
            pstmt.setBoolean(6, promotion.enabled);
            pstmt.setTimestamp(7, new Timestamp(promotion.startDate.getTime()));
            pstmt.setTimestamp(8, new Timestamp(promotion.endDate.getTime()));
            pstmt.setInt(9, promotion.usageCount);
            pstmt.setInt(10, promotion.maxUsage);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        promotion.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新促销
     */
    public static boolean update(Promotion promotion) throws SQLException {
        String sql = "UPDATE promotions SET name = ?, type = ?, threshold = ?, discount = ?, " +
                     "description = ?, enabled = ?, start_date = ?, end_date = ?, " +
                     "usage_count = ?, max_usage = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, promotion.name);
            pstmt.setString(2, promotion.type);
            pstmt.setDouble(3, promotion.threshold);
            pstmt.setDouble(4, promotion.discount);
            pstmt.setString(5, promotion.description);
            pstmt.setBoolean(6, promotion.enabled);
            pstmt.setTimestamp(7, new Timestamp(promotion.startDate.getTime()));
            pstmt.setTimestamp(8, new Timestamp(promotion.endDate.getTime()));
            pstmt.setInt(9, promotion.usageCount);
            pstmt.setInt(10, promotion.maxUsage);
            pstmt.setInt(11, promotion.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除促销
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM promotions WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 增加促销使用次数
     */
    public static boolean incrementUsage(int id) throws SQLException {
        String sql = "UPDATE promotions SET usage_count = usage_count + 1 WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入促销
     */
    public static void batchInsert(List<Promotion> promotions) throws SQLException {
        String sql = "INSERT INTO promotions (name, type, threshold, discount, description, " +
                     "enabled, start_date, end_date, usage_count, max_usage) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Promotion promotion : promotions) {
                pstmt.setString(1, promotion.name);
                pstmt.setString(2, promotion.type);
                pstmt.setDouble(3, promotion.threshold);
                pstmt.setDouble(4, promotion.discount);
                pstmt.setString(5, promotion.description);
                pstmt.setBoolean(6, promotion.enabled);
                pstmt.setTimestamp(7, new Timestamp(promotion.startDate.getTime()));
                pstmt.setTimestamp(8, new Timestamp(promotion.endDate.getTime()));
                pstmt.setInt(9, promotion.usageCount);
                pstmt.setInt(10, promotion.maxUsage);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 Promotion 对象
     */
    private static Promotion mapRowToPromotion(ResultSet rs) throws SQLException {
        Promotion promotion = new Promotion(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("type"),
            rs.getDouble("threshold"),
            rs.getDouble("discount"),
            rs.getString("description"),
            rs.getBoolean("enabled"),
            rs.getTimestamp("start_date"),
            rs.getTimestamp("end_date"),
            rs.getInt("usage_count"),
            rs.getInt("max_usage")
        );
        return promotion;
    }
}