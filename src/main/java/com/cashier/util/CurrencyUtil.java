package com.cashier.util;

import com.cashier.i18n.I18nManager;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 货币格式化工具类
 * 货币符号根据当前语言环境自动选择
 */
public class CurrencyUtil {
    private static final Logger logger = LoggerFactoryUtil.getLogger(CurrencyUtil.class);

    private static String cachedSymbol = "¥";
    private static String cachedCode = "CNY";
    private static DecimalFormat currencyFormat;

    static {
        // 初始化默认格式
        initDefaults();
    }

    /**
     * 初始化默认格式
     */
    private static void initDefaults() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.SIMPLIFIED_CHINESE);
        symbols.setCurrencySymbol("¥");
        currencyFormat = new DecimalFormat("#,##0.00", symbols);
    }

    /**
     * 更新货币格式（当语言切换时调用）
     * 根据语言环境自动选择对应的货币符号
     */
    public static void updateFormat() {
        I18nManager i18n = I18nManager.getInstance();
        if (i18n == null) {
            logger.debug("I18nManager not initialized, using defaults");
            return;
        }

        Locale currentLocale = i18n.getCurrentLocale();
        String symbol = "¥";
        String code = "CNY";

        // 根据语言环境选择货币
        if (currentLocale.equals(Locale.CHINA) || currentLocale.equals(Locale.SIMPLIFIED_CHINESE)) {
            symbol = "¥";
            code = "CNY";
        } else if (currentLocale.equals(Locale.TRADITIONAL_CHINESE)) {
            symbol = "US$";
            code = "USD";
        } else if (currentLocale.equals(Locale.JAPAN)) {
            symbol = "¥";
            code = "JPY";
        } else if (currentLocale.equals(Locale.KOREA)) {
            symbol = "₩";
            code = "KRW";
        } else if (currentLocale.equals(Locale.US) || currentLocale.equals(Locale.ENGLISH)) {
            symbol = "$";
            code = "USD";
        }

        cachedSymbol = symbol;
        cachedCode = code;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(currentLocale);
        symbols.setCurrencySymbol(symbol);
        currencyFormat = new DecimalFormat("#,##0.00", symbols);

        logger.debug("Currency format updated: symbol={}, code={}, locale={}", symbol, code, currentLocale);
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
     * 格式化金额为货币字符串
     * @param amount 金额
     * @return 格式化后的货币字符串，如 "¥1,234.56"
     */
    public static String format(double amount) {
        try {
            return cachedSymbol + currencyFormat.format(amount);
        } catch (Exception e) {
            logger.error("货币格式化失败: {}", amount, e);
            return cachedSymbol + String.format("%.2f", amount);
        }
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
        return currencyFormat.format(amount);
    }

    /**
     * 当语言切换时刷新货币格式
     * 这个方法应该被 I18nManager 或语言切换逻辑调用
     */
    public static void refresh() {
        updateFormat();
    }
}
