#!/usr/bin/env pwsh
# update-changelog.ps1 — best-practices Skill (Teil 1)
#
# Holt den offiziellen Claude-Code-Changelog VERBATIM und pflegt
# best-practices/_changelog-archiv.md:
#   - Erstlauf (oder -FirstRun): komplette Datei neu aufbauen
#   - sonst inkrementell: NUR Versionen neuer als _state.json.last_version oben anhaengen,
#     alte Eintraege bleiben unangetastet.
# Jede Version wird mit ihrem npm-Publish-Datum angereichert (## X.Y.Z — YYYY-MM-DD).
# Quellen (beide offiziell): GitHub raw CHANGELOG.md + npm-Registry time-Map.
#
# Aufruf:
#   pwsh update-changelog.ps1                 # inkrementell (Default)
#   pwsh update-changelog.ps1 -FirstRun       # komplette Neuanlage
#   pwsh update-changelog.ps1 -DataDir <pfad> # anderer Zielordner (Default: ~/proggs/best-practices)

[CmdletBinding()]
param(
  [string]$DataDir = (Join-Path $HOME 'proggs/best-practices'),
  [switch]$FirstRun
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$changelogUrl = 'https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md'
$archive   = Join-Path $DataDir '_changelog-archiv.md'
$stateFile = Join-Path $DataDir '_state.json'
$today     = (Get-Date -Format 'yyyy-MM-dd')

if (-not (Test-Path $DataDir)) { New-Item -ItemType Directory -Force -Path $DataDir | Out-Null }

# --- 1. last_version lesen ---
$lastVersion = $null
if (-not $FirstRun -and (Test-Path $stateFile)) {
  try { $lastVersion = (Get-Content $stateFile -Raw | ConvertFrom-Json).last_version } catch { $lastVersion = $null }
}
if ([string]::IsNullOrWhiteSpace($lastVersion) -or -not (Test-Path $archive)) { $FirstRun = $true }

# --- 2. Changelog verbatim holen ---
$raw = (Invoke-WebRequest -UseBasicParsing -Uri $changelogUrl).Content
$raw = $raw -replace "`r`n", "`n"
$rawLines = $raw -split "`n"

# --- 3. npm-Datums-Map holen (npm view -> Fallback Registry) ---
$npmJson = $null
try { $npmJson = (& npm view '@anthropic-ai/claude-code' time --json) 2>$null } catch {}
if ($npmJson -is [array]) { $npmJson = $npmJson -join "`n" }
if ([string]::IsNullOrWhiteSpace($npmJson)) {
  try {
    $reg = Invoke-RestMethod -UseBasicParsing -Uri 'https://registry.npmjs.org/@anthropic-ai%2Fclaude-code'
    $npmJson = $reg.time | ConvertTo-Json -Depth 3
  } catch { $npmJson = '{}' }
}
# Datums-Map EINMAL aufbauen (Version -> YYYY-MM-DD) statt pro Version den ganzen JSON neu zu scannen
$dateMap = @{}
foreach ($mm in [regex]::Matches($npmJson, '"(\d+\.\d+\.\d+)"\s*:\s*"(\d{4}-\d{2}-\d{2})')) {
  $dateMap[$mm.Groups[1].Value] = $mm.Groups[2].Value
}
function Get-VerDate([string]$v) {
  if ($dateMap.ContainsKey($v)) { return $dateMap[$v] } else { return $null }
}

# --- 4. Versions-Header-Indizes im Quell-Changelog ---
$headerIdx = @()
for ($i = 0; $i -lt $rawLines.Count; $i++) {
  if ($rawLines[$i] -match '^## (\d+\.\d+\.\d+)\s*$') { $headerIdx += $i }
}
if ($headerIdx.Count -eq 0) { throw "Keine Versions-Header im Quell-Changelog gefunden — Abbruch (Quelle pruefen)." }

# Block von Zeile $start bis $endExclusive, Versions-Header mit Datum anreichern
function Enrich-Block([int]$start, [int]$endExclusive) {
  $block = for ($j = $start; $j -lt $endExclusive; $j++) {
    $line = $rawLines[$j]
    if ($line -match '^## (\d+\.\d+\.\d+)\s*$') {
      $v = $Matches[1]; $d = Get-VerDate $v
      if ($d) { "## $v — $d" } else { "## $v — Datum unbekannt" }
    } else { $line }
  }
  return ($block -join "`n")
}

# --- 5. Kopf-Block des Archivs ---
$head = @"
# Claude-Code Changelog-Archiv (vollstaendig, verbatim)

> Quelle: https://github.com/anthropics/claude-code/blob/main/CHANGELOG.md
> Raw: $changelogUrl
> Aktualisiert: $today — WORTWOERTLICH, ungekuerzt (kein WebFetch, keine Zusammenfassung).
> Datum je Version: npm-Publish-Zeitstempel (@anthropic-ai/claude-code) = faktisches Release-Datum.
> Aktualisierung inkrementell: neue Versionen kommen oben dazu, alte bleiben unangetastet.

---
"@

$newest = ($rawLines[$headerIdx[0]] -replace '^## ', '').Trim()

if ($FirstRun) {
  # --- 6a. komplette Neuanlage ---
  $bodyEnriched = Enrich-Block $headerIdx[0] $rawLines.Count
  $out = $head + "`n`n" + $bodyEnriched + "`n"
  Set-Content -Path $archive -Value $out -Encoding utf8
  $mode = 'FirstRun (Voll-Aufbau)'
  $added = $headerIdx.Count
}
else {
  # --- 6b. inkrementell ---
  $lvLine = -1
  foreach ($idx in $headerIdx) {
    if ($rawLines[$idx] -match ('^## ' + [regex]::Escape($lastVersion) + '\s*$')) { $lvLine = $idx; break }
  }
  if ($lvLine -lt 0) {
    throw "last_version '$lastVersion' nicht im Quell-Changelog gefunden — kein sicherer inkrementeller Lauf. Mit -FirstRun komplett neu aufbauen."
  }
  if ($lvLine -eq $headerIdx[0]) {
    $mode = 'inkrementell (nichts Neues)'
    $added = 0
  }
  else {
    $newEnriched = Enrich-Block $headerIdx[0] $lvLine
    $added = ($headerIdx | Where-Object { $_ -lt $lvLine }).Count
    $archLines = Get-Content $archive
    $archHeaderEnd = -1
    for ($k = 0; $k -lt $archLines.Count; $k++) {
      if ($archLines[$k] -match '^## \d+\.\d+\.\d+') { $archHeaderEnd = $k; break }
    }
    if ($archHeaderEnd -lt 1) { throw "Im bestehenden Archiv kein Kopf/Versions-Header gefunden." }
    $archHead = ($archLines[0..($archHeaderEnd - 1)] -join "`n").TrimEnd()
    $archBody = ($archLines[$archHeaderEnd..($archLines.Count - 1)] -join "`n").TrimEnd()
    $out = $archHead + "`n`n" + $newEnriched + "`n`n" + $archBody + "`n"
    Set-Content -Path $archive -Value $out -Encoding utf8
    $mode = "inkrementell (+$added neue Version(en))"
  }
}

# --- 7. _state.json aktualisieren ---
$state = [ordered]@{ last_version = $newest; last_checked = $today }
($state | ConvertTo-Json) | Set-Content -Path $stateFile -Encoding utf8

# --- 8. Verifikation ---
$archHeaders = (Select-String -Path $archive -Pattern '^## \d+\.\d+\.\d+' -AllMatches).Count
$dupNewest   = (Select-String -Path $archive -Pattern ('^## ' + [regex]::Escape($newest) + ' ') -AllMatches).Count
Write-Output "Modus:                  $mode"
Write-Output "Neueste Version:        $newest ($(if (Get-VerDate $newest) { Get-VerDate $newest } else { 'Datum unbekannt' }))"
Write-Output "Neue Versionen:         $added"
Write-Output "Versions-Header gesamt: $archHeaders"
if ($dupNewest -ne 1) { Write-Warning "Neueste Version erscheint $dupNewest-mal (Duplikat?) — bitte pruefen." }
