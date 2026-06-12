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

echo [1/7] Updating version in Java files...
powershell -Command "(Get-Content 'src\main\java\com\cashier\constant\AppConstants.java') -replace 'APP_VERSION = \"[\d\.]+\"', 'APP_VERSION = \"%NEW_VERSION%\"' | Set-Content 'src\main\java\com\cashier\constant\AppConstants.java'"

echo [2/7] Updating version in pom.xml...
powershell -Command "(Get-Content 'pom.xml') -replace '<version>[\d\.]+</version>', '<version>%NEW_VERSION%</version>' | Set-Content 'pom.xml'"

echo [3/7] Updating version in batch scripts...
for %%F in (start.bat quick-start.bat install.bat) do (
    if exist "%%F" (
        powershell -Command "(Get-Content '%%F') -replace 'set \"APP_VERSION=[\d\.]+\"', 'set \"APP_VERSION=%NEW_VERSION%\"' | Set-Content '%%F'"
        powershell -Command "(Get-Content '%%F') -replace 'Version [\d\.]+', 'Version %NEW_VERSION%' | Set-Content '%%F'"
    )
)

echo [4/7] Updating version in diagnose.bat and create-shortcut.bat...
if exist "diagnose.bat" (
    powershell -Command "(Get-Content 'diagnose.bat') -replace 'set \"APP_VERSION=[\d\.]+\"', 'set \"APP_VERSION=%NEW_VERSION%\"' | Set-Content 'diagnose.bat'"
    powershell -Command "(Get-Content 'diagnose.bat') -replace 'APP_VERSION=\"[\d\.]+\"', 'APP_VERSION=\"%NEW_VERSION%\"' | Set-Content 'diagnose.bat'"
)
if exist "create-shortcut.bat" (
    powershell -Command "(Get-Content 'create-shortcut.bat') -replace 'APP_VERSION=v[\d\.]+', 'APP_VERSION=v%NEW_VERSION%' | Set-Content 'create-shortcut.bat'"
)

echo [5/7] Updating version in PowerShell scripts...
for %%F in (run-app.ps1 package-simple.ps1) do (
    if exist "%%F" (
        powershell -Command "(Get-Content '%%F') -replace '\$APP_VERSION = \"[\d\.]+\"', '```$APP_VERSION = \"%NEW_VERSION%\"' | Set-Content '%%F'"
    )
)

echo [6/7] Updating version in shell scripts...
if exist "start.sh" (
    powershell -Command "(Get-Content 'start.sh') -replace 'APP_VERSION=\"[\d\.]+\"', 'APP_VERSION=\"%NEW_VERSION%\"' | Set-Content 'start.sh'"
)
if exist "install.sh" (
    powershell -Command "(Get-Content 'install.sh') -replace 'APP_VERSION:-\"[\d\.]+\"', 'APP_VERSION:-\"%NEW_VERSION%\"' | Set-Content 'install.sh'"
)
if exist ".env.example" (
    powershell -Command "(Get-Content '.env.example') -replace 'APP_VERSION=[\d\.]+', 'APP_VERSION=%NEW_VERSION%' | Set-Content '.env.example'"
)

echo [7/7] Verifying updates...
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
