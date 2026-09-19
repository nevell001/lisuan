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

**Current Version:** v2.6.0 | **Main Entry:** `com.cashier.Launcher`（转发到 `com.cashier.CashierSystemFXApplication`；
可执行 JAR 的 Main-Class 必须是不继承 `Application` 的类，否则 `java -jar` 会报“缺少 JavaFX 运行时组件”）

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

# Package (skip tests)
mvn clean package -DskipTests
java -jar target/lisuan-fx-*-jar-with-dependencies.jar

# Run tests
mvn test
mvn test -Dtest=ProductDAOTest
mvn test -Dtest=PasswordUtilTest#testHashPassword

# Static analysis (SpotBugs high-risk gate)
mvn -q -DskipTests spotbugs:check

# Production release verification
./release.sh           # Linux/macOS
release.bat            # Windows

# Database (Docker Compose - recommended)
docker compose up -d mysql
docker compose logs -f mysql

# Quick install
./install.sh           # Linux/macOS
install.bat            # Windows
```

**Note**: Maven uses Aliyun Maven mirror (`https://maven.aliyun.com/repository/public`) for faster dependency resolution in China.

## Architecture Deep Dive

### Layer Structure

```
Controller (34 classes) → Service (14 classes) → DAO (29 instance DAOs) → Database
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
- Password is injected at runtime via `CASHIER_DB_PASSWORD` (legacy `CASHER_DB_PASSWORD` still accepted)；
  取值顺序是**进程环境变量 → 工作目录 `.env`**（`com.cashier.util.DotEnv`，应用自己也会读，
  所以 `java -jar` 与 `start.bat` 用同一份配置）；`config/*.properties` 里不得存明文密码
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
- Three themes: LiSuan (default), light, dark
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
- Public endpoints: `/api/health`, `/api/auth/login`
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

**Legacy Pattern (Status Updated):**
- The old static `ProductDAO` implementation has been removed from `src/main/java/com/cashier/dao/` and replaced by the instance-based `ProductDAORefactored` accessible via `DAOFactory.getInstance().getProductDAO()`.
- If rollback reference is needed, use Git history for the removed legacy implementation.
- If you have external scripts, CI jobs or docs that reference `ProductDAO`, update them to use `DAOFactory.getInstance().getProductDAO()` or `ProductDAORefactored` instance methods.

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

- **admin**: 完整主界面 `MainView`（全部功能）
- **finance**: 完整主界面 `MainView`（报表与统计）
- **cashier**: 登录后**直接进触屏收银台** `TouchCartView`（`CashierSystemFXApplication.switchToPosModeView`），
  不经完整主界面

标准收银台 `CartView` 只能从完整主界面进入（`MainController.handleCart`，导航栏"收银台"按钮）。
没有"POS 模式"设置项，用哪套收银界面**完全由角色决定**。

**POS Interface Variants:**
- `CartView` - Standard POS with keyboard shortcuts（`CartController`，管理员/财务在完整界面里打开）
- `TouchCartView` - Touch-optimized POS with larger buttons, one-tap language switching
  （`TouchCartController`，`cashier` 角色登录后的默认界面）

### i18n (Internationalization)

**I18nManager** (`i18n/I18nManager.java`)
- ResourceBundle-based localization
- Supported languages: zh_CN (default), zh_TW, en
- Language files: `src/main/resources/com/cashier/i18n/messages_*.properties`
- Dynamic language switching via `I18nManager.setLocale()`
- FXML bindings: Use `%resource_key` syntax

**Language Preference Storage** (`dao/LanguagePreferenceDAO.java`)
- Hierarchical preference resolution: user-specific → global default → system default (zh-CN)
- User preferences stored in `language_preferences` table with `language_tag` field
- Global default stored with `username = 'default'`
- Language tags use BCP 47 format: `zh-CN`, `zh-TW`, `en`

**Touch Screen Language Switching** (v2.6.0)
- TouchCartController provides one-tap language switching via toolbar button
- Saves both user preference and updates global default
- Dialog with radio buttons for language selection

## Important Business Rules

### Product Name Uniqueness (v2.4.3)
- Product names must be UNIQUE
- Database constraint: `ALTER TABLE products ADD CONSTRAINT uk_product_name UNIQUE (name)`
- Application-level validation in ProductDAO insert/update
- Error message: "商品名称已存在，请使用其他名称"

### Member Discount System
- Discount values: 10 = no discount, 9.5 = 5% off, 9 = 10% off, 8.5 = 15% off, 0 = free
- Member levels auto-upgrade based on points (via `MemberService.updateMemberLevel()`):
  - Regular (普通): 0-999 points (10.0 discount - no discount)
  - Silver (银卡): 1000-4999 points (9.5 discount - 5% off)  <!-- 银卡门槛 1000（业务确认，勿改回 2000） -->
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

**TestFX UI Testing**:
- Extend `ApplicationExtension` for UI tests
- Use `@Start` to initialize JavaFX stage
- Keep UI tests simple - avoid complex FXML loading in headless environments
- Test component visibility, IDs, and basic interactions
- Example: `LoginControllerUITest.java` demonstrates simplified UI testing pattern
- 默认构建（`mvn verify`）会排除 `LoginControllerUITest`，因为它需要真实显示环境；
  在具备桌面显示环境时显式运行：`mvn -Pui-tests -Dtest=LoginControllerUITest test`

```java
@ExtendWith(ApplicationExtension.class)
public class YourControllerUITest extends DatabaseTestBase {
    @Start
    public void start(Stage stage) {
        // Initialize test database
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        // Create simple UI components directly
        TextField field = new TextField();
        field.setId("testField");
        Scene scene = new Scene(field);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @DisplayName("Test component properties")
    void testComponent(FxRobot robot) {
        robot.clickOn("#testField");
        // Verify behavior
    }
}
```

## Performance Optimizations (v2.4.0+)

**UI Rendering:**
- Use `UIOptimizer.runInBackground(...)`（查库→刷新界面）或 `UIOptimizer.loadAsync(Task, ...)` for background data loading
- Virtualize large lists with `UIOptimizer.enableTableViewVirtualization(TableView)`（或 `createVirtualListView(int)`）

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

## Security Best Practices

### Random Number Generation
**CRITICAL**: For security-sensitive random values (order IDs, tokens), use `SecureRandom`, never `Math.random()`:
```java
private static final SecureRandom SECURE_RANDOM = new SecureRandom();
String randomCode = String.format("%04d", SECURE_RANDOM.nextInt(10000));
```

### Password Storage
All passwords are hashed using BCrypt via `PasswordUtil`. Never store plaintext passwords.

### 历史明文口令的处置决定（2026-09 审计）

早期提交里存在明文数据库口令（`config/database.properties`、`docker/docker-init.sh`、
`install.sh` 等，文件已删除但仍在 git 历史中）。当时的处置结论：

- **已核对**：这些历史口令与当前 `.env` / `config/*.properties` 里的凭据**逐个比对均不相等**，
  即本机配置层面已作废；但本机没有可连的 MySQL，无法验证它们对其它环境是否仍有效。
- **不重写历史**：仓库已公开且存在 fork，泄露已不可收回，`git filter-repo` 重写只会破坏
  现有克隆/PR（且 GitHub 侧每 3 小时被 Gitee 强制覆盖），收益有限。
  若这些口令曾在开发机/预发/生产复用，**以轮换为准**——重写历史不能替代轮换。
- **防止再犯**：新增 `com.cashier.security.SecretHygienePolicyTest`（随 `mvn verify` 在 CI 运行），
  扫描 **git 跟踪**文件——properties/.env 的凭据键不得带非占位符字面量；脚本/yml 不得出现
  带引号的硬编码凭据或 `${VAR:-"字面量"}` 默认口令。违规时直接报出 `文件:行号`。
  只扫跟踪文件，故本地 `.env`、`config/*.properties`（gitignored，本就该放真实口令）不参与。

### SQL Injection Prevention
Always use `PreparedStatement` with parameterized queries. Never concatenate user input into SQL strings.

### JDBC Resource Management
Always use try-with-resources for `ResultSet` and `Statement`:
```java
try (Connection conn = DatabaseManager.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    // ... operations
}
```

### Validation Rules
Use `FormValidator.Rules` for consistent validation. For numeric range validation, prefer `Double.parseDouble()` over regex:
```java
// Example: DISCOUNT rule validates 0-10 range
value -> {
    try {
        double d = Double.parseDouble(value);
        return d >= 0 && d <= 10;
    } catch (NumberFormatException e) {
        return false;
    }
}
```

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
app.get("/api/resources", ctx -> {
    try {
        List<Resource> resources = ResourceDAO.findAll();
        ctx.json(resources);
    } catch (SQLException e) {
        logger.error("Failed to fetch resources", e);
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
    }
});

// Protected endpoint (requires auth)
app.post("/api/resources", AuthMiddleware.authenticate, ctx -> {
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
- `config/database.properties` - Database connection (HikariCP pool settings included; password via `CASHIER_DB_PASSWORD` from `.env`/environment, legacy `CASHER_DB_PASSWORD` accepted)
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
- ✅ DAO 层：所有在用 DAO 已完成实例化迁移（29 个 `XxxDAORefactored extends BaseDAO`，通过 `DAOFactory` 注册）；
  静态方法风格 DAO 已全部移除；无调用方的 `SpecificationDAORefactored`（商品规格预留类）及
  `calculate*`/`sumByDateRange`/`countByDateRange` 等统计死方法已清理；
  仅保留 `BaseDAO`/`DAOFactory`/`RowMapper` 基础设施
- ✅ Completed: `CartController`, `InventoryController`, `ProductEditController`, `RestockController`, `InventoryAlertController`, `InventoryCheckController`, `ProfitReportController`, `PurchaseOrderController`, `PurchaseInboundController`
- ✅ Completed: API Controllers (`ProductApiController`, `InventoryApiController`, `TransactionApiController`)
- ✅ Completed: `CacheManager`, `ProductDataImporter`
- ✅ Completed: `DataService`, `TransactionService`, `ReturnService`
- ✅ Completed: `SyncBroadcastService`

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

## Code-Side Release Checks (v2.6)

- CI 构建门禁：`.github/workflows/build.yaml` 在 push / PR 到 `main` 时执行
  `xvfb-run -a mvn -B -ntp verify`（与本地一致：测试 + SpotBugs + JaCoCo），
  失败时上传 `target/surefire-reports` 便于定位；`sync.yaml` 同步时会备份并恢复
  `.github/workflows`，故本文件不会被 Gitee 覆盖
- 版本号四处同步（`AppConstants`/`pom.xml`/`installer/Installer.java`/`.env.example`）当前均为 `2.6.0`
- i18n 门禁：`I18nBundleConsistencyTest` 断言三份语言包 key 集合一致、`I18nKeys` 常量齐全、
  源码字面量 i18n 调用 key 齐全（缺 key 时界面会直接显示 key，属 UI 缺陷）
- 并发安全：`ConcurrentDeductionTest` 多线程验证乐观锁防库存超卖、防会员余额超扣
- token 过期：`ApiServerTest.expiredTokensAreRejected` 注入短过期验证过期 token 被拒
- 连接泄漏：`HealthController.detail` 已改为 try-with-resources（健康检查不再泄漏连接池连接），
  `HealthControllerTest.detailHealthCheckReportsDatabase` 覆盖该路径
- 日志轮转：`logback.xml` 已配置 `RollingFileAppender` + `TimeBasedRollingPolicy`（maxHistory 30/90）
- 导出验证：`ExportUtilTest` 用 POI/PDFBox 读回 Excel/PDF 导出文件，验证内容与页数完整；
  非法子目录（路径穿越）被拒绝
- 补测 DAO：盘点明细、语言偏好（回退链/货币）、备份记录与配置（测试内自建表）
- 补测 Service：会员等级边界（金卡 5000/钻石 10000、null/负数、折扣映射）、发票金额/税额计算、
  数据服务设置持久化与主题偏好回退、库存有界加载
- 补测 商品服务 CRUD/分页/批量导入/删除异常、货币工具格式化/解析/货币切换
- 补测 查询优化工具（分批查询、批次失败跳过、EXPLAIN 安全校验）
- 支付回调：`PaymentService.handlePaymentNotify` 增加终态保护（已退款/关闭/取消订单拒绝迟到回调，
  已成功订单重复通知幂等确认）；新增 `PaymentServiceNotifyTest`（验签/金额/幂等/终态）与
  `AlipayPrecreatePaymentProviderTest`（支付宝回调验签正反）
- 打印机降级：`PrinterManagerTest` 补"无可用打印机时任务失败且不中断"覆盖
- 结账口径一致性：`CheckoutConsistencyPolicyTest` 断言税额只在 `TransactionService.calculateTax`
  计算（税率是小数 0.0-1.0，不得再除以 100）、触屏收银台必须计算并落库促销、支付方式筛选
  必须归一化后再比较；`TransactionServiceTest` 覆盖税率小数语义、促销按原价总额计算、
  `selectBestPromotion` 选优；`I18nUiUtilsTest` 覆盖中文/代码支付方式归一化
- 启动入口：可执行 JAR 的 Main-Class 为 `com.cashier.Launcher`（不继承 `Application`），
  使 `java -jar lisuan-fx-*-jar-with-dependencies.jar` 无需 module-path 即可启动

**退货/库存正确性（v2.6.0 补强）**

- 退货按整单实付比例折算退款单价：`ReturnService.refundRatio/refundUnitPrice`，
  9.5 折成交的 100 元只能退 95 元（此前按原价退款，把优惠一起退给顾客）
- 商品编号按"已用最大序号 +1"生成（`ProductDAORefactored.findMaxProductCodeSequence`），
  并带 3 次撞号重试；删除商品后不再重复生成已存在的编号
- 商品名唯一性落到 `ProductDAORefactored.insertWithConnection/update` 与
  `DatabaseManager.ensureProductNameUnique`（含老库补约束、有重名时告警跳过）
- 扣减库存直接使用事务内重读的最新行（不再改动调用方共享的内存/缓存对象，
  也不再用缓存里的旧价格覆盖别处刚提交的修改）；失败回滚后内存库存保持不变
- 应付金额统一走 `TransactionService.calculateFinalAmount`（含 2 位小数四舍五入），
  标准收银台与触屏收银台口径一致，按显示金额付款不再被判"金额不足"

**REST API 加固（v2.6.0 补强）**

- `POST /api/transactions` 只信任明细的商品 ID 与数量：单价/小计/合计/税额服务端重算，
  并复用 `TransactionService.executeTransaction` 完成库存扣减、会员积分与乐观锁
- `POST /api/transactions/:id/refund` 退回会员余额、按"每元 10 分"冲减积分、还原库存，
  退款单价同样按整单实付比例折算
- 操作员身份一律取 `ctx.attribute("currentUser")`（下单与退款均忽略请求体自报身份）
- 支付宝回调不再注入合成参数：`trade_no` 在验签后按渠道取值
  （`PaymentService.channelTransactionId`），真实回调验签不再必然失败
- `/api/invoices/seller-info`（PUT）收紧为管理员专属；开票方信息属于全局配置
- `GET /api/transactions` 支付方式筛选改为**归一化后比较**：`?paymentMethod=CASH` 能命中
  落库为「现金」的记录，反向亦然（此前是裸 `contains`，按代码筛选恒为空）
- `GET /api/transactions` 带日期筛选时 `limit` 下推到交易主表子查询
  （`TransactionDAORefactored.findByDateRange(start, end, limit)`），
  此前忽略 `limit`、可被认证用户用宽区间拉回整段交易
- 支付宝**出站响应**验签：缺少 `sign` 直接判失败
  （`AlipayPrecreatePaymentProvider.verifyAlipayResponse`，此前缺签名会放行）
- 支付退款并发安全（`PaymentService.applyRefund`）：事务内 `SELECT ... FOR UPDATE`
  锁定支付单行并以 `APPLYING` 预占额度，渠道网络调用移到事务外，终态按"已成功退款合计"
  判定（申请中只是预占、不算已退）；并发对同一支付发起多笔退款不再可能累计超过实付金额
  （`PaymentServiceRefundTest.concurrentRefundsCannotExceedPaidAmount`）

**退货退款口径与安全审计 L 级项（v2.6.0 补强）**

- 桌面退货 `ReturnService.completeReturnOrder` 区分退款方式：**现金单退现金**
  （不冲会员余额，只写 `RETURN_REFUND_CASH` 操作日志留痕），非现金单退回余额并写充值流水；
  两种情况都按 `ReturnService.pointsToReverse`（退货金额占原单实付的比例）冲减原单积分并重算等级。
  无会员的退货不再被提前 return，同步广播不再漏发
- Token 过期清理：`ApiServer.generateToken` 签发前调用 `purgeExpiredTokens()`，
  过期条目不再常驻内存（`ApiServerTest.expiredTokensArePurgedOnIssue`）
- 权限收紧：`/api/payment/stats*` 归入财务/管理员（`AuthorizationMiddlewareTest`）
- 打印机发现只扫**本机子网**且端口限定 `STANDARD_PRINTER_PORTS`(9100/515/631)，
  不再接受调用方指定子网与任意端口（`PerformancePolicyTest.printerDiscoveryIsBounded`）
- `POST /api/printers/:id/receipt` 只接受纯文本小票：≤8KB 且拒绝控制字符（含 ESC/POS），
  `PrintApiControllerTest` 覆盖控制字符/超长/正常文本三条路径
- 模拟支付回调密钥改用 `MessageDigest.isEqual` 定长比较
- 发票编号改为 `INV + 时间戳 + 进程内序号 + 6 位随机段`，同毫秒并发不撞号且不可预测
  （`InvoiceTest`）
- 退款编号同样加固：`PaymentDAORefactored.insertRefund` 的 `refund_id`（此前只有毫秒时间戳，
  同毫秒两笔退款必撞主键）与 `RefundRecord.generateRefundNo` 改为
  `时间戳 + 进程内序号 + 6 位随机段`（`PaymentDAOTest.refundIdsAreUniqueWithinSameMillisecond`）
- 删除 `UserDAORefactored.authenticate`（忽略密码参数的死方法，易被误用成免密登录）

**收银台 FX 线程纪律（v2.6.0 补强）**

- 标准收银台与触屏收银台的**库存加载、商品搜索、分类加载、班次查询**统一走
  `UIOptimizer.runInBackground`（后台查库 + `Platform.runLater` 回 UI 线程），
  并以 `productQuerySequence` 丢弃过期结果，避免打开收银台/连续扫码时整屏卡死；
  门禁见 `FxThreadDbPolicyTest`（断言查询必须作为后台任务提交，且旧的同步写法不得回归）
- 触屏收银台初始商品列表由默认选中的「热销推荐」驱动（`initialize()` 不再额外同步
  `loadProducts(null)`），分类加载失败时兜底展示全部商品
- **单行查库同样不得留在 FX 线程**：入车前的班次检查 + 最新库存查询（`CartController.addToCart`
  汇总为 `CartAddContext`，`TouchCartController.addToCart` 拆出 `applyAddToCart`）与
  会员查询（`handleSearchMember` → `findByPhone`）都先提交后台，回 FX 线程再校验/落库；
  `FxThreadDbPolicyTest.singleRowLookupsRunOffTheFxThread` 按**方法体**断言
  （同步写法里"查库与改界面"在同一方法，后台化后必然分离）
- 仍未后台化的同步查询只剩**支付前置校验的班次查询**（`CartController` 现金/电子支付入口、
  `TouchCartController.preCheck()`）：每次点击一次单行查询，且其后立即弹出模态框，
  改造需把三条支付流程都改成回调，风险大于收益，暂按现状保留

**触屏收银台自动化覆盖（v2.6.0 补强）**

- 触屏收银台没有 UI 级自动化测试：TestFX 需要真实显示环境，`mvn verify`/CI 跑不了
  （`LoginControllerUITest` 就被 Surefire 排除）。因此改为**把纯逻辑抽成包内可见的静态方法，
  用无界面单测直接验证生产代码**，而不是复制一份逻辑到测试里
- 抽取出的可测方法：`TouchCartController` 的 `mergeHotProducts(manualHot, topSelling, target)`、
  `applyCashPayment(received, thisPayment, finalAmount)`（返回 `CashProgress`）、
  `currentStock(Product, Map)`、`findCartItem(List<CartItem>, int)`；
  挂单编解码已进一步搬到 `HoldOrderCodec.serialize/parse`（见下节「巨型控制器拆分」）
- 覆盖见 `TouchCartControllerLogicTest`（8 项）：热销推荐按商品 ID 去重补足到 12 且不截断手动标记、
  现金分次收款累计与找零及"差一分钱不结算"边界、库存快照优先与回退、
  按商品 ID 定位购物车行（改名不影响同一行）；挂单编解码的 4 项用例随实现迁到
  `HoldOrderCodecTest`
- 这些用例做过**变异验证**：去掉去重、把付清判定 `>= 0` 改成 `> 0`、把默认数量 1 改成 0，
  对应 4 项立即变红

**巨型控制器拆分（v2.6.0 补强）**

两个收银台控制器一度各自涨到 2200 行上下（查库、结账、打印、弹窗、布局、样式全在一个类里）。
`mvn verify` 与 GitHub CI 都跑不了 UI，所以拆分只做**可机械搬运、行为不变**的部分，并逐块加门禁：

- `TouchCartController` 2267 → 1878 行，搬出三块互不相关的职责：
  - 视图构建 → `TouchCartViewFactory`（商品卡片、购物车行、分类按钮、现金弹窗控件、
    `message`），无状态静态方法，交互行为用 `Consumer` 回调注入；控制器只剩"什么时候画、点了做什么"
  - 挂单明细编解码 → `com.cashier.service.HoldOrderCodec`（`serialize` / `parse(json, dao)`），
    **标准收银台与触屏收银台共用同一实现**——此前两端各一份复制，且损坏字段处理不一致：
    标准端 `FormValidator.parseInt(value)` 会抛 `IllegalArgumentException`，一个坏字段就让整单
    挂单恢复失败；现在两端都只跳过坏行（`HoldOrderCodecTest.parseToleratesBrokenFieldAmongValidRows`）
  - 小票内容构建 → `com.cashier.printer.ReceiptBuilder` + `ReceiptData`（设置与购物车由调用方传入，
    打印线程只读不可变快照）；`ReceiptBuilderTest` 锁住小票上的合计/优惠/实收/找零/会员信息口径
- 体积棘轮门禁 `ControllerSizePolicyTest`：两个收银台控制器超过当前上限即失败
  （`TouchCartController` 1980 / `CartController` 2240），并要求视图构建留在
  `TouchCartViewFactory`（样式类不得回到控制器）。**继续拆分后应把上限一起调小**
- 仍需继续拆分（本轮未做，风险更高）：
  - `TouchCartController` 余下的大块是结算/支付流程（现金弹窗、电子支付、`completeTransaction`、
    打印调度）、商品加载与搜索、挂单对话框——它们共享 `cartItems/inventoryMap/currentMember/
    paymentInProgress` 等可变状态，抽离需要先设计协作接口，且无 UI 测试兜底，须一块一块来
  - `CartController` 2135 行**几乎全是逻辑**（视图构建只剩约 50 行），
    拆分 = 按职责抽逻辑簇（扫码/搜索流水线、支付编排、会员、挂单），风险同上
  - 共享两端 `createTransaction` 的阻碍：两端 `total_amount`/`tax` 口径不一致
    （标准端 `total_amount = 折后金额`、触屏端 `= 商品原价总额`，税额基数随之不同），
    统一前需要业务确认哪种口径权威，并考虑历史数据的处理
- 顺带发现（未修，待定）：
  - `TouchCartController` 里 `import com.cashier.model.Shift;` 已无使用方（历史遗留）
  - 默认回退包 `messages.properties` 语言不统一：`runtime.*` 是中文、部分 `tpos.*` 是英文
    （新增的 `tpos.cash.*` 跟邻居保持一致用了英文）。只有在**不支持的语言环境**下才会落到
    这个包，届时界面会中英混排；不影响 zh_CN/zh_TW/en 三种正式语言
- 已删除的死代码：
  - `TouchCartController.filterByKeyword` / `containsIgnoreCase`（关键字搜索早已改走
    `productDAO.search` 的 SQL，这两个内存过滤方法无任何调用方；
    `InventoryController` 里另有一份仍在使用，未动）
  - `PosModeView.fxml` + `PosModeController`：整套「POS 模式外壳」早已被
    `switchToPosModeView` 直接加载 `TouchCartView.fxml` 取代，没有任何 Java 代码加载该视图；
    连带删除 `CashierSystemFXApplication` 里 `instanceof PosModeController` 的清理分支，
    并修正 `TouchCartController` 类注释（原注释称触屏台"不含挂单/促销/交接班，由 PosModeView 底栏
    处理"，与该类实际能力完全相反）与 `CartViewHost` 注释
  - 新门禁 `ViewWiringPolicyTest`（双向）：视图文件必须有人加载（或登记在 `KNOWN_UNWIRED`）、
    Java 里写到的视图路径必须存在。它同时暴露出 `PasswordResetView.fxml` +
    `PasswordResetController` 也是**未接线**的——那是"首次登录强制改密"（`users.force_password_change`
    目前没有 UI 入口）的待建功能，不是死代码，已登记白名单而非删除
  - 清理连带发现（**待修**）：触屏收银台的底部状态栏只有品牌/班次/日期/时间，**没有状态文本控件**，
    也不绑定 `StatusBarManager.statusLevelProperty()`（原先唯一的绑定者在已删除的 PosModeController
    里，而它从未被加载）。所以触屏版里 `StatusBarManager.updateSuccess/updateError` 这类提示
    （如"商品加载失败"、扫码添加成功）**在界面上看不到**——只有 `warn()`/`showInfo()` 里附带弹窗的
    提示可见。修法：给 `TouchCartView` 底栏加一个状态标签并在 `TouchCartController` 里绑定级别样式
    （照 `MainController.applyStatusLevelStyle` 抄），补上后 `StatusBarSeverityPolicyTest`
    应同时断言触屏控制器

**热销榜统计口径（v2.6.0 补强）**

- 热销榜/商品销量必须按 `product_id` 关联，**不能按商品名称**：名称一旦被改名，
  `p.name = ti.product_name` 会让该商品的历史销量全部归零（排行按 0 计），
  报表侧还会把同一商品按旧名/新名分裂成两行
  - `ProductDAORefactored.findTopSellingProducts`：`ON ti.product_id = p.id
    OR (ti.product_id IS NULL AND ti.product_name = p.name)`——`product_id` 为空的旧数据
    （`batchInsert` 早期版本不写 `product_id`）才回退按名称匹配
  - `TransactionDAORefactored.getTopProducts`：子查询取 `COALESCE(p.name, ti.product_name)`，
    再把结果按名称聚合，改名后历史销量归到商品当前名称下
  - `TransactionDAORefactored.batchInsert` 现在也写 `product_id`（商品 ID 未知时写 NULL），
    新数据不再退化成只能按名称关联
  - 门禁：`ProductDAOTest.testTopSellingKeepsHistoryAfterRename`、
    `TransactionDAOTest.topProductsMergeHistoryAfterRename`（回退成按名称关联即变红）

**i18n 与启动画面（v2.6.0 补强）**

- 触屏现金支付弹窗文案**已全部走 i18n**（此前硬编码中文，触屏有语言切换按钮，
  切到 en/zh_TW 后弹窗仍是中文）：新增 `I18nKeys.Tpos` 的 7 个 `tpos.cash.*`
  （应付金额标题、精确金额、清除、确认收款、收款金额/快捷金额分节标题、部分收款提示）；
  另复用 `runtime.amount_paid` / `amount_remaining` / `payment_amount_hint` /
  `change_amount` / `invalid_amount`——这 5 个键原先只有字面量引用，本次补上了
  `I18nKeys.Runtime` 常量。四份语言包（zh_CN/zh_TW/en + 默认回退包）同步补齐。
  门禁 `TouchCashDialogI18nTest`：三种语言都有译文且**英文≠中文**（防止把中文抄进英文包）、
  带 `{0}` 的文案能正确代入、`TouchCartViewFactory` 的字符串字面量零中文、
  曾经的 13 处硬编码写法不得回归
- REST API 语言**按请求隔离**：新增 `ApiLocaleResolver`（`?locale=` → `Accept-Language` →
  当前用户偏好 → 系统语言），`I18nManager.get(Locale, key)` / `getAvailableLocales(Locale)`
  支持按指定语言取值而不改状态；`PUT /api/i18n/locale` 只写**当前用户**的语言偏好，
  不再调用 `I18nManager.setLocale`（此前任一登录用户切语言会把桌面端与所有终端一起改掉），
  不支持的语言返回 400。测试见 `I18nApiControllerTest`
- 启动画面改为轻量窗口 `SplashWindow`（普通 `Stage`）：不再依赖 JavaFX `Preloader`——
  Preloader 要求主类必须是 `Application` 子类，与可执行 JAR 的 `Launcher` 入口冲突，
  恢复它需改用内部 API `LauncherImpl` 并给启动脚本加 `--add-exports`。原 `SplashScreen`
  与全部 `notifyPreloader` 调用、manifest 的 `JavaFX-Preloader-Class` 已移除；
  `CashierSystemFXApplication.start()` 先显示 `SplashWindow`，延后 60ms 再跑重量级初始化
  （`initializeApplication`），失败时弹窗提示并退出
- 技术债登记在 `docs/TECH_DEBT.md`：TD-001 发票文件路径白名单校验（阻塞于「发票预览/下载」需求）

**数据库密码来源（v2.6.0 补强）**

- 新增 `com.cashier.util.DotEnv`：读取/维护工作目录 `.env`（忽略注释、去引号、重复键取首条、
  写入时保留其它键并把权限收到 `rw-------`）。`DatabaseManager.resolveDatabasePassword` 与
  `DatabaseConnectionHelper.loadConnectionConfig` 改为"环境变量 → `.env` → 配置文件"
- `DatabaseConfigDialog` 与 `Installer.createConfigFiles` **不再把密码写进
  `config/database.properties`**（任何模式都留空，开发模式改写 `.env`），
  既满足 `release.bat`/`release.sh` 的 `db.password` 门禁，也让 dev/prod 不再分叉；
  回归门禁见 `PerformancePolicyTest.installersNeverPersistPasswordIntoConfig`
- 本机约定：密码放根目录 `.env`（已 gitignore），`config/database.properties` 的 `db.password` 留空

**REST API 启用步骤（本地冒烟/生产）**

1. `config/api.properties` 已生成就绪版本（随机 `token.secret`，`api.enabled=false`，gitignored 不入库）
2. 本地冒烟：`api.enabled=true` + `cors.allowed.origins=*`（仅本机临时）后重启，验证 `/api/health` 与 `/api/auth/login`
3. 生产：必须用环境变量 `TOKEN_SECRET`（≥64 字符）覆盖、`CORS_ALLOWED_ORIGINS` 限制具体域名、`api.host` 收紧到受信网段
4. 生产禁用时应用会打印 `REST API 服务器已禁用` 并拒绝全部 API 请求（安全默认）

## Known Issues

### PDF 中文字体：已内置 TrueType 字体，不再依赖系统字体

PDFBox 只能嵌入 **TrueType(glyf)** 字体，而 JavaFX 用的字体可以是 OTF/CFF：

- `src/main/resources/fonts/NotoSansSC-Regular.ttc` / `-Bold.ttc` 实际是 **Noto Sans CJK 的
  OTF/CFF 版本**（子字体名形如 `NotoSansCJKsc-Regular`），没有 `glyf` 表：
  整体嵌入抛 `IOException: Full embedding of TrueType font collections not supported`，
  子集化在 `PDDocument.save()` 时抛 `UnsupportedOperationException: OTF fonts do not have a glyf table`。
  **它们只能给 JavaFX 界面用，不能用于 PDF 导出**。
- **已内置** `src/main/resources/fonts/NotoSansSC-Regular.ttf`（约 10MB，TrueType，
  SIL OFL 1.1，许可证见同目录 `OFL-NotoSansSC.txt`），PDF 导出不再依赖系统字体，
  Linux（含 CI 的 Ubuntu runner）无中文字体时也能正常导出。CI 已不再安装系统中文字体，
  这同时构成"无系统字体仍可用"的回归验证。
- 该 `.ttf` 由 Google Fonts 的**变量字体** `NotoSansSC[wght].ttf` 固化而来：
  其 `fvar` 的 `wght` **默认值是 100（Thin）**，必须用
  `fonttools varLib.instancer ... wght=400` 生成静态 Regular；来源与复现命令见
  `src/main/resources/fonts/README.md`。
- 代码侧加固（`ExportUtil`）：候选顺序为 系统字体 → 文件系统 → 项目资源，其中 `.ttf` 排在
  `.ttc` 之前；`isEmbeddable()` 跳过没有 `glyf` 表的候选；`canRender()` 跳过缺少
  数字/中文覆盖的候选（`Droid Sans Fallback` 只有 CJK 字形，渲染数字会抛
  `No glyph for U+0031`）。
- **替换字体时**：必须是 TrueType(glyf)、覆盖数字与中文、许可证允许嵌入与再分发，
  并同步更新 `fonts/README.md` 与许可证文件。

### JavaFX Class Loading Warning (macOS + Homebrew JDK)
When running tests on macOS with Homebrew's OpenJDK 17, you may see:
```
objc[XXXXX]: Class ButtonAccessibility is implemented in both
/opt/homebrew/Cellar/openjdk@17/.../libawt_lwawt.dylib and
/Users/nevell/.openjfx/cache/17.0.12/libglass.dylib
```
This is a platform-specific warning caused by Homebrew JDK bundling JavaFX native libraries alongside OpenJFX. It does not affect functionality or test results. No fix is required - this warning does not appear in production (packaged) builds.
