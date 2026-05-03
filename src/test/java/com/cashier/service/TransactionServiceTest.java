package com.cashier.service;

import com.cashier.dao.MemberDAO;
import com.cashier.dao.ProductDAO;
import com.cashier.dao.PromotionDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.util.DatabaseTestBase;
import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.model.Promotion;
import com.cashier.model.Transaction;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
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
        MemberDAO.insert(testMember);

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
        MemberDAO.update(testMember);

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
        Product initialProduct1 = ProductDAO.findByName("商品1");
        Product initialProduct2 = ProductDAO.findByName("商品2");

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
        Product updatedProduct1 = ProductDAO.findByName("商品1");
        Product updatedProduct2 = ProductDAO.findByName("商品2");

        assertEquals(initialProduct1.quantity - 2, updatedProduct1.quantity);
        assertEquals(initialProduct2.quantity - 1, updatedProduct2.quantity);
    }

    @Test
    @Order(4)
    @DisplayName("测试交易扣减会员余额")
    void testTransactionDeductMemberBalance() throws Exception {
        // 设置会员折扣
        testMember.discount = BigDecimal.valueOf(9.0);
        MemberDAO.update(testMember);

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
        Member updatedMember = MemberDAO.findByPhone(testMember.phone);
        assertAmountEquals(initialBalance.subtract(BigDecimal.valueOf(finalAmount)), updatedMember.balance);

        // 验证会员积分已增加
        assertAmountEquals(100.0 + finalAmount * 10, updatedMember.points);
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
        MemberDAO.update(testMember);

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
        PromotionDAO.insert(promotion);

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

        Promotion updatedPromotion = PromotionDAO.findById(promotion.id);
        assertNotNull(updatedPromotion);
        assertEquals(1, updatedPromotion.usageCount);
        assertEquals(1, TransactionDAO.findAll().size());
    }

    @Test
    @Order(10)
    @DisplayName("测试促销次数更新失败时交易与库存一起回滚")
    void testExecutePreparedTransactionRollbackOnPromotionUpdateFailure() throws Exception {
        int initialProduct1Quantity = ProductDAO.findById(testProducts.get(0).id).quantity;
        int initialProduct2Quantity = ProductDAO.findById(testProducts.get(1).id).quantity;

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
        assertEquals(initialProduct1Quantity, ProductDAO.findById(testProducts.get(0).id).quantity);
        assertEquals(initialProduct2Quantity, ProductDAO.findById(testProducts.get(1).id).quantity);
        assertTrue(TransactionDAO.findAll().isEmpty());
    }

    @Test
    @Order(11)
    @DisplayName("测试会员交易在同一事务内更新等级与折扣")
    void testExecuteTransactionUpdatesMemberLevelWithinTransaction() throws Exception {
        testMember.points = BigDecimal.valueOf(995.0);
        testMember.level = "普通";
        testMember.discount = BigDecimal.TEN;
        MemberDAO.update(testMember);

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            testMember,
            "会员余额",
            40.0,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());
        Member updatedMember = MemberDAO.findByPhone(testMember.phone);
        assertEquals("银卡", updatedMember.level);
        assertAmountEquals(9.5, updatedMember.discount);
        assertEquals("银卡", testMember.level);
        assertAmountEquals(9.5, testMember.discount);
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

        ProductDAO.insert(product);
        return ProductDAO.findByName(name);
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
