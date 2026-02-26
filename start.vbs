Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Get script directory
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
jarFile = scriptDir & "\target\cashier-system-fx-2.3.1-jar-with-dependencies.jar"
configFile = scriptDir & "\config\jvm.config"

' Check if JAR file exists
If Not fso.FileExists(jarFile) Then
    WScript.Echo "JAR file not found. Compiling..."
    WshShell.Run "cmd /c cd /d """ & scriptDir & """ && mvn clean package -DskipTests", 1, True
End If

' Build JVM parameters from config file
jvmOpts = "-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Dsun.java2d.dpiaware=true"

' Read JVM config file if exists
If fso.FileExists(configFile) Then
    Set configFileStream = fso.OpenTextFile(configFile, 1)
    Do Until configFileStream.AtEndOfStream
        line = Trim(configFileStream.ReadLine)
        ' Ignore empty lines and comments
        If Len(line) > 0 And Left(line, 1) <> "#" Then
            jvmOpts = jvmOpts & " " & line
        End If
    Loop
    configFileStream.Close
End If

' Start application using javaw (no console window)
Dim cmd
cmd = "javaw " & jvmOpts & " -jar " & Chr(34) & jarFile & Chr(34)
WshShell.Run cmd, 0, False