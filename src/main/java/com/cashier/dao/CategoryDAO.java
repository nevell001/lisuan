package com.cashier.dao;

import com.cashier.model.Category;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 分类数据访问对象
 * 负责分类相关的数据库操作
 */
public class CategoryDAO {

    /**
     * 查询所有分类
     */
    public static List<Category> findAll() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name, description FROM categories ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(mapRowToCategory(rs));
            }
        }
        return categories;
    }

    /**
     * 根据ID查找分类
     */
    public static Category findById(int id) throws SQLException {
        String sql = "SELECT id, name, description FROM categories WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToCategory(rs);
            }
        }
        return null;
    }

    /**
     * 根据名称查找分类
     */
    public static Category findByName(String name) throws SQLException {
        String sql = "SELECT id, name, description FROM categories WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToCategory(rs);
            }
        }
        return null;
    }

    /**
     * 插入新分类
     */
    public static boolean insert(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, category.name);
            pstmt.setString(2, category.description);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        category.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新分类
     */
    public static boolean update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.name);
            pstmt.setString(2, category.description);
            pstmt.setInt(3, category.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除分类
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据名称删除分类（兼容旧代码）
     */
    public static boolean deleteByName(String name) throws SQLException {
        String sql = "DELETE FROM categories WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 检查分类是否存在
     */
    public static boolean exists(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ?";

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
     * 批量插入分类
     */
    public static void batchInsert(List<Category> categories) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Category category : categories) {
                pstmt.setString(1, category.name);
                pstmt.setString(2, category.description);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 Category 对象
     */
    private static Category mapRowToCategory(ResultSet rs) throws SQLException {
        return new Category(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description")
        );
    }
}