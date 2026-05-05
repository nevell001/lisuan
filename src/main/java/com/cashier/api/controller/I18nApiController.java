package com.cashier.api.controller;

import com.cashier.i18n.I18nManager;
import com.cashier.i18n.I18nManager.LocaleInfo;
import com.cashier.i18n.I18n;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 国际化 REST API 控制器
 */
public class I18nApiController {
    private static final Logger logger = LoggerFactory.getLogger(I18nApiController.class);
    
    /**
     * 获取当前语言
     * GET /api/i18n/locale
     */
    public static void getCurrentLocale(Context ctx) {
        I18nManager manager = I18nManager.getInstance();
        
        ctx.json(Map.of(
            "success", true,
            "data", Map.of(
                "locale", manager.getCurrentLocale().toLanguageTag(),
                "language", manager.getCurrentLocale().getLanguage(),
                "country", manager.getCurrentLocale().getCountry(),
                "displayName", manager.getCurrentLocale().getDisplayName(manager.getCurrentLocale()),
                "displayNameLocal", manager.getCurrentLocale().getDisplayName()
            )
        ));
    }
    
    /**
     * 设置语言
     * PUT /api/i18n/locale
     * Body: { "locale": "zh-CN" }
     */
    public static void setLocale(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String localeStr = (String) body.get("locale");
            
            if (localeStr == null || localeStr.isEmpty()) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "缺少 locale 参数"
                ));
                return;
            }
            
            I18nManager manager = I18nManager.getInstance();
            manager.setLocale(localeStr);
            
            logger.info("语言已切换: {}", localeStr);
            
            ctx.json(Map.of(
                "success", true,
                "message", "语言设置成功",
                "data", Map.of(
                    "locale", manager.getCurrentLocale().toLanguageTag(),
                    "displayName", manager.getCurrentLocale().getDisplayName(manager.getCurrentLocale())
                )
            ));
            
        } catch (Exception e) {
            logger.error("设置语言失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "设置语言失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取可用语言列表
     * GET /api/i18n/locales
     */
    public static void getAvailableLocales(Context ctx) {
        I18nManager manager = I18nManager.getInstance();
        List<LocaleInfo> locales = manager.getAvailableLocales();
        
        List<Map<String, Object>> list = locales.stream()
            .map(info -> Map.<String, Object>of(
                "locale", info.languageTag,
                "displayName", info.displayName,
                "displayNameLocal", info.displayNameLocal,
                "current", info.current
            ))
            .collect(Collectors.toList());
        
        ctx.json(Map.of(
            "success", true,
            "data", list,
            "total", list.size()
        ));
    }
    
    /**
     * 获取翻译文本
     * GET /api/i18n/messages?key=xxx
     */
    public static void getMessage(Context ctx) {
        String key = ctx.queryParam("key");
        
        if (key == null || key.isEmpty()) {
            ctx.status(400).json(Map.of(
                "success", false,
                "error", "缺少 key 参数"
            ));
            return;
        }
        
        I18nManager manager = I18nManager.getInstance();
        String message = manager.get(key);
        
        ctx.json(Map.of(
            "success", true,
            "data", Map.of(
                "key", key,
                "message", message
            )
        ));
    }
    
    /**
     * 获取所有翻译（当前语言）
     * GET /api/i18n/messages/all
     */
    public static void getAllMessages(Context ctx) {
        I18nManager manager = I18nManager.getInstance();
        
        // 返回常用翻译
        Map<String, String> messages = new LinkedHashMap<>();
        
        // 通用
        messages.put(I18n.OK, I18n.t(I18n.OK));
        messages.put(I18n.CANCEL, I18n.t(I18n.CANCEL));
        messages.put(I18n.SAVE, I18n.t(I18n.SAVE));
        messages.put(I18n.DELETE, I18n.t(I18n.DELETE));
        messages.put(I18n.EDIT, I18n.t(I18n.EDIT));
        messages.put(I18n.ADD, I18n.t(I18n.ADD));
        messages.put(I18n.SEARCH, I18n.t(I18n.SEARCH));
        
        // POS
        messages.put(I18n.POS_TITLE, I18n.t(I18n.POS_TITLE));
        messages.put(I18n.POS_TOTAL, I18n.t(I18n.POS_TOTAL));
        messages.put(I18n.POS_PAY, I18n.t(I18n.POS_PAY));
        messages.put(I18n.POS_CASH, I18n.t(I18n.POS_CASH));
        messages.put(I18n.POS_CARD, I18n.t(I18n.POS_CARD));
        messages.put(I18n.POS_MOBILE, I18n.t(I18n.POS_MOBILE));
        
        // 商品
        messages.put(I18n.PRODUCT_NAME, I18n.t(I18n.PRODUCT_NAME));
        messages.put(I18n.PRODUCT_PRICE, I18n.t(I18n.PRODUCT_PRICE));
        messages.put(I18n.PRODUCT_STOCK, I18n.t(I18n.PRODUCT_STOCK));
        
        // 会员
        messages.put(I18n.MEMBER_NAME, I18n.t(I18n.MEMBER_NAME));
        messages.put(I18n.MEMBER_PHONE, I18n.t(I18n.MEMBER_PHONE));
        messages.put(I18n.MEMBER_BALANCE, I18n.t(I18n.MEMBER_BALANCE));
        
        // 状态
        messages.put(I18n.SUCCESS, I18n.t(I18n.SUCCESS));
        messages.put(I18n.ERROR, I18n.t(I18n.ERROR));
        messages.put(I18n.LOADING, I18n.t(I18n.LOADING));
        
        ctx.json(Map.of(
            "success", true,
            "data", messages,
            "locale", manager.getCurrentLocale().toLanguageTag()
        ));
    }
    
    /**
     * 获取指定语言的所有翻译
     * GET /api/i18n/messages/locale/:locale
     */
    public static void getMessagesForLocale(Context ctx) {
        String localeTag = ctx.pathParam("locale");
        
        try {
            Locale locale = Locale.forLanguageTag(localeTag);
            ResourceBundle bundle = ResourceBundle.getBundle("com.cashier.i18n.messages", locale);
            
            Map<String, String> messages = new LinkedHashMap<>();
            
            // 获取所有键
            for (String key : bundle.keySet()) {
                messages.put(key, bundle.getString(key));
            }
            
            ctx.json(Map.of(
                "success", true,
                "data", messages,
                "locale", locale.toLanguageTag(),
                "total", messages.size()
            ));
            
        } catch (MissingResourceException e) {
            ctx.status(404).json(Map.of(
                "success", false,
                "error", "找不到语言包: " + localeTag
            ));
        }
    }
}