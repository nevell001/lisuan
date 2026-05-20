package com.cashier.service;

import com.cashier.dao.PromotionDAO;
import com.cashier.model.Promotion;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 促销服务类
 * 封装促销相关的业务逻辑
 */
public class PromotionService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PromotionService.class);

    /**
     * 获取所有当前有效的促销
     */
    public static List<Promotion> getActivePromotions() {
        try {
            return PromotionDAO.findActive();
        } catch (SQLException e) {
            logger.error("获取有效促销失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取所有已启用的促销（不限时间）
     */
    public static List<Promotion> getEnabledPromotions() {
        try {
            return PromotionDAO.findEnabled();
        } catch (SQLException e) {
            logger.error("获取已启用促销失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 计算订单可享受的最佳折扣
     * @param amount 订单金额
     * @return 最佳折扣金额（如果没有可用促销则返回 BigDecimal.ZERO）
     */
    public static BigDecimal calculateBestDiscount(BigDecimal amount) {
        List<Promotion> active = getActivePromotions();
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (Promotion promo : active) {
            BigDecimal discount = promo.calculateDiscount(amount);
            if (discount.compareTo(bestDiscount) > 0) {
                bestDiscount = discount;
            }
        }

        return bestDiscount;
    }

    /**
     * 应用促销并增加使用次数（事务内）
     * @param promotionId 促销ID
     * @param amount 订单金额
     * @return 折扣金额（Optional）
     */
    public static Optional<BigDecimal> applyPromotion(int promotionId, BigDecimal amount) {
        try {
            BigDecimal result = DatabaseManager.executeInTransaction(conn -> {
                Promotion promo = PromotionDAO.findById(promotionId);
                if (promo == null || !promo.enabled || !promo.isValid()) {
                    return null;
                }

                if (promo.maxUsage > 0 && promo.usageCount >= promo.maxUsage) {
                    return null;
                }

                BigDecimal discount = promo.calculateDiscount(amount);
                if (discount.compareTo(BigDecimal.ZERO) > 0) {
                    PromotionDAO.incrementUsageWithConnection(conn, promotionId);
                    logger.info("促销 {} 已应用，折扣金额: {}", promo.name, discount);
                    return discount;
                }

                return null;
            });
            return Optional.ofNullable(result);
        } catch (SQLException e) {
            logger.error("应用促销失败", e);
            return Optional.empty();
        }
    }

    /**
     * 创建促销
     */
    public static boolean createPromotion(Promotion promotion) {
        try {
            boolean result = PromotionDAO.insert(promotion);
            if (result) {
                logger.info("促销创建成功: {}", promotion.name);
            }
            return result;
        } catch (SQLException e) {
            logger.error("创建促销失败", e);
            return false;
        }
    }

    /**
     * 更新促销
     */
    public static boolean updatePromotion(Promotion promotion) {
        try {
            boolean result = PromotionDAO.update(promotion);
            if (result) {
                logger.info("促销更新成功: {}", promotion.name);
            }
            return result;
        } catch (SQLException e) {
            logger.error("更新促销失败", e);
            return false;
        }
    }

    /**
     * 删除促销
     */
    public static boolean deletePromotion(int id) {
        try {
            boolean result = PromotionDAO.delete(id);
            if (result) {
                logger.info("促销删除成功: id={}", id);
            }
            return result;
        } catch (SQLException e) {
            logger.error("删除促销失败", e);
            return false;
        }
    }
}
