@echo off
chcp 65001 >nul 2>&1
REM ============================================
REM Cashier System Installation Script (Windows)
REM ============================================

setlocal enabledelayedexpansion

echo ========================================
echo   Cashier System Installation
echo ========================================
echo.

set APP_VERSION=v2.2.1

REM Check if already compiled
if exist "target\cashier-system-fx-%APP_VERSION%.jar" (
    echo [Warning] Detected existing compiled files
    set /p REPLY="Recompile? (y/N): "
    if /i "!REPLY!"=="y" (
        echo [Clean] Cleaning old files...
        call mvn clean >nul 2>&1
    ) else (
        echo [Skip] Skipping compilation
        goto :check_dependencies
    )
)

echo [1/7] Checking Java environment...
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [Error] Java runtime not found!
    echo Please install JDK 17 or higher
    echo Download: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%i
set JAVA_VERSION=%JAVA_VERSION:"=%
echo [Info] Java version: %JAVA_VERSION%

REM Check if javac exists (optional - Maven can use bundled compiler)
where javac >nul 2>&1
if %errorlevel% neq 0 (
    echo [Warning] JDK compiler (javac) not found in PATH
    echo [Note] Maven will use its bundled compiler
    set HAS_JAVAC=false
) else (
    echo [Info] JDK compiler found
    set HAS_JAVAC=true
)

echo [Done] Java environment check passed
echo.

echo [2/7] Checking Maven environment...
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [Error] Maven not found!
    echo Please install Maven 3.8 or higher
    echo Download: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

for /f "tokens=3" %%i in ('mvn -version 2^>^&1 ^| findstr "Apache Maven"') do set MAVEN_VERSION=%%i
echo [Info] Maven version: %MAVEN_VERSION%
echo [Done] Maven environment check passed
echo.

echo [3/7] Creating necessary directories...
if not exist "config" mkdir "config"
if not exist "data" mkdir "data"
if not exist "logs" mkdir "logs"
if not exist "docker\mysql-init" mkdir "docker\mysql-init"
if not exist "docker\mysql-backup" mkdir "docker\mysql-backup"
echo [Done] Directories created
echo.

:check_dependencies
echo [4/7] Checking configuration files...
if not exist "config\database.properties" (
    echo [Create] Creating database config file
    copy "config\database.properties.example" "config\database.properties" >nul
    echo [Tip] Please edit config\database.properties for database connection
)

if not exist "config\jvm.config" (
    echo [Create] Creating JVM config file
    copy "config\jvm.config.example" "config\jvm.config" >nul
)

echo [Done] Configuration files checked
echo.

echo [5/7] Downloading Maven dependencies...
echo [Tip] First installation may take a while, please wait...
call mvn dependency:resolve
if %errorlevel% neq 0 (
    echo [Error] Dependency download failed
    echo Please check network connection or Maven configuration
    pause
    exit /b 1
)
echo [Done] Dependencies downloaded
echo.

echo [6/7] Compiling project...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [Error] Compilation failed
    echo Please check compilation error messages
    echo.
    echo [Debug Info]
    echo   Java version: %JAVA_VERSION%
    echo   Javac in PATH: %HAS_JAVAC%
    echo.
    pause
    exit /b 1
)
echo [Done] Project compiled
echo.

echo [7/7] Creating desktop shortcut...
echo [Tip] Create desktop shortcut?
set /p CREATE_SHORTCUT="Create shortcut? (Y/n): "
if not /i "%CREATE_SHORTCUT%"=="n" (
    call create-shortcut.bat
    if %errorlevel% equ 0 (
        echo [Done] Desktop shortcut created
    ) else (
        echo [Warning] Desktop shortcut creation failed
    )
)
echo.

echo ========================================
echo [Success] Installation completed!
echo ========================================
echo.
echo Application Info:
echo   Version: %APP_VERSION%
echo   Main Class: com.cashier.CashierFXApplication
echo   JAR: target\cashier-system-fx-%APP_VERSION%.jar
echo.
echo Next Steps:
echo   1. Configure database connection (config\database.properties)
echo   2. Run start.bat to start the application
echo.
echo Or simply run:
echo   start.bat
echo.
pause
exit /b 0