package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    @DisplayName("未设默认打印机时 /api/printers 不得 500（Map.of 不接受 null 值）")
    void listPrintersToleratesMissingDefaultPrinter() {
        // 回归：原写法 ctx.json(Map.of(..., "defaultPrinter", x != null ? id : null, ...))
        // 在没有默认打印机（常态）时恒抛 NPE → 整个列表接口 500，冒烟实测发现。
        com.cashier.printer.PrinterManager manager = com.cashier.printer.PrinterManager.getInstance();
        for (com.cashier.printer.PrinterDevice d : manager.getAllDevices()) {
            manager.unregisterDevice(d.getDeviceId());
        }
        assertEquals(null, manager.getDefaultPrinter(), "前置条件：确实没有默认打印机");

        TestContext ctx = new TestContext()
            .withRequest(HandlerType.GET, "/api/printers")
            .withQueryParam("locale", "zh-CN");
        PrintApiController.listPrinters(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status, "没有默认打印机也必须正常返回列表: " + ctx.json);
        assertEquals(Boolean.TRUE, response(ctx).get("success"));
        assertNotNull(response(ctx).get("data"));
        assertEquals(null, response(ctx).get("defaultPrinter"), "无默认打印机时应为 null 而不是报错");
    }

    @Test
    @DisplayName("设置默认打印机失败时不得 500（同一处 Map.of + null）")
    void setDefaultPrinterToleratesUnknownDevice() {
        TestContext ctx = new TestContext()
            .withRequest(HandlerType.POST, "/api/printers/NOT-EXIST/set-default")
            .withPathParam("id", "NOT-EXIST")
            .withQueryParam("locale", "zh-CN");
        PrintApiController.setDefaultPrinter(ctx.context);
        assertEquals(HttpStatus.OK, ctx.status, "未知设备也不该抛 NPE: " + ctx.json);
    }

    @Test
    @DisplayName("枚举双字段（TD-041）：deviceType/status 是稳定代码，*Name 跟随请求语言")
    void enumFieldsAreCodePlusLocalizedName() {
        com.cashier.printer.NetworkPrinterDevice device =
            new com.cashier.printer.NetworkPrinterDevice("P-TD41", "收银台打印机", "192.168.1.50", 9100);
        com.cashier.printer.PrinterManager.getInstance().registerDevice(device);

        TestContext zh = new TestContext()
            .withRequest(HandlerType.GET, "/api/printers/P-TD41")
            .withPathParam("id", "P-TD41")
            .withQueryParam("locale", "zh-CN");
        PrintApiController.getPrinter(zh.context);
        TestContext en = new TestContext()
            .withRequest(HandlerType.GET, "/api/printers/P-TD41")
            .withPathParam("id", "P-TD41")
            .withQueryParam("locale", "en");
        PrintApiController.getPrinter(en.context);

        Object zhData = response(zh).get("data");
        Object enData = response(en).get("data");
        assertNotNull(zhData);
        assertEquals("NETWORK", deviceTypeOf(zhData), "代码字段必须稳定，与语言无关");
        assertEquals("NETWORK", deviceTypeOf(enData));
        assertEquals("网络打印机", nameOf(zhData, "deviceTypeName"));
        assertEquals("Network printer", nameOf(enData, "deviceTypeName"),
            "显示名必须跟随请求语言（此前无论客户端语言都只返回中文）");
    }

    @SuppressWarnings("unchecked")
    private static String deviceTypeOf(Object data) {
        return (String) ((Map<String, Object>) data).get("deviceType");
    }

    @SuppressWarnings("unchecked")
    private static String nameOf(Object data, String field) {
        return (String) ((Map<String, Object>) data).get(field);
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

    @Test
    @DisplayName("响应文案跟随请求语言（TD-041）：?locale=en 时错误信息为英文")
    void errorMessagesFollowRequestLocale() {
        Map<String, Object> body = new HashMap<>();
        body.put("content", "内容\u001b@含控制字符");
        TestContext zh = new TestContext()
            .withRequest(HandlerType.POST, "/api/printers/P1/receipt")
            .withPathParam("id", "P1")
            .withBody(body)
            .withQueryParam("locale", "zh-CN");
        PrintApiController.printReceipt(zh.context);

        TestContext en = new TestContext()
            .withRequest(HandlerType.POST, "/api/printers/P1/receipt")
            .withPathParam("id", "P1")
            .withBody(body)
            .withQueryParam("locale", "en");
        PrintApiController.printReceipt(en.context);

        assertEquals("打印内容不能包含控制字符", errorOf(zh),
            "默认中文请求仍返回中文（老客户端不受影响）");
        assertEquals("Print content must not contain control characters", errorOf(en),
            "英文请求必须返回英文：文案此前写死中文，切语言无从下手");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
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
