@echo off
setlocal enabledelayedexpansion

REM ============================================
REM   Cashier System Start Script (Enhanced)
REM   Version 2.5.4
REM ============================================

set "APP_NAME=Cashier System"

REM Read version from pom.xml automatically (development mode)
set "IS_PACKAGE=0"
set "JAR_FILE="

if exist "pom.xml" (
    for /f "tokens=2 delims=<>" %%a in ('findstr /R "<version>" pom.xml ^| findstr /V "javafx\|maven\|java\|mysql\|hikaricp\|poi\|pdfbox\|controlsfx\|fontawesomefx\|junit\|testfx\|h2\|bcrypt\|logback"') do (
        set "APP_VERSION=%%a"
        goto :version_found
    )
    :version_found
    if "%APP_VERSION%"=="" set "APP_VERSION=2.5.4"
    set "JAR_FILE=target\cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar"
) else (
    REM Package mode - no pom.xml, look for JAR in current directory
    set "IS_PACKAGE=1"
    set "JAR_FILE="
    for %%f in (cashier-system-fx-*-jar-with-dependencies.jar) do (
        set "JAR_FILE=%%f"
        set "FULLNAME=%%~nf"
        goto :jar_found
    )
    :jar_found
    if "%JAR_FILE%"=="" (
        echo [ERROR] No JAR file found in current directory
        echo Expected: cashier-system-fx-*-jar-with-dependencies.jar
        pause
        exit /b 1
    )
    REM Extract version from filename: cashier-system-fx-2.5.4-jar-with-dependencies
    REM Split by '-' and get the 4th token (2.5.4)
    for /f "tokens=4 delims=-" %%v in ("%FULLNAME%") do set "APP_VERSION=%%v"
    if "%APP_VERSION%"=="" set "APP_VERSION=2.5.4"
    echo [INFO] Running in package mode v%APP_VERSION%
)

set "APP_DIR=%~dp0"
set "MAIN_CLASS=com.cashier.CashierSystemFXApplication"
set "CONFIG_FILE=%APP_DIR%\config\jvm.config"

set "DOCKER_AVAILABLE=0"
set "MYSQL_CONTAINER_RUNNING=0"
set "DOCKER_ERROR=0"

cls
echo.
echo =========================================
echo   %APP_NAME% - Start Script v%APP_VERSION%
echo =========================================
echo.

REM ============================================
REM   0. Check for Running Instance
REM ============================================

echo [0/9] Checking for running instances...
echo ----------------------------------------

REM Check for Java processes containing CashierSystem or cashier-system
tasklist /FI "IMAGENAME eq java.exe" /V 2>NUL | findstr /I "CashierSystem\|cashier-system" >NUL 2>&1
if not errorlevel 1 (
    echo [WARNING] 收银系统已在运行中！
    echo.
    echo 检测到系统中已有收银系统进程正在运行。
    echo.
    echo 请检查以下位置：
    echo   - 任务栏中的应用图标
    echo   - 系统托盘中的收银系统图标
    echo   - 任务管理器中的 java.exe 进程
    echo.
    echo 如果确定没有实例运行，请按 Y 继续；否则按 N 退出。
    echo.
    set /p "FORCE_START=强制启动? (Y/N): "
    if /i not "!FORCE_START!"=="y" (
        echo [INFO] 启动已取消
        pause
        exit /b 0
    )
    echo.
)
echo [OK] No conflicting instances found
echo.

REM ============================================
REM   1. Check Java Installation
REM ============================================

echo [1/9] Checking Java installation...
echo ----------------------------------------

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed or not in PATH
    echo.
    echo Java 17+ is required to run this application.
    echo.
    echo Download Java from:
    echo   - Oracle JDK: https://www.oracle.com/java/technologies/downloads/
    echo   - OpenJDK: https://adoptium.net/
    echo   - Windows: winget install Oracle.JDK.17
    echo.
    pause
    exit /b 1
)

REM Get Java version
for /f "usebackq tokens=3" %%a in (`java -version 2^>^&1 ^| findstr /i "version"`) do set "JAVA_VERSION=%%a"
set "JAVA_VERSION=%JAVA_VERSION:"=%"
echo       Version: %JAVA_VERSION%
echo [OK] Java environment check passed
echo.

REM ============================================
REM   2. Check Maven Installation
REM ============================================

echo [2/9] Checking Maven installation...
echo ----------------------------------------

if "%IS_PACKAGE%"=="1" (
    echo [SKIP] Package mode - Maven not required
    echo.
    goto :maven_check_done
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [WARNING] Maven is not in PATH
    echo.
    echo Maven 3.8+ is recommended for building this project.
    echo.
    echo Install Maven:
    echo   - Download: https://maven.apache.org/download.cgi
    echo   - Chocolatey: choco install maven
    echo   - Winget: winget install Apache.Maven
    echo.
    set /p "CONTINUE=Continue without Maven? (Y/N): "
    if /i not "!%CONTINUE%!"=="y" (
        pause
        exit /b 1
    )
) else (
    for /f "usebackq tokens=3" %%a in (`mvn -version 2^>^&1 ^| findstr /i "Apache Maven"`) do set "MAVEN_VERSION=%%a"
    if not "%MAVEN_VERSION%"=="" (
        echo       Version: %MAVEN_VERSION%
        echo [OK] Maven is available
    ) else (
        echo       Version: Unknown
        echo [OK] Maven is available ^<version could not be detected^>
    )
)

:maven_check_done
echo.

REM ============================================
REM   3. Check Docker Installation
REM ============================================

echo [3/9] Checking Docker installation...
echo ----------------------------------------

where docker >nul 2>&1
if errorlevel 1 (
    echo [WARNING] Docker is not installed
    echo.
    echo Docker is used for MySQL database container.
    echo You can use local MySQL instead.
    echo.
    set /p "CONTINUE=Continue with local MySQL setup? (Y/N): "
    if /i not "!CONTINUE!"=="y" (
        echo [INFO] To use Docker MySQL, install Docker Desktop first
        echo   - Download: https://www.docker.com/products/docker-desktop/
        pause
        exit /b 1
    )
    echo [INFO] Will configure local MySQL in database setup step
    set "DOCKER_AVAILABLE=0"
    set "DOCKER_ERROR=1"
    goto :docker_check_done
) else (
    for /f "usebackq tokens=*" %%i in (`docker --version 2^>^&1`) do set "DOCKER_VERSION=%%i"
    if not "%DOCKER_VERSION%"=="" (
        echo       Version: %DOCKER_VERSION%
        set "DOCKER_AVAILABLE=1"
        echo [OK] Docker is available
    ) else (
        echo       Version: Unknown
        set "DOCKER_AVAILABLE=1"
        echo [WARNING] Docker version could not be detected, but it is available
    )
)

:docker_check_done
echo.

REM Check Docker daemon
if %DOCKER_AVAILABLE%==1 (
    docker info >nul 2>&1
    if errorlevel 1 (
        echo [WARNING] Docker daemon is not running
        echo.
        echo Please start Docker Desktop and try again.
        echo.
        echo Starting Docker Desktop...
        start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
        echo Waiting for Docker to start ^<15 seconds^>...
        timeout /t 15 /nobreak >nul

        REM Check again
        docker info >nul 2>&1
        if errorlevel 1 (
            echo [ERROR] Failed to start Docker daemon
            echo Please start Docker Desktop manually and try again.
            pause
            exit /b 1
        )
        echo [OK] Docker daemon is now running
    ) else (
        echo [OK] Docker daemon is running
        set "MYSQL_CONTAINER_RUNNING=1"
    )
)
echo.

REM ============================================
REM   4. Check MySQL Container
REM ============================================

echo [4/9] Checking MySQL container...
echo ----------------------------------------

if %DOCKER_AVAILABLE%==1 (
    docker ps -f name=cashier-mysql --format "{{.State}}" >nul 2>&1
    if errorlevel 1 (
        echo [INFO] MySQL container is not running
    ) else (
        echo [OK] MySQL container is running: cashier-mysql
        set "MYSQL_CONTAINER_RUNNING=1"
    )
) else (
    echo [SKIP] Docker not available, skipping container check
)
echo.

if %MYSQL_CONTAINER_RUNNING%==1 (
    echo [INFO] Checking MySQL port...
    netstat -ano | findstr ":3306 " >nul 2>&1
    if errorlevel 1 (
        echo [OK] Port 3306 is available
    ) else (
        echo [WARNING] Port 3306 is already in use
        echo.
        echo This may indicate another MySQL instance is running
    )
)
echo.

REM ============================================
REM   5. Check Application Files
REM ============================================

echo [5/9] Checking application files...
echo ----------------------------------------

cd /d "%APP_DIR%"

if not exist "%JAR_FILE%" (
    if "%IS_PACKAGE%"=="1" (
        echo [ERROR] Application JAR not found: %JAR_FILE%
        echo.
        echo This appears to be a package distribution, but the JAR file is missing.
        echo.
        echo Please ensure you have the complete package:
        echo   - cashier-system-fx-*-jar-with-dependencies.jar
        echo.
        pause
        exit /b 1
    )
    echo [INFO] Compiled JAR not found: %JAR_FILE%
    echo [INFO] Starting compilation...
    echo.
    echo Compiling with Maven, this may take a while...
    echo.

    call mvn clean package -DskipTests

    if errorlevel 1 (
        echo [ERROR] Compilation failed
        echo.
        echo Please check:
        echo   - Java version ^<need 17+^>
        echo   - Maven installation
        echo   - Network connectivity
        echo.
        pause
        exit /b 1
    )
) else (
    echo [OK] JAR file found: %JAR_FILE%
)
echo.

REM ============================================
REM   6. Start Docker MySQL Container (if needed)
REM ============================================

echo [6/9] Starting MySQL container...
echo ----------------------------------------

if %DOCKER_AVAILABLE%==1 (
    if %MYSQL_CONTAINER_RUNNING%==0 (
        echo [INFO] Starting MySQL container...
        echo.
        cd /d "%APP_DIR%"
        docker compose up -d mysql
        echo.
        timeout /t 10 /nobreak >nul
        
        REM Check if container started
        docker ps -f name=cashier-mysql --format "{{.State}}" >nul 2>&1
        if errorlevel 1 (
            echo [ERROR] Failed to start MySQL container
            echo.
            echo Possible causes:
            echo   - Port 3306 is already in use by another service
            echo   - Docker configuration error
            echo.
            echo Please check docker-compose.yml file
            pause
            exit /b 1
        ) else (
            echo [OK] MySQL container started successfully
            set "MYSQL_CONTAINER_RUNNING=1"
        )
        echo.
        echo [INFO] Waiting for MySQL to be ready ^<10 seconds^>...
        timeout /t 10 /nobreak >nul
    ) else (
        echo [OK] MySQL container is already running
    )
) else (
    echo [SKIP] Docker not available, skipping container start
)
echo.

REM ============================================
REM   7. Check Database Configuration
REM ============================================

echo [7/9] Checking database configuration...
echo ----------------------------------------

set "DB_CONFIG_FILE=%APP_DIR%config\database.properties"

if not exist "%DB_CONFIG_FILE%" (
    echo [INFO] Database configuration not found
    echo.
    echo Please configure database connection:
    echo.

    REM Ask for database type
    echo Select database type:
    echo   1. Local MySQL ^(localhost^)
    echo   2. Docker MySQL ^(via docker-compose^)
    echo   3. Remote MySQL
    echo.
    set /p "DB_TYPE=Choose ^(1/2/3^): "

    if "!DB_TYPE!"=="1" (
        set "DB_HOST=localhost"
        set "DB_PORT=3306"
        set "DB_NAME=cashier_system"
        echo.
        echo Configure local MySQL connection:
        set /p "DB_USER=Enter MySQL username [root]: "
        if "!DB_USER!"=="" set "DB_USER=root"
        set /p "DB_PASS=Enter MySQL password: "
    ) else if "!DB_TYPE!"=="2" (
        set "DB_HOST=localhost"
        set "DB_PORT=3306"
        set "DB_NAME=cashier_system"
        set "DB_USER=root"
        set "DB_PASS=RootPassword123!"
        echo.
        echo [INFO] Using Docker MySQL defaults
    ) else if "!DB_TYPE!"=="3" (
        echo.
        set /p "DB_HOST=Enter MySQL host: "
        set /p "DB_PORT=Enter MySQL port [3306]: "
        if "!DB_PORT!"=="" set "DB_PORT=3306"
        set /p "DB_NAME=Enter database name [cashier_system]: "
        if "!DB_NAME!"=="" set "DB_NAME=cashier_system"
        set /p "DB_USER=Enter MySQL username: "
        set /p "DB_PASS=Enter MySQL password: "
    ) else (
        echo [WARN] Invalid choice, using defaults
        set "DB_HOST=localhost"
        set "DB_PORT=3306"
        set "DB_NAME=cashier_system"
        set "DB_USER=root"
        set "DB_PASS=RootPassword123!"
    )

    echo.
    echo Creating database configuration...
    echo.

    REM Create config directory if not exists
    if not exist "%APP_DIR%config" mkdir "%APP_DIR%config"

    REM Write database.properties
    (
        echo # Cashier System Database Configuration
        echo # Generated by start.bat on %date% %time%
        echo.
        echo db.url=jdbc:mysql://!DB_HOST!:!DB_PORT!/!DB_NAME!?useSSL=false^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&characterEncoding=utf8mb4
        echo db.username=!DB_USER!
        echo db.password=!DB_PASS!
        echo db.pool.size=10
        echo db.connection.timeout=30000
        echo db.idle.timeout=600000
        echo db.max.lifetime=1800000
    ) > "%DB_CONFIG_FILE%"

    echo [OK] Database configuration created: %DB_CONFIG_FILE%
    echo.
    echo Configuration summary:
    echo   Host: !DB_HOST!:!DB_PORT!
    echo   Database: !DB_NAME!
    echo   Username: !DB_USER!
    echo.
) else (
    echo [OK] Database configuration found
)
echo.

REM ============================================
REM   8. Build JVM Parameters
REM ============================================

echo [8/9] Building JVM parameters...
echo ----------------------------------------

set "JVM_OPTS=-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"

REM Check if custom JVM config exists
if exist "%CONFIG_FILE%" (
    echo [INFO] Loading custom JVM configuration from %CONFIG_FILE%...
    set "CUSTOM_OPTS="
    for /f "usebackq tokens=*" %%a in ("%CONFIG_FILE%") do (
        set "LINE=%%a"
        REM Ignore empty lines and comments
        if not "!LINE!"=="" (
            if not "!LINE:~0,1!"=="#" (
                set "CUSTOM_OPTS=!CUSTOM_OPTS! !LINE!"
            )
        )
    )
    if not "%CUSTOM_OPTS%"=="" (
        set "JVM_OPTS=%CUSTOM_OPTS%"
        echo [OK] Custom JVM options loaded
    )
)

if "%JVM_OPTS%"=="" (
    echo [INFO] Using default JVM parameters
)

echo [Done] JVM parameters built
echo       JVM Options: %JVM_OPTS%
echo.

REM ============================================
REM   9. Start Application
REM ============================================

echo [9/9] Starting application...
echo ----------------------------------------

echo.
echo =========================================
echo   %APP_NAME% %APP_VERSION%
echo =========================================
echo.
echo Starting, please wait...
echo.

REM Start application - prioritize JAR for production, Maven for development
if exist "%JAR_FILE%" (
    echo [INFO] Using packaged JAR: %JAR_FILE%
    echo.

    REM Use javaw for console-free startup (Windows only)
    REM Check if javaw is available
    where javaw >nul 2>&1
    if not errorlevel 1 (
        echo [INFO] Using javaw for console-free startup...
        javaw %JVM_OPTS% -jar "%JAR_FILE%"
    ) else (
        echo [INFO] javaw not found, using java...
        start /B java %JVM_OPTS% -jar "%JAR_FILE%"
    )
) else (
    echo [INFO] JAR not found, using Maven JavaFX plugin...
    echo.
    call mvn javafx:run -U
)

REM Check exit code
if errorlevel 1 (
    echo.
    echo =========================================
    echo   ERROR: Application exited abnormally
    echo =========================================
    echo.
    echo Error Code: %errorlevel%
    echo.
    echo Possible causes:
    echo   - Database connection failed
    echo   - Missing required files
    echo   - JavaFX runtime issues
    echo   - Insufficient memory
    echo   - Maven dependency download failed
    echo.
    echo Please check:
    echo   1. Application log: logs\app.log
    echo   2. Database config: config\database.properties
    echo   3. JVM config: config\jvm.config
    echo.
    echo If Maven plugin download failed, try:
    echo   mvn dependency:purge-local-repository -DmanualInclude=org.openjfx:javafx-maven-plugin
    echo   start.bat
    echo.
    echo Opening log file...
    if exist "logs\app.log" (
        notepad logs\app.log
    ) else (
        echo [WARNING] Log file not found
    )
    pause
    exit /b 1
)

echo.
echo =========================================
echo   Application exited normally
echo =========================================
echo.

echo [INFO] Application started successfully
echo.
echo [TIPS]
echo   - Press Ctrl+C to stop the application
echo   - Check logs\app.log for detailed logs
echo   - Database: localhost:3306 (via Docker)
echo   - Config directory: config\
echo.
pause
exit /b 0
