package com.cashier.security;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 明文凭据门禁（回归）。
 *
 * <p>历史上 <code>config/database.properties</code>、<code>docker/docker-init.sh</code>、
 * <code>install.sh</code> 等文件提交过明文数据库口令（后来删除，但仍留在 git 历史里）。
 * 本测试扫描 <b>git 跟踪</b>的文件，阻止同类写法再次进入仓库：</p>
 *
 * <ul>
 *   <li>properties/.env：凭据键（password/secret/token.secret/api.key/private.key 等）
 *       不得带非占位符的字面量值；</li>
 *   <li>脚本/yml：不得把带引号的字面量赋给凭据变量，也不得写成
 *       <code>${VAR:-"字面量"}</code> 的硬编码默认值。</li>
 * </ul>
 *
 * <p>只扫 git 跟踪的文件：本地 <code>.env</code>、<code>config/*.properties</code>
 * 本就该被 gitignore，其中存放真实口令是预期行为。</p>
 */
@DisplayName("明文凭据门禁")
class SecretHygienePolicyTest {

    private static final Set<String> SKIP_EXT = Set.of(
        ".ttc", ".ttf", ".otf", ".png", ".jpg", ".jpeg", ".gif", ".ico", ".icns",
        ".svg", ".jar", ".zip", ".wav", ".pdf");

    /** 凭据键名的后缀（小写比较）。注意不能是 "token" 通配：token.expire.hours 不是凭据。 */
    private static final Pattern CRED_KEY = Pattern.compile(
        "(password|passwd|secret|token\\.secret|api\\.key|api_key|apikey|private\\.key|private_key)$");

    private static final Pattern PROP_LINE = Pattern.compile("^\\s*([A-Za-z0-9_.]+)\\s*[=:]\\s*(.*)$");

    /** 脚本里把带引号的字面量直接赋给凭据变量。 */
    private static final Pattern SCRIPT_ASSIGN = Pattern.compile(
        "(?i)\\b[A-Z0-9_]*(?:PASSWORD|PASSWD|SECRET|TOKEN)[A-Z0-9_]*\\s*=\\s*[\"']([A-Za-z0-9!@#$%^&*_.\\-]{4,})[\"']");

    /** 脚本里 <code>${VAR:-"字面量"}</code> 形式的硬编码默认口令。 */
    private static final Pattern SCRIPT_DEFAULT = Pattern.compile(
        "(?i)\\$\\{[A-Z0-9_]*(?:PASSWORD|PASSWD|SECRET)[A-Z0-9_]*:-\\s*[\"']([^\"']{4,})[\"']");

    private static final List<String> PLACEHOLDER_PREFIXES = List.of(
        "change_me", "change-me", "changeme", "your_", "your-", "xxx", "replace",
        "redacted", "placeholder", "sample", "dummy", "exam", "todo", "<", "***", "...");

    private static final Set<String> PLACEHOLDER_VALUES = Set.of(
        "none", "null", "true", "false", "password", "passwd", "secret", "token", "0", "1");

    @Test
    @DisplayName("git 跟踪的配置与脚本中不得出现明文凭据")
    void noPlaintextCredentialsInTrackedFiles() throws Exception {
        List<String> violations = new ArrayList<>();

        for (String rel : trackedFiles()) {
            Path path = Path.of(rel);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            String lowerRel = rel.toLowerCase(Locale.ROOT);
            if (lowerRel.contains("/i18n/") || lowerRel.contains("messages_")) {
                continue; // 语言包里出现 "password" 只是界面文案
            }
            String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
            int dot = fileName.lastIndexOf('.');
            String ext = dot >= 0 ? fileName.substring(dot) : "";
            if (SKIP_EXT.contains(ext)) {
                continue;
            }

            List<String> lines;
            try {
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                continue; // 非 UTF-8 文本（二进制资源）跳过
            }
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || trimmed.startsWith("//") || trimmed.startsWith("--")) {
                    continue;
                }
                if (isPropertiesLike(fileName) && isCredentialKeyLine(line)) {
                    violations.add(rel + ":" + (i + 1) + " [properties 明文凭据]");
                }
                if (Set.of(".sh", ".bat", ".cmd", ".ps1", ".yml", ".yaml").contains(ext)
                        && scriptHasLiteralCredential(line)) {
                    violations.add(rel + ":" + (i + 1) + " [脚本硬编码凭据]");
                }
            }
        }

        assertTrue(violations.isEmpty(),
            "发现疑似明文凭据，请改为从环境变量或 .env 读取：\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("扫描规则本身能识别历史泄露写法，且不误报占位符")
    void rulesDetectHistoricalShapes() {
        // 历史上真实出现过的三种形态
        assertTrue(isCredentialKeyLine("db.password=Abc123!xyz"));
        assertTrue(scriptHasLiteralCredential("DB_PASSWORD=\"Abc123!xyz\""));
        assertTrue(scriptHasLiteralCredential("DB_PASSWORD=${MYSQL_PASSWORD:-\"Abc123!xyz\"}"));

        // 模板文件必须按 properties 规则扫描（扩展名是 .example，不能只看后缀）
        assertTrue(isPropertiesLike("database.properties.example"));
        assertTrue(isPropertiesLike(".env.example"));
        assertFalse(isPropertiesLike("docker-compose.yml"));

        // 占位符/变量引用/检测用 grep 不得误报
        assertFalse(isCredentialKeyLine("db.password="));
        assertFalse(isCredentialKeyLine("db.password=CHANGE_ME_PWD"));
        assertFalse(isCredentialKeyLine("db.password=${CASHIER_DB_PASSWORD}"));
        assertFalse(isCredentialKeyLine("token.expire.hours=24"));        assertFalse(scriptHasLiteralCredential("export MYSQL_ROOT_PASSWORD=your_secure_password"));
        assertFalse(scriptHasLiteralCredential("MYSQL_PASSWORD_VALUE=$(grep -E \"^MYSQL_PASSWORD=\" .env)"));
        assertFalse(scriptHasLiteralCredential("grep -r -E \"Pwd123!|db.password=.+[^[:space:]]\" config/"));
    }

    /**
     * 是否按 properties/env 规则扫描。
     *
     * <p>注意不能只看扩展名：跟踪进仓库的模板是
     * <code>config/database.properties.example</code>，扩展名是 <code>.example</code>。</p>
     */
    static boolean isPropertiesLike(String fileName) {
        return fileName.startsWith(".env") || fileName.contains(".properties");
    }

    /** properties/.env 行：凭据键 + 非占位符值。 */
    static boolean isCredentialKeyLine(String line) {
        Matcher m = PROP_LINE.matcher(line);
        if (!m.matches()) {
            return false;
        }
        String key = m.group(1).toLowerCase(Locale.ROOT);
        return CRED_KEY.matcher(key).find() && !isPlaceholder(m.group(2));
    }

    /** 脚本行：带引号字面量赋值，或 <code>${VAR:-"字面量"}</code> 默认值。 */
    static boolean scriptHasLiteralCredential(String line) {
        Matcher assign = SCRIPT_ASSIGN.matcher(line);
        if (assign.find() && !isPlaceholder(assign.group(1))) {
            return true;
        }
        Matcher fallback = SCRIPT_DEFAULT.matcher(line);
        return fallback.find() && !isPlaceholder(fallback.group(1));
    }

    static boolean isPlaceholder(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' || first == '\'') && last == first) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }
        if (value.isEmpty() || value.indexOf('$') >= 0 || value.indexOf('%') >= 0) {
            return true; // 变量引用 / 环境变量展开
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (PLACEHOLDER_VALUES.contains(lower)) {
            return true;
        }
        for (String prefix : PLACEHOLDER_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** 用 git ls-files 取跟踪文件；不在 git 仓库里时跳过（例如源码包解压后运行）。 */
    private static List<String> trackedFiles() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "ls-files");
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        List<String> files;
        try (var reader = proc.inputReader(StandardCharsets.UTF_8)) {
            files = reader.lines().collect(Collectors.toList());
        }
        boolean finished = proc.waitFor(60, TimeUnit.SECONDS);
        Assumptions.assumeTrue(finished && proc.exitValue() == 0 && !files.isEmpty(),
            "需要 git 仓库才能扫描跟踪文件");
        return files;
    }
}
