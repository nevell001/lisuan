package com.cashier.model;

import java.sql.Timestamp;

/**
 * 采购审批记录模型
 */
public class PurchaseApproval {
    public int id;                   // 记录ID（数据库自增主键）
    public int orderId;              // 订单ID
    public String approver;          // 审批人
    public String action;            // 审批动作（approve-通过，reject-拒绝）
    public String remark;            // 审批意见
    public Timestamp approvalTime;   // 审批时间

    public PurchaseApproval() {
        this.id = 0;
        this.orderId = 0;
        this.approver = "";
        this.action = "";
        this.remark = "";
        this.approvalTime = new Timestamp(System.currentTimeMillis());
    }

    public PurchaseApproval(int orderId, String approver, String action, String remark) {
        this();
        this.orderId = orderId;
        this.approver = approver;
        this.action = action;
        this.remark = remark;
    }

    /**
     * 获取审批动作显示名称
     */
    public String getActionDisplayName() {
        return "approve".equals(action) ? "通过" : "拒绝";
    }

    /**
     * 是否通过
     */
    public boolean isApproved() {
        return "approve".equals(action);
    }
}