# Cache-TTL-Diagnose — prueft ob ENABLE_PROMPT_CACHING_1H bei Frank wirklich greift
# Hintergrund: Anthropic hat seit Maerz 2026 still den Cache-TTL von 1h auf 5min reduziert
# (GitHub Issue anthropics/claude-code#46829). Dieses Skript zeigt anhand der Session-Logs
# ob Frank's 1h-Setting bei ihm auf dem Server akzeptiert wird oder ob er auch betroffen ist.
#
# Output: Tabelle pro Tag mit 5m- vs 1h-Cache-Write-Tokens + klares Fazit.

param(
    [int]$DaysBack = 60
)

$projectsDir = Join-Path $env:USERPROFILE ".claude\projects"
if (-not (Test-Path $projectsDir)) {
    Write-Host "Fehler: $projectsDir nicht gefunden." -ForegroundColor Red
    exit 1
}

Write-Host "Lese JSONL-Dateien aus $projectsDir ..." -ForegroundColor Cyan

$cutoff = (Get-Date).AddDays(-$DaysBack)
$daily = @{}
$totalLines = 0
$totalParsed = 0

$jsonlFiles = Get-ChildItem -Path $projectsDir -Filter "*.jsonl" -Recurse -File
Write-Host "Gefundene JSONL-Dateien: $($jsonlFiles.Count)" -ForegroundColor Cyan

foreach ($file in $jsonlFiles) {
    if ($file.LastWriteTime -lt $cutoff) { continue }
    $reader = [System.IO.File]::OpenText($file.FullName)
    try {
        while (-not $reader.EndOfStream) {
            $line = $reader.ReadLine()
            $totalLines++
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            try {
                $d = $line | ConvertFrom-Json -ErrorAction Stop
                $usage = $null
                if ($d.message -and $d.message.usage) { $usage = $d.message.usage }
                elseif ($d.usage) { $usage = $d.usage }
                if (-not $usage -or -not $usage.cache_creation) { continue }
                $totalParsed++

                $ts = $d.timestamp
                if (-not $ts) { continue }
                # PowerShell 7+ konvertiert ISO-Strings automatisch zu DateTime — abfangen
                if ($ts -is [DateTime]) {
                    $date = $ts.ToString("yyyy-MM-dd")
                } else {
                    $date = ($ts -split 'T')[0]
                }

                $f5m = [int64]($usage.cache_creation.ephemeral_5m_input_tokens)
                $f1h = [int64]($usage.cache_creation.ephemeral_1h_input_tokens)
                $fread = [int64]($usage.cache_read_input_tokens)

                if (-not $daily.ContainsKey($date)) {
                    $daily[$date] = @{ '5m'=0; '1h'=0; 'read'=0; 'calls'=0 }
                }
                $daily[$date]['5m'] += $f5m
                $daily[$date]['1h'] += $f1h
                $daily[$date]['read'] += $fread
                $daily[$date]['calls']++
            } catch { }
        }
    } finally {
        $reader.Close()
    }
}

Write-Host "`nVerarbeitet: $totalParsed Eintraege aus $totalLines Zeilen" -ForegroundColor Green

if ($daily.Count -eq 0) {
    Write-Host "Keine Daten in den letzten $DaysBack Tagen gefunden." -ForegroundColor Yellow
    exit 0
}

# Ausgabe: Tabelle pro Tag (chronologisch)
$sortedDates = $daily.Keys | Sort-Object
Write-Host ""
Write-Host ("{0,-12} {1,15} {2,15} {3,10} {4,8} {5,8}" -f "Datum", "5m-Write-Tok", "1h-Write-Tok", "Reads", "5m-%", "Verhalten") -ForegroundColor Cyan
Write-Host ("-" * 80)

$totalAll5m = 0; $totalAll1h = 0; $totalAllRead = 0
foreach ($date in $sortedDates) {
    $d = $daily[$date]
    $sum = $d['5m'] + $d['1h']
    $pct5m = if ($sum -gt 0) { [math]::Round(100.0 * $d['5m'] / $sum, 1) } else { 0 }
    $verhalten = if ($sum -eq 0) { "-" }
                 elseif ($pct5m -ge 95) { "5m only" }
                 elseif ($pct5m -le 5) { "1h only" }
                 else { "MIXED" }
    Write-Host ("{0,-12} {1,15:N0} {2,15:N0} {3,10:N0} {4,7}% {5,8}" -f $date, $d['5m'], $d['1h'], $d['read'], $pct5m, $verhalten)
    $totalAll5m += $d['5m']
    $totalAll1h += $d['1h']
    $totalAllRead += $d['read']
}

# Gesamtfazit
Write-Host ("-" * 80)
$totalSum = $totalAll5m + $totalAll1h
$totalPct5m = if ($totalSum -gt 0) { [math]::Round(100.0 * $totalAll5m / $totalSum, 1) } else { 0 }
Write-Host ("{0,-12} {1,15:N0} {2,15:N0} {3,10:N0} {4,7}%" -f "GESAMT", $totalAll5m, $totalAll1h, $totalAllRead, $totalPct5m) -ForegroundColor Yellow

Write-Host "`n=== FAZIT ===" -ForegroundColor Cyan
if ($totalSum -eq 0) {
    Write-Host "Keine Cache-Write-Tokens gefunden — keine Aussage moeglich." -ForegroundColor Yellow
} elseif ($totalPct5m -ge 90) {
    Write-Host "BETROFFEN: $totalPct5m% deiner Cache-Writes laufen ueber 5min-TTL." -ForegroundColor Red
    Write-Host "Deine ENABLE_PROMPT_CACHING_1H=1 Setting wird vom Anthropic-Server ignoriert." -ForegroundColor Red
    Write-Host "Empfehlung: Setting kann entfernt werden, spart 25% Cache-Write-Aufschlag (1.25x statt 2x)." -ForegroundColor Yellow
} elseif ($totalPct5m -le 10) {
    Write-Host "NICHT BETROFFEN: $($totalPct5m)% 5m, $(100-$totalPct5m)% 1h — deine Setting greift sauber." -ForegroundColor Green
    Write-Host "Empfehlung: ENABLE_PROMPT_CACHING_1H=1 behalten." -ForegroundColor Green
} else {
    Write-Host "GEMISCHT: $totalPct5m% 5m, $(100-$totalPct5m)% 1h — deine Setting greift teilweise." -ForegroundColor Yellow
    Write-Host "Empfehlung: Setting behalten, aber tagesweise pruefen wann der Bug zugeschlagen hat." -ForegroundColor Yellow
}
