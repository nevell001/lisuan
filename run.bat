@echo off
REM ============================================
REM 收银系统 Windows 启动脚本
REM ============================================

echo.
echo =========================================
echo   Cashier System Windows 启动脚本
echo =========================================
echo.

REM 检查 Java 是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Java，请先安装 Java 17 或更高版本
    echo.
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo [信息] Java 版本检查通过
echo.

REM 使用 Maven 运行（推荐）
echo [信息] 正在启动收银系统...
echo.

cd /d "%~dp0"

REM 方式 1: 使用 Maven 插件运行（最稳定）
call mvn clean javafx:run

if %errorlevel% neq 0 (
    echo.
    echo [警告] Maven 方式运行失败，尝试备用方式...
    echo.

    REM 方式 2: 使用 Maven 打包后运行
    call mvn clean package

    if %errorlevel% equ 0 (
        echo.
        echo [信息] 正在启动已打包的应用...
        echo.
        java --module-path "target/cashier-system-image" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media -cp target/classes com.cashier.CashierSystemFXApplication
    )
)

echo.
echo =========================================
echo   应用程序已关闭
echo =========================================
echo.
pause
