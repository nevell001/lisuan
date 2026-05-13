package com.cashier.util;

import com.cashier.i18n.I18nManager;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.*;

/**
 * 快捷键管理器
 * 集中管理应用中的所有快捷键定义和帮助信息
 */
public class ShortcutManager {
    private static final I18nManager i18n = I18nManager.getInstance();

    /**
     * 快捷键类别
     */
    public enum Category {
        FUNCTION("快捷键.category.function", "功能键"),
        NAVIGATION("快捷键.category.navigation", "导航"),
        EDITING("快捷键.category.editing", "编辑"),
        SYSTEM("快捷键.category.system", "系统"),
        POS("快捷键.category.pos", "收银");

        private final String i18nKey;
        private final String defaultName;

        Category(String i18nKey, String defaultName) {
            this.i18nKey = i18nKey;
            this.defaultName = defaultName;
        }

        public String getDisplayName() {
            return i18n.get(i18nKey);
        }
    }

    /**
     * 快捷键定义
     */
    public static class Shortcut {
        private final Category category;
        private final String keyCombination;
        private final String descriptionKey;
        private final String defaultDescription;

        public Shortcut(Category category, String keyCombination, String descriptionKey, String defaultDescription) {
            this.category = category;
            this.keyCombination = keyCombination;
            this.descriptionKey = descriptionKey;
            this.defaultDescription = defaultDescription;
        }

        public Category getCategory() {
            return category;
        }

        public String getKeyCombination() {
            return keyCombination;
        }

        public String getDescription() {
            return i18n.get(descriptionKey);
        }

        public KeyCodeCombination getKeyCodeCombination() {
            return parseKeyCombination(keyCombination);
        }

        private KeyCodeCombination parseKeyCombination(String keyStr) {
            String[] parts = keyStr.split("\\+");
            KeyCombination.Modifier[] modifiers = new KeyCombination.Modifier[0];
            KeyCode code = null;

            for (String part : parts) {
                String trimmed = part.trim().toUpperCase();
                switch (trimmed) {
                    case "CTRL":
                    case "CONTROL":
                        KeyCombination.Modifier[] newModifiers = Arrays.copyOf(modifiers, modifiers.length + 1);
                        newModifiers[modifiers.length] = KeyCombination.CONTROL_DOWN;
                        modifiers = newModifiers;
                        break;
                    case "SHIFT":
                        newModifiers = Arrays.copyOf(modifiers, modifiers.length + 1);
                        newModifiers[modifiers.length] = KeyCombination.SHIFT_DOWN;
                        modifiers = newModifiers;
                        break;
                    case "ALT":
                        newModifiers = Arrays.copyOf(modifiers, modifiers.length + 1);
                        newModifiers[modifiers.length] = KeyCombination.ALT_DOWN;
                        modifiers = newModifiers;
                        break;
                    default:
                        try {
                            code = KeyCode.valueOf(trimmed);
                        } catch (IllegalArgumentException e) {
                            // 无效的键码
                        }
                        break;
                }
            }

            if (code != null && modifiers.length > 0) {
                return new KeyCodeCombination(code, modifiers);
            } else if (code != null) {
                return new KeyCodeCombination(code);
            }
            return null;
        }
    }

    /**
     * 所有快捷键定义
     */
    private static final List<Shortcut> SHORTCUTS = new ArrayList<>();

    static {
        // ========== 功能键 ==========
        SHORTCUTS.add(new Shortcut(Category.FUNCTION, "F1", "shortcut.inventory", "商品管理"));
        SHORTCUTS.add(new Shortcut(Category.FUNCTION, "F5", "shortcut.refresh", "刷新"));
        SHORTCUTS.add(new Shortcut(Category.FUNCTION, "F7", "shortcut.members", "会员管理"));
        SHORTCUTS.add(new Shortcut(Category.FUNCTION, "F8", "shortcut.checkout", "收银结算"));
        SHORTCUTS.add(new Shortcut(Category.FUNCTION, "F9", "shortcut.promotions", "促销管理"));
        SHORTCUTS.add(new Shortcut(Category.FUNCTION, "F10", "shortcut.inventory_alert", "库存预警"));
        SHORTCUTS.add(new Shortcut(Category.FUNCTION, "F11", "shortcut.backup", "数据备份"));
        SHORTCUTS.add(new Shortcut(Category.FUNCTION, "F12", "shortcut.restore", "数据恢复"));

        // ========== 导航 ==========
        SHORTCUTS.add(new Shortcut(Category.NAVIGATION, "Ctrl+N", "shortcut.new_inventory", "新建商品"));
        SHORTCUTS.add(new Shortcut(Category.NAVIGATION, "Ctrl+M", "shortcut.members_nav", "会员管理"));
        SHORTCUTS.add(new Shortcut(Category.NAVIGATION, "Ctrl+T", "shortcut.statistics", "数据统计"));
        SHORTCUTS.add(new Shortcut(Category.NAVIGATION, "Ctrl+Shift+F", "shortcut.search", "全局搜索"));
        SHORTCUTS.add(new Shortcut(Category.NAVIGATION, "ESCAPE", "shortcut.close_tab", "关闭标签页"));

        // ========== 编辑 ==========
        SHORTCUTS.add(new Shortcut(Category.EDITING, "Ctrl+S", "shortcut.save", "保存"));
        SHORTCUTS.add(new Shortcut(Category.EDITING, "Ctrl+E", "shortcut.edit", "编辑"));
        SHORTCUTS.add(new Shortcut(Category.EDITING, "Ctrl+A", "shortcut.select_all", "全选"));
        SHORTCUTS.add(new Shortcut(Category.EDITING, "Ctrl+B", "shortcut.batch", "批量操作"));
        SHORTCUTS.add(new Shortcut(Category.EDITING, "Ctrl+D", "shortcut.export", "导出数据"));
        SHORTCUTS.add(new Shortcut(Category.EDITING, "Ctrl+R", "shortcut.refresh_edit", "刷新编辑"));

        // ========== 系统 ==========
        SHORTCUTS.add(new Shortcut(Category.SYSTEM, "Ctrl+Q", "shortcut.exit", "退出系统"));
        SHORTCUTS.add(new Shortcut(Category.SYSTEM, "Ctrl+/", "shortcut.help", "快捷键帮助"));

        // ========== 收银 ==========
        SHORTCUTS.add(new Shortcut(Category.POS, "F2", "shortcut.hold_order", "挂单"));
        SHORTCUTS.add(new Shortcut(Category.POS, "F3", "shortcut.resume_order", "恢复挂单"));
        SHORTCUTS.add(new Shortcut(Category.POS, "F4", "shortcut.clear_cart", "清空购物车"));
        SHORTCUTS.add(new Shortcut(Category.POS, "F6", "shortcut.quick_cash", "快速现金支付"));
        SHORTCUTS.add(new Shortcut(Category.POS, "Ctrl+1", "shortcut.payment_wechat", "微信支付"));
        SHORTCUTS.add(new Shortcut(Category.POS, "Ctrl+2", "shortcut.payment_alipay", "支付宝支付"));
        SHORTCUTS.add(new Shortcut(Category.POS, "Ctrl+3", "shortcut.payment_card", "银行卡支付"));
        SHORTCUTS.add(new Shortcut(Category.POS, "Enter", "shortcut.confirm_payment", "确认支付"));
    }

    /**
     * 获取所有快捷键
     */
    public static List<Shortcut> getAllShortcuts() {
        return new ArrayList<>(SHORTCUTS);
    }

    /**
     * 按类别获取快捷键
     */
    public static Map<Category, List<Shortcut>> getShortcutsByCategory() {
        Map<Category, List<Shortcut>> result = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            result.put(category, new ArrayList<>());
        }
        for (Shortcut shortcut : SHORTCUTS) {
            result.get(shortcut.getCategory()).add(shortcut);
        }
        return result;
    }

    /**
     * 根据键码获取快捷键描述
     */
    public static String getShortcutDescription(KeyCode code, boolean controlDown, boolean shiftDown, boolean altDown) {
        for (Shortcut shortcut : SHORTCUTS) {
            KeyCodeCombination combo = shortcut.getKeyCodeCombination();
            if (combo != null) {
                boolean ctrlMatch = controlDown ?
                    combo.getControl() == KeyCombination.ModifierValue.DOWN :
                    combo.getControl() == KeyCombination.ModifierValue.UP;
                boolean shiftMatch = shiftDown ?
                    combo.getShift() == KeyCombination.ModifierValue.DOWN :
                    combo.getShift() == KeyCombination.ModifierValue.UP;
                boolean altMatch = altDown ?
                    combo.getAlt() == KeyCombination.ModifierValue.DOWN :
                    combo.getAlt() == KeyCombination.ModifierValue.UP;

                if (combo.getCode() == code && ctrlMatch && shiftMatch && altMatch) {
                    return shortcut.getDescription();
                }
            } else if (code.name().equals(shortcut.getKeyCombination())) {
                return shortcut.getDescription();
            }
        }
        return null;
    }

    /**
     * 检查快捷键冲突
     */
    public static boolean hasConflict(String keyCombination) {
        int count = 0;
        for (Shortcut shortcut : SHORTCUTS) {
            if (shortcut.getKeyCombination().equalsIgnoreCase(keyCombination)) {
                count++;
            }
        }
        return count > 1;
    }

    /**
     * 检查快捷键冲突 - 返回冲突的快捷键列表
     */
    public static List<Shortcut> findConflicts(String keyCombination) {
        List<Shortcut> conflicts = new ArrayList<>();
        for (Shortcut shortcut : SHORTCUTS) {
            if (shortcut.getKeyCombination().equalsIgnoreCase(keyCombination)) {
                conflicts.add(shortcut);
            }
        }
        return conflicts;
    }

    /**
     * 检查两个快捷键组合是否冲突
     */
    public static boolean isConflict(String keyCombination1, String keyCombination2) {
        if (keyCombination1 == null || keyCombination2 == null) {
            return false;
        }
        return normalizeKeyCombination(keyCombination1).equals(
               normalizeKeyCombination(keyCombination2));
    }

    /**
     * 标准化快捷键字符串（用于比较）
     */
    public static String normalizeKeyCombination(String keyCombination) {
        return keyCombination
            .toLowerCase()
            .replace("control", "ctrl")
            .replace(" ", "")
            .replace("+", "")
            .trim();
    }

    /**
     * 格式化快捷键显示文本
     */
    public static String formatShortcutDisplay(String keyCombination) {
        return keyCombination
            .replace("Control", "Ctrl")
            .replace("+", " + ")
            .trim();
    }

    /**
     * 解析用户输入的快捷键字符串
     * 支持多种格式：Ctrl+S, ctrl+s, CONTROL+S, Ctrl + S
     */
    public static String parseShortcutInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String[] parts = input.trim().split("\\+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim().toUpperCase();

            // 标准化修饰键
            if (part.equals("CONTROL") || part.equals("CTRL")) {
                result.append("Ctrl");
            } else if (part.equals("SHIFT")) {
                result.append("Shift");
            } else if (part.equals("ALT")) {
                result.append("Alt");
            } else {
                // 验证键码
                try {
                    KeyCode.valueOf(part);
                    result.append(part);
                } catch (IllegalArgumentException e) {
                    return ""; // 无效的键码
                }
            }

            if (i < parts.length - 1) {
                result.append("+");
            }
        }

        return result.toString();
    }

    /**
     * 验证快捷键格式是否有效
     */
    public static boolean isValidShortcut(String keyCombination) {
        if (keyCombination == null || keyCombination.trim().isEmpty()) {
            return false;
        }

        String parsed = parseShortcutInput(keyCombination);
        return !parsed.isEmpty() && parsed.contains("+") ||
               KeyCode.valueOf(parsed) != null;
    }

    /**
     * 获取帮助面板内容（Markdown 格式）
     */
    public static String getHelpContent() {
        StringBuilder sb = new StringBuilder();
        Map<Category, List<Shortcut>> byCategory = getShortcutsByCategory();

        for (Map.Entry<Category, List<Shortcut>> entry : byCategory.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            sb.append("## ").append(entry.getKey().getDisplayName()).append("\n\n");

            for (Shortcut shortcut : entry.getValue()) {
                sb.append("| **").append(formatShortcutDisplay(shortcut.getKeyCombination()))
                  .append("** | ").append(shortcut.getDescription()).append(" |\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 快捷键配置项（用于自定义快捷键）
     */
    public static class ShortcutConfig {
        private final String actionKey;
        private String defaultKeyCombination;
        private String customKeyCombination;

        public ShortcutConfig(String actionKey, String defaultKeyCombination) {
            this.actionKey = actionKey;
            this.defaultKeyCombination = defaultKeyCombination;
            this.customKeyCombination = null;
        }

        public String getActionKey() { return actionKey; }
        public String getDefaultKeyCombination() { return defaultKeyCombination; }
        public String getCustomKeyCombination() { return customKeyCombination; }
        public String getEffectiveKeyCombination() {
            return customKeyCombination != null ? customKeyCombination : defaultKeyCombination;
        }

        public void setCustomKeyCombination(String custom) {
            this.customKeyCombination = custom;
        }

        public void resetToDefault() {
            this.customKeyCombination = null;
        }

        public boolean isCustomized() {
            return customKeyCombination != null;
        }
    }

    /**
     * 获取所有可自定义的快捷键配置
     */
    public static List<ShortcutConfig> getAllShortcutConfigs() {
        List<ShortcutConfig> configs = new ArrayList<>();
        for (Shortcut shortcut : SHORTCUTS) {
            configs.add(new ShortcutConfig(shortcut.descriptionKey, shortcut.keyCombination));
        }
        return configs;
    }
}
