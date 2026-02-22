@echo off
chcp 65001 >nul 2>&1
REM ============================================
REM Cashier System Startup Script (Windows)
REM ============================================
REM 
REM 安全提示：
REM 为了安全起见，建议设置环境变量 CASHER_DB_PASSWORD 来存储数据库密码
REM 
REM Windows CMD 设置方式:
REM   set CASHER_DB_PASSWORD=YourPassword
REM   start.bat
REM 
REM Windows PowerShell 设置方式:
REM   $env:CASHER_DB_PASSWORD="YourPassword"
REM   .\start.bat
REM 
REM 如果设置了环境变量，config\database.properties 中的 db.password 将被忽略
REM ============================================

setlocal enabledelayedexpansion

echo ========================================
echo   Cashier System Startup
echo ========================================
echo.

REM Set application info
set APP_NAME=Cashier System
set APP_VERSION=2.3.0
set MAIN_CLASS=com.cashier.CashierSystemFXApplication
set CONFIG_FILE=config\jvm.config

REM Set application directory
set APP_DIR=%~dp0
cd /d "%APP_DIR%"

echo [1/6] Checking Java environment...
where java >nul 2>&1
if !errorlevel! neq 0 (
    echo [Error] Java runtime not found!
    echo Please ensure JDK 17 or higher is installed
    echo Download: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%i
set JAVA_VERSION=%JAVA_VERSION:"=%
echo [Info] Java version: %JAVA_VERSION%

REM Check if javac exists (to determine if it's a full JDK)
where javac >nul 2>&1
if !errorlevel! neq 0 (
    echo [Warning] Only JRE detected, full JDK recommended
)

echo [Done] Java environment check passed
echo.

echo [2/6] Checking necessary directories...
if not exist "config" mkdir "config"
if not exist "data" mkdir "data"
if not exist "logs" mkdir "logs"
echo [Done] Directory check passed
echo.

echo [3/6] Checking configuration files...
if not exist "config\database.properties" (
    echo [Warning] Database config file not found, using default settings
    echo [Tip] Please run install.bat for full installation
)

if not exist "config\jvm.config" (
    echo [Create] Creating JVM config file
    copy "config\jvm.config.example" "config\jvm.config" >nul 2>&1
)

echo [Done] Configuration files checked
echo.

echo [4/6] Checking dependency files...
if not exist "target\cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar" (
    echo [Warning] Compiled JAR file not found
    echo [Tip] Please run install.bat for compilation
    echo.
    set /p REPLY="Compile now? (Y/n): "
    if /i "!REPLY!"=="n" (
        echo [Cancel] Startup cancelled
        pause
        exit /b 1
    )
    echo [Compile] Starting project compilation...
    call mvn clean package -DskipTests
    if !errorlevel! neq 0 (
        echo [Error] Compilation failed
        pause
        exit /b 1
    )
)

echo [Done] Dependency files checked
echo.

echo [5/6] Building JVM parameters...
set JVM_OPTS=
if exist "%CONFIG_FILE%" (
    for /f "usebackq tokens=*" %%a in ("%CONFIG_FILE%") do (
        set "LINE=%%a"
        REM Ignore empty lines and comments (lines starting with #)
        if not "!LINE!"=="" (
            if not "!LINE:~0,1!"=="#" (
                set JVM_OPTS=!JVM_OPTS! !LINE!
            )
        )
    )
)

REM Default JVM parameters
if "%JVM_OPTS%"=="" (
    set JVM_OPTS=-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Dsun.java2d.dpiaware=true
)

echo [Done] JVM parameters built
echo.

echo [6/6] Starting application...
echo.
echo ========================================
echo   %APP_NAME% %APP_VERSION%
echo ========================================
echo.
echo Starting, please wait...
echo.

REM Set JAR file path (fat jar with all dependencies included)
set JAR_FILE=target\cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar

REM Start application using Maven JavaFX plugin (best for JavaFX apps)
mvn javafx:run

REM Check exit code
if !errorlevel! neq 0 (
    echo.
    echo ========================================
    echo [Error] Application exited abnormally (Error code: !errorlevel!)
    echo ========================================
    echo.
    echo Check log file: logs\app.log
    pause
    exit /b 1
)

echo.
echo ========================================
echo [Info] Application exited normally
echo ========================================
echo.
pause
exit /b 0