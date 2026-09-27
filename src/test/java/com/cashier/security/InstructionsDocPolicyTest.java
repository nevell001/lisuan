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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 两份"当前事实"说明文档必须可信（TD-014）。
 *
 * <p>{@code AGENTS.md}（速查）与 {@code CLAUDE.md}（权威细节）是人和 agent 都会读的指令文档。
 * 它们曾经漂移过：{@code AGENTS.md} 长期被 gitignore（只存在于某一台机器上），
 * 文档里写过 {@code 589/606/647} 之类的测试数量、以及过时的"语言包有三份"。
 * 本门禁只钉住**最常漂移且可自动判定**的三件事：</p>
 * <ol>
 *   <li>两份文档都必须被 git 跟踪（否则换台机器就丢）；</li>
 *   <li>两份文档列出的**版本号同步来源**必须一致（版本升级时最容易只改一处）；</li>
 *   <li>两份文档**不得写死测试数量**——数量每次加测试都会变，应指向构建输出的
 *       {@code Tests run:}（README 的历史更新日志不受此限，那里的数字是史实）。</li>
 * </ol>
 */
@DisplayName("说明文档一致性门禁")
class InstructionsDocPolicyTest {

    private static final Path CLAUDE = Path.of("CLAUDE.md");
    private static final Path AGENTS = Path.of("AGENTS.md");

    /** 版本号的四处来源（用文档里的短名——CLAUDE.md 的同步句写的是 `AppConstants` 而非全名）。 */
    private static final List<String> VERSION_SOURCES = List.of(
        "AppConstants", "pom.xml", "installer/Installer.java", ".env.example");

    /** 测试数量的硬编码：`380 个测试`、`647 tests`、`Tests run: 731` 之类。 */
    private static final Pattern HARDCODED_COUNT = Pattern.compile(
        "\\d{2,}\\s*(个)?\\s*(测试用例|测试|用例|tests?|test cases)|Tests run:\\s*\\d+");

    @Test
    @DisplayName("CLAUDE.md 与 AGENTS.md 都必须被 git 跟踪")
    void instructionDocsAreTracked() throws Exception {
        List<String> tracked = trackedFiles();
        for (Path doc : List.of(CLAUDE, AGENTS)) {
            assertTrue(tracked.contains(doc.toString()),
                doc + " 未被 git 跟踪：换一台机器/克隆后就丢了，说明文档必须入库"
                    + "（不要把它加回 .gitignore）");
        }
    }

    @Test
    @DisplayName("两份文档的版本号同步说明必须列全四个来源")
    void instructionDocsAgreeOnVersionSources() throws IOException {
        // 只看**讲版本同步的那一句**：文档别处也会提到 .env.example（例如 compose 说明），
        // 若只要求"全文出现过"，那种无关提及会让门禁变成假绿灯（变异测试踩过）。
        for (Path doc : List.of(CLAUDE, AGENTS)) {
            String syncLine = versionSyncLine(Files.readString(doc));
            assertTrue(syncLine != null,
                doc + " 找不到版本号同步说明（应有一句同时提到 AppConstants 与 Installer）");
            List<String> missing = new ArrayList<>();
            for (String source : VERSION_SOURCES) {
                if (!syncLine.contains(source)) {
                    missing.add(source);
                }
            }
            assertTrue(missing.isEmpty(),
                doc + " 的版本号同步说明没列全四个来源，缺: " + missing
                    + "\n  该句为: " + syncLine.trim()
                    + "\n  版本升级时四处必须一起改，说明文档写全才不会漏改。");
        }
    }

    /** 取出讲"版本号四处同步"的那一行（同时提到 AppConstants 与 Installer）。 */
    private static String versionSyncLine(String text) {
        for (String line : text.split("\n")) {
            if (line.contains("AppConstants") && line.contains("Installer")) {
                return line;
            }
        }
        return null;
    }

    @Test
    @DisplayName("两份说明文档不得写死测试数量")
    void instructionDocsDoNotHardcodeTestCounts() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path doc : List.of(CLAUDE, AGENTS)) {
            String text = Files.readString(doc);
            Matcher matcher = HARDCODED_COUNT.matcher(text);
            while (matcher.find()) {
                int line = text.substring(0, matcher.start()).split("\n", -1).length;
                violations.add(doc + ":" + line + "  →  " + matcher.group().trim());
            }
        }
        assertFalse(!violations.isEmpty(),
            "说明文档里写死测试数量必然过期（数量每次加测试都会变），"
                + "请改为\"以 `mvn verify` 输出里的 Tests run: 为准\"：\n  "
                + String.join("\n  ", violations));
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
