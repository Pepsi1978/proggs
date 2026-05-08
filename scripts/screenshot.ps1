#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Robuster ADB-Screenshot — duenner Wrapper auf screenshot.sh.

.DESCRIPTION
    Die eigentliche Logik lebt in screenshot.sh (Bash). Dieser Wrapper ruft
    Bash auf und delegiert alle Argumente weiter. Damit gibt es eine einzige
    Quelle der Wahrheit (das .sh) und identisches Verhalten auf Windows
    (Git Bash) und macOS — egal ob aus PowerShell oder Bash aufgerufen.

    Hintergrund: PowerShell hat eigene Quirks bei der Stderr-Handhabung von
    Native-Commands (PSNativeCommandUseErrorActionPreference, $LASTEXITCODE-
    Verhalten bei Pipeline-Stoerungen) die das gleiche Rezept das im Bash-
    Skript funktioniert in PowerShell nicht 1:1 durchlaufen lassen. Statt
    PS-spezifische Workarounds zu pflegen, delegieren wir.

.PARAMETER Args
    Alle Argumente werden 1:1 an screenshot.sh weitergereicht.
    Verwendet werden die gleichen Flags wie im .sh: -s/--device, -o/--output,
    -n/--name, -d/--display.

.EXAMPLE
    .\screenshot.ps1
    Erstes verbundenes Geraet, Default-Pfad in ~/proggs/screenshots/.

.EXAMPLE
    .\screenshot.ps1 -n tasks_screen
    Mit Name-Praefix.

.EXAMPLE
    .\screenshot.ps1 -s RFCX70KTDFX -o C:\Users\barwa\Desktop\test.png
    Spezifisches Geraet und Pfad.
#>
[CmdletBinding(PositionalBinding=$false)]
param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$Args
)

# Bash-Skript-Pfad — neben diesem PS1-Skript
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$bashScript = Join-Path $scriptDir "screenshot.sh"

if (-not (Test-Path $bashScript)) {
    Write-Error "Bash-Skript nicht gefunden: $bashScript"
    exit 1
}

# Git-Bash-Pfade durchprobieren (typische Installationsorte auf Windows)
$bashCandidates = @(
    "C:\Program Files\Git\bin\bash.exe",
    "C:\Program Files (x86)\Git\bin\bash.exe",
    "$env:USERPROFILE\scoop\apps\git\current\bin\bash.exe",
    "/bin/bash",
    "/usr/bin/bash"
)
$bash = $null
foreach ($c in $bashCandidates) {
    if (Test-Path $c) { $bash = $c; break }
}
if (-not $bash) { $bash = (Get-Command bash -ErrorAction SilentlyContinue).Source }
if (-not $bash) {
    Write-Error "bash nicht gefunden. Installiere Git for Windows oder rufe screenshot.sh direkt aus Bash auf."
    exit 1
}

# Bash-Skript-Pfad in Unix-Stil umwandeln fuer Git Bash auf Windows
# (C:\Users\... -> /c/Users/...)
$bashScriptPath = $bashScript
if ($IsWindows -or $env:OS -eq "Windows_NT") {
    $bashScriptPath = $bashScript -replace '^([A-Za-z]):', '/$1' -replace '\\', '/'
    $bashScriptPath = $bashScriptPath.ToLower().Substring(0, 2) + $bashScriptPath.Substring(2)
}

# Delegieren
& $bash $bashScriptPath @Args
exit $LASTEXITCODE
