@echo off
chcp 65001 >nul 2>&1
REM ============================================
REM Cashier System Windows Packaging Script
REM Use jpackage to create Windows installer
REM ============================================

setlocal enabledelayedexpansion

echo ========================================
echo   Cashier System Windows Package Tool
echo ========================================
echo.

set APP_VERSION=v2.5.0
set APP_NAME=Cashier System
set VENDOR=Nevell
set MAIN_CLASS=com.cashier.CashierSystemFXApplication
set JAR_FILE=target\cashier-system-fx-%APP_VERSION%.jar

REM Check Java environment
echo [1/6] Checking Java environment...
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [Error] Java runtime not found!
    pause
    exit /b 1
)

REM Check jpackage tool (requires JDK 14+)
where jpackage >nul 2>&1
if %errorlevel% neq 0 (
    echo [Error] jpackage tool not found!
    echo jpackage requires JDK 14 or higher
    echo Please ensure full JDK is installed (not JRE)
    pause
    exit /b 1
)

echo [Done] Java environment check passed
echo.

REM Check compiled JAR file
echo [2/6] Checking JAR file...
if not exist "%JAR_FILE%" (
    echo [Warning] JAR file not found, starting compilation...
    call mvn clean package -DskipTests
    if %errorlevel% neq 0 (
        echo [Error] Compilation failed
        pause
        exit /b 1
    )
)
echo [Done] JAR file check passed
echo.

REM Create temporary directory
echo [3/6] Preparing package files...
if not exist "temp\package" mkdir "temp\package"
if not exist "temp\package\lib" mkdir "temp\package\lib"
if not exist "temp\package\config" mkdir "temp\package\config"

REM Copy JAR file
copy "%JAR_FILE%" "temp\package\" >nul

REM Copy dependency libraries
if exist "target\lib" (
    xcopy /E /I /Y "target\lib" "temp\package\lib" >nul
)

REM Copy configuration file examples
if exist "config\database.properties.example" (
    copy "config\database.properties.example" "temp\package\config\" >nul
)
if exist "config\jvm.config.example" (
    copy "config\jvm.config.example" "temp\package\config\" >nul
)

REM Copy Docker related files
if exist "docker\mysql-init" (
    xcopy /E /I /Y "docker\mysql-init" "temp\package\docker\mysql-init" >nul
)
if exist "docker-compose.yml" (
    copy "docker-compose.yml" "temp\package\" >nul
)

echo [Done] Package files prepared
echo.

REM Check application icon
echo [4/6] Checking application icon...
set ICON_PATH=
if exist "src\main\resources\images\app-icon.ico" (
    set ICON_PATH=--icon src\main\resources\images\app-icon.ico
    echo [Info] Using custom icon
) else (
    echo [Warning] Application icon not found (src\main\resources\images\app-icon.ico)
    echo [Tip] Default icon will be used
)
echo.

REM Create installer
echo [5/6] Creating Windows installer...
echo [Info] Packaging in progress, please wait...
echo.

jpackage^
    --type msi^
    --name "%APP_NAME%"^
    --app-version %APP_VERSION:~1%^
    --vendor "%VENDOR%"^
    --description "Complete cashier system with inventory management, member management, and transaction features"^
    --main-jar cashier-system-fx-%APP_VERSION%.jar^
    --main-class %MAIN_CLASS%^
    --input temp\package^
    --dest dist^
    --win-menu^
    --win-menu-group "Cashier System"^
    --win-shortcut^
    --win-dir-chooser^
    --win-console^
    %ICON_PATH%^
    --java-options "-Xms512m"^
    --java-options "-Xmx1024m"^
    --java-options "-Dfile.encoding=UTF-8"^
    --java-options "-Dsun.java2d.dpiaware=true"

if %errorlevel% neq 0 (
    echo [Error] Packaging failed
    pause
    exit /b 1
)

echo [Done] Windows installer created successfully
echo.

REM Create portable ZIP
echo [6/6] Creating portable ZIP...
set PORTABLE_DIR=temp\portable\%APP_NAME%-%APP_VERSION%
if exist "%PORTABLE_DIR%" rmdir /S /Q "%PORTABLE_DIR%"
mkdir "%PORTABLE_DIR%"
mkdir "%PORTABLE_DIR%\config"
mkdir "%PORTABLE_DIR%\data"
mkdir "%PORTABLE_DIR%\logs"
mkdir "%PORTABLE_DIR%\docker"

REM Copy files
copy "%JAR_FILE%" "%PORTABLE_DIR%\" >nul
copy "start.bat" "%PORTABLE_DIR%\" >nul
copy "README.md" "%PORTABLE_DIR%\" >nul

REM Copy dependencies
if exist "target\lib" (
    xcopy /E /I /Y "target\lib" "%PORTABLE_DIR%\lib" >nul
)

REM Copy configuration file examples
copy "config\database.properties.example" "%PORTABLE_DIR%\config\" >nul
copy "config\jvm.config.example" "%PORTABLE_DIR%\config\" >nul

REM Copy Docker files
xcopy /E /I /Y "docker\mysql-init" "%PORTABLE_DIR%\docker\mysql-init" >nul
copy "docker-compose.yml" "%PORTABLE_DIR%\" >nul

REM Package ZIP
if exist "dist\%APP_NAME%-%APP_VERSION%-portable.zip" del "dist\%APP_NAME%-%APP_VERSION%-portable.zip"
powershell -Command "Compress-Archive -Path '%PORTABLE_DIR%' -DestinationPath 'dist\%APP_NAME%-%APP_VERSION%-portable.zip'"

echo [Done] Portable ZIP created
echo.

echo ========================================
echo [Success] Packaging completed!
echo ========================================
echo.
echo Output files:
echo   Installer: dist\%APP_NAME%-*.msi
echo   Portable: dist\%APP_NAME%-%APP_VERSION%-portable.zip
echo.
echo Installer instructions:
echo   1. Double-click .msi installer
echo   2. Follow the wizard to complete installation
echo   3. Launch from Start Menu
echo.
echo Portable version instructions:
echo   1. Extract ZIP to any directory
echo   2. Double-click start.bat
echo   3. Configuration files will be created on first run
echo.
pause
exit /b 0