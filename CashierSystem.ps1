# Cashier System Launcher
# PowerShell script to launch JavaFX application with correct module path

$ErrorActionPreference = "Stop"

# Get script directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Configuration
$AppName = "Cashier System"
$MainClass = "com.cashier.CashierSystemFXApplication"
$JVM_OPTS = "-Xms512m", "-Xmx1024m", "-Dfile.encoding=UTF-8", "-Dsun.java2d.dpiaware=true"

# Find JAR file
$JarFile = Get-ChildItem -Path "$ScriptDir\*" -Filter "lisuan-fx-*-jar-with-dependencies.jar" -File | Select-Object -First 1

if (-not $JarFile) {
    # Try target directory
    $JarFile = Get-ChildItem -Path "$ScriptDir\target\*" -Filter "lisuan-fx-*-jar-with-dependencies.jar" -File | Select-Object -First 1
}

if (-not $JarFile) {
    [System.Windows.Forms.MessageBox]::Show(
        "Application JAR not found!`n`nPlease run: mvn clean package",
        "$AppName - Error",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    )
    exit 1
}

# Find JavaFX modules in Maven local repository
$MavenRepo = "$env:USERPROFILE\.m2\repository"
$JavaFXBase = Get-ChildItem -Path "$MavenRepo\org\openjfx\javafx-base\17.0.*" -Filter "javafx-base-*-win.jar" -File | Select-Object -First 1
$JavaFXControls = Get-ChildItem -Path "$MavenRepo\org\openjfx\javafx-controls\17.0.*" -Filter "javafx-controls-*-win.jar" -File | Select-Object -First 1
$JavaFXFXML = Get-ChildItem -Path "$MavenRepo\org\openjfx\javafx-fxml\17.0.*" -Filter "javafx-fxml-*-win.jar" -File | Select-Object -First 1
$JavaFXGraphics = Get-ChildItem -Path "$MavenRepo\org\openjfx\javafx-graphics\17.0.*" -Filter "javafx-graphics-*-win.jar" -File | Select-Object -First 1

# Build module path
$ModulePath = @()
if ($JavaFXBase) { $ModulePath += $JavaFXBase.DirectoryName }
if ($JavaFXControls) { $ModulePath += $JavaFXControls.DirectoryName }
if ($JavaFXFXML) { $ModulePath += $JavaFXFXML.DirectoryName }
if ($JavaFXGraphics) { $ModulePath += $JavaFXGraphics.DirectoryName }

$ModulePathStr = $ModulePath -join ';'

# Build arguments
$Args = @($JVM_OPTS)
if ($ModulePathStr) {
    $Args += "--module-path", $ModulePathStr
    $Args += "--add-modules", "javafx.controls,javafx.fxml"
}
$Args += "-jar", $JarFile.FullName

# Launch application
Write-Host "Starting $AppName..."
Write-Host "JAR: $($JarFile.Name)"

try {
    & java $Args 2>&1
} catch {
    [System.Windows.Forms.MessageBox]::Show(
        "Failed to start application:`n$_",
        "$AppName - Error",
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Error
    )
    exit 1
}
