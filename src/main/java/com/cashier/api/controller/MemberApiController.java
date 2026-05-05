package com.cashier.api.controller;

import com.cashier.dao.MemberDAO;
import com.cashier.model.Member;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员管理 REST API
 */
public class MemberApiController {
    private static final Logger logger = LoggerFactory.getLogger(MemberApiController.class);
    
    /**
     * 获取会员列表
     * GET /api/members
     */
    public static void list(Context ctx) {
        try {
            List<Member> members = MemberDAO.findAll();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", members);
            result.put("total", members.size());
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取会员列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取会员列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个会员
     * GET /api/members/:id
     */
    public static void get(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Member member = MemberDAO.findById(id);
            
            if (member == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "会员不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", member));
        } catch (Exception e) {
            logger.error("获取会员详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取会员详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据手机号获取会员
     * GET /api/members/phone/:phone
     */
    public static void getByPhone(Context ctx) {
        try {
            String phone = ctx.pathParam("phone");
            Member member = MemberDAO.findByPhone(phone);
            
            if (member == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "会员不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", member));
        } catch (Exception e) {
            logger.error("根据手机号获取会员失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取会员失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建会员
     * POST /api/members
     */
    public static void create(Context ctx) {
        try {
            MemberRequest request = ctx.bodyAsClass(MemberRequest.class);
            
            if (request.phone == null || request.phone.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "手机号不能为空"));
                return;
            }
            
            // 检查是否已存在
            Member existing = MemberDAO.findByPhone(request.phone);
            if (existing != null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "该手机号已注册"));
                return;
            }
            
            Member member = new Member();
            member.phone = request.phone;
            member.name = request.name != null ? request.name : "";
            member.level = "普通";
            member.discount = BigDecimal.TEN;
            member.discountRate = BigDecimal.TEN;
            member.balance = BigDecimal.ZERO;
            member.points = BigDecimal.ZERO;
            
            MemberDAO.insert(member);
            
            logger.info("创建会员: {} - {}", member.phone, member.name);
            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", member, "message", "会员创建成功"));
        } catch (Exception e) {
            logger.error("创建会员失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建会员失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新会员
     * PUT /api/members/:id
     */
    public static void update(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            MemberRequest request = ctx.bodyAsClass(MemberRequest.class);
            
            Member member = MemberDAO.findById(id);
            if (member == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "会员不存在"));
                return;
            }
            
            if (request.name != null) member.name = request.name;
            if (request.phone != null) member.phone = request.phone;
            if (request.level != null) member.level = request.level;
            if (request.discount != null) member.discount = request.discount;
            
            MemberDAO.update(member);
            
            logger.info("更新会员: {}", member.phone);
            ctx.json(Map.of("success", true, "data", member, "message", "会员更新成功"));
        } catch (Exception e) {
            logger.error("更新会员失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "更新会员失败: " + e.getMessage()));
        }
    }
    
    /**
     * 会员充值
     * POST /api/members/:id/recharge
     */
    public static void recharge(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            RechargeRequest request = ctx.bodyAsClass(RechargeRequest.class);
            
            Member member = MemberDAO.findById(id);
            if (member == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "会员不存在"));
                return;
            }
            
            if (request.amount == null || request.amount.compareTo(BigDecimal.ZERO) <= 0) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "充值金额必须大于0"));
                return;
            }
            
            member.balance = member.balance.add(request.amount);
            MemberDAO.update(member);
            
            logger.info("会员充值: {} + {}", member.phone, request.amount);
            ctx.json(Map.of("success", true, "data", member, "message", "充值成功"));
        } catch (Exception e) {
            logger.error("会员充值失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "充值失败: " + e.getMessage()));
        }
    }
    
    /**
     * 会员请求 DTO
     */
    public static class MemberRequest {
        public String phone;
        public String name;
        public String level;
        public BigDecimal discount;
    }
    
    /**
     * 充值请求 DTO
     */
    public static class RechargeRequest {
        public BigDecimal amount;
    }
}