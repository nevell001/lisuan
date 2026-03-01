package com.cashier.model;

/**
 * 退货订单明细模型类
 */
public class ReturnOrderItem {
    public int id;
    public String returnOrderId;  // 退货单号
    public int productId;  // 商品ID
    public String productCode;  // 商品编号
    public String productName;  // 商品名称
    public String barcode;  // 条形码
    public String category;  // 分类
    public int returnQuantity;  // 退货数量
    public double unitPrice;  // 单价
    public double returnAmount;  // 退货金额（退货数量 * 单价）
    public String reason;  // 退货原因（商品级别）
    public String condition;  // 商品状态：GOOD（完好）、DAMAGED（损坏）、OPENED（已拆封）

    public ReturnOrderItem() {
        this.returnQuantity = 0;
        this.unitPrice = 0.0;
        this.returnAmount = 0.0;
        this.condition = "GOOD";
    }

    public void calculateAmount() {
        this.returnAmount = this.returnQuantity * this.unitPrice;
    }

    public String getConditionText() {
        switch (condition) {
            case "GOOD":
                return "完好";
            case "DAMAGED":
                return "损坏";
            case "OPENED":
                return "已拆封";
            default:
                return condition;
        }
    }
}