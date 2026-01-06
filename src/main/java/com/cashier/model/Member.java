public class Member {
    String phone;          // 会员手机号（唯一标识）
    String name;           // 会员姓名
    double points;         // 积分
    String level;          // 等级（普通、银卡、金卡、钻石）
    double discount;       // 折扣率（1.0表示不打折，0.9表示9折）
    double balance;        // 会员余额
    String birthday;       // 生日（格式：MM-dd）

    public Member(String phone, String name) {
        this.phone = phone;
        this.name = name;
        this.points = 0;
        this.level = "普通";
        this.discount = 1.0;
        this.balance = 0;
        this.birthday = "";
    }

    public Member(String phone, String name, double points, String level, double discount) {
        this.phone = phone;
        this.name = name;
        this.points = points;
        this.level = level;
        this.discount = discount;
        this.balance = 0;
        this.birthday = "";
    }

    public Member(String phone, String name, double points, String level, double discount, double balance, String birthday) {
        this.phone = phone;
        this.name = name;
        this.points = points;
        this.level = level;
        this.discount = discount;
        this.balance = balance;
        this.birthday = birthday;
    }

    // 根据积分更新等级和折扣
    public void updateLevel() {
        if (points >= 10000) {
            level = "钻石";
            discount = 0.85;  // 8.5折
        } else if (points >= 5000) {
            level = "金卡";
            discount = 0.90;  // 9折
        } else if (points >= 2000) {
            level = "银卡";
            discount = 0.95;  // 9.5折
        } else {
            level = "普通";
            discount = 1.0;   // 不打折
        }
    }

    // 检查是否是会员生日
    public boolean isBirthdayToday() {
        if (birthday == null || birthday.isEmpty()) {
            return false;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM-dd");
        String today = sdf.format(new java.util.Date());
        return today.equals(birthday);
    }
}