package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对外接口的"审计归属"门禁（TD-004）。
 *
 * <p>这些端点都会把操作人写进审计/业务表：请求体里自报的 operator 可以直接被客户端伪造，
 * 因此操作人一律取认证用户 {@code ctx.attribute("currentUser")}（与下单/退款同一约定）。</p>
 */
@DisplayName("REST API 操作员身份门禁")
class OperatorIdentityPolicyTest {

    private static final String[] CONTROLLERS_WITH_AUDIT_OPERATOR = {
        "src/main/java/com/cashier/api/controller/TransactionApiController.java",
        "src/main/java/com/cashier/api/controller/PaymentApiController.java",
        "src/main/java/com/cashier/api/controller/BackupApiController.java",
    };

    @Test
    @DisplayName("写入审计归属的接口必须取认证用户，不得读取请求体里的 operator")
    void auditOperatorComesFromAuthenticatedUser() throws Exception {
        for (String file : CONTROLLERS_WITH_AUDIT_OPERATOR) {
            String source = Files.readString(Path.of(file));

            assertTrue(source.contains("ctx.attribute(\"currentUser\")"),
                file + " 必须从认证上下文取操作员");
            assertFalse(source.contains("getString(body, \"operator\""),
                file + " 不得再读取请求体里的 operator（可被伪造，且此前 PaymentApiController 在 insert 之后赋值、该列恒为 NULL）");
        }
    }

    @Test
    @DisplayName("支付单的操作员必须在落库前写入（否则 payment_orders.operator 恒为 NULL）")
    void paymentOrderOperatorIsPersisted() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/cashier/service/PaymentService.java"));

        int assign = service.indexOf("order.operator = operator");
        int insert = service.indexOf("getPaymentDAO().insert(order)");
        assertTrue(assign > 0, "PaymentService.createPaymentOrder 必须写入操作员");
        assertTrue(insert > 0, "找不到支付单落库调用");
        assertTrue(assign < insert, "操作员必须在 insert 之前赋值，否则不会落库");
    }

    @Test
    @DisplayName("会员更新冲突必须映射成 409（专用异常），不能被兜成 500")
    void memberUpdateConflictMapsTo409() throws Exception {
        String dao = Files.readString(Path.of("src/main/java/com/cashier/dao/MemberDAORefactored.java"));
        String api = Files.readString(Path.of("src/main/java/com/cashier/api/controller/MemberApiController.java"));

        assertTrue(dao.contains("class OptimisticLockException extends SQLException"),
            "会员 DAO 需要可区分的乐观锁异常");
        assertTrue(dao.contains("throw new OptimisticLockException("),
            "version 未命中时必须抛该异常");
        assertTrue(api.contains("catch (com.cashier.dao.MemberDAORefactored.OptimisticLockException"),
            "会员更新接口必须单独捕获乐观锁冲突");
        assertTrue(api.contains("HttpStatus.CONFLICT"),
            "乐观锁冲突必须回 409，而不是伪装成服务器内部错误");
    }
}
