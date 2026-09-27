@echo off
setlocal enabledelayedexpansion

REM ============================================
REM   LiSuan Installation Script (Windows)
REM   简化版 - 环境检查 + 构建 + 生成配置工具
REM ============================================

cd /d "%~dp0"

REM 加载 .env 文件（如果存在）
if exist ".env" (
    echo [INFO] Loading configuration from .env file...
    for /f "usebackq tokens=1,2 delims==" %%a in (".env") do (
        REM 跳过注释行
        echo %%a | findstr /r "^[#]" >nul
        if errorlevel 1 (
            set "%%a=%%b"
        )
    )
)

REM Try to read version from pom.xml
set "APP_VERSION="
for /f "tokens=3 delims=<>" %%a in ('findstr /R "<version>[0-9]" pom.xml 2^>nul ^| findstr /V "javafx maven java mysql hikaricp"') do (
    set "APP_VERSION=%%a"
    goto :version_found
)
:version_found
REM Fallback version if pom.xml not found or unreadable
if "%APP_VERSION%"=="" set "APP_VERSION=2.6.0"

REM 环境类型：development 或 production
if "%ENVIRONMENT%"=="" set "ENVIRONMENT=development"

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
if not exist "temp" mkdir temp

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

REM 检测是否已有 fat JAR。
REM 不能用 for %%f in (通配符)：无匹配时 cmd 会把通配符原样当成一项，于是这里永远"检测到已有 JAR"、
REM 干净机器上会跳过构建，装完启动时才报 Unable to access jarfile。
set "EXISTING_JAR="
for /f "delims=" %%f in ('dir /b /o-d "target\lisuan-fx-*-jar-with-dependencies.jar" 2^>nul') do (
    set "EXISTING_JAR=target\%%f"
    goto :jar_check_done
)
:jar_check_done

if defined EXISTING_JAR (
    echo [SKIP] Detected existing compiled JAR: %EXISTING_JAR%
    echo [TIP] Run 'mvn clean package -DskipTests' to rebuild
    goto :build_done
)

echo Compiling...
call mvn clean package -DskipTests

if errorlevel 1 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)

REM 构建"成功"也可能没产出 fat JAR（例如 packaging 被改过），这里必须真的校验一次
set "EXISTING_JAR="
for /f "delims=" %%f in ('dir /b /o-d "target\lisuan-fx-*-jar-with-dependencies.jar" 2^>nul') do (
    set "EXISTING_JAR=target\%%f"
    goto :jar_check_done2
)
:jar_check_done2

if not defined EXISTING_JAR (
    echo [ERROR] Build finished but no fat JAR found in target\
    echo Please run: mvn clean package -DskipTests
    pause
    exit /b 1
)

:build_done

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
    echo REM 加载 .env 文件（如果存在）
    echo if exist ".env" ^(
    echo     echo [INFO^] Loading configuration from .env file...
    echo     for /f "usebackq tokens=1,2 delims==" %%%%a in ^(".env"^) do ^(
    echo         echo %%%%a ^| findstr /r "^[#^]" ^>nul
    echo         if errorlevel 1 ^(
    echo             set "%%%%a=%%%%b"
    echo         ^)
    echo     ^)
    echo ^)
    echo.
    echo echo =========================================
    echo echo   LiSuan Database Configuration
    echo echo =========================================
    echo echo.
    echo echo [INFO^] ENVIRONMENT: %%ENVIRONMENT%%
    echo echo [INFO] Launching database configuration tool...
    echo echo.
    echo REM Find executable fat JAR. It contains the installer and all runtime dependencies.
    echo REM for /f + dir /b：无匹配时不会像 for %%%%f in ^(通配符^) 那样把通配符原样当成路径。
    echo set "JAR_FILE="
    echo for /f "delims=" %%%%f in ^('dir /b /o-d "target\lisuan-fx-*-jar-with-dependencies.jar"'^) do ^(
    echo     set "JAR_FILE=target\%%%%f"
    echo     goto :jar_found
    echo ^)
    echo :jar_found
    echo.
    echo if not defined JAR_FILE ^(
    echo     echo [ERROR] Application JAR not found in target\
    echo     echo Please run: mvn clean package -DskipTests
    echo     pause
    echo     exit /b 1
    echo ^)
    echo.
    echo echo [INFO] Found application JAR: %%JAR_FILE%%
    echo java -cp "%%JAR_FILE%%" com.cashier.installer.DatabaseConfigDialog
    echo.
    echo if errorlevel 1 ^(
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

echo [INFO] Launching GUI database configuration...
call DataConfig.bat
if errorlevel 1 (
    echo [ERROR] Database configuration failed
    pause
    exit /b 1
)

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
echo   1. Use the GUI configuration tool to save database settings
echo   2. Click Save ^& Start in the GUI, or run start.bat later
echo.
echo Default Login: admin / admin123
echo.
pause
