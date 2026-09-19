package com.cashier.printer;

import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 小票内容构建测试。
 *
 * <p>小票打错金额是直接面向顾客的问题，而打印路径没有 UI 级测试（CI 无显示环境、无打印机），
 * 因此把内容构建抽成无界面纯函数后在这里锁口径：行项目、合计、优惠、实收、找零、会员信息。</p>
 */
@DisplayName("小票内容构建测试")
class ReceiptBuilderTest {

    @Test
    @DisplayName("小票快照包含明细、合计、优惠、实收找零与会员信息")
    void buildsReceiptWithTotalsAndMember() {
        Product first = product(1, "小票商品A", 10.0);
        Product second = product(2, "小票商品B", 5.0);
        List<CartItem> cart = List.of(new CartItem(first, 2), new CartItem(second, 3));
        Member member = new Member("13800000000", "张三", BigDecimal.TEN, "金卡", BigDecimal.valueOf(9.0));

        ReceiptData data = ReceiptBuilder.build(cart, ReceiptBuilder.MemberSnapshot.of(member),
            "收银员小李", "现金",
            new BigDecimal("31.50"), new BigDecimal("50.00"), new BigDecimal("18.50"), settings());

        assertTrue(data.itemsText.contains("小票商品A x2  20.00"), data.itemsText);
        assertTrue(data.itemsText.contains("小票商品B x3  15.00"), data.itemsText);
        assertEquals(5, data.totalQuantity);
        assertEquals(35.0, data.totalAmount, 0.001, "合计应按原价总额计算");
        assertEquals(3.5, data.discountAmount, 0.001, "优惠额 = 原价总额 - 实付");
        assertEquals(31.5, data.finalAmount, 0.001);
        assertEquals(50.0, data.paidAmount, 0.001);
        assertEquals(18.5, data.changeAmount, 0.001);
        assertEquals("现金", data.paymentMethod);
        assertEquals("张三(13800000000) 金卡", data.memberInfo);
        assertEquals("收银员小李", data.cashierName);
        assertEquals("狸算测试店", data.storeName);
        assertTrue(data.printLogo);
        assertEquals("前台打印机", data.printerName);
        assertEquals("80mm", data.paperSize);
    }

    @Test
    @DisplayName("会员等级取结账前快照：结账后升级不影响本单小票")
    void memberLevelComesFromSnapshotTakenBeforeCheckout() {
        Member member = new Member("13800000000", "张三", BigDecimal.TEN, "普通", BigDecimal.valueOf(9.0));

        // 结账前快照
        ReceiptBuilder.MemberSnapshot atSale = ReceiptBuilder.MemberSnapshot.of(member);

        // 结账会就地改写 member：本单 1800 积分把普通升成银卡、折扣跟着变
        member.level = "银卡";
        member.discount = BigDecimal.valueOf(9.5);
        member.discountRate = BigDecimal.valueOf(9.5);

        ReceiptData data = ReceiptBuilder.build(List.of(new CartItem(product(1, "小票商品A", 10.0), 2)),
            atSale, "收银员小李", "现金", new BigDecimal("18.00"),
            new BigDecimal("20.00"), new BigDecimal("2.00"), settings());

        assertTrue(data.memberInfo.endsWith("普通"),
            "小票必须显示成交时的等级，而不是结账后升级的银卡: " + data.memberInfo);
        assertEquals("张三(13800000000) 普通", data.memberInfo);
    }

    @Test
    @DisplayName("非会员交易不打印会员信息")
    void nonMemberReceiptHasNoMemberInfo() {
        List<CartItem> cart = List.of(new CartItem(product(1, "小票商品A", 10.0), 1));

        ReceiptData data = ReceiptBuilder.build(cart, null, "收银员小李", "微信",
            new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, settings());

        assertNull(data.memberInfo);
        assertEquals(10.0, data.totalAmount, 0.001);
        assertEquals(0.0, data.discountAmount, 0.001);
    }

    @Test
    @DisplayName("打印未启用时不生成小票（调用方据此跳过打印）")
    void disabledPrintingYieldsNoReceipt() {
        List<CartItem> cart = List.of(new CartItem(product(1, "小票商品A", 10.0), 1));
        Map<String, String> disabled = new HashMap<>();

        assertNull(ReceiptBuilder.build(cart, null, "收银员", "现金", BigDecimal.TEN,
            BigDecimal.ZERO, BigDecimal.ZERO, disabled), "缺少 enablePrint 应按关闭处理");
        disabled.put("enablePrint", "false");
        assertNull(ReceiptBuilder.build(cart, null, "收银员", "现金", BigDecimal.TEN,
            BigDecimal.ZERO, BigDecimal.ZERO, disabled));
        disabled.put("enablePrint", "true");
        assertTrue(ReceiptBuilder.build(cart, null, "收银员", "现金", BigDecimal.TEN,
            BigDecimal.ZERO, BigDecimal.ZERO, disabled) != null);
    }

    private static Map<String, String> settings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("enablePrint", "true");
        settings.put("storeName", "狸算测试店");
        settings.put("printLogo", "true");
        settings.put("printerName", "  前台打印机  ");
        settings.put("paperSize", "80mm");
        return settings;
    }

    private static Product product(int id, String name, double price) {
        return new Product(id, name, price, 100, "测试分类", "BAR-" + id, "件",
            "描述", "品牌", "供应商", "规格", 0, price * 0.7);
    }
}
