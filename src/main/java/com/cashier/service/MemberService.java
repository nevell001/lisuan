package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.model.Member;
import com.cashier.model.RechargeRecord;
import com.cashier.i18n.I18nManager;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;

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
     * @throws RuntimeException 数据库异常时抛出，调用方可区分"不存在"与"查询失败"
     */
    public static Member findMemberByPhone(String phone) {
        try {
            return DAOFactory.getInstance().getMemberDAO().findByPhone(phone);
        } catch (SQLException e) {
            logger.error("查找会员失败: phone={}", phone, e);
            throw new RuntimeException("查找会员失败", e);
        }
    }

    /**
     * 会员充值（BigDecimal 版本，推荐使用）
     * @param member 会员
     * @param amount 充值金额
     * @param paymentMethod 支付方式
     * @param operator 操作员
     * @return 是否成功
     */
    public static boolean recharge(Member member, BigDecimal amount, String paymentMethod, String operator) {
        BigDecimal rechargeAmount = amount;
        BigDecimal bonusPoints = rechargeAmount.multiply(BigDecimal.TEN);
        try {
            boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                // 获取最新会员信息
                Member latestMember = DAOFactory.getInstance().getMemberDAO().findByIdWithConnection(conn, member.id);
                if (latestMember == null) {
                    throw new SQLException(I18nManager.getInstance().get("service.member_not_found_id", member.id));
                }

                // 在同一事务内统一更新余额、积分和等级
                latestMember.balance = latestMember.getBalance().add(rechargeAmount);
                latestMember.points = latestMember.getPoints().add(bonusPoints);
                latestMember.level = calculateLevel(latestMember.points);
                latestMember.discount = LEVEL_DISCOUNTS.getOrDefault(latestMember.level, BigDecimal.TEN);
                latestMember.discountRate = latestMember.discount;
                if (!DAOFactory.getInstance().getMemberDAO().updateWithConnection(conn, latestMember)) {
                    throw new SQLException(I18nManager.getInstance().get("service.member_update_failed"));
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

                if (!DAOFactory.getInstance().getRechargeRecordDAO().insertWithConnection(conn, record)) {
                    throw new SQLException(I18nManager.getInstance().get("service.recharge_record_create_failed"));
                }

                // 更新传入的会员对象
                member.balance = latestMember.balance;
                member.points = latestMember.points;
                member.level = latestMember.level;
                member.discount = latestMember.discount;
                member.discountRate = latestMember.discountRate;
                member.version = latestMember.version;
                return true;
            });

            if (success) {
                logger.info("会员充值成功: phone={}, amount={}", member.phone, amount);
                AuditService.success(operator, "MEMBER", "MEMBER_RECHARGE",
                    "会员=" + member.phone + ", 金额=" + rechargeAmount, 1);
                
                // 广播会员充值事件
                com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
                    com.cashier.api.sync.SyncEventType.MEMBER_RECHARGED,
                    java.util.Map.of(
                        "id", member.id,
                        "phone", member.phone,
                        "amount", rechargeAmount.toString(),
                        "newBalance", member.balance.toString()
                    )
                );
            }
            return success;
        } catch (SQLException e) {
            logger.error("会员充值失败: phone={}, amount={}", member.phone, amount, e);
            AuditService.failure(operator, "MEMBER", "MEMBER_RECHARGE",
                "会员=" + member.phone + ", 金额=" + rechargeAmount + ", 原因=" + e.getMessage());
            return false;
        }
    }

    /**
     * 会员充值（double 版本，向后兼容，推荐使用 BigDecimal 版本）
     * @deprecated 请使用 {@link #recharge(Member, BigDecimal, String, String)} 避免精度丢失
     */
    @Deprecated
    public static boolean recharge(Member member, double amount, String paymentMethod, String operator) {
        return recharge(member, BigDecimal.valueOf(amount), paymentMethod, operator);
    }

    /**
     * 更新会员等级和折扣（事务内读-改-写，避免并发覆盖）
     * @param member 会员
     * @return 是否成功
     */
    public static boolean updateMemberLevel(Member member) {
        try {
            return DatabaseManager.executeBooleanTransaction(conn -> {
                Member latestMember = DAOFactory.getInstance().getMemberDAO().findByIdWithConnection(conn, member.id);
                if (latestMember == null) {
                    return false;
                }

                // 计算新等级
                String newLevel = calculateLevel(latestMember.points);
                if (!newLevel.equals(latestMember.level)) {
                    latestMember.level = newLevel;
                    latestMember.discount = LEVEL_DISCOUNTS.get(newLevel);
                    DAOFactory.getInstance().getMemberDAO().updateWithConnection(conn, latestMember);
                    // 同步传入的 member 对象
                    member.level = newLevel;
                    member.discount = latestMember.discount;
                    logger.info("会员等级已更新: phone={}, level={}", latestMember.phone, newLevel);
                }

                return true;
            });
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
     * 是否为合法会员等级。
     *
     * <p>供对外接口校验入参：等级是业务枚举，写进库里的值必须能被
     * {@link #calculateLevel(BigDecimal)} / {@link #getDiscountByLevelDecimal(String)} 识别，
     * 否则后续升级与折扣计算都会落空。</p>
     */
    public static boolean isKnownLevel(String level) {
        return level != null && LEVEL_DISCOUNTS.containsKey(level);
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
     * 检查会员余额是否充足（BigDecimal 版本，推荐使用）
     * @param member 会员
     * @param amount 需要的金额
     * @return 是否充足
     */
    public static boolean checkBalanceSufficient(Member member, BigDecimal amount) {
        try {
            Member latestMember = DAOFactory.getInstance().getMemberDAO().findById(member.id);
            return latestMember != null && latestMember.getBalance().compareTo(amount) >= 0;
        } catch (SQLException e) {
            logger.error("检查会员余额失败", e);
            return false;
        }
    }

    /**
     * 检查会员余额是否充足（double 版本，向后兼容）
     * @deprecated 请使用 {@link #checkBalanceSufficient(Member, BigDecimal)} 避免精度丢失
     */
    @Deprecated
    public static boolean checkBalanceSufficient(Member member, double amount) {
        return checkBalanceSufficient(member, BigDecimal.valueOf(amount));
    }

    /**
     * 计算会员折扣后金额（BigDecimal 版本，推荐使用）
     * @param originalAmount 原始金额
     * @param member 会员
     * @return 折扣后金额
     */
    public static BigDecimal calculateDiscountedAmount(BigDecimal originalAmount, Member member) {
        if (member == null) {
            return originalAmount;
        }
        BigDecimal discountRate = member.getDiscount().divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
        return originalAmount.multiply(discountRate);
    }

    /**
     * 计算会员折扣后金额（double 版本，向后兼容）
     * @deprecated 请使用 {@link #calculateDiscountedAmount(BigDecimal, Member)} 避免精度丢失
     */
    @Deprecated
    public static double calculateDiscountedAmount(double originalAmount, Member member) {
        return calculateDiscountedAmount(BigDecimal.valueOf(originalAmount), member).doubleValue();
    }

    /**
     * 获取会员统计信息
     * @return 统计信息
     */
    public static Map<String, Object> getMemberStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            Map<String, Object> summary = DAOFactory.getInstance().getMemberDAO().getMemberSummary();
            BigDecimal totalBalance = (BigDecimal) summary.getOrDefault("totalBalance", BigDecimal.ZERO);
            BigDecimal totalPoints = (BigDecimal) summary.getOrDefault("totalPoints", BigDecimal.ZERO);
            Map<String, Integer> levelStats = DAOFactory.getInstance().getMemberDAO().countByLevel();

            stats.put("totalCount", ((Number) summary.getOrDefault("totalCount", 0L)).intValue());
            stats.put("totalBalance", totalBalance.doubleValue());
            stats.put("totalPoints", totalPoints.doubleValue());
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
        return "REC" + java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
            .format(com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME_MILLIS)
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

}
