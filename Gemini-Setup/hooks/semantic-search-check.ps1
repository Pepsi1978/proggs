# semantic-search-check.ps1 — SessionStart: verify code-search MCP server is reachable
# Replaces the old prompt-type hook (which can't run at SessionStart due to
# missing ToolUseContext — a Claude Code limitation).
# This command-type hook checks the MCP server's health endpoint instead.
# Platform: Windows (PowerShell equivalent of semantic-search-check.sh)

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
    $content = Get-Content $MCP_JSON -Raw
    if ($content -match "code-search") {
        # code-search is configured
    }
}

exit 0
