# LiSuan Cashier System

[简体中文](./README.md) | English | [繁體中文](./README_zh_TW.md)

LiSuan Cashier System is a desktop POS cashier system built with JavaFX 17, covering checkout, products, members, purchasing, inventory, returns, reports, user permissions, data backup, and hardware integration for daily retail operations.

**Current Version**: v2.6.0 | **Latest Update**: 2026-11-15

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue)
![Maven](https://img.shields.io/badge/Maven-3.8+-red)
![MySQL](https://img.shields.io/badge/MySQL-8.4-blue)
![License](https://img.shields.io/badge/License-MulanPSL2-blue)

## Features Overview

| Module | Capabilities |
|--------|--------------|
| Checkout | Product search, cart, member phone lookup; cash/WeChat Pay/Alipay/bank card payment; receipt printing, cash drawer, shift handover; payment modes `disabled` / `mock` / `production`, with one-click simulated success in mock mode |
| Touch POS | Exact-match scanning or typing adds items to the cart automatically (with success/not-found/low-stock sounds), one-tap language switch, shift and clock in the status bar, handover flow aligned with the desktop version |
| Products & Inventory | Product CRUD, unique names (duplicate barcodes allowed), stock alerts and quick restocking, inventory checks, CSV import, Excel/PDF export |
| Members | Registration, lookup, recharge, points accumulation and automatic tier upgrades (Regular/Silver/Gold/Diamond), spending linked with returns |
| Hold & Resume | Park the current cart and resume it later from both the desktop and touch POS; the REST API creates/lists/resumes/cancels holds and lets administrators clean up old records or delete them, with server-side totals, and cashiers can only access their own holds |
| Promotions | Amount-off/percentage-off/coupons with automatic best-offer selection using the same pricing engine as checkout; REST API writes are limited to finance/admin, with a read-only quote endpoint |
| Purchasing | Suppliers, purchase orders and approvals, inbound stock and history, purchase reports |
| Returns | Returns based on original transactions with reasons, item conditions, and refund methods; inventory is restored after approval and refunded via cash, balance, or points |
| Reports | Transaction statistics plus purchasing/inventory/profit/return reports, with Excel and PDF export |
| Users & Auditing | Administrator/Cashier/Accountant roles, BCrypt passwords, password reset and forced change on first login, audit logs |
| Backup & Restore | Local backups (database/config/logs/invoices/business data), scheduled backups and retention policies; admin password required before restore; unimplemented cloud backup endpoints return 501 |
| REST API | Javalin on port `8080`, 100+ routes plus `/ws/sync` WebSocket, token authentication, role authorization, rate limiting, and security headers |
| Hardware | ESC/POS thermal and network printing, printer discovery and status checks, USB HID scanners, payment QR codes |
| i18n & Themes | Simplified Chinese/English/Traditional Chinese; LiSuan/Light/Dark themes; bundled Noto Sans CJK SC |

## Quick Start

### Prerequisites

- JDK 17 or higher
- Maven 3.8 or higher
- MySQL 8.4 (compatible with 8.0/8.3)
- Docker Compose (optional, for quick MySQL startup)

### Configure and Start the Database

Copy `.env.example` to a root-level `.env`, replace at least `MYSQL_ROOT_PASSWORD`, `CASHIER_DB_PASSWORD`, and `TOKEN_SECRET` with independent strong random values, then start the database:

```bash
cp .env.example .env
docker compose up -d mysql
```

Database passwords resolve in this order: **process environment variable → working-directory `.env` → `config/database.properties`**. `db.password` in the config file is always left empty, so the password is never written there. `docker/mysql-init/00-init-complete.sql` runs only when the data volume is empty; later schema upgrades happen automatically at application startup.

### First Run

On an empty database, the app shows a **first-run wizard** for the operator to create the administrator account and set its password. No default credentials ship with the repository.

### Run

```bash
# Development
mvn clean compile
mvn javafx:run

# Packaged JAR
mvn clean package
java -jar target/lisuan-fx-*-jar-with-dependencies.jar
```

On Windows, run `install.bat` for first-time setup, then start with `start.bat` (or `start.bat --gui`); `DataConfig.bat` is the database configuration tool. On Linux/macOS, run `./install.sh`, then `./start.sh`.

> Before starting the API in production, set a `TOKEN_SECRET` of at least 32 characters and restrict `CORS_ALLOWED_ORIGINS` to your real domains (`*` is not allowed).

## Shortcuts

| Shortcut | Action |
|----------|--------|
| `F1` | Add product to cart |
| `Delete` | Remove selected product |
| `Ctrl+L` | Clear cart |
| `F8` | Cash payment |
| `Ctrl+1` | WeChat Pay |
| `Ctrl+2` | Alipay |
| `Ctrl+3` | Bank card |
| `Ctrl+F` | Focus search box |
| `Ctrl+M` | Focus member phone field |
| `Ctrl+/` | Show shortcut help |

## Member Tiers

| Tier | Points Range | Discount |
|------|--------------|----------|
| Regular | 0-999 | No discount (10.0) |
| Silver | 1000-4999 | 5% off (9.5) |
| Gold | 5000-9999 | 10% off (9.0) |
| Diamond | 10000+ | 15% off (8.5) |

Discount formula: `amount * (tier discount / 10.0)`.

## REST API

The API listens on port `8080` by default and registers 100+ HTTP routes plus the `/ws/sync` WebSocket endpoint, covering authentication, products, members, transactions, hold orders, promotions, inventory, reports, settings, invoices, users, printing, payments, backup, i18n, and sync status.

Except for `/api/health`, `/api/auth/login`, and payment callbacks under `/api/payment/notify/*`, all API routes require authentication. Payment callbacks are invoked directly by channel servers and cannot carry a system token; their security relies on WeChat/Alipay signature verification.

## Security Highlights

- Passwords are stored with BCrypt; repeated login failures are counted per user and locked
- All SQL uses `PreparedStatement`; security-sensitive randomness uses `SecureRandom`
- Payment callbacks require signature verification, amount validation, and terminal-state protection (duplicate callbacks are idempotent)
- Token authentication (24-hour expiry), three-tier role authorization, 60 requests per IP per minute
- Optimistic locking on stock and member balances prevents overselling and over-drawing

## Database

The project uses MySQL with HikariCP. Tables are created and upgraded automatically at application startup; the full initialization script is `docker/mysql-init/00-init-complete.sql`, and version notes are in `docker/mysql-init/DATABASE_VERSIONS.md`.

## Related Documents

- [CLAUDE.md](CLAUDE.md) — architecture, migration status, and development conventions
- [AGENTS.md](AGENTS.md) — quick reference and verification commands
- [docs/GO_LIVE_CHECKLIST.md](docs/GO_LIVE_CHECKLIST.md) — go-live checklist
- [docs/HARDWARE_ACCEPTANCE.md](docs/HARDWARE_ACCEPTANCE.md) — hardware acceptance tests (printer/scanner/cash drawer/touch screen/customer display)
- [docs/CREDENTIALS_CHECKLIST.md](docs/CREDENTIALS_CHECKLIST.md) — credential preparation checklist (database/API/payment/backup)
- [docs/DATABASE_INIT.md](docs/DATABASE_INIT.md) — database initialization guide
- [docker/README.md](docker/README.md) — Docker MySQL deployment and backup

## Recent Updates

### v2.6.0

**2026-08-29**
- Print settings wired: Settings-page "Enable Print / Printer Name / Paper Size / Logo" now applies to touch-POS receipts (no receipt when disabled), plus a "Test Print" button
- Added a "Scanner Test" field to the Settings page with the same validation rules (normalization, blank data, 128-char limit)

**2026-08-28**
- Unified the database password variable to `CASHIER_DB_PASSWORD` (legacy spelling still supported); config files no longer store plaintext passwords and read it from `.env` / environment variables
- Docker initialization SQL no longer hardcodes the database name and supports a custom `MYSQL_DATABASE`

**2026-08-23**
- Mock payment mode offers a "Simulate Payment" button for closed-loop testing; callbacks now validate amounts, handle duplicate callbacks idempotently, and reject late callbacks for terminal orders
- Restoring a backup now requires administrator password confirmation
- Touch POS: exact-match scanning with sounds, handover exit flow, status bar, simulate-payment button; desktop exit returns to the login screen
- Fixed payment callbacks being blocked by authentication, a health-check connection leak, and an electronic payment dialog NPE

**2026-07-24**
- Touch POS supports one-tap switching between Simplified Chinese/English/Traditional Chinese, with the choice applied globally
- Fixed language switching not taking effect; new users automatically follow the system language
- Touch POS prompts to finish payment in cash when cash is partially used, preventing accounting errors

Feature and fix records for older versions are available in the Git history.

## Troubleshooting

**Application fails to start**: verify JDK 17+, make sure MySQL is running, and check the logs under `logs/`.

**Database connection error**: make sure `CASHIER_DB_PASSWORD` is provided via the environment or the root `.env` (the config file never stores it) and that `DB_HOST`/`DB_PORT`/`MYSQL_*` match your setup; for Docker, run `docker compose up -d mysql` first.

**API service fails to start**: verify `TOKEN_SECRET` is at least 32 characters, `CORS_ALLOWED_ORIGINS` is not `*`, and check the logs for port conflicts.

**Scanner unresponsive**: confirm the scanner is in USB HID mode and that focus is not held by another input field.

**Printer unresponsive**: check the connection, driver, and paper; for network printing, verify IP, port, and firewall rules.

**Chinese text renders incorrectly**: confirm the bundled Noto Sans CJK SC font is packaged; on Linux you can also install a system CJK font package.

---

## License

Mulan Permissive Software License v2 (MulanPSL2)

**Repository**: https://gitee.com/nevell/lisuan.git

**Issues**: https://gitee.com/nevell/lisuan/issues
