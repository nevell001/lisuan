package com.cashier.api.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.model.Member;
import com.cashier.model.PageResult;
import com.cashier.model.User;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 会员管理 REST API
 */
public class MemberApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(MemberApiController.class);
    
    /**
     * 获取会员列表
     * GET /api/members
     */
    public static void list(Context ctx) {
        try {
            ApiPagination.PageRequest page = ApiPagination.from(ctx);
            PageResult<Member> members = DAOFactory.getInstance().getMemberDAO().findAll(page.page(), page.pageSize());
            ctx.json(ApiPagination.success(members));
        } catch (Exception e) {
            logger.error("获取会员列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.list_failed")));
        }
    }
    
    /**
     * 获取单个会员
     * GET /api/members/:id
     */
    public static void get(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Member member = DAOFactory.getInstance().getMemberDAO().findById(id);
            
            if (member == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.not_found")));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", member));
        } catch (Exception e) {
            logger.error("获取会员详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.detail_failed")));
        }
    }
    
    /**
     * 根据手机号获取会员
     * GET /api/members/phone/:phone
     */
    public static void getByPhone(Context ctx) {
        try {
            String phone = ctx.pathParam("phone");
            Member member = DAOFactory.getInstance().getMemberDAO().findByPhone(phone);
            
            if (member == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.not_found")));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", member));
        } catch (Exception e) {
            logger.error("根据手机号获取会员失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.get_failed")));
        }
    }
    
    /**
     * 创建会员
     * POST /api/members
     */
    public static void create(Context ctx) {
        try {
            MemberRequest request = ApiRequest.parse(ctx, MemberRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.common.bad_request")));
                return;
            }
            
            if (request.phone == null || request.phone.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.phone_required")));
                return;
            }
            
            // 检查是否已存在
            Member existing = DAOFactory.getInstance().getMemberDAO().findByPhone(request.phone);
            if (existing != null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.phone_exists")));
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
            
            DAOFactory.getInstance().getMemberDAO().insert(member);
            
            logger.info("创建会员: {} - {}", member.phone, member.name);
            
            // 广播会员创建事件
            com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
                com.cashier.api.sync.SyncEventType.MEMBER_CREATED,
                Map.of(
                    "id", member.id,
                    "phone", member.phone,
                    "name", member.name
                )
            );
            
            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", member, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.create_success")));
        } catch (Exception e) {
            logger.error("创建会员失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.create_failed")));
        }
    }
    
    /**
     * 更新会员
     * PUT /api/members/:id
     */
    public static void update(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            MemberRequest request = ApiRequest.parse(ctx, MemberRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.common.bad_request")));
                return;
            }
            
            Member member = DAOFactory.getInstance().getMemberDAO().findById(id);
            if (member == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.not_found")));
                return;
            }

            // 等级/折扣直接决定收款金额，改动必须留痕（此处记录改动前的值）
            String levelBefore = member.level;
            BigDecimal discountBefore = member.getDiscount();

            // 校验与桌面端（MemberEditController.isInputValid）保持一致：
            // 此前接口可写入 discount=999/负数、任意等级、非法手机号，且手机号冲突会变成 500
            if (request.name != null) {
                if (request.name.isBlank()) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.name_required")));
                    return;
                }
                member.name = request.name.trim();
            }
            if (request.phone != null) {
                String phone = request.phone.trim();
                if (!phone.matches("\\d{11}")) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.phone_invalid")));
                    return;
                }
                member.phone = phone;
            }
            if (request.level != null) {
                if (!com.cashier.service.MemberService.isKnownLevel(request.level)) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.level_invalid")));
                    return;
                }
                member.level = request.level;
            }
            if (request.discount != null) {
                if (request.discount.compareTo(BigDecimal.ZERO) < 0
                        || request.discount.compareTo(BigDecimal.TEN) > 0) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.discount_invalid")));
                    return;
                }
                member.discount = request.discount;
                member.discountRate = request.discount; // 桌面端同时维护 discountRate，接口此前只改 discount
            }
            
            try {
                DAOFactory.getInstance().getMemberDAO().update(member);
            } catch (com.cashier.dao.MemberDAORefactored.OptimisticLockException e) {
                // 乐观锁冲突是并发语义，回 409 而不是把它当服务器内部错误
                logger.warn("更新会员冲突（乐观锁未命中）: {}", member.phone);
                ctx.status(HttpStatus.CONFLICT)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.concurrent_modified")));
                return;
            }
            
            logger.info("更新会员: {}", member.phone);

            // 等级/折扣变更写审计日志（此前会员路径完全没有操作日志）
            boolean levelChanged = levelBefore == null ? member.level != null : !levelBefore.equals(member.level);
            boolean discountChanged = discountBefore == null ? member.getDiscount() != null
                : member.getDiscount() == null || discountBefore.compareTo(member.getDiscount()) != 0;
            if (levelChanged || discountChanged) {
                User operator = ctx.attribute("currentUser");
                com.cashier.service.AuditService.success(
                    operator != null ? operator.username : "unknown",
                    "MEMBER",
                    "MEMBER_LEVEL_DISCOUNT_UPDATED",
                    "会员 " + member.name + "(" + member.phone + "): 等级 " + levelBefore + "→" + member.level
                        + ", 折扣 " + discountBefore + "→" + member.getDiscount(),
                    1);
            }
            
            // 广播会员更新事件
            com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
                com.cashier.api.sync.SyncEventType.MEMBER_UPDATED,
                Map.of(
                    "id", member.id,
                    "phone", member.phone,
                    "name", member.name,
                    "level", member.level
                )
            );
            
            ctx.json(Map.of("success", true, "data", member, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.update_success")));
        } catch (Exception e) {
            logger.error("更新会员失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.update_failed")));
        }
    }
    
    /**
     * 会员充值
     * POST /api/members/:id/recharge
     */
    public static void recharge(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            RechargeRequest request = ApiRequest.parse(ctx, RechargeRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.common.bad_request")));
                return;
            }
            
            Member member = DAOFactory.getInstance().getMemberDAO().findById(id);
            if (member == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.not_found")));
                return;
            }
            
            if (request.amount == null || request.amount.compareTo(BigDecimal.ZERO) <= 0) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.recharge_amount_invalid")));
                return;
            }
            
            // 操作员一律取认证用户，忽略请求体自报身份（充值流水/审计需要可追溯到人）
            User operator = ctx.attribute("currentUser");
            String operatorName = operator == null ? "system"
                : (operator.name != null && !operator.name.isBlank() ? operator.name : operator.username);

            boolean success = com.cashier.service.MemberService.recharge(
                member, 
                request.amount.doubleValue(), 
                "API", 
                operatorName
            );
            
            if (success) {
                logger.info("会员充值: {} + {}", member.phone, request.amount);
                ctx.json(Map.of("success", true, "data", member, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.recharge_success")));
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.recharge_failed")));
            }
        } catch (Exception e) {
            logger.error("会员充值失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", com.cashier.api.ApiMessages.text(ctx, "api.member.recharge_failed")));
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
