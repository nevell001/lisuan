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

        // 按文案 key 断言级别（不钉中文文案：文案迁 i18n 是 TD-014 的正常演进）
        StatusBarAssertions.assertUsesSuccessLevel(controller, "status_message.product_deleted", "InventoryController");
        StatusBarAssertions.assertUsesSuccessLevel(controller, "status_message.product_created_plain", "InventoryController");
        StatusBarAssertions.assertUsesSuccessLevel(controller, "status_message.product_updated_plain", "InventoryController");
        StatusBarAssertions.assertUsesSuccessLevel(controller, "status_message.quick_restock", "InventoryController");
        StatusBarAssertions.assertUsesSuccessLevel(controller, "status_message.product_list_refreshed", "InventoryController");
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

        StatusBarAssertions.assertUsesSuccessLevel(controller, "status_message.shift_ending_logout", "TouchCartController");
        StatusBarAssertions.assertUsesSuccessLevel(controller, "status_message.shift_completed", "TouchCartController");
    }
}
