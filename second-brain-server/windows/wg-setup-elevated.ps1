# wg-setup-elevated.ps1 - EINMALIGE erhoehte Einrichtung (UAC einmal bestaetigen). Danach laeuft alles unsichtbar.
# ZWEISTUFIGER Reconnect (Direktive #3, Defense in Depth, seit 2026-06-25 - zwei unabhaengige Wege):
#  1) Aufgabe "WG-Drive-Mount" NICHT-erhoeht (Limited) -> PRIMAERER, im Explorer SICHTBARER Mapping-Weg (wg-drive-mount.ps1).
#  2) Aufgabe "WG-Drive-Reconnect" ERHOEHT (Highest) -> WireGuard-Dienst sicherstellen + EnableLinkedConnections + Backup-Mapping.
#  3) Cortex-SSH-Schluessel-ACL idempotent setzen und per BatchMode testen (wenn der Schluessel vorhanden ist).
#  4) WireGuard-Tunnel-Dienst Auto-Wiederherstellung setzen (Neustart bei Boot-Fehler nach 5s/30s/60s).
# Schreibt das Ergebnis nach %LOCALAPPDATA%\wg-setup-result.txt (zur Kontrolle, kein Fenster noetig).
$ErrorActionPreference = 'Continue'
$res = Join-Path $env:LOCALAPPDATA 'wg-setup-result.txt'
function R($m) { $m | Out-File -FilePath $res -Append -Encoding utf8 }
"--- WG-Setup $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ---" | Out-File -FilePath $res -Encoding utf8

# Private Cortex-Schluessel duerfen unter Windows keine geerbten Gruppen-Leserechte behalten.
# Das dedizierte Skript setzt exakt Benutzer + SYSTEM + Administratoren und prueft echten SSH-Zugriff.
$aclSetup = Join-Path $PSScriptRoot 'set-cortex-ssh-key-acl.ps1'
$cortexKey = Join-Path $HOME 'SK\second-brain\id_ed25519'
if ((Test-Path -LiteralPath $aclSetup -PathType Leaf) -and
    (Test-Path -LiteralPath $cortexKey -PathType Leaf)) {
    try {
        & $aclSetup -KeyPath $cortexKey 2>&1 | ForEach-Object { R ("SSH-ACL: " + $_) }
        R "Cortex-SSH-Schluessel-ACL eingerichtet und getestet"
    } catch {
        R ("Cortex-SSH-Schluessel-ACL FEHLER: " + $_.Exception.Message)
    }
} else {
    R ("Cortex-SSH-Schluessel-ACL uebersprungen: Skript oder Schluessel fehlt (" + $cortexKey + ")")
}

# Rechner-/Benutzerkonto und Tunnel-Dienstname DYNAMISCH ermitteln (kein hartcodiertes POWER-PC\barwa /
# WireGuardTunnel$pc mehr) - laeuft so unveraendert auf jedem Windows-Client. Auf dem Haupt-PC ergeben
# diese Ausdruecke exakt die alten Werte (POWER-PC\barwa, WireGuardTunnel$pc) -> verhaltens-neutral.
$acct = "$env:COMPUTERNAME\$env:USERNAME"
R ("Konto (dynamisch): " + $acct)
$svcName = (Get-Service -Name 'WireGuardTunnel$*' -ErrorAction SilentlyContinue | Select-Object -First 1).Name
if (-not $svcName) { $svcName = 'WireGuardTunnel$pc' }
R ("WireGuard-Dienst (dynamisch): " + $svcName)

$vbs = Join-Path $PSScriptRoot 'wg-drive-reconnect.vbs'
$action = New-ScheduledTaskAction -Execute "wscript.exe" -Argument ('"' + $vbs + '"')
$t1 = New-ScheduledTaskTrigger -AtLogOn -User $acct
try { $t1.Repetition = (New-ScheduledTaskTrigger -Once -At (Get-Date) -RepetitionInterval (New-TimeSpan -Minutes 5)).Repetition } catch {}
$set = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -Hidden -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 5)
$principal = New-ScheduledTaskPrincipal -UserId $acct -LogonType Interactive -RunLevel Highest
try {
    Register-ScheduledTask -TaskName "WG-Drive-Reconnect" -Action $action -Trigger $t1 -Settings $set -Principal $principal -Description "Erhoehter Backup-Weg: startet WireGuard (falls aus) + EnableLinkedConnections + ruft wg-drive-mount.ps1 als Backup-Mapping. Primaerer Weg ist die nicht-erhoehte Aufgabe WG-Drive-Mount." -Force | Out-Null
    R ("Aufgabe WG-Drive-Reconnect (erhoeht) registriert, RunLevel: " + ((Get-ScheduledTask -TaskName 'WG-Drive-Reconnect').Principal.RunLevel))
} catch { R ("Aufgabe-FEHLER: " + $_.Exception.Message) }

# Aufgabe "WG-Drive-Mount" NICHT-erhoeht (RunLevel Limited) - der PRIMAERE, im Explorer SICHTBARE Mapping-Weg.
# Mappt im Benutzer-Token -> direkt sichtbar, ohne Abhaengigkeit von EnableLinkedConnections (Microsoft
# MapDrives-Linie, Recherche 2026-06-25). Der erhoehte Task oben ist nur das Backup (zwei unabhaengige Wege).
$vbsMount = Join-Path $PSScriptRoot 'wg-drive-mount.vbs'
$actionM = New-ScheduledTaskAction -Execute "wscript.exe" -Argument ('"' + $vbsMount + '"')
$tM = New-ScheduledTaskTrigger -AtLogOn -User $acct
try { $tM.Repetition = (New-ScheduledTaskTrigger -Once -At (Get-Date) -RepetitionInterval (New-TimeSpan -Minutes 5)).Repetition } catch {}
$setM = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -Hidden -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 5)
$principalM = New-ScheduledTaskPrincipal -UserId $acct -LogonType Interactive -RunLevel Limited
try {
    Register-ScheduledTask -TaskName "WG-Drive-Mount" -Action $actionM -Trigger $tM -Settings $setM -Principal $principalM -Description "Primaerer, im Explorer sichtbarer Reconnect der WireGuard-Netzlaufwerke Y:/Z: (nicht-erhoeht). Teil des zweistufigen Reconnects (siehe wg-drive-mount.ps1)." -Force | Out-Null
    R ("Aufgabe WG-Drive-Mount (nicht-erhoeht) registriert, RunLevel: " + ((Get-ScheduledTask -TaskName 'WG-Drive-Mount').Principal.RunLevel))
} catch { R ("Aufgabe WG-Drive-Mount-FEHLER: " + $_.Exception.Message) }

& sc.exe failure $svcName reset= 86400 actions= restart/5000/restart/30000/restart/60000 | Out-Null
R ("WireGuard-Recovery gesetzt (sc.exe exit " + $LASTEXITCODE + ")")

# EnableLinkedConnections=1: macht im ERHOEHTEN Task gemappte Netzlaufwerke im normalen (nicht-elevated)
# Explorer sichtbar. Ohne diesen Wert sind erhoehte/normale Mappings durch UAC getrennt -> Z:/Y: erscheinen
# nicht im Explorer, obwohl der Task sie erfolgreich verbindet. Wirksam ab naechstem Login/Reboot. (MS KB)
try {
    New-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System' -Name 'EnableLinkedConnections' -Value 1 -PropertyType DWord -Force -ErrorAction Stop | Out-Null
    R "EnableLinkedConnections=1 gesetzt (erhoehte Mappings im Explorer sichtbar ab naechstem Login)"
} catch { R ("EnableLinkedConnections-FEHLER: " + $_.Exception.Message) }
R "FERTIG"
