package com.cashier.model;

import java.math.BigDecimal;

public class InventoryStatistics {
    private final int totalProducts;
    private final int lowStockCount;
    private final int totalQuantity;
    private final BigDecimal totalValue;

    public InventoryStatistics(int totalProducts, int lowStockCount, int totalQuantity, BigDecimal totalValue) {
        this.totalProducts = totalProducts;
        this.lowStockCount = lowStockCount;
        this.totalQuantity = totalQuantity;
        this.totalValue = totalValue;
    }

    public int getTotalProducts() {
        return totalProducts;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
