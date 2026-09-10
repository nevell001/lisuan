package com.cashier.api;

import com.cashier.dao.DAOFactory;
import com.cashier.i18n.I18nManager;
import com.cashier.model.User;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.Locale;

/**
 * 按请求解析语言，<b>不修改进程级 {@link I18nManager} 的当前语言</b>。
 *
 * <p>解析顺序：显式 {@code ?locale=} 参数 → {@code Accept-Language} 请求头 →
 * 当前登录用户保存的语言偏好 → 系统当前语言。</p>
 *
 * <p>这样任一客户端切换语言只影响自己的响应；此前 {@code PUT /api/i18n/locale} 直接调用
 * {@code I18nManager.setLocale}，会连带把桌面端和所有其它终端的语言一起改掉。</p>
 */
public final class ApiLocaleResolver {

    /** 请求属性名：本次请求解析出的 {@link Locale}。 */
    public static final String LOCALE_ATTRIBUTE = "requestLocale";

    private static final Logger logger = LoggerFactoryUtil.getLogger(ApiLocaleResolver.class);

    private ApiLocaleResolver() {
    }

    /**
     * 取本次请求的语言；同一次请求内解析结果会被缓存到请求属性里。
     */
    public static Locale of(Context ctx) {
        Object cached = ctx.attribute(LOCALE_ATTRIBUTE);
        if (cached instanceof Locale locale) {
            return locale;
        }
        Locale resolved = resolve(ctx);
        ctx.attribute(LOCALE_ATTRIBUTE, resolved);
        return resolved;
    }

    private static Locale resolve(Context ctx) {
        String explicit = ctx.queryParam("locale");
        if (explicit == null || explicit.isBlank()) {
            explicit = ctx.header("Accept-Language");
        }
        if (explicit != null && !explicit.isBlank()) {
            String tag = firstLanguageTag(explicit);
            if (I18nManager.isSupported(tag)) {
                return I18nManager.parseSupportedLocale(tag);
            }
        }
        return userPreference(ctx);
    }

    private static Locale userPreference(Context ctx) {
        User user = ctx.attribute("currentUser");
        if (user == null || user.username == null || user.username.isBlank()) {
            return I18nManager.getInstance().getCurrentLocale();
        }
        try {
            String saved = DAOFactory.getInstance().getLanguagePreferenceDAO()
                .getLanguagePreference(user.username);
            if (saved != null && !saved.isBlank()) {
                return I18nManager.parseSupportedLocale(saved);
            }
        } catch (SQLException e) {
            logger.warn("读取用户语言偏好失败: {}", user.username, e);
        }
        return I18nManager.getInstance().getCurrentLocale();
    }

    /**
     * {@code Accept-Language} 可能是 {@code "zh-CN,zh;q=0.9"} 这种带权重列表，取第一个标签。
     */
    private static String firstLanguageTag(String header) {
        int comma = header.indexOf(',');
        String first = comma >= 0 ? header.substring(0, comma) : header;
        int semicolon = first.indexOf(';');
        return (semicolon >= 0 ? first.substring(0, semicolon) : first).trim();
    }
}
