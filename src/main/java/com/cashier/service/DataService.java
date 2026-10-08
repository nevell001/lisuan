package com.cashier.service;

import com.cashier.constant.FXConstants;
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
    public static final String DEFAULT_SQL_BACKUP_PATH = "backups/sql";
    private static final int FIRST_PAGE = 1;
    private static final int LEGACY_LOAD_LIMIT = 5000;
    private static final com.cashier.dao.ProductDAORefactored productDAO = com.cashier.dao.DAOFactory.getInstance().getProductDAO();

    /**
     * 加载库存数据
     */
    public static Map<String, Product> loadInventory() {
        try {
            List<Product> products = productDAO.findAll(FIRST_PAGE, LEGACY_LOAD_LIMIT).getData();
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
     * 使用批量操作替代逐条循环，在同一事务内完成查询+插入+更新
     * @throws SQLException 如果保存失败
     */
    public static void saveInventory(Map<String, Product> inventory) throws SQLException {
        if (inventory == null || inventory.isEmpty()) {
            return;
        }
        DatabaseManager.executeBooleanTransaction(conn -> {
            // 使用同一连接批量查询现有商品（事务内可见性）
            Map<String, Product> existingProducts = productDAO.findByNamesWithConnection(conn, inventory.keySet());

            List<Product> toInsert = new ArrayList<>();
            List<Product> toUpdate = new ArrayList<>();

            for (Product product : inventory.values()) {
                if (product == null || product.name == null || product.name.isBlank()) {
                    continue;
                }

                Product existingProduct = existingProducts.get(product.name);
                if (existingProduct == null) {
                    toInsert.add(product);
                } else {
                    product.id = existingProduct.id;
                    product.version = existingProduct.version;
                    if (product.productCode == null || product.productCode.isBlank()) {
                        product.productCode = existingProduct.productCode;
                    }
                    toUpdate.add(product);
                }
            }

            // 批量插入新商品
            if (!toInsert.isEmpty()) {
                productDAO.batchInsertWithConnection(conn, toInsert);
            }
            // 批量更新已有商品
            if (!toUpdate.isEmpty()) {
                productDAO.batchUpdateWithConnection(conn, toUpdate);
            }
            return true;
        });
    }

    /**
     * 加载用户数据
     */
    public static Map<String, User> loadUsers() {
        try {
            List<User> users = DAOFactory.getInstance().getUserDAO().findAll(FIRST_PAGE, LEGACY_LOAD_LIMIT).getData();
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
     * @throws SQLException 如果保存失败
     */
    public static void saveUsers(Map<String, User> users) throws SQLException {
        List<User> userList = new ArrayList<>(users.values());
        DAOFactory.getInstance().getUserDAO().batchInsert(userList);
    }

    /**
     * 加载会员数据
     */
    public static Map<String, Member> loadMembers() {
        try {
            List<Member> members = DAOFactory.getInstance().getMemberDAO().findAll(FIRST_PAGE, LEGACY_LOAD_LIMIT).getData();
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
     * @throws SQLException 如果保存失败
     */
    public static void saveMembers(Map<String, Member> members) throws SQLException {
        List<Member> memberList = new ArrayList<>(members.values());
        DAOFactory.getInstance().getMemberDAO().batchInsert(memberList);
    }

    /**
     * 加载交易数据
     */
    public static List<Transaction> loadTransactions() {
        try {
            return DAOFactory.getInstance().getTransactionDAO().findRecent(LEGACY_LOAD_LIMIT);
        } catch (SQLException e) {
            logger.error("加载交易数据失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存交易数据
     * @throws SQLException 如果保存失败
     */
    public static void saveTransactions(List<Transaction> transactions) throws SQLException {
        DAOFactory.getInstance().getTransactionDAO().batchInsert(transactions);
    }

    /**
     * 加载促销数据
     */
    public static List<Promotion> loadPromotions() {
        try {
            return DAOFactory.getInstance().getPromotionDAO().findRecent(LEGACY_LOAD_LIMIT);
        } catch (SQLException e) {
            logger.error("加载促销数据失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存促销数据
     * 删除全部旧数据 + 插入新数据在同一事务中，保证原子性
     * @throws SQLException 如果保存失败
     */
    public static void savePromotions(List<Promotion> promotions) throws SQLException {
        DatabaseManager.executeBooleanTransaction(conn -> {
            // 批量删除所有促销（使用单条 SQL，不再逐条删除）
            try (java.sql.PreparedStatement delStmt = conn.prepareStatement("DELETE FROM promotions")) {
                delStmt.executeUpdate();
            }
            // 批量插入新促销
            if (promotions != null && !promotions.isEmpty()) {
                DAOFactory.getInstance().getPromotionDAO().batchInsertWithConnection(conn, promotions);
            }
            return true;
        });
    }

    /**
     * 加载充值记录
     */
    public static List<RechargeRecord> loadRechargeRecords() {
        try {
            return DAOFactory.getInstance().getRechargeRecordDAO().findRecent(LEGACY_LOAD_LIMIT);
        } catch (SQLException e) {
            logger.error("加载充值记录失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存充值记录
     * @throws SQLException 如果保存失败
     */
    public static void saveRechargeRecords(List<RechargeRecord> records) throws SQLException {
        DAOFactory.getInstance().getRechargeRecordDAO().batchInsert(records);
    }

    /**
     * 加载分类数据
     */
    public static List<Category> loadCategories() {
        try {
            return DAOFactory.getInstance().getCategoryDAO().findAll();
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
     * 删除全部旧数据 + 插入新数据在同一事务中，保证原子性
     * @throws SQLException 如果保存失败
     */
    public static void saveCategories(List<Category> categories) throws SQLException {
        DatabaseManager.executeBooleanTransaction(conn -> {
            // 批量删除所有分类（使用单条 SQL，不再逐条删除）
            try (java.sql.PreparedStatement delStmt = conn.prepareStatement("DELETE FROM categories")) {
                delStmt.executeUpdate();
            }
            // 批量插入新分类
            if (categories != null && !categories.isEmpty()) {
                DAOFactory.getInstance().getCategoryDAO().batchInsertWithConnection(conn, categories);
            }
            return true;
        });
    }

    /**
     * 加载操作日志
     */
    public static List<OperationLog> loadOperationLogs() {
        try {
            return DAOFactory.getInstance().getOperationLogDAO().findRecent(LEGACY_LOAD_LIMIT);
        } catch (SQLException e) {
            logger.error("加载操作日志失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存操作日志
     * @throws SQLException 如果保存失败
     */
    public static void saveOperationLogs(List<OperationLog> logs) throws SQLException {
        DAOFactory.getInstance().getOperationLogDAO().batchInsert(logs);
    }

    /**
     * 加载设置数据
     */
    public static Map<String, String> loadSettings() {
        Map<String, String> settings = new HashMap<>();
        try {
            // 使用 getAllSettings 加载所有设置
            Map<String, String> allSettings = DAOFactory.getInstance().getSystemSettingsDAO().getAllSettings();
            settings.putAll(allSettings);

            // 确保必要字段存在（默认值）
            if (!settings.containsKey("taxRate")) {
                settings.put("taxRate", "0.0");
            }
            if (!settings.containsKey("transactionCount")) {
                settings.put("transactionCount", "0");
            }
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
     * @throws SQLException 如果保存失败
     */
    public static void saveSettings(Map<String, String> settings) throws SQLException {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            DAOFactory.getInstance().getSystemSettingsDAO().setSetting(entry.getKey(), entry.getValue());
        }
        logger.info("保存设置数据成功，共保存 {} 个设置项", settings.size());
    }

    /**
     * 读取整数型设置并做区间钳制。
     *
     * <p>给"登录失败锁定次数""空闲自动登出分钟数"这类**必须有安全默认值**的配置用：
     * 设置缺失、非法或超出范围时一律回落到安全值，绝不让一条脏设置把防护关掉
     * （2026-10 审计 F6：这些设置项此前只写不读）。</p>
     *
     * @param key          设置键
     * @param defaultValue 缺失/非法时的默认值
     * @param min          允许的最小值
     * @param max          允许的最大值
     * @return 落在 [min, max] 内的整数
     */
    public static int getIntSetting(String key, int defaultValue, int min, int max) {
        String raw = loadSettings().get(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            logger.warn("设置 {} 无法解析为整数，按默认 {} 处理: {}", key, defaultValue, raw);
            return defaultValue;
        }
    }

    /**
     * 读取开关型设置。
     *
     * @param key          设置键
     * @param defaultValue 缺失时的默认值（与设置页里复选框的默认勾选状态保持一致）
     */
    public static boolean getBooleanSetting(String key, boolean defaultValue) {
        String raw = loadSettings().get(key);
        return raw == null || raw.isBlank() ? defaultValue : Boolean.parseBoolean(raw.trim());
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
            String themeName = DAOFactory.getInstance().getThemePreferenceDAO().findThemePreference(username);
            if (themeName == null && !"default".equals(username)) {
                themeName = DAOFactory.getInstance().getThemePreferenceDAO().findThemePreference("default");
            }
            return normalizeThemeName(themeName != null ? themeName : FXConstants.DEFAULT_THEME);
        } catch (SQLException e) {
            logger.error("加载主题偏好失败", e);
            return FXConstants.DEFAULT_THEME;
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
            DAOFactory.getInstance().getThemePreferenceDAO().setThemePreference(username, normalizeThemeName(themeName));
        } catch (SQLException e) {
            logger.error("保存主题偏好失败", e);
        }
    }

    private static String normalizeThemeName(String themeName) {
        return "intellij".equals(themeName) ? "lisuan" : themeName;
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
            return DAOFactory.getInstance().getLanguagePreferenceDAO().getLanguagePreference(username);
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
            DAOFactory.getInstance().getLanguagePreferenceDAO().setLanguagePreference(username, languageTag);
        } catch (SQLException e) {
            logger.error("保存语言偏好失败", e);
        }
    }

    /**
     * 加载字号偏好
     */
    public static String loadFontSizePreference() {
        return loadFontSizePreference("default");
    }

    /**
     * 加载指定用户的字号偏好
     */
    public static String loadFontSizePreference(String username) {
        try {
            return DAOFactory.getInstance().getFontSizePreferenceDAO().getFontSizePreference(username);
        } catch (SQLException e) {
            logger.error("加载字号偏好失败", e);
            return "medium"; // 默认中等字号
        }
    }

    /**
     * 保存字号偏好
     */
    public static void saveFontSizePreference(String fontSize) {
        saveFontSizePreference("default", fontSize);
    }

    /**
     * 保存指定用户的字号偏好
     */
    public static void saveFontSizePreference(String username, String fontSize) {
        try {
            DAOFactory.getInstance().getFontSizePreferenceDAO().setFontSizePreference(username, fontSize);
        } catch (SQLException e) {
            logger.error("保存字号偏好失败", e);
        }
    }

    /**
     * 检查是否有活跃班次
     */
    /**
     * 班次状态三态（TD-035）。
     *
     * <p>此前 {@code hasActiveShift()} 吞掉 {@code SQLException} 返回 false，调用方于是把
     * "数据库故障"显示成"请先开班/没有活跃班次"，排查方向被带偏。用三态把两者分开：
     * 只有 {@link ActiveShiftState#NONE} 才该提示开班，{@link ActiveShiftState#UNKNOWN} 要报系统错误。</p>
     */
    public enum ActiveShiftState {
        /** 有活跃班次 */
        ACTIVE,
        /** 确认没有活跃班次 */
        NONE,
        /** 查询失败（数据库不可用），无法判定 */
        UNKNOWN
    }

    /**
     * 班次状态对应的提示文案 key（TD-035）。
     *
     * <p>放在这里而不是控制器：多个结账/支付入口都要"没有班次 → 请开班，查不到 → 系统错误"，
     * 判定逻辑集中一处，控制器只负责显示（也避免把 CartController 撑破体积棘轮）。</p>
     *
     * @return ACTIVE 时返回 null（无需提示）
     */
    public static String activeShiftBlockingMessageKey(ActiveShiftState state) {
        if (state == null || state == ActiveShiftState.ACTIVE) {
            return null;
        }
        return state == ActiveShiftState.NONE ? "runtime.no_active_shift" : "runtime.shift_state_unknown";
    }

    /** 查询班次状态；失败返回 {@link ActiveShiftState#UNKNOWN}（不再伪造成"没有班次"）。 */
    public static ActiveShiftState activeShiftState() {
        try {
            return DAOFactory.getInstance().getShiftDAO().hasActiveShift()
                ? ActiveShiftState.ACTIVE : ActiveShiftState.NONE;
        } catch (SQLException e) {
            logger.error("检查活跃班次失败（将按「无法确认」处理，不再当作没有班次）", e);
            return ActiveShiftState.UNKNOWN;
        }
    }

    /**
     * 是否有活跃班次。
     *
     * <p>保留布尔口径给"禁用按钮/提示"这类不涉及资金的展示；**结账等资金路径请用
     * {@link #activeShiftState()}**，否则数据库故障会被误报成"请先开班"（TD-035）。</p>
     */
    public static boolean hasActiveShift() {
        return activeShiftState() == ActiveShiftState.ACTIVE;
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
            File backupDir = new File(resolveSqlBackupPath(backupPath));
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
    
            // 使用时间戳创建备份文件名
            String timestamp = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
                .format(com.cashier.util.DateTimeFormats.BACKUP_TIMESTAMP);
            File backupFile = new File(backupDir, DatabaseManager.getBackupFilePrefix() + "_" + timestamp + ".sql");
    
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
            File backupFile = new File(resolveSqlBackupPath(backupPath));
    
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
        }

        public static String resolveSqlBackupPath(String backupPath) {
            if (backupPath == null || backupPath.trim().isEmpty()) {
                return DEFAULT_SQL_BACKUP_PATH;
            }
            return backupPath.trim();
        }
}
