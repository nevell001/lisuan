package com.cashier.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Promotion {
    public int id;                    // 促销ID（数据库自增主键）
    public String promotionCode;       // 促销编号（用户自定义编号）
    public String name;                  // 促销名称
    public String type;                  // 促销类型: "满减", "打折", "优惠券"
    public BigDecimal threshold;         // 满减/打折的门槛金额
    public BigDecimal discount;          // 折扣值（满减的减额，打折的折扣率，优惠券的面额）
    public String description;           // 描述
    public boolean enabled;              // 是否启用
    public LocalDateTime startDate;               // 开始日期
    public LocalDateTime endDate;                 // 结束日期
    public int usageCount;               // 使用次数
    public int maxUsage;                 // 最大使用次数（-1表示无限制）

    public Promotion() {
        this.id = 0;  // 默认ID为0，表示未保存到数据库
        this.promotionCode = "";  // 促销编号
        this.name = "";
        this.type = "满减";
        this.threshold = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
        this.description = "";
        this.enabled = true;
        this.startDate = LocalDateTime.now();
        this.endDate = LocalDateTime.now().plusDays(30); // 默认30天后
        this.usageCount = 0;
        this.maxUsage = -1;
    }

    public Promotion(String name, String type, BigDecimal threshold, BigDecimal discount, String description) {
        this();
        this.name = name;
        this.type = type;
        this.threshold = defaultDecimal(threshold);
        this.discount = defaultDecimal(discount);
        this.description = description;
    }

    public Promotion(String name, String type, double threshold, double discount, String description) {
        this(name, type, BigDecimal.valueOf(threshold), BigDecimal.valueOf(discount), description);
    }

    public Promotion(int id, String name, String type, BigDecimal threshold, BigDecimal discount, String description, boolean enabled, LocalDateTime startDate, LocalDateTime endDate, int usageCount, int maxUsage) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.threshold = defaultDecimal(threshold);
        this.discount = defaultDecimal(discount);
        this.description = description;
        this.enabled = enabled;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageCount = usageCount;
        this.maxUsage = maxUsage;
    }

    public Promotion(int id, String name, String type, double threshold, double discount, String description, boolean enabled, LocalDateTime startDate, LocalDateTime endDate, int usageCount, int maxUsage) {
        this(id, name, type, BigDecimal.valueOf(threshold), BigDecimal.valueOf(discount), description, enabled, startDate, endDate, usageCount, maxUsage);
    }

    public Promotion(int id, String promotionCode, String name, String type, BigDecimal threshold, BigDecimal discount, String description, boolean enabled, LocalDateTime startDate, LocalDateTime endDate, int usageCount, int maxUsage) {
        this(id, name, type, threshold, discount, description, enabled, startDate, endDate, usageCount, maxUsage);
        this.promotionCode = promotionCode;
    }

    public Promotion(int id, String promotionCode, String name, String type, double threshold, double discount, String description, boolean enabled, LocalDateTime startDate, LocalDateTime endDate, int usageCount, int maxUsage) {
        this(id, promotionCode, name, type, BigDecimal.valueOf(threshold), BigDecimal.valueOf(discount), description, enabled, startDate, endDate, usageCount, maxUsage);
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // 计算折扣金额
    public BigDecimal calculateDiscount(BigDecimal amount) {
        BigDecimal safeAmount = defaultDecimal(amount);
        if (!enabled || !isValid()) {
            return BigDecimal.ZERO;
        }

        if (maxUsage > 0 && usageCount >= maxUsage) {
            return BigDecimal.ZERO;
        }

        switch (type) {
            case "满减":
                if (safeAmount.compareTo(defaultDecimal(threshold)) >= 0) {
                    return defaultDecimal(discount);
                }
                break;
            case "打折":
                if (safeAmount.compareTo(defaultDecimal(threshold)) >= 0) {
                    return safeAmount.multiply(BigDecimal.ONE.subtract(defaultDecimal(discount)));
                }
                break;
            case "优惠券":
                // 优惠券可以直接使用，不设门槛
                return defaultDecimal(discount);
            default:
                break;
        }
        return BigDecimal.ZERO;
    }

    // 检查促销是否有效
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate) && now.isBefore(endDate);
    }

    // 增加使用次数
    public void incrementUsage() {
        usageCount++;
    }

    // 获取促销描述
    public String getDisplayText() {
        String status = enabled ? "启用" : "禁用";
        String validity = isValid() ? "有效" : "已过期";

        switch (type) {
            case "满减":
                return String.format(java.util.Locale.ROOT, "%s - 满%.2f减%.2f [%s] [%s] 使用:%d/%d",
                    name, threshold.doubleValue(), discount.doubleValue(), status, validity, usageCount, maxUsage == -1 ? -1 : maxUsage);
            case "打折":
                return String.format(java.util.Locale.ROOT, "%s - 满%.2f打%.0f折 [%s] [%s] 使用:%d/%d",
                    name, threshold.doubleValue(), discount.multiply(BigDecimal.TEN).doubleValue(), status, validity, usageCount, maxUsage == -1 ? -1 : maxUsage);
            case "优惠券":
                return String.format(java.util.Locale.ROOT, "%s - 面额%.2f元 [%s] [%s] 使用:%d/%d",
                    name, discount.doubleValue(), status, validity, usageCount, maxUsage == -1 ? -1 : maxUsage);
            default:
                return name;
        }
    }

    // Getter方法
    public int getId() {
        return id;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getThreshold() {
        return defaultDecimal(threshold);
    }

    public BigDecimal getDiscount() {
        return defaultDecimal(discount);
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public int getMaxUsage() {
        return maxUsage;
    }
}
