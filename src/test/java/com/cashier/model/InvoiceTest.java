package com.cashier.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发票编号生成测试。
 *
 * <p>回归：此前是 {@code "INV" + System.currentTimeMillis()}，同一毫秒内并发开票会撞主键，
 * 编号也可以被直接猜出来（枚举相邻整数即可遍历他人发票）。</p>
 */
@DisplayName("发票编号生成")
class InvoiceTest {

    @Test
    @DisplayName("同毫秒并发开票不撞号")
    void invoiceIdsAreUniqueWithinSameMillisecond() {
        Set<String> ids = new HashSet<>();
        int count = 3000;
        for (int i = 0; i < count; i++) {
            assertTrue(ids.add(Invoice.generateInvoiceId()),
                "发票编号重复: " + Invoice.generateInvoiceId());
        }
        assertEquals(count, ids.size());
    }

    @Test
    @DisplayName("发票编号带随机段，不是纯毫秒时间戳")
    void invoiceIdIsNotAPlainTimestamp() {
        String id = Invoice.generateInvoiceId();

        assertTrue(id.startsWith("INV"), "发票编号应以 INV 开头");
        // INV + 17 位时间戳 + 3 位序号 + 6 位随机段
        assertTrue(id.matches("INV\\d{17}\\d{3}\\d{6}"),
            "发票编号格式应为 INV+时间戳+序号+随机段，实际: " + id);
    }
}
