package com.cashier.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 生产建表 DDL 一致性测试
 * 回归 P0-1/P0-2：DAO 读写 recharge_records / transactions.status 必须存在于
 * DatabaseManager 与 docker 00-init-complete.sql 两条生产建表通道中，防止再发生
 * "测试 H2 自建表通过、生产 DDL 缺表缺列"的漂移。
 */
@DisplayName("生产建表 DDL 一致性测试")
class ProductionSchemaConsistencyTest {

    /**
     * 截取某张表的完整建表 DDL 文本（从建表关键字到 ENGINE= 结束）。
     */
    private static String sliceTableDdl(String source, String createMarker) {
        int start = source.indexOf(createMarker);
        if (start < 0) {
            fail("生产 DDL 中未找到建表语句: " + createMarker);
        }
        int engine = source.indexOf("ENGINE=", start);
        int end = engine >= 0 ? engine : source.indexOf(';', start);
        if (end <= start) {
            fail("建表语句缺少结束标记: " + createMarker);
        }
        return source.substring(start, end);
    }

    @Test
    @DisplayName("recharge_records 表存在于两条生产建表通道（P0-1 回归）")
    void rechargeRecordsTableExistsInAllProductionDdl() throws Exception {
        String databaseManager = Files.readString(
            Path.of("src/main/java/com/cashier/util/DatabaseManager.java"));
        String initSql = Files.readString(
            Path.of("docker/mysql-init/00-init-complete.sql"));

        String dmDdl = sliceTableDdl(databaseManager,
            "CREATE TABLE IF NOT EXISTS recharge_records (");
        assertTrue(dmDdl.contains("record_id VARCHAR(50)"),
            "DatabaseManager 的 recharge_records 应含 record_id 列（DAO 主键）");
        assertTrue(dmDdl.contains("member_phone VARCHAR(20)"),
            "DatabaseManager 的 recharge_records 应含 member_phone 列");
        assertTrue(dmDdl.contains("timestamp TIMESTAMP"),
            "DatabaseManager 的 recharge_records 应含 TIMESTAMP 列");
        assertFalse(dmDdl.contains("operator_username"),
            "recharge_records 不应再沿用旧表 recharges 的 operator_username 列");

        String sqlDdl = sliceTableDdl(initSql, "CREATE TABLE IF NOT EXISTS recharge_records (");
        assertTrue(sqlDdl.contains("record_id VARCHAR(50)"),
            "docker 初始化 SQL 的 recharge_records 应含 record_id 列");
        assertTrue(sqlDdl.contains("timestamp TIMESTAMP"),
            "docker 初始化 SQL 的 recharge_records 应含 TIMESTAMP 列");
    }

    @Test
    @DisplayName("两条生产建表通道不再创建废弃的 recharges 表")
    void legacyRechargesTableRemovedFromProductionDdl() throws Exception {
        String databaseManager = Files.readString(
            Path.of("src/main/java/com/cashier/util/DatabaseManager.java"));
        String initSql = Files.readString(
            Path.of("docker/mysql-init/00-init-complete.sql"));

        assertFalse(databaseManager.contains("CREATE TABLE IF NOT EXISTS recharges ("),
            "DatabaseManager 不应再创建废弃的 recharges 表");
        assertFalse(initSql.contains("CREATE TABLE IF NOT EXISTS recharges ("),
            "docker 初始化 SQL 不应再创建废弃的 recharges 表");
    }

    @Test
    @DisplayName("transactions 表含 status 列（P0-2 回归）")
    void transactionsHasStatusColumnInAllProductionDdl() throws Exception {
        String databaseManager = Files.readString(
            Path.of("src/main/java/com/cashier/util/DatabaseManager.java"));
        String initSql = Files.readString(
            Path.of("docker/mysql-init/00-init-complete.sql"));

        String dmDdl = sliceTableDdl(databaseManager,
            "CREATE TABLE IF NOT EXISTS transactions (");
        assertTrue(dmDdl.contains("status VARCHAR(20)"),
            "DatabaseManager 的 transactions DDL 应含 status 列（REST 退款终态）");

        String sqlDdl = sliceTableDdl(initSql, "CREATE TABLE IF NOT EXISTS transactions (");
        assertTrue(sqlDdl.contains("status VARCHAR(20)"),
            "docker 初始化 SQL 的 transactions DDL 应含 status 列");
    }

    @Test
    @DisplayName("老库迁移路径包含 transactions.status 补列（P0-2 回归）")
    void upgradeTableStructureAddsTransactionsStatus() throws Exception {
        String databaseManager = Files.readString(
            Path.of("src/main/java/com/cashier/util/DatabaseManager.java"));

        assertTrue(databaseManager.contains("ensureColumn(stmt, \"transactions\", \"status\""),
            "upgradeTableStructure 应为老库补 transactions.status 列");
    }

    @Test
    @DisplayName("users 时间列在三处都是 BIGINT(epoch 毫秒)，DAO 不得再绑 Timestamp")
    void userTimestampColumnsUseEpochMillisEverywhere() throws Exception {
        String databaseManager = Files.readString(
            Path.of("src/main/java/com/cashier/util/DatabaseManager.java"));
        String initSql = Files.readString(
            Path.of("docker/mysql-init/00-init-complete.sql"));
        String testBase = Files.readString(
            Path.of("src/test/java/com/cashier/util/DatabaseTestBase.java"));

        for (String ddl : new String[]{
                sliceTableDdl(databaseManager, "CREATE TABLE IF NOT EXISTS users ("),
                sliceTableDdl(initSql, "CREATE TABLE IF NOT EXISTS users (")}) {
            assertTrue(ddl.contains("last_login_time BIGINT"),
                "users.last_login_time 应为 BIGINT（epoch 毫秒）");
            assertTrue(ddl.contains("create_time BIGINT"),
                "users.create_time 应为 BIGINT（epoch 毫秒）");
        }

        // H2 测试库必须与生产一致：写成 TIMESTAMP 时，Timestamp 绑定的缺陷会在 H2 上假装通过
        assertTrue(testBase.contains("last_login_time BIGINT"),
            "DatabaseTestBase 的 users.last_login_time 必须与生产一致（BIGINT），不能是 TIMESTAMP");
        assertTrue(testBase.contains("create_time BIGINT"),
            "DatabaseTestBase 的 users.create_time 必须与生产一致（BIGINT），不能是 TIMESTAMP");

        String userDao = Files.readString(
            Path.of("src/main/java/com/cashier/dao/UserDAORefactored.java"));
        assertFalse(userDao.contains("new Timestamp("),
            "UserDAORefactored 不得把 Timestamp 绑给 BIGINT 时间列："
                + "MySQL 会按 YYYYMMDDHHMMSS 数值化，读回被当成毫秒后显示成 2612 年");
    }

    @Test
    @DisplayName("偏好表不得对 username 加 users 外键（否则 default 全局默认值永远写不进）")
    void preferenceTablesHaveNoUsernameForeignKey() throws Exception {
        String databaseManager = Files.readString(
            Path.of("src/main/java/com/cashier/util/DatabaseManager.java"));
        String initSql = Files.readString(
            Path.of("docker/mysql-init/00-init-complete.sql"));

        // 这三张表除“每用户一行”外还用字面量 'default' 存全局默认值，'default' 不是真实用户
        for (String table : new String[]{"theme_preferences", "language_preferences", "font_size_preferences"}) {
            String marker = "CREATE TABLE IF NOT EXISTS " + table + " (";
            assertFalse(sliceTableDdl(databaseManager, marker).contains("REFERENCES users(username)"),
                "DatabaseManager 的 " + table + " 不应有 username 外键（需要存 default 全局默认值）");
            assertFalse(sliceTableDdl(initSql, marker).contains("REFERENCES users(username)"),
                "初始化 SQL 的 " + table + " 不应有 username 外键（需要存 default 全局默认值）");
        }

        assertTrue(databaseManager.contains("DROP FOREIGN KEY"),
            "需要保留老库迁移：删掉历史偏好表上的 username 外键");
    }
}
