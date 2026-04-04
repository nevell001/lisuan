package com.cashier.model;

import java.math.BigDecimal;
import java.util.Date;

public class Shift {
    public String shiftId;           // 班次ID
    public String username;          // 操作员用户名
    public String operatorName;      // 操作员姓名
    public Date startTime;           // 开始时间
    public Date endTime;             // 结束时间
    public BigDecimal openingRevenue;    // 开机时的营业额
    public BigDecimal closingRevenue;    // 关机时的营业额
    public int openingTransactionCount;  // 开机时的交易数
    public int closingTransactionCount;  // 关机时的交易数
    public BigDecimal shiftRevenue;      // 本班次营业额
    public int shiftTransactionCount;    // 本班次交易数
    public String notes;             // 备注

    // 各支付方式收入
    public BigDecimal cashRevenue;       // 现金收入
    public BigDecimal wechatRevenue;     // 微信收入
    public BigDecimal alipayRevenue;     // 支付宝收入
    public BigDecimal cardRevenue;       // 银行卡收入

    public Shift() {
        this.shiftId = "";
        this.username = "";
        this.operatorName = "";
        this.startTime = new Date();
        this.endTime = new Date();
        this.openingRevenue = BigDecimal.ZERO;
        this.closingRevenue = BigDecimal.ZERO;
        this.openingTransactionCount = 0;
        this.closingTransactionCount = 0;
        this.shiftRevenue = BigDecimal.ZERO;
        this.shiftTransactionCount = 0;
        this.notes = "";
        this.cashRevenue = BigDecimal.ZERO;
        this.wechatRevenue = BigDecimal.ZERO;
        this.alipayRevenue = BigDecimal.ZERO;
        this.cardRevenue = BigDecimal.ZERO;
    }

    public Shift(String shiftId, String username, String operatorName,
                 Date startTime, BigDecimal openingRevenue, int openingTransactionCount) {
        this();
        this.shiftId = shiftId;
        this.username = username;
        this.operatorName = operatorName;
        this.startTime = startTime;
        this.endTime = startTime;  // 未结束时，endTime等于startTime
        this.openingRevenue = defaultDecimal(openingRevenue);
        this.closingRevenue = this.openingRevenue;
        this.openingTransactionCount = openingTransactionCount;
        this.closingTransactionCount = openingTransactionCount;
    }

    public Shift(String shiftId, String username, String operatorName,
                 Date startTime, double openingRevenue, int openingTransactionCount) {
        this(shiftId, username, operatorName, startTime, BigDecimal.valueOf(openingRevenue), openingTransactionCount);
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // 结束班次
    public void endShift(BigDecimal closingRevenue, int closingTransactionCount) {
        this.endTime = new Date();
        this.closingRevenue = defaultDecimal(closingRevenue);
        this.closingTransactionCount = closingTransactionCount;
        this.shiftRevenue = this.closingRevenue.subtract(defaultDecimal(openingRevenue));
        this.shiftTransactionCount = closingTransactionCount - openingTransactionCount;
    }

    public void endShift(double closingRevenue, int closingTransactionCount) {
        endShift(BigDecimal.valueOf(closingRevenue), closingTransactionCount);
    }

    // 结束班次（带支付方式收入）
    public void endShift(BigDecimal closingRevenue, int closingTransactionCount,
                        BigDecimal cashRevenue, BigDecimal wechatRevenue, BigDecimal alipayRevenue, BigDecimal cardRevenue) {
        this.endTime = new Date();
        this.closingRevenue = defaultDecimal(closingRevenue);
        this.closingTransactionCount = closingTransactionCount;
        this.shiftRevenue = this.closingRevenue.subtract(defaultDecimal(openingRevenue));
        this.shiftTransactionCount = closingTransactionCount - openingTransactionCount;
        this.cashRevenue = defaultDecimal(cashRevenue);
        this.wechatRevenue = defaultDecimal(wechatRevenue);
        this.alipayRevenue = defaultDecimal(alipayRevenue);
        this.cardRevenue = defaultDecimal(cardRevenue);
    }

    public void endShift(double closingRevenue, int closingTransactionCount,
                        double cashRevenue, double wechatRevenue, double alipayRevenue, double cardRevenue) {
        endShift(BigDecimal.valueOf(closingRevenue), closingTransactionCount,
            BigDecimal.valueOf(cashRevenue), BigDecimal.valueOf(wechatRevenue), BigDecimal.valueOf(alipayRevenue), BigDecimal.valueOf(cardRevenue));
    }

    // 计算班次时长（分钟）
    public long getShiftDuration() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (endTime.getTime() - startTime.getTime()) / (1000 * 60);
    }

    // 获取班次时长显示文本
    public String getDurationText() {
        if (startTime == null || endTime == null) {
            return "未完成";
        }
        long minutes = getShiftDuration();
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format("%d小时%d分钟", hours, mins);
    }

    // Getter方法
    public String getShiftId() {
        return shiftId;
    }

    public String getUsername() {
        return username;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public BigDecimal getOpeningRevenue() {
        return defaultDecimal(openingRevenue);
    }

    public BigDecimal getClosingRevenue() {
        return defaultDecimal(closingRevenue);
    }

    public int getOpeningTransactionCount() {
        return openingTransactionCount;
    }

    public int getClosingTransactionCount() {
        return closingTransactionCount;
    }

    public BigDecimal getShiftRevenue() {
        return defaultDecimal(shiftRevenue);
    }

    public int getShiftTransactionCount() {
        return shiftTransactionCount;
    }

    public String getNotes() {
        return notes;
    }

    public BigDecimal getCashRevenue() {
        return defaultDecimal(cashRevenue);
    }

    public BigDecimal getWechatRevenue() {
        return defaultDecimal(wechatRevenue);
    }

    public BigDecimal getAlipayRevenue() {
        return defaultDecimal(alipayRevenue);
    }

    public BigDecimal getCardRevenue() {
        return defaultDecimal(cardRevenue);
    }
}