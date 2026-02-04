package com.cashier.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Promotion {
    public int id;                    // 促销ID（数据库自增主键）
    public String name;                  // 促销名称
    public String type;                  // 促销类型: "满减", "打折", "优惠券"
    public double threshold;             // 满减/打折的门槛金额
    public double discount;              // 折扣值（满减的减额，打折的折扣率，优惠券的面额）
    public String description;           // 描述
    public boolean enabled;              // 是否启用
    public Date startDate;               // 开始日期
    public Date endDate;                 // 结束日期
    public int usageCount;               // 使用次数
    public int maxUsage;                 // 最大使用次数（-1表示无限制）

    public Promotion() {
        this.id = 0;  // 默认ID为0，表示未保存到数据库
        this.name = "";
        this.type = "满减";
        this.threshold = 0;
        this.discount = 0;
        this.description = "";
        this.enabled = true;
        this.startDate = new Date();
        this.endDate = new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000); // 默认30天后
        this.usageCount = 0;
        this.maxUsage = -1;
    }

    public Promotion(String name, String type, double threshold, double discount, String description) {
        this();
        this.name = name;
        this.type = type;
        this.threshold = threshold;
        this.discount = discount;
        this.description = description;
    }

    public Promotion(int id, String name, String type, double threshold, double discount, String description, boolean enabled, Date startDate, Date endDate, int usageCount, int maxUsage) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.threshold = threshold;
        this.discount = discount;
        this.description = description;
        this.enabled = enabled;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageCount = usageCount;
        this.maxUsage = maxUsage;
    }

    // 计算折扣金额
    public double calculateDiscount(double amount) {
        if (!enabled || !isValid()) {
            return 0;
        }

        if (maxUsage > 0 && usageCount >= maxUsage) {
            return 0;
        }

        switch (type) {
            case "满减":
                if (amount >= threshold) {
                    return discount;
                }
                break;
            case "打折":
                if (amount >= threshold) {
                    return amount * (1 - discount);
                }
                break;
            case "优惠券":
                // 优惠券可以直接使用，不设门槛
                return discount;
        }
        return 0;
    }

    // 检查促销是否有效
    public boolean isValid() {
        Date now = new Date();
        return now.after(startDate) && now.before(endDate);
    }

    // 增加使用次数
    public void incrementUsage() {
        usageCount++;
    }

    // 获取促销描述
    public String getDisplayText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String status = enabled ? "启用" : "禁用";
        String validity = isValid() ? "有效" : "已过期";

        switch (type) {
            case "满减":
                return String.format("%s - 满%.2f减%.2f [%s] [%s] 使用:%d/%d",
                    name, threshold, discount, status, validity, usageCount, maxUsage == -1 ? -1 : maxUsage);
            case "打折":
                return String.format("%s - 满%.2f打%.0f折 [%s] [%s] 使用:%d/%d",
                    name, threshold, discount * 10, status, validity, usageCount, maxUsage == -1 ? -1 : maxUsage);
            case "优惠券":
                return String.format("%s - 面额%.2f元 [%s] [%s] 使用:%d/%d",
                    name, discount, status, validity, usageCount, maxUsage == -1 ? -1 : maxUsage);
            default:
                return name;
        }
    }

    // Getter方法
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public double getThreshold() {
        return threshold;
    }

    public double getDiscount() {
        return discount;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public int getMaxUsage() {
        return maxUsage;
    }
}