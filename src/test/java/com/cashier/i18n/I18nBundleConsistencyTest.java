package com.cashier.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * i18n 资源完整性门禁：
 * 1. 四份语言包（zh_CN/zh_TW/en + 默认回退包 messages.properties）key 集合必须完全一致；
 * 2. 每份语言包不得出现重复 key（缺换行导致两行粘连的典型症状）；
 * 3. {@link I18nKeys} 中声明的每个 key 必须存在于语言包；
 * 4. 源码中所有字面量 i18n 调用（I18nManager/i18n.get）的 key 必须存在于语言包，
 *    避免界面直接显示原始 key。
 */
@DisplayName("i18n 语言包一致性测试")
class I18nBundleConsistencyTest {

    private static final Path BASE = Path.of("src/main/resources/com/cashier/i18n");
    private static final String DEFAULT_BUNDLE = "messages.properties";
    private static final String[] LOCALE_BUNDLES = {
        "messages_zh_CN.properties", "messages_zh_TW.properties", "messages_en.properties",
        DEFAULT_BUNDLE};

    private static Set<String> loadKeys(String bundleFileName) throws IOException {
        Set<String> keys = new HashSet<>();
        for (String line : Files.readAllLines(BASE.resolve(bundleFileName))) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }
            int sep = trimmed.indexOf('=');
            if (sep < 0) {
                sep = trimmed.indexOf(':');
            }
            if (sep > 0) {
                keys.add(trimmed.substring(0, sep).trim());
            }
        }
        return keys;
    }

    @Test
    @DisplayName("三份语言包与默认回退包 key 集合一致")
    void allLocalesHaveSameKeys() throws IOException {
        Set<String> reference = loadKeys(LOCALE_BUNDLES[0]);
        for (String bundle : LOCALE_BUNDLES) {
            assertEquals(reference, loadKeys(bundle),
                "语言包 " + bundle + " 的 key 与 " + LOCALE_BUNDLES[0] + " 不一致");
        }
    }

    @Test
    @DisplayName("每份语言包不得出现重复 key")
    void noDuplicateKeysInAnyBundle() throws IOException {
        for (String bundle : LOCALE_BUNDLES) {
            Set<String> seen = new HashSet<>();
            Set<String> duplicates = new HashSet<>();
            for (String line : Files.readAllLines(BASE.resolve(bundle))) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    continue;
                }
                int sep = trimmed.indexOf('=');
                if (sep < 0) {
                    sep = trimmed.indexOf(':');
                }
                if (sep <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, sep).trim();
                if (!seen.add(key)) {
                    duplicates.add(key);
                }
            }
            assertTrue(duplicates.isEmpty(),
                "语言包 " + bundle + " 存在重复 key（疑似缺换行粘连）: " + duplicates);
        }
    }

    @Test
    @DisplayName("I18nKeys 声明的常量均存在于语言包")
    void i18nKeysConstantsExistInBundles() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/cashier/i18n/I18nKeys.java"));
        Pattern constant = Pattern.compile("String\\s+\\w+\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = constant.matcher(source);

        Set<String> declared = new HashSet<>();
        while (matcher.find()) {
            declared.add(matcher.group(1));
        }
        assertTrue(declared.size() >= 100, "I18nKeys 常量数量异常: " + declared.size());

        Set<String> bundle = loadKeys(LOCALE_BUNDLES[0]);
        Set<String> missing = new HashSet<>(declared);
        missing.removeAll(bundle);
        assertTrue(missing.isEmpty(), "I18nKeys 中缺失的语言包 key: " + missing);
    }

    @Test
    @DisplayName("源码字面量 i18n 调用 key 均存在于语言包")
    void hardcodedI18nKeysExistInBundles() throws IOException {
        Pattern call = Pattern.compile(
            "(?:com\\.cashier\\.i18n\\.I18nManager\\s*\\.\\s*getInstance\\s*\\(\\)\\s*\\.\\s*" +
                "|I18nManager\\s*\\.\\s*getInstance\\s*\\(\\)\\s*\\.\\s*" +
                "|i18n\\s*\\.\\s*)get\\s*\\(\"([^\"]+)\"");
        Set<String> bundle = loadKeys(LOCALE_BUNDLES[0]);
        Set<String> missing = new HashSet<>();

        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("I18nManager.java")) {
                    continue;
                }
                Matcher matcher = call.matcher(Files.readString(file));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (!key.startsWith("/") && !bundle.contains(key)) {
                        missing.add(key + "  <-  " + file);
                    }
                }
            }
        }

        assertTrue(missing.isEmpty(), "源码中缺失的语言包 key: " + missing);
    }
}
