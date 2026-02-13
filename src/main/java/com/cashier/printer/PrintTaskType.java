package com.cashier.printer;

/**
 * 打印任务类型枚举
 */
public enum PrintTaskType {
    /**
     * 销售小票
     */
    RECEIPT("销售小票"),
    
    /**
     * 入库单据
     */
    INBOUND("入库单据"),
    
    /**
     * 会员收据
     */
    MEMBER_RECEIPT("会员收据"),
    
    /**
     * 盘点报表
     */
    INVENTORY_REPORT("盘点报表"),
    
    /**
     * 销售统计报表
     */
    SALES_REPORT("销售统计报表"),
    
    /**
     * 利润报表
     */
    PROFIT_REPORT("利润报表"),
    
    /**
     * 采购报表
     */
    PURCHASE_REPORT("采购报表"),
    
    /**
     * 测试打印
     */
    TEST("测试打印"),
    
    /**
     * 其他类型
     */
    OTHER("其他");
    
    private final String displayName;
    
    PrintTaskType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}