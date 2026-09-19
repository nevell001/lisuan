package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.MemberDAORefactored;
import com.cashier.model.Member;
import com.cashier.model.User;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberApiControllerTest extends DatabaseTestBase {

    private final MemberDAORefactored memberDAO = DAOFactory.getInstance().getMemberDAO();

    private Member insertMember(String phone) throws Exception {
        Member member = new Member();
        member.phone = phone;
        member.name = "测试会员";
        member.level = "普通";
        member.discount = BigDecimal.TEN;
        member.discountRate = BigDecimal.TEN;
        member.balance = BigDecimal.ZERO;
        member.points = BigDecimal.ZERO;
        assertTrue(memberDAO.insert(member));
        return member;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("会员列表返回分页数据")
    void listMembers() throws Exception {
        insertMember("13800000001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/members");
        MemberApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("按手机号查询会员")
    void getMemberByPhone() throws Exception {
        insertMember("13800000002");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/members/phone")
            .withPathParam("phone", "13800000002");
        MemberApiController.getByPhone(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("创建会员返回 201")
    void createMemberReturnsCreated() {
        MemberApiController.MemberRequest request = new MemberApiController.MemberRequest();
        request.phone = "13800000003";
        request.name = "新会员";

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/members").withBody(request);
        MemberApiController.create(ctx.context);

        assertEquals(HttpStatus.CREATED, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("重复手机号创建会员返回 400")
    void createDuplicatePhoneRejected() throws Exception {
        insertMember("13800000004");
        MemberApiController.MemberRequest request = new MemberApiController.MemberRequest();
        request.phone = "13800000004";
        request.name = "重复会员";

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/members").withBody(request);
        MemberApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("更新会员资料")
    void updateMemberAppliesChanges() throws Exception {
        Member saved = insertMember("13800000005");
        MemberApiController.MemberRequest request = new MemberApiController.MemberRequest();
        request.name = "改名会员";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/members/1")
            .withPathParam("id", String.valueOf(saved.id))
            .withBody(request);
        MemberApiController.update(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertEquals("改名会员", memberDAO.findById(saved.id).name);
    }

    @Test
    @DisplayName("更新不存在的会员返回 404")
    void updateMissingMemberReturns404() {
        MemberApiController.MemberRequest request = new MemberApiController.MemberRequest();
        request.name = "x";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/members/999999")
            .withPathParam("id", "999999")
            .withBody(request);
        MemberApiController.update(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("会员充值增加余额，并把操作员记为认证用户")
    void rechargeIncreasesBalance() throws Exception {
        Member saved = insertMember("13800000006");
        MemberApiController.RechargeRequest request = new MemberApiController.RechargeRequest();
        request.amount = BigDecimal.valueOf(100);

        User operator = new User();
        operator.username = "finance01";
        operator.name = "财务小李";

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/members/1/recharge")
            .withPathParam("id", String.valueOf(saved.id))
            .withAttribute("currentUser", operator)
            .withBody(request);
        MemberApiController.recharge(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue(memberDAO.findById(saved.id).balance.compareTo(BigDecimal.ZERO) > 0);
        // 充值流水必须归属到真实操作员，而不是写死的 "system"
        var records = DAOFactory.getInstance().getRechargeRecordDAO().findByMemberPhone(saved.phone);
        assertEquals(1, records.size());
        assertEquals("财务小李", records.get(0).operator);
    }

    @Test
    @DisplayName("非正数充值金额返回 400")
    void rechargeInvalidAmountReturns400() throws Exception {
        Member saved = insertMember("13800000007");
        MemberApiController.RechargeRequest request = new MemberApiController.RechargeRequest();
        request.amount = BigDecimal.ZERO;

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/members/1/recharge")
            .withPathParam("id", String.valueOf(saved.id))
            .withBody(request);
        MemberApiController.recharge(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体创建会员返回 400")
    void createWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/members");
        MemberApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }
}
