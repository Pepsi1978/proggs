# Disk Guard: Warns when storage is critically low
# Hook event: SessionStart
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"

try {
    $drive = Get-PSDrive C -ErrorAction Stop
    $freeGB = [math]::Round($drive.Free / 1GB, 1)
    $usedGB = [math]::Round($drive.Used / 1GB, 1)
    $totalGB = [math]::Round(($drive.Free + $drive.Used) / 1GB, 1)
    $usagePct = [math]::Round(($usedGB / $totalGB) * 100, 0)
} catch {
    Hook-LogError "failed to read disk space: $_"
    exit 0
}

if ($usagePct -ge 95) {
    Hook-LogError "CRITICAL disk space: ${usagePct}% used, ${freeGB}GB free"
    Write-Output "WARNUNG: Speicherplatz KRITISCH bei ${usagePct}% — nur ${freeGB}GB frei!"
    Write-Output "Empfehlung: Temporaere Dateien loeschen oder Speicher freigeben."
} elseif ($usagePct -ge 90) {
    Hook-LogWarn "disk space low: ${usagePct}% used, ${freeGB}GB free"
    Write-Output "Disk-Guard: Speicherplatz bei ${usagePct}% — ${freeGB}GB frei (Achtung)."
} else {
    Hook-Log "disk OK: ${usagePct}% used, ${freeGB}GB free"
}

exit 0
