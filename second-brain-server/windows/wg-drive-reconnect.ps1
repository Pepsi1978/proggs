# wg-drive-reconnect.ps1
# ERHOEHTER Teil des ZWEISTUFIGEN Netzlaufwerk-Reconnects (Direktive #3, Defense in Depth, Umbau 2026-06-25).
# Dieser Task laeuft ELEVATED (RunLevel Highest) und macht NUR, was Admin-Rechte braucht:
#   1. EnableLinkedConnections=1 setzen (Sicherheitsnetz, falls doch elevated gemappt wird)
#   2. den WireGuard-Tunnel-Dienst sicherstellen (Start + Auto-Recovery)
#   3. wg-drive-mount.ps1 als BACKUP-Mapping aufrufen (zweiter, unabhaengiger Weg)
#
# Das PRIMAERE, sichtbare Mapping macht der NICHT-erhoehte Task "WG-Drive-Mount" (wg-drive-mount.ps1):
# er mappt im Benutzer-Token, direkt im Explorer sichtbar, OHNE Abhaengigkeit von EnableLinkedConnections
# (Microsoft MapDrives-Linie, Recherche 2026-06-25 - EnableLinkedConnections versagt z.B. bei UAC
# "Prompt for credentials"). Dieser erhoehte Task ist nur das Backup, damit die Laufwerke selbst dann
# kommen, wenn der nicht-erhoehte Task mal nicht laeuft - ZWEI unabhaengige Wege, die Laufwerke zu mounten.
# Start KOMPLETT UNSICHTBAR ueber wg-drive-reconnect.vbs (Aufgabenplanung "WG-Drive-Reconnect").
#
# Historie: frueher machte dieses eine Skript alles (Dienst + Registry + Mapping) im erhoehten Token -
# dadurch hing die Sichtbarkeit allein an EnableLinkedConnections. Seit 2026-06-25 ist das Mapping in
# wg-drive-mount.ps1 ausgelagert und laeuft primaer nicht-erhoeht. Mapping-Bugs (1219, Boot-Race,
# net-use-Hang) sind dort geloest; siehe bugs/server/samba-wireguard.md SS9-SS11.
$ErrorActionPreference = 'SilentlyContinue'
$log = Join-Path $env:LOCALAPPDATA 'wg-drive-reconnect.log'
function Log($m) { try { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $m" | Out-File -FilePath $log -Append -Encoding utf8 } catch {} }

# 1) EnableLinkedConnections=1 sicherstellen - Sicherheitsnetz: falls ein erhoehter Prozess (das
# Backup-Mapping unten) Laufwerke mappt, macht dieser Wert sie im nicht-elevated Explorer sichtbar
# (UAC-Token-Isolation). Idempotent. ACHTUNG (Recherche 2026-06-25, MS KB 3035277): wirkt NICHT bei UAC
# "Prompt for credentials" (dritte Session) - darum ist der nicht-elevated Mount-Task der PRIMAERE Weg.
try {
    $polPath = 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System'
    $cur = (Get-ItemProperty -Path $polPath -Name 'EnableLinkedConnections' -ErrorAction SilentlyContinue).EnableLinkedConnections
    if ($cur -ne 1) {
        New-ItemProperty -Path $polPath -Name 'EnableLinkedConnections' -Value 1 -PropertyType DWord -Force -ErrorAction Stop | Out-Null
        Log 'EnableLinkedConnections=1 gesetzt (wirksam ab naechstem Login/Reboot)'
    }
} catch { Log "EnableLinkedConnections konnte nicht gesetzt werden (nicht elevated?): $($_.Exception.Message)" }

# 2) WireGuard-Dienst SICHERSTELLEN - der Tunnel-Dienst startet beim Booten manchmal nicht von selbst.
# Dieser Task laeuft erhoeht, darf den Dienst also starten + Auto-Wiederherstellung setzen.
# Tunnel-Dienstname dynamisch ermitteln (WireGuardTunnel$<Tunnelname>) - kein hartcodierter
# Rechner-Tunnelname mehr, laeuft so auf jedem Client (POWER-PC='pc', CODI='secondbrain', ...).
$svc = (Get-Service -Name 'WireGuardTunnel$*' -ErrorAction SilentlyContinue | Select-Object -First 1).Name
if (-not $svc) { $svc = 'WireGuardTunnel$pc' }
try {
    $s = Get-Service -Name $svc -ErrorAction Stop
    Log "WireGuard-Dienst Status: $($s.Status)"
    if ($s.Status -ne 'Running') {
        & sc.exe failure $svc reset= 86400 actions= restart/5000/restart/30000/restart/60000 2>$null | Out-Null
        Start-Service -Name $svc -ErrorAction SilentlyContinue
        Log "WireGuard-Dienst war $($s.Status) -> Recovery gesetzt + gestartet"
    }
} catch { Log "WireGuard-Dienst nicht zugaenglich: $($_.Exception.Message)" }

# 3) BACKUP-Mapping: dieselbe Mapping-Logik wie der nicht-erhoehte Task, hier im erhoehten Kontext als
# zweiter unabhaengiger Weg (Defense in Depth). DRY - die EINE Mapping-Logik lebt in wg-drive-mount.ps1
# (HKCU-Cleanup, Tunnel-Gate 445, nicht-persistent Mapping, 1219-Handling). Sieht es ein Laufwerk schon
# als OK, laesst es das in Ruhe (kein Doppel-Mapping/Blinken).
$mount = Join-Path $PSScriptRoot 'wg-drive-mount.ps1'
if (Test-Path $mount) {
    Log 'rufe wg-drive-mount.ps1 als Backup-Mapping auf (elevated)'
    & $mount
} else {
    Log "wg-drive-mount.ps1 nicht gefunden ($mount) - Backup-Mapping uebersprungen"
}
exit 0
