package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 深色主题下"大面积用色"的克制（2026-09 用户反馈）。
 *
 * <p>现象：深色模式下交班管理页面有一条刺眼的橙色横条。根因是——其它页面的顶部栏都用共享的
 * {@code styleClass="toolbar"}（深色下 {@code #383838} 中性灰），**只有交班页**用了自己的
 * {@code shift-toolbar}，而深色主题把它覆盖成 {@code -lisuan-primary-muted}({@code #B85C1B})
 * ——饱和橙在近黑背景上非常抢眼；同一页的 {@code .shift-separator} 在深色主题里**没有覆盖**，
 * 沿用了基础样式的品牌边框色 {@code -lisuan-primary-border}({@code #A3571E})，又是一条橙线。</p>
 *
 * <p>品牌色做大面积背景在浅色主题是有意的页面识别色，但深色主题必须降饱和/降明度，
 * 所以这条门禁钉住"深色下这两处不得再用饱和品牌色"。</p>
 */
@DisplayName("深色主题大面积用色门禁")
class DarkThemeSurfacePolicyTest {

    private static final Path DARK = Path.of("src/main/resources/css/dark-theme.css");

    @Test
    @DisplayName("交班页顶部栏与分隔条在深色模式下不得用饱和品牌橙")
    void shiftPageSurfacesAreCalmInDarkMode() throws Exception {
        String dark = Files.readString(DARK);

        String toolbar = ruleBody(dark, ".shift-toolbar");
        assertFalse(toolbar.isBlank(), "深色主题必须为 .shift-toolbar 提供覆盖规则");
        assertFalse(toolbar.contains("-lisuan-primary-muted"),
            "深色主题里交班页顶部栏若用 -lisuan-primary-muted(#B85C1B)，在近黑背景上就是一条刺眼的"
                + "橙色横条（用户实测反馈）——请改用深品牌色 -lisuan-primary-darker 或中性色");
        assertTrue(toolbar.contains("-lisuan-primary-darker") || toolbar.contains("-lisuan-border"),
            "深色主题里交班页顶部栏应使用深品牌色（-lisuan-primary-darker）或中性色");

        String separator = ruleBody(dark, ".shift-separator");
        assertFalse(separator.isBlank(),
            "深色主题必须为 .shift-separator 提供覆盖规则：基础样式用的是品牌边框色"
                + " -lisuan-primary-border(#A3571E)，深色下就是一条橙线");
        assertTrue(separator.contains("-lisuan-border"),
            "深色主题里分隔条应用中性边框色（-lisuan-border/-lisuan-border-strong）");
    }

    /** 取某条规则的选择器体内文本（忽略注释）。 */
    private static String ruleBody(String css, String selector) {
        Matcher matcher = Pattern.compile("(?s)" + Pattern.quote(selector) + "\\s*\\{([^}]*)}").matcher(css);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * 基础样式里凡是"简单类选择器 + 硬编码亮色背景"的规则，深色主题都必须覆盖。
     *
     * <p>这类规则用的是字面量（不是主题变量），所以不会随主题变化：浅色主题下是浅底提示块，
     * 深色主题下就是近黑背景上的一块亮色——正是交班页橙色横条那类问题的通用形式。
     * 2026-09 按此规则扫出 6 条（`.error-label`/`.validation-error`/`.validation-warning`/
     * `.product-info-card`/`.bg-warning`/`.bg-danger`），前三个还有页面在用（登录页错误提示、
     * 表单校验、补货页信息卡），已全部补上深色覆盖。</p>
     */
    @Test
    @DisplayName("硬编码亮色背景的类都必须在深色主题里有覆盖")
    void brightBackgroundClassesHaveDarkOverrides() throws Exception {
        String base = Files.readString(Path.of("src/main/resources/css/styles.css"));
        String dark = Files.readString(DARK);
        Pattern simpleRule = Pattern.compile("(?m)^(\\.[\\w-]+)\\s*\\{([^}]*)}");
        Pattern bright = Pattern.compile(
            "-fx-background-color:\\s*(white|#(?:fff|ffffff|f[0-9a-f]{5}))", Pattern.CASE_INSENSITIVE);
        List<String> missing = new ArrayList<>();
        int candidates = 0;
        Matcher rule = simpleRule.matcher(base);
        while (rule.find()) {
            if (!bright.matcher(rule.group(2)).find()) {
                continue;
            }
            candidates++;
            String selector = rule.group(1);
            if (!dark.contains(selector)) {
                missing.add(selector);
            }
        }
        assertTrue(candidates > 20,
            "只找到 " + candidates + " 条\"简单类 + 硬编码亮色背景\"规则（2026-09 实际 55 条），识别规则可能失效");
        assertTrue(missing.isEmpty(),
            "以下类的背景是硬编码亮色（不随主题变），深色主题里没有覆盖规则——在近黑背景上会是一块亮色，"
                + "请像 .shift-toolbar 那样加深色覆盖：\n  " + String.join("\n  ", missing));
    }
}
