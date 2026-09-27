package com.cashier.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * docker 初始化脚本与 Java 建表的一致性门禁（TD-010）。
 *
 * <p>{@code docker/mysql-init/*.sql} 被当作"完整初始化"给 DBA/BI 用，而应用启动时
 * ({@code DatabaseManager} + 各 DAO 的 {@code createTable()}) 才是真正的事实来源。
 * 此前脚本缺 9 张表（发票/备份/支付/挂单/登录尝试/设置）并多 5 张全仓库无引用的历史表，
 * 靠"启动自愈"掩盖着。本类把两侧的表集合与列集合锁在一起。</p>
 */
@DisplayName("初始化脚本一致性门禁")
class InitSchemaParityTest {

    /**
     * 只在初始化脚本里存在、且当前 Java 侧没有任何代码引用的历史遗留表。
     *
     * <p>保留建表语句只为兼容仍在使用这些表的旧库/BI 取数；新装部署不需要。
     * 这个白名单由 {@link #legacyWhitelistMatchesReality()} 与
     * {@link #legacyTablesHaveNoJavaReference()} 两重守护——新增无引用表必须显式登记并说明理由。</p>
     */
    private static final Set<String> LEGACY_TABLES = Set.of(
        "specifications", "specification_values", "product_specifications",
        "export_history", "export_templates");

    private static final Pattern CREATE_TABLE = Pattern.compile(
        "CREATE TABLE(?: IF NOT EXISTS)?\\s+`?(\\w+)`?\\s*\\((.*?)\\)\\s*(?:ENGINE|\"\"\")",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern COLUMN_LINE = Pattern.compile(
        "^`?([A-Za-z_][A-Za-z0-9_]*)`?\\s+([A-Za-z]+(?:\\([^)]*\\))?)");

    private static final Pattern NON_COLUMN = Pattern.compile(
        "^(PRIMARY\\s+KEY|UNIQUE|INDEX|KEY|CONSTRAINT|FOREIGN|CHECK|FULLTEXT|SPATIAL)\\b",
        Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("初始化脚本必须建出 Java 侧会建的每一张表（历史遗留表除外）")
    void initScriptCreatesEveryJavaTable() throws Exception {
        Set<String> missing = new TreeSet<>(javaTables().keySet());
        missing.removeAll(sqlTables().keySet());
        assertTrue(missing.isEmpty(),
            "docker/mysql-init 缺少这些 Java 侧会建的表：" + missing
                + "（按本脚本初始化出来的库结构不完整，只能靠应用启动自愈）");
    }

    @Test
    @DisplayName("共有表的列集合与列类型必须一致（两套库结构不同就只能靠启动自愈）")
    void commonTablesHaveSameColumnsAndTypes() throws Exception {
        Map<String, Map<String, String>> java = javaTables();
        Map<String, Map<String, String>> sql = sqlTables();
        StringBuilder drift = new StringBuilder();
        for (String table : new TreeSet<>(java.keySet())) {
            if (!sql.containsKey(table)) {
                continue; // 由 initScriptCreatesEveryJavaTable 负责报
            }
            Set<String> onlyJava = new TreeSet<>(java.get(table).keySet());
            onlyJava.removeAll(sql.get(table).keySet());
            Set<String> onlySql = new TreeSet<>(sql.get(table).keySet());
            onlySql.removeAll(java.get(table).keySet());
            if (!onlyJava.isEmpty() || !onlySql.isEmpty()) {
                drift.append(table).append("（缺列 ").append(onlyJava)
                     .append("；多列 ").append(onlySql).append("）; ");
            }
            for (String column : new TreeSet<>(java.get(table).keySet())) {
                String javaType = java.get(table).get(column);
                String sqlType = sql.get(table).get(column);
                if (sqlType != null && !javaType.equals(sqlType)) {
                    drift.append(table).append('.').append(column).append("（Java ").append(javaType)
                         .append(" vs SQL ").append(sqlType).append("）; ");
                }
            }
        }
        assertTrue(drift.isEmpty(), "初始化脚本与 Java 建表的列/类型不一致: " + drift);
    }

    @Test
    @DisplayName("历史遗留表白名单必须与实际情况一致（新增无引用表要显式登记）")
    void legacyWhitelistMatchesReality() throws Exception {
        Set<String> sqlOnly = new TreeSet<>(sqlTables().keySet());
        sqlOnly.removeAll(javaTables().keySet());
        assertEquals(new TreeSet<>(LEGACY_TABLES), sqlOnly,
            "初始化脚本里「Java 侧没有的表」必须与 LEGACY_TABLES 白名单完全一致："
                + "多出来的表要么补进 Java 建表、要么登记为历史遗留并说明理由");
    }

    @Test
    @DisplayName("登记的历史遗留表确实没有任何 Java 代码引用")
    void legacyTablesHaveNoJavaReference() throws Exception {
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java"))) {
            sources = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        for (String table : new TreeSet<>(LEGACY_TABLES)) {
            Pattern usage = Pattern.compile(
                "(FROM|JOIN|INTO|UPDATE|TABLE)\\s+`?" + Pattern.quote(table) + "`?\\b",
                Pattern.CASE_INSENSITIVE);
            for (Path source : sources) {
                String text = Files.readString(source);
                assertFalse(usage.matcher(text).find(),
                    source + " 引用了历史遗留表 " + table + "：要么把它补回 Java 建表，"
                        + "要么从 LEGACY_TABLES 与服务端脚本里一起删掉");
            }
        }
    }

    @Test
    @DisplayName("初始化脚本至少含这些关键表（防止脚本被截断/回退成旧版本）")
    void initScriptStillContainsCoreTables() throws Exception {
        Set<String> sql = sqlTables().keySet();
        for (String table : List.of("products", "transactions", "transaction_items", "users", "members",
                "invoices", "invoice_items", "payment_orders", "refund_records", "hold_orders",
                "backup_records", "settings", "login_attempts", "return_orders", "purchase_orders")) {
            assertTrue(sql.contains(table), "初始化脚本缺少核心表: " + table);
        }
    }

    // ===== 解析 =====

    /** Java 侧的事实来源：DatabaseManager + 各 DAO 自建的表。 */
    private static Map<String, Map<String, String>> javaTables() throws IOException {
        Map<String, Map<String, String>> tables = new LinkedHashMap<>();
        parseInto(tables, Path.of("src/main/java/com/cashier/util/DatabaseManager.java"));
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java/com/cashier/dao"))) {
            for (Path source : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                parseInto(tables, source);
            }
        }
        return tables;
    }

    /** 初始化脚本侧：docker/mysql-init 下的全部 .sql。 */
    private static Map<String, Map<String, String>> sqlTables() throws IOException {
        Map<String, Map<String, String>> tables = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(Path.of("docker/mysql-init"))) {
            for (Path script : walk.filter(p -> p.toString().endsWith(".sql")).toList()) {
                parseInto(tables, script);
            }
        }
        return tables;
    }

    /** 解析出「表名 -> (列名 -> 规范化类型)」。 */
    private static void parseInto(Map<String, Map<String, String>> tables, Path source) throws IOException {
        Matcher matcher = CREATE_TABLE.matcher(Files.readString(source));
        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase();
            Map<String, String> columns = new LinkedHashMap<>();
            for (String raw : matcher.group(2).split("\n")) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("--") || NON_COLUMN.matcher(line).find()) {
                    continue;
                }
                line = line.replaceAll(",$", "");
                Matcher column = COLUMN_LINE.matcher(line);
                if (column.find()) {
                    columns.put(column.group(1).toLowerCase(), normalizeType(column.group(2)));
                }
            }
            tables.putIfAbsent(table, columns);
        }
    }

    /** 类型比较前的规范化：大写、去空格（DECIMAL(10, 2) 与 DECIMAL(10,2) 视为同一类型）。 */
    private static String normalizeType(String type) {
        return type.toUpperCase().replace(" ", "");
    }
}
