@echo off
REM ============================================
REM Cashier System Silent Launcher (Windows)
REM ============================================
REM This launcher starts the application without showing a console window
REM ============================================

setlocal

REM Set application directory
set APP_DIR=%~dp0
cd /d "%APP_DIR%"

REM Set application info
set APP_NAME=Cashier System
set APP_VERSION=2.3.0
set MAIN_CLASS=com.cashier.CashierSystemFXApplication

REM Set JAR file path
set JAR_FILE=target\cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar

REM Check if JAR file exists
if not exist "%JAR_FILE%" (
    echo JAR file not found, compiling...
    call mvn clean package -DskipTests
)

REM Start application using javaw (no console window)
start "" javaw -jar "%JAR_FILE%"

exit /b 0