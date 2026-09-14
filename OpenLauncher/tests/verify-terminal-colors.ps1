# Prüft die produktiven Umgebungsbereinigungen ohne App-Start oder Update-Dialog.
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$source = Get-Content (Join-Path $project 'Services/OpenLauncherService.cs') -Raw
$match = [regex]::Match($source, 'private const string InheritedAgentEnvScrubScript = """\r?\n(?<body>[\s\S]*?)\r?\n""";')
if (-not $match.Success) { throw 'Startskript-Bereinigung nicht gefunden.' }
$scrub = [scriptblock]::Create($match.Groups['body'].Value)
$updater = Get-Content (Join-Path $project 'update-launcher.ps1') -Raw
$start = $updater.IndexOf('$launcherStart = ')
$end = $updater.IndexOf('$newLauncher = ', $start)
if ($start -lt 0 -or $end -lt 0) { throw 'Updater-Startumgebung nicht gefunden.' }
$prepare = [scriptblock]::Create($updater.Substring($start, $end - $start))
$launcherExe = 'test-only-never-started.exe'
$projectRoot = $project

function Assert-Equal($Actual, $Expected, [string]$Label) {
    if ($Actual -cne $Expected) { throw "$Label stimmt nicht: '$Actual' statt '$Expected'." }
}

# Genau die beobachtete Codex-Vererbung nachstellen.
$env:TERM = 'dumb'
$env:COLORTERM = ''
$env:NO_COLOR = '1'
$env:CLAUDE_CONFIG_DIR = 'test-profile'
. $prepare
Assert-Equal $launcherStart.EnvironmentVariables['TERM'] $null 'Updater TERM'
Assert-Equal $launcherStart.EnvironmentVariables['NO_COLOR'] $null 'Updater NO_COLOR'
Assert-Equal $env:TERM 'dumb' 'Aufrufer bleibt unverändert'
Assert-Equal $env:NO_COLOR '1' 'Aufruferfarbe bleibt unverändert'
. $scrub
Assert-Equal $env:TERM $null 'CLI TERM'
Assert-Equal $env:NO_COLOR $null 'CLI NO_COLOR'
Assert-Equal $env:CLAUDE_CONFIG_DIR 'test-profile' 'Profilauswahl'

# Echte Fähigkeiten einer interaktiven Konsole bleiben erhalten.
$env:TERM = 'xterm-256color'
$env:COLORTERM = 'truecolor'
$env:TERM_PROGRAM = 'test-terminal'
. $prepare
Assert-Equal $launcherStart.EnvironmentVariables['TERM'] 'xterm-256color' 'Updater Farbfähigkeit'
Assert-Equal $launcherStart.EnvironmentVariables['COLORTERM'] 'truecolor' 'Updater Truecolor'
. $scrub
Assert-Equal $env:TERM 'xterm-256color' 'CLI Farbfähigkeit'
Assert-Equal $env:COLORTERM 'truecolor' 'CLI Truecolor'
Assert-Equal $env:TERM_PROGRAM 'test-terminal' 'Terminalkennung'
Write-Output 'PASS: Agenten-Farbabschaltung entfernt; Aufrufer, Profil und echte Farbfähigkeiten erhalten.'
