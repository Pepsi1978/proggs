' Startet blinkschutz.ps1 komplett unsichtbar (kein aufblitzendes Konsolenfenster).
' Aufruf durch die Aufgabe "Explorer-Blinkschutz": wscript.exe blinkschutz.vbs
Dim sh, fso, ordner, ps
Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
ordner = fso.GetParentFolderName(WScript.ScriptFullName)
ps = "powershell.exe"
If fso.FileExists(sh.ExpandEnvironmentStrings("%ProgramFiles%\PowerShell\7\pwsh.exe")) Then
  ps = """" & sh.ExpandEnvironmentStrings("%ProgramFiles%\PowerShell\7\pwsh.exe") & """"
End If
sh.Run ps & " -NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & ordner & "\blinkschutz.ps1""", 0, False
