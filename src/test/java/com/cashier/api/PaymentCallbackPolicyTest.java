package com.cashier.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付回调验签门禁。
 *
 * <p>回归：回调控制器曾在验签前把 {@code trade_no} 复制成 {@code transaction_id} 塞进回调参数，
 * 而支付宝的待验签内容由全部非空参数拼成，导致真实回调永远验签失败、订单一直停在待支付。
 * 渠道交易号应改为验签后按渠道取值（支付宝 {@code trade_no}，微信 {@code transaction_id}）。</p>
 */
class PaymentCallbackPolicyTest {

    private static String readSource(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/com/cashier", relativePath));
    }

    @Test
    @DisplayName("回调入口不得往待验签参数里注入合成字段")
    void notifyHandlerDoesNotInjectSyntheticParams() throws Exception {
        String controller = readSource("api/controller/PaymentApiController.java");

        assertFalse(controller.contains("notifyData.put(\"transaction_id\""),
            "验签前注入 transaction_id 会让支付宝回调验签必然失败");
    }

    @Test
    @DisplayName("渠道交易号按渠道取值并兜底 trade_no")
    void channelTransactionIdReadsTradeNoForAlipay() throws Exception {
        String service = readSource("service/PaymentService.java");

        assertTrue(service.contains("channelTransactionId(notifyData)"),
            "落库的渠道交易号必须走统一取值方法");
        assertTrue(service.contains("notifyData.get(\"trade_no\")"),
            "支付宝回调用 trade_no，需要兜底读取");
    }
}
