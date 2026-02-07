package com.cashier.util;

import com.cashier.dao.*;
import com.cashier.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 数据迁移工具
 * 将现有的文本文件数据迁移到 MySQL 数据库
 */
public class DataMigrationTool {
    private static final Logger logger = LoggerFactory.getLogger(DataMigrationTool.class);

    private static final String DATA_DIR = "data";

    /**
     * 执行完整的数据迁移
     * @return 迁移成功返回 true，否则返回 false
     */
    public static boolean migrate() {
        System.out.println("========== 开始数据迁移 ==========");

        try {
            // 1. 初始化数据库
            System.out.println("初始化数据库连接...");
            DatabaseManager.getConnection();  // 触发静态初始化

            // 2. 检查数据库是否已有数据
            if (isDatabasePopulated()) {
                System.out.println("数据库已包含数据，跳过迁移");
                return true;
            }

            // 3. 备份现有数据文件
            if (!backupDataFiles()) {
                System.err.println("数据文件备份失败，迁移中止");
                return false;
            }

            // 4. 按顺序迁移各个数据表
            boolean success = true;

            success &= migrateUsers();
            success &= migrateProducts();
            success &= migrateMembers();
            success &= migrateTransactions();
            success &= migrateShifts();
            success &= migratePromotions();
            success &= migrateRechargeRecords();
            success &= migrateCategories();
            success &= migrateOperationLogs();
            success &= migrateSystemSettings();
            success &= migrateThemePreference();

            if (success) {
                System.out.println("========== 数据迁移完成 ==========");
                return true;
            } else {
                System.err.println("数据迁移过程中出现错误");
                return false;
            }

        } catch (Exception e) {
            System.err.println("数据迁移失败: " + e.getMessage());
            logger.error("数据迁移失败", e);
            return false;
        }
    }

    /**
     * 检查数据库是否已有数据
     */
    private static boolean isDatabasePopulated() {
        try {
            List<Product> products = ProductDAO.findAll();
            return !products.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 备份现有数据文件
     */
    private static boolean backupDataFiles() {
        System.out.println("备份现有数据文件...");
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path backupDir = Paths.get(DATA_DIR, "backup_" + timestamp);
            Files.createDirectories(backupDir);

            File dataFolder = new File(DATA_DIR);
            File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".txt"));

            if (files != null) {
                for (File file : files) {
                    Path target = backupDir.resolve(file.getName());
                    Files.copy(file.toPath(), target);
                    System.out.println("  已备份: " + file.getName());
                }
            }

            System.out.println("数据文件备份完成: " + backupDir);
            return true;

        } catch (IOException e) {
            System.err.println("备份失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移用户数据
     */
    private static boolean migrateUsers() {
        System.out.println("迁移用户数据...");
        try {
            Map<String, User> users = DataManager.loadUsers();
            if (users.isEmpty()) {
                System.out.println("  没有用户数据需要迁移");
                return true;
            }

            List<User> userList = new ArrayList<>(users.values());
            UserDAO.batchInsert(userList);
            System.out.println("  成功迁移 " + userList.size() + " 个用户");
            return true;

        } catch (Exception e) {
            System.err.println("  用户数据迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移商品数据
     */
    private static boolean migrateProducts() {
        System.out.println("迁移商品数据...");
        try {
            Map<String, Product> products = DataManager.loadInventory();
            if (products.isEmpty()) {
                System.out.println("  没有商品数据需要迁移");
                return true;
            }

            List<Product> productList = new ArrayList<>(products.values());
            ProductDAO.batchInsert(productList);
            System.out.println("  成功迁移 " + productList.size() + " 个商品");
            return true;

        } catch (Exception e) {
            System.err.println("  商品数据迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移会员数据
     */
    private static boolean migrateMembers() {
        System.out.println("迁移会员数据...");
        try {
            Map<String, Member> members = DataManager.loadMembers();
            if (members.isEmpty()) {
                System.out.println("  没有会员数据需要迁移");
                return true;
            }

            List<Member> memberList = new ArrayList<>(members.values());
            MemberDAO.batchInsert(memberList);
            System.out.println("  成功迁移 " + memberList.size() + " 个会员");
            return true;

        } catch (Exception e) {
            System.err.println("  会员数据迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移交易数据
     */
    private static boolean migrateTransactions() {
        System.out.println("迁移交易数据...");
        try {
            List<Transaction> transactions = DataManager.loadTransactions();
            if (transactions.isEmpty()) {
                System.out.println("  没有交易数据需要迁移");
                return true;
            }

            TransactionDAO.batchInsert(transactions);
            System.out.println("  成功迁移 " + transactions.size() + " 个交易");
            return true;

        } catch (Exception e) {
            System.err.println("  交易数据迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移交接班数据
     */
    private static boolean migrateShifts() {
        System.out.println("迁移交接班数据...");
        try {
            List<Shift> shifts = DataManager.loadShifts();
            if (shifts.isEmpty()) {
                System.out.println("  没有交接班数据需要迁移");
                return true;
            }

            ShiftDAO.batchInsert(shifts);
            System.out.println("  成功迁移 " + shifts.size() + " 个交接班记录");
            return true;

        } catch (Exception e) {
            System.err.println("  交接班数据迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移促销数据
     */
    private static boolean migratePromotions() {
        System.out.println("迁移促销数据...");
        try {
            List<Promotion> promotions = DataManager.loadPromotions();
            if (promotions.isEmpty()) {
                System.out.println("  没有促销数据需要迁移");
                return true;
            }

            PromotionDAO.batchInsert(promotions);
            System.out.println("  成功迁移 " + promotions.size() + " 个促销");
            return true;

        } catch (Exception e) {
            System.err.println("  促销数据迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移充值记录
     */
    private static boolean migrateRechargeRecords() {
        System.out.println("迁移充值记录...");
        try {
            List<RechargeRecord> records = DataManager.loadRechargeRecords();
            if (records.isEmpty()) {
                System.out.println("  没有充值记录需要迁移");
                return true;
            }

            RechargeRecordDAO.batchInsert(records);
            System.out.println("  成功迁移 " + records.size() + " 条充值记录");
            return true;

        } catch (Exception e) {
            System.err.println("  充值记录迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移分类数据
     */
    private static boolean migrateCategories() {
        System.out.println("迁移分类数据...");
        try {
            List<Category> categories = DataManager.loadCategories();
            if (categories.isEmpty()) {
                System.out.println("  没有分类数据需要迁移");
                return true;
            }

            CategoryDAO.batchInsert(categories);
            System.out.println("  成功迁移 " + categories.size() + " 个分类");
            return true;

        } catch (Exception e) {
            System.err.println("  分类数据迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移操作日志
     */
    private static boolean migrateOperationLogs() {
        System.out.println("迁移操作日志...");
        try {
            List<OperationLog> logs = DataManager.loadOperationLogs();
            if (logs.isEmpty()) {
                System.out.println("  没有操作日志需要迁移");
                return true;
            }

            OperationLogDAO.batchInsert(logs);
            System.out.println("  成功迁移 " + logs.size() + " 条操作日志");
            return true;

        } catch (Exception e) {
            System.err.println("  操作日志迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移系统设置
     */
    private static boolean migrateSystemSettings() {
        System.out.println("迁移系统设置...");
        try {
            Map<String, String> settings = DataManager.loadSettings();
            if (settings.isEmpty()) {
                System.out.println("  没有系统设置需要迁移");
                return true;
            }

            // 迁移税率
            if (settings.containsKey("taxRate")) {
                double taxRate = Double.parseDouble(settings.get("taxRate"));
                SystemSettingsDAO.setTaxRate(taxRate);
                System.out.println("  迁移税率: " + taxRate);
            }

            // 迁移交易计数
            if (settings.containsKey("transactionCount")) {
                int count = Integer.parseInt(settings.get("transactionCount"));
                SystemSettingsDAO.setTransactionCount(count);
                System.out.println("  迁移交易计数: " + count);
            }

            return true;

        } catch (Exception e) {
            System.err.println("  系统设置迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 迁移主题偏好
     */
    private static boolean migrateThemePreference() {
        System.out.println("迁移主题偏好...");
        try {
            String theme = DataManager.loadThemePreference();
            ThemePreferenceDAO.setThemePreference(theme);
            System.out.println("  迁移主题偏好: " + theme);
            return true;

        } catch (Exception e) {
            System.err.println("  主题偏好迁移失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取迁移进度统计
     */
    public static Map<String, Integer> getMigrationStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();

        try {
            stats.put("用户", UserDAO.findAll().size());
            stats.put("商品", ProductDAO.findAll().size());
            stats.put("会员", MemberDAO.findAll().size());
            stats.put("交易", TransactionDAO.findAll().size());
            stats.put("交接班", ShiftDAO.findAll().size());
            stats.put("促销", PromotionDAO.findAll().size());
            stats.put("充值记录", RechargeRecordDAO.findAll().size());
            stats.put("分类", CategoryDAO.findAll().size());
            stats.put("操作日志", OperationLogDAO.findAll().size());
        } catch (Exception e) {
            System.err.println("获取迁移统计失败: " + e.getMessage());
        }

        return stats;
    }

    /**
     * 主函数 - 独立运行迁移工具
     */
    public static void main(String[] args) {
        System.out.println("收银系统数据迁移工具 (MySQL)");
        System.out.println("=========================\n");

        boolean success = migrate();

        if (success) {
            System.out.println("\n迁移成功！");
            System.out.println("\n数据统计:");
            Map<String, Integer> stats = getMigrationStats();
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }

            System.out.println("\n建议:");
            System.out.println("  1. 验证数据迁移结果");
            System.out.println("  2. 备份文件夹位于: " + DATA_DIR + "/backup_<timestamp>");
            System.out.println("  3. 确认无误后可删除原 .txt 文件");

        } else {
            System.err.println("\n迁移失败！请检查错误信息。");
            System.err.println("原数据文件已备份，可手动恢复。");
            System.exit(1);
        }
    }
}
