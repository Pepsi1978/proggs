# wg-drive-reconnect.ps1
# ERHOEHTER Teil des ZWEISTUFIGEN Netzlaufwerk-Reconnects (Direktive #3, Defense in Depth, Umbau 2026-06-25).
# Dieser Task laeuft ELEVATED (RunLevel Highest) und macht NUR, was Admin-Rechte braucht:
#   1. EnableLinkedConnections=1 setzen (Sicherheitsnetz, falls doch elevated gemappt wird)
#   2. den WireGuard-Tunnel-Dienst sicherstellen (Start + Auto-Recovery)
#   3. wg-drive-mount.ps1 aufrufen - seit 2026-10-08 mappt es erhoeht NICHT mehr, sondern gibt nur
#      erhoeht angelegte Alt-Mappings frei (das fruehere Backup-Mapping gewann den Wettlauf gegen die
#      Benutzer-Aufgabe -> Fehler 85, Z: im Explorer unsichtbar; bugs/server/samba-wireguard.md SS19)
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
    } else {
        # 2b) Dienst "Running" heisst NICHT, dass der Tunnel lebt (2026-11-07: Dienst lief, letzter Handshake
        # 29 Min alt, 10.8.0.1 tot -> Jarvis sah den Windows-PC nicht, nur den Server). Mit Keepalive 25 s
        # erneuert ein lebender Tunnel den Handshake etwa alle 2 Min. Ist er aelter als 180 s (oder nie),
        # Dienst neu starten - aber nur, wenn der Endpunkt im Internet erreichbar ist (sonst offline, Neustart sinnlos).
        $wg = Join-Path $env:ProgramFiles 'WireGuard\wg.exe'
        $tunnel = $svc -replace '^WireGuardTunnel\$', ''
        if (Test-Path $wg) {
            $hs = (& $wg show $tunnel latest-handshakes 2>$null | Select-Object -First 1) -split '\s+'
            $alter = if ($hs.Count -ge 2 -and [long]$hs[1] -gt 0) { [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() - [long]$hs[1] } else { -1 }
            if ($alter -lt 0 -or $alter -gt 180) {
                $ep = ((& $wg show $tunnel endpoints 2>$null | Select-Object -First 1) -split '\s+')[1]
                $epHost = if ($ep) { $ep -replace ':\d+$', '' -replace '^\[|\]$', '' } else { $null }
                if ($epHost -and (Test-Connection -ComputerName $epHost -Count 2 -Quiet -ErrorAction SilentlyContinue)) {
                    Restart-Service -Name $svc -Force -ErrorAction SilentlyContinue
                    Log "Tunnel tot (letzter Handshake vor $alter s, Endpunkt $epHost erreichbar) -> $svc neu gestartet"
                } else {
                    Log "Tunnel ohne Handshake ($alter s), Endpunkt '$epHost' nicht erreichbar -> offline, kein Neustart"
                }
            }
        }
    }
} catch { Log "WireGuard-Dienst nicht zugaenglich: $($_.Exception.Message)" }

# 3) BACKUP-Mapping: dieselbe Mapping-Logik wie der nicht-erhoehte Task, hier im erhoehten Kontext als
# zweiter unabhaengiger Weg (Defense in Depth). DRY - die EINE Mapping-Logik lebt in wg-drive-mount.ps1
# (HKCU-Cleanup, Tunnel-Gate 445, nicht-persistent Mapping, 1219-Handling). Sieht es ein Laufwerk schon
# als OK, laesst es das in Ruhe (kein Doppel-Mapping/Blinken).
$mount = Join-Path $PSScriptRoot 'wg-drive-mount.ps1'
if (Test-Path $mount) {
    # Seit 2026-10-08 mappt wg-drive-mount.ps1 erhoeht NICHT mehr (Wettlauf mit der Benutzer-Aufgabe ->
    # Fehler 85, Z: unsichtbar). Erhoeht gibt es nur noch eigene Alt-Mappings frei (Exit-Code 10);
    # danach wird die Benutzer-Aufgabe angestossen, damit das Laufwerk nicht 5 Min fehlt.
    Log 'rufe wg-drive-mount.ps1 zum Freigeben erhoehter Alt-Mappings auf (elevated)'
    & $mount
    if ($LASTEXITCODE -eq 10) {
        Start-Sleep -Seconds 5
        Start-ScheduledTask -TaskName 'WG-Drive-Mount' -ErrorAction SilentlyContinue
        Log 'Benutzer-Aufgabe WG-Drive-Mount nach Freigabe angestossen'
    }
} else {
    Log "wg-drive-mount.ps1 nicht gefunden ($mount) - Backup-Mapping uebersprungen"
}
exit 0
