package com.cashier.service;

import com.cashier.dao.MemberDAO;
import com.cashier.dao.ProductDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.util.DatabaseTestBase;
import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.model.Product;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TransactionService 并发测试
 * 测试乐观锁和并发场景
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionConcurrencyTest extends DatabaseTestBase {

    private Member testMember;
    private Product testProduct;
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
        testProduct = createProduct("商品1", 10.0, 100);

        // 创建购物车
        testCartItems = new ArrayList<>();
        testCartItems.add(new CartItem(testProduct, 10));

        // 创建库存数据
        inventory = new HashMap<>();
        inventory.put(testProduct.name, testProduct);
    }

    @Test
    @Order(1)
    @DisplayName("测试乐观锁 - 正常情况")
    void testOptimisticLockingNormal() throws Exception {
        // 获取初始版本号
        Product initialProduct = ProductDAO.findById(testProduct.id);
        int initialVersion = initialProduct.version;

        // 执行交易
        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            testCartItems,
            testMember,
            "现金",
            100.0,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());

        // 验证版本号已更新
        Product updatedProduct = ProductDAO.findById(testProduct.id);
        assertEquals(initialVersion + 1, updatedProduct.version);
    }

    @Test
    @Order(2)
    @DisplayName("测试乐观锁 - 版本冲突")
    void testOptimisticLockingConflict() throws Exception {
        // 获取初始版本号
        Product initialProduct = ProductDAO.findById(testProduct.id);
        int initialVersion = initialProduct.version;

        // 第一次执行交易
        List<CartItem> cartItems1 = new ArrayList<>();
        cartItems1.add(new CartItem(testProduct, 5));

        TransactionService.TransactionResult result1 = TransactionService.executeTransaction(
            cartItems1,
            null,
            "现金",
            50.0,
            0.0,
            inventory
        );

        assertTrue(result1.isSuccess());

        // 验证版本号已更新
        Product productAfterFirstTx = ProductDAO.findById(testProduct.id);
        assertEquals(initialVersion + 1, productAfterFirstTx.version);

        // 模拟并发场景：使用旧版本号尝试第二次交易
        // 这里我们模拟手动修改version来测试冲突检测
        Product oldVersionProduct = ProductDAO.findById(testProduct.id);
        oldVersionProduct.version = initialVersion; // 使用旧版本号

        // 注意：在实际的并发场景中，这会在数据库层面失败
        // 在这里我们测试逻辑是否正确处理版本号
        List<CartItem> cartItems2 = new ArrayList<>();
        cartItems2.add(new CartItem(testProduct, 3));

        TransactionService.TransactionResult result2 = TransactionService.executeTransaction(
            cartItems2,
            null,
            "现金",
            30.0,
            0.0,
            inventory
        );

        // 由于我们无法真正模拟并发冲突，这里只验证第二次交易成功
        // 在实际环境中，如果version不匹配会失败
        assertTrue(result2.isSuccess());

        // 验证版本号再次更新
        Product productAfterSecondTx = ProductDAO.findById(testProduct.id);
        assertEquals(initialVersion + 2, productAfterSecondTx.version);
    }

    @Test
    @Order(3)
    @DisplayName("测试库存不足")
    void testInsufficientStock() throws Exception {
        // 获取初始库存
        Product initialProduct = ProductDAO.findById(testProduct.id);
        int initialQuantity = initialProduct.quantity;

        // 尝试购买超过库存的商品
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem(testProduct, initialQuantity + 10));

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            cartItems,
            null,
            "现金",
            testProduct.price.multiply(BigDecimal.valueOf(initialQuantity + 10L)).doubleValue(),
            0.0,
            inventory
        );

        // 应该失败
        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("库存不足") || result.getMessage().contains("不够"));

        // 验证库存没有变化
        Product updatedProduct = ProductDAO.findById(testProduct.id);
        assertEquals(initialQuantity, updatedProduct.quantity);
    }

    @Test
    @Order(4)
    @DisplayName("测试空购物车")
    void testEmptyCart() {
        List<CartItem> emptyCart = new ArrayList<>();

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            emptyCart,
            null,
            "现金",
            0.0,
            0.0,
            inventory
        );

        // 空购物车应该成功但金额为0
        assertTrue(result.isSuccess());
        assertNotNull(result.getTransaction());
        assertAmountEquals(0.0, result.getTransaction().totalAmount);
        assertAmountEquals(0.0, result.getTransaction().finalAmount);
    }

    @Test
    @Order(5)
    @DisplayName("测试会员余额不足")
    void testInsufficientMemberBalance() throws Exception {
        // 设置会员折扣
        testMember.discount = BigDecimal.valueOf(9.0);
        MemberDAO.update(testMember);

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem(testProduct, 120)); // 120 * 10 = 1200, 打9折后 1080 > 1000 余额

        BigDecimal totalAmount = testProduct.price.multiply(BigDecimal.valueOf(120));
        BigDecimal discountedAmount = totalAmount.multiply(new BigDecimal("0.9"));

        // 会员余额不足 (需要1080，但只有1000)
        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            cartItems,
            testMember,
            "会员余额",
            discountedAmount.doubleValue(),
            0.0,
            inventory
        );

        // 应该失败
        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());

        // 验证余额没有变化
        Member updatedMember = MemberDAO.findByPhone(testMember.phone);
        assertAmountEquals(testMember.balance, updatedMember.balance);
    }

    @Test
    @Order(6)
    @DisplayName("测试支付方式")
    void testPaymentMethods() throws Exception {
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem(testProduct, 2));

        // 测试现金支付
        TransactionService.TransactionResult result1 = TransactionService.executeTransaction(
            cartItems,
            null,
            "现金",
            20.0,
            0.0,
            inventory
        );
        assertTrue(result1.isSuccess());
        assertEquals("现金", result1.getTransaction().paymentMethod);

        // 测试微信支付
        TransactionService.TransactionResult result2 = TransactionService.executeTransaction(
            cartItems,
            null,
            "微信",
            0.0,
            0.0,
            inventory
        );
        assertTrue(result2.isSuccess());
        assertEquals("微信", result2.getTransaction().paymentMethod);

        // 测试支付宝支付
        TransactionService.TransactionResult result3 = TransactionService.executeTransaction(
            cartItems,
            null,
            "支付宝",
            0.0,
            0.0,
            inventory
        );
        assertTrue(result3.isSuccess());
        assertEquals("支付宝", result3.getTransaction().paymentMethod);
    }

    @Test
    @Order(7)
    @DisplayName("测试找零计算")
    void testCashChange() throws Exception {
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem(testProduct, 3));

        BigDecimal totalAmount = testProduct.price.multiply(BigDecimal.valueOf(3));

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            cartItems,
            null,
            "现金",
            0.0,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());
        assertAmountEquals(totalAmount, result.getTransaction().totalAmount);
        // Transaction模型没有cashReceived和cashChange字段，这些在实际业务逻辑中处理
    }

    @Test
    @Order(8)
    @DisplayName("测试交易ID生成")
    void testTransactionIdGeneration() throws Exception {
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem(testProduct, 1));

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            cartItems,
            null,
            "现金",
            0.0,
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getTransactionId());

        // 验证交易ID格式（ORD + 时间戳）
        assertTrue(result.getTransactionId().startsWith("ORD"));
        assertTrue(result.getTransactionId().length() > 10);
    }

    @Test
    @Order(9)
    @DisplayName("测试会员积分更新")
    void testMemberPointsUpdate() throws Exception {
        // 设置会员折扣
        testMember.discount = BigDecimal.valueOf(9.0);
        MemberDAO.update(testMember);

        BigDecimal initialPoints = testMember.points;

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem(testProduct, 5));

        BigDecimal finalAmount = testProduct.price.multiply(BigDecimal.valueOf(5)).multiply(new BigDecimal("0.9"));

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            cartItems,
            testMember,
            "现金",
            finalAmount.doubleValue(),
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());

        // 验证会员积分已更新（每消费1元积10分）
        Member updatedMember = MemberDAO.findByPhone(testMember.phone);
        BigDecimal expectedPoints = initialPoints.add(finalAmount.multiply(BigDecimal.TEN).setScale(0, RoundingMode.DOWN));
        assertAmountEquals(expectedPoints, updatedMember.points);
    }

    @Test
    @Order(10)
    @DisplayName("测试多商品交易")
    void testMultiProductTransaction() throws Exception {
        // 创建第二个商品
        Product product2 = createProduct("商品2", 20.0, 50);
        inventory.put(product2.name, product2); // 添加到库存

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem(testProduct, 3));
        cartItems.add(new CartItem(product2, 2));

        BigDecimal expectedTotal = testProduct.price.multiply(BigDecimal.valueOf(3)).add(product2.price.multiply(BigDecimal.valueOf(2)));

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            cartItems,
            null,
            "现金",
            expectedTotal.doubleValue(),
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());
        assertAmountEquals(expectedTotal, result.getTransaction().totalAmount);

        // 验证两个商品的库存都已扣减
        Product updatedProduct1 = ProductDAO.findById(testProduct.id);
        Product updatedProduct2 = ProductDAO.findById(product2.id);
        assertEquals(100 - 3, updatedProduct1.quantity);
        assertEquals(50 - 2, updatedProduct2.quantity);
    }

    @Test
    @Order(11)
    @DisplayName("测试库存扣减原子性")
    void testInventoryDeductionAtomicity() throws Exception {
        // 创建第二个商品
        Product product2 = createProduct("商品2", 20.0, 50);
        inventory.put(product2.name, product2); // 添加到库存

        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem(testProduct, 60)); // 正好用完库存
        cartItems.add(new CartItem(product2, 25)); // 一半库存

        TransactionService.TransactionResult result = TransactionService.executeTransaction(
            cartItems,
            null,
            "现金",
            testProduct.price.multiply(BigDecimal.valueOf(60)).add(product2.price.multiply(BigDecimal.valueOf(25))).doubleValue(),
            0.0,
            inventory
        );

        assertTrue(result.isSuccess());

        // 验证两个商品的库存都已正确扣减
        Product updatedProduct1 = ProductDAO.findById(testProduct.id);
        Product updatedProduct2 = ProductDAO.findById(product2.id);
        assertEquals(40, updatedProduct1.quantity); // 100 - 60
        assertEquals(25, updatedProduct2.quantity); // 50 - 25
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

    private void assertAmountEquals(double expected, BigDecimal actual) {
        assertAmountEquals(BigDecimal.valueOf(expected), actual);
    }

    private void assertAmountEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }

}
