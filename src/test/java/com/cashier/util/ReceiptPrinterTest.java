package com.cashier.util;

import com.cashier.model.CartItem;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 小票金额区间测试（无界面、不打印、不落盘）。
 *
 * <p>背景：`total_amount` 统一为**明细原价合计**后，"商品总额"与"实付金额"在一笔打折交易里
 * 就不再相等，小票必须把优惠额列出来，否则顾客看到 100.00 与 90.00 两个数不知道该差在哪。
 * 此前触屏收银台（total=原价、final=折后）的小票就有这个缺口。</p>
 */
@DisplayName("小票金额区间测试")
class ReceiptPrinterTest {

    @Test
    @DisplayName("有优惠时打印优惠行，且商品总额 - 优惠 = 实付金额")
    void discountedTransactionPrintsDiscountLine() {
        Transaction transaction = transaction(new BigDecimal("100.00"), new BigDecimal("90.00"));

        String content = ReceiptPrinter.generateReceiptContent(transaction, cart(), null);

        assertTrue(content.contains("商品总额:"), content);
        assertTrue(content.contains("优惠:"), "打折交易必须打印优惠行：" + content);
        assertTrue(content.contains("-10.00"), "优惠应以负数呈现，便于对账：" + content);
        assertTrue(content.contains("实付金额:"), content);
    }

    @Test
    @DisplayName("无优惠时不打印优惠行")
    void plainTransactionHasNoDiscountLine() {
        Transaction transaction = transaction(new BigDecimal("30.00"), new BigDecimal("30.00"));

        String content = ReceiptPrinter.generateReceiptContent(transaction, cart(), null);

        assertTrue(content.contains("商品总额:"), content);
        assertFalse(content.contains("优惠:"), "没有优惠不该出现优惠行：" + content);
        assertTrue(content.contains("实付金额:"), content);
    }

    @Test
    @DisplayName("金额字段缺失时不抛异常")
    void missingAmountsDoNotBreakReceipt() {
        Transaction transaction = transaction(null, null);

        String content = ReceiptPrinter.generateReceiptContent(transaction, cart(), null);

        assertTrue(content.contains("商品总额:"), content);
        assertFalse(content.contains("优惠:"), content);
    }

    private static Transaction transaction(BigDecimal totalAmount, BigDecimal finalAmount) {
        Transaction transaction = new Transaction();
        transaction.transactionId = "RCPT-TEST-001";
        transaction.timestamp = "2026-09-20 10:00:00";
        transaction.totalAmount = totalAmount;
        transaction.tax = BigDecimal.ZERO;
        transaction.finalAmount = finalAmount;
        transaction.paymentMethod = "现金";
        transaction.items = List.of(product(1, "小票测试商品", new BigDecimal("10.00"), 9));
        return transaction;
    }

    private static List<CartItem> cart() {
        return List.of(new CartItem(product(1, "小票测试商品", new BigDecimal("10.00"), 10), 10));
    }

    private static Product product(int id, String name, BigDecimal price, int quantity) {
        Product product = new Product(id, name, price, quantity, "测试分类", "RCPT-BAR-" + id,
            "件", "描述", "品牌", "供应商", "规格", 0, price);
        product.productCode = "RCPT-" + id;
        return product;
    }
}
