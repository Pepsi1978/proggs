# Jarvis Windows HP: Wecker. Fragt beim Relay /pc/hp-befehl ab und startet die App, wenn das Handy es verlangt oder Post für sie hat.
# Gegenstück zum Faden für /pc/mac-befehl in der Mac-Brücke. Solange dieses Skript fragt, gilt der HP beim Relay als verbunden.
# Start: Aufgabenplanung „JarvisWindowsWecker“ bei der Anmeldung (publish.ps1). Zugang aus ~/SK/Jarvis/relay.properties. Protokoll: %LOCALAPPDATA%\JarvisWindows\wecker.log
$ErrorActionPreference = 'Continue'
$ordner = Join-Path $env:LOCALAPPDATA 'JarvisWindows'
New-Item -ItemType Directory -Force $ordner | Out-Null
$protokoll = Join-Path $ordner 'wecker.log'
# Läuft aus dem Installationsordner (publish.ps1 kopiert es dorthin): So ändert kein git pull ein laufendes Skript.
$exe = Join-Path $PSScriptRoot 'JarvisWindows.exe'
$marke = Join-Path $ordner 'installiert-gerade'

function Schreib([string]$t) {
    try {
        if ((Test-Path $protokoll) -and (Get-Item $protokoll).Length -gt 1MB) { Remove-Item $protokoll -Force }
        Add-Content -Path $protokoll -Value ("{0:dd.MM.yyyy HH:mm:ss} {1}" -f (Get-Date), $t) -Encoding utf8
    } catch {}
}

# Während publish.ps1 kopiert, nicht starten: Die App sperrte sonst ihre Dateien. Der Relay meldet eine liegende Frage bald erneut.
function Wird-Installiert {
    return (Test-Path $marke) -and ((Get-Date) - (Get-Item $marke).LastWriteTime).TotalMinutes -lt 10
}

function Starte-App([bool]$vorn) {
    for ($versuch = 1; $versuch -le 3; $versuch++) {
        try {
            if ($vorn) { Start-Process -FilePath $exe } else { Start-Process -FilePath $exe -ArgumentList '--hintergrund' }
            return
        } catch {
            Schreib "Start gescheitert (Versuch $versuch): $($_.Exception.Message)"
            Start-Sleep -Seconds 2
        }
    }
}

function Lies-Zugang {
    $werte = @{}
    foreach ($z in Get-Content (Join-Path $env:USERPROFILE 'SK\Jarvis\relay.properties')) {
        if ($z -match '^\s*([^#=\s]+)\s*=\s*(.+?)\s*$') { $werte[$Matches[1]] = $Matches[2] }
    }
    return $werte
}

# Einer zur Zeit: Die Aufgabe startet jede Minute neu, falls er nicht mehr läuft.
$sperre = [System.Threading.Mutex]::new($false, 'Local\JarvisWindowsWecker')
try { if (-not $sperre.WaitOne(0)) { exit 0 } } catch [System.Threading.AbandonedMutexException] { }

# Über curl.exe -4 wie die Brücke: Auf diesem Netz hängt Invoke-WebRequest sonst an IPv6 bis zum Zeitlimit, und der HP gälte als nicht verbunden (bugs/desktop/dotnet-httpclient-ipv6-haenger.md).
$curl = Join-Path $env:SystemRoot 'System32\curl.exe'
$antwortDatei = Join-Path $ordner 'wecker-antwort.json'
$kopfDatei = Join-Path $ordner 'wecker-kopf.txt'

Schreib 'Wecker gestartet'
$pause = 2
while ($true) {
    try {
        $z = Lies-Zugang
        # Der Token steht in einer Datei, nicht in der Befehlszeile: Die kann jedes Programm lesen.
        [System.IO.File]::WriteAllText($kopfDatei, "X-Jarvis-Token: $($z.token)`n", [System.Text.UTF8Encoding]::new($false))
        Remove-Item -LiteralPath $antwortDatei -ErrorAction SilentlyContinue
        $code = & $curl -4 -s -S --max-time 40 -H "@$kopfDatei" -o $antwortDatei -w '%{http_code}' "https://$($z.host)/pc/hp-befehl"
        if ($code -eq '204') { $pause = 2; continue }
        if ($code -ne '200') { throw "HTTP $code" }
        $pause = 2
        $befehl = Get-Content -Raw -Encoding utf8 -LiteralPath $antwortDatei | ConvertFrom-Json
        if ($befehl.befehl -eq 'jarvis_starten') {
            Schreib "Start angefordert, vorn=$($befehl.vorn)"
            # Die App ist Einzelinstanz: Läuft sie schon, holt der zweite Start nur das Fenster nach vorn.
            if (Wird-Installiert) { Schreib 'Wird gerade installiert, kein Start.' } else { Starte-App ([bool]$befehl.vorn) }
        }
    } catch {
        Schreib "Fehler: $($_.Exception.Message)"
        Start-Sleep -Seconds $pause
        $pause = [Math]::Min($pause * 2, 30)
    }
}
