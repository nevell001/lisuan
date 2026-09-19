package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.i18n.I18nManager;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 交易服务类
 * 封装交易相关的业务逻辑
 */
public class TransactionService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TransactionService.class);
    private static final com.cashier.dao.ProductDAORefactored productDAO = com.cashier.dao.DAOFactory.getInstance().getProductDAO();

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
            BigDecimal receivedAmount,
            BigDecimal changeAmount,
            Map<String, Product> inventory) {

        String transactionId = generateOrderNumber();
        Transaction transaction = createTransaction(transactionId, cartItems, member, paymentMethod,
            receivedAmount.doubleValue(), changeAmount.doubleValue());
        return executeTransaction(cartItems, member, transaction, inventory, null);
    }

    /**
     * 执行交易（double 版本，向后兼容）
     * @deprecated 请使用 {@link #executeTransaction(List, Member, String, BigDecimal, BigDecimal, Map)} 避免精度丢失
     */
    @Deprecated
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
                deductInventoryInTransaction(conn, cartItems, inventory, updatedProducts);
                if (member != null) {
                    applyMemberInTransaction(conn, member, transaction, payableAmount);
                }
                persistTransactionAndPromotion(conn, transaction, appliedPromotion, transactionId);
                return true;
            });

            if (!success) {
                logger.warn("Transaction not committed: transactionId={}", transactionId);
                return new TransactionResult(false, null, I18nManager.getInstance().get("service.transaction_failed"), null);
            }

            for (Product product : updatedProducts) {
                inventory.put(product.name, product);
            }

            logger.info("交易成功完成，交易ID: {}", transactionId);
            AuditService.success(transaction.operatorUsername, "TRANSACTION", "SALE_COMPLETED",
                "交易单号=" + transactionId + ", 金额=" + transaction.finalAmount,
                cartItems.size());
            
            // 广播交易成功事件
            com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
                com.cashier.api.sync.SyncEventType.TRANSACTION_CREATED,
                Map.of(
                    "transactionId", transactionId,
                    "finalAmount", transaction.finalAmount.toString(),
                    "paymentMethod", transaction.paymentMethod,
                    "timestamp", transaction.timestamp,
                    "itemCount", cartItems.size()
                )
            );
            
            return new TransactionResult(true, transactionId, I18nManager.getInstance().get("service.transaction_success"), transaction);
        } catch (SQLException | RuntimeException e) {
            logger.error("Transaction failed: {}", e.getMessage(), e);
            AuditService.failure(transaction.operatorUsername, "TRANSACTION", "SALE_FAILED",
                "transactionId=" + transactionId + ", reason=" + e.getMessage());
            return new TransactionResult(false, null, I18nManager.getInstance().get("service.transaction_failed_detail", e.getMessage()), null);
        }
    }

    /**
     * 在事务内扣减库存（乐观锁）。
     */
    private static void deductInventoryInTransaction(Connection conn, List<CartItem> cartItems,
                                                     Map<String, Product> inventory,
                                                     List<Product> updatedProducts) throws SQLException {
        for (CartItem item : cartItems) {
            Product product = inventory.get(item.product.name);
            if (product == null) {
                throw new SQLException(I18nManager.getInstance().get("service.product_not_found", item.product.name));
            }

            Product latestProduct = productDAO.findByIdWithConnection(conn, item.product.id);
            if (latestProduct == null) {
                throw new SQLException(I18nManager.getInstance().get("service.product_not_found", item.product.name));
            }

            if (latestProduct.quantity < item.quantity) {
                throw new SQLException(I18nManager.getInstance().get("service.product_out_of_stock",
                    item.product.name, latestProduct.quantity, item.quantity));
            }

            // 直接扣减事务内重读的那一行：既不把调用方共享的内存/缓存对象提前改掉
            // （回滚时无需复原），也不会用缓存里的旧价格/旧名称覆盖别处刚提交的修改。
            latestProduct.quantity = latestProduct.quantity - item.quantity;

            if (!productDAO.updateWithVersionWithConnection(conn, latestProduct)) {
                throw new SQLException(I18nManager.getInstance().get("service.product_update_failed", item.product.name));
            }

            updatedProducts.add(latestProduct);
        }
    }

    /**
     * 在事务内更新会员积分/余额/等级。
     */
    private static void applyMemberInTransaction(Connection conn, Member member, Transaction transaction,
                                                 BigDecimal payableAmount) throws SQLException {
        Member latestMember = member.id > 0
            ? DAOFactory.getInstance().getMemberDAO().findByIdWithConnection(conn, member.id)
            : DAOFactory.getInstance().getMemberDAO().findByPhoneWithConnection(conn, member.phone);
        if (latestMember == null) {
            throw new SQLException(I18nManager.getInstance().get("service.member_not_found", member.phone));
        }

        // 支付方式可能是本地化文案或接口传入的代码，必须先归一化再判断，
        // 否则 "MEMBER_BALANCE"/英文文案会记成已付款却不扣会员余额。
        boolean memberBalancePayment = "MEMBER_BALANCE".equals(
            com.cashier.util.I18nUiUtils.canonicalPaymentMethod(transaction.paymentMethod));
        if (memberBalancePayment && latestMember.getBalance().compareTo(payableAmount) < 0) {
            throw new SQLException(I18nManager.getInstance().get("service.member_balance_insufficient",
                latestMember.getBalance(), payableAmount));
        }

        // L-1: 使用 FLOOR 而非 DOWN，保证负数（退货场景）也向下取整，业务行为一致
        BigDecimal earnedPoints = payableAmount.multiply(BigDecimal.TEN).setScale(0, RoundingMode.FLOOR);
        BigDecimal updatedPoints = latestMember.getPoints().add(earnedPoints);
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
        member.balance = memberBalancePayment
            ? latestMember.getBalance().subtract(payableAmount)
            : latestMember.getBalance();
        member.points = updatedPoints;

        if (!DAOFactory.getInstance().getMemberDAO().updateWithConnection(conn, member)) {
            throw new SQLException(I18nManager.getInstance().get("service.member_update_failed"));
        }
    }

    /**
     * 在事务内落库交易主记录并累加促销使用次数。
     */
    private static void persistTransactionAndPromotion(Connection conn, Transaction transaction,
                                                       Promotion appliedPromotion, String transactionId) throws SQLException {
        if (!DAOFactory.getInstance().getTransactionDAO().insertWithConnection(conn, transaction)) {
            throw new SQLException(I18nManager.getInstance().get("service.transaction_save_failed", transactionId));
        }

        if (appliedPromotion != null && !DAOFactory.getInstance().getPromotionDAO().incrementUsageWithConnection(conn, appliedPromotion.id)) {
            throw new SQLException(I18nManager.getInstance().get("service.promotion_update_failed", appliedPromotion.id));
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
        transaction.timestamp = com.cashier.util.DateTimeFormats.formatStandard(LocalDateTime.now());
        transaction.items = new ArrayList<>();

        for (CartItem item : cartItems) {
            transaction.items.add(item.product);
        }

        transaction.totalAmount = calculateTotalAmount(cartItems);
        transaction.tax = calculateTax(transaction.getTotalAmount());
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
        // 规整到 2 位小数，避免下游比较/显示出错
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按系统设置中的税率计算税额。
     *
     * <p>税率以小数形式存储：设置界面校验区间为 0.0-1.0，例如 {@code 0.13} 表示 13%，
     * 因此不再除以 100。税率缺失或无法解析时按 0 处理。</p>
     *
     * @param amount 计税基数
     * @return 税额，保留 2 位小数
     */
    public static BigDecimal calculateTax(BigDecimal amount) {
        String configured = DataService.loadSettings().getOrDefault("taxRate", "0.0");
        BigDecimal taxRate;
        try {
            taxRate = new BigDecimal(configured.trim());
        } catch (NumberFormatException e) {
            logger.warn("税率配置无法解析，按 0 处理: {}", configured);
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 选择最优促销（优惠金额最大者）。
     * 口径与标准收银台一致：促销门槛与优惠金额均按商品原价总额计算。
     *
     * @param totalAmount 商品原价总额
     * @return 最优促销；无可用促销时返回 null
     */
    public static Promotion selectBestPromotion(BigDecimal totalAmount) {
        Promotion bestPromotion = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        try {
            for (Promotion promotion : DAOFactory.getInstance().getPromotionDAO().findActive()) {
                BigDecimal discount = promotion.calculateDiscount(totalAmount);
                if (discount.compareTo(bestDiscount) > 0) {
                    bestDiscount = discount;
                    bestPromotion = promotion;
                }
            }
        } catch (Exception e) {
            logger.error("加载促销数据失败", e);
        }
        return bestPromotion;
    }

    /**
     * 计算最终金额（会员折扣 + 促销优惠）。
     * 促销优惠按商品原价总额计算，再从会员折后金额中扣除，与标准收银台口径一致。
     *
     * @param cartItems 购物车商品列表
     * @param member 会员（可为 null）
     * @param promotion 已应用的促销（可为 null）
     * @return 最终应付金额，保留 2 位小数
     */
    public static BigDecimal calculateFinalAmount(List<CartItem> cartItems, Member member, Promotion promotion) {
        BigDecimal amount = calculateFinalAmount(cartItems, member);
        if (promotion != null) {
            BigDecimal discount = promotion.calculateDiscount(calculateTotalAmount(cartItems));
            amount = amount.subtract(discount).max(BigDecimal.ZERO);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成交易单号（ORD + 时间戳 + 序号 + 进程随机段）
     *
     * <p>序号是 JVM 内 AtomicLong（进程重启后归零）；若多个进程/实例在同一毫秒生成单号
     * （各自序号都从 0 开始）会撞 transactions 主键。追加 32 位 SecureRandom 随机段后，
     * 跨进程同毫秒冲突概率可忽略。</p>
     */
    private static final java.util.concurrent.atomic.AtomicLong orderSequence =
        new java.util.concurrent.atomic.AtomicLong(0);
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    public static String generateOrderNumber() {
        String ts = com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME_MILLIS.format(LocalDateTime.now());
        String seq = String.format("%04d", orderSequence.getAndIncrement() % 10000);
        String randomSuffix = String.format("%08x", SECURE_RANDOM.nextInt());
        return "ORD" + ts + seq + randomSuffix;
    }

    /**
     * 获取交易统计信息
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    public static TransactionStatistics getTransactionStatistics(String startDate, String endDate) {
        try {
            return DAOFactory.getInstance().getTransactionDAO().getStatistics(startDate, endDate);
        } catch (SQLException e) {
            logger.error("获取交易统计失败", e);
            return new TransactionStatistics(0, BigDecimal.ZERO, 0, 0, 0);
        }
    }
}
