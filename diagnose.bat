@echo off
setlocal enabledelayedexpansion

echo ============================================
echo   LiSuan Start Script Diagnostic
echo ============================================
echo.

echo [1] Current directory before cd:
cd
echo.

echo [2] Script location (%~dp0):
echo %~dp0
echo.

echo [3] Changing to script directory...
cd /d "%~dp0"
echo.

echo [4] Current directory after cd:
cd
echo.

echo [5] Checking for pom.xml:
if exist "pom.xml" (
    echo [OK] pom.xml EXISTS
    echo.
    echo [6] Attempting to read version:
    for /f "tokens=2 delims=<>" %%a in ('findstr /R "<version>" pom.xml 2^>nul ^| findstr /V "javafx maven java mysql hikaricp poi pdfbox controlsfx fontawesomefx junit testfx h2 bcrypt logback slf4j" 2^>nul') do (
        set "APP_VERSION=%%a"
        echo [FOUND] Version: %%a
        goto :version_found
    )
    :version_found
    if "!APP_VERSION!"=="" (
        echo [WARN] Version not found, using default
        set "APP_VERSION=2.5.4"
    )
    echo [FINAL] APP_VERSION: !APP_VERSION!
    echo.
    echo [7] Expected JAR file:
    set "JAR_FILE=target\lisuan-fx-!APP_VERSION!-jar-with-dependencies.jar"
    echo !JAR_FILE!
    echo.
    echo [8] Checking if JAR exists:
    if exist "!JAR_FILE!" (
        echo [OK] JAR file exists
    ) else (
        echo [ERROR] JAR file NOT found
        echo.
        echo Target directory contents:
        dir /b target\*.jar 2>nul || echo     No JAR files in target\
    )
) else (
    echo [ERROR] pom.xml NOT FOUND - Will enter Package Mode
    echo.
    echo [9] Checking for JAR in current directory:
    dir /b lisuan-fx-*-jar-with-dependencies.jar 2>nul || echo     No JAR files found
)

echo.
echo ============================================
pause
