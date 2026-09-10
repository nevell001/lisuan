package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结账口径一致性门禁。
 *
 * <p>回归：税率、促销、支付方式三处口径曾在标准收银台 / 触屏收银台 / 交易列表中
 * 各写一份，导致税率被多除 100、触屏版不算促销、按支付方式筛选永远为空。</p>
 */
class CheckoutConsistencyPolicyTest {

    private static String readMainSource(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/com/cashier", relativePath));
    }

    @Test
    @DisplayName("税额只在 TransactionService.calculateTax 计算，界面不得再除以 100")
    void taxIsSingleSourced() throws Exception {
        String service = readMainSource("service/TransactionService.java");
        String cart = readMainSource("controller/CartController.java");
        String touch = readMainSource("controller/TouchCartController.java");

        assertTrue(service.contains("public static BigDecimal calculateTax(BigDecimal amount)"),
            "税额计算必须收敛到 TransactionService.calculateTax");
        assertTrue(cart.contains("TransactionService.calculateTax("),
            "标准收银台必须调用统一的税额计算");
        assertTrue(touch.contains("TransactionService.calculateTax("),
            "触屏收银台必须调用统一的税额计算");

        assertFalse(cart.contains(".divide(BigDecimal.valueOf(100)"),
            "税率是小数形式（0.0-1.0），不得再按百分比除以 100");
        assertFalse(touch.contains(".divide(BigDecimal.valueOf(100)"),
            "税率是小数形式（0.0-1.0），不得再按百分比除以 100");
    }

    @Test
    @DisplayName("触屏收银台必须计算并落库促销优惠")
    void touchPosAppliesPromotions() throws Exception {
        String touch = readMainSource("controller/TouchCartController.java");

        assertTrue(touch.contains("private Promotion appliedPromotion"),
            "触屏收银台需要持有当前促销");
        assertTrue(touch.contains("TransactionService.selectBestPromotion("),
            "触屏收银台必须选取最优促销");
        assertTrue(touch.contains("calculateFinalAmount(cartItems, currentMember, appliedPromotion)"),
            "触屏收银台应付金额必须包含促销优惠");
        assertTrue(touch.contains("promotionToApply"),
            "结账必须把促销传给 executeTransaction 以便累加使用次数");
        assertFalse(touch.contains("inventoryMap, null)"),
            "结账不得再以 null 促销落库（会导致促销不生效/次数不累加）");
    }

    @Test
    @DisplayName("退款按整单实付比例折算；标准收银台金额与扣减库存口径统一")
    void refundAndInventoryUseSharedRules() throws Exception {
        String returnDialog = readMainSource("controller/CreateReturnOrderDialogController.java");
        String cart = readMainSource("controller/CartController.java");
        String service = readMainSource("service/TransactionService.java");

        assertTrue(returnDialog.contains("refundUnitPrice("),
            "退货单价必须按整单实付比例折算，不能直接用商品原价（否则折扣全额退给顾客）");
        assertTrue(cart.contains("TransactionService.calculateFinalAmount(cartList, currentMember, appliedPromotion)"),
            "标准收银台应付金额必须与触屏版共用 TransactionService 口径（含四舍五入）");
        assertFalse(service.contains("product.quantity = latestProduct.quantity"),
            "扣减库存不得改动调用方共享的内存/缓存对象（回滚后无法复原）");
        assertTrue(service.contains("updatedProducts.add(latestProduct)"),
            "扣减库存应以事务内重读的最新行为准，避免覆盖别处刚改的价格/名称");
    }

    @Test
    @DisplayName("支付方式筛选与统计按归一化后的代码比较")
    void paymentMethodIsComparedAfterNormalization() throws Exception {
        String transactionController = readMainSource("controller/TransactionController.java");
        String transactionDAO = readMainSource("dao/TransactionDAORefactored.java");

        assertTrue(transactionController.contains("canonicalPaymentMethod"),
            "交易列表筛选必须归一化后再比较（历史数据存中文，下拉框用代码）");
        assertTrue(transactionDAO.contains("IN ('现金', 'CASH')"),
            "现金笔数统计必须同时匹配中文落库值与代码形式");
    }
}
