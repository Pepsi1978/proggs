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
    Write-Host ($Message)
}

$warnings = @()
$fixes = @()

# Detect SessionStart source (startup, clear, resume, compact, ...)
# Used to decide if effortLevel and model should be reset (only on echtem Neustart).
$sessionSource = ""
try {
    $stdinRaw = [Console]::In.ReadToEnd()
    if ($stdinRaw -and $stdinRaw.Trim() -ne "") {
        $stdinJson = $stdinRaw | ConvertFrom-Json -ErrorAction Stop
        if ($stdinJson.source) { $sessionSource = $stdinJson.source }
    }
} catch { }

# Source-Logging fuer Debugging (Vorschlag 1): Pruefe ob das Feld ankommt
try {
    $sourceLogPath = Join-Path $env:USERPROFILE ".claude" "logs" "session-source.log"
    $logDir = Split-Path $sourceLogPath -Parent
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
    $logEntry = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') | source='$sessionSource' | hasStdin=$([bool]$stdinRaw)"
    Add-Content -Path $sourceLogPath -Value $logEntry -Encoding UTF8
} catch { }

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
                [System.IO.File]::WriteAllText($tmpFile, $fixed, (New-Object System.Text.UTF8Encoding $false))
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
# CHECK 2: Remove allow list from ALL settings files
# ================================================
# An explicit allow list acts as whitelist and BLOCKS tools not on it,
# even with bypassPermissions. Claude Code auto-creates allow entries when
# the user manually approves a tool. Remove from ALL files on every start.

function Remove-AllowList {
    param([string]$Path, [string]$Label)
    try {
        if (Test-Path $Path) {
            $rawJson = Get-Content $Path -Raw -Encoding UTF8
            $data = $rawJson | ConvertFrom-Json
            if ($null -ne $data.permissions.allow) {
                $hashData = $rawJson | ConvertFrom-Json -AsHashtable
                $hashData["permissions"].Remove("allow")
                $tmpFile = "$Path.tmp"
                ($hashData | ConvertTo-Json -Depth 20) | Out-File -FilePath $tmpFile -Encoding UTF8 -NoNewline
                Move-Item -Path $tmpFile -Destination $Path -Force
                $script:fixes += "allow-Liste aus $Label entfernt"
                Hook-Log "AUTO-FIX: removed allow list from $Label"
            }
        }
    } catch {
        Hook-LogWarn "allow list cleanup failed for ${Label}: $_"
    }
}

# Clean settings.json
Remove-AllowList -Path $settingsPath -Label "settings.json"

# Clean settings.local.json
Remove-AllowList -Path $localSettingsPath -Label "settings.local.json"

# Clean ALL project-level settings (both settings.json and settings.local.json)
try {
    $projectsDir = Join-Path $env:USERPROFILE ".claude" "projects"
    if (Test-Path $projectsDir) {
        foreach ($pdir in (Get-ChildItem $projectsDir -Directory)) {
            Remove-AllowList -Path (Join-Path $pdir.FullName "settings.json") -Label "Projekt/$($pdir.Name)/settings.json"
            Remove-AllowList -Path (Join-Path $pdir.FullName "settings.local.json") -Label "Projekt/$($pdir.Name)/settings.local.json"
        }
    }
} catch {
    Hook-LogWarn "project allow list cleanup failed: $_"
}

# ================================================
# CHECK 3: Effort Level — Source-aware Reset
# ================================================
# Frank-Regel seit 2026-05-07:
# - Bei echtem Neustart (source=startup oder clear)  → effortLevel auf "high" zuruecksetzen
# - Bei Auto-Compaction oder Resume (source=compact, resume) → effortLevel UNVERAENDERT lassen
# - Wenn $sessionSource leer ist (Fallback) → NICHT zuruecksetzen (sicherer Default)
#
# Damit bleibt manuell gesetzter Wert (z.B. xhigh per /effort xhigh) waehrend der
# ganzen Session erhalten — auch ueber Auto-Compactions hinweg. Erst bei einem
# echten neuen Claude-Start kommt "high" als Default zurueck.

$shouldResetEffort = ($sessionSource -eq "startup" -or $sessionSource -eq "clear")

try {
    if (Test-Path $settingsPath) {
        $raw = Get-Content $settingsPath -Raw -Encoding UTF8
        $parsed = $raw | ConvertFrom-Json
        if ($shouldResetEffort) {
            # Echter Neustart — auf "high" setzen falls nicht schon
            if ($parsed.effortLevel -and $parsed.effortLevel -ne 'high') {
                $oldEffort = $parsed.effortLevel
                $fixed = $raw -replace '"effortLevel"\s*:\s*"[^"]*"', '"effortLevel": "high"'
                $tmpFile = "$settingsPath.tmp"
                [System.IO.File]::WriteAllText($tmpFile, $fixed, (New-Object System.Text.UTF8Encoding $false))
                Move-Item -Path $tmpFile -Destination $settingsPath -Force
                $fixes += "effortLevel zurueckgesetzt (war: $oldEffort, jetzt: high — Quelle: $sessionSource)"
                Hook-Log "AUTO-FIX: effortLevel reset to high (was: $oldEffort, source: $sessionSource)"
            } elseif (-not $parsed.effortLevel -and $raw -notmatch '"effortLevel"') {
                # Feld fehlt komplett — bei echtem Neustart ergaenzen
                $fixed = $raw -replace '(\s*)"permissions"', "`$1`"effortLevel`": `"high`",`$1`"permissions`""
                if ($fixed -ne $raw) {
                    $tmpFile = "$settingsPath.tmp"
                    [System.IO.File]::WriteAllText($tmpFile, $fixed, (New-Object System.Text.UTF8Encoding $false))
                    Move-Item -Path $tmpFile -Destination $settingsPath -Force
                    $fixes += "effortLevel auf 'high' gesetzt (Feld fehlte)"
                    Hook-Log "AUTO-FIX: effortLevel set to 'high' (was missing)"
                }
            }
        } else {
            # Compaction/Resume/unknown — NICHT anfassen
            if ($parsed.effortLevel) {
                Hook-Log "effortLevel preserved: $($parsed.effortLevel) (source: $sessionSource)"
            }
        }
    }
} catch {
    Hook-LogWarn "effortLevel check failed: $_"
}

# ================================================
# CHECK 4: Model — Source-aware Reset (opus[1m])
# ================================================
# Frank-Regel seit 2026-05-07: gleiches Source-Pattern wie effortLevel.
# - Bei echtem Neustart (source=startup oder clear)  → model auf opus[1m] zuruecksetzen
# - Bei Auto-Compaction oder Resume                  → bestehenden Wert NICHT anfassen
# - Wenn $sessionSource leer ist (Fallback)          → NICHT zuruecksetzen (sicherer Default)
#
# Damit kann Frank per /model mitten in der Session das Modell wechseln (z.B. auf
# sonnet fuer Speed) und der Hook ueberschreibt das nicht ueber Compactions hinweg.
# Wenn das model-Feld komplett fehlt, wird es bei JEDEM Source ergaenzt — sonst
# defaultet Claude Code auf 200k Kontext.

try {
    if (Test-Path $settingsPath) {
        $raw = Get-Content $settingsPath -Raw -Encoding UTF8
        $parsed = $raw | ConvertFrom-Json
        $currentModel = $parsed.model
        if ($null -eq $currentModel -or $currentModel -eq '') {
            # Feld fehlt komplett — IMMER ergaenzen (1M-Kontext-Schutz)
            $hashData = $raw | ConvertFrom-Json -AsHashtable
            $hashData["model"] = "opus[1m]"
            $tmpFile = "$settingsPath.tmp"
            ($hashData | ConvertTo-Json -Depth 20) | Out-File -FilePath $tmpFile -Encoding UTF8 -NoNewline
            Move-Item -Path $tmpFile -Destination $settingsPath -Force
            $fixes += "model hinzugefuegt: opus[1m] (war: nicht gesetzt)"
            Hook-Log "AUTO-FIX: model set to opus[1m] (was missing)"
        } elseif ($currentModel -ne 'opus[1m]' -and $shouldResetEffort) {
            # Falscher Wert UND echter Neustart — auf opus[1m] zuruecksetzen
            $fixed = $raw -replace '"model"\s*:\s*"[^"]*"', '"model": "opus[1m]"'
            $tmpFile = "$settingsPath.tmp"
            [System.IO.File]::WriteAllText($tmpFile, $fixed, (New-Object System.Text.UTF8Encoding $false))
            Move-Item -Path $tmpFile -Destination $settingsPath -Force
            $fixes += "model repariert (war: $currentModel, jetzt: opus[1m] — Quelle: $sessionSource)"
            Hook-Log "AUTO-FIX: model restored to opus[1m] (was: $currentModel, source: $sessionSource)"
        } elseif ($currentModel -ne 'opus[1m]') {
            # Compaction/Resume — Wert respektieren, nur loggen
            Hook-Log "model preserved: $currentModel (source: $sessionSource)"
        }
    }
} catch {
    Hook-LogWarn "model check failed: $_"
}

# ================================================
# CHECK 5: Workspace Directory — MUST be ~/proggs/
# ================================================

$expectedDir = Join-Path $env:USERPROFILE "proggs"
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
