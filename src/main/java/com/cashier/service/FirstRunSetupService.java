package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.model.User;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.PasswordUtil;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.Date;

/**
 * 首次运行建号逻辑。
 *
 * <p>空库启动时不再由应用自动生成随机初始密码（javaw/双击启动看不到控制台输出，
 * 等于制造了一个谁也登不进去的账号），也不再依赖 SQL 种子里的 admin/admin123；
 * 首个管理员的用户名和密码一律由 {@code FirstRunSetupDialog} 在启动时向用户收集。</p>
 */
public final class FirstRunSetupService {

    /** 向导里预填的管理员用户名，用户可改 */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    private static final Logger logger = LoggerFactoryUtil.getLogger(FirstRunSetupService.class);

    private FirstRunSetupService() {
    }

    /** 用户表里一个账号都没有 → 需要先走首次运行向导 */
    public static boolean needsFirstRunSetup() throws SQLException {
        return DAOFactory.getInstance().getUserDAO().count() == 0;
    }

    /**
     * 用向导收集的信息创建首个管理员账号。
     *
     * @throws IllegalArgumentException 用户名/密码为空
     * @throws SQLException             用户名已存在或写入失败
     */
    public static User createAdministrator(String username, String password, String displayName) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        String trimmedUsername = username.trim();
        var userDAO = DAOFactory.getInstance().getUserDAO();
        if (userDAO.exists(trimmedUsername)) {
            throw new SQLException("用户名 " + trimmedUsername + " 已存在");
        }

        User admin = new User();
        admin.username = trimmedUsername;
        admin.password = PasswordUtil.hashPassword(password);
        admin.name = (displayName == null || displayName.isBlank()) ? trimmedUsername : displayName.trim();
        admin.role = "admin";
        admin.active = true;
        admin.createTime = new Date();
        admin.forcePasswordChange = false;

        // insert 不写 force_password_change 列：密码本来就是用户自设的，没有"改密"的必要
        if (!userDAO.insert(admin)) {
            throw new SQLException("写入 users 表失败");
        }

        AuditService.success(admin.username, "AUTH", "FIRST_RUN_SETUP", "首次运行向导创建管理员账号", 1);
        logger.info("首次运行向导：管理员账号 {} 创建成功", admin.username);
        return admin;
    }
}
