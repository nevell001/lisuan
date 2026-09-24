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
}
