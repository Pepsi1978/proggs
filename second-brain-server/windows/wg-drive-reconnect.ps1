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
        # 32 Min alt, 10.8.0.1 tot -> Jarvis sah den Windows-PC nicht, nur den Server). Mit Keepalive 15-25 s
        # erneuert ein lebender Tunnel den Handshake etwa alle 2 Min; kurze Aussetzer heilt WireGuard selbst.
        # Darum erst ab 300 s ohne Handshake neu starten, nie in den ersten 5 Min nach Dienststart, hoechstens
        # einmal je 30 Min (Bremse gegen Endlos-Neustarts, wenn der Server selbst kaputt ist), und nur wenn der
        # Server-Host ueberhaupt erreichbar ist (Ping ODER TCP 443, ICMP ist nicht ueberall erlaubt).
        $wg = Join-Path $env:ProgramFiles 'WireGuard\wg.exe'
        if (-not (Test-Path $wg)) { $wg = (Get-Command wg.exe -ErrorAction SilentlyContinue).Source }
        $tunnel = $svc -replace '^WireGuardTunnel\$', ''
        $bremse = Join-Path $env:LOCALAPPDATA 'wg-tunnel-neustart.txt'
        if (-not $wg) {
            Log 'wg.exe nicht gefunden -> Tunnel-Lebenspruefung uebersprungen'
        } else {
            # dump: Zeile 1 = Interface, danach je Peer: key, psk, endpoint, allowed-ips, latest-handshake, rx, tx, keepalive.
            $peers = @(& $wg show $tunnel dump 2>$null | Select-Object -Skip 1 | ForEach-Object { ,($_ -split "`t") } | Where-Object { $_.Count -ge 5 })
            # Der Server-Peer ist der mit 10.8.0.1 bzw. dem ganzen Tunnelnetz in AllowedIPs; sonst der erste.
            $peer = $peers | Where-Object { $_[3] -match '10\.8\.0\.(1/32|0/24)' } | Select-Object -First 1
            if (-not $peer) { $peer = $peers | Select-Object -First 1 }
            $hsZeit = 0L
            if (-not $peer -or -not [long]::TryParse($peer[4], [ref]$hsZeit)) {
                Log "wg show $tunnel dump lieferte nichts Auswertbares -> Tunnel-Lebenspruefung uebersprungen"
            } else {
                $alter = if ($hsZeit -gt 0) { [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() - $hsZeit } else { -1 }
                $dienstPid = (Get-CimInstance Win32_Service -Filter "Name='$($svc -replace "'", "''")'").ProcessId
                $dienstStart = (Get-Process -Id $dienstPid -ErrorAction SilentlyContinue).StartTime
                $dienstFrisch = $dienstStart -and ((Get-Date) - $dienstStart).TotalSeconds -lt 300
                $letzter = $null
                if (Test-Path $bremse) { try { $letzter = [datetime]::Parse((Get-Content $bremse -Raw).Trim()) } catch {} }
                $gebremst = $letzter -and ((Get-Date) - $letzter).TotalMinutes -lt 30
                if (($alter -lt 0 -or $alter -gt 300) -and -not $dienstFrisch) {
                    $epHost = if ($peer[2] -and $peer[2] -ne '(none)') { $peer[2] -replace ':\d+$', '' -replace '^\[|\]$', '' } else { $null }
                    $online = $false
                    if ($epHost) {
                        $online = Test-Connection -ComputerName $epHost -Count 2 -Quiet -ErrorAction SilentlyContinue
                        if (-not $online) {
                            $c = New-Object Net.Sockets.TcpClient
                            try { $online = $c.ConnectAsync($epHost, 443).Wait(3000) -and $c.Connected } catch {} finally { $c.Close() }
                        }
                    }
                    $grund = if ($alter -lt 0) { 'noch nie' } else { "vor $alter s" }
                    if (-not $online) {
                        Log "Tunnel ohne Handshake ($grund), Server '$epHost' nicht erreichbar -> offline, kein Neustart"
                    } elseif ($gebremst) {
                        Log "Tunnel ohne Handshake ($grund), letzter Neustart erst $($letzter.ToString('HH:mm')) -> gebremst, kein Neustart"
                    } else {
                        (Get-Date).ToString('o') | Out-File -FilePath $bremse -Encoding ascii
                        Restart-Service -Name $svc -Force -ErrorAction SilentlyContinue
                        # Auf den ersten Handshake warten, sonst scheitert das Laufwerks-Mapping unten am toten Tunnel.
                        $neu = 0L
                        for ($i = 0; $i -lt 15 -and $neu -le 0; $i++) {
                            Start-Sleep -Seconds 2
                            $z = @(& $wg show $tunnel latest-handshakes 2>$null | ForEach-Object { ,($_ -split '\s+') } | Where-Object { $_[0] -eq $peer[0] })
                            if ($z) { [void][long]::TryParse($z[0][1], [ref]$neu) }
                        }
                        Log ("Tunnel tot (letzter Handshake $grund, Server $epHost erreichbar) -> $svc neu gestartet, " + $(if ($neu -gt 0) { 'Handshake wieder da' } else { 'nach 30 s noch kein Handshake' }))
                    }
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
