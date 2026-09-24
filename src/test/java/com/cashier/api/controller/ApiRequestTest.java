package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.util.DatabaseTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 请求体解析门禁。
 *
 * <p>回归：{@code ctx.bodyAsClass(...)} 在字段名写错、字段类型不符、请求体为空或不是合法 JSON 时
 * 都会<b>抛异常</b>，而各控制器的 {@code catch (Exception e) → 500} 会把它兜成「服务器内部错误」。
 * 实测：给 {@code POST /api/products} 传一个不存在的 {@code stock} 字段（真实字段是 {@code quantity}）
 * 返回的就是 500，日志里只有一段 Jackson 堆栈；而每个调用点后面本来就有
 * {@code if (request == null) → 400} 的判断。所以解析失败必须收敛成 400。</p>
 */
class ApiRequestTest extends DatabaseTestBase {

    @Test
    @DisplayName("请求体写错回 400，而不是 500")
    void badBodyYieldsBadRequestNotServerError() {
        TestContext ctx = new TestContext()
            .withRequest(HandlerType.POST, "/api/products")
            .withBodyParseFailure(new BadRequestResponse("Failed to deserialize request body"));

        ProductApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status,
            "请求体不合法必须回 400；回 500 会让客户端以为服务端故障");
    }

    @Test
    @DisplayName("未知字段的提示要指明字段名与可用字段（用真实 Jackson 异常验证）")
    void unknownFieldMessageNamesFieldAndKnownFields() {
        UnrecognizedPropertyException real = assertThrows(UnrecognizedPropertyException.class, () ->
            new ObjectMapper().readValue("{\"name\":\"x\",\"stock\":5}",
                ProductApiController.ProductRequest.class));

        String detail = ApiRequest.describe(real);

        assertTrue(detail.contains("stock"), "要指出写错的字段名，便于直接改：" + detail);
        assertTrue(detail.contains("quantity"), "要列出可用字段供对照（stock 的正确写法是 quantity）：" + detail);
    }

    @Test
    @DisplayName("空请求体也归为 400")
    void emptyBodyIsBadRequest() {
        assertTrue(ApiRequest.isClientBodyError(new BadRequestResponse("Request body is empty")));
        assertEquals("请求体为空或不是合法 JSON",
            ApiRequest.describe(new BadRequestResponse("Request body is empty")));
    }

    @Test
    @DisplayName("服务端自身故障仍按 500 处理，不得被伪装成 400")
    void serverSideFailureStaysServerError() {
        assertFalse(ApiRequest.isClientBodyError(new IllegalStateException("数据库连接池不可用")),
            "只有 Jackson/Javalin 的解析异常才算客户端请求体问题");

        TestContext ctx = new TestContext()
            .withRequest(HandlerType.POST, "/api/products")
            .withBodyParseFailure(new IllegalStateException("数据库连接池不可用"));

        ProductApiController.create(ctx.context);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ctx.status,
            "服务端故障不能回 400——那会把排查方向引到客户端身上");
    }

    @Test
    @DisplayName("缺必填字段回 400，而不是等 DAO 抛 SQLException 变 500")
    void missingRequiredProductFieldsYieldBadRequest() {
        TestContext ctx = new TestContext()
            .withRequest(HandlerType.POST, "/api/products")
            .withBody(new ProductApiController.ProductRequest());   // productCode / name 均为 null

        ProductApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status,
            "缺必填字段属于客户端错误；等 DAO 抛 SQLException 会被兜成 500");
        assertTrue(String.valueOf(ctx.json).contains("productCode"),
            "要指明缺的是哪个字段，客户端才知道怎么改：" + ctx.json);
    }

    @Test
    @DisplayName("所有控制器必须统一走 ApiRequest.parse，不得再直接调 ctx.bodyAsClass")
    void allControllersParseThroughApiRequest() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/cashier/api"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("ApiRequest.java")) {
                    continue;   // 助手自身是唯一允许直接调用 bodyAsClass 的地方
                }
                if (Files.readString(file).contains("ctx.bodyAsClass(")) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "这些控制器绕过了 ApiRequest.parse，请求体写错会回 500：" + offenders);
    }
}
