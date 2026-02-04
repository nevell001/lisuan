package com.cashier.model;

/**
 * 单位模型类
 * 用于表示商品计量单位
 */
public class Unit {
    public int id;               // 单位ID（数据库自增主键）
    public String name;
    public String description;

    /**
     * 默认构造函数
     */
    public Unit() {
        this.id = 0;  // 默认ID为0，表示未保存到数据库
    }

    /**
     * 构造函数
     * @param name 单位名称（如：个、箱、瓶、公斤等）
     * @param description 单位描述
     */
    public Unit(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    /**
     * 构造函数（仅名称）
     * @param name 单位名称
     */
    public Unit(String name) {
        this(name, "");
    }

    public Unit(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Unit unit = (Unit) obj;
        return id == unit.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}