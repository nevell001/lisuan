package com.cashier.model;

/**
 * 商品规格值模型类
 */
public class SpecificationValue {
    public int id;
    public int specificationId;      // 规格类型ID
    public String value;             // 规格值（如：红色、XL、棉质）
    public String code;              // 规格值代码
    public String colorCode;         // 颜色代码（用于颜色规格，如：#FF0000）
    public int sortOrder;            // 排序序号
    public boolean enabled;          // 是否启用
    public java.util.Date createTime;
    public java.util.Date updateTime;

    public SpecificationValue() {
        this.sortOrder = 0;
        this.enabled = true;
        this.createTime = new java.util.Date();
        this.updateTime = new java.util.Date();
    }

    // Getter方法
    public int getId() {
        return id;
    }

    public int getSpecificationId() {
        return specificationId;
    }

    public String getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }

    public String getColorCode() {
        return colorCode;
    }

    public int getSortOrder() {
        return sortOrder;
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

    public void setSpecificationId(int specificationId) {
        this.specificationId = specificationId;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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
}