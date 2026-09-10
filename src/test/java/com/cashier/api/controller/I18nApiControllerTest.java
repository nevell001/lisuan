package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.i18n.I18nManager;
import com.cashier.model.User;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 国际化 API：语言按请求隔离。
 *
 * <p>回归：{@code PUT /api/i18n/locale} 曾直接调用 {@code I18nManager.setLocale}，
 * 任一登录用户切语言会把桌面端和所有其它终端的语言一起改掉。</p>
 */
@DisplayName("国际化 API：按请求隔离语言")
class I18nApiControllerTest extends DatabaseTestBase {

    private static final String USER_A = "i18n_user_a";
    private static final String USER_B = "i18n_user_b";

    private static User user(String username) {
        User user = new User();
        user.username = username;
        user.name = "语言测试-" + username;
        user.role = "cashier";
        return user;
    }

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS language_preferences (
                    username VARCHAR(50) PRIMARY KEY,
                    language_tag VARCHAR(20),
                    currency_code VARCHAR(10) DEFAULT 'CNY',
                    updated_at BIGINT
                )
                """);
        }
    }

    @AfterEach
    void cleanup() throws SQLException {
        I18nManager.getInstance().setLocale("zh-CN");
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM language_preferences");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dataOf(TestContext ctx) {
        return (Map<String, Object>) response(ctx).get("data");
    }

    private static TestContext putLocale(User user, Map<String, Object> body) {
        return new TestContext()
            .withRequest(HandlerType.PUT, "/api/i18n/locale")
            .withAttribute("currentUser", user)
            .withBody(body);
    }

    @Test
    @DisplayName("空请求体设置语言返回 400")
    void setLocaleWithNullBodyReturns400() {
        TestContext ctx = new TestContext()
            .withRequest(HandlerType.PUT, "/api/i18n/locale")
            .withAttribute("currentUser", user(USER_A));
        I18nApiController.setLocale(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("缺少 locale 参数返回 400")
    void setLocaleMissingParamReturns400() {
        TestContext ctx = putLocale(user(USER_A), Map.of("other", "x"));
        I18nApiController.setLocale(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("不支持的语言返回 400")
    void setLocaleUnsupportedReturns400() {
        TestContext ctx = putLocale(user(USER_A), Map.of("locale", "fr-FR"));
        I18nApiController.setLocale(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("设置语言只写当前用户偏好，不改进程级语言")
    void setLocalePersistsPerUserWithoutTouchingGlobalLocale() throws Exception {
        TestContext ctx = putLocale(user(USER_A), Map.of("locale", "en"));
        I18nApiController.setLocale(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertEquals("en", dataOf(ctx).get("locale"));

        // 偏好落在当前用户名下
        assertEquals("en", DAOFactory.getInstance().getLanguagePreferenceDAO()
            .getLanguagePreference(USER_A));
        // 进程级语言不受影响（桌面端 / 其它终端不应被改）
        assertEquals("zh-CN", I18nManager.getInstance().getCurrentLanguageTag());
    }

    @Test
    @DisplayName("一个用户切语言不影响另一个用户解析出的语言")
    void localeIsIsolatedBetweenUsers() throws Exception {
        TestContext setCtx = putLocale(user(USER_A), Map.of("locale", "en"));
        I18nApiController.setLocale(setCtx.context);
        assertEquals(HttpStatus.OK, setCtx.status);

        TestContext userACtx = new TestContext()
            .withRequest(HandlerType.GET, "/api/i18n/locale")
            .withAttribute("currentUser", user(USER_A));
        I18nApiController.getCurrentLocale(userACtx.context);
        assertEquals("en", dataOf(userACtx).get("locale"));

        TestContext userBCtx = new TestContext()
            .withRequest(HandlerType.GET, "/api/i18n/locale")
            .withAttribute("currentUser", user(USER_B));
        I18nApiController.getCurrentLocale(userBCtx.context);
        assertEquals("zh-CN", dataOf(userBCtx).get("locale"));
    }

    @Test
    @DisplayName("取翻译按请求语言渲染且不改动进程级语言")
    void getMessageUsesRequestLocale() {
        I18nManager manager = I18nManager.getInstance();

        TestContext ctx = new TestContext()
            .withRequest(HandlerType.GET, "/api/i18n/messages")
            .withAttribute("currentUser", user(USER_A))
            .withQueryParam("locale", "en")
            .withQueryParam("key", "common.ok");
        I18nApiController.getMessage(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertEquals("en", response(ctx).get("locale"));
        assertEquals(manager.get(I18nManager.ENGLISH, "common.ok"), dataOf(ctx).get("message"));
        assertEquals("zh-CN", manager.getCurrentLanguageTag());
    }
}
