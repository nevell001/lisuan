package com.cashier.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 交易单号生成测试
 * 回归 P1：单号 = ORD + 17 位毫秒时间戳 + 4 位序号 + 8 位进程随机段，
 * 保证同一进程内唯一；随机段使多进程/重启后同毫秒生成也不会撞 transactions 主键。
 */
@DisplayName("交易单号生成测试")
class OrderNumberGenerationTest {

    private static final Pattern ORDER_PATTERN =
        Pattern.compile("^ORD\\d{17}\\d{4}[0-9a-f]{8}$");

    @Test
    @DisplayName("单号格式含时间戳/序号/随机段，长度不超主键上限")
    void formatIsStableAndBounded() {
        String id = TransactionService.generateOrderNumber();

        assertTrue(ORDER_PATTERN.matcher(id).matches(),
            "单号格式应为 ORD+yyyyMMddHHmmssSSS+4位序号+8位随机段: " + id);
        assertTrue(id.length() <= 50, "单号长度应在 transactions.transaction_id VARCHAR(50) 范围内");
    }

    @Test
    @DisplayName("连续生成大量单号无重复")
    void generatesUniqueOrderNumbers() {
        int count = 5000;
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < count; i++) {
            ids.add(TransactionService.generateOrderNumber());
        }
        assertEquals(count, ids.size(), "生成的单号不应重复");
    }

    @Test
    @DisplayName("带前缀的单号生成器与 ORD 单号同一格式（含序号与随机段）")
    void prefixedTransactionIdUsesSameHardening() {
        String id = TransactionService.generateTransactionId("T");

        assertTrue(id.matches("^T\\d{17}\\d{4}[0-9a-f]{8}$"), "带前缀单号格式应与 ORD 单号一致: " + id);
        assertTrue(id.length() <= 50, "带前缀单号也应落在 VARCHAR(50) 内");
    }

    @Test
    @DisplayName("桌面收银台与 REST API 都不得残留裸毫秒单号")
    void allCheckoutPathsUseHardenedTransactionId() throws Exception {
        String cartController = Files.readString(
            Path.of("src/main/java/com/cashier/controller/CartController.java"));
        String transactionApi = Files.readString(
            Path.of("src/main/java/com/cashier/api/controller/TransactionApiController.java"));

        assertTrue(cartController.contains("TransactionService.generateOrderNumber()"),
            "标准收银台必须复用加固过的单号生成器，不能自建 ORD+毫秒 单号");
        assertTrue(!cartController.contains("\"ORD\" + ts"),
            "标准收银台残留裸毫秒单号：同毫秒/跨终端会撞 transactions 主键、整单回滚");
        assertTrue(transactionApi.contains("TransactionService.generateTransactionId(\"T\")"),
            "REST API 下单必须使用带序号与随机段的单号");
        assertTrue(!transactionApi.contains("\"T\" + LocalDateTime.now()"),
            "REST API 残留裸毫秒单号");
    }
}
