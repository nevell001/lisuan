@echo off
setlocal enabledelayedexpansion

REM ============================================
REM   LiSuan Installation Script (Windows)
REM   简化版 - 环境检查 + 构建 + 生成配置工具
REM ============================================

cd /d "%~dp0"

set "APP_VERSION=2.5.6"

cls
echo.
echo =========================================
echo   LiSuan Installation
echo =========================================
echo.

REM ============================================
REM   1. Check Java
REM ============================================

echo [1/4] Checking Java environment...
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

for /f "usebackq tokens=3" %%a in (`java -version 2^>^&1 ^| findstr /i "version"`) do set "JAVA_VERSION=%%a"
set "JAVA_VERSION=%JAVA_VERSION:"=%"
echo       Version: %JAVA_VERSION%
echo [OK] Java check passed
echo.

REM ============================================
REM   2. Check Maven
REM ============================================

echo [2/4] Checking Maven environment...
echo ----------------------------------------

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found!
    echo.
    echo Please install Maven 3.8+:
    pause
    exit /b 1
)

for /f "usebackq tokens=3" %%a in (`mvn -version 2^>^&1 ^| findstr /i "Apache Maven"`) do set "MAVEN_VERSION=%%a"
echo       Version: %MAVEN_VERSION%
echo [OK] Maven check passed
echo.

REM ============================================
REM   3. Create Directories
REM ============================================

echo [3/4] Creating necessary directories...
echo ----------------------------------------

if not exist "config" mkdir config
if not exist "data" mkdir data
if not exist "logs" mkdir logs

if not exist "config\database.properties" (
    copy /Y "config\database.properties.example" "config\database.properties" >nul 2>&1
    echo [CREATE] config\database.properties
)

if not exist "config\jvm.config" (
    copy /Y "config\jvm.config.example" "config\jvm.config" >nul 2>&1
    echo [CREATE] config\jvm.config
)

echo [OK] Directories created
echo.

REM ============================================
REM   4. Build Project
REM ============================================

echo [4/4] Building project...
echo ----------------------------------------
echo This may take a while on first run...
echo.

if exist "target\lisuan-fx-%APP_VERSION%-jar-with-dependencies.jar" (
    set /p "REBUILD=Rebuild existing JAR? (y/N): "
    if /i "!REBUILD!"=="y" (
        echo [CLEAN] Cleaning old files...
        call mvn clean
    )
)

echo Compiling...
call mvn clean package -DskipTests -Dinnosetup.skip=true

if errorlevel 1 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)

echo [OK] Project built successfully
echo.

REM ============================================
REM   Generate DataConfig.bat
REM ============================================

REM ============================================
REM   Generate DataConfig.bat
REM ============================================

echo [INFO] Creating database configuration tool...

(
    echo @echo off
    echo setlocal enabledelayedexpansion
    echo.
    echo cd /d "%%~dp0"
    echo.
    echo echo =========================================
    echo echo   LiSuan Database Configuration
    echo echo =========================================
    echo echo.
    echo.
    echo echo [INFO] Launching database configuration tool...
    echo echo.
    echo REM Build classpath
    echo set "CP=target\classes"
    echo.
    echo REM Add MySQL JDBC driver from Maven repo (supports both 8.x versions)
    echo set "M2_REPO=%%USERPROFILE%%\.m2\repository"
    echo if exist "%%M2_REPO%%\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar" (
    echo     set "MYSQL_JAR=%%M2_REPO%%\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar"
    echo ^) else if exist "%%M2_REPO%%\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" (
    echo     set "MYSQL_JAR=%%M2_REPO%%\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar"
    echo ^) else if exist "%%M2_REPO%%\mysql\mysql-connector-java\8.0.33\mysql-connector-java-8.0.33.jar" (
    echo     set "MYSQL_JAR=%%M2_REPO%%\mysql\mysql-connector-java\8.0.33\mysql-connector-java-8.0.33.jar"
    echo ^) else (
    echo     echo [ERROR] MySQL JDBC driver not found in Maven repository!
    echo     echo Please run: mvn clean package
    echo     pause
    echo     exit /b 1
    echo ^)
    echo.
    echo set "CP=%%CP%%;%%MYSQL_JAR%%"
    echo.
    echo java -cp "%%CP%%" com.cashier.installer.DatabaseConfigDialog
    echo.
    echo if errorlevel 1 (
    echo     echo.
    echo     echo [ERROR] Configuration tool failed
    echo     echo.
    echo     echo Make sure the project is built: mvn clean package
    echo     pause
    echo     exit /b 1
    echo ^)
    echo.
    echo echo.
    echo echo [INFO] Configuration complete!
    echo echo You can now run start.bat to launch the application.
    echo echo.
    echo pause
) > DataConfig.bat

echo [OK] Created DataConfig.bat

echo.
echo =========================================
echo   Installation Complete!
echo =========================================
echo.
echo Application:
echo   Version: %APP_VERSION%
echo   JAR: target\lisuan-fx-%APP_VERSION%-jar-with-dependencies.jar
echo.
echo Next Steps:
echo   1. Run DataConfig.bat to configure database
echo   2. Run start.bat to launch the application
echo.
echo Default Login: admin / admin123
echo.
pause
