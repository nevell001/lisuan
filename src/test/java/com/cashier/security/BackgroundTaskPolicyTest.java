package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 后台线程与调度纪律门禁（TD-032 / TD-033 / TD-035）。
 *
 * <p>这一批债务的共同形态是"**后台/定时任务没有纪律**"，每条都对应一种实际故障：</p>
 * <ul>
 *   <li>非 daemon 线程：卡住的 JDBC/网络调用让关窗后进程不退出（实测 8 处）；</li>
 *   <li>调度任务不兜异常：{@code ScheduledExecutorService} 的任务抛一次异常就被**永久取消**
 *       （通知静默停摆、备份再也不跑）；</li>
 *   <li>重复 {@code start()} 没有 running 守卫：每次登录/切语言都叠一个调度器（触屏切语言实测）；</li>
 *   <li>切视图不 {@code cleanup()}：时钟 Timeline 与状态栏静态监听继续持有旧界面；</li>
 *   <li>线程里直接写 Node：JavaFX 线程违规（打包向导 {@code appendText}）；</li>
 *   <li>查库失败被吞成业务结论：数据库故障显示成"请先开班"（{@code hasActiveShift}）。</li>
 * </ul>
 */
@DisplayName("后台线程与调度纪律门禁")
class BackgroundTaskPolicyTest {

    private static final Path MAIN = Path.of("src/main/java");

    @Test
    @DisplayName("所有 new Thread 都必须设 daemon（否则关窗后进程可能不退出）")
    void everyThreadIsDaemon() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaSources()) {
            String text = read(file);
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (!lines[i].contains("new Thread(")) {
                    continue;
                }
                Matcher named = Pattern.compile("Thread\\s+(\\w+)\\s*=\\s*new Thread\\(").matcher(lines[i]);
                if (named.find()) {
                    String var = named.group(1);
                    if (!Pattern.compile("\\b" + Pattern.quote(var) + "\\.setDaemon\\(true\\)").matcher(text).find()) {
                        violations.add(file + ":" + (i + 1) + " 变量 " + var + " 未设 daemon");
                    }
                } else {
                    String window = String.join("\n", java.util.Arrays.copyOfRange(
                        lines, i, Math.min(lines.length, i + 3)));
                    if (window.contains(".start()") && !window.contains("setDaemon")) {
                        violations.add(file + ":" + (i + 1) + " 内联线程且未设 daemon");
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(),
            "以下线程会让关窗后进程不退出（TD-035）：\n  " + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("定时任务体必须兜住异常（抛一次就被永久取消调度）")
    void scheduledTasksNeverThrow() throws IOException {
        for (String relative : new String[]{
            "com/cashier/notification/NotificationManager.java",
            "com/cashier/service/BackupService.java",
            "com/cashier/service/PaymentRefundReconcileService.java",
            "com/cashier/service/InventoryAlertService.java"}) {
            String source = read(MAIN.resolve(relative));
            assertTrue(source.contains("catch (Throwable") || source.contains("catch (Exception"),
                relative + " 的调度任务体必须兜住异常：抛一次异常会让后续调度被永久取消（TD-035）");
        }
    }

    @Test
    @DisplayName("可重复启动的服务必须有 running 守卫与 daemon 调度线程")
    void restartableServicesGuardAgainstDoubleStart() throws IOException {
        String backup = read(MAIN.resolve("com/cashier/service/BackupService.java"));
        assertTrue(backup.contains("isRunning"), "BackupService 必须有 isRunning 守卫（TD-033）：每次登录都会 start()");
        assertTrue(backup.contains("setDaemon(true)"), "自动备份调度线程必须是 daemon");
        assertTrue(backup.contains("已在运行中"), "重复 start() 必须被拦下并留日志");

        String reconcile = read(MAIN.resolve("com/cashier/service/PaymentRefundReconcileService.java"));
        assertTrue(reconcile.contains("if (isRunning)"), "退款对账同样要有守卫（同类缺陷）");
    }

    @Test
    @DisplayName("触屏切语言必须先 cleanup 再切视图（否则 Timeline/静态监听泄漏）")
    void touchLanguageSwitchCleansUpOldView() throws IOException {
        String source = read(MAIN.resolve("com/cashier/controller/TouchCartController.java"));
        int switchLanguage = source.indexOf("private void switchLanguage(String languageTag)");
        assertTrue(switchLanguage > 0);
        String body = source.substring(switchLanguage, Math.min(source.length(), switchLanguage + 2000));
        assertTrue(body.contains("cleanup();"),
            "切语言会重建整个触屏视图，必须先 cleanup()（TD-033）：时钟 Timeline 继续跑、"
                + "StatusBarManager 静态监听再叠一层，旧场景无法回收");
        assertTrue(body.indexOf("cleanup();") < body.indexOf("switchToPosModeView("),
            "cleanup() 必须在切换视图之前调用");
    }

    @Test
    @DisplayName("worker 线程不得直接写 JavaFX Node（打包向导 appendText）")
    void logAppendIsFxThreadSafe() throws IOException {
        String source = read(MAIN.resolve("com/cashier/packager/PackageWizardController.java"));
        int method = source.indexOf("private void appendLog(String message)");
        assertTrue(method > 0);
        String body = source.substring(method, Math.min(source.length(), method + 700));
        assertTrue(body.contains("isFxApplicationThread()") && body.contains("Platform.runLater"),
            "appendLog 会被 worker 线程调用，必须切回 FX 线程再写 TextArea（TD-035）");
    }

    @Test
    @DisplayName("查库失败不得被当成业务结论（班次状态三态）")
    void shiftFailureIsNotReportedAsNoShift() throws IOException {
        String dataService = read(MAIN.resolve("com/cashier/service/DataService.java"));
        assertTrue(dataService.contains("enum ActiveShiftState"),
            "班次查询要有三态（TD-035）：ACTIVE/NONE/UNKNOWN，查库失败不能等同于\"没有班次\"");
        assertTrue(dataService.contains("ActiveShiftState.UNKNOWN"),
            "SQLException 必须落到 UNKNOWN");

        String cart = read(MAIN.resolve("com/cashier/controller/CartController.java"));
        assertTrue(cart.contains("requireActiveShift()") && cart.contains("activeShiftBlockingMessageKey"),
            "结账/支付守卫要区分\"没有班次\"与\"查不到班次状态\"（提示 key 由服务层决定）");
        assertTrue(cart.contains("runtime.shift_state_unknown"),
            "数据库不可用时要提示系统错误，而不是\"请先开班\"");
    }

    private static List<Path> javaSources() throws IOException {
        try (var stream = Files.walk(MAIN)) {
            return stream.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}