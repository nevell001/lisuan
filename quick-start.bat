@echo off
cd /d "%~dp0"

echo ============================================
echo   LiSuan - Quick Start
echo ============================================
echo.

REM Check if JAR exists
set "JAR_FILE=target\lisuan-fx-2.5.6-jar-with-dependencies.jar"

if exist "%JAR_FILE%" (
    echo [INFO] JAR found, starting...
    goto :run
)

echo [INFO] JAR not found, building...
echo.
mvn clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)

:run
echo.
echo [INFO] Starting LiSuan...
echo.

REM Set JavaFX path
set "JFX_BASE=%USERPROFILE%\.m2\repository\org\openjfx"
set "JFX_PATH=%JFX_BASE%\javafx-base\17.0.12;%JFX_BASE%\javafx-controls\17.0.12;%JFX_BASE%\javafx-fxml\17.0.12;%JFX_BASE%\javafx-graphics\17.0.12"

start javaw --module-path "%JFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.graphics -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -jar "%JAR_FILE%"

echo [INFO] Application started
pause
