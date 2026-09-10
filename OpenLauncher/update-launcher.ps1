[CmdletBinding()]
param(
    # Ueberspringt die Rueckfrage. NUR auf ausdrueckliche Ansage des Benutzers verwenden.
    [switch]$Force,
    # Baut auch dann neu, wenn der vorhandene Build bereits aktuell ist.
    [switch]$Rebuild,
    # Nach dieser Zeit ohne Klick gilt die Rueckfrage als "Nein" -- nie als "Ja".
    [int]$DialogTimeoutSeconds = 240
)

$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$projectFile = Join-Path $projectRoot 'OpenLauncher.csproj'
$releaseDirectory = Join-Path $projectRoot 'bin\Release\net8.0-windows10.0.19041.0\win-x64'
$launcherExe = Join-Path $releaseDirectory 'OpenLauncher.exe'

if (-not (Test-Path -LiteralPath $projectFile)) {
    throw "Projektdatei nicht gefunden: $projectFile"
}

# Die Rueckfrage kommt IMMER -- auch wenn ein Agent (Claude Code, Codex, OpenCode) das Skript
# aufruft. Frueher entfiel sie ohne Terminal und es galt still "Ja": das Update lief dann ohne
# Freigabe. Dass der Dialog damals "haengen" blieb, lag nicht am fehlenden Terminal, sondern daran,
# dass die WPF-MessageBox eines Hintergrundprozesses hinter anderen Fenstern landete. Deshalb jetzt:
# systemmodal + oberstes Fenster + Vordergrund, und ein Zeitlimit, nach dem "Nein" gilt.
Add-Type -Namespace OpenLauncherUpdate -Name NativeDialog -MemberDefinition @'
[DllImport("user32.dll", CharSet = CharSet.Unicode)]
public static extern int MessageBoxTimeoutW(IntPtr hWnd, string text, string caption, uint type, ushort language, uint milliseconds);
'@ -ErrorAction SilentlyContinue

<#
.SYNOPSIS
    Ja/Nein-Fenster, das garantiert sichtbar ganz oben erscheint. Rueckgabe: yes | no | timeout.
#>
function Show-UpdateDialog([bool]$LauncherLaeuft) {
    $text = if ($LauncherLaeuft) {
        "Der OpenLauncher läuft noch. Für das Update wird er geschlossen, die neue Version wird gebaut und danach automatisch gestartet.`n`nNicht gespeicherte Eingaben gehen dabei verloren. Jetzt aktualisieren?"
    } else {
        "Die neue Version des OpenLauncher wird gebaut und danach gestartet.`n`nJetzt aktualisieren?"
    }
    $title = 'OpenLauncher aktualisieren'
    # MB_YESNO | MB_ICONWARNING | MB_SYSTEMMODAL | MB_SETFOREGROUND | MB_TOPMOST
    $flags = 0x4 -bor 0x30 -bor 0x1000 -bor 0x10000 -bor 0x40000
    try {
        $result = [OpenLauncherUpdate.NativeDialog]::MessageBoxTimeoutW([IntPtr]::Zero, $text, $title, $flags, 0, [uint32]($DialogTimeoutSeconds * 1000))
        if ($result -eq 32000) { return 'timeout' }
    }
    catch {
        # Fallback, falls die native Funktion nicht verfuegbar ist: dieselben Flags, eigenes Zeitlimit.
        # Write-Host statt Write-Output: sonst landet die Zeile im Rueckgabewert der Funktion.
        Write-Host "LAUNCHER_UPDATE_INFO=nativer Dialog nicht verfuegbar ($($_.Exception.Message)) -- Ersatzdialog."
        $result = (New-Object -ComObject WScript.Shell).Popup($text, $DialogTimeoutSeconds, $title, $flags)
        if ($result -eq -1) { return 'timeout' }
    }
    if ($result -eq 6) { return 'yes' }
    return 'no'
}

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

# Ohne Freigabe per Klick passiert nichts: kein Schliessen, kein Build, kein Start.
if ($Force) {
    Write-Output 'LAUNCHER_UPDATE_INFO=-Force gesetzt: Update ohne Rueckfrage.'
}
else {
    switch (Show-UpdateDialog ($runningLaunchers.Count -gt 0)) {
        'yes' { Write-Output 'LAUNCHER_UPDATE_INFO=Update per Klick freigegeben.' }
        'timeout' {
            Write-Output "LAUNCHER_UPDATE_STATUS=no-answer (kein Klick innerhalb von $DialogTimeoutSeconds Sekunden -- nichts geaendert)"
            exit 0
        }
        default {
            Write-Output 'LAUNCHER_UPDATE_STATUS=cancelled'
            exit 0
        }
    }
}

if ($runningLaunchers.Count -gt 0) {
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
    # Keine Build-Server: MSBuild-Knoten, MSBuild-Server und der Compiler-Server (VBCSCompiler)
    # bleiben sonst nach dem Build minutenlang am Leben und halten die geerbte Ausgabe-Pipe des
    # Aufrufers offen. Das Skript ist dann laengst fertig, der Agent wartet aber bis in sein
    # Timeout. Doppelt abgesichert: Umgebungsvariablen UND Kommandozeilen-Schalter.
    $env:MSBUILDDISABLENODEREUSE = '1'
    $env:DOTNET_CLI_USE_MSBUILD_SERVER = '0'
    $env:UseSharedCompilation = 'false'
    $build = Start-Process -FilePath 'dotnet' -ArgumentList @(
        'build', $projectFile, '-c', 'Release',
        '-nodeReuse:false', '-p:UseSharedCompilation=false', '-p:UseRazorBuildServer=false'
    ) -WorkingDirectory $projectRoot -Wait -PassThru -NoNewWindow
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
exit 0
