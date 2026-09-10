package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
