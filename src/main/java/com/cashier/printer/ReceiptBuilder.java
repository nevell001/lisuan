package com.cashier.printer;

import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.service.TransactionService;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 小票内容构建（无界面、无数据库：设置与购物车由调用方传入）。
 *
 * <p>从触屏收银台控制器里抽出，便于单测锁住小票上的金额与明细口径——
 * 小票打错金额是直接面向顾客的问题，而 UI 测试在 CI 里跑不了。</p>
 */
public final class ReceiptBuilder {

    private static final Logger logger = LoggerFactoryUtil.getLogger(ReceiptBuilder.class);

    private ReceiptBuilder() {
    }

    /**
     * 构建小票快照。
     *
     * @param cartItems 购物车明细（用于打印行项目与合计）
     * @param member 会员，可为 null
     * @param cashierName 收银员姓名
     * @param paymentMethod 支付方式
     * @param finalAmount 交易实付金额（用于反推优惠额）
     * @param received 实收金额
     * @param change 找零
     * @param settings 打印相关设置（enablePrint/storeName/printLogo/printerName/paperSize）
     * @return 小票快照；打印功能未启用（{@code enablePrint != true}）时返回 null
     */
    public static ReceiptData build(List<CartItem> cartItems, Member member, String cashierName,
                                    String paymentMethod, BigDecimal finalAmount,
                                    BigDecimal received, BigDecimal change, Map<String, String> settings) {
        if (!Boolean.parseBoolean(settings.getOrDefault("enablePrint", "false"))) {
            logger.info("打印功能未启用（enablePrint=false），跳过小票打印");
            return null;
        }

        StringBuilder items = new StringBuilder();
        int totalQty = 0;
        for (CartItem ci : cartItems) {
            items.append(ci.product.name)
                .append(" x").append(ci.quantity)
                .append("  ").append(String.format("%.2f", ci.subtotal.doubleValue()))
                .append("\n");
            totalQty += ci.quantity;
        }
        BigDecimal total = TransactionService.calculateTotalAmount(cartItems);
        BigDecimal discount = total.subtract(finalAmount);
        String memberInfo = member != null
            ? (member.name + "(" + member.phone + ") " + member.level) : null;

        return new ReceiptData(
            settings.getOrDefault("storeName", "狸算收银"),
            cashierName != null ? cashierName : "",
            items.toString(),
            totalQty,
            total.doubleValue(),
            discount.doubleValue(),
            finalAmount.doubleValue(),
            received.doubleValue(),
            change.doubleValue(),
            paymentMethod,
            memberInfo,
            Boolean.parseBoolean(settings.getOrDefault("printLogo", "true")),
            settings.getOrDefault("printerName", "").trim(),
            settings.getOrDefault("paperSize", ""));
    }
}
