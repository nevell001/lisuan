@echo off
REM 中文字体下载脚本（Windows）
REM 下载开源中文字体到本地 fonts 目录

setlocal enabledelayedexpansion

set "FONTS_DIR=%~dp0fonts"

echo ========================================
echo 收银系统 - 中文字体下载脚本
echo ========================================
echo.

REM 创建字体目录
if not exist "%FONTS_DIR%" mkdir "%FONTS_DIR%"
echo 字体目录: %FONTS_DIR%
echo.

REM 检查是否已有字体
if exist "%FONTS_DIR%\*.ttf" (
    echo 检测到已下载的字体文件
    echo.
    dir /b "%FONTS_DIR%\*.ttf" 2>nul
    echo.
    set /p redownload=是否重新下载字体？(y/N):
    if /i not "!redownload!"=="y" (
        echo 已取消下载。
        exit /b 0
    )
    echo.
)

echo 开始下载中文字体...
echo.

REM 使用 PowerShell 下载字体
echo 正在下载中文字体...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/adobe-fonts/source-han-sans/raw/release/SubsetOTF/CN/SourceHanSansCN-Regular.otf' -OutFile '%FONTS_DIR%\SourceHanSansCN-Regular.otf' -TimeoutSec 30}" 2>nul

if errorlevel 1 (
    echo.
    echo 使用备用下载源...
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://fonts.gstatic.com/s/notosanssc/v36/k3kXo84MPvpLmixcA63oeALZTYKLgASIOQ.woff2' -OutFile '%FONTS_DIR%\NotoSansSC-Regular.woff2' -TimeoutSec 30}" 2>nul
)

echo.
echo ========================================
echo 下载完成！
echo ========================================
echo.

if exist "%FONTS_DIR%\*.otf" (
    echo 字体文件已保存到: %FONTS_DIR%
    echo.
    echo 已下载的字体：
    dir /b "%FONTS_DIR%\*.otf" "%FONTS_DIR%\*.woff2" 2>nul
    echo.
    echo 现在可以运行收银系统了：
    echo   run_with_flatlaf.bat
) else if exist "%FONTS_DIR%\*.woff2" (
    echo 字体文件已保存到: %FONTS_DIR%
    echo.
    echo 已下载的字体：
    dir /b "%FONTS_DIR%\*.otf" "%FONTS_DIR%\*.woff2" 2>nul
    echo.
    echo 现在可以运行收银系统了：
    echo   run_with_flatlaf.bat
) else (
    echo 字体下载失败
    echo.
    echo 请手动下载中文字体并放到 %FONTS_DIR% 目录
    echo 推荐字体：
    echo   - 文泉驿微米黑 (WQY Microhei)
    echo   - 思源黑体 (Source Han Sans)
    echo   - Noto Sans SC
)

pause