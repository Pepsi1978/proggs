# Session Guard: Verifies bypass permissions and workspace directory on every session start
# Hook event: SessionStart
# Platform: Windows (PowerShell 7+)
#
# ROBUSTNESS: Entire script wrapped in try/catch. Any failure → exit 0 (never block session).
#
# Output: stdout → AI context (system-reminder), stderr → user terminal

$ErrorActionPreference = 'SilentlyContinue'

try { . "$PSScriptRoot/hook-log.ps1" } catch { }
try { . "$PSScriptRoot/whiteboard-insert.ps1" } catch { }

function Write-Status {
    param([string]$Message)
    Write-Output $Message
    [Console]::Error.WriteLine($Message)
}

$warnings = @()
$fixes = @()

# =============================================
# CHECK 1: Bypass Permissions — MUST be active
# =============================================

$settingsPath = Join-Path $env:USERPROFILE ".claude" "settings.json"
$localSettingsPath = Join-Path $env:USERPROFILE ".claude" "settings.local.json"

# Check global settings.json
try {
    if (Test-Path $settingsPath) {
        $settings = Get-Content $settingsPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $mode = $settings.permissions.defaultMode
        if ($mode -ne 'bypassPermissions') {
            # AUTO-REPAIR: Fix the mode
            $raw = Get-Content $settingsPath -Raw -Encoding UTF8
            if ($raw -match '"defaultMode"\s*:\s*"[^"]*"') {
                $fixed = $raw -replace '"defaultMode"\s*:\s*"[^"]*"', '"defaultMode": "bypassPermissions"'
                $tmpFile = "$settingsPath.tmp"
                [System.IO.File]::WriteAllText($tmpFile, $fixed, [System.Text.Encoding]::UTF8)
                Move-Item -Path $tmpFile -Destination $settingsPath -Force
                $fixes += "settings.json defaultMode repariert (war: $mode)"
                Hook-Log "AUTO-FIX: defaultMode restored to bypassPermissions (was: $mode)"
            }
        }
    }
} catch {
    Hook-LogWarn "settings.json check failed: $_"
}

# Check settings.local.json — must also have bypassPermissions
try {
    if (Test-Path $localSettingsPath) {
        $localSettings = Get-Content $localSettingsPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $localMode = $localSettings.permissions.defaultMode
        if ($localMode -ne 'bypassPermissions') {
            # AUTO-REPAIR
            $localData = Get-Content $localSettingsPath -Raw -Encoding UTF8 | ConvertFrom-Json -AsHashtable
            if (-not $localData.ContainsKey("permissions")) { $localData["permissions"] = @{} }
            $localData["permissions"]["defaultMode"] = "bypassPermissions"
            $tmpFile = "$localSettingsPath.tmp"
            $localData | ConvertTo-Json -Depth 10 | Out-File -FilePath $tmpFile -Encoding UTF8 -NoNewline
            Move-Item -Path $tmpFile -Destination $localSettingsPath -Force
            $fixes += "settings.local.json defaultMode repariert (war: $localMode)"
            Hook-Log "AUTO-FIX: settings.local.json defaultMode restored to bypassPermissions"
        }
    }
} catch {
    Hook-LogWarn "settings.local.json check failed: $_"
}

# Check ALL project-level settings
try {
    $projectsDir = Join-Path $env:USERPROFILE ".claude" "projects"
    if (Test-Path $projectsDir) {
        $projectDirs = Get-ChildItem $projectsDir -Directory
        foreach ($pdir in $projectDirs) {
            $pLocalSettings = Join-Path $pdir.FullName "settings.local.json"
            if (Test-Path $pLocalSettings) {
                $pData = Get-Content $pLocalSettings -Raw -Encoding UTF8 | ConvertFrom-Json
                $pMode = $pData.permissions.defaultMode
                if ($null -ne $pMode -and $pMode -ne 'bypassPermissions') {
                    # AUTO-REPAIR
                    $pHash = Get-Content $pLocalSettings -Raw -Encoding UTF8 | ConvertFrom-Json -AsHashtable
                    if (-not $pHash.ContainsKey("permissions")) { $pHash["permissions"] = @{} }
                    $pHash["permissions"]["defaultMode"] = "bypassPermissions"
                    $tmpFile = "$pLocalSettings.tmp"
                    $pHash | ConvertTo-Json -Depth 10 | Out-File -FilePath $tmpFile -Encoding UTF8 -NoNewline
                    Move-Item -Path $tmpFile -Destination $pLocalSettings -Force
                    $fixes += "Projekt $($pdir.Name) defaultMode repariert"
                    Hook-Log "AUTO-FIX: $($pdir.Name) defaultMode restored"
                }
            } else {
                # Project has no settings.local.json — create one with bypassPermissions
                $newSettings = @{ permissions = @{ defaultMode = "bypassPermissions" } }
                $tmpFile = "$pLocalSettings.tmp"
                $newSettings | ConvertTo-Json -Depth 10 | Out-File -FilePath $tmpFile -Encoding UTF8 -NoNewline
                Move-Item -Path $tmpFile -Destination $pLocalSettings -Force
                $fixes += "Projekt $($pdir.Name) settings.local.json erstellt"
                Hook-Log "AUTO-FIX: $($pdir.Name) settings.local.json created with bypassPermissions"
            }

            # Also check settings.json (not local)
            $pSettings = Join-Path $pdir.FullName "settings.json"
            if (Test-Path $pSettings) {
                $psData = Get-Content $pSettings -Raw -Encoding UTF8 | ConvertFrom-Json
                $psMode = $psData.permissions.defaultMode
                if ($null -ne $psMode -and $psMode -ne 'bypassPermissions') {
                    $psHash = Get-Content $pSettings -Raw -Encoding UTF8 | ConvertFrom-Json -AsHashtable
                    if (-not $psHash.ContainsKey("permissions")) { $psHash["permissions"] = @{} }
                    $psHash["permissions"]["defaultMode"] = "bypassPermissions"
                    $tmpFile = "$pSettings.tmp"
                    $psHash | ConvertTo-Json -Depth 10 | Out-File -FilePath $tmpFile -Encoding UTF8 -NoNewline
                    Move-Item -Path $tmpFile -Destination $pSettings -Force
                    $fixes += "Projekt $($pdir.Name) settings.json defaultMode repariert"
                }
            }
        }
    }
} catch {
    Hook-LogWarn "project settings check failed: $_"
}

# ================================================
# CHECK 2: Workspace Directory — MUST be ~/proggs/
# ================================================

# Claude Code sets CWD before hooks run. We can check it via the process.
# However, hooks run in their own process. We check via environment hints.
# The most reliable check: Does ~/proggs/.git exist? (Always true)
# The session goal file tracks the actual CWD.
$goalFile = Join-Path $env:TEMP "claude-session-goal.txt"
$expectedDir = Join-Path $env:USERPROFILE "proggs"

# Write a CWD check marker that the AI can read
$cwdCheckFile = Join-Path $env:TEMP "claude-cwd-check.txt"
"EXPECTED_CWD=$expectedDir" | Out-File -FilePath $cwdCheckFile -Encoding UTF8 -NoNewline

# =============================================
# REPORT
# =============================================

if ($fixes.Count -gt 0) {
    $fixStr = $fixes -join "; "
    $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') — Hook: session-guard.ps1 — Auto-Reparatur: $fixStr — Status: AUTO-GEFIXT"
    try { Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry } catch { }
    Write-Status "SessionGuard: AUTO-REPARATUR: $fixStr"
} else {
    Write-Status "SessionGuard: bypassPermissions aktiv auf allen Ebenen."
}

# Always output the workspace reminder for the AI context
Write-Output "SessionGuard: WORKSPACE_CHECK — Arbeitsverzeichnis MUSS C:\Users\barwa\proggs sein. Falls pwd=/c/Users/barwa: WARNUNG an Benutzer!"

exit 0
