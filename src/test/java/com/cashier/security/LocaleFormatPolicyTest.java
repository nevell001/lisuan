package com.cashier.security;

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
 * 金额/百分比格式化必须显式指定 {@link java.util.Locale}（TD-015）。
 *
 * <p>{@code String.format("%.2f", x)} 用的是平台默认 locale：中文/繁体/英文环境下小数分隔符都是
 * {@code .}，所以本机没事；但德语/法语等默认 locale 的机器会打出 {@code 1,50}——
 * 小票（要给人核对）、CSS 的 {@code rgba(..., 0,5)}（直接失效）、以及任何会被再次解析的字符串
 * （{@code Double.parseDouble("1,50")} 抛异常）都会出问题。</p>
 *
 * <p>本门禁只针对**含浮点转换**的格式串（{@code %f}/{@code %.2f}/{@code %e}/{@code %g}）：
 * 纯 {@code %d}/{@code %s} 不受 locale 影响，不必强制。格式串可以是字面量，
 * 也可以是同仓库里的 {@code static final String X = "%.2f%%"} 常量（会跨文件解析）。</p>
 */
@DisplayName("金额格式化 locale 门禁")
class LocaleFormatPolicyTest {

    /** 只定位调用位置；第一个实参按"引号配对"解析，避免格式串内部含逗号时被截断。 */
    private static final Pattern FORMAT_CALL = Pattern.compile("String\\.format\\(");

    /** 浮点转换：%f / %.2f / %10.2f / %e / %g，允许中间带标志与宽度。 */
    private static final Pattern FLOAT_CONVERSION =
        Pattern.compile("%[-#+ 0,(]*[0-9]*(?:\\.[0-9]+)?[eEfgG]");

    private static final Pattern STRING_CONSTANT =
        Pattern.compile("String\\s+(\\w+)\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Test
    @DisplayName("src/main 里含浮点转换的 String.format 必须显式指定 Locale")
    void floatFormattingAlwaysPinsLocale() throws Exception {
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java"))) {
            sources.addAll(walk.filter(p -> p.toString().endsWith(".java")).toList());
        }

        // 先把全仓库的字符串常量收集起来（格式串经常抽成常量，如 PERCENT_FORMAT = "%.2f%%"）
        Map<String, String> constants = new HashMap<>();
        for (Path source : sources) {
            Matcher constants_matcher = STRING_CONSTANT.matcher(Files.readString(source));
            while (constants_matcher.find()) {
                constants.putIfAbsent(constants_matcher.group(1), constants_matcher.group(2));
            }
        }

        List<String> violations = new ArrayList<>();
        for (Path source : sources) {
            String text = Files.readString(source);
            Matcher calls = FORMAT_CALL.matcher(text);
            while (calls.find()) {
                String firstArg = firstArgument(text, calls.end() - 1);
                if (firstArg.equals("java.util.Locale.ROOT") || firstArg.equals("Locale.ROOT")) {
                    continue; // 已指定
                }
                String format = resolveFormat(firstArg, constants);
                if (format != null && FLOAT_CONVERSION.matcher(format).find()) {
                    int line = text.substring(0, calls.start()).split("\n", -1).length;
                    violations.add(source + ":" + line + "  →  String.format(" + firstArg + ", ...) 格式串=\"" + format + "\"");
                }
            }
        }

        assertTrue(violations.isEmpty(),
            "以下位置的浮点格式化没有指定 Locale（非中文默认 locale 的机器会打出「1,50」）：\n  "
                + String.join("\n  ", violations)
                + "\n改法：String.format(java.util.Locale.ROOT, ...)");
    }

    /**
     * 取出 {@code String.format(} 后的第一个实参。
     *
     * <p>按引号配对读取字面量——不能用 {@code [^,)]+} 之类按逗号截断：格式串内部常含逗号
     * （{@code "rgba(%d, %d, %d, %.2f)"} 会被截成 {@code "rgba(%d}，导致漏判）。</p>
     */
    private static String firstArgument(String text, int openParen) {
        int i = openParen + 1;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        if (i < text.length() && text.charAt(i) == '"') {
            StringBuilder literal = new StringBuilder();
            i++;
            while (i < text.length()) {
                char c = text.charAt(i);
                if (c == '\\') {
                    literal.append(c).append(text.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (c == '"') {
                    break;
                }
                literal.append(c);
                i++;
            }
            return "\"" + literal + "\"";
        }
        int start = i;
        while (i < text.length() && text.charAt(i) != ',' && text.charAt(i) != ')') {
            i++;
        }
        return text.substring(start, i).trim();
    }

    /** 把格式串参数解析成实际格式：字面量直接用，常量名去仓库里查。 */
    private static String resolveFormat(String firstArg, Map<String, String> constants) {
        if (firstArg.startsWith("\"") && firstArg.length() >= 2) {
            return firstArg.substring(1, firstArg.length() - 1);
        }
        return constants.get(firstArg);
    }

    @Test
    @DisplayName("小票与样式这两类「错了就静默失效」的格式化必须已固定 Locale")
    void receiptAndStyleFormattingPinnedLocale() throws Exception {
        Map<String, String> constants = stringConstants();
        List<String> violations = new ArrayList<>();
        for (String file : List.of(
                "src/main/java/com/cashier/util/ReceiptPrinter.java",
                "src/main/java/com/cashier/printer/PrintUtil.java",
                "src/main/java/com/cashier/printer/ReceiptBuilder.java",
                "src/main/java/com/cashier/constant/FXConstants.java",
                "src/main/java/com/cashier/util/CurrencyUtil.java")) {
            String text = Files.readString(Path.of(file));
            Matcher calls = FORMAT_CALL.matcher(text);
            while (calls.find()) {
                String firstArg = firstArgument(text, calls.end() - 1);
                if (firstArg.equals("java.util.Locale.ROOT") || firstArg.equals("Locale.ROOT")) {
                    continue;
                }
                String format = resolveFormat(firstArg, constants);
                if (format != null && FLOAT_CONVERSION.matcher(format).find()) {
                    violations.add(file + " → " + format);
                }
            }
        }
        assertTrue(violations.isEmpty(),
            "小票/样式/金额工具里的浮点格式化必须固定 Locale.ROOT（否则非中文默认 locale 会打出 1,50，"
                + "小票对不上、CSS rgba() 直接失效）：\n  " + String.join("\n  ", violations));
    }

    /** 收集全仓库的字符串常量（格式串常抽成常量，如 PERCENT_FORMAT = "%.2f%%"）。 */
    private static Map<String, String> stringConstants() throws IOException {
        Map<String, String> constants = new HashMap<>();
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java"))) {
            for (Path source : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher matcher = STRING_CONSTANT.matcher(Files.readString(source));
                while (matcher.find()) {
                    constants.putIfAbsent(matcher.group(1), matcher.group(2));
                }
            }
        }
        return constants;
    }
}
