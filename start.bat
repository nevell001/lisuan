@echo off
setlocal enabledelayedexpansion

REM ============================================
REM   Cashier System Start Script (Enhanced)
REM   Version 2.5.0
REM ============================================

set "APP_NAME=Cashier System"

REM Read version from pom.xml automatically
for /f "tokens=2 delims=<>" %%a in ('findstr /R "<version>" pom.xml ^| findstr /V "javafx\|maven\|java\|mysql\|hikaricp\|poi\|pdfbox\|controlsfx\|fontawesomefx\|junit\|testfx\|h2\|bcrypt\|logback"') do (
    set "APP_VERSION=%%a"
    goto :version_found
)
:version_found
if "%APP_VERSION%"=="" set "APP_VERSION=2.5.0"
set "APP_DIR=%~dp0"
set "MAIN_CLASS=com.cashier.CashierSystemFXApplication"
set "CONFIG_FILE=%APP_DIR%\config\jvm.config"
set "JAR_FILE=target\cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar"

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
REM   1. Check Java Installation
REM ============================================

echo [1/8] Checking Java installation...
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

echo [2/8] Checking Maven installation...
echo ----------------------------------------

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
echo.

REM ============================================
REM   3. Check Docker Installation
REM ============================================

echo [3/8] Checking Docker installation...
echo ----------------------------------------

where docker >nul 2>&1
if errorlevel 1 (
    echo [WARNING] Docker is not installed
    echo.
    echo Docker is required for MySQL database container.
    echo.
    echo Install Docker:
    echo   - Download: https://www.docker.com/products/docker-desktop/
    echo   - Winget: winget install Docker.DockerDesktop
    echo.
    set /p "CONTINUE=Continue without Docker? (Y/N): "
    if /i not "!%CONTINUE%!"=="y" (
        pause
        exit /b 1
    )
    set "DOCKER_ERROR=1"
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

echo [4/8] Checking MySQL container...
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

echo [5/8] Checking application files...
echo ----------------------------------------

cd /d "%APP_DIR%"

if not exist "%JAR_FILE%" (
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

echo [6/8] Checking MySQL container...
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
REM   7. Build JVM Parameters
REM ============================================

echo [7/8] Building JVM parameters...
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
REM   8. Start Application
REM ============================================

echo [8/8] Starting application...
echo ----------------------------------------

echo.
echo =========================================
echo   %APP_NAME% %APP_VERSION%
echo =========================================
echo.
echo Starting, please wait...
echo.

REM Start application using Maven JavaFX plugin with forced dependency update
call mvn javafx:run -U

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
