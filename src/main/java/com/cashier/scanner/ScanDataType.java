package com.cashier.scanner;

/**
 * 扫描数据类型枚举
 */
public enum ScanDataType {
    /**
     * 条形码
     */
    BARCODE("条形码"),
    
    /**
     * 二维码
     */
    QR_CODE("二维码"),
    
    /**
     * 商品编号
     */
    PRODUCT_CODE("商品编号"),
    
    /**
     * 会员卡号
     */
    MEMBER_CARD("会员卡号"),
    
    /**
     * 未知类型
     */
    UNKNOWN("未知");
    
    private final String displayName;
    
    ScanDataType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}