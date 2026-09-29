package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录界面的"状态必须能恢复"门禁（TD-030，2026-09 审计发现）。
 *
 * <p>现象：`handleLogin` 在异步校验前先 `setLoginState(true)`（禁用用户名/密码框 + 显示转圈）。
 * 首次登录用户会被弹"必须改密"对话框，而 {@code showPasswordChangeDialog} 只有
 * "改密成功且切到主界面"这一条出路——按 **取消** 或关窗时 `showAndWait()` 返回空，
 * `ifPresent(...)` 什么也不做；改密抛异常时也只弹了个错误。两种情况都停在
 * "输入框是灰的、转圈还在转"的登录页，只能杀进程。</p>
 *
 * <p>门禁钉住：该对话框方法必须在 `showAndWait()` **之后**无条件恢复登录状态，
 * 且外层 catch 里也要恢复。</p>
 */
@DisplayName("登录界面状态恢复门禁")
class LoginStateRecoveryPolicyTest {

    private static final Path LOGIN_CONTROLLER =
        Path.of("src/main/java/com/cashier/controller/LoginController.java");

    @Test
    @DisplayName("改密对话框关闭后必须恢复登录界面（取消/失败都不能把输入框永久锁死）")
    void passwordChangeDialogRestoresLoginState() throws Exception {
        String source = Files.readString(LOGIN_CONTROLLER);
        String body = methodBody(source, "private void showPasswordChangeDialog(");

        int dialogIndex = body.indexOf("showAndWait()");
        assertTrue(dialogIndex >= 0, "改密对话框方法里应能找到 showAndWait()");

        int firstRestore = body.indexOf("setLoginState(false)", dialogIndex);
        assertTrue(firstRestore > dialogIndex,
            "showAndWait() 返回之后必须调用 setLoginState(false)："
                + "取消/关窗时 Optional 为空，不恢复就会留下'输入框禁用 + 转圈不停'的登录页，"
                + "用户只能杀进程");

        int restoreCount = 0;
        for (int i = body.indexOf("setLoginState(false)"); i >= 0; i = body.indexOf("setLoginState(false)", i + 1)) {
            restoreCount++;
        }
        assertTrue(restoreCount >= 2,
            "改密失败（内层 catch）与对话框本身出错（外层 catch）都应恢复登录状态，"
                + "当前只有 " + restoreCount + " 处 setLoginState(false)");

        // 恢复必须挂在"没有切到主界面"这一支上：只用 switchedToMain 记录结果，
        // 并断言用的是取反分支——写成 if (switchedToMain) 等于"取消时才恢复、成功时不恢复"，
        // 语义完全反了（第一版门禁只数了调用次数，变异测试正是这样抓出漏检的）
        assertTrue(body.contains("boolean switchedToMain = false;"),
            "改密对话框应用一个默认 false 的标记记录是否真的切到了主界面");
        assertTrue(body.contains("if (!switchedToMain)"),
            "必须在 if (!switchedToMain) 分支里恢复登录状态：取消/关窗/改密失败时 switchedToMain 仍为 false，"
                + "这正是此前会卡死的路径");

        // 反空转：确认这条不变量确实对应 handleLogin 里的 setLoginState(true)
        assertTrue(source.contains("setLoginState(true)"),
            "handleLogin 应当仍然在异步校验前禁用登录控件（否则本门禁已失去前提，需要重新审视）");
    }

    /** 取方法体（按花括号配对）。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法签名: " + signature);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new AssertionError("方法体不闭合: " + signature);
    }
}
