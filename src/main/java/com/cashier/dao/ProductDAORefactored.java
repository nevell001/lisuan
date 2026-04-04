package com.cashier.dao;

import com.cashier.model.PageResult;
import com.cashier.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品数据访问对象（重构版）
 * 支持实例方法和分页查询
 */
public class ProductDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS = 
        "id, product_code, name, price, quantity, category, barcode, unit, description, " +
        "brand, supplier, spec, min_stock, cost, version";

    /**
     * 分页查询所有商品
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     * @return 分页结果
     * @throws SQLException 数据库操作异常
     */
    public PageResult<Product> findAll(int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;

        long total = count();
        int offset = (pageNum - 1) * pageSize;

        List<Product> products = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products ORDER BY name LIMIT ? OFFSET ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                products.add(mapRowToProduct(rs));
            }
        }
        return new PageResult<>(products, pageNum, pageSize, total);
    }

    /**
     * 查询所有商品（不分页，兼容旧代码）
     * @return 商品列表
     * @throws SQLException 数据库操作异常
     */
    public List<Product> findAll() throws SQLException {
        return findAll(1, Integer.MAX_VALUE).getData();
    }

    /**
     * 统计商品数量
     * @return 商品总数
     * @throws SQLException 数据库操作异常
     */
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    /**
     * 根据ID查找商品
     * @param id 商品ID
     * @return 商品对象，未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public Product findById(int id) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToProduct(rs);
            }
        }
        return null;
    }

    /**
     * 插入新商品（带事务）
     * @param product 商品对象
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insert(Product product) throws SQLException {
        return executeInTransaction(conn -> insertWithConnection(conn, product));
    }

    /**
     * 使用指定连接插入商品
     * @param conn 数据库连接
     * @param product 商品对象
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insertWithConnection(Connection conn, Product product) throws SQLException {
        validateProduct(product);
        
        // 检查商品编号是否已存在
        if (existsByProductCode(conn, product.productCode)) {
            throw new SQLException("商品编号 '" + product.productCode + "' 已存在");
        }

        String sql = "INSERT INTO products (product_code, name, price, quantity, category, barcode, unit, " +
                     "description, brand, supplier, spec, min_stock, cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setProductParameters(pstmt, product);
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        product.id = rs.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新商品（带事务和乐观锁）
     * @param product 商品对象
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean update(Product product) throws SQLException {
        return executeInTransaction(conn -> updateWithConnection(conn, product));
    }

    /**
     * 使用指定连接更新商品
     * @param conn 数据库连接
     * @param product 商品对象
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean updateWithConnection(Connection conn, Product product) throws SQLException {
        String sql = "UPDATE products SET product_code = ?, name = ?, price = ?, quantity = ?, " +
                     "category = ?, barcode = ?, unit = ?, description = ?, brand = ?, supplier = ?, " +
                     "spec = ?, min_stock = ?, cost = ?, version = version + 1 WHERE id = ? AND version = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setProductParameters(pstmt, product);
            pstmt.setInt(14, product.id);
            pstmt.setInt(15, product.version);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                product.version++;
                return true;
            }
            return false;
        }
    }

    /**
     * 删除商品（带事务）
     * @param id 商品ID
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean delete(int id) throws SQLException {
        return executeInTransaction(conn -> deleteWithConnection(conn, id));
    }

    /**
     * 使用指定连接删除商品
     * @param conn 数据库连接
     * @param id 商品ID
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean deleteWithConnection(Connection conn, int id) throws SQLException {
        String references = getProductReferences(conn, id);
        if (!references.isEmpty()) {
            throw new SQLException("该商品存在以下引用，无法删除：" + references);
        }

        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入商品（带事务）
     * @param products 商品列表
     * @throws SQLException 数据库操作异常
     */
    public void batchInsert(List<Product> products) throws SQLException {
        if (products == null || products.isEmpty()) {
            return;
        }
        executeInTransaction(conn -> {
            batchInsertWithConnection(conn, products);
            return null;
        });
    }

    /**
     * 使用指定连接批量插入商品
     * @param conn 数据库连接
     * @param products 商品列表
     * @throws SQLException 数据库操作异常
     */
    public void batchInsertWithConnection(Connection conn, List<Product> products) throws SQLException {
        String sql = "INSERT INTO products (product_code, name, price, quantity, category, barcode, unit, " +
                     "description, brand, supplier, spec, min_stock, cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Product product : products) {
                setProductParameters(pstmt, product);
                pstmt.addBatch();
            }
            pstmt.executeBatch();

            // 获取生成的ID
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                int index = 0;
                while (rs.next() && index < products.size()) {
                    products.get(index++).id = rs.getInt(1);
                }
            }
        }
    }

    //
    // ==================== 私有辅助方法 ====================

    private void validateProduct(Product product) throws SQLException {
        if (product.name == null || product.name.trim().isEmpty()) {
            throw new SQLException("商品名称不能为空");
        }
        if (product.productCode == null || product.productCode.trim().isEmpty()) {
            throw new SQLException("商品编号不能为空");
        }
    }

    private void setProductParameters(PreparedStatement pstmt, Product product) throws SQLException {
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
    }

    private boolean existsByProductCode(Connection conn, String productCode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE product_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productCode);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private String getProductReferences(Connection conn, int id) throws SQLException {
        StringBuilder references = new StringBuilder();
        String[] tables = {"purchase_order_items", "purchase_inbound_items", "inventory_check_items"};
        String[] names = {"采购订单明细", "采购入库明细", "库存盘点明细"};

        for (int i = 0; i < tables.length; i++) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM " + tables[i] + " WHERE product_id = ?")) {
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    if (references.length() > 0) references.append("、");
                    references.append(names[i]);
                }
            }
        }
        return references.toString();
    }

    private Product mapRowToProduct(ResultSet rs) throws SQLException {
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
