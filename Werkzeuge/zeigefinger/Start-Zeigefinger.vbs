Option Explicit

Dim shell, dateisystem, ordner, starter, befehl
Set shell = CreateObject("WScript.Shell")
Set dateisystem = CreateObject("Scripting.FileSystemObject")

ordner = dateisystem.GetParentFolderName(WScript.ScriptFullName)
starter = dateisystem.BuildPath(ordner, "Start-Zeigefinger.ps1")
befehl = "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & starter & """"

shell.Run befehl, 0, False
