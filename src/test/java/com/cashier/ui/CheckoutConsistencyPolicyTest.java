package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

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
    @DisplayName("三处结账路径口径一致：total_amount = 明细原价合计，tax 按实付金额计")
    void allCheckoutPathsUseOriginalTotal() throws Exception {
        String cart = readMainSource("controller/CartController.java");
        String touch = readMainSource("controller/TouchCartController.java");
        String api = readMainSource("api/controller/TransactionApiController.java");
        String service = readMainSource("service/TransactionService.java");

        for (String source : List.of(cart, touch, api)) {
            assertTrue(source.contains("totalAmount = TransactionService.calculateTotalAmount("),
                "total_amount 必须写明细原价合计（写折后金额会让 total_amount - final_amount 恒为 0）");
            assertTrue(source.contains("calculateTax("),
                "税额必须走 TransactionService.calculateTax");
            assertFalse(source.contains("totalAmount = getFinalAmount()")
                    || source.contains("totalAmount = getPayableAmount()"),
                "不得把折后/应付金额写进 total_amount");
        }

        // 税额基数按业务确认改为**实付金额**（税是价内税，不参与应付计算），四处都必须一致
        Pattern taxOnFinal = Pattern.compile("calculateTax\\((?:transaction|tx)\\.finalAmount\\)");
        assertTrue(taxOnFinal.matcher(cart).find(), "标准收银台税额基数应为实付金额");
        assertTrue(taxOnFinal.matcher(touch).find(), "触屏收银台税额基数应为实付金额");
        assertTrue(taxOnFinal.matcher(api).find(), "REST API 税额基数应为实付金额");
        assertTrue(service.contains("calculateTax(transaction.finalAmount)"),
            "TransactionService 自身的建单路径也要按实付计税");

        // 回归：不得再按原价合计计税（会产生"打折了但税没少"的口径）
        for (String source : List.of(cart, touch, api, service)) {
            assertFalse(source.contains("calculateTax(transaction.totalAmount)")
                    || source.contains("calculateTax(tx.totalAmount)")
                    || source.contains("calculateTax(transaction.getTotalAmount())"),
                "税额基数不得再是原价合计");
        }
    }

    @Test
    @DisplayName("触屏小票的会员快照必须在结账前取")
    void touchReceiptSnapshotsMemberBeforeCheckout() throws Exception {
        String touch = readMainSource("controller/TouchCartController.java");

        int snapshot = touch.indexOf("ReceiptBuilder.MemberSnapshot.of(currentMember)");
        int checkout = touch.indexOf("TransactionService.executeTransaction(");
        assertTrue(snapshot > 0, "触屏结账应先取一份会员快照供小票使用");
        assertTrue(checkout > 0, "触屏结账应调用 TransactionService.executeTransaction");
        assertTrue(snapshot < checkout,
            "会员快照必须在 executeTransaction 之前取：结账会就地改写 currentMember 的等级/折扣/积分，"
                + "结账后再取会把升级后的等级印进本单小票");
        assertFalse(touch.contains("ReceiptBuilder.build(cartItems, currentMember"),
            "小票不得直接传 currentMember（ReceiptBuilder 只接受结账前的 MemberSnapshot）");
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
        String transactionApi = readMainSource("api/controller/TransactionApiController.java");

        assertTrue(transactionController.contains("canonicalPaymentMethod"),
            "交易列表筛选必须归一化后再比较（历史数据存中文，下拉框用代码）");
        assertTrue(transactionDAO.contains("IN ('现金', 'CASH')"),
            "现金笔数统计必须同时匹配中文落库值与代码形式");
        assertTrue(transactionApi.contains("canonicalPaymentMethod"),
            "REST 交易列表筛选同样必须归一化后再比较（否则 ?paymentMethod=CASH 查不到「现金」）");
    }
}
