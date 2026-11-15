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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    /** 与目标用户不重合的当前登录管理员（避免命中"不能删除自己"分支）。 */
    private static User otherAdmin() {
        User user = new User();
        user.id = 999;
        user.username = "otheradmin";
        user.role = "admin";
        return user;
    }

    private User insertUser(String username) throws Exception {
        return insertUser(username, "cashier");
    }

    private User insertUser(String username, String role) throws Exception {
        User user = new User(username, "hashed", username + "名称", role);
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
        request.role = "cashier";

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

    @Test
    @DisplayName("弱口令建号返回 400（过短 / 缺字母 / 缺数字）")
    void createWithWeakPasswordRejected() throws Exception {
        String[] weakPasswords = {"abc", "abcdef", "123456"};
        for (int i = 0; i < weakPasswords.length; i++) {
            UserApiController.UserRequest request = new UserApiController.UserRequest();
            request.username = "weakpass" + i;
            request.password = weakPasswords[i];

            TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/users")
                .withAttribute("currentUser", adminUser())
                .withBody(request);
            UserApiController.create(ctx.context);

            assertEquals(HttpStatus.BAD_REQUEST, ctx.status, "弱口令必须被拒绝: " + weakPasswords[i]);
            assertNull(DAOFactory.getInstance().getUserDAO().findByUsername(request.username),
                "弱口令账号不得被创建");
        }
    }

    @Test
    @DisplayName("非法角色建号返回 400")
    void createWithInvalidRoleRejected() throws Exception {
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.username = "badroleuser";
        request.password = "pass123";
        request.role = "manager";

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/users")
            .withAttribute("currentUser", adminUser())
            .withBody(request);
        UserApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertNull(DAOFactory.getInstance().getUserDAO().findByUsername(request.username));
    }

    @Test
    @DisplayName("未指定角色时默认收银员")
    void createWithoutRoleDefaultsToCashier() throws Exception {
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.username = "defaultrole";
        request.password = "pass123";

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/users")
            .withAttribute("currentUser", adminUser())
            .withBody(request);
        UserApiController.create(ctx.context);

        assertEquals(HttpStatus.CREATED, ctx.status);
        assertEquals("cashier",
            DAOFactory.getInstance().getUserDAO().findByUsername(request.username).role);
    }

    @Test
    @DisplayName("最后一个启用管理员不得被删除")
    void deleteLastActiveAdminRejected() throws Exception {
        User lastAdmin = insertUser("solo-admin", "admin");

        TestContext ctx = new TestContext().withRequest(HandlerType.DELETE, "/api/users/" + lastAdmin.id)
            .withPathParam("id", String.valueOf(lastAdmin.id))
            .withAttribute("currentUser", otherAdmin());
        UserApiController.delete(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertNotNull(DAOFactory.getInstance().getUserDAO().findByUsername("solo-admin"),
            "最后一个管理员不得被删除");
    }

    @Test
    @DisplayName("还有其他启用管理员时可以删除其中一个")
    void deleteAdminAllowedWhenAnotherActiveAdminExists() throws Exception {
        User first = insertUser("admin-a", "admin");
        insertUser("admin-b", "admin");

        TestContext ctx = new TestContext().withRequest(HandlerType.DELETE, "/api/users/" + first.id)
            .withPathParam("id", String.valueOf(first.id))
            .withAttribute("currentUser", otherAdmin());
        UserApiController.delete(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertNull(DAOFactory.getInstance().getUserDAO().findByUsername("admin-a"));
    }

    @Test
    @DisplayName("最后一个启用管理员不得被降级")
    void demoteLastActiveAdminRejected() throws Exception {
        User lastAdmin = insertUser("solo-admin", "admin");
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.role = "cashier";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/users/" + lastAdmin.id)
            .withPathParam("id", String.valueOf(lastAdmin.id))
            .withAttribute("currentUser", otherAdmin())
            .withBody(request);
        UserApiController.update(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertEquals("admin",
            DAOFactory.getInstance().getUserDAO().findByUsername("solo-admin").role);
    }

    @Test
    @DisplayName("最后一个启用管理员不得被禁用")
    void deactivateLastActiveAdminRejected() throws Exception {
        User lastAdmin = insertUser("solo-admin", "admin");
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.active = false;

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/users/" + lastAdmin.id)
            .withPathParam("id", String.valueOf(lastAdmin.id))
            .withAttribute("currentUser", otherAdmin())
            .withBody(request);
        UserApiController.update(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertTrue(DAOFactory.getInstance().getUserDAO().findByUsername("solo-admin").active,
            "最后一个管理员不得被禁用");
    }

    @Test
    @DisplayName("还有其他启用管理员时允许降级")
    void demoteAdminAllowedWhenAnotherActiveAdminExists() throws Exception {
        User first = insertUser("admin-a", "admin");
        insertUser("admin-b", "admin");
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.role = "finance";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/users/" + first.id)
            .withPathParam("id", String.valueOf(first.id))
            .withAttribute("currentUser", otherAdmin())
            .withBody(request);
        UserApiController.update(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertEquals("finance",
            DAOFactory.getInstance().getUserDAO().findByUsername("admin-a").role);
    }

    @Test
    @DisplayName("非法角色更新返回 400")
    void updateWithInvalidRoleRejected() throws Exception {
        User user = insertUser("roleuser");
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.role = "manager";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/users/" + user.id)
            .withPathParam("id", String.valueOf(user.id))
            .withAttribute("currentUser", adminUser())
            .withBody(request);
        UserApiController.update(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertEquals("cashier",
            DAOFactory.getInstance().getUserDAO().findByUsername("roleuser").role);
    }

    @Test
    @DisplayName("改密为弱口令返回 400")
    void updateWithWeakPasswordRejected() throws Exception {
        User user = insertUser("pwduser");
        UserApiController.UserRequest request = new UserApiController.UserRequest();
        request.password = "123456";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/users/" + user.id)
            .withPathParam("id", String.valueOf(user.id))
            .withAttribute("currentUser", adminUser())
            .withBody(request);
        UserApiController.update(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }
}
