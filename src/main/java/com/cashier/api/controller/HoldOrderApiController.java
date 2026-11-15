package com.cashier.api.controller;

import com.cashier.api.ApiMessages;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.HoldOrderDAORefactored;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.CartItem;
import com.cashier.model.HoldOrder;
import com.cashier.model.Member;
import com.cashier.model.PageResult;
import com.cashier.model.Product;
import com.cashier.model.Promotion;
import com.cashier.model.User;
import com.cashier.service.HoldOrderCodec;
import com.cashier.service.TransactionService;
import com.cashier.util.DateTimeFormats;
import com.cashier.util.LoggerFactoryUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 挂单 REST API。
 *
 * <p>金额一律由服务端按收银台同一算法计算（会员折扣 + 最优促销），客户端传的金额不采纳；
 * 取单/取消使用状态 CAS，避免两个终端同时恢复同一张单。</p>
 */
public class HoldOrderApiController {

    private static final Logger logger = LoggerFactoryUtil.getLogger(HoldOrderApiController.class);

    private static final int MAX_ITEMS = 50;
    private static final int MAX_NOTES_LENGTH = 500;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_RESUMED = 1;
    private static final int STATUS_CANCELLED = 2;
    /** 解析结果的哨兵值：{@code status=all} 表示不过滤。 */
    private static final int STATUS_ALL = -1;

    private static final Pattern ITEM_PRODUCT_ID = Pattern.compile("\"productId\"\\s*:\\s*(\\d+)");

    private HoldOrderApiController() {
    }

    /** GET /api/hold-orders */
    public static void list(Context ctx) {
        try {
            Integer status = parseStatusFilter(ctx);
            if (status == null) {
                return;
            }
            User user = currentUser(ctx);
            if (user == null) {
                error(ctx, HttpStatus.FORBIDDEN, "api.hold.forbidden");
                return;
            }
            ApiPagination.PageRequest page = ApiPagination.from(ctx);
            Integer userId = isAdmin(user) ? null : user.id;
            Integer statusFilter = status == STATUS_ALL ? null : status;
            PageResult<HoldOrder> orders =
                dao().findPage(userId, statusFilter, page.page(), page.pageSize());

            List<HoldOrderView> views = new ArrayList<>();
            for (HoldOrder order : orders.getData()) {
                views.add(toView(ctx, order, false));
            }
            PageResult<HoldOrderView> result = new PageResult<>(
                views, orders.getPageNum(), orders.getPageSize(), orders.getTotal());
            ctx.json(ApiPagination.success(result));
        } catch (SQLException e) {
            logger.error("获取挂单列表失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.hold.list_failed");
        }
    }

    /** GET /api/hold-orders/{id} */
    public static void get(Context ctx) {
        try {
            Integer id = parseId(ctx);
            if (id == null) {
                return;
            }
            HoldOrder order = loadAccessible(ctx, id);
            if (order == null) {
                return;
            }
            ctx.json(body(toView(ctx, order, true), null));
        } catch (SQLException e) {
            logger.error("获取挂单详情失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.hold.detail_failed");
        }
    }

    /** POST /api/hold-orders */
    public static void create(Context ctx) {
        try {
            CreateHoldRequest request = ApiRequest.parse(ctx, CreateHoldRequest.class);
            if (request == null) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.common.bad_request");
                return;
            }
            User user = currentUser(ctx);
            if (user == null) {
                error(ctx, HttpStatus.FORBIDDEN, "api.hold.forbidden");
                return;
            }
            if (request.items == null || request.items.isEmpty()) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.hold.items_required");
                return;
            }
            if (request.items.size() > MAX_ITEMS) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.hold.too_many_items", MAX_ITEMS);
                return;
            }

            List<CartItem> cart = new ArrayList<>();
            Set<Integer> seenProductIds = new HashSet<>();
            ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
            for (HoldItemRequest item : request.items) {
                if (item == null || item.productId == null || item.productId <= 0
                        || item.quantity == null || item.quantity <= 0 || item.quantity > 9999) {
                    error(ctx, HttpStatus.BAD_REQUEST, "api.hold.item_invalid");
                    return;
                }
                if (!seenProductIds.add(item.productId)) {
                    error(ctx, HttpStatus.BAD_REQUEST, "api.hold.duplicate_item", item.productId);
                    return;
                }
                Product product = productDAO.findById(item.productId);
                if (product == null) {
                    error(ctx, HttpStatus.BAD_REQUEST, "api.hold.product_not_found", item.productId);
                    return;
                }
                if (item.quantity > product.quantity) {
                    error(ctx, HttpStatus.BAD_REQUEST, "api.hold.stock_insufficient",
                        product.name, product.quantity);
                    return;
                }
                cart.add(new CartItem(product, item.quantity));
            }

            Member member = null;
            if (request.memberId != null) {
                member = DAOFactory.getInstance().getMemberDAO().findById(request.memberId);
                if (member == null) {
                    error(ctx, HttpStatus.BAD_REQUEST, "api.hold.member_not_found", request.memberId);
                    return;
                }
            }

            String notes = request.notes == null ? "" : request.notes.trim();
            if (notes.length() > MAX_NOTES_LENGTH) {
                error(ctx, HttpStatus.BAD_REQUEST, "api.hold.notes_too_long", MAX_NOTES_LENGTH);
                return;
            }

            BigDecimal total = TransactionService.cartTotal(cart);
            Promotion promotion = TransactionService.selectBestPromotion(total);
            BigDecimal finalAmount = TransactionService.calculateFinalAmount(cart, member, promotion);

            HoldOrder order = new HoldOrder();
            order.orderNumber = HoldOrder.generateOrderNumber();
            order.userId = user.id;
            if (member != null) {
                order.memberId = member.id;
                order.memberName = member.name;
                order.memberPhone = member.phone;
            }
            order.totalAmount = total;
            order.finalAmount = finalAmount;
            order.discountAmount = total.subtract(finalAmount);
            order.itemCount = cart.size();
            order.itemsJson = HoldOrderCodec.serialize(cart);
            order.notes = notes;
            order.status = STATUS_PENDING;

            dao().insert(order);
            HoldOrder saved = dao().findById(order.id);
            logger.info("API 挂单成功: {} - 用户 {}", order.orderNumber, user.username);
            ctx.status(HttpStatus.CREATED)
                .json(body(toView(ctx, saved != null ? saved : order, true),
                    ApiMessages.text(ctx, "api.hold.created")));
        } catch (SQLException e) {
            logger.error("创建挂单失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.hold.create_failed");
        }
    }

    /** POST /api/hold-orders/{id}/resume */
    public static void resume(Context ctx) {
        try {
            Integer id = parseId(ctx);
            if (id == null) {
                return;
            }
            HoldOrder order = loadAccessible(ctx, id);
            if (order == null) {
                return;
            }
            if (order.status == null || order.status != STATUS_PENDING) {
                conflictNotPending(ctx, order);
                return;
            }
            if (dao().updateStatusIfCurrent(id, STATUS_PENDING, STATUS_RESUMED) == 0) {
                HoldOrder latest = dao().findById(id);
                if (latest == null) {
                    error(ctx, HttpStatus.NOT_FOUND, "api.hold.not_found");
                } else {
                    conflictNotPending(ctx, latest);
                }
                return;
            }

            List<CartItem> cart = HoldOrderCodec.parse(
                order.itemsJson, DAOFactory.getInstance().getProductDAO());
            ResumeView view = new ResumeView();
            fillView(ctx, view, order, cart);
            view.status = statusCode(STATUS_RESUMED);
            view.statusName = statusName(ctx, STATUS_RESUMED);
            view.missingProductIds = missingProductIds(order.itemsJson, cart);

            logger.info("API 取单成功: {}", order.orderNumber);
            ctx.json(body(view, ApiMessages.text(ctx, "api.hold.resumed")));
        } catch (SQLException e) {
            logger.error("取单失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.hold.resume_failed");
        }
    }

    /** POST /api/hold-orders/{id}/cancel */
    public static void cancel(Context ctx) {
        try {
            Integer id = parseId(ctx);
            if (id == null) {
                return;
            }
            HoldOrder order = loadAccessible(ctx, id);
            if (order == null) {
                return;
            }
            if (order.status == null || order.status != STATUS_PENDING) {
                conflictNotPending(ctx, order);
                return;
            }
            if (dao().updateStatusIfCurrent(id, STATUS_PENDING, STATUS_CANCELLED) == 0) {
                HoldOrder latest = dao().findById(id);
                if (latest == null) {
                    error(ctx, HttpStatus.NOT_FOUND, "api.hold.not_found");
                } else {
                    conflictNotPending(ctx, latest);
                }
                return;
            }
            order.status = STATUS_CANCELLED;
            logger.info("API 取消挂单成功: {}", order.orderNumber);
            ctx.json(body(toView(ctx, order, false), ApiMessages.text(ctx, "api.hold.cancelled")));
        } catch (SQLException e) {
            logger.error("取消挂单失败", e);
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "api.hold.cancel_failed");
        }
    }

    // ---------- helpers ----------

    private static HoldOrderDAORefactored dao() {
        return DAOFactory.getInstance().getHoldOrderDAO();
    }

    private static User currentUser(Context ctx) {
        return ctx.attribute("currentUser");
    }

    private static boolean isAdmin(User user) {
        return user != null && "admin".equals(user.role);
    }

    private static boolean canAccess(User user, HoldOrder order) {
        return user != null && (isAdmin(user)
            || (order.userId != null && order.userId == user.id));
    }

    private static HoldOrder loadAccessible(Context ctx, int id) throws SQLException {
        HoldOrder order = dao().findById(id);
        if (order == null) {
            error(ctx, HttpStatus.NOT_FOUND, "api.hold.not_found");
            return null;
        }
        if (!canAccess(currentUser(ctx), order)) {
            error(ctx, HttpStatus.FORBIDDEN, "api.hold.forbidden");
            return null;
        }
        return order;
    }

    private static Integer parseId(Context ctx) {
        try {
            return Integer.valueOf(ctx.pathParam("id"));
        } catch (RuntimeException e) {
            error(ctx, HttpStatus.BAD_REQUEST, "api.common.invalid_id");
            return null;
        }
    }

    /**
     * @return 状态值，{@link #STATUS_ALL} 表示不过滤（{@code status=all}）；
     *         {@code null} 表示请求非法、已写入 400 响应。
     */
    private static Integer parseStatusFilter(Context ctx) {
        String raw = ctx.queryParam("status");
        if (raw == null || raw.isBlank()) {
            return STATUS_PENDING;
        }
        String normalized = raw.trim();
        if ("all".equalsIgnoreCase(normalized)) {
            return STATUS_ALL;
        }
        try {
            int status = Integer.parseInt(normalized);
            if (status == STATUS_PENDING || status == STATUS_RESUMED || status == STATUS_CANCELLED) {
                return status;
            }
        } catch (NumberFormatException ignored) {
            // 落到下面的统一 400
        }
        error(ctx, HttpStatus.BAD_REQUEST, "api.hold.status_invalid");
        return null;
    }

    private static void conflictNotPending(Context ctx, HoldOrder order) {
        ctx.status(HttpStatus.CONFLICT).json(Map.of(
            "success", false,
            "message", ApiMessages.text(ctx, "api.hold.not_pending", statusName(ctx, order.status)),
            "currentStatus", statusCode(order.status),
            "currentStatusName", statusName(ctx, order.status)));
    }

    private static String statusCode(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        switch (status) {
            case STATUS_PENDING:
                return "PENDING";
            case STATUS_RESUMED:
                return "RESUMED";
            case STATUS_CANCELLED:
                return "CANCELLED";
            default:
                return "UNKNOWN";
        }
    }

    private static String statusName(Context ctx, Integer status) {
        if (status == null) {
            return ApiMessages.enumName(ctx, "api.hold.status.unknown", "未知");
        }
        switch (status) {
            case STATUS_PENDING:
                return ApiMessages.enumName(ctx, "api.hold.status.pending", "挂单中");
            case STATUS_RESUMED:
                return ApiMessages.enumName(ctx, "api.hold.status.resumed", "已取单");
            case STATUS_CANCELLED:
                return ApiMessages.enumName(ctx, "api.hold.status.cancelled", "已取消");
            default:
                return ApiMessages.enumName(ctx, "api.hold.status.unknown", "未知");
        }
    }

    private static HoldOrderView toView(Context ctx, HoldOrder order, boolean includeItems) {
        List<CartItem> cart = includeItems
            ? HoldOrderCodec.parse(order.itemsJson, DAOFactory.getInstance().getProductDAO())
            : List.of();
        HoldOrderView view = new HoldOrderView();
        fillView(ctx, view, order, cart);
        return view;
    }

    private static void fillView(Context ctx, HoldOrderView view, HoldOrder order, List<CartItem> cart) {
        view.id = order.id;
        view.orderNumber = order.orderNumber;
        view.userId = order.userId;
        view.memberId = order.memberId;
        view.memberName = order.memberName;
        view.memberPhone = order.memberPhone;
        view.status = statusCode(order.status);
        view.statusName = statusName(ctx, order.status);
        view.totalAmount = order.totalAmount;
        view.discountAmount = order.discountAmount;
        view.finalAmount = order.finalAmount;
        view.itemCount = order.itemCount;
        view.holdDate = formatDate(order.holdDate);
        view.holdTime = formatTime(order.holdTime);
        view.notes = order.notes;
        view.items = new ArrayList<>();
        for (CartItem item : cart) {
            HoldItemView itemView = new HoldItemView();
            itemView.productId = item.product.id;
            itemView.name = item.product.name;
            itemView.barcode = item.product.barcode;
            itemView.unit = item.product.unit;
            itemView.quantity = item.quantity;
            itemView.currentPrice = item.product.price;
            itemView.subtotal = item.subtotal;
            view.items.add(itemView);
        }
    }

    private static List<Integer> missingProductIds(String itemsJson, List<CartItem> restored) {
        Set<Integer> available = new HashSet<>();
        for (CartItem item : restored) {
            available.add(item.product.id);
        }
        Set<Integer> missing = new LinkedHashSet<>();
        if (itemsJson != null) {
            Matcher matcher = ITEM_PRODUCT_ID.matcher(itemsJson);
            while (matcher.find()) {
                try {
                    int productId = Integer.parseInt(matcher.group(1));
                    if (!available.contains(productId)) {
                        missing.add(productId);
                    }
                } catch (NumberFormatException ignored) {
                    // 损坏的明细由 codec 负责跳过，这里只负责报告缺失商品
                }
            }
        }
        return new ArrayList<>(missing);
    }

    private static String formatDate(Date date) {
        return date == null ? null : date.toLocalDate().format(DateTimeFormats.DATE);
    }

    private static String formatTime(Time time) {
        return time == null ? null : time.toLocalTime().format(DateTimeFormats.TIME);
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

    public static class CreateHoldRequest {
        public List<HoldItemRequest> items;
        public Integer memberId;
        public String notes;
    }

    public static class HoldItemRequest {
        public Integer productId;
        public Integer quantity;
    }

    public static class HoldOrderView {
        public Integer id;
        public String orderNumber;
        public Integer userId;
        public Integer memberId;
        public String memberName;
        public String memberPhone;
        public String status;
        public String statusName;
        public BigDecimal totalAmount;
        public BigDecimal discountAmount;
        public BigDecimal finalAmount;
        public Integer itemCount;
        public String holdDate;
        public String holdTime;
        public String notes;
        public List<HoldItemView> items = new ArrayList<>();
    }

    public static class ResumeView extends HoldOrderView {
        public List<Integer> missingProductIds = new ArrayList<>();
    }

    public static class HoldItemView {
        public Integer productId;
        public String name;
        public String barcode;
        public String unit;
        public Integer quantity;
        public BigDecimal currentPrice;
        public BigDecimal subtotal;
    }
}
