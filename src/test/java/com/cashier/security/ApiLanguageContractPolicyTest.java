package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REST API 语言契约门禁（TD-041）。
 *
 * <p>定稿的契约是**双字段**：枚举字段给稳定代码（{@code deviceType: "NETWORK"}），
 * 另给 {@code *Name} 字段给按请求语言本地化的显示名。此前 API 只把中文枚举显示名当值返回，
 * 客户端既拿不到可判定的代码，也无法按自己的语言展示。</p>
 *
 * <p>同时钉住实现纪律：显示名必须经 {@code ApiMessages}（按请求语言、不改进程语言），
 * 不得在 JSON 里直接塞 {@code getDisplayName()}。</p>
 */
@DisplayName("REST API 语言契约门禁（TD-041）")
class ApiLanguageContractPolicyTest {

    private static final Path PRINT_API =
        Path.of("src/main/java/com/cashier/api/controller/PrintApiController.java");
    private static final Path API_MESSAGES = Path.of("src/main/java/com/cashier/api/ApiMessages.java");

    @Test
    @DisplayName("JSON 里不得直接塞枚举的中文显示名，必须给代码 + 本地化名字段")
    void enumFieldsAreCodePlusLocalizedName() throws IOException {
        String source = withoutComments(read(PRINT_API));
        List<String> violations = new ArrayList<>();
        for (String line : source.split("\n")) {
            // 只看会进 JSON 的字段赋值语句；测试页正文（content.append）不属契约
            if (line.contains("getDisplayName()") && (line.contains(".put(") || line.contains("\"status\"")
                    || line.contains("\"deviceType\"") || line.contains("\"taskType\""))) {
                violations.add(line.trim());
            }
        }
        assertTrue(violations.isEmpty(),
            "枚举显示名不得直接进 JSON（TD-041），应返回 name() 代码 + *Name 本地化字段：\n  "
                + String.join("\n  ", violations));

        assertTrue(source.contains("\"deviceTypeName\""), "应提供 deviceTypeName（本地化显示名）");
        assertTrue(source.contains("\"statusName\""), "应提供 statusName（本地化显示名）");
        assertTrue(source.contains("\"taskTypeName\""), "应提供 taskTypeName（本地化显示名）");
        assertTrue(source.contains(".getDeviceType().name()"),
            "deviceType 必须返回稳定代码（enum.name()）");
    }

    @Test
    @DisplayName("显示名必须经 ApiMessages：按请求语言解析，且不修改进程级语言")
    void localizedNamesGoThroughApiMessages() throws IOException {
        String source = read(PRINT_API);
        assertTrue(source.contains("com.cashier.api.ApiMessages.enumName("),
            "显示名必须经 ApiMessages.enumName(...) 解析（按请求语言）");

        String helper = read(API_MESSAGES);
        assertTrue(helper.contains("ApiLocaleResolver.of(ctx)"),
            "语言必须按请求解析（?locale= / Accept-Language / 用户偏好）");
        assertTrue(!helper.contains("I18nManager.getInstance().setLocale"),
            "渲染 API 文案时不得修改进程级语言（否则一个客户端切语言会改掉桌面端）");
    }

    @Test
    @DisplayName("四个 printer 枚举的显示名 key 在四份语言包里齐全")
    void printerEnumNameKeysExistInEveryBundle() throws IOException {
        String[] prefixes = {"api.printer.device_type.", "api.printer.device_status.",
            "api.print_task.type.", "api.print_task.status."};
        String[] bundles = {"messages.properties", "messages_zh_CN.properties",
            "messages_zh_TW.properties", "messages_en.properties"};
        Path i18n = Path.of("src/main/resources/com/cashier/i18n");

        for (String bundle : bundles) {
            String content = read(i18n.resolve(bundle));
            for (String prefix : prefixes) {
                assertTrue(content.contains(prefix),
                    bundle + " 缺少枚举显示名 key 前缀 " + prefix
                        + "（TD-041：双字段契约需要四份语言包都齐全）");
            }
        }

        // 英文包必须真的翻译过（不能把中文复制过去充数）
        String en = read(i18n.resolve("messages_en.properties"));
        assertTrue(en.contains("api.printer.device_type.NETWORK=Network printer"),
            "英文包里的枚举显示名必须是英文");
    }

    @Test
    @DisplayName("按语言的 i18n 取值支持占位符（否则带参文案会漏出 {0}）")
    void localeAwareLookupSupportsArguments() throws IOException {
        String manager = read(Path.of("src/main/java/com/cashier/i18n/I18nManager.java"));
        assertTrue(manager.contains("public String get(Locale locale, String key, Object... params)"),
            "TD-041 需要按语言 + 占位符取值：只有 get(Locale,key) 时带参文案会直接漏出 {0}");
        assertTrue(manager.contains("MessageFormat.format(template, params)"),
            "按语言的带参取值必须走 MessageFormat");
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /** 去掉行注释与块注释，避免注释里提到的旧写法造成误报。 */
    private static String withoutComments(String source) {
        StringBuilder out = new StringBuilder();
        for (String line : source.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                continue;
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }
}