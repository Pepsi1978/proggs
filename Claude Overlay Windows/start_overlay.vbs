' start_overlay.vbs - Startet das Mic Overlay unsichtbar (ohne PowerShell-Fenster)
' Kann in den Windows-Autostart gelegt werden.

Dim objShell, scriptDir, venvPython
Set objShell = CreateObject("WScript.Shell")

' Verzeichnis dieses Skripts ermitteln
scriptDir = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
venvPython = scriptDir & "\.venv\Scripts\pythonw.exe"

' pythonw.exe nutzen (kein Konsolenfenster), Fallback auf python.exe
If Not CreateObject("Scripting.FileSystemObject").FileExists(venvPython) Then
    venvPython = scriptDir & "\.venv\Scripts\python.exe"
End If

objShell.CurrentDirectory = scriptDir
objShell.Run """" & venvPython & """ src\overlay_app.py", 0, False
