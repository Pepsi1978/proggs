# Poka-Yoke: JSON-Validierung nach jedem Write/Edit auf JSON-Dateien
# Quelle: Superintelligenz Finding 8 — Fehler unmoeglich machen
# Event: PostToolUse (Edit|Write)
# Zweck: Prueft ob geschriebene JSON-Dateien valide sind

param()

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
