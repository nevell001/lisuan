import java.util.List;

public class Transaction {
    String transactionId;
    String timestamp;
    List<Product> items;
    double totalAmount;
    double tax;
    double finalAmount;
    String paymentMethod;  // 支付方式：现金、微信支付、支付宝、银行卡、组合支付

    public Transaction(String transactionId, String timestamp, List<Product> items, double totalAmount, double tax, double finalAmount) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.items = items;
        this.totalAmount = totalAmount;
        this.tax = tax;
        this.finalAmount = finalAmount;
        this.paymentMethod = "";
    }

    public Transaction(String transactionId, String timestamp, List<Product> items, double totalAmount, double tax, double finalAmount, String paymentMethod) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.items = items;
        this.totalAmount = totalAmount;
        this.tax = tax;
        this.finalAmount = finalAmount;
        this.paymentMethod = paymentMethod;
    }
}