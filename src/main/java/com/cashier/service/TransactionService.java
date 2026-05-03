package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        private boolean success;
        private String transactionId;
        private String message;
        private Transaction transaction;

        public TransactionResult(boolean success, String transactionId, String message, Transaction transaction) {
            this.success = success;
            this.transactionId = transactionId;
            this.message = message;
            this.transaction = transaction;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getMessage() {
            return message;
        }

        public Transaction getTransaction() {
            return transaction;
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

        String transactionId = generateOrderNumber();
        Transaction transaction = createTransaction(transactionId, cartItems, member, paymentMethod, receivedAmount, changeAmount);
        return executeTransaction(cartItems, member, transaction, inventory, null);
    }

    /**
     * 执行交易（使用外部已构造的交易记录，适用于控制器已完成优惠计算的场景）
     * @param cartItems 购物车商品列表
     * @param member 会员（可为 null）
     * @param transaction 已构造的交易记录
     * @param inventory 库存数据（用于更新内存中的库存）
     * @param appliedPromotion 已应用的促销（可为 null）
     * @return 交易结果
     */
    public static TransactionResult executeTransaction(
            List<CartItem> cartItems,
            Member member,
            Transaction transaction,
            Map<String, Product> inventory,
            Promotion appliedPromotion) {

        String transactionId = transaction.transactionId != null ? transaction.transactionId : generateOrderNumber();
        transaction.transactionId = transactionId;
        BigDecimal payableAmount = transaction.finalAmount != null ? transaction.finalAmount : calculateFinalAmount(cartItems, member);
        List<Product> updatedProducts = new ArrayList<>();

        try {
            boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                for (CartItem item : cartItems) {
                    Product product = inventory.get(item.product.name);
                    if (product == null) {
                        throw new SQLException("商品不存在: " + item.product.name);
                    }

                    Product latestProduct = ProductDAO.findByIdWithConnection(conn, item.product.id);
                    if (latestProduct == null) {
                        throw new SQLException("商品不存在: " + item.product.name);
                    }

                    if (latestProduct.quantity < item.quantity) {
                        throw new SQLException("商品 " + item.product.name + " 库存不足！当前库存: " + latestProduct.quantity + ", 需要数量: " + item.quantity);
                    }

                    product.quantity = latestProduct.quantity - item.quantity;
                    product.version = latestProduct.version;

                    if (!ProductDAO.updateWithVersionWithConnection(conn, product)) {
                        throw new SQLException("商品 " + item.product.name + " 库存更新失败，可能已被其他操作修改");
                    }

                    updatedProducts.add(product);
                }

                if (member != null) {
                    Member latestMember = member.id > 0
                        ? MemberDAO.findByIdWithConnection(conn, member.id)
                        : MemberDAO.findByPhoneWithConnection(conn, member.phone);
                    if (latestMember == null) {
                        throw new SQLException("会员不存在: " + member.phone);
                    }

                    if (latestMember.getBalance().compareTo(payableAmount) < 0) {
                        throw new SQLException("会员余额不足！当前余额: " + latestMember.getBalance() + ", 需要支付: " + payableAmount);
                    }

                    BigDecimal updatedPoints = latestMember.getPoints().add(
                        payableAmount.multiply(BigDecimal.TEN).setScale(0, RoundingMode.DOWN)
                    );
                    String updatedLevel = MemberService.calculateLevel(updatedPoints);
                    BigDecimal updatedDiscount = MemberService.getDiscountByLevelDecimal(updatedLevel);

                    member.id = latestMember.id;
                    member.memberCode = latestMember.memberCode;
                    member.phone = latestMember.phone;
                    member.name = latestMember.name;
                    member.level = updatedLevel;
                    member.discount = updatedDiscount;
                    member.discountRate = updatedDiscount;
                    member.birthday = latestMember.getBirthday();
                    member.balance = latestMember.getBalance().subtract(payableAmount);
                    member.points = updatedPoints;

                    if (!MemberDAO.updateWithConnection(conn, member)) {
                        throw new SQLException("更新会员信息失败");
                    }
                }

                if (!TransactionDAO.insertWithConnection(conn, transaction)) {
                    throw new SQLException("保存交易记录失败: transactionId=" + transactionId);
                }

                if (appliedPromotion != null && !PromotionDAO.incrementUsageWithConnection(conn, appliedPromotion.id)) {
                    throw new SQLException("更新促销使用次数失败: promotionId=" + appliedPromotion.id);
                }

                return true;
            });

            if (!success) {
                logger.warn("交易未提交: transactionId={}", transactionId);
                return new TransactionResult(false, null, "交易失败", null);
            }

            for (Product product : updatedProducts) {
                inventory.put(product.name, product);
            }

            logger.info("交易成功完成，交易ID: {}", transactionId);
            return new TransactionResult(true, transactionId, "交易成功", transaction);
        } catch (SQLException | RuntimeException e) {
            logger.error("交易失败: {}", e.getMessage(), e);
            return new TransactionResult(false, null, "交易失败: " + e.getMessage(), null);
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
        transaction.tax = transaction.getTotalAmount()
                .multiply(BigDecimal.valueOf(taxRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

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
    public static BigDecimal calculateTotalAmount(List<CartItem> cartItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            total = total.add(item.subtotal == null ? BigDecimal.ZERO : item.subtotal);
        }
        return total;
    }

    /**
     * 计算最终金额（应用会员折扣）
     * @param cartItems 购物车商品列表
     * @param member 会员
     * @return 最终金额
     */
    public static BigDecimal calculateFinalAmount(List<CartItem> cartItems, Member member) {
        BigDecimal total = calculateTotalAmount(cartItems);
        if (member != null) {
            // 折扣值范围：0-10，10表示不打折，0表示免费
            BigDecimal discountRate = member.getDiscount().divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
            total = total.multiply(discountRate);
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

            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalCount = transactions.size();
            Map<String, Double> paymentMethodStats = new HashMap<>();

            for (Transaction t : transactions) {
                totalAmount = totalAmount.add(t.getFinalAmount());
                paymentMethodStats.merge(t.paymentMethod, t.getFinalAmount().doubleValue(), Double::sum);
            }

            stats.put("totalAmount", totalAmount.doubleValue());
            stats.put("totalCount", totalCount);
            stats.put("averageAmount", totalCount > 0 ? totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP).doubleValue() : 0.0);
            stats.put("paymentMethodStats", paymentMethodStats);

        } catch (SQLException e) {
            logger.error("获取交易统计失败", e);
        }
        return stats;
    }
}