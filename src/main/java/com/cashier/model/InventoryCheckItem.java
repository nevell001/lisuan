package com.cashier.model;

import java.sql.Timestamp;

/**
 * 库存盘点明细模型
 */
public class InventoryCheckItem {
    public int id;                   // 明细ID（数据库自增主键）
    public int checkId;              // 盘点单ID
    public int productId;            // 商品ID
    public String productName;       // 商品名称
    public int bookQuantity;         // 账面数量
    public int actualQuantity;       // 实际数量
    public int diffQuantity;         // 差异数量
    public String diffReason;        // 差异原因
    public Timestamp createTime;     // 创建时间

    public InventoryCheckItem() {
        this.id = 0;
        this.checkId = 0;
        this.productId = 0;
        this.productName = "";
        this.bookQuantity = 0;
        this.actualQuantity = 0;
        this.diffQuantity = 0;
        this.diffReason = "";
        this.createTime = new Timestamp(System.currentTimeMillis());
    }

    public InventoryCheckItem(int checkId, int productId, String productName,
                               int bookQuantity, int actualQuantity) {
        this();
        this.checkId = checkId;
        this.productId = productId;
        this.productName = productName;
        this.bookQuantity = bookQuantity;
        this.actualQuantity = actualQuantity;
        this.diffQuantity = actualQuantity - bookQuantity;
    }

    /**
     * 计算差异数量
     */
    public void calculateDiff() {
        this.diffQuantity = actualQuantity - bookQuantity;
    }

    /**
     * 是否有差异
     */
    public boolean hasDifference() {
        return diffQuantity != 0;
    }

    /**
     * 获取差异类型
     */
    public String getDiffType() {
        if (diffQuantity > 0) {
            return "盘盈";
        } else if (diffQuantity < 0) {
            return "盘亏";
        } else {
            return "无差异";
        }
    }
}