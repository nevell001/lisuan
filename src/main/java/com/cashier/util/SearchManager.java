package com.cashier.util;

import com.cashier.i18n.I18nManager;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局搜索管理器
 * 提供功能和数据的快速搜索
 */
public class SearchManager {
    private static final I18nManager i18n = I18nManager.getInstance();

    /**
     * 搜索结果项
     */
    public static class SearchResult {
        private final String id;
        private final String type;
        private final String title;
        private final String description;
        private final String shortcut;
        private final Runnable action;

        public SearchResult(String id, String type, String title, String description, String shortcut, Runnable action) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.description = description;
            this.shortcut = shortcut;
            this.action = action;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getShortcut() { return shortcut; }
        public void execute() { if (action != null) action.run(); }

        public String getDisplayText() {
            if (shortcut != null && !shortcut.isEmpty()) {
                return title + " (" + shortcut + ")";
            }
            return title;
        }
    }

    /**
     * 搜索结果类型
     */
    public enum SearchType {
        FUNCTION("search.type.function", "功能"),
        DATA("search.type.data", "数据"),
        NAVIGATION("search.type.navigation", "导航");

        private final String i18nKey;
        private final String defaultName;

        SearchType(String i18nKey, String defaultName) {
            this.i18nKey = i18nKey;
            this.defaultName = defaultName;
        }

        public String getDisplayName() {
            return i18n.get(i18nKey);
        }
    }

    // 可搜索的导航项
    private static final List<SearchResult> NAVIGATION_ITEMS = new ArrayList<>();

    static {
        // 收银管理
        NAVIGATION_ITEMS.add(new SearchResult("cart", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.cart"), i18n.get("search.desc.cart"), "F1", null));
        NAVIGATION_ITEMS.add(new SearchResult("shift", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.shift"), i18n.get("search.desc.shift"), "F6", null));

        // 商品管理
        NAVIGATION_ITEMS.add(new SearchResult("inventory", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.inventory"), i18n.get("search.desc.inventory"), "F2", null));
        NAVIGATION_ITEMS.add(new SearchResult("inventory_check", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.inventory_check"), i18n.get("search.desc.inventory_check"), "", null));

        // 客户管理
        NAVIGATION_ITEMS.add(new SearchResult("members", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.members"), i18n.get("search.desc.members"), "F7", null));
        NAVIGATION_ITEMS.add(new SearchResult("return_order", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.return_order"), i18n.get("search.desc.return_order"), "", null));

        // 采购管理
        NAVIGATION_ITEMS.add(new SearchResult("supplier", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.supplier"), i18n.get("search.desc.supplier"), "", null));
        NAVIGATION_ITEMS.add(new SearchResult("purchase_order", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.purchase_order"), i18n.get("search.desc.purchase_order"), "", null));
        NAVIGATION_ITEMS.add(new SearchResult("purchase_approval", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.purchase_approval"), i18n.get("search.desc.purchase_approval"), "", null));
        NAVIGATION_ITEMS.add(new SearchResult("purchase_inbound", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.purchase_inbound"), i18n.get("search.desc.purchase_inbound"), "", null));

        // 数据统计
        NAVIGATION_ITEMS.add(new SearchResult("transactions", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.transactions"), i18n.get("search.desc.transactions"), "", null));
        NAVIGATION_ITEMS.add(new SearchResult("statistics", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.statistics"), i18n.get("search.desc.statistics"), "Ctrl+T", null));
        NAVIGATION_ITEMS.add(new SearchResult("promotions", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.promotions"), i18n.get("search.desc.promotions"), "F9", null));

        // 报表
        NAVIGATION_ITEMS.add(new SearchResult("purchase_report", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.purchase_report"), i18n.get("search.desc.purchase_report"), "", null));
        NAVIGATION_ITEMS.add(new SearchResult("inventory_report", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.inventory_report"), i18n.get("search.desc.inventory_report"), "", null));
        NAVIGATION_ITEMS.add(new SearchResult("profit_report", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.profit_report"), i18n.get("search.desc.profit_report"), "", null));

        // 系统设置
        NAVIGATION_ITEMS.add(new SearchResult("user_management", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.user_management"), i18n.get("search.desc.user_management"), "", null));
        NAVIGATION_ITEMS.add(new SearchResult("settings", SearchType.NAVIGATION.getDisplayName(),
                i18n.get("nav.settings"), i18n.get("search.desc.settings"), "", null));

        // 功能快捷方式
        NAVIGATION_ITEMS.add(new SearchResult("backup", SearchType.FUNCTION.getDisplayName(),
                i18n.get("menu.data.backup"), i18n.get("search.desc.backup"), "F11", null));
        NAVIGATION_ITEMS.add(new SearchResult("restore", SearchType.FUNCTION.getDisplayName(),
                i18n.get("menu.data.restore"), i18n.get("search.desc.restore"), "F12", null));
        NAVIGATION_ITEMS.add(new SearchResult("help", SearchType.FUNCTION.getDisplayName(),
                i18n.get("快捷键.title"), i18n.get("search.desc.help"), "Ctrl+/", null));
    }

    /**
     * 执行搜索
     * @param query 搜索关键词
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    public static List<SearchResult> search(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String lowerQuery = query.toLowerCase().trim();

        for (SearchResult item : NAVIGATION_ITEMS) {
            if (results.size() >= maxResults) break;

            // 搜索标题、描述和快捷键
            if (item.getTitle().toLowerCase().contains(lowerQuery) ||
                item.getDescription().toLowerCase().contains(lowerQuery) ||
                (item.getShortcut() != null && item.getShortcut().toLowerCase().contains(lowerQuery))) {
                results.add(item);
            }
        }

        return results;
    }

    /**
     * 获取所有导航项
     */
    public static List<SearchResult> getAllNavigationItems() {
        return new ArrayList<>(NAVIGATION_ITEMS);
    }

    /**
     * 根据ID获取搜索结果
     */
    public static SearchResult getById(String id) {
        for (SearchResult item : NAVIGATION_ITEMS) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 创建搜索结果的菜单项
     */
    public static MenuItem createMenuItem(SearchResult result) {
        MenuItem item = new MenuItem(result.getDisplayText());
        item.setOnAction(e -> result.execute());
        return item;
    }
}
