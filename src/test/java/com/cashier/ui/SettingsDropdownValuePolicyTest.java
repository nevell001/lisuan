package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 设置页下拉值口径门禁（TD-040，TD-002 同款）。
 *
 * <p>问题形状：下拉 item 直接放 {@code i18n.get(...)} 的**显示串**，保存时写进 settings 表，
 * 读取时再按当前语言的显示串反解（{@code getItems().contains(老值)}）。界面语言一变，
 * 历史值就匹配不上——设置被静默重置成默认值（{@code paperSize} 实测如此）。</p>
 *
 * <p>正确形状：item 是稳定代码，显示层交给 {@code I18nUiUtils.configureComboBox}；
 * 老库里按显示串存的值在读取时归一。</p>
 */
@DisplayName("设置页下拉值口径门禁")
class SettingsDropdownValuePolicyTest {

    private static final Path SOURCE =
        Path.of("src/main/java/com/cashier/controller/SettingsController.java");

    /** 设置页全部下拉框字段名；新增下拉时补进来。 */
    private static final List<String> COMBO_FIELDS = List.of(
        "languageComboBox", "currencyComboBox", "themeComboBox", "fontSizeComboBox",
        "paperSizeComboBox", "backupFrequencyComboBox", "paymentModeComboBox");

    @Test
    @DisplayName("每个设置下拉的 item 都是稳定代码，且都接了显示层转换")
    void dropdownsUseStableCodeValues() throws Exception {
        String text = Files.readString(SOURCE);
        for (String field : COMBO_FIELDS) {
            int setItems = text.indexOf(field + ".setItems(");
            assertTrue(setItems > 0, field + " 未找到 setItems 调用（门禁需要更新）");
            int end = text.indexOf(");", setItems);
            assertTrue(end > setItems, field + " 的 setItems 调用解析失败（门禁需要更新）");
            String itemsBlock = text.substring(setItems, end);
            assertFalse(itemsBlock.contains(".get("),
                field + " 的 item 不得放本地化显示串——切语言后与历史值失配，必须是稳定代码");
            assertTrue(text.contains("configureComboBox(" + field + ","),
                field + " 必须通过 I18nUiUtils.configureComboBox 翻译显示层");
        }
    }

    @Test
    @DisplayName("落库值不得再按显示串回读：paperSize 需归一历史值")
    void persistedValuesAreCodesNotDisplayStrings() throws Exception {
        String text = Files.readString(SOURCE);
        assertFalse(text.contains("paperSizeComboBox.getItems().contains(savedPaperSize)"),
            "按显示串 contains 回读，切语言后必然失配");
        assertTrue(text.contains("paperSizeCodeOf("),
            "老库里 paperSize 存的是显示串，读取时必须归一到代码");
        assertTrue(text.contains("\"paperSize\", defaultText(selectedPaperSize"),
            "settings 表必须落代码而不是显示串");
        assertTrue(text.contains("\"backupFrequency\", defaultText(selectedBackupFreq"),
            "备份频率也必须落代码（BackupService 按代码算周期）");
        assertTrue(text.contains("canonicalFrequency(settings.getOrDefault(\"backupFrequency\""),
            "备份频率必须回读：原先漏了这一步，下拉永远显示默认值，用户再保存就把选择覆盖回默认");
    }
}
