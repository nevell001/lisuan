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

    @Test
    @DisplayName("更新会员：折扣/等级/手机号校验与桌面端一致（非法值回 400）")
    void updateMemberValidatesFieldsLikeDesktop() throws Exception {
        Member saved = insertMember("13800000008");

        assertEquals(HttpStatus.BAD_REQUEST, updateWith(saved, "discount", new BigDecimal("999")).status,
            "折扣超过 10 必须拒绝（此前会原样写库）");
        assertEquals(HttpStatus.BAD_REQUEST, updateWith(saved, "discount", new BigDecimal("-1")).status,
            "负折扣必须拒绝");
        assertEquals(HttpStatus.BAD_REQUEST, updateWith(saved, "level", "超级VIP").status,
            "等级必须是 普通/银卡/金卡/钻石 之一");
        assertEquals(HttpStatus.BAD_REQUEST, updateWith(saved, "phone", "abc").status,
            "手机号必须是 11 位数字");
        assertEquals(HttpStatus.BAD_REQUEST, updateWith(saved, "name", "   ").status, "姓名不能是空白");

        // 合法值应当成功，并同步 discountRate（此前接口只改 discount）
        MemberApiController.MemberRequest ok = new MemberApiController.MemberRequest();
        ok.level = "银卡";
        ok.discount = new BigDecimal("9.5");
        TestContext okCtx = new TestContext().withRequest(HandlerType.PUT, "/api/members/1")
            .withPathParam("id", String.valueOf(saved.id))
            .withBody(ok);
        MemberApiController.update(okCtx.context);

        assertEquals(HttpStatus.OK, okCtx.status);
        Member reloaded = memberDAO.findById(saved.id);
        assertEquals("银卡", reloaded.level);
        assertEquals(0, new BigDecimal("9.5").compareTo(reloaded.discount));
        assertEquals(0, new BigDecimal("9.5").compareTo(reloaded.discountRate), "discountRate 必须同步");
    }

    /** 只改一个字段发一次 PUT，返回 TestContext。 */
    private <T> TestContext updateWith(Member saved, String field, T value) throws Exception {
        MemberApiController.MemberRequest request = new MemberApiController.MemberRequest();
        switch (field) {
            case "name" -> request.name = (String) value;
            case "phone" -> request.phone = (String) value;
            case "level" -> request.level = (String) value;
            case "discount" -> request.discount = (BigDecimal) value;
            default -> throw new IllegalArgumentException(field);
        }
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/members/1")
            .withPathParam("id", String.valueOf(saved.id))
            .withBody(request);
        MemberApiController.update(ctx.context);
        return ctx;
    }
}
