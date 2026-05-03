package com.cashier.service;

import com.cashier.dao.ProductDAORefactored;
import com.cashier.exception.BusinessException;
import com.cashier.exception.DatabaseException;
import com.cashier.model.PageResult;
import com.cashier.model.Product;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * 商品服务层
 * 提供商品相关的业务逻辑和事务管理
 */
public class ProductService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ProductService.class);
    
    private final ProductDAORefactored productDAO;
    
    public ProductService() {
        this.productDAO = new ProductDAORefactored();
    }
    
    /**
     * 构造方法（支持依赖注入）
     * @param productDAO 商品DAO
     */
    public ProductService(ProductDAORefactored productDAO) {
        this.productDAO = productDAO;
    }

    /**
     * 分页查询商品
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<Product> getProductsByPage(int pageNum, int pageSize) {
        if (pageNum <= 0 || pageSize <= 0) {
            throw BusinessException.validationFailed("pageNum/pageSize", "页码和每页大小必须大于0");
        }
        try {
            return productDAO.findAll(pageNum, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询商品失败", e);
            throw DatabaseException.queryFailed("SELECT products", e);
        }
    }

    /**
     * 获取所有商品
     * @return 商品列表
     */
    public List<Product> getAllProducts() {
        try {
            return productDAO.findAll();
        } catch (SQLException e) {
            logger.error("查询所有商品失败", e);
            throw DatabaseException.queryFailed("SELECT all products", e);
        }
    }

    /**
     * 根据ID获取商品
     * @param id 商品ID
     * @return 商品对象
     */
    public Product getProductById(int id) {
        try {
            Product product = productDAO.findById(id);
            if (product == null) {
                throw BusinessException.productNotFound(String.valueOf(id));
            }
            return product;
        } catch (SQLException e) {
            logger.error("查询商品失败, id={}", id, e);
            throw DatabaseException.queryFailed("SELECT product by id", e);
        }
    }

    /**
     * 创建商品（带事务）
     * @param product 商品对象
     * @return 创建后的商品（包含ID）
     */
    public Product createProduct(Product product) {
        try {
            boolean success = productDAO.insert(product);
            if (!success) {
                throw DatabaseException.insertFailed("products", null);
            }
            logger.info("创建商品成功: {}", product.getName());
            return product;
        } catch (SQLException e) {
            logger.error("创建商品失败", e);
            throw DatabaseException.insertFailed("products", e);
        }
    }

    /**
     * 更新商品（带事务和乐观锁）
     * @param product 商品对象
     * @return 更新后的商品
     */
    public Product updateProduct(Product product) {
        try {
            boolean success = productDAO.update(product);
            if (!success) {
                throw BusinessException.validationFailed("version", "商品已被其他用户修改，请刷新后重试");
            }
            logger.info("更新商品成功: {}", product.getName());
            return product;
        } catch (SQLException e) {
            logger.error("更新商品失败", e);
            throw DatabaseException.updateFailed("products", e);
        }
    }

    /**
     * 删除商品（带事务）
     * @param id 商品ID
     */
    public void deleteProduct(int id) {
        try {
            boolean success = productDAO.delete(id);
            if (!success) {
                throw BusinessException.productNotFound(String.valueOf(id));
            }
            logger.info("删除商品成功: id={}", id);
        } catch (SQLException e) {
            logger.error("删除商品失败, id={}", id, e);
            throw new DatabaseException("删除商品失败", DatabaseException.DbErrorType.DELETE_FAILED, e);
        }
    }

    /**
     * 批量导入商品（带事务）
     * @param products 商品列表
     * @return 成功导入的数量
     */
    public int batchImportProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return 0;
        }
        try {
            productDAO.batchInsert(products);
            logger.info("批量导入商品成功, 数量: {}", products.size());
            return products.size();
        } catch (SQLException e) {
            logger.error("批量导入商品失败", e);
            throw DatabaseException.transactionFailed("batch insert products", e);
        }
    }

    /**
     * 获取商品总数
     * @return 商品数量
     */
    public long getProductCount() {
        try {
            return productDAO.count();
        } catch (SQLException e) {
            logger.error("获取商品数量失败", e);
            throw DatabaseException.queryFailed("SELECT count", e);
        }
    }
}