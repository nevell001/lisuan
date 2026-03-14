package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.util.DatabaseTestBase;
import com.cashier.model.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReturnService 单元测试
 * 测试退货服务的核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReturnServiceTest extends DatabaseTestBase {

    private Member testMember;
    private Product testProduct1;
    private Product testProduct2;
    private Transaction testTransaction;

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
        testProduct1 = createProduct("商品1", 10.0, 100);
        testProduct2 = createProduct("商品2", 20.0, 50);

        // 创建测试交易
        testTransaction = new Transaction();
        testTransaction.transactionId = "T202603140001";
        testTransaction.timestamp = "2026-03-14 12:00:00";
        testTransaction.totalAmount = 30.0;
        testTransaction.finalAmount = 30.0;
        testTransaction.paymentMethod = "现金";
        testTransaction.operatorName = "测试操作员";
        testTransaction.operatorUsername = "test_operator";
        TransactionDAO.insert(testTransaction);

        // 注意：transaction_items表没有单独的TransactionItem模型类
        // 直接使用Product模型在Transaction.items中处理
    }

    @Test
    @Order(1)
    @DisplayName("测试创建退货订单 - 正常情况")
    void testCreateReturnOrderNormal() {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.originalTransactionId = testTransaction.transactionId;
        returnOrder.memberId = testMember.id;
        returnOrder.totalAmount = 30.0;
        returnOrder.paymentMethod = "CASH"; // 退款方式：CASH
        returnOrder.returnReason = "商品质量问题";
        returnOrder.operatorName = "测试操作员";

        ReturnOrderItem item1 = new ReturnOrderItem();
        item1.productId = testProduct1.id;
        item1.productName = testProduct1.name;
        item1.unitPrice = testProduct1.price;
        item1.returnQuantity = 2;
        item1.calculateAmount(); // 计算退货金额
        item1.reason = "瑕疵";

        ReturnOrderItem item2 = new ReturnOrderItem();
        item2.productId = testProduct2.id;
        item2.productName = testProduct2.name;
        item2.unitPrice = testProduct2.price;
        item2.calculateAmount(); // 计算退货金额
        item2.returnQuantity = 1;
        item2.returnAmount = 10.0;
        item2.reason = "不想要";

        List<ReturnOrderItem> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        // 创建退货订单
        boolean success = ReturnService.createReturnOrder(returnOrder, items);

        assertTrue(success);

        // 验证退货订单已创建
        ReturnOrder createdOrder = ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
        assertNotNull(createdOrder);
        assertEquals("PENDING", createdOrder.status);
        assertEquals(30.0, createdOrder.totalAmount, 0.01);

        // 验证退货明细已创建
        List<ReturnOrderItem> createdItems = ReturnOrderItemDAO.findByReturnOrderId(returnOrder.returnOrderId);
        assertEquals(2, createdItems.size());
    }

    @Test
    @Order(2)
    @DisplayName("测试创建退货订单 - 空明细")
    void testCreateReturnOrderEmptyItems() {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.originalTransactionId = testTransaction.transactionId;
        returnOrder.totalAmount = 0.0;
        returnOrder.paymentMethod = "CASH";
        returnOrder.operatorName = "测试操作员";

        // 空明细列表
        List<ReturnOrderItem> items = new ArrayList<>();

        // 创建退货订单
        boolean success = ReturnService.createReturnOrder(returnOrder, items);

        assertTrue(success);

        // 验证退货订单已创建
        ReturnOrder createdOrder = ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
        assertNotNull(createdOrder);
        assertEquals("PENDING", createdOrder.status);
    }

    @Test
    @Order(3)
    @DisplayName("测试审批退货订单 - 通过")
    void testApproveReturnOrderApproved() throws Exception {
        // 先创建退货订单
        ReturnOrder returnOrder = createTestReturnOrder(30.0);

        // 获取初始库存
        int initialQuantity = testProduct1.quantity;

        // 审批通过
        boolean success = ReturnService.approveReturnOrder(
            returnOrder.returnOrderId,
            "测试审批员",
            "同意退货",
            true
        );

        assertTrue(success);

        // 验证退货订单状态已更新
        ReturnOrder updatedOrder = ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
        assertEquals("APPROVED", updatedOrder.status);
        assertEquals("测试审批员", updatedOrder.approverName);
        assertNotNull(updatedOrder.approvalDate);

        // 验证库存已增加
        Product updatedProduct = ProductDAO.findById(testProduct1.id);
        assertEquals(initialQuantity + 2, updatedProduct.quantity);
    }

    @Test
    @Order(4)
    @DisplayName("测试审批退货订单 - 拒绝")
    void testApproveReturnOrderRejected() throws Exception {
        // 先创建退货订单
        ReturnOrder returnOrder = createTestReturnOrder(20.0);

        // 获取初始库存
        int initialQuantity = testProduct1.quantity;

        // 审批拒绝
        boolean success = ReturnService.approveReturnOrder(
            returnOrder.returnOrderId,
            "测试审批员",
            "不符合退货条件",
            false
        );

        assertTrue(success);

        // 验证退货订单状态已更新
        ReturnOrder updatedOrder = ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
        assertEquals("REJECTED", updatedOrder.status);
        assertEquals("测试审批员", updatedOrder.approverName);

        // 验证库存没有变化（拒绝的退货不恢复库存）
        Product updatedProduct = ProductDAO.findById(testProduct1.id);
        assertEquals(initialQuantity, updatedProduct.quantity);
    }

    @Test
    @Order(5)
    @DisplayName("测试完成退货订单 - 有会员退款")
    void testCompleteReturnOrderWithMemberRefund() throws Exception {
        // 先创建并审批退货订单
        ReturnOrder returnOrder = createAndApproveReturnOrder(50.0, "会员余额");

        // 获取初始余额
        double initialBalance = testMember.balance;

        // 完成退货订单
        boolean success = ReturnService.completeReturnOrder(returnOrder.returnOrderId);

        assertTrue(success);

        // 验证退货订单状态已更新
        ReturnOrder updatedOrder = ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
        assertEquals("COMPLETED", updatedOrder.status);
        assertNotNull(updatedOrder.completedDate);

        // 验证会员余额已增加
        Member updatedMember = MemberDAO.findByPhone(testMember.phone);
        assertEquals(initialBalance + 50.0, updatedMember.balance, 0.01);

        // 验证充值记录已创建
        List<RechargeRecord> records = RechargeRecordDAO.findAll();
        assertEquals(1, records.size());
        assertEquals(50.0, records.get(0).amount, 0.01);
    }

    @Test
    @Order(6)
    @DisplayName("测试完成退货订单 - 现金退款")
    void testCompleteReturnOrderWithCashRefund() throws Exception {
        // 先创建并审批退货订单（无会员）
        ReturnOrder returnOrder = createTestReturnOrderWithoutMember(30.0, "现金");

        // 完成退货订单
        boolean success = ReturnService.completeReturnOrder(returnOrder.returnOrderId);

        assertTrue(success);

        // 验证退货订单状态已更新
        ReturnOrder updatedOrder = ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
        assertEquals("COMPLETED", updatedOrder.status);
        assertNotNull(updatedOrder.completedDate);

        // 验证没有会员余额变化（非会员退货）
        Member updatedMember = MemberDAO.findByPhone(testMember.phone);
        assertEquals(testMember.balance, updatedMember.balance, 0.01);
    }

    @Test
    @Order(7)
    @DisplayName("测试完成退货订单 - 未审批不能完成")
    void testCompleteReturnOrderNotApproved() throws Exception {
        // 创建未审批的退货订单
        ReturnOrder returnOrder = createTestReturnOrder(30.0);

        // 尝试完成未审批的退货订单
        boolean success = ReturnService.completeReturnOrder(returnOrder.returnOrderId);

        assertFalse(success);

        // 验证退货订单状态没有变化
        ReturnOrder updatedOrder = ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
        assertEquals("PENDING", updatedOrder.status);
        assertNull(updatedOrder.completedDate);
    }

    @Test
    @Order(8)
    @DisplayName("测试获取待审批退货订单")
    void testGetPendingReturnOrders() throws Exception {
        // 创建多个退货订单
        ReturnOrder order1 = createTestReturnOrder(10.0);
        ReturnOrder order2 = createTestReturnOrder(20.0);
        ReturnOrder order3 = createAndApproveReturnOrder(30.0, "现金");

        // 获取待审批退货订单
        List<ReturnOrder> pendingOrders = ReturnService.getPendingReturnOrders();

        assertEquals(2, pendingOrders.size());
        assertTrue(pendingOrders.stream().anyMatch(o -> o.returnOrderId.equals(order1.returnOrderId)));
        assertTrue(pendingOrders.stream().anyMatch(o -> o.returnOrderId.equals(order2.returnOrderId)));
        assertFalse(pendingOrders.stream().anyMatch(o -> o.returnOrderId.equals(order3.returnOrderId)));
    }

    @Test
    @Order(9)
    @DisplayName("测试获取已批准退货订单")
    void testGetApprovedReturnOrders() throws Exception {
        // 创建多个退货订单
        ReturnOrder order1 = createTestReturnOrder(10.0);
        ReturnOrder order2 = createAndApproveReturnOrder(20.0, "现金");
        ReturnOrder order3 = createAndApproveReturnOrder(30.0, "会员余额");

        // 获取已批准退货订单
        List<ReturnOrder> approvedOrders = ReturnService.getApprovedReturnOrders();

        assertEquals(2, approvedOrders.size());
        assertTrue(approvedOrders.stream().anyMatch(o -> o.returnOrderId.equals(order2.returnOrderId)));
        assertTrue(approvedOrders.stream().anyMatch(o -> o.returnOrderId.equals(order3.returnOrderId)));
        assertFalse(approvedOrders.stream().anyMatch(o -> o.returnOrderId.equals(order1.returnOrderId)));
    }

    @Test
    @Order(10)
    @DisplayName("测试获取已完成退货订单")
    void testGetCompletedReturnOrders() throws Exception {
        // 创建并完成多个退货订单
        ReturnOrder order1 = createAndApproveReturnOrder(10.0, "现金");
        ReturnOrder order2 = createAndApproveReturnOrder(20.0, "会员余额");

        // 完成第二个退货订单
        ReturnService.completeReturnOrder(order2.returnOrderId);

        // 获取已完成退货订单
        List<ReturnOrder> completedOrders = ReturnService.getCompletedReturnOrders();

        assertEquals(1, completedOrders.size());
        assertTrue(completedOrders.stream().anyMatch(o -> o.returnOrderId.equals(order2.returnOrderId)));
        assertFalse(completedOrders.stream().anyMatch(o -> o.returnOrderId.equals(order1.returnOrderId)));
    }

    @Test
    @Order(11)
    @DisplayName("测试获取会员退货订单")
    void testGetMemberReturnOrders() throws Exception {
        // 创建多个退货订单（一个有会员，一个无会员）
        ReturnOrder order1 = createAndApproveReturnOrder(10.0, "会员余额");
        ReturnOrder order2 = createTestReturnOrderWithoutMember(20.0, "现金");

        // 获取会员退货订单
        List<ReturnOrder> memberOrders = ReturnService.getMemberReturnOrders(testMember.id);

        assertEquals(1, memberOrders.size());
        assertTrue(memberOrders.stream().anyMatch(o -> o.returnOrderId.equals(order1.returnOrderId)));
        assertFalse(memberOrders.stream().anyMatch(o -> o.returnOrderId.equals(order2.returnOrderId)));
    }

    @Test
    @Order(12)
    @DisplayName("测试计算退货统计")
    void testCalculateReturnStatistics() throws Exception {
        // 创建多个退货订单并设置不同状态
        ReturnOrder order1 = createTestReturnOrder(10.0); // PENDING
        ReturnOrder order2 = createAndApproveReturnOrder(20.0, "现金"); // APPROVED
        ReturnOrder order3 = createAndApproveReturnOrder(30.0, "会员余额");
        ReturnService.completeReturnOrder(order3.returnOrderId); // COMPLETED

        // 创建另一个退货订单并拒绝
        ReturnOrder order4 = createTestReturnOrder(15.0);
        ReturnService.approveReturnOrder(order4.returnOrderId, "测试审批员", "不符合", false); // REJECTED

        // 计算统计
        Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Date endDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        ReturnService.ReturnStatistics stats = ReturnService.calculateReturnStatistics(startDate, endDate);

        // 验证统计结果
        assertNotNull(stats);
        assertEquals(4, stats.totalReturnOrders); // 总共4个退货单
        assertEquals(75.0, stats.totalReturnAmount, 0.01); // 10+20+30+15
        assertEquals(2, stats.approvedOrders); // 2个已批准
        assertEquals(1, stats.rejectedOrders); // 1个已拒绝
        assertEquals(1, stats.completedOrders); // 1个已完成
    }

    /**
     * 辅助方法：创建测试退货订单
     */
    private ReturnOrder createTestReturnOrder(double amount) throws Exception {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.originalTransactionId = testTransaction.transactionId;
        returnOrder.memberId = testMember.id;
        returnOrder.totalAmount = amount;
        returnOrder.paymentMethod = "CASH";
        returnOrder.returnReason = "测试退货";
        returnOrder.operatorName = "测试操作员";

        ReturnOrderItem item = new ReturnOrderItem();
        item.productId = testProduct1.id;
        item.productName = testProduct1.name;
        item.unitPrice = testProduct1.price;
        item.returnQuantity = (int) (amount / testProduct1.price);
        item.returnAmount = amount;

        List<ReturnOrderItem> items = new ArrayList<>();
        items.add(item);

        ReturnService.createReturnOrder(returnOrder, items);
        return ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
    }

    /**
     * 辅助方法：创建测试退货订单（无会员）
     */
    private ReturnOrder createTestReturnOrderWithoutMember(double amount, String paymentMethod) throws Exception {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.originalTransactionId = testTransaction.transactionId;
        returnOrder.memberId = 0; // 无会员
        returnOrder.totalAmount = amount;
        returnOrder.paymentMethod = paymentMethod;
        returnOrder.returnReason = "测试退货";
        returnOrder.operatorName = "测试操作员";

        ReturnOrderItem item = new ReturnOrderItem();
        item.productId = testProduct2.id;
        item.productName = testProduct2.name;
        item.unitPrice = testProduct2.price;
        item.returnQuantity = (int) (amount / testProduct2.price);
        item.returnAmount = amount;

        List<ReturnOrderItem> items = new ArrayList<>();
        items.add(item);

        ReturnService.createReturnOrder(returnOrder, items);
        return ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
    }

    /**
     * 辅助方法：创建并审批退货订单
     */
    private ReturnOrder createAndApproveReturnOrder(double amount, String paymentMethod) throws Exception {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.originalTransactionId = testTransaction.transactionId;
        returnOrder.memberId = testMember.id;
        returnOrder.totalAmount = amount;
        returnOrder.paymentMethod = paymentMethod;
        returnOrder.returnReason = "测试退货";
        returnOrder.operatorName = "测试操作员";

        ReturnOrderItem item = new ReturnOrderItem();
        item.productId = testProduct2.id;
        item.productName = testProduct2.name;
        item.unitPrice = testProduct2.price;
        item.returnQuantity = (int) (amount / testProduct2.price);
        item.returnAmount = amount;

        List<ReturnOrderItem> items = new ArrayList<>();
        items.add(item);

        ReturnService.createReturnOrder(returnOrder, items);
        ReturnService.approveReturnOrder(returnOrder.returnOrderId, "测试审批员", "同意", true);
        return ReturnOrderDAO.findByReturnOrderId(returnOrder.returnOrderId);
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
