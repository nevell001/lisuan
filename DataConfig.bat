@echo off
setlocal

cd /d "%~dp0"

echo =========================================
echo   LiSuan Database Configuration
echo =========================================
echo.

echo [INFO] Launching database configuration tool...
echo.

REM Find MySQL JDBC driver
set "MYSQL_JAR="
if exist "%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar" (
    set "MYSQL_JAR=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar"
)
if exist "%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" (
    set "MYSQL_JAR=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar"
)
if exist "%USERPROFILE%\.m2\repository\mysql\mysql-connector-java\8.0.33\mysql-connector-java-8.0.33.jar" (
    set "MYSQL_JAR=%USERPROFILE%\.m2\repository\mysql\mysql-connector-java\8.0.33\mysql-connector-java-8.0.33.jar"
)

if "%MYSQL_JAR%"=="" (
    echo [ERROR] MySQL JDBC driver not found in Maven repository!
    echo.
    echo Please run: mvn clean package
    pause
    exit /b 1
)

echo [INFO] Found MySQL JDBC driver
echo [INFO] Starting configuration tool...
echo.

java -cp "target/classes;%MYSQL_JAR%" com.cashier.installer.DatabaseConfigDialog

if errorlevel 1 (
    echo.
    echo [ERROR] Configuration tool failed
    echo.
    echo Make sure the project is built: mvn clean package
    pause
    exit /b 1
)

echo.
echo [INFO] Configuration complete!
echo You can now run start.bat to launch the application.
echo.
pause
