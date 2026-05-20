package com.cashier.dao;

import com.cashier.model.Promotion;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromotionDAOTest extends DatabaseTestBase {

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    private Promotion createTestPromotion(String name, String type, double threshold, double discount) {
        Promotion p = new Promotion();
        p.promotionCode = "TEST_" + System.nanoTime();
        p.name = name;
        p.type = type;
        p.threshold = BigDecimal.valueOf(threshold);
        p.discount = BigDecimal.valueOf(discount);
        p.description = "测试促销";
        p.enabled = true;
        p.startDate = new java.util.Date(System.currentTimeMillis() - 86400000);
        p.endDate = new java.util.Date(System.currentTimeMillis() + 86400000);
        p.usageCount = 0;
        p.maxUsage = -1;
        return p;
    }

    @Test
    void testInsert() throws SQLException {
        Promotion p = createTestPromotion("满100减20", "满减", 100, 20);

        boolean result = PromotionDAO.insert(p);
        assertTrue(result);
        assertTrue(p.id > 0);
    }

    @Test
    void testFindById() throws SQLException {
        Promotion p = createTestPromotion("满200减30", "满减", 200, 30);
        PromotionDAO.insert(p);

        Promotion found = PromotionDAO.findById(p.id);
        assertNotNull(found);
        assertEquals("满200减30", found.name);
        assertEquals("满减", found.type);
        assertEquals(0, BigDecimal.valueOf(200).compareTo(found.threshold));
        assertEquals(0, BigDecimal.valueOf(30).compareTo(found.discount));
    }

    @Test
    void testFindByIdNotFound() throws SQLException {
        Promotion found = PromotionDAO.findById(99999);
        assertNull(found);
    }

    @Test
    void testFindAll() throws SQLException {
        PromotionDAO.insert(createTestPromotion("促销A", "满减", 100, 10));
        PromotionDAO.insert(createTestPromotion("促销B", "打折", 200, 0.9));

        List<Promotion> all = PromotionDAO.findAll();
        assertTrue(all.size() >= 2);
    }

    @Test
    void testFindEnabled() throws SQLException {
        Promotion enabled = createTestPromotion("启用促销", "满减", 100, 10);
        enabled.enabled = true;
        PromotionDAO.insert(enabled);

        Promotion disabled = createTestPromotion("禁用促销", "满减", 100, 10);
        disabled.enabled = false;
        PromotionDAO.insert(disabled);

        List<Promotion> enabledList = PromotionDAO.findEnabled();
        assertTrue(enabledList.stream().allMatch(p -> p.enabled));
    }

    @Test
    void testFindActive() throws SQLException {
        Promotion active = createTestPromotion("进行中促销", "满减", 100, 10);
        active.startDate = new java.util.Date(System.currentTimeMillis() - 86400000);
        active.endDate = new java.util.Date(System.currentTimeMillis() + 86400000);
        PromotionDAO.insert(active);

        Promotion expired = createTestPromotion("已过期促销", "满减", 100, 10);
        expired.startDate = new java.util.Date(System.currentTimeMillis() - 172800000);
        expired.endDate = new java.util.Date(System.currentTimeMillis() - 86400000);
        PromotionDAO.insert(expired);

        List<Promotion> activeList = PromotionDAO.findActive();
        assertTrue(activeList.size() >= 1);
        assertTrue(activeList.stream().anyMatch(p -> "进行中促销".equals(p.name)));
    }

    @Test
    void testUpdate() throws SQLException {
        Promotion p = createTestPromotion("原名称", "满减", 100, 10);
        PromotionDAO.insert(p);

        p.name = "新名称";
        p.threshold = BigDecimal.valueOf(200);
        boolean updated = PromotionDAO.update(p);
        assertTrue(updated);

        Promotion found = PromotionDAO.findById(p.id);
        assertEquals("新名称", found.name);
        assertEquals(0, BigDecimal.valueOf(200).compareTo(found.threshold));
    }

    @Test
    void testDelete() throws SQLException {
        Promotion p = createTestPromotion("待删除促销", "满减", 100, 10);
        PromotionDAO.insert(p);

        boolean deleted = PromotionDAO.delete(p.id);
        assertTrue(deleted);

        assertNull(PromotionDAO.findById(p.id));
    }

    @Test
    void testIncrementUsage() throws SQLException {
        Promotion p = createTestPromotion("使用次数测试", "满减", 100, 10);
        PromotionDAO.insert(p);

        PromotionDAO.incrementUsage(p.id);

        Promotion found = PromotionDAO.findById(p.id);
        assertEquals(1, found.usageCount);
    }

    @Test
    void testBatchInsert() throws SQLException {
        List<Promotion> promotions = List.of(
            createTestPromotion("批量1", "满减", 100, 10),
            createTestPromotion("批量2", "打折", 200, 0.8),
            createTestPromotion("批量3", "优惠券", 0, 50)
        );

        PromotionDAO.batchInsert(promotions);

        List<Promotion> all = PromotionDAO.findAll();
        assertTrue(all.size() >= 3);
    }

    @Test
    void testPromotionCalculateDiscount() {
        Promotion p = createTestPromotion("满100减20", "满减", 100, 20);
        p.enabled = true;
        p.startDate = new java.util.Date(System.currentTimeMillis() - 86400000);
        p.endDate = new java.util.Date(System.currentTimeMillis() + 86400000);

        // 未达到门槛
        BigDecimal discount1 = p.calculateDiscount(BigDecimal.valueOf(50));
        assertEquals(0, BigDecimal.ZERO.compareTo(discount1));

        // 达到门槛
        BigDecimal discount2 = p.calculateDiscount(BigDecimal.valueOf(100));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(discount2));
    }

    @Test
    void testPromotionMaxUsage() {
        Promotion p = createTestPromotion("限量促销", "满减", 100, 10);
        p.enabled = true;
        p.usageCount = 5;
        p.maxUsage = 5;
        p.startDate = new java.util.Date(System.currentTimeMillis() - 86400000);
        p.endDate = new java.util.Date(System.currentTimeMillis() + 86400000);

        BigDecimal discount = p.calculateDiscount(BigDecimal.valueOf(200));
        assertEquals(0, BigDecimal.ZERO.compareTo(discount));
    }
}
