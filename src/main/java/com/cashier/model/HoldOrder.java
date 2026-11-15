package com.cashier.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;

/**
 * 挂单模型
 * 用于暂时保存未完成的订单
 */
public class HoldOrder {
    public Integer id;
    public String orderNumber;
    public Integer userId;
    public Integer memberId;
    public String memberName;
    public String memberPhone;
    public BigDecimal totalAmount;
    public BigDecimal discountAmount;
    public BigDecimal finalAmount;
    public Integer itemCount;
    public String itemsJson;  // 购物车项目JSON
    public Date holdDate;
    public Time holdTime;
    public String notes;
    public Integer status;  // 0-挂单中, 1-已恢复, 2-已取消

    public HoldOrder() {
        this.status = 0;
        this.totalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.finalAmount = BigDecimal.ZERO;
        this.itemCount = 0;
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        switch (status) {
            case 0: return com.cashier.i18n.I18nManager.getInstance().get("runtime.hold.pending");
            case 1: return com.cashier.i18n.I18nManager.getInstance().get("runtime.hold.resumed");
            case 2: return com.cashier.i18n.I18nManager.getInstance().get("runtime.hold.cancelled");
            default: return com.cashier.i18n.I18nManager.getInstance().get("runtime.unknown");
        }
    }

    /**
     * 生成订单号（时间戳 + 序号 + 进程随机段，与交易单号同一加固口径）。
     */
    public static String generateOrderNumber() {
        return com.cashier.service.TransactionService.generateTransactionId("HLD");
    }
}
