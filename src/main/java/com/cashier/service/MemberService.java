package com.cashier.service;

import com.cashier.dao.MemberDAO;
import com.cashier.dao.RechargeRecordDAO;
import com.cashier.model.Member;
import com.cashier.model.RechargeRecord;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.Connection;
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
    private static final Map<String, Double> LEVEL_DISCOUNTS = new LinkedHashMap<>();
    static {
        LEVEL_DISCOUNTS.put("普通", 10.0);  // 不打折
        LEVEL_DISCOUNTS.put("银卡", 9.5);   // 9.5折
        LEVEL_DISCOUNTS.put("金卡", 9.0);   // 9折
        LEVEL_DISCOUNTS.put("钻石", 8.5);   // 8.5折
    }

    /**
     * 等级升级所需积分
     */
    private static final Map<String, Integer> LEVEL_POINTS = new LinkedHashMap<>();
    static {
        LEVEL_POINTS.put("银卡", 1000);
        LEVEL_POINTS.put("金卡", 5000);
        LEVEL_POINTS.put("钻石", 10000);
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
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            DatabaseManager.beginTransaction(conn);

            // 获取最新会员信息
            Member latestMember = MemberDAO.findById(member.id);
            if (latestMember == null) {
                throw new SQLException("会员不存在");
            }

            // 更新余额
            latestMember.balance += amount;
            if (!MemberDAO.updateWithConnection(conn, latestMember)) {
                throw new SQLException("更新会员余额失败");
            }

            // 创建充值记录
            RechargeRecord record = new RechargeRecord();
            record.memberPhone = latestMember.phone;
            record.memberName = latestMember.name;
            record.amount = amount;
            record.paymentMethod = paymentMethod;
            record.operator = operator;
            record.timestamp = new Date();

            RechargeRecordDAO.insertWithConnection(conn, record);

            DatabaseManager.commitTransaction(conn);
            logger.info("会员充值成功: phone={}, amount={}", member.phone, amount);

            // 更新传入的会员对象
            member.balance = latestMember.balance;
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                DatabaseManager.rollbackTransaction(conn);
            }
            logger.error("会员充值失败: phone={}, amount={}", member.phone, amount, e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭数据库连接失败", e);
                }
            }
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
    public static String calculateLevel(double points) {
        if (points >= LEVEL_POINTS.get("钻石")) {
            return "钻石";
        } else if (points >= LEVEL_POINTS.get("金卡")) {
            return "金卡";
        } else if (points >= LEVEL_POINTS.get("银卡")) {
            return "银卡";
        } else {
            return "普通";
        }
    }

    /**
     * 根据等级获取折扣
     * @param level 等级
     * @return 折扣值（0-10，10表示不打折）
     */
    public static double getDiscountByLevel(String level) {
        return LEVEL_DISCOUNTS.getOrDefault(level, 10.0);
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
            return latestMember != null && latestMember.balance >= amount;
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
        // 折扣值范围：0-10，10表示不打折，0表示免费
        double discountRate = member.discount / 10.0;
        return originalAmount * discountRate;
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
            double totalBalance = members.stream().mapToDouble(m -> m.balance).sum();
            double totalPoints = members.stream().mapToDouble(m -> m.points).sum();

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
}