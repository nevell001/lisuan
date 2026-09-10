package com.cashier.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code .env} 解析与写入测试。
 *
 * <p>应用需要自己读 {@code .env}，才能让 {@code java -jar}（不经过启动脚本）也用同一份
 * 本地密钥，从而不必把密码写回 {@code config/database.properties}。</p>
 */
@DisplayName("本地 .env 读写")
class DotEnvTest {

    @Test
    @DisplayName("解析：忽略注释与空行，去掉引号，重复键取第一条")
    void parseHandlesCommentsQuotesAndDuplicates() {
        Map<String, String> parsed = DotEnv.parse(List.of(
            "# 注释",
            "",
            "   ",
            "CASHIER_DB_PASSWORD=plain",
            "QUOTED=\"带 空格 的值\"",
            "SINGLE='single'",
            "EMPTY=",
            "WITH_EQUALS=a=b=c",
            "CASHIER_DB_PASSWORD=second-should-be-ignored",
            "NO_EQUALS_LINE",
            "=VALUE_WITHOUT_KEY"
        ));

        assertEquals("plain", parsed.get("CASHIER_DB_PASSWORD"));
        assertEquals("带 空格 的值", parsed.get("QUOTED"));
        assertEquals("single", parsed.get("SINGLE"));
        assertEquals("", parsed.get("EMPTY"));
        assertEquals("a=b=c", parsed.get("WITH_EQUALS"));
        assertFalse(parsed.containsKey("NO_EQUALS_LINE"));
        assertEquals(5, parsed.size());
    }

    @Test
    @DisplayName("写入：新增键、替换已有键，并保留其它键与注释")
    void upsertAddsReplacesAndPreservesOtherEntries(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, String.join("\n",
            "# 头部注释",
            "ENVIRONMENT=development",
            "TOKEN_SECRET=keep-me",
            "CASHIER_DB_PASSWORD=old",
            "CASHIER_DB_PASSWORD=duplicate",
            ""), StandardCharsets.UTF_8);

        DotEnv.upsert(envFile, DotEnv.DB_PASSWORD_KEY, "new-secret");

        String content = Files.readString(envFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("# 头部注释"), "注释应保留");
        assertTrue(content.contains("ENVIRONMENT=development"), "其它键应保留");
        assertTrue(content.contains("TOKEN_SECRET=keep-me"), "其它键应保留");
        assertTrue(content.contains("CASHIER_DB_PASSWORD=new-secret"), "密码应被替换");
        assertFalse(content.contains("duplicate"), "重复键应只留一条");
        assertFalse(content.contains("=old"), "旧密码不应残留");
    }

    @Test
    @DisplayName("写入：文件不存在时创建")
    void upsertCreatesFileWhenMissing(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");

        DotEnv.upsert(envFile, DotEnv.DB_PASSWORD_KEY, "created");

        assertTrue(Files.exists(envFile));
        assertEquals("created", DotEnv.parse(Files.readAllLines(envFile, StandardCharsets.UTF_8))
            .get(DotEnv.DB_PASSWORD_KEY));
    }
}
