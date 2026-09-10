package com.cashier.api.controller;

import com.cashier.api.ApiLocaleResolver;
import com.cashier.constant.ResourceBundleNames;

import com.cashier.dao.DAOFactory;
import com.cashier.i18n.I18nManager;
import com.cashier.i18n.I18nManager.LocaleInfo;
import com.cashier.i18n.I18n;
import com.cashier.model.User;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 国际化 REST API 控制器
 */
public class I18nApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(I18nApiController.class);
    
    /**
     * 获取当前语言（本次请求解析出的语言，非进程级语言）
     * GET /api/i18n/locale
     */
    public static void getCurrentLocale(Context ctx) {
        Locale locale = ApiLocaleResolver.of(ctx);

        ctx.json(Map.of(
            "success", true,
            "data", Map.of(
                "locale", locale.toLanguageTag(),
                "language", locale.getLanguage(),
                "country", locale.getCountry(),
                "displayName", locale.getDisplayName(locale),
                "displayNameLocal", locale.getDisplayName()
            )
        ));
    }
    
    /**
     * 设置语言（只保存当前登录用户的语言偏好，不改进程级语言）
     * PUT /api/i18n/locale
     * Body: { "locale": "zh-CN" }
     */
    public static void setLocale(Context ctx) {
        try {
            User user = ctx.attribute("currentUser");
            if (user == null || user.username == null) {
                ctx.status(401).json(Map.of(
                    "success", false,
                    "error", "缺少认证用户"
                ));
                return;
            }

            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            if (body == null) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "请求体不能为空"
                ));
                return;
            }
            String localeStr = getString(body, "locale");
            
            if (localeStr == null || localeStr.isEmpty()) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "缺少 locale 参数"
                ));
                return;
            }
            if (!I18nManager.isSupported(localeStr)) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "error", "不支持的语言: " + localeStr
                ));
                return;
            }

            Locale locale = I18nManager.parseSupportedLocale(localeStr);
            // 只写当前用户的语言偏好：不再调用 I18nManager.setLocale，避免把桌面端
            // 和其它终端的语言一起改掉；本请求的响应语言通过 ApiLocaleResolver 隔离。
            DAOFactory.getInstance().getLanguagePreferenceDAO()
                .setLanguagePreference(user.username, locale.toLanguageTag());

            logger.info("用户 {} 的语言偏好已更新: {}", user.username, locale.toLanguageTag());

            ctx.json(Map.of(
                "success", true,
                "message", "语言设置成功",
                "data", Map.of(
                    "locale", locale.toLanguageTag(),
                    "displayName", locale.getDisplayName(locale)
                )
            ));
            
        } catch (SQLException e) {
            logger.error("保存语言偏好失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "设置语言失败"
            ));
        } catch (Exception e) {
            logger.error("设置语言失败", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "error", "设置语言失败"
            ));
        }
    }
    
    /**
     * 获取可用语言列表（current 标记本次请求的语言）
     * GET /api/i18n/locales
     */
    public static void getAvailableLocales(Context ctx) {
        List<LocaleInfo> locales = I18nManager.getInstance()
            .getAvailableLocales(ApiLocaleResolver.of(ctx));
        
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
     * 获取翻译文本（本次请求的语言）
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
        
        Locale locale = ApiLocaleResolver.of(ctx);
        String message = I18nManager.getInstance().get(locale, key);
        
        ctx.json(Map.of(
            "success", true,
            "data", Map.of(
                "key", key,
                "message", message
            ),
            "locale", locale.toLanguageTag()
        ));
    }
    
    /**
     * 获取所有翻译（本次请求的语言）
     * GET /api/i18n/messages/all
     */
    public static void getAllMessages(Context ctx) {
        Locale locale = ApiLocaleResolver.of(ctx);
        I18nManager manager = I18nManager.getInstance();
        String tag = locale.toLanguageTag();
        
        // 返回常用翻译
        Map<String, String> messages = new LinkedHashMap<>();
        
        // 通用
        messages.put(I18n.OK, manager.get(locale, I18n.OK));
        messages.put(I18n.CANCEL, manager.get(locale, I18n.CANCEL));
        messages.put(I18n.SAVE, manager.get(locale, I18n.SAVE));
        messages.put(I18n.DELETE, manager.get(locale, I18n.DELETE));
        messages.put(I18n.EDIT, manager.get(locale, I18n.EDIT));
        messages.put(I18n.ADD, manager.get(locale, I18n.ADD));
        messages.put(I18n.SEARCH, manager.get(locale, I18n.SEARCH));
        
        // POS
        messages.put(I18n.POS_TITLE, manager.get(locale, I18n.POS_TITLE));
        messages.put(I18n.POS_TOTAL, manager.get(locale, I18n.POS_TOTAL));
        messages.put(I18n.POS_PAY, manager.get(locale, I18n.POS_PAY));
        messages.put(I18n.POS_CASH, manager.get(locale, I18n.POS_CASH));
        messages.put(I18n.POS_CARD, manager.get(locale, I18n.POS_CARD));
        messages.put(I18n.POS_MOBILE, manager.get(locale, I18n.POS_MOBILE));
        
        // 商品
        messages.put(I18n.PRODUCT_NAME, manager.get(locale, I18n.PRODUCT_NAME));
        messages.put(I18n.PRODUCT_PRICE, manager.get(locale, I18n.PRODUCT_PRICE));
        messages.put(I18n.PRODUCT_STOCK, manager.get(locale, I18n.PRODUCT_STOCK));
        
        // 会员
        messages.put(I18n.MEMBER_NAME, manager.get(locale, I18n.MEMBER_NAME));
        messages.put(I18n.MEMBER_PHONE, manager.get(locale, I18n.MEMBER_PHONE));
        messages.put(I18n.MEMBER_BALANCE, manager.get(locale, I18n.MEMBER_BALANCE));
        
        // 状态
        messages.put(I18n.SUCCESS, manager.get(locale, I18n.SUCCESS));
        messages.put(I18n.ERROR, manager.get(locale, I18n.ERROR));
        messages.put(I18n.LOADING, manager.get(locale, I18n.LOADING));
        
        ctx.json(Map.of(
            "success", true,
            "data", messages,
            "locale", tag
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
            ResourceBundle bundle = ResourceBundle.getBundle(ResourceBundleNames.I18N_MESSAGES, locale);
            
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

    private static String getString(Map<?, ?> body, String key) {
        Object value = body.get(key);
        return value != null ? value.toString() : null;
    }
}
