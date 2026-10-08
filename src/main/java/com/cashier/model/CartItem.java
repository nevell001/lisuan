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
     * 用刚查到的商品行刷新本行，并按新价格重算小计。
     *
     * <p>加购/改数量时会重新查库拿最新行；只更新数量而不换商品对象的话，改价后这一行仍按
     * **首次加购时**的旧价结算（2026-10 审计 F5）。传 null（查库失败）时保持原样，
     * 由调用方决定是否提示——这里不做静默兜底。</p>
     */
    public void refreshProduct(Product refreshed) {
        if (refreshed != null) {
            this.product = refreshed;
            updateSubtotal();
        }
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