# research-approval: Blockt Web-Recherche-Aufrufe bis eine Freigabe-Flag-Datei existiert.
# Setzt research-strategy.md durch (Frage 1 A/B/C/D muss Frank beantworten, bevor mm/or-research
# oder die Firecrawl-MCP laufen). Verbraucht sonst still Firecrawl-Credits / teure Tokens.
# Runs as PreToolUse hook (matcher: Bash | mcp__.*firecrawl.*)
# stdout -> AI context (nur DENY-JSON), stderr -> user terminal. Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    # stdin robust lesen (dual-read, §12.4)
    $raw = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($raw)) { $raw = ($input | Out-String) }
    if ([string]::IsNullOrWhiteSpace($raw)) { exit 0 }   # kein Input -> normaler Flow (FAIL-OPEN)

    $rawLower = $raw.ToLower()

    # --- Research-Erkennung (raw-substring; KEIN jq/strict-Parse-Zwang, umgeht §16.2) ---
    $isResearch = $false
    if ($rawLower -match 'mm-research\.(py|sh)' -or $rawLower -match 'or-research\.(py|sh)') {
        $isResearch = $true
    }
    if (-not $isResearch) {
        # Firecrawl-MCP-Tool? tool_name via JSON (best effort) + raw-Fallback
        $toolName = ""
        try { $toolName = ($raw | ConvertFrom-Json).tool_name } catch { $toolName = "" }
        if ($toolName -and $toolName.ToLower().Contains('firecrawl')) { $isResearch = $true }
        elseif ($rawLower -match 'mcp__[a-z0-9_]*firecrawl') { $isResearch = $true }
    }

    if (-not $isResearch) { exit 0 }   # kein Research-Aufruf -> normaler Flow

    # --- Freigabe-Flag pruefen (Existenz + TTL 30 Min) ---
    $ttl = 1800
    $flag = Join-Path $env:TEMP "research-approved.flag"
    if (Test-Path $flag) {
        $age = (New-TimeSpan -Start (Get-Item $flag).LastWriteTime -End (Get-Date)).TotalSeconds
        if ($age -ge 0 -and $age -lt $ttl) { exit 0 }   # Freigabe aktiv -> normaler Flow
    }

    # --- Keine Freigabe -> DENY (spec-konform, exit 0 + JSON; §16.1, §1.6) ---
    $tmp = $env:TEMP
    $reason = "RESEARCH-FREIGABE FEHLT (Regel research-strategy.md). Vor jeder Web-Recherche MUSS Frank per AskUserQuestion gefragt werden -- Frage 1: A=Firecrawl+MiniMax M3 (max Thinking), B=MiniMax+parallel (max Thinking), C=Opus-Schwarm, D=Freitext. Nach Wahl A oder B die Freigabe setzen (Bash): touch '$tmp/research-approved.flag' (gilt 30 Min), dann den Aufruf erneut starten. Bei C laeuft KEIN mm/or-research (Opus-Researcher stattdessen)."
    $deny = @{ hookSpecificOutput = @{ hookEventName = "PreToolUse"; permissionDecision = "deny"; permissionDecisionReason = $reason } }
    Write-Output ($deny | ConvertTo-Json -Depth 5 -Compress)
    exit 0

} catch {
    # FAIL-OPEN: interner Fehler darf die Session nie blockieren
    Hook-LogWarn "research-approval: $_"
}

exit 0
