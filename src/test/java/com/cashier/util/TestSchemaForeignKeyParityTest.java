package com.cashier.util;

import com.cashier.dao.DAOFactory;
import com.cashier.model.OperationLog;
import com.cashier.model.Transaction;
import com.cashier.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 测试库 schema 与生产的一致性门禁（TD-009）。
 *
 * <p>此前测试用的 H2 schema 一个外键都没有（生产有 14 个），于是"悬空外键""删用户改写历史"
 * 这类问题在 CI 里永远测不出来——本类把两者的外键集合锁在一起，并验证约束真的生效。</p>
 */
@DisplayName("测试库 schema 门禁")
class TestSchemaForeignKeyParityTest extends DatabaseTestBase {

    /** 生产 DDL 里声明外键的表与列（{@code DatabaseManager}）。 */
    private static Set<String> productionForeignKeys() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/cashier/util/DatabaseManager.java"));
        Set<String> keys = new HashSet<>();
        Matcher tables = Pattern.compile(
            "CREATE TABLE IF NOT EXISTS (\\w+)\\s*\\((.*?)\\)\\s*ENGINE", Pattern.DOTALL).matcher(source);
        while (tables.find()) {
            Matcher fks = Pattern.compile(
                "FOREIGN KEY \\((\\w+)\\)\\s+REFERENCES\\s+(\\w+)\\((\\w+)\\)").matcher(tables.group(2));
            while (fks.find()) {
                keys.add(tables.group(1) + "." + fks.group(1) + "->" + fks.group(2) + "(" + fks.group(3) + ")");
            }
        }
        return keys;
    }

    /** 测试 schema 里登记的外键（{@code DatabaseTestBase.TEST_SCHEMA_FOREIGN_KEYS}）。 */
    private static Set<String> testForeignKeys() throws Exception {
        String source = Files.readString(Path.of("src/test/java/com/cashier/util/DatabaseTestBase.java"));
        Set<String> keys = new HashSet<>();
        Matcher entries = Pattern.compile(
            "\\{\"fk_\\w+\",\\s*\"(\\w+)\",\\s*\"(\\w+)\",\\s*\"(\\w+)\\((\\w+)\\)\"").matcher(source);
        while (entries.find()) {
            keys.add(entries.group(1) + "." + entries.group(2) + "->" + entries.group(3) + "(" + entries.group(4) + ")");
        }
        return keys;
    }

    @Test
    @DisplayName("测试库外键集合必须与生产 DDL 完全一致")
    void testSchemaForeignKeysMatchProduction() throws Exception {
        Set<String> production = productionForeignKeys();
        Set<String> test = testForeignKeys();

        assertTrue(production.size() >= 14, "生产 DDL 外键数量异常: " + production.size());
        assertEquals(production, test,
            "测试库外键与生产不一致（缺：" + minus(production, test) + "；多：" + minus(test, production) + "）");
    }

    @Test
    @DisplayName("审计归属列（谁做的）不得挂外键：删用户/改名不得改写历史")
    void auditAttributionColumnsHaveNoForeignKey() throws Exception {
        for (Set<String> keys : List.of(productionForeignKeys(), testForeignKeys())) {
            assertFalse(keys.stream().anyMatch(k -> k.startsWith("transactions.operator_username->")),
                "transactions.operator_username 不得挂外键：ON DELETE SET NULL 会把历史交易的收银员抹掉");
            assertFalse(keys.stream().anyMatch(k -> k.startsWith("operation_logs.username->")),
                "operation_logs.username 不得挂外键：会让「写显示名」的审计调用直接报错，删用户又会抹掉归属");
        }
    }

    @Test
    @DisplayName("测试库外键真的生效：悬空引用必须被拒绝")
    void danglingReferenceIsRejected() {
        SQLException error = assertThrows(SQLException.class, () -> executeSql(
            "INSERT INTO purchase_order_items (order_id, product_id, product_name, quantity, unit_price, total_price) "
            + "VALUES (999999, 999999, '悬空明细', 1, 1, 1)"));
        assertNotNull(error.getMessage());
    }

    @Test
    @DisplayName("删除用户后历史交易与审计日志的操作人仍在（审计归属不被系统销毁）")
    void auditAttributionSurvivesUserDeletion() throws Exception {
        User user = new User("audit_fk_user", com.cashier.util.PasswordUtil.hashPassword("x"), "审计测试员", "cashier");
        assertTrue(DAOFactory.getInstance().getUserDAO().insert(user));

        Transaction transaction = new Transaction("T-AUDIT-FK-1", "2026-09-01 10:00:00", List.of(),
            new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("10.00"));
        transaction.operatorUsername = "audit_fk_user";
        transaction.operatorName = "审计测试员";
        transaction.paymentMethod = "现金";
        assertTrue(DAOFactory.getInstance().getTransactionDAO().insert(transaction));

        OperationLog log = new OperationLog();
        log.username = "audit_fk_user";
        log.operation = "AUDIT_FK_TEST";
        log.details = "删用户前的审计记录";
        log.timestamp = Instant.now();
        log.category = "SYSTEM";
        log.result = "SUCCESS";
        assertTrue(DAOFactory.getInstance().getOperationLogDAO().insert(log));

        assertTrue(DAOFactory.getInstance().getUserDAO().deleteByUsername("audit_fk_user"));

        assertEquals("audit_fk_user",
            DAOFactory.getInstance().getTransactionDAO().findById("T-AUDIT-FK-1").operatorUsername,
            "删除用户不得把历史交易的收银员抹成 NULL");
        assertTrue(DAOFactory.getInstance().getOperationLogDAO().findRecent(10).stream()
                .anyMatch(saved -> "audit_fk_user".equals(saved.username)),
            "删除用户不得抹掉审计日志的操作人");
    }

    @Test
    @DisplayName("docker 初始化脚本也不得给审计归属列加外键（三处 schema 口径一致）")
    void initScriptHasNoAuditAttributionForeignKey() throws Exception {
        String initSql = Files.readString(Path.of("docker/mysql-init/00-init-complete.sql"));
        assertFalse(initSql.contains("FOREIGN KEY (operator_username) REFERENCES users(username)"),
            "00-init-complete.sql 不得给 transactions.operator_username 加外键（与 DatabaseManager 保持一致）");
        assertFalse(initSql.contains("FOREIGN KEY (username) REFERENCES users(username) ON DELETE SET NULL"),
            "00-init-complete.sql 不得给 operation_logs.username 加外键（与 DatabaseManager 保持一致）");
    }

    private static Set<String> minus(Set<String> left, Set<String> right) {
        Set<String> diff = new HashSet<>(left);
        diff.removeAll(right);
        return diff;
    }
}
