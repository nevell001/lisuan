package com.cashier.api;

import com.cashier.model.User;
import com.cashier.api.ApiConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiServerTest {

    @Test
    @DisplayName("生成不可预测的 URL 安全 Token")
    void testGenerateSecureToken() {
        ApiServer apiServer = ApiServer.getInstance();
        User user = new User();
        user.id = 42;

        Set<String> generatedTokens = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            String token = apiServer.generateToken(user);

            assertTrue(token.matches("[A-Za-z0-9_-]{43}\\.[A-Za-z0-9_-]{43}"),
                "Token 应为 32 字节随机数加 HMAC 签名的 Base64URL 格式");
            byte[] tokenIdBytes = Base64.getUrlDecoder().decode(token.split("\\.")[0]);
            assertEquals(32, tokenIdBytes.length, "Token 随机部分应为 32 字节，不可预测");
            assertTrue(generatedTokens.add(token), "Token 不应重复");
        }
    }

    @Test
    @DisplayName("空、篡改和已注销 Token 均无效")
    void invalidTokensAreRejected() {
        ApiServer apiServer = ApiServer.getInstance();
        User user = new User();
        user.id = 42;
        String token = apiServer.generateToken(user);

        assertNull(apiServer.validateToken(null));
        assertNull(apiServer.validateToken("  "));
        assertNull(apiServer.validateToken(token + "tampered"));

        apiServer.invalidateToken(token);
        assertNull(apiServer.validateToken(token));
    }

    @Test
    @DisplayName("过期 Token 无效")
    void expiredTokensAreRejected() throws Exception {
        Field expireField = ApiConfig.class.getDeclaredField("tokenExpireHours");
        expireField.setAccessible(true);
        int original = expireField.getInt(null);
        try {
            expireField.setInt(null, -1);

            ApiServer apiServer = ApiServer.getInstance();
            User user = new User();
            user.id = 42;
            String token = apiServer.generateToken(user);

            assertNull(apiServer.validateToken(token), "过期 Token 应被拒绝");
        } finally {
            expireField.setInt(null, original);
        }
    }

    @Test
    @DisplayName("签发新 Token 时清理已过期 Token")
    void expiredTokensArePurgedOnIssue() throws Exception {
        Field expireField = ApiConfig.class.getDeclaredField("tokenExpireHours");
        expireField.setAccessible(true);
        int original = expireField.getInt(null);

        ApiServer apiServer = ApiServer.getInstance();
        User user = new User();
        user.id = 42;
        // 先清掉其它用例可能留下的过期条目，保证计数可预期
        apiServer.purgeExpiredTokens();
        try {
            expireField.setInt(null, -1);   // 签发即过期
            apiServer.generateToken(user);  // 过期条目 1
            apiServer.generateToken(user);  // 签发前已清理条目 1，只剩这一个
        } finally {
            expireField.setInt(null, original);
        }

        assertEquals(1, apiServer.purgeExpiredTokens(),
            "签发新 Token 时应已清理更早的过期 Token，否则过期条目会常驻内存");
        assertEquals(0, apiServer.purgeExpiredTokens(), "清理后不应再有残留");
    }

    @Test
    @DisplayName("只有基础健康检查和登录接口公开")
    void publicRouteBoundaryIsMinimal() {
        assertTrue(ApiServer.isPublicApiPath("/api/health"));
        assertTrue(ApiServer.isPublicApiPath("/api/auth/login"));
        assertFalse(ApiServer.isPublicApiPath("/api/health/detail"));
        assertFalse(ApiServer.isPublicApiPath("/api/auth/refresh"));
        assertFalse(ApiServer.isPublicApiPath("/api/products"));

        // 支付回调必须公开（微信/支付宝服务器回调无本系统 Token，安全靠验签）
        assertTrue(ApiServer.isPublicApiPath("/api/payment/notify/wechat"));
        assertTrue(ApiServer.isPublicApiPath("/api/payment/notify/alipay"));
        // 其余支付接口（创建/查询/退款/配置）保持受保护
        assertFalse(ApiServer.isPublicApiPath("/api/payment/create"));
        assertFalse(ApiServer.isPublicApiPath("/api/payment/PAY123/status"));
        assertFalse(ApiServer.isPublicApiPath("/api/payment/PAY123/refund"));
    }

    @Test
    @DisplayName("404 兜底只在响应不是业务 JSON 时生效")
    void notFoundFallbackOnlyFillsNonJsonBody() {
        assertTrue(ApiServer.shouldFillNotFoundBody(null, null), "端点没写响应体时用统一的兜底文案");
        assertTrue(ApiServer.shouldFillNotFoundBody("  ", null), "空白响应体同样视作没写");

        // 实测：Javalin 对未匹配路由预填的是纯文本 "Endpoint GET /x not found"（text/plain）
        assertTrue(ApiServer.shouldFillNotFoundBody("Endpoint GET /x not found", "text/plain"),
            "未匹配路由必须换成统一的 JSON 文案，不能把 Javalin 的英文默认文本透给客户端");

        // 实测：端点自己写的业务 404 是 application/json
        assertFalse(ApiServer.shouldFillNotFoundBody(
                "{\"success\":false,\"message\":\"交易不存在\"}", "application/json"),
            "端点已经写了业务 404 时不得覆盖：否则客户端分不清「接口不存在」与「资源不存在」");
    }

    @Test
    @DisplayName("404 处理器必须同时看响应体与 Content-Type，且字面量路由要排在 {id} 之前")
    void notFoundHandlerAndRouteOrderAreGuarded() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/cashier/api/ApiServer.java"));

        assertTrue(source.contains("shouldFillNotFoundBody(existingBody, contentType)"),
            "404 处理器必须按「响应体 + Content-Type」决定要不要兜底，不能无条件覆盖");
        assertTrue(source.contains(".getContentType()"),
            "只看有没有响应体会把 Javalin 预填的纯文本当成业务响应");

        int today = source.indexOf("app.get(\"/api/transactions/today\"");
        int byId = source.indexOf("app.get(\"/api/transactions/{id}\"");
        assertTrue(today >= 0 && byId >= 0, "找不到 transactions 的路由注册");
        assertTrue(today < byId,
            "/api/transactions/today 必须注册在 /api/transactions/{id} 之前，"
                + "否则 Javalin 会把它当成 id=\"today\" 交给详情接口、回一个 404");
    }

    @Test
    @DisplayName("任何字面量路由都不得被更早注册的同方法占位路由遮蔽（通用门禁）")
    void noLiteralRouteIsShadowedByEarlierPatternRoute() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/cashier/api/ApiServer.java"));

        // 按注册顺序解析全部 REST 路由（方法 + 路径）
        List<String[]> routes = new ArrayList<>();
        Matcher matcher = Pattern.compile("app\\.(get|post|put|delete|patch)\\(\"([^\"]+)\"")
            .matcher(source);
        while (matcher.find()) {
            routes.add(new String[]{matcher.group(1), matcher.group(2)});
        }
        assertTrue(routes.size() > 50, "应解析出全部路由注册，实际只有: " + routes.size());

        List<String> shadowed = new ArrayList<>();
        for (int later = 0; later < routes.size(); later++) {
            String method = routes.get(later)[0];
            String laterPath = routes.get(later)[1];
            if (laterPath.contains("{")) {
                continue; // 占位路由本身就是兜底，不存在"被遮蔽"
            }
            for (int earlier = 0; earlier < later; earlier++) {
                if (!routes.get(earlier)[0].equals(method)) {
                    continue;
                }
                String earlierPath = routes.get(earlier)[1];
                if (!earlierPath.contains("{")) {
                    continue; // 两个字面量路径只有完全相同才冲突，注册顺序不影响
                }
                if (patternMatches(earlierPath, laterPath)) {
                    shadowed.add(method.toUpperCase() + " " + laterPath
                        + " 被更早注册的 " + earlierPath + " 遮蔽");
                }
            }
        }

        assertTrue(shadowed.isEmpty(),
            "字面量路由必须注册在同方法同段数的占位路由之前：Javalin 取第一个匹配的路由，"
                + "被遮蔽的端点永远执行不到（实测 /api/products/low-stock、/api/invoices/seller-info 均为该缺陷）。实际: "
                + shadowed);
    }

    /** Javalin 的 {param} 匹配整段；字面量段必须逐段相等。 */
    private static boolean patternMatches(String pattern, String literal) {
        String[] patternSegments = pattern.split("/", -1);
        String[] literalSegments = literal.split("/", -1);
        if (patternSegments.length != literalSegments.length) {
            return false;
        }
        for (int i = 0; i < patternSegments.length; i++) {
            String segment = patternSegments[i];
            if (segment.startsWith("{") && segment.endsWith("}")) {
                continue;
            }
            if (!segment.equals(literalSegments[i])) {
                return false;
            }
        }
        return true;
    }
}
