package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 交易服务类
 * 封装交易相关的业务逻辑
 */
public class TransactionService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TransactionService.class);

    /**
     * 交易结果
     */
    public static class TransactionResult {
        public boolean success;
        public String transactionId;
        public String message;
        public Transaction transaction;

        public TransactionResult(boolean success, String transactionId, String message, Transaction transaction) {
            this.success = success;
            this.transactionId = transactionId;
            this.message = message;
            this.transaction = transaction;
        }
    }

    /**
     * 执行交易
     * @param cartItems 购物车商品列表
     * @param member 会员（可为 null）
     * @param paymentMethod 支付方式
     * @param receivedAmount 实收金额
     * @param changeAmount 找零金额
     * @param inventory 库存数据（用于更新内存中的库存）
     * @return 交易结果
     */
    public static TransactionResult executeTransaction(
            List<CartItem> cartItems,
            Member member,
            String paymentMethod,
            double receivedAmount,
            double changeAmount,
            Map<String, Product> inventory) {

        Connection conn = null;
        String transactionId = generateOrderNumber();

        try {
            // 获取数据库连接并开始事务
            conn = DatabaseManager.getConnection();
            DatabaseManager.beginTransaction(conn);

            // 1. 检查并扣减库存
            List<Product> updatedProducts = new ArrayList<>();
            for (CartItem item : cartItems) {
                Product product = inventory.get(item.product.name);
                if (product == null) {
                    throw new SQLException("商品不存在: " + item.product.name);
                }

                // 从数据库获取最新库存
                Product latestProduct = ProductDAO.findById(product.id);
                if (latestProduct == null) {
                    throw new SQLException("商品不存在: " + item.product.name);
                }

                // 检查库存是否足够
                if (latestProduct.quantity < item.quantity) {
                    throw new SQLException("商品 " + item.product.name + " 库存不足！当前库存: " + latestProduct.quantity + ", 需要数量: " + item.quantity);
                }

                // 扣减库存
                product.quantity = latestProduct.quantity - item.quantity;
                product.version = latestProduct.version;

                // 更新数据库库存（使用乐观锁）
                if (!ProductDAO.updateWithVersionWithConnection(conn, product)) {
                    throw new SQLException("商品 " + item.product.name + " 库存更新失败，可能已被其他操作修改");
                }

                updatedProducts.add(product);
            }

            // 2. 更新会员余额和积分
            if (member != null) {
                double finalAmount = calculateFinalAmount(cartItems, member);

                // 从数据库获取最新会员信息
                Member latestMember = MemberDAO.findById(member.id);
                if (latestMember == null) {
                    throw new SQLException("会员不存在: " + member.phone);
                }

                // 检查余额是否足够
                if (latestMember.balance < finalAmount) {
                    throw new SQLException("会员余额不足！当前余额: " + latestMember.balance + ", 需要支付: " + finalAmount);
                }

                // 更新余额和积分
                member.balance = latestMember.balance - finalAmount;
                member.points = latestMember.points + (int)(finalAmount * 10);

                // 更新数据库
                if (!MemberDAO.updateWithConnection(conn, member)) {
                    throw new SQLException("更新会员信息失败");
                }
            }

            // 3. 创建交易记录
            Transaction transaction = createTransaction(transactionId, cartItems, member, paymentMethod, receivedAmount, changeAmount);
            TransactionDAO.insertWithConnection(conn, transaction);

            // 提交事务
            DatabaseManager.commitTransaction(conn);
            logger.info("交易成功完成，交易ID: {}", transactionId);

            // 更新内存中的库存
            for (Product product : updatedProducts) {
                inventory.put(product.name, product);
            }

            return new TransactionResult(true, transactionId, "交易成功", transaction);

        } catch (SQLException e) {
            // 回滚事务
            if (conn != null) {
                DatabaseManager.rollbackTransaction(conn);
            }
            logger.error("交易失败: " + e.getMessage(), e);
            return new TransactionResult(false, null, "交易失败: " + e.getMessage(), null);
        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                DatabaseManager.rollbackTransaction(conn);
            }
            logger.error("交易失败: " + e.getMessage(), e);
            return new TransactionResult(false, null, "交易失败: " + e.getMessage(), null);
        } finally {
            // 恢复自动提交
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
     * 创建交易记录
     * @param transactionId 交易ID
     * @param cartItems 购物车商品列表
     * @param member 会员
     * @param paymentMethod 支付方式
     * @param receivedAmount 实收金额
     * @param changeAmount 找零金额
     * @return 交易记录
     */
    private static Transaction createTransaction(
            String transactionId,
            List<CartItem> cartItems,
            Member member,
            String paymentMethod,
            double receivedAmount,
            double changeAmount) {

        Transaction transaction = new Transaction();
        transaction.transactionId = transactionId;
        transaction.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        transaction.items = new ArrayList<>();

        for (CartItem item : cartItems) {
            transaction.items.add(item.product);
        }

        transaction.totalAmount = calculateTotalAmount(cartItems);

        // 计算税费
        Map<String, String> settings = DataService.loadSettings();
        double taxRate = Double.parseDouble(settings.getOrDefault("taxRate", "0.0"));
        transaction.tax = transaction.totalAmount * taxRate / 100.0;

        transaction.finalAmount = calculateFinalAmount(cartItems, member);
        transaction.paymentMethod = paymentMethod;

        if (member != null) {
            transaction.memberPhone = member.phone;
        }

        return transaction;
    }

    /**
     * 计算总金额
     * @param cartItems 购物车商品列表
     * @return 总金额
     */
    public static double calculateTotalAmount(List<CartItem> cartItems) {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.subtotal;
        }
        return total;
    }

    /**
     * 计算最终金额（应用会员折扣）
     * @param cartItems 购物车商品列表
     * @param member 会员
     * @return 最终金额
     */
    public static double calculateFinalAmount(List<CartItem> cartItems, Member member) {
        double total = calculateTotalAmount(cartItems);
        if (member != null) {
            // 折扣值范围：0-10，10表示不打折，0表示免费
            double discountRate = member.discount / 10.0;
            total = total * discountRate;
        }
        return total;
    }

    /**
     * 生成订单号
     * @return 订单号
     */
    public static String generateOrderNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return "ORD" + sdf.format(new Date());
    }

    /**
     * 获取交易统计信息
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    public static Map<String, Object> getTransactionStatistics(String startDate, String endDate) {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<Transaction> transactions = TransactionDAO.findByDateRange(startDate, endDate);

            double totalAmount = 0;
            int totalCount = transactions.size();
            Map<String, Double> paymentMethodStats = new HashMap<>();

            for (Transaction t : transactions) {
                totalAmount += t.finalAmount;
                paymentMethodStats.merge(t.paymentMethod, t.finalAmount, Double::sum);
            }

            stats.put("totalAmount", totalAmount);
            stats.put("totalCount", totalCount);
            stats.put("averageAmount", totalCount > 0 ? totalAmount / totalCount : 0);
            stats.put("paymentMethodStats", paymentMethodStats);

        } catch (SQLException e) {
            logger.error("获取交易统计失败", e);
        }
        return stats;
    }
}