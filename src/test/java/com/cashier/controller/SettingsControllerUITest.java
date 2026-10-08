package com.cashier.controller;

import com.cashier.constant.FXConstants;
import com.cashier.i18n.I18nKeys;
import com.cashier.i18n.I18nManager;
import com.cashier.model.User;
import com.cashier.service.DataService;
import com.cashier.util.DatabaseTestBase;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设置页下拉的**端到端冒烟**（TD-040）：加载真实 {@code SettingsView.fxml} + {@link SettingsController}，
 * 断言 item 是稳定代码、按钮单元格按当前语言渲染，且老库里按显示串存的
 * {@code paperSize}/{@code backupFrequency} 能被归一后正确选中。
 *
 * <p>与 {@code SettingsDropdownValuePolicyTest}（源码门禁）互补：那条钉形状，这条验证真实控件行为。</p>
 *
 * <p><b>已知边界</b>：H2 测试库只建了 {@code settings} 表，没有 {@code theme_preferences} /
 * {@code language_preferences} / {@code font_size_preferences}（`DatabaseTestBase` 的 schema 子集），
 * 所以语言/主题/字号/货币这 4 个下拉走的是 DataService 的**默认值回退**路径；真正走库的是
 * {@code paperSize} 与 {@code backupFrequency}（settings 表），这两条也正好是 TD-040 的修复点。</p>
 *
 * <p>需要真实显示环境，默认 {@code mvn verify} 排除（pom surefire excludes）；
 * 桌面环境显式运行：{@code mvn -Pui-tests -Dtest=SettingsControllerUITest test}。</p>
 */
@ExtendWith(ApplicationExtension.class)
@DisplayName("设置页下拉冒烟（真实 FXML + 控制器）")
class SettingsControllerUITest extends DatabaseTestBase {

    private Parent root;

    @Start
    void start(Stage stage) throws Exception {
        DatabaseTestBase.initTestDatabase();

        // 故意写入"老库形状"：这两个键历史上存的是当时的显示串，读取时必须归一到代码
        Map<String, String> legacy = new HashMap<>();
        legacy.put("paperSize", "80mm（热敏纸）");
        legacy.put("backupFrequency", "每周");
        DataService.saveSettings(legacy);

        I18nManager i18n = I18nManager.getInstance();
        i18n.setLocale("zh-CN");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cashier/view/SettingsView.fxml"));
        loader.setResources(i18n.getResourceBundle());
        root = loader.load();
        SettingsController controller = loader.getController();
        controller.setCurrentUser(new User("smoke", "", "冒烟", "admin"));

        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }

    @Test
    @DisplayName("7 个下拉的 item 都是稳定代码，老显示串落库值被归一后正确选中")
    void dropdownsHoldStableCodesAndNormalizeLegacyValues() {
        assertEquals(List.of("zh-CN", "zh-TW", "en"), combo("languageComboBox").getItems());
        assertEquals(List.of("CNY", "USD", "JPY", "KRW", "EUR"), combo("currencyComboBox").getItems());
        assertEquals(List.of("light", "dark", "lisuan"), combo("themeComboBox").getItems());
        assertEquals(List.of("small", "medium", "large", "extra-large"), combo("fontSizeComboBox").getItems());
        assertEquals(List.of("58mm", "80mm", "A4"), combo("paperSizeComboBox").getItems());
        assertEquals(List.of("daily", "weekly", "monthly"), combo("backupFrequencyComboBox").getItems());
        assertEquals(List.of("disabled", "mock", "production"), combo("paymentModeComboBox").getItems());

        // 老显示串 → 代码（这条正是 TD-040 修掉的"切语言后被静默重置"路径）
        assertEquals("80mm", combo("paperSizeComboBox").getValue());
        assertEquals("weekly", combo("backupFrequencyComboBox").getValue());

        // 偏好表里存的本来就是代码，按代码直接选中
        assertEquals("zh-CN", combo("languageComboBox").getValue());
        assertEquals("CNY", combo("currencyComboBox").getValue());
        assertEquals(FXConstants.DEFAULT_THEME, combo("themeComboBox").getValue());
        assertEquals("medium", combo("fontSizeComboBox").getValue());
        assertEquals("disabled", combo("paymentModeComboBox").getValue());
    }

    @Test
    @DisplayName("下拉渲染的是本地化名称；切 en 后随语言变（语种自称除外）")
    void dropdownsRenderLocalizedText() {
        I18nManager i18n = I18nManager.getInstance();
        String original = i18n.getCurrentLanguageTag();
        try {
            i18n.setLocale("zh-CN");
            assertEquals("简体中文", shownText("languageComboBox"));
            assertEquals(i18n.get(I18nKeys.Currency.CNY), shownText("currencyComboBox"));
            assertEquals(i18n.get(I18nKeys.Menu.Theme.LISUAN), shownText("themeComboBox"));
            assertEquals(i18n.get(I18nKeys.Settings.FONT_SIZE_MEDIUM), shownText("fontSizeComboBox"));
            assertEquals("80mm（热敏纸）", shownText("paperSizeComboBox"));
            assertEquals("每周", shownText("backupFrequencyComboBox"));
            assertEquals("已禁用", shownText("paymentModeComboBox"));

            // 切语言后同一份 item（代码）渲染成英文——这正是"代码即数据、显示层翻译"的效果
            i18n.setLocale("en");
            assertEquals("80mm (Thermal)", shownText("paperSizeComboBox"));
            assertEquals("Weekly", shownText("backupFrequencyComboBox"));
            assertEquals("Disabled", shownText("paymentModeComboBox"));
            assertEquals("Chinese Yuan (CNY)", shownText("currencyComboBox"));
            // 语种自称不随界面语言变
            assertEquals("简体中文", shownText("languageComboBox"));
        } finally {
            i18n.setLocale(original);
        }
    }

    private ComboBox<String> combo(String fxId) {
        javafx.scene.Node node = root.lookup("#" + fxId);
        assertNotNull(node, "#" + fxId + " 未出现在场景图里（fx:id 与控制器字段不匹配？）");
        @SuppressWarnings("unchecked")
        ComboBox<String> box = (ComboBox<String>) node;
        return box;
    }

    /** 按钮单元格的**渲染文本**——即用户在下拉里实际看到的那一行。 */
    private String shownText(String fxId) {
        ComboBox<String> box = combo(fxId);
        ListCell<String> cell = box.getButtonCell();
        assertNotNull(cell, fxId + " 没接显示层转换（configureComboBox）");
        String value = box.getValue();
        try {
            // 单元格一旦被 skin 接管，改文本就必须在 FX 线程上做（否则 Parent 会拒绝改子节点）；
            // Cell.updateItem(T, boolean) 是 protected，JavaFX 在本项目按 classpath 加载，反射可直接调
            return org.testfx.util.WaitForAsyncUtils.asyncFx(() -> {
                java.lang.reflect.Method update =
                    javafx.scene.control.Cell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
                update.setAccessible(true);
                update.invoke(cell, value, false);
                return cell.getText();
            }).get();
        } catch (Exception e) {
            fail(fxId + " 读取渲染文本失败: " + e);
            return null;
        }
    }
}
