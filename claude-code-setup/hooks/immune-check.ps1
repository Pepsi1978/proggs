# Immunologisches Gedaechtnis: Bash-Befehle gegen bekannte Fehlermuster pruefen
# Quelle: Superintelligenz Finding 10 — Error-Antigen-System
# Event: PreToolUse (Bash)
# Zweck: Prueft ob ein Bash-Befehl ein bekanntes Fehlermuster enthaelt

param()

$input_data = $null
try {
    $hookInput = [Console]::In.ReadToEnd()
    if (-not $hookInput -or $hookInput.Length -lt 5) { exit 0 }
    $input_data = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch {
    exit 0
}

$command = ""
if ($input_data.tool_input -and $input_data.tool_input.command) {
    $command = $input_data.tool_input.command
}

if ([string]::IsNullOrWhiteSpace($command)) {
    exit 0
}

# Antigen-Datei lesen
$antigenPath = Join-Path $env:USERPROFILE "proggs\.claude\agent-memory\shared\error-antigens.jsonl"
if (-not (Test-Path $antigenPath)) {
    exit 0
}

$warnings = @()

try {
    $lines = Get-Content -Path $antigenPath -Encoding UTF8
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        try {
            $antigen = $line | ConvertFrom-Json -ErrorAction Stop
            $pattern = $antigen.pattern
            if ($command -match $pattern) {
                $severity = $antigen.severity
                $countermeasure = $antigen.countermeasure
                $id = $antigen.id
                $warnings += "IMMUN-CHECK [$id] ($severity): $countermeasure"
            }
        } catch {
            continue  # Kaputte Zeile ueberspringen
        }
    }
} catch {
    exit 0  # Bei Lesefehlern nicht blockieren
}

if ($warnings.Count -gt 0) {
    foreach ($w in $warnings) {
        Write-Host $w
    }
}

# Nie blockieren — nur warnen
exit 0
