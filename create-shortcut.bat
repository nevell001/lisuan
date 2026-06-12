@echo off
setlocal

echo ========================================
echo   Create Desktop Shortcut
echo ========================================
echo.

set APP_NAME=Cashier System
set APP_VERSION=v2.5.6
set SCRIPT_DIR=%~dp0
set SHORTCUT_NAME=%APP_NAME%.lnk
set WORKING_DIR=%SCRIPT_DIR%

REM 检查启动器优先级：Quick Start > Run CashierSystem > start.bat
if exist "%SCRIPT_DIR%Quick Start.bat" (
    set TARGET_SCRIPT=%SCRIPT_DIR%Quick Start.bat
    echo [Info] Using Quick Start launcher (recommended)
) else if exist "%SCRIPT_DIR%Run CashierSystem.bat" (
    set TARGET_SCRIPT=%SCRIPT_DIR%Run CashierSystem.bat
    echo [Info] Using Run CashierSystem launcher
) else if exist "%SCRIPT_DIR%start.bat" (
    set TARGET_SCRIPT=%SCRIPT_DIR%start.bat
    echo [Info] Using start.bat launcher
) else (
    goto :no_start
)

if not exist %TARGET_SCRIPT% goto :no_start
goto :check_shortcut

:no_start
echo [Error] start.bat not found!
echo Please ensure this script is in the same directory as start.bat
pause
exit /b 1

:check_shortcut
set DESKTOP_PATH=%USERPROFILE%\Desktop

if not exist %DESKTOP_PATH%\%SHORTCUT_NAME% goto :create_shortcut
echo [Warning] Shortcut already exists on desktop
set /p REPLY="Overwrite? (y/N): "
if /i "%REPLY%"=="y" goto :do_overwrite
echo [Cancel] Operation cancelled
pause
exit /b 0

:do_overwrite
del "%DESKTOP_PATH%\%SHORTCUT_NAME%" 2>nul

:create_shortcut
echo [Create] Creating desktop shortcut...
echo [Info] Target: %TARGET_SCRIPT%
echo [Info] Working directory: %WORKING_DIR%
echo.

REM 确保桌面目录存在
if not exist "%DESKTOP_PATH%" mkdir "%DESKTOP_PATH%"

REM 使用 PowerShell 创建快捷方式（使用转义的路径）
set "PS_SHORTCUT_PATH=%DESKTOP_PATH%\%SHORTCUT_NAME%"
set "PS_TARGET=%TARGET_SCRIPT%"
set "PS_WORKING=%WORKING_DIR%"
set "PS_DESC=%APP_NAME% - Complete Cashier System"

REM Delete existing shortcut if it exists
if exist "%DESKTOP_PATH%\%SHORTCUT_NAME%" del "%DESKTOP_PATH%\%SHORTCUT_NAME%" 2>nul

REM Create shortcut using PowerShell
powershell -NoProfile -ExecutionPolicy Bypass -Command "& {$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%PS_SHORTCUT_PATH%'); $Shortcut.TargetPath = '%PS_TARGET%'; $Shortcut.WorkingDirectory = '%PS_WORKING%'; $Shortcut.Description = '%PS_DESC%'; $Shortcut.Save(); if (Test-Path '%PS_SHORTCUT_PATH%') { Write-Host 'Success' } else { Write-Host 'Failed'; exit 1 }}"

if errorlevel 1 (
    echo [Error] Desktop shortcut creation failed
    echo Please create shortcut manually
    pause
    exit /b 1
)

REM Verify the shortcut was actually created
if exist "%DESKTOP_PATH%\%SHORTCUT_NAME%" (
    echo [Success] Desktop shortcut created!
    echo.
    echo Shortcut location: %DESKTOP_PATH%\%SHORTCUT_NAME%
    echo.
) else (
    echo [Error] Desktop shortcut file not found after creation
    echo Please create shortcut manually
    pause
    exit /b 1
)
goto :ask_startmenu

:ask_startmenu
echo [Ask] Create start menu shortcut?
set /p CREATE_STARTMENU="Create start menu shortcut? (y/N): "
if /i "%CREATE_STARTMENU%"=="y" goto :create_startmenu
goto :done

:create_startmenu
set STARTMENU_PATH=%APPDATA%\Microsoft\Windows\Start Menu\Programs\%APP_NAME%

REM 确保开始菜单目录存在
if not exist "%STARTMENU_PATH%" (
    echo [Create] Creating start menu directory: %STARTMENU_PATH%
    mkdir "%STARTMENU_PATH%"
)

REM Delete existing shortcut if it exists
if exist "%STARTMENU_PATH%\%SHORTCUT_NAME%" del "%STARTMENU_PATH%\%SHORTCUT_NAME%" 2>nul

REM 使用 PowerShell 创建快捷方式（使用转义的路径）
set "PS_SHORTCUT_PATH=%STARTMENU_PATH%\%SHORTCUT_NAME%"

powershell -NoProfile -ExecutionPolicy Bypass -Command "& {$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%PS_SHORTCUT_PATH%'); $Shortcut.TargetPath = '%PS_TARGET%'; $Shortcut.WorkingDirectory = '%PS_WORKING%'; $Shortcut.Description = '%PS_DESC%'; $Shortcut.Save(); if (Test-Path '%PS_SHORTCUT_PATH%') { Write-Host 'Success' } else { Write-Host 'Failed'; exit 1 }}"

if errorlevel 1 (
    echo [Warning] Start menu shortcut creation may have failed
)

REM Verify the shortcut was actually created
if exist "%STARTMENU_PATH%\%SHORTCUT_NAME%" (
    echo [Success] Start menu shortcut created!
    echo Location: %STARTMENU_PATH%\%SHORTCUT_NAME%
) else (
    echo [Warning] Start menu shortcut file not found after creation
)

:done
echo.
echo ========================================
echo [Done] Shortcut creation completed
echo ========================================
echo.
pause
exit /b 0