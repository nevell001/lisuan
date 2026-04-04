package com.cashier.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 商品规格关联模型类
 */
public class ProductSpecification {
    public int id;
    public int productId;           // 商品ID
    public int specificationId;     // 规格类型ID
    public int specificationValueId;// 规格值ID
    public String skuCode;          // SKU编码
    public BigDecimal priceAdjustment;  // 价格调整值（可为正数或负数）
    public int quantity;            // 库存数量（如果不同规格有独立库存）
    public String barcode;          // 条形码（不同规格可能有不同条形码）
    public boolean enabled;          // 是否启用
    public java.util.Date createTime;
    public java.util.Date updateTime;

    public ProductSpecification() {
        this.priceAdjustment = BigDecimal.ZERO;
        this.quantity = 0;
        this.enabled = true;
        this.createTime = new java.util.Date();
        this.updateTime = new java.util.Date();
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // Getter方法
    public int getId() {
        return id;
    }

    public int getProductId() {
        return productId;
    }

    public int getSpecificationId() {
        return specificationId;
    }

    public int getSpecificationValueId() {
        return specificationValueId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public BigDecimal getPriceAdjustment() {
        return defaultDecimal(priceAdjustment);
    }

    public int getQuantity() {
        return quantity;
    }

    public String getBarcode() {
        return barcode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public java.util.Date getUpdateTime() {
        return updateTime;
    }

    // Setter方法
    public void setId(int id) {
        this.id = id;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public void setSpecificationId(int specificationId) {
        this.specificationId = specificationId;
    }

    public void setSpecificationValueId(int specificationValueId) {
        this.specificationValueId = specificationValueId;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public void setPriceAdjustment(BigDecimal priceAdjustment) {
        this.priceAdjustment = defaultDecimal(priceAdjustment);
    }

    public void setPriceAdjustment(double priceAdjustment) {
        this.priceAdjustment = BigDecimal.valueOf(priceAdjustment);
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(java.util.Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取启用状态文本
     */
    public String getEnabledText() {
        return enabled ? "启用" : "禁用";
    }

    /**
     * 获取价格调整文本
     */
    public String getPriceAdjustmentText() {
        BigDecimal value = defaultDecimal(priceAdjustment);
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        } else if (value.compareTo(BigDecimal.ZERO) < 0) {
            return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        } else {
            return "0.00";
        }
    }
}