package com.cashier.model;

import java.math.BigDecimal;

public class Member {
    private static final BigDecimal DIAMOND_POINTS = new BigDecimal("10000");
    private static final BigDecimal GOLD_POINTS = new BigDecimal("5000");
    private static final BigDecimal SILVER_POINTS = new BigDecimal("1000");
    private static final BigDecimal DIAMOND_DISCOUNT = new BigDecimal("8.5");
    private static final BigDecimal GOLD_DISCOUNT = new BigDecimal("9.0");
    private static final BigDecimal SILVER_DISCOUNT = new BigDecimal("9.5");
    private static final BigDecimal NORMAL_DISCOUNT = new BigDecimal("10.0");

    public int id;               // 会员ID（数据库自增主键）
    public String memberCode;     // 会员编号（用户自定义编号）
    public String phone;          // 会员手机号（唯一标识）
    public String name;           // 会员姓名
    public BigDecimal points;     // 积分
    public String level;          // 等级（普通、银卡、金卡、钻石）
    public BigDecimal discount;   // 折扣值（10表示不打折，9.8表示9.8折，9表示9折，0表示免费）
    public BigDecimal discountRate;   // 折扣率（与discount相同，用于兼容）
    public BigDecimal balance;    // 会员余额
    public String birthday;       // 生日（格式：MM-dd）

    public Member() {
        this.id = 0;  // 默认ID为0，表示未保存到数据库
        this.memberCode = "";  // 会员编号
        this.points = BigDecimal.ZERO;
        this.discount = NORMAL_DISCOUNT;
        this.discountRate = NORMAL_DISCOUNT;
        this.balance = BigDecimal.ZERO;
    }

    public Member(String phone, String name) {
        this();
        this.phone = phone;
        this.name = name;
        this.level = "普通";
        this.birthday = "";
    }

    public Member(String phone, String name, BigDecimal points, String level, BigDecimal discount) {
        this(phone, name);
        this.points = defaultDecimal(points);
        this.level = level;
        this.discount = defaultDecimal(discount);
        this.discountRate = this.discount;
    }

    public Member(String phone, String name, double points, String level, double discount) {
        this(phone, name, BigDecimal.valueOf(points), level, BigDecimal.valueOf(discount));
    }

    public Member(String phone, String name, BigDecimal points, String level, BigDecimal discount, BigDecimal balance, String birthday) {
        this();
        this.phone = phone;
        this.name = name;
        this.points = defaultDecimal(points);
        this.level = level;
        this.discount = defaultDecimal(discount);
        this.discountRate = this.discount;
        this.balance = defaultDecimal(balance);
        this.birthday = birthday;
    }

    public Member(String phone, String name, double points, String level, double discount, double balance, String birthday) {
        this(phone, name, BigDecimal.valueOf(points), level, BigDecimal.valueOf(discount), BigDecimal.valueOf(balance), birthday);
    }

    public Member(int id, String phone, String name, BigDecimal points, String level, BigDecimal discount, BigDecimal balance, String birthday) {
        this(phone, name, points, level, discount, balance, birthday);
        this.id = id;
    }

    public Member(int id, String phone, String name, double points, String level, double discount, double balance, String birthday) {
        this(id, phone, name, BigDecimal.valueOf(points), level, BigDecimal.valueOf(discount), BigDecimal.valueOf(balance), birthday);
    }

    public Member(int id, String memberCode, String phone, String name, BigDecimal points, String level, BigDecimal discount, BigDecimal balance, String birthday) {
        this(id, phone, name, points, level, discount, balance, birthday);
        this.memberCode = memberCode;
    }

    public Member(int id, String memberCode, String phone, String name, double points, String level, double discount, double balance, String birthday) {
        this(id, memberCode, phone, name, BigDecimal.valueOf(points), level, BigDecimal.valueOf(discount), BigDecimal.valueOf(balance), birthday);
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // 根据积分更新等级和折扣
    public void updateLevel() {
        if (defaultDecimal(points).compareTo(DIAMOND_POINTS) >= 0) {
            level = "钻石";
            discount = DIAMOND_DISCOUNT;  // 8.5折
            discountRate = DIAMOND_DISCOUNT;
        } else if (defaultDecimal(points).compareTo(GOLD_POINTS) >= 0) {
            level = "金卡";
            discount = GOLD_DISCOUNT;  // 9折
            discountRate = GOLD_DISCOUNT;
        } else if (defaultDecimal(points).compareTo(SILVER_POINTS) >= 0) {
            level = "银卡";
            discount = SILVER_DISCOUNT;  // 9.5折
            discountRate = SILVER_DISCOUNT;
        } else {
            level = "普通";
            discount = NORMAL_DISCOUNT;   // 不打折
            discountRate = NORMAL_DISCOUNT;
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

    public String getMemberCode() {
        return memberCode;
    }

    public String getPhone() {
        return phone;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPoints() {
        return defaultDecimal(points);
    }

    public String getLevel() {
        return level;
    }

    public BigDecimal getDiscount() {
        return defaultDecimal(discount);
    }

    public BigDecimal getDiscountRate() {
        return defaultDecimal(discountRate);
    }

    public BigDecimal getBalance() {
        return defaultDecimal(balance);
    }

    public String getBirthday() {
        return birthday;
    }
}