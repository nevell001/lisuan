@echo off
REM ============================================
REM 收银系统 MySQL Docker 快速启动脚本 (Windows)
REM ============================================

setlocal enabledelayedexpansion

echo ========================================
echo   收银系统 MySQL Docker 启动脚本
echo ========================================
echo.

REM 检查 Docker 是否运行
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker 未运行！
    echo 请先启动 Docker Desktop
    pause
    exit /b 1
)

REM 创建必要的目录
echo [1/4] 创建必要的目录...
if not exist "docker\mysql-init" mkdir "docker\mysql-init"
if not exist "docker\mysql-backup" mkdir "docker\mysql-backup"
if not exist "config" mkdir "config"
echo [完成] 目录创建完成
echo.

REM 检查配置文件
if not exist "config\database.properties" (
    echo [2/4] 创建数据库配置文件...
    copy "config\database.properties.example" "config\database.properties" >nul
    echo [完成] 配置文件已创建: config\database.properties
    echo [提示] 请编辑此文件并修改数据库连接信息
) else (
    echo [2/4] 配置文件已存在
)
echo.

REM 检查是否已存在容器
docker ps -a | findstr "cashier-mysql" >nul
if %errorlevel% equ 0 (
    echo [3/4] 检测到已存在的 MySQL 容器
    set /p REPLY="是否删除旧容器并重新创建？(y/N): "
    if /i "!REPLY!"=="y" (
        echo [停止] 停止并删除旧容器...
        docker-compose down -v
        echo [完成] 旧容器已删除
    ) else (
        echo [启动] 启动现有容器...
        docker-compose start
        echo [完成] 容器已启动
        goto :success
    )
)

REM 启动 MySQL 容器
echo [3/4] 启动 MySQL 容器...
docker-compose up -d

REM 等待 MySQL 启动
echo [等待] 等待 MySQL 启动...
timeout /t 15 /nobreak >nul

REM 检查容器状态
docker ps | findstr "cashier-mysql" >nul
if %errorlevel% equ 0 (
    goto :success
) else (
    goto :failure
)

:success
echo.
echo ========================================
echo [成功] MySQL 启动成功！
echo ========================================
echo.
echo 数据库连接信息：
echo   主机: localhost
echo   端口: 3306
echo   数据库: cashier_system
echo   用户名: cashier
echo   密码: YourStrongPassword123!
echo.
echo 管理工具：
echo   phpMyAdmin: http://localhost:8080
echo.
echo 常用命令：
echo   查看日志: docker-compose logs -f mysql
echo   停止容器: docker-compose stop
echo   重启容器: docker-compose restart
echo   进入容器: docker exec -it cashier-mysql bash
echo.
echo 下一步：
echo   1. 修改 config\database.properties
echo   2. 启动收银系统应用
echo   3. 应用会自动迁移数据到 MySQL
echo.
pause
exit /b 0

:failure
echo.
echo ========================================
echo [失败] MySQL 启动失败！
echo ========================================
echo.
echo 查看错误日志：
echo   docker-compose logs mysql
echo.
pause
exit /b 1
