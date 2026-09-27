package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
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
}
