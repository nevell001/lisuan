package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeStylePolicyTest {
    private static final List<String> THEME_COLOR_PROPERTIES = List.of(
        "-fx-background-color", "-fx-text-fill", "-fx-border-color"
    );

    /** 声明界面正文字体的样式表（不含 package-wizard 等独立窗口） */
    private static final List<String> UI_FONT_STYLESHEETS = List.of(
        "styles.css", "lisuan-theme.css", "light-theme.css", "dark-theme.css", "splash.css"
    );

    @Test
    @DisplayName("界面字体：每个 CJK 字体栈的首项必须等于 Java 侧校验的族名")
    void uiFontStacksStartWithTheBundledFamily() throws IOException {
        String app = Files.readString(Path.of(
            "src/main/java/com/cashier/CashierSystemFXApplication.java"
        ));
        Matcher declaration = Pattern.compile("UI_FONT_FAMILY\\s*=\\s*\"([^\"]+)\"").matcher(app);
        assertTrue(declaration.find(),
            "CashierSystemFXApplication 必须定义 UI_FONT_FAMILY 常量（启动时按它校验字体是否注册成功）");
        String bundled = declaration.group(1);

        List<String> violations = new ArrayList<>();
        for (String file : UI_FONT_STYLESHEETS) {
            String css = Files.readString(Path.of("src/main/resources/css/" + file));
            Matcher families = Pattern.compile("-fx-font-family:\\s*([^;]+);").matcher(css);
            while (families.find()) {
                String value = families.group(1).replaceAll("\\s+", " ").trim();
                // 只看界面正文字体栈，跳过 FontAwesome / Consolas 这类专用字体
                if (!value.contains("Noto") && !value.contains("YaHei") && !value.contains("SimHei")) {
                    continue;
                }
                int comma = value.indexOf(',');
                String first = (comma < 0 ? value : value.substring(0, comma)).replace("\"", "").trim();
                if (!first.equals(bundled)) {
                    violations.add(file + ": 首个族名是 \"" + first + "\"，应为 \"" + bundled + "\"");
                }
            }
        }

        assertTrue(violations.isEmpty(),
            "JavaFX 只使用 -fx-font-family 的第一个族名（不会遍历回退列表），首项对不上会静默"
                + "回退到平台默认字体（Windows 上是 Microsoft YaHei UI）：\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("FXML 和控制器不得用内联颜色覆盖主题")
    void viewsAndControllersDoNotOverrideThemeColorsInline() throws IOException {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        List<String> violations = new ArrayList<>();

        inspectFiles(projectRoot.resolve("src/main/resources/com/cashier/view"), ".fxml", violations);
        inspectFiles(projectRoot.resolve("src/main/java/com/cashier/controller"), ".java", violations);

        assertTrue(violations.isEmpty(),
            "请改用语义 styleClass，并在主题 CSS 中定义颜色：\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("LiSuan 菜单选中态使用主题主色")
    void lisuanSelectedNavigationUsesBrandPalette() throws IOException {
        String css = Files.readString(Path.of(
            "src/main/resources/css/lisuan-theme.css"
        ));

        assertTrue(css.contains(".sidebar .nav-button-active") || css.contains(".sidebar .nav-button.active"));
        assertTrue(css.contains("-fx-background-color: #B85C1B;") || css.contains("-fx-background-color: -lisuan-primary;"));
        assertTrue(css.contains("-fx-text-fill: #FFFFFF;") || css.contains("-fx-text-fill: -lisuan-surface;"));
        assertTrue(css.contains(".menu-button:showing"));
        assertTrue(css.contains(".menu-button:pressed"));
        assertTrue(css.contains("-fx-background-color: #FFEAD8;") || css.contains("-fx-background-color: -lisuan-surface-selected;"));
    }

    @Test
    @DisplayName("LiSuan 主题应覆盖快捷键帮助窗口的紫色默认样式")
    void lisuanShortcutHelpUsesBrandPalette() throws IOException {
        String css = Files.readString(Path.of(
            "src/main/resources/css/lisuan-theme.css"
        ));

        assertTrue(css.contains(".shortcut-help-view .header-bar"));
        assertTrue(css.contains(".shortcut-help-view .shortcut-key"));
        assertTrue(css.contains("-fx-background-color: #B85C1B;") || css.contains("-fx-background-color: -lisuan-primary;"));
        assertTrue(css.contains("-fx-text-fill: #8F4314;") || css.contains("-fx-text-fill: -lisuan-primary-active;"));
        assertTrue(css.contains("-fx-background-color: linear-gradient(to bottom, #FFF7EF, #FFEAD8);") 
                || css.contains("-fx-background-color: linear-gradient(to bottom, -lisuan-surface-muted, -lisuan-surface-selected);"));
    }

    private void inspectFiles(Path directory, String suffix, List<String> violations) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(path -> path.toString().endsWith(suffix)).forEach(path -> {
                try {
                    List<String> lines = Files.readAllLines(path);
                    boolean insideSetStyle = false;
                    for (int index = 0; index < lines.size(); index++) {
                        String line = lines.get(index);
                        if (line.contains("setStyle(")) {
                            insideSetStyle = true;
                        }
                        if ((line.contains("style=\"") || insideSetStyle)
                            && THEME_COLOR_PROPERTIES.stream().anyMatch(line::contains)) {
                            violations.add(projectRootRelative(path) + ":" + (index + 1));
                        }
                        if (insideSetStyle && line.contains(");")) {
                            insideSetStyle = false;
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("无法检查主题样式文件: " + path, e);
                }
            });
        }
    }

    private String projectRootRelative(Path path) {
        return Path.of(System.getProperty("user.dir")).relativize(path).toString();
    }

    @Test
    @DisplayName("交接班页工具栏按钮配色在深浅主题下均达 WCAG AA（≥4.5:1）")
    void shiftViewToolbarMeetsContrast() throws IOException {
        String base = Files.readString(Path.of("src/main/resources/css/styles.css"));
        String dark = Files.readString(Path.of("src/main/resources/css/dark-theme.css"));
        String lisuan = Files.readString(Path.of("src/main/resources/css/lisuan-theme.css"));

        List<String> failures = new ArrayList<>();
        // 工具栏按钮自身填充 + 白字：开班(#2E7D32)、交班(#BF360C)
        checkContrast(failures, "开班按钮", "#FFFFFF", background(base, ".shift-btn-primary {"), 4.5);
        checkContrast(failures, "交班按钮", "#FFFFFF", background(base, ".shift-btn-warning {"), 4.5);

        // 次级按钮是"深色半透明叠在工具栏底色上"，两种主题的工具栏底色都要够深
        String translucent = background(base, ".shift-btn {");
        checkContrast(failures, "次级按钮(浅色主题工具栏)",
            "#FFFFFF", blendOver(translucent, themeValue(lisuan, "-lisuan-primary"), 0.25), 4.5);
        checkContrast(failures, "次级按钮(深色主题工具栏)",
            "#FFFFFF", blendOver(translucent, themeValue(dark, "-lisuan-primary-muted"), 0.25), 4.5);

        // 工具栏标题白字
        checkContrast(failures, "工具栏标题(浅色主题)",
            "#FFFFFF", themeValue(lisuan, "-lisuan-primary"), 4.5);
        checkContrast(failures, "工具栏标题(深色主题)",
            "#FFFFFF", themeValue(dark, "-lisuan-primary-muted"), 4.5);

        assertTrue(failures.isEmpty(),
            "交接班页配色不达 WCAG AA 4.5:1：\n" + String.join("\n", failures));
    }

    @Test
    @DisplayName("筛选栏按钮跟随主题，禁止白底白字")
    void shiftFilterBarButtonsFollowTheme() throws IOException {
        String base = Files.readString(Path.of("src/main/resources/css/styles.css"));
        String block = cssBlock(base, ".shift-filter-bar .shift-btn {");
        assertTrue(!block.isBlank(), "筛选栏按钮必须有独立的主题化规则（工具栏按钮的半透明白底在浅色筛选栏上会白字白底）");
        assertTrue(block.contains("-fx-text-fill: -lisuan-text;"),
            "筛选栏按钮文字应使用 -lisuan-text 跟随主题，而不是硬编码 white");
    }

    private void checkContrast(List<String> failures, String label, String foreground, String background, double min) {
        double ratio = contrastRatio(foreground, background);
        if (ratio < min) {
            failures.add(String.format("%s: %s on %s = %.2f:1（需 ≥%.1f:1）", label, foreground, background, ratio, min));
        }
    }

    private String cssBlock(String css, String selectorLine) {
        int start = css.indexOf(selectorLine);
        if (start < 0) {
            return "";
        }
        int end = css.indexOf('}', start);
        return end < 0 ? "" : css.substring(start, end + 1);
    }

    private String background(String css, String selectorLine) {
        String block = cssBlock(css, selectorLine);
        Matcher matcher = Pattern.compile("-fx-background-color:\\s*([^;]+);").matcher(block);
        assertTrue(matcher.find(), "缺少 -fx-background-color: " + selectorLine);
        return matcher.group(1).trim();
    }

    private String themeValue(String css, String variable) {
        Matcher matcher = Pattern.compile(Pattern.quote(variable) + ":\\s*(#[0-9a-fA-F]{6})").matcher(css);
        assertTrue(matcher.find(), "主题缺少变量 " + variable);
        return matcher.group(1);
    }

    /** 把 rgba(0,0,0,a) 之类的半透明色叠到不透明底色上 */
    private String blendOver(String overlay, String baseColor, double expectedAlpha) {
        Matcher matcher = Pattern.compile("rgba\\(([^)]+)\\)").matcher(overlay);
        assertTrue(matcher.find(), "需要半透明填充色用于合成：" + overlay);
        String[] parts = matcher.group(1).split(",");
        double alpha = parts.length > 3 ? Double.parseDouble(parts[3].trim()) : expectedAlpha;
        int[] base = rgb(baseColor);
        int[] over = new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())};
        StringBuilder hex = new StringBuilder("#");
        for (int i = 0; i < 3; i++) {
            hex.append(String.format("%02X", (int) Math.round(over[i] * alpha + base[i] * (1 - alpha))));
        }
        return hex.toString();
    }

    private int[] rgb(String hex) {
        String value = hex.replace("#", "");
        return new int[]{
            Integer.parseInt(value.substring(0, 2), 16),
            Integer.parseInt(value.substring(2, 4), 16),
            Integer.parseInt(value.substring(4, 6), 16)
        };
    }

    private double contrastRatio(String a, String b) {
        double la = luminance(rgb(a));
        double lb = luminance(rgb(b));
        double lighter = Math.max(la, lb);
        double darker = Math.min(la, lb);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double luminance(int[] rgb) {
        double[] channels = new double[3];
        for (int i = 0; i < 3; i++) {
            double channel = rgb[i] / 255.0;
            channels[i] = channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
    }
}
