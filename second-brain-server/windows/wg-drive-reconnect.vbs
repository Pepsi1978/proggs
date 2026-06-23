' wg-drive-reconnect.vbs
' Startet wg-drive-reconnect.ps1 KOMPLETT UNSICHTBAR (kein Fenster, kein kurzes Aufblitzen).
' wscript.exe hat selbst keine Konsole; PowerShell wird mit Fensterstil 0 (versteckt) gestartet.
' Wird von der Aufgabenplanung "WG-Drive-Reconnect" bei Anmeldung und Entsperren aufgerufen.
Set sh = CreateObject("WScript.Shell")
ps1 = "C:\Users\barwa\proggs\second-brain-server\windows\wg-drive-reconnect.ps1"
sh.Run "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & ps1 & """", 0, False
