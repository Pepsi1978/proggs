[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$projectFile = Join-Path $projectRoot 'OpenLauncher.csproj'
$releaseDirectory = Join-Path $projectRoot 'bin\Release\net8.0-windows10.0.19041.0\win-x64'
$launcherExe = Join-Path $releaseDirectory 'OpenLauncher.exe'

if (-not (Test-Path -LiteralPath $projectFile)) {
    throw "Projektdatei nicht gefunden: $projectFile"
}

$runningLaunchers = @(Get-Process -Name 'OpenLauncher' -ErrorAction SilentlyContinue)

if ($runningLaunchers.Count -gt 0) {
    Add-Type -AssemblyName PresentationFramework
    $answer = [System.Windows.MessageBox]::Show(
        "Der OpenLauncher läuft noch. Für das Update wird er geschlossen und danach automatisch mit der neuen Version gestartet.`n`nNicht gespeicherte Eingaben gehen dabei verloren. Jetzt aktualisieren?",
        'OpenLauncher aktualisieren',
        [System.Windows.MessageBoxButton]::YesNo,
        [System.Windows.MessageBoxImage]::Warning
    )
    if ($answer -ne [System.Windows.MessageBoxResult]::Yes) {
        Write-Output 'LAUNCHER_UPDATE_STATUS=cancelled'
        exit 0
    }

    foreach ($launcher in $runningLaunchers) {
        if (-not $launcher.HasExited -and -not $launcher.CloseMainWindow()) {
            throw "Der laufende Launcher konnte nicht kontrolliert geschlossen werden. Bitte schließe ihn manuell und starte das Update erneut."
        }
    }

    $deadline = (Get-Date).AddSeconds(10)
    do {
        Start-Sleep -Milliseconds 200
        $runningLaunchers = @($runningLaunchers | Where-Object { -not $_.HasExited })
    } while ($runningLaunchers.Count -gt 0 -and (Get-Date) -lt $deadline)

    if ($runningLaunchers.Count -gt 0) {
        throw "Der Launcher wurde nicht innerhalb von 10 Sekunden geschlossen. Das Update wurde nicht durchgeführt."
    }
}

$build = Start-Process -FilePath 'dotnet' -ArgumentList @('build', $projectFile, '-c', 'Release') `
    -WorkingDirectory $projectRoot -Wait -PassThru -NoNewWindow
if ($build.ExitCode -ne 0) {
    throw "Der Release-Build ist fehlgeschlagen (Exit-Code $($build.ExitCode))."
}
if (-not (Test-Path -LiteralPath $launcherExe)) {
    throw "Die aktualisierte Launcher-Datei wurde nicht erzeugt: $launcherExe"
}

$newLauncher = Start-Process -FilePath $launcherExe -PassThru
Start-Sleep -Seconds 1
if ($newLauncher.HasExited) {
    throw 'Der aktualisierte Launcher wurde gestartet, aber sofort wieder beendet.'
}

Write-Output "LAUNCHER_UPDATE_STATUS=started PID=$($newLauncher.Id)"
