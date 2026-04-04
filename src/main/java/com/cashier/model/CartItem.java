package com.cashier.model;

import java.math.BigDecimal;

/**
 * 购物车项类
 * 表示购物车中的商品项
 */
public class CartItem {
    public Product product;  // 商品对象
    public int quantity;     // 数量
    public BigDecimal subtotal;  // 小计

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.subtotal = BigDecimal.ZERO;
        updateSubtotal();
    }

    /**
     * 更新小计
     */
    public void updateSubtotal() {
        BigDecimal price = product != null && product.price != null ? product.price : BigDecimal.ZERO;
        this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 增加数量
     * @param delta 增加的数量
     */
    public void addQuantity(int delta) {
        this.quantity += delta;
        updateSubtotal();
    }

    /**
     * 设置数量
     * @param quantity 数量
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        updateSubtotal();
    }
}