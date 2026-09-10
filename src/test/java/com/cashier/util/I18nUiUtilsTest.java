package com.cashier.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 支付方式归一化测试。
 *
 * <p>收银端把支付方式以中文写入 transactions.payment_method，而交易列表的筛选下拉框
 * 用的是代码（CASH/WECHAT/ALIPAY/CARD）。两者不归一化会导致按支付方式筛选永远为空。</p>
 */
@DisplayName("支付方式归一化")
class I18nUiUtilsTest {

    @Test
    @DisplayName("中文名称与代码归一化为同一代码")
    void canonicalizesChineseNamesAndCodes() {
        assertEquals("CASH", I18nUiUtils.canonicalPaymentMethod("现金"));
        assertEquals("CASH", I18nUiUtils.canonicalPaymentMethod("CASH"));
        assertEquals("WECHAT", I18nUiUtils.canonicalPaymentMethod("微信"));
        assertEquals("WECHAT", I18nUiUtils.canonicalPaymentMethod("WECHAT"));
        assertEquals("ALIPAY", I18nUiUtils.canonicalPaymentMethod("支付宝"));
        assertEquals("ALIPAY", I18nUiUtils.canonicalPaymentMethod("ALIPAY"));
        assertEquals("CARD", I18nUiUtils.canonicalPaymentMethod("银行卡"));
        assertEquals("CARD", I18nUiUtils.canonicalPaymentMethod("CARD"));
    }

    @Test
    @DisplayName("下拉框选中的代码能匹配收银端落库的中文，筛选不再恒为空")
    void comboCodeMatchesStoredChinese() {
        assertEquals(
            I18nUiUtils.canonicalPaymentMethod("CASH"),
            I18nUiUtils.canonicalPaymentMethod("现金"));
        assertEquals(
            I18nUiUtils.canonicalPaymentMethod("WECHAT"),
            I18nUiUtils.canonicalPaymentMethod("微信"));
        assertEquals(
            I18nUiUtils.canonicalPaymentMethod("ALIPAY"),
            I18nUiUtils.canonicalPaymentMethod("支付宝"));
        assertEquals(
            I18nUiUtils.canonicalPaymentMethod("CARD"),
            I18nUiUtils.canonicalPaymentMethod("银行卡"));
    }

    @Test
    @DisplayName("“全部”与未知值原样返回，不误伤自定义支付方式")
    void unknownValuesPassThrough() {
        assertEquals("全部", I18nUiUtils.canonicalPaymentMethod("全部"));
        assertEquals("其他方式", I18nUiUtils.canonicalPaymentMethod("其他方式"));
        assertNull(I18nUiUtils.canonicalPaymentMethod(null));
    }

    @Test
    @DisplayName("中文与代码显示为同一本地化名称")
    void bothFormsLocalizeToSameLabel() {
        assertEquals(I18nUiUtils.paymentMethod("现金"), I18nUiUtils.paymentMethod("CASH"));
        assertEquals(I18nUiUtils.paymentMethod("微信"), I18nUiUtils.paymentMethod("WECHAT"));
        assertEquals(I18nUiUtils.paymentMethod("银行卡"), I18nUiUtils.paymentMethod("CARD"));
    }
}
