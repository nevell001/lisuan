@echo off
setlocal enabledelayedexpansion

REM ============================================
REM   LiSuan Startup Script (Windows)
REM   Version 2.6.0
REM ============================================

cd /d "%~dp0"

set "APP_NAME=LiSuan"
set "APP_VERSION=2.6.0"

REM 从 .env 定向读取数据库密码（与 start.sh 保持一致，仅读取密码变量，兼容旧版 MYSQL_PASSWORD）
if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        if /i "%%a"=="CASHIER_DB_PASSWORD" if not defined CASHIER_DB_PASSWORD set "CASHIER_DB_PASSWORD=%%b"
        if /i "%%a"=="CASHER_DB_PASSWORD" if not defined CASHIER_DB_PASSWORD if not defined CASHER_DB_PASSWORD set "CASHER_DB_PASSWORD=%%b"
        if /i "%%a"=="MYSQL_PASSWORD" if not defined CASHIER_DB_PASSWORD if not defined CASHER_DB_PASSWORD set "CASHIER_DB_PASSWORD=%%b"
    )
    if defined CASHIER_DB_PASSWORD set "CASHIER_DB_PASSWORD=!CASHIER_DB_PASSWORD:"=!"
    if defined CASHER_DB_PASSWORD set "CASHER_DB_PASSWORD=!CASHER_DB_PASSWORD:"=!"
    if defined CASHIER_DB_PASSWORD echo [INFO] CASHIER_DB_PASSWORD is set and will override db.password in config\database.properties
)

cls
echo.
echo =========================================
echo   %APP_NAME% Startup
echo =========================================
echo.

echo [1/5] Checking Java environment...
echo ----------------------------------------

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found!
    echo.
    echo Please install JDK 17 or higher:
    echo   - Oracle: https://www.oracle.com/java/technologies/downloads/
    echo   - Winget: winget install Oracle.JDK.17
    pause
    exit /b 1
)

echo [OK] Java environment check passed
echo.

echo [2/5] Checking necessary directories...
echo ----------------------------------------

if not exist "config" mkdir config
if not exist "data" mkdir data
if not exist "logs" mkdir logs
if not exist "temp" mkdir temp

echo [OK] Directory check passed
echo.

echo [3/5] Checking configuration files...
echo ----------------------------------------

if not exist "config\database.properties" (
    echo [WARNING] Database config not found
    echo [INFO] Please run install.bat for full installation
    echo.
)

if not exist "config\jvm.config" (
    if exist "config\jvm.config.example" (
        copy /Y "config\jvm.config.example" "config\jvm.config" >nul
        echo [CREATE] config\jvm.config
    )
)

echo [OK] Configuration files checked
echo.

echo [4/5] Checking application files...
echo ----------------------------------------

REM 查找已构建的 fat JAR。
REM 注意：不能写 for %%f in (target\lisuan-fx-*.jar)——cmd 在没有匹配时会把通配符**原样当成一项**，
REM 于是 "%JAR_FILE%"=="" 永远为假、错误分支永远走不到，最后 java -jar 才报 Unable to access jarfile。
REM for /f 配合 dir /b：无匹配时 dir 返回非零且不输出任何行，变量保持未定义。
set "JAR_FILE="
for /f "delims=" %%f in ('dir /b /o-d "target\lisuan-fx-*-jar-with-dependencies.jar" 2^>nul') do (
    set "JAR_FILE=target\%%f"
    goto :jar_found
)
:jar_found

if not defined JAR_FILE (
    echo [ERROR] Application JAR not found in target\
    echo.
    echo Please build it first:
    echo     mvn clean package -DskipTests
    echo.
    pause
    exit /b 1
)

if not exist "%JAR_FILE%" (
    echo [ERROR] JAR file not accessible: %JAR_FILE%
    pause
    exit /b 1
)

echo [OK] JAR file found: %JAR_FILE%

echo.
echo [Done] Application files ready
echo.

echo [5/5] Building JVM parameters...
echo ----------------------------------------

set "JVM_OPTS="
set "JVM_CONFIG_FILE=config\jvm.config"

if exist "%JVM_CONFIG_FILE%" (
    echo [INFO] Reading JVM config from %JVM_CONFIG_FILE%
    for /f "usebackq eol=# tokens=*" %%a in ("%JVM_CONFIG_FILE%") do (
        set "JVM_OPTS=!JVM_OPTS! %%a"
    )
)

REM Default JVM params (if jvm.config is empty or missing)
if "!JVM_OPTS!"=="" (
    set "JVM_OPTS=-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"
)

echo [Done] JVM parameters: !JVM_OPTS!
echo.

echo [INFO] Setting up JavaFX...

set "JFX_BASE=%USERPROFILE%\.m2\repository\org\openjfx"
REM Use Windows-specific JavaFX JARs to avoid platform conflict
set "JFX_PATH=%JFX_BASE%\javafx-base\17.0.12\javafx-base-17.0.12-win.jar;%JFX_BASE%\javafx-controls\17.0.12\javafx-controls-17.0.12-win.jar;%JFX_BASE%\javafx-fxml\17.0.12\javafx-fxml-17.0.12-win.jar;%JFX_BASE%\javafx-graphics\17.0.12\javafx-graphics-17.0.12-win.jar"

if not exist "%JFX_BASE%\javafx-base\17.0.12\javafx-base-17.0.12-win.jar" (
    echo [WARNING] JavaFX not found in Maven repository
    echo [INFO] JavaFX is bundled in the fat JAR, launching without --module-path
    set "JFX_MODULES="
) else (
    set "JFX_MODULES=--module-path "%JFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.graphics"
    echo [OK] JavaFX modules found
)

echo.

echo =========================================
echo   %APP_NAME% %APP_VERSION%
echo =========================================
echo.
echo Starting application...
echo.

if "%1"=="--gui" goto :launch_gui

echo [INFO] Using java (console mode)...
echo [INFO] Use "start.bat --gui" to launch without console window
echo.
java !JFX_MODULES! !JVM_OPTS! -jar "%JAR_FILE%"
goto :after_launch

:launch_gui
where javaw >nul 2>&1
if errorlevel 1 goto :launch_console
echo [INFO] Using javaw (GUI mode)...
start "" javaw !JFX_MODULES! !JVM_OPTS! -jar "%JAR_FILE%"
echo [INFO] Application launched in background
goto :eof

:launch_console
echo [WARNING] javaw not found, using java instead
java !JFX_MODULES! !JVM_OPTS! -jar "%JAR_FILE%"

:after_launch

echo.
echo =========================================
echo   Application exited
echo =========================================
echo.
pause
exit /b 0
