package com.cashier.util;

import com.cashier.dao.CategoryDAO;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.dao.UnitDAO;
import com.cashier.model.Category;
import com.cashier.model.Product;
import com.cashier.model.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品数据导入工具类
 * 支持从多种来源导入商品数据到 MySQL 数据库
 */
public class ProductDataImporter {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ProductDataImporter.class);
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    
    // GitHub 商品条码库 URL
    private static final String GITHUB_BARCODE_URL = "https://raw.githubusercontent.com/EricLiuCN/barcode/master/";
    
    // 数据文件列表（注意：仓库中的文件是压缩格式，需要处理）
    private static final String[] DATA_FILES = {
        "barcodes.csv.zip",  // 商品条码数据（压缩格式）
        "medicine_info.zip"   // 药品条码数据（压缩格式）
    };
    
    // 统计信息
    private int totalProcessed = 0;
    private int successCount = 0;
    private int skippedCount = 0;
    private int errorCount = 0;
    
    /**
     * 从 GitHub 导入商品数据
     * @return 导入结果统计
     */
    public Map<String, Object> importFromGitHub() {
        logger.info("开始从 GitHub 导入商品数据...");
        
        Map<String, Object> result = new HashMap<>();
        List<String> messages = new ArrayList<>();
        
        try {
            // 预先加载分类和单位映射
            Map<String, Category> categoryMap = loadCategoryMap();
            Map<String, Unit> unitMap = loadUnitMap();
            
            // 收集所有商品
            List<Product> allProducts = new ArrayList<>();
            
            for (String dataFile : DATA_FILES) {
                try {
                    logger.info("正在下载文件: {}", dataFile);
                    List<Product> products = downloadAndParseData(dataFile, categoryMap, unitMap);
                    
                    if (!products.isEmpty()) {
                        logger.info("成功解析 {} 条商品数据", products.size());
                        allProducts.addAll(products);
                    } else {
                        messages.add(String.format("%s: 无数据", dataFile));
                    }
                } catch (Exception e) {
                    logger.error("导入文件 {} 失败", dataFile, e);
                    messages.add(String.format("%s: 导入失败 - %s", dataFile, e.getMessage()));
                }
            }
            
            // 创建缺失的分类和单位
            if (!allProducts.isEmpty()) {
                int categoriesCreated = ensureCategoriesExist(allProducts, categoryMap);
                int unitsCreated = ensureUnitsExist(allProducts, unitMap);
                
                if (categoriesCreated > 0) {
                    messages.add(String.format("创建了 %d 个新分类", categoriesCreated));
                }
                if (unitsCreated > 0) {
                    messages.add(String.format("创建了 %d 个新单位", unitsCreated));
                }
                
                int inserted = insertProducts(allProducts);
                messages.add(String.format("成功导入 %d 条商品", inserted));
            }
            
            result.put("success", true);
            result.put("totalProcessed", totalProcessed);
            result.put("successCount", successCount);
            result.put("skippedCount", skippedCount);
            result.put("errorCount", errorCount);
            result.put("messages", messages);
            
            logger.info("GitHub 数据导入完成 - 处理: {}, 成功: {}, 跳过: {}, 错误: {}", 
                totalProcessed, successCount, skippedCount, errorCount);
            
        } catch (Exception e) {
            logger.error("GitHub 数据导入失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
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
            
            result.put("success", true);
            result.put("totalProcessed", totalProcessed);
            result.put("successCount", successCount);
            result.put("skippedCount", skippedCount);
            result.put("errorCount", errorCount);
            result.put("messages", messages);
            
            logger.info("CSV 文件导入完成 - 处理: {}, 成功: {}, 跳过: {}, 错误: {}", 
                totalProcessed, successCount, skippedCount, errorCount);
            
        } catch (Exception e) {
            logger.error("CSV 文件导入失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 下载并解析数据
     */
    private List<Product> downloadAndParseData(String dataFile, Map<String, Category> categoryMap, Map<String, Unit> unitMap) 
            throws Exception {
        
        String url = GITHUB_BARCODE_URL + dataFile;
        logger.info("正在下载: {}", url);
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);  // 增加超时时间
        connection.setReadTimeout(120000);   // 增加超时时间
        
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            logger.warn("下载失败，HTTP 响应码: {}", responseCode);
            throw new Exception("下载失败，HTTP 响应码: " + responseCode);
        }
        
        List<Product> products = new ArrayList<>();
        
        // 检查是否是 ZIP 文件
        if (dataFile.endsWith(".zip")) {
            logger.info("检测到 ZIP 文件，开始解压...");
            products = parseZipData(connection.getInputStream(), categoryMap, unitMap);
        } else {
            // 直接解析 CSV 文件
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    line = line.trim();
                    
                    // 跳过空行和注释行
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    
                    try {
                        Product product = parseProductLine(line, categoryMap, unitMap);
                        if (product != null) {
                            products.add(product);
                        }
                    } catch (Exception e) {
                        logger.warn("解析第 {} 行失败: {}", lineNum, e.getMessage());
                    }
                }
            }
        }
        
        return products;
    }
    
    /**
     * 解析 ZIP 文件中的数据
     */
    private List<Product> parseZipData(java.io.InputStream zipInputStream, Map<String, Category> categoryMap, Map<String, Unit> unitMap) 
            throws Exception {
        
        List<Product> products = new ArrayList<>();
        
        try (java.util.zip.ZipInputStream zipStream = new java.util.zip.ZipInputStream(zipInputStream)) {
            java.util.zip.ZipEntry entry;
            
            while ((entry = zipStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // 只处理 CSV 文件
                if (entryName.endsWith(".csv") || entryName.endsWith(".txt")) {
                    logger.info("解压文件: {}", entryName);
                    
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(zipStream, StandardCharsets.UTF_8))) {
                        
                        String line;
                        int lineNum = 0;
                        while ((line = reader.readLine()) != null) {
                            lineNum++;
                            line = line.trim();
                            
                            // 跳过空行和注释行
                            if (line.isEmpty() || line.startsWith("#")) {
                                continue;
                            }
                            
                            try {
                                Product product = parseProductLine(line, categoryMap, unitMap);
                                if (product != null) {
                                    products.add(product);
                                }
                            } catch (Exception e) {
                                logger.warn("解析 {} 第 {} 行失败: {}", entryName, lineNum, e.getMessage());
                            }
                        }
                    }
                }
                
                zipStream.closeEntry();
            }
        }
        
        return products;
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

        // 解析价格
        if (parts.length > 2) {
            product.price = parseBigDecimal(parts[2], BigDecimal.ZERO);
        }

        // 解析单位（添加验证和标准化）
        if (parts.length > 3) {
            String unitName = parts[3].trim();
            if (!unitName.isEmpty()) {
                // 标准化单位名称
                product.unit = normalizeUnit(unitName);
            } else {
                product.unit = "个"; // 默认单位
            }
        } else {
            product.unit = "个";
        }

        // 解析分类
        if (parts.length > 4) {
            String categoryName = parts[4].trim();
            if (!categoryName.isEmpty()) {
                product.category = categoryName;
            } else {
                product.category = "默认分类"; // 默认分类
            }
        } else {
            product.category = "默认分类";
        }

        // 解析品牌
        if (parts.length > 5) {
            product.brand = parts[5].trim();
        }

        // 解析规格
        if (parts.length > 6) {
            product.spec = parts[6].trim();
        }

        // 解析供应商
        if (parts.length > 7) {
            product.supplier = parts[7].trim();
        }

        // 如果分类为默认分类，尝试根据供应商和商品名称自动分类
        if ("默认分类".equals(product.category)) {
            if (product.supplier != null && !product.supplier.isEmpty()) {
                product.category = classifyBySupplier(product.supplier);
            } else if (product.name != null && !product.name.isEmpty()) {
                product.category = classifyByName(product.name);
            }
        }

        // 设置默认值（注意：库存数量设为 0，不调整库存）
        product.quantity = 0;  // 导入时不设置库存数量
        product.minStock = 10;
        product.cost = product.getPrice().compareTo(BigDecimal.ZERO) > 0
            ? product.getPrice().multiply(new BigDecimal("0.7"))
            : BigDecimal.ZERO; // 默认成本价为售价的70%
        product.description = "从基础数据导入";

        return product;
    }

    /**
     * 根据商品名称自动分类
     */
    private String classifyByName(String name) {
        if (name == null || name.isEmpty()) {
            return "默认分类";
        }

        // 药品类
        if (name.contains("胶囊") || name.contains("片") || name.contains("丸") ||
            name.contains("注射液") || name.contains("颗粒") || name.contains("口服液") ||
            name.contains("糖浆") || name.contains("酊") || name.contains("栓") ||
            name.contains("软膏") || name.contains("乳膏") || name.contains("凝胶") ||
            name.contains("贴剂") || name.contains("感冒") || name.contains("止咳") ||
            name.contains("退烧") || name.contains("止痛") || name.contains("消炎")) {
            return "药品类";
        }

        // 食品类
        if (name.contains("饼干") || name.contains("薯片") || name.contains("糖果") ||
            name.contains("巧克力") || name.contains("饮料") || name.contains("牛奶") ||
            name.contains("酸奶") || name.contains("啤酒") || name.contains("白酒") ||
            name.contains("红酒") || name.contains("米") || name.contains("面") ||
            name.contains("油") || name.contains("盐") || name.contains("酱") ||
            name.contains("醋") || name.contains("调味") || name.contains("肉") ||
            name.contains("蛋") || name.contains("菜") || name.contains("水果") ||
            name.contains("蔬菜") || name.contains("熟食") || name.contains("罐头") ||
            name.contains("方便面") || name.contains("速冻") || name.contains("冷冻") ||
            name.contains("零食") || name.contains("坚果") || name.contains("果脯") ||
            name.contains("月饼") || name.contains("汤圆") || name.contains("粽子")) {
            return "食品类";
        }

        // 日用百货类
        if (name.contains("牙膏") || name.contains("牙刷") || name.contains("洗发水") ||
            name.contains("沐浴露") || name.contains("香皂") || name.contains("洗衣液") ||
            name.contains("洗洁精") || name.contains("洗衣粉") || name.contains("柔顺剂") ||
            name.contains("消毒液") || name.contains("卫生纸") || name.contains("纸巾") ||
            name.contains("湿巾") || name.contains("纸尿裤") || name.contains("卫生巾") ||
            name.contains("护垫") || name.contains("面膜") || name.contains("爽肤水") ||
            name.contains("乳液") || name.contains("面霜") || name.contains("眼霜") ||
            name.contains("精华") || name.contains("防晒") || name.contains("粉底") ||
            name.contains("口红") || name.contains("睫毛膏") || name.contains("眼影") ||
            name.contains("指甲油") || name.contains("香水") || name.contains("洗护")) {
            return "日用百货类";
        }

        // 保健品类
        if (name.contains("人参") || name.contains("阿胶") || name.contains("燕窝") ||
            name.contains("枸杞") || name.contains("冬虫夏草") || name.contains("红枣") ||
            name.contains("西洋参") || name.contains("钙片") || name.contains("维生素") ||
            name.contains("蛋白粉") || name.contains("鱼油") || name.contains("卵磷脂") ||
            name.contains("氨糖") || name.contains("软骨素") || name.contains("褪黑素") ||
            name.contains("叶酸") || name.contains("胶原蛋白") || name.contains("酵素") ||
            name.contains("麦片") || name.contains("燕麦片")) {
            return "保健品类";
        }

        return "默认分类";
    }
    
    /**
     * 解析CSV格式的商品行（逗号分隔，来自EricLiuCN/barcode）
     * 格式: id,barcode,name,spec,unit,price,brand,supplier,made_in,created_at,updated_at,deleted_at
     */
    private Product parseCSVCommaLine(String line, Map<String, Category> categoryMap, Map<String, Unit> unitMap) {
        // 去除引号
        line = line.replaceAll("\"", "");

        // 按逗号分割
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

        // 解析规格
        if (parts.length > 3) {
            String spec = parts[3].trim();
            if (!spec.isEmpty() && !spec.equals("NULL")) {
                product.spec = spec;
            }
        }

        // 解析单位（添加验证和标准化）
        if (parts.length > 4) {
            String unitName = parts[4].trim();
            if (!unitName.isEmpty() && !unitName.equals("NULL")) {
                // 标准化单位名称
                product.unit = normalizeUnit(unitName);
            } else {
                product.unit = "个";
            }
        } else {
            product.unit = "个";
        }

        // 解析价格
        if (parts.length > 5) {
            String priceStr = parts[5].trim();
            if (!priceStr.isEmpty() && !priceStr.equals("NULL")) {
                product.price = parseBigDecimal(priceStr, BigDecimal.ZERO);
            }
        }

        // 解析品牌
        if (parts.length > 6) {
            String brand = parts[6].trim();
            if (!brand.isEmpty() && !brand.equals("NULL")) {
                product.brand = brand;
            }
        }

        // 解析供应商
        if (parts.length > 7) {
            String supplier = parts[7].trim();
            if (!supplier.isEmpty() && !supplier.equals("NULL")) {
                product.supplier = supplier;
            }
        }

        // 根据供应商自动分类
        if (product.supplier != null && !product.supplier.isEmpty()) {
            product.category = classifyBySupplier(product.supplier);
        } else {
            product.category = "默认分类";
        }

        // 设置默认值（注意：库存数量设为 0，不调整库存）
        product.quantity = 0;  // 导入时不设置库存数量
        product.minStock = 10;
        product.cost = product.getPrice().compareTo(BigDecimal.ZERO) > 0
            ? product.getPrice().multiply(new BigDecimal("0.7"))
            : BigDecimal.ZERO; // 默认成本价为售价的70%
        product.description = "从EricLiuCN/barcode导入";

        return product;
    }

    /**
     * 根据供应商名称自动分类
     */
    private String classifyBySupplier(String supplier) {
        if (supplier == null || supplier.isEmpty()) {
            return "默认分类";
        }

        // 药品类
        if (supplier.contains("制药") || supplier.contains("医药") || supplier.contains("药业")) {
            return "药品类";
        }

        // 食品类
        if (supplier.contains("食品") || supplier.contains("粮油") || supplier.contains("饮料") ||
            supplier.contains("乳业") || supplier.contains("糖酒") || supplier.contains("茶")) {
            return "食品类";
        }

        // 日用百货类
        if (supplier.contains("日化") || supplier.contains("化妆") || supplier.contains("洗涤") ||
            supplier.contains("清洁") || supplier.contains("生活") || supplier.contains("家居")) {
            return "日用百货类";
        }

        // 保健品类
        if (supplier.contains("保健") || supplier.contains("营养") || supplier.contains("生物") ||
            supplier.contains("健康") || supplier.contains("养生")) {
            return "保健品类";
        }

        return "默认分类";
    }

    /**
     * 标准化单位名称
     */
    private String normalizeUnit(String unitName) {
        // 移除多余空格
        unitName = unitName.trim();

        // 标准化常见单位
        switch (unitName) {
            case "g":
            case "克":
                return "克";
            case "kg":
            case "千克":
            case "公斤":
                return "千克";
            case "ml":
            case "毫升":
                return "毫升";
            case "l":
            case "L":
            case "升":
                return "升";
            case "piece":
            case "PCS":
            case "pc":
                return "个";
            case "box":
            case "盒":
                return "盒";
            case "bottle":
            case "瓶":
                return "瓶";
            case "bag":
            case "袋":
                return "袋";
            case "package":
            case "包":
                return "包";
            case "set":
            case "套":
                return "套";
            case "pair":
            case "对":
                return "对";
            case "tin":
            case "听":
                return "听";
            case "can":
            case "罐":
                return "罐";
            default:
                return unitName;
        }
    }
    
    /**
     * 解析双精度数值
     */
    private double parseDouble(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty() || value.trim().equals("NULL")) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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
            List<Category> categories = CategoryDAO.findAll();
            for (Category category : categories) {
                categoryMap.put(category.name, category);
            }
            
            // 确保至少有默认分类
            if (!categoryMap.containsKey("默认分类")) {
                Category defaultCategory = new Category("默认分类", "默认商品分类");
                CategoryDAO.insert(defaultCategory);
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
            List<Unit> units = UnitDAO.findAll();
            for (Unit unit : units) {
                unitMap.put(unit.name, unit);
            }
            
            // 确保至少有默认单位
            if (!unitMap.containsKey("个")) {
                Unit defaultUnit = new Unit("个", "默认单位");
                UnitDAO.insert(defaultUnit);
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
                        CategoryDAO.insert(newCategory);
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
                        UnitDAO.insert(newUnit);
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
