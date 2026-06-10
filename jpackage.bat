@echo off
REM ========================================
REM Cashier System - jpackage Packaging Script
REM Version 2.5.5
REM ========================================

setlocal enabledelayedexpansion

echo ========================================
echo   Cashier System - jpackage Packaging
echo ========================================
echo.

REM Read version from pom.xml
for /f "tokens=3 delims=<>" %%a in ('findstr /R "<version>" pom.xml ^| findstr /V "javafx\|maven\|java\|mysql\|hikaricp\|poi\|pdfbox\|controlsfx\|fontawesomefx\|junit\|testfx\|h2\|bcrypt\|logback\|jackson\|javalin\|slf4j\|plugin"') do (
    set "APP_VERSION=%%a"
    goto :version_found
)
:version_found
if "%APP_VERSION%"=="" set "APP_VERSION=2.5.5"

echo [INFO] Version: %APP_VERSION%
echo.

REM Check JAR file
set "FAT_JAR=cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar"
if not exist "target\%FAT_JAR%" (
    echo [ERROR] JAR not found: target\%FAT_JAR%
    echo.
    echo Please run first: mvn clean package
    pause
    exit /b 1
)

REM Check jpackage command
where jpackage >nul 2>&1
if errorlevel 1 (
    echo [ERROR] jpackage command not found
    echo.
    echo jpackage is included in JDK 14+.
    echo Please ensure:
    echo   1. JDK 14+ is installed
    echo   2. JDK bin directory is in PATH
    echo.
    echo Checking JDK:
    where java
    java -version
    echo.
    pause
    exit /b 1
)

REM Clean old package files
echo [1/3] Cleaning old package files...
if exist "target\dist" rmdir /S /Q "target\dist" 2>nul
mkdir "target\dist"
echo [OK] Clean complete
echo.

REM Execute jpackage
echo [2/3] Starting package...
echo.

jpackage ^
    --type exe ^
    --name "CashierSystem" ^
    --app-version "%APP_VERSION%" ^
    --vendor "Cashier System" ^
    --description "Modern POS System - Inventory, Member, Transaction Management" ^
    --dest "target\dist" ^
    --input "target" ^
    --main-jar "%FAT_JAR%" ^
    --main-class "com.cashier.CashierSystemFXApplication" ^
    --java-options "-Xms512m" ^
    --java-options "-Xmx1024m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --win-menu ^
    --win-menu-group "Cashier System" ^
    --win-shortcut ^
    --win-dir-chooser ^
    --win-per-user-install false ^
    --icon "src\main\resources\images\logos\app-icon.ico"

if errorlevel 1 (
    echo.
    echo ========================================
    echo   [ERROR] Package failed!
    echo ========================================
    echo.
    pause
    exit /b 1
)

echo.
echo [3/3] Package complete!
echo.

echo ========================================
echo   [SUCCESS] Native installer created
echo ========================================
echo.
echo Installer location: target\dist\CashierSystem-%APP_VERSION%.exe
echo.
echo After installation:
echo   - Desktop shortcut created
echo   - Start menu added
echo   - No command line needed
echo   - No console window
echo.

REM Open output directory
explorer "target\dist"

pause
exit /b 0
