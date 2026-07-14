# uninstall_autostart.ps1 — Entfernt den Autostart fuer TerminalVoiceOverlay
# Ausfuehren: pwsh -File uninstall_autostart.ps1

$ErrorActionPreference = "Stop"
$taskName = "Spracheingabe - Terminal"
$startup = [Environment]::GetFolderPath("Startup")
$oldLinks = @("TerminalVoiceOverlay.lnk", "Spracheingabe - Terminal.lnk")
$failures = [System.Collections.Generic.List[string]]::new()
$removedAny = $false

try {
    $task = @(Get-ScheduledTask -ErrorAction Stop | Where-Object { $_.TaskName -eq $taskName })
    if ($task.Count -gt 0) {
        Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction Stop
        Write-Host "Geplante Aufgabe entfernt: $taskName" -ForegroundColor Green
        $removedAny = $true
    }
} catch {
    $failures.Add("Geplante Aufgabe '$taskName': $($_.Exception.Message)")
}

foreach ($link in $oldLinks) {
    $linkPath = Join-Path $startup $link
    try {
        if (Test-Path -LiteralPath $linkPath) {
            Remove-Item -LiteralPath $linkPath -Force -ErrorAction Stop
            Write-Host "Autostart-Verknuepfung entfernt: $link" -ForegroundColor Green
            $removedAny = $true
        }
    } catch {
        $failures.Add("Verknuepfung '$linkPath': $($_.Exception.Message)")
    }
}

if ($failures.Count -gt 0) {
    throw "Autostart konnte nicht vollstaendig entfernt werden:`n$($failures -join "`n")"
}

if (-not $removedAny) {
    Write-Host "Kein Autostart-Eintrag gefunden." -ForegroundColor Yellow
}
