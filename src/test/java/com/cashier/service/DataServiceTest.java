package com.cashier.service;

import com.cashier.constant.FXConstants;
import com.cashier.dao.DAOFactory;
import com.cashier.model.Product;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据服务测试：设置持久化、主题偏好、库存加载。
 */
@DisplayName("数据服务测试")
class DataServiceTest extends DatabaseTestBase {

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS theme_preferences (
                    username VARCHAR(50) PRIMARY KEY,
                    theme_name VARCHAR(50),
                    updated_at BIGINT
                )
                """);
        }
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM theme_preferences");
        }
    }

    @Test
    @DisplayName("设置保存后可回读，空库提供默认值")
    void settingsPersistAndDefault() throws SQLException {
        Map<String, String> defaults = DataService.loadSettings();
        assertEquals("0.0", defaults.get("taxRate"));
        assertEquals("0", defaults.get("transactionCount"));

        Map<String, String> settings = new HashMap<>();
        settings.put("taxRate", "0.13");
        settings.put("receiptFooter", "谢谢惠顾");
        DataService.saveSettings(settings);

        Map<String, String> loaded = DataService.loadSettings();
        assertEquals("0.13", loaded.get("taxRate"));
        assertEquals("谢谢惠顾", loaded.get("receiptFooter"));
    }

    @Test
    @DisplayName("整数/开关设置读取：缺失取默认值、越界钳制、脏值回落（登录锁定与空闲登出靠它兜底）")
    void typedSettingsAreClampedAndSafe() throws SQLException {
        // 缺失 → 默认值
        assertEquals(5, DataService.getIntSetting("passwordMaxAttempts", 5, 1, 50));
        assertEquals("0.0", DataService.loadSettings().get("taxRate"));

        Map<String, String> settings = new HashMap<>();
        settings.put("passwordMaxAttempts", "3");
        settings.put("autoLogout", "true");
        settings.put("autoLogoutMinutes", "45");
        DataService.saveSettings(settings);

        assertEquals(3, DataService.getIntSetting("passwordMaxAttempts", 5, 1, 50));
        assertEquals(45, DataService.getIntSetting("autoLogoutMinutes", 30, 5, 120));
        assertTrue(DataService.getBooleanSetting("autoLogout", false));

        // 越界 → 钳制到边界（一条脏设置不得把防护关掉/无限延长）
        settings.put("passwordMaxAttempts", "0");
        settings.put("autoLogoutMinutes", "100000");
        DataService.saveSettings(settings);
        assertEquals(1, DataService.getIntSetting("passwordMaxAttempts", 5, 1, 50));
        assertEquals(120, DataService.getIntSetting("autoLogoutMinutes", 30, 5, 120));

        // 非法值 → 回落到默认值
        settings.put("passwordMaxAttempts", "abc");
        settings.put("autoLogout", "");
        DataService.saveSettings(settings);
        assertEquals(5, DataService.getIntSetting("passwordMaxAttempts", 5, 1, 50));
        assertFalse(DataService.getBooleanSetting("autoLogout", false));
    }

    @Test
    @DisplayName("备份频率映射为小时数：稳定代码与中英文老值都认，无法识别时不改周期")
    void backupFrequencyMapsToHours() {
        // TD-040 后设置页与 settings 表都存稳定代码
        assertEquals(24, BackupService.intervalHoursForFrequency("daily"));
        assertEquals(24 * 7, BackupService.intervalHoursForFrequency("weekly"));
        assertEquals(24 * 30, BackupService.intervalHoursForFrequency("monthly"));
        // 老库里的显示文案仍要能算出来，否则老数据升级后备份频率静默失效
        assertEquals(24, BackupService.intervalHoursForFrequency("每天"));
        assertEquals(24, BackupService.intervalHoursForFrequency("Daily"));
        assertEquals(24 * 7, BackupService.intervalHoursForFrequency("每周"));
        assertEquals(24 * 7, BackupService.intervalHoursForFrequency("Weekly"));
        assertEquals(24 * 30, BackupService.intervalHoursForFrequency("每月"));
        assertEquals(24 * 30, BackupService.intervalHoursForFrequency("Monthly"));
        assertEquals(0, BackupService.intervalHoursForFrequency("随便"));
        assertEquals(0, BackupService.intervalHoursForFrequency(null));

        // 归一函数是设置页回读用的（TD-040）——代码、老文案、未知值
        assertEquals("daily", BackupService.canonicalFrequency("Daily"));
        assertEquals("weekly", BackupService.canonicalFrequency("每周"));
        assertEquals("monthly", BackupService.canonicalFrequency("monthly"));
        assertEquals("", BackupService.canonicalFrequency("随便"));
        assertEquals("", BackupService.canonicalFrequency(null));
    }

    @Test
    @DisplayName("主题偏好默认值、保存回读与用户回退默认")
    void themePreferencePersistsAndFallsBack() {
        assertEquals(FXConstants.DEFAULT_THEME, DataService.loadThemePreference("nobody"));

        DataService.saveThemePreference("default", "dark");
        assertEquals("dark", DataService.loadThemePreference("nobody"));

        DataService.saveThemePreference("alice", "light");
        assertEquals("light", DataService.loadThemePreference("alice"));
        // 旧主题名归一化
        DataService.saveThemePreference("bob", "intellij");
        assertEquals("lisuan", DataService.loadThemePreference("bob"));
    }

    @Test
    @DisplayName("库存加载返回有界商品列表")
    void loadInventoryReturnsProducts() throws SQLException {
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.productCode = "INV" + i;
            product.name = "库存商品" + i;
            product.price = BigDecimal.valueOf(10 + i);
            product.quantity = 10;
            product.category = "测试";
            product.barcode = "B" + i;
            product.unit = "个";
            product.cost = BigDecimal.valueOf(5);
            assertTrue(DAOFactory.getInstance().getProductDAO().insert(product));
        }

        Map<String, Product> inventory = DataService.loadInventory();

        assertFalse(inventory.isEmpty());
        assertEquals("库存商品1", inventory.get("库存商品1").name);
        assertEquals(10, inventory.get("库存商品2").quantity);
    }
}
