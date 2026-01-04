import java.text.SimpleDateFormat;
import java.util.Date;

public class OperationLog {
    public String logId;         // 日志ID
    public String username;      // 用户名
    public String operation;     // 操作类型
    public String details;       // 操作详情
    public Date timestamp;       // 操作时间
    public String ipAddress;     // IP地址（预留）

    public OperationLog() {
        this.logId = "";
        this.username = "";
        this.operation = "";
        this.details = "";
        this.timestamp = new Date();
        this.ipAddress = "";
    }

    public OperationLog(String logId, String username, String operation, String details) {
        this.logId = logId;
        this.username = username;
        this.operation = operation;
        this.details = details;
        this.timestamp = new Date();
        this.ipAddress = "";
    }
}