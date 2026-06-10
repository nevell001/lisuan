@echo off
REM ========================================
REM 收银系统 - jpackage 原生打包脚本
REM ========================================

setlocal enabledelayedexpansion

echo ========================================
echo   收银系统 - jpackage 原生打包
echo ========================================
echo.

REM 从 pom.xml 读取版本号
for /f "tokens=3 delims=<>" %%a in ('findstr /R "<version>" pom.xml ^| findstr /V "javafx\|maven\|java\|mysql\|hikaricp\|poi\|pdfbox\|controlsfx\|fontawesomefx\|junit\|testfx\|h2\|bcrypt\|logback\|jackson\|javalin\|slf4j\|plugin"') do (
    set "APP_VERSION=%%a"
    goto :version_found
)
:version_found
if "%APP_VERSION%"=="" set "APP_VERSION=2.5.5"

echo [INFO] 版本: %APP_VERSION%
echo.

REM 检查 JAR 文件
set "FAT_JAR=cashier-system-fx-%APP_VERSION%-jar-with-dependencies.jar"
if not exist "target\%FAT_JAR%" (
    echo [ERROR] 未找到 JAR 文件: target\%FAT_JAR%
    echo.
    echo 请先运行: mvn clean package
    pause
    exit /b 1
)

REM 检查 jpackage 命令
where jpackage >/dev/null 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 jpackage 命令
    echo.
    echo jpackage 是 JDK 14+ 自带的工具。
    echo 请确保:
    echo   1. 已安装 JDK 14 或更高版本
    echo   2. JDK 的 bin 目录在 PATH 中
    echo.
    echo 检查 JDK:
    where java
    java -version
    echo.
    pause
    exit /b 1
)

REM 清理旧的打包文件
echo [1/3] 清理旧的打包文件...
if exist "target\dist" rmdir /S /Q "target\dist" 2>/dev/null
mkdir "target\dist"
echo [OK] 清理完成
echo.

REM 执行 jpackage
echo [2/3] 开始打包...
echo.

jpackage ^
    --type exe ^
    --name "CashierSystem" ^
    --app-version "%APP_VERSION%" ^
    --vendor "Cashier System" ^
    --description "现代化收银系统 - 库存管理、会员管理、交易管理" ^
    --dest "target\dist" ^
    --input "target" ^
    --main-jar "%FAT_JAR%" ^
    --main-class "com.cashier.CashierSystemFXApplication" ^
    --java-options "-Xms512m" ^
    --java-options "-Xmx1024m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --win-menu ^
    --win-menu-group "Cashier System" ^
    --win-shortcut ^
    --win-dir-chooser ^
    --win-per-user-install false ^
    --icon "src\main\resources\images\logos\app-icon.png"

if errorlevel 1 (
    echo.
    echo ========================================
    echo   [ERROR] 打包失败！
    echo ========================================
    echo.
    pause
    exit /b 1
)

echo.
echo [3/3] 打包完成！
echo.

echo ========================================
echo   [SUCCESS] 原生安装包已创建
echo ========================================
echo.
echo 安装包位置: target\dist\CashierSystem-%APP_VERSION%.exe
echo.
echo 安装后特性:
echo   - 桌面快捷方式
echo   - 开始菜单项
echo   - 无需命令行启动
echo   - 无控制台窗口
echo.

REM 打开输出目录
explorer "target\dist"

pause
exit /b 0
