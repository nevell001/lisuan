package com.cashier.service;

import com.cashier.dao.UserDAORefactored;
import com.cashier.model.User;

import java.sql.SQLException;
import java.util.Set;

/**
 * 用户角色与"最后管理员"保护规则。
 *
 * <p>角色只允许 {@code admin}/{@code cashier}/{@code finance}：REST 建号/改号时此前可以把任意字符串
 * 写进 role，未知角色没有任何权限，等于造出一个登得进去却什么都干不了的账号。</p>
 *
 * <p>"最后一个启用管理员"不得被删除、降级或禁用：否则系统再没有人能管理用户与设置。</p>
 */
public final class UserPolicy {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_CASHIER = "cashier";
    public static final String ROLE_FINANCE = "finance";
    public static final String DEFAULT_ROLE = ROLE_CASHIER;

    public static final Set<String> ROLES = Set.of(ROLE_ADMIN, ROLE_CASHIER, ROLE_FINANCE);

    public static boolean isValidRole(String role) {
        return role != null && ROLES.contains(role);
    }

    public static boolean isActiveAdmin(User user) {
        return user != null && user.active && ROLE_ADMIN.equals(user.role);
    }

    /**
     * 对 {@code current} 执行"改为 role=...、active=..."后，是否会抹掉最后一个启用管理员。
     *
     * <p>注意是"查完再审写"的乐观判定：两个管理员同时互降级仍可能都通过。本批只做最小防护，
     * 真要强一致需要把判定和 UPDATE 放同一事务并锁 users 表。</p>
     */
    public static boolean wouldRemoveLastActiveAdmin(
            User current, String newRole, boolean newActive, UserDAORefactored dao) throws SQLException {
        if (!isActiveAdmin(current)) {
            return false;
        }
        if (ROLE_ADMIN.equals(newRole) && newActive) {
            return false;
        }
        return dao.countActiveAdmins() <= 1;
    }

    private UserPolicy() {
    }
}
