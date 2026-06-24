# wg-drive-reconnect.ps1
# Verbindet die "Gehirn"-Netzlaufwerke Z: (gedanken) und Y: (daten) automatisch neu, sobald der
# WireGuard-Tunnel (10.8.0.1) erreichbar ist. Behebt das rote X / "Nicht verfuegbar" nach Neustart
# oder Standby. Wird KOMPLETT UNSICHTBAR ueber wg-drive-reconnect.vbs durch eine Aufgabenplanung
# (Anmeldung + Entsperren) gestartet — keine Fenster, keine Popups, kein Passwort-Dialog (Credential
# liegt im Windows-Tresor via cmdkey). Schreibt nur eine kleine Log-Datei (kein UI).
$ErrorActionPreference = 'SilentlyContinue'
$log = Join-Path $env:LOCALAPPDATA 'wg-drive-reconnect.log'
function Log($m) { try { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $m" | Out-File -FilePath $log -Append -Encoding utf8 } catch {} }

# Ist der Tunnel oben UND der SMB-Server da? Schneller TCP-Test auf den SMB-Port 445 von 10.8.0.1.
# WICHTIG (Root-Cause-Fix 2026-06-24): Frueher wurde Port 8000 (brain-api) getestet — das machte den
# Laufwerks-Reconnect von einem FREMDEN Dienst abhaengig. Bei einem brain-api-Neustart (z.B. Deploy)
# war 8000 kurz weg -> dieser Gate-Check schlug an -> Z:/Y: wurden NIE gemappt, obwohl SMB (445) lief.
# Jetzt testen wir genau den Port, von dem Z:/Y: tatsaechlich abhaengen: 445 (SMB). $c.Connected statt
# nur Wait()-Rueckgabe (faulted/timeout sauber als "nicht oben" werten), Socket immer disposen.
function WgUp {
    $c = $null
    try {
        $c = New-Object System.Net.Sockets.TcpClient
        $done = $c.ConnectAsync('10.8.0.1', 445).Wait(2000)
        return ($done -and $c.Connected)
    } catch { return $false }
    finally { if ($c) { $c.Close(); $c.Dispose() } }
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
    # Alte (ggf. tote) Verbindung loesen — ein Fehlschlag hier ist normal (war nicht verbunden), nicht loggen.
    cmd /c "net use $d /delete /y" 2>&1 | Out-Null
    # Neu mappen — Ergebnis NICHT verschlucken: Exit-Code + Meldung ins Log (Observability-First).
    # Ohne dieses Logging war der eigentliche Mapping-Fehlschlag unsichtbar (nur "Status: Running" stand im Log).
    $out = cmd /c "net use $d $($map[$d]) /persistent:yes" 2>&1
    $code = $LASTEXITCODE
    $msg = ($out | Where-Object { $_ -and $_.ToString().Trim() } | Select-Object -Last 1)
    if ($code -eq 0) {
        Log "$d war nicht verbunden -> reconnect OK ($($map[$d]))"
    } else {
        Log "$d reconnect FEHLGESCHLAGEN (Exit $code): $msg"
    }
}
exit 0
