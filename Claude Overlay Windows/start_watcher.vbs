' start_watcher.vbs - Startet den Watcher komplett unsichtbar.
' Kein Konsolenfenster, kein Taskleisten-Eintrag.
' Kann direkt oder ueber den Windows-Autostart aufgerufen werden.

Dim objShell, fso, scriptDir, venvPythonw, venvPython, pyExe

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

' pythonw.exe bevorzugen (kein Konsolenfenster)
If fso.FileExists(venvPythonw) Then
    pyExe = venvPythonw
Else
    pyExe = venvPython
End If

' Watcher starten: Fensterstil 0 = komplett unsichtbar, False = nicht auf Ende warten
objShell.CurrentDirectory = scriptDir
objShell.Run """" & pyExe & """ src\watcher.py", 0, False
