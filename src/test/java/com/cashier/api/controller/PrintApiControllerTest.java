package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 网络小票打印接口的入参加固测试。
 *
 * <p>回归：{@code POST /api/printers/:id/receipt} 曾把调用方给的 {@code content} 原样交给打印机，
 * 任意登录用户都能把任意字节（含 ESC/POS 指令）推到门店打印机。</p>
 */
@DisplayName("网络小票打印入参加固")
class PrintApiControllerTest {

    private static TestContext receiptRequest(String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        return new TestContext()
            .withRequest(HandlerType.POST, "/api/printers/P1/receipt")
            .withPathParam("id", "P1")
            .withBody(body);
    }

    private static TestContext cashDrawerRequest(String role) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", "小票内容");
        body.put("openCashDrawer", true);
        TestContext ctx = new TestContext()
            .withRequest(HandlerType.POST, "/api/printers/P1/receipt")
            .withPathParam("id", "P1")
            .withBody(body);
        if (role != null) {
            com.cashier.model.User user = new com.cashier.model.User();
            user.username = role + "01";
            user.role = role;
            ctx.withAttribute("currentUser", user);
        }
        return ctx;
    }

    @Test
    @DisplayName("收银员不能借小票打印开启钱箱（TD-037）：403 且不落到打印环节")
    void cashierCannotOpenCashDrawerThroughReceipt() {
        TestContext ctx = cashDrawerRequest("cashier");
        PrintApiController.printReceipt(ctx.context);

        assertEquals(HttpStatus.FORBIDDEN, ctx.status, "开钱箱是受控操作，必须与 /cashdrawer 同口径");
        assertEquals("只有管理员可以开启钱箱", errorOf(ctx));
    }

    @Test
    @DisplayName("取不到认证用户时同样拒绝开钱箱（fail-closed）")
    void missingUserCannotOpenCashDrawer() {
        TestContext ctx = cashDrawerRequest(null);
        PrintApiController.printReceipt(ctx.context);

        assertEquals(HttpStatus.FORBIDDEN, ctx.status);
    }

    @Test
    @DisplayName("管理员可以开钱箱：不再被 403 拦下（后续按打印机状态处理）")
    void adminMayOpenCashDrawer() {
        TestContext ctx = cashDrawerRequest("admin");
        PrintApiController.printReceipt(ctx.context);

        assertTrue(ctx.status != HttpStatus.FORBIDDEN, "管理员不应被开钱箱的角色检查拦下");
    }

    @SuppressWarnings("unchecked")
    private static String errorOf(TestContext ctx) {
        return (String) ((Map<String, Object>) ctx.json).get("error");
    }

    @Test
    @DisplayName("含 ESC/POS 控制字符的小票内容被拒绝")
    void controlCharactersAreRejected() {
        TestContext ctx = receiptRequest("正常文本\u001b@\u001d\u0056\u0000切纸指令");

        PrintApiController.printReceipt(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertEquals("打印内容不能包含控制字符", errorOf(ctx));
    }

    @Test
    @DisplayName("超长小票内容被拒绝")
    void oversizedContentIsRejected() {
        TestContext ctx = receiptRequest("A".repeat(9000));

        PrintApiController.printReceipt(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertEquals("打印内容过长", errorOf(ctx));
    }

    @Test
    @DisplayName("合法纯文本通过内容校验（之后因未配置打印机而失败）")
    void plainTextPassesContentValidation() {
        TestContext ctx = receiptRequest("狸算收银\n商品A x1   10.00\n合计      10.00\n");

        PrintApiController.printReceipt(ctx.context);

        // 内容校验已通过，失败原因是测试环境没有可用打印机
        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertEquals("打印机未连接", errorOf(ctx));
    }
}
