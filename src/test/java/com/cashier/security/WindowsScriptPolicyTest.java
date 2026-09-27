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
 * Windows 批处理脚本门禁（TD-011）。
 *
 * <p>这些脚本在 macOS/Linux 上**既不能执行也不能语法检查**（没有 cmd.exe），所以缺陷只能靠静态规则挡。
 * 已发生过的真实缺陷是"用 for 迭代通配符判断文件是否存在"：cmd 在**没有匹配**时会把这个通配符
 * **原样当成一项**，于是 {@code if "%JAR_FILE%"==""} 永远为假、构建被跳过，
 * 直到 {@code java -jar "target\...*..."} 才报 {@code Unable to access jarfile}（用户看到的是莫名其妙的一条 Java 报错）。</p>
 *
 * <p>本门禁只钉住**可在文本层面确证**的规则；脚本在 Windows 上的实际行为仍需实机验收
 * （见 {@code docs/TECH_DEBT.md} 的 TD-011 验收清单）。</p>
 */
@DisplayName("Windows 批处理脚本门禁")
class WindowsScriptPolicyTest {

    /** for 后直接跟带通配符的集合——危险写法。 */
    private static final Pattern WILDCARD_FOR_SET =
        Pattern.compile("for\\s+%%[A-Za-z]\\s+in\\s*\\(\\s*\"?[^)\"\r\n]*\\*");

    /** java 收到通配符路径（cmd 不会为外部命令展开通配符）。 */
    private static final Pattern JAVA_WITH_WILDCARD =
        Pattern.compile("java\\s+[^\\r\\n]*\\*[^\\r\\n]*");

    /** 真正的代码页切换（chcp 65001 ...），而不是注释里提到 chcp。 */
    private static final Pattern CODE_PAGE_SWITCH =
        Pattern.compile("(?m)^\\s*chcp\\s+65001");

    /** 真正的 maven 调用（行首，或 call mvn）；错误提示里的 echo mvn ... 不算。 */
    private static final Pattern MAVEN_INVOCATION =
        Pattern.compile("(?m)^\\s*(call\\s+)?mvn\\s");

    /** 正确的检测方式：for /f + dir /b —— 无匹配时不产生任何行。 */
    private static final Pattern DIR_B_DETECTION =
        Pattern.compile("for\\s+/f[^\\r\\n]*\\('dir\\s+/b[^\\r\\n]*\\)");

    @Test
    @DisplayName("任何 .bat 都不得用 for 迭代通配符来判断文件存在")
    void noBatchScriptIteratesWildcardInForSet() throws Exception {
        List<String> violations = new ArrayList<>();
        for (String file : trackedBatFiles()) {
            String text = withoutComments(read(file));
            Matcher matcher = WILDCARD_FOR_SET.matcher(text);
            while (matcher.find()) {
                violations.add(file + ":" + lineOf(text, matcher.start())
                    + "  →  " + matcher.group().replace("\r", "").trim());
            }
        }
        assertTrue(violations.isEmpty(),
            "cmd 在无匹配时会把通配符原样当成一项，导致\"文件已存在\"的判断永远为真、错误分支不可达；"
                + "请改用 for /f \"delims=\" %%f in ('dir /b <模式>') do ... 配合 if not defined：\n  "
                + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("查 JAR 的脚本必须用 dir /b 检测，且用 if not defined 判空")
    void jarDetectionUsesDirBAndDefinedCheck() throws Exception {
        for (String file : List.of("start.bat", "install.bat", "DataConfig.bat")) {
            String text = read(file);
            assertTrue(DIR_B_DETECTION.matcher(text).find(),
                file + " 必须用 for /f + dir /b 查找 JAR（无匹配时不会退化成字面量模式）");
            assertTrue(text.contains("if not defined "),
                file + " 判断未找到 JAR 时必须用 if not defined（先置空再判断），不要用 if \"%VAR%\"==\"\"");
            assertFalse(text.contains("if \"%JAR_FILE%\"==\"\""),
                file + " 仍在使用 if \"%JAR_FILE%\"==\"\"：变量里可能是未展开的通配符，判断不可靠");
        }
    }

    @Test
    @DisplayName("start.bat 不得偷偷跑 maven，缺 JAR 必须明确报错并退出非零")
    void startBatReportsMissingJarInsteadOfBuilding() throws Exception {
        String text = read("start.bat");
        assertFalse(MAVEN_INVOCATION.matcher(text).find(),
            "按产品决定：start.bat 不自动构建，缺 JAR 时提示用户自己跑 mvn clean package -DskipTests"
                + "（错误提示里出现的 echo mvn ... 不算调用）");

        String error = "Application JAR not found";
        assertTrue(text.contains(error), "缺 JAR 必须给出可读的错误信息");
        int errorIndex = text.indexOf(error);
        int exitIndex = text.indexOf("exit /b 1", errorIndex);
        assertTrue(exitIndex > errorIndex, "缺 JAR 的分支必须以 exit /b 1 结束（否则会继续走到 java）");

        // 锚定真正的启动语句（"%JAR_FILE%" 参与的那一行），不能拿开头的 where java 探测当锚点
        int launchIndex = text.indexOf("-jar \"%JAR_FILE%\"");
        assertTrue(launchIndex > exitIndex,
            "启动语句（-jar \"%JAR_FILE%\"）必须排在\"缺 JAR 就退出\"之后");
        assertTrue(text.contains("mvn clean package -DskipTests"),
            "错误信息里要告诉用户该跑什么命令");
    }

    @Test
    @DisplayName("java 命令行不得收到通配符路径")
    void javaNeverReceivesWildcardPath() throws Exception {
        List<String> violations = new ArrayList<>();
        for (String file : trackedBatFiles()) {
            String text = read(file);
            Matcher matcher = JAVA_WITH_WILDCARD.matcher(text);
            while (matcher.find()) {
                String hit = matcher.group();
                if (hit.contains("*jar-with-dependencies.jar")) { // 仅出现在注释/echo 说明里不算
                    if (!hit.trim().startsWith("REM") && !hit.contains("echo")) {
                        violations.add(file + ":" + lineOf(text, matcher.start()) + "  →  " + hit.trim());
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(),
            "cmd 不会为 java 展开通配符，必须传入解析后的真实路径：\n  " + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("install.bat 生成的 DataConfig.bat 与随包版本必须用同一套检测逻辑")
    void generatedDataConfigKeepsSameDetectionIdiom() throws Exception {
        String install = read("install.bat");
        int heredocStart = install.indexOf("echo [INFO] Creating database configuration tool...");
        assertTrue(heredocStart > 0, "找不到生成 DataConfig.bat 的段落");
        String heredoc = install.substring(heredocStart);

        assertFalse(heredoc.contains("for %%%%f in ^(target\\lisuan-fx-*"),
            "生成的脚本不能再用 for 迭代通配符");
        assertTrue(heredoc.contains("for /f \"delims=\" %%%%f in ^('dir /b"),
            "生成的脚本要用 for /f + dir /b 检测 JAR");
        assertTrue(heredoc.contains("if not defined JAR_FILE ^("),
            "生成的脚本要用 if not defined 判断未找到");

        // 生成物与随包 DataConfig.bat 的检测关键片段必须一致，避免两处逻辑漂移
        String shipped = read("DataConfig.bat");
        for (String fragment : List.of("for /f \"delims=\" %%f in ('dir /b",
                                       "if not defined JAR_FILE",
                                       "mvn clean package -DskipTests")) {
            assertTrue(shipped.contains(fragment), "DataConfig.bat 缺少: " + fragment);
        }
    }

    @Test
    @DisplayName("start.bat 切换代码页后必须恢复，不能把用户的窗口留在 65001")
    void startBatRestoresCodePageAfterLaunch() throws Exception {
        String text = read("start.bat");
        assertTrue(CODE_PAGE_SWITCH.matcher(text).find(),
            "start.bat 应为可读的中文日志把控制台切到 65001");
        assertTrue(text.contains("chcp %ORIGINAL_CODE_PAGE%"),
            "切了代码页就必须恢复：javac/maven 按 GBK 输出，留在 65001 会让它们的输出变乱码");
        assertTrue(text.contains(":restore_code_page"),
            "恢复逻辑应放在子过程里，并在 java 返回后与 GUI 模式提前返回前各调用一次");
        assertTrue(text.contains("call :restore_code_page"),
            "java 返回后必须调用恢复子过程");
        // 恢复前要先取出原代码页，且解析结果必须校验为纯数字（chcp 输出随系统语言变化）
        assertTrue(text.contains("ORIGINAL_CODE_PAGE"),
            "必须先记住原代码页再切换");
        assertTrue(text.contains("findstr /r \"^[0-9][0-9]*$\""),
            "恢复前要校验解析出的代码页是纯数字（不同语言的 chcp 输出格式不同，解析失败时宁可不切）");
        // release.bat / docker/start-mysql.bat 自身就是中文输出，保留 65001 是有意为之，不在此规则内
    }

    @Test
    @DisplayName("install.bat 构建后必须真的校验 fat JAR 产出")
    void installVerifiesJarAfterBuild() throws Exception {
        String text = read("install.bat");
        int build = text.indexOf("call mvn clean package");
        assertTrue(build > 0, "install.bat 应显式构建（这是安装步骤，不是偷偷构建）");
        int verify = text.indexOf("if not defined EXISTING_JAR", build);
        assertTrue(verify > build,
            "构建 \"成功\" 也可能没产出 fat JAR，构建后必须再查一次并报错退出");
        assertTrue(text.indexOf("exit /b 1", verify) > verify, "该分支必须退出非零");
    }

    @Test
    @DisplayName(".bat 必须是 CRLF：LF 会让 goto 与 if 块在 Windows 上解析失败")
    void batchFilesAreStoredWithCrlfLineEndings() throws Exception {
        List<String> violations = new ArrayList<>();
        for (String file : trackedBatFiles()) {
            byte[] raw = Files.readAllBytes(Path.of(file));
            String content = new String(raw, StandardCharsets.UTF_8);
            boolean hasLines = content.contains("\n");
            if (hasLines && !content.contains("\r\n")) {
                violations.add(file + " 是纯 LF（Windows 上 goto :label 与 if 块会失效）");
            } else if (hasLines && content.replace("\r\n", "").contains("\n")) {
                violations.add(file + " 是混合行尾（说明有工具写过 LF）");
            }
        }
        assertTrue(violations.isEmpty(),
            "修复方式：git checkout -- <文件>（.gitattributes 里 *.bat 为 eol=crlf），"
                + "或用支持 CRLF 的编辑器重存：\n  " + String.join("\n  ", violations));
    }

    @Test
    @DisplayName(".bat 若含非 ASCII 文本，必须自己设置 UTF-8 代码页")
    void nonAsciiBatchScriptsMustSetCodePage() throws Exception {
        List<String> violations = new ArrayList<>();
        for (String file : trackedBatFiles()) {
            byte[] raw = Files.readAllBytes(Path.of(file));
            boolean hasNonAscii = false;
            for (byte b : raw) {
                if (b < 0) {
                    hasNonAscii = true;
                    break;
                }
            }
            String text = new String(raw, StandardCharsets.UTF_8);
            // 必须是真的切换代码页：只出现 "chcp" 这个词不算（静态检查不能被注释里的自述骗过）
            if (hasNonAscii && !CODE_PAGE_SWITCH.matcher(text).find()) {
                violations.add(file + " 含非 ASCII 字节，但没有 chcp 65001");
            }
        }
        assertTrue(violations.isEmpty(),
            "cmd 用当前 OEM 代码页读 .bat（中文 Windows 是 936/GBK）：UTF-8 中文会变成乱码，"
                + "而乱码会让字节错位，把 REM 后的空格或 ^( 的 ^ 吞掉，"
                + "于是注释被当成命令执行（\"不是内部或外部命令\"）或块解析失败（\"此时不应有\"）——"
                + "实测已在 Windows 上重现过。两种修法：注释改成纯英文（推荐，本仓库 start.bat/install.bat/"
                + "DataConfig.bat/create-shortcut.bat/diagnose.bat 已如此），"
                + "或在文件开头加 chcp 65001 >nul 并保持 UTF-8（release.bat/docker/start-mysql.bat 的既有做法）。"
                + "注意 'echo 里中文' 也会写进生成物，同样算违规：\n  " + String.join("\n  ", violations));
    }

    private static List<String> trackedBatFiles() throws Exception {
        List<String> files = new ArrayList<>();
        for (String file : trackedFiles()) {
            if (file.endsWith(".bat") && Files.exists(Path.of(file))) {
                files.add(file);
            }
        }
        assertFalse(files.isEmpty(), "应当能列出仓库里的 .bat 文件");
        return files;
    }

    /** 去掉 REM / :: 注释行——注释里为说明问题而写出坏写法是合理的。 */
    private static String withoutComments(String text) {
        StringBuilder kept = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String trimmed = line.stripLeading();
            if (trimmed.regionMatches(true, 0, "REM ", 0, 4) || trimmed.startsWith("::")) {
                continue;
            }
            kept.append(line).append('\n');
        }
        return kept.toString();
    }

    private static String read(String file) throws IOException {
        return Files.readString(Path.of(file)).replace("\r\n", "\n");
    }

    private static int lineOf(String text, int index) {
        return text.substring(0, index).split("\n", -1).length;
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
