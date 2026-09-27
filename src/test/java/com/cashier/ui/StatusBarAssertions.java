package com.cashier.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 断言某个 i18n 文案是以**成功级别**写入状态栏的。
 *
 * <p>为什么按 key 而不是按文案字面量断言：这组门禁原本钉的是
 * `StatusBarManager.updateSuccess("商品删除成功: " + name)` 这种**硬编码中文**的完整文本，
 * 于是"把文案迁到 i18n"（TD-014）会被误判成"不再使用成功级别"——门禁与实现细节耦合了。
 * 现在改为：找到文案 key 所在的那条语句，断言它用了 {@code updateSuccess(} 而不是 {@code updateStatus(}。</p>
 *
 * <p>"不许硬编码中文"这条不变量由
 * {@code com.cashier.security.HardcodedUiTextPolicyTest.statusBarTextIsNeverHardcodedRepoWide}
 * 全仓库覆盖（只查状态栏调用实参，日志里的中文不算）——本类不做重复检查。</p>
 */
final class StatusBarAssertions {

    private StatusBarAssertions() {
    }

    /** 断言 {@code key} 所在语句使用成功级别写状态栏。 */
    static void assertUsesSuccessLevel(String source, String key, String where) {
        int at = source.indexOf(key);
        assertTrue(at >= 0, where + " 找不到状态栏文案 key: " + key);
        int start = source.lastIndexOf(';', at) + 1;
        int end = source.indexOf(';', at);
        String statement = source.substring(start, end < 0 ? source.length() : end);
        assertTrue(statement.contains("updateSuccess("),
            where + " 的 " + key + " 必须用 StatusBarManager.updateSuccess（SUCCESS 级别），实际语句: "
                + statement.replaceAll("\\s+", " ").trim());
        assertFalse(statement.contains("updateStatus("),
            where + " 的 " + key + " 不应回退到 updateStatus（NORMAL 级别）: "
                + statement.replaceAll("\\s+", " ").trim());
    }

}
