# Poka-Yoke: JSON-Validierung nach jedem Write/Edit auf JSON-Dateien
# Quelle: Superintelligenz Finding 8 — Fehler unmoeglich machen
# Event: PostToolUse (Edit|Write)
# Zweck: Prueft ob geschriebene JSON-Dateien valide sind

param()
$ErrorActionPreference = 'SilentlyContinue'

$input_data = $null
try {
    $hookInput = [Console]::In.ReadToEnd()
    if (-not $hookInput -or $hookInput.Length -lt 5) { exit 0 }
    $input_data = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch {
    exit 0  # Kein JSON-Input = kein Problem
}

$filePath = ""
if ($input_data.tool_input -and $input_data.tool_input.file_path) {
    $filePath = $input_data.tool_input.file_path
}

# Nur JSON-Dateien pruefen
if (-not ($filePath -match '\.json$')) {
    exit 0
}

# Datei existiert?
if (-not (Test-Path $filePath)) {
    exit 0
}

# BOM-Schutz fuer Claude-Config-JSON (Poka-Yoke, Direktive #3): Ein anderer PostToolUse-Hook
# (Formatter/Guard) kann nach einem Edit ein UTF-8-BOM (re-)einfuegen. Claude Codes strikter
# JSON-Parser verwirft eine BOM-behaftete settings.json STILL -> alle Hooks tot, ohne Warnung
# (claude-config.md 3.2). Wir entfernen das BOM hier automatisch, Root-Cause-unabhaengig.
$isClaudeConfig = ($filePath -match '[\\/]\.claude[\\/]') -or
                  ($filePath -match '[\\/]settings(\.local)?\.json$') -or
                  ($filePath -match '[\\/]\.mcp\.json$') -or
                  ($filePath -match 'claude-code-setup[\\/]')
if ($isClaudeConfig) {
    try {
        $bytes = [System.IO.File]::ReadAllBytes($filePath)
        if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
            $text = [System.IO.File]::ReadAllText($filePath)   # .NET entfernt das BOM aus dem String
            [System.IO.File]::WriteAllText($filePath, $text, (New-Object System.Text.UTF8Encoding $false))
            Write-Host "POKA-YOKE: UTF-8-BOM aus $filePath entfernt (haette Claude Codes JSON-Parser still gebrochen)."
        }
    } catch { }
}

# JSON validieren
try {
    $content = Get-Content -Path $filePath -Raw -Encoding UTF8
    $null = $content | ConvertFrom-Json -ErrorAction Stop
    # Valide JSON — alles gut
    exit 0
} catch {
    # Kaputtes JSON gefunden!
    Write-Host "POKA-YOKE WARNUNG: $filePath enthaelt KEIN valides JSON nach dem letzten Edit!"
    Write-Host "Fehler: $($_.Exception.Message)"
    Write-Host "Bitte die Datei sofort reparieren bevor weitergearbeitet wird."
    exit 0  # Nicht blockieren, nur warnen
}

exit 0
