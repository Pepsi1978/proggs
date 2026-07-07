# subagent-stop-summarizer.ps1 — SubagentStop Hook (type: prompt via command)
# Event: SubagentStop
# Purpose: Injects summary prompt ONLY when a real subagent has completed.
#          Prevents infinite loop caused by type:"prompt" hooks firing on every Stop.
#
# HOW IT WORKS:
#   Codex passes subagent info via stdin as JSON.
#   We read stdin, check for subagent_id. If empty -> silent exit 0.
#   If real subagent -> output prompt text to stdout (Codex reads it as injected prompt).
#
# POKA-YOKE: type:"command" hooks that output text inject that text as a prompt.
#   This only fires when Codex actually calls SubagentStop with a subagent_id.
#
# EXIT CODES:
#   0 = success (with or without output)
#   Non-0 = error (Codex may log but continues)

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

# Read stdin (subagent context passed by Codex)
$inputJson = ""
try {
    $inputJson = [Console]::In.ReadToEnd()
} catch { }

# Parse subagent info
$subagentId = ""
$subagentResult = ""
try {
    if ($inputJson -and $inputJson.Trim() -ne "") {
        $parsed = $inputJson | ConvertFrom-Json -ErrorAction Stop
        $subagentId = $parsed.subagent_id
        if (-not $subagentId) { $subagentId = $parsed.id }
        if (-not $subagentId) { $subagentId = $parsed.agent_id }
        $subagentResult = $parsed.result
    }
} catch { }

# GUARD: If no real subagent_id found, exit silently
# This prevents the infinite loop where type:"prompt" fires on every response
if (-not $subagentId -or $subagentId.Trim() -eq "") {
    exit 0
}

# Real subagent completed — inject summary prompt
$prompt = @"
Ein Subagent hat gerade seine Arbeit beendet (ID: $subagentId).
Fasse in einem Satz auf Deutsch zusammen, was er erreicht hat.

Falls der Subagent FEHLGESCHLAGEN ist oder einen Fehler zurueckgegeben hat:
1. Schreibe eine Sentinel-Datei: `$env:TEMP/agent-writeback-subagent-failure.json`
   Format: {"agent": "subagent-failure", "section": "Offene Fehler & Probleme", "findings": "[Beschreibung]", "timestamp": "[ISO-Datum]"}
2. Die 'findings' muessen enthalten: Agent-Name, Symptom, Root Cause, betroffene Dateien, konkreter Fix-Vorschlag.
3. Fehler von ALLEN Quellen (custom agents, plugins, marketplace agents) MUESSEN geloggt werden.
4. NIEMALS direkt in MEMORY.md schreiben — immer die Sentinel-Datei verwenden.
"@

Write-Output $prompt
exit 0
