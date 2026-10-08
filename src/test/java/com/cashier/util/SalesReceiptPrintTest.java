package com.cashier.util;

import com.cashier.model.CartItem;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.printer.EscPosUtils;
import com.cashier.printer.PrintTask;
import com.cashier.printer.PrintTemplate;
import com.cashier.printer.PrintUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 销售小票内容测试（无界面、不打印、不落盘）。
 *
 * <p>背景：系统设置里的 {@code storeAddress}/{@code storePhone} 此前**只存不读**，任何小票上都看不到
 * 门店地址与电话。这里锁住两条打印路径（文本小票与打印模板）的门店信息格式：填了就印；
 * "地址: "这种没有内容的标签行、以及多余的空行，都不允许出现。</p>
 *
 * <p>另外锁住条码的载体：条码必须是挂在打印任务上的 ESC/POS 原始字节（由
 * {@code NetworkPrinterDevice} 直写设备），而不是混进小票文本——真实打印机只有在
 * ESC/POS 网络打印机路径上才会写出这些字节。</p>
 */
@DisplayName("销售小票内容测试")
class SalesReceiptPrintTest {

    @Test
    @DisplayName("设置了地址与电话时，两条信息都打印在页头之后")
    void storeAddressAndPhoneArePrintedWhenConfigured() {
        String content = ReceiptPrinter.generateReceiptContent(transaction(), cart(), null, null,
            "杭州市西湖区文一西路 1 号", "0571-88886666");

        int orderIndex = content.indexOf("订单号:");
        assertTrue(orderIndex > 0, content);
        String header = content.substring(0, orderIndex);
        assertTrue(header.endsWith("地址: 杭州市西湖区文一西路 1 号\n电话: 0571-88886666\n"),
            "门店地址/电话应紧跟在页头之后：" + header);
    }

    @Test
    @DisplayName("地址/电话留空时不打印空标签行，也不多出空行")
    void blankStoreInfoAddsNoLine() {
        String content = ReceiptPrinter.generateReceiptContent(transaction(), cart(), null, null, "   ", null);

        assertFalse(content.contains("地址:"), content);
        assertFalse(content.contains("电话:"), content);

        int orderIndex = content.indexOf("订单号:");
        assertTrue(content.substring(0, orderIndex).endsWith("====\n\n"),
            "门店信息为空时页头之后应直接是订单号，不得多出空行：" + content.substring(0, orderIndex));
    }

    @Test
    @DisplayName("只填一项时只打印那一行，两项都空时返回空串")
    void onlyNonBlankFieldIsPrinted() {
        assertEquals("地址: 某路 1 号\n", PrintUtil.storeContactLines(" 某路 1 号 ", null));
        assertEquals("电话: 010-1234\n", PrintUtil.storeContactLines("", "010-1234"));
        assertEquals("", PrintUtil.storeContactLines(null, "   "));
    }

    @Test
    @DisplayName("小票模板的 {{storeInfo}} 由调用方提供，留空时不留占位符也不留空行")
    void receiptTemplateSubstitutesStoreInfo() {
        String filled = generateTemplateText(PrintUtil.storeContactLines("某路 1 号", "010-1234"));
        assertFalse(filled.contains("{{storeInfo}}"), "调用方必须提供 storeInfo 变量：" + filled);
        assertTrue(filled.contains("门店名称: 狸算测试店\n地址: 某路 1 号\n电话: 010-1234\n收银员: 小李\n"),
            filled);

        String blank = generateTemplateText(PrintUtil.storeContactLines(null, " "));
        assertFalse(blank.contains("{{storeInfo}}"), blank);
        assertTrue(blank.contains("门店名称: 狸算测试店\n收银员: 小李\n"),
            "门店信息为空时应直接接下一行，不留空行：" + blank);
    }

    @Test
    @DisplayName("条码以 ESC/POS Code128 原始字节挂在打印任务上，未开启时不带任何字节")
    void barcodeIsCarriedAsRawEscPosBytes() {
        byte[] barcode = EscPosUtils.barcodeCode128("RCPT-TEST-001");
        assertEquals((byte) 0x1D, barcode[0], "GS");
        assertEquals((byte) 0x6B, barcode[1], "k");
        assertEquals((byte) 73, barcode[2], "Code128 的 m 值");
        assertEquals((byte) 13, barcode[3], "数据长度");
        assertEquals("RCPT-TEST-001", new String(barcode, 4, barcode.length - 4, StandardCharsets.US_ASCII));

        PrintTask withBarcode = PrintTask.createReceiptTask("小票正文", false, true, barcode);
        assertArrayEquals(barcode, withBarcode.getRawPrintBytes());
        assertFalse(withBarcode.getContent().contains("\u001D"),
            "条码字节不得混进小票文本，否则文本/文件小票也会输出乱码");

        assertNull(PrintTask.createReceiptTask("小票正文", false, true).getRawPrintBytes(),
            "未开启打印条码时任务不得携带任何原始字节");
    }

    private static String generateTemplateText(String storeInfo) {
        PrintTemplate template = PrintTemplate.createReceiptTemplate();
        template.setVariable("storeName", "狸算测试店");
        template.setVariable("storeInfo", storeInfo);
        template.setVariable("cashierName", "小李");
        return template.generate();
    }

    private static Transaction transaction() {
        Transaction transaction = new Transaction();
        transaction.transactionId = "RCPT-STORE-INFO-001";
        transaction.timestamp = "2026-09-20 10:00:00";
        transaction.totalAmount = new BigDecimal("30.00");
        transaction.tax = BigDecimal.ZERO;
        transaction.finalAmount = new BigDecimal("30.00");
        transaction.paymentMethod = "现金";
        transaction.items = List.of(product(1, "小票测试商品", new BigDecimal("10.00"), 3));
        return transaction;
    }

    private static List<CartItem> cart() {
        return List.of(new CartItem(product(1, "小票测试商品", new BigDecimal("10.00"), 3), 3));
    }

    private static Product product(int id, String name, BigDecimal price, int quantity) {
        Product product = new Product(id, name, price, quantity, "测试分类", "STORE-BAR-" + id,
            "件", "描述", "品牌", "供应商", "规格", 0, price);
        product.productCode = "STORE-" + id;
        return product;
    }
}
