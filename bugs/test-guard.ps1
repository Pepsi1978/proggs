# test-guard.ps1 — Regressionstest fuer den bug-almanac-guard Hook (Stufe-2-Erzwingung).
# Ausfuehren: pwsh -File bugs/test-guard.ps1   (Exit 0 = alle Tests gruen, sonst Anzahl Fehler)
# Ruft den AKTIVEN Hook (~/.claude/hooks/bug-almanac-guard.ps1) mit simulierten PreToolUse-Events auf
# und prueft die Invarianten. Nutzt ein ISOLIERTES TEMP -> stoert die echte Session NICHT.
# Bei jeder Aenderung am Guard ausfuehren. SH-Pendant: bugs/test-guard.sh.

$ErrorActionPreference = "Stop"
$hook = Join-Path $env:USERPROFILE ".claude/hooks/bug-almanac-guard.ps1"
if (-not (Test-Path $hook)) { Write-Host "FEHLER: Hook nicht gefunden: $hook" -ForegroundColor Red; exit 99 }

# Isoliertes TEMP, damit Marker die echte Session nicht beeinflussen.
$iso = Join-Path ([System.IO.Path]::GetTempPath()) ("guardtest-" + [System.Guid]::NewGuid().ToString("N").Substring(0,8))
New-Item -ItemType Directory -Path $iso -Force | Out-Null
$origTemp = $env:TEMP; $origTmp = $env:TMP
$env:TEMP = $iso; $env:TMP = $iso

$bugsDir = Join-Path $env:USERPROFILE "proggs/bugs"
# Bereich mit existierendem Almanach (fuer Block-Tests) + Bereich OHNE Almanach (fuer Hinweis-Test) dynamisch waehlen.
# Kategorie-robust: Almanach rekursiv suchen (liegt jetzt in bugs/<kategorie>/<datei>.md).
function Test-AlmExists([string]$name) { [bool](Get-ChildItem -Path $bugsDir -Recurse -Filter $name -File -ErrorAction SilentlyContinue | Select-Object -First 1) }
$haveKotlin = Test-AlmExists "kotlin.md"
$mapMissing = $null
foreach ($pair in @(@('Foo.swift','swift-appkit.md'), @('app.ts','typescript.md'), @('app.rs','rust.md'))) {
    if (-not (Test-AlmExists $pair[1])) { $mapMissing = $pair; break }
}

$fails = 0
function Run([string]$json) { $json | & pwsh -NoProfile -File $hook 2>$null }
function Decision([string]$out) {
    if ([string]::IsNullOrWhiteSpace($out)) { return "(none)" }
    try { return ($out | ConvertFrom-Json).hookSpecificOutput.permissionDecision } catch { return "(parse-error)" }
}
function Check([string]$label, [bool]$ok) {
    if ($ok) { Write-Host "[PASS] $label" -ForegroundColor Green }
    else { Write-Host "[FAIL] $label" -ForegroundColor Red; $script:fails++ }
}
function ClearMarkers { Get-ChildItem -Path $iso -Filter "bug-almanac-*.flag" -EA SilentlyContinue | Remove-Item -Force -EA SilentlyContinue }

if (-not $haveKotlin) {
    Write-Host "WARN: kotlin.md fehlt — Block-Tests uebersprungen" -ForegroundColor Yellow
} else {
    ClearMarkers
    Check "leeres stdin -> kein Output" ([string]::IsNullOrWhiteSpace((Run '')))
    Check "Edit .kt ohne Lesen -> deny" ((Decision (Run '{"tool_name":"Edit","tool_input":{"file_path":"x/Foo.kt"}}')) -eq 'deny')
    Check "Write .kt ohne Lesen -> deny" ((Decision (Run '{"tool_name":"Write","tool_input":{"file_path":"x/Foo.kt"}}')) -eq 'deny')
    Check "MultiEdit .kt ohne Lesen -> deny" ((Decision (Run '{"tool_name":"MultiEdit","tool_input":{"file_path":"x/Foo.kt"}}')) -eq 'deny')

    ClearMarkers
    Run '{"tool_name":"Read","tool_input":{"file_path":"/p/proggs/bugs/android/kotlin.md"}}' | Out-Null
    Check "Read absolut (Kategorie-Pfad) -> Marker gesetzt" (Test-Path (Join-Path $iso "bug-almanac-read-kotlin.flag"))
    Check "Edit .kt nach Read -> kein deny" ((Decision (Run '{"tool_name":"Edit","tool_input":{"file_path":"x/Foo.kt"}}')) -ne 'deny')

    ClearMarkers
    Run '{"tool_name":"Read","tool_input":{"file_path":"bugs/android/kotlin.md"}}' | Out-Null
    Check "Read relativ (Kategorie-Pfad) -> Marker gesetzt" (Test-Path (Join-Path $iso "bug-almanac-read-kotlin.flag"))

    ClearMarkers
    Run '{"tool_name":"Bash","tool_input":{"command":"cat ~/proggs/bugs/android/kotlin.md"}}' | Out-Null
    Check "Bash-cat Almanach -> Marker gesetzt" (Test-Path (Join-Path $iso "bug-almanac-read-kotlin.flag"))
    Check "Bash harmlos -> kein Output" ([string]::IsNullOrWhiteSpace((Run '{"tool_name":"Bash","tool_input":{"command":"echo hallo"}}')))

    ClearMarkers
    New-Item -ItemType File -Path (Join-Path $iso "bug-almanac-disable.flag") -Force | Out-Null
    Check "Notaus aktiv -> kein deny" ((Decision (Run '{"tool_name":"Edit","tool_input":{"file_path":"x/Foo.kt"}}')) -ne 'deny')
    ClearMarkers
}

if ($mapMissing) {
    Check "Kein Almanach ($($mapMissing[1])) -> kein deny (nur Hinweis)" ((Decision (Run ('{"tool_name":"Edit","tool_input":{"file_path":"x/' + $mapMissing[0] + '"}}'))) -ne 'deny')
} else {
    Write-Host "INFO: alle Test-Bereiche haben Almanache — 'kein Almanach'-Test uebersprungen" -ForegroundColor Yellow
}

# Aufraeumen
$env:TEMP = $origTemp; $env:TMP = $origTmp
Remove-Item -Path $iso -Recurse -Force -EA SilentlyContinue

Write-Host ""
if ($fails -eq 0) { Write-Host "ALLE TESTS GRUEN" -ForegroundColor Green; exit 0 }
else { Write-Host "$fails TEST(S) FEHLGESCHLAGEN" -ForegroundColor Red; exit $fails }
