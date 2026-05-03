package com.cashier.service;

import com.cashier.dao.MemberDAO;
import com.cashier.dao.RechargeRecordDAO;
import com.cashier.model.Member;
import com.cashier.model.RechargeRecord;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 会员服务类
 * 封装会员相关的业务逻辑
 */
public class MemberService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(MemberService.class);

    /**
     * 会员等级配置
     */
    private static final Map<String, BigDecimal> LEVEL_DISCOUNTS = new LinkedHashMap<>();
    static {
        LEVEL_DISCOUNTS.put("普通", new BigDecimal("10.0"));  // 不打折
        LEVEL_DISCOUNTS.put("银卡", new BigDecimal("9.5"));   // 9.5折
        LEVEL_DISCOUNTS.put("金卡", new BigDecimal("9.0"));   // 9折
        LEVEL_DISCOUNTS.put("钻石", new BigDecimal("8.5"));   // 8.5折
    }

    /**
     * 等级升级所需积分
     */
    private static final Map<String, BigDecimal> LEVEL_POINTS = new LinkedHashMap<>();
    static {
        LEVEL_POINTS.put("银卡", new BigDecimal("1000"));
        LEVEL_POINTS.put("金卡", new BigDecimal("5000"));
        LEVEL_POINTS.put("钻石", new BigDecimal("10000"));
    }

    /**
     * 根据手机号查找会员
     * @param phone 手机号
     * @return 会员对象，如果不存在返回null
     */
    public static Member findMemberByPhone(String phone) {
        try {
            return MemberDAO.findByPhone(phone);
        } catch (SQLException e) {
            logger.error("查找会员失败: phone={}", phone, e);
            return null;
        }
    }

    /**
     * 会员充值
     * @param member 会员
     * @param amount 充值金额
     * @param paymentMethod 支付方式
     * @param operator 操作员
     * @return 是否成功
     */
    public static boolean recharge(Member member, double amount, String paymentMethod, String operator) {
        BigDecimal rechargeAmount = BigDecimal.valueOf(amount);
        BigDecimal bonusPoints = rechargeAmount.multiply(BigDecimal.TEN);
        try {
            boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                // 获取最新会员信息
                Member latestMember = MemberDAO.findByIdWithConnection(conn, member.id);
                if (latestMember == null) {
                    throw new SQLException("会员不存在");
                }

                // 在同一事务内统一更新余额、积分和等级
                latestMember.balance = latestMember.getBalance().add(rechargeAmount);
                latestMember.points = latestMember.getPoints().add(bonusPoints);
                latestMember.level = calculateLevel(latestMember.points);
                latestMember.discount = LEVEL_DISCOUNTS.getOrDefault(latestMember.level, BigDecimal.TEN);
                latestMember.discountRate = latestMember.discount;
                if (!MemberDAO.updateWithConnection(conn, latestMember)) {
                    throw new SQLException("更新会员信息失败");
                }

                // 创建充值记录
                RechargeRecord record = new RechargeRecord();
                record.recordId = generateRechargeRecordId();
                record.memberPhone = latestMember.phone;
                record.memberName = latestMember.name;
                record.amount = rechargeAmount;
                record.paymentMethod = paymentMethod;
                record.operator = operator;
                record.timestamp = new Date();

                if (!RechargeRecordDAO.insertWithConnection(conn, record)) {
                    throw new SQLException("创建充值记录失败");
                }

                // 更新传入的会员对象
                member.balance = latestMember.balance;
                member.points = latestMember.points;
                member.level = latestMember.level;
                member.discount = latestMember.discount;
                member.discountRate = latestMember.discountRate;
                return true;
            });

            if (success) {
                logger.info("会员充值成功: phone={}, amount={}", member.phone, amount);
            }
            return success;
        } catch (SQLException e) {
            logger.error("会员充值失败: phone={}, amount={}", member.phone, amount, e);
            return false;
        }
    }

    /**
     * 更新会员等级和折扣
     * @param member 会员
     * @return 是否成功
     */
    public static boolean updateMemberLevel(Member member) {
        try {
            Member latestMember = MemberDAO.findById(member.id);
            if (latestMember == null) {
                return false;
            }

            // 计算新等级
            String newLevel = calculateLevel(latestMember.points);
            if (!newLevel.equals(latestMember.level)) {
                latestMember.level = newLevel;
                latestMember.discount = LEVEL_DISCOUNTS.get(newLevel);
                MemberDAO.update(latestMember);
                logger.info("会员等级已更新: phone={}, level={}", latestMember.phone, newLevel);
            }

            return true;
        } catch (SQLException e) {
            logger.error("更新会员等级失败", e);
            return false;
        }
    }

    /**
     * 根据积分计算等级
     * @param points 积分
     * @return 等级
     */
    public static String calculateLevel(BigDecimal points) {
        BigDecimal safePoints = points == null ? BigDecimal.ZERO : points;
        if (safePoints.compareTo(LEVEL_POINTS.get("钻石")) >= 0) {
            return "钻石";
        } else if (safePoints.compareTo(LEVEL_POINTS.get("金卡")) >= 0) {
            return "金卡";
        } else if (safePoints.compareTo(LEVEL_POINTS.get("银卡")) >= 0) {
            return "银卡";
        } else {
            return "普通";
        }
    }

    public static String calculateLevel(double points) {
        return calculateLevel(BigDecimal.valueOf(points));
    }

    /**
     * 根据等级获取折扣
     * @param level 等级
     * @return 折扣值（0-10，10表示不打折）
     */
    public static double getDiscountByLevel(String level) {
        return LEVEL_DISCOUNTS.getOrDefault(level, new BigDecimal("10.0")).doubleValue();
    }

    public static BigDecimal getDiscountByLevelDecimal(String level) {
        return LEVEL_DISCOUNTS.getOrDefault(level, new BigDecimal("10.0"));
    }

    /**
     * 检查会员余额是否充足
     * @param member 会员
     * @param amount 需要的金额
     * @return 是否充足
     */
    public static boolean checkBalanceSufficient(Member member, double amount) {
        try {
            Member latestMember = MemberDAO.findById(member.id);
            return latestMember != null && latestMember.getBalance().compareTo(BigDecimal.valueOf(amount)) >= 0;
        } catch (SQLException e) {
            logger.error("检查会员余额失败", e);
            return false;
        }
    }

    /**
     * 计算会员折扣后金额
     * @param originalAmount 原始金额
     * @param member 会员
     * @return 折扣后金额
     */
    public static double calculateDiscountedAmount(double originalAmount, Member member) {
        if (member == null) {
            return originalAmount;
        }
        BigDecimal discountRate = member.getDiscount().divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(originalAmount).multiply(discountRate).doubleValue();
    }

    /**
     * 获取会员统计信息
     * @return 统计信息
     */
    public static Map<String, Object> getMemberStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<Member> members = MemberDAO.findAll();

            int totalCount = members.size();
            double totalBalance = members.stream().map(Member::getBalance).mapToDouble(BigDecimal::doubleValue).sum();
            double totalPoints = members.stream().map(Member::getPoints).mapToDouble(BigDecimal::doubleValue).sum();

            // 按等级统计
            Map<String, Integer> levelStats = new HashMap<>();
            for (Member member : members) {
                levelStats.merge(member.level, 1, Integer::sum);
            }

            stats.put("totalCount", totalCount);
            stats.put("totalBalance", totalBalance);
            stats.put("totalPoints", totalPoints);
            stats.put("levelStats", levelStats);

        } catch (SQLException e) {
            logger.error("获取会员统计失败", e);
        }
        return stats;
    }

    /**
     * 获取等级配置
     * @return 等级配置
     */
    public static Map<String, Object> getLevelConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("levelDiscounts", LEVEL_DISCOUNTS);
        config.put("levelPoints", LEVEL_POINTS);
        return config;
    }

    private static String generateRechargeRecordId() {
        return "REC" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

}
