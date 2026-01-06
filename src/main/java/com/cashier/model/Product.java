package com.cashier.model;

public class Product {
    public String name;
    public double price;
    public int quantity;
    public String category;
    public String barcode;        // 条形码
    public String unit;           // 单位（个、kg、瓶等）
    public String description;     // 商品描述
    public String brand;          // 品牌
    public String supplier;       // 供应商
    public String spec;           // 规格
    public int minStock;         // 最低库存预警
    public double cost;          // 成本价

    public Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.category = "默认分类";
        this.barcode = "";
        this.unit = "个";
        this.description = "";
        this.brand = "";
        this.supplier = "";
        this.spec = "";
        this.minStock = 10;
        this.cost = price * 0.7;  // 默认成本价为售价的70%
    }

    public Product(String name, double price, int quantity, String category) {
        this(name, price, quantity);
        this.category = category;
    }

    public Product(String name, double price, int quantity, String category, String barcode, String unit, String description) {
        this(name, price, quantity, category);
        this.barcode = barcode;
        this.unit = unit;
        this.description = description;
        this.brand = "";
        this.supplier = "";
        this.spec = "";
        this.minStock = 10;
        this.cost = price * 0.7;
    }

    // Getter方法
    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getCategory() {
        return category;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    public String getBrand() {
        return brand;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getSpec() {
        return spec;
    }

    public int getMinStock() {
        return minStock;
    }

    public double getCost() {
        return cost;
    }
}