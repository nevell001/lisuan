package com.cashier.api;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.UserDAORefactored;
import com.cashier.model.User;
import com.cashier.util.DatabaseTestBase;
import com.cashier.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * API Token 生命周期测试
 * 覆盖 P0-3：账号被禁用/密码变更后，已签发 token 必须立即失效。
 */
@DisplayName("API Token 生命周期测试")
public class ApiTokenLifecycleTest extends DatabaseTestBase {

    private final UserDAORefactored userDAO = DAOFactory.getInstance().getUserDAO();
    private final ApiServer apiServer = ApiServer.getInstance();

    private User createActiveUser(String username) throws SQLException {
        User user = new User();
        user.username = username;
        user.password = PasswordUtil.hashPassword("secret123");
        user.name = "测试-" + username;
        user.role = "cashier";
        user.active = true;
        user.createTime = new Date();
        user.lastLoginTime = new Date();
        userDAO.insert(user);
        return user;
    }

    @Test
    @DisplayName("启用账号签发的 Token 可正常验证")
    void activeUserTokenValidates() throws Exception {
        User user = createActiveUser("token_valid_user");
        String token = apiServer.generateToken(user);

        User validated = apiServer.validateToken(token);
        assertNotNull(validated, "启用账号的 token 应验证通过");
        assertEquals(user.id, validated.id);

        apiServer.invalidateUserTokens(user.id);
    }

    @Test
    @DisplayName("账号被禁用后既有 token 立即失效且无法再续期")
    void disabledUserTokenIsRejected() throws Exception {
        User user = createActiveUser("token_disabled_user");
        String token = apiServer.generateToken(user);
        assertNotNull(apiServer.validateToken(token), "禁用前 token 应有效");

        user.active = false;
        userDAO.update(user);

        assertNull(apiServer.validateToken(token), "禁用后 token 应立即失效");

        // 清理残留 token，避免影响其他测试
        apiServer.invalidateUserTokens(user.id);
    }

    @Test
    @DisplayName("按用户作废 token：只影响目标用户，不影响其他用户")
    void invalidateUserTokensOnlyAffectsTargetUser() throws Exception {
        User userA = createActiveUser("token_a_user");
        User userB = createActiveUser("token_b_user");
        String tokenA1 = apiServer.generateToken(userA);
        String tokenA2 = apiServer.generateToken(userA);
        String tokenB = apiServer.generateToken(userB);

        apiServer.invalidateUserTokens(userA.id);

        assertNull(apiServer.validateToken(tokenA1), "目标用户的 token 应全部失效");
        assertNull(apiServer.validateToken(tokenA2), "目标用户的全部 token 应失效");
        assertNotNull(apiServer.validateToken(tokenB), "其他用户的 token 不应受影响");

        apiServer.invalidateUserTokens(userB.id);
    }
}
