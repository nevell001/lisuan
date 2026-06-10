@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   Cashier System - Distribution Package
echo ========================================
echo.

for /f "tokens=3 delims=<>" %%a in ('findstr /R "<version>" pom.xml ^| findstr /V "javafx\|maven\|java\|mysql\|hikaricp\|poi\|pdfbox\|controlsfx\|fontawesomefx\|junit\|testfx\|h2\|bcrypt\|logback\|jackson\|javalin\|slf4j\|plugin"') do (
    set "APP_VERSION=%%a"
    goto :version_found
)
:version_found

set "APP_VERSION_NUM=%APP_VERSION:v=%"
set APP_NAME=CashierSystem
set FAT_JAR=lisuan-fx-%APP_VERSION_NUM%-jar-with-dependencies.jar

echo [Info] Version: %APP_VERSION_NUM%
echo.

if not exist "target\%FAT_JAR%" (
    echo [INFO] JAR not found, starting compilation...
    echo.
    echo Compiling with Maven, this may take a while...
    echo.
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo.
        echo [ERROR] Compilation failed
        goto :error_exit
    )
    echo.
    echo [OK] Compilation successful
    echo.
)

echo [1/5] Creating distribution...
if exist "dist\%APP_NAME%" rmdir /S /Q "dist\%APP_NAME%"
mkdir "dist\%APP_NAME%"
mkdir "dist\%APP_NAME%\config"
mkdir "dist\%APP_NAME%\data"
mkdir "dist\%APP_NAME%\logs"
mkdir "dist\%APP_NAME%\lib"

copy "target\%FAT_JAR%" "dist\%APP_NAME%\" >nul
if exist "README.md" copy "README.md" "dist\%APP_NAME%\" >nul
if exist "config\database.properties" copy "config\database.properties" "dist\%APP_NAME%\config\" >nul
if exist "config\database.properties.example" copy "config\database.properties.example" "dist\%APP_NAME%\config\" >nul
if exist "config\jvm.config.example" copy "config\jvm.config.example" "dist\%APP_NAME%\config\" >nul
copy "start.bat" "dist\%APP_NAME%\" >nul

echo [2/5] Including GUI database config tool...
xcopy /E /I /Y "target\classes\com" "dist\%APP_NAME%\lib\com\" >nul 2>&1

echo [OK] Distribution created with GUI config tool
echo.

echo [3/5] Creating launchers...

echo @echo off > "dist\%APP_NAME%\Run %APP_NAME%.bat"
echo cd /d "%%~dp0" >> "dist\%APP_NAME%\Run %APP_NAME%.bat"
echo echo Starting Cashier System... >> "dist\%APP_NAME%\Run %APP_NAME%.bat"
echo echo. >> "dist\%APP_NAME%\Run %APP_NAME%.bat"
echo call start.bat >> "dist\%APP_NAME%\Run %APP_NAME%.bat"

echo @echo off > "dist\%APP_NAME%\Quick Start.bat"
echo cd /d "%%~dp0" >> "dist\%APP_NAME%\Quick Start.bat"
echo if not exist "config\database.properties" ( >> "dist\%APP_NAME%\Quick Start.bat"
echo     echo Database config not found. Run "Run %APP_NAME%.bat" first. >> "dist\%APP_NAME%\Quick Start.bat"
echo     pause >> "dist\%APP_NAME%\Quick Start.bat"
echo     exit /b 1 >> "dist\%APP_NAME%\Quick Start.bat"
echo ) >> "dist\%APP_NAME%\Quick Start.bat"
echo set "M2_REPO=%%USERPROFILE%%\.m2\repository" >> "dist\%APP_NAME%\Quick Start.bat"
echo set "MOD_PATH=%%M2_REPO%%\org\openjfx\javafx-base\17.0.12;%%M2_REPO%%\org\openjfx\javafx-controls\17.0.12;%%M2_REPO%%\org\openjfx\javafx-fxml\17.0.12;%%M2_REPO%%\org\openjfx\javafx-graphics\17.0.12" >> "dist\%APP_NAME%\Quick Start.bat"
echo start "" javaw -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 --module-path "%%MOD_PATH%%" --add-modules javafx.controls,javafx.fxml -jar "%FAT_JAR%" >> "dist\%APP_NAME%\Quick Start.bat"

echo [OK] Launchers created
echo.

echo [4/5] Creating GUI Config launcher...
echo @echo off > "dist\%APP_NAME%\Database Config.bat"
echo cd /d "%%~dp0" >> "dist\%APP_NAME%\Database Config.bat"
echo echo ======================================== >> "dist\%APP_NAME%\Database Config.bat"
echo echo   Database Configuration Tool >> "dist\%APP_NAME%\Database Config.bat"
echo echo ======================================== >> "dist\%APP_NAME%\Database Config.bat"
echo echo. >> "dist\%APP_NAME%\Database Config.bat"
echo where java ^>nul 2^>^&1 >> "dist\%APP_NAME%\Database Config.bat"
echo if errorlevel 1 ( >> "dist\%APP_NAME%\Database Config.bat"
echo     echo [ERROR] Java is not installed or not in PATH >> "dist\%APP_NAME%\Database Config.bat"
echo     pause >> "dist\%APP_NAME%\Database Config.bat"
echo     exit /b 1 >> "dist\%APP_NAME%\Database Config.bat"
echo ^) >> "dist\%APP_NAME%\Database Config.bat"
echo for %%%%f in ^(lisuan-fx-*-jar-with-dependencies.jar^) do set "CLASSPATH=lib;%%%%f" >> "dist\%APP_NAME%\Database Config.bat"
echo echo Starting Database Configuration GUI... >> "dist\%APP_NAME%\Database Config.bat"
echo java -cp "%%CLASSPATH%%" com.cashier.installer.DatabaseConfigDialog >> "dist\%APP_NAME%\Database Config.bat"

echo [OK] GUI Config launcher created
echo.

echo [5/5] Creating ZIP archive...
if exist "dist\%APP_NAME%-v%APP_VERSION_NUM%.zip" del "dist\%APP_NAME%-v%APP_VERSION_NUM%.zip"
powershell -Command "Compress-Archive -Path 'dist\%APP_NAME%' -DestinationPath 'dist\%APP_NAME%-v%APP_VERSION_NUM%.zip' -Force"
echo [OK] ZIP created
echo.

echo ========================================
echo [SUCCESS] Package Ready!
echo ========================================
echo.
echo Distribution: dist\%APP_NAME%\
echo ZIP: dist\%APP_NAME%-v%APP_VERSION_NUM%.zip
echo.
echo Launch options:
echo   1. "Database Config.bat" - GUI for database configuration (RECOMMENDED FIRST)
echo   2. "Run %APP_NAME%.bat" - Full setup with command-line database config
echo   3. "Quick Start.bat" - Quick launch (requires existing config)
echo.
echo The package includes:
echo   - Application JAR
echo   - GUI Database Configuration Tool
echo   - start.bat (with database configuration logic)
echo   - config folder with examples
echo.
pause
exit /b 0

:error_exit
echo.
echo [FAILED] Packaging failed!
echo.
pause
exit /b 1
