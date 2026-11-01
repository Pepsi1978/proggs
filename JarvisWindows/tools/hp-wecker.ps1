# Jarvis Windows HP: Wecker. Fragt beim Relay /pc/hp-befehl ab und startet die App, wenn das Handy es verlangt oder Post für sie hat.
# Gegenstück zum Faden für /pc/mac-befehl in der Mac-Brücke. Solange dieses Skript fragt, gilt der HP beim Relay als verbunden.
# Start: Aufgabenplanung „JarvisWindowsWecker“ bei der Anmeldung (publish.ps1). Zugang aus ~/SK/Jarvis/relay.properties. Protokoll: %LOCALAPPDATA%\JarvisWindows\wecker.log
$ErrorActionPreference = 'Continue'
$ordner = Join-Path $env:LOCALAPPDATA 'JarvisWindows'
New-Item -ItemType Directory -Force $ordner | Out-Null
$protokoll = Join-Path $ordner 'wecker.log'
$exe = Join-Path $env:LOCALAPPDATA 'Programs\JarvisWindowsHP\JarvisWindows.exe'

function Schreib([string]$t) {
    try {
        if ((Test-Path $protokoll) -and (Get-Item $protokoll).Length -gt 1MB) { Remove-Item $protokoll -Force }
        Add-Content -Path $protokoll -Value ("{0:dd.MM.yyyy HH:mm:ss} {1}" -f (Get-Date), $t) -Encoding utf8
    } catch {}
}

function Lies-Zugang {
    $werte = @{}
    foreach ($z in Get-Content (Join-Path $env:USERPROFILE 'SK\Jarvis\relay.properties')) {
        if ($z -match '^\s*([^#=\s]+)\s*=\s*(.+?)\s*$') { $werte[$Matches[1]] = $Matches[2] }
    }
    return $werte
}

Schreib 'Wecker gestartet'
$pause = 2
while ($true) {
    try {
        $z = Lies-Zugang
        $antwort = Invoke-WebRequest -Uri "https://$($z.host)/pc/hp-befehl" -Headers @{ 'X-Jarvis-Token' = $z.token } -TimeoutSec 40 -UseBasicParsing
        $pause = 2
        if ($antwort.StatusCode -eq 200) {
            $befehl = $antwort.Content | ConvertFrom-Json
            if ($befehl.befehl -eq 'jarvis_starten') {
                $argumente = if ($befehl.vorn) { @() } else { @('--hintergrund') }
                Schreib "Start angefordert, vorn=$($befehl.vorn)"
                # Die App ist Einzelinstanz: Läuft sie schon, holt der zweite Start nur das Fenster nach vorn.
                if ($argumente.Count) { Start-Process -FilePath $exe -ArgumentList $argumente } else { Start-Process -FilePath $exe }
            }
        }
    } catch {
        Schreib "Fehler: $($_.Exception.Message)"
        Start-Sleep -Seconds $pause
        $pause = [Math]::Min($pause * 2, 30)
    }
}
