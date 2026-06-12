# LiSuan Startup Script
# PowerShell version

$ErrorActionPreference = "Stop"

$APP_NAME = "LiSuan"
$APP_VERSION = "2.5.6"
$JAR_FILE = "target\lisuan-fx-$APP_VERSION-jar-with-dependencies.jar"

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host "  $APP_NAME v$APP_VERSION"  -ForegroundColor Cyan
Write-Host "  狸算收银系统"  -ForegroundColor Cyan
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host ""

# Check JAR
if (-not (Test-Path $JAR_FILE)) {
    Write-Host "Error: JAR not found" -ForegroundColor Red
    Write-Host "Please run: mvn package -DskipTests" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check Java
try {
    $null = Get-Command java -ErrorAction Stop
} catch {
    Write-Host "Error: Java not found" -ForegroundColor Red
    Write-Host "Please install Java 17 or higher" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Set JavaFX path
$JFX_BASE = "$env:USERPROFILE\.m2\repository\org\openjfx"
$JFX_PATH = @(
    "$JFX_BASE\javafx-base\17.0.12",
    "$JFX_BASE\javafx-controls\17.0.12",
    "$JFX_BASE\javafx-fxml\17.0.12",
    "$JFX_BASE\javafx-graphics\17.0.12"
) -join ';'

Write-Host "Starting $APP_NAME..." -ForegroundColor Green
Write-Host ""

# Run
$javaArgs = @(
    "--module-path", $JFX_PATH,
    "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics",
    "-Xms512m",
    "-Xmx1024m",
    "-Dfile.encoding=UTF-8",
    "-jar", $JAR_FILE
) + $args

& java $javaArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Application exited with code: $LASTEXITCODE" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
}
