$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$viewModel = Get-Content -LiteralPath (Join-Path $root 'ViewModels\MainViewModel.cs') -Raw
$launcher = Get-Content -LiteralPath (Join-Path $root 'Services\OpenCodeLauncherService.cs') -Raw
$xaml = Get-Content -LiteralPath (Join-Path $root 'MainWindow.xaml') -Raw
[void][xml]$xaml

$checks = @(
    @{ Name = 'vier Modi in richtiger Reihenfolge'; Pass = $viewModel -match '(?s)Id = "frei".*?Id = "schnell".*?Id = "normal".*?Id = "gruendlich"' }
    @{ Name = 'Schnellmodus bleibt Launcher-Standard'; Pass = $viewModel -match 'SelectedWorkMode = WorkModes\.Single\(mode => mode\.Id == "schnell"\)' }
    @{ Name = 'Modus wird an den OpenCode-Start übergeben'; Pass = $viewModel -match '_launcher\.Launch\(modelString, WorkDir, thinkingLevel, profileSession\.ConfigPath, SelectedWorkMode\.Id\)' }
    @{ Name = 'Startskript setzt prozesslokalen Modus'; Pass = $launcher -match '\$env:OPENCODE_LAUNCHER_WORK_MODE = \{\{PowerShellLiteral\(workMode\)\}\}' }
    @{ Name = 'Modus-Auswahl steht unter Profil'; Pass = $xaml -match 'Text="MODUS"' -and $xaml -match 'ItemsSource="\{Binding WorkModes\}"' }
    @{ Name = 'vier Modus-Kästchen'; Pass = $xaml -match '<UniformGrid Rows="1" Columns="4"/>' }
    @{ Name = 'Vorauswahl bleibt unabhängig vom ersten Listeneintrag'; Pass = ([regex]::Matches($xaml, 'IsSynchronizedWithCurrentItem="False"')).Count -ge 2 }
)

$failed = @($checks | Where-Object { -not $_.Pass })
foreach ($check in $checks) {
    $mark = if ($check.Pass) { '[OK]' } else { '[FEHLER]' }
    "$mark $($check.Name)"
}

if ($failed.Count -gt 0) {
    throw "$($failed.Count) Arbeitsmodus-Prüfung(en) fehlgeschlagen."
}
