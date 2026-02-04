package com.cashier.model;

public class Member {
    public int id;               // 会员ID（数据库自增主键）
    public String phone;          // 会员手机号（唯一标识）
    public String name;           // 会员姓名
    public double points;         // 积分
    public String level;          // 等级（普通、银卡、金卡、钻石）
    public double discount;       // 折扣值（10表示不打折，9.8表示9.8折，9表示9折，0表示免费）
    public double discountRate;   // 折扣率（与discount相同，用于兼容）
    public double balance;        // 会员余额
    public String birthday;       // 生日（格式：MM-dd）

    public Member() {
        this.id = 0;  // 默认ID为0，表示未保存到数据库
    }

    public Member(String phone, String name) {
        this();
        this.phone = phone;
        this.name = name;
        this.points = 0;
        this.level = "普通";
        this.discount = 10.0;
        this.discountRate = 10.0;
        this.balance = 0;
        this.birthday = "";
    }

    public Member(String phone, String name, double points, String level, double discount) {
        this(phone, name);
        this.points = points;
        this.level = level;
        this.discount = discount;
        this.discountRate = discount;
    }

    public Member(String phone, String name, double points, String level, double discount, double balance, String birthday) {
        this();
        this.phone = phone;
        this.name = name;
        this.points = points;
        this.level = level;
        this.discount = discount;
        this.discountRate = discount;
        this.balance = balance;
        this.birthday = birthday;
    }

    public Member(int id, String phone, String name, double points, String level, double discount, double balance, String birthday) {
        this.id = id;
        this.phone = phone;
        this.name = name;
        this.points = points;
        this.level = level;
        this.discount = discount;
        this.discountRate = discount;
        this.balance = balance;
        this.birthday = birthday;
    }

    // 根据积分更新等级和折扣
    public void updateLevel() {
        if (points >= 10000) {
            level = "钻石";
            discount = 8.5;  // 8.5折
            discountRate = 8.5;
        } else if (points >= 5000) {
            level = "金卡";
            discount = 9.0;  // 9折
            discountRate = 9.0;
        } else if (points >= 2000) {
            level = "银卡";
            discount = 9.5;  // 9.5折
            discountRate = 9.5;
        } else {
            level = "普通";
            discount = 10.0;   // 不打折
            discountRate = 10.0;
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

    // Getter方法
    public int getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getName() {
        return name;
    }

    public double getPoints() {
        return points;
    }

    public String getLevel() {
        return level;
    }

    public double getDiscount() {
        return discount;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public double getBalance() {
        return balance;
    }

    public String getBirthday() {
        return birthday;
    }
}