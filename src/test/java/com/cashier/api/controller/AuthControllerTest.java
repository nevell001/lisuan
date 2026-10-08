package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.api.support.TestContext;
import com.cashier.model.User;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthControllerTest {

    @Test
    @DisplayName("空请求体登录返回 400")
    void loginWithNullBodyReturns400() {
        TestContext ctx = new TestContext();

        AuthController.login(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空口令登录返回 400：不得把空串当成有效凭据去验密")
    void loginWithEmptyPasswordReturns400() {
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.username = "admin";
        request.password = "";

        TestContext ctx = new TestContext()
            .withRequest(io.javalin.http.HandlerType.POST, "/api/auth/login")
            .withBody(request);

        AuthController.login(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertEquals(false, response(ctx).get("success"));
    }

    @Test
    @DisplayName("未认证用户不能读取当前用户")
    void currentUserRequiresAuthentication() {
        TestContext ctx = new TestContext();

        AuthController.getCurrentUser(ctx.context);

        assertEquals(HttpStatus.UNAUTHORIZED, ctx.status);
        assertEquals(false, response(ctx).get("success"));
    }

    @Test
    @DisplayName("当前用户响应必须移除密码")
    void currentUserResponseRemovesPassword() {
        User user = new User();
        user.id = 7;
        user.username = "cashier";
        user.password = "hashed-secret";
        TestContext ctx = new TestContext().withAttribute("currentUser", user);

        AuthController.getCurrentUser(ctx.context);

        assertNull(user.password);
        assertTrue((Boolean) response(ctx).get("success"));
        assertSame(user, response(ctx).get("user"));
    }

    @Test
    @DisplayName("注销会立即使 Token 失效")
    void logoutInvalidatesToken() {
        User user = new User();
        user.id = 7;
        String token = ApiServer.getInstance().generateToken(user);
        TestContext ctx = new TestContext().withHeader("Authorization", "Bearer " + token);

        AuthController.logout(ctx.context);

        assertTrue((Boolean) response(ctx).get("success"));
        assertNull(ApiServer.getInstance().validateToken(token));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }
}
