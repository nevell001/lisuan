@echo off
setlocal enabledelayedexpansion

REM ============================================
REM   LiSuan Startup Script (Windows)
REM   Version 2.5.6
REM ============================================

cd /d "%~dp0"

set "APP_NAME=LiSuan"
set "APP_VERSION=2.5.7"

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

set "JAR_FILE=target\lisuan-fx-%APP_VERSION%-jar-with-dependencies.jar"

if not exist "%JAR_FILE%" (
    echo [WARNING] JAR file not found: %JAR_FILE%
    echo [INFO] Building project...
    echo.
    call mvn clean package -DskipTests -Dinnosetup.skip=true
    if errorlevel 1 (
        echo [ERROR] Build failed
        pause
        exit /b 1
    )
    echo [OK] Build completed
) else (
    echo [OK] JAR file found
)

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
set "JFX_PATH=%JFX_BASE%\javafx-base\17.0.12;%JFX_BASE%\javafx-controls\17.0.12;%JFX_BASE%\javafx-fxml\17.0.12;%JFX_BASE%\javafx-graphics\17.0.12"

if not exist "%JFX_BASE%\javafx-base\17.0.12" (
    echo [WARNING] JavaFX not found in Maven repository
    echo [INFO] Will use standard classpath
    set "JFX_PATH="
) else (
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
java --module-path "%JFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.graphics !JVM_OPTS! -jar "%JAR_FILE%"
goto :after_launch

:launch_gui
where javaw >nul 2>&1
if errorlevel 1 goto :launch_console
echo [INFO] Using javaw (GUI mode)...
start "" javaw --module-path "%JFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.graphics !JVM_OPTS! -jar "%JAR_FILE%"
echo [INFO] Application launched in background
goto :eof

:launch_console
echo [WARNING] javaw not found, using java instead
java --module-path "%JFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.graphics !JVM_OPTS! -jar "%JAR_FILE%"

:after_launch

echo.
echo =========================================
echo   Application exited
echo =========================================
echo.
pause
exit /b 0
