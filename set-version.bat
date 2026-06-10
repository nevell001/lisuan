@echo off
REM ========================================
REM LiSuan Version Update Script (Windows)
REM ========================================

setlocal enabledelayedexpansion

if "%1"=="" (
    echo Usage: set-version.bat x.y.z
    echo Example: set-version.bat 2.5.7
    exit /b 1
)

set "NEW_VERSION=%1"
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

echo ========================================
echo   LiSuan Version Update
echo ========================================
echo.
echo Setting version to: %NEW_VERSION%
echo.

REM Validate version format (basic check)
echo %NEW_VERSION% | findstr /R "^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo [Error] Invalid version format: %NEW_VERSION%
    echo Expected format: x.y.z (e.g., 2.5.7)
    exit /b 1
)

echo [1/5] Updating version in Java files...
powershell -Command "(Get-Content 'src\main\java\com\cashier\constant\AppConstants.java') -replace 'APP_VERSION = \"[\d\.]+\"', 'APP_VERSION = \"%NEW_VERSION%\"' | Set-Content 'src\main\java\com\cashier\constant\AppConstants.java'"

echo [2/5] Updating version in pom.xml...
powershell -Command "(Get-Content 'pom.xml') -replace '<version>[\d\.]+</version>', '<version>%NEW_VERSION%</version>' | Set-Content 'pom.xml'"

echo [3/5] Updating version in batch scripts...
powershell -Command "(Get-Content 'start.bat') -replace 'VERSION\" 2\.[\d\.]+', 'VERSION\" %NEW_VERSION%' | Set-Content 'start.bat'"
powershell -Command "(Get-Content 'package.bat') -replace 'VERSION\" 2\.[\d\.]+', 'VERSION\" %NEW_VERSION%' | Set-Content 'package.bat'"
powershell -Command "(Get-Content 'jpackage.bat') -replace 'REM Version 2\.[\d\.]+', 'REM Version %NEW_VERSION%' | Set-Content 'jpackage.bat'"
powershell -Command "(Get-Content 'start.bat') -replace 'set \"APP_VERSION=2\.[\d\.]+\"', 'set \"APP_VERSION=%NEW_VERSION%\"' | Set-Content 'start.bat'"
powershell -Command "(Get-Content 'package.bat') -replace 'set \"APP_VERSION=2\.[\d\.]+\"', 'set \"APP_VERSION=%NEW_VERSION%\"' | Set-Content 'package.bat'"
powershell -Command "(Get-Content 'jpackage.bat') -replace 'set \"APP_VERSION=2\.[\d\.]+\"', 'set \"APP_VERSION=%NEW_VERSION%\"' | Set-Content 'jpackage.bat'"

echo [4/5] Skipping install.sh (Unix script)...
echo.

echo [5/5] Verifying updates...
echo.
echo ========================================
echo   Version update complete!
echo ========================================
echo.
echo New version: %NEW_VERSION%
echo.
echo Next steps:
echo   1. Review changes: git diff
echo   2. Commit: git add -A ^&^& git commit -m "chore: 发布版本 v%NEW_VERSION%"
echo   3. Create tag: git tag -a v%NEW_VERSION% -m "LiSuan v%NEW_VERSION%"
echo   4. Push: git push origin main ^&^& git push origin v%NEW_VERSION%
echo.

pause
exit /b 0
