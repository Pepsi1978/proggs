# statusline.ps1 — Schoene Statusline mit Icons + Fortschrittsbalken
# Einzeilig (Frank 2026-05-31, frueher zweizeilig):
#   Context | Modell | Effort | 5h-Balken | 5h-Pacing | 7d-Balken | 7d-Pacing | Ordner | Zeit
#   (Context ganz vorne — Frank 2026-06-07)
# Cross-Platform-Pendant zu statusline.sh
$ErrorActionPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$input_raw = [Console]::In.ReadToEnd()
$obj = $null
if ($input_raw) {
    try { $obj = $input_raw | ConvertFrom-Json } catch { $obj = $null }
}

# Effort: ZUERST aus Stdin (.effort.level) — das ist der LIVE-Session-Wert,
# der von /effort low/medium/high/xhigh aktualisiert wird. settings.json haelt
# nur den Default fuer den Session-Start. Frueher hat die Statusline NUR aus
# settings.json gelesen und deshalb "HIGH" angezeigt obwohl die Session auf
# "xhigh" stand (Frank-Bug-Report 2026-05-09 Abend).
$effort = $null
if ($obj -and $obj.effort -and $obj.effort.level) {
    $effort = [string]$obj.effort.level
}
if (-not $effort) {
    try {
        $settingsPath = Join-Path $env:USERPROFILE '.claude\settings.json'
        $settings = Get-Content -Raw -Encoding UTF8 -Path $settingsPath | ConvertFrom-Json
        if ($settings.effortLevel) { $effort = [string]$settings.effortLevel }
    } catch { }
}
if (-not $effort) { $effort = '?' }
$effort_upper = $effort.ToUpper()

# Felder
$model = '?'
$cwd = ''
$ctx_remaining = $null
$five_h_used = $null
$five_h_resets = 0
$week_used = $null
$week_resets = 0
$transcript_path = ''

if ($obj) {
    # Modell-Fallback: display_name → id → "?" (CLI liefert manchmal nur id)
    if ($obj.model.display_name) {
        $model = [string]$obj.model.display_name
    } elseif ($obj.model.id) {
        $model = [string]$obj.model.id
    }
    if ($obj.workspace.current_dir) {
        $cwd = $obj.workspace.current_dir
        $homeDir = $env:USERPROFILE
        if ($homeDir -and $cwd.StartsWith($homeDir)) {
            $cwd = ('~' + $cwd.Substring($homeDir.Length)) -replace '\\', '/'
        }
        # Smart-truncate fuer den Ordner-Pfad (Variante B):
        # - <= 35 Zeichen: unveraendert
        # - > 35 Zeichen: ~/proggs/<Projekt>/…/<vorletztes>/<letztes>
        $maxLen = 35
        if ($cwd.Length -gt $maxLen) {
            $segs = $cwd.Split('/')
            $n = $segs.Length
            if ($n -ge 5) {
                if ($segs[0] -eq '~' -and $segs[1] -eq 'proggs' -and $n -ge 5) {
                    $cwd = "~/proggs/$($segs[2])/…/$($segs[$n-2])/$($segs[$n-1])"
                } else {
                    $cwd = "$($segs[0])/$($segs[1])/…/$($segs[$n-2])/$($segs[$n-1])"
                }
            }
        }
    }
    if ($obj.context_window.remaining_percentage -ne $null) {
        $ctx_remaining = [int][Math]::Round($obj.context_window.remaining_percentage)
    }
    if ($obj.context_window.used_percentage -ne $null) {
        $ctx_used_pct = [int][Math]::Round($obj.context_window.used_percentage)
    }
    if ($obj.rate_limits.five_hour.used_percentage -ne $null) {
        $five_h_used = [int][Math]::Round($obj.rate_limits.five_hour.used_percentage)
    }
    if ($obj.rate_limits.five_hour.resets_at) {
        $five_h_resets = [long]$obj.rate_limits.five_hour.resets_at
    }
    if ($obj.rate_limits.seven_day.used_percentage -ne $null) {
        $week_used = [int][Math]::Round($obj.rate_limits.seven_day.used_percentage)
    }
    if ($obj.rate_limits.seven_day.resets_at) {
        $week_resets = [long]$obj.rate_limits.seven_day.resets_at
    }
    if ($obj.transcript_path) {
        $transcript_path = [string]$obj.transcript_path
    }
}

# Modellname kuerzen: "(1M context)" -> "(1M)" — spart Platz in der Leiste (Frank 2026-05-24).
# PS -replace ist case-insensitiv, deckt context/Context/Kontext ab.
$model = $model -replace ' (context|kontext)\)', ')'

# Cross-Session-State-Sharing fuer rate_limits (Frank-Bug-Report 2026-05-09 Abend):
# Jede Claude-Code-Session sieht nur ihre EIGENE letzte API-Antwort. Eine idle
# Session zeigt deshalb veraltete Werte. Loesung: alle Sessions schreiben in
# ~/.claude/state/rate-limits-<session_id>.json. Beim Anzeigen wird der HOECHSTE
# Wert ueber alle Sessions genommen (rate_limits zaehlen im Fenster nur hoch).
# Beim Reset (resets_at aendert sich) wird MAX nur innerhalb des aktuellsten
# resets_at-Fensters gemacht — sonst wuerden alte hohe Werte aus dem letzten
# Fenster die neuen niedrigen Werte verdecken. Identische Logik zu statusline.sh.
#
# ROBUSTHEITS-FIX (Frank-Bug-Report 2026-05-09 22:24): Statusline zeigte 1778368200%
# bei 5h und 309% bei 7d. Root Cause: Eine State-Datei rate-limits-.json (mit LEERER
# session_id) hatte verschobene Felder — five_h enthielt einen Unix-Timestamp,
# seven_d eine UUID. Die MAX-Logik nahm diese Muellwerte ohne Plausibilitaetspruefung.
# Fix nach Direktive 3 (Defense in Depth, Poka-Yoke Stufe 3):
#   1. SCHREIB-GUARDS: kein Schreiben bei leerer session_id, kein Schreiben wenn
#      five_h/seven_d ausserhalb 0..100, kein Schreiben wenn five_h_resets ein
#      unplausibler Timestamp ist (muss zwischen now-1h und now+6h liegen)
#   2. LESE-VALIDIERUNG: ungueltige JSON-Files werden GELOESCHT (nicht nur
#      ignoriert), Werte ausserhalb 0..100 werden geclamped, Eintraege mit
#      leerer/ungueltiger session_id werden geloescht
#   3. ATOMIC WRITE: erst Temp-Datei, dann Move-Item — so kann die State-Datei
#      nicht halb-geschrieben gelesen werden
function Test-ValidPercent($v) {
    if ($v -eq $null) { return $false }
    try {
        $n = [double]$v
        return ($n -ge 0 -and $n -le 100)
    } catch { return $false }
}

function Test-ValidResetTs($ts, $now) {
    if ($ts -eq $null) { return $false }
    try {
        $n = [long]$ts
        # Plausibel: zwischen "vor 1h" (Server-Clock-Drift) und "in 6h" (5h-Fenster + 1h Toleranz)
        return ($n -ge ($now - 3600) -and $n -le ($now + 21600))
    } catch { return $false }
}

function Test-ValidResetTs7d($ts, $now) {
    if ($ts -eq $null) { return $false }
    try {
        $n = [long]$ts
        # 7d-Fenster: zwischen "vor 1h" und "in 8 Tagen" (7d + 1 Tag Toleranz)
        return ($n -ge ($now - 3600) -and $n -le ($now + 691200))
    } catch { return $false }
}

try {
    $sessionId = if ($obj -and $obj.session_id) { [string]$obj.session_id } else { '' }
    $stateDir = Join-Path $env:USERPROFILE '.claude\state'
    if (-not (Test-Path $stateDir)) { New-Item -ItemType Directory -Path $stateDir -Force | Out-Null }
    $nowTs = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

    # Account-Fingerprint (Frank-Bug-Report 2026-05-10): Nach Account-Wechsel
    # (Logout/Login mit anderem Account) zeigte die Statusline noch die rate_limits
    # vom alten Account, weil alte State-Files mit hohen Werten ueberlebten und im
    # MAX-Pass gewonnen haben. Fix: Fingerprint des AKTUELLEN Accounts in jedes
    # State-File schreiben. Beim Lesen werden nur Files mit dem AKTUELLEN Fingerprint
    # beruecksichtigt — alte Account-Files werden ignoriert (und beim Cleanup geloescht).
    #
    # ROOT-CAUSE-FIX (Frank-Bug-Report 2026-06-13): Fingerprint BEVORZUGT aus
    # ~/.claude.json (.oauthAccount.accountUuid) statt aus .credentials.json. Gruende:
    #  (1) Auf macOS existiert .credentials.json gar nicht (Keychain) -> dort war der fp
    #      IMMER "default", die Account-Trennung wirkungslos (7d zeigte 58% statt 2% nach
    #      Account-Wechsel, weil altes+neues Konto denselben kalendarischen seven_d_resets teilen).
    #  (2) Der .credentials.json-Hash aendert sich bei JEDEM Token-Refresh (alle paar Stunden)
    #      -> der fp flappte und schloss eigene gueltige State-Files faelschlich aus.
    #      accountUuid ist stabil ueber Token-Refreshes und wechselt nur bei echtem Account-Wechsel.
    # Fallback: .credentials.json-Datei-Hash (wie bisher), dann "default".
    $accountFp = 'default'
    $acctUuid = $null
    $claudeJson = Join-Path $env:USERPROFILE '.claude.json'
    if (Test-Path $claudeJson) {
        try {
            $cj = Get-Content -Raw -Encoding UTF8 -Path $claudeJson | ConvertFrom-Json
            if ($cj.oauthAccount -and $cj.oauthAccount.accountUuid) { $acctUuid = [string]$cj.oauthAccount.accountUuid }
            elseif ($cj.userID) { $acctUuid = [string]$cj.userID }
        } catch { $acctUuid = $null }
    }
    if ($acctUuid) {
        try {
            $sha = [System.Security.Cryptography.SHA256]::Create()
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($acctUuid)
            $hex = ($sha.ComputeHash($bytes) | ForEach-Object { $_.ToString('x2') }) -join ''
            $accountFp = $hex.Substring(0, 16).ToLower()
        } catch { $accountFp = 'default' }
    } else {
        $credFile = Join-Path $env:USERPROFILE '.claude\.credentials.json'
        if (Test-Path $credFile) {
            try {
                $hashObj = Get-FileHash -Path $credFile -Algorithm SHA256 -ErrorAction Stop
                if ($hashObj -and $hashObj.Hash) {
                    $accountFp = $hashObj.Hash.Substring(0, 16).ToLower()
                }
            } catch { $accountFp = 'default' }
        }
    }

    # 1. SCHREIBEN — nur wenn alle Werte plausibel sind (Schicht 1: Praeventiv).
    #    NIEMALS bei leerer session_id schreiben (Datei rate-limits-.json verhindern).
    $canWrite = ($sessionId -match '^[A-Za-z0-9_-]+$') -and `
                (Test-ValidPercent $five_h_used) -and `
                ((-not $week_used) -or (Test-ValidPercent $week_used)) -and `
                ((-not $five_h_resets) -or $five_h_resets -eq 0 -or (Test-ValidResetTs $five_h_resets $nowTs))

    if ($canWrite) {
        $myState = Join-Path $stateDir "rate-limits-$sessionId.json"
        $tmpState = "$myState.tmp"
        # 7d-Wert: Wenn API in dieser Antwort einen gueltigen Wert liefert -> nehmen.
        # Wenn API keinen Wert liefert (leer/null), den existierenden 7d-Wert aus dem
        # eigenen State-File behalten — sonst wuerde ein API-Call ohne seven_day-Feld
        # den 7d-Wert auf 0 zuruecksetzen, was bei MAX-Aggregation den echten hoeheren
        # Wert anderer Sessions plattmachen koennte (Frank-Bug-Report 2026-05-10 abend).
        $sevenDValue = 0
        if (Test-ValidPercent $week_used) {
            $sevenDValue = [int]$week_used
        } elseif (Test-Path $myState) {
            try {
                $existing = Get-Content -Raw -Encoding UTF8 -Path $myState | ConvertFrom-Json
                if ($existing -and (Test-ValidPercent $existing.seven_d)) {
                    $sevenDValue = [int]$existing.seven_d
                }
            } catch { $sevenDValue = 0 }
        }
        # 7d-Reset: nur uebernehmen wenn plausibel, sonst aus eigenem State behalten (konstant ueber die Woche).
        $sevenDResets = 0
        if (Test-ValidResetTs7d $week_resets $nowTs) {
            $sevenDResets = [long]$week_resets
        } elseif (Test-Path $myState) {
            try {
                $existing7 = Get-Content -Raw -Encoding UTF8 -Path $myState | ConvertFrom-Json
                if ($existing7 -and $existing7.seven_d_resets) { $sevenDResets = [long]$existing7.seven_d_resets }
            } catch { $sevenDResets = 0 }
        }
        $payload = [ordered]@{
            ts_seen        = $nowTs
            session_id     = $sessionId
            account_fp     = $accountFp
            five_h         = [int]$five_h_used
            five_h_resets  = [long]$five_h_resets
            seven_d        = $sevenDValue
            seven_d_resets = [long]$sevenDResets
        }
        # Atomic write: tmp + rename (Schicht 3: Eliminierung von Half-Read)
        ($payload | ConvertTo-Json -Compress) | Out-File -FilePath $tmpState -Encoding UTF8 -Force
        Move-Item -Path $tmpState -Destination $myState -Force -ErrorAction SilentlyContinue
    }

    # 2. Cleanup: State-Files aelter als 24h loeschen — nur ~jeder 600. Aufruf
    #    (also ca. alle 10 Minuten bei refreshInterval=1) damit es nicht jeden
    #    Refresh kostet. Performance-kritisch (Frank-Bug-Report 2026-05-09 22:04).
    #    Zusaetzlich: kaputte Files mit leerer session_id (rate-limits-.json) sofort entfernen.
    if (($nowTs % 600) -lt 2) {
        $cutoff = $nowTs - 86400
        Get-ChildItem -Path $stateDir -Filter 'rate-limits-*.json' -ErrorAction SilentlyContinue |
            Where-Object { $_.LastWriteTimeUtc.Subtract([DateTime]'1970-01-01').TotalSeconds -lt $cutoff } |
            Remove-Item -Force -ErrorAction SilentlyContinue
        Get-ChildItem -Path $stateDir -Filter 'ctx-*' -ErrorAction SilentlyContinue |
            Where-Object { $_.LastWriteTimeUtc.Subtract([DateTime]'1970-01-01').TotalSeconds -lt $cutoff } |
            Remove-Item -Force -ErrorAction SilentlyContinue
    }
    # Defekte Files mit leerer session_id IMMER entfernen (Schicht 2: Reaktiv)
    $emptyFile = Join-Path $stateDir 'rate-limits-.json'
    if (Test-Path $emptyFile) { Remove-Item -Path $emptyFile -Force -ErrorAction SilentlyContinue }

    # Account-Wechsel-Cleanup: Files mit fremdem Fingerprint sofort loeschen,
    # nicht erst nach 24h. Ohne diesen Schritt zeigt die Statusline nach Logout/Login
    # noch die rate_limits des alten Accounts (Frank-Bug-Report 2026-05-10).
    if ($accountFp -ne 'default') {
        Get-ChildItem -Path $stateDir -Filter 'rate-limits-*.json' -ErrorAction SilentlyContinue | ForEach-Object {
            $f = $_.FullName
            try {
                $entryFp = (Get-Content -Raw -Encoding UTF8 -Path $f | ConvertFrom-Json).account_fp
                if ($entryFp -and $entryFp -ne $accountFp) {
                    Remove-Item -Path $f -Force -ErrorAction SilentlyContinue
                }
            } catch { }
        }
    }

    # 3. LESEN + VALIDIEREN: Alle State-Files lesen, kaputte loeschen, Werte clampen.
    $allEntries = @()
    Get-ChildItem -Path $stateDir -Filter 'rate-limits-*.json' -ErrorAction SilentlyContinue | ForEach-Object {
        $f = $_.FullName
        $entry = $null
        try {
            $entry = Get-Content -Raw -Encoding UTF8 -Path $f | ConvertFrom-Json
        } catch {
            # Kaputtes JSON → Datei loeschen (Schicht 2: Reaktiv, Self-Healing)
            Remove-Item -Path $f -Force -ErrorAction SilentlyContinue
            return
        }
        # Plausibilitaetspruefung pro Eintrag
        if (-not $entry) {
            Remove-Item -Path $f -Force -ErrorAction SilentlyContinue
            return
        }
        $sid = [string]$entry.session_id
        if (-not $sid -or $sid -notmatch '^[A-Za-z0-9_-]+$') {
            Remove-Item -Path $f -Force -ErrorAction SilentlyContinue
            return
        }
        if (-not (Test-ValidPercent $entry.five_h)) { return }  # Ueberspringen, nicht loeschen (koennte race-bedingt sein)
        if (($entry.seven_d -ne $null) -and -not (Test-ValidPercent $entry.seven_d)) { return }
        # Account-Filter: alte Files ohne account_fp werden akzeptiert (Migrations-Toleranz),
        # aber Files mit anderem Fingerprint werden ignoriert.
        $entryFp = if ($entry.PSObject.Properties['account_fp']) { [string]$entry.account_fp } else { '' }
        if ($entryFp -and $entryFp -ne $accountFp) { return }
        $resets = 0
        try { $resets = [long]$entry.five_h_resets } catch { $resets = 0 }
        if ($resets -ne 0 -and -not (Test-ValidResetTs $resets $nowTs)) {
            $resets = 0  # Ungueltigen Timestamp ignorieren, aber Eintrag behalten
        }
        $weekResetsEntry = 0
        try { $weekResetsEntry = [long]$entry.seven_d_resets } catch { $weekResetsEntry = 0 }
        if ($weekResetsEntry -ne 0 -and -not (Test-ValidResetTs7d $weekResetsEntry $nowTs)) {
            $weekResetsEntry = 0
        }
        $tsSeen = 0
        try { $tsSeen = [long]$entry.ts_seen } catch { $tsSeen = 0 }
        $allEntries += [PSCustomObject]@{
            session_id     = $sid
            ts_seen        = $tsSeen
            five_h         = [int]$entry.five_h
            seven_d        = if (Test-ValidPercent $entry.seven_d) { [int]$entry.seven_d } else { 0 }
            five_h_resets  = $resets
            seven_d_resets = $weekResetsEntry
        }
    }

    if ($allEntries.Count -gt 0) {
        # Reset-Fenster aus der FRISCHESTEN Session (hoechstes ts_seen) bestimmen,
        # NICHT aus max(resets_at). Grund (Frank-Bug-Report 2026-05-25): Der
        # Reset-Zeitpunkt ist eine accountweite Konstante des aktuellen Fensters —
        # kein Wert der hochzaehlt. max(resets_at) liess eine liegengebliebene
        # Test-/Anomalie-Datei mit kuenstlich grossem Timestamp den Countdown kapern
        # (zeigte 3D10H statt echter 1D23H). Die frischeste API-Antwort hat per
        # Definition den korrekten aktuellen Reset. Verbrauch bleibt max (zaehlt hoch).
        $freshest = $allEntries | Sort-Object -Property @{Expression={[long]$_.ts_seen}} -Descending | Select-Object -First 1
        $freshResets = [long]$freshest.five_h_resets
        $candidates = $allEntries | Where-Object { [long]$_.five_h_resets -eq $freshResets }
        if ($candidates.Count -gt 0) {
            $bestFive = $candidates | Sort-Object -Property @{Expression={[int]$_.five_h}} -Descending | Select-Object -First 1
            # 7d-Reset zuerst: aus der frischesten Session (accountweite Konstante, nicht max)
            $freshWeekResets = [long]$freshest.seven_d_resets
            $freshWeekTs = [long]$freshest.ts_seen
            # 7d-Verbrauch NUR aus dem AKTUELLEN 7d-Fenster (gleiches seven_d_resets wie die
            # frischeste Session) — exakt analog zur 5h-Fenster-Logik oben. Verhindert dass eine
            # idle Session aus dem VORHERIGEN Fenster mit hohem seven_d den frischen niedrigen Wert
            # nach einem 7d-Reset kapert (Frank-Bug-Report 2026-06-03: zeigte 48% statt 5% nach
            # Reset — altes State-File mit seven_d_resets aus dem Vorfenster gewann beim globalen
            # MAX). Fallback auf alle Eintraege wenn kein gueltiges Fenster bekannt ist (lossless).
            #
            # ZUSATZ-FILTER (Defense, Frank-Bug-Report 2026-06-13): Nach einem Account-Wechsel
            # teilen altes und neues Konto denselben kalendarischen seven_d_resets, sodass die
            # Fenster-Trennung den hohen Altwert NICHT aussortiert. Primaer faengt das der
            # account_fp-Filter ab; falls der mal "default" bleibt, begrenzt dieser Frische-Filter
            # das 7d-MAX zusaetzlich auf Dateien innerhalb von 5h (18000s) der frischesten Session.
            # Parallele Live-Sessions refreshen sekuendlich (bleiben drin); tote Vortags-Sessions
            # eines alten Kontos fallen raus. Lossless: kein echter aktueller Wert wird verworfen.
            if ($freshWeekResets -gt 0) {
                $weekCandidates = $allEntries | Where-Object { [long]$_.seven_d_resets -eq $freshWeekResets -and [long]$_.ts_seen -ge ($freshWeekTs - 18000) }
            } else {
                $weekCandidates = $allEntries | Where-Object { [long]$_.ts_seen -ge ($freshWeekTs - 18000) }
            }
            $freshSeven = ($weekCandidates | ForEach-Object { [int]$_.seven_d } | Measure-Object -Maximum).Maximum

            # Letzte Sicherheitsclamp — sollte durch Validierung schon 0..100 sein
            $freshFive = [int]$bestFive.five_h
            if ($freshFive -gt 100) { $freshFive = 100 }
            if ($freshFive -lt 0)   { $freshFive = 0 }
            if ($freshSeven -gt 100) { $freshSeven = 100 }
            if ($freshSeven -lt 0)   { $freshSeven = 0 }

            $five_h_used = $freshFive
            $week_used = $freshSeven
            $five_h_resets = [long]$bestFive.five_h_resets
            # 7d-Reset uebernehmen; Fallback auf Input wenn keiner aggregiert wurde
            if ($freshWeekResets -gt 0) { $week_resets = [long]$freshWeekResets }
        }
    }
} catch { }

# Letzter Schutzwall vor der Anzeige (Schicht 2: Reaktiv) — sollte nie noetig sein,
# aber wenn doch ein Mullwert durchschluepft, lieber 100% als 1.778.368.200% zeigen.
if ($five_h_used -ne $null) {
    if ([double]$five_h_used -gt 100) { $five_h_used = 100 }
    if ([double]$five_h_used -lt 0)   { $five_h_used = 0 }
}
if ($week_used -ne $null) {
    if ([double]$week_used -gt 100) { $week_used = 100 }
    if ([double]$week_used -lt 0)   { $week_used = 0 }
}

# Kontext-Verbrauch: das OFFIZIELLE Feld used_percentage DIREKT nehmen (genau das Mass
# das die Auto-Compaction triggert + die TUI anzeigt). Fallback auf 100-remaining fuer
# aeltere CC-Versionen ohne das Feld. Empirisch verifiziert 2026-05-25. Spiegelt statusline.sh.
$ctx_used = $null
if ($ctx_used_pct -ne $null) { $ctx_used = $ctx_used_pct }
elseif ($ctx_remaining -ne $null) { $ctx_used = 100 - $ctx_remaining }
# Frank-Bug-Report 2026-05-10 abend: Nach /clear oder Session-Start zeigt Claude
# Code in stdin noch den ALTEN context_window.remaining_percentage aus der letzten
# API-Antwort (z.B. 99% obwohl gerade gecleart). Erst nach dem ersten neuen API-
# Call wird der Wert aktualisiert. Fix: Wenn das Transcript-File sehr klein ist
# (frische Session), ueberschreiben wir ctx_used mit 0 — sobald Claude Code echte
# Werte liefert, uebernehmen wir die. Schwelle 8 Zeilen = grob 1-2 Turns.
# Fallback: wenn transcript_path leer ist, aus session_id + cwd rekonstruieren.
# Convention: ~/.claude/projects/<encoded-dir>/<session_id>.jsonl
# encoded-dir: jedes Nicht-Alphanum durch "-" ersetzen.
if (-not $transcript_path -and $obj -and $obj.session_id -and $obj.workspace.current_dir) {
    $sid = [string]$obj.session_id
    if ($sid -match '^[A-Za-z0-9_-]+$') {
        $rawDir = [string]$obj.workspace.current_dir
        $encodedDir = ($rawDir -replace '[^A-Za-z0-9]', '-')
        $candidate = Join-Path $env:USERPROFILE ".claude\projects\$encodedDir\$sid.jsonl"
        if (Test-Path -LiteralPath $candidate) { $transcript_path = $candidate }
    }
}
if ($transcript_path -and (Test-Path -LiteralPath $transcript_path)) {
    try {
        $lineCount = 0
        $reader = [System.IO.File]::OpenText($transcript_path)
        try {
            while ($reader.ReadLine() -ne $null) {
                $lineCount++
                if ($lineCount -ge 8) { break }
            }
        } finally { $reader.Close() }
        if ($lineCount -lt 8) { $ctx_used = 0 }
    } catch { }
}

# 5h Reset-Countdown — mit Plausibilitaetspruefung:
# Realistische Werte sind 0..18000 Sekunden (5h). Wenn der Server mal einen
# kaputten Timestamp liefert (z.B. weit in der Zukunft), zeigen wir lieber
# nichts statt absurde "2.000.000h"-Countdowns.
# [long] statt [int] fuer $now verhindert Year-2038-Ueberlauf.
$five_h_countdown = ''
if ($five_h_resets -gt 0) {
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $diff = $five_h_resets - $now
    # 21600 = 6h Toleranz oberhalb der nominellen 5h-Fenstergrenze
    if ($diff -gt 0 -and $diff -le 21600) {
        $mins = [int]($diff / 60)
        if ($mins -ge 60) {
            $h = [int]($mins / 60); $m = $mins % 60
            $five_h_countdown = "${h}h${m}m"
        } else {
            $five_h_countdown = "${mins}m"
        }
    }
}

# 7d Reset-Countdown — Tage + Stunden (Frank 2026-05-25). Format: "3D10H" bzw. nur
# "10H" wenn weniger als ein Tag verbleibt. Wird in derselben grauen Farbe (DIM) in
# Klammern hinter den 7d-Balken gesetzt, analog zum 5h-Countdown. Plausibel: 0..8 Tage.
$week_countdown = ''
if ($week_resets -gt 0) {
    $nowW = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $wdiff = $week_resets - $nowW
    if ($wdiff -gt 0 -and $wdiff -le 691200) {
        $wdays = [int][Math]::Floor($wdiff / 86400)
        $whours = [int][Math]::Floor(($wdiff % 86400) / 3600)
        if ($wdays -ge 1) {
            $week_countdown = "${wdays}D${whours}H"
        } else {
            $week_countdown = "${whours}H"
        }
    }
}

$time = Get-Date -Format 'HH:mm'

# Farben (ANSI 24-bit)
$ESC = [char]27
$B      = "$ESC[38;2;100;180;255m"
$P      = "$ESC[38;2;30;144;255m"
$GREEN  = "$ESC[38;2;64;200;90m"
$YELLOW = "$ESC[38;2;255;190;40m"
$RED    = "$ESC[38;2;240;70;70m"
$T      = "$ESC[38;2;130;135;160m"
$PACE   = "$ESC[38;2;45;212;191m"   # Teal — Pacing-Feature 5h (Symbol + slow/fast)
$PACE7  = "$ESC[38;2;255;120;180m"  # Pink/Rosa — Pacing-Feature 7d (Symbol + slow/fast)
$MIDLINE = "$ESC[1m" + "$ESC[38;2;240;240;250m"  # Hellweiss fett — Pacing-Ziellinie (Mittelstrich). NICHT $MID nennen: PowerShell ist case-insensitiv, $MID kollidiert mit der lokalen Positionsvariable $mid in Get-PaceBar.
$TIMECOL= "$ESC[38;2;220;180;100m"
$DIM    = "$ESC[38;2;90;95;115m"
$SEPCOL = "$ESC[38;2;235;70;70m"    # Rot — Bereichs-Trenner (dick)
$TRACK  = "$ESC[38;2;55;58;75m"
$BOLD   = "$ESC[1m"
$R      = "$ESC[0m"

# Effort-Farbe
$EFFORT_COL = switch ($effort) {
    'xhigh'  { "$ESC[38;2;255;90;220m" }   # Magenta — extra-hoch
    'high'   { "$ESC[38;2;255;180;30m" }   # Gold
    'medium' { "$ESC[38;2;100;180;255m" }  # Cyan
    'low'    { "$ESC[38;2;130;135;160m" }  # Grau
    default  { "$ESC[38;2;130;135;160m" }
}

function Get-PctColor($pct) {
    if ($pct -eq $null) { return $T }
    if ($pct -ge 80) { return $RED }
    if ($pct -ge 50) { return $YELLOW }
    return $GREEN
}

function Get-Bar($pct, $col) {
    if ($pct -eq $null) { $pct = 0 }
    if ($pct -gt 100) { $pct = 100 }
    if ($pct -lt 0) { $pct = 0 }
    # 5 Segmente (Frank 2026-05-31, ~30% schmaler als die alten 7 — mehr Platz in der
    # Statuszeile). Round half up: filled = round(pct*5/100). Prozent-Logik unveraendert,
    # nur Gesamtbreite kleiner. Konsistent zur sh-Variante.
    $filled = [Math]::Floor(($pct * 5 + 50) / 100)
    if ($filled -gt 5) { $filled = 5 }
    $empty = 5 - $filled
    return "${col}" + ('█' * $filled) + "${TRACK}" + ('░' * $empty) + "${R}"
}

# Pacing-Pendel (Frank 2026-05-24): Zeigt ob der 5h-Verbrauch dem Zeitverlauf
# voraus (zu schnell) oder hinterher (zu langsam) ist. ┃ = Idealtempo (Verbrauch
# == verstrichene Zeit im 5h-Fenster), ● = Ist-Position. Links vom Strich = langsam
# (Reserve da), rechts = schnell (laeuft vor Fensterende ins Limit). Track 7 Zeichen
# (schmal). Mitte (Index 3) ist IMMER der weisse Strich (Ziellinie). Der Marker sitzt
# IMMER daneben — NIE auf der Mitte, damit Strich UND Marker stets sichtbar sind
# (Frank 2026-05-24). Marker GRUEN links (slow) / ROT rechts (fast). Identisch zu sh.
function Get-PaceBar($used, $resets, $now, $window) {
    $rem = $resets - $now
    $elapsed = $window - $rem
    if ($elapsed -lt 0) { $elapsed = 0 }
    if ($elapsed -gt $window) { $elapsed = $window }
    $ideal = [int][Math]::Floor($elapsed * 100 / $window)
    $delta = [int]$used - $ideal
    $mid = 3
    # Seite + Farbe: delta>0 = fast (rot, rechts), delta<=0 = slow/ideal (gruen, links).
    if ($delta -gt 0) { $mcol = $RED; $adev = $delta } else { $mcol = $GREEN; $adev = -$delta }
    # Abstand vom Strich nach Abweichungsstaerke (1=nah, 2=mittel, 3=weit).
    if ($adev -le 10) { $abstand = 1 } elseif ($adev -le 25) { $abstand = 2 } else { $abstand = 3 }
    if ($delta -gt 0) { $mpos = $mid + $abstand } else { $mpos = $mid - $abstand }
    $sb = ''
    for ($i = 0; $i -lt 7; $i++) {
        if ($i -eq $mid) {
            $sb += "${MIDLINE}┃${R}"      # Ziellinie: immer weiss, immer sichtbar
        } elseif ($i -eq $mpos) {
            $sb += "${mcol}●${R}"         # Marker: gruen (slow) / rot (fast), nie auf der Mitte
        } else {
            $sb += "${TRACK}─${R}"
        }
    }
    return $sb
}

$SEP = "${SEPCOL} ┃ ${R}"   # ein dicker roter Strich (Frank 2026-05-24)
$GSEP = " "                 # Gruppen-Abstand (kein Trenner, 1 Leerzeichen): haelt 5h+Pendel bzw. 7d+Pendel eng zusammen
$EMPTY_BAR = "${TRACK}" + ('░' * 5) + "${R}"

# Icons
$ICON_MODEL  = '🤖'
$ICON_EFFORT = '⚡'
$ICON_DIR    = '📁'
$ICON_5H     = '⏱'
$ICON_7D     = '📅'
$ICON_CTX    = '🧠'
$ICON_TIME   = '🕐'
$ICON_PACE   = '🎯'

# Ausgabe
# Reihenfolge (Frank 2026-05-25): zuerst die Limit-Bereiche (5h, 5h-Pacing, 7d,
# 7d-Pacing), DANN Modell, Effort, Ordner, Kontext, Uhrzeit. Kontext bleibt direkt
# hinter dem Ordner wie zuvor. Identische Reihenfolge wie statusline.sh.

# ===== ZEILE 1: Modell | Effort | 5h + Pacing | 7d + Pacing | Kontext (Frank 2026-05-25) =====

# 1. Context-Verbrauch (jetzt GANZ vorne — Frank 2026-06-07).
#    Erstes Element von Zeile 1: KEIN fuehrender Trenner ($out mit = initialisieren).
#    Wenn kein ctx-Wert bekannt ist (frueher Session-Start), faellt das Modell auf
#    die erste Position zurueck (dann ohne Trenner initialisieren).
if ($ctx_used -ne $null) {
    # Ab 80% BLINKT der Kontextbereich auffaellig (Frank 2026-06-07): zwei Techniken
    # kombiniert. (1) Skript-Puls ueber refreshInterval=1 — in der ungeraden Sekunde
    # invers (weisser fetter Text auf rotem Hintergrund), in der geraden normal rot →
    # pulst ~1 Hz, terminal-UNABHAENGIG. (2) zusaetzlich das native ANSI-Blink-Attribut
    # (SGR 5) fuer Terminals die es koennen. Eigener Balken ohne interne Resets, damit
    # der rote Hintergrund im Invers-Zustand durchgehend bleibt. Identisch zu statusline.sh.
    $ctxSec = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() % 2
    if (([int]$ctx_used -ge 80) -and ($ctxSec -eq 1)) {
        $cf = [int][Math]::Floor(([int]$ctx_used * 5 + 50) / 100); if ($cf -gt 5) { $cf = 5 }; $ce = 5 - $cf
        $cbar = ('█' * $cf) + ('░' * $ce)
        $BLINK = "$ESC[5m"
        $INV = "$ESC[1m" + "$ESC[38;2;255;255;255m" + "$ESC[48;2;235;70;70m"  # fett, weiss auf rot
        $out = "${BLINK}${INV} ${ICON_CTX} ctx ${cbar} ${ctx_used}% ${R}"
    } else {
        $col = Get-PctColor $ctx_used
        $bar = Get-Bar $ctx_used $col
        $out = "${col}${ICON_CTX} ctx${R} ${bar} ${col}${ctx_used}%${R}"
    }
    # 2. Modell — fuehrender Trenner, da der Kontext davor steht
    $out += "${SEP}${B}${ICON_MODEL} ${BOLD}${model}${R}"
} else {
    # Kein Kontext bekannt: Modell ist erstes Element OHNE fuehrenden Trenner
    $out = "${B}${ICON_MODEL} ${BOLD}${model}${R}"
}

# 3. Effort (direkt hinter dem Modell)
$out += "${SEP}${EFFORT_COL}${ICON_EFFORT} ${effort_upper}${R}"

# 3. 5h
if ($five_h_used -ne $null) {
    $col = Get-PctColor $five_h_used
    $bar = Get-Bar $five_h_used $col
    if ($five_h_countdown) {
        $out += "${SEP}${col}${ICON_5H} 5h${R} ${bar} ${col}${five_h_used}%${R} ${DIM}(${five_h_countdown})${R}"
    } else {
        $out += "${SEP}${col}${ICON_5H} 5h${R} ${bar} ${col}${five_h_used}%${R}"
    }
} else {
    $out += "${SEP}${DIM}${ICON_5H} 5h${R} ${EMPTY_BAR} ${DIM}--${R}"
}

# 4. 5h-Pacing-Pendel direkt hinter dem 5h-Balken (Frank 2026-05-24):
# Nur zeigen wenn Verbrauch UND Reset-Zeitpunkt bekannt sind.
if ($five_h_used -ne $null -and $five_h_resets -gt 0) {
    $nowPace = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $paceBar = Get-PaceBar $five_h_used $five_h_resets $nowPace 18000
    $out += "${GSEP}${PACE}slow${R} ${paceBar} ${PACE}fast${R}"
}

# 5. 7d (mit Reset-Countdown in Tagen+Stunden — Frank 2026-05-25)
if ($week_used -ne $null) {
    $col = Get-PctColor $week_used
    $bar = Get-Bar $week_used $col
    if ($week_countdown) {
        $out += "${SEP}${col}${ICON_7D} 7d${R} ${bar} ${col}${week_used}%${R} ${DIM}(${week_countdown})${R}"
    } else {
        $out += "${SEP}${col}${ICON_7D} 7d${R} ${bar} ${col}${week_used}%${R}"
    }
} else {
    $out += "${SEP}${DIM}${ICON_7D} 7d${R} ${EMPTY_BAR} ${DIM}--${R}"
}

# 6. 7d-Pacing-Pendel (Frank 2026-05-24): gleiche Logik wie 5h, Fenster 7 Tage (604800s),
# eigene Feature-Farbe (Pink). Nur zeigen wenn Verbrauch UND 7d-Reset bekannt sind.
if ($week_used -ne $null -and $week_resets -gt 0) {
    $nowPace7 = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $paceBar7 = Get-PaceBar $week_used $week_resets $nowPace7 604800
    $out += "${GSEP}${PACE7}slow${R} ${paceBar7} ${PACE7}fast${R}"
}

# (Context steht jetzt GANZ vorne — Frank 2026-06-07, frueher hier an Position 7)

# ===== Ordner + Uhrzeit jetzt am Ende von Zeile 1 (Frank 2026-05-31) =====
# Frueher zweizeilig; auf Wunsch von Frank in EINE Zeile zusammengefuehrt.

# 8. Ordner (mit fuehrendem Trenner, da jetzt hinter dem Kontext in derselben Zeile)
if ($cwd) {
    $out += "${SEP}${P}${ICON_DIR} ${cwd}${R}"
}

# 9. Uhrzeit
$out += "${SEP}${TIMECOL}${ICON_TIME} ${time}${R}"

[Console]::Out.Write($out)

exit 0
