package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 建号路径不得产生"空口令账号"（2026-10 审计 F7）。
 *
 * <p>为什么需要这条门禁：{@code PasswordUtil.hashPassword("")} 会写下一个**有效**凭据——
 * {@code verifyPassword("", hash)} 返回 true（{@code PasswordUtilTest.testEmptyPassword} 就是这个断言）。
 * 而 REST 登录此前只挡 {@code null}，桌面登录页会在本地拦下空口令，于是"口令留空建出来的账号"
 * 在桌面上看着像没配好，却能直接用空口令通过 API 登录（role=admin 即管理员会话）。</p>
 *
 * <p>行为侧已由 {@code UserApiControllerTest.createWithEmptyPasswordRejected} 与
 * {@code AuthControllerTest.loginWithEmptyPasswordReturns400} 守住；这里钉住**桌面新建用户对话框**
 * 这条无法在无头环境直接跑的路径：它的校验只能写在 JavaFX 监听器里，回归了也不会有测试变红。</p>
 */
@DisplayName("建号口令门禁")
class UserCreationPasswordPolicyTest {

    private static final String USER_CONTROLLER = "src/main/java/com/cashier/controller/UserController.java";

    @Test
    @DisplayName("桌面新建用户：口令为空不得哈希、OK 按钮必须被禁用；编辑用户留空仍表示不改口令")
    void desktopNewUserCannotBeCreatedWithoutPassword() throws Exception {
        String source = Files.readString(Path.of(USER_CONTROLLER));

        // 新建用户 + 空口令 → 直接不产生结果（fail-closed），绝不写入空口令哈希
        assertTrue(source.contains("existingUser == null && passwordInput.isEmpty()"),
            "新建用户口令为空时必须拒绝提交（不得再哈希空串）");
        assertFalse(source.contains("existingUser == null || !passwordInput.isEmpty()"),
            "旧写法会把空口令哈希成有效凭据，必须已被替换");

        // 校验要落到 OK 按钮上，并同时盯住用户名与口令两个输入框
        assertTrue(source.contains("okButton.setDisable("),
            "口令/用户名校验结果必须禁用 OK 按钮");
        assertTrue(source.contains("passwordField.textProperty().addListener"),
            "必须在口令输入框上注册校验监听");
        assertTrue(source.contains("usernameField.textProperty().addListener"),
            "必须在用户名输入框上注册校验监听");
    }
}
