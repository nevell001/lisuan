package com.cashier.api.middleware;

import com.cashier.model.User;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Map;

/**
 * REST API 角色授权中间件。
 *
 * 认证只证明调用者是谁；这里集中限制会改变系统配置、备份或资金状态的接口，
 * 避免各控制器自行检查时出现遗漏。
 */
public final class AuthorizationMiddleware {
    private static final String ADMIN = "admin";
    private static final String FINANCE = "finance";

    private AuthorizationMiddleware() {
    }

    public static void authorize(Context ctx) {
        User user = ctx.attribute("currentUser");
        if (user == null || !isAllowed(user.role, ctx.method().name(), ctx.path())) {
            ctx.status(HttpStatus.FORBIDDEN)
                .json(Map.of("success", false, "message", "权限不足"));
            ctx.skipRemainingHandlers();
        }
    }

    static boolean isAllowed(String role, String method, String path) {
        if (ADMIN.equals(role)) {
            return true;
        }

        String normalized = withoutTrailingSlash(path);

        if (isAdminOnlyPath(method, normalized)) {
            return false;
        }

        if (isFinanceOrAdminPath(method, normalized)) {
            return FINANCE.equals(role);
        }

        return true;
    }

    /**
     * 判定角色前先去掉末尾斜杠。
     *
     * <p>Javalin 6 默认 {@code ignoreTrailingSlashes = true}：{@code PUT /api/members/1/} 照样命中
     * 路由 {@code /api/members/{id}}，而 {@code ctx.path()} 返回的是**原始 URI**（带斜杠）。
     * 本类的门禁用 {@code equals}/{@code matches} 做全串比对，多一个斜杠就全部落空 →
     * 整条角色门禁被跳过：收银员即可退款、改会员折扣/等级/手机号、改开票方信息与支付配置
     * （2026-09 审计用真实 Javalin 6.1.3 复现的越权，不要删掉这个归一化）。</p>
     */
    static String withoutTrailingSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        int end = path.length();
        while (end > 1 && path.charAt(end - 1) == '/') {
            end--;
        }
        return path.substring(0, end);
    }

    private static boolean isAdminOnlyPath(String method, String path) {
        return path.startsWith("/api/users")
            || path.startsWith("/api/settings")
            || path.startsWith("/api/backup")
            || path.equals("/api/payment/config")
            // 开票方名称/税号/银行账号会写进之后所有发票，属全局配置，仅管理员可改
            || (path.equals("/api/invoices/seller-info") && isMutating(method))
            || (path.startsWith("/api/products") && isMutating(method))
            || (path.startsWith("/api/inventory") && isMutating(method))
            || (path.startsWith("/api/printers/") && isMutating(method)
                && !path.matches("/api/printers/[^/]+/(receipt|invoice/[^/]+)"));
    }

    private static boolean isFinanceOrAdminPath(String method, String path) {
        return path.matches("/api/transactions/[^/]+/refund")
            || path.matches("/api/payment/[^/]+/refund")
            || path.matches("/api/invoices/[^/]+/void")
            || path.equals("/api/invoices/manual")
            // 会员充值**不在此列**：充值是收银台日常操作（顾客当面充卡），桌面端一直对收银员开放，
            // 且充值会写 RechargeRecord 流水留痕（TD-018：按"收银员可充值"统一两侧口径）。
            // 注意金额类的"改折扣/等级/积分/余额"仍限 finance/admin（上面那条 /api/members/[^/]+ 写方法）。
            || (path.matches("/api/members/[^/]+") && isMutating(method))
            // 资金统计与退款单列表属财务口径，收银员无需查看
            || path.equals("/api/payment/refunds")
            || path.startsWith("/api/payment/stats")
            || (path.startsWith("/api/reports/") && "GET".equals(method));
    }

    private static boolean isMutating(String method) {
        return "POST".equals(method) || "PUT".equals(method)
            || "PATCH".equals(method) || "DELETE".equals(method);
    }
}
