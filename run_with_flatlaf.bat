@echo off
REM 收银系统启动脚本（Windows）
REM 包含编译检查、本地中文字体检测和加载功能

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "FONTS_DIR=%SCRIPT_DIR%fonts"

echo ========================================
echo 收银系统启动中...
echo ========================================
echo.

REM 检查是否已编译
echo 检查编译状态...
if not exist "%SCRIPT_DIR%CashierSystemGUI.class" (
    echo 未检测到编译文件，开始编译...
    echo.
    call "%SCRIPT_DIR%compile_with_flatlaf.bat"

    if errorlevel 1 (
        echo.
        echo 编译失败，请检查错误信息
        pause
        exit /b 1
    )

    echo.
    echo 编译完成
    echo.
) else (
    echo 检测到编译文件
    echo.
)

REM 检测本地字体目录
set "USE_LOCAL_FONT=0"
if exist "%FONTS_DIR%" (
    dir /b "%FONTS_DIR%\*.ttf" "%FONTS_DIR%\*.ttc" "%FONTS_DIR%\*.otf" "%FONTS_DIR%\*.woff2" >nul 2>&1
    if not errorlevel 1 (
        set "USE_LOCAL_FONT=1"
        echo 检测到本地字体文件
        echo   字体目录: %FONTS_DIR%
        echo.
    )
)

REM 如果没有本地字体，提示下载
if "%USE_LOCAL_FONT%"=="0" (
    echo 提示：未检测到本地字体文件
    echo.
    echo 推荐方案：
    echo   1. 下载本地字体（推荐）：download_fonts.bat
    echo   2. 确保系统已安装中文字体
    echo.
    set /p download_font=是否现在下载本地字体？(Y/n):

    if /i not "!download_font!"=="n" (
        echo.
        call "%SCRIPT_DIR%download_fonts.bat"
        if not errorlevel 1 (
            dir /b "%FONTS_DIR%\*.ttf" "%FONTS_DIR%\*.ttc" "%FONTS_DIR%\*.otf" "%FONTS_DIR%\*.woff2" >nul 2>&1
            if not errorlevel 1 (
                set "USE_LOCAL_FONT=1"
            )
        )
    )
    echo.
)

REM 启动收银系统
echo 启动收银系统...

REM 设置 Java 字体参数
if "%USE_LOCAL_FONT%"=="1" (
    java -Djava.awt.fonts="%FONTS_DIR%" -cp flatlaf-3.5.jar;. CashierSystemGUI
) else (
    java -cp flatlaf-3.5.jar;. CashierSystemGUI
)

pause