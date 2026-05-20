package com.cashier.dao;

import com.cashier.model.Product;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 商品数据访问对象（旧版，静态方法）
 * 负责商品相关的数据库操作
 *
 * @deprecated 已被 {@link ProductDAORefactored} 替代，新代码请使用 DAOFactory.getInstance().getProductDAO()
 * 旧版仅保留用于测试兼容，请逐步迁移至 ProductDAORefactored
 */
@Deprecated
public class ProductDAO {

    /**
     * 查询所有商品
     */
    public static List<Product> findAll() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost, version FROM products ORDER BY name";

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
     * 统计商品数量
     */
    public static int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM products";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * 根据ID查找商品
     */
    public static Product findById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return findByIdWithConnection(conn, id);
        }
    }

    /**
     * 使用指定连接根据ID查找商品
     * @param conn 数据库连接
     * @param id 商品ID
     * @return 商品对象，不存在时返回 null
     * @throws SQLException 数据库操作异常
     */
    public static Product findByIdWithConnection(Connection conn, int id) throws SQLException {
        String sql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost, version FROM products WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProduct(rs);
                }
            }
        }
        return null;
    }

    /**
     * 使用指定连接批量查询商品
     * @param conn 数据库连接
     * @param ids 商品ID集合
     * @return 商品映射（商品ID -> 商品对象）
     * @throws SQLException 数据库操作异常
     */
    public static Map<Integer, Product> findByIdsWithConnection(Connection conn, Collection<Integer> ids) throws SQLException {
        Map<Integer, Product> products = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return products;
        }

        String placeholders = String.join(", ", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost, version FROM products WHERE id IN (" + placeholders + ")";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer id : ids) {
                pstmt.setInt(index++, id);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Product product = mapRowToProduct(rs);
                    products.put(product.id, product);
                }
            }
        }
        return products;
    }

    /**
     * 根据名称查找商品
     */
    public static Product findByName(String name) throws SQLException {
        String sql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost, version FROM products WHERE name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProduct(rs);
                }
            }
        }
        return null;
    }

    /**
     * 根据商品编号查找商品
     */
    public static Product findByProductCode(String productCode) throws SQLException {
        String sql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost, version FROM products WHERE product_code = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, productCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProduct(rs);
                }
            }
        }
        return null;
    }

    /**
     * 根据条形码查找商品
     */
    public static Product findByBarcode(String barcode) throws SQLException {
        String sql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost, version FROM products WHERE barcode = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProduct(rs);
                }
            }
        }
        return null;
    }

    /**
     * 插入新商品
     * 如果商品ID大于0，则使用指定的ID；否则由数据库自动生成ID
     */
    public static boolean insert(Product product) throws SQLException {
        // 验证必填字段
        if (product.name == null || product.name.trim().isEmpty()) {
            throw new SQLException("商品名称不能为空");
        }

        // 验证商品编号
        if (product.productCode == null || product.productCode.trim().isEmpty()) {
            throw new SQLException("商品编号不能为空");
        }

        // 检查商品编号是否已存在
        Product existingProduct = findByProductCode(product.productCode);
        if (existingProduct != null) {
            throw new SQLException("商品编号 '" + product.productCode + "' 已存在，请使用其他编号");
        }

        String sql;
        boolean useProvidedId = product.id > 0;

        if (useProvidedId) {
            // 使用用户提供的ID
            sql = "INSERT INTO products (id, product_code, name, price, quantity, category, barcode, unit, description, " +
                  "brand, supplier, spec, min_stock, cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            // 由数据库自动生成ID
            sql = "INSERT INTO products (product_code, name, price, quantity, category, barcode, unit, description, " +
                  "brand, supplier, spec, min_stock, cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (useProvidedId) {
                pstmt.setInt(paramIndex++, product.id);
            }

            pstmt.setString(paramIndex++, product.productCode);
            pstmt.setString(paramIndex++, product.name);
            pstmt.setBigDecimal(paramIndex++, product.price);
            pstmt.setInt(paramIndex++, product.quantity);
            pstmt.setString(paramIndex++, product.category);
            pstmt.setString(paramIndex++, product.barcode);
            pstmt.setString(paramIndex++, product.unit);
            pstmt.setString(paramIndex++, product.description);
            pstmt.setString(paramIndex++, product.brand);
            pstmt.setString(paramIndex++, product.supplier);
            pstmt.setString(paramIndex++, product.spec);
            pstmt.setInt(paramIndex++, product.minStock);
            pstmt.setBigDecimal(paramIndex++, product.cost);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0 && !useProvidedId) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        product.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新商品
     */
    public static boolean update(Product product) throws SQLException {
        String sql = "UPDATE products SET product_code = ?, name = ?, price = ?, quantity = ?, category = ?, barcode = ?, " +
                     "unit = ?, description = ?, brand = ?, supplier = ?, spec = ?, " +
                     "min_stock = ?, cost = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.productCode);
            pstmt.setString(2, product.name);
            pstmt.setBigDecimal(3, product.price);
            pstmt.setInt(4, product.quantity);
            pstmt.setString(5, product.category);
            pstmt.setString(6, product.barcode);
            pstmt.setString(7, product.unit);
            pstmt.setString(8, product.description);
            pstmt.setString(9, product.brand);
            pstmt.setString(10, product.supplier);
            pstmt.setString(11, product.spec);
            pstmt.setInt(12, product.minStock);
            pstmt.setBigDecimal(13, product.cost);
            pstmt.setInt(14, product.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 使用乐观锁更新商品
     * @param product 商品对象
     * @return 如果更新成功返回true，否则返回false
     * @throws SQLException 数据库操作异常
     */
    public static boolean updateWithVersion(Product product) throws SQLException {
        String sql = "UPDATE products SET product_code = ?, name = ?, price = ?, quantity = ?, category = ?, barcode = ?, " +
                     "unit = ?, description = ?, brand = ?, supplier = ?, spec = ?, " +
                     "min_stock = ?, cost = ?, version = version + 1 WHERE id = ? AND version = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.productCode);
            pstmt.setString(2, product.name);
            pstmt.setBigDecimal(3, product.price);
            pstmt.setInt(4, product.quantity);
            pstmt.setString(5, product.category);
            pstmt.setString(6, product.barcode);
            pstmt.setString(7, product.unit);
            pstmt.setString(8, product.description);
            pstmt.setString(9, product.brand);
            pstmt.setString(10, product.supplier);
            pstmt.setString(11, product.spec);
            pstmt.setInt(12, product.minStock);
            pstmt.setBigDecimal(13, product.cost);
            pstmt.setInt(14, product.id);
            pstmt.setInt(15, product.version);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                product.version++; // 更新成功，增加版本号
                return true;
            }
            return false; // 更新失败，版本号不匹配
        }
    }

    /**
     * 使用指定的数据库连接和乐观锁更新商品
     * @param conn 数据库连接
     * @param product 商品对象
     * @return 如果更新成功返回true，否则返回false
     * @throws SQLException 数据库操作异常
     */
    public static boolean updateWithVersionWithConnection(Connection conn, Product product) throws SQLException {
        String sql = "UPDATE products SET product_code = ?, name = ?, price = ?, quantity = ?, category = ?, barcode = ?, " +
                     "unit = ?, description = ?, brand = ?, supplier = ?, spec = ?, " +
                     "min_stock = ?, cost = ?, version = version + 1 WHERE id = ? AND version = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.productCode);
            pstmt.setString(2, product.name);
            pstmt.setBigDecimal(3, product.price);
            pstmt.setInt(4, product.quantity);
            pstmt.setString(5, product.category);
            pstmt.setString(6, product.barcode);
            pstmt.setString(7, product.unit);
            pstmt.setString(8, product.description);
            pstmt.setString(9, product.brand);
            pstmt.setString(10, product.supplier);
            pstmt.setString(11, product.spec);
            pstmt.setInt(12, product.minStock);
            pstmt.setBigDecimal(13, product.cost);
            pstmt.setInt(14, product.id);
            pstmt.setInt(15, product.version);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                product.version++; // 更新成功，增加版本号
                return true;
            }
            return false; // 更新失败，版本号不匹配
        }
    }

    /**
     * 删除商品
     */
    public static boolean delete(int id) throws SQLException {
        // 检查是否有采购订单明细、入库明细、盘点明细引用此商品
        String references = getProductReferences(id);
        if (!references.isEmpty()) {
            throw new SQLException("该商品存在以下引用，无法删除：" + references);
        }

        String sql = "DELETE FROM products WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据名称删除商品（兼容旧代码）
     */
    public static boolean deleteByName(String name) throws SQLException {
        // 先查找ID
        Product product = findByName(name);
        if (product == null) {
            return false;
        }
        return delete(product.id);
    }

    /**
     * 获取商品的所有引用
     * @param id 商品ID
     * @return 引用信息列表
     * @throws SQLException 数据库操作异常
     */
    public static String getProductReferences(int id) throws SQLException {
        StringBuilder references = new StringBuilder();

        // 检查采购订单明细
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM purchase_order_items WHERE product_id = ?")) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    if (references.length() > 0) {
                        references.append("、");
                    }
                    references.append("采购订单明细");
                }
            }
        }

        // 检查采购入库明细
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM purchase_inbound_items WHERE product_id = ?")) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    if (references.length() > 0) {
                        references.append("、");
                    }
                    references.append("采购入库明细");
                }
            }
        }

        // 检查库存盘点明细
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM inventory_check_items WHERE product_id = ?")) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    if (references.length() > 0) {
                        references.append("、");
                    }
                    references.append("库存盘点明细");
                }
            }
        }

        return references.toString();
    }

    /**
     * 更新商品库存（用于交易）
     */
    public static boolean updateQuantity(int id, int delta) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, delta);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新商品库存（带 Connection，用于事务）
     */
    public static boolean updateQuantityWithConnection(Connection conn, int id, int delta) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, delta);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据名称更新商品库存（兼容旧代码）
     */
    public static boolean updateQuantityByName(String name, int delta) throws SQLException {
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
        String sql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost, version FROM products " +
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
        String sql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost, version FROM products " +
                     "WHERE category = ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRowToProduct(rs));
                }
            }
        }
        return products;
    }

    /**
     * 搜索商品（按名称、商品编号或条形码）
     */
    public static List<Product> search(String keyword) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost, version FROM products " +
                     "WHERE name LIKE ? OR product_code LIKE ? OR barcode LIKE ? OR description LIKE ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapRowToProduct(rs));
                }
            }
        }
        return products;
    }

    /**
     * 批量插入商品
     */
    public static void batchInsert(List<Product> products) throws SQLException {
        if (products == null || products.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO products (product_code, name, price, quantity, category, barcode, unit, description, " +
                     "brand, supplier, spec, min_stock, cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            for (Product product : products) {
                pstmt.setString(1, product.productCode);
                pstmt.setString(2, product.name);
                pstmt.setBigDecimal(3, product.price);
                pstmt.setInt(4, product.quantity);
                pstmt.setString(5, product.category);
                pstmt.setString(6, product.barcode);
                pstmt.setString(7, product.unit);
                pstmt.setString(8, product.description);
                pstmt.setString(9, product.brand);
                pstmt.setString(10, product.supplier);
                pstmt.setString(11, product.spec);
                pstmt.setInt(12, product.minStock);
                pstmt.setBigDecimal(13, product.cost);
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();

            // 获取生成的自增ID
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                int index = 0;
                while (rs.next()) {
                    if (index < products.size()) {
                        products.get(index).id = rs.getInt(1);
                        index++;
                    }
                }
            }
        }
    }

    /**
     * 将 ResultSet 映射为 Product 对象
     */
    private static Product mapRowToProduct(ResultSet rs) throws SQLException {
        Product product = new Product(
            rs.getInt("id"),
            rs.getString("product_code"),
            rs.getString("name"),
            rs.getBigDecimal("price"),
            rs.getInt("quantity"),
            rs.getString("category"),
            rs.getString("barcode"),
            rs.getString("unit"),
            rs.getString("description"),
            rs.getString("brand"),
            rs.getString("supplier"),
            rs.getString("spec"),
            rs.getInt("min_stock"),
            rs.getBigDecimal("cost")
        );
        product.version = rs.getInt("version");
        return product;
    }
}
