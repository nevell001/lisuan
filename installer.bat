@echo off
setlocal

echo ========================================
echo   Cashier System Installer v2.3.1
echo ========================================
echo.

REM Check Java
echo [Checking] Java installation...
where java >nul 2>&1
if errorlevel 1 (
    echo.
    echo [Error] Java is not installed or not in PATH
    echo.
    echo Java 17+ is required.
    echo.
    echo Download options:
    echo   1. Oracle JDK:    https://www.oracle.com/java/technologies/downloads/
    echo   2. OpenJDK (Eclipse Temurin): https://adoptium.net/
    echo   3. Microsoft JDK: https://learn.microsoft.com/en-us/java/openjdk/download
    echo.
    echo After installation:
    echo   1. Add Java bin directory to PATH
    echo   2. Run: java -version
    echo   3. Re-run this installer
    echo.
    pause
    exit /b 1
)

REM Get Java version
for /f "tokens=3" %%a in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%a
set JAVA_VERSION=%JAVA_VERSION:"=%
echo [OK] Java version: %JAVA_VERSION%

REM Check Java version (need 17+)
set JAVA_MAJOR=0
for /f "tokens=1,2 delims=." %%a in ("%JAVA_VERSION%") do (
    if "%%a"=="1" (
        set JAVA_MAJOR=%%b
    ) else (
        set JAVA_MAJOR=%%a
    )
)

if %JAVA_MAJOR% LSS 17 (
    echo.
    echo [Error] Java version too old: %JAVA_VERSION%
    echo Need Java 17 or higher
    echo.
    echo Please upgrade your Java installation
    pause
    exit /b 1
)

REM Check Maven
echo.
echo [Checking] Maven installation...
where mvn >nul 2>&1
if errorlevel 1 (
    echo [Warning] Maven is not in PATH
    echo.
    echo Maven 3.8+ is required for installation.
    echo.
    echo Installation options:
    echo   1. Download: https://maven.apache.org/download.cgi
    echo   2. Chocolatey: choco install maven
    echo   3. Scoop:     scoop install maven
    echo   4. Homebrew:  brew install maven ^(for WSL^)
    echo.
    echo After installation:
    echo   1. Add Maven bin directory to PATH
    echo   2. Run: mvn -version
    echo   3. Re-run this installer
    echo.
    set /p CONTINUE="Do you want to continue anyway? (not recommended) [y/N]: "
    if /i not "%CONTINUE%"=="y" (
        pause
        exit /b 1
    )
    set SKIP_MAVEN=1
) else (
    for /f "tokens=3" %%a in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do set MAVEN_VERSION=%%a
    echo [OK] Maven version: %MAVEN_VERSION%
    set SKIP_MAVEN=0
)

REM Check Docker (optional)
echo.
echo [Checking] Docker installation (optional)...
where docker >nul 2>&1
if errorlevel 1 (
    echo [Info] Docker is not installed (optional)
    echo.
    echo Docker Desktop is recommended for MySQL database.
    echo Download: https://www.docker.com/products/docker-desktop
) else (
    for /f "tokens=4" %%a in ('docker --version 2^>^&1') do set DOCKER_VERSION=%%a
    echo [OK] Docker version: %DOCKER_VERSION%
)

echo.
echo ========================================
echo   Environment Check Complete
echo ========================================
echo.

if %SKIP_MAVEN%==1 (
    echo Maven is not available. Installation will be limited.
    echo.
    set /p CONTINUE="Continue with limited installation? [y/N]: "
    if /i not "%CONTINUE%"=="y" (
        pause
        exit /b 1
    )
)

echo.
echo [Starting] Graphical installer...
echo.

if %SKIP_MAVEN%==1 (
    echo Cannot start graphical installer without Maven
    echo.
    echo Please install Maven first or use the command-line installer:
    echo.
    echo Manual installation steps:
    echo   1. Install Java 17+ and Maven 3.8+
    echo   2. Download the project files
    echo   3. Run: mvn clean package -DskipTests
    echo   4. Configure: config/database.properties
    echo   5. Run: start.bat
    echo.
    pause
    exit /b 1
)

call mvn compile exec:java -Dexec.mainClass="com.cashier.installer.Installer" -Dexec.classpathScope=compile

if errorlevel 1 (
    echo.
    echo [Error] Installation failed
    echo.
    echo Possible causes:
    echo   - Network issues (dependencies download)
    echo   - Java version compatibility
    echo   - Maven configuration issues
    echo.
    echo Please check the error messages above.
    echo.
    pause
    exit /b 1
)

echo.
echo Installation completed successfully!
pause
exit /b 0