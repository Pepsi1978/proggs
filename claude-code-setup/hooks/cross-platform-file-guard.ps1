# cross-platform-file-guard.ps1 — Poka-Yoke Stufe 1: Warnt wenn Cross-Platform-Gegenstueck fehlt
# Event: PostToolUse (Edit|Write auf .ps1 oder .sh Dateien in ~/.claude/hooks/)
# Zweck: Sofortmassnahme 2 — Automatische Cross-Platform-Pruefung
#
# Was dieses Skript tut:
# 1. Erkennt wenn eine .ps1-Datei in ~/.claude/hooks/ bearbeitet wurde
# 2. Prueft ob ein .sh-Gegenstueck existiert
# 3. Prueft ob das Gegenstueck aelter ist als die geaenderte Datei
# 4. Warnt Claude per additionalContext — blockiert NICHT (Stufe 1)
#
# Defense in Depth:
#   Schicht 1: Diese automatische Warnung (Poka-Yoke Stufe 1)
#   Schicht 2: Die Cross-Platform-Checkliste in CLAUDE.md (Text-Regel)
#   Schicht 3: Der Benutzer der beim Commit-Message "plattformuebergreifend" prueft
#
# WICHTIG: Standalone-Hook. MUSS mit exit 0 enden. Warnt nur, blockiert nie.

$ErrorActionPreference = "SilentlyContinue"

try {

# --- stdin lesen ---
$inputJson = ""
try {
    $inputJson = $input | Out-String
} catch {
    exit 0
}

if ([string]::IsNullOrWhiteSpace($inputJson)) { exit 0 }

$eventData = $null
try {
    $eventData = $inputJson | ConvertFrom-Json
} catch {
    exit 0
}

# --- Bearbeitete Datei identifizieren ---
$filePath = ""
if ($eventData.tool_input) {
    if ($eventData.tool_input.file_path) { $filePath = $eventData.tool_input.file_path }
    elseif ($eventData.tool_input.path) { $filePath = $eventData.tool_input.path }
}

if ([string]::IsNullOrWhiteSpace($filePath)) { exit 0 }

# Normalisieren
$filePath = $filePath -replace '\\', '/'

# Nur Hook-Dateien in ~/.claude/hooks/ pruefen
if ($filePath -notmatch '\.claude/hooks/') { exit 0 }

# --- Cross-Platform-Logik ---
$warnings = @()
$hooksDir = Join-Path $env:USERPROFILE ".claude" "hooks"

if ($filePath -match '\.ps1$') {
    # Windows-Hook geaendert — gibt es ein .sh-Gegenstueck?
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($filePath)
    $shPath = Join-Path $hooksDir "$baseName.sh"

    if (-not (Test-Path $shPath)) {
        $warnings += "CROSS-PLATFORM: Du hast $baseName.ps1 geaendert, aber es gibt KEIN $baseName.sh Gegenstueck fuer macOS. Soll ich es erstellen?"
    } else {
        # Gegenstueck existiert — ist es aelter?
        $ps1Time = (Get-Item (Join-Path $hooksDir "$baseName.ps1")).LastWriteTime
        $shTime = (Get-Item $shPath).LastWriteTime
        if ($ps1Time -gt $shTime.AddMinutes(5)) {
            $warnings += "CROSS-PLATFORM: $baseName.ps1 wurde geaendert, aber $baseName.sh ist aelter (letzte Aenderung: $($shTime.ToString('yyyy-MM-dd HH:mm'))). Bitte auch die .sh-Version aktualisieren."
        }
    }
}
elseif ($filePath -match '\.sh$') {
    # macOS-Hook geaendert — gibt es ein .ps1-Gegenstueck?
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($filePath)
    $ps1Path = Join-Path $hooksDir "$baseName.ps1"

    if (-not (Test-Path $ps1Path)) {
        $warnings += "CROSS-PLATFORM: Du hast $baseName.sh geaendert, aber es gibt KEIN $baseName.ps1 Gegenstueck fuer Windows. Soll ich es erstellen?"
    } else {
        $shTime = (Get-Item (Join-Path $hooksDir "$baseName.sh")).LastWriteTime
        $ps1Time = (Get-Item $ps1Path).LastWriteTime
        if ($shTime -gt $ps1Time.AddMinutes(5)) {
            $warnings += "CROSS-PLATFORM: $baseName.sh wurde geaendert, aber $baseName.ps1 ist aelter (letzte Aenderung: $($ps1Time.ToString('yyyy-MM-dd HH:mm'))). Bitte auch die .ps1-Version aktualisieren."
        }
    }
}

# --- Warnung ausgeben (wenn noetig) ---
if ($warnings.Count -gt 0) {
    $warnText = $warnings -join "`n"

    # Logging
    $logDir = Join-Path $env:USERPROFILE ".claude" "logs" "hooks"
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
    $logFile = Join-Path $logDir "cross-platform-file-guard-$(Get-Date -Format 'yyyy-MM-dd').log"
    Add-Content -Path $logFile -Value "[$(Get-Date -Format 'HH:mm:ss')] WARN: $warnText" -Encoding UTF8

    # Ausgabe an Claude
    $output = @{
        hookSpecificOutput = @{
            hookEventName = "PostToolUse"
            additionalContext = $warnText
        }
    } | ConvertTo-Json -Depth 5 -Compress
    Write-Output $output
}

} catch {
    # Failsafe
    try {
        $logDir = Join-Path $env:USERPROFILE ".claude" "logs" "hooks"
        if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
        $logFile = Join-Path $logDir "cross-platform-file-guard-$(Get-Date -Format 'yyyy-MM-dd').log"
        Add-Content -Path $logFile -Value "[$(Get-Date -Format 'HH:mm:ss')] FATAL: $($_.Exception.Message)" -Encoding UTF8
    } catch { }
}

# PFLICHT: exit 0
exit 0
