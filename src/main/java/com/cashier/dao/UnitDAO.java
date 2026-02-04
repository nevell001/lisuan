package com.cashier.dao;

import com.cashier.model.Unit;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 单位数据访问对象
 * 负责单位相关的数据库操作
 */
public class UnitDAO {

    /**
     * 查询所有单位
     */
    public static List<Unit> findAll() throws SQLException {
        List<Unit> units = new ArrayList<>();
        String sql = "SELECT id, name, description FROM units ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                units.add(mapRowToUnit(rs));
            }
        }
        return units;
    }

    /**
     * 根据ID查找单位
     */
    public static Unit findById(int id) throws SQLException {
        String sql = "SELECT id, name, description FROM units WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToUnit(rs);
            }
        }
        return null;
    }

    /**
     * 根据名称查找单位
     */
    public static Unit findByName(String name) throws SQLException {
        String sql = "SELECT id, name, description FROM units WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToUnit(rs);
            }
        }
        return null;
    }

    /**
     * 插入新单位
     */
    public static boolean insert(Unit unit) throws SQLException {
        String sql = "INSERT INTO units (name, description) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, unit.name);
            pstmt.setString(2, unit.description);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        unit.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新单位
     */
    public static boolean update(Unit unit) throws SQLException {
        String sql = "UPDATE units SET name = ?, description = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, unit.name);
            pstmt.setString(2, unit.description);
            pstmt.setInt(3, unit.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除单位
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM units WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据名称删除单位（兼容旧代码）
     */
    public static boolean deleteByName(String name) throws SQLException {
        String sql = "DELETE FROM units WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 检查单位是否存在
     */
    public static boolean exists(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM units WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * 批量插入单位
     */
    public static void batchInsert(List<Unit> units) throws SQLException {
        String sql = "INSERT INTO units (name, description) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Unit unit : units) {
                pstmt.setString(1, unit.name);
                pstmt.setString(2, unit.description);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 Unit 对象
     */
    private static Unit mapRowToUnit(ResultSet rs) throws SQLException {
        return new Unit(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description")
        );
    }
}