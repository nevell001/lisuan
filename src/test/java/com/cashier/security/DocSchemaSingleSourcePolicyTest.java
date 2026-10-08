package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文档里的表结构不得成为"第二份 DDL"（2026-10）。
 *
 * <p>背景：`docker/mysql-init/*.sql` 有 {@code InitSchemaParityTest} 守着，但 **`docs/*.md` 里手写的
 * `CREATE TABLE` 谁都不守**。实测 {@code docs/DATABASE_INIT.md} 的 11 段 DDL 已漂移 4 张表——
 * {@code members} 缺 {@code id}/{@code member_code}/{@code version}（照它建表会把 {@code phone} 当主键、
 * 丢掉乐观锁列）、{@code transaction_items} 缺 {@code product_id}/{@code product_code}/{@code barcode}、
 * {@code categories} 缺 {@code id}、{@code operation_logs} 缺 4 个审计列；{@code PURCHASE_TABLE_DESIGN.md}
 * 另有 8 段同类副本。文档副本的危险在于**它是错的却看不出来**——人照着它写 SQL 会建出错误结构。</p>
 *
 * <p>因此本门禁钉住一条规则：{@code docs/} 下的 Markdown **不得内嵌 `CREATE TABLE` 语句**，
 * 表结构一律指向唯一真源（{@code DatabaseManager} 的 {@code createTable*} / 功能 DAO 的按需建表，
 * 镜像到 {@code docker/mysql-init/00-init-complete.sql}）。要说明表结构就用字段说明表。</p>
 *
 * <p>例外：正文里为了讲道理而**提到**这个短语（如"用 `CREATE TABLE IF NOT EXISTS` 幂等建表"）不算内嵌 DDL，
 * 判据是后面是否紧跟表名与左括号。</p>
 */
@DisplayName("文档不得内嵌第二份 DDL（2026-10）")
class DocSchemaSingleSourcePolicyTest {

    private static final Path DOCS = Path.of("docs");

    /** 真正的内嵌 DDL：`CREATE TABLE [IF NOT EXISTS] <表名> (`，忽略 Markdown 引用前缀与缩进。 */
    private static final Pattern INLINE_DDL = Pattern.compile(
        "CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+`?([A-Za-z_][A-Za-z0-9_]*)`?\\s*\\(",
        Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("docs/ 下的 Markdown 不得内嵌 CREATE TABLE 语句（表结构只有一个真源）")
    void docsDoNotEmbedSecondDdl() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (var stream = Files.walk(DOCS)) {
            for (Path file : stream.filter(p -> p.toString().endsWith(".md")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = INLINE_DDL.matcher(text);
                while (m.find()) {
                    offenders.add(DOCS.relativize(file) + " → CREATE TABLE " + m.group(1));
                }
            }
        }
        assertTrue(offenders.isEmpty(),
            "docs/ 里出现了内嵌的 CREATE TABLE（会成为不受门禁保护的\"第二份 DDL\"，必然漂移）。\n"
                + "请改为\"字段说明表 + 指向真源\"（真源：DatabaseManager 的 createTable* / "
                + "docker/mysql-init/00-init-complete.sql，由 InitSchemaParityTest 守着）：\n  "
                + String.join("\n  ", offenders));
    }

    @Test
    @DisplayName("已改为字段说明的两份文档要持续指向真源，且不得回退成 DDL")
    void schemaDocsPointAtSingleSource() throws IOException {
        // 这两份文档曾内嵌 DDL，现已改为字段说明 + 指向真源
        String[] migrated = {"DATABASE_INIT.md", "PURCHASE_TABLE_DESIGN.md"};
        for (String name : migrated) {
            Path file = DOCS.resolve(name);
            assertTrue(Files.exists(file), "文档不存在: " + name);
            String text = Files.readString(file, StandardCharsets.UTF_8);
            assertTrue(!INLINE_DDL.matcher(text).find(),
                name + " 不得再内嵌 CREATE TABLE（曾实测漂移 4 张表）");
            assertTrue(text.contains("InitSchemaParityTest") || text.contains("权威"),
                name + " 必须说明表结构的权威来源（否则读者无从知道去哪儿看真正的 DDL）");
        }
    }

    @Test
    @DisplayName("字段说明表必须覆盖真源的全部列（防说明表自身漂移）")
    void fieldTablesMatchRealSchema() throws IOException {
        String sql = Files.readString(Path.of("docker/mysql-init/00-init-complete.sql"),
            StandardCharsets.UTF_8);
        String doc = Files.readString(DOCS.resolve("DATABASE_INIT.md"), StandardCharsets.UTF_8);

        // 只取 "#### N. <表名> - ..." 到下一个 #### 之间，避免越过段落边界误读（曾因此误报）
        Pattern section = Pattern.compile("^#### \\d+\\. (\\w+) - .*?$(.*?)(?=^#### |\\z)",
            Pattern.MULTILINE | Pattern.DOTALL);
        Matcher sec = section.matcher(doc);
        List<String> problems = new ArrayList<>();
        int checked = 0;
        while (sec.find()) {
            String table = sec.group(1);
            Set<String> documented = new TreeSet<>();
            Matcher field = Pattern.compile("^\\|\\s*`([^`]+)`", Pattern.MULTILINE)
                .matcher(sec.group(2));
            while (field.find()) {
                for (String part : field.group(1).split("\\s*/\\s*")) {
                    if (!part.isBlank()) {
                        documented.add(part.trim().toLowerCase());
                    }
                }
            }
            Set<String> real = realColumns(sql, table);
            if (real.isEmpty()) {
                continue; // 该表不在 SQL 镜像里（可能只在 Java 侧），跳过
            }
            checked++;
            Set<String> missing = new TreeSet<>(real);
            missing.removeAll(documented);
            if (!missing.isEmpty()) {
                problems.add(table + " 的字段说明漏了真源里的列: " + missing);
            }
        }
        assertTrue(checked >= 10, "应至少核对到 10 张表，实际 " + checked + "（解析可能失效）");
        assertTrue(problems.isEmpty(),
            "docs/DATABASE_INIT.md 的字段说明与真源不一致（说明表自身也会漂移）：\n  "
                + String.join("\n  ", problems));
    }

    /** 从 SQL 里取某张表的列名（配对小括号，只看字段定义行）。 */
    private static Set<String> realColumns(String sql, String table) {
        Matcher m = Pattern.compile(
            "CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+`?" + Pattern.quote(table) + "`?\\s*\\(",
            Pattern.CASE_INSENSITIVE).matcher(sql);
        if (!m.find()) {
            return Set.of();
        }
        String rest = sql.substring(m.end() - 1);
        int depth = 0;
        int end = 0;
        for (int i = 0; i < rest.length(); i++) {
            char ch = rest.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }
        Set<String> cols = new TreeSet<>();
        Pattern col = Pattern.compile(
            "^\\s*'?\\s*`?([A-Za-z_][A-Za-z0-9_]*)`?\\s+"
                + "(INT|BIGINT|VARCHAR|DECIMAL|TEXT|TIMESTAMP|DATETIME|DATE|TINYINT|BOOLEAN|CHAR|DOUBLE)\\b",
            Pattern.CASE_INSENSITIVE);
        for (String line : rest.substring(1, end).split("\n")) {
            Matcher cm = col.matcher(line);
            if (cm.find()) {
                cols.add(cm.group(1).toLowerCase());
            }
        }
        return cols;
    }
}