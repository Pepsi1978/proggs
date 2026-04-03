# path-health-check.ps1 -- Lightweight PATH health check at session start
# Event: SessionStart (runs every session, must be fast < 2 seconds)
# Platform: Windows (PowerShell 5.1+)
#
# PURPOSE: Detects orphaned Python installations and ghost PATH entries
# that would cause cryptic errors later. Does NOT do full tool verification
# (that's path-verify.ps1 for manual use after shell updates).
#
# POKA-YOKE STUFE 3: Automatic detection at every session start.
# The user never has to remember to check -- the system does it for them.
#
# Root Cause (2026-04-03): Switching from official Python installer to uv
# left pip.exe without python.exe. pip install failed with cryptic
# "Fatal error in launcher: Unable to create process". This check would
# have caught it immediately at the next session start.

$issues = @()

# ===== Check 1: Orphaned Python (pip.exe without python.exe) =====
# Uses persistent PATH (not process PATH) to avoid stale cache false positives
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
                $issues += "ORPHANED PYTHON: $dir hat pip.exe aber $parentDir hat KEIN python.exe. pip install wird fehlschlagen! Fix: pwsh ~/.claude/hooks/path-verify.ps1 -Fix"
            }
        }
    }
}

# ===== Check 2: Ghost PATH entries (non-existent tool directories) =====
foreach ($dir in $persistentDirs) {
    if ($dir -and $dir -notmatch '^\$|^%') {
        $exists = try { Test-Path $dir -ErrorAction Stop } catch { $true }
        if (-not $exists -and $dir -match 'Python|node|npm|cargo|rustup|Go\\|gradle|Kotlin|Android|bun') {
            $issues += "GHOST-PATH: $dir existiert nicht mehr (toter Eintrag im PATH)"
        }
    }
}

# ===== Output =====
if ($issues.Count -gt 0) {
    $msg = "PATH-Health-Check: $($issues.Count) Problem(e) gefunden:`n"
    foreach ($issue in $issues) {
        $msg += "  - $issue`n"
    }
    $msg += "Reparatur: pwsh ~/.claude/hooks/path-verify.ps1 -Fix"
    Write-Output $msg
} else {
    Write-Output "PATH-Health-Check: OK"
}

exit 0
