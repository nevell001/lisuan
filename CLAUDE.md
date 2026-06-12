# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Behavioral Guidelines

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

## Project Overview

This is a **POS (Point of Sale) cashier system** built with JavaFX 17. It's a desktop application for retail operations including cash register functionality, inventory management, member management, purchasing, returns, and reporting.

**Current Version:** v2.5.6 | **Main Entry:** `com.cashier.CashierSystemFXApplication`

**Tech Stack:**
- JavaFX 17.0.12 for UI
- Java 17
- Maven 3.8+
- MySQL 8.4 (with HikariCP connection pooling)
- Javalin 6.1.3 (REST API server)
- JUnit 5 + TestFX + H2 for testing

**Key Architecture Patterns:**
- **MVC Pattern**: Controllers handle UI logic, DAOs handle data access, Models represent entities
- **Service Layer**: Business logic encapsulation (InventoryService, MemberService, TransactionService, ReturnService)
- **Singleton Managers**: PrinterManager, ScannerManager, CacheManager, NotificationManager
- **Custom ORM**: No JPA/Hibernate - raw JDBC with PreparedStatement for SQL operations
- **FXML Views**: UI defined in FXML files under `src/main/resources/com/cashier/view/`
- **REST API**: Javalin-based HTTP API with token authentication (v2.5.0)
- **WebSocket**: Real-time multi-terminal synchronization (v2.5.0)

## Common Development Commands

```bash
# Build and run
mvn clean compile
mvn javafx:run

# Package
mvn clean package
java -jar target/lisuan-fx-2.5.6-jar-with-dependencies.jar

# Run tests
mvn test
mvn test -Dtest=ProductDAOTest
mvn test -Dtest=PasswordUtilTest#testHashPassword

# Database (Docker Compose - recommended)
docker-compose up -d mysql
docker-compose logs -f mysql

# Quick install
./install.sh           # Linux/macOS
install.bat            # Windows
```

**Note**: Maven uses Aliyun Maven mirror (`https://maven.aliyun.com/repository/public`) for faster dependency resolution in China.

## Architecture Deep Dive

### Layer Structure

```
Controller (31 classes) → Service (7 classes) → DAO (24+ classes) → Database
        ↓                          ↓              ↓
     FXML Views              Business Logic    Data Access

REST API Layer (v2.5.0):
ApiController (15 classes) → Service Layer → DAO Layer
        ↓
    WebSocket Sync (real-time)
```

### Key Components

**DatabaseManager** (`util/DatabaseManager.java`)
- Uses HikariCP connection pooling (pool size, connection timeout, idle timeout, max lifetime configurable)
- Reads from `config/database.properties`
- Initializes all database tables on startup
- Supports UTF-8/utf8mb4 encoding
- Important: Product names have UNIQUE constraint (v2.4.3)
- Platform-specific profiles (windows/mac/linux) for JavaFX runtime path
- Compatible with MySQL 8.0, 8.3, and 8.4 LTS

**CacheManager** (`util/CacheManager.java`)
- 5-minute expiration for product cache
- Multi-dimensional caching (by ID, name, barcode)
- Batch operations auto-clear cache
- Cache warmup on application start

**Service Layer**
- `InventoryService` - Batch inventory updates, low stock queries
- `MemberService` - Member recharge, level upgrades, balance checks
- `TransactionService` - Transaction execution with optimistic locking
- `ReturnService` - Return order creation, approval workflow, inventory restoration
- `DataService` - Theme preferences, system settings, initialization
- `InventoryAlertService` - Scheduled inventory alert monitoring (auto-starts on login)
- `BackupService` - Scheduled automatic database backup (auto-starts on login)

**Theme System**
- Three themes: light, dark, intellij
- Theme preferences persisted per user
- Apply via: `getApp().applyTheme(getScene(), themeName)`
- CSS files in `src/main/resources/css/`

### REST API Architecture (v2.5.0)

**ApiServer** (`api/ApiServer.java`)
- Javalin 6.1.3-based HTTP server on port 8080
- Token-based authentication (24-hour expiration)
- CORS enabled for cross-origin requests
- JSON serialization via Jackson

**API Controllers** (`api/controller/`):
- `AuthController` - Login, token refresh, logout
- `ProductApiController` - Product CRUD (6 endpoints)
- `MemberApiController` - Member CRUD, recharge, search (8 endpoints)
- `TransactionApiController` - Transaction processing, stats (7 endpoints)
- `InventoryApiController` - Stock updates, alerts (5 endpoints)
- `ReportApiController` - Daily/monthly/sales reports (5 endpoints)
- `PaymentApiController` - Electronic payment (11 endpoints)
- `InvoiceApiController` - Invoice management (10 endpoints)
- `PrintApiController` - Network printing (14 endpoints)
- `BackupApiController` - Cloud backup (9 endpoints)
- `I18nApiController` - Multi-language support (6 endpoints)
- `UserApiController` - User management (admin only)
- `SettingsApiController` - System settings
- `HealthController` - Health check (no auth required)

**WebSocket Sync** (`api/sync/`):
- `SyncWebSocketHandler` - WebSocket connection handler
- `SyncManager` - Broadcasts inventory/transaction changes
- `TerminalConnection` - Manages connected terminals
- Event types: PRODUCT_UPDATE, INVENTORY_CHANGE, TRANSACTION_COMPLETE, MEMBER_UPDATE

**Authentication Middleware** (`api/middleware/AuthMiddleware.java`):
- Validates Bearer tokens on protected endpoints
- Public endpoints: `/api/v1/health`, `/api/v1/auth/login`
- Token storage: In-memory ConcurrentHashMap

### Data Access Patterns (v2.5+ DAO Refactoring)

**IMPORTANT**: The codebase is migrating from static DAO methods to instance-based DAOs via `DAOFactory`.

**New Pattern (Recommended):**
- Use `DAOFactory.getInstance().getProductDAO()` to get DAO instances
- DAOs extend `BaseDAO` for connection management and transaction support
- Instance methods instead of static methods
- Supports dependency injection and better testability

```java
// Get DAO instance via factory
private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();

// Use instance methods
Product product = productDAO.findById(id);
List<Product> products = productDAO.findAll();
productDAO.update(product);
```

**Legacy Pattern (Being Phased Out):**
- Old `ProductDAO` uses static methods
- Still exists in some files but should not be used for new code
- Example files still using legacy pattern: `DataService.java`, `TransactionService.java`, `ReturnService.java`

**BaseDAO** (`dao/BaseDAO.java`)
- Abstract base class for all new DAOs
- Provides `getConnection()`, transaction management methods
- `executeInTransaction()` for transactional operations
- Automatic logger setup via `LoggerFactoryUtil`
- **通用查询方法**（v2.5.5+）:
  - `queryList()` - 查询列表
  - `queryOne()/queryOneOrNull()` - 查询单个对象
  - `queryScalar()/queryInt()/queryLong()` - 查询单值
  - `executeUpdate()` - 执行更新
  - `executeInsertReturnId()` - 执行插入并返回ID
  - `batchUpdate()` - 批量更新
  - `exists()` - 检查记录存在
  - `count()` - 统计记录数

**RowMapper** (`dao/RowMapper.java`)
- Functional interface for mapping ResultSet to objects
- Used with BaseDAO query methods

**DAOFactory** (`dao/DAOFactory.java`)
- Singleton factory for DAO instance management
- Currently registers `ProductDAORefactored`
- Pattern: `DAOFactory.getInstance().getProductDAO()`

### Controller Patterns

- Use `@FXML` annotation for UI component injection
- Initialize in `initialize()` method
- Event handlers also use `@FXML`
- Get app reference: `getApp()` or set via `setApplication()`
- Show errors: `showError()` and `showAlert()` utility methods

### User Roles & Views

- **admin**: Full access → MainView (full interface)
- **cashier**: POS operations → PosModeView (simplified interface)
- **finance**: Reports and statistics → MainView

### i18n (Internationalization)

**I18nManager** (`i18n/I18nManager.java`)
- ResourceBundle-based localization
- Supported languages: zh_CN (default), en, ja, ko
- Language files: `src/main/resources/com/cashier/i18n/messages_*.properties`
- Dynamic language switching via `I18nManager.setLocale()`
- FXML bindings: Use `%resource_key` syntax

## Important Business Rules

### Product Name Uniqueness (v2.4.3)
- Product names must be UNIQUE
- Database constraint: `ALTER TABLE products ADD CONSTRAINT uk_product_name UNIQUE (name)`
- Application-level validation in ProductDAO insert/update
- Error message: "商品名称已存在，请使用其他名称"

### Member Discount System
- Discount values: 10 = no discount, 9.5 = 5% off, 9 = 10% off, 8.5 = 15% off, 0 = free
- Member levels auto-upgrade based on points (via `MemberService.updateMemberLevel()`):
  - Regular (普通): 0-1999 points (10.0 discount - no discount)
  - Silver (银卡): 2000-4999 points (9.5 discount - 5% off)
  - Gold (金卡): 5000-9999 points (9.0 discount - 10% off)
  - Diamond (钻石): 10000+ points (8.5 discount - 15% off)
- Discount calculation: `amount * (member.discount / 10.0)`

### Inventory Management
- Product code format: `P + YYYYMMDD + 4-digit sequence`
- Member code format: `MEM + YYYYMMDD + 4-digit sequence` (e.g., MEM202603110001)
- Barcode uniqueness: REMOVED in v2.3.1 (duplicates allowed)
- Product name uniqueness: ADDED in v2.4.3

### Transaction Flow
1. Add items to cart (CartItem)
2. Apply member discount (if applicable)
3. Apply promotions
4. Process payment (cash, WeChat, Alipay, bank card)
5. Update inventory (optimistic locking via `version` field)
6. Record transaction
7. Update member points/balance
8. Broadcast to connected terminals (v2.5.0)

### Return Order Flow (v2.4.0)
1. Create return order linked to original transaction
2. Select items to return
3. Submit for approval
4. Approver reviews and processes
5. Restore inventory (optimistic locking)
6. Process refund (cash/balance/points)

## Database Schema Notes

**Critical Tables:**
- `products` - name has UNIQUE constraint (v2.4.3), barcode allows duplicates
- `members` - member_code is UNIQUE, auto-generated (format: MEM + YYYYMMDD + 4-digit)
- `transactions` - main transaction records
- `transaction_items` - line items with product_id, product_code, barcode (v2.4.1)
- `users` - three roles: admin, cashier, finance
- `specifications` - product specification types (v2.4.4)
- `specification_values` - product specification values (v2.4.4)
- `product_specifications` - product-specification associations with SKU codes (v2.4.4)
- `payment_records` - electronic payment records (v2.5.0)
- `invoices` - invoice management (v2.5.0)

**Initialization:**
- Full init: `docker/mysql-init/00-init-complete.sql`
- v2.4.3: `08-v2.4.3-product-name-unique.sql` - Product name UNIQUE constraint
- v2.4.4: `10-v2.4.4-specification-management.sql` - Specification management tables
- Version management: `docker/mysql-init/DATABASE_VERSIONS.md`
- Use `--default-character-set=utf8mb4` when importing

## Testing Conventions

- Inherit from `DatabaseTestBase` for test configuration
- Use H2 in-memory database for unit tests
- Test class naming: `{ClassName}Test.java`
- Use `@DisplayName` for clear descriptions
- Tests located in `src/test/java/com/cashier/`

## Performance Optimizations (v2.4.0+)

**UI Rendering:**
- Use `UIOptimizer.asyncLoad()` for background data loading
- Virtualize large lists with `UIOptimizer.virtualize()`

**Query Optimization:**
- Batch queries with `QueryOptimizer.batchQuery()`
- 1000-item batch size for large ID sets

**Batch Operations:**
- Use `BatchOperationUtil` for bulk inserts/updates
- JDBC batch processing with transaction management

## Special Modules

**Scanner Integration (v2.3.1)**
- USB HID scanner support
- Auto-focus management via `FocusManager`
- Scan sound feedback in `src/main/resources/sounds/`

**Printer Management (v2.3.1)**
- Print queue via `PrintTask`
- Print preview with `PrintPreviewDialog`
- Multiple device types supported

**Data Export (v2.4.0)**
- Excel: Apache POI 5.2.5
- PDF: Apache PDFBox 3.0.4
- Chinese font: `NotoSansSC-Regular.ttc`
- Export history tracked in `export_history` table

**Notification System (v2.4.1)**
- `NotificationManager` singleton
- Types: INFO, WARNING, ERROR, SUCCESS
- Real-time push notifications

**Specification Management (v2.4.4)**
- Three-tier system: specifications (types) → specification_values (values) → product_specifications (associations)
- Support for: COLOR, SIZE, MATERIAL, OTHER specification types
- SKU-based inventory with price adjustments per specification
- Unique constraints: specification codes, specification value codes, SKU codes, and product-specification-value combinations

## Common Patterns to Follow

### Version Management
**CRITICAL**: `AppConstants.APP_VERSION` is the source of truth for application version. When releasing a new version:
1. Update `AppConstants.APP_VERSION` in `com.cashier.constant.AppConstants`
2. Update `pom.xml` version property to match
3. Version is displayed in Help menu and about dialogs

### Logging (Required for all new classes)
```java
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

public class YourClass {
    private static final Logger logger = LoggerFactoryUtil.getLogger(YourClass.class);

    public void someMethod() {
        logger.info("Info message");
        logger.error("Error message", exception);
    }
}
```
**CRITICAL**: Always use `LoggerFactoryUtil.getLogger()`, never `LoggerFactory.getLogger()`. Never use `System.out.println()` for logging.

### Adding a New Feature Module
1. Create Model class in `model/`
2. Create DAO class extending `BaseDAO` in `dao/` with standard CRUD methods
3. Register DAO in `DAOFactory.registerDefaults()` if it needs factory access
4. Create Service class in `service/` (if business logic needed)
5. Create Controller in `controller/` - use `DAOFactory` to get DAO instances
6. Create FXML view in `src/main/resources/com/cashier/view/`
7. Add menu item in `MainView.fxml`
8. Update `DatabaseManager.initializeDatabase()` if new tables needed
9. For API endpoints, create ApiController in `api/controller/` and register in `ApiServer.registerRoutes()`

**DAO Pattern for New Code:**
```java
public class YourEntityDAO extends BaseDAO {
    private static final String SELECT_COLUMNS = "id, name, ...";

    // 定义静态 RowMapper 复用
    private static final RowMapper<YourEntity> ENTITY_MAPPER = rs -> {
        return new YourEntity(
            rs.getInt("id"),
            rs.getString("name"),
            // ... 其他字段
        );
    };

    // 使用通用方法简化查询
    public YourEntity findById(int id) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM your_table WHERE id = ?";
        return queryOneOrNull(sql, ENTITY_MAPPER, id);
    }

    public List<YourEntity> findAll() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM your_table ORDER BY name";
        return queryList(sql, ENTITY_MAPPER);
    }

    public long count() throws SQLException {
        return queryLong("SELECT COUNT(*) FROM your_table");
    }

    public int insert(YourEntity entity) throws SQLException {
        String sql = "INSERT INTO your_table (name) VALUES (?)";
        return (int) executeInsertReturnId(sql, entity.name);
    }

    public boolean update(YourEntity entity) throws SQLException {
        String sql = "UPDATE your_table SET name = ? WHERE id = ?";
        return executeUpdate(sql, entity.name, entity.id) > 0;
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM your_table WHERE id = ?";
        return executeUpdate(sql, id) > 0;
    }

    // In DAOFactory.registerDefaults():
    // register(YourEntityDAO.class, new YourEntityDAO());
}

// In Controller/Service:
private final YourEntityDAO dao = DAOFactory.getInstance().getDAO(YourEntityDAO.class);
```

### Using Cache
```java
// Get from cache
Product product = CacheManager.getProductFromCache(productId);

// Add to cache
CacheManager.addToCache(product);

// Clear after updates
CacheManager.clearCache();
```

### Database Transactions
```java
Connection conn = DatabaseManager.getConnection();
try {
    conn.setAutoCommit(false);
    // ... operations ...
    conn.commit();
} catch (SQLException e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(true);
}
```

### REST API Pattern (v2.5.0)
```java
// In ApiServer.registerRoutes()
app.get("/api/v1/resources", ctx -> {
    try {
        List<Resource> resources = ResourceDAO.findAll();
        ctx.json(resources);
    } catch (SQLException e) {
        logger.error("Failed to fetch resources", e);
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
    }
});

// Protected endpoint (requires auth)
app.post("/api/v1/resources", AuthMiddleware.authenticate, ctx -> {
    // Resource creation logic
});
```

### WebSocket Broadcasting (v2.5.0)
```java
// Broadcast inventory changes to all connected terminals
SyncManager.broadcastInventoryChange(productId, newQuantity);
SyncManager.broadcastTransactionComplete(transactionId);
```

## Quick Reference

**Default Login:** admin / admin123

**Config Files:**
- `config/database.properties` - Database connection (HikariCP pool settings included)
- `config/jvm.config` - JVM options
- `config/printer.properties` - Printer settings
- `config/api.properties` - API server settings (v2.5.0)
- Note: Maven resource filtering is enabled for `.properties`, `.fxml`, `.css` files

**Key Paths:**
- Controllers: `src/main/java/com/cashier/controller/`
- API Controllers: `src/main/java/com/cashier/api/controller/`
- DAOs: `src/main/java/com/cashier/dao/`
- Models: `src/main/java/com/cashier/model/`
- Views: `src/main/resources/com/cashier/view/`
- Tests: `src/test/java/com/cashier/`
- i18n: `src/main/resources/com/cashier/i18n/`
- DB init scripts: `docker/mysql-init/`

**Shortcut Keys (POS/Checkout):**
- F1 - Add product to cart
- Delete - Remove selected item
- Ctrl+L - Clear cart
- F8 - Cash payment
- Ctrl+1/2/3 - WeChat/Alipay/Bank card
- Ctrl+F - Focus search box
- Ctrl+M - Focus member phone
- Ctrl+/ - Show shortcut help

## Ongoing Migration: DAO Refactoring (v2.5+)

The codebase is migrating from static `ProductDAO` to instance-based `ProductDAORefactored`:

**Status:**
- ✅ Completed: `CartController`, `InventoryController`, `ProductEditController`, `RestockController`, `InventoryAlertController`, `InventoryCheckController`, `ProfitReportController`, `PurchaseOrderController`, `PurchaseInboundController`
- ✅ Completed: API Controllers (`ProductApiController`, `InventoryApiController`, `TransactionApiController`)
- ✅ Completed: `CacheManager`, `ProductDataImporter`
- ✅ Completed: `DataService`, `TransactionService`, `ReturnService`
- ⏳ Pending: `SyncBroadcastService` (has unused import)

**Migration Pattern:**
```java
// OLD (static):
ProductDAO.findById(id);
ProductDAO.findAll();

// NEW (instance via factory):
private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
productDAO.findById(id);
productDAO.findAll();
```

When working on files that still use the old `ProductDAO`, consider migrating them to the new pattern.
