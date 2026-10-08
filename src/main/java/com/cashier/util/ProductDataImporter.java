package com.cashier.util;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.Category;
import com.cashier.model.Product;
import com.cashier.model.Unit;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 商品数据导入工具类
 * 支持从多种来源导入商品数据到 MySQL 数据库
 */
public class ProductDataImporter {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ProductDataImporter.class);
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    private static final String DEFAULT_CATEGORY = "默认分类";
    private static final String DEFAULT_UNIT = "个";
    private static final String BASIC_DATA_IMPORT_DESCRIPTION = "从基础数据导入";
    private static final List<CategoryKeywords> NAME_CATEGORY_KEYWORDS = List.of(
        new CategoryKeywords("药品类", List.of(
            "胶囊", "片", "丸", "注射液", "颗粒", "口服液", "糖浆", "酊", "栓", "软膏",
            "乳膏", "凝胶", "贴剂", "感冒", "止咳", "退烧", "止痛", "消炎"
        )),
        new CategoryKeywords("食品类", List.of(
            "饼干", "薯片", "糖果", "巧克力", "饮料", "牛奶", "酸奶", "啤酒", "白酒",
            "红酒", "米", "面", "油", "盐", "酱", "醋", "调味", "肉", "蛋", "菜",
            "水果", "蔬菜", "熟食", "罐头", "方便面", "速冻", "冷冻", "零食", "坚果",
            "果脯", "月饼", "汤圆", "粽子"
        )),
        new CategoryKeywords("日用百货类", List.of(
            "牙膏", "牙刷", "洗发水", "沐浴露", "香皂", "洗衣液", "洗洁精", "洗衣粉",
            "柔顺剂", "消毒液", "卫生纸", "纸巾", "湿巾", "纸尿裤", "卫生巾", "护垫",
            "面膜", "爽肤水", "乳液", "面霜", "眼霜", "精华", "防晒", "粉底", "口红",
            "睫毛膏", "眼影", "指甲油", "香水", "洗护"
        )),
        new CategoryKeywords("保健品类", List.of(
            "人参", "阿胶", "燕窝", "枸杞", "冬虫夏草", "红枣", "西洋参", "钙片",
            "维生素", "蛋白粉", "鱼油", "卵磷脂", "氨糖", "软骨素", "褪黑素", "叶酸",
            "胶原蛋白", "酵素", "麦片", "燕麦片"
        ))
    );
    private static final List<CategoryKeywords> SUPPLIER_CATEGORY_KEYWORDS = List.of(
        new CategoryKeywords("药品类", List.of("制药", "医药", "药业")),
        new CategoryKeywords("食品类", List.of("食品", "粮油", "饮料", "乳业", "糖酒", "茶")),
        new CategoryKeywords("日用百货类", List.of("日化", "化妆", "洗涤", "清洁", "生活", "家居")),
        new CategoryKeywords("保健品类", List.of("保健", "营养", "生物", "健康", "养生"))
    );
    private static final Map<String, String> UNIT_ALIASES = Map.ofEntries(
        Map.entry("g", "克"),
        Map.entry("克", "克"),
        Map.entry("kg", "千克"),
        Map.entry("千克", "千克"),
        Map.entry("公斤", "千克"),
        Map.entry("ml", "毫升"),
        Map.entry("毫升", "毫升"),
        Map.entry("l", "升"),
        Map.entry("L", "升"),
        Map.entry("升", "升"),
        Map.entry("piece", "个"),
        Map.entry("PCS", "个"),
        Map.entry("pc", "个"),
        Map.entry("box", "盒"),
        Map.entry("盒", "盒"),
        Map.entry("bottle", "瓶"),
        Map.entry("瓶", "瓶"),
        Map.entry("bag", "袋"),
        Map.entry("袋", "袋"),
        Map.entry("package", "包"),
        Map.entry("包", "包"),
        Map.entry("set", "套"),
        Map.entry("套", "套"),
        Map.entry("pair", "对"),
        Map.entry("对", "对"),
        Map.entry("tin", "听"),
        Map.entry("听", "听"),
        Map.entry("can", "罐"),
        Map.entry("罐", "罐")
    );
    
    // 统计信息
    private int totalProcessed = 0;
    private int successCount = 0;
    private int skippedCount = 0;
    private int errorCount = 0;
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_TOTAL_PROCESSED = "totalProcessed";
    private static final String KEY_SUCCESS_COUNT = "successCount";
    private static final String KEY_SKIPPED_COUNT = "skippedCount";
    private static final String KEY_ERROR_COUNT = "errorCount";

    private record CategoryKeywords(String category, List<String> keywords) {
    }
    
    /**
     * 从 CSV 文件导入商品数据
     * @param filePath CSV 文件路径
     * @return 导入结果统计
     */
    public Map<String, Object> importFromCSV(String filePath) {
        logger.info("开始从 CSV 文件导入商品数据: {}", filePath);
        
        Map<String, Object> result = new HashMap<>();
        List<String> messages = new ArrayList<>();
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                result.put("success", false);
                result.put("error", "文件不存在: " + filePath);
                return result;
            }
            
            // 预先加载分类和单位映射
            Map<String, Category> categoryMap = loadCategoryMap();
            Map<String, Unit> unitMap = loadUnitMap();
            
            // 解析CSV文件
            List<Product> products = parseCSVFile(file, categoryMap, unitMap);
            
            if (!products.isEmpty()) {
                logger.info("成功解析 {} 条商品数据", products.size());
                
                // 创建缺失的分类和单位
                int categoriesCreated = ensureCategoriesExist(products, categoryMap);
                int unitsCreated = ensureUnitsExist(products, unitMap);
                
                if (categoriesCreated > 0) {
                    messages.add(String.format("创建了 %d 个新分类", categoriesCreated));
                }
                if (unitsCreated > 0) {
                    messages.add(String.format("创建了 %d 个新单位", unitsCreated));
                }
                
                int inserted = insertProducts(products);
                messages.add(String.format("成功导入 %d 条商品", inserted));
            } else {
                messages.add("无数据");
            }
            
            result.put(KEY_SUCCESS, true);
            result.put(KEY_TOTAL_PROCESSED, totalProcessed);
            result.put(KEY_SUCCESS_COUNT, successCount);
            result.put(KEY_SKIPPED_COUNT, skippedCount);
            result.put(KEY_ERROR_COUNT, errorCount);
            result.put(KEY_MESSAGES, messages);
            
            logger.info("CSV 文件导入完成 - 处理: {}, 成功: {}, 跳过: {}, 错误: {}", 
                totalProcessed, successCount, skippedCount, errorCount);
            
        } catch (Exception e) {
            logger.error("CSV 文件导入失败", e);
            result.put(KEY_SUCCESS, false);
            result.put(KEY_ERROR, e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 解析 CSV 文件
     * 自动检测文件格式（逗号分隔或 | 分隔）
     */
    private List<Product> parseCSVFile(File file, Map<String, Category> categoryMap, Map<String, Unit> unitMap) 
            throws Exception {
        
        List<Product> products = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            
            String line;
            int lineNum = 0;
            
            // 读取第一行来检测格式
            String firstLine = null;
            boolean isCommaSeparated = false;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                
                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // 检测格式
                if (firstLine == null) {
                    firstLine = line;
                    // 检测是否是逗号分隔的CSV（EricLiuCN/barcode格式）
                    if (line.contains(",") && line.contains("\"")) {
                        isCommaSeparated = true;
                        logger.info("检测到CSV格式（逗号分隔）");
                    } else if (line.contains("|")) {
                        isCommaSeparated = false;
                        logger.info("检测到自定义格式（| 分隔）");
                    } else {
                        // 默认使用逗号分隔
                        isCommaSeparated = true;
                        logger.info("默认使用CSV格式（逗号分隔）");
                    }
                    continue; // 跳过表头
                }
                
                try {
                    Product product;
                    if (isCommaSeparated) {
                        product = parseCSVCommaLine(line, categoryMap, unitMap);
                    } else {
                        product = parseProductLine(line, categoryMap, unitMap);
                    }
                    
                    if (product != null) {
                        products.add(product);
                    }
                } catch (Exception e) {
                    logger.warn("解析第 {} 行失败: {}", lineNum, e.getMessage());
                }
                
                // 每10000行输出一次进度
                if (lineNum % 10000 == 0) {
                    logger.info("已处理 {} 行，当前商品数: {}", lineNum, products.size());
                }
            }
        }
        
        logger.info("CSV文件解析完成，共解析 {} 条商品", products.size());
        return products;
    }
    
    /**
     * 解析商品行
     * 支持 CSV 格式: 条码,商品名,价格,单位,分类,品牌,规格,厂商
     */
    private Product parseProductLine(String line, Map<String, Category> categoryMap, Map<String, Unit> unitMap) {
        String[] parts = line.split("\\|"); // 使用 | 作为分隔符

        if (parts.length < 2) {
            return null;
        }

        String barcode = parts[0].trim();
        String name = parts[1].trim();

        // 跳过无效数据
        if (barcode.isEmpty() || name.isEmpty()) {
            return null;
        }

        Product product = new Product();
        product.barcode = barcode;
        product.name = name;
        product.productCode = barcode;  // 商品编号默认使用条码

        product.price = partValue(parts, 2)
            .map(value -> parseBigDecimal(value, BigDecimal.ZERO))
            .orElse(product.price);
        product.unit = partValue(parts, 3).map(this::normalizeUnit).orElse(DEFAULT_UNIT);
        product.category = partValue(parts, 4).orElse(DEFAULT_CATEGORY);
        product.brand = trimmedPart(parts, 5).orElse(product.brand);
        product.spec = trimmedPart(parts, 6).orElse(product.spec);
        product.supplier = trimmedPart(parts, 7).orElse(product.supplier);

        // 如果分类为默认分类，尝试根据供应商和商品名称自动分类
        autoClassifyDefaultCategory(product);

        // 设置默认值（注意：库存数量设为 0，不调整库存）
        applyDefaultImportFields(product, BASIC_DATA_IMPORT_DESCRIPTION);

        return product;
    }

    /**
     * 根据商品名称自动分类
     */
    private String classifyByName(String name) {
        if (name == null || name.isEmpty()) {
            return DEFAULT_CATEGORY;
        }

        return matchCategory(name, NAME_CATEGORY_KEYWORDS);
    }
    
    /**
     * 解析CSV格式的商品行（逗号分隔，来自EricLiuCN/barcode）
     * 格式: id,barcode,name,spec,unit,price,brand,supplier,made_in,created_at,updated_at,deleted_at
     */
    private Product parseCSVCommaLine(String line, Map<String, Category> categoryMap, Map<String, Unit> unitMap) {
        line = line.replaceAll("\"", "");
        String[] parts = line.split(",");

        if (parts.length < 3) {
            return null;
        }

        String barcode = parts[1].trim();
        String name = parts[2].trim();

        // 跳过无效数据
        if (barcode.isEmpty() || name.isEmpty() || barcode.equals("NULL")) {
            return null;
        }

        Product product = new Product();
        product.barcode = barcode;
        product.name = name;
        product.productCode = barcode; // 使用条码作为商品编号

        product.spec = csvValue(parts, 3).orElse(product.spec);
        product.unit = csvValue(parts, 4).map(this::normalizeUnit).orElse(DEFAULT_UNIT);
        product.price = csvValue(parts, 5)
            .map(value -> parseBigDecimal(value, BigDecimal.ZERO))
            .orElse(product.price);
        product.brand = csvValue(parts, 6).orElse(product.brand);
        product.supplier = csvValue(parts, 7).orElse(product.supplier);

        // 根据供应商自动分类
        if (product.supplier != null && !product.supplier.isEmpty()) {
            product.category = classifyBySupplier(product.supplier);
        } else {
            product.category = DEFAULT_CATEGORY;
        }

        // 设置默认值（注意：库存数量设为 0，不调整库存）
        applyDefaultImportFields(product, "从EricLiuCN/barcode导入");

        return product;
    }

    private void applyDefaultImportFields(Product product, String description) {
        product.quantity = 0;  // 导入时不设置库存数量
        product.minStock = 10;
        product.cost = product.getPrice().compareTo(BigDecimal.ZERO) > 0
            ? product.getPrice().multiply(Product.DEFAULT_COST_RATE)
            : BigDecimal.ZERO;
        product.description = description;
    }

    private void autoClassifyDefaultCategory(Product product) {
        if (!DEFAULT_CATEGORY.equals(product.category)) {
            return;
        }
        if (product.supplier != null && !product.supplier.isEmpty()) {
            product.category = classifyBySupplier(product.supplier);
            return;
        }
        if (product.name != null && !product.name.isEmpty()) {
            product.category = classifyByName(product.name);
        }
    }

    private Optional<String> partValue(String[] parts, int index) {
        return trimmedPart(parts, index).filter(value -> !value.isEmpty());
    }

    private Optional<String> trimmedPart(String[] parts, int index) {
        if (parts.length <= index) {
            return Optional.empty();
        }
        return Optional.of(parts[index].trim());
    }

    private Optional<String> csvValue(String[] parts, int index) {
        if (parts.length <= index) {
            return Optional.empty();
        }
        String value = parts[index].trim();
        return value.isEmpty() || "NULL".equals(value) ? Optional.empty() : Optional.of(value);
    }

    /**
     * 根据供应商名称自动分类
     */
    private String classifyBySupplier(String supplier) {
        if (supplier == null || supplier.isEmpty()) {
            return DEFAULT_CATEGORY;
        }

        return matchCategory(supplier, SUPPLIER_CATEGORY_KEYWORDS);
    }

    private String matchCategory(String value, List<CategoryKeywords> categoryKeywords) {
        return categoryKeywords.stream()
            .filter(rule -> rule.keywords().stream().anyMatch(value::contains))
            .map(CategoryKeywords::category)
            .findFirst()
            .orElse(DEFAULT_CATEGORY);
    }

    /**
     * 标准化单位名称
     */
    private String normalizeUnit(String unitName) {
        unitName = unitName.trim();
        return UNIT_ALIASES.getOrDefault(unitName, unitName);
    }
    
    /**
     * 解析 BigDecimal 数值
     */
    private BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.trim().isEmpty() || value.trim().equals("NULL")) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 插入商品到数据库
     */
    private int insertProducts(List<Product> products) {
        int inserted = 0;
        
        for (Product product : products) {
            totalProcessed++;
            
            try {
                // 检查条码是否已存在
                Product existing = productDAO.findByBarcode(product.barcode);
                
                if (existing != null) {
                    // 已存在，跳过
                    skippedCount++;
                    logger.debug("跳过已存在的商品: {} (条码: {})", product.name, product.barcode);
                } else {
                    // 不存在，插入
                    boolean success = productDAO.insert(product);
                    if (success) {
                        successCount++;
                        inserted++;
                        logger.debug("成功导入商品: {} (条码: {})", product.name, product.barcode);
                    } else {
                        errorCount++;
                        logger.warn("插入商品失败: {}", product.name);
                    }
                }
            } catch (SQLException e) {
                errorCount++;
                logger.error("处理商品失败: {}", product.name, e);
            }
        }
        
        return inserted;
    }
    
    /**
     * 加载分类映射
     */
    private Map<String, Category> loadCategoryMap() {
        Map<String, Category> categoryMap = new HashMap<>();
        
        try {
            List<Category> categories = DAOFactory.getInstance().getCategoryDAO().findAll();
            for (Category category : categories) {
                categoryMap.put(category.name, category);
            }
            
            // 确保至少有默认分类
            if (!categoryMap.containsKey("默认分类")) {
                Category defaultCategory = new Category("默认分类", "默认商品分类");
                DAOFactory.getInstance().getCategoryDAO().insert(defaultCategory);
                categoryMap.put(defaultCategory.name, defaultCategory);
            }
            
        } catch (SQLException e) {
            logger.error("加载分类失败", e);
        }
        
        return categoryMap;
    }
    
    /**
     * 加载单位映射
     */
    private Map<String, Unit> loadUnitMap() {
        Map<String, Unit> unitMap = new HashMap<>();
        
        try {
            List<Unit> units = DAOFactory.getInstance().getUnitDAO().findAll();
            for (Unit unit : units) {
                unitMap.put(unit.name, unit);
            }
            
            // 确保至少有默认单位
            if (!unitMap.containsKey("个")) {
                Unit defaultUnit = new Unit("个", "默认单位");
                DAOFactory.getInstance().getUnitDAO().insert(defaultUnit);
                unitMap.put(defaultUnit.name, defaultUnit);
            }
            
        } catch (SQLException e) {
            logger.error("加载单位失败", e);
        }
        
        return unitMap;
    }
    
    /**
     * 确保所有分类都存在
     * @param products 商品列表
     * @param categoryMap 分类映射
     * @return 创建的分类数量
     */
    private int ensureCategoriesExist(List<Product> products, Map<String, Category> categoryMap) {
        int createdCount = 0;
        
        for (Product product : products) {
            if (product.category != null && !product.category.isEmpty()) {
                if (!categoryMap.containsKey(product.category)) {
                    try {
                        Category newCategory = new Category(product.category, "导入商品创建");
                        DAOFactory.getInstance().getCategoryDAO().insert(newCategory);
                        categoryMap.put(newCategory.name, newCategory);
                        createdCount++;
                        logger.debug("创建新分类: {}", product.category);
                    } catch (SQLException e) {
                        logger.error("创建分类失败: {}", product.category, e);
                    }
                }
            }
        }
        
        return createdCount;
    }
    
    /**
     * 确保所有单位都存在
     * @param products 商品列表
     * @param unitMap 单位映射
     * @return 创建的单位数量
     */
    private int ensureUnitsExist(List<Product> products, Map<String, Unit> unitMap) {
        int createdCount = 0;
        
        for (Product product : products) {
            if (product.unit != null && !product.unit.isEmpty()) {
                if (!unitMap.containsKey(product.unit)) {
                    try {
                        Unit newUnit = new Unit(product.unit, "导入商品创建");
                        DAOFactory.getInstance().getUnitDAO().insert(newUnit);
                        unitMap.put(newUnit.name, newUnit);
                        createdCount++;
                        logger.debug("创建新单位: {}", product.unit);
                    } catch (SQLException e) {
                        logger.error("创建单位失败: {}", product.unit, e);
                    }
                }
            }
        }
        
        return createdCount;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalProcessed = 0;
        successCount = 0;
        skippedCount = 0;
        errorCount = 0;
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalProcessed", totalProcessed);
        stats.put("successCount", successCount);
        stats.put("skippedCount", skippedCount);
        stats.put("errorCount", errorCount);
        return stats;
    }
}
