Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Get script directory
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
jarFile = scriptDir & "\target\cashier-system-fx-2.3.1-jar-with-dependencies.jar"

' Check if JAR file exists
If Not fso.FileExists(jarFile) Then
    ' JAR file not found, try to compile
    WScript.Echo "JAR file not found. Compiling..."
    WshShell.Run "cmd /c cd /d """ & scriptDir & """ && mvn clean package -DskipTests", 1, True
End If

' Start application using javaw (no console window)
command = "javaw -jar """ & jarFile & """""
WshShell.Run command, 0, False