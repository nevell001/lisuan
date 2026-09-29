package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 金额/数值精度的源码级门禁（TD-034，2026-09 审计发现）。
 *
 * <p>这三处的口径问题都发生在 JavaFX 控制器里（无法在无显示环境下做行为测试），
 * 因此按本仓库既有做法用源码门禁钉住"不许退回旧写法"。对应的可行为验证部分分别有：
 * 退款/积分在 {@code ReturnServiceTest}、发票在 {@code InvoiceServiceTest} /
 * {@code InvoicePrintServiceTest}、充值积分在 {@code MemberServiceTest}。</p>
 */
@DisplayName("金额精度源码门禁")
class MoneyPrecisionPolicyTest {

    private static final Path CONTROLLERS = Path.of("src/main/java/com/cashier/controller");
    private static final Path MODEL = Path.of("src/main/java/com/cashier/model");

    @Test
    @DisplayName("挂单金额必须用结账同一算法，不得用 double 反推折扣")
    void holdOrderAmountsUseCheckoutAlgorithm() throws Exception {
        String source = Files.readString(CONTROLLERS.resolve("CartController.java"));
        String code = withoutComments(source);
        String finalBody = methodBody(code, "private java.math.BigDecimal calculateHoldOrderFinal()");

        assertTrue(finalBody.contains("TransactionService.calculateFinalAmount(cartList, currentMember, appliedPromotion)"),
            "calculateHoldOrderFinal 必须直接调用 TransactionService.calculateFinalAmount(...)："
                + "此前用 double discountRate 反推折扣，在 .xx5 边界与结账差 1 分（TD-034）");
        assertFalse(code.contains("double discountRate"),
            "不得再用 `double discountRate = discount.doubleValue() / 10.0` 算金额："
                + "二进制浮点会让挂单落库金额与实际收款不一致（注释里提到旧写法不算）");
    }

    /** 取方法体（按花括号配对）。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法签名: " + signature);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new AssertionError("方法体不闭合: " + signature);
    }

    @Test
    @DisplayName("会员积分显示/保存不得截断小数（历史数据可能是 105.5 分）")
    void memberPointsAreNotTruncatedOnEdit() throws Exception {
        String source = Files.readString(CONTROLLERS.resolve("MemberEditController.java"));

        assertFalse(source.contains("getPoints().intValue()"),
            "会员编辑界面不得用 intValue() 显示积分：会把 105.5 截断成 105，"
                + "用户一保存就永久丢了 0.5 分（TD-034）。应原样显示（stripTrailingZeros/toPlainString）");
        assertTrue(source.contains("stripTrailingZeros"),
            "会员编辑界面应保留积分的小数位（stripTrailingZeros().toPlainString()）");
    }

    @Test
    @DisplayName("促销折扣/金额必须校验小数位（promotions.discount 是 DECIMAL(10,2)）")
    void promotionDiscountScaleIsValidated() throws Exception {
        String source = Files.readString(CONTROLLERS.resolve("PromotionController.java"));

        assertTrue(source.contains("stripTrailingZeros().scale() > 2"),
            "促销的折扣/金额列是 DECIMAL(10,2)：输入 0.985 会被数据库静默存成 0.99（实际打 1% 折而不是 1.5%），"
                + "必须在校验阶段拒绝超过 2 位小数的输入（TD-034）");
        assertTrue(source.contains("promotion.validation.discount_scale"),
            "拒绝时应给出明确的 i18n 提示，而不是静默取整");
    }

    @Test
    @DisplayName("发票行金额/税额必须逐行取整到分，表头才会与明细一致")
    void invoiceItemAmountsAreRoundedPerLine() throws Exception {
        String code = withoutComments(Files.readString(MODEL.resolve("InvoiceItem.java")));

        assertTrue(code.contains("this.amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)"),
            "行税额必须 setScale(2, HALF_UP)：不取整时表头累加未取整值，"
                + "会与明细之和差 1 分（3 × 33.33 @13% → 13.00 vs 12.99，TD-034）");
        assertFalse(code.contains("this.taxAmount = this.amount.multiply(this.taxRate);"),
            "不得保留未取整的行税额计算");
    }

    /** 去掉注释（保留字符串字面量），避免注释里引用的旧写法把门禁写红。 */
    private static String withoutComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        boolean inString = false;
        boolean inChar = false;
        boolean inLine = false;
        boolean inBlock = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (inLine) {
                inLine = c != '\n';
                out.append(c == '\n' ? c : ' ');
            } else if (inBlock) {
                if (c == '*' && next == '/') {
                    inBlock = false;
                    out.append("  ");
                    i++;
                } else {
                    out.append(c == '\n' ? c : ' ');
                }
            } else if (inString || inChar) {
                out.append(c);
                if (c == '\\' && next != '\0') {
                    out.append(next);
                    i++;
                } else if (inString && c == '"') {
                    inString = false;
                } else if (inChar && c == '\'') {
                    inChar = false;
                }
            } else if (c == '/' && next == '/') {
                inLine = true;
                out.append("  ");
                i++;
            } else if (c == '/' && next == '*') {
                inBlock = true;
                out.append("  ");
                i++;
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '\'') {
                    inChar = true;
                }
                out.append(c);
            }
        }
        return out.toString();
    }
}
