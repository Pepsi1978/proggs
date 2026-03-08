' watcher.vbs — Ueberwacht ClaudeVoiceOverlay und startet es bei Bedarf neu
' Laeuft unsichtbar im Hintergrund (kein Console-Fenster)

Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Pruefen ob bereits ein anderer Watcher laeuft
Set objWMI = GetObject("winmgmts:\\.\root\cimv2")
Set colWscript = objWMI.ExecQuery("SELECT CommandLine FROM Win32_Process WHERE Name='wscript.exe'")
watcherCount = 0
myScript = LCase(WScript.ScriptFullName)
For Each proc In colWscript
    If InStr(LCase(proc.CommandLine), myScript) > 0 Then
        watcherCount = watcherCount + 1
    End If
Next
If watcherCount > 1 Then
    ' Ein anderer Watcher laeuft bereits — beenden
    WScript.Quit 0
End If

' Pfad zur .exe (publish-Ordner relativ zum Script)
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
exePath = fso.BuildPath(scriptDir, "publish\ClaudeVoiceOverlay.exe")

' Pruefen ob .exe existiert
If Not fso.FileExists(exePath) Then
    MsgBox "ClaudeVoiceOverlay.exe nicht gefunden:" & vbCrLf & exePath, vbCritical, "Watcher Fehler"
    WScript.Quit 1
End If

' Endlosschleife: pruefen ob Prozess laeuft, sonst starten
Do While True
    If Not IsProcessRunning("ClaudeVoiceOverlay.exe") Then
        ' Starten (0 = versteckt, False = nicht warten)
        WshShell.Run """" & exePath & """", 0, False
        WScript.Sleep 5000  ' 5 Sekunden warten nach Start
    End If
    WScript.Sleep 3000  ' Alle 3 Sekunden pruefen
Loop

Function IsProcessRunning(processName)
    IsProcessRunning = False
    Set objWMI2 = GetObject("winmgmts:\\.\root\cimv2")
    Set colProcesses = objWMI2.ExecQuery("SELECT Name FROM Win32_Process WHERE Name='" & processName & "'")
    If colProcesses.Count > 0 Then
        IsProcessRunning = True
    End If
End Function
