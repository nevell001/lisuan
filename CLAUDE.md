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

**历史遗留表（2026-09 澄清）**
- `specifications` / `specification_values` / `product_specifications`（v2.4.4 规格）、
  `export_history` / `export_templates`（v2.4.0 导出）：**当前 Java 侧已无任何代码引用**
  （规格功能下线、`SpecificationDAORefactored` 已删除；导出直接写文件）。这 5 张表只存在于
  初始化脚本里，保留仅为兼容仍在使用它们的旧库/BI 取数；新装部署不需要。
  它们登记在 `InitSchemaParityTest.LEGACY_TABLES` 白名单里——**新增"Java 侧不建的表"必须显式登记**，
  否则门禁会失败

**Initialization:**
- Full init: `docker/mysql-init/00-init-complete.sql`（表/列/类型与 Java 建表一致，
  由 `InitSchemaParityTest` 守着；DDL 取自 `DatabaseManager` 与各 DAO 的 `createTable()`，勿手改）
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
- 导出直接写文件（`ExportUtil`），**不再写 `export_history` 表**——该表已无引用，见上"历史遗留表"

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
- **Windows 批处理脚本门禁**（TD-011）：`WindowsScriptPolicyTest` 钉住四条不变量——
  ① 任何 `.bat` 不得用 `for %%x in (<通配符>)` 判断文件存在（cmd 在无匹配时会把通配符**原样当成一项**，
  于是"没找到"的分支永远不可达；本机**无法执行也无法语法检查** `.bat`，只能靠静态规则挡）；
  正确写法是 `for /f "delims=" %%f in ('dir /b /o-d "<模式>" 2^>nul') do ...` + `if not defined VAR`；
  ② 查 JAR 的脚本必须用 `dir /b` 检测且 `java` 命令行不得带通配符；
  ③ `start.bat` **不得自动跑 maven**（产品决定：缺 JAR 就报错提示用户自己构建并 `exit /b 1`），
  而 `install.bat` 的显式构建后必须校验 fat JAR 真的产出；
  ④ 跟踪的 `.bat` 工作区必须是 **CRLF**（`.gitattributes` 的 `eol=crlf` 只在 checkout 生效，
  被工具写过 LF 后 `git status` 看不出来，但 Windows 上 `goto`/`if` 块会直接解析失败；
  修复用 `git checkout -- <文件>`）。改 `.bat` 时记得 `install.bat` 里**生成的** `DataConfig.bat` heredoc
  要同步改（门禁会比对两处写法）；
  ⑤ **含非 ASCII 字节的 `.bat` 必须自己切代码页**（行首 `chcp 65001`）——cmd 用当前 OEM 代码页读批处理，
  中文 Windows 是 936/GBK，UTF-8 中文会变乱码并**让字节错位**，把 `REM` 后的空格或 `^(` 的 `^` 吞掉，
  于是注释被当成命令执行（`不是内部或外部命令`）或块解析失败（`此时不应有`）。
  2026-09 已在 Windows 上实测撞到：我给 `start.bat`/`install.bat` 加的中文 `REM` 注释直接把
  `install.bat` 跑崩。反过来，**给纯 ASCII 的批处理加 `chcp 65001` 也会影响同一窗口的后续命令**：
  `start.bat` 为让 JVM 的 UTF-8 中文日志可读而切到 65001，结果同窗口接着跑 maven 时 javac 的 GBK
  输出变乱码——所以它**只包住 `java` 调用**（启动前记住原代码页、返回后 `:restore_code_page` 恢复，
  并用 `findstr /r "^[0-9][0-9]*$"` 校验解析结果），门禁第 9 条钉住这个配对。**结论：这几个脚本的注释一律写英文**（`start.bat`/`install.bat`/`DataConfig.bat`/
  `create-shortcut.bat`/`diagnose.bat` 现为纯 ASCII；`release.bat`/`docker/start-mysql.bat` 是中文文案 +
  `chcp 65001` 的既有做法，不要再往里加需要转义的 `^(` 之类）
- 安装/运维脚本门禁：`InstallScriptPolicyTest`（`com.cashier.security`）钉住三条不变量——
  ① `install.sh` 的建库/SQL 导入失败必须报错并 `exit 1`（不得再用 `2>/dev/null || true` 静默成功，
  失败时要打印 mysql 的真实输出）；② `docker/docker-init.sh` 必须先 `. ./.env`，且空/占位口令
  （`YOUR_*_HERE`/`changeme`/空值等）**硬失败**并早于第一条 `ALTER USER`（不再提供"确认后继续"）；
  ③ 容器名只能来自 `MYSQL_CONTAINER_NAME`（`docker/start-mysql.sh` 不得硬编码 `lisuan-mysql`）；
  另外 `.env.example`/凭证清单不得出现无人实现的 `DB_USE_SSL`（SSL 由 `db.url` 的 `sslMode` 决定）
- 版本号四处同步（`AppConstants`/`pom.xml`/`installer/Installer.java`/`.env.example`）当前均为 `2.6.0`；
  **不变量由 JUnit 门禁守着**（`com.cashier.constant.VersionConsistencyTest`，随 `mvn verify` 在 CI 跑）：
  四处必须相等，失败时报出四处实际值；同时断言 `release.sh`/`release.bat` 的**比对清单**确实包含
  Installer 与 `.env.example`（只"提到"来源不算）。两个发布脚本的检查段已同步扩到四处
- 状态/支付方式归一化的大小写折叠一律用 `Locale.ROOT`（`I18nUiUtils`）：平台默认 locale 在土耳其语下
  会把 `CHECKING` 折成 `checkıng`、`CASH` 折成 `cash` 之外的值，导致落库值匹配失败、界面回退成英文原值；
  门禁 `I18nUiUtilsTest.caseFoldingIsLocaleIndependent`（切 `tr_TR` 默认 locale 后断言，改回即变红）
- **金额/百分比格式化必须显式固定 `Locale`**（TD-015）：`String.format("%.2f", x)` 跟随平台默认
  locale，德语/法语机器会打出 `1,50`——小票对不上账、CSS `rgba(...,0,50)` 被 JavaFX 静默丢弃；
  统一用 `String.format(java.util.Locale.ROOT, ...)`（纯 `%d`/`%s` 不受影响，无需强制）。
  门禁：`LocaleFormatPolicyTest`（扫 `src/main`，**按引号配对解析第一个实参**，常量格式串如
  `PERCENT_FORMAT = "%.2f%%"` 也会跨文件解析）+ `LocaleIndependenceBehaviorTest`
  （把默认 locale 切成 `de_DE` 再走金额/CSS 真实代码路径）。另建议整套测试在德语 locale 下跑一遍：
  `mvn test -DargLine="-Duser.language=de -Duser.country=DE"`
- **界面可见文案不得硬编码**（TD-014）：`HardcodedUiTextPolicyTest` 守住已迁移的文件——
  `setTitle/setContentText/setText/setPromptText/show*Alert/showPlaceholder` 与 `new Label/Button("中文")`
  不得含中文（**日志与注释里的中文不算**）；FXML 的 `text/promptText/title` 必须是 `%key`
  （运行时会被覆盖的设计期占位走白名单，且白名单每一条都要注明覆盖它的代码位置）；
  `updateStatus`/`updateWarning` 也在守卫范围内（现已覆盖 13 个文件，全仓库状态栏硬编码为 0）——
  状态栏文案应**复用** `status_message.*`/`nav.*` 等既有 key，别新建同值 key。
  迁移新文件时把路径加进该测试的 `MIGRATED_FILES`。
  **写 i18n 调用时要顺着门禁的形状写**：`I18nManager.get(...)` 的首参必须是字面量或常量
  （`get(cond ? A : B, x)` 会让 `I18nBundleConsistencyTest` 把条件里的字面量当成 key）；
  用户可见文案也可能不在 `updateStatus` 里——`showError`/`showWarning` 同样是文案出口，
  按调用形式筛会漏（第四批就这样漏过一处）。
  注意 `RechargeController` 的支付方式：**下拉项仍需保留规范中文落库值**（TD-002），
  只能通过 `StringConverter` 本地化显示层
- `I18nKeys.java` 里有**嵌套类**（`Menu.Help`、`Menu.Theme`、`Nav`、`Runtime`、`StatusMessage`…）：
  手工或用脚本改这个文件时**不要**"把块内常量行排序后重排"——那种写法会把嵌套类内的常量压平到外层，
  编译期才会以 `找不到符号` 暴露。新增常量要么严格落在所属嵌套类内，要么按类整体替换
- **界面集合状态字段必须"声明即初始化"**（TD-016）：控制器里的 `List/Map/Set/ObservableList/...` 字段
  一律在声明处 `= new ArrayList<>()` / `= FXCollections.observableArrayList()`。
  原因：页面打开时多个**异步加载器互相依赖**（如"加载分类后默认选中第一项"会触发监听器 →
  读商品列表），任一个先回来就会读另一个尚未赋值的字段 → Windows 实测抛过
  `NPE: inventoryList is null`（`InventoryController.updateCountLabel`）与 `orders is null`
  （`PurchaseOrderController.filterOrders`）。声明即初始化后"数据没到"表现为空集合，顺序不再敏感。
  门禁 `UiStateInitializationPolicyTest`（2 项：全量扫描 + 崩溃字段回归锚点）。
  注意：FXML 注入的控件（`TableView`/`ListView`）不是集合字段，不在规则内
- i18n 门禁：`I18nBundleConsistencyTest` 断言三份语言包 key 集合一致、`I18nKeys` 常量齐全、
  源码字面量 i18n 调用 key 齐全（缺 key 时界面会直接显示 key，属 UI 缺陷）；
  另有 `I18nUnusedKeyPolicyTest`（2 项）：**语言包不得残留无人引用的 key**，
  且 `I18nKeys`/`I18n` 两套常量的值都必须存在于语言包。清理/新增 key 时注意：
  判定"在用"的口径是"精确字面量 / 常量引用 / FXML `%key` / `"前缀." + 变量` 拼接"，
  **动态拼接的 key 必须写成 `"前缀." + 变量` 形式**，否则会被判为无用；
  两套常量持有者都要算上（`/api/i18n/messages/all` 用的是 `I18n`）。
  历史坑：静态导入 `I18nKeys.*` 后短名引用会让门禁解析不了——它现在会显式报"门禁需要更新"。
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
  计算（税率是小数 0.0-1.0，不得再除以 100）、三处结账路径的 `total_amount` 都是明细原价合计、
  税额基数都是实付金额、触屏收银台必须计算并落库促销、支付方式筛选
  必须归一化后再比较；`TransactionServiceTest` 覆盖税率小数语义、促销按原价总额计算、
  `selectBestPromotion` 选优；`I18nUiUtilsTest` 覆盖中文/代码支付方式归一化
- **会员「金额类字段」权限**（TD-017）：等级/折扣/**积分**/**余额** 只允许 admin/finance 修改，
  **两侧同改**——API 早就由 `AuthorizationMiddleware`（`/api/members/[^/]+` + 写方法）限死，
  桌面端由 `MemberEditController.setSensitiveFieldsEditable(...)` 按角色禁用（角色判定在
  `MemberController.showEditDialog`，取不到用户时 fail-closed）。两个易错点：
  ① 等级与折扣在保存时**由积分推导**（`MemberService.calculateLevel(points)`），
  只禁用等级/折扣两个控件挡不住改折扣，积分必须一起锁，且保存时把值**还原**（禁用不阻止程序化赋值）；
  ② 余额是储值金额（等同发钱），与 API 把 `recharge` 限 finance/admin 的口径一致。
  等级/折扣变更必须写 `MEMBER/MEMBER_LEVEL_DISCOUNT_UPDATED` 审计日志（此前会员路径**完全没有**留痕）。
  **充值相反**：`POST /api/members/{id}/recharge` 不限角色（2026-09 TD-018 决定：充值是收银台日常操作，
  桌面端一直开放，且写 `RechargeRecord` 流水留痕）；但"改折扣/等级/积分/余额"仍限 finance/admin。
  门禁 `MemberPermissionPolicyTest`（3 项，含直接调 `AuthorizationMiddleware.isAllowed` 的行为级断言）。
  **教训**：判断某接口的角色限制要看**中间件与路由挂载**，不能只看控制器方法体——
  本条最初的结论（"API 不区分角色"）就是这么写错的。
- **状态栏文案门禁是全仓库、表达式感知的**（TD-014）：`statusBarTextIsNeverHardcodedRepoWide` 用
  **平衡括号取实参 + 跳过注释 + 只看字符串字面量**判定，因此续行/三元/拼接都逃不掉；
  单行 grep 会漏（我据此误报过"全仓库 0 处"）。历史坑：`StatusBarManager` 有一张
  `LEGACY_STATUS_KEYS`/`PREFIXED_STATUS_KEYS` 兼容映射会把中文串兜底翻译成 key——
  它让硬编码**在运行时被掩盖**，所以调用方一律直接传 key。另：断言"某操作使用成功级别"要按
  **文案 key** 判级别（测试助手 `StatusBarAssertions`），不要钉中文文案，否则文案迁 i18n 时门禁会误红
- **i18n 占位符必须传参**（TD-019，用户实测发现）：`get(key)` 漏传参会让界面直接显示 `{0}`
  （实测：商品管理页 `商品数量: {0}: 94/94`）。门禁 `I18nPlaceholderArgsPolicyTest` 扫全部
  `get("<key>"…)`/`get(I18nKeys.X…)` 调用，用语言包值里的最大占位符序号对比**实参个数（不含 key）**；
  防空转要求识别到的调用数 > 1000。写这类门禁注意：统计实参时**别把 key 算进去**——
  第一版就这么错了，`get(key)` 恰好通过，是变异测试抓出来的
- **深色主题的大面积用色要降饱和**（TD-020，用户实测发现）：品牌色铺在大面积背景上，
  浅色主题好看，深色主题会变成刺眼的亮条。已修：交班页 `shift-toolbar` 深色下用
  `-lisuan-primary-darker`（原来被覆盖成饱和橙 `-lisuan-primary-muted`），
  `shift-separator` 深色下必须覆盖为中性色（基础样式用的是品牌边框色）。
  门禁 `DarkThemeSurfacePolicyTest` 钉住这两条。**改自定义 surface 样式类时，
  记得给深色主题补覆盖规则**——共享类（如 `toolbar`）有覆盖，自建类容易漏
- **说明文档一致性**（`InstructionsDocPolicyTest`）：`AGENTS.md`（速查，已入库）与 `CLAUDE.md`（权威细节）
  都必须被 git 跟踪；两份文档里**讲"版本号四处同步"的那一句**必须列全
  `AppConstants`/`pom.xml`/`installer/Installer.java`/`.env.example`；
  两份"当前事实"文档**不得写死测试数量**（改版本/加测试时最容易漂移，一律指向构建输出的 `Tests run:`；
  README 的历史更新日志不在此限）。改版本号时除了四处代码来源，记得同步这两份文档里的同步句
- **发票文件路径（TD-001）**：`invoices.pdf_path` / `image_path` 目前**故意不校验**——全仓库没有任何地方
  把它们当文件路径读取（只由 `POST /api/invoices/{id}/print` 原样落库），此时定白名单属于为不存在的能力
  做防御，且合法路径范围取决于尚未确定的发票预览/下载设计。`InvoicePathGuardPolicyTest` 把这个
  "触发条件"变成了 CI 红灯：一旦有人把这两个字段当文件系统路径解引用（读写都算）而没有同批实现校验，
  测试立即失败，并要求把校验过的站点登记进 `VALIDATED_READ_SITES`（目前为空，实现时登记一行）。
  已知边界：解引用匹配按"同一行 ±3 行"，**间接用法（先存局部变量、后方再打开）抓不到**，靠 review
- 启动入口：可执行 JAR 的 Main-Class 为 `com.cashier.Launcher`（不继承 `Application`），
  使 `java -jar lisuan-fx-*-jar-with-dependencies.jar` 无需 module-path 即可启动

**结账字段口径统一（v2.6.0 补强）**

三处结账路径（标准收银台 `CartController.createTransaction`、触屏收银台
`TouchCartController.createTransaction`、`TransactionApiController`）此前字段口径不一致：
API 与触屏台写 `total_amount = 明细原价合计`，**标准端写折后金额**——于是标准端
`total_amount - final_amount` 恒为 0，小票"商品总额"与"实付金额"永远相等，看不出优惠。

- 现已统一：`total_amount` = `TransactionService.calculateTotalAmount(明细)`（原价合计）、
  `final_amount` = 实付（含会员折扣与促销）、**`tax` = `calculateTax(final_amount)`（按实付计，业务确认）**。
  选原价合计作 `total_amount` 的原因：API 与触屏台已是如此（2/3 路径 + 对外 API 契约），
  且只有这样才能从 `total_amount - final_amount` 算出优惠；选实付作税额基数的原因：
  税是**价内税**（`calculateFinalAmount` 不含税、不参与应付金额计算，只在记录/小票上展示），
  顾客实付多少就按多少计税，打折后税额同步下降
- 税额基数四处一致：`CartController` / `TouchCartController` / `TransactionApiController` /
  `TransactionService` 私有建单路径（旧的向后兼容入口仍在用）。注意三处真实结账走的
  `executeTransaction(cartItems, member, transaction, inventory, promotion)` **不重算 tax**，
  控制器写的值就是落库值
- 小票的会员信息必须用**结账前快照**：`TransactionService.applyMemberInTransaction` 会就地改写调用方持有的
  `Member`（等级/折扣/积分/余额都变），结账后再读 `currentMember.level` 会把"本单成交后升级的等级"
  印在小票上（冒烟实测：9 折普通会员买 180 元得 1800 分、当场升银卡，旧实现的小票会印"银卡"）。
  现在 `ReceiptBuilder.build(...)` 只接受 `ReceiptBuilder.MemberSnapshot`（`MemberSnapshot.of(member)`
  须在 `executeTransaction` 之前调用），从类型上杜绝传错对象
- 销售小票两条路径都已接通并修正：
  - **触屏收银台**：`ReceiptBuilder`（结账前会员快照）→ `PrintUtil` 打印，会员等级取快照
  - **标准收银台**：此前**完全没有打印入口**（只有触屏版打小票），现在结账成功后由
    `CartController.printReceiptInBackground` 在 daemon 串行线程打印，受 `enablePrint` 控制，
    配了 `printerName` 走网络打印机（ESC/POS），否则用系统默认打印机；打印失败只记日志，不影响已成交订单
  - `ReceiptPrinter` 的销售模板（`printReceipt` / `generateReceiptOnly` / `printReceiptWithPrinter`）
    随之修正三处：① `收银员: 系统` 是硬编码 → 改为取本单落库的 `operatorName`（回退
    `operatorUsername`，再回退"未知"）；② `会员折扣` 读活 `member.getDiscount()`（会被结账就地改写）
    → 改为显式传入**结账前**折扣，传 null 时**不打印该行**（宁可少一行也不印错）；③ `积分: %d`
    配 `member.points`（BigDecimal）会抛 `IllegalFormatConversionException` → 改为 `%s`
- 配套：小票必须打印优惠行，否则打折单上"商品总额 ≠ 实付金额"无从解释——
  `ReceiptPrinter` 两个模板都加了 `优惠:` 行（`total_amount > final_amount` 时才打，
  以负数呈现，无优惠不出现该行）；交易详情弹窗同样补了一行（复用既有 key
  `cart.discount_amount`，未新增文案）
- **无需数据迁移**：目前没有门店库（无历史数据），本机开发库 35 笔全部 `total_amount == final_amount`
  （从未使用会员折扣/促销），改动前后这些行的值完全相同
- 门禁：
  - `CheckoutConsistencyPolicyTest.allCheckoutPathsUseOriginalTotal`：三处都必须
    `totalAmount = calculateTotalAmount(...)`、税额基数必须是 `calculateTax(<finalAmount>)`，
    不得出现 `totalAmount = getFinalAmount()/getPayableAmount()`，也不得再按原价合计计税
  - `TransactionApiControllerTest.createStoresOriginalTotalAndTaxesPayableAmount`（**行为级、走真实 API**）：
    税率 6% + 会员 9 折下单 2×100 → 落库 `total_amount = 200.00`、`final_amount = 180.00`、
    `tax = 10.80`（按原价会得到 12.00）
  - `ReceiptBuilderTest`（4 项，行为级）：小票字段口径 + **会员等级取结账前快照**
    （把 member 改成银卡 9.5 折后，小票仍须显示成交时的"普通"）
  - `CheckoutConsistencyPolicyTest.touchReceiptSnapshotsMemberBeforeCheckout`：断言快照出现在
    `executeTransaction` **之前**，且小票不得直接传 `currentMember`
  - `ReceiptPrinterTest`（7 项，行为级：打折单打优惠行且金额自洽、无优惠不打、金额缺失不抛异常、
    收银员取本单操作员并回退用户名/未知、会员折扣按结账前值打印、折扣缺失时不打印该行）
  - `CheckoutConsistencyPolicyTest.standardPosPrintsReceiptWithSaleTimeSnapshot`：标准收银台必须提交
    打印任务且调用 `ReceiptPrinter`、折扣快照必须在 `executeTransaction` **之前**取；
    小票模板不得再出现 `收银员: 系统` 与 `积分: %d`
  - 均做过变异验证：把标准端改回 `getFinalAmount()`、删掉小票优惠行、把税额基数改回原价 →
    对应门禁/行为测试各自变红

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

**结账/库存正确性第二批（2026-09 全量审计修复）**

审计方式：`mvn verify` 三关全绿的前提下逐条回读代码，路由匹配用**真实 Javalin 6.1.3 最小复现**确认
（不是读文档推断）。修掉的项：

- **字面量路由被 `{id}` 遮蔽**：`GET /api/products/low-stock`、`GET /api/invoices/seller-info` 注册在
  `/{id}` 之后 → 前者被当成 `id="low-stock"`（parseInt 失败回 500），后者恒回 404「发票不存在」。
  实测最小复现证明 Javalin **取第一个匹配的路由**（`/api/transactions/today` 已修是因为只改了那一处）。
  门禁 `ApiServerTest.noLiteralRouteIsShadowedByEarlierPatternRoute`：**通用扫描**全部路由注册，
  任何"同方法同段数的字面量路径注册在占位路由之后"即失败（不再只盯 transactions 一对）。
  注意 `AuthorizationMiddlewareTest` 曾断言这个不可达端点"允许"，是假绿灯
- **商品/库存 API 丢弃乐观锁返回值**：`productDAO.update(product)` 在 `version` 未命中时**返回 false
  而不抛异常**，两个接口原来直接回 `success:true` → 并发写入静默丢失。现在回 **409**；
  库存接口同时拒绝负库存（400）。门禁 `ProductUpdateConflictPolicyTest`（源码门禁：控制器必须消费
  该布尔；DAO 契约由 `ProductDAORefactoredTest.testOptimisticLock` 覆盖——单请求拿不到陈旧 version，
  冲突只在"读到写之间"的窗口，TestContext 无法确定性触发）
- **裸毫秒单号**：标准收银台 `"ORD" + 毫秒`、REST API `"T" + 毫秒`（只有触屏台用了加固版）；
  `transaction_id` 是主键 → 同毫秒/跨终端碰撞会让整单回滚。统一为
  `TransactionService.generateTransactionId(prefix)`（毫秒+4 位序号+8 位随机段），
  `generateOrderNumber()` 委托它。门禁 `OrderNumberGenerationTest`（格式 + 5000 次不重复 + 两条路径不得回归裸毫秒）
- **支付单主键裸毫秒**：`payment_orders.payment_id` 原来是 `"PAY" + System.currentTimeMillis()`
  （随机段只加在了 `merchant_order_no` 上）→ 同毫秒两笔支付单主键冲突、订单丢失。改为
  `PaymentOrder.generatePaymentId()`（毫秒+4 位序号+6 位随机段），`createScanPayOrder` 与 DAO 兜底都走它。
  门禁 `PaymentOrderIdTest` + `PaymentDAOTest.insertGeneratesHardenedPaymentIdWhenMissing`
- **盘点完成非原子**：原来"先 UPDATE status='completed'，再逐条用各自 autocommit 连接改库存、返回值全丢"
  → 中途失败留下「已完成但库存只改一半」且不可重做（`canComplete` 要求 `checking`），重复点击还会二次累加。
  现在 `completeWithConnection` 带 `AND status <> 'completed'` 守卫，状态迁移 + 逐条
  `updateQuantityWithConnection` 在**同一事务**内。门禁 `InventoryCheckCompletionPolicyTest` +
  `InventoryCheckDAOTest.completeIsGuardedAgainstRepeatedCompletion`
- **会员对象回滚污染**：`executeTransaction` 在事务内就地改写调用方持有的 `Member`（等级/折扣/积分/余额），
  事务后续步骤失败回滚后该对象仍是"升级后"的状态 → 收银台用 `getFinalAmount()` 重算会少收钱。
  现在结算在 `copyMember` 副本上进行、**提交成功后才 `copyMemberFields` 写回**（库存早已用
  `updatedProducts` 的同一思路）。门禁 `TransactionServiceTest.failedTransactionKeepsCallerMemberUntouched`
  （同号交易制造主键冲突，断言等级/折扣/积分/余额/版本全不变）
- **退款 `APPLYING` 预占泄漏**：渠道退款成功后要写两处状态，原来是裸调用、且只在
  `catch (RuntimeException)` 里释放预占 → 落库抛 `SQLException` 时预占永久留在库里、被
  `sumRefundedAmount` 计入已退（额度永久减少，界面永远显示"申请中"）。现在
  `settleRefund` 把两处状态迁移放进一个事务并重试一次，仍失败则留下含
  `refundId/paymentId/channelRefundNo` 的 ERROR 日志供人工核对
- 上述 6 处门禁**全部做过变异验证**：把路由顺序改回、`if (!update(...))` 改成 `if (false)`、
  标准端改回裸毫秒、DAO 兜底改回裸毫秒、控制器改回 autocommit 逐条改库存 + 去掉状态守卫、
  取消会员副本 → 对应测试各自变红（共 7 处变异，逐个确认后还原）
- 未修项（测试库无外键、脚本/文档漂移、FX 线程、API 校验等）登记在
  `docs/TECH_DEBT.md`（TD-004、TD-006 ~ TD-014）；TD-002 / TD-003 / TD-005 已在下一批修复

**支付方式口径与营业额净额（2026-09 审计修复，第三批）**

- **支付方式落库前归一化**（TD-002）：新增 `I18nUiUtils.storedPaymentMethod(value)`
  （中文/繁体/英文/代码 → 规范中文落库值：现金/微信/支付宝/银行卡/会员余额），
  `TransactionApiController.create` 归一化后才写库，无法识别的写法回 **400**
  （一并消除"任意字符串落库"与"超 20 字符 → 500"）。聚合侧全部改为按归一化代码处理：
  `getPaymentMethodStats` 合并同一方式的多种写法（标签优先用规范中文值，已有输出不变）、
  `ReportApiController.dailySales`、`StatisticsController`、`ShiftController.categorizeShiftRevenue`
  按归一化分桶，`getStatistics` 现金笔数兼容 `'现金','現金','CASH','Cash'`
- **营业额净额口径**（TD-003，已定稿）：**销售额** = 区间内未整单退款
  （`COALESCE(status,'NORMAL') <> 'REFUNDED'`）交易的实付合计；**退款额** = 区间内已按
  `completed_date` 完成、且原交易未被整单标记 REFUNDED 的退货单金额（避免与 REFUNDED 重复扣减）；
  **净额** = 两者之差；**分渠道桶**先按归一化方式对有效销售分桶，再按**退款方式**冲减对应渠道
  （现金退款减现金桶，退回余额只影响总额，允许为负）
  - `TransactionDAORefactored` 的 `getStatistics` / `getTotalRevenue` / `getTransactionCount` /
    `getPaymentMethodStats` 全部剔除 REFUNDED；
  - 新增 `ReturnOrderDAORefactored.findCompletedReturnsBetween(start, end)`（含 NOT EXISTS 去重）
    与纯函数 `TransactionService.returnsByMethod / returnsByDay / totalReturns`；
  - 消费方：日报/月报新增 `refundedAmount` / `netAmount`（月报日趋势按退货完成日冲减，
    Σ日趋势 == netAmount）、桌面统计、交班（净额 + 现金桶扣现金退款）、利润报表（已退款不计利润/毛利）；
  - `TransactionApiController.createRefundReturnOrder` 补写 `completedDate`（此前 API 退款写的退货单
    `completed_date` 为 NULL）
- **门禁**（7 处，全部做过变异验证）：
  `TransactionApiControllerTest.createNormalizesPaymentMethodForStorage`、
  `TransactionDAOTest.paymentMethodStatsAndCashCountAreNormalized`、
  `TransactionDAOTest.refundedTransactionsAreExcludedFromRevenueAggregates`、
  `ReportApiControllerTest.dailySalesBucketsPaymentMethodAfterNormalization`、
  `ReportApiControllerTest.dailySalesIsNetOfRefundedTransactionsAndCompletedReturns`、
  `ReportApiControllerTest.monthlySalesNetMatchesDailyTrend`、
  `TransactionServiceTest.returnsAreGroupedByNormalizedMethodAndCompletionDay`；
  `ProfitReportController` / `StatisticsController` / `ShiftController` 三处为 UI 控制器，
  本批只做源码级改动（无 UI 自动化测试兜底），口径由上述 DAO/纯函数门禁锁住
- 有意保留：销量/库龄类口径（热销榜 `getTopProducts`、周转率）仍按"卖出去多少"统计，不剔除已退款交易

**REST API 加固第二批（2026-09 审计修复，第四批）**

- **会员更新校验对齐桌面端**（`MemberApiController.update`）：姓名非空白、手机号 11 位数字、
  等级必须是 `普通/银卡/金卡/钻石`（新增 `MemberService.isKnownLevel`）、折扣 0..10，非法值回 **400**
  （此前可写 `discount=999`/负数/任意等级，且会因列长度变成 500）；同时补 `discountRate = discount`
  （桌面端两边都写，接口只改一个）
- **会员乐观锁回 409**：新增 `MemberDAORefactored.OptimisticLockException extends SQLException`
  （既有按 SQLException 捕获的调用方不变），接口单独捕获后回 409，不再把并发冲突伪装成 500
- **`POST /api/payment/create` 金额交叉校验**：交易必须存在（**404**）、不得为 `REFUNDED`（400）、
  `amount` 必须等于该交易 `finalAmount`（400）。此前只校验 `amount > 0`，认证用户可用 0.01 元
  为 1000 元的单生成真实收款码。⚠️ **契约变更**：调用方必须先 `POST /api/transactions` 落一笔交易，
  再按其实付金额建支付单；桌面两个收银台直接调 `PaymentService`，不走该端点，不受影响
- **操作员一律取认证用户**：`PaymentApiController` / `BackupApiController` 改为
  `ctx.attribute("currentUser")`（忽略请求体 `operator`）；`PaymentService.createPaymentOrder`
  增加 `operator` 参数并**在 insert 之前**赋值——此前接口在 insert 之后赋值，
  `payment_orders.operator` 恒为 NULL，而备份归属可被请求体伪造
- 门禁（5 处变异验证全红）：`MemberApiControllerTest.updateMemberValidatesFieldsLikeDesktop`、
  `MemberDAOTest.staleVersionThrowsTypedOptimisticLockException`、
  `PaymentApiControllerTest.createPaymentValidatesTransactionAndAmount` 与
  `createPaymentInMockModeSucceeds`（落库 operator = 认证用户）、
  `OperatorIdentityPolicyTest`（三个写审计归属的控制器必须取认证上下文 + 支付单操作员早于 insert +
  会员冲突映射 409）
- 未纳入本轮：`POST /api/members` 仍允许 cashier 调用，"改等级/折扣是否需要更高角色"属产品决定，
  已记在 `docs/TECH_DEBT.md` 的补充条目

**测试库外键与审计归属（2026-09 审计修复，第五批）**

测试用的 H2 schema 此前**一个外键都没有**（生产有 16 个），外键类缺陷在 CI 里完全不可见。
本批把测试库与生产对齐，并顺手修掉它暴露的问题：

- **审计归属列不再挂外键**：`transactions.operator_username` 与 `operation_logs.username`
  改为纯文本归属（老库由新增的 `DatabaseManager.dropAuditAttributionForeignKeys` 幂等删除，已加
  `docker/mysql-init` 同步）。理由两条：`ON DELETE SET NULL` 会在删用户时把历史交易/审计日志的
  操作人**抹成 NULL**（改名同样不该改写历史）；且 `operation_logs.username` 在生产里有若干处
  直接写**显示名**（`ReturnService.approveReturnOrder/completeReturnOrder`、现金退款日志），
  挂外键会让这些写入**直接失败**——这正是测试库无外键时长期被隐藏的一类缺陷。
  `transactions.member_phone` 外键保留（会员注销后本单不再归属会员，语义可接受）
- **测试库补齐其余 14 个外键**：`DatabaseTestBase.TEST_SCHEMA_FOREIGN_KEYS` +
  `createTestForeignKeys(stmt)`（建表后 `ALTER TABLE` 添加，按 `INFORMATION_SCHEMA` 判重）；
  清库顺序本就是从依赖倒序删除，无需调整
- **修掉外键暴露的 10 处测试悬空引用**（父行不存在却插子行）：盘点/采购审批/采购订单与明细/
  入库明细/退货明细各测试补父行；`PurchaseServiceTest` 里"用不存在的商品 ID 制造失败"改为
  真实的业务失败条件（入库数量超过采购数量），回滚断言不变
- 门禁 `com.cashier.util.TestSchemaForeignKeyParityTest`（5 项，全部变异验证过）：
  生产 DDL 与测试库外键**集合相等**（缺/多都报）、两个审计归属列两边都**不得**有外键、
  悬空引用必须被拒绝（证明约束真的建出来了）、**删用户后历史交易与审计日志的操作人仍在**
  （把外键加回去即变红：`expected: <audit_fk_user> but was: <null>`）
- 新增测试便利方法 `DatabaseTestBase.executeSql(sql, params...)`（补父行用）

**结账成功弹窗金额（2026-09 审计修复，第六批）**

`CartController.showSuccess` 原来用 `getFinalAmount()` 显示实付金额——该方法按**结账后的**
`currentMember` 重算，而 `executeTransaction` 成功后会就地升级等级/折扣（普通会员跨 1000 分
当场升银卡），于是弹窗显示 9.5 折金额，与实际收款、落库 `final_amount`、小票都不一致。
现改为显示**本单落库值** `transaction.finalAmount`（`createTransaction()` 结账前算好、也是实收值），
无需改方法签名。门禁 `CheckoutConsistencyPolicyTest.successDialogUsesSettledAmount`
（按方法体断言必须用 `transaction.finalAmount`、不得出现 `getFinalAmount()`；改回去即变红）。
注意：源码级门禁扫方法体文本，方法体里的注释也不能出现 `getFinalAmount()` 字面量。

**结账 worker 线程安全（2026-09 审计修复，第七批）**

两个收银台的结账都在 daemon 线程执行，此前把 FX 线程拥有的活对象直接交给事务：
worker 迭代 `cartItems`（`ObservableList`）、对 `inventoryMap`（普通 `HashMap`）做
`computeIfAbsent`、并把活 `currentMember` 传给 `executeTransaction`——而后者会迭代明细、
把扣减后的商品 **put 回传入的 map**、并把新等级/折扣/积分写回传入的会员对象，
于是 FX 线程可能读到写了一半的集合或瞬间变化的会员状态（`paymentInProgress` 挡不住 FX 侧的读）。

- 现在 worker **只吃切线程前的快照**：`itemsAtSale = new ArrayList<>(cartItems/cartList)`、
  `inventoryForSale = new HashMap<>(inventoryMap)`、`memberForSale = TransactionService.copyMember(currentMember)`
  （该助手改为 `public` 且 `null` 安全）、`promotionToApply = appliedPromotion`；
  小票的 `MemberSnapshot` 也提前到切线程前取
- **库存回填**：`executeTransaction` 的"库存输出"落在副本上，新增
  `mergeSettledInventory(itemsAtSale, inventoryForSale)` 在 `Platform.runLater` 里**只把本单涉及的商品**
  并回 FX 侧 `inventoryMap`（避免覆盖结账期间 FX 线程刚查到的数据）；触屏台随后还会 `loadProducts()` 重刷
- 会员无需回写：两条成功路径紧接着都 `clear()` / `resetAfterPayment()` 把 `currentMember` 置空
- 门禁 `CheckoutThreadSafetyPolicyTest`：按方法体切出 `new Thread(` 之后的 worker 段，断言
  worker 内不得出现 `cartItems`/`cartList`/`inventoryMap`/`currentMember`/`appliedPromotion`、
  三个快照必须在切线程前创建且被 `executeTransaction` 使用；两处变异（标准台改回传活对象、
  触屏台改回写 `inventoryMap`）均确认变红

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

**非收银台页面加载的后台化（2026-09 审计修复，第三批）**

此前"查库统一后台化"只覆盖两个收银台，其它页面的"打开即查库"仍在 FX 线程（打开即冻结）。
本批把 **9 个控制器 / 18 个页面级加载方法**统一改为 `UIOptimizer.runInBackground`
（后台查库 + 回填都在 FX 线程）：

- `InventoryController.loadTableData/loadCategories`、`SupplierController.loadSuppliers/handleSearch`、
  `PromotionController.loadPromotions`、`TransactionController.loadTransactions/applyFilters`、
  `ShiftController.loadShifts`、`PurchaseOrderController.loadSuppliers/loadOrders`、
  `PurchaseApprovalController.loadPendingOrders/loadAllOrders/updateCountLabel`、
  `PurchaseInboundController.loadApprovedOrders`（原本还有 N+1 次按订单查明细）、
  `ProductEditController.loadCategories/loadUnits/loadSuppliers`、
  `SearchController.performSearch`（加 `searchSequence` 丢弃过期结果）
- 顺带修正"后台线程读界面控件"的隐患：`TransactionController` 的日期选择器、
  `ShiftController` 的收银员过滤条件、`SupplierController`/`TransactionController` 的搜索框
  都在**提交前**于 FX 线程取值，后台只拿不可变参数
- 渲染与查询分离：新增 `refreshInventoryTable()` / `renderTransactions()` / `applyFiltersAndRender()` /
  `renderShifts()` / `renderOrders()` 等只做界面刷新的方法，方便门禁按方法体断言
- 核实为**误报**：`MemberController.loadTableData`、`UserController.loadUsers` 本来就是
  `new Thread + Platform.runLater`，无需改动
- 门禁 `FxThreadDbPolicyTest.nonPosPageLoadsRunOffTheFxThread`：18 条 (文件, 方法, 查库语句) 逐条断言
  "必须走后台入口 + 查库语句仍在方法内 + 不得再有 `catch (SQLException)` 的同步写法"；
  已做变异验证（把 `InventoryController.loadTableData` 改回同步即变红）
- **报表链路（2026-09 第二批）**：报表页此前是"点按钮同步查两次库再聚合渲染"，现拆成
  "后台 loader 只查库 + FX 线程聚合渲染"：
  - `InventoryReportController.calculateStatistics` → `runInBackground(() -> loadReportData(...), data -> renderStatistics(...))`
    （新增后台 loader `loadReportData` 与渲染方法 `renderStatistics`，删掉原先两个同步加载方法）
  - `PurchaseReportController.handleQuery` → 后台 `loadReportData(startDate, endDate)`（订单 + 明细分组，
    失败返回 `null`），筛选与统计回 FX 线程；`loadData()` 的供应商查询同样放后台
  - `ProfitReportController` 主体本来就是 worker，但 `calculateStatistics`（在 `Platform.runLater` 里）会调
    `loadOperatingCostRatio()` 查设置表 —— 现由 worker 预先查好、以参数传入
  - `ReturnApprovalController.loadOrderItems`（点开待审批退货单查明细）改后台
- **点击后短查询（2026-09 第三批）**：`ReturnOrderController.loadReturnOrderItems`（退货单明细）、
  `SupplierController` 自动生成供应商编号（→ 异步 `applyGeneratedSupplierCode(codeField)`）、
  `InventoryController` 分类/单位管理弹窗（抽出 `loadCategoryManagementData` / `loadUnitManagementData`）、
  `MemberController.handleSearch`（会员搜索）、`ShiftController.updateShiftButtonStates`（活跃班次）
  全部改为后台查询 + 回 FX 线程填表/切换状态
- 门禁（均已变异验证）：`nonPosPageLoadsRunOffTheFxThread` 登记表 **28 条**（此前文档写的 27 是漏数，已订正）；
  新增 `reportLoadersAreOnlyCalledAsBackgroundTask`（loader 里必须有查库语句、且只能以
  `() -> loadXxx(...)` 形式提交一次）与 `profitReportDoesNotQuerySettingsOnTheFxThread`
  （渲染方法不得出现 `loadOperatingCostRatio()`，且 worker 必须先取好比例）
- **交班结算（2026-09 第四批，TD-006 最后一条无界查询）**：`ShiftController.handleEndShift` 原来在
  FX 线程跑"查活跃班次 → 确认框 → `loadShiftTransactions`（**拉整班交易**，忙时上千笔）→ 聚合 → 落库"，
  点"交班"会冻结界面。现拆为：`handleEndShift` 只提交后台任务（活跃班次查询也进后台）→
  `confirmEndShift`（FX，确认框）→ `endShiftInBackground`（后台：取数 + 聚合 + `shiftDao.update`，
  **不得触碰 UI**）→ `renderShiftEnded`（FX：置位 → 刷新 → 提示 → 关窗）。
  **顺序契约**：`shiftEnded = true` 必须早于 `closeWindow();` —— `TouchCartController` 是
  `showAndWait()` 返回后**同步**读 `isShiftEnded()` 决定是否自动退出登录的，顺序调换会让该流程永不退出。
  门禁 `FxThreadDbPolicyTest.endShiftSettlementRunsOffTheFxThread`（11 条断言，含"FX 侧确认方法不得查库"
  这条堵漏 + 上述顺序）；3 处变异（顺序调换 / 后台碰 UI / FX 侧查库）均确认变红
- **明确接受、不再修（2026-09 复核）**：TD-006 剩余条目都有界——`PurchaseInbound`/`PurchaseOrder` 弹窗、
  `UserController` 搜索/停用、`PromotionController` 批量写、会员/用户单行写；每条在
  `docs/TECH_DEBT.md` 里写明**实测开销 / 接受理由 / 何时重新评估**（如"分页被去掉""促销数量级变大"）。
  同类：打包向导 `PackageWizardController` **明确不译** i18n（开发者工具，
  由 `-Ppackager` profile 构建、POS 界面零引用）
- **验证方法教训**：变异测试"没变红"时**先确认变异代码编译通过**——有一次我把直接调用会抛受检异常的
  方法塞进去，Maven 在编译期就失败、日志里没有测试失败行，看起来与"门禁通过"完全一样；
  判断依据是输出里有没有 `Tests run:` 行（或 `COMPILATION ERROR`）

**启动期数据库阶段（v2.6.0 补强）**

- 配置向导、连接池建连/建表迁移、语言偏好读取**整体移到 `startup-database` 后台线程**：
  此前全在 FX 线程同步执行，启动画面被占死（进度文案根本刷不出来），连接一旦被挂住就
  无限期停在启动画面——实测一次 **2 小时 11 分没有任何输出**
- `db.connection.timeout` 默认 **5000 → 15000**：同一 JVM 内首次连接实测 8.5s、后续仅约 40ms，
  5s 会让"数据库其实正常"的机器在建池阶段直接失败。另给 JDBC 设了 `connectTimeout`
  （Connector/J 默认不超时，被丢弃的路由会让启动一直挂着；它只影响建立 TCP 连接，不动长查询）
- 启动阶段有 **60 秒上限**（`STARTUP_DATABASE_TIMEOUT_MS` + Timeline 看门狗；不用阻塞式 `get`，
  否则又占死 FX 线程），每 5 秒在日志留一行 `启动阶段: 仍在等待数据库（已 N 秒）`，
  启动画面同步显示 `正在连接数据库...（已等待 N 秒）`；超时给出可操作文案而不是静默卡住
- 建池失败的错误信息一并带上"确认 MySQL/主机端口/口令，必要时调大 db.connection.timeout
  （当前 15000ms）"——原来只有一句 `Communications link failure`，用户无从下手
- 门禁：`FxThreadDbPolicyTest.startupDatabaseInitializationRunsOffTheFxThreadWithBoundedWait`
  （按方法体断言 `PaymentService.init()` 不在 `initializeApplication` 内、且不得在 FX 线程 `.get(` 阻塞）
  与 `FxThreadDbPolicyTest.databaseConnectionTimeoutToleratesColdStart`（默认值 ≥10s 且必须设 TCP 建连超时）；
  两者均做过变异验证（把默认值改回 5000 即变红）

**REST API 错误语义（v2.6.0 补强）**

实测发现三个问题（都只在真实 HTTP 上才暴露，控制器级的 TestContext 测试看不到）：

- **请求体写错回 500 而不是 400**：`ctx.bodyAsClass(...)` 在未知字段、字段类型不符、请求体为空或
  不是合法 JSON 时都是**抛异常**，会被各控制器自己的 `catch (Exception e) → 500` 兜住——给
  `POST /api/products` 传一个不存在的 `stock` 字段（真实字段是 `quantity`）只得到「服务器内部错误」。
  现在统一走 `com.cashier.api.controller.ApiRequest.parse`：把解析异常收敛成 `null`
  （各控制器后面本来就有 `if (request == null) → 400` 的判断，原意就是"失败返回 null"），
  服务端自身故障仍旧照 500 抛出、不伪装成客户端错误；具体原因（含写错的字段名与可用字段列表）记在 WARN 日志
- **缺必填字段也是 500**：`ProductApiController.create` 把 `productCode` 默认成空串，交给 DAO 才抛
  `SQLException: 商品编号不能为空`，同样被兜成 500。改为就地校验 `productCode`/`name` 并回 400
  且指明缺哪个字段；校验后那行 `request.productCode != null ? … : ""` 成了多余判空，一并简化
- **`/api/transactions/today` 不可达**：Javalin 按注册顺序匹配，`/api/transactions/{id}` 注册在它之前，
  于是 `today` 被当成 `id="today"` 查库后回 404。已把字面量路由挪到 `{id}` 之前
- **404 兜底文案覆盖业务 404**：端点自己写的 404（「交易不存在」）被统一的「接口不存在」覆盖，
  客户端分不清"路由不存在"和"资源不存在"。但**不能只按"有没有响应体"判断**——实测 Javalin 对未匹配
  路由会预填 `text/plain` 的 `Endpoint GET /x not found`，而业务 404 是 `application/json`；
  所以判据是"响应体为空 **或** Content-Type 不是 JSON"
- 门禁：`ApiRequestTest`（5 项：写错回 400、未知字段提示含字段名与可用字段、空请求体归 400、
  服务端故障仍 500、所有控制器不得绕过 `ApiRequest.parse`）与 `ApiServerTest` 2 项
  （404 兜底只在非 JSON 时生效、路由顺序 + 处理器必须看 Content-Type）；
  5 处变异验证（路由顺序改回、404 无条件覆盖、不看 Content-Type、绕过助手、把服务端异常也当客户端错误）全部变红

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
  - 共享两端 `createTransaction` 的口径障碍**已解除**（见下节「结账字段口径」）；
    三处订单号生成方式也已统一（2026-09 审计修）：标准端 `CartController.generateOrderNumber()`
    与触屏端一样委托 `TransactionService.generateOrderNumber()`，REST API 走
    `TransactionService.generateTransactionId("T")`，统一格式为"前缀 + 17 位毫秒 + 4 位序号 + 8 位随机段"。
    此前标准端/API 用的是**裸毫秒时间戳**，同毫秒或跨终端会撞 `transactions.transaction_id` 主键、
    整单回滚（并由 `executeTransaction` 的会员副本机制避免留下脏状态）；
    门禁 `OrderNumberGenerationTest.allCheckoutPathsUseHardenedTransactionId`
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
  - 清理连带发现（**已修，2026-09**）：触屏收银台的底部状态栏原先只有品牌/班次/日期/时间，
    **没有状态文本控件**，也不绑定 `StatusBarManager.statusLevelProperty()`（原先唯一的绑定者在已删除的
    PosModeController 里，而它从未被加载）。所以触屏版里 `StatusBarManager.updateSuccess/updateError`
    这类提示（如"商品加载失败"、扫码添加成功）**在界面上看不到**。现已给 `TouchCartView` 底栏加
    `fx:id="statusLabel"`，并在 `TouchCartController` 里 `bindStatusBar()` 绑定文本 + 级别样式
    （照 `MainController.applyStatusLevelStyle` 抄，同一套 `text-success/warning/danger`），
    `cleanup()` 解除绑定与监听；`StatusBarSeverityPolicyTest` 已同时断言触屏控制器与 FXML

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
- 技术债登记在 `docs/TECH_DEBT.md`：TD-001 发票文件路径白名单校验（阻塞于「发票预览/下载」需求）；
  2026-09 全量审计的结论均已登记，其中 TD-002（支付方式归一化）、TD-003（营业额净额口径）、
  TD-004（API 校验/审计归属）、TD-005（触屏状态栏）、TD-007（结账线程安全）、
  TD-008（成功弹窗金额）、TD-009（测试库外键/审计归属）、TD-013（版本号四处一致）**已修复**，
  TD-006 仅剩约 6 处（含交互流程与写操作），
  TD-014 部分（文档测试数不再写死、locale 折叠已修，可见文案硬编码待办）；
  TD-010（初始化脚本与 Java 建表对齐）、TD-012（安装脚本失败可见与口令守卫）、
  TD-015（`String.format` 默认 locale）**已修复**；
  TD-014 终端用户可见文案已迁完（对话框/占位弹窗/打印预览/启动画面/字体提示/InventoryView、  以及主界面 26 处状态栏文案，均为复用既有 key）；
  TD-011 静态部分已修（Windows 脚本检测/行尾/不自动构建 + 7 项门禁），**实机验收清单见该条目**；
  TD-011 **已关闭**（Windows 实机 5 步验收全通过；实机过程另修掉三处相关问题：中文注释破坏批处理解析、
   `chcp` 影响同窗口后续命令、`install.bat` 覆盖已跟踪的 `DataConfig.bat`）；
  TD-016 已修（Windows 实机暴露的异步顺序 NPE，14 个控制器 / 25 个字段 + 2 项门禁）；
  TD-014 已收口（可见文案、无用 key、说明文档入库 + 一致性门禁），**仅剩打包向导**（维护者工具，是否 i18n 化属产品选择）；
  其它 TODO 见 `docs/TECH_DEBT.md`

**数据库密码来源（v2.6.0 补强）**

- 新增 `com.cashier.util.DotEnv`：读取/维护工作目录 `.env`（忽略注释、去引号、重复键取首条、
  写入时保留其它键并把权限收到 `rw-------`）。`DatabaseManager.resolveDatabasePassword` 与
  `DatabaseConnectionHelper.loadConnectionConfig` 改为"环境变量 → `.env` → 配置文件"
- `DatabaseConfigDialog` 与 `Installer.createConfigFiles` **不再把密码写进
  `config/database.properties`**（任何模式都留空，开发模式改写 `.env`），
  既满足 `release.bat`/`release.sh` 的 `db.password` 门禁，也让 dev/prod 不再分叉；
  回归门禁见 `PerformancePolicyTest.installersNeverPersistPasswordIntoConfig`
- 本机约定：密码放根目录 `.env`（已 gitignore），`config/database.properties` 的 `db.password` 留空
- 口令变量名只有一个：`CASHIER_DB_PASSWORD`（应用侧 `DotEnv.DB_PASSWORD_KEY`，compose 也读它，
  `CASHER_DB_PASSWORD` 是应用侧的历史拼写兼容）
- **旧 `.env` 里的 `MYSQL_PASSWORD` 只有启动/发布脚本还认**：`start.bat`/`start.sh`、
  `release.bat`/`release.sh`、`install.bat`/`install.sh`、`docker/docker-init.sh`、
  `docker/backup-db.sh` 都把它当作 `CASHIER_DB_PASSWORD` 的旧别名回退读取（README 四份都写明了这一兼容）。
  不认它的只有两处：
  - **docker compose**：`docker compose up -d mysql` 会直接报 `required variable CASHIER_DB_PASSWORD is missing`；
  - **应用自身**（`DotEnv`）：直接用 `java -jar` 启动时 `db.password` 为空会连不上。

  也就是说同一份旧 `.env` 经 `start.bat` 能跑、`java -jar` 不能跑。从旧 `.env` 升级时把
  `MYSQL_PASSWORD` 改名为 `CASHIER_DB_PASSWORD` 即可（`.env.example` 已注明）

**REST API 启用步骤（本地冒烟/生产）**

1. `config/api.properties` 已生成就绪版本（随机 `token.secret`，`api.enabled=false`，gitignored 不入库）
2. 本地冒烟：`api.enabled=true` 后重启 —— **注意 API 是在 `loadFullMainView(User)` 里启动的
   （`CashierSystemFXApplication` 第 848 行附近）**，也就是必须有人用 admin/finance 登录进完整主界面
   才会监听；`cashier` 角色登录走 `switchToPosModeView`、没人登录时都**不会**启动 API。
   想不依赖登录做无界面冒烟，直接调 `ApiServer.getInstance().start(ApiConfig.getPort())`
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
