# LiSuan Simple Package Script
# PowerShell version

$ErrorActionPreference = "Stop"

$APP_NAME = "LiSuan"
$APP_VERSION = "2.5.6"
$OUTPUT_DIR = "target\dist"

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host "  $APP_NAME Packaging"  -ForegroundColor Cyan
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host ""

# Build JAR
Write-Host "Building JAR..." -ForegroundColor Yellow
& mvn "clean", "package", "-DskipTests", "-q"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Build failed" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Create output directory
$APP_DIR = "$OUTPUT_DIR\$APP_NAME"
if (Test-Path $APP_DIR) {
    Remove-Item -Recurse -Force $APP_DIR
}
New-Item -ItemType Directory -Path "$APP_DIR\app" -Force | Out-Null
New-Item -ItemType Directory -Path "$APP_DIR\config" -Force | Out-Null

# Copy JAR
Write-Host "Copying files..." -ForegroundColor Yellow
Copy-Item "target\lisuan-fx-$APP_VERSION-jar-with-dependencies.jar" "$APP_DIR\app\" -Force

# Copy config
if (Test-Path "config\database.properties") {
    Copy-Item "config\database.properties" "$APP_DIR\config\" -Force
}

# Create startup scripts
Write-Host "Creating startup scripts..." -ForegroundColor Yellow

# Batch launcher - 使用完整的 JAR 文件名避免变量展开问题
$BAT_CONTENT = @"
@echo off
setlocal
set APP_HOME=%~dp0
set JAR_FILE=%APP_HOME%app\lisuan-fx-$APP_VERSION-jar-with-dependencies.jar
set JFX_BASE=%USERPROFILE%\.m2\repository\org\openjfx
set JFX_PATH=%JFX_BASE%\javafx-base\17.0.12;%JFX_BASE%\javafx-controls\17.0.12;%JFX_BASE%\javafx-fxml\17.0.12;%JFX_BASE%\javafx-graphics\17.0.12
echo Starting $APP_NAME...
java --module-path "%JFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.graphics -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -jar "%JAR_FILE%" %*
endlocal
if errorlevel 1 pause
"@

$BAT_CONTENT | Out-File "$APP_DIR\$APP_NAME.bat" -Encoding ASCII

# PowerShell launcher
$PS_CONTENT = @"
# `$APP_NAME Startup Script
`$APP_NAME = "$APP_NAME"
`$APP_VERSION = "$APP_VERSION"
`$JFX_BASE = "`$env:USERPROFILE\.m2\repository\org\openjfx"
`$JFX_PATH = "`$JFX_BASE\javafx-base\17.0.12;`$JFX_BASE\javafx-controls\17.0.12;`$JFX_BASE\javafx-fxml\17.0.12;`$JFX_BASE\javafx-graphics\17.0.12"
`$JAR_FILE = "`$PSScriptRoot\app\lisuan-fx-`$APP_VERSION-jar-with-dependencies.jar"

java --module-path "`$JFX_PATH" --add-modules javafx.controls,javafx.fxml,javafx.graphics -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -jar "`$JAR_FILE" `$args
if (`$LASTEXITCODE -ne 0) { Read-Host "Press Enter to exit" }
"@

$PS_CONTENT | Out-File "$APP_DIR\$APP_NAME.ps1" -Encoding ASCII

# README
$README = @"
$APP_NAME v$APP_VERSION - 狸算收银系统

启动方法:
  - 双击运行 $APP_NAME.bat (推荐)
  - 右键运行 $APP_NAME.ps1 (如果 .bat 不工作)

系统要求:
  - Java 17 或更高版本
  - JavaFX 17.0.12 (自动从 Maven 本地仓库加载)

如果启动失败，请确保:
  1. 已安装 Java 17+
  2. 已执行 mvn install 安装 JavaFX 依赖
"@

$README | Out-File "$APP_DIR\README.txt" -Encoding ASCII

Write-Host ""
Write-Host "========================================"  -ForegroundColor Green
Write-Host "  Package Complete!"  -ForegroundColor Green
Write-Host "  Location: $APP_DIR\"  -ForegroundColor Green
Write-Host "  Run: $APP_NAME.bat"  -ForegroundColor Green
Write-Host "========================================"  -ForegroundColor Green
Write-Host ""

# Open output directory (only if not called from Java)
if ($env:FROM_JAVA -ne "1") {
    Start-Process "explorer.exe" $APP_DIR
    Write-Host "Press Enter to exit..."
    Read-Host
}
