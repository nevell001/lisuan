@echo off
setlocal enabledelayedexpansion

REM ============================================
REM   Cashier System Installer (Enhanced)
REM   Version 2.3.2 - with Docker Support
REM ============================================

set "INSTALLER_VERSION=2.3.2"
set "ERROR_OCCURRED=0"

cls
echo.
echo =========================================
echo   Cashier System Installer v%INSTALLER_VERSION%
echo =========================================
echo.

REM ============================================
REM   1. Check Java Installation
REM ============================================
echo [1/6] Checking Java installation...
echo ----------------------------------------
where java >nul 2>&1
if errorlevel 1 (
    echo [Error] Java is not installed or not in PATH
    echo.
    echo Java 17+ is required to run installer.
    echo.
    echo Download Java from:
    echo   - Oracle JDK: https://www.oracle.com/java/technologies/downloads/
    echo   - OpenJDK: https://adoptium.net/
    echo.
    echo Or use Chocolatey: choco install openjdk
    pause
    exit /b 1
)

REM Get Java version
for /f "tokens=3" %%a in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%a"
set "JAVA_VERSION=%JAVA_VERSION:"=%
echo       Version: %JAVA_VERSION%
echo.

REM ============================================
REM   2. Check Maven Installation
REM ============================================
echo [2/6] Checking Maven installation...
echo ----------------------------------------
where mvn >nul 2>&1
if errorlevel 1 (
    echo [Warning] Maven is not in PATH
    echo.
    echo Maven 3.8+ is required for installation.
    echo.
    echo Please install Maven:
    echo   - Download: https://maven.apache.org/download.cgi
    echo   - Chocolatey: choco install maven
    echo   - Winget: winget install Apache.Maven
    echo.
    set /p "CONTINUE=Continue anyway? (y/N): "
    if /i not "%CONTINUE%"=="y" (
        pause
        exit /b 1
    )
    set "ERROR_OCCURRED=1"
) else (
    for /f "tokens=3" %%a in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do set "MAVEN_VERSION=%%a"
    echo       Version: %MAVEN_VERSION%
    echo [OK] Maven is available
    echo.
)
echo.

REM ============================================
REM   3. Check Docker Installation
REM ============================================
echo [3/6] Checking Docker installation...
echo ----------------------------------------
where docker >nul 2>&1
if errorlevel 1 (
    echo [Warning] Docker is not installed
    echo.
    echo Docker is required for MySQL database container.
    echo.
    echo Install Docker for Windows:
    echo   - Download: https://www.docker.com/products/docker-desktop/
    echo   - Winget: winget install Docker.DockerDesktop
    echo.
    set /p "CONTINUE=Continue without Docker? (y/N): "
    if /i not "!%CONTINUE%!"=="y" (
        pause
        exit /b 1
    )
    set "ERROR_OCCURRED=1"
    goto :skip_docker_checks
)

REM Get Docker version
docker --version >nul 2>&1
if errorlevel 1 (
    echo [Error] Failed to get Docker version
    set "ERROR_OCCURRED=1"
) else (
    echo [OK] Docker is available
    echo.
)
echo.

REM ============================================
REM   4. Check Docker Daemon Status
REM ============================================
echo [4/6] Checking Docker daemon status...
echo ----------------------------------------

REM Check if Docker daemon is running
docker info >nul 2>&1
if errorlevel 1 (
    echo [Warning] Docker daemon is not running
    echo.
    echo Please start Docker Desktop and try again.
    echo.
    echo Starting Docker Desktop...
    echo.
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    echo Waiting for Docker to start (30 seconds)...
    timeout /t 30 /nobreak >nul

    REM Check again
    docker info >nul 2>&1
    if errorlevel 1 (
        echo [Error] Failed to start Docker daemon
        echo.
        echo Please start Docker Desktop manually and try again.
        pause
        exit /b 1
    )
    echo [OK] Docker daemon is now running
    echo.
) else (
    echo [OK] Docker daemon is running
    echo.
)
echo.

REM ============================================
REM   5. Check Docker Compose
REM ============================================
echo [5/6] Checking Docker Compose...
echo ----------------------------------------

REM Check if docker compose (v2) is available
docker compose version >nul 2>&1
if errorlevel 1 (
    echo [Warning] Docker Compose v2 is not available
    echo.
    echo Please update Docker Desktop to the latest version
    echo.
    set "ERROR_OCCURRED=1"
) else (
    echo [OK] Docker Compose v2 is available
    echo.
)
echo.

REM ============================================
REM   6. Check Port Availability
REM ============================================
echo [6/6] Checking port availability...
echo ----------------------------------------

set "PORT_CHECKED=0"

REM Check port 3306
netstat -ano | findstr ":3306 " >nul 2>&1
if errorlevel 1 (
    echo [OK] Port 3306 is available
    set "PORT_CHECKED=1"
) else (
    echo [Warning] Port 3306 is already in use
    echo.
    echo This may conflict with the MySQL container.
    echo.
    echo Possible causes:
    echo   - Another MySQL instance is running
    echo   - Port 3306 is used by another application
    echo.
    echo Options:
    echo   1. Stop conflicting service
    echo   2. Change port in docker-compose.yml to "3307:3306"
    echo.
    netstat -ano | findstr ":3306"
    echo.
    set /p "CONTINUE=Continue anyway? (y/N): "
    if /i not "%CONTINUE%"=="y" (
        pause
        exit /b 1
    )
    set "PORT_CONFLICT=1"
    set "ERROR_OCCURRED=1"
)

:skip_docker_checks

echo.
echo =========================================
echo   Installation Summary
echo =========================================
echo.
if %ERROR_OCCURRED%==1 (
    echo [Warning] Some checks failed, but continuing...
    echo.
)

echo [Info] Environment checks completed
echo [Info] Java: %JAVA_VERSION%
echo [Info] Maven: %MAVEN_VERSION%
echo [Info] Docker: Available
echo [Info] Port 3306: %PORT_CONFLICT%
echo.

REM =========================================
echo   Starting installation...
echo =========================================
echo.

if not exist logs mkdir logs

set "LOG_FILE=logs\installer-%date:~-4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log"

echo [Info] Log file: %LOG_FILE%
echo.

REM Try to run installer
echo [Info] Preparing build environment...
echo [Info] Downloading dependencies (this may take a while)...
echo.

REM Call Maven build
cd /d "%~dp0"
call mvn clean compile exec:java -Dexec.mainClass="com.cashier.installer.Installer" -Dexec.classpathScope=compile > "%LOG_FILE%" 2>&1

if errorlevel 1 (
    echo.
    echo =========================================
    echo   Installation failed
    echo =========================================
    echo.
    echo Log file: %LOG_FILE%
    echo.
    echo Possible causes:
    echo   - Java version too old (need JDK 17+)
    echo   - Maven not working
    echo   - Network issues (dependencies download failed)
    echo   - Insufficient disk space
    echo   - Missing source files
    echo.
    echo Quick checks:
    echo   1. Java version: %JAVA_VERSION%
    echo   2. Maven version: %MAVEN_VERSION%
    echo   3. Check log file for details
    echo.
    echo Opening log file...
    echo.
    start notepad "%LOG_FILE%"
    echo.
    pause
    exit /b 1
)

echo.
echo =========================================
echo   Installation completed successfully
echo =========================================
echo.
pause
