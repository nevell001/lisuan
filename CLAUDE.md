# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A modern POS (Point of Sale) system built with JavaFX 17, using MySQL for data persistence. The system supports cashier operations, inventory management, member management, transaction tracking, shift management, purchase management, inventory checks, and comprehensive reporting.

**Key Technologies**: Java 17, JavaFX 17.0.8, MySQL 8.0, Maven, HikariCP connection pooling, SLF4J + Logback

**Current Version**: v2.3.2

## Development Commands

### Build and Run
```bash
# Compile the project
mvn compile

# Run the application (requires MySQL database)
mvn javafx:run

# Clean build
mvn clean compile

# Package as JAR
mvn clean package

# Run packaged JAR
java -jar target/cashier-system-fx-2.3.1.jar
```

### Testing
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=UserDAOTest

# Run tests with specific profile
mvn test -Pwindows
```

### Windows Scripts
```batch
# One-click installation (graphical installer)
install.bat

# Command-line installation
installer-simple.bat

# Start application
start.bat

# Start in silent mode (no console)
start-silent.bat

# Create desktop shortcut
create-shortcut.bat

# Package as Windows installer
package-windows.bat
```

**Installer Module** (`src/main/java/com/cashier/installer/`):
- `Installer.java` - Graphical JavaFX-based installer
- Checks Java/Maven versions
- Provides step-by-step installation wizard
- Creates desktop shortcuts

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

## Architecture

### Data Layer Architecture

The system uses **MySQL 8.0 as the primary storage**:
- HikariCP connection pooling for high performance
- Complete DAO layer with type-safe operations
- Transaction support for data consistency
- Auto-created table structures and indexes

**Note**: File storage fallback was removed in v2.3.1. MySQL is now the only supported storage.

### DAO Layer Pattern

**DAO Classes** (`src/main/java/com/cashier/dao/`):
- `UserDAO` - User authentication and management
- `ProductDAO` - Product/Inventory CRUD operations
- `MemberDAO` - Member management, balance, points updates
- `TransactionDAO` - Transaction records and details
- `ShiftDAO` - Shift management (start/end shifts)
- `CategoryDAO` - Category management
- `UnitDAO` - Unit management
- `PromotionDAO` - Promotion management
- `RechargeRecordDAO` - Recharge records
- `OperationLogDAO` - Operation logs
- `SystemSettingsDAO` - System settings
- `ThemePreferenceDAO` - Theme preferences
- `SupplierDAO` - Supplier management
- `PurchaseOrderDAO` - Purchase orders
- `PurchaseOrderItemDAO` - Purchase order line items
- `PurchaseApprovalDAO` - Purchase approvals
- `PurchaseInboundDAO` - Purchase inbound (receipts)
- `PurchaseInboundItemDAO` - Purchase inbound line items
- `InventoryCheckDAO` - Inventory checks
- `InventoryCheckItemDAO` - Inventory check line items

**DAO Pattern**:
- All DAOs are stateless with static methods
- Use `DatabaseManager.getConnection()` for HikariCP connections
- Methods throw `SQLException` - controllers handle errors
- Batch operations use `PreparedStatement.addBatch()`
- Time fields stored as `BIGINT` (millis since epoch), NOT as TIMESTAMP

### Controller Layer

**Controllers** (`src/main/java/com/cashier/controller/`):
- `LoginController` - User authentication
- `CartController` - Shopping cart and checkout
- `CheckoutController` - Payment processing
- `InventoryController` - Product management
- `MemberController` - Member management
- `PromotionController` - Promotion management
- `TransactionController` - Transaction history
- `StatisticsController` - Data statistics
- `ShiftController` - Shift management
- `UserController` - User management
- `SettingsController` - System settings
- `MainController` - Main application shell (admin/finance)
- `PosModeController` - Simplified POS interface for cashiers
- Dialog controllers for edit forms
- Purchase management controllers (Supplier, Order, Approval, Inbound)
- Inventory check controllers
- Report controllers (Purchase, Inventory, Profit)

**Role-Based UI Pattern**:
- `cashier` role → `PosModeController` (simplified interface, only POS + shift)
- `admin`/`finance` roles → `MainController` (full feature access)
- See `CashierSystemFXApplication.switchToMainView()` for routing logic

### Database Schema

**Tables** (created automatically by DatabaseManager):
- `users` - User accounts (username PK, password, name, role, active, create_time, last_login_time)
- `products` - Product inventory (name PK, price, quantity, category, barcode, unit, description, brand, supplier, spec, min_stock, cost, created_at, updated_at)
- `members` - Member accounts (phone PK, name, balance, points, level, discount, join_date, birthday, member_code)
- `transactions` - Transaction headers (transaction_id PK, timestamp, total_amount, tax, final_amount, payment_method, operator_username, operator_name, member_phone)
- `transaction_items` - Transaction line items (transaction_id FK, product_name, price, quantity, subtotal)
- `shifts` - Shift records with revenue statistics
- `categories` - Product categories
- `units` - Measurement units
- `promotions` - Sales promotions
- `recharge_records` - Member recharge records
- `operation_logs` - Operation audit logs
- `system_settings` - System configuration
- `theme_preferences` - User theme preferences
- `suppliers` - Supplier information
- `purchase_orders` - Purchase orders
- `purchase_order_items` - Purchase order line items
- `purchase_approvals` - Purchase approval records
- `purchase_inbound` - Purchase inbound receipts
- `purchase_inbound_items` - Purchase inbound line items
- `inventory_check` - Inventory check records
- `inventory_check_items` - Inventory check line items

**Important Schema Notes**:
- Time fields are `BIGINT` storing milliseconds since epoch
- Foreign keys use `ON DELETE SET NULL` for optional relationships
- Member phone in transactions can be NULL (non-member transactions)
- Empty strings for `member_phone` must be converted to NULL before database insert
- Character set: `utf8mb4` with `utf8mb4_unicode_ci` collation

### Model Layer

**Model Classes** (`src/main/java/com/cashier/model/`):
- `User` - User account with timestamps as `java.util.Date`
- `Product` - Product with all inventory fields
- `Member` - Member with points, balance, level, discount
- `Transaction` - Transaction with List<Product> items
- `Shift` - Shift record with revenue statistics
- `Promotion` - Promotion with type, conditions, discounts
- `Category` - Product category
- `Unit` - Measurement unit
- `RechargeRecord` - Recharge record
- `OperationLog` - Operation log
- `Supplier` - Supplier with level (A/B/C)
- `PurchaseOrder`, `PurchaseOrderItem` - Purchase order entities
- `PurchaseApproval` - Approval record
- `PurchaseInbound`, `PurchaseInboundItem` - Inbound receipt entities
- `InventoryCheck`, `InventoryCheckItem` - Inventory check entities

### Utilities

**Key Utilities** (`src/main/java/com/cashier/util/`):
- `DatabaseManager` - MySQL connection pooling via HikariCP, auto-creates tables
- `PasswordUtil` - Password hashing (BCrypt) and verification
- `FXUtils` - JavaFX helper methods
- `FXMLUtils` - FXML loading utilities
- `StatusBarManager` - Global status bar updates
- `ReceiptPrinter` - Receipt printing functionality
- `LoggerFactoryUtil` - SLF4J logging factory
- `CacheManager` - Product caching with 5-minute expiry, warmup on startup

### Service Layer

**Service Classes** (`src/main/java/com/cashier/service/`):
- `DataService` - High-level data access operations, backup/restore coordination
- `TransactionService` - Transaction business logic (creation, completion, reporting)
- `MemberService` - Member operations (discount calculation, points, level upgrades)
- `InventoryService` - Inventory management operations

**Service Pattern**:
- Services provide business logic abstractions above DAO layer
- Handle multi-DAO transactions and complex operations
- Coordinate caching updates via `CacheManager`
- Throw exceptions for controllers to handle

### Hardware Integration

**Scanner Module** (`src/main/java/com/cashier/scanner/`):
- `ScannerManager` - Manages barcode scanner devices
- `USBHIDScannerDevice` - USB HID scanner implementation
- `FocusManager` - Manages focus targets for scanned input
- Audio feedback: scan_success.wav, scan_error.wav, scan_not_found.wav

**Printer Module** (`src/main/java/com/cashier/printer/`):
- `PrinterManager` - Manages thermal printer devices
- `PrintTemplate` - Configurable receipt templates
- `PrintPreviewDialog` - Preview before printing

**Sound Resources** (`src/main/resources/sounds/`):
- `scan_success.wav` - Played when barcode scan succeeds
- `scan_error.wav` - Played on scan errors
- `scan_not_found.wav` - Played when product not found

### Configuration Files

- `config/database.properties` - MySQL connection configuration
  - Required: `db.url`, `db.username`, `db.password`
  - Optional: `db.pool.size` (default: 10)
  - Example in `config/database.properties.example`
  - Recommended: Use dedicated `cashier` user instead of `root`

- `pom.xml` - Maven build configuration
  - JavaFX 17.0.8
  - MySQL Connector J 8.3.0
  - HikariCP 5.1.0
  - ControlsFX 11.2.1
  - BCrypt 0.10.2
  - SLF4J + Logback for logging
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

3. Modify controller to use DAO:
```java
try {
    features = NewFeatureDAO.findAll();
} catch (SQLException e) {
    System.err.println("从数据库加载失败: " + e.getMessage());
    // Show error to user - no fallback storage
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

### Member Discount System

Member discounts use a 0-10 scale (NOT 0-1):
- 10.0 = No discount (full price)
- 9.8 = 98% of price (2% off)
- 9.0 = 90% of price (10% off)
- 8.5 = 85% of price (15% off)
- 0.0 = Free

### Caching Strategy

**CacheManager** provides product data caching:
- Cache duration: 5 minutes (configurable via `CACHE_EXPIRY_TIME`)
- Three cache maps: by ID, by name, by barcode
- Warmup on application startup via `warmupCache()`
- Use `addToCache()` / `updateCache()` after product modifications
- Use `removeFromCache()` when deleting products
- Always check `isCacheValid()` before reading cache

**Application Startup Sequence**:
1. `DatabaseManager.initialize()` - Create connection pool and tables
2. `DataService.initialize()` - Initialize services
3. `CacheManager.initialize()` - Initialize cache
4. `CacheManager.warmupCache()` - Load all products into cache
5. Load login view and apply theme

### Logging

**Logging Framework**: SLF4J + Logback
- Use `LoggerFactoryUtil.getLogger(ClassName.class)` to get logger instances
- Log levels: ERROR, WARN, INFO, DEBUG
- Configure logback.xml in `src/main/resources/` (if present)
- Log files typically written to project root or logs directory

**Logger Pattern**:
```java
private static final Logger logger = LoggerFactoryUtil.getLogger(MyClass.class);
logger.info("Information message");
logger.error("Error occurred", exception);
```

## Default Credentials

**Admin Account**:
- Username: `admin`
- Password: `admin123`
- Role: Administrator

**Database** (Docker):
- Root: `RootPassword123!`
- Recommended user: `cashier` / `YourStrongPassword123!`
- Database: `cashier_system`

## Known Issues and Gotchas

1. **MySQL Time Zone**: Always include `serverTimezone=Asia/Shanghai` in JDBC URL
2. **Date vs Timestamp**: Database uses BIGINT, NOT TIMESTAMP types
3. **Empty Strings vs NULL**: Must convert empty strings to NULL for foreign keys
4. **Foreign Key Constraints**: Ensure referenced rows exist before inserting
5. **Colima Port Forwarding**: On macOS with Colima, use `--network-address` flag
6. **Transaction Isolation**: DatabaseManager uses auto-commit=true for simple operations
7. **Batch Operations**: Use transactions (conn.setAutoCommit(false)) for multi-table inserts
8. **No File Storage Fallback**: v2.3.1 removed file storage - MySQL is required
9. **Member Discount Scale**: Uses 0-10 scale, not 0-1
10. **Character Encoding**: Use `utf8mb4` for full Unicode/emoji support
11. **Cache Invalidation**: Always update `CacheManager` after product modifications
12. **Role-Based Routing**: Cashier role gets `PosModeController`, not `MainController`
13. **Logger Pattern**: Use `LoggerFactoryUtil.getLogger(ClassName.class)` for logging
14. **Scanner Audio**: Place sounds in `src/main/resources/sounds/` directory

## Key Features Overview

### POS System
- Shopping cart with multiple payment methods (cash, WeChat, Alipay, card)
- Member discount application
- Complete keyboard shortcut system
- Receipt printing

### Inventory Management
- Product CRUD with auto-generated product IDs (P + YYYYMMDD + 4-digit sequence)
- Quick stock intake from multiple sources
- Category and unit management
- Low stock alerts

### Purchase Management (v2.3.0)
- Supplier management with A/B/C level classification
- Purchase order workflow with approval process
- Purchase inbound receipts with partial quantity support
- Automatic inventory updates

### Inventory Check (v2.3.0)
- Full or partial inventory counts
- Variance calculation and recording
- Automatic stock adjustment

### Reporting (v2.3.0)
- Purchase reports (order stats, trends, supplier rankings)
- Inventory reports (turnover, slow-moving, overstock)
- Profit analysis (cost, revenue, margins)

### Member Management
- Member levels with auto-upgrade (Regular -> Silver -> Gold -> Diamond)
- Points accumulation and balance recharge
- Birthday privileges
- Discount system

### Shift Management
- Shift start/end tracking
- Revenue statistics by payment method
- Transaction count tracking

### System Settings
- Three themes (light, dark, IntelliJ)
- Tax rate configuration
- Data backup/restore
- Theme persistence in database
