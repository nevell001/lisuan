package com.cashier.api.controller;

import com.cashier.util.LoggerFactoryUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.slf4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 请求体解析助手：把"客户端把请求体写错了"变成 400，而不是 500。
 *
 * <p>背景：{@link Context#bodyAsClass(Class)} 在<b>字段名写错（未知字段）</b>、字段类型不符、
 * 请求体不是合法 JSON、请求体为空时都会<b>抛异常</b>。这些异常会被各控制器自己的
 * {@code catch (Exception e) → 500} 兜住，于是客户端拼错一个字段名只看到「服务器内部错误」，
 * 服务端日志里还留一堆堆栈，两边都不好排查。</p>
 *
 * <p>各控制器在解析之后本来就有 {@code if (request == null) → 400} 的判断（原意就是"解析失败
 * 返回 null"，只是 {@code bodyAsClass} 并不会返回 null），因此这里解析失败时<b>只返回 null</b>，
 * 让既有的 400 分支生效——无需改动任何一处控制流。</p>
 *
 * <p>失败原因（含写错的字段名与可用字段列表）记在 WARN 日志里。</p>
 */
final class ApiRequest {

    private static final Logger logger = LoggerFactoryUtil.getLogger(ApiRequest.class);

    /** Jackson 未知字段报错形如：Unrecognized field "stock" (class ...) */
    private static final Pattern UNKNOWN_FIELD = Pattern.compile("Unrecognized field \"([^\"]+)\"");
    /** 同一句报错里带着可用字段清单：(13 known properties: "barcode", "unit", ...) */
    private static final Pattern KNOWN_PROPERTIES = Pattern.compile("\\(\\d+ known properties: ([^)]+)\\)");

    private ApiRequest() {
    }

    /**
     * 解析请求体。
     *
     * @return 解析结果；请求体不合法时返回 {@code null}（调用方既有的空判断会回 400）
     */
    static <T> T parse(Context ctx, Class<T> type) {
        try {
            return ctx.bodyAsClass(type);
        } catch (Exception e) {
            if (!isClientBodyError(e)) {
                // 不是请求体的问题（例如服务端自身故障），照旧按 500 处理，不要伪装成 400
                throw e;
            }
            logger.warn("请求体解析失败（{} {}）: {}", ctx.method(), ctx.path(), describe(e));
            return null;
        }
    }

    /** 只有这两类属于"客户端请求体写错了"。 */
    static boolean isClientBodyError(Throwable t) {
        return t instanceof JsonProcessingException || t instanceof BadRequestResponse;
    }

    /** 把 Jackson/Javalin 的英文报错转成能照着改的中文说明。 */
    static String describe(Throwable t) {
        String raw = t.getMessage() == null ? "" : t.getMessage();
        Matcher unknownField = UNKNOWN_FIELD.matcher(raw);
        if (unknownField.find()) {
            return "包含未知字段 \"" + unknownField.group(1) + "\"（" + knownFields(raw) + "）";
        }
        if (t instanceof BadRequestResponse) {
            return "请求体为空或不是合法 JSON";
        }
        return raw;
    }

    private static String knownFields(String raw) {
        Matcher known = KNOWN_PROPERTIES.matcher(raw);
        return known.find()
            ? "可用字段: " + known.group(1).replace("\"", "")
            : "请核对字段名";
    }
}
