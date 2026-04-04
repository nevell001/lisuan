package com.cashier.service;

import com.cashier.dao.MemberDAO;
import com.cashier.dao.RechargeRecordDAO;
import com.cashier.util.DatabaseTestBase;
import com.cashier.model.Member;
import com.cashier.model.RechargeRecord;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemberService 单元测试
 * 测试会员服务的核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberServiceTest extends DatabaseTestBase {

    private Member testMember;

    @BeforeEach
    void setUp() throws Exception {
        // 确保使用测试数据库
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }

        // 清空数据库
        clearTestData();

        // 创建测试会员
        testMember = createMember("13800138000", "测试会员", 1000.0, 100.0);
    }

    @Test
    @Order(1)
    @DisplayName("测试查找会员 - 存在")
    void testFindMemberByPhoneExists() {
        Member member = MemberService.findMemberByPhone("13800138000");

        assertNotNull(member);
        assertEquals("13800138000", member.phone);
        assertEquals("测试会员", member.name);
    }

    @Test
    @Order(2)
    @DisplayName("测试查找会员 - 不存在")
    void testFindMemberByPhoneNotExists() {
        Member member = MemberService.findMemberByPhone("13999999999");

        assertNull(member);
    }

    @Test
    @Order(3)
    @DisplayName("测试会员充值 - 现金")
    void testRechargeWithCash() throws Exception {
        BigDecimal initialBalance = testMember.balance;
        BigDecimal initialPoints = testMember.points;
        double rechargeAmount = 500.0;

        // 执行充值
        boolean success = MemberService.recharge(testMember, rechargeAmount, "现金", "测试操作员");

        assertTrue(success);

        // 验证余额、积分和等级都已更新
        Member updatedMember = MemberDAO.findByPhone("13800138000");
        assertAmountEquals(initialBalance.add(BigDecimal.valueOf(rechargeAmount)), updatedMember.balance);
        assertAmountEquals(initialPoints.add(BigDecimal.valueOf(rechargeAmount).multiply(BigDecimal.TEN)), updatedMember.points);
        assertEquals("金卡", updatedMember.level);
        assertAmountEquals(9.0, updatedMember.discount);

        // 验证充值记录已创建
        List<RechargeRecord> records = RechargeRecordDAO.findAll();
        assertEquals(1, records.size());
        assertAmountEquals(rechargeAmount, records.get(0).amount);
        assertEquals("现金", records.get(0).paymentMethod);
        assertEquals("测试操作员", records.get(0).operator);
        assertFalse(records.get(0).recordId == null || records.get(0).recordId.isBlank());
    }

    @Test
    @Order(4)
    @DisplayName("测试会员充值 - 支付宝")
    void testRechargeWithAlipay() throws Exception {
        BigDecimal initialBalance = testMember.balance;
        double rechargeAmount = 300.0;

        // 执行充值
        boolean success = MemberService.recharge(testMember, rechargeAmount, "支付宝", "测试操作员");

        assertTrue(success);

        // 验证余额已增加
        Member updatedMember = MemberDAO.findByPhone("13800138000");
        assertAmountEquals(initialBalance.add(BigDecimal.valueOf(rechargeAmount)), updatedMember.balance);

        // 验证充值记录已创建
        List<RechargeRecord> records = RechargeRecordDAO.findAll();
        assertEquals(1, records.size());
        assertEquals("支付宝", records.get(0).paymentMethod);
    }

    @Test
    @Order(5)
    @DisplayName("测试会员等级计算 - 普通会员")
    void testCalculateLevelRegular() {
        double points = 500.0;
        String level = MemberService.calculateLevel(points);

        assertEquals("普通", level);
    }

    @Test
    @Order(6)
    @DisplayName("测试会员等级计算 - 银卡会员")
    void testCalculateLevelSilver() {
        double points = 2000.0;
        String level = MemberService.calculateLevel(points);

        assertEquals("银卡", level);
    }

    @Test
    @Order(7)
    @DisplayName("测试会员等级计算 - 金卡会员")
    void testCalculateLevelGold() {
        double points = 6000.0;
        String level = MemberService.calculateLevel(points);

        assertEquals("金卡", level);
    }

    @Test
    @Order(8)
    @DisplayName("测试会员等级计算 - 钻石会员")
    void testCalculateLevelDiamond() {
        double points = 12000.0;
        String level = MemberService.calculateLevel(points);

        assertEquals("钻石", level);
    }

    @Test
    @Order(9)
    @DisplayName("测试会员等级计算 - 边界值银卡")
    void testCalculateLevelSilverBoundary() {
        // 刚好达到银卡门槛
        String level1 = MemberService.calculateLevel(1000.0);
        assertEquals("银卡", level1);

        // 差一点达到银卡门槛
        String level2 = MemberService.calculateLevel(999.0);
        assertEquals("普通", level2);
    }

    @Test
    @Order(10)
    @DisplayName("测试会员等级更新 - 升级到银卡")
    void testUpdateMemberLevelUpgradeToSilver() throws Exception {
        // 设置积分达到银卡门槛
        testMember.points = BigDecimal.valueOf(2000.0);
        MemberDAO.update(testMember);

        // 更新等级
        boolean success = MemberService.updateMemberLevel(testMember);

        assertTrue(success);

        // 验证等级已更新
        Member updatedMember = MemberDAO.findByPhone("13800138000");
        assertEquals("银卡", updatedMember.level);
        assertAmountEquals(9.5, updatedMember.discount);
    }

    @Test
    @Order(11)
    @DisplayName("测试会员等级更新 - 升级到金卡")
    void testUpdateMemberLevelUpgradeToGold() throws Exception {
        // 设置积分达到金卡门槛
        testMember.points = BigDecimal.valueOf(6000.0);
        MemberDAO.update(testMember);

        // 更新等级
        boolean success = MemberService.updateMemberLevel(testMember);

        assertTrue(success);

        // 验证等级已更新
        Member updatedMember = MemberDAO.findByPhone("13800138000");
        assertEquals("金卡", updatedMember.level);
        assertAmountEquals(9.0, updatedMember.discount);
    }

    @Test
    @Order(12)
    @DisplayName("测试会员等级更新 - 升级到钻石")
    void testUpdateMemberLevelUpgradeToDiamond() throws Exception {
        // 设置积分达到钻石门槛
        testMember.points = BigDecimal.valueOf(11000.0);
        MemberDAO.update(testMember);

        // 更新等级
        boolean success = MemberService.updateMemberLevel(testMember);

        assertTrue(success);

        // 验证等级已更新
        Member updatedMember = MemberDAO.findByPhone("13800138000");
        assertEquals("钻石", updatedMember.level);
        assertAmountEquals(8.5, updatedMember.discount);
    }

    @Test
    @Order(13)
    @DisplayName("测试会员等级更新 - 无需升级")
    void testUpdateMemberLevelNoUpgrade() throws Exception {
        // 积分不够升级
        testMember.points = BigDecimal.valueOf(500.0);
        MemberDAO.update(testMember);

        // 更新等级
        boolean success = MemberService.updateMemberLevel(testMember);

        assertTrue(success);

        // 验证等级没有变化
        Member updatedMember = MemberDAO.findByPhone("13800138000");
        assertEquals("普通", updatedMember.level);
        assertAmountEquals(10.0, updatedMember.discount);
    }

    @Test
    @Order(14)
    @DisplayName("测试根据等级获取折扣")
    void testGetDiscountByLevel() {
        // 普通会员
        assertEquals(10.0, MemberService.getDiscountByLevel("普通"), 0.01);

        // 银卡会员
        assertEquals(9.5, MemberService.getDiscountByLevel("银卡"), 0.01);

        // 金卡会员
        assertEquals(9.0, MemberService.getDiscountByLevel("金卡"), 0.01);

        // 钻石会员
        assertEquals(8.5, MemberService.getDiscountByLevel("钻石"), 0.01);

        // 未知等级返回默认折扣
        assertEquals(10.0, MemberService.getDiscountByLevel("未知"), 0.01);
    }

    @Test
    @Order(15)
    @DisplayName("测试检查余额是否充足")
    void testCheckBalanceSufficient() throws Exception {
        // 充足余额
        assertTrue(MemberService.checkBalanceSufficient(testMember, 500.0));

        // 不足余额
        assertFalse(MemberService.checkBalanceSufficient(testMember, 1500.0));

        // 刚好足够
        assertTrue(MemberService.checkBalanceSufficient(testMember, 1000.0));
    }

    @Test
    @Order(16)
    @DisplayName("测试计算折扣后金额 - 无会员")
    void testCalculateDiscountedAmountWithoutMember() {
        double originalAmount = 100.0;
        double discountedAmount = MemberService.calculateDiscountedAmount(originalAmount, null);

        assertEquals(originalAmount, discountedAmount, 0.01);
    }

    @Test
    @Order(17)
    @DisplayName("测试计算折扣后金额 - 有会员")
    void testCalculateDiscountedAmountWithMember() throws Exception {
        double originalAmount = 100.0;

        // 银卡会员（9.5折）
        testMember.level = "银卡";
        testMember.discount = BigDecimal.valueOf(9.5);
        MemberDAO.update(testMember);

        double discountedAmount = MemberService.calculateDiscountedAmount(originalAmount, testMember);
        assertEquals(95.0, discountedAmount, 0.01); // 100 * 0.95

        // 金卡会员（9折）
        testMember.level = "金卡";
        testMember.discount = BigDecimal.valueOf(9.0);
        MemberDAO.update(testMember);

        discountedAmount = MemberService.calculateDiscountedAmount(originalAmount, testMember);
        assertEquals(90.0, discountedAmount, 0.01); // 100 * 0.9

        // 钻石会员（8.5折）
        testMember.level = "钻石";
        testMember.discount = BigDecimal.valueOf(8.5);
        MemberDAO.update(testMember);

        discountedAmount = MemberService.calculateDiscountedAmount(originalAmount, testMember);
        assertEquals(85.0, discountedAmount, 0.01); // 100 * 0.85
    }

    @Test
    @Order(18)
    @DisplayName("测试获取会员统计信息")
    void testGetMemberStatistics() throws Exception {
        // 创建多个会员
        Member member1 = createMember("13900000001", "会员1", 500.0, 100.0);
        Member member2 = createMember("13900000002", "会员2", 1500.0, 200.0);
        Member member3 = createMember("13900000003", "会员3", 3000.0, 600.0);

        // 获取统计信息
        Map<String, Object> stats = MemberService.getMemberStatistics();

        // 验证统计信息
        assertNotNull(stats);
        assertEquals(4, stats.get("totalCount")); // 包括测试会员
        assertTrue((Double) stats.get("totalBalance") > 0);
        assertTrue((Double) stats.get("totalPoints") > 0);

        // 验证等级统计
        @SuppressWarnings("unchecked")
        Map<String, Integer> levelStats = (Map<String, Integer>) stats.get("levelStats");
        assertNotNull(levelStats);
        assertTrue(levelStats.containsKey("普通")); // 所有会员都是普通等级
    }

    @Test
    @Order(19)
    @DisplayName("测试获取等级配置")
    void testGetLevelConfig() {
        Map<String, Object> config = MemberService.getLevelConfig();

        assertNotNull(config);
        assertTrue(config.containsKey("levelDiscounts"));
        assertTrue(config.containsKey("levelPoints"));

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> levelDiscounts = (Map<String, BigDecimal>) config.get("levelDiscounts");
        assertAmountEquals(10.0, levelDiscounts.get("普通"));
        assertAmountEquals(9.5, levelDiscounts.get("银卡"));
        assertAmountEquals(9.0, levelDiscounts.get("金卡"));
        assertAmountEquals(8.5, levelDiscounts.get("钻石"));

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> levelPoints = (Map<String, BigDecimal>) config.get("levelPoints");
        assertAmountEquals(1000.0, levelPoints.get("银卡"));
        assertAmountEquals(5000.0, levelPoints.get("金卡"));
        assertAmountEquals(10000.0, levelPoints.get("钻石"));
    }

    @Test
    @Order(20)
    @DisplayName("测试会员充值失败时回滚余额更新")
    void testRechargeRollbackWhenRecordInsertFails() throws Exception {
        BigDecimal initialBalance = testMember.balance;
        String tooLongOperator = "操".repeat(60);

        boolean success = MemberService.recharge(testMember, 200.0, "现金", tooLongOperator);

        assertFalse(success);

        Member updatedMember = MemberDAO.findByPhone("13800138000");
        assertAmountEquals(initialBalance, updatedMember.balance);
        assertTrue(RechargeRecordDAO.findAll().isEmpty());
    }

    @Test
    @Order(21)
    @DisplayName("测试会员连续充值会生成唯一记录ID")
    void testRechargeGeneratesUniqueRecordIds() throws Exception {
        assertTrue(MemberService.recharge(testMember, 10.0, "现金", "操作员A"));
        assertTrue(MemberService.recharge(testMember, 20.0, "微信", "操作员B"));

        List<RechargeRecord> records = RechargeRecordDAO.findAll();
        assertEquals(2, records.size());
        assertFalse(records.get(0).recordId == null || records.get(0).recordId.isBlank());
        assertFalse(records.get(1).recordId == null || records.get(1).recordId.isBlank());
        assertNotEquals(records.get(0).recordId, records.get(1).recordId);
    }

    /**
     * 辅助方法：创建测试会员
     */
    private Member createMember(String phone, String name, double balance, double points) throws Exception {
        Member member = new Member();
        member.phone = phone;
        member.name = name;
        member.balance = BigDecimal.valueOf(balance);
        member.points = BigDecimal.valueOf(points);
        member.level = "普通";
        member.discount = BigDecimal.TEN;

        MemberDAO.insert(member);
        return MemberDAO.findByPhone(phone);
    }

    private void assertAmountEquals(double expected, BigDecimal actual) {
        assertAmountEquals(BigDecimal.valueOf(expected), actual);
    }

    private void assertAmountEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }

}
