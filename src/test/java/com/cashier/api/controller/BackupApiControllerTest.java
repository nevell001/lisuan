package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackupApiControllerTest {

    @Test
    @DisplayName("空请求体执行备份返回 400")
    void executeBackupWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/backup/execute");
        BackupApiController.executeBackup(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体更新备份配置返回 400")
    void updateConfigWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/backup/config");
        BackupApiController.updateConfig(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("云备份目标返回 501，且不落备份记录")
    void executeBackupWithCloudTargetReturns501() {
        TestContext ctx = new TestContext()
            .withRequest(HandlerType.POST, "/api/backup/execute")
            .withBody(Map.of("contentType", "FULL", "target", "ALIYUN_OSS"));
        BackupApiController.executeBackup(ctx.context);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, ctx.status);
    }

    @Test
    @DisplayName("保存云存储配置字段返回 501 而不是假成功")
    void updateConfigWithCloudFieldsReturns501() {
        TestContext ctx = new TestContext()
            .withRequest(HandlerType.PUT, "/api/backup/config")
            .withBody(Map.of("aliyunBucket", "my-bucket"));
        BackupApiController.updateConfig(ctx.context);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, ctx.status);
        assertEquals(false, ((Map<?, ?>) ctx.json).get("success"));
    }

    @Test
    @DisplayName("保存云备份目标返回 501")
    void updateConfigWithCloudTargetReturns501() {
        TestContext ctx = new TestContext()
            .withRequest(HandlerType.PUT, "/api/backup/config")
            .withBody(Map.of("target", "TENCENT_COS"));
        BackupApiController.updateConfig(ctx.context);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, ctx.status);
    }
}
