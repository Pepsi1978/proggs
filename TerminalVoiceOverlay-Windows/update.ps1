# update.ps1 — Baut neue Version und ersetzt die laufende .exe
# Ausfuehren: pwsh -File update.ps1
#
# Warum ein eigenes Skript?
# Das Overlay hat einen eingebauten Watchdog der es nach jedem Kill sofort
# wieder startet. Deshalb kann man die .exe nicht einfach ueberschreiben.
# Dieses Skript baut in ein temporaeres Verzeichnis, beendet das Overlay am
# kanonischen Zielpfad, tauscht die .exe aus und startet die neue Version.

$ErrorActionPreference = "Stop"
$publishDir = Join-Path $PSScriptRoot "publish"
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) "TerminalVoiceOverlay-update-$PID-$([Guid]::NewGuid().ToString('N'))"
$exeName = "TerminalVoiceOverlay.exe"
$exePath = Join-Path $publishDir $exeName
$watcherPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "watcher.vbs"))
$tempExe = Join-Path $tempDir $exeName
$newExe = "$exePath.new"
$backupExe = "$exePath.old-$PID-$([Guid]::NewGuid().ToString('N'))"
$canonicalExePath = [System.IO.Path]::GetFullPath($exePath)
$deploymentMutex = [System.Threading.Mutex]::new($false, "Local\TerminalVoiceOverlay.Deployment")
$mutexHeld = $false
$swapped = $false
$updateSucceeded = $false
$exitCode = 0
$maxAttempts = 10
$watcherStopped = $false

function Get-TargetOverlayProcesses {
    @(Get-CimInstance Win32_Process -Filter "Name = '$exeName'" -ErrorAction Stop | Where-Object {
        $_.ExecutablePath -and
        [System.IO.Path]::GetFullPath($_.ExecutablePath).Equals(
            $canonicalExePath,
            [System.StringComparison]::OrdinalIgnoreCase
        )
    })
}

function Get-TargetUiProcesses {
    @(Get-TargetOverlayProcesses | Where-Object {
        $_.CommandLine -and $_.CommandLine -match '(?:^|\s)--run(?:\s|$)'
    })
}

function Stop-TargetOverlayProcesses {
    for ($i = 0; $i -lt $maxAttempts; $i++) {
        $processes = @(Get-TargetOverlayProcesses)
        if ($processes.Count -eq 0) {
            return
        }
        foreach ($process in $processes) {
            if ($i -eq 0) {
                try {
                    $managedProcess = Get-Process -Id $process.ProcessId -ErrorAction Stop
                    if ($managedProcess.CloseMainWindow()) {
                        $managedProcess.WaitForExit(2000) | Out-Null
                    }
                } catch { }
            }
            if (-not (Get-Process -Id $process.ProcessId -ErrorAction SilentlyContinue)) {
                continue
            }
            Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Milliseconds 300
    }

    $remaining = @(Get-TargetOverlayProcesses)
    if ($remaining.Count -gt 0) {
        throw "Prozess am Zielpfad laesst sich nicht beenden (PID: $($remaining.ProcessId -join ', '))."
    }
}

function Get-TargetWatcherProcesses {
    @(Get-CimInstance Win32_Process -Filter "Name = 'wscript.exe' OR Name = 'cscript.exe'" -ErrorAction Stop | Where-Object {
        $_.CommandLine -and $_.CommandLine.Contains($watcherPath, [System.StringComparison]::OrdinalIgnoreCase)
    })
}

function Stop-TargetWatcher {
    $watchers = @(Get-TargetWatcherProcesses)
    foreach ($watcher in $watchers) {
        Stop-Process -Id $watcher.ProcessId -Force -ErrorAction Stop
        $script:watcherStopped = $true
    }
    if (@(Get-TargetWatcherProcesses).Count -gt 0) {
        throw "Der zugehoerige Overlay-Watcher laesst sich nicht beenden."
    }
}

function Start-TargetWatcher {
    if (-not $watcherStopped -or -not (Test-Path -LiteralPath $watcherPath -PathType Leaf)) {
        return
    }
    if (@(Get-TargetWatcherProcesses).Count -eq 0) {
        Start-Process -FilePath "wscript.exe" -ArgumentList "`"$watcherPath`"" -ErrorAction Stop | Out-Null
    }
    $script:watcherStopped = $false
}

try {
    try {
        $mutexHeld = $deploymentMutex.WaitOne([TimeSpan]::FromMinutes(30))
    } catch [System.Threading.AbandonedMutexException] {
        $mutexHeld = $true
    }
    if (-not $mutexHeld) {
        throw "Ein anderes TerminalVoiceOverlay-Deployment ist noch aktiv."
    }
    if (-not (Test-Path -LiteralPath $exePath -PathType Leaf)) {
        throw "Die zu aktualisierende EXE fehlt: $exePath"
    }

    # Step 1: Build into a run-specific temporary directory.
    Write-Host "1/4 Baue neue Version..." -ForegroundColor Cyan
    & dotnet publish (Join-Path $PSScriptRoot "TerminalVoiceOverlay.csproj") -c Release -r win-x64 --self-contained true `
        -p:PublishSingleFile=true `
        -p:IncludeNativeLibrariesForSelfExtract=true `
        -p:EnableCompressionInSingleFile=true `
        -o $tempDir

    if ($LASTEXITCODE -ne 0) {
        throw "Build fehlgeschlagen (Exit-Code $LASTEXITCODE)."
    }
    if (-not (Test-Path -LiteralPath $tempExe -PathType Leaf)) {
        throw "Build meldete Erfolg, aber die neue EXE fehlt: $tempExe"
    }

    $newSize = [math]::Round((Get-Item -LiteralPath $tempExe).Length / 1MB, 1)
    Write-Host "   Neue Version gebaut ($newSize MB)" -ForegroundColor Green

    # Stage the complete file before touching the installed executable.
    if (Test-Path -LiteralPath $newExe) {
        Remove-Item -LiteralPath $newExe -Force
    }
    Copy-Item -LiteralPath $tempExe -Destination $newExe -Force
    if ((Get-FileHash -LiteralPath $tempExe -Algorithm SHA256).Hash -ne
        (Get-FileHash -LiteralPath $newExe -Algorithm SHA256).Hash) {
        throw "Die gestagte EXE stimmt nicht mit dem Build-Artefakt ueberein."
    }

    # Step 2: Stop only overlay processes running from the deployment path.
    Write-Host "2/4 Beende laufendes Overlay..." -ForegroundColor Cyan
    Stop-TargetWatcher
    Stop-TargetOverlayProcesses

    # Step 3: Swap the staged executable, restoring the old file after every failed attempt.
    Write-Host "3/4 Ersetze .exe..." -ForegroundColor Cyan
    for ($i = 0; $i -lt $maxAttempts; $i++) {
        try {
            Stop-TargetOverlayProcesses
            Move-Item -LiteralPath $exePath -Destination $backupExe -ErrorAction Stop
            Move-Item -LiteralPath $newExe -Destination $exePath -ErrorAction Stop
            $swapped = $true
            Write-Host "   .exe ersetzt (Versuch $($i + 1))" -ForegroundColor Green
            break
        } catch {
            $swapError = $_.Exception.Message
            if ((Test-Path -LiteralPath $backupExe) -and -not (Test-Path -LiteralPath $exePath)) {
                Move-Item -LiteralPath $backupExe -Destination $exePath -ErrorAction Stop
            }
            Write-Host "   Versuch $($i + 1): $swapError" -ForegroundColor Yellow
            Start-Sleep -Milliseconds 300
        }
    }
    if (-not $swapped) {
        throw "Konnte die EXE nach $maxAttempts Versuchen nicht ersetzen."
    }

    # Step 4: Keep the backup until one concrete new UI process stays alive.
    Write-Host "4/4 Starte neue Version..." -ForegroundColor Cyan
    Stop-TargetOverlayProcesses
    $startedProcess = Start-Process -FilePath $exePath -PassThru -ErrorAction Stop
    $watchdogProcessId = $startedProcess.Id
    $uiProcessId = $null

    for ($i = 0; $i -lt 6; $i++) {
        Start-Sleep -Milliseconds 500
        $uiProcess = @(Get-TargetUiProcesses | Select-Object -First 1)
        if ($uiProcess.Count -gt 0) {
            $uiProcessId = $uiProcess[0].ProcessId
            break
        }
    }
    if ($null -eq $uiProcessId) {
        throw "Die neue EXE startete keinen UI-Prozess mit --run (Watchdog-PID: $watchdogProcessId)."
    }

    for ($i = 0; $i -lt 6; $i++) {
        Start-Sleep -Milliseconds 500
        $stableProcess = @(Get-TargetUiProcesses | Where-Object { $_.ProcessId -eq $uiProcessId })
        if ($stableProcess.Count -eq 0) {
            throw "Der neue UI-Prozess blieb nicht stabil aktiv (PID: $uiProcessId, Watchdog-PID: $watchdogProcessId)."
        }
    }

    Start-TargetWatcher
    $updateSucceeded = $true
    try {
        Remove-Item -LiteralPath $backupExe -Force -ErrorAction Stop
    } catch {
        Write-Host "   WARNUNG: Verifiziertes Backup konnte nicht entfernt werden: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    Write-Host "`nUpdate erfolgreich! Neue UI laeuft stabil (PID: $uiProcessId)" -ForegroundColor Green
} catch {
    $updateError = $_.Exception.Message
    $rollbackError = $null

    if (Test-Path -LiteralPath $backupExe) {
        try {
            Stop-TargetOverlayProcesses
            if (Test-Path -LiteralPath $exePath) {
                Remove-Item -LiteralPath $exePath -Force -ErrorAction Stop
            }
            Move-Item -LiteralPath $backupExe -Destination $exePath -ErrorAction Stop

            $restoredProcesses = @(Get-TargetOverlayProcesses)
            if ($restoredProcesses.Count -eq 0) {
                Start-Process -FilePath $exePath -ErrorAction Stop | Out-Null
            }
            Start-TargetWatcher
            Write-Host "   Alte EXE wurde wiederhergestellt." -ForegroundColor Yellow
        } catch {
            $rollbackError = $_.Exception.Message
        }
    } elseif ($swapped) {
        $rollbackError = "Das Backup der alten EXE ist nicht mehr vorhanden."
    }

    Write-Host "`nUpdate fehlgeschlagen: $updateError" -ForegroundColor Red
    if ($rollbackError) {
        Write-Host "Rollback fehlgeschlagen: $rollbackError" -ForegroundColor Red
    }
    $exitCode = 1
} finally {
    if (Test-Path -LiteralPath $newExe) {
        Remove-Item -LiteralPath $newExe -Force -ErrorAction SilentlyContinue
    }
    if (Test-Path -LiteralPath $tempDir) {
        Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
    if ($watcherStopped) {
        try { Start-TargetWatcher } catch {
            Write-Host "Watcher-Neustart fehlgeschlagen: $($_.Exception.Message)" -ForegroundColor Red
            $exitCode = 1
        }
    }
    if ($mutexHeld) {
        $deploymentMutex.ReleaseMutex()
    }
    $deploymentMutex.Dispose()
}

if (-not $updateSucceeded -or $exitCode -ne 0) {
    exit 1
}
