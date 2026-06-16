# Immunologisches Gedaechtnis: Bash-Befehle gegen bekannte Fehlermuster pruefen
# Quelle: Superintelligenz Finding 10 — Error-Antigen-System
# Event: PreToolUse (Bash)
# Zweck: Prueft ob ein Bash-Befehl ein bekanntes Fehlermuster enthaelt

param()
$ErrorActionPreference = 'SilentlyContinue'

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

# Warnungen als verschachteltes additionalContext ausgeben (claude-hooks.md §2.1) — Write-Host/plain
# stdout erreicht Claude bei PreToolUse NICHT (§2.3, nur Debug-Log); DAS war der Grund, warum dieser
# Schutz-Hook bislang faktisch wirkungslos war. Einschraenkung: v2.1.123-Bash-Matcher-Regression (§2.4,
# #55889) kann auch additionalContext droppen -> best-effort, aber spec-korrekt. error-antigens.jsonl ist
# eine bewusst KURATIERTE Antigen-Liste (kein Auto-Wachstum noetig; manuell um neue Gefahrmuster erweiterbar).
if ($warnings.Count -gt 0) {
    $msg = ($warnings -join "`n")
    @{ hookSpecificOutput = @{ hookEventName = "PreToolUse"; additionalContext = $msg } } | ConvertTo-Json -Depth 5 -Compress
}

# Nie blockieren — nur warnen
exit 0
