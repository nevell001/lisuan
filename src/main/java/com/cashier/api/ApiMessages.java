package com.cashier.api;

import com.cashier.i18n.I18nManager;
import io.javalin.http.Context;

/**
 * REST API 的**请求级**文案与枚举显示名（TD-041）。
 *
 * <p>背景：API 此前把中文枚举显示名/错误文案写死后直接塞进 JSON，客户端拿到的既是"数据"又是
 * "给人看的文案"，切语言也无从下手。现在的契约是**双字段**：</p>
 *
 * <ul>
 *   <li>枚举字段返回**稳定代码**（{@code "NETWORK"}、{@code "SUCCESS"}）——客户端逻辑只依赖它；</li>
 *   <li>另给一个 {@code *Name} 字段返回按请求语言本地化的显示名——界面直接展示。</li>
 * </ul>
 *
 * <p>语言由 {@link ApiLocaleResolver} 按 {@code ?locale=} → {@code Accept-Language} → 用户偏好解析，
 * **不修改进程级语言**（否则一个客户端切语言会把桌面端一起改掉）。</p>
 */
public final class ApiMessages {

    private ApiMessages() {
    }

    /**
     * 按请求语言取文案。
     *
     * @param ctx    当前请求（取 {@code ?locale=}/{@code Accept-Language}/用户偏好）
     * @param key    i18n key
     * @param params 占位符参数
     */
    public static String text(Context ctx, String key, Object... params) {
        return I18nManager.getInstance().get(ApiLocaleResolver.of(ctx), key, params);
    }

    /**
     * 按请求语言取枚举显示名。
     *
     * <p>语言包缺该 key 时回退到枚举自带的显示名（保证老客户端至少拿到可读文案，
     * 而不是 {@code api.printer.device_type.XXX} 这种 key）。</p>
     *
     * @param ctx       当前请求
     * @param key      完整 i18n key（由调用方用字面量前缀 + 枚举代码拼出，门禁据此判定"在用"）
     * @param fallback 枚举自带显示名（语言包缺 key 时兜底，避免把 key 直接返给客户端）
     */
    public static String enumName(Context ctx, String key, String fallback) {
        String localized = I18nManager.getInstance().get(ApiLocaleResolver.of(ctx), key);
        return key.equals(localized) ? fallback : localized;
    }
}