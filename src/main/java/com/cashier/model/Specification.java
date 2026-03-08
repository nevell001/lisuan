package com.cashier.model;

/**
 * 商品规格类型模型类
 */
public class Specification {
    public int id;
    public String name;              // 规格名称（如：颜色、尺寸、材质）
    public String code;              // 规格代码
    public String type;              // 规格类型：COLOR（颜色）、SIZE（尺寸）、MATERIAL（材质）、OTHER（其他）
    public String description;       // 规格描述
    public int sortOrder;            // 排序序号
    public boolean enabled;          // 是否启用
    public java.util.Date createTime;
    public java.util.Date updateTime;

    public Specification() {
        this.sortOrder = 0;
        this.enabled = true;
        this.createTime = new java.util.Date();
        this.updateTime = new java.util.Date();
    }

    // Getter方法
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
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
     * 获取规格类型文本
     */
    public String getTypeText() {
        switch (type) {
            case "COLOR":
                return "颜色";
            case "SIZE":
                return "尺寸";
            case "MATERIAL":
                return "材质";
            case "OTHER":
                return "其他";
            default:
                return type;
        }
    }

    /**
     * 获取启用状态文本
     */
    public String getEnabledText() {
        return enabled ? "启用" : "禁用";
    }
}