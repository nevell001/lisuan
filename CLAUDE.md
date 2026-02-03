# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A modern POS (Point of Sale) system built with JavaFX 17, using MySQL for data persistence. The system supports cashier operations, inventory management, member management, transaction tracking, shift management, and comprehensive reporting.

**Key Technologies**: Java 17, JavaFX 17.0.8, MySQL 8.0, Maven, HikariCP connection pooling

## Development Commands

### Build and Run
```bash
# Compile the project
mvn compile

# Run the application (requires MySQL database or will use file storage as fallback)
mvn javafx:run

# Clean build
mvn clean compile

# Package as JAR
mvn clean package

# Run packaged JAR
java -jar target/cashier-system-fx-2.0.0.jar
```

### Database Setup (Local Development)

**Option 1: Using Docker (Recommended)**
```bash
# Start MySQL 8.0 with phpMyAdmin
docker-compose up -d

# Stop containers
docker-compose down

# View logs
docker-compose logs mysql

# Access phpMyAdmin at http://localhost:8080
# Username: root, Password: RootPassword123!
```

**Option 2: Using Colima (macOS)**
```bash
# Start Colima with network address for port forwarding
colima start --network-address --network-host-addresses

# Then run docker-compose up -d
```

**Option 3: Native MySQL**
- Install MySQL 8.0 locally
- Create database and user: See `docs/MYSQL_SETUP.md`
- Configure connection in `config/database.properties`

### Database Migration
```bash
# Automatic migration on first run
mvn javafx:run

# Manual migration (if needed)
java -cp target/classes com.cashier.util.DataMigrationTool
```

## Architecture

### Data Layer Architecture (Dual Storage with Graceful Degradation)

The system uses a **dual storage architecture** with MySQL as primary and file-based storage as fallback:

1. **Primary Storage**: MySQL database via DAO layer
2. **Fallback Storage**: File-based storage via DataManager (`.txt` files in `data/` directory)

All controllers implement try-catch blocks to automatically fall back to file storage if database operations fail.

### DAO Layer Pattern

**DAO Classes** (`src/main/java/com/cashier/dao/`):
- `UserDAO` - User authentication and management
- `ProductDAO` - Product/Inventory CRUD operations
- `MemberDAO` - Member management, balance, points updates
- `TransactionDAO` - Transaction records and details
- `ShiftDAO` - Shift management (start/end shifts)

**DAO Pattern**:
- All DAOs are stateless with static methods
- Use `DatabaseManager.getConnection()` for HikariCP connections
- Methods throw `SQLException` - controllers handle fallback
- Batch operations use `PreparedStatement.addBatch()`
- Time fields stored as `BIGINT` (millis since epoch), NOT as TIMESTAMP

### Controller Layer

**Controllers** (`src/main/java/com/cashier/controller/`):
- `LoginController` - User authentication (UserDAO)
- `CartController` - Shopping cart and checkout (ProductDAO, MemberDAO, TransactionDAO)
- `InventoryController` - Product management (ProductDAO)
- `MemberController` - Member management (MemberDAO)
- `TransactionController` - Transaction history (TransactionDAO)
- `ShiftController` - Shift management (ShiftDAO)
- `StatisticsController` - Data statistics (TransactionDAO)
- `MainController` - Main application shell
- Plus dialog controllers for edit forms

**Important**: When modifying controllers to use database:
- Wrap DAO calls in try-catch blocks
- Fall back to DataManager methods on SQLException
- Log errors with `System.err.println()`

### Database Schema

**Tables** (created automatically by DatabaseManager):
- `users` - User accounts (username, password, name, role, active, create_time, last_login_time)
- `products` - Product inventory (name PK, price, quantity, category, barcode, unit, description, brand, supplier, spec, min_stock, cost, created_at, updated_at)
- `members` - Member accounts (phone PK, name, balance, points, level, discount, join_date, birthday)
- `transactions` - Transaction headers (transaction_id PK, timestamp, total_amount, tax, final_amount, payment_method, operator_username, operator_name, member_phone)
- `transaction_items` - Transaction line items (transaction_id FK, product_name, price, quantity, subtotal)
- `shifts` - Shift records (shift_id PK, operator_username, operator_name, start_time, end_time, opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, alipay_revenue, card_revenue, notes)

**Important Schema Notes**:
- Time fields are `BIGINT` storing milliseconds since epoch
- Foreign keys use `ON DELETE SET NULL` for optional relationships
- Member phone in transactions can be NULL (non-member transactions)
- Empty strings for `member_phone` must be converted to NULL before database insert

### Model Layer

**Model Classes** (`src/main/java/com/cashier/model/`):
- `User` - User account with `createTime` and `lastLoginTime` as `java.util.Date`
- `Product` - Product with all inventory fields
- `Member` - Member with points, balance, level, discount
- `Transaction` - Transaction with List<Product> items
- `Shift` - Shift record with revenue statistics
- `DataManager` - Legacy file-based storage (fallback)

### Utilities

**Key Utilities** (`src/main/java/com/cashier/util/`):
- `DatabaseManager` - MySQL connection pooling via HikariCP, auto-creates tables
- `PasswordUtil` - Password hashing (BCrypt) and verification
- `FXUtils` - JavaFX helper methods
- `StatusBarManager` - Global status bar updates
- `DataMigrationTool` - Migrates data from files to MySQL

### Configuration Files

- `config/database.properties` - MySQL connection configuration
  - Required: `db.url`, `db.username`, `db.password`
  - Optional: `db.pool.size` (default: 10)
  - Example in `config/database.properties.example`

- `pom.xml` - Maven build configuration
  - JavaFX 17.0.8
  - MySQL Connector J 8.0.33
  - HikariCP 5.1.0
  - ControlsFX 11.2.1
  - JUnit 5 + TestFX for testing

## Common Patterns

### Adding Database Support to a New Feature

1. Create DAO class in `src/main/java/com/cashier/dao/`:
```java
public class NewFeatureDAO {
    public static List<Feature> findAll() throws SQLException { ... }
    public static boolean insert(Feature feature) throws SQLException { ... }
    public static boolean update(Feature feature) throws SQLException { ... }
    public static boolean delete(String id) throws SQLException { ... }

    private static Feature mapRowToFeature(ResultSet rs) throws SQLException {
        // Map BIGINT timestamps to java.util.Date using:
        long millis = rs.getLong("timestamp_column");
        if (!rs.wasNull()) {
            feature.timestamp = new java.util.Date(millis);
        }
    }
}
```

2. Add table creation to `DatabaseManager.initializeDatabase()`

3. Modify controller to use DAO with fallback:
```java
try {
    features = NewFeatureDAO.findAll();
} catch (SQLException e) {
    System.err.println("从数据库加载失败: " + e.getMessage());
    features = DataManager.loadFeatures(); // Fallback
}
```

### Handling Time Fields

**Database**: `BIGINT` (milliseconds since epoch)
**Java Model**: `java.util.Date`

**Writing to database**:
```java
pstmt.setLong(1, date.getTime()); // Convert Date to long
```

**Reading from database**:
```java
long millis = rs.getLong("timestamp_column");
if (!rs.wasNull()) {
    date = new java.util.Date(millis);
}
```

### Handling Optional Foreign Keys

For nullable foreign keys (e.g., `member_phone` in transactions):
```java
String memberPhone = transaction.memberPhone;
if (memberPhone != null && memberPhone.trim().isEmpty()) {
    memberPhone = null; // Convert empty string to NULL
}
pstmt.setString(7, memberPhone);
```

## Default Credentials

**Admin Account**:
- Username: `admin`
- Password: `admin123`
- Role: Administrator

**Database** (Docker):
- Root: `RootPassword123!`
- User: `cashier` / `YourStrongPassword123!`
- Database: `cashier_system`

## File Storage Location

File-based storage (fallback) uses `data/` directory:
- `data/users.txt`
- `data/inventory.txt`
- `data/members.txt`
- `data/transactions.txt`
- `data/shifts.txt`
- `data/promotions.txt`
- `data/settings.txt`
- `data/categories.txt`

**Important**: Do NOT commit actual data files to version control.

## Known Issues and Gotchas

1. **MySQL Time Zone**: Always include `serverTimezone=Asia/Shanghai` in JDBC URL
2. **Date vs Timestamp**: Database uses BIGINT, NOT TIMESTAMP types
3. **Empty Strings vs NULL**: Must convert empty strings to NULL for foreign keys
4. **Foreign Key Constraints**: Ensure referenced rows exist before inserting
5. **Colima Port Forwarding**: On macOS with Colima, use `--network-address` flag
6. **Transaction Isolation**: DatabaseManager uses auto-commit=true for simple operations
7. **Batch Operations**: Use transactions (conn.setAutoCommit(false)) for multi-table inserts
8. **Graceful Degradation**: All database operations must have try-catch with DataManager fallback
