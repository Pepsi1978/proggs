' wg-drive-mount.vbs
' Startet wg-drive-mount.ps1 KOMPLETT UNSICHTBAR (kein Fenster, kein kurzes Aufblitzen).
' wscript.exe hat selbst keine Konsole; PowerShell wird mit Fensterstil 0 (versteckt) gestartet.
' Wird von der NICHT-erhoehten Aufgabe "WG-Drive-Mount" bei Anmeldung und alle 5 Min aufgerufen -
' der primaere, im Explorer sichtbare Mapping-Weg (Direktive #3, Defense in Depth).
Set sh = CreateObject("WScript.Shell")
ps1 = "C:\Users\barwa\proggs\second-brain-server\windows\wg-drive-mount.ps1"
sh.Run "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & ps1 & """", 0, False
