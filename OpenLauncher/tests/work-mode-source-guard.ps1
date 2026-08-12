$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$viewModel = Get-Content -LiteralPath (Join-Path $root 'ViewModels\MainViewModel.cs') -Raw
$launcher = Get-Content -LiteralPath (Join-Path $root 'Services\OpenLauncherService.cs') -Raw
$profiles = Get-Content -LiteralPath (Join-Path $root 'Services\InstructionProfileService.cs') -Raw
$editor = Get-Content -LiteralPath (Join-Path $root 'ProfileEditorWindow.xaml.cs') -Raw
$plugin = Get-Content -LiteralPath (Join-Path (Split-Path -Parent $root) 'opencode-setup\plugins\work-mode.js') -Raw
$workMode = Get-Content -LiteralPath (Join-Path (Split-Path -Parent $root) 'opencode-setup\plugins\token-cost-sidebar\dist\work-mode.ts') -Raw
$xaml = Get-Content -LiteralPath (Join-Path $root 'MainWindow.xaml') -Raw
[void][xml]$xaml
$modeFiles = @('frei', 'schnell', 'normal', 'gruendlich') |
    ForEach-Object { Join-Path $root "Profiles\WorkModes\$_.md" }

$checks = @(
    @{ Name = 'vier Modi in richtiger Reihenfolge'; Pass = $viewModel -match '(?s)Id = "frei".*?Id = "schnell".*?Id = "normal".*?Id = "gruendlich"' }
    @{ Name = 'Minimalprofil ist Launcher-Standard'; Pass = $viewModel -match 'SelectedProfile = Profiles\.Single\(profile => profile\.Id == "minimal"\)' }
    @{ Name = 'Freimodus ist Launcher-Standard'; Pass = $viewModel -match 'SelectedWorkMode = WorkModes\.Single\(mode => mode\.Id == "frei"\)' }
    @{ Name = 'Modellwechsel setzt das Minimalprofil'; Pass = $viewModel -match 'SelectedProfile = Profiles\.FirstOrDefault\(profile => profile\.Id == "minimal"\)' }
    # Zwei Fundstellen: Konstruktor (Erststart) und Profilwechsel. Beide muessen auf "frei" stehen,
    # sonst kippt die Vorauswahl bei Profil- oder Modellwechsel wieder auf einen anderen Modus.
    @{ Name = 'Start UND Profilwechsel setzen den Freimodus'; Pass = ([regex]::Matches($viewModel, 'SelectedWorkMode = WorkModes\.Single\(mode => mode\.Id == "frei"\)')).Count -eq 2 }
    @{ Name = 'kein anderer Modus als Vorauswahl'; Pass = -not ($viewModel -match 'SelectedWorkMode = WorkModes\.Single\(mode => mode\.Id == "(?!frei")') }
    @{ Name = 'Standard und Minimal wählen High vor'; Pass = $viewModel -match 'SelectedProfile\.Id == "strict" \? "xhigh" : "high"' }
    @{ Name = 'Nicht unterstützte Stufe fällt auf höchste verfügbare zurück'; Pass = $viewModel -match '\?\? ThinkingOptions\.Last\(\)' }
    @{ Name = 'Thinking-Vorauswahl folgt Profilwechsel und Optionsladen'; Pass = ([regex]::Matches($viewModel, 'SelectProfileThinkingOption\(\);')).Count -eq 2 }
    @{ Name = 'Modus wird an den OpenCode-Start übergeben'; Pass = $viewModel -match '_launcher\.Launch\(modelString, WorkDir, thinkingLevel, profileSession\.ConfigPath, SelectedWorkMode\.Id\)' }
    @{ Name = 'Startskript setzt prozesslokalen Modus'; Pass = $launcher -match '\$env:OPENLAUNCHER_WORK_MODE = \{\{PowerShellLiteral\(workMode\)\}\}' }
    @{ Name = 'Modus-Auswahl steht unter Profil'; Pass = $xaml -match 'Text="MODUS"' -and $xaml -match 'ItemsSource="\{Binding WorkModes\}"' }
    @{ Name = 'vier Modus-Kästchen'; Pass = $xaml -match '<UniformGrid Rows="1" Columns="4"/>' }
    @{ Name = 'Vorauswahl bleibt unabhängig vom ersten Listeneintrag'; Pass = ([regex]::Matches($xaml, 'IsSynchronizedWithCurrentItem="False"')).Count -ge 2 }
    @{ Name = 'je Modus eine bearbeitbare Prompt-Datei'; Pass = @($modeFiles | Where-Object { Test-Path -LiteralPath $_ }).Count -eq 4 }
    @{ Name = 'Profil-Button und Modus-Button nebeneinander'; Pass = $xaml -match '(?s)Content="Profil bearbeiten".*?Content="Modus bearbeiten"' }
    @{ Name = 'Modus-Button ruft den Modus-Editor'; Pass = $xaml -match 'Command="\{Binding EditWorkModeCommand\}"' -and $viewModel -match 'private void EditWorkMode\(\)' }
    @{ Name = 'Modus-Editor speichert in die Modus-Datei'; Pass = $viewModel -match '_profiles\.SaveWorkMode\(SelectedWorkMode\.Id, editor\.GlobalText\)' -and $profiles -match 'public void SaveWorkMode' }
    @{ Name = 'Editor kennt die Modus-Betriebsart'; Pass = $editor -match 'public ProfileEditorWindow\(string workModeName, string sourcePath, string promptText\)' }
    @{ Name = 'Claude bekommt den Modus hinter das Profil'; Pass = $viewModel -match 'EnsureClaudeConfigDir\(SelectedProfile\.Id, SelectedWorkMode\.Id\)' -and $profiles -match 'private string ComposeClaudeContext' }
    @{ Name = 'OpenCode liest denselben Modus-Prompt aus dem Launcher-Ordner'; Pass = $workMode -match "join\(homedir\(\), ""proggs"", ""OpenLauncher"", ""Profiles"", ""WorkModes""\)" -and $plugin -match 'await resolveWorkModeInstruction\(mode\)' }
)

$failed = @($checks | Where-Object { -not $_.Pass })
foreach ($check in $checks) {
    $mark = if ($check.Pass) { '[OK]' } else { '[FEHLER]' }
    "$mark $($check.Name)"
}

if ($failed.Count -gt 0) {
    throw "$($failed.Count) Arbeitsmodus-Prüfung(en) fehlgeschlagen."
}
