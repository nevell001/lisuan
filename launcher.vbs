' ========================================
' 收银系统无控制台启动器 (Windows)
' ========================================
Option Explicit

On Error Resume Next

Dim shell, fso, appPath, jarFile, javaPath
Dim javaCmd, processCount

Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' 获取脚本所在目录
Dim scriptDir
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)

' JAR 文件路径
jarFile = scriptDir & "\target\cashier-system-fx-2.5.5-jar-with-dependencies.jar"

' 检查 JAR 文件是否存在
If Not fso.FileExists(jarFile) Then
    ' 尝试查找任意版本的 JAR
    Dim folder, file
    Set folder = fso.GetFolder(scriptDir & "\target")
    jarFile = ""
    For Each file In folder.Files
        If InStr(file.Name, "cashier-system-fx") > 0 And InStr(file.Name, "jar-with-dependencies.jar") > 0 Then
            jarFile = file.Path
            Exit For
        End If
    Next
    If jarFile = "" Then
        MsgBox "未找到程序文件！" & vbCrLf & vbCrLf & _
               "请先运行: mvn clean package" & vbCrLf & _
               "或确保 JAR 文件在 target 目录中。", _
               vbCritical, "收银系统启动错误"
        WScript.Quit 1
    End If
End If

' 检查是否已有实例运行
javaCmd = "tasklist /FI ""IMAGENAME eq javaw.exe"" /V 2>/dev/null | find /I ""cashier-system"""
shell.Run javaCmd, 0, True

' 尝试获取 Java 路径
javaPath = shell.ExpandEnvironmentStrings("%JAVA_HOME%")
If javaPath <> "" And Right(javaPath, 1) <> "\" Then
    javaPath = javaPath & "\"
End If

' 检查 javaw.exe 是否存在
If fso.FileExists(javaPath & "bin\javaw.exe") Then
    javaPath = """" & javaPath & "bin\javaw.exe"""
Else
    ' 尝试从 PATH 中获取 Java
    javaPath = shell.ExpandEnvironmentStrings("%javaPath%")
    If javaPath = "" Then
        javaPath = "javaw"
    Else
        javaPath = """" & javaPath & "\javaw.exe"""
    End If
End If

' 启动应用（无控制台窗口，WindowStyle=0，bWaitOnReturn=False）
Dim cmdLine
cmdLine = javaPath & " -jar """ & jarFile & """"
shell.Run cmdLine, 0, False

' 清理
Set shell = Nothing
Set fso = Nothing

WScript.Quit 0
