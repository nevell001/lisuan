@echo off
REM ============================================
REM   LiSuan 打包向导启动脚本
REM   图形界面打包工具
REM ============================================

setlocal enabledelayedexpansion

echo ========================================
echo   启动打包向导...
echo ========================================
echo.

REM 检查 Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Java，请先安装 JDK 17+
    pause
    exit /b 1
)

REM 检查 Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] 未找到 Maven，打包功能可能无法使用
    echo.
    set /p "CONTINUE=是否继续? (Y/N): "
    if /i not "!CONTINUE!"=="y" (
        exit /b 1
    )
)

REM 设置 classpath
set "CLASSPATH=target\classes;target\dependency"

REM 使用 Maven 运行（开发模式）
echo [信息] 使用 Maven 运行打包向导...
call mvn javafx:run -Ppackager

if %errorlevel% neq 0 (
    echo.
    echo [错误] 启动失败
    pause
    exit /b 1
)

pause
exit /b 0
