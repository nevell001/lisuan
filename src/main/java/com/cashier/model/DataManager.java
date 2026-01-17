package com.cashier.model;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataManager {
    private static final String DATA_DIR = "data";
    private static final String INVENTORY_FILE = DATA_DIR + "/inventory.txt";
    private static final String TRANSACTIONS_FILE = DATA_DIR + "/transactions.txt";
    private static final String SETTINGS_FILE = DATA_DIR + "/settings.txt";
    private static final String CATEGORIES_FILE = DATA_DIR + "/categories.txt";
    private static final String MEMBERS_FILE = DATA_DIR + "/members.txt";
    private static final String PROMOTIONS_FILE = DATA_DIR + "/promotions.txt";
    private static final String RECHARGE_FILE = DATA_DIR + "/recharge.txt";
    private static final String USERS_FILE = DATA_DIR + "/users.txt";
    private static final String OPERATION_LOGS_FILE = DATA_DIR + "/operation_logs.txt";
    private static final String SHIFTS_FILE = DATA_DIR + "/shifts.txt";

    public static void initialize() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void saveInventory(Map<String, Product> inventory) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(INVENTORY_FILE))) {
            for (Product product : inventory.values()) {
                writer.printf("%s|%.2f|%d|%s|%s|%s|%s|%s|%s|%s|%d|%.2f\n",
                    escape(product.name),
                    product.price,
                    product.quantity,
                    escape(product.category),
                    escape(product.barcode),
                    escape(product.unit),
                    escape(product.description),
                    escape(product.brand),
                    escape(product.supplier),
                    escape(product.spec),
                    product.minStock,
                    product.cost
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Product> loadInventory() {
        Map<String, Product> inventory = new HashMap<>();
        File file = new File(INVENTORY_FILE);
        if (!file.exists()) {
            return inventory;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(INVENTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");

                if (parts.length >= 12) {
                    // 新格式（包含所有字段）
                    String name = unescape(parts[0]);
                    double price = Double.parseDouble(parts[1]);
                    int quantity = Integer.parseInt(parts[2]);
                    String category = unescape(parts[3]);
                    String barcode = unescape(parts[4]);
                    String unit = unescape(parts[5]);
                    String description = unescape(parts[6]);
                    String brand = unescape(parts[7]);
                    String supplier = unescape(parts[8]);
                    String spec = unescape(parts[9]);
                    int minStock = Integer.parseInt(parts[10]);
                    double cost = Double.parseDouble(parts[11]);

                    Product product = new Product(name, price, quantity, category);
                    product.barcode = barcode;
                    product.unit = unit;
                    product.description = description;
                    product.brand = brand;
                    product.supplier = supplier;
                    product.spec = spec;
                    product.minStock = minStock;
                    product.cost = cost;

                    inventory.put(name, product);
                } else if (parts.length == 11) {
                    // 旧格式（缺少cost字段）
                    String name = unescape(parts[0]);
                    double price = Double.parseDouble(parts[1]);
                    int quantity = Integer.parseInt(parts[2]);
                    String category = unescape(parts[3]);
                    String barcode = unescape(parts[4]);
                    String unit = unescape(parts[5]);
                    String description = unescape(parts[6]);
                    String brand = unescape(parts[7]);
                    String supplier = unescape(parts[8]);
                    String spec = unescape(parts[9]);
                    int minStock = Integer.parseInt(parts[10]);

                    Product product = new Product(name, price, quantity, category);
                    product.barcode = barcode;
                    product.unit = unit;
                    product.description = description;
                    product.brand = brand;
                    product.supplier = supplier;
                    product.spec = spec;
                    product.minStock = minStock;
                    product.cost = price * 0.7; // 默认成本价为售价�?0%

                    inventory.put(name, product);
                } else if (parts.length == 4) {
                    // 中间格式（有分类�?
                    String name = unescape(parts[0]);
                    double price = Double.parseDouble(parts[1]);
                    int quantity = Integer.parseInt(parts[2]);
                    String category = unescape(parts[3]);
                    inventory.put(name, new Product(name, price, quantity, category));
                } else if (parts.length == 3) {
                    // 旧格式（没有分类�?
                    String name = unescape(parts[0]);
                    double price = Double.parseDouble(parts[1]);
                    int quantity = Integer.parseInt(parts[2]);
                    inventory.put(name, new Product(name, price, quantity));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return inventory;
    }

    public static void saveTransactions(List<Transaction> transactions) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(TRANSACTIONS_FILE))) {
            for (Transaction transaction : transactions) {
                writer.printf("%s|%s|%.2f|%.2f|%.2f|%s\n",
                    transaction.transactionId,
                    transaction.timestamp,
                    transaction.totalAmount,
                    transaction.tax,
                    transaction.finalAmount,
                    escape(transaction.paymentMethod)
                );

                for (Product product : transaction.items) {
                    writer.printf("ITEM:%s|%.2f|%d\n",
                        escape(product.name),
                        product.price,
                        product.quantity
                    );
                }
                writer.println("END_TRANSACTION");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        File file = new File(TRANSACTIONS_FILE);
        if (!file.exists()) {
            return transactions;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("END_TRANSACTION")) {
                    continue;
                }

                String[] parts = line.split("\\|", -1);
                if (parts.length == 5) {
                    // 旧格式（没有paymentMethod�?
                    String transactionId = parts[0];
                    String timestamp = parts[1];
                    double totalAmount = Double.parseDouble(parts[2]);
                    double tax = Double.parseDouble(parts[3]);
                    double finalAmount = Double.parseDouble(parts[4]);

                    List<Product> items = new ArrayList<>();
                    String itemLine;
                    while ((itemLine = reader.readLine()) != null && !itemLine.equals("END_TRANSACTION")) {
                        if (itemLine.startsWith("ITEM:")) {
                            String[] itemParts = itemLine.substring(5).split("\\|");
                            if (itemParts.length == 3) {
                                String name = unescape(itemParts[0]);
                                double price = Double.parseDouble(itemParts[1]);
                                int quantity = Integer.parseInt(itemParts[2]);
                                items.add(new Product(name, price, quantity));
                            }
                        }
                    }

                    Transaction transaction = new Transaction(
                        transactionId, timestamp, items, totalAmount, tax, finalAmount, ""
                    );
                    transactions.add(transaction);
                } else if (parts.length == 6) {
                    // 新格式（包含paymentMethod�?
                    String transactionId = parts[0];
                    String timestamp = parts[1];
                    double totalAmount = Double.parseDouble(parts[2]);
                    double tax = Double.parseDouble(parts[3]);
                    double finalAmount = Double.parseDouble(parts[4]);
                    String paymentMethod = unescape(parts[5]);

                    List<Product> items = new ArrayList<>();
                    String itemLine;
                    while ((itemLine = reader.readLine()) != null && !itemLine.equals("END_TRANSACTION")) {
                        if (itemLine.startsWith("ITEM:")) {
                            String[] itemParts = itemLine.substring(5).split("\\|");
                            if (itemParts.length == 3) {
                                String name = unescape(itemParts[0]);
                                double price = Double.parseDouble(itemParts[1]);
                                int quantity = Integer.parseInt(itemParts[2]);
                                items.add(new Product(name, price, quantity));
                            }
                        }
                    }

                    Transaction transaction = new Transaction(
                        transactionId, timestamp, items, totalAmount, tax, finalAmount, paymentMethod
                    );
                    transactions.add(transaction);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    public static void saveSettings(double taxRate, int transactionCount) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SETTINGS_FILE))) {
            writer.printf("taxRate=%.2f\n", taxRate);
            writer.printf("transactionCount=%d\n", transactionCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> loadSettings() {
        Map<String, String> settings = new HashMap<>();
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            settings.put("taxRate", "0.0");
            settings.put("transactionCount", "0");
            return settings;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    settings.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return settings;
    }

    public static void backupData(String backupPath) throws IOException {
        File backupDir = new File(backupPath);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    
        copyFile(INVENTORY_FILE, backupPath + "/inventory.txt");
        copyFile(TRANSACTIONS_FILE, backupPath + "/transactions.txt");
        copyFile(SETTINGS_FILE, backupPath + "/settings.txt");
        copyFile(CATEGORIES_FILE, backupPath + "/categories.txt");
        copyFile(MEMBERS_FILE, backupPath + "/members.txt");
        copyFile(PROMOTIONS_FILE, backupPath + "/promotions.txt");
        copyFile(RECHARGE_FILE, backupPath + "/recharge.txt");
        copyFile(USERS_FILE, backupPath + "/users.txt");
        copyFile(OPERATION_LOGS_FILE, backupPath + "/operation_logs.txt");
        copyFile(SHIFTS_FILE, backupPath + "/shifts.txt");
    }
    
    public static void restoreData(String backupPath) throws IOException {
        File backupDir = new File(backupPath);
        if (!backupDir.exists()) {
            throw new IOException("备份目录不存在: " + backupPath);
        }
        
        copyFile(backupPath + "/inventory.txt", INVENTORY_FILE);
        copyFile(backupPath + "/transactions.txt", TRANSACTIONS_FILE);
        copyFile(backupPath + "/settings.txt", SETTINGS_FILE);
        copyFile(backupPath + "/categories.txt", CATEGORIES_FILE);
        copyFile(backupPath + "/members.txt", MEMBERS_FILE);
        copyFile(backupPath + "/promotions.txt", PROMOTIONS_FILE);
        copyFile(backupPath + "/recharge.txt", RECHARGE_FILE);
        copyFile(backupPath + "/users.txt", USERS_FILE);
        copyFile(backupPath + "/operation_logs.txt", OPERATION_LOGS_FILE);
        copyFile(backupPath + "/shifts.txt", SHIFTS_FILE);
    }
    private static void copyFile(String source, String destination) throws IOException {
        File sourceFile = new File(source);
        if (!sourceFile.exists()) {
            throw new IOException("源文件不存在: " + source);
        }
        
        File destFile = new File(destination);
        File destDir = destFile.getParentFile();
        if (destDir != null && !destDir.exists()) {
            destDir.mkdirs();
        }
        
        try (
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(destination)
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "\\|").replace("\n", "\\n");
    }

    private static String unescape(String text) {
        return text.replace("\\n", "\n").replace("\\|", "|");
    }

    public static void saveCategories(List<Category> categories) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CATEGORIES_FILE))) {
            for (Category category : categories) {
                writer.printf("%s|%s\n",
                    escape(category.name),
                    escape(category.description)
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Category> loadCategories() {
        List<Category> categories = new ArrayList<>();
        File file = new File(CATEGORIES_FILE);
        if (!file.exists()) {
            // 默认分类
            categories.add(new Category("默认分类", "默认商品分类"));
            categories.add(new Category("食品", "食品类商品"));
            categories.add(new Category("饮料", "饮品类商品"));
            categories.add(new Category("日用品", "日用品类商品"));
            return categories;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(CATEGORIES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 1) {
                    String name = unescape(parts[0]);
                    String description = parts.length >= 2 ? unescape(parts[1]) : "";
                    categories.add(new Category(name, description));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return categories;
    }

    public static void saveMembers(Map<String, Member> members) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(MEMBERS_FILE))) {
            for (Member member : members.values()) {
                writer.printf("%s|%s|%.2f|%s|%.2f|%.2f|%s\n",
                    escape(member.phone),
                    escape(member.name),
                    member.points,
                    member.level,
                    member.discount,
                    member.balance,
                    escape(member.birthday)
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Member> loadMembers() {
        Map<String, Member> members = new HashMap<>();
        File file = new File(MEMBERS_FILE);
        if (!file.exists()) {
            return members;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(MEMBERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 7) {
                    // 新格式（包含余额和生日）
                    String phone = unescape(parts[0]);
                    String name = unescape(parts[1]);
                    double points = Double.parseDouble(parts[2]);
                    String level = parts[3];
                    double discount = Double.parseDouble(parts[4]);
                    double balance = Double.parseDouble(parts[5]);
                    String birthday = unescape(parts[6]);
                    
                    Member member = new Member(phone, name, points, level, discount, balance, birthday);
                    members.put(phone, member);
                } else if (parts.length == 6) {
                    // 中间格式（包含余额但没有生日�?
                    String phone = unescape(parts[0]);
                    String name = unescape(parts[1]);
                    double points = Double.parseDouble(parts[2]);
                    String level = parts[3];
                    double discount = Double.parseDouble(parts[4]);
                    double balance = Double.parseDouble(parts[5]);
                    members.put(phone, new Member(phone, name, points, level, discount, balance, ""));
                } else if (parts.length == 5) {
                    // 旧格式（不包含余额和生日�?
                    String phone = unescape(parts[0]);
                    String name = unescape(parts[1]);
                    double points = Double.parseDouble(parts[2]);
                    String level = parts[3];
                    double discount = Double.parseDouble(parts[4]);
                    members.put(phone, new Member(phone, name, points, level, discount));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return members;
    }

    public static void savePromotions(List<Promotion> promotions) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(PROMOTIONS_FILE))) {
            for (Promotion promotion : promotions) {
                writer.printf("%s|%s|%s|%.2f|%.2f|%s|%b|%d|%d|%d\n",
                    escape(promotion.id),
                    escape(promotion.name),
                    escape(promotion.type),
                    promotion.threshold,
                    promotion.discount,
                    escape(promotion.description),
                    promotion.enabled,
                    promotion.startDate.getTime(),
                    promotion.endDate.getTime(),
                    promotion.maxUsage
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Promotion> loadPromotions() {
        List<Promotion> promotions = new ArrayList<>();
        File file = new File(PROMOTIONS_FILE);
        if (!file.exists()) {
            return promotions;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(PROMOTIONS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length >= 10) {
                    Promotion promotion = new Promotion();
                    promotion.id = unescape(parts[0]);
                    promotion.name = unescape(parts[1]);
                    promotion.type = unescape(parts[2]);
                    promotion.threshold = Double.parseDouble(parts[3]);
                    promotion.discount = Double.parseDouble(parts[4]);
                    promotion.description = unescape(parts[5]);
                    promotion.enabled = Boolean.parseBoolean(parts[6]);
                    promotion.startDate = new Date(Long.parseLong(parts[7]));
                    promotion.endDate = new Date(Long.parseLong(parts[8]));
                    promotion.maxUsage = Integer.parseInt(parts[9]);
                    promotion.usageCount = 0; // 重新加载时使用次数重�?

                    promotions.add(promotion);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return promotions;
    }

    // 保存充值记�?
    public static void saveRechargeRecords(List<RechargeRecord> rechargeRecords) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(RECHARGE_FILE))) {
            for (RechargeRecord record : rechargeRecords) {
                writer.printf("%s|%s|%s|%.2f|%s|%d|%s\n",
                    escape(record.recordId),
                    escape(record.memberPhone),
                    escape(record.memberName),
                    record.amount,
                    escape(record.paymentMethod),
                    record.timestamp.getTime(),
                    escape(record.operator)
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载充值记�?
    public static List<RechargeRecord> loadRechargeRecords() {
        List<RechargeRecord> rechargeRecords = new ArrayList<>();
        File file = new File(RECHARGE_FILE);
        if (!file.exists()) {
            return rechargeRecords;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(RECHARGE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 7) {
                    String recordId = unescape(parts[0]);
                    String memberPhone = unescape(parts[1]);
                    String memberName = unescape(parts[2]);
                    double amount = Double.parseDouble(parts[3]);
                    String paymentMethod = unescape(parts[4]);
                    Date timestamp = new Date(Long.parseLong(parts[5]));
                    String operator = unescape(parts[6]);
                    
                    RechargeRecord record = new RechargeRecord(recordId, memberPhone, memberName, amount, paymentMethod, operator);
                    record.timestamp = timestamp;
                    rechargeRecords.add(record);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rechargeRecords;
    }

    // 保存用户数据
    public static void saveUsers(Map<String, User> users) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (User user : users.values()) {
                writer.printf("%s|%s|%s|%s|%d|%d|%d\n",
                    escape(user.username),
                    escape(user.password),
                    escape(user.name),
                    escape(user.role),
                    user.createTime.getTime(),
                    user.lastLoginTime.getTime(),
                    user.active ? 1 : 0
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载用户数据
    public static Map<String, User> loadUsers() {
        Map<String, User> users = new HashMap<>();
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            // 如果用户文件不存在，创建默认管理员账户
            User admin = new User("admin", "admin123", "系统管理员", "admin");
            users.put(admin.username, admin);
            saveUsers(users);
            return users;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 7) {
                    String username = unescape(parts[0]);
                    String password = unescape(parts[1]);
                    String name = unescape(parts[2]);
                    String role = unescape(parts[3]);
                    Date createTime = new Date(Long.parseLong(parts[4]));
                    Date lastLoginTime = new Date(Long.parseLong(parts[5]));
                    boolean active = Integer.parseInt(parts[6]) == 1;

                    User user = new User(username, password, name, role);
                    user.createTime = createTime;
                    user.lastLoginTime = lastLoginTime;
                    user.active = active;
                    users.put(username, user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return users;
    }

    // 保存操作日志
    public static void saveOperationLogs(List<OperationLog> logs) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(OPERATION_LOGS_FILE))) {
            for (OperationLog log : logs) {
                writer.printf("%s|%s|%s|%s|%d|%s\n",
                    escape(log.logId),
                    escape(log.username),
                    escape(log.operation),
                    escape(log.details),
                    log.timestamp.getTime(),
                    escape(log.ipAddress)
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载操作日志
    public static List<OperationLog> loadOperationLogs() {
        List<OperationLog> logs = new ArrayList<>();
        File file = new File(OPERATION_LOGS_FILE);
        if (!file.exists()) {
            return logs;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(OPERATION_LOGS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 6) {
                    String logId = unescape(parts[0]);
                    String username = unescape(parts[1]);
                    String operation = unescape(parts[2]);
                    String details = unescape(parts[3]);
                    Date timestamp = new Date(Long.parseLong(parts[4]));
                    String ipAddress = unescape(parts[5]);

                    OperationLog log = new OperationLog(logId, username, operation, details);
                    log.timestamp = timestamp;
                    log.ipAddress = ipAddress;
                    logs.add(log);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return logs;
    }

    // 保存交接班记�?
    public static void saveShifts(List<Shift> shifts) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SHIFTS_FILE))) {
            for (Shift shift : shifts) {
                writer.printf("%s|%s|%s|%d|%d|%.2f|%d|%.2f|%d|%.2f|%d|%s|%.2f|%.2f|%.2f|%.2f\n",
                    escape(shift.shiftId),
                    escape(shift.username),
                    escape(shift.operatorName),
                    shift.startTime.getTime(),
                    shift.endTime.getTime(),
                    shift.openingRevenue,
                    shift.openingTransactionCount,
                    shift.closingRevenue,
                    shift.closingTransactionCount,
                    shift.shiftRevenue,
                    shift.shiftTransactionCount,
                    escape(shift.notes),
                    shift.cashRevenue,
                    shift.wechatRevenue,
                    shift.alipayRevenue,
                    shift.cardRevenue
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载交接班记�?
    public static List<Shift> loadShifts() {
        List<Shift> shifts = new ArrayList<>();
        File file = new File(SHIFTS_FILE);
        if (!file.exists()) {
            return shifts;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(SHIFTS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", -1);  // 使用-1保留末尾的空字符�?
                if (parts.length == 12) {
                    // 旧格式（没有支付方式收入�?
                    String shiftId = unescape(parts[0]);
                    String username = unescape(parts[1]);
                    String operatorName = unescape(parts[2]);
                    Date startTime = new Date(Long.parseLong(parts[3]));
                    Date endTime = new Date(Long.parseLong(parts[4]));
                    double openingRevenue = Double.parseDouble(parts[5]);
                    int openingTransactionCount = Integer.parseInt(parts[6]);
                    double closingRevenue = Double.parseDouble(parts[7]);
                    int closingTransactionCount = Integer.parseInt(parts[8]);
                    double shiftRevenue = Double.parseDouble(parts[9]);
                    int shiftTransactionCount = Integer.parseInt(parts[10]);
                    String notes = unescape(parts[11]);

                    Shift shift = new Shift(shiftId, username, operatorName, startTime, openingRevenue, openingTransactionCount);
                    shift.endTime = endTime;
                    shift.closingRevenue = closingRevenue;
                    shift.closingTransactionCount = closingTransactionCount;
                    shift.shiftRevenue = shiftRevenue;
                    shift.shiftTransactionCount = shiftTransactionCount;
                    shift.notes = notes;
                    shifts.add(shift);
                } else if (parts.length == 16) {
                    // 新格式（包含支付方式收入�?
                    String shiftId = unescape(parts[0]);
                    String username = unescape(parts[1]);
                    String operatorName = unescape(parts[2]);
                    Date startTime = new Date(Long.parseLong(parts[3]));
                    Date endTime = new Date(Long.parseLong(parts[4]));
                    double openingRevenue = Double.parseDouble(parts[5]);
                    int openingTransactionCount = Integer.parseInt(parts[6]);
                    double closingRevenue = Double.parseDouble(parts[7]);
                    int closingTransactionCount = Integer.parseInt(parts[8]);
                    double shiftRevenue = Double.parseDouble(parts[9]);
                    int shiftTransactionCount = Integer.parseInt(parts[10]);
                    String notes = unescape(parts[11]);
                    double cashRevenue = Double.parseDouble(parts[12]);
                    double wechatRevenue = Double.parseDouble(parts[13]);
                    double alipayRevenue = Double.parseDouble(parts[14]);
                    double cardRevenue = Double.parseDouble(parts[15]);

                    Shift shift = new Shift(shiftId, username, operatorName, startTime, openingRevenue, openingTransactionCount);
                    shift.endTime = endTime;
                    shift.closingRevenue = closingRevenue;
                    shift.closingTransactionCount = closingTransactionCount;
                    shift.shiftRevenue = shiftRevenue;
                    shift.shiftTransactionCount = shiftTransactionCount;
                    shift.notes = notes;
                    shift.cashRevenue = cashRevenue;
                    shift.wechatRevenue = wechatRevenue;
                    shift.alipayRevenue = alipayRevenue;
                    shift.cardRevenue = cardRevenue;
                    shifts.add(shift);
                } else {
                    System.out.println("警告: 班次记录格式错误，字段数=" + parts.length + ", 期望=12�?6, 行内�? " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return shifts;
    }

    /**
     * 获取当前活跃的班次
     * @return 活跃的班次，如果没有则返回null
     */
    public static Shift getActiveShift() {
        List<Shift> shifts = loadShifts();
        for (Shift shift : shifts) {
            // 如果endTime等于startTime，说明班次未结束，是活跃班次
            if (shift.endTime.getTime() == shift.startTime.getTime()) {
                return shift;
            }
        }
        return null;
    }

    /**
     * 检查是否有活跃的班次
     * @return 如果有活跃班次返回true，否则返回false
     */
    public static boolean hasActiveShift() {
        return getActiveShift() != null;
    }

    /**
     * 保存主题偏好设置
     * @param themeName 主题名称
     */
    public static void saveThemePreference(String themeName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SETTINGS_FILE, true))) {
            writer.println("theme=" + themeName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载主题偏好设置
     * @return 主题名称，默认为 light
     */
    public static String loadThemePreference() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            return "light";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("theme=")) {
                    return line.substring("theme=".length());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "light";
    }
}
