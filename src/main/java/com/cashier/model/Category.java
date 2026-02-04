package com.cashier.model;

public class Category {
    public int id;               // 分类ID（数据库自增主键）
    public String name;
    public String description;

    public Category() {
        this.id = 0;  // 默认ID为0，表示未保存到数据库
    }

    public Category(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    public Category(String name) {
        this(name, "");
    }

    public Category(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getter方法
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}