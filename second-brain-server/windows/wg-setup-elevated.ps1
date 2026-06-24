# wg-setup-elevated.ps1 - EINMALIGE erhoehte Einrichtung (UAC einmal bestaetigen). Danach laeuft alles unsichtbar.
#  1) Aufgabe "WG-Drive-Reconnect" ERHOEHT (Highest) registrieren -> darf WireGuard starten + Z:/Y: verbinden.
#  2) WireGuard-Tunnel-Dienst Auto-Wiederherstellung setzen (Neustart bei Boot-Fehler nach 5s/30s/60s).
# Schreibt das Ergebnis nach %LOCALAPPDATA%\wg-setup-result.txt (zur Kontrolle, kein Fenster noetig).
$ErrorActionPreference = 'Continue'
$res = Join-Path $env:LOCALAPPDATA 'wg-setup-result.txt'
function R($m) { $m | Out-File -FilePath $res -Append -Encoding utf8 }
"--- WG-Setup $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ---" | Out-File -FilePath $res -Encoding utf8

$vbs = "C:\Users\barwa\proggs\second-brain-server\windows\wg-drive-reconnect.vbs"
$action = New-ScheduledTaskAction -Execute "wscript.exe" -Argument ('"' + $vbs + '"')
$t1 = New-ScheduledTaskTrigger -AtLogOn -User "POWER-PC\barwa"
try { $t1.Repetition = (New-ScheduledTaskTrigger -Once -At (Get-Date) -RepetitionInterval (New-TimeSpan -Minutes 5)).Repetition } catch {}
$set = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -Hidden -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 5)
$principal = New-ScheduledTaskPrincipal -UserId "POWER-PC\barwa" -LogonType Interactive -RunLevel Highest
try {
    Register-ScheduledTask -TaskName "WG-Drive-Reconnect" -Action $action -Trigger $t1 -Settings $set -Principal $principal -Description "Startet WireGuard (falls aus) + verbindet die Gehirn-Laufwerke Z:/Y: automatisch + unsichtbar neu." -Force | Out-Null
    R ("Aufgabe registriert, RunLevel: " + ((Get-ScheduledTask -TaskName 'WG-Drive-Reconnect').Principal.RunLevel))
} catch { R ("Aufgabe-FEHLER: " + $_.Exception.Message) }

& sc.exe failure 'WireGuardTunnel$pc' reset= 86400 actions= restart/5000/restart/30000/restart/60000 | Out-Null
R ("WireGuard-Recovery gesetzt (sc.exe exit " + $LASTEXITCODE + ")")

# EnableLinkedConnections=1: macht im ERHOEHTEN Task gemappte Netzlaufwerke im normalen (nicht-elevated)
# Explorer sichtbar. Ohne diesen Wert sind erhoehte/normale Mappings durch UAC getrennt -> Z:/Y: erscheinen
# nicht im Explorer, obwohl der Task sie erfolgreich verbindet. Wirksam ab naechstem Login/Reboot. (MS KB)
try {
    New-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System' -Name 'EnableLinkedConnections' -Value 1 -PropertyType DWord -Force -ErrorAction Stop | Out-Null
    R "EnableLinkedConnections=1 gesetzt (erhoehte Mappings im Explorer sichtbar ab naechstem Login)"
} catch { R ("EnableLinkedConnections-FEHLER: " + $_.Exception.Message) }
R "FERTIG"
