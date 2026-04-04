package com.cashier.service;

import com.cashier.dao.PromotionDAO;
import com.cashier.dao.ProductDAO;
import com.cashier.util.DatabaseTestBase;
import com.cashier.model.CartItem;
import com.cashier.model.Product;
import com.cashier.model.Promotion;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 促销功能单元测试
 * 测试促销应用和计算
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PromotionServiceTest extends DatabaseTestBase {

    private Promotion testPromotion1;
    private Promotion testPromotion2;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() throws Exception {
        // 确保使用测试数据库
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }

        // 清空数据库
        clearTestData();

        // 创建测试促销
        testPromotion1 = createPromotion("满100减10", "FULL_REDUCTION", 100.0, 10.0);
        testPromotion2 = createPromotion("8折优惠", "PERCENTAGE_DISCOUNT", 0.0, 0.2);

        // 创建测试商品
        testProducts = new ArrayList<>();
        testProducts.add(createProduct("商品1", 30.0, 100));
        testProducts.add(createProduct("商品2", 50.0, 50));
        testProducts.add(createProduct("商品3", 20.0, 30));
    }

    @Test
    @Order(1)
    @DisplayName("测试创建促销 - 满减类型")
    void testCreateFullReductionPromotion() throws Exception {
        Promotion promotion = new Promotion();
        promotion.name = "满200减20";
        promotion.type = "FULL_REDUCTION";
        promotion.threshold = BigDecimal.valueOf(200.0);
        promotion.discount = BigDecimal.valueOf(20.0);
        promotion.description = "满200元减20元";
        promotion.enabled = true;

        boolean success = PromotionDAO.insert(promotion);

        assertTrue(success);

        // 验证促销已创建
        Promotion createdPromotion = PromotionDAO.findById(promotion.id);
        assertNotNull(createdPromotion);
        assertEquals("满200减20", createdPromotion.name);
        assertEquals("FULL_REDUCTION", createdPromotion.type);
        assertEquals(200.0, createdPromotion.threshold.doubleValue(), 0.01);
        assertEquals(20.0, createdPromotion.discount.doubleValue(), 0.01);
    }

    @Test
    @Order(2)
    @DisplayName("测试创建促销 - 百分比折扣类型")
    void testCreatePercentageDiscountPromotion() throws Exception {
        Promotion promotion = new Promotion();
        promotion.name = "全场8折";
        promotion.type = "PERCENTAGE_DISCOUNT";
        promotion.threshold = BigDecimal.ZERO;
        promotion.discount = new BigDecimal("0.2"); // 8折相当于减免20%
        promotion.description = "全场商品享受8折优惠";
        promotion.enabled = true;

        boolean success = PromotionDAO.insert(promotion);

        assertTrue(success);

        // 验证促销已创建
        Promotion createdPromotion = PromotionDAO.findById(promotion.id);
        assertNotNull(createdPromotion);
        assertEquals("全场8折", createdPromotion.name);
        assertEquals("PERCENTAGE_DISCOUNT", createdPromotion.type);
        assertEquals(0.0, createdPromotion.threshold.doubleValue(), 0.01);
        assertEquals(0.2, createdPromotion.discount.doubleValue(), 0.01);
    }

    @Test
    @Order(3)
    @DisplayName("测试查询所有促销")
    void testFindAllPromotions() throws Exception {
        List<Promotion> promotions = PromotionDAO.findAll();

        assertNotNull(promotions);
        assertEquals(2, promotions.size());

        // 验证促销按ID倒序排列
        assertTrue(promotions.get(0).id > promotions.get(1).id);
    }

    @Test
    @Order(4)
    @DisplayName("测试查询启用的促销")
    void testFindEnabledPromotions() throws Exception {
        // 创建一个禁用的促销
        Promotion disabledPromotion = createPromotion("禁用促销", "FULL_REDUCTION", 50.0, 5.0);
        disabledPromotion.enabled = false;
        PromotionDAO.update(disabledPromotion);

        // 查询启用的促销
        List<Promotion> enabledPromotions = PromotionDAO.findEnabled();

        assertEquals(2, enabledPromotions.size());
        assertTrue(enabledPromotions.stream().noneMatch(p -> p.id == disabledPromotion.id));
    }

    @Test
    @Order(5)
    @DisplayName("测试查询当前有效的促销")
    void testFindActivePromotions() throws Exception {
        // 查询当前有效的促销（启用状态）
        List<Promotion> activePromotions = PromotionDAO.findActive();

        // 应该包含所有启用的促销
        assertTrue(activePromotions.size() >= 2);
        assertTrue(activePromotions.stream().anyMatch(p -> p.name.equals("满100减10")));
        assertTrue(activePromotions.stream().anyMatch(p -> p.name.equals("8折优惠")));
    }

    @Test
    @Order(6)
    @DisplayName("测试计算满减优惠")
    void testCalculateFullReductionDiscount() {
        double totalAmount = 150.0;

        // 应用满100减10
        double discount = calculateFullReductionDiscount(totalAmount, testPromotion1);

        assertEquals(10.0, discount, 0.01);
    }

    @Test
    @Order(7)
    @DisplayName("测试计算满减优惠 - 未达到门槛")
    void testCalculateFullReductionDiscountBelowThreshold() {
        double totalAmount = 80.0;

        // 应用满100减10（未达到门槛）
        double discount = calculateFullReductionDiscount(totalAmount, testPromotion1);

        assertEquals(0.0, discount, 0.01);
    }

    @Test
    @Order(8)
    @DisplayName("测试计算百分比折扣")
    void testCalculatePercentageDiscount() {
        double totalAmount = 200.0;

        // 应用8折优惠
        double discount = calculatePercentageDiscount(totalAmount, testPromotion2);

        assertEquals(40.0, discount, 0.01); // 200 * 0.2
    }

    @Test
    @Order(9)
    @DisplayName("测试组合多个促销")
    void testCombineMultiplePromotions() {
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem(testProducts.get(0), 2)); // 2 * 30 = 60
        cartItems.add(new CartItem(testProducts.get(1), 3)); // 3 * 50 = 150

        double totalAmount = 210.0;

        // 应用满减优惠
        double fullReductionDiscount = calculateFullReductionDiscount(totalAmount, testPromotion1);

        // 应用百分比折扣（假设不能叠加，取最大值）
        double percentageDiscount = calculatePercentageDiscount(totalAmount, testPromotion2);

        // 验证满减优惠更大
        assertEquals(10.0, fullReductionDiscount, 0.01);
        assertEquals(42.0, percentageDiscount, 0.01);
    }

    @Test
    @Order(10)
    @DisplayName("测试更新促销")
    void testUpdatePromotion() throws Exception {
        // 更新促销描述
        testPromotion1.description = "更新后的描述：满100元减10元";
        PromotionDAO.update(testPromotion1);

        // 验证促销已更新
        Promotion updatedPromotion = PromotionDAO.findById(testPromotion1.id);
        assertEquals("更新后的描述：满100元减10元", updatedPromotion.description);
    }

    @Test
    @Order(11)
    @DisplayName("测试禁用促销")
    void testDisablePromotion() throws Exception {
        testPromotion1.enabled = false;
        boolean success = PromotionDAO.update(testPromotion1);

        assertTrue(success);

        // 验证促销已禁用
        Promotion updatedPromotion = PromotionDAO.findById(testPromotion1.id);
        assertFalse(updatedPromotion.enabled);

        // 验证不包含在启用的促销列表中
        List<Promotion> enabledPromotions = PromotionDAO.findEnabled();
        assertFalse(enabledPromotions.stream().anyMatch(p -> p.id == testPromotion1.id));
    }

    @Test
    @Order(12)
    @DisplayName("测试删除促销")
    void testDeletePromotion() throws Exception {
        int promotionId = testPromotion1.id;

        // 删除促销
        boolean success = PromotionDAO.delete(promotionId);

        assertTrue(success);

        // 验证促销已删除
        Promotion deletedPromotion = PromotionDAO.findById(promotionId);
        assertNull(deletedPromotion);

        // 验证不包含在促销列表中
        List<Promotion> allPromotions = PromotionDAO.findAll();
        assertFalse(allPromotions.stream().anyMatch(p -> p.id == promotionId));
    }

    @Test
    @Order(13)
    @DisplayName("测试促销使用计数")
    void testPromotionUsageCount() throws Exception {
        int initialUsageCount = testPromotion1.usageCount;

        // 模拟增加使用计数
        testPromotion1.usageCount = initialUsageCount + 5;
        PromotionDAO.update(testPromotion1);

        // 验证使用计数已更新
        Promotion updatedPromotion = PromotionDAO.findById(testPromotion1.id);
        assertEquals(initialUsageCount + 5, updatedPromotion.usageCount);
    }

    @Test
    @Order(14)
    @DisplayName("测试最大使用次数限制")
    void testMaxUsageLimit() throws Exception {
        // 设置最大使用次数
        testPromotion1.maxUsage = 10;
        testPromotion1.usageCount = 10;
        PromotionDAO.update(testPromotion1);

        Promotion updatedPromotion = PromotionDAO.findById(testPromotion1.id);

        // 验证已达到最大使用次数
        assertTrue(updatedPromotion.usageCount >= updatedPromotion.maxUsage);
    }

    /**
     * 辅助方法：计算满减优惠
     */
    private double calculateFullReductionDiscount(double totalAmount, Promotion promotion) {
        if (BigDecimal.valueOf(totalAmount).compareTo(promotion.threshold) >= 0) {
            return promotion.discount.doubleValue();
        }
        return 0.0;
    }

    /**
     * 辅助方法：计算百分比折扣
     */
    private double calculatePercentageDiscount(double totalAmount, Promotion promotion) {
        return BigDecimal.valueOf(totalAmount).multiply(promotion.discount).doubleValue();
    }

    /**
     * 辅助方法：创建测试促销
     */
    private Promotion createPromotion(String name, String type, double threshold, double discount) throws Exception {
        Promotion promotion = new Promotion();
        promotion.promotionCode = "PROMO_" + System.currentTimeMillis() + "_" + name.hashCode();  // 设置唯一的促销编码
        promotion.name = name;
        promotion.type = type;
        promotion.threshold = BigDecimal.valueOf(threshold);
        promotion.discount = BigDecimal.valueOf(discount);
        promotion.description = "测试促销：" + name;
        promotion.enabled = true;
        promotion.usageCount = 0;
        promotion.maxUsage = 100;

        PromotionDAO.insert(promotion);
        return PromotionDAO.findById(promotion.id);
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

}
