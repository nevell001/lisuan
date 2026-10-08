package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.model.User;
import com.cashier.util.DatabaseTestBase;
import com.cashier.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("首次运行建号逻辑测试")
class FirstRunSetupServiceTest extends DatabaseTestBase {

    private static final String PASSWORD = "Boss2026";

    @Test
    @DisplayName("空用户表：needsFirstRunSetup 为 true")
    void emptyUserTableNeedsFirstRunSetup() throws SQLException {
        assertTrue(FirstRunSetupService.needsFirstRunSetup());
    }

    @Test
    @DisplayName("建号后空库判定消失")
    void creatingAdministratorEndsFirstRunState() throws SQLException {
        FirstRunSetupService.createAdministrator("boss", PASSWORD, "老板");

        assertFalse(FirstRunSetupService.needsFirstRunSetup());
        assertEquals(1, DAOFactory.getInstance().getUserDAO().count());
    }

    @Test
    @DisplayName("建出的账号是可用管理员：admin 角色 / 启用 / 不强制改密 / BCrypt 存储")
    void createdAdministratorIsUsableForLogin() throws SQLException {
        User admin = FirstRunSetupService.createAdministrator("boss", PASSWORD, "老板");

        assertTrue(admin.id > 0);
        User saved = DAOFactory.getInstance().getUserDAO().findByUsername("boss");
        assertNotNull(saved);
        assertEquals("admin", saved.role);
        assertTrue(saved.active);
        assertFalse(saved.forcePasswordChange);
        assertNotEquals(PASSWORD, saved.password);
        assertTrue(PasswordUtil.verifyPassword(PASSWORD, saved.password));
        assertFalse(PasswordUtil.verifyPassword("WrongPass1", saved.password));
    }

    @Test
    @DisplayName("显示名留空时取用户名")
    void blankDisplayNameFallsBackToUsername() throws SQLException {
        User admin = FirstRunSetupService.createAdministrator("boss", PASSWORD, "   ");

        assertEquals("boss", admin.name);
    }

    @Test
    @DisplayName("用户名两侧空白被裁掉")
    void usernameIsTrimmed() throws SQLException {
        User admin = FirstRunSetupService.createAdministrator("  boss  ", PASSWORD, null);

        assertEquals("boss", admin.username);
        assertNotNull(DAOFactory.getInstance().getUserDAO().findByUsername("boss"));
    }

    @Test
    @DisplayName("用户名重复：抛 SQLException（向导留在原地让用户改名重试）")
    void duplicateUsernameIsRejected() throws SQLException {
        FirstRunSetupService.createAdministrator("boss", PASSWORD, null);

        SQLException duplicate = assertThrows(SQLException.class,
            () -> FirstRunSetupService.createAdministrator("boss", "Other2026", null));
        assertTrue(duplicate.getMessage().contains("boss"));
    }

    @Test
    @DisplayName("用户名/密码为空：抛 IllegalArgumentException，不落库")
    void blankUsernameOrPasswordIsRejected() throws SQLException {
        assertThrows(IllegalArgumentException.class,
            () -> FirstRunSetupService.createAdministrator("   ", PASSWORD, null));
        assertThrows(IllegalArgumentException.class,
            () -> FirstRunSetupService.createAdministrator("boss", "", null));

        assertEquals(0, DAOFactory.getInstance().getUserDAO().count());
    }
}
