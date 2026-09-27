package com.cashier.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 语言包占位符与实参个数必须匹配。
 *
 * <p>2026-09 用户实测发现商品管理页右上角显示 <code>商品数量: {0}: 94/94</code>——因为
 * {@code inventory.count} 的值本身是 {@code 商品数量: {0}}，调用处却写成
 * {@code get("inventory.count") + ": " + size + "/" + total}：**没传参**，{@code {0}} 就原样显示出来了。
 * 这类 bug 静默、只在界面上看得见，所以单列一条门禁。</p>
 *
 * <p>判定口径：对每个 {@code get("<key>"…)} / {@code get(I18nKeys.X…)} 调用，
 * 把语言包值里的最大占位符序号 {@code {n}} 与实参个数比较——{@code n+1 > 实参个数} 即失败
 * （第一个实参是 key，剩下的才是占位符参数）。首参是变量/动态拼接的调用跳过（无法静态判定）。</p>
 */
@DisplayName("i18n 占位符与实参匹配门禁")
class I18nPlaceholderArgsPolicyTest {

    private static final Path BUNDLE =
        Path.of("src/main/resources/com/cashier/i18n/messages_zh_CN.properties");
    private static final List<Path> CONSTANT_HOLDERS = List.of(
        Path.of("src/main/java/com/cashier/i18n/I18nKeys.java"),
        Path.of("src/main/java/com/cashier/i18n/I18n.java"));

    private static final Pattern GET_CALL = Pattern.compile("\\.get\\(\\s*(\"[^\"]+\"|I18n(?:Keys)?\\.[\\w.]+)");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");
    private static final Pattern CONSTANT_DECL =
        Pattern.compile("^\\s*public static final String (\\w+)\\s*=\\s*\"([^\"]+)\";");

    @Test
    @DisplayName("每个占位符都必须有对应实参（否则界面直接显示 {0}）")
    void everyPlaceholderHasItsArgument() throws Exception {
        Map<String, String> values = bundleValues();
        Map<String, String> constants = constantValues();
        List<String> violations = new ArrayList<>();
        int checked = 0;

        for (Path file : javaSources()) {
            String text = stripComments(Files.readString(file));
            Matcher call = GET_CALL.matcher(text);
            while (call.find()) {
                String key = resolve(call.group(1), constants);
                String value = key == null ? null : values.get(key);
                if (value == null) {
                    continue;                       // 不是语言包 key（如 Map.get("...")）→ 不参与
                }
                checked++;
                int open = text.indexOf('(', call.start());
                int close = matchingParen(text, open);
                if (close < 0) {
                    continue;
                }
                // 减去 key 本身：剩下才是占位符实参（漏减 1 会让 get(key) 这种漏参写法恰好通过——
                // 变异测试当场抓出来过，别再改回去）
                int supplied = topLevelCommaCount(text.substring(open + 1, close)) - 1;
                int needed = maxPlaceholderIndex(value);
                if (needed > supplied) {
                    int line = text.substring(0, call.start()).split("\n", -1).length;
                    violations.add(relative(file) + ":" + line + "  key=" + key
                        + "  值=\"" + value.trim() + "\"  需要 " + needed + " 个参数，只传了 " + supplied);
                }
            }
        }

        assertTrue(checked > 1000,
            "只识别到 " + checked + " 处可静态判定的 i18n 调用（2026-09 实际 1600+），"
                + "识别规则可能失效——本门禁会变成空转");
        assertTrue(violations.isEmpty(),
            "以下 i18n 调用没有为占位符传参，界面上会把 {0} 原样显示出来（用户实测过商品管理页的"
                + "\"商品数量: {0}: 94/94\"）。请把参数传进去（如 get(key, a + \"/\" + b)），"
                + "或改用不带占位符的 key：\n  " + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("FXML 不得引用带占位符的 key（FXML 不做参数替换，会显示 {0}）")
    void fxmlDoesNotReferencePlaceholderKeys() throws Exception {
        Map<String, String> values = bundleValues();
        List<String> violations = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> walk = Files.walk(Path.of("src/main/resources"))) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                String text = Files.readString(file);
                Matcher matcher = Pattern.compile("\"%([\\w.]+)\"").matcher(text);
                while (matcher.find()) {
                    checked++;
                    String value = values.get(matcher.group(1));
                    if (value != null && PLACEHOLDER.matcher(value).find()) {
                        int line = text.substring(0, matcher.start()).split("\n", -1).length;
                        violations.add(file.getFileName() + ":" + line + "  %" + matcher.group(1)
                            + "  ->  \"" + value.trim() + "\"");
                    }
                }
            }
        }
        assertTrue(checked > 200, "只扫到 " + checked + " 处 FXML %key 引用（2026-09 实际 900），识别规则可能失效");
        assertTrue(violations.isEmpty(),
            "FXML 的 %key 由 ResourceBundle 直接解析，**不做 {0} 参数替换**——这些标签会在控制器填值前"
                + "（页面加载已后台化，慢库上可见）显示占位符原文，加载失败则一直是它。"
                + "请把 FXML 文本改为空（控制器会填），或用不带占位符的 key：\n  "
                + String.join("\n  ", violations));
    }

    // ---------- 解析辅助 ----------

    private static Map<String, String> bundleValues() throws IOException {
        Map<String, String> values = new HashMap<>();
        for (String line : Files.readString(BUNDLE).split("\n", -1)) {
            if (line.startsWith("#") || !line.contains("=")) {
                continue;
            }
            int eq = line.indexOf('=');
            values.put(line.substring(0, eq).trim(), line.substring(eq + 1));
        }
        return values;
    }

    /** 解析 {@code I18nKeys.Outer.NAME} / {@code I18n.NAME} -> 语言包 key。 */
    private static Map<String, String> constantValues() throws IOException {
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

    private static String resolve(String argument, Map<String, String> constants) {
        if (argument.startsWith("\"")) {
            return argument.substring(1, argument.length() - 1);
        }
        return constants.get(argument);
    }

    /** 值里最大占位符序号 +1，即需要的实参个数（没有占位符时为 0）。 */
    private static int maxPlaceholderIndex(String value) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        int needed = 0;
        while (matcher.find()) {
            needed = Math.max(needed, Integer.parseInt(matcher.group(1)) + 1);
        }
        return needed;
    }

    /** 顶层逗号数 + 1 = 实参个数（跳过字符串与嵌套括号/方括号里的逗号）。 */
    private static int topLevelCommaCount(String arguments) {
        if (arguments.isBlank()) {
            return 0;
        }
        int depth = 0;
        int commas = 0;
        for (int i = 0; i < arguments.length(); i++) {
            char c = arguments.charAt(i);
            if (c == '"') {
                i = skipString(arguments, i);
            } else if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                commas++;
            }
        }
        return commas + 1;
    }

    private static int matchingParen(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                i = skipString(text, i);
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int skipString(String text, int start) {
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            } else if (c == '\n') {
                return i - 1;
            }
        }
        return text.length() - 1;
    }

    private static int count(String text, char target) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                total++;
            }
        }
        return total;
    }

    private static String stripComments(String text) {
        return text.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    private static List<Path> javaSources() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java"))) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
        }
        assertTrue(!files.isEmpty(), "未找到 Java 源码，路径不对？");
        return files;
    }

    private static String relative(Path file) {
        return "src/main/java/" + Path.of("src/main/java").relativize(file);
    }
}
