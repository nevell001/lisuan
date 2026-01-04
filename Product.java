public class Product {
    String name;
    double price;
    int quantity;
    String category;
    String barcode;        // 条形码
    String unit;           // 单位（个、kg、瓶等）
    String description;     // 商品描述
    String brand;          // 品牌
    String supplier;       // 供应商
    String spec;           // 规格
    int minStock;         // 最低库存预警
    double cost;          // 成本价

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
}