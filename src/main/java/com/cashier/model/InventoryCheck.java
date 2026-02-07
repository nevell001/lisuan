package com.cashier.model;

import java.sql.Timestamp;

/**
 * 库存盘点模型
 */
public class InventoryCheck {
    public int id;                   // 盘点ID（数据库自增主键）
    public String checkNo;           // 盘点单号
    public String checkDate;         // 盘点日期（yyyy-MM-dd）
    public String checkType;         // 盘点类型（full-全盘，partial-部分盘点）
    public int totalItems;           // 盘点商品总数
    public int diffItems;            // 差异商品数
    public String status;            // 盘点状态（pending-待盘点，checking-盘点中，completed-已完成）
    public String operator;          // 盘点人
    public String checker;           // 审核人
    public String remark;            // 备注
    public Timestamp createTime;     // 创建时间
    public Timestamp updateTime;     // 更新时间

    public InventoryCheck() {
        this.id = 0;
        this.checkNo = "";
        this.checkDate = "";
        this.checkType = "full";
        this.totalItems = 0;
        this.diffItems = 0;
        this.status = "pending";
        this.operator = "";
        this.checker = "";
        this.remark = "";
        this.createTime = new Timestamp(System.currentTimeMillis());
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }

    /**
     * 获取盘点类型显示名称
     */
    public String getCheckTypeDisplayName() {
        return "full".equals(checkType) ? "全盘" : "部分盘点";
    }

    /**
     * 获取状态显示名称
     */
    public String getStatusDisplayName() {
        switch (status) {
            case "pending":
                return "待盘点";
            case "checking":
                return "盘点中";
            case "completed":
                return "已完成";
            default:
                return status;
        }
    }

    /**
     * 是否可以编辑
     */
    public boolean canEdit() {
        return "pending".equals(status) || "checking".equals(status);
    }

    /**
     * 是否可以完成
     */
    public boolean canComplete() {
        return "checking".equals(status);
    }
}