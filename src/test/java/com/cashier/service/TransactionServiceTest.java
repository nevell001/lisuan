package com.cashier.service;

import com.cashier.dao.MemberDAO;
import com.cashier.dao.ProductDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.util.DatabaseTestBase;
import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import org.junit.jupiter.api.*;

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
        testMember.balance = 1000.0;
        testMember.points = 100.0;
        testMember.level = "普通";
        testMember.discount = 10.0;
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

        assertTrue(result.success);
        assertNotNull(result.transaction);
        assertEquals("现金", result.transaction.paymentMethod);
        assertEquals(40.0, result.transaction.totalAmount, 0.01); // 2*10 + 1*20
        assertEquals(40.0, result.transaction.finalAmount, 0.01);
    }

    @Test
    @Order(2)
    @DisplayName("测试执行交易 - 有会员折扣")
    void testExecuteTransactionWithMember() throws Exception {
        // 设置会员折扣为9折
        testMember.discount = 9.0;
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

        assertTrue(result.success);
        assertEquals("微信", result.transaction.paymentMethod);
        assertEquals(40.0, result.transaction.totalAmount, 0.01);
        assertEquals(36.0, result.transaction.finalAmount, 0.01); // 40 * 0.9
        assertEquals(testMember.phone, result.transaction.memberPhone);
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

        assertTrue(result.success);

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
        testMember.discount = 9.0;
        MemberDAO.update(testMember);

        double initialBalance = testMember.balance;
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

        assertTrue(result.success);

        // 验证会员余额已扣减
        Member updatedMember = MemberDAO.findByPhone(testMember.phone);
        assertEquals(initialBalance - finalAmount, updatedMember.balance, 0.01);

        // 验证会员积分已增加
        assertEquals(100.0 + finalAmount * 10, updatedMember.points, 0.01);
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
        if (result.success) {
            // 如果成功，验证金额为0
            assertEquals(0.0, result.transaction.totalAmount, 0.01);
        } else {
            // 如果失败，验证有错误消息
            assertNotNull(result.message);
            assertFalse(result.message.isEmpty());
        }
    }

    @Test
    @Order(6)
    @DisplayName("测试计算总金额")
    void testCalculateTotalAmount() {
        double total = TransactionService.calculateTotalAmount(testCartItems);
        assertEquals(40.0, total, 0.01); // 2*10 + 1*20
    }

    @Test
    @Order(7)
    @DisplayName("测试计算最终金额 - 无会员")
    void testCalculateFinalAmountWithoutMember() {
        double finalAmount = TransactionService.calculateFinalAmount(testCartItems, null);
        assertEquals(40.0, finalAmount, 0.01);
    }

    @Test
    @Order(8)
    @DisplayName("测试计算最终金额 - 有会员")
    void testCalculateFinalAmountWithMember() throws Exception {
        testMember.discount = 9.0;
        MemberDAO.update(testMember);

        double finalAmount = TransactionService.calculateFinalAmount(testCartItems, testMember);
        assertEquals(36.0, finalAmount, 0.01); // 40 * 0.9
    }

    /**
     * 辅助方法：创建测试商品
     */
    private Product createProduct(String name, double price, int quantity) throws Exception {
        Product product = new Product();
        product.productCode = "P" + name.hashCode();
        product.name = name;
        product.price = price;
        product.quantity = quantity;
        product.category = "测试分类";
        product.barcode = "TEST" + name.hashCode();
        product.unit = "个";
        product.minStock = 10;
        product.cost = price * 0.7;
        product.version = 0;

        ProductDAO.insert(product);
        return ProductDAO.findByName(name);
    }

    }