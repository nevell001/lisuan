# LiSuan 收银系统 —— Windows 运行审计报告

- **审计日期**：2026-09-24（第一轮）；2026-10-08（第二轮复验，见文末）
- **代码版本**：第一轮 `main` @ `b09dc9b`；第二轮 `main` @ `75edc45` → `56388c3`（v2.6.0）
- **审计主机**：Windows 11（x64）
- **审计方式**：全新搭建 JDK/Maven/MySQL → 真机编译 → 全量测试 → 打包 → **真实启动应用并连库操作**

> 本报告第一轮（2026-09-24）成稿于源码树外，后随 `a2b5afe` 提交至仓库根目录；第二轮（2026-10-08）直接增补于文末。
> 两轮审计结束时 `git status` 均为干净状态（`.env`、`config/database.properties`、`target/`、`logs/` 均为 gitignore 内文件）。

---

## 一、结论摘要

**项目在 Windows 上可以完整构建、测试、打包并真实运行。** 核心链路（连库、建表、迁移、字体、登录、主界面、触屏收银台、语言切换、增删用户、退出清理）均已在真机验证通过。

但审计发现 **6 个真实缺陷（3 个高影响）**，其中 3 个**只在真实 MySQL 上暴露、CI 的 H2 测试无法发现**：

| ID | 严重度 | 缺陷 | 影响面 |
|---|---|---|---|
| **D1** | 🔴 高 | `release.bat` 静默跳过全部门禁，仍返回 0 | Windows 发布流程安全门禁形同虚设 |
| **D2** | 🔴 高 | `users` 时间戳写入 `Timestamp` 到 `BIGINT` 列 → **显示为 2612 年** | 用户管理、最后登录时间 |
| **D3** | 🔴 高 | `language_preferences` 外键导致全局默认语言**永远写不进去** | 触屏收银台语言切换 |
| **D4** | 🟠 中 | `start.bat` 在 JavaFX 未找到时传 `--module-path ""` → JVM 直接启动失败 | Windows 一键启动 |
| **D5** | 🟠 中 | 全新安装（无 `config/database.properties`）启动即退出，且**不弹任何错误框** | 首次安装体验 |
| **D6** | 🟡 低 | `create-shortcut.bat` 同类解析缺陷，分支错乱执行 | 桌面快捷方式 |

外加一批文档/代码不一致与工程性问题，见第五节。

---

## 二、测试环境与方法

审计机上**原本没有任何 JDK / Maven / Docker / MySQL**，且 `schannel` TLS 在本机不可用（`SEC_E_NO_CREDENTIALS`）。全部依赖按以下方式就地搭建（**未污染系统 PATH，全部落在临时目录**）：

| 组件 | 版本 | 来源 |
|---|---|---|
| JDK | Temurin **17.0.20.1+1** | 清华 TUNA Adoptium 镜像 |
| Maven | **3.9.9** | 阿里云 Maven 镜像 |
| MySQL | **8.4.6** (winx64 zip，`--initialize-insecure`) | 官方 CDN |

- Maven 本地仓库与 `settings.xml` 重定向到临时目录（本机 `~/.m2` 受沙箱限制），依赖经阿里云镜像解析；解析渠道不影响构建结果。
- MySQL 用项目自带的 `docker/mysql-init/00-init-complete.sql` 初始化（**未用 Docker**），并创建 `lisuan`/`lisuan_system` 用户库，与 compose 的初始化等价。

---

## 三、通过项（含证据）

### 3.1 编译

```
mvn clean compile   →  BUILD SUCCESS
  Compiling 229 source files with javac [debug target 17] to target\classes
```
> 仅有既有告警：`RechargeController` 过时 API、`FormValidator` 未检查操作。

### 3.2 全量门禁（`mvn verify`）

```
Tests run: 647, Failures: 0, Errors: 0, Skipped: 0
spotbugs:check  →  BugInstance size is 0 / Error size is 0 / No errors/warnings found
jacoco:check    →  Analyzed bundle 'lisuan-fx' with 371 classes / All coverage checks have been met.
BUILD SUCCESS (01:57 min)
```
与 `.github/workflows/build.yaml` 的 `xvfb-run mvn verify` 完全一致（Windows 有真实桌面，无需 Xvfb）。

### 3.3 打包产物

- `target/lisuan-fx-2.6.0-jar-with-dependencies.jar` = **85.8 MB**
- 清单正确：`Main-Class: com.cashier.Launcher`，并带 `Add-Opens` / `Add-Exports`
- JavaFX 以 `win` 分类器正确入包（`javafx-base/controls/fxml/graphics/media-17.0.12-win.jar`）
- **`java -jar` 无需 module-path 即可启动**（`Launcher` 入口设计生效）

### 3.4 真实运行（连真实 MySQL 8.4.6）

从项目根目录 `java -jar target\lisuan-fx-2.6.0-jar-with-dependencies.jar`：

```
已加载数据库配置: config/database.properties
已从环境变量或 .env 读取数据库密码          ← 读的是 CRLF 换行的 .env，解析正确
HikariPool-1 - Start completed.             ← 成功连上 MySQL 8.4.6
检查表结构... 正在为 promotions 表添加 promotion_code 字段... 正在创建 login_attempts 表...
MySQL 数据库初始化成功
成功加载 Noto Sans CJK SC Regular / Bold 字体
支付服务初始化成功，模式: disabled
REST API 服务器已禁用                        ← 安全默认生效
用户 系统管理员 (管理员) 进入完整主界面
库存预警服务 / 自动备份服务 已启动
```

界面渲染正常（中文字体、主题、侧边栏 15 个功能页签、用户表格、状态栏班次/日期）：

> 截图：`%TEMP%\lisuan-mainview.png`（审计时抓取的应用主窗口）

另外真机验证通过：
- 触屏收银台（`cashier` 角色）正常进入，标题 `狸算(LiSuan)收银系统 - 触屏收银台 - 张慧敏`
- 单实例锁生效、退出时正确释放
- **退出清理干净**，无异常：`单实例锁已释放` / `应用程序已停止`，进程退出码 0

---

## 四、缺陷详情

### D1 🔴 `release.bat` 静默跳过全部门禁（cmd.exe 解析缺陷）

**现象**：Windows 上执行 `release.bat`，输出停在 `[1/3]`，`[2/3]` / `[3/3]` 完全没跑，**退出码却是 0**。

```
[0/3] Checking version consistency...
Version numbers match (2.6.0)
[1/3] Running full verification gate (mvn clean verify)...
...                                   ← 到此为止，无任何报错
```

**根因**：`release.bat:54` 的 `if %ERRORLEVEL% NEQ 0 (` 块，其**块体内**第 55 行有一个**未转义的右括号**：

```bat
54: if %ERRORLEVEL% NEQ 0 (
55:     echo FAILED: full verification (tests/SpotBugs/coverage/package)     ← 这里的 ')' 提前"闭合"了 if 块
56:     exit /b %ERRORLEVEL%
57: )
58: echo Full verification passed (tests/SpotBugs/coverage/package)
```

cmd.exe 扫描块结束符时把第 55 行的 `)` 当成块结尾，批处理解析器偏移错乱，**第 58 行之后的内容被整段丢弃**。又因为成功路径下条件为假、块体不执行，所以既不报错也不打印任何失败信息，直接以 0 退出。

**最小复现**（已实测，三组对照）：

| 变体 | 括号位置 | 结果 |
|---|---|---|
| V1 | 只在 if 块**内**的 echo（＝现状） | **ABORTED-EARLY** |
| V2 | 只在 if 块**外**的 echo | REACHED-END ✅ |
| V3 | 无括号 | REACHED-END ✅ |

**影响**：`release.bat` 的 `[2/3]`、`[3/3]` 是**全部发布安全门禁**——可执行 JAR 存在性、数据库 SSL 配置、`config/database.properties` 不得存 `db.password`、`CASHIER_DB_PASSWORD` 必须设置、API 的 `TOKEN_SECRET` 强度与 CORS 白名单、支付占位凭据、敏感信息泄漏扫描。**这些在 Windows 上从未真正执行过**，而返回值 0 会让任何 `if errorlevel` / CI 包装脚本误判为"通过"。

**修复验证**（已实测）：把第 55、58 行的括号转义为 `^(` `^)` 后，脚本完整跑完并打印 `LiSuan Release Verification PASSED`，门禁全部生效：

```
[2/3] Checking release configuration...
Executable JAR generated
Database password and SSL configuration passed
API disabled, skipping API secret and CORS release gate
No obvious local password leak found
```

**建议**：转义为 `^(` / `^)`；或改写为不含括号的文案；并补一条"`release.bat` 必须执行到 PASSED 横幅"的回归门禁（例如断言输出含 `[3/3]`）。

---

### D2 🔴 用户时间戳被写成 `YYYYMMDDHHMMSS`，界面显示 **2612 年**

**现象**：用户管理表格中，"最后登录"显示为 **`2612-01-17 18:28`**；通过"添加用户"新建的用户，"创建时间"同样是 2612 年。（真机截图可见，DB 实测亦一致。）

**根因**：生产库把两列定义成 **BIGINT 存 epoch 毫秒**，而 DAO 有的地方写 `long`、有的地方写 `java.sql.Timestamp`：

| 位置 | 写法 | 实际落库 |
|---|---|---|
| `00-init-complete.sql:55-56,252` | `last_login_time BIGINT` / `create_time BIGINT`；`UNIX_TIMESTAMP() * 1000` | `1790218286000`（毫秒，正确） |
| `DatabaseManager.java:1274` | `pstmt.setLong(7, currentTime)` | 毫秒（正确） |
| `UserDAORefactored.java:77,84,128` | `new Timestamp(user.createTime.getTime())` | **`20260924105609`** ❌ |
| `UserDAORefactored.java:96-106` | `new Timestamp(System.currentTimeMillis())` | **`20260924105350`** ❌ |
| `UserDAORefactored.java:167-169` | `new Date(num.longValue())` 当作 epoch 毫秒读 | → **2612-01-17** ❌ |

MySQL 把"时间类型 → 整数列"按 `YYYYMMDDHHMMSS` 数值化；Java 侧再把它当成 epoch 毫秒，`20260924105350 ms` ≈ 1970 + 642 年 = **2612 年**。

**实测复现**（用应用同款连接参数，含 `useServerPrepStmts=true`）：

```
INSERT with Timestamp -> OK
UPDATE with Timestamp -> rows=1
  tsprobe_ins  create_time=20260924105656   last_login_time=20260924105656
  admin        create_time=1790218286000    last_login_time=20260924105656
  readDateColumn() -> Fri Jan 17 18:28:25 CST 2612
```

> 顺带发现：若把该连接参数换成默认的**客户端预处理**，同一 `INSERT`/`UPDATE` 会直接抛
> `SQLException: Data truncated for column 'create_time'`（MySQL 8.4 严格模式）。
> 也就是说这段代码对连接参数**极其敏感**，只因为 HikariCP 配了 `useServerPrepStmts=true` 才"侥幸不报错"，转而变成静默脏数据。

**为什么 CI 发现不了**：H2 测试库的 schema 与生产库**不一致**——

| 列 | 生产（MySQL） | 测试（`DatabaseTestBase.java:116-117`） |
|---|---|---|
| `create_time` | `BIGINT` | `TIMESTAMP` |
| `last_login_time` | `BIGINT` | `TIMESTAMP` |

`UserDAOTest` 断言 `lastLoginTime` 落在 `[before, after]` 区间，在 H2 的 `TIMESTAMP` 列上**恒真**，因此 647 个测试全绿。

**建议**：`UserDAORefactored` 统一改 `setLong(...getTime())`；同时把测试 schema 改成与生产一致（或对生产 schema 加一致性门禁——项目已有 `ProductionSchemaConsistencyTest`，可扩展覆盖列类型）。

---

### D3 🔴 全局默认语言**永远写不进库**（外键约束）

**现象**：触屏收银台点"语言切换"→ 日志报错 3 次，看板无提示：

```
ERROR com.cashier.service.DataService - 保存语言偏好失败
java.sql.SQLIntegrityConstraintViolationException: Cannot add or update a child row:
  a foreign key constraint fails (`lisuan_system`.`language_preferences`,
  CONSTRAINT `language_preferences_ibfk_1` FOREIGN KEY (`username`) REFERENCES `users` (`username`) ON DELETE CASCADE)
  at com.cashier.dao.LanguagePreferenceDAORefactored.setLanguagePreference(LanguagePreferenceDAORefactored.java:64)
  at com.cashier.service.DataService.saveLanguagePreference(DataService.java:382)
  at com.cashier.controller.TouchCartController.switchLanguage(TouchCartController.java:1853)
```

**根因**：`00-init-complete.sql:441-447` 给 `language_preferences.username` 加了指向 `users(username)` 的外键；而该表用 **字面量 `'default'`** 存"全局默认偏好"（`LanguagePreferenceDAORefactored.java:35` 读、`DataService` 写），`'default'` 并不是 `users` 里的用户 → 每次写全局默认必然违反外键。

**影响**：
- 全局默认语言（`username='default'`）**永远无法更新**，所有未单独设置偏好的用户只会落到 `zh-CN`；
- 每次触屏切语言都产生一条 ERROR 日志（掩盖真实错误）；
- `TouchCartController` 注释声称"同时保存用户偏好与全局默认"，实测**只写进了用户自身那一行**。
  真机数据库佐证（`language_preferences` 只有当前用户，没有 `default` 行）：

  ```
  +----------+--------------+---------------+---------------+
  | username | language_tag | currency_code | updated_at    |
  +----------+--------------+---------------+---------------+
  | user01   | zh-CN        | CNY           | 1790218732292 |   ← 只有这一行
  +----------+--------------+---------------+---------------+
  ```
  （注意 `updated_at` 用 `setLong` 写入，是**正确**的 epoch 毫秒；写坏的只有 D2 的 `users` 两列。）

**为什么 CI 发现不了**：`DatabaseTestBase.java` 里**根本没有 `language_preferences` 表**（grep 无匹配），`LanguagePreferenceDAORefactoredTest` 是在测试内部自建的无外键表，因此回退链测试通过。

**建议**：二选一——① 去掉该外键（或改为 `ON DELETE SET NULL` 并允许 `NULL` 表示全局），② 为 `'default'` 建一条哨兵用户行。推荐 ①，并在测试 schema 里补上与生产一致的外键以形成回归门禁。

---

### D4 🟠 `start.bat` 的 JavaFX 回退分支传 `--module-path ""`，JVM 直接拒绝启动

**现象**：当 `%USERPROFILE%\.m2\repository\org\openjfx` 下找不到 JavaFX jar 时：

```
[WARNING] JavaFX not found in Maven repository
[INFO] Will use standard classpath
...
java --module-path "" --add-modules javafx.controls,javafx.fxml,javafx.graphics ... -jar "...jar"
```

**实测**：空的 `--module-path` 是 JVM **硬错误**，不是"无操作"：

```
> java --module-path "" -version
Error: --module-path requires module path specification     (exit 1)
```

**触发条件很常见**：① 只拿到 shaded JAR、没跑过 Maven；② Maven 本地仓库被改到别处（自定义 `settings.xml` 的 `<localRepository>` 或 `-Dmaven.repo.local`）；③ `pom.xml` 的 `javafx.version` 一旦升级，`start.bat:145` 硬编码的 **17.0.12** 路径立刻失配 → 必然走进该分支。

**对照**：`start.sh` 用 `JFX_MODULES=""`（空变量展开为"无参数"）并在未找到时给出 Warning——**没有这个问题**。可见是 `start.bat` 独有的缺陷。

**建议**：把 `JFX_PATH` 为空的情况改为拼接出**不含** `--module-path` 的完整命令（`start.sh` 的 `JFX_MODULES` 写法可直接照搬）。

---

### D5 🟠 全新安装启动即退出，且用户看不到任何错误

**现象**（隔离目录实测，`.env` 存在但 `config/database.properties` 不存在）：

```
10:59:30.331  数据库配置不存在，启动配置向导
10:59:30.543  WARN 用户取消配置，退出应用        ← 仅 212ms 后
10:59:30.543  INFO 数据库配置完成
10:59:30.608  配置文件不存在，创建默认配置文件模板
10:59:30.624  已创建默认配置文件模板: config/database.properties
10:59:30.627  ERROR 数据库初始化失败，系统将终止启动
Exception in thread "JavaFX Application Thread" java.lang.ExceptionInInitializerError: ...
```

**根因（两处叠加）**：

1. **配置向导根本没机会显示**：`CashierSystemFXApplication.java:799-801` 用 `SwingUtilities.invokeAndWait(() -> DatabaseConfigDialog.main(...))`，而 `DatabaseConfigDialog.main`（`DatabaseConfigDialog.java:43`）只做了 `SwingUtilities.invokeLater(...)` 就返回——`invokeAndWait` 立刻返回，紧接着第 807 行检查文件仍不存在，于是打印"用户取消配置"并 `Platform.exit()`。
2. **错误是 `Error` 而非 `Exception`，没被捕获**：`initializeApplication` 抛出的 `ExceptionInInitializerError` 是 `Error`，`start()` 里的 `catch (Exception e)`（`CashierSystemFXApplication.java:108`）**不接**，因此 `showStartupFailure()` 从不执行。用 `javaw` 或双击启动时**完全没有可见反馈**，应用只是"闪一下就没了"。

**附带副作用**：退出路径漏了 Swing/AWT 事件线程——实测该进程在日志已打印"应用程序已停止"后**仍存活 5 分钟以上**，还挂着一个 `LiSuan - Database Configuration` 的 Swing 窗口（需手工 kill）。另外它会写下一份**空密码**的 `config/database.properties`，导致**第二次启动**改为报"数据库配置不完整"，错误现象前后不一致，难以排查。

**建议**：让 `DatabaseConfigDialog` 提供可等待的模态 API（直接 `show()` 并阻塞，或返回完成标志）；把 `catch (Exception e)` 放宽为 `catch (Throwable e)` 或显式补 `catch (Error e)`；退出时确保 `System.exit`/关闭 AWT。

---

### D6 🟡 `create-shortcut.bat` 同类括号缺陷，分支错乱执行

`create-shortcut.bat:18` 的 `echo [Info] Using Quick Start launcher (recommended)` 位于 `if exist ... (` 块**内**（第 16-27 行 `if / else if` 链），与 D1 同因。

**实测**（隔离片段）：

```
[Info] Using Run CashierSystem launcher      ← 本不该匹配（该文件不存在）
[Info] Using start.bat launcher
The system cannot find the batch label specified - no_start
```

即**多个互斥分支同时执行**，输出误导；最终 `TARGET_SCRIPT` 恰好落到 `start.bat` 才能用，属"侥幸正确"。

---

## 五、文档 / 代码不一致与工程性问题

| # | 问题 | 位置 | 说明 |
|---|---|---|---|
| C1 | **`MYSQL_PASSWORD` 其实会被读取** | `start.bat:19`、`release.bat:45` | `CLAUDE.md` 与 `.env.example:31-33` 都断言"旧 `.env` 里的 `MYSQL_PASSWORD` 已不再被任何一方读取"。实测 `start.bat` 会把 `MYSQL_PASSWORD` 映射成 `CASHIER_DB_PASSWORD`（`[SHIM-JAVA] CASHIER_DB_PASSWORD=legacy_pw_from_env`）。**对 docker compose / 应用 `DotEnv` 的表述正确，对启动/发布脚本错误**——要么改文档，要么去掉这两处兼容分支。 |
| C2 | **`install.bat` 的 `DB_USER` / `DB_PASSWORD` 是死代码** | `install.bat:37-43` | 只赋值，全文再无引用。实际连接参数由 `DatabaseConfigDialog` 决定。 |
| C3 | **`jvm.config.example` 引用不存在的文件** | `config/jvm.config.example:60` | `-Djava.util.logging.config.file=config/logging.properties`，但 `config/logging.properties` 不存在（项目用 logback）。 |
| C4 | **示例配置默认开启断言** | `config/jvm.config.example:54` | 生产 JVM 配置带 `-ea`，任何 `assert` 失败都会中断业务流程。 |
| C5 | **Windows 上主动关闭 DPI 缩放** | `CashierSystemFXApplication.java:823-829` | 设置了 `sun.java2d.win.uiScaleX/Y=1.0`，与同处 `dpiaware=true` 语义冲突，高分屏下界面会偏小/发虚；且第 825、826 行**重复设置同一属性**。`jvm.config.example:36` 又设了一遍 `dpiaware`。建议只保留 `dpiaware=true`，交给系统缩放。 |
| C6 | **缺少 `.gitattributes`** | 仓库根 | `git` 中存的是 LF，本机 `core.autocrlf=true` 检出后 `*.sh`、`docker/my.cnf`、`*.bat` 全变 CRLF。① `docker/my.cnf` 是 **bind-mount 进 Linux 容器**的，CRLF 有解析风险；② `*.bat` 若被以 LF 检出（`autocrlf=false`/`input`），`goto :label` 可能失效。建议显式声明：`*.sh text eol=lf`、`*.bat text eol=crlf`、`my.cnf text eol=lf`。 |
| C7 | **API 安全告警每次启动都刷屏** | 实测日志 | `config/api.properties` 不存在时 `ApiConfig` 连打 10 行 `安全警告: 使用默认 TOKEN_SECRET` / `CORS 允许所有来源 (*)`，随后才说"REST API 服务器已禁用"。默认关闭时这些告警是噪声，建议仅在 `api.enabled=true` 时告警。 |
| C8 | **shaded JAR 仍带 `Class-Path`** | 打包产物 | 清单里保留了 65 个**并不存在**的兄弟 jar 名。不影响启动，但属噪声，且若分发目录里恰好有同名 jar 会被意外加载。 |
| C9 | `jacoco.exec` 未被 gitignore | `.gitignore` | 正常构建落在 `target/`（已忽略）；审计中一次异常调用在仓库根留下过 `jacoco.exec`（已清理）。可加一条 `jacoco.exec` 规则兜底。 |

### 已核实**不是**问题的项（避免误改）

- `release.bat:74/80` 的 `findstr /R /C:"db.password=..*"`：实测 `/R` 与 `/C` 同用时**正则生效**（有密码的文件匹配返回 0，空密码返回 1），SSL 与密码门禁的正则写法**正确**。
- `start.bat` 主体逻辑（`.env` 定向读取、jar 自动探测、`config/jvm.config` 生成、JVM 参数拼装、JavaFX 存在时的模块路径拼装、`--gui` 分支）实测**全部正常**。
- `.env` 的 **CRLF** 解析正常（`DotEnv` 用 `readAllLines` + `trim`）。
- Unicode/`chcp 65001` **不是** D1 的原因（已用"去 `chcp`"与"剥离全部非 ASCII"两组对照排除）。

---

## 六、覆盖范围与限制

**已覆盖**：编译、全量单测/SpotBugs/覆盖率门禁、打包产物与清单、真实 MySQL 建库建表与迁移、桌面端启动→登录→主界面、触屏收银台、语言切换、用户增删、单实例锁、干净退出、`start.bat`、`release.bat`、`create-shortcut.bat`、`install.bat`（静态）、`DataConfig.bat`（静态）、`diagnose.bat`（静态）。

**未覆盖 / 建议后续**：
- **打印机 / 扫码枪 / 钱箱等硬件**：本机无设备，仅跑了既有单测与降级路径。
- **微信 / 支付宝真实支付**：`payment.mode=disabled`，未接真实渠道。
- **jpackage 安装包**（`mvn jpackage:jpackage`）：需 WiX Toolset 与 jlink 运行时镜像，本次未构建。
- **REST API 端到端**：`api.enabled=false`（安全默认）。建议在隔离环境按 `CLAUDE.md`「REST API 启用步骤」做一次本地冒烟。
- **TestFX UI 自动化**：`LoginControllerUITest` 按设计被 surefire 排除，本次未单跑（`mvn -Pui-tests`）。
- **备份/恢复演练**（`scripts/db-restore-drill.sh` 为 bash，Windows 无对应 `.bat`）。

---

## 七、建议修复顺序

1. **D1**（一行转义，收益最大——恢复整个 Windows 发布门禁）
2. **D2**（改 `setLong`，并同步测试 schema，避免脏数据继续产生）
3. **D3**（改外键或加哨兵行，恢复全局语言偏好）
4. **D4**（照 `start.sh` 改 `start.bat`，消除一键启动的硬失败分支）
5. **D5**（配置向导改为可等待 + `catch (Throwable)`，保证首次安装有可见反馈）
6. **D6 / C1–C9**（按需）

> 建议全流程配一条回归门禁：**至少一个测试类连真实 MySQL**（或校验测试 schema 与 `docker/mysql-init` 的列类型一致）。
> D2 与 D3 **已经证明 H2 与生产 schema 的分叉会系统性地放过真实缺陷**——这是本次审计最值得优先补的结构性缺口。

---

## 八、修复记录（同日完成并验证）

D1–D6 与 C1–C9 已全部修复；修复后 `mvn clean verify` 全绿，并在**真实 MySQL 8.4.6** 上复验。

| ID | 修复内容 | 改动位置 | 验证方式与结果 |
|---|---|---|---|
| **D1** | `if` 块内 `echo` 的括号转义为 `^(` `^)` | `release.bat:55,58` | 桩 mvn 跑 `release.bat`：现完整输出 `[1/3]`→`[3/3]` 并以 `LiSuan Release Verification PASSED` 结束；门禁真正生效（JAR / SSL / 密码 / 泄漏扫描都有输出） |
| **D2** | 时间列统一写 epoch 毫秒（`toEpochMillis`，null 安全）；新增历史脏数据修复迁移；H2 测试库两列改 `BIGINT` | `UserDAORefactored`、`DatabaseManager.repairUserTimestampColumns`、`DatabaseTestBase` | 真机启动：`检测到 users 时间列脏数据 2 行，正在修复为 epoch 毫秒` → 2612 年时间被还原成 `2026-09-24 10:56:09` / `10:58:22`（**原有时刻精确保留**）。变异验证：把 `Timestamp` 改回去，H2 直接报 `Data conversion error converting "TIMESTAMP to BIGINT"`（14 项失败），静态门禁同时变红 |
| **D3** | 三张偏好表去掉 `username` 外键；新增老库删外键迁移；并补上 `DatabaseManager` 里缺失的 `language_preferences` 建表 | `00-init-complete.sql`、`DatabaseManager` | 真机启动日志逐个删除 3 个外键；随后用 DAO 原语句写入 `username='default'` **成功**（修复前必违反外键）。变异验证：把外键加回去，门禁立即变红 |
| **D4** | JavaFX 缺失时改拼 `JFX_MODULES`（为空则完全不传 `--module-path`），照 `start.sh` 的写法 | `start.bat` | 桩测试：ARGS 中已无 `--module-path`，`java -jar` 参数正确 |
| **D5** | 配置向导改为阻塞等待（`showAndWait`）；取消/失败干净返回；启动异常改 `catch (Throwable)`；失败弹窗改 `Platform.runLater` | `DatabaseConfigDialog`、`CashierSystemFXApplication` | 真机全新目录启动：不再出现"用户取消配置"+`ExceptionInInitializerError` 堆栈，也不再写空密码模板；向导真正停住等用户操作（实测由使用者在向导内完成配置并正常退出） |
| **D6** | 同样转义括号 | `create-shortcut.bat:18` | 隔离片段：只输出一条 `Using start.bat launcher`（修复前两个互斥分支会同时执行），`TARGET_SCRIPT` 正确 |
| **C1** | **改文档而不是改代码**：`MYSQL_PASSWORD` 其实是**全项目一致**的旧变量兼容（4 份 README、`start/release/install` 脚本、`docker-init.sh`、`backup-db.sh` 都读） | `CLAUDE.md`、`.env.example` | 改为准确表述：只有 compose 与应用 `DotEnv` 不认它 |
| **C2** | 删除从未被引用的 `DB_USER` / `DB_PASSWORD` 赋值 | `install.bat` | 全文已无引用点 |
| **C3/C4** | 删除引用不存在文件的 `-Djava.util.logging.config.file` 与生产配置里的 `-ea` | `config/jvm.config.example` | 桩测试的 JVM_OPTS 已不含这两项 |
| **C5** | 去掉重复的 `dpiaware` 与把缩放强行按回 100% 的 `sun.java2d.win.uiScaleX/Y` | `CashierSystemFXApplication.main` | 保留 `dpiaware=true`，缩放交还系统 |
| **C6** | 新增 `.gitattributes`（`*.sh`/`my.cnf` 强制 LF、`*.bat` 强制 CRLF、字体等二进制） | `.gitattributes` | 见下方落地说明 |
| **C7** | API 安全告警仅在 `api.enabled=true` 时输出 | `ApiConfig` | 默认关闭时启动日志不再刷 10 行告警 |
| **C8** | 瘦 jar 不再写 `Class-Path`（shaded 清单里 65 个不存在的兄弟 jar 消失） | `pom.xml` | 已核验瘦 jar 无任何引用方；打包后清单只剩 `Main-Class` + `Add-Opens`/`Add-Exports` |
| **C9** | `.gitignore` 增加 `jacoco.exec` | `.gitignore` | 仓库根的异常残留不再出现在 `git status` |

### 修复过程中新发现、并一并修掉的问题

审计报告之外，修复与复验过程又暴露了 3 个真实缺陷：

1. **`DatabaseManager` 从不创建 `language_preferences`** —— 只有 docker 初始化 SQL 建它。走"应用自建库"的部署会因缺表而**静默丢失语言/货币偏好**（DAO 抛异常被 `DataService` 吞掉、只记 ERROR）。已补建表（不带外键）。
2. **`showStartupFailure` 的弹窗根本弹不出来** —— 它被从 `PauseTransition` 的动画回调里同步调用，JavaFX 会抛 `IllegalStateException: showAndWait is not allowed during animation or layout processing`。即"启动失败给用户可见提示"这条路径此前**从未生效过**。已改为 `Platform.runLater`。
3. **静态字段初始化顺序** —— 新增的 `COMPACT_DATE_TIME_FORMAT` 起初声明在 static 初始化块**之后**，而该块启动时就会用到它 → `NullPointerException: formatter`。已移到常量区。
   （这个 bug 单测覆盖不到，是靠**真机跑一遍**才暴露的。）

### 验证汇总

```
mvn clean verify
  Tests run: 650, Failures: 0, Errors: 0, Skipped: 0     ← 修复前 647，新增 3 项回归门禁
  BugInstance size is 0
  All coverage checks have been met.
  BUILD SUCCESS
```

新增回归门禁（均已做**变异验证**——把缺陷改回去，门禁确实变红）：

- `ProductionSchemaConsistencyTest.userTimestampColumnsUseEpochMillisEverywhere`
  —— 两条生产建表通道 + H2 测试库三处都必须 `BIGINT`，且 `UserDAORefactored` 不得再出现 `new Timestamp(`
- `ProductionSchemaConsistencyTest.preferenceTablesHaveNoUsernameForeignKey`
  —— 三张偏好表在两条通道都不得有 `username` 外键，且必须保留删外键的老库迁移
- `UserDAOTest.testTimestampColumnsStoreEpochMillis`
  —— 行为级：落库值必须落在 epoch 毫秒区间，14 位紧凑日期时间即失败

### 三点操作提示

- **`.gitattributes` 的落地**：`* text=auto` 让 git 以 LF 存库、按平台检出。当前工作区里 `*.sh`、`docker/my.cnf` 仍是旧检出的 CRLF，但 git 比较前会先做规范化，所以 `git status` 依旧干净。要让工作区也统一，执行一次 `git add --renormalize .` 后提交即可。
- **Windows 上 `mvn clean` 会被正在运行的应用挡住**：实测 `Failed to delete target\lisuan-fx-*-jar-with-dependencies.jar`（JVM 持有 jar 句柄）。这不是缺陷，但发布/打包前必须先关掉应用——`release.bat` 没有这个前提说明，值得补一句。
- **给 `.bat` 桩测试造临时环境时，别用目录联接（junction）指向仓库**：本轮核查中曾用 `mklink /J` 把临时目录的 `src` 指向仓库源码（为了让 `release.bat` 的泄漏扫描能读到 `src/main/resources`），随后递归删除该临时目录时**沿重解析点删掉了仓库里 422 个跟踪文件**。教训：递归删除会跟随 junction；脚本桩测试应改为**复制**所需文件，或在删除前先确认目标下没有重解析点。万一踩到，用 `git checkout -- <路径>` 从 HEAD 恢复（当时 9 个文件带有未提交改动，需按记录逐处重新应用），再用 `mvn clean verify` 对比测试数确认无遗漏。

### 复验时新测到的一条观察（未改代码）

用真实 MySQL 跑修复后的写入路径时，前两次都在 Hikari 建池阶段失败：
`Failed to initialize pool: Communications link failure / Connection attempt exceeded defined timeout`。
逐层测量后定位到**不是项目缺陷，而是默认值余量太小**：

```
同一个 JVM 内连续三次原生 JDBC 连接（相同 URL/账号）：
  #1 = 8482 ms      ← 首次连接要付冷启动成本
  #2 =   44 ms
  #3 =   33 ms
```

`DatabaseManager` 的 `db.connection.timeout` 默认 **5000 ms**（注释写明是"减少超时时间避免 UI 冻结"），
遇到首次连接要 8 秒的机器就会直接判失败。把该项临时改成 30000 ms 后，同一探针立刻成功。
应用自身三次真机启动都在 ~700 ms 内建池完成，所以这是**环境相关、可复现但不必然触发**的问题。

建议：把默认值提到 10–15 s（仍然远小于用户可感知的"卡死"），或在失败时明确提示
"数据库连接超时，可在 config/database.properties 调大 db.connection.timeout"，
而不是笼统地报 `数据库初始化失败`。我**没有**擅自改这个默认值——它涉及取舍（UI 冻结 vs 冷启动容忍度），
应当由你确认。

### 界面字体门禁（本轮新增）

**背景**：Windows/macOS **默认都不安装** `Noto Sans CJK SC`——本机 `C:\Windows\Fonts` 里只有
`NotoSansSC-VF.ttf`（族名是 `Noto Sans SC`，而且是**可变字体**），没有 `Noto Sans CJK SC`。
所以随包内置的 `.ttc` 是唯一来源。而实测 **JavaFX 17.0.12 只使用 `-fx-font-family` 的第一个族名**
（不会遍历候选列表），一旦对不上就静默换成平台默认字体（本机实测 `Font.getDefault()` 就是
Microsoft YaHei UI）。CSS 里那一长串回退名因此**基本是装饰性的**。

**落地的两道门禁**：

1. `CashierSystemFXApplication.UI_FONT_FAMILY` 常量 + `loadCustomFonts()` 按它校验注册结果。
   失败时打 **ERROR**（原来是 `warn`），并在主窗口显示后弹一次**可见警告**
   （走 `Platform.runLater`，避开"动画/布局期间不允许 `showAndWait`"）。
2. `ThemeStylePolicyTest.uiFontStacksStartWithTheBundledFamily`：CSS 里每个 CJK 字体栈的**首项**
   必须等于该常量，覆盖 `styles.css` / `lisuan-theme.css` / `light-theme.css` / `dark-theme.css` / `splash.css`。

**验证**：

- 变异 1（主题 CSS 首项改成 `Microsoft YaHei UI`）→ 门禁变红并点名 `lisuan-theme.css`
- 变异 2（Java 常量改名）→ 5 个样式表全部点名
- 真机运行（正常包）：只出现 `成功加载 Noto Sans CJK SC Regular/Bold`，无报错
- 真机运行（用 7z 从 JAR 里删掉两个 `.ttc` 的包）：

```
ERROR 未找到界面字体资源 /fonts/NotoSansSC-Regular.ttc（Regular），安装包可能被裁剪
ERROR 未找到界面字体资源 /fonts/NotoSansSC-Bold.ttc（Bold），安装包可能被裁剪
ERROR 内置界面字体 Noto Sans CJK SC 未注册成功：…界面将静默回退到平台默认字体…
```

并弹出警告框——即把"静默降级"变成"可见降级"，但仍不阻断启动（Windows 上 YaHei UI 仍可读）。

> **一个验证教训（我自己的工具问题）**：用 `Copy-Item` 还原被变异的源文件会**保留原 mtime**，
> Maven 增量编译因此判定类是最新的，导致我第一次"正常包"验证跑到的其实还是被变异的类
> （日志里出现 `Some Other Font`）。必须 `mvn clean` 后的结论才可信。

### PDF 导出字体验证（本轮，**推翻**了此前的猜测）

此前怀疑：`ExportUtil.loadChineseFont()` 可能挑到系统里那份**可变字体**
`C:\Windows\Fonts\NotoSansSC-VF.ttf`（`wght` 默认 100 = Thin），印出细体报表。

**核实结论：猜测不成立** —— 该文件**根本不在候选列表里**。Windows 分支的候选是写死的四个：

```
%WINDIR%\Fonts\msyh.ttc   ← 本机存在（18.79 MB），实际被选中
%WINDIR%\Fonts\simhei.ttf
%WINDIR%\Fonts\simsun.ttc
%WINDIR%\Fonts\simkai.ttf
```

用应用真实的 `ExportUtil.export(...)` 生成 PDF，再用 PDFBox 回读「嵌了哪个字体、字重多少」
（把 `WINDIR` 指到不存在的目录即可在本机模拟 Linux/CI 无系统中文字体的分支）：

| 场景 | 选中字体 | PDF 内嵌字体 | `/FontWeight` | 源文件 `usWeightClass` |
|---|---|---|---|---|
| Windows 正常 | `C:\WINDOWS\Fonts\msyh.ttc` | `AAPNJF+MicrosoftYaHei`（PDType0Font / subset / embedded） | 400.0 | **400 Regular** |
| 伪造 `WINDIR`（≈ Linux/CI 无系统中文字体） | `src/main/resources/fonts/NotoSansSC-Regular.ttf` | `AUJIHC+NotoSansSC-Regular` | 400.0 | **400 Regular** |

顺带确认内置两份字体的字重都是 400：`NotoSansSC-Regular.ttf` = 400、
`NotoSansSC-Regular.ttc[NotoSansCJKsc-Regular]` = 400 —— 即 `fonts/README.md` 里
"变量字体默认 `wght=100`，必须用 `varLib.instancer wght=400` 固化"这一步**确实做对了**。

两条分支的页面都渲染成图看过：中文、数字、表格线正常，**没有细体、没有方框**。

> **一个取证细节**：从 PDF 里把子集化后的字体程序解析回来会报 `'post' table is mandatory`——
> PDFBox 子集化会丢掉 `post` 表，所以**字重要读源文件或用 PDF 的 `/FontWeight`**，
> 不能靠回读子集，否则会误判成"字体有问题"。

### 顺带观测到的连接问题（未改代码）

字体门禁验证期间，日志里出现两次 Hikari 建池失败：

```
11:54:38.681 HikariPool-1 - Starting...  → 11:54:47.959 失败（9.3 s）
11:56:06.562 HikariPool-1 - Starting...  → 11:56:06.874 成功（0.3 s）
11:56:32.085 HikariPool-1 - Starting...  → 14:07:07.209 失败（阻塞 2 h 11 min）
```

- 9.3 s 那次印证了前面"首连耗时 vs 5 s `connectionTimeout`"的观察：MySQL 全程在跑，下一次连接只用 0.3 s。
- 2 h 11 min 那次**我不认为是项目缺陷**：进程被冻结时 socket 超时无法触发，最可能是机器休眠/挂起
  （时间跨度与会话闲置数小时吻合）。
- 但它暴露一个真实体验问题：**建库期间界面只停在启动画面，既没有"正在连接数据库…"提示，也没有有界等待**，
  用户看到的就是"一直卡着"。这与 D5「失败必须可见」是同一类问题，建议一并处理。

---

## 附录 A：环境搭建命令（可复现）

```powershell
$T = "$env:TEMP\lisuan-audit"; New-Item -ItemType Directory -Force $T | Out-Null

# JDK 17（清华 TUNA 镜像）
curl.exe -L -o "$T\jdk.zip" "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip"
& "$env:USERPROFILE\scoop\shims\7z.exe" x "$T\jdk.zip" "-o$T" -y      # 不要用 tar.exe，会漏解出 instrument.dll

# Maven 3.9.9
curl.exe -L -o "$T\maven.zip" "https://maven.aliyun.com/repository/public/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
& "$env:USERPROFILE\scoop\shims\7z.exe" x "$T\maven.zip" "-o$T" -y

$env:JAVA_HOME = "$T\jdk-17.0.20.1+1"
$env:PATH = "$env:JAVA_HOME\bin;$T\apache-maven-3.9.9\bin;$env:PATH"

# MySQL 8.4.6（免安装 zip）
curl.exe -L -o "$T\mysql.zip" "https://dev.mysql.com/get/Downloads/MySQL-8.4/mysql-8.4.6-winx64.zip"
& "$env:USERPROFILE\scoop\shims\7z.exe" x "$T\mysql.zip" "-o$T\mysql" -y
$M = "$T\mysql\mysql-8.4.6-winx64"
& "$M\bin\mysqld.exe" --initialize-insecure --basedir="$M" --datadir="$T\mysqldata" --console
Start-Process "$M\bin\mysqld.exe" -ArgumentList "--basedir=$M","--datadir=$T\mysqldata","--port=3306","--bind-address=127.0.0.1"
& "$M\bin\mysql.exe" -u root -e "CREATE DATABASE lisuan_system CHARACTER SET utf8mb4; CREATE USER 'lisuan'@'%' IDENTIFIED BY '<pw>'; GRANT ALL ON lisuan_system.* TO 'lisuan'@'%';"
cmd /c "`"$M\bin\mysql.exe`" -u root --default-character-set=utf8mb4 lisuan_system < docker\mysql-init\00-init-complete.sql"
```

> ⚠️ 本机 `~/.m2` 不可写时：把 `<localRepository>` 指向临时目录并用 `mvn -s <settings.xml>` 指定（单独传 `-Dmaven.repo.local=` 无效，Maven 仍会尝试创建 `~/.m2`）。

## 附录 B：关键复现命令

```powershell
# 全量门禁
mvn -B -ntp -s $S verify

# 真实启动
java -jar target\lisuan-fx-2.6.0-jar-with-dependencies.jar

# D2：时间戳写坏（用应用同款连接参数 + useServerPrepStmts=true）
#   见 TsProbe3 —— create_time / last_login_time 落库为 20260924105656

# D3：语言偏好写失败
Select-String -Path logs\cashier-system.log -Pattern "保存语言偏好失败"

# D1：release.bat 静默跳过门禁
cmd /c "release.bat"   # 只输出到 [1/3]，echo %ERRORLEVEL% 仍为 0

# D4：空 module-path 是硬错误
java --module-path "" -version
```

---

# 第二轮（2026-10-08）：首启向导改造 + 跨平台实机复验

- **复验日期**：2026-10-08
- **代码版本**：`main` @ `75edc45`（阶段一：原代码实机验证）→ `56388c3`（阶段二：首启向导改造）
- **复验主机**：Windows（x64，DPI 100%）原生；另加 Linux 实机——WSL2 Debian（WSLg，`DISPLAY=:0`）
- **数据库**：MySQL 8.0.46，跑在 WSL2 docker 容器 `lisuan-mysql`（`127.0.0.1:3306`；`docker run mysql:8.0` 创建，**未挂载** `docker/mysql-init/`，schema 全部由应用 `DatabaseManager` 自建）

## 一、背景与结论

第一轮修复后遗留一个核心安全问题：**空库启动时应用自动生成随机 16 位临时密码并打印到控制台**——`javaw`/双击启动的用户根本看不到控制台，等于制造了一个谁也登不进去的账号；而 `docker/mysql-init/00-init-complete.sql` 又播种公开弱口令 `admin/admin123`（文档中 "Default login: admin / admin123" 仅对播种过的库成立，极易误导）。

本轮两阶段：

1. **阶段一**：在全新 Windows 环境对 `@75edc45` 做原生实机复验（构建门禁、真实启动、登录、REST API 冒烟）；
2. **阶段二**：以「首次运行向导」取代随机临时密码与 SQL 种子（提交 `56388c3`），并在 Windows 与 Linux（WSLg）双平台实机走通「空库 → 向导建号 → 登录 → 主界面」。

**结论**：两阶段全部通过。`mvn verify` 全绿（771 → **783** 个用例）；向导在双平台实机验证成功；仓库内（代码、SQL、脚本、文档）已不存在任何默认口令。验证后仓库干净，无任何未提交改动。

## 二、阶段一：原代码 Windows 实机验证（@75edc45）

**构建门禁**（`mvn clean verify`）：

| 检查 | 结果 |
|---|---|
| Surefire | **771 个用例，0 失败 / 0 错误** |
| SpotBugs（High） | 通过 |
| JaCoCo 行覆盖 ≥10% | 通过 |
| 产物 | `target/lisuan-fx-2.6.0-jar-with-dependencies.jar`（fat jar）可启动 |

**真实运行**（连真实 MySQL 8.0.46）：`java -jar` 启动 → 登录 →（随机临时密码首登）→ 强制改密对话框 → 主界面。锁定逻辑实测：连续 4 次错误未锁定（阈值 5），第 5 次正确密码登录成功且 `login_attempts.attempt_count` 立即清零。

**REST API 冒烟**（Javalin 6.1.3，`127.0.0.1:8080`；验证期间临时 `api.enabled=true`、CORS 限 `127.0.0.1:19387`，验毕已还原 `false`）：

| # | 请求 | 结果 |
|---|---|---|
| 1 | `GET /api/health` | 200 `{"service":"cashier-api","status":"ok",...}` |
| 2 | `GET /api/health/detail`（无 token） | 401 —— 公开端点只有 `/api/health`，文档如称 health 系列公开需修正 |
| 3 | `POST /api/auth/login`（正确凭据） | 200 + token；`password` 字段为 null（未泄漏哈希） |
| 4/5 | `GET /api/products` / `/api/members`（Bearer） | 200 分页壳 |
| 6 | 无 token | 401 `缺少认证 Token` |
| 7 | 坏 token | 401 `Token 无效或已过期` |
| 8 | 错误凭据登录 | 401 统一文案（不枚举用户；未写 `login_attempts`） |
| 9 | 不存在路由（无 token / 带 token） | 401（鉴权先行）/ 404 `{"message":"接口不存在: ..."}` |

**观察**（均不阻塞，部分在阶段二一并处理）：

- 随机临时密码机制的实际体验与预期一致地糟——这是阶段二改造的直接动因；
- Javalin 6.1.3 启动日志自带"已 949 天未更新"提醒 → 依赖升级候选；
- MySQL 未就绪（WSL 空闲关 VM 连带容器）时应用 fail-fast 退出，控制台可见"数据库初始化失败，系统将终止启动"，但双击启动的用户看不到任何提示；
- `DatabaseManager.initializeDatabase()` **硬编码 `CREATE DATABASE IF NOT EXISTS lisuan_system`**（`db.url` 里的 schema 实际被忽略）——"空库测试"必须清空 `lisuan_system` 本身，新建别的库会拆成两库写入、导致首启检测误判（调试踩坑，非本轮引入）。

**UI 自动化踩坑**（供后续复用）：

- `SendKeys` 高频输入会**静默丢字符**（14 位密码变 13 位，连错 4 次）→ 一律改用剪贴板 `Set-Clipboard` + `Ctrl+V` 原子粘贴；
- 登录错误是**内联 label 且约 3.3 秒自动淡出**——截图必须抢在 3 秒内，否则误判"点击无反应"；
- `SetForegroundWindow` 在窗口已在前台时返回 false → 判定应改用 `GetForegroundWindow() == hwnd`。

## 三、阶段二：首启向导改造（提交 @56388c3）

**改造内容**（20 个文件，+620/−137）：

- 新增 `service/FirstRunSetupService`：`needsFirstRunSetup()` = `users` 表 0 行；`createAdministrator()` 经 BCrypt 落库、`force_password_change=0`（密码本就是用户自设）、写 `operation_logs`（`FIRST_RUN_SETUP`）；
- 新增 `FirstRunSetupDialog`：程序化 `Dialog<User>`，密码用 **`PasswordField`** 收集；策略随系统设置（`passwordMinLength` 默认 6、`passwordComplexity` 默认 true → 须含字母+数字）；输入非法时「创建并进入系统」保持禁用；建号失败不关窗、内联报错可重试；取消向导 → 退出应用（此时一个账号都没有、无路可登）；
- 启动接线：数据库阶段（后台线程）检测空库 → 随不可变结果 `StartupDatabase(languageTag, needsFirstRunSetup)` 带回 FX 线程 → 启动画面关闭后 `Platform.runLater(this::showFirstRunSetup)` 弹出 → 创建成功视为完成登录直接进主界面；
- **删除**：`DatabaseManager` 的 `createDefaultAdminUser`/随机密码生成/打印；`00-init-complete.sql` 的 admin/admin123 种子；安装脚本与 5 份文档（`AGENTS.md`/`CLAUDE.md`/`README`×3）中的默认口令表述；
- i18n：四个 bundle 各补 15 个 `firstrun.*` 键（门禁会检查四份齐全）。

**门禁**（新增 12 个用例，`mvn verify` 771 → **783** 全绿）：

- `FirstRunSetupPolicyTest`（5 个）：旧建号路径已删净；全仓库（SQL / 安装脚本 / Java 源码，剥离注释后）不得出现 `admin123` 及其 BCrypt 哈希；启动接线在位（空库检测 → `Platform.runLater` 弹向导、取消即退出）；密码必须 `PasswordField` + BCrypt；15 个 `firstrun.*` 键 × 4 bundle 全存在；
- `FirstRunSetupServiceTest`（7 个）：空库检测、建号成功 / 重名 / 空值等。

## 四、Windows 实机验证：向导全流程

1. 先 dump 备份 `lisuan_system` → 清空库 → 启动应用：**向导如期弹出**（`shot-wizard-1.png`）；
2. 不合格输入时「创建并进入系统」保持禁用（`shot-wizard-weak.png`、`shot-wizard-weak2.png`）；
3. 填入合规密码创建成功 → 弹「初始化完成：管理员账号已创建，欢迎使用狸算！」（`shot-done-alert.png`）→ 直接进主界面（`shot-main-after-create.png`）；
4. 重启应用：**不再弹向导**，直接到登录页（`shot-relaunch-login.png`）；用向导创建的账号登录成功（`shot-login-filled2.png` → `shot-login-main.png`）；
5. 库内证据：`operation_logs` 有 `FIRST_RUN_SETUP` 与 `LOGIN SUCCESS`；`users` 行 `force_password_change=0`、`last_login_time` 已写；
6. 验证后从备份恢复 `lisuan_system`（33 张表）。（会话使用的测试口令均为一次性验证用，不记入本报告；`lisuan_fresh` 辅助库已删除。）


## 五、Linux 实机验证（WSL2 Debian + WSLg）

- 环境：WSL2 Debian 自带 WSLg（`DISPLAY=:0`）+ Temurin 17；apt Maven 3.9.9（pom 自带 Aliyun 仓库，无需 settings.xml）；补装 GTK3/GL/X11/字体/工具：`libgtk-3-0t64 libgl1 libglib2.0-0t64 libx11-6 libxext6 libxrender1 libxtst6 libxi6 libfreetype6 libasound2t64 fontconfig fonts-noto-cjk fonts-dejavu xdotool imagemagick x11-utils`；
- 运行：`export JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64; mvn -B -ntp javafx:run`（首次约 52 秒）；
- **空库 → 向导 → 建号 → 「初始化完成」→ 登录 → 主界面**全流程走通（`shot-wsl-wizard.png`、`shot-wsl-wizard-filled.png`、`shot-wsl-done-alert.png`、`shot-wsl-login-filled.png`、`shot-wsl-login-main.png` 等）；
- UI 自动化：xdotool 必须先 `windowactivate <id>` 再 click/type，否则首击被吞（与 Windows `SendKeys` 同类坑）；截图 `import -window <id>` 直接可用（GL 渲染不黑屏）；找窗口 `xdotool search --name 狸算`；
- **跨平台 locale 观察**（预存在，非向导引入）：应用未 `Locale.setDefault`，系统 locale 非中文时（WSL 常见），应用自身 i18n 是中文但 JavaFX 标准弹窗按钮显示 "OK"、状态栏星期显示 "Thursday"（Windows 上因系统 locale 是中文而显示"确定/星期四"）。若要修：启动时按 `I18nManager` 设置 `Locale.setDefault`——**未擅自改动，留待确认**。

## 六、提交、清理与遗留

- 提交：`56388c3` `feat(security): 首次运行向导取代默认凭据（删除随机临时密码与 SQL 种子）`（20 文件，+620/−137）；提交后仓库干净；

- 遗留（均不阻塞发布，待确认后再动）：
  1. **locale 一行修复**（见第五节）——影响所有平台的弹窗文案观感，需确认；
  2. `/api/health/detail` 需 token——若文档称 health 系列公开则需改文档；
  3. Javalin 6.1.3 过旧（启动自提醒 949 天未更新）；
  4. MySQL 不可达时双击启动无可见提示（fail-fast）——可接受但值得知晓；
  5. macOS 实机无法验证（无硬件），仅代码与 CI 层面兼容。
