@echo off
setlocal

echo ========================================
echo   Cashier System Installer
echo ========================================
echo.

REM Check if Java is installed
where java >nul 2>&1
if errorlevel 1 (
    echo [Error] Java is not installed or not in PATH
    echo.
    echo Java 17+ is required to run the installer.
    echo.
    echo Download Java from:
    echo   - Oracle JDK: https://www.oracle.com/java/technologies/downloads/
    echo   - OpenJDK: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%a in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%a
set JAVA_VERSION=%JAVA_VERSION:"=%
echo [Info] Java version: %JAVA_VERSION%

REM Check if Maven is in PATH
where mvn >nul 2>&1
if errorlevel 1 (
    echo [Warning] Maven is not in PATH
    echo.
    echo Maven 3.8+ is required for installation.
    echo.
    echo Please install Maven:
    echo   - Download: https://maven.apache.org/download.cgi
    echo   - Windows: choco install maven
    echo.
    set /p CONTINUE="Continue anyway? (y/N): "
    if /i not "%CONTINUE%"=="y" (
        pause
        exit /b 1
    )
) else (
    for /f "tokens=3" %%a in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do set MAVEN_VERSION=%%a
    echo [Info] Maven version: %MAVEN_VERSION%
)

echo.
echo [Info] Starting graphical installer...
echo [Info] Please wait while the installer loads...
echo.

REM Try to run installer
call mvn compile exec:java -Dexec.mainClass="com.cashier.installer.Installer" -Dexec.classpathScope=compile 2>nul

if errorlevel 1 (
    echo.
    echo [Error] Installation failed
    echo.
    echo Possible causes:
    echo   - Java version too old (need JDK 17+)
    echo   - Maven not in PATH
    echo   - Network issues (dependencies download)
    echo.
    echo Please check the error messages above and try again.
    echo.
    pause
    exit /b 1
)

exit /b 0