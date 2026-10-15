# Jarvis-PC-Brücke: holt Programmieraufträge vom Jarvis-Relay ab und startet dafür am PC eine Claude-Code-Sitzung
# mit Remote Control. Frank sieht sie in der Claude-App unter Code und kann mitlesen oder Rückfragen beantworten.
#
# Ablauf: Dauerschleife GET https://<Server>/pc/auftrag (Long-Polling, bis 25 s). Solange das Skript fragt, gilt der PC
# beim Relay als da, und Jarvis schickt Aufträge hierher statt in die Cloud. Ist der PC aus, nimmt Jarvis die Cloud.
# Start: Aufgabenplanung „JarvisPcBruecke“ bei der Anmeldung (einrichten.ps1). Zugang aus ~/SK/Jarvis/relay.properties.
#
# Sitzung: Profil Minimal, Schnellmodus, Claude Opus 5.5 (1M), Effort High (Franks Vorgabe vom 15.10.2026), siehe unten.

$ErrorActionPreference = 'Stop'
$Profil = 'minimal'
$Modus = 'schnell'
$Modell = 'claude-opus-5-5[1m]'
$Effort = 'high'
$proggs = Join-Path $env:USERPROFILE 'proggs'
$protokoll = Join-Path $env:LOCALAPPDATA 'JarvisPcBruecke\bruecke.log'
New-Item -ItemType Directory -Force -Path (Split-Path $protokoll) | Out-Null

function Schreib([string]$text) {
    $zeile = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $text"
    try {
        if ((Test-Path $protokoll) -and (Get-Item $protokoll).Length -gt 1MB) { Move-Item $protokoll "$protokoll.alt" -Force }
        Add-Content -Path $protokoll -Value $zeile -Encoding utf8
    } catch { }
}

# Nur eine Brücke je Anmeldung.
$sperre = [System.Threading.Mutex]::new($false, 'Local\JarvisPcBruecke')
if (-not $sperre.WaitOne(0)) { exit 0 }

function Lies-Zugang {
    $datei = Join-Path $env:USERPROFILE 'SK\Jarvis\relay.properties'
    $werte = @{}
    foreach ($zeile in Get-Content $datei) {
        if ($zeile -match '^\s*([^#=]+?)\s*=\s*(.*)$') { $werte[$Matches[1]] = $Matches[2].Trim() }
    }
    if (-not $werte.host -or -not $werte.token) { throw "host oder token fehlt in $datei" }
    return $werte
}

function Finde-Claude {
    $kandidaten = @(
        (Join-Path $env:USERPROFILE '.local\bin\claude.exe'),
        (Join-Path $env:APPDATA 'npm\node_modules\@anthropic-ai\claude-code\bin\claude.exe')
    ) + @(Get-Command claude.exe -All -ErrorAction SilentlyContinue | ForEach-Object Source)
    $treffer = $kandidaten | Where-Object { $_ -and (Test-Path -LiteralPath $_ -PathType Leaf) } | Select-Object -First 1
    if (-not $treffer) { throw 'Keine Claude-Code-Installation gefunden.' }
    return $treffer
}

function Starte-Sitzung($auftrag) {
    $app = ($auftrag.app -replace '[^\p{L}\p{N} ._-]', '').Trim()
    if (-not $app) { $app = 'App' }
    $name = "Jarvis: $app"
    $review = if ($auftrag.review) { 'ja: vor dem Abschluss die Änderung mit /code-review prüfen und Befunde beheben' } else { 'nein' }
    $text = @"
Programmierauftrag von Frank über Jarvis (seinen Sprachassistenten auf dem Handy). Frank verfolgt diese Sitzung per Remote Control in der Claude-App.

APP: $($auftrag.app)
REVIEW: $review
AUFTRAG:
$($auftrag.auftrag)

VORGEHEN
1. Finde die App im Repo anhand des Namens und des Auftrags (Ordnernamen, README.md, CLAUDE.md, settings.gradle.kts, app_name, applicationId). Bekannt: Geniale Aufgaben = Aufgaben, Genialer Wecker = GenialerWeckerAndroid, Geniale Ideen = GenialeIdeen, Entropie Reductor = EntropieReductor, Jarvis = Jarvis, UpdateStation = UpdateStation. Ist die Zuordnung nicht eindeutig, frag Frank mit AskUserQuestion und nenne die Kandidaten, statt zu raten.
2. Setze den Auftrag vollständig um. Ist etwas Wesentliches unklar, frag Frank mit AskUserQuestion; Kleinigkeiten entscheidest du selbst.
3. Schließe nach den Profilregeln vollständig ab: bauen, Version bumpen, committen, pushen, per WLAN aufs Handy installieren und bei Android-Apps den Skill apk-update ausführen. Abschlussmeldung auf Deutsch.
"@
    # Die Brücke läuft tagelang und behielte sonst die Umgebung von ihrem Start: Nach einem JDK-Update zeigte
    # JAVA_HOME in jeder neuen Sitzung auf den gelöschten Ordner, und gradlew brach ab. Darum vor jedem Auftrag
    # JAVA_HOME und PATH frisch aus Windows lesen, wie ein neu gestartetes Programm.
    $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
    if (-not $javaHome) { $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine') }
    if ($javaHome) { $env:JAVA_HOME = $javaHome.TrimEnd('\') }
    $env:PATH = [Environment]::ExpandEnvironmentVariables(
        ([Environment]::GetEnvironmentVariable('PATH', 'Machine').TrimEnd(';') + ';' + [Environment]::GetEnvironmentVariable('PATH', 'User')))
    $stempel = Get-Date -Format 'yyyyMMdd-HHmmss'
    $ordner = Join-Path $env:TEMP 'JarvisPcBruecke'
    New-Item -ItemType Directory -Force -Path $ordner | Out-Null
    $auftragDatei = Join-Path $ordner "auftrag-$stempel.txt"
    Set-Content -Path $auftragDatei -Value $text -Encoding utf8

    # Normalfall: über den OpenLauncher, genau wie der Startknopf (Profil Minimal, Schnellmodus, Opus 5.5, Effort High),
    # dazu Remote Control mit Namen und der Auftrag als erste Nachricht. Ab OpenLauncher 1.24.89 (--jarvis-auftrag).
    $openLauncher = Join-Path $proggs 'OpenLauncher\bin\Release\net8.0-windows10.0.19041.0\win-x64\OpenLauncher.exe'
    $kannJarvis = $false
    if (Test-Path -LiteralPath $openLauncher) {
        try { $kannJarvis = [version](Get-Item -LiteralPath $openLauncher).VersionInfo.ProductVersion.Split('+')[0] -ge [version]'1.24.89' } catch { }
    }
    if ($kannJarvis) {
        $argumente = @('--jarvis-auftrag', "`"$auftragDatei`"", '--name', "`"$name`"", '--profil', $Profil, '--modus', $Modus, '--modell', "`"$Modell`"", '--effort', $Effort)
        # Mit der Nummer bekommt die Sitzung die Jarvis-Hooks: Fortschritt und Rückfragen erscheinen im Jarvis-Chat.
        if ("$($auftrag.nummer)" -match '^[A-Za-z0-9]{1,20}$') { $argumente += @('--auftrag-id', $auftrag.nummer) }
        $prozess = Start-Process -FilePath $openLauncher -ArgumentList $argumente -PassThru -WindowStyle Hidden
        # Spätestens nach 30 s quittieren: Der Relay wartet 45 s, danach wüsste das Handy nicht, ob die Sitzung läuft.
        if (-not $prozess.WaitForExit(30000)) { throw 'Der OpenLauncher hat nach 30 Sekunden nicht geantwortet.' }
        if ($prozess.ExitCode -ne 0) { throw "Der OpenLauncher konnte die Sitzung nicht starten (Code $($prozess.ExitCode), siehe OpenLauncher-Log)." }
        return $name
    }

    # Notlösung für einen älteren OpenLauncher: direkt starten (Profil-Ordner, Modell und Effort wie oben; die aktive
    # CLAUDE.md und damit der Modus bleiben dann so, wie der OpenLauncher sie zuletzt geschrieben hat).
    Schreib 'OpenLauncher ohne --jarvis-auftrag, starte direkt'
    $configDir = Join-Path $proggs "OpenLauncher\Profiles\ClaudeCode\$Profil"
    $claude = Finde-Claude
    $start = Join-Path $ordner "start-$stempel.ps1"
    @"
`$Host.UI.RawUI.WindowTitle = '$($name -replace "'", "''")'
Set-Location -LiteralPath '$proggs'
`$env:CLAUDE_CONFIG_DIR = '$configDir'
`$sync = Join-Path `$env:USERPROFILE 'proggs\OpenLauncher\Profiles\hooks\claude-login-sync.py'
if (Test-Path `$sync) { try { `$null = python `$sync 2>`$null } catch { } }
`$auftrag = Get-Content -Raw -Encoding utf8 -LiteralPath '$auftragDatei'
& '$claude' --dangerously-skip-permissions --model '$Modell' --effort '$Effort' --remote-control '$($name -replace "'", "''")' `$auftrag
"@ | Set-Content -Path $start -Encoding utf8

    $pwsh = (Get-Command pwsh.exe -ErrorAction SilentlyContinue).Source
    if (-not $pwsh) { $pwsh = 'powershell.exe' }
    $wt = Join-Path $env:LOCALAPPDATA 'Microsoft\WindowsApps\wt.exe'
    if (Test-Path $wt) {
        Start-Process -FilePath $wt -ArgumentList @('-w', 'new', '--title', "`"$name`"", '-d', "`"$proggs`"", $pwsh, '-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$start`"")
    } else {
        Start-Process -FilePath $pwsh -WorkingDirectory $proggs -ArgumentList @('-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$start`"")
    }
    return $name
}

# Abfragen über curl.exe -4: Auf diesem Netz bekommt Windows IPv6-Adressen, leitet IPv6 aber zeitweise nicht weiter.
# Invoke-WebRequest versucht IPv6 zuerst und hängt dann bis zum Zeitlimit (bugs/desktop/dotnet-httpclient-ipv6-haenger.md).
$curl = Join-Path $env:SystemRoot 'System32\curl.exe'
$antwortDatei = Join-Path $env:TEMP 'JarvisPcBruecke\antwort.json'
$quittungDatei = Join-Path $env:TEMP 'JarvisPcBruecke\quittung.json'
New-Item -ItemType Directory -Force -Path (Split-Path $antwortDatei) | Out-Null

function Rufe([string]$adresse, [string]$token, [int]$sekunden, [string]$rumpfDatei) {
    $argumente = @('-4', '-s', '-S', '--max-time', $sekunden, '-o', $antwortDatei, '-w', '%{http_code}',
        '-H', "X-Jarvis-Token: $token", '-H', "X-Jarvis-Rechner: $env:COMPUTERNAME")
    if ($rumpfDatei) { $argumente += @('-X', 'POST', '-H', 'Content-Type: application/json; charset=utf-8', '--data-binary', "@$rumpfDatei") }
    Remove-Item -LiteralPath $antwortDatei -ErrorAction SilentlyContinue
    $code = & $curl @argumente $adresse 2>&1
    if ($LASTEXITCODE -ne 0) { throw "curl $LASTEXITCODE`: $code" }
    return [int]("$code".Trim())
}

Schreib 'Brücke gestartet'
$pause = 2
while ($true) {
    try {
        $zugang = Lies-Zugang
        $code = Rufe "https://$($zugang.host)/pc/auftrag" $zugang.token 40 $null
        $pause = 2
        if ($code -eq 204) { continue }
        if ($code -ne 200) { throw "HTTP $code" }
        $auftrag = Get-Content -Raw -Encoding utf8 -LiteralPath $antwortDatei | ConvertFrom-Json
        Schreib "Auftrag für $($auftrag.app)"
        try {
            $name = Starte-Sitzung $auftrag
            $quittung = @{ id = $auftrag.id; ok = $true; text = "Sitzung '$name' gestartet" }
            Schreib "Sitzung '$name' gestartet"
        } catch {
            $quittung = @{ id = $auftrag.id; ok = $false; text = "Start fehlgeschlagen: $($_.Exception.Message)" }
            Schreib $quittung.text
        }
        [System.IO.File]::WriteAllText($quittungDatei, ($quittung | ConvertTo-Json -Compress), [System.Text.UTF8Encoding]::new($false))
        $null = Rufe "https://$($zugang.host)/pc/quittung" $zugang.token 20 $quittungDatei
    } catch {
        Schreib "Fehler: $($_.Exception.Message)"
        Start-Sleep -Seconds $pause
        $pause = [Math]::Min($pause * 2, 10)
    }
}
