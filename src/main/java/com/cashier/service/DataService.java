package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * 数据服务
 * 提供数据访问接口，使用 MySQL 数据库
 */
public class DataService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(DataService.class);
    private static final com.cashier.dao.ProductDAORefactored productDAO = com.cashier.dao.DAOFactory.getInstance().getProductDAO();

    /**
     * 加载库存数据
     */
    public static Map<String, Product> loadInventory() {
        try {
            List<Product> products = productDAO.findAll();
            Map<String, Product> inventory = new HashMap<>();
            for (Product product : products) {
                inventory.put(product.name, product);
            }
            return inventory;
        } catch (SQLException e) {
            logger.error("加载商品数据失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 保存库存数据
     */
    public static void saveInventory(Map<String, Product> inventory) {
        try {
            // 批量删除所有商品
            List<Product> existing = productDAO.findAll();
            for (Product p : existing) {
                productDAO.delete(p.id);
            }

            // 批量插入新商品
            List<Product> products = new ArrayList<>(inventory.values());
            productDAO.batchInsert(products);
        } catch (SQLException e) {
            logger.error("保存商品数据失败", e);
        }
    }

    /**
     * 加载用户数据
     */
    public static Map<String, User> loadUsers() {
        try {
            List<User> users = UserDAO.findAll();
            Map<String, User> userMap = new HashMap<>();
            for (User user : users) {
                userMap.put(user.username, user);
            }
            return userMap;
        } catch (SQLException e) {
            logger.error("加载用户数据失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 保存用户数据
     */
    public static void saveUsers(Map<String, User> users) {
        try {
            List<User> userList = new ArrayList<>(users.values());
            UserDAO.batchInsert(userList);
        } catch (SQLException e) {
            logger.error("保存用户数据失败", e);
        }
    }

    /**
     * 加载会员数据
     */
    public static Map<String, Member> loadMembers() {
        try {
            List<Member> members = MemberDAO.findAll();
            Map<String, Member> memberMap = new HashMap<>();
            for (Member member : members) {
                memberMap.put(member.phone, member);
            }
            return memberMap;
        } catch (SQLException e) {
            logger.error("加载会员数据失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 保存会员数据
     */
    public static void saveMembers(Map<String, Member> members) {
        try {
            List<Member> memberList = new ArrayList<>(members.values());
            MemberDAO.batchInsert(memberList);
        } catch (SQLException e) {
            logger.error("保存会员数据失败", e);
        }
    }

    /**
     * 加载交易数据
     */
    public static List<Transaction> loadTransactions() {
        try {
            return TransactionDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载交易数据失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存交易数据
     */
    public static void saveTransactions(List<Transaction> transactions) {
        try {
            TransactionDAO.batchInsert(transactions);
        } catch (SQLException e) {
            logger.error("保存交易数据失败", e);
        }
    }

    /**
     * 加载促销数据
     */
    public static List<Promotion> loadPromotions() {
        try {
            return PromotionDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载促销数据失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存促销数据
     */
    public static void savePromotions(List<Promotion> promotions) {
        try {
            // 批量删除所有促销
            List<Promotion> existing = PromotionDAO.findAll();
            for (Promotion p : existing) {
                PromotionDAO.delete(p.id);
            }

            // 批量插入新促销
            PromotionDAO.batchInsert(promotions);
        } catch (SQLException e) {
            logger.error("保存促销数据失败", e);
        }
    }

    /**
     * 加载充值记录
     */
    public static List<RechargeRecord> loadRechargeRecords() {
        try {
            return RechargeRecordDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载充值记录失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存充值记录
     */
    public static void saveRechargeRecords(List<RechargeRecord> records) {
        try {
            RechargeRecordDAO.batchInsert(records);
        } catch (SQLException e) {
            logger.error("保存充值记录失败", e);
        }
    }

    /**
     * 加载分类数据
     */
    public static List<Category> loadCategories() {
        try {
            return CategoryDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载分类数据失败", e);
            List<Category> categories = new ArrayList<>();
            // 返回默认分类
            categories.add(new Category("默认分类", "默认商品分类"));
            categories.add(new Category("食品", "食品类商品"));
            categories.add(new Category("饮料", "饮品类商品"));
            categories.add(new Category("日用品", "日用品类商品"));
            return categories;
        }
    }

    /**
     * 保存分类数据
     */
    public static void saveCategories(List<Category> categories) {
        try {
            // 批量删除所有分类
            List<Category> existing = CategoryDAO.findAll();
            for (Category c : existing) {
                CategoryDAO.delete(c.id);
            }

            // 批量插入新分类
            CategoryDAO.batchInsert(categories);
        } catch (SQLException e) {
            logger.error("保存分类数据失败", e);
        }
    }

    /**
     * 加载操作日志
     */
    public static List<OperationLog> loadOperationLogs() {
        try {
            return OperationLogDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载操作日志失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存操作日志
     */
    public static void saveOperationLogs(List<OperationLog> logs) {
        try {
            OperationLogDAO.batchInsert(logs);
        } catch (SQLException e) {
            logger.error("保存操作日志失败", e);
        }
    }

    /**
     * 加载设置数据
     */
    public static Map<String, String> loadSettings() {
        Map<String, String> settings = new HashMap<>();
        try {
            double taxRate = SystemSettingsDAO.getTaxRate();
            int transactionCount = SystemSettingsDAO.getTransactionCount();
            settings.put("taxRate", String.valueOf(taxRate));
            settings.put("transactionCount", String.valueOf(transactionCount));
        } catch (SQLException e) {
            logger.error("加载设置数据失败", e);
            // 返回默认值
            settings.put("taxRate", "0.0");
            settings.put("transactionCount", "0");
        }
        return settings;
    }

    /**
     * 保存设置数据
     */
    public static void saveSettings(double taxRate, int transactionCount) {
        try {
            SystemSettingsDAO.setTaxRate(taxRate);
            SystemSettingsDAO.setTransactionCount(transactionCount);
        } catch (SQLException e) {
            logger.error("保存设置数据失败", e);
        }
    }

    /**
     * 加载主题偏好
     */
    public static String loadThemePreference() {
        return loadThemePreference("default");
    }

    /**
     * 加载指定用户的主题偏好
     */
    public static String loadThemePreference(String username) {
        try {
            return ThemePreferenceDAO.getThemePreference(username);
        } catch (SQLException e) {
            logger.error("加载主题偏好失败", e);
            return "light"; // 默认主题
        }
    }

    /**
     * 保存主题偏好
     */
    public static void saveThemePreference(String themeName) {
        saveThemePreference("default", themeName);
    }

    /**
     * 保存指定用户的主题偏好
     */
    public static void saveThemePreference(String username, String themeName) {
        try {
            ThemePreferenceDAO.setThemePreference(username, themeName);
        } catch (SQLException e) {
            logger.error("保存主题偏好失败", e);
        }
    }

    /**
     * 加载语言偏好
     */
    public static String loadLanguagePreference() {
        return loadLanguagePreference("default");
    }

    /**
     * 加载指定用户的语言偏好
     */
    public static String loadLanguagePreference(String username) {
        try {
            return LanguagePreferenceDAO.getLanguagePreference(username);
        } catch (SQLException e) {
            logger.error("加载语言偏好失败", e);
            return "zh-CN"; // 默认简体中文
        }
    }

    /**
     * 保存语言偏好
     */
    public static void saveLanguagePreference(String languageTag) {
        saveLanguagePreference("default", languageTag);
    }

    /**
     * 保存指定用户的语言偏好
     */
    public static void saveLanguagePreference(String username, String languageTag) {
        try {
            LanguagePreferenceDAO.setLanguagePreference(username, languageTag);
        } catch (SQLException e) {
            logger.error("保存语言偏好失败", e);
        }
    }

    /**
     * 检查是否有活跃班次
     */
    public static boolean hasActiveShift() {
        try {
            return ShiftDAO.hasActiveShift();
        } catch (SQLException e) {
            logger.error("检查活跃班次失败", e);
            return false;
        }
    }

    /**
     * 初始化数据服务
     */
    public static void initialize() {
        // 数据库已通过 DatabaseManager 初始化
    }

    /**
         * 备份数据库
         * @param backupPath 备份目录路径
         */
        public static void backupData(String backupPath) throws IOException {
            File backupDir = new File(backupPath);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
    
            // 使用时间戳创建备份文件名
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            File backupFile = new File(backupDir, "cashier_system_" + timestamp + ".sql");
    
            boolean success = DatabaseManager.backup(backupFile);
            if (!success) {
                throw new IOException("数据库备份失败");
            }
        }
    
        /**
         * 恢复数据库
         * @param backupPath 备份文件路径或备份目录路径
         */
        public static void restoreData(String backupPath) throws IOException {
            File backupFile = new File(backupPath);
    
            // 如果是目录，查找最新的 .sql 文件
            if (backupFile.isDirectory()) {
                File[] sqlFiles = backupFile.listFiles((dir, name) -> name.endsWith(".sql"));
                if (sqlFiles == null || sqlFiles.length == 0) {
                    throw new IOException("备份目录中未找到 SQL 备份文件: " + backupPath);
                }
    
                // 按修改时间排序，取最新的
                java.util.Arrays.sort(sqlFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                backupFile = sqlFiles[0];
            }
    
            if (!backupFile.exists()) {
                throw new IOException("备份文件不存在: " + backupFile.getAbsolutePath());
            }
    
            boolean success = DatabaseManager.restore(backupFile);
            if (!success) {
                throw new IOException("数据库恢复失败");
            }
        }}
