' ============================================
'   Cashier System Launcher (Console-Free)
'   Version 2.5.0
'
'   This VBS script launches the cashier system
'   without displaying a console window.
'
'   Usage: Double-click this file or create a
'          shortcut to it on desktop/Start Menu
' ============================================

Option Explicit

Dim shell, env, fso
Dim javaPath, jarFile, appDir, jvmOpts
Dim configFilePath, configFile
Dim procCount, processes

' Create shell and filesystem objects
Set shell = CreateObject("WScript.Shell")
Set env = shell.Environment("Process")
Set fso = CreateObject("Scripting.FileSystemObject")

' Get application directory (script location)
appDir = fso.GetParentFolderName(WScript.ScriptFullName)

' Check for running instances
procCount = 0
Set processes = GetObject("winmgmts:root\cimv2").ExecQuery( _
    "SELECT * FROM Win32_Process WHERE Name='java.exe' AND CommandLine LIKE '%cashier-system%'")

For Each proc In processes
    procCount = procCount + 1
Next

If procCount > 0 Then
    Dim result
    result = MsgBox("收银系统已在运行中！" & vbCrLf & vbCrLf & _
                   "是否要强制启动另一个实例？" & vbCrLf & vbCrLf & _
                   "点击"是"强制启动，点击"否"退出。", _
                   vbExclamation + vbYesNo, "系统提示")
    If result = vbNo Then
        WScript.Quit 0
    End If
End If

' Determine Java path
javaPath = env.Item("JAVA_HOME")
If javaPath <> "" Then
    If Right(javaPath, 1) <> "\" Then
        javaPath = javaPath & "\"
    End If
    javaPath = """" & javaPath & "bin\javaw.exe"""
Else
    ' Try to find javaw in PATH
    javaPath = "javaw"
End If

' Verify Java is available
Dim javaCheck
javaCheck = shell.Run("java -version", 0, True)
If javaCheck <> 0 Then
    javaCheck = shell.Run("javaw -version", 0, True)
    If javaCheck <> 0 Then
        MsgBox "未找到 Java 运行时！" & vbCrLf & vbCrLf & _
               "请安装 Java 17 或更高版本。" & vbCrLf & vbCrLf & _
               "下载地址：" & vbCrLf & _
               "  - Oracle JDK: https://www.oracle.com/java/technologies/downloads/" & vbCrLf & _
               "  - OpenJDK: https://adoptium.net/" & vbCrLf & _
               "  - Winget: winget install Oracle.JDK.17", _
               vbCritical, "启动失败"
        WScript.Quit 1
    End If
End If

' Find JAR file (try different version patterns)
Dim version, jarPath
jarPath = ""

' Try to read version from pom.xml
If fso.FileExists(appDir & "\pom.xml") Then
    Dim pomLine, pomFile
    Set pomFile = fso.OpenTextFile(appDir & "\pom.xml", 1, False)
    Do While Not pomFile.AtEndOfStream
        pomLine = pomFile.ReadLine()
        If InStr(pomLine, "<version>") > 0 And InStr(pomLine, "javafx") = 0 And InStr(pomLine, "maven") = 0 Then
            version = Mid(pomLine, InStr(pomLine, "<version>") + 9, InStr(pomLine, "</version>") - InStr(pomLine, "<version>") - 9)
            jarPath = appDir & "\target\cashier-system-fx-" & version & "-jar-with-dependencies.jar"
            If fso.FileExists(jarPath) Then
                Exit Do
            End If
        End If
    Loop
    pomFile.Close
End If

' Fallback to default path if not found
If jarPath = "" Or Not fso.FileExists(jarPath) Then
    jarPath = appDir & "\target\cashier-system-fx-2.5.0-jar-with-dependencies.jar"
    If Not fso.FileExists(jarPath) Then
        ' Try to find any JAR with cashier-system-fx in the name
        Dim folder, file
        Set folder = fso.GetFolder(appDir & "\target")
        For Each file In folder.Files
            If InStr(file.Name, "cashier-system-fx") > 0 And Right(file.Name, 4) = ".jar" Then
                jarPath = file.Path
                Exit For
            End If
        Next
    End If
End If

' Check if JAR exists
If Not fso.FileExists(jarPath) Then
    MsgBox "未找到应用程序 JAR 文件！" & vbCrLf & vbCrLf & _
           "请先运行 start.bat 进行编译。" & vbCrLf & vbCrLf & _
           "预期路径：" & vbCrLf & _
           "  " & jarPath, _
           vbCritical, "启动失败"
    WScript.Quit 1
End If

' Load JVM options from config file if exists
configFilePath = appDir & "\config\jvm.config"
jvmOpts = ""

If fso.FileExists(configFilePath) Then
    Set configFile = fso.OpenTextFile(configFilePath, 1, False)
    Do While Not configFile.AtEndOfStream
        Dim line
        line = Trim(configFile.ReadLine())
        If Left(line, 1) <> "#" And line <> "" Then
            jvmOpts = jvmOpts & " " & line
        End If
    Loop
    configFile.Close
End If

' If no custom config, use defaults
If jvmOpts = "" Then
    jvmOpts = "-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"
End If

' Launch the application
Dim command
command = javaPath & " " & jvmOpts & " -jar """ & jarPath & """"

' Run without showing any window (0 = hide, False = don't wait)
shell.Run command, 0, False

' Clean up
Set shell = Nothing
Set env = Nothing
Set fso = Nothing
