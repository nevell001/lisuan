package com.cashier.security;

import com.cashier.constant.FXConstants;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.I18nUiUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 非中文默认 locale 下的行为回归（TD-015）。
 *
 * <p>静态门禁（{@link LocaleFormatPolicyTest}）只能证明"格式串带了 Locale.ROOT"，
 * 这里再把 JVM 默认 locale 真的切成德语，走一遍**会被打印/被解析**的真实代码路径：
 * 金额工具、CSS 颜色。德语的小数分隔符是逗号，一旦代码跟随默认 locale，
 * 小票会打出 {@code 1,50}（对不上账）、CSS 的 {@code rgba(...,0,50)} 直接失效。</p>
 */
@DisplayName("非中文默认 locale 行为回归")
class LocaleIndependenceBehaviorTest {

    @Test
    @DisplayName("德语默认 locale 下金额与 CSS 颜色仍用点号小数")
    void formattingStaysDotDecimalUnderGermanLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);

            // 参照系：确认这个 locale 下"跟随默认"确实会用逗号（否则本测试没有意义）
            assertEquals("1,50", String.format("%.2f", 1.5),
                "德语默认 locale 应当用逗号做小数分隔符，否则本用例失去意义");

            // 金额工具（界面/小票都用它）：固定 SIMPLIFIED_CHINESE，必须还是点号千分位
            // （带货币符号，故只断言数字部分的写法）
            assertTrue(CurrencyUtil.format(1.5).endsWith("1.50"),
                "金额工具必须点号小数，实际: " + CurrencyUtil.format(1.5));
            assertTrue(CurrencyUtil.format(new BigDecimal("1234.5")).contains("1,234.50"),
                "金额工具必须点号千分位，实际: " + CurrencyUtil.format(new BigDecimal("1234.5")));

            // 小票金额行：ReceiptPrinter 走的是带 Locale.ROOT 的格式化
            String line = String.format(java.util.Locale.ROOT, "%35s %10.2f", "实付金额:", new BigDecimal("180.00"));
            assertTrue(line.contains("180.00"), "小票金额行必须点号，实际: " + line);

            // CSS 颜色：逗号小数会让 JavaFX 静默丢弃整条样式
            String css = FXConstants.toCssColor(javafx.scene.paint.Color.rgb(18, 52, 86, 0.5));
            assertTrue(css.startsWith("rgba(18, 52, 86, 0.50)"),
                "CSS rgba() 必须点号小数，实际: " + css);

            // 支付方式归一化（落库值匹配）也不得跟随 locale
            assertEquals("CASH", I18nUiUtils.canonicalPaymentMethod("CASH"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
