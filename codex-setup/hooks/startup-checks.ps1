# startup-checks.ps1 — Consolidated SessionStart Health Checks
# Event: SessionStart
# Type: command
# Platform: Windows (PowerShell 7+)
#
# CONSOLIDATION (2026-04-12): Vereint 6 einfache SessionStart-Hooks in einer Datei:
#   1. disk-guard        — Speicherplatz pruefen
#   2. path-health-check — PATH auf Orphans und Ghost-Eintraege pruefen
#   3. doctor-lite       — Cloud-MCP, .mcp.json, sequential-thinking
#   4. semantic-search    — Code-Search MCP installiert?
#   5. mcp-auth-check    — MCP-Server-Authentifizierung pruefen
#   6. mirror-check      — Mirror-Ledger ausstehende Eintraege
#
# Jede Pruefung ist eine eigenstaendige Funktion die unabhaengig von den anderen laeuft.
# Wenn eine Pruefung fehlschlaegt, laufen die restlichen trotzdem weiter.
#
# ROBUSTNESS: Non-critical hook. Jeder Fehler → weitermachen. Am Ende immer exit 0.

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }
try { . "$PSScriptRoot/whiteboard-insert.ps1" } catch { }

$warnings = @()
$okChecks = @()

# ============================================================
# CHECK 1: Speicherplatz (ehemals disk-guard.ps1)
# ============================================================
function Check-DiskSpace {
    try {
        $drive = Get-PSDrive C -ErrorAction Stop
        $freeGB = [math]::Round($drive.Free / 1GB, 1)
        $usagePct = [math]::Round(($drive.Used / ($drive.Free + $drive.Used)) * 100, 0)

        if ($usagePct -ge 95) {
            $script:warnings += "Speicherplatz KRITISCH: ${usagePct}% belegt, nur ${freeGB}GB frei!"
            try {
                $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') — Hook: startup-checks.ps1 — Speicherplatz KRITISCH bei ${usagePct}%"
                Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
            } catch { }
        } elseif ($usagePct -ge 90) {
            $script:warnings += "Speicherplatz bei ${usagePct}% — ${freeGB}GB frei (Achtung)"
        } else {
            $script:okChecks += "Disk: ${freeGB}GB frei (${usagePct}%)"
        }
    } catch {
        $script:warnings += "Disk-Check fehlgeschlagen: $_"
    }
}

# ============================================================
# CHECK 2: PATH-Gesundheit (ehemals path-health-check.ps1)
# ============================================================
function Check-PathHealth {
    $issues = @()
    $machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $persistentDirs = (($machinePath + ';' + $userPath) -split ';') | Where-Object { $_ -ne '' }

    foreach ($dir in $persistentDirs) {
        $dirOk = try { Test-Path $dir -ErrorAction Stop } catch { $false }
        if ($dirOk -and $dir -match 'Python.*Scripts') {
            $pipPath = Join-Path $dir 'pip.exe'
            if (Test-Path $pipPath) {
                $parentDir = Split-Path $dir -Parent
                $pythonPath = Join-Path $parentDir 'python.exe'
                if (-not (Test-Path $pythonPath)) {
                    $issues += "ORPHANED PYTHON: $dir hat pip.exe aber kein python.exe"
                }
            }
        }
    }

    foreach ($dir in $persistentDirs) {
        if ($dir -and $dir -notmatch '^\$|^%') {
            $exists = try { Test-Path $dir -ErrorAction Stop } catch { $true }
            if (-not $exists -and $dir -match 'Python|node|npm|cargo|rustup|Go\\|gradle|Kotlin|Android|bun') {
                $issues += "GHOST-PATH: $dir existiert nicht mehr"
            }
        }
    }

    if ($issues.Count -gt 0) {
        foreach ($issue in $issues) {
            $script:warnings += "PATH: $issue"
        }
    } else {
        $script:okChecks += "PATH: OK"
    }
}

# ============================================================
# CHECK 3: Doctor-Lite (ehemals doctor-lite.ps1)
# ============================================================
function Check-DoctorLite {
    $localWarnings = 0
    $claudeJson = "$env:USERPROFILE\.claude.json"
    if (Test-Path $claudeJson) {
        $raw = Get-Content $claudeJson -Raw -ErrorAction SilentlyContinue

        # Cloud MCP connectors active but not blocked?
        $flagActive = $raw -match '"tengu_claudeai_mcp_connectors":\s*true'
        $hasBlocker = $raw -match '"disabledMcpjsonServers":\s*\[' -and $raw -match '"claude\.ai'
        if ($flagActive -and -not $hasBlocker) {
            $script:warnings += "Cloud-MCP-Connectoren aktiv und NICHT blockiert (~21K Token)"
            $localWarnings++
        }

        # sequential-thinking sneaked back?
        if ($raw -match '"sequential-thinking"') {
            $script:warnings += "sequential-thinking MCP-Server ist wieder konfiguriert (redundant)"
            $localWarnings++
        }
    }

    # Check .mcp.json for macOS paths on Windows
    $mcpJson = "$env:USERPROFILE\.mcp.json"
    if (Test-Path $mcpJson) {
        $macosServers = @()
        $serverName = ""
        foreach ($line in (Get-Content $mcpJson)) {
            if ($line -match '"(\w[\w-]+)":\s*\{') { $serverName = $Matches[1] }
            if ($serverName -ne "code-search" -and $line -match '/opt/homebrew/') {
                $macosServers += $serverName
            }
        }
        if ($macosServers.Count -gt 0) {
            $script:warnings += ".mcp.json: macOS-Pfade auf Windows: $($macosServers -join ', ')"
            $localWarnings++
        }
    }

    if ($localWarnings -eq 0) {
        $script:okChecks += "Doctor-Lite: OK"
    }
}

# ============================================================
# CHECK 4: Semantic Search (ehemals semantic-search-check.ps1)
# ============================================================
function Check-SemanticSearch {
    $mcpDir = Join-Path $env:USERPROFILE "proggs\mcp-code-search"
    if (-not (Test-Path $mcpDir)) {
        $script:okChecks += "Semantic-Search: Nicht installiert (optional)"
        return
    }
    $nodeModules = Join-Path $mcpDir "node_modules"
    if (-not (Test-Path $nodeModules)) {
        $script:warnings += "Semantic-Search: node_modules fehlen. Fix: cd ~/Codex/mcp-code-search && bun install"
        return
    }
    $script:okChecks += "Semantic-Search: OK"
}

# ============================================================
# CHECK 5: MCP-Auth (DISABLED 2026-04-12)
# ============================================================
# REMOVED: "codex mcp list" spawns orphan MCP server processes on every call.
# This was a root cause of the process leak and session destruction bug.
# The check provided minimal value (just showing auth status) while actively
# making the system worse by spawning 2-5 orphan node.exe processes per session start.
# See observation #4072: "MCP Server Duplication Root Cause: codex mcp list Spawns Orphans"
function Check-McpAuth {
    $script:okChecks += "MCP-Auth: Pruefung deaktiviert (spawnte Orphans)"
}

# ============================================================
# CHECK 6: Mirror-Ledger (ehemals mirror-check.ps1)
# ============================================================
function Check-MirrorLedger {
    $ledger = Join-Path $env:USERPROFILE "proggs\codex-harness\mirror-ledger.md"
    if (-not (Test-Path $ledger)) { return }

    $pattern = "APPLIED: windows/codex=PENDING"
    $count = (Select-String -Pattern ([regex]::Escape($pattern)) -Path $ledger -SimpleMatch -ErrorAction SilentlyContinue | Measure-Object).Count
    if ($null -eq $count) { $count = 0 }

    if ($count -gt 0) {
        $script:warnings += "Mirror-Bridge: $count ausstehende Eintraege von anderen Plattformen"
    }
}

# ============================================================
# MAIN: Alle Checks ausfuehren
# ============================================================

# Reihenfolge: Schnelle lokale Checks zuerst, langsamer externer Check (MCP-Auth) zuletzt.
# So bekommt der Benutzer die lokalen Ergebnisse sofort, auch wenn MCP-Auth haengt.
Check-DiskSpace
Check-PathHealth
Check-DoctorLite
Check-SemanticSearch
Check-MirrorLedger
Check-McpAuth  # LETZTER Check — ruft codex mcp list auf (kann bis zu 5s dauern)

# ============================================================
# OUTPUT: Kompakte Zusammenfassung
# ============================================================

if ($warnings.Count -gt 0) {
    Write-Output "Startup-Checks: $($warnings.Count) Problem(e):"
    foreach ($w in $warnings) {
        Write-Output "  ! $w"
    }
}

# Show OK summary only if no warnings (keep output clean)
if ($warnings.Count -eq 0 -and $okChecks.Count -gt 0) {
    Write-Output "Startup-Checks: Alle OK ($($okChecks.Count) Pruefungen bestanden)"
}

exit 0
