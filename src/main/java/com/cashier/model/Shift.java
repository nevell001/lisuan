package com.cashier.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Shift {
    public String shiftId;           // 班次ID
    public String username;          // 操作员用户名
    public String operatorName;      // 操作员姓名
    public Date startTime;           // 开始时间
    public Date endTime;             // 结束时间
    public double openingRevenue;    // 开机时的营业额
    public double closingRevenue;    // 关机时的营业额
    public int openingTransactionCount;  // 开机时的交易数
    public int closingTransactionCount;  // 关机时的交易数
    public double shiftRevenue;      // 本班次营业额
    public int shiftTransactionCount;    // 本班次交易数
    public String notes;             // 备注
    
    // 各支付方式收入
    public double cashRevenue;       // 现金收入
    public double wechatRevenue;     // 微信收入
    public double alipayRevenue;     // 支付宝收入
    public double cardRevenue;       // 银行卡收入

    public Shift() {
        this.shiftId = "";
        this.username = "";
        this.operatorName = "";
        this.startTime = new Date();
        this.endTime = new Date();
        this.openingRevenue = 0;
        this.closingRevenue = 0;
        this.openingTransactionCount = 0;
        this.closingTransactionCount = 0;
        this.shiftRevenue = 0;
        this.shiftTransactionCount = 0;
        this.notes = "";
        this.cashRevenue = 0;
        this.wechatRevenue = 0;
        this.alipayRevenue = 0;
        this.cardRevenue = 0;
    }

    public Shift(String shiftId, String username, String operatorName, 
                 Date startTime, double openingRevenue, int openingTransactionCount) {
        this.shiftId = shiftId;
        this.username = username;
        this.operatorName = operatorName;
        this.startTime = startTime;
        this.endTime = startTime;  // 未结束时，endTime等于startTime
        this.openingRevenue = openingRevenue;
        this.closingRevenue = openingRevenue;
        this.openingTransactionCount = openingTransactionCount;
        this.closingTransactionCount = openingTransactionCount;
        this.shiftRevenue = 0;
        this.shiftTransactionCount = 0;
        this.notes = "";
        this.cashRevenue = 0;
        this.wechatRevenue = 0;
        this.alipayRevenue = 0;
        this.cardRevenue = 0;
    }

    // 结束班次
    public void endShift(double closingRevenue, int closingTransactionCount) {
        this.endTime = new Date();
        this.closingRevenue = closingRevenue;
        this.closingTransactionCount = closingTransactionCount;
        this.shiftRevenue = closingRevenue - openingRevenue;
        this.shiftTransactionCount = closingTransactionCount - openingTransactionCount;
    }
    
    // 结束班次（带支付方式收入）
    public void endShift(double closingRevenue, int closingTransactionCount, 
                        double cashRevenue, double wechatRevenue, double alipayRevenue, double cardRevenue) {
        this.endTime = new Date();
        this.closingRevenue = closingRevenue;
        this.closingTransactionCount = closingTransactionCount;
        this.shiftRevenue = closingRevenue - openingRevenue;
        this.shiftTransactionCount = closingTransactionCount - openingTransactionCount;
        this.cashRevenue = cashRevenue;
        this.wechatRevenue = wechatRevenue;
        this.alipayRevenue = alipayRevenue;
        this.cardRevenue = cardRevenue;
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

    public double getOpeningRevenue() {
        return openingRevenue;
    }

    public double getClosingRevenue() {
        return closingRevenue;
    }

    public int getOpeningTransactionCount() {
        return openingTransactionCount;
    }

    public int getClosingTransactionCount() {
        return closingTransactionCount;
    }

    public double getShiftRevenue() {
        return shiftRevenue;
    }

    public int getShiftTransactionCount() {
        return shiftTransactionCount;
    }

    public String getNotes() {
        return notes;
    }

    public double getCashRevenue() {
        return cashRevenue;
    }

    public double getWechatRevenue() {
        return wechatRevenue;
    }

    public double getAlipayRevenue() {
        return alipayRevenue;
    }

    public double getCardRevenue() {
        return cardRevenue;
    }
}