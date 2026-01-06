public class Category {
    String name;
    String description;

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Category(String name) {
        this.name = name;
        this.description = "";
    }
}