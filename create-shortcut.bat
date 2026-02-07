@echo off
chcp 65001 >nul 2>&1
REM ============================================
REM Create Desktop Shortcut Script
REM ============================================

setlocal enabledelayedexpansion

echo ========================================
echo   Create Desktop Shortcut
echo ========================================
echo.

set APP_NAME=Cashier System
set APP_VERSION=v2.2.1
set SCRIPT_DIR=%~dp0
set SHORTCUT_NAME=%APP_NAME%.lnk
set TARGET_SCRIPT=%SCRIPT_DIR%start.bat
set WORKING_DIR=%SCRIPT_DIR%

REM Check if start.bat exists
if not exist "%TARGET_SCRIPT%" (
    echo [Error] start.bat not found!
    echo Please ensure this script is in the same directory as start.bat
    pause
    exit /b 1
)

REM Get desktop path
for /f "tokens=2 delims==" %%A in ('wmic OS Get LocalDateTime /value') do set "dt=%%A"
set "YY=%dt:~2,2%" & set "YYYY=%dt:~0,4%" & set "MM=%dt:~4,2%" & set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" & set "Min=%dt:~10,2%" & set "SS=%dt:~12,2%"
set "timestamp=%YYYY%-%MM%-%DD%_%HH%-%Min%-%SS%"

REM Get desktop path
set DESKTOP_PATH=%USERPROFILE%\Desktop

REM Check if shortcut already exists
if exist "%DESKTOP_PATH%\%SHORTCUT_NAME%" (
    echo [Warning] Shortcut already exists on desktop
    set /p REPLY="Overwrite? (y/N): "
    if not /i "!REPLY!"=="y" (
        echo [Cancel] Operation cancelled
        pause
        exit /b 0
    )
    del "%DESKTOP_PATH%\%SHORTCUT_NAME%"
)

REM Create shortcut using PowerShell
echo [Create] Creating desktop shortcut...
echo [Info] Target: %TARGET_SCRIPT%
echo [Info] Working directory: %WORKING_DIR%
echo.

powershell -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%DESKTOP_PATH%\%SHORTCUT_NAME%'); $Shortcut.TargetPath = '%TARGET_SCRIPT%'; $Shortcut.WorkingDirectory = '%WORKING_DIR%'; $Shortcut.Description = '%APP_NAME% %APP_VERSION% - Complete Cashier System'; $Shortcut.Save()"

if %errorlevel% equ 0 (
    echo [Success] Desktop shortcut created!
    echo.
    echo Shortcut location: %DESKTOP_PATH%\%SHORTCUT_NAME%
    echo.
) else (
    echo [Error] Desktop shortcut creation failed
    echo Please create shortcut manually
    pause
    exit /b 1
)

REM Ask if create start menu shortcut
echo [Ask] Create start menu shortcut?
set /p CREATE_STARTMENU="Create start menu shortcut? (y/N): "

if /i "!CREATE_STARTMENU!"=="y" (
    set STARTMENU_PATH=%APPDATA%\Microsoft\Windows\Start Menu\Programs\%APP_NAME%
    if not exist "%STARTMENU_PATH%" mkdir "%STARTMENU_PATH%"

    powershell -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%STARTMENU_PATH%\%SHORTCUT_NAME%'); $Shortcut.TargetPath = '%TARGET_SCRIPT%'; $Shortcut.WorkingDirectory = '%WORKING_DIR%'; $Shortcut.Description = '%APP_NAME% %APP_VERSION% - Complete Cashier System'; $Shortcut.Save()"

    if %errorlevel% equ 0 (
        echo [Success] Start menu shortcut created!
        echo Location: %STARTMENU_PATH%\%SHORTCUT_NAME%
    )
)

echo.
echo ========================================
echo [Done] Shortcut creation completed
echo ========================================
echo.
pause
exit /b 0