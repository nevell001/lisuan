' ========================================
' LiSuan Launcher (Windows)
' ========================================
Option Explicit

On Error Resume Next

Dim shell, fso, appPath, jarFile, javaPath
Dim javaCmd, processCount

Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Get script directory
Dim scriptDir
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)

' JAR file path
jarFile = scriptDir & "\target\cashier-system-fx-2.5.5-jar-with-dependencies.jar"

' Check if JAR file exists
If Not fso.FileExists(jarFile) Then
    ' Try to find any version of JAR
    Dim folder, file
    If fso.FolderExists(scriptDir & "\target") Then
        Set folder = fso.GetFolder(scriptDir & "\target")
        jarFile = ""
        For Each file In folder.Files
            If InStr(file.Name, "cashier-system-fx") > 0 And InStr(file.Name, "jar-with-dependencies.jar") > 0 Then
                jarFile = file.Path
                Exit For
            End If
        Next
    End If
    If jarFile = "" Then
        MsgBox "Application JAR file not found!" & vbCrLf & vbCrLf & _
               "Please run: mvn clean package" & vbCrLf & _
               "Or ensure JAR file is in target directory.", _
               vbCritical, "LiSuan Launcher Error"
        WScript.Quit 1
    End If
End If

' Check if Java is available
javaPath = shell.ExpandEnvironmentStrings("%JAVA_HOME%")
If javaPath <> "" And Right(javaPath, 1) <> "\" Then
    javaPath = javaPath & "\"
End If

' Check if javaw.exe exists
If javaPath <> "" And fso.FileExists(javaPath & "bin\javaw.exe") Then
    javaPath = """" & javaPath & "bin\javaw.exe"""
Else
    ' Try to get Java from PATH
    javaPath = "javaw"
End If

' Start application (no console window)
Dim cmdLine
cmdLine = javaPath & " -jar """ & jarFile & """"
shell.Run cmdLine, 0, False

' Cleanup
Set shell = Nothing
Set fso = Nothing

WScript.Quit 0
