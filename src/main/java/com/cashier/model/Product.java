package com.cashier.model;

import java.math.BigDecimal;

public class Product {
    private static final BigDecimal DEFAULT_COST_RATE = new BigDecimal("0.7");

    public int id;               // 商品ID（数据库自增主键）
    public String productCode;   // 商品编号（用户自定义编号）
    public String name;
    public BigDecimal price;
    public int quantity;
    public String category;
    public String barcode;        // 条形码
    public String unit;           // 单位（个、kg、瓶等）
    public String description;     // 商品描述
    public String brand;          // 品牌
    public String supplier;       // 供应商
    public String spec;           // 规格
    public int minStock;         // 最低库存预警
    public BigDecimal cost;      // 成本价
    public int version;          // 版本号（用于乐观锁）

    public Product() {
        this.id = 0;  // 默认ID为0，表示未保存到数据库
        this.productCode = "";  // 商品编号
        this.price = BigDecimal.ZERO;
        this.cost = BigDecimal.ZERO;
        this.version = 0;  // 默认版本号为0
    }

    public Product(String name, BigDecimal price, int quantity) {
        this();
        this.name = name;
        this.price = defaultDecimal(price);
        this.quantity = quantity;
        this.category = "默认分类";
        this.barcode = "";
        this.unit = "个";
        this.description = "";
        this.brand = "";
        this.supplier = "";
        this.spec = "";
        this.minStock = 10;
        this.cost = this.price.multiply(DEFAULT_COST_RATE);  // 默认成本价为售价的70%
    }

    public Product(String name, double price, int quantity) {
        this(name, BigDecimal.valueOf(price), quantity);
    }

    public Product(String name, BigDecimal price, int quantity, String category) {
        this(name, price, quantity);
        this.category = category;
    }

    public Product(String name, double price, int quantity, String category) {
        this(name, BigDecimal.valueOf(price), quantity, category);
    }

    public Product(String name, BigDecimal price, int quantity, String category, String barcode, String unit, String description) {
        this(name, price, quantity, category);
        this.barcode = barcode;
        this.unit = unit;
        this.description = description;
        this.brand = "";
        this.supplier = "";
        this.spec = "";
        this.minStock = 10;
        this.cost = this.price.multiply(DEFAULT_COST_RATE);
    }

    public Product(String name, double price, int quantity, String category, String barcode, String unit, String description) {
        this(name, BigDecimal.valueOf(price), quantity, category, barcode, unit, description);
    }

    public Product(int id, String name, BigDecimal price, int quantity, String category, String barcode, String unit, String description, String brand, String supplier, String spec, int minStock, BigDecimal cost) {
        this();
        this.id = id;
        this.name = name;
        this.price = defaultDecimal(price);
        this.quantity = quantity;
        this.category = category;
        this.barcode = barcode;
        this.unit = unit;
        this.description = description;
        this.brand = brand;
        this.supplier = supplier;
        this.spec = spec;
        this.minStock = minStock;
        this.cost = defaultDecimal(cost);
    }

    public Product(int id, String name, double price, int quantity, String category, String barcode, String unit, String description, String brand, String supplier, String spec, int minStock, double cost) {
        this(id, name, BigDecimal.valueOf(price), quantity, category, barcode, unit, description, brand, supplier, spec, minStock, BigDecimal.valueOf(cost));
    }

    public Product(int id, String productCode, String name, BigDecimal price, int quantity, String category, String barcode, String unit, String description, String brand, String supplier, String spec, int minStock, BigDecimal cost) {
        this(id, name, price, quantity, category, barcode, unit, description, brand, supplier, spec, minStock, cost);
        this.productCode = productCode;
    }

    public Product(int id, String productCode, String name, double price, int quantity, String category, String barcode, String unit, String description, String brand, String supplier, String spec, int minStock, double cost) {
        this(id, productCode, name, BigDecimal.valueOf(price), quantity, category, barcode, unit, description, brand, supplier, spec, minStock, BigDecimal.valueOf(cost));
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // Getter方法
    public int getId() {
        return id;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return defaultDecimal(price);
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

    public BigDecimal getCost() {
        return defaultDecimal(cost);
    }
}