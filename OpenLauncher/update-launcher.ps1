[CmdletBinding()]
param(
    # Ueberspringt die Rueckfrage und schliesst einen laufenden Launcher ohne Dialog.
    [switch]$Force,
    # Baut auch dann neu, wenn der vorhandene Build bereits aktuell ist.
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$projectFile = Join-Path $projectRoot 'OpenLauncher.csproj'
$releaseDirectory = Join-Path $projectRoot 'bin\Release\net8.0-windows10.0.19041.0\win-x64'
$launcherExe = Join-Path $releaseDirectory 'OpenLauncher.exe'

if (-not (Test-Path -LiteralPath $projectFile)) {
    throw "Projektdatei nicht gefunden: $projectFile"
}

# Ohne echtes Terminal (Aufruf aus einem Agenten, einer Pipeline oder einer Verknuepfung mit
# umgeleiteter Eingabe) darf KEIN modaler Dialog erscheinen: er wartet dort ewig auf einen Klick,
# den niemand sieht, und der Aufrufer laeuft in sein Timeout. In dem Fall gilt automatisch "Ja".
$interactive = -not $Force -and -not [System.Console]::IsInputRedirected -and [Environment]::UserInteractive

<#
.SYNOPSIS
    Version aus der Projektdatei -- einzige Quelle der Wahrheit fuer den Versionsstand.
#>
function Get-ProjectVersion {
    $xml = [xml](Get-Content -LiteralPath $projectFile -Raw)
    foreach ($group in @($xml.Project.PropertyGroup)) {
        if ($group.Version) { return ([string]$group.Version).Trim() }
    }
    return $null
}

<#
.SYNOPSIS
    Prueft, ob der vorhandene Release-Build bereits dem Quellstand entspricht.
.DESCRIPTION
    Zwei Bedingungen: die gebaute Exe traegt die Version aus der Projektdatei, UND keine Quelldatei
    ist neuer als die Exe. Damit erkennt das Skript einen Build, der kurz zuvor schon gelaufen ist,
    und baut nicht ein zweites Mal.
#>
function Test-BuildIstAktuell {
    if (-not (Test-Path -LiteralPath $launcherExe)) { return $false }

    $projectVersion = Get-ProjectVersion
    if (-not $projectVersion) { return $false }

    $builtVersion = [System.Diagnostics.FileVersionInfo]::GetVersionInfo($launcherExe).ProductVersion
    if (-not $builtVersion) { return $false }
    # ProductVersion kann ein Suffix wie "+commit" tragen -- nur der Versionsteil zaehlt.
    $builtVersion = ($builtVersion -split '\+')[0].Trim()
    if ($builtVersion -ne $projectVersion) { return $false }

    # Nur echter Quellcode zaehlt. Laufzeitdateien wie models.json schreibt der Launcher im Betrieb
    # selbst neu -- wuerden sie mitgeprueft, gaelte der Build unmittelbar nach dem Start wieder als
    # veraltet und es wuerde bei jedem Aufruf sinnlos neu gebaut.
    $exeStand = (Get-Item -LiteralPath $launcherExe).LastWriteTimeUtc
    $quellen = Get-ChildItem -Path $projectRoot -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Extension -in '.cs', '.xaml', '.csproj' -and $_.FullName -notmatch '\\(bin|obj)\\'
        }
    foreach ($quelle in $quellen) {
        if ($quelle.LastWriteTimeUtc -gt $exeStand) { return $false }
    }
    return $true
}

$buildAktuell = -not $Rebuild -and (Test-BuildIstAktuell)
$runningLaunchers = @(Get-Process -Name 'OpenLauncher' -ErrorAction SilentlyContinue)

# Nichts zu tun: der Build entspricht dem Quellstand und laeuft bereits. Sofort zurueck, statt den
# Benutzer zu fragen und anschliessend denselben Stand noch einmal zu bauen.
if ($buildAktuell -and $runningLaunchers.Count -gt 0) {
    Write-Output "LAUNCHER_UPDATE_STATUS=already-current VERSION=$(Get-ProjectVersion) PID=$($runningLaunchers[0].Id)"
    exit 0
}

if ($runningLaunchers.Count -gt 0) {
    if ($interactive) {
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
    }
    else {
        Write-Output 'LAUNCHER_UPDATE_INFO=laufender Launcher wird ohne Rueckfrage geschlossen (keine interaktive Sitzung).'
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

if ($buildAktuell) {
    Write-Output "LAUNCHER_UPDATE_INFO=Build ist bereits aktuell ($(Get-ProjectVersion)) -- es wird nur gestartet."
}
else {
    $build = Start-Process -FilePath 'dotnet' -ArgumentList @('build', $projectFile, '-c', 'Release') `
        -WorkingDirectory $projectRoot -Wait -PassThru -NoNewWindow
    if ($build.ExitCode -ne 0) {
        throw "Der Release-Build ist fehlgeschlagen (Exit-Code $($build.ExitCode))."
    }
    if (-not (Test-Path -LiteralPath $launcherExe)) {
        throw "Die aktualisierte Launcher-Datei wurde nicht erzeugt: $launcherExe"
    }
}

$newLauncher = Start-Process -FilePath $launcherExe -PassThru
Start-Sleep -Seconds 1
if ($newLauncher.HasExited) {
    throw 'Der aktualisierte Launcher wurde gestartet, aber sofort wieder beendet.'
}

Write-Output "LAUNCHER_UPDATE_STATUS=started VERSION=$(Get-ProjectVersion) PID=$($newLauncher.Id)"
