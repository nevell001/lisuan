import java.text.SimpleDateFormat;
import java.util.Date;

public class RechargeRecord {
    public String recordId;      // 充值记录ID
    public String memberPhone;   // 会员手机号
    public String memberName;    // 会员姓名
    public double amount;        // 充值金额
    public String paymentMethod; // 支付方式
    public Date timestamp;       // 充值时间
    public String operator;      // 操作员

    public RechargeRecord() {
        this.recordId = "";
        this.memberPhone = "";
        this.memberName = "";
        this.amount = 0;
        this.paymentMethod = "现金";
        this.timestamp = new Date();
        this.operator = "系统";
    }

    public RechargeRecord(String recordId, String memberPhone, String memberName, double amount, String paymentMethod, String operator) {
        this.recordId = recordId;
        this.memberPhone = memberPhone;
        this.memberName = memberName;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.timestamp = new Date();
        this.operator = operator;
    }
}