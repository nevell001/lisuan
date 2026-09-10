package com.cashier.dao;

import com.cashier.model.InventoryStatistics;
import com.cashier.model.PageResult;
import com.cashier.model.Product;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品数据访问对象（重构版）
 * 支持实例方法和分页查询，使用 BaseDAO 通用方法简化代码
 */
public class ProductDAORefactored extends BaseDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ProductDAORefactored.class);

    private static final String SELECT_COLUMNS =
        "id, product_code, name, price, quantity, category, barcode, unit, description, " +
        "brand, supplier, spec, min_stock, cost, version, is_hot";

    // 行映射器（静态复用）
    private static final RowMapper<Product> PRODUCT_MAPPER = new RowMapper<Product>() {
        @Override
        public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
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
            product.isHot = rs.getBoolean("is_hot");
            return product;
        }
    };

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

        String sql = "SELECT " + SELECT_COLUMNS + " FROM products ORDER BY name LIMIT ? OFFSET ?";
        List<Product> products = queryList(sql, PRODUCT_MAPPER, pageSize, offset);

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
        return queryLong("SELECT COUNT(*) FROM products");
    }

    /**
     * 根据ID查找商品
     * @param id 商品ID
     * @return 商品对象，未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public Product findById(int id) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE id = ?";
        return queryOneOrNull(sql, PRODUCT_MAPPER, id);
    }

    /**
     * 根据名称查找商品
     * @param name 商品名称
     * @return 商品对象，未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public Product findByName(String name) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE name = ?";
        return queryOneOrNull(sql, PRODUCT_MAPPER, name);
    }

    /**
     * 根据商品编号查找商品
     * @param productCode 商品编号
     * @return 商品对象，未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public Product findByProductCode(String productCode) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE product_code = ?";
        return queryOneOrNull(sql, PRODUCT_MAPPER, productCode);
    }

    /**
     * 根据条形码查找商品
     * @param barcode 条形码
     * @return 商品对象，未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public Product findByBarcode(String barcode) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE barcode = ?";
        return queryOneOrNull(sql, PRODUCT_MAPPER, barcode);
    }

    /**
     * 根据分类查询商品
     * @param category 分类名称
     * @return 商品列表
     * @throws SQLException 数据库操作异常
     */
    public List<Product> findByCategory(String category) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE category = ? ORDER BY name";
        return queryList(sql, PRODUCT_MAPPER, category);
    }

    /**
     * 根据分类分页查询商品。
     */
    public PageResult<Product> findByCategory(String category, int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;

        long total = queryLong("SELECT COUNT(*) FROM products WHERE category = ?", category);
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE category = ? ORDER BY name LIMIT ? OFFSET ?";
        List<Product> products = queryList(sql, PRODUCT_MAPPER, category, pageSize, offset);

        return new PageResult<>(products, pageNum, pageSize, total);
    }

    /**
     * 使用指定连接根据ID查找商品
     * @param conn 数据库连接
     * @param id 商品ID
     * @return 商品对象
     * @throws SQLException 数据库操作异常
     */
    public Product findByIdWithConnection(Connection conn, int id) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return PRODUCT_MAPPER.mapRow(rs, 1);
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
    public Map<Integer, Product> findByIdsWithConnection(Connection conn, Collection<Integer> ids) throws SQLException {
        Map<Integer, Product> products = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return products;
        }

        String placeholders = String.join(", ", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE id IN (" + placeholders + ")";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer id : ids) {
                pstmt.setInt(index++, id);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                int rowNum = 0;
                while (rs.next()) {
                    Product product = PRODUCT_MAPPER.mapRow(rs, ++rowNum);
                    products.put(product.id, product);
                }
            }
        }
        return products;
    }

    /**
     * 根据商品名称批量查询商品。
     */
    public Map<String, Product> findByNames(Collection<String> names) throws SQLException {
        Map<String, Product> products = new HashMap<>();
        if (names == null || names.isEmpty()) {
            return products;
        }

        List<String> filteredNames = names.stream()
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .toList();
        if (filteredNames.isEmpty()) {
            return products;
        }

        String placeholders = String.join(", ", Collections.nCopies(filteredNames.size(), "?"));
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE name IN (" + placeholders + ")";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < filteredNames.size(); i++) {
                pstmt.setString(i + 1, filteredNames.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                int rowNum = 0;
                while (rs.next()) {
                    Product product = PRODUCT_MAPPER.mapRow(rs, ++rowNum);
                    products.put(product.name, product);
                }
            }
        }
        return products;
    }

    /**
     * 使用指定连接根据商品名称批量查询商品（事务内调用）。
     * @param conn 数据库连接
     * @param names 商品名称集合
     * @return 商品名称到 Product 的映射
     * @throws SQLException 数据库操作异常
     */
    public Map<String, Product> findByNamesWithConnection(Connection conn, Collection<String> names) throws SQLException {
        Map<String, Product> products = new HashMap<>();
        if (names == null || names.isEmpty()) {
            return products;
        }

        List<String> filteredNames = names.stream()
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .toList();
        if (filteredNames.isEmpty()) {
            return products;
        }

        String placeholders = String.join(", ", Collections.nCopies(filteredNames.size(), "?"));
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE name IN (" + placeholders + ")";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < filteredNames.size(); i++) {
                pstmt.setString(i + 1, filteredNames.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                int rowNum = 0;
                while (rs.next()) {
                    Product product = PRODUCT_MAPPER.mapRow(rs, ++rowNum);
                    products.put(product.name, product);
                }
            }
        }
        return products;
    }

    /**
     * 更新商品库存（用于交易）
     * @param id 商品ID
     * @param delta 变化量
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean updateQuantity(int id, int delta) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";
        return executeUpdate(sql, delta, id) > 0;
    }

    /**
     * 更新商品库存（带 Connection，用于事务）
     * @param conn 数据库连接
     * @param id 商品ID
     * @param delta 变化量
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean updateQuantityWithConnection(Connection conn, int id, int delta) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, delta);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 查询低库存商品
     * @return 低库存商品列表
     * @throws SQLException 数据库操作异常
     */
    public List<Product> findLowStock() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE quantity <= min_stock ORDER BY quantity";
        return queryList(sql, PRODUCT_MAPPER);
    }

    /**
     * 分页查询低库存商品。
     */
    public PageResult<Product> findLowStock(int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;

        long total = countLowStock();
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE quantity <= min_stock ORDER BY quantity LIMIT ? OFFSET ?";
        List<Product> products = queryList(sql, PRODUCT_MAPPER, pageSize, offset);

        return new PageResult<>(products, pageNum, pageSize, total);
    }

    public long countLowStock() throws SQLException {
        return queryLong("SELECT COUNT(*) FROM products WHERE quantity <= min_stock");
    }

    /**
     * 查询指定编号前缀下已使用的最大序号（商品编号 = 前缀 + 序号）。
     *
     * <p>按"已用最大序号"而非"当天记录数 +1"生成下一个编号：删除过商品后记录数会回退，
     * 用记录数会重新生成一个已存在的编号，导致保存失败。</p>
     *
     * @param prefix 编号前缀（如 P20260213）
     * @return 已用最大序号；无匹配时返回 0
     * @throws SQLException 数据库操作异常
     */
    public long findMaxProductCodeSequence(String prefix) throws SQLException {
        String sql = "SELECT product_code FROM products WHERE product_code LIKE ?";
        long maxSequence = 0;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, prefix + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    if (code == null || code.length() <= prefix.length()) {
                        continue;
                    }
                    try {
                        maxSequence = Math.max(maxSequence, Long.parseLong(code.substring(prefix.length())));
                    } catch (NumberFormatException e) {
                        // 不符合本规则的历史编号，忽略
                    }
                }
            }
        }
        return maxSequence;
    }

    public Map<String, Long> getInventorySummary() throws SQLException {
        Map<String, Long> summary = new HashMap<>();
        String sql = "SELECT COUNT(*) AS total_count, " +
                     "SUM(CASE WHEN quantity <= 0 THEN 1 ELSE 0 END) AS zero_stock_count, " +
                     "SUM(CASE WHEN quantity > 0 AND quantity < min_stock THEN 1 ELSE 0 END) AS low_stock_count " +
                     "FROM products";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                long total = rs.getLong("total_count");
                long zeroStock = rs.getLong("zero_stock_count");
                long lowStock = rs.getLong("low_stock_count");
                summary.put("totalProducts", total);
                summary.put("zeroStockCount", zeroStock);
                summary.put("lowStockCount", lowStock);
                summary.put("healthyCount", total - zeroStock - lowStock);
            }
        }
        return summary;
    }

    /**
     * 直接在数据库侧计算库存统计，避免为汇总信息加载完整商品列表。
     */
    public InventoryStatistics getInventoryStatistics() throws SQLException {
        String sql = "SELECT COUNT(*) AS total_count, " +
                     "COALESCE(SUM(quantity), 0) AS total_quantity, " +
                     "COALESCE(SUM(cost * quantity), 0) AS total_value, " +
                     "SUM(CASE WHEN quantity <= min_stock THEN 1 ELSE 0 END) AS low_stock_count " +
                     "FROM products";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return new InventoryStatistics(
                    rs.getInt("total_count"),
                    rs.getInt("low_stock_count"),
                    rs.getInt("total_quantity"),
                    rs.getBigDecimal("total_value")
                );
            }
        }
        return new InventoryStatistics(0, 0, 0, BigDecimal.ZERO);
    }

    /**
     * 查询需要发送库存预警的商品，只返回设置了最低库存且低于阈值的记录。
     */
    public List<Product> findProductsRequiringStockAlert() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products " +
                     "WHERE min_stock > 0 AND quantity <= min_stock " +
                     "ORDER BY quantity, name";
        return queryList(sql, PRODUCT_MAPPER);
    }

    /**
     * 搜索商品（支持分页）
     * @param keyword 关键词
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     * @throws SQLException 数据库操作异常
     */
    public PageResult<Product> search(String keyword, int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;

        String searchPattern = "%" + keyword + "%";

        // 统计总数
        String countSql = "SELECT COUNT(*) FROM products WHERE name LIKE ? OR product_code LIKE ? OR barcode LIKE ? OR description LIKE ?";
        long total = queryLong(countSql, searchPattern, searchPattern, searchPattern, searchPattern);

        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products " +
                     "WHERE name LIKE ? OR product_code LIKE ? OR barcode LIKE ? OR description LIKE ? " +
                     "ORDER BY name LIMIT ? OFFSET ?";

        List<Product> products = queryList(sql, PRODUCT_MAPPER,
            searchPattern, searchPattern, searchPattern, searchPattern, pageSize, offset);

        return new PageResult<>(products, pageNum, pageSize, total);
    }

    /**
     * 搜索商品（不分页）
     * @param keyword 关键词
     * @return 商品列表
     * @throws SQLException 数据库操作异常
     */
    public List<Product> search(String keyword) throws SQLException {
        return search(keyword, 1, Integer.MAX_VALUE).getData();
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

        // 检查商品名称是否已存在（v2.4.3 唯一约束）
        if (existsByName(conn, product.name, 0)) {
            throw new SQLException("商品名称已存在，请使用其他名称");
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
        return executeInTransaction(conn -> {
            // 名称唯一性在对外入口校验（v2.4.3）；底层 updateWithConnection 被结账扣减等
            // 热路径复用，不能对每件商品都做一次名称查询。
            if (existsByName(conn, product.name, product.id)) {
                throw new SQLException("商品名称已存在，请使用其他名称");
            }
            return updateWithConnection(conn, product);
        });
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
     * 使用指定连接和乐观锁更新商品（别名，兼容旧代码）
     * @param conn 数据库连接
     * @param product 商品对象
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean updateWithVersionWithConnection(Connection conn, Product product) throws SQLException {
        return updateWithConnection(conn, product);
    }

    /**
     * 使用乐观锁更新商品
     * @param product 商品对象
     * @return 是否成功
     * @throws SQLException 数据库操作异常
     */
    public boolean updateWithVersion(Product product) throws SQLException {
        return update(product);
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

    /**
     * 使用指定连接批量更新商品（含乐观锁）
     * @param conn 数据库连接
     * @param products 商品列表
     * @throws SQLException 数据库操作异常
     */
    public void batchUpdateWithConnection(Connection conn, List<Product> products) throws SQLException {
        if (products == null || products.isEmpty()) {
            return;
        }
        String sql = "UPDATE products SET product_code = ?, name = ?, price = ?, quantity = ?, " +
                     "category = ?, barcode = ?, unit = ?, description = ?, brand = ?, supplier = ?, " +
                     "spec = ?, min_stock = ?, cost = ?, version = version + 1 WHERE id = ? AND version = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Product product : products) {
                setProductParameters(pstmt, product);
                pstmt.setInt(14, product.id);
                pstmt.setInt(15, product.version);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            // 批量更新成功后递增版本号
            for (Product product : products) {
                product.version++;
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

    /**
     * 名称是否已被其它商品占用。
     *
     * @param excludeId 排除的商品 id（更新时传自身 id，新增时传 0）
     */
    private boolean existsByName(Connection conn, String name, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE name = ? AND id <> ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, excludeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
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

    /**
     * 查询手动标记的热销商品
     * @return 热销商品列表
     * @throws SQLException 数据库操作异常
     */
    public List<Product> findHotProducts() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM products WHERE is_hot = 1 ORDER BY name";
        List<Product> products = queryList(sql, PRODUCT_MAPPER);
        logger.info("查询热销商品: SQL={}, 返回{}个", sql, products.size());
        for (Product p : products) {
            logger.info("  热销商品: {} (ID: {}, isHot: {})", p.name, p.id, p.isHot);
        }
        return products;
    }

    /**
     * 查询最近N天销量最高的商品
     * @param days 天数（如7天、30天）
     * @param limit 返回数量限制
     * @return 按销量降序排列的商品列表
     * @throws SQLException 数据库操作异常
     */
    public List<Product> findTopSellingProducts(int days, int limit) throws SQLException {
        // 为带表前缀的列创建别名，避免歧义
        String sql =
            "SELECT p.id, p.product_code, p.name, p.price, p.quantity, p.category, p.barcode, " +
            "p.unit, p.description, p.brand, p.supplier, p.spec, p.min_stock, p.cost, p.version, p.is_hot, " +
            "COALESCE(SUM(ti.quantity), 0) as total_sold " +
            "FROM products p " +
            "LEFT JOIN transaction_items ti ON p.name = ti.product_name " +
            "LEFT JOIN transactions t ON ti.transaction_id = t.transaction_id " +
            "  AND t.timestamp >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
            "GROUP BY p.id, p.product_code, p.name, p.price, p.quantity, p.category, p.barcode, " +
            "p.unit, p.description, p.brand, p.supplier, p.spec, p.min_stock, p.cost, p.version, p.is_hot " +
            "ORDER BY total_sold DESC, p.name " +
            "LIMIT ?";
        return queryList(sql, new RowMapper<Product>() {
            @Override
            public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
                Product product = PRODUCT_MAPPER.mapRow(rs, rowNum);
                return product;
            }
        }, days, limit);
    }

    /**
     * 更新商品的热销标记
     * @param productId 商品ID
     * @param isHot 是否热销
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public boolean updateHotStatus(int productId, boolean isHot) throws SQLException {
        String sql = "UPDATE products SET is_hot = ? WHERE id = ?";
        return executeUpdate(sql, isHot ? 1 : 0, productId) > 0;
    }
}
