' watcher.vbs — Ueberwacht ClaudeVoiceOverlay und startet es bei Bedarf neu
' Laeuft unsichtbar im Hintergrund (kein Console-Fenster)
'
' Crash-Loop-Schutz: Wenn die .exe 5x hintereinander binnen 30 Sekunden crasht
' (typisch fuer Config-Fehler), bricht der Watcher ab statt endlos neu zu starten.

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
logPath = fso.BuildPath(scriptDir, "publish\watcher.log")

' Pruefen ob .exe existiert
If Not fso.FileExists(exePath) Then
    MsgBox "ClaudeVoiceOverlay.exe nicht gefunden:" & vbCrLf & exePath, vbCritical, "Watcher Fehler"
    WScript.Quit 1
End If

' Crash-Loop-Schutz Variablen
Const MAX_RAPID_CRASHES = 5      ' Max Crashes
Const RAPID_WINDOW_MS = 30000    ' Innerhalb von 30 Sekunden
Const STABLE_THRESHOLD_MS = 60000 ' Nach 60 Sekunden gilt App als stabil
crashTimes = ""
lastStartTime = 0

' Endlosschleife: pruefen ob Prozess laeuft, sonst starten
Do While True
    If Not IsProcessRunning("ClaudeVoiceOverlay.exe") Then
        nowMs = Timer * 1000  ' Sekunden seit Mitternacht * 1000

        ' Wenn die App vorher gestartet wurde und schnell wieder weg ist = Crash
        If lastStartTime > 0 And (nowMs - lastStartTime) < STABLE_THRESHOLD_MS Then
            crashTimes = crashTimes & "," & nowMs
            ' Nur Crashes innerhalb des Fensters zaehlen
            recentCrashes = CountRecentCrashes(crashTimes, nowMs, RAPID_WINDOW_MS)
            If recentCrashes >= MAX_RAPID_CRASHES Then
                LogMsg logPath, "ABBRUCH: " & recentCrashes & " Crashes in " & (RAPID_WINDOW_MS / 1000) & "s — vermutlich Config-Fehler. Watcher beendet."
                WScript.Quit 2
            End If
        Else
            ' App lief stabil — Crash-Counter zuruecksetzen
            crashTimes = ""
        End If

        ' Starten (0 = versteckt, False = nicht warten)
        ' Race-Condition-Schutz: Waehrend eines Rebuilds (publish.ps1) wird die .exe
        ' kurzzeitig geloescht/ersetzt. Dann NICHT mit blockierender Fehlerbox (80070002)
        ' abstuerzen, sondern Start ueberspringen und beim naechsten Durchlauf erneut versuchen.
        If fso.FileExists(exePath) Then
            On Error Resume Next
            WshShell.Run """" & exePath & """", 0, False
            If Err.Number <> 0 Then
                LogMsg logPath, "Start uebersprungen (Fehler " & Err.Number & "): " & Err.Description
                Err.Clear
            Else
                lastStartTime = nowMs
            End If
            On Error GoTo 0
            WScript.Sleep 5000  ' 5 Sekunden warten nach Start
        Else
            ' .exe gerade nicht vorhanden (vermutlich laeuft ein Rebuild) — geduldig warten
            LogMsg logPath, "exe nicht vorhanden (vermutlich Rebuild) — warte und versuche erneut."
            WScript.Sleep 3000
        End If
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

Function CountRecentCrashes(crashList, nowMs, windowMs)
    Dim parts, i, count, t
    count = 0
    parts = Split(crashList, ",")
    For i = 0 To UBound(parts)
        If Len(parts(i)) > 0 Then
            t = CDbl(parts(i))
            If (nowMs - t) <= windowMs Then
                count = count + 1
            End If
        End If
    Next
    CountRecentCrashes = count
End Function

Sub LogMsg(path, msg)
    On Error Resume Next
    Dim ts, f
    ts = FormatDateTime(Now, vbGeneralDate)
    Set f = CreateObject("Scripting.FileSystemObject").OpenTextFile(path, 8, True)
    f.WriteLine "[" & ts & "] " & msg
    f.Close
End Sub
