package com.cashier.model;

import java.util.Date;

/**
 * 退货占用台账（F10）：一行 = 一张退货单对某个商品的占用数量。
 *
 * <p>建单时与退货单、明细写在**同一事务**里，用来把"累计退货量 ≤ 原销量"这条不变式落到数据库：
 * 并发建单靠原交易行锁串行化，可退余量 = 原单数量 − 台账占用合计。</p>
 *
 * <p><b>是否占用以父退货单的状态为准</b>（{@code return_orders.status <> 'REJECTED'}），
 * 台账行的 {@link #status} 只用于审计与后续（F10-b）的状态同步——这样即使台账状态尚未同步，
 * 被驳回的退货也不会继续占额度。</p>
 */
public class ReturnReservation {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_REJECTED = "REJECTED";

    public int id;

    /** 退货单号 */
    public String returnOrderId;

    /** 原交易号 */
    public String originalTransactionId;

    /** 商品ID（历史数据 product_id 为空时按 0 记账） */
    public int productId;

    /** 占用数量 */
    public int quantity;

    /** 台账行状态（占用与否以父退货单状态为准，见类注释） */
    public String status = STATUS_PENDING;

    public Date createTime;
}
