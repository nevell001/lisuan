@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion
REM 狸算(LiSuan) 生产发布候选验证脚本 (Windows)
REM 与 release.sh 保持同一套发布门禁

cd /d "%~dp0"

echo ====================================================
echo       LiSuan Production Release Verification
echo ====================================================

REM 0. 版本一致性门禁
echo [0/3] Checking version consistency...
set "VERSION_POM="
for /f "tokens=3 delims=<>" %%V in ('findstr /R "<version>[0-9]" pom.xml 2^>nul ^| findstr /V "javafx maven java mysql hikaricp"') do (
    set "VERSION_POM=%%V"
    goto :version_pom_found
)
:version_pom_found

set "VERSION_JAVA="
for /f "tokens=2 delims==" %%J in ('findstr /C:"APP_VERSION =" src\main\java\com\cashier\constant\AppConstants.java') do (
    set "VERSION_JAVA=%%J"
    goto :version_java_found
)
:version_java_found
set "VERSION_JAVA=!VERSION_JAVA:"=!"
set "VERSION_JAVA=!VERSION_JAVA:;=!"
set "VERSION_JAVA=!VERSION_JAVA: =!"

if not "%VERSION_POM%"=="%VERSION_JAVA%" (
    echo ERROR: Version numbers do not match!
    echo   pom.xml: %VERSION_POM%
    echo   AppConstants.java: %VERSION_JAVA%
    exit /b 1
)
echo Version numbers match (%VERSION_POM%)

REM 从 .env 定向读取数据库密码（仅限密码变量，与 start.sh/release.sh 保持一致）
if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        if /i "%%a"=="CASHIER_DB_PASSWORD" if not defined CASHIER_DB_PASSWORD set "CASHIER_DB_PASSWORD=%%b"
        if /i "%%a"=="CASHER_DB_PASSWORD" if not defined CASHER_DB_PASSWORD set "CASHER_DB_PASSWORD=%%b"
        if /i "%%a"=="MYSQL_PASSWORD" if not defined CASHIER_DB_PASSWORD if not defined CASHER_DB_PASSWORD set "CASHIER_DB_PASSWORD=%%b"
    )
)
if defined CASHIER_DB_PASSWORD set "CASHIER_DB_PASSWORD=!CASHIER_DB_PASSWORD:"=!"
if defined CASHER_DB_PASSWORD set "CASHER_DB_PASSWORD=!CASHER_DB_PASSWORD:"=!"

REM 1. 完整验证门禁：单元测试 + SpotBugs + JaCoCo 覆盖率 + 打包
echo [1/3] Running full verification gate (mvn clean verify)...
call mvn clean verify -DskipTests=false
if %ERRORLEVEL% NEQ 0 (
    echo FAILED: full verification ^(tests/SpotBugs/coverage/package^)
    exit /b %ERRORLEVEL%
)
echo Full verification passed ^(tests/SpotBugs/coverage/package^)

REM 2. 环境与配置验证
echo [2/3] Checking release configuration...

if not exist "target\lisuan-fx-%VERSION_POM%-jar-with-dependencies.jar" (
    echo ERROR: executable JAR not found
    exit /b 1
)
echo Executable JAR generated

if not exist "config\database.properties" (
    echo ERROR: database config file not found
    exit /b 1
)

findstr /R /C:"useSSL=false" /C:"sslMode=DISABLED" "config\database.properties" >nul
if %ERRORLEVEL% EQU 0 (
    echo ERROR: database SSL is disabled; use sslMode=PREFERRED/REQUIRED/VERIFY_CA/VERIFY_IDENTITY
    exit /b 1
)

findstr /R /C:"db.password=..*" "config\database.properties" >nul
if %ERRORLEVEL% EQU 0 (
    echo ERROR: config\database.properties must not store db.password; use CASHIER_DB_PASSWORD instead
    exit /b 1
)

if "%CASHIER_DB_PASSWORD%"=="" if "%CASHER_DB_PASSWORD%"=="" (
    echo ERROR: CASHIER_DB_PASSWORD not set; production release requires the DB password via environment variable
    exit /b 1
)
echo Database password and SSL configuration passed

REM API 生产配置门禁：API 未开启时允许发布；开启后必须使用强密钥和限定 CORS
set "API_ENABLED="
if exist "config\api.properties" (
    findstr /B /C:"api.enabled=true" "config\api.properties" >nul
    if not errorlevel 1 set "API_ENABLED=1"
)
if defined API_ENABLED (
    set "TOKEN_SECRET_VALUE=!TOKEN_SECRET!"
    set "CORS_VALUE=!CORS_ALLOWED_ORIGINS!"

    if not defined TOKEN_SECRET_VALUE (
        if exist "config\api.properties" (
            for /f "usebackq tokens=1,* delims==" %%a in ("config\api.properties") do (
                if /i "%%a"=="token.secret" set "TOKEN_SECRET_VALUE=%%b"
            )
        )
    )
    if not defined CORS_VALUE (
        if exist "config\api.properties" (
            for /f "usebackq tokens=1,* delims==" %%a in ("config\api.properties") do (
                if /i "%%a"=="cors.allowed.origins" set "CORS_VALUE=%%b"
            )
        )
    )

    if "!TOKEN_SECRET_VALUE:~31!"=="" (
        echo ERROR: API enabled but TOKEN_SECRET length is less than 32
        exit /b 1
    )
    if /i "!TOKEN_SECRET_VALUE!"=="default_secret_key" (
        echo ERROR: API enabled but TOKEN_SECRET uses the default value
        exit /b 1
    )
    echo !TOKEN_SECRET_VALUE! | findstr /I /C:"REPLACE_" /C:"change_this" /C:"your_secret" >nul
    if not errorlevel 1 (
        echo ERROR: API enabled but TOKEN_SECRET contains placeholder
        exit /b 1
    )
    if "!CORS_VALUE!"=="" (
        echo ERROR: API enabled but CORS_ALLOWED_ORIGINS/cors.allowed.origins is not restricted to concrete origins
        exit /b 1
    )
    echo !CORS_VALUE! | findstr "\*" >nul
    if not errorlevel 1 (
        echo ERROR: API enabled but CORS_ALLOWED_ORIGINS must not contain wildcard
        exit /b 1
    )
    echo API security configuration passed
) else (
    echo API disabled, skipping API secret and CORS release gate
)

REM 支付配置门禁：生产发布禁止 mock 模式；production 模式必须无占位凭据；disabled 给出提示
set "PAYMENT_MODE="
if exist "config\payment.properties" (
    for /f "usebackq tokens=1,* delims==" %%a in ("config\payment.properties") do (
        if /i "%%a"=="payment.mode" set "PAYMENT_MODE=%%b"
    )
)
if defined PAYMENT_MODE (
    set "PAYMENT_MODE=!PAYMENT_MODE: =!"
    if /i "!PAYMENT_MODE!"=="mock" (
        echo ERROR: production release must not use mock payment mode; configure a real payment channel or keep disabled
        exit /b 1
    )
    if /i "!PAYMENT_MODE!"=="production" (
        findstr /I /C:"YOUR_" /C:"REPLACE_WITH" /C:"CHANGE_ME" /C:"change_me" /C:"your-" "config\payment.properties" >nul
        if not errorlevel 1 (
            echo ERROR: production payment mode still contains placeholder credentials ^(YOUR_/REPLACE_WITH/CHANGE_ME^); fill in real merchant configuration
            exit /b 1
        )
        echo Payment uses production mode with no placeholder credentials ^(real channel callbacks need separate verification^)
    )
    if /i "!PAYMENT_MODE!"=="disabled" (
        echo Electronic payment is currently disabled; configure a real channel if WeChat/Alipay collection is needed in production
    )
)

REM 检查敏感信息（二次确认）
set "LEAK_FOUND=0"
for /r "config" %%f in (*) do call :check_leak "%%f"
for /r "src\main\resources" %%f in (*) do call :check_leak "%%f"
if "%LEAK_FOUND%"=="1" (
    echo WARNING: possible leaked local password found in code or config; clean up before release
    exit /b 1
)
echo No obvious local password leak found

REM 检查 .env 是否被误包含在 target 中
if exist "target\classes\.env" (
    echo WARNING: .env file is incorrectly included in packaged resources
    exit /b 1
)
if exist "target\classes\.env.example" (
    echo WARNING: .env.example file is incorrectly included in packaged resources
    exit /b 1
)

echo ====================================================
echo       LiSuan Release Verification PASSED
echo       Release version: %VERSION_POM%
echo ====================================================
exit /b 0

:check_leak
set "LEAK_FILE=%~1"
echo !LEAK_FILE! | findstr /I /C:".example" /C:"database.properties.template" >nul
if not errorlevel 1 exit /b 0
findstr /I /C:"RootPassword123!" /R /C:"db.password=..*" "!LEAK_FILE!" >nul 2>&1
if not errorlevel 1 (
    echo WARNING: possible leaked local password found in !LEAK_FILE!
    set "LEAK_FOUND=1"
)
exit /b 0
