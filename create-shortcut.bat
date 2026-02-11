@echo off
setlocal

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
del %DESKTOP_PATH%\%SHORTCUT_NAME%

:create_shortcut
echo [Create] Creating desktop shortcut...
echo [Info] Target: %TARGET_SCRIPT%
echo [Info] Working directory: %WORKING_DIR%
echo.

powershell -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%DESKTOP_PATH%\%SHORTCUT_NAME%'); $Shortcut.TargetPath = '%TARGET_SCRIPT%'; $Shortcut.WorkingDirectory = '%WORKING_DIR%'; $Shortcut.Description = '%APP_NAME% %APP_VERSION% - Complete Cashier System'; $Shortcut.Save()"

if errorlevel 1 goto :shortcut_failed
echo [Success] Desktop shortcut created!
echo.
echo Shortcut location: %DESKTOP_PATH%\%SHORTCUT_NAME%
echo.
goto :ask_startmenu

:shortcut_failed
echo [Error] Desktop shortcut creation failed
echo Please create shortcut manually
pause
exit /b 1

:ask_startmenu
echo [Ask] Create start menu shortcut?
set /p CREATE_STARTMENU="Create start menu shortcut? (y/N): "
if /i "%CREATE_STARTMENU%"=="y" goto :create_startmenu
goto :done

:create_startmenu
set STARTMENU_PATH=%APPDATA%\Microsoft\Windows\Start Menu\Programs\%APP_NAME%
if not exist %STARTMENU_PATH% mkdir %STARTMENU_PATH%

powershell -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%STARTMENU_PATH%\%SHORTCUT_NAME%'); $Shortcut.TargetPath = '%TARGET_SCRIPT%'; $Shortcut.WorkingDirectory = '%WORKING_DIR%'; $Shortcut.Description = '%APP_NAME% %APP_VERSION% - Complete Cashier System'; $Shortcut.Save()"

if errorlevel 1 goto :done
echo [Success] Start menu shortcut created!
echo Location: %STARTMENU_PATH%\%SHORTCUT_NAME%

:done
echo.
echo ========================================
echo [Done] Shortcut creation completed
echo ========================================
echo.
pause
exit /b 0