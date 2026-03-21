# Disk Guard: Warns when storage is critically low
# Hook event: SessionStart
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"
. "$PSScriptRoot/whiteboard-insert.ps1"

try {
    $drive = Get-PSDrive C -ErrorAction Stop
    $freeGB = [math]::Round($drive.Free / 1GB, 1)
    $usedGB = [math]::Round($drive.Used / 1GB, 1)
    $totalGB = [math]::Round(($drive.Free + $drive.Used) / 1GB, 1)
    # Calculate percentage directly from raw bytes to avoid double-rounding via intermediate GB values
    $usagePct = [math]::Round(($drive.Used / ($drive.Free + $drive.Used)) * 100, 0)
} catch {
    Hook-LogError "failed to read disk space: $_"
    exit 0
}

if ($usagePct -ge 95) {
    Hook-LogError "CRITICAL disk space: ${usagePct}% used, ${freeGB}GB free"
    Write-Output "WARNUNG: Speicherplatz KRITISCH bei ${usagePct}% — nur ${freeGB}GB frei!"
    Write-Output "Empfehlung: Temporaere Dateien loeschen oder Speicher freigeben."
    # Log to whiteboard — wrapped in try/catch to prevent circular failure on full disk
    try {
        $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') — Hook: disk-guard.ps1 — Speicherplatz KRITISCH bei ${usagePct}%"
        Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
    } catch {
        Hook-LogWarn "Could not write disk warning to whiteboard (disk may be full)"
    }
} elseif ($usagePct -ge 90) {
    Hook-LogWarn "disk space low: ${usagePct}% used, ${freeGB}GB free"
    Write-Output "Disk-Guard: Speicherplatz bei ${usagePct}% — ${freeGB}GB frei (Achtung)."
} else {
    Hook-Log "disk OK: ${usagePct}% used, ${freeGB}GB free"
}

exit 0
