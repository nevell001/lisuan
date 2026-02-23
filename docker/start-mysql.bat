@echo off
chcp 65001 >nul 2>&1
REM ============================================
REM Cashier System MySQL Docker Quick Start Script (Windows)
REM ============================================

setlocal enabledelayedexpansion

echo ========================================
echo   Cashier System MySQL Docker Startup
echo ========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [Error] Docker not running!
    echo Please start Docker Desktop first
    pause
    exit /b 1
)

REM Create necessary directories
echo [1/4] Creating necessary directories...
if not exist "docker\mysql-init" mkdir "docker\mysql-init"
if not exist "docker\mysql-backup" mkdir "docker\mysql-backup"
if not exist "config" mkdir "config"
echo [Done] Directories created
echo.

REM Check configuration file
if not exist "config\database.properties" (
    echo [2/4] Creating database config file...
    copy "config\database.properties.example" "config\database.properties" >nul
    echo [Done] Config file created: config\database.properties
    echo [Tip] Please edit this file and modify database connection info
) else (
    echo [2/4] Config file already exists
)
echo.

REM Check if container already exists
docker ps -a | findstr "cashier-mysql" >nul
if %errorlevel% equ 0 (
    echo [3/4] Detected existing MySQL container
    set /p REPLY="Remove old container and recreate? (y/N): "
    if /i "!REPLY!"=="y" (
        echo [Stop] Stopping and removing old container...
        docker-compose down -v
        echo [Done] Old container removed
    ) else (
        echo [Start] Starting existing container...
        docker-compose start
        echo [Done] Container started
        goto :success
    )
)

REM Start MySQL container
echo [3/4] Starting MySQL container...
docker-compose up -d

REM Wait for MySQL to start
echo [Wait] Waiting for MySQL to start...
timeout /t 15 /nobreak >nul

REM Check container status
docker ps | findstr "cashier-mysql" >nul
if %errorlevel% equ 0 (
    goto :success
) else (
    goto :failure
)

:success
echo.
echo ========================================
echo [Success] MySQL started successfully!
echo ========================================
echo.
echo Database connection info:
echo   Host: localhost
echo   Port: 3306
echo   Database: cashier_system
echo   Username: cashier
echo   Password: YourStrongPassword123!
echo.
echo Management tools:
echo   Recommended: DBeaver (https://dbeaver.io/download/)
echo.
echo Common commands:
echo   View logs: docker-compose logs -f mysql
echo   Stop container: docker-compose stop
echo   Restart container: docker-compose restart
echo   Enter container: docker exec -it cashier-mysql bash
echo.
echo Next steps:
echo   1. Modify config\database.properties
echo   2. Start cashier system application
echo   3. Application will automatically migrate data to MySQL
echo.
pause
exit /b 0

:failure
echo.
echo ========================================
echo [Failure] MySQL startup failed!
echo ========================================
echo.
echo View error logs:
echo   docker-compose logs mysql
echo.
pause
exit /b 1