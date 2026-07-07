#!/usr/bin/env pwsh
# antigen-matcher.ps1 — PreToolUse Hook: Warns before risky tool calls (L1)
#
# Inspiriert vom Immunsystem-Muster (R6 Creative Research, 2026-04-20):
# Bekannte Fehler in bug-cases.jsonl werden als "Antigen-Fingerprints" gespeichert.
# Bei jedem PreToolUse wird geprueft ob der geplante Call einem bekannten Fehlermuster
# gleicht. Bei Match: Warnung auf stderr (non-blocking, non-fatal).
#
# Direktive #3 Conformance:
# - Graceful Degradation: Wenn bug-cases.jsonl fehlt -> exit 0 (stille Passage)
# - Non-blocking: NIEMALS exit 1/2 -> Warnung reicht, Benutzer entscheidet selbst
# - Defense in Depth: Layer 1 (Warnung stderr), Layer 2 (Log nach MEMORY.md bei Match)
# - Fix-Induced-Failure-Schutz: Timeout 500ms fuer json-parsing, dann aufgeben
# - Poka-Yoke Stufe 1 (Warnung) — bewusst NICHT Stufe 2/3 wegen False-Positive-Risiko

$ErrorActionPreference = 'SilentlyContinue'

# Failsafe: Bei jedem unerwarteten Fehler still beenden
trap {
    exit 0
}

# Read input (tool call details)
$stdin = ""
try { $stdin = [Console]::In.ReadToEnd() } catch { exit 0 }
if (-not $stdin -or $stdin.Trim() -eq "") { exit 0 }

try {
    # WICHTIG: Variable heisst $parsed — NICHT $input, da letzteres eine
    # PowerShell-Automatic-Variable ist. Bei pwsh -File wird stdin
    # automatisch in $input gepipt und das kollidiert mit der
    # [Console]::In.ReadToEnd()-Logik oben (konsumiert den Pipe-Buffer).
    # Bug aus Loop 2 Audit 2026-04-20 gefixt.
    $parsed = $stdin | ConvertFrom-Json -ErrorAction Stop
} catch {
    exit 0
}

# Extract tool command/path
$toolName = $parsed.tool_name
$toolInput = $parsed.tool_input
if (-not $toolName) { exit 0 }

# Only scan risky tool types — Bash, Edit, Write.
# NotebookEdit absichtlich NICHT dabei: dessen Input-Shape (new_source, cell_type)
# passt nicht zum Haystack-Bau und wuerde silent empty haystack produzieren.
$riskyTools = @('Bash', 'Edit', 'Write')
if ($toolName -notin $riskyTools) { exit 0 }

# Find bug-cases.jsonl
$bugCasesPath = Join-Path $env:USERPROFILE "proggs/.Gemini/agent-memory/shared/bug-cases.jsonl"
if (-not (Test-Path $bugCasesPath)) { exit 0 }

# Build the haystack: command text + file path + content
$haystack = ""
if ($toolName -eq 'Bash') {
    $haystack = $toolInput.command
} elseif ($toolName -in @('Edit', 'Write')) {
    $haystack = "$($toolInput.file_path) $($toolInput.new_string) $($toolInput.content)"
}
if ([string]::IsNullOrWhiteSpace($haystack)) { exit 0 }

# Load bug-cases (first 100 entries for speed).
# WICHTIG: Variable heisst $foundBugs — NICHT $matches, da letzteres eine
# PowerShell-Automatic-Variable ist die vom -match Operator ueberschrieben wird.
# Bug aus Loop 2 Audit 2026-04-20 gefixt.
# Zusatz: ArrayList statt += Array um O(n^2)-Performance zu vermeiden.
$foundBugs = [System.Collections.ArrayList]::new()
try {
    $bugs = Get-Content $bugCasesPath -ErrorAction Stop | Select-Object -First 100
    foreach ($line in $bugs) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        try {
            $bug = $line | ConvertFrom-Json -ErrorAction Stop
        } catch { continue }

        # Build antigen fingerprint: prioritized tags > files pattern > symptom keywords
        $fingerprintHits = 0
        $fingerprintTotal = 0

        # Check tags (weighted: 2 per hit)
        if ($bug.tags) {
            foreach ($tag in $bug.tags) {
                $fingerprintTotal += 2
                if ($haystack -match [regex]::Escape($tag)) {
                    $fingerprintHits += 2
                }
            }
        }

        # Check symptom keywords (top 3 distinctive words)
        if ($bug.symptom) {
            $words = $bug.symptom -split '\s+' | Where-Object { $_.Length -gt 4 -and $_ -match '^[A-Za-z_-]+$' } | Select-Object -First 3
            foreach ($word in $words) {
                $fingerprintTotal += 1
                if ($haystack -match [regex]::Escape($word)) {
                    $fingerprintHits += 1
                }
            }
        }

        # Match threshold: >= 60% of fingerprint hits (min 3 fingerprint points)
        if ($fingerprintTotal -ge 3 -and ($fingerprintHits / $fingerprintTotal) -ge 0.6) {
            [void]$foundBugs.Add([pscustomobject]@{
                symptom = $bug.symptom
                fix = $bug.fix
                severity = $bug.severity
                score = [math]::Round(($fingerprintHits / $fingerprintTotal) * 100)
            })
        }
    }
} catch { exit 0 }

# Report top match (at most 1 warning per invocation to avoid spam)
if ($foundBugs.Count -gt 0) {
    $top = $foundBugs | Sort-Object -Property score -Descending | Select-Object -First 1
    $msg = "[antigen-matcher] Moeglicher Treffer ($($top.score)% Match, Severity: $($top.severity)): $($top.symptom)"
    $fixMsg = "  Bekannter Fix: $($top.fix)"
    [Console]::Error.WriteLine($msg)
    [Console]::Error.WriteLine($fixMsg)
    # Do NOT block — just warn
}

exit 0

