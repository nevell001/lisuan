package com.cashier.dao;

import com.cashier.model.Product;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 商品数据访问对象
 * 负责商品相关的数据库操作
 */
public class ProductDAO {

    /**
     * 查询所有商品
     */
    public static List<Product> findAll() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost FROM products ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapRowToProduct(rs));
            }
        }
        return products;
    }

    /**
     * 根据名称查找商品
     */
    public static Product findByName(String name) throws SQLException {
        String sql = "SELECT name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost FROM products WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToProduct(rs);
            }
        }
        return null;
    }

    /**
     * 根据条形码查找商品
     */
    public static Product findByBarcode(String barcode) throws SQLException {
        String sql = "SELECT name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost FROM products WHERE barcode = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToProduct(rs);
            }
        }
        return null;
    }

    /**
     * 插入新商品
     */
    public static boolean insert(Product product) throws SQLException {
        String sql = "INSERT INTO products (name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.name);
            pstmt.setDouble(2, product.price);
            pstmt.setInt(3, product.quantity);
            pstmt.setString(4, product.category);
            pstmt.setString(5, product.barcode);
            pstmt.setString(6, product.unit);
            pstmt.setString(7, product.description);
            pstmt.setString(8, product.brand);
            pstmt.setString(9, product.supplier);
            pstmt.setString(10, product.spec);
            pstmt.setInt(11, product.minStock);
            pstmt.setDouble(12, product.cost);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新商品
     */
    public static boolean update(Product product) throws SQLException {
        String sql = "UPDATE products SET price = ?, quantity = ?, category = ?, barcode = ?, " +
                     "unit = ?, description = ?, brand = ?, supplier = ?, spec = ?, " +
                     "min_stock = ?, cost = ? WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, product.price);
            pstmt.setInt(2, product.quantity);
            pstmt.setString(3, product.category);
            pstmt.setString(4, product.barcode);
            pstmt.setString(5, product.unit);
            pstmt.setString(6, product.description);
            pstmt.setString(7, product.brand);
            pstmt.setString(8, product.supplier);
            pstmt.setString(9, product.spec);
            pstmt.setInt(10, product.minStock);
            pstmt.setDouble(11, product.cost);
            pstmt.setString(12, product.name);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除商品
     */
    public static boolean delete(String name) throws SQLException {
        String sql = "DELETE FROM products WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新商品库存（用于交易）
     */
    public static boolean updateQuantity(String name, int delta) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ? WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, delta);
            pstmt.setString(2, name);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 查询低库存商品
     */
    public static List<Product> findLowStock() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost FROM products " +
                     "WHERE quantity <= min_stock ORDER BY quantity";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapRowToProduct(rs));
            }
        }
        return products;
    }

    /**
     * 根据分类查询商品
     */
    public static List<Product> findByCategory(String category) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost FROM products " +
                     "WHERE category = ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                products.add(mapRowToProduct(rs));
            }
        }
        return products;
    }

    /**
     * 搜索商品（按名称或条形码）
     */
    public static List<Product> search(String keyword) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost FROM products " +
                     "WHERE name LIKE ? OR barcode LIKE ? OR description LIKE ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                products.add(mapRowToProduct(rs));
            }
        }
        return products;
    }

    /**
     * 批量插入商品
     */
    public static void batchInsert(List<Product> products) throws SQLException {
        String sql = "INSERT INTO products (name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Product product : products) {
                pstmt.setString(1, product.name);
                pstmt.setDouble(2, product.price);
                pstmt.setInt(3, product.quantity);
                pstmt.setString(4, product.category);
                pstmt.setString(5, product.barcode);
                pstmt.setString(6, product.unit);
                pstmt.setString(7, product.description);
                pstmt.setString(8, product.brand);
                pstmt.setString(9, product.supplier);
                pstmt.setString(10, product.spec);
                pstmt.setInt(11, product.minStock);
                pstmt.setDouble(12, product.cost);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 Product 对象
     */
    private static Product mapRowToProduct(ResultSet rs) throws SQLException {
        Product product = new Product(
            rs.getString("name"),
            rs.getDouble("price"),
            rs.getInt("quantity"),
            rs.getString("category")
        );
        product.barcode = rs.getString("barcode");
        product.unit = rs.getString("unit");
        product.description = rs.getString("description");
        product.brand = rs.getString("brand");
        product.supplier = rs.getString("supplier");
        product.spec = rs.getString("spec");
        product.minStock = rs.getInt("min_stock");
        product.cost = rs.getDouble("cost");
        return product;
    }
}
