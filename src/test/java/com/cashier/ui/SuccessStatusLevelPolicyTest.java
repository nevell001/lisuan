package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuccessStatusLevelPolicyTest {

    @Test
    @DisplayName("库存高频成功操作应显式使用成功状态")
    void inventorySuccessActionsUseSuccessStatus() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryController.java"
        ));

        assertTrue(controller.contains("StatusBarManager.updateSuccess(\"商品删除成功: \" + product.name)"));
        assertTrue(controller.contains("StatusBarManager.updateSuccess(item == null ? \"商品添加成功\" : \"商品更新成功\")"));
        assertTrue(controller.contains("StatusBarManager.updateSuccess(\"快速入库成功: \" + selected.name"));
        assertTrue(controller.contains("StatusBarManager.updateSuccess(\"商品列表已刷新\")"));
        assertFalse(controller.contains("StatusBarManager.updateStatus(\"商品删除成功"));
        assertFalse(controller.contains("StatusBarManager.updateStatus(item == null ? \"商品添加成功\""));
    }

    @Test
    @DisplayName("盘点和促销状态助手应显式使用成功状态")
    void inventoryCheckAndPromotionStatusHelpersUseSuccessStatus() throws Exception {
        String inventoryCheckController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryCheckController.java"
        ));
        String promotionController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PromotionController.java"
        ));

        assertTrue(inventoryCheckController.contains("StatusBarManager.updateSuccess(status)"));
        assertTrue(promotionController.contains("StatusBarManager.updateSuccess(status)"));
        assertFalse(inventoryCheckController.contains("StatusBarManager.updateStatus(status)"));
        assertFalse(promotionController.contains("StatusBarManager.updateStatus(status)"));
    }

    @Test
    @DisplayName("触屏收银台交接班完成应显式使用成功状态")
    void posSuccessActionsUseSuccessStatus() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/TouchCartController.java"
        ));

        assertTrue(controller.contains("StatusBarManager.updateSuccess(\"交接班完成，正在退出…\")"));
        assertTrue(controller.contains("StatusBarManager.updateSuccess(\"交接班操作完成\")"));
    }
}
