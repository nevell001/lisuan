package com.cashier.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * 退货订单模型类
 */
public class ReturnOrder {
    public int id;
    public String returnOrderId;  // 退货单号（格式：R + 年月日 + 4位序号）
    public String originalTransactionId;  // 原交易ID
    public Integer memberId;  // 会员ID（可选）
    public String memberName;  // 会员名称
    public Date returnDate;  // 退货日期
    public String returnReason;  // 退货原因
    public BigDecimal totalAmount;  // 退货总金额
    public String status;  // 状态：PENDING（待审批）、APPROVED（已批准）、REJECTED（已拒绝）、COMPLETED（已完成）
    public String paymentMethod;  // 退款方式：CASH（现金）、WECHAT（微信）、ALIPAY（支付宝）、CARD（银行卡）
    public String operatorName;  // 操作员
    public String approverName;  // 审批人
    public Date approvalDate;  // 审批日期
    public String approvalComment;  // 审批意见
    public Date completedDate;  // 完成日期
    public String notes;  // 备注
    public Date createTime;  // 创建时间
    public Date updateTime;  // 更新时间

    public ReturnOrder() {
        this.returnDate = new Date();
        this.createTime = new Date();
        this.updateTime = new Date();
        this.status = "PENDING";
        this.totalAmount = BigDecimal.ZERO;
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // Getter 方法
    public int getId() {
        return id;
    }

    public String getReturnOrderId() {
        return returnOrderId;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public Date getReturnDate() {
        return returnDate;
    }

    public String getReturnDateFormatted() {
        return returnDate != null ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(returnDate) : "";
    }

    public String getReturnReason() {
        return returnReason;
    }

    public BigDecimal getTotalAmount() {
        return defaultDecimal(totalAmount);
    }

    public String getTotalAmountFormatted() {
        return com.cashier.util.CurrencyUtil.format(getTotalAmount().doubleValue());
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getApproverName() {
        return approverName;
    }

    public Date getApprovalDate() {
        return approvalDate;
    }

    public String getApprovalComment() {
        return approvalComment;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public String getNotes() {
        return notes;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public String getStatusText() {
        switch (status) {
            case "PENDING":
                return "待审批";
            case "APPROVED":
                return "已批准";
            case "REJECTED":
                return "已拒绝";
            case "COMPLETED":
                return "已完成";
            default:
                return status;
        }
    }

    public String getPaymentMethodText() {
        switch (paymentMethod) {
            case "CASH":
                return "现金";
            case "WECHAT":
                return "微信";
            case "ALIPAY":
                return "支付宝";
            case "CARD":
                return "银行卡";
            default:
                return paymentMethod;
        }
    }
}
