package com.cashier.util;

import com.cashier.dao.DAOFactory;
import com.cashier.i18n.I18nManager;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 货币格式化工具类
 * 支持独立配置的货币，而非自动跟随语言
 */
public class CurrencyUtil {
    private static final Logger logger = LoggerFactoryUtil.getLogger(CurrencyUtil.class);

    /**
     * 货币信息类
     */
    public static class CurrencyInfo {
        public final String symbol;
        public final String code;
        public final String name;

        public CurrencyInfo(String symbol, String code, String name) {
            this.symbol = symbol;
            this.code = code;
            this.name = name;
        }
    }

    /**
     * 支持的货币列表
     */
    public static final Map<String, CurrencyInfo> SUPPORTED_CURRENCIES = new ConcurrentHashMap<>();

    static {
        SUPPORTED_CURRENCIES.put("CNY", new CurrencyInfo("¥", "CNY", "人民币"));
        SUPPORTED_CURRENCIES.put("USD", new CurrencyInfo("$", "USD", "美元"));
        SUPPORTED_CURRENCIES.put("JPY", new CurrencyInfo("¥", "JPY", "日元"));
        SUPPORTED_CURRENCIES.put("KRW", new CurrencyInfo("₩", "KRW", "韩元"));
        SUPPORTED_CURRENCIES.put("EUR", new CurrencyInfo("€", "EUR", "欧元"));
    }

    private static volatile String cachedSymbol = "¥";
    private static volatile String cachedCode = "CNY";

    // ThreadLocal DecimalFormat（DecimalFormat 非线程安全）
    private static volatile int formatVersion = 0;
    private static final ThreadLocal<DecimalFormat> TL_FORMAT = new ThreadLocal<>();
    private static final ThreadLocal<Integer> TL_VERSION = new ThreadLocal<>();

    /**
     * 获取当前线程的 DecimalFormat（版本不匹配时自动重建）
     */
    private static DecimalFormat getThreadLocalFormat() {
        Integer version = TL_VERSION.get();
        if (version == null || version != formatVersion) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.SIMPLIFIED_CHINESE);
            symbols.setCurrencySymbol(cachedSymbol);
            DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
            // TD-035：DecimalFormat 默认 HALF_EVEN，而全应用金额是 HALF_UP——
            // 1.005 会显示成 1.00（只影响 3 位以上小数的中间值，但口径必须统一）
            format.setRoundingMode(java.math.RoundingMode.HALF_UP);
            TL_FORMAT.set(format);
            TL_VERSION.set(formatVersion);
        }
        return TL_FORMAT.get();
    }

    static {
        // 初始化默认格式
        initDefaults();
    }

    /**
     * 初始化默认格式
     */
    private static void initDefaults() {
        // formatVersion 初始为 0，ThreadLocal 首次访问时自动创建
    }

    /**
     * 更新货币格式（根据当前用户配置）
     */
    public static void updateFormat() {
        try {
            // 从数据库获取用户货币偏好
            String username = getCurrentUsername();
            String currencyCode = DAOFactory.getInstance().getLanguagePreferenceDAO().getCurrencyPreference(username);

            CurrencyInfo currencyInfo = SUPPORTED_CURRENCIES.get(currencyCode);
            if (currencyInfo == null) {
                logger.warn("未找到货币代码: {}, 使用默认 CNY", currencyCode);
                currencyInfo = SUPPORTED_CURRENCIES.get("CNY");
            }

            cachedSymbol = currencyInfo.symbol;
            cachedCode = currencyInfo.code;

            // 递增版本号，使各线程的 ThreadLocal DecimalFormat 在下次访问时自动重建
            formatVersion++;

            logger.debug("货币格式已更新: {} ({})", currencyInfo.symbol, currencyInfo.code);
        } catch (Exception e) {
            logger.warn("更新货币格式失败: {}", e.getMessage());
            // 使用默认值
            cachedSymbol = "¥";
            cachedCode = "CNY";
        }
    }

    /**
     * 获取当前用户名（用于货币配置）
     */
    private static String getCurrentUsername() {
        // 尝试从 I18nManager 获取当前用户信息
        // 如果无法获取，使用默认用户
        try {
            // 这里应该从实际的用户会话中获取
            // 暂时使用 "default" 作为默认用户
            return "default";
        } catch (Exception e) {
            return "default";
        }
    }

    /**
     * 设置货币（供用户手动配置货币时调用）
     * @param currencyCode 货币代码 (CNY, USD, JPY, KRW, EUR)
     * @return 是否设置成功
     */
    public static boolean setCurrency(String currencyCode) {
        if (!SUPPORTED_CURRENCIES.containsKey(currencyCode)) {
            logger.warn("不支持的货币代码: {}", currencyCode);
            return false;
        }

        try {
            String username = getCurrentUsername();
            boolean success = DAOFactory.getInstance().getLanguagePreferenceDAO().setCurrencyPreference(username, currencyCode);
            if (success) {
                updateFormat();
                logger.info("货币已设置为: {}", currencyCode);
            }
            return success;
        } catch (Exception e) {
            logger.error("设置货币失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取当前货币符号
     */
    public static String getSymbol() {
        return cachedSymbol;
    }

    /**
     * 获取当前货币代码
     */
    public static String getCode() {
        return cachedCode;
    }

    /**
     * 获取当前货币信息
     */
    public static CurrencyInfo getCurrencyInfo() {
        return SUPPORTED_CURRENCIES.get(cachedCode);
    }

    /**
     * 格式化金额为货币字符串
     * @param amount 金额
     * @return 格式化后的货币字符串，如 "¥1,234.56"
     */
    public static String format(double amount) {
        try {
            return cachedSymbol + getThreadLocalFormat().format(amount);
        } catch (Exception e) {
            logger.error("货币格式化失败: {}", amount, e);
            return cachedSymbol + String.format(java.util.Locale.ROOT, "%.2f", amount);
        }
    }

    /**
     * 格式化 BigDecimal 金额为货币字符串
     * @param amount 金额
     * @return 格式化后的货币字符串，如 "¥1,234.56"
     */
    public static String format(java.math.BigDecimal amount) {
        if (amount == null) return format(0.0);
        return format(amount.doubleValue());
    }

    /**
     * 格式化金额为货币字符串
     * @param amount 金额
     * @return 格式化后的货币字符串
     */
    public static String format(long amount) {
        return format((double) amount);
    }

    /**
     * 格式化金额为货币字符串（带语言支持）
     * @param amount 金额
     * @param locale 指定语言环境
     * @return 格式化后的货币字符串
     */
    public static String format(double amount, Locale locale) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
        return nf.format(amount);
    }

    /**
     * 解析货币字符串为金额
     * @param currencyString 货币字符串
     * @return 金额
     */
    public static double parse(String currencyString) {
        if (currencyString == null || currencyString.isEmpty()) {
            return 0;
        }

        // 移除货币符号和其他非数字字符（保留小数点和负号）
        String cleaned = currencyString.replaceAll("[^0-9.−-]", "");

        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            logger.warn("无法解析货币字符串: {}", currencyString);
            return 0;
        }
    }

    /**
     * 格式化金额，不包含货币符号（仅格式化数字）
     * @param amount 金额
     * @return 格式化后的数字字符串，如 "1,234.56"
     */
    public static String formatNumberOnly(double amount) {
        return getThreadLocalFormat().format(amount);
    }

    /**
     * 当用户切换货币设置时刷新货币格式
     * 这个方法应该被设置界面调用
     */
    public static void refresh() {
        updateFormat();
    }

    /**
     * 获取支持的货币列表
     */
    public static Map<String, CurrencyInfo> getSupportedCurrencies() {
        return new ConcurrentHashMap<>(SUPPORTED_CURRENCIES);
    }

    /**
     * 根据货币代码获取货币符号
     */
    public static String getSymbol(String currencyCode) {
        CurrencyInfo info = SUPPORTED_CURRENCIES.get(currencyCode);
        return info != null ? info.symbol : "";
    }

    /**
     * 根据货币代码获取货币名称
     */
    public static String getCurrencyName(String currencyCode) {
        CurrencyInfo info = SUPPORTED_CURRENCIES.get(currencyCode);
        return info != null ? info.name : "";
    }
}
