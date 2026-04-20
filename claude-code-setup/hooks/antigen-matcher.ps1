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
    $input = $stdin | ConvertFrom-Json -ErrorAction Stop
} catch {
    exit 0
}

# Extract tool command/path
$toolName = $input.tool_name
$toolInput = $input.tool_input
if (-not $toolName) { exit 0 }

# Only scan risky tool types — Bash, Edit, Write (not Read/Grep/etc)
$riskyTools = @('Bash', 'Edit', 'Write', 'NotebookEdit')
if ($toolName -notin $riskyTools) { exit 0 }

# Find bug-cases.jsonl
$bugCasesPath = Join-Path $env:USERPROFILE "proggs/.claude/agent-memory/shared/bug-cases.jsonl"
if (-not (Test-Path $bugCasesPath)) { exit 0 }

# Build the haystack: command text + file path + content
$haystack = ""
if ($toolName -eq 'Bash') {
    $haystack = $toolInput.command
} elseif ($toolName -in @('Edit', 'Write')) {
    $haystack = "$($toolInput.file_path) $($toolInput.new_string) $($toolInput.content)"
}
if ([string]::IsNullOrWhiteSpace($haystack)) { exit 0 }

# Load bug-cases (first 100 entries for speed)
$matches = @()
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

        # Match threshold: >= 60% of fingerprint hits
        if ($fingerprintTotal -ge 3 -and ($fingerprintHits / $fingerprintTotal) -ge 0.6) {
            $matches += @{
                symptom = $bug.symptom
                fix = $bug.fix
                severity = $bug.severity
                score = [math]::Round(($fingerprintHits / $fingerprintTotal) * 100)
            }
        }
    }
} catch { exit 0 }

# Report top match (at most 1 warning per invocation to avoid spam)
if ($matches.Count -gt 0) {
    $top = $matches | Sort-Object -Property score -Descending | Select-Object -First 1
    $msg = "[antigen-matcher] Moeglicher Treffer ($($top.score)% Match, Severity: $($top.severity)): $($top.symptom)"
    $fixMsg = "  Bekannter Fix: $($top.fix)"
    [Console]::Error.WriteLine($msg)
    [Console]::Error.WriteLine($fixMsg)
    # Do NOT block — just warn
}

exit 0
