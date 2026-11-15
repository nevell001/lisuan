package com.cashier.dao;

import com.cashier.model.HoldOrder;
import com.cashier.model.PageResult;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("挂单 DAO 分页与状态 CAS")
class HoldOrderDAORefactoredTest extends DatabaseTestBase {

    private static final HoldOrderDAORefactored DAO = DAOFactory.getInstance().getHoldOrderDAO();

    @Test
    @DisplayName("分页按用户/状态过滤，并给出正确总数")
    void findPageFiltersByUserAndStatus() throws Exception {
        HoldOrder minePending = insert(1, 0);
        insert(1, 1);
        HoldOrder otherPending = insert(2, 0);

        PageResult<HoldOrder> mine = DAO.findPage(1, null, 1, 10);
        assertEquals(2, mine.getTotal(), "用户 1 应有 2 张挂单");
        assertEquals(2, mine.getData().size());

        PageResult<HoldOrder> pending = DAO.findPage(null, 0, 1, 10);
        assertEquals(2, pending.getTotal(), "全部待取单应为 2 张");
        assertEquals(2, pending.getData().size());

        PageResult<HoldOrder> secondPage = DAO.findPage(1, null, 2, 1);
        assertEquals(2, secondPage.getTotal());
        assertEquals(1, secondPage.getData().size(), "每页 1 条时第 2 页应有 1 条");
        assertEquals(minePending.id, secondPage.getData().get(0).id,
            "同一时间按 id 倒序，第 2 页应是先插入的那张");

        assertEquals(1, DAO.count(2, 0));
        assertNotNull(otherPending.id);

        PageResult<HoldOrder> none = DAO.findPage(3, null, 1, 10);
        assertEquals(0, none.getTotal());
        assertEquals(0, none.getData().size());
    }

    @Test
    @DisplayName("CAS 更新只有第一次成功，第二次返回 0")
    void updateStatusIfCurrentIsCompareAndSet() throws Exception {
        HoldOrder order = insert(1, 0);

        assertEquals(1, DAO.updateStatusIfCurrent(order.id, 0, 1));
        assertEquals(1, DAO.findById(order.id).status);

        assertEquals(0, DAO.updateStatusIfCurrent(order.id, 0, 1), "状态已变为 1，不能再次从 0 更新");
        assertEquals(1, DAO.findById(order.id).status);
        assertEquals(0, DAO.updateStatusIfCurrent(99999999, 0, 1), "不存在的记录应返回 0");
    }

    private static HoldOrder insert(int userId, int status) throws Exception {
        HoldOrder order = new HoldOrder();
        order.orderNumber = HoldOrder.generateOrderNumber();
        order.userId = userId;
        order.totalAmount = new BigDecimal("20.00");
        order.finalAmount = new BigDecimal("18.00");
        order.discountAmount = new BigDecimal("2.00");
        order.itemCount = 1;
        order.itemsJson = "[{\"productId\":1,\"quantity\":2}]";
        order.notes = "";
        order.status = status;
        DAO.insert(order);
        return order;
    }
}
