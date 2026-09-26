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
               .json(Map.of("success", false, "message", "获取会员列表失败"));
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
                   .json(Map.of("success", false, "message", "会员不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", member));
        } catch (Exception e) {
            logger.error("获取会员详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取会员详情失败"));
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
                   .json(Map.of("success", false, "message", "会员不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", member));
        } catch (Exception e) {
            logger.error("根据手机号获取会员失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取会员失败"));
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
                   .json(Map.of("success", false, "message", "请求体为空或字段不合法"));
                return;
            }
            
            if (request.phone == null || request.phone.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "手机号不能为空"));
                return;
            }
            
            // 检查是否已存在
            Member existing = DAOFactory.getInstance().getMemberDAO().findByPhone(request.phone);
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
               .json(Map.of("success", true, "data", member, "message", "会员创建成功"));
        } catch (Exception e) {
            logger.error("创建会员失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建会员失败"));
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
                   .json(Map.of("success", false, "message", "请求体为空或字段不合法"));
                return;
            }
            
            Member member = DAOFactory.getInstance().getMemberDAO().findById(id);
            if (member == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "会员不存在"));
                return;
            }
            
            // 校验与桌面端（MemberEditController.isInputValid）保持一致：
            // 此前接口可写入 discount=999/负数、任意等级、非法手机号，且手机号冲突会变成 500
            if (request.name != null) {
                if (request.name.isBlank()) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", "会员姓名不能为空"));
                    return;
                }
                member.name = request.name.trim();
            }
            if (request.phone != null) {
                String phone = request.phone.trim();
                if (!phone.matches("\\d{11}")) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", "手机号必须是 11 位数字"));
                    return;
                }
                member.phone = phone;
            }
            if (request.level != null) {
                if (!com.cashier.service.MemberService.isKnownLevel(request.level)) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", "会员等级不合法（可用：普通/银卡/金卡/钻石）"));
                    return;
                }
                member.level = request.level;
            }
            if (request.discount != null) {
                if (request.discount.compareTo(BigDecimal.ZERO) < 0
                        || request.discount.compareTo(BigDecimal.TEN) > 0) {
                    ctx.status(HttpStatus.BAD_REQUEST)
                       .json(Map.of("success", false, "message", "折扣必须在 0 到 10 之间（10 = 不打折）"));
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
                   .json(Map.of("success", false, "message", "会员已被其他操作修改，请重新获取后再试"));
                return;
            }
            
            logger.info("更新会员: {}", member.phone);
            
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
            
            ctx.json(Map.of("success", true, "data", member, "message", "会员更新成功"));
        } catch (Exception e) {
            logger.error("更新会员失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "更新会员失败"));
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
                   .json(Map.of("success", false, "message", "请求体为空或字段不合法"));
                return;
            }
            
            Member member = DAOFactory.getInstance().getMemberDAO().findById(id);
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
                ctx.json(Map.of("success", true, "data", member, "message", "充值成功"));
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .json(Map.of("success", false, "message", "充值失败"));
            }
        } catch (Exception e) {
            logger.error("会员充值失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "充值失败"));
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
