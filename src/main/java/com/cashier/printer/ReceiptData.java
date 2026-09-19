package com.cashier.printer;

/**
 * 打印小票所需的一条交易快照。
 *
 * <p>打印线程只依赖这份不可变数据，不再回头读控制器/数据库状态。</p>
 */
public final class ReceiptData {

    public final String storeName;
    public final String cashierName;
    public final String itemsText;
    public final int totalQuantity;
    public final double totalAmount;
    public final double discountAmount;
    public final double finalAmount;
    public final double paidAmount;
    public final double changeAmount;
    public final String paymentMethod;
    public final String memberInfo;
    public final boolean printLogo;
    public final String printerName;
    public final String paperSize;

    public ReceiptData(String storeName, String cashierName, String itemsText, int totalQuantity,
                       double totalAmount, double discountAmount, double finalAmount, double paidAmount,
                       double changeAmount, String paymentMethod, String memberInfo, boolean printLogo,
                       String printerName, String paperSize) {
        this.storeName = storeName;
        this.cashierName = cashierName;
        this.itemsText = itemsText;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.paidAmount = paidAmount;
        this.changeAmount = changeAmount;
        this.paymentMethod = paymentMethod;
        this.memberInfo = memberInfo;
        this.printLogo = printLogo;
        this.printerName = printerName;
        this.paperSize = paperSize;
    }
}
