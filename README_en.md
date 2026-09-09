# LiSuan Cashier System

[简体中文](./README.md) | English | [繁體中文](./README_zh_TW.md)

LiSuan Cashier System is a desktop POS (Point of Sale) cashier system built with JavaFX 17. It is designed for daily retail operations, covering checkout, products, members, purchasing, inventory, returns, reports, user permissions, data backup, and hardware integration.

**Current Version**: v2.6.0 | **Latest Update**: 2026-08-29 | **Test Coverage**: 520 test cases

> Test scope: `mvn -q clean verify` runs 520 test cases by default (including SpotBugs and JaCoCo gates);
> `LoginControllerUITest` (17 cases) requires a real display environment and is run explicitly with
> `mvn -Pui-tests -Dtest=LoginControllerUITest test`, for a total of 537 cases.

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![MySQL](https://img.shields.io/badge/MySQL-8.4-blue)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

---

## Features Overview

### Checkout Management
- Checkout & cash register, shopping cart CRUD, product search, member phone number recognition.
- Supports multiple payment methods including cash, WeChat Pay, Alipay, and bank cards.
- Payment modes: `disabled` / `mock` / `production`; in mock mode the payment dialog offers a "Simulate Payment" button for closed-loop local testing.
- Member discounts, points accumulation, and balance consumption linked with transaction history.
- Receipt printing, print preview, cash drawer trigger, and shortcut key help.
- Shift management to record shift sales and cashier performance.
- Touch screen POS: scanning/typing an exact product match adds it to the cart automatically (with success/not-found/low-stock sounds), enabling fast continuous entry.
- Touch screen POS bottom status bar shows "Touch POS", shift info, date (with weekday) and time; the exit-and-handover flow is aligned with the desktop version (Handover / Cancel / OK).

### Product & Inventory
- Product CRUD supporting product code, barcode, category, unit, specification, selling price, cost price, and stock levels.
- Unique product name constraint, duplicate barcodes allowed.
- Stock level alerts, low-stock queries, and quick inbound restocking.
- Inventory check, check details, discrepancy processing, and inventory check status tracking.
- Product CSV import and Excel/PDF export.

### Members & Customers
- Member registration, editing, queries, recharge, and balance management.
- Points accumulation from purchases, and automatic member tier upgrades.
- Tiered discount rules: Regular (no discount, 10.0), Silver (5% off, 9.5), Gold (10% off, 9.0), Diamond (15% off, 8.5).
- Recharge history queries, and linkage between member spending and return workflows.

### Purchasing Management
- Supplier profile maintenance.
- Purchase order creation, editing, querying, and status workflows.
- Purchase approval process.
- Purchase inbound stock and inbound history.
- Purchase report statistics.

### Returns Management
- Create return orders based on original transactions.
- Record returned items, return reasons, item conditions, and refund methods.
- Return approval process.
- Restores inventory upon approval and refunds via cash, balance, or points.
- Return orders and reports support querying, filtering, exporting, and printing.

### Reports & Statistics
- Transaction log querying and today's sales statistics.
- Data visualization charts.
- Purchase reports, inventory reports, profit analysis, and return reports.
- Supports Excel and PDF export.
- Profit analysis includes sales, cost, gross profit, margin, category, and daily trend views.

### Users, Permissions & Auditing
- Default roles: Administrator, Cashier, Accountant.
- BCrypt password encryption, supporting password resets and forced change on first login.
- User activation, deactivation, deletion, and role management.
- Admin auditing logs covering authentication, transactions, returns, inventory, purchases, members, users, and system settings.

### Data Backup & Recovery
- In-menu data backup and restore.
- SQL backups default to `backups/sql`.
- Backup service packages database, config, logs, invoices, and business data.
- Supports local backup, automated backup configuration, retention policies, and auto-cleanup.
- API retains cloud backup config fields for object storage integration.
- Restoring a backup now requires administrator password confirmation to prevent unauthorized data overwrites.

### REST API & Synchronization
- Built-in Javalin API service, running on port `8080` by default.
- Currently registers 90+ HTTP routes and WebSocket sync endpoints.
- Covers auth, products, members, transactions, inventory, reports, settings, invoices, users, printing, payments, backup, i18n, and sync status.
- Token-based authentication, role-based authorization, rate limiting, and security headers.
- Payment callback routes (`/api/payment/notify/*`) are public; security is guaranteed by channel signature verification (WeChat RSA/AES-GCM, Alipay RSA, mock secret).
- WebSocket sync endpoint: `/ws/sync`.

### Hardware Support
- ESC/POS thermal printing, network printing, print queues, and printing history.
- Printer discovery, connection, status checks, and default printer configuration.
- USB HID barcode scanner integration and focus management.
- Electronic payment QR code generation and payment status queries.

### Internationalization & Themes
- Supports Simplified Chinese, English, and Traditional Chinese.
- Runtime text, dialogs, form validation, status bar, and report pages fully localized.
- Currency display internationalization.
- Themes: LiSuan, Light, Dark, compatible with legacy IntelliJ theme preferences.
- Built-in Noto Sans CJK SC fonts for cross-platform Chinese rendering.
- Left navigation icons streamlined semantically to avoid redundancy.
- Touch screen POS supports one-tap language switching, settings apply globally.

---

## Quick Start

### Prerequisites
- JDK 17 or higher
- Maven 3.8 or higher
- MySQL 8.4 or compatible MySQL 8.0/8.3
- Docker Compose (optional, for quick MySQL startup)

### Default Accounts
- Username: `admin`
- Initial Password: `admin123`
- *It is highly recommended to change the password immediately upon first login.*

### Starting Database

```bash
docker compose up -d mysql
```

The database initialization script is located at `docker/mysql-init/00-init-complete.sql`. The default configuration file is `config/database.properties`. For production environments, it is recommended to pass the password via environment variables:

```bash
export CASHIER_DB_PASSWORD=your_password
```

### Environment Config (.env)

The project supports managing installation and startup configuration centrally via a root-level `.env` file, avoiding hard-coded passwords and secrets in config files or scripts.

```bash
cp .env.example .env
```

`.env.example` is the template maintained in the repository; `.env` holds your local actual configuration (and is ignored by `.gitignore`, so never commit real secrets). Key variables:

| Variable | Description |
|----------|-------------|
| `APP_VERSION` / `APP_NAME` | Application version and branding (used to package via `install.sh`) |
| `ENVIRONMENT` | `development` (connects as root) or `production` (dedicated user `lisuan`) |
| `DB_TYPE` | Database type: `docker` / `local` / `none`, used to guide `install.sh` |
| `MYSQL_CONTAINER_NAME` / `MYSQL_IMAGE` | Docker MySQL container and image |
| `MYSQL_ROOT_PASSWORD` / `CASHIER_DB_PASSWORD` / `MYSQL_USER` / `MYSQL_DATABASE` | Docker database initialization credentials (`CASHIER_DB_PASSWORD` is also used by the application at runtime) |
| `DB_HOST` / `DB_PORT` | Database host and port |
| `TZ` | Time zone (default `Asia/Shanghai`) |
| `JVM_OPTS` | JVM launch options (`config/jvm.config` takes precedence) |
| `TOKEN_SECRET` | API token secret, at least 32 random characters |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins for the API; do not use `*` in production |
| `CASHIER_DB_PASSWORD` / `CASHER_DB_PASSWORD` | Application DB user password (shared by Docker DB bootstrap and app runtime; `CASHER_DB_PASSWORD` is the legacy spelling) |

Usage notes:

- `install.sh` / `install.bat` load the whole `.env` for database initialization, config generation, and packaging; undefined variables fall back to interactive prompts.
- When `.env` is detected (or `ENVIRONMENT=production`), `install.sh` does not write the password into `config/database.properties`; the runtime password is provided solely by `CASHIER_DB_PASSWORD`.
- `start.sh` only selectively reads the two password variables `CASHIER_DB_PASSWORD` / `CASHER_DB_PASSWORD` to override the DB password; it does not bring placeholder configs such as `TOKEN_SECRET`, `CORS_ALLOWED_ORIGINS` into the runtime environment (those are still passed to the API via system environment variables).
- `DataConfig.bat` also reads `.env` to generate the database config.
- **Security note**: On first use, replace `MYSQL_ROOT_PASSWORD`, `CASHIER_DB_PASSWORD`, and `TOKEN_SECRET` with independent strong random values, and restrict `CORS_ALLOWED_ORIGINS`. The legacy `MYSQL_PASSWORD` key in `.env` has been unified into `CASHIER_DB_PASSWORD` (scripts still read the old variable for backward compatibility).

### Development Run

```bash
mvn clean compile
mvn javafx:run
```

### Package and Run

```bash
mvn clean package
java -jar target/lisuan-fx-2.6.0-jar-with-dependencies.jar
```

Alternatively, launch using wildcards:

```bash
java -jar target/lisuan-fx-*-jar-with-dependencies.jar
```

### Windows

For the first-time installation, it is recommended to run:

```bat
install.bat
```

For daily launch:

```bat
start.bat
start.bat --gui
```

Database Configuration Tool:

```bat
DataConfig.bat
```

### Linux / macOS

```bash
chmod +x install.sh start.sh
./install.sh
./start.sh
```

`install.sh` checks for Java/Maven, builds the executable JAR, and guides you through configuring Docker or local/remote MySQL. `start.sh` prioritizes running the executable JAR with dependencies.

---

## Common Commands

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package while skipping tests
mvn clean package -DskipTests

# Run specific test class
mvn test -Dtest=ProductDAOTest

# Run specific test method
mvn test -Dtest=PasswordUtilTest#testHashPassword

# Static quality checks (SpotBugs)
mvn -q -DskipTests spotbugs:check
```

---

## Shortcuts

| Shortcut Key | Function |
|--------------|----------|
| `F1` | Add item to cart |
| `Delete` | Remove selected item |
| `Ctrl+L` | Clear cart |
| `F8` | Pay with Cash |
| `Ctrl+1` | Pay with WeChat Pay |
| `Ctrl+2` | Pay with Alipay |
| `Ctrl+3` | Pay with Bank Card |
| `Ctrl+F` | Focus search bar |
| `Ctrl+M` | Focus member phone number field |
| `Ctrl+/` | Show shortcuts help |

---

## Member Tiers

| Tier | Points Range | Discount |
|------|--------------|----------|
| Regular Member | 0-999 | No discount, calculated as 10.0 |
| Silver Member | 1000-4999 | 5% off (9.5) |
| Gold Member | 5000-9999 | 10% off (9.0) |
| Diamond Member | 10000+ | 15% off (8.5) |

Discount Calculation: `Total Amount * (Member Discount / 10.0)`.

---

## REST API

The API service runs on port `8080` by default. Before launching the API in production, security parameters must be configured:

```bash
export TOKEN_SECRET=at_least_32_characters_secret
export CORS_ALLOWED_ORIGINS=https://your-domain.example
```

Key API endpoints grouping:

| Group | Path | Description |
|-------|------|-------------|
| Health Check | `/api/health` | Server and database status |
| Authentication | `/api/auth/*` | Login, refresh token, logout, current user |
| Products | `/api/products/*` | Product CRUD, low-stock warnings |
| Members | `/api/members/*` | Member CRUD, phone query, recharge |
| Transactions | `/api/transactions/*` | Transaction creation, querying, refunding, today's stats |
| Inventory | `/api/inventory/*` | Inventory list, alerts, checking, updates |
| Reports | `/api/reports/*` | Daily, monthly reports, hot products, payment method stats |
| Settings | `/api/settings/*` | Read/write system settings |
| Invoices | `/api/invoices/*` | Invoice creation, querying, voiding, print history |
| Users | `/api/users/*` | User management |
| Printing | `/api/printers/*` | Printer discovery, connection, status, and printing |
| Payment | `/api/payment/*` | Payment creation, status querying, refunding, config |
| Backup | `/api/backup/*` | Backup execution, restoring, downloading, configuring |
| Internationalization | `/api/i18n/*` | Query language, messages, and language packs |
| Synchronization | `/ws/sync`, `/api/sync/status` | Multi-terminal sync and online status |

All API endpoints except `/api/health`, `/api/auth/login`, and the payment callbacks
`/api/payment/notify/*` require authentication by default (payment callbacks are invoked by
WeChat/Alipay servers without our system token; their security relies on channel signature verification).

---

## Database

The project uses MySQL, with HikariCP as the connection pool. Key database tables include:

| Table | Description |
|-------|-------------|
| `products` | Product information, product name is unique |
| `members` | Member information |
| `transactions`, `transaction_items` | Transactions and transaction details |
| `return_orders`, `return_order_items` | Return orders and return details |
| `purchase_orders`, `purchase_order_items` | Purchase orders and details |
| `purchase_approvals` | Purchase approval records |
| `purchase_inbound`, `purchase_inbound_items` | Purchase inbound stock and details |
| `suppliers` | Supplier profiles |
| `inventory_check`, `inventory_check_items` | Inventory checks and details |
| `users` | Users and roles |
| `shifts` | Shift logs |
| `invoices` | Invoices |
| `payment_orders`, `refund_records` | Payment orders and refund records |
| `promotions` | Promotion rules |
| `backup_records`, `backup_config` | Backup logs and configuration |
| `operation_logs` | Audit logs |
| `settings` | System settings |

For database schema version details, see `docker/mysql-init/DATABASE_VERSIONS.md`.

---

## Project Structure

```text
src/main/java/com/cashier/
├── api/           # Javalin REST API, Auth, Sync
├── component/     # Reusable JavaFX components
├── constant/      # App, DB, and system property constants
├── controller/    # JavaFX Controllers
├── dao/           # Data Access Objects (DAOs)
├── i18n/          # I18n management and key constants
├── model/         # Business entities
├── notification/  # Notification system
├── printer/       # Printing devices and templates
├── scanner/       # Barcode scanner integration
├── service/       # Business services
└── util/          # Utilities for DB, import/export, logs, themes
```

```text
src/main/resources/
├── com/cashier/view/      # FXML Views
├── com/cashier/i18n/      # Language bundles (.properties)
├── css/                   # LiSuan, Light, and Dark themes
├── fonts/                 # Built-in fonts
└── images/                # App icons and image assets
```

## Related Documents

- [CLAUDE.md](CLAUDE.md) — architecture, migration status, and development conventions
- [docs/GO_LIVE_CHECKLIST.md](docs/GO_LIVE_CHECKLIST.md) — go-live checklist
- [docs/CREDENTIALS_CHECKLIST.md](docs/CREDENTIALS_CHECKLIST.md) — credential preparation checklist (database/API/payment/cloud backup)
- [docs/DATABASE_INIT.md](docs/DATABASE_INIT.md) — database initialization guide

---

## Code Quality & Security

### Security Measures
- **Password Storage**: Uses BCrypt encryption, plaintext passwords are prohibited
- **SQL Injection Prevention**: All queries use `PreparedStatement` parameterization
- **Random Number Generation**: Security-sensitive scenarios use `SecureRandom`
- **Resource Management**: JDBC connections use try-with-resources to prevent leaks
- **Login Anti-Brute-Force**: Failed attempts are counted per user and locked; reset on success or expiry
- **Restore Protection**: Data restore requires administrator password confirmation
- **Payment Security**: Callback signature verification + amount validation + terminal-state protection (late callbacks rejected for refunded/closed orders, duplicate callbacks idempotent)
- **API Authentication**: Token-based authentication with 24-hour expiration
- **Role Authorization**: Three-tier permissions (Administrator, Cashier, Accountant)
- **Rate Limiting**: Maximum 60 requests per IP per minute
- **Security Headers**: X-Content-Type-Options, X-Frame-Options, X-XSS-Protection

### Code Quality
- **Unit Tests**: 520 test cases (`mvn -q clean verify`) covering DAOs, services, utilities, concurrency safety, and the API (537 in total including display-dependent UI tests)
- **Static Analysis**: SpotBugs high-risk defect gate
- **Coverage Gate**: JaCoCo line coverage ≥10%
- **i18n Gate**: Enforces identical keys across the three language bundles, complete `I18nKeys` constants, and complete keys for source i18n calls
- **Concurrency Tests**: Optimistic locking prevents overselling stock and over-drawing member balances
- **Dependency Management**: Maven Enforcer plugin ensures dependency consistency
- **Logging Standards**: Unified use of SLF4J + LoggerFactoryUtil
- **Code Standards**: Follows guidelines in CLAUDE.md

---

## Development Conventions

- Logs must uniformly use `LoggerFactoryUtil.getLogger()`.
- DAOs must use `PreparedStatement` and extend `BaseDAO`.
- Business transactions spanning multiple DAOs must be handled within transaction blocks via entry points like `DatabaseManager.executeBooleanTransaction()`.
- New UI views should prefer FXML + Controller and provide fully localized bundles for all three supported languages.
- New localized keys should be declared in `I18nKeys` to prevent scattered literal strings in controllers.
- New non-localized constants should be centralized in their respective constant classes to avoid duplicate literals.
- After fixing code quality warnings, run compiling checks, and ensure related tests are executed if public logic is modified.

---

## Recent Updates

### v2.6.0

#### 2026-08-29
- Print settings wired: Settings-page "Enable Print / Printer Name / Paper Size / Logo" now applies to touch-POS checkout receipts (no receipt when disabled); added a "Test Print" button to the Settings page
- Scanner test: added a "Scanner Test" field to the Settings page using the same validation rules (normalization, blank data, 128-char limit)

#### 2026-08-28
- Configuration security: unified the DB password variable to `CASHIER_DB_PASSWORD` (backward compatible with the legacy `CASHER_DB_PASSWORD` and old `MYSQL_PASSWORD`); config files no longer store plaintext passwords; the runtime password is injected from `.env`/environment variables
- Docker: initialization SQL no longer hardcodes the database name and supports a custom `MYSQL_DATABASE`; compose variables and mounted `my.cnf`
- Script fixes: install.sh JDBC characterEncoding changed to UTF-8; release.bat Chinese output and version-gate fixes

#### 2026-08-23
- DAO layer fully migrated to instance-based `XxxDAORefactored extends BaseDAO` + `DAOFactory`; static DAOs and dead code removed
- Payment: mock mode "Simulate Payment" button for closed-loop UI testing; payment callback routes made public with channel signature verification
- Payment callback hardening: amount validation, idempotent duplicate callbacks, terminal-state protection for refunded/closed orders
- Data restore now requires administrator password confirmation
- Touch screen POS: exact-match scanning adds to cart automatically with success/not-found/low-stock sounds; exit-and-handover flow (Handover/Cancel/OK, exits after handover); bottom status bar shows "Touch POS", shift info, date with weekday and time; payment dialog simulate button
- Desktop: main-view exit returns to the login screen (login screen exit still closes the app); search bar clears after adding a product
- Code quality: fixed payment callback auth interception, health-check connection leak, and electronic payment dialog NPE; added i18n completeness gate, concurrency-safety tests, and token-expiry tests
- Added go-live checklist `docs/GO_LIVE_CHECKLIST.md` and credentials checklist `docs/CREDENTIALS_CHECKLIST.md`

#### 2026-07-24
- Touch screen POS adds language switching: One-tap toggle between Simplified Chinese/English/Traditional Chinese from toolbar
- Language preference optimization: Saves both user preference and global default, new users automatically use system language
- Fixed language switching not working: Unified language preference storage logic, prioritizes user preference > global default > system default
- I18nManager improvement: Uses language tag matching instead of Locale object comparison for better cross-platform compatibility
- Code quality unified: LanguagePreferenceDAO logging migrated from java.util.logging to SLF4J
- Touch screen POS now prompts for continued cash-only payment when partially paid in cash, preventing accounting errors

### v2.5.9 (2026-07-19)
- Security hardening: Payment orders and refund records now use `SecureRandom` for random code generation
- Fixed FormValidator DISCOUNT validation rule boundary issue (10.1 should be invalid)
- Fixed DatabaseManager JDBC resource leak (ResultSet not closed)
- Dependency updates: Jackson 2.18.2, SLF4J 2.0.16, Mockito 5.15.2, JUnit 5.11.3, H2 2.3.232
- Test enhancements: Added FormValidatorTest (33 tests) and LoginControllerUITest (17 tests)
- All 380 test cases passing, test coverage continuously improving

### v2.5.9-maintenance (2026-07-05)
- Fixed API Token security test failure.
- Optimized Mockito configuration for test environments.
- Strengthened security prompts in production configuration templates.
- Unified database initialization scripts, enabling out-of-the-box support for more modules.
- Optimized README structure for better readability.

### v2.5.9-maintenance (2026-07-04)
- Organized `I18nKeys`, centralizing commonly used internationalization keys.
- Centralized non-localized constants such as system properties, database configs, resource bundles, and date-time formats.
- Fixed several code quality warnings including complexity, duplicate strings, switch/default statements, resource closures, and static analysis issues.
- Updated left navigation icons to ensure uniqueness and logical grouping.
- Re-organized README to reflect current features, API, database schema, and maintenance procedures.

### v2.5.9 (2026-06-20)
- Enhanced localization: unified Simplified Chinese, English, and Traditional Chinese resources, filling gaps in dialogs, statuses, dates, approvals, and form validation hints.
- Optimized return workflow: added quick date filters, fixed multilingual displays for viewing original transactions, printing receipts, completing returns, and exporting notifications.
- Fixed purchasing and inventory check: fixed invisible columns in inbound stock history, text truncation in inventory check type labels, and corrected empty table warnings to respect current language.
- Fixed form layouts: removed vertical red lines caused by empty error labels on product add/edit views and localized validation errors.
- Theme and font optimizations: defaulted to the LiSuan theme, prioritized Noto Sans CJK SC for Simplified Chinese, and improved cross-platform fallbacks.
- Simplified language support: removed Japanese and Korean, maintaining only Simplified Chinese, English, and Traditional Chinese.

### v2.5.7-l10n (2026-06-20)
- Streamlined language packs down to Simplified Chinese, English, and Traditional Chinese.
- Filled missing localization keys to minimize fallback occurrences.
- Optimized UI copy for dialogs, sub-pages, status bars, and tooltip notifications.
- Adjusted widths for top headers and input fields across various pages.

### v2.5.7 (2026-06-12)
- Added support for separating development and production environments.
- Uses `root` for development, and the dedicated `lisuan` user for production.
- Refactored `FormValidator` parsing methods to prevent `NumberFormatException`.
- Installation scripts now support `.env` files and automatic environment detection.
- Windows configuration tool updated to support environment variables.
- Branding unified across scripts as "LiSuan System".

### v2.5.6 (2026-06-10)
- Unified branding to "狸算(LiSuan)收银系统" (LiSuan Cashier System).
- Updated user interfaces, shell scripts, receipt prints, application icons, and Docker container branding.

### v2.5.5 (2026-06-09)
- Optimized tab width and visual style of close buttons.
- Added font-size adjustment capability.
- Fixed compatibility issues with the `fcitx5` input method.
- Improved Traditional Chinese text rendering and cross-platform font fallback chains.

### v2.5.4 (2026-05-21)
- Added GUI database configuration utility tool.
- Optimized Windows distribution package and setup script.

### v2.5.3 (2026-05-15)
- Windows-specific performance optimizations.
- Enhanced application startup user experience.
- Strengthened data sync robustness.
- Completed DAO layer refactoring.
- Fixed resource disposal issues and memory leaks.

---

## Troubleshooting

**Application Fails to Start**
- Verify JDK is version 17 or higher.
- Ensure MySQL is running.
- Check database settings in `config/database.properties`.
- Check log files inside the `logs/` directory.

**Database Connection Error**
- Double-check host, port, database name, username, and password.
- If using environment variables, ensure `CASHIER_DB_PASSWORD` is properly exported.
- If managing configuration via `.env`, ensure `cp .env.example .env` was run and that `DB_HOST`/`DB_PORT`/`MYSQL_*` match your local setup.
- For Docker, verify container state with `docker compose up -d mysql`.

**API Service Fails to Run**
- Verify that a `TOKEN_SECRET` of at least 32 characters has been defined.
- In production, avoid using wildcard `CORS_ALLOWED_ORIGINS=*`.
- Check logs for port conflicts or config errors.

**Scanner Unresponsive**
- Verify the barcode scanner is connected and operating in USB HID keyboard emulation mode.
- Ensure focus is not locked inside other inputs or text boxes on the current screen.

**Printer Unresponsive**
- Check printer power, cable connection, driver status, and paper.
- Review printer state detection and printing logs.
- For network printing, verify IP address, port, and network firewall rules.

**Incorrect Chinese Characters Rendering**
- Ensure built-in Noto Sans CJK SC fonts are packed inside resources.
- On Linux, consider installing a local CJK font package.

---

## License

Mulan Permissive Software License v2 (MulanPSL2)

---

**Repository**: https://gitee.com/nevell/lisuan.git

**Issues**: https://gitee.com/nevell/lisuan/issues
