# doctor-lite.ps1 — Lightweight SessionStart health check
# Checks: cloud MCP feature flag, .mcp.json sanity, dead MCP servers
# Created: 2026-03-26 | Runs in < 2 seconds

$warnings = @()

# 1. Check if cloud MCP connectors are active despite disabledMcpjsonServers
#    The feature flag gets reset by the server — disabledMcpjsonServers is the resilient blocker.
#    If BOTH are true (flag=true AND disabledMcpjsonServers empty), warn.
$claudeJson = "$env:USERPROFILE\.claude.json"
if (Test-Path $claudeJson) {
    $raw = Get-Content $claudeJson -Raw -ErrorAction SilentlyContinue
    $flagActive = $raw -match '"tengu_claudeai_mcp_connectors":\s*true'
    $hasBlocker = $raw -match '"disabledMcpjsonServers":\s*\[' -and $raw -match '"claude\.ai'
    if ($flagActive -and -not $hasBlocker) {
        $warnings += "Cloud-MCP-Connectoren sind aktiv und NICHT blockiert (~21K Token). Entweder auf claude.ai > Settings > Integrations trennen oder disabledMcpjsonServers in .claude.json pruefen."
    }
}

# 2. Check .mcp.json for macOS paths on Windows (ignore code-search — intentionally macOS per isolation rule)
$mcpJson = "$env:USERPROFILE\.mcp.json"
if (Test-Path $mcpJson) {
    $raw = Get-Content $mcpJson -Raw -ErrorAction SilentlyContinue
    # Count servers with macOS paths, excluding code-search (protected by semantic-search-isolation rule)
    $macosServers = @()
    $inServer = $false
    $serverName = ""
    foreach ($line in (Get-Content $mcpJson)) {
        if ($line -match '"(\w[\w-]+)":\s*\{') {
            $serverName = $Matches[1]
        }
        if ($serverName -ne "code-search" -and $line -match '/opt/homebrew/') {
            $macosServers += $serverName
        }
    }
    if ($macosServers.Count -gt 0) {
        $warnings += "~/.mcp.json: Server mit macOS-Pfaden auf Windows: $($macosServers -join ', ')"
    }
}

# 3. Check sequential-thinking not sneaked back in
if (Test-Path $claudeJson) {
    $raw = Get-Content $claudeJson -Raw -ErrorAction SilentlyContinue
    if ($raw -match '"sequential-thinking"') {
        $warnings += "sequential-thinking MCP-Server ist wieder konfiguriert (redundant mit eingebautem Thinking)."
    }
}

# Output
if ($warnings.Count -gt 0) {
    Write-Output "Doctor-Lite: $($warnings.Count) Problem(e):"
    foreach ($w in $warnings) {
        Write-Output "  ! $w"
    }
} else {
    Write-Output "Doctor-Lite: Alle Checks OK."
}
exit 0
