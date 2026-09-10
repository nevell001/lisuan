package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.PromotionDAORefactored;
import com.cashier.dao.TransactionDAORefactored;
import com.cashier.dao.MemberDAORefactored;
import com.cashier.util.DatabaseTestBase;
import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.model.Promotion;
import com.cashier.model.Transaction;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TransactionService 单元测试
 * 测试交易服务的核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionServiceTest extends DatabaseTestBase {

    private final PromotionDAORefactored promotionDAO = DAOFactory.getInstance().getPromotionDAO();
    private final TransactionDAORefactored transactionDAO = DAOFactory.getInstance().getTransactionDAO();
    private final MemberDAORefactored memberDAO = DAOFactory.getInstance().getMemberDAO();

    private Member testMember;
    private List<Product> testProducts;
    private List<CartItem> testCartItems;
    private Map<String, Product> inventory;

    @BeforeEach
    void setUp() throws Exception {
        // 确保使用测试数据库
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        
        // 清空数据库
        clearTestData();

        // 创建测试会员
        testMember = new Member();
        testMember.phone = "13800138000";
        testMember.name = "测试会员";
        testMember.balance = BigDecimal.valueOf(1000.0);
        testMember.points = BigDecimal.valueOf(100.0);
        testMember.level = "普通";
        testMember.discount = BigDecimal.TEN;
        memberDAO.insert(testMember);

        // 创建测试商品
        testProducts = new ArrayList<>();
        testProducts.add(createProduct("商品1", 10.0, 100));
        testProducts.add(createProduct("商品2", 20.0, 50));
        testProducts.add(createProduct("商品3", 30.0, 30));

        // 创建购物车
        testCartItems = new ArrayList<>();
        testCartItems.add(new CartItem(testProducts.get(0), 2));
        testCartItems.add(new CartItem(testProducts.get(1), 1));

        // 创建库存数据
        inventory = new HashMap<>();
        for (Product product : testProducts) {
            inventory.put(product.name, product);
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试执行交易 - 无会员")
    void testExecuteTransactionWithoutMember() throws Exception {
        // 执行交易
        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            null,
            "现金",
            0.0,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getTransaction());
        assertEquals("现金", result.getTransaction().paymentMethod);
        assertAmountEquals(40.0, result.getTransaction().totalAmount); // 2*10 + 1*20
        assertAmountEquals(40.0, result.getTransaction().finalAmount);
    }

    @Test
    @Order(2)
    @DisplayName("测试执行交易 - 有会员折扣")
    void testExecuteTransactionWithMember() throws Exception {
        // 设置会员折扣为9折
        testMember.discount = BigDecimal.valueOf(9.0);
        memberDAO.update(testMember);

        // 执行交易
        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            testMember,
            "微信",
            0.0,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());
        assertEquals("微信", result.getTransaction().paymentMethod);
        assertAmountEquals(40.0, result.getTransaction().totalAmount);
        assertAmountEquals(36.0, result.getTransaction().finalAmount); // 40 * 0.9
        assertEquals(testMember.phone, result.getTransaction().memberPhone);
    }

    @Test
    @Order(3)
    @DisplayName("测试交易扣减库存")
    void testTransactionDeductInventory() throws Exception {
        // 获取初始库存
        Product initialProduct1 = DAOFactory.getInstance().getProductDAO().findByName("商品1");
        Product initialProduct2 = DAOFactory.getInstance().getProductDAO().findByName("商品2");

        // 执行交易
        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            null,
            "支付宝",
            0.0,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());

        // 验证库存已扣减
        Product updatedProduct1 = DAOFactory.getInstance().getProductDAO().findByName("商品1");
        Product updatedProduct2 = DAOFactory.getInstance().getProductDAO().findByName("商品2");

        assertEquals(initialProduct1.quantity - 2, updatedProduct1.quantity);
        assertEquals(initialProduct2.quantity - 1, updatedProduct2.quantity);
    }

    @Test
    @Order(4)
    @DisplayName("测试交易扣减会员余额")
    void testTransactionDeductMemberBalance() throws Exception {
        // 设置会员折扣
        testMember.discount = BigDecimal.valueOf(9.0);
        memberDAO.update(testMember);

        BigDecimal initialBalance = testMember.balance;
        double finalAmount = 36.0; // 40 * 0.9

        // 执行交易
        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            testMember,
            "会员余额",
            finalAmount,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());

        // 验证会员余额已扣减
        Member updatedMember = memberDAO.findByPhone(testMember.phone);
        assertAmountEquals(initialBalance.subtract(BigDecimal.valueOf(finalAmount)), updatedMember.balance);

        // 验证会员积分已增加
        assertAmountEquals(100.0 + finalAmount * 10, updatedMember.points);
    }

    @Test
    @DisplayName("非会员余额支付只累计积分而不扣会员余额")
    void externalPaymentDoesNotDeductMemberBalance() throws Exception {
        BigDecimal initialBalance = testMember.balance;

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            testMember,
            "微信",
            0.0,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());
        Member updatedMember = memberDAO.findByPhone(testMember.phone);
        assertAmountEquals(initialBalance, updatedMember.balance);
        assertTrue(updatedMember.points.compareTo(BigDecimal.valueOf(100)) > 0);
    }

    @Test
    @Order(5)
    @DisplayName("测试空购物车交易")
    void testEmptyCartTransaction() throws Exception {
        List<CartItem> emptyCart = new ArrayList<>();

        // 空购物车应该失败或返回0金额的交易
        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            emptyCart,
            null,
            "现金",
            0.0,
            0.0,
            inventory
        );

        // 检查交易结果
        if (result.isSuccess()) {
            // 如果成功，验证金额为0
            assertAmountEquals(0.0, result.getTransaction().totalAmount);
        } else {
            // 如果失败，验证有错误消息
            assertNotNull(result.getMessage());
            assertFalse(result.getMessage().isEmpty());
        }
    }

    @Test
    @Order(6)
    @DisplayName("测试计算总金额")
    void testCalculateTotalAmount() {
        BigDecimal total = TransactionService.calculateTotalAmount(testCartItems);
        assertAmountEquals(40.0, total); // 2*10 + 1*20
    }

    @Test
    @Order(7)
    @DisplayName("测试计算最终金额 - 无会员")
    void testCalculateFinalAmountWithoutMember() {
        BigDecimal finalAmount = TransactionService.calculateFinalAmount(testCartItems, null);
        assertAmountEquals(40.0, finalAmount);
    }

    @Test
    @Order(8)
    @DisplayName("测试计算最终金额 - 有会员")
    void testCalculateFinalAmountWithMember() throws Exception {
        testMember.discount = BigDecimal.valueOf(9.0);
        memberDAO.update(testMember);

        BigDecimal finalAmount = TransactionService.calculateFinalAmount(testCartItems, testMember);
        assertAmountEquals(36.0, finalAmount); // 40 * 0.9
    }

    @Test
    @Order(9)
    @DisplayName("测试执行预构造交易时会原子增加促销使用次数")
    void testExecutePreparedTransactionIncrementsPromotionUsage() throws Exception {
        Promotion promotion = new Promotion();
        promotion.name = "立减5元";
        promotion.type = "满减";
        promotion.threshold = BigDecimal.valueOf(30.0);
        promotion.discount = BigDecimal.valueOf(5.0);
        promotion.description = "测试促销";
        promotion.enabled = true;
        promotionDAO.insert(promotion);

        Transaction transaction = createPreparedTransaction("T-PROMO-001", "现金", BigDecimal.valueOf(35.0), null);

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            null,
            transaction,
            inventory,
            promotion
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getTransaction());
        assertEquals("T-PROMO-001", result.getTransaction().transactionId);
        assertAmountEquals(35.0, result.getTransaction().finalAmount);

        Promotion updatedPromotion = promotionDAO.findById(promotion.id);
        assertNotNull(updatedPromotion);
        assertEquals(1, updatedPromotion.usageCount);
        assertEquals(1, transactionDAO.findAll().size());
    }

    @Test
    @Order(10)
    @DisplayName("测试促销次数更新失败时交易与库存一起回滚")
    void testExecutePreparedTransactionRollbackOnPromotionUpdateFailure() throws Exception {
        int initialProduct1Quantity = DAOFactory.getInstance().getProductDAO().findById(testProducts.get(0).id).quantity;
        int initialProduct2Quantity = DAOFactory.getInstance().getProductDAO().findById(testProducts.get(1).id).quantity;

        Promotion invalidPromotion = new Promotion();
        invalidPromotion.id = Integer.MAX_VALUE;

        Transaction transaction = createPreparedTransaction("T-PROMO-ROLLBACK", "现金", BigDecimal.valueOf(35.0), null);

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            null,
            transaction,
            inventory,
            invalidPromotion
        );

        assertFalse(result.isSuccess());
        assertNull(result.getTransaction());
        assertTrue(result.getMessage().contains("更新促销使用次数失败"));
        assertEquals(initialProduct1Quantity, DAOFactory.getInstance().getProductDAO().findById(testProducts.get(0).id).quantity);
        assertEquals(initialProduct2Quantity, DAOFactory.getInstance().getProductDAO().findById(testProducts.get(1).id).quantity);
        assertTrue(transactionDAO.findAll().isEmpty());
    }

    @Test
    @Order(11)
    @DisplayName("测试会员交易在同一事务内更新等级与折扣")
    void testExecuteTransactionUpdatesMemberLevelWithinTransaction() throws Exception {
        testMember.points = BigDecimal.valueOf(995.0);
        testMember.level = "普通";
        testMember.discount = BigDecimal.TEN;
        memberDAO.update(testMember);

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            testMember,
            "会员余额",
            40.0,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());
        Member updatedMember = memberDAO.findByPhone(testMember.phone);
        assertEquals("银卡", updatedMember.level);
        assertAmountEquals(9.5, updatedMember.discount);
        assertEquals("银卡", testMember.level);
        assertAmountEquals(9.5, testMember.discount);
    }

    @Test
    @DisplayName("税率按小数解析：0.13 表示 13%，不再除以 100")
    void taxRateIsFractionNotPercentage() throws Exception {
        Map<String, String> settings = new HashMap<>();
        settings.put("taxRate", "0.13");
        DataService.saveSettings(settings);

        // 40.00 * 0.13 = 5.20（若误按百分比再除以 100 会得到 0.05）
        assertAmountEquals(5.20, TransactionService.calculateTax(new BigDecimal("40.00")));

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems, null, "现金", 0.0, 0.0, inventory);

        assertTrue(result.isSuccess());
        assertAmountEquals(5.20, result.getTransaction().tax);
    }

    @Test
    @DisplayName("税率配置非法时按 0 计税，不抛出异常")
    void invalidTaxRateFallsBackToZero() throws Exception {
        Map<String, String> settings = new HashMap<>();
        settings.put("taxRate", "not-a-number");
        DataService.saveSettings(settings);

        assertAmountEquals(BigDecimal.ZERO, TransactionService.calculateTax(new BigDecimal("40.00")));
    }

    @Test
    @DisplayName("最终金额 = 会员折后金额 - 促销优惠（促销按原价总额计算）")
    void promotionDiscountAppliesAfterMemberDiscount() {
        // 会员 9 折
        testMember.discount = BigDecimal.valueOf(9.0);

        // 原价总额 40.00，满 30 减 5
        Promotion promotion = activePromotion("满30减5", "满减",
            new BigDecimal("30"), new BigDecimal("5"));

        // 40 * 0.9 = 36.00，再减 5 = 31.00
        assertAmountEquals(new BigDecimal("31.00"),
            TransactionService.calculateFinalAmount(testCartItems, testMember, promotion));
        // 无促销时仍只有会员折扣
        assertAmountEquals(new BigDecimal("36.00"),
            TransactionService.calculateFinalAmount(testCartItems, testMember, null));
    }

    @Test
    @DisplayName("促销优惠大于应付金额时应付金额下限为 0")
    void promotionNeverProducesNegativeAmount() {
        Promotion promotion = activePromotion("满0减1000", "满减",
            BigDecimal.ZERO, new BigDecimal("1000"));

        assertAmountEquals(BigDecimal.ZERO,
            TransactionService.calculateFinalAmount(testCartItems, null, promotion));
    }

    @Test
    @DisplayName("selectBestPromotion 返回优惠金额最大的促销")
    void selectBestPromotionPicksLargestDiscount() throws Exception {
        promotionDAO.insert(activePromotion("满30减5", "满减", new BigDecimal("30"), new BigDecimal("5")));
        promotionDAO.insert(activePromotion("满30减8", "满减", new BigDecimal("30"), new BigDecimal("8")));
        promotionDAO.insert(activePromotion("满100减20", "满减", new BigDecimal("100"), new BigDecimal("20")));

        Promotion best = TransactionService.selectBestPromotion(new BigDecimal("40.00"));

        assertNotNull(best);
        assertEquals("满30减8", best.name);
    }

    @Test
    @DisplayName("没有满足门槛的促销时 selectBestPromotion 返回 null")
    void selectBestPromotionReturnsNullWhenNoneApplies() throws Exception {
        promotionDAO.insert(activePromotion("满500减50", "满减", new BigDecimal("500"), new BigDecimal("50")));

        assertNull(TransactionService.selectBestPromotion(new BigDecimal("40.00")));
    }

    @Test
    @DisplayName("交易失败回滚后内存库存不受影响")
    void failedTransactionLeavesInMemoryInventoryUntouched() throws Exception {
        Product product = testProducts.get(0);
        int originalQuantity = product.quantity;

        // 第 1 行正常、第 2 行超库存 -> 整单回滚
        List<CartItem> oversized = new ArrayList<>();
        oversized.add(new CartItem(product, 1));
        oversized.add(new CartItem(product, originalQuantity + 1));

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            oversized, null, "现金", 0.0, 0.0, inventory);

        assertFalse(result.isSuccess());
        assertEquals(originalQuantity, inventory.get(product.name).quantity,
            "回滚后内存中的库存不应被扣减");
        assertEquals(originalQuantity,
            DAOFactory.getInstance().getProductDAO().findByName(product.name).quantity,
            "回滚后数据库库存应保持原值");
    }

    @Test
    @DisplayName("扣减库存不会用内存里的旧字段覆盖别处刚改的价格")
    void deductionDoesNotOverwriteConcurrentlyEditedFields() throws Exception {
        Product stale = testProducts.get(0);   // 收银台内存快照，价格仍是 10.00

        // 另一端把价格改成 99.00 并提交
        Product edited = DAOFactory.getInstance().getProductDAO().findByName(stale.name);
        edited.price = new BigDecimal("99.00");
        assertTrue(DAOFactory.getInstance().getProductDAO().update(edited));

        // 用仍持旧价格快照的购物车结账
        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            List.of(new CartItem(stale, 1)), null, "现金", 0.0, 0.0, inventory);
        assertTrue(result.isSuccess());

        Product after = DAOFactory.getInstance().getProductDAO().findByName(stale.name);
        assertAmountEquals(new BigDecimal("99.00"), after.price);
    }

    @Test
    @DisplayName("应付金额统一四舍五入到 2 位小数")
    void finalAmountIsRoundedToTwoDecimals() {
        Product product = testProducts.get(0);
        product.price = new BigDecimal("1.90");
        testMember.discount = BigDecimal.valueOf(9.5);   // 9.5 折

        BigDecimal finalAmount =
            TransactionService.calculateFinalAmount(List.of(new CartItem(product, 1)), testMember);

        // 1.90 * 0.95 = 1.805 -> HALF_UP -> 1.81（未四舍五入会得到 1.8050，按显示金额付款被判金额不足）
        assertEquals(2, finalAmount.scale());
        assertAmountEquals(new BigDecimal("1.81"), finalAmount);
    }

    private Promotion activePromotion(String name, String type, BigDecimal threshold, BigDecimal discount) {
        Promotion promotion = new Promotion();
        promotion.promotionCode = "PROMO_" + name.hashCode() + "_" + System.nanoTime();
        promotion.name = name;
        promotion.type = type;
        promotion.threshold = threshold;
        promotion.discount = discount;
        promotion.description = "测试促销：" + name;
        promotion.enabled = true;
        promotion.startDate = LocalDateTime.now().minusDays(1);
        promotion.endDate = LocalDateTime.now().plusDays(30);
        promotion.usageCount = 0;
        promotion.maxUsage = -1;
        return promotion;
    }

    /**
     * 辅助方法：创建测试商品
     */
    private Product createProduct(String name, double price, int quantity) throws Exception {
        Product product = new Product();
        product.productCode = "P" + name.hashCode();
        product.name = name;
        product.price = BigDecimal.valueOf(price);
        product.quantity = quantity;
        product.category = "测试分类";
        product.barcode = "TEST" + name.hashCode();
        product.unit = "个";
        product.minStock = 10;
        product.cost = BigDecimal.valueOf(price).multiply(new BigDecimal("0.7"));
        product.version = 0;

        DAOFactory.getInstance().getProductDAO().insert(product);
        return DAOFactory.getInstance().getProductDAO().findByName(name);
    }

    private Transaction createPreparedTransaction(String transactionId, String paymentMethod, BigDecimal finalAmount, Member member) {
        Transaction transaction = new Transaction();
        transaction.transactionId = transactionId;
        transaction.timestamp = "2026-04-04 13:37:00";
        transaction.totalAmount = finalAmount;
        transaction.tax = BigDecimal.ZERO;
        transaction.finalAmount = finalAmount;
        transaction.paymentMethod = paymentMethod;
        transaction.items = new ArrayList<>();

        for (CartItem cartItem : testCartItems) {
            Product product = new Product();
            product.id = cartItem.product.id;
            product.productCode = cartItem.product.productCode;
            product.barcode = cartItem.product.barcode;
            product.name = cartItem.product.name;
            product.price = cartItem.product.price;
            product.quantity = cartItem.quantity;
            product.category = cartItem.product.category;
            product.unit = cartItem.product.unit;
            product.cost = cartItem.product.cost;
            transaction.items.add(product);
        }

        if (member != null) {
            transaction.memberPhone = member.phone;
        }

        return transaction;
    }

    private void assertAmountEquals(double expected, BigDecimal actual) {
        assertAmountEquals(BigDecimal.valueOf(expected), actual);
    }

    private void assertAmountEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }

}
