# AGENTS.md

LiSuan (狸算) is a **single-module** Java 17 / JavaFX 17 POS desktop app (Maven, package `com.cashier`) with an embedded Javalin REST API (port 8080). Full architecture, business rules, migration status: **`CLAUDE.md`** is the authoritative companion doc — read it for anything beyond the quick facts below.

> Both `AGENTS.md` (this quick-facts sheet) and `CLAUDE.md` (authoritative detail) are tracked.
> Keep them consistent: the version-sync source list and the gate list must match — a policy test
> (`com.cashier.security.InstructionsDocPolicyTest`) enforces the parts that drift most often.

## Verification commands

```bash
mvn -q clean compile          # fast compile check
mvn test -Dtest=FooTest       # single class (also works: FooTest#method)
mvn test                      # run suite (skips SpotBugs/JaCoCo)
mvn -q -DskipTests spotbugs:check   # static analysis gate only
mvn verify                    # FULL gate: tests + SpotBugs + JaCoCo
mvn javafx:run                # run app (needs display + running MySQL)
```

**Gotchas:**
- `mvn verify` runs THREE gates: Surefire tests, SpotBugs `High` check (verify phase, `config/spotbugs-exclude.xml`), and a **JaCoCo ≥10% line-coverage gate**. It can fail with all tests green (e.g. a large refactor adds untested lines).
- Enforcer fails unless Java ≥ 17 and Maven ≥ 3.8; dependencies resolve from Aliyun mirror (needs network).
- CI: `.github/workflows/build.yaml` runs `xvfb-run -a mvn -B -ntp verify` on push/PR to `main` (same three gates as local). `sync.yaml` backs up and restores `.github/workflows`, so it survives Gitee syncs.
- `full test` reporting: `mvn verify` must be green (test count grows; read `Tests run:` from the output). Most output is noise, use `-q`.
- **Do not add tests named like `LoginControllerUITest`** or rely on default `mvn test` for UI tests: Surefire excludes `**/LoginControllerUITest` unless run as `mvn -Pui-tests -Dtest=LoginControllerUITest test` (needs a real display; hangs headless/CI).
- Tests use H2 in-memory (`jdbc:h2:mem:...;MODE=MySQL`) via `DatabaseTestBase` — DAO/Service tests call `initTestDatabase()`. MySQL-specific SQL won't run in tests.

## Database & config

- Schema is auto-created/upgraded **in Java** on every startup by `util/DatabaseManager.initializeDatabase()` (`CREATE TABLE IF NOT EXISTS` + idempotent `ALTER` in try/catch, e.g. `products.is_hot`). When adding a table: make that change **here** (executable source of truth), not just in a SQL file. `docker/mysql-init/00-init-complete.sql` is the full init; the `07-`/`08-`/`09-`/`10-`/`11-`/`99-` scripts are for manual upgrades (not auto-mounted by compose).
- MySQL conn comes from `config/database.properties` (gitignored; copy `config/database.properties.example`). Password can be overridden at launch with `CASHIER_DB_PASSWORD`/`CASHER_DB_PASSWORD`.
- `docker compose up -d mysql` **fails unless `.env` exists** (compose uses required-var expansion `MYSQL_ROOT_PASSWORD?...`). Copy `.env.example` → `.env` and set real secrets.
- Default login: `admin` / `admin123`.
- `config/*.properties`, `.env`, `logs/*.log`, `target/` are gitignored — never commit secrets.

## Non-obvious conventions

- **Logging:** always `LoggerFactoryUtil.getLogger(...)`; never `LoggerFactory.getLogger(...)` or `System.out`.
- **DAO pattern:** static `ProductDAOCold` removed. Use interface via `com.cashier.dao.ProductDAORefactored` + `DAOFactory.getInstance().getProductDAO()` (which returns instance-based DAOs) seeded in `DAOFactory.registerDefaults()`. New DAOs extend `BaseDAO`, register themselves.
- **Security:** BCrypt for passwords; never `Math.random()` for order/token IDs (use `SecureRandom`); always `PreparedStatement`.
- **i18n:** each new visible string needs a key in `I18nKeys` **and all four bundles**: `messages_zh_CN.properties`, `messages_zh_TW.properties`, `messages_en.properties` **and the fallback `messages.properties`** (all four must have identical key sets; do not add languages). FXML uses `%key`. Unused keys are rejected by `I18nUnusedKeyPolicyTest`.
- **Version bumps** require syncing 4 files: `AppConstants.APP_VERSION`, `pom.xml` `<version>`, `installer/Installer.java` `APP_VERSION`, `.env.example` `APP_VERSION`.
- **Maven resource filtering** applies to `.properties`, `.fxml`, `.css`, `.png`, `.jpg`, `.xml` in `src/main/resources` — `${...}` in those files is substituted at build; keep placeholders explicit.
- Product **names must be UNIQUE** (DB constraint + app validation), barcodes may repeat.
