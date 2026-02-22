@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   Cashier System Installer
echo ========================================
echo.

REM Check if Java is installed
where java >nul 2>&1
if errorlevel 1 (
    echo [Error] Java is not installed or not in PATH
    echo Please install JDK 17 or higher
    pause
    exit /b 1
)

echo [Info] Java found
echo [Info] Starting graphical installer...
echo.

REM Check if compiled
if not exist "target\classes\com\cashier\installer\Installer.class" (
    echo [Info] Compiling installer...
    call mvn compile -DskipTests
    if errorlevel 1 (
        echo [Error] Failed to compile installer
        pause
        exit /b 1
    )
)

REM Set classpath
set "CLASSPATH=target\classes;target\classes\com\cashier\installer;"

REM Add all JAR files from target
for %%f in (target\*.jar) do (
    set "CLASSPATH=!CLASSPATH!;%%f"
)

REM Add Maven dependencies
for /f "delims=" %%f in ('dir /b /s "%USERPROFILE%\.m2\repository\*.jar" 2^>nul ^| findstr /i "mysql-connector hikari logback slf4j"') do (
    set "CLASSPATH=!CLASSPATH!;%%f"
)

echo [Info] Starting installer...
java -cp "!CLASSPATH!" com.cashier.installer.Installer

if errorlevel 1 (
    echo.
    echo [Error] Installer failed to start
    pause
    exit /b 1
)

exit /b 0