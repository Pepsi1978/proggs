' Start Overlay (Desktop).vbs - Desktop-Verknuepfung fuer den Watcher.
' Startet den Watcher unsichtbar. Dieser ueberwacht Claude Desktop
' und startet/beendet das Overlay automatisch.

Dim objShell, fso, scriptDir, venvPythonw, venvPython

Set objShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Verzeichnis dieses Skripts ermitteln
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
venvPythonw = scriptDir & "\.venv\Scripts\pythonw.exe"
venvPython = scriptDir & "\.venv\Scripts\python.exe"

' Pruefen ob die venv existiert
If Not fso.FileExists(venvPythonw) And Not fso.FileExists(venvPython) Then
    MsgBox "Python-Umgebung nicht gefunden!" & vbCrLf & vbCrLf & _
           "Erwartet: " & venvPythonw & vbCrLf & vbCrLf & _
           "Bitte zuerst die Installation gemaess README.md durchfuehren.", _
           vbExclamation, "Claude Overlay Watcher"
    WScript.Quit 1
End If

' pythonw.exe bevorzugen (kein Konsolenfenster), Fallback auf python.exe
Dim pyExe
If fso.FileExists(venvPythonw) Then
    pyExe = venvPythonw
Else
    pyExe = venvPython
End If

' Watcher starten
objShell.CurrentDirectory = scriptDir
objShell.Run """" & pyExe & """ src\watcher.py", 0, False
