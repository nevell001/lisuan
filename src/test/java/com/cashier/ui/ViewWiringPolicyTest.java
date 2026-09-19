package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FXML 视图接线门禁。
 *
 * <p>回归：`PosModeView.fxml` + `PosModeController` 曾经是一整套「POS 模式外壳」，
 * 后来 <code>switchToPosModeView</code> 改成直接加载 `TouchCartView.fxml`，
 * 这套视图就再也没被任何 Java 代码加载过——文件还在、控制器还在、
 * `instanceof PosModeController` 的清理分支还在，成了一段谁都不敢删的死结构，
 * 且它的注释还在误导人（说触屏台"不含挂单/交接班，由 PosModeView 底栏处理"）。</p>
 *
 * <p>此门禁双向校验：视图文件必须有人加载；Java 里写到的视图路径必须真实存在。</p>
 */
@DisplayName("FXML 视图接线测试")
class ViewWiringPolicyTest {

    private static final Path VIEW_DIR = Path.of("src/main/resources/com/cashier/view");
    private static final Pattern VIEW_PATH = Pattern.compile("com/cashier/view/([A-Za-z0-9_]+\\.fxml)");

    /**
     * 已存在但尚未接线的视图（不要因为"没人加载"就删掉——这是待接的需求，不是死代码）：
     * 强制改密流程（`users.force_password_change`）目前没有 UI 入口，
     * `PasswordResetView.fxml` + `PasswordResetController` 就是为它准备的。
     */
    private static final Set<String> KNOWN_UNWIRED = Set.of("PasswordResetView.fxml");

    @Test
    @DisplayName("每个视图都要被 Java 代码加载，或明确登记为未接线的待建功能")
    void everyViewIsLoadedOrKnownUnwired() throws IOException {
        Set<String> loaded = new HashSet<>();
        Matcher matcher = VIEW_PATH.matcher(allJavaSources());
        while (matcher.find()) {
            loaded.add(matcher.group(1));
        }

        List<String> orphans = new ArrayList<>();
        try (var views = Files.list(VIEW_DIR)) {
            for (Path view : views.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                String name = view.getFileName().toString();
                if (!loaded.contains(name) && !KNOWN_UNWIRED.contains(name)) {
                    orphans.add(name);
                }
            }
        }

        assertTrue(orphans.isEmpty(),
            "以下视图没有任何 Java 代码加载（死视图，应删除或登记到 KNOWN_UNWIRED）：" + orphans);
    }

    @Test
    @DisplayName("Java 里引用的视图文件必须存在")
    void referencedViewsExist() throws IOException {
        List<String> missing = new ArrayList<>();
        Matcher matcher = VIEW_PATH.matcher(allJavaSources());
        Set<String> checked = new HashSet<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (checked.add(name) && !Files.exists(VIEW_DIR.resolve(name))) {
                missing.add(name);
            }
        }

        assertTrue(missing.isEmpty(), "Java 引用了不存在的视图文件：" + missing);
    }

    private static String allJavaSources() throws IOException {
        StringBuilder sources = new StringBuilder();
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                sources.append(Files.readString(file)).append('\n');
            }
        }
        return sources.toString();
    }
}
