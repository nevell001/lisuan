package com.cashier.api.controller;

import com.cashier.api.ApiMessages;
import com.cashier.dao.DAOFactory;
import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.model.PageResult;
import com.cashier.model.Promotion;
import com.cashier.service.PromotionRules;
import com.cashier.service.TransactionService;
import com.cashier.util.DateTimeFormats;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 促销 REST API。
 *
 * <p>对外类型使用稳定代码（REDUCTION/DISCOUNT/COUPON），数据库仍存中文类型；
 * 金额与折扣校验复用 {@link PromotionRules}，与桌面端保持同一口径。</p>
 */
public class PromotionApiController {

    private static final Logger logger = LoggerFactoryUtil.getLogger(PromotionApiController.class);

    private static final int MAX_NAME_LENGTH = 200;

    private PromotionApiController() {
    }

    /** GET /api/promotions */
    public static void list(Context ctx) {
        try {
            ApiPagination.PageRequest page = ApiPagination.from(ctx);
            String typeParam = trimToNull(ctx.queryParam("type"));
            String legacyType = null;
            if (typeParam != null) {
                legacyType = PromotionRules.toLegacyType(typeParam);
                if (legacyType == null) {
                    error(ctx, HttpStatus.BAD_REQUEST, "api.promotion.type_invalid");
                    return;
                }
            }

            Boolean enabled;
            Boolean activeOnly;
            try {
                enabled = booleanParam(ctx, "enabled");
                activeOnly = booleanParam(ctx, "activeOnly");
            } catch (IllegalArgumentException e) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.promotion.boolean_invalid", e.getMessage());
                return;
            }

            PageResult<Promotion> promotions = DAOFactory.getInstance().getPromotionDAO()
                .findPage(page.page(), page.pageSize(), legacyType, enabled, Boolean.TRUE.equals(activeOnly));

            List<PromotionView> views = new ArrayList<>();
            for (Promotion promotion : promotions.getData()) {
                views.add(toView(ctx, promotion));
            }
            PageResult<PromotionView> result = new PageResult<>(
                views, promotions.getPageNum(), promotions.getPageSize(), promotions.getTotal());
            ctx.json(ApiPagination.success(result));
        } catch (SQLException e) {
            logger.error("获取促销列表失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.promotion.list_failed");
        }
    }

    /** GET /api/promotions/active */
    public static void active(Context ctx) {
        try {
            List<Promotion> promotions = DAOFactory.getInstance().getPromotionDAO().findActive();
            List<PromotionView> views = new ArrayList<>();
            for (Promotion promotion : promotions) {
                views.add(toView(ctx, promotion));
            }
            ctx.json(body(views, null));
        } catch (SQLException e) {
            logger.error("获取有效促销失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.promotion.list_failed");
        }
    }

    /**
     * POST /api/promotions/quote
     *
     * <p>按收银台同一口径试算：会员折扣先算，促销优惠按商品原价总额计算。
     * 只读接口，不占用促销次数，也不校验库存（试算不产生副作用）。</p>
     */
    public static void quote(Context ctx) {
        try {
            QuoteRequest request = ApiRequest.parse(ctx, QuoteRequest.class);
            if (request == null) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.common.bad_request");
                return;
            }
            List<CartItem> cart = CartRequestBinder.bind(
                ctx, request.items, false, CartRequestBinder.QUOTE);
            if (cart == null) {
                return;
            }

            Member member = null;
            if (request.memberId != null) {
                member = DAOFactory.getInstance().getMemberDAO().findById(request.memberId);
                if (member == null) {
                    error(ctx, HttpStatus.BAD_REQUEST,
                        "api.promotion.quote.member_not_found", request.memberId);
                    return;
                }
            }

            BigDecimal total = TransactionService.cartTotal(cart).setScale(2, RoundingMode.HALF_UP);
            BigDecimal memberDiscounted = TransactionService.calculateFinalAmount(cart, member);
            Promotion promotion = TransactionService.selectBestPromotion(total);
            BigDecimal finalAmount = TransactionService.calculateFinalAmount(cart, member, promotion);
            // 用会员折后价与实付的差额作为促销优惠，保证 total - 会员优惠 - 促销优惠 = finalAmount
            BigDecimal promotionDiscount = memberDiscounted.subtract(finalAmount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            QuoteView view = new QuoteView();
            view.itemCount = cart.size();
            view.totalAmount = total;
            view.memberDiscountRate = member == null ? BigDecimal.TEN : member.getDiscount();
            view.memberDiscountAmount = total.subtract(memberDiscounted)
                .setScale(2, RoundingMode.HALF_UP);
            view.promotionDiscount = promotionDiscount;
            view.finalAmount = finalAmount;
            if (promotion != null) {
                view.appliedPromotion = appliedPromotionView(ctx, promotion);
            }
            ctx.json(body(view, null));
        } catch (SQLException e) {
            logger.error("试算促销优惠失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.promotion.quote.failed");
        }
    }

    /** GET /api/promotions/{id} */
    public static void get(Context ctx) {
        try {
            Integer id = parseId(ctx);
            if (id == null) {
                return;
            }
            Promotion promotion = DAOFactory.getInstance().getPromotionDAO().findById(id);
            if (promotion == null) {
                error(ctx, HttpStatus.NOT_FOUND, "api.promotion.not_found");
                return;
            }
            ctx.json(body(toView(ctx, promotion), null));
        } catch (SQLException e) {
            logger.error("获取促销详情失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.promotion.detail_failed");
        }
    }

    /** POST /api/promotions */
    public static void create(Context ctx) {
        try {
            PromotionRequest request = ApiRequest.parse(ctx, PromotionRequest.class);
            if (request == null) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.common.bad_request");
                return;
            }
            warnIgnoredServerOwnedFields(request);

            String name = trimToNull(request.name);
            if (name == null) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.promotion.name_required");
                return;
            }
            if (name.length() > MAX_NAME_LENGTH) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.promotion.name_too_long", MAX_NAME_LENGTH);
                return;
            }
            String legacyType = PromotionRules.toLegacyType(request.type);
            if (legacyType == null) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.promotion.type_invalid");
                return;
            }

            BigDecimal threshold = request.threshold;
            if (threshold == null && PromotionRules.TYPE_COUPON.equals(legacyType)) {
                threshold = BigDecimal.ZERO;
            }
            PromotionRules.ValidationIssue issue = PromotionRules.validateThreshold(threshold);
            if (issue != null) {
                error(ctx, HttpStatus.BAD_REQUEST, issue.getKey(), issue.getParams());
                return;
            }
            issue = PromotionRules.validateDiscount(legacyType, threshold, request.discount);
            if (issue != null) {
                error(ctx, HttpStatus.BAD_REQUEST, issue.getKey(), issue.getParams());
                return;
            }
            issue = PromotionRules.validateMaxUsage(request.maxUsage);
            if (issue != null) {
                error(ctx, HttpStatus.BAD_REQUEST, issue.getKey(), issue.getParams());
                return;
            }

            LocalDate start = parseDate(ctx, request.startDate, "promotion.validation.start_date");
            if (start == null) {
                return;
            }
            LocalDate end = parseDate(ctx, request.endDate, "promotion.validation.end_date");
            if (end == null) {
                return;
            }
            if (end.isBefore(start)) {
                error(ctx, HttpStatus.BAD_REQUEST, "promotion.validation.date_order");
                return;
            }

            Promotion promotion = new Promotion();
            promotion.promotionCode = TransactionService.generateTransactionId("P");
            promotion.name = name;
            promotion.type = legacyType;
            promotion.threshold = threshold;
            promotion.discount = request.discount;
            promotion.description = request.description == null ? "" : request.description.trim();
            promotion.enabled = request.enabled == null || request.enabled;
            promotion.startDate = start.atStartOfDay();
            promotion.endDate = end.atTime(23, 59, 59);
            promotion.usageCount = 0;
            promotion.maxUsage = PromotionRules.normalizeMaxUsage(request.maxUsage);

            try {
                DAOFactory.getInstance().getPromotionDAO().insert(promotion);
            } catch (SQLException e) {
                if (isUniqueViolation(e)) {
                    error(ctx, HttpStatus.CONFLICT, "api.promotion.code_exists");
                    return;
                }
                throw e;
            }

            logger.info("API 创建促销: {} ({})", promotion.name, promotion.promotionCode);
            ctx.status(HttpStatus.CREATED)
                .json(body(toView(ctx, promotion), ApiMessages.text(ctx, "api.promotion.created")));
        } catch (SQLException e) {
            logger.error("创建促销失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.promotion.create_failed");
        }
    }

    /** PUT /api/promotions/{id} */
    public static void update(Context ctx) {
        try {
            Integer id = parseId(ctx);
            if (id == null) {
                return;
            }
            PromotionRequest request = ApiRequest.parse(ctx, PromotionRequest.class);
            if (request == null) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.common.bad_request");
                return;
            }
            Promotion promotion = DAOFactory.getInstance().getPromotionDAO().findById(id);
            if (promotion == null) {
                error(ctx, HttpStatus.NOT_FOUND, "api.promotion.not_found");
                return;
            }
            warnIgnoredServerOwnedFields(request);

            if (request.name != null) {
                String name = trimToNull(request.name);
                if (name == null) {
                    error(ctx, HttpStatus.BAD_REQUEST, "api.promotion.name_required");
                    return;
                }
                if (name.length() > MAX_NAME_LENGTH) {
                    error(ctx, HttpStatus.BAD_REQUEST, "api.promotion.name_too_long", MAX_NAME_LENGTH);
                    return;
                }
                promotion.name = name;
            }

            if (request.type != null) {
                String legacyType = PromotionRules.toLegacyType(request.type);
                if (legacyType == null) {
                    error(ctx, HttpStatus.BAD_REQUEST, "api.promotion.type_invalid");
                    return;
                }
                promotion.type = legacyType;
            }

            if (request.threshold != null) {
                promotion.threshold = request.threshold;
            } else if (promotion.threshold == null) {
                promotion.threshold = BigDecimal.ZERO;
            }
            PromotionRules.ValidationIssue issue = PromotionRules.validateThreshold(promotion.threshold);
            if (issue != null) {
                error(ctx, HttpStatus.BAD_REQUEST, issue.getKey(), issue.getParams());
                return;
            }
            if (request.discount != null) {
                promotion.discount = request.discount;
            }
            issue = PromotionRules.validateDiscount(promotion.type, promotion.threshold, promotion.discount);
            if (issue != null) {
                error(ctx, HttpStatus.BAD_REQUEST, issue.getKey(), issue.getParams());
                return;
            }
            issue = PromotionRules.validateMaxUsage(request.maxUsage);
            if (issue != null) {
                error(ctx, HttpStatus.BAD_REQUEST, issue.getKey(), issue.getParams());
                return;
            }

            LocalDate start = request.startDate != null
                ? parseDate(ctx, request.startDate, "promotion.validation.start_date")
                : existingDate(ctx, promotion.startDate, "promotion.validation.start_date");
            if (start == null) {
                return;
            }
            LocalDate end = request.endDate != null
                ? parseDate(ctx, request.endDate, "promotion.validation.end_date")
                : existingDate(ctx, promotion.endDate, "promotion.validation.end_date");
            if (end == null) {
                return;
            }
            if (end.isBefore(start)) {
                error(ctx, HttpStatus.BAD_REQUEST, "promotion.validation.date_order");
                return;
            }
            promotion.startDate = start.atStartOfDay();
            promotion.endDate = end.atTime(23, 59, 59);

            if (request.description != null) {
                promotion.description = request.description.trim();
            }
            if (request.enabled != null) {
                promotion.enabled = request.enabled;
            }
            if (request.maxUsage != null) {
                promotion.maxUsage = request.maxUsage;
            }

            if (!DAOFactory.getInstance().getPromotionDAO().update(promotion)) {
                error(ctx, HttpStatus.NOT_FOUND, "api.promotion.not_found");
                return;
            }
            logger.info("API 更新促销: {} ({})", promotion.name, promotion.promotionCode);
            ctx.json(body(toView(ctx, promotion), ApiMessages.text(ctx, "api.promotion.updated")));
        } catch (SQLException e) {
            logger.error("更新促销失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.promotion.update_failed");
        }
    }

    /** DELETE /api/promotions/{id} */
    public static void delete(Context ctx) {
        try {
            Integer id = parseId(ctx);
            if (id == null) {
                return;
            }
            if (!DAOFactory.getInstance().getPromotionDAO().delete(id)) {
                error(ctx, HttpStatus.NOT_FOUND, "api.promotion.not_found");
                return;
            }
            logger.info("API 删除促销: id={}", id);
            ctx.json(body(null, ApiMessages.text(ctx, "api.promotion.deleted")));
        } catch (SQLException e) {
            logger.error("删除促销失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.promotion.delete_failed");
        }
    }

    // ---------- helpers ----------

    private static PromotionView toView(Context ctx, Promotion promotion) {
        PromotionView view = new PromotionView();
        view.id = promotion.id;
        view.promotionCode = promotion.promotionCode;
        view.name = promotion.name;
        view.type = PromotionRules.toCode(promotion.type);
        view.typeName = typeName(ctx, promotion.type);
        view.threshold = promotion.getThreshold();
        view.discount = promotion.getDiscount();
        view.description = promotion.description;
        view.enabled = promotion.enabled;
        view.startDate = promotion.startDate == null ? null
            : promotion.startDate.toLocalDate().format(DateTimeFormats.DATE);
        view.endDate = promotion.endDate == null ? null
            : promotion.endDate.toLocalDate().format(DateTimeFormats.DATE);
        view.usageCount = promotion.usageCount;
        view.maxUsage = PromotionRules.normalizeMaxUsage(promotion.maxUsage);
        view.active = isActive(promotion);
        return view;
    }

    private static AppliedPromotionView appliedPromotionView(Context ctx, Promotion promotion) {
        AppliedPromotionView view = new AppliedPromotionView();
        view.id = promotion.id;
        view.promotionCode = promotion.promotionCode;
        view.name = promotion.name;
        view.type = PromotionRules.toCode(promotion.type);
        view.typeName = typeName(ctx, promotion.type);
        view.threshold = promotion.getThreshold();
        view.discount = promotion.getDiscount();
        return view;
    }

    private static boolean isActive(Promotion promotion) {
        if (!promotion.enabled || promotion.startDate == null || promotion.endDate == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(promotion.startDate) || !now.isBefore(promotion.endDate)) {
            return false;
        }
        return promotion.maxUsage <= 0 || promotion.usageCount < promotion.maxUsage;
    }

    private static String typeName(Context ctx, String legacyType) {
        String key = PromotionRules.typeNameKey(legacyType);
        if (key == null) {
            return ApiMessages.enumName(ctx, "api.promotion.type.unknown", "未知");
        }
        switch (legacyType) {
            case PromotionRules.TYPE_REDUCTION:
                return ApiMessages.enumName(ctx, "api.promotion.type.reduction", "满减");
            case PromotionRules.TYPE_DISCOUNT:
                return ApiMessages.enumName(ctx, "api.promotion.type.discount", "打折");
            case PromotionRules.TYPE_COUPON:
                return ApiMessages.enumName(ctx, "api.promotion.type.coupon", "优惠券");
            default:
                return ApiMessages.enumName(ctx, "api.promotion.type.unknown", "未知");
        }
    }

    private static LocalDate parseDate(Context ctx, String value, String missingKey) {
        if (value == null || value.isBlank()) {
            error(ctx, HttpStatus.BAD_REQUEST, missingKey);
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormats.DATE);
        } catch (DateTimeParseException e) {
            error(ctx, HttpStatus.BAD_REQUEST, "api.promotion.date_format");
            return null;
        }
    }

    private static LocalDate existingDate(Context ctx, LocalDateTime value, String missingKey) {
        if (value == null) {
            error(ctx, HttpStatus.BAD_REQUEST, missingKey);
            return null;
        }
        return value.toLocalDate();
    }

    private static Integer parseId(Context ctx) {
        try {
            return Integer.valueOf(ctx.pathParam("id"));
        } catch (RuntimeException e) {
            error(ctx, HttpStatus.BAD_REQUEST, "api.common.invalid_id");
            return null;
        }
    }

    private static Boolean booleanParam(Context ctx, String name) {
        String raw = ctx.queryParam(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(raw.trim())) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(raw.trim())) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException(name);
    }

    private static void warnIgnoredServerOwnedFields(PromotionRequest request) {
        if (request.id != null || request.promotionCode != null || request.usageCount != null
                || request.typeName != null || request.active != null) {
            logger.warn("忽略促销请求体里的服务端字段: id={}, promotionCode={}, usageCount={}, "
                    + "typeName={}, active={}",
                request.id, request.promotionCode, request.usageCount, request.typeName, request.active);
        }
    }

    private static boolean isUniqueViolation(SQLException e) {
        if (e instanceof SQLIntegrityConstraintViolationException) {
            return true;
        }
        String state = e.getSQLState();
        return state != null && state.startsWith("23");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void error(Context ctx, HttpStatus status, String key, Object... params) {
        ctx.status(status).json(Map.of("success", false, "message", ApiMessages.text(ctx, key, params)));
    }

    private static Map<String, Object> body(Object data, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        if (data != null) {
            body.put("data", data);
        }
        if (message != null) {
            body.put("message", message);
        }
        return body;
    }

    // ---------- DTO ----------

    public static class PromotionRequest {
        public String name;
        public String type;
        public BigDecimal threshold;
        public BigDecimal discount;
        public String description;
        public Boolean enabled;
        public String startDate;
        public String endDate;
        public Integer maxUsage;
        /** 只读字段：客户端把 GET 结果整包回传时忽略（服务端生成/维护）。 */
        public Integer id;
        public String promotionCode;
        public Integer usageCount;
        public String typeName;
        public Boolean active;
    }

    public static class PromotionView {
        public Integer id;
        public String promotionCode;
        public String name;
        public String type;
        public String typeName;
        public BigDecimal threshold;
        public BigDecimal discount;
        public String description;
        public Boolean enabled;
        public String startDate;
        public String endDate;
        public Integer usageCount;
        public Integer maxUsage;
        public Boolean active;
    }

    public static class QuoteRequest {
        public List<HoldOrderApiController.HoldItemRequest> items;
        public Integer memberId;
    }

    public static class QuoteView {
        public Integer itemCount;
        public BigDecimal totalAmount;
        /** 会员折扣值（10 表示不打折）。 */
        public BigDecimal memberDiscountRate;
        public BigDecimal memberDiscountAmount;
        public AppliedPromotionView appliedPromotion;
        public BigDecimal promotionDiscount;
        public BigDecimal finalAmount;
    }

    public static class AppliedPromotionView {
        public Integer id;
        public String promotionCode;
        public String name;
        public String type;
        public String typeName;
        public BigDecimal threshold;
        public BigDecimal discount;
    }
}
