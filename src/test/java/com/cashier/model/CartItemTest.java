package com.cashier.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 购物车行的小计必须跟着**库里的最新价格**走（2026-10 审计 F5）。
 */
@DisplayName("购物车行测试")
class CartItemTest {

    private static Product product(String name, String price) {
        Product product = new Product();
        product.name = name;
        product.price = new BigDecimal(price);
        product.quantity = 100;
        return product;
    }

    private static void assertAmount(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    @DisplayName("refreshProduct：换成新价格的行后小计重算；传 null 保持原样")
    void refreshProductRepricesLine() {
        CartItem item = new CartItem(product("改价商品", "10.00"), 2);
        assertAmount(new BigDecimal("20.00"), item.subtotal);

        item.refreshProduct(product("改价商品", "20.00"));
        assertAmount(new BigDecimal("40.00"), item.subtotal);

        // 查库失败（null）时不做静默兜底，也不改坏当前行
        item.refreshProduct(null);
        assertAmount(new BigDecimal("40.00"), item.subtotal);
        assertEquals(2, item.quantity);
    }
}
