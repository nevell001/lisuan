package com.cashier.i18n;

import com.cashier.constant.ResourceBundleNames;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.text.MessageFormat;

/**
 * 国际化管理器
 * 支持 ResourceBundle 多语言
 */
public class I18nManager {
    private static final Logger logger = LoggerFactoryUtil.getLogger(I18nManager.class);
    
    private static I18nManager instance;
    private Locale currentLocale;
    private ResourceBundle bundle;
    private final ConcurrentHashMap<String, ResourceBundle> bundles = new ConcurrentHashMap<>();
    private static final Map<String, String> FALLBACK_TEXTS = Map.of(
        "inventory.status.out_of_stock", "Out of Stock",
        I18nKeys.Inventory.Status.LOW_STOCK, "Low Stock",
        I18nKeys.Inventory.Status.NORMAL, "Normal"
    );
    
    // 支持的语言列表
    public static final Locale CHINESE_SIMPLIFIED = Locale.SIMPLIFIED_CHINESE;
    public static final Locale CHINESE_TRADITIONAL = Locale.TRADITIONAL_CHINESE;
    public static final Locale ENGLISH = Locale.ENGLISH;

    // 可用的语言列表
    public static final List<Locale> AVAILABLE_LOCALES = Arrays.asList(
        CHINESE_SIMPLIFIED, CHINESE_TRADITIONAL, ENGLISH
    );

    private I18nManager() {
        // 默认使用简体中文
        setLocaleInternal(CHINESE_SIMPLIFIED);
    }

    /**
     * 获取单例实例
     */
    public static I18nManager getInstance() {
        if (instance == null) {
            instance = new I18nManager();
            // 在实例完全构造后刷新货币格式
            try {
                com.cashier.util.CurrencyUtil.refresh();
            } catch (Exception e) {
                LoggerFactoryUtil.getLogger(I18nManager.class).warn("刷新货币格式失败", e);
            }
        }
        return instance;
    }

    /**
     * 设置当前语言
     */
    public void setLocale(Locale locale) {
        setLocaleInternal(locale);

        // 刷新货币格式
        try {
            com.cashier.util.CurrencyUtil.refresh();
        } catch (Exception e) {
            logger.warn("刷新货币格式失败", e);
        }
    }

    /**
     * 内部设置语言方法（不触发货币刷新）
     */
    private void setLocaleInternal(Locale locale) {
        this.currentLocale = locale;
        this.bundle = getBundle(locale);
        logger.info("语言已切换到: {} ({})", locale.getDisplayLanguage(), locale);
    }
    
    /**
     * 设置当前语言（字符串格式）
     */
    public void setLocale(String languageTag) {
        boolean supported = isSupported(languageTag);
        Locale locale = supported ? parseSupportedLocale(languageTag) : CHINESE_SIMPLIFIED;

        if (!supported) {
            logger.warn("语言标签 {} 不在可用列表中，使用默认简体中文", languageTag);
        }

        logger.info("设置语言: {} -> Locale: {} (language={}, country={})",
            languageTag, locale, locale.getLanguage(), locale.getCountry());
        setLocale(locale);
    }

    /**
     * 语言标签是否在支持列表中。
     */
    public static boolean isSupported(String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            return false;
        }
        String normalizedTag = Locale.forLanguageTag(languageTag).toLanguageTag();
        return AVAILABLE_LOCALES.stream().anyMatch(locale -> locale.toLanguageTag().equals(normalizedTag));
    }

    /**
     * 把语言标签解析为受支持的 {@link Locale} 常量；不支持或为空时返回默认简体中文。
     */
    public static Locale parseSupportedLocale(String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            return CHINESE_SIMPLIFIED;
        }
        String normalizedTag = Locale.forLanguageTag(languageTag).toLanguageTag();
        for (Locale locale : AVAILABLE_LOCALES) {
            if (locale.toLanguageTag().equals(normalizedTag)) {
                return locale;
            }
        }
        return CHINESE_SIMPLIFIED;
    }
    
    /**
     * 获取当前语言
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * 获取当前语言标签
     */
    public String getCurrentLanguageTag() {
        return currentLocale.toLanguageTag();
    }
    
    /**
     * 获取 ResourceBundle
     */
    private ResourceBundle getBundle(Locale locale) {
        return bundles.computeIfAbsent(locale.toLanguageTag(), tag -> {
            try {
                return ResourceBundle.getBundle(ResourceBundleNames.I18N_MESSAGES, locale);
            } catch (MissingResourceException e) {
                logger.warn("找不到语言包: {}, 使用默认", locale);
                return ResourceBundle.getBundle(ResourceBundleNames.I18N_MESSAGES, CHINESE_SIMPLIFIED);
            }
        });
    }

    /**
     * 获取当前 ResourceBundle（用于 FXML 加载）
     */
    public ResourceBundle getResourceBundle() {
        return bundle;
    }
    
    /**
     * 获取翻译文本（当前语言）
     */
    public String get(String key) {
        return get(currentLocale, key);
    }

    /**
     * 按指定语言获取翻译文本，<b>不改变当前语言</b>。
     *
     * <p>供 REST API 按请求语言渲染响应使用：某个客户端切语言只影响它自己，
     * 不会把桌面端和其它终端的语言一起改掉。</p>
     *
     * @param locale 目标语言；为 null 时使用当前语言
     * @param key 资源键
     * @return 翻译文本；缺失时返回兜底文案或 key 本身
     */
    public String get(Locale locale, String key) {
        ResourceBundle target = locale == null ? bundle : getBundle(locale);
        try {
            return target.getString(key);
        } catch (MissingResourceException e) {
            String fallbackText = FALLBACK_TEXTS.get(key);
            if (fallbackText != null) {
                return fallbackText;
            }
            logger.warn("找不到翻译: {} ({})", key, locale);
            return key;
        }
    }
    
    /**
     * 获取翻译文本（带参数）
     */
    public String get(String key, Object... params) {
        String template = get(key);
        if (params == null || params.length == 0) {
            return template;
        }
        
        // 使用 MessageFormat 格式化
        return MessageFormat.format(template, params);
    }
    
    /**
     * 判断是否存在翻译
     */
    public boolean has(String key) {
        return bundle.containsKey(key);
    }
    
    /**
     * 获取所有可用的语言
     */
    public List<LocaleInfo> getAvailableLocales() {
        return getAvailableLocales(currentLocale);
    }

    /**
     * 获取所有可用的语言，并把指定语言标记为 current。
     *
     * @param currentLocale 用于标记 current 与渲染本地名称的语言；为 null 时用当前语言
     */
    public List<LocaleInfo> getAvailableLocales(Locale currentLocale) {
        Locale effective = currentLocale != null ? currentLocale : this.currentLocale;
        List<LocaleInfo> list = new ArrayList<>();
        
        for (Locale locale : AVAILABLE_LOCALES) {
            LocaleInfo info = new LocaleInfo();
            info.locale = locale;
            info.languageTag = locale.toLanguageTag();
            info.displayName = locale.getDisplayLanguage(locale);
            info.displayNameLocal = locale.getDisplayLanguage(effective);
            info.current = locale.equals(effective);
            list.add(info);
        }
        
        return list;
    }
    
    /**
     * 语言信息
     */
    public static class LocaleInfo {
        public Locale locale;
        public String languageTag;
        public String displayName;      // 该语言的本地名称
        public String displayNameLocal; // 当前语言下的名称
        public boolean current;         // 是否当前语言
        
        @Override
        public String toString() {
            return displayName + " (" + languageTag + ")";
        }
    }
}
