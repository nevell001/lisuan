package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 首次运行向导门禁（回归）。
 *
 * <p>历史问题：空库启动时应用自动生成随机 16 位临时密码打印到控制台
 * （javaw/双击启动看不到控制台，等于制造了一个谁也登不进去的账号），
 * SQL 种子里则是公开的默认弱口令。现在的契约是：空库首次启动必须由
 * {@code FirstRunSetupDialog} 向用户收集管理员账号与密码，且仓库里
 * 不得再出现默认口令、其 BCrypt 哈希或向 users 表播种账号的 SQL。</p>
 */
@DisplayName("首次运行向导门禁")
class FirstRunSetupPolicyTest {

    private static final Path MAIN = Path.of("src/main/java/com/cashier");
    private static final Path APP = MAIN.resolve("CashierSystemFXApplication.java");
    private static final Path DIALOG = MAIN.resolve("FirstRunSetupDialog.java");
    private static final Path SERVICE = MAIN.resolve("service/FirstRunSetupService.java");
    private static final Path DATABASE_MANAGER = MAIN.resolve("util/DatabaseManager.java");

    /** 历史默认口令与其 BCrypt 哈希前缀：出现在任何扫描目标里都是回归。 */
    private static final String LEAKED_PASSWORD = "admin123";
    private static final String LEAKED_HASH_PREFIX = "$2a$10$EVvVqIyQ";

    /** 建表脚本里向 users 表播种账号（INSERT，可带 IGNORE，表名可带反引号）。 */
    private static final Pattern SEED_USERS =
        Pattern.compile("(?i)insert\\s+(?:ignore\\s+)?into\\s+`?users`?");

    private static final List<String> FIRST_RUN_KEYS = List.of(
        "firstrun.title", "firstrun.header", "firstrun.username", "firstrun.username_hint",
        "firstrun.display_name", "firstrun.display_name_hint", "firstrun.password",
        "firstrun.confirm_password", "firstrun.password_policy", "firstrun.password_policy_complexity",
        "firstrun.create", "firstrun.exit", "firstrun.create_failed",
        "firstrun.created_title", "firstrun.created_message");

    @Test
    @DisplayName("启动时不得再自动建号/打印临时密码（DatabaseManager 的旧路径已删净）")
    void databaseManagerNoLongerCreatesUsers() throws Exception {
        String source = Files.readString(DATABASE_MANAGER);

        for (String oldPath : List.of("createDefaultAdminUser", "printInitialAdminPassword",
            "generateRandomPassword", "isDatabasePopulated")) {
            assertFalse(source.contains(oldPath),
                "DatabaseManager 不得再有 " + oldPath + "：空库建号必须走首次运行向导，"
                    + "随机临时密码在 javaw 启动下用户根本看不到");
        }
    }

    @Test
    @DisplayName("仓库里不得再出现默认口令、其哈希或 users 种子 SQL")
    void noDefaultCredentialsAnywhere() throws Exception {
        StringBuilder violations = new StringBuilder();

        try (Stream<Path> sqls = Files.list(Path.of("docker/mysql-init"))) {
            for (Path sql : sqls.filter(p -> p.getFileName().toString().endsWith(".sql")).toList()) {
                String text = Files.readString(sql);
                if (containsLeaked(text)) {
                    violations.append(sql).append(": 含历史默认口令或哈希; ");
                }
                if (SEED_USERS.matcher(text).find()) {
                    violations.append(sql).append(": 向 users 表播种账号（空库建号由首次运行向导负责）; ");
                }
            }
        }

        for (String script : List.of("install.sh", "install.bat")) {
            String text = Files.readString(Path.of(script)).toLowerCase(Locale.ROOT);
            if (text.contains(LEAKED_PASSWORD)) {
                violations.append(script).append(": 含历史默认口令; ");
            }
        }

        // Java 源码剥离注释后再扫：注释里保留"历史默认口令已废弃"的说明是允许的
        try (Stream<Path> javaFiles = Files.walk(MAIN)) {
            for (Path java : javaFiles.filter(p -> p.toString().endsWith(".java")).toList()) {
                String code = stripComments(Files.readString(java));
                if (containsLeaked(code)) {
                    violations.append(java).append(": 代码里含历史默认口令或哈希; ");
                }
            }
        }

        assertTrue(violations.isEmpty(),
            "以下位置出现默认凭据（应改为由首次运行向导收集）: " + violations);
    }

    @Test
    @DisplayName("启动流程必须检测空库并调起首次运行向导")
    void startupWiresTheFirstRunWizard() throws Exception {
        String app = Files.readString(APP);

        String dbPhase = methodBody(app, "private StartupDatabase initializeDatabasePhase(");
        assertTrue(dbPhase.contains("FirstRunSetupService.needsFirstRunSetup()"),
            "数据库阶段（后台线程）应检测是否为空库，结果随 StartupDatabase 带回 FX 线程");

        assertTrue(app.contains("private record StartupDatabase(String languageTag, boolean needsFirstRunSetup)"),
            "空库标记应通过不可变结果对象传给启动收口");

        assertTrue(app.contains("Platform.runLater(this::showFirstRunSetup)"),
            "向导必须在启动画面关闭后、事件循环空闲时再弹（showAndWait 不能卡住启动收尾）");

        String wizard = methodBody(app, "private void showFirstRunSetup()");
        assertTrue(wizard.contains("FirstRunSetupDialog.showAndWait("),
            "showFirstRunSetup 必须调起向导");
        assertTrue(wizard.contains("switchToMainView("),
            "向导创建成功即视为完成登录，直接进入主界面");
        assertTrue(wizard.contains("exitApplication()"),
            "用户取消向导时一个账号都没有、无路可登，必须退出而不是停在登录页");
    }

    @Test
    @DisplayName("向导必须以密码框收密码，且口令只经 BCrypt 落库")
    void wizardCollectsPasswordSecurely() throws Exception {
        String dialog = Files.readString(DIALOG);
        String service = Files.readString(SERVICE);

        assertTrue(dialog.contains("new PasswordField()"),
            "管理员密码必须用 PasswordField 收集（明文 TextField 会直接露在屏幕上）");
        assertFalse(dialog.contains("System.out"),
            "向导不得把任何输入打印到控制台");
        assertFalse(service.contains("System.out"),
            "建号服务不得把口令打印到控制台（这就是随机临时密码方案的老毛病）");
        assertTrue(service.contains("PasswordUtil.hashPassword("),
            "口令必须经 BCrypt 落库");
    }

    @Test
    @DisplayName("firstrun.* 文案键在全部四个 bundle 里都存在")
    void wizardKeysExistInAllBundles() throws Exception {
        Path i18nDir = Path.of("src/main/resources/com/cashier/i18n");
        List<String> bundles = List.of(
            "messages.properties", "messages_zh_CN.properties",
            "messages_zh_TW.properties", "messages_en.properties");

        StringBuilder missing = new StringBuilder();
        for (String bundle : bundles) {
            String text = Files.readString(i18nDir.resolve(bundle));
            for (String key : FIRST_RUN_KEYS) {
                if (!text.contains("\n" + key + "=") && !text.startsWith(key + "=")) {
                    missing.append(bundle).append(" 缺 ").append(key).append("; ");
                }
            }
        }
        assertTrue(missing.isEmpty(), "首次运行向导文案不全: " + missing);
    }

    private static boolean containsLeaked(String text) {
        return text.toLowerCase(Locale.ROOT).contains(LEAKED_PASSWORD) || text.contains(LEAKED_HASH_PREFIX);
    }

    /** 剥离块注释与行注释：注释里保留历史说明（"不再依赖 admin123 种子"）不应触发误报。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    /** 取出指定方法（按大括号配对）的方法体，便于断言启动接线是否仍在。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法签名: " + signature);
        int open = source.indexOf('{', start + signature.length());
        assertTrue(open >= 0, "方法没有方法体: " + signature);
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
        throw new IllegalStateException("方法体未闭合: " + signature);
    }
}
