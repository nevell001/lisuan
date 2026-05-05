package com.cashier.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.text.MessageFormat;

/**
 * 国际化管理器
 * 支持 ResourceBundle 多语言
 */
public class I18nManager {
    private static final Logger logger = LoggerFactory.getLogger(I18nManager.class);
    
    private static I18nManager instance;
    private Locale currentLocale;
    private ResourceBundle bundle;
    private final ConcurrentHashMap<String, ResourceBundle> bundles = new ConcurrentHashMap<>();
    
    // 支持的语言列表
    public static final Locale CHINESE = Locale.SIMPLIFIED_CHINESE;
    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale JAPANESE = Locale.JAPANESE;
    public static final Locale KOREAN = Locale.KOREAN;
    
    // 可用的语言列表
    public static final List<Locale> AVAILABLE_LOCALES = Arrays.asList(
        CHINESE, ENGLISH, JAPANESE, KOREAN
    );
    
    private I18nManager() {
        // 默认使用中文
        setLocale(CHINESE);
    }
    
    /**
     * 获取单例实例
     */
    public static I18nManager getInstance() {
        if (instance == null) {
            instance = new I18nManager();
        }
        return instance;
    }
    
    /**
     * 设置当前语言
     */
    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        this.bundle = getBundle(locale);
        logger.info("语言已切换到: {} ({})", locale.getDisplayLanguage(), locale);
    }
    
    /**
     * 设置当前语言（字符串格式）
     */
    public void setLocale(String languageTag) {
        Locale locale = Locale.forLanguageTag(languageTag);
        if (!AVAILABLE_LOCALES.contains(locale)) {
            locale = CHINESE; // 默认中文
        }
        setLocale(locale);
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
                return ResourceBundle.getBundle("com.cashier.i18n.messages", locale);
            } catch (MissingResourceException e) {
                logger.warn("找不到语言包: {}, 使用默认", locale);
                return ResourceBundle.getBundle("com.cashier.i18n.messages", CHINESE);
            }
        });
    }
    
    /**
     * 获取翻译文本
     */
    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            logger.warn("找不到翻译: {}", key);
            return key; // 返回 key 作为默认值
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
        List<LocaleInfo> list = new ArrayList<>();
        
        for (Locale locale : AVAILABLE_LOCALES) {
            LocaleInfo info = new LocaleInfo();
            info.locale = locale;
            info.languageTag = locale.toLanguageTag();
            info.displayName = locale.getDisplayLanguage(locale);
            info.displayNameLocal = locale.getDisplayLanguage(currentLocale);
            info.current = locale.equals(currentLocale);
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