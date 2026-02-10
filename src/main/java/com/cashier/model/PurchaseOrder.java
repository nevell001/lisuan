package com.cashier.model;

import java.sql.Timestamp;
import java.math.BigDecimal;

/**
 * 采购订单模型
 */
public class PurchaseOrder {
    public int id;                   // 订单ID（数据库自增主键）
    public String orderNo;           // 采购订单号
    public int supplierId;           // 供应商ID
    public String supplierName;      // 供应商名称
    public String purchaseDate;      // 采购日期（yyyy-MM-dd）
    public String expectedDate;      // 预计到货日期（yyyy-MM-dd）
    public BigDecimal totalAmount;   // 订单总金额
    public String status;            // 订单状态（pending-待审批，approved-已审批，rejected-已拒绝，completed-已完成）
    public String purchaser;         // 采购人
    public String approver;          // 审批人
    public Timestamp approvalTime;   // 审批时间
    public String approvalRemark;    // 审批意见
    public String remark;            // 备注
    public Timestamp createTime;     // 创建时间
    public Timestamp updateTime;     // 更新时间

    public PurchaseOrder() {
        this.id = 0;
        this.orderNo = "";
        this.supplierId = 0;
        this.supplierName = "";
        this.purchaseDate = "";
        this.expectedDate = "";
        this.totalAmount = BigDecimal.ZERO;
        this.status = "pending";
        this.purchaser = "";
        this.approver = "";
        this.approvalRemark = "";
        this.remark = "";
        this.createTime = new Timestamp(System.currentTimeMillis());
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }

    /**
     * 获取状态显示名称
     */
    public String getStatusDisplayName() {
        switch (status) {
            case "pending":
                return "待审批";
            case "approved":
                return "已审批";
            case "rejected":
                return "已拒绝";
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
        return "pending".equals(status);
    }

    /**
     * 是否可以审批
     */
    public boolean canApprove() {
        return "pending".equals(status);
    }

    /**
     * 是否可以入库
     */
    public boolean canInbound() {
        return "approved".equals(status);
    }

    // Getter 方法
    public int getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public String getExpectedDate() {
        return expectedDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getPurchaser() {
        return purchaser;
    }

    public String getApprover() {
        return approver;
    }

    public Timestamp getApprovalTime() {
        return approvalTime;
    }

    public String getApprovalRemark() {
        return approvalRemark;
    }

    public String getRemark() {
        return remark;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }
}