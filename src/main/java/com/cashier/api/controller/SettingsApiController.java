package com.cashier.api.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统设置 REST API
 *
 * <p>读写的是桌面端同一份持久化：{@code settings} 表（{@code DataService.loadSettings/saveSettings}、
 * {@code SystemSettingsDAORefactored}）。此前这里读写的是 {@code config/settings.properties}，
 * 而全仓库没有任何代码读那个文件——接口回了 {@code success:true}，桌面端的税率/店名等设置却
 * 一点没变；写入失败也只在本地 catch 里记一条日志，调用方拿到的是假成功（2026-10 审计 F8）。</p>
 */
public class SettingsApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(SettingsApiController.class);

    /** 设置项 key 的长度上限，与 settings 表的列宽一致（`key` VARCHAR(100)），避免落库时截断/报错 */
    private static final int MAX_KEY_LENGTH = 100;

    /** 校验路径参数里的 key；不合法时写响应并返回 false。 */
    private static boolean validateKey(Context ctx) {
        String key = ctx.pathParam("key");
        if (key == null || key.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(Map.of("success", false, "message", "设置项名称不能为空"));
            return false;
        }
        if (key.length() > MAX_KEY_LENGTH) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(Map.of("success", false, "message", "设置项名称过长（最多 " + MAX_KEY_LENGTH + " 字符）"));
            return false;
        }
        return true;
    }

    /**
     * 获取所有设置
     * GET /api/settings
     */
    public static void list(Context ctx) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", new HashMap<>(DAOFactory.getInstance().getSystemSettingsDAO().getAllSettings()));
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取系统设置失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取系统设置失败"));
        }
    }

    /**
     * 获取单个设置
     * GET /api/settings/:key
     */
    public static void get(Context ctx) {
        if (!validateKey(ctx)) {
            return;
        }
        try {
            String key = ctx.pathParam("key");
            String value = DAOFactory.getInstance().getSystemSettingsDAO().getSetting(key);

            if (value == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "设置项不存在"));
                return;
            }

            ctx.json(Map.of("success", true, "key", key, "value", value));
        } catch (Exception e) {
            logger.error("获取设置项失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取设置项失败"));
        }
    }

    /**
     * 设置值
     * PUT /api/settings/:key
     */
    public static void set(Context ctx) {
        if (!validateKey(ctx)) {
            return;
        }
        try {
            String key = ctx.pathParam("key");
            Map<?, ?> body = ApiRequest.parse(ctx, Map.class);
            if (body == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "请求体为空或字段不合法"));
                return;
            }
            Object rawValue = body.get("value");
            String value = rawValue != null ? rawValue.toString() : null;

            if (value == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "缺少 value 参数"));
                return;
            }

            // 写失败必须如实报错：之前吞掉异常回 success，调用方以为设置生效了
            if (!DAOFactory.getInstance().getSystemSettingsDAO().setSetting(key, value)) {
                logger.error("设置未保存: {} = {}", key, value);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .json(Map.of("success", false, "message", "设置未保存"));
                return;
            }

            logger.info("设置已更新: {} = {}", key, value);
            ctx.json(Map.of("success", true, "key", key, "value", value, "message", "设置已更新"));
        } catch (Exception e) {
            logger.error("更新设置失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "更新设置失败"));
        }
    }

    /**
     * 删除设置
     * DELETE /api/settings/:key
     */
    public static void delete(Context ctx) {
        if (!validateKey(ctx)) {
            return;
        }
        try {
            String key = ctx.pathParam("key");

            if (DAOFactory.getInstance().getSystemSettingsDAO().getSetting(key) == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "设置项不存在"));
                return;
            }

            if (!DAOFactory.getInstance().getSystemSettingsDAO().deleteSetting(key)) {
                logger.error("设置未删除: {}", key);
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .json(Map.of("success", false, "message", "设置未删除"));
                return;
            }

            logger.info("设置已删除: {}", key);
            ctx.json(Map.of("success", true, "message", "设置已删除"));
        } catch (Exception e) {
            logger.error("删除设置失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "删除设置失败"));
        }
    }
}
