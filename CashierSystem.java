import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class CashierSystem {
    private static List<Product> cart = new ArrayList<>();
    private static Map<String, Product> inventory = new HashMap<>();
    private static List<Transaction> transactions = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);
    private static double taxRate = 0.0;
    private static int transactionCount = 0;

    // ANSI颜色代码
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_MAGENTA = "\u001B[95m";
    private static final String BRIGHT_CYAN = "\u001B[96m";

    public static void main(String[] args) {
        System.out.println("\n" + CYAN + "╔══════════════════════════════════════════════════╗");
        System.out.println("║                                                  ║");
        System.out.println("║            " + BRIGHT_YELLOW + "🏪 欢迎使用收银系统 🏪" + CYAN + "               ║");
        System.out.println("║                                                  ║");
        System.out.println("╚══════════════════════════════════════════════════╝" + RESET);

        while (true) {
            System.out.println("\n" + CYAN + "╔══════════════════════════════════════╗");
            System.out.println("║            " + BRIGHT_YELLOW + "主菜单 - 请选择操作" + CYAN + "          ║");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  1. " + GREEN + "🛒 添加商品到购物车" + CYAN + "                ║");
            System.out.println("║  2. " + BLUE + "👀 查看购物车" + CYAN + "                       ║");
            System.out.println("║  3. " + YELLOW + "💰 结账" + CYAN + "                            ║");
            System.out.println("║  4. " + MAGENTA + "📦 查看库存" + CYAN + "                         ║");
            System.out.println("║  5. " + GREEN + "➕ 添加库存商品" + CYAN + "                      ║");
            System.out.println("║  6. " + BLUE + "🔄 补货" + CYAN + "                            ║");
            System.out.println("║  7. " + BRIGHT_CYAN + "🔍 搜索商品" + CYAN + "                         ║");
            System.out.println("║  8. " + BRIGHT_MAGENTA + "✏️ 编辑购物车" + CYAN + "                      ║");
            System.out.println("║  9. " + BRIGHT_YELLOW + "📋 查看交易记录" + CYAN + "                    ║");
            System.out.println("║ 10. " + BRIGHT_YELLOW + "📊 库存排序" + CYAN + "                        ║");
            System.out.println("║ 11. " + BRIGHT_YELLOW + "⚙️ 设置税率" + CYAN + "                        ║");
            System.out.println("║ 12. " + RED + "🚪 退出系统" + CYAN + "                         ║");
            System.out.println("╚══════════════════════════════════════╝" + RESET);
            System.out.print("\n请输入选项 (1-12): ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    addProductToCart();
                    break;
                case 2:
                    viewCart();
                    break;
                case 3:
                    checkout();
                    break;
                case 4:
                    viewInventory();
                    break;
                case 5:
                    addInventoryProduct();
                    break;
                case 6:
                    restockProduct();
                    break;
                case 7:
                    searchProduct();
                    break;
                case 8:
                    editCart();
                    break;
                case 9:
                    viewTransactions();
                    break;
                case 10:
                    sortInventory();
                    break;
                case 11:
                    setTaxRate();
                    break;
                case 12:
                    System.out.println("\n" + CYAN + "╔══════════════════════════════════════════════════╗");
                    System.out.println("║                                                  ║");
                    System.out.println("║              " + BRIGHT_YELLOW + "👋 感谢使用，再见！ 👋" + CYAN + "              ║");
                    System.out.println("║                                                  ║");
                    System.out.println("╚══════════════════════════════════════════════════╝" + RESET);
                    scanner.close();
                    return;
                default:
                    System.out.println(RED + "❌ 无效选项，请输入 1-12 之间的数字！" + RESET);
            }
        }
    }

    private static void addInventoryProduct() {
        System.out.println("\n" + MAGENTA + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📦 添加新商品到库存");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);

        System.out.print("➤ 请输入商品名称: ");
        String name = scanner.nextLine();

        if (name.trim().isEmpty()) {
            System.out.println(RED + "❌ 商品名称不能为空！" + RESET);
            return;
        }

        if (inventory.containsKey(name)) {
            System.out.println(YELLOW + "⚠️  该商品已存在于库存中，请使用补货功能！" + RESET);
            return;
        }

        double price = readPositiveDouble("➤ 请输入商品价格 (元): ");
        int quantity = readPositiveInt("➤ 请输入库存数量: ");

        Product product = new Product(name, price, quantity);
        inventory.put(name, product);
        System.out.println(GREEN + "✅ 库存商品添加成功！" + RESET);
    }

    private static void restockProduct() {
        if (inventory.isEmpty()) {
            System.out.println(YELLOW + "⚠️  库存为空，请先添加库存商品！" + RESET);
            return;
        }

        System.out.println("\n" + BLUE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔄 商品补货");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);

        System.out.print("➤ 请输入要补货的商品名称: ");
        String name = scanner.nextLine();

        if (!inventory.containsKey(name)) {
            System.out.println(RED + "❌ 该商品不存在于库存中！" + RESET);
            return;
        }

        System.out.printf("📊 %s 当前库存: %d\n", name, inventory.get(name).quantity);
        int addQuantity = readPositiveInt("➤ 请输入补货数量: ");

        Product product = inventory.get(name);
        product.quantity += addQuantity;
        System.out.println(GREEN + "✅ 补货成功！" + RESET + " " + name + " 当前库存: " + BRIGHT_GREEN + product.quantity + RESET);
    }

    private static void viewInventory() {
        if (inventory.isEmpty()) {
            System.out.println(YELLOW + "⚠️  库存为空！" + RESET);
            return;
        }

        System.out.println("\n" + MAGENTA + "╔══════════════════════════════════════════════════╗");
        System.out.println("║                 📦 库存清单                        ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║ %-16s %-10s %-10s %-10s ║\n", "商品名称", "单价(元)", "库存", "状态");
        System.out.println("╠══════════════════════════════════════════════════╣");

        int totalItems = 0;
        double totalValue = 0;
        int inStockCount = 0;

        for (Product product : inventory.values()) {
            String status = product.quantity > 0 ? GREEN + "✅ 有货" + RESET : RED + "❌ 缺货" + RESET;
            System.out.printf("║ %-16s %-10.2f %-10d %-10s ║\n",
                product.name, product.price, product.quantity, status);
            totalItems += product.quantity;
            totalValue += product.price * product.quantity;
            if (product.quantity > 0) inStockCount++;
        }

        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║ 📊 商品种类: %d  |  总库存: %d  |  总价值: %.2f 元 ║\n",
            inventory.size(), totalItems, totalValue);
        System.out.println("╚══════════════════════════════════════════════════╝" + RESET);
    }

    private static void addProductToCart() {
        if (inventory.isEmpty()) {
            System.out.println(YELLOW + "⚠️  库存为空，请先添加库存商品！" + RESET);
            return;
        }

        System.out.println("\n" + GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🛒 添加商品到购物车");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);

        System.out.print("➤ 请输入商品名称: ");
        String name = scanner.nextLine();

        if (!inventory.containsKey(name)) {
            System.out.println(RED + "❌ 该商品不存在于库存中！" + RESET);
            return;
        }

        Product inventoryProduct = inventory.get(name);

        if (inventoryProduct.quantity <= 0) {
            System.out.println(RED + "❌ 该商品库存不足！" + RESET);
            return;
        }

        System.out.println("\n📊 " + CYAN + "商品信息: " + RESET + name);
        System.out.println("💰 单价: " + YELLOW + String.format("%.2f", inventoryProduct.price) + " 元" + RESET);
        System.out.println("📦 当前库存: " + BRIGHT_GREEN + inventoryProduct.quantity + RESET);
        int quantity = readPositiveInt("➤ 请输入购买数量: ");

        if (quantity > inventoryProduct.quantity) {
            System.out.printf(RED + "❌ 库存不足！当前库存: %d" + RESET + "\n", inventoryProduct.quantity);
            return;
        }

        Product cartProduct = new Product(name, inventoryProduct.price, quantity);
        cart.add(cartProduct);
        System.out.println(GREEN + "✅ 成功添加 " + quantity + " 个 " + name + " 到购物车！" + RESET);
    }

    private static void viewCart() {
        if (cart.isEmpty()) {
            System.out.println(YELLOW + "⚠️  购物车为空！" + RESET);
            return;
        }

        System.out.println("\n" + BLUE + "╔══════════════════════════════════════════════════╗");
        System.out.println("║                 🛒 购物车                         ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║ %-16s %-10s %-10s %-10s ║\n", "商品名称", "单价(元)", "数量", "小计(元)");
        System.out.println("╠══════════════════════════════════════════════════╣");

        double total = 0;
        for (Product product : cart) {
            double subtotal = product.price * product.quantity;
            total += subtotal;
            System.out.printf("║ %-16s %-10.2f %-10d %-10.2f ║\n",
                product.name, product.price, product.quantity, subtotal);
        }

        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║ 💰 总计: %36.2f 元 ║\n", total);
        System.out.println("╚══════════════════════════════════════════════════╝" + RESET);
    }

    private static void checkout() {
        if (cart.isEmpty()) {
            System.out.println(YELLOW + "⚠️  购物车为空，无法结账！" + RESET);
            return;
        }

        double subtotal = 0;
        System.out.println("\n" + YELLOW + "╔══════════════════════════════════════════════════╗");
        System.out.println("║                 💳 结账清单                        ║");
        System.out.println("╠══════════════════════════════════════════════════╣");

        for (Product product : cart) {
            double itemTotal = product.price * product.quantity;
            subtotal += itemTotal;
            System.out.printf("║ %-16s x %-2d = %26.2f 元 ║\n",
                product.name, product.quantity, itemTotal);
        }

        double tax = subtotal * taxRate;
        double total = subtotal + tax;

        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║ 💰 小计金额: %32.2f 元 ║\n", subtotal);
        if (taxRate > 0) {
            System.out.printf("║ 📋 税费 (%.1f%%): %27.2f 元 ║\n", taxRate * 100, tax);
        }
        System.out.printf("║ 💳 应付金额: %32.2f 元 ║\n", total);
        System.out.println("╚══════════════════════════════════════════════════╝" + RESET);

        double totalReceived = 0;
        System.out.println("\n" + GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("💵 开始收款");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);

        while (totalReceived < total) {
            double remaining = total - totalReceived;
            System.out.printf("\n📊 还需收款: " + BRIGHT_RED + "%.2f 元" + RESET + "\n", remaining);
            double received = readPositiveDouble("➤ 请输入收款金额: ");

            totalReceived += received;
            System.out.println(GREEN + "✅ 已收款: " + BRIGHT_GREEN + String.format("%.2f", totalReceived) + " 元" + RESET);
        }

        double change = totalReceived - total;

        System.out.println("\n" + BRIGHT_GREEN + "╔══════════════════════════════════════════════════╗");
        System.out.println("║             ✨ 交易成功 ✨                        ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║ 💰 应付金额: %32.2f 元 ║\n", total);
        System.out.printf("║ 💵 实收金额: %32.2f 元 ║\n", totalReceived);
        System.out.printf("║ 💸 找零金额: %32.2f 元 ║\n", change);
        System.out.println("╚══════════════════════════════════════════════════╝" + RESET);

        for (Product cartProduct : cart) {
            Product inventoryProduct = inventory.get(cartProduct.name);
            if (inventoryProduct != null) {
                inventoryProduct.quantity -= cartProduct.quantity;
            }
        }

        transactionCount++;
        String transactionId = String.format("T%06d", transactionCount);
        String timestamp = java.time.LocalDateTime.now().toString();

        List<Product> itemsCopy = new ArrayList<>();
        for (Product p : cart) {
            itemsCopy.add(new Product(p.name, p.price, p.quantity));
        }

        Transaction transaction = new Transaction(transactionId, timestamp, itemsCopy, subtotal, tax, total);
        transactions.add(transaction);

        cart.clear();
        System.out.println("\n" + GREEN + "✅ 购物车已清空" + RESET);
        System.out.println(GREEN + "✅ 库存已更新" + RESET);
        System.out.println(GREEN + "✅ 交易已记录 (ID: " + transactionId + ")" + RESET);
        System.out.println("\n" + BRIGHT_YELLOW + "感谢您的惠顾，欢迎下次光临！🎉" + RESET);
    }

    private static void searchProduct() {
        if (inventory.isEmpty()) {
            System.out.println(YELLOW + "⚠️  库存为空，无法搜索！" + RESET);
            return;
        }

        System.out.println("\n" + BRIGHT_CYAN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔍 搜索商品");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);

        System.out.print("➤ 请输入搜索关键词: ");
        String keyword = scanner.nextLine().toLowerCase();

        boolean found = false;
        System.out.println("\n" + BRIGHT_CYAN + "╔══════════════════════════════════════════════════╗");
        System.out.println("║                 🔍 搜索结果                        ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║ %-16s %-10s %-10s %-10s ║\n", "商品名称", "单价(元)", "库存", "状态");
        System.out.println("╠══════════════════════════════════════════════════╣");

        for (Product product : inventory.values()) {
            if (product.name.toLowerCase().contains(keyword)) {
                String status = product.quantity > 0 ? GREEN + "✅ 有货" + RESET : RED + "❌ 缺货" + RESET;
                System.out.printf("║ %-16s %-10.2f %-10d %-10s ║\n",
                    product.name, product.price, product.quantity, status);
                found = true;
            }
        }

        System.out.println("╚══════════════════════════════════════════════════╝" + RESET);

        if (!found) {
            System.out.println(YELLOW + "⚠️  未找到匹配的商品！" + RESET);
        }
    }

    private static double readPositiveDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                double value = scanner.nextDouble();
                scanner.nextLine();

                if (value <= 0) {
                    System.out.println(RED + "❌ 请输入大于0的数值！" + RESET);
                    continue;
                }
                return value;
            } catch (Exception e) {
                System.out.println(RED + "❌ 输入格式错误，请输入有效的数字！" + RESET);
                scanner.nextLine();
            }
        }
    }

    private static int readPositiveInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = scanner.nextInt();
                scanner.nextLine();

                if (value <= 0) {
                    System.out.println(RED + "❌ 请输入大于0的整数！" + RESET);
                    continue;
                }
                return value;
            } catch (Exception e) {
                System.out.println(RED + "❌ 输入格式错误，请输入有效的整数！" + RESET);
                scanner.nextLine();
            }
        }
    }

    private static void editCart() {
        if (cart.isEmpty()) {
            System.out.println(YELLOW + "⚠️  购物车为空，无法编辑！" + RESET);
            return;
        }

        System.out.println("\n" + BRIGHT_MAGENTA + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("✏️ 编辑购物车");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);

        System.out.println("\n" + BLUE + "╔══════════════════════════════════════════════════╗");
        System.out.println("║                 🛒 购物车                         ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║ %-3s %-16s %-10s %-10s %-10s ║\n", "序号", "商品名称", "单价(元)", "数量", "小计(元)");
        System.out.println("╠══════════════════════════════════════════════════╣");

        for (int i = 0; i < cart.size(); i++) {
            Product product = cart.get(i);
            double subtotal = product.price * product.quantity;
            System.out.printf("║ %-3d %-16s %-10.2f %-10d %-10.2f ║\n",
                i + 1, product.name, product.price, product.quantity, subtotal);
        }

        System.out.println("╚══════════════════════════════════════════════════╝" + RESET);

        System.out.println("\n请选择操作:");
        System.out.println("1. 修改商品数量");
        System.out.println("2. 删除商品");
        System.out.println("3. 返回");
        System.out.print("请输入选项 (1-3): ");

        int choice = readPositiveInt("请输入选项 (1-3): ");
        if (choice < 1 || choice > 3) {
            System.out.println(RED + "❌ 无效选项！" + RESET);
            return;
        }

        if (choice == 3) {
            return;
        }

        System.out.print("➤ 请输入商品序号: ");
        int index = scanner.nextInt();
        scanner.nextLine();

        if (index < 1 || index > cart.size()) {
            System.out.println(RED + "❌ 无效的序号！" + RESET);
            return;
        }

        Product product = cart.get(index - 1);

        if (choice == 1) {
            System.out.printf("当前数量: %d\n", product.quantity);
            System.out.print("➤ 请输入新数量: ");
            int newQuantity = scanner.nextInt();
            scanner.nextLine();

            if (newQuantity <= 0) {
                System.out.println(RED + "❌ 数量必须大于0！" + RESET);
                return;
            }

            Product inventoryProduct = inventory.get(product.name);
            if (inventoryProduct != null && newQuantity > inventoryProduct.quantity) {
                System.out.printf(RED + "❌ 库存不足！当前库存: %d" + RESET + "\n", inventoryProduct.quantity);
                return;
            }

            product.quantity = newQuantity;
            System.out.println(GREEN + "✅ 数量修改成功！" + RESET);
        } else if (choice == 2) {
            cart.remove(index - 1);
            System.out.println(GREEN + "✅ 商品已删除！" + RESET);
        }
    }

    private static void viewTransactions() {
        if (transactions.isEmpty()) {
            System.out.println(YELLOW + "⚠️  暂无交易记录！" + RESET);
            return;
        }

        System.out.println("\n" + BRIGHT_YELLOW + "╔══════════════════════════════════════════════════╗");
        System.out.println("║                 📋 交易记录                        ║");
        System.out.println("╠══════════════════════════════════════════════════╣");

        double totalRevenue = 0;
        for (Transaction transaction : transactions) {
            System.out.println("║ " + CYAN + "交易ID: " + RESET + transaction.transactionId);
            System.out.println("║ " + CYAN + "时间: " + RESET + transaction.timestamp);
            System.out.println("║ " + CYAN + "商品清单:" + RESET);
            for (Product product : transaction.items) {
                System.out.printf("║   - %-16s x %d = %.2f 元\n",
                    product.name, product.quantity, product.price * product.quantity);
            }
            System.out.println("║ " + CYAN + "小计: " + RESET + String.format("%.2f 元", transaction.totalAmount));
            if (transaction.tax > 0) {
                System.out.println("║ " + CYAN + "税费: " + RESET + String.format("%.2f 元", transaction.tax));
            }
            System.out.println("║ " + CYAN + "总计: " + RESET + String.format("%.2f 元", transaction.finalAmount));
            System.out.println("╠══════════════════════════════════════════════════╣");
            totalRevenue += transaction.finalAmount;
        }

        System.out.printf("║ 💰 总销售额: %34.2f 元 ║\n", totalRevenue);
        System.out.println("╚══════════════════════════════════════════════════╝" + RESET);
    }

    private static void sortInventory() {
        if (inventory.isEmpty()) {
            System.out.println(YELLOW + "⚠️  库存为空，无法排序！" + RESET);
            return;
        }

        System.out.println("\n" + BRIGHT_YELLOW + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📊 库存排序");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);

        System.out.println("\n请选择排序方式:");
        System.out.println("1. 按商品名称排序");
        System.out.println("2. 按价格排序（从低到高）");
        System.out.println("3. 按价格排序（从高到低）");
        System.out.println("4. 按库存数量排序（从多到少）");
        System.out.println("5. 返回");
        System.out.print("请输入选项 (1-5): ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice < 1 || choice > 5) {
            System.out.println(RED + "❌ 无效选项！" + RESET);
            return;
        }

        if (choice == 5) {
            return;
        }

        List<Product> sortedProducts = new ArrayList<>(inventory.values());

        switch (choice) {
            case 1:
                sortedProducts.sort((a, b) -> a.name.compareTo(b.name));
                break;
            case 2:
                sortedProducts.sort((a, b) -> Double.compare(a.price, b.price));
                break;
            case 3:
                sortedProducts.sort((a, b) -> Double.compare(b.price, a.price));
                break;
            case 4:
                sortedProducts.sort((a, b) -> Integer.compare(b.quantity, a.quantity));
                break;
        }

        System.out.println("\n" + BRIGHT_YELLOW + "╔══════════════════════════════════════════════════╗");
        System.out.println("║                 📦 排序结果                        ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║ %-16s %-10s %-10s %-10s ║\n", "商品名称", "单价(元)", "库存", "状态");
        System.out.println("╠══════════════════════════════════════════════════╣");

        for (Product product : sortedProducts) {
            String status = product.quantity > 0 ? GREEN + "✅ 有货" + RESET : RED + "❌ 缺货" + RESET;
            System.out.printf("║ %-16s %-10.2f %-10d %-10s ║\n",
                product.name, product.price, product.quantity, status);
        }

        System.out.println("╚══════════════════════════════════════════════════╝" + RESET);
    }

    private static void setTaxRate() {
        System.out.println("\n" + BRIGHT_YELLOW + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("⚙️ 设置税率");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);

        if (taxRate > 0) {
            System.out.printf("当前税率: " + YELLOW + "%.1f%%" + RESET + "\n", taxRate * 100);
        } else {
            System.out.println("当前税率: 0%");
        }

        System.out.print("➤ 请输入新税率 (百分比，例如输入 6 表示6%): ");
        double newRate = scanner.nextDouble();
        scanner.nextLine();

        if (newRate < 0) {
            System.out.println(RED + "❌ 税率不能为负数！" + RESET);
            return;
        }

        taxRate = newRate / 100.0;
        System.out.println(GREEN + "✅ 税率设置成功！当前税率: " + String.format("%.1f%%", taxRate * 100) + RESET);
    }
}