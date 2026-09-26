package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商品/库存 API 的乐观锁门禁。
 *
 * <p>为什么用源码门禁而不是行为断言：控制器总是先 {@code findById} 再 {@code update}，
 * 单请求天然拿不到陈旧 version——冲突只出现在"读到写之间"的并发窗口，
 * TestContext 无法确定性地插入该窗口。DAO 契约（陈旧 version → 返回 false 且不抛异常）
 * 已由 {@code ProductDAORefactoredTest.testOptimisticLock} 覆盖，这里只需盯住
 * "接口必须消费这个返回值"。</p>
 */
@DisplayName("商品/库存 API 乐观锁门禁")
class ProductUpdateConflictPolicyTest {

    @Test
    @DisplayName("两个更新接口都必须检查乐观锁返回值并回 409，不得静默回 success")
    void updateEndpointsConsumeOptimisticLockResult() throws Exception {
        String productApi = Files.readString(
            Path.of("src/main/java/com/cashier/api/controller/ProductApiController.java"));
        String inventoryApi = Files.readString(
            Path.of("src/main/java/com/cashier/api/controller/InventoryApiController.java"));

        assertTrue(productApi.contains("if (!productDAO.update(product))"),
            "PUT /api/products/{id} 必须检查 update 的布尔返回：version 未命中时 update 返回 false 而不抛异常");
        assertTrue(productApi.contains("HttpStatus.CONFLICT"),
            "乐观锁未命中必须回 409，否则并发写入丢失却回 success:true");

        assertTrue(inventoryApi.contains("if (!productDAO.update(product))"),
            "PUT /api/inventory/{id} 必须检查 update 的布尔返回");
        assertTrue(inventoryApi.contains("HttpStatus.CONFLICT"),
            "乐观锁未命中必须回 409，否则收银台扣减会被静默覆盖");
        assertTrue(inventoryApi.contains("product.quantity < 0"),
            "库存接口必须拒绝负库存");
    }
}
