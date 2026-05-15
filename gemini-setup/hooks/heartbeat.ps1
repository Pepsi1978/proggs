# heartbeat.ps1 — SessionStart Health Check (Windows)
# Windows-Portierung der macOS heartbeat.sh Session-Score-Checks.
# Laeuft bei jedem SessionStart (statt alle 15min via launchd auf macOS).
#
# Checks:
#   1. Disk space (WARNING if >90%, CRITICAL if >95%)
#   2. Config file integrity (settings.json, .mcp.json)
#   3. Whiteboard exists and is readable
#   4. Key directories exist
#   5. Validator compliance (last 3 session scores >= 3.0)
#   6. Length drift (monotonically falling scores)
#   7. Session regression (recent avg vs baseline avg)
#   8. Behavioral drift (corrections > 3 in last session)
#
# Output: Writes status to ~/.Gemini/heartbeat-status.json (atomic write)
# Notifications: Toast notification for CRITICAL issues (Windows 10+)
#
# Defense in Depth (Directive #3):
#   - Every check wrapped in try/catch
#   - Script ALWAYS exits 0 (never blocks session start)
#   - Atomic JSON write (temp file + Move-Item)
#   - Graceful degradation if data missing

$ErrorActionPreference = 'SilentlyContinue'

$heartbeatDir = Join-Path $env:USERPROFILE ".Gemini"
$statusFile = Join-Path $heartbeatDir "heartbeat-status.json"
$logDir = Join-Path $heartbeatDir "logs\heartbeat"
$logFile = Join-Path $logDir "$(Get-Date -Format 'yyyy-MM-dd').log"
$proggsDir = Join-Path $env:USERPROFILE "proggs"
$whiteboard = Join-Path $proggsDir ".Gemini\agent-memory\shared\MEMORY.md"
$settingsJson = Join-Path $env:USERPROFILE ".Gemini\settings.json"
$settingsLocal = Join-Path $env:USERPROFILE ".Gemini\settings.local.json"
$mcpJson = Join-Path $proggsDir ".mcp.json"
$scoresFile = Join-Path $env:USERPROFILE ".Gemini\session-scores.jsonl"

# Setup
try { New-Item -ItemType Directory -Path $logDir -Force | Out-Null } catch {}

function Log-Msg([string]$msg) {
    try { Add-Content -Path $logFile -Value "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg" -Encoding UTF8 } catch {}
}

function Notify-Critical([string]$msg) {
    Log-Msg "CRITICAL: $msg"
    # Windows toast notification (best-effort, non-blocking)
    try {
        [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
        $template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent(
            [Windows.UI.Notifications.ToastTemplateType]::ToastText02)
        $textNodes = $template.GetElementsByTagName("text")
        $textNodes.Item(0).AppendChild($template.CreateTextNode("Gemini Heartbeat KRITISCH")) | Out-Null
        $textNodes.Item(1).AppendChild($template.CreateTextNode($msg)) | Out-Null
        $toast = [Windows.UI.Notifications.ToastNotification]::new($template)
        [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier("Gemini CLI").Show($toast)
    } catch {
        # Fallback: BurntToast or just log (notification failure is OK)
    }
}

# ── Check functions ──

function Check-DiskSpace {
    try {
        $drive = Get-PSDrive C -ErrorAction Stop
        $usedPct = [math]::Round(($drive.Used / ($drive.Used + $drive.Free)) * 100)
        $freeGB = [math]::Round($drive.Free / 1GB, 1)
        if ($usedPct -ge 95) {
            Notify-Critical "Speicherplatz KRITISCH: ${usedPct}% belegt, nur ${freeGB}GB frei!"
            return @{ status = "CRITICAL"; detail = "${usedPct}% used, ${freeGB}GB free" }
        } elseif ($usedPct -ge 90) {
            return @{ status = "WARNING"; detail = "${usedPct}% used, ${freeGB}GB free" }
        } else {
            return @{ status = "OK"; detail = "${usedPct}% used" }
        }
    } catch {
        return @{ status = "UNKNOWN"; detail = "Could not read disk usage" }
    }
}

function Check-ConfigIntegrity {
    $issues = 0
    $details = ""

    foreach ($cfg in @($settingsJson, $settingsLocal, $mcpJson)) {
        if (Test-Path $cfg) {
            try {
                Get-Content $cfg -Raw -Encoding UTF8 | ConvertFrom-Json | Out-Null
            } catch {
                $issues++
                $name = Split-Path $cfg -Leaf
                $details += "$name CORRUPT; "
                Notify-Critical "$name ist beschaedigt!"
            }
        } elseif ($cfg -eq $settingsJson) {
            $issues++
            $details += "settings.json MISSING; "
        }
    }

    if ($issues -gt 0) {
        return @{ status = "CRITICAL"; detail = $details.TrimEnd("; ") }
    }
    return @{ status = "OK"; detail = "All config files valid" }
}

function Check-Whiteboard {
    if (-not (Test-Path $whiteboard)) {
        Notify-Critical "Whiteboard (MEMORY.md) fehlt!"
        return @{ status = "CRITICAL"; detail = "MEMORY.md missing" }
    }
    $lines = (Get-Content $whiteboard -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
    if ($lines -eq 0) {
        return @{ status = "WARNING"; detail = "MEMORY.md is empty" }
    }
    return @{ status = "OK"; detail = "$lines lines" }
}

function Check-KeyDirectories {
    $missing = 0
    $details = ""
    foreach ($dir in @(
        (Join-Path $env:USERPROFILE ".Gemini\hooks"),
        (Join-Path $env:USERPROFILE ".Gemini\agents"),
        (Join-Path $env:USERPROFILE ".Gemini\rules"),
        (Join-Path $proggsDir ".Gemini\agent-memory\shared")
    )) {
        if (-not (Test-Path $dir)) {
            $missing++
            $details += "$(Split-Path $dir -Leaf) MISSING; "
        }
    }
    if ($missing -gt 0) {
        return @{ status = "WARNING"; detail = $details.TrimEnd("; ") }
    }
    return @{ status = "OK"; detail = "All key directories exist" }
}

function Get-SessionScores {
    # Returns array of overall scores from session-scores.jsonl
    if (-not (Test-Path $scoresFile)) { return @() }
    $scores = @()
    foreach ($line in (Get-Content $scoresFile -Encoding UTF8)) {
        if (-not $line.Trim()) { continue }
        try {
            $entry = $line | ConvertFrom-Json
            $overall = $entry.overall
            if ($null -eq $overall) { $overall = $entry.score }
            if ($null -ne $overall) { $scores += [double]$overall }
        } catch {}
    }
    return $scores
}

function Check-ValidatorCompliance {
    $scores = Get-SessionScores
    if ($scores.Count -lt 3) {
        return @{ status = "UNKNOWN"; detail = "Only $($scores.Count) score(s) — need 3" }
    }
    $last3 = $scores[-3..-1]
    $below = $last3 | Where-Object { $_ -lt 3.0 }
    if ($below.Count -eq 0) {
        $avg = ($last3 | Measure-Object -Average).Average
        return @{ status = "OK"; detail = "Last 3 scores all >= 3.0 (avg $([math]::Round($avg, 1)))" }
    } elseif ($below.Count -eq 1) {
        return @{ status = "WARNING"; detail = "1 of last 3 sessions scored below 3.0" }
    } else {
        Notify-Critical "Session-Qualitaet KRITISCH: $($below.Count) von 3 Sessions unter 3.0"
        return @{ status = "CRITICAL"; detail = "$($below.Count) of last 3 sessions scored below 3.0" }
    }
}

function Check-LengthDrift {
    $scores = Get-SessionScores
    if ($scores.Count -lt 3) {
        return @{ status = "UNKNOWN"; detail = "Only $($scores.Count) score(s) — need 3" }
    }
    $last3 = $scores[-3..-1]
    if ($last3[0] -gt $last3[1] -and $last3[1] -gt $last3[2]) {
        return @{ status = "WARNING"; detail = "Monotonically falling scores: $($last3[0]) → $($last3[1]) → $($last3[2])" }
    }
    return @{ status = "OK"; detail = "No consistent downward drift" }
}

function Check-SessionRegression {
    $scores = Get-SessionScores
    if ($scores.Count -lt 8) {
        return @{ status = "UNKNOWN"; detail = "Only $($scores.Count) score(s) — need 8 for regression check" }
    }
    $recent3 = $scores[-3..-1]
    $baseline5 = $scores[(-8)..(-4)]
    $avgRecent = ($recent3 | Measure-Object -Average).Average
    $avgBaseline = ($baseline5 | Measure-Object -Average).Average

    if ($avgRecent -lt $avgBaseline) {
        $delta = [math]::Round($avgBaseline - $avgRecent, 2)
        return @{ status = "WARNING"; detail = "Recent avg $([math]::Round($avgRecent,2)) < baseline avg $([math]::Round($avgBaseline,2)) (delta -$delta)" }
    }
    $delta = [math]::Round($avgRecent - $avgBaseline, 2)
    return @{ status = "OK"; detail = "Recent avg $([math]::Round($avgRecent,2)) >= baseline avg $([math]::Round($avgBaseline,2)) (delta +$delta)" }
}

function Check-BehavioralDrift {
    if (-not (Test-Path $scoresFile)) {
        return @{ status = "UNKNOWN"; detail = "session-scores.jsonl not found" }
    }
    $lastLine = Get-Content $scoresFile -Tail 1 -ErrorAction SilentlyContinue
    if (-not $lastLine) {
        return @{ status = "UNKNOWN"; detail = "No session score entries" }
    }
    try {
        $entry = $lastLine | ConvertFrom-Json
        $corrections = $entry.corrections
        if ($null -eq $corrections) {
            return @{ status = "UNKNOWN"; detail = "No corrections field in last entry" }
        }
        $corrections = [int]$corrections
        if ($corrections -gt 3) {
            return @{ status = "WARNING"; detail = "Last session had $corrections corrections (threshold: 3)" }
        }
        return @{ status = "OK"; detail = "Last session had $corrections correction(s)" }
    } catch {
        return @{ status = "UNKNOWN"; detail = "Cannot parse last session entry" }
    }
}

# ── Run all checks ──
Log-Msg "Heartbeat check started"

try {
    $checks = @{
        disk_space = Check-DiskSpace
        config_integrity = Check-ConfigIntegrity
        whiteboard = Check-Whiteboard
        key_directories = Check-KeyDirectories
        validator_compliance = Check-ValidatorCompliance
        length_drift = Check-LengthDrift
        session_regression = Check-SessionRegression
        behavioral_drift = Check-BehavioralDrift
    }

    # Determine overall status
    $overall = "OK"
    foreach ($check in $checks.Values) {
        if ($check.status -eq "CRITICAL") { $overall = "CRITICAL"; break }
        if ($check.status -eq "WARNING") { $overall = "WARNING" }
    }

    # Build status JSON
    $status = @{
        timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        status = $overall
        checks = $checks
    } | ConvertTo-Json -Depth 4

    # Atomic write: temp file → rename
    $tempStatus = "$statusFile.tmp"
    $status | Set-Content $tempStatus -Encoding UTF8
    # Validate JSON before replacing
    try {
        Get-Content $tempStatus -Raw | ConvertFrom-Json | Out-Null
        Move-Item -Path $tempStatus -Destination $statusFile -Force
    } catch {
        Log-Msg "ERROR: Generated status JSON is invalid — keeping old status file"
        Remove-Item $tempStatus -Force -ErrorAction SilentlyContinue
    }

    Log-Msg "Heartbeat check completed: $overall"

    # Print summary for SessionStart hook output (visible to Gemini)
    $warnings = @()
    foreach ($name in $checks.Keys) {
        $s = $checks[$name].status
        if ($s -eq "CRITICAL" -or $s -eq "WARNING") {
            $warnings += "$name=$s"
        }
    }
    if ($warnings.Count -gt 0) {
        Write-Output "Heartbeat: $($warnings -join ', ')"
    } else {
        Write-Output "Heartbeat: OK"
    }

} catch {
    Log-Msg "ERROR: Heartbeat check failed: $_"
    Write-Output "Heartbeat: ERROR (check failed, see log)"
}

# Log rotation: delete logs older than 14 days
try {
    Get-ChildItem $logDir -Filter "*.log" -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } |
        Remove-Item -Force -ErrorAction SilentlyContinue
} catch {}

# ALWAYS exit 0
exit 0

