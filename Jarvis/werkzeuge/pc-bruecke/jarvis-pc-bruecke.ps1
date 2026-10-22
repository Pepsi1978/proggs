# Jarvis-PC-Brücke: holt Programmieraufträge vom Jarvis-Relay ab und startet dafür am PC eine Claude-Code-Sitzung
# mit Remote Control. Frank sieht sie in der Claude-App unter Code und kann mitlesen oder Rückfragen beantworten.
#
# Ablauf: Dauerschleife GET https://<Server>/pc/auftrag (Long-Polling, bis 25 s). Solange das Skript fragt, gilt der PC
# beim Relay als da, und Jarvis schickt Aufträge hierher statt in die Cloud. Ist der PC aus, nimmt Jarvis die Cloud.
# Start: Aufgabenplanung „JarvisPcBruecke“ bei der Anmeldung (einrichten.ps1). Zugang aus ~/SK/Jarvis/relay.properties.
#
# Sitzung: Profil Minimal, Schnellmodus, Claude Opus 5.5 (1M), Effort High (Franks Vorgabe vom 15.10.2026), siehe unten.
# Anmeldung: Jede Abfrage trägt den lokalen Anmeldestand von Claude Code (X-Jarvis-Anmeldung: lokal, abgemeldet, unbekannt,
# dazu die Prüfzeit), wie die Mac-Brücke: `claude auth status` im Profilordner, höchstens alle fünf Minuten und frisch vor
# jedem Start, nur „loggedIn“. Abgemeldet startet keine Sitzung (Quittung mit anmeldung_fehlt, das Handy nimmt nicht die Cloud).

$ErrorActionPreference = 'Stop'
$Profil = 'minimal'
$Modus = 'schnell'
$Modell = 'claude-opus-5-5[1m]'
# Wählt Frank beim Start „Sol 6.1“: Codex CLI über den OpenLauncher (Franks Vorgabe vom 22.10.2026), Profil, Modus und Effort wie oben.
$ModellSol = 'gpt-6.1-sol'
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

# Aufträge, Startskripte und Hook-Zustände bleiben sonst für immer liegen, samt dem vollen Auftragstext in %TEMP%.
# 14 Tage: Kein Warter lebt länger als 12 h, und so alte Sitzungen laufen nicht mehr.
function Raeume-Auf {
    $grenze = (Get-Date).AddDays(-14)
    $muster = @(
        @{ Ordner = (Join-Path $env:TEMP 'JarvisPcBruecke'); Namen = @('auftrag-*.txt', 'start-*.ps1', 'settings-*.json') },
        @{ Ordner = (Split-Path $protokoll); Namen = @('hook-*.json', 'hook-*.json.*.tmp', 'hook-*.sperre', 'hook-*.lock', 'puls-*.txt', 'warter-*.pid') }
    )
    foreach ($m in $muster) {
        foreach ($name in $m.Namen) {
            Get-ChildItem -LiteralPath $m.Ordner -Filter $name -File -ErrorAction SilentlyContinue |
                Where-Object { $_.LastWriteTime -lt $grenze } | Remove-Item -Force -ErrorAction SilentlyContinue
        }
    }
}

# Der OpenLauncher-Ausgabeordner hängt am Zielframework (net8.0-windows…): suchen statt fest verdrahten, sonst landete
# nach einem Framework-Wechsel jeder Auftrag im Notweg.
function Finde-OpenLauncher {
    $wurzel = Join-Path $proggs 'OpenLauncher\bin\Release'
    Get-ChildItem -LiteralPath $wurzel -Filter 'OpenLauncher.exe' -File -Recurse -Depth 3 -ErrorAction SilentlyContinue |
        Where-Object { $_.Directory.Name -eq 'win-x64' } | Sort-Object LastWriteTime -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}

# Dieselben Hooks wie OpenLauncherService.JarvisHooks(), nur für den Notweg: Ohne sie sähe Jarvis von der Sitzung nichts.
function Jarvis-Hooks {
    $skript = (Join-Path $proggs 'Jarvis\werkzeuge\pc-bruecke\jarvis_hook.py').Replace('\', '/')
    $befehl = "python `"$skript`""
    function Eintrag($matcher, $art, $timeout, [bool]$hintergrund = $false) {
        $hook = [ordered]@{ type = 'command'; command = "$befehl $art"; timeout = $timeout }
        if ($hintergrund) { $hook.async = $true }
        $e = [ordered]@{ hooks = @($hook) }
        if ($null -ne $matcher) { $e.matcher = $matcher }
        return $e
    }
    $warter = [ordered]@{ hooks = @([ordered]@{ type = 'command'; command = "$befehl warte"; timeout = 43200;
                asyncRewake = $true; rewakeSummary = 'Nachtrag von Frank über Jarvis' }) }
    return [ordered]@{
        PostToolUse        = @(Eintrag '*' 'post' 15 $true)
        PostToolUseFailure = @(Eintrag 'Bash|PowerShell' 'post' 15 $true)
        PreToolUse         = @((Eintrag 'AskUserQuestion' 'frage' 660), (Eintrag '*' 'puls' 10 $true))
        Stop               = @((Eintrag $null 'stop' 660), $warter)
        StopFailure        = @(Eintrag $null 'fehlschlag' 30)
        SessionStart       = @($warter)
    }
}

$anmeldung = @{ stand = 'unbekannt'; zeit = 0 }
$authProzess = $null
$authDatei = Join-Path $env:TEMP 'JarvisPcBruecke\auth-status.json'

# claude auth status als eigener Prozess mit Ausgabe in eine Datei: mit Zeitlimit, und Text auf stderr bricht unter
# Windows PowerShell 5.1 nichts ab. Der Profilordner kommt über die geerbte Umgebung.
function Starte-AuthStatus {
    $claude = Finde-Claude
    $alt = $env:CLAUDE_CONFIG_DIR
    $env:CLAUDE_CONFIG_DIR = Join-Path $proggs "OpenLauncher\Profiles\ClaudeCode\$Profil"
    try {
        Remove-Item -LiteralPath $authDatei -ErrorAction SilentlyContinue
        return Start-Process -FilePath $claude -ArgumentList @('auth', 'status', '--json') -RedirectStandardOutput $authDatei `
            -RedirectStandardError "$authDatei.err" -WindowStyle Hidden -PassThru
    } finally { $env:CLAUDE_CONFIG_DIR = $alt }
}

function Lies-AuthStatus {
    try {
        $angemeldet = (Get-Content -Raw -Encoding utf8 -LiteralPath $authDatei | ConvertFrom-Json).loggedIn
        if ($angemeldet -eq $true) { return 'lokal' }
        if ($angemeldet -eq $false) { return 'abgemeldet' }
    } catch { }
    return 'unbekannt'
}

function Setze-Anmeldung([string]$stand) {
    if ($stand -ne $script:anmeldung.stand) { Schreib "Anmeldung: $stand" }
    $script:anmeldung = @{ stand = $stand; zeit = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() }
}

function Beende-Leise($prozess) {
    try { $prozess.Kill() } catch { }
}

# Regelmäßig (alle fünf Minuten) im Hintergrund: Die Abfrageschleife wartet nie darauf, sonst gäbe es eine Lücke ohne
# offene Abfrage, in der der Mac einen Auftrag des Hauptrechners übernähme.
function Pruefe-Anmeldung-Hintergrund {
    try {
        if ($script:authProzess) {
            if ($script:authProzess.HasExited) { Setze-Anmeldung (Lies-AuthStatus); $script:authProzess = $null }
            elseif (((Get-Date) - $script:authProzess.StartTime).TotalSeconds -gt 20) {
                Beende-Leise $script:authProzess
                $script:authProzess = $null; Setze-Anmeldung 'unbekannt'
            }
            return
        }
        if ([DateTimeOffset]::UtcNow.ToUnixTimeSeconds() - $script:anmeldung.zeit -ge 300) { $script:authProzess = Starte-AuthStatus }
    } catch { Schreib "Anmeldung nicht prüfbar: $($_.Exception.GetType().Name)"; Setze-Anmeldung 'unbekannt' }
}

# Frisch vor jedem Start, je Aufruf höchstens 10 s; abgemeldet: erst der Login-Abgleich (höchstens 15 s), dann noch einmal.
function Pruefe-AuthEinmal {
    if ($script:authProzess -and -not $script:authProzess.HasExited) { Beende-Leise $script:authProzess }
    $script:authProzess = $null
    $p = Starte-AuthStatus
    if (-not $p.WaitForExit(10000)) { Beende-Leise $p; return 'unbekannt' }
    return Lies-AuthStatus
}

function Pruefe-Anmeldung-Jetzt {
    $stand = 'unbekannt'
    try {
        $stand = Pruefe-AuthEinmal
        $sync = Join-Path $proggs 'OpenLauncher\Profiles\hooks\claude-login-sync.py'
        if ($stand -eq 'abgemeldet' -and (Test-Path -LiteralPath $sync)) {
            $py = Start-Process -FilePath 'python' -ArgumentList @("`"$sync`"") -WindowStyle Hidden -PassThru
            if (-not $py.WaitForExit(15000)) { Beende-Leise $py }
            $stand = Pruefe-AuthEinmal
        }
    } catch { Schreib "Anmeldung nicht prüfbar: $($_.Exception.GetType().Name)"; $stand = 'unbekannt' }
    Setze-Anmeldung $stand
    return $stand
}

function Starte-Sitzung($auftrag) {
    $app = ($auftrag.app -replace '[^\p{L}\p{N} ._-]', '').Trim()
    if (-not $app) { $app = 'App' }
    $name = "Jarvis: $app"
    # Franks Wahl beim Start: opus (Vorgabe, Claude Code wie bisher) oder sol (Codex CLI mit Sol 6.1, Minimal, Schnellmodus, Effort High).
    $sol = "$($auftrag.modell)" -eq 'sol'
    # Sol 6.1 läuft im Codex CLI (Vorgabe) oder im OpenCode CLI (Franks Wahl per Knopf in Jarvis).
    $openCode = $sol -and "$($auftrag.cli)" -eq 'opencode'
    $codex = $sol -and -not $openCode
    # Opus und OpenCode fragen am Ende selbst nach dem Review: Der Kopf nennt es dann nicht (Franks Wahl vom 22.10.2026).
    $reviewZeile = ''
    $kopf = 'Programmierauftrag von Frank über Jarvis (Sprachassistent auf seinem Handy); Frank verfolgt die Sitzung per Remote Control.'
    $unklar1 = 'Nicht eindeutig: Frank mit AskUserQuestion fragen und die Kandidaten nennen.'
    $unklar2 = '2. Wesentliche Unklarheiten mit AskUserQuestion klären, Kleinigkeiten selbst entscheiden.'
    if ($openCode) {
        # OpenCode: Rückfragen über das Werkzeug question; das Jarvis-Plugin bringt sie aufs Handy und die Antwort zurück.
        $kopf = 'Programmierauftrag von Frank über Jarvis (Sprachassistent auf seinem Handy); Frank verfolgt die Schritte in Jarvis.'
        $unklar1 = 'Nicht eindeutig: Frank mit dem Werkzeug question fragen und die Kandidaten nennen.'
        $unklar2 = '2. Wesentliche Unklarheiten mit dem Werkzeug question klären, Kleinigkeiten selbst entscheiden.'
    }
    if ($codex) {
        # Codex CLI: keine Rückfragen und kein /code-review, und niemand antwortet im Terminal. Das Review hat Frank in Jarvis gewählt.
        $unklar1 = 'Nicht eindeutig: die wahrscheinlichste App nehmen und das in der ersten Meldung nennen.'
        $unklar2 = '2. Niemand antwortet in dieser Sitzung: Unklarheiten selbst sinnvoll entscheiden und in der Meldung nennen, nie auf eine Antwort warten.'
        $reviewZeile = if ($auftrag.review) { "REVIEW: Ja (vor dem Abschluss den gesamten Diff gründlich selbst auf Fehler prüfen und Befunde beheben)`r`n" } else { "REVIEW: Nein`r`n" }
        $hook = (Join-Path $proggs 'Jarvis\werkzeuge\pc-bruecke\jarvis_hook.py').Replace('\', '/')
        $kopf = "Programmierauftrag von Frank über Jarvis (Sprachassistent auf seinem Handy); Frank verfolgt die Schritte in Jarvis.`r`n" +
            "MELDEN (statt programmier_fortschritt, das es hier nicht gibt): python `"$hook`" melde <laeuft|fertig|fehler> `"<ein Satz>`""
    }
    $text = @"
$kopf

APP: $($auftrag.app)
$($reviewZeile)AUFTRAG:
$($auftrag.auftrag)

VORGEHEN
1. App im Repo finden. Bekannt: Geniale Aufgaben = Aufgaben, Genialer Wecker = GenialerWeckerAndroid, Geniale Ideen = GenialeIdeen, Entropie Reductor = EntropieReductor, Jarvis = Jarvis, UpdateStation = UpdateStation. $unklar1
$unklar2
3. Nach den Profilregeln vollständig abschließen: bauen, Version, Commit, Push, Installation per WLAN, bei Android-Apps Skill apk-update. Abschlussmeldung auf Deutsch.
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
    Raeume-Auf
    $auftragDatei = Join-Path $ordner "auftrag-$stempel.txt"
    Set-Content -Path $auftragDatei -Value $text -Encoding utf8
    $nummer = "$($auftrag.nummer)"
    $mitHooks = $nummer -match '^[A-Za-z0-9]{1,20}$'

    # Normalfall: über den OpenLauncher, genau wie der Startknopf (Profil Minimal, Schnellmodus, Opus 5.5, Effort High),
    # dazu Remote Control mit Namen und der Auftrag als erste Nachricht. Ab OpenLauncher 1.24.89 (--jarvis-auftrag).
    $openLauncher = Finde-OpenLauncher
    $kannJarvis = $false
    # Sol (Codex CLI) erst ab OpenLauncher 1.24.98 (--cli codex); einen Notweg ohne OpenLauncher gibt es dafür nicht.
    # OpenCode mit Jarvis-Auftrag erst ab 1.24.99 (--cli opencode).
    $ab = if ($openCode) { '1.24.99' } elseif ($sol) { '1.24.98' } else { '1.24.89' }
    if ($openLauncher -and (Test-Path -LiteralPath $openLauncher)) {
        try { $kannJarvis = [version](Get-Item -LiteralPath $openLauncher).VersionInfo.ProductVersion.Split('+')[0] -ge [version]$ab } catch { }
    }
    if ($sol -and -not $kannJarvis) { throw "Sol 6.1 braucht OpenLauncher ab $ab ($(if ($openCode) { 'OpenCode CLI' } else { 'Codex CLI' })); bitte den OpenLauncher aktualisieren." }
    if ($kannJarvis) {
        $argumente = @('--jarvis-auftrag', "`"$auftragDatei`"", '--name', "`"$name`"", '--profil', $Profil, '--modus', $Modus, '--modell', "`"$Modell`"", '--effort', $Effort)
        if ($sol) {
            $argumente = @('--jarvis-auftrag', "`"$auftragDatei`"", '--name', "`"$name (Sol 6.1$(if ($openCode) { ', OpenCode' }))`"", '--profil', $Profil, '--modus', $Modus,
                '--cli', $(if ($openCode) { 'opencode' } else { 'codex' }), '--modell', $ModellSol, '--effort', $Effort)
        }
        # Mit der Nummer bekommt die Sitzung die Jarvis-Hooks: Fortschritt und Rückfragen erscheinen im Jarvis-Chat.
        if ($mitHooks) { $argumente += @('--auftrag-id', $nummer) }
        $prozess = Start-Process -FilePath $openLauncher -ArgumentList $argumente -PassThru -WindowStyle Hidden
        # Spätestens nach 30 s quittieren: Der Relay wartet 45 s, danach wüsste das Handy nicht, ob die Sitzung läuft.
        # Kein Fehler melden, wenn der OpenLauncher nur langsam ist: Er startet die Sitzung womöglich noch. Mit ok=false
        # spränge das Handy auf die Cloud aus, und zwei Sitzungen arbeiteten am selben Repo.
        if (-not $prozess.WaitForExit(30000)) {
            Schreib 'OpenLauncher nach 30 s noch nicht fertig, Start unbestätigt'
            return "$name (Start unbestätigt: der OpenLauncher war nach 30 Sekunden noch nicht fertig)"
        }
        if ($prozess.ExitCode -ne 0) { throw "Der OpenLauncher konnte die Sitzung nicht starten (Code $($prozess.ExitCode), siehe OpenLauncher-Log)." }
        return $name
    }

    # Notlösung für einen älteren OpenLauncher: direkt starten (Profil-Ordner, Modell und Effort wie oben; die aktive
    # CLAUDE.md und damit der Modus bleiben dann so, wie der OpenLauncher sie zuletzt geschrieben hat). Mit der Nummer
    # bekommt auch diese Sitzung die Jarvis-Hooks und JARVIS_AUFTRAG_ID, wie am Mac.
    Schreib 'OpenLauncher ohne --jarvis-auftrag, starte direkt'
    $configDir = Join-Path $proggs "OpenLauncher\Profiles\ClaudeCode\$Profil"
    $claude = Finde-Claude
    $einstellungen = Join-Path $ordner "settings-$stempel.json"
    $daten = [ordered]@{ model = $Modell; effortLevel = $Effort }
    if ($mitHooks) { $daten.hooks = Jarvis-Hooks }
    [System.IO.File]::WriteAllText($einstellungen, ($daten | ConvertTo-Json -Depth 10 -Compress), [System.Text.UTF8Encoding]::new($false))
    $auftragId = if ($mitHooks) { "`$env:JARVIS_AUFTRAG_ID = '$nummer'" } else { '' }
    $start = Join-Path $ordner "start-$stempel.ps1"
    @"
`$Host.UI.RawUI.WindowTitle = '$($name -replace "'", "''")'
Set-Location -LiteralPath '$($proggs -replace "'", "''")'
`$env:CLAUDE_CONFIG_DIR = '$($configDir -replace "'", "''")'
$auftragId
`$sync = Join-Path `$env:USERPROFILE 'proggs\OpenLauncher\Profiles\hooks\claude-login-sync.py'
if (Test-Path `$sync) { try { `$null = python `$sync 2>`$null } catch { } }
`$auftrag = Get-Content -Raw -Encoding utf8 -LiteralPath '$($auftragDatei -replace "'", "''")'
& '$($claude -replace "'", "''")' --dangerously-skip-permissions --settings '$($einstellungen -replace "'", "''")' --model '$Modell' --effort '$Effort' --remote-control '$($name -replace "'", "''")' `$auftrag
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

# Der Token gehört nicht in die Befehlszeile von curl: Die sieht jede Prozessliste und eine Prozess-Überwachung
# (Ereignis 4688). curl liest die Kopfzeilen darum aus einer Datei, die nur der eigene Benutzer lesen darf.
$kopfDatei = Join-Path (Split-Path $protokoll) 'kopf.txt'
$kopfToken = $null

function Schreib-Kopf([string]$token) {
    if ($script:kopfToken -eq $token -and (Test-Path -LiteralPath $kopfDatei)) { return }
    if (-not (Test-Path -LiteralPath $kopfDatei)) {
        # Zuerst leer anlegen und abschließen, erst dann den Token hineinschreiben.
        [System.IO.File]::WriteAllText($kopfDatei, '', [System.Text.UTF8Encoding]::new($false))
    }
    & icacls.exe $kopfDatei /inheritance:r /grant:r "$($env:USERNAME):(R,W,D)" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Rechte der Kopfzeilen-Datei ließen sich nicht einschränken.' }
    [System.IO.File]::WriteAllText($kopfDatei, "X-Jarvis-Token: $token`r`nX-Jarvis-Rechner: $env:COMPUTERNAME`r`n", [System.Text.UTF8Encoding]::new($false))
    $script:kopfToken = $token
}

function Rufe([string]$adresse, [string]$token, [int]$sekunden, [string]$rumpfDatei) {
    Schreib-Kopf $token
    $argumente = @('-4', '-s', '-S', '--max-time', $sekunden, '-o', $antwortDatei, '-w', '%{http_code}',
        '-H', "@$kopfDatei", '-H', "X-Jarvis-Anmeldung: $($script:anmeldung.stand)", '-H', "X-Jarvis-Anmeldung-Zeit: $($script:anmeldung.zeit)")
    if ($rumpfDatei) { $argumente += @('-X', 'POST', '-H', 'Content-Type: application/json; charset=utf-8', '--data-binary', "@$rumpfDatei") }
    Remove-Item -LiteralPath $antwortDatei -ErrorAction SilentlyContinue
    $code = & $curl @argumente $adresse 2>&1
    if ($LASTEXITCODE -ne 0) { throw "curl $LASTEXITCODE`: $code" }
    return [int]("$code".Trim())
}

Schreib 'Brücke gestartet'
Raeume-Auf
$pause = 2
while ($true) {
    try {
        $zugang = Lies-Zugang
        Pruefe-Anmeldung-Hintergrund
        $code = Rufe "https://$($zugang.host)/pc/auftrag" $zugang.token 40 $null
        $pause = 2
        if ($code -eq 204) { continue }
        if ($code -ne 200) { throw "HTTP $code" }
        $auftrag = Get-Content -Raw -Encoding utf8 -LiteralPath $antwortDatei | ConvertFrom-Json
        Schreib "Auftrag für $($auftrag.app)"
        try {
            # Ohne Anmeldung keine Sitzung: Sie bliebe sofort bei „Please run /login“ stehen.
            # Sol läuft im Codex oder OpenCode CLI: Die Claude-Anmeldung zählt dort nicht.
            if ("$($auftrag.modell)" -ne 'sol' -and (Pruefe-Anmeldung-Jetzt) -eq 'abgemeldet') {
                $quittung = @{ id = $auftrag.id; ok = $false; anmeldung_fehlt = $true
                    text = "Claude Code auf $env:COMPUTERNAME ist nicht angemeldet. Claude Code auf dem Rechner öffnen und /login ausführen, danach den Auftrag neu senden." }
                Schreib 'Nicht gestartet: nicht angemeldet'
            } else {
                $name = Starte-Sitzung $auftrag
                $quittung = @{ id = $auftrag.id; ok = $true; text = "Sitzung '$name' gestartet" }
                Schreib "Sitzung '$name' gestartet"
            }
        } catch {
            $quittung = @{ id = $auftrag.id; ok = $false; text = "Start fehlgeschlagen: $($_.Exception.Message)" }
            Schreib $quittung.text
        }
        [System.IO.File]::WriteAllText($quittungDatei, ($quittung | ConvertTo-Json -Compress), [System.Text.UTF8Encoding]::new($false))
        # Ohne Quittung kennt das Handy die Sitzung nicht und verwirft ihre Meldungen: kurz wiederholen.
        for ($versuch = 1; $versuch -le 3; $versuch++) {
            try {
                $quittungCode = Rufe "https://$($zugang.host)/pc/quittung" $zugang.token 20 $quittungDatei
                # curl meldet HTTP-Fehler nur als Code (etwa 502 beim Neustart des Relays): auch das ist ein Fehlversuch.
                if ($quittungCode -ge 300) { throw "HTTP $quittungCode" }
                break
            }
            catch { Schreib "Quittung, Versuch $versuch`: $($_.Exception.Message)"; if ($versuch -lt 3) { Start-Sleep -Seconds 2 } }
        }
    } catch {
        Schreib "Fehler: $($_.Exception.Message)"
        Start-Sleep -Seconds $pause
        $pause = [Math]::Min($pause * 2, 10)
    }
}
