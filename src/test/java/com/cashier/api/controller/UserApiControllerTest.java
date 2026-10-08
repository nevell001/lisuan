package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.model.User;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserApiControllerTest extends DatabaseTestBase {

    private static User adminUser() {
        User user = new User();
        user.id = 1;
        user.username = "admin";
        user.role = "admin";
        return user;
    }

    private static User cashierUser() {
        User user = new User();
        user.id = 2;
        user.username = "cashier";
        user.role = "cashier";
        return user;
    }

    private User insertUser(String username) throws Exception {
        User user = new User(username, "hashed", username + "名称", "收银员");
        assertTrue(DAOFactory.getInstance().getUserDAO().insert(user));
        return user;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("未认证访问用户列表返回 401")
    void listRequiresAuthentication() {
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/users");
        UserApiController.list(ctx.context);

        assertEquals(HttpStatus.UNAUTHORIZED, ctx.status);
    }

    @Test
    @DisplayName("非管理员访问用户列表返回 403")
    void listRejectsNonAdmin() {
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/users")
            .withAttribute("currentUser", cashierUser());
        UserApiController.list(ctx.context);

        assertEquals(HttpStatus.FORBIDDEN, ctx.status);
    }

    @Test
    @DisplayName("管理员获取用户列表")
    void listAsAdmin() throws Exception {
        insertUser("listuser01");
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/users")
            .withAttribute("currentUser", adminUser());
        UserApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("不存在的用户详情返回 404")
    void getMissingUserReturns404() {
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/users/999999")
            .withPathParam("id", "999999")
            .withAttribute("currentUser", adminUser());
        UserApiController.get(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("管理员创建用户返回 201")
    void createAsAdmin() {
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.username = "newcashier";
        request.password = "pass123";
        request.name = "新收银员";
        request.role = "收银员";

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/users")
            .withAttribute("currentUser", adminUser())
            .withBody(request);
        UserApiController.create(ctx.context);

        assertEquals(HttpStatus.CREATED, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("重复用户名创建用户返回 400")
    void createDuplicateUsernameRejected() throws Exception {
        insertUser("dupuser");
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.username = "dupuser";
        request.password = "pass123";

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/users")
            .withAttribute("currentUser", adminUser())
            .withBody(request);
        UserApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体创建用户返回 400")
    void createWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/users")
            .withAttribute("currentUser", adminUser());
        UserApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空口令建号返回 400：空串不是有效凭据（否则该账号可用空口令登录）")
    void createWithEmptyPasswordRejected() throws Exception {
        for (String blank : new String[] {"", "   "}) {
            UserApiController.UserRequest request = new UserApiController.UserRequest();
            request.username = "nopass" + blank.length();
            request.password = blank;

            TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/users")
                .withAttribute("currentUser", adminUser())
                .withBody(request);
            UserApiController.create(ctx.context);

            assertEquals(HttpStatus.BAD_REQUEST, ctx.status, "空口令必须回 400");
            assertNull(DAOFactory.getInstance().getUserDAO().findByUsername(request.username),
                "空口令账号不得被创建");
        }
    }

    @Test
    @DisplayName("空用户名建号返回 400")
    void createWithBlankUsernameRejected() {
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.username = "   ";
        request.password = "pass123";

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/users")
            .withAttribute("currentUser", adminUser())
            .withBody(request);
        UserApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("不能删除自己的账号")
    void deleteSelfRejected() throws Exception {
        User saved = insertUser("selfuser");
        User admin = adminUser();
        admin.id = saved.id;
        TestContext ctx = new TestContext().withRequest(HandlerType.DELETE, "/api/users/1")
            .withPathParam("id", String.valueOf(saved.id))
            .withAttribute("currentUser", admin);
        UserApiController.delete(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("删除不存在的用户返回 404")
    void deleteMissingUserReturns404() {
        TestContext ctx = new TestContext().withRequest(HandlerType.DELETE, "/api/users/999999")
            .withPathParam("id", "999999")
            .withAttribute("currentUser", adminUser());
        UserApiController.delete(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }
}
