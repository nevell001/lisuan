package com.cashier.model;

import java.math.BigDecimal;

public class TransactionStatistics {
    private final int totalTransactions;
    private final BigDecimal totalAmount;
    private final int totalItems;
    private final int cashCount;
    private final int memberCount;

    public TransactionStatistics(int totalTransactions, BigDecimal totalAmount, int totalItems, int cashCount, int memberCount) {
        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount;
        this.totalItems = totalItems;
        this.cashCount = cashCount;
        this.memberCount = memberCount;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getCashCount() {
        return cashCount;
    }

    public int getMemberCount() {
        return memberCount;
    }
}
