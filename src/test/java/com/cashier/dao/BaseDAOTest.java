package com.cashier.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseDAO 事务管理测试
 */
class BaseDAOTest {

    @Test
    @DisplayName("executeInTransaction 成功后应提交事务并关闭连接")
    void testExecuteInTransactionClosesConnectionOnSuccess() throws SQLException {
        TrackingConnection tracking = TrackingConnection.create();
        try {
            TestBaseDAO dao = new TestBaseDAO(tracking.connection());

            String result = dao.runInTransaction(conn -> "ok");

            assertEquals("ok", result);
            assertTrue(tracking.committed(), "成功路径应提交事务");
            assertFalse(tracking.rolledBack(), "成功路径不应回滚事务");
            assertTrue(tracking.closed(), "成功路径应关闭连接");
            assertEquals(Boolean.TRUE, tracking.autoCommitStateWhenClosed(), "关闭前应恢复 autoCommit");
        } finally {
            tracking.forceClose();
        }
    }

    @Test
    @DisplayName("executeInTransaction SQLException 时应回滚并关闭连接")
    void testExecuteInTransactionClosesConnectionOnSQLException() throws SQLException {
        TrackingConnection tracking = TrackingConnection.create();
        try {
            TestBaseDAO dao = new TestBaseDAO(tracking.connection());

            SQLException exception = assertThrows(SQLException.class,
                () -> dao.runInTransaction(conn -> {
                    throw new SQLException("boom");
                }));

            assertEquals("boom", exception.getMessage());
            assertFalse(tracking.committed(), "异常路径不应提交事务");
            assertTrue(tracking.rolledBack(), "SQLException 路径应回滚事务");
            assertTrue(tracking.closed(), "SQLException 路径应关闭连接");
            assertEquals(Boolean.TRUE, tracking.autoCommitStateWhenClosed(), "关闭前应恢复 autoCommit");
        } finally {
            tracking.forceClose();
        }
    }

    @Test
    @DisplayName("executeInTransaction 运行时异常时也应回滚并关闭连接")
    void testExecuteInTransactionClosesConnectionOnRuntimeException() throws SQLException {
        TrackingConnection tracking = TrackingConnection.create();
        try {
            TestBaseDAO dao = new TestBaseDAO(tracking.connection());

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> dao.runInTransaction(conn -> {
                    throw new IllegalStateException("boom");
                }));

            assertEquals("boom", exception.getMessage());
            assertFalse(tracking.committed(), "异常路径不应提交事务");
            assertTrue(tracking.rolledBack(), "运行时异常路径也应回滚事务");
            assertTrue(tracking.closed(), "运行时异常路径应关闭连接");
            assertEquals(Boolean.TRUE, tracking.autoCommitStateWhenClosed(), "关闭前应恢复 autoCommit");
        } finally {
            tracking.forceClose();
        }
    }

    private static class TestBaseDAO extends BaseDAO {
        private final Connection connection;

        private TestBaseDAO(Connection connection) {
            this.connection = connection;
        }

        @Override
        protected Connection getConnection() {
            return connection;
        }

        private <T> T runInTransaction(TransactionOperation<T> operation) throws SQLException {
            return executeInTransaction(operation);
        }
    }

    private static class TrackingConnection {
        private final Connection delegate;
        private final Connection proxy;
        private final AtomicBoolean closed;
        private final AtomicBoolean committed;
        private final AtomicBoolean rolledBack;
        private final AtomicReference<Boolean> autoCommitStateWhenClosed;

        private TrackingConnection(
            Connection delegate,
            Connection proxy,
            AtomicBoolean closed,
            AtomicBoolean committed,
            AtomicBoolean rolledBack,
            AtomicReference<Boolean> autoCommitStateWhenClosed
        ) {
            this.delegate = delegate;
            this.proxy = proxy;
            this.closed = closed;
            this.committed = committed;
            this.rolledBack = rolledBack;
            this.autoCommitStateWhenClosed = autoCommitStateWhenClosed;
        }

        static TrackingConnection create() throws SQLException {
            Connection delegate = DriverManager.getConnection(
                "jdbc:h2:mem:base_dao_test_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1"
            );
            AtomicBoolean closed = new AtomicBoolean(false);
            AtomicBoolean committed = new AtomicBoolean(false);
            AtomicBoolean rolledBack = new AtomicBoolean(false);
            AtomicReference<Boolean> autoCommitStateWhenClosed = new AtomicReference<>();

            Connection proxy = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (ignored, method, args) -> {
                    try {
                        return switch (method.getName()) {
                            case "commit" -> {
                                committed.set(true);
                                yield method.invoke(delegate, args);
                            }
                            case "rollback" -> {
                                rolledBack.set(true);
                                yield method.invoke(delegate, args);
                            }
                            case "close" -> {
                                autoCommitStateWhenClosed.set(delegate.getAutoCommit());
                                closed.set(true);
                                yield method.invoke(delegate, args);
                            }
                            default -> method.invoke(delegate, args);
                        };
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }
            );

            return new TrackingConnection(delegate, proxy, closed, committed, rolledBack, autoCommitStateWhenClosed);
        }

        Connection connection() {
            return proxy;
        }

        boolean closed() {
            return closed.get();
        }

        boolean committed() {
            return committed.get();
        }

        boolean rolledBack() {
            return rolledBack.get();
        }

        Boolean autoCommitStateWhenClosed() {
            return autoCommitStateWhenClosed.get();
        }

        void forceClose() throws SQLException {
            if (!delegate.isClosed()) {
                delegate.close();
            }
        }
    }
}
