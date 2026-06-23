# wg-drive-reconnect.ps1
# Verbindet die "Gehirn"-Netzlaufwerke Z: (gedanken) und Y: (daten) automatisch neu, sobald der
# WireGuard-Tunnel (10.8.0.1) erreichbar ist. Behebt das rote X / "Nicht verfuegbar" nach Neustart
# oder Standby. Wird KOMPLETT UNSICHTBAR ueber wg-drive-reconnect.vbs durch eine Aufgabenplanung
# (Anmeldung + Entsperren) gestartet — keine Fenster, keine Popups, kein Passwort-Dialog (Credential
# liegt im Windows-Tresor via cmdkey). Schreibt nur eine kleine Log-Datei (kein UI).
$ErrorActionPreference = 'SilentlyContinue'
$log = Join-Path $env:LOCALAPPDATA 'wg-drive-reconnect.log'
function Log($m) { try { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $m" | Out-File -FilePath $log -Append -Encoding utf8 } catch {} }

# Ist der WireGuard-Tunnel oben? (schneller TCP-Test auf das Gehirn, max 1,5 s)
function WgUp {
    try {
        $c = New-Object System.Net.Sockets.TcpClient
        $ok = $c.ConnectAsync('10.8.0.1', 8000).Wait(1500)
        $c.Close()
        return $ok
    } catch { return $false }
}

# 0) WireGuard-Dienst SICHERSTELLEN — HAUPTFIX: der Tunnel-Dienst startet beim Booten oft NICHT von
# selbst (seine Recovery-Aktionen waren leer, Windows versucht es nicht erneut). Diese Aufgabe laeuft
# erhoeht (RunLevel Highest, da 'barwa' Admin ist), darf den Dienst also starten + die Auto-Wiederherstellung setzen.
$svc = 'WireGuardTunnel$pc'
try {
    $s = Get-Service -Name $svc -ErrorAction Stop
    Log "WireGuard-Dienst Status: $($s.Status)"
    if ($s.Status -ne 'Running') {
        # Auto-Wiederherstellung setzen (bei Fehler nach 5s/30s/60s neu starten) + jetzt starten
        & sc.exe failure $svc reset= 86400 actions= restart/5000/restart/30000/restart/60000 2>$null | Out-Null
        Start-Service -Name $svc -ErrorAction SilentlyContinue
        Log "WireGuard-Dienst war $($s.Status) -> Recovery gesetzt + gestartet"
    }
} catch { Log "WireGuard-Dienst nicht zugaenglich: $($_.Exception.Message)" }

# 1) Auf den Tunnel warten (Handshake braucht ein paar Sekunden) — max ~120 s
$up = $false
for ($i = 0; $i -lt 60; $i++) { if (WgUp) { $up = $true; break }; Start-Sleep -Seconds 2 }
if (-not $up) { Log 'WireGuard nach Start-Versuch nicht erreichbar - abgebrochen'; exit 0 }

# Laufwerke pruefen; nur die neu verbinden, die NICHT "OK" sind (kein unnoetiges Blinken bei gesunden)
$map = [ordered]@{ 'Z:' = '\\10.8.0.1\gedanken'; 'Y:' = '\\10.8.0.1\daten' }
$lines = cmd /c "net use" 2>$null
foreach ($d in $map.Keys) {
    $okState = $false
    foreach ($ln in $lines) {
        if ($ln -match [regex]::Escape($map[$d]) -and $ln.TrimStart().StartsWith('OK')) { $okState = $true; break }
    }
    if ($okState) { Log "$d ist OK"; continue }
    cmd /c "net use $d /delete /y" 2>$null | Out-Null
    cmd /c "net use $d $($map[$d]) /persistent:yes" 2>$null | Out-Null
    Log "$d war nicht verbunden -> reconnect"
}
exit 0
