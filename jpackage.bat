@echo off
REM ========================================
REM LiSuan - jpackage Packaging Script
REM Version 2.5.5
REM Creates Windows EXE with embedded JRE
REM ========================================

setlocal enabledelayedexpansion

echo ========================================
echo   LiSuan - jpackage Packaging
echo   (With Embedded JRE)
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

REM Check jlink command
where jlink >nul 2>&1
if errorlevel 1 (
    echo [ERROR] jlink command not found
    echo.
    echo jlink is included in JDK 14+.
    echo Please ensure JDK 14+ is installed and in PATH.
    pause
    exit /b 1
)

echo [1/4] Creating custom JRE with jlink...
echo.

set "JRE_OUTPUT=target\custom-jre"
if exist "%JRE_OUTPUT%" rmdir /S /Q "%JRE_OUTPUT%"

REM Create custom JRE with required modules
jlink ^
  --add-modules java.base,java.sql,java.logging,java.naming,java.desktop,java.xml,java.net.http ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
  --output "%JRE_OUTPUT%" ^
  --strip-native-commands ^
  --compress=2 ^
  --no-man-pages ^
  --no-header-files

if errorlevel 1 (
    echo [ERROR] jlink failed!
    pause
    exit /b 1
)

echo [OK] Custom JRE created
echo.

REM Check JRE size
for /f "tokens=3" %%a in ('dir /s "%JRE_OUTPUT%" ^| findstr "bytes"') do set "JRE_SIZE=%%a"
echo [INFO] JRE size: %JRE_SIZE% bytes
echo.

echo [2/4] Cleaning old package files...
if exist "target\dist" rmdir /S /Q "target\dist" 2>nul
mkdir "target\dist"
echo [OK] Clean complete
echo.

echo [3/4] Starting jpackage...
echo.

jpackage ^
    --type exe ^
    --name "LiSuan" ^
    --app-version "%APP_VERSION%" ^
    --vendor "LiSuan" ^
    --description "LiSuan - Modern POS System" ^
    --dest "target\dist" ^
    --input "target" ^
    --runtime-image "%JRE_OUTPUT%" ^
    --main-jar "%FAT_JAR%" ^
    --main-class "com.cashier.CashierSystemFXApplication" ^
    --java-options "-Xms512m" ^
    --java-options "-Xmx1024m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --win-menu ^
    --win-menu-group "LiSuan" ^
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
echo [4/4] Package complete!
echo.

echo ========================================
echo   [SUCCESS] Native installer created
echo   With embedded JRE - No Java required!
echo ========================================
echo.
echo Installer location: target\dist\LiSuan-%APP_VERSION%.exe
echo.

REM Show file size
for %%F in ("target\dist\LiSuan-%APP_VERSION%.exe") do (
    set "SIZE=%%~zF"
    set /a "SIZE_MB=!SIZE!/1048576"
    echo Installer size: !SIZE_MB! MB
)
echo.
echo After installation:
echo   - Desktop shortcut created
echo   - Start menu added
echo   - NO Java installation required
echo   - No console window
echo.

REM Open output directory
explorer "target\dist"

pause
exit /b 0
