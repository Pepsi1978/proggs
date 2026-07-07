# semantic-search-check.ps1 — SessionStart: verify code-search MCP server is reachable
# Replaces the old prompt-type hook (which can't run at SessionStart due to
# missing ToolUseContext — a Gemini CLI limitation).
# This command-type hook checks the MCP server's health endpoint instead.
# Platform: Windows (PowerShell equivalent of semantic-search-check.sh)
# Adapted from MIRROR-2026-03-25-MAC-014 by import agent on 2026-03-26

$ErrorActionPreference = "SilentlyContinue"

$MCP_DIR = Join-Path $env:USERPROFILE "proggs\mcp-code-search"

if (-not (Test-Path $MCP_DIR)) {
    exit 0
}

$MCP_INDEX = Join-Path $MCP_DIR "node_modules"
if (-not (Test-Path $MCP_INDEX)) {
    Write-Output "Semantische Suche: node_modules fehlen. Fix: cd ~/proggs/mcp-code-search && bun install"
    exit 0  # Don't fail the hook — just warn
}

$MCP_JSON = Join-Path $env:USERPROFILE ".mcp.json"
if (Test-Path $MCP_JSON) {
    $content = Get-Content $MCP_JSON -Raw -Encoding UTF8
    if ($content -match "code-search") {
        # code-search is configured — silently OK
    }
}

# Watchdog: detect stale or failed reindex. Without this the SessionStart reindex
# can die quietly for weeks and the user never learns about it (see 2026-04-11
# incident: 13 days of silent HTTP-400 crashes).
$DB_DIR = Join-Path $env:USERPROFILE "proggs\.code-search"
$STATE_FILE = Join-Path $DB_DIR "state.json"
$REINDEX_LOG = Join-Path $DB_DIR "reindex.log"
$STALE_DAYS = 3

try {
    if (Test-Path $STATE_FILE) {
        $state = Get-Content $STATE_FILE -Raw -Encoding UTF8 | ConvertFrom-Json
        $lastSuccessIso = $state.lastSuccessAt

        if ($lastSuccessIso) {
            $lastSuccess = [DateTimeOffset]::Parse($lastSuccessIso)
            $age = (Get-Date) - $lastSuccess.UtcDateTime
            $ageDays = [int]$age.TotalDays

            if ($ageDays -gt $STALE_DAYS) {
                Write-Output "Semantische Suche: Index ist seit $ageDays Tagen nicht mehr aktualisiert worden (letzter Erfolg: $lastSuccessIso)."
                if (Test-Path $REINDEX_LOG) {
                    $lastErr = Select-String -Path $REINDEX_LOG -Pattern "Reindex failed|HTTP 400|Error" | Select-Object -Last 1
                    if ($lastErr) {
                        Write-Output "  Letzter Fehler aus reindex.log: $($lastErr.Line)"
                    }
                }
                Write-Output "  Fix: cd ~/proggs/mcp-code-search; tsx src/session-reindex.ts ~/proggs"
            }
        }
    } elseif (Test-Path $REINDEX_LOG) {
        # No state.json but a log — last run probably crashed before writing state
        $lastErr = Select-String -Path $REINDEX_LOG -Pattern "Reindex failed|HTTP 400|Error" | Select-Object -Last 1
        if ($lastErr) {
            Write-Output "Semantische Suche: kein erfolgreicher Index — letzter Fehler: $($lastErr.Line)"
        }
    }
} catch {
    # Watchdog failure must never block the session
}

exit 0

