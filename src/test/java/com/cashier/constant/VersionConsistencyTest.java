package com.cashier.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 版本号一致性门禁（TD-013）。
 *
 * <p>版本号有四处来源，原来只有 {@code release.sh}/{@code release.bat} 里的
 * "pom.xml ↔ AppConstants" 两条比对：发布脚本只在本地发版时跑，CI（{@code mvn verify}）
 * 完全看不到，而且 {@code installer/Installer.java} 与 {@code .env.example} 根本没纳入。
 * 现在改成 JUnit 门禁，CI 每次构建都守：四处不一致即失败，并报出每处的实际值。</p>
 */
@DisplayName("版本号四处一致门禁")
class VersionConsistencyTest {

    @Test
    @DisplayName("pom.xml / AppConstants / Installer / .env.example 的版本号必须一致")
    void versionIsConsistentAcrossAllSources() throws Exception {
        Map<String, String> versions = new LinkedHashMap<>();

        // 1) 运行时真值：AppConstants.APP_VERSION
        versions.put("AppConstants.APP_VERSION", AppConstants.APP_VERSION);

        // 2) pom.xml 的项目版本（取 <project> 下的第一个 <version>）
        String pom = Files.readString(Path.of("pom.xml"));
        Matcher pomVersion = Pattern.compile("<version>([^<]+)</version>").matcher(pom);
        assertTrue(pomVersion.find(), "pom.xml 里找不到项目版本号");
        versions.put("pom.xml", pomVersion.group(1).trim());

        // 3) 安装器里的版本常量（安装包标题会直接显示它）
        String installer = Files.readString(Path.of("src/main/java/com/cashier/installer/Installer.java"));
        versions.put("installer/Installer.java", extract(installer,
            "private static final String APP_VERSION = \"([^\"]+)\""));

        // 4) .env.example 的 APP_VERSION（部署脚本据此展示版本）
        String envExample = Files.readString(Path.of(".env.example"));
        versions.put(".env.example", extract(envExample, "(?m)^APP_VERSION=(.+)$"));

        String expected = AppConstants.APP_VERSION;
        for (Map.Entry<String, String> entry : versions.entrySet()) {
            assertEquals(expected, entry.getValue(),
                "版本号不一致：" + entry.getKey() + " = " + entry.getValue()
                    + "，但 AppConstants.APP_VERSION = " + expected + "。四处必须同步（" + versions + "）");
        }

        // 空版本号或占位符也算失败
        assertFalse(expected.isBlank(), "版本号不能为空");
    }

    @Test
    @DisplayName("发布脚本必须比对四处的版本号（不能只比 pom 与 AppConstants）")
    void releaseScriptsCheckEverySource() throws Exception {
        String sh = Files.readString(Path.of("release.sh"));
        String bat = Files.readString(Path.of("release.bat"));

        // 必须是"比对列表"里出现，而不是只在读取变量时提到（只提到来源、不参与比对等于没查）
        assertTrue(sh.contains("installer/Installer.java:$VERSION_INSTALLER"),
            "release.sh 的四处比对列表必须包含 installer/Installer.java");
        assertTrue(sh.contains(".env.example:$VERSION_ENV"),
            "release.sh 的四处比对列表必须包含 .env.example");
        assertTrue(bat.contains("\"installer\\Installer.java=%VERSION_INSTALLER%\""),
            "release.bat 的四处比对列表必须包含 installer/Installer.java");
        assertTrue(bat.contains("\".env.example=%VERSION_ENV%\""),
            "release.bat 的四处比对列表必须包含 .env.example");
    }

    private static String extract(String source, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(source);
        assertTrue(matcher.find(), "找不到版本号，正则: " + regex);
        return matcher.group(1).trim();
    }
}
