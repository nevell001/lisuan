@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"

echo ========================================
echo Cashier System Starting...
echo ========================================
echo.

echo Checking compile status...
if not exist "%SCRIPT_DIR%CashierSystemGUI.class" (
    echo No compiled files found, compiling...
    echo.
    call "%SCRIPT_DIR%compile_with_flatlaf.bat"

    if errorlevel 1 (
        echo.
        echo Compile failed, please check error messages
        pause
        exit /b 1
    )

    echo.
    echo Compile completed
    echo.
) else (
    echo Compiled files detected
    echo.
)

echo Starting Cashier System...
java -cp flatlaf-3.5.jar;. CashierSystemGUI

pause