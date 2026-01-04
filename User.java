import java.text.SimpleDateFormat;
import java.util.Date;

public class User {
    public String username;      // 用户名
    public String password;      // 密码（实际应用中应该加密存储）
    public String name;          // 真实姓名
    public String role;          // 角色：admin(管理员)、cashier(收银员)、finance(财务)
    public Date createTime;      // 创建时间
    public Date lastLoginTime;   // 最后登录时间
    public boolean active;       // 是否激活

    public User() {
        this.username = "";
        this.password = "";
        this.name = "";
        this.role = "cashier";
        this.createTime = new Date();
        this.lastLoginTime = new Date();
        this.active = true;
    }

    public User(String username, String password, String name, String role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
        this.createTime = new Date();
        this.lastLoginTime = new Date();
        this.active = true;
    }

    // 检查是否有指定权限
    public boolean hasPermission(String permission) {
        if (!active) {
            return false;
        }

        switch (role) {
            case "admin":
                // 管理员拥有所有权限
                return true;
            case "cashier":
                // 收银员权限：收银、查看商品
                return "checkout".equals(permission) || 
                       "view_inventory".equals(permission) ||
                       "view_transactions".equals(permission);
            case "finance":
                // 财务权限：查看报表、导出数据
                return "view_reports".equals(permission) || 
                       "export_data".equals(permission) ||
                       "view_transactions".equals(permission);
            default:
                return false;
        }
    }

    // 获取角色显示名称
    public String getRoleDisplayName() {
        switch (role) {
            case "admin": return "管理员";
            case "cashier": return "收银员";
            case "finance": return "财务";
            default: return "未知";
        }
    }
}