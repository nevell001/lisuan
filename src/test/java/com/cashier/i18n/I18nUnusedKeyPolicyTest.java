package com.cashier.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 语言包不得残留无人引用的 key（TD-014）。
 *
 * <p>无用 key 是"看不到的技术债"：翻译要维护四份、改名时容易漏、读代码时还会误以为某功能存在。
 * 2026-09 一次性清掉 196 个（另删 `I18n` 里 59 个只指向这些 key 的常量），本门禁防止再长回来。</p>
 *
 * <p>判定"已被引用"的口径（保守——宁可漏报无用 key，也不误删在用的）：</p>
 * <ol>
 *   <li>Java 里出现**完整等于该 key 的字符串字面量**（`get("a.b")` 这类）；</li>
 *   <li>引用了 `I18nKeys.xxx` / `I18n.xxx` 常量，且常量值等于该 key；</li>
 *   <li>FXML/资源文件里出现 `"%key"`；</li>
 *   <li>代码里有 `"前缀." + 变量` 这类**拼接**，则该前缀下的所有 key 一律视为在用
 *       （例如 `audit.category.` / `audit.result.` / `payment.channel.`）。</li>
 * </ol>
 *
 * <p>如果以后引入了本类不认识的引用方式（例如静态导入 `I18nKeys.*` 后直接用短名），
 * 门禁会显式报出来让人更新规则，而不是悄悄当成"无用"。</p>
 */
@DisplayName("语言包无用 key 门禁")
class I18nUnusedKeyPolicyTest {

    private static final Path BUNDLE = Path.of("src/main/resources/com/cashier/i18n/messages_zh_CN.properties");
    private static final List<Path> CONSTANT_HOLDERS = List.of(
        Path.of("src/main/java/com/cashier/i18n/I18nKeys.java"),
        Path.of("src/main/java/com/cashier/i18n/I18n.java"));

    private static final Pattern KEY_LINE = Pattern.compile("^(\\w[\\w.]*)\\s*=", Pattern.MULTILINE);
    private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"\\\\\\n]+)\"");
    private static final Pattern LITERAL_PLUS = Pattern.compile("\"([^\"\\\\\\n]+)\"\\s*\\+");
    private static final Pattern HOLDER_REF = Pattern.compile("\\b(I18nKeys|I18n)\\.(\\w+(?:\\.\\w+)*)");
    private static final Pattern FXML_KEY = Pattern.compile("\"%([\\w.]+)\"");
    private static final Pattern CONSTANT_DECL =
        Pattern.compile("^\\s*public static final String (\\w+)\\s*=\\s*\"([^\"]+)\";");
    private static final Pattern STATIC_IMPORT =
        Pattern.compile("import static com\\.cashier\\.i18n\\.(I18nKeys|I18n)[\\w.]*\\.\\*;");

    @Test
    @DisplayName("语言包里不得有无人引用的 key")
    void bundlesHaveNoUnusedKeys() throws Exception {
        Set<String> keys = bundleKeys();
        assertTrue(keys.size() > 1000, "语言包 key 数量异常: " + keys.size());

        Map<String, String> constants = constantMap();
        Set<String> used = new HashSet<>();
        Set<String> dynamicPrefixes = new HashSet<>();
        List<String> unsupported = new ArrayList<>();

        for (Path file : javaSources()) {
            String text = stripComments(Files.readString(file));
            boolean holder = CONSTANT_HOLDERS.contains(file);
            if (STATIC_IMPORT.matcher(text).find()) {
                unsupported.add(file + " 使用了静态导入 I18nKeys.*/I18n.*，本门禁的常量解析会漏判");
            }
            if (!holder) {
                // 1) 精确字面量
                Matcher literals = STRING_LITERAL.matcher(text);
                while (literals.find()) {
                    if (keys.contains(literals.group(1))) {
                        used.add(literals.group(1));
                    }
                }
                // 2) 常量引用
                Matcher refs = HOLDER_REF.matcher(text);
                while (refs.find()) {
                    String value = constants.get(refs.group(1) + "." + refs.group(2));
                    if (value != null) {
                        used.add(value);
                    }
                }
            }
            // 4) 拼接前缀（保守：该前缀下所有 key 视为在用）
            Matcher plus = LITERAL_PLUS.matcher(text);
            while (plus.find()) {
                String prefix = plus.group(1);
                if (!prefix.isEmpty()) {
                    dynamicPrefixes.add(prefix);
                }
            }
        }
        for (Path file : resourceFiles()) {
            Matcher matcher = FXML_KEY.matcher(Files.readString(file));
            while (matcher.find()) {
                if (keys.contains(matcher.group(1))) {
                    used.add(matcher.group(1));
                }
            }
        }

        List<String> unused = new ArrayList<>();
        for (String key : keys) {
            if (used.contains(key)) {
                continue;
            }
            boolean dynamic = false;
            for (String prefix : dynamicPrefixes) {
                if (key.startsWith(prefix)) {
                    dynamic = true;
                    break;
                }
            }
            if (!dynamic) {
                unused.add(key);
            }
        }

        assertTrue(unsupported.isEmpty(),
            "门禁需要更新：\n  " + String.join("\n  ", unsupported));
        assertTrue(unused.isEmpty(),
            "以下语言包 key 没有任何代码/FXML 引用（四份语言包都要一起删，指向它们的常量也一并删）：\n  "
                + String.join("\n  ", unused)
                + "\n若确实由动态拼接使用，请在代码里出现 `\"前缀.\" + 变量` 的拼接形式（门禁据此放行）。");
    }

    @Test
    @DisplayName("I18nKeys 与 I18n 的常量值都必须存在于语言包")
    void constantValuesExistInBundles() throws Exception {
        Set<String> keys = bundleKeys();
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> entry : constantMap().entrySet()) {
            if (!keys.contains(entry.getValue())) {
                missing.add(entry.getKey() + " -> " + entry.getValue());
            }
        }
        assertTrue(missing.isEmpty(),
            "常量指向了语言包里不存在的 key（用起来会直接显示 key 本身）：\n  "
                + String.join("\n  ", missing));
    }

    // ---------- 解析辅助 ----------

    private static Set<String> bundleKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        Matcher matcher = KEY_LINE.matcher(Files.readString(BUNDLE));
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    /** 解析两个常量持有者：`I18nKeys.Outer.NAME` / `I18n.NAME` -> 语言包 key。 */
    private static Map<String, String> constantMap() throws IOException {
        Map<String, String> out = new HashMap<>();
        for (Path file : CONSTANT_HOLDERS) {
            String holder = file.getFileName().toString().replace(".java", "");
            List<String> stack = new ArrayList<>();
            for (String line : stripComments(Files.readString(file)).split("\n", -1)) {
                Matcher decl = CONSTANT_DECL.matcher(line);
                Matcher nested = Pattern.compile("\\b(?:class|interface|enum|record)\\s+(\\w+)").matcher(line);
                int opens = count(line, '{');
                int closes = count(line, '}');
                if (decl.find()) {
                    List<String> chain = new ArrayList<>();
                    for (String name : stack) {
                        if (name != null) {
                            chain.add(name);
                        }
                    }
                    if (!chain.isEmpty() && chain.get(0).equals(holder)) {
                        chain.remove(0);
                    }
                    chain.add(decl.group(1));
                    out.put(holder + "." + String.join(".", chain), decl.group(2));
                }
                if (nested.find() && opens > 0) {
                    stack.add(nested.group(1));
                    for (int i = 1; i < opens; i++) {
                        stack.add(null);
                    }
                } else {
                    for (int i = 0; i < opens; i++) {
                        stack.add(null);
                    }
                }
                for (int i = 0; i < closes && !stack.isEmpty(); i++) {
                    stack.remove(stack.size() - 1);
                }
            }
        }
        return out;
    }

    private static int count(String text, char c) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == c) {
                total++;
            }
        }
        return total;
    }

    private static String stripComments(String text) {
        String withoutBlocks = text.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlocks.replaceAll("(?m)//.*$", "");
    }

    private static List<Path> javaSources() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String root : List.of("src/main/java", "src/test/java")) {
            try (Stream<Path> walk = Files.walk(Path.of(root))) {
                files.addAll(walk.filter(p -> p.toString().endsWith(".java")).toList());
            }
        }
        return files;
    }

    private static List<Path> resourceFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Path.of("src/main/resources"))) {
            files.addAll(walk.filter(p -> {
                String name = p.toString();
                return Files.isRegularFile(p) && !name.contains("/i18n/")
                    && (name.endsWith(".fxml") || name.endsWith(".css") || name.endsWith(".xml"));
            }).toList());
        }
        return files;
    }
}
